package tv.own.owntv.features.epg

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ChannelEntity

class GuideRenamedSearchTest {
    private fun ch(id: Long, source: Long = 4, remote: String? = "$id") =
        ChannelEntity(id = id, sourceId = source, remoteId = remote, name = "Provider $id", streamUrl = "test")

    @Test fun `renamed search resolves exact source keys and deduplicates`() = runBlocking {
        val a = ch(1); val b = ch(2, 5); val c = ch(3, remote = null)
        val names = listOf(a, b, c).associate { CustomizeKeys.channel(it) to "Custom news" }
        val result = guideRenamedCandidates(names, "NEWS", setOf(4), 100,
            remote = { _, _ -> listOf(a, b) }, named = { _, _ -> listOf(a, c) })
        assertEquals(listOf(1L, 3L), result.map { it.id })
        assertTrue(guideChannelEligible(a, setOf(4), emptySet(), emptySet()))
        assertFalse(guideChannelEligible(a.copy(categoryId = 7), setOf(4), setOf(7), emptySet()))
        assertFalse(guideChannelEligible(a, setOf(4), emptySet(), setOf(CustomizeKeys.channel(a))))
    }

    @Test fun `blank query performs no lookup and cap stops candidate accumulation`() = runBlocking {
        val names = (1L..1000L).associate { "4:$it" to "Custom" }
        var calls = 0
        val lookup: suspend (Long, List<String>) -> List<ChannelEntity> = { _, tails ->
            calls++; assertTrue(tails.size <= 400); tails.map { ch(it.toLong()) }
        }
        assertTrue(guideRenamedCandidates(names, "", setOf(4), 2, lookup, lookup).isEmpty())
        assertEquals(0, calls)
        assertEquals(2, guideRenamedCandidates(names, "custom", setOf(4), 2, lookup, lookup).size)
        assertEquals(2, calls)
        assertTrue(guideRenamedCandidates(names, "other", setOf(4), 2, lookup, lookup).isEmpty())
        assertEquals(2, calls)
    }
}
