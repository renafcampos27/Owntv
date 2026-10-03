package tv.own.owntv.features.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
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
    playOnly: Boolean = false,
) {
    val settings by vm.channelVersionSettings.collectAsStateWithLifecycle()
    val rows by produceState<List<ChannelEntity>?>(null, channel.id, settings, playOnly) {
        value = vm.channelVersions(channel, includeHidden = !playOnly)
    }
    var adding by remember(channel.id) { mutableStateOf(false) }
    val closeFocus = remember { FocusRequester() }
    val firstVersionFocus = remember { FocusRequester() }
    OwnTVPopup(onDismissRequest = onDismiss) {
        LaunchedEffect(playOnly, playOnly && !rows.isNullOrEmpty(), adding) {
            if (adding) return@LaunchedEffect
            repeat(5) {
                withFrameNanos { }
                val target = if (playOnly && !rows.isNullOrEmpty()) firstVersionFocus else closeFocus
                if (runCatching { target.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            }
        }
        Column(Modifier.dialogPanel(width = 680.dp).background(Color(0xFF141D29), RoundedCornerShape(20.dp)).longPressMenuGuard().trapAllFocusExit().focusGroup()) {
            Text(stringResource(R.string.channel_versions_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (!playOnly) Text(stringResource(if (settings.prioritizeChannelVersions) R.string.channel_versions_active else R.string.channel_versions_inactive),
                color = Color.White, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VersionButton(stringResource(R.string.content_close), onClick = onDismiss, modifier = Modifier.focusRequester(closeFocus))
                VersionButton(stringResource(R.string.channel_versions_add), onClick = { adding = true })
                if (!playOnly) VersionButton(stringResource(R.string.channel_versions_reset), onClick = { onSaveOrder(null) })
            }
            Spacer(Modifier.height(12.dp))
            val versions = rows
            if (versions == null) Text(stringResource(R.string.channel_versions_loading), color = Color.White)
            else if (versions.isEmpty()) Text(stringResource(R.string.channel_versions_empty), color = Color.White)
            else LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(versions, key = { _, item -> item.id }) { index, item ->
                    if (playOnly) {
                        VersionButton(item.name, onClick = { onPlay(item) },
                            modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstVersionFocus) else Modifier))
                        return@itemsIndexed
                    }
                    val hidden = CustomizeKeys.channel(item) in settings.hiddenItems
                    Column {
                        Text(stringResource(R.string.channel_versions_position, index + 1, item.name), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        if (hidden) Text(stringResource(R.string.channel_versions_hidden), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VersionButton(stringResource(R.string.content_downloads_play), onClick = { onPlay(item) }, enabled = !hidden)
                            VersionButton(stringResource(R.string.channel_versions_up), enabled = index > 0,
                                onClick = { onSaveOrder(versions.toMutableList().apply { add(index - 1, removeAt(index)) }) })
                            VersionButton(stringResource(R.string.channel_versions_down), enabled = index < versions.lastIndex,
                                onClick = { onSaveOrder(versions.toMutableList().apply { add(index + 1, removeAt(index)) }) })
                            if (CustomizeKeys.channel(item) in settings.channelVersionGroups) {
                                VersionButton(stringResource(R.string.channel_versions_unlink), onClick = { vm.removeManualChannelVersion(item) })
                            }
                        }
                    }
                }
            }
        }
    }
    if (adding) AddChannelVersionDialog(channel, vm) { adding = false }
}

@Composable
private fun VersionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FocusableSurface(onClick = onClick, modifier = modifier, enabled = enabled,
        focusedContainerColor = Color(0xFF24548B), unfocusedContainerColor = Color(0xFF263344),
        showFocusBorder = false, focusedScale = 1f, glowElevation = 0) { focused ->
        Box(Modifier.border(if (focused) 2.dp else 1.dp,
            if (focused) Color.White else Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
            Text(label, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AddChannelVersionDialog(channel: ChannelEntity, vm: LiveViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val candidates by produceState<List<ChannelEntity>?>(emptyList(), channel.id, query) {
        value = null
        delay(250)
        value = vm.channelVersionCandidates(channel, query)
    }
    val searchFocus = remember { FocusRequester() }
    OwnTVPopup(onDismissRequest = onDismiss) {
        LaunchedEffect(Unit) {
            repeat(5) {
                withFrameNanos { }
                if (runCatching { searchFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            }
        }
        Column(Modifier.dialogPanel(width = 680.dp).background(Color(0xFF141D29), RoundedCornerShape(20.dp))
            .trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.channel_versions_add), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.channel_versions_add_hint), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            SearchBar(query, { query = it }, modifier = Modifier.fillMaxWidth().focusRequester(searchFocus), surface = null)
            VersionButton(stringResource(R.string.content_close), onDismiss)
            if (candidates == null) Text(stringResource(R.string.channel_versions_loading), color = Color.White)
            else LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(candidates.orEmpty(), key = { _, item -> item.id }) { _, item ->
                    VersionButton(item.name, { vm.addChannelVersion(channel, item); onDismiss() }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
