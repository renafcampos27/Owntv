package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test

class ReplayEvidenceStoreTest {
    private fun key(profile: Long = 1, source: Long = 1, start: Long = 100) =
        ReplayEvidenceStore.Key(profile, source, 1, start, start + 10)

    @Test fun separatesProfilesSourcesAndProgrammeIntervals() {
        val store = ReplayEvidenceStore()
        store.record(key(), ReplayEvidenceStore.Outcome.STARTED)
        assertNull(store.get(key(profile = 2)))
        assertNull(store.get(key(source = 2)))
        assertNull(store.get(key(start = 101)))
        assertEquals(ReplayEvidenceStore.Outcome.STARTED, store.get(key()))
    }

    @Test fun expiryAndCapacityBoundOldObservations() {
        var now = 0L
        val store = ReplayEvidenceStore(2) { now }
        store.record(key(start = 1), ReplayEvidenceStore.Outcome.STARTED)
        store.record(key(start = 2), ReplayEvidenceStore.Outcome.ATTEMPT_FAILED)
        store.record(key(start = 3), ReplayEvidenceStore.Outcome.STARTED)
        assertNull(store.get(key(start = 1)))
        now = 5 * 60_000L
        assertNull(store.get(key(start = 2)))
        assertNotNull(store.get(key(start = 3)))
        now = 15 * 60_000L
        assertNull(store.get(key(start = 3)))
    }

    @Test fun aNewAttemptReplacesFailureWithActualStart() {
        val store = ReplayEvidenceStore()
        store.record(key(), ReplayEvidenceStore.Outcome.ATTEMPT_FAILED)
        store.record(key(), ReplayEvidenceStore.Outcome.STARTED)
        assertEquals(2L, store.revision.value)
        assertEquals(ReplayEvidenceStore.Outcome.STARTED, store.get(key()))
    }
}
