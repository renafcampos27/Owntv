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

    @Test fun stageProgressionFollowsThresholds() {
        assertEquals(ChannelPressStage.IDLE, stageForElapsed(-500L))
        assertEquals(ChannelPressStage.IDLE, stageForElapsed(0L))
        assertEquals(ChannelPressStage.HOLDING_TO_VERSIONS, stageForElapsed(1L))
        assertEquals(ChannelPressStage.HOLDING_TO_VERSIONS, stageForElapsed(1_999L))
        assertEquals(ChannelPressStage.VERSIONS_THRESHOLD, stageForElapsed(2_000L))
        assertEquals(ChannelPressStage.VERSIONS_THRESHOLD, stageForElapsed(2_999L))
        assertEquals(ChannelPressStage.MENU_THRESHOLD, stageForElapsed(3_000L))
        assertEquals(ChannelPressStage.MENU_THRESHOLD, stageForElapsed(5_000L))
    }

    @Test fun boundaryReleaseTimings() {
        val press = ChannelPressGesture()
        press.begin(0)
        assertEquals(ChannelPressAction.PLAY, press.release(0))

        press.begin(0)
        assertEquals(ChannelPressAction.PLAY, press.release(1_999))

        press.begin(0)
        assertEquals(ChannelPressAction.VERSIONS, press.release(2_000))

        press.begin(0)
        assertEquals(ChannelPressAction.VERSIONS, press.release(2_999))

        press.begin(0)
        assertEquals(ChannelPressAction.MENU, press.release(3_000))
    }
}
