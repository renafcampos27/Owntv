# OwnTV — implementação do plano mestre

Data: 2026-10-02. Código alterado na app e no Core local. A validação final é registada abaixo; compatibilidade e desempenho no equipamento continuam a exigir ensaio na Xiaomi, Thomson e smartphone.

## Decisão sobre o volume

O ganho da aplicação permanece a 100%: mpv usa volume e limite de volume 100; Exo usa ganho 1. Os ajustes de volume, o boost e a aplicação de volumes antigos por canal deixam de atuar. Os controlos de volume foram retirados do HUD e das definições da OwnTV. As teclas de volume estão protegidas contra atalhos da app e pertencem ao sistema/box. Valores antigos continuam no armazenamento e nos backups por compatibilidade, mas são ignorados pelos reprodutores.

O mute dos previews e das vistas sem som permanece independente. As interrupções de áudio utilizam pausa com proprietário/revisão; não reduzem o ganho da aplicação. A opção Estéreo continua a ser uma opção de saída, sem aumentar o volume.

## Correções e integração

| Referências do plano | Implementação |
|---|---|
| B1 | Intenção Play/Pause real no adaptador mpv e nos caminhos Exo. Comandos explícitos, revisão de decisões, cancelamento de recuperação ao pausar, retoma de preparação definida e callbacks associados ao motor/sintonia atual. |
| B2 | Um resgate estéreo por episódio de sintonia, conservado em reconstruções internas. Falha persistente termina sem renovar indefinidamente o orçamento. |
| B3 | Mistura PCM16 explícita apenas no modo Estéreo, com layouts de 1–8 canais, centro/diálogo, LFE, canais laterais e remapeamento. Matriz normalizada, estéreo FL/FR intacto e layouts desconhecidos recusados. Offload codificado não pode contornar o processamento estéreo. |
| B4/B5/C1 | Ganho fixo conforme a decisão do utilizador. Foco concedido/atrasado/recusado/transitório separados; pedidos e retomas associados a motor, geração e revisão. Pausa manual posterior prevalece. |
| B6 | Alternativa de formato só consome o erro se tiver sido efetivamente agendada. |
| B7 | URL HLS identificável precede aprendizagem DASH de outros canais, conservando declarações/decisões explícitas e Apenas HLS. |
| B8 | Uma tentativa elegível de User-Agent só ensina após reprodução confirmada. A lição fica limitada à URL e ao prazo de validade; não é generalizada ao servidor inteiro nem aprendida por um 503 transitório. |
| B9 | Retry-After preserva o prazo real, incluindo HTTP-date. 429/503 com espera não geram retries internos da biblioteca antes do prazo. Esperas acima do teto automático terminam com erro. Alteração de definição, pausa e Home não encurtam o prazo. |
| B10 | Cancelamento de coroutine fecha a chamada HTTP bloqueada antes dos headers ou durante a leitura. A preparação Live aguarda a drenagem das chamadas antigas e recusa abrir outra fonte se o prazo de drenagem expirar. |
| C2 | Velocidade reaplicada em cargas mpv, handoffs e reconstruções Exo. |
| C3/C4 | Pausa AFR com revisão/proprietário e cleanup em sucesso, cancelamento e timeout. Home durante hold conserva a intenção anterior; Pause manual/Stop/nova seleção invalidam a retoma. O listener observa o display e modo relevantes e é removido. |
| C5 | Política comum limita fallback involuntário para software UHD confirmado em dispositivos com recursos limitados. Hardware OFF explicitamente escolhido permanece permitido. Informação desconhecida não é recusada por suposição. |
| C6/C7/C8 | Restauro do foco termina no primeiro sucesso e cede à navegação. Menu do canal mantém a âncora normal; categorias confirmam foco com tentativas limitadas após scroll/layout. |
| C9/C10 | Invalidação inclui linhas do fornecedor; chaves de contexto distinguem offsets e personalizações. Now expira no limite do programa, além do prazo máximo do cache. |
| C11 | A linha temporal não é subscrita quando oculta. Usa o canal em reprodução e uma janela curta de resumos; o seletor de histórico completo conserva os diferidos disponíveis. |
| C12 | Abertura/migração da base de dados em IO, com estado observado e ViewModel retido durante rotação. Consumidores da base de dados aguardam Ready; erro tardio e retry explícito permanecem acessíveis. |
| C13 | ENDLIST guarda os últimos segmentos e finaliza normalmente, sem toast de falha. |
| C14 | Produtor local recupera erros elegíveis com número finito de tentativas por segmento; não avança a sequência antes do commit. Sequências expiradas produzem salto/discontinuity explícitos. |
| C15 | Origem estável e URL final de resolução separadas. Refresh conserva identidade da variante e headers e permite renovar URLs assinadas. |
| C16 | Intervalos HLS exigem resposta 206 e Content-Range/bytes coerentes, tanto em gravação como em timeshift. Escrita não ultrapassa a reserva. |
| C17 | Prazo sem progresso proporcional à cadência declarada, limitado a 45–300 s no produtor local. Não altera o timeout normal de abertura de HLS remoto. |

A revisão de integração corrigiu ainda o watchdog de abertura ao retomar um carregamento, preservou zoom/atraso/posição em reconstruções internas e impediu overflow de MEDIA-SEQUENCE no produtor local.

## Otimizações e respetivos limites

| Referência | Resultado e limite |
|---|---|
| O1 | Títulos pedidos para viewport, overscan e seleção; não reduz o snapshot/índices usados no zapping. Ganho em frame-time e alocações ainda por medir. |
| O2 | Leitor partilhado por DI e pedidos brutos equivalentes agrupados por fonte/conta/canal. Cancelar um consumidor não cancela os restantes; invalidar a geração rejeita respostas antigas. |
| O3 | Timers de lista/guia/relógio suspendem fora do lifecycle ativo e refrescam na retoma. |
| O4 | Taxa de download separada de bitrate declarado, com filtro de transferências locais e amostra partilhada entre overlays. Zero entre rajadas HLS continua a ser normal. |
| O5 | Resumo QoE por evento correlacionado: duração de buffering inicial/rebuffer, número e tempo acumulado de rebuffers, reconstruções, buffer, live offset e frames perdidos. Mantém diagnósticos HTTP/áudio/decoder e monitor UI existentes; sem novo registo por frame. |
| O6 | Conserva Automático/Assíncrono/Compatibilidade síncrona e a instrumentação para comparação. Não move operações JNI/Surface para um worker genérico; essa decisão depende de traces e do contrato do motor. |
| O7 | Falha de EPG não é guardada como sucesso vazio. A vista pode apresentar dados anteriores ainda elegíveis, fora do cache de sucesso, e recuperar na próxima consulta normal. Cancelamento/invalidação não se tornam ausência de programas. |
| O8 | Timeshift filtra incompatibilidade conhecida de família de codec/resolução usando capacidades declaradas. Metadata desconhecida e Dolby Vision inconclusivo permanecem elegíveis. Reserva real/incremental conta temporários e ficheiros presos por leitores. Gravações e HLS remoto conservam as respetivas políticas. |
| O9 | Durabilidade/fsync e quotas conservados. Alterar cópias/remux/prioridades de disco exige medição de competição com reprodução; não há benefício demonstrado que justifique retirar essas garantias. |

O6 e O9 são ensaios condicionados a equipamento, não defeitos com uma correção universal. Não se afirma melhoria 2×–4×, eliminação de cortes de 0,2 s ou compatibilidade total a partir dos testes de unidade.

## Validação final

- Compilação integrada StandardDebug da app, Core e player-core: aprovada, com os inventários de literais ativos.
- Testes de unidade: **app 275 + Core 966 + player-core 353 = 1 594**, em 206 classes, sem falhas, erros ou testes ignorados.
- Testes novos incluem PCM real/layout/remapeamento, intenção/revisão, pausa AFR/Home, QoE, pedidos HTTP com sockets reais, cancelamento antes dos headers/durante o corpo, espera observada, limites de retry, URLs assinadas, ENDLIST, Range, quotas, falha→recuperação de cache e retenção/single-flight/erro tardio de startup ViewModel.
- Verificações de internacionalização, locale numérico e overflow de texto: aprovadas nos dois repositórios. Recursos/traduções validados; SupportedLocales e artefactos de comunidade atualizados e verificados.
- Android Lint player-core, com o código final: **0 erros, 5 avisos**.
- Android Lint App: concluído, **não aprovado: 4 erros, 118 avisos e 32 sugestões**. Os quatro erros continuam a ser os três RestrictedApi no dispatchKeyEvent de MainActivity e o LocalContextGetResourceValueCall de EpgScreen, já registados no relatório anterior. Não foram suprimidos. A execução terminou antes da interrupção planeada; nenhum processo foi terminado. UElementAsPsi foi excluído apenas desta ronda App através de um script temporário, devido ao problema previamente observado; nenhuma regra do projeto foi desativada permanentemente. Não se declara aprovação integral de Lint. A passagem prolongada pelo JoinEffectDetector foi registada em C:/Users/renat/AppData/Local/Temp/owntv-master-lint-active-stack.txt.
- Log Lint desta ronda: C:/Users/renat/AppData/Local/Temp/owntv-master-lint-final.log. Relatório final dos reprodutores: C:/Users/renat/Downloads/OwnTV_Core/player-core/build/reports/lint-results-debug.html.
- Log da compilação/testes: C:/Users/renat/AppData/Local/Temp/owntv-master-integration-final.log. Relatórios XML em app/build/test-results/testStandardDebugUnitTest, Core/core/build/test-results/testDebugUnitTest e Core/player-core/build/test-results/testDebugUnitTest.

As asserções de HTTP usam pedidos/corpos reais, mas não certificam a libertação da sessão no servidor de um fornecedor. Os testes de intenção/QoE são determinísticos; callbacks de foco Android, HDMI/AFR, Surface/JNI, métricas Perfetto e reprodução no equipamento requerem os ensaios abaixo.

A integração PCM foi também cruzada com o [DefaultAudioSink do Media3 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/audio/DefaultAudioSink.java): uma alteração de contagem pelo pipeline retira a máscara original; offload codificado não passa pelo processamento PCM. Isto fundamenta a proteção da saída estéreo, sem provar desempenho da box.

## Ensaio na box e no smartphone

1. Gerar a variante StandardDebug no Android Studio com o Core local atualizado. Instalar como atualização da app existente.
2. Confirmar que os botões de volume alteram apenas o sistema, que não existe ajuste de volume da OwnTV e que a reprodução/previews têm o comportamento de som esperado.
3. Percorrer A → B → C → A com intervalos curtos e normais, em lista e fullscreen. Confirmar cancelamento do anterior, ausência de erro antigo em reprodução saudável e funcionamento das setas de canal.
4. Pausar durante abertura/retry/espera do fornecedor, carregar Play/Pause repetidamente e testar Home/retoma. Uma pausa manual permanece; a espera do fornecedor não se encurta.
5. Smartphone: rotação durante abertura da app, reprodução e timeshift; retirada de auscultadores/Bluetooth; interrupção e retoma de foco. Box: Home durante hold AFR, saída do player e retorno.
6. Navegar no EPG, alterar offset/personalização, verificar mudança de programa no minuto correto e recuperação após erro de rede. Confirmar histórico/recuo e gravações preservados.
7. Modo Estéreo: ensaiar faixas estéreo e 5.1 com diálogo central, HDMI e velocidades de reprodução. A validação PCM automatizada não certifica a cadeia física da box/receiver.
8. Reproduzir o mesmo canal saudável 30–60 minutos, com diagnóstico detalhado quando necessário. Comparar processamento Exo mantendo rede, formato, buffer, latência e canal constantes. Separar cortes do fornecedor, falta de media, underrun, timestamps e alterações de saída/foco.

Código existente e documentos anteriores foram preservados. Não foi gerado APK, alterada versão nem realizado commit/push nesta implementação.
