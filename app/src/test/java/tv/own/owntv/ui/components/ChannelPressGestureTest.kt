package tv.own.owntv.ui.components

import org.junit.Assert.*
import org.junit.Test

class ChannelPressGestureTest {
    @Test fun shortPressPlays() {
        val press = ChannelPressGesture()
        press.begin(100)
        assertEquals(ChannelPressAction.PLAY, press.release(2_099))
    }
    @Test fun twoSecondsChoosesVersionsWithoutPlaying() {
        val press = ChannelPressGesture()
        press.begin(100)
        assertEquals(ChannelPressAction.VERSIONS, press.release(2_100))
    }
    @Test fun belowThreeSecondsStillChoosesVersions() {
        val press = ChannelPressGesture()
        press.begin(100)
        assertEquals(ChannelPressAction.VERSIONS, press.release(3_099))
    }
    @Test fun threeSecondsOpensFullMenuEvenIfTimerWasDelayed() {
        val press = ChannelPressGesture()
        press.begin(100)
        assertEquals(ChannelPressAction.MENU, press.release(3_100))
    }
    @Test fun menuOpenedWhileHeldDoesNotClickAnythingOnRelease() {
        val press = ChannelPressGesture()
        press.begin(100)
        assertEquals(ChannelPressAction.MENU, press.menuDeadline())
        assertNull(press.menuDeadline())
        assertNull(press.release(3_500))
    }
    @Test fun repeatedKeyDownDoesNotResetElapsedTime() {
        val press = ChannelPressGesture()
        assertTrue(press.begin(100))
        assertFalse(press.begin(2_099))
        assertEquals(ChannelPressAction.VERSIONS, press.release(2_100))
    }
    @Test fun focusLossDragOrDisposalCancelsWithoutAction() {
        val press = ChannelPressGesture()
        press.begin(100)
        press.cancel()
        assertNull(press.menuDeadline())
        assertNull(press.release(3_500))
        assertTrue(press.begin(4_000))
        assertEquals(ChannelPressAction.PLAY, press.release(4_050))
    }
    @Test fun orphanKeyUpDoesNothing() {
        assertNull(ChannelPressGesture().release(10_000))
    }
}
