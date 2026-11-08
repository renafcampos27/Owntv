# Plano de implementação — HLS, áudio, cache e codecs

Data: 3 de novembro de 2026. Projeto: OwnTV Android TV personalizado.

**Estado em 08/11/2026: fases 1A/1C/1D, 2A/2B e 3A/3B implementadas e validadas localmente, incluindo integração Release da App. O lint integral do Core mantém cinco erros anteriores documentados na secção 18. A 1B condicionada e as fases seguintes permanecem pendentes. Ver secções 17–21 e relatórios de execução; aceitação física pendente.**

**DECISÃO VIGENTE — revisão de compatibilidade e fluidez em 07/11/2026:** consultar primeiro a secção 16, incluindo a regra de aceitação da secção 16.7. Abrange 30 propostas e reduz/rebaixa várias aprovações anteriores. Em caso de conflito, a secção 16 prevalece sobre os textos iniciais e a antiga ordem das fases. Uma hipótese, um ensaio ou um protótipo não equivale a autorização para alterar os defaults ou a recuperação automática.

**Guia de execução exato:** a secção 13 identifica os ficheiros reais, os pontos de entrada e as operações mínimas. Para localização e âmbito das alterações, essa secção prevalece sobre os resumos da secção 5. «Adicionar» descreve trabalho futuro, não código já existente. Propostas condicionadas continuam sem autorização técnica para ativação automática.

**Adenda de 05/11/2026:** a secção 14 integra N18–N24 e R01–R03 com operações por ficheiro. Não altera a contagem original de P01–P17, não declara fases executadas e não transforma ensaios de áudio em correções de reprodução já aprovadas.

**Segunda revisão de 05/11/2026:** a secção 15 acrescenta N25–N27, distinguindo recuperação de rede de captura de gravações HLS. As propostas de diagnóstico/ensaios continuam separadas de alterações automáticas.

## 1. Decisão do council

Foram reanalisadas todas as propostas do anexo e das duas auditorias desta data. Participaram três agentes especializados: rede/HLS, áudio/codecs e cache/timeshift. O agente principal cruzou os pareceres, as fontes e o código e consolidou as decisões.

A revisão inicial de 03/11 reuniu **17 unidades de trabalho: 12 aprovadas, 2 parciais e 3 condicionadas**. Essa classificação inicial foi revista. A decisão vigente da secção 16 abrange **30 famílias: 4 manter, 10 reduzir, 10 testar primeiro e 6 adiar**. Duplicados foram fundidos; estes números não são uma contagem de bugs independentes nem de alterações já feitas.

- **Manter:** correção pontual proposta, com âmbito e testes normais preservados.
- **Reduzir:** executar futuramente só a parte mínima descrita; expansões não estão incluídas.
- **Testar primeiro:** provar desenho/benefício e ausência de regressão antes de integrar em produção.
- **Adiar:** fora da execução corrente, até existir necessidade e evidência suficientes.

Não foi eliminada uma família inteira apenas por ser falsa; foram recusadas formas de implementação concretas e retiradas aprovações demasiado amplas. O detalhe das rejeições e dependências está na secção 16.

A revisão não garante risco zero. Os testes de coexistência e os critérios de avanço são obrigatórios. A causa dos cortes na Xiaomi ainda não está confirmada. Este é o plano operacional consolidado destas propostas; prevalece sobre sugestões anteriores divergentes quanto à ordem, ao âmbito e à ativação automática. Um pedido para executar uma fase abrange apenas essa fase.

## 2. Fontes e base verificada

- [Análises reunidas pelo utilizador](<C:/Users/renat/.codex/attachments/666749f9-f446-4f19-84a9-3eebf0831032/Texto colado.txt>).
- [Auditoria HLS](C:/Users/renat/Downloads/OwnTV-main/docs/ANALISE_HLS_BLOQUEIOS_E_OTIMIZACOES_2026-11-03.md).
- [Complemento áudio/cache/codecs](C:/Users/renat/Downloads/OwnTV-main/docs/COMPLEMENTO_HLS_AUDIO_CACHE_CODECS_2026-11-03.md).

O anexo é material para avaliação, não uma instrução independente para aplicar todas as suas sugestões.

App: `C:/Users/renat/Downloads/OwnTV-main`. Core: `C:/Users/renat/Downloads/OwnTV_Core`. Snapshot indicado pela App: `70aab7a373d8d6dc8449eda6a3c3e720118387e196a81fa0222f15710456ceb9`, 1089 ficheiros. Dependências analisadas: Media3 1.11.1, OkHttp Android 5.5.0 e libmpv 2026.10.4. Não há atualização de bibliotecas prevista neste plano.

Os ficheiros relevantes foram comparados com o snapshot nas auditorias. A execução futura deve verificar novamente o estado real e preservar alterações existentes.

### Evidência anterior à implementação

Há seis verificações locais de transporte e três cenários de componentes de timeshift. Não são testes de aceitação de uma alteração já feita nem prova de reprodução física.

| Ensaio controlado | Resultado | Limite da conclusão |
| --- | --- | --- |
| Cabeçalhos 200, corpo atrasado 650 ms | 2 ms sem peek; 653 ms com peek | Reproduz espera introduzida pela leitura. |
| Recusa 429 com corpo atrasado | Callback aos cerca de 654 ms | Recusa já conhecida fica à espera do corpo. |
| Guard sob prazo de cabeçalhos | Timeout com recusa HTTP na causa | O código não é necessariamente perdido; o prazo inclui o interceptor interior. |
| DNS conclui depois de expirar a espera | Consulta repetida | Sucesso tardio não é aproveitado no cenário. |
| Cinco pedidos lentos e HLS no mesmo host | Fila partilhada 511 ms; própria 2 ms | Starvation possível; não é percentagem de ganho global. |
| Janela local com segmentos 6/3/3 s | Target mudou de 6 para 3 | Confirma geração de manifesto variável. |
| AES-128 local | Aceite à entrada; TS só é válido depois de desencriptar | Confirma incoerência do produtor local, não falha do ExoPlayer direto. |
| Variante sem AC-3 nativo | Excluída | Não demonstra suporte FFmpeg real na box. |

Evidência externa ao código funcional:

- `C:/Users/renat/Downloads/OwnTV-HLS-auditoria-2026-11-03-0snxl8pi`.
- `C:/Users/renat/Downloads/OwnTV-HLS-audio-cache-analise-2026-11-03-uy3j48n3`.

## 3. Contratos obrigatórios

1. Volume da aplicação a 100%; mute do preview independente e volume da box preservado.
2. Preferências globais, por lista e por canal: motor, formato, descodificação, buffers, atraso e versões.
3. Escolhas ExoPlayer/apenas HLS e exceções manuais continuam superiores às heurísticas.
4. Identidade por sintonia/fonte, cancelamento de fontes antigas e fecho real dos corpos HTTP.
5. Intervalo de reabertura, espera do fornecedor e orçamento de tentativas. Fecho local não confirma libertação remota.
6. Retoma imediata com dados reproduzíveis; nenhum pré-buffer de retoma escondido.
7. Limites de memória, armazenamento e sessões; preservar leitores e gravações existentes.
8. TLS normal primeiro, exceções no âmbito autorizado, proteção de redirecionamentos e escolhas explícitas. HTTP recusado não autoriza outra política TLS.
9. Operações do Player na thread exigida pelo Media3; callbacks de rede usam snapshots, não consultas diretas ao Player.
10. Diagnóstico limitado, sem credenciais, sondagens novas ou escrita por byte/fotograma.
11. Preview/multiview/outros leitores mantêm propriedade independente; zapping não cancela globalmente pedidos.
12. Novas preferências, se aprovadas, preservam defaults, overrides e backup/restauro.

## 4. Registo P01–P17 — classificação revista pela secção 16

| ID | Proposta consolidada | Decisão | Fase | Origem |
| --- | --- | --- | --- | --- |
| P01 | Respostas sem peeks desnecessários e corpos de erro limitados | Reduzir; P01b testar primeiro | 1A–1B | HLS-01 |
| P02 | Recusas pelos cabeçalhos e prazo correto | Manter, só remoção de leitura bloqueante | 1C | HLS-02 |
| P03 | Dispatcher próprio de reprodução | Testar primeiro após P01/P02 | 2A | HLS-03 |
| P04 | Sucesso DNS tardio com invalidação | Testar primeiro | 2B | HLS-04 |
| P05 | Recuperação de transporte seletiva | Adiar | 6A | HLS-05 |
| P06 | Classificação conservadora e aprendizagem por host | Reduzir | 3A | HLS-06 |
| P07 | Métricas HLS correlacionadas | Reduzir | 1A e 3A | HLS-07 |
| P08 | Observar áudio após o arranque | Reduzir; integrar com P07 | 1D | Análise inicial de áudio |
| P09 | Ensaio PCM 750/1000 ms | Adiar | 6B | Análise inicial de áudio |
| P10 | TARGETDURATION fixo no timeshift local | Testar primeiro; preservar compatibilidade | 4A | C08 |
| P11 | AES: correção mínima e suporte completo separado | Testar primeiro; preservar compatibilidade | 4B; extensão 6D | C09 |
| P12 | Capacidades efetivas injetadas para variantes locais | Testar primeiro | 5A | C10 revisto |
| P13 | Rota prevista vs real | Reduzir; só representação inicial | 1D; extensão 6C | C11 |
| P14 | Recuperação de áudio durante reprodução | Adiar | 6E | C12 e análise inicial |
| P15 | Separar áudio de origem e de saída | Reduzir; configuração não é sinal físico | 1D | C13 |
| P16 | Evitar cópias inúteis no parser SDR | Adiar até medir custo | 5B | C14 |
| P17 | Explicar reserva, segmentos e limite de memória | Reduzir; texto compacto | 3B | Buffer informativo |

## 5. Especificação de implementação

### P01 — Respostas e corpos de erro

Ficheiros principais: `LivePreviewEngine.kt`, `PlaybackHttpCalls.kt` e um pequeno adaptador de resposta no playback, se necessário.

**P01a, âmbito reduzido:** retirar primeiro os peeks usados apenas para diagnóstico de sucesso. O prefixo de recusas tem também uso na explicação ao utilizador; alterar esse percurso exige testar a mensagem/classificação resultante. Nunca esperar por texto remoto só para completar diagnóstico. Texto opcional pode usar bytes já disponíveis; preservar status/headers e explicação mínima quando não houver texto. Preservar a deteção funcional de TS usada para desfazer uma aprendizagem HLS incorreta em hosts mistos; provar paridade antes de a substituir.

**P01b, acrescentado pelo council:** `OkHttpDataSource.open()` do Media3 1.11.1, linhas 295–309 do sources JAR, lê integralmente o corpo de respostas não 2xx. Retirar só os peeks da app não resolve erros com corpo lento ou muito grande.

Desenho preferível a provar: manter Media3 original e envolver respostas de erro do playback num corpo em memória, vazio ou prefixo já recebido. Preservar status, mensagem e headers; o wrapper delega fecho ao original e é registado na resposta entregue a `PlaybackHttpCalls.opened`.

- Não chamar `call.cancel()` antes de `opened`: este rejeita chamadas canceladas.
- Não esquecer o corpo original nem declarar fecho antes de ele terminar com sucesso. Serializar fechos concorrentes; uma falha não pode impedir nova tentativa de limpeza.
- Respostas 2xx conservam os bytes e o percurso originais.
- Preservar 416 que representa EOF quando a posição pedida coincide com o tamanho do recurso; manter erro no 416 inválido.
- Medir o custo limitado de descarte no fecho OkHttp; não prometer latência zero ou fecho remoto garantido.

Não criar uma recusa universal nem copiar todo o DataSource. A abordagem só é aceite após ensaio com DataSource real e cadeia real de guards/ownership.

### P02 — Guard e prazo

Ficheiros: `ProviderRetryGuard.kt`, `ResponseDeadline.kt`, `LivePreviewEngine.kt`. Processar status e `Retry-After` sem aguardar texto remoto. Manter guard como interceptor de rede e conservar a ordem: movê-lo para aplicação permitiria follow-ups antes do veto.

Manter preparação privada do timeshift loopback e `Retry-After` em segundos/data. Não converter recusas em loops. Clarificar a fase medida pelo prazo após remover leituras. O valor geral de 8 s não muda neste lote.

### P03 — Fila própria

Ficheiro: `StreamingHttpClient.kt`; validar coexistência com `OwnTVApp.kt`. Um dispatcher duradouro para reprodução, herdado pelos clientes derivados. Não criar por canal, desligar executor ao sair ou chamar `dispatcher.cancelAll()` no zapping.

A separação pode aumentar concorrência agregada no mesmo host. Manter limites moderados e controlo de sessões no coordenador existente. Medir espera de fila depois de P01/P02; não acrescentar semáforos bloqueantes. O dispatcher controla chamadas assíncronas enquanto ocupam a fila/callback, não toda a leitura do corpo pelo loader nem todos os pedidos síncronos. Pedidos de segmentos não equivalem automaticamente a sessões contratadas.

### P04 — DNS tardio

Ficheiros: `BoundedDns.kt` e integração com contexto DNS/rede. Guardar sucesso na conclusão, associado à geração da configuração e identidade da consulta. Resultado antigo não substitui posterior nem reintroduz endereço da configuração anterior.

Só respostas válidas/não vazias; manter limites de entradas/workers. Validade conta da conclusão, sem renovar ao ler. Cinco minutos são política local: `Dns.lookup` não fornece TTL autoritativo. Não anunciar esse TTL nem prometer cancelar resolver nativo. Testar mudanças de DNS/rede com consultas pendentes e conclusões fora de ordem.

### P05 — Retries de transporte

**Condicionada. Não ativar `retryOnConnectionFailure(true)` indistintamente.** Um GET inicial Xtream pode abrir/contar sessão; repetir pedido enviado sem resposta pode agravar A–B–A.

Antes de alterar, classificar entrada/abertura, playlist, segmento e chave; definir tentativas elegíveis e orçamento comum. Provar recuperação de ligação reutilizada fechada sem repetir perigosamente abertura de sessão. Preservar guard 408/429/503, TLS, cancelamento e cooldown. Se não houver política segura com complexidade aceitável, manter o comportamento atual e melhorar apenas o percurso já existente no Media3.

### P06 — Política HLS conservadora

Ficheiros: `LiveStreamQuirks.kt`, `LivePreviewEngine.kt`, `LiveExoWatchdog.kt`, `LiveTuneController.kt`. Dois segmentos recusados não provam tokens incompatíveis ou defeito do servidor inteiro. Rever inferências/mensagens e limitar conclusões ao canal/sessão até haver prova para uma regra por host. Preservar gates de motor/apenas HLS.

**Correção da análise anterior:** `onLoadError` já exige `C.DATA_TYPE_MEDIA` antes de contar segmentos recusados. Chaves fora dessa contagem são contrato de regressão, não novo bug confirmado nessa função.

Distinguir 403/404/410 e estágio do pedido. Aproveitar recuperação Media3 antes de reabrir canal. Uma nova rotina própria de refresh de playlist não está aprovada automaticamente: primeiro provar a lacuna. Não saltar segmentos nem fabricar continuidade sem decisão explícita sobre perda de conteúdo.

### P07 — Métricas HLS

Usar listeners e escrita limitada existentes. Medir proprietário/pedido, fila, DNS, ligação/TLS, cabeçalhos, bytes/duração e retries. OkHttp 5.5.0 já oferece `dispatcherQueueStart`, `dispatcherQueueEnd` e `retryDecision`. Para transferência completa, usar eventos de load Media3; tempo do interceptor não é tempo total do segmento.

Correlacionar avanço da playlist, variante/faixa, buffer e estado. Metadados compactos, agregação e limites; sem logs por byte/fotograma. Contadores baratos podem existir sem detalhe; strings/disco/pormenor seguem a opção de diagnóstico. Não ler Player na thread da rede.

### P08 — Áudio após o arranque

**Só observação aprovada.** Associar underruns, codec, formato, criação/libertação de saída, velocidade e tempos a IDs de sintonia/saída. Avanço do relógio não prova som audível na TV.

Não ligar sintomas PCM ao resgate de passthrough. Em Stereo, uma razão do watchdog pode terminar o canal; reutilizar esse caminho para underrun isolado criaria regressão. Nenhum novo watchdog de reinício nem detetor por amplitude/silêncio.

### P09 — Reserva PCM

**Condicionada a underruns de saída demonstrados com media disponível.** Default Automático mantém o comportamento Media3 atual. Comparar atual/750/1000 ms na mesma box, rota, canal e codec; só PCM. Não alterar passthrough, volume, canais, frequência ou codec de vídeo. Capacidade do buffer não equivale a atraso fixo adicional.

Se houver UI, explicar âmbito PCM, default Automático, persistência/backup e overrides. Aplicar na próxima abertura para evitar reconexão inesperada. Não criar preferência sem integração testada.

### P10 — Target do timeshift

**Gate revisto em 07/11: testar primeiro, sem nova rejeição geral por metadados.**

Ficheiros: `LocalSegmentStore.kt`, `LocalTimeshiftSession.kt`. Fixar target local inteiro/positivo antes da primeira publicação e preservar na rotação. Separar esta correção da política perante target upstream ausente, não canónico ou incoerente: testar essa política antes de impor nova rejeição. Validar durações pelo arredondamento HLS, sem rejeitar um EXTINF válido nem inventar um fallback universal.

Não usar máximo histórico crescente nem target exagerado. Se novo conteúdo violar o target, preservar segmentos existentes e terminar/recusar o novo conteúdo com motivo específico. Nova apresentação exige identidade própria e decisão de retoma; não reiniciar silenciosamente o mesmo manifesto. Não mudar parser de gravações sem testes próprios.

### P11 — AES no timeshift

**P11a candidata; testar primeiro segundo a secção 16.7:** detetar AES-128 não suportado nos segmentos novos/elegíveis que seriam efetivamente transferidos, antes do download. Não rejeitar uma janela por chave histórica, segmento antigo ou GAP ignorado. Explicar a limitação e permitir seleção normal do direto após fecho/libertação; não é obrigatório criar outra UI. Não alterar globalmente `HlsMediaPlaylist.isEncrypted` nem o comportamento das gravações.

**P11b condicionada:** obter chave/contexto corretos, aplicar IV explícito/por sequência e validar antes de publicar. Contar cifrado `.part`, `.clear`, leitores antigos e reserva de disco: reutilizar `HlsAes` sem contabilizar espaço temporário pode violar quota.

Testar AES/NONE, rotação, chave truncada/recusada, padding, ranges cifrados, falta de espaço e cancelamento. Erros criptográficos não originam loop de rede. Nunca publicar parcial, deixar temporários ou guardar chaves em logs/backups. Não alterar gravações existentes nem supor suporte de outros métodos.

### P12 — Capacidades efetivas

**Desenho candidato; testar primeiro segundo a secção 16.** Core mantém política/modelo; uma eventual ponte App/player usa o contrato `variantSupported` já existente. Suporte de MIME no FFmpeg não comprova configuração, perfil, layout ou desempenho. Demonstrar capacidade na configuração real do leitor local antes de introduzir a ponte. Sem dependência Core→player.

Preservar limites de vídeo, tratamento de metadata desconhecida e restrições estruturais, incluindo áudio separado que o produtor não captura. Não alterar default de `HlsRecordingPlan.selectVariant`, partilhado com gravações. Biblioteca carregada não prova suporte de todos os codecs; em caso inconclusivo manter a política conservadora.

### P13 — Rota prevista e real

**P13a aprovada:** marcar `getAudioDevicesForAttributes` como estimativa, corrigindo representação. Não zerar offsets guardados nem mudar a aplicação destes no lote. Desconhecido não é confirmado.

**P13b condicionada:** observar AudioTrack real com API suportada/lifecycle testado. Media3 1.11.1 dispõe de `ForwardingAudioOutputProvider` e `AudioTrackAudioOutput.getAudioTrack`; verificar a instância/API efetiva na integração.

Preservar caminho Stereo: capabilities mínimas, mixer PCM16, offload não suportado e `DelayedClockAudioSink`. Provider com Context pode reintroduzir passthrough. Não misturar provider moderno com setters antigos incompatíveis. Remover callbacks na libertação, rejeitar eventos tardios, sem reflexão/polling por fotograma. Primeiro observar; não alternar offsets para zero durante recriações.

### P14 — Recuperação do áudio

**Condicionada a falha persistente demonstrada e protótipo viável.** Fallback de inicialização não garante recuperação de codec que falha a meio.

Preferir recuperar só saída/áudio se a API suportar. Se exigir reconstrução/reabertura HLS, usar coordenador, ownership, cooldown e orçamento existentes, sem job HTTP paralelo. Não prometer troca invisível.

Uma ocorrência não pode acionar aumento de buffer, latch Stereo, FFmpeg e reconexão em paralelo. Uma decisão/limite comum por sintonia; manter escolha de vídeo por hardware. Se não for seguro recuperar áudio isolado, manter automático pendente e usar preferência existente na próxima abertura.

### P15 — Origem e saída de áudio

Reutilizar configuração do AudioTrack já recebida, sem novo amostrador. Mostrar compactamente codec/frequência de origem, PCM/passthrough efetivo e rota estimada/confirmada. Separar capacidade configurada de saída observada. Frequências diferentes não provam erro; não forçar 48 kHz ou conversão.

### P16 — Parser SDR

Prioridade baixa; lote isolado em `SdrVariants.kt`. Evitar copiar playlists de segmentos quando só a multivariante precisa de filtragem. Preservar variáveis, content steering/casos especiais, captions e parser Media3. Entrada inesperada mantém comportamento seguro; Preferir SDR desligado conserva percurso anterior.

Testar equivalência e medir alocações. Não alterar preferência SDR nem anunciar correção dos cortes de dois segundos.

### P17 — Informação de buffer

**Apenas informação aprovada.** Distinguir pré-buffer inicial, reserva de transmissão, limiar superior, reserva efetiva, limite de bytes e buffer PCM. Explicar que terminar segmento pode ultrapassar limiar e que limite de bytes pode impedir duração pedida.

Reutilizar medições de segmentos existentes, sem novos pedidos. Avisar só com informação suficiente, sem alertas repetidos. Não alterar valores guardados, adaptar buffer/qualidade automaticamente, remover limite de RAM ou atrasar retoma.

## 6. Conflitos e decisões de integração

| Interação | Regressão possível | Regra de integração |
| --- | --- | --- |
| P01 × hosts mistos | Perder deteção de TS e falhar parsing | Preservar sniff ou provar observação equivalente no consumo normal. |
| P01 × DataSource/drain | Corpo original órfão ou fecho declarado cedo | Wrapper conserva original; testar entrega/read/close/cancel reais. |
| P02 × P05 | Retry acontece antes do veto do fornecedor | Guard de rede mantido; sem retries globais cegos. |
| P03 × P05 | Concorrência e retries geram bursts | Um orçamento/controlo de sessões, limites moderados; P05 condicionado. |
| P04 × DNS/rede alterados | Resposta antiga reintroduz endereço obsoleto | Geração/contexto e identidade da consulta, validade limitada. |
| P07/P08 × desempenho | Diagnóstico gera mais trabalho que o observado | Eventos existentes, agregação e buffers/escrita limitados. |
| P08 × watchdog Stereo | Underrun PCM termina o canal | Observação separada da ação; não reutilizar resgate para novos sintomas. |
| P09/P13b × Stereo/AV delay | Provider reativa passthrough ou perde mixer/clock | Um ponto de construção de áudio, com paridade de capabilities/mixer/offload/clock. |
| P14 × reabertura | Resgate encontra cooldown ou abre nova sessão | Mesmo coordenador; preferir recuperação sem rede; automático condicionado. |
| P10 × mudança na origem | Manifesto altera parâmetros ou perde conteúdo | Target fixo, erro controlado/nova apresentação explícita e segmentos preservados. |
| P11b × quota/cancelamento | Cifrado+claro excedem disco ou publicam parcial | Reserva temporária completa, publicação atómica e limpeza verificada. |
| P12 × camadas/gravações | Ciclo de dependências/alteração da gravação | Capacidade injetada App/player→Core; default partilhado preservado. |
| P13 × offsets | Sincronismo oscila por rota provisória | Primeiro corrigir representação; não reescrever offsets existentes. |
| P06 × opções do utilizador | Motor/formato muda indevidamente | Gates Exo/HLS e exceções manuais continuam superiores às heurísticas. |

## 7. Fases e critérios de avanço

Todas as tarefas funcionais estão por executar. Este catálogo agrupa áreas técnicas; a ordem e os gates vigentes são os da secção 16. Itens «testar primeiro» não entram automaticamente em produção ao executar uma fase, e itens «adiar» não fazem parte da execução corrente. Subfases devem ser pequenas; a instrumentação mínima acompanha apenas a mudança que precisa de medir.

### Fase 0 — Preparação

- [x] Verificar checkout/snapshot/alterações existentes — comparação integral dos 1089 inputs anteriores; ver secção 17.
- [x] Guardar cópia verificável dos ficheiros afetados — cópias e hashes externos ao checkout; ver secção 17.
- [x] Registar configurações de ensaio sem credenciais e comportamento de referência — fixtures sintéticas e contratos preservados; sem leitura das preferências ou reprodução da box.
- [x] Distinguir direto, diferido e timeshift local — nenhum comportamento novo de timeshift/gravação nesta fase.

Sem migração da base de dados em uso nem alteração de preferências pessoais.

### Fase 1 — Observar e retirar esperas desnecessárias

- [x] **1A:** P07 mínimo + P01a: métricas e diagnóstico sem leituras adicionais em respostas bem-sucedidas. Implementado no âmbito reduzido; ver secção 17.
- [ ] **1B — protótipo condicionado:** P01b só após DataSource real e fecho comprovado; sem integração automática.
- [x] **1C:** P02: recusa pelos cabeçalhos, mantendo guard/prazo. Implementado; ver secção 17.
- [x] **1D:** P08 + P13a + P15: observação de áudio, rota estimada, origem/saída. Implementado sem nova recuperação; ver secção 17.

Avançar só com 200 intacto, erros lentos/grandes/gzip, 416 válido/inválido, cancelamento e fecho concorrente testados. Hosts mistos mantêm compatibilidade. Não introduzir resgate automático nem mudança do som.

### Fase 2 — Concorrência e DNS

- [x] **2A — testar primeiro:** fila residual demonstrada após P01/P02; dispatcher próprio integrado e testes locais/Release da App aprovados em 07/11/2026. Ver secção 18; aceitação física e lint integral anterior separados.
- [x] **2B — testar primeiro:** benefício e contexto DNS/rede comprovados; âmbito final integrado e validado em 08/11/2026, incluindo ensaio conjunto 2A/2B. Ver secção 19.

Sublotes distintos. Exigir isolamento de filas, cancelamento por proprietário, preview/multiview independentes, validade DNS e resultados fora de ordem. Testar os dois em conjunto antes de encerrar a fase.

### Fase 3 — Política HLS e clareza do buffer

- [x] **3A:** P06 + completar P07 no âmbito reduzido: estágio/status e inferências conservadoras. Implementada e validada localmente em 08/11/2026; ver secção 20.
- [x] **3B:** P17: informação de reserva/segmentos/bytes, sem alterar valores. Implementada e validada localmente em 08/11/2026; ver secção 21.

Exigir testes de playlist/segmento/chave, opções Exo/apenas HLS e espera do fornecedor. Manter recuperação normal Media3. Não acrescentar refresh/skip/reabertura em paralelo.

### Fase 4 — Produtor local de timeshift

- [ ] **4A — testar primeiro:** P10, manifesto local coerente sem nova rejeição geral de origens antes utilizáveis.
- [ ] **4B — testar primeiro:** P11a, paridade dos casos antes aceites; sem bloqueio geral de HLS/AES.

Exigir janela a rodar, duração variável, leitores antigos abertos, sequência reiniciada, quota e fecho sem sobreposição. Direto e gravações existentes conservam comportamento. Esta fase corrige apenas o percurso local.

### Fase 5 — Capacidades e eficiência isolada

- [ ] **5A — testar primeiro:** P12, variante realmente reproduzida pelo leitor local e configuração efetiva.
- [ ] **5B — adiada:** P16, medir antes de separar o parser SDR.

Testar paridade nativa, MIME FFmpeg suportado/não suportado, restrições de estrutura e gravações. No parser, provar equivalência e redução de cópias. Não depende de novo mecanismo de recuperação automática.

### Fase 6 — Ramos condicionados; não executar automaticamente

- [ ] **6A / P05:** protótipo de retry por tipo de pedido, sem replay perigoso de sessão.
- [ ] **6B / P09:** ensaio PCM atual/750/1000 ms perante underruns demonstrados.
- [ ] **6C / P13b:** rota real, lifecycle e paridade Stereo comprovados.
- [ ] **6D / P11b:** AES local completo, com chave/quota/cancelamento.
- [ ] **6E / P14:** recuperação de áudio com evidência e autoridade única.

Cada ramo requer decisão própria depois do ensaio. Sem benefício ou API segura, manter pendente/recusar o ramo e concluir os restantes. LL-HLS, PDT e rotina nova de refresh de playlist também precisam de evidência própria antes de modificar comportamento.

## 8. Validação e limites de aceitação

| Área | Cenários mínimos | Resultado esperado |
| --- | --- | --- |
| DataSource | 403/404/410 com corpo lento, silencioso, grande e gzip | Status/headers preservados; erro limitado em leitura/memória. |
| Range | 416 com posição igual/diferente do tamanho | EOF válido e erro inválido preservados. |
| Guard | 408/429/503; Retry-After segundos/data | Sem follow-up oculto nem pedido antecipado; loopback preservado. |
| HTTP lifecycle | Cancelar em fila/DNS/TLS/headers/body/close; resposta tardia | Fonte antiga não reabre; corpo original não fica órfão; drain só após fecho real. |
| Concorrência | Imagens lentas + HLS; preview/multiview | Reprodução protegida e nenhum cancelamento global. |
| DNS | Tardio, config/rede mudam, respostas fora de ordem | Só contexto/consulta válidos atualizam cache; falhas não ficam em cache. |
| Zapping | A–B–A, A–B–C–A, sair/entrar fullscreen | Ordem/propriedade/cooldown preservados, sem sobreposição local. |
| HLS | Host misto, 404 transitório, 403 persistente, chave recusada | Sem aprendizagem injustificada nem motor/formato proibido. |
| Áudio | PCM/passthrough; Stereo/Auto; pausa/buffering/silêncio | Nenhum resgate novo; volume 100%; dados da saída atual. |
| Rota | HDMI/Bluetooth/USB; recriação/callback tardio | Previsto não é confirmado; offsets preservados. |
| Timeshift | Rotação, target inválido/mais longo, leitores fixados | Apresentação/quota coerentes e conteúdo existente preservado. |
| AES autorizado | Rotação/NONE/IV/range/padding/disco/cancelamento | Só segmento completo validado publicado; sem chaves ou temporários abandonados. |
| Capacidades | Nativo ausente, FFmpeg sem MIME, motor pinado | Apenas percurso permitido autoriza variante; vídeo/gravações preservados. |
| Preferências | Opções afetadas; backup/restauro de opções novas | Preservar explícitos/overrides; defaults equivalentes aos anteriores. |

Reutilizar os testes existentes de `PlaybackHttpCalls`, `SourceDrain`, ownership, áudio e timeshift; acrescentar cenários em falta. Testes devem verificar comportamento, não repetir implementação.

Quando a fase alterar Core, renovar snapshot pelo mecanismo do projeto, comparar ficheiros/lock e validar a App com esse snapshot. Compilar e executar testes dirigidos e checks exigidos. Não desativar verificação de dependências ou lint nem criar baseline para esconder regressão.

Na Xiaomi, comparar canal/variante, motor, saída e definições equivalentes por pelo menos 30 minutos; repetir com navegação/zapping. Medir início, pausas de rede, cortes só de áudio, tentativas e fila. Recolher ocorrências com hora quando existam; diagnóstico é redigido. Não contactar fornecedor usando credenciais na máquina de desenvolvimento sem âmbito específico.

Compilar, testar componentes, instalar APK e validar reprodução física são evidências distintas. O utilizador gera o APK. Este plano não autoriza assinatura, instalação, commit, push ou publicação. Não prometer ganhos percentuais sem comparação equivalente.

## 9. Ideias excluídas ou reduzidas

Não executar como otimização geral:

- Retries para todos os GET sem saber se já abriram sessão.
- Reiniciar por underrun PCM, silêncio ou rota provisória.
- Remover drain/confirmação de fecho ou acrescentar atrasos fixos a todos os canais.
- Aumentar workers/concorrência/memória sem limites ou carregar canais vizinhos.
- Cache genérica de playlists; retirar tokens/queries da identidade ou partilhar entre contas.
- Forçar 48 kHz, software ou hardware para todo o áudio.
- Flags permissivas TS, remoção de PDT ou continuidade fabricada em todos os canais.
- Troca genérica para Cronet/HTTP2/HTTP3, pipeline HLS novo ou fork completo do DataSource.
- Reaplicar correções já presentes em Media3 1.11.1 ou vender defaults equivalentes como ganho novo.

Não foram eliminadas integralmente as 17 famílias por serem todas falsas. O âmbito útil foi delimitado; expansões permanecem condicionadas e os atalhos acima foram recusados. P01/P02 partilham raiz de espera e não representam duas causas independentes demonstradas dos cortes. Instrumentação também não equivale a um bug de reprodução confirmado.

## 10. Decisões da revisão

| Especialidade | Parecer adotado | Ajuste ao plano anterior |
| --- | --- | --- |
| Rede/HLS | Retirar esperas, separar fila e DNS com contexto; retry seletivo condicionado | Cobrir corpo integral do DataSource; preservar guard, sniff TS e 416. |
| Áudio/codecs | Observar primeiro, corrigir estimativa e origem/saída | Separar deteção de ação; preservar Stereo/mixer/offload/clock; PCM/rota real/recovery condicionados. |
| Cache/timeshift | Target fixo, AES mínimo e capacidade injetada | Contar temporários cifrado+claro; não alterar gravações nem criar Core→player. |

A consolidação é uma revisão técnica assistida por agentes, não uma validação humana ou física. Os contratos e testes são o mecanismo para detetar que uma melhoria prejudica outra; não há promessa de risco zero.

## 11. Fontes primárias e rastreabilidade

- [Media3: personalização](https://developer.android.com/media/media3/exoplayer/customization).
- [Media3: stacks de rede](https://developer.android.com/media/media3/exoplayer/network-stacks).
- [RFC 8216: target e playlists](https://datatracker.ietf.org/doc/html/rfc8216#section-6.2.1).
- [RFC 9110: idempotência](https://datatracker.ietf.org/doc/html/rfc9110#section-9.2.2).
- [Android: rota prevista](https://developer.android.com/reference/android/media/AudioManager#getAudioDevicesForAttributes(android.media.AudioAttributes)).
- [Android: rota ativa](https://developer.android.com/reference/android/media/AudioRouting#getRoutedDevice()).
- [Media3: fallback na inicialização](https://developer.android.com/reference/androidx/media3/exoplayer/DefaultRenderersFactory#setEnableDecoderFallback(boolean)).
- [Media3: buffer PCM](https://developer.android.com/reference/androidx/media3/exoplayer/audio/DefaultAudioTrackBufferSizeProvider.Builder#setTargetPcmBufferDurationUs(int)).
- [OkHttp 5.5.0: Dispatcher](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Dispatcher.kt).

Detalhes de API foram cruzados com os sources JAR locais das versões usadas, incluindo `OkHttpDataSource.java` e `EventListener.kt`. Não depender só de documentação de versões posteriores.

Hashes dos documentos de entrada:

```
Texto colado.txt: db708d865ccb2ca827a5f262b69ac742ae206290eb388c7c02bd5c70d8ad1622
ANALISE_HLS_BLOQUEIOS_E_OTIMIZACOES_2026-11-03.md: 2ff6a6d2a3fd8492c1091e1d3ef055997d495412c1b5c29ca69b316146745749
COMPLEMENTO_HLS_AUDIO_CACHE_CODECS_2026-11-03.md: db7ae59514d6d6441fa91db30c584107c75b84af67320dcbdf9916252ec5fbdb
```

## 12. Estado para retoma

- [x] Reunir e deduplicar as análises.
- [x] Reanalisar código/fontes com três especialistas.
- [x] Resolver conflitos e criar este plano.
- [x] Pedido de implementação da primeira fase recebido em 07/11/2026; âmbito executado em Fase 0 + 1A/1C/1D reduzidas, conforme secção 17.
- [ ] Registar por fase: alterações, testes, snapshot, limitações e rollback.

Não marcar uma fase concluída apenas por compilar nem avançar silenciosamente para ramos condicionados.

## 13. Mapa exato de alterações — revisão de 3 de novembro de 2026

### 13.1. Como executar sem perder contexto

Este mapa foi cruzado com os ficheiros presentes nos dois checkouts. As âncoras abaixo são nomes de classes, métodos ou expressões pesquisáveis, **não números de linha permanentes**. Antes de editar, reler a função inteira e os seus chamadores; se a âncora tiver desaparecido ou o comportamento já tiver sido corrigido, atualizar o plano em vez de aplicar o patch por aproximação.

Regras de âmbito:

1. **Alterar** = substituir o comportamento identificado no ficheiro indicado, preservando o restante.
2. **Remover** = retirar apenas a leitura, inferência ou cópia explicitamente identificada. **Não há ficheiros, bibliotecas ou funcionalidades inteiras aprovados para eliminação neste plano.**
3. **Adicionar** = o dado, contrato ou teste descrito. Um nome marcado **NOVO PROPOSTO** ainda não existe e não pode ser citado como evidência de implementação.
4. **Preservar / apenas verificar** = consultar e testar o contrato; não fazer um refactor preventivo.
5. **Condicionado** = não alterar agora. A localização é indicativa para um ensaio futuro; faltam as condições da secção 5.
6. Nenhuma fase aprovada exige novos defaults de reprodução, migração de base de dados, alteração de credenciais ou novas opções de definições. UI de PCM, novos retries e recuperação automática continuam condicionados.
7. Não editar os ZIPs em `vendor`, sources JAR, caches Gradle ou cópias extraídas. A fonte editável é o checkout Core; o snapshot da App é gerado só depois da validação da fase.

### 13.2. Inventário de ficheiros existentes

Os identificadores servem apenas para encurtar este documento. Cada um corresponde exatamente ao caminho absoluto da tabela. Não são nomes de módulos ou novas APIs.

| Ref. | Ficheiro existente e papel nesta revisão |
| --- | --- |
| E1 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt` — cliente HTTP, listeners e informação Exo. |
| E2 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ProviderRetryGuard.kt` — guard HTTP e `ProviderHttpRefusal`. |
| E3 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ResponseDeadline.kt` — prazo existente. |
| E4 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/PlaybackHttpCalls.kt` — propriedade e fecho real das respostas. |
| E5 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/PlayerDiagnostics.kt` — classificação, incluindo `PlayerErrors.httpStatusOf`. Não existe aqui um ficheiro separado chamado `PlayerErrors.kt`. |
| E6 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveStreamQuirks.kt` — aprendizagem e recusas. |
| E7 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveExoWatchdog.kt` — consumo da suspeita de recusa de segmentos. |
| E8 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveTuneController.kt` — coordenação e gates de motor/formato. |
| E9 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt` — `AudioWatchdog` e construção da saída. |
| E10 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioRouteSync.kt` — rota prevista e offsets. |
| E11 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/StreamInfo.kt` — modelos e `displayText`. |
| E12 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ExoAudioSupport.kt` — `FfmpegAudio`, atualmente interno ao módulo player. |
| E13 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/SdrVariants.kt` — `ParserFactory` e `Capture`. |
| N1 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/StreamingHttpClient.kt` — cliente/pool de reprodução. |
| N2 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/BoundedDns.kt` — espera, cache e consultas pendentes. |
| N3 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/DnsConfig.kt` — `DnsConfigHolder`, configuração e resolver. |
| N4 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/ConnectivityObserver.kt` — `state` com identidade da rede; não fornece atualmente um contrato público de geração DNS. |
| N5 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/di/DataModule.kt` — ligação dos singletons DNS/rede/HTTP. |
| T1 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/timeshift/LocalSegmentStore.kt` — `commit`, `playlist`, rotação e quota. |
| T2 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/timeshift/LocalTimeshiftSession.kt` — `playlist`, `loadSnapshot`, `capture`, `download`, `close`, `variantSupported`. |
| T3 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/timeshift/LocalTimeshiftServer.kt` — servidor local; sobretudo contrato de regressão. |
| T4 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/timeshift/HlsVariantSupport.kt` — capacidades nativas e política de variantes. |
| T5 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsMediaPlaylist.kt` — parser partilhado; preservar semântica de gravações. |
| T6 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsRecordingPlan.kt` — seleção partilhada; preservar default de gravações. |
| T7 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsAes.kt` — desencriptação existente; apenas referência na fase mínima. |
| A1 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/features/live/LiveViewModel.kt` — `watchWithLocalTimeshift`, abertura, libertação e erro local. |
| A2 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/player/StreamInfoOverlay.kt` — apresentação compacta/detalhada. |
| A3 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/OwnTVApp.kt` — `newImageLoader`; verificar isolamento, sem mudança obrigatória. |
| R1 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/res/values/live_buffer_diagnostics.xml` — textos técnicos base já existentes. |
| R2 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/res/values-pt/live_buffer_diagnostics.xml` — textos técnicos em português. |
| R3 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/res/values/local_timeshift.xml` — explicação base da indisponibilidade local. |
| R4 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/res/values-pt/local_timeshift.xml` — explicação em português. |
| R5 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/res/values/ui_clarity.xml` — resumo `ui_buffer_limits`. |
| R6 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/res/values-pt/ui_clarity.xml` — resumo em português. |

**Atenção aos recursos:** os textos técnicos do player estão em `OwnTV_Core/core/src/main/res`, não numa pasta `player-core/src/main/res/values` presumida. As pastas desta última localização não existem no checkout verificado.

### 13.3. Operações mínimas por proposta

#### P01 — Remover leituras de diagnóstico e limitar o corpo entregue ao DataSource

**Alterar E1**, em `diagnosticHttpClient`, no interceptor que chama `playbackHttpCalls.opened`:

- **Remover primeiro** o `response.peekBody(564)` do caso de sucesso usado apenas para diagnóstico. O ramo de erro do bloco `failed || LiveDiagnosticsLog.enabled` também fornece a mensagem do fornecedor: modificá-lo separadamente, com teste de classificação/mensagem e sem esperar por novos bytes apenas para diagnóstico. Corrigir os comentários; não alegar paridade do texto se passar a estar ausente.
- **Preservar** o outro `peekBody`, condicionado por `response.isSuccessful && tune.hlsFromHostLesson` e URL da sintonia atual: é a deteção funcional de TS. Não o eliminar neste lote.
- **Alterar** `prefix`, `failureText` e o diagnóstico de resposta para aceitarem prefixo ausente. Status e cabeçalhos bastam para classificar a recusa. Texto remoto é opcional; só usar bytes já em memória, com limite de 564 bytes e redação existente. Não usar `request`, `peekBody`, `read`, `exhausted` ou equivalente para tentar obter novos bytes para esta mensagem.
- **Adicionar**, apenas na subfase 1B, adaptação de respostas não 2xx antes de registar a resposta entregue em `opened`: preservar código/mensagem/headers e apresentar ao Media3 um corpo de erro finito em memória. O adaptador mantém o corpo original para fecho, mesmo se o conteúdo apresentado estiver vazio. A resposta registada tem de ser a mesma efetivamente devolvida ao DataSource.
- **Preservar** 2xx sem substituição do corpo; o Media3 continua responsável pela exceção HTTP e pela exceção especial 416/EOF. Não transformar todos os códigos em `ProviderHttpRefusal`.

**Adicionar NOVO PROPOSTO**: `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/BoundedPlaybackErrorBody.kt`, se o adaptador for necessário após o ensaio. Este nome é uma escolha de implementação futura, não uma classe existente. Responsabilidade única: fonte de erro finita + posse/fecho do corpo original. Não lê o original para preencher o prefixo.

**E4, verificar primeiro; alterar só se o adaptador exigir:** `opened`, fecho/retirada de respostas e confirmação de fecho. Fecho concorrente deve ser serializado; só confirmar depois do fecho efetivo. Uma tentativa que lance exceção não pode marcar definitivamente o recurso como libertado nem impedir nova limpeza. Não chamar `cancel()` antes de `opened` nem retirar o registo do corpo original por engano.

**Não alterar:** Media3 original, E1 `reprepare`, ordem de drain/retirada, E5 classificação HTTP e respostas 2xx. `OkHttpDataSource.java` é fonte de consulta da dependência, não ficheiro editável do projeto.

**Prova antes de aceitar 1B:** DataSource real com erro lento/grande, corpo vazio, cancelamento durante entrega, close concorrente/falho e 416 válido/inválido. Um teste só ao adaptador não prova a integração Media3. Se o ensaio falhar, não publicar 1B; manter 1A independente.

#### P02 — Recusa imediata a partir de cabeçalhos

**Alterar E2**, `ProviderRetryGuard.intercept`:

- **Remover** `response.peekBody(564).string()` na criação de `ProviderHttpRefusal`.
- **Alterar** `responseText` para opcional/vazio ou prefixo já disponível sem leitura; conservar `responseCode` e `responseHeaders`.
- **Preservar** o conjunto específico 408/429/503, `LocalPlaylistReadiness.preparing`, fecho da resposta, callback e lançamento da recusa. «Imediata» significa sem esperar pelo texto; não significa que `close()` de transporte tenha custo zero.

**E1 e E3:** conservar instalação/ordem do guard como interceptor de rede e prazo geral de 8000 ms. Em E3, alterar apenas descrição/medição se os testes mostrarem que continua incorreta depois de retirar a leitura interior. Não aumentar o prazo nem mover o guard para aplicação como solução.

**Não alterar:** `Retry-After`, exceção privada de preparação loopback, classificação em E5 e política TLS. Testar recusa com corpo atrasado + prazo curto e preparação local 503 que deve continuar transitória.

#### P03 — Separar a fila da reprodução

**Testar primeiro; alterar N1 apenas se o ensaio residual justificar**, na construção de `client`:

- **Adicionar** um único `okhttp3.Dispatcher` com duração do singleton de reprodução e passá-lo por `.dispatcher(...)` ao cliente derivado.
- **Preservar** inicialmente os limites padrão da versão usada; registar os valores efetivos no ensaio. Não aumentar limites para obter um resultado artificial.
- **Remover apenas** a partilha implícita do dispatcher do cliente base. Pool próprio, TLS, DNS, proxy, headers, protocolos e timeouts mantêm a configuração existente.
- **Adicionar** teste de identidade/isolamento e ensaio de fila com metadados/imagens no mesmo host.

**N5 e A3, apenas verificar:** os consumidores continuam a receber o singleton correto; imagens/EPG não passam a usar a fila de streaming. Não é necessário dar outro dispatcher ao Coil para isolar N1. Clientes obtidos por `newBuilder()` a partir do playback herdam o seu dispatcher.

**Não alterar:** quantidade de sessões permitidas, `evictAllAsync`, budgets de sintonia. Não usar `dispatcher.cancelAll()` ou encerrar o executor por canal. Não há novo semáforo ou atraso global aprovado.

#### P04 — Aproveitar DNS tardio sem misturar configurações/redes

**Prototipar/testar primeiro; não executar já o refactor de N2/N3/N1/N5/E1.** O lookup atual inicia refresh mesmo quando há cache; publicar sucesso tardio não elimina por si consultas repetidas. Provar ganho e invalidação antes de escolher as alterações candidatas de N2 `lookup`, `FutureTask.done` e chaves de `answers`/`inFlight`:

- **Mover** a publicação do sucesso para a conclusão da tarefa; não depender de `pending.get` terminar dentro do prazo do primeiro chamador.
- **Adicionar** contexto imutável por consulta: geração DNS/rede + resolver associado à configuração capturada + identidade da tarefa. As chaves/cache têm de impedir reaproveitar respostas de contexto antigo.
- **Preservar** limite de 64 entradas, executor de 4 workers/fila 64, espera e validade limitadas; copiar lista válida/não vazia. A validade começa na conclusão, não na leitura da cache.

**Alterar N3**, `DnsConfigHolder`: a classe tem atualmente `current`, `snapshot()` e `dns`, mas **não tem geração pública**. Adicionar um contrato que capture configuração/resolver/geração de forma coerente, incrementando a geração apenas quando a configuração mudar. Não inventar uma chamada a `dnsHolder.generation` como se já existisse, nem capturar geração antes e resolver com um `current` posterior.

**Alterar N1 e N5 somente para ligar esse novo contrato** ao DNS de reprodução. Usar a identidade observada em N4 `state` para invalidar mudança de rede; `isOnline` sozinho não distingue Wi-Fi de Ethernet. **N4 não precisa de nova sondagem nem novo callback Android:** consumir o observador partilhado existente. Não vincular sockets a uma `Network` diferente neste lote.

**Alterar E1**, atual `.dns(BoundedDns(streamingHttp.client.dns))`, para receber o contexto que a ligação anterior fornecer. Se a instância limitada passar para N1, retirar só este encapsulamento duplicado. O nome e a assinatura do novo contrato devem ser declarados no patch e cobertos por testes; ainda não existem no código.

**Não aceitar uma implementação parcial** que só acrescente `answers[host]` a `done()`. Demonstrar: timeout seguido de sucesso aproveitado; mudança DNS/rede com tarefa pendente; conclusões fora de ordem; erro/vazio; saturação do executor. Não cancelar o resolver nativo por promessa nem conservar endereços antigos indefinidamente.

#### P05 — Transporte seletivo: nenhuma alteração aprovada neste momento

**Não alterar** `retryOnConnectionFailure` em N1/N5 ou o número de tentativas em E1/E8 nesta fase. Não adicionar opção de retry às definições.

Apenas após o ensaio da fase 6A, os pontos candidatos são N1 e a criação de pedidos em E1 `httpDataSourceFor`, onde já existem tags de sintonia/âmbito. Falta demonstrar classificação de pedido e repetição segura depois de envio parcial; uma etiqueta «GET» não basta. Não está aprovado criar mais uma camada de reconexão.

#### P06 — Eliminar conclusão indevida de defeito do host

**Alterar E1**, `noteSegmentRefusal`, e **E6**, caminho `rememberSegmentRefusal`/`refusesSegments`:

- **Remover** a promoção automática de duas recusas distintas de segmentos de um canal para uma conclusão geral sobre o host. A memória automática futura não pode afetar outros canais/contas só porque partilham o domínio.
- **Preservar inicialmente** o sinal limitado à sintonia, caso os testes mostrem que ainda é necessário para a escada existente. Se também precisar de ajuste, limitar esse ajuste ao mesmo proprietário e orçamento; não adicionar refresh paralelo.
- **Alterar** texto/log que apresenta «signed token» ou incompatibilidade como causa demonstrada. Guardar código, fase e observação; não inventar a causa do fornecedor.
- Antes de remover um método/mapa de E6, verificar todas as referências. Retirar apenas o código que ficar realmente sem uso por esta mudança; não apagar outras aprendizagens HLS/DASH/arquivo.

**Alterar E7 e E8 somente onde consomem essa conclusão**, atualizando mensagem/consulta correspondente. Preservar `watch`, `watchExo`, gates de `play` e preferências Exo/apenas HLS/exceções manuais.

**Não alterar:** filtro `C.DATA_TYPE_MEDIA` em E1 `onLoadError`, que já existe; sucesso de segmento continua a limpar a sequência de recusas. Não tratar chave/playlist como segmento, não agrupar 403/404/410 como causa única e não trocar de formato contra a opção do utilizador.

#### P07 — Medir com os eventos existentes

**Alterar E1**, `eventListenerFactory`, `withSourceDiagnostics` e os callbacks `onLoadCompleted`/`onLoadError`:

- **Adicionar** tempos de fila nos callbacks OkHttp `dispatcherQueueStart`/`dispatcherQueueEnd` e observação de `retryDecision`, com as assinaturas da versão 5.5.0 instalada.
- **Adicionar** resumo de conclusão Media3: tipo de load, duração e bytes; distinguir resposta/cabeçalhos de transferência completa. Correlacionar com engine/sintonia/fonte e papel do pedido.
- **Reutilizar** timestamps e IDs existentes; manter contador barato separado da construção de strings e escrita detalhada. Não introduzir relógio periódico novo.
- **Remover/renomear** apenas métricas cujo nome faça passar tempo do interceptor por tempo de download do segmento.

**Preservar:** limites dos escritores/rings e redação. Os callbacks HTTP não consultam o Player. Se faltar um campo no resumo em memória, acrescentá-lo ao objeto de diagnóstico existente depois de verificar o seu proprietário; não criar mais um serviço global de telemetria.

#### P08 — Observar falhas de áudio posteriores ao primeiro avanço

**Alterar E9**, `AudioWatchdog`: reutilizar `onAudioInputFormatChanged`, `onAudioDecoderInitialized`, `onAudioPositionAdvancing`, `onAudioTrackInitialized`, `onAudioTrackReleased`, `onAudioUnderrun`, `onAudioSinkError` e `describe`.

- **Adicionar** correlação temporal das ocorrências após o arranque e identidade da saída atual; não perder a informação por `advancing` já ter ficado verdadeiro.
- **Adicionar** apenas estado/resumo limitado para diagnóstico. Libertação de saída antiga não pode apagar a configuração da saída nova.
- **E1:** associar `onAudioCodecError` e o resumo à sintonia correta; aproveitar o percurso de diagnóstico já existente.

**Não alterar** retorno de `poll`, `raise`, `canRecoverUnderrun`, latch Stereo ou `recoverCurrentPlayback` com os novos sintomas. Nenhum evento novo desta proposta pode parar, reabrir ou trocar o áudio/canal. Relógio a avançar e ausência de underrun não garantem som audível.

#### P09 — Buffer PCM: nenhuma preferência nova nesta fase

**Não alterar E9 `buildAudioSink` agora.** 750/1000 ms são valores de ensaio condicionado, não novos defaults nem uma solução confirmada.

Se a fase 6B for autorizada após evidência, a aplicação técnica fica nesse ponto de construção do sink; qualquer UI exige então mapear explicitamente persistência, override e backup. Os nomes dessas preferências e ficheiros de UI **não estão definidos nem aprovados neste documento**. Não presumir que `SettingsRepository` já possui essa opção. Não tocar no vídeo ou passthrough para testar PCM.

#### P10 — Fixar o target da apresentação local

**Desenho candidato: exige a paridade da secção 16.7 antes de qualquer rejeição nova.**

**Alterar T2**, `playlist`/`loadSnapshot`/início de `capture`:

- **Separar** validação/seleção do target local da validade estrita da tag upstream, usando o texto já descarregado e durações válidas. Testar tag ausente, subestimada, zero/negativa e não canónica antes de decidir rejeição ou derivação local. Não fazer outro pedido nem confundir fallback do parser com tag original. Não rejeitar um stream antes utilizável só para implementar target local fixo.
- **Adicionar** transmissão desse inteiro positivo e estável a T1 antes do primeiro `commit` publicável. Se for necessário um método de inicialização de apresentação, declará-lo como API nova e impedir segunda inicialização divergente.

**Alterar T1**, `playlist` e `commit`:

- **Remover** o cálculo de `EXT-X-TARGETDURATION` a partir do maior segmento que ainda está na janela.
- **Adicionar** target fixo à apresentação e validar cada duração pelo arredondamento exigido por HLS antes de publicar.
- **Preservar** target durante eviction/rotação, sequences, descontinuidades, leases de leitores e quota. Não substituir por máximo histórico crescente.
- Violação posterior: não publicar o novo segmento; preservar os já válidos e sinalizar falha controlada através da sessão. Não reiniciar silenciosamente o mesmo URL com target diferente.

**T3, apenas verificar:** antes da preparação continuar a devolver o estado transitório existente; manifesto pronto conserva target/sequência válidos. Sem alteração global de T5, sem migração e sem mexer nas gravações.

#### P11 — Identificar limitação AES local sem bloquear conteúdo antes aceite

**Desenho candidato: não integrar recusa antecipada só por tag de chave sem provar paridade com os casos antes aceites, segundo a secção 16.7.**

**Alterar T2**, identificação da chave em `loadSnapshot` e, decisivamente, elegibilidade em `capture` antes de `download`:

- **Adicionar** deteção explícita de AES-128 nos segmentos novos/elegíveis que seriam realmente transferidos, depois de excluir sequências antigas e GAP. A condição atual `!media.isEncrypted` não representa essa limitação completa, mas `media.segments.any { it.key != null }` também seria demasiado ampla. Não usar só o `encryptionMethod` histórico; testar AES→NONE, chave em GAP e segmentos antigos.
- **Representar** a limitação antes do download cifrado. Reutilizar o erro/texto local existente se suficiente; novo tipo/mensagem só se necessário. Não tornar um tipo público novo requisito desta correção.
- **Preservar** reprodução HLS direta e gravações. Não mudar globalmente `HlsMediaPlaylist.isEncrypted` em T5, nem chamar T7 neste lote mínimo.

**Alterar A1**, `watchWithLocalTimeshift` e respetivo tratamento de falha: reconhecer esse motivo, aguardar `disposeLocalTimeshift`/fecho e libertação do claim, e explicar como voltar ao direto. A seleção normal do canal já oferece esse percurso; **não acrescentar diálogo/botão novo nem abrir direto automaticamente em paralelo** só por causa desta correção.

**R3/R4:** alterar `local_timeshift_failed` apenas se o motivo específico não puder ser expresso corretamente com o texto existente; se for criada chave específica, identificá-la como nova e manter o erro genérico para os outros casos. Hoje o texto já refere HLS não cifrado e seleção normal para voltar ao direto.

**P11b permanece excluída da fase 4:** não implementar download de chaves, IV, `.clear` ou suporte AES completo. A fase 6D exige orçamento para ficheiros cifrados/claros e testes próprios; T7 existir não prova integração segura.

#### P12 — Usar capacidade real sem criar dependência circular

**P12 é testar primeiro, não criação obrigatória de ponte.** T4 já tem `supports(variant, capability)` e `deviceSupports`; não duplicar callback/mapeamento. `HlsVariantSupport` é `internal`. Só se uma variante recusada tocar efetivamente no leitor local, avaliar fachada pública mínima no mesmo ficheiro. Não tornar todo o objeto público nem expor APIs player no Core. **Preservar T6**, vídeo, áudio separado, metadata desconhecida e seleção estável.

**Alterar E12** para expor uma pequena ponte pública de capacidade efetiva ao chamador App. `FfmpegAudio` é hoje `internal`; A1 não pode simplesmente chamar esse objeto de outro módulo. A ponte nova deve usar `available && supports(mime)` e respeitar a seleção/política de áudio efetiva. Não tornar toda a implementação interna pública.

**A1 é ponto candidato após prova**, na construção de `LocalTimeshiftSession`: `variantSupported` já existe. Atenção: E8 `startLocalHls` chama `start(..., localHls = true)`, que atualmente faz `channelOptions = null`. Não assumir que o leitor local recebe overrides originais do canal. A futura ponte deve refletir a configuração real desse leitor; preservar overrides ali exigiria decisão adicional. Não criar DI circular nem importar objetos internos entre módulos.

**Remover apenas** a exclusão indevida quando houver suporte efetivo demonstrado e permitido. Se não houver suporte FFmpeg daquele MIME ou a política não o permitir, conservar recusa nativa. Sem alteração de decoder de vídeo, sem novo codec anunciado como suportado por simples presença da biblioteca.

#### P13 — Corrigir estimativa; deixar a rota real separada

**Alterar E10**, `AudioRouteSync.read`, ramo Android API 33+: a expressão com `getAudioDevicesForAttributes(... ) ... to false` deve passar a marcar a observação como estimada (`true`). Essa API descreve rota prevista, não o AudioTrack efetivamente em uso.

**Preservar** família escolhida, `Correction.ms`, offsets guardados, escolha manual, observação API antiga e aplicação atual do atraso. Uma mudança de etiqueta não deve alterar o som.

**E11/A2:** atualizar representação apenas se ainda ocultar essa distinção; não é obrigatório redesenhar o diálogo. **P13b condicionada:** não introduzir já provider moderno, listener de AudioTrack, reflexão ou nova política de offsets. Isso exige um ensaio separado da fase 6C.

#### P14 — Recuperação em execução: não implementar por inferência

**Sem alteração funcional agora** em E9/E1/E8. Fallback de inicialização não é recuperação de erro a meio da reprodução. Os novos diagnósticos P07/P08 não podem disparar esta proposta implicitamente.

Só a fase 6E poderá definir uma ação e orçamento únicos, após provar a falha e a API disponível. Se precisar de nova sintonia, tem de passar por E8 e respeitar cooldown/ownership. Não há aprovado nenhum `reset`, novo job HTTP ou troca automática de codec com base em ruído, silêncio ou underrun isolado.

#### P15 — Mostrar separadamente origem e saída efetiva

**Alterar E9**, `onAudioTrackInitialized`/`onAudioTrackReleased`: expor um snapshot imutável e limitado da configuração de saída já recebida (`AudioTrackConfig`), associado à saída atual. Nenhum novo polling ao hardware é necessário.

**Alterar E11**, `StreamInfoValue.AudioOutput` e `displayText`, apenas para dados necessários do snapshot: encoding/frequência/máscara configurados no AudioTrack, não prova do sinal físico recebido pela TV/soundbar. Campo desconhecido fica ausente. Preservar `Audio` como origem. `estimated=false` também pode vir de escolha manual; não o traduzir automaticamente como rota física confirmada.

**Alterar E1**, construção de `StreamInfoValue.AudioOutput`, para fornecer o snapshot; manter `p.audioFormat` na linha de origem. **A2, R1/R2:** adaptar texto compacto/detalhado apenas onde os novos dados o exigirem, com recursos localizáveis. Não acrescentar texto fixo técnico em português ao Kotlin.

**Não alterar:** frequência do AudioTrack, canais, codec, mixer, volume ou atrasos. Uma origem de 24 kHz e uma saída de 48 kHz podem ser conversão válida; o diagnóstico não deve acusar falha só por isso.

#### P16 — Reduzir captura do parser SDR sem perder entradas especiais

**Adiar até medir custo com Preferir SDR ligado.** Só perante ganho útil avaliar alterações a E13 `ParserFactory.createPlaylistParser(master, previous)`/`wrap`/`Capture`:

- **Remover** captura/cópia de playlists de segmentos quando comprovadamente só se precisa de as entregar ao parser Media3.
- **Preservar** captura limitada e filtragem da multivariante necessária para Preferir SDR; preferência desligada conserva o percurso original.
- Não assumir que o overload com `master` torna toda a entrada futura uma media playlist: cobrir entrada inesperada e preservar comportamento seguro. A técnica concreta de separação deve ser provada pelos testes; não fazer retorno direto sem essa prova.

Sem mudanças em definições, HDR global, timestamps ou seleção de codecs. Medir redução de alocações; não apresentar como solução dos cortes de áudio.

#### P17 — Explicar limites sem mudar a política de buffer

**Alterar E11**, `StreamInfoValue.LiveBuffer`/`HlsSegments` e `displayText`, **apenas se faltar informação/clareza**. Os campos de duração de segmentos, limite de bytes, carregamento e limiar superior já existem: reutilizá-los.

**Alterar R1/R2** nas chaves existentes como `player_buffer_maximum`, `player_hls_target_duration` e textos relacionados; **R5/R6**, `ui_buffer_limits`, se o resumo continuar a prometer um teto rígido. Explicar compactamente que a descarga de um segmento pode ultrapassar o limiar temporal e que a memória pode limitar a reserva.

**A2:** só ajustar agrupamento/compactação necessário para não cortar o conteúdo. Não reintroduzir textos longos que o utilizador já pediu para reduzir.

**Não alterar** `LiveBuffer` em `core/settings/LiveLatency.kt`, load control, limites de RAM, valores guardados, pré-buffer ou retoma. Esta proposta não acrescenta outro modo Automático nem novos tempos nas definições; essas opções não fazem parte desta aprovação informativa.

### 13.4. Testes existentes e testes novos — sem confundir os dois

Bases absolutas para resolver os nomes desta tabela:

- **TP** = `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player`.
- **TN** = `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/network`.
- **TT** = `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/timeshift`.
- **TA** = `C:/Users/renat/Downloads/OwnTV-main/app/src/test/java/tv/own/owntv/player`.

| Propostas | Ficheiros de teste existentes a estender/executar | Cobertura necessária, ainda não declarada como concluída |
| --- | --- | --- |
| P01/P02 | TP/`ProviderRetryGuardTest.kt`, `ResponseDeadlineTest.kt`, `PlaybackHttpCallsTest.kt`, `MixedHlsHostTest.kt`, `SourceDrainTest.kt` | Corpo lento/grande, preservação de recusa e 416/EOF, sniff funcional, fecho/cancelamento/retirada com DataSource real. |
| P03 | Não foi encontrado teste dedicado `StreamingHttpClientTest.kt` no inventário verificado. | NOVO PROPOSTO em TN: `StreamingHttpClientTest.kt`; isolamento do dispatcher/configuração. Ensaio controlado de fila separado. |
| P04 | TN/`BoundedDnsTest.kt`, `DnsConfigHolderTest.kt` | Conclusão tardia, geração/configuração/rede, ordem, limites e ausência de respostas antigas. |
| P06/P07 | TP/`LiveStreamQuirksTest.kt`, `LiveTuneControllerTest.kt`, `HlsOnlyPolicyTest.kt`, `PlaybackSessionDiagnosticsTest.kt` | Isolamento entre canais, gates e eventos da sintonia certa; validar fila/loads em integração. |
| P08/P13/P15 | TP/`AudioRecoveryTest.kt`, `AudioTrackRetentionTest.kt`, `AudioDelayClockTest.kt`, `AnnouncedAudioInfoTest.kt`; TA/`StreamInfoSummaryTest.kt` | Observação sem recuperação nova, saída antiga/nova, dados origem/saída e representação compacta. `AudioDelayClockTest` não prova por si a rota Android ativa. |
| P10/P11 | TT/`LocalSegmentStoreTest.kt`, `LocalTimeshiftServerTest.kt` | Target fixo após rotação, origem inválida, segmento incompatível e AES rejeitado antes do download; provar libertação da sessão/claim na integração. |
| P12 | TT/`HlsVariantSupportTest.kt`; TP/`SoftwareAudioDecoderPolicyTest.kt` | Matriz nativo/FFmpeg/MIME/definição efetiva, callback injetado e restrições estruturais preservadas. |
| P16 | Não foi encontrado `SdrVariantsTest.kt` no inventário verificado. | NOVO PROPOSTO em TP: `SdrVariantsTest.kt`; equivalência, variáveis/content steering, input inesperado e ausência de captura desnecessária. |
| P17 | TP/`HlsSegmentDiagnosticsTest.kt`, `LiveBufferTest.kt`; TA/`StreamInfoSummaryTest.kt` | Texto corresponde às medições; matemática e política de buffer anteriores continuam iguais. |

**Outros testes NOVOS PROPOSTOS, só quando necessários à fase:** TP/`BoundedPlaybackErrorBodyTest.kt` para posse/fecho do adaptador; TT/`LocalTimeshiftSessionTest.kt` para rejeição/fecho do produtor; TP/`AudioRouteSyncTest.kt` para a classificação estimada; TP/`EffectiveHlsVariantSupportTest.kt` para a ponte de capacidades. Estes nomes não significam que os testes já existam ou tenham passado.

Para DataSource/API Android, escolher teste de integração com dependências reais da versão usada. Se o runner JVM disponível não reproduzir o comportamento Android, acrescentar o teste no source set `androidTest` do módulo correspondente e registar execução pendente; não declarar aceitação só porque compila. Não alterar configuração de testes/bibliotecas sem demonstrar essa necessidade.

### 13.5. Ficheiros que não devem ser alterados por arrastamento

- `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/settings/SettingsRepository.kt`: nenhuma nova preferência nas fases aprovadas. P09 exige decisão posterior.
- `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/settings/LiveLatency.kt`: política de buffer preservada por P17.
- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioDelayClock.kt`: não retirar o clock para adotar outra saída.
- `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsAes.kt`: sem integração AES completa na fase 4.
- Ficheiros de backup, migrações de DB, versões de dependências, verificação Gradle e lint: não há mudança necessária identificada neste plano.
- Nenhuma nova política TLS, cache genérica de playlists, purge global de rede ou espera fixa universal está aprovada.

### 13.6. Fecho de cada subfase e manutenção do contexto

1. Registar na própria subfase quais ficheiros da tabela foram realmente alterados e o motivo. Uma referência de consulta não conta como ficheiro a modificar.
2. Executar os testes dirigidos com seleção e resultado explícitos. Distinguir falha anterior, regressão, teste não executado e aceitação física pendente.
3. Havendo alteração Core validada, gerar o snapshot pelo mecanismo existente `C:/Users/renat/Downloads/OwnTV-main/tools/core_snapshot/snapshot_core.py`, depois validar a App com o novo lock. **Não editar manualmente** `C:/Users/renat/Downloads/OwnTV-main/core-source-lock.json` para fingir equivalência, nem substituir Core por upstream.
4. Guardar hash/snapshot de entrada e saída e rollback por ficheiro; preservar alterações alheias. Não fazer reset ao checkout personalizado.
5. Só marcar a subfase concluída quando cumprir o critério definido. Um teste de corpo HTTP não prova correção do som; um teste de áudio não prova libertação remota da sessão IPTV.
6. **Estado desta revisão:** apenas o plano foi detalhado. Nenhuma alteração de reprodução, teste de correção, novo snapshot ou APK é entregue por esta edição documental. A próxima execução funcional continua a começar pela fase 0 e subfase 1A, quando pedida.

## 14. Adenda executável — lacunas da revisão de 05/11/2026

### 14.1. Autoridade, estado e referências adicionais

Esta secção concentra a localização e o âmbito das alterações propostas no complemento `C:/Users/renat/Downloads/OwnTV-main/docs/COMPLEMENTO_LACUNAS_STREAMING_CODECS_COUNCIL_2026-11-05.md`. O complemento conserva a análise e a fundamentação; esta secção é o mapa de execução correspondente. Aplicam-se as regras da secção 13.1: reler código e chamadores, preservar alterações existentes e distinguir ficheiro existente de nome proposto.

**Estado: tudo por implementar nesta adenda.** O pedido atual autoriza documentar e analisar, não alterar a reprodução. As linhas são apenas orientação da revisão; os nomes de função são as âncoras. Se o código mudar, não aplicar por semelhança.

Reutilizar E1–E13, N1–N5, T1–T7, A1–A3 e R1–R6 da secção 13.2. Referências adicionais reais:

| Ref. | Caminho absoluto existente |
| --- | --- |
| X1 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt` |
| X2 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/StreamHeaders.kt` |
| X3 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/PtsFrameRate.kt` |
| X4 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/FpsSample.kt` |
| X5 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioDelayClock.kt` |
| X6 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ffmpeg/FfmpegAudioDecoder.java` |
| X7 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ffmpeg/FfmpegAudioRenderer.java` |
| X8 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/StereoPcmAudioProcessor.kt` |
| X9 | `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/player/MpvVideoSurface.kt` |
| X10 | `C:/Users/renat/Downloads/OwnTV-main/tools/performance/local_iptv_fixture.py` |
| X11 | `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ExoSubtitleEngine.kt` |

X10 já cria HLS sintético com ffmpeg e serve ficheiros por HTTP em loopback. Não criar outro servidor estático idêntico nem apresentar a ferramenta como inexistente. A evolução N24 é acrescentar cenários determinísticos e asserções, não criar do zero toda a infraestrutura.

| ID | O que fica autorizado tecnicamente no plano | O que não fica decidido |
| --- | --- | --- |
| N18 | Corrigir montagem dos headers em três percursos App | Alterar codecs, TLS, retry ou transporte inteiro |
| N19 | Invalidar FPS de formato anterior e rearmar medição limitada | Forçar HDMI/AFR em cada segmento ou criar nova sintonia |
| N20 | Restringir evidência que ativa o workaround mpv | Novos valores temporais sem fixture ou nova política global de sincronismo |
| N21 | Diagnóstico agregado de atraso do vídeo | Reinício/fallback automático com base nessa métrica |
| N22 | Ensaios reais do ajuste A/V | Modificar fórmula/clock sem falha reproduzida |
| N23 | Ensaios de mudanças áudio/layout | Alterar FFmpeg/JNI por hipótese |
| N24 | Ferramenta local de desenvolvimento e regressão | Servidor permanente na App ou uso automático do fornecedor |
| R01 | Corrigir serialização mpv depois de provar o formato da API instalada | Retirar o filtro de vírgulas e concatenar dados sem escaping |
| R02 | Investigar necessidade de cookies por sessão | CookieJar global ou nova persistência de cookies |
| R03 | Testar edit lists em MP4 progressivo | Retirar flags globalmente ou alegar correção HLS |

### 14.2. N18 — Headers coerentes: três alterações em A1

**ALTERAR apenas A1 `LiveViewModel.kt` nos pontos abaixo; X2 é contrato já existente, não nova funcionalidade.**

1. `watchWithLocalTimeshift` (~351): substituir a origem de `headers`, atualmente `StreamHeaders.decode(channel.httpHeaders)`, por descodificação do resultado de `StreamHeaders.forSource(source, channel.httpHeaders)`. Manter o bloco de User-Agent que respeita o valor explícito do canal. Passar estes headers apenas ao produtor upstream T2; não ao leitor do loopback.
2. `playCatchupProgramme` (~2623/2641): obter e conservar a fonte completa no bloco IO onde hoje só se obtém `sourceUa`. Passar `source?.userAgent` e `StreamHeaders.forSource(source, ch.httpHeaders)` a `player.play`. Preservar os checks de `CatchupOwner` depois da suspensão e a política existente se a fonte desaparecer.
3. `loadArchiveStream` (~2827/2856): o bloco IO já carrega `source`, mas devolve apenas `archive` e `source.userAgent`. Conservar também os headers efetivos, ou a fonte imutável da mesma resolução, e fornecê-los a `player.play`. Não acrescentar uma segunda leitura independente da fonte depois da libertação do leitor antigo.

**REMOVER:** apenas a montagem incompleta nesses três pontos. Não retirar checks de geração, `releaseAndAwaitArchive`, `disposeLocalTimeshift`, claims, cancelamento ou exclusão Stalker.

**ADICIONAR:** prova do pedido efetivo no percurso local e nos dois percursos de arquivo. Não criar outro construtor de headers. X2 `forSource` já preserva Referer do canal sobre o da fonte e mantém o transporte Stalker separado.

**PRESERVAR:** direto normal (~2145) e diferido externo (~2579), que já usam `forSource`; UA explícito, restantes headers e políticas por fonte/canal. Não alargar compatibilidade TLS por arrastamento.

**TESTES existentes:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/network/SourceStreamHeadersTest.kt` e `C:/Users/renat/Downloads/OwnTV-main/app/src/test/java/tv/own/owntv/features/live/CatchupRequestsTest.kt`. O segundo testa pedidos/ownership do guia, não prova sozinho a identidade HTTP enviada pelo player.

**NOVO PROPOSTO, se faltar ponto testável de integração:** `C:/Users/renat/Downloads/OwnTV-main/app/src/androidTest/java/tv/own/owntv/features/live/ArchiveStreamIdentityTest.kt`. Nome futuro, não teste existente. Servidor sintético exige Referer e observa override, fontes diferentes e Stalker. Não usar credenciais reais nem exigir que a fonte atual tenha um erro 403 para aceitar a correção.

### 14.3. N19 — Invalidar a medição do formato antigo em E1

**ALTERAR E1** `onVideoInputFormatChanged`, `displayFps`, `restartFpsMeasurement` e apenas o estado de medição necessário:

- Identificar mudança relevante do formato/cadência da sintonia atual; excluir evento sem ownership. Não tratar todos os eventos de bitrate como nova cadência.
- Limpar `measuredPtsFps` e resetar X3 `ptsFrameRate` quando a medição anterior deixar de representar o novo formato.
- Limpar também o fallback obsoleto X4 `fpsSample.lastFps` nesse caso, usando a API existente `resetAll`. `restartFpsMeasurement` atualmente chama apenas `resetWindow`, que conserva a leitura antiga; invocá-lo sozinho **não corrige** o caso.
- Rearmar a janela limitada se `measuredStatsEnabled || autoFrameRateEnabled`, aproveitando `fpsFastRefresh`, `FPS_MAX_ATTEMPTS` e ownership existentes. Enquanto desconhecido, usar declaração válida do formato ou null; não publicar a medição anterior como se fosse nova.

**REMOVER:** apenas a sobrevivência de leituras do formato anterior. Não retirar a prioridade do FPS medido válido, o reset de sintonia ou as proteções de stop/release.

**X3/X4, preservar primeiro:** já disponibilizam `reset`/`resetAll`; não alterar o algoritmo de medição sem um teste que demonstre necessidade. **X9 apenas verificar:** já consome `_videoFps`; não é necessário reescrever AFR nem provocar nova sintonia. **X11 é precedente de consulta**, não alvo de refactor.

**TESTES existentes:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/UpstreamStageBTest.kt`, `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/FpsSampleTest.kt`, `C:/Users/renat/Downloads/OwnTV-main/app/src/test/java/tv/own/owntv/player/FrameRateChooseModeTest.kt`.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveFormatCadenceTest.kt`, se os testes existentes não exercitarem a invalidação do engine. Cobrir 25↔50, fracionário, declaração ausente, mesma cadência/bitrate diferente, leitura antiga e evento tardio. Testar chamadas HTTP/HDMI separadamente na integração; um teste do medidor não prova o caminho AFR inteiro.

### 14.4. N20 — Critério de timestamps mpv em X1

**ALTERAR X1** `BROKEN_PTS_RX`, tratamento de log que chama `noteBrokenTimestamp`, `noteBrokenTimestamp` e o estado correspondente de `LoadState` (tipo real definido no fim de X1; o teste chama-se `PlayerResetStateTest`, o que não muda o nome do tipo):

- Separar reconhecimento diagnóstico de prova que autoriza desligar o sincronismo. Correspondência em `desynchroni` ou `non-monotonic` sem contexto de vídeo não basta para aprender PTS de vídeo quebrado.
- Acrescentar timestamps monotónicos limitados da evidência elegível, associados à geração/sessão atual; expirar ocorrências esparsas e resetar após intervalo saudável.
- Avaliar a ação uma única vez por carga, preservando a proteção equivalente a `brokenPtsHandled`.
- Só após critério válido chamar a aprendizagem e a alteração de propriedades já existentes. Preservar o cenário de feed realmente patológico.

**REMOVER:** a suficiência de `++load.brokenPtsHits >= BROKEN_PTS_HITS` como contador sem janela e a decisão tomada só por mensagem genérica. Não remover todos os logs nem o workaround inteiro.

**Não inventar os valores de janela/limiar:** são parâmetros internos a justificar com fixtures antes de integrar a ação. O plano não cria slider novo nas definições. Se não houver cenário patológico reproduzível, manter a alteração comportamental pendente e concluir apenas classificação/testes.

**E6, preservar primeiro:** `rememberBrokenTimestamps`/`hasBrokenTimestamps` são por stream. Não convertê-los em regra de host nem apagar outras aprendizagens. Se o helper de janela ficar separado, **NOVO PROPOSTO** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/BrokenVideoPtsEvidence.kt`, somente para tornar esse critério testável; não é serviço global nem ficheiro existente.

**TESTES existentes:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveStreamQuirksTest.kt` e `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/PlayerResetStateTest.kt`. O teste atual aceita a mensagem genérica como regex; ao separar diagnóstico/ação, atualizar o que cada assert comprova. Não conservar um assert que obrigue uma mensagem genérica a ativar a recuperação.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/BrokenVideoPtsEvidenceTest.kt`. Cobrir densidade real, avisos espaçados, áudio genérico, mudança de geração, reset, ação única e aprendizagem só do stream correto. Este achado não é uma explicação dos cortes ExoPlayer.

### 14.5. N21 — Diagnóstico agregado em E1

**ADICIONAR em E1 `analytics`** o callback Media3 existente `onVideoFrameProcessingOffset(EventTime, totalProcessingOffsetUs, frameCount)`, respeitando `owns(eventTime)`. A API foi verificada nos sources da versão 1.11.1; não é método a inventar no Player.

Guardar resumo limitado por sintonia/formato/decoder. Só calcular média com `frameCount > 0`; preservar o sinal e indicar unidade. Valor negativo significa disponibilidade tardia face ao prazo previsto, não um diagnóstico automático de rede lenta. Resetar a agregação com a sintonia e com mudança relevante do formato, em coordenação com N19.

**REMOVER:** nada no percurso de reprodução. **PRESERVAR:** opção de diagnóstico, redação, limites dos rings/escritores e P07/P08. Não adicionar `VideoFrameMetadataListener` paralelo, timer, leitura do Player em thread HTTP ou UI nova obrigatória.

Contadores de frames ignorados/consecutivamente perdidos só entram se o objeto de contadores da versão efetiva os fornecer e a leitura já ocorrer num ponto seguro; não criar polling por fotograma para os obter. E11/A2 só necessitam de alteração se houver autorização explícita para mostrar novos campos; logging agregado basta para esta proposta.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/VideoProcessingSummaryTest.kt`, se for extraído um pequeno agregador. Testar contagem zero, sinal/unidade, reset, evento antigo e limitação de logs. Não atribuir ao teste prova de apresentação física da TV.

### 14.6. N22 — Testes do relógio A/V; não alterar X5

**ALTERAR apenas testes de integração, não código funcional nesta proposta.** X5 `shift`/`DelayedClockAudioSink.getCurrentPositionUs`, E9 construção do sink e offsets guardados ficam intactos até existir divergência reproduzível.

**PRESERVAR teste existente:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/AudioDelayClockTest.kt` continua a cobrir aritmética. Não o substituir por uma cópia da implementação nem afirmar que já cobre reprodução real.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/androidTest/java/tv/own/owntv/player/AudioDelayClockIntegrationTest.kt`. Usar media sintética A/V conhecida e player real para posição, pause/play, seek, fim, offsets 0/±150/±500 ms e velocidades permitidas nos diferidos. A verificação do som percebido/rota HDMI/Bluetooth exige dispositivo e fica registada separadamente das asserções automáticas.

**REMOVER:** nada. Não modificar fórmula, frequência, amostras PCM, vídeo por hardware ou volume. Se o teste encontrar falha, criar uma correção delimitada com o caso reproduzido; N22 não autoriza um redesenho preventivo do clock.

### 14.7. N23 — Testar formato/layout dinâmico; não alterar X6/X7/X8

**ADICIONAR fixtures e testes; manter o código funcional inicial.** Consultar X6 `hasOutputFormat`, X7 `getOutputFormat`, X8 e eventos de E9. A existência de metadata guardada após a primeira saída não prova falha: a mudança sinalizada pode provocar recriação correta do decoder.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/androidTest/java/tv/own/owntv/player/AudioFormatTransitionTest.kt`. Cenários AAC LC↔HE-AAC, 24↔48 kHz, estéreo↔5.1, PID/faixa e layout não padrão. Separar fluxo válido de fluxo deliberadamente mal sinalizado; o segundo deve produzir diagnóstico/erro controlado, não necessariamente reprodução perfeita.

Comparar MediaCodec e FFmpeg efetivamente disponíveis; mpv exige ensaio do seu próprio percurso, não outro teste do renderer Exo. Confirmar configuração PCM, ciclo de vida e centro/diálogo onde há referência multicanal.

**REMOVER:** nada. Não reiniciar o decoder por cada segmento, retirar `hasOutputFormat`, inventar máscara de canais nem alterar JNI sem fonte nativa correspondente ao binário. Se faltar esse material, declarar a investigação bloqueada para essa alteração específica, preservando o restante trabalho possível.

### 14.8. N24 — Estender a ferramenta que já existe

**ALTERAR futuramente X10**, `create`/`main` e handler HTTP, apenas para acrescentar perfis de cenário explícitos sem alterar o modo básico existente. O servidor atual usa `SimpleHTTPRequestHandler`; respostas com atraso/headers obrigatórios/erros exigem handler de ensaio próprio, não apenas ficheiros diferentes.

**ADICIONAR:** cenários determinísticos para as correções autorizadas, diário limitado de pedidos/fechos e referência de áudio/vídeo para N19/N22/N23. A–B–A simulado valida o protocolo do ensaio, não prova quando o fornecedor real liberta a sessão.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV-main/tools/performance/test_local_iptv_fixture.py`, se não existir à data da execução, para testar rotas, delays limitados, headers e fecho do servidor. Media gerada fica num diretório temporário/ensaio explicitamente escolhido; não incluir credenciais ou grandes binários na App.

**PRESERVAR:** bind loopback, diretório novo sem sobrescrever dados, encerramento em `finally`, necessidade explícita de ffmpeg para gerar media e possibilidade de servir fixtures já existentes. Ensaios físicos por ADB/instalação exigem âmbito próprio; não são executados por esta documentação.

Não copiar os scripts de auditoria de 03/11 para produção nem construir outro pipeline HLS. Não há novo Worker, Service, permissão de rede ou dependência Android necessária para este laboratório externo.

### 14.9. R01 — Serialização mpv: pendência antiga, não descoberta nova

**ALVO X2** `toMpvHeaderFields` e **X1** escrita de `http-header-fields` (~3582).

**ALTERAR:** preservar valores com vírgulas através de serialização de lista comprovadamente suportada pela versão libmpv instalada, ou API de lista/item já disponível. Antes de escolher o formato, verificar o binding real; não escrever uma chamada `setPropertyNode` inexistente por suposição.

**REMOVER o filtro** `.filter { !it.value.contains(',') }` **só depois** de o substituir pela representação correta. Concatenar valores crus não é uma correção. Preservar normalização, proteção CR/LF e limpeza dos headers da carga anterior, incluindo o caso de lista vazia.

**Teste existente:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/parser/M3uHeaderParsingTest.kt` verifica o descarte atual. Atualizar o contrato esperado apenas depois de existir serialização segura; acrescentar vírgula, barra invertida, aspas, valor vazio e vários headers. Teste unitário da string não substitui servidor local que confirma os bytes recebidos do mpv.

Sem API/formato validado, manter R01 pendente; não introduzir um escape inventado nem alargar TLS/cookies.

### 14.10. R02/R03 — Localização para investigação, sem alterações agora

**R02 cookies:** N1/N5 e E1 são pontos de consulta do cliente Exo; X1 é transporte distinto do mpv. Nenhum deles recebe CookieJar nesta fase. Só após fixture demonstrar cookie de resposta obrigatório definir contrato por fonte/conta/sessão, precedência manual, redirects e expiração. Não há preferência, ficheiro de armazenamento ou nome de classe aprovados. Não passar cookies de uma origem a outra por conveniência.

**R03 edit lists:** consultar E1 `httpDataSourceFor`, construção `DefaultExtractorsFactory` e as chamadas `setMp4ExtractorFlags`/`setFragmentedMp4ExtractorFlags`. **Não remover agora** `FLAG_WORKAROUND_IGNORE_EDIT_LISTS`. Comparar fixtures progressivas MP4 válidas/problemáticas antes de decidir sobre exceção por conteúdo; HLS usa outra fábrica e não pode ser declarado corrigido por esta alteração. Não criar opção por canal sem provar a necessidade.

### 14.11. Ordem, integração e ponto de paragem

- [ ] D1 — N18: montagem de identidade nos três chamadores, testes de requests e libertação preservada.
- [ ] D2 — N19: estado FPS, fallback antigo, janela limitada e integração AFR.
- [ ] D3 — N20: reproduzir falso disparo, classificar evidência e validar janela antes da ação.
- [ ] D4 — N21: agregação de diagnóstico coordenada com P07/P08, sem recuperação.
- [ ] D5 — N22/N23: testes de integração áudio; reportar hipóteses confirmadas/rejeitadas antes de alterar produção.
- [ ] D6 — N24: consolidar laboratório; R01 só com serialização real comprovada.
- [ ] R02/R03 — continuam condicionadas, não executar automaticamente ao concluir D1–D6.

Estas subfases complementam P01–P17; a execução de uma não autoriza todas as outras. Se a fase P07 já tiver instrumentação equivalente, estender o mesmo agregador em N21 em vez de duplicar listeners/logs. N19 não deve resetar os contadores de diagnóstico de outra sintonia. N18 não muda a posse do produtor local nem o âmbito TLS. N20 não passa a decidir sobre ExoPlayer.

**Critério documental:** nenhuma biblioteca, ficheiro de produção, funcionalidade ou preferência inteira está aprovada para remoção. «Remover» nesta adenda refere-se apenas aos comportamentos expressamente identificados. Novos nomes de testes/helpers são propostas, não evidência de existência. Conclusão de compilação, teste automático e aceitação física continuam a ser estados distintos.

## 15. Segunda revisão especializada — rede e produtor de gravações

### 15.1. Resultado, âmbito e ficheiros reais

Nova revisão apenas de leitura por três agentes: continuidade de faixas/definições, recuperação/ciclo de vida e parser/captura HLS. O agente principal cruzou os percursos e rejeitou duplicados. **Nenhum ficheiro de produção foi alterado, nenhum novo teste foi executado e nenhum APK foi gerado.**

| ID | Resultado | Decisão proposta |
| --- | --- | --- |
| N25 | Qualquer estado de rede online pode renovar o orçamento de uma sessão terminal | Corrigir o gatilho: restauro/transição real, sem reabrir por mudança só de metadados. |
| N26 | Booleano de captura perde a recusa/espera e permite pedir outro segmento | Corrigir contrato e chamador do produtor de gravação; não alterar HLS direto. |
| N27 | Captura de gravação pode continuar ativa sem novos commits até ao fim | Acrescentar ensaio/deteção de falta de progresso; ação e prazo dependem de fixture. |

N25 é relevante para reprodução Exo com sessão terminal ainda presente. N26/N27 pertencem a gravações/downloads que usem `RecordingEngine.recordHls`, **não ao HlsMediaSource do canal em direto**. Não atribuir os cortes Xiaomi a estes achados. O coordenador/UI pode já ter abandonado a sessão antes do evento N25; o problema não ocorre em toda a mudança de canal.

Referências adicionais, todas existentes na revisão:

| Ref. | Caminho absoluto |
| --- | --- |
| Y1 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/DefaultNetworkTracker.kt` — `ConnectivityState` e identidade/capabilities. |
| Y2 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsSegmentRetry.kt` — `HlsSegmentRetry.capture`, resultado e classificação. |
| Y3 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/RecordingAttemptPolicy.kt` — `awaitRetry` e classe `RecordingHttpFailure`. |
| Y4 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/RecordingEngine.kt` — produtor HLS e laço de tentativas. |
| Y5 | `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/recording/HlsCaptureCompleteness.kt` — proteção de integridade final já existente. |

### 15.2. N25 — Não confundir metadados de rede com restabelecimento

**Evidência:** E1 `init` que observa `connectivity.state` (~945) chama `onNetworkRestored` em qualquer emissão com `network.online`. Y1 `ConnectivityState` inclui `metered`; esse campo pode mudar com o mesmo ID e `online=true`. E1 `onNetworkRestored` (~958) pode limpar erro, zerar `retryCount` e reconectar se a sessão tiver reproduzido e estiver em `gaveUp`.

**ALTERAR E1 apenas no consumidor de conectividade:** guardar a observação anterior necessária para decidir se houve transição elegível. Distinguir queda/restauro observados e mudança real da rede padrão de alteração de custo/transportes/metadados na mesma rede. Replay inicial e leitura desconhecida não devem, por si, conceder novo orçamento.

**REMOVER:** chamada incondicional a `onNetworkRestored` só porque `online == true`. Não remover o observador nem a recuperação legítima.

**PRESERVAR em `onNetworkRestored`:** proprietário/suspensão, intenção de reprodução, `hasPlayed`, paragem intencional, canal atual e a decisão de não recuperar automaticamente a primeira abertura. Rede nova não autoriza reiniciar canal saudável nem ignorar espera do fornecedor. Não aumentar o número de tentativas, não alterar cooldown e não criar callback Android/timer paralelo.

**Y1/N4, apenas consultar/testar:** o estado já contém identidade e propriedades; não é necessário acrescentar sondagem síncrona a `NetworkCallback`. Se um pequeno helper de decisão for necessário, pode ficar junto do consumidor; o nome de uma API nova não é pressuposto neste plano.

**Testes existentes a preservar:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/network/DefaultNetworkTrackerTest.kt`. Esse teste valida o produtor do estado, não o orçamento de reconexão do engine.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveNetworkRecoveryTriggerTest.kt`. Cobrir mesmo ID online com `metered` alterado, replay inicial, estado desconhecido, offline→online, substituição A→B, canal saudável, sessão antiga, pausa/Stop/background e espera de fornecedor. Confirmar zero alteração de orçamento nos eventos inelegíveis.

### 15.3. N26 — Não reduzir espera do fornecedor a gap do segmento

**Evidência:** Y2 `capture` devolve `false` para atraso superior a `MAX_WAIT_MS` (10 s), erro terminal, expiração e esgotamento. Y4 `recordHls` (~775) interpreta qualquer `false` como `session.skip` e pode pedir imediatamente o próximo segmento. Exemplo de fixture: segmento 1 recebe 429/503 com `Retry-After: 15`; o helper não repete esse segmento, mas o chamador continua a sequência.

**ALTERAR Y2, âmbito reduzido:** preservar a espera explícita aplicável ao produtor em resultado interno distinto de sucesso/gap. Não é obrigatório redesenhar todas as classes de erro. Os restantes casos mantêm a semântica atual salvo fixture específica; verificar referências antes de mudar a assinatura. «Terminal para este pedido» não significa «terminal para toda a gravação».

**ALTERAR Y4 `recordHls`:** o resultado de espera aplicável não pode seguir `session.skip` e pedir imediatamente outro segmento. Conservar código/âmbito/prazo para adiar ou terminar a tentativa. Erro terminal isolado de segmento continua com o comportamento anterior até haver prova para o mudar; 403/404 não devem automaticamente reabrir a fonte inteira.

**REUTILIZAR Y3 quando adequado:** `RecordingAttemptPolicy.awaitRetry` e o laço externo conhecem espera. Não encaminhar indiscriminadamente erros de segmento como `RecordingHttpFailure`: Y4 converte essa classe em `STREAM_UNAVAILABLE` e pode reabrir até `stopMs`. Preservar deadline residual e orçamento; se a espera não couber na janela, terminar controladamente com o capturado preservado. Sem sleep global ou bloqueio de outras fontes.

**REMOVER:** perda da razão no contrato booleano/chamador, não a recuperação de gap válida. **PRESERVAR:** cancelamento, `stopMs`, validação de byte ranges/comprimento, rotação de chaves, fecho dos pedidos e Y5 integridade final. A classificação do âmbito da espera deve vir do pedido/serviço; um Retry-After não prova uma proibição universal para todos os hosts/contas.

**Teste existente:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/HlsSegmentRetryTest.kt` já cobre a recusa de espera excessiva para o mesmo segmento. Atualizar asserts ao contrato novo, conservando o cancelamento e datas HTTP. É obrigatório acrescentar teste do chamador com pelo menos dois segmentos.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/RecordingHlsBackoffIntegrationTest.kt`, usando transporte real se o runner o suportar, ou no `androidTest` correspondente se necessário. Medir timestamps dos pedidos; demonstrar espera respeitada, stop/cancelamento, gap normal e segmento 403/404 seguido de segmento saudável sem reabrir indevidamente a fonte.

**Deduplicação:** suporte a Retry-After nas gravações já tinha sido analisado em `C:/Users/renat/Downloads/OwnTV-main/docs/REVISAO_COUNCIL_APP_APOS_LIBMPV_2026.10.1.md`. A lacuna adicional é a perda do resultado na fronteira helper→produtor, não ausência total desse suporte.

### 15.4. N27 — Detetar gravação ativa sem progresso útil

**Evidência:** Y4 `recordHls` (~657–784) verifica fim agendado, cancelamento e ENDLIST; ignora sequências já capturadas, mas não tem prazo monotónico de falta de novos commits. Manifesto HTTP 200 repetido ou reset de sequência pode manter a captura ativa sem novos bytes até `stopMs`.

**Primeiro ADICIONAR teste e observação delimitada em Y4:** medir tempo desde progresso útil/commit, distinguir arranque de captura estável e registar manifesto parado/reset de sequência. Um novo HTTP 200, atualização de UI ou sequência ignorada não representa novo conteúdo capturado.

**Ação funcional condicionada à fixture:** prazo derivado da cadência validada e limites explícitos. O laço externo é limitado pelo fim agendado, não por um pequeno número fixo de tentativas; não o descrever como recuperação já finita por orçamento. Se houver ação futura, definir o orçamento e excluir espera intencional do fornecedor, pausa/download, offline conhecido e finalização/remux. Preservar spool e `lastSequence`; não anexar silenciosamente outra apresentação nem reabrir repetidamente até ao fim do programa por cada stall.

**PRESERVAR Y5 e finalização de Y4:** `HlsCaptureCompleteness.missingWindow` já pode marcar a gravação PARTIAL pela duração em falta. **Não afirmar que o estado atual marca sempre COMPLETED** nem substituir essa proteção por um novo sucesso presumido. Preservar operações parar/guardar, apagar e retomar.

**Testes existentes:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/HlsCaptureReliabilityTest.kt`, `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/RecordingIntegrityTest.kt` e `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/RecordingAttemptPolicyTest.kt`. Não são prova automática da nova condição sem a exercitar.

**NOVO PROPOSTO:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/recording/RecordingHlsProgressTest.kt`. Fixture com janela parada, janela legitimamente lenta, reset, gaps, cancelamento/stop e preservação dos segmentos. Verificar estado final parcial quando falta conteúdo, sem falsos positivos só por duração de segmento longa. Pode partilhar a infraestrutura N24/N26, mantendo cenários independentes.

### 15.5. O que esta ronda não promove a nova implementação

- Retenção de áudio ao trocar de motor já é pendência do council de 19/10; não contar novamente como inovação.
- Aplicação de decoder/áudio software ao Exo de diferido já ativo já consta como S1 na auditoria de 11/10. Verificar o contrato existente antes de adicionar reconstrução automática.
- Retenção semântica de faixa Live após `reprepare` com TrackGroup diferente fica como teste de baixa prioridade, apenas se houver seleção manual acessível. Não há nova persistência aprovada.
- Velocidade manual, áudio e legendas foram retirados de certos controlos TV. Não inventar um percurso ativo de utilizador para justificar resets novos.
- Surface detach, snapshots de background, intenção de pausa e espera do fornecedor já têm proteções. Não as substituir sem nova falha reproduzida.
- Parser manual de gravação e HlsMediaSource não são o mesmo componente. Variáveis/delta updates/LL-HLS no parser manual exigem amostra/fixture e decisão própria; não prometer suporte apenas por ignorar tags.

### 15.6. Integração e fontes

- [ ] D7 — N25: teste do gatilho de rede e correção localizada do consumidor.
- [ ] D8 — N26: contrato de resultado + produtor/coordenador + teste com vários segmentos.
- [ ] D9 — N27: fixture/observação; só depois decidir prazo e ação.

Nenhuma destas tarefas remove os critérios de P01–P17 ou N18–N24. D9 é trabalho no produtor de gravações, não mais um watchdog do player direto.

Fontes primárias: [Android: estado, identidade e capabilities de rede](https://developer.android.com/develop/connectivity/network-ops/reading-network-state), [RFC 9110: Retry-After](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.3), [RFC 8216: recarregamento de playlists](https://www.rfc-editor.org/rfc/rfc8216.html#section-6.3.4). O código local e os chamadores determinam o âmbito concreto; as normas não demonstram o comportamento do fornecedor do utilizador.

## 16. Decisão vigente — reanálise crítica de todas as 30 propostas

### 16.1. Âmbito e conclusão

Revisão em 05/11/2026 pelo agente principal e três especialistas, agora procurando falhas nas próprias recomendações em vez de procurar acrescentar funcionalidades. Foram reavaliadas P01–P17, N18–N27 e R01–R03: **30 famílias**, não toda a história de alterações da aplicação nem 30 bugs confirmados.

**Conclusão: não implementar o conjunto inteiro como pacote de otimização.** Há correções úteis, mas algumas propostas só acrescentam observação, outras exigem protótipo e várias podem piorar compatibilidade, consumo ou número de reconexões se forem aplicadas literalmente.

Classificação consolidada: **4 manter, 10 reduzir, 10 testar primeiro, 6 adiar**. Não há família inteira eliminada como necessariamente falsa; há formas de implementação expressamente rejeitadas na secção 16.5. Esta classificação substitui as aprovações anteriores demasiado amplas. As subpartes condicionadas continuam condicionadas mesmo quando a família contém uma correção mínima útil.

### 16.2. Decisão individual e risco a controlar

| ID | Decisão vigente | Parte útil e limite obrigatório / risco |
| --- | --- | --- |
| P01 | Reduzir | Retirar primeiro peek de sucesso só diagnóstico. O ramo de erro tem valor na mensagem ao utilizador; alterar com teste. P01b é protótipo com DataSource real e ownership/fecho, nunca integração automática. |
| P02 | Manter | Retirar leitura bloqueante do guard 408/429/503; preservar status/headers, loopback, posição e prazo de 8 s. Não é um refactor geral de erros/retries. |
| P03 | Testar primeiro | Medir fila assíncrona residual após P01/P02. Dispatcher separado pode aumentar threads/concorrência e não governa toda a leitura dos corpos ou chamadas síncronas. |
| P04 | Testar primeiro | Sucesso DNS tardio pode ajudar, mas não elimina o refresh atual. Provar benefício/contexto coerente antes de refactor em vários módulos; invalidar só por mudança relevante, sem tempestade por metadados. |
| P05 | Adiar | Retry automático pode repetir abertura de sessão já enviada. Sem classificação/limite comum demonstrados, conservar política atual. |
| P06 | Reduzir | Corrigir prosa/inferência e contaminação entre canais; não apagar a recuperação útil da sintonia. Testar host misto e host realmente problemático antes de mudar a aprendizagem. |
| P07 | Reduzir | Um resumo limitado por proprietário, sem nova infraestrutura. Guardar contadores baratos; construir strings frequentes só quando o detalhe é pedido. |
| P08 | Reduzir | Integrar observação áudio em P07, sem outro timer/writer e sem ligar sintomas novos ao retorno de poll/recovery. |
| P09 | Adiar | Buffer PCM diferente só perante underrun de saída demonstrado com media disponível. Não mudar Automático, passthrough ou defaults por hipótese. |
| P10 | Testar primeiro | Provar target local estável e política perante origem imperfeita sem rejeitar casos antes utilizáveis. Nenhuma nova validação é aplicada ao HLS direto. |
| P11 | Testar primeiro | Provar limitação local efetiva e paridade antes de recusa por tag de chave. AES no HLS direto e gravações mantêm os percursos atuais. |
| P12 | Testar primeiro | MIME no FFmpeg não garante perfil/layout/desempenho nem uso pelo leitor local. Demonstrar variante real e configuração efetiva; não assumir overrides que startLocalHls atualmente remove. |
| P13 | Reduzir | Corrigir a etiqueta de rota prevista. Escolha manual também não é rota física observada; provider/rota real fica condicionado e não deve alterar offsets. |
| P14 | Adiar | Recuperação áudio pode criar novas sessões/interrupções. Sem falha persistente e ação única testada, não introduzir novo watchdog nem fallback. |
| P15 | Reduzir | Mostrar origem e configuração AudioTrack conhecida. Não afirmar que frequência/encoding do AudioTrack prova sinal físico HDMI/TV. Evitar novos observadores e texto extenso. |
| P16 | Adiar | Com SDR desligado já não há captura extra. Ganho com SDR ligado tem de justificar o risco de alterar parser/entradas especiais. |
| P17 | Reduzir | Texto compacto sobre limiar, segmento e bytes. Não acrescentar modos/avisos repetidos nem alterar a política de buffer. |
| N18 | Manter | Corrigir os três chamadores de headers; preservar overrides, Stalker, ownership e separação upstream/loopback. Não migrar toda a rede. |
| N19 | Manter | Corrigir FPS obsoleto com teste do gatilho. Não resetar a janela de 4 s por toda mudança de bitrate/resolução nem provocar instabilidade AFR/HDMI. |
| N20 | Testar primeiro | Restringir avisos genéricos/espaçados, mas provar também o stream patológico que depende do workaround. Nenhuma janela nova é automaticamente correta. |
| N21 | Reduzir | Fundir métrica agregada de vídeo com P07/P08; não medir/logar por frame nem usar como prova de imagem física ou gatilho de fallback. |
| N22 | Testar primeiro | Validar clock A/V em reprodução real; começar por 1×, seek, pausa e saídas acessíveis. Não alterar fórmula nem reintroduzir controlos TV removidos. |
| N23 | Testar primeiro | Testar transições áudio sinalizadas; código upstream não é bug por si. Sem reprodução e fonte nativa correta, não alterar JNI/layout. |
| N24 | Reduzir | Estender a fixture existente só para o cenário necessário. Não construir laboratório enorme nem bloquear correções pequenas por falta dele. |
| N25 | Manter | Corrigir gatilho de rede e preservar restauro legítimo. Replay/metadados não renovam orçamento; rede nova não ignora pausa ou espera do fornecedor. |
| N26 | Reduzir | Conservar espera explícita aplicável ao produtor. Não promover todo 403/404 de um segmento a falha de toda a gravação/reabertura da fonte. |
| N27 | Testar primeiro | Observar progresso com fixture; excluir esperas legítimas e definir orçamento antes de qualquer ação. Finalização já pode marcar PARTIAL; não duplicar integridade. |
| R01 | Testar primeiro | Descarte de vírgulas existe, mas é preciso provar serialização/API libmpv antes de retirar o filtro. Concatenar cru pode corromper headers. |
| R02 | Adiar | Cookies automáticos só perante caso necessário; jar global mistura sessões e amplia superfície de segurança. |
| R03 | Adiar | Edit lists pertencem ao percurso progressivo identificado. Sem fixture, retirar workaround pode estragar conteúdo que hoje funciona e não corrige HLS. |

### 16.3. Correções de contexto ao desenho anterior

**A. HTTP — não confundir benefício conhecido com desenho provado.** E1/E2 podem retirar leituras de diagnóstico sem reconstruir o transporte. O adaptador P01b cria outra fronteira de posse do corpo: precisa de DataSource, cancelamento e retirement reais. Se texto remoto passar a estar vazio, a mensagem pode perder detalhe; conservar código/headers e não prometer paridade. Nunca marcar fecho antes da conclusão para satisfazer o drain.

**B. Dispatcher — medir depois da remoção das esperas.** O sources Media3 1.11.1 usa `enqueue`; o OkHttp liberta a vaga assíncrona após callback, e o loader continua a ler o corpo. Chamadas por `execute` não obedecem aos mesmos limites. O ensaio anterior demonstra congestionamento possível, não uma aceleração geral do streaming. Se P01/P02 resolverem a fila, não acrescentar P03 só para completar o plano.

**C. DNS — benefício e invalidação são duas provas distintas.** O lookup atual faz refresh mesmo com cache; guardar a resposta tardia não prova reduzir o número de consultas. Não atribuir à alteração esse resultado sem medi-lo. Geração e resolver têm de ser capturados coerentemente; alterar apenas `done` não basta, mas isso também não justifica automaticamente redesenhar vários módulos. Não invalidar por toda mudança de metered, transportes descritivos ou replay do observador.

**D. Diagnóstico — a opção desligada não elimina todo o custo.** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveDiagnosticsLog.kt`, `event`, ainda formata data e guarda ring em memória com detalhe desligado. Novos eventos frequentes P07/P08/N21 devem testar nível/necessidade antes de construir strings/contexto. Manter os eventos raros de falha e limites existentes; não desligar globalmente o ring para esconder o custo novo. Fundir o trabalho, não multiplicar pipelines.

**E. Timeshift — corrigir a apresentação local sem reduzir compatibilidade por acidente.** P10 não exige recusar qualquer target upstream imperfeito; primeiro testar a política de escolha local. A duração de EXTINF arredondada ao inteiro mais próximo tem de caber no target; uma comparação mais estrita pode rejeitar conteúdo válido. P11 só examina chaves dos segmentos novos/elegíveis antes de os transferir, preservando dados já capturados. Sem novo suporte AES completo por arrastamento.

**F. Capacidade — testar o leitor que realmente toca.** E8 `startLocalHls` atualmente remove `channelOptions` ao chamar `start`. P12 não pode demonstrar compatibilidade com definições que esse leitor não recebe. `FfmpegLibrary.supportsFormat` comprova presença de decoder MIME, não todas as configurações. Fazer passar overrides ao leitor local é uma decisão adicional, não um detalhe escondido da ponte.

**G. Sincronismo — configuração, seleção manual e sinal físico são factos diferentes.** AudioTrackConfig é configuração observada do AudioTrack. A rota de AudioManager é prevista; a escolha manual é preferência. Não as apresentar como análise do sinal recebido na TV/soundbar. Não introduzir provider moderno só para preencher o diálogo técnico, preservando Stereo/PCM16/mixer/offload/clock.

**H. Gravações — terminal do pedido não é terminal da captura.** N26 limita a alteração comportamental à espera explícita aplicável. Y4 pode reabrir até ao fim agendado; lançar RecordingHttpFailure para qualquer segmento terminal pode aumentar pedidos/sessões. N27 não deve entrar durante o adiamento N26, offline, pausa ou finalização, nem criar novo ciclo de reaberturas sem orçamento. Preservar Y5 missingWindow e os ficheiros válidos.

### 16.4. Ordem revista e critérios de paragem

1. **Referência mínima:** estado do checkout/snapshot, configurações comparáveis e P07 apenas suficiente para medir o caso. Reutilizar a fixture existente; nenhuma credencial de fornecedor nem APK/instalação automática.
2. **Correções pequenas, separadas:** P01a limitado/P02; N18; N25; N19; P13a e P17 se necessários à representação. Uma alteração por verificação, sem mudar PCM/surround/latência/qualidade.
3. **Redução de inferências e observação:** P06 separando prosa de decisão; P08/P15/N21 integrados no diagnóstico mínimo. Avaliar custo antes/depois e não poluir o HUD.
4. **Percursos locais/gravação, conforme uso:** N26 restrito, com fixtures; P10/P11 e N27 só ensaio antes de ação funcional. Nenhum deles deve bloquear o direto. Se não se usa gravação/timeshift, não os vender como aceleração do direto.
5. **Protótipos isolados:** P01b, P03/P04 perante problema residual, P12 com variante real, N20/R01 com paridade da compatibilidade anterior, N22/N23 para validar áudio. Integrar apenas o que passar o critério e tiver benefício útil.
6. **Fora da execução corrente:** P05/P09/P14/P16/R02/R03 e expansões P11b/P13b sem os seus gates. Reabrir a decisão quando houver caso/evidência; não fazer por completude.

Se não houver benefício mensurável ou se o desenho exigir mais estado/mecanismos do que o problema justifica, **não integrar o protótipo**. Manter a conclusão/teste no documento não obriga a ter código de produção correspondente. Não exigir nova opção nas definições só para expor um ensaio.

### 16.5. Formas de implementação rejeitadas

Estas rejeições não são uma nova contagem de famílias:

- Fecho artificial de corpo, retirada da confirmação de libertação ou timeout usado como prova de que recurso fechou.
- Retries gerais em conjunto com mais concorrência para tentar esconder recusa do fornecedor.
- Cache DNS sem contexto/invalidação ou invalidada por qualquer evento de metadados.
- Rejeição geral de origem por target imperfeito ou de janela clara por AES apenas em GAP/segmento ignorado.
- Aceitar variante porque existe um decoder MIME e declará-la automaticamente compatível ou com desempenho adequado.
- Novos resets de áudio, provider, PCM e fallback aplicados ao mesmo tempo sem causa demonstrada.
- Contagem de avisos genéricos que desliga sincronismo, ou eliminação global do workaround que mantém feeds patológicos utilizáveis.
- Alteração de AFR/HDMI por cada variação de bitrate ou logs novos por frame/segmento sem necessidade e limite.
- Reinício da gravação por todo erro isolado de segmento ou stall detetado durante espera legítima.
- CookieJar global, headers sem escaping ou retirada global de edit lists/flags TS para «aceitar tudo».

### 16.6. Estado e rastreabilidade desta revisão

Foram alterados **apenas este plano e a nota de remissão no complemento**. Nenhum teste de correção, reprodução física, mudança de defaults, snapshot ou APK é resultado desta reanálise. As conclusões são revisão de desenho/código/fontes e não percentagens de velocidade ou robustez.

Consultados os sources locais Media3 1.11.1/OkHttp 5.5.0 e [RFC 8216: target duration](https://www.rfc-editor.org/rfc/rfc8216.html#section-4.3.3.1), [Media3: diagnóstico e limitações](https://developer.android.com/media/media3/exoplayer/troubleshooting), além das fontes anteriores. As referências de ficheiros/métodos das secções 13–15 continuam válidas, com os limites revistos aqui. Antes de executar, reler código atual e nunca aplicar por aproximação se o contexto tiver mudado.

### 16.7. Prioridade de compatibilidade e fluidez — revisão de 07/11/2026

**Preferência explícita do utilizador:** não aumentar a rigidez se isso bloquear transmissões utilizáveis ou tornar a app lenta. Esta regra condiciona todas as 30 propostas, incluindo os mapas de ficheiros anteriores. Foi relido o plano e foram novamente consultados os percursos atuais de drain, sintonia local, capacidades e captura. Esta revisão documental não acrescenta uma nova validação física nem uma nova votação de agentes à revisão de 05/11.

**Duas decisões ficam mais conservadoras:** P10 e P11 passam de «reduzir» para **«testar primeiro»**. O resultado atual é **4 manter, 10 reduzir, 10 testar primeiro, 6 adiar**. Nenhum novo bloqueio de manifesto/cifragem é integrado automaticamente. As operações detalhadas anteriormente são desenhos candidatos, sujeitos aos critérios abaixo.

#### Regra aplicada ao conjunto completo

| Propostas abrangidas | O que se procura melhorar | Restrição de compatibilidade/fluidez |
| --- | --- | --- |
| P01, P02, P03, P04, P05, N18, R01, R02 | Identidade HTTP e espera evitável | Não acrescentar HEAD/GET de sondagem antes do pedido real, abrir sessão duplicada, criar verificador prévio por canal ou ativar retries/concorrência por defeito. Não retirar headers válidos sem substituição comprovada. |
| P06, N20, N25 | Decisão de recuperação | Não transformar um aviso, metadado ou falha isolada em recusa definitiva. Preservar os percursos úteis existentes e não reabrir por eventos sem causa demonstrada. |
| P07, P08, P13, P15, P17, N21 | Observação e informação correta | Novos eventos frequentes sem construção de strings/disco quando o detalhe não é necessário; sem timers paralelos, leituras por frame, alertas repetidos ou crescimento do diálogo técnico. |
| P10, P11, P12, N26, N27 | Timeshift e gravações coerentes | Não impor um parser/whitelist mais restritivo ao HLS direto. Provar paridade do produtor afetado, preservar conteúdo válido e limitar falha real a esse percurso. |
| P09, P14, N22, N23 | Compatibilidade e sincronismo de áudio | Manter defaults/clock/mixer/codec até existir evidência. Não reiniciar áudio por silêncio legítimo nem provocar novas sessões HTTP para corrigir hipótese. |
| P16, N19, R03 | Vídeo, cadência e parser | Nenhuma troca HDMI por cada variante, simplificação de parser sem fixtures ou remoção global de workarounds. Desconhecido/inconclusivo não se torna incompatível por conveniência. |
| N24 | Validação reproduzível | Executar fora da utilização normal e acrescentar só o cenário necessário. Não incluir servidor permanente na App nem criar automaticamente um ficheiro/helper por cada proposta. |

#### Cuidados concretos obrigatórios antes de integrar

1. **Preservar o percurso que já reproduz.** Não acrescentar checks de conformidade antes do Media3/mpv só porque a origem tem metadados imperfeitos. Isto não permite interpretar qualquer conteúdo como vídeo: um erro real do motor continua a ser tratado pelo percurso atual, com cancelamento e limites. Não substituir o parser HLS geral por um parser próprio mais rígido.
2. **P10: ensaiar o manifesto produzido localmente.** Fixar a apresentação local não autoriza rejeitar a origem só por uma tag ausente/subestimada ou por arredondamento excessivamente estrito. A escolha/derivação do target e a resposta a um segmento posterior maior precisam de fixture antes de produção. Nenhum novo bloqueio se aplica ao direto.
3. **P11: separar tag de chave de incapacidade efetiva do produtor.** Testar AES→NONE, GAP, sequências ignoradas e metadata incoerente com bytes claros; este último é um cenário sintético de regressão, não uma afirmação sobre o fornecedor atual. A detecção por metadata não deve eliminar automaticamente um caso antes aceite. Se o produtor realmente não conseguir captar, a limitação é do timeshift local; [HLS AES-128 no ExoPlayer direto é um percurso suportado](https://developer.android.com/media/media3/exoplayer/hls), que não deve ser desativado por esta alteração. Sem suporte real, não fingir desencriptação nem publicar bytes cifrados como TS claro.
4. **P12: desconhecido continua inconclusivo.** Não criar whitelist obrigatória de codecs/boxes nem recusar uma variante apenas porque não foi possível consultar capacidades. O leitor efetivo continua a ter a decisão real de inicialização; uma ponte de capabilities não pode ignorar preferências ou presumir que o leitor local recebeu overrides que não recebe.
5. **Nenhum atraso novo obrigatório.** Não somar margens fixas a todas as trocas, aumentar pré-buffer/retoma ou aguardar estatísticas/diagnóstico/capabilities para mostrar o primeiro frame. Preservar os tempos explicitamente escolhidos para reabertura/alternativas e a espera aplicável do fornecedor. Não reduzir essas esperas silenciosamente para melhorar um benchmark.
6. **Fecho local continua correto e limitado.** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/SourceDrain.kt` verifica pendências imediatamente e tem um limite atual de 1500 ms; a reavaliação não aumenta esse limite nem acrescenta margem após o fecho. A espera deve referir-se aos recursos antigos relevantes, não a toda a atividade de EPG/imagens. Não exigir prova de encerramento remoto do fornecedor, que o fecho local não fornece. Se houver falso pendente, investigar ownership/ack com teste, sem somar timeout ou declarar falsamente o recurso fechado.
7. **Sem escadas de recuperação empilhadas.** Um incidente não pode acionar ao mesmo tempo retry do transporte, reabertura do player, troca de versão, latch Stereo e mudança de decoder. Manter coordenação, cancelamento e orçamento comuns. Um segmento recusado não invalida automaticamente os seguintes nem o host inteiro.
8. **Otimizações devem justificar o custo no seu âmbito.** Na alteração de reprodução, comparar arranque/zapping, reconexões, buffer, continuidade A/V e uso de CPU/RAM com o mesmo cenário. A existência de novo contador ou de um benchmark sintético favorável não prova melhoria na box. Alterações apenas de texto/rotulagem não precisam de infraestrutura de benchmark; devem continuar sem novo I/O.
9. **Preservar as escolhas do utilizador.** ExoPlayer/apenas HLS, exceções por canal, hardware/software, áudio, volume 100%, buffers, atrasos, prioridades, backups e quotas mantêm o contrato atual. Não mudar motor/formato ou enfraquecer a política TLS como efeito escondido de uma melhoria de compatibilidade.

#### Critério de aceitação e recusa de integração

Para um protótipo que altera reprodução, usar casos saudáveis e casos imperfeitos anteriormente aceites, além do caso que se pretende corrigir. Demonstrar que não acrescenta rejeição, pedido, espera ou reconexão indevida. Confirmar também que a opção relevante funciona desligada/default e que há caminho de retorno ao comportamento anterior.

Se um canal antes reproduzível passar a ser recusado, houver nova espera fixa, regressão consistente no arranque/zapping, crescimento desnecessário de CPU/RAM ou mais reaberturas, **suspender a integração e rever o desenho**. Não compensar a regressão aumentando buffers, timeouts ou concorrência. Uma limitação estrutural real deve ser explicada no percurso afetado, com alternativas já suportadas e autorizadas, sem converter a falha local em proibição global.

Não há promessa de risco zero ou de ganho percentual: são critérios para detetar regressões antes de entregar. **Estado em 07/11: só documentação atualizada; nenhum código, default, snapshot ou APK alterado e nenhuma reprodução física validada nesta revisão.**


## 17. Execução da primeira fase — 07/11/2026

O pedido posterior para implementar por fases autorizou a primeira fase, sujeita à revisão de compatibilidade da secção 16.7. Foi executado apenas o âmbito reduzido **1A + 1C + 1D**. A 1B continua condicionada e sem integração. Os estados documentais das secções 13–16 descrevem as respetivas revisões anteriores; esta secção e o estado no topo representam a execução posterior.

[Relatório exato de implementação, ficheiros, testes e limites](IMPLEMENTACAO_FASE_1_HLS_AUDIO_2026-11-07.md).

- Diagnóstico de respostas HTTP bem-sucedidas sem leitura adicional; mantida a deteção funcional estreita de TS e a mensagem dos erros HTTP genéricos.
- Recusas 408/429/503 pelos cabeçalhos e apenas bytes já disponíveis; preservados prazo, Retry-After, fecho e orçamento existentes.
- Métricas de fila/retry/carregamento com callbacks nativos e opção de diagnóstico verificada antes de formatar novos eventos frequentes.
- Configuração AudioTrack conhecida e underruns após avanço observados; rota de AudioManager representada como estimada. Sem novo reset ou fallback de áudio.
- Nenhum default, buffer, tempo de espera, decoder, formato ou política TLS alterado. Volume continua no contrato atual de 100%.

Validação do reprodutor: **623 testes JVM, zero falhas/erros/ignorados**, compilação dos testes Android e lint com zero erros (10 avisos em linhas não alteradas). Os seis testes da ferramenta de snapshot passaram. A App compilou em Standard Release com o novo snapshot, passaram 470 testes JVM adicionais sem falhas/erros/ignorados, e o lint vital Release terminou com `No issues found.`. São 1093 testes JVM no total, além dos seis testes da ferramenta de snapshot. Compilar testes Android não equivale a executá-los numa box.

Foram preservadas cópias anteriores e hashes em `C:/Users/renat/Downloads/OwnTV-fase1-2026-11-07-mra3ib5z`. O novo snapshot é comparado integralmente com o checkout. A configuração existente AGP 9.5.0 exigiu acrescentar hashes de dependências em falta, corroborados nos repositórios oficiais, sem desativar verificação nem atualizar versões.

Não houve APK, instalação, publicação ou reprodução física. A causa dos cortes de áudio não fica confirmada por esta entrega. As fases seguintes mantêm os seus gates de evidência e autorização; não avançaram automaticamente.


## 18. Execução da fase 2A — 07/11/2026

O pedido posterior autorizou apenas **2A/P03**. O teste anterior à alteração confirmou fila residual após a fase 1: o cliente real partilhava o dispatcher das imagens; cinco pedidos de imagens retidos antes dos cabeçalhos impediram a playlist HLS de chegar ao servidor. A espera sintética registada foi 474,806 ms. A retenção era deliberada; este valor não é uma medição de desempenho físico.

Foi integrada a alteração mínima: um dispatcher próprio, com os limites padrão 64/5, no singleton `StreamingHttpClient`. Os clientes dos motores derivados reutilizam-no; ownership, fecho, eviction, TLS, DNS e tempos permanecem. O teste posterior recebeu a playlist enquanto os cinco pedidos de imagens continuavam pendentes. Foi também confirmado o limite da própria fila, o cancelamento de uma chamada pendente sem a enviar e a independência do retirement de dois proprietários.

Passaram **78 testes JVM de rede e 624 de reprodução**, sem falhas/erros/ignorados. A fase 2B não foi iniciada. A separação pode aumentar a atividade assíncrona agregada entre as duas filas; não se alteraram limites nem orçamentos de sessões e não se confunde dispatcher com o tempo total de leitura dos corpos.

A integração revelou ainda cache de configuração da App a reutilizar o Core anterior apesar do lock novo. Foi necessário declarar a leitura do lock com `providers.fileContents(...).asText`: o teste funcional confirmou mudança A→B e posterior reutilização em B. A cache continua ativa. Passaram o teste funcional e os seis testes Python do publicador. A primeira tentativa de validação da App com configuração antiga não conta para esta fase.

[Relatório de implementação e validação da fase 2A](IMPLEMENTACAO_FASE_2A_FILA_REPRODUCAO_2026-11-07.md). Snapshot de 1092 inputs: `fcd36cd7247c44c8ccddc00a14dcb12b92c534980fe583c89ce86ecaa360ddf4`. Confirmou-se a igualdade integral com o checkout e a criação do dispatcher no bytecode Release compilado a partir desse snapshot.

**Validação da App concluída:** Standard Release compilou com o snapshot correto, passaram 470 testes JVM adicionais e o lint vital Release terminou `No issues found.`. Total desta entrega: 1172 testes JVM e sete testes Python/Gradle aprovados; `BUILD SUCCESSFUL in 23m 56s` na integração final.  O lint integral do Core terminou com cinco erros de pluralização e 67 avisos; os cinco erros pertencem a quatro ficheiros de recursos cujos hashes já eram iguais no início desta fase. Os locais exatos constam de `core-full-lint-preexisting-errors.json`, em `C:/Users/renat/Downloads/OwnTV-fase2a-2026-11-07-121345`. Permanecem pendentes fora da 2A; não se criou baseline ou supressão para os ocultar.

Não houve APK, instalação ou teste físico. Os testes locais não garantem a eliminação dos cortes de áudio ou do bloqueio temporário do fornecedor. A aceitação na box permanece separada.


## 19. Execução da fase 2B — 08/11/2026

O pedido posterior autorizou apenas **2B/P04**. A execução começou por comparar integralmente o checkout com o snapshot final da 2A e preservar os ficheiros. O teste anterior confirmou a perda do sucesso DNS que chegava depois do timeout; um segundo lookup com falha transitória também falhava. A versão final preserva esse sucesso e recupera a alternativa válida. A contagem de consultas nativas manteve-se em duas: **não se anuncia redução geral de consultas nem ganho percentual**.

[Relatório da fase 2B, operações por ficheiro e resultados](IMPLEMENTACAO_FASE_2B_DNS_2026-11-08.md).

- `DnsConfigHolder.capture()` fornece geração e resolver coerentes com a configuração capturada, sem alterações dos backends UDP/DoH.
- `BoundedDns` publica ao concluir o trabalho nativo, sem depender do sucesso do chamador; chaves e identidade da tarefa impedem reutilização/publicação em outro contexto DNS/rede.
- `StreamingHttpClient.liveDns` é partilhado entre leitores Live; `DataModule` liga o holder e o observador existente, e `LivePreviewEngine` deixa de criar o wrapper por instância. Os outros percursos conservam o DNS anterior.
- A observação de rede é consumida de forma partilhada, com subscrição lazy no primeiro uso do Live DNS. Replays, validação/metering/transportes não invalidam a cache. Falha de observação conserva a última identidade; uma mudança/perda conhecida muda geração. Não há filtro de conectividade que recuse resolver um canal saudável.
- Preservados os prazos de 5/1 s, validade local de 5 minutos, 64 entradas e executor de quatro workers/fila 64. O refresh por lookup continua. Não há novos retries HTTP, sondagens, binding a Network, buffers ou política de áudio.

**Validação:** 95 testes JVM de rede, 624 do reprodutor e 470 da App aprovados, sem falhas/erros/ignorados: **1189 no total**, incluindo 17 novos. O cenário conjunto recebe HLS com fallback DNS enquanto cinco pedidos de imagens continuam pendentes. A compilação Standard Release e o lint vital Release da App passaram. A confirmação do lock definitivo terminou `BUILD SUCCESSFUL in 1m 34s`, após a compilação completa das mesmas fontes de produção.

Snapshot final: **1094 inputs**, SHA-256 `683d66fa7a260834d89898bca939627743a6925b8a5b4df90ac1fa78a41fe84d`. Checkout, ZIP e inputs extraídos/compilados foram comparados integralmente. Cópias, provas e logs estão em `C:/Users/renat/Downloads/OwnTV-fase2b-2026-11-08-122356`.

A política experimental mais restritiva perante observação desconhecida foi retirada antes da entrega; a primeira compilação desse candidato foi interrompida e não conta como validação. O teste conjunto foi acrescentado antes da confirmação do snapshot definitivo. Não foram repetidos testes de ferramentas inalteradas só para aumentar contagens.

A implementação das duas subfases 2A/2B fica concluída no âmbito aprovado e nos testes locais. Mantêm-se a aceitação física na box e a pendência anterior dos cinco erros de pluralização do lint integral do Core; os recursos afetados não mudaram. Não houve APK, instalação, commit ou publicação. A 1B e as fases seguintes não avançaram automaticamente.
## 20. Execução da fase 3A — 08/11/2026

**Estado:** P06 e complemento reduzido de P07 implementados e validados localmente. Relatório: [IMPLEMENTACAO_FASE_3A_HLS_2026-11-08.md](IMPLEMENTACAO_FASE_3A_HLS_2026-11-08.md).

- Um teste acrescentado antes da alteração falhou por confirmar a contaminação entre canais/contas no mesmo servidor. Foi removido exclusivamente o mapa global de recusas de segmentos, respetiva validade/API e consulta na decisão do motor inicial. As restantes aprendizagens de formato, UA, arquivo e esperas foram preservadas.
- Mantido sinal por load para a escada existente: duas URLs distintas com o mesmo status, janela de 30 s, retry da mesma URL sem contar como outro segmento, limpeza por sucesso/fonte nova/reset saudável. Códigos mistos não são promovidos à mesma evidência. Não se afirma token inválido ou incompatibilidade definitiva.
- Mantido filtro `C.DATA_TYPE_MEDIA`; playlist/chave/inicialização não contam como segmento. Eventos existentes passam a incluir task ID, papel baseado no data type, status de erro e bytes/duração. O papel HLS fica capturado por fonte; callbacks antigos não assumem a rota da fonte seguinte. Strings frequentes só com detalhe ativo; sem relógio, serviço de telemetria ou recuperação paralela novos.
- Mantidos DRM, preferências/pins explícitos, Exo/apenas HLS, exceções manuais, retries por URL, orçamentos, esperas do fornecedor, buffers, áudio/volume, TLS/UA, fila 2A e DNS 2B. Nenhuma migração ou nova definição.

**Validação:** 631 testes JVM do reprodutor e 470 da App aprovados: **1101**, zero falhas/erros/ignorados na execução final. Inclui 11 testes novos; quatro testes da API global eliminada foram retirados e os restantes ajustados. Compilação Standard Release e lint vital Release da App aprovados (**No issues found**). A primeira execução encontrou uma corrida num teste antigo de pesquisa: esperava pela chamada, não pela receção do resultado. Passou isoladamente; corrigida apenas essa espera no teste, mantidas as asserções e repetida a suite completa. O código de pesquisa da App não mudou. Provas da falha inicial preservadas.

Snapshot final: **1098 inputs**, SHA-256 `4d0cd73fee3e62243dc4202a7c32e17aa3514e83f4b0f27328fcd7666b1646ed`. Checkout, ZIP e inputs extraídos comparados integralmente; JAR Release verificado com a assinatura final de `LiveRouting.decide` e os novos helpers. Cópias/provas: `C:/Users/renat/Downloads/OwnTV-fase3a-2026-11-08-131813`.

Não foi repetido o lint integral do Core, que mantém os cinco erros anteriores de pluralização em recursos inalterados. Não houve APK, instalação, commit, publicação ou teste físico. As esperas do fornecedor A–B–A não estão demonstradas como resolvidas por esta fase. **3B, 1B condicionada e fases seguintes permanecem pendentes.**
## 21. Execução da fase 3B — 08/11/2026

**Estado:** P17 implementada e validada localmente no âmbito informativo reduzido. Relatório: [IMPLEMENTACAO_FASE_3B_BUFFER_2026-11-08.md](IMPLEMENTACAO_FASE_3B_BUFFER_2026-11-08.md).

- Substituído o resumo «parar acima de» por indicação de limiar; explicação curta de que segmentos podem ultrapassá-lo e que a memória pode limitar a reserva. Mantidas as dimensões, scroll, foco e estrutura do diálogo.
- No resumo HLS, a duração passa a identificar o «Último publicado», sem o confundir com conteúdo descarregado e conservando dados desconhecidos como indisponíveis. Nos detalhes, clarificados o limiar temporal, a referência da playlist e o buffer alocado/alvo em MiB.
- Reutilizados campos, recursos e formatação existentes em inglês/PT/PT-PT. Não foi necessário alterar `StreamInfo.kt` ou criar métricas, modos, preferências, timers ou testes que apenas repitam os textos.
- A comparação dos 1098 inputs com o snapshot anterior prova que apenas os dois XML de textos do Core mudaram. Política de buffer/load control, RAM, pré-buffer, arranque/retoma, áudio/volume, HLS, DNS, TLS e recuperação 3A conservaram os mesmos bytes. Na App, alteração limitada aos três XML do resumo e à apresentação do último segmento no overlay.

**Validação:** 21 testes JVM existentes de buffer/segmentos e 470 da App aprovados — **491**, zero falhas/erros/ignorados. Preservados nomes/argumentos dos recursos. Compilação Standard Release e lint vital Release da App aprovados (**No issues found**); comando final **BUILD SUCCESSFUL in 5m 6s**. Não foi repetido o lint integral do Core, que mantém os cinco erros anteriores em recursos inalterados nesta fase.

Snapshot final: **1098 inputs**, SHA-256 `a7645d3038d51596469feaa3616bc4cac8ea081a7c96252f2f86382f37ddade2`; checkout, ZIP e inputs extraídos do build comparados integralmente. Cópias/provas: `C:/Users/renat/Downloads/OwnTV-fase3b-2026-11-08-133943`.

Sem APK, instalação, publicação ou teste físico. Esta fase esclarece as medições; não altera limites nem demonstra correção dos bloqueios do fornecedor/cortes de áudio. A fase 3 fica concluída nas duas subfases aprovadas e na validação local. **1B condicionada e fases seguintes permanecem pendentes.**
