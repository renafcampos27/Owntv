package tv.own.owntv.features.shell.components

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SettingsSearchTextTest {
    @Test fun `Portuguese labels match without requiring accent input`() {
        val tokens = settingsSearchTokens("  definicoes   reproducao ")
        val target = settingsSearchNormalize("Definições — Reprodução")
        assertTrue(tokens.all(target::contains))
        assertEquals(2, tokens.size)
    }
    @Test fun `matching does not change with device casing locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            assertEquals("iptv", settingsSearchNormalize("IPTV"))
        } finally { Locale.setDefault(original) }
    }
    @Test fun `empty and line separated queries are handled consistently`() {
        assertTrue(settingsSearchTokens(" ").isEmpty())
        assertEquals(listOf("buffer", "hls"), settingsSearchTokens("buffer\nhls"))
    }
}
