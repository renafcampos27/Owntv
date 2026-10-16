package tv.own.owntv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineSeekMathTest {
    @Test fun seekDeltaUsesTheCurrentPositionAndLongDurations() {
        assertEquals(40_000L, timelineDelta(0.5f, 100_000L, 10_000L))
        assertEquals(-10_000L, timelineDelta(-1f, 100_000L, 10_000L))
        assertEquals(2_592_000_000L, timelineDelta(1f, 2_592_000_000L, 0L))
    }
}
