package tv.own.owntv.features.epg

import tv.own.owntv.core.database.entity.EpgProgrammeEntity

/** Drawing boundaries: a newer programme wins where two guide entries overlap. */
internal object GuideProgrammeCells {
    data class FragmentKey(val sourceId: Long, val channel: String, val programmeStartMs: Long, val fragmentStartMs: Long)
    data class Cell(val programmeIndex: Int, val startMs: Long, val stopMs: Long, val key: FragmentKey)

    /** Sorted, non-overlapping cells: seek once, then visit only the viewport. */
    fun visible(cells: List<Cell>, from: Long, to: Long): IntRange {
        if (to <= from) return IntRange.EMPTY
        var low = 0
        var high = cells.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (cells[mid].stopMs <= from) low = mid + 1 else high = mid
        }
        val first = low
        while (low < cells.size && cells[low].startMs < to) low++
        return first until low
    }

    fun layout(programmes: List<EpgProgrammeEntity>, windowStart: Long, windowEnd: Long): List<Cell> =
        tv.own.owntv.diagnostics.UiWorkDiagnostics.measure(tv.own.owntv.diagnostics.UiWorkWindow.Operation.EPG_LAYOUT) {
        if (windowEnd <= windowStart) return@measure emptyList()
        val ordered = programmes.withIndex().filter { it.value.stopMs > it.value.startMs }.sortedBy { it.value.startMs }
        if (ordered.isEmpty()) return@measure emptyList()
        // Sweep interval boundaries rather than truncate each row permanently at its successor.
        // A shorter/newer listing temporarily wins; the older programme can resume afterwards.
        val boundaries = LongArray(ordered.size * 2)
        ordered.forEachIndexed { i, entry -> boundaries[i * 2] = entry.value.startMs; boundaries[i * 2 + 1] = entry.value.stopMs }
        boundaries.sort()
        val active = java.util.PriorityQueue(compareByDescending<IndexedValue<EpgProgrammeEntity>> { it.value.startMs }
            .thenByDescending { it.value.stopMs }.thenBy { it.value.sourceId }.thenBy { it.value.id }
            .thenBy { it.value.title }.thenBy { it.index })
        val fragments = ArrayList<Cell>()
        var next = 0
        var previousWinner = -1
        var fragmentStart = 0L
        for (i in 0 until boundaries.lastIndex) {
            val time = boundaries[i]
            val stop = boundaries[i + 1]
            if (stop <= time) continue
            while (next < ordered.size && ordered[next].value.startMs <= time) {
                if (ordered[next].value.stopMs > time) active.add(ordered[next])
                next++
            }
            while (true) {
                val head = active.peek() ?: break
                if (head.value.stopMs > time) break
                active.remove()
            }
            val winner = active.peek()
            if (winner == null) { previousWinner = -1; continue }
            if (previousWinner != winner.index) { previousWinner = winner.index; fragmentStart = time }
            val start = maxOf(time, windowStart)
            val end = minOf(stop, windowEnd)
            if (end <= start) continue
            val last = fragments.lastOrNull()
            if (last?.programmeIndex == winner.index && last.stopMs == start) {
                fragments[fragments.lastIndex] = last.copy(stopMs = end)
            } else {
                fragments += Cell(winner.index, start, end, FragmentKey(winner.value.sourceId,
                    winner.value.epgChannelId, winner.value.startMs, fragmentStart))
            }
        }
        fragments
    }
}
