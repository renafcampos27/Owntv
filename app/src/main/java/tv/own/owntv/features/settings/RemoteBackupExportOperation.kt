package tv.own.owntv.features.settings

import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun remoteBackupExportFolder(cacheRoot: File): File = File(cacheRoot, UUID.randomUUID().toString())

/** Owns the export job and its download server, including exporters that ignore cancellation. */
internal class RemoteBackupExportOperation(
    private val scope: CoroutineScope,
    private val startServer: (File) -> Unit,
    private val stopServer: () -> Unit,
    private val working: () -> Unit,
    private val idle: () -> Unit,
    private val failed: () -> Unit,
) {
    private var generation = 0L
    private var job: Job? = null
    private var hasOperation = false
    private var ownsServer = false

    @Synchronized
    fun start(export: suspend () -> Result<File>) {
        close()
        val owner = generation
        hasOperation = true
        working()
        job = scope.launch(start = CoroutineStart.LAZY) {
            val result = try {
                export()
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Exception) {
                Result.failure(error)
            }
            synchronized(this@RemoteBackupExportOperation) {
                // Cancellation alone cannot retire blocking work. Check both ownership and activity
                // before *any* server/state side effect; stale completions never stop a newer server.
                if (owner != generation || !isActive) return@synchronized
                job = null
                result.fold(
                    onSuccess = { file ->
                        try {
                            ownsServer = true
                            startServer(file)
                            idle()
                        } catch (_: Exception) {
                            ownsServer = false
                            stopServer()
                            failed()
                        }
                    },
                    onFailure = { failed() },
                )
            }
            // A cancelled export's encrypted file stays in its own internal cache directory until
            // Android/app cache cleanup. Stale completion neither serves it nor removes other files.
        }
        job?.start()
    }

    @Synchronized
    fun close() {
        generation++
        val retiring = job
        job = null
        retiring?.cancel()
        if (ownsServer) {
            ownsServer = false
            stopServer()
        }
        if (hasOperation) {
            hasOperation = false
            idle()
        }
    }
}
