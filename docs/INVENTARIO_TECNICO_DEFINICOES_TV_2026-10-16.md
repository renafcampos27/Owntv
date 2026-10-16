# Anexo técnico — definições OwnTV TV, 16/10/2026

Este anexo apresenta o contrato atual do código. Revisão de limpeza: brilho ambiente, política de animações e meteorologia removidos; apenas nomes de campos antigos permanecem na compatibilidade de backup. Não é uma exportação da box e não contém valores pessoais guardados. Campos de estado (último backup, migrações, IDs, contadores), opções legadas e opções retiradas da UI estão incluídos para que não sejam confundidos com opções ativas. «Remover da UI» deve ser decidido no inventário principal; remoção de API/dados exige análise do consumidor.

## Propriedades globais de SettingsRepository

Foram encontradas **154 propriedades públicas reativas**. Algumas são valores compostos/derivados ou estado, não botões independentes. As expressões seguintes incluem os defaults e a normalização atuais. Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/settings/SettingsRepository.kt`.

### `lastBackup` — linha 603

```kotlin
val lastBackup: Flow<LastBackup?> = prefsFlow { prefs ->
        prefs[Keys.LAST_BACKUP_AT]?.takeIf { it > 0 }?.let { at ->
            LastBackup(
                at = at,
                bytes = prefs[Keys.LAST_BACKUP_BYTES] ?: 0L,
                encrypted = prefs[Keys.LAST_BACKUP_ENCRYPTED] ?: false,
                // Blank for a backup written before the path was recorded — the date is still true,
                // so the row is simply left out rather than the whole record thrown away.
                path = prefs[Keys.LAST_BACKUP_PATH].orEmpty(),
            )
        }
    }
```

### `lastLiveChannelId` — linha 627

```kotlin
val lastLiveChannelId: Flow<Long> = prefsFlow { it[Keys.LAST_LIVE_CHANNEL] ?: -1L }
```

### `resumeLastChannel` — linha 702

```kotlin
val resumeLastChannel: Flow<Boolean> = prefsFlow { it[Keys.RESUME_LAST_CHANNEL] ?: false }
```

### `lastLiveCategory` — linha 709

```kotlin
val lastLiveCategory: Flow<String> = prefsFlow { it[Keys.LAST_LIVE_CATEGORY] ?: "" }
```

### `lastMoviesCategory` — linha 713

```kotlin
val lastMoviesCategory: Flow<String> = prefsFlow { it[Keys.LAST_MOVIES_CATEGORY] ?: "" }
```

### `lastSeriesCategory` — linha 717

```kotlin
val lastSeriesCategory: Flow<String> = prefsFlow { it[Keys.LAST_SERIES_CATEGORY] ?: "" }
```

### `rememberCategoryLive` — linha 724

```kotlin
val rememberCategoryLive: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_CAT_LIVE] ?: true }
```

### `rememberCategoryMovies` — linha 728

```kotlin
val rememberCategoryMovies: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_CAT_MOVIES] ?: true }
```

### `rememberCategorySeries` — linha 732

```kotlin
val rememberCategorySeries: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_CAT_SERIES] ?: true }
```

### `rememberLastLive` — linha 740

```kotlin
val rememberLastLive: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_LAST_LIVE] ?: false }
```

### `rememberLastMovies` — linha 744

```kotlin
val rememberLastMovies: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_LAST_MOVIES] ?: false }
```

### `rememberLastSeries` — linha 748

```kotlin
val rememberLastSeries: Flow<Boolean> = prefsFlow { it[Keys.REMEMBER_LAST_SERIES] ?: false }
```

### `recentSearches` — linha 755

```kotlin
val recentSearches: Flow<List<String>> = prefsFlow { prefs ->
        prefs[Keys.RECENT_SEARCHES]?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
```


### `visualProfile` — linha 774

```kotlin
val visualProfile: Flow<tv.own.owntv.core.theme.VisualProfile> = prefsFlow { prefs ->
        tv.own.owntv.core.theme.VisualProfile.entries.firstOrNull { it.name == prefs[Keys.VISUAL_PROFILE] }
            ?: tv.own.owntv.core.theme.VisualProfile.AUTO
    }
```


### `uiDiagnostics` — linha 781

```kotlin
val uiDiagnostics: Flow<Boolean> = prefsFlow { it[Keys.UI_DIAGNOSTICS] ?: false }
```






### `metadataMode` — linha 794

```kotlin
val metadataMode: Flow<tv.own.owntv.core.metadata.MetadataMode> = prefsFlow { p ->
        parseMetadataMode(p)
    }
```

### `tmdbApiKey` — linha 815

```kotlin
val tmdbApiKey: Flow<String> = prefsFlow { it[Keys.TMDB_API_KEY] ?: "" }
```

### `metadataServerUrl` — linha 822

```kotlin
val metadataServerUrl: Flow<String> = prefsFlow { it[Keys.METADATA_SERVER_URL] ?: "" }
```

### `openSubtitlesApiKey` — linha 828

```kotlin
val openSubtitlesApiKey: Flow<String> = prefsFlow { it[Keys.OPEN_SUBTITLES_API_KEY] ?: "" }
```

### `openSubtitlesServerUrl` — linha 831

```kotlin
val openSubtitlesServerUrl: Flow<String> = prefsFlow { it[Keys.OPEN_SUBTITLES_SERVER_URL] ?: "" }
```

### `metadataLanguage` — linha 842

```kotlin
val metadataLanguage: Flow<String> = prefsFlow { it[Keys.METADATA_LANGUAGE] ?: "" }
```

### `metadataConfigFlow` — linha 857

```kotlin
val metadataConfigFlow: Flow<tv.own.owntv.core.metadata.MetadataConfig> = prefsFlow { p ->
        tv.own.owntv.core.metadata.MetadataConfig(
            mode = parseMetadataMode(p),
            tmdbApiKey = p[Keys.TMDB_API_KEY] ?: "",
            customServerUrl = p[Keys.METADATA_SERVER_URL] ?: "",
            language = p[Keys.METADATA_LANGUAGE] ?: "",
        )
    }
```

### `catchupTimezone` — linha 901

```kotlin
val catchupTimezone: Flow<CatchupTimezone> = prefsFlow { prefs ->
        prefs[Keys.CATCHUP_TZ]?.let { runCatching { CatchupTimezone.valueOf(it) }.getOrNull() } ?: CatchupTimezone.DEVICE
    }
```

### `catchupOffsetMinutes` — linha 906

```kotlin
val catchupOffsetMinutes: Flow<Int> = prefsFlow { it[Keys.CATCHUP_OFFSET_MIN] ?: 0 }
```

### `catchupPlayer` — linha 917

```kotlin
val catchupPlayer: Flow<CatchupPlayer> = prefsFlow { prefs ->
        prefs[Keys.CATCHUP_PLAYER]?.let { runCatching { CatchupPlayer.valueOf(it) }.getOrNull() } ?: CatchupPlayer.INTERNAL
    }
```

### `catchupFormat` — linha 921

```kotlin
val catchupFormat: Flow<tv.own.owntv.core.epg.CatchupFormat> = prefsFlow { prefs ->
        prefs[Keys.CATCHUP_FORMAT]?.let { runCatching { tv.own.owntv.core.epg.CatchupFormat.valueOf(it) }.getOrNull() }
            ?: tv.own.owntv.core.epg.CatchupFormat.AUTO
    }
```

### `catchupRequestShiftMinutes` — linha 939

```kotlin
val catchupRequestShiftMinutes: Flow<Int> = prefsFlow {
        (it[Keys.CATCHUP_REQUEST_SHIFT_MIN] ?: 60).coerceIn(catchupOffsetRangeMinutes)
    }
```

### `epgOffsetMinutes` — linha 964

```kotlin
val epgOffsetMinutes: Flow<Int> = prefsFlow { it[Keys.EPG_OFFSET_MIN] ?: 0 }
```

### `guideDaysToKeep` — linha 978

```kotlin
val guideDaysToKeep: Flow<Int> = prefsFlow {
        (it[Keys.GUIDE_DAYS_TO_KEEP] ?: GuideRetention.DEFAULT_DAYS)
            .coerceIn(GuideRetention.MIN_DAYS, GuideRetention.MAX_DAYS)
    }
```

### `updateCheckOnStart` — linha 998

```kotlin
val updateCheckOnStart: Flow<Boolean> = prefsFlow { it[Keys.UPDATE_CHECK_ON_START] ?: true }
```

### `localTimeshiftResumeMode` — linha 1008

```kotlin
val localTimeshiftResumeMode: Flow<ResumeMode> = prefsFlow { prefs ->
        prefs[Keys.LOCAL_TIMESHIFT_RESUME_MODE]?.let { runCatching { ResumeMode.valueOf(it) }.getOrNull() } ?: ResumeMode.ASK
    }
```

### `resumeMode` — linha 1016

```kotlin
val resumeMode: Flow<ResumeMode> = prefsFlow { prefs ->
        prefs[Keys.RESUME_MODE]?.let { runCatching { ResumeMode.valueOf(it) }.getOrNull() } ?: ResumeMode.ASK
    }
```

### `navMenuMode` — linha 1033

```kotlin
val navMenuMode: Flow<NavMenuMode> = prefsFlow { prefs ->
        prefs[Keys.NAV_MENU_MODE]?.let { runCatching { NavMenuMode.valueOf(it) }.getOrNull() } ?: NavMenuMode.STATIC
    }
```

### `navMenuHidden` — linha 1042

```kotlin
val navMenuHidden: Flow<Set<String>> = prefsFlow { it[Keys.NAV_MENU_HIDDEN] ?: emptySet() }
```

### `liveTvOnlyMode` — linha 1053

```kotlin
val liveTvOnlyMode: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
```

### `sortLive` — linha 1067

```kotlin
val sortLive: Flow<SortMode> = prefsFlow { parseSort(it[Keys.SORT_LIVE], SortMode.PLAYLIST) }
```

### `sortMovies` — linha 1068

```kotlin
val sortMovies: Flow<SortMode> = prefsFlow { parseSort(it[Keys.SORT_MOVIES], SortMode.PLAYLIST) }
```

### `sortSeries` — linha 1069

```kotlin
val sortSeries: Flow<SortMode> = prefsFlow { parseSort(it[Keys.SORT_SERIES], SortMode.PLAYLIST) }
```

### `guideView` — linha 1096

```kotlin
val guideView: Flow<GuideView?> = prefsFlow { prefs ->
        prefs[Keys.GUIDE_VIEW]?.let { runCatching { GuideView.valueOf(it) }.getOrNull() }
    }
```

### `guideDensityPct` — linha 1105

```kotlin
val guideDensityPct: Flow<Int> = prefsFlow { it[Keys.GUIDE_DENSITY_PCT] ?: 100 }
```

### `vodViewMode` — linha 1112

```kotlin
val vodViewMode: Flow<VodViewMode> = prefsFlow { prefs ->
        prefs[Keys.VOD_VIEW_MODE]?.let { runCatching { VodViewMode.valueOf(it) }.getOrNull() } ?: VodViewMode.GRID
    }
```

### `vodLayout` — linha 1127

```kotlin
val vodLayout: Flow<VodLayout> = prefsFlow { prefs ->
        prefs[Keys.VOD_LAYOUT]?.let { runCatching { VodLayout.valueOf(it) }.getOrNull() } ?: VodLayout.SEPARATE
    }
```

### `vodGridColumns` — linha 1140

```kotlin
val vodGridColumns: Flow<Int> = prefsFlow { it[Keys.VOD_GRID_COLUMNS] ?: 0 }
```

### `episodeViewMode` — linha 1151

```kotlin
val episodeViewMode: Flow<VodViewMode> = prefsFlow { prefs ->
        prefs[Keys.EPISODE_VIEW_MODE]?.let { runCatching { VodViewMode.valueOf(it) }.getOrNull() } ?: VodViewMode.LIST
    }
```

### `backgroundPlayback` — linha 1159

```kotlin
val backgroundPlayback: Flow<Boolean> = prefsFlow { it[Keys.BACKGROUND_PLAYBACK] ?: true }
```

### `pipEnabled` — linha 1165

```kotlin
val pipEnabled: Flow<Boolean> = prefsFlow { it[Keys.PIP_ENABLED] ?: true }
```

### `dataSaver` — linha 1176

```kotlin
val dataSaver: Flow<Boolean> = prefsFlow { it[Keys.DATA_SAVER] ?: false }
```

### `gestureSensitivityPct` — linha 1182

```kotlin
val gestureSensitivityPct: Flow<Int> = prefsFlow { it[Keys.GESTURE_SENSITIVITY_PCT] ?: 100 }
```

### `downloadsWifiOnly` — linha 1188

```kotlin
val downloadsWifiOnly: Flow<Boolean> = prefsFlow { it[Keys.DOWNLOADS_WIFI_ONLY] ?: false }
```

### `miniPlayerStyle` — linha 1203

```kotlin
val miniPlayerStyle: Flow<MiniPlayerStyle> = prefsFlow { prefs ->
        prefs[Keys.MINI_PLAYER_STYLE]?.let { runCatching { MiniPlayerStyle.valueOf(it) }.getOrNull() }
            ?: MiniPlayerStyle.FLOATING
    }
```

### `pipOnBack` — linha 1216

```kotlin
val pipOnBack: Flow<Boolean> = prefsFlow { it[Keys.PIP_ON_BACK] ?: false }
```

### `pipSize` — linha 1224

```kotlin
val pipSize: Flow<PipSize> = prefsFlow { prefs ->
        prefs[Keys.PIP_SIZE]?.let { runCatching { PipSize.valueOf(it) }.getOrNull() } ?: PipSize.MEDIUM
    }
```

### `pipSnap` — linha 1233

```kotlin
val pipSnap: Flow<Boolean> = prefsFlow { it[Keys.PIP_SNAP] ?: true }
```

### `audioOnScreenOff` — linha 1243

```kotlin
val audioOnScreenOff: Flow<Boolean> = prefsFlow { it[Keys.AUDIO_ON_SCREEN_OFF] ?: true }
```

### `audioOnMobileData` — linha 1250

```kotlin
val audioOnMobileData: Flow<Boolean> = prefsFlow { it[Keys.AUDIO_ON_MOBILE_DATA] ?: false }
```

### `audioPerChannel` — linha 1257

```kotlin
val audioPerChannel: Flow<Boolean> = prefsFlow { it[Keys.AUDIO_PER_CHANNEL] ?: true }
```

### `sortGuide` — linha 1266

```kotlin
val sortGuide: Flow<GuideSort> = prefsFlow { prefs ->
        prefs[Keys.SORT_GUIDE]?.let { runCatching { GuideSort.valueOf(it) }.getOrNull() } ?: GuideSort.LIVE_TV
    }
```

### `guideReplayOnly` — linha 1274

```kotlin
val guideReplayOnly: Flow<Boolean> = prefsFlow { it[Keys.GUIDE_REPLAY_ONLY] ?: true }
```

### `decoderQueueing` — linha 1283

```kotlin
val decoderQueueing: Flow<DecoderQueueing> = prefsFlow { DecoderQueueing.fromStored(it[Keys.DECODER_QUEUEING]) }
```

### `hwDecoding` — linha 1288

```kotlin
val hwDecoding: Flow<Boolean> = prefsFlow { it[Keys.HW_DECODING] ?: true }
```

### `softwareAudio` — linha 1289

```kotlin
val softwareAudio: Flow<Boolean> = prefsFlow { it[Keys.SOFTWARE_AUDIO] ?: false }
```

### `liveEnginePreference` — linha 1313

```kotlin
val liveEnginePreference: Flow<tv.own.owntv.core.player.EnginePreference> = prefsFlow { prefs ->
        prefs[Keys.LIVE_ENGINE]?.let { runCatching { tv.own.owntv.core.player.EnginePreference.valueOf(it) }.getOrNull() }
            ?: tv.own.owntv.core.player.EnginePreference.EXO_FIRST
    }
```

### `vodEnginePreference` — linha 1341

```kotlin
val vodEnginePreference: Flow<tv.own.owntv.core.player.EnginePreference> = prefsFlow { prefs ->
        prefs[Keys.VOD_ENGINE]?.let { runCatching { tv.own.owntv.core.player.EnginePreference.valueOf(it) }.getOrNull() }
            ?: if (prefs[Keys.VOD_PREFER_EXO] == true) tv.own.owntv.core.player.EnginePreference.EXO_FIRST
            else tv.own.owntv.core.player.EnginePreference.MPV_FIRST
    }
```

### `measuredStreamStats` — linha 1355

```kotlin
val measuredStreamStats: Flow<Boolean> = prefsFlow { it[Keys.MEASURED_STREAM_STATS] ?: true }
```

### `archivePlaybackConfigs` — linha 1362

```kotlin
val archivePlaybackConfigs: Flow<List<tv.own.owntv.core.epg.ArchivePlaybackConfig>> = prefsFlow {
        tv.own.owntv.core.epg.ArchivePlaybackConfig.decode(it[Keys.ARCHIVE_PLAYBACK_CONFIGS])
    }
```

### `channelPlaybackConfigs` — linha 1380

```kotlin
val channelPlaybackConfigs: Flow<List<ChannelPlaybackConfig>> = prefsFlow {
        ChannelPlaybackConfig.includeLegacy(ChannelPlaybackConfig.decode(it[Keys.CHANNEL_PLAYBACK_CONFIGS]), ManualTsChannel.decode(it[Keys.LIVE_MANUAL_TS_CHANNELS]))
    }
```

### `manualTsChannels` — linha 1422

```kotlin
val manualTsChannels: Flow<List<ManualTsChannel>> = prefsFlow { ManualTsChannel.decode(it[Keys.LIVE_MANUAL_TS_CHANNELS]) }
```

### `liveHlsOnly` — linha 1440

```kotlin
val liveHlsOnly: Flow<Boolean> = prefsFlow { it[Keys.LIVE_HLS_ONLY] ?: true }
```

### `detailedDiagnostics` — linha 1449

```kotlin
val detailedDiagnostics: Flow<Boolean> = prefsFlow { it[Keys.DETAILED_DIAGNOSTICS] ?: false }
```

### `directTune` — linha 1458

```kotlin
val directTune: Flow<Boolean> = prefsFlow { it[Keys.DIRECT_TUNE] ?: true }
```

### `externalPlayerMovies` — linha 1474

```kotlin
val externalPlayerMovies: Flow<Boolean> = prefsFlow { it[Keys.EXTERNAL_PLAYER_MOVIES] ?: it[Keys.EXTERNAL_PLAYER] ?: false }
```

### `externalPlayerSeries` — linha 1475

```kotlin
val externalPlayerSeries: Flow<Boolean> = prefsFlow { it[Keys.EXTERNAL_PLAYER_SERIES] ?: it[Keys.EXTERNAL_PLAYER] ?: false }
```

### `externalPlayerLive` — linha 1476

```kotlin
val externalPlayerLive: Flow<Boolean> = prefsFlow { it[Keys.EXTERNAL_PLAYER_LIVE] ?: false }
```

### `surroundSound` — linha 1510

```kotlin
val surroundSound: Flow<Boolean> = prefsFlow { it[Keys.SURROUND_SOUND] ?: false }
```

### `surroundMode` — linha 1527

```kotlin
val surroundMode: Flow<SurroundMode> = prefsFlow {
        SurroundMode.of(it[Keys.SURROUND_MODE], it[Keys.SURROUND_SOUND])
    }
```

### `autoPlayNext` — linha 1540

```kotlin
val autoPlayNext: Flow<Boolean> = prefsFlow { it[Keys.AUTO_PLAY_NEXT] ?: true }
```

### `defaultZoom` — linha 1547

```kotlin
val defaultZoom: Flow<String> = prefsFlow { it[Keys.DEFAULT_ZOOM] ?: "FIT" }
```

### `defaultVolume` — linha 1558

```kotlin
val defaultVolume: Flow<Int> = prefsFlow { (it[Keys.DEFAULT_VOLUME] ?: 100).coerceIn(0, 150) }
```

### `seekStepSec` — linha 1571

```kotlin
val seekStepSec: Flow<Int> = prefsFlow { it[Keys.SEEK_STEP_SEC] ?: SeekSteps.DEFAULT_SEEK_STEP_SEC }
```

### `deinterlace` — linha 1586

```kotlin
val deinterlace: Flow<Boolean> = prefsFlow { it[Keys.DEINTERLACE] ?: false }
```

### `liveRewindStepSec` — linha 1593

```kotlin
val liveRewindStepSec: Flow<Int> = prefsFlow { it[Keys.LIVE_REWIND_STEP_SEC] ?: SeekSteps.DEFAULT_LIVE_REWIND_STEP_SEC }
```

### `subtitleScaleMpv` — linha 1612

```kotlin
val subtitleScaleMpv: Flow<Float> = prefsFlow {
        it[Keys.SUB_SCALE_MPV] ?: it[Keys.SUB_SCALE] ?: SubtitleStyle.SCALE_DEFAULT
    }
```

### `subtitleScaleExo` — linha 1621

```kotlin
val subtitleScaleExo: Flow<Float> = prefsFlow {
        it[Keys.SUB_SCALE_EXO] ?: it[Keys.SUB_SCALE] ?: SubtitleStyle.SCALE_DEFAULT
    }
```

### `subtitleStyleEnabled` — linha 1636

```kotlin
val subtitleStyleEnabled: Flow<Boolean> = prefsFlow { prefs ->
        prefs[Keys.SUB_STYLE_ENABLED] ?: SubtitleStyle.hasScale(prefs[Keys.SUB_SCALE] ?: SubtitleStyle.SCALE_DEFAULT)
    }
```

### `subtitleFont` — linha 1645

```kotlin
val subtitleFont: Flow<AppFontFamily?> = prefsFlow { prefs ->
        prefs[Keys.SUB_FONT]?.let { stored -> AppFontFamily.entries.firstOrNull { it.name == stored } }
    }
```

### `subtitleColor` — linha 1656

```kotlin
val subtitleColor: Flow<String> = prefsFlow { it[Keys.SUB_COLOR] ?: SubtitleStyle.COLOR_DEFAULT }
```

### `subtitlePosition` — linha 1663

```kotlin
val subtitlePosition: Flow<SubtitleStyle.Position> =
        prefsFlow { SubtitleStyle.Position.fromKey(it[Keys.SUB_POSITION]) }
```

### `subtitleBgOpacity` — linha 1671

```kotlin
val subtitleBgOpacity: Flow<Int> = prefsFlow { it[Keys.SUB_BG_OPACITY] ?: SubtitleStyle.OPACITY_DEFAULT }
```

### `audioDelayMs` — linha 1679

```kotlin
val audioDelayMs: Flow<Int> = prefsFlow { it[Keys.AUDIO_DELAY_MS] ?: 0 }
```

### `chNavEnabled` — linha 1687

```kotlin
val chNavEnabled: Flow<Boolean> = prefsFlow { it[Keys.CH_NAV_ENABLED] ?: true }
```

### `chNavUpSkip` — linha 1692

```kotlin
val chNavUpSkip: Flow<Int> = prefsFlow {
        (it[Keys.CH_NAV_UP_SKIP] ?: ChNavLimits.DEFAULT_SKIP).coerceIn(1, ChNavLimits.HARD_MAX)
    }
```

### `chNavDownSkip` — linha 1699

```kotlin
val chNavDownSkip: Flow<Int> = prefsFlow {
        (it[Keys.CH_NAV_DOWN_SKIP] ?: ChNavLimits.DEFAULT_SKIP).coerceIn(1, ChNavLimits.HARD_MAX)
    }
```

### `remoteShortcutBindings` — linha 1707

```kotlin
val remoteShortcutBindings: Flow<List<RemoteShortcutBinding>> = prefsFlow { prefs ->
        if (Keys.REMOTE_SHORTCUT_BINDINGS in prefs) {
            RemoteShortcutBindings.decode(prefs[Keys.REMOTE_SHORTCUT_BINDINGS].orEmpty())
        } else {
            RemoteShortcutBindings.defaults
        }
    }
```

### `guideWidthEnabled` — linha 1831

```kotlin
val guideWidthEnabled: Flow<Boolean> = prefsFlow { it[Keys.GUIDE_WIDTH_ON] ?: false }
```

### `guideWidthShares` — linha 1833

```kotlin
val guideWidthShares: Flow<GuideWidthShares?> = prefsFlow { prefs ->
        val channels = prefs[Keys.GUIDE_WIDTH_CHANNELS]
        val epg = prefs[Keys.GUIDE_WIDTH_EPG]
        if (channels == null || epg == null) null
        else normalizeGuideWidths(GuideWidthShares(channels, epg))
    }
```

### `preferredAudioLang` — linha 1850

```kotlin
val preferredAudioLang: Flow<String> = prefsFlow { it[Keys.PREF_AUDIO_LANG] ?: "" }
```

### `preferredSubLang` — linha 1857

```kotlin
val preferredSubLang: Flow<String> = prefsFlow { it[Keys.PREF_SUB_LANG] ?: "" }
```

### `subSearchFilterEnabled` — linha 1870

```kotlin
val subSearchFilterEnabled: Flow<Boolean> = prefsFlow { it[Keys.SUB_SEARCH_FILTER] ?: false }
```

### `subSearchLanguages` — linha 1877

```kotlin
val subSearchLanguages: Flow<String> = prefsFlow { it[Keys.SUB_SEARCH_LANGS] ?: "" }
```

### `quickPinnedKeys` — linha 1888

```kotlin
val quickPinnedKeys: Flow<List<String>> = prefsFlow { prefs ->
        (prefs[Keys.QUICK_PINNED] ?: DEFAULT_QUICK_PINNED.joinToString(","))
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
```

### `playlistAutoRefresh` — linha 1920

```kotlin
val playlistAutoRefresh: Flow<Map<Long, PlaylistRefresh>> =
        prefsFlow { prefs ->
            readRefreshMap(prefs[Keys.PLAYLIST_AUTO_REFRESH]).entries.mapNotNull { (key, value) ->
                key.toLongOrNull()?.let { it to PlaylistRefresh.parse(value) }
            }.toMap()
        }
```

### `epgAutoRefresh` — linha 1928

```kotlin
val epgAutoRefresh: Flow<Map<Long, EpgRefresh>> =
        prefsFlow { prefs ->
            readRefreshMap(prefs[Keys.EPG_AUTO_REFRESH]).entries.mapNotNull { (key, value) ->
                key.toLongOrNull()?.let { it to EpgRefresh.parse(value) }
            }.toMap()
        }
```

### `epgUseLogos` — linha 1939

```kotlin
val epgUseLogos: Flow<Set<Long>> = prefsFlow { prefs ->
        parseRefreshMap(prefs[Keys.EPG_USE_LOGOS])
            .filterValues { it.toBoolean() }
            .keys.mapNotNullTo(LinkedHashSet()) { it.toLongOrNull() }
    }
```

### `epgRefillChecked` — linha 1993

```kotlin
val epgRefillChecked: Flow<Boolean> = prefsFlow { it[Keys.EPG_REFILL_CHECKED] == true }
```

### `restoreInProgress` — linha 2007

```kotlin
val restoreInProgress: Flow<String?> = prefsFlow { it[Keys.RESTORE_IN_PROGRESS] }
```

### `liveGuideDelayMs` — linha 2050

```kotlin
val liveGuideDelayMs: Flow<Int> = prefsFlow { if (it[Keys.LIVE_GUIDE_DELAY_MS] == 0) 0 else 3_000 }
```

### `livePreviewEnabled` — linha 2057

```kotlin
val livePreviewEnabled: Flow<Boolean> = prefsFlow { it[Keys.LIVE_PREVIEW] ?: true }
```

### `livePreviewPanelActive` — linha 2060

```kotlin
val livePreviewPanelActive: Flow<Boolean> = prefsFlow { !livePreviewPanelHidden(it) }
```

### `heroPreviewEnabled` — linha 2074

```kotlin
val heroPreviewEnabled: Flow<Boolean> = prefsFlow { it[Keys.HERO_PREVIEW] ?: heroPreviewDefault }
```

### `livePreviewAudio` — linha 2087

```kotlin
val livePreviewAudio: Flow<Boolean> = prefsFlow { it[Keys.LIVE_PREVIEW_AUDIO] ?: false }
```

### `hdrEnabled` — linha 2094

```kotlin
val hdrEnabled: Flow<Boolean> = prefsFlow { it[Keys.HDR_ENABLED] ?: false }
```

### `multiviewEnabled` — linha 2104

```kotlin
val multiviewEnabled: Flow<Boolean> = prefsFlow { it[Keys.MULTIVIEW_ENABLED] ?: false }
```

### `multiviewTiles` — linha 2114

```kotlin
val multiviewTiles: Flow<Int> = prefsFlow {
        (it[Keys.MULTIVIEW_TILES] ?: DEFAULT_MULTIVIEW_TILES).coerceIn(MIN_MULTIVIEW_TILES, MAX_MULTIVIEW_TILES)
    }
```

### `recordingReserveConnection` — linha 2134

```kotlin
val recordingReserveConnection: Flow<Boolean> = prefsFlow {
        it[Keys.RECORDING_RESERVE_CONNECTION] ?: true
    }
```

### `recordingPreRollMinutes` — linha 2152

```kotlin
val recordingPreRollMinutes: Flow<Int> = prefsFlow {
        (it[Keys.RECORDING_PRE_ROLL_MINUTES] ?: RecordingSchedule.DEFAULT_PRE_ROLL_MINUTES)
            .coerceIn(0, RecordingSchedule.MAX_ROLL_MINUTES)
    }
```

### `recordingPostRollMinutes` — linha 2163

```kotlin
val recordingPostRollMinutes: Flow<Int> = prefsFlow {
        (it[Keys.RECORDING_POST_ROLL_MINUTES] ?: RecordingSchedule.DEFAULT_POST_ROLL_MINUTES)
            .coerceIn(0, RecordingSchedule.MAX_ROLL_MINUTES)
    }
```

### `recordingOverMobileData` — linha 2188

```kotlin
val recordingOverMobileData: Flow<Boolean> = prefsFlow {
        it[Keys.RECORDING_OVER_MOBILE_DATA] ?: true
    }
```

### `recordWhatImWatching` — linha 2205

```kotlin
val recordWhatImWatching: Flow<Boolean> = prefsFlow { it[Keys.RECORD_WHAT_IM_WATCHING] ?: false }
```

### `multiviewWarningAccepted` — linha 2212

```kotlin
val multiviewWarningAccepted: Flow<Boolean> = prefsFlow { it[Keys.MULTIVIEW_WARNING_ACCEPTED] ?: false }
```

### `autoFrameRate` — linha 2223

```kotlin
val autoFrameRate: Flow<Boolean> = prefsFlow { prefs ->
        // Also report Off before the startup migration coroutines complete, so an auto-resumed channel
        // cannot briefly request a display-mode switch on the first launch after either safety reset.
        val migrated = prefs[Keys.AUTO_FRAME_RATE_RESET_416] == true &&
            prefs[Keys.AUTO_FRAME_RATE_RESET_PRE12] == true
        if (migrated) prefs[Keys.AUTO_FRAME_RATE] ?: false else false
    }
```

### `autoFrameRatePrompted` — linha 2239

```kotlin
val autoFrameRatePrompted: Flow<Boolean> = prefsFlow { it[Keys.AUTO_FRAME_RATE_PROMPTED] ?: false }
```

### `afrPauseSecs` — linha 2260

```kotlin
val afrPauseSecs: Flow<Int> = prefsFlow { (it[Keys.AFR_PAUSE_SECS] ?: 0).coerceIn(0, afrPauseMaxSecs) }
```

### `afrMatchResolution` — linha 2268

```kotlin
val afrMatchResolution: Flow<Boolean> = prefsFlow { it[Keys.AFR_MATCH_RESOLUTION] ?: false }
```

### `androidTvHomeEnabled` — linha 2317

```kotlin
val androidTvHomeEnabled: Flow<Boolean> =
        if (!CoreBuildInfo.tvHome) flowOf(false) else prefsFlow { it[Keys.ANDROID_TV_HOME] ?: true }
```

### `defaultSourceId` — linha 2325

```kotlin
val defaultSourceId: Flow<Long> = prefsFlow { it[Keys.DEFAULT_SOURCE] ?: -1L }
```

### `internalMediaQuotaGiB` — linha 2332

```kotlin
val internalMediaQuotaGiB: Flow<Int> = prefsFlow { (it[Keys.INTERNAL_MEDIA_QUOTA] ?: 4).coerceIn(1, 32) }
```

### `externalMediaQuotaGiB` — linha 2333

```kotlin
val externalMediaQuotaGiB: Flow<Int> = prefsFlow { (it[Keys.EXTERNAL_MEDIA_QUOTA] ?: 16).coerceIn(1, 256) }
```

### `downloadRoot` — linha 2337

```kotlin
val downloadRoot: Flow<String> = prefsFlow { it[Keys.DOWNLOAD_ROOT] ?: "" }
```

### `themeMode` — linha 2343

```kotlin
val themeMode: Flow<ThemeMode> = prefsFlow { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.DARK
    }
```

### `uiZoomPercent` — linha 2352

```kotlin
val uiZoomPercent: Flow<Int> = prefsFlow { prefs ->
        UiZoom.clamp(prefs[Keys.UI_ZOOM_PCT] ?: UiZoom.DEFAULT)
    }
```

### `fontCustomization` — linha 2360

```kotlin
val fontCustomization: Flow<FontCustomization> = prefsFlow { prefs ->
        FontCustomization(
            sizePercent = UiFontScale.clamp(prefs[Keys.FONT_SIZE_PCT] ?: UiFontScale.DEFAULT),
            mainFamily = AppFontFamily.fromStored(
                prefs[Keys.MAIN_FONT_FAMILY],
                AppFontFamily.SYSTEM_SANS,
            ),
            popupFamily = AppFontFamily.fromStored(
                prefs[Keys.POPUP_FONT_FAMILY],
                AppFontFamily.LORA,
            ),
            popupFontSizePercent = PopupFontScale.clamp(
                prefs[Keys.POPUP_FONT_SIZE_PCT] ?: PopupFontScale.DEFAULT,
            ),
            popupSizePercent = PopupSizeScale.clamp(
                prefs[Keys.POPUP_SIZE_PCT] ?: PopupSizeScale.DEFAULT,
            ),
        )
    }
```

### `miniPlayerSizePct` — linha 2392

```kotlin
val miniPlayerSizePct: Flow<Int> = prefsFlow { prefs ->
        tv.own.owntv.core.player.MiniPlayerSize.clamp(prefs[Keys.MINI_PLAYER_SIZE_PCT] ?: tv.own.owntv.core.player.MiniPlayerSize.DEFAULT)
    }
```

### `miniPlayerPosition` — linha 2401

```kotlin
val miniPlayerPosition: Flow<String> = prefsFlow { prefs ->
        prefs[Keys.MINI_PLAYER_POSITION] ?: tv.own.owntv.core.player.MiniPlayerPosition.DEFAULT.name
    }
```

### `liveLatencyMode` — linha 2415

```kotlin
val liveLatencyMode: Flow<String> = prefsFlow { prefs ->
        // Also report Balanced before the startup migration coroutine completes, so an auto-resumed
        // channel cannot briefly reuse an unsafe low/custom latency on the first 4.1.6 launch.
        effectiveLegacyLatencyMode(prefs)
    }
```

### `liveLatencyCustomSecs` — linha 2440

```kotlin
val liveLatencyCustomSecs: Flow<Int> = prefsFlow { prefs ->
        LiveBuffer.clampCustom(prefs[Keys.LIVE_LATENCY_CUSTOM_SECS] ?: LiveBuffer.CUSTOM_DEFAULT)
    }
```

### `liveReserveMode` — linha 2452

```kotlin
val liveReserveMode: Flow<String> = prefsFlow { prefs ->
        LiveReservePreferences.mode(prefs, effectiveLegacyLatencyMode(prefs))
    }
```

### `liveReserveCustomSecs` — linha 2456

```kotlin
val liveReserveCustomSecs: Flow<Int> = prefsFlow(LiveReservePreferences::customSecs)
```

### `liveReserveConfiguration` — linha 2458

```kotlin
val liveReserveConfiguration: Flow<LiveReserveConfiguration> = prefsFlow { prefs ->
        LiveReservePreferences.configuration(prefs, effectiveLegacyLatencyMode(prefs))
    }
```

### `liveReserveExtraSecs` — linha 2462

```kotlin
val liveReserveExtraSecs: Flow<Int> = liveReserveConfiguration.map { it.extraSecs }.distinctUntilChanged()
```

### `liveReserveSeconds` — linha 2464

```kotlin
val liveReserveSeconds: Flow<Int?> = liveReserveConfiguration.map { it.seconds }.distinctUntilChanged()
```

### `livePrerollSecs` — linha 2499

```kotlin
val livePrerollSecs: Flow<Int> = prefsFlow { prefs ->
        (prefs[Keys.LIVE_PREROLL_SECS] ?: LiveBuffer.PREROLL_OFF).coerceIn(0, 30)
    }
```

### `channelReopenIntervalSecs` — linha 2507

```kotlin
val channelReopenIntervalSecs: Flow<Int> = prefsFlow { prefs ->
        ChannelReopenInterval.normalize(prefs[Keys.CHANNEL_REOPEN_INTERVAL_SECS] ?: ChannelReopenInterval.DEFAULT_SECONDS)
    }
```

### `liveTuneTimeoutSecs` — linha 2524

```kotlin
val liveTuneTimeoutSecs: Flow<Int> = prefsFlow { prefs ->
        (prefs[Keys.LIVE_TUNE_TIMEOUT_SECS] ?: tv.own.owntv.core.player.PlaybackDefaults.LIVE_TUNE_BUDGET_SECS)
            .coerceIn(0, 60)
    }
```

### `liveBufferSeconds` — linha 2534

```kotlin
val liveBufferSeconds: Flow<Int?> = combine(liveLatencyMode, liveLatencyCustomSecs) { mode, custom ->
        LiveBuffer.effectiveSeconds(LiveLatency.fromName(mode), custom)
    }
```

### `accent` — linha 2538

```kotlin
val accent: Flow<AccentColor> = prefsFlow { prefs ->
        prefs[Keys.ACCENT]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
            ?: AccentColor.TEAL
    }
```

### `customAccent` — linha 2552

```kotlin
val customAccent: Flow<String> = prefsFlow { it[Keys.ACCENT_CUSTOM] ?: "" }
```

### `focusHighlight` — linha 2560

```kotlin
val focusHighlight: Flow<String> = prefsFlow { it[Keys.FOCUS_HIGHLIGHT] ?: "" }
```

### `focusHighlightWidth` — linha 2563

```kotlin
val focusHighlightWidth: Flow<Int> = prefsFlow { it[Keys.FOCUS_HIGHLIGHT_WIDTH] ?: 2 }
```

### `bgImagePath` — linha 2575

```kotlin
val bgImagePath: Flow<String> = prefsFlow { it[Keys.BG_IMAGE_PATH] ?: "" }
```

### `glassConfig` — linha 2578

```kotlin
val glassConfig: Flow<tv.own.owntv.core.theme.GlassConfig> = prefsFlow { p ->
        val bits = p[Keys.GLASS_SCOPE] ?: GLASS_SCOPE_DEFAULT_BITS
        val alphaPct = p[Keys.GLASS_ALPHA] ?: GLASS_ALPHA_DEFAULT_PCT
        val blurPct = p[Keys.GLASS_BLUR] ?: GLASS_BLUR_DEFAULT_PCT
        val highlightPct = p[Keys.GLASS_HIGHLIGHT] ?: GLASS_HIGHLIGHT_DEFAULT_PCT
        val allowFullTransparency = p[Keys.GLASS_ALLOW_FULL_TRANSPARENCY] ?: false
        val depthEffects = p[Keys.GLASS_DEPTH_EFFECTS] ?: true
        val glint = p[Keys.GLASS_GLINT] ?: true
        val preset = tv.own.owntv.core.theme.GlassPreset.fromStored(
            name = p[Keys.GLASS_PRESET],
            customAlpha = alphaPct / 100f,
            customBlur = blurPct / 100f,
        )
        tv.own.owntv.core.theme.GlassConfig.fromBitmask(
            bits,
            alpha = alphaPct / 100f,
            blurStrength = blurPct / 100f,
            preset = preset,
            highlightStrength = highlightPct / 100f,
            allowFullTransparency = allowFullTransparency,
            depthEffects = depthEffects,
            glint = glint,
        )
    }
```

### `avatarId` — linha 2655

```kotlin
val avatarId: Flow<Int> = prefsFlow { it[Keys.AVATAR_ID] ?: 0 }
```

### `activeProfileId` — linha 2662

```kotlin
val activeProfileId: Flow<Long> = prefsFlow { it[Keys.ACTIVE_PROFILE] ?: -1L }
```

### `proxyConfig` — linha 2674

```kotlin
val proxyConfig: Flow<tv.own.owntv.core.network.ProxyConfig> = prefsFlow { p ->
        tv.own.owntv.core.network.ProxyConfig(
            enabled = p[Keys.PROXY_ENABLED] ?: false,
            host = p[Keys.PROXY_HOST] ?: "",
            port = p[Keys.PROXY_PORT] ?: 0,
            username = p[Keys.PROXY_USER] ?: "",
            password = p[Keys.PROXY_PASS] ?: "",
        )
    }
```

### `dnsConfig` — linha 3212

```kotlin
val dnsConfig: Flow<tv.own.owntv.core.network.DnsConfig> = prefsFlow { p ->
        tv.own.owntv.core.network.DnsConfig(
            enabled = p[Keys.DNS_ENABLED] ?: false,
            host = p[Keys.DNS_HOST] ?: "",
            port = p[Keys.DNS_PORT] ?: 53,
            dohUrl = p[Keys.DNS_DOH_URL] ?: "",
        )
    }
```

## Configuração por perfil, layout e menu

```kotlin
fun startupMode(profileId: Long): Flow<StartupMode> = prefsFlow { prefs ->
        prefs[stringPreferencesKey("startup_mode_$profileId")]?.let { runCatching { StartupMode.valueOf(it) }.getOrNull() }
            ?: if (prefs[Keys.RESUME_LAST_CHANNEL] == true) StartupMode.LAST_CHANNEL else StartupMode.HOME
    }
```

```kotlin
fun startupChannel(profileId: Long): Flow<StartupChannelRef?> = prefsFlow { prefs ->
        StartupChannelRef.fromJson(prefs[stringPreferencesKey("startup_channel_$profileId")])
    }
```

```kotlin
fun customizePin(profileId: Long): Flow<String?> = prefsFlow { prefs ->
        prefs[stringPreferencesKey("customize_pin_$profileId")]?.takeIf { it.isNotBlank() }
    }
```

```kotlin
fun hideNewCategoriesDefault(profileId: Long): Flow<Boolean> = prefsFlow { prefs ->
        prefs[booleanPreferencesKey("hide_new_categories_$profileId")] ?: false
    }
```

```kotlin
fun homeConfig(profileId: Long): Flow<HomeConfig> = prefsFlow { prefs ->
        HomeConfig.fromJson(prefs[homeConfigKey(profileId)])
    }
```

```kotlin
fun externalPlayer(section: ExternalPlayerSection): Flow<Boolean> = when (section) {
        ExternalPlayerSection.LIVE_TV -> externalPlayerLive
        ExternalPlayerSection.MOVIES -> externalPlayerMovies
        ExternalPlayerSection.SERIES -> externalPlayerSeries
    }
```

```kotlin
fun externalPlayerFor(mediaType: tv.own.owntv.core.model.MediaType): Flow<Boolean> =
        when (mediaType) {
            tv.own.owntv.core.model.MediaType.LIVE -> externalPlayerLive
            tv.own.owntv.core.model.MediaType.SERIES -> externalPlayerSeries
            else -> externalPlayerMovies
        }
```

```kotlin
fun panelWidthEnabled(s: PanelSection): Flow<Boolean> = prefsFlow { it[panelOnKey(s)] ?: false }
```

```kotlin
fun panelShares(s: PanelSection): Flow<PanelShares?> = prefsFlow { p ->
        val category = p[panelCategoryKey(s)]
        val list = p[panelListKey(s)]
        val preview = p[panelPreviewKey(s)]
        if (category == null || list == null || preview == null || category <= 0 || list <= 0 || preview < 0) {
            null
        } else {
            balanceToTotal(
                PanelShares(
                    PanelWidthLimits.snap(category),
                    PanelWidthLimits.snap(list),
                    PanelWidthLimits.snapPreview(preview),
                ),
            )
        }
    }
```

```kotlin
fun cinematicDetailsHeight(s: PanelSection): Flow<Int> =
        prefsFlow { it[cinematicDetailsKey(s)]?.coerceIn(0, CINEMATIC_DETAILS_MAX) ?: CINEMATIC_DETAILS_DEFAULT }
```

```kotlin
fun menuOrder(menu: String): Flow<List<String>> = prefsFlow { prefs ->
        (prefs[menuOrderKey(menu)] ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
```

## Modelos e defaults adicionais

Campos sem `= ...` são obrigatórios: não têm default universal. `null` em opções por canal/lista significa normalmente herdar, não desligar. Campos da entidade de dados podem ter filtros adicionais da edição só TV; por exemplo, existirem flags syncMovies/syncSeries no schema não reativa esses catálogos.

### ProfileEntity

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/database/entity/ProfileEntities.kt`

```kotlin
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarColor: Int,
    /** Index into the shell's `OwnTVAvatars` cartoon set. */
    val avatarId: Int = 0,
    /**
     * A picture of the user's own, as an absolute path in app-private storage, or null for the drawn
     * tile named by [avatarId] (v37). Written by `ProfileAvatarStore`, which is the only thing that
     * puts files there.
     *
     * A path rather than the bytes: profiles are read on every screen that shows who is watching, and
     * carrying an image blob through those queries would cost something on every one of them.
     */
    val avatarPath: String? = null,
    val isKids: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
```

### HomeConfig

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/model/HomeRow.kt`

```kotlin
data class HomeConfig(
    val order: List<HomeRow> = HomeRow.entries.toList(),
    val hidden: Set<HomeRow> = setOf(HomeRow.RECENT_CHANNELS),
    val heroIncludeLive: Boolean = true,
    val heroIncludeMovies: Boolean = true,
    val heroIncludeSeries: Boolean = true,
    val recentLiveMode: HomeLiveRowMode = HomeLiveRowMode.CARDS,
    val favoriteLiveMode: HomeLiveRowMode = HomeLiveRowMode.ON_NOW,
    val trendingStyle: HomeTrendingStyle = HomeTrendingStyle.HERO,
)
```

### ChannelPlaybackOptions

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/settings/ChannelPlaybackConfig.kt`

```kotlin
data class ChannelPlaybackOptions(
    val engine: EnginePreference? = null,
    val format: ChannelStreamFormat? = null,
    val reserveSecs: Int? = null,
    val extraSecs: Int? = null,
    val prerollSecs: Int? = null,
    val latencySecs: Int? = null,
    val softwareAudio: Boolean? = null,
    val audioDelayMs: Int? = null,
    val hlsDetectAccessUnits: Boolean? = null,
    val hlsAllowNonIdrKeyframes: Boolean? = null,
    val hlsPrepareFromSegments: Boolean? = null,
)
```

### ArchivePlaybackConfig

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/epg/ArchivePlaybackConfig.kt`

```kotlin
data class ArchivePlaybackConfig(
    val sourceId: Long,
    val remoteId: String? = null,
    val channelName: String? = null,
    val templates: List<String>,
    val manifestType: String? = null,
)
```

### EpgSource

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/epg/EpgSourceStore.kt`

```kotlin
data class EpgSource(
    val id: Long,            // synthetic, always negative so it never collides with a Room source id
    val name: String,
    val url: String,
    val userAgent: String? = null,
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)
```

### FontCustomization

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/theme/FontCustomization.kt`

```kotlin
data class FontCustomization(
    val sizePercent: Int = UiFontScale.DEFAULT,
    val mainFamily: AppFontFamily = AppFontFamily.SYSTEM_SANS,
    val popupFamily: AppFontFamily = AppFontFamily.LORA,
    val popupFontSizePercent: Int = PopupFontScale.DEFAULT,
    val popupSizePercent: Int = PopupSizeScale.DEFAULT,
)
```

### ProxyConfig

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/ProxyConfig.kt`

```kotlin
data class ProxyConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 0,
    val username: String = "",
    val password: String = "",
)
```

### DnsConfig

Origem: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/DnsConfig.kt`

```kotlin
data class DnsConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = DNS_DEFAULT_PORT,
    val dohUrl: String = "",
)
```

### SimpleModePreferences.kt

```kotlin
data class SimpleModeOptions(val hideCategories: Boolean = true, val hideSidebar: Boolean = false, val channelRecovery: Boolean = true, val startOnBoot: Boolean = false, val startOnWake: Boolean = false, val recoveryTimeoutSeconds: Int = RecoveryTimeout.DEFAULT_SECONDS)
```

### RecoveryTimeout.kt

```kotlin
package tv.own.owntv.features.settings

internal object RecoveryTimeout {
    const val DEFAULT_SECONDS = 7
    val seconds = 1..60
    fun normalize(value: Int?): Int = (value ?: DEFAULT_SECONDS).coerceIn(seconds)
    fun milliseconds(value: Int): Long = normalize(value) * 1_000L
}

```

## Constantes/regras que completam os defaults

### settings/LiveLatency.kt

```kotlin
package tv.own.owntv.core.settings

/**
 * How close to the live edge to play Live TV, trading latency against stability. Applied on the next
 * channel open (live streams only — VOD is unaffected):
 *  - ExoPlayer live → a `MediaItem.LiveConfiguration` target offset (the main HLS latency lever);
 * The forward reserve is configured separately through the reserve preferences.
 *
 * [BALANCED] is the default and applies no override — the engines keep their existing behaviour, so
 * the manifest's live offset is retained.
 */
enum class LiveLatency {
    LOW,
    BALANCED,
    STABLE,
    CUSTOM;

    companion object {
        val DEFAULT = BALANCED
        fun fromName(name: String?): LiveLatency = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Shared legacy preset values; live offset and forward reserve are resolved independently. */
object LiveBuffer {
    const val LOW_SECS = 2
    const val STABLE_SECS = 15
    const val CUSTOM_MIN = 1
    const val CUSTOM_MAX = 60
    const val CUSTOM_DEFAULT = 8

    /** Below this many seconds counts as "lower than Balanced" — the low-latency warning threshold. */
    const val WARN_BELOW_SECS = CUSTOM_DEFAULT

    fun clampCustom(secs: Int): Int = secs.coerceIn(CUSTOM_MIN, CUSTOM_MAX)

    /** True when [secs] is a real, below-Balanced buffer worth warning the user about. */
    fun isLowLatency(secs: Int?): Boolean = secs != null && secs < WARN_BELOW_SECS

    /**
     * A per-playlist latency override, carrying an effective depth that is **itself** nullable —
     * Balanced means "engine defaults, no target offset". A plain `Int?` could not tell "this playlist
     * says Balanced" from "this playlist says nothing", which is the difference between overriding the
     * global setting and inheriting it.
     */
    @JvmInline
    value class Override(val secs: Int?)

    /** Resolve a preset depth/offset; null leaves the relevant engine default in place. */
    fun effectiveSeconds(mode: LiveLatency, customSecs: Int): Int? = when (mode) {
        LiveLatency.LOW -> LOW_SECS
        LiveLatency.STABLE -> STABLE_SECS
        LiveLatency.CUSTOM -> clampCustom(customSecs)
        LiveLatency.BALANCED -> null
    }

    /**
     * Balanced targets an 8–10 s forward reserve. The lower threshold requests more media; the upper
     * threshold stops read-ahead. Their difference is a media-duration range, not a guarantee about
     * socket idle time: segment availability, download speed and memory also affect loading.
     */
    const val BALANCED_SECS = 8
    const val DEFAULT_EXTRA_SECS = 2
    /** Upper loading threshold, preserving the previous 60 + 10 s extreme. */
    const val MAX_RESERVE_SECS = 70
    const val MAX_EXTRA_SECS = MAX_RESERVE_SECS - CUSTOM_MIN
    fun clampExtra(secs: Int): Int = secs.coerceIn(0, MAX_EXTRA_SECS)
    fun clampExtraForMinimum(secs: Int, minimumSecs: Int): Int =
        clampExtra(secs).coerceAtMost(MAX_RESERVE_SECS - clampCustom(minimumSecs))

    /** Initial preparation and underrun recovery deliberately use independent thresholds. */
    const val DEFAULT_START_MS = 1_500
    // After an underrun the renderer resumes as soon as it has playable samples.
    const val DEFAULT_RESTART_MS = 0

    /** Initial pre-buffer choices; 0 keeps the automatic initial threshold. */
    val PREROLL_CHOICES = listOf(0) + (2..10).toList()
    const val PREROLL_OFF = 0

    /** Resolved ExoPlayer `DefaultLoadControl` durations for a live tune. */
    data class LoadControlMs(
        val minBufferMs: Int,
        val maxBufferMs: Int,
        val bufferForPlaybackMs: Int,
        val bufferForPlaybackAfterRebufferMs: Int,
    )

    /**
     * Resolve the forward reserve ([bufferSecs], null = Balanced), initial pre-buffer and
     * refill range independently of the HLS live offset. Recovery never adds a pre-buffer wait.
     *
     * The pre-roll raises the buffer floor when it has to: `DefaultLoadControl` requires
     * `minBufferMs >= bufferForPlayback*`, and asking to buffer 10 s before starting means the buffer
     * must be allowed to hold 10 s in the first place — so a deep pre-roll wins over a shallow preset
     * rather than throwing.
     */
    fun loadControlFor(bufferSecs: Int?, prerollSecs: Int, extraSecs: Int = DEFAULT_EXTRA_SECS): LoadControlMs {
        val prerollMs = prerollSecs.coerceAtLeast(0) * 1000
        val startMs = if (prerollMs > 0) prerollMs else DEFAULT_START_MS
        val restartMs = DEFAULT_RESTART_MS
        val depthSecs = bufferSecs ?: BALANCED_SECS
        val minMs = (depthSecs * 1000).coerceAtLeast(maxOf(startMs, restartMs))
        val requestedMaxMs = (depthSecs + clampExtraForMinimum(extraSecs, depthSecs)) * 1000
        return LoadControlMs(
            minBufferMs = minMs,
            maxBufferMs = maxOf(minMs, requestedMaxMs),
            bufferForPlaybackMs = startMs,
            bufferForPlaybackAfterRebufferMs = restartMs,
        )
    }

    /**
     * Allocator byte target, scaled for deeper reserves and initial pre-buffer. This is a soft loading
     * threshold, not a hard cap on application RAM. High-bitrate streams can reach it before the time
     * target and may start with less buffered media. Bound the scaling more conservatively on low-RAM
     * devices; diagnostics expose the requested reserve, actual duration and allocator target.
     */
    fun targetBufferBytes(bufferSecs: Int?, prerollSecs: Int, defaultBytes: Int, lowSpec: Boolean = false): Int {
        val depthSecs = maxOf(bufferSecs ?: BALANCED_SECS, prerollSecs.coerceAtLeast(0))
        if (depthSecs <= BALANCED_SECS) return defaultBytes
        val scaled = defaultBytes.toLong() * depthSecs / BALANCED_SECS
        val maxMultiplier = if (lowSpec) 2 else 3
        return scaled.coerceAtMost(defaultBytes.toLong() * maxMultiplier).toInt()
    }
}

```

### settings/LiveReservePreferences.kt

```kotlin
package tv.own.owntv.core.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Reserve durations published together so a tune cannot combine different preference edits. */
data class LiveReserveConfiguration(val seconds: Int?, val extraSecs: Int)

/** Preserve the old combined buffer choice once, before delay and reserve can diverge. */
internal object LiveReservePreferences {
    val MODE = stringPreferencesKey("live_reserve_mode")
    val CUSTOM_SECS = intPreferencesKey("live_reserve_custom_secs")
    val EXTRA_SECS = intPreferencesKey("live_reserve_extra_secs")
    private val legacyModeKey = stringPreferencesKey("live_latency_mode")
    private val legacyCustom = intPreferencesKey("live_latency_custom_secs")

    fun mode(prefs: Preferences, legacyMode: String): String =
        LiveLatency.fromName(prefs[MODE] ?: prefs[legacyModeKey] ?: legacyMode).name

    fun customSecs(prefs: Preferences): Int = LiveBuffer.clampCustom(
        prefs[CUSTOM_SECS] ?: prefs[legacyCustom] ?: LiveBuffer.CUSTOM_DEFAULT,
    )

    fun extraSecs(prefs: Preferences): Int = LiveBuffer.clampExtra(prefs[EXTRA_SECS] ?: 2)

    fun configuration(prefs: Preferences, legacyMode: String): LiveReserveConfiguration =
        LiveReserveConfiguration(
            LiveBuffer.effectiveSeconds(LiveLatency.fromName(mode(prefs, legacyMode)), customSecs(prefs)),
            extraSecs(prefs),
        )

    fun materialize(prefs: MutablePreferences, legacyMode: String) {
        val chosenMode = mode(prefs, legacyMode)
        val chosenCustom = customSecs(prefs)
        if (prefs[MODE] == null) prefs[MODE] = chosenMode
        if (prefs[CUSTOM_SECS] == null) prefs[CUSTOM_SECS] = chosenCustom
    }

    fun setMode(prefs: MutablePreferences, name: String, legacyMode: String) {
        materialize(prefs, legacyMode)
        val selected = LiveLatency.fromName(name)
        prefs[MODE] = selected.name
        if (selected != LiveLatency.CUSTOM) prefs[EXTRA_SECS] = LiveBuffer.DEFAULT_EXTRA_SECS
    }

    fun setRange(prefs: MutablePreferences, minimumSecs: Int, maximumSecs: Int, legacyMode: String) {
        materialize(prefs, legacyMode)
        val minimum = LiveBuffer.clampCustom(minimumSecs)
        prefs[CUSTOM_SECS] = minimum
        prefs[EXTRA_SECS] = LiveBuffer.clampExtraForMinimum(maximumSecs - minimum, minimum)
        prefs[MODE] = LiveLatency.CUSTOM.name
    }

    /** Apply only fields supplied by a backup; legacy combined fields also seed the reserve. */
    fun importBackup(prefs: MutablePreferences, input: org.json.JSONObject, legacyMode: String) {
        materialize(prefs, legacyMode)
        val hasIndependentReserve = input.has(MODE.name) || input.has(CUSTOM_SECS.name) || input.has(EXTRA_SECS.name)
        val modeKey = if (hasIndependentReserve) MODE.name else legacyModeKey.name
        if (input.has(modeKey)) prefs[MODE] = LiveLatency.fromName(input.getString(modeKey)).name
        val customKey = if (hasIndependentReserve) CUSTOM_SECS.name else legacyCustom.name
        if (input.has(customKey)) prefs[CUSTOM_SECS] = LiveBuffer.clampCustom(input.getInt(customKey))
        if (input.has(EXTRA_SECS.name)) prefs[EXTRA_SECS] = LiveBuffer.clampExtra(input.getInt(EXTRA_SECS.name))
        else if (!hasIndependentReserve && input.has(legacyModeKey.name)) prefs[EXTRA_SECS] = LiveBuffer.DEFAULT_EXTRA_SECS
    }
}

```

### settings/DecoderQueueing.kt

```kotlin
package tv.own.owntv.core.settings

/** Renderer construction policy; changing it takes effect on the next playback opening. */
enum class DecoderQueueing {
    AUTO, ASYNCHRONOUS, SYNCHRONOUS;

    companion object {
        fun fromStored(value: String?): DecoderQueueing =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

```

### settings/ChannelReopenInterval.kt

```kotlin
package tv.own.owntv.core.settings

/** Quiet time after leaving a live channel, measured with a monotonic clock. */
object ChannelReopenInterval {
    const val DEFAULT_SECONDS = 20
    const val MIN_SECONDS = 10
    const val MAX_SECONDS = 50
    val CHOICES = MIN_SECONDS..MAX_SECONDS
    fun normalize(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
}

```

### settings/SubtitleStyle.kt

```kotlin
package tv.own.owntv.core.settings

/**
 * Shared contract for the custom subtitle look (#96) — the single place the three very different
 * subtitle renderers agree on values, defaults and geometry:
 *
 *  - mpv's own OSD (`sub-scale` / `sub-color` / `sub-back-color` / `sub-pos` / `sub-align-x`),
 *  - Media3's [androidx.media3.ui.SubtitleView] (Live TV and the VOD image-subtitle handoff),
 *  - the app-drawn Compose overlay used on mpv's direct-render path.
 *
 * **Every option has its own "Default" value** ([SCALE_DEFAULT], [COLOR_DEFAULT],
 * [Position.DEFAULT], [OPACITY_DEFAULT]) meaning *leave this alone*. Turning the master toggle on
 * therefore changes nothing on its own: only the options the user actually picks are pushed to a
 * renderer, and the rest keep their stock behaviour — including the styling broadcasters embed in
 * Live TV (CEA-608/teletext) cues.
 */
object SubtitleStyle {

    /** Size: 1.0 = the renderer's own text size. */
    const val SCALE_DEFAULT = 1.0f

    /** Color: blank = the renderer's own color (and, for [SubtitleView], embedded broadcaster colors). */
    const val COLOR_DEFAULT = ""

    /** Background transparency: negative = untouched. Otherwise 0 (no box) … 100 (solid). */
    const val OPACITY_DEFAULT = -1
    const val OPACITY_MIN = 0
    const val OPACITY_MAX = 100
    const val OPACITY_STEP = 10

    /** Where the stepper starts when transparency is moved off "Default". */
    const val OPACITY_START = 50

    /** One of six fixed screen anchors, or [DEFAULT] to leave placement to the stream/renderer. */
    enum class Position(val key: String) {
        DEFAULT("default"),
        TOP_LEFT("top_left"),
        TOP_CENTER("top_center"),
        TOP_RIGHT("top_right"),
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_CENTER("bottom_center"),
        BOTTOM_RIGHT("bottom_right");

        val isTop: Boolean get() = this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT
        val isLeft: Boolean get() = this == TOP_LEFT || this == BOTTOM_LEFT
        val isRight: Boolean get() = this == TOP_RIGHT || this == BOTTOM_RIGHT

        companion object {
            fun fromKey(key: String?): Position = entries.firstOrNull { it.key == key } ?: DEFAULT

            /** The six real anchors, in reading order — the 3×2 grid the picker draws. */
            val ANCHORS: List<Position> = listOf(TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT)
        }
    }

    fun hasScale(scale: Float): Boolean = scale != SCALE_DEFAULT

    fun hasColor(hex: String): Boolean = hex.isNotBlank()

    fun hasOpacity(pct: Int): Boolean = pct >= OPACITY_MIN

    fun clampOpacity(pct: Int): Int = pct.coerceIn(OPACITY_MIN, OPACITY_MAX)

    /** Parse "#RRGGBB"/"#AARRGGBB" into an ARGB int, falling back to opaque white. */
    fun colorArgb(hex: String): Int {
        val s = hex.trim().removePrefix("#")
        return runCatching {
            when (s.length) {
                6 -> (0xFF000000L or s.toLong(16)).toInt()
                8 -> s.toLong(16).toInt()
                else -> WHITE
            }
        }.getOrDefault(WHITE)
    }

    /** Black background box at the requested opacity, as an ARGB int. */
    fun backgroundArgb(opacityPct: Int): Int = (clampOpacity(opacityPct) * 255 / 100) shl 24

    /** mpv wants colors as "#AARRGGBB". */
    fun mpvColor(hex: String): String = "#%08X".format(colorArgb(hex))

    fun mpvBackColor(opacityPct: Int): String = "#%08X".format(backgroundArgb(opacityPct))

    // --- Geometry ---------------------------------------------------------------------------
    // mpv `sub-pos` is a percentage of screen height: 100 = bottom (its default), 0 = top.

    const val MPV_POS_BOTTOM = 100
    const val MPV_POS_TOP = 5
    const val MPV_ALIGN_X_DEFAULT = "center"

    fun mpvSubPos(position: Position): Int = if (position.isTop) MPV_POS_TOP else MPV_POS_BOTTOM

    fun mpvAlignX(position: Position): String = when {
        position.isLeft -> "left"
        position.isRight -> "right"
        else -> MPV_ALIGN_X_DEFAULT
    }

    /** Vertical placement as a fraction of the video height, measured from the top edge. */
    fun lineFraction(position: Position): Float = if (position.isTop) 0.05f else 0.95f

    /** Horizontal placement as a fraction of the video width, measured from the left edge. */
    fun positionFraction(position: Position): Float = when {
        position.isLeft -> 0.05f
        position.isRight -> 0.95f
        else -> 0.5f
    }

    private const val WHITE = 0xFFFFFFFF.toInt()
}

```

### player/PlaybackDefaults.kt

```kotlin
package tv.own.owntv.core.player

/**
 * Playback defaults the settings store has to know before any engine exists.
 *
 * The engine that acts on them lives in the player, but the value a fresh install starts from is
 * part of the persisted setting, so it is declared here and referenced from the engine — never the
 * other way round, and never copied.
 */
object PlaybackDefaults {
    /** Settings → "Give up after", in seconds. Long enough for a 4K channel on a distant panel to
     *  open, short enough that a removed channel is not ninety seconds of black screen. */
    const val LIVE_TUNE_BUDGET_SECS = 15
}

```

### player/MiniPlayerLayout.kt

```kotlin
package tv.own.owntv.core.player

/**
 * Size (as a percentage of screen width) and screen corner/edge for the docked mini-player. Both are
 * user-configurable in Settings → Playback and adjustable on the fly from the mini-player's controls.
 * The window is laid out as `fillMaxWidth(pct/100f).aspectRatio(16f/9f)` aligned to [MiniPlayerPosition],
 * so it scales consistently across TV sizes and the UI zoom (unlike the old fixed 340×191 dp box).
 *
 * The alignment each position resolves to, and each position's display label, are rendering concerns
 * and live with the mini-player in the app module.
 */
object MiniPlayerSize {
    const val MIN = 10
    const val MAX = 50
    const val DEFAULT = 25
    const val STEP = 5

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)
    fun fraction(percent: Int): Float = clamp(percent) / 100f

    /** Next size for the on-the-fly resize button — steps up by [STEP], wrapping [MAX] back to [MIN]. */
    fun next(percent: Int): Int {
        val stepped = clamp(percent) + STEP
        return if (stepped > MAX) MIN else stepped
    }
}

/** The six docking spots for the mini-player. */
enum class MiniPlayerPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /** Next spot for the on-the-fly move button — cycles through all six in order. */
    fun next(): MiniPlayerPosition = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = BOTTOM_RIGHT
        fun fromName(name: String?): MiniPlayerPosition = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

```

### player/SurroundMode.kt

```kotlin
package tv.own.owntv.core.player

/**
 * How the user wants multichannel audio handled. Persisted as a string (see
 * `SettingsRepository.surroundMode`); the legacy `surround_sound` boolean maps onto it.
 *
 * The distinction that matters is **who decodes Dolby/DTS**:
 *
 *  - [STEREO] — we always decode in-app and hand the sink plain 2.0 PCM. Nothing is bitstreamed, so a
 *    TV/soundbar that lies about its capabilities never gets the chance to mis-play anything. Safest,
 *    lowest latency, and the only mode that is correct on 2.0/2.1 speakers (which is most TVs).
 *  - [SURROUND] — the sink's advertised capabilities are used as-is: Dolby/DTS may be passed through
 *    for the TV/receiver to decode, or decoded to multichannel PCM. Right for a real 5.1/7.1 receiver.
 *  - [AUTO] — starts like [SURROUND] but demotes itself to [STEREO] the moment the audio output is
 *    caught misbehaving. Available when explicitly selected.
 *
 * The demotion is **not** the mode's job — see [AudioOutputPolicy]. The watchdog runs in all three
 * modes and cannot be switched off, because "no sound at all" is never what the user asked for.
 */
enum class SurroundMode {
    AUTO, STEREO, SURROUND;

    companion object {
        /**
         * Read from the persisted string, falling back to the pre-4.1.7 boolean.
         *
         * A user who never touched the old switch gets [STEREO] (the default); one who
         * explicitly turned it **on** clearly wants multichannel, so gets [SURROUND]; one who
         * explicitly turned it **off** was working around a broken output, so keeps [STEREO].
         */
        fun of(stored: String?, legacyBoolean: Boolean?): SurroundMode {
            stored?.let { s -> entries.firstOrNull { it.name == s }?.let { return it } }
            return when (legacyBoolean) {
                null -> STEREO
                true -> SURROUND
                false -> STEREO
            }
        }
    }
}

```

### theme/FontCustomization.kt

```kotlin
package tv.own.owntv.core.theme

/** The five font families users can independently apply to the main UI and popup chrome. */
enum class AppFontFamily {
    LORA,
    SYSTEM_SANS,
    MONOSPACE,
    PLAYFAIR_DISPLAY,
    DANCING_SCRIPT,
    POPPINS;

    companion object {
        fun fromStored(value: String?, fallback: AppFontFamily): AppFontFamily =
            entries.firstOrNull { it.name == value } ?: fallback
    }
}

/** App-only text scaling. Android's system font scale remains the base and is multiplied by this. */
object UiFontScale {
    const val MIN = 60
    const val MAX = 140
    const val DEFAULT = 100
    const val STEP = 5

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)
    fun factor(percent: Int): Float = clamp(percent) / 100f
}

/** Text scaling inside popups only. Today's popup text size is 100%. */
object PopupFontScale {
    const val MIN = 50
    const val MAX = 120
    const val DEFAULT = 100
    const val STEP = 5

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)
    fun factor(percent: Int): Float = clamp(percent) / 100f
}

/** Geometry scaling for popup panels and controls. Today's popup dimensions are 100%. */
object PopupSizeScale {
    const val MIN = 50
    const val MAX = 120
    const val DEFAULT = 100
    const val STEP = 5

    fun clamp(percent: Int): Int = percent.coerceIn(MIN, MAX)
    fun factor(percent: Int): Float = clamp(percent) / 100f
}

data class FontCustomization(
    val sizePercent: Int = UiFontScale.DEFAULT,
    val mainFamily: AppFontFamily = AppFontFamily.SYSTEM_SANS,
    val popupFamily: AppFontFamily = AppFontFamily.LORA,
    val popupFontSizePercent: Int = PopupFontScale.DEFAULT,
    val popupSizePercent: Int = PopupSizeScale.DEFAULT,
)

```

### settings/RemoteShortcuts.kt

```kotlin
package tv.own.owntv.core.settings

import android.view.KeyEvent

enum class RemoteShortcutAction {
    OPEN_HOME,
    OPEN_LIVE_TV,
    OPEN_MOVIES,
    OPEN_SERIES,
    OPEN_DOWNLOADS,
    OPEN_GUIDE,
    OPEN_SEARCH,
    OPEN_SETTINGS,
    OPEN_PROFILE_SWITCHER,
    OPEN_PLAYLIST_SWITCHER,
    CONTINUE_LAST_WATCHED,
    FOCUS_NOW_PLAYING,
    EXPAND_NOW_PLAYING,
    ENTER_MINI_PLAYER,
    ENTER_AUDIO_MODE,
    PLAY_PAUSE,
    PAGE_TOWARD_FIRST,
    PAGE_TOWARD_LAST,
    JUMP_TO_FIRST,
    JUMP_TO_LAST,
    RETURN_TO_LIVE,
    OPEN_SUBTITLE_CONTROLS,
    OPEN_AUDIO_CONTROLS,
    OPEN_ASPECT_CONTROLS,
    TOGGLE_PLAYBACK_INFO,
}

enum class RemoteShortcutPress { SHORT, LONG }

data class RemoteShortcutBinding(
    val keyCode: Int,
    val press: RemoteShortcutPress,
    val action: RemoteShortcutAction,
) {
    val id: String get() = "$keyCode:${press.name}"
}

object RemoteShortcutBindings {
    private const val SEPARATOR = '|'

    val defaults: List<RemoteShortcutBinding> = listOf(
        RemoteShortcutBinding(KeyEvent.KEYCODE_CHANNEL_UP, RemoteShortcutPress.SHORT, RemoteShortcutAction.PAGE_TOWARD_FIRST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_CHANNEL_DOWN, RemoteShortcutPress.SHORT, RemoteShortcutAction.PAGE_TOWARD_LAST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_MEDIA_REWIND, RemoteShortcutPress.SHORT, RemoteShortcutAction.PAGE_TOWARD_FIRST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, RemoteShortcutPress.SHORT, RemoteShortcutAction.PAGE_TOWARD_LAST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_CHANNEL_UP, RemoteShortcutPress.LONG, RemoteShortcutAction.JUMP_TO_FIRST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_CHANNEL_DOWN, RemoteShortcutPress.LONG, RemoteShortcutAction.JUMP_TO_LAST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_MEDIA_REWIND, RemoteShortcutPress.LONG, RemoteShortcutAction.JUMP_TO_FIRST),
        RemoteShortcutBinding(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, RemoteShortcutPress.LONG, RemoteShortcutAction.JUMP_TO_LAST),
    )

    fun encode(bindings: Collection<RemoteShortcutBinding>): Set<String> =
        bindings.mapTo(linkedSetOf()) { "${it.keyCode}$SEPARATOR${it.press.name}$SEPARATOR${it.action.name}" }

    fun decode(values: Collection<String>): List<RemoteShortcutBinding> = values.mapNotNull { value ->
        val parts = value.split(SEPARATOR)
        if (parts.size != 3) return@mapNotNull null
        val keyCode = parts[0].toIntOrNull() ?: return@mapNotNull null
        val press = runCatching { RemoteShortcutPress.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
        val action = runCatching { RemoteShortcutAction.valueOf(parts[2]) }.getOrNull() ?: return@mapNotNull null
        RemoteShortcutBinding(keyCode, press, action)
    }.distinctBy(RemoteShortcutBinding::id)

    fun replace(
        bindings: Collection<RemoteShortcutBinding>,
        binding: RemoteShortcutBinding,
    ): List<RemoteShortcutBinding> = bindings.filterNot { it.id == binding.id } + binding

    fun isProtectedKey(keyCode: Int): Boolean = keyCode in protectedKeys

    private val protectedKeys = buildSet {
        add(KeyEvent.KEYCODE_UNKNOWN)
        add(KeyEvent.KEYCODE_BACK)
        add(KeyEvent.KEYCODE_HOME)
        add(KeyEvent.KEYCODE_POWER)
        add(KeyEvent.KEYCODE_TV_INPUT)
        add(KeyEvent.KEYCODE_DPAD_UP)
        add(KeyEvent.KEYCODE_DPAD_DOWN)
        add(KeyEvent.KEYCODE_DPAD_LEFT)
        add(KeyEvent.KEYCODE_DPAD_RIGHT)
        add(KeyEvent.KEYCODE_DPAD_CENTER)
        add(KeyEvent.KEYCODE_ENTER)
        add(KeyEvent.KEYCODE_NUMPAD_ENTER)
        add(KeyEvent.KEYCODE_ESCAPE)
        add(KeyEvent.KEYCODE_BUTTON_A)
        add(KeyEvent.KEYCODE_VOLUME_UP)
        add(KeyEvent.KEYCODE_VOLUME_DOWN)
        add(KeyEvent.KEYCODE_VOLUME_MUTE)
        add(KeyEvent.KEYCODE_MUTE)
    }
}

```

## Catálogo de linhas do leitor

Lista declarativa da UI; algumas entradas são condicionais ou legadas. Uma ação como repor/limpar não tem valor predefinido.

```kotlin
internal val VIDEO_QUICK_ROWS: List<VideoQuickRef> = listOf(
    VideoQuickRef("vp_hw", SECTION_ENGINE, OwnTVIcon.VIDEO, R.string.settings_video_decoder_preference, R.string.settings_video_decoder_preference_description),
    VideoQuickRef("vp_decoder_queueing", SECTION_ENGINE, OwnTVIcon.VIDEO, CoreR.string.settings_decoder_queueing, CoreR.string.settings_decoder_queueing_description),
    VideoQuickRef("vp_deinterlace", SECTION_ENGINE, OwnTVIcon.VIDEO, R.string.settings_deinterlace_mpv, CoreR.string.settings_deinterlace_description),
    VideoQuickRef("vp_hdr", SECTION_ENGINE, OwnTVIcon.VIDEO, R.string.settings_hdr_mpv_hint, R.string.settings_hdr_mpv_hint_description),
    VideoQuickRef("vp_afr", SECTION_ENGINE, OwnTVIcon.VIDEO, CoreR.string.settings_auto_frame_rate, CoreR.string.settings_auto_frame_rate_description),
    VideoQuickRef("vp_afr_pause", SECTION_ENGINE, OwnTVIcon.PAUSE, CoreR.string.settings_afr_pause, R.string.settings_afr_pause_description),
    VideoQuickRef("vp_afr_resolution", SECTION_ENGINE, OwnTVIcon.ASPECT, CoreR.string.settings_afr_resolution, R.string.settings_afr_resolution_description),
    VideoQuickRef("vp_multiview", SECTION_ENGINE, OwnTVIcon.LIST_GRID, CoreR.string.settings_multiview, CoreR.string.settings_multiview_description),
    VideoQuickRef("vp_multiview_tiles", SECTION_ENGINE, OwnTVIcon.LIST_GRID, CoreR.string.settings_multiview_tiles_max, CoreR.string.settings_multiview_description),
    VideoQuickRef("vp_channel_playback", SECTION_ENGINE, OwnTVIcon.LIST_GRID, CoreR.string.settings_channel_playback, CoreR.string.settings_channel_playback_description),
    VideoQuickRef("vp_hls_only", SECTION_ENGINE, OwnTVIcon.LIVE_TV, CoreR.string.settings_live_hls_only, CoreR.string.settings_live_hls_only_description),
    VideoQuickRef("vp_live_engine", SECTION_ENGINE, OwnTVIcon.PLAY, CoreR.string.settings_live_tv_player, CoreR.string.settings_live_player_description),
    VideoQuickRef("vp_live_engine_sources", SECTION_ENGINE, OwnTVIcon.PLAY, CoreR.string.settings_live_engine_per_playlist, CoreR.string.settings_live_engine_per_playlist_description),
    VideoQuickRef("vp_vod_engine", SECTION_ENGINE, OwnTVIcon.PLAY, R.string.settings_movies_series_player, R.string.settings_movies_player_description),
    VideoQuickRef("vp_reset_pins", SECTION_ENGINE, OwnTVIcon.PLAY, CoreR.string.settings_reset_player_choices, R.string.settings_reset_player_choices_description),
    VideoQuickRef("vp_external", SECTION_ENGINE, OwnTVIcon.PLAY, CoreR.string.settings_live_player_target, CoreR.string.settings_live_player_target_description),
    VideoQuickRef("vp_zoom", SECTION_ENGINE, OwnTVIcon.ASPECT, CoreR.string.settings_default_zoom, CoreR.string.settings_default_zoom_description),
    VideoQuickRef("vp_reset_zoom", SECTION_ENGINE, OwnTVIcon.ASPECT, CoreR.string.settings_reset_saved_zoom, R.string.settings_reset_saved_zoom_description),
    VideoQuickRef("vp_seek_step", SECTION_ENGINE, OwnTVIcon.FORWARD, CoreR.string.settings_seek_step, R.string.settings_seek_step_description),
    VideoQuickRef("vp_rewind_step", SECTION_ENGINE, OwnTVIcon.REWIND, CoreR.string.settings_live_rewind_step, CoreR.string.settings_live_rewind_step_description),
    VideoQuickRef("vp_live_preview", SECTION_LIVE, OwnTVIcon.LIVE_TV, CoreR.string.settings_quick_live_preview, CoreR.string.settings_live_preview_description),
    VideoQuickRef("vp_local_timeshift_resume", SECTION_LIVE, OwnTVIcon.REWIND, R.string.settings_local_timeshift_resume, R.string.settings_local_timeshift_resume_description),
    VideoQuickRef("vp_preview_audio", SECTION_LIVE, OwnTVIcon.AUDIO, CoreR.string.settings_preview_audio, CoreR.string.settings_preview_audio_description),
    VideoQuickRef("vp_live_latency", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_offset_title, R.string.settings_live_offset_description),
    VideoQuickRef("vp_latency_sources", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_offset_sources, R.string.settings_live_offset_sources_description),
    VideoQuickRef("vp_live_reserve", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_reserve_title, R.string.settings_live_reserve_description),
    VideoQuickRef("vp_reserve_sources", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_reserve_sources, R.string.settings_live_reserve_sources_description),
    VideoQuickRef("vp_preroll", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_initial_buffer_title, R.string.settings_live_initial_buffer_description),
    VideoQuickRef("vp_reopen_interval", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_channel_reopen_interval, R.string.settings_channel_reopen_interval_description),
    VideoQuickRef("vp_tune_timeout", SECTION_LIVE, OwnTVIcon.LIVE_TV, CoreR.string.settings_live_tune_timeout, CoreR.string.settings_live_tune_timeout_description),
    VideoQuickRef("vp_preroll_sources", SECTION_LIVE, OwnTVIcon.LIVE_TV, R.string.settings_live_initial_buffer_sources, R.string.settings_live_initial_buffer_sources_description),
    VideoQuickRef("vp_channel_numbers", SECTION_LIVE, OwnTVIcon.LIVE_TV, CoreR.string.settings_channel_numbers, CoreR.string.settings_channel_numbers_description),
    VideoQuickRef("vp_audio_lang", SECTION_SOUND, OwnTVIcon.AUDIO, CoreR.string.settings_preferred_audio_language, CoreR.string.settings_preferred_language_description),
    VideoQuickRef("vp_surround", SECTION_SOUND, OwnTVIcon.AUDIO, CoreR.string.settings_surround_sound),
    VideoQuickRef("vp_software_audio", SECTION_SOUND, OwnTVIcon.AUDIO, CoreR.string.settings_software_audio, R.string.settings_software_audio_application_description),
    VideoQuickRef("vp_audio_sync", SECTION_SOUND, OwnTVIcon.AUDIO, CoreR.string.settings_audio_sync, CoreR.string.settings_audio_sync_description),
    VideoQuickRef("vp_reset_audio_delay", SECTION_SOUND, OwnTVIcon.AUDIO, CoreR.string.settings_reset_saved_audio_delay, R.string.settings_reset_saved_audio_delay_description),
    VideoQuickRef("vp_sub_style", SECTION_SUBTITLES, OwnTVIcon.SUBTITLE, CoreR.string.settings_subtitle_appearance, CoreR.string.settings_subtitle_appearance_description),
    VideoQuickRef("vp_sub_lang", SECTION_SUBTITLES, OwnTVIcon.SUBTITLE, CoreR.string.settings_preferred_subtitle_language, CoreR.string.settings_preferred_language_description),
    VideoQuickRef("vp_resume", SECTION_EPISODES, OwnTVIcon.PLAY, CoreR.string.settings_resume_playback, R.string.settings_resume_playback_description),
    VideoQuickRef("vp_mini", SECTION_EPISODES, OwnTVIcon.PIP, CoreR.string.settings_mini_player_root, CoreR.string.settings_mini_player_root_description),
    VideoQuickRef("vp_measured_stats", SECTION_DIAGNOSTICS, OwnTVIcon.VIDEO, CoreR.string.settings_measured_stats, CoreR.string.settings_measured_stats_description),
    VideoQuickRef("vp_logging", SECTION_DIAGNOSTICS, OwnTVIcon.INFO, CoreR.string.settings_detailed_playback_logging, CoreR.string.settings_detailed_playback_logging_description),
)
```


## Backup automático da App TV

`AutomaticBackupState.months` tem default `6`; valores suportados `0` (manual), `3`, `6`, `12`. Configuração inicial obrigatória: perfis autorizados, pasta e palavra-passe. A data-base não é reiniciada ao abrir a app. O backup de definições inclui o campo opcional `automaticBackupIntervalMonths`; pasta, palavra-passe e autorização Keystore permanecem locais.

Pasta sugerida: `OwnTV/backups/automatic` na raiz de armazenamento da app. Retenção: três ficheiros automáticos concluídos. Não se apagam backups manuais. Ver `IMPLEMENTACAO_BACKUP_AUTOMATICO_2026-10-16.md`.
