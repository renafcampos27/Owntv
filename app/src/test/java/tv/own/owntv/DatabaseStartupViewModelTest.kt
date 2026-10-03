package tv.own.owntv

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

class DatabaseStartupViewModelTest {
    @Test fun repeatedRetryWhileOpeningKeepsSingleProbe() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val model = DatabaseStartupViewModel(coroutineContext[ContinuationInterceptor] as CoroutineDispatcher) {
            calls++
            entered.complete(Unit)
            finish.await()
        }
        entered.await()
        repeat(4) { model.retry() }
        assertEquals(1, calls)
        finish.complete(Unit)
        assertEquals(DatabaseStartupViewModel.State.Ready, model.state.first { it !is DatabaseStartupViewModel.State.Loading })
    }

    @Test fun delayedFailureIsObservableAndExplicitRetryCanRecover() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val model = DatabaseStartupViewModel(coroutineContext[ContinuationInterceptor] as CoroutineDispatcher) {
            calls++
            if (calls == 1) {
                entered.complete(Unit)
                finish.await()
                error("migration failed")
            }
        }
        entered.await()
        assertEquals(DatabaseStartupViewModel.State.Loading, model.state.value)
        finish.complete(Unit)
        assertEquals("migration failed", (model.state.first { it is DatabaseStartupViewModel.State.Failed } as DatabaseStartupViewModel.State.Failed).message)
        model.retry()
        assertEquals(DatabaseStartupViewModel.State.Ready, model.state.first { it is DatabaseStartupViewModel.State.Ready })
        assertEquals(2, calls)
    }

    @Test fun recreatedOwnerUsingSameStoreKeepsResultAndDoesNotOpenAgain() = runBlocking {
        val store = ViewModelStore()
        var calls = 0
        val dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                DatabaseStartupViewModel(dispatcher) { calls++ } as T
        }
        try {
            val initial = ViewModelProvider(store, factory)[DatabaseStartupViewModel::class.java]
            initial.state.first { it is DatabaseStartupViewModel.State.Ready }
            val recreated = ViewModelProvider(store, factory)[DatabaseStartupViewModel::class.java]
            assertSame(initial, recreated)
            assertEquals(DatabaseStartupViewModel.State.Ready, recreated.state.value)
            assertEquals(1, calls)
        } finally { store.clear() }
    }
}
