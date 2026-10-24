package tv.own.owntv.features.home

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.File
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.database.dao.CategoryDao
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.EpgDao
import tv.own.owntv.core.database.dao.HistoryDao
import tv.own.owntv.core.database.dao.MetadataDao
import tv.own.owntv.core.database.dao.MovieDao
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.ProgressDao
import tv.own.owntv.core.database.dao.SeriesDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.dao.TrendingDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.epg.EpgSourceStore
import tv.own.owntv.core.home.HeroItem
import tv.own.owntv.core.home.HomeFeed
import tv.own.owntv.core.home.HomeFeedReader
import tv.own.owntv.core.i18n.LocaleStore
import tv.own.owntv.core.launcher.LauncherRecommendationPlanner
import tv.own.owntv.core.live.GuideReader
import tv.own.owntv.core.live.LiveEpgReader
import tv.own.owntv.core.live.OpenStreamRegistry
import tv.own.owntv.core.metadata.MetadataOverrideStore
import tv.own.owntv.core.metadata.MetadataProvider
import tv.own.owntv.core.metadata.MetadataRepository
import tv.own.owntv.core.model.SourceType
import tv.own.owntv.core.network.HttpClient
import tv.own.owntv.core.network.StreamingHttpClient
import tv.own.owntv.core.parser.XtreamClient
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.stalker.StalkerAuthManager
import tv.own.owntv.core.stalker.StalkerClient
import tv.own.owntv.core.stalker.StalkerCredentials
import tv.own.owntv.core.stalker.StalkerSession
import tv.own.owntv.core.stalker.StreamUrlResolver
import tv.own.owntv.player.HeroPreviewEngine

@OptIn(ExperimentalCoroutinesApi::class)
class HomePreviewHandoverTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var streamingHttp: StreamingHttpClient
    private lateinit var registry: OpenStreamRegistry

    private val stalkerSource = SourceEntity(
        id = 1L,
        name = "Stalker Single Session",
        type = SourceType.STALKER,
        url = "http://portal.example.invalid/c",
        mac = "00:1A:79:11:22:33",
        maxConnections = 1,
        userAgent = "MAG250-Test",
    )

    private val channelA = ChannelEntity(
        id = 101L,
        sourceId = 1L,
        categoryId = 10L,
        name = "Channel A",
        streamUrl = "ffrt http://localhost/ch/101",
    )

    private val channelB = ChannelEntity(
        id = 102L,
        sourceId = 1L,
        categoryId = 10L,
        name = "Channel B",
        streamUrl = "ffrt http://localhost/ch/102",
    )

    private val channelC = ChannelEntity(
        id = 103L,
        sourceId = 1L,
        categoryId = 10L,
        name = "Channel C",
        streamUrl = "ffrt http://localhost/ch/103",
    )

    private val heroA = HeroItem.LiveHero(channel = channelA, watchedAt = 1_000L)
    private val heroB = HeroItem.LiveHero(channel = channelB, watchedAt = 900L)

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
        val rootDir = temp.newFolder("owntv-home-preview-test")
        val prefs = InMemorySharedPreferences()
        context = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(rootDir, "files").apply { mkdirs() }
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
        }
        val localeStore = LocaleStore.overPreferences(prefs, null)
        settings = SettingsRepository(context, localeStore, InMemoryPreferencesDataStore())
        streamingHttp = StreamingHttpClient(OkHttpClient())
        registry = OpenStreamRegistry()
    }

    @After
    fun tearDown() {
        try { streamingHttp.evictAll() } finally { Dispatchers.resetMain() }
    }

    @Test(timeout = 10_000)
    fun `suspended preview A resolution completing after channel B open request neither plays A nor publishes late state`() = runBlocking {
        val resolveEntered = CompletableDeferred<Unit>()
        val resolveGate = CompletableDeferred<String>()
        val playedPlayerBuilds = CopyOnWriteArrayList<String>()
        val stateTransitionsAfterChannelOpen = CopyOnWriteArrayList<HeroPreviewEngine.State>()
        val channelOpenRequested = AtomicBoolean(false)

        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            playerFactory = { ua, _ ->
                playedPlayerBuilds += ua
                null
            },
        )
        val stateObserver = launch {
            engine.state.collect { st ->
                if (channelOpenRequested.get() && st != HeroPreviewEngine.State.IDLE) {
                    stateTransitionsAfterChannelOpen += st
                }
            }
        }

        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA, heroB),
            onResolveChannel = { ch, _ ->
                assertEquals(channelA.id, ch.id)
                resolveEntered.complete(Unit)
                withContext(NonCancellable) {
                    resolveGate.await()
                }
            },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            // 1. Start preview A and wait until its URL resolution is suspended in Dispatchers.IO.
            val previewJob = async { vm.startPreview(heroA) }
            resolveEntered.await()
            assertTrue("Preview A must report active while URL resolution is in flight", vm.hasActivePreview())

            // 2. Request opening channel B while preview A's URL resolution is still suspended.
            channelOpenRequested.set(true)
            val openedChannels = CopyOnWriteArrayList<Long>()
            vm.requestOpenChannel(channelB.id, listOf(channelB), this) { id, _ ->
                openedChannels += id
            }
            assertTrue("Channel transition latch must be active immediately on channel open", vm.channelTransitionActive.value)
            kotlinx.coroutines.yield()
            assertEquals("Channel B must open without waiting on an unstarted preview player", listOf(channelB.id), openedChannels)

            // 3. New previews must be blocked while the channel transition is active.
            assertFalse(
                "Starting a new preview while channel transition is active must be rejected",
                vm.startPreview(heroA),
            )

            // 4. Complete preview A's resolution late and verify A neither opens nor publishes late state.
            resolveGate.complete("http://portal.example.invalid/live/101.m3u8")
            assertFalse("Invalidated preview A must return false after late resolution", previewJob.await())

            assertTrue("HeroPreviewEngine must never build/play for late preview A", playedPlayerBuilds.isEmpty())
            assertNull("HeroPreviewEngine currentUrl must remain null", engine.currentUrl)
            assertEquals("HeroPreviewEngine state must remain IDLE", HeroPreviewEngine.State.IDLE, engine.state.value)
            assertTrue("No non-IDLE state may be published after channel open", stateTransitionsAfterChannelOpen.isEmpty())
            assertFalse("HeroPreviewEngine must not hold an active session", engine.hasActiveSession)
            assertEquals("OpenStreamRegistry must have 0 watching claims", 0, registry.openOn(stalkerSource.id).watching)
        } finally {
            resolveGate.complete("")
            stateObserver.cancel()
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `opening channel during 520ms preview dwell cancels scheduled preview before URL resolution starts`() = runBlocking {
        val resolveCalls = AtomicInteger(0)
        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            playerFactory = { _, _ -> null },
        )
        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA),
            onResolveChannel = { _, _ ->
                resolveCalls.incrementAndGet()
                "http://portal.example.invalid/live/101.m3u8"
            },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            val dwellEntered = CompletableDeferred<Unit>()
            val previewJob = async {
                dwellEntered.complete(Unit)
                vm.startPreview(heroA, dwellDelayMs = 5_000L)
            }
            dwellEntered.await()
            while (!vm.hasActivePreview()) {
                kotlinx.coroutines.yield()
            }

            assertTrue("Channel B open must succeed after cancelling dwell preview", vm.stopPreviewBeforeChannel(channelB.id))
            assertFalse("Dwell preview must return false once cancelled by channel open", previewJob.await())
            assertEquals("URL resolver must never be invoked when preview is cancelled during dwell", 0, resolveCalls.get())
            assertEquals(HeroPreviewEngine.State.IDLE, engine.state.value)
            assertNull(engine.currentUrl)
        } finally {
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `changing hero selection while preview URL resolution is suspended drops stale preview`() = runBlocking {
        val resolveEntered = CompletableDeferred<Unit>()
        val resolveGate = CompletableDeferred<String>()
        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            playerFactory = { _, _ -> null },
        )
        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA, heroB),
            onResolveChannel = { _, _ ->
                resolveEntered.complete(Unit)
                withContext(NonCancellable) {
                    resolveGate.await()
                }
            },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            val previewJob = async { vm.startPreview(heroA) }
            resolveEntered.await()

            // Navigate to heroB while heroA's resolution is still in flight.
            vm.onHeroUserNavigate(1)
            resolveGate.complete("http://portal.example.invalid/live/101.m3u8")

            assertFalse("Hero A preview must be dropped after user navigated to Hero B", previewJob.await())
            assertEquals(HeroPreviewEngine.State.IDLE, engine.state.value)
            assertNull(engine.currentUrl)
            assertFalse(engine.hasActiveSession)
        } finally {
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `repeated channel clicks while single-session preview is releasing only open the latest channel after confirmed release`() = runBlocking {
        val releaseEntered = CompletableDeferred<Unit>()
        val releaseGate = CompletableDeferred<Boolean>()
        val releaseConfirmations = AtomicInteger(0)

        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            releaseConfirmation = {
                releaseConfirmations.incrementAndGet()
                releaseEntered.complete(Unit)
                releaseGate.await()
            },
            playerFactory = { _, _ -> null },
        )
        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA),
            onResolveChannel = { _, _ -> "http://portal.example.invalid/live/101.m3u8" },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            // 1. Start and establish an active single-session preview in HeroPreviewEngine.
            assertTrue(vm.startPreview(heroA))
            assertEquals(HeroPreviewEngine.State.LOADING, engine.state.value)
            assertTrue(engine.hasActiveSession)
            assertTrue(engine.hasSingleSessionPreview)
            assertEquals(1, registry.openOn(stalkerSource.id).watching)

            // 2. Click channel A, then channel B, then channel C while preview release confirmation is still pending.
            val openedChannels = CopyOnWriteArrayList<Long>()
            vm.requestOpenChannel(channelA.id, listOf(channelA), this) { id, _ ->
                openedChannels += id
            }
            releaseEntered.await()

            vm.requestOpenChannel(channelB.id, listOf(channelB), this) { id, _ ->
                openedChannels += id
            }
            vm.requestOpenChannel(channelC.id, listOf(channelC), this) { id, _ ->
                openedChannels += id
            }

            assertTrue(
                "No channel may open before the active single-session preview confirms release",
                openedChannels.isEmpty(),
            )
            assertEquals(
                "OpenStreamRegistry claim must remain held until release confirms",
                1,
                registry.openOn(stalkerSource.id).watching,
            )

            // 3. Confirm release of the preview session and verify ONLY the most recent channel (channelC) opens.
            releaseGate.complete(true)
            while (openedChannels.isEmpty()) {
                kotlinx.coroutines.yield()
            }
            kotlinx.coroutines.yield()

            assertEquals(
                "Only the most recent channel click must open the main player",
                listOf(channelC.id),
                openedChannels.toList(),
            )
            assertEquals(
                "Single retiring preview session must only run release confirmation once",
                1,
                releaseConfirmations.get(),
            )
            assertFalse("Preview session must be fully released after confirmation", engine.hasActiveSession)
            assertEquals("OpenStreamRegistry claim must be released after confirmation", 0, registry.openOn(stalkerSource.id).watching)
        } finally {
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `unconfirmed single-session preview release blocks channel open and resets transition latch`() = runBlocking {
        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            releaseConfirmation = { false },
            playerFactory = { _, _ -> null },
        )
        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA),
            onResolveChannel = { _, _ -> "http://portal.example.invalid/live/101.m3u8" },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            assertTrue(vm.startPreview(heroA))
            assertFalse(
                "Opening a channel must be blocked when single-session preview fails to confirm release",
                vm.stopPreviewBeforeChannel(channelB.id),
            )
            assertFalse(
                "Channel transition latch must reset when release confirmation fails",
                vm.channelTransitionActive.value,
            )
        } finally {
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `returning to Home or finishing current generation clears transition latch while stale generation is ignored`() = runBlocking {
        var shouldFailResolve = false
        val engine = HeroPreviewEngine(
            context = context,
            streamingHttp = streamingHttp,
            settings = null,
            streamInUse = { false },
            openStreams = registry,
            playerFactory = { _, _ -> null },
        )
        val vm = buildHomeViewModel(
            engine = engine,
            heroItems = listOf(heroA),
            onResolveChannel = { _, _ ->
                if (shouldFailResolve) throw java.io.IOException("Portal unreachable")
                "http://portal.example.invalid/live/101.m3u8"
            },
        )

        try {
            vm.refresh(1L)
            vm.uiState.first { !it.isLoading }
            vm.setHeroFocused(true)

            val firstGen: Long
            vm.requestOpenChannel(channelA.id, listOf(channelA), this) { _, _ -> }
            firstGen = vm.activeChannelOpenGeneration
            assertTrue(vm.channelTransitionActive.value)

            vm.requestOpenChannel(channelB.id, listOf(channelB), this) { _, _ -> }
            val secondGen = vm.activeChannelOpenGeneration
            assertTrue(secondGen > firstGen)

            // A stale completion from the first channel open must not clear the latch of the second open.
            vm.onChannelTransitionFinished(expectedGeneration = firstGen)
            assertTrue(
                "Stale channel open generation must not clear channelTransitionActive",
                vm.channelTransitionActive.value,
            )

            // Returning to Home (setPreviewEnabled(true) even when _previewEnabled was never flipped false)
            // or completing the current generation must clear the latch and allow previews again.
            vm.setPreviewEnabled(true)
            assertFalse(
                "setPreviewEnabled(true) after returning from fullscreen must clear channelTransitionActive",
                vm.channelTransitionActive.value,
            )

            // A portal resolution failure must return false cleanly without cancelling the caller scope.
            shouldFailResolve = true
            assertFalse(
                "Resolver IOException must return false without crashing caller scope",
                vm.startPreview(heroA),
            )
            shouldFailResolve = false
            assertTrue(
                "Preview must succeed after transient resolver failure",
                vm.startPreview(heroA),
            )
        } finally {
            engine.dispose()
        }
    }

    @Test(timeout = 10_000)
    fun `feed completing after active profile changes cannot publish the previous profile`() = runBlocking {
        val loaded = CompletableDeferred<Unit>()
        val engine = HeroPreviewEngine(context = context, streamingHttp = streamingHttp, settings = null,
            streamInUse = { false }, openStreams = registry, playerFactory = { _, _ -> null })
        val vm = buildHomeViewModel(engine, listOf(heroA), onResolveChannel = { _, _ -> "http://portal.example.invalid/live/101.m3u8" },
            beforeFeed = { settings.setActiveProfile(2L); loaded.complete(Unit) })
        try {
            vm.refresh(1L)
            loaded.await()
            kotlinx.coroutines.yield()
            assertTrue("Stale feed must not finish loading the previous profile", vm.uiState.value.isLoading)
            assertTrue(vm.uiState.value.heroItems.isEmpty())
        } finally { engine.dispose() }
    }

    private fun buildHomeViewModel(
        engine: HeroPreviewEngine,
        heroItems: List<HeroItem>,
        onResolveChannel: suspend (ChannelEntity, SourceEntity?) -> String,
        beforeFeed: suspend () -> Unit = {},
    ): HomeViewModel {
        val channelsById = listOf(channelA, channelB, channelC).associateBy { it.id }
        val sourcesById = mapOf(stalkerSource.id to stalkerSource)

        val movieDao = proxyDao<MovieDao>()
        val seriesDao = proxyDao<SeriesDao>()
        val categoryDao = proxyDao<CategoryDao>()
        val profileDao = proxyDao<ProfileDao>()
        val historyDao = proxyDao<HistoryDao>()
        val progressDao = proxyDao<ProgressDao>()
        val trendingDao = proxyDao<TrendingDao>()
        val epgDao = proxyDao<EpgDao>()
        val metadataDao = proxyDao<MetadataDao>()
        val metadataProvider = proxyDao<MetadataProvider>()

        val channelDao = proxyDao<ChannelDao> { method, args ->
            when (method.name) {
                "getById" -> channelsById[args?.getOrNull(0) as? Long]
                else -> null
            }
        }
        val sourceDao = proxyDao<SourceDao> { method, args ->
            when (method.name) {
                "getById" -> sourcesById[args?.getOrNull(0) as? Long]
                "allSourceIds", "sourceIdsForProfile" -> sourcesById.keys.toList()
                else -> null
            }
        }

        val stalkerClient = object : StalkerClient(OkHttpClient()) {
            override suspend fun createLink(
                apiBase: String,
                mac: String,
                token: String,
                userAgent: String?,
                cmd: String,
                type: String,
                episode: Int?,
            ): String {
                val matched = channelsById.values.firstOrNull { it.streamUrl == cmd } ?: channelA
                return onResolveChannel(matched, stalkerSource)
            }
        }
        val stalkerAuth = object : StalkerAuthManager(stalkerClient) {
            override suspend fun <T> withAuthRetry(
                creds: StalkerCredentials,
                block: suspend (StalkerSession) -> T,
            ): T = block(
                StalkerSession(
                    apiBase = "http://portal.example.invalid/stalker_portal/server/load.php",
                    token = "test-token",
                    expiresAtMs = Long.MAX_VALUE,
                    profile = emptyMap(),
                ),
            )
        }
        val streamUrlResolver = StreamUrlResolver(
            stalkerAuth = stalkerAuth,
            stalkerClient = stalkerClient,
            sourceDao = sourceDao,
        )

        val planner = LauncherRecommendationPlanner(
            sourceDao = sourceDao,
            movieDao = movieDao,
            seriesDao = seriesDao,
            progressDao = progressDao,
            categoryDao = categoryDao,
            profileDao = profileDao,
        )
        val epgSourceStore = EpgSourceStore(context)
        val xtreamClient = XtreamClient(HttpClient(OkHttpClient()))
        val liveEpgReader = LiveEpgReader(
            epgDao = epgDao,
            epgSourceStore = epgSourceStore,
            sourceDao = sourceDao,
            xtreamClient = xtreamClient,
            streamUrlResolver = streamUrlResolver,
        )
        val guideReader = GuideReader(
            epgDao = epgDao,
            epgSourceStore = epgSourceStore,
            sourceDao = sourceDao,
            liveEpgReader = liveEpgReader,
        )
        val feedReader = object : HomeFeedReader(
            planner = planner,
            movieDao = movieDao,
            seriesDao = seriesDao,
            channelDao = channelDao,
            categoryDao = categoryDao,
            customize = CustomizationStore(context),
            sourceDao = sourceDao,
            settings = settings,
            profileDao = profileDao,
            trendingDao = trendingDao,
            guide = guideReader,
        ) {
            override suspend fun load(profileId: Long, liveOnly: Boolean): HomeFeed {
                beforeFeed()
                return HomeFeed(heroItems = heroItems, recentLive = listOf(channelA, channelB, channelC))
            }
        }
        val metadataRepository = MetadataRepository(
            provider = metadataProvider,
            dao = metadataDao,
            settings = settings,
            overrideStore = MetadataOverrideStore(context),
            profileDao = profileDao,
        )

        return HomeViewModel(
            feed = feedReader,
            movieDao = movieDao,
            seriesDao = seriesDao,
            channelDao = channelDao,
            categoryDao = categoryDao,
            sourceDao = sourceDao,
            settings = settings,
            profileDao = profileDao,
            heroPreviewEngine = engine,
            historyDao = historyDao,
            progressDao = progressDao,
            metadata = metadataRepository,
            trendingDao = trendingDao,
            streamUrlResolver = streamUrlResolver,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> proxyDao(
        crossinline handler: (java.lang.reflect.Method, Array<out Any?>?) -> Any? = { _, _ -> null },
    ): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "Fake${T::class.java.simpleName}"
            else -> {
                val custom = handler(method, args)
                if (custom != null) {
                    custom
                } else {
                    when (method.returnType) {
                        Flow::class.java -> emptyFlow<Any?>()
                        List::class.java -> emptyList<Any?>()
                        Map::class.java -> emptyMap<Any?, Any?>()
                        Set::class.java -> emptySet<Any?>()
                        java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> false
                        java.lang.Integer.TYPE, java.lang.Integer::class.java -> 0
                        java.lang.Long.TYPE, java.lang.Long::class.java -> 0L
                        else -> null
                    }
                }
            }
        }
    } as T

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = LinkedHashMap<String, Any?>()
        private val listeners = LinkedHashSet<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = HashMap(values)
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.add(listener)
        }
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.remove(listener)
        }

        private inner class EditorImpl : SharedPreferences.Editor {
            private val staged = LinkedHashMap<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
                apply { if (key != null) staged[key] = values?.toSet() }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { if (key != null) staged[key] = null }
            override fun clear(): SharedPreferences.Editor = apply { clearAll = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) values.clear()
                for ((k, v) in staged) {
                    if (v == null) values.remove(k) else values[k] = v
                }
                for (k in staged.keys) {
                    for (l in listeners.toList()) {
                        l.onSharedPreferenceChanged(this@InMemorySharedPreferences, k)
                    }
                }
            }
        }
    }
}
