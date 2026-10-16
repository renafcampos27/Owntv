package tv.own.owntv.features.recordings

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RecordingOpenSelectionTest {
    @Test fun slowLookupCannotOpenAfterNewerChoiceEvenIfItIgnoresCancellation() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val opened = mutableListOf<String>()
        val slowLookup = CompletableDeferred<Unit>()
        val a = selection.next()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { slowLookup.await() }
            selection.check(a)
            opened += "A"
        }
        job.cancel()
        val b = selection.next()
        selection.check(b); opened += "B"
        slowLookup.complete(Unit)
        job.join()
        assertEquals(listOf("B"), opened)
    }

    @Test fun aToBToARejectsFirstAWithoutDependingOnCancellation() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val oldLookup = CompletableDeferred<Unit>()
        val opened = mutableListOf<Long>()
        val firstA = selection.next()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            oldLookup.await()
            selection.check(firstA)
            opened += firstA
        }
        selection.next() // B
        val secondA = selection.next()
        selection.check(secondA); opened += secondA
        oldLookup.complete(Unit); job.join()
        assertTrue(job.isCancelled)
        assertEquals(listOf(secondA), opened)
    }

    @Test fun aToBToCOnlyLastLookupMayPrepare() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val ready = List(3) { CompletableDeferred<Unit>() }
        val opened = mutableListOf<Int>()
        val jobs = (0..2).map { rec ->
            val id = selection.next()
            launch(start = CoroutineStart.UNDISPATCHED) {
                ready[rec].await()
                selection.check(id)
                opened += rec
            }
        }
        for (rec in listOf(2, 0, 1)) { ready[rec].complete(Unit); jobs[rec].join() }
        assertEquals(listOf(2), opened)
    }

    @Test fun exitInvalidatesUncancelledLookupAndRepeatedChoiceGetsNewId() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val id = selection.next()
        selection.invalidate() // exit / invalidate
        assertFalse(selection.owns(id))
        assertThrows(CancellationException::class.java) { selection.requireCurrent(id) }
        assertTrue(selection.next() > id)
    }

    @Test fun stoppedScreenRejectsOpeningUntilStarted() {
        val selection = RecordingOpenSelection()
        val stoppedToken = selection.next()
        assertFalse(selection.canOpen)
        assertFalse(selection.owns(stoppedToken))
        selection.setForeground(true)
        val foregroundToken = selection.next()
        assertTrue(selection.canOpen)
        assertTrue(selection.owns(foregroundToken))
        selection.setForeground(false)
        assertFalse(selection.owns(foregroundToken))
    }

    @Test fun homeRejectsLatePublicationAndClosesItsLeaseWithoutJobCancellation() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val prepared = CompletableDeferred<Unit>()
        val token = selection.next()
        var transferredToPlayer = false
        var leaseClosed = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                prepared.await()
                selection.check(token)
                transferredToPlayer = true
            } finally {
                if (!transferredToPlayer) leaseClosed = true
            }
        }
        selection.setForeground(false) // ON_STOP; even an uncancelled IO result is stale.
        prepared.complete(Unit)
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(transferredToPlayer)
        assertTrue(leaseClosed)
    }

    @Test fun homeThenReturnCannotReviveOldOpeningButAllowsFreshSelection() = runBlocking {
        val selection = RecordingOpenSelection().apply { setForeground(true) }
        val oldToken = selection.next()
        selection.setForeground(false)
        selection.setForeground(true)
        assertFalse(selection.owns(oldToken))
        assertThrows(CancellationException::class.java) { selection.requireCurrent(oldToken) }
        val freshToken = selection.next()
        selection.check(freshToken)
        assertTrue(selection.owns(freshToken))
    }
}
