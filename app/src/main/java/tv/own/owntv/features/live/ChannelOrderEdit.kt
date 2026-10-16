package tv.own.owntv.features.live

/** Returns an immutable edit result; invalid destinations leave the session unchanged. */
internal fun <T> moveOrderItem(items: List<T>, from: Int, to: Int): List<T> {
    if (from !in items.indices || to !in items.indices || from == to) return items
    return items.toMutableList().apply { add(to, removeAt(from)) }
}
