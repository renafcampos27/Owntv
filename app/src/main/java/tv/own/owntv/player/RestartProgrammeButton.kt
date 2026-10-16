package tv.own.owntv.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tv.own.owntv.core.R as CoreR
import tv.own.owntv.ui.components.OwnTVIcon

/** Shares the cosmetic heartbeat only while this HUD control is composed and foregrounded. */
@Composable
internal fun RestartProgrammeButton(
    programmes: List<LiveProgramme>, contextKey: Any?, windowSec: () -> Int, onRestart: (Long) -> Unit,
) {
    var minute by remember(contextKey) { mutableLongStateOf(System.currentTimeMillis() / 60_000L) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(contextKey, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            PlayerHeartbeat.beat.map { System.currentTimeMillis() / 60_000L }
                .distinctUntilChanged().collect { minute = it }
        }
    }
    val programme = remember(programmes, minute, windowSec()) {
        restartableProgramme(programmes, System.currentTimeMillis(), windowSec())
    }
    if (programme != null) {
        CtrlButton(OwnTVIcon.REWIND, label = stringResource(CoreR.string.content_epg_watch_start)) {
            // The EPG or archive window can expire while the remote is resting on the control.
            restartableProgramme(programmes, System.currentTimeMillis(), windowSec())?.let { onRestart(it.startMs) }
        }
    }
}
