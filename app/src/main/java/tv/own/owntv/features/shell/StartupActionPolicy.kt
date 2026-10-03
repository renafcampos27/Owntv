package tv.own.owntv.features.shell

/** Protected streams require the internal licence path; external launches never mount a second player. */
internal fun startupUsesInternalPlayer(externalEnabled: Boolean, drmProtected: Boolean): Boolean =
    !externalEnabled || drmProtected

/** Delayed startup selection may not replace newer user input, a profile or an active player. */
internal fun startupActionIsCurrent(
    initialInputRevision: Long,
    currentInputRevision: Long,
    initialProfile: Long,
    currentProfile: Long,
    playerIdle: Boolean,
): Boolean = playerIdle && initialInputRevision == currentInputRevision && initialProfile == currentProfile

/** Only a fixed channel replaces an old session when the application returns from background. */
internal fun startupModeCanRun(
    mode: tv.own.owntv.core.settings.StartupMode,
    returningFromBackground: Boolean,
    playerIdle: Boolean,
    explicitLaunchPending: Boolean,
): Boolean = !explicitLaunchPending && if (returningFromBackground) {
    mode == tv.own.owntv.core.settings.StartupMode.SPECIFIC_CHANNEL
} else playerIdle
