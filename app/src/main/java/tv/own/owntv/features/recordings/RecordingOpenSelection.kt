package tv.own.owntv.features.recordings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.updateAndGet

/**
 * Monotonic generation controller for recording selection and playback opening.
 *
 * Guarantees that rapid D-pad browsing never allows a slow, superseded recording preparation
 * to overwrite a newer user selection or orphan a local playback server.
 */
internal class RecordingOpenSelection {
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()
    val current: Long get() = _revision.value
    private var foreground = false
    val canOpen: Boolean get() = foreground

    fun setForeground(value: Boolean) {
        if (foreground == value) return
        foreground = value
        if (!value) invalidate()
    }

    fun next(): Long = _revision.updateAndGet { it + 1 }
    fun owns(id: Long): Boolean = foreground && current == id

    suspend fun check(id: Long) {
        currentCoroutineContext().ensureActive()
        requireCurrent(id)
    }

    fun requireCurrent(id: Long) {
        if (!owns(id)) throw CancellationException("Superseded recording choice")
    }

    fun invalidate() {
        next()
    }
}
