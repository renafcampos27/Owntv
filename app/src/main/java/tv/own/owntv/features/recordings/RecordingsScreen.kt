package tv.own.owntv.features.recordings

import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import tv.own.owntv.R
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
    val compactLayout = tv.own.owntv.ui.components.rememberCompactLayout()
    val rows by vm.rows.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val externalPlayerOn by vm.externalPlayerOn.collectAsStateWithLifecycle()
    val colors = OwnTVTheme.colors
    val context = LocalContext.current
    LaunchedEffect(vm, context) {
        vm.retryUnavailable.collect {
            android.widget.Toast.makeText(context, R.string.recording_retry_unavailable, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(vm, context) {
        vm.archiveActionUnavailable.collect {
            android.widget.Toast.makeText(context, R.string.recording_archive_resume_unavailable, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(vm, context) {
        vm.playUnavailable.collect {
            android.widget.Toast.makeText(context, R.string.recording_watch_unavailable, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(vm, context) {
        vm.partsRequireInternal.collect {
            android.widget.Toast.makeText(context, R.string.recording_parts_require_internal, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val recoveries = remember(rows) { tv.own.owntv.core.recording.RecordingIntegrity.latestRecoveries(rows) }
    val listRows = remember(rows) { buildRecordingRows(rows) }
    val firstItemId = listRows.firstNotNullOfOrNull { (it as? RecordingListRow.Item)?.recording?.id }

    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val contextFocus = remember { FocusRequester() }

    // Acting on a row can remove it — stopping a recording moves it out of "Recording now", deleting
    // takes it away entirely. Without this, focus escapes to the sidebar every time. Same shape as
    // the Downloads screen's restore.
    var contextId by remember { mutableStateOf<Long?>(null) }
    var contextIndex by remember { mutableStateOf(-1) }
    // The status the acted-on row had when it was acted on. Stop does not remove a row — it moves it
    // from "Recording now" to "Recorded" — so the restore below never ran, while the button that had
    // focus (Stop) was replaced by different ones (Play, Delete) and focus went with it. Comparing
    // the status is what tells the two cases apart: the row survived, but not the thing focused.
    var contextStatus by remember { mutableStateOf<RecordingStatus?>(null) }
    LaunchedEffect(listRows) {
        val target = contextId ?: return@LaunchedEffect
        val items = listRows.filterIsInstance<RecordingListRow.Item>()
        val survivor = items.firstOrNull { it.recording.id == target }
        if (survivor != null) {
            // Only when its buttons have actually changed — the list also updates as bytes arrive,
            // and re-grabbing focus on every tick would drag the user back here mid-navigation.
            if (survivor.recording.status != contextStatus) {
                contextStatus = survivor.recording.status
                withFrameNanos { }
                runCatching { contextFocus.requestFocus() }
            }
            return@LaunchedEffect
        }
        withFrameNanos { }
        if (items.isEmpty()) {
            contextId = null
            contextIndex = -1
            runCatching { firstFocus.requestFocus() }
            return@LaunchedEffect
        }
        val neighbour = items.getOrNull(contextIndex.coerceAtLeast(0)) ?: items.last()
        contextId = neighbour.recording.id
        contextIndex = listRows.indexOfFirst {
            it is RecordingListRow.Item && it.recording.id == neighbour.recording.id
        }
        runCatching { listState.scrollToItem(contextIndex.coerceAtLeast(0)) }
        withFrameNanos { }
        runCatching { contextFocus.requestFocus() }
    }

    // Arriving on the screen has to land focus on the first row itself. The `onEnter` below only
    // fires when focus *moves into* the group, and opening this from More gives it nothing to move
    // from — so the screen opened with nothing focused at all and the first OK press went nowhere,
    // until a stray RIGHT happened to put focus somewhere. Waits a frame so the row exists to take
    // it, and stands aside once a row has been acted on, which the restore above already owns.
    // Embedded, the host owns arrival focus — it lands on its tab strip, and grabbing focus down
    // into this list from here would pull the user past the tabs and the folder button every time.
    LaunchedEffect(firstItemId, embedded) {
        if (embedded || firstItemId == null || contextId != null) return@LaunchedEffect
        withFrameNanos { }
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        modifier = modifier.testTag("owntv_recordings_screen")
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
                    Modifier.padding(horizontal = if (compactLayout) 12.dp else Dimens.ScreenPaddingH, vertical = if (compactLayout) 12.dp else Dimens.ScreenPaddingV)
                },
            ),
    ) {
        if (!embedded) {
            Text(
                stringResource(R.string.recording_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.recording_description),
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.recording_empty),
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
                                recovery = recoveries[recording.id],
                                focusModifier = when {
                                    recording.id == contextId -> Modifier.focusRequester(contextFocus)
                                    recording.id == firstItemId -> Modifier.focusRequester(firstFocus)
                                    else -> Modifier
                                },
                                onPlay = { vm.play(recording) { if (!externalPlayerOn) onFullscreen() } },
                                allowInProgress = !externalPlayerOn,
                                onStop = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.stop(recording) },
                                onCancel = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.cancel(recording) },
                                onPause = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.pauseArchive(recording) },
                                onResume = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.resumeArchive(recording) },
                                onRetry = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.retry(recording) },
                                onDelete = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.delete(recording) },
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
    NOW(R.string.recording_group_now),
    SCHEDULED(R.string.recording_group_scheduled),
    PAUSED(R.string.recording_archive_paused),
    PARTIAL(R.string.recording_group_partial),
    COMPLETED(R.string.recording_group_saved),
    FAILED(R.string.recording_group_failed),
    MISSED(R.string.recording_group_missed),
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
            stringResource(R.string.content_downloads_count, count),
            style = MaterialTheme.typography.labelMedium,
            color = OwnTVTheme.colors.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordingRow(
    recording: RecordingEntity,
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
) {
    val colors = OwnTVTheme.colors
    val compactLayout = tv.own.owntv.ui.components.rememberCompactLayout()
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
                maxLines = if (compactLayout) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                recording.channelName,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = if (compactLayout) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            StatusLine(recording)
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
            OwnTVButton(stringResource(R.string.recording_archive_resume), onClick = onResume, modifier = focusModifier)
        } else when (recording.status) {
            // No icon: it sits next to Delete, and the glyph made Play the wider of the two for no
            // reason a viewer could name.
            RecordingStatus.COMPLETED, RecordingStatus.PARTIAL -> OwnTVButton(
                stringResource(if (tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) R.string.content_downloads_play else R.string.recording_recover_archive),
                onClick = if (tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) onPlay else onRetry,
                modifier = focusModifier,
            )
            RecordingStatus.RECORDING -> OwnTVButton(
                stringResource(if (growing) R.string.recording_watch_in_progress else R.string.recording_stop),
                onClick = if (growing) onPlay else onStop,
                style = OwnTVButtonStyle.SECONDARY,
                modifier = focusModifier,
            )
            RecordingStatus.SCHEDULED -> OwnTVButton(
                stringResource(R.string.common_cancel),
                onClick = onCancel,
                style = OwnTVButtonStyle.SECONDARY,
                modifier = focusModifier,
            )
            RecordingStatus.FAILED, RecordingStatus.MISSED -> OwnTVButton(
                stringResource(if (recording.programmeStopMs <= System.currentTimeMillis()) R.string.recording_recover_archive else R.string.common_retry),
                onClick = onRetry,
                style = OwnTVButtonStyle.SECONDARY,
                modifier = focusModifier,
            )
            RecordingStatus.CANCELLED -> Unit
        }
        if (growing) {
            if (!compactLayout) Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(R.string.recording_stop), onClick = onStop, style = OwnTVButtonStyle.SECONDARY)
        }
        if (recording.archivePaused && recording.status == RecordingStatus.PARTIAL && recording.failure != RecordingFailure.NONE) {
            if (!compactLayout) Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(R.string.recording_recover_archive), onClick = onRetry, style = OwnTVButtonStyle.SECONDARY)
        }
        if (tv.own.owntv.core.recording.ArchiveResumePolicy.canPause(recording)) {
            if (!compactLayout) Spacer(Modifier.width(10.dp))
            OwnTVButton(stringResource(R.string.recording_archive_pause), onClick = onPause, style = OwnTVButtonStyle.SECONDARY)
        }
        if (recording.status == RecordingStatus.PARTIAL && tv.own.owntv.core.recording.RecordingIntegrity.canPlay(recording)) {
            if (!compactLayout) Spacer(Modifier.width(10.dp))
            OwnTVButton(
                stringResource(if (recording.programmeStopMs <= System.currentTimeMillis()) R.string.recording_recover_archive else R.string.common_retry),
                onClick = onRetry,
                style = OwnTVButtonStyle.SECONDARY,
            )
        }
        // Nothing to delete while it is only scheduled: there is no file yet, and Cancel is the
        // action that means "do not do this".
        if (recording.status != RecordingStatus.SCHEDULED) {
            if (!compactLayout) Spacer(Modifier.width(10.dp))
            OwnTVButton(
                stringResource(R.string.common_delete),
                onClick = onDelete,
                style = OwnTVButtonStyle.SECONDARY,
            )
        }
    }
    val rowModifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(colors.surfaceContainerHigh).padding(14.dp)
    if (compactLayout) {
        Column(rowModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = information)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    } else {
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            information()
            Spacer(Modifier.width(12.dp))
            actions()
        }
    }
}

/** The line that says where this recording got to, or why it never did. */
@Composable
private fun StatusLine(recording: RecordingEntity) {
    if (recording.archivePaused && recording.status == RecordingStatus.PARTIAL) {
        Text(stringResource(R.string.recording_archive_paused), style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.recording_archive_retained), style = MaterialTheme.typography.labelSmall)
        return
    }

    val colors = OwnTVTheme.colors
    val context = LocalContext.current
    when (recording.status) {
        // Confirmed media duration and file bytes, never wall-clock time relabelled as captured media.
        RecordingStatus.RECORDING -> Text(
            buildList {
                add(stringResource(R.string.recording_group_now))
                if (recording.capturedDurationMs > 0) add(stringResource(R.string.recording_captured_duration, captureDurationText(recording.capturedDurationMs)))
                if (recording.bytes > 0) add(stringResource(R.string.common_size_mb, sizeMb(recording.bytes)))
            }.joinToString(stringResource(R.string.content_epg_bits_separator)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.primary,
            fontWeight = FontWeight.SemiBold,
        )
        RecordingStatus.SCHEDULED -> Text(
            stringResource(R.string.recording_scheduled_for, whenText(recording.startMs)),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        RecordingStatus.COMPLETED, RecordingStatus.PARTIAL -> Column {
            Text(
                buildList {
                    add(stringResource(if (recording.status == RecordingStatus.PARTIAL) R.string.recording_group_partial else R.string.recording_file_saved))
                    if (recording.capturedDurationMs > 0) {
                        add(stringResource(R.string.recording_captured_duration, captureDurationText(recording.capturedDurationMs)))
                    } else {
                        add(stringResource(R.string.recording_duration_unmeasured))
                    }
                    if (recording.bytes > 0) add(stringResource(R.string.common_size_mb, sizeMb(recording.bytes)))
                }.joinToString(stringResource(R.string.content_epg_bits_separator)),
                style = MaterialTheme.typography.bodySmall,
                color = colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (recording.recoveryOfId != null) {
                Text(stringResource(R.string.recording_archive_copy), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        // The whole point of the Missed group: the reason, in words, not a code.
        RecordingStatus.FAILED, RecordingStatus.MISSED -> Text(
            RecordingRules.displayTextOf(recording.failure, context.resources)
                ?: stringResource(R.string.recording_failed_unknown),
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
            Text(stringResource(R.string.recording_expected_duration, captureDurationText((row.programmeStopMs - row.programmeStartMs).coerceAtLeast(0))),
                style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            val captureWindow = (row.stopMs - row.startMs).coerceAtLeast(0)
            if (row.recoveryOfId == null && captureWindow != row.programmeStopMs - row.programmeStartMs) {
                Text(stringResource(R.string.recording_capture_window, captureDurationText(captureWindow)),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.status == RecordingStatus.FAILED) {
                Text(stringResource(if (row.capturedDurationMs > 0) R.string.recording_captured_duration else R.string.recording_duration_unmeasured,
                    captureDurationText(row.capturedDurationMs)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.gapCount > 0 || row.missingDurationMs > 0) {
                Text(stringResource(R.string.recording_missing_estimate, captureDurationText(row.missingDurationMs), row.gapCount),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            if (row.captureFailure != null && row.captureFailure != RecordingFailure.NONE) {
                RecordingRules.displayTextOf(row.captureFailure!!, res)?.let {
                    Text(stringResource(R.string.recording_capture_failure, it), style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                }
            }
            if (tv.own.owntv.core.recording.RecordingIntegrity.finalizationFailed(row)) {
                Text(stringResource(R.string.recording_finalize_failure,
                    RecordingRules.displayTextOf(row.finalizationFailure!!, res) ?: stringResource(R.string.recording_failed_unknown)),
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                if (row.bytes > 0) Text(stringResource(R.string.recording_retained_capture),
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            } else if (row.captureFailure == null && row.status == RecordingStatus.PARTIAL && row.failure != RecordingFailure.NONE) {
                RecordingRules.displayTextOf(row.failure, res)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
                }
            }
            if (row.capturedDurationMs <= 0 && row.status != RecordingStatus.FAILED) {
                Text(stringResource(R.string.recording_integrity_unverified), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
        if (recovery != null) {
            val status = stringResource(when (recovery.status) {
                RecordingStatus.SCHEDULED -> R.string.recording_group_scheduled
                RecordingStatus.RECORDING -> R.string.recording_group_now
                RecordingStatus.COMPLETED -> R.string.recording_file_saved
                RecordingStatus.PARTIAL -> R.string.recording_group_partial
                RecordingStatus.FAILED -> R.string.recording_group_failed
                RecordingStatus.MISSED -> R.string.recording_group_missed
                RecordingStatus.CANCELLED -> R.string.recording_recovery_cancelled
            })
            Text(stringResource(R.string.recording_recovery_state, recovery.recoveryAttempt, status),
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

@Composable
private fun StorageBar(info: RecordingStorageInfo) {
    val colors = OwnTVTheme.colors
    val compactLayout = tv.own.owntv.ui.components.rememberCompactLayout()
    val used = (info.totalBytes - info.freeBytes).coerceAtLeast(0L)
    val fraction = if (info.totalBytes > 0) (used.toFloat() / info.totalBytes).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth()) {
        RecordingStorageHeader(compactLayout) {
            Text(
                stringResource(R.string.content_downloads_storage),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            if (!compactLayout) Spacer(Modifier.weight(1f))
            Text(
                if (info.known) stringResource(
                    R.string.content_downloads_storage_free,
                    gigabytes(info.freeBytes),
                    gigabytes(info.totalBytes),
                ) else stringResource(R.string.media_space_unknown),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordingStorageHeader(compact: Boolean, content: @Composable RowScope.() -> Unit) {
    if (compact) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
