# UP70-C — libmpv 2026.11.0, integração isolada

Data: 08/11/2026. Projeto OwnTV Android TV/Core personalizado. Autorização: implementar UP70-C.
Estado: implementação e validação automatizada concluídas; aceitação física na box pendente.

## Implementado

- `OwnTV_Core/gradle/libs.versions.toml`: apenas `libmpv = "2026.10.4"` → `"2026.11.0"`.
- `OwnTV_Core/gradle/verification-metadata.xml` e `OwnTV-main/gradle/verification-metadata.xml`: acrescentados exatamente os hashes do AAR, POM e module da nova versão. Entradas anteriores mantidas, sem wildcard novo, desativação de verificação ou aceitação de hash divergente.
- `OwnTV_Core/tools/mpv/native_manifest.json`: inventário atualizado para o AAR Maven efetivamente usado, com hashes dos 30 ELF, arquitetura, dependências, símbolos JNI, build-id, `.text`, alinhamento e contrato da ponte FFmpeg. É um ficheiro técnico de auditoria, não lógica de reprodução.
- `OwnTV-main/core-source-lock.json` e ZIP imutável: integração do Core atualizado pelo mecanismo existente.

Não houve mudança de código Kotlin/Java/C++, UI, buffers, latência, retries, audiofocus, HLS-only/exceções, cooldown/reabertura, descodificação por canal ou preferências persistidas. Volume da App a 100%. AGP 9.5.1, Media3 1.11.1, Kotlin 2.4.20, schema 51 e backup 26 mantidos. Não se copiou o catálogo AGP 9.5.0 usado para produzir o AAR upstream.

## Artefactos oficiais reconfirmados nesta implementação

[Release oficial 2026.11.0](https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.11.0), commit publicado `5c09e5910bc9844a0727d46fca070756f02aa14a`. Maven público do mesmo projeto. Os três ficheiros foram novamente descarregados por HTTPS e comparados com os hashes auditados. O module também confirma o hash do AAR.

| Artefacto Maven | SHA-256 |
|---|---|
| `libmpv-2026.11.0.aar` | `89f8f6b7eb007ccfd07224c1a02482963fe41726f2eeb47c1c268aedde51cd7f` |
| `libmpv-2026.11.0.pom` | `a447141ba4e123f0d3c2d1486aa8f820d4be1c08dab060f3360793702b56910b` |
| `libmpv-2026.11.0.module` | `6494a8ae29915498d564e8a30763990841fd8019294de4d5f7fc3bf06a3c13ef` |

AAR: 32179902 bytes; anterior 31944195 bytes. A diferença de 235707 bytes é do AAR, não um aumento medido do APK. A discrepância conhecida para o asset AAR GitHub continua documentada na análise anterior: não se usou o hash GitHub para validar Maven. Os símbolos de crash só podem ser usados quando o build-id corresponde ao binário Maven aqui inventariado.

Prova: `official-artifacts-verified.json` na pasta de cópias. Esta reconfirmação não atualiza para uma versão diferente da que foi autorizada.

## Verificação nativa e da ponte ExoPlayer

O AAR não é apenas usado pelo mpv: também fornece `libowntvffmpeg.so`, carregado pelo FFmpeg de áudio do ExoPlayer. Por isso foram verificados ambos os contratos.

- Mesmo conjunto de **30 bibliotecas**, nas ABI `armeabi-v7a`, `arm64-v8a` e `x86_64`; Android mínimo 26 preservado no manifest do AAR.
- **Nove classes Java byte-idênticas** entre os AAR anterior/novo; comparação das entradas `.class`, não apenas do ZIP `classes.jar`.
- Arquiteturas, símbolos exportados e `DT_NEEDED` equivalentes nas 30 bibliotecas. A secção `.text` dos dois shims JNI é idêntica à anterior em cada ABI.
- Hashes e build-id novos guardados. PT_LOAD com alinhamento e congruência de 16 KiB nas ABI de 64 bits, e pelo menos 4 KiB em ARMv7, como antes. Isso não comprova alinhamento ZIP de um APK que não foi gerado.
- Presença do caminho `wait_keyframe` verificada em libavcodec nas três ABI. Os patches HLS/underrun já existentes não são apresentados como novidades deste update.
- As classes/métodos e descritores de `FfmpegLibrary`, `FfmpegAudioDecoder` e callback `growOutputBuffer` foram comparados nos três binários e contra `javap -private -s` das classes Java realmente compiladas. Verificados `JNI_OnLoad`, tipos de buffer e nove métodos nativos.

As bibliotecas foram lidas, não executadas nativamente no Windows. O ensaio usa pyelftools 0.32 extraído exclusivamente para a pasta de auditoria, com wheel verificado contra o hash oficial PyPI. Não foi acrescentada uma dependência de desenvolvimento/execução ao projeto. Script e prova: `verify_native_update.py`, `native-integration-verification.json`, `audit-python-tool.json` e dumps javap.

O manifesto/script legado `tools/ffmpeg/audio_manifest.json` / `verify_audio.py` refere a integração FFmpeg anterior, não a ponte partilhada atual. Não foi utilizado `--restore` nem reintroduzido `libffmpegJNI.so`; a verificação deste lote usa a ponte atual em `tv.own.owntv.player.ffmpeg`.

## Benefício e limites

A versão inclui a seleção do decoder MediaCodec Dolby Vision para HEVC perfil 5 quando existe decoder compatível, além das alterações internas mpv/libplacebo/texto analisadas previamente. FFmpeg continua 9.0.2. [Changelog e fontes fixadas](https://github.com/ahXN00/OwnTV_libmpv/blob/5c09e5910bc9844a0727d46fca070756f02aa14a/CHANGELOG.md).

Não há prova de que este update resolva cortes periódicos AAC/MPEG-L2/PCM, cooldown/403 do fornecedor ou ruído analógico. Não ativa upscaling, não cria capacidade Dolby Vision em hardware incompatível e não garante conversão DV→SDR. Não se alterou a política HDR/SDR local para bloquear a seleção do decoder correto.

## Validação automatizada

- **633 testes JVM do player aprovados**, sem falhas/erros/ignorados, incluindo políticas HLS, handover, áudio e mapping FFmpeg. São testes de lógica; não descodificam os streams na box.
- Inspeção estática dos ELF/Java/JNI aprovada nas três ABI, conforme acima.

- App: compilação Release aprovada; os resultados dos **470 testes JVM da App** permanecem sem falhas/erros/ignorados. A tarefa ficou `UP-TO-DATE` por os inputs Java/Kotlin não mudarem; não se apresenta como nova execução física/nativa.
- `lintVitalStandardRelease`: **No issues found.** Verificação estrita das dependências conservada.
- `mergeStandardReleaseNativeLibs`: **30 bibliotecas** do AAR novo comparadas byte a byte com os ficheiros realmente unidos pelo build, nas três ABI. Ausência de `libffmpegJNI.so` legado confirmada. O merge não demonstra os filtros/alinhamento ZIP de um APK ainda não gerado.
- `minifyStandardReleaseWithR8`: executado e aprovado. Mapping e **DEX efetivamente gerados** confirmam **12 nomes de classes e 26 métodos nativos/callbacks**, incluindo os tipos/descritores de buffer do FFmpeg. O dexdump do SDK Android confirmou flags NATIVE e assinaturas; não se inferiu presença de um método nativo só pelo mapping, que pode omitir identidades sem código Java. Não houve alteração de regras ProGuard.
- Build conjunto: **BUILD SUCCESSFUL in 6m 38s**, 171 tarefas: 60 executadas, 57 recuperadas da cache e 54 atualizadas. Com `--dependency-verification=strict -PowntvUseCoreSnapshot=true`, sem assemble/package/sign APK.
- Provas: `r8-jni-contract-verification.json`, `merged-native-libraries.json`, `player-tests-summary.json`, `app-tests-summary.json`, `lint-results-standardRelease-final.txt` e logs. Total de resultados JVM aprovados: 633 player executados neste lote e 470 App mantidos pela cache de inputs; não é teste de reprodução nativa.
- A suite integral e o lint integral do Core não foram reexecutados; as falhas anteriores de testes de gravação/pluralização não foram suprimidas nem declaradas resolvidas neste lote.

## Snapshot e preservação

Novo snapshot: **1109 inputs**, SHA-256 `ecc95fb3e16dad1b8d291d744cee3e4ee96d0229a5d2cfa78992c7c1f3722561`. Os únicos inputs existentes do Core alterados são catálogo de versões, XML de verificação e manifesto técnico nativo. Zero ficheiros Kotlin/Java/C++ alterados, zero inputs adicionados/removidos. Este número não é uma declaração de equivalência integral ao Core upstream 1.0.70.

Todos os 1109 inputs foram comparados byte a byte entre checkout, ZIP bloqueado e extração efetivamente usada no build. Os XML de verificação foram comparados estruturalmente: configuração de confiança e todos os hashes anteriores idênticos, apenas os três artefactos novos acrescentados em cada build. Provas em `integration-input-proof.json` e `final-validation.json`.

Pasta de cópias, artefactos e provas: `C:/Users/renat/Downloads/OwnTV-up70c-2026-11-08-225223/`. Inclui snapshot anterior integral `5a537cee…`, hashes dos inputs, estados Git anteriores, metadados anteriores e cópia dos artefactos libmpv 2026.10.4 disponíveis na cache. Sem acesso a listas/credenciais ou dados persistentes do utilizador.

## Regressão física por executar

Na box, testar mpv e ExoPlayer com HLS H.264+AAC e MPEG-L2, som plataforma/FFmpeg quando aplicável, volume a 100%, mudança de faixa e zapping A–B–A com o mesmo cooldown. mpv por pelo menos 30 minutos, incluindo pausa, suspensão/retoma, diferidos/seek e EOF. ARMv7 e ARM64 reais se ambos forem usados. Dolby Vision perfil 5 apenas em hardware/stream apropriado, com regressão SDR/HDR10 e fallback sem decoder DV.

Estes ensaios não foram executados nesta fase automatizada. Um build, ELF alinhado ou testes JVM não são prova de reprodução física, qualidade superior ou ganho percentual.

## Como voltar a 2026.10.4

O snapshot anterior e binários anteriores foram preservados. Para reversão estrita deste lote, depois de confirmar que os três ficheiros não têm alterações posteriores, recuperar catálogo, metadados do Core e manifesto nativo do snapshot anterior e os metadados da App da cópia; regenerar snapshot/lock e validar com verificação estrita. Não fazer reset do checkout, limpar dados da App ou restaurar bases de dados. Os hashes antigos foram mantidos; a resolução 10.4 permanece verificável.

Não houve geração/assinatura de APK, instalação, commit ou push. UP70-A e B1 permanecem integrados; B2 continua não integrado conforme o seu gate. Propostas opcionais, migrações upstream e beta de upscaling não foram incluídas.
