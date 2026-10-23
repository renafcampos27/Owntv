package tv.own.owntv.features.home

internal fun <K> retainedItemIndex(previous: List<K>, index: Int, current: List<K>): Int {
    val key = previous.getOrNull(index)
    return current.indexOf(key).takeIf { it >= 0 } ?: index.coerceIn(0, current.lastIndex.coerceAtLeast(0))
}
