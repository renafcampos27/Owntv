package tv.own.owntv.features.epg

/** Half-open slot indices, including one label of overscan on each side. */
internal fun visibleGuideSlots(total: Int, scrollPx: Int, viewportPx: Int, slotPx: Float): IntRange {
    if (total <= 0 || viewportPx <= 0 || slotPx <= 0f) return IntRange.EMPTY
    val first = ((scrollPx / slotPx).toInt() - 1).coerceIn(0, total)
    val end = (kotlin.math.ceil((scrollPx + viewportPx) / slotPx).toInt() + 1).coerceIn(first, total)
    return first until end
}

internal data class GuideBrowseContext(
    val channelId: Long?, val cursorMs: Long, val leftMs: Long,
    val cellMode: Boolean, val rowIndex: Int, val rowOffset: Int,
)

/** A day follows the displayed calendar (including DST); an hour is elapsed time. */
internal fun shiftGuideTime(timeMs: Long, amount: Int, days: Boolean, zone: java.time.ZoneId): Long =
    if (days) java.time.Instant.ofEpochMilli(timeMs).atZone(zone).plusDays(amount.toLong()).toInstant().toEpochMilli()
    else timeMs + amount * 3_600_000L

/** Six-hour ownership blocks plus one neighbouring block on each side. */
internal fun guideReadWindow(centerMs: Long, from: Long, to: Long): Pair<Long, Long> {
    val block = 6 * 3_600_000L
    val start = Math.floorDiv(centerMs, block) * block
    return maxOf(from, start - block) to minOf(to, start + 2 * block)
}
