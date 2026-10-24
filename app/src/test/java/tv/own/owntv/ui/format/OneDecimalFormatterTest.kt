package tv.own.owntv.ui.format

import java.util.Locale
import java.util.concurrent.Executors
import org.junit.Assert.*
import org.junit.Test

class OneDecimalFormatterTest {
    @Test fun localeChangesAndConcurrentCallsKeepTheirOwnFormatters() {
        val pool = Executors.newFixedThreadPool(4)
        try {
            val calls = (0 until 200).map { i -> pool.submit<String> {
                OneDecimalFormatter.format(1234.5, if (i % 2 == 0) Locale.US else Locale.GERMANY)
            } }
            calls.forEachIndexed { i, future -> assertEquals(if (i % 2 == 0) "1,234.5" else "1.234,5", future.get()) }
        } finally { pool.shutdownNow() }
    }
}
