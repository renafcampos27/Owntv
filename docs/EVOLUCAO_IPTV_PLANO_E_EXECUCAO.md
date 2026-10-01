# Evolução IPTV — plano autorizado e execução

## Âmbito aprovado

Implementar os pontos 1 e 3–9 do plano de melhorias. O ponto 2, pesquisa de programas, está excluído. Preservar o modo simples, o zapping ↑/↓, a opção HLS exclusivo, os diferidos e as gravações. Não acrescentar favoritos, histórico, filmes ou séries.

As fases abaixo são incrementais; aprovação do plano não significa que todas estejam implementadas.

## Fase 1 — comando, barra temporal e disponibilidade do diferido

Estado: implementada; compilação e testes automatizados verificados. Validação na box e com fornecedor real pendente.

- Com os controlos ocultos: OK curto revela-os; OK prolongado abre ações para guia, lista de canais, direto quando aplicável, áudio, legendas e informação do stream. A ação ocorre ao libertar OK. A libertação não pode acionar o primeiro botão do novo painel.
- Preservar ↑/↓ para mudar de canal enquanto os controlos estão ocultos. Com os controlos visíveis, as setas continuam a navegar entre botões. Os diálogos e a informação técnica impedem o zapping pelo handler do HUD.
- Na barra do diferido do fornecedor, Left/Right pré-visualizam o destino. Um toque move 10 segundos; após 1 segundo premido, cada passo passa a 30 segundos; após 3 segundos, a 60 segundos. Repetições mais rápidas que 100 ms não acrescentam passos.
- Libertar a seta envia um único pedido relativo ao ponto então reproduzido. Perder o foco ou mudar de canal descarta a pré-visualização. Os botões de recuo/avanço mantêm a definição de passo existente.
- A geometria da barra e das marcas usa a mesma janela; a visualização pode expandir quando o destino ultrapassa as duas horas, limitada à profundidade declarada para o canal. Essa profundidade não prova disponibilidade no servidor. Não se inicia uma consulta EPG a cada passo.
- EPG e seletor de diferidos distinguem anúncio do fornecedor, tentativa fora do intervalo declarado, reprodução iniciada recentemente e última tentativa falhada. Uma URL válida não é evidência de reprodução; uma falha de rede não é prova de remoção do programa.
- Observações do leitor interno são separadas por perfil/fonte/canal/início/fim, limitadas a 256 entradas e não persistidas: início expira após 15 minutos, falha após 5. O observador é cancelado ao mudar de sintonia, lê apenas o player existente e termina após início, erro ou 48 segundos. Dois avanços consecutivos da posição, sem buffering, confirmam apenas o início, não o programa completo.
- O leitor externo não produz confirmação fictícia. Não se fazem sondagens automáticas, conexões adicionais nem tentativas extra de recuperação.

Limites: a barra nativa HLS/local e a barra de ficheiros mantêm o seu comportamento anterior; a alteração de gesto desta fase incide na barra do diferido do fornecedor. Não altera o motor, buffers, rede ou armazenamento.

Aceitação manual: OK curto/prolongado sem clique duplo; abrir/fechar menu; ↑/↓ antes/depois; manter seta 5 segundos sem sucessivos carregamentos; libertar e carregar uma vez; sair durante o gesto sem pedido tardio; abrir programa e observar indicação de início no EPG; falha não impedir nova tentativa.

## Fase 2 — integridade das gravações

Estado: implementada. Compilação e testes automatizados verificados; validação real de gravação, falta de espaço e recuperação na box pendente.

- Apresenta duração prevista do programa e, quando diferente, janela de captura com margens. Duração capturada continua a ser a dos segmentos completos confirmados; tempo decorrido e tamanho do ficheiro não passam a ser duração de vídeo.
- Apresenta duração estimada em falta e número de lacunas conhecidas. Os checkpoints atuais não identificam horários absolutos de cada lacuna: não se inventa uma cronologia nem uma percentagem de cobertura do programa baseada na soma de segmentos e margens.
- Persiste separadamente `captureFailure` e `finalizationFailure`, preservando também o motivo agregado existente. A migração Room 48 → 49 acrescenta colunas opcionais; os registos antigos ficam com fase desconhecida, sem reclassificar ficheiros ou apagar dados.
- Falha na montagem, conversão ou promoção impede oferecer Reproduzir. A mesma regra é verificada no controlo interno ao abrir a gravação, após reler a linha atual.
- Erros inesperados na finalização HLS/DASH são convertidos em resultado de finalização falhada, preservando os segmentos/faixas recuperáveis. Interrupção do processo não é apresentada como diagnóstico confirmado de falha de rede na captura.
- O original mostra o estado da tentativa de recuperação mais recente. A recuperação continua numa linha e num ficheiro separados; novas tentativas não herdam os erros de fase do original.
- Não há sondagens de ficheiros, scans de disco nem pedidos de stream durante a composição da lista. A relação entre originais e recuperações é calculada uma vez quando as linhas mudam.
- Quotas, reserva de espaço, orçamento de sessões e cancelamento existentes mantêm-se. Não foi acrescentada recuperação automática nem união de intervalos em falta.

Testes adicionados: ficheiro por finalizar não reproduz; parcial finalizado pode reproduzir; estado antigo desconhecido preservado; linhas ativas/sem caminho/sem bytes recusadas; associação da recuperação ao original; migração real SQLite idempotente com preservação de caminho, bytes e motivo antigo.

Aceitação manual: gravação HLS normal; interrupção/rede; falta de espaço na finalização; original preservado; recuperação numa cópia independente; atualização de uma instalação com gravações antigas. A existência de ficheiro guardado não garante integridade de todos os frames/áudio do programa.

## Fase 3A — grupos e prioridade de versões

Estado: implementada. Compilação e testes automatizados verificados; validação na box pendente.

Definições disponíveis em **Definições → Personalização do menu lateral**, junto às opções de mudança automática:

- **Agrupar versões do mesmo canal**: opção desligada inicialmente; quando ativa, uma linha por canal lógico.
- **Prioridade de versões do mesmo canal**: opção desligada inicialmente.
- Ordem automática quando ativada: **Full HD → HEVC → HD → normal → Low**. Reconhecer `hvec` como alias de HEVC. Um nome que inclua Full HD e HEVC ocupa a classe Full HD. Nomes sem indicação reconhecida de qualidade ficam na classe normal.
- **Prioridade personalizada do grupo**: reordenar as versões; ordem manual prevalece no respetivo grupo. Grupo sem personalização usa a ordem automática quando a opção estiver ativa.
- **Versões ocultas**: excluídas da seleção inicial e de todas as alternativas automáticas, mesmo que constem da ordem manual guardada. Respeitar também categorias ocultas e restrições do perfil.
- Ao voltar a mostrar uma versão, ela regressa à posição manual guardada, ou à classe automática correspondente. Ocultar não elimina a configuração nem o canal da fonte.

Regras:

1. Reutilizar identificação/grupos e cancelamento existentes. Não juntar canais diferentes por correspondência parcial: RTP 1 e RTP 2 permanecem distintos. Números no nome não são descartados indiscriminadamente.
2. Abrir uma linha agrupada escolhe a primeira versão visível elegível. Escolha explícita de uma versão pelo utilizador é respeitada nessa sintonia.
3. Falha percorre alternativas elegíveis na ordem configurada quando a mudança automática existente está ativa, respeitando o seu tempo escolhido. A prioridade não introduz mudanças periódicas nem liga a recuperação sem autorização do utilizador nas definições.
4. ↑/↓ navega pelos canais lógicos no modo agrupado; com agrupamento desligado preserva a lista atual.
5. Não transformar qualidade nominal em qualidade comprovada. Não forçar HEVC em equipamento sem suporte; conservar políticas de reprodução e HLS exclusivo.
6. Preferências por perfil e identificação estável do grupo/versão; persistência e backup completo. Atualizar uma lista não deve perder prioridades por causa de IDs recriados.

Implementação: reutiliza os grupos conservadores existentes, separados por fonte. As prioridades usam IDs do fornecedor (ou nomes originais), em vez dos IDs locais recriados na atualização. As opções e ordens ficam nas personalizações do perfil e integram o backup completo; no restauro, apenas a identidade da fonte é remapeada.

Na lista, **OK prolongado → Versões do canal** abre as versões disponíveis e permite reproduzir uma versão diretamente, subir/descer a prioridade e repor a ordem automática. Uma versão oculta mantém a posição guardada e não permite reprodução pelo painel. Categorias ocultas e restrições do perfil continuam respeitadas.

O agrupamento aplica-se às listas de canais em direto e ao zapping; o EPG mantém a estrutura atual. A preparação lê o catálogo local por blocos em trabalho de fundo, conservando apenas um representante por grupo. A lista normal mantém a paginação anterior. Não há sondagens de streams nem ligações extra para determinar a prioridade. Em catálogos grandes, a primeira preparação agrupada pode demorar; não foi medida aceleração na box.

Testes adicionados: ordem automática e aliases, Full HD+HEVC, ordem manual, atualização dos IDs locais, versões ocultas e reposição, grupo totalmente oculto, identidade distinta entre fontes/canais, paginação sem repetição com tamanhos diferentes, invalidação Room, zapping a partir de uma alternativa e backup com remapeamento da fonte.

Aceitação manual: confirmar ambas as opções desligadas após atualização; ativar agrupamento e prioridade; verificar Full HD/HEVC/HD/normal/Low; ocultar a primeira versão e confirmar substituição; reordenar e repor; escolher Low explicitamente e confirmar que abre Low; testar ↑/↓ após uma alternativa; confirmar que a recuperação só funciona quando ativada e usa o tempo escolhido; reiniciar, alternar perfil, atualizar lista e restaurar backup, verificando a preservação das opções e ordens.

## Fase 3B — pausar/retomar downloads de diferidos

Estado: implementada para arquivos HLS finitos com segmentos completos e checkpoint confirmado. Compilação e testes automatizados verificados; teste com o fornecedor e armazenamento da box pendente.

- Na biblioteca de gravações/downloads, **Pausar download** aparece depois de o arquivo HLS confirmar segmentos completos e uma playlist finita (`EXT-X-ENDLIST`). O download passa para **Downloads em pausa**, com **Retomar download**. Gravações em direto, TS bruto, DASH e playlists dinâmicas conservam o comportamento anterior: não se anuncia uma retoma cuja continuidade não pode ser provada.
- A pausa regista a intenção na base de dados, cancela o pedido ativo e aguarda a libertação do escritor, dos corpos HTTP e da ligação contabilizada. Conserva segmentos completos e checkpoint; não monta o ficheiro final. Uma pausa explícita sobrevive ao reinício e não retoma automaticamente.
- A retoma obtém uma URL atualizada para o intervalo original do programa, com os cabeçalhos atuais do canal. Não aplica Range à playlist e não descarrega novamente todos os segmentos anteriores. Reutiliza as sequências confirmadas depois da validação; apenas volta a descarregar o último segmento completo para comparar tamanho e SHA-256, sem o acrescentar ao ficheiro.
- O checkpoint liga a captura ao registo, perfil, fonte e intervalo do programa. A assinatura temporal compara sequências, durações, descontinuidades, lacunas e tipo de inicialização, sem guardar URLs ou tokens. As identidades de variante/codecs declarados e a verificação de inicialização fMP4 existentes continuam em vigor. Não é uma análise completa de todos os codecs dentro dos payloads TS.
- URLs com assinaturas novas são permitidas. Sequências renumeradas, timeline modificada, segmento de continuidade diferente ou ausência do material necessário recusam a junção. A app conserva a captura em pausa e apresenta o motivo. Uma recusa HTTP não prova por si só que o programa expirou.
- Falta de armazenamento, orçamento de ligações ou fonte disponível não apaga nem transforma os segmentos em ficheiro reproduzível. A reserva de espaço, quota e limite de downloads de arquivo por fonte mantêm-se. As janelas de execução são renovadas; o intervalo EPG original mantém-se.
- Após uma retoma falhada, **Recuperar do arquivo** permite pedir uma cópia separada pelo mecanismo existente. Não há reinício destrutivo ou substituição automática do original.
- A migração Room **49 → 50** acrescenta `archivePaused`, desligado nos registos antigos. A escolha de versões da fase 3A e as outras configurações mantêm-se. Os ficheiros de gravação e checkpoints continuam locais ao dispositivo, sem entrarem no backup portátil.

Testes adicionados: elegibilidade da pausa, persistência do checkpoint, URL assinada renovada, IDs locais recriados, isolamento por programa/perfil/fonte, timeline modificada, sequência expirada, descontinuidade/container incompatível, rejeição de payload diferente, limpeza de fragmento incompleto, segmentos sem duplicação e migração SQLite idempotente com preservação do material.

Aceitação manual: descarregar um programa antigo em HLS; pausar após aparecer o botão; confirmar que o progresso para e a ligação é libertada; reiniciar a app/box e confirmar que fica em pausa; retomar e reproduzir o resultado sem repetição do início; repetir pausa/retoma; tentar após expiração/alteração da origem; USB removido e quota cheia; excluir uma captura em pausa; pedir recuperação separada após recusa. Os testes automatizados não reproduzem o comportamento do fornecedor ou de USB real.

Referência de protocolo: [RFC 8216, HLS](https://www.rfc-editor.org/rfc/rfc8216), secções 4.3.3.2, 4.3.3.3 e 6.3.5. As salvaguardas de identidade e conteúdo acima são decisões da aplicação, não garantias do fornecedor.

## Fase 4A — ver uma gravação enquanto está a gravar

Estado: implementada para captura HLS em TS segmentado e fMP4, no leitor interno. Compilação e testes automatizados verificados; leitura/seek reais na Xiaomi/Thomson e USB pendentes.

- **Ver gravação em curso** aparece na biblioteca depois de existirem segmentos HLS completos confirmados. A reprodução começa no início do material guardado; os controlos do leitor permitem pausa e navegação dentro do material disponível. A ação **Parar gravação** continua disponível separadamente.
- Reutiliza a captura existente. O servidor privado `127.0.0.1`, protegido por token aleatório, publica uma playlist HLS `EVENT` incremental. Não abre ligações ao fornecedor, não descarrega outro stream, não expõe credenciais nem aceita caminhos de ficheiros enviados pelo cliente.
- Só segmentos depois do commit durável entram na playlist. Fragmentos em escrita, registos internos e segmentos inexistentes são recusados. fMP4 publica a inicialização completa via `EXT-X-MAP`; lacunas e mudanças de descontinuidade têm marcas próprias. A barra mede o material disponível, sem inventar cobertura dos intervalos perdidos.
- A playlist cresce sem retirar os primeiros segmentos e recebe `ENDLIST` quando a captura termina, é interrompida ou pausada. O target duration mantém-se fixo; se a origem o ultrapassar, a publicação local termina no último segmento compatível, mas a captura existente continua a guardar o material.
- A finalização pode montar o ficheiro final enquanto o leitor usa os segmentos. Estes só são eliminados após libertação tanto da sessão de reprodução como das respostas HTTP em curso. A quota contabiliza o ficheiro final e o material temporariamente retido. A limpeza após terminar a reprodução corre fora da interface.
- Fechar o leitor, mudar de conteúdo, libertar o player ou enviar a app para segundo plano fecha a sessão local. Uma URL local fechada não é restaurada automaticamente; pode voltar a abrir-se a gravação pela biblioteca. A captura mantém o seu worker e as regras Android existentes.
- Ao eliminar a gravação ou retomar um download que ainda estava aberto no leitor, a app fecha primeiro a sua reprodução local e aguarda a libertação dos leitores. Outros leitores que ainda existam impedem a remoção/reutilização dos ficheiros. Uma captura pausada continua guardada depois de fechar o leitor.
- Depois de reiniciar o processo, material temporário deixado por leitores desaparecidos só é limpo quando existe um ficheiro final não vazio e finalização explicitamente confirmada. Capturas antigas de estado desconhecido, pausadas ou com finalização falhada são preservadas.
- Retirar o USB pode interromper a leitura; não é criada outra captura nem feito download para contornar a perda do destino. A reprodução em curso não é oferecida a leitores externos, TS bruto ou DASH. Ficheiros já finalizados mantêm a reprodução anterior.
- Sem nova migração de base de dados. Os registos privados dos segmentos passam a conservar também a descontinuidade; os antigos continuam legíveis.

Testes adicionados: publicação só após commit, crescimento EVENT desde o início, lacunas/descontinuidades, inicialização fMP4, retenção até fechar sessão e resposta, pausa sem destruição, servidor HTTP real em loopback com pedidos Range e rejeição de caminhos/segmentos inválidos, finalização com ENDLIST e target duration fixo.

Aceitação manual: gravar um canal HLS; abrir **Ver gravação em curso**; pausar e recuar; confirmar chegada de novos segmentos sem outra sessão no fornecedor; parar a gravação enquanto se vê e reproduzir até ao fim; sair para Home e voltar pela biblioteca; pausar/retomar download após leitura; eliminar enquanto está aberto; remover/religar USB; reiniciar durante leitura/finalização e confirmar preservação do ficheiro final e limpeza segura do temporário. Não foi medida a fluidez ou o comportamento dos motores na box.

Referência de protocolo: [RFC 8216](https://www.rfc-editor.org/rfc/rfc8216.html), secções 4.3.2.5, 4.3.3.5 e 6.2.1. A gestão de leitores e limpeza é específica da OwnTV.

## Fase 4B — gravações por partes em FAT32

Estado: implementada para capturas HLS em TS segmentado e fMP4, quando o destino é identificado como FAT32. Testes automatizados verificados; aceitação na box e em USB real pendente.

- A captura conserva os segmentos HLS completos existentes; a finalização em FAT32 escreve um pacote no mesmo volume escolhido, com partes de até **64 MiB**, agrupadas para cerca de **30 segundos**. Um segmento maior que esse tempo continua inteiro, respeitando o teto de bytes. Lacunas e descontinuidades iniciam outra parte. Não há transcodificação, alteração da qualidade nem corte arbitrário de MP4 progressivo.
- TS concatena segmentos completos; fMP4 conserva a inicialização separada e agrupa fragmentos completos. O índice privado valida nomes, formato, tamanhos e identidade, sem URLs, utilizadores ou tokens. O pacote aparece numa única linha da biblioteca, com o tamanho agregado do vídeo.
- O leitor interno recebe uma playlist HLS local VOD com `ENDLIST`, inicialização quando necessária e marcas de descontinuidade. Os pedidos ficam em loopback, protegidos por token; não voltam ao fornecedor. A passagem entre partes é feita pela playlist, sem pedir ao utilizador que abra o ficheiro seguinte. Seek e descodificação reais nos motores continuam sujeitos à aceitação na box.
- Cada cópia verifica o SHA-256 do segmento original. O índice só é publicado depois de fechar/sincronizar as partes; em caminhos de ficheiro usa promoção por rename atómico, em SAF fecha e relê o documento. Falha de escrita, falta de quota ou desconexão preserva os segmentos de origem e não anuncia finalização bem-sucedida.
- Após interrupção, a montagem pode reconstruir o pacote incompleto a partir da captura confirmada, sem duplicar segmentos. Um índice confirmado da mesma captura é reutilizado; identidade diferente é recusada. Se o processo morrer após promover o caminho na base de dados, o arranque valida o pacote e termina a linha como parcial, com causa da captura desconhecida, sem tentar gravar novamente sobre o índice. Não se promete recuperar material nunca confirmado.
- A quota contabiliza o vídeo agregado, o índice e o material de origem temporariamente retido por leitores; inclui também partes deixadas por uma montagem interrompida. A montagem reserva o espaço da cópia e a margem de segurança. Consultas de metadados de pacotes incompletos correm em IO na reconciliação de armazenamento; não há leitura de payloads ou varrimento de segmentos durante a composição da lista.
- Apagar aguarda a libertação do escritor/leitores e remove todas as partes, inicialização, índice e restos de montagem. Uma repetição tolera ficheiros já removidos. A limpeza verifica a pasta derivada do ID e recusa conteúdo alheio, subpastas ou links simbólicos. A abertura local e a eliminação são serializadas para evitar apagar enquanto se admite um leitor.
- Caminhos normais e pastas SAF locais são suportados. A divisão depende da deteção física existente (`vfat`/`msdos`); um sistema que esconda esse tipo atrás de FUSE pode não ser identificado. Não se assume que qualquer USB é FAT32. ExFAT e armazenamento sem esse limite mantêm a finalização anterior num ficheiro único.
- Leitores externos e mudança de localização de apenas o índice são recusados para pacotes; a app explica a necessidade do leitor interno. Para mover uma gravação por fora da app é necessário conservar a pasta completa, mas esta fase não implementa importação/exportação portátil do pacote. TS bruto e DASH mantêm a proteção anterior do limite de ficheiro; não se anuncia divisão compatível nesses formatos.
- Sem nova migração de base de dados, alteração de versão ou geração de APK. A reprodução de gravações em curso da fase 4A mantém-se.

Testes adicionados: teto individual com agregado simulado acima de 4 GiB; agrupamento sem cortar segmentos; lacunas/descontinuidades; índice com nomes inseguros/tamanhos inválidos; montagem TS e inicialização fMP4; reinício e reutilização; interrupção sem publicação; alteração de identidade e corrupção da origem; reprodução HTTP local de várias partes e Range; contagem/libertação de leitores; parte em falta; eliminação com índice ausente e repetição; conteúdo alheio e ID incorreto preservados. Os payloads de teste verificam transporte/integridade, não descodificação de vídeo real.

Aceitação manual: gravar HLS em FAT32 até ultrapassar 4 GiB; confirmar uma linha e partes abaixo do teto; reproduzir e fazer seek através de várias partes; repetir com fMP4 e pasta SAF na raiz/subpasta; interromper durante a montagem e reiniciar; desligar/religar USB; quota cheia; eliminar enquanto aberto; verificar libertação do espaço; confirmar que exFAT mantém o ficheiro único. Não foi possível executar estes ensaios na Xiaomi/Thomson ou numa pen real nesta sessão.

## Validação por entrega

- Fase 1: compilação concluída e **1 442 testes aprovados**: app 235, Core 896, player-core 311; zero falhas, erros ou testes ignorados.
- Inventário de textos e verificação de whitespace aprovados nos dois repositórios.
- Log da fase 1: `fase-comando-validation.log`, na raiz da app. Foram acrescentados oito testes para gestos, geometria e observações do diferido.
- Fase 2: **1 447 testes aprovados** (app 235, Core 901, player-core 311), sem falhas, erros ou testes ignorados. Log: `fase2-gravacoes-validation.log`. Inclui cinco testes adicionais de integridade e migração.
- Fase 3A: **1 460 testes aprovados** (app 245, Core 904, player-core 311), sem falhas, erros ou testes ignorados. Log: `fase3a-versoes-validation.log`. Treze testes adicionais cobrem prioridade, ocultação, agrupamento/paginação, zapping e persistência/restauro. Inventário de textos e whitespace aprovados nos dois repositórios.
- Fase 3B: **1 469 testes aprovados** (app 245, Core 913, player-core 311), sem falhas, erros ou testes ignorados. Log: `fase3b-diferidos-validation.log`. Nove testes adicionais de retoma e migração.
- Fase 4A: **1 476 testes aprovados** (app 245, Core 920, player-core 311), sem falhas, erros ou testes ignorados. Log: `fase4a-reproducao-validation.log`. Sete testes adicionais com publicação/ficheiros e HTTP local.
- Fase 4B: **1 490 testes aprovados** (app 245, Core 934, player-core 311), sem falhas, erros ou testes ignorados. Log: `fase4b-fat32-validation.log`. Catorze testes adicionais de planeamento, montagem, recuperação, integridade, HTTP local e eliminação. Compilação concluída; validação de USB/FAT32 e reprodução real pendente.
- Não gerar APK nem alterar versão/publicar. O utilizador gera o APK no Android Studio.
- Testes automatizados não substituem testes na Xiaomi/Thomson, com o comando real e o fornecedor IPTV.
