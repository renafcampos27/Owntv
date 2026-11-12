package tv.own.owntv.ui.images

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class ChannelLogoRepositoryTest {
    private val url = "https://example.invalid/logo.png"
    private val key = ChannelLogoDiskCache.key(url)
    private fun disk() = ChannelLogoDiskCache(Files.createTempDirectory("logo-repository").toFile())
    private fun response(chain: okhttp3.Interceptor.Chain, code: Int = 200, bytes: ByteArray = byteArrayOf(1)) =
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
            .body(bytes.toResponseBody()).build()

    @Test fun scrollingAndRestartReuseTheLocalCopyWithoutNewHttpRequests() = runBlocking {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); response(it) }.build()
        val cache = disk()
        val repository = ChannelLogoRepository(cache, client, { _, _ -> webpFixture() })
        repository.refresh(url, 100)
        repeat(10) { repository.refresh(url, 0); repository.refresh(url, 100) }
        ChannelLogoRepository(ChannelLogoDiskCache(cache.root), client, { _, _ -> webpFixture() }).refresh(url, 100)
        assertEquals(1, calls.get()); assertNotNull(cache.read(key).asset)
    }

    @Test fun listUpdateRevalidatesAnd304KeepsTheExistingThumbnail() = runBlocking {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor {
            if (calls.getAndIncrement() == 0) response(it).newBuilder().header("ETag", "logo-v1").build()
            else {
                assertEquals("logo-v1", it.request().header("If-None-Match"))
                response(it, 304)
            }
        }.build()
        val cache = disk(); val repository = ChannelLogoRepository(cache, client, { _, _ -> webpFixture() })
        repository.refresh(url, 100)
        val original = cache.read(key).asset
        repository.refresh(url, 200)
        assertEquals(2, calls.get()); assertEquals(original, cache.read(key).asset)
        assertEquals(200L, cache.read(key).generation)
    }

    @Test fun missingOrBrokenProviderImageCannotEraseAGoodLogo() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 100, 1, null, null)
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); response(it, 404) }.build()
        val repository = ChannelLogoRepository(cache, client, { _, _ -> error("must not decode an HTTP error") })
        repository.refresh(url, 200); repository.refresh(url, 200)
        assertEquals(1, calls.get()); assertEquals(original, cache.read(key).asset)
    }

    @Test fun decodeFailureDoesNotPublishOrRemoveThePreviousThumbnail() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 100, 1, null, null)
        val client = OkHttpClient.Builder().addInterceptor { response(it) }.build()
        ChannelLogoRepository(cache, client, { _, _ -> error("bad provider body") }).refresh(url, 200)
        assertEquals(original, cache.read(key).asset); assertEquals(100L, cache.read(key).generation)
    }

    @Test fun concurrentRequestsForOneLogoAreDeduplicated() = runBlocking {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); response(it) }.build()
        val repository = ChannelLogoRepository(disk(), client, { _, _ -> webpFixture() })
        coroutineScope { (0..20).map { async { repository.refresh(url, 100) } }.awaitAll() }
        assertEquals(1, calls.get())
    }

    @Test fun parallelDownloadsNeverExceedTwoAndHonourPlaylistUserAgent() = runBlocking {
        val running = AtomicInteger(); val peak = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor {
            assertEquals("custom-list-agent", it.request().header("User-Agent"))
            val n = running.incrementAndGet(); peak.accumulateAndGet(n, Math::max)
            try { Thread.sleep(20); response(it) } finally { running.decrementAndGet() }
        }.build()
        val repository = ChannelLogoRepository(disk(), client, { _, _ -> webpFixture() })
        coroutineScope { (0..9).map { async { repository.refresh("$url?channel=$it", 100, "custom-list-agent") } }.awaitAll() }
        assertTrue(peak.get() in 1..2); assertEquals(0, running.get())
    }

    @Test fun oversizedResponseIsCancelledBeforeDecodeAndKeepsTheOldThumbnail() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 1, 1, null, null)
        var call: okhttp3.Call? = null
        val client = OkHttpClient.Builder().addInterceptor {
            call = it.call(); response(it, bytes = ByteArray(ChannelLogoRepository.MAX_DOWNLOAD_BYTES + 1))
        }.build()
        ChannelLogoRepository(cache, client, { _, _ -> error("oversized decode") }).refresh(url, 2)
        assertTrue(call!!.isCanceled()); assertEquals(original, cache.read(key).asset)
    }

    @Test fun oldLogoIsVisibleWhileASlowListRefreshIsStillDownloading() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 1, 1, null, null)
        val started = CompletableDeferred<Unit>(); val release = java.util.concurrent.CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor {
            started.complete(Unit)
            check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
            response(it)
        }.build()
        val repository = ChannelLogoRepository(cache, client, { _, _ -> webpFixture(2) })
        val updating = launch { repository.refresh(url, 2) }
        try {
            withTimeout(3_000) { started.await() }
            assertEquals(original, withTimeout(1_000) { repository.observe(url).first { it != null } })
        } finally { release.countDown(); updating.join() }
    }

    @Test fun providerRetryAfterSurvivesRestartAndIsNotBypassedByANewListGeneration() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 1, 1, null, null)
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor {
            calls.incrementAndGet(); response(it, 429).newBuilder().header("Retry-After", "3600").build()
        }.build()
        var time = 10_000L
        ChannelLogoRepository(cache, client, { _, _ -> webpFixture() }, { time }).refresh(url, 2)
        time += 600_000
        ChannelLogoRepository(ChannelLogoDiskCache(cache.root), client, { _, _ -> webpFixture() }, { time }).refresh(url, 3)
        assertEquals(1, calls.get()); assertEquals(original, cache.read(key).asset)
        time = 3_610_001
        ChannelLogoRepository(cache, client, { _, _ -> webpFixture() }, { time }).refresh(url, 3)
        assertEquals(2, calls.get())
    }

    @Test fun resumedBackgroundBatchDoesNotRestartOldFailedDownloads() = runBlocking {
        val calls = AtomicInteger(); var time = 1_000L
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); response(it, 404) }.build()
        val repository = ChannelLogoRepository(disk(), client, { _, _ -> webpFixture() }, { time })
        repository.refresh(url, 100, background = true)
        time += 20 * 60_000
        repository.refresh(url, 100, background = true)
        assertEquals(1, calls.get())
        repository.refresh(url, 101, background = true)
        assertEquals(2, calls.get())
    }

    @Test fun leavingARowCancelsItsQueuedDownloadWithoutCancellingThePlaylistWorker() = runBlocking {
        val arrived = java.util.concurrent.CountDownLatch(2); val release = java.util.concurrent.CountDownLatch(1)
        val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
        val client = OkHttpClient.Builder().addInterceptor {
            calls.add(it.request().url.encodedPath); arrived.countDown()
            check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
            response(it)
        }.build()
        val repository = ChannelLogoRepository(disk(), client, { _, _ -> webpFixture() })
        val workers = (1..2).map { launch { repository.refresh("https://example.invalid/worker-$it", 1) } }
        var row: Job? = null
        try {
            assertTrue(withContext(Dispatchers.IO) { arrived.await(3, java.util.concurrent.TimeUnit.SECONDS) })
            val subscribed = CompletableDeferred<Unit>()
            row = launch { repository.observe("https://example.invalid/offscreen").collect { subscribed.complete(Unit) } }
            withTimeout(3_000) { subscribed.await() }
            row.cancelAndJoin()
        } finally { row?.cancel(); release.countDown(); workers.joinAll() }
        assertEquals(2, calls.size); assertFalse(calls.contains("/offscreen"))
    }

    @Test fun upgradeRetriesMissingLegacyLogosOnceWithoutDiscardingGoodLegacyCopies() = runBlocking {
        val cache = disk()
        cache.checked(key, cache.read(key), 100, 1_000, false)
        val goodUrl = "$url?good=1"; val goodKey = ChannelLogoDiskCache.key(goodUrl)
        val good = cache.save(goodKey, webpFixture(), 100, 1_000, null, null)
        for (legacyKey in listOf(key, goodKey)) {
            val file = cache.root.resolve("$legacyKey.properties")
            val properties = java.util.Properties().apply { file.inputStream().use(::load); remove("policyVersion") }
            file.outputStream().use { properties.store(it, null) }
        }
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { calls.incrementAndGet(); response(it) }.build()
        val repository = ChannelLogoRepository(cache, client, { _, _ -> webpFixture() }, { 1_001 })
        repository.refresh(url, 100, background = true)
        repository.refresh(url, 100, background = true)
        repository.refresh(goodUrl, 100, background = true)
        assertEquals(1, calls.get()); assertNotNull(cache.read(key).asset)
        assertEquals(good, cache.read(goodKey).asset)
    }

    @Test fun largerOriginalIsStagedOnDiskAndRemovedAfterConversion() = runBlocking {
        val cache = disk(); val bytes = ByteArray(3 * 1024 * 1024) { 7 }
        val client = OkHttpClient.Builder().addInterceptor {
            response(it, bytes = bytes).newBuilder().header("Content-Type", "image/png").build()
        }.build()
        var staged: java.io.File? = null
        ChannelLogoRepository(cache, client, { file, _ ->
            staged = file; assertEquals(bytes.size.toLong(), file.length())
            assertEquals(7, file.inputStream().use { it.read() }); webpFixture()
        }).refresh(url, 100)
        assertNotNull(cache.read(key).asset); assertNotNull(staged); assertFalse(staged!!.exists())
        assertTrue(cache.root.listFiles().orEmpty().none { it.extension == "part" })
    }

    @Test fun failedOrCancelledConversionRemovesStagedOriginal() = runBlocking {
        for (cancel in listOf(false, true)) {
            val cache = disk(); val client = OkHttpClient.Builder().addInterceptor { response(it) }.build()
            val entered = CompletableDeferred<Unit>(); var staged: java.io.File? = null
            val repository = ChannelLogoRepository(cache, client, { file, _ ->
                staged = file; entered.complete(Unit)
                if (cancel) awaitCancellation() else error("broken image")
            })
            val job = launch { repository.refresh(url, 100) }
            withTimeout(3_000) { entered.await() }
            if (cancel) job.cancelAndJoin() else job.join()
            assertFalse(staged!!.exists()); assertNull(cache.read(key).asset)
            assertTrue(cache.root.listFiles().orEmpty().none { it.extension == "part" })
        }
    }

    @Test fun svgMimeHintIsForwardedFromResponseBodyOrFileExtension() = runBlocking {
        val types = listOf("image/svg+xml; charset=utf-8", "application/octet-stream", null)
        for (type in types) {
            val cache = disk()
            val client = OkHttpClient.Builder().addInterceptor {
                response(it).newBuilder().body(byteArrayOf(1).toResponseBody(type?.toMediaType())).build()
            }.build()
            var received: String? = null
            ChannelLogoRepository(cache, client, { _, mime -> received = mime; webpFixture() })
                .refresh("https://example.invalid/LOGO.SVG", 1)
            assertEquals(if (type?.startsWith("image/svg+xml") == true) type else "image/svg+xml", received)
        }
    }

    @Test fun oversizedUnknownLengthDownloadIsCancelledAndItsStagingFileIsRemoved() = runBlocking {
        val cache = disk(); val original = cache.save(key, webpFixture(), 1, 1, null, null)
        var call: okhttp3.Call? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            call = chain.call()
            val body = object : okhttp3.ResponseBody() {
                val content = okio.Buffer().write(ByteArray(ChannelLogoRepository.MAX_DOWNLOAD_BYTES + 1))
                override fun contentType(): okhttp3.MediaType? = null
                override fun contentLength(): Long = -1
                override fun source(): okio.BufferedSource = content
            }
            response(chain).newBuilder().body(body).build()
        }.build()
        ChannelLogoRepository(cache, client, { _, _ -> error("oversized decode") }).refresh(url, 2)
        assertTrue(call!!.isCanceled()); assertEquals(original, cache.read(key).asset)
        assertTrue(cache.root.listFiles().orEmpty().none { it.extension == "part" })
    }
}
