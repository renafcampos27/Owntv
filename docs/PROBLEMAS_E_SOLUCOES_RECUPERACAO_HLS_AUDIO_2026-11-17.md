# Recuperação HLS e áudio — problemas, soluções e evidência

Data: 17/11/2026. Projeto: OwnTV Android TV personalizado.

**Estado: A01 implementado; restantes propostas pendentes ou condicionadas.** Este documento consolida as revisões anteriores e identifica o que já é solução conhecida, o que existe na OwnTV e o que falta comprovar na box. O registo de execução e validação do A01 está em [IMPLEMENTACAO_A01_AUDIO_2026-11-17.md](C:/Users/renat/Downloads/OwnTV-main/docs/IMPLEMENTACAO_A01_AUDIO_2026-11-17.md).

**Auditoria independente posterior:** consultar [o parecer dos 12 itens](C:/Users/renat/Downloads/OwnTV-main/docs/AUDITORIA_ITEM_A_ITEM_HLS_AUDIO_2026-11-17.md), que prevalece no grau de aprovação. A01 permanece diretamente recomendada; A02 passa a ensaio residual e correção mínima, não a refactor antecipadamente aprovado.

## 1. O que significa «comprovado» neste documento

| Grau | Significado |
| --- | --- |
| **Confirmado no código** | Existe um percurso verificável na fonte usada pela App. Não prova que causou o sintoma observado na Xiaomi. |
| **Solução documentada** | A API, especificação ou implementação oficial estabelece o tratamento recomendado. Ainda é preciso validar a integração personalizada. |
| **Correção upstream reproduzida** | Os mantenedores identificaram e corrigiram um caso concreto. Só se aplica à OwnTV se a versão e as condições coincidirem. |
| **Proposta condicionada** | Tem fundamento técnico, mas ainda não demonstrou benefício neste projeto. Não ativar por suposição. |

**Não existe uma solução universal comprovada para qualquer falha IPTV.** Se o fornecedor deixar de disponibilizar conteúdo ou recusar a conta, a App pode aguardar ou voltar a tentar nas condições permitidas; não pode produzir o conteúdo em falta. O objetivo é recuperar automaticamente quando possível, sem provocar interrupções ou pedidos desnecessários.

Não foram feitos novos testes de reprodução, builds, APK ou ensaios físicos para criar este ficheiro. As soluções ainda não estão comprovadas na Xiaomi. Os casos de teste abaixo são requisitos de aceitação, não resultados obtidos.

## 2. Base técnica verificada e âmbito

- App: `C:/Users/renat/Downloads/OwnTV-main`.
- Core editável: `C:/Users/renat/Downloads/OwnTV_Core`.
- Lock: [core-source-lock.json](C:/Users/renat/Downloads/OwnTV-main/core-source-lock.json).
- Snapshot: `dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`, 1117 ficheiros.
- Dependências analisadas: Media3 **1.11.1** e OkHttp **5.5.0**.

Os ficheiros centrais usados na revisão foram comparados com o ZIP fixado pela App. Confirmar novamente o lock e os símbolos antes de implementar; linhas e versões podem mudar. O APK instalado na box não foi inspecionado.

Complemento técnico: [revisão crítica completa](C:/Users/renat/Downloads/OwnTV-main/docs/REVISAO_CRITICA_CONTINUIDADE_HLS_AUDIO_2026-11-17.md). Este documento torna explícita a recuperação HLS, além das correções de áudio.

## 3. Decisão resumida

| ID | Problema/caso | Solução e decisão |
| --- | --- | --- |
| A01 | Aviso de áudio desencadeia pausa/rebuild | Corrigir; usar o callback como diagnóstico e preservar falhas efetivas |
| A02 | Decisão antiga de áudio afeta saída recuperada | Testar o caso residual após A01; corrigir minimamente se demonstrado |
| A03 | Erro fatal real de áudio/decoder | Preservar recuperação existente e testar; não eliminar ao corrigir A01 |
| H01 | Segmento HLS temporariamente indisponível | Preservar retries/fallbacks Media3; testar a especialização OwnTV |
| H02 | Erro HTTP recebe reconexão rápida inadequada | Distinguir papel, status e repetição; corrigir casos demonstrados |
| H03 | Reprodução ficou fora da janela do direto | Tratamento específico conhecido; avaliar integração limitada ao direto |
| H04 | Ligação efetivamente perdida ou presa | Preservar/validar reconexão coordenada e cancelamento; não criar outro retry engine |
| H05 | Servidor exige espera ou limita sessões | Preservar prazo e uma tentativa ativa; harmonizar indicação e abandono |
| H06 | Watchdog interrompe pedido que recuperaria | Hipótese a testar; não aumentar 12 s globalmente |
| H07 | Descarga parcial e mudança de variante corrompem posição | Correção upstream conhecida; reproduzir antes de integrar versão/patch |
| T01 | Socket reutilizado deixou de funcionar | Experiência playback-only com OkHttp; não ativar globalmente |
| T02 | Origem exige cookies ou cabeçalhos de CDN | Compatibilidade condicional, isolada por fonte/conta |

## 4. Áudio

### A01 — aviso recuperável tratado como falha

**Problema confirmado:** `AudioWatchdog.onAudioSinkError` chama `raise()` para quase todos os erros, exceto descontinuidade. O consumo dessa decisão pode pausar toda a reprodução ou forçar estéreo e reconstruir o leitor. O aviso pode chegar antes de o ExoPlayer concluir a própria recuperação.

**Solução documentada:** manter o evento como diagnóstico; não executar uma recuperação da App apenas por esse aviso. O contrato Media3 distingue-o de `onPlayerError`. [Contrato oficial](https://developer.android.com/reference/kotlin/androidx/media3/exoplayer/analytics/AnalyticsListener).

**Alterar/remover:** retirar de `onAudioSinkError` a promoção direta para `raise` destrutivo. Manter logs e tratamento das falhas efetivas. Não desativar todo o watchdog nem todos os mecanismos de resgate.

**Onde:**

- [AudioOutputPolicy.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt:397): `onAudioSinkError`, `raise`, comentários.
- [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:1034): validar o consumo, pausa e rebuild.

**Aceitação:** aviso seguido de avanço normal não provoca pausa, estéreo global ou reabertura HTTP; falha fatal atual continua a chegar à recuperação.

**Limite:** esta correção evita uma intervenção indevida. Não repara automaticamente pacotes de áudio ausentes no fornecedor, uma falha HDMI ou qualquer defeito de firmware.

### A02 — decisão pendente depois de a saída recuperar

**Problema confirmado:** `pendingReason` pode continuar preenchido depois de um novo AudioTrack ou novo avanço. `poll()` devolve a razão antes de verificar o estado de avanço atual.

**Solução recomendada:** vincular a decisão à sintonia e saída relevantes; invalidar/reavaliar a decisão quando a condição que a justificava deixou de existir. Não limpar todos os contadores a cada avanço, porque isso esconderia falhas recorrentes.

**Onde:** [AudioOutputPolicy.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt:304): eventos de avanço/criação/libertação e `poll`.

**Aceitação:** erro da saída antiga → nova saída → avanço → consulta de saúde não executa um resgate obsoleto. Libertação atrasada de uma saída antiga não desarma a atual.

**Grau:** percurso antigo confirmado no código; a alteração exata de estado precisa de testes. Não apresentar como correção de hardware comprovada.

**Restrição da auditoria:** A01 elimina um produtor de decisões pendentes. O caso residual identificado vem de underruns passthrough. Não limpar só `pendingReason` deixando `fired` permanentemente ativo; também não limpar ambos indiscriminadamente rearmando resgates. Usar a identidade existente e especificar a transição completa apenas para o caso demonstrado.

### A03 — manter a recuperação de falhas reais

**Problema:** retirar A01 de forma incompleta poderia deixar erros fatais sem tratamento.

**Solução documentada:** tratar `onPlayerError`, que pode exigir nova preparação, e respeitar intenção de reprodução/identidade. [Eventos do leitor](https://developer.android.com/media/media3/exoplayer/listening-to-player-events).

**Estado OwnTV:** existe handler e recuperação em [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:1587). Confirmar que um erro atual não é confundido com callback antigo. Um rebuild integral também reabre rede; deve ser reservado a necessidade real.

**Não adicionar:** reset periódico de áudio, deteção de falha pela amplitude, correção automática de relógio ou software forçado globalmente. O silêncio do programa pode ser legítimo; o relógio de áudio participa na sincronização do vídeo.

**Testes existentes a estender:** [AudioRecoveryTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/AudioRecoveryTest.kt). O teste que chama um aviso de sink de «falha efetiva» deve ser reespecificado pelo contrato, sem retirar cobertura do erro fatal verdadeiro.

## 5. Recuperação da transmissão HLS

### H01 — falhou um segmento, mas a transmissão pode continuar

**Problema:** um fragmento pode falhar temporariamente; reiniciar imediatamente toda a fonte pode perder buffer válido e repetir autenticação/sessão.

**Solução conhecida e já implementada na biblioteca:** Media3 dispõe de retries e fallback/exclusão de variantes ou localizações elegíveis. A OwnTV deve preservar essa recuperação antes de reagir ao erro fatal. [Personalização da política](https://developer.android.com/media/media3/exoplayer/customization#customizing-error-handling).

**Estado OwnTV:** `edgeRefusalPolicy` conserva fallbacks, mas especializa alguns segmentos 403/404/410 com uma repetição de 500 ms. Não remover essa especialização sem comparar o caso que procura resolver.

**Auditoria adicional:** há outro caminho: recusas de duas URLs distintas com o mesmo status podem sinalizar `segmentsRefused` e pedir handover antes do erro fatal. Testar em conjunto retry curto, sinal de recusas e reconexão runtime; não assumir que todo o controlo OwnTV espera pelo erro fatal Media3.

**Onde:** [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:3478).

**Melhorar:** correlacionar erro de load, retry/fallback interno, erro fatal e reprepare. Reutilizar os logs existentes. O mínimo de retries da biblioteca não é um teto universal; oito reconexões da App não significam oito pedidos HTTP.

**Aceitação:** um segmento isolado que volta a ficar disponível recupera sem uma cadeia de reaberturas desnecessárias. Uma falha persistente continua a terminar ou avançar segundo a política definida.

### H02 — autenticação, manifesto, segmento e sessão recebem tratamento demasiado semelhante

**Problema confirmado no código:** após reprodução, o ramo HLS de mau status HTTP é abrangente e pode limitar a espera de reconexão a 1,5 s. As regras específicas de sessão no arranque não têm exatamente o mesmo percurso runtime.

**Solução recomendada:** preservar o papel do pedido, status, identidade e histórico curto de progresso. Distinguir erro transitório de segmento, recusa persistente da entrada, espera explícita e sessão ocupada. Usar os controladores existentes.

**Onde:** [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:1602): `onPlayerError`, `reconnect`, `hlsHttpReconnectDelayMs`, `retryAfterSessionRelease`; reutilizar `MediaLoadRole` e evidência da fonte atual.

**O que ainda não está comprovado:** qual mudança de ritmo beneficia o fornecedor atual. Não tornar todo 403 terminal, não interpretar 458 como norma HTTP universal e não adivinhar espera remota a partir de um código.

**Aceitação:** comparar 403 recuperável de segmento, 401/403 persistente de entrada, 458 antes/depois de READY e 503 com/sem prazo. Preservar a recuperação útil e reduzir insistência redundante, sem criar recusa nova dos canais saudáveis.

### H03 — a posição ficou para trás da janela disponível do direto

**Problema conhecido:** após pausa ou carregamento prolongado, os segmentos dessa posição podem já não existir. Repetir a posição antiga não os recupera.

**Solução documentada pelo Android:** para `ERROR_CODE_BEHIND_LIVE_WINDOW`, regressar à posição padrão válida e preparar novamente. [Exemplo oficial](https://developer.android.com/media/media3/exoplayer/live-streaming).

**Estado OwnTV:** existe reconexão genérica que recria a fonte; não é ausência total de recuperação. Avaliar se uma recuperação específica reduz trabalho.

**Alteração candidata:** dentro do handler atual, reconhecer exclusivamente esta condição, na fonte atual e no modo direto; utilizar a posição padrão válida e a preparação existente, sem criar outro ciclo paralelo.

**Onde:** [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:1587) e integração com os watchdogs atuais.

**Restrições:** não enviar um programa em diferido para o direto; não aplicar esta resposta a qualquer 404; preservar pausa e saída do leitor. Mesmo em direto, o utilizador poderá perder a posição antiga se já não existir no servidor.

**Identificação obrigatória:** `LivePreviewEngine.isLiveContent` é constante true e o mesmo motor serve timeshift local. Essa flag não basta. Distinguir direto do fornecedor, buffer local e arquivo antes de escolher a posição de recuperação.

**Aceitação:** janela HLS avança enquanto o cliente fica parado; ao retomar, recupera numa posição válida uma única vez, sem ressuscitar sintonia abandonada. Comparar com a recuperação genérica atual antes de integrar.

### H04 — a ligação morreu ou ficou presa

**Problema:** a biblioteca pode já ter esgotado a recuperação daquele carregamento, ou a reprodução pode ficar sem progresso útil.

**Solução existente a preservar e validar:** uma única reconexão coordenada, com cancelamento da tentativa antiga, preparação de fonte atual e limites finitos. No erro fatal, Media3 permite nova preparação; isso não exige reconstruir o ExoPlayer em todos os casos.

**Estado OwnTV:** já existem reconexão, watchdogs, escada, ownership e fecho local. Não adicionar outro motor de retries.

**Onde:**

- [LivePreviewEngine.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt:2295): `reconnect` e `reprepare`.
- [LiveTuneController.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveTuneController.kt:795): coordenação e fallback permitido.
- [PlaybackHttpCalls.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/PlaybackHttpCalls.kt): cancelamento e fecho dos corpos.
- [SourceDrain.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/SourceDrain.kt): verificação de recursos pendentes.

**Preservar:** os 300 ms são intervalo de reavaliação quando há recursos pendentes, não atraso obrigatório. O fecho local não prova que o fornecedor já libertou a sessão remota. Não contornar o gate ou somar novos delays fixos.

**Aceitação:** servidor interrompe corpo/ligação durante reprodução e volta antes do abandono terminal. A App recupera se a tentativa e a intenção ainda estiverem ativas; Back, zap, pausa e suspensão impedem reinício indevido. Depois de regressar à lista, não prometer continuação automática sem opção própria. Nunca coexistem duas aberturas lógicas concorrentes; pedidos legítimos de áudio/vídeo/playlist/chave não são, por si, duplicação.

### H05 — servidor exige espera ou limita sessões

**Problema:** insistir antes do prazo pode ser inútil e aumentar recusas.

**Solução padronizada:** respeitar `Retry-After` quando aplicável. [RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.3).

**Estado OwnTV:** já existem guarda e espera. Preservar [ProviderBackoffGuard.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ProviderBackoffGuard.kt) e [LiveStreamQuirks.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveStreamQuirks.kt). O prazo conhecido é guardado em memória do processo, não comprovadamente após reinício.

**Melhorar:** tornar coerentes o prazo global, a contagem apresentada e a decisão de abandonar. É válido sair à lista antes de o servidor permitir nova tentativa; é inválido anunciar uma tentativa garantida que outra camada irá cancelar ou tentar antes do prazo.

**Onde:** [LiveTuneController.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LiveTuneController.kt:722), `usesStrictDeadline` e `startAlarm`. Identificar a fase explicitamente, mantendo inicialmente os limites existentes.

**Aceitação:** `Retry-After: 40` impede pedidos prematuros durante a mesma sessão do processo, incluindo retorno à lista e nova tentativa. O resultado de UX — aguardar ou abandonar — deve ser explícito, finito e cancelável.

### H06 — o temporizador reinicia um pedido que recuperaria sozinho

**Evidência:** a OwnTV reconecta após 12 s contínuos de BUFFERING; a tolerância baseada em `startupProgress` não cobre da mesma forma a primeira interrupção de uma fonte já pronta.

**Solução ainda não comprovada neste projeto:** reavaliar progresso útil no vencimento e permitir eventualmente uma extensão única e finita. **Manter os 12 s até o teste demonstrar benefício.** Bytes de playlist/chave não provam recuperação de vídeo.

**Onde, apenas se aprovado:** `LivePreviewEngine`, `LiveExoWatchdog`, `LiveWaitBudget` e `ResponseDeadline`. O limite de 8 s de headers não cobre toda a ligação/DNS/TLS nem todo o corpo; os limites precisam de ser avaliados em conjunto.

**Aceitação:** headers chegam a tempo, corpo lento permite READY aos 13–14 s. Comparar com pedido sem progresso. Uma extensão só passa se evitar reabertura sem prolongar de forma prejudicial os casos mortos.

### H07 — correção HLS já descoberta e implementada upstream

**Problema conhecido:** certas falhas de descarga parcial seguidas de exclusão/mudança de variante podem produzir junção ou posição de recarregamento incorretas.

**Solução existente online:** o mantenedor identificou a correção [c4b16f9](https://github.com/androidx/media/commit/c4b16f9) e indicou primeira disponibilidade em 1.12.0-beta01 no [issue #3362](https://github.com/androidx/media/issues/3362). O trecho anterior ainda existe na 1.11.1 analisada.

**Decisão:** reproduzir o cenário e comparar com uma versão que contenha a correção. Se confirmado, preferir uma integração suportada e validada; não atualizar automaticamente para beta nem copiar uma linha isolada do conjunto.

**Onde:** dependência em `C:/Users/renat/Downloads/OwnTV_Core/gradle/libs.versions.toml` — confirmar a declaração efetiva antes de alterar — e testes HLS. `HlsSampleStreamWrapper` pertence à dependência, não é ficheiro Kotlin local a editar diretamente.

**Limite:** é uma solução conhecida para condições específicas, não cura demonstrada do corte periódico apenas de som na Xiaomi.

## 6. Soluções online que já temos ou que não devemos copiar globalmente

- **Áudio/underruns:** [#3210](https://github.com/androidx/media/issues/3210) originou correções já presentes na 1.11.1, com PCM perto de 500 ms e margem breve de prontidão. Não reaplicar aumento generalizado para vários segundos.
- **LL-HLS e posição antiga:** a correção do [#3311](https://github.com/androidx/media/issues/3311) já está no source analisado. Serve para regressão, não como patch novo.
- **Sockets:** `retryOnConnectionFailure(false)` é herdado do cliente base. Testar a alternativa somente em [StreamingHttpClient.kt](C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/StreamingHttpClient.kt), sem alterar catálogo. [Fonte OkHttp](https://raw.githubusercontent.com/square/okhttp/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt).
- **Cookies:** há precedente de streaming protegido no [ExoPlayer #5975](https://github.com/google/ExoPlayer/issues/5975). Só implementar se a origem atual exigir `Set-Cookie` nos pedidos seguintes; isolar fonte/conta, validade e destino. Não ativar depósito global.
- **Cabeçalhos de CDN (T02):** testar separadamente redirect e URL absoluta de segmento/chave, com destino autorizado que exige Referer e destino que não deve receber identidade sensível. Não criar restrições globais novas nem retirar proteção TLS por suposição.
- **Capacidades de áudio:** manter negociação da saída. PCM, passthrough e software não são universalmente superiores. [Android TV](https://developer.android.com/training/tv/playback/audio-capabilities).
- **Memória:** medir Java/native/graphics; mais segundos de buffer não equivalem automaticamente a maior estabilidade. [Android TV](https://developer.android.com/training/tv/playback/memory).

## 7. Fases propostas para implementação posterior

### Fase 1 — áudio e manutenção da recuperação real

Aplicar A01, validar A03 e testar o caso residual A02; corrigir apenas a transição mínima demonstrada. Corrigir comentários/testes. Não alterar formatos, buffers, volume, HDR ou preferências.

### Fase 2 — recuperação HLS com falhas controladas

Validar H01/H02/H04/H05 antes de alterar políticas. Contar pedidos e reaberturas, não só exceções. Corrigir a classificação dos casos que falham nos ensaios. Avaliar H03 em teste separado, apenas para direto.

### Fase 3 — melhorias condicionadas

Comparar H06, H07 e T01 um de cada vez. T02 apenas se houver necessidade. Não converter hipóteses em defaults.

As fases não significam que o HLS fique sem recuperação até à fase 2: os mecanismos atuais permanecem ativos. O primeiro lote evita intervenções indevidas; o segundo verifica e melhora a recuperação da transmissão propriamente dita.

## 8. Testes e conclusão de cada fase

Estender conforme o caso, sem criar testes que apenas repitam a implementação:

- [AudioRecoveryTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/AudioRecoveryTest.kt).
- [LiveTuneControllerTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveTuneControllerTest.kt).
- [LiveReconnectLadderTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveReconnectLadderTest.kt).
- [LiveResumeWatchdogsTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/LiveResumeWatchdogsTest.kt).
- [ResponseDeadlineTest.kt](C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/ResponseDeadlineTest.kt).
- [StreamingHttpClientTest.kt](C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/network/StreamingHttpClientTest.kt).
- [local_iptv_fixture.py](C:/Users/renat/Downloads/OwnTV-main/tools/performance/local_iptv_fixture.py): atualmente gera fixtures/servidor estático; estender para falhas controladas, sem afirmar que esses cenários já existem completos.

Antes de aceitar uma alteração: testar canal saudável, falha recuperável, falha permanente, mudança rápida de canal e saída/suspensão durante recuperação. Preservar apenas HLS, motor escolhido, ordem de alternativas e definições por canal.

Depois dos testes dirigidos: compilação/lint apropriados, atualização verificada do snapshot Core e ensaio físico prolongado. Medir cortes, reaberturas, sincronismo, memória e tempo de recuperação. Testes JVM não provam audibilidade HDMI nem aceitação do fornecedor.

**Estado atualizado:** A01 foi implementado posteriormente à análise, conforme o registo ligado no início. Restantes propostas não foram aplicadas. Não houve atualização de dependências ou validação física.
