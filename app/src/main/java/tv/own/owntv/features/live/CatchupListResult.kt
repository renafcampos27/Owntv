package tv.own.owntv.features.live

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import tv.own.owntv.core.database.entity.EpgProgrammeEntity

internal sealed interface CatchupListResult {
    data object Loading : CatchupListResult
    data class Ready(val programmes: List<EpgProgrammeEntity>) : CatchupListResult
    data object Error : CatchupListResult
}

internal data class CatchupFailurePrompt(
    val owner: CatchupOwner,
    val channelName: String,
    val retry: () -> Unit,
    val live: () -> Unit,
)

/** A cancelled popup must neither publish an empty guide nor display an error. */
internal suspend fun loadCatchupList(load: suspend () -> List<EpgProgrammeEntity>): CatchupListResult = try {
    val rows = load()
    currentCoroutineContext().ensureActive()
    CatchupListResult.Ready(rows)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    currentCoroutineContext().ensureActive()
    CatchupListResult.Error
}
