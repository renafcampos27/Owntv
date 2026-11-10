package tv.own.owntv.features.live

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class LocalTimeshiftFailureTest {
    @Test fun releaseAndNotificationWaitForProducerClosure() = runBlocking {
        val stopped = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val job = launch {
            finishLocalTimeshiftFailure({ true }, { stopped.await(); events.add("stopped") },
                { events.add("released") }, { events.add("notified") })
        }
        yield()
        assertTrue(events.isEmpty())
        stopped.complete(Unit); job.join()
        assertEquals(listOf("stopped", "released", "notified"), events)
    }

    @Test fun ownerChangedDuringClosureCannotReleaseNewClaimOrNotify() = runBlocking {
        var current = true
        finishLocalTimeshiftFailure({ current }, { current = false },
            { fail("stale claim release") }, { fail("stale notification") })
    }

    @Test fun alreadyStaleFailureCannotStopANewCapture() = runBlocking {
        finishLocalTimeshiftFailure({ false }, { fail("stale stop") },
            { fail("stale release") }, { fail("stale notification") })
    }

    @Test fun cancellationCannotAnnounceSuccessfulClosure() = runBlocking {
        val result = runCatching {
            finishLocalTimeshiftFailure({ true }, { throw CancellationException() },
                { fail("premature release") }, { fail("premature notification") })
        }
        assertTrue(result.exceptionOrNull() is CancellationException)
    }
}
