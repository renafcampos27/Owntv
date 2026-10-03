# Compatibilidade HLS por canal

## Decisão e âmbito

Implementação autorizada após revisão independente de HLS, codecs e council. Preservar o percurso habitual dos canais sem ajustes. Não existe configuração automática especial para Odisseia. A causa desse stream continua por confirmar; funcionar mal noutras apps não identifica o defeito.

## Utilização

Definições → Reprodutor de vídeo → Motor e imagem → Configurações por canal → lista → canal/versão.

- HLS: detetar limites dos fotogramas — ExoPlayer. Apenas H.264 em segmentos MPEG-TS sem AUD; pode aumentar CPU.
- HLS: aceitar fotogramas-chave alternativos — ExoPlayer. Apenas H.264/TS sem IDR; pode causar corrupção visual temporária no arranque/recuo.
- HLS: descobrir faixas pelos segmentos — ExoPlayer. Desativa chunkless preparation nesse canal; aumenta tempo de abertura. Não corrige automaticamente declarações CODECS incorretas.
- Formato Apenas HLS + motor mpv explícito: alternativa manual, sem TS direto. Herdar mantém o motor ExoPlayer habitual com HLS exclusivo.

Todas as opções de extração estão desligadas por defeito. Guardar e reabrir aplica. Repor e Guardar remove as exceções. As opções ExoPlayer não são interpretadas pelo mpv.

## Isolamento e persistência

Opções ligadas à fonte e identificador do canal/versão, transportadas no snapshot da sintonia. JSON antigo sem campos mantém os padrões. False e true explícitos são preservados. Backup completo inclui as novas opções através de channelPlaybackConfigs, com o remapeamento de fontes existente.

A factory HLS compara a configuração efetiva antes de construir a fonte e repõe flags/preparação quando muda para um canal normal. Reconstrução da factory HTTP invalida também a configuração de extração memorizada. Fontes já construídas conservam as suas próprias opções.

## Diagnóstico

Registar seleção pedida, nome real, flags/canonical name fornecidos pelo fabricante, presença de erro observado e formato de vídeo. Uma classificação software não demonstra por si só que o hardware falhou. Os registos usam os limites e o escritor de diagnóstico existentes; não acrescentam polling por fotograma nem processamento contínuo de PCM.

## Verificação física necessária

Comparar uma mudança de cada vez no mesmo stream/box. Medir arranque, 30 minutos de reprodução, fotogramas perdidos, áudio, zapping e recuo/diferidos. Alternar canal personalizado e normal para confirmar ausência de transferência das opções. Não há garantia de corrigir segmentos ausentes ou corrompidos na origem.

## Fontes

- https://developer.android.com/media/media3/exoplayer/troubleshooting#why-do-some-mpeg-ts-files-fail-to-play
- https://developer.android.com/media/media3/exoplayer/hls#disabling-chunkless-preparation
- https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer_hls/src/main/java/androidx/media3/exoplayer/hls/DefaultHlsExtractorFactory.java
- https://developer.android.com/reference/android/media/MediaCodecInfo#isHardwareAccelerated()

## Validação automatizada

Compilação StandardDebug e verificações i18n concluídas. 1718 testes de app/core/player-core, zero falhas, erros ou testes ignorados. Cobertura de JSON antigo/novas flags, motor HLS explícito, headers e URL com parâmetros, ausência de fallback TS e regresso ao padrão no canal seguinte. Revisão independente final confirmou reposição da factory e snapshot das fontes. Log: %TEMP%/owntv-hls-compat-20261003-final.log. Não houve teste físico nem geração de APK nesta implementação.
