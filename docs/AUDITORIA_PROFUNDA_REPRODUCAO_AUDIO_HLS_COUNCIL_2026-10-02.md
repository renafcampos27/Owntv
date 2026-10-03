# OwnTV — auditoria profunda de reprodução, áudio e HLS

Data: 2026-10-02. Estado: **análise concluída; propostas ainda não implementadas**.

## Âmbito e evidência

Revisão do código atual, incluindo alterações e ficheiros ainda não versionados, em `C:/Users/renat/Downloads/OwnTV-main` e `C:/Users/renat/Downloads/OwnTV_Core`. A análise considera o lote A1–A9 de 2026-10-01 e procura falhas adicionais. Media3 declarado: **1.11.1**; app: minSdk 26, targetSdk 36.

Três revisores especializados analisaram rede/HLS, áudio/foco e ciclo de vida/controlo. Houve uma segunda ronda de council para questionar prioridades, riscos e contraevidência. O coordenador confirmou os caminhos principais no código e consultou fontes primárias. Não se alterou código de produção, não se gerou APK e não se executou reprodução em equipamento nesta auditoria.

**Confirmado no código** significa que a condição e o caminho incorreto existem, não que o sintoma tenha sido reproduzido na Xiaomi. Os cortes de cerca de 0,2 s continuam sem causa concreta demonstrada. Não há prova de crash, de falta de RAM ou de perda de pacotes causada pela app nesse instante.

As referências abaixo são relativas à raiz **OwnTV_Core**, salvo indicação. As linhas correspondem ao checkout observado e podem mudar após implementação.

## Decisão do council

Corrigir primeiro a intenção de reprodução e a recuperação limitada. Depois corrigir falhas de seleção/controlo HLS e coordenação de pedidos HTTP. A mistura real para estéreo deve ter uma entrega e testes próprios. A instrumentação mínima acompanha todos os lotes; a otimização do processamento depende de medições comparáveis.

Não criar outro watchdog global, não reiniciar todos os players a cada canal, não aumentar buffers genericamente, não impor processamento assíncrono ou downmix a todos os utilizadores. Manter a opção Apenas HLS e as proteções atuais de identidade/cancelamento.

## Achados confirmados

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

## Otimizações que fazem sentido medir

1. **Eliminar trabalho obsoleto:** B1/B2/B6/B10 evitam reconstruções, esperas ou transferências sem utilidade. É uma otimização funcional concreta; o ganho de CPU/RAM e tempo ainda não foi medido.
2. **Estatísticas corretas:** `ThroughputTracker.kt:31` não filtra isNetwork e `ExoStreamStats.kt:7` apresenta taxa de transferência como bitrate. Downloads HLS são por rajadas: 0 Mbps entre pedidos não prova falha nem equivale ao bitrate do vídeo. Separar taxa de transporte, bitrate declarado e tamanho/duração dos segmentos; só publicar snapshots partilhados se houver vários consumidores. P3, não falha da reprodução. O caminho HTTP atual normalmente já fornece isNetwork=true, pelo que o filtro é também robustez do componente, não prova de tráfego local contado nesta utilização.
3. **Medição de qualidade de reprodução:** duração de abertura, rebuffer, reconstruções, erros, pedidos ativos e tempos de playlist/segmentos. Aproveitar eventos Media3 existentes, amostragem moderada e escrita assíncrona limitada; não criar registo por frame. [Analytics Media3](https://developer.android.com/media/media3/exoplayer/analytics).
4. **CPU/RAM no momento do corte:** trace curta no equipamento para cruzar threads, agendamento, GC e atividade da UI/decoder. Perfetto e profiler Android são ferramentas de medição adequadas; não há trace desta box nesta auditoria. [System tracing Android](https://developer.android.com/topic/performance/tracing).
5. **Queueing Automático/Síncrono:** comparar mesmo canal, resolução, saída, reserva e duração; Assíncrono permanece ensaio. Não mudar outra predefinição sem resultado. O projeto já tem módulo baselineprofile e profileinstaller; propor adicioná-los do zero duplicaria infraestrutura existente.

## Suspeitas que não devem ser promovidas a causa confirmada

- **READY suprimido:** `LivePreviewEngine.kt:898` não consulta playbackSuppressionReason ao medir congelamento. Validar motivo real e depois não acumular tempo de freeze durante supressão legítima; rearmar baseline quando termina. Não usar apenas isPlaying, pois isso pode esconder freezes reais. O Live atual gere foco manualmente, portanto uma chamada não implica automaticamente este estado. [Contrato Media3 de estado e intenção](https://developer.android.com/media/media3/exoplayer/listening-to-player-events).
- **Preview Stalker:** `LiveTuneController.kt:229` resolve create_link antes de verificar bloqueio de sessão em :232. Num portal em que minting invalida a ligação anterior, pode afetá-la antes do gate. Falta confirmar alcance na app e comportamento do portal. A identidade conta/source deve ser conhecida antes de qualquer pedido com efeitos na sessão.
- **Release de áudio após reset:** AudioWatchdog conserva registos antigos mas limpa identidade atual. Testar init A → reset → init B igual → release B com release A filtrado. Sem correlação de callbacks demonstrada, é suspeita de contabilidade, não defeito audível confirmado.

## Plano por lotes aprovado

| Lote | Conteúdo | Critério para avançar |
|---|---|---|
| 1 | B1: intenção/Play/Pause; B4/B5: owner de volume e estados de foco; instrumentação mínima | Sequências de interrupção, buffering, pausa e handoff preservam decisões; comandos repetidos idempotentes |
| 2 | B2: resgate limitado e orçamento conservado entre reconstruções | Falha persistente termina sem reconstrução infinita; nova escolha explícita pode tentar |
| 3 | B6/B7/B8: fluxo de erro, rota e aprendizagem | Apenas HLS permanece respeitado; fornecedor misto e alternativas sem URL terminam corretamente |
| 4 | B9/B10: espera do servidor e cancelamento real HTTP | Contagem/timestamps de pedidos comprovam cancelamento e ausência de retry antecipado |
| 5 | B3: garantia estéreo real; telemetria P3 e ensaios de desempenho | Fixtures de canais/ganho/sincronismo e testes no equipamento; sem mistura global nem qualidade degradada |

B2 pode entrar no mesmo release de B1, mas em alteração/testes separados. A telemetria necessária para validar cada lote entra nesse próprio lote; não esperar pelo lote 5 para começar a medir.

## Matriz de validação necessária

- Exo Live HLS, Exo diferidos e mpv: pausa durante abertura, buffering, retry, backoff e handoff; Play/Pause via comando, UI e MediaSession.
- A→B→C→A com intervalos curtos e longos: identidade, cancelamento e número de pedidos; nenhuma operação antiga reabre o canal abandonado.
- 429 com segundos/data e 403 real: respeitar janela, não mascarar bloqueio com buffer maior e preservar erro explicativo.
- Faixas AAC estéreo, AC3/EAC3 multicanal, PCM e mudanças de saída: resgate finito e formato efetivo confirmado.
- Xiaomi/Thomson e smartphone: mesmo canal/saída durante 30–60 min, antes/depois; cortes/rebuffers por hora, tempo de abertura e reconstruções. Não exigir tráfego idêntico entre apps sem confirmar URL/variante.
- Comparação de performance em build representativa, sem trocar simultaneamente reserva, latência, queueing e resolução. Sem hardware e trace, não prometer 2×–4× ou eliminação de todos os cortes.

## Proteções existentes a conservar

Identidade de sintonia/fonte; rejeição de callbacks antigos; invalidar factory antes de stop/clear; cancelamento de corpo HTTP; espera de drenagem limitada sem atraso quando não há pedidos; prepare jobs canceláveis; separação de reserva, pré-buffer e offset; sucesso real revalidado antes de desistência; timeout finito de alinhamento HLS; registo assíncrono limitado; UA configurado pelo utilizador; configuração Apenas HLS.

Fechar o pedido local não prova libertação instantânea da sessão no fornecedor. Manter o timeout/diagnóstico existente e testar pedidos efetivos; não adicionar sleeps fixos a todo o zapping.

Esta auditoria não repetiu os 1 533 testes do lote anterior. Esses testes verificaram a implementação anterior, mas não demonstram que os novos cenários descritos aqui estejam cobertos. A próxima implementação deve acrescentar testes de sequência e HTTP observado, mantendo a distinção entre validação automatizada e teste no equipamento.
