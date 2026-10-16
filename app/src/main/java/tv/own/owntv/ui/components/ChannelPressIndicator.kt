package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.theme.OwnTVTheme

@Composable
internal fun ChannelPressFeedbackBar(
    stage: ChannelPressStage,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    if (stage == ChannelPressStage.IDLE) return
    val colors = OwnTVTheme.colors
    val clampedProgress = progress.coerceIn(0f, 1f)

    val fillColor = when (stage) {
        ChannelPressStage.MENU_THRESHOLD -> colors.primary
        ChannelPressStage.VERSIONS_THRESHOLD -> colors.primary
        ChannelPressStage.HOLDING_TO_VERSIONS -> colors.primary.copy(alpha = 0.75f)
        ChannelPressStage.IDLE -> colors.primary
    }

    val labelText = when (stage) {
        ChannelPressStage.MENU_THRESHOLD -> stringResource(R.string.channel_press_hint_menu)
        ChannelPressStage.VERSIONS_THRESHOLD -> stringResource(R.string.channel_press_hint_versions)
        ChannelPressStage.HOLDING_TO_VERSIONS, ChannelPressStage.IDLE -> stringResource(R.string.channel_press_hint_hold)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surfaceContainerLowest.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                color = if (stage == ChannelPressStage.VERSIONS_THRESHOLD || stage == ChannelPressStage.MENU_THRESHOLD) {
                    colors.primary
                } else {
                    colors.onSurfaceVariant
                },
                fontWeight = if (stage != ChannelPressStage.HOLDING_TO_VERSIONS) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.onSurface.copy(alpha = 0.15f)),
        ) {
            if (clampedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(fillColor),
                )
            }
            // 2-second milestone notch at 2000 / 3000 = 66.7%
            Box(
                modifier = Modifier
                    .fillMaxWidth(2f / 3f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(colors.onSurface.copy(alpha = 0.5f)),
                )
            }
        }
    }
}
