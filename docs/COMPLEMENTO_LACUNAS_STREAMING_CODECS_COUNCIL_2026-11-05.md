# Complemento crítico — lacunas de streaming, codecs e sincronização

Data: 5 de novembro de 2026. Âmbito: OwnTV Android TV personalizado.

**Estado: análise e recomendações; nenhuma alteração funcional aplicada.**

**Prioridade de 07/11/2026:** a secção 16.7 do plano principal exige preservar compatibilidade e fluidez. P10/P11 passaram a «testar primeiro»: não aplicar recusas novas só por metadados do manifesto/cifragem. A classificação vigente é 4 manter, 10 reduzir, 10 testar primeiro e 6 adiar; as descrições históricas abaixo não autorizam implementação automática.

**Classificação revista:** a secção 16 do [plano principal](C:/Users/renat/Downloads/OwnTV-main/docs/PLANO_IMPLEMENTACAO_COUNCIL_HLS_AUDIO_CACHE_2026-11-03.md) é a decisão vigente após reanálise crítica das 30 propostas. Algumas recomendações abaixo foram reduzidas ou passaram a exigir protótipo; não executar a ordem histórica deste complemento sem consultar essa revisão. Em especial, N26 preserva a espera aplicável sem promover todo erro terminal de segmento a reabertura da gravação, e N27 continua condicionado a ensaio.

**Mapa exato integrado:** a secção 14 do [plano principal](C:/Users/renat/Downloads/OwnTV-main/docs/PLANO_IMPLEMENTACAO_COUNCIL_HLS_AUDIO_CACHE_2026-11-03.md) identifica, para N18–N24 e R01–R03, os ficheiros/métodos, o que alterar/remover/adicionar, testes existentes e nomes novos propostos. Para execução e limites de âmbito, consultar essa secção; este complemento mantém a fundamentação. O laboratório já tem uma base em `C:/Users/renat/Downloads/OwnTV-main/tools/performance/local_iptv_fixture.py`, a estender sem duplicar a ferramenta.

## 1. Objetivo e método

Revisão pelo agente principal e três agentes especializados: rede/HLS, áudio/sincronização e vídeo/codecs. Os pareceres foram cruzados com o código e com as auditorias locais, incluindo o plano P01–P17 de 03/11. Não se afirma ter examinado todas as conversas ou todas as execuções históricas de agentes. «Novo» significa lacuna específica não encontrada nos documentos consultados; pendências antigas identificadas estão separadas.

Fontes editáveis verificadas:

- App: `C:/Users/renat/Downloads/OwnTV-main`.
- Core: `C:/Users/renat/Downloads/OwnTV_Core`.
- Plano anterior: `C:/Users/renat/Downloads/OwnTV-main/docs/PLANO_IMPLEMENTACAO_COUNCIL_HLS_AUDIO_CACHE_2026-11-03.md`.

Snapshot atual da App: `70aab7a373d8d6dc8449eda6a3c3e720118387e196a81fa0222f15710456ceb9`, 1089 ficheiros. Foram comparados com esse ZIP sete ficheiros relevantes: LivePreviewEngine, OwnTVPlayer, AudioDelayClock, FfmpegAudioDecoder, FfmpegAudioRenderer, StreamHeaders e LocalTimeshiftSession; todos coincidem byte a byte com os correspondentes no checkout Core nesta revisão. Isto não valida APK instalado nem todos os ficheiros do checkout.

Não foram feitos pedidos ao fornecedor, builds, testes de reprodução ou ensaios em boxes. A causa dos cortes de áudio Xiaomi continua por demonstrar. Os achados mpv não explicam automaticamente sintomas em ExoPlayer.

## 2. Decisão consolidada

| ID | Proposta | Evidência / decisão |
| --- | --- | --- |
| N18 | Identidade HTTP coerente no diferido e timeshift | Omissão concreta em percursos específicos; correção pontual recomendada. A categoria Referer já tinha sido discutida anteriormente. |
| N19 | Invalidar FPS antigo quando a cadência muda | Lacuna concreta de estado; correção recomendada com testes e sem nova sintonia. |
| N20 | Restringir aprendizagem automática de timestamps mpv | Algoritmo aceita avisos espaçados/genéricos; correção de critério recomendada, mantendo exceções realmente necessárias. |
| N21 | Medir atraso agregado do renderer de vídeo | Nova instrumentação complementar, sem recuperação automática. |
| N22 | Validar o relógio A/V completo | Lacuna de testes de integração; sem bug de sincronismo confirmado. |
| N23 | Validar mudanças de formato/layout de áudio durante a emissão | Lacuna de testes e compatibilidade; sem patch FFmpeg/JNI aprovado. |
| N24 | Laboratório HLS determinístico para regressões | Ferramenta de validação proposta; sem carga extra durante o uso normal. |
| R01 | Headers com vírgula descartados pelo mpv | Pendência antiga de 19/10 agora fundamentada; não contar como descoberta inteiramente nova. |
| R02 | Cookies transitórios de sessão HLS | Compatibilidade condicionada a necessidade demonstrada; não ativar globalmente. |
| R03 | Edit lists MP4 ignoradas no percurso progressivo | Investigação condicionada; não atribuir este comportamento ao HLS nem remover flags globalmente. |

Este complemento não substitui P01–P17 nem autoriza implementação. N18–N20 são correções pontuais propostas; N21 é diagnóstico; N22–N24 são trabalho de validação. R02/R03 permanecem condicionadas. Não apresentar esta tabela como dez bugs confirmados.

## 3. N18 — Referer da lista não chega a alguns percursos internos

**Ficheiro principal:** `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/features/live/LiveViewModel.kt`.

Âncoras verificadas:

- Direto normal, aproximadamente linha 2145: usa `StreamHeaders.forSource(source, channel.httpHeaders)`.
- Diferido externo, aproximadamente linha 2579: usa o mesmo construtor.
- `watchWithLocalTimeshift`, aproximadamente linha 351: descodifica apenas `channel.httpHeaders`.
- `playCatchupProgramme`, aproximadamente linha 2641: passa apenas `ch.httpHeaders`.
- Percurso de arquivo/recuo, aproximadamente linha 2856: passa apenas `ch.httpHeaders`.

**Contrato existente:** `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/network/StreamHeaders.kt`, `forSource`, aproximadamente linha 18, já combina Referer da fonte e overrides do canal, excluindo Stalker.

**Gatilho:** servidor exige o Referer definido na lista e esse valor não foi repetido em cada canal. Direto pode funcionar, enquanto diferido interno ou produtor local recebe outra resposta, por exemplo 403. Não se demonstrou que o fornecedor atual imponha esta condição.

**Alteração mínima futura:** aplicar a mesma montagem de identidade nesses três percursos, obtendo a fonte completa onde hoje se consulta apenas o User-Agent. No timeshift, o header da origem pertence aos pedidos upstream do produtor; não o transportar inadvertidamente para o URL loopback local. Preservar overrides do canal, exclusão Stalker, claims e fecho antes da transição. Não alterar decoder, buffer ou retry.

**Teste necessário:** servidor local que exige Referer no manifesto, segmentos e arquivo; verificar override do canal e ausência de mistura entre fontes. O teste existente `C:/Users/renat/Downloads/OwnTV_Core/core/src/test/java/tv/own/owntv/core/network/SourceStreamHeadersTest.kt` cobre o construtor, mas não substitui prova dos chamadores App.

**Deduplicação:** a semântica geral do Referer já foi discutida na auditoria de 13/10. O contributo desta revisão é identificar os percursos que continuam sem usar o contrato adotado, não inventar novamente essa funcionalidade.

## 4. N19 — FPS medido antigo pode sobreviver a novo formato

**Ficheiro:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt`.

`onVideoInputFormatChanged` (~476) só regista o formato. `displayFps` (~800) prefere `measuredPtsFps`; `fpsFastRefresh` (~983) termina depois de obter uma medição precisa. O valor publicado em `_videoFps` alimenta AFR pela App em `C:/Users/renat/Downloads/OwnTV-main/app/src/main/java/tv/own/owntv/player/MpvVideoSurface.kt` (~182).

**Gatilho:** a cadência/formato muda durante a mesma emissão, por exemplo 25↔50 fps, sem nova sintonia. O valor medido anteriormente pode continuar a prevalecer.

**Alterar futuramente:** invalidar o estado de medição associado ao formato antigo e rearmar uma janela limitada quando houver mudança relevante de cadência/identidade de formato. Preservar ownership, limites de amostragem e cooldown AFR. Não reiniciar a medição em cada segmento nem forçar uma mudança HDMI por qualquer variação de bitrate/resolução. Há precedente de reset no próprio Core: `ExoSubtitleEngine.kt`, `onVideoInputFormatChanged` (~205).

**Testes:** 25→50, 50→25, taxa fracionária, formato sem FPS declarado, mudança só de bitrate e evento antigo após zapping. Confirmar atualização do fluxo AFR sem reconexão HTTP. Frequência prática do problema nas boxes não foi medida.

## 5. N20 — Heurística de PTS mpv demasiado abrangente

**Ficheiro:** `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt`.

`BROKEN_PTS_RX` (~466) aceita `invalid video timestamp`, `non-monotonic` e `desynchroni`. `noteBrokenTimestamp` (~1504) acumula 20 correspondências sem janela temporal e aplica `video-sync=desync`/`framedrop=no`. A aprendizagem também influencia `correct-pts` na próxima abertura (~820).

**Problema de critério:** o comentário refere avisos densos durante poucos segundos; o algoritmo aceita igualmente avisos espaçados ao longo de uma sessão longa. Uma mensagem genérica de dessincronização também não demonstra PTS de vídeo inválido. A aprendizagem é por stream em LiveStreamQuirks, não por host; não alegar contaminação geral do domínio.

**Alterar futuramente:** classificar a origem/tipo do aviso; exigir densidade numa janela limitada e limpar evidência após período saudável. Associar contagem e ação à geração atual. Dessincronização genérica serve inicialmente para diagnóstico, sem desligar sincronismo por si só. O tamanho da janela/limiar novo deve vir de fixtures e não de um número inventado nesta análise.

**Preservar:** workaround para streams comprovadamente patológicos. Não trocar globalmente para outro `video-sync`, remover toda a aprendizagem ou aplicar esta regra ao ExoPlayer. A opção desync deixa de sincronizar vídeo com áudio; por isso merece um critério mais estreito que uma tentativa comum de recuperação.

**Testes:** 20 avisos dispersos não ativam; sequência densa comprovada pode ativar; aviso de áudio genérico não decide PTS vídeo; evento tardio não afeta nova sintonia; reabertura preserva apenas aprendizagem válida. Nenhum teste destes foi executado nesta revisão.

## 6. N21 — Observar atraso de vídeo sem culpar automaticamente a rede

**Ponto:** listener `analytics` de LivePreviewEngine. O código e as auditorias pesquisadas não usam `onVideoFrameProcessingOffset`.

A assinatura foi verificada no sources JAR local Media3 1.11.1: `onVideoFrameProcessingOffset(EventTime, long totalProcessingOffsetUs, int frameCount)`. O contrato distingue frames disponíveis antes/depois do prazo de renderização. É um resumo agregado, não prova de apresentação física no painel.

**Adicionar futuramente:** resumo por sintonia/formato/decoder, média derivada apenas quando `frameCount > 0` e contadores úteis já disponíveis. Usar limites e redação de P07; sem callback novo por fotograma, timer paralelo ou escrita contínua.

**Benefício:** perante buffer saudável, separar atraso do decoder/saída de falta de segmentos. Não reiniciar canal, mudar hardware/software ou reduzir qualidade com base num único valor.

## 7. N22 — Relógio A/V: falta validar integração, não apenas aritmética

**Ficheiros:**

- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioDelayClock.kt`, `shift`/`DelayedClockAudioSink.getCurrentPositionUs` (~27/33).
- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/test/java/tv/own/owntv/player/AudioDelayClockTest.kt`.

Os cinco testes atuais verificam soma, sinal, valor não definido e limites. O sink fornece o relógio consumido pelos renderers áudio Media3; falta uma prova integrada dos efeitos em posição, seek, velocidades e transições de saída.

**Adicionar apenas ensaios:** sinal A/V conhecido, 0/±150/±500 ms, direto/diferido, 1× e velocidades alternativas nos diferidos, pause/play, seek, limites do conteúdo e mudança HDMI/Bluetooth. Comparar posição reportada e sincronismo percebido. Não alterar fórmula, default ou amostras sem divergência reproduzida. Não confundir desvio configurado com drift que cresce ao longo do tempo.

## 8. N23 — Mudanças de áudio durante programas/publicidade

**Ficheiros:**

- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ffmpeg/FfmpegAudioDecoder.java`, `hasOutputFormat` (~143).
- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/ffmpeg/FfmpegAudioRenderer.java`, `getOutputFormat` (~174).
- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/StereoPcmAudioProcessor.kt`.

O decoder guarda frequência/contagem após a primeira saída; o formato Java de saída não explicita máscara de layout. Isto não prova bug: Media3 pode recriar corretamente o decoder quando a mudança é sinalizada, e o padrão é herdado de upstream.

**Adicionar ensaios:** AAC LC↔HE-AAC, 24↔48 kHz, estéreo↔5.1, mudança PID/faixa e layout não padrão. Separar stream corretamente sinalizado de stream inválido. Comparar plataforma/FFmpeg/mpv com o mesmo conteúdo, verificar centro/diálogo, formato PCM e ciclo de vida do decoder.

Não alterar JNI, forçar 48 kHz, software ou reconstrução do player apenas por esta leitura. Sem fontes nativas correspondentes ao binário, nenhuma alteração JNI fica aprovada. Não atribuir automaticamente os cortes Xiaomi a esta hipótese.

## 9. N24 — Laboratório de regressão HLS

Proposta de ferramenta de desenvolvimento, não outra funcionalidade no menu. Servidor/fixtures locais e sintéticos permitem repetir: headers obrigatórios, erros com corpo lento, 429/503, redirects, discontinuidades, troca 25/50 fps, troca de áudio, cancelamento e A–B–A.

Reutilizar os ensaios já criados em 03/11; não fingir que ainda não existem testes de transporte. A inovação é juntar carga de media reproduzível, identidade de sessão e asserções de ciclo de vida, com cenários para ExoPlayer e mpv nas boxes. Não abrir canais reais vizinhos, guardar credenciais ou instalar um servidor permanente na app de produção.

Critérios de valor: reproduzir a mesma falha antes/depois, não degradar os cenários saudáveis, contar pedidos/fechos e medir áudio/vídeo de referência. Nenhuma percentagem de robustez é atribuída sem esses resultados.

## 10. Pendências e propostas condicionadas

### R01 — Headers com vírgula no mpv

`StreamHeaders.kt`, `toMpvHeaderFields` (~78), elimina todo o header se o valor contiver vírgula. `OwnTVPlayer.kt` (~3582) envia a string resultante à propriedade `http-header-fields`.

O descarte é confirmado; foi já anotado em `C:/Users/renat/Downloads/OwnTV-main/implementar_rena/ANALISE_OWNTV_2026-10-19.md` (~743/1118). Não contar como descoberta nova.

Recomendar serialização correta da lista mpv ou API de item/nó suportada pela versão concreta, provada com servidor local. Não basta remover o filtro: a vírgula sem escaping dividiria a lista. Preservar limpeza de headers antigos por canal e proteção contra CR/LF.

### R02 — Cookies de sessão HLS

Não foi encontrada CookieJar no cliente global de `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/di/DataModule.kt`; cookies manuais são outro mecanismo. Se uma origem devolver Set-Cookie obrigatório para segmentos/chaves, a compatibilidade merece ensaio.

Só perante esse caso: armazenamento em memória, limitado e isolado por fonte/conta/sessão, regras Domain/Path/Secure/expiração e precedência do Cookie manual. Não criar jar global partilhada com imagens/APIs nem copiar cookies para qualquer CDN. Sem evidência, manter pendente. Não resolve por definição o intervalo imposto pelo fornecedor em A–B–A.

### R03 — Edit lists MP4

LivePreviewEngine (~3559–3560) configura `IGNORE_EDIT_LISTS` na fábrica progressiva; HLS usa outra fábrica (~3562). Avaliar com arquivos MP4 que dependam desses metadados, preservando os casos que motivaram o workaround. Não anunciar correção HLS/fMP4 nem remover flags globalmente. Prioridade inferior para utilização predominantemente HLS.

## 11. Ideias recusadas como solução geral

- Mais buffer para corrigir qualquer corte de som ou criar detalhe de imagem.
- Flags TS permissivas, troca para software, 48 kHz ou desentrelaçamento pesado em todos os canais.
- Banda nova de velocidade no direto sem verificar o comportamento Media3 atual.
- Reiniciar áudio por silêncio/underrun isolado ou mudar o codec sem evidência.
- Alargar TLS automático a todos os motores por simples ausência de tags: `docs/AUTO_TLS_FALLBACK_V2_2026-10-31.md` define deliberadamente o âmbito ExoPlayer Live.
- Apresentar novamente o foco áudio como ausente: PlaybackSession já tem estados, proprietário e epoch.
- Afirmar que upscaling beta está pronto: a pendência `docs/MELHORIAS_FUTURAS_PENDENTES.md` foi consultada. Continua a exigir comparação com o escalonamento da box/TV e medição de GPU/RAM/latência; buffer maior não cria resolução.

## 12. Ordem recomendada se houver autorização posterior

1. N18 + N19: corrigir omissões determinísticas, com testes isolados dos chamadores e do estado de FPS.
2. N20: reproduzir o falso disparo e apertar a heurística mpv; preservar o cenário patológico verdadeiro.
3. N21: incorporar a métrica agregada ao diagnóstico existente, coordenada com P07/P08.
4. N22 + N23: testes de integração áudio; nenhuma recuperação nova sem resultado.
5. N24: consolidar fixtures e matriz física. R01 pode entrar num lote pequeno próprio após testar a serialização libmpv. R02/R03 continuam opcionais/condicionadas.

Manter volume 100%, preferências e overrides, ordem de fecho/abertura, budgets/cooldowns, RAM/disco, TLS estrito primeiro e regras das fases P01–P17. Este complemento não autoriza implementar todas as linhas por arrastamento.

## 13. Fontes primárias consultadas

- [Media3: seleção de faixas](https://developer.android.com/media/media3/exoplayer/track-selection).
- [Media3: direto e ajuste de velocidade](https://developer.android.com/media/media3/exoplayer/live-streaming).
- [Media3: AnalyticsListener](https://developer.android.com/reference/androidx/media3/exoplayer/analytics/AnalyticsListener), cruzado com o sources JAR local 1.11.1.
- [Media3: troubleshooting e edit lists](https://developer.android.com/media/media3/exoplayer/troubleshooting).
- [Manual mpv: sincronismo](https://mpv.io/manual/stable/#options-video-sync).
- [Manual mpv: headers HTTP](https://mpv.io/manual/stable/#options-http-header-fields).
- [OkHttp 5.5.0: configuração de CookieJar](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt).

Documentação web pode evoluir. Uma API nova do manual não é prova de disponibilidade no libmpv instalado. Confirmar versão e integração concreta antes de escolher a implementação.

## 14. Segunda ronda — continuação em 05/11/2026

Foi realizada outra revisão com três agentes especializados em continuidade/faixas, recuperação/ciclo de vida e parser/captura. O mapa exato de ficheiros e operações está agora na **secção 15 do plano principal**. Nenhuma implementação foi feita.

- **N25 — Gatilho de recuperação de rede:** LivePreviewEngine trata qualquer emissão `online=true` como restauro; uma alteração só de `metered` na mesma rede pode renovar o orçamento de uma sessão terminal ainda presente. Correção proposta apenas no consumidor, preservando ownership, pausa e cooldown.
- **N26 — Espera do fornecedor nas gravações:** HlsSegmentRetry devolve false para situações distintas; RecordingEngine pode saltar o segmento e pedir o seguinte apesar de uma espera aplicável. O suporte a Retry-After já existia; falta conservar o resultado até ao produtor/coordenador.
- **N27 — Gravação sem novos commits:** manifesto parado ou reset de sequência pode manter a captura ativa até ao fim agendado. Primeiro fixture/observação; prazo e recuperação ficam condicionados. A marcação PARTIAL por HlsCaptureCompleteness já existe e deve ser preservada.

N26/N27 dizem respeito ao produtor de gravações, não ao HLS direto do ExoPlayer. A revisão de faixas não encontrou outro bug novo suficientemente forte: descartou duplicados de handover/definições e conservou apenas hipótese de teste em reprepare com TrackGroup diferente. Não foram encontrados novos defeitos comprovados de Surface/standby que justifiquem reescrever esses percursos.
