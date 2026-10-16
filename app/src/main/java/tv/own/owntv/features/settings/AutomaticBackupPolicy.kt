package tv.own.owntv.features.settings

import java.time.Instant
import java.time.ZoneOffset

/** Calendar months, rather than approximating every month as thirty days. */
internal object AutomaticBackupPolicy {
    const val DEFAULT_MONTHS = 6
    val intervals = listOf(0, 3, 6, 12)
    fun valid(months: Int) = months in intervals
    fun nextAt(months: Int, anchor: Long): Long? {
        require(valid(months))
        if (months == 0 || anchor <= 0L) return null
        return Instant.ofEpochMilli(anchor).atZone(ZoneOffset.UTC).plusMonths(months.toLong()).toInstant().toEpochMilli()
    }
    fun due(months: Int, anchor: Long, now: Long): Boolean = nextAt(months, anchor)?.let { now >= it } ?: false
}
