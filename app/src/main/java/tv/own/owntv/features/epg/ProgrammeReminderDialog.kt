package tv.own.owntv.features.epg

import tv.own.owntv.core.R as CoreR

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.OwnTVPopup
import tv.own.owntv.ui.components.dialogPanel
import tv.own.owntv.ui.components.modalScrim
import tv.own.owntv.ui.components.trapAllFocusExit
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Confirmation prompt shown when a scheduled EPG programme reminder becomes due.
 *
 * When [protectedPlayback] is true (catch-up archive, recording/timeshift copy, rewound live
 * stream, or VOD playback is active), default focus lands on Dismiss and a note warns that
 * switching will stop the current replay/recording, so an accidental OK press never interrupts it.
 */
@Composable
fun ProgrammeReminderDialog(
    reminder: ProgrammeReminder,
    protectedPlayback: Boolean,
    onWatchChannel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    val formatTime = rememberSystemTimeFormatter()
    val defaultFocus = remember { FocusRequester() }
    LaunchedEffect(reminder.key, protectedPlayback) {
        repeat(5) {
            withFrameNanos { }
            if (runCatching { defaultFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    OwnTVPopup(onDismissRequest = onDismiss, automaticPrompt = true) {
        BackHandler { onDismiss() }
        Box(
            Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.dialogPanel(width = 480.dp, padding = 20.dp, scroll = true),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.epg_reminder_due_title, reminder.channelName),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
                Text(
                    reminder.programmeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Text(
                    stringResource(
                        CoreR.string.content_epg_time_range,
                        formatTime(reminder.displayStartMs),
                        formatTime(reminder.displayStopMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (protectedPlayback) {
                    Text(
                        stringResource(R.string.epg_reminder_due_protected_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OwnTVButton(
                        stringResource(CoreR.string.content_epg_watch_channel),
                        onClick = onWatchChannel,
                        icon = OwnTVIcon.PLAY,
                        style = if (protectedPlayback) OwnTVButtonStyle.SECONDARY else OwnTVButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f).then(
                            if (!protectedPlayback) Modifier.focusRequester(defaultFocus) else Modifier,
                        ),
                    )
                    OwnTVButton(
                        stringResource(R.string.epg_reminder_dismiss),
                        onClick = onDismiss,
                        style = if (protectedPlayback) OwnTVButtonStyle.PRIMARY else OwnTVButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f).then(
                            if (protectedPlayback) Modifier.focusRequester(defaultFocus) else Modifier,
                        ),
                    )
                }
            }
        }
    }
}
