package tv.own.owntv.ui.components

import tv.own.owntv.core.R as CoreR

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.companion.CompanionFailure

@Composable
fun CompanionFailure.displayText(): String = when (this) {
    CompanionFailure.InvalidPort -> stringResource(CoreR.string.setup_companion_invalid_port)
    is CompanionFailure.PortInUse -> stringResource(CoreR.string.setup_companion_port_in_use, port)
    CompanionFailure.Unavailable -> stringResource(CoreR.string.setup_companion_unavailable)
}

@Composable
fun companionLockedText(): String = stringResource(CoreR.string.setup_companion_locked)
