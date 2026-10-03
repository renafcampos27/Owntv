package tv.own.owntv.features.epg

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Shared reads survive an individual UI waiter; invalidation cancels the entire old generation. */
internal class GuideRowCache<K, V>(
    private val scope: CoroutineScope,
    private val maxRows: Int,
    private val maxItems: Int,
) {
    private val lock = Any()
    private val reads = Semaphore(4)
    private val rows = LinkedHashMap<K, List<V>>(16, 0.75f, true)
    private val pending = mutableMapOf<K, Deferred<List<V>>>()
    private var items = 0
    private var generation = 0L

    fun peek(key: K): List<V>? = synchronized(lock) { rows[key] }
    fun size(): Int = synchronized(lock) { rows.size }
    fun itemCount(): Int = synchronized(lock) { items }
    /** Failed reads may be displayed with eligible older data, but never become cached successes. */
    suspend fun getOrFallback(key: K, loader: suspend () -> List<V>, fallback: (Exception) -> List<V>): List<V> {
        val owner = synchronized(lock) { generation }
        return try {
            get(key, loader)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failed: Exception) {
            currentCoroutineContext().ensureActive()
            synchronized(lock) {
                if (owner != generation) throw CancellationException()
                rows[key]
            } ?: fallback(failed)
        }
    }
    fun invalidate() = synchronized(lock) {
        generation++
        rows.clear()
        items = 0
        pending.values.forEach { it.cancel() }
        pending.clear()
    }

    suspend fun get(key: K, loader: suspend () -> List<V>): List<V> {
        val owner = synchronized(lock) { generation }
        val read = synchronized(lock) {
            if (owner != generation) throw CancellationException()
            rows[key]?.let { return it }
            pending[key] ?: run {
                scope.async(start = CoroutineStart.LAZY) {
                    try {
                        val result = reads.withPermit { loader() }
                        currentCoroutineContext().ensureActive()
                        synchronized(lock) {
                            if (owner != generation) throw CancellationException()
                            // An oversized row may be displayed, but never defeats the cache budget.
                            if (result.size <= maxItems) {
                                items -= rows.put(key, result)?.size ?: 0
                                items += result.size
                                while (rows.size > maxRows || items > maxItems) {
                                    val oldest = rows.entries.iterator()
                                    items -= oldest.next().value.size
                                    oldest.remove()
                                }
                            }
                        }
                        result
                    } finally {
                        synchronized(lock) { if (owner == generation) pending.remove(key) }
                    }
                }.also { pending[key] = it }
            }
        }
        val result = read.await()
        currentCoroutineContext().ensureActive()
        synchronized(lock) { if (owner != generation) throw CancellationException() }
        return result
    }
}
