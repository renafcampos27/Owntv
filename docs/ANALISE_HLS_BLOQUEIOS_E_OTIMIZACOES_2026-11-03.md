# OwnTV: análise aprofundada de bloqueios e otimizações HLS

Data: 3 de novembro de 2026. Estado: análise concluída; propostas por implementar.

## Decisão

Recomendo remover esperas introduzidas pela própria instrumentação, dar à reprodução uma fila de pedidos independente das imagens e corrigir o aproveitamento das respostas DNS tardias. Depois, rever recuperações de transporte e de segmentos com testes específicos. Não recomendo substituir o motor HLS, aumentar indiscriminadamente a concorrência ou introduzir mais atrasos fixos.

Os mecanismos abaixo foram identificados no código atual. Parte foi reproduzida em servidor local controlado. Não está demonstrado que expliquem os cortes reais na Xiaomi, em particular os cortes apenas de áudio com imagem fluida.

## Base verificada e alcance

- App: `C:/Users/renat/Downloads/OwnTV-main`.
- Core: `C:/Users/renat/Downloads/OwnTV_Core`.
- Core utilizado pela App: arquivo indicado em `core-source-lock.json`, SHA-256 `70aab7a373d8d6dc8449eda6a3c3e720118387e196a81fa0222f15710456ceb9`.
- Doze ficheiros relevantes do Core foram comparados byte a byte com esse arquivo; coincidem.
- Dependências analisadas: Media3 1.11.1, OkHttp Android 5.5.0 e Okio 3.18.1.
- Foram compilados diretamente os ficheiros atuais `ResponseDeadline.kt`, `ProviderRetryGuard.kt`, `LocalPlaylistReadiness.kt` e `BoundedDns.kt`, juntamente com um pequeno programa de ensaio externo ao projeto.
- O ensaio usa apenas HTTP em loopback, DNS sintético e um substituto mínimo de `android.util.Log` para execução na JVM. Não simula o AudioTrack, o descodificador nem o sistema Android da box.
- Não foram alterados código funcional, dependências, configurações persistentes ou snapshot do Core. Não foi gerado APK nem feita validação física.

## O percurso já é assíncrono onde interessa

O ExoPlayer prepara o HLS e carrega playlists/segmentos pelos seus loaders. O `OkHttpDataSource` desta versão usa `Call.enqueue()` e espera o resultado na thread de carregamento. A espera não é uma prova de bloqueio da interface. A sintonização também aguarda o fecho local anterior por uma corrotina suspensa.

Portanto, adicionar `Dispatchers.IO` ou criar mais threads em todos os pontos não é uma solução geral. Os alvos úteis são esperas evitáveis antes da entrega da resposta, filas partilhadas, trabalho repetido e decisões de recuperação demasiado abrangentes. Operações do Player devem continuar na thread exigida pelo Media3.

## Resultados locais reproduzidos

O servidor enviou cabeçalhos imediatamente e atrasou o corpo por 650 ms. Os tempos de entrega foram medidos antes do fecho da resposta. Um ensaio anterior incluía esse fecho e produzia cerca de 103 ms no caso base; os números abaixo substituem essa medição.

| Ensaio | Resultado observado | Interpretação limitada ao cenário |
| --- | --- | --- |
| Resposta 200, sem leitura antecipada | 2 ms até entregar a resposta | Os cabeçalhos podem chegar ao consumidor antes do corpo. |
| Resposta 200, com `peekBody(564)` como no diagnóstico | 653 ms | O diagnóstico pode esperar por dados futuros da rede. |
| Resposta 429 com `ProviderRetryGuard` atual | callback aos 654 ms; conclusão aos 655 ms | A recusa conhecida nos cabeçalhos fica à espera do corpo. |
| Mesmo 429 com prazo de cabeçalhos de 200 ms | cerca de 206 ms; `SocketTimeoutException` com `ProviderHttpRefusal` na causa | O prazo engloba trabalho do interceptor interior, incluindo leitura do corpo. O código HTTP continua recuperável pela causa. |
| DNS demora 120 ms; espera limitada a 25 ms; segunda consulta após conclusão da primeira | duas consultas reais ao delegado | O sucesso que chega depois do prazo não é guardado pelo caminho atual. |
| Cinco pedidos lentos do mesmo host; pedido HLS em fila partilhada/própria | 511 ms / 2 ms | A fila comum pode atrasar o início do HLS quando estiver ocupada. |

As seis verificações passaram. Os tempos são de um ensaio sintético nesta máquina; não são uma estimativa percentual do ganho na app nem garantem reprodução sem cortes.

## HLS-01 — Leituras de diagnóstico no caminho crítico

**Estado: confirmado por código e ensaio. Prioridade alta.**

Em `LivePreviewEngine.kt:3339`, respostas falhadas ou diagnóstico detalhado ativo levam a `peekBody(564)`. Existe ainda uma leitura condicionada à confirmação de uma resposta TS. `peekBody` chama `request(byteCount)` no source do OkHttp: pode aguardar a rede, não se limita a consultar bytes já recebidos.

No caso de respostas saudáveis, isto não acrescenta um novo pedido, mas pode atrasar a entrega ao Media3 e ocupar um lugar na fila assíncrona enquanto espera. Com diagnóstico desligado, o percurso geral das respostas 200 não faz esse peek; não atribuir todos os cortes a este ponto.

**Proposta:** registar cabeçalhos, códigos, tempos e tamanhos sem antecipar leituras. Quando for indispensável obter texto de erro, consultar apenas bytes já disponíveis em memória, com limite e redação de credenciais. Uma observação adicional do corpo pode acompanhar a leitura normal pelo consumidor; não deve criar uma segunda leitura nem reter buffers grandes. Rever os comentários que afirmam que corpos saudáveis nunca são lidos.

**Aceitação:** um corpo atrasado não atrasa a entrega dos cabeçalhos por causa do diagnóstico; nenhuma duplicação de pedidos; cancelamento e redação preservados.

## HLS-02 — O tratamento da recusa interfere com o prazo dos cabeçalhos

**Estado: confirmado por código e ensaio. Prioridade alta. Está relacionado com HLS-01, não contar como causa independente comprovada na box.**

`ProviderRetryGuard.kt:24` lê até 564 bytes de respostas 408/429/503 antes de fechar e notificar a recusa. `ResponseDeadline` está colocado antes desse interceptor na cadeia de rede. A sua chamada a `chain.proceed` só termina quando o interceptor interior acabar, podendo abranger a leitura do corpo que aquele executa.

Assim, o prazo descrito como apenas de cabeçalhos também pode disparar por um corpo de erro lento. O estado HTTP é mantido numa causa e `PlayerErrors.httpStatusOf` consegue procurar essa causa; não é correto afirmar que o código é necessariamente perdido.

**Proposta:** construir e publicar a recusa a partir do status e de `Retry-After`, sem esperar por texto remoto. Tratar o texto como informação opcional. Tornar claro e testado que o prazo mede a fase desejada. Manter a exceção privada do timeshift local e as regras de espera do fornecedor.

**Aceitação:** 429 com cabeçalhos válidos e corpo silencioso não atrasa a aplicação do prazo do fornecedor. 408/503 não originam tentativas ocultas. Cancelamento continua a fechar a resposta.

## HLS-03 — Pool de ligações separado, mas fila assíncrona partilhada

**Estado: arquitetura e mecanismo confirmados. A frequência real de saturação ainda não foi medida. Prioridade alta.**

`StreamingHttpClient.kt:49` deriva o cliente com `newBuilder()` e substitui o pool de ligações, mas herda o `Dispatcher`. O cliente de imagens em `OwnTVApp.kt:194` também deriva do cliente global e mantém esse dispatcher. O HLS usa `enqueue()`, pelo que os limites da fila assíncrona são relevantes.

O ensaio reproduziu a espera quando cinco pedidos lentos ocupavam o limite do mesmo host. Este problema exige concorrência suficiente e coincidência dos limites; não significa que cada logótipo bloqueia a reprodução. As consultas normais de catálogo/EPG usam `execute()` em IO e não ficam sujeitas da mesma forma ao limite da fila assíncrona; podem disputar rede/CPU, mas é outro mecanismo.

**Proposta:** um `Dispatcher` duradouro e exclusivo no cliente de reprodução, herdado pelos seus clientes derivados. Manter limites moderados e medir espera de fila; não criar dispatcher por canal e não aumentar globalmente o número de pedidos.

**Aceitação:** pedidos de imagens lentos não ocupam os lugares de execução do HLS. Cancelar uma sintonia não cancela as imagens nem outra reprodução legítima. Respeitar o limite de sessões contratado continua obrigatório.

## HLS-04 — Sucesso DNS tardio é desperdiçado

**Estado: confirmado por código e ensaio. Prioridade média.**

`BoundedDns` guarda a resposta em `answers` apenas depois de `pending.get()` ter sucesso. Se o consumidor expirar e a consulta terminar mais tarde, `done()` remove a operação em curso sem guardar o resultado. A próxima tentativa pode repetir a mesma resolução e voltar a expirar.

**Proposta:** guardar sucessos válidos no momento da conclusão do trabalho DNS, com validade limitada e sincronização correta. Não guardar erros ou listas vazias. Não permitir que uma resposta iniciada sob configuração DNS antiga substitua a configuração atual. Preservar a partilha de consultas em curso e o limite de workers.

**Limite:** isto reduz trabalho repetido; não torna cancelável o resolver nativo nem resolve um DNS indisponível. O executor global de quatro workers pode ficar ocupado por consultas nativas lentas; aumentar threads indefinidamente não é uma correção aceitável.

## HLS-05 — Política de recuperação de transporte herdada do catálogo

**Estado: configuração confirmada; benefício no serviço real depende do tipo de falha. Prioridade média, depois das remoções de bloqueios.**

O cliente global define `retryOnConnectionFailure(false)` e o cliente HLS herda-o. Media3 já faz recuperação de carregamentos. Não se deve acrescentar outra escada de tentativas sem mapear as duas.

**Proposta:** permitir recuperação apropriada de conectividade no cliente de reprodução, com ensaios de ligação reutilizada fechada, falha de rota, cancelamento e orçamento total. Os guards atuais para 408/429/503 devem continuar a impedir follow-ups HTTP fora do orçamento. Não repetir recusas de autorização como se fossem falhas de transporte.

**Aceitação:** recuperar uma falha de ligação elegível sem reabrir o canal completo, quando possível; ausência de pedidos tardios da sintonia anterior; contagem de tentativas observável e finita.

## HLS-06 — Generalização excessiva de recusas de segmentos

**Estado: regra confirmada por leitura; não foi reproduzida uma troca incorreta na box. Prioridade alta na fase de política.**

O código classifica 403/404/410 em conjunto e dois segmentos distintos recusados podem marcar o host como problemático. A seleção futura consulta essa aprendizagem em percursos aplicáveis; `hlsOnly` já impede essa consulta na decisão inicial. Existe também um observador de `segmentsRefused` que encaminha para a escada de recuperação. Não concluir que a app ignora sempre a opção ExoPlayer/apenas HLS: é necessário manter os gates do controlador.

Dois erros distintos não provam uma assinatura de tokens incompatível com Media3. Um 404 temporário, um segmento removido e uma recusa 403 podem ter tratamentos diferentes. O Media3 já possui mecanismos de atualização e exclusão de variantes; não substituí-los por uma política genérica paralela.

**Proposta:** separar playlist/segmento/chave e status; reduzir conclusões automáticas ao canal/sessão; exigir evidência antes de aprender uma regra do host. Dar oportunidade a uma recuperação apropriada da playlist antes de uma reabertura completa. Uma chave recusada não deve ser tratada como segmento expirado. Respeitar a escolha de motor/formato e `Retry-After`.

**Limite:** avançar para uma posição válida pode implicar um salto de conteúdo. Não prometer conservar conteúdo que o fornecedor já retirou.

## HLS-07 — Instrumentação para decisões proporcionais ao buffer

**Estado: lacunas de observabilidade confirmadas. Prioridade alta, com implementação leve.**

Os registos atuais têm estados, códigos HTTP e eventos de transporte, mas `onLoadCompleted` não regista os tempos e tamanhos de cada carregamento. O tempo do interceptor HTTP não é o descarregamento completo do segmento. A playlist é resumida em alguns acontecimentos, não como uma sequência contínua de atualizações.

**Proposta:** identificar pedidos por sessão e carregamento; medir fila, DNS, ligação/TLS, cabeçalhos e carregamento concluído; registar tipo, faixa, bytes e duração de conteúdo. Acrescentar evolução da sequência da playlist e tempo desde a última publicação nova, com registos limitados. Usar os eventos existentes, sem sondagens adicionais. Nunca consultar métodos do Player a partir de uma thread de rede: publicar snapshots pequenos pela thread correta.

Só depois avaliar prazos de recuperação conforme dados efetivamente disponíveis e a fase do pedido. Uma transferência lenta com progresso não deve ser confundida com servidor silencioso; bytes de uma playlist não provam que existe áudio/vídeo reproduzível.

## O que manter e o que fica condicionado

- Manter a identidade por sintonia e fonte, o cancelamento efetivo e o fecho dos corpos. A espera em `SourceDrain` já suspende a corrotina e já pode acordar por evento. Removê-la para parecer mais rápido pode voltar a sobrepor sessões.
- Fecho local confirmado não demonstra que o fornecedor libertou a sessão remota. O intervalo configurado para reabrir um canal continua a ser uma política independente. Não acrescentar mais 300 ms ou segundos fixos a todos os canais.
- A retoma imediata após interrupção é uma escolha expressa do utilizador. Manter; não introduzir novo pré-buffer de retoma como alteração escondida.
- Não prometer um teto rígido de segundos de buffer: segmentos completos e o controlo de carga do Media3 podem ultrapassar o limiar. Preservar o limite de memória e explicar o valor efetivo.
- HTTP/2/HTTP/3 ou Cronet: apenas ensaio separado se houver necessidade. A app tem compatibilidade de fornecedores, TLS, cancelamento e diagnósticos próprios; mudar de stack pode perder esses contratos. Não propor como primeiro lote.
- Não paralelizar arbitrariamente todos os segmentos nem carregar canais vizinhos: aumenta pedidos, memória e risco de exceder sessões. Usar o paralelismo de faixas que o Media3 já gere.
- Prazos LL-HLS: um pedido de atualização bloqueante pode legitimamente demorar. Rever o prazo único de 8 s só se a playlist e os registos demonstrarem esse modo; não aumentar ou reduzir globalmente sem teste.
- Timestamps/PDT: existe relato upstream de posições negativas com mudanças inconsistentes de `PROGRAM-DATE-TIME`. O código 1.11.1 ainda tem o ramo relevante do tracker, mas isto não reproduz nem confirma o bug na OwnTV. Diagnosticar primeiro; não remover PDT de todas as playlists.
- Evitar reaplicar correções já presentes: o tracker 1.11.1 já verifica se uma playlist agendada continua primária/ativa antes de atualizar. Não importar novamente a correção de #3254.
- Erros apenas de áudio com imagem contínua requerem diagnóstico próprio de AudioTrack/codec/timestamps. As melhorias de rede não garantem resolver esse sintoma.

## Plano de implementação recomendado

### Fase A — Tirar esperas evitáveis

HLS-01 + HLS-02 + HLS-03 + métricas mínimas de HLS-07. Remover leitura antecipada, tratar recusas pelos cabeçalhos e isolar a fila de reprodução. Sem novos controlos de buffer nem alterações ao intervalo de reabertura.

Ensaios: corpo lento/silencioso em 200 e erro; filas de imagens saturadas; 429 e 503 com Retry-After; resposta tardia e cancelamento; HLS de áudio/vídeo separados e timeshift local.

### Fase B — DNS e recuperação de transporte

HLS-04 + HLS-05. Aproveitar sucessos DNS tardios sem reutilizar respostas de configuração antiga; permitir recuperação de conectividade adequada mantendo uma contagem finita de tentativas e os guards do fornecedor.

Ensaios: DNS lento e alterado, IPv4/IPv6, ligação reutilizada fechada, cancelamento durante ligação e transição A-B-C-A sem pedidos tardios.

### Fase C — Política HLS específica

HLS-06 + completar HLS-07. Distinguir estágios e erros; rever aprendizagem por host; testar recuperação de playlist e evitar reaberturas completas desnecessárias. Utilizar o comportamento do Media3 sempre que já cubra a situação.

Ensaios: 404 temporário, segmento removido, 403 persistente, chave recusada, playlist parada/repentinamente reiniciada, tokens renovados, limites de sessão e escolha ExoPlayer/apenas HLS.

### Fase D — Só mediante evidência real

Avaliar adaptações de prazos LL-HLS, problemas de PDT e perfil de saída PCM. Não implementar automaticamente as três hipóteses. Se não houver evidência, encerrar a otimização nas fases A-C.

## Critério de validação

Os testes de transporte verificam mecanismo e regressões. Depois da implementação, compilar e executar os testes dirigidos do Core e da App, renovar e comparar o snapshot e validar o percurso release autorizado. O utilizador gera o APK.

Na Xiaomi: comparar o mesmo canal/variante e definições, por pelo menos 30 minutos antes/depois; repetir com navegação de logótipos e mudanças de canal. Medir tempo até primeiro fotograma, pausas de rede, cortes apenas de áudio, tentativas e espera de fila. Não declarar ganhos percentuais globais sem medições equivalentes no dispositivo.

## Fontes primárias consultadas

- [Arquitetura e personalização Media3](https://developer.android.com/media/media3/exoplayer/customization).
- [Stacks de rede Media3](https://developer.android.com/media/media3/exoplayer/network-stacks).
- [Suporte e funcionamento HLS](https://developer.android.com/media/media3/exoplayer/hls).
- [Fonte OkHttp 5.5.0: Dispatcher](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Dispatcher.kt).
- [Fonte OkHttp 5.5.0: Response.peekBody](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Response.kt).
- [Correção upstream já incluída: playlists inativas, #3254](https://github.com/androidx/media/issues/3254).
- [Relato upstream condicionado: saltos de PDT, #3337](https://github.com/androidx/media/issues/3337). Trata-se de um relato para outra versão; não usar como prova de ocorrência na OwnTV.

As fontes publicadas foram cruzadas com os sources JAR das versões efetivamente usadas. Relatos upstream foram tratados como pistas e verificados contra o código; não foram assumidos como diagnóstico da box.

## Evidência local

C:\Users\renat\Downloads\OwnTV-HLS-auditoria-2026-11-03-0snxl8pi

Contém o programa de ensaio, resultados, registo de compilação e manifesto de hashes dos ficheiros analisados. Os JAR de dependências não foram copiados; o ensaio utilizou os artefactos já existentes na cache local. O ficheiro de estado contém os caminhos usados na máquina.
