package tv.own.owntv.ui.components

import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** ABI describes the CPU; input behaviour follows the device capabilities instead. */
internal fun remoteTextInput(televisionMode: Boolean, televisionFeature: Boolean, touchscreen: Boolean): Boolean =
    televisionMode || televisionFeature || !touchscreen

/** Legacy television feature is retained for older boxes with incomplete feature declarations. */
@Suppress("DEPRECATION")
private fun hasTelevisionFeature(pm: PackageManager): Boolean =
    pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)

/** Device classification for services/DI, independent of the current window width. */
internal fun usesRemoteInput(context: android.content.Context): Boolean {
    val pm = context.packageManager
    val mode = (context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager)?.currentModeType
        ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK)
    return remoteTextInput(mode == Configuration.UI_MODE_TYPE_TELEVISION,
        hasTelevisionFeature(pm),
        pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN))
}

@Composable
internal fun rememberRemoteTextInput(): Boolean {
    val context = LocalContext.current
    val mode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    return remember(context, mode) {
        val pm = context.packageManager
        remoteTextInput(mode == Configuration.UI_MODE_TYPE_TELEVISION,
            hasTelevisionFeature(pm),
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN))
    }
}
