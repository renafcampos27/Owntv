# Implementação dos lotes 3 e 4 — auditoria de 15/10/2026

Âmbito: aplicação TV `OwnTV-main` e Core TV `OwnTV_Core`. O projeto Mobile e o seu Core privado não foram alterados. Este relatório complementa os lotes 1–2; a otimização experimental da grelha (A15-11/lote 5) continua pendente de medição.

## Lote 3 — coerência da interface

- **A15-08:** o Guia limpa a pesquisa no `ON_STOP` real da atividade, excluindo mudança de configuração. Sair apenas da secção Guia ou abrir/fechar o leitor fullscreen não limpa a pesquisa. A desativação de trabalho do Guia continua separada deste evento.
- **A15-09:** a pesquisa mantém os resultados pelo nome original e acrescenta candidatos pelo nome personalizado. Resolve só chaves personalizadas correspondentes em lotes de 400 parâmetros, por origem, sem carregar o catálogo inteiro. Uma consulta de IDs EPG verifica origem/período em lotes; a filtragem existente de itens/categorias ocultos, perfis infantis e apenas diferidos mantém-se. Há deduplicação e limite final de canais. Não há migração de base de dados.
- **A15-10:** associação EPG, desvio por canal e herança do Guia recebem um proprietário capturado na abertura da janela. Perfil, origens, restauro e saída para background invalidam a sessão. As escritas usam o perfil capturado e revalidam o contexto antes de guardar; as continuações de atualização do desvio também revalidam depois da espera. A associação/desvio na lista TV usa a mesma proteção.
- **A15-12:** o estado de permissão de alarmes é observável e atualizado ao abrir/retomar as definições de gravação, incluindo regresso das definições Android. Sem sondagem contínua. Não altera a política do agendador.

## Lote 4 — ferramentas, CI e ensaios

- **A15-13:** geração em ficheiro temporário, ZIP imutável identificado por SHA-256 e troca atómica do lock. Versão inválida e falhas de leitura/publicação deixam o par anterior utilizável. Arquivos antigos não são apagados. Ver `tools/core_snapshot/README.md` para publicação e recuperação; a resistência a falhas de energia continua sujeita às garantias do sistema de ficheiros.
- **A15-14:** removidas as dispensas de testes/i18n para ramos `bump/core-*`. A fusão automática de pins foi substituída por política manual sem permissões de escrita. Removidos o input obsoleto `force_core_source` e o repositório GitHub Packages não usado pela resolução TV. O repositório nativo libmpv mantém-se; credenciais de publicação do Core não foram alteradas.
- **A15-15:** a CI verifica o snapshot, compara o catálogo de idiomas da App com o ZIP efetivamente usado e executa explicitamente os testes da App, `core` e `player-core`. Inclui os testes de publicação do snapshot e recolhe os relatórios dos módulos do snapshot.
- **A15-16:** a preparação do benchmark identifica o filtro apenas diferidos e explica como o desligar para a fonte sintética sem arquivo. A documentação exige repor a escolha anterior ao terminar. O ensaio não altera preferências nem anuncia arquivo inexistente. Emulador verifica o percurso; desempenho requer box física.
- Removidos dois `maxLines = Int.MAX_VALUE` redundantes no painel de diagnóstico: a política de texto continua multilinha e a verificação de overflow deixa de falhar.

## Validação

Core validado isoladamente: 1 255 testes de `core` e 549 de `player-core`, sem falhas, erros ou testes ignorados; compilação dos testes Android do Core aprovada. Ferramenta de snapshot: 6 testes aprovados, incluindo falhas de publicação e corrupção de arquivo imutável. A consulta SQL real foi verificada em SQLite para origem, chave, período e deduplicação. Estrutura YAML e verificação do snapshot da CI verificadas localmente.

Snapshot: 997 ficheiros, 4 004 077 bytes, SHA-256 `312b86242ef11eb10374811c68a875df36a0ba1ee7751d12d3b79ae5b660759f`. Versão personalizada mantém-se `1.0.57-own.20261011`; não declara equivalência com outra release upstream.

Validação integrada concluída: **BUILD SUCCESSFUL**, com 2 213 testes unitários (1 255 Core + 549 player + 409 App), sem falhas, erros ou testes ignorados. App compilada em debug/release; `lintVitalStandardRelease` aprovado; testes Android da App/Core e módulo benchmark compilados. Os testes Android e benchmarks não foram executados num dispositivo.

Inventário i18n da App aprovado: 51 entradas baseline e 1 073 literais técnicos classificados. Verificações de formatação numérica e overflow de texto aprovadas. A validação inicial do Core aprovou também o respetivo inventário de literais. Verificação de whitespace aprovada.

Registos: `C:/Users/renat/AppData/Local/Temp/owntv-lotes34-core.log` e `C:/Users/renat/AppData/Local/Temp/owntv-lotes34-final.log`. A CI foi configurada e os seus verificadores foram inspecionados/executados localmente; não foi disparada uma execução no GitHub. Não foi gerado APK, commit ou push. Ensaios físicos na box continuam pendentes.

## Verificação na box

1. Pesquisar no Guia; abrir um programa fullscreen e voltar: pesquisa mantém-se. Carregar em Home e reabrir: pesquisa vazia.
2. Procurar um canal pelo nome personalizado e pelo original. Confirmar que categorias/canais ocultos e origens desativadas não reaparecem.
3. Abrir edição EPG, sair para Home e voltar antes de guardar: a ação antiga não deve escrever no contexto novo. Reabrir a janela para editar.
4. Nas definições de gravação, conceder permissão de alarmes nas definições Android e voltar: aviso deve atualizar sem reiniciar a app.
5. Para benchmark sintético, anotar e desligar apenas diferidos; confirmar programas no Guia e repor a escolha depois.

Os testes automáticos não substituem validação visual, foco D-pad e ciclo de vida numa box. Não foi pedido APK, publicação ou alteração dos parâmetros de reprodução.
