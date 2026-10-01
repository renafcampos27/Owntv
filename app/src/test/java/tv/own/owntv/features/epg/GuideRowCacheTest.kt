package tv.own.owntv.features.epg

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GuideRowCacheTest {
    @Test fun concurrentQueriesHaveAnExplicitBudget() = runBlocking {
        val cache = GuideRowCache<Int, Int>(this, 10, 20)
        val fourStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var active = 0
        var peak = 0
        val requests = (0..7).map { key ->
            async {
                cache.get(key) {
                    active++
                    peak = maxOf(peak, active)
                    if (active == 4) fourStarted.complete(Unit)
                    try { release.await(); listOf(key) } finally { active-- }
                }
            }
        }
        fourStarted.await()
        release.complete(Unit)
        requests.awaitAll()
        assertEquals(4, peak)
        assertEquals(0, active)
    }
    @Test fun simultaneousWaitersShareOneRead() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var calls = 0
            val first = async { cache.get("a") { calls++; started.complete(Unit); release.await(); listOf(1) } }
            started.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { cache.get("a") { calls++; listOf(2) } }
            release.complete(Unit)
            assertEquals(listOf(1), first.await())
            assertEquals(listOf(1), second.await())
            assertEquals(1, calls)
        } finally { scope.cancel() }
    }

    @Test fun leavingOneRowDoesNotCancelAnotherWaiter() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async { cache.get("a") { started.complete(Unit); release.await(); listOf(7) } }
            started.await()
            first.cancelAndJoin()
            val second = async { cache.get("a") { error("duplicate") } }
            release.complete(Unit)
            assertEquals(listOf(7), second.await())
        } finally { scope.cancel() }
    }

    @Test fun lateReadCannotRepopulateNewGeneration() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val old = async {
                cache.get("a") { withContext(NonCancellable) { started.complete(Unit); release.await(); listOf(1) } }
            }
            started.await()
            cache.invalidate()
            assertEquals(listOf(2), cache.get("a") { listOf(2) })
            release.complete(Unit)
            try { old.await(); fail("Old generation returned") } catch (_: CancellationException) { }
            assertEquals(listOf(2), cache.peek("a"))
        } finally { scope.cancel() }
    }

    @Test fun unchangedCountStillReplacesContentsAfterInvalidation() = runBlocking {
        val cache = GuideRowCache<String, Int>(this, 4, 10)
        cache.get("a") { listOf(1, 2) }
        cache.invalidate()
        assertEquals(listOf(3, 4), cache.get("a") { listOf(3, 4) })
    }

    @Test fun programmeBudgetEvictsLeastRecentlyUsedRows() = runBlocking {
        val cache = GuideRowCache<String, Int>(this, 10, 5)
        cache.get("a") { listOf(1, 2) }
        cache.get("b") { listOf(3, 4) }
        cache.peek("a")
        cache.get("c") { listOf(5, 6) }
        assertNull(cache.peek("b"))
        assertNotNull(cache.peek("a"))
        assertEquals(4, cache.itemCount())
    }

    @Test fun oversizedRowDoesNotDefeatBudget() = runBlocking {
        val cache = GuideRowCache<String, Int>(this, 2, 3)
        assertEquals(4, cache.get("a") { listOf(1, 2, 3, 4) }.size)
        assertEquals(0, cache.itemCount())
        assertNull(cache.peek("a"))
    }

    @Test fun emptyRowsAreAlsoBoundedAndFailedReadCanRetry() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 2, 10)
            try { cache.get("a") { error("read failed") }; fail() } catch (_: IllegalStateException) { }
            cache.get("a") { emptyList() }
            cache.get("b") { emptyList() }
            cache.get("c") { emptyList() }
            assertEquals(2, cache.size())
            assertNull(cache.peek("a"))
        } finally { scope.cancel() }
    }
}
