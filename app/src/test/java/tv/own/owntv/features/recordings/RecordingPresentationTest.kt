package tv.own.owntv.features.recordings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RecordingPresentationTest {
    @Test fun `measured media duration retains seconds and does not inflate short captures`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("0:00", captureDurationText(-1))
            assertEquals("0:04", captureDurationText(4_999))
            assertEquals("2:01", captureDurationText(121_000))
            assertEquals("1:01:02", captureDurationText(3_662_000))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `estimated storage duration splits hours and minutes correctly`() {
        assertEquals(Pair(0L, 0L), splitEstimatedDuration(-1000L))
        assertEquals(Pair(0L, 0L), splitEstimatedDuration(0L))
        assertEquals(Pair(0L, 0L), splitEstimatedDuration(59_000L))
        assertEquals(Pair(0L, 1L), splitEstimatedDuration(60_000L))
        assertEquals(Pair(0L, 59L), splitEstimatedDuration(3_540_000L))
        assertEquals(Pair(1L, 0L), splitEstimatedDuration(3_600_000L))
        assertEquals(Pair(1L, 30L), splitEstimatedDuration(5_400_000L))
        assertEquals(Pair(24L, 15L), splitEstimatedDuration((24 * 3600 + 15 * 60) * 1000L))
    }
}
