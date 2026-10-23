# OwnTV TV — fase 5: comando, definições e continuidade

**Âmbito autorizado:** UI-01/02/03/04/07/08/10 e PLY-07 do plano principal. **Data:** 22/10/2026.

**Estado:** implementada e validada automaticamente no Core e na App. A aceitação física na box continua pendente. Não foi gerado APK nem iniciada a fase 6.

## UI-01 — DNS sem apagar a configuração

- Parser puro comum para apresentação, Guardar e Testar: hostname/IPv4, IPv6 com ou sem colchetes e porta explícita entre 1 e 65535. Em IPv6, uma porta personalizada exige `[endereço]:porta`.
- DoH exige HTTPS, sem credenciais embutidas nem fragmento. Não se faz resolução de nomes durante a validação da entrada.
- Deixa de haver conversão silenciosa de porta inválida para 53 no teste/edição. Guardar e Testar usam a mesma interpretação.
- O editor espera pela configuração real antes de iniciar o rascunho. Desativar conserva os dados anteriormente guardados; ativar/preparar um preset requer Guardar e mostra «Alterações por guardar».
- Foco inicial no interruptor, sem saltar automaticamente para um preset que pudesse modificar a configuração. Erro de entrada apresentado junto aos controlos.

## UI-02 — proxy e endereços de fontes

- Guardar e Testar proxy validam o mesmo host e porta, incluindo IPv6. Uma entrada inválida não substitui a configuração ativa. Desativar conserva servidor, porta e credenciais guardados.
- Campos de fontes e EPG verificam o tipo de endereço antes de permitir iniciar/guardar: HTTP(S), portas privadas, ficheiros locais e `content://` mantêm-se. Portais continuam a usar também a validação específica já existente.
- A edição de EPG mantém os identificadores internos de portal reconhecidos pelo Core.
- Origens repetidas mostram um aviso e podem ser adicionadas separadamente. Não há fusão, remoção ou alteração automática de credenciais.
- As alterações de edição por identidade e sincronização da fase 2 foram preservadas; não se substituiu esse percurso.

## UI-03 — foco em números e pesquisa das definições

- O diálogo numérico restaura o foco se o botão −/+ ficar desativado por atingir um extremo. Se ambos estiverem desativados, o campo de número é o destino válido.
- Pedidos de foco têm tentativas limitadas e cedem a nova ação do comando. Campo, Repor e Concluir deixam de ser confundidos com o último botão de incremento focado.
- A pesquisa dá prioridade às ações específicas que transportam secção/linha, em vez de as substituir por entradas genéricas com o mesmo título.
- Resultados têm destinos de foco próprios e limitados ao conjunto atual. O regresso de um diálogo/subpágina pode voltar ao resultado que o abriu. Falta de destino termina com fallback explícito; flags antigas não permanecem pendentes após esgotar as tentativas.
- Reutilizado o contrato de restauro após diálogo que já cancela perante input; não foi criada uma segunda rotina global de restauro.

## UI-04 — listas agrupadas e ações de gravação

- Resolvida separadamente a posição visual e a identidade do item: cabeçalhos ocupam espaço, mas não contam como gravações/downloads selecionáveis.
- Ao remover um item, o cursor procura o vizinho no lugar visual esperado; ao mover de grupo, segue o mesmo ID.
- Restauros após ações são consumidos e deixam de voltar a roubar foco em atualizações posteriores. Nova ação do comando ou troca de perfil invalida o pedido antigo.
- Lista vazia tem um destino explícito: estado vazio focável nas gravações ou separador no host de downloads. Regresso com lista vazia conclui a indicação de restauro.
- Não foram reintroduzidos menus de filmes/séries ou funções retiradas. O componente partilhado de downloads recebeu apenas a correção de foco.

## UI-07 — continuidade no início

- Atualizar os dados conserva o cartão ativo pela sua chave, incluindo mudanças de ordem. Reduções e listas vazias têm índice limitado.
- Um preview não é parado apenas por o seu cartão deixar de ser o primeiro; verifica-se se a identidade continua no novo conjunto.
- O resultado da consulta volta a verificar o perfil ativo antes de publicar. A seleção atual é lida depois da consulta, para não repor um índice capturado antes de uma ação recente.
- Atualizações em fundo deixam de causar o scroll incondicional do ecrã para a linha zero. O restauro real continua a poder rolar e focar o destino, cedendo ao comando.
- A secção «No momento» atualiza a hora a cada 30 segundos enquanto a linha está visível e a App está em primeiro plano. A visibilidade usa estado derivado, sem observar todos os píxeis de scroll no nó raiz. Ao expirar a janela carregada, pede atualização do guia; não altera o relógio/offset global do EPG.
- Seleção de canal nessa secção segue a identidade após atualização; índices são limitados e os extremos permitem saída D-pad previsível. Reentrada na linha hero usa o cartão ativo.

## UI-08 — entrega única de atalhos

- Consumir um deep link limpa o seu URI no Intent e regista o consumo no estado da Activity.
- Recriação conserva uma ação já consumida e preserva uma ação ainda não entregue.
- Um novo Intent, mesmo para o mesmo canal, cria uma nova revisão. Um callback antigo não pode consumir a revisão seguinte.
- A validação de perfil/visibilidade continua no resolvedor existente. Launcher e entrada normal da App conservados.

## UI-10 — estados e acessibilidade

- Bitrate não declarado aparece como «—». Uma taxa de transferência realmente medida a zero continua a aparecer como zero, o que é normal entre pedidos HLS.
- Códigos de idioma não presentes na lista traduzida são apresentados como código, sem serem falsamente rotulados «Automático».
- A superfície de ação partilhada expõe seleção e papel de botão também nos percursos com toque médio/longo. Os nomes acessíveis já existentes dos controlos do leitor foram conservados; não se acrescentaram descrições repetidas aos logótipos decorativos.
- A leitura física em 720p e por leitor de ecrã ainda precisa de validação na box.

## PLY-07 — mudança de canal além da página visível

- A abertura pela lista indica se o contexto está completo ou paginado. Listas completas e passos dentro da janela já carregada continuam em memória.
- Ao atravessar uma página, mantém-se o contexto capturado: perfil, fontes, pesquisa, ordenação, categorias personalizadas, ocultações e versões agrupadas.
- O novo índice retém IDs, não um segundo catálogo de entidades/URLs. A leitura não agrupada percorre páginas de 256 fora da thread principal; versões agrupadas reutilizam os IDs produzidos pela mesma consulta agrupada da lista.
- A leitura dos canais do destino é feita numa pequena janela circular de, no máximo, 17 entidades, por uma consulta de IDs. Não se abrem streams vizinhos.
- Geração, canal âncora e contexto são conferidos antes de publicar uma janela. Uma consulta atrasada não substitui uma lista escolhida depois. A passagem para um contexto de sintonia numérica fora da janela descarta o carregador anterior.
- A cache preexistente do catálogo agrupado não foi redesenhada nesta fase; não se afirma que toda a App passou a reter apenas 17 entidades. A primeira passagem de fronteira numa lista grande ainda exige medir a duração da indexação na box.

## Validação e integridade

- Core: **1 360 testes JVM**, sem falhas/erros/ignorados; motores: **610**, sem falhas/erros/ignorados. Cinco novos testes de validação de endereços.
- Testes Android Core compilados, não executados em dispositivo.
- App: **452 testes JVM**, sem falhas/erros/ignorados. Compilação standard debug/release e lint vital de release aprovados.
- Total: **2 422 testes JVM**, incluindo **16 testes novos** nesta fase (5 Core e 11 App), além de seis testes da ferramenta de snapshot. Não são 16 bugs ou ensaios físicos.
- Seis testes da ferramenta de snapshot aprovados.
- i18n Core: 263 entradas de baseline + 5 837 literais técnicos; App: 47 + 1 089. A dívida declarada da App diminuiu de 51 para 47 entradas ao remover formatação antiga de DNS. Não se classificou texto novo visível como técnico.
- Log Core: `%TEMP%/owntv-phase5-core.log` — sucesso em 4 min. Log final App: `%TEMP%/owntv-phase5-app-complete.log` — sucesso em 52 s, com resultados reutilizados apenas nas tarefas cujos inputs não mudaram.
- Configuração normal, sem forçar o snapshot na linha de comando: `%TEMP%/owntv-phase5-normal-config.log` — sucesso em 5 s; resolveu o mesmo SHA-256 personalizado.
- As execuções intermédias detetaram uma exposição indevida de tipo interno, uma referência de composição por qualificar e uma regressão na validação do perfil do Home. Esta última bloqueava o contrato de entrada com perfil já validado pelo chamador e provocou seis timeouts de teste. Corrigida mantendo esse contrato e recusando alterações de perfil durante a consulta; acrescentado teste de regressão. A suite completa final passou. Não se alteraram os limites nem as asserções dos seis testes existentes para os fazer passar.
- Avisos já existentes no suporte de testes de Home/timeshift e de funcionalidades Gradle obsoletas continuam visíveis. Lint não foi desativado nem contornado com baseline.
- Snapshot **1 061 inputs**, SHA-256 `1dcc86280d668242d65d379f3b845b357a73901464e4674b71b941212afb5011`, conferido por hash, inventário e bytes com o Core externo.
- Cópia anterior e manifesto verificados: `C:/Users/renat/Downloads/OwnTV-fase5-backup-20261022/`. Snapshot anterior `12015db51124dafae2de1cff483eb7147d1a15cc780858b7b254c1fa2da2d942` conservado.
- Esquema 51, contratos de backup, gravações, motores, HLS/exceções, buffers, reabertura, configurações por canal, correção dos diferidos +1 h e volume interno de 100% preservados. Sem alteração do OwnTV Mobile, APK, assinatura, commit, push ou publicação.
- Não se mediram percentagens de desempenho/robustez. Os oito IDs são blocos do plano, não oito bugs reproduzidos na box.

## Aceitação física pendente

1. DNS: desligar/reabrir/religar, preset sem Guardar, IPv6/porta inválida e teste da porta personalizada; repetir proxy com e sem credenciais.
2. Pesquisa das definições → opção específica → alterar → fechar/Voltar; confirmar cursor no resultado e ausência de refoco após nova ação.
3. Diálogo numérico no mínimo/máximo, digitar, Repor e Concluir; comando sempre utilizável.
4. Gravações em dois grupos: parar, mover de estado, apagar primeira/intermédia/última, lista vazia e troca de perfil durante a ação.
5. Atualizar o início enquanto está num cartão intermédio; sair/regressar; confirmar programa «No momento» e saídas D-pad.
6. Abrir um atalho, recriar a Activity, receber novo atalho para o mesmo canal e trocar de perfil durante a resolução.
7. Lista com mais de 300 canais: CH+/CH− nas fronteiras, ordenação personalizada, pesquisa, ocultações e grupos; conferir tempo da primeira indexação sem antecipar streams.
8. Diagnóstico com bitrate ausente e transferência zero entre segmentos; idioma não listado, leitor de ecrã e apresentação em 720p.

**Fase 6 continua por iniciar.**
