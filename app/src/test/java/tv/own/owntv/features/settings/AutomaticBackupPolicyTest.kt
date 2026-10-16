package tv.own.owntv.features.settings

import java.io.File
import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AutomaticBackupPolicyTest {
    @get:Rule val folder = TemporaryFolder()
    private fun at(value: String) = Instant.parse(value).toEpochMilli()

    @Test fun sixCalendarMonthsAreTheDefault() {
        assertEquals(6, AutomaticBackupState().months)
        assertFalse(AutomaticBackupState().configured)
        assertNull(AutomaticBackupState().nextAt)
    }
    @Test fun calendarMonthEndsAreClampedWithoutThirtyDayApproximation() {
        assertEquals(at("2027-02-28T12:00:00Z"), AutomaticBackupPolicy.nextAt(6, at("2026-08-31T12:00:00Z")))
        assertEquals(at("2026-04-30T12:00:00Z"), AutomaticBackupPolicy.nextAt(3, at("2026-01-31T12:00:00Z")))
    }
    @Test fun yearlyBackupHandlesLeapDay() {
        assertEquals(at("2025-02-28T12:00:00Z"), AutomaticBackupPolicy.nextAt(12, at("2024-02-29T12:00:00Z")))
    }
    @Test fun manualModeNeverBecomesDue() {
        assertNull(AutomaticBackupPolicy.nextAt(0, at("2026-01-01T00:00:00Z")))
        assertFalse(AutomaticBackupPolicy.due(0, 1L, Long.MAX_VALUE))
    }
    @Test fun dueAtBoundaryAndAfterMissedExecutionWhileBoxWasOff() {
        val anchor = at("2026-01-01T00:00:00Z")
        val due = at("2026-07-01T00:00:00Z")
        assertFalse(AutomaticBackupPolicy.due(6, anchor, due - 1))
        assertTrue(AutomaticBackupPolicy.due(6, anchor, due))
        assertTrue(AutomaticBackupPolicy.due(6, anchor, at("2026-08-01T00:00:00Z")))
    }
    @Test fun successfulBackupReanchorsAndIntervalChangesUseTheSameAnchor() {
        val success = at("2026-10-16T14:00:00Z")
        assertEquals(at("2027-01-16T14:00:00Z"), AutomaticBackupPolicy.nextAt(3, success))
        assertEquals(at("2027-04-16T14:00:00Z"), AutomaticBackupPolicy.nextAt(6, success))
        assertEquals(at("2027-10-16T14:00:00Z"), AutomaticBackupPolicy.nextAt(12, success))
    }
    @Test fun restoreAcceptsOnlySupportedIntegerIntervals() {
        for (value in listOf(0, 3, 6, 12, 6L)) assertEquals((value as Number).toInt(), automaticBackupMonths(value))
        for (value in listOf<Any>(-1, 1, 5, 24, 6.0, "6", 4294967302L)) {
            assertThrows(IllegalArgumentException::class.java) { automaticBackupMonths(value) }
        }
    }
    @Test fun profilePinOrParentalChangesRevokeUnattendedAuthorization() {
        val profile = tv.own.owntv.core.database.entity.ProfileEntity(
            id = 7, name = "TV", avatarColor = 0, pinHash = "old-hash", createdAt = 100L)
        val auth = AutomaticBackupPreferences.authorization(profile)
        assertEquals(auth, AutomaticBackupPreferences.authorization(profile.copy(name = "Renamed")))
        assertNotEquals(auth, AutomaticBackupPreferences.authorization(profile.copy(pinHash = "new-hash")))
        assertNotEquals(auth, AutomaticBackupPreferences.authorization(profile.copy(isKids = true)))
        assertNotEquals(auth, AutomaticBackupPreferences.authorization(profile.copy(createdAt = 200L)))
    }

    @Test fun rotationKeepsNewestThreeAndPreservesManualAndUnrelatedFiles() {
        val root = folder.newFolder("backups")
        val copies = (1L..5L).map { File(root, "owntv-auto-$it-${UUID.randomUUID()}.own").apply { writeText("sealed") } }
        val manual = File(root, "owntv-backup.own").apply { writeText("manual") }
        val unrelated = File(root, "owntv-auto-important.own").apply { writeText("user file") }
        val staging = File(root, ".owntv-auto-staging").apply { mkdir() }
        AutomaticBackupFiles.rotate(root)
        assertFalse(copies[0].exists()); assertFalse(copies[1].exists())
        assertTrue(copies.drop(2).all(File::exists))
        assertTrue(manual.exists()); assertTrue(unrelated.exists()); assertTrue(staging.exists())
    }
}
