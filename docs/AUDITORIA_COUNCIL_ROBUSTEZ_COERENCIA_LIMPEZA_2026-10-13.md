# OwnTV TV — auditoria especializada e decisão do council

Data: 13 de outubro de 2026.
Estado: análise para implementação futura. Nenhuma alteração de código, preferências, base de dados ou dependências foi aplicada nesta auditoria. Foi criado apenas este relatório e executado um teste existente dirigido.

## Âmbito e método

Checkouts: `C:\Users\renat\Downloads\OwnTV-main` e `C:\Users\renat\Downloads\OwnTV_Core`. OwnTV Mobile não foi analisado nem alterado. Não foram encontrados AGENTS.md nesses checkouts.

Três especialistas analisaram interface/EPG, reprodução/rede e configurações/backup/limpeza. Foram feitas duas rondas, revisão cruzada das conclusões e confirmação pelo coordenador dos principais percursos. Inclui revisão crítica das implementações UI de 13/10; compilação bem-sucedida não elimina erros de estados, concorrência ou cancelamento.

P1 significa risco funcional importante condicionado ao percurso descrito; P2 identifica falha de coerência/robustez; P3 é melhoria de nomenclatura/manutenção. Nenhum novo P0 foi demonstrado. Confirmado no código não significa reproduzido na box.

## Decisão resumida

1. Corrigir admissão de sessões/gravações e tornar a reserva multiview atómica.
2. Completar o backup das configurações TV e explicitar a semântica de restauro.
3. Corrigir o Referer na sincronização protegida e definir separadamente o seu âmbito na reprodução.
4. Corrigir rascunhos, guardar/cancelar, estados de erro e diagnóstico introduzidos ou expostos nas últimas melhorias.
5. Ajustar lifecycle do EPG e opções que não correspondem ao motor/efeito.
6. Só depois remover dependências e código morto com validação por módulo.

## 1. Sessões, reprodução e rede

### R1 — gravação pode exceder as sessões disponíveis — P1 confirmado

Core `core/src/main/java/tv/own/owntv/core/live/ConnectionBudget.kt:89`: o ramo RECORDING compara `open.recording` com o limite de gravações, sem limitar `open.total`.

Cenário preciso: fonte permite duas sessões; dois tiles multiview estão ativos; zero gravações; inicia gravação agendada. A regra autoriza um terceiro consumidor.

Percurso confirmado: App `features/live/LiveViewModel.kt:413` e `features/epg/EpgViewModel.kt:180` verificam/agendam; Core `recording/RecordingManager.kt:133` usa a mesma regra; `RecordingEngine.kt:191` reserva e inicia a captura. Não há preempção dos tiles neste percurso. `OwnTVShell.kt` liberta o multiview ao sair/background, não por uma nova gravação.

O teste `core/src/test/java/tv/own/owntv/core/live/ConnectionBudgetTest.kt:66` permite gravar numa conta de uma sessão com imagem ativa e pressupõe no comentário que a imagem para. Essa paragem não faz parte do teste.

Correção mínima: impedir admissão quando o total já ocupa o limite conhecido. Se a gravação tiver prioridade, essa prioridade exige parar explicitamente uma visualização interrompível e confirmar encerramento antes de admitir a captura. A recusa segura é menor; preempção é uma decisão adicional de produto.

Aceitação: matriz de limites 1/2/3, gravações/tiles, reserva ligada/desligada. Nenhuma captura começa enquanto não houver capacidade. Não confundir sessões IPTV com número de pedidos HTTP dos segmentos HLS. Não afirmar que isto explica A→B→A.

### R2 — admissão multiview separa verificar e reservar — P2 confirmado

App `features/multiview/MultiviewState.kt:149–160`: chama connectionBudget e depois registry.claim. Uma gravação pode reservar entre os dois passos. Core `core/live/OpenStreamRegistry.kt:49` já tem tryClaim atómico.

Correção: admissão atómica também para tiles, com resultado que conserva a informação de recusa. Substituir o canal de um tile precisa de identidade de substituição e de libertação do transporte, além da reserva lógica.

Aceitação: duas operações suspensas numa barreira tentam ocupar a última vaga; apenas uma é admitida.

### R3 — vaga pode ser disponibilizada antes do drain — P2, hipótese a comprovar

App `MultiviewState.kt:201–202` liberta claim antes de pool.release; releaseAll tem a mesma ordem. Core LivePreviewEngine.release inicia retireAsync. O contrato do pool não devolve confirmação do encerramento HTTP.

Isto demonstra uma lacuna de contrato, não uma sessão remota comprovadamente aberta. Primeiro reproduzir com transporte bloqueado. Se confirmado, introduzir estado a encerrar, encerramento assíncrono com limite e disponibilidade da vaga após confirmação local. Não adicionar atrasos fixos a todos os canais.

### R4 — Referer usado no teste não acompanha o sync — P1 para listas que o exigem

Core SourceTester.kt:81,116 e parser/XtreamClient.kt:513 enviam source.httpReferer. sync/M3uSyncer.kt:474 chama http.get sem headers. network/HttpClient.kt:31 usa mapa vazio por defeito. Pesquisa global não encontrou interceptor que derive esse valor da fonte.

Consequência: uma lista protegida pode passar o teste/importação e falhar na atualização.

Correção imediata: usar o Referer da fonte no descarregamento dessa lista. Para streams, LiveTuneController.kt:903 só passa channel.httpHeaders; é necessário decidir se o campo significa Referer da lista ou da lista e canais. Não assumir que um header de importação deve viajar para todos os hosts. Stalker já tem Referer próprio.

Se houver herança para reprodução, usar política central, override por canal sem sensibilidade à capitalização e regras explícitas para redireções, segmentos/chaves e loopback local.

Aceitação: servidor de teste exige Referer no probe e sync; headers por item prevalecem; dois fornecedores não partilham identidade. Isto não prova a causa dos 403 do utilizador.

### R5 — captura HTTP do diagnóstico é incompleta — P2 confirmado

App LiveViewModel.kt:2049 captura errorInfo após tuneChannel e escolhe o motor apenas uma vez. Core LivePreviewEngine.kt:1594–1598 pode tratar 403 através de retry de User-Agent antes de publicar errorInfo; backoff limpa esse estado. LiveTuneController.abandon substitui o erro visível por detalhe terminal.

Resultado: o registo pode conservar uma falha sem o código HTTP inicial, mesmo quando os testes puros de armazenamento passam. Troca de motor também pode deixar o observador no motor anterior.

Correção: evento estruturado sanitizado emitido na origem antes da recuperação, associado à geração da sintonia, motor e etapa. Usar analytics/diagnóstico existente, não outro polling permanente. Registar no histórico de falhas apenas o resultado pertinente da tentativa; não apresentar um erro transitório recuperado como falha de um canal que está saudável.

Aceitação: 403→retry→prazo terminal, fallback Exo→mpv, cancelamento A→B; erro antigo nunca é atribuído a B. Captura não depende de conseguir ler o HUD a tempo.

## 2. Configurações, backup e restauro

### S1 — oito escolhas TV não entram no backup — P1 confirmado

Core SettingsRepository.kt declara e utiliza, mas omite das listas exportáveis em 2766–2847:

- MULTIVIEW_ENABLED, MULTIVIEW_TILES, MULTIVIEW_WARNING_ACCEPTED;
- RECORDING_RESERVE_CONNECTION;
- RECORDING_PRE_ROLL_MINUTES, RECORDING_POST_ROLL_MINUTES;
- RECORD_WHAT_IM_WATCHING;
- RECORDING_OVER_MOBILE_DATA, cuja portabilidade deve ser classificada mesmo estando fora da UI TV.

Os valores personalizados das margens/reserva/gravação e multiview não têm serialização alternativa encontrada. São distintos dos campos de reprodução por canal que já são guardados.

Correção: acrescentar chaves tipadas, validação e testes round-trip. Decidir se aceitação de avisos dependentes do dispositivo deve viajar; não transportar uma confirmação de capacidade da box como garantia para outra box.

Aceitação: exportar valores não predefinidos e importar sobre destino com valores opostos. Confirmar reserva de sessões e margens, além de flags visuais.

### S2 — backup completo e merge têm semânticas diferentes — P2 confirmado

SettingsRepository.kt:2868–2884 exporta a maioria das chaves só se persistidas. ImportSettings:2914–2928 altera apenas campos presentes. BackupManager.kt:1118 não reseta o destino.

Origem nunca alterou hardware, logo usa true implícito; destino gravou false. A origem não exporta essa chave, e o restauro mantém false. Isto é merge compatível por desenho, mas não reproduz a configuração efetiva de origem esperada num clone completo.

Correção: backups novos materializam os valores efetivos portáteis e identificam semântica/versão. Manter compatibilidade dos antigos: ausência não pode passar a apagar opções do destino. Não limpar permissões SAF, IDs de dispositivo, sessões ou secções não selecionadas. SimpleModeBackupCodec já materializa os próprios defaults e deve servir de referência.

Aceitação: default implícito→destino diferente, explícito→default, ficheiro antigo, secções parciais. Não confundir isto com S1, que perde até escolhas explicitamente persistidas.

### S3 — Pulso pode ser alterado mas nunca produz efeito — P2 confirmado

App SettingsScreen.kt:1511,1513,2890–2893 oferece Pulso. OwnTVShell.kt:1473 só permite se a animação estiver ativa. Core SettingsRepository.kt:782–783 fixa o nível pedido em OFF.

Decisão do council: remover o controlo Pulso e a sua entrada de pesquisa; preservar a luminosidade estática e a chave legada de importação. Respeitar o pedido de remover animações, sem reativar esse efeito.

### S4 — Guardar significa coisas diferentes — P2 confirmado

NumberInputDialog.kt:92–96 grava imediatamente nos passos +/−. Voltar apenas fecha. O botão Guardar fecha após confirmar texto. Já a prioridade de versões tem rascunho real.

Correção: distinguir ajuste imediato (Concluído e indicação adequada) de edição transacional (Guardar/Cancelar). Se adicionar modo transacional ao componente, rever todos os chamadores; não mudar callbacks genericamente.

### S5 — limites e âmbito dos campos divergem — P2/P3

Pré-buffer geral/lista usa PREROLL_CHOICES=0 e 2..10; editor por canal usa 0..10, incluindo 1. Backend/backup global e por lista aceitam até 30; normalização por canal clampa a 10. Não é uma UI geral que prometa 30 s; é incoerência de domínio/legado.

Correção: constante de domínio, política para valores legados não listados e apresentação correta desses valores. Não aumentar buffers para supostamente corrigir streaming.

Pré-buffer 0 mantém limiar automático no Exo (1,5 s), apesar da constante legada PREROLL_OFF. Não é bug de execução. Uniformizar nomenclatura como sem pré-buffer adicional/limiar do motor, preservando diferenças mpv/Exo.

Desentrelaçamento é apresentado genericamente, mas o caminho Exo não aplica PlaybackSettings.deinterlace. Identificar âmbito/capacidade do motor. HDR já está identificado como mpv; não repetir correção aplicada.

SourceEntity.liveTuneTimeoutSecs existe e viaja em backup, mas CoreHost.globalBudgetSecs lê só global. Não há editor atual por fonte para esse campo: classificar como legado/potencial funcionalidade, não como toggle visível avariado.

### S6 — operações locais de backup podem publicar resultados tardios — P2, percurso condicionado

BackupViewModel.kt:138–217 lança exportar/inspecionar/desbloquear/importar como jobs independentes. Reset apenas muda UI e termina operação remota. Botões Working reduzem a exposição, mas não constituem identidade da operação local.

Correção: geração/owner para publicar resultados e serialização explícita do restauro. Fechar o ecrã não deve cancelar uma importação a meio de escrita: preservar consistência e informar conclusão. Testar conclusão antiga após reset/novo pedido antes de alegar corrupção.

## 3. Interface, rascunhos e EPG

### U1 — Voltar pode fechar enquanto guarda — P2 confirmado

ChannelPlaybackSettingsDialog.kt:268 permite dismiss pelo BackHandler; popup usa onDismiss sem guarda. Os botões estão desativados, mas a coroutine pertence à composição. Fechar pode cancelar a operação ou retirar a confirmação de uma escrita já concluída.

Correção: política única de saída e snapshot do rascunho no clique. Não usar NonCancellable geral para resolver o problema. Editor em lote também precisa de finally e propagação de CancellationException; runCatching em chamadas suspend não deve converter cancelamento em falha normal.

### U2 — falha de refresh elimina a ordem em edição — P2 confirmado

ChannelVersionsDialog.kt:45 publica emptyList numa exceção; o efeito 49–55 reconcilia o draft com esse vazio. Quando Retry recupera, a edição foi perdida. O início da repetição também pode habilitar Guardar enquanto mantém o vazio anterior.

Correção: Loading/Data/Error separados; conservar última resposta e rascunho em falha; reconciliar apenas sucesso; bloquear Guardar durante refresh. Não afirmar corrupção da ordem persistida: o defeito é perda do draft e apresentação de vazio como dados autoritativos.

### U3 — Restaurar antes da carga mostra uma ordem e guarda outra — P2 confirmado

Restaurar está ativo com rows=null, marca resetOrder e draft nulo. A resposta posterior pode usar ordem manual; Guardar envia null e restaura realmente a automática.

Correção: ação só com dados válidos e mesma função de ordenação para o que se apresenta e guarda. Teste com leitura atrasada e ordem manual prévia.

### U4 — Desassociar não participa no bloqueio de Guardar — P2 confirmado

Botão em ChannelVersionsDialog.kt:159–162 não depende de saving. RemoveManualChannelVersion em LiveViewModel.kt:1400 inicia operação sem resultado/falha na UI.

Correção: resultado suspenso, bloqueio durante guardar/desassociar e atualização após confirmação. Adição/desassociação imediata é a regra atualmente indicada; não tornar todas as ações transacionais sem decisão explícita.

### U5 — resumo pode apagar nome do decoder desconhecido — P2 confirmado

StreamInfoOverlay.kt:146–149 remove name mesmo em NAMED sem flags hardware/software. Core StreamInfo.kt:189–195 pode devolver texto vazio. Esse estado é possível quando a classificação do decoder não chegou.

Correção: conservar nome ou mostrar não identificado. Abreviar apenas classificação conhecida. O botão Detalhes sem foco é intencional: OK no corpo alterna; não foi classificado como inoperante.

### U6 — falhas de carregamento não são uniformemente tratadas — P2

Pesquisa dos ajustes de canal/lote e o rodapé EPG chamam loaders sem o mesmo tratamento Loading/Data/Error das linhas. Rodapé EpgScreen.kt:1149–1154 chama programmesFor; EpgViewModel.kt:460–465 pode propagar exceções não classificadas como indisponibilidade do fornecedor.

Correção: estado comum e tratamento explícito que propaga cancelamento; distinguir carregar/sem informação/falha. Existe percurso sem tratamento; não foi provado crash na box.

### U7 — EPG conserva células mas OK pode parecer inativo — P2/P3

EpgScreen conserva programas durante refresh e permite setas, mas 987–989 consome OK sempre que loading. O programa antigo pode ainda estar válido, mas a ação não abre.

Correção: permitir abrir apenas dados da mesma identidade/offset e programa ainda válido, ou explicar a espera. Não remover a barreira para todos os refreshes: mudança de perfil/desvio não equivale a mover uma janela temporal.

### U8 — leituras finitas do Guia sobrevivem à saída — P2, otimização

EpgViewModel.kt:394 usa cache com viewModelScope; prefetch em 479. SetGuideActive(false), em 921–923, cancela apenas loadJob. Cache partilhada deixa leituras sobreviverem ao cancelamento de um waiter.

Correção: cancelar prefetch e leituras pendentes ao desativar, manter LRU concluído, reemitir os cancelados ao regressar. Confirmar cancelamento dos pedidos HTTP. Não é fuga infinita nem leak de decoder: trabalho é limitado.

### U9 — isolamento do diagnóstico por perfil — melhoria de robustez

FailedChannelAttempts é singleton de processo com nomes de canais, sem profileId/clear. Ao trocar perfil, pode continuar a apresentar nomes do perfil anterior nos detalhes.

Correção: limpar ou filtrar pelo perfil/âmbito autorizado, mantendo isolamento de owner. Não é reprodução de canais proibidos; o risco é divulgação de texto de diagnóstico entre perfis.

## 4. Limpeza de código e dependências

### Candidatos fortes no módulo App

- navigation-compose: dependência app/build.gradle.kts:378 sem imports NavHost/NavController em Kotlin App; não confundir com navigationevent transitivo da Activity.
- KSP e Room compiler no App: app/build.gradle.kts:27,403, sem entidades/DAOs/Database locais. Core mantém o próprio processador.
- Componentes sem chamadas encontradas: CountBadge, DownloadStatusStrip, ProgressRing, ResumeDialog, SetTmdbNameDialog, quickPreviewRows, CinematicBrowse/CinematicDetails/cinematicQualityBadges, SettingsRowMenu.
- Funções pontuais: LoadingState/ErrorState em StateViews e rememberAirDateFormatter; conservar as restantes funções dos ficheiros.
- OpenSubtitlesAccountScreen, SubtitleSearchScreen e SubtitleDeletePopup; remover juntamente com SubtitleSearchViewModel e seu registo Koin, se o grafo confirmar ausência de entrada.

Validar Kotlin debug/release, androidTest, lint e DI antes de considerar remoção concluída. R8 pode já eliminar parte do bytecode; não prometer redução notável de RAM/APK sem medir. A retirada de processamento KSP desnecessário pode reduzir trabalho de build.

### Preservar

Room runtime/ktx, Paging, Media3, navigationevent da Activity, baseline profiles, TimeshiftResumeDialog, diferidos/gravações, legendas embutidas/importadas/cache, migrações DB e compatibilidade de backups. Não remover dados VOD do Core apenas porque a UI TV não os apresenta.

### Comentários/nomenclatura

Rever PREROLL_OFF e comentário que promete pré-buffer também na retoma; documentação de fallback HLS→TS perante modo apenas HLS/exceção manual; comentário DEFAULT_VOLUME/boost perante volume unitário; comentário wallpaper só path perante container com bytes. Preservar chaves legadas de meteorologia/animações/mobile na leitura de backups quando forem necessárias.

## 5. Inovações com utilidade concreta

1. **Percurso da ligação:** usar fases/eventos existentes para apresentar encerramento, intervalo, pedido, primeiro áudio/vídeo e reprodução. Distinguir tempo de rede do tempo de espera configurado; incluir falha sanitizada anterior sem outra ligação.
2. **Backup verificável:** após exportar, validar leitura/integridade do ficheiro e apresentar resumo sem segredos de listas e configurações incluídas. Antes de restaurar, mostrar diferenças relevantes e a semântica de merge/restauro completo.
3. **Catálogo de configuração:** declarar por família default, limites, origem, motor aplicável e portabilidade. UI/pesquisa/backup usam a mesma definição. Começar por reprodução/gravações; não reescrever SettingsRepository inteiro num lote.
4. **Cobertura de backup:** toda chave nova deve ser classificada como exportável, serializada noutra secção, local/efémera ou legada. Um teste deve falhar quando uma chave fica sem classificação.
5. **Estados comuns de operação:** um padrão curto de Loading/Data/Error e Idle/Saving/Error para os editores, evitando diferenças entre guardar simples, lote, versões e backup.
6. **Navegação direta por data no Guia:** avaliar usar o título da data como seletor compacto de dias disponíveis; filtrar segundo capacidade real de arquivo do canal. Não prometer dias que o fornecedor não publica nem confundir EPG disponível com diferido disponível.

## 6. Fases recomendadas para execução

| Fase | Conteúdo | Aceitação mínima |
|---|---|---|
| 1 — Sessões e identidade HTTP | R1, R2 e Referer no sync; instrumentação de R3 antes de alterar contrato | Não exceder limite conhecido; reserva atómica; fonte protegida funciona no teste e sync |
| 2 — Backup fiel | S1 e semântica S2, guardas de operação local S6 | Round-trip das escolhas explícitas/defaults; compatibilidade dos antigos; sem resultados tardios |
| 3 — Robustez dos editores | U1–U4, consultas de U6 e cancelamento correto | Falha/Retry conserva draft; Voltar durante save não cria resultado ambíguo; reset corresponde ao que se vê |
| 4 — Diagnóstico e Guia | R5, U5–U9 | Código da falha chega da origem; motor/owner/perfil corretos; footer não falha silenciosamente; saída cancela leituras pendentes |
| 5 — Coerência das opções | S3–S5, semântica Guardar/Concluído, âmbito por motor | Opção visível tem efeito ou explica âmbito; valores legados apresentados corretamente |
| 6 — Limpeza e evolução | Grafo de dependências/componentes e inovações selecionadas | Compile debug/release/androidTest+lint+testes, medir build e UI antes/depois |

Não adicionar delays globais, buffers maiores, restart obrigatório de decoder ou novos transportes sem um caso dirigido que o justifique. Limpeza não deve remover suporte de diferidos/gravações nem compatibilidade histórica.

## 7. Evidência executada e limites

Executado nesta auditoria: `:core:testDebugUnitTest --tests '*ConnectionBudgetTest'` — 17 testes, zero falhas/erros. Eles confirmam a política atualmente codificada, incluindo a autorização cuja preempção é apenas presumida. Não constituem teste de encerramento de tiles.

Não houve novo build completo da app, medição CPU/RAM/fotogramas, ensaio na Xiaomi/Thomson ou reprodução física dos casos. Foram feitas pesquisas de referências/DI, inspeção dos percursos e consulta de documentação oficial. Os 380 testes da implementação UI anterior são evidência dessa execução anterior, não substituem os casos de falha/cancelamento propostos aqui.

Referências oficiais utilizadas para fundamentar os padrões, sem atribuir-lhes validação desta app:

- [Efeitos e scopes em Compose](https://developer.android.com/develop/ui/compose/side-effects): scopes de composição cancelam ao sair; mudanças de keys reiniciam efeitos. Fundamenta a separação entre dados válidos, loading e falha, e a propriedade das operações de guardar.
- [Restauração de foco](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-restoration): foco em listas requer política de alvo/restauro. Não valida o D-pad desta implementação.
- [Analytics Media3](https://developer.android.com/media/media3/exoplayer/analytics) e [eventos do player](https://developer.android.com/media/media3/exoplayer/listening-to-player-events): eventos existentes permitem diagnósticos associados à reprodução. Não é necessário criar polling de rede paralelo.
- [Cancelamento Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html): distinguir cancelamento cooperativo de erro operacional; não engolir CancellationException.

## 8. Ronda adicional — gravações, reprodutor externo e publicação

Adenda de 13/10/2026. Estes pontos foram acrescentados à lista existente, sem aplicar correções. Os especialistas forneceram achados adicionais e o coordenador verificou os principais percursos no código. A nova síntese conjunta dos agentes não pôde ser concluída devido a limite de utilização; a consolidação abaixo distingue evidência estática de testes ainda necessários. Não foram executados novos builds ou ensaios físicos nesta adenda.

### N1 — CI pode publicar um APK só arm64 como universal — P1 confirmado no pipeline

App `app/build.gradle.kts:111–117` produz splits ARM e universal. O metadata local de Standard Release contém `app-standard-arm64-v8a-release.apk`, `app-standard-armeabi-v7a-release.apk` e `app-standard-universal-release.apk`.

`.github/workflows/android.yml:265` seleciona `ls .../*.apk | head -n1`; a ordenação lexical normal escolhe arm64 antes de armeabi/universal. O ficheiro é depois renomeado para asset genérico, com comentário que anuncia suporte às duas arquiteturas.

Impacto: o pipeline pode publicar uma atualização incompatível com boxes 32 bits. Isto não implica que o APK universal escolhido manualmente no Android Studio esteja errado, nem comprova que uma release remota concreta já foi publicada assim.

Correção: selecionar explicitamente o output UNIVERSAL de Standard Release, falhar se ausente e verificar as bibliotecas `lib/armeabi-v7a` e `lib/arm64-v8a` do asset final. Para assets por arquitetura, nomes e metadata devem conservar a ABI. Validar o flavor x86 separadamente.

Aceitação: três outputs em ordens diferentes continuam a publicar o universal correto; ficheiro genérico contém ambas as ABIs. Atualizar comentários do CI para corresponder ao formato real.

### N2 — atualizador não distingue os splits ARM — P2 confirmado

Core `core/update/UpdateManager.kt:128–146` diferencia apenas x86_64 de não-x86_64; não classifica v7a/arm64/universal. Se não encontra correspondência, aceita o primeiro APK disponível.

Correção: seletor puro baseado nas ABIs suportadas e na identidade do asset; universal compatível como alternativa; falha explícita sem pacote compatível. Verificar pacote, minSdk/ABI e integridade antes da instalação, preservando a verificação de assinatura do PackageInstaller.

Aceitação: v7a, arm64, x86, universal, release só com ABI errada e nomes inesperados. Não aceitar asset arbitrário.

Hipótese descartada: a app personalizada NÃO está configurada para atualizar a partir do upstream. `OwnTVApp.kt:95` aponta para `renafcampos27/Owntv`. Não há evidência de atualização apagar customizações por esse motivo.

### N3 — retry de arquivo RAW pode concatenar o início duas vezes — P1, integridade condicionada a interrupção

Core `recording/RecordingEngine.kt:527` pede a URL sem Range; `recordRaw:569` abre saída com append. Se uma leitura parcial lança IOException, `runRecording:343–354` classifica NETWORK e a rotina volta a tentar a mesma origem.

Para um direto contínuo, append pode ser a política desejada. Para um arquivo finito TS/MP4, se a nova resposta começa novamente no byte zero, o ficheiro recebe o prefixo já escrito seguido do programa desde o início. Não há prova de que esse servidor retome automaticamente no ponto interrompido.

Correção mínima: diferenciar reconexão Live de recuperação de arquivo. Sem prova de retoma, preservar o arquivo como parcial e não concatenar outra resposta inteira. Retoma por bytes só com Range/206, Content-Range e identidade/validador da representação verificados; resposta 200 não deve ser tratada como continuação. Um reinício deve truncar/recriar explicitamente e conservar a alternativa parcial segundo política definida.

Aceitação: endpoint envia um prefixo e corta; segundo pedido devolve corpo inteiro. O resultado nunca contém prefixo duplicado nem é declarado completo indevidamente. Cobrir 200, 206 correto/incorreto, validator alterado e cancelamento. A recomendação não altera o retry dos canais Live saudáveis.

### N4 — erro de finalização DASH pode ser tratado como sucesso — P1 confirmado no contrato

Core `recording/DashRemux.kt:73` devolve true depois de copiar samples; `:81` chama muxer.stop no finally dentro de runCatching, ignorando uma falha. RecordingEngine.finishDashSession apaga os tracks temporários quando result.succeeded (`:1286`).

Cenário: a cópia termina, mas MediaMuxer.stop falha ao finalizar o contentor. O resultado continua true e os dados de origem podem ser apagados. HlsRemux já condiciona sucesso à finalização, portanto também há incoerência entre formatos.

Correção: stop bem-sucedido faz parte do caminho de sucesso; só publicar sucesso e remover inputs após finalização e validação básica do output. Em falha, preservar tracks para recuperação e eliminar apenas output inválido segundo política. Propagar cancelamento; rever cópia SAF e loop de samples quanto a cooperação com cancelamento/limites, sem alegar leak comprovado.

Aceitação: adapter do muxer lança em stop; resultado é falha, fontes ficam intactas e recuperação continua possível. Testar output vazio/inválido, sucesso e destino SAF.

### N5 — Retry-After é respeitado nos segmentos, mas perdido noutros pedidos de gravação — P2 confirmado

RecordingEngine.downloadHls:820 conserva status e Retry-After. Já attemptRecord:532–542 devolve classificação sem esse header e loadHlsPlaylist:784 lança IOException sem conservá-lo. O ciclo geral usa RecordingRules.retryDelayMs(attempt), podendo pedir antes de um prazo explícito do servidor.

Correção: metadados tipados de falha HTTP e prazo de repetição para pedido inicial/playlist/segmentos, com espera limitada, monotónica e cancelável. Reutilizar parser/política existentes; não acrescentar delay global. Se a janela de gravação já terminou, não insistir.

Aceitação: playlist responde 429/503 com Retry-After 30 s; nenhum novo pedido antes do prazo, cancelamento termina logo e sucesso posterior retoma sem duplicação.

### N6 — resolução externa antiga pode abrir depois de mudar de contexto — P1/P2 confirmado por falta de ownership

App LiveViewModel.playExternal:1891–1927 valida perfil e categoria antes de consultas/resolve suspensos. Antes de ACTION_VIEW só verifica cancelamento, sem confirmar novamente perfil/fonte/sintonia. ExternalLaunchJob é cancelado por outro lançamento externo/retorno, mas não está integrado na invalidade de uma nova sintonia interna ou onBackground.

EpgViewModel.playCatchupExternal:601–618 também resolve URL/UA e lança sem geração de operação e revalidação final.

Cenário: resolver lento; utilizador muda de perfil, sai ou abre outro canal; a resolução antiga termina e lança o reprodutor externo. Isto é distinto de um erro de rede do canal.

Correção: owner de perfil/fonte/canal e geração, invalidado por nova seleção/mudança de contexto; verificações depois dos awaits e imediatamente antes do lançamento. A transição normal para o próprio reprodutor externo gera background e não deve ser cancelada retroativamente como se fosse uma saída manual.

Aceitação: resolver suspenso + troca de perfil/nova sintonia/Home não lança o pedido antigo; lançamento autorizado e regresso ao canal continuam a funcionar.

### N7 — diferido externo perde identidade HTTP numa das rotas — P2 confirmado

LiveViewModel.playCatchupExternal:2340 envia URL/título, mas não UA nem headers. LiveViewModel.playExternal:1912–1918 e EpgViewModel.playCatchupExternal:609–617 já enviam ambos.

Impacto condicionado: diferido que requer UA/cookies/Referer pode funcionar internamente ou por outra rota e falhar nesta. Nem todos os reprodutores externos honram extras; a obrigação da app é fornecer de forma coerente o que o launcher suporta.

Correção: construir descritor de lançamento comum com source UA e headers do canal, aplicado com o owner de N6. Resolver herança Referer segundo R4, sem espalhar credenciais entre contas.

Aceitação: launcher falso recebe os mesmos parâmetros nas rotas correspondentes de Live/arquivo; fonte com UA próprio e canal com header específico; cancelamento de contexto não lança.

Extensão da hipótese R3: handoff externo também chama stop sem aguardar confirmação antes de ACTION_VIEW. É outro consumidor do contrato de libertação; investigar com transporte bloqueado, sem declarar sobreposição remota comprovada.

### N8 — gravação Stalker pode perder a correção temporal ao renovar URL — P2 confirmado, não explica Xtream

RecordingManager.archiveUrl:548–551 corrige os tempos para construir o pedido, preservando os timestamps originais em RecordingEntity (`:250–253`) para identidade e apresentação. RecordingEngine.resolveTarget:1494–1504, em Stalker/needsResolve, ignora a URL anterior e resolve com os tempos originais, sem reaplicar a correção.

Impacto: leitor e gravação/download podem pedir programas diferentes quando existe desvio configurado. O ramo Xtream reutiliza a URL já corrigida; não atribuir este caso ao problema Xtream relatado pelo utilizador.

Correção: resolução fresca usa tempos de pedido corrigidos exatamente uma vez, com função partilhada e snapshot coerente de offset/timezone; DB continua com identidade original. Não aplicar shift duas vezes.

Aceitação: +60/−60 min, mudança de dia/DST, primeira captura e retoma; resolver recebe tempos esperados e apresentação original não muda.

### N9 — lembrete consumido antes do início — proposta opcional, não bug universal

ProgrammeReminderStore.kt:145–152 remove um lembrete quando o canal correspondente já está a reproduzir durante a janela de antecedência, mesmo antes do início. Um teste existente valida essa política, portanto é uma escolha atual.

Caso a melhorar: ver A a 30 s do início, mudar para B antes do programa começar; o lembrete já desapareceu.

Proposta: suprimir aviso enquanto se vê A, mas conservar o lembrete até ao início efetivo; consumir se se continua a ver A nesse momento. Se se muda para B, o lembrete ainda pode alertar. Confirmar intenção de produto antes de alterar; não acrescentar novo menu.

Aceitação: A→B antes do início mantém alerta; ver A continuamente até ao início consome; pausa/buffering e perfis continuam respeitados.

## 9. Lista consolidada e prioridades após a adenda

Mantêm-se R1–R5, S1–S6 e U1–U9 da auditoria anterior. Acrescentam-se N1–N8 como novos problemas/casos de robustez e N9 como evolução opcional. Assim existem 28 pontos de correção/robustez/coerência e uma proposta adicional de lembrete; não são 28 bugs físicos reproduzidos.

| Ordem | Trabalho consolidado |
|---|---|
| A — Integridade e compatibilidade | N3/N4 (arquivos/finalização) e N1/N2 (APK/arquitetura), R1/R2 (sessões) |
| B — Pedidos e propriedade | R4/R5, N5/N6/N7; comprovar R3 e handoff externo antes de mudar o contrato |
| C — Configurações e backup | S1/S2/S6 e U1–U4/U6; valores efetivos, cancelamento e confirmação |
| D — Guia/diagnóstico/coerência | U5/U7–U9, S3–S5 e N8; N9 só se aprovado como política |
| E — Limpeza/otimização | Remoções candidatas da secção 4, descritores comuns de pedido/operação, documentação e medições |

As fases anteriores continuam utilizáveis, mas a fase de integridade deve incluir N3/N4 antes de continuar a evoluir downloads. N1 não exige alterar o APK manual correto; exige reparar a seleção feita no pipeline.

Novas referências oficiais consultadas: [HTTP Semantics/RFC 9110](https://www.rfc-editor.org/rfc/rfc9110.html), para Retry-After e retoma parcial validada; [MediaMuxer](https://developer.android.com/reference/android/media/MediaMuxer), para distinguir escrita de samples de finalização do ficheiro. Estas referências não provam o comportamento na box.

Nesta adenda foram lidos código e metadata já existentes. Não houve nova execução de testes, geração/publicação de APK, alteração de biblioteca ou modificação de dados. A validação futura precisa dos casos de falha dirigidos acima e de ensaio físico onde indicado.
