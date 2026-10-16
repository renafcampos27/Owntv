# Atualização libmpv 2026.10.2 — 9 de outubro de 2026

Atualização autorizada e aplicada: libmpv 2026.10.1 → 2026.10.2 no catálogo do Core TV externo. Manifesto tools/mpv/native_manifest.json atualizado com a proveniência e inspeção real do artefacto Maven.

## Alteração

Inclui os dois patches upstream para evitar espera bloqueada do MediaCodec e reconhecer ausência simultânea de progresso de entrada/saída. A correção de CPU/AudioTrack da 2026.10.1 permanece. Não foram alterados buffers, tempos de handover/reabertura, preferências de hardware, configurações por canal ou volume a 100%. ExoPlayer, a extensão FFmpeg de áudio e o Core privado Mobile foram preservados.

## Verificação

- Artefacto Maven SHA256: 166696341e6d7ef4c75c34f5acc41d284c1c3da80123a0a92d3350b5e2ba67f4; igual ao ficheiro resolvido pelo Gradle.
- ZIP íntegro e checksums publicados SHA256/SHA1 previamente conferidos.
- 34 binários inspecionados; apenas libmpv.so e libavcodec.so mudam nos três ABIs.
- Wrapper JVM e manifesto iguais; mínimo Android API 26.
- Dependências nativas, símbolos JNI e conjuntos exportados de funções FFmpeg conservados; sem redução do alinhamento ELF, incluindo pelo menos 16 KiB nos binários de 64 bits.
- Bibliotecas ARM integradas pela tarefa de merge conferidas: libmpv/libavcodec do candidato e libffmpegJNI personalizada preservada.
- Compilação Kotlin da variante standardDebug e merge de bibliotecas nativas concluídos.
- Player-core: 443 testes, 0 falhas, 0 erros, 0 ignorados.
- Registo: C:\Users\renat\AppData\Local\Temp\owntv_mpv_102_build.log.

Sem APK, commit ou publicação. Os testes JVM não executam o decoder nativo: a recuperação com imagem/som no modo mediacodec_embed, a temperatura e a estabilidade nas boxes continuam dependentes de teste físico. Não foi demonstrada resolução do problema de reabrir canais em 10–15 segundos.

## Próximo teste no dispositivo

Gerar novo APK da variante habitual no Android Studio. Comparar A–B–A e A–B–C–A com regressos antes de 15 segundos e após 20–30 segundos; testar direto/diferidos em mpv e reprodução normal prolongada. Confirmar ExoPlayer sem regressão. Se houver fallback, confirmar imagem, som e capacidade de cancelar/mudar canal. Consultar ANALISE_LIBMPV_2026.10.2_2026-10-09.md para os critérios completos.

## Reversão limitada

Originais anteriores a esta atualização: C:\Users\renat\AppData\Local\Temp\owntv-libmpv-102-analysis\before-20261009-225541. Reverter apenas o pin para 2026.10.1 e restaurar a proveniência correspondente, preservando outras alterações posteriores no catálogo/manifesto.

Fonte: https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.10.2
