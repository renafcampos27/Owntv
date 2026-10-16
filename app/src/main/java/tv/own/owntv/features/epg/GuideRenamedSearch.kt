package tv.own.owntv.features.epg

import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.ChannelEntity

/** Resolve only matching renamed keys; never materialise the channel catalogue for a search. */
internal suspend fun guideRenamedCandidates(
    names: Map<String, String>, query: String, sourceIds: Set<Long>, limit: Int,
    remote: suspend (Long, List<String>) -> List<ChannelEntity>,
    named: suspend (Long, List<String>) -> List<ChannelEntity>,
): List<ChannelEntity> {
    if (query.isBlank() || limit <= 0) return emptyList()
    val keys = names.filter { (key, name) -> key.substringBefore(':').toLongOrNull() in sourceIds &&
        name.contains(query, ignoreCase = true) }.keys
    val result = linkedMapOf<Long, ChannelEntity>()
    for ((source, group) in keys.groupBy { it.substringBefore(':').toLong() }) {
        for (batch in group.chunked(400)) {
            val tails = batch.map(CustomizeKeys::tailOf)
            val allowed = batch.toSet()
            for (channel in remote(source, tails) + named(source, tails)) {
                if (channel.sourceId == source && CustomizeKeys.channel(channel) in allowed) result[channel.id] = channel
                if (result.size >= limit) return result.values.toList()
            }
        }
    }
    return result.values.toList()
}
