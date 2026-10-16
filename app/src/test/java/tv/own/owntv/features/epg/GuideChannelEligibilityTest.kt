package tv.own.owntv.features.epg

import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.database.entity.CategoryEntity
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.model.MediaType

class GuideChannelEligibilityTest {
    private val channel = ChannelEntity(id = 7, sourceId = 4, categoryId = 10,
        remoteId = "remote", name = "Channel", streamUrl = "https://example.invalid/stream", catchup = true)
    @Test fun `manually matched hidden category cannot reappear`() {
        assertFalse(guideChannelEligible(channel.copy(epgChannelId = "manual"), setOf(4), setOf(10), emptySet()))
    }
    @Test fun `kids policy still hides a matched adult category`() {
        val categories = listOf(CategoryEntity(10, 4, MediaType.LIVE, "Adult +18"))
        val hidden = AdultCategoryClassifier.hiddenCategoryIds(categories, emptySet(), true)
        assertFalse(guideChannelEligible(channel.copy(epgChannelId = "fallback"), setOf(4), hidden, emptySet()))
    }
    @Test fun `inactive sources and hidden current item keys cannot be reintroduced`() {
        assertFalse(guideChannelEligible(channel, setOf(8), emptySet(), emptySet()))
        assertFalse(guideChannelEligible(channel, setOf(4), emptySet(), setOf(CustomizeKeys.channel(channel))))
    }
    @Test fun `new database ids do not suppress an otherwise visible matched channel`() {
        val imported = channel.copy(id = 99, categoryId = 20)
        assertTrue(guideChannelEligible(imported, setOf(4), setOf(10), emptySet()))
        assertFalse(guideChannelEligible(imported, setOf(4), setOf(20), emptySet()))
        assertTrue(guideChannelEligible(imported.copy(categoryId = null), setOf(4), setOf(10), emptySet()))
    }
}
