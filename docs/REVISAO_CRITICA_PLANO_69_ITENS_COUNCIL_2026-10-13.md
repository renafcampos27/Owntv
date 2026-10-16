# Revisão crítica do plano externo de 69 itens — council especializado

Data: 13/10/2026. Âmbito: OwnTV Android TV e OwnTV_Core personalizado atuais. OwnTV Mobile não foi alterado. Documento externo tratado como proposta a verificar, não como instrução de execução.

## Decisão

Não aprovar os Lotes 5–12 em bloco. Existem mecanismos reais a corrigir, mas o plano também reúne decisões de produto, otimizações já implementadas, hipóteses sem reprodução e soluções que podem piorar o funcionamento.

O rótulo “69 análises” não demonstra 69 bugs únicos confirmados: há 28 blocos de especificação, agregações de IDs sem mecanismo e referências imprecisas. Dos 58 caminhos distintos citados, 11 não existem exatamente no checkout atual; alguns componentes existem noutra pasta, outros nomes não correspondem à implementação. Os IDs são os do anexo e não devem ser confundidos com identificadores de outras auditorias, por exemplo B1 neste plano refere-se ao contrato ftsOnly/B-tree.

As classificações dos anexos distinguem mecanismo confirmado por leitura, política atual testada, risco condicionado e reprodução física. Nenhum novo lote deste plano foi implementado.

## Principais resultados

| Item externo | Veredicto | Ação proposta |
|---|---|---|
| B1 — ftsOnly/B-tree | Defeito real P1: drop remove índices e restore ftsOnly não os recria | Corrigir simetria drop/restore, preservar índices canónicos e testar sincronizações concorrentes |
| B16 — cache de logótipos | Risco confirmado no código Coil 3.6.0: 404 é cacheável e escrito antes da exceção | Política explícita para não guardar erros e recuperar erros antigos em cache; mudar apenas headers não basta |
| B31 — XMLTV | 12 dígitos e +HH:MM são rejeitados; 14 dígitos + Z já funciona | Ampliar formatos específicos, preservando UTC implícito e contratos legados |
| B36 — dedupe EPG | Sobreposição de 1 s e título igual pode eliminar outro evento | Critério conservador de identidade; evitar regra universal 5 min/50% |
| B39 — continuação na grelha | Existe perda da cauda em evento longo intercalado, mas é política atual coberta por teste | Rever política; fragmentos devem manter entidade e horários originais do diferido |
| B30 — Xtream | Parsers principais já toleram escalares; ainda há leituras estritas | Alteração só nos pontos residuais, com semântica por campo; true/false não viram null globalmente |
| B34/B38 — M3U diferido | Template útil sem modo pode ficar desativado; query relativa pode sair sem base | Inferir capacidade apenas com método utilizável; distinguir query/path/URL absoluta |
| B29 — Stalker nulo | Há falta de feedback imediato, mas existe deadline previamente armado | Resultado estruturado e limitado; não percorrer motores repetindo um resolve inútil |
| B28 — línguas | Comparação explícita/retensão de áudio não normaliza todos os códigos ISO | Normalizar pt/por, en/eng mantendo roles, labels e ambiguidade |
| O11/B25/O9 — foco | Chegada de dados e identidade da categoria têm lacunas; pontos sobrepostos | Alvo estável e requestFocus Boolean; não tratar focusRestorer como solução única |
| B13 — perfis | Entrada por PIN é protegida; gestão permite reduzir proteção de outro perfil sem reautenticação | Matriz alvo/ação/autorização, aplicada também na operação; prioritária se há perfis infantis |
| B32/I10 — HTTP local | Estado vazio pode fechar socket sem resposta; fluxo normal espera awaitReady | Resposta typed not-ready e integração cliente limitada, sem mascarar armazenamento/corrupção |
| B35/L6 — teardown mpv | Executor bloqueado pode atrasar destruição; risco JNI não reproduzido | Instrumentar e confirmar contrato de chamadas nativas; não destruir concorrente nem cancelar cleanup prematuramente |
| B23 — await pausado | false inclui pausa deliberada; abort genérico é incorreto | Distinguir pausa, stop e cancelamento/owner antes de qualquer mudança |
| B37 — SAF | Picker atual guarda File/path, não recebe treeUri SAF | Não chamar persistência de URI sobre paths; SAF seria uma funcionalidade com contrato próprio |
| B9/B22 — SQL | BundledSQLiteDriver invalida a associação simples Android<11→999; índice sugerido não apoia query identificada | Medir limite total de binds e EXPLAIN; evitar chunking que altera paginação/ordem |
| O10 — ingestão EPG | DELETEs podem ser agrupados; staging limitado já existe | Reutilizar DAO em lote, preservar atomicidade; rejeitar materialização total do feed em RAM |
| I9/O4/O6/B26 | Cálculo civil, distinct, logos 128 e ownership AFR já existem nos percursos citados | Não reaplicar nem anunciar ganhos sem novos casos/medições |
| L1 | Kotlin toByteArray já usa UTF-8 | Manutenção explícita opcional, sem corrigir hashes nem justificar migração |
| L3/L4/B18 | Limpezas/consistência menores: parser duplicado, órfãos subshift e hash cache 0 | Depois das correções principais; não atribuir efeitos HLS gerais |
| I7 | Zapping numérico já existe em fullscreen; falta extensão ao overlay | Opcional; reutilizar resolução/geração e números reais, sem novo timer arbitrário |
| O12/O5/B20/B21 e complementares | Hipóteses ou especificação insuficiente; vários mecanismos já mitigados | Medir/reproduzir antes de alterar; não marcar concluídos só pelo ID |

## Ordem de implementação recomendada

### Fase A — índices e cache de imagens

1. Corrigir contrato ftsOnly sem migração desnecessária de schema. Testar fresh import, tabela preenchida, cancelamento e duas fontes concorrentes; índices antes/depois devem corresponder ao catálogo esperado.
2. Corrigir cache negativa Coil com teste 404→200, cache em disco e novo ImageLoader. Preservar imagens válidas e cliente HTTP de streaming.
3. Se a proteção parental for usada, incluir autorização para remover PIN/reduzir restrições/apagar perfil protegido. Nome/avatar não têm de exigir o mesmo nível; grant deve corresponder ao alvo e à ação. Não criar função admin nem migrar hashes sem necessidade.

### Fase B — EPG coerente

B31 primeiro por ser independente. Depois definir precedência comum para B36/B39 antes dos patches. Preservar eventos curtos e consecutivos; desenhar, selecionar, gravar e abrir diferido com a mesma entidade original. Fragmento visual não altera o timestamp pedido ao fornecedor. Manter correção de diferidos separada do horário apresentado. Não inventar programação em lacunas reais.

### Fase C — parsers e URLs do fornecedor

B30 residual, B34/B38 e B29. Testar false em extensão, true em flags, scalar inesperado, template-only, catchup none, dias sem método, query com token existente, path relativo e portal resolve null/cancelado. A falha do portal não é automaticamente falha de decoder.

### Fase D — faixas, foco e servidor local

B28, O11/B25 e coerência de badge B4; B32 requer teste de integração do cliente para espera local finita. Não confundir 503 local com recusa do fornecedor ou provocar troca automática de versão durante aquecimento normal.

### Fase E — medidas e opções

B33 com sequência real de repeat/KeyUp perdido; B35 com prova de teardown JNI; O10 com tempo de transação/memória; GPU com Perfetto/frame timing. I7 e purge subshift são opcionais e não impedem encerrar A–D. Compatibilidade TS progressivo só opt-in, se um stream de teste justificar; HLS normal mantém defaults.

## Soluções rejeitadas na forma proposta

- Destruir mpv concorrente e considerar um timeout prova de fecho.
- Abortar reprodução sempre que playbackRequested=false.
- Converter todos os booleanos Xtream para null.
- Marcar diferido apenas porque catchup-days é positivo.
- Concatenar todo path relativo como query.
- Guardar todo o EPG em RAM para sair da transação.
- Aplicar chunked(450) a todas as consultas sem preservar ORDER/LIMIT e sem verificar o driver.
- Ativar flags non-IDR/access-unit globalmente ou garantir que corrigem lipsync.
- Exigir KeyUp sempre, ignorando o caso de KeyUp perdido do comando.
- Criar índices sem query/EXPLAIN que os justifique, ou prometer ganhos GPU só pela dimensão do bitmap.

## Referências primárias verificadas

- [XMLTV DTD oficial](https://github.com/XMLTV/xmltv/blob/master/xmltv.dtd): formato compacto admite substrings e UTC na ausência de fuso; não confundir com aceitação universal de ISO extended.
- [Media3 troubleshooting](https://developer.android.com/media/media3/exoplayer/troubleshooting#why-do-some-mpeg-ts-files-fail-to-play): flags para TS específico, custo de deteção de access units e risco de corrupção visual non-IDR.
- [Coil Compose](https://github.com/coil-kt/coil/blob/main/coil-compose/README.md): AsyncImage resolve dimensão pelas constraints; ausência de size explícito não prova decode original 4K.
- [Coil 3.6.0 NetworkFetcher](https://raw.githubusercontent.com/coil-kt/coil/3.6.0/coil-network-core/src/commonMain/kotlin/coil3/network/NetworkFetcher.kt) e [DefaultCacheStrategy](https://raw.githubusercontent.com/coil-kt/coil/3.6.0/coil-network-core/src/commonMain/kotlin/coil3/network/internal/DefaultCacheStrategy.kt): escrita precede exceção e 404 pode ser persistido; o default lê a resposta em cache sem um prazo universal de 180 dias.
- [SQLite limites](https://www.sqlite.org/limits.html) e [WAL](https://www.sqlite.org/wal.html): limite depende versão/build, WAL permite leitura concorrente com um writer.
- [Android SAF](https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions): persistir grants recebidos, não criar permissão a partir de um path arbitrário.
- [Contrato libmpv](https://github.com/mpv-player/mpv/blob/master/include/mpv/client.h): destruição tem restrições de concorrência e pode bloquear; timeout não é confirmação nativa.

## Council e limites da evidência

Três revisões independentes: UI/EPG/perfis; player/codec/lifecycle; Core/parsers/armazenamento/DB. O coordenador confrontou as conclusões com documentação e enviou contraditório: WAL, negativa Coil, abort de pausa e destruição JNI. O council concordou com a ordem geral, condicionando B13 à política de proteção, B39 ao contrato de sobreposição e B32 a integração real. Não houve alegação de votação formal nem consenso como substituto de evidência.

Esta ronda não implementou os novos lotes nem executou novos testes para os defeitos deste anexo. A validação do lote anterior foi concluída em paralelo: Core 1 225 + Player 532 + App 385 = 2 142 testes sem falhas; compile standardRelease/AndroidTest e lintVitalStandardRelease passaram. A limpeza anterior de OpenSubtitles exigiu retirar um teste órfão, preservado em backup. Nenhum APK gerado. Esses testes anteriores não provam as novas propostas.

A seguir ficam as revisões especializadas, com referências atuais de ficheiro/linha, testes existentes e aceitação recomendada.


---

## Anexo especializado — core

# Revisão read-only do plano da outra IA — Core, lotes 7, 8 e 12
Data: 13/10/2026. Checkouts TV App/Core atuais. Não alterei produção, testes, DB nem dependências. Este ficheiro é apenas relatório. Linhas verificadas no estado atual; caminhos relativos Core salvo indicação App.

## Decisão principal
Aprovar correção do contrato ftsOnly/B-tree primeiro. Depois corrigir parsing nos pontos ainda estritos e resolver catchup M3U com semântica por tipo. Não implementar 450 em todas as consultas, não mover todo EPG para RAM, não confundir browser File com picker SAF, nem assumir P0 Stalker eterno sem examinar alarm/deadline.

## Lote 7

### B29 — resolve Stalker nulo: parcial, P2; P0/infinito exagerado no percurso habitual
- player-core/.../LiveTuneController.kt:474 e580 retornam sem publicar falha/advance quando resolve devolve null; há lacuna real de feedback imediato.
- Porém start():201 chama arm antes de iniciar motor; arm:674–684 arma deadlineJob. Alarm:696–729 abandona com orçamento limitado. playbackRequested():824–830 devolve true enquanto engineAttemptStarted=false; não trata automaticamente o resolve pendente como pausa.
- Com orçamento normal15s e alternativas7s, não está demonstrado loading eterno. Orçamento ilimitado/configurações especiais podem expor espera sem limite; classificar por percurso.
- Corrigir: após ensureActive+owner/current guard, falha estruturada de resolução, terminar/tentar rung pertinente. Não alternar motor se a falha é do portal e ambos usam o mesmo resolve sem estratégia útil.
- Testar null inicial e na troca, deadline finito/ilimitado, cancelamento A→B. Tests player-core/LiveTuneControllerTest já cobrem cancelamento de lookup tardio, não todos os retornos nulos.

### B30 — booleanos Xtream: parcialmente já aplicado; correção proposta precisa de semântica
- core/parser/XtreamClient.kt:919–932/972–1028 já usam nextScalarStringOrNull para canais/VOD/séries; :1066–1076 aceita booleanos como texto, arrays/objetos são ignorados. Não é necessário converter todos de novo.
- Ainda estritos: nextStringOrNull():333–334, usado episódios:282–292 e formato EPG:603; readObject():1048–1064 chama nextString para BOOLEAN e pode lançar erro Android JsonReader.
- Corrigir leitura escalar só nesses pontos; false num campo de formato deve significar não configurado, não extensão .false. true/false em flags precisam preservar significado. Converter todos para null pode apagar informação legítima, sobretudo is_adult/tv_archive e sinalizadores.
- Teste instrumentado Android: core/src/androidTest/.../parser/XtreamClientTest.kt (android.util.JsonReader é stub em JVM); casos falso/ext, true/adult, numérico/string IDs, objetos inesperados, feed live sem VOD.

### B34 — catchup M3U: defeito confirmado com catchup-source válido, solução OR-dias excessiva
- core/sync/M3uSyncer.kt:211 usa entry.catchup != null; source/days são guardados:212–214. Template absoluto válido sem catchup tipo fica invisível ao EPG.
- core/epg/CatchupUrl.kt:96 rejeita !channel.catchup, embora forM3u:167–175 suporte template sem tipo.
- Aprovar capacidade baseada num template/modo utilizável. catchup-days>0 sozinho não constrói URL, e catchup="none" não deve tornar canal diferido. Não inventar arquivo só por dias declarados.
- Testes: template-only válido, disabled/none, dias-only sem método, dias+modo reconhecido, leitura anterior mantida.

### B38 — template relativo: query sem tipo é bug confirmado; concatenar qualquer URI sem protocolo é incorreto
- core/epg/CatchupUrl.kt:170 apenas append faz joinQuery; :171 devolve fromTemplate literalmente. ?utc={utc} sem append vira URI nua.
- joinQuery():188–196 já corrige ?/&/bare-query quando modo append é explícito; essa parte está aplicada.
- Aprovar resolver queries ?/& contra liveUrl preservando parâmetros. Caminhos /archive/x.m3u8, ../x, //cdn.example e URIs completas exigem resolução URI/HttpUrl apropriada, não joinQuery. Fragmentos query sem marcador devem exigir modo que o documente ou validação sintática.
- Testar query token existente, relativo path/host, URL absoluta, templates desconhecidos. CatchupUrlTest e CatchupFormatsTest existem; não foram reexecutados nesta auditoria.

### O13/O14/L5 — custo Calendar/Map e formatador: oportunidade P3 sem medição
- CatchupUrl.kt:48–61 aloca Calendar/map; XtreamClient.kt:453–454 instancia SimpleDateFormat com timezone recebido.
- Não é percurso por frame: corre por abertura/URL de diferido. Ganho improvável de resolver lentidão generalizada UI.
- Pode evitar Calendar quando não há placeholders de data. Se otimizar datas, preservar UTC/template e timezone/DST de Xtream. Não partilhar SimpleDateFormat mutável entre coroutines: não é thread-safe. Formatter imutável com zone por chamada ou thread-local justificado, com testes DST.
- A outra IA mistura O13/O14/L5 sem delimitar bugs; não prioridade superior a URLs erradas.

## Lote 8

### B37 — SAF: diagnóstico da UI atual incorreto/inaplicável
- core/storage/StorageAccess.kt:122–128 define persistAccess; não há caller App/Core atual.
- App/features/settings/RecordingSettingsScreen.kt:222–225 usa StorageBrowser, onPick File.absolutePath; App/ui/components/StorageBrowser.kt:95–107 guarda raízes File e verifica acesso, :169 abre definições de storage. Não é ACTION_OPEN_DOCUMENT_TREE, não há treeUri transitório a persistir nesse callback.
- Não chamar takePersistableUriPermission sobre File/path ou contentUri não concedido: não transforma acesso arbitrário em persistente.
- Se introduzir picker SAF, ActivityResult deve receber grant flags efetivos, persistir READ/WRITE concedidos, só gravar URI após sucesso e validar revogação/unmount. Restaurar backup nunca recria grant Android.
- core/storage/MediaStorageBudget.kt:114–138 resolve volume/acquire target; indisponibilidade já é recusada. Ao implementar SAF não relaxar quota quando espaço/free desconhecido; diferenciar unknown vs0, manter identidade do volume/provider, staged bytes e reservas concorrentes.

### B32/I10 — server local vazio: defeito de protocolo confirmado, alcance de arranque parcial
- LocalSegmentStore.playlist():117–122 exige snapshot não vazio; RecordingPlaybackStore.playlist():92–96 exige entradas.
- LocalTimeshiftServer.kt:72 chama playlist antes de headers; exceção termina socket sem resposta (catch:37). Falta HTTP explícito em estado not-ready.
- Mas App/LiveViewModel.kt:309 aguarda session.awaitReady antes de :313 startLocalHls; LocalTimeshiftSession:68 tem timeout ready. Portanto não provar que reader normal pede manifesto antes do primeiro segmento.
- Resposta 503+Retry-After bounded é defesa razoável para empty/not-ready. Capturar apenas exceção própria de indisponibilidade, não mascarar corrupção/permissão/cancelamento. Manifesto vazio HLS não é solução equivalente garantida; testar policy Media3/libmpv e evitar loop infinito.
- LocalTimeshiftServerTest cobre segmentos completos/cancelamento/intervalos. Novo teste necessário: GET/HEAD empty retorna HTTP completo e transita a200 depois commit, termina após close.

### O10 — deletes EPG e staging: parcial; solução todoRAM rejeitada
- GuideCacheReplacement.kt:24–38 faz staging completo antes da transação, memória limitada. :40–54 limpa uma vez por chave e deserializa lotes500 dentro da transação.
- O10 tem oportunidade real: reduzir muitos DELETEs. EpgDao.kt:36–38 já tem deleteProgrammesForChannels(sourceId, epgChannelIds), reutilizar com orçamento de binds, sem criar redundante matriz de sources desnecessária.
- WAL não significa lock exclusivo bloqueando todos leitores; leitores existentes e writer podem coexistir, um writer por vez. Writes longos podem atrasar outros writes e invalidation/novas operações, mas não demonstram travar o decoder de vídeo.
- Não carregar feed inteiro para RAM em boxes2GB. Preservar atomicidade e bounded staging. Opções: staging tabela/SQL merge, menos DELETEs, pré-validação file streaming fora do writer lock; leitura batch dentro da transação pode ser tradeoff aceitável.
- GuideCacheReplacementTest e Android GuideCacheTransactionTest exercitam preservação após parse/rollback; manter cancelamento e old guide quando falha.

### B20 — rearmAll race: hipótese condicionada, especificação insuficiente
- RecordingScheduler.kt:62–86 obtém snapshot scheduled e depois arm/cancel por row; arm():91–110/PendingIntent data por recordingID é idempotente. cancel:112–115 não coordena o snapshot do DB.
- Existe possível stale rearm depois de cancel DB+alarm, ou updateProgress de snapshot antigo. Mas anexo não fornece interleaving reproduzido nem demonstra captura errada; motor/worker consultam DB de novo.
- Não basta “cancelamento atómico dos alarmes”: precisa política coordenada entre estado durável e arm, revalidação, CAS/transaction de statuses, teste deferred rearm contra cancel/pause/recover e resultado observável.

### B21/L2 — buffers Timeshift: caminho errado, micro-otimização não medida
- Não existe core/timeshift/TimeshiftEngine.kt no checkout. Implementação é LocalTimeshiftSession.kt.
- LocalTimeshiftSession.kt:108–116 usa ByteArrayOutputStream e chunk8192 no download limitado da playlist; não é buffer64KiB em loop TS a cada frame.
- LocalTimeshiftServer.kt:114,144 aloca buffer64KiB por leitura/cópia; é alocação por pedido em scope limitado a4 sockets (slots:27), não comprovada fuga RAM.
- Reutilização deve ser por cliente/coroutine/pool com leases; buffer partilhado único cria corrupção concorrente. Lazy buffer evita HEAD/path sem corpo se houver alocação antes, mas alguns ramos já retornam antes do buffer. Medir churn antes de mexer.

## Lote 12

### B1 — contrato ftsOnly perde B-tree: confirmado P1, corrigir primeiro
- BulkInsertHelper.kt:39–43 chama dropIndexesForBulkInsert(table,ftsTable) sem flags. :96–106 escolhe e remove non-unique B-tree.
- restoreIndexes():125 só recria canonical se !ftsOnly. Com ftsOnly=true remove B-tree e não restaura.
- Callers reais: M3uSyncer.kt:86–90; XtreamSyncer.kt:246–250; StalkerSyncer.kt:122 e402. Primeira carga elegível em tabela vazia pode deixar índices ausentes até startup repair, não apenas tabelas pequenas.
- Correção mínima: ftsOnly nunca remove B-tree; restore reconstitui exatamente/canonicamente tudo que realmente retirou. Conservar UNIQUE, finally NonCancellable, coordenação de writers concorrentes.
- BulkInsertIndexContractTest só compara canonical/KNOWN_TABLES; NÃO executa drop/restore ftsOnly. Precisa instrumentação SQLite (sqlite_master antes/depois) com cancelamento e duas fontes simultâneas.

### B9 — “999 em Android<11” incorreto para este driver; proteção por budget é condicionada
- Core/di/DatabaseModule.kt:36 configura BundledSQLiteDriver; não usa SQLite system Android8/9 para esse limite.
- ChannelDao.kt:150 tem dois IN (sourceIds e remoteIds), outras queries várias cláusulas/fixed params. IN(sourceIds) usa número de listas, não número de canais240. Não se prova estouro só por ter240 canais.
- Descobrir driver compile option SQLITE_MAX_VARIABLE_NUMBER e contar bindings totais. chunk450 pode ser margem segura para duas listas450 + fixedparams, não para qualquer consulta3listas.
- Chunk lookups/deletes simples é possível preservando atomicidade/dedupe. Queries com ORDER BY/LIMIT/Room PagingSource (ChannelDao:263–272,365–377) não podem concatenar resultados de chunks e aplicar LIMIT local: precisa ordem global e paging corretos, ou temp table/join IDs/uma query.
- Não aplicar genericamente em todas queries nem indexação baseada no AndroidSDK. Testar1000+, duas listas, ordenação e páginas sem duplicações/saltos.

### B22 — índice targetItemId/mediaType: sem query que o use; rejeitar por agora
- TvProviderEntities.kt:24–25 já possui profileId e unique(profileId,surface,mediaType,groupId).
- TvProviderProgramDao.kt:20–37 usa essa identidade/profile/surface; nenhum WHERE targetItemId+mediaType.
- targetItemId é payload no TvHomeRepository, não chave de lookup DAO atual. Novo índice adiciona escrita/migração/DBsize sem benefício demonstrado. Requer query/EXPLAIN QUERY PLAN e volume antes de aprovar.

### B13/I4/L1 — PIN/segurança: separar UI autorizada de hashing
- Caminho do anexo core/model/Pin.kt é incorreto: real core/util/Pin.kt.
- util/Pin.kt:28 usa pin.toByteArray() Kotlin, cujo default é UTF-8; “charset desconhecido” não é bug funcional. Explicitá-lo é estilo/manutenção P3, sem migração de hashes.
- ManageProfiles/ProfileGate bypass não vem com interleaving ou entrypoint no anexo. Especialista UI deve verificar auth/entrypoints; não concluir só por proposta genérica.

### B3/B4 — multiview codec e flags: parcialmente aplicado, não mudar genericamente
- Caminho MultiviewState do anexo player-core é incorreto: App/features/multiview/MultiviewState.kt.
- LivePreviewEngine.kt:410–419 já tem softwareDecoderAllowed/budget guard para lowSpec; ExoRenderers:44–50 mantém backstop hardware/software intencionalmente. Não prova fallback descontrolado, precisa limite por tile/resolução/capacidade.
- MultiviewState.kt:196,216 remove soundOnly; :199–229 reatribui audible quando remove/falha; :244 clear. Problema genérico de flags parcialmente já tratado.
- Sem paths/sinais adicionais, só testar remover tileaudible/error/replace/remaining audio; não impor congelamento frame em todos devices.

### Complementares B2/B7/B8/B10/B11/B12/B14/B17/B18/B24, O1/O2/O3/O7/O8,I1/I2/I5
- B2 ABI packaging já corrigido hoje: App CI seleciona universal explicitamente e verifica libs v7a+arm64; updater seleciona ABI e inspeciona native entries. Não repetir lote.
- B7 isolamento source backup tem filtros/remaps existentes BackupManager.kt:124,182–186, remapKeys/helpers e BackupV17ScopingTest/BackupMergeRemappingTest. Anexo não fornece nova falha; classificar já aplicado para problemas cobertos, sem afirmar restauro universal perfeito.
- B8 histórico: não há mecanismo/linha concreta; preservar compatibilidade, sem inovação de histórico que utilizador excluiu.
- B10/B11/B12/B14/B17 VOD/pesquisa: genéricos, UI filmes/séries retirada; remeter especialista UI ao grafo real. Não remover dados históricos Core só para “fechar IDs”.
- B18 contentHash: EpgRepository.kt:328–339 calcula hash para ingestão normal; GuideCacheReplacement:42–49 reconstrói EpgProgrammeEntity sem contentHash e default EpgEntities:76 é0. Isso pode provocar update/re-hash completo no sync seguinte; não prova programas errados nem perda de dados. Melhor calcular hash canónico em cache restore se percurso precisa; teste diff apóscache restore.
- B24/O8 sync local pipe binário: sem localização/mecanismo no anexo; lotes1–4 dizem encerrado, não são prova. Não reabrir alterações byte crypto/sync sem reprodução.
- O1 UserDataResolver:384–419 já agrupa INSERT em chunks de transações, mas locate/describe ainda fazem queries por item (:426–447,:483). Otimização set-based possível para grandesrestauros, distinta de bug; testes relink/tombstones necessários. Não concatenar queries ordenadas ingenuamente, preservar identity (source/type/remote/name).
- O2 polling disco: MediaStorageBudget.refresh():91–109 já tem cache5s, lease checks500ms:151–156. Necessidade de adaptativo exige métricas; não reduzir verificaçõesfree/quota em writes só para diminuirCPU.
- O3/O7/I1/I2/I5 não têm descrição separada na lista complementar; não auditáveis pelo ID. Solicitar especificação ou manter pendentes, nunca marcar concluídos em conjunto.

## Aceitação / limites
Só inspeção e inventário de testes existentes; nenhum teste novo/build efetuado nesta revisão. Preferir 1) ftsOnly índices, 2) URLs/parsers dirigidos, 3) resposta HTTP local typed quando notready, 4) otimização DB medida; SAFE picker e calendários apenas seproduto realmente precisar. Documentação oficial consultada pelo coordenador sustenta limites SQLite, WALconcurrent, grantsSAF e thread-safety; não valida o runtime destas boxes.


---

## Anexo especializado — ui

# Revisão crítica dos Lotes 5, 9, 11 e perfis do Lote 12

13/10/2026. Checkouts TV OwnTV-main / OwnTV_Core; não Mobile. Análise read-only do plano externo. Única correção fora da análise: limpeza do teste órfão LinkedSubtitleDisplayTitleTest, após confirmar que os dois helpers removidos do antigo DeleteSubtitlesScreen não têm nenhum chamador em produção/androidTest. Teste e implementação anterior estão preservados em Temp/owntv-ui-oct13-before. Não restaurei uma feature morta só para sustentar esse teste; não removi testes do backend de legendas.

## Lote 5

| ID | Classificação | Evidência e decisão |
|---|---|---|
| B31 | Parcialmente confirmado | Core parser/XmltvTime.kt:32–48 exige 14 dígitos; 12 sem segundos e +HH:MM são rejeitados. 14 dígitos seguidos de Z já resultam corretamente em UTC; XmltvTimeTest.kt:60 testa essa entrada. Não chamar a todos os exemplos ISO8601 extended: são variantes compactas XMLTV. Ampliar parser explícito preservando UTC sem offset e politica para entradas parciais. |
| B36 | Confirmado mecanismo; solução necessita desenho | Core epg/EpgDedupe.kt:64–65 considera qualquer sobreposição positiva e título normalizado igual. Blocos A 10:00–10:30:01/B10:30–11:00 com mesmo título colapsam. Não há teste desse 1s; testes atuais só contiguidade exata, duplicates comuns e prioridade de feed. Critério fixo >=5min ou50% precisa de testes de eventos curtos, duas fontes e episódios homónimos; não mergear pela mera semelhança do título. |
| B39 | Parcialmente confirmado; política atual explícita | App features/epg/GuideProgrammeCells.kt:44–52 trunca o programa longo no início do seguinte e não recria continuação depois do bloco curto. GuideProgrammeCellsTest.kt:52–57 exige exatamente o vazio posterior; portanto o plano altera uma política existente, não corrige um comportamento sem teste. Concordo avaliar fragmentação por prioridade, conservando identidade e timestamps originais para diferido. Sem inventar programação onde nenhum intervalo original cobre. |
| I9 | Já aplicado no essencial | XmltvTime.kt:53–64 e83–95 já usam daysFromCivil/Howard Hinnant; sem Calendar/TimeZone/SimpleDateFormat por evento. Ainda há trim/replace potencialmente alocante em32: não afirmar zero objetos universal nem repetir o algoritmo. |
| L3 | Confirmado como limpeza menor | XmltvParser.kt:258–300 contém skipElement/recoverElement com corpos iguais. Pode consolidar num helper sem mudar cancelamento ou profundidade. Não é solução para pausas de vídeo nem motivo de prioridade P1. |

Aceitação L5: timestamps 12/14, ±HHMM, ±HH:MM, Z e timezone da box diferente; preservar epoch de14Z/UTC. Eventos contíguos e1s overlap mesmo título devem manter identidades; duplicates substanciais entre fontes devem respeitar feed preferido. Fragmentação longa→curta→continuação deve desenhar/selecionar a mesma entidade original, com diferido a usar seu start original. Tests existentes não medem todos estes casos.

## Lote 9

| ID | Classificação | Evidência e crítica |
|---|---|---|
| B33 | Hipótese plausível / análise causal incompleta | LongPressMenuGuard.kt:36–44 usa0 e pode armar ao primeiro tick100ms sem evento; isso é deliberado para KeyUp perdido conforme comentário18–29. Os repeats podem chegar depois desse primeiro tick; porém exigir KeyUp sempre reintroduz o caso documentado que engole o próximo OK. Inicializar wall clock sozinho também não resolve um primeiro repeat tardio. Preferir identificar abertura por gesture/eventTime, ignorar repeats e aceitar nova pressão deliberada, com clock monotónico/fallback bounded. |
| O11 | Parcialmente confirmado | ChannelListOverlay.kt:77–92 efeito depende só currentId, não chegada/mudança de canais; requestFocus testa isFailure em vez de retorno Boolean. Dados vazios→carregados com currentId estável podem não refocar. Não provei fuga de foco apenas porque o item inicial foi reciclado: LazyColumn tem navegação própria. FocusRestorer sozinho não resolve carregamento/alvo inexistente. |
| B25 | Parcialmente confirmado | CategoryRail real fica em features/shell/components, não ui/components do plano. Linha161 cria requesters por tamanho e linha299 usa índice original como key; reorder/filtrar mantendo tamanho pode associar posição a categoria diferente. Há tentativas/selectedFocus e retorno por target index já implementados. Alteração mínima é chave estável real de RailCategory, com fallback quando some, não category.id presumido para uma categoria virtual. |
| O9 | Parcial / sobreposto a B25 | Mesmo mecanismo; não contar como outro bug descoberto. Avaliar foco selecionado e último focado separadamente. |
| I7 | Funcionalidade opcional parcialmente existente | DirectTune e captura numérica do fullscreen já existem. ChannelListOverlay não recebe input de números; HUD fica inert com esse overlay, portanto extensão ali é possível. Reutilizar a resolução/owner existente, não novo timer independente de1,5s e índice visual confundido com número real de canal. Não prometer velocidade maior com animação de scroll. |

Tests: ChannelPressGestureTest cobre limite2/3s, repeats não reiniciarem gesto, cancelamento/foco e orphanKeyUp. Não testa a composição LongPressMenuGuard nem entrega de eventos entre janelas OEM. Necessário ensaio UI com autorepeat500ms/KeyUp perdido/menu aberto ainda com tecla premida. Para overlay: montar vazio, preencher sem alterar currentId, mudar conjunto mantendo tamanho, currentId removido, scroll longe e voltar, confirmar requestFocus Boolean e nunca autoplay.

## Lote 11

| ID | Classificação | Evidência e crítica |
|---|---|---|
| O12 | Justificação incorreta / ganho hipótese | Glass.kt:568–600 explica whole-image float sampling para não introduzir desalinhamento de pixels. RoundedPanel.kt:76,115; DialogPanel.kt:59; FocusableSurface.kt:223 já clipam shape. Afirmar renderização visível fora do painel/saturação universal sem medição é indevido. clipRect adicional pode ser defensivo num caller sem clip; deve conservar transforms float e ser medido. Idle cards já evitam resampling em Glass.kt:509–520. |
| O4 | Já aplicado no caso descrito | LiveScreen.kt:1117–1122 usa distinctUntilChanged em títulos do canal; isFocusedChannel é Boolean calculado por comparação, não flow repetitivo independente. Não identifica emissões concretas por eliminar. Ainda se podem medir custo de38ms gesture ticks/recomposições, mas isso é outro caso e não uma prova de ganho2–4x. |
| O6 | Já aplicado aos canais principais / generalização incorreta | ChannelLogoTile.kt:43–46 já size128x128, Precision.INEXACT e crossfadefalse; LiveScreen.kt1144 usa esse helper, overlay também. Coil AsyncImage com constraints não implica carregar sempre o PNG4K original. Auditar os restantes pontos conforme tamanho real; não forçar128 em posters/backdrops. INEXACT é opção de decoder, não teto rígido de RAM. |
| B16 | Confirmado risco de cache negativo; duração exata não provada | Local correto é OwnTVApp.kt:170–182: adiciona max-age180dias a qualquer resposta e usa factory Coil sem estratégia própria. Verificação oficial Coil3.6.0 pelo coordenador: NetworkFetcher98–102 escreve disco ANTES de throwIfFailureResponseCode; DefaultCacheStrategy32–43 admite404e leitura15 reutiliza resposta de disco. Logo erro pode persistir até expulsão/invalidação, mas não afirmar timer exato180dias. Restringir apenas o interceptor a2xx é insuficiente: estratégia default ainda aceita404. Usar estratégia que não escreve não2xx e que rejeita entradas antigas de falha antes de pedir rede. |
| B26 | Já mitigado / hipótese residual | AutoFrameRateEffect está no próprio player/FrameRateController.kt386+. holdThroughSwitch441–461 guarda item, revision de playback e requestedfalse; finally Handler só devolve sua própria pausa se estado ainda é pertinente. Não há resume incondicional. Não removerfinally cegamente: deixar pausa temporária sem restituição pode congelar áudio/vídeo. Testar stop/newitem/userpause/desligarAFR/cancelar Activity antes de outra alteração. |
| O5 | Não especificado suficientemente / parcialmente já aplicado | Listener singleton AFR usa ownership por identidade416–429 e limpa apenas o listener que instalou. O plano não apresenta outro singleton/caller/estado que demonstre leak. Não eliminar o controlador partilhado sem contract de display. |

Tests atuais GlassConfigTest medem configuração, não GPU/frame-time. FrameRateChooseModeTest mede seleção de modo, não lifecycle da pausa/finally. Regressão Coil precisa servidor local404depois200: confirmar dois GET e logo recuperado, inclusive recriação de ImageLoader com disco persistente e entrada404legada. Perfetto/frame timing em box para O12; não estimar consumo Mali pela dimensão lógica do bitmap.

Fontes oficiais Coil3.6.0 utilizadas pelo council: [DefaultCacheStrategy](https://raw.githubusercontent.com/coil-kt/coil/3.6.0/coil-network-core/src/commonMain/kotlin/coil3/network/internal/DefaultCacheStrategy.kt) e [NetworkFetcher](https://raw.githubusercontent.com/coil-kt/coil/3.6.0/coil-network-core/src/commonMain/kotlin/coil3/network/NetworkFetcher.kt). Esta revisão adicional eliminou a hipótese anterior de que o erro seria lançado antes da escrita.

## Lote 12 — perfis App/PIN

| ID | Classificação | Evidência e decisão |
|---|---|---|
| B13 | Parcialmente confirmado; distinguir administrar de entrar | ProfileGate.kt:63,95–100 verifica PIN para entrar; ProfileGateSession mantém autenticação por perfil, perde-a em process death e invalida mudança. Um bypass de entrada externo não foi demonstrado. ManageProfilesScreen.kt:154–166 abre editor e vm.edit diretamente; ProfileComponents.kt187–209 permite remover/alterar PIN sem autenticar PIN anterior; ProfilesViewModel.kt69–70 e Core profile/ProfileManager.kt55+ gravam sem confirmação. Isso é uma política de administração global real: precisa decidir se qualquer sessão autenticada administra perfis, se só adult/admin, ou se exige PIN atual do alvo para reduzir proteção. Não descrever a existência da UI como exploit reproduzido sem a política. |
| I4 | Proposta de política, não inovação tecnicamente especificada | Determinar que ações requerem reautenticação: remover PIN, alterar parentalidade, apagar perfil protegido. Usar grant efémero vinculado ao alvo/ação, não booleano global. Testar perfilAautenticado editandoBbloqueado, cancelamento e alteração do hash durante a operação. |
| L1 | Incorreto como bug de charset | Pin real é Core util/Pin.kt:26; Kotlin pin.toByteArray() usa UTF-8 por defeito. Torná-lo explícito pode melhorar leitura mas não muda bytes nem resolve bypass. Não mudar normalização/hash existente sem estratégia compatível; 4–6dígitos é proteção parental leve, não vault. |

Tests ProfileGateSessionViewModelTest cobrem requestSwitch, contexto de autenticação, configuração/process lifecycle e apagar ativo/nãoativo. Não cobrem autorização de edição de outro perfil protegido. A matriz de administração deve preceder uma correção automática que possa bloquear o único utilizador.

## Prioridade recomendada

1. B31 variantes comprovadas, B36 marginal overlap com matriz de curtos/repeats; definir política B39 antes de implementação.
2. Estado/foco overlay vazio→carregado e identities CategoryRail. B33 só com reprodução/eventtrace, evitando regressionKeyUp.
3. B16 corrigir estratégia de cache negativo e testar recuperação404→200/disco legado; definir autorização B13 com política clara.
4. Não repetir I9/O4/O6/B26/L1 já aplicados ou incorretos. GPU O12 e I7 são avaliação/funcionalidade, não correções críticas.

Não executei testes/build nesta tarefa: coordenador faz validação final. Leitura de testes existentes não equivale a novos resultados. Nenhuma alteração de produção relativa ao plano novo foi aplicada.


---

## Anexo especializado — player

# Revisão crítica do plano externo — leitor, áudio/lifecycle e multiview

Data: 13/10/2026. Apenas leitura. Fontes: anexo `24aa615a-c4cd-4f60-aa87-9cf78d5e2e51/Texto colado.txt`, checkouts OwnTV-main e OwnTV_Core atuais; Mobile excluído. Não executei novos builds/testes nesta revisão.

Os números do plano são identificadores documentais, não evidência de 69 problemas únicos. O Lote 12 também mistura itens históricos, sem especificação de mecanismo. Esta revisão cobre Lote 6, Lote 10 e B3/B4 multiview do Lote 12; não certifica todos os IDs DB/backup desse lote.

## Classificação resumida

| ID | Resultado no checkout atual | Decisão recomendada |
|---|---|---|
| B27 | Configuração só HLS confirmada; classificação P0 e promessa causal excessivas | Não estender automaticamente; opção TS explícita apenas com caso dirigido |
| B28 | Confirmado, normalização parcial existente | Corrigir correspondência ISO preservando identidade/ambiguidade das faixas |
| L4 | Stop normal limpa; órfãos após crash/cancelamento IO continuam possíveis | Limpeza pequena ao arranque, segura e fora da main; baixa prioridade |
| B35 | Encerramento terminal enfileirado confirmado; cenário bloqueado é condicionado | Auditar contrato JNI e propriedade antes de redesenhar teardown |
| L6 | retirementScope não cancelado; não prova coroutine eterna | Cancelar somente após fecho/resultado e libertação segura dos recursos |
| B23 | Espera pausada deliberadamente ilimitada; abort proposto é incorreto | Diferenciar pausa, stop e cancelamento de owner; não usar somente false |
| I8 | Callbacks HDMI já existem; rearmar latch automaticamente não existe | Hipótese de evolução, não correção universal; utilizar rota efetiva |
| B15 | Prime seekable já existe; versão unseekable não demonstrada necessária | Não aplicar re-prime/reconnect em todos os canais |
| B19 | Stop no background e gestão de foco já existem; receiver citado não existe | Testar screen-off sem onStop na box antes de acrescentar hook |
| B3 | Fallback de decoder habilitado; é limitado pela lista Media3 | Eventual política específica multiview, preservando descodificação explícita por software |
| B4 | Remoção/expiração e transferência de áudio implementadas | Não reaplicar; pequena coerência de badge audível merece teste |

## Lote 6

### B27 — flags MPEG-TS

Evidência Core: `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:3469–3476` constrói DefaultExtractorsFactory com caption descriptors; `3498–3503` aplica HlsCompatibility à fábrica HLS; `3565` regista compatibilidade como não aplicável fora de HLS. `HlsCompatibility.kt:7–22` explicita exceções por canal que conservam os defaults HLS. O domínio/persistência usa campos `hlsDetectAccessUnits` e `hlsAllowNonIdrKeyframes`, não um contrato genérico TS.

A leitura do mecanismo é correta, mas chamar a separação de escopos de P0 não está justificado. Uma opção denominada HLS não tem necessariamente de alterar TS progressivo. O Android documenta que deteção de access units custa CPU, e permitir non-IDR pode apresentar corrupção visual inicial/após seek. Isto não prova nem corrige lipsync crónico, HTTP403 ou zapping. [Media3 troubleshooting](https://developer.android.com/media/media3/exoplayer/troubleshooting#why-do-some-mpeg-ts-files-fail-to-play).

Se houver um TS específico sem AUD/IDR, adicionar compatibilidade TS explicitamente opt-in, defaults intactos, e invalidar a factory por valores/rota. Aceitação: TS sem AUD, TS sem IDR, TS normal, mudança de canal sem carryover; HLS normal intacto. `HlsCompatibilityTest.kt` cobre independência/defaults dos campos, não reprodução real de TS.

### B28 — língua na seleção de áudio/legendas

Confirmado: `AudioTrackRetention.kt:8–15` compara língua sem normalização ISO; `ExoSubtitleEngine.kt:974–975` faz o mesmo no restauro de escolha explícita. O ramo de preferência automática já usa `subtitleLanguageMatches` em `954`. `SubtitleLanguage.kt:6–18` tem o helper ISO/locale.

Usar o helper nos dois percursos é razoável, mas não basta trocar equals: AudioTrackRetention também exige label; legendas usam firstOrNull e singleOrNull como fallback, podendo escolher outra faixa quando há idiomas/roles ambíguos. Preservar a distinção Original/Comentário/SDH e critérios de unicidade. Testes presentes: `AudioTrackRetentionTest.kt` cobre reordenação, labels e ambiguidade; `SubtitleLanguageTest.kt` cobre eng/en/região. Faltam pt/por e seleção cruzada explícita com duas faixas da mesma língua. Prioridade P2 de correção localizada; não prova perda universal.

### L4 — cache subshift

`SubtitleShift.kt:20–33` gera/reutiliza ficheiros; `ExoSubtitleEngine.kt:1114–1118` cancela job e apaga os ficheiros conhecidos ao parar; release também cancela shiftScope em `1121–1123`. Não existe purgeOrphaned no checkout. Crash impede cleanup; a tarefa IO `486–489` pode criar uma cópia e ser cancelada antes de a registar no mapa.

Purga ao arranque é útil como higiene, sem promessa de ganhos HLS. lastModified não é tempo de inatividade: uma rotina genérica não deve apagar ficheiros da sessão ativa só porque são antigos. Preferir purge antes da criação dos engines, prefixo/diretório restrito, fora da main, e cleanup da cópia cancelada. `SubtitleShiftTest.kt` cobre transformação, e `HeroPreviewAndSubtitleLifecycleTest.kt` scopes reutilizáveis; não provam purge após process death.

## Lote 10

### B35 + L6 — destruição libmpv

`OwnTVPlayer.kt:4145–4172` cancela scope principal, cria retirement e enfileira removeObserver/destroy no mpvExecutor; shutdown não interrompe um comando em execução. Portanto um executor bloqueado pode atrasar o encerramento terminal. Não foi demonstrado nesta revisão que a thread JNI de eventos esteja bloqueada: o ponto observável é o executor de comandos/properties.

Já há quarentena e confirmação em `3872–3954`, libertação de descritores só em completeNativeRetirement, e hardReset com thread dedicada em `3988–4005`. Cancelar retirementScope imediatamente elimina a autoridade que ainda confirma/liberta recursos; um SupervisorJob vivo sem children também não é prova de leak.

A solução externa é perigosa sem fence de chamadas nativas: a API oficial declara destruição bloqueante e proíbe chamadas concorrentes no mesmo contexto durante terminate/destroy. Um timeout Kotlin não mata a operação JNI nem prova fd fechado. [Contrato libmpv client.h](https://raw.githubusercontent.com/mpv-player/mpv/master/include/mpv/client.h).

Recomendação: conferir a implementação MPVLib.destroy da versão usada, retirar owner/eventos/comandos, excluir chamadas antigas/in-flight, executar teardown segundo esse contrato e só depois cancelar retirementScope e libertar leases. Auditar também o hardReset existente em vez de copiar cegamente o seu padrão. Testes atuais NativeUnloadProofTest/NativeResourceLeasesTest cobrem provas/leases, não concorrência JNI real. Teste mínimo: comando pendente + release, zero chamadas concorrentes depois do fence, descritor permanece até destruição efetiva, falha não autoriza reaproveitamento.

### B23 — awaitMpvOutcome

O método está em `LiveEngines.kt:191–208`, não em OwnTVPlayer; devolve MpvOutcome, não PlaybackOutcome.Aborted. `LiveWaitBudget.kt:49–64` dá crédito ao tempo pausado. `LiveTuneController.kt:693–710` e `829–836` preservam pausa intencional; a máquina cancela watchers ao mudar escolha/stop. Testes LiveTuneControllerTest:693,706,753,1019 cobrem pausa, handover e cancelamento.

False representa pausa e stop. Retornar Aborted sempre que false destruiria a pausa deliberada e poderia disparar alternativa/erro sem ação do utilizador. Um stop sem cancelamento do watcher é uma hipótese a testar com owner/revision, não um loop P0 confirmado em todos os percursos. Implementar resultado abortado apenas com estado explícito de stop/cancel/owner obsoleto; confirmar que Play após pausa continua a tentativa válida.

### I8 + B15 + B19 — HDMI e standby

Callbacks já presentes: OwnTVPlayer.kt:733–749 atualiza canais PCM no mpv; PlaybackSession.kt:268–288 observa outputs e regista informação. AudioOutputPolicy.kt:49–62 mantém latch estéreo até mudança deliberada da configuração. Dispositivo disponível não prova rota AudioTrack efetiva; limpar o latch por qualquer AudioDeviceCallback pode reativar um passthrough já comprovadamente defeituoso e não atualiza por si só sinks já construídos. Evolução exige identidade da rota efetiva, reconfiguração dirigida e teste soundbar on/off sem regressão estéreo.

LivePreviewEngine.kt:3032–3042 já faz re-prime após troca de faixa apenas se passthrough e seekable. O guard evita reconexão de direto não seekable. Perda de pacote de rede não demonstra que HDMI precise de prime: não introduzir seek/reconnect global por B15.

`StandbyReceiver.kt` citado não existe no inventário. MainActivity.kt:149–161 já pára os leitores em onStop; PlaybackSession.kt:132–133 liberta foco conforme intenção/estado. SCREEN_OFF em AutoStartService.kt:33 controla apenas despertar automático. Se a box apagar e mantiver Activity STARTED, é um caso físico a investigar. Pausar decoder mantendo ligação difere da política atual de parar/libertar IPTV no background. Não duplicar authorities nem confundir SCREEN_OFF com todas as ações HDMI-CEC.

## Lote 12 — multiview

### B3 — software fallback

`ExoRenderers.kt:43–45` habilita decoder fallback, comum aos três engines, com listas/ordenação controladas pelas preferências. `LivePreviewEngine.kt:1608–1617` trata esgotamento reconhecido sem reconstruções indefinidas, mas depois da seleção/fallback interno Media3. O fallback não é demonstrado descontrolado; uma lista finita pode entrar num decoder software que funciona mas pesa na CPU.

Se houver medição problemática, política específica dos tiles para software-video fallback quando hardware é pedido. Não desativar áudio software/FFmpeg, secure decoders ou a preferência explícita software. Suspender/fotograma fixo exige também parar transporte/retirar reservation quando adequado. Testes SoftwareAudioDecoderPolicyTest verificam separação áudio/vídeo, não capacidade de quatro MediaCodec reais.

### B4 — flags de áudio

Já aplicado em grande parte: App `MultiviewState.kt:127–147` silencia falha e transfere som; `192–230` remove soundOnly ao expirar/remover, escolhe sobrevivente válido e, sem sobreviventes, pool.giveSoundTo(null). Pool e UI já têm retirada confirmada/owner/peek, gate por slot/fonte e retirada no background. O caminho citado no plano (`Core/player/MultiviewState.kt`) está errado: a classe vive na App.

Pequena incoerência residual possível: `silenceFailed():132` deixa state.audible no índice antigo quando o pool fica sem tile audível; `MultiviewScreen.kt:265` usa state.audible para badge. Um mosaico ERROR pode mostrar speaker sem áudio ativo. Corrigir badge a partir de pool.audibleTile ou separar índice selecionado de saída efetivamente audível; não apagar automaticamente a escolha soundOnly numa falha transitória sem definir intenção de retoma. ConfirmedRetirementTest tem cinco casos fake de ordenação/cancelamento, não teste físico de áudio multiview.

## Ordem aconselhada

1. B28 localizado, com testes ISO/ambiguidade.
2. Revisão B35/L6 com contrato JNI, modelo fake e prova física antes de alterar destroy.
3. L4 e badge B4 como higiene/coerência pequena.
4. B27 opt-in TS, I8 e B3 somente se existirem streams/dispositivos/medições que justifiquem.
5. Não aplicar o abort false de B23, prime unseekable universal B15 nem duplicar standby B19 como estão escritos.

Nenhuma nova implementação nesta tarefa. Não confundir presença de testes existentes com nova execução nem substituir ensaios Android TV/HDMI/JNI por inspeção estática.
