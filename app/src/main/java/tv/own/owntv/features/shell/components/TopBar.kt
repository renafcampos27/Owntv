package tv.own.owntv.features.shell.components

import tv.own.owntv.core.R as CoreR

import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import tv.own.owntv.ui.components.FocusableSurface
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.core.theme.GlassSurface
import tv.own.owntv.ui.theme.Dimens
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.glass
import tv.own.owntv.ui.theme.ownTvTween
import java.util.Date

// Top-bar chips: corner matches the nav buttons (14dp, not full-pill) and a lighter frost than the
// big panels so the small chrome reads as glass without being heavy.
private val TopBarChipCorner = 14.dp
private const val TopBarFrost = 0.45f

@Composable
fun TopBar(
    playlistName: String,
    playlistInteractive: Boolean = false,
    onPlaylistClick: () -> Unit = {},
    playlistDownFocusRequester: FocusRequester? = null,
    // Audio Mode (plan §8): the now-playing bar, shown while PlayerMode.AUDIO
    // is active. Null = not in Audio Mode.
    audioBar: (@Composable () -> Unit)? = null,
    /** The audio capsule has taken focus and grown; the strip grows with it so content is pushed, not covered. */
    audioBarExpanded: Boolean = false,
    // The shell reserves this width for its navigation rail. Extending into that reservation lets
    // the top bar own the complete top strip while the rail itself begins below it.
    leadingExtension: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    // Audio Mode can expand into a two-line now-playing card and 36 dp transport control. Preserve the
    // existing 48 dp strip for it; ordinary pills need only 40 dp, reclaiming 4 dp above and below.
    val hasAudioBar = audioBar != null
    val barHeight by animateDpAsState(
        when {
            hasAudioBar && audioBarExpanded -> Dimens.TopBarAudioExpandedHeight
            hasAudioBar -> Dimens.TopBarHeight
            else -> Dimens.TopBarCompactHeight
        },
        // ownTvTween, so Reduce animations snaps between the two heights instead of sliding.
        animationSpec = tv.own.owntv.ui.theme.ownTvTween(180),
        label = "topBarHeight",
    )
    val verticalInset = if (hasAudioBar) 4.dp else 2.dp
    Row(
        modifier = modifier
            .layout { measurable, constraints ->
                val extensionPx = leadingExtension.roundToPx()
                val expandedWidth = (constraints.maxWidth + extensionPx).coerceAtLeast(constraints.minWidth)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = expandedWidth, maxWidth = expandedWidth),
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(-extensionPx, 0)
                }
            }
            .fillMaxWidth()
            .height(barHeight)
            // The complete top strip follows the mockup's small screen-edge inset.
            .padding(start = 10.dp, end = 20.dp, top = verticalInset, bottom = verticalInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            audioBar?.invoke()
            ClockChip()
            if (playlistName.isNotBlank()) {
                PlaylistChip(
                    label = playlistName,
                    interactive = playlistInteractive,
                    onClick = onPlaylistClick,
                    downFocusRequester = playlistDownFocusRequester,
                )
            }
        }
    }
}

@Composable
private fun ClockChip() {
    val colors = OwnTVTheme.colors
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); now = System.currentTimeMillis() } }
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val formatted = remember(now, timeFormat) { timeFormat.format(Date(now)) }
    // Display-only (non-focusable) and neutral (no accent), matching the other chips. Frosts in
    // glass mode (TOPBAR surface) so it reads as glass like the focusable chips.
    val shape = RoundedCornerShape(TopBarChipCorner)
    Box(Modifier.clip(shape).glass(GlassSurface.TOPBAR, colors.surfaceContainer.copy(alpha = 0.6f), shape, frostScale = TopBarFrost, condenseChrome = true).padding(horizontal = 14.dp, vertical = 7.dp)) {
        Text(formatted, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun PlaylistChip(
    label: String,
    interactive: Boolean = false,
    onClick: () -> Unit = {},
    downFocusRequester: FocusRequester? = null,
) {
    val colors = OwnTVTheme.colors
    // Static badge when there's only one playlist (nothing to switch); a focusable button with a chevron
    // when there are 2+, opening the playlist quick-switcher.
    if (!interactive) {
        // Neutral display-only badge (no always-on accent), frosts in glass mode like the other chips.
        val shape = RoundedCornerShape(TopBarChipCorner)
        Box(Modifier.clip(shape).glass(GlassSurface.TOPBAR, colors.surfaceContainer.copy(alpha = 0.6f), shape, frostScale = TopBarFrost, condenseChrome = true).padding(horizontal = 14.dp, vertical = 7.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        return
    }
    FocusableSurface(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 240.dp)
            .then(
                if (downFocusRequester != null) {
                    Modifier.focusProperties { down = downFocusRequester }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(TopBarChipCorner),
        surface = GlassSurface.TOPBAR,
        glassFrostScale = TopBarFrost,
        glassIdleRimAlpha = 0.18f,
        glassCondensesWithContent = true,
        // Neutral when idle; accent fill only when focused (no always-on accent).
        focusedContainerColor = colors.primaryContainer,
        unfocusedContainerColor = colors.surfaceContainer.copy(alpha = 0.6f),
        contentAlignment = Alignment.Center,
    ) { focused ->
        val fg = if (focused) colors.onPrimaryContainer else colors.onSurfaceVariant
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (focused) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
                ),
            )
            OwnTVIcon(icon = OwnTVIcon.CHEVRON, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}
