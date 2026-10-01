package tv.own.owntv.features.live

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tv.own.owntv.core.database.entity.ChannelEntity

/** Optional grouped catalogue, built once off the UI thread; Room invalidates it after source updates. */
internal class GroupedChannelPagingSource(
    private val delegate: PagingSource<Int, ChannelEntity>,
    private val include: (ChannelEntity) -> Boolean,
    private val identity: (ChannelEntity) -> String,
    private val preferred: (ChannelEntity, ChannelEntity) -> ChannelEntity,
    private val onReady: (List<ChannelEntity>) -> Unit = {},
) : PagingSource<Int, ChannelEntity>() {
    private val mutex = Mutex()
    private var snapshot: List<ChannelEntity>? = null
    init { delegate.registerInvalidatedCallback { invalidate() } }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChannelEntity> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (snapshot == null) {
                val rows = LinkedHashMap<String, ChannelEntity>()
                var request: LoadParams<Int> = LoadParams.Refresh(null, 500, false)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    if (invalid || delegate.invalid) return@withLock LoadResult.Invalid()
                    when (val result = delegate.load(request)) {
                        is LoadResult.Page -> {
                            result.data.forEach { ch ->
                                currentCoroutineContext().ensureActive()
                                if (include(ch)) {
                                    val key = identity(ch)
                                    rows[key] = rows[key]?.let { preferred(it, ch) } ?: ch
                                }
                            }
                            val next = result.nextKey ?: break
                            request = LoadParams.Append(next, 500, false)
                        }
                        is LoadResult.Error -> return@withLock LoadResult.Error(result.throwable)
                        is LoadResult.Invalid -> return@withLock LoadResult.Invalid()
                    }
                }
                if (invalid || delegate.invalid) return@withLock LoadResult.Invalid()
                snapshot = rows.values.toList()
                onReady(snapshot!!)
            }
            if (invalid) return@withLock LoadResult.Invalid()
            val rows = snapshot!!
            val boundary = (params.key ?: 0).coerceIn(0, rows.size)
            val start = if (params is LoadParams.Prepend) (boundary - params.loadSize).coerceAtLeast(0) else boundary
            val end = if (params is LoadParams.Prepend) boundary else (start + params.loadSize).coerceAtMost(rows.size)
            LoadResult.Page(rows.subList(start, end), if (start == 0) null else start,
                if (end == rows.size) null else end, itemsBefore = start, itemsAfter = rows.size - end)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ChannelEntity>): Int? = state.anchorPosition
}
