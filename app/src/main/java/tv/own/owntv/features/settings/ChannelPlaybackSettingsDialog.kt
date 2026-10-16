package tv.own.owntv.features.settings

import tv.own.owntv.core.R as CoreR
import tv.own.owntv.R

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.player.EnginePreference
import tv.own.owntv.core.settings.ChannelPlaybackOptions
import tv.own.owntv.core.settings.ChannelStreamFormat
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
internal fun ChannelPlaybackSettingsDialog(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val configs by vm.channelPlaybackConfigs.collectAsStateWithLifecycle()
    val loaded by vm.phase23ChannelPlaybackLoaded.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var channel by remember { mutableStateOf<ChannelEntity?>(null) }
    var batchMode by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var dialogProfileId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(loaded?.profileId) {
        val profileId = loaded?.profileId ?: return@LaunchedEffect
        if (dialogProfileId != null && dialogProfileId != profileId) {
            channel = null
            batchMode = false
            sourceId = null
            query = ""
        }
        dialogProfileId = profileId
    }
    var lookupRetry by remember { mutableIntStateOf(0) }
    val lookup by produceState<UiLoadState<List<ChannelEntity>>>(UiLoadState.Loading(), sourceId, query, lookupRetry) {
        value = UiLoadState.Loading()
        try {
            value = UiLoadState.Ready(sourceId?.let { selected -> delay(200); vm.searchChannelPlaybackChannels(selected, query) }.orEmpty())
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { value = UiLoadState.Failed() }
    }
    val candidates = lookup.valueOrNull
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    BackHandler { if (batchMode) batchMode = false else if (channel != null) channel = null else if (sourceId != null) sourceId = null else onDismiss() }
    val selectedChannel = channel
    if (selectedChannel != null) {
        val ready = loaded
        var editorProfileId by remember(selectedChannel.sourceId, selectedChannel.id) { mutableStateOf<Long?>(null) }
        LaunchedEffect(ready?.profileId) {
            if (ready != null) {
                if (editorProfileId == null) editorProfileId = ready.profileId
                else if (editorProfileId != ready.profileId) channel = null
            }
        }
        if (ready == null || (editorProfileId != null && editorProfileId != ready.profileId)) {
            OwnTVPopup(onDismissRequest = { channel = null }) {
                Column(Modifier.dialogPanel(width = 680.dp)) {
                    Text(stringResource(CoreR.string.channel_versions_loading), color = colors.onSurface)
                    OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = { channel = null })
                }
            }
            return
        }
        key(ready.profileId, selectedChannel.sourceId, selectedChannel.id) {
            ChannelPlaybackEditor(
                selectedChannel,
                vm, sources.firstOrNull { it.id == selectedChannel.sourceId },
                ready.rows.firstOrNull { it.channel.matches(selectedChannel) }?.options ?: ChannelPlaybackOptions(),
                onSave = { opts -> vm.saveChannelPlaybackOptions(selectedChannel, opts, ready.profileId) },
                onDismiss = { channel = null }
            )
        }
        return
    }
    if (batchMode && sourceId != null) {
        BatchChannelPlaybackEditor(
            sourceId = sourceId!!,
            sourceName = sources.firstOrNull { it.id == sourceId }?.name.orEmpty(),
            initialQuery = query,
            vm = vm,
            onDismiss = { batchMode = false },
        )
        return
    }
    LaunchedEffect(sourceId, selectedChannel, batchMode) {
        repeat(5) {
            withFrameNanos { }
            if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Column(Modifier.dialogPanel(width = 680.dp).trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(CoreR.string.settings_channel_playback), color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(CoreR.string.settings_channel_apply_hint), color = colors.onSurfaceVariant)
            if (sourceId == null) {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(sources.filter { it.syncLive }, key = { _, it -> it.id }) { index, source ->
                        FocusableSurface(onClick = { sourceId = source.id; query = "" }, modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(focus) else Modifier)) {
                            Text(source.name, color = colors.onSurface, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                OwnTVButton(stringResource(CoreR.string.content_close), onClick = onDismiss,
                    modifier = if (sources.none { it.syncLive }) Modifier.focusRequester(focus) else Modifier)
            } else {
                Text(sources.firstOrNull { it.id == sourceId }?.name.orEmpty(), color = colors.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SearchBar(query, { query = it }, modifier = Modifier.fillMaxWidth().focusRequester(focus), surface = null)
                    }
                    OwnTVButton(
                        stringResource(R.string.settings_channel_batch_button),
                        onClick = { batchMode = true },
                        enabled = sourceId != null,
                    )
                }
                if (lookup is UiLoadState.Loading) Text(stringResource(CoreR.string.channel_versions_loading), color = colors.onSurfaceVariant)
                if (lookup is UiLoadState.Failed) {
                    Text(stringResource(R.string.ui_load_failed), color = colors.onSurfaceVariant)
                    OwnTVButton(stringResource(CoreR.string.common_retry), onClick = { lookupRetry++ })
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 330.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates.orEmpty(), key = { it.id }) { item ->
                        val customized = configs.any { it.channel.matches(item) }
                        FocusableSurface(onClick = { channel = item }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.name, color = colors.onSurface)
                                Text(stringResource(if (customized) R.string.channel_own_settings else R.string.channel_general_settings), color = if (customized) colors.primary else colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (candidates != null && candidates.orEmpty().isEmpty()) Text(stringResource(CoreR.string.content_no_channels_here), color = colors.onSurfaceVariant)
                OwnTVButton(stringResource(CoreR.string.common_back), onClick = { sourceId = null })
            }
        }
    }
}

private enum class ChannelField { ENGINE, FORMAT, RESERVE, EXTRA, PREROLL, LATENCY, SOFTWARE_AUDIO, AUDIO_DELAY, HLS_BOUNDARIES, HLS_KEYFRAMES, HLS_SEGMENTS }

private data class PlaybackBatchCatalogue(
    val scope: Phase23BatchScope?,
    val categories: List<tv.own.owntv.core.database.entity.CategoryEntity>,
)

private fun ChannelField.isHlsCompatibility() = this == ChannelField.HLS_BOUNDARIES ||
    this == ChannelField.HLS_KEYFRAMES || this == ChannelField.HLS_SEGMENTS

@Composable
private fun ChannelPlaybackEditor(
    channel: ChannelEntity,
    vm: SettingsViewModel,
    source: tv.own.owntv.core.database.entity.SourceEntity?,
    initial: ChannelPlaybackOptions,
    onSave: suspend (ChannelPlaybackOptions) -> Boolean,
    onDismiss: () -> Unit
) {
    var draft by remember(channel.id) { mutableStateOf(initial) }
    var isSaving by remember(channel.id) { mutableStateOf(false) }
    var saveError by remember(channel.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var picker by remember { mutableStateOf<ChannelField?>(null) }
    var returnField by remember { mutableStateOf(ChannelField.ENGINE) }
    val focus = remember { ChannelField.entries.associateWith { FocusRequester() } }
    val listState = rememberLazyListState()
    val colors = OwnTVTheme.colors
    val snapshot by vm.playbackSnapshot.collectAsStateWithLifecycle()
    val inherit = stringResource(CoreR.string.settings_channel_inherit)
    val labels = mapOf(
        ChannelField.ENGINE to stringResource(CoreR.string.settings_live_tv_player),
        ChannelField.FORMAT to stringResource(CoreR.string.settings_channel_format),
        ChannelField.RESERVE to stringResource(CoreR.string.settings_channel_reserve),
        ChannelField.EXTRA to stringResource(CoreR.string.settings_channel_extra),
        ChannelField.PREROLL to stringResource(CoreR.string.settings_channel_preroll),
        ChannelField.LATENCY to stringResource(CoreR.string.settings_channel_latency),
        ChannelField.SOFTWARE_AUDIO to stringResource(CoreR.string.settings_channel_software_audio),
        ChannelField.AUDIO_DELAY to stringResource(CoreR.string.settings_channel_audio_sync),
        ChannelField.HLS_BOUNDARIES to stringResource(CoreR.string.settings_channel_hls_boundaries),
        ChannelField.HLS_KEYFRAMES to stringResource(CoreR.string.settings_channel_hls_keyframes),
        ChannelField.HLS_SEGMENTS to stringResource(CoreR.string.settings_channel_hls_segments),
    )
    val hints = mapOf(
        ChannelField.ENGINE to stringResource(CoreR.string.settings_channel_hls_hint),
        ChannelField.RESERVE to stringResource(CoreR.string.settings_channel_reserve_hint),
        ChannelField.EXTRA to stringResource(CoreR.string.settings_channel_extra_hint),
        ChannelField.PREROLL to stringResource(CoreR.string.settings_channel_preroll_hint),
        ChannelField.LATENCY to stringResource(CoreR.string.settings_channel_latency_hint),
        ChannelField.AUDIO_DELAY to stringResource(CoreR.string.settings_channel_audio_hint),
        ChannelField.HLS_BOUNDARIES to stringResource(CoreR.string.settings_channel_hls_boundaries_hint),
        ChannelField.HLS_KEYFRAMES to stringResource(CoreR.string.settings_channel_hls_keyframes_hint),
        ChannelField.HLS_SEGMENTS to stringResource(CoreR.string.settings_channel_hls_segments_hint),
    )
    val numbers = mapOf(ChannelField.RESERVE to (1..60).toList(), ChannelField.EXTRA to (0..tv.own.owntv.core.settings.LiveBuffer.MAX_EXTRA_SECS).toList(),
        ChannelField.PREROLL to (tv.own.owntv.core.settings.LiveBuffer.PREROLL_CHOICES + listOfNotNull(draft.prerollSecs)).distinct().sorted(), ChannelField.LATENCY to (1..60).toList(), ChannelField.AUDIO_DELAY to (-5000..5000 step 50).toList())
    val choices = ChannelField.entries.associateWith { field ->
        (if (field.isHlsCompatibility()) emptyList() else listOf("inherit" to inherit)) + when (field) {
            ChannelField.ENGINE -> listOf(EnginePreference.EXO_ONLY.name to engineLabel(EnginePreference.EXO_ONLY),
                EnginePreference.MPV_ONLY.name to engineLabel(EnginePreference.MPV_ONLY))
            ChannelField.FORMAT -> listOf(ChannelStreamFormat.HLS.name to stringResource(CoreR.string.settings_channel_hls),
                ChannelStreamFormat.TS.name to stringResource(CoreR.string.settings_channel_ts),
                ChannelStreamFormat.AUTO.name to stringResource(CoreR.string.settings_channel_auto_format))
            ChannelField.HLS_BOUNDARIES, ChannelField.HLS_KEYFRAMES, ChannelField.HLS_SEGMENTS -> listOf(
                "false" to stringResource(CoreR.string.settings_channel_hls_standard),
                "true" to stringResource(CoreR.string.settings_channel_hls_enabled))
            ChannelField.SOFTWARE_AUDIO -> listOf("false" to stringResource(CoreR.string.settings_channel_audio_auto), "true" to stringResource(CoreR.string.settings_channel_audio_software))
            else -> numbers.getValue(field).map { value -> value.toString() to
                if (field == ChannelField.PREROLL && value == 0) stringResource(CoreR.string.settings_auto)
                else stringResource(if (field == ChannelField.AUDIO_DELAY) CoreR.string.settings_channel_ms else CoreR.string.settings_channel_seconds, value) }
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
    @Composable fun requestedValue(field: ChannelField): String {
        val global = snapshot ?: return stringResource(CoreR.string.channel_versions_loading)
        val reserve = tv.own.owntv.core.settings.LiveReserveResolver.sourceOverride(source)
        val fromPlaylist = when (field) {
            ChannelField.ENGINE -> source?.liveEnginePreference != null
            ChannelField.RESERVE, ChannelField.EXTRA -> reserve != null
            ChannelField.PREROLL -> source?.livePrerollSecs?.let { it >= 0 } == true
            ChannelField.LATENCY -> source?.liveLatencyMode != null
            else -> false
        }
        val value = if (selected(field) != "inherit") choices.getValue(field).firstOrNull { it.first == selected(field) }?.second ?: inherit
        else when (field) {
            ChannelField.ENGINE -> engineLabel(draft.effectiveEngine(
                EnginePreference.entries.firstOrNull { it.name == source?.liveEnginePreference } ?: global.liveEnginePreference,
                draft.hlsOnly(global.liveHlsOnly), draft.directTs()))
            ChannelField.FORMAT -> stringResource(if (global.liveHlsOnly) CoreR.string.settings_channel_hls else CoreR.string.settings_channel_auto_format)
            ChannelField.RESERVE -> stringResource(CoreR.string.settings_channel_seconds, (if (reserve != null) reserve.seconds else global.liveReserveSeconds) ?: tv.own.owntv.core.settings.LiveBuffer.BALANCED_SECS)
            ChannelField.EXTRA -> stringResource(CoreR.string.settings_channel_seconds, reserve?.extraSecs ?: global.liveReserveExtraSecs)
            ChannelField.PREROLL -> (source?.livePrerollSecs?.takeIf { it >= 0 } ?: global.livePrerollSecs).let {
                if (it == 0) stringResource(CoreR.string.settings_auto) else stringResource(CoreR.string.settings_channel_seconds, it) }
            ChannelField.LATENCY -> (if (source?.liveLatencyMode != null) tv.own.owntv.core.settings.LiveBuffer.effectiveSeconds(
                tv.own.owntv.core.settings.LiveLatency.fromName(source.liveLatencyMode), source.liveLatencyCustomSecs) else global.liveBufferSeconds).let {
                if (it == null) stringResource(CoreR.string.settings_auto) else stringResource(CoreR.string.settings_channel_seconds, it) }
            ChannelField.SOFTWARE_AUDIO -> stringResource(if (global.softwareAudio) CoreR.string.settings_channel_audio_software else CoreR.string.settings_channel_audio_auto)
            ChannelField.AUDIO_DELAY -> stringResource(CoreR.string.settings_channel_ms, global.audioDelayMs)
            else -> choices.getValue(field).first().second
        }
        val origin = if (selected(field) != "inherit" && (!field.isHlsCompatibility() || selected(field) == "true")) R.string.ui_setting_custom
            else if (fromPlaylist) R.string.ui_setting_playlist else R.string.ui_setting_inherited
        return stringResource(origin, value)
    }
    BackHandler { if (!isSaving) { if (picker != null) picker = null else onDismiss() } }
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
    OwnTVPopup(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Column(Modifier.dialogPanel(width = 720.dp).trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(channel.name, color = colors.onSurface, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (draft.normalized().isDefault) R.string.channel_general_settings else R.string.channel_own_settings), color = colors.primary)
            Text(stringResource(R.string.ui_settings_requested_hint), color = colors.onSurfaceVariant)
            val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 250.dp).coerceIn(140.dp, 390.dp)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxHeight), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChannelField.entries, key = { it.name }) { item ->
                    Row2(icon = OwnTVIcon.PLAY, title = labels.getValue(item), desc = hints[item],
                        chip = requestedValue(item),
                        chevron = true, primaryChip = selected(item) != "inherit" && (!item.isHlsCompatibility() || selected(item) == "true"), modifier = Modifier.focusRequester(focus.getValue(item)),
                        onClick = { if (!isSaving) { returnField = item; picker = item } })
                }
            }
            OwnTVButton(
                stringResource(CoreR.string.settings_channel_reset),
                onClick = { draft = ChannelPlaybackOptions(); saveError = false },
                style = OwnTVButtonStyle.SECONDARY,
                enabled = !isSaving
            )
            if (saveError) {
                Text(stringResource(R.string.ui_save_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = onDismiss, style = OwnTVButtonStyle.SECONDARY, enabled = !isSaving)
                OwnTVButton(
                    stringResource(if (isSaving) CoreR.string.channel_versions_loading else CoreR.string.common_save),
                    onClick = {
                        if (isSaving) return@OwnTVButton
                        val confirmedDraft = draft.normalized()
                        isSaving = true
                        scope.launch {
                            saveError = false
                            try {
                                if (onSave(confirmedDraft)) onDismiss() else saveError = true
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { saveError = true }
                            finally { isSaving = false }
                        }
                    },
                    enabled = !isSaving
                )
            }
        }
    }
}

private enum class BatchFieldAction { UNCHANGED, INHERIT, CUSTOM }

@Composable
private fun BatchChannelPlaybackEditor(
    sourceId: Long,
    sourceName: String,
    initialQuery: String,
    vm: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = OwnTVTheme.colors
    var lookupRetry by remember(sourceId) { mutableIntStateOf(0) }
    val lookup by produceState<UiLoadState<PlaybackBatchCatalogue>>(UiLoadState.Loading(), sourceId, lookupRetry) {
        value = UiLoadState.Loading()
        try {
            value = UiLoadState.Ready(PlaybackBatchCatalogue(vm.phase23LoadBatchScope(sourceId), vm.loadLiveCategoriesForSource(sourceId)))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { value = UiLoadState.Failed() }
    }
    val scopeSnapshot = lookup.valueOrNull?.scope
    val categories = lookup.valueOrNull?.categories.orEmpty()

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var channelPickerOpen by remember { mutableStateOf(false) }

    val channels = scopeSnapshot?.channels.orEmpty()
    var batchQuery by remember(sourceId) { mutableStateOf(initialQuery) }
    val filteredChannels = remember(channels, selectedCategoryId, batchQuery) {
        phase23FilterScope(channels, selectedCategoryId, batchQuery)
    }
    // Selection belongs to the full source snapshot; changing a filter must not erase exclusions.
    var selectedIds by remember(channels) {
        mutableStateOf(channels.map { it.id }.toSet())
    }
    val targetChannels = remember(filteredChannels, selectedIds) {
        filteredChannels.filter { it.id in selectedIds }
    }

    val modifiedFields = remember { mutableStateMapOf<ChannelField, BatchFieldAction>() }
    val fieldValues = remember { mutableStateMapOf<ChannelField, String?>() }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<ChannelField?>(null) }
    var returnField by remember { mutableStateOf(ChannelField.ENGINE) }
    val focus = remember { ChannelField.entries.associateWith { FocusRequester() } }
    val listState = rememberLazyListState()

    val inherit = stringResource(CoreR.string.settings_channel_inherit)
    val keep = stringResource(R.string.settings_channel_batch_field_keep)

    val labels = mapOf(
        ChannelField.ENGINE to stringResource(CoreR.string.settings_live_tv_player),
        ChannelField.FORMAT to stringResource(CoreR.string.settings_channel_format),
        ChannelField.RESERVE to stringResource(CoreR.string.settings_channel_reserve),
        ChannelField.EXTRA to stringResource(CoreR.string.settings_channel_extra),
        ChannelField.PREROLL to stringResource(CoreR.string.settings_channel_preroll),
        ChannelField.LATENCY to stringResource(CoreR.string.settings_channel_latency),
        ChannelField.SOFTWARE_AUDIO to stringResource(CoreR.string.settings_channel_software_audio),
        ChannelField.AUDIO_DELAY to stringResource(CoreR.string.settings_channel_audio_sync),
        ChannelField.HLS_BOUNDARIES to stringResource(CoreR.string.settings_channel_hls_boundaries),
        ChannelField.HLS_KEYFRAMES to stringResource(CoreR.string.settings_channel_hls_keyframes),
        ChannelField.HLS_SEGMENTS to stringResource(CoreR.string.settings_channel_hls_segments),
    )
    val hints = mapOf(
        ChannelField.ENGINE to stringResource(CoreR.string.settings_channel_hls_hint),
        ChannelField.RESERVE to stringResource(CoreR.string.settings_channel_reserve_hint),
        ChannelField.EXTRA to stringResource(CoreR.string.settings_channel_extra_hint),
        ChannelField.PREROLL to stringResource(CoreR.string.settings_channel_preroll_hint),
        ChannelField.LATENCY to stringResource(CoreR.string.settings_channel_latency_hint),
        ChannelField.AUDIO_DELAY to stringResource(CoreR.string.settings_channel_audio_hint),
        ChannelField.HLS_BOUNDARIES to stringResource(CoreR.string.settings_channel_hls_boundaries_hint),
        ChannelField.HLS_KEYFRAMES to stringResource(CoreR.string.settings_channel_hls_keyframes_hint),
        ChannelField.HLS_SEGMENTS to stringResource(CoreR.string.settings_channel_hls_segments_hint),
    )
    val numbers = mapOf(
        ChannelField.RESERVE to (1..60).toList(),
        ChannelField.EXTRA to (0..tv.own.owntv.core.settings.LiveBuffer.MAX_EXTRA_SECS).toList(),
        ChannelField.PREROLL to tv.own.owntv.core.settings.LiveBuffer.PREROLL_CHOICES,
        ChannelField.LATENCY to (1..60).toList(),
        ChannelField.AUDIO_DELAY to (-5000..5000 step 50).toList()
    )
    val choices = ChannelField.entries.associateWith { field ->
        when (field) {
            ChannelField.ENGINE -> listOf(
                EnginePreference.EXO_ONLY.name to engineLabel(EnginePreference.EXO_ONLY),
                EnginePreference.MPV_ONLY.name to engineLabel(EnginePreference.MPV_ONLY)
            )
            ChannelField.FORMAT -> listOf(
                ChannelStreamFormat.HLS.name to stringResource(CoreR.string.settings_channel_hls),
                ChannelStreamFormat.TS.name to stringResource(CoreR.string.settings_channel_ts),
                ChannelStreamFormat.AUTO.name to stringResource(CoreR.string.settings_channel_auto_format)
            )
            ChannelField.HLS_BOUNDARIES, ChannelField.HLS_KEYFRAMES, ChannelField.HLS_SEGMENTS -> listOf(
                "false" to stringResource(CoreR.string.settings_channel_hls_standard),
                "true" to stringResource(CoreR.string.settings_channel_hls_enabled)
            )
            ChannelField.SOFTWARE_AUDIO -> listOf(
                "false" to stringResource(CoreR.string.settings_channel_audio_auto),
                "true" to stringResource(CoreR.string.settings_channel_audio_software)
            )
            else -> numbers.getValue(field).map { value ->
                value.toString() to
                    if (field == ChannelField.PREROLL && value == 0) stringResource(CoreR.string.settings_auto)
                    else stringResource(if (field == ChannelField.AUDIO_DELAY) CoreR.string.settings_channel_ms else CoreR.string.settings_channel_seconds, value)
            }
        }
    }

    fun fieldChipText(field: ChannelField): String {
        return when (modifiedFields[field] ?: BatchFieldAction.UNCHANGED) {
            BatchFieldAction.UNCHANGED -> keep
            BatchFieldAction.INHERIT -> inherit
            BatchFieldAction.CUSTOM -> {
                val value = fieldValues[field]
                choices.getValue(field).firstOrNull { it.first == value }?.second ?: value ?: keep
            }
        }
    }

    BackHandler {
        if (isSaving) return@BackHandler
        if (picker != null) picker = null
        else if (categoryPickerOpen) categoryPickerOpen = false
        else if (channelPickerOpen) channelPickerOpen = false
        else onDismiss()
    }

    if (categoryPickerOpen) {
        val categoryItems = listOf("all" to pluralStringResource(R.plurals.settings_channel_batch_category_all, channels.size, channels.size)) +
            categories.map { cat ->
                val count = channels.count { it.categoryId == cat.id }
                cat.id.toString() to pluralStringResource(R.plurals.settings_channel_batch_category_filter, count, cat.name, count)
            }
        val currentSelectedKey = selectedCategoryId?.toString() ?: "all"
        PickerDialog(
            title = stringResource(CoreR.string.content_category_browser_title),
            options = categoryItems,
            selected = currentSelectedKey,
            onSelect = { key ->
                selectedCategoryId = if (key == "all") null else key.toLongOrNull()
                categoryPickerOpen = false
            },
            onDismiss = { categoryPickerOpen = false },
        )
        return
    }

    if (channelPickerOpen) {
        var channelFilterQuery by remember { mutableStateOf("") }
        val displayedChannels = remember(filteredChannels, channelFilterQuery) {
            if (channelFilterQuery.isBlank()) filteredChannels
            else filteredChannels.filter { it.name.contains(channelFilterQuery.trim(), ignoreCase = true) }
        }
        var visibleLimit by remember(channelFilterQuery, selectedCategoryId, batchQuery) { mutableIntStateOf(100) }
        val pickerFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(60)
            runCatching { pickerFocus.requestFocus() }
        }
        OwnTVPopup(onDismissRequest = { channelPickerOpen = false }) {
            Column(
                modifier = Modifier
                    .dialogPanel(width = 680.dp)
                    .trapAllFocusExit()
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.settings_channel_batch_select_channels_title),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SearchBar(
                            channelFilterQuery,
                            { channelFilterQuery = it },
                            placeholder = stringResource(CoreR.string.common_search_hint),
                            modifier = Modifier.fillMaxWidth().focusRequester(pickerFocus),
                            surface = null,
                        )
                    }
                    OwnTVButton(
                        label = stringResource(R.string.settings_channel_batch_select_all),
                        onClick = { selectedIds = selectedIds + displayedChannels.map { it.id } },
                        style = OwnTVButtonStyle.SECONDARY,
                    )
                    OwnTVButton(
                        label = stringResource(R.string.settings_channel_batch_deselect_all),
                        onClick = { selectedIds = selectedIds - displayedChannels.map { it.id }.toSet() },
                        style = OwnTVButtonStyle.SECONDARY,
                    )
                }
                Text(
                    pluralStringResource(R.plurals.settings_channel_batch_selected_count, targetChannels.size, targetChannels.size),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
                val maxPickerHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 260.dp).coerceIn(160.dp, 360.dp)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = maxPickerHeight),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(displayedChannels.take(visibleLimit), key = { it.id }) { ch ->
                        val isSelected = ch.id in selectedIds
                        FocusableSurface(
                            onClick = {
                                selectedIds = if (isSelected) selectedIds - ch.id else selectedIds + ch.id
                            },
                            modifier = Modifier.fillMaxWidth(),
                            selected = isSelected,
                            selectedContainerColor = colors.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    ch.name,
                                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    OwnTVIcon(
                                        OwnTVIcon.STAR,
                                        tint = colors.onPrimaryContainer,
                                        filled = true,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (displayedChannels.size > visibleLimit) item {
                        OwnTVButton(stringResource(R.string.phase23_load_more), onClick = { visibleLimit += 100 })
                    }
                }
                OwnTVButton(
                    stringResource(CoreR.string.common_done),
                    onClick = { channelPickerOpen = false },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        return
    }

    val activeField = picker
    if (activeField != null) {
        val items = listOf("unchanged" to keep, "inherit" to inherit) + choices.getValue(activeField)
        val currentKey = when (modifiedFields[activeField] ?: BatchFieldAction.UNCHANGED) {
            BatchFieldAction.UNCHANGED -> "unchanged"
            BatchFieldAction.INHERIT -> "inherit"
            BatchFieldAction.CUSTOM -> fieldValues[activeField] ?: "unchanged"
        }
        PickerDialog(
            title = labels.getValue(activeField),
            options = items,
            selected = currentKey,
            onSelect = { value ->
                when (value) {
                    "unchanged" -> {
                        modifiedFields[activeField] = BatchFieldAction.UNCHANGED
                        fieldValues.remove(activeField)
                    }
                    "inherit" -> {
                        modifiedFields[activeField] = BatchFieldAction.INHERIT
                        fieldValues.remove(activeField)
                    }
                    else -> {
                        modifiedFields[activeField] = BatchFieldAction.CUSTOM
                        fieldValues[activeField] = value
                    }
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
        return
    }

    val activeFieldEntries = modifiedFields.filter { it.value != BatchFieldAction.UNCHANGED }
    val previewSeparator = stringResource(R.string.settings_channel_batch_separator)
    val itemTemplate = stringResource(R.string.settings_channel_batch_field_item)
    val previewSummary = if (activeFieldEntries.isEmpty()) {
        stringResource(R.string.settings_channel_batch_preview_none)
    } else {
        activeFieldEntries.entries.joinToString(previewSeparator) { (f, _) ->
            itemTemplate.format(labels.getValue(f), fieldChipText(f))
        }
    }

    OwnTVPopup(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Column(
            modifier = Modifier
                .dialogPanel(width = 720.dp)
                .trapAllFocusExit()
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.settings_channel_batch_title),
                color = colors.onSurface,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(R.string.settings_channel_batch_hint),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (lookup is UiLoadState.Loading) Text(stringResource(CoreR.string.channel_versions_loading), color = colors.onSurfaceVariant)
            if (lookup is UiLoadState.Failed) {
                Text(stringResource(R.string.ui_load_failed), color = colors.onSurfaceVariant)
                OwnTVButton(stringResource(CoreR.string.common_retry), onClick = { lookupRetry++ })
            }
            Text(stringResource(R.string.phase23_batch_scope, sourceName,
                scopeSnapshot?.sourceTotal ?: 0, channels.size,
                categories.firstOrNull { it.id == selectedCategoryId }?.name ?: stringResource(R.string.phase23_all_categories),
                batchQuery.ifBlank { stringResource(R.string.phase23_no_search) }, filteredChannels.size,
                filteredChannels.size - targetChannels.size), color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
            SearchBar(batchQuery, { batchQuery = it }, modifier = Modifier.fillMaxWidth(), surface = null)

            // Category filter and selection controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OwnTVButton(
                    label = if (selectedCategoryId == null) {
                        pluralStringResource(R.plurals.settings_channel_batch_category_all, channels.size, channels.size)
                    } else {
                        val catName = categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()
                        pluralStringResource(R.plurals.settings_channel_batch_category_filter, filteredChannels.size, catName, filteredChannels.size)
                    },
                    onClick = { categoryPickerOpen = true },
                    style = OwnTVButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
                OwnTVButton(
                    label = stringResource(R.string.settings_channel_batch_select_all),
                    onClick = { selectedIds = selectedIds + filteredChannels.map { it.id } },
                    style = OwnTVButtonStyle.SECONDARY,
                )
                OwnTVButton(
                    label = stringResource(R.string.settings_channel_batch_deselect_all),
                    onClick = { selectedIds = selectedIds - filteredChannels.map { it.id }.toSet() },
                    style = OwnTVButtonStyle.SECONDARY,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    pluralStringResource(R.plurals.settings_channel_batch_selected_count, targetChannels.size, targetChannels.size),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                OwnTVButton(
                    label = stringResource(R.string.settings_channel_batch_choose_channels),
                    onClick = { channelPickerOpen = true },
                    style = OwnTVButtonStyle.SECONDARY,
                    enabled = filteredChannels.isNotEmpty(),
                )
            }

            // Fields list
            val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 420.dp).coerceIn(100.dp, 300.dp)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(ChannelField.entries, key = { it.name }) { item ->
                    val isCustomized = (modifiedFields[item] ?: BatchFieldAction.UNCHANGED) != BatchFieldAction.UNCHANGED
                    Row2(
                        icon = OwnTVIcon.PLAY,
                        title = labels.getValue(item),
                        desc = hints[item],
                        chip = fieldChipText(item),
                        chevron = true,
                        primaryChip = isCustomized,
                        modifier = Modifier.focusRequester(focus.getValue(item)),
                        onClick = { if (!isSaving) { returnField = item; picker = item } },
                    )
                }
            }

            // Preview
            Text(
                stringResource(R.string.settings_channel_batch_preview_fields, previewSummary),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            if (saveError) {
                Text(
                    stringResource(R.string.ui_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OwnTVButton(
                    stringResource(CoreR.string.common_cancel),
                    onClick = onDismiss,
                    style = OwnTVButtonStyle.SECONDARY,
                    enabled = !isSaving,
                )
                OwnTVButton(
                    if (isSaving) stringResource(CoreR.string.channel_versions_loading)
                    else pluralStringResource(R.plurals.settings_channel_batch_apply_confirm, targetChannels.size, targetChannels.size),
                    onClick = {
                        if (isSaving || activeFieldEntries.isEmpty() || targetChannels.isEmpty()) return@OwnTVButton
                        val confirmedProfileId = scopeSnapshot?.profileId ?: return@OwnTVButton
                        val confirmedTargets = targetChannels.toList()
                        val confirmedFields = activeFieldEntries.toMap()
                        val confirmedValues = fieldValues.toMap()
                        isSaving = true
                        scope.launch {
                            saveError = false
                            try {
                                val ok = vm.phase23ApplyBatch(confirmedProfileId, confirmedTargets) { existing ->
                                    var draft = existing
                                    confirmedFields.forEach { (field, action) ->
                                        when (action) {
                                            BatchFieldAction.UNCHANGED -> Unit
                                            BatchFieldAction.INHERIT -> {
                                                draft = when (field) {
                                                    ChannelField.ENGINE -> draft.copy(engine = null)
                                                    ChannelField.FORMAT -> draft.copy(format = null)
                                                    ChannelField.RESERVE -> draft.copy(reserveSecs = null)
                                                    ChannelField.EXTRA -> draft.copy(extraSecs = null)
                                                    ChannelField.PREROLL -> draft.copy(prerollSecs = null)
                                                    ChannelField.LATENCY -> draft.copy(latencySecs = null)
                                                    ChannelField.SOFTWARE_AUDIO -> draft.copy(softwareAudio = null)
                                                    ChannelField.AUDIO_DELAY -> draft.copy(audioDelayMs = null)
                                                    ChannelField.HLS_BOUNDARIES -> draft.copy(hlsDetectAccessUnits = null)
                                                    ChannelField.HLS_KEYFRAMES -> draft.copy(hlsAllowNonIdrKeyframes = null)
                                                    ChannelField.HLS_SEGMENTS -> draft.copy(hlsPrepareFromSegments = null)
                                                }
                                            }
                                            BatchFieldAction.CUSTOM -> {
                                                val v = confirmedValues[field]
                                                val num = v?.toIntOrNull()
                                                draft = when (field) {
                                                    ChannelField.ENGINE -> draft.copy(engine = EnginePreference.entries.firstOrNull { it.name == v })
                                                    ChannelField.FORMAT -> draft.copy(format = ChannelStreamFormat.entries.firstOrNull { it.name == v })
                                                    ChannelField.RESERVE -> draft.copy(reserveSecs = num)
                                                    ChannelField.EXTRA -> draft.copy(extraSecs = num)
                                                    ChannelField.PREROLL -> draft.copy(prerollSecs = num)
                                                    ChannelField.LATENCY -> draft.copy(latencySecs = num)
                                                    ChannelField.SOFTWARE_AUDIO -> draft.copy(softwareAudio = v?.toBooleanStrictOrNull())
                                                    ChannelField.AUDIO_DELAY -> draft.copy(audioDelayMs = num)
                                                    ChannelField.HLS_BOUNDARIES -> draft.copy(hlsDetectAccessUnits = v?.toBooleanStrictOrNull()?.takeIf { it })
                                                    ChannelField.HLS_KEYFRAMES -> draft.copy(hlsAllowNonIdrKeyframes = v?.toBooleanStrictOrNull()?.takeIf { it })
                                                    ChannelField.HLS_SEGMENTS -> draft.copy(hlsPrepareFromSegments = v?.toBooleanStrictOrNull()?.takeIf { it })
                                                }
                                            }
                                        }
                                    }
                                    draft
                                }
                                if (ok) onDismiss() else saveError = true
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                saveError = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving && activeFieldEntries.isNotEmpty() && targetChannels.isNotEmpty(),
                )
            }
        }
    }
}
