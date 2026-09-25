package tv.own.owntv.features.live

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TuneSelectionTest {
    @Test fun slowLookupCannotOpenAfterNewerChoiceEvenIfItIgnoresCancellation() = runBlocking {
        val selection = TuneSelection()
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
        val selection = TuneSelection()
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
        val selection = TuneSelection()
        val ready = List(3) { CompletableDeferred<Unit>() }
        val opened = mutableListOf<Int>()
        val jobs = (0..2).map { channel ->
            val id = selection.next()
            launch(start = CoroutineStart.UNDISPATCHED) {
                ready[channel].await()
                selection.check(id)
                opened += channel
            }
        }
        for (channel in listOf(2, 0, 1)) { ready[channel].complete(Unit); jobs[channel].join() }
        assertEquals(listOf(2), opened)
    }

    @Test fun exitInvalidatesUncancelledLookupAndRepeatedChoiceGetsNewId() = runBlocking {
        val selection = TuneSelection()
        val id = selection.next()
        selection.next() // exit
        assertFalse(selection.owns(id))
        assertThrows(CancellationException::class.java) { selection.requireCurrent(id) }
        assertTrue(selection.next() > id)
    }
}
