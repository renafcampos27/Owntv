# OwnTV 5.2.0 / Core 1.0.68 — análise para a versão TV personalizada

Data: 18/10/2026. Estado: análise estática e plano proposto; nenhuma alteração funcional aplicada.

## Base de comparação

- App personalizada: `C:/Users/renat/Downloads/OwnTV-main`.
- Core personalizado: `C:/Users/renat/Downloads/OwnTV_Core`.
- App upstream: tag `v5.2.0`, comparada com `v5.1.0` e com os percursos locais relevantes.
- Core upstream: tag `core-1.0.68`, commit `c14e944b8d17e5cbb559d116afb30dc3b34864cb`, comparada com `core-1.0.67` e com o Core local.
- Lock atual da App: `3db885fdb4e8308d358a78f0102169916f74b7b960ae8b40c080ff6357e66370`, 994 ficheiros, versão interna `1.0.57-own.20261011`. Este identificador não significa equivalência integral com uma release upstream.
- Core local: base de dados v50; exportação de backup declara versão 25. Upstream novo: base de dados v49 e backup v25. Números iguais de backup não demonstram identidade de todos os campos personalizados.
- Ambos os catálogos de Core consultados usam Media3 1.11.1 e libmpv 2026.10.3. Esta atualização não fornece, por si, um motor nativo mais recente do que o já configurado localmente.

Foram consultados os registos locais de integração de Core e App e a pendência `UPSCALING-BETA-01`. O histórico foi confirmado no código relevante; não foi tratado como prova de execução numa box.

## Decisão geral

Recomenda-se adaptar correções selecionadas, preservando o Core personalizado. Não substituir o checkout nem importar os controladores completos da versão original.

Preservar: HLS estrito e exceções por canal, intervalo configurável para reabertura, confirmação de libertação dos pedidos/motores, configurações por canal, separação entre hora do EPG e correção do pedido de diferido, volume interno a 100%, backups personalizados, modo simples e interface dedicada a TV.

## Recomendações por fase

### Fase A — correções pequenas e coerência

**A1 — Normalizar os formatos de data/hora antes de combinar os campos. Recomendada.**

O helper local `ui/format/TimeFormat.kt` acrescenta `Hm`/`hm` diretamente ao argumento recebido. `RecordingsScreen.kt` chama-o com `EEEdMMMjm`, que já inclui hora. A versão 5.2.0 remove os campos de hora existentes antes de acrescentar o formato final. Adaptar esta correção evita padrões redundantes que podem provocar uma exceção de formatação. O caso local identificado é a apresentação de gravações; não assumir que todos os ecrãs de backup/log estão atualmente a provocar a mesma falha.

Validação: formatos 12/24 h, chamadas com e sem hora, datas das gravações e mudança de idioma.

**A2 — Leitor externo predefinido e lançamento mais leve. Recomendada.**

`ExternalPlayerLauncher.kt` local ainda usa `Intent.createChooser` quando existem vários leitores e lê o idioma de forma bloqueante antes de qualquer lançamento. O upstream usa o resolvedor normal do Android, permitindo a escolha persistente quando o sistema a disponibiliza, e só lê o contexto localizado para mostrar erros. Adaptar estas duas mudanças, mantendo os headers, candidatos MIME e o protocolo local de regresso à lista. Não prometer que todas as boxes apresentam o botão «Sempre»; isso depende do resolvedor/firmware.

Validação: um/vários leitores instalados, escolher predefinido, uma pressão em Voltar, sem reabrir automaticamente o canal anterior.

**A3 — Completar a validação das definições restauradas. Recomendada, sem substituir o formato local.**

Já existem `PortableSettingsSnapshot` e `SettingsBackupValidation`. Contudo, a tabela de limites não cobre todos os valores discretos de navegação/áudio/legendas. O Core novo introduz regras para passos de seek válidos, atraso de áudio, presets e escala de legendas. Adaptar os princípios aos limites reais desta versão. Conservar a política local de validação antes da escrita e decidir explicitamente rejeição/normalização; não copiar silenciosamente a política upstream de ignorar entradas.

Validação: backup válido antigo, valores inválidos, ausência de chaves, preservação de credenciais e de configurações por canal; restauração sem alterações parciais quando a validação falha.

**A4 — Corrigir a decisão após a espera do refresh automático de EPG. Recomendada como adaptação local.**

No código local, `EpgSyncWorker.doWork()` chama `watchSession.awaitIdleWithin(120_000L)` mas ignora o Boolean devolvido. Assim, pode iniciar a sincronização mesmo que a reprodução continue após dois minutos. `CatalogSyncWorker` já trata esse resultado e devolve `Result.retry()`. Tornar a decisão coerente. Não importar simplesmente uma espera ilimitada: uma TV que reproduz continuamente pode ficar com o guia desatualizado. Usar adiamento controlado e uma política explícita de atualização, mantendo o período escolhido pelo utilizador.

Validação: reprodução contínua, reprodução terminada durante a espera, trabalho manual e cancelamento do worker.

### Fase B — diferidos e relógios

**B1 — Contador de recuo baseado no início efetivo do arquivo. Recomendada.**

Localmente, `LiveTimeshift.loadArchive` devolve Boolean e `archiveBaseWall` recebe o instante pedido. Xtream escreve o início do pedido apenas até ao minuto. Por exemplo, pedir 12:34:47 pode abrir 12:34:00: o relógio/atraso pode ficar 47 segundos desalinhado. O Core novo devolve também o início efetivo. Adaptar o contrato `ResolvedArchiveSource`/`LiveArchiveUrls` e os chamadores para transportar esse instante.

O início para o pedido e o início apresentado ao utilizador precisam de semânticas separadas: preservar a correção manual de +1 h sem deslocar novamente o relógio visível. Aplicar o arredondamento apenas aos formatos que realmente trabalham ao minuto.

Validação: início aos segundos 00/30/59, recuar/avançar, mudança de programa e correção manual de +1 h.

**B2 — Fuso automático do painel Xtream. Recomendada como opção adicional.**

Hoje `CatchupClock` distingue dispositivo/manual por origem, sem utilizar o fuso anunciado em `server_info`. O upstream lê um identificador de zona válido e, na ausência dele, tenta inferir um offset a partir de `time_now`/`timestamp_now`. Adaptar com validação e fallback. Um offset inferido não substitui uma zona com regras de horário de verão para arquivos antigos.

Manter as escolhas manuais existentes e a correção dos diferidos. Não converter automaticamente uma instalação com +1 h manual para o novo modo. O diagnóstico deve mostrar separadamente zona usada, correção aplicada e início solicitado, sem credenciais.

**B3 — Mais convenções de diferido M3U. Recomendada, impacto condicionado às listas que as usam.**

O parser local não trata a nova informação `catchup-correction`/`timeshift="N"`, e o resolvedor não reconhece argumentos como `{duration:60}` e `{utc:YmdHMS}`. Além disso, localmente `timestamp` pertence aos tokens de início; o novo resolvedor trata-o como instante atual nas convenções Kodi/TiviMate. Adaptar com testes e compatibilidade para templates manuais já guardados; alterar o significado global sem migração pode quebrar uma alternativa personalizada.

Guardar a correção declarada pela lista separadamente da correção manual do utilizador, com regra explícita de composição e sem a aplicar duas vezes.

**B4 — Identidade estável de arquivo e nova ligação Stalker no retry. Recomendada.**

As chamadas locais de reprodução de programas/recuo não passam uma chave estável de diferido nem um provider para renovar especificamente o URL do mesmo programa. O upstream introduz uma identidade `CATCHUP` separada de `LIVE` e a resolução de uma ligação nova Stalker em cada tentativa. Adaptar mantendo a identidade/generation local, alternativas de formato e cabeçalhos. Não copiar uma chave de arquivo para a configuração do canal em direto.

Benefício principal: preferências de motor/zoom deixam de depender de um URL com data/token. A renovação Stalker só beneficia esse tipo de origem; a lista Xtream do utilizador não depende dela.

### Fase C — memória e recuperação do timeshift local

**C1 — Pressão de memória em Android 14+. Recomendada.**

`OwnTVApp.onTrimMemory` local limpa imagens e encaminha o aviso para mpv. Não foi encontrado o monitor de pressão ativo nem a política de redução do orçamento do Exo presentes no `PlaybackEngines` novo. Android deixa de enviar os níveis `RUNNING_*` desde API 34. Adaptar uma consulta periódica leve apenas com reprodução ativa, com ação uma vez por subida de pressão; manter os callbacks nas boxes antigas.

A redução do orçamento do Exo deve preparar uma próxima construção/sintonia, sem reiniciar um canal saudável no instante do aviso. Não confundir Home/`UI_HIDDEN` com pressão crítica e não substituir permanentemente os valores personalizados escolhidos pelo utilizador.

**C2 — Timeshift local recuperável depois de uma indisponibilidade prolongada. Recomendada, adaptada à arquitetura local.**

Esta versão usa `LocalTimeshiftSession` e não `TimeshiftDownloader`/`TimeshiftManager` upstream. Existem retries curtos, respeito por `Retry-After`, atualização de referências e prazos finitos. Quando se esgotam, a sessão passa a `FAILED`; o observador local no ViewModel apresenta uma mensagem, sem criar uma sessão substituta.

Adaptar a ideia do upstream: distinguir falta de rede de recusa do fornecedor; aguardar conectividade de forma cancelável; depois de uma cópia encerrada, renovar a sessão apenas se ainda pertencer ao mesmo perfil/origem/canal. Mostrar a lacuna real. Não insistir em 403/429/458 nem trocar para direto quando o utilizador está a ver conteúdo antigo sem uma política explícita.

**C3 — URLs HLS assinados: manter a implementação local. Já coberto no ponto principal.**

O upstream troca `java.net.URI.resolve` por resolução OkHttp para referências com caracteres especiais. O `LocalTimeshiftSession.resolve` local já usa OkHttp; a deduplicação usa sequência, e a recuperação refresca referências assinadas. Não substituir este percurso pelo downloader upstream nem remover a query/token. Acrescentar casos de regressão apenas ao mexer na recuperação.

### Fase D — zapping mais rápido sem enfraquecer as garantias locais

**D1 — Eliminar os 500 ms apenas numa pressão isolada. Recomendada após as fases anteriores.**

O `zapTo` local espera sempre 500 ms. `ZapTuneDelay.kt` upstream distingue uma pressão isolada de uma sequência rápida: a primeira pode sintonizar imediatamente; as seguintes são agregadas. Adaptar preservando o intervalo configurável de reabertura do mesmo canal, backoff do fornecedor e confirmação de libertação. Esta otimização não reduz a espera exigida pelo painel para A→B→A.

**D2 — Medir da tecla até à primeira imagem. Recomendada como complemento de diagnóstico.**

O Core novo acrescenta `ZapTiming` e `FirstFrameGate`. A versão local já tem diagnóstico por tune/source, rejeição de callbacks antigos e evento de primeira imagem. Aproveitar a medição desde a tecla e avaliar se falta alguma barreira de ordenação na medição; não substituir o sistema de ownership nem instalar uma segunda instrumentação por fotograma. Uma melhoria de medição não acelera a reprodução por si.

**D3 — Libertação mpv→Exo e confirmação real de abertura mpv. Já existem soluções locais.**

`retireMpvBeforeExo` local aguarda `mpvStopAndConfirmRelease()` e verifica a geração. `awaitMpvOutcome` usa `livePlaybackReady`, não a simples ausência do spinner. A correção upstream não deve substituir estas garantias. Rever os testes de pausa/cancelamento ao adaptar D1, sem presumir que a release resolve bloqueios temporários impostos pelo fornecedor.

## Não importar automaticamente

| Área anunciada | Decisão para esta App |
|---|---|
| Sports Hub, resultados, alertas, equipas e novos ecrãs | Opcional, fora dos lotes recomendados. Não é uma correção de streaming e acrescenta dados/pedidos/UI. Só com interesse explícito do utilizador. |
| SmartBuffer / buffer de filmes | Não aplicar ao HLS em direto. A mudança anuncia e implementa um percurso de VOD, diferente das reservas/pre-buffer/limites personalizados de live. Avaliar separadamente para arquivos se houver um problema medido. |
| Reformulação Stage/glass, tema claro e remoção de componentes antigos | Não importar em bloco. A App tem apresentação simplificada e ajustes próprios; um componente removido upstream pode continuar em uso aqui. |
| Filmes, séries, favoritos, histórico, meteorologia, animações, smartphone, OpenSubtitles | Fora do objetivo atual; não reintroduzir funcionalidades retiradas. |
| HLS master/AES, gravação manual por hora, FFmpeg de áudio, recuperação de arquivo, backups completos | Já existem implementações/adaptações locais relevantes. Não apresentar como inteiramente novo nem substituir sem comparar testes e contratos. |
| Reconexão mpv→Exo / Retry depois de desistir | Já existem percursos locais de retry e recuperação; manter e testar, sem copiar os controladores completos. |
| País nos nomes UK:/IT: e prefixos de categorias | Baixa prioridade. Não foi identificado benefício direto para a lista portuguesa; adaptar só se necessário, protegendo as chaves de personalização. |
| Abrir direto quando um diferido em curso deixa de chegar | Não aplicar como fallback genérico. Poderia repetir o problema anterior de escolher um programa e acabar no direto. Avaliar apenas um fim de arquivo confirmado, próximo do direto, com regra explícita. |
| Mais codecs / suposta cura dos cortes periódicos de áudio | Não demonstrado por esta atualização. `AudioOutputPolicy`/`AudioDynamics` mudam comentários no diff 67→68, sem uma nova solução de processamento para esses cortes. |

## Persistência e integração

Se forem adicionados `serverTimeZone` e `catchupCorrectionMin`, criar uma migração própria posterior à v50 local. Não copiar a numeração v49 nem substituir os schemas. Campos novos devem atravessar importação/sincronização, hashes, cópia, exportação/restauração e testes de migração.

Depois de uma futura alteração no Core, publicar um snapshot de fontes e atualizar o lock pelo fluxo existente. Uma compilação que usa outro Core não valida esta integração.

## Pendência futura preservada

`UPSCALING-BETA-01` continua em `MELHORIAS_FUTURAS_PENDENTES.md`, sem implementação autorizada. Estas releases não fornecem uma solução de super-resolução por hardware a importar. Avaliar APIs/GPU/boxes e ganho real em separado; aumentar buffer não cria detalhe na imagem.

## Critérios para começar a implementar

1. Começar pela Fase A; depois B, C e D em lotes independentes.
2. Testes de contratos, migração e backup ao tocar em persistência; testes de cancelamento/ownership ao tocar na reprodução.
3. Verificação de compilação/lint após cada lote que altere código.
4. Testes físicos de diferidos com +1 h, rede desligada/restabelecida no timeshift, A→B→A com espera configurada, Home/regresso e leitores externos.
5. Não prometer eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias.

Nesta análise não foram executados builds, testes nem ensaios em boxes; foram comparados fontes e contratos. As recomendações não equivalem a correções já aplicadas.

## Fontes primárias

- [OwnTV 5.2.0](https://github.com/ahXN00/OwnTV/releases/tag/v5.2.0)
- [Core 1.0.68](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.68)
- [Zapping e medição — PR 32](https://github.com/ahXN00/OwnTV_Core/pull/32)
- [Core: PlaybackEngines](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.68/player-core/src/main/java/tv/own/owntv/player/PlaybackEngines.kt)
- [Core: CatchupUrl](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.68/core/src/main/java/tv/own/owntv/core/epg/CatchupUrl.kt)
- [Core: LiveEpgReader/LiveArchiveUrls](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.68/core/src/main/java/tv/own/owntv/core/live/LiveEpgReader.kt)
- [App: TimeFormat](https://github.com/ahXN00/OwnTV/blob/v5.2.0/app/src/main/java/tv/own/owntv/ui/format/TimeFormat.kt)
- [Android: ComponentCallbacks2](https://developer.android.com/reference/android/content/ComponentCallbacks2)
