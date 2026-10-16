package tv.own.owntv.diagnostics

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class Phase4DiagnosticsMetricsTest {
    private val pt = Locale.forLanguageTag("pt-PT")
    private val unavailable = "N/D"

    @Test fun `CPU time preserves multi thread totals and actual delayed window`() {
        assertEquals(24_000L, phase4diagnosticsDelta(50_000, 26_000))
        assertEquals("12,5", Phase4DiagnosticsFormat.seconds(12_500, pt, unavailable))
        assertEquals("0", Phase4DiagnosticsFormat.integer(0, pt, unavailable))
    }

    @Test fun `heap units use mebibytes without combining allocations`() {
        assertEquals("1,5", Phase4DiagnosticsFormat.mebibytes(1_572_864, pt, unavailable))
        assertEquals("512,0", Phase4DiagnosticsFormat.mebibytes(536_870_912, pt, unavailable))
        assertEquals("0,0", Phase4DiagnosticsFormat.mebibytes(0, pt, unavailable))
    }

    @Test fun `unknown values remain unavailable instead of zero`() {
        assertEquals(unavailable, Phase4DiagnosticsFormat.integer(null, pt, unavailable))
        assertEquals(unavailable, Phase4DiagnosticsFormat.seconds(null, pt, unavailable))
        assertEquals(unavailable, Phase4DiagnosticsFormat.mebibytes(null, pt, unavailable))
    }

    @Test fun `invalid values and zero windows are unavailable`() {
        assertEquals(unavailable, Phase4DiagnosticsFormat.integer(-1, pt, unavailable))
        assertEquals(unavailable, Phase4DiagnosticsFormat.seconds(0, pt, unavailable))
        assertEquals(unavailable, Phase4DiagnosticsFormat.seconds(-1, pt, unavailable))
        assertEquals(unavailable, Phase4DiagnosticsFormat.mebibytes(-1, pt, unavailable))
    }

    @Test fun `missing or reset cumulative GC counters are unavailable`() {
        assertNull(phase4diagnosticsDelta(null, 10))
        assertNull(phase4diagnosticsDelta(10, null))
        assertNull(phase4diagnosticsDelta(2, 10))
        assertNull(phase4diagnosticsDelta(10, -1))
        assertEquals(0L, phase4diagnosticsDelta(10, 10))
        assertEquals(3L, phase4diagnosticsDelta(13, 10))
    }

    @Test fun `format follows selected locale`() {
        assertEquals("1.5", Phase4DiagnosticsFormat.mebibytes(1_572_864, Locale.US, unavailable))
        assertEquals("1,5", Phase4DiagnosticsFormat.mebibytes(1_572_864, pt, unavailable))
    }

    @Test fun `summary publishes process and frame values together and resets`() {
        val frames = UiPerformanceWindow().take()
        val process = Phase4DiagnosticsProcessSample(12_500, 24_000, 1_572_864, 536_870_912, null, 0)
        UiPerformanceSummary.publish(frames, process)
        assertEquals(frames, UiPerformanceSummary.snapshot.value?.ui)
        assertEquals(process, UiPerformanceSummary.snapshot.value?.process)
        assertNull(UiPerformanceSummary.snapshot.value?.process?.gcCount)
        UiPerformanceSummary.reset()
        assertNull(UiPerformanceSummary.snapshot.value)
    }
}
