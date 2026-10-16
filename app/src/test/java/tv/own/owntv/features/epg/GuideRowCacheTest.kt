package tv.own.owntv.features.epg

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GuideRowCacheTest {
    @Test fun deactivationCancelsPendingReadsButKeepsCompletedRowsAndAllowsReentry() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            cache.get("ready") { listOf(7) }
            val started = CompletableDeferred<Unit>()
            val pending = async { cache.get("pending") { started.complete(Unit); awaitCancellation() } }
            started.await()
            cache.cancelPendingOutside { false }
            try { pending.await(); fail("Inactive read completed") } catch (_: CancellationException) { }
            assertEquals(listOf(7), cache.peek("ready"))
            assertNull(cache.peek("pending"))
            assertEquals(listOf(8), cache.get("pending") { listOf(8) })
        } finally { scope.cancel() }
    }

    @Test fun obsoleteReadCannotRemoveReplacementAfterQuickReturn() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            cache.get("completed") { listOf(8) }
            val started = CompletableDeferred<Unit>()
            val oldRelease = CompletableDeferred<Unit>()
            val old = async {
                cache.get("old") { withContext(NonCancellable) { started.complete(Unit); oldRelease.await(); listOf(1) } }
            }
            started.await()
            cache.cancelPendingOutside { it != "old" }
            val newStarted = CompletableDeferred<Unit>()
            val newRelease = CompletableDeferred<Unit>()
            val replacement = async { cache.get("old") { newStarted.complete(Unit); newRelease.await(); listOf(2) } }
            newStarted.await()
            oldRelease.complete(Unit)
            try { old.await(); fail("Obsolete read returned") } catch (_: CancellationException) { }
            val shared = async(start = CoroutineStart.UNDISPATCHED) { cache.get("old") { error("Replacement lost") } }
            newRelease.complete(Unit)
            assertEquals(listOf(2), replacement.await())
            assertEquals(listOf(2), shared.await())
            assertEquals(listOf(8), cache.peek("completed"))
        } finally { scope.cancel() }
    }

    @Test fun adjacentTimeBlocksRemainReusableWhileOldBlockIsLoading() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<Pair<Long, Long>, Int>(scope, 4, 10)
            val older = 0L to 6L
            val newer = 6L to 12L
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val oldRead = async { cache.get(older) { started.complete(Unit); release.await(); listOf(1) } }
            started.await()
            assertEquals(listOf(2), cache.get(newer) { listOf(2) })
            release.complete(Unit)
            assertEquals(listOf(1), oldRead.await())
            assertEquals(listOf(1), cache.get(older) { error("Duplicate history fetch") })
            assertEquals(listOf(2), cache.peek(newer))
        } finally { scope.cancel() }
    }

    @Test fun unavailableFallbackIsNotCachedAndNextReadCanRecover() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val cache = GuideRowCache<String, Int>(scope, 4, 10)
            var calls = 0
            assertEquals(listOf(7), cache.getOrFallback("a", { calls++; error("provider unavailable") }) { listOf(7) })
            assertNull(cache.peek("a"))
            assertEquals(listOf(8), cache.getOrFallback("a", { calls++; listOf(8) }) { emptyList() })
            assertEquals(listOf(8), cache.peek("a"))
            assertEquals(2, calls)
        } finally { scope.cancel() }
    }

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
