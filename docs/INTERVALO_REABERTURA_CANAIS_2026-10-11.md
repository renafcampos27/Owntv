# Intervalo configurável de reabertura de canais

## Objetivo e estado

O utilizador observou que a reabertura rápida A → B → A também falha noutras aplicações. Uma restrição temporal do fornecedor é uma hipótese plausível; o bloqueio não deve ser marcado como definitivamente resolvido sem ensaio real. Esta alteração permite experimentar o intervalo necessário.

## Definição

Definições → Leitor de vídeo → TV em direto → **Intervalo para reabrir o mesmo canal**.

- Valores inteiros de **10 a 50 segundos**, com passos de 1 segundo.
- Predefinido: **20 segundos**.
- Intervalo mínimo contado desde a saída do canal, não desde o primeiro fotograma.
- Se o intervalo for 20 s e tiverem passado 15 s, a app aguarda apenas os 5 s restantes.
- Não impõe esta espera à primeira abertura, a um canal novo ou à promoção de um canal que já esteja a reproduzir.
- Identidade por origem e canal; versões diferentes continuam a ser canais diferentes.
- Substitui a regra anterior de janela de 20 s com espera fixa adicional de 4 s.

## Implementação e limites

`ChannelReopenInterval` centraliza o valor predefinido e os limites. `SettingsRepository` persiste a opção e inclui-a na exportação/importação das definições, com validação 10–50 s. Backups antigos sem esta chave continuam válidos. `PlaybackSettings` disponibiliza o valor ao controlador desde a primeira sintonia.

`RecentChannelReopen` mantém os instantes de saída no processo usando relógio monotónico. Seleções canceladas não são marcadas como ligações abertas e não prolongam a espera original. O retorno à lista com paragem do leitor também regista a saída. Não se guarda histórico de acessos nem URLs nesta estrutura; a configuração persiste, os instantes de saída não sobrevivem à morte do processo.

`LiveTuneController` cancela as tentativas anteriores, pede a paragem/libertação dos dois motores, aguarda apenas o restante intervalo e volta a verificar recursos que ainda estejam pendentes antes do novo pedido. Mantém a proteção que recusa abrir quando a libertação não foi confirmada. Esta confirmação é local: não prova que o servidor já apagou a sessão. A espera não é contabilizada no prazo de desistência nem na troca automática de versões. O teto global de segurança das alternativas também exclui esta espera, com crédito limitado a 50 s para que um estado de espera preso não fique infinito. Escolher outro canal ou sair cancela a espera. O leitor de timeshift HLS local permanece isento.

## Ensaio na box

1. Gerar e instalar um APK novo.
2. Começar com 20 s e repetir A → B → A e A → B → C → A.
3. Se persistir, aumentar progressivamente para 25, 30, 40 e 50 s mantendo as restantes definições.
4. Quando se encontrar um intervalo estável, diminuir em passos de 1 s para encontrar o menor intervalo fiável.
5. Verificar igualmente primeira abertura, retorno à lista, cancelamento durante a espera e restauração de backup.

A limitação do fornecedor pode ser por canal ou por conta; esta opção atua apenas na reabertura do mesmo canal. Não garante resolver HTTP 403/404, limites de sessões ou recursos presos por outras causas.

## Verificação

- **1 967 testes passaram**, sem falhas: app 331, Core 1 150, player 486.
- Compilação Kotlin Standard Release e lint vital de release concluídos com sucesso.
- Verificação de textos/traduções passou.
- Testes cobrem intervalos, fronteiras, A/B/C/A, cancelamentos, saída para a lista, alteração da configuração, confirmação de libertação e orçamento de abertura separado de uma espera de 50 s.
- Sem APK gerado, commit ou publicação. Sem teste físico ou confirmação de qual intervalo o fornecedor exige.
- Registo final: `C:\Users\renat\AppData\Local\Temp\owntv-channel-reopen-validation-final.log`.
