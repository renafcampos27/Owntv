package tv.own.owntv.player

import org.junit.Assert.*
import org.junit.Test

class TimelineScrubTest {
    @Test fun heldArrowAccumulatesOneFinalTargetWithoutSeekingOnEveryRepeat() {
        var scrub = TimelineScrub()
        repeat(20) { scrub = scrub.move(900_000, 3_600_000, -30_000, 1) }
        assertEquals(300_000L, scrub.targetMs)
        assertEquals(-600_000L, scrub.deltaToCommit(900_000, 3_600_000, 1))
    }
    @Test fun directionChangesKeepTheSelectedTarget() {
        val scrub = TimelineScrub().move(900_000, 3_600_000, -300_000, 1)
            .move(900_000, 3_600_000, 60_000, 1)
        assertEquals(660_000L, scrub.targetMs)
    }
    @Test fun commitUsesTheLatestPlaybackClockWhilePlaying() {
        val scrub = TimelineScrub().move(900_000, 3_600_000, -300_000, 1)
        assertEquals(-302_000L, scrub.deltaToCommit(902_000, 3_600_000, 1))
    }
    @Test fun replacementLoadCannotInheritADraftEvenWhenTheProgrammeTitleIsIdentical() {
        val scrub = TimelineScrub().move(900_000, 3_600_000, -300_000, 1)
        assertNull(scrub.deltaToCommit(900_000, 3_600_000, 2))
        assertEquals(70_000L, scrub.move(100_000, 3_600_000, -30_000, 2).targetMs)
    }
    @Test fun outOfRangeStepsAndOverflowClampToTheAvailableProgramme() {
        assertEquals(0L, TimelineScrub().move(900_000, 3_600_000, Long.MIN_VALUE, 1).targetMs)
        assertEquals(3_600_000L, TimelineScrub().move(900_000, 3_600_000, Long.MAX_VALUE, 1).targetMs)
        assertEquals(Long.MAX_VALUE, TimelineScrub().move(Long.MAX_VALUE - 1, Long.MAX_VALUE, 10, 1).targetMs)
    }
    @Test fun unknownDurationDiscardsTheDraftAndCannotIssueASeek() {
        val scrub = TimelineScrub().move(900_000, 3_600_000, -300_000, 1)
        assertNull(scrub.deltaToCommit(900_000, 0, 1))
        assertNull(scrub.move(900_000, 0, -30_000, 1).targetMs)
    }
    @Test fun seekingBeyondTheStartWhileAlreadyAtZeroDoesNotFlushAgain() {
        val scrub = TimelineScrub().move(0, 3_600_000, -30_000, 1)
        assertNull(scrub.deltaToCommit(0, 3_600_000, 1))
    }
    @Test fun durationShrinkBeforeCommitCannotSeekPastTheNewEnd() {
        val scrub = TimelineScrub().move(500_000, 3_600_000, 400_000, 1)
        assertEquals(100_000L, scrub.deltaToCommit(500_000, 600_000, 1))
    }
    @Test fun aDisabledStepCannotPinTheClockAndCreateAnUnrequestedRewind() {
        val scrub = TimelineScrub().move(500_000, 3_600_000, 0, 1)
        assertNull(scrub.targetMs)
        assertNull(scrub.deltaToCommit(510_000, 3_600_000, 1))
    }
}
