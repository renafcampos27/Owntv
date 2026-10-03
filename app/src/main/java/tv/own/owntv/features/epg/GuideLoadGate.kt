package tv.own.owntv.features.epg

/** Main-thread policy: coalesce any number of hidden-guide invalidations into one active load. */
internal class GuideLoadGate {
    private var active = false
    private var dirty = true

    fun setActive(next: Boolean): Boolean {
        if (active == next) return false
        active = next
        if (!next) dirty = true // reopen also refreshes the wall-clock guide window
        return next && dirty
    }

    fun invalidate(): Boolean {
        dirty = true
        return active
    }

    fun beginLoad(): Boolean {
        if (!active || !dirty) return false
        dirty = false
        return true
    }
}
