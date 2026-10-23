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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.core.R as CoreR
import tv.own.owntv.core.backup.BackupManager
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme

/** Keeping local protection is the default; authorization exists only for this restore in memory. */
@Composable
internal fun BackupProtectionReviewDialog(
    review: BackupViewModel.State.ReviewProtection,
    onAuthorize: (Long, String) -> Unit,
    onRevoke: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val firstFocus = remember { FocusRequester() }
    val focusGuard = remember { FocusRequestGuard() }
    var pinFor by remember(review.file) { mutableStateOf<BackupManager.ProfileProtectionTarget?>(null) }
    LaunchedEffect(Unit) { firstFocus.requestBoundedFocus(focusGuard) }
    LaunchedEffect(review.authorizedPins.keys) {
        if (pinFor?.localProfileId in review.authorizedPins) pinFor = null
    }
    BackHandler { if (pinFor != null) pinFor = null else onDismiss() }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup().cancelPendingFocusOnInput(focusGuard), contentAlignment = Alignment.Center) {
            Column(Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
                .dialogPanel(width = 640.dp, padding = 24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_phase1_protection_title), color = colors.onSurface,
                    style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.backup_phase1_protection_message), color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                if (review.targets.isEmpty()) Text(stringResource(R.string.backup_phase1_protection_none), color = colors.onSurface)
                review.targets.forEach { target ->
                    val permitted = target.localProfileId in review.authorizedPins
                    OwnTVButton(
                        stringResource(if (permitted) R.string.backup_phase1_protection_revoke
                            else R.string.backup_phase1_protection_allow, target.name),
                        onClick = {
                            if (permitted) onRevoke(target.localProfileId)
                            else if (target.requiresPin) pinFor = target
                            else onAuthorize(target.localProfileId, "")
                        },
                        style = if (permitted) OwnTVButtonStyle.PRIMARY else OwnTVButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.backup_phase1_protection_details,
                        stringResource(if (target.currentIsKids) CoreR.string.common_on else CoreR.string.common_off),
                        stringResource(if (target.incomingIsKids) CoreR.string.common_on else CoreR.string.common_off),
                        stringResource(if (target.pinWillChange) CoreR.string.common_on else CoreR.string.common_off)),
                        color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = onDismiss, style = OwnTVButtonStyle.SECONDARY)
                    OwnTVButton(stringResource(CoreR.string.settings_backup_restore_action), onClick = onConfirm,
                        modifier = Modifier.focusRequester(firstFocus))
                }
            }
        }
    }
    pinFor?.let { target ->
        var pin by remember(target.localProfileId) { mutableStateOf("") }
        val fieldFocus = remember { FocusRequester() }
        val pinFocusGuard = remember { FocusRequestGuard() }
        LaunchedEffect(target.localProfileId) { fieldFocus.requestBoundedFocus(pinFocusGuard) }
        OwnTVPopup(onDismissRequest = { pinFor = null }) {
            Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup().cancelPendingFocusOnInput(pinFocusGuard), contentAlignment = Alignment.Center) {
                Column(Modifier.dialogPanel(width = 480.dp, padding = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_phase1_protection_pin, target.name), color = colors.onSurface,
                        style = MaterialTheme.typography.titleMedium)
                    OwnTVTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                        label = stringResource(CoreR.string.settings_backup_profile_pin), isPassword = true,
                        keyboardType = KeyboardType.NumberPassword, focusRequester = fieldFocus)
                    if (review.pinError == target.localProfileId) Text(stringResource(if (review.authorizationUnavailable)
                        R.string.backup_phase1_authorization_unavailable else CoreR.string.settings_backup_pin_incorrect),
                        color = Color(0xFFEF4444), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = { pinFor = null }, style = OwnTVButtonStyle.SECONDARY)
                        OwnTVButton(stringResource(CoreR.string.common_ok), onClick = { onAuthorize(target.localProfileId, pin) }, enabled = pin.length >= 4)
                    }
                }
            }
        }
    }
}
