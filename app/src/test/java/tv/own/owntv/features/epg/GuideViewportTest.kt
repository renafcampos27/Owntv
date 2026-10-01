package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test

class GuideViewportTest {
    @Test fun longHistoryLoadsOnlyNearbyBlocksIncludingTheirBoundaries() {
        val hour = 3_600_000L
        val window = guideReadWindow(120 * hour, 0, 31 * 24 * hour)
        assertEquals(114 * hour to 132 * hour, window)
        assertEquals(window, guideReadWindow(125 * hour, 0, 31 * 24 * hour))
        assertEquals(120 * hour to 138 * hour, guideReadWindow(126 * hour, 0, 31 * 24 * hour))
        assertEquals(0L to 12 * hour, guideReadWindow(0, 0, 31 * 24 * hour))
    }
    @Test fun dayNavigationPreservesLocalHourAcrossDaylightSaving() {
        val zone = java.time.ZoneId.of("Europe/Lisbon")
        val before = java.time.ZonedDateTime.of(2026, 3, 28, 12, 0, 0, 0, zone)
        val after = shiftGuideTime(before.toInstant().toEpochMilli(), 1, true, zone)
        assertEquals(12, java.time.Instant.ofEpochMilli(after).atZone(zone).hour)
        assertEquals(23 * 3_600_000L, after - before.toInstant().toEpochMilli())
        assertEquals(before.toInstant().toEpochMilli(), shiftGuideTime(after, -1, true, zone))
    }
    @Test fun axisWorkDependsOnViewportRatherThanHistory() {
        assertEquals(visibleGuideSlots(500, 1200, 960, 120f), visibleGuideSlots(2000, 1200, 960, 120f))
        assertTrue(visibleGuideSlots(2000, 1200, 960, 120f).count() <= 10)
    }
    @Test fun axisClampsAtBothEndsAndHandlesEmptyLayout() {
        assertEquals(0..2, visibleGuideSlots(3, 0, 960, 120f))
        assertEquals(8..9, visibleGuideSlots(10, 1080, 960, 120f))
        assertTrue(visibleGuideSlots(10, 0, 0, 120f).isEmpty())
    }
    @Test fun cellLookupUsesHalfOpenBoundsAndPreservesGaps() {
        val cells = listOf(GuideProgrammeCells.Cell(0, 0, 10), GuideProgrammeCells.Cell(1, 20, 30), GuideProgrammeCells.Cell(2, 30, 40))
        assertTrue(GuideProgrammeCells.visible(cells, 10, 20).isEmpty())
        assertEquals(1..1, GuideProgrammeCells.visible(cells, 20, 30))
        assertEquals(1..2, GuideProgrammeCells.visible(cells, 29, 31))
    }
}
