# Análise Council — OwnTV (Android TV/box) — Ronda 6

**Data:** 19/10/2026
**Checkout analisado:** `main`, commit `bdffec4` (working tree com `implementar_rena/` por versionar)
**Core:** snapshot personalizado SHA-256 `e2081e78acce95e5ac9b881164c98f066648e6a9400e554c9c9612f527e14890` (v1.0.57-own.20261011), lido de `.gradle/core-snapshot/…/OwnTV_Core`
**Método:** seis agentes especializados em paralelo + verificação direta do Council no código
**Modalidade:** revisão estática — sem build, sem testes, sem execução em box, **nenhum ficheiro de código alterado**

---

## 1. Método desta ronda

Foram lançados seis agentes independentes, cada um com um domínio e a instrução de **não repetir** as rondas anteriores sem reverificar o código atual:

| Agente | Domínio |
|---|---|
| 1 | Reprodução, motores (mpv/ExoPlayer), HUD, zapping, multiview, gravações, lifecycle |
| 2 | Estado, concorrência, ViewModels, persistência, backup/restauro, sync |
| 3 | Compose for TV, foco D-pad, navegação, acessibilidade, i18n |
| 4 | Segurança, Manifest/Providers, build, CI, assinatura, releases, supply chain |
| 5 | Core snapshot: parsers, Room/migrações, Stalker, sync local, timeshift, downloads |
| 6 | Produto/UX/inovação, jornadas, remoção de complexidade |

Depois da entrega, o **Council releu no código** os achados de maior impacto antes de os aceitar (secção 9). Cada item abaixo traz:

- **CONFIRMADO** — caminho de código lido e citado (ficheiro:linha);
- **CONDICIONAL / HIPÓTESE** — mecanismo presente, impacto depende de dispositivo, fornecedor ou concorrência;
- **JÁ REPORTADO** — continua aberto de rondas anteriores, reconfirmado no código de hoje.

Regras respeitadas: distinguir defeito de risco; não elevar alegações sem evidência; medição antes de otimização; cada item tem solução descrita. Cumprida a regra do README: `docs/MELHORIAS_FUTURAS_PENDENTES.md` foi consultado e a pendência **UPSCALING-BETA-01** é avaliada na secção 8.

---

## 2. Resumo executivo

A app está madura e bem estruturada; esta ronda **não encontrou nenhuma regressão catastrófica nova**, mas encontrou:

- **3 defeitos novos de reprodução/lifecycle** confirmados no código (atalho do HUD morto, multiview fora da autoridade de foco de áudio, `PlaybackSession` sem detach em transições de shell).
- **5 defeitos novos de dados/sincronização** (identidade de `userData` sem fontes no backup, corrida no conjunto pendente, histórico de pesquisa gravado antes do resultado, contentor de sync local descartado em silêncio, ficheiros `incoming-*.own` nunca apagados).
- **2 riscos novos no Core** (leitura sem limite no cliente de sync local; fast-path Stalker que pode aceitar um dump parcial).
- **5 novos achados de build/CI/acessibilidade** (asset x86_64 referenciado incondicionalmente, Actions sem pin, Activity de QA exportada em debug, sidebar sem rótulos para TalkBack, feedback dividido entre Toasts do sistema e in-app).
- **Todos os P1 de release/Kids/backup/LocalSync das rondas anteriores continuam presentes** e são, nesta ordem, o que mais importa corrigir.

**Top 5 para corrigir primeiro**

1. **Release quebrada por desenho**: parser de tags de 3 componentes (`android.yml:113-114`) + `versionCode` incompatível (`app/build.gradle.kts:50-51`) + fallback para assinatura debug (`app/build.gradle.kts:175-179`).
2. **Proteção Kids contornável**: adicionar perfil adulto sem credencial adulta (`ProfileGate.kt:89`), categoria desconhecida tratada como permitida (`AdultCategoryClassifier.kt:36-44,66-86`), restauro ainda pode alterar `isKids`.
3. **LocalSync**: HTTP na LAN com segredo no header + ficheiros recebidos em cache que nunca são apagados + leitura de resposta sem limite (`LocalSyncClient.kt:103-118`).
4. **Integridade de dados**: `userData` exportado sem `sources` mantém `src` cru e associa ao id errado noutro dispositivo (`BackupManager.kt:1088-1089`, `UserDataResolver.kt:484`); downloads homónimos podem partilhar ficheiro e `416 → COMPLETED` (`DownloadEngine.kt:183-185`).
5. **Feedback e acessibilidade**: navegação sem rótulos para TalkBack (`Sidebar.kt:425-499`), 20 Toasts de sistema fora do canal in-app, chips de Pesquisa limitados a canais (`SearchScreen.kt:191`), separador de Downloads inerte (`DownloadsScreen.kt:94,245-260`).

---

## 3. Estado das rondas anteriores (o que continua aberto)

| Item anterior | Estado hoje (verificado) | Evidência |
|---|---|---|
| Release sem keystore cai para debug signing | **Aberto** | `app/build.gradle.kts:175-179` |
| Parser de tag 4 componentes / `versionCode` | **Aberto** | `.github/workflows/android.yml:113-114` (`read -r MA MI PA`), `app/build.gradle.kts:50-51` |
| Caminho absoluto `E:/MEGA/...` hardcoded | **Aberto** | `app/build.gradle.kts:40-43` |
| APK universal publicado em vez de splits; x86_64 por `ls … head -n1` | **Aberto** | `android.yml:185-188` |
| Notas de release vazias (sem secção `v105.*`) | **Aberto** | `android.yml:263-267`; `CHANGELOG_APP.md` só até `## v100.0.0` (linha 12) |
| `usesCleartextTraffic=true` global | **Aberto** | `AndroidManifest.xml:49` |
| FileProvider com `external-path "."`, `files-path "."`, `cache-path "."` | **Aberto** | `res/xml/file_paths.xml:4-10` |
| Chave TMDB/edge embutida em BuildConfig | **Aberto** | `app/build.gradle.kts:80-84` |
| LocalSync sem cifragem de transporte (HTTP + `X-Companion-Pin`) | **Aberto** | `LocalSyncClient.kt:100,121-128` |
| PBKDF2 do backup sem teto de iterações | **Aberto** | `BackupCrypto.kt:65-69` |
| Kids: adicionar perfil sem credencial adulta | **Aberto** | `ProfileGate.kt:89` |
| Kids: categoria nula/desconhecida permitida | **Aberto** | `AdultCategoryClassifier.kt:36-44,66-72` |
| Downloads: separador LIVE inerte; filmes/séries inalcançáveis | **Aberto** | `DownloadsScreen.kt:94,245-260` |
| Home só live (`liveOnly = true`) | **Aberto** | `HomeViewModel.kt:548` |
| Pesquisa sem entrada visível na navegação | **Aberto** (só atalho remoto) | decisão anterior; sem alteração no código |
| i18n do módulo app fora do pipeline | **Aberto** (verificado: 266 strings base, 170 `pt`, 233 `pt-rPT`; só 4 pastas de locale no app) | `app/src/main/res/values*` |
| Rota More: Favoritos/Histórico/LocalSync sem linhas | **Aberto** | `SettingsScreen.kt:152,433` (LOCAL_SYNC sem atribuição de navegação) |
| `lastZapPressMs` herdado após escolha deliberada | **Mitigado** — limpo em `onBackground()` | `LiveViewModel.kt:346` |
| `LocalTimeshiftServer` com bypass de token vazio | **REFUTADO nesta ronda** — token é `UUID.randomUUID()` e o servidor liga-se a `127.0.0.1` | `LocalTimeshiftServer.kt:21-27,70` |
| Corrida de resolução Stalker (`loadItem`) | **Não revalidado** — a app atual não contém `loadItem`; antes de implementar, relocalizar no Core | — |

---

## 4. Defeitos e riscos novos — confirmados ao nível do código

### 4.1 Reprodução e lifecycle

| ID | Sev. | Título | Evidência | Impacto | Solução concreta |
|---|---|---|---|---|---|
| R6-PLY-01 | P3 | Atalho de histórico no HUD é código morto | `player/PlayerHud.kt:159` declara `onOpenHistoryList`; usado em `:567-568`; **nenhum** call site o injeta (grep global = só estas 3 ocorrências) | O utilizador carrega em END no HUD e nada acontece; funcionalidade anunciada não existe | Ligar o handler no shell (`OwnTVShell.kt`) e desambiguar a tecla END (guia vs histórico), ou remover o parâmetro e o ramo |
| R6-PLY-02 | P2 | Multiview toca som fora da autoridade de foco de áudio | `features/multiview/MultiviewState.kt:142-150` e `LiveEnginePool.kt` só fazem `setMuted`; **nenhuma** referência a `PlaybackSession` em `features/multiview/**`; o único `requestAudioFocus` vive em `player-core/…/PlaybackSession.kt:322` | Interrupções (Assistente, notificação, HDMI-CEC, outra app) não pausam nem fazem duck ao tile audível; áudio "escapa" ao sistema | Registar o tile audível no `PlaybackSession` (attach/detach no `giveSoundTo`) ou pedir foco no pool; pausar/duck no focus loss |
| R6-PLY-03 | P3 | `PlaybackSession` sem detach quando o shell sai de composição | `OwnTVShell.kt:318-322` (`LaunchedEffect` que só faz `attach`; comentário promete "detach when the player is closed"); `MainActivity.kt:483-491` para engines no switch de perfil mas não repõe `playerMode` | Durante o gate de perfis (shell fora da composição) a MediaSession pode continuar a responder a teclas de transporte de um engine já parado | `DisposableEffect` com `onDispose { playbackSession.attach(null) }` + reset de `playerMode` no teardown |
| R6-PLY-04 | P2 cond. | Catchup/rewind interno inicia mpv sem drenar o Exo | `LiveViewModel.kt:2483-2484` e `:2686-2690` (`releaseForArchive(); player.stop()`) vs. o padrão correto em `:2426-2427` (`stopAndAwaitDrain()/stopAndAwaitRelease()`) | Em chipsets com decoder único (Realtek), sobreposição de decoders → ecrã preto prolongado/áudio atrasado ao abrir o arquivo | Reusar o caminho com drain também nos arranques de catch-up/rewind internos (JÁ REPORTADO, continua aberto) |

### 4.2 Dados, estado e sincronização

| ID | Sev. | Título | Evidência | Impacto | Solução concreta |
|---|---|---|---|---|---|
| R6-DAT-01 | **P1/P2** | `userData` exportado sem `sources` mantém `src` numérico cru | `BackupManager.kt:1089` `sourceIdMap[src]?.let { e.put("src", it) }` (pass-through quando não há mapa); `UserDataResolver.kt:484` `val src = e.getLong("src")`; o bloco `userData` é exportado fora do `if (Section.SOURCES in sections)` (`BackupManager.kt:149-157`) | Exportar sem a secção Fontes (ou com fontes não emparelhadas) faz favoritos/histórico/retoma apontarem para o id de outra fonte no destino: ficam pendentes para sempre **ou** associam ao item errado | Exportar identidade estável (tipo+URL normalizado+username/MAC) ou proibir `userData` sem `SOURCES`; no import, quarentena de ids sem mapeamento e nunca reutilizar o valor cru |
| R6-DAT-02 | P2 cond. | `resolvePending()` faz read-modify-write sem lock partilhado | `UserDataResolver.kt:371-379` lê `pendingStore.data.first()`, resolve e escreve o "remaining" inteiro; `addPending()` pode correr em paralelo (sync/restauro) | Um `addPending` concorrente entre leitura e escrita é substituído; registos pendentes novos perdem-se em silêncio | Mutex/actor único para `addPending`+`resolvePending`; editar por merge de chaves (remover só os resolvidos), nunca substituir o conjunto inteiro; teste de stress concorrente |
| R6-DAT-03 | P2 | Histórico gravado antes de saber se a reprodução foi aceite | `features/search/SearchViewModel.kt:198-234` (launch com validações e `return@launch`) e `:235` `record(MediaType.MOVIE, movie.id)` **fora** do launch; `record()` relê o perfil ativo em `:241` | Abrir um filme que é negado (Kids/oculto) ou falha na resolução Stalker grava histórico; com troca rápida de perfil, grava sob o perfil novo | Mover `record` para dentro do coroutine, depois do play/handoff, com o mesmo `pid` validado; só registar após sucesso |
| R6-DAT-04 | P2 | Contentor recebido em LocalSync descartado sem erro | `LocalSyncViewModel.kt:87` `previewIncoming(file).getOrNull() ?: return@collect`; o emissor conclui com sucesso (`LocalSyncManager` remetente `Done(sent=true)`) | Backup corrompido/chave desconhecida desaparece sem aviso; o remetente acredita que sincronizou | Mapear para `BadPayload`, mostrar erro, quarentena do ficheiro e resposta 4xx ao remetente; retry explícito |
| R6-DAT-05 | P2/P3 | Ficheiros `incoming-*.own` nunca são apagados | `LocalSyncViewModel.kt:193-206` (`confirm` com sucesso não apaga), `:219-223` (`onCleared` não apaga); só `cancel()` apaga (`:209`); o ficheiro é criado em `cacheDir` (`core/…/LocalSyncManager.kt:294`) | Cada RECEIVE/MERGE deixa uma cópia completa do backup (dezenas de MB) na cache; retenção desnecessária de dados de família | Apagar `payload.file` no `onSuccess` e no `onCleared`; purga TTL de `incoming-*` no arranque (app; a purga no Core exige novo snapshot) |
| R6-DAT-06 | P3 | Último canal Live guardado por id global sem validar perfil | `LiveViewModel.kt:1184-1185` grava `settings.setLastLiveChannelId`; `:1192` relê com `channelDao.getById(savedId)` sem `isVisibleToActiveProfile` (usado em `:1951`) | Depois de adulto → Kids, o preview/arranque mostra metadados de um canal adulto; após recriar a BD o id pode apontar para outro canal | Guardar `(profileId, sourceId, remoteId)` e validar visibilidade antes de publicar; limpar no profile switch |

### 4.3 Core (snapshot bloqueado por checksum)

| ID | Sev. | Título | Evidência | Impacto | Solução + onde |
|---|---|---|---|---|---|
| R6-CORE-01 | P2 | Resposta do par de sync local lida sem limite | `core/…/sync/local/LocalSyncClient.kt:103-118` `inputStream.use { it.readBytes() }` no GET e no POST, antes de qualquer validação | Par comprometido/servidor falsificado na LAN envia corpo ilimitado → pressão de heap/OOM antes da validação do contentor | **Core (novo snapshot):** orçamento `min(Content-Length, 32 MB)` + `copyTo` limitado; rejeitar corpo sem tamanho |
| R6-CORE-02 | P2 cond. | Fast-path Stalker aceita dump com `totalItems ≤ items.size` | `core/…/sync/StalkerSyncer.kt:449-453` `bulk.totalItems in 1..bulk.items.size → return@launch` | Se o portal responder a `category=*` com uma categoria real de total pequeno, o dump parcial é aceite e a poda pode apagar o resto do catálogo | **Core:** exigir `items.size == totalItems` (ou confirmar soma por categoria) e nunca podar dumpe sem verificação; **teste:** portal sintético que trata `*` como categoria |

### 4.4 Build, CI e release (achados novos desta ronda)

| ID | Sev. | Título | Evidência | Impacto | Solução concreta |
|---|---|---|---|---|---|
| R6-REL-01 | P2 | Asset x86_64 referenciado incondicionalmente | `android.yml:222-225` copia condicionalmente; `:276-279` lista o ficheiro **sempre** em `files:` | Numa tag em que `X86_SRC` fica vazio, a publicação pode falhar ou omitir o asset que o updater espera | Construir a lista `files` a partir de ficheiros verificados (`test -f`) e falhar a tag se faltar um |
| R6-REL-02 | P2 | GitHub Actions sem pin (tags móveis) com token de escrita | `android.yml` usa `actions/*@v4`, `@v3`, `softprops/action-gh-release@v2`; não há `environment` com aprovação | Tag/conta de fornecedor comprometido publica APK malicioso como release oficial | Fixar cada action ao SHA completo; `permissions` mínimas por job; `environment: release` com reviewer |
| R6-REL-03 | P3 | Activity de QA exportada em builds debug | `app/src/debug/AndroidManifest.xml:7-9` `FontFallbackQaActivity android:exported="true"` | Qualquer app pode abrir o ecrã em builds debug; se o fallback `assembleDebug` da CI publicar, a superfície chega ao utilizador | `android:exported="false"`; gate de release que impede APK debug (já pedido em R6-REL-04/P1 anteriores) |

### 4.5 Interface, acessibilidade e i18n (novos)

| ID | Sev. | Título | Evidência | Impacto | Solução concreta |
|---|---|---|---|---|---|
| R6-UX-01 | P2 | Rail de navegação e avatar sem nome acessível | `Sidebar.kt:425-499` (`FocusableSurface` + `NavDuotoneIcon`, sem `semantics`), `:384-398` (`AvatarButton`); ícones sem `contentDescription` | TalkBack anuncia "botão" em Live/Filmes/Séries/Definições e no avatar: um utilizador cego não navega | Calcular o rótulo fora do lambda e aplicar `Modifier.semantics { contentDescription = label; role = Role.Tab }`; avatar "Perfil: <nome>" |
| R6-UX-02 | P2 | Feedback dividido: 20 Toasts do sistema vs. canal in-app | ex.: `RecordingsScreen.kt:102,124`, `EpgScreen.kt:187`, `LiveViewModel.kt:271`, `ManageProfilesScreen.kt:232`; o próprio `InAppToast.kt:25-27` documenta que o Toast de sistema é pior na TV | Mensagens pequenas/fora da área segura; durações e estilos inconsistentes; TalkBack pode nem anunciá-las | Host único de feedback no shell (fila, duração mínima, dispensa com OK, `liveRegion`) e migrar os 20 `android.widget.Toast` |
| R6-UX-03 | P2 | Chips de intenção da Pesquisa limitados a canais | `SearchScreen.kt:191` `listOf(SearchIntent.CHANNELS)`; `CONTINUE`/`UNWATCHED` existem com texto traduzido (`:63-68`) | Descoberta reduzida; funcionalidade já implementada no Core fica invisível | Iterar os três `SearchIntent` (ou remover rótulos se for decisão de produto) |
| R6-UX-04 | P3 | Texto persistente abaixo do mínimo legível a 3 m | `MoreScreen.kt:401` (10 sp), `:414` (9,5 sp); `VideoPlayerSettingsScreen.kt` usa 8–9 sp em rodapés | Rótulos ilegíveis em 720p com zoom 90% | Piso de 12 sp para texto informativo; 8–10 sp só em elementos decorativos; validar a 720p |
| R6-UX-05 | P2 | Separador de Downloads focável mas inerte | `DownloadsScreen.kt:94` fixa `DownloadsTab.LIVE`, `:245-260` desenha só LIVE com `onClick` vazio | D-pad para num botão que não faz nada; filmes/séries descarregados não têm entrada | Decidir produto: implementar separadores reais ligados ao ViewModel **ou** renomear para "Gravações" e remover a barra |

**Nota positiva de acessibilidade:** o caminho inverso já existe e serve de modelo — `EpgScreen.kt:434-435` aplica `contentDescription` ao botão Voltar, `PlayerHudControls.kt:84-100` deriva rótulo do ícone e `PlayerHud.kt:411-419` devolve o foco ao fechar diálogos. Basta copiar o padrão.

---

## 5. Melhorias propostas (com solução e critério de sucesso)

| ID | Área | Proposta | Solução | Critério de sucesso |
|---|---|---|---|---|
| MELH-01 | Release | Gate fail-closed e versão única | Falhar `release` sem keystore; verificar fingerprint (`apksigner verify --print-certs`), `debuggable=false` e ABI antes de publicar; `version.properties` como fonte única (parser de 4 componentes, monotónico) | Tag sem keystore falha antes de qualquer artefacto; `versionCode` sempre > último release |
| MELH-02 | Kids | Fechar os três caminhos | Credencial adulta independente para adicionar/alargar perfil; política explícita para categoria desconhecida (recomendado negar por omissão com exceção adulta); restauro exige confirmação da mudança de `isKids`/PIN | Kids não consegue sair do modo sem autorização; teste de lista/pesquisa/EPG/Home/catch-up/deep link em perfil Kids |
| MELH-03 | LocalSync | Transporte e ciclo de vida | TLS efémero com fingerprint trocado no emparelhamento (ou payload com chave de sessão acordada fora do canal); revogação imediata no hosting; apagar `incoming-*` no sucesso; limites de tamanho antes de ler | Sniffing LAN passivo não obtém segredo reutilizável; cache sem backups órfãos |
| MELH-04 | Backup | Identidade estável e limites | `userData` sempre com identidade de fonte (ou proibido exportar sem `SOURCES`); validar algoritmo/salt/iterações do KDF antes de derivar; exportação incompleta visível com contadores por secção | Restauro multi-dispositivo com fontes reordenadas: 100% dos favoritos no destino certo; `.own` fabricado rejeitado rapidamente |
| MELH-05 | Downloads | Identidade única por item | Nome/caminho derivado de `(profileId, sourceId, tipo, id)` com título apenas como rótulo; retomar só com identidade confirmada; nunca `416 → COMPLETED` sem prova de tamanho/hash | Dois itens homónimos coexistem; `COMPLETED` implica sempre conteúdo correto |
| MELH-06 | Playback | Foco e ciclo de vida | Tile audível do multiview sob `PlaybackSession`; `DisposableEffect` de detach; drain em todos os arranques de catch-up; cancelar `cleanupScope` no fecho do grid | Interrupção silencia o tile ≤200 ms; MediaSession inativa após sair; zero decoders sobrepostos |
| MELH-07 | Feedback | Canal único acessível | Host no shell com fila, duração mínima 4–6 s, dispensa com OK, `liveRegion = Polite`; migrar Toasts do sistema | Uma única linguagem visual e sonora em todos os ecrãs; TalkBack anuncia |
| MELH-08 | Performance | Só depois de medir | Traçar primeiro: flows no `SettingsScreen`, ticker do hero, N+1 de `DownloadsViewModel.isHidden` (`DownloadsViewModel.kt:58-88`), projeção do EPG; corrigir apenas o gargalo demonstrado | Frame time p95 e pico de heap antes/depois numa box modesta |
| MELH-09 | i18n | Cobertura real | Incluir `app/src/main/res` no gen/validate; completar `pt`/`pt-rPT` (96 e 36 chaves em falta) e alinhar o selo do seletor com a união App+Core | Verificador CI "0 faltas" para locales anunciados |
| MELH-10 | Navegação | Uma porta por destino | Pesquisa no TopBar; Downloads coerente; linhas suportadas no hub More (Favoritos/Histórico/LocalSync) ou remoção de enums/ramos mortos | Nenhum botão inerte; todos os ecrãs alcançáveis por D-pad |

---

## 6. Inovações propostas (novas em relação às rondas anteriores)

| Prioridade | Ideia | Valor | Esforço | Caminho de implementação |
|---|---|---|---|---|
| ⭐ Flagship | **Cofre de Catch-up** — tratar o arquivo do fornecedor como biblioteca com prazo | Único no mercado típico: o catch-up expira em dias e desaparece; o OwnTV guarda o que o utilizador não quer perder | L (faseado) | Fase 1: badge "expira em X" no guia + ação "Guardar até ver" → grava o arquivo para armazenamento local com progresso em Downloads; Fase 2: regras por série/favorito com WorkManager fora de horas, quota e retenção; Fase 3: superfície "Cofre" na Home. Serializar sessões (avisar), nunca gravar canais protegidos, desligado por defeito |
| 1 | **"Só o que tenho" (offline automático)** | Queda de rede deixa de ser um ecrã morto: Home mostra gravações+downloads com banner honesto | S-M | Observar conectividade; Home condicional; corrigir separadores de Downloads (MELH-10) |
| 2 | **Piloto automático offline** — "manter N episódios à frente" da série ativa | Continuidade sem intervenção; só Wi-Fi/carregamento, quota e expiração | M | `OfflineAutopilotWorker` (padrão do `AutomaticBackupWorker`); regras por perfil |
| 3 | **Lista branca Kids por perfil** | Substitui a heurística por regex por escolha explícita dos pais: fontes/categorias/canais permitidos | M-L | `ProfileContentPolicy` no Core (novo snapshot), UI em `ManageProfilesScreen`, filtros em Live/Guia/Home/Pesquisa e gate final de reprodução |
| 4 | **Pedido parental "Pode ver?"** | A criança pede, o adulto aprova na TV (ou telemóvel na LAN) | M | `WatchRequestStore` (Room) + caixa de entrada; PIN nunca sai do dispositivo |
| 5 | **Guia por áudio + zapping falado** | Acessibilidade real para utilizadores cegos: D-pad percorre canais/programas com TTS | M | Observar foco em `GuideCore`/`LiveScreen` e alimentar fila TTS cancelável; toggle em Definições→Acessibilidade |
| 6 | **Biblioteca portátil** — trocar de fornecedor sem perder favoritos | Recupera favoritos/histórico/retomas por `tvg-id` + título normalizado (+TMDB) com pré-visualização e aceitação item a item | L | `SourceMigrationMatcher` (Core) + diálogo em `ManageSourcesScreen`; transação com log |
| 7 | **Pesquisa com ações** | Um campo encontra e oferece: Ver, Ver desde o início, Guardar, Programar | M | `SearchViewModel` + DAOs; secções com ações; filtros Kids/ocultos primeiro |
| 8 | **Modo desporto sem spoilers** | Esconde resultado/descrição no EPG e lembretes; "começar do início" via catch-up | S-M | Máscara em `GuideProgrammeCells`/`LiveEpgCard` + `ProgrammeReminderStore`; toggle por perfil |
| 9 | **Pré-aquecimento preditivo de zapping** | Reduzir zapping p95 com 1 decoder extra mudo | M (arriscado) | Reutilizar `LiveEnginePool` como deck de aquecimento, corte em `isLowRamDevice`; **atenção ao orçamento de sessões do fornecedor** — só com medição |

*Ideias do agente de produto não promovidas nesta ronda: alinhamento automático de legendas por DSP (pesado, mpv/Exo não expõem PCM facilmente), modo cinema/Ambilight, kiosk, OTA empresarial — ficam registadas nos relatórios anteriores ou carecem de hardware/dependências que não se justificam agora.*

---

## 7. Remover ou simplificar (redução de complexidade)

| Item | Porquê | Ação |
|---|---|---|
| **Now Trending + gateway TMDB partilhado** | Na TV as linhas VOD não renderizam (`liveOnly = true`); mantém Worker, quota e chave extraível | Escolher: ativar VOD na Home (com medição) **ou** remover linhas/indicadores/config mortas e manter metadados TMDB locais |
| **Barra de separadores de Downloads** | Botão inerte e ramos inalcançáveis | Implementar ou remover + renomear para "Gravações" |
| **Rotas duplicadas do hub More** | Enums/páginas que voltam a ROOT e duplicam sidebar/Definições | Uma porta por destino |
| **Glass Effect: 6 presets × 10 níveis × por superfície** | QA combinatória e custo GPU em box fraca | Reduzir a "painéis translúcidos on/off" + tema |
| **Três sliders de largura + altura de detalhes do Cinematic** | Combinações sem fim, ganho marginal | Colapsar em 3 presets (Conforto/Densidade/Cinema) |

---

## 8. UPSCALING-BETA-01 — veredicto do Council

Pendência registada em `docs/MELHORIAS_FUTURAS_PENDENTES.md`; avaliada por esta ronda conforme a regra do projeto. **O registo não foi alterado.**

**Veredicto: NO-GO nesta fase** (manter apenas como laboratório interno, desligado por defeito e sem promessa de "4K").

Fundamentos:

1. **Caminho de render é zero-copy.** `MpvVideoSurface` usa `SurfaceView` e ExoPlayer escreve direto ao compositor — não existe ponto de injeção de shaders sem quebrar esse caminho. Upscaling por GPU exigiria `TextureView`/`ImageReader`+shader: em boxes de 2–4 GB, 1080p→4K a 60 fps é inviável; 720p→1080p é possível mas com custo de GPU/RAM/energia e risco de frames perdidos e dessincronia A/V.
2. **O buffer não cria detalhe.** Aumentar buffer absorve jitter e evita paragens — não aumenta resolução, e adiciona latência/memória. Em SD/interlaçado pode até piorar.
3. **DRM e HDR.** Frames de descodificador seguro (L1) nunca são expostos; HDR sem tone mapping dedicado degrada. A TV já faz escalonamento por hardware de qualidade — competir com ele raramente compensa.
4. **Custo de manutenção.** O render path vive no `player-core` (snapshot bloqueado por checksum); suportar múltiplas GPUs/box exige validação por dispositivo, sem fim previsível.

Se algum dia reabrir: A/B cego em boxes alvo (TCL Google TV, Fire Stick 4K, box 2 GB Amlogic), 720p/1080p SDR+HDR, padrões + desporto, medir frames perdidos (`dumpsys gfxinfo`), GPU/RAM, energia, zapping, sincronismo A/V e 30 min de soak; decidir por preferência cega de utilizadores, não por PSNR.

---

## 9. Reavaliação cruzada do Council

| Alegação | Veredicto | Fundamento |
|---|---|---|
| R6-PLY-01 (atalho morto) | **Aceite** | Verificado: só 3 ocorrências no código, todas em `PlayerHud.kt` |
| R6-PLY-02 (multiview sem foco) | **Aceite** | Verificado: `features/multiview/**` sem `PlaybackSession`; único requester em `player-core` |
| R6-PLY-03 (detach) | **Aceite com rebaixamento a P3** | A maioria dos fechos passa `playerMode = NONE` → o efeito re-corre com `null`; o risco material é o gate de perfis (shell fora da composição) |
| R6-PLY-04 (drain no catchup) | **Aceite como aberto** | Padrões divergentes no mesmo ficheiro; falta ensaio em box Realtek |
| R6-DAT-01 (userData sem sources) | **Aceite (P1/P2)** | Código confirma pass-through; impacto depende de exportar sem Fontes |
| R6-DAT-02 (RMW pendente) | **Aceite como condicional** | Mecanismo presente; perda só com concorrência real |
| R6-DAT-03 (histórico cedo) | **Aceite** | `record` fora do launch; relê o perfil ativo |
| R6-DAT-04/05 (LocalSync silencioso / lixo) | **Aceite** | `getOrNull() ?: return@collect`; `confirm`/`onCleared` não apagam |
| R6-CORE-01 (readBytes sem limite) | **Aceite** | Código direto no cliente |
| R6-CORE-02 (Stalker bulk) | **Condicional** | Exige um portal que devolva categoria `*` pequena; teste com portal sintético antes de corrigir |
| R6-REL-01/02/03 | **Aceites** | Verificados no workflow atual |
| R6-UX-01..03 | **Aceites** | Código verificado; impacto TalkBack a confirmar em box |
| R6-UX-04 (tipografia) | **Aceite P3** | 9,5–10 sp medidos no ficheiro |
| Alegação "token do timeshift vazio" (ronda anterior) | **REFUTADA** | `UUID.randomUUID()` + bind `127.0.0.1`; risco não existe no snapshot atual |
| "Três pedidos por rajada de zapping" | **Mantém-se rejeitada** | Já refutada em rondas anteriores; nada mudou |
| "Fuga do backdrop blur" | **Mantém-se refutada** | `StackBlur` opera in-place |

**Itens que exigem validação física antes de qualquer correção:** foco do multiview e diálogos (D-pad), ciclo de vida do freeze frame, drain Exo→mpv em chipset Realtek, interrupção de áudio no multiview, TalkBack na sidebar, e o fast-path Stalker com um fornecedor real.

---

## 10. Roadmap sugerido

**Fase 1 — Distribuição e proteção (dias):** gate de assinatura fail-closed; parser de tags/versionCode numa só fonte; remover caminho `E:/…`; verificação de fingerprint/ABI/notas no preflight; credencial adulta para adicionar/alargar perfis; política de categoria desconhecida; restauro não altera `isKids` sem autorização.

**Fase 2 — Dados e rede:** identidade estável de `userData`; limites de KDF; exportação incompleta visível; LocalSync com transporte protegido + revogação imediata + limites de leitura; apagar `incoming-*`; destinos únicos de downloads e `416` só com identidade confirmada; lock do conjunto pendente.

**Fase 3 — Reprodução e UX:** foco de áudio no multiview; detach da `PlaybackSession`; drain no catch-up; canal único de feedback + rótulos acessíveis; Pesquisa/Downloads/More coerentes; drain de ficheiros órfãos no Leitor de armazenamento.

**Fase 4 — Produto:** Cofre de Catch-up (fase 1); "Só o que tenho"; piloto automático offline; guia por áudio. Performance e demais otimizações apenas após trace em box modesta.

### Checklist pré-release (mínimo)

1. `assembleStandardRelease` + `assembleX86_64Release` com keystore; nenhum `assembleDebug` em job de tag.
2. Assinatura = certificado de produção; `debuggable=false`; ABI presente no interior do APK.
3. `versionCode` > último release; tag de 4 componentes aceite; notas retiradas de `CHANGELOG_APP.md`.
4. `SHA256SUMS`, `mapping.txt` e lista de assets verificada (`test -f`).
5. Instalação sobre a release anterior com a mesma chave (upgrade real).
6. `lint`/`testStandardRelease` verdes; smoke em emulador TV API 26/31/36.

### Testes a acrescentar

- Migração Room v3/v5/v6/v50 → v51 com `MigrationTestHelper` (a alegação antiga merece um teste real).
- Restauro `.own` sem `SOURCES` com favoritos/resumes: nenhum item no destino errado.
- Sync local com corpo de 40 MB e corpo sem `Content-Length`: rejeição rápida.
- Dois downloads homónimos e retoma após troca de item.
- Kids: categoria nula na lista, pesquisa, EPG, Home, catch-up e deep link.
- Interrupção de áudio durante multiview; teclas de transporte após fechar o player.

---

## 11. O que está bem e não deve ser mexido

- **Integridade do Core:** `core-source-lock.json` + `gradle/custom-core.settings.gradle.kts` verificam SHA-256 do ZIP e de cada um dos 1007 ficheiros, rejeitam ficheiros extra e zip-slip. É uma base sólida — qualquer correção no Core exige **novo snapshot**, não edição local.
- **Parsers e timeshift:** HLS com guardas de overflow; timeshift em loopback com token UUID, quota com reserva e commit atómico; Stalker com retry classificado e poda só em passe completo (com a exceção condicional R6-CORE-02).
- **Backup defensivo:** marcador de restauro interrompido, GCM validado antes de escrever, `allowBackup=false` com regras de extração fechadas, PIN local preservado quando o backup não tem palavra-passe.
- **Reprodução:** autoridade única de foco/MediaSession é o desenho certo (falta cobertura, não redesenho); retenção explícita do preview com `stopAndAwaitDrain`; orçamento de conexões do multiview.
- **UI:** restauro de foco deliberado (`MoreScreen.kt:128-183`, `EpgScreen.kt:403-423`), HUD com rótulos derivados do ícone, `UiZoom` 50–150% e base i18n do Core com 29 pastas de recursos.

---

## 12. Limites desta análise

- Revisão **estática**: sem builds, testes, profiling ou box física; desempenho, timing e comportamento de codec continuam por medir.
- Não foram alterados ficheiros da app ou do Core; este documento é um plano, **não autoriza implementação**.
- Itens marcados CONDICIONAL/HIPÓTESE só devem virar trabalho após o teste indicado (portal sintético Stalker, interrupção de áudio, D-pad em box, TalkBack).
- Nenhuma percentagem de melhoria foi medida; contagens de linhas citadas referem-se ao commit `bdffec4`.

---
---

# Anexo — Ronda 7: novos achados, verificação de alegações e reavaliação global

**Data:** 19/10/2026 · **Checkout:** o mesmo `bdffec4` (nenhum ficheiro de código foi alterado) · **Core:** snapshot `e2081e78…`
**Método:** sete frentes de análise estática — Definições (2 vagas), Setup/Fontes/Perfis, Home/Live/EPG, HUD/Gravações/Downloads/Legendas, Core 2ª vaga e Verificação de 18 alegações anteriores — seguidas de **re-verificação direta do Council** em ~60 pontos de código. Todos os ficheiros de código da app e o snapshot do Core foram lidos; nada foi executado.
**Convenções:** `app/…` = `app/src/main/java/tv/own/owntv/…`; `core/…` = snapshot `OwnTV_Core/core/src/main/java/tv/own/owntv/core/…`; **CONFIRMADO** = linha lida e citada; **HIPÓTESE** = requer medição/runtime; **JÁ REPORTADO** = aberto de rondas anteriores (site novo anotado); **NCS** = correção exige **novo snapshot do Core**. O veredicto UPSCALING-BETA-01 (secção 8) não é alterado; `docs/MELHORIAS_FUTURAS_PENDENTES.md` foi reconferido e nada nele muda com esta ronda.

**Top 10 da Ronda 7 (corrigir primeiro dos novos):**

1. **S1/S3** — 4 toggles de listas e o layout "Cinematic" de VOD estão inalcançáveis na UI (Definições).
2. **S5 (com S17)** — DNS: foco cai no 1º preset ao abrir e um Enter sobrescreve o servidor próprio; desligar o toggle apaga o servidor guardado.
3. **S11** — PIN de perfil/painel sem throttling: brute-force de 4 dígitos viável no comando.
4. **Home A1** — a busca de Filmes/Séries está *sempre vazia* (flags forçadas a `false`).
5. **Home A2** — o ecrã Personalizar não tem seletor de secção: Movies/Séries inalcançáveis.
6. **HUD-REC** — "Gravar" no HUD falha em silêncio quando o orçamento de conexões recusa.
7. **DL-DEL-PLAY** — apagar um download não pára a reprodução local (assimetria com gravações).
8. **Setup A7** — credenciais Xtream não são codificadas nas URLs de live/VOD/série/catch-up.
9. **C01 (Core/NCS)** — títulos do guia Xtream que sejam base64 válido (ex.: `News`) aparecem corrompidos.
10. **C05 (Core/NCS)** — `DROP INDEX` de bulk sem registo de estado parcial pode deixar a base sem índices até reiniciar.

---

## A. Verificação das 18 alegações anteriores

### A.1 Continuam abertas (11) — com solução

| # | Alegação | Evidência hoje (verificada) | Solução |
|---|---|---|---|
| 1 | Índice de linha vs. índice de item em listas com cabeçalhos (Gravações) | `app/features/recordings/RecordingsScreen.kt:304-319` grava `contextIndex = index` (espaço de **linha**, `itemsIndexed(listRows)` em `:294`), mas `:211` usa-o em `items` (sem cabeçalhos) | Unificar o espaço: guardar sempre o **id** focado e resolver posições com `indexOfFirst` em cada lista (ou manter só o índice de linha e converter com o id); teste com cabeçalhos |
| 2 | Pesquisa do EPG não é limpa ao desativar o guia | `app/features/epg/EpgViewModel.kt:991-1004` (`setGuideActive`) não toca em `_query`; só `onBackground()` (`:984-988`) limpa | Limpar/parquear `_query` em `setGuideActive(false)` (ou ao reabrir com gate) e repor o filtro visual |
| 3 | Polling de recuperação a 20 Hz | `app/features/live/LiveViewModel.kt:2146-2151` `while(true) { …; delay(50) }` | Substituir por espera orientada a eventos (`StateFlow`/`CompletableDeferred` dos flags `recoveryOpenedId/FailedId`) com deadline; sem busy-wait |
| 4 | Deep link re-despachado ao recriar a Activity | `app/MainActivity.kt:202` (onCreate re-parseia `intent.data` em qualquer criação) vs. consumo em `:479-480` | Parsear só com `savedInstanceState == null`; após consumo, limpar `intent`/flag para sobreviver a rotação |
| 5 | `indexOfFirst` no overlay de canais | `app/features/shell/components/ChannelListOverlay.kt:77-78` (`coerceAtLeast(0)` esconde "não encontrado") | Mapa `id→índice` memorizado com `channels`; fallback explícito (primeiro item **ou** manter scroll) quando o id atual não está na lista |
| 6 | Poll de 32 ms no press de canal | `app/ui/components/ChannelPressInput.kt:81-94` (`delay(32)` para a animação de progresso) | Alimentar o progresso com `withFrameNanos` (vsync) enquanto pressionado; elimina wakeups fixos |
| 7 | `split('&')` cru nos headers M3U | `core/parser/M3uParser.kt:377` — valor com `&` não codificado corta o header | Segmento sem `=` passa a ser continuação do valor anterior (já descodificado); teste com `User-Agent=Foo Bar&Baz` |
| 8 | `KEEP_SCREEN_ON` limpo em mudança de configuração | `app/MainActivity.kt:159` `clearFlags(...)` incondicional no `onStop` (o guard `isChangingConfigurations` existe em `:151` mas não aqui) | Só limpar quando a reprodução parar; reaplicar em `onStart` se reproduzir; nunca limpar durante `isChangingConfigurations` |
| 9 | `restoreFocus` preso nos Downloads | `app/features/downloads/DownloadsScreen.kt:164-172` — `return@LaunchedEffect` em `:165` **sem** `onRestored()`; chaves sem `lastPlayedId` | Chamar `onRestored()` também no caminho de lista vazia e adicionar `lastPlayedId` às chaves |
| 10 | `CancellationException` engolida | `app/features/live/ChannelAlternatives.kt:134-138` `catch (_: CancellationException) { false }` | Re-lançar se o coroutine chamador foi cancelado (`currentCoroutineContext().ensureActive()`); `false` só para cancelamento do filho |
| 11 | `UpdateManager` sem validação de integridade do APK | `core/update/UpdateManager.kt:244-256` (parse/ABI/packageName apenas; `digest` do release ignorado em `:135-144`) | Validar SHA-256 do asset (`digest`) antes de instalar + comparação de certificado; ver C08/C09 | 

### A.2 Resolvidas / refutadas nesta verificação (7) — sem ação

| Alegação | Veredicto |
|---|---|
| Opacidade de legendas | **Resolvida** — não reproduz no código atual |
| Watchdogs de retoma (resume) | **Resolvida** |
| Preferência de gravações | **Resolvida** |
| Categorias ocultas do EPG a reaparecer | **Resolvida** |
| Autenticação de PIN de perfil (bypass) | **Resolvida** (o risco remanescente é outro: *throttling* — ver S11) |
| Pré-aquecimento duplicado do guia | **Resolvida** (a *ineficácia* do pré-aquecimento é achado novo — ver A.3.1) |
| Escaping Xtream (alegação antiga) | **Resolvida** no ponto antigo; permanece a lacuna nova nos **ids** de API (`XtreamClient.kt:227,366`) — ver A.3.2 |

### A.3 Achados adicionais notados durante a verificação (4) — com solução

| ID | Sev. | Título | Evidência | Solução |
|---|---|---|---|---|
| V-01 | P3 | "Pré-aquecimento" do guia nunca corre com o guia inativo | `app/features/epg/GuideLoadGate.kt:8-24` — `beginLoad()` exige `active`; com o guia fechado só marca `dirty` (é coalescência, não prewarm) | Ou implementar carga limitada em background (respeitando orçamento), ou remover o nome/promessa "prewarm" e documentar o comportamento real |
| V-02 | P3 | Ids Xtream interpolados sem escape em chamadas de API | `core/parser/XtreamClient.kt:227` (`&series_id=$seriesId`), `:366` (`&stream_id=$streamId`) | Validar formato numérico na origem (sync) e/ou `URLEncoder` defensivo; teste com id contendo `&` |
| V-03 | P3 | `restoreFocus` preso no EPG quando o foco nunca consegue | `app/features/epg/EpgScreen.kt:255-268` — `onRestored()` só é chamado no sucesso; após 3 tentativas o flag fica | Chamar `onRestored()` como `finally`/fallback (focar primeiro item) após esgotar as tentativas |
| V-04 | P3 | Efeito de restauro dos Downloads sem chave `lastPlayedId` | idem A.1.9 (`DownloadsScreen.kt:164`) | idem A.1.9 |

---

## B. Definições — novos achados (S1–S18)

*S1–S15 da vaga nova (leitura integral de `SettingsScreen.kt`, 4 575 linhas, + `features/settings/**`); S16–S18 da 1ª vaga, re-verificados pelo Council.*

| ID | Sev. | Estado | Título | Evidência | Impacto | Solução |
|---|---|---|---|---|---|---|
| S1 | Alta | CONFIRMADO | "Navegação & listas" só renderiza 2 dos 6 toggles | `app/features/shell/components/SettingsScreen.kt:3362-3376` (só `catLive`/`itemLive`); params Movies/Séries em `:3321-3332` sem uso | 4 configurações persistidas são inalcançáveis | Renderizar os 6 botões agrupados por secção; critério: alternar Movies persiste após restart |
| S2 | Média | CONFIRMADO | Chip "Tamanho de painel" ignora Movies/Séries | `SettingsScreen.kt:373` `val panelWidthCustom = panelWidthLive` (coleta em `:371-372` sem uso) | Utilizador acha que nada foi aplicado | `= live \|\| movies \|\| series`; teste de chip |
| S3 | Alta | CONFIRMADO | Layout "Cinematic" VOD sem UI alcançável | `SettingsScreen.kt:228` (nasce `false`), `:1413-1437` (diálogo só fecha); nenhuma escrita `= true` | Feature morta; ninguém troca o layout VOD | Ligar a linha real (Customize/Home) ou remover o diálogo; critério: alternar aplica e persiste |
| S4 | Baixa | CONFIRMADO | Picker de categorias morto | `SettingsScreen.kt:211` (`showCategories` nunca `true`), diálogo `:1394-1402` | Código/strings mortas; risco de reativação | Remover ou dar entrada real |
| S5 | Alta | CONFIRMADO | DNS: foco inicial no 1º preset; um Enter sobrescreve o servidor próprio | `DnsSettingsScreen.kt:112-119` (foco ao ligar) + `:174-192` (preset faz `server=url; applySave()` imediato) | Reabrir com DoH próprio e confirmar troca por Google sem aviso | Focar o toggle/campo; preset exige confirmação quando já há servidor; "Desfazer" (ver J.3) |
| S6 | Média | CONFIRMADO | Teste DNS ignora a porta digitada | `DnsSettingsScreen.kt:222-225` passa o texto cru e porta fixa `53`; Save usa parser próprio `:85-101` | `8.8.8.8:5353` salva bem mas o teste "falha" | Reutilizar o mesmo parser no teste, com porta real; teste unitário |
| S7 | Média | CONFIRMADO | Validação fraca de host/porta/IPv6/DoH | `DnsSettingsScreen.kt:85-101` (`lastIndexOf(':')`; `[v6]:porta` fica com colchetes; `https://` vazio aceito; porta inválida → 53 em silêncio) | Config inválida salva sem erro | `parseDnsInput()` único: `[v6]:porta`, 1-65535, URL DoH com host; erro inline |
| S8 | Média | CONFIRMADO | Toggle "Ligado" não persiste sem Save | `DnsSettingsScreen.kt:63-82` (ON é estado local; `effectiveEnabled` local `:72`) | Chip diz "Ligado" mas o DNS real segue desligado | Persistir ao ligar com servidor salvo, ou marcar "não guardado" |
| S9 | Média | CONFIRMADO | Export de backup sobrescreve ficheiro anterior | `BackupViewModel.kt:48-53` (o Core escreve sempre `owntv-backup.own` na pasta escolhida) | 2º export apaga o 1º ("antes"/"depois" de reconfigurar) | Nome com timestamp (`owntv-backup-<data>.own`) ou confirmar sobrescrita |
| S10 | Média | CONFIRMADO | Restauro/export sem cancelar e sem progresso fora do ecrã | `BackupScreen.kt:177-181` (só spinner); `LocalBackupOperation.kt:47-52` (`reset()` só re-mostra); `BackupScreen.kt:285` `skipLabel=null` deixa `onSkip` inalcançável | Back sai e a escrita continua sem indicação; sem cancelamento | Botão Cancelar (aviso de escrita parcial, com geração da operação); estado visível ao voltar; remover caminho morto |
| S11 | Alta | CONFIRMADO | PIN de perfil/painel sem throttling em nenhum fluxo | `BackupScreen.kt:577-581` `verify(pin)` direto; sem contador/bloqueio em `features/profiles/**` | PIN de 4 dígitos brute-forçável no comando em minutos | Contador + atraso exponencial + bloqueio persistido por perfil/dispositivo; teste: 5 erros → 30 s |
| S12 | Baixa/Média | CONFIRMADO | Back durante o download de update cancela em silêncio | `UpdateDialog.kt:80-81` (`dismiss()`→`cancelPendingUpdate()`); `core/update/UpdateManager.kt:230-237` | Utilizador perde o download sem saber | Estado "Downloading" com "Cancelar" explícito + aviso "sair cancela a atualização" |
| S13 | Baixa | CONFIRMADO | Bloco morto `if (false && mode.enrich)` no Metadata | `MetadataSettingsScreen.kt:254-296` (cards inalcançáveis com `LaunchedEffect` dentro) | Apodrecimento; risco de duplicação | Remover; extrair composable se reutilizável |
| S14 | Baixa | HIPÓTESE | Save de chave/URL TMDB sem validação | `MetadataSettingsScreen.kt:391-394` + `SettingsViewModel.kt:1656` aceitam qualquer string | Chave errada salva "com sucesso" | Validar 32 hex e URL https com host; erro inline |
| S15 | Baixa | CONFIRMADO | A11y/RTL: setas como glifos, chip `"v"` cru | `HomeSettingsScreen.kt:240-246` (`"⤒ ↑ ↓ ⤓"` sem `contentDescription`, `enabled=` tira do foco); `SettingsScreen.kt:733,1001` (`"v"+VERSION_NAME`) | Sem strings traduzíveis; foco inconsistente | Strings recursivas + `contentDescription`; manter focoável/esmaecer; string formatável para a versão |
| S16 | Média | CONFIRMADO (1ª vaga) | "Limpar histórico" só oferece Live e All | `SettingsScreen.kt:2311-2316` (enum tem MOVIES/SERIES) mas `:2351-2355` só desenha Live+All (com Spacers órfãos `:2352-2354`) | Não se pode limpar só Filmes ou só Séries | Renderizar os 4 botões a partir do enum; teste de cada scope |
| S17 | Média | CONFIRMADO (1ª vaga) | Desligar o DNS apaga servidor guardado | `DnsSettingsScreen.kt:75-82` `saveDns(enabled=false, host="", port=53, dohUrl="")` | Perde-se a config (host/porta/DoH) só de desligar | Persistir apenas `enabled=false`; manter host/porta/DoH guardados |
| S18 | Média | CONFIRMADO (1ª vaga) | Export de backup sem repetição da palavra-passe | `BackupScreen.kt:281-289` e `:435-447` (campo único, sem confirmação) | Typo na password torna o backup irrecuperável | Segundo campo "repetir" + aviso de força; critério: export recusa se diferirem |

---

## C. Setup / Fontes / Perfis — novos achados (A1–A8)

| ID | Sev. | Estado | Título | Evidência | Impacto | Solução |
|---|---|---|---|---|---|---|
| A1 | Média | CONFIRMADO | Retomar submissão falhada é impossível; rascunho destruído | `app/features/settings/SettingsViewModel.kt:1187` `val lastFailedSource get() = null` (KDoc admite); consumido em `ManageSourcesScreen.kt:237`; `AddSourceScreen.kt:153-155,376` descarta o rascunho/payload no dispose; `SetupWizard.kt:196` `vm.reset()` | "Tentar novamente" abre o formulário vazio; no fluxo por telemóvel o payload já foi consumido | Snapshot da última tentativa devolvido no retry (wizard + Definições); consumir payload só após sucesso |
| A2 | Média | CONFIRMADO | `backgroundHandoff` pegajoso (nunca é `false`) | `app/features/setup/SetupViewModel.kt:202` (`= true`); nenhuma escrita `= false` no repo; `core/setup/SourceImporter.kt:467` decide por ele; instância `factory` (`core/di/DataModule.kt:301`) limita o alcance ao VM do wizard | Falha posterior no wizard mantém a fonte morta em vez de a remover | Flag por importação (param) ou repor no início de cada `runImport/reset`; teste de duas visitas |
| A3 | Média | CONFIRMADO | Nomes de perfil duplicados + omissão fixa "Renato" | `ProfileComponents.kt:144` (`?: "Renato"`) e `:210` (`!nameTaken`); `SetupWizard.kt:145-149` cria sem `takenNames`; `core/setup/SourceImporter.kt:107-118` e `core/profile/ProfileManager.kt:44-53` inserem sem verificar | Nome duplicado quebra o invariante de merge do restore; diálogo abre já em erro com "Renato" existente | Unicidade (trim/lower) no Core com erro; passar `takenNames` ao wizard; omissão vazia + placeholder localizado |
| A4 | Média | CONFIRMADO | Perfil órfão se o onboarding for abandonado | `SetupWizard.kt:147` cria já em `core/setup/SourceImporter.kt:109`; `SetupViewModel.kt:185-191` limpa só estado; `MainActivity.kt:436` reabre first-run | Fica perfil sem fontes e nunca ativado; a próxima tentativa cria outro | Persistir o perfil só no `finish()` (id provisório em memória) ou apagar ao abandonar; retoma abre com `takenNames` |
| A5 | Média | CONFIRMADO | "Re-test" liga pelo nome, não pelo id | `ManageSourcesScreen.kt:376` `sources.firstOrNull { it.name == state.sourceName }`; `SourceTestUi` só transporta o nome (`SettingsViewModel.kt:1782-1797`); nomes repetíveis (`:221` default "My IPTV") | Mede e grava o limite na fonte errada | Incluir `sourceId` no `SourceTestUi` e procurar por id; avisar/renomear duplicados na adição |
| A6 | Média | CONFIRMADO | Editar URL/credenciais não re-sincroniza o conteúdo | `SettingsViewModel.kt:1142` só enfileira com `scopeTurnedOn \|\| lastSyncAt == null`; stream URLs ficam gravados no sync (`core/sync/XtreamSyncer.kt:86`) | Corrigir a password "grava" mas a reprodução continua a falhar até re-sync manual | Detetar mudança de url/user/pass e enfileirar re-sync (ou marcar linha "desatualizada" com ação) |
| A7 | Média-Alta | CONFIRMADO | URLs Xtream não codificam user/password | `core/parser/XtreamClient.kt:444,453,456,458` interpolam `s.username`/`s.password` crus; a API usa `URLEncoder` em `:514-515` | Password com `/ # ? %` ou espaço passa no teste mas todas as URLs falham no sync | Helper único de URL + `URLEncoder` em todos os builders; teste `p@ss/word?x#y` (live/VOD/série/catch-up) |
| A8 | Baixa | CONFIRMADO | URLs sem validação de esquema; EPG sem deteção de duplicados | `AddSourceScreen.kt:384-388` (`isNotBlank` para Xtream/M3U); `EpgSourcesScreen.kt:483`; `EpgSourcesViewModel.kt:65-70` grava sem validar; URL do worker TMDB só é trimada (`MetadataSettingsScreen.kt:547`, `core/settings/SettingsRepository.kt:824-826`) | "asdf"/"http:/" aceites; dois feeds iguais duplicam o guia | Validação comum `http(s)://` sem espaços em fontes/EPG/TMDB; avisar duplicado com sugestão da entrada existente |

---

## D. Home / Live / EPG — novos achados (A1–A12)

| ID | Sev. | Estado | Título | Evidência | Impacto | Solução |
|---|---|---|---|---|---|---|
| A1 | Alta | CONFIRMADO | Busca de filmes/séries sempre vazia | `app/features/search/SearchViewModel.kt:75` força `syncMovies = false, syncSeries = false`; `SearchReader.kt:88,98,124-133` consulta por `movieSourceIds/seriesSourceIds` | Aba de Filmes/Séries e curados (CONTINUAR/NÃO VISTOS) nunca devolvem resultados | Não zerar as flags; derivar ids reais e filtrar pela aba ativa (honra a intenção do comentário em `:73`); teste dos 3 modos |
| A2 | Alta | CONFIRMADO | Personalizar sem seletor de secção | `features/customize/CustomizeViewModel.kt:112` fixa `_section = MediaType.LIVE`; `selectSection` (`:123`) sem call sites; `CustomizeScreen.kt:236-242` só tem botão A-Z (reaplica `ALPHA`) | Ocultar/renomear/reordenar categorias de Filmes/Séries é inalcançável; `currentSort` (`:217`) nunca usado | Reintroduzir abas LIVE/FILMES/SÉRIES ligadas a `selectSection` + alternador A-Z↔Manual refletindo `currentSort` |
| A3 | Média | CONFIRMADO | Refresh da Home salta para o topo | `features/home/HomeScreen.kt:245-259` — efeito com `trendingItems/heroItems/…` nas chaves e `scrollToItem(targetIndex)` onde `targetIndex = 0` quando não há restauro | Atualização de fundo reposiciona a lista e perde a posição | Rolar só em `restoreFocus`/restauração de busca; em updates, manter posição do item focado |
| A4 | Média | CONFIRMADO | "No momento" com relógio congelado | `HomeGuideSlice.kt:301` `now = guide.now`; `core/home/HomeFeed.kt:464` captura `currentTimeMillis()` uma vez por carga | Programa atual/progresso obsoletos; virada de programa não acompanha | Ticker de 30-60 s alinhado ao minuto (ou clock injetável no render) |
| A5 | Baixa | CONFIRMADO | `visibleStartIndex` órfão | `HomeGuideSlice.kt:177,184,294` — clamp só em `selectRow`; `rows.drop(visibleStartIndex)` usa valor antigo se a lista encolher | Cartão pode renderizar vazio até nova navegação | Clampar na recomposição (`coerceAtMost(maxStart)`) e gravar de volta |
| A6 | Média | CONFIRMADO | Foco preso no "No momento" | `HomeGuideSlice.kt:199-207` — com linha ativa, Up/Down devolvem sempre `true` | D-pad não sai da secção nos extremos | Devolver `false`/mover foco quando já no primeiro/último item |
| A7 | Baixa | CONFIRMADO | Reentrada no hero volta ao cartão 0 | `HomeScreen.kt:1184-1191` — `onEnter` faz `scrollToItem(0)` + foco no requester do hero | Perde o cartão focado/expandido | Rolar/focar no índice ativo, preservando por key |
| A8 | Média | CONFIRMADO | Reload reseta o hero e corta o preview | `HomeViewModel.kt:560` `activeHeroIndex = 0` por carga; `:550-552` `stopPreview()` quando o selecionado não é o 1.º da nova lista; `HomeScreen.kt:1127-1136` | Refresh de fundo durante o dwell corta preview e salta ao primeiro cartão | Preservar índice por `heroKey` entre cargas; revalidar por key |
| A9 | Média | CONFIRMADO | Zapping limitado à janela carregada | `LiveScreen.kt:814` passa `channels.itemSnapshotList.items`; `LiveViewModel.kt:1694` `zapList.armFromBrowse(list)`; `LiveZapList.kt:131-139` adota sem hidratar a categoria | Em categorias longas, CH± só percorre o que já foi paginado | Marcar contexto parcial e hidratar a categoria completa em background, ancorado na posição atual |
| A10 | Baixa | HIPÓTESE | Cache de páginas sem limite | `LiveViewModel.kt:932` `MAX_SIZE_UNBOUNDED` + `:969` `cachedIn(viewModelScope)` | Memória cresce com o uso em TVs de pouca RAM | `maxSize` finito (3-5 páginas) ou reter só ids; medir com profiler antes |
| A11 | Baixa | CONFIRMADO | Contador 0 ao agrupar versões | `LiveViewModel.kt:912/936/949/1098-1099` — `groupedCount` nasce `null`, zera em rebuild e só preenche em `onReady`; `grouped ?: 0` | Título mostra 0 canais até a enumeração completa | Mostrar "—"/spinner até `groupedCount`, com `rawCount` de fallback |
| A12 | Baixa | HIPÓTESE | EPG re-chaveia linhas a cada janela | `EpgScreen.kt:901-904` `key(channel.id, profileId, cacheRevision)`; `EpgViewModel.kt:1169` `clearRows()` + `:1177` `cacheRevision++` em `windowChanged` | Avanço de bloco descarta linhas retidas e re-lê visíveis — flicker/reload | Re-chavear por canal/perfil e invalidar por linha, preservando rows do bloco adjacente; medir |

---

## E. HUD / Gravações / Downloads / Legendas — novos achados

| ID | Sev. | Estado | Título | Evidência | Impacto | Solução |
|---|---|---|---|---|---|---|
| HUD-REC | P2 | CONFIRMADO | "Gravar" do HUD falha em silêncio com orçamento esgotado | `app/features/live/LiveViewModel.kt:436` devolve `null` se `canRecordOn(...) !is StreamGrant.Allowed` (o comentário `:380` promete "a refusal is a sentence"); resultado descartado em `:391`; sem canal de erro no shell (`OwnTVShell.kt:1692`) | Botão prime e nada acontece — parece avaria | Propagar o `StreamGrant` até à UI e mostrar a recusa com motivo; teste: limite 0 → mensagem ≤ 1 s |
| DL-DEL-PLAY | P2 | CONFIRMADO | Apagar download não pára a reprodução local | `app/features/downloads/DownloadsViewModel.kt:197` `delete = downloadManager.delete(download)`; contraste com `RecordingsViewModel.kt:278,305` (`hasLocalRecordingPlayback` → `player.stop()`) | Ficheiro removido com o mpv/Exo a lê-lo; resume/`lastPlayedId` apontam para inexistente | Parar + limpar `_lastPlayedId` antes de apagar (assimetria com gravações); teste com mini-player |
| DL-IDPREM | P3 | CONFIRMADO **com nuance** | `_lastPlayedId` escrito antes da verificação Kids | `DownloadsViewModel.kt:141` grava antes do `isDownloadAllowed` (`:146`) em `play()`; **mas** `playExternal` grava depois (`:132-133`) — só metade da alegação original se confirma | No perfil Kids, o regresso foca linha que nunca tocou | Mover a escrita para depois da verificação nos dois fluxos (só falta o `play()`) |
| REC-LABEL | P3 | CONFIRMADO | "Retry"/"Recuperar do arquivo" não reage ao relógio | `RecordingsScreen.kt:515,537` `if (recording.programmeStopMs <= System.currentTimeMillis())` — só muda com nova recomposição | Janela fecha e o rótulo continua a prometer a ação errada | Relógio que tica (`produceState` + `delay(60_000)`); teste da transição sem interação |
| PERF-FMT | P3 | CONFIRMADO | `NumberFormat` por recomposição | `DownloadsScreen.kt:376,472`; `RecordingsScreen.kt:689,797` `NumberFormat.getNumberInstance()` dentro de composables (linhas recompoem ~2 Hz com progresso) | Alocação + resolução de locale a cada frame de progresso | Memorizar formatter por locale (`remember`) ou formatar fora da composição |
| PERF-STOR | P3 | JÁ REPORTADO (R5-B4, site novo) | Barra de espaço recalculada a 2 Hz | `DownloadsViewModel.kt:100-103` (`combine(...).mapLatest { storageInfo() }` reemite com `updateProgress`) + `core/download/DownloadManager.kt:65` (`Dispatchers.IO`) | StatFs 2×/s por download ativo com o ecrã aberto | Desacoplar: recalcular só com mudança de lista/root, com debounce ≥ 10 s (padrão já proposto para gravações) |
| DL-FGS | P3 | JÁ REPORTADO (R4-P3, site novo) | 1º `setForeground` do DownloadWorker sem proteção | `core/download/DownloadWorker.kt:30` sem `runCatching` (os refreshes em `:39` usam) | `ForegroundServiceStartNotAllowedException` mata a drenagem que preserva a fila | Envolver no mesmo `runCatching` + kick no próximo alarme |
| DL-MSG | P3 | JÁ REPORTADO (Ronda 5) | Motivo real da falha descartado antes da UI | `core/download/DownloadEngine.kt:248-249` grava só `FAILED`; `DownloadsScreen.kt:441` mostra mensagem genérica | Quota, disco cheio e cartão removido pedem ações diferentes e dizem o mesmo | Persistir `reason` na entidade e traduzir por motivo (nulo nas linhas antigas) |
| SUB-UNK | P3 | HIPÓTESE | Código de idioma fora da lista aparece como "Nenhum/Automático" | `app/features/settings/VideoPlayerSettingsScreen.kt:403,428,1320` (`LANGUAGE_CODES` fixa; `else -> settings_none_auto`); o valor viaja em backups (`core/settings/SettingsRepository.kt:2723`) | Backup com `pol` faz o ecrã dizer "Nenhum" enquanto a reprodução o prefere | Validar o código na escrita/restauro (ISO-639-2 conhecido) ou mostrar o código cru; teste com `pol` |

---

## F. Core 2ª vaga — novos achados (C01–C12, todos **NCS**)

| ID | Sev. | Estado | Título | Evidência | Impacto | Solução (novo snapshot) |
|---|---|---|---|---|---|---|
| C01 | Média | CONFIRMADO | Short EPG Xtream: base64 descodificado incondicionalmente | `core/parser/XtreamClient.kt:437-438` `Base64.decode(s).getOrDefault(s)` — o fallback só protege input inválido, não input válido-não-base64 | Títulos como `News`/`Test`/`Live` viram mojibake (`9\uFFFD…`) | Só descodificar com verificação de ida-e-volta (re-encode == original) + validador sem U+FFFD; teste de tabela |
| C02 | Média | CONFIRMADO | M3U lido sempre como UTF-8 | `core/parser/M3uParser.kt:85` (`bufferedReader`, UTF-8 com REPLACE) | Playlists Latin-1/Windows-1252 (comuns) corrompem nomes; degrada o matching de EPG | Reutilizar `SubtitleCharset`/juniversalchardet no início do parse; fallback windows-1252; teste ISO-8859-1 |
| C03 | Média | CONFIRMADO | `XmltvParser.readText` sem teto | `core/parser/XmltvParser.kt:111-126` laço sem limite no `StringBuilder` | `<desc>` gigante → OOM no import de EPG | Teto por nó (ex.: 64 KB) continuando a consumir até ao END_TAG; contar truncagens em `ParseMetrics` |
| C04 | Média | HIPÓTESE (condicional) | Deteção de gzip decidida numa leitura de 2 bytes | `XmltvParser.kt:286-293` `read(sig,0,2)` pode devolver 1 (legal); aí um `.gz` segue cru e o import falha todo | Import do guia inteiro falha | Laço até 2 bytes/EOF antes de decidir; teste com stream de 1 byte/chamada |
| C05 | Alta | HIPÓTESE (mecanismo CONFIRMADO) | `DROP INDEX` de bulk sem registo de estado parcial | `core/database/BulkInsertHelper.kt:40-41` (`state.dropped = dropIndexes…` só completa no retorno) e `:101-104` (DROPs autocommit, fora de transação) | Falha a meio (ex.: `database is locked`) deixa índices já removidos **commitados** e `state.dropped == null` → full scans até reiniciar | Construir o `BulkIndexState` **antes** dos DROPs (ou try/catch que restaura); teste injetando exceção no 3.º DROP |
| C06 | Média | CONFIRMADO | Caches por canal do `LiveEpgReader` sem eviction | `core/live/LiveEpgReader.kt:73,78` (`ConcurrentHashMap` só limpos por `clearCache()/invalidate()`; contraste com `historyRows(maximumSize=4)` em `:87`) | Percorrer milhares de canais deixa now/next e rows de todos residentes para sempre | LRU limitado (ex.: 512) ou reutilizar `GuideRequestCache`; limpar em troca de perfil/offset; teste com 5 000 canais |
| C07 | Média | CONFIRMADO | Sem índice de expressão para `LOWER(TRIM(epgChannelId))` | `core/database/dao/ChannelDao.kt:95-96` e `:108-112` (`NOT IN (SELECT DISTINCT epgChannelId …)`); `Index("epgChannelId")` não serve a expressão | Scan completo de `channels` + temp B-tree em **cada** sync de EPG | DDL runtime `CREATE INDEX … ON channels(LOWER(TRIM(epgChannelId)))` (padrão `EpgRepository.ensureEpgIndexes`) + reescrever `NOT IN` como `LEFT JOIN/EXISTS` |
| C08 | Média | HIPÓTESE (hardening) | `UpdateManager` não valida integridade do APK | `core/update/UpdateManager.kt:244-256` valida parse/ABI/packageName apenas; `:135-144` ignora o campo `digest` do release | APK corrompido-mas-parseável ou substituído chega ao instalador | Ler `assets[].digest` (sha256) e comparar antes de `install()`; comparar certificado; `Failure.ChecksumMismatch` |
| C09 | Média | HIPÓTESE (hardening) | Redirect https→http permitido no download do APK | `UpdateManager.kt:168` sobre o `client` injetado (OkHttp segue redirects cross-scheme por omissão) | 302 para `http://` faz o APK descer em claro | Cliente dedicado `followSslRedirects(false)` + validação do host final (`github.com`, `objects.githubusercontent.com`) |
| C10 | Média | CONFIRMADO | Thread de destroy do mpv sem timeout nem daemon | `core/…/player/OwnTVPlayer.kt:4004-4013` `Thread({ … it.destroy() … }, "owntv-mpv-destroy").start()` (não-daemon; sem orçamento) | Se `destroy()` não regressar (core preso em read HTTP), thread+core nativos ficam até o processo morrer; zapping repetido pode acumular | `isDaemon=true` + referência + timeout com diagnóstico; cancelar/registar em `release()` |
| C11 | Baixa | CONFIRMADO | `retirementScope` nunca cancelado em `release()` | `OwnTVPlayer.kt:3882` cria o scope; só `scope` é cancelado em `:4161` e `mpvExecutor` em `:4176` | Jobs de retirement sobrevivem ao release e tentam executar sobre executor encerrado | `retirementScope.cancel()` no release + guarda `if (mpvExecutor.isShutdown) return` |
| C12 | Média | CONFIRMADO | `HttpClient` ignora `Retry-After` no 429 | `core/network/HttpClient.kt:91-100` (429 retryable; backoff 750 ms × attempt, teto 3 s); helper pronto e usado no player (`HttpRetryAfter.kt` ↔ `LivePreviewEngine.kt:3890`) | Painel que responde `429 Retry-After: 120` é martelado 750 ms depois (risco de bloqueio de conta/IP) | `HttpStatusException` transporta o header; backoff `max(RetryAfter, linear)`; teste 429 + `Retry-After: 60` |

---

## G. Melhorias consolidadas da Ronda 7 (26, com critérios de aceite)

| ID | Área | Proposta e solução | Critério de aceite |
|---|---|---|---|
| R7-MELH-01 | Definições | Recuperar os toggles e o chip de painel (S1,S2): renderizar Live/Movies/Séries por secção; `panelWidthCustom = live \|\| movies \|\| series` | Alternar cada um persiste após restart; ligar só Movies muda o chip |
| R7-MELH-02 | Definições | DNS com um único parser e foco seguro (S5–S8): `parseDnsInput()` para Save/Teste/chip; foco no toggle; preset só troca após confirmação | `8.8.8.8:5353` testa na porta 5353; OK acidental não altera servidor próprio |
| R7-MELH-03 | Definições | Backup: nome único + cancelamento + PIN throttled (S9,S10,S11,S18): timestamp; Cancelar com aviso de escrita parcial; backoff e bloqueio de PIN | Dois exports coexistem; cancelar não publica "Done"; 5 PINs errados → 30 s |
| R7-MELH-04 | Definições | Varredura de código morto com guarda de CI (S3,S4,S13,S10-dead): remover blocos; detector para `if (false`, flags nunca atribuídas e parâmetros sem uso | CI verde; código morto novo quebra o build |
| R7-MELH-05 | Definições | Update e chip de versão honestos (S12,S15): estado Downloading com Cancelar explícito; string formatável para versão | Nenhuma saída cancela sem dizer; chip traduzido |
| R7-MELH-06 | Setup | Retomada de submissão (C-A1): snapshot da última tentativa devolvido ao retry; consumir payload só após sucesso | Retry reabre tudo preenchido; payload não se perde |
| R7-MELH-07 | Setup | Handoff de uma só utilização (C-A2): flag por importação reposta no início de cada tentativa | Falha posterior remove a fonte e repõe auto-refresh |
| R7-MELH-08 | Setup | Identidade estável (C-A3,A4,A5): unicidade no Core, `takenNames` no wizard, perfil do onboarding só no fim, Re-test por id | Dois "Renato" impossíveis; retest da 2ª playlist nunca usa a 1ª |
| R7-MELH-09 | Setup | Validação partilhada + codificação (C-A7,A8): helper de URL + `URLEncoder` nos builders; validação `http(s)://` em fontes/EPG/TMDB | `p@ss/word?x#y` reproduz em live/VOD/catch-up; "asdf" recusado |
| R7-MELH-10 | Setup | Coerência pós-edição e limpeza (C-A6 + preferências órfãs): re-sync ao mudar url/credenciais; apagar chaves por fonte | Editar password → re-sync automático; sem chaves `"<id>"` órfãs |
| R7-MELH-11 | Home | Busca facetada (D-A1): ids reais por secção; filtrar por chip/aba | Busca de filme devolve com Movies ON; 3 modos testados |
| R7-MELH-12 | Home | Personalizar completo (D-A2): abas de secção + alternador A-Z↔Manual | Categorias de Filmes/Séries ocultáveis/renomeáveis; `currentSort` refletido |
| R7-MELH-13 | Home | Continuidade de posição (D-A3,A7,A8): só rolar em restauro; hero preservado por key; preview não é cortado | Update de dados não move scroll; dois snapshots de regressão |
| R7-MELH-14 | Home | "No momento" vivo e navegável (D-A4,A5,A6): ticker 30–60 s; clamp na recomposição; Up/Down sai nos extremos | Virada de programa em ≤60 s; nunca vazio; foco sai naturalmente |
| R7-MELH-15 | Live | Zapping íntegro (D-A9,A10,A11): hidratar categoria em background; cache limitada; contador honesto | CH± alcança extremos; memória estável após 20 min |
| R7-MELH-16 | HUD | Feedback de gravação ponta-a-ponta (HUD-REC): propagar `StreamGrant` à UI | Recusa visível ≤1 s; sucesso liga badge ≤1 s |
| R7-MELH-17 | HUD | Paragem segura antes de apagar mídia local (DL-DEL-PLAY, DL-IDPREM): stop + limpar `_lastPlayedId` antes de remover | Nenhuma remoção com reprodução ativa; foco desloca ao vizinho |
| R7-MELH-18 | HUD | Formatação/armazenamento desacoplados do progresso (PERF-FMT, PERF-STOR): formatter memorizado; `storageInfo` com debounce ≥10 s | Zero formatters por recomposição; StatFs ≤1×/10 s |
| R7-MELH-19 | HUD | Rótulos de tempo honestos (REC-LABEL): relógio que tica | Retry↔Recuperar muda ≤60 s sem interação |
| R7-MELH-20 | HUD | Motivo de falha visível (DL-MSG): `reason` na entidade + strings por motivo | Quota/disco/cartão com mensagens distintas; migração preserva antigos |
| R7-MELH-21 | Core (NCS) | Guardas XMLTV (C03,C04): teto por nó; gzip robusto | `<desc>` 5 MB não excede heap; stream de 1 byte deteta gzip |
| R7-MELH-22 | Core (NCS) | Base64 conservador Xtream (C01): ida-e-volta + validador | `News`/`Test`/`Live` intactos; base64 real continua descodificado |
| R7-MELH-23 | Core (NCS) | Índice de expressão EPG (C07): DDL + reescrita `EXISTS` | `EXPLAIN` sem scan/temp B-tree; resultados idênticos |
| R7-MELH-24 | Core (NCS) | Caches limitados `LiveEpgReader` (C06): LRU configurável | 5 000 canais → cache ≤ teto; hit ≥90% no zapping repetido |
| R7-MELH-25 | Core (NCS) | Backoff informado pelo servidor (C12): honrar `Retry-After` em catálogo/EPG | 429+60 s → 2º pedido ≥60 s; 503 sem header mantém linear |
| R7-MELH-26 | Core (NCS) | Integridade do APK de update (A.1.11, C08, C09): checksum do asset + certificado + redirects restritos | Falha dedicada por checksum; redirect cross-scheme recusado |

---

## H. Inovações consolidadas da Ronda 7 (15)

| ID | Ideia | Área | Valor | Esforço | Caminho |
|---|---|---|---|---|---|
| I-01 | Rascunho partilhado TV↔telemóvel | Setup | Continuar importação falhada no telemóvel (QR), sem redigitar | M | Endpoint no `core/companion` + rascunho em memória (TTL, password nunca persistida) |
| I-02 | "Por que falhou?" — diagnóstico guiado | Setup | Checklist DNS→host→API→canal→HLS com ações de um toque | M | `SourceDiagnosticsViewModel` reutilizando tester/`XtreamClient`/`StalkerAuthManager` |
| I-03 | Importação inicial retomável | Setup | Primeira importação como trabalho persistente com notificação na TV | M | `SourceImporter.runImport` num `CoroutineWorker`; `SetupViewModel` observa |
| I-04 | "Continuar no direto" | Home | Retomar canal e posição onde parou, integrável ao catch-up | M | `recentlyWatched` + posição no `CorePlayer`; card na Home |
| I-05 | "Descobrir no Guia" | EPG | Recomendações das próximas 2 h pontuadas localmente, com "porquê" | M | Scoring on-device na janela carregada; reutiliza lembretes |
| I-06 | "Mesmo programa noutro canal" | EPG | Outras emissões/alternativas com salto em 1 passo | M | Índice normalizado (título+faixa) no DAO de EPG + painel de detalhe |
| I-07 | Badge de gravação acionável com estimativa | HUD | Long-press pára; estimativa cruza bitrate × duração × orçamento | M | `PlayerHud` → `togglePlayerRecording` → `RecordingManager`; diálogo simples |
| I-08 | Recuperação guiada de downloads | Downloads | Falhas agrupadas por motivo com ações em lote | S-M | `reason` na entidade + grupos `FAILED` + `StorageBrowser` |
| I-09 | "Ver desde o início" ao parar gravação | HUD | Parar passa a oferecer o ficheiro com marcador de tempo | S | `openInProgress` já existe; botão no HUD + WatchNext |
| I-10 | "Testar tudo" no hub de Settings | Definições | Diagnóstico de configuração em 1 toque (DNS, proxy, metadata, fontes) | M | `SettingsSelfCheckViewModel` agregando estados existentes |
| I-11 | Perfis de configuração por QR/LAN | Definições | Transferir subconjunto de settings entre TVs sem credenciais | M | `CompanionKeyDialog` + `BackupManager.Section.SETTINGS` + diff/hash |
| I-12 | "Desfazer" de configurações destrutivas | Definições | Toast "DNS alterado — Desfazer" durante ~10 s | S-M | Wrapper de escrita no VM + snapshot em DataStore |
| I-13 | Diagnóstico de EPG "por que este canal não tem guia" | Core | Acaba com a classe de bugs "guia em branco" | M | Instrumentar `channelFilterMiss/Hit`, `guideIdsInWindow` → `Flow<GuideHealth>` |
| I-14 | Retenção do EPG por orçamento em MB | Core | Evita crescimento sem controle do ficheiro de guia | M | Migração `auto_vacuum=INCREMENTAL` + worker de compactação fora de uso |
| I-15 | Telemetria local de falhas de rede por painel | Core | Alimenta o `AdaptivePortalLimiter` antes do bloqueio de conta | M | Contadores por origem a partir de `HttpStatusException`/`Retry-After` |

---

## I. Veredictos do Council sobre a Ronda 7

**Aceites integralmente (verificados por leitura direta):** S1, S2, S3, S5, S6, S9, S10 (caminho morto confirmado em `BackupScreen.kt:285`), S11, S16, S17, S18; C-A1, C-A2, C-A5, C-A6, C-A7, C-A8; D-A1, D-A2, D-A3, D-A4, D-A5, D-A8, D-A9, D-A11; HUD-REC, DL-DEL-PLAY, REC-LABEL, PERF-FMT; C01, C03, C05, C06, C07, C10, C11, C12; V-02.

**Aceites com nuance:**
- **DL-IDPREM (parcial)** — `playExternal` grava o id **depois** da verificação Kids (`DownloadsViewModel.kt:132-133`); só `play()` grava antes (`:141` vs `:146`). Corrigir apenas esse fluxo.
- **S5** — o mecanismo exato é: ao reabrir, o foco cai no primeiro *preset* (`:112-119`) e um Enter ativa-o gravando de imediato (`:174-192`); o botão "Guardar" em si não sobrescreve.
- **C-A2** — o alcance real é limitado à instância `factory` do `SetupViewModel` (`core/di/DataModule.kt:301`); o efeito prático é o documentado (fonte morta mantida numa visita posterior do wizard).
- **C04** — mecanismo legal mas raro; aceite como correção defensiva barata.
- **C05** — mecanismo confirmado (DROPs autocommit, atribuição só no retorno); o gatilho (`database is locked`/exceção a meio) é raro — aceite com teste que injeta a falha.
- **V-01** — o comportamento é coalescência deliberada (`GuideLoadGate`); só a promessa/nome "prewarm" induz. Renomear/documentar ou implementar carga em background.
- **V-03** — o flag só fica preso quando nenhum estado volta a mudar; robustez P3.
- **S14** — mantém-se HIPÓTESE até alguém demonstrar um caso real de chave inválida aceite com consequência.

**Hipóteses a medir antes de otimizar:** D-A10, D-A12, SUB-UNK, C04 (frequência), C05 (gatilho). **Nada de novo foi refutado** nesta ronda; as refutações anteriores (token do timeshift, `loadItem`, backdrop blur, três pedidos por rajada) permanecem válidas.

---

## J. Reavaliação global consolidada — Rondas 1 a 7

Nenhum ficheiro de código foi alterado desde a Ronda 6, logo **todos os itens abertos das Rondas 1–6 continuam válidos** e juntam-se os novos da Ronda 7. Esta secção é o índice único com uma solução por item; a evidência detalhada está nas secções acima e no corpo da Ronda 6.

### J.1 Correções das Rondas 1–6 ainda abertas (solução resumida)

| ID/Origem | Sev. | Item | Solução |
|---|---|---|---|
| R6 §3 | P1 | Release sem keystore cai para assinatura debug | Gate fail-closed: sem keystore de produção, o job de release falha antes de gerar artefactos |
| R6 §3 | P1 | Parser de tag de 3 componentes + `versionCode` manual | `version.properties` como fonte única; parser aceita 4 componentes e valida monotonicidade |
| R6 §3 | P1 | Caminho `E:/MEGA/...` hardcoded | Mover para propriedade/env do Gradle com default local |
| R6 §3 | P1 | APK universal; x86_64 por `ls … head -n1` | Publicar splits por ABI; escolher o asset com verificação (`test -f`, checksum) |
| R6 §3 | P2 | Notas de release vazias | Extrair do `CHANGELOG_APP.md` no workflow (secção `v105.*`) |
| R6 §3 | P2 | `usesCleartextTraffic=true` global | Restringir por domínio via network security config |
| R6 §3 | P2 | FileProvider com raízes `"."` | Estreitar para `files/recordings`, `files/downloads`, etc. |
| R6 §3 | P2 | Chave TMDB/edge em BuildConfig | Servir metadados por proxy próprio; rodar a chave exposta |
| R6 §3 | P1 | LocalSync HTTP + segredo no header | TLS efémero com fingerprint trocado no emparelhamento (ou chave de sessão); ver MELH-03 da R6 |
| R6 §3 | P2 | PBKDF2 do backup sem teto de iterações | Validar iterações/limites antes de derivar; rejeitar valores absurdos |
| R6 §3 | P1 | Kids: adicionar/alargar perfil sem credencial | Credencial adulta independente para criar/editar perfis |
| R6 §3 | P1 | Kids: categoria desconhecida permitida | Negar por omissão com exceção explícita para adulto |
| R6 §3 | P2 | Downloads: separador inerte | Implementar abas reais ligadas ao VM **ou** remover e renomear para "Gravações" |
| R6 §3 | P2 | Home só live (`liveOnly=true`) | Decisão de produto: ativar VOD com medição ou remover o que não renderiza |
| R6 §3 | P3 | Pesquisa sem entrada na navegação | Entrada no TopBar (ou aceitar o atalho remoto como decisão e documentar) |
| R6 §3 | P2 | i18n do módulo app fora do pipeline | Incluir `app/src/main/res` no gen/validate; completar `pt`/`pt-rPT` |
| R6 §3 | P3 | Rotas do hub More duplicadas/mortas | Uma porta por destino; remover enums/ramos sem uso |
| R6-PLY-01 | P3 | Atalho de histórico do HUD morto | Ligar `onOpenHistoryList` no shell ou remover o parâmetro/ramo |
| R6-PLY-02 | P2 | Multiview fora da autoridade de foco de áudio | Registar o tile audível no `PlaybackSession`; pausar/duck no focus loss |
| R6-PLY-03 | P3 | `PlaybackSession` sem detach ao sair do shell | `DisposableEffect { onDispose { attach(null) } }` + reset de `playerMode` |
| R6-PLY-04 | P2 | Catch-up/rewind sem drenar o Exo | Reusar `stopAndAwaitDrain()/stopAndAwaitRelease()` nesses arranques |
| R6-DAT-01 | P1/P2 | `userData` sem `SOURCES` mantém `src` cru | Exportar identidade estável ou proibir `userData` sem Fontes; quarentena de ids no import |
| R6-DAT-02 | P2 | `resolvePending()` sem lock partilhado | Mutex/actor único + merge por chaves (remover só resolvidos) |
| R6-DAT-03 | P2 | Histórico gravado antes de saber se tocou | Mover `record` para dentro do coroutine, após sucesso, com o mesmo `pid` |
| R6-DAT-04 | P2 | Contentor LocalSync descartado em silêncio | Mapear para `BadPayload`, erro visível, quarentena e 4xx ao remetente |
| R6-DAT-05 | P2/P3 | `incoming-*.own` nunca apagados | Apagar no sucesso/`onCleared` + purga TTL no arranque |
| R6-DAT-06 | P3 | Último canal Live sem validar perfil | Guardar `(profileId, sourceId, remoteId)` e validar visibilidade antes de publicar |
| R6-CORE-01 | P2 | Resposta do sync local sem limite | Orçamento `min(Content-Length, 32 MB)` + `copyTo` limitado (NCS) |
| R6-CORE-02 | P2 | Fast-path Stalker aceita dump parcial | Exigir `items.size == totalItems`; nunca podar sem verificação (NCS) |
| R6-REL-01 | P2 | Asset x86_64 listado sempre | Montar `files:` a partir de ficheiros verificados; falhar a tag se faltar |
| R6-REL-02 | P2 | Actions sem pin com token de escrita | Fixar SHAs, permissões mínimas, `environment: release` com reviewer |
| R6-REL-03 | P3 | Activity de QA exportada em debug | `android:exported="false"` + gate que impede APK debug em release |
| R6-UX-01 | P2 | Sidebar/avatar sem nome acessível | `semantics { contentDescription = …; role = Tab }`; avatar "Perfil: nome" |
| R6-UX-02 | P2 | 20 Toasts de sistema vs. canal in-app | Host único no shell (fila, duração mínima, OK para dispensar, `liveRegion`) |
| R6-UX-03 | P2 | Chips da Pesquisa só CANAIS | Iterar os três `SearchIntent` |
| R6-UX-04 | P3 | Texto abaixo do mínimo legível | Piso 12 sp para informativo; validar a 720p |
| R6-UX-05 | P2 | Separador de Downloads inerte | Igual ao item R6 §3 correspondente (implementar ou remover) |

### J.2 Correções da Ronda 7 (solução de uma linha; evidência nas secções A–F)

| ID | Sev. | Solução |
|---|---|---|
| S1 | Alta | Renderizar os 6 toggles agrupados por secção em `BrowsingListsDialog` |
| S2 | Média | `panelWidthCustom = live \|\| movies \|\| series` |
| S3 | Alta | Ligar o layout VOD a uma linha real ou remover o diálogo morto |
| S4 | Baixa | Remover o picker morto (`showCategories`) |
| S5 | Alta | Focar o toggle ao abrir; preset exige confirmação com servidor próprio + Desfazer |
| S6 | Média | Reutilizar o parser do Save no teste DNS (porta real) |
| S7 | Média | `parseDnsInput()` com IPv6 `[v6]:porta`, 1–65535 e validação de DoH |
| S8 | Média | Persistir o ligar do toggle (ou marcá-lo "não guardado") |
| S9 | Média | Nome de export com timestamp ou confirmação de sobrescrita |
| S10 | Média | Botão Cancelar com aviso de escrita parcial + estado visível fora do ecrã |
| S11 | Alta | Throttling persistido do PIN (contador + backoff + bloqueio temporário) |
| S12 | Baixa/Média | Estado Downloading com Cancelar explícito e aviso de saída |
| S13 | Baixa | Remover o bloco `if (false && …)` |
| S14 | Baixa | Validar chave (32 hex) e URL https do TMDB com erro inline |
| S15 | Baixa | Strings recursivas + `contentDescription`; remover `"v"` cru |
| S16 | Média | Desenhar os 4 scopes do histórico a partir do enum |
| S17 | Média | Desligar o DNS só grava `enabled=false` (manter host/porta/DoH) |
| S18 | Média | Campo "repetir password" no export do backup |
| C-A1 | Média | Snapshot da tentativa falhada devolvido no retry; payload só consumido após sucesso |
| C-A2 | Média | `backgroundHandoff` por importação (ou reposto em `runImport/reset`) |
| C-A3 | Média | Unicidade de nomes no Core + `takenNames` no wizard + placeholder localizado |
| C-A4 | Média | Persistir o perfil do onboarding só no fim (ou apagar se abandonado) |
| C-A5 | Média | Re-test por `sourceId` no `SourceTestUi` |
| C-A6 | Média | Re-sync automático quando url/credenciais mudam |
| C-A7 | Média-Alta | `URLEncoder` nos builders de URL Xtream (helper único) |
| C-A8 | Baixa | Validação `http(s)://` comum + aviso de EPG duplicado |
| D-A1 | Alta | Não zerar `syncMovies/syncSeries`; derivar ids reais e filtrar pela aba |
| D-A2 | Alta | Abas LIVE/FILMES/SÉRIES ligadas a `selectSection` + alternador de sort |
| D-A3 | Média | Rolar para o topo apenas em restauro de foco |
| D-A4 | Média | Ticker de 30–60 s para o "No momento" |
| D-A5 | Baixa | Clampar `visibleStartIndex` na recomposição |
| D-A6 | Média | Up/Down devolvem `false` nos extremos da secção |
| D-A7 | Baixa | Reentrada no hero preserva o cartão focado (por key) |
| D-A8 | Média | Preservar `activeHeroIndex` por `heroKey`; não cortar preview |
| D-A9 | Média | Hidratar a categoria completa em background para o zapping |
| D-A10 | Baixa | `maxSize` finito no cache de páginas (medir antes) |
| D-A11 | Baixa | Contador "—"/spinner até `groupedCount` |
| D-A12 | Baixa | Re-chavear por canal; invalidar por linha (medir antes) |
| HUD-REC | P2 | Propagar `StreamGrant` e mostrar a recusa no HUD |
| DL-DEL-PLAY | P2 | Parar reprodução e limpar `_lastPlayedId` antes de apagar o ficheiro |
| DL-IDPREM | P3 | Mover a escrita do id para depois da verificação Kids em `play()` |
| REC-LABEL | P3 | Rótulo derivado de relógio que tica |
| PERF-FMT | P3 | Formatter memorizado por locale |
| PERF-STOR | P3 | `storageInfo()` desacoplado com debounce ≥10 s |
| DL-FGS | P3 | `runCatching` no primeiro `setForeground` + kick de seguimento |
| DL-MSG | P3 | Persistir `reason` e traduzir por motivo |
| SUB-UNK | P3 | Validar código de idioma na escrita/restauro (teste com `pol`) |
| C01 | Média | Base64 só com verificação de ida-e-volta (NCS) |
| C02 | Média | Deteção de charset no M3U (NCS) |
| C03 | Média | Teto por nó de texto no XMLTV (NCS) |
| C04 | Média | Leitura robusta dos 2 bytes de gzip (NCS) |
| C05 | Alta | `BulkIndexState` construído antes dos DROPs ou try/catch que restaura (NCS) |
| C06 | Média | LRU nos caches do `LiveEpgReader` (NCS) |
| C07 | Média | Índice `LOWER(TRIM(epgChannelId))` + `EXISTS` (NCS) |
| C08 | Média | SHA-256 do asset + certificado antes de instalar (NCS) |
| C09 | Média | Update só https e hosts esperados (NCS) |
| C10 | Média | Thread de destroy daemonificada com orçamento e diagnóstico (NCS) |
| C11 | Baixa | Cancelar `retirementScope` no `release()` + guarda de shutdown (NCS) |
| C12 | Média | Honrar `Retry-After` no 429 (NCS) |
| A.1.1 | P3 | Unificar espaço de índices (linha vs. item) nas listas com cabeçalhos |
| A.1.2 | P3 | Limpar a pesquisa do EPG ao desativar o guia |
| A.1.3 | P3 | Substituir o busy-wait de 20 Hz por sinalização de eventos |
| A.1.4 | P3 | Deep link só em criação nova; limpar intenção após consumo |
| A.1.5 | P3 | Mapa id→índice no overlay com fallback explícito |
| A.1.6 | P3 | Progresso do press via `withFrameNanos` |
| A.1.7 | P3 | '&' sem '=' continua o valor anterior no M3U |
| A.1.8 | P2/P3 | Não limpar `KEEP_SCREEN_ON` em `isChangingConfigurations` |
| A.1.9 | P3 | `onRestored()` no caminho vazio + `lastPlayedId` nas chaves |
| A.1.10 | P3 | Re-lançar `CancellationException` do chamador |
| A.1.11 | P2 | Checksum + certificado no `UpdateManager` (NCS) |
| V-01 | P3 | Definir "prewarm" real ou renomear; documentar coalescência |
| V-02 | P3 | Validar/escapar ids Xtream nas chamadas de API (NCS) |
| V-03 | P3 | `onRestored()` de fallback no EPG |
| V-04 | P3 | idem A.1.9 |

### J.3 Melhorias das Rondas 1–6 ainda válidas (solução resumida)

| ID | Solução |
|---|---|
| MELH-01 | Gate fail-closed de release + versão única (parser 4 componentes, `version.properties`) |
| MELH-02 | Fechar os três caminhos Kids (credencial adulta, categoria desconhecida negada, `isKids`/PIN no restauro) |
| MELH-03 | LocalSync: TLS efémero/fingerprint, revogação imediata, apagar `incoming-*`, limites antes de ler |
| MELH-04 | Backup: identidade de fonte sempre (ou `userData` proibido sem `SOURCES`); validar KDF; exportação incompleta visível |
| MELH-05 | Downloads com identidade única `(profile, source, tipo, id)`; `416 → COMPLETED` só com prova de tamanho/hash |
| MELH-06 | Multiview sob `PlaybackSession`; detach no teardown; drain no catch-up; `cleanupScope` cancelado |
| MELH-07 | Canal único de feedback no shell (fila, duração mínima, `liveRegion`); migrar 20 Toasts |
| MELH-08 | Performance só depois de traçar (flows do Settings, ticker do hero, N+1, projeção do EPG) |
| MELH-09 | Incluir `app/src/main/res` no pipeline i18n; completar `pt`/`pt-rPT`; selo com união App+Core |
| MELH-10 | Uma porta por destino (Pesquisa no TopBar; Downloads coerente; linhas do More suportadas ou removidas) |

**Novas melhorias da Ronda 7:** as 26 de G — todas válidas, com critérios de aceite.

### J.4 Ideias das Rondas 1–6 ainda válidas

| Ideia | Solução/caminho (resumo) |
|---|---|
| Cofre de Catch-up (flagship) | Badge "expira em X" + "Guardar até ver" → gravação local; regras por série; superfície "Cofre" na Home |
| "Só o que tenho" (offline automático) | Home condicional por conectividade com gravações/downloads + banner honesto |
| Piloto automático offline | `OfflineAutopilotWorker` estilo `AutomaticBackupWorker` com regras por perfil |
| Lista branca Kids por perfil | `ProfileContentPolicy` no Core + UI em Perfis + filtros nas superfícies + gate de reprodução |
| Pedido parental "Pode ver?" | `WatchRequestStore` + caixa de entrada; PIN nunca sai do dispositivo |
| Guia por áudio + zapping falado | Observar foco no `GuideCore`/`LiveScreen`; fila TTS cancelável; toggle em Acessibilidade |
| Biblioteca portátil (trocar de fornecedor) | `SourceMigrationMatcher` por `tvg-id`+título normalizado (+TMDB) com pré-visualização |
| Pesquisa com ações | Campo único com Ver/Desde o início/Guardar/Programar; filtros Kids/ocultos primeiro |
| Modo desporto sem spoilers | Máscara no guia/lembretes + "começar do início" via catch-up; toggle por perfil |
| Pré-aquecimento preditivo de zapping | Deck mudo com `LiveEnginePool` **se** o orçamento de sessões do fornecedor permitir (medir) |

**Novas ideias da Ronda 7:** as 15 de H (I-01 a I-15) — todas registadas como propostas, nenhuma é correção.

### J.5 Complemento ao roadmap (delta da Ronda 7)

| Fase | Acrescentar |
|---|---|
| Fase 1 — Distribuição e proteção | S11 (lockout de PIN); A.1.11/C08/C09 (integridade do APK); S5/S17 (DNS não destrutivo) |
| Fase 2 — Dados e rede | S9/S10/S18 (backup); C-A1/A3/A4/A5/A6/A7/A8; Core C01–C07, C12 e V-02 (NCS); A.1.7 |
| Fase 3 — Reprodução e UX | HUD-REC, DL-DEL-PLAY, DL-IDPREM, REC-LABEL, PERF-FMT/STOR, DL-FGS, DL-MSG, SUB-UNK; D-A1…A9; S1–S3, S6–S8, S13–S16; A.1.1–A.1.6, A.1.8–A.1.10; V-01, V-03, V-04 |
| Fase 4 — Produto | Escolher prioridades em H (sugestão: I-04, I-07, I-08, I-10, I-13) |

### J.6 Testes a acrescentar (Ronda 7)

Conta Xtream com `p@ss/word?x#y` (live/VOD/série/catch-up); listas com cabeçalhos (índices de contexto em Gravações/Downloads); 429 + `Retry-After: 60` no catálogo/EPG; short EPG com títulos `News`/`Test`/`Live`; M3U acentuado em ISO-8859-1; `<desc>` de 5 MB; stream gzip que devolve 1 byte por chamada; `DROP INDEX` com exceção injetada no 3.º índice; PIN errado 5× → espera de 30 s; dois exports de backup na mesma pasta coexistem; criar dois perfis com o mesmo nome (recusado); retry de fonte falhada reabre preenchido; deep link após rotação não re-despacha.

### J.7 Encerramento

- A Ronda 7 não encontrou nenhuma regressão catastrófica nova; encontrou **muitos itens de acessibilidade funcional** (features implementadas mas inalcançáveis: S1, S3, D-A1, D-A2, V-01) — que tendem a passar despercebidos porque não produzem erros.
- Os P1 de release/Kids/backup/LocalSync das rondas anteriores **continuam a ser a prioridade máxima**; os novos P1/P2 (S5, S11, HUD-REC, C05, D-A1) seguem-se.
- Itens **NCS** exigem novo snapshot do Core com os mesmos critérios de integridade (`core-source-lock.json` + checksum) já descritos na secção 11.
- Revisão estática: sem builds, sem box física; hipóteses identificadas nas secções A.1, D (A10/A12), E (SUB-UNK) e F (C04/C05) só devem virar trabalho após o teste indicado. **Este anexo não autoriza implementação.**
- Relatórios brutos dos sete agentes foram consolidados neste anexo.
