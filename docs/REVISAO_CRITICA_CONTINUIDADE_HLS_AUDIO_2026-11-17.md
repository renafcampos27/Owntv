# Revisão crítica — continuidade HLS, áudio e decisões de recuperação

Data: 17/11/2026. Estado: **análise; nenhuma implementação funcional autorizada ou aplicada nesta revisão**.

**Revisão posterior:** o [parecer independente item a item](C:/Users/renat/Downloads/OwnTV-main/docs/AUDITORIA_ITEM_A_ITEM_HLS_AUDIO_2026-11-17.md) restringe a aprovação de A02 e acrescenta condições HLS/timeshift. Consultar esse parecer antes de executar recomendações históricas deste ficheiro.

## 1. Âmbito, evidência e limites

Revisões independentes por três agentes: áudio/codecs, transporte HLS/IPTV e coordenação da recuperação. O agente principal cruzou os pareceres, verificou contrapontos e integrou a decisão do council. Não se trata de uma auditoria por profissionais humanos externos.

App: `C:/Users/renat/Downloads/OwnTV-main`. Core editável: `C:/Users/renat/Downloads/OwnTV_Core`.

Fonte consumida pela App confirmada em `core-source-lock.json`: SHA-256 `dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`, 1117 ficheiros. Os ficheiros centrais de áudio, recuperação e transporte citados foram confrontados com o ZIP fixado; os analisados coincidem. O número interno de versão não estabelece equivalência com uma release upstream.

Não foram executados novos testes JVM, builds, APK, pedidos ao fornecedor ou ensaios físicos. As sequências abaixo são casos de teste propostos, não reproduções já executadas. Os achados demonstram decisões e riscos no código; **não identificam por si a causa dos cortes periódicos da Xiaomi**. Não existem percentagens medidas de melhoria.

Os números de linha são âncoras desta revisão; confirmar símbolos e fonte ativa antes de implementar. Preservar as alterações locais, os relatórios existentes, as preferências e os contratos App/Core.

## 2. Decisão consolidada

| ID | Proposta | Decisão |
| --- | --- | --- |
| C01 | Evitar recuperação destrutiva baseada apenas em `onAudioSinkError` | Recomendada; contradição direta com o contrato Media3 |
| C02 | Invalidar decisões de áudio pendentes que pertencem a uma saída já recuperada/substituída | Recomendada em conjunto com C01; não apagar histórico indiscriminadamente |
| C03 | Distinguir papel do pedido e recusa antes da reconexão HLS rápida | Testes e contexto recomendados; alterar a política apenas nos casos demonstrados |
| C04 | Coordenar limites de abertura/recuperação e esperas do servidor | Contrato e testes recomendados; mudança de comportamento condicionada ao resultado |
| C05 | Tolerância finita por progresso útil no primeiro BUFFERING prolongado | Testar primeiro; manter 12 s até demonstrar benefício |
| C06 | Recuperação de socket pelo OkHttp apenas no cliente de reprodução | Experiência condicionada; não ativar globalmente |
| C07 | Continuar a tentar depois de abandono terminal | Opcional futura, desativada por defeito; altera o requisito de regressar à lista |
| C08 | Cookies transitórios de sessão HLS | Condicional a origem que os exija; não ativar globalmente |
| C09 | Política coerente de cabeçalhos entre origem, redirect e CDN | Investigar/testar; preservar compatibilidade sem divulgar identidade a destinos arbitrários |
| X01 | Nova separação entre arranque e reprodução | Não implementar: já existe |
| X02 | Reinício periódico, pedidos de reserva paralelos, esperas fixas em todos os segmentos | Rejeitado para esta utilização |

C01/C02 são duas partes do mesmo lote, não duas causas independentes confirmadas no dispositivo.

## 3. C01/C02 — áudio: o aviso não pode ser transformado automaticamente em falha terminal

**Ficheiros a alterar numa futura implementação:**

- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/AudioOutputPolicy.kt`: `AudioWatchdog.onAudioSinkError` (397–417), `raise` (420–423), `poll` (430–441), eventos de avanço/criação de saída (304–330) e comentários de cobertura.
- `C:/Users/renat/Downloads/OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt`: consumo do watchdog (1034–1051), para validar a integração, sem remover a recuperação de erros reais.
- Estender os testes existentes `player-core/src/test/java/tv/own/owntv/player/AudioRecoveryTest.kt`; adicionar teste de integração dirigido se a simulação existente não cobrir o comportamento real.

**Evidência:** exceto descontinuidade de timestamps, o callback chama `raise`. Na ronda seguinte, o motor pode pausar tudo e publicar erro terminal se estiver em estéreo ou já tiver consumido o resgate; caso contrário, força estéreo e reconstrói o leitor.

Media3 1.11.1, `AnalyticsListener.java:1105–1121`, classifica esse callback como diagnóstico e permite recuperação interna. A aplicação não deve iniciar repetição ou erro visível com base apenas nele.

Além disso, `pendingReason` pode sobreviver a um novo AudioTrack ou avanço da saída. `poll` entrega essa razão antes de consultar `advancing`. Uma saída recuperada pode ser interrompida por uma decisão anterior.

**Alteração mínima recomendada:**

1. Manter o diagnóstico, retirando de `onAudioSinkError` a promoção direta para pausa terminal, estéreo global e rebuild.
2. Associar qualquer decisão de saúde restante à saída/sintonia válida e reavaliar se já houve recuperação. Não limpar todos os contadores a cada callback.
3. Preservar `onPlayerError`, ownership, pausa/suspensão, tratamento existente de PCM/underrun e descontinuidades.
4. Corrigir os comentários: o teste de seis segundos é sobretudo de **primeiro avanço da saída**, não um detetor contínuo de ausência de som.

**Testes obrigatórios:** aviso recuperável seguido de avanço; erro da saída antiga seguido de nova saída; erro fatal atual; callback antigo após zap; PCM com underrun; descontinuidade; resgate já consumido. Um erro atual não pode ser confundido com obsoleto e uma saída recuperada não pode provocar reabertura HTTP.

Não usar amplitude/silêncio como prova de falha: silêncio pode fazer parte do programa. Não prometer reset isolado de áudio: o resgate atual em `LivePreviewEngine.rebuildForSettingChange` liberta o player e reabre a transmissão. O áudio participa no relógio A/V.

## 4. C03 — reconexão rápida HLS demasiado abrangente

**Ficheiro principal:** `LivePreviewEngine.kt`, `onPlayerError` (1602–1620), tratamento de sessão (1650; 2440–2457), `hlsHttpReconnectDelayMs` (3803–3806) e limite de 1500 ms (3910).

**Evidência:** depois de o canal já ter reproduzido, `ERROR_CODE_IO_BAD_HTTP_STATUS` em HLS entra no percurso rápido, salvo 429 e esperas explícitas já intercetadas. O ramo inclui 401/403/404, 458 e 503 sem prazo aplicável. A rotina específica de sessão ocupada é chamada mais abaixo, num ramo que a recuperação runtime já ultrapassou.

**Risco:** reconexões rápidas e repetidas contra uma recusa de entrada/sessão podem ser inúteis e agravar a limitação do fornecedor. O código atual não implica pedidos paralelos nem prova bloqueio real; o problema é a amplitude da classificação.

**O que adicionar/alterar:** conservar evidência do papel do pedido que falhou, identidade e status; restringir a aceleração aos casos transitórios apropriados. Reutilizar `MediaLoadRole`, diagnósticos de fonte e controlador existentes, evitando outro motor de retries. Não inferir o papel apenas da extensão da URL. Se o erro fatal tiver perdido esse contexto, propagá-lo sem usar estado de uma fonte antiga.

**Preservar:** um 403 de segmento pode recuperar ao obter manifesto válido; não declarar todos os 403 terminais. 458 é convenção do fornecedor, não código HTTP universal. Respeitar 429/503 com espera e as opções HLS/motor.

**Precisão após consulta dos sources:** as oito reconexões não são oito pedidos HTTP. Antes de um erro fatal, Media3 pode repetir carregamentos e excluir variantes/localizações. `DEFAULT_MIN_LOADABLE_RETRY_COUNT=3` é um mínimo antes da propagação, não um teto universal de três pedidos. O OwnTV especializa certos segmentos 403/404/410 com uma repetição de 500 ms e mantém os fallbacks elegíveis. Medir a sequência agregada, sem multiplicar constantes como se fossem uma contagem fixa.

**Testes a estender:** `LiveReconnectLadderTest.kt`, `LiveTuneControllerTest.kt`, `HlsSegmentRefusalsTest.kt` e fixture local. Comparar 458 antes/depois de READY; 403 de entrada vs segmento isolado; 503 com/sem `Retry-After`; sucesso após um manifesto renovado; cancelamento e limites totais.

## 5. C04/C05 — temporizadores e progresso: coordenar antes de prolongar

**Ficheiros envolvidos:** `LivePreviewEngine.kt`, `LiveExoWatchdog.kt`, `LiveWaitBudget.kt`, `LiveTuneController.kt`, `LiveLadder.kt`, `ResponseDeadline.kt`.

### 5.1 Linha temporal real

- `ResponseDeadline` aplica 8 s à fase de resposta no interceptor de rede depois de o canal ter reproduzido. Não é teto total de DNS/TLS, de todos os redirects ou do corpo.
- `BUFFERING` contínuo arma o stall interno de 12 s.
- Reconexão normal começa com espera de 1,5 s, podendo depois aumentar.
- O watchdog externo tem base de handoff de 15,5 s, com concessões limitadas.
- O episódio de recuperação de 30 s do controlador começa em `advance`, não necessariamente no primeiro instante do corte.

Não somar estes números como se todas as fases fossem sempre sequenciais: há sobreposição, cancelamento e condições diferentes. Também não anunciar «recuperação máxima de 30 s desde o primeiro corte».

### 5.2 Decisão de prazo baseada no número

`LiveTuneController.usesStrictDeadline` (722) considera estritos os valores 9–30 s. Inclui assim os 30 s de recuperação, embora o comentário se refira ao arranque escolhido pelo utilizador. Em 735–736, ignora créditos de progresso/provedor que outra camada pode admitir.

O prazo de arranque estrito é intencional: `LiveTuneControllerTest` (612–621) já o exige. **Preservá-lo.** A futura alteração deve identificar a fase explicitamente e estabelecer quem decide a concessão runtime. Não prolongar qualquer tentativa silenciosamente só por deixar de a classificar como estrita.

Exemplo de teste: recuperação inicia; aos 15 s o servidor pede mais 20 s. O motor anuncia tentativa aos 35 s e o controlador pode abandonar aos 30 s. Isto é conflito de política/UX; não prova um pedido prematuro.

### 5.3 Primeiro stall não recebe a mesma tolerância

`LivePreviewEngine.startupProgress` (767–772) retorna null quando `sourceReady=true`. Ao entrar em BUFFERING após READY (1447–1458), essa flag não é limpa; o callback de 12 s (920) reconecta diretamente. A flag só é reiniciada em nova preparação (1204–1206).

**Candidato condicionado:** no vencimento, reavaliar a fonte atual e eventual progresso útil, com uma extensão única e finita por interrupção. Não criar timer independente. Continuar a terminar um stream sem progresso.

**Não usar qualquer byte como sinal suficiente:** os bytes atuais (3533–3538) podem vir de playlist, chave ou média. `receivingData` (298) e a concessão de 3 s externa não demonstram produção de conteúdo reproduzível.

**Ensaios antes de alterar valores:** segmento lento que só permite retoma aos 13–14 s; headers lentos com buffer ainda saudável; corpo preso; playlist atualiza mas não há segmentos válidos; recuperação exatamente no limite; `Retry-After` maior que orçamento; pausa/Back/zap durante cada fase. Estender `ResponseDeadlineTest`, `LiveWaitBudgetTest`, `LiveResumeWatchdogsTest` e `LiveTuneControllerTest`.

## 6. C06 — política de transporte herdada do catálogo: experimentar, não ativar já

`core/src/main/java/tv/own/owntv/core/di/DataModule.kt:48–66` configura timeouts e `retryOnConnectionFailure(false)` para o cliente base; `StreamingHttpClient.kt` herda a escolha. O cliente de reprodução já tem dispatcher e pool próprios.

Desativar a recuperação de conexão também afeta sockets reutilizados que deixaram de funcionar. Não corresponde apenas a evitar repetir respostas HTTP. Contudo, ligar a opção pode criar tentativas transparentes adicionais.

**Decisão:** protótipo limitado a `StreamingHttpClient`, comparando socket obsoleto, cancelamento, falha antes/depois de envio e múltiplos endereços. Preservar `ProviderRetryGuard` para recusas. Contar tentativas físicas e duração total; impedir pedidos de uma sintonia cancelada. Não alterar o cliente global, que também serve sincronização e catálogo.

`StreamingHttpClientTest` atualmente testa herança desta política; uma eventual mudança precisa de contrato deliberado, não de atualizar a asserção só para a suite passar.

## 7. C08/C09 — compatibilidade condicional de sessão e cabeçalhos

### 7.1 Cookies

O cliente não configura `CookieJar`; o default de OkHttp é `NO_COOKIES`. Cabeçalhos explícitos da lista continuam a ser transmitidos, mas um `Set-Cookie` recebido não passa automaticamente aos pedidos seguintes. Uma origem que dependa disso pode falhar; uma conta Xtream autenticada pelo URL pode nunca precisar desta função.

**Somente se confirmado:** armazenamento em memória, isolado por fonte/sessão/conta, respeitando domínio, caminho, validade e `Secure`. Não sobrescrever um cookie explícito nem criar um depósito global persistente. Ficheiros possíveis: `StreamingHttpClient.kt`, infraestrutura de escopo TLS/sessão e seus testes; confirmar o ponto de isolamento antes de escolher a classe. Testar manifesto que emite cookie, segmento que o exige, rotação e duas contas no mesmo host. Esta análise não autoriza adicionar já essa dependência funcional.

### 7.2 Redirect e URLs absolutas dentro do manifesto

`core/src/main/java/tv/own/owntv/core/network/PlaybackTlsInterceptor.kt:239–251` remove cabeçalhos fora da lista permitida ao redirecionar HTTPS para outra origem nos modos não estritos aplicáveis. Isso pode retirar `Referer`/`Origin` necessários, mesmo com certificados válidos. Entretanto, uma URL absoluta de segmento/chave é um novo pedido: `LivePreviewEngine.kt:3544` pode reaplicar os cabeçalhos explícitos, sem a mesma condição de redirect.

**Risco de incoerência:** perder cabeçalho necessário num percurso e enviar identidade sensível a outro host noutro. Não ficou demonstrado que um canal atual dependa desse caso. Não corrigir permitindo ou retirando todos os cabeçalhos globalmente.

**Próximo passo:** fixture origem A → CDN B com `Referer` obrigatório, seguida de manifesto com URLs absolutas B/C e cabeçalhos sensíveis. Definir destinos autorizados e distinguir identidade de requisitos de reprodução antes de alterar `redirectGuard` ou `StreamHeaders`. Não relaxar TLS nem criar uma recusa nova por host sem este ensaio. O resultado desejado é permitir a CDN legítima e impedir divulgação a destino arbitrário.

## 8. O que preservar e o que não acrescentar

- `SourceDrain` verifica imediatamente e espera apenas enquanto existem recursos locais pendentes; 300 ms não são atraso obrigatório em todos os canais. Teto atual 1,5 s.
- `PlaybackHttpCalls` separa confirmação de cancelamento, fecho de corpo e respostas tardias; isso não prova libertação da sessão remota pelo fornecedor. Não ignorar o gate nem aumentar delays fixos sem medir `cancelAckMissing`, `bodyCloseMissing` e `lateCloses`.
- `ProviderBackoffGuard` impede nova rede antes de expirar a espera. O estado é em memória; não prometer persistência após reinício do processo.
- Abertura e recuperação já estão separadas em `LiveLadder`/`LiveTuneController`. Não duplicar.
- Não criar ligações paralelas de reserva, sondagens periódicas ou reinícios preventivos de canais saudáveis.
- Não usar `AudioDelayClock` como reparação automática de cortes. Alterações de relógio podem segurar imagem ou saltar frames; não são tratamento de rede.
- Offload/tunneling já estão desativados nos defaults analisados. Não criar opções redundantes para os desligar.
- Flags de compatibilidade MPEG-TS/HLS devem continuar por canal. Deteção adicional de access units custa CPU; aceitar non-IDR pode introduzir artefactos. Não ativar globalmente.
- Cookies de sessão são compatibilidade condicional, não falta universal. Não ativar armazenamento global de cookies por suposição.
- Não retirar isolamento TLS/redirects nem aceitar certificados de qualquer CDN para «melhorar HLS». TLS não resolve 403 nem limitações de reabertura.
- Não alterar buffer, volume a 100%, descodificação, ordem de canais, EPG ou preferências existentes como efeito lateral destas correções.

## 9. Ordem futura aprovada pelo council

1. **Subfase A — C01/C02:** correção de intervenção indevida e estado pendente do áudio, comentários e testes dirigidos.
2. **Subfase B — C03:** fixtures de recusas, contexto e contagem agregada de pedidos. Corrigir a classificação apenas onde o ensaio demonstrar insistência redundante. Não endurecer todos os 403.
3. **Subfase C — C04:** contrato explícito de abertura/recuperação/esperas, testes de relógio e cancelamento; preservar inicialmente os valores e só depois alterar a política necessária.
4. **Subfase D — C05/C06:** experiências separadas, uma variável de cada vez. Integrar apenas o que reduzir intervenções ou falhas sem aumentar pedidos, latência ou consumo excessivo.
5. **C07:** opção futura independente. Não ativar por defeito nem contrariar a saída automática à lista sem escolha explícita.
6. **C08/C09:** testes de compatibilidade por origem e decisão específica; não incorporar como defaults globais no mesmo lote de correção de áudio.

Após qualquer implementação Core, atualizar e verificar o snapshot consumido pela App segundo o processo existente. Compilação, lint e testes dirigidos não substituem aceitação na box. Medir AAC/MP2, HDMI PCM/passthrough, zapping, Back/suspensão e uma sessão prolongada de 8–12 h. Comparar número de reaberturas, interrupções, sincronismo, memória e pedidos; não inventar percentagens.

## 10. Referências primárias

- [Media3 AnalyticsListener — contrato de onAudioSinkError](https://developer.android.com/reference/androidx/media3/exoplayer/analytics/AnalyticsListener): aviso não equivale a falha fatal.
- [Media3 — reprodução em direto](https://developer.android.com/media/media3/exoplayer/live-streaming): janela móvel e recuperação de posição indisponível.
- [Media3 — troubleshooting](https://developer.android.com/media/media3/exoplayer/troubleshooting): custos e limites das flags de extração.
- [RFC 9110 — Retry-After](https://www.rfc-editor.org/rfc/rfc9110.html#section-10.2.3): significado da espera do servidor.
- [OkHttp 5.5.0 — configuração](https://raw.githubusercontent.com/square/okhttp/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/OkHttpClient.kt) e [recuperação](https://raw.githubusercontent.com/square/okhttp/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/http/RetryAndFollowUpInterceptor.kt): distinguir transporte de respostas HTTP.
- [RFC 6265 — armazenamento de cookies](https://www.rfc-editor.org/rfc/rfc6265.html#section-5.3): referência para C08, não autorização para ativação global.
- Fontes locais efetivas: Media3 1.11.1 e OkHttp 5.5.0. Verificar a mesma versão antes de usar exemplos da documentação atual.

## 11. Reavaliação documental e relatos técnicos — decisão revista

Esta secção resulta de nova pesquisa em documentação Android TV, fontes das dependências e discussões dos mantenedores, com três revisões críticas independentes. Prevalece sobre qualquer leitura anterior que apresente C03/C04/C05 como alterações comportamentais já aprovadas. Nenhuma alteração funcional foi aplicada.

### 11.1 Critérios de aceitação de uma proposta

1. Um contrato explícito da API e um percurso incompatível no código atual sustentam correção dirigida.
2. Um relato só sustenta aplicar o respetivo patch se coincidirem versão, percurso, condições e causa reproduzida. Sintomas semelhantes não chegam.
3. Uma otimização deve reduzir interrupções/pedidos/trabalho no cenário alvo, sem piorar falha permanente, cancelamento, recursos ou canais saudáveis.
4. Uma ausência de mecanismo não é automaticamente bug: por exemplo, cookies não são necessários em todos os fornecedores.
5. Preservar Media3 como responsável pelos seus retries/fallbacks. Não adicionar controlo paralelo de playlists ou temporizadores por analogia com outra App.

### 11.2 O que os mantenedores e as fontes realmente suportam

| Fonte verificada | O que demonstra | Aplicação à OwnTV |
| --- | --- | --- |
| [Media3 — eventos do leitor](https://developer.android.com/media/media3/exoplayer/listening-to-player-events) e contrato `onAudioSinkError` | Distinguir notificação diagnóstica de falha efetiva e recuperação interna | Sustenta C01; preservar `onPlayerError` e testar saída recuperada |
| [Media3 #3210](https://github.com/androidx/media/issues/3210), reproduzido pelo mantenedor | AudioTrack pequeno e micro-underruns podiam induzir BUFFERING; houve correções upstream | A 1.11.1 usada já contém buffer PCM de cerca de 500 ms e margem de cerca de 100 ms nos renderers. Não reimplementar workaround antigo ou aumentar tudo para vários segundos |
| [Media3 #3311](https://github.com/androidx/media/issues/3311), reproduzido pelo mantenedor | Falha de playlist LL-HLS podia fazer o recarregamento regressar à posição antiga | A correção b842f19 já está no source 1.11.1 verificado. Usar o cenário para regressão, não portar o patch novamente |
| [Media3 #3311](https://github.com/androidx/media/issues/3311) e [correção b842f19](https://github.com/androidx/media/commit/b842f19baeaa2f6add0b4b559cacf9979209f776) | Falha LL-HLS com A/V separados podia recarregar conteúdo numa posição errada | O trecho corrigido já existe em `HlsSampleStreamWrapper` 1.11.1. Usar como regressão, não importar novamente |
| [Media3 #3362](https://github.com/androidx/media/issues/3362), comentário do mantenedor de 19/10/2026, e [correção c4b16f9](https://github.com/androidx/media/commit/c4b16f9) | Descarga parcial, exclusão de variante e junção/reset de posição exigiram outra correção | Candidato upstream novo: o mantenedor indicou primeira disponibilidade em 1.12.0-beta01; a fonte 1.11.1 ainda tem o trecho anterior. Exige reprodução antes de atualizar ou portar o conjunto relacionado |
| [Media3 — política de erros](https://developer.android.com/media/media3/exoplayer/customization#customizing-error-handling) e [fonte 1.11.1](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/upstream/DefaultLoadErrorHandlingPolicy.java) | A política pode considerar o erro e a carga; já existem retries e fallbacks | Sustenta testes C03, não uma segunda rotina de recuperação |
| [Apple — Low-Latency HLS](https://developer.apple.com/documentation/http-live-streaming/enabling-low-latency-http-live-streaming-hls) | Certas playlists e preload hints podem ser pedidos deliberadamente bloqueantes | Se a lista usar LL-HLS, testar o limite de headers por papel do pedido. Não confundir ausência imediata de resposta com ligação morta; não remover o limite globalmente |
| [Android TV — capacidades de áudio](https://developer.android.com/training/tv/playback/audio-capabilities) | A saída ativa e os formatos suportados podem mudar; saídas não têm capacidades iguais | Preservar negociação de capacidades Media3 e testes de HDMI/Bluetooth. Não forçar passthrough, PCM ou software globalmente com base num relato |
| [Android TV — memória](https://developer.android.com/training/tv/playback/memory) | Pressão de memória pode bloquear threads do codec e causar falhas de A/V | Medir Java/native/graphics e não só segundos de buffer. Não prova fuga nesta App; não aumentar buffers como solução universal |

O issue #3362 merece investigação de dependência, não declaração de que explica os cortes periódicos de áudio AAC/MP2. O relato envolve condições específicas de erro e variante. Não atualizar automaticamente para beta nem copiar só a linha de reset: a correção upstream tem partes relacionadas.

### 11.3 Fóruns: utilidade e limites

Foram encontrados relatos de utilizadores com observações de primeira mão, incluindo [cortes a cada quatro minutos no CoreELEC](https://discourse.coreelec.org/t/audio-dropouts-every-4-minutes-in-mp4-playback/9787) e [cortes periódicos a 44,1 kHz em Ugoos](https://discourse.coreelec.org/t/periodical-audio-dropouts-when-outputting-44-1-khz-via-hdmi/54477). Sugerem que a saída HDMI/PCM também pode produzir periodicidade, mas usam CoreELEC/ALSA, hardware e condições diferentes. Não autorizam portar uma configuração de driver para AudioTrack/Media3 no Android.

O [issue #3522](https://github.com/androidx/media/issues/3522) relata intervalos audíveis depois de seek em passthrough, mas não reproduz o caso Xiaomi/AAC/MP2 em reprodução contínua com a nossa versão. O mantenedor pediu reprodução com Media3 recente. Não sustenta impedir a libertação de AudioTrack nem escolher passthrough como default universal.

Não foi encontrado um relato primário suficientemente equivalente que confirme a causa do corte Xiaomi descrito. Os fóruns ajudam a definir ensaios; não substituem a captura do evento na box.

### 11.4 O que implementar, melhorar, remover ou manter pendente

**Implementação diretamente fundamentada: C01/C02.** Retirar a promoção destrutiva do callback de diagnóstico; impedir decisões obsoletas da saída; corrigir os comentários e testar falha fatal real. O teste existente `AudioRecoveryTest.actualSinkFailureStillTriggersRecoveryWithoutPassthrough` trata uma exceção no callback como prova de falha: precisa de ser reespecificado pelo contrato da API, não apenas mantido por estar verde.

**Melhoria de validação necessária: C03/C04.** Construir ensaios dirigidos antes de mudar limites ou ritmo. O ficheiro existente `OwnTV-main/tools/performance/local_iptv_fixture.py` gera fixtures e um servidor estático; não é já um laboratório completo de falhas HLS. Estendê-lo, ou criar suporte de teste complementar mínimo, para injetar falha parcial, recusa por papel do pedido e esperas. Não afirmar que os cenários já foram executados.

**Diagnóstico adicional apenas se faltar evidência.** Já existem `load_result`, `http_load_completed`, `http_response`, buffer, origem, duração e dados de saúde. Reutilizar esses eventos; acrescentar somente a correlação que faltar entre erro de load, fallback/retry interno, erro fatal e decisão de reprepare. Não duplicar logs nem criar consulta frequente/reflection sobre AudioTrack privado. [A API AudioTrack](https://developer.android.com/reference/android/media/AudioTrack) não torna o contador de underruns um diagnóstico causal de HLS.

**Manter condicionado: C05/C06/C08/C09 e candidato #3362.** Cada um exige prova comparativa e deve ser avaliado separadamente. Para #3362, comparar com uma versão estável que contenha a correção quando for integrar; não transformar a revisão numa atualização geral de bibliotecas.

**Retirar do plano imediato:** novos delays fixos, tentativas permanentes por defeito, reinícios periódicos, aumento generalizado do buffer de áudio/vídeo, correção automática de relógio e workarounds upstream já presentes. Preservar os timers atuais até o ensaio justificar a alteração.

### 11.5 Ensaios que decidem a implementação

| Ensaio | Critério para aprovar uma mudança |
| --- | --- |
| Aviso de áudio seguido de recuperação interna e novo AudioTrack | Nenhuma pausa/reabertura causada por aviso obsoleto; erro fatal real continua recuperável |
| 403 de segmento recuperável vs 401/403 persistente de entrada | Recupera o primeiro sem prolongar pedidos redundantes nos restantes |
| 458 antes/depois de READY e 429/503 com espera | Política coerente de sessão, sem pedidos antes do prazo nem tentativas simultâneas |
| Segmento com headers rápidos e corpo lento, recuperável aos 13–14 s | Eventual extensão reduz reaberturas; caso sem progresso não fica preso mais tempo sem benefício |
| Falha parcial seguida de fallback de variante | Nenhum regresso incorreto de PTS/posição; decide se #3362 se aplica |
| Socket reutilizado fechado antes/depois de o servidor processar o pedido | Transporte recupera apenas os casos seguros, sem duplicar abertura lógica ou contornar cancelamento |
| Reprodução prolongada, áudio AAC/MP2, saída HDMI e mudanças de saída | Sem aumento de cortes, dessincronia, memória, número de fontes/reaberturas ou sessões ativas |

Esta revisão altera a prioridade e o grau de certeza do plano, não o comportamento da App. Nenhum teste de box ou novo teste de streaming foi executado nesta análise.
