package tv.own.owntv.features.epg

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Precision
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.ui.components.longPressMenuGuard
import tv.own.owntv.ui.components.ChannelGenre
import tv.own.owntv.ui.components.ErrorState
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.components.ProviderChip
import tv.own.owntv.ui.components.ContentPanelFill
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.components.OwnTVSpinner
import tv.own.owntv.ui.components.SearchBar
import tv.own.owntv.ui.components.trapAllFocusExit
import tv.own.owntv.ui.components.trapVerticalFocusExit
import tv.own.owntv.ui.components.dialogPanel
import tv.own.owntv.ui.components.modalScrim
import tv.own.owntv.features.epg.GuideGridDefaults
import tv.own.owntv.features.epg.ProgrammeDetailDialog
import tv.own.owntv.features.epg.ProgrammeStripCanvas
import tv.own.owntv.ui.format.rememberBestDateFormatter
import tv.own.owntv.ui.format.rememberSystemTimeFormatter
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
private fun guideSortLabel(sort: SettingsRepository.GuideSort): String = stringResource(
    when (sort) {
        SettingsRepository.GuideSort.ALPHA -> R.string.content_epg_sort_alpha
        SettingsRepository.GuideSort.PROVIDER -> R.string.content_epg_sort_provider
        SettingsRepository.GuideSort.LIVE_TV -> R.string.content_epg_sort_live
        SettingsRepository.GuideSort.CATCHUP -> R.string.content_epg_sort_catchup
        SettingsRepository.GuideSort.FAVORITES -> R.string.content_epg_sort_favorites
    },
)

@Composable
private fun epgMessageText(message: EpgMessage): String = when (message) {
    EpgMessage.CreateProfile -> stringResource(R.string.content_epg_create_profile)
    EpgMessage.AddPlaylist -> stringResource(R.string.content_epg_add_playlist)
    is EpgMessage.NoChannelsForQuery -> stringResource(R.string.content_epg_no_channels_query, message.query)
    EpgMessage.MismatchedIds -> stringResource(R.string.content_epg_mismatched_ids)
}

@Composable
private fun epgStatsText(stats: EpgStats): String {
    val catchup = if (stats.catchupChannels > 0) {
        pluralStringResource(R.plurals.content_epg_catchup_count, stats.catchupChannels, stats.catchupChannels)
    } else {
        stringResource(R.string.content_epg_no_catchup_channels)
    }
    return if (stats.programmes > 0) {
        stringResource(
            R.string.content_epg_stats_loaded,
            pluralStringResource(R.plurals.content_epg_stats_channels, stats.guideChannels, stats.guideChannels),
            pluralStringResource(R.plurals.content_epg_stats_programmes, stats.programmes, stats.programmes),
            catchup,
        )
    } else if (stats.catchupChannels > 0) {
        pluralStringResource(
            R.plurals.content_epg_stats_catchup_available,
            stats.catchupChannels,
            stats.catchupChannels,
        )
    } else {
        stringResource(R.string.content_epg_stats_no_catchup)
    }
}

@Composable
private fun epgMatchSummaryText(summary: EpgMatchSummary): String = when (summary) {
    EpgMatchSummary.CatchupUnavailable -> stringResource(R.string.content_epg_catchup_unavailable)
    EpgMatchSummary.MatchedNoProgrammes -> stringResource(R.string.content_epg_matched_no_programmes)
    EpgMatchSummary.AddPlaylist -> stringResource(R.string.content_epg_add_playlist_first)
    EpgMatchSummary.NoData -> stringResource(R.string.content_epg_no_match_data)
    EpgMatchSummary.AllMatched -> stringResource(R.string.content_epg_all_matched)
    is EpgMatchSummary.NoMatch -> stringResource(R.string.content_epg_no_match, summary.channelName)
    is EpgMatchSummary.AutoMatched -> if (summary.review > 0) {
        stringResource(
            R.string.content_epg_auto_matched,
            pluralStringResource(R.plurals.content_epg_auto_matched_applied, summary.applied, summary.applied),
            pluralStringResource(R.plurals.content_epg_auto_matched_review, summary.review, summary.review),
        )
    } else {
        pluralStringResource(
            R.plurals.content_epg_auto_matched_no_review,
            summary.applied,
            summary.applied,
        )
    }
}

/**
 * The full EPG guide: a time × channel grid. Channel labels are pinned on the left; every channel row
 * and the time axis share one horizontal scroll state, so moving the D-pad across programmes scrolls
 * the whole guide in lock-step. Picking a programme opens its details.
 */
@Composable
fun EpgScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {},
    onAddEpg: () -> Unit = {},
    restoreFocus: Boolean = false,
    onRestored: () -> Unit = {},
    onContentScrolled: (Boolean) -> Unit = {},
    /** Required: every live tune in the app goes through the one shared path in LiveViewModel, so a
     *  channel gets the same Prefer HLS handling, ExoPlayer→mpv ladder, per-channel engine pin and
     *  external-player routing however the user reached it. */
    onPlayChannel: (channel: ChannelEntity, channels: List<ChannelEntity>) -> Unit,
    /** "Watch from start" on a catch-up programme. Required for the same reason, and additionally so the
     *  archive is tracked as catch-up playback (which decides what engine toggle the player HUD offers). */
    onPlayCatchup: (channel: ChannelEntity, programme: EpgProgrammeEntity, owner: tv.own.owntv.features.live.CatchupOwner) -> Unit,
    captureCatchupOwner: (ChannelEntity) -> tv.own.owntv.features.live.CatchupOwner,
    acceptsCatchupOwner: (tv.own.owntv.features.live.CatchupOwner) -> Boolean,
    onPlayCatchupExternal: (ChannelEntity, EpgProgrammeEntity, tv.own.owntv.features.live.CatchupOwner) -> Unit,
    catchupContextKey: Any? = null,
) {
    val compactLayout = tv.own.owntv.ui.components.rememberCompactLayout()
    val touch = !tv.own.owntv.ui.components.rememberRemoteTextInput()
    val vm: EpgViewModel = koinViewModel()
    val recordContext = LocalContext.current
    LaunchedEffect(vm) {
        vm.recordMessages.collect { message ->
            android.widget.Toast.makeText(recordContext, recordContext.getString(message), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val liveNow by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val query by vm.query.collectAsStateWithLifecycle()
    val matching by vm.matching.collectAsStateWithLifecycle()
    val review by vm.review.collectAsStateWithLifecycle()
    val matchSummary by vm.matchSummary.collectAsStateWithLifecycle()
    val sortGuide by vm.sortGuide.collectAsStateWithLifecycle()
    val categoryFilter by vm.categoryFilter.collectAsStateWithLifecycle()
    val guideCategories by vm.guideCategories.collectAsStateWithLifecycle()
    val providerNames by vm.providerNames.collectAsStateWithLifecycle()
    val guideWidthShares by vm.guideWidthShares.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteChannelIds.collectAsStateWithLifecycle()
    val catchupPlayer by vm.catchupPlayer.collectAsStateWithLifecycle()
    var showCategoryPicker by remember { mutableStateOf(false) }
    val colors = OwnTVTheme.colors
    val hScroll = rememberScrollState()
    val rowListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val chromeScrollThresholdPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val contentScrolled by remember(hScroll, rowListState, chromeScrollThresholdPx) {
        androidx.compose.runtime.derivedStateOf {
            hScroll.value > chromeScrollThresholdPx ||
                rowListState.firstVisibleItemIndex > 0 ||
                rowListState.firstVisibleItemScrollOffset > chromeScrollThresholdPx
        }
    }
    LaunchedEffect(contentScrolled) { onContentScrolled(contentScrolled) }
    val firstCell = remember { FocusRequester() }
    val tunedCell = remember { FocusRequester() }
    // The channel a dialog was opened from — focus returns to its row when the dialog closes.
    val restoreCell = remember { FocusRequester() }
    var restoreChannelId by remember { mutableStateOf<Long?>(null) }
    var detail by remember { mutableStateOf<Pair<ChannelEntity, EpgProgrammeEntity>?>(null) }
    var detailOwner by remember { mutableStateOf<Any?>(null) }
    var detailRequestOwner by remember { mutableStateOf<tv.own.owntv.features.live.CatchupOwner?>(null) }
    LaunchedEffect(catchupContextKey) { if (detailOwner != catchupContextKey) detail = null }
    var matchingChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var matchChooser by remember { mutableStateOf<ChannelEntity?>(null) }
    var inheritanceChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var offsetChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    // Two-stage timeline navigation (#4): Right from a channel focuses its whole programme row (ROW
    // stage); OK steps into per-programme browsing (CELL stage) where Left/Right move a cursor and
    // Up/Down jump to the adjacent channel at the same time. cursorTime is the highlighted time.
    val savedBrowse = remember(state.profileId) { vm.browseContext }
    var viewportReady by remember(state.profileId) { mutableStateOf(false) }
    var inCellMode by remember(state.profileId) { mutableStateOf(savedBrowse?.cellMode ?: false) }
    var cursorTime by remember(state.profileId) { mutableStateOf(savedBrowse?.cursorMs ?: 0L) }
    // The channel whose programme strip currently has focus — drives the non-modal bottom info strip.
    var focusedChannel by remember(state.profileId) { mutableStateOf<ChannelEntity?>(null) }
    // The pending focus target onEnter routes to: our own restore requests cross into this group
    // from outside, so onEnter must cooperate or it would hijack them to the first channel.
    var pendingEnter by remember { mutableStateOf<FocusRequester?>(null) }

    // No BackHandler here: the Guide is a top-level section, so Back is the shell's job (content →
    // sidebar → exit dialog). A screen-level handler would swallow Back forever and block app exit.
    LaunchedEffect(Unit) { vm.load() } // reload from DB each time the guide is opened
    // Phase 6 fix — don't auto-focus the first channel when the guide mounts. The guide nav item
    // keeps focus on click; RIGHT press from the sidebar enters the grid via focusProperties.onEnter
    // (which routes to firstCell/tunedCell). Auto-focusing here stole the sidebar's focus on section
    // switch, making the guide feel jumpy.
    // Back from a channel tuned in the guide: scroll to and refocus that channel's row. Must wait
    // for the reload (vm.load() runs on every mount) — while state.loading the grid isn't composed
    // at all (spinner branch), so a requestFocus would silently fail and burn the restore flag.
    LaunchedEffect(restoreFocus, state.loading, state.channels.size) {
        if (!restoreFocus || state.loading || state.channels.isEmpty()) return@LaunchedEffect
        val idx = (vm.lastTunedChannelId ?: savedBrowse?.channelId)?.let { id -> state.channels.indexOfFirst { it.id == id } } ?: -1
        val target = if (idx >= 0) tunedCell else firstCell
        if (idx >= 0) runCatching { rowListState.scrollToItem(idx) }
        pendingEnter = target
        kotlinx.coroutines.delay(80)
        repeat(3) {
            if (runCatching { target.requestFocus() }.getOrDefault(false)) {
                onRestored()
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(80)
        }
    }

    // The catch-up guide spans past→future; center "now" so current programme titles keep room on
    // both sides (past remains reachable with D-pad Left).
    val density = LocalDensity.current
    var guideContentWidthPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val guideChannelWidth = if (compactLayout) {
        if (guideContentWidthPx > 0) minOf(128.dp, with(density) { guideContentWidthPx.toDp() } * 0.34f) else 110.dp
    } else if (guideWidthShares != null && guideContentWidthPx > 0) {
        with(density) { guideContentWidthPx.toDp() } * (guideWidthShares!!.channels / 100f)
    } else {
        GuideGridDefaults.ChannelCol
    }
    val guideTimelineWidthPx = (
        guideContentWidthPx - with(density) { guideChannelWidth.roundToPx() }
    ).coerceAtLeast(0)
    fun nowAnchoredScrollPx(now: Long): Int {
        val nowPx = with(density) {
            ((((now - state.windowStart) / 60_000f) * GuideGridDefaults.PxPerMin.value).dp).roundToPx()
        }
        return (nowPx - guideTimelineWidthPx * 3 / 8).coerceAtLeast(0)
    }
    LaunchedEffect(state.profileId, state.windowStart, state.channels.isNotEmpty(), guideTimelineWidthPx) {
        if (state.channels.isEmpty()) return@LaunchedEffect
        viewportReady = false
        val minutesBack = ((state.now - state.windowStart) / 60_000L).toInt()
        if (minutesBack <= GuideGridDefaults.SlotMin) { viewportReady = true; return@LaunchedEffect }
        val previousLeft = vm.browseContext?.leftMs ?: savedBrowse?.leftMs
        val px = if (previousLeft != null) with(density) {
            (((previousLeft.coerceIn(state.windowStart, state.windowEnd) - state.windowStart) / 60_000f) * GuideGridDefaults.PxPerMin.value).dp.roundToPx()
        } else nowAnchoredScrollPx(state.now)
        // Wait until the time-axis row is laid out so maxValue is known — otherwise scrollTo runs before
        // layout and clamps to 0 (a no-op), leaving the strips at the past edge (no data yet) → blank guide
        // until a later real scroll. Bounded so we never hang if the row stays unscrollable.
        kotlinx.coroutines.withTimeoutOrNull(2000) {
            androidx.compose.runtime.snapshotFlow { hScroll.maxValue }.first { it > 0 }
        }
        runCatching { hScroll.scrollTo(px) }
        viewportReady = true
        if (!restoreFocus && savedBrowse != null) {
            rowListState.scrollToItem(savedBrowse.rowIndex.coerceIn(0, state.channels.lastIndex), savedBrowse.rowOffset)
        }
    }

    val scope = rememberCoroutineScope()
    // "Jump to Now" — scrolls the shared timeline back to the current time. Mirrors the on-mount auto-scroll
    // above so the user can return to "now" after browsing the catch-up archive. Shown only when "now" is
    // actually inside the loaded window (it isn't, for example, right after a fresh load with no lookback).
    val jumpToNow: () -> Unit = {
        scope.launch {
            val minutesBack = ((liveNow - state.windowStart) / 60_000L).toInt()
            if (minutesBack <= GuideGridDefaults.SlotMin) return@launch
            cursorTime = liveNow
            val px = nowAnchoredScrollPx(liveNow)
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                androidx.compose.runtime.snapshotFlow { hScroll.maxValue }.first { it > 0 }
            }
            runCatching { hScroll.scrollTo(px) }
        }
    }

    // Keep reads around the viewport in six-hour blocks, with neighbouring blocks as overscan.
    // A 31-day guide never materialises 31 days of programmes for each visible channel.
    val readBlock by remember(hScroll, state.windowStart, guideTimelineWidthPx, density) {
        androidx.compose.runtime.derivedStateOf {
            val pxPerMin = with(density) { GuideGridDefaults.PxPerMin.toPx() }
            val center = state.windowStart + ((hScroll.value + guideTimelineWidthPx / 2) / pxPerMin * 60_000).toLong()
            Math.floorDiv(center, 6 * 3_600_000L) * (6 * 3_600_000L)
        }
    }
    LaunchedEffect(readBlock, state.windowStart, state.windowEnd, viewportReady) {
        if (!viewportReady) return@LaunchedEffect
        val window = guideReadWindow(readBlock, state.windowStart, state.windowEnd)
        vm.setReadWindow(window.first, window.second)
    }
    LaunchedEffect(state.windowStart, state.windowEnd) {
        if (cursorTime > 0 && state.windowEnd > state.windowStart) {
            cursorTime = cursorTime.coerceIn(state.windowStart, state.windowEnd - 1)
        }
    }
    LaunchedEffect(state.windowStart, state.channels, viewportReady) {
        if (!viewportReady || state.channels.isEmpty()) return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow {
            val pxPerMin = with(density) { GuideGridDefaults.PxPerMin.toPx() }
            GuideBrowseContext(focusedChannel?.id ?: savedBrowse?.channelId, cursorTime,
                state.windowStart + (hScroll.value / pxPerMin * 60_000).toLong(), inCellMode,
                rowListState.firstVisibleItemIndex, rowListState.firstVisibleItemScrollOffset)
        }.collect { vm.rememberBrowse(it) }
    }
    val moveTime: (Int, Boolean) -> Unit = { amount, days ->
        val reference = if (cursorTime > 0) cursorTime else liveNow
        val target = shiftGuideTime(reference, amount, days, java.time.ZoneId.systemDefault())
        if (state.windowEnd > state.windowStart) {
            cursorTime = target.coerceIn(state.windowStart, state.windowEnd - 1)
            scope.launch { hScroll.scrollTo(nowAnchoredScrollPx(cursorTime)) }
        }
    }

    // In CELL mode, Back steps out to whole-row (ROW) selection instead of leaving the guide.
    BackHandler(enabled = inCellMode) { inCellMode = false }

    // Keep the highlighted (cursor) programme in view while browsing a row in CELL mode. Entering
    // CELL at "now" must not disturb the centered timeline; scroll only after navigation reaches an edge.
    LaunchedEffect(cursorTime, inCellMode, guideTimelineWidthPx) {
        if (!inCellMode || state.channels.isEmpty() || guideTimelineWidthPx <= 0) return@LaunchedEffect
        val cursorPx = with(density) {
            ((((cursorTime - state.windowStart) / 60_000f) * GuideGridDefaults.PxPerMin.value).dp).roundToPx()
        }
        val marginPx = with(density) {
            (GuideGridDefaults.SlotMin * GuideGridDefaults.PxPerMin.value).dp.roundToPx()
        }
        val viewportStart = hScroll.value
        val viewportEnd = viewportStart + guideTimelineWidthPx
        val target = when {
            cursorPx < viewportStart + marginPx -> cursorPx - marginPx
            cursorPx > viewportEnd - marginPx -> cursorPx - guideTimelineWidthPx + marginPx
            else -> null
        }
        target?.let { runCatching { hScroll.scrollTo(it.coerceIn(0, hScroll.maxValue)) } }
    }

    // Closing ANY of the guide's overlays (programme detail, the match chooser, the manual EPG picker)
    // would otherwise drop focus to the sidebar. Restore focus to the row the dialog was opened from.
    val anyDialogOpen = detail != null || matchChooser != null || matchingChannel != null || inheritanceChannel != null
    var hadDialog by remember { mutableStateOf(false) }
    LaunchedEffect(anyDialogOpen) {
        if (anyDialogOpen) {
            hadDialog = true
            return@LaunchedEffect
        }
        if (!hadDialog) return@LaunchedEffect
        hadDialog = false
        val idx = restoreChannelId?.let { id -> state.channels.indexOfFirst { it.id == id } } ?: -1
        val target = if (idx >= 0) restoreCell else firstCell
        if (idx >= 0) runCatching { rowListState.scrollToItem(idx) }
        pendingEnter = target
        kotlinx.coroutines.delay(80)
        if (!runCatching { target.requestFocus() }.getOrDefault(false)) runCatching { firstCell.requestFocus() }
        restoreChannelId = null // focus is set; release the row so playback-restore can reuse it
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .roundedPanel(fillColor = ContentPanelFill)
            // Entry from the sidebar lands on the first channel — unless a restore is pending
            // (back from playback / dialog close), which onEnter routes to instead of hijacking.
            // onEnter only fires for entries from OUTSIDE the group — search bar / refresh / back
            // are inside it, so moving up from the grid to them never re-triggers this.
            .focusProperties {
                onEnter = {
                    val target = pendingEnter ?: firstCell
                    pendingEnter = null
                    // tunedCell fallback: when the last-tuned channel IS row 0, firstCell isn't attached.
                    if (!runCatching { target.requestFocus() }.getOrDefault(false)) runCatching { tunedCell.requestFocus() }
                }
            }
            // Held Up/Down can outrun the guide's row composition and escape this pane
            // (landing on the top bar) — trap vertical exits; Left/Right/Back leave normally.
            .trapVerticalFocusExit()
            .focusGroup()
            .padding(horizontal = if (compactLayout) 12.dp else 32.dp, vertical = if (compactLayout) 12.dp else 24.dp),
    ) {
        // Header: back + title + date + refresh
        GuideHeaderRow(compactLayout) {
            val backDescription = stringResource(R.string.common_back)
            FocusableSurface(onClick = onBack, modifier = Modifier.size(if (touch) 48.dp else 44.dp).semantics { contentDescription = backDescription }, shape = RoundedCornerShape(14.dp), contentAlignment = Alignment.Center, surface = GlassSurface.CARDS) { _ ->
                OwnTVIcon(OwnTVIcon.BACK, tint = colors.onSurface, modifier = Modifier.size(20.dp))
            }
            Text(stringResource(R.string.content_epg_title), style = if (compactLayout) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge, color = colors.onSurface)
            val formatHeaderDate = rememberBestDateFormatter("EEEdMMM")
            if (state.now > 0) {
                // The day being browsed: "now" on open; follows the cursor when D-padding left into
                // the catch-up archive (windowStart would show the archive start — days in the past).
                val headerDate = if (cursorTime > 0) cursorTime else liveNow
                Text(
                    stringResource(R.string.guide_browse_time, formatHeaderDate(headerDate), rememberSystemTimeFormatter()(headerDate)),
                    style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
                )
            }
            // Jump the timeline back to the current time (useful after browsing the catch-up archive).
            if (liveNow in state.windowStart..state.windowEnd) {
                OwnTVButton(stringResource(R.string.content_epg_jump_now), onClick = jumpToNow, icon = OwnTVIcon.HISTORY, style = OwnTVButtonStyle.SECONDARY)
            }
            if (!compactLayout) Spacer(Modifier.weight(1f))
            // Guide sort: A–Z / Provider / Live TV (mirrors Live) / Catch-up (archive first; hidden when none).
            val sortLabel = when {
                sortGuide == SettingsRepository.GuideSort.CATCHUP && state.catchupCount == 0 -> guideSortLabel(SettingsRepository.GuideSort.LIVE_TV)
                sortGuide == SettingsRepository.GuideSort.FAVORITES && state.favoriteCount == 0 -> guideSortLabel(SettingsRepository.GuideSort.LIVE_TV)
                else -> guideSortLabel(sortGuide)
            }
            // Category filter (#8): narrow the guide to one group instead of all channels at once.
            if (guideCategories.isNotEmpty()) {
                val catLabel = categoryFilter?.let { key -> guideCategories.firstOrNull { it.key == key }?.name } ?: stringResource(R.string.content_epg_all)
                OwnTVButton(stringResource(R.string.content_epg_category_button, catLabel), onClick = { showCategoryPicker = true }, icon = OwnTVIcon.MENU, style = OwnTVButtonStyle.SECONDARY)
                if (!compactLayout) Spacer(Modifier.width(12.dp))
            }
            OwnTVButton(stringResource(R.string.content_epg_sort_button, sortLabel), onClick = vm::cycleGuideSort, icon = OwnTVIcon.SORT, style = OwnTVButtonStyle.SECONDARY)
            if (!compactLayout) Spacer(Modifier.width(12.dp))
            // Smart-match: auto-link channels whose tvg-id doesn't match the EPG feed, by name (#13).
            if (matching) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OwnTVSpinner(sizeDp = 20)
                    Text(stringResource(R.string.content_epg_matching), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                }
            } else {
                OwnTVButton(stringResource(R.string.content_epg_match_button), onClick = vm::autoMatchEpg, icon = OwnTVIcon.EPG, style = OwnTVButtonStyle.SECONDARY)
            }
        }
        if (state.channels.isNotEmpty()) {
            GuideHeaderRow(compactLayout, 8.dp) {
                OwnTVButton(stringResource(R.string.guide_previous_day), onClick = { moveTime(-1, true) }, style = OwnTVButtonStyle.SECONDARY)
                OwnTVButton(stringResource(R.string.guide_previous_hour), onClick = { moveTime(-1, false) }, style = OwnTVButtonStyle.SECONDARY)
                OwnTVButton(stringResource(R.string.guide_next_hour), onClick = { moveTime(1, false) }, style = OwnTVButtonStyle.SECONDARY)
                OwnTVButton(stringResource(R.string.guide_next_day), onClick = { moveTime(1, true) }, style = OwnTVButtonStyle.SECONDARY)
            }
        }
        state.stats?.let { stats ->
            Spacer(Modifier.height(4.dp))
            Text(epgStatsText(stats), style = MaterialTheme.typography.labelLarge, color = colors.primary)
        }
        // Outcome of the last auto-match run (auto-applied count / how many need review). Dismissible.
        matchSummary?.let { summary ->
            if (review.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                FocusableSurface(onClick = vm::clearReview, shape = RoundedCornerShape(10.dp), unfocusedContainerColor = colors.surfaceContainerHigh, contentAlignment = Alignment.CenterStart, surface = GlassSurface.CARDS) { _ ->
                    Text(epgMatchSummaryText(summary), style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SearchBar(
            query = query,
            onQueryChange = vm::setQuery,
            placeholder = stringResource(R.string.content_epg_search_hint),
            modifier = Modifier.fillMaxWidth().onSizeChanged { guideContentWidthPx = it.width },
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> CenterBox { OwnTVSpinner(sizeDp = 56) }
            // No EPG feed added yet → guide it can't fill. Point the user to EPG Sources.
            !state.hasEpgSources && state.channels.isEmpty() -> CenterBox {
                Text(stringResource(R.string.content_epg_empty), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.content_epg_add_description),
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                OwnTVButton(stringResource(R.string.content_epg_add), onClick = onAddEpg, icon = OwnTVIcon.ADD)
            }
            state.channels.isEmpty() -> CenterBox {
                Text(state.message?.let { epgMessageText(it) } ?: stringResource(R.string.content_epg_no_guide), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
            else -> {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    GuideTimeAxis(state.windowStart, state.windowEnd, liveNow, guideChannelWidth, guideTimelineWidthPx, hScroll)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f), state = rowListState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(
                            state.channels,
                            key = { _, ch -> ch.id },
                            contentType = { _, _ -> 0 },
                        ) { index, channel ->
                            GuideChannelRow(
                                vm = vm,
                                channel = channel,
                                windowStart = state.windowStart,
                                windowEnd = state.windowEnd,
                            now = liveNow,
                                hScroll = hScroll,
                                labelFocus = when {
                                    channel.id == restoreChannelId -> restoreCell
                                    channel.id == (vm.lastTunedChannelId ?: savedBrowse?.channelId) -> tunedCell
                                    index == 0 -> firstCell
                                    else -> null
                                },
                                onTune = { vm.noteChannelTuned(channel); onPlayChannel(channel, state.channels) },
                                onOpen = { restoreChannelId = channel.id; detailOwner = catchupContextKey; detailRequestOwner = captureCatchupOwner(channel); detail = channel to it },
                                onMatchEpg = { restoreChannelId = channel.id; matchChooser = channel },
                                inCellMode = inCellMode,
                                cursorTime = cursorTime,
                            onEnterCell = {
                                    if (cursorTime !in state.windowStart until state.windowEnd) cursorTime = liveNow
                                    inCellMode = true
                                },
                                onExitToChannels = { inCellMode = false },
                                onMoveCursor = { cursorTime = it },
                                onStripFocused = { focusedChannel = channel },
                                categoryColor = guideCategories.firstOrNull { it.categoryId == channel.categoryId }?.name?.let { ChannelGenre.dotFor(it) },
                                providerName = providerNames[channel.sourceId],
                                channelWidth = guideChannelWidth,
                            )
                        }
                    }
                }
                // Non-modal bottom strip — previews the cursor programme while browsing in CELL mode.
                GuideInfoStrip(
                    focusedChannel = focusedChannel,
                    cursorTime = cursorTime,
                    inCellMode = inCellMode,
                    vm = vm,
                now = liveNow,
                )
            }
        }
    }

    detail?.let { (channel, p) ->
        val requestOwner = detailRequestOwner
        val replayRevision by sessionReplayEvidence.revision.collectAsStateWithLifecycle()
        val replayEvidence = remember(replayRevision, requestOwner, channel.id, p.startMs, p.stopMs) {
            requestOwner?.let { owner ->
                sessionReplayEvidence.get(ReplayEvidenceStore.Key(owner.profileId, channel.sourceId, channel.id, p.startMs, p.stopMs))
            }
        }
        // Recompute as the recordings change, so pressing Record swaps the button to Cancel without
        // closing the dialog.
        val recordingRows by vm.recordingRows.collectAsStateWithLifecycle()
        val existingRecording = remember(recordingRows, channel.id, p.startMs) {
            vm.recordingFor(channel, p)
        }
        // The clash is a database read, so it happens once per opened programme rather than per frame.
        val clash by produceState<String?>(null, channel.id, p.startMs, recordingRows) {
            value = vm.clashFor(channel, p)
        }
        // Re-read as the recordings change, so adding or removing the rule flips the button without
        // closing the dialog.
        val seriesRule by produceState<tv.own.owntv.core.database.entity.RecordingRuleEntity?>(
            null, channel.id, p.title, recordingRows,
        ) {
            value = vm.seriesRuleFor(channel, p)
        }
        ProgrammeDetailDialog(
            channelName = channel.name,
            programme = p,
            replayEvidence = replayEvidence,
            // A provider-fetched row is in no table, so its synopsis is the one it already carries;
            // only a stored row needs fetching by id.
            loadDescription = { id -> p.description ?: vm.programmeDescription(id) },
            canCatchup = vm.canAttemptCatchup(channel, p, liveNow),
            replayUnconfirmed = vm.canAttemptCatchup(channel, p, liveNow) && !vm.canCatchup(channel, p, liveNow),
            canRecord = vm.canRecord(channel, p, liveNow),
            recording = existingRecording,
            clashWith = clash,
            onRecord = { vm.record(channel, p) },
            onStopRecording = { existingRecording?.let { vm.stopRecording(it) } },
            onCancelRecording = { existingRecording?.let { vm.cancelRecording(it) } },
            seriesRuleActive = seriesRule != null,
            onRecordSeries = { vm.recordSeries(channel, p) },
            onStopSeries = { seriesRule?.let { vm.stopSeries(it) } },
            isFavorite = channel.id in favoriteIds,
            onToggleFavorite = { vm.toggleFavoriteChannel(channel) },
            onWatch = { detail = null; if (requestOwner?.let(acceptsCatchupOwner) == true) { vm.noteChannelTuned(channel); onPlayChannel(channel, state.channels) } },
            onPlayCatchup = {
                detail = null
                requestOwner?.takeIf(acceptsCatchupOwner)?.let { owner ->
                    vm.noteChannelTuned(channel)
                    onPlayCatchup(channel, p, owner)
                }
            },
            // External play needs no shell involvement: nothing is mounted in-app, so it goes straight
            // through the Guide's own VM (which owns the archive-URL builder) in both hosting modes.
            onPlayCatchupExternal = { detail = null; requestOwner?.takeIf(acceptsCatchupOwner)?.let { vm.noteChannelTuned(channel); onPlayCatchupExternal(channel, p, it) } },
            catchupPlayer = catchupPlayer,
            onDismiss = { detail = null },
        )
    }

    // Long-press a channel → channel options: favourite toggle + EPG match (auto or manual pick).
    matchChooser?.let { channel ->
        EpgMatchChooserDialog(
            channelName = channel.name,
            isFavorite = channel.id in favoriteIds,
            onToggleFavorite = { vm.toggleFavoriteChannel(channel) },
            onAuto = { vm.autoMatchOne(channel); matchChooser = null },
            onManual = { matchChooser = null; matchingChannel = channel },
            onOffset = { matchChooser = null; offsetChannel = channel },
            onInheritance = { matchChooser = null; inheritanceChannel = channel },
            onDismiss = { matchChooser = null },
        )
    }

    inheritanceChannel?.let { channel ->
        val inheritedProfile = remember(channel) { state.profileId }
        LaunchedEffect(state.profileId) { if (state.profileId != inheritedProfile) inheritanceChannel = null }
        var retry by remember(channel) { androidx.compose.runtime.mutableIntStateOf(0) }
        var failed by remember(channel, retry) { mutableStateOf(false) }
        val candidates by produceState<List<Pair<ChannelEntity, String>>?>(null, channel, retry) {
            value = null
            try { value = vm.epgFallbackCandidates(channel) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true; value = emptyList() }
        }
        if (failed) {
            tv.own.owntv.ui.components.OwnTVPopup(onDismissRequest = { inheritanceChannel = null }) {
                Box(Modifier.fillMaxSize().modalScrim(), contentAlignment = Alignment.Center) {
                    Column(Modifier.dialogPanel()) {
                        Text(stringResource(R.string.mini_guide_failed), color = colors.onSurface)
                        OwnTVButton(stringResource(R.string.update_try_again), onClick = { retry++ })
                        OwnTVButton(stringResource(R.string.common_cancel), onClick = { inheritanceChannel = null })
                    }
                }
            }
        } else if (candidates == null) {
            tv.own.owntv.ui.components.OwnTVPopup(onDismissRequest = { inheritanceChannel = null }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { OwnTVSpinner(sizeDp = 40) }
            }
        } else {
            tv.own.owntv.features.settings.PickerDialog(
                title = stringResource(R.string.guide_share_title, channel.name),
                options = listOf("" to stringResource(R.string.guide_share_none)) + candidates.orEmpty().map { it.second to it.first.name },
                selected = vm.currentEpgFallback(channel) ?: "",
                onSelect = { vm.setEpgFallback(channel, it.takeIf { key -> key.isNotBlank() }, inheritedProfile); inheritanceChannel = null },
                onDismiss = { inheritanceChannel = null },
                searchable = false,
            )
        }
    }

    offsetChannel?.let { channel ->
        tv.own.owntv.features.live.EpgOffsetDialog(
            channelName = channel.name,
            currentMinutes = vm.currentEpgShift(channel),
            globalMinutes = vm.globalEpgShift(),
            onSet = { vm.setEpgShift(channel, it) },
            onDismiss = { offsetChannel = null },
        )
    }

    matchingChannel?.let { channel ->
        tv.own.owntv.features.live.EpgMatchDialog(
            channelName = channel.name,
            currentMatch = vm.currentEpgMatch(channel),
            loadChannels = { vm.availableEpgChannels(channel.name, it) },
            onPick = { vm.setEpgMatch(channel, it); matchingChannel = null },
            onClear = { vm.setEpgMatch(channel, null); matchingChannel = null },
            onDismiss = { matchingChannel = null },
        )
    }

    if (review.isNotEmpty()) {
        EpgMatchReviewDialog(
            suggestions = review,
            onAccept = vm::acceptSuggestion,
            onSkip = vm::dismissSuggestion,
            onAcceptAll = vm::acceptAllSuggestions,
            onSkipAll = vm::clearReview,
            onDone = vm::clearReview,
        )
    }

    if (showCategoryPicker) {
        tv.own.owntv.features.settings.PickerDialog(
            title = stringResource(R.string.content_epg_guide_category),
            options = listOf("ALL" to stringResource(R.string.content_epg_all_categories)) + guideCategories.map { it.key to it.name },
            selected = categoryFilter ?: "ALL",
            trailingLabels = guideCategories.mapNotNull { category ->
                category.providerName?.let { category.key to it }
            }.toMap(),
            onSelect = { vm.setCategoryFilter(it.takeUnless { value -> value == "ALL" }); showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
            searchable = true,
        )
    }
}

/**
 * Review screen for the smart EPG matcher (#13): lists the lower-confidence suggestions the auto-match
 * couldn't apply on its own, so the user can accept the good ones and skip the rest. High-confidence
 * matches are applied automatically and never reach here.
 */
@Composable
private fun EpgMatchReviewDialog(
    suggestions: List<EpgViewModel.EpgMatchSuggestion>,
    onAccept: (EpgViewModel.EpgMatchSuggestion) -> Unit,
    onSkip: (EpgViewModel.EpgMatchSuggestion) -> Unit,
    onAcceptAll: () -> Unit,
    onSkipAll: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    BackHandler { onDone() }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { firstFocus.requestFocus() } }

    // Popup(focusable=true) creates a hard focus boundary — clicking Accept/Skip removes an item
    // from the LazyColumn, but focus stays inside instead of escaping to the main nav bar.
    tv.own.owntv.ui.components.OwnTVPopup(onDismissRequest = onDone) {
    tv.own.owntv.ui.theme.PopupFontTheme(fontScale = 0.75f) {
    Box(
        Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup(),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.dialogPanel(width = 576.dp, corner = 18.dp, padding = 18.dp)) {
            Text(stringResource(R.string.content_epg_review_matches), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.content_epg_review_description),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            // Actions live in a right-hand column so a D-pad right from ANY suggestion row reaches
            // Accept all/Skip all/Done directly — no scrolling to the bottom of a long list.
            val listHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - 200.dp).coerceIn(160.dp, 300.dp)
            Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxWidth().height(listHeight), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(suggestions, key = { _, s -> s.channel.id }) { index, s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.channel.name, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                stringResource(R.string.content_epg_channel_score, s.epgName ?: s.epgChannelId, (s.score * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FocusableSurface(
                            onClick = { onAccept(s) },
                            modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                            shape = RoundedCornerShape(10.dp),
                            unfocusedContainerColor = colors.primaryContainer,
                            contentAlignment = Alignment.Center,
                            surface = GlassSurface.DIALOGS,
                        ) { _ -> Text(stringResource(R.string.content_epg_accept), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                        FocusableSurface(
                            onClick = { onSkip(s) },
                            shape = RoundedCornerShape(10.dp),
                            unfocusedContainerColor = colors.surfaceContainerHigh,
                            contentAlignment = Alignment.Center,
                            surface = GlassSurface.DIALOGS,
                        ) { _ -> Text(stringResource(R.string.content_epg_skip), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) }
                    }
                }
            }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.width(140.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bulk actions only make sense for a multi-channel run; a single auto-match shows just accept/skip.
                if (suggestions.size > 1) {
                    OwnTVButton(stringResource(R.string.content_epg_accept_all), onClick = onAcceptAll, icon = OwnTVIcon.PLAY, modifier = Modifier.fillMaxWidth())
                    OwnTVButton(stringResource(R.string.content_epg_skip_all), onClick = onSkipAll, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
                }
                OwnTVButton(stringResource(R.string.common_done), onClick = onDone, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
            }
        }
    }
    } // PopupFontTheme
    } // Popup
}

/**
 * Long-press chooser for a Guide channel's EPG: either auto-match just this channel by name, or open
 * the manual picker to choose its guide channel from the full list (#10/#13).
 */
@Composable
private fun EpgMatchChooserDialog(
    channelName: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAuto: () -> Unit,
    onManual: () -> Unit,
    onOffset: () -> Unit,
    onInheritance: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = OwnTVTheme.colors
    BackHandler { onDismiss() }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { firstFocus.requestFocus() } }

    tv.own.owntv.ui.components.OwnTVPopup(onDismissRequest = onDismiss) {
    Box(
        Modifier.fillMaxSize().modalScrim().trapAllFocusExit().focusGroup()
            .longPressMenuGuard(), // long-press OK is still held — don't auto-click the first option
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.dialogPanel()) {
            Text(channelName, style = MaterialTheme.typography.titleLarge, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.content_epg_favourite_description),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OwnTVButton(
                stringResource(if (isFavorite) R.string.content_epg_remove_favourite else R.string.content_epg_add_favourite),
                onClick = { onToggleFavorite(); onDismiss() },
                icon = OwnTVIcon.FAVORITE,
                modifier = Modifier.fillMaxWidth().focusRequester(firstFocus),
            )
            Spacer(Modifier.height(10.dp))
            OwnTVButton(stringResource(R.string.content_epg_match_button), onClick = onAuto, style = OwnTVButtonStyle.SECONDARY, icon = OwnTVIcon.EPG, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OwnTVButton(stringResource(R.string.content_epg_pick_manually), onClick = onManual, style = OwnTVButtonStyle.SECONDARY, icon = OwnTVIcon.SEARCH, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OwnTVButton(stringResource(R.string.guide_share_choose), onClick = onInheritance, style = OwnTVButtonStyle.SECONDARY, icon = OwnTVIcon.EPG, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OwnTVButton(stringResource(R.string.content_epg_time_offset), onClick = onOffset, style = OwnTVButtonStyle.SECONDARY, icon = OwnTVIcon.EPG, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OwnTVButton(stringResource(R.string.common_cancel), onClick = onDismiss, style = OwnTVButtonStyle.SECONDARY)
        }
    }
    }
}

/**
 * One guide row: pinned tunable channel label + lazily loaded programme strip. Programmes are fetched
 * from the DB only when the row scrolls into view (indexed query + VM cache), so the guide can list
 * every channel without holding the whole day's data in memory.
 */
@Composable
private fun GuideChannelRow(
    vm: EpgViewModel,
    channel: ChannelEntity,
    windowStart: Long,
    windowEnd: Long,
    now: Long,
    hScroll: androidx.compose.foundation.ScrollState,
    labelFocus: FocusRequester?,
    onTune: () -> Unit,
    onOpen: (EpgProgrammeEntity) -> Unit,
    onMatchEpg: () -> Unit,
    inCellMode: Boolean,
    cursorTime: Long,
    onEnterCell: () -> Unit,
    onExitToChannels: () -> Unit,
    onMoveCursor: (Long) -> Unit,
    onStripFocused: (ChannelEntity) -> Unit,
    categoryColor: Color?,
    providerName: String?,
    channelWidth: androidx.compose.ui.unit.Dp,
) {
    val colors = OwnTVTheme.colors
    val compactLayout = tv.own.owntv.ui.components.rememberCompactLayout()
    val touch = !tv.own.owntv.ui.components.rememberRemoteTextInput()
    val pixelsPerMinute = with(LocalDensity.current) { GuideGridDefaults.PxPerMin.toPx() }
    // Cache peek as the initial value → a row scrolled back into view renders instantly, with no
    // flash and no second query. A miss reads that one channel through the indexed per-channel query
    // and warms the rows below it. Re-key on cacheRevision so a row re-reads after the cache is
    // dropped (window moved, sync settled, shift changed).
    val cacheRevision by vm.cacheRevision.collectAsStateWithLifecycle()
    val readWindow by vm.readWindow.collectAsStateWithLifecycle()
    val programmes = key(channel.id, windowStart, windowEnd, readWindow, cacheRevision) {
        val rows by produceState(initialValue = vm.cachedProgrammes(channel)) {
            value = vm.cachedProgrammes(channel) ?: vm.programmesFor(channel)
        }
        rows
    }
    val labelFR = remember { FocusRequester() }
    val stripFR = remember { FocusRequester() }
    var stripFocused by remember { mutableStateOf(false) }

    Row {
        // Pinned channel label — OK tunes the channel; long-press opens the EPG-match chooser; Right
        // steps into the timeline (the strip becomes the focus target).
        FocusableSurface(
            onClick = onTune,
            onLongClick = onMatchEpg,
            modifier = Modifier.width(channelWidth).height(GuideGridDefaults.RowHeight).padding(end = 6.dp)
                .focusRequester(labelFR)
                .then(if (labelFocus != null) Modifier.focusRequester(labelFocus) else Modifier)
                // Physical by design: the guide is an LTR timeline, so the strip is always right.
                .focusProperties { right = stripFR }
                .onFocusChanged { if (it.isFocused) onExitToChannels() }, // back on a label ⇒ leave CELL stage
            shape = RoundedCornerShape(10.dp),
            unfocusedContainerColor = colors.surfaceContainerHigh,
            contentAlignment = Alignment.CenterStart,
            surface = GlassSurface.CARDS,
        ) { focused ->
            Row(
                modifier = Modifier.padding(horizontal = if (compactLayout) 6.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Genre colour dot — best-effort keyword match on the channel's category name; no dot when
                // the category is unknown (no wrong colour rather than a misleading one).
                if (categoryColor != null) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(categoryColor))
                }
                if (!compactLayout) GuideChannelLogo(channel)
                Text(
                    channel.number?.let { stringResource(R.string.content_epg_channel_number, it, channel.name) } ?: channel.name,
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Content),
                    color = if (focused) colors.primary else colors.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                providerName?.takeUnless { compactLayout }?.let {
                    ProviderChip(name = it, maxWidth = 66.dp, compact = true)
                }
            }
        }
        // Programme strip = ONE focus target (cells aren't individually focusable). ROW stage outlines
        // the whole strip; OK enters CELL stage where Left/Right move the cursor and Up/Down jump rows.
        val rowSelected = stripFocused && !inCellMode
        Box(
            modifier = Modifier.weight(1f).height(GuideGridDefaults.RowHeight)
                .then(if (touch) Modifier.pointerInput(programmes, windowStart, windowEnd, pixelsPerMinute) {
                    detectTapGestures { point ->
                        val instant = windowStart + ((point.x + hScroll.value) / pixelsPerMinute * 60_000).toLong()
                        programmes?.let { openAtCursor(it, instant, windowStart, windowEnd, onOpen) }
                    }
                } else Modifier)
                .focusRequester(stripFR)
                .onFocusChanged { stripFocused = it.isFocused; if (it.isFocused) onStripFocused(channel) }
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val progs = programmes
                    if (inCellMode) when (e.key) {
                        // Physical by design: left is earlier and right is later on the LTR timeline.
                        Key.DirectionLeft -> { moveGuideCursor(progs, cursorTime, -1, windowStart, windowEnd, onMoveCursor); true }
                        Key.DirectionRight -> { moveGuideCursor(progs, cursorTime, +1, windowStart, windowEnd, onMoveCursor); true }
                        Key.DirectionCenter, Key.Enter -> { progs?.let { openAtCursor(it, cursorTime, windowStart, windowEnd, onOpen) }; true }
                        else -> false // Up/Down fall through to spatial nav (jump to the next channel's row)
                    } else when (e.key) {
                        Key.DirectionCenter, Key.Enter -> { if (!progs.isNullOrEmpty()) onEnterCell(); true }
                        // Physical by design: channel labels stay left of the timeline strip.
                        Key.DirectionLeft -> { runCatching { labelFR.requestFocus() }; true } // back to the channel
                        // Let spatial focus navigation continue: a docked mini-player may be to the
                        // right of the guide and must remain reachable from this whole-row stage.
                        Key.DirectionRight -> false
                        else -> false
                    }
                }
                .focusable()
                .clip(RoundedCornerShape(10.dp))
                .then(if (rowSelected) Modifier.border(tv.own.owntv.ui.theme.LocalFocusBorderWidth.current, colors.focusBorder, RoundedCornerShape(10.dp)) else Modifier),
        ) {
            programmes?.let { progs ->
                // Catch-up-eligible programmes for this row — precomputed once per render (not per frame).
                val catchupIds = remember(progs, channel, now) {
                    progs.filter { vm.canCatchup(channel, it, now) }.map { it.id }.toSet()
                }
                ProgrammeStripCanvas(
                    programmes = progs,
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    now = now,
                    highlightTime = if (stripFocused && inCellMode) cursorTime else null,
                    catchupIds = catchupIds,
                    hScroll = hScroll,
                )
            }
        }
    }
}

/**
 * The channel's logo in the guide's label column.
 *
 * Deliberately collapses to nothing — rather than to a placeholder icon — when there is no logo or the
 * logo fails to load. The label column is narrow and user-resizable, so reserving 34dp on every row of
 * a playlist that carries no logos would cost the channel name two characters for no gain.
 */
@Composable
private fun GuideChannelLogo(channel: ChannelEntity) {
    val url = channel.displayLogoUrl
    if (url.isNullOrBlank()) return
    var failed by remember(url) { mutableStateOf(false) }
    if (failed) return
    val context = LocalContext.current
    val imageRequest = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .size(128, 128)
            .precision(Precision.INEXACT)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(34.dp),
        onState = { if (it is AsyncImagePainter.State.Error) failed = true },
    )
}

/** Move the CELL-stage cursor [delta] programmes within [progs], reporting the new highlighted time. */
private fun moveGuideCursor(
    progs: List<EpgProgrammeEntity>?,
    cursorTime: Long,
    delta: Int,
    windowStart: Long,
    windowEnd: Long,
    onMove: (Long) -> Unit,
) {
    if (windowEnd <= windowStart) return
    val next = progs?.let { GuideProgrammeCells.adjacentTime(it, cursorTime, delta, windowStart, windowEnd) }
    val edge = progs?.let { if (delta < 0) it.minOfOrNull { p -> p.startMs } else it.maxOfOrNull { p -> p.stopMs } }
    val target = when {
        next == null -> cursorTime + delta * 3_600_000L
        delta < 0 && edge != null && next >= cursorTime -> edge - 3_600_000L
        delta > 0 && edge != null && next <= cursorTime -> edge
        else -> next
    }
    onMove(target.coerceIn(windowStart, windowEnd - 1))
}

/** A gap never opens a neighbouring programme that is not drawn under the cursor. */
private fun openAtCursor(progs: List<EpgProgrammeEntity>, cursorTime: Long, windowStart: Long, windowEnd: Long, onOpen: (EpgProgrammeEntity) -> Unit) {
    GuideProgrammeCells.at(progs, cursorTime, windowStart, windowEnd)?.let(onOpen)
}


@Composable
private fun CenterBox(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/** Non-modal bottom strip that previews the cursor programme while browsing a row in CELL mode. No
 *  focusable controls — it never steals D-pad from the grid; OK still opens the full detail dialog. */
@Composable
private fun GuideInfoStrip(
    focusedChannel: ChannelEntity?,
    cursorTime: Long,
    inCellMode: Boolean,
    vm: EpgViewModel,
    now: Long,
) {
    val colors = OwnTVTheme.colors
    val formatTime = rememberSystemTimeFormatter()
    val cacheRevision by vm.cacheRevision.collectAsStateWithLifecycle()
    val guideState by vm.state.collectAsStateWithLifecycle()
    val readWindow by vm.readWindow.collectAsStateWithLifecycle()
    val programmes = key(focusedChannel, inCellMode, cacheRevision, readWindow, guideState.windowStart, guideState.windowEnd) {
        val rows by produceState<List<EpgProgrammeEntity>?>(null) {
            value = if (!inCellMode || focusedChannel == null) null
            else vm.cachedProgrammes(focusedChannel) ?: vm.programmesFor(focusedChannel)
        }
        rows
    }
    val programme = remember(focusedChannel, programmes, cursorTime, inCellMode, guideState.windowStart, guideState.windowEnd) {
        if (!inCellMode || focusedChannel == null || cursorTime <= 0L) null
        else programmes?.let { progs ->
            GuideProgrammeCells.at(progs, cursorTime, guideState.windowStart, guideState.windowEnd)
        }
    }
    val synopsis = key(focusedChannel, programme, cacheRevision) {
        val description by produceState<String?>(programme?.description) {
            // A provider-fetched row carries its own synopsis and has no id to look up.
            value = programme?.description ?: programme?.id?.takeIf { it > 0 }?.let { vm.programmeDescription(it) }
        }
        description
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val p = programme
        if (p != null && focusedChannel != null) {
            val catchup = vm.canCatchup(focusedChannel, p, now)
            Column(Modifier.weight(1f)) {
                Text(p.title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val runtimeMin = ((p.stopMs - p.startMs) / 60_000L).coerceAtLeast(0L).toInt()
                val bits = listOfNotNull(
                    stringResource(R.string.guide_shared_origin).takeIf { vm.usesSharedGuide(focusedChannel, p) },
                    focusedChannel.name,
                    stringResource(R.string.content_epg_time_range, formatTime(p.startMs), formatTime(p.stopMs)),
                    runtimeMin.takeIf { it > 0 }?.let { stringResource(R.string.content_epg_runtime, it) },
                    catchup.takeIf { it }?.let { stringResource(R.string.content_epg_catchup) },
                ).joinToString(stringResource(R.string.content_epg_bits_separator))
                Text(bits, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                synopsis?.takeIf { it.isNotBlank() }?.let { s ->
                    Text(s, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Text(
                if (inCellMode) stringResource(R.string.content_epg_no_programme) else stringResource(R.string.content_epg_move_hint),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Chunk empty space so a long history does not create a single oversized child constraint. */
@Composable
private fun GuideAxisSpace(slots: Int, slotWidth: Dp) {
    var remaining = slots
    while (remaining > 0) {
        val chunk = minOf(remaining, 64)
        Spacer(Modifier.width(slotWidth * chunk))
        remaining -= chunk
    }
}

/** Scroll observations stay here; moving the axis does not recompose the entire guide pane. */
@Composable
private fun GuideTimeAxis(
    windowStart: Long, windowEnd: Long, liveNow: Long,
    channelWidth: Dp, timelineWidthPx: Int, hScroll: androidx.compose.foundation.ScrollState,
) {
    val colors = OwnTVTheme.colors
    val density = LocalDensity.current
    val formatTime = rememberSystemTimeFormatter()
    val slots = ((windowEnd - windowStart) / (GuideGridDefaults.SlotMin * 60_000L)).toInt().coerceAtLeast(0)
    val slotWidth = (GuideGridDefaults.SlotMin * GuideGridDefaults.PxPerMin.value).dp
    val slotPx = with(density) { slotWidth.toPx() }
    val visibleSlots by remember(slots, hScroll, timelineWidthPx, slotPx) {
        androidx.compose.runtime.derivedStateOf { visibleGuideSlots(slots, hScroll.value, timelineWidthPx, slotPx) }
    }
    Row {
        Spacer(Modifier.width(channelWidth))
        Box(Modifier.horizontalScroll(hScroll)) {
            Row {
                GuideAxisSpace(if (visibleSlots.isEmpty()) 0 else visibleSlots.first, slotWidth)
                for (i in visibleSlots) {
                    val slotMs = windowStart + i * GuideGridDefaults.SlotMin * 60_000L
                    Text(
                        formatTime(slotMs),
                        style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Content),
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(slotWidth).padding(start = 6.dp),
                    )
                }
                val endSlot = if (visibleSlots.isEmpty()) 0 else visibleSlots.last + 1
                GuideAxisSpace((slots - endSlot).coerceAtLeast(0), slotWidth)
            }
            if (liveNow in windowStart..windowEnd) {
                val nowOffset = (((liveNow - windowStart) / 60_000f) * GuideGridDefaults.PxPerMin.value).dp
                Text(
                    stringResource(R.string.content_epg_now, formatTime(liveNow)),
                    style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Content),
                    color = Color(0xFF18211E),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = nowOffset - 34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFC857))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** Header actions wrap in narrow windows; TV retains its original single-row arrangement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GuideHeaderRow(compact: Boolean, spacing: androidx.compose.ui.unit.Dp = 12.dp, content: @Composable RowScope.() -> Unit) {
    if (compact) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
