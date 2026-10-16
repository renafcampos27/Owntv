package tv.own.owntv.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One recent measurement window, in memory only. No addresses, channel IDs or key contents. */
internal object UiPerformanceSummary {
    data class Snapshot(val ui: UiPerformanceWindow.Snapshot, val process: Phase4DiagnosticsProcessSample)
    private val latest = MutableStateFlow<Snapshot?>(null)
    val snapshot = latest.asStateFlow()
    fun reset() { latest.value = null }
    fun publish(ui: UiPerformanceWindow.Snapshot, process: Phase4DiagnosticsProcessSample) {
        latest.value = Snapshot(ui, process)
    }
}
