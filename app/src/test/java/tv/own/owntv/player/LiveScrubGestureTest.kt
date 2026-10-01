package tv.own.owntv.player

import org.junit.Assert.*
import org.junit.Test

class LiveScrubGestureTest {
    @Test fun shortPressSeeksTenSecondsOnlyOnRelease() {
        val gesture = LiveScrubGesture()
        assertEquals(110, gesture.press(true, 100, 100, 7200))
        assertEquals(10, gesture.release(true, 100))
        assertNull(gesture.release(true, 100))
    }

    @Test fun holdAcceleratesWithoutFloodingRepeats() {
        val gesture = LiveScrubGesture()
        assertEquals(10, gesture.press(true, 0, 0, 7200))
        assertEquals(10, gesture.press(true, 20, 0, 7200))
        assertEquals(40, gesture.press(true, 1000, 0, 7200))
        assertEquals(100, gesture.press(true, 3000, 0, 7200))
        assertEquals(100, gesture.release(true, 0))
    }

    @Test fun respectsBoundsAndUsesCurrentPositionWhenCommitting() {
        val gesture = LiveScrubGesture()
        assertEquals(120, gesture.press(true, 0, 118, 120))
        assertEquals(3, gesture.release(true, 117))
        assertEquals(0, gesture.press(false, 100, 3, 120))
        assertEquals(-3, gesture.release(false, 3))
    }

    @Test fun oppositeReleaseAndCancelledGestureDoNotSubmit() {
        val gesture = LiveScrubGesture()
        gesture.press(true, 0, 100, 7200)
        assertNull(gesture.release(false, 100))
        gesture.cancel()
        assertNull(gesture.release(true, 100))
    }
}
