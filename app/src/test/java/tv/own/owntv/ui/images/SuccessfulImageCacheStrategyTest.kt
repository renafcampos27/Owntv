package tv.own.owntv.ui.images

import android.app.Application
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.network.*
import coil3.request.CachePolicy
import coil3.request.Options
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Path.Companion.toOkioPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoilApi::class)
class SuccessfulImageCacheStrategyTest {
    @get:Rule val folder = TemporaryFolder()
    private val url = "https://image.invalid/channel.png"
    // A controlled NetworkClient fixture supplies responses; this avoids android.jar's main-thread
    // stub. Production keeps normal network policy. Real Coil writes/reads the actual disk cache.
    private val options = Options(Application(), networkCachePolicy = CachePolicy.DISABLED)
    private fun cache() = DiskCache.Builder().directory(folder.root.toOkioPath()).maxSizeBytes(1024 * 1024).build()
    private class Client(var status: Int) : NetworkClient {
        var requests = 0
        override suspend fun <T> executeRequest(request: NetworkRequest,
            block: suspend (NetworkResponse) -> T): T {
            requests++
            val body = if (status == 304) null else NetworkResponseBody(Buffer().writeUtf8("image-body"))
            return try { block(NetworkResponse(code = status,
                headers = NetworkHeaders.Builder().set("Content-Type", "image/png").build(), body = body)) }
            finally { body?.close() }
        }
    }
    private suspend fun fetch(cache: DiskCache, client: Client, strategy: CacheStrategy, imageUrl: String = url) {
        val result = NetworkFetcher(imageUrl, options, lazyOf(client), lazyOf(cache), lazyOf(strategy),
            lazyOf(ConnectivityChecker.ONLINE), lazyOf(ConcurrentRequestStrategy.UNCOORDINATED)).fetch()
        (result as SourceFetchResult).source.use { assertEquals("image-body", it.source().readUtf8()) }
    }
    @Test fun `temporary 404 never prevents later success and success survives loader restart`() = runBlocking {
        val client = Client(404)
        var disk = cache()
        try {
            try { fetch(disk, client, SuccessfulImageCacheStrategy); fail("Expected HTTP failure") }
            catch (expected: HttpException) { assertEquals(404, expected.response.code) }
            assertNull(disk.openSnapshot(url))
            client.status = 200
            fetch(disk, client, SuccessfulImageCacheStrategy)
            assertEquals(2, client.requests)
        } finally { disk.shutdown() }
        disk = cache()
        try {
            fetch(disk, client, SuccessfulImageCacheStrategy)
            assertEquals(2, client.requests)
        } finally { disk.shutdown() }
    }
    @Test fun `legacy cached failure is refreshed without deleting unrelated good images`() = runBlocking {
        val client = Client(404)
        var disk = cache()
        try {
            try { fetch(disk, client, CacheStrategy.DEFAULT); fail("Expected HTTP failure") }
            catch (expected: HttpException) { assertEquals(404, expected.response.code) }
            assertNotNull(disk.openSnapshot(url)?.also { it.close() })
            client.status = 200
            fetch(disk, client, CacheStrategy.DEFAULT, "https://image.invalid/unrelated.png")
        } finally { disk.shutdown() }
        disk = cache()
        try {
            client.status = 200
            fetch(disk, client, SuccessfulImageCacheStrategy)
            fetch(disk, client, SuccessfulImageCacheStrategy)
            fetch(disk, client, SuccessfulImageCacheStrategy, "https://image.invalid/unrelated.png")
            assertEquals(3, client.requests)
        } finally { disk.shutdown() }
    }
    @Test fun `revalidated successful body survives restart without another network request`() = runBlocking {
        val client = Client(200)
        var disk = cache()
        try {
            fetch(disk, client, SuccessfulImageCacheStrategy)
            client.status = 304
            val forceRevalidation = object : CacheStrategy {
                override suspend fun read(cacheResponse: NetworkResponse, networkRequest: NetworkRequest,
                    options: Options) = CacheStrategy.ReadResult(networkRequest)
                override suspend fun write(cacheResponse: NetworkResponse?, networkRequest: NetworkRequest,
                    networkResponse: NetworkResponse, options: Options) =
                    SuccessfulImageCacheStrategy.write(cacheResponse, networkRequest, networkResponse, options)
            }
            fetch(disk, client, forceRevalidation)
            assertEquals(2, client.requests)
        } finally { disk.shutdown() }
        disk = cache()
        try {
            fetch(disk, client, SuccessfulImageCacheStrategy)
            assertEquals(2, client.requests)
        } finally { disk.shutdown() }
    }
    @Test fun `304 preserves a successful status when refreshing cached metadata`() = runBlocking {
        val result = SuccessfulImageCacheStrategy.write(NetworkResponse(code = 200),
            NetworkRequest(url), NetworkResponse(code = 304), options)
        assertEquals(200, result.response?.code)
        assertNull(result.response?.body)
        assertNotNull(SuccessfulImageCacheStrategy.read(result.response!!, NetworkRequest(url), options).response)
    }
    @Test fun `304 without a successful cached body is not persisted`() = runBlocking {
        val result = SuccessfulImageCacheStrategy.write(NetworkResponse(code = 404),
            NetworkRequest(url), NetworkResponse(code = 304), options)
        assertNull(result.response)
    }
}
