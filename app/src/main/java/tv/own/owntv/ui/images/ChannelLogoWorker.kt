package tv.own.owntv.ui.images

import android.content.Context
import android.os.SystemClock
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.epg.EpgLogoStore

internal object ChannelLogoScheduler {
    private const val WORK = "owntv-channel-logos"
    suspend fun observe(context: Context) {
        GlobalContext.get().get<SourceDao>().observeAll()
            .map { sources -> sources.filter { it.syncLive }.map { Triple(it.id, it.lastSyncAt, it.userAgent) } }.distinctUntilChanged()
            .collect {
                val request = OneTimeWorkRequestBuilder<ChannelLogoWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
            }
    }
}

/** The normal WorkManager constructor also works after process death; no Activity is needed. */
class ChannelLogoWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val koin = GlobalContext.get()
        val repository = koin.get<ChannelLogoRepository>()
        val sources = koin.get<SourceDao>()
        val channels = koin.get<ChannelDao>()
        val active = mutableSetOf<String>()
        val start = SystemClock.elapsedRealtime()
        try {
            for (source in sources.observeAll().first().filter { it.syncLive }) {
                var cursor = -1L
                val generation = source.lastSyncAt ?: source.createdAt
                do {
                    currentCoroutineContext().ensureActive()
                    if (SystemClock.elapsedRealtime() - start > 7 * 60_000L)
                        return if (runAttemptCount < 3) Result.retry() else Result.success()
                    // Large international lists stay bounded; further visible logos load on demand.
                    if (active.size >= 8192) return Result.success()
                    val current = sources.getById(source.id)
                    if (current == null || current.lastSyncAt != source.lastSyncAt) return Result.retry()
                    val page = channels.logoPage(source.id, cursor, 200)
                    if (page.isEmpty()) break
                    val urls = page.mapNotNull { EpgLogoStore.iconFor(it.epgChannelId) ?: it.logoUrl }
                        .map(String::trim)
                        .filter { it.startsWith("https://", true) || it.startsWith("http://", true) }.distinct()
                    urls.forEach { active.add(ChannelLogoDiskCache.key(it)) }
                    for (batch in urls.chunked(2)) coroutineScope {
                        batch.map { url -> async { repository.refresh(url, generation, source.userAgent, background = true) } }.awaitAll()
                    }
                    cursor = page.last().id
                } while (true)
            }
            repository.prune(active)
            return Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { return if (runAttemptCount < 3) Result.retry() else Result.failure() }
    }
}
