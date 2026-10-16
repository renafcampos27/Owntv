package tv.own.owntv.features.shell.components

import org.junit.Assert.*
import org.junit.Test

class RailCategoryIdentityTest {
    @Test fun duplicateLabelsRemainDistinctAfterReorderAndRename() {
        val a = RailCategory("News", providerName = "List", stableId = "folder:1")
        val b = RailCategory("News", providerName = "List", stableId = "folder:2")
        val before = railCategoryKeys(listOf(a, b))
        val after = railCategoryKeys(listOf(b.copy(fullName = "Notícias"), a))
        assertNotEquals(before[0], before[1])
        assertEquals(before[0], after[1])
        assertEquals(before[1], after[0])
    }
    @Test fun syntheticRowsRemainStableWhenAnotherCategoryIsInserted() {
        val all = RailCategory("All", stableId = "all")
        val folder = RailCategory("News", stableId = "folder:1")
        val old = railCategoryKeys(listOf(all, folder))
        val now = railCategoryKeys(listOf(all.copy(fullName = "TV DIRETO"), RailCategory("Sport", stableId = "folder:2"), folder))
        assertEquals(old[0], now[0])
        assertEquals(old[1], now[2])
    }
}
