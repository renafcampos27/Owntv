package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.entity.EpgProgrammeEntity

class GuideProgrammeCellsTest {
    private fun at(rows: List<EpgProgrammeEntity>, time: Long, from: Long, to: Long) =
        GuideProgrammeRow.prepare(rows, from, to).at(time)
    private fun adjacentTime(rows: List<EpgProgrammeEntity>, time: Long, delta: Int, from: Long, to: Long) =
        GuideProgrammeRow.prepare(rows, from, to).adjacentTime(time, delta)

    private fun programme(start: Long, stop: Long) = EpgProgrammeEntity(sourceId = 1, epgChannelId = "channel", title = "programme", startMs = start, stopMs = stop)

    @Test fun overlappingFeedsHaveSeparateVisibleCells() {
        val cells = GuideProgrammeCells.layout(listOf(programme(0, 100), programme(80, 180)), 0, 200)
        assertEquals(80L, cells[0].stopMs)
        assertEquals(cells[0].stopMs, cells[1].startMs)
    }

    @Test fun oldProgrammeIsClippedToWindowWithoutChangingItsPlaybackTime() {
        val old = programme(0, 100)
        val cells = GuideProgrammeCells.layout(listOf(old), 20, 90)
        assertEquals(20L, cells.single().startMs)
        assertEquals(90L, cells.single().stopMs)
        assertEquals(0L, old.startMs)
    }

    @Test fun invalidRowsAndDuplicateStartsDoNotProduceNegativeWidths() {
        val cells = GuideProgrammeCells.layout(listOf(programme(10, 50), programme(10, 60), programme(80, 70)), 0, 100)
        assertEquals(1, cells.size)
        assertEquals(1, cells.single().programmeIndex)
        assertTrue(cells.all { it.stopMs > it.startMs })
    }

    @Test fun gapsRemainEmptyAndUnsortedFeedsKeepCorrectProgrammeIdentity() {
        val cells = GuideProgrammeCells.layout(listOf(programme(100, 150), programme(0, 50)), 0, 200)
        assertEquals(listOf(1, 0), cells.map { it.programmeIndex })
        assertEquals(50L, cells.first().stopMs)
        assertEquals(100L, cells.last().startMs)
    }

    @Test fun gapDoesNotSelectPreviousOrFutureProgramme() {
        val rows = listOf(programme(0, 50), programme(100, 150))
        assertNull(at(rows, 75, 0, 200))
        assertNull(at(rows, -1, 0, 200))
        assertNull(at(rows, 150, 0, 200))
    }

    @Test fun exactBoundarySelectsVisibleNextProgramme() {
        val rows = listOf(programme(0, 100), programme(80, 180))
        assertSame(rows[0], at(rows, 79, 0, 200))
        assertSame(rows[1], at(rows, 80, 0, 200))
        assertNull(at(rows, 180, 0, 200))
    }

    @Test fun selectionUsesTheShorterProgrammeThenReturnsToTheOriginalBroadcast() {
        val rows = listOf(programme(0, 100), programme(50, 60))
        assertSame(rows[1], at(rows, 55, 20, 90))
        assertSame(rows[0], at(rows, 70, 20, 90))
        assertNull(at(rows, 19, 20, 90))
    }

    @Test fun nestedListingsKeepOriginalPlaybackTimesAndStableFragmentKeys() {
        val film = programme(0, 100)
        val news = programme(50, 60)
        val cells = GuideProgrammeCells.layout(listOf(film, news), 0, 100)
        assertEquals(listOf(0, 1, 0), cells.map { it.programmeIndex })
        assertEquals(listOf(0L, 50L, 60L), cells.map { it.startMs })
        assertEquals(listOf(50L, 60L, 100L), cells.map { it.stopMs })
        assertSame(film, at(listOf(film, news), 70, 0, 100))
        assertEquals(0L, film.startMs)
        assertEquals(60L, adjacentTime(listOf(film, news), 55, 1, 0, 100))
        val reordered = GuideProgrammeCells.layout(listOf(news, film), 20, 90)
        assertEquals(cells.map { it.key }, reordered.map { it.key })
    }

    @Test fun navigationSkipsHiddenDuplicateAndUsesDrawingOrder() {
        val rows = listOf(programme(100, 150), programme(10, 50), programme(10, 60))
        assertSame(rows[2], at(rows, 10, 0, 200))
        assertEquals(100L, adjacentTime(rows, 10, 1, 0, 200))
        assertEquals(10L, adjacentTime(rows, 100, -1, 0, 200))
    }

    @Test fun navigationCanLeaveGapInEitherDirection() {
        val rows = listOf(programme(0, 50), programme(100, 150))
        assertEquals(0L, adjacentTime(rows, 75, -1, 0, 200))
        assertEquals(100L, adjacentTime(rows, 75, 1, 0, 200))
        assertNull(adjacentTime(emptyList(), 75, 1, 0, 200))
    }
}
