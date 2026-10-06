# Regresso a um canal recente: espera de quatro segundos

Data: 04/10/2026.

## Regra implementada

Ao regressar a um canal abandonado há menos de 20 segundos, o controlador termina a reprodução anterior e aguarda pelo menos quatro segundos antes de pedir o canal novamente. Aplica-se a A→B→A e A→B→C→A, incluindo outras sequências de regresso.

A janela começa quando se sai de um canal para outro: ver A durante vários minutos e regressar rapidamente depois de B também fica protegido. Aos 20 segundos exatos ou depois, esta espera específica não se aplica. Primeiras aberturas e outras mudanças sem regresso recente mantêm o comportamento anterior.

## Encerramento e cancelamento

- Pedida a paragem dos motores antes da espera; utilizados o controlo de pedidos HTTP retirados e a confirmação disponível da fila de encerramento do mpv.
- Se a primeira verificação de encerramento falhar, é repetida após a espera. Um pedido que terminou durante esses quatro segundos pode então permitir a abertura.
- O novo pedido não é autorizado se o encerramento continuar a falhar; o caso segue o retorno por falha final à lista de canais.
- Uma nova escolha cancela a espera anterior. A identidade da geração é verificada após as suspensões.
- O tempo de desistência do leitor começa após esta espera deliberada. A recuperação por versões também deixa de contar a espera de passagem como uma falha da versão.
- A reavaliação condicional de 300 ms continua a existir no caminho normal de pedidos pendentes.

## Dados e âmbito

O controlo guarda apenas identificadores de lista/canal e instantes monotónicos na memória do processo. Não acrescenta histórico visível, URLs, dados persistentes ou novas definições. O caminho de HLS local fica fora desta regra de novas ligações ao fornecedor.

## Validação

Compilação aprovada. Suite: 1 772 testes unitários aprovados, sem falhas, erros ou ignorados. Inclui 12 novos testes de sequências, fronteira de 20 segundos, fontes diferentes, cancelamento, encerramento tardio ou falhado e compatibilidade da espera com os tempos de desistência/versões.

Não foi gerado APK nem executada reprodução física numa box. Os mecanismos locais não comprovam que o servidor já libertou a sessão; os quatro segundos são uma margem deliberada, não uma confirmação do estado do fornecedor.

## Reversão

Originais dos ficheiros existentes alterados: `C:\Users\renat\Downloads\OwnTV-regresso-canal-4s-backup-20261004.zip`.

Ficheiros novos: `player-core/src/main/java/tv/own/owntv/player/RecentChannelReopen.kt` e `player-core/src/test/java/tv/own/owntv/player/RecentChannelReopenTest.kt`.
