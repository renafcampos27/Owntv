package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test

class GuideLoadGateTest {
    @Test fun hiddenInvalidationsCoalesceIntoOneLoadOnEntry() {
        val gate = GuideLoadGate()
        repeat(100) { assertFalse(gate.invalidate()); assertFalse(gate.beginLoad()) }
        assertTrue(gate.setActive(true))
        assertTrue(gate.beginLoad())
        assertFalse(gate.beginLoad())
        assertFalse(gate.setActive(true))
    }
    @Test fun visibleChangeReloadsAndHiddenChangeWaits() {
        val gate = GuideLoadGate()
        gate.setActive(true); gate.beginLoad()
        assertTrue(gate.invalidate()); assertTrue(gate.beginLoad())
        assertFalse(gate.setActive(false))
        assertFalse(gate.invalidate()); assertFalse(gate.beginLoad())
        assertTrue(gate.setActive(true)); assertTrue(gate.beginLoad())
    }
    @Test fun returningRefreshesWallClockWithoutDuplicatingActivation() {
        val gate = GuideLoadGate()
        gate.setActive(true); gate.beginLoad(); gate.setActive(false)
        assertTrue(gate.setActive(true)); assertTrue(gate.beginLoad())
        assertFalse(gate.setActive(true)); assertFalse(gate.beginLoad())
    }
}
