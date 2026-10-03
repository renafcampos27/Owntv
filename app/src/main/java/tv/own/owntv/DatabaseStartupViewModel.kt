package tv.own.owntv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The database opening result belongs to the retained Activity store, not one Activity instance. */
internal class DatabaseStartupViewModel(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val openDatabase: suspend () -> Unit,
) : ViewModel() {
    sealed interface State {
        data object Loading : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state = mutableState.asStateFlow()
    private var probe: Job? = null

    init { retry() }

    /** User retry starts one new probe; rotation and repeated callbacks cannot duplicate it. */
    fun retry() {
        if (probe?.isActive == true) return
        mutableState.value = State.Loading
        probe = viewModelScope.launch(dispatcher) {
            mutableState.value = try {
                openDatabase()
                State.Ready
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failed: Exception) {
                State.Failed(failed.message ?: failed.javaClass.simpleName)
            }
        }
    }
}
