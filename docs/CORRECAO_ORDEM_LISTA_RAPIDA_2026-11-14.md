# Ordem personalizada na lista rápida do leitor — 14/11/2026

Pedido: a lista rápida aberta durante a reprodução deve respeitar os canais movidos na lista principal.

## Problemas verificados no código

- O painel apresentava diretamente `zapChannels`, que podia ser um snapshot anterior à edição da ordem. A invalidação do catálogo preparava o próximo passo de zapping, mas não atualizava imediatamente esse snapshot ao abrir o painel.
- Entradas sem um contexto de navegação explícito podiam carregar a categoria do fornecedor, mesmo quando a lista principal selecionada era Todos os canais, com outra ordem personalizada.
- A janela circular usada no zapping podia publicar, por exemplo, C–A–B em vez de A–B–C. O destino de zapping estava correto, mas a apresentação começava a meio da ordem guardada.

Estes percursos são verificáveis no código e nos testes. A confirmação do percurso concreto observado na box depende de testar o novo APK com a ordem do utilizador.

## Correção

1. Abrir a lista rápida passa a preparar uma leitura com **a mesma função `livePagingSource`, perfil, contexto e modo de ordenação da lista principal**. A pesquisa temporária não limita este atalho de canais do contexto.
2. A leitura respeita as posições manuais, agrupamento de versões, ocultação, categorias e nomes personalizados. Quando válido, reutiliza o índice de grupos já calculado pela lista principal.
3. O resultado atualiza a lista partilhada pelo painel e pelo zapping. Não abre novamente o stream, não interrompe a reprodução e não altera as posições guardadas.
4. A janela apresentada mantém-se contínua na ordem do catálogo; a mudança do último para o primeiro canal continua a funcionar, sem rodar a lista visualmente.
5. A seleção do painel reconhece o representante de um grupo quando o canal em reprodução é outra qualidade desse grupo.
6. Uma mudança de canal, perfil, definições ou categoria impede uma leitura anterior de substituir o contexto novo. Uma revisão do catálogo durante a leitura permite uma segunda tentativa limitada. Falhas conservam a lista anterior.

O trabalho de catálogo corre fora da thread principal. Entidades são obtidas em lotes de até 400 IDs e mantém-se o limite anterior de **2000 canais em memória** para o painel. Listas maiores usam uma janela ordenada em torno do canal; a navegação paginada mantém o índice completo de IDs. Não existe atraso fixo introduzido no atalho, consulta por fotograma ou novo pedido de streaming.

## Ficheiros

Raiz: `C:/Users/renat/Downloads/OwnTV-main/`.

- `app/src/main/java/tv/own/owntv/features/live/LiveViewModel.kt`: preparação da lista com a consulta partilhada, regras de visibilidade/agrupamento, seleção da janela e verificação do contexto.
- `app/src/main/java/tv/own/owntv/features/live/LiveZapList.kt`: atualização assíncrona protegida e identidade da categoria escolhida explicitamente.
- `app/src/main/java/tv/own/owntv/features/live/PagedZapIndex.kt`: janela ordenada e destino com retorno ao início/fim.
- `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt`: preparação no atalho e seleção do canal/grupo atual.
- `app/src/test/java/tv/own/owntv/features/live/LiveZapListTest.kt`, `PagedZapIndexTest.kt`: ordem manual, navegação, limites, revisões concorrentes e resultados obsoletos.
- `tools/i18n/safe_literals.txt`: apenas o novo diagnóstico técnico e a ocorrência adicional de uma pesquisa vazia. Baseline de tradução original preservada.

**Apenas App.** Core e `core-source-lock.json` mantêm o snapshot `dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`. Sem migração, alteração das listas guardadas ou mudança das regras de HLS, áudio, ligação entre canais, letra a 120% e abertura discreta.

Backup e provas: `C:/Users/renat/Downloads/OwnTV-ordem-lista-rapida-2026-11-14-163450/`.

## Validação concluída

- **525 testes JVM da App aprovados**, zero falhas, erros ou skips. Incluem 24 testes do controlador de zapping e oito da paginação/janela; sete testes são novos nesta correção.
- Compilação Standard Release e `lintVitalStandardRelease` aprovados, com dependências em verificação estrita e o Core bloqueado existente.
- Inventário de tradução aprovado com a baseline original de 47 entradas/56 ocorrências, byte a byte igual ao backup. Apenas dois literais técnicos novos/adicionais foram classificados.
- Hashes dos nove inputs registados antes da validação final reconfirmados depois. `core-source-lock.json` permanece byte a byte igual ao estado inicial.
- Backup contém `validation-final.log`, `validated-input-hashes.json`, `validation-summary.json` e resultados XML dos testes.
- Sem geração/instalação de APK, execução física, commit ou push. Não se atribui uma percentagem de melhoria nem se considera a aceitação na box concluída.

## Aceitação na box

Mover dois canais na lista principal, guardar, abrir um canal e usar o atalho. Confirmar a mesma sequência, seleção do canal/grupo atual e Cima/Baixo. Repetir depois de fechar/reabrir a app, numa categoria personalizada, com agrupamento ativo e ao passar do último para o primeiro canal. Verificar também Voltar e que abrir o painel não reinicia a emissão. A confirmação física ainda está pendente.
