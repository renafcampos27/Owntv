# EPG: shift ativo com horas originais

Data: 04/10/2026.

## Pedido

Manter o shift EPG de +1 hora que alinha os programas, sem acrescentar essa hora aos horários apresentados junto aos programas.

## Implementação

- Os horários operacionais continuam a receber o shift global ou a exceção por canal: seleção de agora/seguinte, disposição dos blocos, elegibilidade de diferidos e pedidos de reprodução.
- Os programas transportam o deslocamento aplicado como metadado temporário. As horas apresentadas são calculadas subtraindo esse deslocamento.
- O mesmo tratamento aplica-se aos programas lidos da base e ao EPG recebido diretamente do fornecedor.
- Atualizadas as horas no guia, detalhes, miniguia, apresentação agora/seguinte e seletor de diferidos. A data mostrada no seletor também usa o horário original, incluindo mudanças de dia.
- O relógio e a régua temporal continuam a representar a hora real; não são deslocados. Os intervalos escritos junto aos programas representam os horários originais do fornecedor, independentemente da posição ajustada dos blocos.
- O shift guardado não é alterado. A correção separada dos pedidos de diferidos também não é alterada nem aplicada novamente às horas apresentadas.
- Os novos metadados são ignorados pelo Room: não há nova coluna, migração ou alteração do formato de backup.

## Validação

Quatro testes adicionais: shift positivo, negativo e cópia de descrição, aplicação sucessiva de shifts e cópia de programa do fornecedor. Compilação aprovada (BUILD SUCCESSFUL em 3 min 16 s), incluindo os testes Android. Suite unitária: 1 756 testes aprovados (app 307, Core 1 032, player-core 417), zero falhas, erros ou ignorados. Sem avisos Kotlin/Java nesta execução. Os testes Android foram compilados, não executados. O código gerado do Room usa o construtor sem metadados e o esquema 50 não inclui os novos campos.

Não foi gerado APK nem testado um fornecedor real numa box. Esta alteração separa apresentação e operação; não comprova a causa do desvio do EPG no fornecedor.

## Reversão

Originais dos onze ficheiros alterados: `C:\Users\renat\Downloads\OwnTV-epg-horas-originais-backup-20261004.zip`.
