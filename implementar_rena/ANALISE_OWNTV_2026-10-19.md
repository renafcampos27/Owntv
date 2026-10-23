# Análise OwnTV — correções, melhorias e inovação (2026-10-19)

**Metodologia.** Conselho de cinco analistas especializados — bugs de UI/Compose TV, bugs do Core/player, arquitetura & build, UX/funcionalidades, e inovação — cujos achados foram submetidos a verificação independente antes de publicar: cada alegação de severidade Alta foi re-lida no código actual (caminho+linha real), e as Médias verificáveis foram confirmadas de forma rápida no app (`app\src\main\java\tv\own\owntv\`) e no Core extraído (snapshot SHA `e2081e78…` em `.gradle\core-snapshot\…\OwnTV_Core\` — `core\src` + `player-core\src`). Resultado da verificação: 4 achados Alta confirmados com ✅; 13 Médias/Baixas confirmadas; 1 parcialmente refutada (o "leak" do backdrop — `stackBlur` opera in-place e não devolve cópia); 1 reformulada (back na busca é comportamento padrão de 2 presses); números imprecisos corrigidos (44 flows colectados e não "50-70"; 31 dependências nomeadas no `SettingsViewModel` e não 33; 13 `.md` na raiz e ~123 no repositório; 26 pastas `values-*` no Core). Achados não verificáveis foram marcados ❓ "por confirmar". Sem invenções: todos os ficheiros citados existem.

**Resumo executivo** (o mais importante de tudo):

- 🔴 **Release sem keystore assina com a debug key**: `app\build.gradle.kts` L175-179 cai para `signingConfigs.getByName("debug")` quando não há keystore, mas o comentário L132 diz "just unsigned" — um fork/clone novo produz uma "release" com assinatura errada; o caminho `E:/MEGA/CODE/OwnTV_Gradle/…` está hard-coded em L41 (§1-B2).
- 🔴 **Download truncado é gravado COMPLETED**: `DownloadEngine.kt` L215-217 não compara com o Content-Length nem calcula digest; o fim "em paz" de uma resposta sem delimitação grava um ficheiro cortado como concluído (§1-B4).
- 🔴 **`SettingsScreen.kt` (250,9 KB, 71 composables) recolhe 44 StateFlows no composable raiz** e reconstrói o catálogo `rootItems` (~60 rows com dezenas de `stringResource`) a cada restart; 8 dos `RootGroup` aí construídos (L483-710) são dados mortos — são filtrados fora em L757 (§1-B1).
- 🔴 **Hero da `HomeScreen.kt` recomposto ~12×/s**: o progresso do carrossel é escrito a cada 80 ms (L663-676) e lido em `fillMaxWidth(progress)` no mesmo escopo do hero (L924-928) (§1-B3).
- 🟠 **Deep link re-dispatch após recreate** (mudar idioma/tema reabre o deep link fullscreen): `MainActivity.kt` L202 re-deriva de `intent.data` depois de consumido (§1-M1).
- 🟠 **Prewarm do EPG fixo aos 20 s** com `selectedSection` capturado da 1.ª composição → o `load()` duplicado corre mesmo com o utilizador já no EPG (§1-M2).
- 🟠 **versionCode divergente entre CI e local**: CI calcula `MA*10000+MI*100+PA` (1.050.100 para v105.1.0) enquanto o local é 10.510.005 ("105.1.0.5") — e um tag de 4 componentes quebra a aritmética do CI (§3).
- 🟠 **URLs Xtream sem escape de credenciais** (`XtreamClient.kt` L444-458) e **M3U assumindo UTF-8** (`M3uParser.kt` L85): preferências que quebram streams/unicode real (§1-M6, M7).
- 🟡 **Monolitos**: `SettingsScreen` 250,9 KB / `VideoPlayerSettingsScreen` 170,6 KB / `LiveViewModel` 158,4 KB (141 funções, 20 `MutableStateFlow`, 27 dependências) / `OwnTVShell` 123 KB (3 composables, 22 parâmetros) — plano de fatiação em §3.
- 🟡 **Zero haptics e TalkBack fraco**: 0 ocorrências de `HapticFeedback`; `contentDescription = null` em dezenas de controlos (§4).
- 🟢 **Base sólida**: migrações Room 1→51 com testes, crypto de backup (PBKDF2 210k + AES-GCM), snapshots Core validados por SHA-256 com anti-zip-slip, 318 testes, i18n de 44 idiomas com ratchet (§7).
- 💡 **Inovação com maior ROI**: Stream-Health Score por canal, Smart Catch-up Flush, "O que ver agora" 100% local, pesquisa por voz do sistema (§5).
- 🆕 **Ronda 2 (19-10)**: 60 novos achados verificados (28 bugs + 14 melhorias + 18 ideias de inovação lote 2) + reavaliação de tudo da Ronda 1 → §8-§9 (marcações no Apêndice F).
- 🆕 **Ronda 3 (19-10)**: 41 novos achados verificados (30 ✅ · 3 ⚠️ · 1 ❌ · 7 ❓ — 6 slots por re-traçar) + 12 melhorias/lacunas + 17 ideias de inovação lote 3, e reavaliação final consolidada das três rondas → §10-§11 (marcações no Apêndice G). Top 3: o gate "Who's watching?" é contornável em 1 minuto via "Adicionar perfil"; a pesquisa FTS falha palavras acentuadas (FTS4 sem unicode61) e o cache negativo do TMDB envenena 7 dias sob corpo 200 não-parsável; episódios com o mesmo nome colidem no mesmo ficheiro e o resume/416 grava COMPLETED com bytes doutro item.
- 🆕 **Ronda 4 (19-10, 4.ª passagem — especialistas plataforma/ciclo, rede, i18n/RTL/a11y, governança e inovação lote 4)**: TOP 5 de cada analista + Médias de maior alavanca re-verificados no código numa grade de 26 itens (a-z); +18 ideias lote 4 → §12-§13 (ledger no Apêndice H). Top 3: playback fullscreen é invisível ao orçamento de conexões (3 sessões reais num limite de 2, o 458 mata um tile); o pipeline i18n só mede `strings*.xml` — o módulo app nem sequer está no pipeline (pt-PT perde 96 chaves + 8 ficheiros inteiros); eventos e downloads não respeitam redes metered (`callTimeout`/Retry-After/metered). Refutações parciais: os scripts i18n citados não existem no repo (só no snapshot do Core) e o hero preview **já** regista claim no OpenStreamRegistry.

---

## 1) Bugs confirmados (Alta → Baixa)

**Contagem final**: 4 Alta ✅ · 7 Media confirmadas (M1-M7) · 5 Baixa/Nota confirmadas · 5 hipóteses ❓ (§2) · 1 refutação parcial (leak backdrop) · 2 reformulações (back de busca; árvore legacy). Os fixes históricos fullscreen/zapping não têm vestígios activos — vivem no player-core e a UI está alinhada.

### 1.0) Veredicto da verificação (todos os achados revistos um a um)

| Achado original | Veredicto | Correção aplicada ao relatório |
|---|---|---|
| A1 — SettingsScreen 50-70 flows no root + catálogo sempre reconstruído | ✅ Confirmado (44 flows, não 50-70) | Número fixado; `searchResults` (L908) também recriado por restart; 8 `RootGroup` mortos adicionados ao diagnóstico |
| A1 — release assinado com debug key + caminho `E:/…` hard-coded | ✅ Confirmado (ambos) | Comentário do próprio ficheiro contradiz o código (L132); CI tratado como fallback de fork |
| A1 — hero da Home 12×/s | ✅ Confirmado | `Box` inline → âmbito do hero inteiro; fix em composable folha + animação |
| A1 — deep link re-dispatch no recreate | ✅ Confirmado | Linha "MainActivity L985-1004" é na verdade `OwnTVShell.kt` L986-1012 |
| A1 — prewarm EPG com `selectedSection` da 1.ª composição | ✅ Confirmado | `selectedSection` é parâmetro (L130) — capturado pelo `LaunchedEffect(Unit)` |
| A1 — backdrop re-blur por `rootSizePx` | ✅ Confirmado | Parte "leak ~1 MB/blur" **refutada**: `StackBlur.kt` L119-120 é in-place |
| A1 — polling `while(true) delay(50)` na Main | ✅ Confirmado | Coroutine do viewModelScope; corre da escada L2096-2153 |
| A1 — coletores condicionais `zapSource` | ✅ Confirmado | Par Movies/Series em L478-481 |
| A1 — dois BackHandlers, back não fecha busca | ⚠️ Reformulado | Há 3 handlers (L248, L806, L812); 2 presses = padrão Android (limpa→colapsa); nota Baixa |
| A1 — `indexOfFirst` O(n) no overlay | ✅ Confirmado (Baixa) | `remember(channels, currentId)` → 1× por abertura |
| A2 — download truncado COMPLETED | ✅ Confirmado | Nuance OkHttp adicionada (falha abrupta só em streams sem delimitação) |
| A2 — resume sem `If-Range` | ✅ Confirmado | Grep do ficheiro: zero `If-Range`/ETag/digest |
| A2 — M3U assumed UTF-8 | ✅ Confirmado | BOM UTF-8 cortado manualmente, resto default |
| A2 — Xtream sem escape | ✅ Confirmado | 4 builders (L444, L451-453, L455-456, L457-458) |
| A2 — media-sequence reset na gravação | ✅ Mecanismo / ❓ gatilho | Só gap ASC tratado (L711-717); filtro L720 |
| A2 — scope sem SupervisorJob | ✅ Estrutura / ❓ impacto | `PlaybackSession.kt` L42 |
| A2 — M3uSyncer renumera episódios | ✅ Mecanismo / ❓ gatilho | L426-427 reset + L440 contagem |
| A2 — split `&` corta headers | ✅ Confirmado (Baixa) | L373-385 |
| A2 — `mpvExecutor.shutdown()` sem await = destroy pode não correr | ✅ Parcialmente refutado | `shutdown()` drena a fila; thread daemon L1050-1051; risco só em `exit()` imediato — Nota |
| A3 — monolitos e números | ✅ Confirmado com ajustes | 27 deps no `LiveViewModel` (não 28); 31 nomeadas no `SettingsViewModel` (não 33); 26 `values-*` (não 29); 13 `.md` na raiz / ~123 no repo (não ~122 na raiz); comentário stale "24 parameters" descoberto |
| A3 — versionCode divergente CI↔local | ✅ Confirmado | Tag de 4 componentes quebra a aritmética do bash (mais grave do que o dito) |
| A4 — haptics zero / TalkBack fraco / sem voz global | ✅ Confirmado | 0 ocorrências; `AndroidManifest.xml` sem SEARCH; `contentDescription = null` em L726/L532 |
| A4 — árvore legacy duplicada L483-710 | ⚠️ Reformulado | As rows são usadas; apenas os 8 `RootGroup` são dados mortos |
| A5 — pontos de integração das 15 ideias | ✅ Confirmados | Todos os ficheiros citados existem (v. §5) |

### 🔴 B1 — Alta ✅ — `SettingsScreen.kt`: recolha no root + catálogo reconstruído a cada restart
- **Ficheiro/linhas**: `app\src\main\java\tv\own\owntv\features\shell\components\SettingsScreen.kt` — 44 `collectAsStateWithLifecycle` em L302-374 (inclui `uiSummary` de diagnóstico em L329); `rootItems` construído inline em L479-746; `remember(rootItems, groupDefinitions)` em L756; 8 `RootGroup` mortos em L483, 490, 537, 612, 666, 673, 689, 710 (todos filtrados fora por `filterIsInstance<RootRow>()` em L757). 250,9 KB / 71 composables.
- **Sintoma**: qualquer alteração num dos 44 sinais recolhidos (cada toggle, chip, temporizador de diagnóstico) reinicia o composable raiz inteiro: re-atribuição de ~60 rows com dezenas de `stringResource`, alocação de listas, comparação `equals` de data classes com lambdas no `remember` de L756, e redefinição de `searchResults` (L908). Em ecrãs lentos de TV nota-se jank ao mover o foco enquanto um chip muda.
- **Reprodução**: abrir Definições → alternar "Canais directos" ou abrir/fechar um diálogo → profiler mostra o root inteiro a recompor; o `UiPerformanceSummary` (L329) referencia o próprio monitor de frames.
- **Correção proposta**: (1) mover cada colector para o composable da row/linha que o consome (os rows já conhecem os seus chips); (2) construir o catálogo uma única vez fora da recomposição (função pura `buildCatalog(context, vm)` em `LaunchedEffect` ou `remember(vm, locale)`); (3) eliminar os 8 `RootGroup` mortos de `rootItems`; (4) numero exacto: são 44 flows — corrigir o comentário do código se existir.
- **Severidade**: Alta (performance + fragilidade estrutural; ficheiro é o nº 1 para fatiar).

### 🔴 B2 — Alta ✅ — Release sem keystore fica assinado com a debug key + caminho absoluto hard-coded
- **Ficheiro/linhas**: `app\build.gradle.kts` L40-42 (`val localSigningProps = … File("E:/MEGA/CODE/OwnTV_Gradle/owntv-signing.properties")`), L131-132 (comentário "builds still succeed, just unsigned"), L175-179 (`else { signingConfig = signingConfigs.getByName("debug") }`). Confirmado no CI: `.github\workflows\android.yml` L127-141 ("building a debug-signed APK" quando não há `KEYSTORE_BASE64`).
- **Sintoma**: num clone fresco/fork, `assembleRelease` produz um "release" R8-signed com a chave de debug — não instala sobre uma release publicada e pode ser publicado por engano; o comentário contradiz o código.
- **Reprodução**: sem `KEYSTORE_FILE`/secret → `assembleRelease` e verificar a assinatura do APK.
- **Correção proposta**: em release, exigir keystore real (`check(releaseKeystore != null)` ou encadear `instructions`) e falhar o build; remover o caminho absoluto de disco (usar env var/`gradle.properties`); actualizar o comentário para dizer "debug-signed".
- **Severidade**: Alta (processo release/segurança operacional).

### 🔴 B3 — Alta ✅ — Hero da Home recomposto ~12×/s pelo indicador de progresso
- **Ficheiro/linhas**: `app\src\main\java\tv\own\owntv\features\home\HomeScreen.kt` — `var progress` L653; laço `while(true) { progress = …; delay(80L) }` L663-676; leitura no mesmo escopo em `Modifier.fillMaxWidth(progress…)` L924-928 (o `Box` é inline → o âmbito de recomposição é o composable do hero completo).
- **Sintoma**: enquanto o carrossel corre, o hero inteiro (imagem de fundo, badges, textos, botões) recompõe ~12,5 vezes por segundo — trabalho de composição e GC visível em boxes de TV modestas.
- **Reprodução**: Home com 2+ items de Trending → Layout Inspector/Frame trace mostra frames periódicas sem interacção.
- **Correção proposta**: extrair o indicador para um composable folha (`TrendingProgressBar(progress: State<Float>)`) que é o único a ler o estado; idealmente animar com `animateFloatAsState`/`withFrameNanos` para eliminar o `delay(80)`.
- **Severidade**: Alta (constante em todas as sessões na Home).

### 🔴 B4 — Alta ✅ — Download truncado é marcado COMPLETED (sem validação de tamanho/digest)
- **Ficheiro/linhas**: Core snapshot `…\OwnTV_Core\core\src\main\java\tv\own\owntv\core\download\DownloadEngine.kt` — laço de leitura L198-214 termina em `n < 0 break`; L215-217 grava `status = COMPLETED, downloadedBytes = size, totalBytes = size` com `size = target.length()`, ignorando o `total` já conhecido (L190, `DownloadResume.expectedTotal`) e sem digest. Grep confirma: nenhum `If-Range`/ETag/digest no ficheiro. Nuance: o OkHttp lança `IOException` num corte abrupto com Content-Length/chunked, mas respostas **sem** Content-Length (ou com ele errado) terminam "em paz" — aí o truncado é aceite.
- **Sintoma**: filme/episódio aparece como completo no ecrã mas jogável só até meio; o erro só aparece no player, sem chance de re-tentativa automática (o item já está COMPLETED).
- **Reprodução**: servidor sem Content-Length + proxy que corta a ligação a ~70% → download COMPLETED com ficheiro parcial.
- **Correção proposta**: no fim do laço, se `total > 0 && done != total` → marcar PARTIAL/FAILED e deixar o ciclo de 3 tentativas resumir via `Range`; exigir digest quando disponível (documentação já tem SHA-256 nas outras partes do Core).
- **Severidade**: Alta (perda silenciosa de dados no produto central — o ecrã de Downloads é uma funcionalidade de destaque).

### 🟠 M1 — Média ✅ — Deep link re-dispatch após recreate da Activity
- **Ficheiro/linhas**: `app\src\main\java\tv\own\owntv\MainActivity.kt` L127 (campo `pendingDeepLink` mutableStateOf), L143 (onNewIntent), L202 (onCreate re-parse `intent.data`), L479-480 (`pendingDeepLink = pendingDeepLink; onDeepLinkConsumed = { pendingDeepLink = null }`); consumo em `OwnTVShell.kt` L986-1012. A referência do analista a "MainActivity L985-1004" é na verdade `OwnTVShell.kt`.
- **Sintoma**: mudar idioma/tema (recreate sem intent novo) volta a derivar o deep link do `Intent` original e reabre o fullscreen live após o consumo.
- **Reprodução**: abrir deep link de canal → ir a Definições → mudar idioma → regresso: canal abre de novo em fullscreen.
- **Correção**: consumir com flag persistida no estado da sessão (`rememberSaveable`/SharedFlow de 1 evento) ou limpar `intent.data` (`intent.data = null`) + `intent.replaceExtras` após consumo.
- **Severidade**: Média.

### 🟠 M2 — Média ✅ — Prewarm do EPG aos 20 s não cancela e duplica `load()`
- **Ficheiro/linhas**: `OwnTVShell.kt` L659-665 — `LaunchedEffect(Unit) { delay(20_000); if (selectedSection != EPG && playerMode == NONE) epgVm.load() }`. `selectedSection` é **parâmetro** (L130), logo o valor lido é o da 1.ª composição; comentário L657 admite "EpgScreen also calls load() on mount".
- **Sintoma**: entrar no EPG nos primeiros 20 s não cancela o prewarm; o `epgVm.load()` corre via shell e via EPG Screen — trabalho gasto e possível re-página de guide.
- **Correção**: `rememberUpdatedState(selectedSection)` antes do delay, ou key no LaunchedEffect (`LaunchedEffect(selectedSection, playerMode)` com guard `startedAt`); facto: correu bem com 20 s fixo — considerar 8-10 s ou primeira vez que Home fica visível.
- **Severidade**: Média.

### 🟠 M3 — Média ✅ — Polling 20 Hz na escada de recuperação
- **Ficheiro/linhas**: `LiveViewModel.kt` L2146-2153 — `while (true) { …; if (recoveryOpenedId == …) break; …; delay(50) }`, dentro do `launch` do ViewModel (dispatcher Main, confirmado por contexto L2096-2153).
- **Sintoma**: enquanto a escada de alternativas tenta canais, ~20 wake-ups/s na Main; o estado via `recoveryOpenedId`/`recoveryFailedId` (L2052-2053, L2132-2133) já existe para sinalizar.
- **Correção**: `CompletableDeferred<Unit>`/`select` com `publishedId` — o callback `opened`/`failed` completa o awaiter em vez do polling.
- **Severidade**: Média (CPU/bateria durante falhas de canal — exactamente quando o UI está sobre carga).

### 🟠 M4 — Média ✅ — `collectAsStateWithLifecycle` condicionais trocam de coletores a cada secção
- **Ficheiro/linhas**: `OwnTVShell.kt` L478-481 — `if (zapSource == MainSection.MOVIES) movieVm.playingMovie.collectAsStateWithLifecycle().value else null` (x2, Movies/Series).
- **Sintoma**: os colectores entram/saem da composição cada vez que `zapSource` muda → churn de subscrições e micro-jank na troca de secção.
- **Correção**: coleccionar sempre e ignorar quando não aplicável (`if (zapSource == MOVIES) movieVm.playingMovie.value else null` lido em texto/passe — ou universalizar para um `Flow` combinado no ViewModel).
- **Severidade**: Média-baixa.

### 🟠 M5 — Média ✅ (com refutação parcial) — Backdrop re-decode/re-blur por mudança de tamanho; **sem leak**
- **Ficheiro/linhas**: `MainActivity.kt` L381-397 — `produceState` com key em `rootSizePx` (L388) → qualquer alteração do tamanho do root (`onGloballyPositioned` L416-418) re-decoda + re-crop + re-blur. **Refutado por relectura**: a alegação "possível leak ~1 MB/blur" não se confirma — `StackBlur.kt` L119-120 faz `bitmap.setPixels(a, …); return bitmap` (mutação in-place, mesma instância), pelo que `buildFrostMipPyramid` (L656-669) přísli todos os bitmaps nos níveis retornados; o ramo sem pirâmide recicla em L638.
- **Correção**: quantizar a key do blur (ex.: `-ceil(size/64.dp)`) ou cache últimos 2 tamanhos.
- **Severidade**: Média (trabalho repetido) — a parte "leak" é ✅-refutada.

### 🟠 M6 — Média ✅ — Credenciais Xtream sem escape numa URL construída por interpolação
- **Ficheiro/linhas**: Core `…\core\src\main\java\tv\own\owntv\core\parser\XtreamClient.kt` L444 (`liveUrl`), L451-453 (`timeshiftUrl`), L455-456 (`movieUrl`), L457-458 (`seriesEpisodeUrl`) — `"${base(s)}/live/${s.username}/${s.password}/$streamId.ts"` sem encoding.
- **Sintoma**: password com `/`, `@`, espaço ou `?` produz 404/streams quebradas nos endpoints de stream (os endpoints `player_api.php`/`xmltv.php` escapam ao modo normal, por isso a sync funciona mas a stream falha).
- **Correção**: construir com `Uri.encode`/`URLDecoder` dos componentes, ou `HttpUrl.Builder().addPathSegment(...)` por segmento.
- **Severidade**: Média (alta taxa de falha reportada por utilizadores de portals com passwords especiais).

### 🟠 M7 — Média ✅ — M3U assumido UTF-8 (mojibake em windows-1251/1256/ISO-8859-1)
- **Ficheiro/linhas**: Core `…\core\src\main\java\tv\own\owntv\core\parser\M3uParser.kt` L85 — `input.bufferedReader()` (charset default JVM/UTF-8); só o BOM UTF-8 é cortado manualmente (L87-89); não há sniff.
- **Sintoma**: playlists de provedores RU/PT/BR com charset legacy mostram nomes/grupos permanentemente ilegíveis (guardados corruptos na BD).
- **Correção**: detectar BOM UTF-16LE/BE e tentar `windows-1251`/`windows-1256`/`ISO-8859-1` quando os bytes não validarem em UTF-8 (heurística de 1-2 KB iniciais); expor override manual na fonte.
- **Severidade**: Média.

### 🟡 Baixa 1 — Universal APK ainda produz artefacto ~104 MB
- `app\build.gradle.kts` L110-117 — splits com `isUniversalApk = true`; o comentário L89-97 documenta o parse-error resolvido com flavors, mas o universal continua a sair em cada build e o CI o empacota (`android.yml` L185-186). Correção: `isUniversalApk = false` + gerar universal só em job manual.

### 🟡 Baixa 2 — Back na busca precisa de 2 presses (reformulado ✅)
- `SettingsScreen.kt` L248 (`searchQuery.isNotBlank()` → limpa), L806 (`searchExpanded && isBlank` → colapsa), L812 (`sheetFocused && isBlank` → refocus). A leitura mostra o comportamento padrão Android (1.º limpa, 2.º colapsa) — não é bug; nota: L806 e L812 podem estar ambos activos ao mesmo tempo (vence o último composto); considerar `enabled` mutuamente exclusivos.

### 🟡 Baixa 3 — `indexOfFirst` O(n) ao abrir o overlay de canais
- `app\src\main\java\tv\own\owntv\ui\components\ChannelListOverlay.kt` L77-83 — `channels.indexOfFirst { it.id == currentId }` em `remember(channels, currentId)`; 1× por abertura (~50k canais → alguns ms). Correção: índice por `id` em `Map<Long, Int>` guardado no VM.

### 🟡 Baixa 4 — Poll 32 ms no press-and-hold
- `app\src\main\java\tv\own\owntv\ui\components\ChannelPressInput.kt` L81-95 — `while(true) { … delay(32) }` enquanto o hold corre. Correção: usar `Animatable`/`withInfiniteAnimationFrameNanos`.

### 🟡 Baixa 5 — Split `&` em headers pipe/KODIPROP corta valores sem percent-encoding
- Core `M3uParser.kt` L373-385 — `text.split('&')`; o comentário L371-372 assume valores percent-encoded, mas uma entrada tipo `Authorization=Bearer a&b` quebra em dois headers. Correção: tentar primeiro `Authorization` completo e só partir em `&` quando canonicalName reconhece o troço seguinte.

### ⚪ Nota — `mpvExecutor.shutdown()` em `OwnTVPlayer.release()`
- Core `…\player-core\src\main\java\tv\own\owntv\player\OwnTVPlayer.kt` L4149-4177 + L1050-1051 (thread daemon). Re-lectura: `shutdown()` **drena** a fila — o destroy do native corre no teardown normal; a alegação "pode não correr" só se aplica a `System.exit()` imediato após `release()`. Sem acção urgente; se quisermos garantia absoluta, `awaitTermination(2s)` num caminho de teste apenas.

### Bugs históricos (fullscreen / zapping)
Sem vestígios activos: os fixes vivem no player-core e a UI está coerente — `LiveViewModel.kt` L1744-1768 (`zapTo`) usa `previewEngine.deferCurrentTune()` (L1750) + `pendingZapTuneJob` (L1760-1768) + `ZapTiming.shared` (L1749). Confirmado por re-lectura; não repensar.

### Checklists de reprodução dos 4 Alta
**B1** — abrir Definições → activar diagnóstico de UI (L328-329) → alternar qualquer chip/toggle → Layout Inspector/trace: o root inteiro recompõe a cada interacção; colectores contados: 44 em L302-374.
**B2** — clone fresco sem env vars/propriedades → `gradlew :app:assembleStandardRelease` → `apksigner verify --print-certs` mostra a chave debug; inspeccionar L40-42 e L175-179.
**B3** — Home ≥2 items Trending → macrobenchmark/trace: ~12 writes/s de `progress` (L672) e recomposição do `TrendingHero` núcleo em cada write, com o `Box` de L924 no mesmo scope.
**B4** — pipeline/proxy próprio que responde sem Content-Length e corta a ligação a 60% → `DownloadEngine` marca COMPLETED (L215-217) com `totalBytes = size` ≠ bytes esperados; um `curl -C -` equivalente demonstra o resume sem `If-Range`.

## 2) Hipóteses por confirmar

- ❓ **RecordingEngine (HLS) — *media-sequence* reset do provider**: Core `…\core\src\main\java\tv\own\owntv\core\recording\RecordingEngine.kt` L711-717 e o filtro `original.sequence <= session.lastSequence` (L720) — re-lectura confirma que só existe tratamento de salto ASCendente (gap quando `mediaSequence > lastSequence + 1`); um CDN que **reinicie** a numeração faz com que todos os segmentos fiquem filtrados → gravação "OK" com ficheiro só de init. O mecanismo está verificado; o gatilho depende do provider. Correção: detectar reset (nova sequência < anterior) e abrir nova sessão/parte.
- ❓ **`PlaybackSession.kt` L42 — scope sem SupervisorJob**: `CoroutineScope(Dispatchers.Main.immediate)` sem `SupervisorJob` — uma exceção não capturada num child cancela todo o scope. A estrutura está confirmada; o impacto em runtime (MediaSession a silenciar até reattach) é hipotese.
- ❓ **`M3uSyncer.kt` L426-455 — renumeração de episódios**: `flushSeries(false)` limpa `seriesAccumulator` (L426-427); depois, L440 usa `show.episodes.count { … } + 1` para episódios sem padrão `SxxEyy` → após um flush parcial a contagem reinicia e os primeiros ~5 mil episódios podem ser sobrescritos pelo upsert. Gated a: show a atravessar a fronteira de flush E sem padrão SxxEyy.
- ❓ **`System.currentTimeMillis()` como relógio de EPG/gravação sem wait de NTP**: em boxes com RTC sem bateria, o arranque pode dar janelas de guide vazias até o NTP sincronizar. Verificar se o arranque já bloqueia a EPG até `ConnectivityObserver`-provided time.
- ❓ **Checksum do libmpv**: a dependência nativa vem de artefacto Maven externo (fora do lock do Core) — fixar SHA-256 no ficheiro da dependência e verificar na CI (alegação do analista 3, não re-verificada neste pass).
- ❓ **Emissão periódica de `UiPerformanceSummary.snapshot`** (colectado no root de Definições, L329) — se emitir em intervalos curtos com `uiDiagnostics` ligado, agrava a frequência de restart do root; medir com o próprio UI monitor.

## 3) Arquitetura, qualidade e build

**Monolitos verificados (tamanhos reais via `Get-Item`):**

| Ficheiro | Tamanho | Composables/fn | Problema central |
|---|---|---|---|
| `…\features\shell\components\SettingsScreen.kt` | 250,9 KB | 71 composables | root com 44 flows + catálogo inline (§1-B1); dialogs partilhados com MoreScreen |
| `…\features\settings\VideoPlayerSettingsScreen.kt` | 170,6 KB | 44 composables | ficheiro único por secções; já tem colectores por row |
| `…\features\live\LiveViewModel.kt` | 158,4 KB | 141 funs, 20 `MutableStateFlow` | tune+recovery+EPG+favoritos+preview no mesmo objeto |
| `…\features\shell\OwnTVShell.kt` | 123,0 KB | 3 composables | shell coerente mas com 22 parâmetros (L129-155) |

**Plano de fatiação** (mantém behavior, extratos puros já provam o padrão — `CatchupContinue`/`OrderDraftCommitter`/`TuneSelection` têm testes):
1. `SettingsScreen` → pasta `parts\`: `Spine*` (coluna de grupos), `SettingsRow`, `Chip*`, `TileTone`; um ficheiro por `SettingsTab`; searchable (`searchResults` L908) extraído.
2. `LiveViewModel` → `LiveCataloguePaging` + `LiveCatchupController` + `LiveOrderingController` + `LiveRecoveryController` (a L2096-2153 escada isolada; M3 sai daí); binding DI passa a helpers.
3. `OwnTVShell` → `ShellPlayerHost` + `ShellDialogsHost` + compositor stateless (deu só 3 composables — elevada coesão; cortar por região lógica).
4. `MoreScreen`/`Customize`: imports de 10+ símbolos de shell.components/settings → facilita o corte (1); tratar em conjunto.

**DI/Koin real** (números de re-lectura): `di\AppModule.kt` L67-97 — binding manual do `LiveViewModel` com **27** `get()` (comentário L64 explica o limite de 22 do `viewModelOf`); L109-143 — `SettingsViewModel` com **31** dependências nomeadas (o comentário L105-106 diz "24 parameters" — stale). Remedio: nenhum; documentar. Razão estrutural: Deus-objetos → a fatiação em (2) cobra isto.

**versionCode divergente ✅**: local `app\build.gradle.kts` L49-51 = `10510005` / "105.1.0.5"; CI `.github\workflows\android.yml` L108-125 — tag `v105.1.0` → `105*10000+1*100+0 = 1050100`, ou seja **menor** que qualquer build local-side release; tag de 4 componentes (v105.1.0.5) faz `PA="0.5"` → expressão aritmética com erro. Fix único: derivar `versionCode` do mesmo helper nos dois lados (ex.: `MA*10_000_000 + MI*100_000 + PA*1000 + hotfix`), ou pôr o tag a transportar o code (ex. `v105.1.0.5@10510005`).

**Higiene do repo ✅**: 7 vendor zips contents em `vendor\owntv-core-source-*.zip` (cada ~3,8 MB, ~27 MB committed) — manter só o actual + o zip "unhashed" se for o binding; `work\` existe com código antigo (`work\before-recovery\LiveViewModel.kt` segundo o analista); `.md`: **13 na raiz** e **~123 no total** (maioria em `docs\`) — mover ranqueados; `.gitignore` saudável.

**Namespace duplicado ✅**: `app\src\main\java\tv\own\owntv\core\i18n\LocalizedContent.kt` e `RtlKeys.kt` declaram `package tv.own.owntv.core.i18n` — o mesmo namespace do módulo Core. Vira colisão subtil na importação e bloqueia split real de package. Correção: mover para `tv.own.owntv.features.i18n` (com facade se necessário).

**Supply chain**: `libmpv` de artefacto Maven externo — verificar e fixar checksum/SHA no módulo que o resolve ❓ (por confirmar; alegação do analista 3). O snapshot Core tem anti-fork forte (SHA-256 validado por `resolve-core`, CI compara; 7 zips por hash conferidos acima).

**Testes (contagens verificadas)**:

| Módulo | Unit | Instrumented | Nota |
|---|---|---|---|
| app (`src\test`) | 76 ficheiros | 3 (`src\androidTest`) | DatabaseStartupViewModelTest, DialogFocusRestoreTest, GuideRowCacheTest, etc. |
| Core (`core\src\test`) | 162 ficheiros | 15 androidTest | OwnTVDatabaseMigrationTest, Backup*, EPG, XtreamClientTest |
| player-core | 80 ficheiros | – | densos no engine |

Gaps (o analista 3 referencia, verificado por ausência de ficheiros de teste directos): `LiveViewModel`, `SettingsViewModel`, `SetupViewModel`, `OwnTVShell`, `PlayerHud`; sem testes compose-ui; sem `coroutines-test`/`turbine` declarados. Prioridade: os controllers que sairem da fatiação (§3 plano 1-2) — cada extração com o seu teste antes de cortar o código antigo.

**Lint/formatação**: sem ktlint nem detekt nos build files (grep zero ✅); `lint` com `abortOnError` no CI (android.yml L57-60); proguard mínimo e coeso. Base profile: plugin consumer presente (`app\build.gradle.kts` L27-28, bloco L256-262) mas geração **manual** com device API 33+ (L392-394) — sem job de CI; ideal: gerar em runner ligado + PR check de drift.

**Stack**: AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.08.00, Room 2.8.5, Media3 1.11.1, OkHttp 5.5.0, Koin 4.2.2 — tudo confirmado em `gradle\libs.versions.toml`.

## 4) UX & funcionalidades

**Mapa do fluxo actual (do conselho — descritivo, sem bug apontado):** Welcome → Text size → Disclaimer → Setup (QR / Restore / From another device) → Profile → Add source; shell de 3 colunas com preview; Search com recents; Downloads com StorageBar; catch-up com "Go back to…".

**Fricções top (verificadas/inferidas de código):**
1. **4-6 cliques** para tocar num ajuste de player: Definições → grupo → sub-ecrã → abra "Video player" — reforçado pelo B1 (§1) e pela árvore proposta adiante.
2. **Grupos legacy mortos** em `SettingsScreen` L483-710 (8 `RootGroup` com titles traduzidos por nada — filtrados L757). Remover = menos `stringResource` e mais clareza de código (§1-B1).
3. **Typing no TV** para Xtream/MAC: QR+PIN só existe em "Add source" e TMDB key — não no OpenSubtitles nem na edição de fonte (posição do analista 4; extensão por confirmar nos ficheiros).
4. **Favoritos sem bulk actions** (span-select só em `CustomizeItemsScreen`).
5. **Multiview pouco descobrível**: setup prévio em Definições → Video player; sem botão no HUD até estar ligado (analista 4 — verificar `PlayerHud.kt`).
6. **Catch-up escondido**: sem entrada na Home; empty state genérico (existe `EmptyState` em `ui\components\StateViews.kt:63` ✅ — usar em mais pontos).
7. **EPG sem "próxima repetição / próximo episódio"** a partir de programa futuro.
8. **Sem pesquisa por voz do sistema**: `AndroidManifest.xml` sem `android.intent.action.SEARCH` ✅ (0 match) — deep links apurados em `MainActivity` L140-145 servem intents afins, não busca.
9. **Zero haptics/áudio de foco** ✅ — 0 ocorrências de `HapticFeedback` no código do app.
10. **TalkBack fraco** ✅ — padrão `contentDescription = null` (HomeScreen L726; MainActivity L532; padrão repetido), sem descrições de controlos de TV remote.

**Lacunas vs TiviMate/Netflix TV**: sem leanback search global/recomendações no launcher; sem posters de programa nas células do guide (só logos); sem seek preview thumbnails; sem skip intro; HUD sem "próximo programa" via OK+UP; sem presets de provider; sem "recomeçar do início" no canal (só catch-up).

**Árvore de Settings proposta** (≤3 níveis, sub-ecrãs a plain-screen; cada sub-ecrã começa colectando os seus próprios flows — fix B1):
- **Fontes** (Playlists · EPG · Import portal)
- **Reprodução** (Player · Live · Áudio & Legendas · Multiview · Gravações · Por canal)
- **Ecrãs & Navegação** (Sidebar · Categorias · Largura do painel · CH+/CH−)
- **Aparência** (Tema · Accent · Vidro · Tamanho de letra · Telas de fundo)
- **Perfis & Kids** (Perfis PIN · Kids — uso por perfil)
- **Sistema** (Rede · DNS · Backup · Idioma · Diagnóstico · Atualizações · Sobre)

**Quick wins de UX (do conselho, todos com local de encaixe verificado):**
1. `contentDescription` nas bases do shell/Home (padrão já localizado: HomeScreen L726, e L1297-1459 segundo o analista; MainActivity L532).
2. Botão Multiview permanente no HUD com diálogo "Activar?" — termina com o problema de descoberta (§4-5).
3. Linha "Catch-up" na Home + `EmptyState` (`ui\components\StateViews.kt:63`) nos vazios de catch-up.
4. Chips de valor nas sub-ecrãs já aparecem — replicar em Network/DNS (o padrão existe no root, §1-B1 chips).
5. Aviso one-shot de CH+/CH− na primeira troca (o canal continua a ser o instinto do remoto).
6. Som/haptic de foco opcional — hoje zero respostas sensoriais (grep ✅).
7. Matar os grupos legacy de `SettingsScreen` (§1-B1) e actualizar o rótulo "Definições avançadas" (`advancedKeys` L776) para o novo padrão.
8. Consistência: Glass uniforme (GlassSurface + LocalActionSurface) — manter como régua para novas superfícies.

## 5) Ideias de inovação (15)

Pontos de integração confirmados por re-lectura (ficheiros existem): `phase4diagnosticsMetrics.kt`, `player\PlayerHud.kt` (contém `PlayerHudChrome`), `DEFAULT_MULTIVIEW_TILES` em `features\live\LiveScreen.kt`/`SettingsViewModel.kt`, `MpvVideoSurface` em `player-core\…\OwnTVPlayer.kt`, `PlaybackPrefsDao.kt`, `LiveTuneController.kt`, `features\live\LiveScreen.kt`, e `docs\SEGMENTOS_HLS_E_LIMIAR_SUPERIOR_2026-10-11.md`.

| # | Nome | Problema | Solução | Esforço | Diferencial | Onde integra |
|---|---|---|---|---|---|---|
| 1 | Stream-Health Score por canal | Utilizador não sabe porque um canal falha | Score 0-100 (buffer ms, bitrate, descartes, reconexões); troca automática de engine abaixo do limiar; versão incluída no backup v24 | M | Ninguém no espaço IPTV Android TV tem isto visível | `diagnostics\phase4diagnosticsMetrics.kt` → `player\LiveTuneController.kt` (Core) |
| 2 | Pré-aquecimento de rede | Primeira troca de canal lenta | DNS preconnect + HEAD do canal ±1 ao navegar a lista | S | Zapping percebido como "instantâneo" | `LiveScreen.kt` (eventos de foco) → Core `LiveTuneController` |
| 3 | "O que ver agora" 100% local | Home estática em TV | Composição EPG: começa em 10 min / está a meio / retomar + próximo episódio, filtrado por perfil | S/M | Zero-cloud, vantagem vs. apps dependentes de TMDB-isso-só | `HomeScreen.kt` + VMs existentes |
| 4 | Smart Catch-up Flush | Salto a meio do programa | Deteção de gap por segmento → salto automático com marca na barra | M | Corrige frustração nº 1 do catch-up | continua `docs\SEGMENTOS_HLS_E_LIMIAR_SUPERIOR_2026-10-11.md` |
| 5 | Overlay diagnóstico no HUD | Reprodução de bugs remota | Motor/codec/buffer/bitrate/HTTP/score em overlay | S | Relatórios de utilizador vêm úteis | `player\PlayerHud.kt` (`PlayerHudChrome`) |
| 6 | Multiview dinâmico N tiles | Multiview fixo | Tiles extra mantêm áudio+ícone, surface off (`MpvVideoSurface` já nota modo); `DEFAULT_MULTIVIEW_TILES` como base | M | Degradação por tile — bom em boxes fracas | `LiveScreen.kt`, `SettingsViewModel.kt` |
| 7 | "Sala de Bits" (screensaver idle) | TV gratuita em idle | Mosaico 3-6 tiles sem áudio com hora/EPG; sai com tecla M/L | M | Único no génerio live-TV | Shell: reutiliza multiview + relógio |
| 8 | Pesquisa por voz do sistema + direções home | Sem voz no TV (grep ✅ zero SEARCH intent) | `LEANBACK_SEARCH` intent handler + favoritos 1.º nível via D-pad home | M | Cobertura de launcher global | `AndroidManifest.xml` + `MainActivity` L140-145 |
| 9 | Busca "aquele filme com…" | Metadados incompletos | FTS4 + MiniLM on-device (zero-cloud) | L | Nenhuma app IPTV tem busca semântica local | `core\database` FTS + SearchViewModel |
| 10 | Sumário do programa em curso | "O que é isto?" | Regras local + BYOK opcional (TMDB/LLM) | M | Privacidade-first | EPG detail + `PlaybackPrefsDao`/metadados |
| 11 | Auto-Replay de gravações | Saltar às partes boas | Picos de volume + trocas de cena → marcadores "possíveis momentos" | L | Gravações ficam navegáveis | `RecordingEngine` + player marks |
| 12 | Auto-quirks por canal | Quirks manuais demoram | Stream-Health propõe preencher `playback_quirks` | M | A app aprende com uso | `core\database\dao\PlaybackPrefsDao.kt` |
| 13 | Bundle "Hoje" partilhável | Setup repetido família | `.own` v24 sem credenciais (canais/categorias/notas de saúde) via Wi-Fi/Telegram | S | Partilha social rara no espaço | BackupContainer (v24 já planeada) |
| 14 | Kids — painel parental de uso | Kids sem controlo real | DAO local de uso por perfil + dashboard PIN | M | Perfis já têm PIN infra | `ProfilesViewModel` + DAO novo |
| 15 | ZIP "relatório de comunidade" | Suporte à mão | Agregado de diagnostics sem URLs/uso + stats | S | Loop de feedback do projecto (t.me/owntvplayer) | `diagnostics\phase4diagnosticsMetrics.kt` + Backup |

**TOP 5 ROI**: #1, #4, #8, #3, #10.

**Porquê agora — notas para o top 5:**
- **#1 Stream-Health**: a telemetria já está recolhida e o motor de troca existe; o custo é agregação + UI + uma tabela nova na v24 — o valor mais alto por linha de código.
- **#4 Catch-up Flush**: compete directo com o reporte de "a gravação/catch-up salta ou fica congelado"; o doc `SEGMENTOS_HLS…` já caracteriza o limiar superior.
- **#8 Voz do sistema**: o manifest tem os deep links prontos (`MainActivity` L140-145) — é o degrau natural antes de recomendações de launcher.
- **#3 "O que ver agora"**: usa apenas EPG local (nada de rede extra) e resolve a Home estática que a análise UI/UX cruzou duas vezes.
- **#10 Sumário do programa**: reuso de metadados existentes; BYOK deixa o custo on-device e evita lições de privacidade do género streaming.

## 6) Roadmap

**A) Quick wins (1-2 dias)** — ver §1 e §3
1. Keystore em release obrigatória + remover `E:/…` hard-code + comentário corrigido (B2).
2. Indicador de progresso do hero em composable folha com animação própria (B3).
3. `rememberUpdatedState` no prewarm EPG (M2) + coletores condicionais sempre ligados (M4).
4. Remover os 8 `RootGroup` mortos (B1 passo 3) e fix `uiSummary`/prefs comments.
5. Verificação de `done == total` no fim do download (primeira metade de B4), sem digest ainda.
6. Limpeza de repo: apagar/`git rm` 6 vendor zips antigos, mover `work\`, reorganizar 13 `.md` da raiz para `docs\` (§3).
7. `isUniversalApk = false` nos splits e gerar universal só num job manual (Baixa 1, §3).
8. Índice `Map<Long, Int>` para o overlay de canais (Baixa 3) e `.distinctBy`/levemente amarrar a escada `advancedKeys` L776 se houver edição.
9. Remover `BackHandler` redundante de L812 quando `sheetFocused` implique `searchExpanded` (Baixa 2 — clareza, não bug).

**B) 1-2 semanas**
1. `SettingsScreen`: colectores para as rows + catálogo memoizado por inputs estáveis (B1 passos 1-2).
2. Completo B4: digest SHA-256 opcional + estado PARTIAL + resume honesto; testes unit para `DownloadEngine`.
3. URLs Xtream com encoding por segmento + testes de credenciais especiais (M6).
4. Detectar charset M3U (BOM/UTF-16/1251/1256/ISO fallback) (M7).
5. `versionCode` único CI↔local (§3) + tag de 4 componentes suportado.
6. Polling da escada de recuperação para signalizado (M3) + teste coroutines.
7. TalkBack/haptics quick pass: `contentDescription` nas bases (§4) + som de foco opcional.

**C) 1 mês+**
1. Fatiar monolitos na ordem: `LiveViewModel` → `OwnTVShell` → `VideoPlayerSettingsScreen` → `SettingsScreen` (§3 plano).
2. ktlint + detekt nos build files e no CI; ratchet de complexidade.
3. Testes para os controllers extraídos + primeiro compose-ui test; `baselineProfile` gerado em CI runner (§3).
4. Árvore de Settings nova (§4) com sub-ecrãs independentes.
5. Inovações: #1 Stream-Health Score, #4 Smart Catch-up Flush, #8 voz + direções home (§5); depois #3/#10.

## 7) O que já está bem — não retrabalhar

1. **Migrações Room validadas**: `OwnTVDatabase.kt` L119 declara `version = 51` com cadeia explícita `MIGRATION_1_2`… (L158+) e `healSchema`; `OwnTVDatabaseMigrationTest.kt` em androidTest cobre o upgrade.
2. **Crypto de backup**: `BackupCrypto.kt` — PBKDF2 210k + AES-GCM com IV aleatório; correcto, não substituir.
3. **Snapshots Core com anti-fork**: CI valida SHA-256 (`resolve-core` + sanity) e compara `locales.json`.
4. **Parsers densos**: 162 testes unit + 15 androidTest só no Core (XtreamClientTest, EPG, backup, sync).
5. **Ficheiros seguros**: path traversal contido (sanitize + SHA-256 por parte + `ATOMIC_MOVE`); gravações com regras de espaço/stop.
6. **Credenciais**: StalkerAuth token em memória; OpenSubtitles `runBlocking` confinado a IO; audio focus completo no `PlaybackSession`.
7. **Performance consciente**: `StackBlur` com documentação do bug de sign-wrap (L35-42); budgets de demuxer; lazy lists com focus restore sofisticado.
8. **i18n industrial**: 44 idiomas, 26 pastas `values-*` no Core, RTL (`RtlKeys.kt`), ratchet de strings hard-coded com baseline.
9. **CI honesto**: JDK 21 fixado, unit + lint `abortOnError`, secrets mascaradas, dev builds HIGH (99999) instaláveis sobre qualquer release.
10. **UX de base sólida**: shell 3-colunas com preview, Glass consistente (`LocalActionSurface`), EmptyState em `StateViews.kt`, search com recents; stack moderna (AGP 9.4.1 / Kotlin 2.4.20 / Compose 2026.08 / Media3 1.11.1).
11. **Fixes históricos estáveis**: fullscreen/zapping vivem no player-core (`ZapTiming`/`deferCurrentTune`) e a UI está alinhada.

**Perguntas abertas ao dono** (definem o planeamento de B/C): (1) o universal ~104 MB é requisito de alguma distribuição, ou pode sair dos artefactos? (2) prioridade de charsets no detector M3U (windows-1251 vs ISO-8859-1/1252)? (3) aceitar fail-fast de release sem keystore na CI fork? (4) a v24 do backup inclui a tabela do Stream-Health (§5-#1)? (5) qual a box mínima suportada oficialmente — aguarda os dados de §5-#5?

**Recomendação final**: guardar esta base — os riscos estão concentrados em 4 ficheiros de UI/VM monolíticos e em 4 pontos de Core (download, escape, charset, recording reset), não dispersos pelo codebase. Fatiar antes de acrescentar funcionalidades pesadas.

## 8) Ronda 2 — novos bugs, melhorias e inovação (com soluções)

Segunda passagem com 5 analistas novos (player/áudio, EPG/grelha/BD, segurança/rede/update ofensiva, performance/memória para escala, inovação lote 2). Marcações: ✅ re-verificado neste conselho (ficheiro+linhas); ⚠️ parcialmente refutado/reformulado; ❓ mecanismo plausível não confirmado em runtime. Dedupe vs. Ronda 1 marcado com 🔁 — cruzamentos: perf de cold start (§7), robustez gravação/sync/BD (§7-1/5), FTS global (§5-#9), testes dos monolitos (§3), fixes históricos multiview (§7-11). Ledger de verificação completo no Apêndice F.

### 8.1) Bugs

**Player / áudio / legendas** (R2-1 … R2-11)

- **R2-1 — Média ✅ — `setVolume()` ignora o nível pedido (HUD/áudio fixo a 100)** — `player-core\…\OwnTVPlayer.kt` L3744-3748 — *Sintoma*: pedir 50% grava `_volume.value = 100` e aplica 100 no engine activo; `adjustVolume = Unit` (L3749); por consequência o `defaultVolume` (SettingsViewModel L487) e os valores por item `volumeBoost` (PlaybackPrefsDao L40-80, colunas da v51) nunca se aplicam — dados mortos. *Repro*: slider/HUD do volume ≠ 100 → saída idéntica. *Correção*: honrar `percent` com clamp 0-150 (Exo `setVolume(percent/100f)`, mpv `setPropertyDouble("volume", percent)`), escrever `_volume.value = percent` — e decidir a política: o comentário L3743 declara "System/box volume owns gain", pelo que ou se liga o remember por item ao engine, ou se apaga o data-path morto (DAO + UI) de vez.
- **R2-2 — Baixa/Média ✅ — mute não reflete no estado do volume (ícones velhos)** — L3537-3541 — *Sintoma*: `setMuted()` escreve `internallyMuted` mas não toca em `_volume`; `MiniPlayer.kt` L143 e `AudioNowPlayingBar.kt` L291 trocam o ícone só quando `volume <= 0` → mute pelo remoto deixa o ícone "alto". *Correção*: expor `isMuted` (StateFlow) e compor ícone com `muted || volume <= 0` — ou sincronizar `_volume` no toggle (0/100); alinhado com a decisão de R2-1.
- **R2-3 — Média ✅ — troca manual de engine perde a pista de áudio escolhida** — L2230-2312 — *Sintoma*: `toggleVodEngine()` leva a legenda activa (L2245-2248/2278-2280) mas não a pista de áudio; `pendingExoAudio` só é preenchido pelo observador de surround (L1180) e consumido no startExo (L2065-2066); o consumidor com áudio #2 selecionado volta à pista default após o toggle. *Correção*: no ramo →Exo preencher `item.pendingExoAudio` com o track seleccionado; no ramo →mpv re-selecionar `aid` pelo índice/idioma do track activo após o load (como já faz para legendas).
- **R2-4 — Média ✅ — `time-pos`/`duration` lidos como INT64 (posição a 1 Hz)** — L2632-2634 — *Sintoma*: mpv publica double em segundos; `MPV_FORMAT_INT64` trunca para segundos inteiros e emite de 1 em 1 s → HUD salta, retomas/small-seeks desfasados ≤1 s. *Correção*: ler em `MPV_FORMAT_DOUBLE` ×1000 para ms (mantendo Int64 para width/height/estatísticas inteiras) e validar em VOD mpv-first (o caso de uso com melhor perfil para isto).
- **R2-5 — Média ✅ — `mpvAsync` após `release()` lança `RejectedExecutionException`** — L1064-1066 + L4149-4177 + L4143-4146 — *Sintoma*: o `runCatching` está dentro da task; o `execute()` rejeita FORA do catch; `release()` faz `shutdown()` (L4176) e o `onTrimMemory` pode chegar depois (teardown de Activity) → exceção na fila do executor. *Correção*: flag `released` conferida antes de enfileirar + `try/catch` à volta do `execute` (ignorar rejeição pós-shutdown); alternativa mais pesada: recriar o executor on-demand e nunca manter shutdown antes de a instância ficar inalcançável.
- **R2-6 — Média ✅ — mimes/charset por extensão nas legendas da rota Exo** — `player-core\…\ExoSubtitleEngine.kt` L414-418 — *Sintoma*: `subtitleMime` mapeia ass/ssa→SSA e **todo o resto → SubRip** (.sub/.idx/.sup/psb erradas); grep confirma zero leitura de charset → vobsub/GBK/windows-1251 quebram silenciosamente (mesma família do §1-M7). *Correção*: mapear `.sub/.idx→APPLICATION_VOBSUB`, `.sup→APPLICATION_PGS` (conforme header do content), e converter texto para UTF-8 com detecção BOM/1251 (reusar a heurística proposta em §1-M7); invalidar o cache de decode quando o charset difere do exigido.
- **R2-7 — Baixa ⚠️ — latch "áudio undecodable" reporta só o 1.º ficheiro da sessão** — ExoSubtitleEngine L255-257/327 — *Refutação parcial*: o latch É reposto na abertura de cada item (L327) — o texto submetido dizia "nunca reposto"; o que fica de pé é a janela intra-sessão (N ficheiros de legenda externos no mesmo item, só o 1.º reporta). *Correção*: repor a flag após cada `SubtitleConfiguration` adicionada (callback addExternal) em vez de por item.
- **R2-8 — Média/Baixa ✅ (ramo trim) — probe trim aplicado a VOD .ts sem conferir `isLiveContent`** — L773/835 — *Sintoma*: `rawTs = (.ts || /timeshift/) && !effectiveHls` e `trim = rawTs && !forceFullProbe` cortam a análise em VOD .ts (conflito: perde-se a análise de legendas/faixas laterais); a re-verificação em FILE_LOADED só abre ver AUDIO ❓ (não traçado linha-a-linha neste pass). *Correção*: condicionar trim a `isLiveContent` (`trim = rawTs && isLiveContent && !forceFullProbe`) ou re-verificar legendas/extra no FILE_LOADED e re-abrir análise se faltar track.
- **R2-9 — Baixa/Média ✅ — janela live/seek-to-edge é no-op no contrato do engine (barra DVR morta em mpv)** — `player-core\…\PlaybackEngine.kt` L46-49 (`liveSeekWindow → NO_LIVE_WINDOW`, `seekLiveBy {}`, `seekToLiveEdge {}`), sem override no OwnTVPlayer (grep zero) — *Sintoma*: em mpv a barra live desenha mas não faz seek. *Correção*: implementar DVR window no mpv (expor a janela DVR e mapear o seek nesse intervalo) ou esconder a barra quando `liveSeekWindow.value == null` e apontar para o fallback timeshift local que o app já tem.
- **R2-10 — Baixa ✅ — `END_TOLERANCE_MS` dispara auto-next em VOD curto** — L266-278 — `tolerance = min(8 s, duração/4)` — VOD <32 s aceita "fim" com até 25% restante. *Correção*: em VOD usar `min(8 s, duração/10)` e exigir `position >= duration - min(8s, dur/10)` + tempo mínimo decorrido; live mantém a regra actual (watchdog diferente).
- **R2-11 — Baixa ⚠️ — `pendingFps` re-aplica hint AFR no `surfaceCreated` sem reler o setting** — `app\…\player\MpvVideoSurface.kt` L38-39/66-68 — *Refinação*: o `update {}` Compose (L123) aplica `if (autoFrameRate) fps else 0f` na recomposição (o texto submetido desprezava isto); a janela restante é o recreate de Surface sem recomposição (backgrounding) — o último FPS com AFR off é re-afirmado. *Correção*: no `surfaceCreated` ler o setting actual do player e aplicar 0 quando off (ou limpar `pendingFps` quando a setting muda).

**EPG / guias / timeshift** (R2-12 … R2-20)

- **R2-12 — Alta ✅ — lembretes de programa são só em memória (não sobrevivem a kill/reboot; nem tocam em background)** — `app\…\features\epg\ProgrammeReminderStore.kt` L41-160 (singleton `shared` com `MutableStateFlow`), disparo único em `EpgViewModel` L278/301 `evaluateDue(..., inForeground)`; grep bloqueio: zero `AlarmManager|WorkManager|PendingIntent` para lembretes (WorkManager existe só para backups/updates). *Sintoma*: recordar um programa → fechar a app (ou reboot) → nada toca; deixar a app em background → nada toca (o store assume que ON_STOP silencia). *Correção*: persistência + agendamento — (a) guardar em Room (tabela nova na v24) ou DataStore por perfil, (b) agendar `AlarmManager.setExactAndAllowWhileIdle` + `WorkManager` para a janela de lead (60 s) com receiver de boot, (c) notification/toast quando fora de foreground; alternativa honesta (menos trabalho): renomear a UX para "lembrete da sessão" e não prometer sobrevivência.
- **R2-13 — Alta ✅ — `XmltvTime` aceita só offsets com minuto; "+02" devolve 0 em silêncio** — Core `core\…\parser\XmltvTime.kt` L46-52 — trace: t="2026…+02" → `digitsAt(minuteIndex=15, 2)` fora da string → `return 0` para o programa inteiro (startMs=0) e o feed perde-se sem erro. *Sintoma*: guide vazio por provider com offsets curtos, sem log de causa. *Correção*: tratar MM ausente como "00" (offset HH completo) e, antes de qualquer `return 0` de offset, contabilizar "feed com N timestamps rejeitados" em `EpgSource.lastError` + log único com sample — para visibilidade de causa.
- **R2-14 — Média ⚠️ — serverTimeZone Xtream fixo "GMT±hh:mm" sem DST (refutado em parte: já há mapeamento IANA)** — Core `XtreamClient.kt` L1112-1125 + `CatchupClock.kt` L22-25 — o texto submetido ignorava que L1113-1114 primeiro tenta o nome do painel como zona IANA e o delta é recalculado por sync; o que fica: quando o painel não dá IANA, o fallback "GMT+02:00" é fixo (sem DST) e `validatedZone` aceita → ±1 h em catch-up/rewind/gravações após transição. *Correção*: preferir IANA (já feito), expor override manual por fonte (o enum `CatchupTimezone.MANUAL` existe), e quando o fallback GMT estiver activo, detectar gap (delta entre o offset aplicado e o `timestamp_now` recebido) para avisar o utilizador.
- **R2-15 — Média ❓ — `CatchupJumps` instant/point por delta wall-clock (DST round-trip mostra minuto errado)** — mecanismo plausível, não traçado linha-a-linha neste pass. *Correção*: nos pontos de dia fixo ("ontem às HH:mm") usar `ZonedDateTime`/`Calendar` no fuso resolvido da fonte (CatchupClock) em vez de aritmética de ms de `now`, e testar com TZ Europe/Lisbon em 29/03.
- **R2-16 — Média ⚠️ — janela de histórico coerceda para ≥7 dias (refutado em parte)** — `GuideHistoryPolicy.kt` L12-13 — *Confirmado*: `windowStart = declaredDays.coerceIn(7, MAX)` expõe rewind além do provider para catchupDays 1-6 (MiniGuideOverlay L46 usa-o). *Refutado*: a parte "idem canCatchup:22" — canCatchup (L20-23) respeita `declaredDays > 0` (coerce só ao máximo). *Correção*: `coerceAtMost(MAX)` quando declaredDays>0 (default 7 apenas para 0/indisponível) e UI a mostrar a janela real "arquivo até X dias" por canal.
- **R2-17 — Média/Baixa ✅ (gatilho ❓) — limpeza local-timeshift aborta com `check(it.delete())`** — `LiveViewModel.kt` L301-304 — *Confirmado*: `forEach { check(it.delete()) }` lança `IllegalStateException` no 1.º falhado e a limpeza aborta. *Refutável parcial*: a premisa "lease impede delete" é OS-dependent (unlink com fd aberto funciona em Linux); gatilho real por confirmar. *Correção*: `runCatching { it.delete() }` por ficheiro + varrer os leftovers por idade na próxima sessão; log de falhas (não abortar o fluxo de abrir timeshift).
- **R2-18 — Média/Baixa ✅ — servidor local-timeshift fecha excedentes sem resposta** — Core `LocalTimeshiftServer.kt` L26/33-34 — `Semaphore(4)` + `tryAcquire()` falhado → `client.close(); continue`: o 5.º client (ex. player abre vídeo+áudio+subtitle em burst) não recebe resposta e espera timeout. *Correção*: responder `503 + Retry-After: 2` antes de fechar (3 linhas com o framing já existente) ou fila pequena (bounded executor) para absorver bursts.
- **R2-19 — Baixa ✅ — quota de timeshift exige `1..maxBytes` por segmento** — Core `LocalSegmentStore.kt` L42/L57 — `require` lança antes da escrita se um segmento >256 MB (default) chegar → kill de sessão sem fallback para descartar a parte. *Correção*: rejeitar o segmento fora do range (log + continue) em vez de lançar; usar o mesmo padrão de caps do resto do Core — uma parte grande não devia matar a sessão.
- **R2-20 — Baixa ✅ — tokens catch-up com formato não suportado mantidos verbatim** — Core `CatchupUrl.kt` L63-83 — `{utc:%Y}` com strftime cai no `else -> match.value` (match mantido literal) e o provider recebe/template não-resolvido; `ArchivePlaybackConfig` com tokens Homegrown não reconhece `%`. *Correção*: mapear os strftime mais comuns (%Y→Y, %m→m, %d→d, %H→H, %M→M, %S→S) e, quando houver `%` no arg, log + usar formato ISO fallback (ou renderizar com `java.text.SimpleDateFormat` a partir do instant) em vez de manter o literal.

**Segurança / rede / update / autorização** (R2-21 … R2-28)

- **R2-21 — Alta ✅ (com mitigação existente PIN-gated anotada) — Companion/server local sem TLS: bind 0.0.0.0, PIN na query-URL, sessionPassword e creds transitam em plaintext** — Core `CompanionHttpServer.kt` L140 (bind em `InetSocketAddress(port)` — todas as interfaces) + `CompanionLink.kt` L23 (8089) + `CompanionHtml.kt` L226/250/266/318/347/389/431/485/541 (PIN em `?pin=`, incl. `/backup.own?pin=`) + `LocalSyncClient.kt` L127 (`X-Companion-Pin` plaintext) + `LocalSyncManager.kt` L193 (`.put("session", sessionPassword)` no JSON do `/sync/hello`). Mitigações verificadas: `/sync/hello` e `/backup.own` exigem `requireSyncAuth` (PIN/secret de pairing, L289/321-331; lockout de PIN no companion) — então o atacante precisa mesmo de sniffar o PIN; mas em rede própria LAN isso é 1 pacote (query-URL) e depois tem o segredo de 256 bits e o .own com passwords de playlists. *Correção*: (1) PIN nunca em query — mover todo para `X-Companion-Pin` (o client já usa header; HTML/formulários podem POST) e nonce one-shot no QR; (2) selar sessão com `HKDF(deviceSecret)` → canais opacos (ou TLS-PSK local) em vez de enviar sessionPassword/passwords no canal; (3) ACL no bind: aceitar RFC1918/loopback + rejeitar interfaces fora dessas faixas; (4) apagar o PIN da query do link `/backup.own` (usar referência de sessão).
- **R2-22 — Média ✅ — UpdateManager sem digest SHA-256 do asset** — Core `update\UpdateManager.kt` L203/244 — único `verifyApk(apk)` (assinatura PackageInstaller; um replacement com a mesma chave passa); grep zero sha/digest — a GitHub API expõe digest por asset. *Correção*: exigir/validar SHA-256 do APK (da release metadata ou pin em registry local) antes de `installApk`; fail-fast se o digest não bater.
- **R2-23 — Média ✅ — DashManifest sem secure-processing (XXE/billion-laughs por portal malicious)** — Core `recording\DashManifest.kt` L78 — `DocumentBuilderFactory.newInstance()` e grep zero `setFeature/expandEntityReference=false` — doctype não desligado no parser da gravação. *Correção*: 3 features de hardening (`disallow-doctype-decl`, `secure-processing ON`, `expand-entity-references false`) ou trocar por `XmlPullParser` (já usado no XmltvParser) — e cap de tamanho do doc contra billion-laughs.
- **R2-24 — Média ✅ — PIN de perfil comparado em `==` e sem rate-limit** — Core `util\Pin.kt` L27 (`digest(salt, pin) == parts[1]`) + `ProfileGate.kt` L94-104 (sem delay/contador — o lockout 10+delay existe só no PIN do Companion) — *Correção*: `MessageDigest.isEqual(...)` em `Pin.verify` e contador/atraso incremental após N erros no gate do perfil; também pode separar hash de PIN de perfil (PBKDF2) se o objetivo é resistir a backup selado.
- **R2-25 — Média ✅ — `usesCleartextTraffic="true"` global** — `app\src\main\AndroidManifest.xml` L49 — qualquer http TMDB/updates/BD/Coil sem CA-controle. *Correção*: `networkSecurityConfig` com cleartext off global + domínios IPTV fixos na allowlist (M3U/xmltv `http://` das fontes continuam; app principal usa `base-config cleartextTrafficPermitted=false`).
- **R2-26 — Média ⚠️ — Kids detecta categorias 18+, mas títulos explicitos passam na pesquisa** — Core `content\AdultCategoryClassifier.kt` L12-30 — *Refinação*: é **category-only por design** (doc própria: false positives com "Sex and the City"), e os markers não contêm "sexo/uncensored/hardcore" (conferido por leitura + grep). *Correção*: só com opt-in por perfil: matcher título-level com allowlist ratings (TV-MA/18) na franja Kids, aplicando o filtro por corrente quando o provider entrega rating (senão fica isolado a palavras soltas) — sem trocar a política category-only global.
- **R2-27 — Baixa ✅ — `file_paths.xml` expõe a raiz do armazenamento externo** — `app\src\main\res\xml\file_paths.xml` L6-9 — `external-path path="."` (shared_media) + `files-path path="."` + `cache-path path="."` grantable por FileProvider. *Correção*: subpastas específicas (`downloads/`, `updates/`, `movies/`) e eliminar a raiz `.` — os consumidores do provider apontam para essas pastas.
- **R2-28 — Baixa ✅/❓ — M3U sem caps de tamanho/itens no parser Core** — Core `M3uParser.kt` (grep zero `maxBytes/cap/limi`); Stalker tem caps referenciados; `file://` na import ❓ (caminho app-side não traçado neste pass). *Correção*: cap de bytes (~64 MB) e de itens (~150k) checados antes de parsear, com erro claro registado na fonte; teto análogo no import de ficheiros locais.

**Não-bugs verificados (amostragem das validações "OK" da Ronda 2)** — ✅ `canRetrieveWindowContent="false"` na acessibilidade (`auto_start_service.xml` L11) e receiver de boot/exported no manifest conferidos por amostragem; ✅ caps/anti-zip-slip do `BackupContainer` conhecidos (não re-traçado linha-a-linha neste pass); ✅ strings do HTML do Companion escapadas (amostrado); ✅ cleanup de `subshift_*`/`sessionExternalSubs`/timeshift scrub sem duplo desconto; ✅ `LiveEnginePool`/Multiview/`FrameRateController` ok (🔁 alinha com fixes históricos §7-11); ✅ `EpgSyncWorker`/`Room v51 heal/índices WAL`/`LocalTimeshiftSession/Store/Recovery` sólidos (🔁 §7-1/5); ✅ `EpgMatcher` fuzzy e `resolveCatchupTimeZone` per-source ok; ❓ audio-focus gain/SpeedDialog não re-traçados neste pass; ✅ "zero hoje" reconfirmado por grep: `TextToSpeech`=0, `enterPictureInPictureMode`=0, `Nsd/mDNS`=0 (app).

### 8.2) Melhorias — arquitetura / performance / UX (lote 2)

1. **FTS4 nas secções de pesquisa** ✅ — `MovieDao` L240-263 (searchAll/searchInCategory/searchFavorites/searchHistory) e `ChannelDao` L370-374 (searchAll/searchInCategory) sam `LIKE '%q%'` (o próprio DAO anota em L251 que o like com prefixo escaneia todas as ~170k linhas por tecla); FTS4 existe e é usado só na busca global (`channels_fts MATCH`, L365) — *solução*: reutilizar `ftsQueryFor`+`MATCH` nas queries de secção com fallback LIKE para queries com símbolos e subconsulta FTS para favoritos/histórico (join) — acaba o full-scan por tecla.
2. **EPG sem `indexOfFirst` por row + prefetch coalescido** ✅ — `EpgViewModel.kt` L501-529 — *solução*: reconstruir `HashMap<Long,Int>` quando `channels` muda (o padrão do fix Baixa 3 §1) e coalescer a prefetch (`conflate`/debounce 150 ms; guard por cacheRevision já existe) para estancar o churn cancel/relaunch por linha.
3. **LiveHero: 2 AsyncImage do mesmo logo (um com blur runtime)** ✅ — `HomeScreen.kt` L1336-1350 — *solução*: 1 AsyncImage 80 dp + fundo do cartaz pré-desfocado 1× (cache por `logoUrl` com invalidação no reset da grelha) — menos 1 bitmap e sem custo de RenderEffect por frame; fix junto ao §1-B3 (mesmo registo do hero).
4. **`favoriteIds` Eagerly → WhileSubscribed** ✅ — `LiveViewModel.kt` L1102-1105 — trocar `SharingStarted.Eagerly` por `WhileSubscribed(5000)` para parar de requery numa sync sem UI (carregar o custo apenas quando alguém mostra).
5. **`LocaleStore.readBlocking` 2× na main iniciar** ✅ — `OwnTVApp.kt` L63 (`attachBaseContext`) + `MainActivity.kt` L136 (novamente fora da 1.ª frame) — *solução*: cache estático a nível de processo (`AtomicReference` com o tag resolvido) e verificação de validade — economiza I/O síncrono antes da 1.ª frame.
6. **Portal EPG sem `ProgrammeHashTracker` no batch** ✅ — Core `EpgRepository.kt` L498-511 (portal) vs L324-339 (XML, tracker em L244) — *Correção*: aplicar o tracker igual no batch do portal para evitar re-upsert completo de 50k+ rows por sync.
7. **GuideCore pré-medir por largura quantizada** ✅ — `GuideCore.kt` L179-181 — medir por célula (2× por draw) no Canvas — *solução*: cache de measurements por (largura quantizada, style, texto), invalidado na mudança de janela/idioma (o cache por row já existe no `GuideRowCache`).
8. **EpgSourceStore: parse vazio silencioso** ✅ — Core `EpgSourceStore.kt` L152-169 (`getOrDefault(emptyList())`) — *solução*: expor `lastError` global e manter o raw para diagnóstico até o próximo parse válido (a fonte aparece "sem EPG" em vez de "desaparecida").
9. **EpgMatcher NUMBER_WORDS só inglês** ✅ — Core `epg\EpgMatcher.kt` L60-64 (`one..ten`) — *solução*: mapa extra com termos comuns ("een/uno/um/tres/deux…") ou extrair dígitos por regex quando o título começa por palavra número, antes da normalização. Baixa prioridade (Stream-Health cobre a maioria dos casos).
10. **Testes DST/golden para catch-up** ✅ (🔁 §3 gaps) — *solução*: golden tests de `CatchupJumps/CatchupUrl/LiveTimeshift` com TZ Europe/Lisbon 29/03 (transição) + macro-teste de tokens `{utc}`; entra nos jobs de CI em vez de re-run manual.
11. **DnsConfig flag "bloquear em falha"** ✅ — Core `network\DnsConfig.kt` L76/127/132/182 (fallbackToSystem=true default silencioso) — *solução*: UI toggle "falhar sem fallback" + aviso/diagnóstico quando o fallback aconteceu, para não degradar a privacidade às escondidas do utilizador.
12. **networkSecurityConfig com domínios IPTV permitidos** ✅ — tratada como R2-25 (§8.1); ver ali.
13. **VolumeBoost: ligar ou apagar** ✅ — colunas/UI/DAO de `volumeBoost` existem (PlaybackPrefsDao L40-80) mas zero usos do LoudnessEnhancer (grep app+core) — *solução*: decidir entre remover (DB migration v52 menor + limpar UI/DAO) ou ligar ao engine com ganho seguro (e opção «off» por default).
14. **Nudge de legenda sem re-prepare** ✅ (Baixa; cache mitigado) — `ExoSubtitleEngine` re-prepara o VOD em cada setSubtitleDelayMs — *solução*: aplicar shift no path do player (track de formato text já carregado, com re-cache do shift) e reservar o re-prepare para bitmap/PGS/VOBSUB.

### 8.3) Inovação — lote 2 (18 ideias)

| # | Nome | Problema | Solução | Integração | Esforço | Diferencial |
|---|---|---|---|---|---|---|
| 1 | Espelhos Vivos intra-playlist | Mesmo canal com 2+ URLs na mesma fonte falha em cascata | Índice duplicado por EPGid/título+qualidade; HEAD idle de 1-byte; failover silencioso na mesma fonte; re-sync após salto de rede; backup v24 | M3uSyncer + LiveViewModel + PlaybackPrefsDao | M | Primeira a usar o .m3u como redundância nativa |
| 2 | Substituto Inteligente cross-fonte | O único "backup" hoje é outro canal | Candidatos noutras fontes por título normalizado/grupo/EPGid, ordenados por Stream-Health; "Avançar para substituto?" | LiveRecoveryController + ChannelDao | M/L | Failover entre fontes que nenhum rival faz |
| 3 | Radar "possível down" | Utilizador re-tenta à toa | Heartbeat HEAD/1-byte em favoritos/visitadas (JobScheduler idle); tag "possível down · última prova 12 min" na grelha | LiveViewModel + StreamLog | M | Sinal honesto em vez de spinner infinito |
| 4 | Templates de setup por provider | Setup raiz repetido | Presets Xtream/M3U-RU/Stalker (engine, buffer, charset sniff, quirks, track language, deinterlace) | ManageSourcesScreen + SetupViewModel | S/M | Onboarding <60 s para o provider X |
| 5 | Tracks declarativas por perfil | "DUBLADO PT" precisa procurar na mão cada vez | Fuzzy match PT-PT/Português/Dubl/DUAL/Leg na abertura, fallback idioma do sistema; por perfil | PlaybackSession + Profiles | M | Preferência de idioma por perfil offline |
| 6 | Mapa de Loudness por canal | Volume salta entre canais | LUFS aproximado por canal; curva ≤±6 dB; guardado junto ao Stream-Health; passthrough excluído | PlaybackSession + diagnostics | M/L | Continuidade tipo broadcast sem misturador externo |
| 7 | Adaptação de cena escura anti-ABL | ABL/bloom em cenas escuras | Luma ~1 Hz + gamma/brilho suave mpv/Exo; por defeito OFF | player-core render path | M | UX em boxes com ABL de fábrica |
| 8 | Deinterlace automático por canal SD | vp_deinterlace hoje é manual | Res ≤576/480 + métrica de interlace para auto-ON | player-core deinterlace | S/M | Menos configurações para cenas SD |
| 9 | Modo Noite DRC agendado | Volume/dinâmica à noite irrita | Range compression + reduzir boost >100% por horário/perfil | PlaybackSession + scheduler | S | Sem par com agendamento de perfil |
| 10 | Guildas de Saúde (.own sem creds) | Não há social layer de "canal está caindo" | Export .own com nome salgado-HMAC + score/última prova; união fuzzy "falhou para outros também" | BackupContainer + §5-#13 bundle | M | Colaboração sem cloud |
| 11 | Watch Party LAN | Sincronização entre devices é manual | mDNS; host controla; guest sintoniza com clock-anchor+drift; áudio no host ou escolha | mDNS + Companion (infra LAN existente) | L | Único no espaço IPTV TV |
| 12 | Auto-Manual de Manutenção | Diagnóstico sem linguagem humana | Agregar PlaybackErrorLog canal×tipo×hora → texto/ZIP "costuma falhar às 21h" | PlaybackErrorLog + diagnostics | M | Relatório de suporte a partir do device |
| 13 | Automações local IFTTT-like | Fluxos repetidos sem automação | Regras JSON: abrir app → canal X; fonte cai → trocar Y; começa favorito EPG → badge | arranque + backup + StreamLog | L | Extensibilidade local sem servidor |
| 14 | RecommendationRow de Canais/EPG | Launcher não mostra LIVE | O launcher já publica Movie/Series; estender "continuar em directo"/"a seguir no favorito" | LauncherIntegrationRepository/TvHomeRepository | M | Presença no launcher de TV |
| 15 | PiP de canal secundário | Ver dois canais sem interromper | `enterPictureInPictureMode` (zero hoje) no Android 8+; feature-gated | MainActivity + player host | S/M | PiP nativo com qualidade reduzida |
| 16 | Cor dinâmica Material You | Accent fixo | Paleta do backdrop/poster → accent por programa; opt-in; cache LRU | theme + TMDB/Coil | S/M | Personalização sem settings extra |
| 17 | Setup narrado por TTS | Onboarding sem voz auxiliar | `TextToSpeech` (zero hoje) coordenado com TalkBack; só quando acessibilidade activa | Setup + Welcome | S | Acessibilidade real no first-run |
| 18 | Combos D-pad long-press no player | Atalhos do HUD custam foco | LEFT+UP (dados técnicos), DOWN-hold (radar), OK-hold 2× (multiview); ChannelPressInput já tem base de press | PlayerHud + ChannelPressInput | S | Atalhos tipo "pro" sempre disponíveis |

TOP 5 lote 2: Espelhos Vivos; Substituto Inteligente; RecommendationRow canais; Tracks declarativas; Watch Party LAN.

## 9) Reavaliação final — inventário completo (Ronda 1 + Ronda 2)

Veredictos: ✅ válido · ⚠️ parcial · ❌ refutado · 🔁 já coberto · ❓ por confirmar. Solução: referência à secção que a detalha. A Ronda 2 não contrariou nenhum veredicto da Ronda 1 — os 4 ajustes afectam apenas os textos submetidos da Ronda 2 (listados no fim).

### 9.1) Bugs Ronda 1 (§1)

| Achado | Veredicto | Solução validada |
|---|---|---|
| B1 — 44 flows no root Settings + catálogo restart | ✅ | ver §1 (colectores por row + catálogo fora da recomposição) |
| B2 — release assinado debug key + `E:/…` hard-code | ✅ | ver §1 (keystore exigida em release; env/property) |
| B3 — hero recomposto ~12×/s | ✅ | ver §1 (composable folha + animação); R2 adiciona o duplo blur do LiveHero (§8.2-3) |
| B4 — truncado gravado COMPLETED | ✅ | ver §1 (`done==total` + digest + PARTIAL) |
| M1 — deep link re-dispatch no recreate | ✅ | ver §1 (consumir/clear intent) |
| M2 — prewarm EPG aos 20 s duplicado | ✅ | ver §1 (rememberUpdatedState) |
| M3 — polling 20 Hz na escada | ✅ | ver §1 (signalizado; sem `while(true) delay(50)`) |
| M4 — coletores condicionais trocam | ✅ | ver §1 (sempre ligados) |
| M5 — backdrop re-blur ("leak" sub-claim) | ✅ (leak ❌) | ver §1 (quantizar key do blur) |
| M6 — Xtream cred sem escape | ✅ | ver §1 (encoding por segmento) |
| M7 — M3U assumido UTF-8 | ✅ | ver §1 (sniff BOM/1251/1256/ISO); padrão estendido a legendas Exo (R2-6) |
| Baixa 1 — universal APK ~104 MB | ✅ | ver §1 (`isUniversalApk = false`) |
| Baixa 2 — back 2 presses na busca | ⚠️ reformulado | ver §1 (padrão Android; `enabled` mutuamente exclusivos) |
| Baixa 3 — `indexOfFirst` overlay | ✅ | ver §1 (Map<Long,Int>); padrão repetido no EPG (§8.2-2) |
| Baixa 4 — poll 32 ms press-and-hold | ✅ | ver §1 (Animatable/frames) |
| Baixa 5 — split `&` em headers | ✅ | ver §1 (tornar canonical antes de partir) |
| Nota — mpvExecutor shutdown sem await | ⚠️ (§1: parcialmente refutado) | ver §1; novo ângulo pós-release coberto por R2-5 |

### 9.2) Hipóteses Ronda 1 (§2)

| Hipótese | Veredicto | Solução/estado |
|---|---|---|
| RecordingEngine media-sequence reset | ⚠️ | mecanismo ✅ / gatilho provider ❓ (§2); fix: detectar reset (nova sessão/parte) |
| PlaybackSession scope sem SupervisorJob | ⚠️/❓ | estrutura ✅ / impacto runtime ❓ (§2) |
| M3uSyncer renumeração episódios | ❓ | §2 (gatilho: show que atravessa flush sem SxxEyy) |
| EPG/gravação com relógio sem NTP wait | ⚠️ reforçado | R2 confirma 2.ª causa de guide vazia: XmltvTime "+02"→0 silencioso (R2-13) → fix prioritário |
| Checksum libmpv (Maven externo) | ❓ | §3; distinto do digest do update APK (R2-22, este já ✅ como novo) |
| Emissão periódica UiPerformanceSummary | ❓ | unchanged (§2) |

### 9.3) Assuntos de arquitetura/qualidade (§3)

| Assunto | Veredicto | Solução validada |
|---|---|---|
| Monolitos (SettingsScreen/VideoPlayerSettings/LiveViewModel/OwnTVShell) | ✅ | ver §3 (plano de fatiação 1-4) |
| DI/Koin bindings 27/31 | ✅ | ver §3 (documentar; remédio via fatiação) |
| versionCode divergente CI↔local | ✅ | ver §3 (helper único ou code no tag) |
| Higiene repo (7 zips, work\, .md) | ✅ | ver §3 |
| Namespace duplicado `core.i18n` no app | ✅ | ver §3 (mover p/ `features.i18n`) |
| Testes — gaps nos monolitos | ✅ (🔁 §3) | ver §3; lote 2 acrescenta DST/golden catch-up (§8.2-10) |
| ktlint/detekt ausentes | ✅ | ver §3 + CI abortOnError já aguenta |
| Baseline profile manual | ✅/❓ | ver §3 (4 macrobenchmarks prontos mas sem CI persistida de resultados) |
| Supply-chain libmpv | ❓ | §3; complemento novo: digest do update ✅ (R2-22) |
| (novo) Cold-start reality check | 🔁 §7 | R2 confirma "boa realidade" e sem regressões; único gap: persistir benchmarks em CI |

### 9.4) FRICÇÕES / lacunas / quick-wins (§4) e ideias (§5)

| Grupo | Veredicto | Observação |
|---|---|---|
| Fricções 1-10 (cliques, grupos legacy, QR/PIN, bulk, multiview, catch-up escondido, EPG next, voz, haptics, TalkBack) | ✅ | ver §4; quick-wins 1-8 mantêm; combos D-pad (§8.3-18) cobrem atalhos; "PiP/TTS zero hoje" reconfirmado por grep ✅ |
| Lacunas vs rivais (launcher, guide cells, thumbs, skip intro, presets, restart) | ✅ | ver §4; PiP (§8.3-15) e RecommendationRow (§8.3-14) são novos destaques da Ronda 2 |
| #1-#15 ideias | ✅ | ver §5; TOP 5 ROI intacto. Cruzamentos lote 2: #3 ↔ E-14 (launcher), #12 ↔ E-1/2 (quirks/automações), #13 ↔ E-10 (guildas), #8 reconfirmado (zero SEARCH intent) |

### 9.5) Achados Ronda 2 (§8) — veredictos finais

| ID | Achado | Veredicto | Solução |
|---|---|---|---|
| R2-1 | setVolume ignora `percent` (fixo 100) | ✅ | honrar percent + decidir política (§8.1) |
| R2-2 | mute não reflete ícones | ✅ | expor isMuted / sincronizar state |
| R2-3 | toggle engine perde pista áudio | ✅ | carry pendingExoAudio/re-select `aid` |
| R2-4 | time-pos/duration INT64 (1 Hz) | ✅ | MPV_FORMAT_DOUBLE ×1000 |
| R2-5 | mpvAsync pós-release Rejected | ✅ | flag released + try/catch execute |
| R2-6 | legendas Exo mime/charset | ✅ | mimes correctos + conversão UTF-8 |
| R2-7 | latch undecodable "nunca reposto" | ⚠️ | reset existe por item (L327); restante intra-sessão — repor por config |
| R2-8 | probe trim sem isLiveContent | ✅ | condicionar trim; re-verificar no FILE_LOADED |
| R2-9 | live seek no-op no engine | ✅ | DVR window mpv ou esconder barra |
| R2-10 | END_TOLERANCE dispara em VOD curto | ✅ | min(8 s, dur/10) + tempo mínimo |
| R2-11 | pendingFps re-assert no surfaceCreated | ⚠️ (refinado) | janela de recreate existe — limpar `pendingFps` ao mudar o setting |
| R2-12 | lembretes in-memory, sem fono | ✅ Alta | persistir + Alarm/WorkManager + boot; ou UI "da sessão" |
| R2-13 | XmltvTime "+02" → 0 silencioso | ✅ Alta | aceitar HH; visibilidade de feed rejeitado |
| R2-14 | Xtream TZ fixo sem DST | ⚠️ | IANA path já existe; restante override/manual + gap detect |
| R2-15 | CatchupJumps DST wall-clock | ❓ | ZonedDateTime no fuso da fonte + golden tests |
| R2-16 | janela histórica coerceda 7 d | ⚠️ | windowStart ✅ / canCatchup refutado; coerceAtMost + janela real |
| R2-17 | limpeza aborta com check(delete) | ✅/❓ | runCatching + sweep por idade; gatilho OS ❓ |
| R2-18 | servidor 4 conns fecha sem resposta | ✅ | 503+Retry-After / fila curta |
| R2-19 | quota exige segmento ≤ cap | ✅ | rejeitar segmento (log+continue), não matar a sessão |
| R2-20 | template catch-up % verbatim | ✅ | map strftime → tokens; fallback ISO |
| R2-21 | Companion sem TLS/PIN query/session | ✅ Alta | header+nonce via QR; HKDF/PSK; ACL RFC1918 |
| R2-22 | update sem SHA-256 | ✅ | validar digest antes de install |
| R2-23 | DashManifest XXE | ✅ | features secure-processing / XmlPullParser |
| R2-24 | PIN `==` + sem rate limit | ✅ | MessageDigest.isEqual + delay/counter |
| R2-25 | cleartext global | ✅ | networkSecurityConfig allowlist |
| R2-26 | Kids títulos explicitos | ⚠️ | é category-only por design; matcher título opt-in por perfil na busca Kids |
| R2-27 | file_paths raiz inteira | ✅ | subpastas específicas |
| R2-28 | M3U sem caps | ✅/❓ | caps de bytes/itens; file:// por confirmar |

### 9.6) Melhorias e inovação lote 2

| Grupo | Veredicto | Observação |
|---|---|---|
| §8.2 melhorias 1-14 | ✅ | cada qual com solução concreta; M12 = mesma solução da R2-25; M10 repete o gap de testes (🔁 §3) |
| §8.3 ideias 1-18 | ✅ | integrações amostradas: TTS/PiP/Nsd "zero hoje" ✅ por grep; demais por confirmar em implementação. TOP 5: Espelhos Vivos, Substituto Inteligente, RecommendationRow, Tracks declarativas, Watch Party |

### 9.7) Ciência — refutações/ajustes aplicados nesta ronda

1. **A7 (latch legenda)** — texto "nunca reposto" ❌: `undecodableAudioReported` é reposto na abertura de cada item (ExoSubtitleEngine L327); restante por sessão verdade.
2. **B5 (janela catch-up)** — a alegação "idem canCatchup:22" ❌: canCatchup respeita `declaredDays>0`; só `windowStart` coerce para ≥7.
3. **B3 (Xtream TZ)** — "fixo 'GMT±hh:mm' sem mapeamento" ❌ em parte: IANA-first em XtreamClient L1113-1114; o fixed-GMT é o fallback.
4. **A13 (pendingFps)** — janela mais estreita do que o texto submetido: o `update {}` Compose corrige na recomposição; bug real apenas via `surfaceCreated` sem recreate compose.
5. **B13 (LocalTimeshiftServer)** — confirmado como descrito (`tryAcquire` → fecho sem resposta); alegação exacta mantida.
6. Nenhuma evidência da Ronda 2 contrariou veredictos ou contagens da Ronda 1 (§1.0/Apêndice B permanecem).

## 10) Ronda 3 — novos achados (com soluções)

Terceira vaga multi-agente: 41 alegações do lote 3, cada uma re-lida no ficheiro/linhas citados (app + snapshot Core `e2081e78…`). Marcas usadas nesta ronda: ✅ confirmado no código · ⚠️ parcial/matiz (alegação ajustada) · ❌ alegação refutada (o mecanismo já protege) · ❓ não re-traçado individualmente nesta síntese (ledger no Apêndice G). Os IDs R3-* mantêm a numeração acordada do lote: Perfis/Backup/Kids R3-1..R3-8, Sync/Fontes R3-9..R3-16, Pesquisa/TMDB/Multiview R3-17..R3-30, PVR/Downloads R3-31..R3-41.

### 10.1 Bugs novos — por domínio

**Perfis / Backup / Kids**

- **R3-1** ✅ **O gate "Who's watching?" é contornável pelo fluxo "Adicionar perfil".** *Ficheiro/linhas:* `features\profiles\ProfileGate.kt` L89 (`item { AddTile(onClick = onAddProfile) }` no ecrã de escolha), `MainActivity.kt` L429-434/L456, `ProfileGateSessionViewModel.kt` L82-91, `features\setup\SetupWizard.kt` L101/L145-156/L200-204/L249. *Sintoma:* do próprio gate, o tile "Adicionar perfil" abre o onboarding, que termina com entrada directa no shell — sem PIN do perfil, sem PIN de arranque e sem passar pela sessão verificada. *Repro:* ecrã de escolha → "Adicionar perfil" → concluir wizard → shell aberto num perfil novo (kids ou não) sem verificação alguma (~1 minuto). *Correção:* exigir credencial de "adulto" (PIN de setup/`startup_mode`) antes de `onAddProfile` e re-verificação do gate após criar perfil; no wizard, não assumir perfil activo em passos posteriores. *Severidade:* Alta. 🔗 R2-24/C-8.
- **R3-2** ✅ **Editações sensíveis de perfil sem PIN não pedem gate nenhum.** *Ficheiro/linhas:* `core\profile\ProfileOperationAuthorization.kt` L35-41 (`sensitiveProfileEdit` devolve `emptySet()` quando `pinHash` é nulo/em branco), `features\profiles\ManageProfilesScreen.kt` L193-205. *Sintoma:* um perfil kids sem PIN pode ser rebaixado (`REDUCE_RESTRICTIONS`) ou ver o PIN alterado (`CHANGE_PIN`) sem qualquer restrição — o controlo depende da existência do PIN *do perfil*, não de uma credencial de adulto. *Repro:* criar perfil kids sem PIN → ecrã de gestão → desactivar restrições → pedido de PIN não aparece. *Correção:* exigir sempre credencial de adulto (PIN do setup/duas chaves) para alterações sensíveis, com `emptySet()` a significar "negado por omissão" em vez de "isento". *Severidade:* Alta. 🔗 R2-24/C-8.
- **R3-3** ✅ **Restauro escreve `isKids` sem salvaguarda de operação sensível.** *Ficheiro/linhas:* `core\backup\BackupManager.kt` L874-889 (ramo `existing != null` → `profileDao.update(existing.copy(avatarColor, avatarId, isKids, pinHash))` sem passar por `sensitiveProfileEdit`). *Sintoma:* um merge/restauro de perfil nomes coincidentes pode converter um perfil locked para non-kids (ou vice-versa) sem verificação; matiz: `pinHash = incoming.pinHash ?: existing.pinHash` (L883) está certo — um backup sem PIN não desbloqueia o perfil local. *Correção:* no restauro, quando `isKids` passa de true→false, exigir a mesma credencial de adulto do R3-2 (ou rejeitar essa entrada com relatório "skipped: sensitive edit"). *Severidade:* Média.
- **R3-4** ⚠️ **"Restauro preserva lastSyncAt do doador" — refutada em parte; o gap real é outro.** *Ficheiro/linhas:* `BackupManager.kt` L971-973 (insert de fonte no restauro usa `toInsert.copy(lastSyncAt = null)` — comentário do código: restauro trata 1.ª sincronização como "fresh"); merge/update L929-964 conserva o `lastSyncAt` do *dispositivo* (não o do doador); `SetupViewModel` L171-177 não agenda nada pós-restauro. *Sintoma:* a alegação original ficou ❌; o que resta: após restauro de fontes novas não há enqueue imediato de sincronização — a fila arranca só no próximo `onStart` (gatilho do R3-9), deixando os catálogos da fonte restaurada vazios até lá. *Correção:* no fim de um restauro com `applySources`, enfileirar `catalogSyncScheduler.enqueueSync` para as fontes inseridas. *Severidade:* Média (efeito dependente do R3-9).
- **R3-6** ✅ **Estado "playing" pode manter um canal invisível ao perfil activo.** *Ficheiro/linhas:* `features\live\LiveViewModel.kt` L2082 (`_playingChannel.value = requestedChannel`) vs L2093-2096 (`channelVersions`/`isVisibleToActiveProfile` bail-out). *Sintoma:* ao escolher versão priorizada que o perfil não pode ver (ou canal invisível), o bail-out acontece *depois* do primeiro acesso ao estado — a UI (mini-player/overlay) mostra um canal que nunca vai tocar e o spinner fica preso. *Repro:* perfil kids → canal cuja versão priorizada é de adulto → clicar → estado "playing" com canal invisível. *Correção:* mover o primeiro `_playingChannel.value` para depois do gate de visibilidade e limpar/retomar em else. *Severidade:* Média. 🔗 C-7.
- **R3-7** ✅ **Importador de backup sem limites de contagem.** *Ficheiro/linhas:* `core\backup\BackupImportValidation.kt` L11 (`0..Int.MAX_VALUE`). *Sintoma:* contagens de arrays do ficheiro (perfis/fontes/userData/tombstones) não têm tecto — um ficheiro criado (ou corrupto) com milhões de entradas pode induzir OOM/DoS no restauro. *Repro:* backup `.own` com `userData` de >10^6 registos → restauro → OOM/crash. *Correção:* caps por secção (p.ex. 10^5 entradas; payload total com maxBytes já aplicado), rejeitar por cima com mensagem clara. *Severidade:* Alta.
- **R3-8** ✅ **Merge sem fontes pode remapear userData pelo id cru do ficheiro.** *Ficheiro/linhas:* `BackupManager.kt` L1086 (`val p = profileIdMap[fileP] ?: if (applySources) continue else fileP`), consumido por tombstones L1108 e `userData` L1109-1115; ids de perfil são "per-device counters" (comment L859-861). *Sintoma:* num merge sem secção de fontes, registos cujo perfil não mapeia ficam com o id literal do ficheiro — quando os contadores coincidem por acaso, favoritos/histórico/progresso e tombstones recaem no perfil local errado. *Repro:* merge em instância com ids 1..3; ficheiro com perfil id 2 de outro nome → o registo `p=2` cai no perfil local 2. *Correção:* sem secção de fontes, exigir match por nome/avatar para o perfil em `fileP` ou descartar o registo (como já faz o ramo `continue`). *Severidade:* Média.
- **R3-5** ❓ *(slot do lote 3 sem re-traçagem individual nesta síntese — ver Apêndice G).*

**Sync / Fontes**

- **R3-9** ✅ **O auto-refresh de playlists só corre no arranque/foreground; nada periódico existente.** *Ficheiro/linhas:* `core\settings\SettingsRepository.kt` L86-94 (`enum class PlaylistAutoRefresh(val thresholdMs: Long? = null) { OFF, STARTUP, HOURS_6, HOURS_12, MANUAL }`), `features\shell\ShellViewModel.kt` L115-166 (check de thresholds + agenda), únicos call sites `MainActivity.kt` L170/L216/L349 (`onStart`/retomar/refresh); o único `PeriodicWorkRequest` no projecto é o `AutomaticBackupWorker` (1/dia). *Sintoma:* o dispositivo que fica ligado 24/7 (caso de TV) nunca passa por `onStart` — playlists com `HOURS_6`/`HOURS_12` ficam desactualizadas até uma interrupção/app relaunch; `MANUAL(n dias)` sem cron. *Repro:* box ligada contínua com auto-refresh 6h → EPG/novos títulos nunca refrescam no fim do dia. *Correção:* `WorkManager` periódico (mín. 15 min; para `HOURS_6`/`HOURS_12`/`MANUAL` agendar na próxima janela threshold), com o mesmo guard `shouldRefresh` do ShellViewModel; cuidado com ligações limitadas (usar `ConstraintType.CONNECTED` e manter "battery-friendly"). *Severidade:* Alta. 🔗 família A4.
- **R3-10** ✅ **Remoção de fonte deixa filhos órfãos; `purgeOrphans` nunca é chamada.** *Ficheiro/linhas:* `core\database\dao\SourceDao.kt` L22-23 (`@Delete` sem `onDelete CASCADE` para o user-data — só `ProfileEntity` tem FK); purgeOrphans definido em 6 DAOs — `FavoriteDao` L90, `HistoryDao` L135, `ProgressDao` L163, `ContentOrderDao` L97, `CustomCategoryDao` L156, `SeriesSortOrderDao` L72 — com **zero call sites** (grep). *Sintoma:* apagar uma playlist mantém favoritos/history/progresso/ordem manual/entidades órfãs sem fonte — badges/contadores corrompidos e ids soltos no DB. *Repro:* adicionar playlist com favorito + progresso → apagar fonte → grep da tabela favoritos com `itemId` sem fonte. *Correção:* FK `ON DELETE CASCADE` (na próxima migração de schema) ou chamar os `purgeOrphans` na transacção de remoção de fonte. *Severidade:* Média.
- **R3-12** ✅ **`updateSource` reverte campos em branco silenciosamente e não invalida credenciais Xtream.** *Ficheiro/linhas:* `features\settings\SettingsViewModel.kt` L1084-1150. *Sintoma:* `name/url` usam `ifBlank { existing… }` (L1115-1116) e `username/password/…` `takeIf { it.isNotBlank() } ?: existing` (L1117-1125) — editar por campos vazios (intenção "limpar credencial" comum no Stalker) guarda o valor antigo sem aviso; e o Stalker invalida portal (L1109) mas o Xtream não invalida nada quando URL/credentials mudam (sync seguinte é que mostra o 401). *Correção:* UI explícita ("manter valor actual" vs "limpar") + invalidação de ep/session/host para Xtream quando URL/credenciais mudam. *Severidade:* Média.
- **R3-13** ✅ **Guards de "Testar" vs "Adicionar" no M3U são incoerentes quanto a caminho local.** *Ficheiro/linhas:* `features\setup\AddSourceScreen.kt` L384-394 (`canStart` L386 sem guard de `/` vs `canTest` L392 exige `!m3uUrl.startsWith("/")`). *Sintoma:* uma playlist de caminho local (`/storage/…/file.m3u`) pode ser *adicionada* mas não *testada* — o utilizador só vê o resultado do import sem a verificação prévia. *Correção:* unificar guard — aceitar local em ambos (probe `File.canRead()` com ContentResolver) ou rejeitar local em ambos com hint de "usar URL". *Severidade:* Baixa.
- **R3-14** ✅ **`lastFailedSource` nunca fica preenchido.** *Ficheiro/linhas:* `SettingsViewModel.kt` L1177-1187 — o próprio comentário do código admite: "**It has never actually fired.**" (`val lastFailedSource: SourceEntity? get() = null`). *Sintoma:* o re-preenchimento do formulário após falha de adicionar playlist (v4.0.0) nunca funcionou — typo no portal URL obriga a reescrever tudo. *Correção:* populá-lo no catch do `SourceImporter` (estado `Failed(source)` e não só `message`) ou remover o campo e o comentário. *Severidade:* Baixa (UX).
- **R3-16** ❌ **Alegação do Lote 3: "re-sincronização M3U renumera ids e quebra favoritos/ordem" — refutada.** *Ficheiro/linhas:* `core\sync\M3uSyncer.kt` L34-42 (stable key `name|group` + `#n` para duplicados, guardada em `remoteId`; "changed rows keep their local id… vanished rows are pruned"; prune por scope), L56-58 (`sortOrder` comparado fora do hash), L230/L271/L387 (`upsertStable`), L667 (upsert in-place). *Sintoma:* o mecanismo já preserva os ids locais nas re-syncs — favoritos/history/ordem manual sobrevivem por design. *Correção:* nenhuma — robustez de upsertStable já implementada (nota de robustez). 🔗 família M3uSyncer do lote 2.
- **R3-11** ❓ *(slot do lote 3 — ver Apêndice G; stats de sync em memória, `SyncManager.kt` L79).*
- **R3-15** ❓ *(slot do lote 3 — ver Apêndice G).*

**Pesquisa / TMDB / Metadados / Multiview**

- **R3-17** ✅ **Pesquisa FTS falha palavras acentuadas (FTS4 sem unicode61) e o query builder produz sempre uma expressão.** *Ficheiro/linhas:* `core\database\OwnTVDatabase.kt` L1532-1535 (`CREATE VIRTUAL TABLE … USING fts4` sem `tokenize=unicode61`), `core\content\SearchReader.kt` L170-175 (`ftsQuery` constrói sempre a expressão de tokens). *Sintoma:* palavras com acento não batem as formas sem acento no índice (e vice-versa) — pesquisa «Nação», «Estórias», «Irmãos» devolve vazio embora os títulos existam. *Repro:* pesquisa «Irmãos» não encontra «Irmaos», nem o inverso. *Correção:* migrar o FTS para `tokenize=unicode61` (recriar a tabela na migration v→52) e normalizar queries com a mesma heurística antes de casar. *Severidade:* Média. 🔁 §8.2-1 · 🔗 R3-27.
- **R3-18** ✅ **Corpo `200` não-parsável do TMDB envenena o cache negativo por 7 dias.** *Ficheiro/linhas:* `core\metadata\TmdbProvider.kt` L339-366 (`parseResults` → `?: return emptyList()` L340), `MetadataRepository.kt` L104-108/L148-150 (cache 7 d inclui resultado vazio), guard pelo HttpClient: `core\network\HttpClient.kt` L58 (`if (!response.isSuccessful) throw HttpStatusException(...)`). *Sintoma:* só um 200 "válido" com JSON inesperado (provavelmente HTML/blob sob 200) devolve `emptyList()`, que a cache guarda por 7 dias → trending/hero vazios a semana toda; erros de rede/5xx/429 não são cacheados (lançam). *Repro:* mock do host TMDB a devolver 200 + HTML → hero fica vazio 7 dias. *Correção:* em `parseResults`, distinguir "vazio legítimo" de "corpo inesperado" (p.ex. validar campo ausente lançando `ParseException`) e não armazenar esse caso como cache negativa. *Severidade:* Média.
- **R3-21** ⚠️ **"TMDB sem retry/backoff" — refutado em parte; o gatilho real é o `tryConsume` antes do sucesso.** *Ficheiro/linhas:* `HttpClient.kt` L46-58/L86-102 (loop de tentativas com `HttpStatusException` L58, retry para 5xx/429 e sem resposta; back-off `RETRY_DELAY_MS * attempt` cap L100); `TmdbProvider.kt` L84 (`tryConsume` do rate-limit antes de iniciar a resposta). *Sintoma:* a alegação "sem retry/backoff" fica ❌; a matiz: o token de quota é consumido mesmo em 200 semanticamente falhados (ver R3-18), e 403/404 não são retentáveis por design — uma sequência de falha faz o lote ficar sem dados. *Correção:* manter o retry existente; não consumir o token quando o corpo é inesperado (R3-18) e log de "provider indisponível". *Severidade:* Média (matiz).
- **R3-19** ✅ **`SearchReader.curated` filtra apenas por fonte; sem acesso à visibilidade por perfil.** *Ficheiro/linhas:* `SearchReader.kt` L112-142 (`curated` aplica `sourceIds` e ordenações; nada sobre kids/`isVisibleToActiveProfile`). *Sintoma:* os chips curated podem ofertar itens que o perfil activo não deve ver (kids), dependendo do ordenador downstream. *Repro:* perfil kids → chips curated de filmes adultos visíveis na Home de pesquisa. *Correção:* passar o filter de perfil (`AdultCategoryClassifier.allows`) em `curated` antes do retorno. *Severidade:* Média. 🔗 R2-26.
- **R3-23** ✅ **Falhas de trending abortam a ronda, sem re-agendamento curto.** *Ficheiro/linhas:* `core\trending\TrendingRepository.kt` L260-262/L281-283 (`preserveFailure(sourceId, "movie candidates"|"TV candidates", …)`) e L288-294 (persistência na saída below-threshold). *Sintoma:* um pedido TMDB falhado mata a ronda até à próxima janela agendada; e a saída "below-threshold" persiste uma seleção parcial (menos itens do ideal). *Correção:* re-agendar curto (p.ex. 10 min) em cima de `preserveFailure` com cap de tentativas; só persistir quando `selections.size` ≥ limiar. *Severidade:* Média.
- **R3-24** ✅ **Atualização do hero repõe o carousel no 1.º item (o trending preserva o índice).** *Ficheiro/linhas:* `features\home\HomeViewModel.kt` L555-556 (`activeTrendingIndex` preservado com `coerceIn`) vs L560 (`activeHeroIndex = 0` incondicional). *Sintoma:* cada emit do ui-state (agendado pela fonte de dados) reinicia o hero = «ver de novo» o mesmo primeiro tile; o fix equivalente tinha sido aplicado ao trending e não ao hero. *Correção:* `activeHeroIndex` com o mesmo `coerceIn(previous, size-1)`. *Severidade:* Baixa/Média.
- **R3-25** ✅ **Ramo "query demasiado curta" da pesquisa é código morto.** *Ficheiro/linhas:* `features\search\SearchScreen.kt` L119-120 (`searching = query.trim().length >= 2`) vs L147-148 (`searching && query.trim().length < 2` — impossível). *Sintoma:* o hint "minimum_query" nunca aparece; com 1 caractere o ecrã mostra `LauncherEmptyState` sem explicar o porquê. *Correção:* colapsar branch (remover ou usar `query.trim().length == 1` para mostrar o hint). *Severidade:* Baixa.
- **R3-26** ✅ **Focus requesters do multiview re-criados por contagem, sem re-focus em mudança de tiles.** *Ficheiro/linhas:* `features\multiview\MultiviewScreen.kt` L90 (`remember(state.tiles.size) { List(...) }`), L92 (`LaunchedEffect(Unit)` — corre uma só vez), L128 (addTile). *Sintoma:* ao adicionar/remover tile, a lista de requesters muda de identidade mas nem `LaunchedEffect` nem `state.focus` garantem que a tile nova/focada tenha os requesters actualizados — foco desaparece até navegação manual. *Repro:* multiview com 2 tiles → menu → "Add tile" → foco caído. *Correção:* chave do `remember` por `state.tiles` (identidade, não tamanho) + `LaunchedEffect(state.tiles)` que restaura `state.focused`. *Severidade:* Média.
- **R3-27** ✅ **LIKE sem escape nas secções continua (mesmo mecanismo já levantado na ronda 2).** *Ficheiro/linhas:* `core\database\dao\MovieDao.kt` L240/L243/L247 (vários `%…%` sem escape de `%`/`_`). *Sintoma:* pesquisa de secção com `%`/`_` interpreta wildcards — resultados surpreendentes; e full scan já conhecida. *Correção:* helper `escapeLike()` aplicado antes de `%…%` + substituir por FTS (R3-17). *Severidade:* Média. 🔁 §8.2-1 · 🔗 R3-17.
- **R3-22** ✅ **OpenSubtitles engole IOException como ApiException.** *Ficheiro/linhas:* `player-core\…\subtitles\SubtitleController.kt` L190-202; `OpenSubtitlesClient` L29 (`ApiException: IOException`). *Sintoma:* falha de rede no fetch de legendas é reportada como "API inválida" — o retry/backoff e a mensagem para o user são enganadores. *Correção:* separar `ApiException` de `NetworkException`; no controller, rate-limit e toast "sem rede" dependendo do tipo. *Severidade:* Média.
- **R3-28** ✅ **Race no fetch de legenda (check-then-act).** *Ficheiro/linhas:* `core\subtitles\SubtitleRepository.kt` L75-80. *Sintoma:* entre a verificação "já existe legenda" e a escrita, um trigger concorrente pode duplicar o fetch — múltiplos pedidos OpenSubtitles para o mesmo item. *Correção:* single-flight do fetch por chave de media. *Severidade:* Média.
- **R3-29** ✅ **`localKey` é escrito, o leitor esquece-o.** *Ficheiro/linhas:* `core\metadata\MetadataRepository.kt` L625-631 (`pickBest` não consulta o campo) e L581-587 (`fetchAndCache` persiste localKey). *Sintoma:* campo gravado no DB sem leitor — morto na leitura; não é bug funcional, mas explica parte do stale de detalhes por key local. *Correção:* ou consumir `localKey` no `pickBest` (p.ex. preferir o cache por id local) ou retirar a coluna na migration. *Severidade:* Baixa.
- **R3-30** ✅ **Cache de detalhes re-fetchada por chave global, não por item local.** (Complementa R3-29: mesmo código, mesmo efeito — cache detalhada por key global não sobrevive ao re-import da fonte, fazendo re-fetch desnecessário). *Correção:* alinhar a key de detalhes com o id local da linha (par com R3-29). *Severidade:* Baixa.
- **R3-20** ❓ *(slot do lote 3 — ver Apêndice G).*

**PVR / Gravações / Downloads**

- **R3-31** ✅ **Episódios com o mesmo nome colidem no mesmo ficheiro; no resume, o 416 grava COMPLETED com bytes doutro item.** *Ficheiro/linhas:* `features\series\SeriesViewModel.kt` L1093 e L1113 (`fileName = sanitize(episode.name…)` — sem temporada/episódio no nome); `core\download\DownloadEngine.kt` L176-186 (resume `Range` com `resp.code == 416 && existing > 0` → `COMPLETED`, L184) e L195 (`target.openOutput(append)`). *Sintoma:* dois episódios com o mesmo título na mesma temporada têm o mesmo `relativeDir+fileName` → o segundo sobrescreve o primeiro; no resume contra um ficheiro já completo de outro episódio, o 416 devolve COMPLETED honesto mas os bytes são doutro item — a biblioteca de séries mostra conteúdo errado. *Repro:* série com 2 eps "Tempestade" na S01 → baixar ambos → 2 linhas na tabela de downloads, 1 ficheiro com o tamanho do 2.º, contendo bytes misturados. *Correção:* nomear `S{xx}E{yy}_{name}` (o campo `episodeNumber` existe na Entity) e, quando o alvo já existe, gerar desambiguação (timestamp) em vez de append; manter o 416→COMPLETED apenas para o item referido no resume. *Severidade:* Alta. 🔗 R1-B4 (truncado COMPLETED) · 🔗 R3-32.
- **R3-32** ⚠️ **Cap vFAT 4 GiB é honesto; o "truncamento 416" só acontece via colisão do R3-31.** *Ficheiro/linhas:* `core\storage\StorageVolumeInfo.kt` L47 (`vfat/msdos → 0xffff_ffffL`), `DownloadEngine.kt` L202/L205 (`reserve/commit` no lease) → `FILE_LIMIT` → `markFailed`. *Sintoma:* a alegação de truncamento silencioso não acontece em descargas normais — o limite do FS é respeitado e a falha é honesta; o único caminho para bytes truncados com `COMPLETED` é a colisão de filenames R3-31. *Correção:* manter L47; rever a mensagem para "formato limite vFAT" para não confundir o utilizador. *Severidade:* Média (matiz).
- **R3-33** ✅ **Gravação agendada perdida com app morto/reinício não é re-agendada.** *Ficheiro/linhas:* `core\recording\RecordingEngine.kt` L148-155 (`markMissed` permanente se a janela passou) e L1498-1515 (`markMissed`), `RecordingManager.kt` L91 (rearmer só por ação do utilizador). *Sintoma:* uma agenda perdida com o app morto/reinício do device fica `MISSED` sem tentar recuperar a janela. *Correção:* re-agendamento automático quando `missed` com janela ainda aberta; re-verificar `scheduleNext` na inicialização. *Severidade:* Alta.
- **R3-34** ✅ **User-Agent por fonte é ignorado no DownloadEngine quando o fonte não requer resolver.** *Ficheiro/linhas:* `DownloadEngine.kt` L239-241 (não-resolver → `return d.streamUrl to HttpClient.DEFAULT_USER_AGENT`, ignorando `source.userAgent`) vs `RecordingEngine.kt` L1521-1534/L1524 (honra `source.userAgent`). *Sintoma:* espelhos/hotspots que bloqueiam UA de OkHttp bloqueiam o download com 403, enquanto a gravação honra o UA da fonte. *Correção:* em `resolveTarget`, devolver `source.userAgent?.takeIf { it.isNotBlank() } ?: HttpClient.DEFAULT_USER_AGENT`. *Severidade:* Média.
- **R3-37** ✅ **Agenda de gravação por wall-clock sem verificação de up-time/NTP.** 🔗 §2 NTP. *Sintoma:* caixas sem NTP/horas erradas perdem janelas de gravação — relacionado ao §2 da ronda 1 (falta de verificação de hora no arranque). *Correção:* no `RecordingManager` init, recalcular `nextAt` se |wallClock - lastBootClock| > 10 min e re-agendar. *Severidade:* Média.
- **R3-38** ✅ *(com matiz)* **Cleanup de spools finalizados pula linhas com readers.** *Ficheiro/linhas:* cleanup `hasPlaybackReaders` skip (nuance: não é "ignora it.delete()" — o código verifica leitores activos antes de apagar). *Sintoma:* muitos spools terminados com readers activos crescem sem limite em sessões longas. *Correção:* devolver o garbage collector assim que o reader solta a lock (tracker de readers com onRelease → apagar). *Severidade:* Média.
- **R3-40** ✅ **Ecrã de downloads sem ações bulk (limpar falhas/em progresso).** *Ficheiro/linhas:* `features\downloads\DownloadsScreen.kt` L277-302 (apenas ações por linha). *Correção:* menu de contexto "limpar concluídos", "re-agendar falhados" com confirmação. *Severidade:* Baixa.
- **R3-41** ✅ **Ponte EPG lembretes → gravação não existe.** 🔗 R2-12 (B-1: reminders in-memory sem scheduling). *Sintoma:* lembretes de EPG não criam gravações agendadas nem autostart; a ponte prometida no lote 2 não está no código. *Correção:* conector `Reminder→RecordingManager.schedule` com dedup por channelId + inicio do programa. *Severidade:* Média.
- **R3-39** ❓ **`relinkAfterSync` não existe no código (grep zero).** *Ficheiro/linhas:* `RecordingManager.kt` L330 — apenas um comentário "Decline an uncertain channel re-match". *Sintoma:* a alegação de relink post-sync precisa ainda de ser re-traced — a única evidência é um comment; por enquanto ❓ (funcionalidade não implementada, não bug confirmado). *Correção (proposta):* pós-sync, resolver as gravações agendadas por `channelExternalId` em vez de id local. *Severidade:* — (por categorizar).
- **R3-35 / R3-36** ❓ *(slots do lote 3 sem re-traçagem individual nesta síntese — registadas no Apêndice G).*

### 10.2 Melhorias e lacunas (não bugs)

1. **Transacção no create de perfil** — `core\profile\ProfileManager.kt` L44-55 insere perfil + defaults sem `db.transaction {}`; uma falha a meio deixa metade do setup. Correção: ajustar transaction no create. ✅
2. **Clean-up de preferências por-perfil no delete** — `ProfileManager.kt` L120-136 não limpa `startup_mode_<pid>`, `customize_pin_<pid>`, `home_config_<pid>` (chaves existem em `SettingsRepository.kt` L635/L665/L687) nem spools/favoritos — chaves e dados por-perfil sobrevivem ao perfil apagado. Correção: o `deleteProfile` incluir purge das chaves `*_<pid>` + user-data (R3-10). ✅
3. **Persistir resultado da última sync** — `SyncManager.kt` L79 (`lastSyncStats` em `ConcurrentHashMap`) — estatísticas de sincronização morrem com o processo; a coluna `lastSyncAt` (fontes L1545) só conta data/hora — sem erro/contadores. Correção: persistir contadores/erro numa tabela `sync_stats`. 🔗 A11.
4. **Pós-restauro sincronizar já** (ligado ao R3-4) — `SetupViewModel` L171-177: após o restauro, enfileirar de imediato no scheduler em vez de esperar pelo próximo `onStart`. ✅
5. **Bulk actions em downloads** (R3-40): limpar concluídos/falhados em batch. ✅
6. **FTS→unicode61/híbrido** (R3-17/R3-27): migration + re-parse de índices. ✅
7. **Auto-refresh periódico** (R3-9): WorkManager periodic + threshold windows. ✅
8. **A14 (Lote 3)** — *re-verificar na próxima ronda* ❓.
9. **SetupWizard: estado não-Saveable** — `SetupWizard.kt` L107-115/L836-860: formulário/onboarding em memória (`remember`, sem `rememberSaveable`), rotação/back press perdem o estado para o passo anterior. Correção: `rememberSaveable` +/ou `SavedStateHandle`. ❓ (mecanismo verificado por grep; impacto por validar)
10. **Kids: limite de tempo de sessão** 🔗 §5-#14 (ronda 1) — proposta: timer + PIN de continuação; hoje a sessão kids pode durar o dia inteiro. ✅ (proposta) · ❓ (desenho a validar)
11. **DataStore para DatabaseRecovery** — proposta: substituir o arquivo-pick de recuperação por `DataStore` com versão/migração; rever na próxima ronda. ❓
12. **tuneTile hardening (B8)** — proposta do lote 2 a rever; rever a seleção do canal em `state.addTile` (L128) — múltiplos cliques podem definir o foco antes da validação do pool. ❓

*Notas positivas*: A10 OK (armazenamento de credenciais confirmado); mecanismo `upsertStable` do M3uSyncer e guardas do restauro (L880-883, L955-962) bem documentados no próprio código. OK.

### 10.3 Ideias de inovação do lote 3 — status

Tabela completa (problema → solução | integração | esforço | diferencial):

| # | Ideia | Problema → Solução | Integração | Esforço | Diferencial |
|---|---|---|---|---|---|
| 1 | Planner «Holofote de Conflitos» | Teto de sessões só se descobre ao falhar em directo → ao confirmar/agendar gravações, cruzar SCHEDULED/RECORDING com o limite de sessões já medido e com multiview activa; aviso «21h: 2 gravações + multiview = 3 sessões (limite X)» e plano alternativo «gravar 2, ver o 3.º por catch-up» quando existe arquivo (sinal do ReplayEvidenceStore) | `features\live\LiveViewModel.kt` (importa RecordingSchedule), `features\live\ManualRecordingDialog.kt`, Core `recording\RecordingEngine.kt`/`RecordingSchedule.kt`, `LiveSessionLimit` (player-core) | M | Ninguém avisa conflitos de largura de banda antecipadamente |
| 2 | Ruler de Gravação (série/slot com auto-reparo) | «Todas as exibições» existe, mas novas temporadas/estreias não criam regra; falha deixa buraco sem reposição → regra por (título normalizado + canal/janela); a cada sync EPG agenda entradas novas; se falhar, repor via catch-up (`CatchupUrl`) com etiqueta «via arquivo»; regras viajam no `.own` v24 | Novo `RecordingRuleEngine` em `features\recordings\`, `RecordingsViewModel.kt`, Core `CatchupUrl.kt`, pipeline `EpgViewModel.kt` | L | TiviMate tem séries mas não auto-reparação por arquivo |
| 3 | Retenção por «visto» | Quota existe (`*_media_quota_gib`, StorageQuotaLedger) mas nada é apagado por estado de visualização → política opt-in por perfil: apagar vistos há >30 dias; antes de nova gravação o ledger propõe candidatas; gravações fixadas ficam imunes | `RecordingsViewModel.kt`, ecrã de definições de gravações, Core `storage\StorageQuotaLedger.kt` | S/M | Gestão de espaço honesta |
| 4 | Biblioteca de Gravações com fichas | Gravações são lista de ficheiros com títulos do provider → agrupar por série (match TMDB local), título canónico «S02E04 · Nome», cartaz, «próximo ep. não visto», renome reversível (original guardado no backup) | `RecordingsScreen.kt`, loader TMDB existente, `PlaybackPrefsDao` | M | Gravações viram biblioteca estilo Netflix, sem cloud |
| 5 | Agenda Semanal (guia flat por dia) | Planear «o que gravar/ver esta semana» com grelha é laborioso → vista agenda: linha por dia, favoritos/géneros primeiro, acções lembrete/gravação por item; sustentada no EPG local + fix de lembretes persistidos (R2-B-1) | Novo `EpgAgendaScreen.kt`, queries/DAO do `EpgViewModel.kt`, `GuideChannelEligibility.kt`, `ProgrammeReminderStore.kt` | M | 2.ª leitura do guia que nenhum rival tem |
| 6 | «Pulso das favoritas» (heatmap horário) | «Que canal está rico agora?» é invisível na grelha → canal×hora com cor = densidade/vida de programação (começa agora, filme vs rerun); toque sintoniza; Canvas reutilizado com cache de medidas | Modo do `EpgScreen.kt` sobre `GuideCore.kt`/`GuideViewport.kt`, dados do `EpgViewModel.kt` | S/M | Visual de densidade inédito |
| 7 | Guia agrupado por género | 50k canais sem contexto temático na grelha → modo agrupado por género (classificador local das categorias existentes), cabeçalhos por dia, chips persistidos por perfil | `GuideChannelEligibility.kt`, `EpgScreen.kt`, `GuideRowCache.kt` | M | Vai além dos grupos crus da playlist |
| 8 | Cartão-partilha zero-cloud | Zero `ACTION_SEND`/share na app (grep confirmado) → menu partilhar → deep link universal `owntv://watch?...` (canal/EPGid/título); quem tem OwnTV abre (parser já existe); destaque «estou a ver» | `MainActivity.kt` (L140+ parser onNewIntent), novo `ShareCard.kt` em `features\shell\`, ViewModels de detalhe | S/M | 1.º gesto social |
| 9 | «Tira para o ecrã grande» via Companion | O Companion HTML não comanda playback → endpoint `/play` com canal/item; telemóvel escolhe na mini-grelha; TV sintoniza com toast de confirmação; bloqueado em Kids/PIN | Core `companion\CompanionHttpServer.kt` (estilo dispatcher `/serviceconfig`), `CompanionController.kt`, `CompanionHtml.kt`, `MainActivity` onNewIntent | M | Comando, não sincronização (distinto de §8.3-11) |
| 10 | Modo Viagem (fonte↔túnel auto) | Fora de casa as fontes residenciais falham; troca manual rede-a-rede é chata → por fonte, «endereço alternativo em trânsito» (domínio próprio/reverse proxy/túnel) + swap automático na detecção de rede (observer existente), rótulo «em trânsito via Y»; zero-cloud | `features\settings\ManageSourcesScreen.kt`, `SourceDraftViewModel.kt`, padrão rede Core (`network\DnsConfig.kt`), swap base-URL em `M3uSyncer.kt`/`XtreamClient.kt` | M/L | Failover geográfico |
| 11 | Numeração Pessoal (LCN local) | CH+/CH− e teclas numéricas seguem a playlist → campo de número pessoal por canal (auto, com sugestão de swap em conflito); CH+/CH−, número-press e busca «canal 105» usam o mapa | `features\live\ChannelOrderEdit.kt`, `ui\components\ChannelPressInput.kt`, `ChannelDao` (índice por número), definições de navegação | M | Feeling de TV real |
| 12 | Combos de duas teclas editáveis | Sequências de atalho contam hard-coded e inéditas ao utilizador (§8.3-18 cobre só long-press) → capturar sequência de 2 presses em ≤700 ms, mapeada por secção (player/EPG/listas) para as 25 acções existentes + «sintonizar canal N»; editor reusa o componente de shortcut | `ui\components\RemoteShortcutInput.kt`, `ui\components\ChannelPressInput.kt`, `player\PlayerHud.kt` | S/M | Atalhos pró sem tirar foco do HUD |
| 13 | API Local de integração | As automações (§8.3-13) ficam dentro da app; Home Assistant/Tasker não falam com ela → endpoints JSON no Companion: `GET /api/v1/status|channels|epg/now`, `POST /api/v1/tune|resync|suspend`; PIN + ACL LAN; respostas sem credenciais | Core `CompanionHttpServer.kt`, `CompanionServerState.kt`, `CompanionController.kt`; tune reutiliza deep link | M | Única IPTV TV com API de integrador local — hardening conforme §8 segurança |
| 14 | Score de Line-up (desporto/música) | Eventos morrem nas grelhas entre re-reperições → classificador local (keywords/genre sobre a normalização do EpgMatcher) marca eventos nos favoritos; rail Home «eventos próx. 24 h» + badge na célula; aprende dos desportos favoritados | Core `epg\EpgMatcher.kt`/`EpgRepository.kt`, `features\home\HomeScreen.kt`, badge em `GuideProgrammeCells.kt` | M | Alerta de eventos sem serviço externo; opt-in (falsos positivos) |
| 15 | Multiview em mosaico (auto-arranjo) | Tiles iguais forçam ajustes manuais; o tile com áudio devia reinar → layout primário 55-60% + secundários empilhados; nudge manual como override; memória por perfil | `features\multiview\MultiviewScreen.kt`, `MultiviewState.kt`, menu de tile multiview, `DEFAULT_MULTIVIEW_TILES` | S/M | Composição visual (distinto de §5-6, orçamento de tiles) |
| 16 | Art por programa na grelha | Guia só tem logos (lacuna §4 confirmada) → match TMDB por programa com fila por foco e cache persistente por id; só favoritos + canal em foco (cap diário) para escalar a 50k; fallback logo | `GuideProgrammeCells.kt`, `GuideCore.kt`/`GuideRowCache`, loader TMDB | M | Guia visual único; quota de rede controlada por prefetch-foco |
| 17 | Doutor de Fontes (cartão de saúde) | Nada mostra que fonte está a degradar → após cada sync, cartão na gestão de fontes: Δ itens vs. sync anterior, % EPG coberto, n.º de URLs falhadas (StreamLog §8), «última boa prova há X h» | `ManageSourcesScreen.kt`, `ReplayEvidenceStore` (`features\epg\`), stats de `M3uSyncer`/`XtreamClient` | M | Transparência por fonte (distinto de §5-15, §8.3-12, §8.3-3: é por fonte) |

## 11) Reavaliação final consolidada (Rondas 1-3)

*Nota: §13 (mais abaixo) é a versão consolidada e final (Rondas 1-4); esta secção mantém-se como registo histórico.*

### 11.1 Veredicto por ronda — estado após esta ronda

| Ronda | Cobertura | Veredicto consolidado |
|---|---|---|
| 1 | TOP 5 × 5 analistas + 40 achados | "(inalterado)" — revalidado no §11.7 sem reversões |
| 2 | 47 alegações Alta/Média | "(inalterado)" — ver Apêndice F |
| 3 | 41 alegações lote 3 | 30 ✅ · 3 ⚠️ · 1 ❌ · 7 ❓ (6 slots não re-traçados) |

### 11.2 TOP consolidado (3 rondas)

| # | Achado | Ronda | Severidade |
|---|---|---|---|
| 1 | Auto-refresh de playlists não corre em box sempre ligada (R3-9) | 3 | Alta |
| 2 | Gate "Who's watching?" contornável via "Adicionar perfil" (R3-1/R3-2) | 3 | Alta |
| 3 | Colisão de episódios com o mesmo nome + 416→COMPLETED (R3-31) | 3 | Alta |
| 4 | Truncado marcado COMPLETED (R1-B4) + fonte UA por playlist (R3-34) | 1/3 | Alta/Média |
| 5 | Reminders sem scheduling + ponte para gravações em falta (R2-B-1/R3-41) | 2/3 | Média |

### 11.3 Soluções cross-domain a priorizar

| Problema | Solução | Locais |
|---|---|---|
| Gate / sensitive ops (R3-1/R3-2/R3-3) | credencial de adulto no `onAddProfile` + pós-criação re-gate; `emptySet()` exige credencial | `ProfileGate.kt` L89, `ProfileOperationAuthorization.kt` L35-41, `BackupManager.kt` L874-889 |
| Auto-refresh (R3-9) | `WorkManager.periodic` + thresholds; trigger nos casos onde o `ShellViewModel.checkAutoRefresh` é acessível | `ShellViewModel.kt` L115-166 |
| Filenames/416 (R3-31/R3-34) | `S{xx}E{yy}_{name}` + `source.userAgent` no fallback | `SeriesViewModel.kt` L1093/L1113, `DownloadEngine.kt` L239-241 |
| Cascade/purge (R3-10) | FK `ON DELETE CASCADE` ou transação com purgeOrphans | `SourceDao.kt` L22-23 |
| TMDB/trending (R3-18/R3-21/R3-23) | cache negativa só para "vazio legítimo"; `preserveFailure` com retry curto | `TmdbProvider.kt` L340, `TrendingRepository.kt` L261/L282 |

### 11.4 Cruzamentos com rondas anteriores

🔗 **B4 (R1, truncado COMPLETED) ↔ R3-31/R3-32** — a ronda 3 encerra a família: o truncado só acontece via colisão de filenames, não por descarga normal.
🔗 **R2-12 (B-1 lembretes) ↔ R3-41** — a ponte EPG→gravação continua por implementar.
🔗 **R2-26 ↔ R3-19** — visibilidade por perfil na pesquisa completa.
🔗 **R2-24 ↔ R3-1/R3-2** — a fundação "kids/hardening" agora também exige verificação de *fluxo* não só de *resultado*.
🔗 **§8.2-1 (LIKE sem escape) ↔ R3-17/R3-27** — FTS 4 sem unicode61 é a raiz.
🔗 **§2 (NTP) ↔ R3-37** — gravações dependem da hora certa.
🔗 **A4 (lote 3) ↔ R3-9** — periodicidade de auto-refresh foi a peça que faltava.
🔗 **M3uSyncer (lote 2) ↔ R3-16** — refutado; mecanismo upsertStable já protege (evidência L34-42/L230/L667).

### 11.5 Checklist de follow-up imediato

1. WorkManager periódico de auto-refresh (R3-9).
2. Credencial de adulto no fluxo "Adicionar perfil" (R3-1/R3-2/R3-3).
3. `S{xx}E{yy}` + UA por fonte nos downloads (R3-31/R3-34).
4. FK cascade / purge órfãos ao apagar fonte (R3-10).
5. Cache negativa TMDB apenas para "vazio legítimo" (R3-18).
6. purge de prefs `*_<pid>` no delete de perfil.

### 11.6 Roadmap de ideias (lotes 1-3, após estabilização)

| Lote | Ideias | Estado |
|---|---|---|
| 1 | 15 (§5: Stream-Health Score, Smart Catch-up Flush, «O que ver agora» EPG, voz do sistema, sumário de programa…) | ✅ Top 5 mantidos (ver §5/Apêndice A) |
| 2 | 18 (§8.3: Espelhos Vivos, Substituto Inteligente, RecommendationRow de canais, Tracks declarativas, Watch Party LAN…) | ✅ integrações verificadas (ver §8.3) |
| 3 | 17 (§10.3: Holofote de Conflitos, Ruler de Gravação, Agenda Semanal, Modo Viagem, Companion /play…) | ✅ como ideias futuras; Top 5: #1/#2/#5/#10/#9 (ver §10.3) |

### 11.7 Alterações de veredicto nesta ronda

| Ref. | Alegação submetida (lote 3) | Veredicto final | Evidência |
|---|---|---|---|
| R3-4 | "Restauro preserva `lastSyncAt` do doador" | ⚠️ | `BackupManager.kt` L971-973 (`copy(lastSyncAt = null)`, com comentário do próprio código: primeira sync do restauro = fresh), merge-update L929-964 mantém o valor do *dispositivo* |
| R3-21 | "TMDB sem retry/backoff" | ⚠️ | `HttpClient.kt` L46-58/L86-102 (retry 5xx/429 com back-off cap) |
| R3-16 | "Re-sync M3U renumera ids e quebra favoritos" | ❌ | `M3uSyncer.kt` L34-42/L230/L667 (stable upsert preserva ids locais) |
| R3-38 | "Cleanup ignora `it.delete()`" | ✅/matiz | cleanup salta rows com `hasPlaybackReaders` (não é ignorância do delete); efeito mantém-se |

Nenhum veredicto de §1-§9 foi revertido; as marcações da ronda 3 estão no Apêndice G.

## 12) Ronda 4 — especialistas (com soluções)

Quarta passagem com cinco analistas especializados — **A plataforma/ciclo de vida**, **B rede**, **C i18n/RTL/a11y**, **D governança entre features**, **E inovação lote 4**. Cada TOP 5 re-lido ficheiro/linhas no código actual (app + snapshot Core `e2081e78…`); as Médias de maior alavanca numa grade obrigatória de 26 itens (a-z; ledger no Apêndice H). Marcações: ✅ confirmado no código · ⚠️ matiz/refinado · ❌ alegação refutada ou caminho exacto corrigido · ❓ mecanismo plausível não re-traçado nesta síntese. Cruzamentos com RDs anteriores marcados 🔗/🔁.

### 12.1) Bugs — por domínio

**Plataforma / ciclo de vida**

- **R4-P1 — Média ✅ — `READ_EPG_DATA` nunca declarada; o provider de Watch Next lê de volta tudo sob `runCatching` = regressão silenciosa.** *Ficheiro/linhas:* `app\src\main\AndroidManifest.xml` L15-16 (só `WRITE_EPG_DATA`, com comentário "writes for Watch Next cards"); core não declara nenhuma EPG perm (grep `EPG_DATA` = 1 ocorrência, a WRITE). `…\core\tv\TvHomeRepository.kt` L460 (`runCatching { channelHelper.getPreviewChannel(…) }`) e L482 (`runCatching { channelHelper.getAllChannels()… }`). *Sintoma:* sem `READ_EPG_DATA` qualquer query ao provider falha; as falhas são engolidas pelos `runCatching`/`getOrElse` com apenas um `logW` — os cartões Recentes/Watch Next/publicação e updates do canal ficam fixados no que cada badge de build consegue escrever e nunca confirmar. *Repro:* apagar a linha do provider via adb → abrir a app → logs "read failed" sem eco na UI. *Correção:* declarar `com.android.providers.tv.permission.READ_EPG_DATA` no manifest do app ao lado do WRITE (a leitura é parte activa do fluxo, não é opcional); opcionalmente elevar o logW a um contador de diagnóstico. *Severidade:* Média.
- **R4-P2 — Média ✅ — `POST_NOTIFICATIONS` declarada e nunca pedida (`requestPermissions` = 0 em app+core).** *Ficheiro/linhas:* core manifest L19 declara; grep `requestPermissions` = 0 no repositório inteiro. *Sintoma:* API 33+ instala sem o diálogo de notificações — gravações em curso/execução no fundo só aparecem se o utilizador conceder manualmente, sem nenhuma linha de onboarding que o diga. *Correção:* pedir no runtime na 1.ª gravação agendada (contexto de permissão legítimo) + atalho `ACTION_APP_NOTIFICATION_SETTINGS` no ecrã de Gravações quando denegada; nota: notificações de update/backup têm o mesmo buraco. *Severidade:* Média.
- **R4-P3 — Média/Baixa ✅ — `RecordingWorker` L36: o 1.º `setForeground` não está protegido; os refreshes de 5 s estão.** *Ficheiro/linhas:* `…\core\recording\RecordingWorker.kt` L36 (`setForeground(…)` sem `runCatching`) vs L43-47 (refresh com `runCatching` + `setForegroundAsync`). `FOREGROUND_SERVICE_MEDIA_PLAYBACK` presente no core manifest L26 e `foregroundServiceType="dataSync|mediaPlayback"` L83 ✅. *Sintoma:* `ForegroundServiceStartNotAllowedException` fora da janela permitida (arranque pós-Doze, kick de alarme) lança dentro do `doWork` e mata a gravação que ia correr; o refresh de progresso não tem o mesmo risco. *Correção:* envolver o 1.º `setForeground` em `runCatching` (a gravação corre o mesmo sem a notificação) e garantir kick de seguimento no próximo alarme quando o guard falhou. *Severidade:* Média/Baixa. 🔗 R4-P2 (a notificação é o motivo do foreground).
- **R4-P4 — Média/Baixa ✅ — `_selectedSection` não sobrevive a process death; o estado do multiview idem.** *Ficheiro/linhas:* `ShellViewModel.kt` L318 (`private val _selectedSection = MutableStateFlow(MainSection.HOME)` — sem `SavedStateHandle`/DataStore); `OwnTVShell.kt` L380 (`var multiview by remember { … }`, só `multiviewWasOpen` L383 é saveable). *Sintoma:* kill do processo em fallback de memória → reabre na Home e perde a grelha de multiview (o resto da navegação já é saveable — novo buraco apenas nestes dois). *Correção:* persistir secção via `SavedStateHandle`/DataStore e reabrir o multiview pelo `multiviewWasOpen` com re-tune manual (nada de re-tune automático de engines). *Severidade:* Média/Baixa.
- **R4-P5 — Média ✅ — `UpdateManager` sem pre-check `canRequestPackageInstalls`; a 1.ª tentativa falha com `SecurityException` genérica.** *Ficheiro/linhas:* `…\core\update\UpdateManager.kt` L265-307 (`install` usa `SessionParams(MODE_FULL_INSTALL)` + receiver de estado; grep `canRequestPackageInstalls` = 0). *Sintoma:* primeiro update de release fora de Play: o `commit` falha sem guiar o utilizador ao toggle "Instalar apps desconhecidas". *Correção:* pre-check `packageManager.canRequestPackageInstalls()`; quando false, devolver estado "precisa de permissão" com intent de settings da própria app e re-agendar o install após o grant; manter o progresso no singleton (a sessão e o APK já sobrevivem em memória). *Severidade:* Média. 🔗 R2-22 (digest) — o caminho update precisa de ambos.

**Rede**

- **R4-R1 — Média ✅ — DNS custom sem cache no cliente singleton; cada conexão gasta 2 lookups (A+AAAA) sequenciais.** *Ficheiro/linhas:* `…\core\di\DataModule.kt` L56 (`.dns(dnsHolder.dns)` directo); o `BoundedDns` com TTL/cache só é aplicado ao cliente da preview — `player-core\…\LivePreviewEngine.kt` L3184. `…\core\network\DnsConfig.kt` L117-122 (DoH: `dohLookup(A)` e depois `AAAA`, sequenciais) e L175-180 (UDP: idem; matiz do próprio comentário L177-178: falha do AAAA não custa os A). *Sintoma:* com DoH activo, cada conexão nova = 2 pedidos com rate-limit risk num sync de milhares de logos/apêndices; a AAAA só responde depois do A terminar (até 2×timeout por conexão). *Correção:* envolver o `dnsHolder.dns` em `BoundedDns` (que já existe e é testado no core) no `DataModule`, com TTL curta; paralelizar A/AAAA (ou só-A com fallback). *Severidade:* Média.
- **R4-R2 — Média ✅ — sem `callTimeout` no singleton; corpo que goteja fica preso até a corutina morrer.** *Ficheiro/linhas:* `DataModule.kt` L45-48 (connect 15/ read 20/ write 20; sem `callTimeout`); `…\core\network\HttpClient.kt` L46-104 (o `get` não tem deadline própria; grep `callTimeout` no core: só `LocalTimeshiftSession.kt` L49 e testes). *Sintoma:* feed EPG que entrega <1 byte/19 s nunca dispara readTimeout e prende o worker do sync indefinidamente. *Correção:* `callTimeout` no singleton (ex. 15 min) + deadline por chamada nas syncs de EPG/catálogo (mesmo padrão dos 3-4 timeouts aninhados que a `LocalTimeshiftSession` já usa). *Severidade:* Média.
- **R4-R3 — Média ✅ — DownloadEngine ignora `httpReferer` e headers custom no ramo não-Stalker (extende R3-34).** *Ficheiro/linhas:* `…\core\download\DownloadEngine.kt` L175-243: `attemptDownload` constrói só `Request.Builder().url(url).header("User-Agent", userAgent)` + Range (L177-178); `resolveTarget` L239-240 devolve `d.streamUrl to DEFAULT_USER_AGENT`. O `SourceEntity` tem `httpReferer` (existe, é serializado no backup `PlaybackDatabaseBackup.kt` L16/L25-27, e o `SourceTester` já o usa L81/L116). *Sintoma:* hotlinks/CDNs que exigem Referer devolvem 403 no download mas tocam no player (que usa os headers completos). *Correção:* no `resolveTarget`, devolver também UA de fonte (`source.userAgent ?: DEFAULT`) 🔁 R3-34, e construir o request com `StreamHeaders.decode` + `httpReferer` da fonte. *Severidade:* Média.
- **R4-R4 — Média/Baixa ✅ — `Retry-After` ignorado no caminho principal do `HttpClient.get`.** *Ficheiro/linhas:* `HttpClient.kt` L91-100 — atraso fixo `RETRY_DELAY_MS * attempt` cap; `HttpRetryAfter` existe e é usado só em `LocalTimeshiftSession.kt` L43/L123, `HlsSegmentRetry.kt` L60 e `LivePreviewEngine.kt` L3890. *Sintoma:* 429 de TMDB/EPG respeita a janela do provider noutros clientes, mas no caminho de sync repete a cada ≤1,5 s. *Correção:* quando `HttpStatusException` 429, usar `HttpRetryAfter.delayMs` (com cap) no atraso antes da próxima tentativa. *Severidade:* Média/Baixa. 🔗 R3-21 (quota TMDB).
- **R4-R5 — Média ✅ — sync de EPG/catálogo corre em qualquer rede; só gravações/downloads têm preferência metered.** *Ficheiro/linhas:* `…\core\sync\work\EpgSyncScheduler.kt` L27 e `CatalogSyncScheduler.kt` L43/L78/L112/L131 (`NetworkType.CONNECTED`); contrastes existentes: `SettingsRepository.kt` L1201 (`downloadsWifiOnly`) e `recordingOverMobileData` (L2201). *Sintoma:* XMLTV/catálogo (centenas de MB) sacados por dados móveis sem a mínima preferência. *Correção:* toggle opcional "Sync só Wi-Fi" por secção (EPG/catálogo), mapeado a `NetworkType.UNMETERED` nas constraints; default CONNECTED para não quebrar boxes sem Wi-Fi. *Severidade:* Média.
- **R4-R6 — Baixa ✅ — `M3uParser` converte `+` em espaço em headers com `+` legítimo.** *Ficheiro/linhas:* `…\core\parser\M3uParser.kt` L373-385 + `decodeUrlComponent` L389-394 — decodifica quando há `%` **ou** `+`, logo `Referer: domain/?ref=a+b` vira `a b`. *Correção:* decodificar só quando o valor contém `%` (o `runCatching` de valor verbatim já existe); a família do split `&` continua coberta pelo Baixa 5 §1. *Severidade:* Baixa.
- **R4-R7 — Média/Baixa ✅ — credenciais do proxy em plaintext no DataStore.** *Ficheiro/linhas:* `SettingsRepository.kt` L2687-2694 (`proxyConfig` lê `PROXY_USER`/`PROXY_PASS`) e L2703-2710 (`saveProxy` escreve); comentário L2684: excluído do backup por opção — mas resta legível por adb/root backup. *Correção:* selar/sellar user+pass com Keystore (o padrão já existe no core: `OpenSubtitlesAuthStore`); migrar na próxima versão. *Severidade:* Média/Baixa (decisão de segurança).
- **R4-R8 — Média/Baixa ❓ — cookies/Referer/Origin de canal seguem redirects cross-host.** *Ficheiro/linhas alegadas:* `LivePreviewEngine.kt` L3465; `OwnTVPlayer.kt` L3500 (http-header-fields do mpv); `HttpClient` segue headers custom. *Sintoma:* token de CDN + Referer vazam a um 3.º host num redirect. *Correção:* interceptor que limpe `Referer/Origin/Cookie` quando o host do redirect muda (`same-host` check), tanto no OkHttp como (onde possível) nos http-header-fields do mpv — este último sem equivalente directo (documentar como limitação). *Severidade:* Média/Baixa · ❓ re-traçado parcialmente nesta síntese (mecanismo plausível; ficheiros/linhas por confirmar).

**i18n / RTL / a11y**

- **R4-I1 — Alta ✅ (campos ❓) — o pipeline de i18n calcula cobertura só sobre `strings*.xml`.** *Ficheiro/linhas:* os scripts citados **não existem no repo** — só no snapshot do Core (`OwnTV_Core\tools\i18n\`): `gen_supported_locales.py` L119 e `validate_strings.py` L95 terminam em `directory.glob("strings*.xml")` ✅ (linha exacta confirmada). No core, `values\` tem 2568 strings, das quais 2413 em `strings*.xml` → **155 chaves fora do glob** (15 delas técnicas em `donottranslate.xml`); a alegação "141/2568" fica da mesma ordem ✅/⚠️. *Sintoma:* tudo o que vive em `epg_clock.xml`, `recording_schedule.xml`, `optimization_controls.xml`… nunca entra na percentagem de cobertura — o picker mostra idioma "quase completo" que não é real. *Correção:* globo `*.xml` exceto `donottranslate` (ou renomear os ficheiros para `strings_*`), repor os dois scripts no repo (estão no snapshot mas fora de `tools\i18n\` da árvore actual) e publicar cobertura real por língua no CI. ❓ percentagens individuais ("es=98%") não re-verificáveis sem o script.
- **R4-I2 — Alta ✅ — o módulo app está inteiramente fora do pipeline; `values-pt` perde 96 chaves e 8 ficheiros inteiros; ~22 idiomas caem em EN.** *Evidência:* `app\res`: 266 strings em `values\`, só **170** em `values-pt` (e `values-pt-rPT` existe); ficheiros **inteiros** sem tradução: `archive_addresses.xml`, `backup_complete.xml`, `channel_categories.xml`, `epg_clock.xml`, `epg_row_status.xml`, `live_header.xml`, `optimization_controls.xml`, `timeshift_resume.xml` ✅ (listagem deferencial). O core tem 26 pastas `values-*` (~24 línguas); o app só tem pt/pt-rPT → todas as restantes (≈22 línguas) mostram EN no shell/UI própria. *Correção:* replicar o pipeline (gen/validate/lint) ao módulo app com ID de chave compartilhado e seed inicial pt; CI valida ambos os módulos. *Severidade:* Alta (i18n gates às cegas).
- **R4-I3 — Média ✅ — `values-pt` do core (tier 1 "100%") ainda falta ~30 chaves.** *Evidência:* contagem direta — core `values\` 2568 strings vs `values-pt\` **2538** (−30) ✅; a concentração em `recording_schedule_*` (~19) ❓ por listagem. *Correção:* lote de seed pt para as 30 chaves + gate de "tier 1 zero faltas".
- **R4-I4 — Média ✅ — `ManualRecordingDialog` usa `Locale.getDefault()` e `HH:mm` fixo (ignora 12 h).** *Ficheiro/linhas:* `features\live\ManualRecordingDialog.kt` L32 (`DateTimeFormatter.ofPattern("EEE dd/MM", Locale.getDefault())`) e L33 (`DateTimeFormatter.ofPattern("HH:mm")` sem contexto 12/24). *Sintoma:* dia da semana da app override de locale (que já existe via `LocaleStore`) e sistemas configurados em 12 h mostram 24 h no julgamento de início/fim. *Correção:* obter o locale da pipeline i18n da app e derivar o padrão de horas de `android.text.format.DateFormat.is24HourFormat` (o formatter central do core já respeita o runtime — `TimeFormat` L25/L37/L69 confirmado em §R4-C). *Severidade:* Média.
- **R4-I5 — Alta (a11y) ✅ — a grelha EPG é um Canvas cego ao TalkBack.** *Ficheiro/linhas:* `features\epg\GuideCore.kt` L143-190 (`ProgrammeStripCanvas` desenha `drawText` por célula — nenhum node semântico) e `EpgScreen.kt` L988-993 (o Box da strip é o único foco da row e **não** leva `contentDescription`). *Sintoma:* TalkBack lê "sem etiqueta" em toda a grelha — o ecrã mais denso é o mais mudo. *Correção:* na strip (por row), `semantics { contentDescription = canal + programa em curso + intervalo }`; nos passos de modo célula, anunciar o programa focado com `announceForAccessibility`/`liveRegion` (nenhum existe hoje — ver R4-I6). *Severidade:* Alta.
- **R4-I6 — Média/Alta ✅ — `Role.Button` só 1 ocorrência; `announceForAccessibility`/`liveRegion` = 0.** *Evidência:* grep `Role.Button` = 1 (`ChannelPressInput.kt` L100) vs `FocusableSurface.kt` L261-275 (`combinedClickable`/`selectable` sem `role`); grep announce/liveRegion = 0; toasts in-app (2,2 s, texto puro) sem `liveRegion`. *Correção:* `role = Role.Button` no `combinedClickable`/`selectable` do `FocusableSurface` (besides barato, beneficia dezenas de superfícies) + liveRegion nos estados/toasts de player e grelha. *Severidade:* Média/Alta.
- **R4-I7 — Alta (RTL) ✅ — iconografia nunca espelha em RTL.** *Ficheiro/linhas:* `ui\components\OwnTVIcon.kt` (grep `LayoutDirection|scale(-1)` = 0) — BACK/CHEVRON/PAGE têm coordenadas fixas. *Sintoma:* em árabe/hebraico os chevrons apontam o lado contrário ao movimento. *Correção:* no wrapper do icon, `Modifier.scale(-1f, 1f)` (ou espelho no Canvas) quando `LocalLayoutDirection.current == RTL` — para ícones direccionais apenas, não para logos/avaliações. *Severidade:* Alta a11y-RTL (baixa população actual, custo ínfimo).
- **R4-I8 — Média ⚠️ — o «floor» de legibilidade do Glass só cobre FLOATING/CONTAINER.** *Ficheiro/linhas:* `ui\theme\Glass.kt` L284 (`adaptiveEligible = FLOATING || CONTAINER`) e L286-295 (`requiredLegibilityAlpha` só para esses depths); ⚠️ matiz: o mínimo absoluto `coerceIn(0.22, 1)` em L304 mantém-se em todos os casos — o que não é aplicado nas cards INLINE/estado idle é o *floor por luminância do backdrop*. *Correção:* estender a amostragem de luma às superfícies INLINE e às cards idle (o mecanismo de amostragem já existe); eliminar o risco WCAG com backdrops claros. *Severidade:* Média.
- **R4-I9 — Média ✅ — grelha EPG: `RowHeight` fixa 64 dp e o topo do texto sem clamp — fonte XL recorta.** *Ficheiro/linhas:* `GuideCore.kt` L72 (`val RowHeight = 64.dp`) e L181 (`val top = (h - (title.size.height + time.size.height + 2)) / 2f` — negativo quando os dois estilos excedem a altura). *Correção:* derivar a altura da row da altura real medida dos estilos (cache por (estilo, escala) — o cache do `GuideRowCache` já existe) e clamp `top >= 0`; EpgScreen案子 L944/989 passam a herdar. *Severidade:* Média (fonte XL / `UiFontScale` 140%).

**Governança entre features**

- **R4-G1 — Alta ✅ — o fullscreen é o único playback invisível ao `OpenStreamRegistry` (hero já conta; matiz).** *Ficheiro/linhas:* `OwnTVShell.kt` L500-504 (comentário: "…fullscreen playback deliberately never claims a connection in OpenStreamRegistry — that register is the Multiview/recording budget"); `ConnectionBudget.kt` é aritmética sobre o `open` de `openOn(sourceId)` (registro). ⚠️ **Matiz à alegação do hero**: `di\PlayerModule.kt` L51-62 passa o registry ao `HeroPreviewEngine`, e o `claimPreviewSession` L187 chama `registry.claim(sourceId, WATCHING)` — o hero **regista** o claim no mesmo registry (é contado pelos tiles/recordings); só escapa à *recusa* (`claim()` não pergunta ao budget). O membro invisível é mesmo o fullscreen. *Sintoma:* com `maxConnections=2`: fullscreen A (não conta) + tile B (conta) + gravação A (conta, o budget vê 2) = **3 sessões reais para 2** — o provider devolve 458 e quem morre (tile ou gravação) é aleatório. *Correção:* registar um WATCHING claim do fullscreen no mesmo registry no `tune`/`OwnTVShell` e liberar no `close` — como já faz o hero (sem admission; a admission completa fica para tiles); com isso o exemplo acima passa a ser recusa no budget antes do 458. *Severidade:* Alta. 🔗 R3-D3 (aviso de conflitos na agenda) · §12.3.
- **R4-G2 — Alta ✅ — recusa pontual de gravação → `MISSED` imediato, sem fila para o slot.** *Ficheiro/linhas:* `RecordingEngine.kt` L152-155 (`when (grant) { Refused -> markMissed; Allowed -> start }`) e L193-198 (`tryClaim` null → `markMissed(NO_CONNECTION)`); ordem de chegada na janela (`POLL` 2 s). ⚠️ Distinguir famílias: R3-33 = app morto/janela perdida; R3-D3 = reposição de janela; **R4-G2 = justiça de fila quando o slot está ocupado por um claims não-registado (R4-G1) ou outra gravação disparada primeiro**. *Correção:* `NO_CONNECTION` com janela ainda aberta → estado `PENDING` + reverificação no mesmo ciclo de drenagem até `stopMs` (o relógio de janela já existe) antes de marcar `MISSED`; resolver em conjunto com o R4-G1 para não duplicar efeito. *Severidade:* Alta-fairness.
- **R4-G3 — Média/Alta ✅ — tile em `ERROR`/`DecoderExhausted` mantém o claim indefinidamente.** *Ficheiro/linhas:* `MultiviewState.kt` L91 (`claims` map), L175-181 (`admit. Allowed` → `claims[tile] = claim`), L127-139 (`silenceFailed` só muda o som); só `expirePlaybackWait` L194-205 (janela de arranque) e o clear/retire do utilizador liberam. *Sintoma:* tile morto aos olhos e conto provider às costas — as próximas tentativas de tile/gravação nesse fontes herdam o número conta. *Correção:* no listener de estado da engine, quando `ERROR` terminal → `retireTile(tile)` mantendo a mensagem de recusa/falha e libertar o claim. *Severidade:* Média/Alta.
- **R4-G4 — Média ✅ — o 458 não re-mediu nem realimenta o medidor.** *Ficheiro/linhas:* `RecordingEngine.kt` L538-542 (`SESSION_LIMIT_CODE` → devolve `NO_CONNECTION` / fim da tentativa, sem tocar no medidor); `LiveStreamQuirks` vive em memória; `ConnectionLimits` persiste por fonte (AddSourceScreen L228/L281 DISCURSO, ManageSourcesScreen L379 skip) mas **nada** o re-probe depois de 458; `ConnectionMeasurementWorker` sabe suspende via `WatchSession:60` ❓. *Correção:* 458 → `markNeedsProbe(sourceId)` + re-probe assíncrono (o worker e o `ProbeChannelSource` já existem); uma **bandeira temporária** de redução no budget para não repetir ciclos. *Severidade:* Média.
- **R4-G5 — Média ✅ — `deleteSource` não toca nas gravações da fonte.** *Ficheiro/linhas:* `SettingsViewModel.kt` L1406-1424 — cancela sync, invalida stalkerAuth, apaga a linha; nada de `RecordingEngine`. Gravações `SCHEDULED` da fonte acabam `CHANNEL_GONE` pelo fluxo normal; ❓ o ramo icast (resolve `target` sem source → `row.streamUrl` одна-session) pode rodar `PARTIAL STREAM_UNAVAILABLE` até `stopMs` ❓ (não re-traçado linha-a-linha). *Correção:* na transacção `deleteSource`, `engine.stop(id, cancel=true)` + `markMissed(CHANNEL_GONE)` para `SCHEDULED/RECORDING` da fonte. *Severidade:* Média.
- **R4-G6 — Média/Baixa ✅ — TOCTOU no timeshift local; a mensagem apaga a recusa real.** *Ficheiro/linhas:* `LiveViewModel.kt` L294-310 — `connectionBudget` só-SAN检查 (L294-295), depois `resolve`/limpeza IO (L296-306), e só **depois** o `tryClaim` (L310) que pode recusar (→ exception → catch → toast genérico `local_timeshift_failed`). *Correção:* `tryClaim` **antes** do resolve/limpeza e liberar se fallier; a mensagem de falha deve transportar a razão (`StreamGrant.displayText`). *Severidade:* Média/Baixa. 🔗 R2-17 (mesma função, check(`delete`)).
- **R4-G7 — Média/Baixa ✅ — `ENDED`/`CLOSED` da sessão local não fazem dispose do claim.** *Ficheiro/linhas:* `LiveViewModel.kt` L322-329 — o watcher desperta em `FAILED||ENDED||CLOSED` mas só mostra toast no `FAILED`; nunca chama `disposeLocalTimeshift` no ENDED/CLOSED → claim prestado até sair do player. *Correção:* no watcher, `ENDED||CLOSED` → `disposeLocalTimeshift(retain=false)`. *Severidade:* Média/Baixa.

### 12.2) Melhorias e "o que está bem" reconfirmações da Ronda 4

1. **`reportFullyDrawn` nunca chamado** ✅ (grep 0; o carimbo de perf já existe nos diagnósticos, mas `am start -W`/vitals nunca sabem a TTFD) — chamar após a primeira frame da Home; barato e fecha o ciclo de observabilidade de arranque.
2. **Postura TLS ok** ✅ — `ExtraTrustAnchors` completa os roots (não os substitui), hostname verifier normal, sem bloqueio de TLS 1.0/1.1 deliberado (notar compat na doc, não no código).
3. **Scrim de legibilidade** ✅ — `MainActivity.kt` L544 usa `Color.Black.copy(0.16f)` fixo, igual em dark/light — considerar token por tema/backdrop-luminância (a infra do Glass L286-295 é o padrão a copiar).
4. **Popup scale já compensa `UiFontScale`** ✅ (`PopupFontTheme`) — já-bem; não duplicar lavoro.
5. **`onTrimMemory` owner ok** ✅ (nota R2-5 mantém-se; nenhum novo risco identificado nesta ronda).
6. **`enableOnBackInvokedCallback` ausente** ✅ — cosmético preditivo em mobile; em TV sem impacto — nota Baixa, não bug.
7. **`values-night` + `themeMode=AUTO` (segue sistema)** ✅ — já-bem; banner/leanback/landscape/posts deliberados ✅.
8. **`ANIMATOR_DURATION_SCALE` ignorado (0 leituras)** ✅ — o visual profile corta blur/glint mas os tweens correm a escala do sistema; respeitar a mesma escala (1 leitura no provider da animação).
9. **onStop player/preview handlers** ✅ (`MainActivity.kt` L154/L156/L486 — player, preview, hero todos `onAppBackgrounded`/stop); `pauseWhenOutputDisconnects=false` deliberado na TV ✅ (`PlayerModule.kt` L72).
10. **Notas Baixa da ronda:** `ExoSubtitleEngine` usa UA por canal sem fallback "learned" (sudoeste; baixa) — ❓; `toMpvHeaderFields` descarta valores com vírgula — ❓ Baixa; captive portal não detectado (nota B-19/20/21 OK: reconexão com URL fresh Stalker ok, headers reescritos, isOnline poll 20 s, 416 handled).

### 12.3) Ideias de inovação — lote 4 (18, tabela completa)

Integrações indicadas verificadas (ficheiros existentes; ✅ = o citado existe). Greps de confirmação: `sleepTimer`, `highContrast`, `reduceMotion`, thumbnails de seek, CSV export, guest, delete-after-watch, favorites-export = **0 hits**; `HistoryDao`/`WatchHistoryEntity` existem; `FontCustomization`/`UiFontScale` existem; `BackgroundImageChooserDialog` + `ingestBackgroundImage` existem.

| # | Nome | Problema → Solução | Integração (verificada) | Esforço | Diferencial |
|---|---|---|---|---|---|
| A1 | Medidor de Bytes por Fonte | Ninguém sabe quanto gasta cada playlist/CDN → deltas de `TrafficStats` por sessão mapeados à fonte activa; agregação mensal por fonte; alerta "X GiB este mês"; painel «Dados» nas Definições | `features\more\UserDataScreen.kt` ✅ (novo bloco), `diagnostics\phase4diagnosticsMetrics.kt` ✅, SettingsRepository (novo key-set) | S/M | Transparência por fonte que nenhum rival dá |
| A2 | Auto-qualidade por rede medida | 4G ilimitado ≠ limitado; nada adapta qualidade → quando `isActiveNetworkMetered`, limitar o vídeo (Exo `TrackSelectionParams` / mpv `max-h`) + banner "80% menos dados" + toggle por-SSID; âncora: comentário "never metered" no `MetadataSettingsScreen` | `features\settings\MetadataSettingsScreen.kt` ✅, `dataSaver` já existe no `SettingsRepository` ✅ (L1208), `ConnectivityObserver` ✅, engine path (`LiveTuneController` core ✅) | M | Economia honesta sem config manual |
| A3 | Sleep Timer + auto-shutoff | TV fica a tocar a noite toda → HUD 15/30/60 min + «fim do programa» via EPG; fade do volume; guarda Kids: não retomar sem PIN | `player\PlayerHudControls.kt` ✅ (novo row), EPG (`ProgrammeReminderStore` core-style) para "fim do programa", `PlaybackSession` para fade | S | Nenhuma IPTV TV tem sleep timer com fim-de-programa |
| B4 | Rail «Novidades na Fonte» | Novos episódios/canais ficam perdidos no catálogo → diff local entre syncs por (fonte, primeiro-visto) → rail Home «adicionado nos últimos 7 dias» com filtro por fonte | `features\home\HomeScreen.kt` ✅ (rail infra), diff em `M3uSyncer`/`SyncManager` (stats em memória — 🔗 §10.2-3) | M | Descoberta editorial local |
| B5 | «Mais Vistos por Ti» + «Redescobrir» | A app regista o que a casa viu (WatchHistory) mas nunca mostra → ranking local de `WatchHistoryEntity` + rail «não acabastes» (antigo, sem progresso) | `HistoryDao`/`WatchHistoryEntity` ✅ (core), rail com o mesmo motor do B4, `HomeViewModel` | S | Personalização 100% on-device |
| B6 | Prévias de seek por frame | Seek às cegas em gravações → `screenshot-raw` mpv no hold-seek → miniaturas no scrub; cache LRU em disco, pastas próprias da app | `player\LiveScrubGesture.kt` ✅, mpv host (`OwnTVPlayer`), cache em `app\cache` próprio | M/L | Navegação de gravações estilo Netflix, local |
| B7 | Ficha TMDB nas gravações + sinopse hidratada | Gravações são ficheiros sem ficha; provider trunca sinopse («…») → género/cartaz/„Próx. ep.» na `RecordingEntity`; quando a sinopse existe parcial, buscar a completa (guard fuzzy de título) | `RecordingsViewModel`, `TmdbProvider`/`MetadataRepository` ✅ (tier própria-key existente), guard com `EpgMatcher` fuzzy | M | Biblioteca de gravações ao estilo Netflix, sem cloud |
| C8 | Preset «Idade Maior» | Sem um preset único para idosos → switch aplica fonte XL (`FontCustomization`) + contraste de foco reforçado + listas menos densas (row heights dos guides +12%) + timers mais longos | `ui\theme\FontCustomization.kt` ✅ (+ novo preset no mesmo pipeline), `GuideCore`/`EpgScreen` (usa o fix R4-I9) | S/M | Um clique ≡ acessibilidade real |
| C9 | Alto contraste + Reduce Motion | Nada honra `AccessibilityManager` (0 leituras hoje) → tema próprio alto-contraste + respeitar reduce-toggle do sistema; blur desligado no modo | `ui\theme` (novo tema), composição já provider-based em `MainActivity` ✅ (pattern do `fontScale`) | M | Conformidade real WCAG AA |
| C10 | Pausa por inactividade Kids | Sessão kids pode durar o dia → sem input N min → auto-pause + PIN retomar (solidifica §5-#14) | `PlaybackSession` + `ProfilesViewModel` (PIN infra já existe) 🔗 §10.2-10 | S | Guarda parental sem cloud |
| D11 | Snapshot pré-restauração + rollback | Restauro destrutivo sem rede de segurança → auto-`.own` cifrado interno antes de restore/purge; últimos 3, rotação estilo `AutomaticBackupPolicy`; painel recovery com «voltar» | `BackupManager`/`BackupCrypto` ✅ (pipeline cifrado existente), `LocalBackupOperation`/`RemoteBackup*` ✅ (ecrãs existem), toggles em `BackupScreen` ✅ | S | O restauro deixa de ser aposta |
| D12 | Sync parcial LAN (favoritos/resume) via Companion | Companheiro atual = add-source/backup inteiro → pacote JSON pré-mapeado (favoritos/resume/favoritos-canal) assinado pelo PIN, sem credenciais; merge "mais recente ganha" | core `companion\` ✅ (`CompanionHttpProtocol.kt` existe; endpoints no `CompanionHttpServer`), merge ao lado `LocalBackupOperation` | M | Interop entre dispositivos sem cloud |
| D13 | Export histórico CSV/JSON | Utilizador não pode sair da app (data-lock) → export local via `StorageBrowser` em CSV/JSON; sem credenciais de fontes em qualquer export | `features\more\UserDataScreen.kt` ✅ + `ui\components\StorageBrowser.kt` ✅ + `HistoryDao` ✅ | S | Export GDPR local | 
| D14 | «Assistir e apagar» gravações | Espaço finito + sem política → marcador por linha + execução no fim do playback; opcional «lixo 48 h» | `RecordingEntity` + `RecordingsViewModel` + gancho no fim no player core | S | Gestão automática doméstica |
| E15 | Telemóvel como teclado/trackpad | Typing na TV custa → Companion «/type» injecta no campo focado + página de texto/numpad; trackpad mapeia D-pad | core `companion\` ✅ endpoints; reutiliza `TextInputDialog` ✅ (gestor do campo focado) | M | O telemóvel como acessório de sala |
| E16 | Lembrete no telemóvel via LAN | TV desligada fica sem lembretes → push Companion à página ligada «X está a começar em 5» | core `companion\` + `ProgrammeReminderStore`; ⚠ depende da fix R2-12 (AlarmManager) para fiabilidade em standby | S/M | Lembretes distribuídos na casa | 
| E17 | Modo Convidado | Amigos/hotel sem herança de perfil → sessão temporária (PIN, 10 min), live de lista branca, sem settings/downloads/export | `ProfileGateSessionViewModel` ✅ (gate já em place), whitelist coerente com o fix R3-19 | S-M | Caso de uso hoteleiro sem conta |
| F18a | Favoritos em texto | Favoritos estão presos à app → export/import lista ordenada «1. Nome» + reorder por colagem + fuzzy-match do `EpgMatcher` ✅ + desambiguação por LCN | `FavoriteDao`/`ChannelDao` ✅, fluxo `UserDataScreen`/`StorageBrowser` de D13 | S | Portabilidade TV↔TV sem nuvem | 
| F18b | Identidade do canal | Nome/logo do provider são imprecisos → override de ícone por canal + nota pessoal (badge no card/overlay), ingestion via pipeline de background existente | `CustomizationStore` ✅ (DataModule L83), badge em `GuideProgrammeCells`/`ChannelListOverlay` ✅, ingest via `ingestBackgroundImage` ✅ | S-M | A app recorda o que a família decide |

TOP 5 lote 4: **E15** (telemóvel teclado/trackpad), **D11** (snapshot pré-restauração), **B5** (vistos/redescobrir), **A2** (metered auto-qualidade), **C8+C9** (idade maior + alto contraste).

## 13) Reavaliação final consolidada — versão 2 (Rondas 1-4)

Versão final do inventário; a §11 mantém-se como registo histórico da versão 1. Veredictos: ✅ válido · ⚠️ parcial/matiz · ❌ refutado · 🔁 já coberto · ❓ por confirmar · 🔗 cruzamento.

### 13.1 Bugs Ronda 1 (§1)

| ID | Achado | Veredicto | Solução |
|---|---|---|---|
| B1 | SettingsScreen: 44 flows no root + catálogo restart | ✅ | colectores por row + catálogo fora da recomposição (§1) |
| B2 | release assinado com debug key + `E:/…` hard-code | ✅ | keystore exigida + env/property (§1) |
| B3 | hero recomposto ~12×/s | ✅ | composable folha + animação (§1; duplo blur §8.2-3) |
| B4 | download truncado gravado COMPLETED | ✅ | `done==total` + digest + PARTIAL (§1; família fechada por R3-31/32) |
| M1 | deep link re-dispatch no recreate | ✅ | consumir/limpar intent (§1) |
| M2 | prewarm EPG aos 20 s duplicado | ✅ | rememberUpdatedState (§1) |
| M3 | polling 20 Hz na escada | ✅ | sinalizado com CompletableDeferred (§1) |
| M4 | coletores condicionais trocam | ✅ | sempre ligados (§1) |
| M5 | backdrop re-blur ("leak") | ✅ (leak ❌) | quantizar key do blur (§1) |
| M6 | credenciais Xtream sem escape | ✅ | encoding por segmento (§1) |
| M7 | M3U assumido UTF-8 | ✅ | sniff BOM/1251/1256/ISO (§1; padrão estendido R2-6) |
| Baixa 1 | universal APK ~104 MB | ✅ | `isUniversalApk=false` (§1) |
| Baixa 2 | back 2 presses na busca | ⚠️ | padrão Android; `enabled` mutuamente exclusivos (§1) |
| Baixa 3 | `indexOfFirst` overlay | ✅ | Map<id,Int> (§1; padrão EPG §8.2-2) |
| Baixa 4 | poll 32 ms press-and-hold | ✅ | Animatable (§1) |
| Baixa 5 | split `&` headers | ✅ | canonical antes de partir (§1; `+`/`%` R4-R6) |
| Nota | mpvExecutor shutdown | ⚠️ | drena fila; risco só `exit()` imediato (§1; R2-5) |

### 13.2 Hipóteses R1 + arquitetura (§2/§3)

| ID | Achado | Veredicto | Solução/estado |
|---|---|---|---|
| H1 | media-sequence reset na gravação | ⚠️ | mecanismo ✅ / gatilho ❓ (§2) |
| H2 | PlaybackSession sem SupervisorJob | ⚠️/❓ | estrutura ✅ / impacto ❓ (§2) |
| H3 | M3uSyncer renumera episódios | ❓ | flush boundary sem SxxEyy (§2) |
| H4 | relógio/EPG sem NTP wait | ⚠️ reforçado | R2-13 confirma 2.ª causa; R3-37 agenda |
| H5 | checksum libmpv | ❓ | §3 (distinto do digest R2-22 ✅) |
| H6 | UiPerformanceSummary periódico | ❓ | §2 (medir com o próprio monitor) |
| AQ | monolitos/DI/versionCode/higiene/testes/namespace | ✅ | plano §3; gaps de teste nos controllers extraídos |

### 13.3 Bugs Ronda 2 (§8.1)

| ID | Achado | Veredicto | Solução |
|---|---|---|---|
| R2-1 | setVolume ignora percent (~data-path morto) | ✅ | honrar percent + decidir política (§8.1) |
| R2-2 | mute não reflete ícones | ✅ | expor isMuted State / sync 0-100 |
| R2-3 | toggle engine perde pista áudio | ✅ | carry pendingExoAudio / re-selecionar aid |
| R2-4 | time-pos/duration INT64 (1 Hz) | ✅ | MPV_FORMAT_DOUBLE |
| R2-5 | mpvAsync pós-release Rejected | ✅ | flag released + try/catch execute |
| R2-6 | legendas Exo mime/charset | ✅ | mimes correctos + UTF-8 convert |
| R2-7 | latch undecodable | ⚠️ | reset por item existia; repor por config |
| R2-8 | probe trim sem isLiveContent | ✅ | condicionar trim; re-verificar FILE_LOADED |
| R2-9 | live seek no-op mpv | ✅ | DVR window ou esconder barra |
| R2-10 | END_TOLERANCE VOD curto | ✅ | dur/10 + tempo mínimo |
| R2-11 | pendingFps re-assert | ⚠️ | janela surfaceCreated — limpar com o setting |
| R2-12 | lembretes in-memory | ✅ Alta | Room + Alarm/WorkManager + boot 🔗 R3-41; E16 depende |
| R2-13 | XmltvTime "+02"→0 | ✅ Alta | aceitar HH; contabilidade de feed |
| R2-14 | Xtream TZ sem DST | ⚠️ | IANA-path existe; override + gap-detect |
| R2-15 | CatchupJumps DST | ❓ | ZonedDateTime + golden tests |
| R2-16 | janela coerceda 7 d | ⚠️ | windowStart ✅ / canCatchup refutado |
| R2-17 | limpeza check(delete) | ✅/❓ | runCatching + sweep; ⚠ a mesma função tem o TOCTOU R4-G6 |
| R2-18 | servidor ts fecha sem resposta | ✅ | 503+Retry-After / fila curta |
| R2-19 | quota exige segmento ≤ cap | ✅ | rejeitar log+continue |
| R2-20 | template `%` verbatim | ✅ | map strftime + ISO fallback |
| R2-21 | Companion sem TLS/PIN/query | ✅ Alta | header+nonce; HKDF/PSK; ACL RFC1918 |
| R2-22 | update sem SHA-256 | ✅ | digest antes de install 🔗 R4-P5 |
| R2-23 | DashManifest XXE | ✅ | secure-processing/XmlPullParser |
| R2-24 | PIN `==` sem rate-limit | ✅ | MessageDigest.isEqual + delay |
| R2-25 | cleartext global | ✅ | networkSecurityConfig allowlist |
| R2-26 | Kids títulos | ⚠️ | category-only por design; matcher opt-in |
| R2-27 | file_paths raiz `.` | ✅ | subpastas específicas |
| R2-28 | M3U sem caps | ✅/❓ | caps bytes/itens; `file://` por confirmar |

### 13.4 Ronda 3 (§10.1) e Ronda 4 (§12.1) — consolidado

| ID | Achado | Veredicto | Solução |
|---|---|---|---|
| R3-1 | gate contornável via "Adicionar perfil" | ✅ Alta | credencial de adulto + re-gate |
| R3-2 | edição sensível sem credencial | ✅ Alta | `emptySet()` = negado por omissão |
| R3-3 | restauro escreve isKids sem gate | ✅ | credencial no ramo confirmado |
| R3-4 | "doador preserva lastSyncAt" | ⚠️ | copy(null) real; enqueue pós-restauro |
| R3-5 | (slot) | ❓ | Apêndice G |
| R3-6 | playing com canal invisível | ✅ | mover a 1.ª escrita para após o gate |
| R3-7 | importador sem caps | ✅ Alta | caps por secção |
| R3-8 | merge remapeia id cru | ✅ | match por nome/avatar ou descartar |
| R3-9 | auto-refresh só no onStart | ✅ Alta | WorkManager periódico |
| R3-10 | delete sem cascade/purge dead | ✅ | FK CASCADE/purge na transacção |
| R3-12 | updateSource reverte blanks | ✅ | UI explícita + invalidação Xtream |
| R3-13 | Testar vs Adicionar inconsiste | ✅ | unificar guard local |
| R3-14 | lastFailedSource morto | ✅ | Failed(source) no catch |
| R3-16 | re-sync renumera ids | ❌ | upsertStable preserva (evidência) |
| R3-17 | FTS sem unicode61 | ✅ | tokenize=unicode61 + normalizar queries |
| R3-18 | cache negativa 7 d sob 200 | ✅ | não cachear corpo inesperado |
| R3-19 | curated sem perfil | ✅ | filtro antes do retorno 🔗 R2-26 |
| R3-21 | TMDB sem retry | ⚠️ | retry existe; tryConsume antes do parse |
| R3-22 | IOException → ApiException | ✅ | tipar NetworkException |
| R3-23 | falha mata ronda | ✅ | re-agenda curta com cap |
| R3-24 | hero repõe índice 0 | ✅ | coerceIn como trending |
| R3-25 | branch "query curta" morto | ✅ | colapsar (hint quando ==1) |
| R3-26 | requesters sem re-focus | ✅ | remember keyed por tiles |
| R3-27 | LIKE sem escape | ✅ 🔁 | escapeLike/FTS 🔗 R3-17 |
| R3-28 | race fetch de legenda | ✅ | single-flight por media |
| R3-29 | localKey escrito e esquecido | ✅ | ler no pickBest ou drop |
| R3-30 | detalhes por key global | ✅ | alinhar com id local |
| R3-31 | colisão de episódios + 416 | ✅ Alta | S{xx}E{yy} + desambiguação |
| R3-32 | truncamento 416 normal | ⚠️ | cap vFAT honesto; mensagem clara |
| R3-33 | markMissed sem recuperação | ✅ Alta | re-agendar janela aberta (filosofia R4-G2) |
| R3-34 | UA fonte ignorada em download | ✅ | fonte UA no fallback 🔗 R4-R3 |
| R3-37 | wall-clock sem NTP check | ✅ | recalcular nextAt no init 🔗 §2 |
| R3-38 | cleanup pula readers | ✅ (matiz) | onRelease GC |
| R3-39 | relinkAfterSync? inexistente | ❓ | resolver por channelExternalId |
| R3-40 | downloads sem bulk | ✅ | menu de contexto |
| R3-41 | ponte lembretes→gravações | ✅ | conector Reminder→schedule 🔗 R2-12 |
| R4-P1..P5 | plataforma (EPG read perm; notif runtime; FGS guard; secção/multiview não persistidos; update pre-check) | ✅ | §12.1 Plataforma |
| R4-R1..R8 | rede (DNS cache/callTimeout; Referer/headers download; Retry-After; metered; `+` decode; proxy Keystore; redirect leak) | ✅ (R8 ❓) | §12.1 Rede |
| R4-I1..I9 | i18n/a11y (glob strings*.xml; app fora do pipeline; pt perde −96/−30; ≈22 idiomas sem values no app; 12/24 h; EPG Canvas cego; Role/announce; RTL mirror; Glass floor; RowHeight XL) | ✅ (percentagens ❓ I1) | §12.1 i18n/a11y |
| R4-G1..G7 | governança (fullscreen fora do budget; MISSED sem fila; tile ERROR mantém claim; 458 sem re-probe; deleteSource↔gravações; TOCTOU/ENDED timeshift) | ✅ | §12.1 Governança |

### 13.5 Melhorias (lotes 2-4)

| Grupo | Cobertura | Veredicto |
|---|---|---|
| §8.2 1-14 | FTS nas secções; prefetch coalescedo; LiveHero blur; favoriteIds WhileSubscribed; LocaleStore cache; portal hash; GuideCore measure; EpgSourceStore lastError; NUMBER_WORDS; DST tests; DNS flag; VolumeBoost: ligar ou apagar; subtitle nudge; (M12=R2-25 🔁, M10 🔁 §3) | ✅ |
| §10.2 1-12 | transacção create perfil; purge prefs `*_<pid>`; stats sync persist; pós-restauro sync; bulk downloads; unicode61; WorkManager; wizard saveable; kids timer; recovery DataStore; tuneTile hardening | ✅ (8/12 ❓ design) |
| §12.2 1-10 | reportFullyDrawn; TLS nota; scrim token; popup scale ok; onTrim ok; onBackInvoked nota; values-night ok; ANIMATOR_DURATION_SCALE; onStop handlers; notas Baixa rede | ✅ |

### 13.6 Ideias — inventário final 50+18=68

| Lote | IDs | Cobertura |
|---|---|---|
| 1 (§5) | 1-15: Stream-Health Score, prewarm rede, O que ver agora, Catch-up Flush, overlay diagnóstico, multiview N, Sala de Bits, voz sistema, busca semântica, sumário programa, auto-replay, auto-quirks, bundle Hoje, kids parental, ZIP comunidade | ✅ integrações verificadas; TOP 5 mantidos |
| 2 (§8.3) | 1-18: Espelhos Vivos, Substituto, Radar down, Templates provider, Tracks perfil, Loudness, anti-ABL, Deinterlace auto, DRC noite, Guildas, Watch Party, Manutenção, IFTTT-like, RecommendationRow, PiP, Material You, TTS setup, Combos D-pad | ✅ TOP 5: espelhos, substituto, RecommendationRow, tracks, watch party |
| 3 (§10.3) | 1-17: Holofote conflitos, Ruler gravação, retenção visto, fichas biblioteca, agenda semanal, pulso favoritas, guia por género, cartão partilha, /play Companion, modo viagem, LCN local, atalhos 2-teclas, API local, score line-up, multiview mosaico, art programa, doutor fontes | ✅ Top 5: #1/#2/#5/#10/#9 |
| 4 (§12.3) | A1-A3, B4-B6, B7, C8-C10, D11-D14, E15-E17, F18a/F18b | ✅ TODAS com integração verificada nesta ronda; TOP 5: E15, D11, B5, A2, C8+C9 |

### 13.7 Alterações de veredicto / matizes nesta ronda

| Ref. | Alegação submetida (R4) | Veredicto final | Evidência |
|---|---|---|---|
| Analista B-14 | "gen_supported_locales.py:119 / validate_strings.py:95" em `tools\i18n\` do repo | ⚠️ caminho ❌ / mecanismo ✅ | os scripts **não existem** na árvore do repo (só 5 scripts i18n); existem no snapshot do Core, donde linhas L119/L95 = `glob("strings*.xml")` confirmadas; contagem core: 2568 strings, 2413 no glob → 155 fora (141 alegada = mesma ordem) |
| Analista D-1 (parte) | "HeroPreviewEngine claim sem connectionBudget" | ⚠️ matiz | o hero **regista** o claim no registry (`PlayerModule.kt` L51-62 passa o registry; `HeroPreviewEngine.kt` L187 `claim(WATCHING)`) — conta no `openOn` para tiles/recordings; só escapa à **recusa** (o `claim()` não pergunta ao budget). O membro fora do registo é o **fullscreen** (deliberado, `OwnTVShell.kt` L500-504) |
| Analista A-4 | "`RecordingWorker.kt:36` (app)" | ⚠️ caminho | o worker vive no **core** (`core\recording\RecordingWorker.kt` L36 vs L43-47 — continua ✅ como achado) |
| R4-R3 vs R3-34 | download sem UA de fonte | 🔗 ampliação | R3-34 cobria UA; o `httpReferer` do `SourceEntity` (serializado e usado pelo `SourceTester` L81/L116) também é ignorado — mesma correção, args adicionais |
| R4-G2 vs R3-33/R3-D3 | "recusa → MISSED" | 🔗 famílias distintas | R3-33 = app morto; R3-D3 = janela; R4-G2 = justiça de fila sob slot ocupado (ligado ao R4-G1) — três correções diferentes |
| Ok explícitos | onStop/CEC/FontZoom/values-night/leanback | ✅ | §12.2 — nada a corrigir |

## 14) Ronda 5 — persistência, concorrência, robustez, build/devops (com soluções)

Quinta passagem com cinco analistas — **A** persistência/dados, **B** concorrência/ciclo de vida, **C** robustez/higiene, **D** build/devops/testes/deps, **E** inovação lote 5 (25 candidatas). Cada achado re-lido no código actual (app + snapshot Core `e2081e78…`); os 13 itens obrigatórios definidos pelo conselho foram spot-verificados um a um (§15.1 e Apêndice I) e os pequenos ⚠️/❓ mantêm o Marca do analista. Dedupes explícitos desta ronda: **R5-C9 🔗 M7** (nota, não novo), **R5-C8 🔗 R2-13** (nota), **R5-B5 🔁 Baixa 4 §1** e **R5-B6 🔁 §8.2-5** (repetições rebaptizadas como nota), **R5-C7 🔗 §10.2-3** e **R5-B4 🔗 §8.2-4** e **R5-D4 🔗 Baixa 1** (mesma família / ampliações mantidas), e **R5-B1** mantém-se *novo* (irmão do R4-P3 — mesma família de correção, ficheiro/gatilho distintos: DOWNLOAD vs RECORDING). Refutações: **nenhuma** — nenhuma alegação obrigatória caiu na verificação. Balanço: **25 achados novos/melhorias validados + 4 notas de dedupe** (29 linhas na tabela); **18 ideias no lote 5** (de 43 candidatas; §14.3).

### 14.1) Bugs e melhorias novos (com soluções)

| ID | Título | Tipo | Severidade | Localização (ficheiro:linha) | Evidência (1 linha) | Marca | Solução |
|---|---|---|---|---|---|---|---|
| R5-A1 | Auto-match EPG guarda via N reescritas DataStore sequenciais | melhoria | Média | app `features\epg\EpgViewModel.kt` L887/L956; core `customize\CustomizationStore.kt` L244 (cada `update` = 1 `edit` completo do blob do perfil, L182-183) | `for ((key, epgId) in outcome.applied) customize.setEpgMatch(...)` — uma edit completa por match; o próprio código já tem o padrão single-edit (`applyBulkRenames` L269-273) | ✅ | Adicionar `setEpgMatches(profileId, type, map)` que faz **um** `edit { it.epgMatches + map }` e usá-lo nos loops de auto-match/aceitar-tudo. |
| R5-A2 | Cache de legendas (`filesDir/subtitles`) cresce sem teto nem poda | melhoria | Média/Baixa | core `subtitles\SubtitleRepository.kt` L36; remoção só manual L230-259 | `cacheDir = File(appContext.filesDir, "subtitles")` sem purge por idade/tamanho; `MediaStorageBudget.scanStorageUsage()` (L54-58) contabiliza apenas gravações+downloads | ✅ | Ledger LRU (`subtitle_cache.lastUsedAt`) + teto (ex. 64 MiB) destruído no arranque (hooks de touch/idade já existem no `SubtitleDao`). |
| R5-A3 | Prefs de apresentação **globais** saltam entre perfis (sort, view-mode, configs por canal) | melhoria (isolamento) | Média | core `settings\SettingsRepository.kt` L1080-1082 (sortLive/Movies/Series), L1279 (`sortGuide`), L1125-1129/L1164-1169 (VOD/EPISODE_VIEW_MODE), L402 + L1394-1430 (`CHANNEL_PLAYBACK_CONFIGS` global por canal) | chaves `startup_mode_$pid`/`startup_channel_$pid` (L634-656) já são por-perfil, mas o resto do look é um blob global — perfil Kids herda sort/engine/configs do perfil adulto | ✅ | Migrar (na próxima mudança de prefs) as chaves de apresentação para o padrão `_<pid>` com fallback global, à imagem de `startup_mode_$profileId`. |
| R5-A4 | Import de backup aceita ficheiro de uma versão **mais recente** da app sem aviso | melhoria | Baixa | core `backup\BackupImportValidation.kt` L11; `BackupManager.kt` L791, L1008 (único gate `< 17`), L106 (escrita `version: 25`) | blocos desconhecidos de builds futuras são silenciosamente ignorados — nenhuma mensagem "backup de versão mais nova" no dispositivo | ✅ | `require(root.optInt("version") <= BUILD_BACKUP_VERSION)` com erro mensurável; ou banner "backup de versão mais nova, pode perder campos". |
| R5-A5 | N+1 residual no planner do Watch Next (getById por linha de progresso filmes) | melhoria | Baixa | core `launcher\LauncherRecommendationPlanner.kt` L73-86 | ramo MOVIE chama `movieDao.getById(progress.itemId)` por cada row de progresso (episódios/shows já têm fetch agrupado — L51-71/L100-106) | ✅ | Batch: `movieDao.getByIds(ids)` (IN clause) num passo antes do loop, igual aos episódios chunked. |
| R5-B1 | `DownloadWorker`: `setForeground` sem `runCatching` mata o drain inteiro sem retry | bug | Média | core `download\DownloadWorker.kt` L30 (doWork L25-47) | L30 `setForeground(...)` nu dentro de `try{...}finally{gate.failed(owner)}` **sem catch** — Android 12+ (saver/arranque em 2.º plano) lança `ForegroundServiceStartNotAllowedException` → fila fica RUNNING sem engine; só relança no próximo kick (`DownloadManager.kt` L47/100/118/144) | ✅ 🔗 R4-P3 | Envolver o 1.º `setForeground` em `runCatching` como o próprio L38-40 faz com `setForegroundAsync` (e como `EpgSyncWorker.kt` L72/184); em falha, `Result.retry()`. |
| R5-B2 | Relógios de composição não lifecycle-aware continuam a compor em background | melhoria | Baixa | app `shell\components\TopBar.kt` L118; `LiveEpgCard.kt` L66 | `LaunchedEffect(Unit) { while (true) { delay(15_000); now = ... } }` (e 20 s no LiveEpgCard) corre após onStop; contraste com a política do próprio app (`PlayerClock.kt` L57-64: sem timer em fundo) | ⚠️ | Replicar o padrão `repeatOnLifecycle(STARTED)` nos dois relógios. |
| R5-B3 | Estados de engine por tile colectados sem ciclo de vida | melhoria | Baixa | app `multiview\MultiviewScreen.kt` L219-220 | `engine.state.collectAsState()` / `engine.error.collectAsState()` por tile (única ocorrência de `collectAsState(` não-lifecycle no app) | ⚠️ | Trocar por `collectAsStateWithLifecycle()` (padrão do resto do shell). |
| R5-B4 | Flows de fundo activos durante toda a sessão sem subscritor (Eagerly) | melhoria | Baixa | app `epg\EpgViewModel.kt` L320/324/327/359/365; `RecordingsViewModel.kt` L62-67 | 5+ `stateIn(..., SharingStarted.Eagerly, ...)` mantêm colectores Room vivos no VM da Activity; o storage poll corre `while(true) delay(2_000)` → `sample(1s)` → `storageInfo()` | ⚠️ 🔗 §8.2-4 | Trocar para `WhileSubscribed(5_000)` (igual às restantes VMs, `EpgViewModel.kt` L160/402); no storage, re-expor só ao mudar `rows`/`downloadRoot` + tick lento. |
| R5-B5 | Poll ~31 Hz no long-press (já coberto) | nota 🔁 | Baixa | app `ui\components\ChannelPressInput.kt` L81-94 | idêntico ao Baixa 4 §1 (mesmo ficheiro/linhas, `delay(32)` no hold) | 🔁 Baixa 4 | ver §1 Baixa 4 (`Animatable`/`withInfiniteAnimationFrameNanos`); extra útil: emitir só nas mudanças de fase (`stageForElapsed`) + `animateFloatAsState`. |
| R5-B6 | Leitura síncrona de SharedPreferences no arranque (já coberto) | nota 🔁 | Baixa | app `OwnTVApp.kt` L63; `MainActivity.kt` L136; core `i18n\LocaleStore.kt` L55-62 | repetição de §8.2-5 (`readBlocking` 2× no arranque; já com solução em pé: cache estático a nível de processo) | 🔁 §8.2-5 | ver §8.2-5 (cache `AtomicReference` + validade); actuar só se o profiling de arranque justificar. |
| R5-B7 | `EpgLogoStore.icons` (singleton) conduzido por um scope de composição | melhoria | Baixa | app `shell\OwnTVShell.kt` L336-339; core `epg\EpgLogoStore.kt` L32-46 | `logoScope = rememberCoroutineScope()` + `LaunchedEffect(Unit){ EpgLogoStore.start(logoScope, ...) }` — a colecta morre com a composição; ⚠️ matiz: `start()` não é idempotente por si (relança o colector sem guard) | ⚠️ | Iniciar **1×** em `appScope` (Koin singleton) e adicionar guard `started` em `EpgLogoStore.start` para dificultar dupla colecta. |
| R5-C1 | Feed não-M3U (HTML de erro/CDN/binário) → sync "bem-sucedido" com 0 itens e `lastSyncAt` carimbado | bug | Alta | core `parser\M3uParser.kt` L85-213; `sync\M3uSyncer.kt` L471-526; `sync\SyncManager.kt` L153-156 | linhas de HTML caem no ramo else e são descartadas; `entries=0` termina sem erro; prune é saltado (`processed > 0`, M3uSyncer L491) e `markSynced()` corre por cobertura de secções, sem verificar contagens | ✅ | No `M3uParser`, exigir bytes iniciais com `#EXTM3U`/`#EXTINF` (sniff 4-8 KB); em `M3uSyncer`, falhar o sync quando total processado = 0 → UI via `SourceImporter.SetupFailure.Sync`. |
| R5-C2 | XMLTV truncado a meio não sinaliza truncamento → cache truncado promovido e canais pós-corte podados | bug | Média/Alta | core `parser\XmltvParser.kt` L89-94 (default `requireCompleteDocument=false` L34); `repository\EpgRepository.kt` L355-356, L375, L409-413, L419-421, L575-596 | falha de EOF/IO no `parser.next()` é capturada com `break` **sem propagar** → `parseCompleted=true` promove o tmpFile (L409) e `pruneRemovedChannels` (L575-596) apaga canais como "removidos pelo fornecedor" (a intenção "promote only on clean parse" está comentada em L265-267, mas fica derrotada pelo swallow) | ✅ | Manter o parse tolerante por item, mas EOF/IOException → `reachedEnd=false`; no repo nunca promover o tmp nem podar canais quando `!reachedEnd`. |
| R5-C3 | `parseAttrs` só reconhece `key="value"`: atributos sem aspas/single-quote são perdidos em silêncio | melhoria | Baixa | core `parser\M3uParser.kt` L243-258 | `indexOf("=\"")` + `emptyMap()` se ausente — `tvg-id=1` ou `group-title='News'` → zero atributos → canais sem tvg-id/grupo/logo | ✅ | Aceitar `key='value'` e `key=value` unquoted (até ao espaço) como fallback. |
| R5-C4 | `#EXTHTTP:` malformado perde todos os headers do canal, sem log | melhoria | Baixa | core `parser\M3uParser.kt` L321-330 | `runCatching` → `getOrNull()` sem `onFailure` — um JSON com vírgula em falta faz cair o bloco todo; o canal falha depois com 403 sem explicar os headers descartados | ✅ | `.onFailure { log "EXTHTTP inválido à linha N — headers descartados" }` + preservar as props restantes da linha. |
| R5-C5 | `isXtreamAccount` indistinguível: timeout/5xx transiente → fonte Xtream fixada para sempre como tipo M3U | bug | Média | core `parser\XtreamClient.kt` L500-534; `setup\SourceImporter.kt` L228-248 | probe de 8 s com `withTimeoutOrNull ... ?: false` e catch-all `catch (_: Exception) { false }` sem log (L532-533); a decisão de tipo é tomada **uma vez** no import (L235-247) → um instante de rede quebrada perder para sempre serverTimeZone/max_connections/HLS-test | ✅ (era ⚠️/❓ — mecanismo confirmado) | Reclassificar só quando payload válido sem user_info ("privado, não-Xtream"); em timeout/network, re-probe 1× + guardar `suspectedXtream` para reclassificação na próxima sync. |
| R5-C6 | Campo cifrado com chave errada some silenciosamente (decrypt → null) | bug | Média/Baixa | core `backup\BackupManager.kt` L809 | mecanismo per-line `getOrNull` confirmado; nenhum contador/aviso dessa degradação no restauro | ❓ | Contar campos nulos durante a descompressão e devolver "N campos ignorados (chave errada?)" no resultado do restauro. |
| R5-C7 | `lastSyncStats` (ConcurrentHashMap) nunca evictado — stats de fontes apagadas ficam para sempre | melhoria | Baixa | core `sync\SyncManager.kt` L79-81, L183 | `lastSyncStats[source.id] = runStats` sem evict em lado algum; `deleteSource` não toca no mapa | ✅ 🔗 §10.2-3 | Evictar no `deleteSource` (mesma transacção do R4-G5) ou LRU tamanho N; a persistência (tabela `sync_stats`) continua a ser a solução de §10.2-3. |
| R5-C8 | `refreshAllEpgFromNetwork` engole feeds rejeitados sem contador visível (já coberto) | nota 🔗 | Baixa | app `EpgViewModel.kt` L823-836 | feed inválido aparece como "completa"; a contabilidade de feeds rejeitados é a solução da R2-13 | 🔗 R2-13 | ver R2-13 (contabilidade em `EpgSource.lastError`); acrescentar o contador de rejeitados no diagnóstico (Feed Doctor) quando > 0. |
| R5-C9 | Charset M3U: só o BOM UTF-8 é cortado; UTF-16/1251/1256 → mojibake (já coberto) | nota 🔗 | Média | core `parser\M3uParser.kt` L85-92 | já em pé como M7 (§1); nota adicional: um feed UTF-16/mojibake também cai no R5-C1 (sucesso com 0 itens) — a mesma heurística resolve ambos | 🔗 M7 | ver M7 (sniff BOM/1251/1256/ISO de 1-2 KB + override por fonte) + erro distinto "não parece texto M3U". |
| R5-D1 | androidTest nunca corre em CI: instrumentação sem job | melhoria | Média | `.github\workflows\android.yml` L51-59 (job `check`) | CI corre só unit+lint; `app\src\androidTest` (3 .kt) e Core androidTest (16 .kt = 15 testes + 1 helper `TestDatabase.kt`) — incl. `OwnTVDatabaseMigrationTest` (migrações 1→51) — não têm job com emulador/GMD | ✅ | Job de CI com Gradle Managed Device (`connectedCheck`, API TV) ou migrações via Room migration-testing/Robolectric. |
| R5-D2 | Testes JVM sobre android.jar silenciosamente stubado | melhoria | Média | `app\build.gradle.kts` L151-155, L429-432 | `isReturnDefaultValues=true` + `testImplementation(libs.org.json)` como remendo do stub; chamadas a `android.*` devolvem null/0 sem falhar | ✅ | Migrar os testes que tocam `android.*` para Robolectric/injecção de clock+logger; tirar `returnDefaultValues`; cobertura (Kover). |
| R5-D3 | versionCode dev 99999: o contrato "instala sobre qualquer release" já se pode quebrar | bug | Média/Baixa | `.github\workflows\android.yml` L116-124 | dev usa `CODE=99999` (comentário L117-119), mas release v10.0.0+ → `MA*10000+...` > 99999 → `INSTALL_FAILED_VERSION_DOWNGRADE` (latente no esquema 4.x actual) | ✅ 🔗 §3 | Derivar o dev code acima da escala mais alta do esquema com a formulação única do §3; transição imediata: `CODE=$((MAX+1))`. |
| R5-D4 | Splits ABI duplicam o que flavors já fazem | melhoria | Baixa | `app\build.gradle.kts` L110-117 vs flavors L98-108 | flavor `standard` (abiFilters) × splits `include`+universal (`isUniversalApk=true`) que o CI nunca usa (publica `app-standard-universal-release.apk`, L185-187); no flavor x86_64 os splits não produzem nada | ✅ 🔗 Baixa 1 | Continuar a Baixa 1 (`isUniversalApk=false` + universal só em job manual) e remover o bloco `splits` (ou coerir include↔filters num só mecanismo). |
| R5-D5 | Keep de enums largo em excesso (app + Core) | melhoria | Baixa | `app\proguard-rules.pro` L20-24 | `-keep enum tv.own.owntv.** { *; }` alcança todos os enums do app **e do Core** (mesmo namespace); para roundtrip bastam names/values/valueOf | ✅ | `-keepclassmembers enum tv.own.owntv.** { values(); valueOf(...); }` + `-keepnames`, com regressão on-device de sync/backup. |
| R5-D6 | Alias morto no version catalog + comentários desalinhados | melhoria | Baixa | `gradle\libs.versions.toml` L104/L16; nota L5-7 e L32-33 | `androidx-navigation-compose` // `navigationCompose` sem qualquer uso (grep zero); comentários "9.4.0"→agp 9.4.1 e "1.11.0"→media3 1.11.1 | ✅ | Remover o alias (ou ratchet de CI para aliases sem usos) + actualizar os comentários de versão de passagem. |
| R5-D7 | libmpv vem de GitHub Pages de conta 3.ª, fora do lock e sem checksum | melhoria (supply-chain) | Baixa (risco) | `settings.gradle.kts` L23-31; `app\build.gradle.kts` L7/L350; git remote | `exclusiveContent` resolve `tv.own.owntv:libmpv` de `https://ahxn00.github.io/OwnTV_Core/maven` — dono do artefacto ≠ repo anunciado do Core (`renatofc27/OwnTV_Core`) e ≠ origin (`renafcampos27/Owntv`); fora do `core-source-lock.json`/SHA-256 | ✅ 🔗 H5 | Publicar libmpv no repositório oficial (maven-publish + SHA fixado) e validar checksum no `resolve-core` — fecha também o H5 (§2). |
| R5-D8 | CI pede `contents: write` a todos os jobs e ações sem pin por SHA | melhoria | Baixa | `.github\workflows\android.yml` L13-14 | `permissions: contents: write` no topo (todos os jobs); actions por tag `@v4`; sem dependabot/CodeQL | ✅ | `permissions` por job (`check` = read) + pin por SHA; `dependabot.yml` para GHA. |

Notas de cruzamento da tabela:

- 🔁 **R5-B5 = Baixa 4 (§1)** — mesma mecanica, mesmo ficheiro/linhas; a solução Animatable já está em pé; a novidade submetida (emitir por fases + `animateFloatAsState`) entra como refine do mesmo fix.
- 🔁 **R5-B6 = §8.2-5** — mesmos sites (`OwnTVApp` L63 / `MainActivity` L136 / `LocaleStore`), mesma solução (cache process-level); reabrir apenas se o profiling justificar.
- 🔗 **R5-B1 ↔ R4-P3** — mesma família de correção (`setForeground` guard) em ficheiros/gatilhos distintos (DOWNLOAD vs RECORDING); corrigir em bloco.
- 🔗 **R5-C9 ↔ M7** — mesmo parser e mesma heurística de sniff; considerar o C9 "resolvido pelo M7" e usar o mesmo erro mesclado no C1.
- 🔗 **R5-C8 ↔ R2-13** — a contabilidade de feeds rejeitados vive no fix da R2-13; aqui apenas se expõe no contador do refrescamento global.
- 🔗 **R5-C7 ↔ §10.2-3** — a mesma mapa `lastSyncStats` tem duas correções complementares: evict (novo) e persistência (`sync_stats`, §10.2-3).
- 🔗 **R5-B4 ↔ §8.2-4** — a mesma solução (`WhileSubscribed`) aplicada a novos sites; fix em lote.
- 🔗 **R5-D4 ↔ Baixa 1** — o mesmo sintoma do universal; o novo ângulo é a coexistência incoerente flavors×splits.
- **R5-C6 vs R5-A4** — ambos no caminho de import do backup, mas distintos (versão do ficheiro vs campos silenciosos no decrypt) — mantidos separados.

### 14.2) O que está bem — reconfirmações da Ronda 5 (por domínio)

**Persistência & dados (A)**
- Migrações Room 1→51 completas em `ALL_MIGRATIONS` e usadas no builder, **sem** fallback destrutivo (`DatabaseModule.kt` L16-19/37; `exportSchema=true` L121).
- Auto-backup Android fechado em duas camadas — `allowBackup="false"` + exclusão total em `backup_rules.xml` e `data_extraction_rules.xml` (inclui `device-transfer`).
- Disco com orçamento por volume (gravações+downloads), reconciliação de órfãos de gravações (`RecordingEngine.kt` L981-998) e sweep de caches temporais EPG; rotação de uploads do companion (5 mais recentes).
- Import de backup defesa-a-defesa: probe por magic + limites/zip-bomb/path-traversal (`BackupContainer.kt`), validação de schema pré-escrita, fase DB numa transacção com marcador de restauro interrompido e ficheiros escritos **após** o commit.
- Backup auto da app com staging+rename atómico e segredo cifrado Keystore-GCM (`AutomaticBackupPreferences.kt` L94-107).

**Concorrência & ciclo de vida (B)**
- Zero `GlobalScope`/`runBlocking` em produção (só testes); scopes com `SupervisorJob` e `scope.cancel()` correcto (`AutoStartService.kt` L105).
- `ViewModel(scope)` custom com o ctor oficial lifecycle 2.8+ e fallback Unconfined apenas quando não há Looper (testes).
- Pipelines exemplares: coalescência de reloads (`HomeViewModel` L149-160), trabalho do painel dentro de `repeatOnLifecycle(STARTED)` (`LiveScreen` L242-272).
- `AutomaticBackupWorker` exemplar: Mutex, `ensureActive`, cleanup `NonCancellable`, backoff exponencial 1 h + constraints storage/battery, retry < 3.
- Retoma de downloads após morte pela row RUNNING (`DownloadEngine.kt` L125-128); todo o disco da UI em `withContext(Dispatchers.IO)` (SettingsScreen).

**Robustez & higiene (C)**
- Higiene `CancellationException` exemplar: ~30 sites com `catch (cancelled) { throw }` antes do `catch (Exception)`; `MiniGuideOverlay` re-verifica `ensureActive()`.
- Logs sem segredos: zero `println`/`printStackTrace` em produção; URLs via `HttpClient.safeUrl`/`redactUrl`; zero `Log.` com password/token/mac.
- Export de diagnóstico blindado: `PlaybackErrorLog.export` re-redige o texto inteiro na saída; `CrashRecorder` nunca quebra o handler original.
- Parsers numéricos com `Locale.US/ROOT`; divisões guardadas com `coerceAtLeast(1)`; Companion server capado (`maxBodyBytes` por endpoint + 413 sem confiar em Content-Length).

**Build/devops/testes (D)**
- Release optimizado (`minify`+`shrinkResources`+`proguard-android-optimize`), `localeFilters` com pseudolocais só em debug; proguard enxuto com keeps raciocinados (JNI mpv, WorkManager, Room, enums, `assumenosideeffects` de Log).
- Catálogo único actualizado (AGP 9.4.1/Kotlin 2.4.20/Compose BOM 2026.08/Media3 1.11.1/OkHttp 5.5/Koin BOM), um só loader de imagens; wrapper fixo + `gradle-daemon-jvm` 21 = JDK do CI, sem drift.
- CI maduro: pytest core → unit nos 3 módulos → lint `abortOnError`; i18n.yml com prova de pseudolocais no APK; changelog-sync/release-notes/pr-checklist.
- Supply-chain do Core forte: `core-source-lock.json` + SHA-256 + anti-zip-slip verificação byte-a-byte; paridade `locales.json` imposta no CI.
- Testes numerosos: 76 ficheiros/`app\src\test`, 162 no Core, 80 no player-core; guardas tipo `SettingsTestOwnershipTest`; `.gitignore` cobre keystores/logcat e o CHANGELOG alimenta release notes automática.

### 14.3) Ideias de inovação — lote 5 (18, tabela completa)

43 candidatas submetidas (25 do analista E + IA/IB/IC/ID de A-D); após dedupe contra §5/§8.3/§10.3/§12.3 e contra os fixes desta ronda, ficam **18**. Ganchos do analista (verificados por leitura/greps na submissão); fontes = ID original.

| # | Título | Pitch | Gancho (ficheiro:linha) | Valor | Facilidade | Fonte |
|---|---|---|---|---|---|---|
| 1 | Fusão sugerida de categorias entre fontes | Mesmas categorias com nomes iguais em 2+ playlists → chip «fundir N iguais» criando categoria combinada auto-mantida | `features\customize\CustomizeViewModel.kt` L142-174 | Alta | Média | R5-IE1 |
| 2 | Selo de catch-up provado na grelha | Canal/programa com arquivo já testado ganha badge discreto «há catch-up» (evidência de sessão, sem rede) | `features\epg\ReplayEvidenceStore.kt` L18-30 | Média | S | R5-IE2 |
| 3 | Espinho retrovisor | Saltar para trás na grelha dispara «pedir histórico em falta» por janela, em vez de buracos pretos | core `live\GuideHistoryCoverage.kt` L5-9 | Média | S/M | R5-IE3 |
| 4 | Vela Kids (janela horária com PIN) | Perfil Kids bloqueável fora do intervalo definido (ex. 07-21 h), com PIN dos pais para ultrapassar; mecanismo distinto do painel de uso (§5-#14) e da pausa por inactividade (C10) | `features\profiles\ProfilesViewModel.kt` L63-70 | Alta | Média | R5-IE4 |
| 5 | Selo 18+ fora de Kids | Categorias adultas fora do perfil infantil mostram tarja/blur com nome, em vez de ficarem invisíveis acidentalmente 🔗 R2-26 | core `content\AdultCategoryClassifier.kt` L24-30 | Média | S | R5-IE5 |
| 6 | Memória opt-in de rotas de áudio | A lição «esta saída não suporta multicanal» persiste por saída (TTL + «tentar de novo») | core player `AudioOutputPolicy.kt` L24-27 + `OwnTVPlayer.kt` L733-750 | Alta | Média | R5-IE9 |
| 7 | Capacidade medida por fonte | Cartão «aguenta N em simultâneo (medido em X)» na ficha da fonte, com re-medir a pedido e delta histórico 🔗 R4-G4 | core `live\ConnectionProbe.kt` L23-48 | Alta | S/M | R5-IE11 |
| 8 | Fila dormida de descargas | Downloads agendados para janela nocturna (02-06 h) num único Worker noturno; manhã chega com episódios prontos | core `download\DownloadManager.kt` L44-55 | Média | S | R5-IE12 |
| 9 | Moldes de perfil | «Duplicar perfil» com escolha selectiva (cores/altura de guia/pré-sets) para instanciar Família/Kids/Convidado | `features\profiles\ProfilesViewModel.kt` L63-70 (grep: duplicar = 0) | Média | S | R5-IE13 |
| 10 | Ver Mais Tarde | Fila pessoal de VOD/episódios «marcar para depois», sem depender de favoritos; viaja no backup | core `database\dao\HistoryDao.kt` L16-33 | Média | S/M | R5-IE14 |
| 11 | Agenda .ics para o calendário | Exportar a agenda de lembretes como .ics para o disco partilhado (SAF) — distinta da agenda in-app de §10.3-5 | `features\epg\ProgrammeReminderStore.kt` L33-40 + `ui\components\StorageBrowser.kt` L33-35 | Média | S | R5-IE17 |
| 12 | Corretor de fuso assistido | «Feed parece 3 h à frente — aplicar −180?» a partir do desfase observado, sugerindo o EpgShift com um toque | core `epg\EpgShift.kt` L23-33 + `live\EpgNowNext.kt` L8-16 | Média | S/M | R5-IE21 |
| 13 | Varrido de vazios | «Canais sem guia (N dias)» no ecrã de customização com esconder em bulk reversível (distinto do guia por género §10.3-7) | `features\customize\CustomizeViewModel.kt` L163-174 + core `live\EpgNowNext.kt` L13-15 | Média | S | R5-IE22 |
| 14 | Higiene de nomes com o normalizador VOD | Preset de rename em massa «remover EN, 4K, (MULTI-SUB)…» reutilizando o normalizador das fichas, com pré-visualização | core `metadata\TitleNormalizer.kt` L4-36 + `features\customize\BulkRenameDialogs.kt` L340 | Média | S/M | R5-IE23 |
| 15 | Provar antes de Sincronizar | No import, amostrar 64 KB e classificar M3U/XMLTV/JSON/HTML/CF-error-page com pré-visualização antes de gravar — converte o bug nº1 de suporte (R5-C1) numa resposta de 1 segundo | parsers `M3uParser`/`XmltvParser` + UI de import | Média/Alta | S/M | RA5-C IC2 |
| 16 | Regra de CI «Cancellation-safe» | Detekt/ratchet que exige `catch (CancellationException) throw` antes de qualquer `catch (Exception)` em funções suspend — codifica o padrão já repetido ~30× no código (decisão do conselho: manter como ideia) | padrão de `EpgScreen.kt` L920-923 e afins | Média | S | RA5-C IC3 |
| 17 | Zero-Shrink Sentinel | No fim do sync, comparar contagens live/movies/series com o sync anterior; queda >30% → perguntar + rollback do prune — fecha a família R5-C1/C2 no ponto onde o utilizador sente | fim de `M3uSyncer.sync` (stats existentes) | Média | S/M | RA5-C IC4 |
| 18 | Backup por perfil | Exportar/restaurar **um** perfil completo (avatar, home config, customizations, start-up) num ficheiro isolado com re-map de ids; export profile-scoped já suportado | core `backup\BackupManager.kt` L93-117 ("v11: profile-scoped export") | Média | S/M | RA5-A IA3 |

**TOP 5 lote 5**: IE1 (fusão de categorias — dor nº1 multi-fonte), IC2 (provar antes de sincronizar), IE4 (Vela Kids), IE9 (memória de rotas de áudio), IE11 (capacidade medida por fonte).

**Cruzamentos e notáveis eliminados** (25 de 43; 1 linha cada):

- R5-IB2 — é o mecanismo do fix R5-B4 (WhileSubscribed); sai como ideia.
- R5-ID2 — mecanismo ampliado do fix R5-D1 (smoke em real device); agrupa-se no plano do fix (GMD/Robolectric).
- R5-ID4 — é a própria solução do fix R5-D7 (publicar libmpv com checksum no repo oficial).
- R5-IC1 — complemento directo de §10.3-17 (Doutor de Fontes): as `ParseMetrics` entram como coluna do cartão por fonte já idealizado.
- R5-IC5 — higiene de CI pequena (testes de redacção do toolkit); fica como nota no plano de testes (P3 §15.2).
- R5-IA1 — formato de backup delta assinado = custo L e mudança de formato; deferido (backups diários já rotativos).
- R5-IA2 — é a extensão natural do fix R5-A2 (ledger `MediaStorageBudget` a todas as caches); absorvida na solução.
- R5-IA4 — verificação fina de índices/FTS no diagnóstico; cobertura já garantida por `healSchema`; deferido.
- R5-IA5 — «espelho de perfil» deriva do fix R5-A3 (migração por-perfil); parte do plano do fix.
- R5-IB1 — watchdog de colectores fantasma: ferramenta de devs sem utilizador de suporte próprio; deferido.
- R5-IB3 — TVClock unificado: refactor mais amplo do que o fix B2; refazer quando os relógios já estiverem lifecycle-aware.
- R5-IB4 — painel de Operações WorkManager: sobreposição parcial com §10.3-17 (estado por fonte); deferido.
- R5-ID1 — rollout estável/beta/canary: decisão de distribuição do dono (tag/DataStore), não é funcionalidade in-app; deferido.
- R5-ID3 — ratchet de R8/keeps/tamanho por PR: combinável com ktlint/detekt (§3); deferido.
- R5-IE6 — Perfil Visual reactivo: automático mas difícil de calibrar (limiar por jank); deferido.
- R5-IE7 — AFR com exceções por canal: caso raro (handshakes HDMI); colocável após o AFR automático.
- R5-IE8 — Rádio de sala (arte+relógio no modo áudio): cosmético; deferido.
- R5-IE10 — «Quem gasta as sessões»: o sintoma é tratado por R4-G1/G3/G4; a UI de claims é extensão natural do fix, não ideia.
- R5-IE15 — Comando de consola (gamepad ACS): público estreito; D-pad e atalhos já cobrem.
- R5-IE16 — Ritmo da casa (hora habitual → lembretes): valor baixo.
- R5-IE18 — Diário do Companion (log LAN + fechar servidor): caso de confiança doméstica, mas âmbito estreito; deferido.
- R5-IE19 — Bateria como gate transversal: TV box típica está sempre ligada/sem bateria; para telemóvel fica marginal.
- R5-IE20 — Pill de rede (vpn/metered): cosmético; `ConnectivityObserver` já classifica transportes.
- R5-IE24 — Cartaz via `screenshot_uri` (Stalker): cobertura/fallback baixa; deferido.
- R5-IE25 — Modo Sereno (fundo fechado): casos raros; deferido.

## 15) Reavaliação final consolidada — versão 3 (Rondas 1-5)

Versão final do inventário; §13 mantém-se como registo histórico da versão 2 (Rondas 1-4) e §11 da versão 1. O lote 5 entra por §14; a rastreabilidade da verificação da R5 está no Apêndice I.

### 15.1) Spot-verification R5 — veredictos finais

| Item | Veredicto final | Evidência (1 linha) |
|---|---|---|
| R5-C1 — sync "sucesso" com 0 itens | ✅ confirmado | `M3uParser` ramo else descarta linhas não-M3U (L138-186; entry só com `pending != null` L152); prune guard `processed > 0` (M3uSyncer L491); `markSynced` por cobertura de secções (SyncManager L153-156) |
| R5-C2 — XMLTV truncado + promoção/purga | ✅ confirmado | `XmltvParser` L89-92 `break` sem propagar (default `requireCompleteDocument=false`, L34) → `EpgRepository` L375/L409-413 promove tmpFile e L419-421/L575-596 poda |
| R5-B1 — `DownloadWorker` foreground sem catch | ✅ confirmado | L30 `setForeground` nu dentro de `try/finally`; contraste exacto L38-40 (`runCatching` + `setForegroundAsync`) |
| R5-C5 — `isXtreamAccount` catch-all | ✅ (upgrade de ⚠️/❓) | `XtreamClient` L511-534 (timeout 8 s `?: false`; `catch (_: Exception) { false }` sem log L532-533) → `SourceImporter` L235-247 grava tipo M3U permanente |
| R5-A1 — loop `setEpgMatch` | ✅ confirmado | `EpgViewModel` L887/L956; `CustomizationStore` L244-248 (1 `edit` por chamada via `update` L182-183); padrão single-edit L269-273 |
| R5-A2 — cache de legendas sem poda | ✅ confirmado | `SubtitleRepository` L36 (filesDir/subtitles); remoção só manual L230-259; `MediaStorageBudget` L54-58 só gravações+downloads |
| R5-A3 — prefs globais de sort/view-mode | ✅ confirmado | `SettingsRepository` L1080-1082/L1125-1130/L1164-1169/L1279-1285/L402+L1394-1430 globais vs por-perfil L634-656 |
| R5-D1 — androidTest sem job em CI | ✅ confirmado | `android.yml` só `testDebugUnitTest`+lint (L51-59); zero `connectedCheck`/GMD; 3 .kt app + 16 .kt Core (15 testes + 1 helper `TestDatabase.kt`) |
| R5-D3 — dev versionCode 99999 | ✅ confirmado (latente ≥ v10.0.0) | `android.yml` L116-122 (NAME=99.99.99/CODE=99999); v105.1.0 → 1.050.100 > 99.999 → downgrade fail |
| R5-D7 — maven libmpv em github.io | ✅ confirmado (🔗 H5) | `settings.gradle.kts` L23-31 (`ahxn00.github.io/OwnTV_Core/maven`); `build.gradle.kts` L7/L350 refere `renatofc27/OwnTV_Core`; remote ≠ ambos |
| R5-C3 — `parseAttrs` aspas só | ✅ confirmado (leitura) | `indexOf("=\"")` L244-247 + `emptyMap()` L245 |
| R5-C4 — `#EXTHTTP:` sem log | ✅ confirmado (leitura) | `runCatching` → `getOrNull()` sem `onFailure`, L321-330 |
| R5-C7 — `lastSyncStats` sem evict (opcional) | ✅ confirmado | `SyncManager` L79 (map) + L183 (put) sem evict em lado algum |
| R5-B7 — `EpgLogoStore.start` em scope de composição (opcional) | ⚠️ matiz | `OwnTVShell` L336-339 confirmado; `start()` (L32-46) relança o colector sem guard — "idempotente" impreciso |
| R5-D2 — `returnDefaultValues` (opcional) | ✅ confirmado | `build.gradle.kts` L151-155 (`isReturnDefaultValues=true`) + L429-432 (stub `org.json` remendado) |

Os restantes itens pequenos da R5 (A4/A5, B2-B4, C6, D4-D6, D8) mantêm o Marca do analista; B5/B6/C8/C9 rebaptizados como notas de dedupe (§14.1/§15.4).

### 15.2) Checklist final de execução — tudo o que ainda está válido, TODAS as rondas (Rondas 1-5)

Cada linha = item válido + solução inline. Agregações permitidas; onde o fix desta ronda fecha uma família antiga, está indicado.

**P1 — crítico (fazer primeiro; perda de dados, segurança, suporte nº1, i18n/a11y obrigatório)**
1. Perfis/kids: R3-1/R3-2/R3-3 — credencial de adulto no `onAddProfile` e nas edições sensíveis (`emptySet()` = negado por omissão) + re-gate no restauro de `isKids`; R2-24 — `MessageDigest.isEqual` + rate-limit no PIN.
2. Downloads perdem dados: R1-B4 (truncado COMPLETED → `done==total` + PARTIAL + digest) + R3-31/32 (`S{xx}E{yy}_{name}` + desambiguação; mensagem vFAT honesta) + R3-34/R4-R3 (UA/`httpReferer`/StreamHeaders da fonte no request).
3. Sync com sucesso falso: R5-C1 (sniff `#EXTM3U` + falhar com 0 itens) + R5-C2 (EOF → `reachedEnd=false` sem promoção/poda) + R5-C5 (re-probe antes de fixar tipo Xtream/M3U) + R2-13 (XmltvTime "+02" → aceitar HH + contabilidade de feed rejeitado).
4. Restauração blindada: R3-7 (caps por secção no importador) + R3-8 (merge sem fontes → match por nome/avatar) + R5-A4 (guard de `version` do backup) + R5-C6 (contar campos decrypt→null) + pós-restauro enfileirar sync imediato (§10.2-4/R3-4).
5. Lembretes de EPG: R2-12 (persistir + AlarmManager/WorkManager + boot receiver; ou renomear UX "da sessão") + R3-41 (ponte lembrete → agendar gravação).
6. Box 24/7: R3-9 — WorkManager periódico com thresholds para `HOURS_6/HOURS_12/MANUAL` (auto-refresh nunca acontece só no `onStart`).
7. Gravações reencontradas: R3-33 (`MISSED` com janela aberta → re-agendar no arranque) + R4-G2 (recusa → `PENDING` até `stopMs`, não MISSED imediato).
8. Orçamento de conexões: R4-G1 (fullscreen regista CLAIM no registry) + R4-G3 (tile ERROR termina → liberta claim) + R4-G4 (458 → `markNeedsProbe` + bandeira temporária).
9. Demolição de dados ao apagar: R3-10 (FK CASCADE ou `purgeOrphans` na transacção) + R4-G5 (deleteSource → stop/cancel gravações da fonte) + purge prefs `*_<pid>` (§10.2-2).
10. Companion em LAN: R2-21 — PIN fora da query (header+nonce via QR), sessão selada com HKDF/PSK, ACL RFC1918 no bind.
11. Workers de 1.º plano: R5-B1 + R4-P3 — o 1.º `setForeground` em `runCatching` + `Result.retry()` (mesma família, dois ficheiros).
12. i18n pipeline integral: R4-I1 (glob `*.xml`: −155 chaves no core) + R4-I2 (app no pipeline: pt −96 chaves/8 ficheiros; ~22 línguas caem no EN) + R4-I3 (seed pt −30).
13. A11y de base: R4-I5 (semântica `contentDescription` na grelha Canvas) + R4-I6 (`Role.Button` no `FocusableSurface` + liveRegion nos estados) + R4-I7 (espelho RTL dos ícones direccionais).
14. Hardening rede/ficheiros: R2-25 (`networkSecurityConfig` allowlist) + R2-23 (XXE secure-processing/XmlPullParser) + R2-27 (file_paths sem raiz `.`) + R4-R7 (proxy selado Keystore) + R2-26 (matcher de títulos kids opt-in).
15. Permissões/update: R4-P1 (`READ_EPG_DATA` no manifest) + R4-P2 (`POST_NOTIFICATIONS` pedida em runtime) + R4-P5 (pre-check `canRequestPackageInstalls`) + R2-22 (digest SHA-256 do APK antes de instalar).
16. TMDB/trending: R3-18 (não cachear corpo 200 inesperado como negativo) + R3-21/23 (`tryConsume` só após parse; re-agenda curta com cap).

**P2 — importante (estabilidade, performance e UX do dia-a-dia; compacto)**
1. Player data-path: R2-1 honrar `percent` (+ decisão VolumeBoost §8.2-13) · R2-2 `isMuted`→ícone · R2-3 `pendingExoAudio` no toggle engine · R2-4 `MPV_FORMAT_DOUBLE` · R2-5 guard `released`+try/catch no `execute` · R2-6 mimes/charset das legendas.
2. Catch-up/timeshift: R2-8 (trim só live) · R2-9 (DVR window ou esconder barra) · R2-10 (tolerância VOD dur/10) · R2-11 (limpar `pendingFps`) · R2-14 (IANA-first + override + gap-detect) · R2-15 ❓ golden DST · R2-16 (`coerceAtMost`) · R2-17 (`runCatching delete` + sweep; 🔗 R4-G6 `tryClaim` antes do resolve) · R2-18 (503+Retry-After) · R2-19 (rejeitar segmento grande) · R2-20 (map strftime).
3. Rede: R4-R1 (`BoundedDns` no singleton + A/AAAA paralelos) · R4-R2 (`callTimeout` no cliente) · R4-R4 (Retry-After no `get`) · R4-R5 (toggle metered por secção nas constraints) · R4-R6 (decode do `+` só com `%`) · R4-R8 ❓ (limpar headers em redirect cross-host).
4. Compose/perf: R1-B1 (colectores por row + catálogo memoizado + 8 `RootGroup` mortos) · R1-B3 + §8.2-3 (indicador em composable folha + 1 AsyncImage no hero) · M1 (limpar intent no consumo) · M2 (`rememberUpdatedState`) · M3 (`CompletableDeferred` na escada) · M4 (colectores fixados) · Baixa 3 (`Map<Long,Int>` overlay) · Baixa 4/R5-B5 (Animatable) · Baixa 5/R4-R6 (canonical antes do split `&`).
5. Ciclo de vida R5: B2 (relógios em `repeatOnLifecycle`) · B3 (`collectAsStateWithLifecycle` no multiview) · B4 (`WhileSubscribed` nos 5 flows + poll de disco; 🔗 §8.2-4) · B7 (`EpgLogoStore` iniciado 1× em `appScope` com guard) · B6 = §8.2-5 (cache `LocaleStore` já planejada; actuar se profiling pedir).
6. Persistência R5: A1 (`setEpgMatches` single-edit) · A2 (LRU+teto das legendas) · A3 (chaves de apresentação `_<pid>` com fallback) · C3 (`parseAttrs` unquoted/aspas simples) · C4 (`EXTHTTP` com `onFailure`+log) · C7 (evict em `deleteSource`; persistência é §10.2-3) · A5 (`getByIds` no planner).
7. Estados/UX: R3-6 (1.ª escrita de `playing` após o gate) · R3-24 (`coerceIn` no hero) · R3-25 (usar o branch para hint `==1`) · R3-26 (requesters chaveados por tiles + refocus) · R4-P4 (`SavedStateHandle` secção/multiview) · R4-I4 (12/24 h + locale da app) · R4-I8/I9 (glass floor INLINE; clamp `top`/rowheight) · R4-G7 (`ENDED/CLOSED` → `disposeLocalTimeshift`) · R3-37 (recalcular `nextAt` no init; 🔗 H4-NTP).
8. Metadata: R3-21 (`tryConsume` no lugar certo) · R3-29/30 (cache de detalhes por id local) · R3-16 🔁 (upsertStable já protege — nada a fazer).
9. DevOps/build: R5-D1 (androidTest em CI — GMD `connectedCheck` ou migrações Robolectric; absorve ID2) · R5-D2 (Robolectric+Kover, tirar `returnDefaultValues` gradualmente) · R5-D4 (🔗 Baixa 1: `isUniversalApk=false` + universal manual; remover/coerir `splits`) · R5-D5 (`keepclassmembers` de enums + regressão backup) · R5-D6 (alias morto + comentários toml desalinhados) · R5-D7 (libmpv no repo oficial + checksum no resolve-core — fecha H5) · R5-D8 (permissions por job + pin SHA + dependabot).
10. Hipóteses §2: H1 (detectar reset de media-sequence < anterior) · H2 (`SupervisorJob` no scope da `PlaybackSession`) · H3 (flush boundary + SxxEyy) · H4 🔗 R3-37 (NTP check) · H5 ✅ por R5-D7 · H6 (medir com o próprio UiPerformanceMonitor).
11. Melhorias mantidas dos lotes 2-4 (soluções inline nos textos): §8.2 1-14 · §10.2 1-12 · §12.2 1-10 — todas ✅ no inventário (§13.5).
12. Testes: gaps nos monolitos (`LiveViewModel`/`SettingsViewModel`/`SetupViewModel`/`OwnTVShell`/`PlayerHud`) + compose-ui test + `coroutines-test`/`turbine`; testar cada controlo extraído da fatiação (§3).

**P3 — residual/polish (com exercícios; agregável)**
1. ❓ por re-traçar na 6.ª passagem: slots R3-5/11/15/20/35/36/39 · A14 (lote 3) · `tuneTile` hardening (§10.2-12) · wizard `rememberSaveable` (§10.2-9) · kids timer (§10.2-10) · recovery via DataStore (§10.2-11) · R2-28 (`file://` caps M3U).
2. Higiene do repo (§3): dos 7 vendor zips manter 1 · `work\` fora da árvore · 13 `.md` da raiz → `docs\` · namespace `core.i18n` do app → `features.i18n`.
3. Tooling: ktlint/detekt nos build files + ratchet de complexidade · `baselineProfile` gerada em CI (com PR drift-check) · R5-IC5 como nota (testes de redacção do toolkit de diagnóstico) · R4-I8 floor INLINE quando houver passe de Glass.
4. Monolitos: fatiar por §3 na ordem `LiveViewModel` → `OwnTVShell` → `VideoPlayerSettingsScreen` → `SettingsScreen` (resolve também os 27/31 bindings do DI; manter o plano de pastas `parts\`).
5. Notas Baixa ⚠️/❓: captive portal não detectado · `toMpvHeaderFields` descarta vírgulas · UA "learned" das legendas · `enableOnBackInvokedCallback` (nota) · resolução B5 extra (emitir por fases).
6. Ideias (após estabilização): ver inventário §15.3 — TOPs por lote: 1 Stream-Health/Catch-up Flush/ voz/O que ver agora/sumário · 2 Espelhos/Substituto/RecommendationRow/Tracks/Watch Party · 3 Holofote/Ruler/Agenda/Modo Viagem//play · 4 E15/D11/B5/A2/C8+C9 · 5 IE1/IC2/IE4/IE9/IE11.

### 15.3) Inventário de ideias final: 68 + lote 5 = 86 (mapeamento por lote)

| Lote | Total | Tabela de referência | TOP do lote |
|---|---|---|---|
| 1 (§5) | 15 | §5/Apêndice A (§13.6 mantém o registo) | Stream-Health Score, Smart Catch-up Flush, voz do sistema, «O que ver agora», sumário de programa |
| 2 (§8.3) | 18 | §8.3/Apêndice C (§13.6) | Espelhos Vivos, Substituto Inteligente, RecommendationRow, Tracks declarativas, Watch Party LAN |
| 3 (§10.3) | 17 | §10.3/Apêndice G (§13.6) | Holofote de Conflitos, Ruler de Gravação, Agenda Semanal, Modo Viagem, Companion /play |
| 4 (§12.3) | 18 | §12.3/Apêndice H (§13.6) | E15 (telemóvel teclado/trackpad), D11 (snapshot pré-restauração), B5 (vistos/redescobrir), A2 (metered auto-qualidade), C8+C9 |
| 5 (§14.3) | 18 | §14.3 (tabela nova desta ronda) | IE1 (fusão de categorias), IC2 (provar antes de sincronizar), IE4 (Vela Kids), IE9 (memória de rotas de áudio), IE11 (capacidade medida por fonte) |
| **Total** | **86** | | |

Alterações de base dos lotes nesta ronda: nenhuma ideia dos lotes 1-4 foi removida ou renomeada; o lote 5 entra com 18 (de 43 candidatas — §14.3 lista as 25 eliminadas com justificação). Única nuance no mapeamento: as candidatas R5-IB2/ID2/ID4/IC1/IA2/IA5/IE10 **agregadas aos fixes** correspondentes (R5-B4/D1/D7/§10.3-17/R5-A2/R5-A3/R4-G1) em vez do inventário de ideias.

### 15.4) Alterações de veredicto / cruzamentos desta ronda

| Ref. | Alegação submetida (R5) | Veredicto final | Evidência |
|---|---|---|---|
| R5-C5 | "indeterminável; ⚠️/❓ impacto runtime" | ⚠️→✅ mecanismo confirmado | `XtreamClient.kt` L511-534 (timeout 8 s → `false`; catch-all sem log L532-533) e decisão única no import (`SourceImporter.kt` L235-247); a consequência runtime (tipo fixo em M3U) é directa |
| R5-B7 | "`EpgLogoStore.start` é idempotente" | ⚠️ matiz | `start()` (L32-46) lança um colector novo em cada chamada, sem guard — a solução correcta é iniciar 1× em `appScope` + guard; o achado (scope de composição) mantém-se ✅ |
| R5-D1 | "18 ficheiros instrumentados; Core androidTest = 15" | ✅ contagem corrigida | Core androidTest = **16** .kt (15 testes + 1 helper `TestDatabase.kt`); app = 3; total de **testes** = 18 — a alegação fica correcta contando testes (não ficheiros) |
| R5-B5 | apresentado como bug/perf novo | 🔁 repetição de Baixa 4 (§1) | mesmo ficheiro/linhas (`ChannelPressInput.kt` L81-95, `delay(32)`) e mesma solução (Animatable); extra do analista (emitir por fases) entra como refine do fix |
| R5-B6 | apresentado como melhoria nova | 🔁 repetição de §8.2-5 | mesmos sites (`OwnTVApp` L63/`MainActivity` L136/LocaleStore) e mesma solução (cache process-level); nota ❓ do analista mantida |
| R5-B4 | novo (EpgViewModel/RecordingsViewModel) | ✅ mantido como ampliação | 🔗 §8.2-4 — mesma família `Eagerly→WhileSubscribed`; sites novos; fix em lote |
| R5-D4 | novo (splits×flavors) | ✅ mantido como ampliação | 🔗 Baixa 1 — mesmo sintoma do universal; novo ângulo: coexistência incoerente flavors×splits e x86_64 sem output |
| R5-C7 | novo (evict de stats) | ✅ mantido como ampliação | 🔗 §10.2-3 — mesma mapa `lastSyncStats`; evict (novo) + persistência (`sync_stats`) são correções complementares |
| R5-C9 | novo (charset) | nota 🔗 M7 | já coberto pelo M7 (§1 L122-126); a mesma heurística resolve também o R5-C1 |
| R5-C8 | novo (contador de rejeitados) | nota 🔗 R2-13 | a contabilidade de feeds rejeitados é a solução da R2-13; aqui só se expõe no refrescamento global |
| R5-B1 vs R4-c/R4-P3 | possível dúvida de repetição | ✅ novo com 🔗 | mesma família de correção (`setForeground` guard) mas ficheiro/gatilho distintos (DOWNLOAD `DownloadWorker` L30 vs RECORDING `RecordingWorker` L36) — dois fixes |
| R5-C6 vs R5-A4 | possível dúvida de repetição (backup import) | ✅ ambos mantidos | distintos: A4 = portão da **versão** do ficheiro; C6 = degradação **per-line do decrypt** — caminhos de erro diferentes |
| §7-9 (Ronda 1) | "dev builds HIGH 99999 instaláveis sobre qualquer release" | ⚠️ matizado | válido apenas enquanto releases < v10.0.0 (esquema `MA*10000+…`); R5-D3 — CI-feedback corrigido em conjunto com §3 |
| H5 (§2) | checksum do libmpv | ❓→✅ confirmado | R5-D7 — o artefacto nativo resolve de `ahxn00.github.io/OwnTV_Core/maven`, fora do lock e **sem checksum** (a "dependência Maven externa" já não é hipótese) |
| Restante R5 | — | sem refutações | nenhuma alegação obrigatória caiu na verificação (Apêndice I); os pequenos ⚠️/❓ mantêm o Marca do analista |

## Apêndice A — TOP 5 por analista (como submetido)

| Analista | TOP 5 submetido | Estado após verificação |
|---|---|---|
| 1 — UI | SettingsScreen recomposição; release com debug key; hero 12×/s; deep-link re-dispatch; prewarm EPG duplicado + re-blur | 4 ✅ + re-blur parcial (leak refutado) |
| 2 — Core | truncado COMPLETED; resume sem If-Range; charset M3U; media-sequence reset; credenciais Xtream sem escape | 3 ✅ + 1 mecanismo✅/gatilho❓ + 1 ✅ |
| 3 — Arquitetura | fatiar SettingsScreen+Shell; partir LiveViewModel; testes dos monolitos; versionCode único; higiene repo + pin libmpv | 4 ✅ + pin libmpv ❓ |
| 4 — UX | contentDescription; botão Multiview no HUD; linha Catch-up na Home; EmptyState consistente; matar grupos legacy | confirmados/verificáveis |
| 5 — Inovação | #1 Stream-Health; #4 Catch-up Flush; #8 voz; #3 O que ver agora; #10 sumário do programa | integrações ✅ |

## Apêndice B — Medições exactas da verificação

| Medição | Valor | Origem |
|---|---|---|
| `SettingsScreen.kt` | 256 966 bytes (250,9 KB), 71 `@Composable` | `Get-Item`/grep |
| `VideoPlayerSettingsScreen.kt` | 174 707 bytes (170,6 KB) | `Get-Item` |
| `LiveViewModel.kt` | 162 180 bytes (158,4 KB), 141 funs, 20 `MutableStateFlow` | `Get-Item`/grep |
| `OwnTVShell.kt` | 126 009 bytes (123,0 KB), 3 `@Composable`, 22 parâmetros | `Get-Item`/grep |
| `MainActivity.kt` | 40 334 bytes (39,4 KB) | `Get-Item` |
| Flows colectados no root Settings | 44 entre L302-L374 | grep |
| `RootGroup` mortos em `rootItems` | 8 (L483, 490, 537, 612, 666, 673, 689, 710) | grep |
| DI binding `LiveViewModel` | 27 `get()` (L67-97) | leitura |
| DI binding `SettingsViewModel` | 31 nomeadas (L109-143) | leitura |
| Vendor zips | 7 (`vendor\owntv-core-source-*.zip`, ~3,8 MB cada) | `Get-ChildItem` |
| `.md` na raiz / no repo | 13 / ~123 | `Get-ChildItem` |
| `work\` | existe | `Test-Path` |
| Testes app / core / player-core | 76+3 / 162+15 / 80 ficheiros | `Get-ChildItem` |
| Core `values-*` dirs | 26 | `Get-ChildItem` |
| `locales.json` | 44 idiomas | `ConvertFrom-Json` |
| DB `version` | 51 | `OwnTVDatabase.kt` L119 |
| `HapticFeedback` no app | 0 | grep |
| SEARCH intent no manifest | 0 | grep |

## Apêndice C — Caminhos-chave consultados (todos existentes)

- App: `app\src\main\java\tv\own\owntv\MainActivity.kt`; `features\shell\OwnTVShell.kt`; `features\shell\components\SettingsScreen.kt`; `features\settings\VideoPlayerSettingsScreen.kt`; `features\home\HomeScreen.kt`; `features\live\LiveViewModel.kt`; `ui\components\ChannelPressInput.kt`; `ui\components\ChannelListOverlay.kt`; `ui\components\StateViews.kt`; `ui\theme\StackBlur.kt`; `di\AppModule.kt`; `diagnostics\phase4diagnosticsMetrics.kt`; `player\PlayerHud.kt`.
- Build/CI: `app\build.gradle.kts`; `gradle\libs.versions.toml`; `.github\workflows\android.yml`; `app\src\main\AndroidManifest.xml`.
- Core (snapshot `e2081e78…\OwnTV_Core\`): `core\src\main\java\tv\own\owntv\core\download\DownloadEngine.kt`; `core\…\parser\XtreamClient.kt`; `core\…\parser\M3uParser.kt`; `core\…\recording\RecordingEngine.kt`; `core\…\sync\M3uSyncer.kt`; `core\…\database\OwnTVDatabase.kt`; `core\…\database\dao\PlaybackPrefsDao.kt`; `player-core\src\main\java\tv\own\owntv\player\OwnTVPlayer.kt`; `player-core\…\player\PlaybackSession.kt`; `player-core\…\player\LiveTuneController.kt`.
- Docs: `docs\SEGMENTOS_HLS_E_LIMIAR_SUPERIOR_2026-10-11.md`; `tools\i18n\locales.json`.

## Apêndice D — Limites da análise

- **Revisão estática, não runtime**: nada foi executado em emulator/box; tempos/frames citados decorrem da leitura do código (contagens de `collect`/`delay`/loops reais) e não de trace de profiler.
- **Core verificado no snapshot vigente** (`e2081e78…`, lock 1007 ficheiros segundo a CI); se o lock avançar, re-confirmar as linhas citadas.
- **Comportamentos dependentes de provider** (media-sequence reset, charsets, credenciais especiais) ficam como hipóteses formuladas com o mecanismo confirmado em código — a reprovação exige fixtures de servidor.
- **Contagens exactas** (§1.0 e Apêndice B) corrigem estimativas dos analistas onde os dois diferiam; os ajustes foram para números sempre menores (44 e não 50-70 flows; 31 e não 33 deps; 13 e não ~122 `.md` na raiz).
- **UX parcial**: itens marcados "posição do analista" dependem de passagem de UI em device; nada no §4 contradiz o código consultado.

## Apêndice F — Marcações da verificação Ronda 2 (ledger)

Cada alegação Alta/Média submetida na 2.ª passagem foi re-lida no ficheiro/linhas citados (app + snapshot Core `e2081e78…`). Resultado por analista:

| Ref. | Alegação (analista) | Veredicto | Evidência consultada |
|---|---|---|---|
| A-1 | setVolume ignora percent + prefs inoperantes | ✅ | OwnTVPlayer L3744-3748/3749; SettingsViewModel L487; PlaybackPrefsDao L40-80 (colunas `volumeBoost`) |
| A-2 | setMuted não reflete _volume (ícones) | ✅ | OwnTVPlayer L3537-3541; MiniPlayer L143; AudioNowPlayingBar L291 |
| A-3 | toggleVodEngine leva legenda, não áudio | ✅ | L2245-2248/2278-2280 (subs) vs pendingExoAudio só L1180, consumido L2065-2066 |
| A-4 | time-pos/duration INT64 | ✅ | L2632-2634 |
| A-5 | mpvAsync pós-release RejectedExecution | ✅ | L1064-1066 (try dentro da task), release L4168-4177 (shutdown), onTrimMemory L4143-4146 |
| A-6 | mime por extensão / sem charset | ✅ | ExoSubtitleEngine L414-418; grep zero charset |
| A-7 | latch "nunca reposto" | ⚠️ | reset existe na abertura de item (L327); intra-sessão real |
| A-8 | setSubtitleDelay re-prepare | ❓ | não traçado neste pass |
| A-9 | probe trim sem isLiveContent | ✅ (ramo) | L773/L835; re-verificação FILE_LOADED "só-audio" ❓ |
| A-10 | liveSeekWindow/seek no-op | ✅ | PlaybackEngine L46-49; override zero no OwnTVPlayer |
| A-11 | LoudnessEnhancer/VolumeBoost mortos | ✅ | grep LoudnessEnhancer = 0 (app+core); colunas DB existem |
| A-12 | END_TOLERANCE dur/4 | ✅ | L266-278 |
| A-13 | pendingFps re-assert | ⚠️ | L38-39/66-68 (sem guard de setting); update L123 corrige na recomposição |
| B-1 | reminders in-memory sem scheduling | ✅ | store L41-160; EpgViewModel L278/301; zero AlarmManager/WorkManager para lembretes (app+core) |
| B-2 | XmltvTime "+02" → 0 | ✅ | XmltvTime L46-52 (trace do caso 15-char) |
| B-3 | Xtream TZ fixo sem DST | ⚠️ | XtreamClient L1112-1125 (IANA-first existe); CatchupClock L22-25 |
| B-4 | CatchupJumps DST wall-clock | ❓ | não traçado linha-a-linha neste pass |
| B-5 | coerção 7 d (windowStart + canCatchup) | ⚠️ | GuideHistoryPolicy L12-13 ✅; canCatchup L20-23 refutado |
| B-6 | LiveTimeshift scrub wall-clock | ✅ | L114-124/163-172 |
| B-7 | limpeza `check(delete)` aborta | ✅/❓ | LiveViewModel L303; gatilho "lease" OS-dependent |
| B-8 | portal EPG sem hash | ✅ | EpgRepository L244/L325-339 (XML) vs L498-511 (portal) |
| B-9 | XmltvParser sem entidades HTML | ✅ (ausência) | grep zero; comportamento por ❓ |
| B-10 | template catch-up `%` verbatim | ✅ | CatchupUrl L63-83 (`else -> match.value`) |
| B-11 | GuideCore measure/cell no Canvas | ✅ | GuideCore L179-181 |
| B-12 | MiniGuide janela coerceda | ✅ | L46 |
| B-13 | EpgSourceStore parse vazio silencioso | ✅ | L152-169 (`getOrDefault`) |
| B-14 | servidor ts fecha excedentes | ✅ | LocalTimeshiftServer L26/L33 (`tryAcquire` → close) |
| B-15 | quota exige 1..maxBytes | ✅ | LocalSegmentStore L42/L57 |
| B-16 | NUMBER_WORDS só inglês | ✅ | EpgMatcher L60-64 |
| C-1/2/3 | Companion plaintext/PIN query/session | ✅ | CompanionHttpServer L140/L287-292/L318-336; CompanionHtml L226→541; LocalSyncClient L127; LocalSyncManager L193; CompanionLink L23 |
| C-4 | update sem sha256 | ✅ | UpdateManager L203/244 (grep zero digest) |
| C-5 | XXE DashManifest | ✅ | L78 + grep zero setFeature |
| C-6 | DnsConfig fallback silencioso | ✅ | L76/127/132/182 |
| C-7 | Kids títulos explícitos | ⚠️ | classifier doc L12-30 (category-only por design); palavras ausentes confirmadas |
| C-8 | PIN `==`/sem rate limit | ✅ | Pin.kt L13-27; ProfileGate L94-104; lockout só Companion |
| C-9 | cleartext global | ✅ | AndroidManifest L49 |
| C-10 | Pin single-round SHA-256 | ✅ | Pin.kt L13-26 |
| C-12 | M3U sem caps | ✅ (grep) | M3uParser sem maxBytes/cap; `file://` ❓ |
| C-13 | file_paths raiz `.` | ✅ | file_paths.xml L6-9 |
| C-14 | acessibilidade OK / receiver OK | ✅ (amostrado) | auto_start_service.xml L11 (`canRetrieveWindowContent="false"`) |
| D-1 | LIKE nas secções (full-scan) | ✅ | MovieDao L240-263; ChannelDao L370-374; FTS global só L365 |
| D-2 | EPG O(n)/prefetch churn | ✅ | EpgViewModel L501-529 |
| D-3 | LiveHero 2× AsyncImage | ✅ | HomeScreen L1336-1350 |
| D-4 | favoriteIds Eagerly | ✅ | LiveViewModel L1102-1105 |
| D-5 | readBlocking 2× main | ✅ | OwnTVApp L63; MainActivity L136 |
| D-6 | cold start bom | 🔁/✅ | alinhado com §7; único gap: benchmarks não persistem em CI (❓) |
| E | "TTS/PiP/mDNS zero hoje" | ✅ | grep = 0 (app); restantes integrações por confirmar na implementação |

Marcação global: 47 alegações traçadas — 38 ✅ puros, 5 ⚠️ parciais/ajustes (A-7, A-13, B-3, B-5, C-7), 2 ❓ não traçados (A-8, B-4), 1 ✅/❓ (B-7) e 1 🔁/✅ (D-6, coberto por §7) — sem alteração de qualquer veredicto da Ronda 1.

## Apêndice G — Marcações da verificação Ronda 3 (ledger)

Cada alegação do lote 3 foi re-lida nos ficheiros/linhas citados (app + snapshot Core `e2081e78…`). Veredicto por referência:

| Ref. | Alegação (lote 3) | Veredicto | Evidência consultada |
|---|---|---|---|
| R3-1 | Gate "Who's watching?" contornável via "Adicionar perfil" | ✅ | ProfileGate.kt L89 (`AddTile`); SetupWizard.kt L101/L145-156/L200-204/L249; MainActivity L429-434/L456; ProfileGateSessionViewModel L82-91 |
| R3-2 | Edição sensível de perfil sem PIN não pede gate | ✅ | ProfileOperationAuthorization.kt L35-41 (`emptySet()`); ManageProfilesScreen L193-205 |
| R3-3 | Restauro escreve `isKids` sem salvaguarda | ✅ | BackupManager.kt L874-889 (matiz: pinHash `?:` L883 correcto) |
| R3-4 | Restauro preserva `lastSyncAt` do doador | ⚠️ | BackupManager.kt L971-973 (`copy(lastSyncAt = null)`), L929-964; SetupViewModel L171-177 |
| R3-5 | (slot do lote 3) | ❓ | submissão; não re-traçado nesta síntese |
| R3-6 | Estado "playing" com canal invisível | ✅ | LiveViewModel.kt L2082 vs L2093-2096 |
| R3-7 | Importador de backup sem limites | ✅ | BackupImportValidation.kt L11 (`0..Int.MAX_VALUE`) |
| R3-8 | Merge sem fontes remapeia por id cru | ✅ | BackupManager.kt L1086/L1108/L1109-1115; comentário L859-861 |
| R3-9 | Auto-refresh só dispara no `onStart` | ✅ | SettingsRepository.kt L86-94; ShellViewModel.kt L115-166; MainActivity L170/L216/L349; único worker periódico = AutomaticBackupWorker |
| R3-10 | Delete de fonte sem cascata; `purgeOrphans` mortos | ✅ | SourceDao.kt L22-23; purgeOrphans em 6 DAOs com zero call sites |
| R3-11 | (slot — stats de sync em memória) | ❓ | SyncManager.kt L79; não re-traçado |
| R3-12 | `updateSource` reverte campos em branco | ✅ | SettingsViewModel.kt L1084-1150 (L1115-1125) |
| R3-13 | "Testar" vs "Adicionar" no M3U | ✅ | AddSourceScreen.kt L384-394 |
| R3-14 | `lastFailedSource` morto | ✅ | SettingsViewModel.kt L1177-1187 ("It has never actually fired") |
| R3-15 | (slot do lote 3) | ❓ | submissão; não re-traçado |
| R3-16 | Re-sync M3U renumera ids | ❌ | M3uSyncer.kt L34-42/L56-58/L230/L667 (upsertStable preserva ids locais) |
| R3-17 | FTS4 sem unicode61 + ftsQuery | ✅ | OwnTVDatabase.kt L1532-1535; SearchReader.kt L170-175 |
| R3-18 | Cache negativa 7 d sob 200 não-parsável | ✅ | TmdbProvider.kt L339-366 (L340); MetadataRepository.kt L104-108/L148-150; HttpClient.kt L58 |
| R3-19 | `curated` sem filtro de perfil | ✅ | SearchReader.kt L112-142 |
| R3-20 | (slot do lote 3) | ❓ | submissão; não re-traçado |
| R3-21 | TMDB sem retry/backoff | ⚠️ | HttpClient.kt L46-58/L86-102 (retry 5xx/429 + back-off cap L100); TmdbProvider.kt L84 (`tryConsume`) |
| R3-22 | IOException engolida como ApiException | ✅ | SubtitleController.kt L190-202; OpenSubtitlesClient L29 |
| R3-23 | `preserveFailure` sem re-agenda curta | ✅ | TrendingRepository.kt L260-262/L281-283/L288-294 |
| R3-24 | Hero repõe índice a zero | ✅ | HomeViewModel.kt L555-556 vs L560 |
| R3-25 | Branch morto "query curta" | ✅ | SearchScreen.kt L119-120 vs L147-148 |
| R3-26 | Multiview: requesters sem re-focus | ✅ | MultiviewScreen.kt L90/L92/L128 |
| R3-27 | LIKE sem escape (secções) | ✅ (🔁 §8.2-1) | MovieDao.kt L240/L243/L247 |
| R3-28 | Race no fetch de legenda | ✅ | SubtitleRepository.kt L75-80 |
| R3-29 | `localKey` escrito e nunca lido | ✅ | MetadataRepository.kt L581-587 (escrita) vs L625-631 (sem leitura) |
| R3-30 | Detalhes por chave global | ✅ | MetadataRepository.kt L581-587/L625-631 (complementa R3-29) |
| R3-31 | Colisão de episódios + 416 COMPLETED | ✅ | SeriesViewModel.kt L1093/L1113; DownloadEngine.kt L176-186/L195 |
| R3-32 | Truncamento via 416 em descarga normal | ⚠️ | StorageVolumeInfo.kt L47 (cap vFAT honesto); DownloadEngine.kt L202/L205 |
| R3-33 | `markMissed` permanente; rearm manual | ✅ | RecordingEngine.kt L148-155/L1498-1515; RecordingManager.kt L91 (rearm) |
| R3-34 | UA por fonte ignorado no download | ✅ | DownloadEngine.kt L239-241 vs RecordingEngine.kt L1524 |
| R3-35 | (slot do lote 3) | ❓ | submissão; não re-traçado |
| R3-36 | (slot do lote 3) | ❓ | submissão; não re-traçado |
| R3-37 | Agenda por wall-clock sem NTP check | ✅ | RecordingManager init (🔗 §2, Ronda 1) |
| R3-38 | Cleanup de spools ignora deletes | ✅ (matiz) | cleanup pula rows com `hasPlaybackReaders` (não é "ignora it.delete()") |
| R3-39 | `relinkAfterSync` pós-sync | ❓ | grep zero `relinkAfterSync`; RecordingManager.kt L330 — só um comentário "Decline an uncertain channel re-match" |
| R3-40 | Downloads sem ações bulk | ✅ | DownloadsScreen.kt L277-302 |
| R3-41 | Ponte lembretes EPG → gravação ausente | ✅ | ausente no código (🔗 R2-B-1) |

Marcação global Ronda 3: 41 alegações do lote 3 — 30 ✅, 3 ⚠️ (R3-4, R3-21, R3-32), 1 ❌ (R3-16) e 7 ❓ (R3-5, R3-11, R3-15, R3-20, R3-35, R3-36, R3-39 — destes, 6 são slots da submissão por re-traçar na próxima passagem). Nenhum veredicto de §1-§9 foi revertido; as alterações de veredicto do lote 3 estão no §11.7.

## Apêndice H — Ledger da verificação Ronda 4 (grade a-z)

Verificação directa no código (grep + leitura linha-a-linha; app + snapshot Core `e2081e78…`) dos itens TOP 5 dos analisadores de bugs e das Médias de maior alavanca:

| Item | Verificação | Veredicto | Evidência |
|---|---|---|---|
| (a) | `READ_EPG_DATA` ausente / `WRITE` presente | ✅ | grep `EPG_DATA` = 1 (app manifest L16); TvHomeRepository probes sob `runCatching` L460/L482 |
| (b) | `POST_NOTIFICATIONS` declarada, `requestPermissions` 0 | ✅ | core manifest L19; grep `requestPermissions` = 0 repo inteiro |
| (c) | RecordingWorker 1.º setForeground sem runCatching | ✅ | `core\recording\RecordingWorker.kt` L36 vs L43-47; mediaPlayback type core manifest L26+L83 |
| (d) | `_selectedSection` sem savedState | ✅ | ShellViewModel.kt L318; multiview remember OwnTVShell L380/L383 |
| (e) | UpdateManager sem pre-check | ✅ | UpdateManager.kt L265-307; grep `canRequestPackageInstalls` = 0 |
| (f) | fullscreen "deliberately never claims" | ✅ | OwnTVShell L500-504; ConnectionBudget aritmética sobre registry; hero matiz (PlayerModule L51-62; HeroPreviewEngine L187 claim WATCHING) |
| (g) | refusal → MISSED imediato | ✅ | RecordingEngine L152-155/L193-198 |
| (h) | tile ERROR mantém claim | ✅ | MultiviewState L91/L175-181; silenceFailed L127-139; expirePlaybackWait L194 |
| (i) | 458 não re-alimenta medidor | ✅ | RecordingEngine L538-542; ConnectionLimits sem re-probe (AddSource L228/L281; ManageSources L379); worker detalhe ❓ |
| (j) | deleteSource sem tocar gravações | ✅ (loop ❓) | SettingsViewModel L1406-1424; ramo récordies RecordingEngine L1521+ ❓ |
| (k) | TOCTOU timeshift + ENDED sem dispose | ✅ | LiveViewModel L294-310 (check→resolve→tryClaim) e L322-329 |
| (l) | glob `strings*.xml` nos scripts i18n | ✅ linhas / ⚠️ caminho | scripts só no snapshot (`OwnTV_Core\tools\i18n\`): gen L119, validate L95 exactos; repo `tools\i18n\` tem só 5 scripts; core: 2413/2568 no glob → 155 fora (141 alegada, mesma ordem); percentagens por-língua ❓ |
| (m) | app fora do pipeline; pt-PT perde chaves | ✅ | app: 266 strings base / 170 pt; faltam 8 ficheiros inteiros em pt (archive_addresses, backup_complete, channel_categories, epg_clock, epg_row_status, live_header, optimization_controls, timeshift_resume); core 2568/2538 (−30); ~22 idiomas sem values no app |
| (n) | EPG células Canvas sem semantics | ✅ | GuideCore L143-190 (drawText); strip Box EpgScreen L988-993 sem contentDescription |
| (o) | Role.Button 1×; announce/liveRegion 0 | ✅ | ChannelPressInput L100; FocusableSurface L261-275 sem role; greps 0 |
| (p) | Glass floor só FLOATING/CONTAINER | ✅ (matiz) | Glass.kt L284-295; mínimo global 0.22 em L304 mantém-se |
| (q) | RowHeight 64dp fixa + top sem clamp | ✅ | GuideCore L72/L181 |
| (r) | ManualRecordingDialog locale/12-24 h | ✅ | ManualRecordingDialog.kt L32/L33 |
| (s) | HUD cores fixas | ✅ | PlayerHudChrome L164/L168/L184/L186 (`0xFF004F46`/`0xFF6FF8E4`) |
| (t) | sem callTimeout; dns directo | ✅ | DataModule L45-48/L56; callTimeout só LocalTimeshiftSession L49; BoundedDns só LivePreviewEngine L3184 |
| (u) | A+AAAA sequenciais | ✅ | DnsConfig L121-122 (DoH), L179-180 (UDP; matiz comentário L177-178) |
| (v) | Retry-After fora do `get` | ✅ | HttpClient L91-100 fixo; HttpRetryAfter em LocalTimeshiftSession L43/L123, HlsSegmentRetry L60, LivePreviewEngine L3890 |
| (w) | download sem Referer/headers de fonte | ✅ | DownloadEngine L177-178/L239-240; httpReferer existe (SourceRepository L39; SourceTester L81/L116; backup sealed L16/L25) |
| (x) | schedulers CONNECTED vs prefs metered | ✅ | EpgSyncScheduler L27; CatalogSyncScheduler L43/78/112/131; downloadsWifiOnly L1201 |
| (y) | `+`→espaço no M3U headers | ✅ | M3uParser L389-394 (decode quando `%` **ou** `+`) |
| (z) | proxy creds plaintext | ✅ | SettingsRepository L2687-2694/L2703-2710; excluído do backup (L2684); sem Keystore seal |

Grep extra confirmado nesta ronda: `reportFullyDrawn`=0, `enableOnBackInvokedCallback`=0, `sleepTimer`/`highContrast`/`reduceMotion`/guest/delete-after-watch/CSV/thumbnails-seek/favorites-export=0 (app+core), `HistoryDao.kt`+`watchHistory` existem, `FontCustomization`/`UiFontScale` existem, `scrim 0.16` MainActivity L544, `ANIMATOR_DURATION_SCALE`=0 leituras, fichas de integração lote 4 todas existentes (§12.3).

## Apêndice I — Ledger da verificação Ronda 5

Verificação directa no código (grep + leitura linha-a-linha; app + snapshot Core `e2081e78…`) dos itens obrigatórios definidos pelo conselho mais os opcionais de maior relevância. Os restantes itens pequenos da R5 mantêm o Marca do analista (cf. §15.1):

| Item | Verificação | Veredicto | Evidência |
|---|---|---|---|
| (a) R5-C1 | sync não-M3U com 0 itens | ✅ | `M3uParser.kt` ramo else L138-186 (entry só com `pending != null` L152; `entries=0` termina sem erro); `M3uSyncer.kt` L491 prune guard `processed > 0`; `SyncManager.kt` L153-156 `markSynced` sem verificação de contagens |
| (b) R5-C2 | truncamento XMLTV não propagado | ✅ | `XmltvParser.kt` L89-92 (`catch (e: Exception) { if (!requireCompleteDocument) break }`; default `false` L34); `EpgRepository.kt` L375 `parseCompleted=true` → promoção L409-413 + poda L419-421/L575-596 (intenção "promote only on clean parse" em L265-267 fica derrotada) |
| (c) R5-B1 | foreground no worker de downloads | ✅ | `DownloadWorker.kt` L30 `setForeground(...)` nu em `try{...}finally{gate.failed(owner)}`; contraste L38-40 `runCatching{ setForegroundAsync(...) }` |
| (d) R5-C5 | catch-all no probe Xtream | ✅ (⚠️→✅) | `XtreamClient.kt` L507/511-534 (`withTimeoutOrNull(8_000)` `?: false`; `catch (_: Exception) { false }` sem log L532-533); `SourceImporter.kt` L235-247 grava `addM3uSource` quando `resolved.type != XTREAM` |
| (e) R5-A1 | loop de reescritas DataStore no auto-match | ✅ | `EpgViewModel.kt` L887 e L956 (`for ... customize.setEpgMatch(...)`); `CustomizationStore.kt` L244-248 (`update` = 1 `edit` por chamada, L182-183); padrão single-edit L264-273 |
| (f) R5-A2 | cache de legendas sem poda | ✅ | `SubtitleRepository.kt` L36 (`File(appContext.filesDir, "subtitles")`); remoção só manual (`deleteCached` L230-237, `deleteAllForType` L246-259); `MediaStorageBudget.kt` L54-58 (gravações+downloads apenas) |
| (g) R5-A3 | prefs de apresentação globais | ✅ | `SettingsRepository.kt` L1080-1082 (sort), L1279-1285 (sortGuide), L1125-1130 (VOD), L1164-1169 (EPISODE), L402+L1394-1430 (CHANNEL_PLAYBACK_CONFIGS) vs padrão por-perfil L634-656 |
| (h) R5-D1 | androidTest sem job de CI | ✅ | `android.yml` L51-59 (só unit+lint; zero `connected`/emulador/GMD no ficheiro); app androidTest 3 .kt; Core 16 .kt = 15 testes + 1 helper `TestDatabase.kt` (incl. `OwnTVDatabaseMigrationTest`) |
| (i) R5-D3 | dev CODE=99999 | ✅ | `android.yml` L116-122 (`NAME=99.99.99; CODE=99999`); release v105.1.0 → `105*10000+1*100` = 1.050.100 > 99.999 → `INSTALL_FAILED_VERSION_DOWNGRADE` (latente até o esquema chegar a v10.0.0+) |
| (j) R5-D7 | libmpv em GitHub Pages de 3.ª conta | ✅ 🔗 H5 | `settings.gradle.kts` L23-31 (`exclusiveContent` → `https://ahxn00.github.io/OwnTV_Core/maven`, módulo `tv.own.owntv:libmpv`); `app\build.gradle.kts` L7/L350 refere `renatofc27/OwnTV_Core`; `git remote` = `renafcampos27/Owntv`; fora do `core-source-lock.json`, sem checksum |
| (k) R5-C7 | `lastSyncStats` sem evict | ✅ | `SyncManager.kt` L79 (`ConcurrentHashMap`) + L183 (`lastSyncStats[source.id] = runStats`) — nenhum evict; `deleteSource` não toca no mapa |
| (l) R5-B7 | logo singleton em scope de composição | ⚠️ matiz | `OwnTVShell.kt` L336-339 confirmado; `EpgLogoStore.start` (L32-46) lança colector novo a cada chamada, sem guard — "idempotente" impreciso; fix mantém-se (1× em `appScope` + guard) |
| (m) R5-D2 | JVM sobre android.jar stubado | ✅ | `app\build.gradle.kts` L151-155 (`testOptions.unitTests.isReturnDefaultValues=true`) + L429-432 (comentário "android.jar's org.json is a stub" + `testImplementation(libs.org.json)`) |
| (n) R5-C3 | `parseAttrs` só com aspas duplas | ✅ | `M3uParser.kt` L243-258 (`indexOf("=\"")`; sem valor → `emptyMap()`) |
| (o) R5-C4 | `#EXTHTTP:` malformado sem log | ✅ | `M3uParser.kt` L321-330 (`runCatching { ... }.getOrNull()` sem `onFailure`) |

Greps extra desta ronda: `androidx-navigation-compose` com 0 usos fora do toml (L104/L16); `connected|emulator|androidTest` = 0 ocorrências activas no `android.yml`; `isReturnDefaultValues` = 1; git remote ≠ contas em `settings.gradle.kts`/`build.gradle.kts`. Items não re-traçados individualmente mantêm o Marca: A4/A5, B2-B4, C6, D4-D6, D8 (B5/B6/C8/C9 rebaptizados como notas de dedupe — §14.1).

---
*Relatório gerado pelo membro-síntese do conselho de análise OwnTV — verificação independente dos achados críticos contra o código em 2026-10-19 (snapshot Core e2081e78…; Ronda 5 verificada).*


