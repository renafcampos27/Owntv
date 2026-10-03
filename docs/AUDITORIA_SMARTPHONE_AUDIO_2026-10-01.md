# Auditoria — erros visíveis no smartphone e cortes de áudio na box

Data: 2026-10-01. Análise do checkout atual; sem alterações ao código de produção ou às definições. Não houve reprodução em equipamento nem recolha de logs do instante do corte.

## Sintomas relatados

- Smartphone: em cada abertura/mudança de canal aparece um erro de ligação HLS/recusa do fornecedor, mas o vídeo continua a reproduzir. Carregar em repetir recarrega e remove o aviso. O código HTTP exato não está confirmado; a referência do utilizador a «443» não foi convertida automaticamente para 403.
- Box: cortes de áudio breves, aproximadamente 0,2 s, separados por minutos. Saída de áudio, motor ativo e comportamento simultâneo do vídeo ainda não confirmados.

## Achados e correções propostas

### A1 — estado terminal incompatível com reprodução real — P1

Evidência: `player-core/.../LivePreviewEngine.kt`, `failLoad()` e callback `STATE_READY`; `app/.../player/PlayerHud.kt`, overlay de estado.

`failLoad()` publica erro e estado terminal, mas não interrompe o ExoPlayer subjacente. Se a preparação terminar entretanto, READY publica PLAYING e limpa buffering sem limpar explicitamente `error/errorInfo/gaveUp`. O HUD dá prioridade a qualquer erro publicado, independentemente da reprodução atual. É uma lacuna confirmada no modelo de estados; a origem concreta do aviso HTTP relatado ainda requer o evento que o publicou.

Correção: erro e recuperação devem pertencer à geração/source atual e ser atualizados coerentemente. Um sucesso posterior comprovado na mesma fonte deve invalidar o erro anterior, sem ocultar um erro HTTP atual nem reabrir o canal só para retirar o aviso. Testar timeout seguido de READY/frame, erro antigo depois de nova sintonia e falha real sem sucesso posterior.

### A2 — decisão de desistência sem última verificação de sucesso — P1

Evidência: `player-core/.../LiveTuneController.kt`, `startAlarm()`/`advance()`; `LiveExoWatchdog.kt`, abertura observada por polling.

O alarme verifica dono/canal e orçamento, mas não revalida uma abertura acabada de acontecer antes de chamar abandon. O watcher pode observar a abertura no seu ciclo seguinte. Esta condição permite uma disputa entre sucesso e deadline e deve ser testada; não está demonstrado que explique todas as aberturas no smartphone.

Correção: identidade de tentativa/fonte e evento de abertura único; antes da decisão terminal, revalidar sucesso e falha atuais. Não aceitar um simples READY como prova de vídeo saudável para todos os casos; conservar classificação audio-only e evidência de renderização quando aplicável.

### A3 — watchdog de áudio rearmado indevidamente na reutilização — P1

Evidência: `player-core/.../AudioOutputPolicy.kt`, `onAudioInputFormatChanged()`, `onAudioDecoderInitialized()`, `onAudioPositionAdvancing()` e `poll()`.

Qualquer mudança de formato limpa `advancing` e `decoderInitialized`. A reutilização do decoder/saída não exige novo evento de inicialização nem novo início do AudioTrack. O watchdog pode voltar a esperar um evento que não tem de ocorrer e, após seis segundos de reprodução contabilizada, concluir que não há som. O owner reconstrói então o player e força estéreo. Também há inferência de passthrough pela ausência de uma inicialização de decoder recém-observada.

Confirmado por leitura do código e confronto com as fontes Media3 1.11.1 presentes no cache Gradle: o renderer publica mudança de formato com avaliação de reutilização; a notificação de posição depende do início/retoma da saída. Não foi confirmado que este caminho se execute no intervalo dos cortes relatados.

Correção: distinguir mudança de formato, reutilização e nova saída. Conservar evidência válida enquanto a saída não é substituída; identificar PCM/passthrough pela configuração efetiva. Rearmar apenas com a identidade correta da saída. Testes: decoder reutilizado, formato sem nova saída, nova saída sem avanço e nova sintonia.

### A4 — recuperação de underrun sem prova de reserva no Live — P1

Evidência: `LivePreviewEngine.kt` constrói `AudioWatchdog(accepts=...)` com `canRecoverUnderrun` predefinido em true. `ExoSubtitleEngine.kt` já exige reprodução e pelo menos 2 s de reserva para esta decisão.

O Live pode tratar quatro underruns passthrough em dez segundos como problema da saída sem distinguir falta de media. Isso pode reconstruir um player por atraso de rede/origem. PCM corretamente classificado não dispara este ramo. Cortes separados por minutos não atingem, por si só, este limiar.

Correção: incorporar estado/reserva e classificar a causa antes da recuperação. Não reconstruir por um microcorte isolado; manter resposta a falhas reais da saída.

### A5 — descodificação síncrona forçada globalmente — candidato a ensaio, P2

Evidência: `player-core/.../ExoRenderers.kt` chama `forceDisableMediaCodecAsynchronousQueueing()` para todos os ExoPlayers.

A política pode ter uma razão de compatibilidade; remover o bloqueio não é automaticamente seguro. Contudo, desativa também o caminho assíncrono que o Media3 utiliza por defeito no Android 12+ e que pode reduzir underruns e fotogramas perdidos.

Proposta: Auto, Assíncrono e Compatibilidade síncrona, com Auto respeitando a biblioteca e exceções de dispositivo verificadas. Ensaio comparativo por motor/device/codec, sem alteração de HLS, qualidade ou buffers. Manter retorno à política atual quando houver regressões. Não foi medida vantagem nesta box.

### A6 — observabilidade incompleta do áudio e foco — P2

`AudioWatchdog` já envia underruns para `LiveDiagnosticsLog`. Mudanças de formato, decoder e descontinuidades de timestamps ficam sobretudo em Logcat; faltam eventos completos de saída criada/libertada, rota efetiva e mudanças de foco no diagnóstico exportado. `PlaybackSession.publish()` abandona foco quando isPlaying=false, incluindo buffering, e volta a pedir ao recuperar; isso é um risco de transições desnecessárias, não causa comprovada dos cortes periódicos.

Correção: eventos pequenos, só em transições, com owner, reserva, duração desde a última alimentação, formato/reuse, rota, velocidade efetiva, timestamp gap, criação/libertação da saída e foco. Distinguir pausa solicitada de buffering/supressão na posse de foco. Não escrever por frame nem expor URLs/credenciais.

### A7 — Back compacto confundido com modo simples — P2

Evidência: `app/.../features/shell/OwnTVShell.kt`. `sidebarHidden` também é true em janela compacta; o ramo `sidebarHidden -> showSimpleMenu=true` precede o regresso de Settings.

Correção: separar layout compacto de ocultação pedida no modo simples. Back deve fechar overlays, sair do leitor e regressar à página anterior segundo a hierarquia, sem depender da existência física do rail.

### A8 — rotação tratada como saída da app no ViewModel — P1

Evidência: `OwnTVShell.kt` chama `liveVm.onBackground()` em todos os ON_STOP. `MainActivity.onStop()` protege isChangingConfigurations, mas o observer da shell não tem essa proteção. `LiveViewModel.onBackground()` cancela recuperação e, se houver timeshift local, para e descarta a sessão.

Correção: coordenar lifecycle numa única política, distinguindo recriação da Activity de background real. Preservar sintonia/recuperação/sessão local durante rotação e substituir a Surface, sem manter playback invisível após Home real.

### A9 — política de áudio da TV aplicada ao smartphone — P2

Evidência: `app/.../di/PlayerModule.kt` instancia `PlaybackSession(androidContext())`; defaults DUCK e pauseWhenOutputDisconnects=false. A classe tem política PAUSE/auscultadores, mas não é selecionada por dispositivo nesta App adaptada.

Correção: selecionar política TV/telefone pela classificação do dispositivo, separadamente da largura da janela. No telefone, testar chamada, perda permanente/transitória de foco e retirada de auscultadores/Bluetooth. Não associar este achado ao aviso HLS sem evidência.

## Ordem de implementação recomendada

1. A1/A2: corrigir consistência do erro e a decisão de desistência; reproduzir opening/deadline/retry com testes determinísticos. O vídeo saudável não deve precisar de reload para remover um erro anterior.
2. A3/A4 + instrumentação A6: corrigir decisões de recuperação de áudio e recolher dois cortes reais antes de alterar o caminho de processamento.
3. A8/A7/A9: lote de lifecycle, Back e áudio específico do smartphone; aceitação de rotação/IME/chamadas/auscultadores.
4. A5: ensaio de queueing assíncrono. Ajuste específico do buffer AudioTrack apenas se forem demonstrados underruns PCM com media disponível; não aumentar a reserva de rede globalmente.

## Teste de áudio que distingue as causas

- Underrun com reserva de media disponível: investigar agendamento/decoder/saída, comparar Auto vs síncrono; só depois avaliar buffer PCM da saída.
- Reserva esgotada com BUFFERING: investigar tempos de playlist/segmentos/rede; não culpar a saída nem forçar estéreo.
- Discontinuidade PTS/formato coincidente: verificar origem e reutilização; pré-buffer não repara timestamps em falta.
- AudioTrack/libertação/foco/rota coincidente: corrigir ciclo de saída/foco ou compatibilidade HDMI/Bluetooth.

Critérios: abrir/mudar/repetir canais sem aviso falso; erro real continua visível e retry funciona; comparação de áudio durante 30–60 min com mesmo canal/saída, eventos coincidentes e sem regressão de zapping. A duração do ensaio não garante ausência de falhas futuras.

## Referências primárias

- [Configuração Media3 e queueing assíncrono](https://developer.android.com/media/media3/exoplayer/customization#enabling-asynchronous-buffer-queueing).
- [AnalyticsListener: eventos de áudio, underrun e saída](https://developer.android.com/reference/androidx/media3/exoplayer/analytics/AnalyticsListener).
- [MediaCodecAudioRenderer 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/MediaCodecAudioRenderer.java).
- [DefaultAudioSink 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java).

As fontes do artefacto 1.11.1 efetivamente presente no cache Gradle também foram consultadas para verificar a reutilização e a notificação de avanço. As referências não provam a causa concreta de um corte sem os logs do equipamento.
