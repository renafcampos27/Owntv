# Robustez da libertação e abertura de canais HLS — 2026-10-03

## Alterações implementadas

1. `PlaybackHttpCalls.retireAsync()` invalida imediatamente as fábricas antigas, sem fazer cancelamento de sockets ou fecho de respostas no thread da interface. Um executor reutilizável faz a libertação; mudanças rápidas são agrupadas num trabalhador por proprietário. As chamadas ao ExoPlayer permanecem no thread da aplicação.
2. Uma falha de cancelamento/fecho recebe uma segunda passagem limitada. Recursos que continuam sem confirmação de libertação permanecem registados e impedem a nova abertura. Não se apagam entradas para simular sucesso, não se recria o leitor em cada mudança e não se contorna o limite de sessões do fornecedor.
3. A identidade do proprietário fica associada ao `Call`, permitindo registar uma resposta tardia mesmo depois da remoção inicial do pedido. Enquanto o fecho dessa resposta decorre, a entrada permanece pendente; callbacks terminais não podem removê-la prematuramente. Se o fecho tardio falhar, é agendada recuperação sem invalidar a sintonia nova.
4. Os pedidos HTTP de reprodução passam por `ProviderBackoffGuard`, antes de DNS/ligação. Recusas 429 (com a espera indicada ou o valor já existente de recurso) e 503 com `Retry-After` registam uma espera que sobrevive à mudança de canal. A origem lógica da sintonia acompanha os pedidos redirecionados para CDN. Não se regista uma espera para um 403 genérico.
5. Esperas de contas Xtream diferentes no mesmo servidor são separadas por uma chave com digest das credenciais extraídas do percurso `/live/` ou `/timeshift/`; o registo não contém as credenciais em texto. URLs sem identidade Xtream reconhecível usam o âmbito do servidor. A passagem do tempo usa relógio monotónico. Uma recusa com prazo mais curto não encurta um prazo já registado.
6. A limpeza dos sockets inativos usa um executor reutilizável e agrupa pedidos repetidos. Os comentários deixam de tratar um socket inativo como prova de uma sessão remota ocupada.
7. O diagnóstico regista duração da passagem de fecho, cancelamentos ainda sem confirmação, respostas por fechar e fechos tardios. O erro de timeout distingue estes dados dos contadores de eventos HTTP.

## Comportamento mantido

- HLS apenas quando essa opção está ativa; volume interno a 100%; zapping pelas setas.
- Identidades distintas em A → B → C → A e rejeição de callbacks antigos.
- Verificação imediata da libertação local. A abertura ExoPlayer aguarda apenas se houver recursos antigos pendentes: notificações de fecho acordam a verificação imediatamente, com reverificação de recurso a cada 200 ms. Não existe margem obrigatória depois do fecho.
- EPG, diferidos, gravações e configurações de buffer não foram alterados.

## Validação executada

Compilação `:app:compileStandardDebugKotlin` e suites:

| Suite | Testes | Falhas / erros / ignorados |
|---|---:|---:|
| App — standardDebug | 306 | 0 / 0 / 0 |
| player-core — debug | 383 | 0 / 0 / 0 |
| core — debug | 997 | 0 / 0 / 0 |

`BUILD SUCCESSFUL`; verificação de literais técnicos/i18n e de whitespace aprovada.

Os oito testes novos cobrem: retorno imediato da libertação com fecho bloqueado; agrupamento de trocas rápidas e preservação do pedido novo; recuperação de falha transitória; limite de tentativas numa falha persistente; resposta tardia com fecho em curso; espera antes do próximo pedido; origem lógica em CDN; isolamento de contas/servidores e preservação do maior prazo. Alguns comportamentos partilham o mesmo teste.

### Revisão adicional: resposta entre invalidação e cancelamento

O utilizador confirmou que ainda não tinha instalado o APK com as alterações anteriores. Os bloqueios observados na box não são, por isso, um resultado de teste desta implementação.

Foi entretanto reproduzida uma falha adicional por teste automatizado: depois de `retireAll()`, se os headers chegassem antes de o trabalhador cancelar o pedido, `opened()` rejeitava e fechava a resposta, mas não cancelava o transporte nesse caminho. A confirmação `canceled` podia ficar em falta. A interleaving em que o trabalhador ignora temporariamente uma entrada com fecho tardio podia deixar a barreira pendente.

Correção: a rejeição da resposta cancela também o pedido, mantendo o fecho do corpo num `finally`. Assim, não depende da ordem de execução do trabalhador. Uma falha continua a agendar recuperação limitada e não invalida a sintonia nova.

O nono teste novo falhou antes da correção e passou depois. Compilação, 306 testes da app e 377 testes do leitor passaram na validação final. Os 997 testes do Core passaram na validação anterior desta implementação; o Core não foi alterado nesta revisão adicional. Esta reprodução não demonstra que a janela seja a causa de todos os bloqueios observados na box.

## Limites e decisões

- A confirmação é local: não demonstra quando o fornecedor liberta a sessão da conta.
- Uma operação de I/O que não regressa pode ultrapassar o tempo da barreira, mas decorre fora da interface. A app termina a tentativa com erro; não abre sobre um fecho não confirmado.
- O teste existente de 30 substituições HTTP usa servidor sequencial. Não constitui prova de ordenação observada por um servidor concorrente.
- Não foram executados testes de ExoPlayer numa box/smartphone físico nesta implementação.
- Por correção posterior do utilizador, os 200 ms são um intervalo de reverificação condicional, não uma margem de libertação remota. A confirmação continua a referir-se aos recursos locais.
- Não foi gerado APK, alterada a versão ou publicado código.

## Teste na Xiaomi

Gerar o APK a partir de `OwnTV-main`, que usa o Core local em `C:/Users/renat/Downloads/OwnTV_Core`.

1. Com HLS apenas e ExoPlayer, percorrer A → B → C → A pelo menos 30 vezes, incluindo intervalos inferiores a cinco segundos.
2. Repetir saindo de fullscreen para a lista e voltando ao canal.
3. Confirmar que o comando e a lista respondem durante a libertação e que apenas a escolha atual fica em reprodução.
4. Se houver bloqueio, guardar o diagnóstico detalhado com `http_retirement`, `source_drain_timeout`, códigos HTTP e tempos; ocultar credenciais. Distinguir falha local de fecho de 403/429/503 reais.

Backup dos cinco ficheiros de produção anteriores: `%TEMP%/owntv-hls-robustez-20261003`.

## Histórico: margem adicional de 200 ms, substituída pela espera condicional abaixo

`LivePreviewEngine` passa a pedir uma margem a `SourceDrain` quando já preparou uma fonte anteriormente. Primeiro aguarda recursos antigos sem fecho confirmado; depois espera 200 ms e verifica novamente se existem recursos pendentes. Se uma resposta antiga estiver em fecho, não avança para a nova fonte. O job e a identidade da sintonia continuam a impedir aberturas de escolhas entretanto abandonadas.

A primeira preparação do motor continua sem margem. A promoção de um preview existente não abre outra fonte e não recebe este atraso. As trocas e reaberturas posteriores recebem a margem, inclusive a reabertura do mesmo URL. A barreira tem orçamento total de 1,7 s quando a margem está ativa; sem margem mantém 1,5 s. Recursos não libertados causam timeout, não uma abertura forçada.

Cinco testes adicionais verificam: margem depois do fecho, margem com transporte já fechado, cancelamento por nova escolha, resposta tardia durante a margem e timeout sem autorização de abertura. Compilação e suites da app/leitor aprovadas: 306 e 382 testes, sem falhas. Nenhum APK foi gerado. Backup desta revisão: `%TEMP%/owntv-hls-grace-200ms-20261003`.

## Comportamento final: 200 ms condicionais, com notificação de fecho

O utilizador pediu para retirar a espera obrigatória de todos os canais. `SourceDrain` verifica recursos pendentes antes de esperar. Se não houver, a abertura prossegue imediatamente. Se houver, aguarda uma notificação de alteração de cancelamento/fecho, com timeout de 200 ms para reverificar caso a notificação não chegue. A notificação nunca autoriza uma abertura por si só: o predicado de recursos pendentes é sempre consultado novamente.

Não há intervalo adicional depois do fecho, nem distinção artificial entre primeira fonte e fontes seguintes. Mantêm-se cancelamento da abertura por escolha mais recente e limite total da barreira de 1,5 s. Cancelamentos e fechos antigos continuam a decorrer fora da interface; a nova fonte só avança com a confirmação local exigida.

Os cinco testes de margem foram adaptados para verificar espera condicional. Um sexto teste, usando `PlaybackHttpCalls` e um `Call` real do OkHttp sem o executar na rede, confirma que a notificação de libertação aos 50 ms permite concluir aos 50 ms em vez de esperar 200 ms (tempo virtual de teste, não medição da box).

Validação final: compilação aprovada, 306 testes da app e 383 do leitor, sem falhas, erros ou testes ignorados. Não foi gerado APK.
