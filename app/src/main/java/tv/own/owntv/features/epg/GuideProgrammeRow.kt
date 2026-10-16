package tv.own.owntv.features.epg

import java.util.Collections
import tv.own.owntv.core.database.entity.EpgProgrammeEntity

/** One immutable interpretation of a row for painting, information, selection and D-pad. */
internal class GuideProgrammeRow private constructor(
    val programmes: List<EpgProgrammeEntity>,
    val cells: List<GuideProgrammeCells.Cell>,
    val windowStart: Long,
    val windowEnd: Long,
) {
    val earliestStart = programmes.minOfOrNull { it.startMs }
    val latestStop = programmes.maxOfOrNull { it.stopMs }

    private fun afterStart(time: Long): Int {
        var low = 0
        var high = cells.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (cells[mid].startMs <= time) low = mid + 1 else high = mid
        }
        return low
    }

    fun at(time: Long): EpgProgrammeEntity? {
        val index = afterStart(time) - 1
        return cells.getOrNull(index)?.takeIf { time < it.stopMs }?.let { programmes[it.programmeIndex] }
    }

    fun adjacentTime(time: Long, delta: Int): Long? {
        if (cells.isEmpty()) return null
        val after = afterStart(time)
        val previous = after - 1
        val current = previous.takeIf { it >= 0 && time < cells[it].stopMs }
        val target = when {
            current != null -> (current + delta).coerceIn(0, cells.lastIndex)
            delta < 0 -> previous.coerceAtLeast(0)
            else -> after.coerceAtMost(cells.lastIndex)
        }
        return cells[target].startMs
    }

    companion object {
        fun prepare(programmes: List<EpgProgrammeEntity>, from: Long, to: Long): GuideProgrammeRow {
            val snapshot = Collections.unmodifiableList(programmes.toList())
            return GuideProgrammeRow(snapshot,
                Collections.unmodifiableList(GuideProgrammeCells.layout(snapshot, from, to)), from, to)
        }
    }
}

/** Main-thread UI owner. Inputs are immutable reader emissions; identity avoids an O(n) equality scan.
 * Derived data is bounded separately from the raw-row cache; no process-global state.
 */
internal class GuideProgrammeRowCache<K>(private val maxRows: Int = 24, private val maxItems: Int = 16_000) {
    private class Entry(val input: List<EpgProgrammeEntity>, val row: GuideProgrammeRow) {
        val weight = input.size.toLong() * 2 + row.cells.size
    }
    private val entries = LinkedHashMap<K, Entry>(16, 0.75f, true)
    private var items = 0L

    fun get(key: K, programmes: List<EpgProgrammeEntity>, from: Long, to: Long): GuideProgrammeRow {
        entries[key]?.let { if (it.input === programmes && it.row.windowStart == from && it.row.windowEnd == to) return it.row }
        val entry = Entry(programmes, GuideProgrammeRow.prepare(programmes, from, to))
        entries.remove(key)?.let { items -= it.weight }
        if (entry.weight <= maxItems && maxRows > 0) {
            entries[key] = entry
            items += entry.weight
            while (entries.size > maxRows || items > maxItems) {
                val oldest = entries.entries.iterator()
                items -= oldest.next().value.weight
                oldest.remove()
            }
        }
        return entry.row
    }

    fun clear() { entries.clear(); items = 0L }
    internal fun size() = entries.size
    internal fun itemCount() = items
}
