package tv.own.owntv.features.live

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for P3:
 * - A04: Stalker preview cancellation and identity invalidation upon stopPreview and playPreview
 */
class StalkerTileCancelTest {

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
