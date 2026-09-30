# Reabertura do mesmo canal após fullscreen

28/09/2026

## Problema e evidência

Sintoma relatado: sair do fullscreen e voltar ao mesmo canal deixa a reprodução a carregar. O caminho de saída, com preview desligado, já chama stop/clearMediaItems no ExoPlayer. Contudo, a limpeza adicional de rede apenas expulsava ligações inativas do pool; não cancelava explicitamente os pedidos ainda em curso. Esta é uma fragilidade confirmada no código, não uma reprodução do problema na box nem prova de causa única.

## Alteração

- Novo registo de pedidos HTTP por instância de LivePreviewEngine.
- stop/release cancelam os pedidos registados, incluindo leituras em curso, antes da desmontagem do player.
- Factories antigas são invalidadas: um pedido tardio da reprodução terminada nasce cancelado mesmo se o URL for igual ao da nova reprodução.
- A reprodução nova usa outra geração. Outros players, EPG e tráfego geral não são cancelados.
- Pedidos terminados são removidos pelo EventListener; não se guardam URLs/credenciais no registo.
- Sem espera fixa, alteração de buffer, formato, motor ou reinicialização global a cada zapping.

## Validação

Compilação da app concluída. 243 testes do player passaram, incluindo 5 novos testes: cancelamento, reabertura do mesmo URL, pedido criado durante a saída, isolamento entre motores e leitura HTTP bloqueada num servidor local. A validação na Xiaomi/Thomson permanece pendente; o cancelamento local não garante a rapidez com que o fornecedor liberta uma sessão do lado do servidor.

Teste na box: gerar e instalar novo APK, com preview desligado abrir o canal, sair e reabrir imediatamente 10 vezes; repetir após esperar 2 segundos. Se continuar, recolher o diagnóstico desse intervalo para distinguir rede/sessão, superfície de vídeo e decoder. Não mudar outras opções durante a comparação.


## Complemento — troca direta entre variantes

O relato de bloqueio após 6–7 mudanças entre SIC/SIC HD/SIC Full HD revelou uma lacuna de cobertura: o cancelamento anterior estava ligado a stop/release, mas a troca direta usa reprepare sem chamar stop. Agora reprepare invalida a identidade da fonte anterior, cancela os seus pedidos HTTP e só depois cria as factories da nova fonte. Abrange zapping e reconexões; a promoção de preview continua a reutilizar o stream existente.

Teste de regressão adicional: 30 substituições entre três URLs, com três pedidos por transmissão e tentativas tardias de todas as factories antigas. Verifica cancelamento dos pedidos anteriores, rejeição de pedidos tardios e sobrevivência dos pedidos atuais após callbacks antigos. Complementa o teste de interrupção de uma leitura HTTP real num servidor local. Não simula o fornecedor IPTV nem o decoder da box.

A acumulação de sessões no fornecedor continua a ser uma hipótese, não um diagnóstico confirmado na box. Se o bloqueio persistir, recolher o diagnóstico durante as mudanças, incluindo pedidos ativos/HTTP, estado do player e decoder, em vez de acrescentar novos atrasos ou resets indiscriminados.


## Complemento — espera pelo encerramento efetivo dos pedidos

Sintoma adicional: falha com mudanças espaçadas menos de cinco segundos, tanto pelas setas como pela lista. Sem logs da box, a causa concreta permanece por confirmar.

O cancelamento HTTP passa a ser seguido de uma barreira assíncrona no caminho comum de preparação: só instala a nova fonte quando os pedidos de fontes anteriores tiverem terminado (EventListener callEnd/callFailed, incluindo corpo da resposta). A verificação distingue fonte e sintonia, pelo que uma reconexão ao mesmo URL também espera pela tentativa anterior. Sem pedidos anteriores, não há atraso. A espera tem teto de 1500 ms e não bloqueia a thread da interface.

Ao expirar o teto, para/limpa o media item antigo e comunica erro ao mecanismo de recuperação existente; não abre outra transmissão sobre os pedidos ainda pendentes. Uma escolha nova, saída ou encerramento cancela a preparação em espera. A identidade da fonte e da instância do player é novamente verificada antes de abrir. Logs source_drain_wait/source_drain_timeout permitem saber se esta barreira foi necessária.

Não foi acrescentado um atraso fixo de cinco segundos, nem um reinício do ExoPlayer em todas as mudanças. Isto confirma o fim dos pedidos no cliente; não confirma que o fornecedor já libertou a sessão, nem que um decoder da box está operacional. Essas condições exigem o teste e os logs do dispositivo.

Testes adicionais: caminho imediato sem atraso, espera até terminar o último corpo, timeout na reconexão ao mesmo canal e cancelamento por nova seleção. Ver resultado da execução no resumo desta entrega.
