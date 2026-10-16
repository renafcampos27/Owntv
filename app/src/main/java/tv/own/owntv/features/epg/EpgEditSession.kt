package tv.own.owntv.features.epg

/** A dialog may only save into the foreground profile/source session that opened it. */
internal class EpgEditSession {
    internal data class Owner(val profile: Long, val source: Long, val epoch: Long)
    private var context: Triple<Long, Set<Long>, String?>? = null
    private var epoch = 0L

    fun update(profile: Long, sources: Collection<Long>, restoring: String?) {
        val next = Triple(profile, sources.toSet(), restoring)
        if (context != next) { context = next; epoch++ }
    }

    fun invalidate() { epoch++ }

    fun capture(profile: Long, source: Long): Owner = Owner(profile, source, epoch)

    fun accepts(owner: Owner): Boolean {
        val current = context ?: return false
        return owner.epoch == epoch && owner.profile >= 0 && owner.profile == current.first &&
            owner.source in current.second && current.third == null
    }
}
