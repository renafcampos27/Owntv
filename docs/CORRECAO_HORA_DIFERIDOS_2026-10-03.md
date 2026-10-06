# Correção horária dos diferidos sem deslocar o guia

## Problema

Adiantar o EPG uma hora altera simultaneamente a grelha e a hora enviada ao arquivo. Isso pode compensar um desfasamento do servidor e abrir o programa certo, mas deixa a hora apresentada errada. A correção do pedido e a correção do guia precisam de controlos independentes.

## Implementação

Nova opção: **Definições → Programação retroativa → Correção dos diferidos**. Ajuste em passos de 30 minutos, com reposição a zero. Um valor positivo pede um instante posterior ao servidor; não altera o programa apresentado, a duração, a posição temporal do leitor ou os dados EPG guardados.

O fuso horário do servidor continua separado. O novo ajuste é relativo, aplicado ao instante antes da formatação no fuso escolhido; não significa UTC+1.

Aplicado uma vez, ao construir o pedido, em:

- EPG, com reprodutor interno ou externo;
- seleção de programa e recuo no leitor em direto;
- gravação/download de programas antigos e recuperação do arquivo.

Gravações de direto e agendamentos mantêm os seus horários. A correção não muda a disponibilidade de arquivo indicada pelo fornecedor.

Definição persistente: `catchup_request_shift_minutes`, predefinição 0, incluída na exportação/restauro das configurações. As definições antigas do EPG não são migradas automaticamente: podem ter sido usadas para corrigir efetivamente o guia.

## Configuração para o caso relatado

Se o guia estava correto sem o ajuste, repor o ajuste EPG usado anteriormente em 0 e colocar **+1 hora na nova Correção dos diferidos**. Se o ajuste antigo era específico de um canal, repor esse ajuste no canal. Manter o fuso dos diferidos que já estava selecionado para não somar duas compensações.

## Validação

Testes de regressão para zero, +1 hora sem alterar o guia, -1 hora cruzando meia-noite, duração do pedido e independência entre os dois ajustes.

Compilação da app/Core/player-core e testes unitários concluídos com sucesso. Core: 1011 testes; player-core: 405; app: 307. Total: 1723 testes. Registo: `%TEMP%\owntv-epg-correction-validation.log`.

Não foi observado o arquivo real do fornecedor. O ajuste permite corrigir o desfasamento reportado; não determina automaticamente o fuso ou o relógio desse servidor. Não foi gerado APK.
