package tv.own.owntv.baselineprofile

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val IPTV_PACKAGE = "tv.own.owntv"
private const val TIMEOUT_MS = 15_000L

internal fun remoteDevice(): Boolean {
    val context = InstrumentationRegistry.getInstrumentation().context
    return (context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager).currentModeType ==
        Configuration.UI_MODE_TYPE_TELEVISION ||
        context.packageManager.hasSystemFeature("android.software.leanback") ||
        context.packageManager.hasSystemFeature("android.hardware.type.television") ||
        !context.packageManager.hasSystemFeature("android.hardware.touchscreen")
}

private fun MacrobenchmarkScope.requireTag(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), TIMEOUT_MS)) {
        "Missing $tag. Complete setup, import the fixture and show Live/Guide/Recordings navigation."
    }

private fun MacrobenchmarkScope.navigate(section: String, screen: String) {
    val selector = By.res("owntv_nav_$section")
    var target = device.findObject(selector)
    if (target == null) {
        val navigation = device.findObject(By.res("owntv_navigation"))
        if (navigation != null) {
            repeat(5) {
                if (target == null) {
                    navigation.swipe(Direction.LEFT, 0.75f)
                    device.waitForIdle()
                    target = device.findObject(selector)
                }
            }
            repeat(5) {
                if (target == null) {
                    navigation.swipe(Direction.RIGHT, 0.75f)
                    device.waitForIdle()
                    target = device.findObject(selector)
                }
            }
        }
    }
    (target ?: requireTag("owntv_nav_$section")).click()
    requireTag(screen)
}

private fun MacrobenchmarkScope.leavePlayer() {
    repeat(4) {
        if (device.hasObject(By.res("owntv_live_screen"))) return
        device.pressBack()
        device.waitForIdle()
    }
    requireTag("owntv_live_screen")
}

/** Locale-independent assertions stop recording if setup/empty content replaced the intended path. */
internal fun MacrobenchmarkScope.exerciseIptvJourney(remote: Boolean) {
    requireTag("owntv_shell")
    navigate("LIVE_TV", "owntv_live_screen")
    val list = requireTag("owntv_channel_list")
    val rows = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
    check(rows.size >= 3) { "Fixture needs at least three visible channels." }
    if (remote) {
        val id = rows.first().resourceName.substringAfterLast('_')
        rows.first().click() // Open/return first so D-pad focus restoration is exercised too.
        requireTag("owntv_player_ready_$id")
        leavePlayer()
        repeat(8) { device.pressDPadDown(); device.waitForIdle() }
        repeat(8) { device.pressDPadUp(); device.waitForIdle() }
    } else {
        val bounds = list.visibleBounds
        device.swipe(bounds.centerX(), bounds.bottom - 32, bounds.centerX(), bounds.top + 32, 20)
        device.waitForIdle()
        device.swipe(bounds.centerX(), bounds.top + 32, bounds.centerX(), bounds.bottom - 32, 20)
        device.waitForIdle()
    }
    repeat(3) { index ->
        val visible = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
        check(visible.size >= 3) { "Channel list did not return after playback." }
        if (remote) {
            repeat(index + 1) { device.pressDPadDown(); device.waitForIdle() }
        }
        val row = if (remote) checkNotNull(device.findObject(By.res(Pattern.compile("owntv_channel_[0-9]+")).focused(true))) {
            "D-pad focus did not return to a channel row."
        } else visible[index]
        val id = row.resourceName.substringAfterLast('_')
        if (remote) device.pressDPadCenter() else row.click()
        requireTag("owntv_player_ready_$id")
        if (remote) {
            device.pressDPadUp()
            check(device.wait(Until.hasObject(By.res(Pattern.compile("owntv_player_ready_(?!$id$)[0-9]+"))), TIMEOUT_MS)) {
                "D-pad zapping did not produce playback on a different channel."
            }
        }
        leavePlayer()
    }
    navigate("EPG", "owntv_epg_screen")
    requireTag("owntv_epg_ready")
    if (remote) {
        device.pressDPadRight()
        repeat(4) { device.pressDPadDown(); device.waitForIdle() }
        repeat(4) { device.pressDPadLeft(); device.waitForIdle() }
    } else {
        val bounds = requireTag("owntv_epg_screen").visibleBounds
        device.swipe(bounds.right - 32, bounds.centerY(), bounds.left + 32, bounds.centerY(), 20)
    }
    navigate("DOWNLOADS", "owntv_recordings_screen")
    navigate("LIVE_TV", "owntv_live_screen")
}
