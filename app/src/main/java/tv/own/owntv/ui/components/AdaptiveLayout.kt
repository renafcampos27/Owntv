package tv.own.owntv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

internal fun compactWindow(widthDp: Float): Boolean = widthDp < 840f

/** Panel density follows window size; interaction chrome follows input capabilities as well. */
internal data class InteractionLayout(val compactWindow: Boolean, val remote: Boolean) {
    val touch: Boolean get() = !remote
    val compactPlayerControls: Boolean get() = compactWindow || touch
}

@Composable
internal fun rememberInteractionLayout(): InteractionLayout =
    InteractionLayout(rememberCompactLayout(), rememberRemoteTextInput())

@Composable
internal fun rememberCompactLayout(): Boolean = compactWindow(
    LocalWindowInfo.current.containerSize.width / LocalDensity.current.density)
