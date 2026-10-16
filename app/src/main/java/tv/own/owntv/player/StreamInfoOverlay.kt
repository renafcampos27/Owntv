package tv.own.owntv.player

import tv.own.owntv.core.R as CoreR

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tv.own.owntv.ui.components.OwnTVButton
import kotlinx.coroutines.launch

/**
 * Scrollable technical readout for the current stream (codec, resolution, HDR, bitrate, decoder, audio,
 * buffer, source). Reads [PlaybackEngine.streamInfo] live — re-polled once a second so bitrate/buffer update
 * — and works on whichever engine is playing (mpv or ExoPlayer). Toggled from the player's info button.
 */
@Composable
fun StreamInfoOverlay(player: PlaybackEngine, modifier: Modifier = Modifier) {
    // Starts empty and is filled by the effect below: the mpv read now happens on the player's own
    // executor, so there is nothing to read synchronously during composition (A-F2).
    var rows by remember(player) { mutableStateOf(emptyList<StreamInfoRow>()) }
    // Re-read on the player's shared cosmetic beat rather than a timer of its own, so having the overlay
    // up alongside the position readout costs one wake-up rather than two (see [PlayerHeartbeat]).
    LaunchedEffect(player) {
        PlayerHeartbeat.beat.collect { rows = player.streamInfo() }
    }
    var details by remember(player) { mutableStateOf(false) }
    val attempts by FailedChannelAttempts.state.collectAsStateWithLifecycle()
    val shownRows = if (details) rows else rows.filter { it.label in setOf(
        StreamInfoLabel.ENGINE, StreamInfoLabel.FORMAT, StreamInfoLabel.VIDEO, StreamInfoLabel.AUDIO,
        StreamInfoLabel.BUFFER, StreamInfoLabel.LIVE_BUFFER, StreamInfoLabel.DECODER, StreamInfoLabel.HLS_SEGMENTS) }
    if (rows.isEmpty()) return
    val res = LocalResources.current
    @Composable fun durationText(seconds: Double?): String = if (seconds == null) stringResource(tv.own.owntv.R.string.ui_not_available)
        else stringResource(tv.own.owntv.R.string.ui_duration_seconds, java.text.NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
        }.format(seconds))
    val colors = OwnTVTheme.colors
    val scrollState = rememberScrollState()
    val scrollScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val labelWidth = (configuration.screenWidthDp * 0.18f).coerceIn(100f, 144f).dp
    val maximumHeight = (screenHeight - 64).coerceAtLeast(120).dp
    val scrollStep = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }
    LaunchedEffect(Unit) {
        repeat(3) {
            if (runCatching { focusRequester.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            androidx.compose.runtime.withFrameNanos { }
        }
    }

    Column(
        modifier = modifier
            .zIndex(10f)
            .widthIn(min = 300.dp, max = 640.dp)
            .heightIn(max = maximumHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            stringResource(CoreR.string.player_stream_info),
            style = MaterialTheme.typography.labelMedium,
            color = colors.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    val direction = when (event.key) {
                        Key.DirectionUp -> -1
                        Key.DirectionDown -> 1
                        Key.DirectionCenter, Key.Enter -> {
                            if (event.type == KeyEventType.KeyUp) { details = !details; scrollScope.launch { scrollState.scrollTo(0) } }
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    if (event.type == KeyEventType.KeyDown) {
                        scrollScope.launch { scrollState.scrollTo((scrollState.value + direction * scrollStep.toInt()).coerceIn(0, scrollState.maxValue)) }
                    }
                    true
                }
                .focusable(),
        ) {
            shownRows.forEach { row ->
                val value = row.value
                Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                    Text(
                        stringResource(row.label.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.width(labelWidth).padding(end = 12.dp),
                    )
                    Text(
                        when {
                            !details && value is StreamInfoValue.HlsSegments -> durationText(value.lastPublishedSeconds)
                            !details && value is StreamInfoValue.LiveBuffer -> stringResource(tv.own.owntv.R.string.ui_buffer_limits,
                                durationText(value.depthSeconds), durationText(value.maxBufferSeconds))
                            !details && value is StreamInfoValue.Decoder -> value.forSummary().displayText(res, compact = true)
                            else -> row.value.displayText(res, compact = true)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (details && attempts.isNotEmpty()) {
                Text(stringResource(tv.own.owntv.R.string.ui_attempts_title), color = colors.primary, modifier = Modifier.padding(top = 12.dp))
                attempts.asReversed().forEach { attempt ->
                    val time = android.text.format.DateFormat.getTimeFormat(androidx.compose.ui.platform.LocalContext.current).format(java.util.Date(attempt.atMs))
                    val reason = attempt.httpCode?.let { stringResource(tv.own.owntv.R.string.ui_http_status, it) } ?: stringResource(tv.own.owntv.R.string.ui_attempt_failed)
                    Text(stringResource(tv.own.owntv.R.string.ui_attempt_line, time, attempt.channel, reason), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OwnTVButton(stringResource(if (details) tv.own.owntv.R.string.ui_diagnostic_summary else tv.own.owntv.R.string.ui_diagnostic_details),
            onClick = { details = !details; scrollScope.launch { scrollState.scrollTo(0) } },
            modifier = Modifier.focusProperties { canFocus = false })
        Text(stringResource(tv.own.owntv.R.string.ui_diagnostic_toggle_hint), color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        if (scrollState.maxValue > 0) {
            Text(
                stringResource(tv.own.owntv.R.string.stream_info_scroll_hint),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Only hide a decoder's name when its classification conveys useful information on its own. */
internal fun StreamInfoValue.Decoder.forSummary(): StreamInfoValue.Decoder {
    val classification = when {
        hardware || kind == DecoderKind.HARDWARE -> DecoderKind.HARDWARE
        software || kind == DecoderKind.SOFTWARE -> DecoderKind.SOFTWARE
        else -> DecoderKind.NAMED
    }
    return copy(kind = classification, name = name.takeIf { classification == DecoderKind.NAMED })
}
