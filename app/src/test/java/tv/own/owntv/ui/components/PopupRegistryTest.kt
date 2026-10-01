package tv.own.owntv.ui.components

import org.junit.Assert.*
import org.junit.Test

class PopupRegistryTest {
    @Test fun automaticNoticeWaitsUntilEveryUserWindowCloses() {
        val registry = PopupRegistry()
        val first = Any()
        val nested = Any()
        registry.enter(first)
        registry.enter(nested)
        registry.leave(first)
        assertEquals(setOf(nested), registry.active.value)
        registry.leave(nested)
        assertTrue(registry.active.value.isEmpty())
    }
    @Test fun repeatedRegistrationOrDisposalDoesNotCorruptPresence() {
        val registry = PopupRegistry()
        val owner = Any()
        registry.enter(owner)
        registry.enter(owner)
        assertEquals(1, registry.active.value.size)
        registry.leave(owner)
        registry.leave(owner)
        assertTrue(registry.active.value.isEmpty())
    }
}
