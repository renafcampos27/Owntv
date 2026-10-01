package tv.own.owntv.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Automatic notices wait for user-owned windows; nested user dialogs remain deliberate. */
internal class PopupRegistry {
    private val owners = MutableStateFlow<Set<Any>>(emptySet())
    val active = owners.asStateFlow()
    fun enter(owner: Any) { owners.update { it + owner } }
    fun leave(owner: Any) { owners.update { it - owner } }
}

internal val popupPresence = PopupRegistry()
