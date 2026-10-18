package tv.own.owntv.features.live

import org.junit.Assert.assertEquals
import org.junit.Test

class ZapTuneDelayTest {
    @Test fun `only a closely spaced press waits and a fresh or reversed clock opens immediately`() {
        assertEquals(0L, zapTuneDelayMs(null, 10))
        assertEquals(500L, zapTuneDelayMs(1000, 1170))
        assertEquals(500L, zapTuneDelayMs(1000, 2499))
        assertEquals(0L, zapTuneDelayMs(1000, 2500))
        assertEquals(0L, zapTuneDelayMs(1000, 999))
    }
}
