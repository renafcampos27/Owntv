# Fase 2A — fila própria de reprodução

Data: 07/11/2026. Projeto: OwnTV Android TV personalizado. Estado: fase 2A implementada, testes funcionais aprovados e integração Release da App validada. O lint integral do Core tem cinco erros anteriores, detalhados abaixo; aceitação física pendente.

## Decisão e prova anterior à alteração

A fase 2A/P03 foi autorizada com o critério «testar primeiro». Antes de alterar produção, os 1091 inputs do Core foram comparados com o snapshot validado da fase 1 (`5ca2e6084c14fb8d343153b2dbb3d8f080309ad002e16058f8c766c3a56cd448`); eram iguais. Foram guardadas cópias e hashes dos ficheiros afetados.

O cliente real `StreamingHttpClient(base).client` ainda partilhava o dispatcher do cliente base. A App deriva o cliente de imagens desse mesmo cliente base. O ensaio JVM usou OkHttp 5.5.0 real e um servidor HTTP local, com cinco pedidos assíncronos de imagens à espera de cabeçalhos no mesmo host. Os limites eram os padrões: 64 chamadas assíncronas no total, cinco por host.

Com a fila partilhada, a playlist HLS ficou na fila e não chegou ao servidor enquanto as imagens estavam retidas. Depois de libertar as imagens, o tempo registado de fila foi **474,806 ms**. O teste passou antes da alteração de produção. A retenção de 350 ms foi deliberada para demonstrar a dependência; este valor não é uma medição da box ou do fornecedor.

Isto confirma uma espera residual possível após a fase 1: acontece antes de o pedido obter cabeçalhos, portanto retirar leituras de diagnóstico não a elimina. Não confirma a causa dos cortes de áudio ou dos bloqueios impostos pelo fornecedor.

## Alteração mínima integrada

1. `OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/StreamingHttpClient.kt`: acrescentar `.dispatcher(Dispatcher())` à construção do cliente, com o import correspondente. O cliente permanece uma propriedade única da instância singleton fornecida pela injeção de dependências. Os clientes dos motores derivados por `newBuilder()` reutilizam a mesma fila de reprodução.
2. `OwnTV_Core/core/src/main/java/tv/own/owntv/core/di/DataModule.kt`: atualizar apenas o comentário sobre o singleton, indicando o isolamento da fila além do pool de sockets. A configuração de injeção não mudou.
3. `OwnTV_Core/core/src/test/java/tv/own/owntv/core/network/StreamingHttpClientTest.kt` (novo): três testes para isolamento sob cinco imagens pendentes, limites e cancelamento de chamada em fila, reutilização entre clientes derivados e preservação da configuração do transporte.
4. `OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/PlaybackHttpCallsTest.kt`: acrescentar um cenário assíncrono com o cliente real, dois proprietários de reprodução e tráfego base. Retirar um proprietário deve libertá-lo sem cancelar o preview ou as imagens, e permitir nova chamada. Inclui os guards e o diagnóstico de sucesso da fase 1.

Não há nova biblioteca, opção, timer, atraso, semáforo ou política de retry. Os métodos de eviction e os coordenadores de sintonia/retirement não foram alterados. Nenhum código de produção chama `dispatcher.cancelAll()` ou encerra o executor por canal. Encerramentos de executores na fixture são apenas limpeza do teste.

## Limites e coexistência

- A reprodução conserva os limites padrão **64/5**. Não se aumentaram esses valores para obter um resultado favorável.
- As duas filas podem permitir mais chamadas assíncronas agregadas do que uma fila partilhada. A separação não é uma garantia global de número de ligações HTTP nem altera o orçamento de sessões de reprodução. Pedidos HTTP de imagens/segmentos não correspondem automaticamente a sessões IPTV contratadas.
- O dispatcher limita a execução assíncrona até o callback terminar; não representa toda a leitura do corpo pelo loader. As chamadas síncronas `execute()`, usadas em percursos de catálogo/EPG, não ficam sujeitas a estes limites. Não se anuncia aceleração geral do EPG ou da transferência de segmentos.
- A App continua a obter o cliente base para Coil e o singleton de reprodução para os motores. Hero preview e leitores Live derivados mantêm ownership individual; nenhum dispatcher por canal foi introduzido.
- DNS/2B, TLS, UA, buffers, descodificação, volume, tempos de reabertura, alternativas, gravações e preferências permanecem fora desta alteração.

O ensaio pós-alteração exige que a playlist seja recebida enquanto as cinco imagens continuam pendentes, não apenas depois de a sua demora acabar. Outro teste mantém cinco chamadas de reprodução pendentes, cancela a sexta em fila, confirma que essa chamada não chega ao servidor e que o cliente base continua a funcionar. Os testes comparam também proxy/DNS/TLS/headers/protocolos/timeouts e a identidade da fila nos clientes derivados.

## Correção necessária da integração do build

Durante a validação, o Gradle reutilizou a configuração do Core anterior: o lock apontava para o snapshot novo, mas a pasta desse snapshot nem sequer tinha sido extraída e todas as tarefas antigas apareciam `UP-TO-DATE`. Essa execução inicial (`app-validation.log`) **não conta como validação da fase 2A na App**.

- `OwnTV-main/gradle/custom-core.settings.gradle.kts`: substituir a leitura `JsonSlurper.parse(File)` por `parseText(providers.fileContents(...).asText.get())`. Assim o conteúdo de `core-source-lock.json` é uma entrada explícita da cache de configuração. As verificações de hash existentes permanecem, e a cache não foi desativada.
- `OwnTV-main/tools/core_snapshot/test_settings_cache.py` (novo, opt-in): teste funcional com o script real e dois snapshots mínimos. Após aquecer a cache em A, publicar B tem de invalidá-la, executar o código B e voltar a reutilizar a cache em B. Os seis processos Gradle desse ensaio passaram. A primeira extração pode exigir uma execução adicional de estabilização devido aos ficheiros recém-criados.
- O teste usa `-PowntvUseCoreSnapshot=true` para isolar o ensaio de overrides de Core na máquina. Os seis testes existentes do publicador também passaram.

A correção segue a [API oficial de fileContents](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.provider/-provider-factory/file-contents.html), que regista o conteúdo lido durante a configuração como entrada do modelo. Foi necessária para validar e entregar efetivamente o Core novo; não altera o comportamento do streaming no APK.

Para repetir o teste funcional, definir `OWNTV_RUN_GRADLE_CACHE_TEST=1` e executar `python -B -m unittest discover -s tools/core_snapshot -p test_settings_cache.py -v`. Sem a variável, este teste pesado fica explicitamente ignorado; os testes normais de publicação continuam independentes.

## Resultados finais e snapshot

Testes concluídos: **78 testes JVM de rede do Core e 624 testes JVM do reprodutor**, zero falhas/erros/ignorados; **seis testes Python do publicador e um teste funcional Python/Gradle** aprovados. A App com o snapshot correto compilou em Standard Release, passou os **470 testes JVM da App** sem falhas/erros/ignorados e terminou `:app:lintVitalStandardRelease` com **`No issues found.`**. O comando final terminou `BUILD SUCCESSFUL in 23m 56s`. No total: **1172 testes JVM e sete testes Python/Gradle aprovados**. Os quatro testes JVM novos são três de fila/configuração e um de retirement entre proprietários.

O lint completo `:core:lintDebug` terminou com **cinco erros de PluralsCandidate e 67 avisos**. Os cinco erros estão em `core/src/main/res/values/channel_playback.xml`, `recording_capture.xml`, `strings_settings.xml` (duas ocorrências) e `visual_performance.xml`. Os hashes desses quatro ficheiros são idênticos aos anteriores à fase 2A. Ficaram registados em `core-full-lint-preexisting-errors.json`, sem os ocultar, criar baseline ou alterar textos fora do âmbito. Por isso o comando combinado do Core terminou com falha de lint, embora os dois conjuntos de testes tenham passado. Não se declara o lint integral do Core aprovado.

Snapshot publicado: **1092 inputs**, SHA-256 `fcd36cd7247c44c8ccddc00a14dcb12b92c534980fe583c89ce86ecaa360ddf4`. Todos os 1092 inputs foram comparados byte a byte com o checkout. No Core, foram alterados três ficheiros existentes (incluindo comentário e teste) e acrescentado um ficheiro de teste; não houve remoções. O bytecode Release do snapshot correto contém a chamada `OkHttpClient.Builder.dispatcher(new Dispatcher())`. As preferências, DNS, drain, guards da fase 1 e regras de verificação de dependências mantêm os hashes anteriores.

Registos finais: `app-validation-current-core.log`, `core-unit-results.json`, `player-core-unit-results.json`, `app-unit-results.json`, `settings-cache-test-3.log`, `snapshot-tests.log`, `queue-isolation-proof.json`, `compiled-release-streaming-client.txt` e `snapshot-integrity.json`, na pasta de evidência. A execução inicial com configuração antiga está explicitamente rejeitada como prova.

Para reversão da fila, restaurar o ficheiro anterior e publicar novamente um snapshot validado; as cópias anteriores e o arquivo da fase 1 foram preservados. A correção da leitura do lock é independente da política de rede e deve manter-se para os builds reconhecerem alterações futuras.

## Evidência, fontes e aceitação física

Cópias, fixture anterior à alteração, XML de resultados, hashes e logs: `C:/Users/renat/Downloads/OwnTV-fase2a-2026-11-07-121345`.

A política de fila foi cruzada com o [código oficial de Dispatcher no OkHttp 5.5.0](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Dispatcher.kt) e com os sources usados pelo projeto. As conclusões de implementação são verificadas pelos testes locais; não são percentagens de desempenho numa box.

Não se gerou APK nem se instalou ou testou a App numa box. Falta comparar arranque/zapping com imagens por carregar, CPU/RAM e continuidade da reprodução no equipamento real. Não houve acesso a credenciais nem ligações a canais do fornecedor. A fase 2B continua pendente; esta entrega não encerra toda a fase 2.
