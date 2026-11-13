package tv.own.owntv.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.reflect.KProperty

/** Information and controls are separate: dismissing an intro cannot hide controls opened with OK. */
@Stable
internal class PlayerHudVisibility(compactStart: Boolean) {
    private enum class Panel { INFORMATION, CONTROLS, HIDDEN }
    private var panel by mutableStateOf(if (compactStart) Panel.INFORMATION else Panel.CONTROLS)
    val informationVisible: Boolean get() = panel == Panel.INFORMATION
    val controlsVisible: Boolean get() = panel == Panel.CONTROLS

    fun showInformation() { panel = Panel.INFORMATION }
    fun hideInformation() { if (informationVisible) panel = Panel.HIDDEN }
    fun showControls() { panel = Panel.CONTROLS }
    fun hideControls() { panel = Panel.HIDDEN }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = controlsVisible
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        if (value) showControls() else hideControls()
    }
}
