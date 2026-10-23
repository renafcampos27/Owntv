package tv.own.owntv.features.recordings

import tv.own.owntv.ui.components.FocusRequestGuard
import tv.own.owntv.ui.components.cancelPendingFocusOnInput
import tv.own.owntv.ui.components.requestBoundedFocus
import tv.own.owntv.ui.components.listFocusTarget
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import tv.own.owntv.core.R as CoreR
import tv.own.owntv.R

import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.core.database.entity.RecordingEntity
import tv.own.owntv.core.model.RecordingStatus
import tv.own.owntv.core.model.RecordingFailure
import tv.own.owntv.core.recording.RecordingRules
import tv.own.owntv.core.recording.RecordingStorageInfo
import tv.own.owntv.ui.components.ContentPanelFill
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.components.trapVerticalFocusExit
import tv.own.owntv.ui.format.formatBestDateTime
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme
import java.text.NumberFormat
import java.util.Locale

/**
 * Everything the recorder has done, is doing, or could not do.
 *
 * Saved and partial files stay playable; failed and missed captures retain their reason.
 * Archive recovery, when available, creates a separate file and preserves the captured original.
 */
@Composable
fun RecordingsScreen(
    onFullscreen: () -> Unit,
    onChildFocused: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Draw only the list. Downloads hosts this as its **Live TV** tab and already owns the panel,
     * the title, the storage bar and the folder button — a recording is a download of a live
     * channel, so it shares that screen's chrome rather than repeating it inside it.
     */
    embedded: Boolean = false,
    vm: RecordingsViewModel = koinViewModel(),
) {

    val rows by vm.rows.collectAsStateWithLifecycle()
    val unarmedIds by vm.unarmedRecordingIds.collectAsStateWithLifecycle()
    val rearmingIds by vm.rearmingIds.collectAsStateWithLifecycle()
    val deletingIds by vm.deletingIds.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val unavailableFiles by vm.unavailableFiles.collectAsStateWithLifecycle()
    val externalPlayerOn by vm.externalPlayerOn.collectAsStateWithLifecycle()
    val colors = OwnTVTheme.colors
    val context = LocalContext.current
    LaunchedEffect(vm, context) {
        vm.fileUnavailable.collect { availability ->
            tv.own.owntv.core.storage.MediaAvailabilityProbe.text(availability, context.resources)?.let {
                android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val playbackLifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm, playbackLifecycle) {
        vm.onPlaybackForegroundChanged(playbackLifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> vm.onPlaybackForegroundChanged(true)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> vm.onPlaybackForegroundChanged(false)
                else -> Unit
            }
        }
        playbackLifecycle.addObserver(observer)
        onDispose {
            playbackLifecycle.removeObserver(observer)
            vm.onPlaybackForegroundChanged(false)
        }
    }
    LaunchedEffect(vm, context) {
        vm.actionFailures.collect { notice ->
            if (vm.acceptsActionNotice(notice)) {
                tv.own.owntv.core.recording.RecordingRules.displayTextOf(notice.reason, context.resources)?.let { text ->
                    android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(vm, context) {
        vm.playUnavailable.collect {
            android.widget.Toast.makeText(context, CoreR.string.recording_watch_unavailable, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(vm, context) {
        vm.partsRequireInternal.collect {
            android.widget.Toast.makeText(context, CoreR.string.recording_parts_require_internal, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(vm, context) {
        vm.rearmUnavailable.collect {
            android.widget.Toast.makeText(context, CoreR.string.recording_rearm_failed, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(vm, context) {
        vm.deleteResult.collect { result ->
            val messageRes = when (result) {
                tv.own.owntv.core.recording.RecordingDeleteResult.SUCCESS -> null
                tv.own.owntv.core.recording.RecordingDeleteResult.IN_USE -> CoreR.string.recording_delete_in_use
                tv.own.owntv.core.recording.RecordingDeleteResult.STORAGE_UNAVAILABLE -> CoreR.string.recording_delete_storage_unavailable
                tv.own.owntv.core.recording.RecordingDeleteResult.PERMISSION_DENIED -> CoreR.string.recording_delete_permission_denied
                tv.own.owntv.core.recording.RecordingDeleteResult.DELETE_FAILED -> CoreR.string.recording_delete_failed
                tv.own.owntv.core.recording.RecordingDeleteResult.STALE_SELECTION -> CoreR.string.recording_delete_stale
            }
            if (messageRes != null) {
                android.widget.Toast.makeText(context, messageRes, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val recoveries = remember(rows) { tv.own.owntv.core.recording.RecordingIntegrity.latestRecoveries(rows) }
    val listRows = remember(rows) { buildRecordingRows(rows) }
    val firstItemId = listRows.firstNotNullOfOrNull { (it as? RecordingListRow.Item)?.recording?.id }

    val focusProfileId by vm.focusProfileId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val contextFocus = remember { FocusRequester() }

    // Acting on a row can remove it — stopping a recording moves it out of "Recording now", deleting
    // takes it away entirely. Without this, focus escapes to the sidebar every time. Same shape as
    // the Downloads screen's restore.
    var contextId by remember(focusProfileId) { mutableStateOf<Long?>(null) }
    var contextIndex by remember(focusProfileId) { mutableStateOf(-1) }
    val actionFocusGuard = remember { FocusRequestGuard() }
    var contextRevision by remember { mutableStateOf(0L) }
    // The status the acted-on row had when it was acted on. Stop does not remove a row — it moves it
    // from "Recording now" to "Recorded" — so the restore below never ran, while the button that had
    // focus (Stop) was replaced by different ones (Play, Delete) and focus went with it. Comparing
    // the status is what tells the two cases apart: the row survived, but not the thing focused.
    var contextStatus by remember { mutableStateOf<RecordingStatus?>(null) }
    var contextNeedsRearm by remember { mutableStateOf(false) }
    var contextRearming by remember { mutableStateOf(false) }
    LaunchedEffect(listRows, unarmedIds, rearmingIds, focusProfileId) {
        val target = contextId ?: return@LaunchedEffect
        if (contextIndex < 0 || contextRevision != actionFocusGuard.revision) {
            contextIndex = -1
            return@LaunchedEffect
        }
        val items = listRows.filterIsInstance<RecordingListRow.Item>()
        val survivor = items.firstOrNull { it.recording.id == target }
        if (survivor != null) {
            // Only when its buttons have actually changed — the list also updates as bytes arrive,
            // and re-grabbing focus on every tick would drag the user back here mid-navigation.
            if (survivor.recording.id !in rearmingIds && (survivor.recording.status != contextStatus ||
                (survivor.recording.id in unarmedIds) != contextNeedsRearm || contextRearming)) {
                contextRearming = false
                contextNeedsRearm = survivor.recording.id in unarmedIds
                contextStatus = survivor.recording.status
                contextFocus.requestBoundedFocus(actionFocusGuard, contextRevision)
                contextIndex = -1
            }
            return@LaunchedEffect
        }
        withFrameNanos { }
        if (items.isEmpty()) {
            contextId = null
            contextIndex = -1
            firstFocus.requestBoundedFocus(actionFocusGuard, contextRevision)
            return@LaunchedEffect
        }
        val neighbour = listFocusTarget(listRows.map { (it as? RecordingListRow.Item)?.recording?.id }, target, contextIndex)
            ?: return@LaunchedEffect
        contextId = neighbour.id
        listState.scrollToItem(neighbour.visualIndex)
        contextFocus.requestBoundedFocus(actionFocusGuard, contextRevision)
        contextIndex = -1
    }

    // Arriving on the screen has to land focus on the first row itself. The `onEnter` below only
    // fires when focus *moves into* the group, and opening this from More gives it nothing to move
    // from — so the screen opened with nothing focused at all and the first OK press went nowhere,
    // until a stray RIGHT happened to put focus somewhere. Waits a frame so the row exists to take
    // it, and stands aside once a row has been acted on, which the restore above already owns.
    // Embedded, the host owns arrival focus — it lands on its tab strip, and grabbing focus down
    // into this list from here would pull the user past the tabs and the folder button every time.
    var arrivalDone by remember { mutableStateOf(false) }
    LaunchedEffect(firstItemId, embedded) {
        if (arrivalDone || embedded || firstItemId == null || contextId != null) return@LaunchedEffect
        arrivalDone = true
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        modifier = modifier.cancelPendingFocusOnInput(actionFocusGuard).testTag("owntv_recordings_screen")
            .fillMaxSize()
            .then(if (embedded) Modifier else Modifier.roundedPanel(fillColor = ContentPanelFill))
            // Both of these belong to whoever owns the screen, and embedded that is Downloads. Kept
            // here they made a second trap inside the host's: once focus was in this list, Up could
            // not climb back out to the tab strip or the folder button — the list became a dead end.
            .then(
                if (embedded) {
                    Modifier
                } else {
                    Modifier
                        .focusProperties { onEnter = { runCatching { firstFocus.requestFocus() } } }
                        .trapVerticalFocusExit()
                },
            )
            .focusGroup()
            .onFocusChanged { if (it.hasFocus) onChildFocused() }
            .then(
                if (embedded) {
                    Modifier
                } else {
                    Modifier.padding(horizontal = Dimens.ScreenPaddingH, vertical = Dimens.ScreenPaddingV)
                },
            ),
    ) {
        if (!embedded) {
            Text(
                stringResource(CoreR.string.recording_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(CoreR.string.recording_description),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))

            storage?.let {
                StorageBar(it)
                Spacer(Modifier.height(16.dp))
            }
        }

        if (listRows.isEmpty()) {
            Box(Modifier.fillMaxSize().focusRequester(firstFocus).focusable(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(CoreR.string.recording_empty),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.focusGroup(),
            ) {
                itemsIndexed(listRows, key = { _, r -> r.key }) { index, r ->
                    when (r) {
                        is RecordingListRow.Header -> SectionHeader(r.group, r.count)
                        is RecordingListRow.Item -> {
                            val recording = r.recording
                            RecordingRow(
                                recording = recording,
                                availability = unavailableFiles[recording.id],
                                needsRearm = recording.id in unarmedIds,
                                rearming = recording.id in rearmingIds,
                                onRearm = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status;
                                    contextNeedsRearm = true; contextRearming = true; vm.rearm(recording) },
                                recovery = recoveries[recording.id],
                                focusModifier = when {
                                    recording.id == contextId -> Modifier.focusRequester(contextFocus)
                                    recording.id == firstItemId -> Modifier.focusRequester(firstFocus)
                                    else -> Modifier
                                },
                                onPlay = { vm.play(recording) { if (!externalPlayerOn) onFullscreen() } },
                                allowInProgress = !externalPlayerOn,
                                onStop = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.stop(recording) },
                                onCancel = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.cancel(recording) },
                                onPause = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.pauseArchive(recording) },
                                onResume = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.resumeArchive(recording) },
                                onRetry = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.retry(recording) },
                                onDelete = { contextId = recording.id; contextIndex = index; contextRevision = actionFocusGuard.revision; contextStatus = recording.status; vm.delete(recording) },
                                deleting = recording.id in deletingIds,
                            )
                        }
                    }
                }
            }
        }
    }

    // Back returns to the More hub rather than to the rail, the same way every other More page does.
    // Not when embedded: as a tab inside Downloads there is no page of its own to leave, and taking
    // Back here swallowed it — the remote's Back key did nothing at all on the whole screen.
    if (!embedded) androidx.activity.compose.BackHandler(onBack = onBack)
}

/** Most urgent groups first; partial captures are separate from saved files. */
private enum class RecordingGroup(val labelRes: Int) {
    NOW(CoreR.string.recording_group_now),
    SCHEDULED(CoreR.string.recording_group_scheduled),
    PAUSED(CoreR.string.recording_archive_paused),
    PARTIAL(CoreR.string.recording_group_partial),
    COMPLETED(CoreR.string.recording_group_saved),
    FAILED(CoreR.string.recording_group_failed),
    MISSED(CoreR.string.recording_group_missed),
}

private sealed interface RecordingListRow {
    val key: String

    data class Header(val group: RecordingGroup, val count: Int) : RecordingListRow {
        override val key get() = "rhdr_$group"
    }

    data class Item(val recording: RecordingEntity) : RecordingListRow {
        override val key get() = "r_${recording.id}"
    }
}

/**
 * Group the rows, with a header before each group that has anything in it.
 *
 * Cancelled recordings are not shown at all. The user cancelled it; a list that keeps reminding them
 * of something they deliberately called off is a list they stop reading.
 */
private fun buildRecordingRows(recordings: List<RecordingEntity>): List<RecordingListRow> {
    val now = recordings.filter { it.status == RecordingStatus.RECORDING }
    // Soonest first: a scheduled list is read forwards, unlike the finished ones.
    val scheduled = recordings.filter { it.status == RecordingStatus.SCHEDULED }.sortedBy { it.startMs }
    val completed = recordings.filter { it.status == RecordingStatus.COMPLETED }
    val partial = recordings.filter { it.status == RecordingStatus.PARTIAL && !it.archivePaused }
    val paused = recordings.filter { it.archivePaused && it.status == RecordingStatus.PARTIAL }
    val failed = recordings.filter { it.status == RecordingStatus.FAILED }
    val missed = recordings.filter { it.status == RecordingStatus.MISSED }
    return buildList {
        listOf(
            RecordingGroup.NOW to now,
            RecordingGroup.SCHEDULED to scheduled,
            RecordingGroup.PAUSED to paused,
            RecordingGroup.PARTIAL to partial,
            RecordingGroup.COMPLETED to completed,
            RecordingGroup.FAILED to failed,
            RecordingGroup.MISSED to missed,
        ).forEach { (group, list) ->
            if (list.isNotEmpty()) {
                add(RecordingListRow.Header(group, list.size))
                list.forEach { add(RecordingListRow.Item(it)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(group: RecordingGroup, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Text(
            stringResource(group.labelRes).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = OwnTVTheme.colors.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(CoreR.string.content_downloads_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = OwnTVTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordingRow(
    recording: RecordingEntity,
    availability: tv.own.owntv.core.storage.MediaAvailability?,
    needsRearm: Boolean,
    rearming: Boolean,
    onRearm: () -> Unit,
    focusModifier: Modifier,
    recovery: RecordingEntity?,
    onPlay: () -> Unit,
    allowInProgress: Boolean,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit,
    deleting: Boolean = false,
) {
    val colors = OwnTVTheme.colors

    val growing = allowInProgress && recording.status == RecordingStatus.RECORDING &&
        !recording.archivePaused && recording.hlsCheckpoint != null && recording.capturedDurationMs > 0
    val information: @Composable RowScope.() -> Unit = {
        Box(
            Modifier
                .size(56.dp, 78.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            if (!recording.channelIconUrl.isNullOrBlank()) {
                AsyncImage(model = recording.channelIconUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                OwnTVIcon(OwnTVIcon.LIVE_TV, tint = colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                recording.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                recording.channelName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            if (availability == null) StatusLine(recording, needsRearm)
            else tv.own.owntv.core.storage.MediaAvailabilityProbe.text(availability, LocalContext.current.resources)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            IntegrityDetails(recording, recovery)
            // The whole path, not a crumb. "TV" says which folder inside the download root; it does
            // not say which disk, and on a television with an internal drive and a USB stick that is
            // the only part worth reading. Two lines so a long path is shown rather than clipped.
            recording.filePath?.takeIf { it.isNotBlank() }?.let { path ->
                Spacer(Modifier.height(2.dp))
                Text(
                    path,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        // One primary action per state, and never more than the state can honestly offer.
        if (recording.archivePaused && recording.status == RecordingStatus.PARTIAL) {
            OwnTVButton(stringResource(CoreR.string.recording_archive_resume), onClick = onResume, modifier = focusModifier)
        } else when (recording.status) {
            // No icon: it sits next to Delete, and the glyph made Play the wider of the two for no
            // reason a viewer could name.
            RecordingStatus.COMPLETED, RecordingStatus.PARTIAL -> OwnTVButton(
                stringResource(if (tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) CoreR.string.content_downloads_play else CoreR.string.recording_recover_archive),
                onClick = if (tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) onPlay else onRetry,
                modifier = focusModifier,
            )
            RecordingStatus.RECORDING -> OwnTVButton(
                stringResource(if (growing) CoreR.string.recording_watch_in_progress else CoreR.string.recording_stop),
                onClick = if (growing) onPlay else onStop,
                style = OwnTVButtonStyle.SECONDARY,
                modifier = focusModifier,
            )
            RecordingStatus.SCHEDULED -> {
                if (needsRearm) {
                    OwnTVButton(stringResource(CoreR.string.recording_rearm), onClick = onRearm,
                        enabled = !rearming, modifier = focusModifier)
                    Spacer(Modifier.width(10.dp))
                }
                OwnTVButton(stringResource(CoreR.string.common_cancel), onClick = onCancel,
                    enabled = !rearming, style = OwnTVButtonStyle.SECONDARY,
                    modifier = if (needsRearm) Modifier else focusModifier)
            }
            RecordingStatus.FAILED, RecordingStatus.MISSED -> OwnTVButton(
                stringResource(if (recording.programmeStopMs <= System.currentTimeMillis()) CoreR.string.recording_recover_archive else CoreR.string.common_retry),
                onClick = onRetry,
                style = OwnTVButtonStyle.SECONDARY,
                modifier = focusModifier,
            )
            RecordingStatus.CANCELLED -> Unit
        }
        if (growing) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(CoreR.string.recording_stop), onClick = onStop, style = OwnTVButtonStyle.SECONDARY)
        }
        if (recording.archivePaused && recording.status == RecordingStatus.PARTIAL && recording.failure != RecordingFailure.NONE) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(CoreR.string.recording_recover_archive), onClick = onRetry, style = OwnTVButtonStyle.SECONDARY)
        }
        if (tv.own.owntv.core.recording.ArchiveResumePolicy.canPause(recording)) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(CoreR.string.recording_archive_pause), onClick = onPause, style = OwnTVButtonStyle.SECONDARY)
        }
        if (recording.status == RecordingStatus.PARTIAL && tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(
                stringResource(if (recording.programmeStopMs <= System.currentTimeMillis()) CoreR.string.recording_recover_archive else CoreR.string.common_retry),
                onClick = onRetry,
                style = OwnTVButtonStyle.SECONDARY,
            )
        }
        // Nothing to delete while it is only scheduled: there is no file yet, and Cancel is the
        // action that means "do not do this".
        if (recording.status != RecordingStatus.SCHEDULED) {
            Spacer(Modifier.width(10.dp))
            OwnTVButton(
                stringResource(CoreR.string.common_delete),
                onClick = onDelete,
                enabled = !deleting,
                style = OwnTVButtonStyle.SECONDARY,
            )
        }
    }
    val rowModifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(colors.surfaceContainerHigh).padding(14.dp)
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            information()
            Spacer(Modifier.width(12.dp))
            actions()
        }
}

/** The line that says where this recording got to, or why it never did. */
@Composable
private fun StatusLine(recording: RecordingEntity, needsRearm: Boolean) {
    if (needsRearm && recording.status == RecordingStatus.SCHEDULED) {
        Text(stringResource(CoreR.string.recording_needs_rearm), style = MaterialTheme.typography.bodySmall,
            color = OwnTVTheme.colors.primary, fontWeight = FontWeight.SemiBold)
        Text(stringResource(CoreR.string.recording_scheduled_for, whenText(recording.startMs)),
            style = MaterialTheme.typography.labelSmall, color = OwnTVTheme.colors.onSurfaceVariant)
        return
    }
    if (recording.archivePaused && recording.status == RecordingStatus.PARTIAL) {
        Text(stringResource(CoreR.string.recording_archive_paused), style = MaterialTheme.typography.labelSmall)
        Text(stringResource(CoreR.string.recording_archive_retained), style = MaterialTheme.typography.labelSmall)
        return
    }

    val colors = OwnTVTheme.colors
    val context = LocalContext.current
    when (recording.status) {
        // Confirmed media duration and file bytes, never wall-clock time relabelled as captured media.
        RecordingStatus.RECORDING -> Text(
            buildList {
                add(stringResource(CoreR.string.recording_group_now))
                if (recording.capturedDurationMs > 0) add(stringResource(CoreR.string.recording_captured_duration, captureDurationText(recording.capturedDurationMs)))
                if (recording.bytes > 0) add(stringResource(CoreR.string.common_size_mb, sizeMb(recording.bytes)))
            }.joinToString(stringResource(CoreR.string.content_epg_bits_separator)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.primary,
            fontWeight = FontWeight.SemiBold,
        )
        RecordingStatus.SCHEDULED -> Text(
            stringResource(CoreR.string.recording_scheduled_for, whenText(recording.startMs)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        RecordingStatus.COMPLETED, RecordingStatus.PARTIAL -> Column {
            Text(
                buildList {
                    add(stringResource(if (recording.status == RecordingStatus.PARTIAL) CoreR.string.recording_group_partial else CoreR.string.recording_file_saved))
                    if (recording.capturedDurationMs > 0) {
                        add(stringResource(CoreR.string.recording_captured_duration, captureDurationText(recording.capturedDurationMs)))
                    } else {
                        add(stringResource(CoreR.string.recording_duration_unmeasured))
                    }
                    if (recording.bytes > 0) add(stringResource(CoreR.string.common_size_mb, sizeMb(recording.bytes)))
                }.joinToString(stringResource(CoreR.string.content_epg_bits_separator)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (recording.recoveryOfId != null) {
                Text(stringResource(CoreR.string.recording_archive_copy), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        // The whole point of the Missed group: the reason, in words, not a code.
        RecordingStatus.FAILED, RecordingStatus.MISSED -> Text(
            RecordingRules.displayTextOf(recording.failure, context.resources)
                ?: stringResource(CoreR.string.recording_failed_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFEF4444),
        )
        RecordingStatus.CANCELLED -> Unit
    }
}

/** No disk scans or media probing during composition: all evidence comes from durable capture data. */
@Composable
private fun IntegrityDetails(row: RecordingEntity, recovery: RecordingEntity?) {
    val colors = OwnTVTheme.colors
    val res = LocalContext.current.resources
    Column {
        if (row.status == RecordingStatus.RECORDING || row.status == RecordingStatus.PARTIAL ||
            row.status == RecordingStatus.COMPLETED || row.status == RecordingStatus.FAILED) {
            Text(stringResource(CoreR.string.recording_expected_duration, captureDurationText((row.programmeStopMs - row.programmeStartMs).coerceAtLeast(0))),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            val captureWindow = (row.stopMs - row.startMs).coerceAtLeast(0)
            if (row.recoveryOfId == null && captureWindow != row.programmeStopMs - row.programmeStartMs) {
                Text(stringResource(CoreR.string.recording_capture_window, captureDurationText(captureWindow)),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.status == RecordingStatus.FAILED) {
                Text(stringResource(if (row.capturedDurationMs > 0) CoreR.string.recording_captured_duration else CoreR.string.recording_duration_unmeasured,
                    captureDurationText(row.capturedDurationMs)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.gapCount > 0 || row.missingDurationMs > 0) {
                Text(stringResource(CoreR.string.recording_missing_estimate, captureDurationText(row.missingDurationMs), row.gapCount),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.captureFailure != null && row.captureFailure != RecordingFailure.NONE) {
                RecordingRules.displayTextOf(row.captureFailure!!, res)?.let {
                    Text(stringResource(CoreR.string.recording_capture_failure, it), style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                }
            }
            if (tv.own.owntv.core.recording.RecordingIntegrity.finalizationFailed(row)) {
                Text(stringResource(CoreR.string.recording_finalize_failure,
                    RecordingRules.displayTextOf(row.finalizationFailure!!, res) ?: stringResource(CoreR.string.recording_failed_unknown)),
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                if (row.bytes > 0) Text(stringResource(CoreR.string.recording_retained_capture),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            } else if (row.captureFailure == null && row.status == RecordingStatus.PARTIAL && row.failure != RecordingFailure.NONE) {
                RecordingRules.displayTextOf(row.failure, res)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                }
            }
            if (row.capturedDurationMs <= 0 && row.status != RecordingStatus.FAILED) {
                Text(stringResource(CoreR.string.recording_integrity_unverified), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        if (recovery != null) {
            val status = stringResource(when (recovery.status) {
                RecordingStatus.SCHEDULED -> CoreR.string.recording_group_scheduled
                RecordingStatus.RECORDING -> CoreR.string.recording_group_now
                RecordingStatus.COMPLETED -> CoreR.string.recording_file_saved
                RecordingStatus.PARTIAL -> CoreR.string.recording_group_partial
                RecordingStatus.FAILED -> CoreR.string.recording_group_failed
                RecordingStatus.MISSED -> CoreR.string.recording_group_missed
                RecordingStatus.CANCELLED -> CoreR.string.recording_recovery_cancelled
            })
            Text(stringResource(CoreR.string.recording_recovery_state, recovery.recoveryAttempt, status),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

/** Megabytes to one decimal, formatted for the locale. Same shape as the Downloads screen's. */
private fun sizeMb(bytes: Long): String =
    java.text.NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(bytes / 1_048_576.0)

internal fun splitEstimatedDuration(durationMs: Long): Pair<Long, Long> {
    val totalMinutes = durationMs.coerceAtLeast(0) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return Pair(hours, minutes)
}

@Composable
private fun estimatedDurationText(durationMs: Long): String {
    val (hours, minutes) = splitEstimatedDuration(durationMs)
    return when {
        hours > 0L && minutes > 0L -> stringResource(R.string.recording_storage_hours_minutes, hours, minutes)
        hours > 0L -> stringResource(R.string.recording_storage_hours, hours)
        else -> stringResource(R.string.recording_storage_minutes, minutes)
    }
}

@Composable
private fun StorageBar(info: RecordingStorageInfo) {
    val colors = OwnTVTheme.colors

    val used = (info.totalBytes - info.freeBytes).coerceAtLeast(0L)
    val fraction = if (info.totalBytes > 0) (used.toFloat() / info.totalBytes).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth()) {
        RecordingStorageHeader() {
            Text(
                stringResource(CoreR.string.content_downloads_storage),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (info.known) stringResource(
                    CoreR.string.content_downloads_storage_free,
                    gigabytes(info.freeBytes),
                    gigabytes(info.totalBytes),
                ) else stringResource(CoreR.string.media_space_unknown),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.surfaceContainerLowest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.primary),
            )
        }
        if (info.known) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.recording_storage_usable, gigabytes(info.writableBytes)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (info.writableBytes > 0L) colors.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
                Text(
                    stringResource(
                        R.string.recording_storage_estimated_capacity,
                        estimatedDurationText(info.estimatedRemainingMs),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (info.writableBytes > 0L) colors.primary else colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.recording_storage_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}

/**
 * When a scheduled recording starts — day, date and time.
 *
 * A skeleton through the app's own formatter rather than a date and a time glued together: the order
 * of the parts, the separator and whether the clock is 12- or 24-hour are all decided by the user's
 * locale, and concatenating them here would put the time in the wrong place in several of the
 * languages OwnTV ships.
 */
@Composable
private fun whenText(atMs: Long): String =
    formatBestDateTime(LocalContext.current, "EEEdMMMjm", atMs)

private fun decimal(value: Double): String = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
}.format(value)

private fun gigabytes(bytes: Long): String = decimal(bytes.coerceAtLeast(0) / 1_073_741_824.0)

@Composable
private fun RecordingStorageHeader(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)
}
