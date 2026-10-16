package tv.own.owntv.ui.components

/** A failed refresh is not an authoritative empty result. Keep valid previous data separately. */
internal sealed interface UiLoadState<out T> {
    data class Loading<T>(val previous: T? = null) : UiLoadState<T>
    data class Ready<T>(val value: T) : UiLoadState<T>
    data class Failed<T>(val previous: T? = null) : UiLoadState<T>
}

internal val <T> UiLoadState<T>.valueOrNull: T?
    get() = when (this) {
        is UiLoadState.Loading -> previous
        is UiLoadState.Ready -> value
        is UiLoadState.Failed -> previous
    }
