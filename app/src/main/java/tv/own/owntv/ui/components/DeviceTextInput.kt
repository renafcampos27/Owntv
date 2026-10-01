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

@Composable
internal fun rememberRemoteTextInput(): Boolean {
    val context = LocalContext.current
    val mode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    return remember(context, mode) {
        val pm = context.packageManager
        remoteTextInput(mode == Configuration.UI_MODE_TYPE_TELEVISION,
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) || pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION),
            pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN))
    }
}
