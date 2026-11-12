package tv.own.owntv.ui.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

/** Compile-checked here; real Android bitmap/SVG decoding still requires device execution. */
class ChannelLogoEncodingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun transparentPngBecomesSmallWebpWithoutLosingAlphaOrAspectRatio() = runBlocking {
        val bitmap = Bitmap.createBitmap(1024, 512, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawRect(256f, 128f, 768f, 384f, Paint().apply { color = Color.RED })
        val raw = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val compressed = encodeChannelLogo(context, raw)
        val thumbnail = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
        assertEquals(192, thumbnail.width); assertEquals(96, thumbnail.height)
        assertEquals(0, Color.alpha(thumbnail.getPixel(0, 0)))
        assertTrue(compressed.size <= ChannelLogoDiskCache.MAX_THUMBNAIL_BYTES)
    }

    @Test fun svgLogoIsRasterizedOnceIntoABoundedThumbnail() = runBlocking {
        val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="640" height="320"><rect width="640" height="320" fill="red"/></svg>"""
        val compressed = encodeChannelLogo(context, svg.toByteArray())
        val thumbnail = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
        assertTrue(thumbnail.width in 1..192); assertTrue(thumbnail.height in 1..192)
        assertTrue(thumbnail.width > thumbnail.height)
    }

    @Test fun providerHtmlIsRejectedInsteadOfBecomingALogo() = runBlocking {
        assertTrue(runCatching { encodeChannelLogo(context, "<html>not an image</html>".toByteArray()) }.isFailure)
    }

    @Test fun svgContentTypeHandlesLongXmlPreambles() = runBlocking {
        val svg = "<?xml version=\"1.0\"?><!--" + " ".repeat(2048) + "-->" +
            """<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="600"><rect width="1800" height="600" fill="red"/></svg>"""
        val compressed = encodeChannelLogo(context, svg.toByteArray(), "image/svg+xml; charset=utf-8")
        val thumbnail = BitmapFactory.decodeByteArray(compressed, 0, compressed.size)
        assertEquals(192, thumbnail.width); assertEquals(64, thumbnail.height)
    }

    @Test fun legacyCacheFallbackCannotDownloadOrOverwriteOriginals() {
        val request = cachedChannelLogoRequest(context, "https://example.invalid/logo.png")
        assertEquals(coil3.request.CachePolicy.DISABLED, request.networkCachePolicy)
        assertEquals(coil3.request.CachePolicy.READ_ONLY, request.diskCachePolicy)
        assertEquals(coil3.request.CachePolicy.READ_ONLY, request.memoryCachePolicy)
    }
}
