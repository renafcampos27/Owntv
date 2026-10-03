package tv.own.owntv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.currentKoinScope
import org.koin.core.annotation.KoinInternalApi
import org.koin.viewmodel.defaultExtras
import org.koin.viewmodel.resolveViewModel

/** Uses the same owner/key as koinViewModel, without starting an unused destination's jobs. */
@OptIn(KoinInternalApi::class)
@Composable
internal inline fun <reified T : ViewModel> rememberLazyShellViewModel(): Lazy<T> {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val scope = currentKoinScope()
    return remember(owner, scope) {
        lazy(LazyThreadSafetyMode.NONE) {
            resolveViewModel(T::class, owner.viewModelStore, extras = defaultExtras(owner), scope = scope)
        }
    }
}
