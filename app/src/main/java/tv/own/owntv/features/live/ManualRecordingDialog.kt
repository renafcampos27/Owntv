package tv.own.owntv.features.live

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import tv.own.owntv.core.R as CoreR
import tv.own.owntv.ui.components.*
import tv.own.owntv.ui.theme.OwnTVTheme
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Remote-friendly clock controls. Scheduling never depends on a programme or EPG offset. */
@Composable
internal fun ManualRecordingDialog(channelName: String, onSave: suspend (Long, Long) -> Boolean, onDismiss: () -> Unit) {
    val now = remember { ZonedDateTime.now() }
    var day by remember { mutableIntStateOf(0) }
    var startMinute by remember { mutableIntStateOf(now.hour * 60 + now.minute) }
    var endMinute by remember { mutableIntStateOf((now.hour * 60 + now.minute + 60) % 1440) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val colors = OwnTVTheme.colors
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE dd/MM", Locale.getDefault()) }
    fun clock(minute: Int) = LocalTime.of(minute / 60, minute % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
    androidx.activity.compose.BackHandler(enabled = !saving) { onDismiss() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { focus.requestFocus() } }
    OwnTVPopup(onDismissRequest = { if (!saving) onDismiss() }) {
        Box(Modifier.fillMaxSize().modalScrim().trapAllFocusExit(), contentAlignment = Alignment.Center) {
            Column(Modifier.dialogPanel(width = 540.dp, padding = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(CoreR.string.recording_schedule_manual), color = colors.onSurface)
                Text(channelName, color = colors.onSurface)
                @Composable fun adjust(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, first: Boolean = false) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(label, Modifier.weight(1f), color = colors.onSurface)
                        OwnTVButton(stringResource(CoreR.string.recording_schedule_minus), onClick = { if (!saving) { failed = false; onMinus() } }, modifier = if (first) Modifier.focusRequester(focus) else Modifier)
                        Text(value, color = colors.onSurface)
                        OwnTVButton(stringResource(CoreR.string.recording_schedule_plus), onClick = { if (!saving) { failed = false; onPlus() } })
                    }
                }
                adjust(stringResource(CoreR.string.recording_schedule_day), now.toLocalDate().plusDays(day.toLong()).format(dayFormatter),
                    { day = (day - 1).coerceAtLeast(0) }, { day = (day + 1).coerceAtMost(30) }, first = true)
                adjust(stringResource(CoreR.string.recording_schedule_start_hour), clock(startMinute),
                    { startMinute = (startMinute + 1380) % 1440 }, { startMinute = (startMinute + 60) % 1440 })
                adjust(stringResource(CoreR.string.recording_schedule_start_minute), clock(startMinute),
                    { startMinute = (startMinute + 1439) % 1440 }, { startMinute = (startMinute + 1) % 1440 })
                adjust(stringResource(CoreR.string.recording_schedule_end_hour), clock(endMinute),
                    { endMinute = (endMinute + 1380) % 1440 }, { endMinute = (endMinute + 60) % 1440 })
                adjust(stringResource(CoreR.string.recording_schedule_end_minute), clock(endMinute),
                    { endMinute = (endMinute + 1439) % 1440 }, { endMinute = (endMinute + 1) % 1440 })
                if (endMinute <= startMinute) Text(stringResource(CoreR.string.recording_schedule_next_day), color = colors.onSurface)
                if (failed) Text(stringResource(CoreR.string.recording_schedule_failed), color = colors.onSurface)
                OwnTVButton(stringResource(CoreR.string.recording_schedule_manual), onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            try {
                                val date = now.toLocalDate().plusDays(day.toLong())
                                val startLocal = date.atTime(startMinute / 60, startMinute % 60)
                                val endLocal = date.plusDays(if (endMinute <= startMinute) 1 else 0).atTime(endMinute / 60, endMinute % 60)
                                // Do not silently shift a clock time that does not exist on the DST change day.
                                if (now.zone.rules.getValidOffsets(startLocal).isEmpty() || now.zone.rules.getValidOffsets(endLocal).isEmpty()) {
                                    failed = true
                                } else {
                                    val start = startLocal.atZone(now.zone)
                                    val end = endLocal.atZone(now.zone)
                                    if (onSave(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())) onDismiss() else failed = true
                                }
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { failed = true }
                            finally { saving = false }
                        }
                    }
                })
                OwnTVButton(stringResource(CoreR.string.content_close), onClick = { if (!saving) onDismiss() }, style = OwnTVButtonStyle.SECONDARY)
            }
        }
    }
}
