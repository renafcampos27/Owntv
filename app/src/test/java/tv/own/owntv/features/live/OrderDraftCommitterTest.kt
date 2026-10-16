package tv.own.owntv.features.live

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class OrderDraftCommitterTest {
    private data class Draft(val items: List<Int>, val saving: Boolean = false, val failed: Boolean = false)
    private fun committer(state: MutableStateFlow<Draft?>) = OrderDraftCommitter(state, { it.saving }) { draft, saving, failed ->
        draft.copy(saving = saving, failed = failed)
    }

    @Test fun duplicateSaveIsIgnoredAndEditorClosesOnlyAfterPersistence() = runBlocking {
        val state = MutableStateFlow<Draft?>(Draft(listOf(2, 1)))
        val saver = committer(state)
        val finish = CompletableDeferred<Unit>()
        var writes = 0
        var commits = 0
        val persist: suspend (Draft) -> Unit = { writes++; finish.await() }
        saver.commit(this, { true }, persist) { commits++ }
        saver.commit(this, { true }, persist) { commits++ }
        yield()
        assertEquals(1, writes)
        assertTrue(state.value!!.saving)
        assertEquals(0, commits)
        finish.complete(Unit)
        yield()
        assertNull(state.value)
        assertEquals(1, commits)
    }

    @Test fun failedWriteKeepsDraftAndAllowsExplicitRetry() = runBlocking {
        val state = MutableStateFlow<Draft?>(Draft(listOf(2, 1)))
        val saver = committer(state)
        var commits = 0
        saver.commit(this, { true }, { throw IOException() }) { commits++ }
        yield()
        assertEquals(listOf(2, 1), state.value!!.items)
        assertFalse(state.value!!.saving)
        assertTrue(state.value!!.failed)
        assertEquals(0, commits)
        saver.commit(this, { true }, {}) { commits++ }
        yield()
        assertNull(state.value)
        assertEquals(1, commits)
    }

    @Test fun oldCompletionCannotCloseOrMutateANewEditor() = runBlocking {
        val state = MutableStateFlow<Draft?>(Draft(listOf(2, 1)))
        val saver = committer(state)
        val finish = CompletableDeferred<Unit>()
        var commits = 0
        saver.commit(this, { true }, { finish.await() }) { commits++ }
        yield()
        val replacement = Draft(listOf(9, 8))
        state.value = replacement
        finish.complete(Unit)
        yield()
        assertSame(replacement, state.value)
        assertEquals(0, commits)
    }

    @Test fun changedOwnerDoesNotWriteToAnyProfile() = runBlocking {
        val state = MutableStateFlow<Draft?>(Draft(listOf(2, 1)))
        committer(state).commit(this, { false }, { fail("must not persist") }) { fail("must not close") }
        yield()
        assertTrue(state.value!!.failed)
        assertFalse(state.value!!.saving)
    }

    @Test fun cancellationDoesNotMasqueradeAsStorageFailure() = runBlocking {
        val state = MutableStateFlow<Draft?>(Draft(listOf(2, 1)))
        committer(state).commit(this, { true }, { throw CancellationException() }) { fail("must not close") }
        yield()
        assertEquals(Draft(listOf(2, 1)), state.value)
    }
}
