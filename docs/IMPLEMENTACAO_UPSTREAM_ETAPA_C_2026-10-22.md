# OwnTV — etapa C: eficiência do diagnóstico e retoma (22/10/2026)

Estado: implementada e validada automaticamente; aceitação física na box pendente. Âmbito: R09/R10 da análise OwnTV 5.2.1/Core 1.0.69/libmpv 2026.10.4. Não corresponde nem renumera a fase 3 do plano principal.

## R09 — Diagnóstico com menos trabalho repetido

- As três expressões regulares de ocultação de credenciais em `HttpClient.redactUrl` passam a ser compiladas uma vez. Mantêm os mesmos padrões, substituições e resultados; a comparação entre versões está coberta por teste. Os pedidos de rede e as suas credenciais não são alterados.
- Avisos/erros nativos repetidos são deduplicados depois da ocultação dos dados. Retêm-se no máximo quatro mensagens de 2 048 caracteres por janela de 10 segundos. Cada mensagem inclui severidade e a geração da sintonia; uma falha fatal ou o primeiro erro de outra sintonia não se confundem com o aviso anterior.
- São emitidos resumos de repetições antes de um evento novo, no limite temporal e na saída/troca de sintonia. A causa específica do erro continua a ser atualizada; mensagens genéricas não substituem uma causa mais precisa.
- Avisos por frame de timestamps inválidos continuam a alimentar a proteção existente, mas não passam a ser classificados como causa terminal apenas por conterem a palavra `invalid`. Erros/fatais mantêm o tratamento correspondente.
- Com **Registo detalhado de reprodução** ativo, os contadores mpv de frames, falhas/atrasos de apresentação, cadência estimada e buffer são registados a cada 10 segundos na fila existente. Ativar/desativar durante a reprodução inicia/cancela a recolha, sem depender de mudar de canal.
- A leitura dos contadores usa o executor mpv existente e aceita **no máximo uma leitura pendente**. Um timeout ou uma mudança de canal não acumula novas leituras atrás de uma chamada nativa bloqueada. A geração/instância/configuração são verificadas antes de ler e antes de publicar.
- `DiagnosticWriter`, a sua fila de 512 entradas, escrita em lotes, rotação de ficheiros e os limites de eventos/mensagem existentes foram preservados. Não foi importado um executor sem limite. Não há novo interruptor nem novo valor predefinido de diagnóstico.

## R10 — Retoma antes da abertura, com alternativa

- Em ficheiros/gravações e arquivos indexados identificados como HLS/DASH, a posição relativa ao conteúdo é passada ao mpv através de `start` antes de `loadfile`.
- Escreve-se `start=none` em carregamentos sem posição e em direto, para que um canal não herde a posição de uma gravação anterior.
- A posição mantém os milissegundos, usando aritmética inteira e ponto decimal independente do idioma. A alternativa `seek` também conserva os milissegundos, em vez de truncar para segundos inteiros.
- A leitura de confirmação da opção tem de corresponder à posição pedida; uma opção positiva antiga não é aceite como prova. Se não corresponder, conserva-se a procura após `FILE_LOADED`, limitada à geração e ao estado do carregamento atual.
- Arquivos TS sem índice conhecido conservam o percurso alternativo existente; não se força uma procura inicial num serviço sem suporte conhecido. Esta alteração não cria suporte de Range ou arquivo no fornecedor.
- A posição, natureza do conteúdo e estado de retoma são capturados por carregamento. Antes de abrir o conteúdo, volta-se a confirmar geração, estado e instância. Um `FILE_LOADED` anterior à configuração desta retoma não consome a posição pendente da nova.
- A intenção de pausa é aplicada antes e depois da abertura. A confirmação tardia usa a intenção atual, não um `pause=true` antigo que pudesse contrariar Play ou afetar o canal seguinte.
- Horário do EPG, correção de diferidos +1 h, posição no programa e posição relativa no ficheiro continuam separados. As URLs dos arquivos não são recalculadas por esta alteração.
- ExoPlayer já abria com posição inicial; esse percurso, a escolha do motor e as alternativas HLS/formato foram preservados.

A opção aceite pelo motor não é prova de que todos os fornecedores suportam a procura com precisão. O comportamento real depende do índice, keyframes, manifesto, janela disponível e servidor. Não se garante uma única ligação HTTP nem uma melhoria percentual de rapidez.

## Proteção e validação

Backup verificado antes das alterações: `C:/Users/renat/Downloads/OwnTV-upstream-C-backup-20261022/`, com App/Core, snapshot anterior, lock e manifesto SHA-256. A comparação inicial com o snapshot da etapa B teve zero diferenças e zero inputs novos.

Mantidos: Core personalizado TV, modo simples, volume interno 100%, HLS exclusivo/exceções, preferências por canal, buffers, intervalo de reabertura, base de dados, formato de backup e correção +1 h dos diferidos. O projeto Mobile não foi editado.

Testes adicionados: precisão temporal/idioma, limpeza da posição entre conteúdos, elegibilidade de arquivo indexado, confirmação da opção, deduplicação/contagem/severidade/geração, limites temporais/memória e leitura nativa única durante timeout/cancelamento/erro. Acrescentado teste da equivalência da ocultação de credenciais.

Core: **1 330 testes JVM Core + 588 testes dos motores**, zero falhas/erros/ignorados; testes Android do Core compilados, sem execução num dispositivo. A etapa acrescenta **16 testes** (um Core e 15 dos motores).

Snapshot: **1 039 inputs**, SHA-256 `6eabcd44c622925b2f505a10559a4320606e08113ac57728d941f7feeaa3c1f4`, comparado byte a byte com os fontes e com conjunto de nomes idêntico. Seis testes da ferramenta de snapshot aprovados. Inventários i18n: Core 263 baseline/5 761 safe; App 51 baseline/1 084 safe. A dívida de tradução não aumentou; verificação de espaços nas linhas alteradas aprovada.

Validação final contra o snapshot definitivo: compilação standard debug/release da App, **429 testes JVM App**, lint vital de release e preparação de bibliotecas nativas — `BUILD SUCCESSFUL`. Configuração normal de build confirmada por `gradlew help`, sem forçar o snapshot nem desligar as verificações de integridade.

Total final sem contar repetições: **2 347 testes JVM + 6 testes da ferramenta de snapshot aprovados**, zero falhas/erros/ignorados. A etapa acrescentou **16 testes**. Os testes Android foram apenas compilados; não foram executados numa box.

Logs finais em `%TEMP%`: `owntv-upstream-c-core.log`, `owntv-upstream-c-app.log` e `owntv-upstream-c-normal-config.log`. O snapshot foi novamente comparado com os fontes no fecho, sem divergências. Não foi gerado APK, alterada assinatura, feito commit/push ou publicada versão.

## Aceitação física pendente

1. Em mpv, retomar gravações locais e SAF e arquivos HLS a meio do conteúdo. Comparar a posição pedida e a primeira posição efetiva; confirmar pausa inicial e Play/Stop durante a abertura.
2. Abrir gravação/arquivo retomado, sair e abrir direto ou outro conteúdo desde o início. Não deve herdar o ponto anterior. Repetir em sequência rápida, incluindo A→B→A.
3. Testar um arquivo TS não indexado e um serviço que recuse procura. Conferir o erro/alternativa sem forçar formatos ou alterar URLs de canais saudáveis.
4. Ligar/desligar o diagnóstico durante a reprodução mpv. Confirmar contadores a cada 10 s apenas quando ativo e ausência de registos tardios de outro canal.
5. Exportar um diagnóstico durante avisos repetidos. Confirmar presença da primeira falha, resumos e causa específica, com credenciais ocultas; os limites de memória/disco mantêm-se.
6. Verificar na box antiga e recente, com os mesmos motores, HLS, preferências por canal e volume do sistema. Testes JVM e compilação não substituem estes ensaios.

## Fontes e ponto de continuação

- [Manual mpv — start](https://mpv.io/manual/stable/#options-start): tempo relativo, precisão decimal e reposição através de `none`.
- [Core 1.0.69 — implementação upstream](https://github.com/ahXN00/OwnTV_Core/tree/core-1.0.69/player-core/src/main/java/tv/own/owntv/player): adaptada seletivamente, sem substituir os ficheiros personalizados nem a fila limitada de diagnóstico.

Com esta etapa implementada e validada automaticamente, fica fechada a sequência recomendada **A–C** destas publicações. **C01/C02/C03 (etapa D)** continuam opcionais/condicionais e dependem de decisão separada. As fases 3–6 do plano principal mantêm o âmbito anterior; não foram executadas automaticamente por esta integração.


Atualização posterior: C01/C02/C03 foram expressamente autorizadas e executadas na [etapa D](IMPLEMENTACAO_UPSTREAM_ETAPA_D_2026-10-22.md). O estado anterior de opcionais pendentes acima fica contextualizado por essa entrega, sem alterar o âmbito ou os resultados da etapa C.
