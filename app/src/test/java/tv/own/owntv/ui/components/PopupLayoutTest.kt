package tv.own.owntv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PopupLayoutTest {
    @Test fun televisionKeepsEstablishedScaleAndWidth() {
        assertEquals(0.70f, popupBaseScale(), 0f)
        assertEquals(480f, popupPanelWidth(480f, null), 0f)
    }
}
