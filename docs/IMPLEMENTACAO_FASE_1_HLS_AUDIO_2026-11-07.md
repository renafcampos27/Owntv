# Implementação da fase 1 — HLS e observação de áudio

Data: 07/11/2026. App Android TV: `OwnTV-main`; Core personalizado: `OwnTV_Core`.

Estado: fase 1A/1C/1D implementada e validada automaticamente no Core e na App. A subfase 1B e as fases seguintes continuam pendentes; a aceitação física na box não foi realizada.

## Âmbito entregue

Esta execução segue as restrições de compatibilidade e fluidez da secção 16.7 do [plano principal](PLANO_IMPLEMENTACAO_COUNCIL_HLS_AUDIO_CACHE_2026-11-03.md). O pedido abrange apenas a primeira fase. A subfase 1B é um protótipo condicionado e fica pendente; não faz parte da entrega funcional.

### 1A — Diagnóstico HTTP sem leitura adicional em respostas bem-sucedidas

- `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt`: o diagnóstico deixa de pedir o prefixo do corpo em respostas HTTP bem-sucedidas. O registo indica `not_sampled`, em vez de apresentar ausência de amostra como corpo vazio. Mantém-se a identificação funcional estreita de TS usada pelo percurso existente de compatibilidade.
- `player-core/src/main/java/tv/own/owntv/player/HttpResponseDiagnostics.kt` (novo): concentra esta política sem substituir o corpo, alterar o seu proprietário ou assumir o seu fecho. O comportamento anterior de leitura limitada dos erros HTTP genéricos é preservado.
- `LivePreviewEngine.kt`: acrescenta medições dos callbacks nativos de fila e decisão de retry do OkHttp e de conclusão de carregamento do Media3. Os novos eventos frequentes verificam o diagnóstico detalhado antes de construir as mensagens. Não há sondagens HTTP, nova fila, timer, retry ou pedido de rede.

### 1C — Recusas conhecidas sem esperar pelo corpo

- `player-core/src/main/java/tv/own/owntv/player/ProviderRetryGuard.kt`: recusas 408/429/503 usam estado/cabeçalhos e, quando disponível, apenas o texto já armazenado no buffer. Não se solicita mais corpo para obter uma explicação.
- Mantêm-se o prazo de cabeçalhos, `Retry-After`, a exceção existente de preparação local, a ordem de fecho e o controlo de tentativas. Um corpo vazio ou atrasado pode deixar a explicação remota vazia; o código e os cabeçalhos continuam disponíveis.
- Não se introduz um adaptador de corpo nem se declara um recurso fechado antes do fecho real. A subfase 1B precisará de validação própria com DataSource real, cancelamento e fecho concorrente antes de qualquer integração.

### 1D — Observar áudio sem nova recuperação

- `player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt`: guarda a configuração reportada pelo AudioTrack, distingue as instâncias de saída, conta underruns e identifica os ocorridos após o áudio começar a avançar. Os registos de underrun ficam limitados a um por segundo; os contadores continuam a contabilizar as observações aceites. A decisão de recuperação já existente não é alterada.
- `player-core/src/main/java/tv/own/owntv/player/StreamInfo.kt`: acrescenta um valor opcional imutável para a configuração de AudioTrack e uma indicação curta da frequência reportada, separada da frequência do áudio de origem. Não se apresenta esta informação como medição do sinal físico recebido na TV, HDMI ou soundbar.
- `LivePreviewEngine.kt` e `player-core/src/main/java/tv/own/owntv/player/ExoSubtitleEngine.kt`: reutilizam essa configuração no diagnóstico existente, sem abrir novos observadores.
- `player-core/src/main/java/tv/own/owntv/player/AudioRouteSync.kt`: a rota devolvida por `getAudioDevicesForAttributes` passa a ser representada como estimada, também no Android 13+. Não se altera a preferência manual ou o atraso guardado.
- `core/src/main/res/values/live_buffer_diagnostics.xml` e `values-pt/live_buffer_diagnostics.xml`: acrescentam a etiqueta curta `AudioTrack: … kHz`.

## Contratos preservados

Não foram alterados os tempos de reabertura, o drain, os buffers, o pré-buffer, a retoma, o volume de 100%, os codecs/descodificadores, a política de surround/PCM, TLS, a seleção HLS/TS, as preferências por canal, o timeshift, as gravações ou a base de dados. Não há atualização de bibliotecas nesta fase.

Os contadores e a configuração do AudioTrack ajudam a separar hipóteses; não detetam todo silêncio audível nem demonstram a causa dos cortes periódicos da Xiaomi. Não há reset novo de áudio ou ligação por causa destes dados.

## Testes acrescentados

- `player-core/src/test/java/tv/own/owntv/player/HttpResponseDiagnosticsTest.kt` (novo): cinco casos que verificam ausência de leitura em sucesso e no guard, preservação/não consumo de bytes já disponíveis, limite do prefixo e mensagem de erro genérico preservada.
- `player-core/src/test/java/tv/own/owntv/player/ProviderRetryGuardTest.kt`: um caso com servidor local, exercitando 408/429/503, corpo gzip grande retido, prazo de cabeçalhos e `Retry-After` preservados.
- `player-core/src/test/java/tv/own/owntv/player/AudioRecoveryTest.kt`: três casos para saídas antiga/nova, underruns PCM após avanço sem recuperação nova e rejeição de eventos da fonte antiga; amplia também um teste existente de configurações iguais.

São nove testes novos. Estes testes exercitam as funções reais alteradas. A fixture HTTP é apenas de teste; não integra a utilização normal da App.

## Pré-requisitos de compilação

Os catálogos já indicavam AGP 9.5.0, mas faltavam regras de verificação para artefactos dessa configuração. Foram acrescentados apenas os hashes em falta depois de os comparar com os repositórios oficiais. A verificação estrita mantém-se ativa; nenhuma versão foi atualizada ou revertida.

Ficheiros: `gradle/verification-metadata.xml` do Core e da App. O manifesto técnico `tools/i18n/safe_literals.txt` do Core foi atualizado com as novas mensagens de diagnóstico e entradas antigas removidas; a baseline de texto não traduzido permanece intacta.

## Validação final e snapshot

| Verificação | Resultado | Limite |
| --- | --- | --- |
| `:player-core:testDebugUnitTest` | 623 testes, 89 suites; zero falhas/erros/ignorados | JVM; inclui os nove testes novos. |
| `:player-core:compileDebugAndroidTestKotlin` | Passou | Compilação apenas, sem dispositivo. |
| `:player-core:lintDebug` | Zero erros, 10 avisos em linhas não alteradas | Não é medição de desempenho físico. |
| `tools/core_snapshot` | Seis testes passaram | Valida a ferramenta de publicação do snapshot. |
| `:app:compileStandardReleaseKotlin` | Passou com o novo snapshot | Compilação, sem gerar APK. |
| `:app:testStandardDebugUnitTest` | 470 testes, 86 suites; zero falhas/erros/ignorados | JVM da App com o Core atualizado. |
| `:app:lintVitalStandardRelease` | `No issues found.` | Verificação vital Release, não lint integral da App. |

**Total: 1093 testes JVM e seis testes Python aprovados.** A execução final do Core terminou `BUILD SUCCESSFUL in 14m 15s`; a integração da App terminou `BUILD SUCCESSFUL in 6m 53s`. A tarefa de testes unitários Release não existe nesta App; a tentativa inicial de seleção foi corrigida para a tarefa Debug acima. Os registos intermédios de pré-requisitos/seleção não são os resultados finais.

A App foi validada com `-PowntvUseCoreSnapshot=true`. O ficheiro `core-source-lock.json` aponta para o novo arquivo imutável de **1091 ficheiros**, SHA-256:

`5ca2e6084c14fb8d343153b2dbb3d8f080309ad002e16058f8c766c3a56cd448`

Todos os inputs do checkout foram comparados byte a byte com o arquivo. O snapshot anterior foi preservado. Face aos 1089 inputs anteriores: 12 ficheiros existentes alterados, dois novos e nenhum removido. Os ficheiros de configurações, drain, renderers, latência, versões e a baseline i18n mantêm os hashes anteriores.

Foram corroborados 86 artefactos da ferramenta de build nos repositórios oficiais. Mantiveram-se todas as regras de verificação anteriores: 1033 no Core e 1625 na App. Acrescentaram-se 86 regras no Core e 85 na App, porque esta já conhecia uma delas. A lista e as fontes estão em `verified-build-artifacts.jsonl`, com prova de preservação em `dependency-verification-proof.json`.

Resultados e integridade: `core-validation-4.log`, `app-validation-2.log`, `player-unit-results.json`, `app-unit-results.json`, `core-validation-summary.json`, `snapshot-tests.log`, `snapshot-integrity.json` e `final-input-changes.json`, na pasta de evidência indicada abaixo.

## Evidência e limites

Cópias dos ficheiros anteriores, hashes de entrada, resultados e registos de validação estão em `C:/Users/renat/Downloads/OwnTV-fase1-2026-11-07-mra3ib5z`.

Não foi gerado APK nem feita instalação, assinatura, commit ou publicação. Não houve acesso às credenciais do fornecedor nem ensaio de reprodução numa box. A validação física de arranque/zapping e dos cortes de áudio continua necessária; não se atribuem percentagens de velocidade ou robustez.
