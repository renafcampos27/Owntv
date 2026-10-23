package tv.own.owntv.features.live

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive

/** Register before tuning: even an immediate open/error belongs to one exact choice and channel. */
internal class ChannelAttemptSignal {
    class Ticket(val choice: Long, val source: Long, val channel: Long) {
        val outcome = CompletableDeferred<Boolean>()
    }
    private var active: Ticket? = null
    fun begin(choice: Long, source: Long, channel: Long): Ticket {
        cancel()
        return Ticket(choice, source, channel).also { active = it }
    }
    fun complete(choice: Long?, source: Long, channel: Long, opened: Boolean) {
        val ticket = active ?: return
        if (ticket.choice == choice && ticket.source == source && ticket.channel == channel) ticket.outcome.complete(opened)
    }
    fun clear(ticket: Ticket) { if (active === ticket) cancel() }
    fun cancel() { active?.outcome?.cancel(); active = null }
}

/** The observer is a child of this attempt, never a new LiveTuneController.launch/tune owner. */
internal suspend fun awaitChannelAttempt(
    ticket: ChannelAttemptSignal.Ticket,
    start: suspend () -> Unit,
    observeErrors: suspend () -> Unit,
): Boolean = kotlinx.coroutines.coroutineScope {
    kotlinx.coroutines.currentCoroutineContext().ensureActive()
    if (ticket.outcome.isCancelled) return@coroutineScope ticket.outcome.await()
    start()
    if (ticket.outcome.isCompleted) return@coroutineScope ticket.outcome.await()
    val observer = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { observeErrors() }
    try { ticket.outcome.await() } finally { observer.cancel() }
}
