package tv.own.owntv.features.shell

import tv.own.owntv.core.nav.MainSection

internal enum class ShellBackAction { QUICK_MENU, LIVE, MORE, EXIT, FOCUS }

/** TV navigation retains explicit simple-mode and sidebar focus behaviour. */
internal fun shellBackAction(section: MainSection, hideSidebar: Boolean, sidebarFocused: Boolean): ShellBackAction = when {
    section == MainSection.SETTINGS -> if (hideSidebar) ShellBackAction.LIVE else ShellBackAction.MORE
    hideSidebar && section == MainSection.LIVE_TV -> ShellBackAction.QUICK_MENU
    sidebarFocused -> ShellBackAction.EXIT
    else -> ShellBackAction.FOCUS
}
