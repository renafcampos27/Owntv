package tv.own.owntv.diagnostics

/** Fixed-size histograms; no per-frame objects or unbounded sample lists. */
internal class UiPerformanceWindow {
    private val frameTimes = LongArray(1001)
    private val keyQueueTimes = LongArray(1001)
    private var frames = 0L
    private var slowFrames = 0L
    private var reportsDropped = 0L
    private var keys = 0L

    @Synchronized fun frame(durationNs: Long, deadlineNs: Long, dropped: Int, firstDraw: Boolean) {
        reportsDropped += dropped.coerceAtLeast(0)
        if (firstDraw || durationNs < 0) return
        frames++
        if (deadlineNs > 0 && durationNs >= deadlineNs) slowFrames++
        frameTimes[((durationNs + 999_999) / 1_000_000).coerceIn(0, 1000).toInt()]++
    }

    @Synchronized fun key(queueMs: Long) {
        keys++
        keyQueueTimes[queueMs.coerceIn(0, 1000).toInt()]++
    }

    @Synchronized fun take(): Snapshot {
        val result = Snapshot(frames, slowFrames, reportsDropped, percentile(frameTimes, frames),
            keys, percentile(keyQueueTimes, keys))
        frameTimes.fill(0); keyQueueTimes.fill(0)
        frames = 0; slowFrames = 0; reportsDropped = 0; keys = 0
        return result
    }

    private fun percentile(buckets: LongArray, count: Long): Int? {
        if (count == 0L) return null
        val rank = (count * 95 + 99) / 100
        var seen = 0L
        for (i in buckets.indices) {
            seen += buckets[i]
            if (seen >= rank) return i
        }
        return null
    }

    data class Snapshot(val frames: Long, val slowFrames: Long, val reportsDropped: Long,
                        val frameP95BucketMs: Int?, val keys: Long, val keyQueueP95BucketMs: Int?)
}
