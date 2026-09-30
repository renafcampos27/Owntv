package tv.own.owntv.features.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.ChannelEntity

/** Main-thread owned batch cache. Failed or superseded reads never become "no guide" results. */
internal class NowPlayingTitleLoader(
    private val scope: CoroutineScope,
    maximumSize: Int,
    private val load: suspend (List<ChannelEntity>) -> Map<Long, String>,
    private val publish: (Map<Long, String>) -> Unit,
    private val ensureDelayMs: Long = 150L,
) {
    private val cache = object : LinkedHashMap<Long, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>?): Boolean =
            size > maximumSize
    }
    private var generation = 0L
    private var job: Job? = null
    private var latestChannels: List<ChannelEntity> = emptyList()

    fun ensure(channels: List<ChannelEntity>) {
        if (channels.isEmpty()) return
        latestChannels = channels.toList()
        val missing = channels.filter { it.id !in cache }
        if (missing.isNotEmpty()) start(missing, ensureDelayMs)
    }

    fun refresh(channels: List<ChannelEntity>) {
        if (channels.isEmpty()) return
        latestChannels = channels.toList()
        start(channels, 0L)
    }

    /** Invalidate old work before publishing the cleared state; reload the last visible batch. */
    fun clear() {
        invalidateWork()
        cache.clear()
        publish(emptyMap())
        if (latestChannels.isNotEmpty()) start(latestChannels, ensureDelayMs)
    }

    /** A changed match or shift affects one channel; preserve the other resolved titles. */
    fun invalidate(channel: ChannelEntity) {
        invalidateWork()
        cache.remove(channel.id)
        publish(HashMap(cache))
        val channels = latestChannels.filterNot { it.id == channel.id } + channel
        latestChannels = channels
        start(channels.filter { it.id !in cache }, ensureDelayMs)
    }

    private fun invalidateWork() {
        generation++
        job?.cancel()
        job = null
    }

    private fun start(channels: List<ChannelEntity>, delayMs: Long) {
        invalidateWork()
        val owner = generation
        job = scope.launch {
            if (delayMs > 0L) delay(delayMs)
            val titles = try {
                load(channels)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep previous titles and leave uncached channels eligible for another read.
                return@launch
            }
            currentCoroutineContext().ensureActive()
            if (owner != generation) return@launch
            for (channel in channels) cache[channel.id] = titles[channel.id].orEmpty()
            publish(HashMap(cache))
        }
    }
}
