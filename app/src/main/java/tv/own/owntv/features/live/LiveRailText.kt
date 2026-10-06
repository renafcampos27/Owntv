package tv.own.owntv.features.live

import tv.own.owntv.core.R as CoreR

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.live.LiveKey

/** Built-in rail labels are presentation text; category names remain provider/profile data. */
@Composable
fun LiveRailItem.displayLabel(@StringRes allLabelRes: Int = CoreR.string.content_category_all_channels): String = title ?: when (key) {
    LiveKey.Favorites -> stringResource(CoreR.string.content_category_favorites)
    LiveKey.History -> stringResource(CoreR.string.content_category_history)
    LiveKey.Catchup -> stringResource(CoreR.string.content_catchup)
    LiveKey.All -> stringResource(allLabelRes)
    is LiveKey.Folder -> stringResource(allLabelRes)
    is LiveKey.Custom -> stringResource(allLabelRes)
}
