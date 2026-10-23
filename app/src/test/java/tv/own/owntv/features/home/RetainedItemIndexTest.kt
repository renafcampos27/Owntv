package tv.own.owntv.features.home

import org.junit.Assert.assertEquals
import org.junit.Test

class RetainedItemIndexTest {
    @Test fun refreshPreservesIdentityAfterReorder() { assertEquals(2, retainedItemIndex(listOf(1, 2, 3), 1, listOf(3, 1, 2))) }
    @Test fun removedSelectionAndEmptyRailHaveBoundedFallback() {
        assertEquals(0, retainedItemIndex(listOf(1, 2, 3), 2, listOf(1)))
        assertEquals(0, retainedItemIndex(listOf(1), 0, emptyList()))
    }
}
