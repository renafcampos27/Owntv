package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tv.own.owntv.ui.theme.OwnTVTheme

/** Shared separator for channel and category context menus. */
@Composable
fun ContextMenuDivider() {
    Box(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp)
            .background(OwnTVTheme.colors.outlineVariant.copy(alpha = 0.45f)),
    )
}
