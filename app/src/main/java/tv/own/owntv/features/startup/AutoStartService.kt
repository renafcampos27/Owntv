package tv.own.owntv.features.startup

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import tv.own.owntv.MainActivity
import tv.own.owntv.features.settings.SimpleModeOptions
import tv.own.owntv.features.settings.SimpleModePreferences

/** Opt-in system-bound service. No screen content, gestures or remote keys are collected. */
class AutoStartService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var options = SimpleModeOptions()
    private var registered = false
    private var pendingWake = false
    private var lastLaunch = 0L
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> pendingWake = true
                Intent.ACTION_SCREEN_ON -> { pendingWake = true; launchIfNeeded() }
                Intent.ACTION_USER_PRESENT -> launchIfNeeded()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        failure.value = null
        // Keep the manifest configuration intact. Do not replace serviceInfo with an empty
        // event mask during activation. Use a non-empty, app-scoped configuration.
        if (registered) { connected.value = true; return }
        try {
            ContextCompat.registerReceiver(this, receiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }, ContextCompat.RECEIVER_NOT_EXPORTED)
            registered = true
            connected.value = true
            scope.launch {
                SimpleModePreferences.observe(this@AutoStartService)
                    .catch { reportFailure(it) }
                    .collect {
                        options = it
                        launchIfNeeded()
                    }
            }
        } catch (error: Exception) {
            reportFailure(error)
        }
    }

    private fun reportFailure(error: Throwable) {
        connected.value = false
        failure.value = error.javaClass.simpleName
        android.util.Log.e(AutoStartService::class.java.simpleName, "Automatic startup failed", error)
    }

    private fun launchIfNeeded() {
        try { launchWhenReady() } catch (error: Exception) { reportFailure(error) }
    }

    private fun launchWhenReady() {
        if (!getSystemService(PowerManager::class.java).isInteractive ||
            getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        val bootCount = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1)
        val state = getSharedPreferences("automatic_start_state", Context.MODE_PRIVATE)
        val newBoot = bootCount >= 0 && state.getInt("last_boot", -1) != bootCount
        if (!(options.startOnBoot && newBoot) && !(options.startOnWake && pendingWake)) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastLaunch != 0L && now - lastLaunch < 2_000) return
        lastLaunch = now
        pendingWake = false
        startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(AUTO_START, true)
            })
        // MainActivity acknowledges the boot only if the system actually opened it.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onUnbind(intent: Intent?): Boolean {
        connected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected.value = false
        if (registered) runCatching { unregisterReceiver(receiver) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        val connected = kotlinx.coroutines.flow.MutableStateFlow(false)
        val failure = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        const val AUTO_START = "tv.own.owntv.AUTO_START"
        fun acknowledge(context: Context) {
            val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            if (boot >= 0) context.getSharedPreferences("automatic_start_state", Context.MODE_PRIVATE)
                .edit().putInt("last_boot", boot).apply()
        }
    }
}
