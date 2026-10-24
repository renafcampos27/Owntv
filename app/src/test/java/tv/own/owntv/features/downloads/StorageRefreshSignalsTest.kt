package tv.own.owntv.features.downloads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageRefreshSignalsTest {
    private data class Progress(val ids: List<Long>, val bytes: Int)
    @Test fun progressIsSampledButRootAndRemovalAreImmediate() = runTest {
        val progress = MutableStateFlow(Progress(listOf(1), 0))
        val root = MutableStateFlow("one")
        var queries = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            storageRefreshSignals(progress, root) { it.ids }.collect { queries++ }
        }
        runCurrent()
        assertEquals(1, queries)
        repeat(100) { progress.value = Progress(listOf(1), it + 1); runCurrent() }
        assertEquals(1, queries)
        advanceTimeBy(2_000); runCurrent()
        assertEquals(2, queries)
        root.value = "two"; runCurrent()
        assertEquals(3, queries)
        progress.value = Progress(emptyList(), 100); runCurrent()
        assertEquals(4, queries)
    }
}
