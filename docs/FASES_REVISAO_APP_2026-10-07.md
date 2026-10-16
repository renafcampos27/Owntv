# OwnTV — fases da nova revisão de 7 de outubro de 2026

## Âmbito e estado

Este documento refere-se à revisão posterior às melhorias registadas em MELHORIAS_APP_2026-10-07.md. A numeração não é a desse documento anterior.

Pedido: aplicar todas as recomendações por fases, começando apenas pelos pontos 1 e 2. Esta primeira fase está implementada. Os restantes pontos continuam pendentes. Sem APK nem publicação no GitHub.

## Ponto 1 — guardar a ordem dos canais com segurança

- O editor só fecha depois de a transação da base de dados terminar com sucesso.
- Enquanto guarda, mostra «A guardar…» e bloqueia Guardar, Cancelar, deslocações, Desfazer, Restaurar e fecho por Voltar/toque exterior.
- Se a escrita falhar, conserva o rascunho e mostra uma mensagem que permite tentar novamente. Desfazer e Cancelar continuam disponíveis depois da falha.
- A operação usa o perfil e o contexto capturados na sessão. Uma conclusão antiga não pode fechar nem alterar outra sessão de edição.
- A leitura das posições conservadas e a substituição do contexto decorrem numa única transação Room.
- Canais de fontes inativas/fora do conjunto editado mantêm a sua ordem relativa. Outros perfis, contextos e tipos não são substituídos.
- Não foi necessário alterar o esquema da base de dados.

Ficheiros principais: LiveViewModel.kt, OrderDraftCommitter.kt, MoveOrderOverlay.kt, ContentOrderDao.kt e ContentOrderMerge.kt.

## Ponto 2 — regras de gravação e falhas de agendamento

### Programas duplicados entre fontes de EPG

Antes de calcular os aliases de gravações existentes e os novos agendamentos, as regras usam a mesma deduplicação do guia: EpgDedupe.collapse, com preferência pela origem do canal.

O mesmo título em períodos sobrepostos é tratado como uma única emissão; a emissão preferida conserva os seus horários. Repetições reais em períodos distintos são mantidas. Não se apagam feeds nem gravações/agendamentos existentes. A reconciliação de horários e a correção dos diferidos mantêm o comportamento anterior.

### Alarme que não pôde ser preparado

A lista de gravações observa os identificadores que o RecordingScheduler não conseguiu armar. Uma gravação ainda agendada mostra «Necessita de reativação» e o botão «Reativar agendamento».

A reativação:

- relê o registo e confirma perfil, canal, origem, estado e janela ainda válida;
- tenta preparar o alarme existente uma vez por ação;
- impede pedidos repetidos enquanto a ação decorre;
- conserva o registo e os ficheiros;
- remove o aviso se tiver sucesso ou mostra uma mensagem de falha;
- repõe o foco na linha quando a ação termina.

A falta de autorização para alarmes exatos continua a usar o fallback para alarmes inexatos; não é confundida com falha total de agendamento. Não foi criado um ciclo de tentativas automáticas. Os indicadores de falha pertencem ao processo; no arranque, a rotina existente relê os agendamentos persistidos e volta a preparar os alarmes, reconstruindo os indicadores quando falha.

Ficheiros principais: RecordingManager.kt, RecordingsViewModel.kt e RecordingsScreen.kt.

## Validação executada

- Compilação Kotlin de appStandardDebug: aprovada.
- App: 321 testes, zero falhas/erros/ignorados.
- Core: 1 067 testes, zero falhas/erros/ignorados.
- Total nesta fase: 1 388 testes, incluindo 11 novos.
- Novos casos: gravação repetida durante escrita, rascunho preservado após falha e nova tentativa, conclusão antiga, mudança de proprietário e cancelamento; conservação de posições fora do conjunto editado; contexto/perfil/tipo; IDs repetidos; EPG duplicado e repetição verdadeira.
- Testes existentes de fallback de alarmes e propagação de falhas também aprovados.
- Inventário de literais: sem novos textos de interface fora dos recursos. Recursos novos em inglês e português europeu validados.
- Código gerado por Room confirma que replaceEditedContext envolve a leitura e a escrita numa transação.

Não foi executada validação visual numa box/smartphone nem teste instrumental de rollback em Room num dispositivo. Os testes de alarmes cobrem a política de falhas/fallback, não uma falha real do AlarmManager numa box. Não se demonstra um ganho de FPS ou de zapping com esta fase.

## Verificação no dispositivo

1. Alterar a ordem de dois canais, Guardar e voltar a abrir a lista: a ordem deve persistir.
2. Alterar a ordem, Desfazer e Cancelar: a edição não deve ser gravada.
3. Uma regra com duas fontes do mesmo programa deve criar um único novo agendamento; uma repetição posterior deve continuar a poder ser agendada.
4. Se houver uma falha real ao preparar um alarme, confirmar o aviso na lista de gravações e a reativação manual, sem duplicar o registo.

## Recomendações restantes — pendentes

3. Evitar consultas de filmes/séries em percursos usados apenas para canais e corrigir a reatividade da pesquisa à mudança de fontes.
4. Adaptar os controlos de ordenação a ecrãs compactos.
5. Simplificar o ajuste de desvio de áudio.
6. Melhorar a deslocação tátil do EPG.
7. Mostrar as definições efetivas por canal e recuperar o foco ao regressar de resultados de pesquisa nas definições.
8. Otimizar a leitura/atualização de avatares e rever a política de cache de imagens.
9. Alinhar os limites de importação e exportação de configurações por canal.
10. Antecipar espaço necessário para gravações.
11. Aplicar configurações a vários canais com pré-visualização e preservação das opções não escolhidas.

A próxima fase só deve alterar um grupo destes pontos, com validação própria. Esta implementação termina nos pontos 1 e 2.

## Pedido adicional — remover meteorologia e animações

Removidas as opções do menu e da pesquisa das definições, o seletor de animações, a página de meteorologia e os controlos sem utilização do SettingsViewModel. Ambas ficam efetivamente desativadas, inclusive após importar um backup antigo. As chaves persistidas foram conservadas para compatibilidade; meteorologia não efetua pedidos de rede com esta política. Compilação appStandardDebug e SettingsSearchCoverageTest aprovados. Sem APK nem validação visual em dispositivo.

## Pedido adicional — remover a opção OpenSubtitles

Removidos o acesso à conta OpenSubtitles no menu, o resultado da pesquisa das definições, os identificadores de agrupamento sem utilização e o acesso à pesquisa online de legendas no leitor. A janela dessa pesquisa deixou de ser montada pelo shell. Mantidos legendas integradas no stream, importação de ficheiros locais e dados/API do Core para compatibilidade, sem apagar legendas já guardadas. Compilação appStandardDebug e os três testes de SettingsSearchCoverageTest aprovados. Sem APK.
