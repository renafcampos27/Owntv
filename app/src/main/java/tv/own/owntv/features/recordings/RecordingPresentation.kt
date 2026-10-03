package tv.own.owntv.features.recordings

import java.util.Locale

/** Media time only: wall-clock activity and programme windows are not measured capture duration. */
internal fun captureDurationText(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0) / 1_000L
    return if (seconds >= 3_600) {
        String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3_600, seconds / 60 % 60, seconds % 60)
    } else {
        String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
    }
}
