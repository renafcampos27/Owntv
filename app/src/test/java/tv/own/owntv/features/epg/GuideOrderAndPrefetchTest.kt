package tv.own.owntv.features.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tv.own.owntv.core.database.entity.ChannelEntity

class GuideOrderAndPrefetchTest {
    private fun channel(id: Long, source: Long = 1, order: Int = id.toInt(), name: String = "Channel $id") =
        ChannelEntity(id = id, sourceId = source, name = name, streamUrl = "https://example.test/$id", sortOrder = order)

    @Test fun filteredGuidePreservesSavedRelativeOrderAndIgnoresMissingItems() {
        val ranks = mapOf(3L to 0, 1L to 1, 2L to 2, 99L to 3)
        val all = listOf(channel(1), channel(2), channel(3), channel(4))
        assertEquals(listOf(3L, 1L, 2L, 4L), guideManualOrder(all, ranks).map { it.id })
        assertEquals(listOf(3L, 2L, 4L), guideManualOrder(all.filter { it.id != 1L }, ranks).map { it.id })
    }

    @Test fun unpositionedChannelsUseProviderThenOrderThenNameThenId() {
        val rows = listOf(channel(8, 2), channel(6, order = 1, name = "B"),
            channel(5, order = 1, name = "A"), channel(4, order = 1, name = "A"), channel(3, order = 0))
        assertEquals(listOf(6L, 3L, 4L, 5L, 8L), guideManualOrder(rows, mapOf(6L to 0)).map { it.id })
        assertEquals(listOf(3L, 4L, 5L, 6L, 8L), guideManualOrder(rows, emptyMap()).map { it.id })
    }

    @Test fun prefetchReadsOnlyThreeEntriesEvenAtStartOfLargeGuide() {
        val accessed = mutableListOf<Int>()
        val rows = object : AbstractList<Int>() {
            override val size = 20_000
            override fun get(index: Int): Int { accessed += index; return index }
        }
        assertEquals(listOf(1, 2, 3), guidePrefetchSlice(rows, 0, 3))
        assertEquals(listOf(1, 2, 3), accessed)
        accessed.clear()
        assertEquals(listOf(19_999), guidePrefetchSlice(rows, 19_998, 3))
        assertEquals(listOf(19_999), accessed)
        assertTrue(guidePrefetchSlice(rows, 19_999, 3).isEmpty())
        assertTrue(guidePrefetchSlice(rows, -1, 3).isEmpty())
        assertTrue(guidePrefetchSlice(rows, 20_000, 3).isEmpty())
        assertTrue(guidePrefetchSlice(rows, 5, 0).isEmpty())
    }
}
