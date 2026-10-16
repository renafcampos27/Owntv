package tv.own.owntv.ui.components

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.R as CoreR

/** Shared duration format for the player's elapsed/remaining readout and audio bar. */
@Composable
fun formatTimestamp(ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) stringResource(CoreR.string.common_timestamp_hours, h, m, s)
    else stringResource(CoreR.string.common_timestamp_minutes, m, s)
}

/** The same format at a button press outside composition. */
fun formatTimestamp(res: Resources, ms: Long): String {
    val totalSec = ms.coerceAtLeast(0L) / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) res.getString(CoreR.string.common_timestamp_hours, h, m, s)
    else res.getString(CoreR.string.common_timestamp_minutes, m, s)
}
