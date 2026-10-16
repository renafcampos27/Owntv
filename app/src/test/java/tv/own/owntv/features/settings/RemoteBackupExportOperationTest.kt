package tv.own.owntv.features.settings

import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RemoteBackupExportOperationTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun overlappingExportsWithSameCoreFilenameCannotOverwriteEachOther() {
        val a = remoteBackupExportFolder(folder.root).apply { mkdirs() }
        val b = remoteBackupExportFolder(folder.root).apply { mkdirs() }
        assertNotEquals(a, b)
        assertEquals(folder.root, a.parentFile)
        assertEquals(folder.root, b.parentFile)
        val served = File(b, "owntv-backup.own").apply { writeText("new encrypted export") }
        File(a, "owntv-backup.own").writeText("late cancelled encrypted export")
        assertEquals("new encrypted export", served.readText())
    }

    /** Like a blocking exporter: completion still returns after its job was cancelled. */
    private class Export {
        lateinit var completion: Continuation<Result<File>>
        suspend fun run(): Result<File> = suspendCoroutine { completion = it }
        fun succeed(name: String) = completion.resume(Result.success(File(name)))
        fun fail() = completion.resume(Result.failure(IllegalStateException("export failed")))
    }

    private class Harness {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val started = mutableListOf<File>()
        var stops = 0
        var state = "idle"
        val operation = RemoteBackupExportOperation(
            scope, startServer = { started += it }, stopServer = { stops++ },
            working = { state = "working" }, idle = { state = "idle" },
            failed = { state = "error" },
        )
        fun dispose() { operation.close(); scope.cancel() }
    }

    @Test fun closingBeforeBlockingCompletionPreventsServerAndStatePublication() {
        val h = Harness()
        try {
            val a = Export()
            h.operation.start(a::run)
            assertEquals("working", h.state)
            h.operation.close()
            h.state = "restore picker"
            a.succeed("cancelled.own")
            assertTrue(h.started.isEmpty())
            assertEquals(0, h.stops)
            assertEquals("restore picker", h.state)
        } finally { h.dispose() }
    }

    @Test fun oldSuccessAfterNewSuccessCannotReplaceOrStopNewServer() {
        val h = Harness()
        try {
            val a = Export()
            val b = Export()
            h.operation.start(a::run)
            h.operation.start(b::run)
            b.succeed("B.own")
            a.succeed("A.own")
            assertEquals(listOf(File("B.own")), h.started)
            assertEquals("idle", h.state)
            assertEquals(0, h.stops)
            h.operation.close()
            assertEquals(1, h.stops)
        } finally { h.dispose() }
    }

    @Test fun oldSuccessWhileReplacementPendingDoesNotClearWorkingState() {
        val h = Harness()
        try {
            val a = Export()
            val b = Export()
            h.operation.start(a::run)
            h.operation.start(b::run)
            a.succeed("A.own")
            assertEquals("working", h.state)
            assertTrue(h.started.isEmpty())
            b.succeed("B.own")
            assertEquals(listOf(File("B.own")), h.started)
        } finally { h.dispose() }
    }

    @Test fun oldFailureAfterNewSuccessCannotPublishErrorOrStopNewServer() {
        val h = Harness()
        try {
            val a = Export()
            val b = Export()
            h.operation.start(a::run)
            h.operation.start(b::run)
            b.succeed("B.own")
            a.fail()
            assertEquals("idle", h.state)
            assertEquals(listOf(File("B.own")), h.started)
            assertEquals(0, h.stops)
        } finally { h.dispose() }
    }

    @Test fun normalExportStartsOnceAndRepeatedCloseStopsOnce() {
        val h = Harness()
        try {
            val a = Export()
            h.operation.start(a::run)
            a.succeed("A.own")
            assertEquals(listOf(File("A.own")), h.started)
            assertEquals("idle", h.state)
            h.operation.close()
            h.operation.close()
            assertEquals(1, h.stops)
        } finally { h.dispose() }
    }

    @Test fun replacementStopsExistingServerAndOwnsTheNextOne() {
        val h = Harness()
        try {
            h.operation.start { Result.success(File("A.own")) }
            val b = Export()
            h.operation.start(b::run)
            assertEquals(1, h.stops)
            assertEquals("working", h.state)
            b.succeed("B.own")
            h.operation.close()
            assertEquals(listOf(File("A.own"), File("B.own")), h.started)
            assertEquals(2, h.stops)
        } finally { h.dispose() }
    }

    @Test fun currentFailurePublishesErrorWithoutStartingServer() {
        val h = Harness()
        try {
            val a = Export()
            h.operation.start(a::run)
            a.fail()
            assertEquals("error", h.state)
            assertTrue(h.started.isEmpty())
            assertEquals(0, h.stops)
        } finally { h.dispose() }
    }
}
