package tv.own.owntv.player

import org.junit.Assert.*
import org.junit.Test

class PlayerHudVisibilityTest {
    @Test fun channelOpensWithInformationAndOkRevealsControls() {
        val hud = PlayerHudVisibility(compactStart = true)
        assertTrue(hud.informationVisible); assertFalse(hud.controlsVisible)
        hud.showControls()
        assertTrue(hud.controlsVisible); assertFalse(hud.informationVisible)
    }
    @Test fun anExpiredIntroCannotDismissControlsOpenedWithOk() {
        val hud = PlayerHudVisibility(true)
        hud.showControls(); hud.hideInformation()
        assertTrue(hud.controlsVisible)
    }
    @Test fun hidingControlsDoesNotReopenTheIntroduction() {
        val hud = PlayerHudVisibility(true)
        hud.showControls(); hud.hideControls()
        assertFalse(hud.controlsVisible); assertFalse(hud.informationVisible)
    }
    @Test fun zappingReturnsToInformationAndANewChannelStartsCompact() {
        val hud = PlayerHudVisibility(true)
        hud.showControls(); hud.showInformation()
        assertTrue(hud.informationVisible); assertFalse(hud.controlsVisible)
        hud.hideInformation()
        assertTrue(PlayerHudVisibility(true).informationVisible)
    }
    @Test fun nonChannelPlaybackRetainsItsInitialControls() {
        assertTrue(PlayerHudVisibility(false).controlsVisible)
    }
}
