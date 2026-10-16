package tv.own.owntv.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FailedChannelAttempt(val channel: String, val atMs: Long, val httpCode: Int?)

/** Diagnostic metadata for this process only. No URLs, headers, tokens or raw errors. */
internal object FailedChannelAttempts {
    private val mutable = MutableStateFlow<List<FailedChannelAttempt>>(emptyList())
    val state = mutable.asStateFlow()
    private val status = Regex("(?:response\\s+code|http(?:\\s+status)?)[ :]+([45][0-9]{2})", RegexOption.IGNORE_CASE)
    internal fun httpCode(error: String?): Int? = error?.let { status.find(it)?.groupValues?.get(1)?.toIntOrNull() }
    private var observedOwner = 0L
    private var observedCode: Int? = null
    @Synchronized fun clear() {
        mutable.value = emptyList()
        observedOwner = 0L
        observedCode = null
    }
    @Synchronized fun observe(owner: Long, raw: String?) {
        if (owner <= 0L) return
        if (observedOwner != owner) { observedOwner = owner; observedCode = null }
        httpCode(raw)?.let { observedCode = it }
    }
    @Synchronized fun record(channel: String, error: String?, nowMs: Long = System.currentTimeMillis(), owner: Long = PlaybackSessionDiagnostics.state.value.owner) {
        mutable.value = (mutable.value + FailedChannelAttempt(channel.take(120), nowMs, httpCode(error) ?: PlaybackSessionDiagnostics.state.value.takeIf { it.owner == owner && owner > 0 }?.httpCode ?: observedCode.takeIf { owner > 0 && observedOwner == owner })).takeLast(10)
    }
}
