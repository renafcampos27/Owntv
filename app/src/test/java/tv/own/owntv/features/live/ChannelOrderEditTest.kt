package tv.own.owntv.features.live

import org.junit.Assert.*
import org.junit.Test

class ChannelOrderEditTest {
    @Test fun movingToEndKeepsEveryOtherChannelInItsRelativeOrder() {
        val original = listOf(10L, 20L, 30L, 40L)
        assertEquals(listOf(10L, 30L, 40L, 20L), moveOrderItem(original, 1, 3))
        assertEquals(listOf(10L, 20L, 30L, 40L), original)
    }
    @Test fun movingToBeginningAndUndoingRestoresTheExactOrder() {
        val original = listOf(10L, 20L, 30L, 40L)
        val edited = moveOrderItem(original, 3, 0)
        assertEquals(listOf(40L, 10L, 20L, 30L), edited)
        assertEquals(original, moveOrderItem(edited, 0, 3))
        assertEquals(original.toSet(), edited.toSet())
    }
    @Test fun invalidOrUnchangedPositionsDoNotCopyOrMutateTheSession() {
        val original = listOf(10L, 20L)
        assertSame(original, moveOrderItem(original, 0, -1))
        assertSame(original, moveOrderItem(original, 1, 2))
        assertSame(original, moveOrderItem(original, 0, 0))
        val empty = emptyList<Long>()
        assertSame(empty, moveOrderItem(empty, 0, 0))
    }
}
