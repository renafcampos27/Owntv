# Core 1.0.70 e libmpv 2026.11.0 — análise técnica, council e plano seletivo

Data: **08/11/2026**. Projeto: **OwnTV Android TV personalizado**.
Estado da auditoria original: análise concluída sem implementação. **Atualização após autorização: UP70-A (U01–U05) implementado e validado por compilação Release, 1542 testes relevantes e lint vital da App; aceitação física pendente.** UP70-B foi autorizado: B1 implementado e validado por compilação Release, 687 testes JVM dirigidos/App e lint vital; B2 medido e não integrado por falhar o gate em feeds parciais. UP70-C autorizado: libmpv 2026.11.0 integrado, 633 testes do player aprovados; Release, lint, junção nativa e R8/DEX validados. Ver [implementação UP70-C](IMPLEMENTACAO_UP70_C_2026-11-08.md). O texto da auditoria conserva o referencial inicial; consultar o [registo de implementação UP70-A](IMPLEMENTACAO_UP70_A_2026-11-08.md) e o [registo UP70-B](IMPLEMENTACAO_UP70_B_2026-11-08.md) para ficheiros, testes e limites atuais.

O Core efetivamente integrado mantém 1109 inputs, com novo snapshot `ecc95fb3e16dad1b8d291d744cee3e4ee96d0229a5d2cfa78992c7c1f3722561`. libmpv passa a 2026.11.0; Media3 1.11.1, schema 51 e backup 26 mantidos. Sem APK ou instalação.

## 1. Decisão executiva

**Recomenda-se integração seletiva, não a substituição integral do Core.** Há cinco correções pontuais justificadas para o primeiro lote: redação de credenciais nos diagnósticos, autenticação Xtream recusada, preservação do limite de ligações medido, posições de catálogo entre lotes e aprendizagem HLS restrita ao pedido do canal. A seguir, otimização da escrita EPG e avaliação de preload com memória limitada. O libmpv 2026.11.0 merece um lote próprio, reversível e com regressão A/V.

Não importar resets de ordenações, migrações de schema com números coincidentes, o segundo produtor de histórico EPG, sondagens GET adicionais no arranque mpv ou três retries de decoder globais. Não confundir melhorias já existentes no fork com novidades por implementar.

Esta é uma aprovação técnica para **propor** trabalho futuro. Não autoriza implementação automática de todas as opções. Os lotes chamam-se **UP70-A/B/C** para não se confundirem com a fase 4A/4B de timeshift do plano HLS anterior, que permanece pendente.

## 2. Bases verificadas e método

### 2.1 Estado local de referência da auditoria inicial

| Elemento | Estado verificado |
| --- | --- |
| App | `C:/Users/renat/Downloads/OwnTV-main` |
| Core de desenvolvimento | `C:/Users/renat/Downloads/OwnTV_Core` |
| Core realmente consumido pela App | Snapshot ZIP bloqueado por `core-source-lock.json`; **1098 inputs** |
| SHA-256 desse snapshot | `c46ccf95335e84fd58fd83a5a4fa21bbc461482987ac2b36ee6aecdce3eaf361` |
| Identificador interno do Core | `1.0.57-own.20261011`; **não significa equivalência integral** a uma release upstream |
| Ferramenta de compilação | **AGP 9.5.1** na App/Core; preservar a correção recente |
| Media3 / libmpv em uso | **1.11.1 / 2026.10.4** |
| Room local / backup local | **Schema 51 / formato 26**, com contratos próprios |
| Contratos locais relevantes | Apenas HLS/manual exceptions, owner/generation e drain, cooldown do mesmo canal, volume 100%, TV Avó, prioridade/ordem manual de alternativas, EPG visual separado da correção de diferidos, preferências por canal e backup portátil |

Os inputs do Core foram comparados integralmente com o ZIP bloqueado antes da revisão. A árvore já tinha alterações locais; não foi tratada como um checkout limpo nem restaurada a um commit antigo. A auditoria usa fontes atuais, não os estados históricos dos documentos como prova de implementação.

### 2.2 Upstream congelado

| Comparação | Commits de referência | Resultado |
| --- | --- | --- |
| Core 1.0.69 → 1.0.70 | `1de78b1e2690c22b50c060e986bce221454bf880` → `9de56d7106491e8dea8e37636ff5df9a69cce156` | **169 commits; 447 ficheiros diferentes: 304 modificados e 143 novos** |
| libmpv 2026.10.4 → 2026.11.0 | `f2b51ad0fa53ed21321b6a4bda7d236ecff37ef1` → `5c09e5910bc9844a0727d46fca070756f02aa14a` | **18 commits; 10 ficheiros diferentes** no repositório do wrapper/build |
| Motor mpv incorporado | `3186d369f9f090cd1363be0ac46a037824b702c6` → `914a11c30c6a1710fe1f4333654696f2dfb3c3af` | **286 commits / 191 ficheiros**, conforme comparação do motor; superfície de mudança superior ao patch DV |

A API compare do Core devolve no máximo 300 ficheiros. Foram descarregados os quatro snapshots completos por SHA e inventariadas as diferenças integrais. Entre os 447 ficheiros Core alterados upstream, 267 têm uma versão local personalizada diferente, 166 estão ausentes e 14 coincidem com a versão antiga após normalizar fins de linha. **Isto é inventário textual, não contagem de bugs nem prova de ausência funcional**: o fork pode resolver o mesmo problema com outra implementação.

Fontes primárias: [release Core 1.0.70](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.70), [comparação Core](https://github.com/ahXN00/OwnTV_Core/compare/core-1.0.69...core-1.0.70), [release libmpv 2026.11.0](https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.11.0) e [comparação libmpv](https://github.com/ahXN00/OwnTV_libmpv/compare/v2026.10.4...v2026.11.0).

### 2.3 Multiagentes e council

Participaram três agentes especializados: **reprodução/HLS/lifecycle/DRM**, **EPG/sincronização/persistência** e **mpv/FFmpeg/artefactos nativos**. O agente principal auditou também privacidade, perfis, contratos de UI, pendências e integração/build. Houve revisão cruzada dos resultados, não só soma das sugestões.

A revisão corrigiu recomendações potencialmente perigosas: o estacionamento transacional é parte obrigatória da correção das posições; o preload upstream não respeita o teto dentro de um canal gigante; os hashes Maven/GitHub são diferentes; o native update não é um patch DV isolado; e o reset de ordenações não está na migração 51→52, mas em startup/DAO/backup.

Evidência estática e leitura de AAR/ELF distinguem-se de testes JVM/Room e de reprodução real. Só foi executado um **modelo SQL sintético em memória**, explicitado em U04. Não foram executados testes Android, benchmarks de boxes ou os testes de aplicação citados nas releases. As alegações upstream de **14× no EPG e 4× no refresh não são ganhos medidos neste projeto**.

## 3. Matriz consolidada de decisões

| ID | Família | Decisão nesta análise | Prioridade/limite |
| --- | --- | --- | --- |
| U01 | Ocultar MAC Stalker e URLs Xtream curtas | Recomendar correção mínima | UP70-A1; não mudar os pedidos |
| U02 | Autenticação Xtream recusada / HTTP 513 | Recomendar, restrita à API/login | UP70-A2; ausência de resposta não é senha errada |
| U03 | Não substituir limite de ligações medido no sync | Recomendar correção mínima | UP70-A3; sem ignorar limites nem sondagens novas |
| U04 | Ocupação/posições em refresh por lotes | Recomendar conjunto completo adaptado | UP70-A4; estado mutável + estacionamento/rollback |
| U05 | Aprendizagem HLS top-level e URL canónica | Recomendar correção mínima | UP70-A5; owner e resposta 2xx, sem GET extra |
| U06 | Escrita EPG por lote | Implementada adaptação mínima: remover transação exterior redundante | Room já prepara uma vez por lote; ver registo UP70-B |
| U07 | Preload de hashes EPG por cursor | Avaliado; não integrado como default | Ganha em feeds completos, mas antecipa leitura/memória desnecessária em feeds parciais; manter lazy |
| U08 | libmpv 2026.11.0 | Implementado em UP70-C, com reversão preservada | Release, JNI/DEX e merge nativo aprovados; aceitação física pendente |
| U09 | Templates M3U `${(b)…}` / `${(e)…}` | Opcional útil para listas que os usem | Compatibilidade separada; não altera Xtream |
| U10 | Retry/numeração de M3U | Opcional; retry exige teste de replay | Não repetir corpo já consumido ou recusa HTTP |
| U11 | Stalker redirects/auth/limite adaptativo/token hash | Adaptar só se esse portal for usado | Não reintroduzir VOD/backfill nem credenciais entre hosts |
| U12 | Prioridade explícita de origens EPG | Opcional perante feeds discordantes | Não é correção universal de horários |
| U13 | Herança de listas de perfis com PIN | Recomendar guard reduzido se houver perfis protegidos | Não copiar todo o sistema novo de locks |
| U14 | Remover índices redundantes | Medir antes e migrar pelo schema local | Não copiar migrations/Entity upstream |
| U15 | Três rebuilds de decoder | Testar primeiro; não ativar globalmente | Default local 15 s, alternativa 7 s |
| U16 | GET de sondagem mpv após EOF | Rejeitar implementação literal | Release/owner/cancelamento insuficientes |
| U17 | Reabrir live após pausa de 60 s | Opcional, dependente da validade da janela | Pode perder posição válida e criar nova sessão |
| U18 | Audio focus / notificações | Investigar com logs antes de mudar | Sem duck manual/telefonia; volume da App 100% |
| U19 | FairPlay / inline ClearKey | Opcional e dependente de suporte adicional | Não é suporte FairPlay Android universal |
| U20 | Limpeza persistida de nomes | Rejeitar cópia direta; apresentação apenas como opção futura | HD/HEVC entram na identidade/prioridade local |
| U21 | Reset de ordens / semântica de backup 26 | Rejeitar | Apagaria escolhas locais e ordens de backups antigos |
| U22 | Segundo produtor de histórico EPG | Rejeitar cópia literal | Local já tem histórico; upstream tem substituição não atómica |
| U23 | XMLTV truncado, encoding Xtream e resync ao editar | Já cobertos localmente | Preservar soluções locais, em parte mais fortes |
| U24 | Controlos, arranque, categorias e alternativas no leitor | Parcialmente existentes / restantes opcionais | Preservar TV Avó, canal fixo e long-press personalizado |
| U25 | Sports, filmes/séries, mobile, PiP e auto-sync local | Não importar neste projeto | Funcionalidades removidas/excluídas ou sem necessidade demonstrada |
| U26 | Upscaling beta por hardware | Manter pendente | libplacebo/buffer novo não ativa upscaling no percurso direto |

São **26 famílias de avaliação**, não 26 bugs confirmados ou 26 implementações autorizadas. U01–U08 formam o roteiro principal; U07 continua condicionada a medição. As opções restantes não entram automaticamente nesse roteiro.

## 4. UP70-A — correções pontuais sem migração

### U01 / A1 — Redação de credenciais nos diagnósticos

**Constatação:** `HttpClient.redactUrl` local mascara query comum, `/live/user/pass/…`, timeshift e userinfo, mas não inclui `mac=` nem o formato curto `/user/pass/123.m3u8`. Há consumidores de playback/diagnóstico desse helper. O catálogo já usa `diagnosticOrigin` e mensagens controladas, mais seguras que logar URLs completas; preservar essa proteção.

**Fontes:** local `core/src/main/java/tv/own/owntv/core/network/HttpClient.kt:150–177`; upstream, mesmo ficheiro, `:141–169` e `core/src/test/java/tv/own/owntv/core/network/RedactUrlTest.kt`. [Patch no contexto upstream](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/network/HttpClient.kt#L141).

**Alterar estritamente:** acrescentar `mac` ao padrão existente e uma regra de URL curta que reconheça o segmento final de ID numérico, incluindo extensão/query e URL embutida em texto. Compilar os padrões uma vez. Não substituir o helper inteiro, não voltar a logar erro bruto/URL completa no catálogo e não alterar URL, headers ou credenciais que são enviados ao fornecedor.

**Remover/adicionar:** não remover os padrões existentes. Adicionar apenas os casos em testes locais de redação; se houver novo ficheiro, marcá-lo como novo no lote. Não é necessária preferência nem migração.

**Aceitação:** casos sintéticos `.invalid` com MAC maiúsculo/minúsculo, URLs longas/curtas, query assinada, userinfo, timeshift e texto com URL; nenhum segredo sintético permanece no resultado. A URL efetivamente transmitida fica byte-equivalente. Não usar contas reais nos testes.

### U02 / A2 — Credenciais Xtream explicitamente recusadas

**Constatação:** local `sync/XtreamSyncer.kt:35` obtém `fetchAccountDetails`, mas não verifica `details.authOk` antes de atualizar metadados e sincronizar listas. Upstream acrescenta `requireAccepted` (`:37/:528`). Uma recusa API pode ser tratada como resultado de catálogo vazio/sucesso; não se afirma que todos os casos apagam a base.

**Alterar:** em `core/.../sync/XtreamSyncer.kt`, rejeitar **apenas `authOk == false` explícito**, imediatamente após obter os detalhes, antes de quaisquer writes/prunes dessa sincronização. Reutilizar o erro tipado/renderer existente. Se necessário, adaptar `core/.../parser/XtreamClient.kt` e `core/.../util/ErrorMessages.kt` só no percurso API/login para HTTP **513** não normalizado; ausência do campo/resposta, timeout e erro TLS continuam classificados pela causa observada.

**Não fazer:** classificar HTTP 513 de HLS/segmento como senha errada; acrescentar login antes de cada canal; duplicar o editor de source ou retries de streaming. Os tokens de geração/cancelamento e a última lista válida são preservados.

**Testes:** auth0/auth1 numérico/string, campo ausente, resposta nula, cancelamento, erro de rede e código513 fora/dentro do contexto correto; comprovar que a recusa explícita não escreve timezone/limite, não publica sucesso e não elimina o catálogo bom. Corrigir a senha e atualizar deve recuperar normalmente. [Referência upstream](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/sync/XtreamSyncer.kt#L35).

### U03 / A3 — Preservar o limite de ligações já medido

**Constatação adicional do council:** o sync local chama `updateMaxConnections` incondicionalmente (`XtreamSyncer.kt:45`), mas existe `maxConnectionsProbedAt` e o percurso de medição. Pode substituir a medição pelo número anunciado pelo fornecedor e deixar o timestamp de medição a apresentar esse valor como medido.

**Alterar:** `core/.../database/dao/SourceDao.kt` deve ter update atómico que só aceite o valor anunciado quando `maxConnectionsProbedAt <= 0`, como `updateMaxConnectionsUnlessMeasured` upstream (`:56`). `XtreamSyncer` deve usar esse contrato e não voltar a impor em memória a regra single-session a partir do anúncio quando uma medição válida a contradiz. Ler/usar a autoridade efetiva sem criar uma corrida entre sync e medição.

**Preservar:** `SourceRepository.updateSource` local (`:109–119`) já invalida limite e timestamp quando muda a identidade da ligação. Preservar esse reset por configuração, os claims/drain e o limite efetivo do fornecedor. Não importar `ignoreConnectionLimit`, não aumentar sessões nem medir automaticamente ao abrir um canal. Se um teste demonstrar estado single-session antigo que não pode ser limpo pela API atual, resolver de forma localizada antes de anunciar a correção completa.

**Testes:** sem medição aceita anúncio; com medição conserva valor e timestamp; medição conclui antes/depois de sync; edição de servidor/utilizador/senha invalida a medição antiga; outro source/conta não herda o resultado. Abertura/reabertura A–B–A e captura mantêm o mesmo orçamento de sessões. [DAO upstream](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/database/dao/SourceDao.kt#L49).

### U04 / A4 — Posições de catálogo consistentes entre e dentro dos lotes

**Duas causas distintas:** `SyncSupport.kt:488` local reconstrói a ocupação a partir do snapshot inicial em cada lote. Além disso, `ChannelDao.applyPositionRanges:217` aplica ranges sequenciais diretamente: uma linha movida por uma range pode cair na range seguinte e ser movida novamente. Corrigir só o primeiro problema deixa o segundo ativo.

**Prova limitada:** um modelo SQLite sintético, em memória, reproduziu **8 posições erradas** numa fixture de 20 itens com inserções em três pontos e lotes de cinco. O artefacto é `data_order_sql_model.json`; não é um teste da App/Kotlin e não mede desempenho da box.

**Alterar estritamente:**

1. `core/.../sync/SyncSupport.kt`: estado de linhas/ocupação único por passe e tipo; atualizar após insert/update/relocate e transportar o mesmo estado para recuperação de chaves truncadas. Adaptar `StoredRows`, sem perder o filtro TV-only nem o guard `lookup` vazio que evita `legacyStableKey` desnecessário.
2. `M3uSyncer.kt`, `XtreamSyncer.kt` e `StalkerSyncer.kt`: ajustar apenas os loaders/adapters que consomem a API; não reintroduzir processamento VOD.
3. `core/.../database/dao/ChannelDao.kt`: estacionar temporariamente os destinos e desempacotar/restaurar no fim, numa única transação, para que ranges não voltem a atingir linhas movidas. Um helper `PositionParking.kt` seria **novo localmente**, não um ficheiro já implementado.
4. Os restantes DAOs só entram se forem consumidores reais/necessários à API partilhada; a compilação deve continuar válida sem anunciar uma reintrodução de filmes/séries.

**Cuidado que impede copiar cegamente:** `sortOrder` e deltas são **Int** no local; a sentinela upstream é `1_000_000_000`. Provar domínio/colisões e overflow em Kotlin antes de bind64bit SQLite. Se o domínio não garantir um intervalo livre, redesenhar o estacionamento em vez de depender da constante. Rollback tem de deixar zero posições estacionadas.

**Não alterar:** `content_order`, prioridades manuais das versões, IDs estáveis, hashes de conteúdo, agrupamento, nomes ou preferências. Esta correção é de posições do catálogo do fornecedor, não uma migração de ordenação manual.

**Aceitação:** fixture multi-lote + ranges com destinos cruzados, posições duplicadas/negativas/limiteInt, aleatorização por seed fixa, source isolada, insert/update/delete e falha/cancelamento a meio da transação. Comparar com a ordem esperada e confirmar persistência das customizações após refresh e backup/restauro. Referências: [SyncSupport](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/sync/SyncSupport.kt#L678), [ChannelDao](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/database/dao/ChannelDao.kt#L234) e [PositionParking](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/core/src/main/java/tv/own/owntv/core/database/dao/PositionParking.kt).

### U05 / A5 — Aprender HLS só do pedido do próprio canal

**Lacuna:** o interceptor local em `player-core/.../LivePreviewEngine.kt:3318–3328` também recebe variantes/chaves/segmentos. Uma resposta HLS de uma variante sem extensão pode ensinar indevidamente a lição ao host. A URL final com extensão HLS também não prova sucesso quando a resposta é um erro HTTP.

**Adaptação mínima:** capturar, no owner/source correto e fora da thread de rede, o `HttpUrl` canónico do pedido do canal. Só aprender quando `request.url` corresponde a esse URL, a resposta é bem-sucedida e o conteúdo/redirecionamento prova HLS. Uma URL de outro canal, variante, chave ou erro não pode criar essa memória. O guard de propriedade atual é mantido; comparar URLs canónicas evita diferenças puramente de normalização.

**Ficheiro:** apenas o interceptor/captura correspondente em `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt`; testes de host misto/ownership existentes a ampliar. [Critério top-level upstream](https://github.com/ahXN00/OwnTV_Core/blob/9de56d7106491e8dea8e37636ff5df9a69cce156/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt#L3143).

**Preservar:** caches URL/host locais com TTL/limite, prova TS por três pacotes de 188 bytes, host misto, HLS-only/exceções manuais, Media3, `PlaybackHttpCalls`, body/drain, orçamento, cooldown e fase3A. **Não importar a aprendizagem global de segmentos recusados**, que ainda existe no upstream e foi retirada localmente.

**Aceitação:** top-level `.ts`→HLS ensina; variante sem extensão/chave/segmento não ensinam; `.m3u8` explícito não prova redirecionamentoTS; encoding equivalente é reconhecido; 403/404/410 não criam nem apagam lição indevida; callback deA depois deB não ensinaB. Mesmos requests e timings de política: **zero GET extra, zero espera/retry novos**.

## 5. UP70-B — otimização EPG sem alterar horários ou o schema

### U06 / B1 — Uma transação e um statement por lote: implementado

**Revisão após autorização:** a análise dos fontes do Room 2.8.5 e do KSP atual revelou que `@Insert` já reutiliza uma instrução por lote. O `EntityInsertAdapter.insert(Iterable)` faz um `prepare`, bind/step/reset por row e `close`; o `performSuspending(db,false,true)` gere a transação IMMEDIATE e invalidação. A melhoria necessária é remover o wrapper exterior `db.transaction { epgDao.upsertProgrammes(batch) }` em `EpgRepository.storeProgrammes`, ficando só a chamada DAO. Evita o savepoint redundante sem introduzir SQL/binds/notificações manuais.

As oito colunas locais, `nullif(id,0)`, REPLACE, hashes, IDs e nulos ficam no adapter gerado existente. Nenhuma alteração de schema, `EpgDao`, `RoomTransaction`, horários, source/configuração, mutex, parser, cache ou prune. As outras transações que abrangem várias operações foram conservadas.

Testes do adapter KSP real em SQLite/JDBC e testes Android do DAO/Flows/rollback/cancelamento adicionados. A compilação Android não demonstra execução na box. Ver [implementação, provas de código e limites UP70-B](IMPLEMENTACAO_UP70_B_2026-11-08.md). A proposta inicial de writer manual de oito colunas foi substituída pela solução menor depois de confirmar o comportamento real da dependência; não há vantagem em duplicar o SQL gerado só para obter o mesmo transaction/statement.

### U07 / B2 — Preload EPG limitado durante a leitura

**Avaliação executada:** protótipo SQLite limitado, cinco testes de segurança e três cenários com três repetições. Não integrado: um feed parcial de 12/240 canais passa de 4800 para 96000 linhas lidas/retidas, antecipando trabalho e memória. Os cenários completos ganham tempo no modelo, mas não cumprem uma melhoria universal de arranque/memória. O tracker primitivo/lazy de produção permanece igual. Scripts, números e limites em [UP70-B](IMPLEMENTACAO_UP70_B_2026-11-08.md). O texto seguinte conserva os critérios para eventual revisão adaptativa futura.

O local já tem `ProgrammeHashTracker.kt:14`, hashes primitivos e `epgHashesForChannelBounded` com `remaining + 1`. O upstream faz uma leitura ordenada por source/canal/início em vez de várias queries por canal, mas verifica o limite só ao mudar de canal (`EpgRepository.kt:1067`). Um canal enorme pode ultrapassar o orçamento antes dessa mudança.

**Decisão:** protótipo e medição antes de integrar. Alimentar o tracker local, sem o substituir, por um cursor ordenado. Validar o limite **dentro** da leitura, sem materializar uma source inteira. Se um canal exceder o saldo, tratá-lo como não acompanhado/write-through; nunca usar o prefixo parcial para apagar programas. Conservar a marca de canais efetivamente alcançados no feed.

**Ficheiros candidatos:** `repository/EpgRepository.kt`, `repository/ProgrammeHashTracker.kt` e `database/dao/EpgDao.kt` apenas onde a consulta/contrato o exigir. Não criar um segundo cache global nem uma migração por defeito. A chave natural existente pode servir a leitura; confirmar plano/índice antes de acrescentar outro.

**Aceitação:** canal maior que todo o limite, zero/limite exato, canais pré-carregados ausentes no feed, truncagem, duplicados, alteração concorrente e memória máxima. Integrar só se reduzir queries/tempo sem aumentar materialmente memória/arranque ou comprometer prune. Caso contrário, manter o lazy loading atual. Não associar o ensaio a um ganho garantido de14×.

## 6. UP70-C / U08 — libmpv 2026.11.0 em lote isolado

**Atualização após autorização:** integrado sem mudança de políticas Kotlin/Java. Artefactos Maven novamente descarregados e hashes reconfirmados; símbolos, ABI, assinaturas JNI FFmpeg e inventário nativo revistos. AAR novo usado pelo build e 633 testes do player aprovados. Ver [registo UP70-C](IMPLEMENTACAO_UP70_C_2026-11-08.md) para prova de merge/R8 e resultado final Release. Os parágrafos seguintes conservam a análise inicial de risco e os gates físicos; referências a 10.4 como «em uso» descrevem o momento dessa auditoria.

### 6.1 O que efetivamente muda

- **FFmpeg mantém 9.0.2**. O patch novo seleciona o decoder MediaCodec Dolby Vision para **HEVC com perfil 5 e decoder compatível**. Evita o caminho de cores incorretas nesse cenário; não cria suporte DV em boxes sem decoder e não converte DV em SDR.
- mpv muda286 commits. Há alterações comuns em áudio/vídeo, inicialização/seleção de tracks, EOF de cadeias lazy, temporização do primeiro frame, demux e resolução de referências após redirects. **Não tratar o update como um patch isolado de cores.**
- `ao_audiotrack` e `vo_mediacodec_embed` não mudam nesse intervalo, mas isso não significa que todo o caminho A/V fique igual: `player/audio.c`, `player/video.c` e filtros comuns mudam.
- libplacebo 7.360.1→7.387.0, harfbuzz 14.5.0→15.0.0, freetype 2.14.3→2.15.0 e fribidi 1.0.17→1.0.18. Relevância para GPU fallback/texto; a saída hardware principal continua direta.
- Os patches HLS, HTTP/MediaCodec anteriores, espera no underrun e estimativa de FPS já existem em 2026.10.4. O patch mpv0001 mudou apenas contexto por uma linha RTP: **não é uma nova correção de HLS**.

Referências: [changelog congelado](https://github.com/ahXN00/OwnTV_libmpv/blob/5c09e5910bc9844a0727d46fca070756f02aa14a/CHANGELOG.md#L5), [dependências](https://github.com/ahXN00/OwnTV_libmpv/blob/5c09e5910bc9844a0727d46fca070756f02aa14a/buildscripts/include/depinfo.sh), [patch DV5](https://github.com/ahXN00/OwnTV_libmpv/blob/5c09e5910bc9844a0727d46fca070756f02aa14a/buildscripts/patches/ffmpeg/0007-mediacodec-dolby-vision-profile-5.patch) e [comparação do motor mpv](https://github.com/mpv-player/mpv/compare/3186d369f9f090cd1363be0ac46a037824b702c6...914a11c30c6a1710fe1f4333654696f2dfb3c3af).

### 6.2 Inspeção dos artefactos, JNI e ABI

Foram descarregados e lidos os AAR na pasta de auditoria; não foram carregados/executados no Windows ou adicionados ao projeto.

| Elemento | Resultado observado |
| --- | --- |
| AAR 2026.10.4 local | 31 944 195 bytes |
| AAR 2026.11.0 | 32 179 902 bytes; aumento235 707 bytes (~230 KiB); não é medição do APK |
| ABI/minSDK | Ambos: ARMv7, ARM64, x86_64; API 26 |
| `classes.jar` | Nove classes Java/Kotlin byte-idênticas; ZIP do JAR difere no empacotamento |
| ELF | Mesmos símbolos exportados e `DT_NEEDED` nos 30 `.so` |
| Bibliotecas byte-idênticas por ABI | `libavformat`, `libavfilter`, `libavutil`, `libswresample`, `libswscale`, `libc++_shared` |
| Alterações nativas | `libavcodec` pela correção DV; `libmpv` pelo motor/dependências; `.text` dos shims JNI igual |
| 16 KiB | Todos os ELF ARM64/x86_64 verificados têm alinhamento PT_LOAD≥16 KiB e congruência; três shims/libc++ ARMv7 mantêm4 KiB, como antes |

Alinhamento ELF não prova empacotamento ZIP ou execução final do APK. Não anunciar compatibilidade universal sem ensaio do APK num ambiente16 KiB. Os nomes Xiaomi/Thomson/Mortal não identificam por si ABI, API ou capacidade DV. A biblioteca e a App mantêm mínimo Android 8/API 26; não há novo suporte a Android 7. Referência técnica: [Android 16 KiB](https://developer.android.com/guide/practices/page-sizes).

### 6.3 Diferença de hashes entre Maven e GitHub — resolvida na análise

| Artefacto | SHA-256 verificado |
| --- | --- |
| AAR 2026.10.4 da cache atual | `30da5e483342ee7c44ac57a694050279a1a69a5a0d55876ebf94cfdd90fbbfe4` |
| AAR 2026.11.0 GitHub Release | `c067fe578b74be6128d0ebeaa82e21595e820ba313c73bcf2de105faf89f9329` |
| **AAR 2026.11.0 Maven candidato** | **`89f8f6b7eb007ccfd07224c1a02482963fe41726f2eeb47c1c268aedde51cd7f`** |
| POM 2026.11.0 Maven | `a447141ba4e123f0d3c2d1486aa8f820d4be1c08dab060f3360793702b56910b` |
| `.module` 2026.11.0 Maven | `6494a8ae29915498d564e8a30763990841fd8019294de4d5f7fc3bf06a3c13ef` |

O hash Maven coincide com o próprio `.module`. As entradas ZIP GitHub/Maven só diferem em seis ficheiros: `libplayer.so` e `libowntvffmpeg.so` das três ABI. Em cada um mudam **exatamente20 bytes dentro de `.note.gnu.build-id`**; os restantes bytes são iguais. Não é prova de código comprometido. Contudo, são ficheiros diferentes para a verificação Gradle e para correspondência de símbolos.

**Regra de implementação:** a resolução Maven deve usar o hash do AAR Maven, não o do asset GitHub. Reconfirmar os três artefactos oficiais no dia da implementação; nunca desativar verificação para aceitar a discrepância. Os symbols da Release só servem para um binário cujo build-id corresponda. Não chamar ao AAR novo «em uso»: a App continua em 2026.10.4 nesta análise.

### 6.4 Alterações futuras mínimas e gates

1. `OwnTV_Core/gradle/libs.versions.toml`: alterar só libmpv 2026.10.4→2026.11.0.
2. `OwnTV-main/gradle/verification-metadata.xml` e `OwnTV_Core/gradle/verification-metadata.xml`: acrescentar apenas AAR/POM/module Maven reconfirmados. Preservar hashes antigos e verificação estrita.
3. Regenerar snapshot/lock pelo mecanismo existente; comparar todos os inputs e confirmar o SHA reconhecido pelo build. Manter **AGP 9.5.1**, Media3 e Kotlin atuais. Os scripts nativos upstream usam AGP 9.5.0, mas o consumidor recebe AAR pronto: **não copiar esse catálogo nem fazer downgrade**.
4. Não misturar o lote com buffers, retries, audiofocus, seleção SDR/HDR, nova sondagem HLS ou escalaGPU. Conservar rollback para 10.4.
5. Validar ligação JNI/renderer FFmpeg, compilação e testes pertinentes. Não inferir autorização de APK/instalação desta análise.

O AAR contém também a ponte FFmpeg de áudio do ExoPlayer (`AudioOutputPolicy.kt:134–140` e `ffmpeg/FfmpegLibrary.java` locais). Testar ExoPlayer após o update é necessário, mesmo para quem só pretende melhorar mpv. Não foi identificado um patch novo para os cortes periódicos AAC/MPEG-L2/PCM. Não altera ruído analógico de jack nem elimina restrições/cooldown403 do fornecedor.

**Smoke físico mínimo:** Android 8+ ARMv7 e ARM64 efetivamente usados; HLS H.264+AAC e MPEG-L2; áudio plataforma/FFmpeg quando aplicável; volume a 100%; mpv30 min com pausa curta/longa, standby, seek/diferido, mudança de faixa e EOF; A–B–A com o mesmo cooldown. DV5 apenas em hardware adequado, repetindo HDR10/SDR, hardware/software e sem decoderDV. Resultados TCL upstream não são prova para as boxes do utilizador.

O switch HDR local é `target-colorspace-hint` no mpv (`OwnTVPlayer.kt:2689`); PreferirSDR é aplicado ao HLS ExoPlayer (`:1268/:2083`). **Nenhum garante conversãoDV→SDR nem proíbe o decoderDV5 nativo.** Não bloquear o decoder correto com base nesse hint e produzir cores erradas quando não há baseSDR/HDR10.

## 7. Opções secundárias: o que faria e quando

### U09 — Mais templates de diferidos M3U

Novo `CatchupUrl.kt:40/:108` suporta `${(b)…}`/`${(e)…}`, formatos de início/fim e `:utc`. O local não tem esse padrão completo. É uma extensão útil **apenas se a lista o usar**, separada da construção de arquivos Xtream já existente.

Alteração candidata só em `core/.../epg/CatchupUrl.kt` e testes correspondentes, preservando todos os tokens locais, encoding, correção de arquivo aplicada uma vez e fuso efetivo. Testar DST, `:utc`, formato inválido e regressão do desvio visual/operacional. Não deslocar novamente o relógio EPG nem recomendar isto como cura genérica do404.

### U10 — M3U: retry seguro e numeração

O upstream passa `maxAttempts=2` no download da lista. Não copiar sem distinguir falha **antes da resposta/emissão de entradas**, corpo parcial e recusa HTTP. Um retry de catálogo não deve repetir callbacks já consumidos nem ultrapassar429/Retry-After, cancelamento ou erro de autenticação. A política do cliente atual tem de ser testada antes de ativar esse novo chamador. Isto não altera streaming HLS.

A numeração automática por ordem do ficheiro, quando **nenhum** `tvg-chno` existe, é opcional. Preservar números do fornecedor, IDs, hidden/manual order e grupos; não usar a lista visual filtrada como fonte de números. Ambos têm prioridade secundária para a utilização Xtream deste projeto.

### U11 — Stalker por portal, sem reintroduzir VOD

Há benefícios candidatos para portals usados: host canónico após redirect, login/senha opcionais, limite de paralelismo aprendido entre passes e hash que ignora apenas `play_token` volátil, mantendo a URL atual para tocar. O local já tem orçamento adaptativo por passe e invalidação de sessão por identidade; não começa do zero.

Se autorizado: adaptar `StalkerClient`, `StalkerAuthManager`, `StalkerSyncer` e update separado de URLs em `BulkInsertHelper` apenas para Live. Preservar MAC-only sem novo pedido login, headers/cookies sem fuga entre hosts, Retry-After e isolamento por source/configuração. Um token volátil ignorado no hash não pode esconder mudança real de cmd/DRM/headers/EPG. Nada disto melhora automaticamente uma lista Xtream.

### U12 — Ordem explícita de origens EPG

O local já prefere a source do canal em `EpgDedupe`/`GuideReader`/`LiveEpgReader`, com associações e `GuideInheritance`. Uma ordem de fontes escolhida pelo utilizador pode resolver conflitos reais, mas muda política e exige UI+backup/remapeamento de IDs.

O `GuidePriority`70 descarta um programa inferior inteiro perante qualquer sobreposição; isso pode deixar buracos. Não recortar timestamps ou inventar programas para encher a grelha. Se implementado, aplicar a mesma decisão limitada por janela a agora/seguinte/guia/gravações, conservar rows originais e a identidade do arquivo. Nunca usar esta opção para alterar o desvio visual ou a correção +1 h de diferidos.

### U13 — Perfis com PIN e herança de listas

O local já protege alterações/remoção/restauro de perfis com `ProfileOperationAuthorization`, grants curtos/de uso único e verificação local no backup. Não copiar `ProfileManager`/`Pin`/`BackupManager` inteiros.

Há, porém, uma lacuna estática específica: `ProfileManager.create:49–59` liga **todas** as fontes ao perfil novo. `ProfileGate.kt:89` oferece adicionar perfil; `MainActivity.kt:477` chama `startAddingProfile`, que só muda estado (`ProfileGateSessionViewModel.kt:82`); `ProfilesViewModel.create:63` chama o manager. O filtro de herança upstream evita expor listas de perfis protegidos a um perfil novo aberto.

**Candidato reduzido se o PIN for usado:** derivar fontes herdáveis a partir dos PIN de perfil **já existentes**, `ProfileDao.getAllOnce` e `SourceDao.allLinks/allSourceIds`, com consistência transacional apropriada. Nova instalação/perfis semPIN conservam o comportamento. Uma lista partilhada com perfil protegido não deve ser herdada silenciosamente só por também estar ligada a um perfil aberto; partilha de listas protegidas exige decisão/autorização própria. Não é necessário importar os novos 23 ficheiros de category lock/settingsPIN para corrigir esse ponto.

Testar novo perfil antes da autenticação, fontes sem dono/protegidas/partilhadas, corrida com edição e nome duplicado, e perfis novos vazios sem crash. Preservar grants/backup/autorização atuais. CategoriaPIN, PIN separado de definições e conflitos Merge/Replace/KeepBoth são opções adicionais, não pré-requisitos nem mudanças que se devam fazer silenciosamente num projeto de um só utilizador.

### U14 — Limpeza de índices: só após prova local

Upstream remove dois índices EPG e sete de catálogo. Podem ser redundantes no upstream, mas o fork tem queries e índices próprios. Executar `EXPLAIN QUERY PLAN` e medir import/leitura/scroll com dados sintéticos antes de uma migração local. Índice composto pode servir prefixo, mas tamanho, cobertura, collation e ordenação contam; 240canais não garantem benefício relevante.

Se aprovado: alinhar DAO, Entity, schema exportado, `healSchema`/criação de índices e `BulkInsertHelper` em nova migração **a partir do schema local51**. Não apagar tabelas VOD/legacy ou campos personalizados por a UI já não os usar. DROP não prova redução imediata do ficheiro; não executar VACUUM contra dados reais nesta análise.

## 8. O que não deve entrar automaticamente

### U15 — Três reconstruções de decoder

O upstream `LiveStartRungs.kt:91–102` usa500 / 1500 / 3000 ms e o teste pressupõe cerca de 13 s dentro de um default de 30 s. O local já tem uma reconstrução antes do primeiro frame (`LivePreviewEngine.kt:2848–2885`), mas default de abandono15s e tempo independente para tentar outra qualidade.

Não importar `LiveStartRungs`/watchdog em bloco. Aumentar tentativas pode consumir quase todo o orçamento, atrasar alternativas e abrir sessões sem corrigir o codec. Se houver falha codec reproduzível, testar uma vs duas reconstruções pelo coordenador existente, dentro do mesmo prazo e só após drain; 403/404, buffering, áudio-only e ausência de dados não são erro codec. O local usa `LiveWaitBudget`, owner/revisões e crédito de espera por identidade; não os substituir por renovação de janelas.

### U16 — GET mpv para detetar manifesto após EOF

**Rejeitar a cópia literal de `OwnTVPlayer.kt:1418–1498` upstream.** Há uma intenção válida de identificar `.ts` que afinal é HLS, mas o GET adicional começa depois de `withTimeoutOrNull { stopped.await() }` cujo resultado é ignorado. Executar `stop` não prova END_FILE/idle/drain nativo. A Call síncrona de sondagem também não pertence ao owner/registro de cancelamento local; um guard de geração depois da resposta não fecha essa chamada ao mudar de canal.

Outros riscos: só 0x47 no primeiro byte é menos forte que os três pacotes TS locais; classificar OTHER como erro pode recusar formatos válidos; `.m3u8` sibling não é universal para URLs assinadas/portals/M3U. O patch nativo HLS já existente em10.4/11.0 deixa FFmpeg abrir o manifesto pelo URL e opções corretas, embora não prove todos os casos resolvidos.

Só investigar se uma fixture mpv-only reproduzir a falha residual. Preferir evidência do pedido nativo existente. Uma eventual sondagem teria de usar o mesmo claim/owner/orçamento, release confirmado, cancelamento de Call/body e resultado inconclusivo não fatal. Nada é aprovado por defeito neste update, especialmente no uso Exo-only/HLS-only.

### U17 — Retomar sempre no live-edge após 60 s

`LiveResume.kt:15–24` novo força reload em live pausado por mais de 60 s, excluindo arquivo/local. Pode ajudar uma janela expirada, mas também perde uma posição ainda válida e abre outra ligação ao mesmo canal.

O local já para/ressintoniza no retorno de background (`LivePreviewEngine.kt:2149–2198`) e mantém identidade/retoma de arquivo e cópia local. A implementaçãoC03 não deve ser substituída. Se houver lacuna, avaliar validade real da posição/janela e intenção do utilizador; comparar seek ao default atual com reload. Usar `setPlaybackRequested` para todas as entradas Play/MediaSession, preservando revisão, tickets, cooldown e fecho. Não instalar um temporizador ou limiar de 60 s universal sem ensaio.

### U18 — Audiofocus não é uma cura demonstrada para os cortes só de som

Local `PlaybackSession.kt:239–251/:346` pausa em CAN_DUCK e pede pausa ao sistema. Upstream separa políticaTV/telefone e duck. Isto pode explicar pausas causadas por outra app, mas não prova a origem de cortes só de áudio com imagem fluida.

Usar primeiro o log existente `audio_focus`. Testar notificação vs perda real, gain de owner antigo, pausa manual e leitor externo. Uma futura mudançaTV deve manter a autoridade única local; não acrescentar simultaneamente outro gestorMedia3. **Volume da App permanece100%**, sem duck manual emdB ou lógica de chamadas telefónicas. A atenuação que o Android pode aplicar é um mecanismo diferente do ganho da App. [Referência Android](https://developer.android.com/media/optimize/audio-focus#automatic-ducking).

### U19 — FairPlay e ClearKey inline não são um pequeno enum

O local suporta Widevine/ClearKey por URL; falta também a infraestrutura inline anterior à1.0.70. A novidade70 consegue reconhecer FairPlay real e tratar certos conteúdos incorretamente identificados que trazem material inline válido comoClearKey. **Não torna FairPlay real reproduzível no Android.**

Se uma lista autorizada exigir isto, criar lote próprio: parser/serialização, provider local inline, factoriesMedia3/DRM→Exo, headers/licença/rotação e redação de chaves. Sem ignorar licenças, sem chaves em logs/backups e sem alterar HLS/AES128 comum. Não recomendar para MPEG-L2, HTTP403 ou cortesPCM.

### U20 — Limpeza de nomes pode estragar a escolha automática de qualidade

`ChannelNameCleanup.applyTo`70 grava o nome limpo em `ChannelEntity.name` e conserva raw só em `providerName`. Remove sufixosHD/HEVC e outros tokens. A App local usa **`ch.name`** em `features/live/ChannelVersionPolicy.kt:10/:19/:50` para grupo, rank e ordem. Preservar raw só para matchingEPG não protege esses consumidores.

Uma cópia direta pode eliminar a distinção FullHD/HEVC/HD/normal/low, mudar groupKey e desligar prioridades manuais. Se houver interesse futuro, começar pela **apresentação apenas**, sem alterar nome persistido/identidade. Migração raw/display completa exige inventário de TODOS os consumidores, backup/remapeamento e testes; defaultoff. Não é uma limpeza de código neutra.

### U21 — Resets de ordenações e número26 do backup

O reset não está na migração51→52. Está em `PlaybackStartup.kt:44–49` → `ContentOrderDao.dropListOrdersExcept` + marca `SettingsRepository.listOrdersReset`; o backup usa `isOldListOrder/withoutOldListOrders` para descartar ordens não-Favorites de ficheiros anteriores a26.

**Não importar esses caminhos.** A App tem `content_order.replaceEditedContext` e `SectionCustomizations.channelVersionOrders`. O comportamento novo de só fixar itens movidos e deixar os restantes em A–Z muda a semântica das escolhas locais.

O backup local26 já significa identidade portátil de fontes; upstream26 inclui outros campos/limpezas. Igualdade do número não é compatibilidade. Manter leitores de backups locais antigos, credenciais, atualização da lista, offsets, override por canal, ordem de alternativas, TV Avó e PINs/autorização atuais. Se novos campos forem futuramente aprovados, acrescentá-los explicitamente ao contrato local, com defaults e remapeamento, sem importar a regra de descartar ordens antigas. Testar merge/restauro idempotente e proteção de perfis.

### U22 — Histórico EPG: não criar um produtor concorrente

O local já obtém histórico Xtream sob demanda com cache/coalescência, semáforo, cancelamento por geração e limite 2048 (`XtreamClient.getHistoricalEpg`, `LiveEpgReader.providerProgrammes`, `GuideReader`). A nova release não concede mais dias que o arquivo do fornecedor.

O novo `ArchiveGuideFiller` criaria outra autoridade/pedidos. Além disso, `ArchiveGuidePlan.changed:78` compara apenas inícios, ignorando título/fim alterados; `EpgRepository.fillArchiveGuide:918` apaga rows antes de as reescrever, sem substituição atómica única. **Não copiar literalmente.**

Se houver necessidade de persistir mais histórico, estender o produtor local único, com chave source/configuração/intervalo, TTL/retenção e substituição atómica. `has_archive` ausente é desconhecido; não usar o campo como bloqueio rígido para todo o guia/futuro. Não alterar os horários de programas para forçar uma ligação e não prometer eliminar404/cooldown.

### U23 — Proteções que já existem e devem ser conservadas

1. **XMLTV truncado:** local `XmltvParser.kt:34/:95/:99` exige documento completo no caminho apropriado; `EpgRepository.kt:394/:432/:448` só promove cache/prune após sucesso. O parser70 é menos abrangente nessa proteção. Não o substituir como suposta correção nova.
2. **Credenciais especiais Xtream:** local `parser/XtreamUrls.kt` usa `HttpUrl.addPathSegment/addQueryParameter` e já resolve `%`, Unicode, espaços e caracteres reservados. Não trocar por encoder manual upstream ou dupla descodificação.
3. **Edição de fonte e resync:** `SettingsViewModel.kt:1152/:1166` já compara `catalogConfigurationKey`, cancela o sync antigo e agenda atualização após alteração relevante. Não introduzir outro editor concorrente/agenda duplicada.
4. **Host misto HLS:** caches por URL/host, TTL e provaTS locais já evitam que404 sozinho apague a lição. U05 corrige o papel do pedido sem substituir esses mecanismos.
5. **Arquivo, áudio e lifecycle locais:** manter buffer/reserva independentes, crédito de espera, ownership, cópia local retida, offsets por saída, Stereo/mixer/FFmpeg e regras de volume. Testes/upstream novos podem fornecer fixtures, não autorização para apagar as implementações locais.

### U24 — UI, comando, arranque e detalhes de programas

As novidadesCore de short/longOK, inversãoCH, teclasnuméricas, etiquetas, voz, memória de categorias e escolhas de preview precisam de integração de UI; a presença de enums/preferences no Core não prova que uma opção funcionará na App.

Preservar os contratos existentes: canal de arranque fixo escolhido pelo utilizador, cursor no canal atual ao voltar, ações de versões/menu por pressão longa, agrupamento/ordens por qualidade, versões ocultas fora da prioridade, TV Avó sem controles adicionais e pesquisa limpa ao reabrir. Não acrescentar outro botãoSources quando as alternativas já têm UI própria, nem religar pesquisa/títulos removidos.

A separaçãoAFR Live/VOD acrescenta pouco numa App sóTV. PINs de definições/categorias, voz, gestão por browser e prioridade de fontes são opções independentes, não correções obrigatórias. O editor por browser abre uma nova rota de escrita e exige revisão de modo/permissão/autenticação/segredos; o resync interno já funciona. Não o introduzir como dependência dos fixes de catálogo.

A nova página de detalhes XMLTV e descrições completas implica modelos/colunas/UI adicionais; não está incluída no writerEPG de oito colunas. Os callbacks locais de EPG já transportam `EpgProgrammeEntity` e `CatchupOwner` (`EpgScreen.kt:176`): não assumir que falta todo o contrato de identidade só porque upstream adicionou um novo modelo de linha. «Ver do início»/ordem do menuGoBack devem ser avaliados pelos controles existentes e capacidade real de arquivo, respeitando TV Avó; não desbloqueiam arquivo inexistente.

### U25 — Funcionalidades fora da utilização pretendida

Não importar Sports/calendários/rankings/novas ligas, filmes/séries/posters/Continue, lógica de smartphone/telefonia, PiP excluído pelo utilizador ou auto-sync/escuta permanente de LAN. São funcionalidades, não correções de HLS. Não reintroduzir custo de sincronização, listeners, permissões e menus que o utilizador retirou.

**Preservar** diferidos, gravações, downloads de programas antigos e quotas de armazenamento interno/externo. O novo switch de downloads por perfil ligado a VOD não deve ficar como preferência inerte nem bloquear esses fluxos sem decisão própria e integração end-to-end.

A opção `ignoreConnectionLimit` só ignora a contagem local; não aumenta o que o fornecedor autoriza. Não a usar para «corrigir» A–B–A ou sessões presas. Medição explícita/coerente e fecho real são distintos de desativar os limites.

### U26 — Upscaling por hardware permanece uma hipótese beta

Foi consultado `docs/MELHORIAS_FUTURAS_PENDENTES.md`, conforme o pedido original. O percurso principal usa `vo=mediacodec_embed` (`OwnTVPlayer.kt:487–490/:2616–2619`), não a cadeiaGPU de pós-processamento libplacebo. A atualização da biblioteca e mais segundos de buffer **não ativam um algoritmo de upscaling nem criam detalhe**.

Manter a ideia pendente, opcional e desligada por defeito. Qualquer ensaio deve comparar com o scaling já feito pela box/TV, medir GPU/RAM/temperatura/frames/A–V/latência e permitir retorno imediato ao percurso normal. Não misturar essa beta com a atualização de compatibilidade nativa.

## 9. Ordem de execução recomendada, sem confusão com o plano anterior

| Lote | Unidades | Entrada | Critério de saída |
| --- | --- | --- | --- |
| **UP70-A** | A1 / U01, A2 / U02, A3 / U03, A4 / U04, A5 / U05 | Aprovação de implementação; cópia e baseline verificadas | Cada correção isolada tem teste útil; conjunto preserva fontes/ordens/HLS/owners; snapshot correto |
| **UP70-B1** | U06 | A estável; fixtures SQLite/Room de paridade | Mesmo resultado/Flow, rollback e horas; medição de escrita sem regressão de navegação |
| **UP70-B2** | U07 | B1 aferido; decisão baseada em ganho real | Teto durante cursor, nenhum prune parcial, redução de queries/tempo demonstrada; caso contrário manter lazy |
| **UP70-C** | U08 | Correções pontuais estabilizadas; hashes Maven reconfirmados | Verificação estrita/JNI/ABI e regressãoA/V; aceite físico antes de anunciar benefício nas boxes |
| **UP70-D opcionais** | U09–U14/U17/U19/U20 (só apresentação)/U24, conforme necessidade | Pedido específico, benefício e contrato definidos | Testes e persistência end-to-end; sem efeitos por omissão |
| **Investigação apenas** | U15/U16/U18/U26 | Fixture/log ou hardware que demonstre a lacuna | Parecer novo antes de alterar comportamento; não integrar protótipo automaticamente |

A lista acima não autoriza um merge de169 commits, um bump de schema 51→53 ou reinstalação do Core. O plano HLS anterior continua com as suas fases4A/4B condicionadas; não as marcar como executadas por esta análise. A1–A5 podem ser implementadas por subfases, evitando juntar a complexidade da ocupaçãoSQL com transporte/codec num único patch.

## 10. Critérios comuns de aceitação e rollback

### 10.1 Automatizados antes de integrar

- Cópia verificável dos ficheiros afetados, hash/source lock e estadoGit sujo preservado. Para qualquer alteração persistente, usar primeiro DB/perfis/listas sintéticos e uma cópia de segurança validada; nunca migrar dados reais para ensaiar uma proposta.
- Testes dirigidos aos contratos dos cartões, usando fixtures upstream úteis **adaptadas** ao fork. Ficheiros citados upstream não são testes locais já existentes; um teste novo deve ser identificado como novo no lote.
- Compilação App/Core pertinentes e teste da App com **o snapshot final correto**. Usar `-PowntvUseCoreSnapshot=true` e verificar o SHA no log e nos inputs extraídos; não aceitar um build antigo emcache como prova do novoCore.
- Manter verificação de dependências estrita. Não aceitar outro hash em silêncio, introduzir wildcard, desligar assinaturas ou criar baseline/suppress para esconder falhas novas.
- Executar lint de Release aplicável. O histórico tem cinco erros anteriores de pluralização no lint integral do Core; os alvos executados e falhas anteriores devem continuar separados. Os 470 testes/buildAGP anteriores não validam estas propostas ainda não implementadas.
- Regressões de backup: credenciais/URL/atualização da lista, source identity, offsets, tempo de alternativa/abandono/reabertura, versões/ordens manuais, ocultação/sidebar/TVAvó e opções por canal; importação antiga/merge/remapeamento sem apagar escolhas.

### 10.2 Físicos quando houver alteração de reprodução

- Mesma box, fonte/canal/variante, motor e saída de áudio, antes/depois. Registar API/ABI reais; nomes comerciais não bastam.
- Sessão mínima30 min em AAC e MPEG-L2, imagem/som separados, zapping A–B–A/A–B–C–A, fullscreen→lista→mesmo canal, pausa/standby, diferido/seek e leitor externo nos fluxos afetados.
- Sem nova ligação antes do fecho/claim devido; sem retry que ultrapasse prazo do utilizador ou ignore Retry-After. Um HTTP403/cooldown do fornecedor não é SSL, codec ou prova de motor incompatível.
- No nativo, testar Exo áudioFFmpeg e mpv, não só um deles. Para DV/16 KiB/AFR, ensaio específico no ambiente correto; não deduzir resultado do TCL upstream.
- Não prometer percentual de velocidade, menor RAM ou eliminação de cortes sem comparação equivalente. Registos redigidos, sem contas/chaves/URL sensível.

### 10.3 Rollback e limpeza

LotesA/B não devem exigir nova schema apenas para o SQL proposto. Qualquer migração futura de U14/novos campos precisa de passo local próprio e plano de recuperação; não usar destructive fallback. No lote nativo preservar versão10.4/hashes, snapshot anterior e assinatura/empacotamento atuais. Não apagar snapshots antigos ou o backup do utilizador para «limpar» a atualização.

**O que retirar do plano, não do código funcional sem prova:** cópias globais de controllers, resets/limpeza de ordens, novo produtor histórico duplicado, GETextra de mpv, três retries por defeito, encoderXtream manual duplicado, reinstalação de VOD/mobile/Sports e propostas de ganho14×/4× como garantia local. Remover índices/código restante só depois de identificar consumidores, formato persistido e efeito medido; não confundir nome de ficheiro novo upstream com necessidade de o importar.

## 11. Evidência e cobertura auditável

Pasta local de auditoria: `C:/Users/renat/Downloads/OwnTV-upstream-audit-2026-11-08-164931`.

| Artefacto | Conteúdo |
| --- | --- |
| `upstream-snapshots.json` | SHAs dos quatro snapshots, URLs, hashes ZIP e contagem de ficheiros |
| `core-changed-files.json` | Inventário integral dos447 ficheiros e comparação textual local/antigo/novo |
| `local-core-lock.json` e `local-core-input-hashes-before.json` | Baseline real dos1098 inputs, independente do commitGit antigo |
| `playback_review.md` | Oito decisões especializadas, paths/linhas locais/upstream, recovery/HLS/DRM/lifecycle e parecer crítico |
| `data_review.md` | Sync/EPG/backup/ordens/migrations, patchessetoriais e parecer crítico |
| `data_order_sql_model.json` | Fixture/modeloSQL em memória; não execução Kotlin/Android |
| `native_review.md` | Nove decisões nativas, testes, compatibilidade/hardware e limites |
| `native-aar-inventory.json`, `native-elf-jni-comparison.json` | Manifest/ABI/classes/símbolos/ELF/DT_NEEDED comparados |
| `maven-vs-release-aar.json`, `maven-vs-release-elf-buildids.json` | Prova da diferença restrita a build-id dos seis shims |
| `mpv-master-286-compare.json`, `native-carried-patches.json` | Delta do motor subjacente e patches preservados |
| `App-git-status-before.txt`, `Core-git-status-before.txt` | Estado de trabalho existente preservado |

A comparação de todos os ficheiros foi inventariada. A revisão linha-a-linha concentrou-se nos caminhos funcionais relevantes; traduções e funcionalidades excluídas foram classificadas por âmbito, **não apresentadas como447 auditorias integrais independentes**. O upstream da UI TV completo não foi importado/reconstruído nesta análise: os efeitos de UI são cruzados com a App local e os contratos do Core, sem afirmar que uma preferência nova funciona só por existir no Core.

Fontes complementares: [HLS/RFC8216](https://www.rfc-editor.org/rfc/rfc8216.html), [Android audiofocus](https://developer.android.com/media/optimize/audio-focus), [Android 16 KiB](https://developer.android.com/guide/practices/page-sizes), [Core 70 por SHA](https://github.com/ahXN00/OwnTV_Core/tree/9de56d7106491e8dea8e37636ff5df9a69cce156) e [libmpv 11 por SHA](https://github.com/ahXN00/OwnTV_libmpv/tree/5c09e5910bc9844a0727d46fca070756f02aa14a).

## 12. Estado final para retoma

- **Analisado:** Core 1.0.70 + libmpv 2026.11.0, comparações completas, código personalizado, pendênciasOwnCore e ideia de upscaling.
- **Recomendado:** roteiro seletivo UP70-A/B/C; opções e investigações separadas, com operações/testes acima.
- **Após autorização:** UP70-A implementado/validado; UP70-B1 implementado e validado por compilação Release, 687 testes JVM dirigidos/App e lint vital. UP70-B2 medido e não integrado, conforme gate de memória/arranque. Ver os registos de implementação A/B para os resultados e limites. UP70-C autorizado, integrado com libmpv 2026.11.0 e validado por Release/lint/merge nativo/R8/DEX; aceitação física pendente. Propostas opcionais continuam sem implementação; mantêm-se Media3 1.11.1, AGP 9.5.1, schema 51 e backup 26 próprios.
- **Não demonstrado:** cura de403/A–B–A, cortes periódicos de áudio, ganho14×/4× ou compatibilidade física universal.
- **Após os lotes autorizados:** aceitação física de UP70-A/B1/C pendente. Não voltar a propor A/B1/C como código por integrar, nem ativar B2 globalmente apesar do resultado do ensaio. Propostas opcionais, migrações upstream e geração/instalação de APK exigem âmbito próprio.

## 13. Índice de caminhos locais para implementação futura

Neste documento, `core/...` abrevia `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/` e `player-core/...` abrevia `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/`, salvo quando o caminho completo é fornecido. O caminho completo prevalece; os números de linha referem o snapshot desta análise e devem ser reconfirmados antes de editar.

| Unidade | Ficheiros locais existentes prioritários | Acrescentar só se necessário |
| --- | --- | --- |
| U01 | `core/.../network/HttpClient.kt` | Casos nos testes de redação existentes ou ficheiro de teste novo identificado no lote |
| U02 | `core/.../sync/XtreamSyncer.kt`, `core/.../parser/XtreamClient.kt`, `core/.../util/ErrorMessages.kt` | Helper de autenticação apenas se permitir testar o guard sem duplicar parser |
| U03 | `core/.../database/dao/SourceDao.kt`, `core/.../sync/XtreamSyncer.kt`; verificar `core/.../live/ConnectionLimits.kt` e `core/.../repository/SourceRepository.kt` | Testes de autoridade/concorrência de limite medido |
| U04 | `core/.../sync/SyncSupport.kt`, `M3uSyncer.kt`, `XtreamSyncer.kt`, `StalkerSyncer.kt`, `core/.../database/dao/ChannelDao.kt` | `core/.../database/dao/PositionParking.kt` seria NOVO; adaptar testes multi-lote/Room |
| U06 | `core/.../repository/EpgRepository.kt`; novos testes `EpgGeneratedInsertTest.kt` e `EpgBatchTransactionTest.kt` | Chamada DAO direta, sem transação exterior; nenhum SQL duplicado, campo ou migração |
| U07 | `core/.../repository/EpgRepository.kt`, `core/.../repository/ProgrammeHashTracker.kt`, `core/.../database/dao/EpgDao.kt` | Consulta ordenada/bounded somente após provar ganho e orçamento |
| U05 | `player-core/.../LivePreviewEngine.kt` | Testes de origem/canonicalização de aprendizagem, preservando guards existentes |
| U08 | `C:/Users/renat/Downloads/OwnTV_Core/gradle/libs.versions.toml`; XML de verificação Core/App; `OwnTV-main/core-source-lock.json` via ferramenta | Novo ZIP imutável pelo snapshot tool, após validação |

`M3uSyncer.kt`, `XtreamSyncer.kt` e `StalkerSyncer.kt` na linha U04 ficam todos no diretório `core/.../sync/`, não na raiz. Não criar por engano ficheiros com nomes iguais noutro package. Testes upstream mencionados são referências para adaptar; não se declara que existam já no fork.

## 14. Fecho do council e verificação de âmbito

- **Reprodução:** aprova U05 com URL canónica capturada por owner/source e resposta 2xx; exige requests/orçamentos idênticos. Mantém rejeição do GET extra e das três reconstruções globais, e preservação das proteções locais superiores.
- **Dados:** aprova U02/U03 e U04 como conjunto completo, incluindo estacionamento transacional. Aprova writer adaptado de oito colunas; preload só com teto firme durante o cursor e ganho demonstrado. Rejeita resets e filler concorrente. A revisão final destas secções pelo especialista de dados foi conforme.
- **Nativo:** concorda com Core pontual primeiro e libmpv depois, isolado/reversível. Revisão final do documento conforme para hashes/ABI/compatibilidade; exige testes físicos e distingue artefacto candidato de dependência em uso.
- **Coordenação:** aceita apenas as operações delimitadas. Opções de PIN/EPG/nomes/DRM permanecem independentes; novas features de UI e limpeza de schema não são disfarçadas de fixes de reprodução.

No fecho, os 1098 inputs do Core e o lock foram novamente comparados com a baseline, sem alteração. O estadoGit do Core manteve-se; na App as únicas entradas novas/alteradas por esta auditoria são este relatório e o registo documental de pendências. Não houve atualização do binário, migração, alteração de configuração, build, teste Kotlin/Android ou APK. O modelo SQL sintético e a inspeção estática/nativa permanecem claramente separados da aceitação de produção.
