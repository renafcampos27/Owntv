package tv.own.owntv.features.settings

import tv.own.owntv.core.database.dao.Phase23PlaybackChannel
import tv.own.owntv.core.settings.ChannelPlaybackConfig

internal data class Phase23LoadedPlayback(val profileId: Long, val rows: List<ChannelPlaybackConfig>)

internal data class Phase23BatchScope(
    val profileId: Long,
    val channels: List<Phase23PlaybackChannel>,
    val sourceTotal: Int,
)

internal fun phase23FilterScope(
    channels: List<Phase23PlaybackChannel>,
    categoryId: Long?,
    query: String,
): List<Phase23PlaybackChannel> = channels.filter {
    (categoryId == null || it.categoryId == categoryId) &&
        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
}
