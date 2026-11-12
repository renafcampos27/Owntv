package tv.own.owntv.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.*
import coil3.size.Precision
import coil3.size.Scale
import coil3.toBitmap
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.koin.compose.koinInject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

internal const val CHANNEL_LOGO_SIZE = 192

internal fun createChannelLogoRepository(context: Context, base: OkHttpClient): ChannelLogoRepository {
    val client = base.newBuilder().dispatcher(Dispatcher().apply { maxRequests = 2; maxRequestsPerHost = 2 })
        .connectionPool(ConnectionPool(2, 30, TimeUnit.SECONDS))
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1)).retryOnConnectionFailure(true)
        .callTimeout(10, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    return ChannelLogoRepository(ChannelLogoDiskCache(context.noBackupFilesDir.resolve("channel-logos")), client,
        encode = { file, mimeType -> encodeChannelLogo(context, file, mimeType) })
}

internal suspend fun encodeChannelLogo(context: Context, data: Any, mimeType: String? = null): ByteArray {
    val request = ImageRequest.Builder(context).data(data).size(CHANNEL_LOGO_SIZE, CHANNEL_LOGO_SIZE)
        .scale(Scale.FIT).precision(Precision.EXACT).allowHardware(false).allowRgb565(false)
        .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED)
        .networkCachePolicy(CachePolicy.DISABLED).apply {
            // Content-Type can identify SVGs beyond Coil's short signature-sniffing window.
            if (mimeType?.substringBefore(';')?.trim().equals("image/svg+xml", true)) {
                decoderFactory { result, options, _ -> coil3.svg.SvgDecoder(result.source, options) }
            }
        }.build()
    val result = SingletonImageLoader.get(context).execute(request)
    check(result is SuccessResult)
    val image = result.image
    val original = image.toBitmap()
    val (width, height) = logoThumbnailSize(original.width, original.height, CHANNEL_LOGO_SIZE)
    val bitmap = if (width == original.width && height == original.height) original
        else Bitmap.createScaledBitmap(original, width, height, true)
    val output = ByteArrayOutputStream()
    @Suppress("DEPRECATION")
    val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    if (!bitmap.compress(format, 90, output)) {
        output.reset()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
    }
    check(output.size() <= ChannelLogoDiskCache.MAX_THUMBNAIL_BYTES)
    return output.toByteArray()
}

/** The repository owns network requests; rows can also reuse an older Coil cache entry offline. */
@Composable
internal fun ChannelLogoImage(
    model: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    fallback: @Composable () -> Unit = {},
) {
    val url = model?.trim()
    if (url.isNullOrBlank()) { fallback(); return }
    if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
        val request = ImageRequest.Builder(LocalContext.current).data(url).size(CHANNEL_LOGO_SIZE, CHANNEL_LOGO_SIZE).build()
        AsyncImage(model = request, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale, alpha = alpha)
        return
    }
    key(url) { PersistentChannelLogo(url, contentDescription, modifier, contentScale, alpha, fallback) }
}

/** Never writes originals back into Coil's disk cache or creates a second network download. */
internal fun cachedChannelLogoRequest(context: Context, url: String): ImageRequest =
    ImageRequest.Builder(context).data(url).size(CHANNEL_LOGO_SIZE, CHANNEL_LOGO_SIZE)
        .scale(Scale.FIT).precision(Precision.INEXACT).allowRgb565(false).crossfade(false)
        .memoryCachePolicy(CachePolicy.READ_ONLY).diskCachePolicy(CachePolicy.READ_ONLY)
        .networkCachePolicy(CachePolicy.DISABLED).build()

@Composable
private fun PersistentChannelLogo(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alpha: Float,
    fallback: @Composable () -> Unit,
) {
    val repository = koinInject<ChannelLogoRepository>()
    val flow = remember(url, repository) { repository.observe(url) }
    val initial = remember(url, repository) { repository.cached(url) }
    val asset by flow.collectAsState(initial = initial)
    val context = LocalContext.current
    var failed by remember(asset) { mutableStateOf(false) }
    val request = remember(asset, context) {
        asset?.let {
            ImageRequest.Builder(context).data(it.file).size(CHANNEL_LOGO_SIZE, CHANNEL_LOGO_SIZE)
                .scale(Scale.FIT).precision(Precision.INEXACT).allowRgb565(false).crossfade(false).build()
        }
    }
    if (request != null && !failed) {
        AsyncImage(model = request, contentDescription = contentDescription, modifier = modifier,
            contentScale = contentScale, alpha = alpha,
            onError = { failed = true; asset?.let { repository.repair(url, it) } })
    } else {
        val cachedRequest = remember(context, url) { cachedChannelLogoRequest(context, url) }
        var cacheFailed by remember(url) { mutableStateOf(false) }
        var cacheReady by remember(url) { mutableStateOf(false) }
        if (cacheFailed) fallback()
        else Box(modifier) {
            if (!cacheReady) fallback()
            AsyncImage(model = cachedRequest, contentDescription = contentDescription, modifier = Modifier.matchParentSize(),
                contentScale = contentScale, alpha = alpha,
                onSuccess = { cacheReady = true }, onError = { cacheFailed = true })
        }
    }
}
