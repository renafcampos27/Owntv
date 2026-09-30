package tv.own.owntv.features.settings

import org.junit.Assert.*
import org.junit.Test

class LiveReserveDraftTest {
    @Test fun `consecutive remote increments accumulate without persistence emissions`() {
        var draft = LiveReserveDraft.from(8, 4)
        repeat(2) { draft = draft.adjustMinimum(1) }
        assertEquals(10, draft.minimum)
        assertEquals(12, draft.maximum)
        repeat(2) { draft = draft.adjustMaximum(1) }
        assertEquals(14, draft.maximum)
    }

    @Test fun `raising minimum past maximum keeps valid whole pair`() {
        val draft = LiveReserveDraft.from(8, 0).adjustMinimum(1)
        assertEquals(9, draft.minimum)
        assertEquals(9, draft.maximum)
    }

    @Test fun `reset produces defaults without changing the original draft`() {
        val original = LiveReserveDraft.from(15, 4)
        val reset = LiveReserveDraft.defaults()
        assertEquals(15, original.minimum)
        assertEquals(19, original.maximum)
        assertEquals(8, reset.minimum)
        assertEquals(10, reset.maximum)
    }

    @Test fun `held control stays bounded at both extremes`() {
        var draft = LiveReserveDraft.from(1, 0)
        repeat(100) { draft = draft.adjustMinimum(-1) }
        assertEquals(1, draft.minimum)
        repeat(100) { draft = draft.adjustMaximum(1) }
        assertEquals(11, draft.maximum)
        repeat(100) { draft = draft.adjustMinimum(1) }
        assertEquals(60, draft.minimum)
        assertEquals(60, draft.maximum)
    }
}
