package tv.own.owntv.features.settings

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.context.GlobalContext
import tv.own.owntv.core.backup.BackupManager
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.settings.SettingsRepository
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

internal object AutomaticBackupScheduler {
    private const val PERIODIC = "owntv-automatic-backup"
    private const val NOW = "owntv-automatic-backup-now"
    suspend fun update(context: Context) {
        val state = AutomaticBackupPreferences.read(context)
        val manager = WorkManager.getInstance(context)
        if (state.months == 0 || !state.configured) {
            manager.cancelUniqueWork(PERIODIC)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS).build()
        manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }
    fun immediateWork(context: Context) = WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(NOW)

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AutomaticBackupWorker>()
            .setInputData(workDataOf("force" to true))
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build()).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request)
    }
}

/** Public two-argument constructor allows WorkManager's normal factory fallback after process death. */
class AutomaticBackupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    companion object { private val operation = Mutex() }
    override suspend fun doWork(): Result = operation.withLock {
        var staging: File? = null
        var published = false
        var previousBackup: SettingsRepository.LastBackup? = null
        val settings = GlobalContext.get().get<SettingsRepository>()
        try {
            val state = AutomaticBackupPreferences.read(applicationContext)
            val forced = inputData.getBoolean("force", false)
            if (!forced && !AutomaticBackupPolicy.due(state.months, state.anchor, System.currentTimeMillis())) return@withLock Result.success()
            val config = AutomaticBackupPreferences.config(applicationContext) ?: return@withLock Result.success()
            val profileDao = GlobalContext.get().get<ProfileDao>()
            val profiles = profileDao.getAllOnce().associateBy { it.id }
            check(config.profiles.isNotEmpty() && config.profiles.all { (id, auth) ->
                profiles[id]?.let { AutomaticBackupPreferences.authorization(it) == auth } == true
            })
            val folder = tv.own.owntv.core.storage.StorageAccess.resolveRoot(applicationContext, config.folder)
            val stage = File(folder, ".owntv-auto-" + UUID.randomUUID()).also { check(it.mkdir()) }
            staging = stage
            previousBackup = settings.lastBackup.first()
            val exported = GlobalContext.get().get<BackupManager>().export(stage,
                BackupManager.Section.entries.toSet(), config.password, config.profiles.keys).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (!AutomaticBackupPreferences.isCurrent(applicationContext, config.revision)) return@withLock Result.success()
            val currentProfiles = profileDao.getAllOnce().associateBy { it.id }
            check(config.profiles.all { (id, auth) ->
                currentProfiles[id]?.let { AutomaticBackupPreferences.authorization(it) == auth } == true
            })
            val now = System.currentTimeMillis()
            val destination = File(folder, "owntv-auto-" + now + "-" + UUID.randomUUID() + ".own")
            check(File(exported).renameTo(destination))
            published = true
            AutomaticBackupPreferences.success(applicationContext, config.revision, now)
            settings.recordBackup(now, destination.length(), true, destination.absolutePath)
            // Rotate only this feature's completed files, and only after the new backup is safe.
            AutomaticBackupFiles.rotate(folder)
            Result.success()
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) {
            AutomaticBackupPreferences.failure(applicationContext)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            staging?.let { stage ->
                withContext(NonCancellable) {
                    // Core records its export location; undo that transient location if publishing failed.
                    if (!published && settings.lastBackup.first()?.path?.startsWith(stage.absolutePath + File.separator) == true) {
                        val previous = previousBackup
                        settings.recordBackup(previous?.at ?: 0L, previous?.bytes ?: 0L,
                            previous?.encrypted ?: false, previous?.path.orEmpty())
                    }
                    stage.deleteRecursively()
                }
            }
        }
    }
}
