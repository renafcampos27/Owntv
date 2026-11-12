package tv.own.owntv.ui.images

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import tv.own.owntv.core.network.cancelBlockingCallWithCoroutine
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** A failed update keeps the last good thumbnail; UI requests never wait on the main thread. */
internal class ChannelLogoRepository(
    internal val disk: ChannelLogoDiskCache,
    private val client: OkHttpClient,
    private val encode: suspend (File, String?) -> ByteArray,
    private val now: () -> Long = System::currentTimeMillis,
) {
    companion object { const val MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024 }
    private class DeferredImage(val waitMs: Long) : IllegalStateException()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val permits = Semaphore(2)
    private val locks = Array(64) { Mutex() }
    private val states = ConcurrentHashMap<String, MutableStateFlow<LogoAsset?>>()
    private val repaired = ConcurrentHashMap.newKeySet<String>()

    fun repair(url: String, asset: LogoAsset) {
        val key = ChannelLogoDiskCache.key(url.trim())
        if (!repaired.add(key + asset.revision)) return
        scope.launch {
            locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
                runCatching { disk.invalidate(key, asset.revision) }
            }
            refresh(url, 0)
        }
    }

    fun cached(url: String): LogoAsset? = states[ChannelLogoDiskCache.key(url.trim())]?.value

    fun observe(url: String): Flow<LogoAsset?> = flow {
        val key = ChannelLogoDiskCache.key(url.trim())
        val state = states.getOrPut(key) { MutableStateFlow(null) }
        coroutineScope {
            val loading = launch(Dispatchers.IO) {
                // A background revalidation may hold the URL lock for seconds. Show disk now.
                val cached = disk.read(key).asset
                if (state.value == null && cached != null) state.value = cached
                refresh(url, 0)
            }
            // Leaving the row cancels its queued/active demand, not the independent playlist worker.
            try { emitAll(state) } finally { loading.cancel() }
        }
    }

    suspend fun refresh(url: String, generation: Long, userAgent: String? = null, background: Boolean = false) = withContext(Dispatchers.IO) {
        val key = ChannelLogoDiskCache.key(url.trim())
        locks[(key.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            val record = disk.read(key)
            states[key]?.value = record.asset
            // Old failed entries get one retry after a decoder-policy upgrade; valid copies stay put.
            if (background && record.attemptedGeneration >= generation &&
                (record.asset != null || record.policyVersion >= ChannelLogoDiskCache.POLICY_VERSION)) return@withLock
            if (!record.needsRefresh(generation, now())) return@withLock
            var staging: File? = null
            try {
                permits.withPermit {
                    currentCoroutineContext().ensureActive()
                    val request = Request.Builder().url(url.trim()).apply {
                        header("Cache-Control", "no-cache")
                        userAgent?.takeIf { it.isNotBlank() }?.let { header("User-Agent", it) }
                        if (record.asset != null) {
                            record.etag?.let { header("If-None-Match", it) }
                            record.modified?.let { header("If-Modified-Since", it) }
                        }
                    }.build()
                    val call = client.newCall(request)
                    val context = currentCoroutineContext()
                    data class Download(val file: File?, val mimeType: String?, val etag: String?, val modified: String?)
                    val download = cancelBlockingCallWithCoroutine(call::cancel) {
                        call.execute().use { response ->
                            try {
                                if (response.code == 304) {
                                    check(record.asset != null)
                                    Download(null, null, response.header("ETag") ?: record.etag, response.header("Last-Modified") ?: record.modified)
                                } else {
                                    if (response.code == 429 || response.code == 503) {
                                        tv.own.owntv.core.network.HttpRetryAfter.delayMs(response.header("Retry-After"), now())
                                            ?.let { throw DeferredImage(it) }
                                    }
                                    check(response.isSuccessful)
                                    val body = response.body
                                    check(body.contentLength() <= MAX_DOWNLOAD_BYTES)
                                    val file = disk.newDownload().also { staging = it }
                                    val buffer = ByteArray(32 * 1024)
                                    val input = body.byteStream()
                                    file.outputStream().use { output ->
                                        var total = 0L
                                        while (true) {
                                            context.ensureActive()
                                            val n = input.read(buffer)
                                            if (n < 0) break
                                            check(total + n <= MAX_DOWNLOAD_BYTES)
                                            output.write(buffer, 0, n)
                                            total += n
                                        }
                                    }
                                    val reportedType = body.contentType()?.toString()
                                    val genericType = reportedType == null || reportedType.substringBefore(';').trim().equals("application/octet-stream", true)
                                    val type = if (genericType && response.request.url.encodedPath.endsWith(".svg", true)) "image/svg+xml" else reportedType
                                    Download(file, type, response.header("ETag"), response.header("Last-Modified"))
                                }
                            } catch (error: Exception) { call.cancel(); throw error }
                        }
                    }
                    val file = download.file
                    val asset = if (file == null) {
                        disk.checked(key, record, generation, now(), true, download.etag, download.modified)
                        record.asset
                    } else {
                        val thumbnail = encode(file, download.mimeType)
                        currentCoroutineContext().ensureActive()
                        disk.save(key, thumbnail, generation, now(), download.etag, download.modified)
                    }
                    states[key]?.value = asset
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                // No raw URLs or credentials in logs. An unavailable logo must not fail a playlist sync.
                val time = now()
                val wait = (error as? DeferredImage)?.waitMs ?: 0
                val deadline = if (wait == 0L) 0L else if (wait > Long.MAX_VALUE - time) Long.MAX_VALUE else time + wait
                runCatching { disk.checked(key, record, generation, time, false, retryAfterUntil = deadline) }
            } finally { staging?.delete() }
        }
    }

    suspend fun prune(active: Set<String>) = withContext(Dispatchers.IO) {
        disk.prune(active).forEach { states[it]?.value = null }
        // UI state is disposable; disk remains authoritative when a channel is revisited.
        if (states.size > 1024) states.entries.removeIf { it.value.subscriptionCount.value == 0 }
    }
}
