# OwnTV TV — análise libmpv 2026.10.2

Data: 9 de outubro de 2026. Estado: análise concluída; atualização não aplicada.

Atualização posterior: o utilizador autorizou e a dependência foi atualizada em 9 de outubro de 2026. Compilação e 443 testes aprovados; teste físico continua pendente. Consultar `ATUALIZACAO_LIBMPV_2026.10.2_2026-10-09.md`. O texto seguinte preserva a análise e decisão anteriores à implementação.

## Decisão do council

Recomendar um ensaio isolado da atualização 2026.10.1 → 2026.10.2. A adoção definitiva depende de confirmar recuperação com imagem e som no modo de vídeo da OwnTV personalizada e ausência de regressões nos dispositivos representativos. Não há fundamento para prometer melhoria geral de HLS, áudio ou ExoPlayer.

Participaram agentes especializados em alterações upstream/MediaCodec, integração local/JNI e council de decisão. A comparação de fontes e binários foi verificada pelo coordenador. A análise não alterou dependências, código de reprodução, Mobile ou configurações do utilizador e não gerou APK.

## Versão e artefacto atuais

- Core externo utilizado: `C:\Users\renat\Downloads\OwnTV_Core`.
- `gradle/libs.versions.toml:47`: libmpv 2026.10.1.
- `tools/mpv/native_manifest.json`: artefacto 2026.10.1 e SHA256 `38cd193b229e90eb1c7149fa6fbdb476d1b63d4f6e4af9a316af14ef6891f893`, coincidente com o AAR local analisado.
- AAR candidato analisado: Maven público, `https://ahxn00.github.io/OwnTV_Core/maven/tv/own/owntv/libmpv/2026.10.2/libmpv-2026.10.2.aar`.
- SHA256 candidato: `166696341e6d7ef4c75c34f5acc41d284c1c3da80123a0a92d3350b5e2ba67f4`. Confere com o checksum publicado nesse Maven. Este valor identifica o artefacto Maven, não necessariamente o ficheiro anexo da release GitHub.

## Diferenças confirmadas

Comparação upstream: três commits e sete ficheiros. Mantém mpv no commit `3186d369f9f090cd1363be0ac46a037824b702c6` e FFmpeg n9.0.2.

1. **Espera do MediaCodec:** chamadas de dequeue passam timeout zero. Quando o chamador pede espera, o FFmpeg repete a consulta com pausas até 1 ms e respeita o prazo solicitado. Evita uma espera nativa que se mostrou bloqueada no decoder Realtek analisado pelo autor.
2. **Decoder sem progresso:** quando entrada e saída devolvem simultaneamente EAGAIN durante cerca de três segundos, devolve AVERROR_EXTERNAL e regista `MediaCodec stalled`. Permite ao mpv reconhecer falha e tentar recuperação/fallback. Três segundos não garantem recuperação completa nesse tempo.
3. **Correção documental da 2026.10.1:** a espera AudioTrack de 10 ms evitava consumo de CPU em ciclo ativo durante falta de amostras; o autor retirou a alegação de que corrigia o congelamento inicial. O código desse patch de áudio não mudou na 2026.10.2. Esta informação corrige a interpretação do relatório anterior desta app.
4. **Script WSL:** facilita construir/testar a biblioteca pelo mantenedor. Não melhora diretamente o APK publicado e não é necessário importar para a nossa compilação normal.

A comparação binária do Maven confirma:

| Verificação | Resultado |
|---|---|
| Integridade ZIP e checksums SHA256/SHA1 | Sem erro; coincidentes |
| Classes Java/Kotlin do wrapper | Nove classes idênticas |
| Manifesto | Idêntico; mantém contrato mínimo Android API 26 |
| ABIs | arm64-v8a, armeabi-v7a e x86_64 |
| Entradas adicionadas/removidas | Nenhuma |
| Binários alterados | Apenas libmpv.so e libavcodec.so em cada ABI |
| Tamanho AAR | 36 344 164 → 36 345 625 bytes; +1 461 bytes |

Não foi executada a biblioteca candidata. Compilação, símbolos JNI/exportados e integração final do APK devem ser verificados no ensaio de implementação; identidade do wrapper não prova reprodução correta.

## Aplicabilidade à nossa OwnTV

O benefício direto é no mpv que utiliza MediaCodec: tornar recuperável uma condição de bloqueio do decoder e permitir que o leitor volte a responder. Pode ajudar na abertura de diferidos e no cancelamento de reprodução afetada por esse bloqueio.

Não muda Media3/ExoPlayer, codecs disponíveis, qualidade de imagem, lógica de pedidos HLS, prioridades de versões ou regras de handover HTTP. Não demonstra uma solução para HTTP 403, limites de sessões do fornecedor ou `Previous stream requests did not finish within the handover deadline`. Um decoder bloqueado pode dificultar libertar a reprodução; isso não prova que seja a causa dos nossos problemas anteriores.

A extensão FFmpeg de áudio do ExoPlayer utiliza `libffmpegJNI.so`, separada. O manifesto local dessa extensão não declara dependência de libavcodec do mpv. A atualização não substitui o seu processamento de áudio; testar coexistência entre motores continua apropriado.

## Ressalva principal: saída de vídeo e fallback

O resultado upstream foi obtido numa TCL/Realtek Android 14, num diferido Stalker, com `hwdec=mediacodec-copy` e `vo=gpu`. O autor reportou cerca de 5 bloqueios em 12 aberturas antes; depois 11/11 aberturas funcionaram, sete com fallback para software, primeira imagem em cerca de 5,5 s e 1080p50 sem frames descartados. São observações do autor, não medições nas nossas boxes.

No nosso `OwnTVPlayer.kt`, hardware direto usa `hwdec=mediacodec` e `vo=mediacodec_embed` (aproximadamente linhas 663–678). A recuperação por reload da app pode selecionar copy/software e `vo=gpu` (aproximadamente 1760–1797). Porém, a notificação `hwdec-current` apenas atualiza estado e executa a proteção do decoder (aproximadamente 4412–4418); não muda aí a saída de vídeo.

**Inferência pendente:** fallback automático dentro da mesma reprodução não foi demonstrado com a saída direta da nossa app. Não afirmar que falha, mas exigir imagem e som efetivos no teste. As proteções existentes podem impedir software em resoluções elevadas ou dispositivos limitados.

Os patches não são limitados a TCL/Realtek: afetam os caminhos MediaCodec abrangidos pelo código. O polling pode aumentar despertares de CPU; o fallback pode aumentar carga e temperatura. O resultado 1080p50 upstream não garante reprodução 4K/HEVC em software nas boxes do utilizador.

## Implementação proposta, apenas após avançar

1. Preservar uma referência de comparação 2026.10.1; alterar somente o pin libmpv no Core TV para 2026.10.2.
2. Verificar símbolos/dependências nativas, alinhamento e empacotamento do candidato; atualizar o registo `tools/mpv/native_manifest.json` com a proveniência efetivamente validada.
3. Compilar TV/Core e executar verificações relevantes. Preservar hardware preferido, volume a 100%, configurações por canal, ExoPlayer e Core privado Mobile.
4. Comparar versões com os mesmos conteúdos e equipamento. Não alterar buffers, latências e saídas globais durante esta comparação.
5. Se o fallback direto não produzir imagem/som, investigar essa transição antes da adoção definitiva; não mudar todos os canais para software ou copy por precaução.
6. Reverter para 2026.10.1 e respetiva proveniência se houver regressão. Não existe migração de dados nesta atualização nativa.

## Aceitação no dispositivo

- Repetir 10–12 aberturas do conteúdo afetado, se existir; registar `MediaCodec stalled`, `hwdec-current`, saída de vídeo, tempo até primeira imagem, som e capacidade de cancelar/mudar canal.
- Testar hardware direto, recuperação copy e software; incluir direto, diferidos, seek e regresso à lista/fullscreen.
- Testar sequência rápida A–B–C–A e cancelamento durante abertura, confirmando que o leitor continua responsivo e não fica a segurar uma reprodução antiga.
- Reproduzir canais saudáveis H.264/HEVC em 1080p e 4K, com HDR quando usado, durante pelo menos 30 minutos; comparar quedas, som, aquecimento e fluidez.
- Verificar preview/fullscreen e alternância mpv/Exo no mesmo processo, incluindo áudio FFmpeg do Exo.
- Incluir Xiaomi e Thomson e, se se declarar suporte validado aos Android antigos, um dispositivo Android 8/API 26. Manifesto compatível não equivale a validação física.

## Fontes primárias

- Release: https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.10.2
- Comparação: https://github.com/ahXN00/OwnTV_libmpv/compare/v2026.10.1...v2026.10.2
- Alteração e teste upstream: https://github.com/ahXN00/OwnTV_libmpv/commit/a435770a88877b871f6f87e6f4d353b64c3f4eb9
- Patch dequeue: https://github.com/ahXN00/OwnTV_libmpv/blob/v2026.10.2/buildscripts/patches/ffmpeg/0001-mediacodec-never-pass-a-dequeue-timeout.patch
- Patch stall: https://github.com/ahXN00/OwnTV_libmpv/blob/v2026.10.2/buildscripts/patches/ffmpeg/0002-mediacodec-fail-a-stalled-decoder.patch
- Pins: https://github.com/ahXN00/OwnTV_libmpv/blob/v2026.10.2/buildscripts/include/depinfo.sh

Limite desta análise: fontes e artefactos inspecionados; sem compilação com o candidato, APK ou teste físico. A recomendação é condicional, não uma declaração de problema resolvido.
