package tv.own.owntv.diagnostics

/** Fixed counters for work, not key-to-screen latency or provider transfer speed. */
internal class UiWorkWindow {
    enum class Operation { EPG_LAYOUT, EPG_ROW_READ, EPG_PREFETCH_READ, LIVE_VISIBLE_ROWS }
    enum class Outcome { COMPLETED, CANCELLED, FAILED }
    data class Snapshot(val operation: Operation, val calls: Long, val completed: Long,
        val cancelled: Long, val failed: Long, val totalNs: Long, val maxNs: Long, val p95BucketMs: Int?)
    private class Counters {
        val buckets = LongArray(1001)
        var calls = 0L; var cancelled = 0L; var failed = 0L
        var totalNs = 0L; var maxNs = 0L
    }
    private var serial = 0L
    @Volatile var epoch = 0L
        private set
    private val counters = Array(Operation.entries.size) { Counters() }

    @Synchronized fun start(): Long {
        clear()
        epoch = ++serial
        return epoch
    }
    @Synchronized fun stop(owner: Long) {
        if (epoch != owner) return
        epoch = 0L
        clear()
    }
    @Synchronized fun record(owner: Long, operation: Operation, durationNs: Long, outcome: Outcome) {
        if (owner == 0L || owner != epoch) return
        val c = counters[operation.ordinal]
        val ns = durationNs.coerceAtLeast(0)
        c.calls++
        c.totalNs += ns
        c.maxNs = maxOf(c.maxNs, ns)
        if (outcome == Outcome.CANCELLED) c.cancelled++
        if (outcome == Outcome.FAILED) c.failed++
        c.buckets[(ns / 1_000_000 + if (ns % 1_000_000 == 0L) 0 else 1).coerceAtMost(1000).toInt()]++
    }
    @Synchronized fun take(owner: Long = epoch): List<Snapshot> {
        if (owner == 0L || owner != epoch) return emptyList()
        val result = Operation.entries.mapNotNull { op ->
            val c = counters[op.ordinal]
            if (c.calls == 0L) null else {
                val rank = (c.calls * 95 + 99) / 100
                var seen = 0L
                val p95 = c.buckets.indexOfFirst { seen += it; seen >= rank }
                Snapshot(op, c.calls, c.calls - c.cancelled - c.failed, c.cancelled, c.failed,
                    c.totalNs, c.maxNs, p95.takeIf { it >= 0 })
            }
        }
        clear()
        return result
    }
    private fun clear() { for (c in counters) {
        c.calls = 0; c.cancelled = 0; c.failed = 0; c.totalNs = 0; c.maxNs = 0
        c.buckets.fill(0)
    } }
}

/** No clock reads or recording when disabled; stale jobs cannot enter a new diagnostic session. */
internal object UiWorkDiagnostics {
    @PublishedApi internal val window = UiWorkWindow()
    inline fun <T> measure(operation: UiWorkWindow.Operation, block: () -> T): T {
        val owner = window.epoch
        if (owner == 0L) return block()
        val start = System.nanoTime()
        var outcome = UiWorkWindow.Outcome.COMPLETED
        try { return block() }
        catch (cancelled: kotlinx.coroutines.CancellationException) {
            outcome = UiWorkWindow.Outcome.CANCELLED
            throw cancelled
        } catch (failed: Throwable) {
            outcome = UiWorkWindow.Outcome.FAILED
            throw failed
        } finally { window.record(owner, operation, System.nanoTime() - start, outcome) }
    }
}
