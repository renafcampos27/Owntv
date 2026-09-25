package tv.own.owntv.features.live

import java.util.Locale
import kotlinx.coroutines.withTimeoutOrNull
import tv.own.owntv.core.database.entity.ChannelEntity

/** Only trailing transport/quality labels are removed. Country, number and programme stay intact. */
internal object ChannelAlternatives {
    private val number = Regex("^\\s*\\d+\\.\\s*")
    private val quality = Regex("\\s+\\(?(full\\s*hd|fhd|hd|sd|hq|low|hevc|hvec|h[. ]?26[45]|1080p|720p|4k|uhd)\\)?$", RegexOption.IGNORE_CASE)
    fun key(name: String): String {
        var value = name.replace(number, "").trim().lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ").replace(Regex("\\s*:\\s*"), ":")
        while (quality.containsMatchIn(value)) value = value.replace(quality, "").trim()
        // Explicitly confirmed by the user; do not infer other channel aliases.
        return when (value) {
            "pt:canal historia" -> "pt:historia"
            "pt:discovery channel" -> "pt:discovery"
            else -> value
        }
    }
    fun searchTerm(name: String): String = key(name).substringAfter(':').substringBefore(' ')
    fun ordered(original: ChannelEntity, candidates: List<ChannelEntity>): List<ChannelEntity> =
        candidates.filter { it.sourceId == original.sourceId && key(it.name) == key(original.name) }
            .sortedWith(compareBy<ChannelEntity> { rank(it.name) }.thenBy { it.sortOrder }.thenBy { it.id })
            .let { listOf(original) + it }
            .distinctBy { it.id }
            .distinctBy { it.streamUrl }

    private fun rank(name: String): Int {
        val suffix = quality.find(name)?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.replace(" ", "")
        return when (suffix) {
            "fullhd", "fhd", "1080p", "4k", "uhd" -> 0
            "hd", "720p" -> 1
            "hq" -> 2
            "hevc", "hvec", "h265", "h.265", "h264", "h.264" -> 4
            "low", "sd" -> 5
            else -> 3
        }
    }
}

/** Each attempt owns its timeout. Cancellation propagates, so a remote action always wins. */
internal suspend fun <T> tryChannelAlternatives(
    initial: T,
    alternatives: suspend () -> List<T>,
    timeoutMs: Long = 3_000,
    attempt: suspend (T) -> Boolean,
    failed: () -> Unit,
    switching: (T) -> Unit,
): Boolean {
    suspend fun tryOne(item: T): Boolean {
        val result = withTimeoutOrNull(timeoutMs) { attempt(item) } == true
        if (!result) failed()
        return result
    }
    if (tryOne(initial)) return true
    for (item in alternatives()) {
        switching(item)
        if (tryOne(item)) return true
    }
    return false
}
