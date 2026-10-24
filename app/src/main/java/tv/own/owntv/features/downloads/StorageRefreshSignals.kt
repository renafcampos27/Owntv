package tv.own.owntv.features.downloads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/** Structural/root changes are immediate; progress-only events refresh informational space at most every 2s. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal fun <T, S> storageRefreshSignals(progress: Flow<T>, roots: Flow<String>, shape: (T) -> S): Flow<Unit> =
    roots.distinctUntilChanged().flatMapLatest {
        merge(progress.map(shape).distinctUntilChanged().map { Unit },
            progress.drop(1).map { Unit }.sample(2_000L)).conflate()
    }
