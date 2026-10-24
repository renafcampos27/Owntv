package tv.own.owntv.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tv.own.owntv.core.backup.BackupManager
import java.io.File

/** Phase 12 — drives Backup & Restore (selective export/import to a JSON file). */
enum class DoneKind { EXPORTED, RESTORED }
enum class BackupError { EXPORT, READ, IMPORT }

class BackupViewModel(
    private val backup: BackupManager,
    private val profileDao: tv.own.owntv.core.database.dao.ProfileDao,
    private val settings: tv.own.owntv.core.settings.SettingsRepository,
    private val companion: tv.own.owntv.core.companion.CompanionController,
) : ViewModel() {

    // ---- Remote restore: another device uploads a backup JSON to the TV over the LAN companion server. ----
    /** Server lifecycle (Idle / Starting / Listening with PIN+QR / Failed) for the remote-restore panel. */
    val remoteState get() = companion.state

    /** Uploaded backup files — the screen collects this and feeds each into [inspect]. */
    val remoteBackups get() = companion.backups

    fun startRemoteRestore(port: Int = tv.own.owntv.core.companion.CompanionLink.DEFAULT_PORT) {
        if (localOperation.active) return
        remoteExport.close()
        companion.startForBackupRestore(port)
    }

    fun stopRemoteRestore() = companion.stop()

    /** Exports to an app-internal cache file, then serves it over the companion server for another device
     *  to download. On failure the base screen shows the error; on success the remote panel shows PIN+QR. */
    fun exportRemote(sections: Set<BackupManager.Section>, backupPassword: String?, profileIds: Set<Long>) {
        if (localOperation.active) return
        if (backupPassword.isNullOrBlank()) {
            remoteExport.close()
            _state.value = State.Error(BackupError.EXPORT)
            return
        }
        // Core always writes owntv-backup.own. Give each job its own directory so an uncancellable
        // older writer cannot overwrite the file already being served by its replacement.
        val folder = remoteBackupExportFolder(companion.backupExportDir)
        remoteExport.start {
            backup.export(folder, sections, backupPassword, profileIds).map(::File)
        }
    }

    fun stopRemoteExport() = remoteExport.close()

    /** All profiles + the active one, for the export flow's profile-picker step. */
    data class ProfileChoices(val profiles: List<tv.own.owntv.core.database.entity.ProfileEntity>, val activeId: Long)

    private val _profileChoices = MutableStateFlow<ProfileChoices?>(null)
    val profileChoices: StateFlow<ProfileChoices?> = _profileChoices.asStateFlow()

    fun loadProfiles() {
        viewModelScope.launch {
            _profileChoices.value = ProfileChoices(
                profiles = profileDao.getAllOnce(),
                activeId = settings.activeProfileId.first(),
            )
        }
    }

    /** PIN check for ticking a locked, non-active profile in the export picker. */
    fun verifyPin(profile: tv.own.owntv.core.database.entity.ProfileEntity, pin: String): Boolean =
        tv.own.owntv.core.util.Pin.verify(pin, profile.pinHash, scope = tv.own.owntv.core.util.Pin.profileScope(profile.id))

    sealed interface State {
        data object Idle : State
        data object Working : State

        /** A restore file was picked & inspected: let the user choose which sections to apply.
         *  [password] is non-null only for a sealed `.own` already unlocked in the step before —
         *  it must ride through to the import, or the file would have to be decrypted twice. */
        data class ChooseRestore(
            val file: File,
            val available: Set<BackupManager.Section>,
            val encrypted: Boolean,
            val password: String? = null,
        ) : State
        data class ReviewProtection(
            val file: File,
            val sections: Set<BackupManager.Section>,
            val password: String?,
            val targets: List<BackupManager.ProfileProtectionTarget>,
            val authorizedPins: Map<Long, String> = emptyMap(),
            val pinError: Long? = null,
            val authorizationUnavailable: Boolean = false,
        ) : State
        data class Done(
            val kind: DoneKind,
            val path: String? = null,
            val items: Int = 0,
            val passwordsOmitted: Boolean = false,
            val skippedSources: Int = 0,
            val invalidLocale: Boolean = false,
            val skippedSettings: Int = 0,
            val skippedSubtitleItems: Int = 0,
        ) : State
        data class Error(val kind: BackupError) : State

        /**
         * Encrypted restore needs the backup password — [retry] is true after a wrong attempt.
         *
         * [sections] is null for a **sealed** `.own` container, which is asked for the password
         * BEFORE anything else: none of its contents — not even the section list — is readable until
         * it is decrypted, so there is nothing to pick yet and nothing to gain by skipping.
         * A non-null [sections] is the older field-encrypted flow: sections picked, password
         * optional, skipping restores everything but the saved passwords.
         */
        data class NeedPassword(
            val file: File,
            val sections: Set<BackupManager.Section>?,
            val retry: Boolean = false,
        ) : State {
            /** Whole-file encryption: the password is mandatory and there is nothing to skip to. */
            val sealed: Boolean get() = sections == null
        }
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val localOperation = LocalBackupOperation<State>(viewModelScope,
        working = { _state.value = State.Working }, idle = { _state.value = State.Idle },
        publish = { _state.value = it })

    private val remoteExport = RemoteBackupExportOperation(
        scope = viewModelScope,
        startServer = { file ->
            companion.startForBackupDownload(tv.own.owntv.core.companion.CompanionLink.DEFAULT_PORT, file)
        },
        stopServer = { companion.stop() },
        working = { _state.value = State.Working },
        idle = { _state.value = State.Idle },
        failed = { _state.value = State.Error(BackupError.EXPORT) },
    )

    override fun onCleared() {
        localOperation.close()
        remoteExport.close()
        super.onCleared()
    }

    /** Export requires a backup passphrase so playlist credentials are never silently omitted.
     *  [profileIds] are the PIN-authorized profiles from the picker step. */
    fun export(folder: File, sections: Set<BackupManager.Section>, backupPassword: String?, profileIds: Set<Long>) {
        if (localOperation.active) return
        remoteExport.close()
        if (backupPassword.isNullOrBlank()) { _state.value = State.Error(BackupError.EXPORT); return }
        localOperation.start(onFailure = { State.Error(BackupError.EXPORT) }) {
            backup.export(folder, sections, backupPassword, profileIds, fileName = manualBackupFilename()).fold(
                onSuccess = { State.Done(DoneKind.EXPORTED, path = it, passwordsOmitted = false) },
                onFailure = { State.Error(BackupError.EXPORT) })
        }
    }

    /**
     * Step 1 of restore: inspect the picked file so the section picker can show what's inside.
     *
     * A sealed `.own` can't be inspected at all yet — it asks for the password first and comes back
     * through [unlock]. Everything else (plain `.own`, legacy `.json`) inspects immediately.
     */
    fun inspect(file: File) {
        if (localOperation.active) return
        remoteExport.close()
        localOperation.start(onFailure = { State.Error(BackupError.READ) }) {
            if (backup.isSealed(file)) State.NeedPassword(file, sections = null)
            else backup.sectionsIn(file).fold(
                onSuccess = { State.ChooseRestore(file, it.sections, it.encrypted) },
                onFailure = { State.Error(BackupError.READ) })
        }
    }

    /** Sealed-container step 1b: decrypt before offering section choices. */
    fun unlock(file: File, password: String) {
        if (localOperation.active) return
        remoteExport.close()
        localOperation.start(onFailure = { State.Error(BackupError.READ) }) {
            backup.sectionsIn(file, password).fold(
                onSuccess = { State.ChooseRestore(file, it.sections, it.encrypted, password) },
                onFailure = { if (it is BackupManager.WrongPasswordException)
                    State.NeedPassword(file, sections = null, retry = true) else State.Error(BackupError.READ) })
        }
    }

    /** Step 2: serialised import remains owned until its writes complete. */
    fun import(file: File, sections: Set<BackupManager.Section>, backupPassword: String?, profileProtectionPins: Map<Long, String> = emptyMap()) {
        if (localOperation.active) return
        remoteExport.close()
        localOperation.start(onFailure = { State.Error(BackupError.IMPORT) }) {
            applyImport(file, sections, backupPassword, profileProtectionPins.toMap())
        }
    }

    private suspend fun applyImport(file: File, sections: Set<BackupManager.Section>, password: String?, pins: Map<Long, String> = emptyMap()): State =
        backup.import(file, sections, password, profileProtectionPins = pins).fold(
            onSuccess = { summary -> State.Done(DoneKind.RESTORED, items = summary.items,
                passwordsOmitted = password.isNullOrBlank(), skippedSources = summary.skippedSources,
                invalidLocale = summary.invalidLocale, skippedSettings = summary.skippedSettings,
                skippedSubtitleItems = summary.skippedSubtitleItems) },
            onFailure = { if (it is BackupManager.WrongPasswordException)
                State.NeedPassword(file, sections, retry = true) else State.Error(BackupError.IMPORT) },
        )

    /** Restore proceeds: import straight away when we already hold the password (sealed container,
     *  unlocked in [unlock]) or nothing is encrypted; otherwise prompt for the optional passphrase. */
    fun beginImport(
        file: File,
        sections: Set<BackupManager.Section>,
        encrypted: Boolean,
        password: String? = null,
    ) {
        if (localOperation.active) return
        remoteExport.close()
        when {
            password != null -> prepareImport(file, sections, password)
            encrypted -> _state.value = State.NeedPassword(file, sections)
            else -> prepareImport(file, sections, null)
        }
    }

    /** Review protection changes without treating a backup password as local profile authorization. */
    fun prepareImport(file: File, sections: Set<BackupManager.Section>, password: String?) {
        if (localOperation.active) return
        if (BackupManager.Section.SOURCES !in sections) { import(file, sections, password); return }
        remoteExport.close()
        localOperation.start(onFailure = { State.Error(BackupError.READ) }) {
            backup.profileProtectionTargets(file, password).fold(
                onSuccess = { if (it.isEmpty()) applyImport(file, sections, password)
                    else State.ReviewProtection(file, sections, password, it) },
                onFailure = { if (it is BackupManager.WrongPasswordException) State.NeedPassword(file, sections, true)
                    else State.Error(BackupError.READ) },
            )
        }
    }

    fun authorizeProtection(profileId: Long, pin: String) {
        val review = _state.value as? State.ReviewProtection ?: return
        if (review.targets.none { it.localProfileId == profileId }) return
        viewModelScope.launch {
            var unavailable = false
            val accepted = try {
                val profile = profileDao.getById(profileId)
                profile != null && tv.own.owntv.core.util.Pin.verify(pin, profile.pinHash, scope = tv.own.owntv.core.util.Pin.profileScope(profile.id))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                unavailable = true
                false
            }
            if (_state.value !== review) return@launch
            _state.value = if (accepted) review.copy(authorizedPins = review.authorizedPins + (profileId to pin), pinError = null, authorizationUnavailable = false)
                else review.copy(pinError = profileId, authorizationUnavailable = unavailable)
        }
    }

    fun revokeProtection(profileId: Long) {
        val review = _state.value as? State.ReviewProtection ?: return
        _state.value = review.copy(authorizedPins = review.authorizedPins - profileId, pinError = null)
    }

    fun confirmProtectionReview() {
        val review = _state.value as? State.ReviewProtection ?: return
        import(review.file, review.sections, review.password, review.authorizedPins)
    }

    fun reset() {
        if (!localOperation.reset()) return
        remoteExport.close()
        _state.value = State.Idle
    }
}
