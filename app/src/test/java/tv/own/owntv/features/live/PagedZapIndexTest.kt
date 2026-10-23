package tv.own.owntv.features.live

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity

class PagedZapIndexTest {
    private fun row(id: Long) = ChannelEntity(id = id, sourceId = 1, name = "$id", streamUrl = "")
    @Test fun indexCrossesPagesKeepsOrderAndAppliesVisibilityWithoutRetainingEntities() = runBlocking {
        val source = object : PagingSource<Int, ChannelEntity>() {
            override fun getRefreshKey(state: PagingState<Int, ChannelEntity>): Int? = null
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChannelEntity> {
                assertEquals(256, params.loadSize)
                val offset = params.key ?: 0
                return LoadResult.Page((offset + 1L..minOf(600L, offset + 256L)).map(::row), null,
                    (offset + 256).takeIf { it < 600 })
            }
        }
        val ids = collectZapIds(source) { it.id % 2L == 0L }
        assertArrayEquals((2L..600L step 2).toList().toLongArray(), ids)
        assertTrue(source.invalid)
        val window = zapWindowIds(ids, 512, 1)
        assertEquals(17, window.size); assertEquals(514L, window[8])
    }
    @Test fun windowWrapsOnlyAtTheRealContextBoundary() {
        val ids = (1L..600L).toList().toLongArray()
        assertEquals(301L, zapWindowIds(ids, 300, 1)[8])
        assertEquals(1L, zapWindowIds(ids, 600, 1)[8])
        assertTrue(zapWindowIds(ids, 999, 1).isEmpty())
    }
    @Test fun lateWindowCannotOverwriteAnotherBrowseContext() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val list = LiveZapList(scope, { 1L }, { emptyList() }, { emptyList() }, { emptyList() }, { null })
        try {
            list.armFromBrowse(listOf(row(1), row(2)), null, null, null, adjacent = { _, _ ->
                entered.complete(Unit); finish.await(); ZapWindow(row(3), listOf(row(2), row(3)))
            })
            val next = async { list.nextAvailable(-1) }
            entered.await()
            list.armFromBrowse(listOf(row(1), row(9)), null, null, null)
            finish.complete(Unit)
            assertNull(next.await()); assertEquals(listOf(1L, 9L), list.channels.value.map { it.id })
        } finally { scope.cancel() }
    }
}
