package tv.own.owntv.diagnostics

import java.text.NumberFormat
import java.util.Locale

/** Process CPU is cumulative across threads; heaps are allocations, not total process memory. */
internal data class Phase4DiagnosticsProcessSample(
    val windowMs: Long?,
    val cpuMs: Long?,
    val javaHeapBytes: Long?,
    val nativeHeapBytes: Long?,
    val gcCount: Long?,
    val gcMs: Long?,
)

/** Missing counters and counter resets must never look like measured zero activity. */
internal fun phase4diagnosticsDelta(current: Long?, previous: Long?): Long? =
    if (current != null && previous != null && previous >= 0 && current >= previous) current - previous else null

internal object Phase4DiagnosticsFormat {
    fun integer(value: Long?, locale: Locale, unavailable: String): String =
        value?.takeIf { it >= 0 }?.let { NumberFormat.getIntegerInstance(locale).format(it) } ?: unavailable

    fun seconds(valueMs: Long?, locale: Locale, unavailable: String): String =
        decimal(valueMs?.takeIf { it > 0 }?.div(1_000.0), locale, unavailable)

    fun mebibytes(valueBytes: Long?, locale: Locale, unavailable: String): String =
        decimal(valueBytes?.takeIf { it >= 0 }?.div(1_048_576.0), locale, unavailable)

    private fun decimal(value: Double?, locale: Locale, unavailable: String): String =
        value?.let {
            NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 1
                maximumFractionDigits = 1
            }.format(it)
        } ?: unavailable
}
