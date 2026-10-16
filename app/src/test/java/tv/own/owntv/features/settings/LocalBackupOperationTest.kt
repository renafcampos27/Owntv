package tv.own.owntv.features.settings

import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test

class LocalBackupOperationTest {
    private class Work {
        lateinit var completion: Continuation<String>
        suspend fun run(): String = suspendCoroutine { completion = it }
        fun finish(value: String) = completion.resume(value)
    }
    private class Harness {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var state = "idle"
        val operation = LocalBackupOperation<String>(scope, working = { state = "working" },
            idle = { state = "idle" }, publish = { state = it })
        fun dispose() { operation.close(); scope.cancel() }
    }

    @Test fun resetWhileImportWritesDoesNotCancelOrAdmitAnotherWriter() {
        val h = Harness()
        try {
            val import = Work()
            assertTrue(h.operation.start({ "error" }, import::run))
            assertFalse(h.operation.reset())
            assertEquals("working", h.state)
            var competingRan = false
            assertFalse(h.operation.start({ "error" }) { competingRan = true; "wrong export" })
            assertFalse(competingRan)
            import.finish("restored")
            assertEquals("restored", h.state)
            assertFalse(h.operation.active)
            assertTrue(h.operation.reset())
            assertEquals("idle", h.state)
        } finally { h.dispose() }
    }

    @Test fun retiredBlockingCompletionCannotOverwriteReplacement() {
        val h = Harness()
        try {
            val old = Work()
            h.operation.start({ "error" }, old::run)
            h.operation.close()
            val newer = Work()
            h.operation.start({ "error" }, newer::run)
            old.finish("old result")
            assertEquals("working", h.state)
            assertTrue(h.operation.active)
            newer.finish("new result")
            assertEquals("new result", h.state)
        } finally { h.dispose() }
    }

    @Test fun unexpectedFailureReleasesOwnershipAndNextOperationCanRun() {
        val h = Harness()
        try {
            h.operation.start({ "error" }) { throw IllegalStateException("synthetic") }
            assertEquals("error", h.state)
            assertFalse(h.operation.active)
            h.operation.start({ "error" }) { "exported" }
            assertEquals("exported", h.state)
        } finally { h.dispose() }
    }
}
