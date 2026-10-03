package tv.own.owntv.features.live

/** Metadata follows visible rows and a small overscan, never the history retained by Paging. */
internal fun liveMetadataWindow(visible: List<Int>, itemCount: Int, overscan: Int = 8): IntRange {
    if (itemCount <= 0) return IntRange.EMPTY
    val first = ((visible.minOrNull() ?: 0) - overscan).coerceAtLeast(0)
    val last = ((visible.maxOrNull() ?: 0) + overscan).coerceAtMost(itemCount - 1)
    return first..last
}
