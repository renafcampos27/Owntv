# Council — bloqueio de ligação e confirmação do cancelamento

Data: 2026-10-03. Pedido: avaliar a correção já aplicada com agentes especializados e documentação primária; avançar se tecnicamente recomendada.

## Decisão final

**Manter o princípio da correção e aplicar o reforço de confirmação de cancelamento. Implementado e validado por compilação e testes.** Não regressar à barreira baseada no contador de callEnd/callFailed.

| Revisão independente | Resultado |
|---|---|
| HTTP / OkHttp | Mantém separação entre fecho local e notificações terminais. Ressalva que não há confirmação de encerramento da sessão no fornecedor. |
| Media3 / Android / HLS | Mantém cancelamento, proteção por geração e rejeição de respostas antigas. stop/clear são comandos do leitor, não confirmação remota. |
| Concorrência | Identificou duas condições de corrida que exigiam ajuste; aprovou a versão reforçada após nova leitura. |
| Council independente | Aprovou manter com ajuste de ACK e testes determinísticos; rejeitou esperas fixas e cancelamento global. |

Estes pareceres são revisões de agentes sobre código e fontes públicas, não avaliações de pessoas externas nem certificação Android.

## Fundamentação oficial

O projeto usa Media3 1.11.1 e OkHttp 5.5.0, verificados no catálogo de dependências.

- [RealCall, OkHttp 5.5.0](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RealCall.kt): a flag canceled é marcada antes da execução das operações de cancelamento do transporte. O evento canceled é emitido depois dessas operações. isCanceled sozinho não é confirmação suficiente perante cancelamento concorrente.
- [Call.addEventListener, OkHttp 5.5.0](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Call.kt): API pública para acrescentar um observador por chamada sem substituir os existentes.
- [EventListener, OkHttp 5.5.0](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/EventListener.kt): callbacks devem ser rápidos, sem I/O ou chamadas de volta ao cliente. O novo callback escreve apenas num AtomicBoolean.
- [OkHttpDataSource, Media3 1.11.1](https://github.com/androidx/media/blob/1.11.1/libraries/datasource_okhttp/src/main/java/androidx/media3/datasource/okhttp/OkHttpDataSource.java): Media3 também chama cancel quando o loader é interrompido; o cancelamento concorrente é possível no percurso real.
- [Modelo de execução do ExoPlayer, Android Developers](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer): operações do leitor e carregamentos usam processos de execução diferentes. As chamadas ao player permanecem no seu thread de aplicação.

A documentação suporta estes mecanismos. A barreira por geração e por recursos é um desenho específico da OwnTV, não uma receita oficial universal para IPTV. Nenhuma recomendação de fórum foi usada como autoridade suficiente para alterar o leitor.

## Defeitos encontrados na primeira correção

1. cancelAll verificava isCanceled após cancel. Se Media3 ou o timeout interno já estivesse a cancelar noutro thread, o segundo cancel poderia retornar antes de o primeiro acabar.
2. finished removia incondicionalmente a entrada. Um evento terminal durante response.close poderia eliminar a proteção antes de o fecho retornar, inclusive quando o fecho acabava por falhar.

## Implementação reforçada

- Cada chamada recebe um observador canceled antes de ser devolvida ao carregador.
- cancelAcknowledged e bodyCloseCompleted são estados separados. Uma fonte retirada permanece protegida até ambos confirmarem conclusão local.
- O observador apenas atualiza uma flag atómica; não espera, não fecha respostas e não escreve ficheiros.
- finished não remove uma entrada retirada durante fecho incompleto ou falhado.
- A confirmação atrasada de cancelamento permite avançar sem esperar por uma notificação terminal de DNS/intercetor ainda bloqueado.
- Mantidos cancelamento por geração, fecho dos corpos, rejeição de respostas tardias e isolamento de pedidos entre motores.
- Corrigido comentário que associava obrigatoriamente sockets ociosos a uma sessão IPTV ainda ocupada no fornecedor.

Não foram acrescentadas pausas fixas, alterações de buffer, reconstrução global do leitor, cancelamento de tráfego global ou mecanismos para ignorar 403/429. HLS apenas e volume interno a 100% mantidos.

## Validação executada

- Novo teste: isCanceled já verdadeiro enquanto o cancelamento externo está bloqueado. A abertura permanece protegida até à confirmação canceled.
- Novo teste: callback terminal durante fecho bloqueado do corpo. A proteção permanece até o fecho retornar.
- Novo teste: terminal seguido de exceção no fecho. A proteção permanece; nova tentativa bem-sucedida permite avançar.
- Mantido teste de DNS atrasado: novo pedido ao mesmo servidor abre após confirmação local, antes do evento terminal antigo.
- Teste de 30 alternâncias agora consulta a mesma barreira usada em produção.
- Compilação StandardDebug: passou.
- App: 306 testes, falhas=0, erros=0, ignorados=0.
- Leitor: 368 testes, falhas=0, erros=0, ignorados=0.
- Verificação de diferenças sem erros de espaços em branco.
- Registo final: `%TEMP%/owntv-council-cancel-ack-final.log`.
- Backup anterior ao reforço: `%TEMP%/owntv-council-cancel-ack-20261003`.

## Limites e teste no dispositivo

Esta validação confirma cancelamento e fecho no cliente. Não confirma quando o fornecedor liberta a sessão nem garante ausência de qualquer falha de rede. O teste de 30 alternâncias usa servidor sequencial, não mede todos os intervalos de receção/EOF num servidor concorrente e não é um teste completo de Media3/HLS em hardware. HTTP/2 e o fornecedor real continuam sem validação específica nesta execução.

Gerar e instalar novo APK; Forçar paragem uma vez sem apagar dados; testar A → B → C → A, mudanças rápidas e lentas, retorno a canais anteriores, Home e reabertura. Confirmar ausência da mensagem de deadline. Qualquer 403 posterior deve ser analisado separadamente.

APK não gerado. Versão não alterada. Não houve commit/publicação.
