package tv.own.owntv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamInfoSummaryTest {
    @Test fun unknownClassificationKeepsTheUsefulDecoderName() {
        val input = StreamInfoValue.Decoder(DecoderKind.NAMED, name = "vendor.decoder")
        assertEquals("vendor.decoder", input.forSummary().name)
        assertEquals(DecoderKind.NAMED, input.forSummary().kind)
    }

    @Test fun knownClassificationCanBeAbbreviatedWithoutChangingTheDiagnostic() {
        val input = StreamInfoValue.Decoder(DecoderKind.NAMED, name = "vendor.decoder", hardware = true)
        val summary = input.forSummary()
        assertNull(summary.name)
        assertEquals(DecoderKind.HARDWARE, summary.kind)
        assertEquals("vendor.decoder", input.name)
    }
}
