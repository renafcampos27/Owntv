package tv.own.owntv.features.home

internal data class HomeGuideSelection(val index: Int, val channelId: Long?, val start: Int)

internal fun reconcileHomeGuideSelection(ids: List<Long>, channelId: Long?, index: Int, start: Int): HomeGuideSelection {
    if (ids.isEmpty()) return HomeGuideSelection(-1, null, 0)
    if (index < 0) return HomeGuideSelection(-1, null, start.coerceIn(0, (ids.size - 5).coerceAtLeast(0)))
    val selected = ids.indexOf(channelId).takeIf { it >= 0 } ?: index.coerceIn(ids.indices)
    return HomeGuideSelection(selected, ids[selected], visibleStartFor(selected, start, ids.size))
}
