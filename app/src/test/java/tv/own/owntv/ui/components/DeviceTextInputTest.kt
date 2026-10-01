package tv.own.owntv.ui.components

import org.junit.Assert.*
import org.junit.Test

class DeviceTextInputTest {
    @Test fun smartphoneAndTabletUseDirectInput() {
        assertFalse(remoteTextInput(false, false, true))
    }
    @Test fun televisionModeTakesPriorityOverTouchscreen() {
        assertTrue(remoteTextInput(true, false, true))
    }
    @Test fun televisionFeatureKeepsRemoteInputEvenWithGenericMode() {
        assertTrue(remoteTextInput(false, true, true))
    }
    @Test fun boxWithoutTouchscreenUsesRemoteInput() {
        assertTrue(remoteTextInput(false, false, false))
    }
}
