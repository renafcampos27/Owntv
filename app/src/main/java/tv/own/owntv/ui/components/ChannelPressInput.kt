package tv.own.owntv.ui.components

import android.os.SystemClock
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Shared touch/remote timings; cancellation and repeats never play the channel on release. */
internal fun Modifier.channelPressInput(
    interaction: MutableInteractionSource,
    enabled: Boolean,
    selected: Boolean,
    onPlay: () -> Unit,
    onVersions: () -> Unit,
    onMenu: () -> Unit,
): Modifier = composed {
    val play by rememberUpdatedState(onPlay)
    val versions by rememberUpdatedState(onVersions)
    val menu by rememberUpdatedState(onMenu)
    val scope = rememberCoroutineScope()
    val gesture = remember { ChannelPressGesture() }
    val pending = remember { ChannelPressPending() }
    fun dispatch(action: ChannelPressAction?) {
        when (action) {
            ChannelPressAction.PLAY -> play()
            ChannelPressAction.VERSIONS -> versions()
            ChannelPressAction.MENU -> menu()
            null -> Unit
        }
    }
    fun finish(cancelled: Boolean, now: Long = SystemClock.uptimeMillis()) {
        pending.timer?.cancel()
        pending.timer = null
        pending.press?.let {
            interaction.tryEmit(if (cancelled) PressInteraction.Cancel(it) else PressInteraction.Release(it))
        }
        pending.press = null
        if (cancelled) gesture.cancel() else dispatch(gesture.release(now))
    }
    fun begin(now: Long, position: Offset) {
        if (!gesture.begin(now)) return
        pending.press = PressInteraction.Press(position).also { interaction.tryEmit(it) }
        pending.timer = scope.launch {
            delay(ChannelPressGesture.MENU_MS)
            dispatch(gesture.menuDeadline())
        }
    }
    DisposableEffect(enabled) { onDispose { finish(cancelled = true) } }

    semantics(mergeDescendants = true) {
        role = Role.Button
        this.selected = selected
        if (!enabled) disabled()
        onClick { if (enabled) play(); enabled }
        onLongClick { if (enabled) menu(); enabled }
    }.onPreviewKeyEvent { event ->
        val activation = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar
        if (!activation || !enabled) false
        else {
            when (event.type) {
                KeyEventType.KeyDown -> if (event.nativeKeyEvent.repeatCount == 0) begin(event.nativeKeyEvent.eventTime, Offset.Zero)
                KeyEventType.KeyUp -> finish(event.nativeKeyEvent.isCanceled, event.nativeKeyEvent.eventTime)
                else -> Unit
            }
            true
        }
    }.onFocusChanged { if (!it.isFocused) finish(cancelled = true) }
        .focusable(enabled, interaction)
        .pointerInput(enabled, interaction) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                begin(SystemClock.uptimeMillis(), down.position)
                try {
                    val up = waitForUpOrCancellation()
                    up?.consume()
                    finish(cancelled = up == null)
                } finally {
                    finish(cancelled = true)
                }
            }
        }
}

private class ChannelPressPending {
    var timer: Job? = null
    var press: PressInteraction.Press? = null
}
