# OwnTV — revisão especializada e decisão do council

Data: 2026-10-02. Estado: análise; implementação não iniciada nesta ronda.

## Alcance e evidência

Três agentes especializados analisaram reprodução/áudio/HLS, interface TV/smartphone e rede/armazenamento/gravações. O agente principal verificou os caminhos críticos e consolidou as recomendações. Foram consultados os relatórios de implementação atuais para excluir correções já realizadas. Esta é uma revisão técnica conjunta, não uma certificação externa.

Repositórios efetivamente inspecionados:

- App: `C:/Users/renat/Downloads/OwnTV-main`.
- Core: `C:/Users/renat/Downloads/OwnTV_Core`.

CONFIRMADO_CODIGO significa que a condição e o caminho estão presentes no código; não significa que o comportamento foi reproduzido na Xiaomi, Thomson ou smartphone. CONDICIONAL exige ensaio para confirmar o cenário/alcance. OTIMIZACAO identifica trabalho evitável sem prometer um ganho medido.

Não foram executados novos testes, builds ou reprodução nesta ronda. `adb devices` não apresentou dispositivos ligados. Os resultados das rondas anteriores não constituem validação dos futuros patches. Apenas este documento foi criado; código e alterações anteriores foram preservados.

## Decisão e sequência

| Lote | Conteúdo | Razão e saída exigida |
|---|---|---|
| 1 | R1 intenção de pausa; R2 observação de rede; R6 arranque com sidebar oculta; R10 confirmação real nos testes | Corrigir contratos funcionais e tornar os ensaios capazes de detetar um player sem imagem. Testes determinísticos e instrumentais quando houver equipamento. |
| 2 | R3 alarme de gravação; R4 saída da fila; R5 reserva física concorrente | Fiabilidade do agendamento e integridade de armazenamento, com testes de interleaving e dois escritores. |
| 3 | R7 foco/scroll; R8 EPG fora do ecrã; R9 teclado/redimensionamento | Melhorar comando/toque e reduzir trabalho desnecessário. R9 exige reproduzir o cenário Android antes de fechar o patch. |
| 4 | R11 drenagem Exo→mpv | Completar a transição entre motores, sem atribuir esta lacuna ao zapping exclusivamente Exo. |
| 5 | Medição comparativa no equipamento | Áudio, frame-time, primeiro frame, memória e disco. Só então decidir mudanças de processamento/buffer. |

Não adicionar novas opções para bugs de implementação. Conservar volume interno a 100%, Apenas HLS, setas de canais, modos simples, EPG, diferidos, recuo e gravações. PiP e novas funções de favoritos/recentes permanecem fora do âmbito.

## R1 — Pausa não suspende os orçamentos exteriores de abertura/recuperação

**P1, CONFIRMADO_CODIGO.** Core `player-core/src/main/java/tv/own/owntv/player/LiveTuneController.kt:548`, `LiveExoWatchdog.kt:80` e `:146`, `LiveEngines.kt:156`. `LivePreviewEngine.kt:2743` suspende corretamente os mecanismos internos ao pausar, mas o controlador/watchers exteriores continuam a contar tempo corrido. `stillOurs` em `LiveTuneController.kt:491` só verifica geração/canal/motor.

Gatilho: Pause antes do primeiro frame, durante recuperação ou uma interrupção de foco durante abertura. O engine deixa de produzir deliberadamente; o exterior pode declarar falha, consumir uma alternativa ou abandonar a sintonia. Não é evidência de causa dos cortes breves durante reprodução estável.

Solução: expor intenção/revisão no contrato dos motores; suspender o consumo dos orçamentos de abertura/recuperação durante pausa; verificar intenção e geração imediatamente antes de avançar/abandonar. Retomar o acompanhamento após Play. Conservar o deadline absoluto de Retry-After e os limites acumulados: pausa não permite antecipar pedidos nem renovar infinitamente tentativas. Não terminar definitivamente o watcher só porque houve pausa.

Aceitação: pausa maior que todos os timeouts não gera erro/handoff/pedido novo; Play posterior mantém acompanhamento; Pause→novo canal invalida o anterior; foco transitório e Retry-After preservam intenção e prazo.

## R2 — Uma exceção pode terminar a observação partilhada de rede

**P1, CONFIRMADO_CODIGO; ocorrência de falha OEM por confirmar.** Core `core/src/main/java/tv/own/owntv/core/network/ConnectivityObserver.kt:72`, `:76`, `:81`, `:87`: registo/consultas/poll sem tratamento antes de shareIn.

Uma exceção no upstream termina a coroutine de partilha; SupervisorJob não a reinicia. Os consumidores podem ficar sem novas notificações ou pode existir uma exceção não tratada no scope. Essa semântica está documentada em [Kotlin shareIn](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/share-in.html).

Solução: proteger operações individuais de plataforma e permitir recuperação controlada antes da partilha; identificar observação indeterminada sem fingir conectividade; conservar o último estado conhecido quando apropriado, revisão/identidade, cancelamento e apenas um callback/poll. Não usar retry sem limite para falhas permanentes de permissão.

Aceitação: registo falha uma vez e recupera; poll falha e uma mudança posterior chega a dois consumidores; cancelamento não é engolido; callback antigo continua rejeitado; nenhum reinício de canal saudável só por handover de rede.

## R3 — Alarme exato falha sem fallback imediato

**P2, CONFIRMADO_CODIGO.** Core `core/src/main/java/tv/own/owntv/core/recording/RecordingScheduler.kt:89`: após canBeExact=true, uma falha de setExactAndAllowWhileIdle apenas é registada. Um rearmAll futuro pode ocorrer tarde.

Solução: tratar SecurityException no ramo exato e tentar imediatamente o alarme inexato; se ambos falharem, expor resultado verificável de agendamento não armado. Não converter qualquer exceção em erro de permissão nem prometer pontualidade com alarme inexato. O Android documenta a permissão e as limitações dos alarmes em [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms).

Aceitação: check=true→SecurityException chama inexato uma vez; falha dupla é observável; sem apagar gravação; testar também revogação real, suspensão e reinício no equipamento.

## R4 — Item novo pode perder o wakeup na conclusão do worker

**P2, CONFIRMADO_CODIGO quanto à janela; interleaving ainda não reproduzida.** Core `recording/RecordingEngine.kt:157` verifica queueDirty antes de sair; `RecordingWorker.kt:73` usa trabalho único KEEP. Há um intervalo entre a última verificação e a conclusão persistida no WorkManager. Um kick nesse intervalo pode ser ignorado porque o worker anterior ainda está incompleto. O download legado tem o mesmo padrão em `download/DownloadEngine.kt:85` e `DownloadWorker.kt:66`.

Solução: protocolo durável de wakeup/conclusão ou sucessor deduplicado, mantendo um único escritor. A confirmação de fila vazia dentro do engine não cobre, sozinha, a transição do WorkManager. [ExistingWorkPolicy](https://developer.android.com/reference/androidx/work/ExistingWorkPolicy) confirma que KEEP ignora um novo pedido perante trabalho incompleto.

Aceitação: bloquear após última verificação, inserir item e emitir kick, concluir o worker; item inicia sem reabrir a app. Verificar múltiplos kicks, cancelamento e ausência de escrita duplicada. Não substituir por REPLACE, que interromperia trabalho ativo, nem criar uma cadeia ilimitada de workers vazios.

## R5 — Reserva física não contabiliza globalmente todos os escritores

**P2, CONFIRMADO_CODIGO.** Core `core/src/main/java/tv/own/owntv/core/storage/MediaStorageBudget.kt:98`: snapshot de espaço livre por Lease durante 500 ms; `:109` desconta bytes próprios e scratch alheio, mas não todos os novos bytes de media dos outros leases. A quota lógica já está protegida; piso físico e quota são contratos distintos.

Solução: fotografia/reconciliação e reservas físicas partilhadas por volume sob o mutex atual; contabilizar reservas pendentes/deltas escritos sem contar duas vezes bytes que StatFs já incorpora. Uma app não controla todas as escritas de outras apps: conservar margem e tratar erro de escrita final.

Aceitação: dois escritores e margem menor que a soma das próximas escritas; segundo pedido recusado; fechar/cancelar liberta reservas; snapshots novos conciliam bytes; quota lógica independente; USB removido preserva estado recuperável. Não é prova de disco cheio observado ou de cortes áudio.

## R6 — Barra lateral oculta bloqueia a ação configurada de arranque

**P2, CONFIRMADO_CODIGO.** App `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt:459` retorna antes de ler startupMode se simple.hideSidebar=true. Afeta também SPECIFIC_CHANNEL; apresentação e reprodução ficaram acopladas.

Solução: retirar o veto de apresentação da rotina de arranque; limitar apenas pedidos de foco à barra efetivamente visível. Proteger seleção atual/perfil ao aplicar qualquer ação atrasada. Respeitar o arranque escolhido pelo utilizador, sem introduzir novos modos.

Aceitação: barra oculta/visível com canal específico; canal presente/ausente; interação/perfil alterados durante espera não abrem um alvo obsoleto; zapping conserva a lista completa.

## R7 — Restauro e diálogos ainda podem disputar foco com o utilizador

**P2, CONFIRMADO_CODIGO.** App `ui/components/FocusTrap.kt:68` e `:99`: impõem scroll/foco durante até dez frames sem revisão de input. App `features/live/LiveScreen.kt:1705`: marca didInitialFocus antes de pedido único após 60 ms, fora de OwnTVPopup. `features/epg/EpgScreen.kt:792` e `:882` têm pedidos únicos semelhantes.

Solução: reutilizar FocusRequestGuard/requestBoundedFocus dentro da janela do popup. Validar revisão antes de cada scroll/request no restauro; cessar ao comando/toque/arrasto posterior. Preservar alvo/posição quando não há intervenção e conservar cancelamento.

Aceitação: fechar→D-pad imediato; fechar→arrastar; popup lento; alvo removido; resultados vazios; escrever antes da resposta; Back antes da resposta. Não retirar foco TV para melhorar só toque.

## R8 — EPG ainda reconstrói dados quando não está visível

**P2, OTIMIZACAO confirmada em estrutura; benefício não medido.** App `OwnTVShell.kt:278` cria EpgViewModel sempre; `EpgViewModel.kt:465–509` reage a fontes, customização, ordem e atualizações de guia; `:884` carrega sem gate de visibilidade. Cancelamento/mutex atuais já evitam a antiga concorrência: não repetir esse diagnóstico.

Solução: invalidar barato e marcar dirty fora do guia; reconstruir uma vez quando estiver ativo. Preservar VM/cache após primeira visita e os leitores necessários ao HUD/recuo; não suspender sincronização, gravações ou o EPG do canal em reprodução. Avaliar Home como candidato secundário, não remover módulos às cegas.

Aceitação: sincronização enquanto canal toca produz zero reconstruções do grid oculto; entrada no guia atualiza uma vez; perfil/offset e histórico continuam corretos. Medir CPU, alocações e frame-time antes/depois. Não afirmar eliminação de cortes áudio.

## R9 — Baseline do teclado pode ficar obsoleto após resize

**P2, CONDICIONAL no equipamento.** App `ui/components/TvImeMetrics.kt:95–117`: baselineVisibleBottom conserva maxOf; displayHeight mistura display e janela. Uma redução de janela sem recriação pode ser interpretada como teclado mesmo sem inset IME visível.

Solução após reprodução: associar baseline à geometria/identidade da janela e coordenadas locais; renovar bounds quando mudou a janela sem IME. Manter estimativa OEM de TV quando necessária; smartphone deve privilegiar os sinais reais do teclado.

Aceitação instrumental: popup aberto, fullscreen→split-screen→fullscreen; teclado aberto/fechado; rotação; fonte grande; recorte. Não considerar o cenário comprovado apenas pela aritmética.

## R10 — Benchmark aceita isPlaying antes da confirmação de imagem

**P2, CONFIRMADO_CODIGO; lacuna do teste.** App `OwnTVShell.kt:660` observa dockedEngine.isPlaying; `:1422` usa isso no tag owntv_player_ready. Core `LivePreviewEngine.kt:1416` publica isPlaying antes de recoverCurrentPlayback verificar confirmação. `baselineprofile/.../IptvJourney.kt` aceita esse tag para seguir o ensaio.

Solução: estado de readiness próprio associado a canal/geração/surface e confirmação real do renderer; distinguir áudio-only. Exigir conteúdo EPG efetivamente carregado e exercitar entrada por OK em TV, além dos clicks de automação. Não poluir UI com estes identificadores técnicos.

O contrato isPlaying usa READY, intenção de reprodução e ausência de supressão, não fornece uma asserção de primeiro frame para o ensaio: [Media3 player events](https://developer.android.com/media/media3/exoplayer/listening-to-player-events). A política de confirmação do engine já exige renderer/metadata recente; isso não deve ser apresentado novamente como bug do engine.

O fixture atual é HLS VOD sintético; útil para UI/repetibilidade, insuficiente para limites de sessões, playlist live deslizante e diferidos do fornecedor. Medir primeiro frame separadamente das métricas de UI: [Macrobenchmark metrics](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics).

Aceitação: READY/isPlaying sem primeiro frame não satisfaz o teste; callback de geração antiga não satisfaz; áudio-only tem critério próprio; medir cold start, lista com 240 canais, EPG, A→B→C→A e Home/retoma em TV e smartphone.

## R11 — Handoff Exo→mpv não espera confirmação de drenagem HTTP

**P2, CONFIRMADO_CODIGO; impacto condicionado a Automático/transição entre motores.** Core `LiveTuneController.kt:432` para Exo e `:470` espera apenas margem temporal de decoder. `LiveEngines.kt:127` delega stop sem await de drenagem; `LivePreviewEngine.kt:2023` cancela e retorna, com expulsão de pool assíncrona em `:2072`. SourceDrain.await já existe no reprepare Exo em `:1137`.

Solução: exoStopAndAwaitDrain suspenso com deadline/cancelamento/identidade, reutilizando o tracker; aplicar depois apenas a margem de decoder ainda necessária. Timeout não inicia outra fonte concorrente. Não atrasar todos os zaps com uma espera fixa.

Aceitação: socket/segmento antigo com fecho lento, timeout, A→B durante espera, cancelamento e ausência de penalização quando drenagem já terminou. Não atribuir isto ao problema exclusivamente Exo: o caminho Exo→Exo já tem drenagem. Fecho local não certifica quando o fornecedor liberta a sessão remota.

## Hipóteses e itens condicionados

- PlaybackSession.kt:229 atribui resumeTicket depois de pause. Possível reentrância com Main.immediate/combine: testar ordem real antes de declarar defeito; se reproduzido, instalar marca antes da emissão e conciliar revisão.
- DownloadEngine.kt:183 aceita 416 como completo para qualquer ficheiro não vazio; :189 aceita append 206 sem validar início. Defeito do download legado de filmes/episódios, não do atual diferido RecordingEngine. Manter em apêndice enquanto essa função está fora da app de canais. Se mantida, validar Content-Range e tamanho/identidade antes de completar/anexar, segundo [RFC 9110 §14.4](https://www.rfc-editor.org/rfc/rfc9110.html#section-14.4).
- Recuperação automática após falha da primeira abertura: tune.hasPlayed limita deliberadamente onNetworkRestored a sessões previamente abertas. Ampliar exige distinguir rede transitória de 403/429; não classificar já como bug.

## Áudio/vídeo: decisão do council sobre otimizações

Os achados acima não confirmam a causa dos cortes físicos de 0,2 s. Recolher por sintonia, numa mesma linha temporal: audio_underrun, avanço do áudio, buffer, HTTP/segmentos, timestamps/discontinuities, troca de faixa, foco/rota HDMI-Bluetooth e frames perdidos. Correlacionar com Perfetto para main thread, scheduling/GC e disco durante gravação. Dados do fornecedor continuam separados de falhas locais.

Comparar canal saudável durante 30–60 minutos, com mesmos formato/variante, decoder, rede, latência e buffer; depois repetir com lista/EPG e gravação ativos. Smartphone acrescenta rotação, teclado, chamadas/auscultadores e mudança de rede. Box acrescenta Home/suspensão, HDMI/AFR e D-pad rápido. O Android documenta o contrato de interrupções em [Audio focus](https://developer.android.com/media/optimize/audio-focus).

Rejeitados neste momento: mais threads genéricas, mover JNI/Surface indiscriminadamente, recriar decoder a cada canal, alterar buffer/passthrough automaticamente, desativar fsync, reduzir qualidade por modelo sem capacidades ou medição, e prometer 2×–4×. As melhorias anteriores de lazy VOD, identidade Surface, estado de rede partilhado, HUD tátil e confirmação do renderer já existem; não voltam à lista de trabalho como se estivessem por fazer.

Critério global: ausência de erros/handoffs durante pausa, nenhuma sintonia antiga interfere na atual, gravações acordam e respeitam espaço, foco cede ao input, EPG oculto evita reconstruções e ensaios só aprovam reprodução realmente confirmada. Ganhos de fluidez e cortes áudio só ficam aprovados com medição física comparativa.
