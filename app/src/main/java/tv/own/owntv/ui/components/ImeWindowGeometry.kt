package tv.own.owntv.ui.components

/** A touchscreen dialog is sized in its window; TV retains its OEM display fallback. */
internal fun imeHostHeight(remote: Boolean, displayHeight: Int, rootHeight: Int, hostHeight: Int): Int =
    if (remote) maxOf(displayHeight, rootHeight, hostHeight)
    else rootHeight.takeIf { it > 0 } ?: hostHeight.takeIf { it > 0 } ?: displayHeight

/** Visible-frame coordinates are local to the host window, not the physical screen. */
internal class ImeWindowBaseline {
    private var width = -1
    private var height = -1
    private var originY = Int.MIN_VALUE
    var bottom: Int = -1
        private set

    fun update(width: Int, height: Int, originY: Int, visibleBottom: Int, keyboardActive: Boolean) {
        if (width != this.width || height != this.height || originY != this.originY) {
            this.width = width
            this.height = height
            this.originY = originY
            bottom = -1
        }
        if (!keyboardActive && visibleBottom > 0) bottom = maxOf(bottom, visibleBottom.coerceAtMost(height))
    }

    fun obscured(visibleBottom: Int): Int =
        if (bottom > 0 && visibleBottom > 0) (bottom - visibleBottom).coerceAtLeast(0) else 0
}
