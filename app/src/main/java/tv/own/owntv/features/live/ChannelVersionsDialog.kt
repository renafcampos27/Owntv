package tv.own.owntv.features.live

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.ui.components.*

/** On-demand local catalogue lookup; editing priority never opens a stream. */
@Composable
internal fun ChannelVersionsDialog(
    channel: ChannelEntity, vm: LiveViewModel,
    onPlay: (ChannelEntity) -> Unit,
    onSaveOrder: (List<ChannelEntity>?) -> Unit,
    onDismiss: () -> Unit,
) {
    val settings by vm.channelVersionSettings.collectAsStateWithLifecycle()
    val rows by produceState<List<ChannelEntity>?>(null, channel.id, settings) {
        value = vm.channelVersions(channel, includeHidden = true)
    }
    val closeFocus = remember { FocusRequester() }
    OwnTVPopup(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) {
            repeat(5) {
                withFrameNanos { }
                if (runCatching { closeFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            }
        }
        Column(Modifier.dialogPanel(width = 680.dp).longPressMenuGuard().trapAllFocusExit().focusGroup()) {
            Text(stringResource(R.string.channel_versions_title), style = MaterialTheme.typography.titleLarge)
            Text(channel.name, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(if (settings.prioritizeChannelVersions) R.string.channel_versions_active else R.string.channel_versions_inactive),
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OwnTVButton(stringResource(R.string.content_close), onClick = onDismiss, modifier = Modifier.focusRequester(closeFocus))
                OwnTVButton(stringResource(R.string.channel_versions_reset), onClick = { onSaveOrder(null) })
            }
            Spacer(Modifier.height(12.dp))
            val versions = rows
            if (versions == null) Text(stringResource(R.string.channel_versions_loading))
            else if (versions.isEmpty()) Text(stringResource(R.string.channel_versions_empty))
            else LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(versions, key = { _, item -> item.id }) { index, item ->
                    val hidden = CustomizeKeys.channel(item) in settings.hiddenItems
                    Column {
                        Text(stringResource(R.string.channel_versions_position, index + 1, item.name), style = MaterialTheme.typography.bodyMedium)
                        if (hidden) Text(stringResource(R.string.channel_versions_hidden), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OwnTVButton(stringResource(R.string.content_downloads_play), onClick = { onPlay(item) }, enabled = !hidden)
                            OwnTVButton(stringResource(R.string.channel_versions_up), enabled = index > 0,
                                onClick = { onSaveOrder(versions.toMutableList().apply { add(index - 1, removeAt(index)) }) })
                            OwnTVButton(stringResource(R.string.channel_versions_down), enabled = index < versions.lastIndex,
                                onClick = { onSaveOrder(versions.toMutableList().apply { add(index + 1, removeAt(index)) }) })
                        }
                    }
                }
            }
        }
    }
}
