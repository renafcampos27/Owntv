package tv.own.owntv.features.shell

import org.junit.Assert.assertEquals
import org.junit.Test
import tv.own.owntv.core.nav.MainSection

class ShellBackPolicyTest {
    @Test fun compactSettingsReturnsToMoreInsteadOfOpeningSimpleMenu() {
        assertEquals(ShellBackAction.MORE, shellBackAction(MainSection.SETTINGS, true, false, false))
    }
    @Test fun compactRootCanExitAndOtherDestinationsReturnToChannels() {
        assertEquals(ShellBackAction.EXIT, shellBackAction(MainSection.LIVE_TV, true, false, false))
        assertEquals(ShellBackAction.LIVE, shellBackAction(MainSection.MORE, true, false, false))
    }
    @Test fun explicitSimpleModeStillHasItsQuickMenu() {
        assertEquals(ShellBackAction.QUICK_MENU, shellBackAction(MainSection.LIVE_TV, true, true, false))
        assertEquals(ShellBackAction.LIVE, shellBackAction(MainSection.SETTINGS, true, true, false))
    }
    @Test fun televisionKeepsTheFocusAndExitBehaviour() {
        assertEquals(ShellBackAction.FOCUS, shellBackAction(MainSection.LIVE_TV, false, false, false))
        assertEquals(ShellBackAction.EXIT, shellBackAction(MainSection.LIVE_TV, false, false, true))
    }
}
