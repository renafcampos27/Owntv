package tv.own.owntv.player

import org.junit.Assert.*
import org.junit.Test

class FailedChannelAttemptsTest {
    @Test fun `profile switch clears rows and the captured status`() {
        FailedChannelAttempts.observe(201, "Response code: 403")
        FailedChannelAttempts.record("Old profile", null, 1, owner = 201)
        FailedChannelAttempts.clear()
        assertTrue(FailedChannelAttempts.state.value.isEmpty())
        FailedChannelAttempts.record("New profile", null, 2, owner = 201)
        assertNull(FailedChannelAttempts.state.value.last().httpCode)
    }
    @Test fun `HTTP status is retained without retaining provider credentials`() {
        FailedChannelAttempts.record("Test", "Response code: 403 https://host/user/secret/token", 1)
        val row = FailedChannelAttempts.state.value.last()
        assertEquals(403, row.httpCode)
        assertFalse(row.toString().contains("secret"))
        assertFalse(row.toString().contains("token"))
    }
    @Test fun `digits from URLs or deadlines are not classified as HTTP status`() {
        assertNull(FailedChannelAttempts.httpCode("https://host/403/channel deadline 404 ms"))
        assertEquals(404, FailedChannelAttempts.httpCode("HTTP status: 404"))
        assertNull(FailedChannelAttempts.httpCode(null))
    }
    @Test fun `original status survives a generic terminal error only for the same attempt`() {
        FailedChannelAttempts.observe(101, "Response code: 403")
        FailedChannelAttempts.record("A", null, 1, owner = 101)
        assertEquals(403, FailedChannelAttempts.state.value.last().httpCode)
        FailedChannelAttempts.record("B", null, 2, owner = 102)
        assertNull(FailedChannelAttempts.state.value.last().httpCode)
    }
    @Test fun `failed attempts stay bounded and chronological after an alternative opens`() {
        repeat(15) { FailedChannelAttempts.record("Channel $it", null, it.toLong()) }
        val rows = FailedChannelAttempts.state.value
        assertEquals(10, rows.size)
        assertEquals(5L, rows.first().atMs)
        assertEquals(14L, rows.last().atMs)
    }
}
