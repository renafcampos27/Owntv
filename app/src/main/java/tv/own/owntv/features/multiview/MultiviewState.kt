package tv.own.owntv.features.multiview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.live.OpenStreamRegistry
import tv.own.owntv.core.live.StreamGrant
import tv.own.owntv.core.live.StreamPurpose
import tv.own.owntv.core.live.connectionBudget
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.player.LiveEnginePool

/**
 * How many tiles a grid opens with: two, or the ceiling when the user set it lower.
 *
 * Two because Multiview is for watching more than one thing — opening at one would be
 * indistinguishable from full screen — and because the second tile is the invitation: it says
 * "+ Add channel" and is one press away. Everything beyond the second is asked for.
 */
private fun openingTileCount(maxTiles: Int): Int = minOf(2, maxTiles).coerceAtLeast(1)

/** One tile of the grid: empty, playing a channel, or explaining why it could not start. */
data class MultiviewTile(
    val channel: ChannelEntity? = null,
    /** Set when the playlist had no connection to spare — the tile shows this instead of a picture. */
    val refusal: StreamGrant.Refused? = null,
    /** No playback within the tile budget; this does not identify a hardware or provider cause. */
    val waitExpired: Boolean = false,
    val retiring: Boolean = false,
    val retirementFailed: Boolean = false,
) {
    val isEmpty: Boolean get() = channel == null && refusal == null && !waitExpired && !retiring && !retirementFailed
}

/**
 * What the Multiview grid is currently showing, and the rules for changing it.
 *
 * Held by the shell for as long as the grid is open, and thrown away with it. Everything that
 * decides *whether* a tile may tune is core's ([connectionBudget] over [OpenStreamRegistry]); this
 * class only asks, and remembers the answer so the tile can say it.
 */
@UnstableApi
class MultiviewState(
    val pool: LiveEnginePool,
    private val registry: OpenStreamRegistry,
    private val live: LiveViewModel,
    /**
     * The **most** tiles this grid may have, from Settings — not how many it opens with.
     *
     * The setting used to be the literal size, so choosing four meant every grid was four, and
     * watching two channels was impossible without going back into Settings to change a number. The
     * number the user picks is the ceiling their television and their account can stand; how many to
     * actually watch is a decision made in front of the grid, every time.
     */
    private val maxTiles: Int,
) {
    /**
     * The tiles on screen now. Grows and shrinks from the end only, which is what keeps a tile's
     * index — and therefore its engine in [LiveEnginePool] — the same for as long as it exists.
     */
    val tiles = mutableStateListOf<MultiviewTile>().apply {
        repeat(openingTileCount(maxTiles)) { add(MultiviewTile()) }
    }

    var primaryRetiring by mutableStateOf(false)

    /** Room for one more? */
    val canAddTile: Boolean get() = tiles.size < maxTiles

    /** One more empty tile for the user to fill. */
    fun addTile() {
        if (canAddTile) tiles.add(MultiviewTile())
    }

    /** Which tile the D-pad is on. */
    var focused by mutableIntStateOf(0)
        private set

    /** Which tile has the sound. Kept here as well as in the pool so the badge can be drawn. */
    var audible by mutableIntStateOf(-1)
        private set

    private val claims = HashMap<Int, OpenStreamRegistry.Claim>()
    private var released = false
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val revisions = HashMap<Int, Long>()

    /**
     * Tiles playing sound with no picture — the owner's background-audio case (§2.4): watch one
     * channel while another's commentary plays behind it.
     *
     * It costs a provider connection exactly as a picture does, because dropping the video track does
     * not close the stream, so the tile says so instead of looking free.
     */
    val soundOnly = mutableStateListOf<Int>()

    fun setSoundOnly(tile: Int, value: Boolean) {
        if (released) return
        if (tiles.getOrNull(tile)?.channel == null) return
        if (tiles[tile].retiring || tiles[tile].retirementFailed) return
        pool.setSoundOnly(tile, value)
        if (value) {
            if (tile !in soundOnly) soundOnly.add(tile)
            audible = tile
        } else {
            soundOnly.remove(tile)
        }
    }

    fun focus(tile: Int) {
        if (tile in tiles.indices) focused = tile
    }

    /**
     * A tile whose engine has failed keeps its audio track playing — the picture is what failed, not
     * the stream — so a tile reading "Couldn't play this channel" was still making a noise. Silence
     * it, and move the sound to a tile that is actually showing something.
     */
    fun silenceFailed(tile: Int) {
        if (released) return
        if (tile in soundOnly) pool.setSoundOnly(tile, false)
        pool.peek(tile)?.setMuted(true)
        soundOnly.remove(tile)
        if (audible != tile) return
        val next = tiles.indices.firstOrNull { it != tile && tiles[it].channel != null && !failed(it) }
        if (next != null) giveSoundTo(next) else { audible = -1; pool.giveSoundTo(null) }
    }

    private fun failed(tile: Int): Boolean =
        tiles.getOrNull(tile)?.let { it.retiring || it.retirementFailed } == true ||
            pool.peek(tile)?.state?.value == tv.own.owntv.player.LivePreviewEngine.State.ERROR

    /** Give the sound to a filled tile. An empty one keeps whatever had it — there is nothing to hear. */
    fun giveSoundTo(tile: Int) {
        if (released) return
        if (tiles.getOrNull(tile)?.channel == null) return
        if (tiles[tile].retiring || tiles[tile].retirementFailed) return
        // A failed tile has no sound to give, so handing it the sound would only un-mute the audio of
        // a channel that is not on screen.
        if (failed(tile)) return
        audible = tile
        pool.giveSoundTo(tile)
    }

    /**
     * Put [channel] in [tile], if that playlist has a connection to spare.
     *
     * The check is D11's: tiles and recordings count against one budget, per playlist, and a tile
     * that loses is told why instead of being left to fail into a spinner. Refilling a tile releases
     * what it held first, so swapping a channel never counts as two.
     */
    fun fill(tile: Int, channel: ChannelEntity) {
        if (released || primaryRetiring) return
        if (tile !in tiles.indices) return
        val retired = retireTile(tile)
        val revision = revisions.getValue(tile)
        tiles[tile] = MultiviewTile(channel = channel, retiring = true)
        soundOnly.remove(tile)
        cleanupScope.launch {
            val closed = retired.await() && pool.retryTileRetirements(tile) && pool.retryRetirements(channel.sourceId)
            if (released || revisions[tile] != revision || tile !in tiles.indices) return@launch
            if (!closed) {
                tiles[tile] = MultiviewTile(channel = channel, retirementFailed = true)
                return@launch
            }
            val source = live.sourceOf(channel)
            when (val admission = registry.admit(channel.sourceId, source, StreamPurpose.WATCHING)) {
                is OpenStreamRegistry.Admission.Refused -> {
                    tiles[tile] = MultiviewTile(refusal = admission.grant)
                    return@launch
                }
                is OpenStreamRegistry.Admission.Allowed -> claims[tile] = admission.claim
            }
            val takesSound = tiles.indices.none { it != tile && tiles[it].channel != null && !tiles[it].retiring && !tiles[it].retirementFailed }
            val engine = pool.engineFor(tile)
            tiles[tile] = MultiviewTile(channel = channel)
            live.tuneTile(tile = tile, engine = engine, channel = channel,
                muted = !takesSound && audible != tile,
                isStillActive = { !released && revisions[tile] == revision && pool.peek(tile) === engine &&
                    !engine.isDisposed && tiles.getOrNull(tile)?.channel?.id == channel.id })
            if (takesSound) giveSoundTo(tile) else if (audible == tile) pool.giveSoundTo(tile)
        }
    }

    /** Release an unresponsive tile after its bounded wait without guessing why it failed. */
    fun expirePlaybackWait(tile: Int) {
        if (released) return
        if (tiles.getOrNull(tile)?.channel == null) return
        retireTile(tile)
        soundOnly.remove(tile)
        tiles[tile] = MultiviewTile(waitExpired = true)
        // The sound cannot stay on a tile that has none. Hand it to a tile that still plays.
        if (audible == tile) {
            val next = tiles.indices.firstOrNull { tiles[it].channel != null && !failed(it) }
            if (next != null) giveSoundTo(next) else { audible = -1; pool.giveSoundTo(null) }
        }
    }

    /**
     * Empty a tile: stop its engine, hand the connection back, and forget the refusal it was showing.
     *
     * Emptying the *last* tile also removes it, so "Remove tile" shrinks a grid the user has finished
     * with instead of leaving a permanent hole. Only ever from the end, so no surviving tile changes
     * index and no engine is silently handed to a different channel.
     */
    fun clear(tile: Int) {
        if (released) return
        if (tile !in tiles.indices) return
        retireTile(tile)
        soundOnly.remove(tile)
        tiles[tile] = MultiviewTile()
        while (tiles.size > openingTileCount(maxTiles) && tiles.last().isEmpty) {
            retireTile(tiles.lastIndex)
            tiles.removeAt(tiles.lastIndex)
        }
        if (focused > tiles.lastIndex) focused = tiles.lastIndex
        if (audible == tile) {
            val next = tiles.indices.firstOrNull {
                val survivor = tiles[it]
                survivor.channel != null && survivor.refusal == null && !survivor.waitExpired && !failed(it)
            }
            if (next != null) giveSoundTo(next) else {
                audible = -1
                pool.giveSoundTo(null)
            }
        }
    }

    /** Leaving the grid. Every engine and every claim goes, or the next tile is refused for a ghost. */
    fun releaseAll() {
        // An outgoing composition can dispose after its successor starts using the shared pool.
        // Release this owner's resources once; never tear down a later grid from a stale callback.
        if (released) return
        released = true
        live.releaseAllTiles()
        (tiles.indices.toList() + claims.keys).distinct().forEach { retireTile(it) }
        pool.releaseAll()
        audible = -1
        soundOnly.clear()
        tiles.indices.forEach { tiles[it] = MultiviewTile() }
    }

    /** A transition to the main reader checks this after releaseAll; false does not authorize a new stream. */
    suspend fun awaitRetirement(): Boolean = pool.awaitRetirements()

    private fun retireTile(tile: Int): kotlinx.coroutines.Deferred<Boolean> {
        revisions[tile] = (revisions[tile] ?: 0L) + 1L
        live.cancelTile(tile)
        val claim = claims.remove(tile)
        return pool.retire(tile, claim?.sourceId) { if (claim != null) registry.release(claim) }
    }
}
