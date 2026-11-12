package tv.own.owntv.ui.images

import org.junit.Assert.*
import org.junit.Test

class LogoThumbnailSizeTest {
    @Test fun largeLandscapeKeepsItsAspectRatio() {
        assertEquals(192 to 64, logoThumbnailSize(1800, 600))
    }
    @Test fun portraitAndSquareAreBounded() {
        assertEquals(48 to 192, logoThumbnailSize(1000, 4000))
        assertEquals(192 to 192, logoThumbnailSize(4096, 4096))
    }
    @Test fun smallImagesAreNotUpscaled() {
        assertEquals(64 to 32, logoThumbnailSize(64, 32))
    }
    @Test fun extremeAspectRatiosCannotProduceZeroSizedBitmaps() {
        assertEquals(192 to 1, logoThumbnailSize(Int.MAX_VALUE, 1))
        assertEquals(1 to 192, logoThumbnailSize(1, Int.MAX_VALUE))
    }
    @Test fun invalidDimensionsAreRejectedBeforeBitmapAllocation() {
        assertTrue(runCatching { logoThumbnailSize(0, 192) }.isFailure)
        assertTrue(runCatching { logoThumbnailSize(192, -1) }.isFailure)
    }
}
