package tv.own.owntv.features.live

import androidx.paging.PagingSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import tv.own.owntv.core.database.entity.ChannelEntity

/** Retains IDs, not a second catalogue of stream URLs/entities. Page transfers stay bounded. */
internal suspend fun collectZapIds(
    source: PagingSource<Int, ChannelEntity>,
    include: (ChannelEntity) -> Boolean,
): LongArray {
    val ids = ArrayList<Long>()
    var request: PagingSource.LoadParams<Int> = PagingSource.LoadParams.Refresh(null, 256, false)
    try {
        while (true) {
            currentCoroutineContext().ensureActive()
            if (source.invalid) throw CancellationException()
            when (val page = source.load(request)) {
                is PagingSource.LoadResult.Page -> {
                    page.data.filter(include).forEach { ids.add(it.id) }
                    val next = page.nextKey ?: return ids.toLongArray()
                    check(next != request.key) // A broken source must not loop forever.
                    request = PagingSource.LoadParams.Append(next, 256, false)
                }
                is PagingSource.LoadResult.Error -> throw page.throwable
                is PagingSource.LoadResult.Invalid -> throw CancellationException()
            }
        }
    } finally { source.invalidate() }
}

internal data class ZapWindow(val target: ChannelEntity, val rows: List<ChannelEntity>)

/** A small circular entity window around the destination; ordering is the captured browse ordering. */
internal fun zapWindowIds(ids: LongArray, anchor: Long, delta: Int): List<Long> {
    val index = ids.indexOf(anchor)
    if (index < 0 || ids.size < 2) return emptyList()
    val target = Math.floorMod(index + delta, ids.size)
    val count = minOf(17, ids.size)
    val before = count / 2
    return (0 until count).map { ids[Math.floorMod(target - before + it, ids.size)] }
}
