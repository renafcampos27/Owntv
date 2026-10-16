package tv.own.owntv.diagnostics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class UiWorkWindowTest {
    @Test fun `disabled and stale completions do not enter another session`() {
        val w = UiWorkWindow()
        w.record(0, UiWorkWindow.Operation.EPG_LAYOUT, 10, UiWorkWindow.Outcome.COMPLETED)
        val old = w.start()
        w.stop(old)
        val current = w.start()
        w.record(old, UiWorkWindow.Operation.EPG_LAYOUT, 10, UiWorkWindow.Outcome.COMPLETED)
        w.stop(old)
        assertEquals(current, w.epoch)
        assertTrue(w.take().isEmpty())
    }
    @Test fun `cancelled work is separate from errors and successful reads`() {
        val w = UiWorkWindow(); val owner = w.start()
        for (outcome in UiWorkWindow.Outcome.entries) {
            w.record(owner, UiWorkWindow.Operation.EPG_ROW_READ, 1_500_000, outcome)
        }
        val s = w.take().single()
        assertEquals(3L, s.calls); assertEquals(1L, s.completed)
        assertEquals(1L, s.cancelled); assertEquals(1L, s.failed)
        assertEquals(4_500_000L, s.totalNs); assertEquals(2, s.p95BucketMs)
        assertTrue(w.take().isEmpty())
    }
    @Test fun `a retired monitor cannot consume the new session report`() {
        val w = UiWorkWindow(); val old = w.start(); val current = w.start()
        w.record(current, UiWorkWindow.Operation.EPG_LAYOUT, 1, UiWorkWindow.Outcome.COMPLETED)
        assertTrue(w.take(old).isEmpty())
        assertEquals(1L, w.take(current).single().calls)
    }
    @Test fun `long reads keep exact maximum and bounded percentile bucket`() {
        val w = UiWorkWindow(); val owner = w.start()
        repeat(94) { w.record(owner, UiWorkWindow.Operation.EPG_PREFETCH_READ, 1, UiWorkWindow.Outcome.COMPLETED) }
        repeat(6) { w.record(owner, UiWorkWindow.Operation.EPG_PREFETCH_READ, 15_000_000_000L, UiWorkWindow.Outcome.COMPLETED) }
        val s = w.take().single()
        assertEquals(1000, s.p95BucketMs); assertEquals(15_000_000_000L, s.maxNs)
    }
    @Test fun `measurement returns values unchanged and rethrows failure`() {
        val owner = UiWorkDiagnostics.window.start()
        try {
            assertEquals(42, UiWorkDiagnostics.measure(UiWorkWindow.Operation.EPG_LAYOUT) { 42 })
            val failure = IllegalStateException()
            try { UiWorkDiagnostics.measure(UiWorkWindow.Operation.EPG_LAYOUT) { throw failure }; fail() }
            catch (actual: IllegalStateException) { assertSame(failure, actual) }
            val s = UiWorkDiagnostics.window.take().single()
            assertEquals(1L, s.failed); assertEquals(1L, s.completed)
        } finally { UiWorkDiagnostics.window.stop(owner) }
    }
    @Test fun `suspension and cancellation keep their original outcome`() = runBlocking {
        val owner = UiWorkDiagnostics.window.start()
        try {
            val cancelled = CancellationException()
            try {
                UiWorkDiagnostics.measure(UiWorkWindow.Operation.EPG_ROW_READ) { yield(); throw cancelled }
                fail()
            } catch (actual: CancellationException) { assertSame(cancelled, actual) }
            assertEquals(1L, UiWorkDiagnostics.window.take().single().cancelled)
        } finally { UiWorkDiagnostics.window.stop(owner) }
    }
    @Test fun `disabled probe executes without recording`() {
        UiWorkDiagnostics.window.stop(UiWorkDiagnostics.window.epoch)
        assertEquals(7, UiWorkDiagnostics.measure(UiWorkWindow.Operation.LIVE_VISIBLE_ROWS) { 7 })
        assertTrue(UiWorkDiagnostics.window.take().isEmpty())
    }
}
