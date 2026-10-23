# OwnTV TV — fase 4: reprodução, diferidos e mudança de canal

**Data:** 22/10/2026. **Âmbito autorizado:** PLY-01/02/03/04/05/06/08/09, UI-09 e OPT-04 do plano principal. As etapas A–D das novas publicações mantêm o seu estado anterior; não são a numeração desta fase.

**Estado:** implementada e validada automaticamente no Core e na App. Aceitação física na box pendente. Não foi gerado APK.

## Alterações implementadas

### PLY-01 — libertação antes de abrir diferidos

- A abertura interna de um programa e o recuo pelo arquivo passam pela confirmação de libertação dos dois leitores locais. A abertura externa reutiliza a mesma proteção.
- As duas operações de paragem são iniciadas antes de a corrotina ceder a execução, para uma limpeza antiga não vir parar um leitor mais recente. O cancelamento do pedido não abandona essa limpeza.
- A origem, a escolha e a geração continuam a ser verificadas antes/depois da espera. Uma escolha ultrapassada não pode abrir um programa antigo por cima de outro.
- Se a libertação falhar, apresenta-se o aviso existente; o programa não é substituído silenciosamente pelo direto. Uma Pausa pedida durante a transição é aplicada ao diferido interno.
- A confirmação é **local**. Não prova que o fornecedor terminou a sessão remota; os intervalos configurados de reabertura continuam necessários.

### PLY-02 — distinguir erros e aprender apenas com sucesso

- Classificação explícita de HTTP, rede, timeout, handover, cancelamento, formato, codec e causa desconhecida. Um 403/404, um erro de decoder ou uma falha desconhecida não passam a significar «HLS incompatível».
- Uma preferência temporária por TS só é aprendida após uma falha de formato HLS seguida de reprodução TS confirmada no **mesmo motor**. Uma tentativa TS falhada ou o sucesso de outro motor não confirmam essa conclusão.
- Essas aprendizagens ficam limitadas a 512 entradas por mapa, com validade de 30 minutos, por URL/motor e apenas no processo atual. A revisão da fonte não foi acrescentada como nova identidade nesta fase.
- HLS obrigatório e exceções manuais por canal mantêm-se. Esta mudança não acrescenta alternativas TS aos canais configurados para HLS estrito.

### PLY-03 e OPT-04 — Play/Pausa e contratos coerentes

- Os botões do leitor, minileitor e barra de áudio passam a representar a intenção de reprodução. Buffering não é tratado visualmente como uma Pausa do utilizador.
- O contrato dos motores exige a intenção, a sua revisão e a operação explícita de a alterar; deixa de aceitar uma implementação implícita baseada apenas em «está a tocar».
- O watchdog da grelha respeita uma Pausa deliberada. Comentários e contratos antigos foram ajustados, sem alterar os processadores de áudio/vídeo nem o ganho interno.

### PLY-04 — dono da sessão e do foco de áudio

- A ligação à MediaSession tem um proprietário por identidade. A limpeza de um proprietário antigo não pode desligar a sessão de um novo.
- A sessão acompanha o leitor principal ou o quadrado audível do multiview, conforme o contexto.
- Ao sair de primeiro plano, liberta os recursos da sessão/foco, conservando apenas o direito do proprietário atual de se voltar a associar. Um leitor já eliminado não é reassociado.
- Foram conservadas as proteções de libertação do multiview que já existiam. Retirado o parâmetro obsoleto de política de foco cujas duas opções tinham o mesmo comportamento.
- **Volume interno continua a 100%.** Não foi introduzido ducking, boost, normalização ou novo processamento de som.

### PLY-05 — imagem congelada segura

- Uma imagem já publicada para apresentação deixa de ser reciclada manualmente enquanto o Compose ainda a pode estar a desenhar.
- Mantêm-se o limite de resolução da captura e a reciclagem das capturas privadas que falharam ou ficaram ultrapassadas antes de serem publicadas.
- A memória gráfica e a repetição de transições ainda precisam de observação na box; não foi medido um ganho de RAM.

### PLY-06 — resultado de abertura pertencente à escolha certa

- Cada tentativa regista o seu resultado antes de iniciar a sintonia, identificado por escolha, origem e canal. Respostas antigas não confirmam nem fazem falhar o canal novo.
- Corrigida a criação do observador de erros: é filho da tentativa e não uma nova operação do controlador que possa cancelar a sua própria sintonia.
- Sucesso/falha acordam a espera diretamente. Cancelar ou mudar de perfil/canal não é convertido numa falha que desencadeie outra versão.
- Uma Pausa deliberada suspende o prazo de abertura e a espera acorda com Play, resultado terminal ou cancelamento. A retoma deixa de descontar o tempo da pausa no prazo ativo.
- Uma paragem interna durante a preparação de outro canal é distinguida de Pausa. A avaliação de metadados/prazos durante uma tentativa ativa continua limitada a intervalos de 50 ms; não se afirma que todos os temporizadores foram removidos.
- Mantêm-se o teto de espera ativa, a espera configurável de reabertura e as restrições de backoff do fornecedor.

### PLY-08 e UI-09 — gravações e continuidade com falhas controladas

- Contrato comum para operação concluída, ultrapassada por outro contexto ou falhada com o motivo existente. Cancelamento continua a propagar-se.
- Erros operacionais de armazenamento/rede têm mensagens existentes e localizadas, sem expor a exceção, URL ou credenciais ao utilizador. A causa original é conservada internamente.
- Ações de gravação capturam o perfil antes de esperar pelo bloqueio e voltam a verificar perfil/origem; mensagens tardias não são apresentadas noutro perfil.
- Continuar após um programa trata falhas na consulta do EPG sem fechar a App nem escolher outro programa/direto silenciosamente.
- Retomar um download e apagar um ficheiro em reprodução aguardam a libertação local necessária. Se o leitor não confirmar a libertação para apagar, o ficheiro é preservado e a operação é recusada.
- Removidos fluxos de avisos sem produtores que foram substituídos pelo contrato comum. Sem novas tabelas, enums persistidos ou migrações.

### PLY-09 — canal selecionado e foco na grelha

- O aviso de mudança de canal usa a escolha atual, sem depender dos metadados atrasados do leitor anterior. Resultados da sintonia direta e motivos de alternativa são confrontados com canal/origem.
- Os destinos de foco da grelha ficam estáveis por sessão/quadrado. Adicionar, preencher ou reduzir a grelha pede foco ao quadrado correspondente com tentativas limitadas.
- Uma nova ação do comando prevalece sobre um restauro de foco pendente. Mudar foco não reinicia o decoder.

## Evidência e limites da validação

- Core: **1 355 testes JVM**, sem falhas/erros/ignorados.
- Motores: **610 testes JVM**, sem falhas/erros/ignorados.
- App: **441 testes JVM**, sem falhas/erros/ignorados. Compilação standard debug/release e lint vital de release aprovados.
- Total: **2 406 testes JVM**, incluindo **30 novos** nesta fase (6 Core, 12 motores e 12 App). Estes testes não equivalem a 30 bugs ou a ensaios de compatibilidade em dispositivos reais.
- Seis testes da ferramenta de snapshot aprovados. Testes Android do Core compilados; não executados num dispositivo.
- Inventário i18n: Core 263 entradas de baseline + 5 826 literais técnicos; App 51 + 1 088. Os dois baselines são idênticos aos da cópia anterior.
- Log final Core: `%TEMP%/owntv-phase4-core-final-verified.log`, sucesso. A execução imediatamente anterior completa também aprovou os 1 355 testes Core; as tarefas inalteradas foram reutilizadas na última execução.
- Log final App: `%TEMP%/owntv-phase4-app-complete.log`, sucesso em 4 min 49 s.
- Configuração normal, sem forçar a seleção do snapshot na linha de comando: `%TEMP%/owntv-phase4-normal-config.log`, sucesso em 12 s. Continua a resolver o Core personalizado fixado.
- Durante o desenvolvimento houve falhas de teste relacionadas com fontes alterados durante a compilação, uma expectativa antiga de aprendizagem antes do sucesso e a contagem de pausa entretanto corrigida. Não se usam essas execuções como validação final.
- Um teste de rede existente do timeshift local falhou numa execução em simultâneo com a compilação da App (três pedidos em vez de dois). O código e o teste estavam iguais à cópia anterior; a suite completa sequencial posterior passou. Isto não demonstra a causa da ocorrência nem justifica relaxar o teste.
- Não foram medidos ganhos percentuais de velocidade, memória, qualidade ou robustez. Os dez IDs são blocos do plano, não dez bugs reproduzidos; UI-09/PLY-08 constituem um contrato relacionado.
- O aviso geral existente sobre funcionalidades Gradle obsoletas continua visível. Não se desativou lint nem se criou um baseline para contornar falhas de release.

## Integridade, recuperação e preservação

- Cópia anterior verificada: `C:/Users/renat/Downloads/OwnTV-fase4-backup-20261022/`, com App/Core, snapshot anterior e manifesto SHA-256.
- Snapshot anterior: `eb335c31686c9e9ad116c2adf91dab4d721cc5e5c117ea5af8b93334118099d9`, 1 052 inputs.
- Snapshot desta fase: **1 058 inputs**, SHA-256 **`12015db51124dafae2de1cff483eb7147d1a15cc780858b7b254c1fa2da2d942`**. Hash, inventário e bytes conferidos integralmente com o Core externo.
- Snapshots anteriores conservados. Não se editou a cache de extração como origem de desenvolvimento.
- Preservados: esquema 51, backups/dados legados, configurações por canal, motores escolhidos, HLS/exceções, buffers/latência, espera de reabertura, setas em fullscreen, gravações, correção de diferidos +1 h independente da hora apresentada e volume interno de 100%.
- OwnTV Mobile não foi alterado. Sem APK, assinatura, commit, push ou publicação.

## Aceitação física pendente

1. Direto → programa diferido → direto; escolher rapidamente dois programas e Voltar enquanto o leitor anterior fecha. Confirmar que um 404 não abre o direto como substituição.
2. A → B → A, com o intervalo de reabertura escolhido e HLS obrigatório; distinguir recusa do fornecedor da libertação local.
3. Pausa durante carregamento/transição, esperar além do prazo de abertura e premir Play. Repetir com cancelamento e mudança de perfil.
4. Multiview: escolher quadrado audível, adicionar/remover quadrados, Assistente, Home e regresso; verificar áudio e foco do comando.
5. Gravações: recusa, falta de espaço, USB desligado, parar/retomar e apagar um ficheiro que está a tocar; confirmar preservação quando o fecho falha.
6. Repetir transições entre leitores e observar memória gráfica/heap para validar a vida das imagens congeladas.

## Continuação

**Fase 5 não iniciada.** A próxima fase do plano é comando e definições: UI-01/02/03/04/07/08/10 e PLY-07. Os restantes itens condicionais conservam os critérios do plano principal.
