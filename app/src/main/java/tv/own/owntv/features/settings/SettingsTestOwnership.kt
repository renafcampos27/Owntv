package tv.own.owntv.features.settings

/** A reset retires a test even if its blocking backend ignores cancellation. */
internal class SettingsTestOwnership {
    private var revision = 0L
    private val cancellations = mutableListOf<() -> Unit>()

    @Synchronized fun begin(): Long {
        invalidate()
        return revision
    }

    @Synchronized fun invalidate() {
        revision++
        val retiring = cancellations.toList()
        cancellations.clear()
        retiring.forEach { it() }
    }

    @Synchronized fun attach(owner: Long, cancel: () -> Unit) {
        if (owner == revision) cancellations += cancel else cancel()
    }

    @Synchronized fun isCurrent(owner: Long) = owner == revision

    @Synchronized fun finish(owner: Long) {
        if (owner == revision) cancellations.clear()
    }
}
