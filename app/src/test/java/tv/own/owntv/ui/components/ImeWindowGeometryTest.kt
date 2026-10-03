package tv.own.owntv.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ImeWindowGeometryTest {
    @Test fun phoneUsesWindowRatherThanWholeDisplay() {
        assertEquals(800, imeHostHeight(false, 2400, 800, 800))
        assertEquals(2400, imeHostHeight(true, 2400, 800, 800))
    }
    @Test fun shrinkingAndGrowingWindowAreNotKeyboardObstruction() {
        val baseline = ImeWindowBaseline()
        baseline.update(1080, 2400, 0, 2400, false)
        baseline.update(1080, 900, 0, 900, false)
        assertEquals(0, baseline.obscured(900))
        baseline.update(1080, 2400, 0, 2400, false)
        assertEquals(0, baseline.obscured(2400))
    }
    @Test fun keyboardUsesBaselineOnlyWithinSameWindow() {
        val baseline = ImeWindowBaseline()
        baseline.update(1080, 2000, 200, 2000, false)
        baseline.update(1080, 2000, 200, 1300, true)
        assertEquals(700, baseline.obscured(1300))
        baseline.update(1080, 1000, 800, 700, true)
        assertEquals(0, baseline.obscured(700))
        baseline.update(1080, 1000, 800, 1000, false)
        assertEquals(0, baseline.obscured(1000))
    }
}
