package tv.own.owntv.player

import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test

class TvAvoKeyPolicyTest {
    @Test fun arrowsAndChannelKeysKeepZappingWithoutTransportControls() {
        for (code in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT)) {
            assertEquals(TvAvoAction.NEXT, tvAvoKeyAction(code, true, true, false))
            assertEquals(TvAvoAction.CONSUME, tvAvoKeyAction(code, false, true, false))
        }
        for (code in listOf(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            assertEquals(TvAvoAction.PREVIOUS, tvAvoKeyAction(code, true, true, false))
        assertEquals(TvAvoAction.CONSUME, tvAvoKeyAction(KeyEvent.KEYCODE_DPAD_UP, true, false, false))
    }
    @Test fun hiddenToolsCannotBeOpenedAndSystemVolumeStillPassesThrough() {
        for (code in listOf(KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_GUIDE,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_REWIND))
            assertEquals(TvAvoAction.CONSUME, tvAvoKeyAction(code, true, true, false))
        for (code in listOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_POWER))
            assertEquals(TvAvoAction.PASS, tvAvoKeyAction(code, true, true, false))
        assertEquals(TvAvoAction.PASS, tvAvoKeyAction(KeyEvent.KEYCODE_DPAD_UP, true, true, true))
    }
    @Test fun backExitsAndEveryOkOnlyRevealsInformationWithoutPausing() {
        assertEquals(TvAvoAction.CONSUME, tvAvoKeyAction(KeyEvent.KEYCODE_BACK, true, true, false))
        assertEquals(TvAvoAction.BACK, tvAvoKeyAction(KeyEvent.KEYCODE_BACK, false, true, false))
        repeat(2) {
            assertEquals(TvAvoAction.CONSUME, tvAvoKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, true, true, false))
            assertEquals(TvAvoAction.SHOW, tvAvoKeyAction(KeyEvent.KEYCODE_DPAD_CENTER, false, true, false))
        }
        assertEquals(TvAvoAction.TOGGLE, tvAvoKeyAction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, true, true, false))
    }
}
