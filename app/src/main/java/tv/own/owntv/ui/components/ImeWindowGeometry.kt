package tv.own.owntv.ui.components

/** TV keyboards may report only physical display geometry. */
internal fun imeHostHeight(displayHeight: Int, rootHeight: Int, hostHeight: Int): Int =
    maxOf(displayHeight, rootHeight, hostHeight)
