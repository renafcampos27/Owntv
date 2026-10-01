package tv.own.owntv.player

/** A remote gesture previews locally; only release submits a seek to the player. */
internal class LiveScrubGesture {
    private var direction = 0
    private var startedMs = 0L
    private var lastStepMs = 0L
    var targetSec: Int? = null
        private set

    fun press(backward: Boolean, nowMs: Long, currentSec: Int, windowSec: Int): Int {
        val nextDirection = if (backward) 1 else -1
        if (direction != nextDirection) {
            direction = nextDirection
            startedMs = nowMs
            lastStepMs = nowMs - 100L
        }
        if (nowMs - lastStepMs >= 100L) {
            val step = when {
                nowMs - startedMs >= 3_000L -> 60
                nowMs - startedMs >= 1_000L -> 30
                else -> 10
            }
            targetSec = ((targetSec ?: currentSec).toLong() + direction * step)
                .coerceIn(0L, windowSec.coerceAtLeast(0).toLong()).toInt()
            lastStepMs = nowMs
        }
        return targetSec ?: currentSec
    }

    fun release(backward: Boolean, currentSec: Int): Int? {
        if (direction != if (backward) 1 else -1) return null
        return commit(currentSec)
    }

    fun commit(currentSec: Int): Int? {
        val delta = targetSec?.minus(currentSec)
        cancel()
        return delta?.takeIf { it != 0 }
    }

    fun cancel() { direction = 0; targetSec = null }
}
