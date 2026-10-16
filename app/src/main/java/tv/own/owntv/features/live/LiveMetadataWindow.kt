package tv.own.owntv.features.live

/** Metadata follows visible rows and a small overscan, never the history retained by Paging. */
internal fun liveMetadataWindow(visible: List<Int>, itemCount: Int, overscan: Int = 8): IntRange {
    return liveMetadataWindow(visible.minOrNull() ?: 0, visible.maxOrNull() ?: 0, itemCount, overscan)
}

internal fun liveMetadataWindow(firstVisible: Int, lastVisible: Int, itemCount: Int, overscan: Int = 8): IntRange {
    if (itemCount <= 0) return IntRange.EMPTY
    val first = (firstVisible - overscan).coerceAtLeast(0)
    val last = (lastVisible + overscan).coerceAtMost(itemCount - 1)
    return first..last
}
