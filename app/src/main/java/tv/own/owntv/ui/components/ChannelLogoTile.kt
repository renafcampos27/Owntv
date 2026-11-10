package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import tv.own.owntv.ui.images.ChannelLogoImage
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * A channel logo on its tile.
 *
 * Rendered from persistent, size-limited local thumbnails shared by the channel UI.
 * The subtle [fill] plate ensures dark logos remain clearly legible on dark TV surfaces
 * without needing CPU software-bitmap pixel measurements.
 */
@Composable
fun ChannelLogoTile(
    logoUrl: String?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier.fillMaxSize(),
    fill: Color = OwnTVTheme.colors.surfaceContainerLowest,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit,
) {
    Box(modifier = modifier.background(fill), contentAlignment = Alignment.Center) {
        ChannelLogoImage(model = logoUrl, contentDescription = null, contentScale = contentScale,
            modifier = imageModifier, fallback = fallback)
    }
}
