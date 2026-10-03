package tv.own.owntv.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CancellationException

/** User input supersedes a pending automatic focus request; it is never consumed here. */
internal class FocusRequestGuard {
    var revision: Long = 0
        private set
    var pointerActive: Boolean = false
        private set
    fun invalidate() { revision++ }
    fun beginPointer() { pointerActive = true; invalidate() }
    fun endPointer() { pointerActive = false }
}

internal fun Modifier.cancelPendingFocusOnInput(guard: FocusRequestGuard): Modifier =
    onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown) guard.invalidate()
        false
    }.pointerInput(guard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            guard.beginPointer()
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
            } finally { guard.endPointer() }
        }
    }

internal suspend fun requestBoundedFocus(
    request: () -> Boolean,
    isCurrent: () -> Boolean,
    awaitLayout: suspend () -> Unit = { withFrameNanos { } },
    attempts: Int = 10,
): Boolean {
    repeat(attempts) {
        awaitLayout()
        if (!isCurrent()) return false
        val accepted = try { request() } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) { false }
        if (accepted) return true
    }
    return false
}

internal suspend fun FocusRequester.requestBoundedFocus(
    guard: FocusRequestGuard,
    revision: Long = guard.revision,
): Boolean {
    return requestBoundedFocus({ requestFocus() }, { guard.revision == revision && !guard.pointerActive })
}
