# Análise OwnTV — Ronda 4 (bugs novos, segurança, build e produto) — 2026-10-19

**Projeto:** `C:\Users\renat\Downloads\OwnTV-main`
**Pasta de saída:** `C:\Users\renat\Downloads\OwnTV-main\implementar_rena`
**Modalidade:** análise estática do código. Não houve build, testes nem execução em dispositivo. Nenhum ficheiro do projeto foi alterado.

**Metodologia:** cinco agentes especializados em paralelo (UI/Compose TV, Player/Lifecycle, Segurança/Dados, Build/CI/Repo, Produto/Inovação). Os achados foram depois verificados por leitura direta do código antes de entrarem aqui. Esta ronda acrescenta ao que já existe em `implementar_rena\` (três relatórios anteriores) e em `docs\MELHORIAS_FUTURAS_PENDENTES.md`. Não repete o que já está lá, exceto quando é necessário para contexto.

---

## 1. Resumo executivo

Os três relatórios anteriores cobrem bem a parte de **backup, pareamento, PIN, build e monolitos**. Esta ronda encontrou problemas que ainda não estavam listados:

- 🔴 **Restauro de backup altera perfis existentes sem autorização** (`pinHash` e `isKids`). Permite desligar o modo Kids ou trocar o PIN de um perfil importando um ficheiro.
- 🔴 **Perfis Kids deixam passar canais sem `categoryId`**. Canais de uma M3U sem `group-title` aparecem mesmo em perfis Kids.
- 🔴 **Downloads e Gravações têm índices desfasados** entre a lista com cabeçalhos e a lista só com itens. Depois de apagar um item, o foco cai no vizinho errado.
- 🟠 **Desemparelhar um dispositivo não revoga o acesso durante a hospedagem** do pareamento.
- 🟠 **Sem limite de tentativas no PIN de perfil** (TV). Um PIN de 4 dígitos pode ser testado em todas as combinações.
- 🟠 **Tag de release sem keystore publica APK assinada com debug** (o CI não tem guarda).
- 🟠 **Deep link `owntv://` é `BROWSABLE`** e inicia reprodução sem confirmação.
- 🟡 **Cleartext global** no manifest e `MANAGE_EXTERNAL_STORAGE`.
- 💡 **Melhor retorno para esforço**: temporizador de sono, reprodução aleatória e faixa de audiodescrição, todos pequenos e sem dependências externas.

---

## 2. Bugs novos — com solução

### 2.1 Severidade alta

#### R4-B01 — Restauro de backup sobrescreve `isKids` e `pinHash` de perfis existentes
- **Localização (Core):** `core\backup\BackupManager.kt` linhas 874-886 (no snapshot `e2081e78…`, linhas 879 e 883). Usa `profileDao.update(existing.copy(isKids = incoming.isKids, pinHash = incoming.pinHash ?: existing.pinHash))`.
- **Evidência:** a escrita é feita com `@Update` sem verificação de autorização, ao contrário de `updateAuthorized` usado noutros caminhos. O `unseal` aceita o `pinHash` como string. A grep confirmou a linha no snapshot atual.
- **Sintoma / cenário:** um ficheiro `.own` com um perfil de nome igual a um perfil do dispositivo, com `"isKids": false` e um `pinHash` novo, desliga o modo Kids e troca o PIN sem o PIN antigo.
- **Solução:**
  1. No restauro, nunca alterar `isKids` nem `pinHash` de um perfil existente sem grant válido (`ProfileAuthorizationGrant`).
  2. Ou usar `updateAuthorized`, ou ignorar esses campos quando o perfil já existe.
  3. Rejeitar `pinHash` em claro vindo de ficheiro (o hash deve ser recalculado localmente ou o campo ignorado).
  4. Teste: restauro com perfil de mesmo nome, `isKids` diferente, `pinHash` diferente → o perfil local mantém o estado.
- **Confiança:** média-alta (código claro; o cenário exige que o utilizador importe um ficheiro).

#### R4-B02 — Índice desfasado entre `rows` e `items` em Downloads
- **Localização:** `app\src\main\java\tv\own\owntv\features\downloads\DownloadsScreen.kt` linhas 148-157 (lido). A linha 297 (`contextIndex = index`) foi indicada pelo agente de UI e não foi relida nesta ronda.
- **Evidência:** `items = rows.filterIsInstance<Item>()` (sem cabeçalhos), mas `contextIndex` é depois usado como índice de `items` (linha 154), e a linha 157 volta a gravar um índice de `rows` em `contextIndex`. Os dois espaços de índices estão misturados.
- **Sintoma:** com `rows = [H1, A, B, H2, C, D]`, apagar A deixa `items = [B, C, D]`. O `contextIndex` continua a 1 e o foco vai para C em vez de B.
- **Reprodução:** separador com dois grupos (cabeçalhos), apagar o primeiro item do grupo de cima.
- **Solução:** guardar a posição **dentro de `items`** (`shown.indexOf(d)` ou o `id`) e usar sempre o mesmo espaço de índices. Nunca misturar `rows` e `items`.
- **Confiança:** alta.

#### R4-B03 — O mesmo desfasamento em Gravações
- **Localização:** `app\src\main\java\tv\own\owntv\features\recordings\RecordingsScreen.kt` linhas 189, 211, 213 e 304-319 (indicado pelo agente, não relido linha a linha).
- **Evidência:** `items = listRows.filterIsInstance<Item>()`, mas `items.getOrNull(contextIndex)` (linha 211) e `contextIndex = listRows.indexOfFirst {...}` (linha 213) usam índices de `listRows`.
- **Sintoma:** depois de cancelar, apagar, parar ou repetir uma gravação, o foco salta para a linha errada, ou para a última quando o índice excede o tamanho.
- **Solução:** igual a R4-B02.
- **Confiança:** média-alta (confirmar linha a linha antes de corrigir).

#### R4-B04 — Perfis Kids: itens sem `categoryId` passam todos os filtros de conteúdo adulto
- **Localização:** Core `content\AdultCategoryClassifier.kt` linhas 37 e 81-85 (`isNullOrBlank → false`; `allows` devolve `true` quando não há nome). App `features\live\LiveViewModel.kt` linhas 1112, 1358, 1416 (`categoryId == null ||`). Core `sync\M3uSyncer.kt` linhas 200-203 e 248-251 (`categoryId = group?.let{…}`).
- **Sintoma:** uma M3U sem `group-title` gera canais com `categoryId = null`. Num perfil Kids esses canais aparecem sempre, porque não há categoria para classificar. O mesmo vale para Downloads (`DownloadsViewModel.kt` 79) e EPG (`EpgViewModel.kt` 1104).
- **Reprodução:** perfil Kids + M3U sem `group-title` com conteúdo adulto.
- **Solução:** para perfis Kids, tratar `categoryId == null` como **não permitido** (allowlist). Alternativa: classificar pelo nome do item quando não há categoria. Aplicar a mesma regra em Live, EPG, Downloads, Home e pesquisa.
- **Confiança:** alta.

### 2.2 Severidade média

#### R4-B05 — Desemparelhar não revoga o acesso durante a hospedagem
- **Localização (Core):** `sync\local\LocalSyncManager.kt` linhas 173, 197 (`secrets = { acceptedSecrets }`, snapshot), 275 (`unpair` só chama `paired.remove`); `sync\local\PairedDeviceStore.kt` linhas 120-124 (o comentário promete revogação imediata); `companion\CompanionHttpServer.kt` linhas 458-466.
- **Sintoma:** desemparelhar um dispositivo perdido com a TV em hospedagem não corta o acesso. O segredo continua aceite em `/sync/hello`, `/backup.own` e `/backup` até parar a hospedagem.
- **Solução:** `unpair` deve remover o segredo de `acceptedSecrets`. O lambda deve ler `paired.secrets()` em tempo real e não um snapshot.
- **Confiança:** alta.

#### R4-B06 — Pareamento: `id` do cliente pode substituir um dispositivo já emparelhado
- **Localização (Core):** `companion\CompanionHttpServer.kt` linhas 297-316 (`id = fields["id"]`); `sync\local\PairedDeviceStore.kt` linhas 103-108 (`put` faz `filterNot { it.id == device.id }`).
- **Cenário:** alguém com o PIN do ecrã (visível durante a hospedagem) envia `POST /sync/pair` com o `id` de um dispositivo já emparelhado. A linha legítima é substituída pelo IP e segredo do atacante. Na próxima sincronização, o backup é selado com esse segredo e enviado para o IP do atacante.
- **Solução:** não usar o `id` vindo do cliente como chave de substituição. Derivar o `id` no host (por exemplo, hash do segredo). Recusar substituir um `id` existente com outro endereço ou segredo sem confirmação explícita na UI.
- **Confiança:** média (exige o PIN; o fluxo está confirmado no código).

#### R4-B07 — Sem limite de tentativas no PIN de perfil (TV)
- **Localização:** `features\profiles\ProfileGate.kt` linhas 94-101 (um erro só muda a mensagem); `features\profiles\ProfileComponents.kt` linhas 102 e 121 (PIN de 4 a 6 dígitos); Core `profile\ProfileOperationAuthorization.kt` linhas 19-25 (`issue` verifica o PIN sem contador).
- **Cenário:** uma criança tenta os 10.000 PINs de 4 dígitos sem atraso nem bloqueio. Pode também usar `authorizeEdit` e `authorizeDelete`. O companion já tem `MAX_PIN_ATTEMPTS` (`CompanionHttpServer.kt` 615), mas a TV não.
- **Solução:** backoff exponencial persistido por perfil (na base de dados, não em memória), bloqueio temporário e PIN mínimo de 6 dígitos.
- **Confiança:** alta.

#### R4-B08 — Perfil Kids sem PIN pode ser desbloqueado sem verificação
- **Localização (Core):** `profile\ProfileOperationAuthorization.kt` linhas 35-41 (`if (profile.pinHash.isNullOrBlank()) return emptySet()`); `profile\ProfileManager.kt` linhas 69-78.
- **Cenário:** um perfil Kids sem PIN pode ser mudado para não-Kids pela própria criança.
- **Solução:** exigir confirmação ou PIN do adulto para reduzir restrições, mesmo sem PIN. Ou mostrar claramente na UI que o perfil não é protegido.
- **Confiança:** alta.

#### R4-B09 — Ações de gravação sem `try/catch`
- **Localização:** `features\recordings\RecordingsViewModel.kt` linhas 261-266 (`stop`), 271-274 (`pauseArchive`), 276-281 (`resumeArchive`), 284-289 (`cancel`), 322-327 (`retry`). Só `rearm` (85-95) e `delete` (302-318) têm proteção.
- **Sintoma:** se `settings.activeProfileId.first()` ou as operações de `recordings.*` lançarem exceção de IO/base de dados, a exceção sobe ao `viewModelScope` e a app fecha.
- **Solução:** envolver cada corpo em `try { … } catch (c: CancellationException) { throw c } catch (_: Exception) { emitir o evento de indisponível }`, como já se faz em `rearm`.
- **Confiança:** média (depende de `recordings.*` lançar de facto; o código lido não o garante).

#### R4-B10 — `continueAfterCatchup` sem proteção contra exceções
- **Localização:** `features\live\LiveViewModel.kt` linhas 2541-2553 (`programmeAfter` e `epgOffsetMinutes.first()` sem `try/catch`). As outras entradas de catch-up (2406-2443, 2459-2520) já capturam `Exception`.
- **Sintoma:** falha na leitura do guia no fim de um arquivo com auto-play pode fechar a app.
- **Solução:** `try { … } catch (c: CancellationException) { throw c } catch (_: Exception) { parar sem continuar }`.
- **Confiança:** média.

#### R4-B11 — Credenciais e headers enviados a apps externas
- **Localização (Core):** `player\ExternalPlayerLauncher.kt` linhas 63-66 (`FLAG_GRANT_READ_URI_PERMISSION`), 107-123 (headers e UA em extras), 125-128 (URL verbatim).
- **Cenário:** URLs Xtream têm utilizador e password no caminho (`/live/USER/PASS/id.ts`). Vão tal como estão num `ACTION_VIEW`, com os headers, para qualquer app que trate `video/*`.
- **Solução:** passar a stream por proxy local ou remover credenciais do URI quando há headers. Usar `setPackage` para apps conhecidas quando possível.
- **Confiança:** média.

#### R4-B12 — `FileProvider` expõe o `filesDir` inteiro
- **Localização:** `app\src\main\res\xml\file_paths.xml` linhas 9-12 (`files-path name="files_root" path="."` e `cache-path path="."`); `ExternalPlayerLauncher.kt` 125-141 (`uriFor` faz `File(url).exists()` → `getUriForFile`).
- **Cenário:** o DataStore fica em `filesDir/datastore/`, incluindo `owntv_paired_devices` com os segredos de pareamento em claro. Um canal com URL apontando para esse ficheiro, em modo de player externo, gera uma URI de leitura para a app escolhida.
- **Solução:** expor só as pastas necessárias (`updates/`, downloads). Em `uriFor`, aceitar apenas ficheiros cujo caminho canónico esteja dentro das pastas de download conhecidas.
- **Confiança:** média.

#### R4-B13 — Deep link `owntv://` é `BROWSABLE` e inicia reprodução sem confirmação
- **Localização:** `app\src\main\AndroidManifest.xml` linhas 97-107; Core `launcher\LauncherDeepLink.kt` 68-102; `features\shell\OwnTVShell.kt` 986-1012.
- **Cenário:** qualquer página web no dispositivo pode disparar `owntv://play/live?name=…` e iniciar reprodução no perfil ativo. O filtro Kids aplica-se, por isso o impacto é sobretudo de UX (reprodução indesejada).
- **Solução:** remover `BROWSABLE` ou exigir confirmação antes de reproduzir.
- **Confiança:** alta.

### 2.3 Severidade baixa

#### R4-B14 — Cleartext global e acesso amplo ao armazenamento
- **Localização:** `AndroidManifest.xml` linha 49 (`usesCleartextTraffic="true"`), linhas 24-27 (`READ`, `WRITE`, `MANAGE_EXTERNAL_STORAGE`).
- **Solução:** `networkSecurityConfig` com `cleartextTrafficPermitted="false"` por defeito, com exceções só para hosts IPTV configurados pelo utilizador. Avaliar se `MANAGE_EXTERNAL_STORAGE` pode ser trocado por SAF.
- **Confiança:** alta.

#### R4-B15 — Companion: 10 PINs errados param o servidor inteiro, e o PIN circula em URL
- **Localização:** `companion\CompanionHttpServer.kt` linhas 256-262 (PIN em `?pin=`), 478-489 (`pinRejected` chama `stop()`); `companion\CompanionHtml.kt` linha 347.
- **Solução:** bloqueio temporário por IP (não parar o servidor). Passar o PIN no corpo do POST, não na query.
- **Confiança:** alta.

#### R4-B16 — Atualização sem checksum próprio
- **Localização (Core):** `update\UpdateManager.kt` linhas 112-143 (URL vinda do JSON), 166-168 (download), 244-256 (`verifyApk` só verifica `packageName` e ABI).
- **Solução:** publicar SHA-256 nas releases e verificar antes de instalar. Comparar o `versionCode` do APK com a tag.
- **Confiança:** média. O Android já impede instalar APK com outra assinatura, o que limita o impacto.

#### R4-B17 — `FLAG_KEEP_SCREEN_ON` limpo também em mudança de configuração
- **Localização:** `MainActivity.kt` linha 159. Fica fora do bloco `if (!isChangingConfigurations …)` (151-158).
- **Impacto:** baixo. O `keepScreenOn` da view cobre a maior parte dos casos.
- **Solução:** mover o `clearFlags` para dentro do `if`.
- **Confiança:** baixa-média.

#### R4-B18 — `ChannelAlternatives.kt` engole `CancellationException` do `await()` (hipótese)
- **Localização:** `features\live\ChannelAlternatives.kt` linhas 134-138.
- **Sintoma:** um cancelamento converte-se em "tentativa falhou". Na prática, a troca de perfil já cancela a recuperação antes, por isso não foi reproduzido.
- **Solução:** distinguir cancelamento do próprio `attemptJob` (retornar `false`) de cancelamento do chamador (`ensureActive`/`throw`).
- **Confiança:** baixa.

#### R4-B19 — Restore de `RecordingsScreen`/`DownloadsScreen`: `restoreFocus` fica `true` com lista vazia
- **Localização:** `DownloadsScreen.kt` linhas 164-173. Com `shown` vazio, o `return` salta `onRestored()`. A flag continua `true`, e quando os downloads chegam mais tarde o foco move-se sem relação com a ação do utilizador.
- **Solução:** chamar `onRestored()` também no ramo vazio. Repetir o `requestFocus` como faz o EPG (3 tentativas, `EpgScreen.kt` ~262).
- **Confiança:** média.

#### R4-B20 — Build/CI: tags sem keystore publicam APK assinada com debug
- **Localização:** `.github\workflows\android.yml`: se `KEYSTORE_FILE` estiver vazio, faz `assembleDebug` sem falhar. Com `IS_TAG == 'true'`, o passo "Publish GitHub Release" publica `OwnTV.apk`, o link "always-latest".
- **Relação:** é a manifestação no CI do problema de assinatura já reportado, mas não tem guarda própria.
- **Solução:** falhar o job quando for tag e não houver keystore.
- **Confiança:** alta.

#### R4-B21 — Build/CI: permissões e supply chain
- **Localização:** `android.yml` linhas 15-16 (`permissions: contents: write` global, incluindo o job `check` que corre em `pull_request` e compila o `build.gradle.kts` do PR). Nenhuma action pinada por SHA e nenhum `timeout-minutes` (contagem zero).
- **Solução:** `permissions: contents: read` no topo, `write` só no job de release. Pinar actions por SHA. Adicionar `timeout-minutes`.
- **Confiança:** alta.

#### R4-B22 — Build: verificação de integridade do Gradle e das dependências desligada
- **Localização:** `gradle\wrapper\gradle-wrapper.properties` tem `validateDistributionUrl=true` mas não tem `distributionSha256Sum`. Não existe `gradle/verification-metadata.xml`.
- **Solução:** adicionar `distributionSha256Sum` e ativar verificação de dependências (`verification-metadata.xml`).
- **Confiança:** alta.

#### R4-B23 — Build: testes e lint só cobrem `standardDebug`
- **Localização:** `android.yml` corre `:app:testStandardDebugUnitTest` e `lintStandardDebug`. Nenhum job corre lint ou testes sobre `release`, onde o R8 muda o código. Os 3 testes de `androidTest` não correm em CI. O flavor `x86_64` nunca é compilado num PR.
- **Solução:** adicionar `lintStandardRelease` e `assembleX86_64Debug` no PR.
- **Confiança:** alta.

#### R4-B24 — Build: dependências atrasadas e não usadas
- **Localização:** `gradle\libs.versions.toml`: `composeBom = "2026.08.00"` (o metadata do Google lista `2026.09.00`). `benchmark = "1.5.0-rc02"` (existe `1.5.0` estável). `targetSdk = 36` com `compileSdk = 37` (`app\build.gradle.kts` linha 48). `navigationCompose` e `androidx-room-compiler` sem referência em nenhum `build.gradle.kts`.
- **Solução:** atualizar BOM e benchmark, alinhar `targetSdk` e remover dependências mortas.
- **Confiança:** média (versões verificadas por metadata; confirmar antes de atualizar).

#### R4-B25 — Build: caminhos longos em `.gradle\core-snapshot` (Windows)
- **Evidência:** `git status --ignored` emite "Filename too long" em `.gradle\core-snapshot\<sha>\…\intermediates`. A pasta `.gradle` tem ~1,8 GB, o `core-snapshot` ~1,7 GB, e há várias versões do snapshot (`e2081e…`, `0bd7b6…`, `0c7aaf…`, etc.).
- **Solução:** ativar `LongPathsEnabled` no Windows. Apagar snapshots antigos e manter só o do `core-source-lock.json`.
- **Confiança:** alta.

#### R4-B26 — Kids: `TMDB_EDGE_KEY` embutida em todas as APKs
- **Localização:** `app\build.gradle.kts` linhas 76-84 (o comentário diz "NEVER in the repo"), mas o valor vai para `BuildConfig`. No CI, é `HMAC(VERSION_NAME, OWNTV_EDGE_MASTER)`, extraível de qualquer APK.
- **Solução:** tratar a chave como pública (é por versão) ou mover a validação para o servidor. Já estava em `implementar_rena` como P2-21; esta ronda confirma o mecanismo.
- **Confiança:** alta.

---

## 3. Melhorias e UX novas

| # | Melhoria | Problema | Solução | Esforço |
|---|---|---|---|---|
| U1 | **Temporizador de sono** | A TV fica ligada à noite com canal ao vivo. Não existe `sleep` no código (grep sem resultados). | Opção no HUD (30/60/90 min) que pausa e liberta o ecrã. Não interfere com gravações ativas. Integra em `player\PlayerHud.kt` (`HudDialog`) e `MainActivity.kt` (`FLAG_KEEP_SCREEN_ON`). | S |
| U2 | **Faixa de audiodescrição** | Utilizadores cegos ou com baixa visão não escolhem a faixa de audiodescrição. Não existe grep para o termo. | Detetar faixa com flag de acessibilidade e mostrá-la no diálogo de áudio. Se não houver, mostrar "sem faixa". Integra em `player\PlayerHudDialogs.kt` e nas preferências de idioma de áudio. | S/M |
| U3 | **"Surpreende-me"** (reprodução aleatória) | Decisão difícil em filmes e séries. | Botão que escolhe ao acaso, excluindo títulos já vistos e conteúdo oculto ou restrito pelo perfil. Integra em `features\movies\MovieViewModel.kt` e `features\series\SeriesViewModel.kt`. | S |
| U4 | **Auditoria de TalkBack** | Muitos `contentDescription = null` (ex.: `MainActivity.kt` 532, `HomeScreen.kt` 726, `AudioNowPlayingBar.kt` 215). Não há `liveRegion`, `stateDescription` nem `announceForAccessibility`. | Descrições em `FocusableSurface`, `OwnTVButton`, `PosterCard`, `ChannelLogoTile` e `PlayerHudControls`. Anunciar mudanças de estado (pausa, canal, erro). | M |
| U5 | **Pesquisa por voz do sistema** | Sem `SEARCH` no manifest. Já listado em `implementar_rena` (ANALISE_OWNTV_2026-10-19, item 8). | Adicionar `SEARCH` / `LEANBACK_SEARCH` e tratar a query em `MainActivity.kt` `onNewIntent`. | M |
| U6 | **Horário parental** (ex.: sem adultos depois das 22h) | Só há Kids e PIN, sem regras temporais. | Regra por categoria e horário, com PIN para exceção. Integra em `features\profiles\ProfileLaunchGate.kt`. Comunicar ao utilizador que o relógio do box pode ser alterado, por isso não é segurança forte. | M |
| U7 | **Estatísticas de visualização por perfil** | Não há noção de quanto tempo se vê e de quê. Só existe `recentlyWatchedSnapshot`. | Registar tempo de reprodução por perfil e mostrar em "Mais" (`features\more\UserDataScreen.kt`). Requer nova tabela (Core). | M |
| U8 | **Saltar abertura por série** | Ver a abertura de cada episódio. A maioria das M3U não tem capítulos. | Botão "Saltar" que o utilizador configura por série (memoriza o salto). Não automático. | M |
| U9 | **Normalização de volume noturna** | Diálogos baixos e efeitos altos à noite. Não existe `LoudnessEnhancer`/`DynamicsProcessing`. | Opção desativada por defeito. Atenção ao custo de CPU em boxes fracas e ao alinhamento A/V. | M |
| U10 | **Canal "a seguir" no launcher** | O launcher só mostra filmes e séries em continuação. | Publicar linha de canais em direto. Depende de alterações no Core (não verificável neste repo). | M |

**Já existem (não propor):** favoritos com reordenação (`LiveScreen.kt` 1977), estilo de legendas (`SettingsViewModel.kt` 728-748), atalhos numéricos (guia do utilizador), velocidade de reprodução (`PlayerHud.kt`), perfis com PIN e Kids.

---

## 4. Inovações (além das já registadas)

| # | Ideia | Valor | Esforço | Notas |
|---|---|---|---|---|
| I1 | **Scrobble para Trakt.tv** | Médio | M/L | Nada no código (só INN-06 em `implementar_rena`). Exige OAuth e consentimento explícito. O produto é "só player", por isso manter opt-in. |
| I2 | **Transmissão DLNA/UPnP** | Baixo/médio | L | Fora do foco "só player". Risco alto por variedade de dispositivos. |
| I3 | **Dashboard parental de uso** | Médio | M | Ligar a U7 e U6. Já estava como INOV-11 em `implementar_rena`. |

---

## 5. Plano sugerido

**Fase 1 — Correções de segurança e dados (urgente)**
1. R4-B01 (restauro altera perfis) e R4-B04 (Kids sem `categoryId`).
2. R4-B05 e R4-B06 (revogação e substituição no pareamento).
3. R4-B07 (limite de tentativas no PIN) e R4-B08 (Kids sem PIN).
4. R4-B13 (`BROWSABLE`) e R4-B20 (tag sem keystore).

**Fase 2 — Estabilidade e foco**
5. R4-B02 e R4-B03 (índices de Downloads e Gravações) e R4-B19.
6. R4-B09 e R4-B10 (try/catch nas ações de gravações e catch-up).
7. R4-B12 (FileProvider) e R4-B11 (credenciais em apps externas).

**Fase 3 — Build e CI**
8. R4-B21 a R4-B23 (permissões do CI, pins, verificação do Gradle, testes em release).
9. R4-B24 e R4-B25 (dependências e caminhos longos).

**Fase 4 — Produto**
10. U1 (sono), U2 (audiodescrição), U3 (aleatório) — S, baixo risco.
11. U4 (TalkBack) e U5 (voz), depois U6 e U7.

---

## 6. Limitações e próximos passos

- **Análise estática.** Nenhum achado foi reproduzido em dispositivo. Os níveis de confiança estão indicados em cada item.
- **Relógios de linha.** Algumas linhas foram verificadas por leitura direta e outras pelos agentes. R4-B03, R4-B19 e R4-B09 devem ser confirmadas linha a linha antes de corrigir.
- **Snapshot do Core.** Os achados do Core foram verificados no snapshot `e2081e78…`, que é o referido em `core-source-lock.json`. Outras versões em `.gradle\core-snapshot\` podem diferir.
- **Recomendação:** antes de implementar qualquer correção de backup ou perfis, correr os testes `OwnTVDatabaseMigrationTest` e os de backup/restauro, e escrever os testes de regressão propostos em cada item.
