package tv.own.owntv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * A channel logo on its tile.
 *
 * Rendered using native GPU hardware-accelerated image decoding and caching.
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
    var failed by remember(logoUrl) { mutableStateOf(false) }
    val showLogo = !logoUrl.isNullOrBlank() && !failed
    val context = LocalContext.current
    val imageRequest = remember(logoUrl, context) {
        if (logoUrl.isNullOrBlank()) null
        else ImageRequest.Builder(context)
            .data(logoUrl)
            .size(128, 128)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier.background(fill),
        contentAlignment = Alignment.Center,
    ) {
        if (showLogo && imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = contentScale,
                modifier = imageModifier,
                onError = { failed = true },
            )
        } else {
            fallback()
        }
    }
}
