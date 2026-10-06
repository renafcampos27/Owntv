package tv.own.owntv.player

import tv.own.owntv.core.R as CoreR

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import tv.own.owntv.ui.format.formatSystemTime

/** Formats semantic metadata at the current Compose/resource boundary. */
@Composable
internal fun MediaMeta.localizedSubtitle(): String? {
    val rewind = rewindStartMs
    return if (rewind != null) {
        stringResource(CoreR.string.content_rewind_at, formatSystemTime(LocalContext.current, rewind))
    } else {
        subtitle
    }
}
