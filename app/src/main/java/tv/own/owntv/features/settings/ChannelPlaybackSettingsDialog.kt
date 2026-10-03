package tv.own.owntv.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.player.EnginePreference
import tv.own.owntv.core.settings.ChannelPlaybackOptions
import tv.own.owntv.core.settings.ChannelStreamFormat
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
internal fun ChannelPlaybackSettingsDialog(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val configs by vm.channelPlaybackConfigs.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var channel by remember { mutableStateOf<ChannelEntity?>(null) }
    var query by remember { mutableStateOf("") }
    val candidates by produceState<List<ChannelEntity>?>(null, sourceId, query) {
        value = null
        sourceId?.let { selected -> delay(200); value = vm.searchChannelPlaybackChannels(selected, query) }
    }
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    BackHandler { if (channel != null) channel = null else if (sourceId != null) sourceId = null else onDismiss() }
    val selectedChannel = channel
    if (selectedChannel != null) {
        ChannelPlaybackEditor(selectedChannel, configs.firstOrNull { it.channel.matches(selectedChannel) }?.options ?: ChannelPlaybackOptions(),
            onSave = { vm.setChannelPlaybackOptions(selectedChannel, it); channel = null }, onDismiss = { channel = null })
        return
    }
    LaunchedEffect(sourceId, selectedChannel) {
        repeat(5) {
            withFrameNanos { }
            if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Column(Modifier.dialogPanel(width = 680.dp).trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_channel_playback), color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_channel_apply_hint), color = colors.onSurfaceVariant)
            if (sourceId == null) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(sources.filter { it.syncLive }, key = { _, it -> it.id }) { index, source ->
                        FocusableSurface(onClick = { sourceId = source.id; query = "" }, modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(focus) else Modifier)) {
                            Text(source.name, color = colors.onSurface, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                OwnTVButton(stringResource(R.string.content_close), onClick = onDismiss,
                    modifier = if (sources.none { it.syncLive }) Modifier.focusRequester(focus) else Modifier)
            } else {
                Text(sources.firstOrNull { it.id == sourceId }?.name.orEmpty(), color = colors.onSurface)
                SearchBar(query, { query = it }, modifier = Modifier.fillMaxWidth().focusRequester(focus), surface = null)
                if (candidates == null) Text(stringResource(R.string.channel_versions_loading), color = colors.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates.orEmpty(), key = { it.id }) { item ->
                        val customized = configs.any { it.channel.matches(item) }
                        FocusableSurface(onClick = { channel = item }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.name, color = colors.onSurface)
                                if (customized) Text(stringResource(R.string.settings_channel_customized), color = colors.primary)
                            }
                        }
                    }
                }
                if (candidates != null && candidates.orEmpty().isEmpty()) Text(stringResource(R.string.content_no_channels_here), color = colors.onSurfaceVariant)
                OwnTVButton(stringResource(R.string.common_back), onClick = { sourceId = null })
            }
        }
    }
}

private enum class ChannelField { ENGINE, FORMAT, RESERVE, EXTRA, PREROLL, LATENCY, SOFTWARE_AUDIO, AUDIO_DELAY, HLS_BOUNDARIES, HLS_KEYFRAMES, HLS_SEGMENTS }

private fun ChannelField.isHlsCompatibility() = this == ChannelField.HLS_BOUNDARIES ||
    this == ChannelField.HLS_KEYFRAMES || this == ChannelField.HLS_SEGMENTS

@Composable
private fun ChannelPlaybackEditor(channel: ChannelEntity, initial: ChannelPlaybackOptions,
    onSave: (ChannelPlaybackOptions) -> Unit, onDismiss: () -> Unit) {
    var draft by remember(channel.id) { mutableStateOf(initial) }
    var picker by remember { mutableStateOf<ChannelField?>(null) }
    var returnField by remember { mutableStateOf(ChannelField.ENGINE) }
    val focus = remember { ChannelField.entries.associateWith { FocusRequester() } }
    val listState = rememberLazyListState()
    val colors = OwnTVTheme.colors
    val inherit = stringResource(R.string.settings_channel_inherit)
    val labels = mapOf(
        ChannelField.ENGINE to stringResource(R.string.settings_live_tv_player),
        ChannelField.FORMAT to stringResource(R.string.settings_channel_format),
        ChannelField.RESERVE to stringResource(R.string.settings_channel_reserve),
        ChannelField.EXTRA to stringResource(R.string.settings_channel_extra),
        ChannelField.PREROLL to stringResource(R.string.settings_channel_preroll),
        ChannelField.LATENCY to stringResource(R.string.settings_channel_latency),
        ChannelField.SOFTWARE_AUDIO to stringResource(R.string.settings_channel_software_audio),
        ChannelField.AUDIO_DELAY to stringResource(R.string.settings_channel_audio_sync),
        ChannelField.HLS_BOUNDARIES to stringResource(R.string.settings_channel_hls_boundaries),
        ChannelField.HLS_KEYFRAMES to stringResource(R.string.settings_channel_hls_keyframes),
        ChannelField.HLS_SEGMENTS to stringResource(R.string.settings_channel_hls_segments),
    )
    val hints = mapOf(
        ChannelField.ENGINE to stringResource(R.string.settings_channel_hls_hint),
        ChannelField.RESERVE to stringResource(R.string.settings_channel_reserve_hint),
        ChannelField.EXTRA to stringResource(R.string.settings_channel_extra_hint),
        ChannelField.PREROLL to stringResource(R.string.settings_channel_preroll_hint),
        ChannelField.LATENCY to stringResource(R.string.settings_channel_latency_hint),
        ChannelField.AUDIO_DELAY to stringResource(R.string.settings_channel_audio_hint),
        ChannelField.HLS_BOUNDARIES to stringResource(R.string.settings_channel_hls_boundaries_hint),
        ChannelField.HLS_KEYFRAMES to stringResource(R.string.settings_channel_hls_keyframes_hint),
        ChannelField.HLS_SEGMENTS to stringResource(R.string.settings_channel_hls_segments_hint),
    )
    val numbers = mapOf(ChannelField.RESERVE to (1..60).toList(), ChannelField.EXTRA to (0..10).toList(),
        ChannelField.PREROLL to (0..10).toList(), ChannelField.LATENCY to (1..60).toList(), ChannelField.AUDIO_DELAY to (-5000..5000 step 50).toList())
    val choices = ChannelField.entries.associateWith { field ->
        (if (field.isHlsCompatibility()) emptyList() else listOf("inherit" to inherit)) + when (field) {
            ChannelField.ENGINE -> listOf(EnginePreference.EXO_ONLY.name to engineLabel(EnginePreference.EXO_ONLY),
                EnginePreference.MPV_ONLY.name to engineLabel(EnginePreference.MPV_ONLY))
            ChannelField.FORMAT -> listOf(ChannelStreamFormat.HLS.name to stringResource(R.string.settings_channel_hls),
                ChannelStreamFormat.TS.name to stringResource(R.string.settings_channel_ts),
                ChannelStreamFormat.AUTO.name to stringResource(R.string.settings_channel_auto_format))
            ChannelField.HLS_BOUNDARIES, ChannelField.HLS_KEYFRAMES, ChannelField.HLS_SEGMENTS -> listOf(
                "false" to stringResource(R.string.settings_channel_hls_standard),
                "true" to stringResource(R.string.settings_channel_hls_enabled))
            ChannelField.SOFTWARE_AUDIO -> listOf("false" to stringResource(R.string.settings_channel_audio_auto), "true" to stringResource(R.string.settings_channel_audio_software))
            else -> numbers.getValue(field).map { value -> value.toString() to
                if (field == ChannelField.PREROLL && value == 0) stringResource(R.string.settings_auto)
                else stringResource(if (field == ChannelField.AUDIO_DELAY) R.string.settings_channel_ms else R.string.settings_channel_seconds, value) }
        }
    }
    fun selected(field: ChannelField): String = when (field) {
        ChannelField.ENGINE -> draft.engine?.name
        ChannelField.FORMAT -> draft.format?.name
        ChannelField.RESERVE -> draft.reserveSecs?.toString()
        ChannelField.EXTRA -> draft.extraSecs?.toString()
        ChannelField.PREROLL -> draft.prerollSecs?.toString()
        ChannelField.LATENCY -> draft.latencySecs?.toString()
        ChannelField.SOFTWARE_AUDIO -> draft.softwareAudio?.toString()
        ChannelField.AUDIO_DELAY -> draft.audioDelayMs?.toString()
        ChannelField.HLS_BOUNDARIES -> draft.hlsDetectAccessUnits?.toString()
        ChannelField.HLS_KEYFRAMES -> draft.hlsAllowNonIdrKeyframes?.toString()
        ChannelField.HLS_SEGMENTS -> draft.hlsPrepareFromSegments?.toString()
    } ?: if (field.isHlsCompatibility()) "false" else "inherit"
    BackHandler { if (picker != null) picker = null else onDismiss() }
    val field = picker
    if (field != null) {
        val selected = selected(field)
        val items = choices.getValue(field)
        // Put the current value first so the selected focus target is composed even for 201 offsets.
        PickerDialog(labels.getValue(field), items.filter { it.first == selected } + items.filterNot { it.first == selected }, selected,
            onSelect = { value ->
                val number = value.toIntOrNull()
                draft = when (field) {
                    ChannelField.ENGINE -> draft.copy(engine = EnginePreference.entries.firstOrNull { it.name == value })
                    ChannelField.FORMAT -> {
                        val format = ChannelStreamFormat.entries.firstOrNull { it.name == value }
                        draft.copy(format = format)
                    }
                    ChannelField.RESERVE -> draft.copy(reserveSecs = number)
                    ChannelField.EXTRA -> draft.copy(extraSecs = number)
                    ChannelField.PREROLL -> draft.copy(prerollSecs = number)
                    ChannelField.LATENCY -> draft.copy(latencySecs = number)
                    ChannelField.SOFTWARE_AUDIO -> draft.copy(softwareAudio = value.toBooleanStrictOrNull())
                    ChannelField.AUDIO_DELAY -> draft.copy(audioDelayMs = number)
                    ChannelField.HLS_BOUNDARIES -> draft.copy(hlsDetectAccessUnits = value.toBooleanStrictOrNull()?.takeIf { it })
                    ChannelField.HLS_KEYFRAMES -> draft.copy(hlsAllowNonIdrKeyframes = value.toBooleanStrictOrNull()?.takeIf { it })
                    ChannelField.HLS_SEGMENTS -> draft.copy(hlsPrepareFromSegments = value.toBooleanStrictOrNull()?.takeIf { it })
                }
                picker = null
            }, onDismiss = { picker = null })
        return
    }
    LaunchedEffect(picker) {
        listState.scrollToItem(returnField.ordinal)
        repeat(5) {
            withFrameNanos { }
            if (runCatching { focus.getValue(returnField).requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Column(Modifier.dialogPanel(width = 720.dp).trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(channel.name, color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_channel_apply_hint), color = colors.onSurfaceVariant)
            val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 250.dp).coerceIn(140.dp, 390.dp)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxHeight), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChannelField.entries, key = { it.name }) { item ->
                    Row2(icon = OwnTVIcon.PLAY, title = labels.getValue(item), desc = hints[item],
                        chip = choices.getValue(item).firstOrNull { it.first == selected(item) }?.second ?: inherit,
                        chevron = true, primaryChip = selected(item) != "inherit" && (!item.isHlsCompatibility() || selected(item) == "true"), modifier = Modifier.focusRequester(focus.getValue(item)),
                        onClick = { returnField = item; picker = item })
                }
            }
            OwnTVButton(stringResource(R.string.settings_channel_reset), onClick = { draft = ChannelPlaybackOptions() }, style = OwnTVButtonStyle.SECONDARY)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OwnTVButton(stringResource(R.string.common_cancel), onClick = onDismiss, style = OwnTVButtonStyle.SECONDARY)
                OwnTVButton(stringResource(R.string.common_save), onClick = { onSave(draft.normalized()) })
            }
        }
    }
}
