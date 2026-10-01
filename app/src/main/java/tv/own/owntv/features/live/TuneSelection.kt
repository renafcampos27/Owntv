package tv.own.owntv.features.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Main-thread choice generation. URL equality does not imply the same user request. */
internal class TuneSelection {
    private val _revision = MutableStateFlow(0L)
    val revision = _revision.asStateFlow()
    val current: Long get() = _revision.value
    fun next(): Long = (current + 1).also { _revision.value = it }
    fun owns(id: Long) = current == id
    suspend fun check(id: Long) {
        currentCoroutineContext().ensureActive()
        requireCurrent(id)
    }
    fun requireCurrent(id: Long) {
        if (!owns(id)) throw CancellationException("Superseded channel choice")
    }
}

/** A modal belongs to the choice that opened it, including an A→B→A sequence. */
data class CatchupOwner(val profileId: Long, val generation: Long, val channelId: Long, val sourceId: Long) {
    fun accepts(profile: Long, choice: Long, sources: List<Long>) =
        profileId == profile && generation == choice && sourceId in sources
}
