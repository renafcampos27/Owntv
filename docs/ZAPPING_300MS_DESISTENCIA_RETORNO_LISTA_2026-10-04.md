# Zapping: reavaliação de 300 ms e retorno à lista

Data: 04/10/2026.

## Alterações

- A reavaliação de pedidos HTTP anteriores pendentes passa de 200 para 300 ms. Continua a verificar imediatamente e pode acordar antes por notificação de encerramento. Não acrescenta uma espera fixa de 300 ms a todos os canais, nem autoriza abrir com transportes antigos pendentes.
- A opção de desistência do reprodutor passa a oferecer todos os segundos de 9 a 30. Mantidas as opções anteriores de 60 segundos e Nunca. O valor guardado não é alterado automaticamente.
- Os valores de 9 a 30 segundos não recebem prolongamentos por preparação do leitor ou espera do fornecedor. A pausa explícita da reprodução continua a suspender a contagem. O limite diz respeito à tentativa sem reprodução confirmada; não interrompe um canal saudável que já abriu.
- O controlador comunica a falha final juntamente com a identidade da seleção. Só uma falha da seleção atual pode fechar o leitor; callbacks de seleções anteriores não devem afetar o canal novo.
- Em caso de desistência ou falha final, a app termina a reprodução e regressa à lista de canais, usando o mecanismo existente de reposição do foco.
- Quando a recuperação por versões está ativa, uma falha final termina a tentativa dessa versão e permite tentar as restantes. Se nenhuma versão abrir, regressa à lista. Não elimina a funcionalidade anterior de recuperação automática.
- Incluído o caso de falha no encerramento do ExoPlayer antes de entregar a reprodução ao mpv.

## Configurações

- Reprodutor de vídeo → secção de direto → opção de desistência: limite da tentativa do leitor, agora com 9, 10, 11…30 segundos.
- A opção de tempo de mudança para versões alternativas continua separada; já oferece valores de 1 a 60 segundos. Não se alterou o seu valor guardado.
- Mantida a persistência existente destas definições e a inclusão no backup; sem nova chave ou migração.

## Validação

Testes do encerramento condicional atualizados para 300 ms. Acrescentadas verificações de opções disponíveis, falha terminal com identidade da seleção, seleção antiga cancelada e limite de nove segundos. Verificado também o retorno de falha por encerramento pendente.

Compilação final aprovada (BUILD SUCCESSFUL). Suite: 1 760 testes unitários aprovados — app 307, Core 1 032, player-core 421; zero falhas ou erros. A validação inclui quatro testes novos do controlador e os testes atualizados de encerramento. O registo final não contém avisos Kotlin/Java.

Não foi gerado APK nem testado um fornecedor real. A alteração de 200 para 300 ms não comprova nem garante a resolução de bloqueios de sessões no servidor.

## Reversão

Ficheiros anteriores guardados em `C:\Users\renat\Downloads\OwnTV-zapping-300ms-retorno-backup-20261004.zip`.
