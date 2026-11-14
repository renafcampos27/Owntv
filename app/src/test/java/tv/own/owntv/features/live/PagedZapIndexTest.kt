package tv.own.owntv.features.live

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity

class PagedZapIndexTest {
    @Test fun catalogueRevisionRefreshesEvenAnInteriorWindowStep() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var revision = 0L
        var loads = 0
        val list = LiveZapList(scope, { 1L }, { emptyList() }, { emptyList() }, { emptyList() }, { null })
        try {
            list.armFromBrowse(listOf(row(1), row(2)), null, null, null,
                adjacent = { _, _ -> loads++; ZapWindow(row(3), listOf(row(1), row(3))) }, revision = { revision })
            assertEquals(2L, list.nextAvailable(1)?.id)
            assertEquals(0, loads)
            revision++
            list.catalogChanged()
            assertEquals(3L, list.nextAvailable(1)?.id)
            assertEquals(1, loads)
            assertEquals(listOf(1L, 3L), list.channels.value.map { it.id })
        } finally { scope.cancel() }
    }

    @Test fun catalogueChangeDuringReadCannotPublishTheOldWindow() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var revision = 0L
        val list = LiveZapList(scope, { 1L }, { emptyList() }, { emptyList() }, { emptyList() }, { null })
        try {
            list.armFromBrowse(listOf(row(1)), null, null, null,
                adjacent = { _, _ -> revision++; ZapWindow(row(2), listOf(row(1), row(2))) }, revision = { revision })
            assertNull(list.nextAvailable(1))
            assertEquals(listOf(1L), list.channels.value.map { it.id })
            list.catalogChanged()
            assertTrue(list.canZap.value)
        } finally { scope.cancel() }
    }

    @Test fun removingThePlayingRowKeepsDirectionAndFallsBackAtTheContextBoundary() {
        val old = longArrayOf(1, 2, 3, 4)
        val current = longArrayOf(1, 3, 4)
        val forward = replacementZapAnchor(current, old, 2, 1)!!
        val backward = replacementZapAnchor(current, old, 2, -1)!!
        assertEquals(3L, current[Math.floorMod(current.indexOf(forward) + 1, current.size)])
        assertEquals(1L, current[Math.floorMod(current.indexOf(backward) - 1, current.size)])
        assertEquals(current.toList(), zapWindowIds(current, backward, -1))
        assertNull(replacementZapAnchor(longArrayOf(1), old, 2, 1))
        assertEquals(4L, replacementZapAnchor(current, longArrayOf(), 99, 1))
    }

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
        assertEquals((1L..17L).toList(), zapWindowIds(ids, 600, 1))
        assertEquals((584L..600L).toList(), zapWindowIds(ids, 1, -1))
        assertTrue(zapWindowIds(ids, 999, 1).isEmpty())
    }
    @Test fun smallManualListsNeverRotateOrSortByChannelNumber() {
        val ids = longArrayOf(5, 1, 4, 2, 3)
        for (anchor in ids) for (delta in listOf(-1, 0, 1))
            assertEquals(ids.toList(), zapWindowIds(ids, anchor, delta))
        assertEquals(listOf(9L), zapWindowIds(longArrayOf(9), 9, 0))
    }
    @Test fun overlayWindowsKeepTheWholeCustomOrderUntilTheirExistingEntityLimit() {
        val ids = (240L downTo 1L).toList().toLongArray()
        assertEquals(ids.toList(), zapWindowIds(ids, 100, 0, 2000))
        val big = (1L..5000L).toList().toLongArray()
        assertEquals(2000, zapWindowIds(big, 4500, 0, 2000).size)
        assertTrue(4500L in zapWindowIds(big, 4500, 0, 2000))
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
