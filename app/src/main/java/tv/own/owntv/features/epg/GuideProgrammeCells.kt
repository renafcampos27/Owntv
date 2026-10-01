package tv.own.owntv.features.epg

import tv.own.owntv.core.database.entity.EpgProgrammeEntity

/** Drawing boundaries: a newer programme wins where two guide entries overlap. */
internal object GuideProgrammeCells {
    data class Cell(val programmeIndex: Int, val startMs: Long, val stopMs: Long)

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

    /** Selection uses the same clipped intervals as drawing; a gap has no programme. */
    fun at(programmes: List<EpgProgrammeEntity>, timeMs: Long, windowStart: Long, windowEnd: Long): EpgProgrammeEntity? =
        layout(programmes, windowStart, windowEnd)
            .firstOrNull { timeMs >= it.startMs && timeMs < it.stopMs }
            ?.let { programmes[it.programmeIndex] }

    /** Navigate visible cells, including across gaps, without selecting a hidden overlap. */
    fun adjacentTime(programmes: List<EpgProgrammeEntity>, timeMs: Long, delta: Int, windowStart: Long, windowEnd: Long): Long? {
        val cells = layout(programmes, windowStart, windowEnd)
        if (cells.isEmpty()) return null
        val current = cells.indexOfFirst { timeMs >= it.startMs && timeMs < it.stopMs }
        val target = if (current >= 0) {
            (current + delta).coerceIn(0, cells.lastIndex)
        } else if (delta < 0) {
            cells.indexOfLast { it.stopMs <= timeMs }.coerceAtLeast(0)
        } else {
            cells.indexOfFirst { it.startMs > timeMs }.let { if (it < 0) cells.lastIndex else it }
        }
        return cells[target].startMs
    }

    fun layout(programmes: List<EpgProgrammeEntity>, windowStart: Long, windowEnd: Long): List<Cell> {
        if (windowEnd <= windowStart) return emptyList()
        val ordered = programmes.withIndex().filter { it.value.stopMs > it.value.startMs }.sortedBy { it.value.startMs }
        return ordered.mapIndexedNotNull { index, entry ->
            val start = maxOf(entry.value.startMs, windowStart)
            val stop = minOf(entry.value.stopMs, windowEnd, ordered.getOrNull(index + 1)?.value?.startMs ?: windowEnd)
            if (stop <= start) null else Cell(entry.index, start, stop)
        }
    }
}
