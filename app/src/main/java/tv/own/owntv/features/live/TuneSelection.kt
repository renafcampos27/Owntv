package tv.own.owntv.features.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Main-thread choice generation. URL equality does not imply the same user request. */
internal class TuneSelection {
    var current: Long = 0
        private set
    fun next(): Long = (++current)
    fun owns(id: Long) = current == id
    suspend fun check(id: Long) {
        currentCoroutineContext().ensureActive()
        requireCurrent(id)
    }
    fun requireCurrent(id: Long) {
        if (!owns(id)) throw CancellationException("Superseded channel choice")
    }
}
