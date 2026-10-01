package tv.own.owntv.features.epg

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Session-only observations. Provider metadata and a successfully built URL are not observations. */
internal class ReplayEvidenceStore(
    private val capacity: Int = 256,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    data class Key(val profileId: Long, val sourceId: Long, val channelId: Long, val startMs: Long, val stopMs: Long)
    enum class Outcome { STARTED, ATTEMPT_FAILED }
    private data class Entry(val outcome: Outcome, val atMs: Long)
    private val entries = LinkedHashMap<Key, Entry>()
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()

    @Synchronized fun record(key: Key, outcome: Outcome) {
        entries.remove(key)
        entries[key] = Entry(outcome, nowMs())
        while (entries.size > capacity.coerceAtLeast(1)) entries.remove(entries.keys.first())
        _revision.value++
    }

    @Synchronized fun get(key: Key): Outcome? {
        val entry = entries[key] ?: return null
        val ttl = if (entry.outcome == Outcome.STARTED) 15 * 60_000L else 5 * 60_000L
        if (nowMs() - entry.atMs >= ttl) { entries.remove(key); return null }
        return entry.outcome
    }
}

/** Shared by the guide and the live player, bounded and discarded when the app process ends. */
internal val sessionReplayEvidence = ReplayEvidenceStore()
