# Fase 4 — volume e sincronismo áudio/vídeo

Implementação de 28/09/2026 no Core local utilizado por OwnTV-main.

## Entregue

- Curva comum de volume: 0–100% representa amplitude linear; 100–150% acrescenta até 10 dB. O valor enviado ao mpv é convertido para compensar a curva cúbica deste motor. O ExoPlayer utiliza o efeito de amplificação já existente, com o mesmo ganho alvo.
- O relógio de áudio do ExoPlayer recebe o ajuste de sincronismo, limitado a −5000/+5000 ms. Zero mantém o comportamento anterior. O ajuste é manual e não tenta corrigir automaticamente fotogramas perdidos.
- O direto usa o ajuste global ao abrir e permite guardar um valor por canal através do menu de áudio existente. Uma leitura atrasada das preferências não substitui um ajuste manual acabado de fazer.
- VOD e fallback para ExoPlayer também recebem o ajuste ativo, antes da abertura do motor. Ajustes posteriores são transmitidos ao motor.
- O amplificador do ExoPlayer VOD volta a ser associado quando muda a sessão de áudio, aproveitando a atualização periódica existente; não foi acrescentado outro temporizador.
- Mantidas as definições guardadas, a estrutura da base de dados e a navegação do comando.

## Decisões de âmbito

- ChannelRecall não foi acrescentado: mantém-se o pedido anterior de excluir funções de canal anterior/últimos canais.
- Mantido `dev.jdtech.mpv:libmpv:1.0.0`. A referência local core-1.0.61 aponta para uma distribuição diferente, `tv.own.owntv:libmpv:2026.09.2`, cuja própria configuração prevê teste em dispositivo. Não foi demonstrada uma falha de codec que exija substituir o binário nesta fase.
- Não foi implementado timeshift local nem qualquer migração de base de dados.

## Limites e teste nas boxes

O ganho real acima de 100% depende do suporte do dispositivo ao LoudnessEnhancer e da saída de áudio. Passthrough e processamento do televisor/recetor podem impedir igualdade de volume audível. Esta alteração alinha o ganho pedido; não constitui uma medição acústica.

Alterar o sincronismo durante a reprodução pode provocar uma breve retenção ou descarte de imagem enquanto o relógio se ajusta. Deve ser usado para desfasamento áudio/vídeo, não para tentar eliminar pausas de rede.

Verificar na Xiaomi e na Thomson:

1. Canal sincronizado a 0 ms mantém áudio e imagem como antes.
2. Menu de áudio: experimentar +100 e −100 ms e voltar a zero.
3. Guardar um ajuste num canal, mudar de canal e regressar; confirmar a persistência após reiniciar a app.
4. Comparar volume 50%, 100% e 125% entre ExoPlayer/mpv, usando o mesmo conteúdo e saída; verificar mute e restauro.
5. Confirmar os mesmos ajustes no VOD ExoPlayer e após uma reabertura do motor.

Nenhum APK foi gerado.

## Referências verificadas

- [mpv: cálculo do ganho de áudio](https://github.com/mpv-player/mpv/blob/master/player/audio.c) — `audio_get_gain` aplica a curva cúbica.
- [Media3: ForwardingAudioSink](https://developer.android.com/reference/androidx/media3/exoplayer/audio/ForwardingAudioSink) — extensão do relógio via `getCurrentPositionUs`, preservando o estado sem posição disponível.
- Implementação local de referência: tag `core-1.0.61`, adaptada sem importar as restantes funcionalidades dessa versão.

## Validação local

Compilação StandardDebug e testes unitários concluídos com sucesso.

- OwnTV-main: 179 testes, 0 falhas e 0 erros.
- OwnTV_Core: 238 testes, 0 falhas e 0 erros.

Incluem 12 testes novos da curva de volume e do relógio de sincronismo. Hardware real não testado.
