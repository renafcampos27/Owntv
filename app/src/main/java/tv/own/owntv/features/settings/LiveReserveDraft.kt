package tv.own.owntv.features.settings

import tv.own.owntv.core.settings.LiveBuffer

/** Local editor state; remote-control events do not wait for a persistence emission. */
@ConsistentCopyVisibility
internal data class LiveReserveDraft private constructor(val minimum: Int, val maximum: Int) {
    fun adjustMinimum(delta: Int): LiveReserveDraft {
        val next = LiveBuffer.clampCustom(minimum + delta)
        return LiveReserveDraft(next, maximum.coerceIn(next, next + 10))
    }

    fun adjustMaximum(delta: Int): LiveReserveDraft =
        copy(maximum = (maximum + delta).coerceIn(minimum, minimum + 10))

    companion object {
        fun from(minimum: Int, extra: Int): LiveReserveDraft {
            val normalized = LiveBuffer.clampCustom(minimum)
            return LiveReserveDraft(normalized, normalized + LiveBuffer.clampExtra(extra))
        }

        fun defaults(): LiveReserveDraft = from(LiveBuffer.CUSTOM_DEFAULT, LiveBuffer.DEFAULT_EXTRA_SECS)
    }
}
