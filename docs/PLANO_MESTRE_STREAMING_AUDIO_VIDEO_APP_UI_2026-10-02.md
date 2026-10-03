# OwnTV — plano mestre de correções e otimizações

**Data:** 2026-10-02. **Estado:** auditoria e council concluídos; correções e otimizações seguras implementadas na app e no Core local. Estado de validação e limites de medição no [relatório de implementação](IMPLEMENTACAO_PLANO_MESTRE_2026-10-02.md).

**Decisão posterior do utilizador:** volume interno sempre a 100%, com controlo exclusivo pelo sistema/box. Esta decisão substitui as propostas de ajuste, boost, restauro ou atenuação de volume deste plano. O mute dos previews permanece separado. O texto de análise abaixo é preservado como rastreabilidade; O6/O9 e a aceitação em hardware são ensaios por realizar, sem alteração especulativa de defaults.

## 1. Decisão e limites da análise

Corrigir primeiro o controlo da reprodução e a navegação; depois a coordenação HLS/HTTP; em seguida o EPG e o trabalho desnecessário da interface. Tratar AFR de diferidos, timeshift local e conversão estéreo em entregas próprias. A instrumentação mínima acompanha cada lote.

A revisão incidiu no checkout atual da app e do Core local, incluindo alterações anteriores ainda não versionadas. Três agentes especializados examinaram áudio/vídeo/ciclo de vida, HLS/rede/timeshift e Compose/foco/desempenho. Uma segunda ronda de council questionou prioridades, alcance e contraindicações. O coordenador cruzou os caminhos principais e acrescentou a análise de EPG, arranque e subscrição da linha temporal. A revisão complementar do último achado da linha temporal não foi concluída pelo agente; esse achado foi verificado diretamente pelo coordenador.

**Confirmado no código** significa que o caminho descrito existe. Não significa reprodução do sintoma na Xiaomi ou na Thomson. Não foram recolhidos novos traces de equipamento, medidos cortes de áudio ou executados testes desta auditoria. Não está demonstrado que os cortes de 0,2 s sejam causados por GC, CPU, RAM, decoder, falta de pacotes ou disco. Os problemas num canal que também falha noutra app continuam separados dos defeitos da OwnTV.

Este documento consolida os dez achados pendentes B1–B10 da auditoria anterior, dezassete novos pontos C1–C17 e oportunidades de desempenho O1–O9. C17 é robustez condicionada por uma cadência longa; C5 é inconsistência de política com impacto por medir. As oportunidades não são apresentadas como defeitos audíveis demonstrados.

### Raízes e rastreabilidade

- **App:** C:/Users/renat/Downloads/OwnTV-main
- **Core:** C:/Users/renat/Downloads/OwnTV_Core
- **App live:** app/src/main/java/tv/own/owntv/features/live/
- **App shell:** app/src/main/java/tv/own/owntv/features/shell/
- **App player:** app/src/main/java/tv/own/owntv/player/
- **Core player:** player-core/src/main/java/tv/own/owntv/player/
- **Core live:** core/src/main/java/tv/own/owntv/core/live/
- **Core timeshift:** core/src/main/java/tv/own/owntv/core/timeshift/

As referências ficheiro:linha correspondem ao estado observado e devem ser verificadas antes de editar. Media3 declarado na análise: 1.11.1. Os caminhos B, salvo indicação, são relativos à raiz Core.

Documentos de origem preservados em docs da app:

- AUDITORIA_SMARTPHONE_AUDIO_2026-10-01.md
- CORRECOES_SMARTPHONE_AUDIO_IMPLEMENTACAO_2026-10-01.md
- AUDITORIA_PROFUNDA_REPRODUCAO_AUDIO_HLS_COUNCIL_2026-10-02.md

## 2. O que já foi implementado — não repetir como trabalho pendente

O relatório de implementação de 2026-10-01 regista o lote A1–A9:

| ID | Alteração anterior | Estado da evidência |
|---|---|---|
| A1 | Limpar erro antigo quando a fonte atual demonstra reprodução saudável | Implementado; não usar READY sozinho como sucesso |
| A2 | Revalidar sucesso antes do prazo final/troca automática | Implementado; limites de abertura continuam ativos |
| A3 | Watchdog distingue saída reutilizada de nova saída de áudio | Implementado; conservar identidade dos callbacks |
| A4 | Resgate de passthrough exige reprodução e reserva, distinguindo falta de media | Implementado; não fazer resgate por qualquer underrun |
| A5 | Exo: Automático / Assíncrono / Compatibilidade síncrona | Implementado, persistente e incluído em backup; aplicado na abertura seguinte |
| A6 | Diagnóstico de áudio/decoder/foco e intenção Live | Implementado; B1 demonstra que ainda falta coerência nos outros caminhos |
| A7 | Back distingue layout compacto e modo simples | Implementado |
| A8 | Recriação/rotação preserva reprodução e timeshift local | Implementado; C3 encontra interação AFR adicional |
| A9 | Telefone usa pausa em interrupções/retirada de saída; TV mantém política própria | Implementado; B1/B4/B5 corrigem falhas remanescentes da sessão |

O relatório anterior regista compilação e 1 533 testes aprovados: app 267, Core 939, player-core 327. Estes resultados não foram repetidos nesta auditoria e não comprovam cobertura dos novos cenários. Lint anterior: player-core sem erros; app com quatro erros anteriores ao lote e avisos. Uma regra foi excluída apenas numa execução temporária; não declarar aprovação integral de Lint.

## 3. Achados anteriores ainda pendentes — B1–B10

### B1 — pausa, Play e recuperação não partilham uma intenção consistente — P1

**Evidência:** `player-core/src/main/java/tv/own/owntv/player/PlaybackEngine.kt:15` e `:166`; `OwnTVPlayer.kt:1517`; `PlaybackSession.kt:205`, `:206`, `:270`; `LivePreviewEngine.kt:812`, `:1124`, `:1256`, `:2011`, `:2605`.

São três caminhos relacionados que exigem uma correção comum:

- O adaptador `MpvPlaybackEngine`, que também representa OwnTVPlayer quando entrega diferidos ao Exo, não substitui `playbackRequested`. O default é `isPlaying`, que pode ficar false durante BUFFERING apesar de o pedido continuar a ser reproduzir. A sessão abandona foco indevidamente e um comando externo Play pode alternar um Exo que já tem `playWhenReady=true` para Pause.
- `pauseForInterruption` e o comando externo Pause usam buffering como condição para executar toggle. Se o utilizador já pausou enquanto carrega, uma perda transitória de foco ou Pause repetido pode **retomar**. Duas interrupções seguidas podem inverter a primeira pausa.
- O Live mantém alarmes/retries após pausa durante buffering. A recuperação termina em `reprepare`, que impõe `playWhenReady=true`. Pode desfazer pausa manual, pausa por foco ou retirada de auscultadores; uma reconstrução também pode perder a posição de recuo.

**Solução:** intenção real, com identidade/revisão, no controlador comum; comandos explícitos e idempotentes Play/Pause em todos os motores; retoma após foco apenas se a sessão retirou uma intenção ativa e nenhuma decisão mais recente a substituiu. Separar preparação, apresentação e alarmes. Uma pausa cancela/suspende recuperação automática; uma preparação interna não pode criar um pedido novo de reproduzir. Play deve ter um caminho definido para retomar a preparação que ficou suspensa.

**Aceitação:** pause durante buffering, espera HTTP e retry atrasado permanece pausado; Play repetido não pausa; Pause repetido não reproduz; duas losses não reproduzem; handoff mpv/Exo conserva a decisão. Testar através dos adaptadores e callbacks reais, não só da função booleana de política de foco.

**Contraevidência:** pausa num canal saudável já bloqueia o watchdog de progresso por `playWhenReady=false`. A falha está nos outros caminhos e trabalhos já pendentes. Não inferir que toda perda de foco dispara o problema.

### B2 — recuperação de áudio Live pode renovar indefinidamente o próprio orçamento — P1

**Evidência:** `LivePreviewEngine.kt:918`, `:923`, `:1777`, `:1785`; contraponto em `ExoSubtitleEngine.kt:672`.

O Live reconstrói quando AudioWatchdog indica erro de saída, mesmo se já está na configuração de fallback ou se o utilizador escolheu Estéreo. A reconstrução reinicia o watchdog e chama `play`, criando nova sintonia e novos limites. Um erro PCM persistente pode repetir saída falhada → reconstrução → saída falhada. O latch estéreo ser idempotente não limita a reconstrução. O Exo de diferidos já tem a proteção `!builtForStereo` para esse resgate.

**Solução:** uma tentativa de resgate por sintonia/episódio de saída, conservada através de reconstruções internas. Após falha persistente, terminar com erro claro ou alternativa de motor apenas se autorizada pela configuração. Novo orçamento por ação explícita ou mudança efetiva de saída comprovada; inventário de dispositivos disponíveis não prova troca de rota.

**Aceitação:** duas saídas sucessivas com o mesmo erro não criam uma terceira geração nem renovam o limite. Nova sintonia explícita pode tentar novamente. Underrun por falta de dados não gasta o resgate de hardware.

### B3 — Estéreo não garante dois canais PCM — garantia incorreta, P1/P2

**Evidência:** `AudioOutputPolicy.kt:94`; `ExoRenderers.kt:23`; `LivePreviewEngine.kt:3290`; `ExoSubtitleEngine.kt:548`.

O fallback usa `AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES`, retirando passthrough. Não há processador de mistura para dois canais. Na implementação Media3 1.11.1, PCM16 é suportado e o output PCM utiliza a contagem/máscara do formato recebido. Uma única faixa 5.1 pode continuar como PCM multicanal. A designação e os comentários prometem mais do que a configuração garante. [Fonte Media3 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/AudioTrackAudioOutputProvider.java).

**Solução:** selecionar uma faixa estéreo quando existe; para garantir dois canais no modo Estéreo, usar mistura PCM explícita e testada quando necessária. Uma restrição do selector a dois canais, isolada, não garante conversão da única faixa 5.1. Não alterar o modo Surround nem aplicar mistura global.

**Aceitação:** observar configuração efetiva de saída com faixa única 5.1, estéreo+5.1, diálogo apenas no canal central, mudanças de faixa e velocidades 0,5×/1×/2×. Verificar diálogo, ganho, clipping, layout e sincronismo. O Android pode já misturar internamente; não declarar falha de som universal nem atribuir os cortes a este ponto sem medição.

### B4 — volume reduzido por interrupção pode ficar associado ao motor errado — P2

**Evidência:** `PlaybackSession.kt:98`, `:100`, `:382`, `:399`.

`attach` troca a referência do motor antes de retirar a atenuação. Ao desligar, já não encontra o motor anterior para restaurar o volume. Na troca direta A→B, o estado de volume anterior pode pertencer a A e ser aplicado a B. Resultado possível: motor reutilizado permanece a 25%, ou o novo motor recebe uma restauração indevida.

**Solução:** associar o estado de atenuação ao motor proprietário e tratar a restauração antes da troca. Preservar uma alteração manual mais recente de volume.

**Aceitação:** A100 → atenuação25 → detach → A100; trocar A→B não altera B; ajuste manual durante interrupção permanece. Condicional à ocorrência de ducking manual, não uma explicação automática para cada microcorte.

### B5 — foco recusado fica memorizado como pedido pendente — P2

**Evidência:** `PlaybackSession.kt:366`, `:370`, `:375`.

`focusRequested=true` é atribuído antes de avaliar o resultado. Se Android recusar, não há nova tentativa enquanto a sessão continuar; o pedido não aceita foco atrasado, portanto não há garantia de GAIN futuro. A política permissiva de TV acaba também por abranger o telefone.

**Solução:** estados concedido, recusado e atrasado separados. Atrasado só existe se explicitamente solicitado. Repetir de forma limitada numa retoma explícita ou transição de estado permitida, sem pedir a cada tick. Definir comportamento do smartphone perante recusa. Não ativar simultaneamente foco automático Exo e a gestão manual comum. [Contrato Android de foco](https://developer.android.com/media/optimize/audio-focus).

**Aceitação:** FAILED inicial não bloqueia uma tentativa posterior autorizada; GRANTED não repete pedidos; recusa não produz um ciclo a 1 Hz. Testar com lifecycle e restrições da versão Android.

### B6 — erro de formato consumido sem iniciar alternativa — P1/P2

**Evidência:** `LivePreviewEngine.kt:1454`, `:2370`, `:2374`, `:2376`.

O callback chama `retryAlternateFormat()` e retorna. O helper pode retornar sem agendar nada se a URL não tiver alternativa ou se o HLS alternativo já estiver excluído. O erro original não segue para a alternativa `direct_source` nem para a publicação terminal; o utilizador espera pelo prazo exterior.

**Solução:** o helper devolve se agendou efetivamente a tentativa; só então sair do callback. Caso contrário, continuar a decisão normal com o erro original.

**Aceitação:** URL sem extensão com erro e direct_source; TS sem HLS conhecido; alternativa válida; sem alternativa e sem direct_source. Contar tentativas e verificar estado terminal imediato quando apropriado.

**Aplicabilidade:** este ramo só é usado quando **Apenas HLS está desligado**. Não apresentá-lo como causa direta do sintoma numa utilização estritamente HLS. Os watchdogs atuais limitam a espera, mas não corrigem o controlo de fluxo.

### B7 — aprendizagem DASH de outro canal sobrepõe uma URL HLS explícita — P1/P2

**Evidência:** `LivePreviewEngine.kt:3397`, `:3402`, aprendizagem em `:2945`; `LiveStreamQuirks.kt:155`.

Após descobrir MPD numa URL sem extensão, a app guarda a lição para o host. `knownDashHost` precede `inferredHls`; uma URL posterior `.m3u8` no mesmo host sem manifest_type pode ser entregue ao parser DASH.

**Solução:** evidência explícita da fonte/URL antes de heurísticas aprendidas noutro canal; âmbito URL/canal ou padrão de endpoint validado; invalidar hipóteses perante evidência contrária. Tratar conflitos explícitos com diagnóstico.

**Aceitação:** mesmo host, DASH sem extensão → HLS .m3u8 → TS; declarações explícitas conservam prioridade; aprendizagem correta continua útil. Teste existente de prioridade entre hosts aprendidos não cobre este conflito com URL concreta.

**Aplicabilidade:** manifest_type HLS e modo Apenas HLS já protegem este caso. Impacto condicionado a fornecedores que misturam formatos e a uma configuração que permite essa mistura.

### B8 — User-Agent aprendido antes de comprovar recuperação — P2

**Evidência:** `LivePreviewEngine.kt:1433`, `:2413`; `LiveStreamQuirks.kt:396`; precedente correto em `OwnTVPlayer.kt:4363`.

Um 403/503 inicial pode ensinar que o host recusa o User-Agent, antes de a tentativa alternativa reproduzir. Mesmo falhando os dois pedidos, os canais seguintes ficam com a identidade alternativa durante a sessão.

**Solução:** manter a experiência local pendente e só aprender após reprodução confirmada da fonte atual; respeitar UA personalizado, âmbito e expiração. Sucesso após 503 pode ser recuperação espontânea, pelo que não comprova por si só bloqueio por identidade. Evitar aprendizagem permanente/generalizada a partir desse único sinal.

**Aceitação:** ambas as identidades falham → nenhuma lição; recuperação confirmada elegível → lição limitada; tentativa cancelada/antiga → nenhuma lição; UA configurado permanece.

### B9 — Retry-After só é considerado depois de retries internos, e pode ser encurtado — P2

**Evidência:** `LivePreviewEngine.kt:3095`, `:2202`, `:3527`.

A política de loader própria altera segmentos 403/404/410, mas deixa 429 nos retries padrão Media3. Podem existir novos pedidos antes de o erro chegar ao countdown da app. O parser ignora HTTP-date e limita valores numéricos a 60 s: uma espera anunciada de 120 s não deve autorizar retry aos 60 s. [Política Media3](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/upstream/DefaultLoadErrorHandlingPolicy.java), [Retry-After no RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after).

**Solução:** um proprietário da espera, no nível em que os pedidos são repetidos; coordenar loader e controlador. Interpretar segundos e HTTP-date, convertendo prazo para referência monotónica após leitura. Limitar a experiência da UI terminando a tentativa, se necessário, sem pedir antes da janela do servidor. Não duplicar esperas nem créditos de abertura em várias camadas.

**Aceitação:** servidor de teste observa timestamps de pedidos de manifesto/segmento/chave; segundos, data, ausência de header, espera longa e cancelamento A→B. Nenhum pedido automático antes do prazo aplicável. Uma espera não bloqueia inadvertidamente consumidores de outra conta/origem.

### B10 — cliente HTTP geral cancela tarde operações bloqueadas — P2

**Evidência:** `core/src/main/java/tv/own/owntv/core/network/HttpClient.kt:53`, `:59`, `:104`; solução já existente em `BlockingCallCancellation.kt:20`.

`invokeOnCompletion` não interrompe imediatamente execute/read bloqueado; o hook é ainda removido no finally antes da conclusão. Uma sincronização anulada pode continuar a ocupar thread/socket e a transferir até resposta ou timeout. [Semântica de conclusão do Job](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/invoke-on-completion.html).

**Solução:** reutilizar `cancelBlockingCallWithCoroutine`, cobrindo conexão e leitura completa. O StalkerClient já utiliza esta proteção; não regressar esse caminho. Cancelar só a chamada proprietária, sem desligar globalmente o cliente partilhado.

**Aceitação:** servidor bloqueado antes dos headers e depois dos headers; cancelar fecha a chamada prontamente; parsing suspenso e anulado; conclusão normal preserva reutilização de conexões. A relação desta concorrência com cortes de áudio é hipótese, não causalidade demonstrada.

## 4. Novos achados — áudio, vídeo e ciclo de vida

### C1 — alterações de tracks podem repor o ganho para 100% — P1

**Evidência:** Core player LivePreviewEngine.kt:1843–1852, :2613–2622, callback :1370 e updateTracks :2858.

O ajuste para 50% termina corretamente em volume efetivo 0,5. Mas applyMute atribui 1,0 quando não está muted, antes do retorno antecipado. Uma mudança de tracks chama essa função novamente: o áudio pode subir para 100% enquanto o HUD continua a indicar 50%. Aplica-se a Live Exo; seleção de áudio/subtítulos, Audio Mode e mudança de grupos HLS podem percorrer o caminho. Não depende de perda de foco.

**Solução:** uma função comum aplica ganho escolhido, mute e boost. Alterar seleção de track não altera a escolha do utilizador. Duck temporário deve ter proprietário/revisão e não substituir o volume base.

**Teste de aceitação:** AAC/PCM a 25/50/100/125%, troca de tracks, mute/unmute e duck; estado publicado e ganho real coerentes. Passthrough pode ignorar ganho, portanto não é a única fixture. Zero e 100% são contraexemplos em que o salto não aparece.

**Verificação adicional no mesmo lote:** LivePreviewEngine.kt:1681 usa coerceAtLeast(defaultVolume) numa reabertura do mesmo canal. Pode elevar uma redução manual para o default. As preferências lembradas são aplicadas assincronamente em :2629–2639; testar o resultado final com e sem preferência gravada. Não contar como defeito novo independente sem esse teste.

### C2 — nova instância Exo perde velocidade escolhida — P2

**Evidência:** Core player OwnTVPlayer.kt:1836–1913, :1929–1947, :3259–3261; ExoSubtitleEngine.kt:331 e :641.

O handoff transporta volume e atraso áudio, mas não reaplica a velocidade em startExo. Uma instância reconstruída começa a 1× e pode conservar 2× no estado mostrado. Abrange diferidos/arquivo e outros usos do leitor VOD, não é proposta de readicionar filmes/séries à interface. Player reutilizado pode conservar a velocidade: o caso forte é reconstrução/handoff.

**Solução:** velocidade pertence ao pedido de reprodução; transportar e reaplicar antes de iniciar uma nova instância, publicando também o valor efetivo. Separar escolha explícita em diferidos do pequeno ajuste automático de velocidade usado na latência Live.

**Aceitação:** arquivo 1,5×/2×, handoff mpv→Exo e inverso, resgate áudio e reconstrução decoder; valor efetivo e HUD iguais. Nova seleção explícita pode seguir a política configurada de reset.

### C3 — cancelamento do hold AFR pode deixar um diferido pausado — P2

**Evidência:** App player FrameRateController.kt:414–431 e :444–453; MpvVideoSurface.kt:106–108; MainActivity.kt:139–149; shell OwnTVShell.kt:1417–1419.

O hold de mudança não seamless pausa e retoma apenas na conclusão normal. Rotação/docking/AFR desligado pode cancelar antes da retoma enquanto o mesmo player continua vivo. A nova composição pode já encontrar o modo desejado e não criar novo hold. O resultado possível é pausa permanente.

**Alcance:** arquivo/VOD apresentado através de MpvVideoSurface, mpv ou Exo, AFR ativo e pausa AFR maior que zero. Live passa film=null e não utiliza este hold. Não explicar cortes Live com este achado.

**Solução:** pausa temporária com identidade da reprodução e revisão de intenção, dependente de B1. Transferir/concluir o hold quando o mesmo item continua; respeitar Play/Pause posterior. Fechar realmente o player cancela sem retomar. Não usar uma retoma incondicional em finally.

**Aceitação:** rotação, docking, AFR off, intervenção manual e saída real durante hold. Apenas a pausa criada pelo AFR pode ser removida pelo AFR.

### C4 — DisplayListener AFR não é removido no sucesso — P2

**Evidência:** App player FrameRateController.kt:457–468.

O callback resume a continuação; a remoção só existe em invokeOnCancellation. Conclusão normal não executa esse handler, conservando listeners inúteis. Timeout/cancelamento durante a espera já faz limpeza. O mesmo alcance condicionado de arquivo/hold aplica-se aqui.

**Solução:** remover exatamente uma vez em sucesso, falha, timeout e cancelamento, com cleanup estruturado e proteção perante corrida callback/cancel. Filtrar o display da Activity. Testar também mudança já concluída antes de observar e eventos de outro display.

**Aceitação:** N mudanças bem-sucedidas deixam zero listeners pendentes; timeout e cancelamento não removem duas vezes. Retenção de Activity/PSS deve ser medida; o registo residual está confirmado, a dimensão da fuga não foi medida. [Contrato Kotlin de suspensão cancelável](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html).

### C5 — fallback automático contorna a política de software UHD — P2, impacto condicionado

**Evidência:** Core player ExoRenderers.kt:43; ExoSubtitleEngine.kt:155–162 e :182–195; LivePreviewEngine.kt:389–410; contraponto mpv OwnTVPlayer.kt:4252.

A recusa de resgate explícito para software acima de 1080p não cobre o decoder realmente escolhido pelo fallback Media3. Os callbacks registam-no, mas não aplicam a política. Hardware Off também pode escolher software. Há inconsistência confirmada; saturação CPU, calor, OOM ou drops não foram demonstrados.

**Solução:** definir política por codec, formato real, resolução, capacidade/orçamento e escolha explícita. Observar inicialização e alteração de formato; só terminar ou procurar alternativa quando a política aplicável o exigir. Não desativar fallback útil globalmente nem obrigar a software para contornar qualquer erro de hardware.

**Aceitação:** hardware falha→software 2160p; software 1080p; equipamento capaz; dimensões desconhecidas; formato muda durante reprodução. Medir CPU/drops no equipamento antes de alterar defaults.

## 5. Novos achados — interface, EPG e arranque

### C6 — restauro repetido pode puxar o foco para trás — P2

**Evidência:** App live LiveScreen.kt:367–374 e :394–407; restoreToContextRow :325–338.

Depois de Renomear/Associar EPG, cinco restauros com intervalo 200 ms continuam mesmo após sucesso. Navegar imediatamente pode ser contrariado pelo job antigo durante cerca de um segundo; também pode deslocar a lista durante um gesto no telefone. A disposição/reabertura do diálogo já cancela os efeitos; não é fuga indefinida.

**Solução:** uma intenção de restauro cancelável por lista/ação. Terminar no primeiro sucesso; cancelar perante nova navegação/modal. Esperar composição/load state do alvo em vez de reaplicações fixas.

**Aceitação:** fechar diálogo, mover três linhas nos primeiros 200 ms, aguardar um segundo; a escolha permanece. Repetir com recriação do pager e alvo removido.

### C7 — âncora do menu deixa o canal sem requester normal — P2

**Evidência:** App live LiveScreen.kt:320, :336, :750–758, regresso da rail :552–565.

contextChannelId permanece após fechar o menu; a linha passa a anexar contextFocus em vez de selFocus. O regresso categorias→lista tenta selFocus/firstItemFocus, podendo saltar para a primeira linha ou falhar.

**Gatilho comprovável:** canal selecionado não é o primeiro → pressão longa → fechar menu → Esquerda para categorias → Direita. Fullscreen dispõe LiveScreen (OwnTVShell.kt:938), limpando a memória local; não usar retorno fullscreen como prova garantida deste defeito.

**Solução/aceitação:** separar âncora temporária do menu e identidade normal da seleção; resolver requester realmente anexado ao alvo. Não apagar a âncora antes de terminar o restauro. Testar primeira linha, linha distante e canal escondido com fallback explícito.

### C8 — CategoryRail consome restauro sem confirmar foco — P2

**Evidência:** App shell components/CategoryRail.kt:161–167 e :290–310.

O caminho de restauro faz um pedido e chama onRowFocused mesmo que falhe. LazyColumn só anexa requesters às linhas compostas; alvo distante/reordenado pode ficar sem foco e a intenção é apagada.

**Solução/aceitação:** resolver índice, scroll, aguardar composição e tentativas limitadas canceláveis; consumir só após sucesso ou fallback decidido. Reutilizar o padrão onEnter já correto. Testar índice fora do viewport, reordenação e filtro que remove alvo. [Foco programático Compose](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus).

### C9 — invalidação EPG não limpa linhas do fornecedor — P2

**Evidência:** Core live LiveEpgReader.kt:76–86, :186–207; App live LiveViewModel.kt:600–610, :829, :891; EpgViewModel.kt:277 possui outro reader.

clearCache/invalidate só limpam now/next. providerRows contém linhas já deslocadas pelo offset, mas a chave é apenas channelId. Alterar offset global/per-canal ou associação pode reutilizar horas/chave antigas durante cinco minutos. O caminho de invalidação existe; não é suficiente para ambas as caches.

**Solução:** invalidar as duas caches e impedir que pedido antigo volte a preenchê-las após a alteração. Preferir cache de dados brutos por identidade fonte/canal/revisão, derivando offset/associação para apresentação. Se continuar a guardar resultado derivado, incluir todas as entradas determinantes na chave. Invalidação precisa de alcance entre leitores; não apenas na instância Live.

**Aceitação:** aquecer providerRows, mudar +60→−60 min, associação e perfil/lista; nenhuma linha com hora/chave anterior; resposta antiga concorrente não reinstala o resultado. Diferido selecionado usa o instante correto, sem confundir horário visual com URL do arquivo.

### C10 — cache Now pode sobreviver ao fim do programa — P2

**Evidência:** Core live LiveEpgReader.kt:39, :90–134 e :316–322.

nowNext devolve resposta com TTL de cinco minutos sem revalidar o intervalo. cachedNowTitle faz o mesmo para o título Now; a alternativa providerRows já verifica start/stop. Consulta um minuto antes de terminar pode continuar a rotular o programa anterior depois do fim. Não há clock como entrada de nowNext; uma nova consulta ainda pode atingir a cache antiga.

**Solução:** limitar validade ao próximo limite relevante de programa, além do TTL. Recalcular Now/Next a partir de entradas válidas quando possível. Publicar atualização nos consumidores ativos na fronteira, tratando gaps corretamente. O cálculo pertence ao EPG, não a um novo polling global por segundo.

**Aceitação:** relógio controlado, consulta antes/depois de stopMs, intervalos curtos, gap sem Now, Next distinto, offset e relógio corrigido. Não aumentar tráfego por segundo para corrigir apresentação.

### C11 — barra temporal oculta provoca consulta de histórico ao navegar — P2

**Evidência:** App shell OwnTVShell.kt:312 coleta timelineProgrammes incondicionalmente; uso visual condicionado em :1518. App live LiveViewModel.kt:559–567 usa debounce 200 ms, :1018–1020 muda previewChannel ao focar; LiveScreen.kt:735–737 chama esse caminho. Core live LiveEpgReader.kt:358–382 consulta, filtra, ordena e desloca o histórico de GuideHistoryPolicy.windowStart até aproximadamente agora+1 h.

O consumidor da shell mantém a produção ativa mesmo quando a barra não está a ser usada. Focar canais com catchup por mais de 200 ms pode consultar dias de histórico na lista, ignorando o atraso liveGuideDelayMs de browseNowNext. **É trabalho local de base de dados, não um pedido HLS nem download EPG remoto neste método.** Cancelamento mapLatest já existe, mas não evita iniciar uma consulta que não tem consumidor útil.

**Solução pequena a antecipar:** ativar a subscrição apenas quando a barra temporal precisa de dados, com identidade do canal realmente em reprodução e contexto do diferido. Ao regressar à lista, cancelar produção imediatamente; não deixar grace period contornar a política de browsing.

**Otimização posterior:** consulta própria de janela temporal para ticks da barra, sem reutilizar o picker de todo o histórico. Preservar os dias disponíveis no EPG/diferidos; ao arrastar fora da janela, carregar incrementalmente. Não reduzir retenção EPG para tornar a barra leve.

**Aceitação:** lista com preview off, canais catchup e foco 250/500 ms; zero consultas de histórico atribuíveis à barra oculta. Barra aberta carrega janela correta, seek e programas antigos continuam acessíveis. Medir queries/linhas/tempo e fluidez antes/depois; não prometer um multiplicador de velocidade.

### C12 — probe da base de dados bloqueia arranque e perde erro tardio — P2

**Evidência:** App MainActivity.kt:177–193 e chamada :227.

O worker abre Room, mas onCreate faz join até 150 ms no main. Se a migração/abertura falhar depois, a função já devolveu null e o erro não conduz ao ecrã de recuperação. O timeout é tratado como saudável, apesar de a verificação continuar. A demora concreta depende da instalação; o caminho de espera e a perda de resultado tardio estão presentes.

**Solução:** inicialização/verificação assíncrona com resultado observável, uma execução controlada por processo/estado e UI de loading/recuperação. Separar deadline visual do splash da conclusão da base. Não remover diagnóstico, não apagar dados automaticamente, não criar migrações para resolver esta espera.

**Aceitação:** abertura rápida, lenta, migração falha depois dos 150 ms, rotação durante abertura; zero join no main e erro tardio apresentado. Repetir/cancelar não cria sondas paralelas indefinidas. Preservar confirmação explícita para reset de dados.

## 6. Novos achados — timeshift local, apenas quando ativado

O caminho existe em App live LiveViewModel.kt:175–239: para a reprodução anterior, cria LocalTimeshiftSession, aguarda preparação e entrega HLS local ao player. Estes pontos não explicam diretamente um corte no HLS remoto normal com timeshift local desligado. HLS pode conter segmentos TS; manter HLS não exige segmentos fMP4.

### C13 — ENDLIST final é recusado antes de guardar os últimos segmentos — P2

**Evidência:** Core timeshift LocalTimeshiftSession.kt:106 contradiz os ramos :132 e :138; App live LiveViewModel.kt:225–229 trata também ENDED como falha.

O guard exige ausência de ENDLIST antes de percorrer segmentos finais, tornando o fim normal inalcançável. Um evento finito pode falhar e perder os últimos segmentos.

**Solução/aceitação:** validar e guardar segmentos finais, finalizar playlist local e distinguir ENDED de FAILED na UI. Media retida permanece legível. Fixture live→ENDLIST com segmento novo deve conservar conteúdo, publicar fim local, parar pedidos e não emitir toast de falha. Inicial vazio continua a falhar quando não há media utilizável. [Semântica ENDLIST no RFC 8216](https://www.rfc-editor.org/rfc/rfc8216.html#section-4.3.3.4).

### C14 — erro transitório termina o produtor sem recuperação — P2

**Evidência:** Core timeshift LocalTimeshiftSession.kt:41, :69, :114–120, :140; precedente Core recording/HlsSegmentRetry.kt:11.

Uma exceção de manifesto/segmento termina o produtor; a sequência avança antes do download. Um retry superficial que só apanhe exceção e prossiga perderia o segmento.

**Solução:** recuperar por sequência/episódio com orçamento finito, refresh de referência assinada quando adequado e classificação de recusa. Avançar sequência após commit ou skip explícito com discontinuity. Um pedido upstream ativo, sem renovar limite interno indefinidamente. Depende de B9/B10 e da validação C16.

**Aceitação:** falha temporária→sucesso, referência renovada, falhas persistentes terminam, cancelamento durante espera, segmento expirado com gap explícito, sem duplicação. Media parcial continua proibida de publicação. Não mascarar 403 real ou limite de sessão.

### C15 — redirect assinado substitui origem estável de refresh — P2, condicionado

**Evidência:** Core timeshift LocalTimeshiftSession.kt:89, :94–98, :140. Comparação recording/RecordingEngine.kt:467.

O produtor passa a refrescar a URL final. Se o endpoint estável renovava o token, essa possibilidade perde-se. Master com variant assinada exige tratamento da identidade escolhida. A gravação tem risco semelhante, mas o seu loop exterior volta a resolveTarget após falha: não assumir comportamento idêntico.

**Solução/aceitação:** separar identidade/origem de refresh e base final de resolução relativa; recuperar a mesma variant por identidade quando necessário. Origem→A, A expira, origem→B: pedidos e segmentos relativos usam B, headers são conservados e cancelamento impede a sessão antiga. Não alternar resolução/qualidade silenciosamente.

### C16 — resposta 206 com intervalo errado pode entrar na store — P2

**Evidência:** Core timeshift LocalTimeshiftSession.kt:70 e :123; validação já existente em recording/RecordingEngine.kt:712–718.

Código 206 e Content-Length não provam correspondência ao BYTERANGE pedido. Outro intervalo TS válido pode passar a validação de estrutura e ser publicado com duração/posição erradas.

**Solução/aceitação:** reutilizar validação comum de Content-Range, offset e comprimento solicitado. 206 correto entra; offset errado com TS válido, tamanho errado e 200 que ignora Range não entram. Não ampliar retries antes de impedir publicação de bytes incorretos.

### C17 — deadline fixo de 45 s pode falhar com segmentos longos — P2, robustez condicionada

**Evidência:** Core timeshift LocalTimeshiftSession.kt:105.

TARGETDURATION de 60 s com atualização legítima mais tardia pode ultrapassar o limite fixo mesmo após READY. Não foi observado um fornecedor concreto com esta cadência; IPTV de segmentos curtos não prova este problema.

**Solução/aceitação:** deadline proporcional à cadência declarada/progresso, com mínimo/máximo finitos. Tempo virtual para cadências curtas e longas, atualização válida dentro da janela e fonte imóvel além dela. Não aumentar todos os timeouts nem renovar orçamento em pausa. [Cadência de publicação HLS](https://www.rfc-editor.org/rfc/rfc8216.html#section-6.2.1).

## 7. Otimizações a medir — não confundir com causas confirmadas

| ID | Evidência/oportunidade | Solução e medição propostas |
|---|---|---|
| O1 | LiveScreen.kt:179–199 renova títulos de todos os itens carregados; paginação unbounded LiveViewModel.kt:747–751; NowPlayingTitleLoader.kt:30–38 e :81–85 copia/publica mapas | Pedir títulos para viewport+overscan+selecionado. Medir queries/alocações com 240 e milhares de canais. Não reduzir maxSize isoladamente: snapshot/índices alimentam zapping |
| O2 | Readers Live/Epg separados; caches nowNext/providerRows distintas e check→fetch sem coalescing em LiveEpgReader | Partilhar dados brutos e pedidos equivalentes por fonte/conta/canal/revisão. Medir duplicação real; cancelamento de um consumidor não deve cancelar os restantes. Começar após C9/C10; não misturar offsets/identidades |
| O3 | LiveScreen.kt:194–199, EpgScreen.kt:223–228 e PlayerClock.kt:58 têm timers enquanto composição existe | Suspender em lifecycle adequado e refrescar na retoma. Contar execuções durante Home. São timers pouco frequentes; benefício provável pequeno |
| O4 | ThroughputTracker.kt:31 ignora isNetwork; ExoStreamStats.kt:7 apresenta transporte como bitrate | Separar Mbps de download, bitrate declarado e tamanho/duração de segmento. HLS tem rajadas; zero entre pedidos é normal. Filtrar transferências locais. Caminho HTTP atual normalmente isNetwork=true |
| O5 | Já existem diagnóstico limitado, UiPerformanceMonitor e eventos Media3 | Completar snapshots correlacionados de QoE, buffer, HTTP, saída e reconstrução. Sem registo por frame nem escrita síncrona na UI. Trace curta Perfetto no corte e na navegação |
| O6 | Queueing Exo configurável; callbacks JNI/Surface podem ser síncronos | Comparar Auto/Síncrono/Assíncrono em condições iguais. Medir duração JNI/Surface antes de mover operações; respeitar lifetime/thread do player, sem worker genérico para tudo |
| O7 | providerEntries em LiveEpgReader.kt:152–167 converte falha em lista vazia; caches podem reter resultado vazio | Distinguir sucesso vazio, indisponibilidade, erro e cancelamento; não guardar erro como ausência válida por cinco minutos. Política curta/limitada de retry, preservando último dado elegível. Testar CancellationException e falha→recuperação |
| O8 | Timeshift escolhe variant de maior bandwidth, HlsRecordingPlan.kt:13; reserveIncoming sempre 64 MiB em Session.kt:116 | Medir compatibilidade codec/resolução, estabilidade e retenção efetiva. Escolher variant dentro das capacidades e política do utilizador; reserva proporcional/incremental só com quota segura, temporários e ficheiros presos por leitores contabilizados |
| O9 | fsync por segmento; finalização/remux pode ler/copiar gravação inteira | Medir I/O, latência de commit, CPU e cortes simultâneos. Agendar/frear trabalho de fundo só onde trace comprovar competição. Não retirar durabilidade/limites de quota como otimização gratuita |

Compose favorece cálculos lembrados, chaves estáveis e leitura de estado no menor âmbito necessário; vários destes mecanismos já existem na app. Usar a documentação para orientar medições, não para repetir alterações já presentes. [Boas práticas Compose](https://developer.android.com/develop/ui/compose/performance/bestpractices), [analytics Media3](https://developer.android.com/media/media3/exoplayer/analytics), [system tracing Android](https://developer.android.com/topic/performance/tracing).

## 8. Hipóteses ainda fora dos lotes de correção automática

1. **READY com supressão:** LivePreviewEngine.kt:898 não consulta playbackSuppressionReason. Primeiro reproduzir uma supressão legítima nesse player de foco manual; depois suspender contagem de freeze/rearmar baseline. Apenas isPlaying pode esconder congelamento real.
2. **Preview Stalker:** LiveTuneController.kt:229 resolve create_link antes do gate :232. Falta comprovar caminho alcançável e se esse portal invalida a ligação anterior. Identidade de conta/fonte deve preceder pedidos com efeitos na sessão.
3. **Release áudio após reset:** verificar init A→reset→init B igual→release B, com release A filtrado. Sem correlação de callbacks demonstrada, não atribuir corte a esta contabilidade.
4. **AudioDelay extremo versus watchdog:** mudanças +5000→−5000 ms podem legitimamente segurar/dropar imagem ao mudar o relógio. Ensaiar interação; não classificar o efeito esperado como bug.
5. **AFR observa display errado/tarde:** C4 inclui fixtures de evento alheio e alteração concluída antes do registo. Não mudar refresh globalmente para esconder espera.

Uma hipótese só sobe a correção quando há sequência reproduzida, alcance comprovado e teste que falha antes/sucede depois. [Estados e intenção Media3](https://developer.android.com/media/media3/exoplayer/listening-to-player-events).

## 9. Fases aprovadas pelo council

Não fazer uma única alteração abrangente de todos os motores. Cada sublote tem diff, testes e possibilidade de comparação próprios; partilhar invariantes apenas onde necessário.

| Fase | Sublotes e IDs | Resultado esperado e critério de saída |
|---|---|---|
| 1 — Controlo e comando | 1A B1/B2: intenção e resgate; 1B B4/B5/C1/C2: foco áudio e parâmetros; 1C C6/C7/C8/C11: foco UI e desligar consulta oculta | Pause/Play idempotentes; recuperação finita; volume/velocidade preservados em recuperação; foco não volta atrás; barra oculta não consulta histórico. Telemetria mínima desde 1A |
| 2 — HLS e HTTP | 2A B6/B7/B8: erro/rota/aprendizagem; 2B B9/B10: backoff/cancelamento; 2C C13/C16: fim e integridade local pequenos | Apenas HLS respeitado, erros não engolidos, aprendizagem elegível, pedido antigo cancelado e nenhum retry antes da espera do servidor; bytes locais corretos |
| 3 — EPG, arranque e carga UI | 3A C9/C10/O7: caches corretas; 3B C11 janela/O1/O2: trabalho necessário; 3C C12/O3/O4: arranque, lifecycle, métricas | Horas/Now corretos, respostas antigas rejeitadas, trabalho proporcional ao viewport/janela e erro DB tardio observável. Ganho avaliado por queries, frame-time e tempo de abertura |
| 4 — AFR em diferidos | C3/C4; confirmar C2 em todas as reconstruções e handoffs | Pausa temporária com proprietário; rotação/docking não deixam pausa residual; zero listeners depois da operação; saída real não retoma |
| 5 — Timeshift local robusto | C14/C15/C17; completar integração C13/C16; avaliar O8/O9 | Recuperação finita, tokens renováveis, sequência/Range válidos, ENDLIST normal, quota e janela verdadeiras, sem pedidos de sessão cancelada |
| 6 — Saída áudio, decoder e desempenho medido | 6A B3: estéreo real; 6B C5: política decoder; 6C O5/O6 e medições restantes | Fixtures A/V e testes no equipamento; decidir defaults apenas com comparação. Downmix e software-UHD são entregas independentes |

**Dependências:** B1 fornece intenção/revisão a B2, foco e hold AFR. C2 acompanha os handoffs alterados na fase 1; fase 4 é validação complementar. C4 é cleanup pequeno que pode ser antecipado num diff isolado. C13/C16 precedem novos retries do produtor; C14/C15 dependem de B9/B10. C9/C10 precedem partilha de cache O2. C11 gating entra cedo; janela limitada fica na fase 3. Um teste que revele publicação de conteúdo errado ou sessão cancelada ainda ativa eleva a correção correspondente, sem obrigar a antecipar todo o timeshift.

### Primeira entrega concreta recomendada

Começar por **1A**: corrigir intenção Play/Pause nos adaptadores reais, preservar decisão através de buffering/reprepare e limitar o resgate de áudio sem renovar orçamento. Acrescentar testes de sequência e eventos mínimos. Depois **1B** e **1C** em alterações separadas; podem integrar a mesma versão após validação, mas não misturar tudo no primeiro diff.

Se a prioridade imediata for exclusivamente comando/lista, **1C pode ser implementado primeiro**, pois os erros UI/gating não dependem de B1. A ordem não obriga o utilizador a esperar pelo redesenho da sessão para obter melhoria na navegação.

## 10. Como ficam configurações e funcionalidades

A maioria são correções transparentes, sem novos menus. Conservar Apenas HLS, limite de troca automática para versões do mesmo canal, prioridades/exclusões, pré-buffer, reserva, latência, processamento Exo, modo simples, barra lateral, EPG/diferidos e gravações.

- **Atraso de consulta EPG na lista:** manter a opção atual; nenhum consumidor oculto deve contorná-la. A barra do player consulta apenas quando necessária.
- **Processamento Exo:** manter Automático como predefinição e os modos de comparação existentes. Mudar aplicação só na próxima abertura quando a construção do motor exige isso.
- **Estéreo:** passar a garantir saída de dois canais quando esta opção é selecionada, depois dos testes da fase 6A. Surround continua separado.
- **Decoder:** manter escolhas atuais; diagnóstico mostra o decoder efetivo e uma incompatibilidade real tem resultado explicativo. Não criar uma opção UHD software por hipótese nem reduzir resolução global.
- **Timeshift local:** permanece opcional, com quota/janela reais. Fim normal deixa os segmentos retidos disponíveis; falha transitória pode recuperar dentro do orçamento.
- **Diagnóstico:** distinguir taxa de download e bitrate; mostrar estado/erro atual, reserva e formato de saída sem apresentar inventário de dispositivos como rota física comprovada.

Qualquer nova preferência que se venha a justificar exige default seguro, leitura no snapshot, persistência e backup/restauro. Correções de ownership, foco, cache e cancelamento não precisam de nova preferência. Não readicionar favoritos, últimos canais, filmes/séries ou PiP Android, que não fazem parte deste pedido.

## 11. Validação e critérios de aceitação

### Testes automatizados por contrato

- Adaptadores reais Exo Live/OwnTVPlayer→Exo/mpv: Play/Pause repetidos, buffering, retry/backoff, duas perdas de foco, retirada de saída, pause manual e handoff. A função isolada de foco não cobre a sequência completa.
- Watchdog/rescue com gerações: falha persistente tem limite, rebuild não repõe créditos e callbacks antigos não alteram estado atual. Underrun sem media não gasta resgate de saída.
- Servidor HTTP controlado: bloqueio antes/depois dos headers; cancelamento; 429 com segundos/HTTP-date/espera longa; manifesto, chave e segmento; redirects e Range. Observar timestamps, identidade e contagem dos pedidos, além de estado final.
- EPG com tempo controlado: limite de programa, gap, offset, associação, mudança de fonte/perfil, pedido antigo e erro vazio. Queries de barra apenas quando consumidor ativo e dentro da janela.
- UI: D-pad e toque após Renomear/Associar; menu→categorias→lista; alvo fora do viewport/removido; Back; rotação e teclado smartphone.
- AFR: sucesso/timeout/cancel; display alheio; rotação/docking/AFR off; pause manual e saída definitiva.
- Timeshift: segmentos finais, retransmissão, sequência reset/gap, token renova, 206 incorreto, cadência longa, leitor pausado, ficheiros presos, quota/limpeza e cancelamento.
- Áudio: AAC/PCM/AC3/EAC3, faixa única 5.1 e stereo+5.1; centro isolado, clipping/ganho/layout, troca de faixa e velocidades 0,5/1/2×; verificar saída efetiva.

### Matriz no equipamento

Xiaomi TV Box S 3.ª geração, Thomson 240 e pelo menos um smartphone Android. Registar Android/build/saída áudio/rede reais; ARMv7/ARMv8 não determina se o dispositivo é TV.

1. **Zapping:** A→B→C→A e variantes do mesmo canal; intervalos <1 s, 2–5 s e cerca de 10 s. Incluir regressar à lista/abrir novamente. Nenhum callback/pedido antigo reabre uma escolha abandonada.
2. **Direto:** 30–60 min de canal saudável, mesma variant/saída/rede/configuração; contar rebuffer, duração de cortes, underruns, reconstruções e tempo de abertura. Medir antes/depois de cada lote.
3. **Lista:** 240 canais e lista grande; navegação repetida e parada; p50/p95/p99 de frames, main-thread, alocações, queries e latência tecla→alteração visível. A métrica keyQueue do monitor não equivale sozinha à resposta visual.
4. **Arquivo/EPG:** vários dias conforme disponibilidade real do fornecedor; seleção abre o intervalo pretendido, não salta silenciosamente para direto. Barra continua a permitir recuo.
5. **Carga:** direto sozinho e com sync/gravação/timeshift quando permitido; medir I/O/PSS/CPU. O target do allocator não é teto da RAM total. Não abrir sessões extra num fornecedor que não as permite.
6. **Compatibilidade:** queueing Auto vs Síncrono, Async apenas comparação; mesma configuração e uma variável por ensaio. Build representativa de distribuição para desempenho, sem concluir a partir de debug apenas.

Correção funcional exige teste de regressão do cenário. Benefício de desempenho exige baseline comparável. Eliminação dos cortes exige registos no instante e reprodução em equipamento; não pode ser garantida pela leitura do código.

## 12. Proteções existentes a conservar e propostas rejeitadas

Já existem identidade de sintonia/fonte, rejeição de callbacks antigos, invalidar factory antes de stop/clear, cancelamento de corpo HTTP, drenagem limitada sem espera quando não há pedidos, jobs de prepare canceláveis, sucesso revalidado antes de desistir, timeout HLS finito, separação pré-buffer/reserva/offset e diagnóstico assíncrono limitado. Fecho local não prova libertação instantânea da sessão do fornecedor.

UI já tem listas lazy com keys/contentType, logos lembrados 128×128 sem crossfade, gradientes rasterizados/cacheados, blur partilhado/downscaled fora do main e visual leve por capacidade. EPG já possui queries por janela/batches e trabalho pesado deslocado de thread onde observado. Base de dados já usa WAL; baselineprofile/profileinstaller já existem.

Rejeitar neste plano:

- Novos watchdogs sobrepostos, retries infinitos, sleeps fixos em todo o zapping ou reinicializar globalmente cada motor a cada canal.
- Aumentar/diminuir buffers, baixar qualidade ou forçar queueing/software/downmix para todos sem comparação.
- Tratar 403 real como falta de buffer, aprender User-Agent por 503 isolado ou alterar o UA configurado silenciosamente.
- Desligar globalmente o cliente HTTP partilhado para cancelar um pedido.
- Reduzir cache de paginação sem verificar snapshot/índices de zapping; remover UI já otimizada por suposição.
- Retirar guards de fMP4, encriptação e áudio separado do produtor local sem implementar a cadeia correspondente.
- Retirar fsync/contabilidade de temporários/quota sem medir e preservar a garantia pretendida.
- Prometer 2×–4×, zero cortes ou zero bugs antes de medir; declarar teste unitário como prova de compatibilidade na box.

## 13. Checklist para iniciar e concluir cada sublote

1. Confirmar Core efetivamente usado pelo Gradle, estado Git e ficheiros atuais; preservar alterações anteriores e não sobrescrever trabalho alheio.
2. Delimitar IDs e cenário que falha; não incluir funcionalidades de conveniência fora do lote.
3. Alterar o menor caminho responsável e criar regressão que exercita sequência/callback/HTTP relevante.
4. Executar compilação e testes apropriados; Lint com resultado explícito, sem supressão permanente para obter verde.
5. Documentar resultado, limites e teste manual necessário. Se não há teste no equipamento, dizer isso.
6. Comparar no dispositivo antes de alterar defaults ou continuar com otimização dependente de medição.

**Estado final deste documento:** plano preparado para implementação faseada; nenhum destes lotes pendentes foi aplicado nesta auditoria. Apenas documentação criada, sem APK, alteração de versão, commit ou publicação.
