package tv.own.owntv.features.epg

import tv.own.owntv.core.R as CoreR

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.features.live.CatchupOwner
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.format.rememberBestDateFormatter
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
fun MiniGuideOverlay(
    channels: List<ChannelEntity>, currentId: Long, contextKey: Any,
    liveVm: LiveViewModel,
    onLive: (ChannelEntity) -> Unit,
    onArchive: (ChannelEntity, EpgProgrammeEntity, CatchupOwner) -> Unit,
    onZap: (Int) -> Unit, onDismiss: () -> Unit,
) {
    if (channels.isEmpty()) return
    val openedContext = remember { contextKey }
    LaunchedEffect(contextKey) { if (openedContext != contextKey) onDismiss() }
    var selectedId by remember { mutableLongStateOf(currentId) }
    val index = channels.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val channel = channels[index]
    var cursor by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var retry by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()
    val oldest = tv.own.owntv.core.epg.GuideHistoryPolicy.windowStart(now, channel.catchupDays)
    val latest = now + 7 * 86_400_000L
    LaunchedEffect(channel.id) { cursor = cursor.coerceIn(oldest, latest - 1) }
    val window = guideReadWindow(cursor, oldest, latest)
    val owner = remember(channel, contextKey, window, retry) { liveVm.catchupOwner(channel) }
    var failed by remember(channel, contextKey, window, retry) { mutableStateOf(false) }
    val rows by produceState<List<EpgProgrammeEntity>?>(null, channel, contextKey, window, retry) {
        value = null
        try {
            val loaded = liveVm.miniGuideProgrammes(channel, window.first, window.second)
            coroutineContext.ensureActive()
            value = loaded
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { coroutineContext.ensureActive(); value = emptyList(); failed = true }
    }
    // Loading failures have a separate, retryable presentation; empty guide stays an ordinary gap.
    val colours = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    val formatTime = rememberSystemTimeFormatter()
    val formatDate = rememberBestDateFormatter("EEEdMMM")
    val programme = rows?.let { GuideProgrammeCells.at(it, cursor, window.first, window.second) }
    BackHandler { onDismiss() }
    LaunchedEffect(Unit) {
        repeat(3) {
            kotlinx.coroutines.delay(60)
            if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    OwnTVPopup(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.padding(24.dp).dialogPanel(width = 760.dp)
                .focusRequester(focus).onPreviewKeyEvent { event ->
                    val handled = event.key in listOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight,
                        Key.Enter, Key.DirectionCenter, Key.NumPadEnter, Key.ChannelUp, Key.ChannelDown)
                    if (!handled) return@onPreviewKeyEvent false
                    if (openedContext != contextKey) return@onPreviewKeyEvent true
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                    when (event.key) {
                        Key.DirectionUp -> selectedId = channels[(index - 1).coerceAtLeast(0)].id
                        Key.DirectionDown -> selectedId = channels[(index + 1).coerceAtMost(channels.lastIndex)].id
                        Key.ChannelUp -> onZap(-1)
                        Key.ChannelDown -> onZap(1)
                        Key.DirectionLeft, Key.DirectionRight -> {
                            val delta = if (event.key == Key.DirectionLeft) -1 else 1
                            val next = rows?.let { GuideProgrammeCells.adjacentTime(it, cursor, delta, window.first, window.second) }
                            cursor = if (next == null || (delta < 0 && next >= cursor) || (delta > 0 && next <= cursor))
                                (cursor + delta * 3_600_000L).coerceIn(oldest, latest - 1) else next.coerceIn(oldest, latest - 1)
                        }
                        else -> {
                            if (failed) { failed = false; retry++ }
                            else if (programme != null && liveVm.acceptsCatchup(owner)) {
                                when (miniGuideAction(programme.startMs, programme.stopMs, System.currentTimeMillis(), channel.catchup)) {
                                    MiniGuideAction.LIVE -> onLive(channel)
                                    MiniGuideAction.ARCHIVE -> onArchive(channel, programme, owner)
                                    MiniGuideAction.NONE -> Unit
                                }
                            }
                        }
                    }
                    true
                }.focusable().trapAllFocusExit()) {
                Text(channel.name, style = MaterialTheme.typography.titleLarge, color = colours.onSurface)
                Text(stringResource(CoreR.string.guide_browse_time, formatDate(cursor), formatTime(cursor)), color = colours.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                when {
                    failed -> Text(stringResource(CoreR.string.mini_guide_failed), color = colours.onSurfaceVariant)
                    rows == null -> OwnTVSpinner(sizeDp = 28)
                    programme == null -> Text(stringResource(CoreR.string.content_epg_no_programme), color = colours.onSurfaceVariant)
                    else -> {
                        if (liveVm.usesSharedGuide(channel, programme)) Text(stringResource(CoreR.string.guide_shared_origin), color = colours.onSurfaceVariant)
                        Text(programme.title, style = MaterialTheme.typography.titleMedium, color = colours.onSurface)
                        Text(stringResource(CoreR.string.content_epg_time_range, formatTime(programme.displayStartMs), formatTime(programme.displayStopMs)), color = colours.onSurfaceVariant)
                        when {
                            programme.startMs > now -> Text(stringResource(CoreR.string.mini_guide_future), color = colours.onSurfaceVariant)
                            programme.stopMs <= now && !channel.catchup -> Text(stringResource(CoreR.string.mini_guide_no_archive), color = colours.onSurfaceVariant)
                            programme.stopMs <= now && (channel.catchupDays <= 0 || programme.startMs < now - channel.catchupDays * 86_400_000L) ->
                                Text(stringResource(CoreR.string.mini_guide_attempt), color = colours.onSurfaceVariant)
                        }

                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(CoreR.string.mini_guide_navigation), style = MaterialTheme.typography.bodySmall, color = colours.onSurfaceVariant)
            }
        }
    }
}

internal enum class MiniGuideAction { NONE, LIVE, ARCHIVE }
internal fun miniGuideAction(start: Long, stop: Long, now: Long, catchup: Boolean): MiniGuideAction = when {
    stop <= start || start > now -> MiniGuideAction.NONE
    now < stop -> MiniGuideAction.LIVE
    catchup -> MiniGuideAction.ARCHIVE
    else -> MiniGuideAction.NONE
}
