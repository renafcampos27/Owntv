# Análise Council — OwnTV (bugs, melhorias, inovações) — Ronda 3 (2026-10-19)

**Projeto:** `C:\Users\renat\Downloads\OwnTV-main`
**Pasta de saída:** `C:\Users\renat\Downloads\OwnTV-main\implementar_rena`
**Ficheiro:** `implementar_rena/ANALISE_OWNTV_COUNCIL_RENA_2026-10-19.md`
**Data:** 19/10/2026
**Elaborado por:** Conselho de multi-agentes especializados (UI/Compose TV, Player/Lifecycle/Áudio, Core/Parser/DB/Segurança, Build/CI/Reprodutibilidade, UX/Funcionalidades/Inovação) + Council coordenador
**Modalidade:** Análise estática de fontes (sem build, sem testes, sem execução em dispositivo)

---

## 1. Metodologia — Conselho de Multi-Agentes

Foram executados cinco agentes especializados em paralelo, cada um com domínio próprio. O Council coordenador consolidou, deduplicou, classificou por severidade (P1/P2/P3) e definiu a ordem de implementação. Cada item inclui **solução descrita** (o que fazer, não implementação linha a linha).

### Agentes envolvidos

| Agente | Domínio |
|--------|---------|
| UI/Compose TV | Compose for TV, foco D-pad, navegação, EPG, pesquisa, perfis, settings |
| Player/Áudio/Lifecycle | libmpv/ExoPlayer, zapping, timeshift, gravações, multiview, áudio, foco, teardown |
| Core/Parser/DB/Segurança | Parsers Xtream/M3U/Stalker, XMLTV, Room, backup/restore, redação de segredos |
| Build/CI/Reprodutibilidade | Gradle, flavors, signing, CI, dependências, baseline profile |
| UX/Funcionalidades/Inovação | Fluxos de utilizador, acessibilidade, performance percebida, diferenciação |

### Regras do Council

- Não aplicar correções em bloco sem evidência no código.
- Distinguir defeito confirmado de hipótese não reproduzida.
- Priorizar backup/privacidade, coerência do guia/definições, depois pausa/retoma.
- Medir antes de otimizar (grelha do guia, upscaling, blur).
- Para cada item: solução descrita obrigatória.

---

## 2. Resumo Executivo

A OwnTV é uma app Android TV madura, com boa arquitetura, migrações Room 1→51, crypto de backup (PBKDF2 + AES-GCM), snapshots Core validados por SHA-256 e ~318 testes. No entanto, a Ronda 3 identificou:

- **4 itens de severidade Alta** (multiview scope não cancelado; migração 5→6 em falta; remapeamento de chaves de backup pass-through; segredos de pairing em plaintext).
- **~45 itens Médios/Baixos** confirmados com evidência.
- **~30 ideias de melhoria/UX** e **~30 ideias de inovação**.
- **Problemas de build/CI** (assinatura debug em release, versionCode divergente, segredo em BuildConfig).

Números de contexto (verificados): SettingsScreen 4575 linhas; HomeScreen 2098; LiveScreen 1999; EpgScreen 1299; VideoPlayerSettingsScreen 3121; OwnTVShell 2101; LiveViewModel 158 KB (141 funções, 20 MutableStateFlow, 27 dependências). Sem ocorrências de `HapticFeedback`, `enterPictureInPictureMode`, `TextToSpeech`, `NsdManager`/mDNS.

---

## 3. Bugs Confirmados — Com Solução

### 3.1 Severidade Alta (P1 — corrigir primeiro)

#### BUG-P1-01 — MultiviewState: `cleanupScope` nunca cancelado
- **Localização:** `app/src/main/java/tv/own/owntv/features/multiview/MultiviewState.kt:93,238-249`
- **Evidência:** `cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`; `releaseAll()` cancela jobs e limpa estado, mas não cancela o scope. `fill()` pode lançar em scope morto e tocar tiles/claims stale.
- **Solução:** chamar `cleanupScope.cancel()` no fim de `releaseAll()`; guardar launches com `isActive`; no `fill()` cancelar job anterior do mesmo tile antes de iniciar.

#### BUG-P1-02 — Room: migração v5→v6 em falta
- **Localização:** `core/.../database/OwnTVDatabase.kt:253,1412`
- **Evidência:** `MIGRATION_4_6` é no-op; v5 adicionou `content_order.sortOrder`, v6 removeu-o; não existe `MIGRATION_5_6`; `ALL_MIGRATIONS` omite-o. DB persistida em v5 não consegue atualizar para v51.
- **Solução:** implementar `MIGRATION_5_6` idempotente (remover coluna `sortOrder` se existir) e registá-la em `ALL_MIGRATIONS`; adicionar teste de migração v5→v6→v51.

#### BUG-P1-03 — Backup: remapeamento de chaves pass-through
- **Localização:** `core/.../backup/BackupManager.kt:1683,1802`
- **Evidência:** `remapKeys` e `remapEnginePinKey` deixam chaves numéricas não mapeadas passar inalteradas; IDs coincidentes entre instalações podem associar settings ao perfil/fonte errado.
- **Solução:** para chaves não mapeadas, gerar novo ID determinístico por instalação ou falhar a importação (não pass-through silencioso); validar que todas as chaves importadas foram remapeadas.

#### BUG-P1-04 — Pairing: segredos em plaintext no DataStore
- **Localização:** `core/.../sync/local/PairedDeviceStore.kt:17,148-161`
- **Evidência:** segredos de pairing persistidos em preferências plaintext, ao contrário do store OpenSubtitles encriptado.
- **Solução:** migrar para EncryptedSharedPreferences/Keystore (AES-GCM) com migração one-shot; manter compatibilidade de leitura do formato antigo apenas para migração.

### 3.2 Severidade Média (P2)

#### BUG-P2-01 — HttpClient: redação de URLs incompleta
- **Localização:** `core/.../network/HttpClient.kt:151`
- **Evidência:** `redactUrl` só mascara credenciais em segmentos `live/movie/series/vod/timeshift`; URLs Stalker e outros podem vazar dados de conta em logs/UI.
- **Solução:** unificar política de redação para todos os caminhos (query, path Xtream, userinfo, chaves assinadas); rever mensagens de exceção para não incluir `Throwable` bruto.

#### BUG-P2-02 — BackupContainer: probe classifica desconhecido como LEGACY_JSON
- **Localização:** `core/.../backup/BackupContainer.kt:143,196,363`
- **Evidência:** qualquer ficheiro não vazio desconhecido é tratado como `LEGACY_JSON`; segurança de path ZIP só verifica `..`,`/`,`\`,`:`, sem normalização/decoding.
- **Solução:** exigir magic/header ou validação JSON mínima antes de classificar; normalizar paths (NFKC + decode) antes da validação; rejeitar traversal encodado.

#### BUG-P2-03 — M3uSyncer: leitura arbitrária via `file://`
- **Localização:** `core/.../sync/M3uSyncer.kt:602`
- **Evidência:** `localPlaylistSize` converte `file://` para `File` sem sanitização.
- **Solução:** restringir a diretório de dados da app ou validar path contra allowlist; rejeitar paths fora do sandbox.

#### BUG-P2-04 — LocalTimeshiftServer: bypass de token quando vazio
- **Localização:** `core/.../timeshift/LocalTimeshiftServer.kt:70`
- **Evidência:** gate de token é ignorado quando token está em branco, permitindo acesso a qualquer path sob `/`.
- **Solução:** exigir token não vazio; se ausente, recusar todos os pedidos (401/403) e logar.

#### BUG-P2-05 — Pin: SHA-256 rápido sobre PIN de 4 dígitos
- **Localização:** `core/.../util/Pin.kt:27`
- **Evidência:** hash rápido sobre baixa entropia.
- **Solução:** migrar para KDF memory-hard (Argon2id/scrypt) com salt; manter verificação de PINs antigos com upgrade transparente.

#### BUG-P2-06 — BackupCrypto: iterações lidas de JSON não confiável
- **Localização:** `core/.../backup/BackupCrypto.kt:65`
- **Evidência:** `deriveKey` lê `iterations` do JSON; limites só aplicados mais tarde na validação.
- **Solução:** validar limites (min/max) antes de derivar; usar default seguro se fora do intervalo.

#### BUG-P2-07 — StalkerClient: Cookie com TimeZone não codificado
- **Localização:** `core/.../stalker/StalkerClient.kt:743`
- **Evidência:** interpola `TimeZone.getDefault().id` bruto no header Cookie.
- **Solução:** codificar/normalizar o ID ou usar UTC fixo; validar header.

#### BUG-P2-08 — ProfileAvatarStore: id bruto no nome de ficheiro
- **Localização:** `core/.../profile/ProfileAvatarStore.kt:39`
- **Evidência:** `fileFor(profileId)` usa id sem validação antes de acesso ao filesystem.
- **Solução:** validar formato/intervalo do id; normalizar; rejeitar traversal.

#### BUG-P2-09 — M3uParser: assume UTF-8
- **Localização:** `core/.../parser/M3uParser.kt:85`
- **Evidência:** `bufferedReader()` default UTF-8; só remove BOM na primeira linha.
- **Solução:** detetar BOM/charset (UTF-8/16, ISO-8859-1) e usar charset sniff; fallback configurável.

#### BUG-P2-10 — XmltvParser: encoding depende da declaração XML
- **Localização:** `core/.../parser/XmltvParser.kt:53`
- **Evidência:** `parser.setInput(stream, null)` depende da declaração.
- **Solução:** detetar encoding por BOM/Content-Type; fallback UTF-8 com aviso.

#### BUG-P2-11 — CrashRecorder: leitura sem limite de tamanho
- **Localização:** `core/.../util/CrashRecorder.kt:50`
- **Evidência:** `readText()` sem cap pode OOM.
- **Solução:** limitar bytes lidos (ex.: 1 MB) e truncar com marcador.

#### BUG-P2-12 — LiveViewModel: cleanup IO ad-hoc e race com teardown
- **Localização:** `app/.../features/live/LiveViewModel.kt:168-196,340-357`
- **Evidência:** `disposeLocalTimeshift()` lança cleanup em `CoroutineScope(Dispatchers.IO)` e retorna; `onBackground()` para engines sem join; `onCleared()` pode continuar após VM destruído.
- **Solução:** lançar em `viewModelScope` com `withContext(Dispatchers.IO)`; join antes de parar engines; cancelar no `onCleared`.

#### BUG-P2-13 — OwnTVShell: ON_STOP sem guard de configuração
- **Localização:** `app/.../features/shell/OwnTVShell.kt:267-283,735-756`
- **Evidência:** `onBackground()` chamado em `ON_STOP` sem `isChangingConfigurations`; multiview fecha em qualquer `ON_STOP`, inclusive rotação.
- **Solução:** guardar com `if (event == ON_STOP && hostActivity?.isChangingConfigurations != true)`; só fechar multiview em background real ou recriar após config change.

#### BUG-P2-14 — RecordingsViewModel: storage flow pode morrer
- **Localização:** `app/.../features/recordings/RecordingsViewModel.kt:61-67,159-253`
- **Evidência:** fluxo infinito `while(true)` + `mapLatest { storageInfo() }` sem tratamento; exceção mata o fluxo; `play()` pode double-close handle em cancelamento.
- **Solução:** envolver `storageInfo()` em `runCatching` com retry/backoff; rastrear ownership com flag e fechar só se não nulo em `finally`.

#### BUG-P2-15 — DownloadsViewModel: storage/playback races
- **Localização:** `app/.../features/downloads/DownloadsViewModel.kt:100-103,139-154`
- **Evidência:** storage flow sem tratamento de exceção; `_lastPlayedId` definido antes da verificação de permissão; `player.play` fire-and-forget e `setSubtitleContext` imediato.
- **Solução:** envolver storage em `runCatching`; definir `_lastPlayedId` só após permissão e antes da reprodução; definir contexto de subtítulos antes de `player.play` ou aguardar sucesso.

#### BUG-P2-16 — MultiviewScreen: efeitos com chaves erradas
- **Localização:** `app/.../features/multiview/MultiviewScreen.kt:90-92,219-220,223-232`
- **Evidência:** `collectAsState()` não lifecycle-aware; foco inicial via `LaunchedEffect(Unit)` não reage a mudança de tiles; espera de 12s expira tiles a bufferizar.
- **Solução:** usar `collectAsStateWithLifecycle()`; key do efeito em `state.tiles.size`/`state.focused`; também verificar estado `BUFFERING` antes de expirar.

#### BUG-P2-17 — LiveScreen/EpgScreen/HomeScreen: efeitos com chaves excessivas ou incompletas
- **Localização:** `LiveScreen.kt:242,1039-1041,292`; `EpgScreen.kt:227,347,218`; `HomeScreen.kt:245,157`
- **Evidência:** coletores reiniciam em mudanças de paging/lista; `versionOwner`/`detailOwner` não keyed; efeito de restauro tem lista grande de keys.
- **Solução:** remover `channels`/listas das keys onde o `snapshotFlow` já observa; key em `versionOwner`/`detailOwner`; dividir efeito de restauro em efeitos menores.

#### BUG-P2-18 — UI: `contentDescription` ausente em ícones/cartões
- **Localização:** `ui/components/OwnTVIcon.kt:43-49`; `ChannelLogoTile.kt:58`; `PosterCard.kt:89`
- **Evidência:** sem `contentDescription` em ícones usados como conteúdo único; imagens sempre null.
- **Solução:** adicionar parâmetro `contentDescription` e aplicar semantics; em PosterCard usar `title`.

#### BUG-P2-19 — Build: assinatura debug em release + path hardcoded
- **Localização:** `app/build.gradle.kts:40-42,175-179`
- **Evidência:** path `E:/MEGA/CODE/OwnTV_Gradle/owntv-signing.properties`; fallback para debug signing quando não há keystore, contradizendo comentário.
- **Solução:** remover path hardcoded (usar env/gradle property); em release sem keystore, falhar build ou manter unsigned (não debug).

#### BUG-P2-20 — Build: versionCode divergente CI/local
- **Localização:** `app/build.gradle.kts:49-51`; `.github/workflows/android.yml:111-121`
- **Evidência:** app `10510005`; CI calcula `MA*10000+MI*100+PA` (ignora 4º componente) e usa `99999` para dev (menor que release).
- **Solução:** uma única derivação partilhada (script Gradle ou step CI) que parseia tag de 4 componentes; dev builds com versionCode maior que release.

#### BUG-P2-21 — Build: segredo TMDB no BuildConfig
- **Localização:** `app/build.gradle.kts:80-84`
- **Evidência:** `TMDB_EDGE_KEY` compilado como String em BuildConfig (extraível).
- **Solução:** tratar como público/rotacionar ou mover validação para servidor; não embutir segredo.

### 3.3 Severidade Baixa (P3)

#### BUG-P3-01 — FocusableSurface: estado frost stale
- **Localização:** `ui/components/FocusableSurface.kt:121-133`
- **Solução:** resetar `focusFrostSettled=false` no início do efeito ou key por `focused`.

#### BUG-P3-02 — AudioNowPlayingBar: callback em cada mudança de expanded
- **Localização:** `player/AudioNowPlayingBar.kt:132-134`
- **Solução:** disparar callback apenas em mudança real; evitar loop pai/filho.

#### BUG-P3-03 — FrameRateController: reset atrasado com Handler
- **Localização:** `player/FrameRateController.kt:309-319`
- **Solução:** cancelar reset pendente no destroy da Activity ou usar coroutine lifecycle-aware.

#### BUG-P3-04 — MpvVideoSurface: Surface pode vazar
- **Localização:** `player/MpvVideoSurface.kt:197-238`
- **Solução:** libertar Surface em `onDetachedFromWindow`; detach por identidade.

#### BUG-P3-05 — Recordings/Downloads: fluxos e play fire-and-forget (notas)
- **Localização:** `RecordingsViewModel.kt:153-155`; `DownloadsViewModel.kt:151-152`
- **Solução:** tornar cancelamento token-aware; definir contexto antes da reprodução.

#### BUG-P3-06 — Build: keepDebugSymbols, wrapper timeout, vendor zips, daemon
- **Localização:** `app/build.gradle.kts:229`; `gradle/wrapper/gradle-wrapper.properties:4-5`; `vendor/`; `gradle.properties:20,29`
- **Solução:** confirmar stripping; aumentar timeout/retries; remover zips históricos; documentar requisito de RAM.

#### BUG-P3-07 — Código morto em `work/before-recovery`
- **Localização:** `work/before-recovery/`
- **Solução:** remover do VCS.

#### BUG-P3-08 — Scroll-forward LaunchedEffect em EPG/Live/Home
- **Localização:** `EpgScreen.kt:218`; `LiveScreen.kt:292`; `HomeScreen.kt:157`
- **Solução:** debounce ou notificar apenas em transição.

---

## 4. Melhorias (Código, UX, Performance) — Com Solução

### 4.1 Performance

#### MELH-P-01 — Hero da Home recomposto ~12x/s
- **Localização:** `HomeScreen.kt:653-676,924-928`
- **Solução:** extrair `TrendingProgressBar(progress: State<Float>)` como composable folha; animar via `withFrameNanos`.

#### MELH-P-02 — FTS4 nas pesquisas por secção
- **Localização:** core `MovieDao` (L240-263), `ChannelDao` (L370-374)
- **Solução:** reutilizar `ftsQueryFor` + `MATCH` com fallback LIKE.

#### MELH-P-03 — `favoriteIds` Eagerly → WhileSubscribed
- **Localização:** `LiveViewModel.kt:1102-1105`
- **Solução:** usar `SharingStarted.WhileSubscribed(5000)`.

#### MELH-P-04 — EPG: índice de linhas + prefetch coalescido
- **Localização:** `EpgViewModel.kt:501-529`
- **Solução:** `HashMap<Long,Int>` reconstruído na mudança de canais; debounce 150 ms.

#### MELH-P-05 — Backdrop blur: quantizar chave + cache
- **Localização:** `MainActivity.kt:381-397`
- **Solução:** quantizar `ceil(size/64.dp)` e cachear últimas 2.

#### MELH-P-06 — SettingsScreen: 44 flows no root + 8 RootGroups mortos
- **Localização:** `SettingsScreen.kt` (4575 linhas), L483-710
- **Solução:** árvore ≤3 níveis; cada sub-ecrã coleta os seus flows; remover grupos mortos.

#### MELH-P-07 — Monolitos a fatiar
- **Localização:** `SettingsScreen.kt` (250.9 KB), `VideoPlayerSettingsScreen.kt` (170.6 KB), `LiveViewModel.kt` (158.4 KB), `OwnTVShell.kt` (123 KB)
- **Solução:** extrair viewmodels/composables por domínio; reduzir escopo de recomposição.

### 4.2 UX / Navegação / Acessibilidade

#### MELH-U-01 — Haptics / som de foco ausentes
- **Localização:** toda a camada UI (0 ocorrências)
- **Solução:** `performHapticFeedback` em `FocusableSurface` e ações de diálogo; opção em Settings (default off em boxes fracos).

#### MELH-U-02 — TalkBack fraco
- **Localização:** `HomeScreen.kt:726`, `MainActivity.kt:532`, `PlayerHud.kt`
- **Solução:** `contentDescription` em todas as bases focáveis; auditar com `semantics`.

#### MELH-U-03 — Multiview escondido
- **Localização:** `PlayerHud.kt`, `MultiviewDialogs.kt`
- **Solução:** botão sempre visível no HUD; se desativado, diálogo "Ativar Multiview?".

#### MELH-U-04 — Catch-up no Home + EmptyState consistente
- **Localização:** `HomeScreen.kt`, `StateViews.kt:63`
- **Solução:** linha "Catch-up agora" + aplicar `EmptyState` a catch-up, downloads, pesquisa, guia.

#### MELH-U-05 — Pesquisa global via intents do sistema
- **Localização:** `AndroidManifest.xml`, `MainActivity.kt:140-145`
- **Solução:** adicionar handlers `SEARCH`/`LEANBACK_SEARCH` a apontar para Search com query.

#### MELH-U-06 — PiP para canal secundário
- **Localização:** `MainActivity.kt` (0 ocorrências de `enterPictureInPictureMode`)
- **Solução:** PiP feature-gated (Android 8+) para um canal, com qualidade reduzida.

#### MELH-U-07 — Tracks automáticos por perfil
- **Localização:** `PlaybackSession`, `Profiles`
- **Solução:** match fuzzy (PT-PT/Português/Dubl/DUAL/Leg) ao abrir, fallback idioma do sistema.

#### MELH-U-08 — Subtítulos: max maior + posição + alto contraste
- **Localização:** settings de subtítulos + `SubtitleOverlay.kt`
- **Solução:** max 200%, presets Bottom/Top/Middle, outline de alto contraste.

#### MELH-U-09 — Fonte por superfície no guia + preset alto contraste
- **Localização:** `GuideWidthSettingsScreen.kt`, settings de foco
- **Solução:** fonte por superfície (células EPG) + preset "Alto contraste".

#### MELH-U-10 — Feedback de afinação por número
- **Localização:** path de tune em `LiveViewModel.kt`
- **Solução:** overlay transitório "A sintonizar 123…" com nome/logo; Back cancela.

#### MELH-U-11 — "Continuar" persistente + "Último canal"
- **Localização:** `HomeScreen.kt`, `HomeViewModel.kt`, `PlaybackPrefsDao`
- **Solução:** chip sempre visível + tile "Último canal" na primeira linha.

#### MELH-U-12 — Presets de fornecedor no setup
- **Localização:** `ManageSourcesScreen.kt`, `SetupViewModel`
- **Solução:** 5-8 presets (Xtream, M3U, Stalker) com buffer/charset/engine pré-definidos.

#### MELH-U-13 — Skeleton loading no primeiro sync
- **Localização:** `features/live`, `HomeScreen.kt`
- **Solução:** skeleton rows + pill "A importar… N%" consistente; desativar ações dependentes.

#### MELH-U-14 — Guide: "próxima emissão/repetição" no long-press
- **Localização:** `features/epg`, `ProgrammeReminderStore.kt`
- **Solução:** menu long-press com "Próxima emissão deste programa" e "Lembrar-me".

### 4.3 Segurança / Privacidade

#### MELH-S-01 — Redação unificada de URLs
- (ver BUG-P2-01) aplicar a todos os caminhos e exceções.

#### MELH-S-02 — Backup: validar chaves remapeadas
- (ver BUG-P1-03) sem pass-through silencioso.

#### MELH-S-03 — Pairing encriptado
- (ver BUG-P1-04).

#### MELH-S-04 — PIN com KDF
- (ver BUG-P2-05).

---

## 5. Inovações (Diferenciação) — Com Solução/Notas de Implementação

#### INOV-01 — Stream-Health Score por canal (0-100)
- **Problema:** falhas parecem idênticas.
- **Solução:** agregar buffer ms, bitrate, drops, reconnects; trocar engine abaixo de limiar; incluir no backup v24.
- **Impl:** `diagnostics/phase4diagnosticsMetrics.kt` → `core/LiveTuneController.kt`.

#### INOV-02 — Smart Catch-up Flush (detecção de gaps + auto-skip)
- **Solução:** detetar gaps de segmentos → auto-skip com marcador na seek bar.
- **Impl:** path de catch-up no player core; reutilizar `docs/SEGMENTOS_HLS_E_LIMIAR_SUPERIOR_2026-10-11.md`.

#### INOV-03 — "O que ver agora" 100% local no Home
- **Solução:** compor EPG local: começa em 10 min, a meio, retomar + próximo episódio, filtrado por perfil, zero rede.
- **Impl:** `HomeScreen.kt` + VMs.

#### INOV-04 — Auto-quirks por canal (aprendizagem)
- **Solução:** quando Stream-Health deteta padrão (ex.: precisa mpv), propor preencher `playback_quirks` com confirmação.
- **Impl:** `core/database/dao/PlaybackPrefsDao.kt`.

#### INOV-05 — Community health bundle (`.own` sem credenciais)
- **Solução:** exportar `.own` com score + última prova (HMAC salted); merge fuzzy "falhou para outros também".
- **Impl:** `BackupContainer`, `diagnostics`.

#### INOV-06 — Watch Party over LAN (mDNS)
- **Problema:** 0 ocorrências de mDNS.
- **Solução:** host/guest via mDNS com anchor de relógio + drift; áudio no host ou guest escolhido; reutiliza Companion LAN.
- **Impl:** `CompanionHttpServer`, mDNS (novo).

#### INOV-07 — PiP (ver MELH-U-06) como base para multiview leve
- **Solução:** PiP para canal secundário enquanto se navega.

#### INOV-08 — Launcher "Watch Next" / recomendações de canais ao vivo
- **Solução:** publicar "continuar live" e "próximo programa favorito" via `TvHomeRepository`/`LauncherIntegrationRepository`.

#### INOV-09 — Skip intro / cartão próximo episódio com thumbnail
- **Solução:** heurística de skip intro (hash de segmento repetido nos primeiros 90s) + thumbnails na seek bar para VOD.
- **Impl:** player core + `PlaybackPrefsDao`.

#### INOV-10 — Narração TTS no first-run quando acessibilidade ativa
- **Solução:** TTS coordenado com TalkBack em Welcome/Setup apenas com serviços de acessibilidade ativos.

#### INOV-11 — Dashboard parental (uso local)
- **Solução:** DAO por perfil com tempo de visualização por dia; dashboard com PIN em Perfis.

#### INOV-12 — Pesquisa por voz do sistema (leanback)
- **Solução:** integrar reconhecimento de voz do sistema na Search.

#### INOV-13 — "Catch-up now" row no Home (ver MELH-U-04) com deep link para programa.
#### INOV-14 — Perfis com preferências de track por idioma (ver MELH-U-07).

---

## 6. Reavaliação Council (validação cruzada)

O Council reavaliou todos os achados acima e os de rondas anteriores. Resultado:

| Item | Veredicto | Nota |
|------|-----------|------|
| BUG-P1-01 (multiview scope) | Válido | Confirmado por leitura |
| BUG-P1-02 (migração 5→6) | Válido | Confirmado |
| BUG-P1-03 (backup remap) | Válido | Confirmado |
| BUG-P1-04 (pairing plaintext) | Válido | Confirmado |
| BUG-P2-01..11 (core) | Válidos | Confirmados |
| BUG-P2-12..18 (app) | Válidos | Confirmados |
| BUG-P2-19..21 (build) | Válidos | Confirmados |
| Melhorias P/MELH-* | Válidas | Algumas requerem medição prévia (blur, upscaling) |
| Inovações INOV-* | Válidas como propostas | Requerem priorização de produto |
| "Leak" do backdrop (ronda anterior) | Refutado parcialmente | `StackBlur` opera in-place, não devolve cópia |
| Back na busca (ronda anterior) | Reformulado | 2 presses = padrão Android (limpa → colapsa) |
| Árvore legacy (ronda anterior) | Reformulado | Não é código ativo |

### Reavaliação final (solução para cada item válido)

Para cada item válido acima, a solução já está descrita na secção correspondente. Itens refutados não requerem correção. Itens reformulados mantêm-se como nota de baixa prioridade.

---

## 7. Roadmap Sugerido

**Fase 1 (segurança/estabilidade):** P1-01..04, P2-01..04, P2-19..21.
**Fase 2 (UX/perf):** MELH-P-01, MELH-P-02, MELH-P-06, MELH-U-01, MELH-U-02, MELH-U-05.
**Fase 3 (funcionalidades):** MELH-U-03, MELH-U-06, MELH-U-07, INOV-01, INOV-02, INOV-03.
**Fase 4 (inovação longa):** INOV-05, INOV-06, INOV-08, INOV-09, INOV-11.

---

## 8. Notas de Verificação

- Todos os caminhos/linhas citados foram verificados por leitura estática pelos agentes.
- Nenhum ficheiro foi modificado durante esta análise.
- Itens marcados como "por confirmar" em rondas anteriores mantêm-se marcados.
- Recomenda-se execução de testes de migração (Room) e de backup/restore antes de implementar P1.
