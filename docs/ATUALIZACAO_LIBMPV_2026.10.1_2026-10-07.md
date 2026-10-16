# Atualização libmpv 2026.10.1 — 7 de outubro de 2026

## Alteração autorizada e aplicada

Dependência tv.own.owntv:libmpv atualizada de 2026.10.0 para 2026.10.1 no catálogo do Core local. Manifesto de integridade tools/mpv/native_manifest.json atualizado com o novo pacote e inspeção dos 34 binários nativos.

Correção upstream: ao_audiotrack aguarda até 10 ms, libertando o bloqueio interno quando não existem amostras de áudio. Evita o ciclo ativo que podia consumir CPU e bloquear a reprodução mpv após um underrun. Não altera buffers, volume, preferências por canal ou regras de zapping.

Fonte: https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.10.1
Patch: https://github.com/ahXN00/OwnTV_libmpv/blob/v2026.10.1/buildscripts/patches/mpv/0002-audiotrack-no-spin-on-underrun.patch

## Verificação do pacote

- SHA-256: 38cd193b229e90eb1c7149fa6fbdb476d1b63d4f6e4af9a316af14ef6891f893.
- Checksums SHA-256 e SHA-1 publicados pelo Maven verificados.
- AAR resolvido pelo Gradle tem o mesmo SHA-256.
- ZIP sem erros de integridade; minSdk continua em 26 (Android 8).
- Nove classes JVM iguais à versão anterior.
- Apenas libmpv.so mudou nas três arquiteturas; restantes binários, incluindo FFmpeg e libffmpegJNI personalizados, mantêm os hashes anteriores.
- Arquiteturas, dependências nativas, símbolos JNI, API mínima declarada nas notas ELF e contagem de símbolos FFmpeg conservados.
- Sem redução dos alinhamentos ELF; binários de 64 bits mantêm alinhamento de pelo menos 16 KiB.
- Binários libmpv nos resultados de merge nativo conferidos com o manifesto 2026.10.1; libffmpegJNI conservados.

## Validação

Compilação appStandardDebug e integração de bibliotecas nativas aprovadas. Player-core: 443 testes, 0 falhas, 0 erros e 0 ignorados. Registo: %TEMP%/owntv_mpv_101_build.log.

Sem APK, commit ou publicação. Não foi feita reprodução em box/smartphone: os testes JVM não executam o novo código nativo de áudio. Confirmar direto e diferidos em mpv, aberturas repetidas, áudio, zapping e reprodução ExoPlayer no dispositivo. Não há prova de resolução dos problemas de rede/403 ou zapping ExoPlayer com este patch.

## Reversão limitada

Originais do catálogo e manifesto conservados em C:\Users\renat\AppData\Local\Temp\owntv-mpv-2026.10.1-review\before-20261007-201403. Para reverter apenas esta atualização, regressar a 2026.10.0 e restaurar o manifesto dessa versão, preservando outras alterações posteriores. As bibliotecas JNI personalizadas não foram substituídas.
