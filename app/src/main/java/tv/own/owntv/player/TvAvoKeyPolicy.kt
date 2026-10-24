package tv.own.owntv.player

import android.view.KeyEvent

internal enum class TvAvoAction { PASS, CONSUME, BACK, SHOW, NEXT, PREVIOUS, TOGGLE, PLAY, PAUSE }

/** Only permitted remote actions reach this reader; hidden tools cannot open through shortcuts. */
internal fun tvAvoKeyAction(code: Int, down: Boolean, canZap: Boolean, inert: Boolean): TvAvoAction {
    if (inert) return TvAvoAction.PASS
    fun press(action: TvAvoAction) = if (down) action else TvAvoAction.CONSUME
    return when (code) {
        // Exit on release, matching the shell: the same press cannot also leave the channel list.
        KeyEvent.KEYCODE_BACK -> if (down) TvAvoAction.CONSUME else TvAvoAction.BACK
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT ->
            press(if (canZap) TvAvoAction.NEXT else TvAvoAction.CONSUME)
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
            press(if (canZap) TvAvoAction.PREVIOUS else TvAvoAction.CONSUME)
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER ->
            if (down) TvAvoAction.CONSUME else TvAvoAction.SHOW
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> press(TvAvoAction.TOGGLE)
        KeyEvent.KEYCODE_MEDIA_PLAY -> press(TvAvoAction.PLAY)
        KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> press(TvAvoAction.PAUSE)
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_TV_INPUT -> TvAvoAction.PASS
        else -> TvAvoAction.CONSUME
    }
}
