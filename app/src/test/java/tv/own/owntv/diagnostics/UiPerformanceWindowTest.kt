package tv.own.owntv.diagnostics

import org.junit.Assert.*
import org.junit.Test

class UiPerformanceWindowTest {
    @Test fun `first draws are excluded and dropped reports are not reported as video drops`() {
        val window = UiPerformanceWindow()
        window.frame(100_000_000, 16_666_666, 3, true)
        window.frame(16_000_001, 16_666_666, 0, false)
        window.frame(20_000_000, 16_666_666, 0, false)
        val snapshot = window.take()
        assertEquals(2L, snapshot.frames)
        assertEquals(1L, snapshot.slowFrames)
        assertEquals(3L, snapshot.reportsDropped)
        assertEquals(20, snapshot.frameP95BucketMs)
    }

    @Test fun `percentiles include slow tail and windows do not accumulate old samples`() {
        val window = UiPerformanceWindow()
        repeat(94) { window.key(1) }
        repeat(6) { window.key(2000) }
        assertEquals(1000, window.take().keyQueueP95BucketMs)
        assertNull(window.take().keyQueueP95BucketMs)
        window.key(-5)
        assertEquals(0, window.take().keyQueueP95BucketMs)
    }
}
