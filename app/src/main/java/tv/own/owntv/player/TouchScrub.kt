package tv.own.owntv.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

internal val LocalTouchScrubActivity = compositionLocalOf<(Boolean) -> Unit> { {} }
internal val LocalTouchScrubContext = compositionLocalOf<Any?> { null }

internal fun timelineFraction(x: Float, width: Int): Float = if (width > 0) (x / width).coerceIn(0f, 1f) else 0f
internal fun timelineDelta(fraction: Float, duration: Long, position: Long): Long =
    (fraction.coerceIn(0f, 1f).toDouble() * duration.coerceAtLeast(0)).toLong() - position

/** The release belongs to the context captured on down and can be consumed only once. */
internal class TouchScrubCommitGuard(private val owner: Any?) {
    private var finished = false
    fun release(currentOwner: Any?, enabled: Boolean): Boolean {
        if (finished) return false
        finished = true
        return enabled && currentOwner == owner
    }
    fun cancel() { finished = true }
}

/** Preview only while pressed; a released single-pointer gesture commits once. Cancellation never seeks. */
@Composable
internal fun Modifier.touchScrub(key: Any?, enabled: Boolean, preview: (Float?) -> Unit, commit: (Float) -> Unit): Modifier {
    val owner = key to LocalTouchScrubContext.current
    val currentPreview by rememberUpdatedState(preview)
    val currentCommit by rememberUpdatedState(commit)
    val currentKey by rememberUpdatedState(owner)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentActivity by rememberUpdatedState(LocalTouchScrubActivity.current)
    return if (!enabled) this else pointerInput(owner, enabled) {
        try {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                var fraction = timelineFraction(down.position.x, size.width)
                val guard = TouchScrubCommitGuard(owner)
                currentActivity(true)
                try {
                    currentPreview(fraction)
                    var released = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (currentKey != owner || !currentEnabled ||
                            event.changes.count { it.pressed } > 1 || change.isConsumed) break
                        fraction = timelineFraction(change.position.x, size.width)
                        change.consume()
                        currentPreview(fraction)
                        if (!change.pressed) { released = true; break }
                    }
                    if (released && guard.release(currentKey, currentEnabled)) currentCommit(fraction)
                } finally {
                    guard.cancel()
                    currentPreview(null)
                    currentActivity(false)
                }
            }
        } finally { currentPreview(null); currentActivity(false) }
    }
}
