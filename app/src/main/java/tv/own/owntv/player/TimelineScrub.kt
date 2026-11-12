package tv.own.owntv.player

/** Pure preview state: repeated arrows select a target without repeatedly flushing the decoder. */
internal data class TimelineScrub(val targetMs: Long? = null, val generation: Long? = null) {
    fun move(position: Long, duration: Long, delta: Long, owner: Long): TimelineScrub {
        if (duration <= 0) return TimelineScrub()
        if (delta == 0L) return if (generation == owner) this else TimelineScrub()
        val base = (targetMs?.takeIf { generation == owner } ?: position).coerceIn(0, duration)
        val target = when {
            delta > 0 && delta > duration - base -> duration
            delta < 0 && delta < -base -> 0L
            else -> base + delta
        }
        return TimelineScrub(target, owner)
    }

    fun deltaToCommit(position: Long, duration: Long, owner: Long): Long? {
        val target = targetMs?.takeIf { generation == owner && duration > 0 } ?: return null
        return (target.coerceIn(0, duration) - position.coerceIn(0, duration)).takeIf { it != 0L }
    }
}
