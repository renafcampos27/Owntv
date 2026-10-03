package tv.own.owntv.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {
    @Test fun wideTouchWindowKeepsTouchControls() {
        val layout = InteractionLayout(compactWindow = false, remote = false)
        assertTrue(layout.compactPlayerControls)
        assertFalse(layout.compactWindow)
    }
    @Test fun narrowBoxKeepsRemoteInteraction() {
        val layout = InteractionLayout(compactWindow = true, remote = true)
        assertTrue(layout.compactPlayerControls)
        assertFalse(layout.touch)
    }
    @Test fun smallWindowUsesCompactLayout() {
        assertTrue(compactWindow(360f))
        assertTrue(compactWindow(839f))
    }
    @Test fun wideWindowKeepsPanels() {
        assertFalse(compactWindow(840f))
        assertFalse(compactWindow(1280f))
    }
}
