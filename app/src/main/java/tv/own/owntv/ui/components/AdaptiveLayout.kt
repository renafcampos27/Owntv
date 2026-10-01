package tv.own.owntv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

internal fun compactWindow(widthDp: Float): Boolean = widthDp < 840f

@Composable
internal fun rememberCompactLayout(): Boolean = compactWindow(
    LocalWindowInfo.current.containerSize.width / LocalDensity.current.density)
