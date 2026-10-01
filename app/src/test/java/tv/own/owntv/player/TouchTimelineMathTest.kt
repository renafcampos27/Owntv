package tv.own.owntv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchTimelineMathTest {
    @Test fun positionsClampToTheTrackAndEmptyTrackIsSafe() {
        assertEquals(0f, timelineFraction(-20f, 100), 0f)
        assertEquals(0.5f, timelineFraction(50f, 100), 0f)
        assertEquals(1f, timelineFraction(120f, 100), 0f)
        assertEquals(0f, timelineFraction(10f, 0), 0f)
    }
    @Test fun seekDeltaUsesTheCurrentPositionAndLongDurations() {
        assertEquals(40_000L, timelineDelta(0.5f, 100_000L, 10_000L))
        assertEquals(-10_000L, timelineDelta(-1f, 100_000L, 10_000L))
        assertEquals(2_592_000_000L, timelineDelta(1f, 2_592_000_000L, 0L))
    }
}
