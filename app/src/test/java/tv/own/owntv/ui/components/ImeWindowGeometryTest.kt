package tv.own.owntv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeWindowGeometryTest {
    @Test fun televisionUsesLargestAvailableHeightForOemKeyboardFallback() {
        assertEquals(1080, imeHostHeight(1080, 720, 640))
        assertEquals(1080, imeHostHeight(720, 1080, 640))
        assertEquals(1080, imeHostHeight(720, 640, 1080))
    }
}
