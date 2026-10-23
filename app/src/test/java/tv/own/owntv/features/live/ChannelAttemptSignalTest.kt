package tv.own.owntv.features.live

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ChannelAttemptSignalTest {
    @Test fun synchronousSuccessBeforeAwaitIsNotLost() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        signal.complete(1, 7, 42, true); assertTrue(ticket.outcome.await())
    }
    @Test fun staleChoiceSourceOrChannelCannotSettleTheCurrentAttempt() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(2, 7, 42)
        signal.complete(1, 7, 42, true); signal.complete(2, 8, 42, true); signal.complete(2, 7, 43, false)
        assertFalse(ticket.outcome.isCompleted)
        signal.complete(2, 7, 42, false); assertFalse(ticket.outcome.await())
    }
    @Test fun newAttemptCancelsOldAndOldFinallyCannotCancelNew() = runBlocking {
        val signal = ChannelAttemptSignal(); val old = signal.begin(1, 7, 42); val next = signal.begin(2, 7, 42)
        assertTrue(old.outcome.isCancelled); signal.clear(old); assertFalse(next.outcome.isCompleted)
        signal.complete(2, 7, 42, true); assertTrue(next.outcome.await())
    }
    @Test fun cancellationIsDistinctFromAChannelFailure() {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        signal.cancel(); assertTrue(ticket.outcome.isCancelled)
        signal.complete(1, 7, 42, true); assertTrue(ticket.outcome.isCancelled)
    }
    @Test fun immediateOutcomeDoesNotStartADiagnosticJobOrLoseSuccess() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        assertTrue(awaitChannelAttempt(ticket, start = { signal.complete(1, 7, 42, true) },
            observeErrors = { error("already settled") }))
    }
    @Test fun observerBelongsToAttemptAndCannotReplaceTheTuneOwner() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        val observing = CompletableDeferred<Unit>(); val stopped = CompletableDeferred<Unit>()
        val attempt = async {
            awaitChannelAttempt(ticket, start = {}, observeErrors = {
                observing.complete(Unit)
                try { awaitCancellation() } finally { stopped.complete(Unit) }
            })
        }
        observing.await(); assertTrue(attempt.isActive)
        signal.complete(1, 7, 42, true); assertTrue(attempt.await()); stopped.await()
        assertTrue(currentCoroutineContext().isActive)
    }
    @Test fun ownerCancellationAlsoCancelsItsObserver() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        val observing = CompletableDeferred<Unit>(); val stopped = CompletableDeferred<Unit>()
        val attempt = launch {
            awaitChannelAttempt(ticket, start = {}, observeErrors = {
                observing.complete(Unit)
                try { awaitCancellation() } finally { stopped.complete(Unit) }
            })
        }
        observing.await(); attempt.cancelAndJoin(); stopped.await()
    }

    @Test fun aCancelledTicketCannotStartANewConnection() = runBlocking {
        val signal = ChannelAttemptSignal(); val ticket = signal.begin(1, 7, 42)
        signal.cancel()
        try { awaitChannelAttempt(ticket, start = { error("stale start") }, observeErrors = { error("stale observer") }); fail("Expected cancellation") }
        catch (_: CancellationException) { }
    }

}
