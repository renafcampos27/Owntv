package tv.own.owntv.features.live

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CatchupRequestsTest {
    @Test fun emptyGuideAndReadFailureAreDifferentResults() = runBlocking {
        assertEquals(CatchupListResult.Ready(emptyList()), loadCatchupList { emptyList() })
        assertEquals(CatchupListResult.Error, loadCatchupList { throw IOException("unavailable") })
    }

    @Test fun cancellationIsNotAnEmptyGuideOrAnError() = runBlocking {
        val job = launch {
            loadCatchupList { throw CancellationException("popup closed") }
            fail("cancelled result was published")
        }
        job.join()
        assertTrue(job.isCancelled)
    }

    @Test fun lateLoaderIgnoringCancellationCannotPublish() = runBlocking {
        val ready = CompletableDeferred<Unit>()
        var published = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            loadCatchupList { withContext(NonCancellable) { ready.await() }; emptyList() }
            published = true
        }
        job.cancel()
        ready.complete(Unit)
        job.join()
        assertFalse(published)
    }

    @Test fun sameChannelInNewGenerationRejectsOldModal() {
        val selection = TuneSelection()
        val owner = CatchupOwner(1, selection.next(), 10, 7)
        assertTrue(owner.accepts(1, selection.current, listOf(7)))
        selection.next() // B
        selection.next() // A again
        assertFalse(owner.accepts(1, selection.current, listOf(7)))
        assertEquals(selection.current, selection.revision.value)
    }

    @Test fun differentProfileOrRemovedSourceRejectsConfirmation() {
        val owner = CatchupOwner(1, 3, 10, 7)
        assertFalse(owner.accepts(2, 3, listOf(7)))
        assertFalse(owner.accepts(1, 3, listOf(8)))
        assertTrue(owner.accepts(1, 3, listOf(7, 8)))
    }
}
