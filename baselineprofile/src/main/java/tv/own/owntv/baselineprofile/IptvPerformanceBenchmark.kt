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

/** Run on a prepared Android TV emulator, then compare release before/after. */
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
        exerciseIptvJourney()
    }

    @Test fun warmListNavigation() = rule.measureRepeated(
        packageName = IPTV_PACKAGE, metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(), iterations = 5,
        setupBlock = { startActivityAndWait(); prepareListNavigation() },
    ) { exerciseListNavigation() }

    @Test fun repeatedChannelHandover() = rule.measureRepeated(
        packageName = IPTV_PACKAGE, metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(), iterations = 5,
        setupBlock = { startActivityAndWait() },
    ) { exerciseRepeatChannels() }

    /** Guide-only movement; startup/import and entering the first programme are outside measurement. */
    @Test fun warmGuideTimelineNavigation() = rule.measureRepeated(
        packageName = IPTV_PACKAGE, metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(), iterations = 5,
        setupBlock = { startActivityAndWait(); prepareGuideNavigation() },
    ) { exerciseGuideNavigation() }
}
