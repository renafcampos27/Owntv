package tv.own.owntv.features.more

import tv.own.owntv.core.R as CoreR

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.core.live.LiveKey
import tv.own.owntv.features.live.LiveScreen
import tv.own.owntv.features.settings.SettingsViewModel
import tv.own.owntv.features.shell.components.ClearHistoryDialog
import tv.own.owntv.features.settings.data.BrowseContainerPadding
import tv.own.owntv.ui.components.ContentPanelFill
import tv.own.owntv.ui.components.OwnTVButton
import tv.own.owntv.ui.components.OwnTVButtonStyle
import tv.own.owntv.ui.components.OwnTVPopup
import tv.own.owntv.ui.components.roundedPanel
import tv.own.owntv.ui.components.trapVerticalFocusExit
import tv.own.owntv.ui.theme.OwnTVTheme

/** Live channel favourites; legacy saved movie/series data remains in storage. */
@Composable
fun FavoritesScreen(
    onFullscreen: () -> Unit,
    onChildFocused: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = UserDataScreen(
    key = LiveKey.Favorites,
    titleRes = CoreR.string.content_category_favorites,
    onFullscreen = onFullscreen,
    onChildFocused = onChildFocused,
    onBack = onBack,
    modifier = modifier,
)

/**
 * [FavoritesScreen] over history instead of favourites, newest first — and the home Clear history
 * now has. Its scope dialog and its confirm are the ones that already shipped; only the door moved,
 * onto the screen it acts on.
 */
@Composable
fun HistoryScreen(
    onFullscreen: () -> Unit,
    onChildFocused: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) = UserDataScreen(
    key = LiveKey.History,
    titleRes = CoreR.string.content_category_history,
    onFullscreen = onFullscreen,
    onChildFocused = onChildFocused,
    onBack = onBack,
    modifier = modifier,
)

/**
 * The screen both of them are. The only differences are which folder is pinned and, for history,
 * the Clear button — so it is one implementation with one branch rather than two near-copies.
 */
@Composable
private fun UserDataScreen(
    key: LiveKey,
    titleRes: Int,
    onFullscreen: () -> Unit,
    onChildFocused: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    counts: MoreCountsViewModel = koinViewModel(),
    settingsVm: SettingsViewModel = koinViewModel(),
) {
    val history = key == LiveKey.History
    var showClear by remember { mutableStateOf(false) }
    val counted by (if (history) counts.history else counts.favorites).collectAsStateWithLifecycle()

    // Arriving lands on the tab strip rather than deep in the list: the first thing a remote wants
    // here is to choose a type.
    val tabFocus = remember { FocusRequester() }
    val clearFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { tabFocus.requestFocus() } }
    // Closing the clear dialog puts focus back on the button that opened it — but only after it has
    // been open once, or arriving on the screen would land on Clear instead of the tab strip.
    var returnToClear by remember { mutableStateOf(false) }
    LaunchedEffect(showClear) {
        if (showClear) {
            returnToClear = true
        } else if (returnToClear) {
            returnToClear = false
            runCatching { clearFocus.requestFocus() }
        }
    }

    // Back leaves for More, exactly as Backup and Local sync do. Without it the shell's own handler
    // ran instead, which offers to leave the app — there was no way out of this screen at all.
    // Nested handlers still win where they should: inside a show, Back closes the show first.
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            // This screen owns the panel — the title, the tabs, Clear and the list are one box, the
            // same box every other More page is. The pinned pane below draws none of its own.
            .roundedPanel(fillColor = ContentPanelFill)
            .padding(BrowseContainerPadding)
            // And it owns the vertical trap the pinned pane gave up, so Up out of the list lands on
            // the tab strip and still cannot escape past it to the shell's top bar.
            .trapVerticalFocusExit()
            .onFocusChanged { if (it.hasFocus) onChildFocused() }
            .focusGroup(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = OwnTVTheme.colors.onSurface,
                modifier = Modifier.padding(end = 10.dp),
            )
            OwnTVButton(
                label = stringResource(CoreR.string.common_nav_live_tv) + stringResource(CoreR.string.content_epg_bits_separator) + counted.live,
                onClick = { }, style = OwnTVButtonStyle.PRIMARY, selected = true, compact = true,
                modifier = Modifier.focusRequester(tabFocus),
            )
            if (history) {
                Spacer(Modifier.weight(1f))
                OwnTVButton(
                    label = stringResource(CoreR.string.settings_clear_history),
                    onClick = { showClear = true },
                    style = OwnTVButtonStyle.SECONDARY,
                    compact = true,
                    modifier = Modifier.focusRequester(clearFocus),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LiveScreen(
            onFullscreen = onFullscreen, onChildFocused = onChildFocused,
            lockedKey = key, modifier = Modifier.fillMaxSize(),
        )
    }

    if (showClear) {
        OwnTVPopup(onDismissRequest = { showClear = false }) {
            ClearHistoryDialog(
                onClear = { type -> settingsVm.clearWatchHistory(type); showClear = false },
                onDismiss = { showClear = false },
            )
        }
    }
}
