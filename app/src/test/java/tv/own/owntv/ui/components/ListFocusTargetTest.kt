package tv.own.owntv.ui.components

import org.junit.Assert.*
import org.junit.Test

class ListFocusTargetTest {
    @Test fun removedItemUsesItsVisualSlotWithoutCountingHeadersAsItems() {
        assertEquals(ListFocusTarget(3, 3), listFocusTarget(listOf(null, 1, null, 3, 4), 2, 3))
        assertEquals(ListFocusTarget(4, 4), listFocusTarget(listOf(null, 1, null, 3, 4), 8, 4))
    }
    @Test fun movedItemIsFollowedByIdentityAndLastRemovalUsesLastSurvivor() {
        assertEquals(ListFocusTarget(2, 4), listFocusTarget(listOf(null, 1, null, 3, 2), 2, 1))
        assertEquals(ListFocusTarget(3, 3), listFocusTarget(listOf(null, 1, null, 3), 4, 5))
    }
    @Test fun noItemsHasAnExplicitEmptyResult() {
        assertNull(listFocusTarget(emptyList(), 1, 0))
        assertNull(listFocusTarget(listOf(null, null), 1, 0))
    }
}
