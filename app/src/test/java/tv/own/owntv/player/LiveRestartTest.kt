package tv.own.owntv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveRestartTest {
    private val programme = LiveProgramme(1_000_000L, 2_000_000L, "programme")

    @Test fun initialMinuteAndMissingArchiveCannotRestart() {
        assertNull(restartableProgramme(listOf(programme), programme.startMs + 59_999L, 600))
        assertNull(restartableProgramme(listOf(programme), programme.startMs + 60_000L, 0))
        assertEquals(programme, restartableProgramme(listOf(programme), programme.startMs + 60_000L, 600))
    }

    @Test fun expiredArchiveAndGuideGapDoNotSelectAnotherProgramme() {
        assertNull(restartableProgramme(listOf(programme), programme.startMs + 600_001L, 600))
        assertNull(restartableProgramme(listOf(programme), programme.stopMs, 3600))
        assertNull(restartableProgramme(listOf(programme.copy(stopMs = programme.startMs)), programme.startMs + 60_000L, 3600))
    }
}
