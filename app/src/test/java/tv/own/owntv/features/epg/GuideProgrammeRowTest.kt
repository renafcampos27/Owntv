package tv.own.owntv.features.epg

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.diagnostics.UiWorkDiagnostics
import tv.own.owntv.diagnostics.UiWorkWindow

class GuideProgrammeRowTest {
    private fun programme(start: Long, stop: Long, id: Long = start) =
        EpgProgrammeEntity(id = id, sourceId = 1, epgChannelId = "channel", title = "programme",
            startMs = start, stopMs = stop, guideShiftMs = 60_000)

    @Test fun preparedSelectionAndNavigationMatchExistingOverlapGapAndWindowPolicy() {
        val random = Random(42)
        repeat(25) {
            val programmes = List(30) { index ->
                val start = random.nextLong(-100, 1_000)
                programme(start, start + random.nextLong(-10, 200), index.toLong())
            }.shuffled(random)
            val row = GuideProgrammeRow.prepare(programmes, 20, 900)
            for (time in -120L..1_200L step 13) {
                assertSame(LegacyGuideProgrammeSelection.at(programmes, time, 20, 900), row.at(time))
                for (delta in listOf(-2, -1, 0, 1, 2)) {
                    assertEquals(LegacyGuideProgrammeSelection.adjacentTime(programmes, time, delta, 20, 900),
                        row.adjacentTime(time, delta))
                }
            }
        }
    }

    @Test fun clippingAndSourceMutationNeverChangeTheSelectedOriginalProgramme() {
        val original = programme(0, 100)
        val input = mutableListOf(original, programme(50, 60))
        val row = GuideProgrammeRow.prepare(input, 20, 90)
        input.clear()
        assertSame(original, row.at(70))
        assertEquals(0L, row.at(70)!!.startMs)
        assertEquals(60_000L, row.at(70)!!.guideShiftMs)
        assertNull(row.at(19))
        assertNull(row.at(90))
        assertEquals(listOf(20L, 50L, 60L), row.cells.map { it.startMs })
        assertEquals(listOf(0L, 50L, 0L), row.cells.map { it.key.programmeStartMs })
    }

    @Test fun cacheReusesOnlySameInputOwnerAndWindowAndHonoursBothBudgets() {
        val input = listOf(programme(0, 100))
        val cache = GuideProgrammeRowCache<String>(maxRows = 2, maxItems = 6)
        val a = cache.get("profile1-source1", input, 0, 100)
        assertSame(a, cache.get("profile1-source1", input, 0, 100))
        assertNotSame(a, cache.get("profile1-source1", input.toMutableList(), 0, 100))
        val clipped = cache.get("profile1-source1", input, 20, 90)
        assertEquals(20L, clipped.cells.first().startMs)
        assertNotSame(a, clipped)
        cache.get("profile2-source1", input, 0, 100)
        cache.get("profile2-source2", input, 0, 100)
        assertEquals(2, cache.size())
        assertTrue(cache.itemCount() <= 6)
        assertNotSame(clipped, cache.get("profile1-source1", input, 20, 90))
        cache.clear()
        assertEquals(0, cache.size())
        assertEquals(0L, cache.itemCount())
    }

    @Test fun oversizedAndEmptyRowsCannotDefeatMemoryLimit() {
        val cache = GuideProgrammeRowCache<Int>(maxRows = 2, maxItems = 2)
        val input = listOf(programme(0, 100))
        val large = cache.get(1, input, 0, 100)
        assertSame(input.first(), large.at(1))
        assertEquals(0, cache.size())
        repeat(10) { cache.get(it, emptyList(), 0, 100) }
        assertEquals(2, cache.size())
        assertNull(GuideProgrammeRow.prepare(input, 100, 100).at(100))
        assertNull(GuideProgrammeRow.prepare(emptyList(), 0, 100).adjacentTime(0, 1))
    }

    @Test fun repeatedCursorMovesDoNotRebuildTheLayout() {
        val programmes = List(240) { programme(it * 100L, it * 100L + 150, it.toLong()) }
        val owner = UiWorkDiagnostics.window.start()
        try {
            repeat(5) { round ->
                repeat(100) { i ->
                    LegacyGuideProgrammeSelection.at(programmes, i * 100L, 0, 24_100)
                    LegacyGuideProgrammeSelection.adjacentTime(programmes, i * 100L, 1, 0, 24_100)
                }
                val before = UiWorkDiagnostics.window.take(owner).single { it.operation == UiWorkWindow.Operation.EPG_LAYOUT }
                val row = GuideProgrammeRow.prepare(programmes, 0, 24_100)
                repeat(100) { i -> row.at(i * 100L); row.adjacentTime(i * 100L, 1) }
                val after = UiWorkDiagnostics.window.take(owner).single { it.operation == UiWorkWindow.Operation.EPG_LAYOUT }
                assertEquals(200L, before.calls)
                assertEquals(1L, after.calls)
                println("guide_row_work round=$round programmes=240 actions=200 old_layout_calls=${before.calls} new_layout_calls=${after.calls} " +
                    "old_layout_ns=${before.totalNs} new_layout_ns=${after.totalNs}; host JVM only, not Android frame latency")
            }
        } finally { UiWorkDiagnostics.window.stop(owner) }
    }
}

/** Frozen pre-lote-5 selection algorithm: differential oracle and operation-count baseline only. */
private object LegacyGuideProgrammeSelection {
    /** Selection uses the same clipped intervals as drawing; a gap has no programme. */
    fun at(programmes: List<EpgProgrammeEntity>, timeMs: Long, windowStart: Long, windowEnd: Long): EpgProgrammeEntity? =
        GuideProgrammeCells.layout(programmes, windowStart, windowEnd)
            .firstOrNull { timeMs >= it.startMs && timeMs < it.stopMs }
            ?.let { programmes[it.programmeIndex] }

    /** Navigate visible cells, including across gaps, without selecting a hidden overlap. */
    fun adjacentTime(programmes: List<EpgProgrammeEntity>, timeMs: Long, delta: Int, windowStart: Long, windowEnd: Long): Long? {
        val cells = GuideProgrammeCells.layout(programmes, windowStart, windowEnd)
        if (cells.isEmpty()) return null
        val current = cells.indexOfFirst { timeMs >= it.startMs && timeMs < it.stopMs }
        val target = if (current >= 0) {
            (current + delta).coerceIn(0, cells.lastIndex)
        } else if (delta < 0) {
            cells.indexOfLast { it.stopMs <= timeMs }.coerceAtLeast(0)
        } else {
            cells.indexOfFirst { it.startMs > timeMs }.let { if (it < 0) cells.lastIndex else it }
        }
        return cells[target].startMs
    }

}
