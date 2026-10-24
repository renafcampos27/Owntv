package tv.own.owntv.features.epg

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.nav.MainSection

/**
 * A scheduled EPG programme reminder for a specific profile and channel.
 */
data class ProgrammeReminder(
    val profileId: Long,
    val channelId: Long,
    val channelName: String,
    val programmeTitle: String,
    val startMs: Long,
    val stopMs: Long,
    val displayStartMs: Long = startMs,
    val displayStopMs: Long = stopMs,
) {
    val key: Key get() = Key(profileId, channelId, startMs)

    data class Key(
        val profileId: Long,
        val channelId: Long,
        val startMs: Long,
    )
}

/**
 * Session store for EPG programme reminders ("Remind me / Watch channel when starting").
 *
 * Reminders become due [LEAD_TIME_MS] (60s) before [ProgrammeReminder.startMs] and remain
 * eligible until [ProgrammeReminder.stopMs] unless consumed, dismissed, or automatically
 * satisfied because the programme has started and the user is already watching that
 * live channel at the live edge. During the 60s lead window, suppress rather than consume.
 */
class ProgrammeReminderStore {

    private val lock = Any()
    private val _reminders = MutableStateFlow<List<ProgrammeReminder>>(emptyList())
    val reminders: StateFlow<List<ProgrammeReminder>> = _reminders.asStateFlow()


    fun canSchedule(programme: EpgProgrammeEntity, nowMs: Long): Boolean =
        programme.startMs > nowMs && programme.stopMs > programme.startMs

    fun isScheduled(profileId: Long, channelId: Long, startMs: Long): Boolean {
        if (profileId < 0) return false
        val target = ProgrammeReminder.Key(profileId, channelId, startMs)
        return _reminders.value.any { it.key == target }
    }

    /**
     * Toggles the reminder for [programme] on [channel] under [profileId].
     * Returns `true` when the reminder is now scheduled, or `false` when cancelled/ineligible.
     */
    fun toggle(
        profileId: Long,
        channel: ChannelEntity,
        programme: EpgProgrammeEntity,
        nowMs: Long,
    ): Boolean {
        if (profileId < 0) return false
        val target = ProgrammeReminder.Key(profileId, channel.id, programme.startMs)
        synchronized(lock) {
            val current = _reminders.value
            if (current.any { it.key == target }) {
                _reminders.value = current.filterNot { it.key == target }
                return false
            }
            if (!canSchedule(programme, nowMs)) return false
            val added = ProgrammeReminder(
                profileId = profileId,
                channelId = channel.id,
                channelName = channel.name,
                programmeTitle = programme.title,
                startMs = programme.startMs,
                stopMs = programme.stopMs,
                displayStartMs = programme.displayStartMs,
                displayStopMs = programme.displayStopMs,
            )
            _reminders.value = (current + added).sortedWith(
                compareBy<ProgrammeReminder> { it.startMs }.thenBy { it.channelId },
            )
            return true
        }
    }

    fun dismiss(key: ProgrammeReminder.Key) {
        synchronized(lock) {
            _reminders.value = _reminders.value.filterNot { it.key == key }
        }
    }

    fun consume(key: ProgrammeReminder.Key) {
        dismiss(key)
    }

    /**
     * Prunes expired reminders (`stopMs <= nowMs`), auto-consumes any due reminder for a channel
     * the user is already watching live at the live edge ([playingLiveChannelId]) after its start, and returns the
     * earliest due reminder for [profileId] inside `[startMs - LEAD_TIME_MS, stopMs)`.
     *
     * When [inForeground] is `false` (after `ON_STOP` when the Activity is in the background or the
     * box is suspended), evaluation does nothing and returns `null` so reminders are neither shown
     * nor silently consumed while the app is invisible.
     */
    fun evaluateDue(
        profileId: Long,
        nowMs: Long,
        playingLiveChannelId: Long? = null,
        inForeground: Boolean = true,
    ): ProgrammeReminder? {
        if (!inForeground) return null
        synchronized(lock) {
            val current = _reminders.value
            if (current.isEmpty()) return null
            val kept = ArrayList<ProgrammeReminder>(current.size)
            var changed = false
            var candidate: ProgrammeReminder? = null

            for (item in current) {
                if (item.stopMs <= nowMs) {
                    changed = true
                    continue
                }
                val isDue = item.profileId == profileId &&
                    profileId >= 0 &&
                    nowMs >= item.startMs - LEAD_TIME_MS &&
                    nowMs < item.stopMs
                if (isDue && playingLiveChannelId != null && item.channelId == playingLiveChannelId) {
                    // Suppress the prompt while already on the channel, but keep the reminder
                    // until the programme actually starts: the viewer may zap away in the lead window.
                    if (nowMs >= item.startMs) {
                        changed = true
                    } else {
                        kept.add(item)
                    }
                    continue
                }
                kept.add(item)
                if (isDue && (candidate == null || item.startMs < candidate.startMs)) {
                    candidate = item
                }
            }
            if (changed) {
                _reminders.value = kept
            }
            return candidate
        }
    }

    fun evaluateDue(
        profileId: Long,
        nowMs: Long,
        playingLiveChannelId: Long? = null,
        lifecycleState: Lifecycle.State,
    ): ProgrammeReminder? = evaluateDue(
        profileId = profileId,
        nowMs = nowMs,
        playingLiveChannelId = playingLiveChannelId,
        inForeground = isForegroundLifecycle(lifecycleState),
    )

    fun clearAll() {
        synchronized(lock) {
            _reminders.value = emptyList()
        }
    }

    companion object {
        /** Lead time before programme start when the reminder confirmation prompt becomes due. */
        const val LEAD_TIME_MS = 60_000L

        val shared = ProgrammeReminderStore()

        /**
         * Returns `true` when the Activity lifecycle is at least [Lifecycle.State.STARTED] (visible
         * in the foreground). Reminders must not tick, auto-consume, or open dialogs after `ON_STOP`.
         */
        fun isForegroundLifecycle(state: Lifecycle.State): Boolean =
            state.isAtLeast(Lifecycle.State.STARTED)

        /**
         * Returns `true` when the shell is in the foreground and no blocking overlay/dialog is up.
         */
        fun canShowReminderDialog(
            lifecycleState: Lifecycle.State,
            overlaysClear: Boolean,
        ): Boolean = isForegroundLifecycle(lifecycleState) && overlaysClear

        /**
         * Resolves the channel id currently being watched live at the live edge, or `null` when
         * the app is backgrounded, the player engine is not actively playing, multiview is open,
         * or playback is protected (catch-up, timeshift, local recording copy, or VOD).
         */
        fun resolveActiveLiveEdgeChannelId(
            lifecycleState: Lifecycle.State,
            playerFullscreen: Boolean,
            multiviewOpen: Boolean,
            zapSource: MainSection?,
            enginePlaying: Boolean,
            protectedPlayback: Boolean,
            playingChannelId: Long?,
        ): Long? {
            if (!isForegroundLifecycle(lifecycleState)) return null
            if (!playerFullscreen || multiviewOpen || zapSource != MainSection.LIVE_TV) return null
            if (!enginePlaying || protectedPlayback) return null
            return playingChannelId
        }

        /**
         * Returns `true` when the user is in an active replay/archive, local recording/timeshift
         * copy, rewound live stream, or non-live VOD playback that must never be interrupted
         * without explicit confirmation.
         */
        fun shouldProtectCurrentPlayback(
            playerOpen: Boolean,
            zapSource: MainSection?,
            catchupActive: Boolean,
            localCopyPlayback: Boolean,
            timeshifted: Boolean,
        ): Boolean {
            if (!playerOpen) return false
            return catchupActive ||
                localCopyPlayback ||
                timeshifted ||
                (zapSource != null && zapSource != MainSection.LIVE_TV)
        }
    }
}
