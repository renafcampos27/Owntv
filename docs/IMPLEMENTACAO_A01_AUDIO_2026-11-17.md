# A01 — avisos de áudio sem recuperação destrutiva

Data: 17/11/2026. Âmbito autorizado: **apenas A01**.

## Alteração

Em `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt`, o callback `AudioWatchdog.onAudioSinkError` mantém o diagnóstico, mas deixa de chamar `raise()` por esse aviso isolado. O Media3 pode recuperar a saída internamente; a App não deve antecipar pausa terminal, latch estéreo ou reconstrução do leitor com base apenas nessa notificação.

Os comentários foram ajustados para distinguir diagnóstico, verificação do primeiro avanço e resgate limitado por underruns passthrough. Não se apresenta o watchdog como detetor contínuo de audibilidade.

O handler `onPlayerError`, os seus filtros e reconexões não foram alterados. Também não foram alterados A02, HLS, timers, buffers, descodificação, volume a 100%, preferências ou dependências.

## Ficheiros de Core alterados

1. `player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt`: callback e comentários.
2. `player-core/src/test/java/tv/own/owntv/player/AudioRecoveryTest.kt`: substituído um teste com pressuposto incorreto por três testes de regressão.
3. `tools/i18n/safe_literals.txt`: removida exclusivamente a classificação técnica da mensagem eliminada, usando o comando de manutenção existente. O gate de tradução mantém-se ativo.

## Testes

Antes da alteração funcional, os três testes novos falharam no comportamento antigo. Cobrem avisos de inicialização/escrita, descontinuidade e exceção genérica, incluindo as variantes `isRecoverable=true/false`, em PCM e passthrough; verificam ainda que o aviso não consome o resgate por underruns nem desativa a verificação inicial sem avanço.

Depois da alteração passaram **88 testes dirigidos**, zero falhas, erros ou ignorados:

- `AudioRecoveryTest`: 16.
- `LivePlaybackConfirmationTest`: 4.
- `LiveTuneControllerTest`: 68.

Os testes de controlador usam motores simulados; não equivalem a ensaio físico de AudioTrack/HDMI. A aprovação dos testes não prova que esta seja a causa dos cortes periódicos na Xiaomi.

A primeira validação após a alteração detetou a entrada obsoleta no inventário de literais. Foi removida apenas essa entrada e a validação passou. Não se desativou nenhuma verificação.

Compilação `:app:compileStandardReleaseKotlin` e lint `:app:lintVitalStandardRelease`, usando `-PowntvUseCoreSnapshot=true`: **BUILD SUCCESSFUL**, 4 min 56 s. A compilação da App pôde reutilizar o resultado incremental; o player-core Release foi recompilado. Nenhum APK foi gerado.

## Integração na App

Snapshot anterior: `dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`.

Snapshot A01: `29b18f24125821406b365b1dff81c77a718872769acd078e868bc6256f9a72bf`.

Foram verificados os 1117 inputs: apenas os três ficheiros acima mudaram, sem adições/remoções. O ZIP novo coincide byte a byte com os inputs do checkout Core e o SHA-256 coincide com `core-source-lock.json`. O arquivo anterior foi preservado.

A validação da App força a utilização do snapshot, para não depender de um override local. Não foi gerado APK.

## Cópia de segurança e provas

`C:/Users/renat/Downloads/OwnTV-A01-20261117-175150` contém os originais dos ficheiros funcionais/testes, inventário anterior, lock anterior, hashes de todos os inputs, XML/log da falha anterior e XML/log dos testes aprovados. O novo lock verificado também foi guardado.

## Aceitação física pendente

Gerar/instalar um APK atualizado e testar reprodução prolongada nos canais AAC/MP2, com as opções habituais da box. Confirmar ausência de pausa/reabertura causada apenas pelo aviso e manutenção da recuperação perante uma falha real. A02 e restantes itens continuam pendentes; não houve instalação, commit ou publicação.
