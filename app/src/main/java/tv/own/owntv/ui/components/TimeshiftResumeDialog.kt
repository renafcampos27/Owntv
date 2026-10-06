package tv.own.owntv.ui.components

import tv.own.owntv.core.R as CoreR

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
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

@Composable
fun TimeshiftResumeDialog(channelName: String, onResume: () -> Unit, onLive: () -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(5) { withFrameNanos { }; if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
    }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup(), contentAlignment = Alignment.Center) {
            Column(Modifier.dialogPanel(width = 480.dp, padding = 20.dp, scroll = true), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.local_timeshift_resume_title), style = MaterialTheme.typography.titleLarge)
                Text(channelName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.local_timeshift_resume_description), style = MaterialTheme.typography.bodyMedium)
                OwnTVButton(stringResource(CoreR.string.common_resume), onClick = onResume, modifier = Modifier.focusRequester(focus))
                OwnTVButton(stringResource(CoreR.string.catchup_return_live), onClick = onLive, style = OwnTVButtonStyle.SECONDARY)
            }
        }
    }
}
