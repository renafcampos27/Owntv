package tv.own.owntv.ui.format

import java.text.NumberFormat
import java.util.Locale

/** NumberFormat is mutable: cache per thread and replace it when the effective locale changes. */
internal object OneDecimalFormatter {
    private val local = ThreadLocal<Pair<Locale, NumberFormat>>()
    fun format(value: Double, locale: Locale = Locale.getDefault()): String {
        val formatter = local.get()?.takeIf { it.first == locale }?.second ?: NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }.also { local.set(locale to it) }
        return formatter.format(value)
    }
}
