package tv.own.owntv.ui.components

internal enum class ChannelPressAction { PLAY, VERSIONS, MENU }

/** A medium hold is resolved on release, so it cannot steal the three-second menu gesture. */
internal class ChannelPressGesture {
    private var startedAt: Long? = null
    private var menuDelivered = false

    fun begin(now: Long): Boolean {
        if (startedAt != null) return false
        startedAt = now
        menuDelivered = false
        return true
    }

    fun menuDeadline(): ChannelPressAction? {
        if (startedAt == null || menuDelivered) return null
        menuDelivered = true
        return ChannelPressAction.MENU
    }

    fun release(now: Long): ChannelPressAction? {
        val start = startedAt ?: return null
        val delivered = menuDelivered
        cancel()
        if (delivered) return null
        return when ((now - start).coerceAtLeast(0)) {
            in 0 until VERSIONS_MS -> ChannelPressAction.PLAY
            in VERSIONS_MS until MENU_MS -> ChannelPressAction.VERSIONS
            else -> ChannelPressAction.MENU
        }
    }

    fun cancel() { startedAt = null; menuDelivered = false }

    companion object {
        const val VERSIONS_MS = 2_000L
        const val MENU_MS = 3_000L
    }
}
