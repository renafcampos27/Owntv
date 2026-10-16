package tv.own.owntv.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.own.owntv.R
import tv.own.owntv.core.R as CoreR
import tv.own.owntv.core.database.entity.ProfileEntity
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
internal fun AutomaticBackupControls(vm: BackupViewModel, onDialogChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val state by remember(context) { AutomaticBackupPreferences.observe(context) }
        .collectAsStateWithLifecycle(initialValue = AutomaticBackupState())
    val immediate by remember(context) { AutomaticBackupScheduler.immediateWork(context) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val immediateBusy = immediate.any { !it.state.isFinished }
    val choices by vm.profileChoices.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var chooseProfiles by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<List<ProfileEntity>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val dialogOpen = chooseProfiles || selected != null
    var wasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(dialogOpen) {
        onDialogChanged(dialogOpen)
        if (wasOpen && !dialogOpen) { delay(100); runCatching { focus.requestFocus() } }
        wasOpen = dialogOpen
    }
    val colors = OwnTVTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.automatic_backup_title), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutomaticBackupPolicy.intervals.forEach { months ->
                OwnTVButton(stringResource(intervalLabel(months)), onClick = {
                    scope.launch {
                        saving = true
                        try { withContext(Dispatchers.IO) { AutomaticBackupPreferences.setInterval(context, months) }; failed = false }
                        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                        catch (_: Exception) { failed = true }
                        finally { saving = false }
                    }
                }, enabled = !saving,
                    style = if (state.months == months) OwnTVButtonStyle.PRIMARY else OwnTVButtonStyle.SECONDARY,
                    compact = true)
            }
        }
        Text(stringResource(R.string.automatic_backup_description), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Text(when {
            failed || state.failed -> stringResource(R.string.automatic_backup_failed)
            state.months == 0 -> stringResource(R.string.automatic_backup_manual_status)
            !state.configured -> stringResource(R.string.automatic_backup_setup_required)
            AutomaticBackupPolicy.due(state.months, state.anchor, System.currentTimeMillis()) -> stringResource(R.string.automatic_backup_pending)
            else -> stringResource(R.string.automatic_backup_next,
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(state.nextAt ?: 0L)))
        }, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        if (state.lastSuccess > 0L) Text(stringResource(R.string.automatic_backup_last,
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(state.lastSuccess))),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OwnTVButton(stringResource(R.string.automatic_backup_configure), onClick = {
                vm.loadProfiles(); chooseProfiles = true
            }, modifier = Modifier.focusRequester(focus), compact = true, enabled = !saving)
            OwnTVButton(stringResource(if (immediateBusy) CoreR.string.settings_backup_working else R.string.automatic_backup_now), onClick = {
                AutomaticBackupScheduler.runNow(context)
            }, enabled = state.configured && !saving && !immediateBusy, compact = true, style = OwnTVButtonStyle.SECONDARY)
        }
    }
    if (chooseProfiles) choices?.let { profiles ->
        ProfilePickerDialog(profiles.profiles, profiles.activeId, vm::verifyPin,
            onConfirm = { ids -> selected = profiles.profiles.filter { it.id in ids }; chooseProfiles = false },
            onDismiss = { chooseProfiles = false })
    }
    selected?.let { profiles ->
        AutomaticBackupConfigurationDialog(saving, onDismiss = { if (!saving) selected = null },
            onSave = { folder, password ->
                scope.launch {
                    saving = true
                    try {
                        withContext(Dispatchers.IO) { AutomaticBackupPreferences.configure(context, folder, password, profiles) }
                        failed = false; selected = null
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                    } catch (_: Exception) { failed = true
                    } finally { saving = false }
                }
            }, failed = failed)
    }
}

private fun intervalLabel(months: Int): Int = when (months) {
    0 -> R.string.automatic_backup_manual
    3 -> R.string.automatic_backup_three_months
    12 -> R.string.automatic_backup_yearly
    else -> R.string.automatic_backup_six_months
}

@Composable
private fun AutomaticBackupConfigurationDialog(saving: Boolean, failed: Boolean,
    onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val context = LocalContext.current
    var folder by remember { mutableStateOf(File(context.getExternalFilesDir(null) ?: context.filesDir, "OwnTV/backups/automatic").absolutePath) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var browser by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(browser) { if (!browser) { delay(100); runCatching { focus.requestFocus() } } }
    if (browser) {
        StorageBrowser(stringResource(CoreR.string.settings_backup_choose_folder_action), BrowseMode.FOLDER,
            onPick = { folder = it.absolutePath; browser = false }, onDismiss = { browser = false })
        return
    }
    OwnTVPopup(onDismissRequest = { if (!saving) onDismiss() }) {
        BackHandler { if (!saving) onDismiss() }
        val colors = OwnTVTheme.colors
        Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup(), contentAlignment = Alignment.Center) {
            Column(Modifier.dialogPanel(width = 620.dp, padding = 24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.automatic_backup_configure), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Text(stringResource(R.string.automatic_backup_password_note), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(folder, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                OwnTVButton(stringResource(CoreR.string.settings_backup_choose_folder_action),
                    onClick = { browser = true }, enabled = !saving, style = OwnTVButtonStyle.SECONDARY, compact = true)
                OwnTVTextField(password, { password = it }, label = stringResource(CoreR.string.settings_backup_password),
                    isPassword = true, focusRequester = focus)
                OwnTVTextField(confirmation, { confirmation = it }, label = stringResource(R.string.automatic_backup_confirm_password), isPassword = true)
                if (failed) Text(stringResource(R.string.automatic_backup_failed), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = onDismiss,
                        enabled = !saving, style = OwnTVButtonStyle.SECONDARY)
                    OwnTVButton(stringResource(R.string.automatic_backup_save), onClick = { onSave(folder, password) },
                        enabled = !saving && password.isNotBlank() && password == confirmation)
                }
            }
        }
    }
}
