package tv.own.owntv.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run on a prepared TV emulator and phone emulator, then compare release before/after. */
@RunWith(AndroidJUnit4::class)
class IptvPerformanceBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun startupAndIptvNavigation() = rule.measureRepeated(
        packageName = IPTV_PACKAGE,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        exerciseIptvJourney(remote = remoteDevice())
    }
}
