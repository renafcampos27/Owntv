package tv.own.owntv.ui.components

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import tv.own.owntv.core.R as CoreR

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import tv.own.owntv.R
import tv.own.owntv.ui.theme.animationsOn
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * Full-screen overlay for manually reordering items via D-pad Up/Down.
 * Up/down moves the selected item; left/right enters the additional controls.
 * Save commits the session; Back cancels it without writing the order.
 */
@Composable
fun MoveOrderOverlay(
    title: String,
    itemNames: List<String>,
    activeIndex: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    onMoveTo: ((Int) -> Unit)? = null,
    onUndo: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    saving: Boolean = false,
    saveFailed: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val panelScroll = rememberScrollState()

    val animated = animationsOn
    var position by remember(activeIndex) { mutableStateOf((activeIndex + 1).toString()) }
    var confirmRestore by remember { mutableStateOf(false) }
    var arrowMoveMode by remember { mutableStateOf(true) }
    LaunchedEffect(saving) {
        if (saving) return@LaunchedEffect
        repeat(6) {
            withFrameNanos { }
            if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    LaunchedEffect(activeIndex, animated) {
        if (itemNames.isNotEmpty()) {
            val index = activeIndex.coerceIn(0, itemNames.lastIndex)
            if (animated) listState.animateScrollToItem(index) else listState.scrollToItem(index)
        }
    }
    androidx.activity.compose.BackHandler { if (!saving) onCancel() }

    // A separate popup isolates focus and keeps numeric input above the TV keyboard.
    OwnTVPopup(onDismissRequest = { if (!saving) onCancel() }) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .trapAllFocusExit()
            .focusGroup()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (saving) return@onKeyEvent true
                when (event.key) {
                    Key.DirectionUp -> if (arrowMoveMode) { onMoveUp(); true } else false
                    Key.DirectionDown -> if (arrowMoveMode) { onMoveDown(); true } else false
                    Key.DirectionRight, Key.DirectionLeft -> { if (onMoveTo != null) arrowMoveMode = false; false }
                    Key.Back -> { onCancel(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceContainerHigh)

                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (saveFailed) Text(stringResource(R.string.order_save_failed), color = colors.onSurface,
                style = MaterialTheme.typography.bodyMedium)
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Text(
                stringResource(CoreR.string.common_move_instructions),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            if (onMoveTo != null) {
                Text(stringResource(R.string.order_controls_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                if (!arrowMoveMode) OwnTVButton(stringResource(R.string.order_arrow_mode), enabled = !saving, onClick = {
                    arrowMoveMode = true
                    runCatching { focus.requestFocus() }
                }, compact = true)
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                state = listState,
                modifier = (Modifier.weight(1f, fill = false)).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(itemNames) { idx, name ->
                    val isActive = idx == activeIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) colors.primary else colors.surfaceContainerLowest)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isActive) {
                                Text(stringResource(CoreR.string.setup_move_indicator) + " ", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimary)
                            }
                            Text(
                                name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isActive) colors.onPrimary else colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OwnTVButton(stringResource(CoreR.string.channel_versions_up), enabled = !saving, onClick = onMoveUp, modifier = Modifier.weight(1f))
                OwnTVButton(stringResource(CoreR.string.channel_versions_down), enabled = !saving, onClick = onMoveDown, modifier = Modifier.weight(1f))
            }
            onMoveTo?.let { move ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OwnTVTextField(position, { if (!saving) position = it.filter(Char::isDigit).take(7) },
                        stringResource(R.string.order_position), Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    OwnTVButton(stringResource(R.string.order_move), enabled = !saving, onClick = {
                        position.toIntOrNull()?.takeIf { it in 1..itemNames.size }?.let { move(it - 1) }
                    })
                    OwnTVButton(stringResource(R.string.order_first), enabled = !saving, onClick = { move(0) }, compact = true)
                    OwnTVButton(stringResource(R.string.order_last), enabled = !saving, onClick = { move(itemNames.lastIndex) }, compact = true)
                }
            }
            if (onUndo != null || onRestore != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onUndo?.let { OwnTVButton(stringResource(R.string.order_undo), enabled = !saving, onClick = it, compact = true) }
                    onRestore?.let { restore ->
                        OwnTVButton(stringResource(if (confirmRestore) R.string.order_confirm_restore else R.string.order_restore),
                            enabled = !saving, onClick = { if (confirmRestore) { restore(); confirmRestore = false } else confirmRestore = true }, compact = true)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // Save gets the focus: TV focus lands here on overlay open, so onKeyEvent fires.
                OwnTVButton(stringResource(if (saving) R.string.order_saving else CoreR.string.common_save), enabled = !saving, onClick = onCommit, modifier = Modifier.weight(1f).focusRequester(focus))
                OwnTVButton(stringResource(CoreR.string.common_cancel), enabled = !saving, onClick = onCancel, style = OwnTVButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
            }
        }
    }
    }
}
