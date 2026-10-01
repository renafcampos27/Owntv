package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.database.entity.EpgProgrammeEntity

class GuideProgrammeCellsTest {
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
        assertNull(GuideProgrammeCells.at(rows, 75, 0, 200))
        assertNull(GuideProgrammeCells.at(rows, -1, 0, 200))
        assertNull(GuideProgrammeCells.at(rows, 150, 0, 200))
    }

    @Test fun exactBoundarySelectsVisibleNextProgramme() {
        val rows = listOf(programme(0, 100), programme(80, 180))
        assertSame(rows[0], GuideProgrammeCells.at(rows, 79, 0, 200))
        assertSame(rows[1], GuideProgrammeCells.at(rows, 80, 0, 200))
        assertNull(GuideProgrammeCells.at(rows, 180, 0, 200))
    }

    @Test fun selectionCannotOpenClippedOrHiddenOverlap() {
        val rows = listOf(programme(0, 100), programme(50, 60))
        assertSame(rows[1], GuideProgrammeCells.at(rows, 55, 20, 90))
        assertNull(GuideProgrammeCells.at(rows, 70, 20, 90))
        assertNull(GuideProgrammeCells.at(rows, 19, 20, 90))
    }

    @Test fun navigationSkipsHiddenDuplicateAndUsesDrawingOrder() {
        val rows = listOf(programme(100, 150), programme(10, 50), programme(10, 60))
        assertSame(rows[2], GuideProgrammeCells.at(rows, 10, 0, 200))
        assertEquals(100L, GuideProgrammeCells.adjacentTime(rows, 10, 1, 0, 200))
        assertEquals(10L, GuideProgrammeCells.adjacentTime(rows, 100, -1, 0, 200))
    }

    @Test fun navigationCanLeaveGapInEitherDirection() {
        val rows = listOf(programme(0, 50), programme(100, 150))
        assertEquals(0L, GuideProgrammeCells.adjacentTime(rows, 75, -1, 0, 200))
        assertEquals(100L, GuideProgrammeCells.adjacentTime(rows, 75, 1, 0, 200))
        assertNull(GuideProgrammeCells.adjacentTime(emptyList(), 75, 1, 0, 200))
    }
}
