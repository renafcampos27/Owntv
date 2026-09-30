package tv.own.owntv.features.live

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity

class NowPlayingTitleLoaderTest {
    private fun channel(id: Long) = ChannelEntity(
        id = id, sourceId = 1, categoryId = 1, name = "Channel $id", streamUrl = "https://example.test/$id",
    )

    @Test fun `cancelled old read cannot overwrite a newer page even if its backend finishes late`() = runBlocking {
        withTimeout(2_000) {
            val oldStarted = CompletableDeferred<Unit>()
            val releaseOld = CompletableDeferred<Unit>()
            val newPublished = CompletableDeferred<Unit>()
            val published = mutableListOf<Map<Long, String>>()
            var calls = 0
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = {
                if (++calls == 1) {
                    oldStarted.complete(Unit)
                    withContext(NonCancellable) { releaseOld.await() }
                    mapOf(1L to "old")
                } else mapOf(1L to "new", 2L to "second")
            }, publish = {
                published += it
                if (it[1L] == "new") newPublished.complete(Unit)
            }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            oldStarted.await()
            loader.ensure(listOf(channel(1), channel(2)))
            newPublished.await()
            releaseOld.complete(Unit)
            yield()
            assertEquals(mapOf(1L to "new", 2L to "second"), published.last())
            assertFalse(published.any { it[1L] == "old" })
        }
    }

    @Test fun `a failed query is retried instead of cached as no guide`() = runBlocking {
        withTimeout(2_000) {
            val published = mutableListOf<Map<Long, String>>()
            val success = CompletableDeferred<Unit>()
            var calls = 0
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = {
                if (++calls == 1) throw IllegalStateException("temporary read failure")
                mapOf(1L to "programme")
            }, publish = { published += it; success.complete(Unit) }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            yield()
            assertTrue(published.isEmpty())
            loader.ensure(listOf(channel(1)))
            success.await()
            assertEquals(2, calls)
            assertEquals("programme", published.last()[1L])
        }
    }

    @Test fun `query cancellation never publishes absence and a subsequent request can retry`() = runBlocking {
        withTimeout(2_000) {
            var calls = 0
            val published = mutableListOf<Map<Long, String>>()
            val success = CompletableDeferred<Unit>()
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = {
                if (++calls == 1) throw CancellationException("query cancelled")
                mapOf(1L to "programme")
            }, publish = { published += it; success.complete(Unit) }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            yield()
            assertTrue(published.isEmpty())
            loader.ensure(listOf(channel(1)))
            success.await()
            assertEquals(2, calls)
            assertEquals("programme", published.last()[1L])
        }
    }

    @Test fun `failed minute refresh preserves the last successful title`() = runBlocking {
        withTimeout(2_000) {
            val published = mutableListOf<Map<Long, String>>()
            val initial = CompletableDeferred<Unit>()
            var calls = 0
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = {
                if (++calls == 1) mapOf(1L to "programme") else throw IllegalStateException("read failure")
            }, publish = { published += it; initial.complete(Unit) }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            initial.await()
            loader.refresh(listOf(channel(1)))
            yield()
            assertEquals(2, calls)
            assertEquals(listOf(mapOf(1L to "programme")), published)
        }
    }

    @Test fun `a successful empty result avoids querying missing guide on every append`() = runBlocking {
        withTimeout(2_000) {
            var calls = 0
            val first = CompletableDeferred<Unit>()
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20,
                load = { calls++; emptyMap() }, publish = { first.complete(Unit) }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            first.await()
            loader.ensure(listOf(channel(1)))
            yield()
            assertEquals(1, calls)
        }
    }

    @Test fun `offset invalidation cannot be repopulated by the old read`() = runBlocking {
        withTimeout(2_000) {
            val oldStarted = CompletableDeferred<Unit>()
            val releaseOld = CompletableDeferred<Unit>()
            val releaseNew = CompletableDeferred<Unit>()
            val newPublished = CompletableDeferred<Unit>()
            val published = mutableListOf<Map<Long, String>>()
            var calls = 0
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = {
                if (++calls == 1) {
                    oldStarted.complete(Unit)
                    withContext(NonCancellable) { releaseOld.await() }
                    mapOf(1L to "old offset")
                } else {
                    releaseNew.await()
                    mapOf(1L to "new offset")
                }
            }, publish = {
                published += it
                if (it[1L] == "new offset") newPublished.complete(Unit)
            }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1)))
            oldStarted.await()
            loader.clear()
            assertEquals(emptyMap<Long, String>(), published.last())
            releaseOld.complete(Unit)
            yield()
            assertFalse(published.any { it[1L] == "old offset" })
            releaseNew.complete(Unit)
            newPublished.await()
            assertEquals("new offset", published.last()[1L])
        }
    }

    @Test fun `one channel invalidation preserves other titles and queries the updated entity`() = runBlocking {
        withTimeout(2_000) {
            val published = mutableListOf<Map<Long, String>>()
            val initial = CompletableDeferred<Unit>()
            val refreshed = CompletableDeferred<Unit>()
            var calls = 0
            val updated = channel(1).copy(name = "updated entity")
            val loader = NowPlayingTitleLoader(CoroutineScope(coroutineContext), 20, load = { channels ->
                if (++calls == 1) mapOf(1L to "old", 2L to "other")
                else {
                    assertEquals(listOf(updated), channels)
                    mapOf(1L to "updated")
                }
            }, publish = {
                published += it
                if (it[1L] == "old") initial.complete(Unit)
                if (it[1L] == "updated") refreshed.complete(Unit)
            }, ensureDelayMs = 0)
            loader.ensure(listOf(channel(1), channel(2)))
            initial.await()
            loader.invalidate(updated)
            assertEquals(mapOf(2L to "other"), published.last())
            refreshed.await()
            assertEquals(mapOf(1L to "updated", 2L to "other"), published.last())
        }
    }
}
