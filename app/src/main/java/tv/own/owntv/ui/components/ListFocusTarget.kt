package tv.own.owntv.ui.components

internal data class ListFocusTarget(val id: Long, val visualIndex: Int)

/** Headers occupy visual slots, but are never candidates for restoring item focus. */
internal fun listFocusTarget(rows: List<Long?>, id: Long, previousVisualIndex: Int): ListFocusTarget? {
    val survivor = rows.indexOf(id)
    val index = if (survivor >= 0) survivor else
        rows.indices.firstOrNull { it >= previousVisualIndex.coerceAtLeast(0) && rows[it] != null }
            ?: rows.indexOfLast { it != null }
    return rows.getOrNull(index)?.let { ListFocusTarget(it, index) }
}
