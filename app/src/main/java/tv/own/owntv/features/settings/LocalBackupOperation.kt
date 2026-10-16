package tv.own.owntv.features.settings

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Local writes run to completion when a dialog closes; only the current operation may publish. */
internal class LocalBackupOperation<T>(
    private val scope: CoroutineScope,
    private val working: () -> Unit,
    private val idle: () -> Unit,
    private val publish: (T) -> Unit,
) {
    private var generation = 0L
    private var job: Job? = null
    private var running = false

    @get:Synchronized val active: Boolean get() = running

    @Synchronized fun start(onFailure: (Exception) -> T, action: suspend () -> T): Boolean {
        if (running) return false
        val owner = ++generation
        running = true
        working()
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val result = try { action() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { onFailure(error) }
                synchronized(this@LocalBackupOperation) {
                    if (owner == generation && isActive) publish(result)
                }
            } finally {
                synchronized(this@LocalBackupOperation) {
                    if (owner == generation) { running = false; job = null }
                }
            }
        }
        job?.start()
        return true
    }

    /** Reset cannot cancel a partially written import/export or admit a competing writer. */
    @Synchronized fun reset(): Boolean {
        if (running) { working(); return false }
        generation++
        idle()
        return true
    }

    /** Host disposal revokes publication; Core's interrupted-restore marker handles process shutdown. */
    @Synchronized fun close() {
        generation++
        job?.cancel()
        job = null
        running = false
    }
}
