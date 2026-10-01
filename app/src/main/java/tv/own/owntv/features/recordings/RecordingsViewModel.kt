package tv.own.owntv.features.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.RecordingEntity
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.model.RecordingStatus
import tv.own.owntv.core.player.ExternalPlayerLauncher
import tv.own.owntv.core.recording.RecordingManager
import tv.own.owntv.core.recording.RecordingStorageInfo
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.player.OwnTVPlayer

/**
 * What has been recorded, what is being recorded, and what never was.
 *
 * Everything here is core's — the same table the phone reads — so this only observes it and offers
 * the four things a recording can be told. Recordings are **not** filtered by the customisation
 * hidden-items list the way downloads are: a recording is a file the user asked for by name, not a
 * catalogue row, and hiding a channel should not make last night's programme disappear.
 */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class RecordingsViewModel(
    private val settings: SettingsRepository,
    private val recordings: RecordingManager,
    val player: OwnTVPlayer,
    private val externalPlayerLauncher: ExternalPlayerLauncher,
) : ViewModel() {

    val rows: StateFlow<List<RecordingEntity>> = settings.activeProfileId
        .flatMapLatest { pid -> if (pid < 0) flowOf(emptyList()) else recordings.observe(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Free and total space on the volume recordings are written to, plus the floor they stop at.
     *
     * Keyed on the download **root** as well as on the list — the same defect the Downloads screen
     * had: keyed on the list alone, pointing the folder at another volume left the bar showing the
     * old one until the app was restarted, and with nothing recorded yet the list never changes.
     */
    val storage: StateFlow<RecordingStorageInfo?> =
        kotlinx.coroutines.flow.merge(
            combine(rows, settings.downloadRoot, settings.internalMediaQuotaGiB, settings.externalMediaQuotaGiB) { _, _, _, _ -> Unit },
            kotlinx.coroutines.flow.flow { while (true) { emit(Unit); kotlinx.coroutines.delay(2_000) } },
        ).sample(1_000)
            .mapLatest { recordings.storageInfo() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * False when Android will not let the app set exact alarms, so the screen can say that a
     * recording may begin a few minutes late. Read once: revoking the permission stops the app, so
     * the answer cannot change under a running process.
     */
    val timersAreExact: Boolean = recordings.timersAreExact()

    private val _lastPlayedId = MutableStateFlow<Long?>(null)

    /** The recording last asked to play, so returning from the player lands back on its row. */
    val lastPlayedId: StateFlow<Long?> = _lastPlayedId.asStateFlow()

    private val _retryUnavailable = MutableSharedFlow<Unit>()
    val retryUnavailable = _retryUnavailable.asSharedFlow()

    private val _playUnavailable = MutableSharedFlow<Unit>()
    val playUnavailable = _playUnavailable.asSharedFlow()
    private val _partsRequireInternal = MutableSharedFlow<Unit>()
    val partsRequireInternal = _partsRequireInternal.asSharedFlow()

    /** Finished files and the local, committed segment view use the same player controls. */
    fun play(recording: RecordingEntity, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            if (settings.activeProfileId.first() != recording.profileId) return@launch
            val current = recordings.current(recording) ?: return@launch
            val external = settings.externalPlayerFor(MediaType.LIVE).first()
            if (current.status == RecordingStatus.RECORDING) {
                if (external) return@launch
                val playback = recordings.openInProgress(current)
                if (playback == null) { _playUnavailable.emit(Unit); return@launch }
                if (settings.activeProfileId.first() != current.profileId) { playback.close(); return@launch }
                try { player.playLocalRecording(playback, current.title) }
                catch (_: Exception) { playback.close(); _playUnavailable.emit(Unit); return@launch }
            } else {
                if (!tv.own.owntv.core.recording.RecordingIntegrity.canPlay(current)) return@launch
                val path = current.filePath ?: return@launch
                if (settings.activeProfileId.first() != current.profileId) return@launch
                if (recordings.isParts(current)) {
                    if (external) { _partsRequireInternal.emit(Unit); return@launch }
                    val playback = recordings.openParts(current)
                    if (playback == null) { _playUnavailable.emit(Unit); return@launch }
                    if (settings.activeProfileId.first() != current.profileId) { playback.close(); return@launch }
                    try { player.playLocalRecording(playback, current.title) }
                    catch (_: Exception) { playback.close(); _playUnavailable.emit(Unit); return@launch }
                } else if (external) externalPlayerLauncher.launch(path, current.title)
                else player.play(path, title = current.title, isLive = false)
            }
            _lastPlayedId.value = current.id
            onStarted()
        }
    }

    /** True when playback should be handed to another app, so the shell does not mount its player. */
    val externalPlayerOn: StateFlow<Boolean> = settings.externalPlayerFor(MediaType.LIVE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Ends a running recording and keeps the captured file, including a partial capture. */
    fun stop(recording: RecordingEntity) { viewModelScope.launch {
        if (settings.activeProfileId.first() == recording.profileId) recordings.stopAndAwait(recording)
    } }

    private val _archiveActionUnavailable = MutableSharedFlow<Unit>()
    val archiveActionUnavailable = _archiveActionUnavailable.asSharedFlow()

    fun pauseArchive(recording: RecordingEntity) { viewModelScope.launch {
        if (settings.activeProfileId.first() == recording.profileId && !recordings.pauseArchive(recording))
            _archiveActionUnavailable.emit(Unit)
    } }

    fun resumeArchive(recording: RecordingEntity) { viewModelScope.launch {
        if (settings.activeProfileId.first() == recording.profileId) {
            if (_lastPlayedId.value == recording.id && player.hasLocalRecordingPlayback) player.stop()
            if (!recordings.resumeArchive(recording)) _archiveActionUnavailable.emit(Unit)
        }
    } }

    /** Drops a scheduled recording. Nothing on disk is touched, because nothing is there yet. */
    fun cancel(recording: RecordingEntity) { viewModelScope.launch {
        if (settings.activeProfileId.first() == recording.profileId) recordings.cancel(recording)
    } }

    /** Removes the row **and** the file. The only thing here that deletes anything (D2). */
    fun delete(recording: RecordingEntity) { viewModelScope.launch {
        if (settings.activeProfileId.first() == recording.profileId) {
            if (_lastPlayedId.value == recording.id && player.hasLocalRecordingPlayback) player.stop()
            recordings.delete(recording)
        }
    } }

    /** Retry a live window or request a separate archive recovery, keeping the original file. */
    fun retry(recording: RecordingEntity) {
        viewModelScope.launch {
            if (settings.activeProfileId.first() != recording.profileId) return@launch
            if (!recordings.retryRecording(recording)) _retryUnavailable.emit(Unit)
        }
    }
}
