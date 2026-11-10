# Implementação seletiva UP70-A — Core 1.0.70

Data: 08/11/2026. Âmbito autorizado: UP70-A completo, U01–U05. Estado: implementação e validação automatizada concluídas; aceitação física na box pendente.

## Resultado funcional

1. Diagnósticos: mascarar MAC na query e credenciais no formato Xtream curto, conservando as regras anteriores. Só muda a representação para diagnóstico; não muda o pedido enviado.
2. Sincronização Xtream: recusa explícita de autenticação termina o sync antes de atualizar capacidades, importar ou apagar catálogo. O código 513 só é convertido neste contexto de conta/API. Rede, resposta inválida e campo de autenticação ausente continuam distintos de credenciais rejeitadas; cancelamento propaga.
3. Limites: anúncio do painel só é escrito atomicamente se não existe medição. Uma medição que termine durante o sync prevalece. O valor efetivo é relido. Evidência de catálogo substituível por origem/conta fica separada de recusas realmente observadas durante playback; um anúncio já ultrapassado deixa de ficar preso no conjunto de recusas.
4. Ordenação: um estado de ocupação por fonte/tipo/passe acompanha inserts, alterações e mudanças de posição entre chunks, incluindo aliases M3U de nomes antigos. IDs e hashes existentes são preservados. Os novos inserts só acrescentam ocupação; não se inventam IDs de base de dados. Ranges são estacionadas fora de TODO o domínio Int e restauradas na mesma transação.
5. HLS: capturar a URL canónica e o contexto imutável no owner/source correto; aprender apenas de resposta bem-sucedida ao pedido principal. Variantes, segmentos, chaves e respostas de erro não ensinam a regra. O contexto é capturado depois da decisão de routing, preservando a prova TS de três pacotes em hosts mistos.

## Ficheiros alterados/adicionados

Base Core: `C:/Users/renat/Downloads/OwnTV_Core/`.

| Bloco | Produção |
|---|---|
| A1 | `core/src/main/java/tv/own/owntv/core/network/HttpClient.kt` |
| A2 | `core/src/main/java/tv/own/owntv/core/parser/XtreamClient.kt`, `core/src/main/java/tv/own/owntv/core/sync/XtreamSyncer.kt` |
| A3 | `core/src/main/java/tv/own/owntv/core/database/dao/SourceDao.kt`, `core/src/main/java/tv/own/owntv/core/live/ConnectionLimits.kt`, `core/src/main/java/tv/own/owntv/core/player/LiveSessionLimit.kt`, `core/src/main/java/tv/own/owntv/core/repository/ActiveSources.kt`, `player-core/src/main/java/tv/own/owntv/player/LiveStreamQuirks.kt` |
| A4 | `core/src/main/java/tv/own/owntv/core/sync/SyncSupport.kt`, `core/src/main/java/tv/own/owntv/core/sync/M3uSyncer.kt`, novo `core/src/main/java/tv/own/owntv/core/sync/StoredRows.kt`; `ChannelDao.kt`, `MovieDao.kt`, `SeriesDao.kt` e novo `PositionParking.kt` em `core/src/main/java/tv/own/owntv/core/database/dao/` |
| A5 | `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt`, novo `player-core/src/main/java/tv/own/owntv/player/ChannelResponseContext.kt` |

App: `app/src/main/java/tv/own/owntv/OwnTVApp.kt` perde a ligação que transformava anúncios substituíveis do catálogo em recusas permanentes da sessão. O player consulta o registo efetivo de catálogo e mantém em separado a sua evidência observada. `core-source-lock.json` e ZIP imutável integram os inputs finais.

Mantida a API de loaders/deferred baseada em Map: os carregadores passam a devolver StoredRows. Xtream e Stalker usam o mesmo estado por passe sem alterar os seus pedidos, escopos ou política de fallback. Os DAOs de filmes/séries são consumidores existentes da API genérica: corrigir a transação partilhada não reativa processamento VOD no modo só TV.

## Garantias e limites de estacionamento

`PositionParking.OFFSET = 4_294_967_296L`. Posições persistidas e destinos são Int; binds e operações temporárias são Long/SQLite INTEGER. O intervalo temporário é [2_147_483_648, 6_442_450_943], disjunto de todo o domínio Int. Não se copiou a sentinela upstream de 1_000_000_000, que colidiria com posições válidas.

Antes de escrever, validar tamanhos das listas, spans ordenados sem sobreposição e destinos por aritmética Long dentro de Int. A transação verifica que a fonte não tem posições já fora do domínio; uma corrupção assim não pode ser confundida com estacionamento nosso. A restauração é restrita à mesma fonte e intervalo temporário. Exceção/cancelamento deve provocar rollback da transação Room; nenhum estacionamento é persistido isoladamente.

`content_order`, prioridades manuais, agrupamento, nomes, configurações por canal, schema 51 e formato de backup 26 não mudam. O estado em memória só avança após as escritas do chunk concluírem. Uma falha de escrita aborta o passe; não se publica um estado de sucesso fictício.

## Validação

- Core: **439 testes JVM dirigidos**, zero falhas/erros/ignorados (network, sync, parser, database, live, player e repository).
- Player: **633 testes JVM**, suite completa, zero falhas/erros/ignorados.
- `:core:compileDebugAndroidTestKotlin`: aprovado. O teste `Up70XtreamAccountAndroidTest` cobre o JsonReader real do Android (auth numérico/string/booleano, ausente/indeterminado e JSON inválido), mas **não foi executado em dispositivo**. O teste JVM de recusa explícita injeta o resultado de conta na fronteira I/O do sync; o caso HTTP 513 atravessa o cliente HTTP real. Ambos verificam zero operações DAO após rejeição.
- O primeiro ensaio JVM do parser revelou os stubs de `android.util.JsonReader` do ambiente PC. Esse ensaio foi movido para androidTest; não se alterou o parser de produção para contornar o ambiente de teste.
- App: compilação `compileStandardReleaseKotlin`, **470 testes JVM** sem falhas/erros/ignorados e `lintVitalStandardRelease` aprovados. Relatório vital: **No issues found.** Build conjunto com `--dependency-verification=strict -PowntvUseCoreSnapshot=true`: **BUILD SUCCESSFUL in 7m 40s**; 143 tarefas (68 executadas, 42 da cache, 33 atualizadas).
- Total relevante aprovado: **1542 testes JVM** (439 Core + 633 player + 470 App). A execução dirigida final do Core terminou em 35 s; não equivale a declarar aprovada a suite geral com os dez testes de gravação abaixo.
- Suite geral do Core: 1437 testes na execução inicial, 12 falhas: duas dos novos testes por dependerem dos stubs Android (corrigidas e aprovadas na execução dirigida) e dez em RecordingEngineOperationsTest. Estes dez testes pertencem ao percurso de gravação, não alterado neste lote. As mesmas dez falhas, com os mesmos nomes de teste, foram reproduzidas nos 13 testes dessa classe usando as classes de produção do snapshot anterior `c46ccf…`. O registo de carregamento de classes confirmou o JAR anterior, não o JAR modificado. Prova em `recording-baseline-comparison.json` e `baseline-classloading-confirmed.log`. Isto identifica uma falha anterior na validação dessa classe; não demonstra por si só defeito na gravação física. Não foi suprimida nem corrigida fora do âmbito. Depois da comparação, a execução dirigida normal foi repetida e aprovada para repor relatórios do Core atual.
- O lint integral do Core não foi reexecutado. Mantém-se o registo histórico de cinco erros anteriores de pluralização em recursos que este lote não altera; não se declara aprovação desse lint integral.

Novos/ampliados testes: `HttpClientDiagnosticPrivacyTest`, `XtreamSyncAccountTest`, `M3uSyncerLiveOnlyTest` (recusa Xtream antes de qualquer operação DAO), `LiveSessionLimitTest`, `Up70BatchedOrderTest`, `Up70PositionSqlTest`, `ChannelResponseContextTest`.

A fixture de ordenação cobre inserções/alterações entre chunks e 160 seeds deterministas com reordenação, posições repetidas/negativas e remoções no feed. Preserva os IDs dos itens existentes. As queries dos DAOs são lidas dos fontes e executadas em SQLite/JDBC em memória: destinos cruzados, posição válida 1_000_000_000, limite Int, outra fonte, rollback e anúncio após medição. Isto testa SQL real e lógica Kotlin, mas não substitui executar o DAO Room numa box.

## Compilação e segurança das dependências

O primeiro build autónomo do Core expôs dois artefactos AAPT2 9.5.1 ainda sem checksum registado (POM e Windows JAR). Comparados com o repositório HTTPS oficial Google, com prova em `aapt2-official-checksums.json`, e adicionados exatamente esses dois hashes aos metadados da App e Core. Verificação estrita mantida, sem wildcards novos ou aceitação de hash divergente. Nenhuma versão de ferramenta foi alterada neste lote.

O inventário i18n classifica apenas novos literais técnicos (SQL, regex e protocolo/diagnóstico). Não se suprimiu lint nem se criou baseline para ocultar erros.

## Integração e cópias

Pasta anterior e provas: `C:/Users/renat/Downloads/OwnTV-up70a-2026-11-08-220242/`. Contém o snapshot anterior integral, hashes dos inputs, estados Git anteriores, cópias dos ficheiros da App e metadados, e logs de validação. O checkout já tinha alterações locais extensas; nenhum reset/restauro global foi efetuado.

Snapshot publicado: **1107 inputs**, SHA-256 `c1f69fa88589d6066380d0479a1222f6941347907ace56c1f98a37239ab7e5f1`. Comparação integral entre checkout, ZIP e extração usada na App aprovada. Face ao baseline: 18 inputs modificados, nove adicionados, zero removidos. Prova em `integration-input-proof.json`.

`Up70XtreamAccountAndroidTest.kt` é o sexto novo ficheiro de testes; os outros novos inputs são três helpers de produção e cinco testes JVM. Os testes Android foram apenas compilados; não se gerou APK de teste nem se instalou qualquer app.

## O que permanece pendente

- UP70-B: writer/preload EPG, com medição e teto de memória; não implementado.
- UP70-C: atualização libmpv 2026.11.0 e respetiva regressão A/V; não implementado. Mantém-se 2026.10.4.
- Fases 4A/4B do plano HLS anterior: separadas deste lote.
- Validação física: refresh com ordenação e prioridades pessoais; backup/restauro; HLS saudável, host misto e zapping/reabertura; duas contas no mesmo painel; recusa de autenticação sem perda de catálogo. Não usar credenciais reais em logs.

Sem APK, assinatura, instalação, commit ou push. Estes testes não medem percentagens de velocidade/robustez nem demonstram resolução de restrições de reabertura impostas pelo fornecedor. Volume continua a 100%; HLS-only/exceções manuais, cooldown, drain, settings persistidos e modo TV Avó são preservados.
