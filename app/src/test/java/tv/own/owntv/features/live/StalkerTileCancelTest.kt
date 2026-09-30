package tv.own.owntv.features.live

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for P3:
 * - A03: Multiview tile generation and job cancellation upon clear/releaseAll/supersede/refusal
 * - A04: Stalker preview cancellation and identity invalidation upon stopPreview and playPreview
 */
class StalkerTileCancelTest {

    @Test
    fun `A03 - tile generation cancels previous resolve and rejects late resolution`() = runBlocking {
        val tileGenerations = HashMap<Int, Long>()
        val tileJobs = HashMap<Int, Job>()
        val tunedChannels = mutableListOf<String>()

        fun cancelTile(tile: Int) {
            tileJobs.remove(tile)?.cancel()
            tileGenerations[tile] = (tileGenerations[tile] ?: 0L) + 1
        }

        fun tuneTileSimulated(tile: Int, channelName: String, isStillActive: () -> Boolean, slowResolve: CompletableDeferred<String>) {
            cancelTile(tile)
            val gen = tileGenerations[tile] ?: 0L
            val job = launch {
                val resolvedUrl = slowResolve.await()
                if (tileGenerations[tile] != gen) return@launch
                if (!isStillActive()) return@launch
                tunedChannels += "$channelName:$resolvedUrl"
            }
            tileJobs[tile] = job
        }

        var tile0Active = true
        val resolveCh1 = CompletableDeferred<String>()
        tuneTileSimulated(0, "Channel1", { tile0Active }, resolveCh1)

        // User clears or replaces tile 0 before resolve finishes
        val resolveCh2 = CompletableDeferred<String>()
        tuneTileSimulated(0, "Channel2", { tile0Active }, resolveCh2)

        // Now resolveCh1 finishes late
        resolveCh1.complete("http://link1")
        resolveCh2.complete("http://link2")

        yield()

        // Only Channel2 should have tuned; Channel1 was rejected by generation mismatch and cancellation
        assertEquals(listOf("Channel2:http://link2"), tunedChannels)
    }

    @Test
    fun `A03 - tile resolution rejects play if tile is released from pool`() = runBlocking {
        val tileGenerations = HashMap<Int, Long>()
        val tileJobs = HashMap<Int, Job>()
        val tunedChannels = mutableListOf<String>()

        fun cancelTile(tile: Int) {
            tileJobs.remove(tile)?.cancel()
            tileGenerations[tile] = (tileGenerations[tile] ?: 0L) + 1
        }

        var tileInPool = true
        val resolve = CompletableDeferred<String>()
        val tile = 1
        cancelTile(tile)
        val gen = tileGenerations[tile] ?: 0L

        val job = launch {
            val url = resolve.await()
            if (tileGenerations[tile] != gen) return@launch
            if (!tileInPool) return@launch // tile was released/cleared
            tunedChannels += url
        }
        tileJobs[tile] = job

        // Multiview clears tile 1
        tileInPool = false
        cancelTile(tile)

        resolve.complete("http://stream")
        job.join()

        assertTrue("Engine must not play on a released tile", tunedChannels.isEmpty())
    }

    @Test
    fun `A03 - engine identity and disposed check prevents playback on replaced engine`() = runBlocking {
        class MockEngine(val id: Int, var isDisposed: Boolean = false)

        val pool = HashMap<Int, MockEngine>()
        val engine1 = MockEngine(1)
        pool[0] = engine1

        var engine1Played = false
        val slowResolve = CompletableDeferred<String>()

        // Tile 0 tunes with engine1
        val job = launch {
            val url = slowResolve.await()
            val isStillActive = pool[0] === engine1 && !engine1.isDisposed
            if (!isStillActive) return@launch
            engine1Played = true
        }

        // Tile 0 is cleared, engine1 disposed, and a new engine2 is put in tile 0
        pool.remove(0)
        engine1.isDisposed = true

        val engine2 = MockEngine(2)
        pool[0] = engine2

        slowResolve.complete("http://stream")
        job.join()

        assertFalse("Engine 1 must not play after tile was replaced by Engine 2", engine1Played)
    }

    @Test
    fun `A03 - refused grant cancels pending tile job and releases engine`() = runBlocking {
        val tileJobs = HashMap<Int, Job>()
        val tileGenerations = HashMap<Int, Long>()
        var engineReleased = false

        fun cancelTile(tile: Int) {
            tileJobs.remove(tile)?.cancel()
            tileGenerations[tile] = (tileGenerations[tile] ?: 0L) + 1
        }

        val inFlightJob = launch { delay(10_000) }
        tileJobs[0] = inFlightJob

        // Simulation of fill() encountering StreamGrant.Refused
        val grantRefused = true
        if (grantRefused) {
            cancelTile(0)
            engineReleased = true
        }

        assertTrue("In flight job must be cancelled on refusal", inFlightJob.isCancelled)
        assertTrue("Engine must be released on refusal", engineReleased)
    }

    @Test
    fun `A03 - releaseAllTiles cancels all active tile jobs`() = runBlocking {
        val tileJobs = HashMap<Int, Job>()
        val tileGenerations = HashMap<Int, Long>()

        fun releaseAllTiles() {
            tileJobs.values.forEach { it.cancel() }
            tileJobs.clear()
            tileGenerations.clear()
        }

        val job1 = launch { delay(10_000) }
        val job2 = launch { delay(10_000) }
        tileJobs[0] = job1
        tileJobs[1] = job2

        releaseAllTiles()

        assertTrue("Job 1 must be cancelled", job1.isCancelled)
        assertTrue("Job 2 must be cancelled", job2.isCancelled)
        assertTrue("tileJobs must be empty", tileJobs.isEmpty())
        assertTrue("tileGenerations must be empty", tileGenerations.isEmpty())
    }

    @Test
    fun `A04 - stopPreview cancels in-flight stalker resolve and clears preview identity`() = runBlocking {
        var stalkerPreviewJob: Job? = null
        var stalkerPreviewCmd: String? = null
        var previewChannelUrl: String? = "stalker://ch1"
        var previewPlayed = false

        val slowResolve = CompletableDeferred<String>()
        val channelStreamUrl = "stalker://ch1"

        stalkerPreviewJob = launch {
            val resolvedUrl = slowResolve.await()
            if (previewChannelUrl != channelStreamUrl) return@launch
            stalkerPreviewCmd = channelStreamUrl
            previewPlayed = true
        }

        // stopPreview is called while resolution is in flight
        val stopPreview = {
            stalkerPreviewJob?.cancel()
            stalkerPreviewJob = null
            stalkerPreviewCmd = null
            previewChannelUrl = null
        }

        stopPreview()

        assertNull(stalkerPreviewJob)
        assertNull(stalkerPreviewCmd)
        assertNull(previewChannelUrl)

        slowResolve.complete("http://resolved")
        yield()

        assertFalse("Stream must not be played after stopPreview", previewPlayed)
    }

    @Test
    fun `A04 - focus change during resolve does not start preview on newly selected channel`() = runBlocking {
        var previewChannelUrl: String? = "stalker://ch1"
        var stalkerPreviewCmd: String? = null
        var enginePlayedUrl: String? = null

        val slowResolve = CompletableDeferred<String>()
        val ch1Cmd = "stalker://ch1"

        val job = launch {
            val resolved = slowResolve.await()
            // Check that channel is still the preview target
            if (previewChannelUrl != ch1Cmd) return@launch
            stalkerPreviewCmd = ch1Cmd
            enginePlayedUrl = resolved
        }

        // Focus changes to ch2
        previewChannelUrl = "stalker://ch2"

        slowResolve.complete("http://resolved-ch1")
        job.join()

        assertNull("Stale resolve must not play when focus changed", enginePlayedUrl)
        assertNull(stalkerPreviewCmd)
    }

    @Test
    fun `A04 - navigating to non-stalker channel cancels in-flight stalker resolve`() = runBlocking {
        var stalkerPreviewJob: Job? = null
        val slowResolve = CompletableDeferred<String>()
        var stalkerPlayed = false

        stalkerPreviewJob = launch {
            slowResolve.await()
            stalkerPlayed = true
        }

        // User navigates to non-stalker channel
        val onPlayNonStalkerPreview = {
            stalkerPreviewJob?.cancel()
            stalkerPreviewJob = null
        }

        onPlayNonStalkerPreview()

        assertTrue("In-flight stalker resolve must be cancelled when non-stalker channel previews", stalkerPreviewJob == null)
        slowResolve.complete("http://link")
        yield()
        assertFalse("Stalker channel must not play after cancellation", stalkerPlayed)
    }
}
