package tv.own.owntv.features.shell.components

import java.text.Normalizer
import java.util.Locale

private val accents = Regex("\\p{M}+")
private val spaces = Regex("\\s+")
internal fun settingsSearchNormalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(accents, "").lowercase(Locale.ROOT)
internal fun settingsSearchTokens(query: String): List<String> =
    settingsSearchNormalize(query).trim().split(spaces).filter { it.isNotBlank() }
