package tv.own.owntv.ui.theme

import tv.own.owntv.core.R as CoreR

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import tv.own.owntv.core.theme.AccentColor
import tv.own.owntv.core.theme.roles

/**
 * The display label each [AccentColor] carries, and its tonal palette as Compose colours.
 *
 * The tonal values live in core (`AccentColor.roles(isDark)`) so the mobile app seeds its scheme
 * from the same table; only the label — a resource, and therefore per-app — is here. Neutrals
 * (background, surface containers, text, outline) are theme-only and live in [OwnTVColors].
 */
val AccentColor.labelRes: Int
    @StringRes get() = when (this) {
        AccentColor.TEAL -> CoreR.string.settings_accent_teal
        AccentColor.BLUE -> CoreR.string.settings_accent_blue
        AccentColor.VIOLET -> CoreR.string.settings_accent_violet
        AccentColor.GREEN -> CoreR.string.settings_accent_green
        AccentColor.AMBER -> CoreR.string.settings_accent_amber
    }

fun AccentColor.primary(isDark: Boolean) = Color(roles(isDark).primary)
fun AccentColor.onPrimary(isDark: Boolean) = Color(roles(isDark).onPrimary)
fun AccentColor.primaryContainer(isDark: Boolean) = Color(roles(isDark).primaryContainer)
fun AccentColor.onPrimaryContainer(isDark: Boolean) = Color(roles(isDark).onPrimaryContainer)
