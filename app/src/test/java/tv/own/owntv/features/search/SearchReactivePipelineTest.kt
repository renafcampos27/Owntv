@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package tv.own.owntv.features.search

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import tv.own.owntv.core.content.SearchResults
import tv.own.owntv.core.database.dao.ChannelSearchResult
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.repository.ActiveProfileSources

class SearchReactivePipelineTest {

    private fun dummyChannel(id: Long, name: String, sourceId: Long = 1) = ChannelSearchResult(
        channel = ChannelEntity(
            id = id,
            sourceId = sourceId,
            remoteId = "rem-$id",
            name = name,
            streamUrl = "http://example.com/$id",
        ),
        categoryName = "General",
    )

    private fun dummySource(id: Long, name: String) = SourceEntity(
        id = id,
        name = name,
        type = tv.own.owntv.core.model.SourceType.M3U,
        url = "http://source.example.com",
        syncLive = true,
        syncMovies = true,
        syncSeries = true,
    )

    @Test
    fun `short query emits empty results without executing search`() = runBlocking {
        val query = MutableStateFlow("")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(dummySource(1, "Main"))))
        var searchesExecuted = 0

        val pipeline = buildTestPipeline(query, ctx) { _, _, _ ->
            searchesExecuted++
            SearchResults()
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }
        delay(50)

        query.value = "a"
        delay(50)

        assertEquals(0, searchesExecuted)
        assertTrue(emissions.last().isEmpty)

        job.cancel()
    }

    @Test
    fun `valid query triggers search with active context`() = runBlocking {
        val query = MutableStateFlow("")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(dummySource(1, "Main"))))
        val executedQueries = mutableListOf<Triple<Long, Long, String>>()

        val pipeline = buildTestPipeline(query, ctx) { pid, sources, q ->
            executedQueries += Triple(pid, sources.sources.first().id, q)
            SearchResults(channels = listOf(dummyChannel(10, "RTP 1")))
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }

        query.value = "RTP"
        awaitCondition { executedQueries.size == 1 }

        assertEquals(1, executedQueries.size)
        assertEquals(Triple(1L, 1L, "RTP"), executedQueries.first())
        assertEquals(1, emissions.last().channels.size)
        assertEquals("RTP 1", emissions.last().channels.first().channel.name)

        job.cancel()
    }

    @Test
    fun `profile change with same query re-triggers search with new profile`() = runBlocking {
        val query = MutableStateFlow("RTP")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(dummySource(1, "Main"))))
        val executedProfiles = mutableListOf<Long>()

        val pipeline = buildTestPipeline(query, ctx) { pid, _, _ ->
            executedProfiles += pid
            if (pid == 1L) {
                SearchResults(channels = listOf(dummyChannel(10, "RTP 1 (Profile 1)")))
            } else {
                SearchResults(channels = listOf(dummyChannel(20, "RTP 1 (Profile 2)")))
            }
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }

        awaitCondition { executedProfiles.size == 1 }
        assertEquals(listOf(1L), executedProfiles)
        assertEquals("RTP 1 (Profile 1)", emissions.last().channels.first().channel.name)

        // Change profile while query stays "RTP"
        ctx.value = ActiveProfileSources(2L, listOf(dummySource(1, "Main")))
        awaitCondition { executedProfiles.size == 2 }

        assertEquals(listOf(1L, 2L), executedProfiles)
        assertEquals("RTP 1 (Profile 2)", emissions.last().channels.first().channel.name)

        job.cancel()
    }

    @Test
    fun `source list change with same query re-triggers search with new sources`() = runBlocking {
        val query = MutableStateFlow("SIC")
        val source1 = dummySource(1, "Source 1")
        val source2 = dummySource(2, "Source 2")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(source1)))
        val executedSources = mutableListOf<List<Long>>()

        val pipeline = buildTestPipeline(query, ctx) { _, sources, _ ->
            executedSources += sources.sourceIds
            SearchResults(channels = listOf(dummyChannel(sources.sourceIds.first(), "SIC")))
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }

        awaitCondition { executedSources.size == 1 }
        assertEquals(listOf(listOf(1L)), executedSources)

        // Switch active source/playlist
        ctx.value = ActiveProfileSources(1L, listOf(source2))
        awaitCondition { executedSources.size == 2 }

        assertEquals(listOf(listOf(1L), listOf(2L)), executedSources)
        assertEquals(2L, emissions.last().channels.first().channel.id)

        job.cancel()
    }

    @Test
    fun `changing profile cancels in-flight search and prevents stale results from publishing`() = runBlocking {
        val query = MutableStateFlow("Eurosport")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(dummySource(1, "Main"))))
        val completedSearches = mutableListOf<Long>()

        val pipeline = buildTestPipeline(query, ctx) { pid, _, _ ->
            if (pid == 1L) {
                delay(200) // slow search for profile 1
            } else {
                delay(10) // fast search for profile 2
            }
            completedSearches += pid
            SearchResults(channels = listOf(dummyChannel(pid, "Result for $pid")))
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }
        delay(50) // profile 1 search started, still in progress

        // Quick profile switch to profile 2 before profile 1 finishes
        ctx.value = ActiveProfileSources(2L, listOf(dummySource(1, "Main")))
        awaitCondition { emissions.any { !it.isEmpty && it.channels.first().channel.id == 2L } }
        delay(250) // allow profile 1 delay to expire if it was running

        // Only profile 2 results should have been published, never stale profile 1 results
        val publishedChannelIds = emissions.filter { !it.isEmpty }.map { it.channels.first().channel.id }
        assertTrue("Published IDs: $publishedChannelIds", publishedChannelIds.all { it == 2L })
        assertFalse(publishedChannelIds.contains(1L))

        job.cancel()
    }

    @Test
    fun `switching profile immediately clears previous results before new search finishes`() = runBlocking {
        val query = MutableStateFlow("News")
        val ctx = MutableStateFlow(ActiveProfileSources(1L, listOf(dummySource(1, "Main"))))

        val pipeline = buildTestPipeline(query, ctx) { pid, _, _ ->
            if (pid == 1L) {
                SearchResults(channels = listOf(dummyChannel(10, "Profile 1 Channel")))
            } else {
                delay(100) // Profile 2 search takes time
                SearchResults(channels = listOf(dummyChannel(20, "Profile 2 Channel")))
            }
        }

        val emissions = mutableListOf<SearchResults>()
        val job = launch { pipeline.collect { emissions += it } }

        // Wait for profile 1 results
        awaitCondition { emissions.any { it.channels.any { ch -> ch.channel.id == 10L } } }
        assertEquals(10L, emissions.last().channels.first().channel.id)

        // Switch to profile 2
        ctx.value = ActiveProfileSources(2L, listOf(dummySource(1, "Main")))

        // Immediately after context switch, emissions must record an empty result clearing Profile 1
        awaitCondition { emissions.last().isEmpty }
        assertTrue("Previous profile results must be cleared immediately upon context switch", emissions.last().isEmpty)

        // Then profile 2 results arrive
        awaitCondition { emissions.any { it.channels.any { ch -> ch.channel.id == 20L } } }
        assertEquals(20L, emissions.last().channels.first().channel.id)

        job.cancel()
    }

    private suspend fun awaitCondition(timeoutMs: Long = 2000, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - start > timeoutMs) fail("Condition timed out")
            delay(20)
        }
    }

    @Test
    fun `movie and series sync flags are stripped from search context`() {
        val rawSources = listOf(
            dummySource(1, "S1").copy(syncLive = true, syncMovies = true, syncSeries = true),
            dummySource(2, "S2").copy(syncLive = true, syncMovies = false, syncSeries = true),
        )
        val context = ActiveProfileSources(1L, rawSources)
        val stripped = context.copy(sources = context.sources.map { it.copy(syncMovies = false, syncSeries = false) })

        assertTrue(stripped.movieSourceIds.isEmpty())
        assertTrue(stripped.seriesSourceIds.isEmpty())
        assertEquals(listOf(1L, 2L), stripped.liveSourceIds)
    }

    private fun buildTestPipeline(
        query: MutableStateFlow<String>,
        ctx: MutableStateFlow<ActiveProfileSources>,
        searchReader: suspend (profileId: Long, sources: ActiveProfileSources, query: String) -> SearchResults,
    ): Flow<SearchResults> {
        var lastSearchedContext: ActiveProfileSources? = null
        val debouncedQuery = query
            .map { it.trim() }
            .distinctUntilChanged()
            .debounce { q -> if (q.length < 2) 0L else 300L }
            .distinctUntilChanged()

        return combine(debouncedQuery, ctx) { q, context ->
            q to context
        }
            .distinctUntilChanged()
            .flatMapLatest { (q, context) ->
                if (q.length < 2 || context.profileId < 0L) {
                    flowOf(SearchResults())
                } else {
                    flow {
                        if (lastSearchedContext != null && lastSearchedContext != context) {
                            emit(SearchResults())
                        }
                        val searchResults = searchReader(context.profileId, context, q)
                        if (currentCoroutineContext().isActive && ctx.value == context) {
                            lastSearchedContext = context
                            emit(searchResults)
                        }
                    }
                }
            }
    }
}
