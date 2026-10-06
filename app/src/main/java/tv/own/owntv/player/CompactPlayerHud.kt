package tv.own.owntv.player

import tv.own.owntv.core.R as CoreR

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
internal fun CompactPlayerHeader(player: PlaybackEngine, onBack: () -> Unit, onMore: () -> Unit, modifier: Modifier) {
    val meta by player.currentMeta.collectAsStateWithLifecycle()
    Row(modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(OwnTVIcon.BACK, 48, onClick = onBack)
        Text(meta.title.orEmpty(), Modifier.weight(1f).padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleMedium, color = OwnTVTheme.colors.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        OwnTVButton(stringResource(CoreR.string.common_nav_more), onClick = onMore)
    }
}

@Composable
internal fun CompactPlayerTimeline(player: PlaybackEngine, isLive: Boolean, position: () -> Long, duration: Long,
    offset: () -> Int?, programmes: List<LiveProgramme>, onScrub: ((Int) -> Unit)?,
    key: Any?, window: (() -> Int)?, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when {
            onScrub != null -> LiveTimelineBar(offset() ?: 0, programmes, System.currentTimeMillis(), onScrub,
                key, window?.invoke()?.takeIf { it > 0 } ?: LIVE_WINDOW_SEC)
            !isLive && duration > 0 -> {
                val step by player.seekStepMs.collectAsStateWithLifecycle()
                val buffered by player.bufferedMs.collectAsStateWithLifecycle()
                val meta by player.currentMeta.collectAsStateWithLifecycle()
                val pos = position()
                SeekBar(pos, duration, buffered, step, { player.seekBy(it) }, contextKey = meta)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(pos), color = OwnTVTheme.colors.onSurface)
                    Text(formatTime(duration), color = OwnTVTheme.colors.onSurface)
                }
            }
            isLive -> NativeLiveTimeline(player)
        }
    }
}
