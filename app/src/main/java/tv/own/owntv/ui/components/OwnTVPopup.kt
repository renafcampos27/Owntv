package tv.own.owntv.ui.components

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import tv.own.owntv.ui.theme.PopupFontTheme

/**
 * The single host for OwnTV modal popups.
 *
 * MainActivity deliberately keeps Android TV's broadly-compatible `adjustPan`. Each modal owns a
 * focus-isolated platform window configured as `adjustNothing`; OwnTV measures the unobstructed
 * display band and lays the popup out inside it. This also works with TV keyboards which publish no
 * useful IME inset and ignore `adjustResize`.
 *
 * Popup chrome and the user-selected popup typography are reduced together here. Keeping the scale in the host means
 * nested popups (Rule builder -> Rule value) cannot silently return to full application size.
 */
@Composable
fun OwnTVPopup(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    fontScale: Float? = null,
    automaticPrompt: Boolean = false,
    content: @Composable () -> Unit,
) {
    val remote = rememberRemoteTextInput()
    val owner = remember { Any() }
    DisposableEffect(owner, automaticPrompt) {
        if (!automaticPrompt) popupPresence.enter(owner)
        onDispose { if (!automaticPrompt) popupPresence.leave(owner) }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogView = LocalView.current
        var parent = dialogView.parent
        var provider: DialogWindowProvider? = null
        while (parent != null && provider == null) {
            provider = parent as? DialogWindowProvider
            parent = parent.parent
        }
        val dialogWindow = provider?.window
        DisposableEffect(dialogWindow) {
            val previousMode = dialogWindow?.attributes?.softInputMode
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            onDispose {
                if (previousMode != null) dialogWindow.setSoftInputMode(previousMode)
            }
        }
        SideEffect {
            // Compose/OEM code can update dialog attributes after initial attachment.
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }

        val watcher = remember(dialogView, remote) { TvImeWatcher(dialogView, allowEstimate = remote) }
        DisposableEffect(watcher) {
            watcher.attach()
            onDispose { watcher.detach() }
        }
        LaunchedEffect(watcher.imeRequested) {
            if (!watcher.imeRequested) return@LaunchedEffect
            val count = (TvImeDefaults.POLL_DURATION_MS / TvImeDefaults.POLL_INTERVAL_MS).toInt()
            repeat(count) {
                kotlinx.coroutines.delay(TvImeDefaults.POLL_INTERVAL_MS)
                watcher.poll()
            }
        }

        val baseDensity = LocalDensity.current
        val metrics = watcher.metrics
        val displayHeightPx = metrics.displayHeightPx.takeIf { it > 0 }
            ?: dialogView.resources.displayMetrics.heightPixels
        val systemSafe = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        val marginPx = with(baseDensity) { 24.dp.roundToPx() }
        val topSafePx = if (remote) marginPx else maxOf(marginPx, systemSafe.getTop(baseDensity))
        val bottomSafePx = if (remote) marginPx else maxOf(marginPx, systemSafe.getBottom(baseDensity))
        val keyboardGapPx = with(baseDensity) { 16.dp.roundToPx() }
        val usableBottomPx = if (metrics.visible) {
            (metrics.keyboardTopPx - keyboardGapPx).coerceAtLeast(topSafePx)
        } else {
            (displayHeightPx - bottomSafePx).coerceAtLeast(topSafePx)
        }
        val availableHeightPx = (usableBottomPx - topSafePx).coerceAtLeast(1)
        val availableHeightDp = with(baseDensity) { availableHeightPx.toDp() }

        val popupScale = popupBaseScale(remote)
        // The host owns the fixed TV-safe base scale. PopupFontTheme applies the user's independent
        // popup geometry and font controls for both hosted and legacy inline popup content.
        val popupDensity = Density(
            density = baseDensity.density * popupScale,
            fontScale = baseDensity.fontScale / popupScale,
        )

        // The host already excludes the measured keyboard band. Mark its inset consumed so inline
        // focus traps do not subtract the same keyboard again; their IME padding remains useful
        // when used outside this host.
        Box(Modifier.fillMaxSize().consumeWindowInsets(WindowInsets.ime)
            .then(if (remote) Modifier else Modifier.windowInsetsPadding(systemSafe.only(WindowInsetsSides.Horizontal)))) {
            // Centre inside the unobstructed physical band. ADJUST_NOTHING ensures this is the only
            // movement, eliminating double pan/translation across different TV implementations.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, topSafePx) }
                    .height(availableHeightDp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalDensity provides popupDensity,
                    LocalTvImeWatcher provides watcher,
                    LocalTvImeMetrics provides metrics,
                ) {
                    PopupFontTheme(fontScale = fontScale ?: popupScale) {
                        BoxWithConstraints(
                            Modifier.fillMaxSize().then(if (remote) Modifier else Modifier.padding(horizontal = 16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CompositionLocalProvider(LocalPopupMaxWidth provides if (remote) null else maxWidth) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}
