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
}
