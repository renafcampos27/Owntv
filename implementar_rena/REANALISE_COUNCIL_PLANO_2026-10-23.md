# OwnTV TV — reanálise crítica do plano e decisão do council

**Data:** 23/10/2026. **Âmbito:** análise, sem implementação de código.

Documento reavaliado: [analise_final_ia_analisadasol.md](analise_final_ia_analisadasol.md). Esta revisão prevalece sobre a utilização das propostas e dos números de linha antigos como tarefas atuais. O histórico de implementação é conservado.

## 1. Decisão

**Não aplicar todas as propostas novamente, nem tratar todas as pendências como correções comprovadas.** A maior parte dos blocos das fases 1–5 tem implementação material. A fase 6 contém trabalho útil, mas também mecanismos já presentes e exemplos de limpeza desatualizados. Os candidatos MED e as novidades NOV mantêm condições próprias.

Participaram três agentes independentes: dados/EPG/Room/backup; reprodução/HLS/áudio/ciclo de vida; segurança/distribuição/serviços/limpeza. O coordenador reviu interface, definições e identidade dos fontes, confrontou os pareceres e pediu contraditório sobre libertação do timeshift, seleção do Home e classificação/prioridade final. É uma revisão técnica multiagente, não certificação externa.

### Contagem dos 41 IDs principais

| Classificação atual | Quantidade | Significado |
|---|---:|---|
| Implementados no âmbito inspecionado | 28 | Preservar; não reimplementar só porque continuam descritos nas secções originais. |
| Implementados com lacuna/complemento residual | 6 | DAT-03, INT-02, PLY-01, PLY-02, PLY-07, UI-07; ver secção 4. |
| Blocos da fase 6 a completar | 6 | INT-06/07/08/09 e OPT-01/02. Vários já têm proteções parciais. |
| Verificação transversal por completar | 1 | OPT-03: alcance real das definições. |

São **IDs de trabalho**, não 41 bugs reproduzidos nem 28 funcionalidades aceites numa box. UI-09 e PLY-08 complementam o mesmo domínio; não devem inflacionar a contagem de defeitos. Os 15 MED, seis NOV e 37 linhas agrupadas de propostas recusadas/condicionadas da secção 10 original são inventários separados. Essas 37 linhas não equivalem a 37 falsos bugs: incluem propostas perigosas, não demonstradas, fora do âmbito ou deliberadamente adiadas.

## 2. Base efetivamente verificada

- App: `C:/Users/renat/Downloads/OwnTV-main`, HEAD `bdffec4`, com alterações locais preexistentes.
- Core TV externo: `C:/Users/renat/Downloads/OwnTV_Core`, HEAD `e7a044e`, também personalizado e com alterações locais.
- Input efetivo da App: `core-source-lock.json`, versão técnica `1.0.57-own.20261011`, **1 061 inputs**, SHA-256 `1dcc86280d668242d65d379f3b845b357a73901464e4674b71b941212afb5011`.
- Conferidos nesta revisão: hash do ZIP, inventário e bytes dos 1 061 inputs contra os fontes externos selecionados pela ferramenta de snapshot; **zero diferenças**. Não se pressupôs que a App usa automaticamente qualquer edição no Core externo.
- Esquema personalizado: **51**. O SHA de 1 007 ficheiros da secção 2 original é uma fotografia de 20/10, não o estado atual.

**Método e limites:** leitura dirigida de código, testes existentes, registos das fases e documentação primária. Não se executaram Gradle, testes, builds, benchmarks ou ensaios físicos nesta reanálise. O registo da fase 5 de 22/10 reporta 2 422 testes JVM, seis testes da ferramenta de snapshot, compilações e lint aprovados; é **evidência histórica não repetida agora**, não prova dos resíduos aqui encontrados. Não foi gerado APK, feita publicação, alterada assinatura ou tocada a instalação/dados da box ou OwnTV Mobile.

Nas referências seguintes:

- `App:` = `OwnTV-main/app/src/main/java/tv/own/owntv/`.
- `Core:` = `OwnTV_Core/core/src/main/java/tv/own/owntv/core/`.
- `Player:` = `OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/`.
- Linhas são localizadores desta revisão; podem mudar com edições posteriores.

## 3. O que já está implementado e deve ser preservado

| IDs | Verificação atual e limites |
|---|---|
| UI-01, UI-02 | DNS/proxy com validação comum, configuração conservada ao desligar, presets sem gravação imediata; fontes e formulário EPG validam as entradas permitidas e avisam duplicados. HTTP IPTV, portas privadas e ficheiros locais/content não são proibidos. |
| UI-03, UI-04 | Foco numérico limitado, precedência das ações específicas das definições, identidade separada da posição visual nas listas agrupadas e invalidação de pedidos antigos. Comando real continua a exigir ensaio na box. |
| UI-05 | Teste/edição de listas vinculado a sourceId e revisão; resultados de configurações antigas recusados e teste correspondente dispensado ao editar. `features/settings/SettingsViewModel.kt:1139–1140,1254–1288,1821–1841`. |
| UI-06 | Importação com autoridade e transferência explícita para trabalho de fundo; retorno a Idle apresenta interrupção em vez de spinner eterno. `features/setup/SetupWizard.kt:834–836,898–899` e `SetupViewModel.kt:158,221`. Não garante conclusão após qualquer morte do processo. |
| UI-08 | Deep link consumido sobrevive à recriação; novo Intent para o mesmo canal tem revisão distinta. `MainActivity.kt:127–160` e `LaunchDelivery.kt`. Não exige remover launcher/deep links. |
| UI-09 / PLY-08 | Resultados tipados de gravação, identidade/perfil, libertação antes de apagar o ficheiro em reprodução e falhas de continuação tratadas. `features/live/LiveViewModel.kt:429–486,2653–2676`. A passagem ao direto decidida pela política de continuidade não é uma exceção silenciosamente escondida. |
| UI-10 | Bitrate desconhecido distinto de zero medido, idioma desconhecido sem rótulo Automático enganador e semântica de seleção/papel nos controlos revistos. Não constitui certificação de acessibilidade; 720p e leitor de ecrã pendentes. |
| INT-01, INT-03, INT-04, INT-05 | Identidades portáveis sem colagem por ID numérico desconhecido; limites de cifra antes do KDF; nomes manuais distintos e confirmação da senha; proteção local do perfil preservada no restauro. O PIN local não é substituído pela senha do backup. |
| DAT-01, DAT-02 | XMLTV incompleto não promove o staging nem desencadeia poda como atualização saudável; deteção Xtream distingue indisponibilidade de resposta negativa; M3U inválido/vazio é recusado antes da poda. Não é uma transação que bloqueia a BD durante toda a transferência. |
| DAT-04 | Base64 conservador nos títulos e BOM/UTF estritos. Não há fundamento para fallback universal Windows-1252 sem amostra. |
| DAT-05, DAT-06, DAT-07 | Contrato dos triggers FTS/reparação dirigida, restauro transacional dos índices, lotes EPG com owner/estado esperado e fila de pendências de backup serializada. Preservar esquema 51 e contratos legados. |
| DAT-08 | Caches EPG derivadas têm limites por quantidade/peso e não cortam a resposta nem cancelam produtores por eviction. `Core:live/LiveEpgReader.kt:72–92`, `GuideMemoryCache.kt:14–34`. A cache crua de pedidos continua limitada por quantidade/TTL, não por bytes: risco a medir em MED-02, não OOM demonstrado. |
| DAT-09 | Retry-After respeitado na operação/processo atual com erro explícito se exceder o orçamento curto. O mapa é em memória: não prometer persistência após morte do processo. Scheduler novo não é exigido pelo contrato atual. |
| PLY-03, PLY-04 | Ícone Play/Pausa segue intenção, mesmo em buffering; MediaSession/foco têm proprietário, incluindo mosaico audível. `Player:PlaybackSession.kt:54–69,239–272`; `App:features/shell/OwnTVShell.kt:375–393`. Assistant/HDMI exigem teste físico. |
| PLY-05 | Frames publicados deixam de ser reciclados prematuramente; bitmaps privados de captura falhada continuam libertados. `Player:OwnTVPlayer.kt:1690–1699,2488–2495`. Não demonstra redução de heap ou um crash físico resolvido. |
| PLY-06 | Outcome/pausa por eventos, observadores ligados à tentativa e cancelamento propagado. Subsiste verificação ativa delimitada a 50 ms para metadados/prazos: não anunciar eliminação total de polling. |
| PLY-09 | OSD segue canal selecionado e multiview tem foco por slot/revisão. `App:player/PlayerHud.kt:604–605`, `features/multiview/MultiviewScreen.kt:93–98,180–181`. |
| OPT-04 | Contrato obrigatório de intenção nos motores/adaptadores, pausa em perda de foco e volume interno unitário. Não introduzir ducking, boost ou normalização. |

## 4. Seis resíduos: implementar apenas a diferença necessária

### R1 — DAT-03: conversões de URL de diferidos ainda podem mudar as credenciais

**Evidência:** `Core:epg/CatchupUrl.kt:165–168,292–298`. O builder principal de `parser/XtreamUrls.kt:8–21` está correto, mas dois conversores ainda interpolam segmentos de path diretamente numa query PHP. São alcançáveis pelo fallback `epg/CatchupFormats.kt:13–21` e pelo modo M3U `xc/xtream`.

Uma senha cujo segmento contenha `p&a` passa a `password=p&a`: o separador passa a ter significado na query. Um `+` literal tem também semântica diferente entre path e query. A transformação incorreta é verificável estaticamente; o efeito num fornecedor depende dos caracteres e do endpoint utilizado.

**Recomendação:** extrair os segmentos lógicos e construir os parâmetros com o builder adequado, conservando tokens existentes e sem dupla codificação. Alteração restrita aos conversores; não alterar fuso, +1 h, extensão saudável ou política HLS.

**Aceitação futura:** testes ponta a ponta dos dois conversores com `&`, `+`, `%`, `/`, `?`, `#`, valores já codificados e tokens. Comparar os valores decodificados que o servidor receberia, não apenas a aparência da string.

### R2 — INT-02: backup pode omitir recursos legados sem resumo inequívoco

**Evidência:** `Core:backup/BackupManager.kt:358–360,382,391,415,1312–1314,1326–1327`. A exportação principal já exige cifra e falha nas leituras selecionadas antes da publicação atómica. Persistem omissões de legendas referidas ausentes/vazias/acima de orçamento, referências retiradas e importação de legendas reduzida a zero quando falha. A seleção Off pode também perder-se quando não existe qualquer cache empacotada.

**Recomendação:** distinguir sucesso completo de parcial/erro para recursos pedidos; apresentar omissões no resultado; preservar a seleção Off independentemente de existir um ficheiro. Não reintroduzir filmes, séries ou OpenSubtitles para justificar o fix. A relevância concreta depende dos dados legados presentes; não é prova de que fontes, senha e preferências atuais estejam todas a perder-se.

**Aceitação futura:** recurso ausente, vazio, demasiado grande, falha de leitura/importação e Off sem cache; verificar payload e resumo final. Deduplicar com MED-13/NOV-02. Prioridade de integridade antes de limpeza ou novidades.

### R3 — PLY-01: confirmar fecho também ao entrar no timeshift local a partir de Exo

**Evidência:** `App:features/live/LiveViewModel.kt:318,335,354–359`; `Player:LiveTuneController.kt:416–441`; `LivePreviewEngine.kt:2196–2209`; `Core:timeshift/LocalTimeshiftSession.kt:23–50,76–78,120–122`.

Abrir programa/recuo remoto já espera os dois leitores. No percurso adjacente **Exo direto → timeshift local**, a paragem Exo é assíncrona, só se aguarda a autoridade OwnTVPlayer e pode começar o produtor HTTP local antes de confirmar o drain Exo. O produtor e o cliente injetado não têm uma segunda barreira escondida que resolva este intervalo.

**Recomendação:** reutilizar a confirmação conjunta pertinente à sessão antes de iniciar o produtor, revalidar owner/geração e preservar pausa/cancelamento/limpeza. Sem atraso fixo novo, sem interromper gravações. É lacuna de ordenação local num percurso opcional, não prova da causa de todas as recusas A–B–A do fornecedor.

**Aceitação futura:** segurar artificialmente o drain Exo e comprovar zero pedidos do novo produtor; testar cancelamento, pausa, nova seleção e resultado negativo de libertação. Um timeout não pode ser tratado como fecho confirmado.

### R4 — PLY-07: índice do zapping fica desatualizado após alteração do catálogo

**Evidência:** `App:features/live/LiveViewModel.kt:1810–1843`, `PagedZapIndex.kt:10–33`, `LiveZapList.kt:186–194`.

O índice de IDs é construído por páginas e guardado. A validade compara contexto/personalização, mas não revisão do catálogo. Se o próximo ID desaparecer numa sincronização, a consulta atual não o encontra, devolve null e a âncora fica igual: CH+ pode repetir o mesmo alvo desaparecido. Entradas novas e nova ordem também podem ficar ausentes dessa sessão.

**Recomendação:** invalidar/reconstruir por revisão relevante e tratar alvos desaparecidos com procura limitada e owner conferido. Preservar contexto, ordem, ocultos/grupos, janela de 17 entidades e leitura fora da thread principal. Não reconstruir todo o catálogo a cada seta nem abrir streams vizinhos.

**Aceitação futura:** depois de criar o índice, remover o próximo canal, inserir canal, reordenar e sincronizar durante carga; CH± deve continuar sem ressuscitar IDs antigos ou aceitar resultado de outro contexto.

### R5 — UI-07: seleção de «No momento» pode ficar fora da janela visível

**Evidência:** `App:features/home/HomeGuideSlice.kt:199–215,253,327,470–483`. O refresh remapeia a seleção por ID mas apenas limita o início da janela; o helper que assegura visibilidade só é chamado na seleção manual.

Exemplo: oito linhas, janela em 0 e canal selecionado movido do índice 1 para 7. As cinco linhas visíveis continuam a ser 0–4, mas OK pode abrir a seleção invisível. O helper existente colocaria o início em 3.

**Recomendação:** reconciliar identidade, seleção substituta quando removida e janela visível na mesma atualização. Aplicar só a este modo do Home; não repor scroll incondicional em cada refresh nem retirar preview cuja identidade persiste.

**Aceitação futura:** reordenação, remoção, redução e esvaziamento das linhas mantendo a ação OK coerente com o elemento visível. Correção de usabilidade de âmbito pequeno, sem ensaio físico nesta análise.

### R6 — PLY-02: aprendizagem temporária não é invalidada por revisão da fonte

**Evidência:** `Player:LiveStreamQuirks.kt:128,138–156,381–392,471–479`; `LiveLadder.kt:145–151,201–213`; `LiveTuneController.kt:808`.

Já se distingue formato de HTTP/transporte/timeout e só se aprende após formato inválido seguido de TS confirmado no mesmo motor. A memória tem TTL de 30 minutos e capacidade limitada; HLS obrigatório continua protegido. Porém usa URL/motor sem revisão explícita de headers/fonte; o reset encontrado é de teste.

**Recomendação:** invalidar por alteração relevante de fonte/headers e delimitar o reset de produção necessário. Não criar um botão/score novo por defeito, não memorizar HTTP 403/404 como formato e não retirar exceções manuais. É hardening complementar, não falha permanente ou bypass do HLS estrito.

**Aceitação futura:** corrigir a fonte mantendo a URL, antes do TTL; a decisão antiga não deve sobreviver à revisão. Manter os testes de evidência negativa, motor diferente e HLS obrigatório.

## 5. Fase 6: aprovada com âmbito atualizado

| ID | O que ainda recomendo | O que já existe ou não deve ser feito |
|---|---|---|
| INT-06 | Autoridade comum e persistida de tentativas de PIN, por alvo/credencial, aplicada nos vários gates; migração de hash após autenticação válida e política explícita para Kids sem PIN. | Grants por alvo/ação/expiração/uso único já existem. Não criar limitador só num diálogo, exigir seis dígitos a todos, invalidar hashes antigos ou bloquear o único adulto sem migração. `Core:util/Pin.kt:13–27`, `profile/ProfileOperationAuthorization.kt:8–31`, `ProfileManager.kt:69–78,120–125`. |
| INT-07 | Revogar segredo na autoridade ativa, proteger substituição de identidade emparelhada, limitar respostas efetivamente lidas pelo cliente, staging com owner, resultados e limpeza em IO/leases/TTL. | `LocalSyncManager.kt:173,197,275` mantém segredo em memória após unpair. Pair com ID exige PIN válido: não é takeover sem autenticação. Servidor já tem autenticação, matriz de modos, limites e GET por streaming. `LocalSyncClient.kt:103–118` ainda tem readBytes sem teto. |
| INT-08 | Fonte de versão coerente, parser que suporta o formato escolhido e barreira de distribuição com assinatura autorizada. Preservar chave instalada e versionCode crescente face ao APK realmente distribuído. | `app/build.gradle.kts:49–51,175–179` admite fallback debug; workflow `:113–121` assume três componentes apesar da versão local de quatro. Não bloquear Sync/testes/compilação sem secrets nem inventar versão instalada. Nenhum APK foi inspecionado agora. |
| INT-09 | Permissões por job, Actions fixadas por SHA revisto, assets existentes/determinísticos, checksum oficial Gradle, dependências verificadas e lint release explícito; testes Android em job próprio. | CI já testa App/Core/motores e ferramenta de snapshot; hash/determinismo/publicação atómica do snapshot já existem. ARM já é conferido; seleção x86/lista de assets ainda merece correção. Checksum gerado localmente não autentica origem. |
| OPT-01 | Completar separação entre progresso e informação de armazenamento em Downloads, reutilizar formatters por locale com confinamento, invalidar imediatamente por raiz/remoção/erro e medir custo. | Gravações já têm amostragem/coalescência; storageInfo já corre em IO. Quotas/reservas antes da escrita não são a consulta informativa e devem ficar intactas. Não anunciar I/O bloqueante da UI ou ganhos percentuais sem prova. |
| OPT-02 | Remover mapa versionGroupKeys sem leitura, bloco if(false && mode.enrich), estado/diálogo VOD sem abertura após confirmar consumidores; avaliar dismissedKeys com testes de lembretes. | Picker do canal de arranque e cor **funcionam** e não são lixo. Comentário GuideLoadGate já foi corrigido. Não apagar Room KTX, contratos/tabelas Core, chaves legadas ou snapshots/rollback. |

### Ajustes obrigatórios à descrição antiga de INT-07

- `CompanionHttpProtocol.readBody` depende de Content-Length. Ausente/inválido resulta em vazio; negativo precisa de rejeição explícita. Os limites já existem, mas não provam suporte geral a chunked ou contagem autónoma até EOF.
- A solução segura é definir/rejeitar framing inválido ou não suportado e preservar os limites, **não ler indefinidamente até a ligação fechar**. Corrigir o teste chamado «header lies small», que atualmente não simula esse caso.
- O Core já separa receber de aplicar. Falta fechar o contrato de staging/resultados em todos os caminhos; ACK HTTP não significa restauro aplicado. `Core:companion/CompanionController.kt:193–207` pode capturar falha ao persistir temporário sem a devolver como falha de transporte utilizável.
- O menu LocalSync está oculto, mas o onboarding ainda tem consumidor; esconder um menu não torna todo o serviço morto.

### OPT-03 deve acompanhar os lotes, sem assumir que as opções estão todas erradas

Construir a matriz `opção TV → UI/atalho → setter → persistência → valor entregue ao motor`. Já existem cadeias reais entre `VideoPlayerSettingsScreen`, `SettingsViewModel`, `SettingsRepository`, `PlaybackSettings`, `LivePreviewEngine` e `OwnTVPlayer`. Testes de backup comprovam transporte/persistência, não toda a cadeia.

Priorizar hardware, áudio por software, saída/atraso de áudio, HLS/exceções por canal, buffers/latência e SDR. Distinguir opção guardada de aplicada ao motor. Registar opções internas/legadas/ocultas sem reabrir os menus removidos. Hardware, HDMI e codec efetivo continuam a exigir box.

## 6. MED-01–15: não promover automaticamente a correções

| ID | Decisão atual |
|---|---|
| MED-01 | Medir consultas/EXPLAIN com catálogo representativo antes de novos índices ou NOT IN→EXISTS; DAT-05 não prova necessidade de novos índices. |
| MED-02 | Medir arranque, heap, grupos e Paging. O índice de zapping já reduz entidades retidas, mas Pager/caches de grupos e cache crua EPG têm políticas próprias. Não assumir fuga/OOM nem reduzir maxSize cegamente. |
| MED-03 | Trace de recomposições/tempo de frame antes de mover leituras para draw/layout. Sem contagens/percentagens inventadas ou reintrodução de animações. |
| MED-04 | Medir I/O de gravação/timeshift simultâneo; conservar fsync, quotas, checkpoints e publicação atómica. Não trocar durabilidade por desempenho presumido. |
| MED-05 | Ensaiar restrições foreground nas versões Android alvo. A recusa inicial de setForeground propaga-se atualmente; é falsa a alegação de que é sempre engolida e o trabalho continua sem foreground. Recuperação/estados continuam a avaliar. |
| MED-06 | Testar executor/JNI bloqueado antes de novo coordenador terminal mpv. Daemon, timeout e cancelamento de scope não confirmam destruição de recursos. |
| MED-07 | Updater já valida vazio/truncado/package/ABI e geração. Desenhar cliente exclusivo HTTPS, redirects/CDN, digest e certificado/lineage com compatibilidade legada antes de alterar; não o chamar «sem validação». |
| MED-08 | Streaming Base64 compatível pode reduzir pico de memória. 16 MiB é corpo codificado, não backup binário; o útil é inferior a 12 MiB com overhead. TLS autenticado requer desenho de confiança/pareamento e versão compatível, não substituição literal do URL. |
| MED-09 | Mapear emissores/concessões SAF/FileProvider/OEM antes de restringir raízes. exported=false já existe; raízes amplas não são diretórios públicos por si só. |
| MED-10 | Whitelist Kids e autoridade adulta independente são política de produto/migração, separadas do limite de tentativas. Não ocultar todos os canais sem categoria num update silencioso. |
| MED-11 | Novos charsets/reparação de headers só com fixtures reais; BOM/UTF conservador já implementado. |
| MED-12 | Verificar alcance do DownloadEngine VOD antes de endurecer 416/EOF. Programas EPG usam RecordingEngine; não confundir os dois nem reintroduzir VOD. |
| MED-13 | Identidade/hash de legendas e MIME exigem consumidores/formatos reais. A omissão concreta entra em R2; colisões por nome ainda não são corrupção reproduzida. |
| MED-14 | A–B–A exige medição por host/conta/canal e respeito pelo intervalo escolhido. Libertação local não prova sessão fechada no servidor. Sem retries agressivos ou nova espera global. |
| MED-15 | Extrair monólitos gradualmente quando facilitar uma alteração concreta. Sem reescrever arquitetura/DI antes de corrigir um defeito delimitado. |

## 7. NOV-01–06: úteis apenas como produto opcional

| ID | Situação e prioridade |
|---|---|
| NOV-01 | **Prioridade opcional alta:** diagnóstico guiado por tentativa. Já há snapshot/HTTP/fases em PlaybackSessionDiagnostics; completar apresentação/exportação manual sanitizada, sem telemetria ou score que muda o motor. |
| NOV-02 | **Prioridade opcional alta:** resumo de restauro. Preview e proteção já existem; ampliar mudanças/omissões sem credenciais em claro. O mínimo para não esconder omissão é R2, não precisa de esperar por todo o produto NOV-02. |
| NOV-03 | Temporizador de sono continua opcional; strings não provam mecanismo ativo. Parar só visualização, preservar gravações e respeitar o sistema. |
| NOV-04 | Janela de diferidos mais clara tem infraestrutura parcial e feedback de tentativas; não garante duração do arquivo. Reutilizar EpgScreen/GuideCore/ReplayEvidenceStore e motor de gravação existente. |
| NOV-05 | Desfazer apenas definições reversíveis por revisão/TTL; excluir ficheiros, PINs, credenciais e restauros parcialmente aplicados. |
| NOV-06 | Alternativas do mesmo programa continuam opcionais e distintas de versões do canal; identidade/fonte/horário e confirmação, sem streaming antecipado. |

UPSCALING-BETA-01 continua apenas registado para investigação TV, opcional e desligado por defeito se vier a existir. Buffer não cria detalhe; não é correção de HLS, áudio ou zapping.

## 8. Falsos positivos, recomendações antigas e alterações a recusar

Conservar as decisões de rejeição da secção 10 original, com estas distinções explícitas:

1. **Já corrigido não é nova tarefa:** multiview já fecha no lifecycle; ContentFds tem sincronização/leases; preview separa stop/release; parar/guardar gravação não cancela indiscriminadamente finalização; GET backup já é streaming; servidor já tem matriz de modos. Reabrir só com reprodução nova específica.
2. **Limpeza antiga pode estar errada:** remover o picker do canal de arranque ou a cor apagaria comportamento atualmente funcional. GuideLoadGate já tem comentário honesto. LocalSync oculto no menu ainda tem consumidor no onboarding.
3. **Candidato descartado nesta própria reanálise:** suspeita de falta de validação de um campo EPG opcional em AddSourceScreen. O campo visível foi removido (`:502–503`); EPG é gerido separadamente e EpgSourcesScreen valida a entrada (`:449–452`). Não inventar esse defeito nem voltar a expor o campo.
4. **Inferências não são prova:** scope vivo não é automaticamente leak; AsyncImage sem size explícito não prova bitmap 4K; polling ativo de 50 ms não é recomposição por frame; ausência de trace não permite prometer 2×, 4× ou X%.
5. **Pausa é intenção válida:** não abortar tentativas só por playbackRequested=false. Corrigir a recuperação mantendo pausa e cancelamento.
6. **Compatibilidade não deve ser global:** non-IDR/access-unit pode ser útil para exceção específica, mas ativá-lo universalmente não demonstra correção de lipsync e pode aceitar vídeo incompleto.
7. **Preservar conteúdo HLS:** não cortar segmentos por ultrapassarem alvo de buffer, partilhar arrays mutáveis entre sockets ou antecipar streams/decoders dos canais vizinhos por defeito.
8. **Não mascarar recusas do fornecedor:** sem timestamps/tokens aleatórios, retries agressivos, User-Agent como cura universal, reconstrução de Exo ou delays globais sem medição.
9. **Não quebrar dados/compatibilidade:** sem esquema upstream sobre o 51, retirada de credenciais dos backups cifrados, mudança unilateral de Base64, eliminação de Room KTX/rollback ou bloqueio global de HTTP IPTV.
10. **Sem expansão contrária ao produto:** não repor smartphone, filmes/séries, favoritos/histórico, OpenSubtitles, brilho ambiente, meteorologia ou animações. Não alterar o volume interno de 100%.

## 9. Ordem recomendada para uma autorização futura

Não iniciar a antiga fase 6 inteira nem as MED/NOV em bloco. A revisão recomenda:

| Lote | Âmbito delimitado | Condição de saída |
|---|---|---|
| A — resíduos funcionais | R1 URLs de diferidos; R4 invalidação do zapping; R3 barreira Exo→timeshift local. R5 Home pode acompanhar em alteração separada. | Testes dirigidos aos cenários descritos, compilação/lint adequados e snapshot coerente se Core mudar. Não alterar offsets, catálogo a cada seta ou gravações. |
| B — integridade residual | R2 omissões de backup e R6 invalidação da aprendizagem por revisão. | Resultados completos/parciais inequívocos e evidência de revisão; preservar legado, HLS estrito e exceções por canal. Não adiar R2 para depois de limpeza ou novidades. |
| C — distribuição | INT-08/09. **Antecipar antes de qualquer nova distribuição.** | Versão/certificado/package/ABIs do artefacto autorizado e inputs confiáveis; compilação e testes locais continuam possíveis sem secrets. |
| D — serviços e autorização | INT-06 e INT-07, em alterações separadas por autoridade. | PIN legado, limites partilhados, revogação no pedido seguinte, cliente limitado e staging/resultados/temporários corretos. |
| E — custo e limpeza | OPT-01/02 apenas nos consumidores atuais. | Medição do custo, regressões dos percursos preservados, consumidores mortos conferidos e nenhum contrato de dados removido. |
| Transversal | OPT-03 desde o primeiro lote; aceitação física por funcionalidade. | Distinguir alcance da opção, persistência e valores aplicados ao motor; ensaios HDMI/decoder/comando na box. |
| Posterior opcional | MED por evidência; NOV-01/02 antes de novas expansões, se desejadas. | Requisito e orçamento próprios, sem converter hipótese em bug. |

Não atribuir ganhos percentuais: faltam medições comparáveis na mesma box/lista/rede. Testes automáticos não validam fecho da sessão no fornecedor, compatibilidade efetiva do áudio, consumo de RAM ou fluidez percebida.

## 10. Referências primárias que sustentam as restrições

- Android: [versionamento](https://developer.android.com/studio/publish/versioning) e [assinatura da aplicação](https://developer.android.com/studio/publish/app-signing). A distribuição deve preservar identidade de assinatura e progressão de versão; uma chave de debug não substitui a chave autorizada.
- GitHub: [uso seguro de Actions](https://docs.github.com/en/actions/reference/security/secure-use), para permissões mínimas e fixação por SHA de commit.
- Gradle: [verificação de dependências](https://docs.gradle.org/current/userguide/dependency_verification.html). Uma lista de checksums gerada sobre o que acabou de ser descarregado exige revisão de confiança; não autentica automaticamente a origem.
- Media3: [troubleshooting do ExoPlayer](https://developer.android.com/media/media3/exoplayer/troubleshooting). Flags MPEG-TS para access units/non-IDR têm efeitos e compromissos; não justificam ativação global como cura de áudio.

**Entrega desta intervenção:** esta reanálise e anotação do plano original. Nenhuma das correções propostas foi aplicada ao código nesta análise.
