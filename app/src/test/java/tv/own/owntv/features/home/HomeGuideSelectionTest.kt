package tv.own.owntv.features.home

import org.junit.Assert.*
import org.junit.Test

class HomeGuideSelectionTest {
    @Test fun reorderingKeepsTheSameChannelVisible() {
        val selection = reconcileHomeGuideSelection(listOf(1, 3, 4, 5, 6, 7, 8, 2), 2, 1, 0)
        assertEquals(HomeGuideSelection(7, 2, 3), selection)
    }
    @Test fun removalSelectsAVisibleNeighbourWithItsNewIdentity() {
        assertEquals(HomeGuideSelection(1, 3, 0), reconcileHomeGuideSelection(listOf(1, 3, 4), 2, 1, 0))
        assertEquals(HomeGuideSelection(-1, null, 0), reconcileHomeGuideSelection(emptyList(), 2, 1, 3))
    }
    @Test fun shrinkingAndAnUnselectedContainerDoNotStealFocus() {
        assertEquals(HomeGuideSelection(0, 9, 0), reconcileHomeGuideSelection(listOf(9), 2, 7, 3))
        assertEquals(-1, reconcileHomeGuideSelection(listOf(1, 2), null, -1, 0).index)
    }
}
