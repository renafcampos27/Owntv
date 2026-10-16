package tv.own.owntv.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val IPTV_PACKAGE = "tv.own.owntv"
private const val TIMEOUT_MS = 15_000L

private fun MacrobenchmarkScope.requireTag(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), TIMEOUT_MS)) {
        "Missing $tag. Complete setup, import the fixture and show Live/Guide/Recordings navigation."
    }

private fun MacrobenchmarkScope.navigate(section: String, screen: String) {
    val selector = By.res("owntv_nav_$section")
    val target = device.findObject(selector)
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
internal fun MacrobenchmarkScope.exerciseIptvJourney() {
    requireTag("owntv_shell")
    navigate("LIVE_TV", "owntv_live_screen")
    requireTag("owntv_channel_list")
    val rows = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
    check(rows.size >= 3) { "Fixture needs at least three visible channels." }
    val id = rows.first().resourceName.substringAfterLast('_')
        rows.first().click() // Open/return first so D-pad focus restoration is exercised too.
        requireTag("owntv_player_ready_$id")
        leavePlayer()
        repeat(8) { device.pressDPadDown(); device.waitForIdle() }
        repeat(8) { device.pressDPadUp(); device.waitForIdle() }
    repeat(3) { index ->
        val visible = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
        check(visible.size >= 3) { "Channel list did not return after playback." }
        repeat(index + 1) { device.pressDPadDown(); device.waitForIdle() }
        val row = checkNotNull(device.findObject(By.res(Pattern.compile("owntv_channel_[0-9]+")).focused(true))) {
            "D-pad focus did not return to a channel row."
        }
        val id = row.resourceName.substringAfterLast('_')
        device.pressDPadCenter()
        requireTag("owntv_player_ready_$id")
        device.pressDPadUp()
            check(device.wait(Until.hasObject(By.res(Pattern.compile("owntv_player_ready_(?!$id$)[0-9]+"))), TIMEOUT_MS)) {
                "D-pad zapping did not produce playback on a different channel."
            }
        leavePlayer()
    }
    navigate("EPG", "owntv_epg_screen")
    check(!device.hasObject(By.res("owntv_epg_replay_only_on")) ||
        device.wait(Until.hasObject(By.res("owntv_epg_ready")), TIMEOUT_MS)) {
        "Synthetic fixture has no archive. Disable Guide 'Replay only' before the test and restore its previous value afterwards."
    }
    requireTag("owntv_epg_ready")
    device.pressDPadRight()
        repeat(4) { device.pressDPadDown(); device.waitForIdle() }
        repeat(4) { device.pressDPadLeft(); device.waitForIdle() }
    navigate("DOWNLOADS", "owntv_recordings_screen")
    navigate("LIVE_TV", "owntv_live_screen")
}

/** A→B→C→A uses the same fixture IDs and asserts readiness on every return. */
internal fun MacrobenchmarkScope.exerciseRepeatChannels() {
    navigate("LIVE_TV", "owntv_live_screen")
    val ids = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
        .take(3).map { it.resourceName }
    check(ids.size == 3) { "Three fixture channels are required." }
    repeat(2) {
        ids.forEach { resource ->
            requireTag(resource).click()
            requireTag("owntv_player_ready_${resource.substringAfterLast('_')}")
            leavePlayer()
        }
    }
}

/** Prepare focus and playback outside the frame measurement. */
internal fun MacrobenchmarkScope.prepareListNavigation() {
    navigate("LIVE_TV", "owntv_live_screen")
    val rows = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
    check(rows.size >= 3) { "Populate the fixture before measuring." }
    val id = rows.first().resourceName.substringAfterLast('_')
        rows.first().click()
        requireTag("owntv_player_ready_$id")
        leavePlayer()
        check(device.wait(Until.hasObject(By.res(Pattern.compile("owntv_channel_[0-9]+")).focused(true)), TIMEOUT_MS)) {
            "Focus did not return to the channel list."
        }
}

/** Measure only list movement; startup and network opening happen outside this journey. */
internal fun MacrobenchmarkScope.exerciseListNavigation() {
    requireTag("owntv_live_screen")
    val rows = device.findObjects(By.res(Pattern.compile("owntv_channel_[0-9]+")))
    check(rows.size >= 3) { "Populate the fixture before measuring." }
    check(device.hasObject(By.res(Pattern.compile("owntv_channel_[0-9]+")).focused(true))) { "Missing channel focus." }
        repeat(12) { device.pressDPadDown() }
        repeat(12) { device.pressDPadUp() }
    device.waitForIdle()
}

internal fun MacrobenchmarkScope.prepareGuideNavigation() {
    navigate("EPG", "owntv_epg_screen")
    check(!device.hasObject(By.res("owntv_epg_replay_only_on")) ||
        device.wait(Until.hasObject(By.res("owntv_epg_ready")), TIMEOUT_MS)) {
        "Synthetic fixture has no archive. Disable Guide 'Replay only' before the test and restore its previous value afterwards."
    }
    requireTag("owntv_epg_ready")
    val strips = By.res(Pattern.compile("owntv_epg_strip_ready_[0-9]+"))
    check(device.wait(Until.hasObject(strips), TIMEOUT_MS)) { "Fixture needs loaded guide programmes." }
    // A strip is a D-pad focus target, not a clickable programme. Enter CELL mode once.
    repeat(4) {
        if (!device.hasObject(strips.focused(true))) device.pressDPadRight()
    }
    check(device.wait(Until.hasObject(strips.focused(true)), TIMEOUT_MS)) { "Guide strip did not receive focus." }
    device.pressDPadCenter()
    device.waitForIdle()
    requireTag("owntv_epg_screen")
}

internal fun MacrobenchmarkScope.exerciseGuideNavigation() {
    requireTag("owntv_epg_screen")
    repeat(12) { device.pressDPadLeft() }
    repeat(12) { device.pressDPadRight() }
    device.waitForIdle()
    check(device.hasObject(By.res(Pattern.compile("owntv_epg_strip_ready_[0-9]+")).focused(true))) {
        "Guide movement lost focus or the visible programme data is unavailable."
    }
}
