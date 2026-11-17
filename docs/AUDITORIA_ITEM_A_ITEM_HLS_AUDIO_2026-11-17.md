# Auditoria independente das propostas de áudio e recuperação HLS

Data: 17/11/2026. Estado: **auditoria, sem alteração funcional da App/Core**.

**Execução posterior à auditoria:** apenas A01 foi implementado, após autorização do utilizador. Consultar [o registo de implementação e validação](C:/Users/renat/Downloads/OwnTV-main/docs/IMPLEMENTACAO_A01_AUDIO_2026-11-17.md). As observações de código abaixo descrevem a baseline auditada; os restantes itens continuam pendentes.

Documento auditado: [problemas e soluções](C:/Users/renat/Downloads/OwnTV-main/docs/PROBLEMAS_E_SOLUCOES_RECUPERACAO_HLS_AUDIO_2026-11-17.md). Este parecer revê o grau de aprovação dos 12 itens, não autoriza executar todos.

## 1. Método e limites

Foram obtidos quatro pareceres de agentes IA: áudio/codecs, HLS/HTTP, coordenação de recuperação e um revisor novo, sem o histórico anterior. O agente principal cruzou divergências e evidências. Não são profissionais humanos externos nem quatro reproduções independentes na box.

Lock atual: `C:/Users/renat/Downloads/OwnTV-main/core-source-lock.json`; snapshot `dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`. Os especialistas reconfirmaram igualdade dos ficheiros centrais com o ZIP; o revisor novo verificou percursos locais, sem repetir comparação integral. Media3 1.11.1; OkHttp 5.5.0.

Foram consultados documentação, sources oficiais, discussões com mantenedores e relatos de utilizadores. Um relato de outro dispositivo/versão não prova a causa do sintoma Xiaomi. Não foram executados novos testes de reprodução, builds, alterações de dependências ou APK.

## 2. Veredito por proposta

| ID | Decisão revista | Existe solução conhecida? | O que fazer na OwnTV |
| --- | --- | --- | --- |
| **A01** | **Aplicar em lote controlado** | Sim: contrato explícito Media3 | Retirar a recuperação destrutiva desencadeada apenas pelo aviso de sink, preservando falhas fatais |
| **A02** | **Teste residual após A01; correção mínima se demonstrada** | Existe problema de estado local; não foi encontrado patch externo idêntico | Validar razão, saída, `pendingReason` e `fired` em conjunto; evitar refactor amplo |
| **A03** | **Manter e testar** | Sim, handler e recuperação já existem | Garantir que A01 não remove tratamento de erro fatal atual |
| **H01** | **Manter e testar com H02** | Sim, recuperação Media3 e especialização OwnTV | Medir os três caminhos antes de mudar retry/handover |
| **H02** | **Testar classificação e corrigir os casos demonstrados** | A política é configurável; não há receita universal para o fornecedor | Separar papel/status/contexto; não tornar todo 403 terminal |
| **H03** | **Comparação separada** | Sim, exemplo Android e recuperação genérica local | Só integrar ramo específico se reduzir trabalho e preservar timeshift/arquivo |
| **H04** | **Manter e validar** | Sim, reconexão, ownership e drain já existem | Corrigir falha reproduzida; não criar outro retry engine |
| **H05** | **Manter guardas; esclarecer indicação e contrato** | Sim, Retry-After padronizado e guarda local | Testar datas, limites e saída; não prolongar espera automaticamente |
| **H06** | **Experiência condicionada** | Há fundamento para ensaio, não benefício comprovado | Manter 12 s até teste comparativo justificar extensão finita |
| **H07** | **Reproduzir e comparar upstream** | Sim, correção real posterior à 1.11.1 | Confirmar cenário de variante interna HLS; não atualizar para beta por suposição |
| **T01** | **Experiência playback-only** | Sim, recuperação de ligação no OkHttp | Comparar pedidos/sessões/cancelamento; não tratar como solução geral de corpo interrompido |
| **T02** | **Condicional; separar cookies de cabeçalhos** | Sim para origens que exigem cookie; headers dependem do destino | Não ativar cookies globais nem novas recusas de CDN sem caso concreto |

**Contagem:** 12 itens auditados. A01 é a única mudança diretamente aprovada pelo contrato atual. A02 é uma correção residual dependente de ensaio. A03/H01/H04/H05 são quatro blocos principalmente de preservação/validação. H02/H03/H06/H07/T01/T02 são seis candidatos condicionados. Isto não corresponde a 12 bugs confirmados na box.

## 3. Áudio: auditoria e solução por item

### A01 — aviso de áudio provoca pausa ou rebuild

**Evidência local:** `AudioOutputPolicy.kt:397–423` promove o aviso a `raise`; `LivePreviewEngine.kt:1034–1051` pode pausar, latchear estéreo ou reconstruir a reprodução.

**Evidência externa:** o [contrato exato Media3 1.11.1](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/analytics/AnalyticsListener.java) distingue diagnóstico de falha. [Eventos do leitor](https://developer.android.com/media/media3/exoplayer/listening-to-player-events) descrevem o percurso fatal. Não é necessário encontrar um post de fórum para contrariar um contrato explícito.

**Solução aprovada:** manter observação/log e retirar desse aviso isolado a decisão destrutiva. Preservar `onPlayerError`, tokens e tratamento de falha efetiva. Os testes que equiparam exceção neste callback a erro fatal precisam de ser reespecificados.

**Teste:** aviso de inicialização/escrita seguido de recuperação; nenhum novo pedido/rebuild/pausa. Depois erro fatal atual; recuperação permanece funcional. Testar PCM, passthrough e eventos antigos.

### A02 — estado pendente obsoleto

**Revisão importante:** retirar A01 elimina o produtor mais abrangente de `pendingReason`. Resta o caso de underruns passthrough repetidos. Não aprovar uma grande mudança de gerações por antecipação.

**Caso residual:** saída A avança → quatro underruns elegíveis → decisão pendente → saída B inicia e avança → `poll` ainda entrega a decisão A. Novo output já tem identidade local; aproveitar a existente.

**Risco de uma correção mal feita:** limpar só `pendingReason` pode deixar `fired=true` e desarmar deteções futuras. Limpar ambos indiscriminadamente pode rearmar resgates indefinidamente. `onAudioPositionAdvancing` sinaliza primeiro avanço, não saúde contínua; uma flag antiga não prova recuperação atual.

**Decisão:** reproduzir o caso após A01; especificar invalidação e rearme por razão/saída, preservando o orçamento de resgate. Não foi encontrado um patch público exatamente equivalente para este estado personalizado da OwnTV.

### A03 — falha fatal real

O handler já existe. O filtro de playback confirmado considera `playerError`, pelo que não deve ser condenado só por ignorar certos eventos antigos. Testar erro atual vs obsoleto, Back, pausa, suspensão e troca de canal.

**Solução:** preservar a integração existente; não acrescentar reset periódico, amplitude como detetor de falha ou correção automática de relógio. A01 não substitui a recuperação de ligação HLS.

## 4. HLS: auditoria e solução por item

### H01/H02 — auditar três caminhos em conjunto

O documento anterior descrevia bem retries e erro fatal, mas precisava de explicitar uma escalada adicional:

1. `edgeRefusalPolicy`, `LivePreviewEngine.kt:3478+`: especialização de segmentos 403/404/410 com retry curto, preservando fallback elegível.
2. `onLoadError` → `noteSegmentRefusal` → duas URLs distintas com o mesmo status → `segmentsRefused`; o watchdog pode pedir handover antes do erro fatal do player.
3. `onPlayerError` runtime → reconexão HLS rápida, cuja espera fica limitada a 1,5 s.

O controlador revalida playback confirmado antes de avançar; isso protege um canal já recuperado. Durante BUFFERING, os caminhos ainda podem antecipar-se entre si. A origem de uma decisão não pode ser inferida apenas pelo último erro visível.

**Fontes:** [política Media3 1.11.1](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/upstream/DefaultLoadErrorHandlingPolicy.java), [personalização oficial](https://developer.android.com/media/media3/exoplayer/customization#customizing-error-handling).

**Solução recomendada:** uma fixture para os três percursos, conservando papel do load, sintonia e status. Medir load retries, fallback, handover e reprepare, sem contagem simplista de «oito pedidos». Só alterar o caminho responsável pelo caso demonstrado.

**Ensaios:** segmento recupera em 600–1500 ms; duas recusas seguidas de recuperação; variante elegível disponível; recusa persistente; 401/403 da entrada; 458 antes/depois de READY; 503 com/sem prazo.

**Contraargumento obrigatório:** retirar retries curtos ou o sinal de recusas pode piorar canais que anunciam segmentos permanentemente inacessíveis. Não remover esses mecanismos globalmente.

### H03 — posição anterior à janela do direto

O [Android documenta](https://developer.android.com/media/media3/exoplayer/live-streaming) `seekToDefaultPosition()` e `prepare()` para a condição específica. O percurso genérico OwnTV já recria a fonte, pelo que o benefício de um ramo adicional ainda deve ser demonstrado.

**Novo risco confirmado:** `LivePreviewEngine.isLiveContent` é constante true, e `LiveTuneController.startLocalHls` também usa esse motor para timeshift local. Essa flag não distingue direto do fornecedor de uma posição histórica local. O arquivo do fornecedor segue outro percurso.

**Condição de implementação:** identificar provider-live/local-timeshift/archive e a intenção atual, além do token. `defaultPosition` não significa posição zero nem borda absoluta; respeita a janela e a configuração efetiva.

**Teste:** comparar recuperação genérica e específica; impedir salto indevido de um rewind/arquivo válido e reinício após saída. Sem benefício mensurável, manter a recuperação atual.

### H04 — ligação perdida ou bloqueada

Reconexão, callback por fonte, cancelamento de chamadas/corpos e drain já existem. A [implementação HLS oficial](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/exoplayer_hls/src/main/java/androidx/media3/exoplayer/hls/playlist/DefaultHlsPlaylistTracker.java) também tem mecanismos internos. Não adicionar um recuperador paralelo.

**Precisão:** «uma recuperação ativa» não significa um único HTTP. Áudio, vídeo, playlist e chave podem legitimamente ter pedidos concorrentes. O objetivo é impedir duas sintonias/reaberturas lógicas concorrentes ou pedidos de uma fonte abandonada.

**Aceitação corrigida:** se o servidor voltar antes do abandono terminal, deve poder recuperar segundo a política atual. Se a App já saiu à lista, não há promessa atual de continuar a tentar. Essa continuação seria outra funcionalidade, opcional e explícita.

**Manter:** drain imediato quando não há recursos pendentes; 300 ms não obrigatórios; fecho local não equivale a libertação remota da conta.

### H05 — espera do servidor e limites

O [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.3) fundamenta Retry-After; a guarda local continua a impedir pedidos antes do prazo. Não foi encontrada violação demonstrada. Desistir da interface antes do fim não é, por si, erro.

**Melhoria:** explicitar se uma contagem é condicional à tentativa ainda ativa; distinguir abertura e recuperação sem alterar valores implicitamente.

**Testes:** número e HTTP-date, prazo maior que o teto automático, nova seleção, volta à lista e duas contas no mesmo servidor. O parser já suporta datas; não criar esse suporte novamente a partir de comentário antigo.

### H06 — alongar o watchdog

A assimetria de progresso do primeiro stall existe, mas não comprova que esperar mais recuperaria. Manter 12 s. Não usar bytes de playlist/chave como prova suficiente.

**Solução apenas condicional:** uma extensão finita por interrupção, se a comparação mostrar recuperação útil e não piorar os casos mortos. Não introduzir refresh externo da playlist, já gerido por Media3.

Relatos [#3337](https://github.com/androidx/media/issues/3337) e [#3362](https://github.com/androidx/media/issues/3362) mostram outras classes de bloqueio de posição/timestamps; não são prova de defeito OwnTV, mas reforçam que «mais tempo» não é solução universal.

### H07 — patch upstream de chunks parciais

O [issue #3362](https://github.com/androidx/media/issues/3362) e o [patch c4b16f9](https://github.com/androidx/media/commit/c4b16f9) sustentam o caso; a correção completa não está na 1.11.1 analisada. O mantenedor indicou disponibilidade inicial em 1.12.0-beta01.

**Condição ignorada que passa a ser explícita:** variante interna do mesmo manifesto HLS não é o mesmo que mudar SIC → SIC HD na lista. Confirmar que o cenário efetivo envolve variantes/chunks parciais antes de o relacionar com a falha.

**Solução:** fixture reproduzível e comparação com versão que contenha o conjunto. Não copiar só o reset de posição nem atualizar para beta automaticamente. A correção distinta do #3311 já está presente; não a confundir com este ramo.

## 5. Transporte e compatibilidade

### T01 — sockets e rotas

O [source OkHttp 5.5.0](https://raw.githubusercontent.com/square/okhttp/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt) confirma a opção herdada `retryOnConnectionFailure(false)`. A alternativa pode tratar sockets reutilizados e falhas de rota/IP/proxy; não é recuperação universal de corpo já entregue ao Media3.

O [relato #8587](https://github.com/lysine-dev/okhttp/issues/8587) discute repetições com efeitos de autenticação em versão antiga, sem prova de defeito em 5.5.0. É contraponto à ativação global, não diagnóstico da OwnTV.

**Solução candidata:** protótipo só no playback, medindo falha antes/depois de o servidor processar o GET, rota, socket antigo e cancelamento. Contar pedidos físicos e sessões lógicas. Preservar provider guards e cliente de catálogo.

### T02 — dividir cookies e cabeçalhos de CDN

**Cookies:** o [ExoPlayer #5975](https://github.com/google/ExoPlayer/issues/5975) documenta necessidade em CloudFront, reconhecida pelo mantenedor. O [#6127](https://github.com/google/ExoPlayer/issues/6127) foi resolvido pela inicialização correta dos cookies, não pela suposição de um novo retry. Nenhum prova que o fornecedor atual precise disso.

**Solução condicionada:** se `Set-Cookie` for necessário nos pedidos seguintes, memória limitada e isolamento por fonte/conta, respeitando validade/domínio/caminho/Secure. Não adicionar depósito global por defeito.

**Cabeçalhos:** o guard de redirects e os novos pedidos para URLs absolutas de segmento não seguem exatamente o mesmo percurso. O [OkHttpDataSource 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/datasource_okhttp/src/main/java/androidx/media3/datasource/okhttp/OkHttpDataSource.java) aplica cabeçalhos configurados aos pedidos. Isso exige testar destino autorizado e identidade, não permitir ou recusar tudo.

**Solução condicionada:** fixture origem A, CDN B autorizada que exige Referer e destino C não autorizado. Não relaxar TLS nem criar nova barreira global. O ficheiro de propostas precisava de desenvolver este subcaso, além dos cookies.

## 6. Auditoria das recomendações complementares e dos relatos

| Proposta complementar | Resultado |
| --- | --- |
| Aumentar buffer PCM por causa do #3210 | Não reaplicar: correções relevantes já constam da 1.11.1 |
| Portar o fix LL-HLS #3311 | Já presente; usar como regressão |
| Forçar software, PCM ou passthrough para todos | Rejeitar como default universal; manter capacidades e exceções verificadas |
| Aumentar buffers/purgar memória por suposição | Não aprovar; medir Java/native/graphics primeiro |
| Reset periódico, ligações de reserva paralelas ou delays fixos novos | Rejeitar para o plano imediato |
| Fases de testes e integração do Core | Manter, com os limites e correções desta auditoria |

O [#3210](https://github.com/androidx/media/issues/3210) foi reproduzido pelo mantenedor e levou a correções já integradas. O [#3522](https://github.com/androidx/media/issues/3522) descreve seek/passthrough noutro equipamento/versão; não demonstra o corte periódico AAC/MP2 na Xiaomi.

Os fóruns [CoreELEC, cortes periódicos](https://discourse.coreelec.org/t/audio-dropouts-every-4-minutes-in-mp4-playback/9787) e [Ugoos, 44,1 kHz](https://discourse.coreelec.org/t/periodical-audio-dropouts-when-outputting-44-1-khz-via-hdmi/54477) contêm observações úteis, mas são outros sistemas de áudio, hardware e condições. Não portar configurações ALSA/driver como patch Android.

As referências [Android TV áudio](https://developer.android.com/training/tv/playback/audio-capabilities) e [memória](https://developer.android.com/training/tv/playback/memory) sustentam negociação e medição. Não foi encontrado um relato primário que comprove exatamente a causa Xiaomi descrita.

## 7. Ordem de execução revista

1. A01, com regressão A03; ensaio residual A02 e correção mínima necessária, sem rearmar resgates ilimitados.
2. Fixture conjunta H01/H02; H04/H05 podem terminar validados sem mudança funcional. H03 exige teste próprio e distinção de origem.
3. H06/H07/T01 separadamente; T02 só perante necessidade demonstrada.

Antes de integrar: atualizar fonte fixada da App, verificar as fontes efetivamente compiladas e executar checks dirigidos. Aceitação física separada, com reprodução prolongada e falhas controladas. Não transformar testes propostos em resultados nem prometer percentagens.

**Conclusão operacional:** existem soluções oficiais e correções públicas relevantes, mas a maioria já está parcialmente presente ou depende de condições específicas. A auditoria aprova uma correção contratual imediata e restringe as restantes ao menor trabalho demonstravelmente necessário.
