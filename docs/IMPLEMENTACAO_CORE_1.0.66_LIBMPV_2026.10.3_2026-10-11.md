# Integração seletiva Core 1.0.66 e libmpv 2026.10.3

Data: 11/10/2026. Autorização: implementar as recomendações por fases.
Âmbito: OwnTV para TV e Core externo personalizado. O projeto Mobile e o seu Core privado não foram alterados.

## Fase 1 — Motor nativo

- Dependência `tv.own.owntv:libmpv` atualizada de 2026.10.2 para 2026.10.3.
- Confirmada a presença de libowntvffmpeg, FFmpeg e mpv nas arquiteturas ARMv7, ARM64 e x86_64; verificados os nomes de classes JNI e o callback growOutputBuffer no binário.
- A versão contém a correção de falso bloqueio MediaCodec, a alteração de aleatoriedade TLS, proteções nativas e redução de componentes do motor upstream.

## Fase 2 — Áudio

- Java do descodificador FFmpeg Media3 1.11.1 integrado no pacote específico tv.own.owntv.player.ffmpeg, correspondente ao JNI da nova libmpv. Licenças originais preservadas.
- Automático mantém MediaCodec antes de FFmpeg; preferir áudio por software mantém FFmpeg antes do descodificador do dispositivo. Não se ativa um renderer de vídeo por software devido à escolha de áudio.
- Retirados os quatro ficheiros Java e as quatro bibliotecas libffmpegJNI antigas, após verificação estática do contrato nativo e cópia de segurança.
- Preservado o misturador estéreo local com posições dos canais e normalização. O volume interno mantém-se a 100%.
- A avaliação de áudio aceita faixas que excedam as capacidades anunciadas. Erros efetivos continuam a seguir a recuperação existente.
- A avaliação de passagem para ExoPlayer reconhece também a ponte FFmpeg para DTS/TrueHD/MP2; MLP não é confundido com TrueHD. O leitor de diferidos/ficheiros sinaliza áudio existente mas sem faixa reproduzível uma vez por abertura, reutilizando os callbacks existentes e respeitando motores exclusivos.
- ExoPlayer reavalia a seleção quando as capacidades da saída mudam. O mpv usa os layouts PCM anunciados pela saída e escreve audio-channels numa mudança de rota apenas se o valor mudar. Callback removido no release; comandos não atingem uma instância mpv aposentada.

## Fase 3 — Recuperação mpv

- network-timeout de 10 segundos no direto, 60 segundos nos restantes conteúdos. Não altera o tempo de desistência do controlador.
- Cache nativa a crescer enquanto paused-for-cache está ativo evita um diagnóstico prematuro de congelamento. A margem desse episódio é de 5 segundos, sem crédito indefinido. Buffer parado, decrescente ou inválido não recebe margem; só progresso de reprodução permite um novo episódio.
- Estado de cache limpo quando a reprodução é substituída; o watchdog continua associado à geração da abertura.
- Mantidos reserva/atraso independentes, limite superior de 70 segundos, proteção de memória, HLS estrito e cancelamento existente.
- Não importadas tentativas HTTP 458 próprias de TS para o percurso HLS, nem uma recuperação paralela.

## Fase 4 — Comando e pesquisa

- MediaSession aceita Play/Pause quando recebe apenas ACTION_UP. Pares DOWN/UP normais continuam a ser tratados pelo Android, sem segunda ação. O toggle usa a intenção de reprodução, incluindo durante buffering.
- Pesquisa FTS separa termos por pontuação: RTP-1 torna-se RTP* 1*, em vez de RTP1*. Mantido o limite de resultados anterior.

## Preservação e validação

Originais e bibliotecas antigas: C:\Users\renat\AppData\Local\Temp\owntv-core66-before-20261011. As cópias iniciais foram verificadas por hash. Não usar um reset global de Git para reverter, porque existem alterações anteriores.

Validação central concluída com sucesso: compilação Standard Release, 330 testes da app, 1 150 do Core e 481 do player (1 961 no total, sem falhas), lint vital, merge de bibliotecas nativas e minificação R8, sem gerar APK. Seis testes novos; os dois testes antigos FFmpeg foram migrados para o pacote novo, preservando as verificações MP2/AAC e áudio sem vídeo. Inventários de tradução da app e do Core aprovados; apenas literais técnicos revistos foram classificados.

O AAR resolvido pelo Gradle corresponde por SHA-256 ao pacote usado na verificação nativa. A junção do release contém libowntvffmpeg nas três ABIs, sem libffmpegJNI. O mapeamento R8 atual, em app/build/intermediates/mapping/standardRelease/minifyStandardReleaseWithR8/mapping.txt, conserva FfmpegLibrary, FfmpegAudioDecoder, SimpleDecoderOutputBuffer e growOutputBuffer. Isto confirma o contrato estático do release; não demonstra descodificação física na box.

Registo: C:\Users\renat\AppData\Local\Temp\owntv-core66-validation-final.log. Não houve APK, commit ou publicação. Sincronizar o projeto no Android Studio antes de gerar o APK novo.

Verificação física necessária: som AAC/MP2/AC3/DTS disponível no fornecedor, modos automático/software, estéreo, troca HDMI/soundbar quando aplicável, direto/diferido, buffer inicial e reserva 8–30/8–60, zapping rápido e Play/Pause. A alteração não demonstra que o bloqueio A–B–A tenha desaparecido.

## Referências

- https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.66
- https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.10.3
