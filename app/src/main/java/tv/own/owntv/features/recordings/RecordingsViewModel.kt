package tv.own.owntv.features.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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
import tv.own.owntv.core.recording.RecordingDeleteResult
import tv.own.owntv.core.recording.RecordingManager
import tv.own.owntv.core.recording.RecordingPlayback
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
     * recording may begin a few minutes late. Re-read on resume after system permission settings.
     */
    private val _timersAreExact = MutableStateFlow(recordings.timersAreExact())
    val timersAreExact = _timersAreExact.asStateFlow()
    fun refreshAlarmPermission() { _timersAreExact.value = recordings.timersAreExact() }
    val unarmedRecordingIds = recordings.unarmedRecordingIds
    private val _rearmingIds = MutableStateFlow<Set<Long>>(emptySet())
    val rearmingIds = _rearmingIds.asStateFlow()
    private val _rearmUnavailable = MutableSharedFlow<Unit>()
    val rearmUnavailable = _rearmUnavailable.asSharedFlow()

    fun rearm(recording: RecordingEntity) {
        if (recording.id in _rearmingIds.value) return
        _rearmingIds.value = _rearmingIds.value + recording.id
        viewModelScope.launch {
            try {
                if (!recordings.rearm(recording)) _rearmUnavailable.emit(Unit)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _rearmUnavailable.emit(Unit)
            } finally {
                _rearmingIds.value = _rearmingIds.value - recording.id
            }
        }
    }

    private val _lastPlayedId = MutableStateFlow<Long?>(null)

    /** The recording last asked to play, so returning from the player lands back on its row. */
    val lastPlayedId: StateFlow<Long?> = _lastPlayedId.asStateFlow()

    private val _retryUnavailable = MutableSharedFlow<Unit>()
    val retryUnavailable = _retryUnavailable.asSharedFlow()

    private val _playUnavailable = MutableSharedFlow<Unit>()
    val playUnavailable = _playUnavailable.asSharedFlow()
    private val _partsRequireInternal = MutableSharedFlow<Unit>()
    val partsRequireInternal = _partsRequireInternal.asSharedFlow()

    private val availabilityActive = MutableStateFlow(false)
    private val _fileUnavailable = MutableSharedFlow<tv.own.owntv.core.storage.MediaAvailability>()
    val fileUnavailable = _fileUnavailable.asSharedFlow()
    val unavailableFiles = combine(
        rows.map { list -> list.filter { it.status != RecordingStatus.RECORDING &&
            tv.own.owntv.core.recording.RecordingIntegrity.canPlay(it) } }.distinctUntilChanged(),
        availabilityActive,
    ) { rows, active -> rows to active }
        .flatMapLatest { (rows, active) ->
            if (!active) flowOf(emptyMap<Long, tv.own.owntv.core.storage.MediaAvailability>())
            else kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(recordings.unavailableFiles(rows))
                    kotlinx.coroutines.delay(5_000L)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyMap())

    private val openSelection = RecordingOpenSelection()
    private var openJob: Job? = null
    private val _openingRecordingId = MutableStateFlow<Long?>(null)
    val openingRecordingId: StateFlow<Long?> = _openingRecordingId.asStateFlow()

    fun onPlaybackForegroundChanged(foreground: Boolean) {
        availabilityActive.value = foreground
        openSelection.setForeground(foreground)
        if (!foreground) cancelPlaybackOpening()
    }

    private suspend fun checkPlaybackOpening(token: Long, profileId: Long) {
        val activeProfile = settings.activeProfileId.first()
        openSelection.check(token)
        if (activeProfile != profileId) throw kotlinx.coroutines.CancellationException()
    }

    fun cancelPlaybackOpening() {
        openSelection.invalidate()
        openJob?.cancel()
        openJob = null
        _openingRecordingId.value = null
    }

    /** Finished files and the local, committed segment view use the same player controls. */
    fun play(recording: RecordingEntity, onStarted: () -> Unit = {}) {
        if (!openSelection.canOpen) return
        openJob?.cancel()
        val token = openSelection.next()
        _openingRecordingId.value = recording.id
        openJob = viewModelScope.launch {
            var acquiredPlayback: RecordingPlayback? = null
            try {
                if (settings.activeProfileId.first() != recording.profileId) return@launch
                val current = recordings.current(recording) ?: return@launch
                openSelection.check(token)
                val external = settings.externalPlayerFor(MediaType.LIVE).first()
                if (current.status == RecordingStatus.RECORDING) {
                    if (external) return@launch
                    val playback = recordings.openInProgress(current)
                    if (playback == null) {
                        if (openSelection.owns(token)) _playUnavailable.emit(Unit)
                        return@launch
                    }
                    acquiredPlayback = playback
                    if (!openSelection.owns(token) || settings.activeProfileId.first() != current.profileId) {
                        playback.close()
                        acquiredPlayback = null
                        return@launch
                    }
                    try {
                        checkPlaybackOpening(token, current.profileId)
                        player.playLocalRecording(playback, current.title, recording = current)
                        acquiredPlayback = null
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (e: Exception) {
                        playback.close()
                        acquiredPlayback = null
                        if (openSelection.owns(token)) _playUnavailable.emit(Unit)
                        return@launch
                    }
                } else {
                    if (!tv.own.owntv.core.recording.RecordingIntegrity.canPlay(current)) return@launch
                    val path = current.filePath ?: return@launch
                    val availability = recordings.availability(current, verifyReadable = true)
                    checkPlaybackOpening(token, current.profileId)
                    if (availability != tv.own.owntv.core.storage.MediaAvailability.AVAILABLE) {
                        if (openSelection.owns(token)) _fileUnavailable.emit(availability)
                        return@launch
                    }
                    if (!openSelection.owns(token) || settings.activeProfileId.first() != current.profileId) return@launch
                    if (recordings.isParts(current)) {
                        if (external) {
                            if (openSelection.owns(token)) _partsRequireInternal.emit(Unit)
                            return@launch
                        }
                        val playback = recordings.openParts(current)
                        if (playback == null) {
                            if (openSelection.owns(token)) _playUnavailable.emit(Unit)
                            return@launch
                        }
                        acquiredPlayback = playback
                        if (!openSelection.owns(token) || settings.activeProfileId.first() != current.profileId) {
                            playback.close()
                            acquiredPlayback = null
                            return@launch
                        }
                        try {
                            checkPlaybackOpening(token, current.profileId)
                            player.playLocalRecording(playback, current.title, recording = current)
                            acquiredPlayback = null
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            playback.close()
                            acquiredPlayback = null
                            if (openSelection.owns(token)) _playUnavailable.emit(Unit)
                            return@launch
                        }
                    } else if (external) {
                        checkPlaybackOpening(token, current.profileId)
                        externalPlayerLauncher.launch(path, current.title)
                    } else {
                        checkPlaybackOpening(token, current.profileId)
                        player.play(path, title = current.title, isLive = false)
                    }
                }
                checkPlaybackOpening(token, current.profileId)
                _lastPlayedId.value = current.id
                onStarted()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                acquiredPlayback?.close()
                throw cancelled
            } catch (e: Exception) {
                acquiredPlayback?.close()
                if (openSelection.owns(token)) {
                    _playUnavailable.emit(Unit)
                }
            } finally {
                if (openSelection.owns(token)) {
                    _openingRecordingId.value = null
                }
            }
        }
    }

    /** True when playback should be handed to another app, so the shell does not mount its player. */
    val externalPlayerOn: StateFlow<Boolean> = settings.externalPlayerFor(MediaType.LIVE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Ends a running recording and keeps the captured file, including a partial capture. */
    fun stop(recording: RecordingEntity) {
        if (_openingRecordingId.value == recording.id) cancelPlaybackOpening()
        viewModelScope.launch {
            if (settings.activeProfileId.first() == recording.profileId) recordings.stopAndAwait(recording)
        }
    }

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
    fun cancel(recording: RecordingEntity) {
        if (_openingRecordingId.value == recording.id) cancelPlaybackOpening()
        viewModelScope.launch {
            if (settings.activeProfileId.first() == recording.profileId) recordings.cancel(recording)
        }
    }

    private val _deletingIds = MutableStateFlow<Set<Long>>(emptySet())
    val deletingIds: StateFlow<Set<Long>> = _deletingIds.asStateFlow()

    private val _deleteResult = MutableSharedFlow<RecordingDeleteResult>()
    val deleteResult = _deleteResult.asSharedFlow()

    /** Removes the row **and** the file. The only thing here that deletes anything (D2). */
    fun delete(recording: RecordingEntity) {
        if (recording.id in _deletingIds.value) return
        _deletingIds.value = _deletingIds.value + recording.id
        if (_openingRecordingId.value == recording.id) cancelPlaybackOpening()
        viewModelScope.launch {
            try {
                if (settings.activeProfileId.first() == recording.profileId) {
                    if (_lastPlayedId.value == recording.id && player.hasLocalRecordingPlayback) player.stop()
                    val result = recordings.delete(recording)
                    if (result != RecordingDeleteResult.SUCCESS) {
                        _deleteResult.emit(result)
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _deleteResult.emit(RecordingDeleteResult.DELETE_FAILED)
            } finally {
                _deletingIds.value = _deletingIds.value - recording.id
            }
        }
    }

    /** Retry a live window or request a separate archive recovery, keeping the original file. */
    fun retry(recording: RecordingEntity) {
        viewModelScope.launch {
            if (settings.activeProfileId.first() != recording.profileId) return@launch
            if (!recordings.retryRecording(recording)) _retryUnavailable.emit(Unit)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelPlaybackOpening()
    }
}
