package tv.own.owntv.player

import org.junit.Assert.*
import org.junit.Test

class TouchScrubCommitGuardTest {
    @Test fun releaseCommitsOnlyOnceForTheOriginalContext() {
        val guard = TouchScrubCommitGuard("channel-a")
        assertTrue(guard.release("channel-a", true))
        assertFalse(guard.release("channel-a", true))
    }
    @Test fun cancellationNeverCommitsEvenAfterAnotherRelease() {
        val guard = TouchScrubCommitGuard("channel-a")
        guard.cancel()
        assertFalse(guard.release("channel-a", true))
    }
    @Test fun changedChannelOrDisabledTimelineRejectsOldGesture() {
        assertFalse(TouchScrubCommitGuard("channel-a").release("channel-b", true))
        assertFalse(TouchScrubCommitGuard("channel-a").release("channel-a", false))
    }
    @Test fun changedProfileRejectsOldGestureEvenWithTheSameChannel() {
        assertFalse(TouchScrubCommitGuard("channel-a" to 1).release("channel-a" to 2, true))
    }
}
