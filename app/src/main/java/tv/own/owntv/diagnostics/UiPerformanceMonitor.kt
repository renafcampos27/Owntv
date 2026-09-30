package tv.own.owntv.diagnostics

import android.os.Build
import android.hardware.display.DisplayManager
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.KeyEvent
import android.view.Display
import android.view.Window
import tv.own.owntv.player.LiveDiagnosticsLog

/** Installed only while detailed diagnostics are enabled and the activity is visible. */
internal class UiPerformanceMonitor(private val window: Window) : AutoCloseable {
    private val samples = UiPerformanceWindow()
    private val thread = HandlerThread("owntv-ui-diagnostics", Process.THREAD_PRIORITY_BACKGROUND)
    private lateinit var handler: Handler
    private var lastCpuMs = 0L
    private var lastWallMs = 0L
    private var lastGcCount = 0L
    private var lastGcMs = 0L
    @Volatile private var closed = false
    private val displayManager = window.context.getSystemService(DisplayManager::class.java)
    private val displayId = window.decorView.display?.displayId ?: Display.DEFAULT_DISPLAY
    @Volatile private var fallbackDeadlineNs = (1_000_000_000.0 /
        (window.decorView.display?.refreshRate?.takeIf { it > 0 } ?: 60f)).toLong()
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) = Unit
        override fun onDisplayRemoved(id: Int) = Unit
        override fun onDisplayChanged(id: Int) {
            if (id == displayId) displayManager?.getDisplay(id)?.refreshRate?.takeIf { it > 0 }?.let {
                fallbackDeadlineNs = (1_000_000_000.0 / it).toLong()
            }
        }
    }

    private val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
        if (!closed) {
            val deadline = if (Build.VERSION.SDK_INT >= 31) metrics.getMetric(FrameMetrics.DEADLINE) else -1L
            // Consume scalar values in this callback; the platform reuses the FrameMetrics object.
            samples.frame(metrics.getMetric(FrameMetrics.TOTAL_DURATION),
                deadline.takeIf { it > 0 } ?: fallbackDeadlineNs, dropped,
                metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L)
        }
    }
    private val report = object : Runnable {
        override fun run() {
            if (closed || !LiveDiagnosticsLog.enabled) return
            val snapshot = samples.take()
            val cpuMs = Process.getElapsedCpuTime()
            val wallMs = SystemClock.elapsedRealtime()
            val gcCount = gcStat("art.gc.gc-count")
            val gcMs = gcStat("art.gc.gc-time")
            val runtime = Runtime.getRuntime()
            LiveDiagnosticsLog.event("ui_perf windowMs=${wallMs - lastWallMs} frames=${snapshot.frames} slow=${snapshot.slowFrames} " +
                "frameP95BucketMs=${snapshot.frameP95BucketMs} reportsDropped=${snapshot.reportsDropped} " +
                "deadlineMode=${if (Build.VERSION.SDK_INT >= 31) "platform-or-display" else "display"} " +
                "keys=${snapshot.keys} keyQueueP95BucketMs=${snapshot.keyQueueP95BucketMs} " +
                "processCpuMs=${(cpuMs - lastCpuMs).coerceAtLeast(0)} " +
                "javaHeapMiB=${(runtime.totalMemory() - runtime.freeMemory()) / MIB} " +
                "nativeHeapMiB=${Debug.getNativeHeapAllocatedSize() / MIB} " +
                "gcCount=${(gcCount - lastGcCount).coerceAtLeast(0)} gcMs=${(gcMs - lastGcMs).coerceAtLeast(0)}")
            lastCpuMs = cpuMs; lastWallMs = wallMs; lastGcCount = gcCount; lastGcMs = gcMs
            handler.postDelayed(this, 10_000)
        }
    }

    fun start() {
        thread.start()
        handler = Handler(thread.looper)
        lastCpuMs = Process.getElapsedCpuTime()
        lastWallMs = SystemClock.elapsedRealtime()
        lastGcCount = gcStat("art.gc.gc-count"); lastGcMs = gcStat("art.gc.gc-time")
        window.addOnFrameMetricsAvailableListener(listener, handler)
        displayManager?.registerDisplayListener(displayListener, handler)
        handler.postDelayed(report, 10_000)
    }

    /** Dispatch queue delay, not end-to-end key-to-screen latency. No key code/text is stored. */
    fun key(event: KeyEvent) {
        if (!closed && event.action == KeyEvent.ACTION_DOWN) {
            samples.key(SystemClock.uptimeMillis() - event.eventTime)
        }
    }

    override fun close() {
        closed = true
        runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
        runCatching { displayManager?.unregisterDisplayListener(displayListener) }
        if (::handler.isInitialized) handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }

    private fun gcStat(key: String): Long = runCatching { Debug.getRuntimeStat(key)?.toLongOrNull() ?: 0 }.getOrDefault(0)

    private companion object { const val MIB = 1024L * 1024 }
}
