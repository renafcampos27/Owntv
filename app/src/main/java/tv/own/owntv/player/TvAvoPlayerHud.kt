package tv.own.owntv.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tv.own.owntv.R
import tv.own.owntv.ui.components.OwnTVSpinner
import tv.own.owntv.ui.theme.LocalActionSurface

/** Separate composition so advanced controls, diagnostics and their polling are never started here. */
@Composable
internal fun TvAvoPlayerHud(
    player: PlaybackEngine,
    onBack: () -> Unit,
    channelName: String?,
    channelKey: Long,
    onChannelUp: (() -> Unit)?,
    onChannelDown: (() -> Unit)?,
    programming: (@Composable () -> Unit)?,
    reopenSeconds: StateFlow<Int>?,
    inert: Boolean,
    modifier: Modifier,
) {
    val requested by player.playbackRequested.collectAsStateWithLifecycle()
    val buffering by player.buffering.collectAsStateWithLifecycle()
    val error by player.error.collectAsStateWithLifecycle()
    val meta by player.currentMeta.collectAsStateWithLifecycle()
    val waiting by remember(reopenSeconds) {
        reopenSeconds?.map { it > 0 }?.distinctUntilChanged() ?: flowOf(false)
    }.collectAsStateWithLifecycle(false)
    var visible by remember { mutableStateOf(true) }
    var activity by remember { mutableIntStateOf(0) }
    val showControls = visible || !requested || error != null
    val catchFocus = remember { FocusRequester() }
    val canZap = onChannelUp != null && onChannelDown != null
    val toggle: () -> Unit = { if (error != null) player.retry() else player.togglePlayPause(); visible = true; activity++ }

    BackHandler(enabled = !inert, onBack = onBack)
    LaunchedEffect(player, channelKey) { visible = true; activity++ }
    LaunchedEffect(visible, requested, buffering, waiting, error, inert, activity) {
        if (visible && requested && !buffering && !waiting && error == null && !inert) {
            delay(4_500)
            visible = false
        }
    }
    LaunchedEffect(inert, player, channelKey) {
        if (inert) return@LaunchedEffect
        repeat(4) {
            withFrameNanos { }
            if (runCatching { catchFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }

    CompositionLocalProvider(LocalActionSurface provides null) {
        Box(modifier.fillMaxSize().onPreviewKeyEvent { event ->
            val down = event.type == KeyEventType.KeyDown
            val action = tvAvoKeyAction(event.nativeKeyEvent.keyCode, down, canZap, inert)
            if (down && !inert) activity++
            when (action) {
                TvAvoAction.PASS -> false
                TvAvoAction.CONSUME -> true
                TvAvoAction.BACK -> { if (!event.nativeKeyEvent.isCanceled) onBack(); true }
                TvAvoAction.SHOW -> { if (!event.nativeKeyEvent.isCanceled) visible = true; true }
                // Same delta convention as the regular HUD: physical Up/CH+ advances the list.
                TvAvoAction.NEXT -> { onChannelDown?.invoke(); visible = true; true }
                TvAvoAction.PREVIOUS -> { onChannelUp?.invoke(); visible = true; true }
                TvAvoAction.TOGGLE -> { if (event.nativeKeyEvent.repeatCount == 0) toggle(); true }
                TvAvoAction.PLAY -> {
                    if (event.nativeKeyEvent.repeatCount == 0) { if (error != null) player.retry() else player.play() }
                    visible = true; true
                }
                TvAvoAction.PAUSE -> { player.pause(); visible = true; true }
            }
        }) {
            Box(Modifier.fillMaxSize().focusRequester(catchFocus).focusable())
            if (showControls) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(28.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.80f)).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(channelName?.takeIf { it.isNotBlank() } ?: meta.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        programming?.invoke()
                        if (error != null) Text(stringResource(R.string.tv_avo_channel_unavailable),
                            style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
            if (error == null && (buffering || waiting)) OwnTVSpinner(sizeDp = 56, modifier = Modifier.align(Alignment.Center))
        }
    }
}
