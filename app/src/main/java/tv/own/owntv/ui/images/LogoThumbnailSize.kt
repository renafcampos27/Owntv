package tv.own.owntv.ui.images

/** Bound the stored image itself; a decoder's intrinsic dimensions are not an eligibility test. */
internal fun logoThumbnailSize(width: Int, height: Int, limit: Int = 192): Pair<Int, Int> {
    require(width > 0 && height > 0 && limit > 0)
    val scale = minOf(1.0, limit.toDouble() / width, limit.toDouble() / height)
    return (width * scale).toInt().coerceIn(1, limit) to (height * scale).toInt().coerceIn(1, limit)
}
