package tv.own.owntv.features.live

import org.junit.Assert.*
import org.junit.Test

class LiveMetadataWindowTest {
    @Test fun largePagingHistoryDoesNotExpandVisibleMetadataDemand() {
        assertEquals(4992..5017, liveMetadataWindow((5000..5009).toList(), 20000))
    }
    @Test fun beginningAndEndNeverReadOutsideLoadedRows() {
        assertEquals(0..17, liveMetadataWindow((0..9).toList(), 20))
        assertEquals(6..19, liveMetadataWindow((14..19).toList(), 20))
    }
    @Test fun anEmptyListDoesNotRequestMetadata() {
        assertTrue(liveMetadataWindow(emptyList(), 0).isEmpty())
    }
    @Test fun initialCompositionOnlyReadsACompactWindow() {
        assertEquals(0..8, liveMetadataWindow(emptyList(), 10000))
    }
}
