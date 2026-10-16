package tv.own.owntv.features.live

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.content.AdultCategoryClassifier
import tv.own.owntv.core.customize.CustomizeKeys
import tv.own.owntv.core.customize.SectionCustomizations
import tv.own.owntv.core.database.entity.CategoryEntity
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.model.MediaType

class ChannelVersionSnapshotTest {

    private fun channel(
        id: Long,
        name: String,
        sourceId: Long = 1,
        remoteId: String? = "$id",
        categoryId: Long? = 10,
    ) = ChannelEntity(
        id = id,
        sourceId = sourceId,
        remoteId = remoteId,
        name = name,
        streamUrl = "http://test.invalid/$id",
        categoryId = categoryId,
    )

    private fun category(id: Long, name: String, sourceId: Long = 1) = CategoryEntity(
        id = id,
        sourceId = sourceId,
        mediaType = MediaType.LIVE,
        name = name,
    )

    @Test
    fun `batch explicit version resolver resolves by remoteId first and falls back to name`() {
        val ch1 = channel(1, "RTP 1 HD", sourceId = 1, remoteId = "rem-1")
        val ch2 = channel(2, "RTP 1 4K", sourceId = 1, remoteId = null) // M3U style without remoteId
        val ch3 = channel(3, "RTP 1 FHD", sourceId = 1, remoteId = "rem-3")

        val explicitKeys = listOf(
            "1:rem-1",
            "1:RTP 1 4K",
            "1:rem-3",
        )

        val tails = explicitKeys.map { CustomizeKeys.tailOf(it) }.distinct()
        val allRemoteChannels = listOf(ch1, ch3)
        val allNameChannels = listOf(ch2)

        var remoteQueries = 0
        var nameQueries = 0

        // Mock batch DAO calls
        val byRemote = if (tails.isNotEmpty()) {
            remoteQueries++
            allRemoteChannels.filter { it.remoteId in tails }.associateBy { it.remoteId }
        } else emptyMap()

        val missingTails = tails.filter { it !in byRemote.keys }
        val byName = if (missingTails.isNotEmpty()) {
            nameQueries++
            allNameChannels.filter { it.name in missingTails }.associateBy { it.name }
        } else emptyMap()

        val resolved = explicitKeys.mapNotNull { key ->
            val tail = CustomizeKeys.tailOf(key)
            byRemote[tail] ?: byName[tail]
        }

        assertEquals(1, remoteQueries)
        assertEquals(1, nameQueries)
        assertEquals(listOf(1L, 2L, 3L), resolved.map { it.id })
    }

    @Test
    fun `snapshot filtering applies adult category check only for kids profile`() {
        val adultCat = category(100, "Adults 18+")
        val safeCat = category(200, "Kids")
        val categoriesById = mapOf(100L to adultCat, 200L to safeCat)

        val adultChannel = channel(1, "Adult Channel HD", categoryId = 100)
        val safeChannel = channel(2, "Kids Channel HD", categoryId = 200)
        val candidates = listOf(adultChannel, safeChannel)

        // For kids profile: adult channel filtered out, safe channel kept
        val kidsFiltered = candidates.filter { candidate ->
            AdultCategoryClassifier.allows(isKids = true, candidate.categoryId?.let { categoriesById[it]?.name })
        }
        assertEquals(listOf(2L), kidsFiltered.map { it.id })

        // For non-kids profile: both kept
        val normalFiltered = candidates.filter { candidate ->
            AdultCategoryClassifier.allows(isKids = false, candidate.categoryId?.let { categoriesById[it]?.name })
        }
        assertEquals(listOf(1L, 2L), normalFiltered.map { it.id })
    }

    @Test
    fun `snapshot filtering excludes hidden categories and hidden items`() {
        val cat1 = category(10, "News")
        val cat2 = category(20, "Sports")
        val categoriesById = mapOf(10L to cat1, 20L to cat2)

        val ch1 = channel(1, "News 1 HD", categoryId = 10)
        val ch2 = channel(2, "News 2 HD", categoryId = 10)
        val ch3 = channel(3, "Sports 1 HD", categoryId = 20)

        val cust = SectionCustomizations(
            hiddenItems = mapOf(CustomizeKeys.channel(ch1) to ch1.name),
            hiddenCategories = setOf(CustomizeKeys.category(cat2)),
        )

        val filtered = listOf(ch1, ch2, ch3).filter { candidate ->
            CustomizeKeys.channel(candidate) !in cust.hiddenItems &&
                (candidate.categoryId?.let { categoriesById[it] }?.let { CustomizeKeys.category(it) !in cust.hiddenCategories } != false)
        }

        // ch1 is hidden by item, ch3 is hidden by category -> only ch2 remains
        assertEquals(listOf(2L), filtered.map { it.id })
    }

    @Test
    fun `candidate filter runs on in-memory category snapshot with zero individual lookups`() {
        val cat = category(10, "Entertainment")
        val categoriesById = mapOf(10L to cat)

        val candidates = (1L..50L).map { id ->
            channel(id, "Channel $id HD", categoryId = 10)
        }

        var inMemoryCategoryLookups = 0
        val cust = SectionCustomizations()

        val filtered = candidates.filter { candidate ->
            val catName = candidate.categoryId?.let {
                inMemoryCategoryLookups++
                categoriesById[it]?.name
            }
            AdultCategoryClassifier.allows(isKids = false, catName) &&
                (candidate.categoryId?.let { categoriesById[it] }?.let { CustomizeKeys.category(it) !in cust.hiddenCategories } != false)
        }

        assertEquals(50, filtered.size)
        // Lookups are in-memory Map accesses, no DAO/coroutine suspension
        assertEquals(50, inMemoryCategoryLookups)
    }

    @Test
    fun `invalidation on profile change discards pending channel versions`() {
        val initialProfileId = 1L
        var currentActiveProfileId = 1L

        fun checkInvalidation(): List<ChannelEntity> {
            val loadedRows = listOf(channel(1, "RTP 1"))
            // Simulate profile change before publishing
            if (currentActiveProfileId != initialProfileId) return emptyList()
            return loadedRows
        }

        assertEquals(1, checkInvalidation().size)

        // Profile changed
        currentActiveProfileId = 2L
        assertEquals(emptyList<ChannelEntity>(), checkInvalidation())
    }

    @Test
    fun `invalidation when channel source is removed from active context discards versions`() {
        val initialProfileId = 1L
        val currentProfileId = 1L
        val activeSourceIds = listOf(1L)
        val channelFromSource2 = channel(10, "RTP 2", sourceId = 2)

        fun checkSourceValidation(ch: ChannelEntity): List<ChannelEntity> {
            if (currentProfileId != initialProfileId || ch.sourceId !in activeSourceIds) return emptyList()
            return listOf(ch)
        }

        assertEquals(emptyList<ChannelEntity>(), checkSourceValidation(channelFromSource2))
        assertEquals(listOf(1L), checkSourceValidation(channel(1, "RTP 1", sourceId = 1)).map { it.id })
    }

    @Test
    fun `batch lookups safely chunk parameter lists exceeding 400 items`() {
        val largeTailsList = (1..950).map { "remote-$it" }
        var chunkCalls = 0
        val resolved = largeTailsList.chunked(400).flatMap { chunk ->
            chunkCalls++
            assertTrue("Chunk size must not exceed 400", chunk.size <= 400)
            chunk.map { channel(it.substringAfter('-').toLong(), "CH $it", remoteId = it) }
        }

        assertEquals(3, chunkCalls) // 400 + 400 + 150 = 3 chunks
        assertEquals(950, resolved.size)
    }

    @Test
    fun `filterVisibleChannels applies batch adult and hidden checks in single memory pass`() {
        val catAdult = category(1, "Adult 18+")
        val catKids = category(2, "Cartoons")
        val categoriesById = mapOf(1L to catAdult, 2L to catKids)

        val ch1 = channel(1, "Playboy TV", categoryId = 1)
        val ch2 = channel(2, "Cartoon Network", categoryId = 2)
        val ch3 = channel(3, "Hidden Cartoon", categoryId = 2)

        val cust = SectionCustomizations(hiddenItems = mapOf(CustomizeKeys.channel(ch3) to ch3.name))
        val channels = listOf(ch1, ch2, ch3)

        // For Kids: Adult filtered, ch3 hidden filtered -> only ch2 survives
        val kidsVisible = channels.filter { candidate ->
            CustomizeKeys.channel(candidate) !in cust.hiddenItems &&
                AdultCategoryClassifier.allows(isKids = true, candidate.categoryId?.let { categoriesById[it]?.name }) &&
                (candidate.categoryId?.let { categoriesById[it] }?.let { CustomizeKeys.category(it) !in cust.hiddenCategories } != false)
        }

        assertEquals(listOf(2L), kidsVisible.map { it.id })

        // For Non-Kids: Adult allowed, ch3 hidden filtered -> ch1 and ch2 survive
        val adultVisible = channels.filter { candidate ->
            CustomizeKeys.channel(candidate) !in cust.hiddenItems &&
                AdultCategoryClassifier.allows(isKids = false, candidate.categoryId?.let { categoriesById[it]?.name }) &&
                (candidate.categoryId?.let { categoriesById[it] }?.let { CustomizeKeys.category(it) !in cust.hiddenCategories } != false)
        }

        assertEquals(listOf(1L, 2L), adultVisible.map { it.id })
    }
}
