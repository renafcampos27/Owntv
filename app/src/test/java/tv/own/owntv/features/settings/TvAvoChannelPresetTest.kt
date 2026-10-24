package tv.own.owntv.features.settings

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.customize.SectionCustomizations

@OptIn(ExperimentalCoroutinesApi::class)
class TvAvoChannelPresetTest {
    @Test fun enablingPoliciesPreservesManualPriorityAndGroupAssignments() {
        val original = SectionCustomizations(channelVersionOrders = mapOf("station" to "[3,1,2]"),
            channelVersionGroups = mapOf("version" to "station"), categoryNames = mapOf("category" to "name"))
        val enabled = tvAvoChannelVersions(original)
        assertEquals(original.copy(groupChannelVersions = true, prioritizeChannelVersions = true), enabled)
        assertEquals(enabled, tvAvoChannelVersions(enabled))
    }

    @Test fun activationAndProfileEntryApplyOnceWithoutRewritingOnUnrelatedPreferences() = runTest {
        val options = MutableStateFlow(SimpleModeOptions())
        val profile = MutableStateFlow(-1L)
        val prepared = mutableListOf<Long>()
        var recoveries = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeTvAvoChannelPreset(options, profile, { prepared += it }, { recoveries++ })
        }
        runCurrent()
        options.value = options.value.copy(tvAvoMode = true); runCurrent()
        assertTrue(prepared.isEmpty())
        profile.value = 7; runCurrent()
        assertEquals(listOf(7L), prepared); assertEquals(1, recoveries)
        options.value = options.value.copy(recoveryTimeoutSeconds = 19); runCurrent()
        assertEquals(1, prepared.size)
        profile.value = 8; runCurrent(); assertEquals(listOf(7L, 8L), prepared)
        options.value = options.value.copy(tvAvoMode = false); runCurrent()
        profile.value = 9; runCurrent(); assertEquals(2, prepared.size)
        options.value = options.value.copy(tvAvoMode = true); runCurrent()
        assertEquals(listOf(7L, 8L, 9L), prepared)
    }
}
