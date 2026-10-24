package tv.own.owntv.features.live

import tv.own.owntv.core.R as CoreR

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.ui.components.*

/** On-demand local catalogue lookup; editing priority never opens a stream. */
@Composable
internal fun ChannelVersionsDialog(
    channel: ChannelEntity, vm: LiveViewModel,
    onPlay: (ChannelEntity) -> Unit,
    onSaveOrder: suspend (List<ChannelEntity>?) -> Boolean,
    onDismiss: () -> Unit,
    playOnly: Boolean = false,
) {
    val settings by vm.channelVersionSettings.collectAsStateWithLifecycle()
    var reload by remember(channel.id) { mutableIntStateOf(0) }
    val catalogue by produceState<UiLoadState<List<ChannelEntity>>>(UiLoadState.Loading(), channel.id, settings, playOnly, reload) {
        val previous = value.valueOrNull
        value = UiLoadState.Loading(previous)
        try { value = UiLoadState.Ready(vm.channelVersions(channel, includeHidden = !playOnly)) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { value = UiLoadState.Failed(previous) }
    }
    val rows = catalogue.valueOrNull
    val loadFailed = catalogue is UiLoadState.Failed
    val loading = catalogue is UiLoadState.Loading
    var draftOrder by remember(channel.id) { mutableStateOf<List<ChannelEntity>?>(null) }
    var orderDirty by remember(channel.id) { mutableStateOf(false) }
    var resetOrder by remember(channel.id) { mutableStateOf(false) }
    LaunchedEffect(catalogue) {
        draftOrder = reconcileVersionDraft(catalogue, draftOrder, orderDirty, resetOrder)
    }
    var saving by remember(channel.id) { mutableStateOf(false) }
    var saveFailed by remember(channel.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rowFocus = remember(channel.id) { mutableMapOf<Long, FocusRequester>() }
    var movedId by remember(channel.id) { mutableStateOf<Long?>(null) }
    var moveDirection by remember(channel.id) { mutableIntStateOf(0) }
    val versions = draftOrder ?: rows
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(draftOrder, movedId) {
        val id = movedId ?: return@LaunchedEffect
        val index = versions?.indexOfFirst { it.id == id } ?: -1
        if (index >= 0) {
            listState.scrollToItem(index)
            repeat(5) {
                withFrameNanos { }
                if (runCatching { rowFocus[id]?.requestFocus() == true }.getOrDefault(false)) return@LaunchedEffect
            }
        }
    }
    var adding by remember(channel.id) { mutableStateOf(false) }
    val closeFocus = remember { FocusRequester() }
    val firstVersionFocus = remember { FocusRequester() }
    OwnTVPopup(onDismissRequest = { if (!saving) onDismiss() }) {
        LaunchedEffect(playOnly, playOnly && !rows.isNullOrEmpty(), adding) {
            if (adding) return@LaunchedEffect
            repeat(5) {
                withFrameNanos { }
                val target = if (playOnly && !rows.isNullOrEmpty()) firstVersionFocus else closeFocus
                if (runCatching { target.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            }
        }
        Column(Modifier.dialogPanel(width = 680.dp).background(Color(0xFF141D29), RoundedCornerShape(20.dp)).longPressMenuGuard().trapAllFocusExit().focusGroup()) {
            Text(stringResource(CoreR.string.channel_versions_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            if (!playOnly) Text(stringResource(if (settings.prioritizeChannelVersions) CoreR.string.channel_versions_active else CoreR.string.channel_versions_inactive),
                color = Color.White, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VersionButton(stringResource(if (playOnly) CoreR.string.content_close else CoreR.string.common_cancel), onClick = onDismiss, modifier = Modifier.focusRequester(closeFocus), enabled = !saving)
                VersionButton(stringResource(CoreR.string.channel_versions_add), onClick = { adding = true }, enabled = !saving)
                if (!playOnly) {
                    VersionButton(stringResource(CoreR.string.channel_versions_reset), onClick = {
                        orderDirty = true; resetOrder = true
                        draftOrder = rows?.sortedWith(compareBy<ChannelEntity> { ChannelVersionPolicy.rank(it.name) }.thenBy { it.sortOrder }.thenBy { it.id })
                    }, enabled = !saving && !loading && rows != null && !loadFailed)
                    VersionButton(stringResource(CoreR.string.common_save), onClick = {
                        if (saving || loading || loadFailed) return@VersionButton
                        val confirmedOrder = if (resetOrder) null else (draftOrder ?: rows)?.toList()
                        saving = true
                        scope.launch {
                            saveFailed = false
                            try {
                                if (onSaveOrder(confirmedOrder)) onDismiss() else saveFailed = true
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { saveFailed = true }
                            finally { saving = false }
                        }
                    }, enabled = !saving && !loading && rows != null && !loadFailed)
                }
            }
            if (!playOnly) Text(stringResource(tv.own.owntv.R.string.ui_versions_draft), color = Color.White, style = MaterialTheme.typography.bodySmall)
            if (saveFailed) Text(stringResource(tv.own.owntv.R.string.ui_save_failed), color = Color.White)
            if (loadFailed) VersionButton(stringResource(CoreR.string.common_retry), onClick = { reload++ }, enabled = !saving)
            Spacer(Modifier.height(12.dp))
            if (versions == null) Text(stringResource(if (loadFailed) tv.own.owntv.R.string.ui_load_failed else CoreR.string.channel_versions_loading), color = Color.White)
            else if (versions.isEmpty()) Text(stringResource(CoreR.string.channel_versions_empty), color = Color.White)
            else LazyColumn(Modifier.heightIn(max = 340.dp), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(versions, key = { _, item -> item.id }) { index, item ->
                    if (playOnly) {
                        VersionButton(item.name, onClick = { onPlay(item) },
                            modifier = Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstVersionFocus) else Modifier))
                        return@itemsIndexed
                    }
                    val itemFocus = remember(rowFocus, item.id) { rowFocus.getOrPut(item.id) { FocusRequester() } }
                    val hidden = CustomizeKeys.channel(item) in settings.hiddenItems
                    val targetButton = when {
                        movedId == item.id && moveDirection < 0 && index > 0 -> -1
                        movedId == item.id && moveDirection > 0 && index < versions.lastIndex -> 1
                        hidden && index > 0 -> -1
                        hidden && index < versions.lastIndex -> 1
                        else -> 0
                    }
                    Column {
                        Text(stringResource(CoreR.string.channel_versions_position, index + 1, item.name), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        if (hidden) Text(stringResource(CoreR.string.channel_versions_hidden), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VersionButton(stringResource(CoreR.string.content_downloads_play), onClick = { onPlay(item) }, modifier = if (targetButton == 0 && !hidden) Modifier.focusRequester(itemFocus) else Modifier, enabled = !hidden && !saving)
                            VersionButton(stringResource(CoreR.string.channel_versions_up), modifier = if (targetButton == -1) Modifier.focusRequester(itemFocus) else Modifier, enabled = !saving && index > 0,
                                onClick = {
                                    val current = draftOrder ?: versions
                                    val updated = moveOrderItem(current, index, index - 1)
                                    draftOrder = updated
                                    orderDirty = true; resetOrder = false; moveDirection = -1; movedId = item.id
                                })
                            VersionButton(stringResource(CoreR.string.channel_versions_down), modifier = if (targetButton == 1) Modifier.focusRequester(itemFocus) else Modifier, enabled = !saving && index < versions.lastIndex,
                                onClick = {
                                    val current = draftOrder ?: versions
                                    val updated = moveOrderItem(current, index, index + 1)
                                    draftOrder = updated
                                    orderDirty = true; resetOrder = false; moveDirection = 1; movedId = item.id
                                })
                            if (CustomizeKeys.channel(item) in settings.channelVersionGroups) {
                                VersionButton(stringResource(CoreR.string.channel_versions_unlink), enabled = !saving && !loading, onClick = {
                                    if (saving) return@VersionButton
                                    saving = true
                                    scope.launch {
                                        saveFailed = false
                                        try {
                                            if (!vm.removeManualChannelVersion(item)) saveFailed = true
                                        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                                        catch (_: Exception) { saveFailed = true }
                                        finally { saving = false }
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
    if (adding) AddChannelVersionDialog(channel, vm) {
        adding = false
    }
}

/** A failed/loading refresh cannot discard user edits or claim that the catalogue is empty. */
internal fun reconcileVersionDraft(
    catalogue: UiLoadState<List<ChannelEntity>>,
    previous: List<ChannelEntity>?,
    dirty: Boolean,
    reset: Boolean,
): List<ChannelEntity>? {
    val fresh = (catalogue as? UiLoadState.Ready<List<ChannelEntity>>)?.value ?: return previous
    if (reset) return fresh.sortedWith(compareBy<ChannelEntity> { ChannelVersionPolicy.rank(it.name) }.thenBy { it.sortOrder }.thenBy { it.id })
    if (!dirty) return fresh
    val byId = fresh.associateBy { it.id }
    val retained = previous.orEmpty().mapNotNull { byId[it.id] }
    val retainedIds = retained.mapTo(hashSetOf()) { it.id }
    return retained + fresh.filter { it.id !in retainedIds }
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
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val candidates by produceState<List<ChannelEntity>?>(emptyList(), channel.id, query, retry) {
        value = null
        delay(250)
        failed = false
        try { value = vm.channelVersionCandidates(channel, query) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true; value = emptyList() }
    }
    val searchFocus = remember { FocusRequester() }
    var isAdding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    OwnTVPopup(onDismissRequest = { if (!isAdding) onDismiss() }) {
        LaunchedEffect(Unit) {
            repeat(5) {
                withFrameNanos { }
                if (runCatching { searchFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            }
        }
        Column(Modifier.dialogPanel(width = 680.dp).background(Color(0xFF141D29), RoundedCornerShape(20.dp))
            .trapAllFocusExit().focusGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(CoreR.string.channel_versions_add), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(CoreR.string.channel_versions_add_hint), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            SearchBar(query, { query = it }, modifier = Modifier.fillMaxWidth().focusRequester(searchFocus), surface = null)
            VersionButton(stringResource(CoreR.string.content_close), onDismiss, enabled = !isAdding)
            if (failed) {
                Text(stringResource(tv.own.owntv.R.string.ui_load_failed), color = Color.White)
                VersionButton(stringResource(CoreR.string.common_retry), onClick = { retry++ })
            } else if (candidates != null && candidates.isNullOrEmpty()) Text(stringResource(CoreR.string.channel_versions_empty), color = Color.White)
            if (candidates == null) Text(stringResource(CoreR.string.channel_versions_loading), color = Color.White)
            else LazyColumn(Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(candidates.orEmpty(), key = { _, item -> item.id }) { _, item ->
                    VersionButton(item.name, onClick = {
                        if (isAdding) return@VersionButton
                        scope.launch {
                            isAdding = true
                            try {
                                if (vm.addChannelVersion(channel, item)) onDismiss() else failed = true
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { failed = true }
                            finally { isAdding = false }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !isAdding)
                }
            }
        }
    }
}
