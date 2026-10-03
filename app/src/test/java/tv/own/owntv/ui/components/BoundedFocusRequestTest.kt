package tv.own.owntv.ui.components

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BoundedFocusRequestTest {
    @Test fun userInputBeforeContentLoadsPreventsInitialFocus() = runBlocking {
        val guard = FocusRequestGuard()
        guard.invalidate()
        var calls = 0
        assertFalse(requestBoundedFocus({ calls++; true }, { guard.revision == 0L }, {}))
        assertEquals(0, calls)
    }
    @Test fun falseIsRetriedUntilAccepted() = runBlocking {
        var calls = 0
        assertTrue(requestBoundedFocus({ ++calls == 3 }, { true }, {}))
        assertEquals(3, calls)
    }
    @Test fun userInputDuringLayoutCancelsRequest() = runBlocking {
        val guard = FocusRequestGuard()
        val revision = guard.revision
        var calls = 0
        assertFalse(requestBoundedFocus({ calls++; true }, { guard.revision == revision }, { guard.invalidate() }))
        assertEquals(0, calls)
    }
    @Test fun missingTargetHasBoundedAttempts() = runBlocking {
        var calls = 0
        assertFalse(requestBoundedFocus({ calls++; throw IllegalStateException("not attached") }, { true }, {}, 4))
        assertEquals(4, calls)
    }
}
