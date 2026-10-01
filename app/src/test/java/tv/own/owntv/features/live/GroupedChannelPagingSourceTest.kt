package tv.own.owntv.features.live

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.entity.ChannelEntity

class GroupedChannelPagingSourceTest {
    private class Catalogue(private val rows: List<ChannelEntity>) : PagingSource<Int, ChannelEntity>() {
        var loads = 0
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChannelEntity> {
            loads++
            val at = params.key ?: 0
            return LoadResult.Page(listOf(rows[at]), null, if (at + 1 == rows.size) null else at + 1)
        }
        override fun getRefreshKey(state: PagingState<Int, ChannelEntity>): Int? = null
    }
    private fun row(id: Long, name: String) = ChannelEntity(id = id, sourceId = 1, remoteId = "$id", name = name, streamUrl = "")
    private val options = SectionCustomizations(prioritizeChannelVersions = true)
    private fun grouped(base: Catalogue, include: (ChannelEntity) -> Boolean = { true }) = GroupedChannelPagingSource(
        base, include, ChannelVersionPolicy::groupKey,
        { a, b -> ChannelVersionPolicy.ordered(listOf(a, b), options).first() })

    @Test fun versionsAcrossPagesCollapseAndLaterPreferredVersionWinsWithoutRequerying() = runBlocking {
        val base = Catalogue(listOf(row(1, "SIC Low"), row(2, "RTP 1 HD"), row(3, "SIC Full HD")))
        val source = grouped(base)
        val first = source.load(PagingSource.LoadParams.Refresh(null, 1, false)) as PagingSource.LoadResult.Page<Int, ChannelEntity>
        assertEquals(3L, first.data.single().id)
        assertEquals(1, first.itemsAfter)
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 1, false)) as PagingSource.LoadResult.Page<Int, ChannelEntity>
        assertEquals(2L, second.data.single().id)
        assertEquals(3, base.loads)
    }

    @Test fun prependUsesExclusiveBoundaryWhenRefreshAndPageSizesDiffer() = runBlocking {
        val source = grouped(Catalogue((1L..10L).map { row(it, "Channel $it") }))
        val refresh = source.load(PagingSource.LoadParams.Refresh(3, 5, false)) as PagingSource.LoadResult.Page<Int, ChannelEntity>
        val previous = source.load(PagingSource.LoadParams.Prepend(refresh.prevKey!!, 2, false)) as PagingSource.LoadResult.Page<Int, ChannelEntity>
        assertEquals(listOf(4L, 5L, 6L, 7L, 8L), refresh.data.map { it.id })
        assertEquals(listOf(2L, 3L), previous.data.map { it.id })
        assertEquals(1, previous.prevKey)
        assertEquals(3, previous.nextKey)
        assertTrue(previous.data.none { it in refresh.data })
    }

    @Test fun hiddenVersionIsFilteredBeforeSelectingARepresentative() = runBlocking {
        val source = grouped(Catalogue(listOf(row(1, "SIC Full HD"), row(2, "SIC HD")))) { it.id != 1L }
        val page = source.load(PagingSource.LoadParams.Refresh(null, 300, false)) as PagingSource.LoadResult.Page<Int, ChannelEntity>
        assertEquals(listOf(2L), page.data.map { it.id })
    }

    @Test fun roomInvalidationNeverReturnsTheOldSnapshot() = runBlocking {
        val base = Catalogue(listOf(row(1, "SIC HD")))
        val source = grouped(base)
        source.load(PagingSource.LoadParams.Refresh(null, 1, false))
        base.invalidate()
        assertTrue(source.invalid)
        assertTrue(source.load(PagingSource.LoadParams.Refresh(null, 1, false)) is PagingSource.LoadResult.Invalid)
    }
}
