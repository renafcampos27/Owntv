package tv.own.owntv.features.settings

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** Manual exports coexist. Automatic rotation and companion staging keep their established names. */
internal fun manualBackupFilename(): String =
    "owntv-backup-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT))}-${UUID.randomUUID()}.own"
