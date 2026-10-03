package tv.own.owntv.ui.components

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DialogFocusRestoreTest {
    @Test fun ongoingDragBeforeRestoreNeverGetsScrolledBack() = runBlocking {
        val guard = FocusRequestGuard()
        guard.beginPointer()
        val revision = guard.revision
        var scrolls = 0; var requests = 0
        restoreDialogFocus(true, { requests++; true }, { scrolls++ },
            { guard.revision == revision && !guard.pointerActive }, {})
        assertEquals(0, scrolls); assertEquals(0, requests)
        guard.endPointer()
        assertFalse(guard.pointerActive)
    }
    @Test fun inputDuringLayoutPreventsScrollAndFocus() = runBlocking {
        val guard = FocusRequestGuard()
        val revision = guard.revision
        var scrolls = 0; var requests = 0
        restoreDialogFocus(true, { requests++; true }, { scrolls++ },
            { guard.revision == revision }, { guard.invalidate() })
        assertEquals(0, scrolls); assertEquals(0, requests)
    }
    @Test fun inputDuringScrollPreventsFocus() = runBlocking {
        val guard = FocusRequestGuard()
        val revision = guard.revision
        var requests = 0
        restoreDialogFocus(true, { requests++; true }, { guard.invalidate() },
            { guard.revision == revision }, {})
        assertEquals(0, requests)
    }
    @Test fun cancellationFromScrollPropagatesWithoutFocus() = runBlocking {
        var requests = 0
        try {
            restoreDialogFocus(true, { requests++; true }, { throw CancellationException("cancel") }, { true }, {})
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(0, requests)
    }
    @Test fun missingTargetStopsAtBoundAndNeverTreatsFalseAsSuccess() = runBlocking {
        var requests = 0
        restoreDialogFocus(true, { requests++; false }, {}, { true }, {}, attempts = 5)
        assertEquals(5, requests)
    }
}
