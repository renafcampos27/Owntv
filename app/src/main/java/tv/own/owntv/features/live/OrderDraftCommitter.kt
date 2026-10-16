package tv.own.owntv.features.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Main-thread editor lifecycle. A save owns its exact pending snapshot until completion. */
internal class OrderDraftCommitter<T : Any>(
    private val state: MutableStateFlow<T?>,
    private val isSaving: (T) -> Boolean,
    private val mark: (T, Boolean, Boolean) -> T,
) {
    fun commit(
        scope: CoroutineScope,
        ownerValid: suspend (T) -> Boolean,
        persist: suspend (T) -> Unit,
        onCommitted: () -> Unit,
    ) {
        val draft = state.value ?: return
        if (isSaving(draft)) return
        val pending = mark(draft, true, false)
        state.value = pending
        scope.launch {
            try {
                if (!ownerValid(pending)) {
                    if (state.value === pending) state.value = mark(pending, false, true)
                    return@launch
                }
                persist(pending)
                if (state.value === pending) {
                    onCommitted()
                    state.value = null
                }
            } catch (cancelled: CancellationException) {
                if (state.value === pending) state.value = mark(pending, false, false)
                throw cancelled
            } catch (_: Exception) {
                if (state.value === pending) state.value = mark(pending, false, true)
            }
        }
    }
}
