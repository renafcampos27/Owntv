package tv.own.owntv.features.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupActionPolicyTest {
    @Test fun externalStartupDoesNotMountAnInternalPlayer() {
        assertFalse(startupUsesInternalPlayer(true, false))
        assertTrue(startupUsesInternalPlayer(false, false))
        assertTrue(startupUsesInternalPlayer(true, true))
        assertTrue(startupUsesInternalPlayer(false, true))
    }
    @Test fun idleUnchangedProfileCanStart() {
        assertTrue(startupActionIsCurrent(0, 0, 7, 7, true))
    }
    @Test fun delayedStartupYieldsToInputProfileAndPlayback() {
        assertFalse(startupActionIsCurrent(0, 1, 7, 7, true))
        assertFalse(startupActionIsCurrent(0, 0, 7, 8, true))
        assertFalse(startupActionIsCurrent(0, 0, 7, 7, false))
    }

    @org.junit.Test fun foregroundReturnSelectsOnlyTheFixedChannelAndRespectsExplicitLaunch() {
        for (mode in tv.own.owntv.core.settings.StartupMode.entries) {
            org.junit.Assert.assertEquals(mode == tv.own.owntv.core.settings.StartupMode.SPECIFIC_CHANNEL,
                startupModeCanRun(mode, true, false, false))
            org.junit.Assert.assertFalse(startupModeCanRun(mode, true, false, true))
        }
    }

    @org.junit.Test fun recreationPreservesActivePlaybackWhileColdStartNeedsAnIdlePlayer() {
        val mode = tv.own.owntv.core.settings.StartupMode.SPECIFIC_CHANNEL
        org.junit.Assert.assertFalse(startupModeCanRun(mode, false, false, false))
        org.junit.Assert.assertTrue(startupModeCanRun(mode, false, true, false))
        org.junit.Assert.assertFalse(startupModeCanRun(mode, false, true, true))
    }
}
