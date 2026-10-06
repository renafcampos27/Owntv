package tv.own.owntv.player

import tv.own.owntv.core.R as CoreR

import androidx.annotation.StringRes
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import tv.own.owntv.core.player.MiniPlayerPosition

/** Where on screen each docking spot puts the mini-player window. */
val MiniPlayerPosition.alignment: Alignment
    get() = when (this) {
        MiniPlayerPosition.TOP_LEFT -> AbsoluteAlignment.TopLeft
        MiniPlayerPosition.TOP_CENTER -> Alignment.TopCenter
        MiniPlayerPosition.TOP_RIGHT -> AbsoluteAlignment.TopRight
        MiniPlayerPosition.BOTTOM_LEFT -> AbsoluteAlignment.BottomLeft
        MiniPlayerPosition.BOTTOM_CENTER -> Alignment.BottomCenter
        MiniPlayerPosition.BOTTOM_RIGHT -> AbsoluteAlignment.BottomRight
    }

/** The user-facing label for each docking spot. */
val MiniPlayerPosition.labelRes: Int
    @StringRes get() = when (this) {
        MiniPlayerPosition.TOP_LEFT -> CoreR.string.player_mini_top_left
        MiniPlayerPosition.TOP_CENTER -> CoreR.string.player_mini_top_center
        MiniPlayerPosition.TOP_RIGHT -> CoreR.string.player_mini_top_right
        MiniPlayerPosition.BOTTOM_LEFT -> CoreR.string.player_mini_bottom_left
        MiniPlayerPosition.BOTTOM_CENTER -> CoreR.string.player_mini_bottom_center
        MiniPlayerPosition.BOTTOM_RIGHT -> CoreR.string.player_mini_bottom_right
    }
