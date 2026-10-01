package tv.own.owntv.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One recent ten-second UI summary, in memory only. No addresses, channel IDs or key contents. */
internal object UiPerformanceSummary {
    private val latest = MutableStateFlow<UiPerformanceWindow.Snapshot?>(null)
    val snapshot = latest.asStateFlow()
    fun reset() { latest.value = null }
    fun publish(value: UiPerformanceWindow.Snapshot) { latest.value = value }
}
