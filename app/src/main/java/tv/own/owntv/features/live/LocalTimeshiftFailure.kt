package tv.own.owntv.features.live

/** Release only this capture's claim, after its producer has stopped; stale completions do nothing. */
internal suspend fun finishLocalTimeshiftFailure(
    isCurrent: () -> Boolean,
    stopCapture: suspend () -> Unit,
    releaseClaim: () -> Unit,
    notifyFailure: () -> Unit,
) {
    if (!isCurrent()) return
    stopCapture()
    if (!isCurrent()) return
    releaseClaim()
    notifyFailure()
}
