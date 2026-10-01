package tv.own.owntv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PopupLayoutTest {
    @Test fun televisionKeepsEstablishedScaleAndWidth() {
        assertEquals(0.70f, popupBaseScale(true), 0f)
        assertEquals(480f, popupPanelWidth(480f, null), 0f)
    }
    @Test fun touchPopupUsesFullScaleAndFitsMeasuredWindow() {
        assertEquals(1f, popupBaseScale(false), 0f)
        assertEquals(328f, popupPanelWidth(480f, 328f), 0f)
        assertEquals(440f, popupPanelWidth(440f, 900f), 0f)
    }
}
