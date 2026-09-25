package tv.own.owntv.features.settings

import android.content.Context
import org.json.JSONObject
import tv.own.owntv.core.backup.BackupAppSettings

/** The Core backup transport owns encryption, file/LAN delivery and selective restore. */
class TvBackupAppSettings(private val context: Context) : BackupAppSettings {
    override suspend fun export() = JSONObject().put(SimpleModeBackupCodec.KEY, SimpleModePreferences.exportBackup(context))

    override fun validate(data: JSONObject) {
        if (data.has(SimpleModeBackupCodec.KEY)) {
            SimpleModeBackupCodec.decode(data.getJSONObject(SimpleModeBackupCodec.KEY), SimpleModeOptions())
        }
    }

    override suspend fun restore(data: JSONObject) {
        if (data.has(SimpleModeBackupCodec.KEY)) {
            SimpleModePreferences.restoreBackup(context, data.getJSONObject(SimpleModeBackupCodec.KEY))
        }
    }
}
