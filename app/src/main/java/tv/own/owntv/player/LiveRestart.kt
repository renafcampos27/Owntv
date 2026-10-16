package tv.own.owntv.player

/** Only restart an airing programme whose beginning is within the provider's usable archive. */
internal fun restartableProgramme(programmes: List<LiveProgramme>, nowMs: Long, windowSec: Int): LiveProgramme? {
    if (windowSec < 60) return null
    return programmes.firstOrNull {
        it.startMs >= 0L && it.startMs < it.stopMs && nowMs >= it.startMs && nowMs < it.stopMs &&
            nowMs - it.startMs in 60_000L..windowSec.toLong() * 1_000L
    }
}
