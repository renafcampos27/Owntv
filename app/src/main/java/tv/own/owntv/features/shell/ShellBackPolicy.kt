package tv.own.owntv.features.shell

import tv.own.owntv.core.nav.MainSection

internal enum class ShellBackAction { QUICK_MENU, LIVE, MORE, EXIT, FOCUS }

/** Layout width does not imply that the user enabled the simple-mode menu. */
internal fun shellBackAction(section: MainSection, compact: Boolean, hideSidebar: Boolean, sidebarFocused: Boolean): ShellBackAction = when {
    section == MainSection.SETTINGS -> if (hideSidebar) ShellBackAction.LIVE else ShellBackAction.MORE
    hideSidebar && section == MainSection.LIVE_TV -> ShellBackAction.QUICK_MENU
    compact && section != MainSection.LIVE_TV -> ShellBackAction.LIVE
    compact || sidebarFocused -> ShellBackAction.EXIT
    else -> ShellBackAction.FOCUS
}
