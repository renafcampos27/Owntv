# EPG, gravações e timeshift — execução por fases

Data: 1 de outubro de 2026.

Plano de referência: `C:\Users\renat\Downloads\OwnTV_EPG_Gravacao_Timeshift_Plano_por_Fases (1).md`.
O documento original foi preservado. Os seus 123 IDs são propostas de trabalho, não 123 defeitos demonstrados. A implementação usa o Core local em `C:\Users\renat\Downloads\OwnTV_Core`, incluindo as alterações que já existiam nesse checkout.

## Decisão do council

Três revisores analisaram arquitetura/prioridades, reprodução de arquivos e segurança das gravações. Concordaram em começar pelos defeitos locais demonstráveis, reutilizar os componentes existentes e adiar funcionalidades que aumentam ligações, disco ou complexidade antes de estabilizar a navegação temporal.

### Fase 1 — primeiro lote implementado

| ID | Problema observado no código | Alteração |
|---|---|---|
| R24, mitigação inicial | Um erro genérico ou texto contendo `458` oferecia uma ação que parava todas as gravações do tracker global, sem fonte/perfil/conta. A função nem executava o retry anunciado | Removidos o ramo do HUD e `RecordingConflict.kt`. Mantido o Retry normal. A gestão de gravações continua na biblioteca |
| E18, acesso pela lista | A lista de diferidos limitava o histórico aos dias anunciados pelo canal, ocultando programas retidos de há cinco dias quando o canal anunciava apenas um | `LiveEpgReader.catchupProgrammes` usa a política existente: mínimo de sete dias para consulta, máximo de 31. Mantém o shift na consulta e a distinção entre tentativa e disponibilidade anunciada |
| E18, abertura no mpv | Um arquivo rejeitado antes de `FILE_LOADED` entrava no reset destinado a VOD comum, impedindo a alternativa Xtream já existente | O gate de `END_FILE` distingue arquivo de VOD comum e deixa o arquivo chegar à recuperação existente, incluindo `timeshift.php`. O gate é do caminho mpv; não é uma nova recuperação ExoPlayer |
| E19 | OK e a faixa de informação podiam selecionar um programa anterior numa lacuna ou um intervalo diferente do desenhado | Desenho, seleção e navegação usam os mesmos intervalos de `GuideProgrammeCells`. Lacuna não abre programa vizinho; navegação percorre apenas células visíveis |
| E01, correção parcial de apresentação | Informação memoizada podia permanecer antiga após carregamento/revisão; efeitos assíncronos podiam manter dados do canal anterior | Estado Compose das linhas e da informação é recriado por canal/janela/revisão. Sinopse tem contexto próprio. Esta alteração não implementa ainda revisão transacional de feeds no Core |

Na lista de diferidos, programas fora da janela anunciada usam o aviso e a ação de tentativa já existentes. Não se apresenta uma janela consultável como garantia de arquivo reproduzível.

As teclas ↑/↓ do fullscreen mantêm a função de mudar canal. Não se adicionou o mini-guia na tecla ↓. Não foram alterados buffers, latência, formato do direto ou a política HLS existente. Não foi gerado APK nem publicada uma versão.

## Validação do lote

Testes de regressão abrangem lacunas, limites semiabertos, sobreposições, células ocultas, lista desordenada, navegação nas duas direções e preservação dos horários originais. No Core, verificam o gate de reset para arquivo/VOD e a construção da alternativa PHP, além do caso de cinco dias com metadados de apenas um dia.

O teste do arquivo não executa eventos nativos mpv nem respostas de fornecedor. O reset comum de VOD continua coberto. A remoção da ação de gravação foi revista nos seus callers e compilada; não foi simulada uma captura numa box.

Validação integrada concluída: compilação bem-sucedida e **1 366 testes**, dos quais 205 da app, 852 do Core e 309 do player-core. Zero falhas, erros ou testes ignorados. A app foi recompilada e os seus testes repetidos após a última alteração da apresentação das tentativas de diferido. `git diff --check` da app passou.

Logs locais: `OwnTV-main/phase1-validation.log` (app + Core + player-core) e `OwnTV-main/phase1-validation-final.log` (app após a última alteração). A compilação reportou avisos de depreciação de constantes de trim de memória já presentes; não houve erro de compilação.

## Próximas fases

| Fase | Âmbito | Condição para concluir |
|---|---|---|
| 2 — pedido temporal e carregamento, implementada | T22/T23: instante UTC e contexto da sintonia; E29: loading, lista vazia e erro distintos; T01: guardas e limpeza de estado | Testes locais; ensaio do comando/box e fornecedor ainda necessário |
| 3 — cache e navegação eficiente, implementada | E01/E02: publicação/revisão e rejeição de resultados atrasados; E03: virtualizar eixo temporal e limitar cache por programas; E08: dia/hora e restauração de contexto | Consulta antiga não repovoa a nova janela; foco não se perde; medir memória e resposta em históricos curtos/longos |
| 4 — experiência prioritária, implementada | E16/E17/U01: mini-guia com mapeamento compatível com zapping, equivalências aprovadas de EPG e aviso central de atualização | Abrir/navegar guia não abre stream; variante conserva a sua conta/ID de arquivo; atualização tem ação e fecho previsíveis |
| 5 — gravação, implementada | Estados por canal/perfil, ações sobre estado atual, formato/destino, confirmação de conflito com job selecionado e libertação efetiva | Nenhuma ação para outra gravação; ficheiros reproduzíveis e recuperação sem modificar o original |
| 6 — timeshift local, implementação inicial concluída | Produtor segmentado, leitor independente, teto de disco, limpeza e eventual leitura enquanto grava | Disco e ligações medidos; intervalos confirmados; cancelamento e recuperação testados antes de integração numa barra única |

Esta sequência consolida dependências; não significa que todos os IDs de uma fase serão entregues numa única alteração. Cada lote deve ter testes pertinentes e limitações explícitas. O acesso a programas antigos não espera pelo buffer local.

## Adiar ou rejeitar

- Não duplicar `LiveArchiveUrls`, a alternativa Xtream ou os coordenadores de sintonia existentes.
- Não trocar arquivos para `.m3u8` só porque o direto prefere HLS. O formato do arquivo depende do endpoint real.
- Não diagnosticar conflito de sessões pela presença de uma gravação e uma mensagem genérica, nem repetir o stop global.
- Não testar disponibilidade de todos os arquivos em segundo plano: gasta pedidos e pode atingir limites da conta.
- Não prometer 31 dias reproduzíveis apenas porque existem no EPG.
- Não começar por miniaturas, IA, grande cache HTTP, transcodificação ou uma reescrita dos motores.
- Não usar mudanças de buffer/latência como correção genérica para identidade, horários ou seleção errada.

## Ensaio necessário na box

1. Escolher um programa conhecido de há cinco dias e confirmar título/hora, tanto no EPG como na lista de diferidos. Quando a retenção anunciada for menor, confirmar que aparece como tentativa.
2. Verificar que OK numa lacuna não abre o direto nem um programa vizinho; navegar para o bloco seguinte/anterior.
3. Mudar rapidamente o foco e atualizar o guia: a informação inferior não deve mostrar o programa do canal anterior.
4. Com gravação ativa, provocar uma falha de reprodução e usar Retry: a gravação deve continuar.
5. Recolher diagnóstico redigido se um arquivo antigo falhar. URL resolvida, resposta e primeiro frame são necessários para distinguir falha de fornecedor da app. Não exportar credenciais.

Não houve acesso ao fornecedor nem validação em Xiaomi/Thomson nesta entrega. A regressão externa de abrir programas antigos não fica declarada resolvida apenas pelos testes locais.

## Fase 2 — alterações implementadas

- **T22:** as sugestões e o seletor manual entregam o instante apresentado, em milissegundos UTC. `LiveTimeshift.beginAtInstant` conserva esse instante durante o coalescing; recalcula apenas a duração necessária até ao presente. Uma escolha expirada ou futura é rejeitada, sem clamp para outro programa. O seletor continua a usar o fuso da apresentação; o resolver mantém o fuso configurado para o fornecedor. Xtream mantém a precisão de minutos do endpoint, sem promessa de seek ao fotograma.
- **T23:** confirmação associada a perfil, fonte, canal e geração de `TuneSelection`. HUD, lista e detalhe EPG invalidam o contexto antigo; a execução verifica o token depois de consultas/suspensões e antes de abrir o leitor ou uma app externa. A mesma identidade de canal numa nova geração não reutiliza o menu antigo. Não foi criado outro coordenador de playback.
- **E29:** a lista distingue carregamento, resultado válido vazio e erro. Em erro apresenta mensagem e Retry. Cancelamento é propagado; mesmo um loader que termine tarde não publica lista após cancelamento.
- **T01:** o contexto de programa fixo é marcado apenas depois da resolução, autorização e definições, quando o pedido é entregue ao leitor. Retornos precoces não deixam o flag ativo. Sair/parar preview e mudar perfil/fontes limpam o contexto e cancelam pedidos pendentes. O token da resolução do recuo é o capturado no pedido original, não um token recapturado depois da espera.
- **Falha de resolução/recuo:** removido o fallback silencioso para direto. O aviso permite Retry no mesmo instante, Voltar ao direto por escolha explícita ou Fechar. Retry e retorno ao direto verificam o proprietário. Falhas posteriores do transporte/decoder continuam a usar o diagnóstico e a recuperação já existentes no leitor; não foi criada outra rotina automática de recovery.
- **Publicação do canal:** no recuo iniciado pela lista, canal/contexto e zapping são atualizados depois da resolução válida e das definições, antes da abertura; uma tentativa sem ligação válida não regista uma visualização bem-sucedida.

### Validação da Fase 2

Cobertura adicional: hora absoluta preservada durante espera, expiração, correção do relógio, cancelamento durante coalescing, resolver tardio que ignora cancelamento, A→B→A, perfil/fonte diferentes, lista vazia versus erro e cancelamento de leitura. O ensaio com relógio injetado valida preservação do epoch; não simula uma transição sazonal real no comando Android.

Verificação final: compilação bem-sucedida e **1 376 testes**, dos quais 210 da app, 857 do Core e 309 do player-core. Zero falhas, erros ou testes ignorados. Dez novos testes de regressão nesta fase. `git diff --check` passou para a app e para os ficheiros de timeshift alterados no Core. Log: `OwnTV-main/phase2-validation-accepted.log`.

Ensaio na box: abrir o seletor, esperar e confirmar a hora; alternar canal/perfil durante resolução; abrir guia sem dados e guia com falha de leitura; escolher um instante expirado; usar os três botões do aviso; verificar ↑/↓ no fullscreen e Back entre os níveis dos seletores. Sem APK, deploy ou pedidos ao fornecedor nesta entrega. A fase 3 foi implementada no lote seguinte, descrito abaixo.


## Fase 3 — cache, consultas e navegação do EPG

- **E01/E02:** o Core expõe eventos Room das tabelas de programas e canais, incluindo EPG nativo das playlists. A app invalida a cache após estabilização dos eventos, mesmo quando o número de programas permanece igual. Não é uma nova transação de publicação de um feed inteiro: mantém as transações dos escritores existentes e o debounce de atualização da apresentação.
- **Propriedade das leituras:** cada invalidação cancela a geração anterior. O resultado verifica cancelamento e geração antes de entrar na cache e novamente antes de ser entregue à linha. O contexto inclui perfil, fonte, canal, identificação EPG, shift e intervalo da consulta. Uma leitura que ignore cancelamento não repovoa a nova geração. Alterações de perfil, fontes, correspondências e offsets invalidam leituras; mudanças apenas de ordenação/filtro reutilizam dados compatíveis.
- **Pedidos partilhados:** linha, informação inferior e prefetch usam o mesmo pedido em curso para a mesma chave. Há no máximo quatro loaders simultâneos; o prefetch percorre apenas três canais seguintes, sequencialmente. Cancelar um consumidor não cancela o pedido ainda partilhável; invalidar a geração cancela todos os pedidos daquela geração.
- **E03 — leitura temporal:** o eixo mantém todo o intervalo consultável, mas as linhas carregam blocos de seis horas com um bloco vizinho em cada direção: até 18 horas por consulta, recortadas aos limites do guia. O bloco acompanha a zona visível. Ao alcançar uma extremidade dos dados carregados, o cursor pode atravessar para o bloco seguinte; uma lacuna não inventa um programa reproduzível. A leitura inicial também usa a janela limitada, antes de o eixo ficar posicionado.
- **E03 — memória:** LRU limitada simultaneamente a 240 linhas e 8 000 programas. Linhas vazias contam no limite de linhas; uma resposta individual acima do teto pode ser apresentada, mas não é guardada na cache. Este teto conta entidades, não representa uma medição exata de bytes ou um limite de memória total da aplicação.
- **E03 — desenho:** o eixo compõe etiquetas apenas na zona visível, com pequena margem. O espaço restante usa blocos vazios, sem centenas/milhares de Texts. A observação do scroll foi isolada num componente próprio. Nas linhas, uma procura binária encontra a primeira célula visível; apenas essas células são percorridas e têm horários formatados. O histórico completo deixa de ser percorrido em cada desenho.
- **E08:** botões Dia anterior, Hora anterior, Hora seguinte e Dia seguinte, além de Agora. Cabeçalho apresenta data e hora escolhidas. A passagem de dia conserva a hora local durante mudanças sazonais; a passagem de hora usa tempo decorrido. A navegação respeita os limites da janela.
- **Regresso ao guia:** conserva posição horizontal, posição vertical, cursor, canal/contexto e modo de seleção durante a sessão do ViewModel. Não adiciona histórico de visualizações nem persistência desse cursor depois de encerrar a app. O retorno de playback tenta restaurar foco com retries e só consome o pedido após requestFocus devolver true; removido o efeito duplicado que também tentava focar a primeira linha.

Não se alterou a ligação de streaming, o motor, os buffers ou o formato dos canais. ↑/↓ no fullscreen conserva zapping. As definições persistentes das fases anteriores mantêm o seu funcionamento. Não foi gerado APK nem publicado código.

### Validação da Fase 3

Os novos testes cobrem pedidos simultâneos deduplicados, cancelamento de um consumidor, loader tardio não cancelável, atualização com quantidade igual, orçamento de consultas, eviction LRU por programas, respostas grandes, linhas vazias, retry após falha, eixo proporcional ao viewport, limites semiabertos/lacunas, consulta limitada e passagem de dia através da mudança da hora.

Compilação e validação integrada concluídas: **1 389 testes**, dos quais 223 da app, 857 do Core e 309 do player-core; zero falhas, erros ou testes ignorados. Treze novos testes desta fase. As verificações de traduções e `git diff --check` passaram. Log final: `OwnTV-main/phase3-validation-accepted.log`. As tarefas do Core/player sem novas alterações de código nesta última execução reutilizaram a validação integrada anterior; a app foi recompilada e os seus testes executados após as últimas alterações.

### Ensaio da Fase 3 na box

1. Abrir um guia com pelo menos sete dias; navegar cinco dias para trás com os novos botões, abrir um programa e regressar: confirmar data/hora, posição e foco.
2. Percorrer muitas linhas rapidamente e alternar dias: não deve aparecer uma linha de uma janela anterior. Confirmar navegação para além de 18 horas e retorno a Agora.
3. Atualizar o EPG mantendo a mesma quantidade de programas, mas corrigindo título/horário: a grelha deve apresentar a versão nova após a atualização estabilizar.
4. Mudar de perfil/fonte e regressar ao guia: não reutilizar programas do contexto anterior. Testar Back e retorno de fullscreen; confirmar ↑/↓ de zapping.
5. Medir memória, tempo de abertura e resposta do comando na Xiaomi/Thomson, com históricos de sete e 31 dias. Os testes locais validam limites e coerência, não demonstram um ganho de FPS ou um fator de velocidade na box.

A Fase 4 foi implementada na continuação seguinte, descrita abaixo.


## Fase 4 — mini-guia, EPG entre variantes e atualização

### E16 — mini-guia durante a reprodução

- **Entrada:** Direita com os controlos ocultos abre o mini-guia; a tecla dedicada Guia também o abre. Respeitam-se os atalhos personalizados existentes, que têm prioridade. ↑/↓ fora do mini-guia mantêm zapping; CH+/CH− no mini-guia fecham-no e usam a sintonia existente.
- **Navegação:** ↑/↓ percorrem os canais do conjunto atual; ←/→ percorrem programas e atravessam blocos temporais. Back fecha o overlay. Canal focado e canal em reprodução são distintos.
- **Sem sintonia ao navegar:** lê metadados de um canal num bloco de até 18 horas, reutilizando `GuideReader`, sem abrir stream, preview, decoder ou gravação. O fallback existente de metadados pode consultar o short-EPG do canal focado; isso não abre uma ligação de vídeo nem faz probes de arquivos.
- **Confirmação:** OK num programa atual abre o direto; num programa terminado abre o arquivo da variante selecionada, se anunciar diferidos. Programa futuro, intervalo inválido e lacuna não abrem o direto. Falha de leitura tem Retry por OK. Disponibilidade não confirmada aparece como tentativa, incluindo programas mais antigos que os dias anunciados.
- **Contexto:** perfil, fontes e geração da sintonia invalidam o overlay/pedidos. O HUD fica inerte enquanto o popup possui o comando. Pedidos antigos não confirmam ações sobre uma nova sintonia. O histórico consultável usa a política existente (mínimo sete dias, máximo 31); o mini-guia permite também consultar até sete dias futuros, sem prometer disponibilidade de arquivo.

### E17 — associação manual de guia entre variantes

- **Entrada:** pressão longa em OK sobre um canal do EPG → Partilhar guia com uma variante. Só são oferecidos candidatos da mesma fonte e da família normalizada já usada em `ChannelAlternatives`, com um ID de guia e programas guardados. Nomes/regiões/+1/Notícias permanecem distintos; sufixos de qualidade podem gerar candidatos, nunca uma associação automática.
- **Escolha explícita e reversível:** seleciona-se a variante que fornece o guia; Não usar guia alternativo remove a associação. Guarda-se o ID EPG aprovado em `epgFallbacks`, por perfil e chave exata de fonte/canal. Não se cria um grafo automático de famílias nem uma ligação dinâmica ao decoder/stream do doador. Se o doador passar a usar outro ID EPG, a escolha pode ser refeita.
- **Prioridade:** correspondência manual primária/guia próprio conservam prioridade quando têm dados no intervalo consultado; o guia alternativo só é usado na sua ausência. Não se preenchem silenciosamente lacunas de um guia próprio válido com outra emissão. O regresso do guia próprio torna-o prioritário sem apagar a escolha alternativa.
- **Consistência:** grelha, mini-guia, now/next, título atual em lote, lista de diferidos e procura do programa seguinte usam a resolução correspondente. As consultas de vários canais mantêm passagens em lote para os alternativos, sem pedidos de rede por canal. Regras/reconciliação de gravações respeitam a associação no canal de destino; agendamentos escolhidos na grelha guardam o ID do programa efetivamente selecionado.
- **Identidade e horários:** fonte, canal, stream, remote ID e capacidade de arquivo continuam os da variante de destino. A associação só altera a consulta de programas. O shift do destino é aplicado uma vez; o shift do doador não é somado. Mini-guia e informação inferior do EPG mostram EPG partilhado quando a entrada vem efetivamente do alternativo.
- **Persistência:** a associação integra o JSON de personalizações/DataStore e o backup completo existente, sem migração SQLite. O restauro remapeia a chave da fonte de destino e mantém o ID do guia. Backups antigos sem o novo campo continuam com associação vazia.

### U01 — aviso central de atualização

- Substituído o cartão de canto automático por diálogo central com versão, Atualizar e Agora não. Notas extensas não aparecem obrigatoriamente no aviso automático; continuam na verificação manual das definições.
- O check automático ocorre uma vez por sessão, permanece silencioso em ausência de versão nova/erro de consulta e espera pelo fim de playback e de popups registados no host `OwnTVPopup`. A presença de popups tem proprietários próprios, incluindo menus aninhados; o aviso automático não conta como popup de utilizador, evitando ciclos de abrir/fechar.
- Back/Agora não/fechar cancelam um download pendente, revogam a sua geração e não programam instalação posterior. Chamadas HTTP e jobs são cancelados; uma nova tentativa aguarda a limpeza da anterior antes de reutilizar o ficheiro temporário. Progresso/falhas tardios não podem publicar por uma geração revogada.
- O foco inicial fica em Agora não. O guard de gesto evita confirmar Atualizar com a pressão que abriu o menu. Duplo clique reutiliza a máquina de estados existente e não abre dois downloads.
- Verificação global de gravações antes do download e antes da entrega ao instalador, repetida após copiar o APK para a sessão: gravações ativas ou previstas nos dois minutos seguintes bloqueiam a atualização com uma mensagem. Nenhuma gravação é parada automaticamente. Este gate consulta o estado atual; não substitui um futuro coordenador transacional de todos os recursos.
- Commit ao instalador verifica a geração sob lock. Callbacks nativos têm proprietário e PendingIntent por sessão; callbacks revogados não lançam uma confirmação tardia. Ao cancelar, abandona-se a sessão pendente quando o sistema permite. Depois de a instalação estar efetivamente entregue/confirmada no sistema, o ciclo do instalador Android continua a pertencer ao sistema.
- Removido `UpdateStatusToast.kt`, que ficou sem callers. Não foi publicado release nem descarregado/instalado um APK nesta implementação.

### Validação da Fase 4

Compilação e validação integrada concluídas: **1 403 testes** (228 da app, 866 do Core e 309 do player-core), com zero falhas, erros ou testes ignorados. Catorze testes novos nesta fase. Verificações de traduções e `git diff --check` passaram. Log final: `OwnTV-main/phase4-validation-accepted.log`. Cobertura nova: prioridade/reaparecimento do guia próprio, ausência do alternativo, canais sem associação sem probes adicionais, separação de contas com o mesmo remote ID, shift do destino, remoção da associação, remapeamento do backup, decisão direto/arquivo/futuro/sem capacidade, presença de popups aninhados e gravações ativas/iminentes/expiradas.

Os testes locais não executam janelas/foco Android TV, tráfego do fornecedor nem a confirmação nativa do PackageInstaller. A política de gravações e a escolha de guia são testadas localmente; o cancelamento HTTP/instalador ainda exige ensaio no dispositivo. O registo de presença cobre o host `OwnTVPopup`; não é uma reescrita de todos os popups legados nem a entrega integral de U02–U04.

### Ensaio na box

1. Num canal em fullscreen, abrir com Direita/Guia. Percorrer canais/programas e confirmar que imagem/som continuam no canal original até OK. Back fecha; ↑/↓ fora do overlay e CH+/CH− conservam zapping.
2. Escolher programa atual, antigo, futuro e uma lacuna. Confirmar que só o atual abre o direto; os casos indisponíveis explicam a ausência de ação. Verificar um programa de cinco dias como tentativa quando ultrapassa os dias anunciados.
3. No EPG, associar uma variante sem guia a uma da mesma família com guia. Confirmar títulos/horários, deslocamento e arquivo no ID/conta da variante escolhida. Retirar a associação e verificar que a configuração entra no backup/restauro.
4. Disponibilizar uma release de teste válida: verificar aviso central, foco em Agora não, Back, pressão mantida e adiamento durante outro popup/playback. Confirmar que o check sem novidades fica silencioso.
5. Cancelar durante o download e antes de confirmar no instalador; não deve surgir uma confirmação posterior de uma geração cancelada. Tentar atualizar com gravação ativa/iminente; deve mostrar o impedimento e manter a captura.

**Estado do plano:** Fases 1–4 implementadas e validadas localmente. Permanecem a Fase 5 (gravações) e a Fase 6 (timeshift local), além dos ensaios reais de cada lote. Esta continuação implementa apenas a Fase 4; o plano completo ainda não está terminado.


## Fases 5 e 6 — gravações seguras e recuo local

### Fase 5 — alterações e componentes reutilizados

- O indicador REC observa as gravações reais do **perfil, fonte e canal em reprodução**. Não guarda uma referência global à última gravação iniciada pelo botão: mudar de canal não oferece parar a gravação de outro canal; terminar pela biblioteca atualiza o indicador.
- As ações reconsultam o job pelo ID e verificam perfil, fonte, canal e estado atual. Parar só finaliza um job em RECORDING; um menu antigo de SCHEDULED não cancela um writer já admitido. Seleções de outro perfil são rejeitadas na biblioteca. A reprodução usa o destino atual, incluindo uma finalização de TS para MP4.
- `stopAndAwait` devolve apenas depois do cancelamento/join do writer e da libertação do claim. Ações destrutivas/finalização são serializadas no manager. A admissão do engine verifica a supressão sob lock, evitando abrir um writer que já foi parado/cancelado. Cancelar um agendamento só o reserva se nenhum writer foi admitido; não cancela a captura que ganhou essa disputa.
- A exclusão usa a localização final depois da paragem/remux, e conserva a linha quando não consegue remover o ficheiro. Start/Stop pelo leitor e Gravar agora partilham um mutex; um job já ativo/agendado para agora no mesmo canal é reutilizado. O perfil é verificado novamente antes de agendar, depois das suspensões.
- Claims concorrentes não perdem proprietários no registo partilhado. A nova reserva atómica de budget é usada por capturas de gravação e pelo produtor local. Esta garantia abrange os participantes desse registo; não representa uma transação de todas as aberturas de todos os motores/apps externas nem prova o limite real que o fornecedor aplica.
- Foram reutilizados os estados, destino mostrado na biblioteca, captura HLS/DASH, remux e recuperação separada já existentes. Não se criou um segundo engine ou outra recuperação que altere o original. A biblioteca continua a oferecer a paragem do job selecionado. Não foi reintroduzido um aviso automático de conflito com base apenas em HTTP 458/403; nenhum erro genérico autoriza parar gravações.

### Fase 6 — utilização

Na lista de canais: **pressão longa em OK → Ver com recuo local (até 256 MB / 30 min)**. É uma escolha para aquela sessão; a reprodução normal continua a usar o caminho habitual. O modo local usa ExoPlayer/HLS explicitamente, sem alterar a preferência persistente de motor.

Um único produtor descarrega a playlist e os segmentos; o leitor reproduz uma playlist HLS privada em `127.0.0.1`, com porta dinâmica e caminho aleatório. A playlist local refere apenas ficheiros locais, sem URL/credenciais do fornecedor. Os leitores não fazem fetches adicionais ao fornecedor. Direto local, pausa e recuo usam a janela nativa do HLS do ExoPlayer, incluindo a barra já existente. Recuar/avançar chama o seek nativo; Voltar ao direto procura a posição segura atual da playlist local. ↑/↓ mantêm zapping e encerram a sessão local antes da nova sintonia.

O modo suporta inicialmente **HLS live não cifrado, com áudio e vídeo nos mesmos segmentos MPEG-TS**. Reutiliza o parser, seleção de variante muxed e transferência validadora da gravação. Num master, fixa a variante muxed de maior bandwidth que esse seletor admite; não implementa adaptação ABR durante esta captura. Cifração, DRM, áudio separado, fMP4/init e playlist finita são recusados. Não converte formatos nem substitui um fluxo incompatível por TS do fornecedor. O ficheiro local `.ts` é um segmento dentro da ligação HLS local, não uma ligação TS alternativa ao servidor IPTV.

### Limites e ciclo de vida

- Limite de **256 MiB** para segmentos guardados/pinned e espaço reservado ao segmento temporário, mais **30 minutos** de duração confirmada: vence o limite atingido primeiro. A reserva antecipada considera o máximo de um download (64 MiB), podendo reduzir a duração/tamanho efetivamente retidos abaixo do teto. O produtor preserva ainda pelo menos 512 MiB livres em disco. Não é um buffer de 256 MiB em RAM: cópia por blocos, manifest limitado a 1 MiB e leitura por blocos de 64 KiB.
- Publica apenas depois de EOF, validação de tamanho, fsync, verificação de MPEG-TS e rename. Um `.part` nunca aparece na playlist. Início só entrega a URL após três segmentos completos, com prazo de 35 segundos; este modo opcional pode arrancar mais lentamente que o direto normal.
- O disco e a duração incluem apenas media confirmado. Segmentos saltados/gaps marcam descontinuidade; recuos de media-sequence encerram a captura em vez de anexar dados de uma emissão nova à antiga. Ausência de progresso durante 45 segundos também termina a captura. Não se promete correspondência UTC/EPG ao fotograma sem timestamps fiáveis.
- Leitores mantêm um lease do ficheiro enquanto enviam a resposta. Eviction retira-o da janela, mas só apaga quando o último leitor fecha. Ficheiros pinned continuam a contar na quota; se impedirem nova admissão, a captura falha em vez de ultrapassar o orçamento.
- Até quatro respostas locais simultâneas; GET/HEAD e Range único limitado, sem acesso arbitrário a caminhos. Fecho cancela/junta o HTTP upstream, fecha os sockets locais, aguarda os leitores e limpa os ficheiros. O produtor tem pool HTTP próprio, libertado no fecho; não purga o pool de EPG/imagens.
- A sessão está associada ao perfil/fonte/canal/geração. Cleanup captura o proprietário antigo e não fecha uma sessão posterior. Troca de canal, arquivo, saída do fullscreen, segundo plano, perfil/fontes e destruição do ViewModel encerram a captura. A sessão seguinte espera pela limpeza anterior. O diretório exclusivo em cache permite limpar ficheiros órfãos após crash no próximo arranque do modo.
- Em falha de captura, a janela já confirmada fica finita e é mostrado um aviso; o modo não abre silenciosamente o direto ou outro motor. Selecionar normalmente o canal encerra o modo e usa a sintonia habitual.
- Este recuo é temporário, não uma gravação permanente nem o arquivo de vários dias do fornecedor. Não integra ainda um eixo único UTC entre buffer local/arquivo nem leitura simultânea de uma gravação permanente em crescimento. Essas extensões eram condicionais no plano e ficam fora deste lote inicial.

### Validação e ensaio obrigatório no dispositivo

Validação integrada: **1 422 testes** — app 228, Core 884 e player-core 310 — sem falhas, erros ou testes ignorados. **19 testes novos**: identidade/estado das ações, claims concorrentes/reserva do último slot, quota/duração/reserva, leitores durante eviction/fecho, ficheiro incompleto, proteção de caminhos, ranges, produtor e servidor HTTP locais, segmento inválido, cancelamento de HTTP bloqueado e routing HLS local sem credenciais/direct_source/fallback TS/mpv. Compilação e verificações de traduções passaram. Log final: `phase56-validation-final.log`.

Não houve ensaio num fornecedor real, Xiaomi/Thomson, media codec ou superfície Android TV. O teste HTTP usa segmentos TS artificiais para validar transporte/armazenamento; não demonstra descodificação audiovisual. Não foi gerado APK, publicado código ou alterada a versão.

Ensaio recomendado:

1. Gravar A, mudar para B e usar REC: deve agir sobre B, mantendo A. Terminar A pela biblioteca e confirmar o indicador ao regressar. Testar duplo clique, mudança de perfil e um agendamento que começa enquanto o menu está aberto.
2. Abrir uma gravação HLS finalizada, confirmar extensão/destino e reprodução. Recuperar uma captura parcial e confirmar que o original permanece distinto.
3. Num HLS MPEG-TS muxed, iniciar o recuo local, esperar alguns minutos, pausar, recuar, avançar e Voltar ao direto. Confirmar que a janela cresce apenas com segmentos recebidos e que o consumo de disco respeita os limites.
4. Alternar A→B→A com ↑/↓, Back para a lista, mudar perfil e suspender a box. Verificar fecho de ligações, fim da escrita e limpeza. Reabrir o canal normalmente para confirmar que não ficou preso ao endpoint local.
5. Testar HLS cifrado/fMP4/áudio separado e falha de rede/espaço: mensagem, ausência de fallback automático e nenhuma gravação permanente afetada.

**Estado atualizado:** os seis lotes do plano consolidado têm implementação local. A Fase 6 tem o suporte inicial delimitado acima; integrações avançadas e aceitação no hardware/fornecedor continuam pendentes. Não se declara resolvido um problema de streaming real apenas por estes testes.
