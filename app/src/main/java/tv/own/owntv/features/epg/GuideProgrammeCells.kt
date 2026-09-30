package tv.own.owntv.features.epg

import tv.own.owntv.core.database.entity.EpgProgrammeEntity

/** Drawing boundaries: a newer programme wins where two guide entries overlap. */
internal object GuideProgrammeCells {
    data class Cell(val programmeIndex: Int, val startMs: Long, val stopMs: Long)

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
