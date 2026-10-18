package tv.own.owntv.features.live

/** A single deliberate step opens now; a held key/burst opens only the final selection after 500 ms. */
internal const val ZAP_TUNE_DELAY_MS = 500L
private const val ZAP_ISOLATED_PRESS_MS = 1_500L
internal fun zapTuneDelayMs(lastPressMs: Long?, nowMs: Long): Long =
    if (lastPressMs == null || nowMs - lastPressMs !in 0 until ZAP_ISOLATED_PRESS_MS) 0L else ZAP_TUNE_DELAY_MS
