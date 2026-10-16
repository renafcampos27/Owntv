package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test

class EpgEditSessionTest {
    @Test fun `profile round trip does not revive an old dialog`() {
        val session = EpgEditSession()
        session.update(1, listOf(10), null)
        val owner = session.capture(1, 10)
        assertTrue(session.accepts(owner))
        session.update(2, listOf(20), null)
        session.update(1, listOf(10), null)
        assertFalse(session.accepts(owner))
        assertTrue(session.accepts(session.capture(1, 10)))
    }

    @Test fun `background restore and source removal invalidate callbacks`() {
        for (change in listOf<(EpgEditSession) -> Unit>(
            { it.invalidate() }, { it.update(1, emptyList(), null) },
            { it.update(1, listOf(10), "restore"); it.update(1, listOf(10), null) },
        )) {
            val session = EpgEditSession()
            session.update(1, listOf(10), null)
            val owner = session.capture(1, 10)
            change(session)
            assertFalse(session.accepts(owner))
        }
    }

    @Test fun `same context emissions preserve owner but unknown source and restore reject edits`() {
        val session = EpgEditSession()
        session.update(1, listOf(10, 20), null)
        val owner = session.capture(1, 10)
        session.update(1, listOf(20, 10), null)
        assertTrue(session.accepts(owner))
        assertFalse(session.accepts(session.capture(1, 99)))
        session.update(1, listOf(10), "restore")
        assertFalse(session.accepts(session.capture(1, 10)))
    }
}
