# OwnTV — implementação das melhorias de interface

Data: 13 de outubro de 2026.
Âmbito: aplicação Android TV `OwnTV-main`. Sem alterações no Core, nas migrações de base de dados ou nas configurações persistidas.

## Fase 1 — Clareza dos estados e configurações

- O leitor distingue a espera de reabertura, a espera indicada pelo fornecedor e o pedido de ligação. Mostra o nome do canal pedido e identifica a tentativa de outra versão.
- Os detalhes do diagnóstico conservam até dez tentativas falhadas desta execução, com canal, hora e código HTTP quando disponível. A observação do código está associada à identidade da tentativa; não é transportada para outra sintonia. URLs, credenciais e mensagens brutas não são guardadas neste registo.
- O editor por canal mostra os valores pedidos e a respetiva origem: canal, lista ou definições gerais. Usa o snapshot de reprodução e o resolvedor de reserva já utilizados pelo leitor. O diagnóstico continua a ser a referência para o motor e descodificador efetivamente ativos.
- As falhas ao guardar têm uma mensagem própria e libertam o estado de operação em curso, incluindo em caso de exceção.

## Fase 2 — Edição e navegação

- A ordem das versões fica provisória até Guardar. Cancelar/fechar descarta a edição de ordem. Restaurar ordem automática também fica pendente até Guardar.
- Adicionar e desassociar versões mantêm o efeito imediato e isso é indicado no diálogo. A atualização das linhas conserva a ordem provisória dos itens ainda existentes.
- Após mover uma versão, a lista acompanha o item e tenta devolver-lhe o foco pelo identificador, em vez de pela posição anterior.
- As consultas de versões têm estados de ausência de resultados, falha e repetição. O estado de adição é libertado em caso de falha.
- A lista distingue o canal atual através de um pequeno indicador, sem substituir o destaque do cursor.
- O Guia TV mostra a data do programa selecionado, distingue carregamento/ausência/falha e indica quando o diferido desse programa não está disponível. Conserva a navegação pelos programas existentes durante a atualização do intervalo.

## Fase 3 — Diagnóstico e pesquisa

- O diagnóstico abre em resumo: motor, formato, vídeo, áudio, descodificação, buffer, limiares e duração do último segmento publicado.
- OK alterna entre resumo e detalhes; cima/baixo deslocam o conteúdo. Os detalhes incluem as tentativas anteriores e os dados técnicos completos.
- A largura dos títulos adapta-se ao espaço do ecrã. Dados ausentes tipados são distinguidos de números medidos quando a API o permite; não se interpreta automaticamente um zero como ausência de medição.
- A pesquisa das definições reutiliza os textos normalizados e aceita termos sem acentos. Os valores e ações continuam atuais; não são congelados com o índice de pesquisa.
- Os estados de foco existentes são mantidos; o indicador de canal atual tem um significado separado.

## Validação

- Compilação Kotlin Standard Release: validada.
- Lint vital Standard Release: validado.
- Testes da app: 380, sem falhas, erros ou testes ignorados na verificação final, incluindo o ajuste do nome apresentado.
- Testes novos: privacidade/limite do registo de falhas, isolamento entre tentativas e pesquisa independente de acentos/locale.
- Atualizado o teste de cobertura de pesquisa para reconhecer a normalização separada, mantendo as verificações de cobertura das páginas e opções.
- Sem APK gerado, sem commit ou publicação.

## Verificação na box

1. Abrir A, mudar para B e regressar a A: conferir contador e nome do canal pedido. Após falha e troca automática, abrir diagnóstico → detalhes.
2. Editar a ordem, cancelar e reabrir: a ordem guardada deve continuar igual. Editar novamente, guardar e reabrir: a nova ordem deve persistir. Repetir com restauração automática.
3. Mover uma versão várias posições com o comando, incluindo limites e itens ocultos. Verificar que o foco continua visível e acessível.
4. Confirmar valores herdados de uma lista e um valor personalizado, incluindo latência automática da lista sobre latência global personalizada.
5. Percorrer dias anteriores no Guia TV e regressar: conferir data, foco e programas durante carregamentos.
6. Pesquisar `definicoes`/`reproducao`, abrir uma opção e alterar o valor: o resultado deve refletir o valor novo.
7. Verificar resumo/detalhes e deslocação do diagnóstico na resolução real da TV.

A validação automática não comprova legibilidade ou comportamento físico do D-pad. Estas alterações de interface também não demonstram que um bloqueio HTTP do fornecedor ficou resolvido.

