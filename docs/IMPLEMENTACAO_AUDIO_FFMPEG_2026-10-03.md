# ExoPlayer: alternativa automática de áudio FFmpeg

## Comportamento

- Preferir áudio por software desligado: modo Automático. O renderer Android tem prioridade; FFmpeg fica disponível para formatos sem suporte no renderer Android, incluindo MP2 (`audio/mpeg-L2`).
- Opção ligada: FFmpeg tem prioridade para os formatos que suporta; o renderer Android continua disponível.
- A decisão é por faixa de áudio/dispositivo. Não existe uma lista fixa de marcas de boxes.
- Só é registado o renderer FFmpeg de áudio. O percurso de vídeo não ganha uma extensão de software.
- HLS, volume interno a 100%, persistência e backup da opção mantêm-se.
- Um descodificador Android que anuncia suporte mas produz silêncio sem erro não é automaticamente identificável. Nesse caso, usar Preferir software para comparar.
- Faixas anunciadas sem seleção continuam visíveis no diagnóstico, com formato e indicação de incompatibilidade/não seleção. Não são confundidas com ausência de áudio no stream.

## Dependências e rastreabilidade

As classes Java do renderer de áudio são as fontes oficiais Media3 **1.11.1**, compiladas no player-core contra a mesma versão dos restantes módulos Media3. Não se inclui o renderer experimental de vídeo.

Os quatro binários `libffmpegJNI.so` vêm do artefacto Maven Central mantido por Jellyfin, `org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1`. Não se inclui o seu classes.jar nem se baixa a versão do Media3 da aplicação. As assinaturas JNI e o algoritmo de áudio foram comparados com as fontes 1.11.1: a alteração no wrapper upstream é a forma de registar as mesmas funções JNI. Os binários publicados exportam os nomes JNI usados pelas classes Java atuais.

- Artefacto: https://repo.maven.apache.org/maven2/org/jellyfin/media3/media3-ffmpeg-decoder/1.9.0+1/media3-ffmpeg-decoder-1.9.0+1.aar
- SHA-256 do artefacto: `dfc726c8bc9d9db02e15b160f886a03bdd6dd51e88ba2de89aa74caf5e8cd181`.
- Projeto de compilação: https://github.com/jellyfin/jellyfin-androidx-media/tree/v1.9.0%2B1
- Submódulo Media3 da compilação nativa: `7cc1056f840ce226598d3b990d4a6f7cd17e2831`.
- Submódulo FFmpeg: `37c90f55d8b6c3948c79449b04755527b4ae1351`, https://git.ffmpeg.org/ffmpeg.git.
- As quatro ABIs têm segmentos ELF LOAD alinhados a 16 KiB. A variante Standard inclui ARMv7 e ARM64; os binários x86/x86_64 permitem outras variantes/testes.
- Os avisos e licenças Apache-2.0/GPL-3.0 são incluídos nos assets do player-core. A licença GPL-3.0 do OwnTV_Core é preservada.
- As regras de redução de código preservam as classes/métodos procurados pelo JNI.

No OwnTV_Core, `python tools/ffmpeg/verify_audio.py` verifica checksums, entradas JNI e alinhamento dos binários. `--restore` recupera apenas os ficheiros identificados no manifesto, verificando os hashes antes de escrever.

## Validação e limite

Compilar a app, executar os testes do player-core/app e verificar `mergeDebugNativeLibs`. Os testes cobrem a associação MP2 ao decoder MPEG do FFmpeg, a rejeição de vídeo pelo renderer de áudio e a apresentação de faixas incompatíveis.

A confirmação de som exige instalar o APK atualizado na box: testar AAC e MP2 em modo Automático, depois MP2/AAC com Preferir software. Verificar a linha Descodificador de áudio (nome iniciado por `ffmpeg` quando escolhido), som, zapping e volume. Se não houver nenhuma faixa anunciada, investigar a descoberta HLS; acrescentar um decoder não cria uma faixa ausente.

Os testes no computador e a verificação estática dos binários não comprovam a reprodução na Mortal T1.

Validação executada em 03/10/2026: compilação StandardDebug concluída; 307 testes da app e 392 testes do player-core sem falhas; mergeDebugNativeLibs concluído; manifesto, entradas JNI e alinhamento de 16 KiB verificados. Não foi gerado um novo APK nem executado um teste físico na box.
