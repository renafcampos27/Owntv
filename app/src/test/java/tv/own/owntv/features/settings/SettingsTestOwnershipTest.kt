package tv.own.owntv.features.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsTestOwnershipTest {
    @Test fun `reset cancels old transport and rejects its result after newer test`() {
        val requests = SettingsTestOwnership()
        val a = requests.begin()
        var cancelled = false
        requests.attach(a) { cancelled = true }
        requests.invalidate()
        val b = requests.begin()
        var displayed = "B"
        if (requests.isCurrent(a)) displayed = "A"
        assertTrue(cancelled)
        assertEquals("B", displayed)
        assertTrue(requests.isCurrent(b))
    }

    @Test fun `transport created after reset is cancelled immediately`() {
        val requests = SettingsTestOwnership()
        val a = requests.begin()
        requests.invalidate()
        var cancelled = false
        requests.attach(a) { cancelled = true }
        assertTrue(cancelled)
        assertFalse(requests.isCurrent(a))
    }

    @Test fun `finishing old test does not remove new transports cancellation`() {
        val requests = SettingsTestOwnership()
        val a = requests.begin()
        val b = requests.begin()
        var cancelled = false
        requests.attach(b) { cancelled = true }
        requests.finish(a)
        requests.invalidate()
        assertTrue(cancelled)
    }
}
