package tv.own.owntv.features.settings

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.dao.Phase23PlaybackChannel

class Phase23PlaybackScopeTest {
    @Test fun `category beyond first 500 remains a complete batch scope`() {
        val rows = (1L..750L).map {
            Phase23PlaybackChannel(it, 1, "$it", "Channel $it", if (it > 500) 2L else 1L)
        }
        val complete = phase23FilterScope(rows, 2, "")
        assertEquals(250, complete.size)
        assertEquals((501L..750L).toSet(), complete.map { it.id }.toSet())
        assertEquals(100, complete.take(100).size)
        // Rendered page does not supply the confirmed operation's IDs.
        val exclusions = setOf(520L, 730L)
        val confirmed = complete.filterNot { it.id in exclusions }
        assertEquals(248, confirmed.size)
        assertTrue(confirmed.any { it.id == 750L })
    }

    @Test fun `search and category share the complete scope and clear search expands it`() {
        val rows = listOf(
            Phase23PlaybackChannel(1, 1, "1", "News HD", 3),
            Phase23PlaybackChannel(2, 1, "2", "NEWS SD", 3),
            Phase23PlaybackChannel(3, 1, "3", "News HD", 4),
        )
        assertEquals(listOf(1L, 2L), phase23FilterScope(rows, 3, " news ").map { it.id })
        assertEquals(rows, phase23FilterScope(rows, null, ""))
        assertTrue(phase23FilterScope(rows, 3, "missing").isEmpty())
    }
}
