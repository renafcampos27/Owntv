# Fase 2B — respostas DNS tardias com contexto

Data: 08/11/2026. Projeto: OwnTV Android TV personalizado.
Estado: fase 2B implementada e validada localmente no Core/reprodutor e na App; aceitação física pendente.

## Decisão e benefício demonstrado

Executada apenas a fase 2B/P04 do plano HLS/áudio. Antes de alterar produção, os 1092 inputs do checkout foram comparados com o snapshot final da fase 2A (`fcd36cd7247c44c8ccddc00a14dcb12b92c534980fe583c89ce86ecaa360ddf4`); correspondiam integralmente. Foram guardados hashes e cópias dos ficheiros afetados.

O teste anterior à alteração confirmou que a primeira espera expirava, a resolução nativa terminava depois com sucesso, mas essa resposta não ficava na cache. Um segundo lookup com falha transitória não tinha alternativa e também falhava. O teste final repete esse cenário e recupera a resposta tardia válida. Em ambos os cenários houve **duas consultas nativas**.

A política de refresh por lookup foi preservada. O benefício provado é o aproveitamento do sucesso tardio como fallback válido, e não uma redução geral do número de consultas ou um ganho percentual de velocidade. Não se aguardam estatísticas ou confirmação de rede para iniciar uma resolução saudável.

## Alterações exatas

| Ficheiro no Core | Alteração |
| --- | --- |
| `core/src/main/java/tv/own/owntv/core/network/BoundedDns.kt` | Capturar o contexto uma vez por consulta; usar chaves hostname/geração DNS/geração de rede; publicar uma resposta não vazia ao concluir o trabalho nativo, antes de acordar os chamadores, incluindo quando estes já expiraram. Guardar cópias independentes e impedir publicação de tarefa/contexto substituídos. Validade calculada desde a conclusão, sem renovação pela leitura. |
| `core/src/main/java/tv/own/owntv/core/network/DnsConfig.kt` | Acrescentar `DnsConfigHolder.ResolverSnapshot(generation, resolver)` e `capture()`. A geração e o resolver que fecha sobre a configuração capturada são publicados juntos num estado imutável. `configure()` só muda geração quando a configuração difere. O DNS global continua dinâmico; os backends UDP/DoH, fallback e parsers não foram reescritos. |
| `core/src/main/java/tv/own/owntv/core/network/PlaybackDnsContext.kt` (novo) | Declarar `DnsLookupContext` e juntar a captura DNS à geração de identidade de rede observada. Mudanças de rede e perda conhecida criam outro contexto; voltar à mesma rede depois de outra não reutiliza a geração antiga. |
| `core/src/main/java/tv/own/owntv/core/network/StreamingHttpClient.kt` | Expor um `liveDns` limitado e partilhado pelos leitores Live. Iniciar a subscrição do observador existente apenas no primeiro uso desse DNS. O cliente base e os leitores que não usavam o DNS limitado conservam o seu DNS anterior. |
| `core/src/main/java/tv/own/owntv/core/di/DataModule.kt` | Passar `dnsHolder::capture` e `ConnectivityObserver.state` ao singleton de reprodução. |
| `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt` | Substituir apenas `.dns(BoundedDns(streamingHttp.client.dns))` por `.dns(streamingHttp.liveDns)`, retirando a instância duplicada por motor. |

A publicação ocorre dentro do trabalho do `FutureTask`, antes de completar a promessa, com verificação da identidade da tarefa no mapa. `done()` só remove a entrada correspondente. Isto evita depender de um `get()` bem-sucedido e evita a janela entre `isDone` e publicação. Não se cancela nem se promete cancelar o resolver nativo.

## Compatibilidade e política de rede

- Permanecem **5 s de espera sem resposta anterior**, **1 s quando há alternativa anterior**, **5 minutos de validade local**, **64 respostas**, **quatro workers** e **fila de 64 trabalhos**. Os executores pequenos injetados nos testes não alteram os limites de produção. A validade local não é apresentada como TTL autoritativo do servidor.
- Alterar metering, transportes descritivos, validação/internet ou repetir a mesma identidade não invalida a cache. Não se exige `VALIDATED` para resolver ou reproduzir.
- **Uma observação indisponível não é uma desconexão:** conserva a última identidade observada e a alternativa válida. Não foi integrado um filtro que desative a cache por desconhecer o estado da rede. Uma perda conhecida muda de geração; uma nova resolução saudável continua permitida nesse contexto e pode fornecer a alternativa válida subsequente.
- Captura de configuração e geração é coerente, mas o contexto de rede representa a identidade observada. Não há `bindSocket`, associação a uma `Network` ou garantia de observação instantânea de toda mudança física. Um pedido já iniciado pode terminar com o seu resolver capturado; o resultado não entra na cache de outro contexto.
- Reutiliza-se o observador partilhado e o seu backstop existente. Não foi criado outro callback Android ou timer. A subscrição do Live DNS mantém-se durante a vida do singleton após o primeiro uso; isto pode manter o observador existente ativo, sem multiplicar callbacks. A avaliação de custo na box continua pendente.
- A fila de reprodução da fase 2A, limites HTTP, cancelamento por proprietário, fecho/drain, retry, TLS, UA, buffers, áudio, volume, formatos, tempos de reabertura, alternativas e preferências não foram alterados.

## Testes novos e resultados

**17 testes JVM novos**, além dos existentes:

- Dez em `core/src/test/java/tv/own/owntv/core/network/BoundedDnsTest.kt`: benefício após sucesso tardio, validade desde a conclusão/sem renovação, mudança DNS e de rede com resultado antigo a terminar depois, resposta vazia/falha, cópias independentes, executor saturado/recuperação, interrupção do chamador, limite de 64 entradas e continuidade da alternativa em observação desconhecida.
- Três em `DnsConfigHolderTest.kt`: gerações só em alteração real, resolver capturado que mantém a configuração e captura concorrente coerente.
- Três em `PlaybackDnsContextTest.kt` (novo): metadados/replay, troca e regresso de rede, desconhecido vs perda efetiva.
- Um em `StreamingHttpClientTest.kt`: wiring real do DNS Live, partilha entre clientes derivados e preservação do DNS base.

A versão final terminou o comando do Core com **BUILD SUCCESSFUL**, **95 testes JVM de rede e 624 do reprodutor**, zero falhas/erros/ignorados. Inclui os testes da fase 2A no mesmo conjunto. A App compilou em Standard Release, os **470 testes JVM da App** passaram e o lint vital Release terminou com **`No issues found.`**. Total: **1189 testes JVM**, zero falhas/erros/ignorados. A compilação completa com as fontes finais de produção terminou `BUILD SUCCESSFUL in 7m 44s`; após ampliar o teste conjunto e regenerar o snapshot (apenas esse teste mudou), a confirmação com o lock definitivo terminou `BUILD SUCCESSFUL in 1m 34s`, reutilizando os resultados válidos de compilação/testes. Estes tempos são da ferramenta de build, não medições de velocidade do streaming.

O teste anterior da fase 2A foi ampliado para executar HTTP HLS com DNS Live: semeadura de uma resposta válida, falha da resolução nova e recuperação da alternativa enquanto os cinco pedidos de imagens continuam bloqueados nos cabeçalhos. Passou no mesmo conjunto de 95 testes e confirma coexistência 2A/2B. São 17 testes novos mais esse cenário existente ampliado.

O ensaio temporário da versão anterior foi arquivado fora do checkout. A primeira compilação da App foi interrompida para retirar a política experimental mais restritiva de cache quando a observação era desconhecida; esse candidato não é a entrega final e não conta como validação. Os testes foram repetidos após o ajuste de compatibilidade.

## Snapshot, evidência e limites

Snapshot final: **1094 inputs**, SHA-256 `683d66fa7a260834d89898bca939627743a6925b8a5b4df90ac1fa78a41fe84d`.

Cópias, hashes, testes antes/depois e logs: `C:/Users/renat/Downloads/OwnTV-fase2b-2026-11-08-122356`. Referências principais: `baseline-dns-result.xml`, `after-dns-result.xml`, `dns-benefit-proof.json`, `core-validation-compatible.log` e os manifestos de resultados. A validação da App usou `-PowntvUseCoreSnapshot=true` e identificou explicitamente o hash final. Os 1094 inputs do checkout e da cópia compilada foram comparados byte a byte com o ZIP. Confirmou-se no artefacto Release a API `getLiveDns()` e as gerações de contexto, sem a política experimental de bloqueio por observação desconhecida. Resultados finais em `app-validation-locked-final.log`, `app-unit-results.json`, `queue-dns-coexistence-result.xml`, `snapshot-integrity.json` e `validation-final.json`.

O lint integral do Core não foi declarado aprovado nem repetido nesta fase: os cinco erros de pluralização anteriormente documentados na fase 2A permanecem fora do âmbito, em recursos inalterados. O lint vital Release da App foi aprovado. Permanecem os cinco erros anteriores de pluralização, em recursos cujos hashes são iguais aos do início desta fase.

Não houve APK, instalação, commit/push, acesso a credenciais do fornecedor ou reprodução física. O resultado não demonstra que o bloqueio A–B–A do fornecedor ou os cortes de áudio ficaram resolvidos. As fases seguintes permanecem pendentes.

Fontes de desenho: [contrato DNS do OkHttp 5.5.0](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Dns.kt) e [Android: estado e eventos de rede](https://developer.android.com/develop/connectivity/network-ops/reading-network-state). A implementação usa o código atual do checkout e os resultados locais; estas fontes não são prova de melhoria física na box.
