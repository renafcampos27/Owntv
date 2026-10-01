package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test

class MiniGuideActionTest {
    @Test fun presentOpensLiveAndCompletedProgrammeUsesArchive() {
        assertEquals(MiniGuideAction.LIVE, miniGuideAction(0, 100, 50, true))
        assertEquals(MiniGuideAction.ARCHIVE, miniGuideAction(0, 100, 100, true))
    }
    @Test fun futureAndInvalidEntriesNeverOpenLive() {
        assertEquals(MiniGuideAction.NONE, miniGuideAction(100, 200, 50, true))
        assertEquals(MiniGuideAction.NONE, miniGuideAction(100, 100, 100, true))
    }
    @Test fun guideSharingDoesNotCreateArchiveCapability() {
        assertEquals(MiniGuideAction.NONE, miniGuideAction(0, 100, 100, false))
        assertEquals(MiniGuideAction.LIVE, miniGuideAction(0, 100, 50, false))
    }
}
