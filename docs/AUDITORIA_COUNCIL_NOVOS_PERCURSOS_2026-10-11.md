# OwnTV TV — nova auditoria após as quatro fases

Data: 11/10/2026. Estado: análise; recomendações abaixo não implementadas.

Continuação: as quatro fases foram implementadas no trabalho seguinte; ver [IMPLEMENTACAO_NOVOS_PERCURSOS_2026-10-11.md](IMPLEMENTACAO_NOVOS_PERCURSOS_2026-10-11.md) para alterações e validação. Este relatório conserva a evidência anterior à implementação e distingue propostas opcionais.

## Âmbito e método

Inspeção do código atual de `C:\Users\renat\Downloads\OwnTV-main` e `C:\Users\renat\Downloads\OwnTV_Core`, por três especialistas (definições, reprodução e interface), revisão independente do council e verificação do coordenador. As correções de `IMPLEMENTACAO_FASES_AUDITORIA_2026-10-11.md` foram excluídas dos novos problemas. OwnTV Mobile não foi alterado.

Foi consultada documentação oficial Android/Media3 para confrontar as propostas. Não foram executados novos builds, testes, medições de fluidez ou ensaios físicos nesta auditoria. “Confirmado no código” identifica um percurso ou perda de estado verificável; não significa reprodução do sintoma na box. Foi criado apenas este relatório, sem alteração de fontes, preferências, bases de dados ou dependências.

P1: correção prioritária de ciclo de vida/propriedade. P2: incoerência funcional. P3: otimização/limpeza limitada. Não foi demonstrado novo P0 nem uma nova causa para restrições de reabertura Live do fornecedor.

## 1. Reprodução, diferidos e gravações

### R1 — abertura de gravação pode concluir após Home — P1

Evidência App: `RecordingsScreen.kt:98` cancela a abertura em `onDispose`, sem observador ON_STOP; `RecordingsViewModel.kt:125–175` abre partes/gravações em curso de forma assíncrona e valida seleção/perfil antes de reproduzir. `MainActivity.kt:149–160` para o leitor ao sair, mas não invalida essa abertura. Os ficheiros referidos pertencem a `app/src/main/java/tv/own/owntv/`.

Cenário: selecionar gravação em armazenamento lento, carregar Home e deixar a abertura concluir. A composição pode continuar existente e a seleção continua válida; a tarefa pode iniciar reprodução depois da paragem da Activity.

Solução: invalidar/cancelar a abertura no background, com identidade de foreground/seleção verificada imediatamente antes de transferir o recurso ao leitor. Conservar a limpeza existente em cancelamento e fechar os recursos adquiridos exatamente uma vez. Não usar um atraso fixo.

Aceitação: suspender abertura de partes e gravação em curso, enviar ON_STOP e concluir a abertura. Nenhum play nem alteração tardia do shell; recursos fechados; nova abertura em foreground funciona.

### R2 — gravações por partes perdem retoma após Home — P2

Evidência Core: `player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt:3955–3967` guarda BackgroundRestore e chama stop; `:3759` liberta a gravação; `:2627–2630` fecha a publicação local e apaga backgroundRestore. Em `:3972`, o regresso já não tem estado para restaurar.

Solução: guardar identidade da gravação, perfil, posição e intenção de pausa/reprodução, sem manter a ligação ou descritor aberto em background. Ao regressar, reabrir através de RecordingManager e criar uma nova publicação válida. Guardar apenas o URL localhost antigo não resolve: a publicação anterior foi fechada.

Aceitação: partes e gravação em curso libertam ligação/lease ao sair e recuperam posição ao regressar. Gravação apagada ou perfil alterado suprimem a retoma. Considerar a duração atualmente disponível em gravação crescente.

### R3 — retoma do diferido perde opções de abertura — P2

Evidência App: `features/live/LiveViewModel.kt:2372` e `:2562–2572` passam manifest e alternativas específicas do programa. Core: `OwnTVPlayer.kt:549–558` guarda apenas parte da configuração; `:3976–3996` reabre sem o manifest, alternativas tipadas e escolha explícita do motor. DRM também não é preservado, mas é secundário para este uso TV.

Solução: descritor imutável completo da reprodução, conservando a identidade do programa, tipo de manifest, alternativas e respetivos tipos, motor, headers/UA, posição e intenção. Restaurar esse descritor, sem reconstruir o pedido com argumentos incompletos.

Aceitação: diferido HLS sem extensão com duas alternativas tipadas; Home/retorno conserva a configuração; falhar a primeira alternativa ainda permite a segunda do mesmo programa. Deteção automática pode compensar alguns casos, pelo que esta perda não prova falha em todos os diferidos.

## 2. Definições e resultados assíncronos

### S1 — formulários podem ficar inicializados com valores vazios — P2

Evidência App: `features/settings/NetworkSettingsScreen.kt:65–73` inicializa uma vez e marca seeded, enquanto `SettingsViewModel.kt:1417–1418` fornece inicialmente ProxyConfig vazio através de stateIn. `MetadataSettingsScreen.kt:154–171` faz o mesmo com chaves/URL, cujos fluxos começam vazios em `SettingsViewModel.kt:1595–1600`.

Cenário condicionado pelo tempo de carregamento: na primeira abertura, o formulário recebe o valor inicial e ignora depois os valores persistidos. Pode mostrar campos vazios apesar de haver configuração; Guardar pode substituí-la.

Solução: distinguir explicitamente “a carregar” de “configuração vazia carregada”. Inicializar o draft apenas após a leitura persistida, respeitando alterações deliberadas do utilizador; impedir Guardar enquanto não houver estado inicial válido. Não escolher tempos de espera arbitrários.

Aceitação: leitura persistida atrasada com proxy/chave já configurados; formulário apresenta os valores corretos e não os apaga. Alterações locais não são substituídas por emissões posteriores irrelevantes.

### S2 — teste de proxy/DNS antigo pode publicar resultado desatualizado — P2

Evidência App: `SettingsViewModel.kt:1443–1491` e `:1520–1564`. Reset muda o estado visual, mas não mantém/cancela um job nem invalida uma geração. Um teste bloqueante iniciado antes de Guardar/reset pode publicar depois, incluindo após início de outro teste.

Solução: identidade de teste + job cancelável, cancelamento da chamada quando possível e validação da identidade antes de publicar. O resultado deve corresponder aos parâmetros testados; cancelamento sozinho não garante que uma operação bloqueante já terminada não publique.

Aceitação: iniciar A, Guardar/reset, iniciar B, concluir B e depois A; apenas B permanece. Não apresentar resultado de uma configuração anterior como validação da atual.

### S3 — leitura antiga de preferências pode desfazer ajuste manual — P2

Evidência Core: `OwnTVPlayer.kt:3639–3650` verifica apenas loadGeneration ao aplicar atraso/zoom guardados. Um ajuste recente do HUD em `:3571` ou mudança de memorizar em `:3670` mantém a mesma geração. A implementação Live Exo já verifica audioPreferenceRevision em `LivePreviewEngine.kt:2897–2904`.

Solução: revisão das escolhas de áudio/zoom, além da geração de reprodução. A leitura pode restaurar o valor apenas se não houve escolha mais recente do utilizador. Proteger áudio e zoom separadamente quando necessário.

Aceitação: atrasar a leitura, alterar o atraso/estado memorizar ou zoom no HUD, concluir a leitura; a última escolha mantém-se. Sem intervenção manual, os valores guardados continuam a restaurar.

### S4 — AFR muda nas definições mas não no Exo ativo — P2

Evidência Core: `OwnTVPlayer.kt:1239` atualiza apenas a variável envolvente; `:1984` copia-a para Exo no arranque. `ExoSubtitleEngine.kt:359–362` aplica a estratégia no arranque; `:608` usa o mesmo valor em rebuild/retry; `:741` é um campo simples. O controlo de display da App atualiza-se separadamente.

Solução recomendada: setter que atualiza o campo e a estratégia Media3 do leitor atual na thread correta, sem reabrir a transmissão. Caso a aplicação só na próxima abertura seja intencional, o texto tem de o dizer; o council prefere aplicação imediata desta opção suportada por setter.

Aceitação: AFR ON→OFF→ON num diferido/gravação Exo atualiza ONLY_IF_SEAMLESS→OFF→ONLY_IF_SEAMLESS sem novo load. OFF mantém-se num retry/rebuild. Frequência/HDMI reais precisam de ensaio na TV.

### S5 — limpar valores guardados: esclarecer o âmbito — P2 de contrato

`SettingsViewModel.kt:499/507` limpa linhas da base de dados; os leitores ativos mantêm os seus valores/flags. A confirmação `res/values/live_catalog.xml:30` diz que os atrasos regressam ao valor configurado, sem explicar quando. Uma nova alteração do HUD com flag memorizar ainda ativa pode voltar a persistir o atraso.

Antes da implementação, escolher contrato explícito: limpar para a próxima abertura (texto claro), ou também repor imediatamente o item ativo respeitando a prioridade da configuração por canal. O council não recomenda alterar som/zoom ativos silenciosamente. Não confundir este âmbito com S3, que é uma escolha recente sobrescrita por uma leitura antiga.

## 3. Guia, fluidez e limpeza

### U1 — guia ignora ordem Personalizada de TV DIRETO — P2

`features/epg/EpgViewModel.kt:973–990` trata CUSTOM como ordem do fornecedor. `features/live/LiveViewModel.kt:2663–2692` usa content_order/ALL_CONTEXT.

Solução: quando o guia segue TV DIRETO, usar os ranks da ordem Todos do perfil ativo, com fallback determinístico para canais sem posição. Não confundir ordens específicas de categorias com a ordem global.

Aceitação: guardar C,A,B; guia segue C,A,B; filtros/diferidos/pesquisa conservam a ordem relativa dos canais restantes. Outros modos de ordenação mantêm-se independentes.

### U2 — janela de desvio EPG não participa no restauro de foco — P2

`features/epg/EpgScreen.kt:388` calcula anyDialogOpen sem offsetChannel. Em `:633`, fechar matchChooser e abrir offsetChannel parece ao efeito uma saída de todos os diálogos, apesar de a janela continuar aberta em `:676–682`. A tentativa de restaurar/limpar o proprietário acontece cedo.

Solução: incluir offsetChannel na propriedade do grupo de diálogos. Restaurar a linha de origem apenas após fechar a última janela, com as guardas de entrada existentes.

Aceitação física: linha N → menu EPG → desvio → OK/Voltar; foco regressa a N, sem pedido de foco na grelha por baixo da janela.

### O1 — cópia excessiva para pré-carregar três canais — P3

`features/epg/EpgViewModel.kt:438` usa channels.drop(from + 1).take(3), materializando primeiro toda a lista restante. O guia aceita até 20 000 canais em `:1123`.

Solução: selecionar diretamente o intervalo limitado de zero a três canais; conservar sequência, cancelamento, deduplicação e gerações da cache. Aceitação: mesmos IDs no início/meio/fim sem cópia do sufixo inteiro. Redução de trabalho é visível no código; ganhos de fluidez não foram medidos.

Limpeza segura identificada: local pixelsPerMinute sem uso em `EpgScreen.kt:882`; `providerName?.takeUnless { false }?.let` em `:960` pode ser providerName?.let. Rever comentários de PlaybackPrefsStore/OwnTVPlayer que ainda descrevem volume por item, distinguindo compatibilidade histórica das APIs da utilização TV atual a 100%. Não eliminar colunas/APIs legadas sem auditar backups e consumidores do Core.

## 4. Decisão do council e fases

| Fase | Implementação aprovada | Critério principal |
|---|---|---|
| 1 | R1, R2 e R3: ciclo de vida das gravações e descritor de retoma completo | Nunca iniciar após Home; fechar recursos; retomar gravações/diferidos com identidade e opções preservadas |
| 2 | S1–S4; explicitar contrato S5 | Valores guardados aparecem; resultados/leituras antigos não vencem escolhas novas; AFR chega ao Exo ativo sem reconnect |
| 3 | U1 e U2 | Ordem consistente e foco após a última janela; ensaio com comando |
| 4 | O1 e limpeza pequena; documentação | Trabalho limitado aos três canais; compilação/lint e resultados equivalentes |

Após mudanças no Core, regenerar o snapshot de fontes e validar local/snapshot segundo `vendor/README.md`. Não modificar o cache extraído nem o Core privado Mobile.

Inovações com utilidade concreta: retoma segura de gravações por partes, guia coerente com a ordem escolhida e indicação opcional compacta do valor efetivo/origem de uma definição (canal/lista/global) nos detalhes de reprodução. A última é proposta de apresentação; confirmar primeiro o que já existe para não duplicar a interface. Não criar outra fonte de preferências nem mais chamadas de rede.

## 5. O que não avançar com esta evidência

- Buffers maiores, atrasos universais ou outra escada de reconnect como resposta aos problemas acima.
- Cortar segmentos HLS ao atingir um limite de segundos ou generalizar flags/descodificadores para todos os canais.
- Alegar que erros de retoma encontrados explicam os bloqueios Live do fornecedor ou microcortes de áudio.
- Alegar fugas/OOM/ganhos quantitativos sem medição.
- Remover Multiview, diferidos, gravações, APIs/colunas de backup ou ficheiros de filmes/séries apenas pelo nome.
- Reintroduzir volume interno, favoritos/histórico ou funcionalidades smartphone.

## Referências oficiais

- [ExoPlayer: estratégia de frequência de vídeo](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer#setVideoChangeFrameRateStrategy(int)): suporte para atualizar a estratégia; resultado no dispositivo depende do percurso de render/display.
- [Media3: seleção de faixas](https://developer.android.com/media/media3/exoplayer/track-selection): alterar parâmetros do leitor é diferente de guardar uma preferência.
- [Android: boas práticas de corrotinas](https://developer.android.com/kotlin/coroutines/coroutines-best-practices): cancelamento cooperativo e regras de duração/propriedade das operações.
- [Kotlin: cancelamento de corrotinas](https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/topics/coroutines-cancellation.md): cancelamento e operações bloqueantes precisam de tratamento adequado.
- [Compose: fases e desempenho](https://developer.android.com/develop/ui/compose/performance/phases): reduzir trabalho desnecessário não prova por si um ganho de fluidez medido.

Estas referências apoiam as técnicas propostas, não confirmam os bugs específicos da OwnTV. A evidência desses bugs é o percurso do código indicado acima.
