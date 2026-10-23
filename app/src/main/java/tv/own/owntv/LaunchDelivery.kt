package tv.own.owntv

/** A recreation restores consumption; a deliberate new Intent is always a new delivery. */
internal class LaunchDelivery(consumed: Boolean = false) {
    var consumed: Boolean = consumed
        private set
    var revision: Long = 0L
        private set
    fun receive(): Long { consumed = false; return ++revision }
    fun consume(expectedRevision: Long): Boolean {
        if (expectedRevision != revision || consumed) return false
        consumed = true
        return true
    }
}
