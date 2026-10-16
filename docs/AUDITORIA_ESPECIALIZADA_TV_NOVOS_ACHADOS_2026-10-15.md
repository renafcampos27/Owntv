# OwnTV TV — nova auditoria especializada, 15/10/2026

## Âmbito e método

Análise de `C:\Users\renat\Downloads\OwnTV-main` e `C:\Users\renat\Downloads\OwnTV_Core`, com contributos de três agentes especializados em streaming, UI/EPG e definições/armazenamento, seguidos de revisão crítica pelo coordenador. Os agentes atingiram o limite de utilização durante a investigação; o coordenador completou a validação dos mecanismos principais. Não se apresenta uma segunda ronda independente de conselho como concluída.

Sem implementação, mudanças em configurações, APK, commit, contacto ao fornecedor ou alteração do projeto Mobile. Foram criados apenas este relatório e provas isoladas em diretórios temporários. Checkouts com muitas alterações anteriores: não houve reset/clean. HEAD App observado: `03e23db`; o estado não commitado é parte do código analisado. Snapshot Core observado: `7d575983924a02912495339bc8b31faf2ba1f4c00ae7aa989ee0d2be5b3367a6`.

Consultados os relatórios/implementações de 13/10, as fases A–D de 14/10 e E1/E2 para não reapresentar correções antigas como novas. P1 indica prioridade de correção, não prova de ocorrência no aparelho; os riscos condicionados são identificados expressamente. Nenhum item abaixo demonstra a causa de bloqueios do fornecedor em A→B→A.

## Decisão recomendada

Estado posterior: lotes 1 e 2 (A15-01 a A15-07) implementados e validados em 15/10/2026. Ver `IMPLEMENTACAO_LOTES_1_2_AUDITORIA_2026-10-15.md` para âmbito, limites e resultados. Lotes 3 e 4 (A15-08/09/10/12/13/14/15/16) também implementados e validados em 15/10/2026; ver `IMPLEMENTACAO_LOTES_3_4_AUDITORIA_2026-10-15.md`. O restante documento conserva a análise original; o lote 5 (A15-11) tem implementação e validação local concluídas, com aceitação física de desempenho ainda pendente; ver `IMPLEMENTACAO_LOTE_5_EPG_2026-10-15.md`.

Há correções localizadas justificadas. Não aprovar uma reescrita geral do player, da base de dados ou do modelo de threads. Priorizar domínios de backup e privacidade, coerência do guia e das definições, depois pausa/retoma. Medir antes de aplicar a otimização da grelha. E3/JNI e E4/ingestão de EPG continuam condicionadas: não foram convertidas em necessárias por esta auditoria.

## 1. Integridade e privacidade

### A15-01 — um valor legítimo das legendas torna o backup irrestaurável — P1

`OwnTV_Core/core/.../settings/SubtitleStyle.kt:26` define `OPACITY_DEFAULT=-1`. `SettingsRepository.kt:1733–1737` aceita e guarda esse valor quando se escolhe Usar predefinição; `:2819` inclui a chave no backup. Contudo, `:2945` valida `SUB_BG_OPACITY` apenas em 0..100 e a importação valida antes de escrever (`:2950`). O botão correspondente existe em `OwnTV-main/app/.../settings/VideoPlayerSettingsScreen.kt:3033`.

Trigger: escolher a predefinição do fundo de legendas, exportar Definições e restaurar. Pode bloquear o restauro de todas as secções selecionadas porque a validação ocorre antes das escritas. Não implica corrupção de ficheiros.

**Prova executada:** compilados os helpers reais `BackupValueValidator` e `SettingsBackupValidation`, usando o intervalo verificado no repository; -1 é rejeitado e 0/100 são aceites. Não foi executado roundtrip de DataStore/backup Android completo.

**Correção mínima:** permitir exatamente -1 e 0..100, distinguindo predefinição de transparente. Testar setter→exportação→JSON→importação, mais -2/101 recusados.

### A15-02 — ocultação de segredos não é uniforme nos pedidos de catálogo — P1

`OwnTV_Core/core/.../network/HttpClient.kt:45,58,108–109` usa um redator privado que só cobre algumas chaves de query. O helper público `redactUrl:144–151` também cobre credenciais no caminho Xtream, userinfo e outras chaves assinadas. `:65–76` escreve headers em nível info; `:93–97` escreve mensagem e Throwable bruto. Assim, um endereço com credenciais no caminho ou token noutra chave pode chegar a logs/erros sem a ocultação mais completa já existente.

**Correção:** unificar a política de ocultação para os caminhos de catálogo e rever mensagens/cadeias de exceção, não apenas o prefixo do log. Conservar status/tipo/duração úteis; evitar Throwable bruto com segredos. Uma expressão genérica não garante cobrir todos os tokens arbitrários: usar nomes de campos estritamente necessários e origem sem caminho/query quando não houver forma segura de os mostrar.

**Aceitação:** URLs sintéticas com query, caminho Xtream, userinfo e endereço em mensagem de IOException não revelam os segredos. Não é necessário alterar User-Agent nem os pedidos reais.

### A15-03 — associações EPG podem reintroduzir categorias ocultas — P1 de política de apresentação

`OwnTV-main/app/.../epg/EpgViewModel.kt:1038–1056` calcula as categorias ocultas, incluindo perfil infantil, e filtra `auto`. Depois `applyEpgMatches:1155–1177` resolve canais em falta e acrescenta-os novamente sem reaplicar `hiddenCatIds`. A lista final só filtra capacidade de diferido (`:1059–1060`).

Condição: associação manual/fallback guardada para um canal elegível resolvido nessa passagem, categoria ocultada depois; se Apenas diferidos estiver ativo, o canal também tem de ter catchup. Pode reaparecer nome/EPG de categoria oculta, inclusive no perfil infantil.

**Contraprova importante:** `LiveViewModel.playCatchupProgramme` e `playCatchupExternal` revalidam a política adulta. Não foi demonstrado bypass da reprodução adulta; o achado é exposição no guia.

**Correção:** aplicar a política final de elegibilidade depois de integrar associações, incluindo fontes, categorias e itens ocultos. Testar match/fallback, categoria oculta normal/infantil e reimportação com novo ID.

### A15-04 — restauro seletivo pode confundir IDs coincidentes — P1 condicionado

Em `OwnTV_Core/core/.../backup/BackupManager.kt`, perfis são associados por nome (`:858–903`) e fontes só entram no mapa quando resolvidas. Porém `remapKeys:1673–1681` e `remapEnginePinKey:1792–1797` preservam entradas numéricas sem mapping. `startupModes/startupChannels/customizePins:1141–1161` aceitam existência de IDs locais. `PlaybackDatabaseBackup.kt:36–45` também usa `sources[oldSource] ?: oldSource`.

Condição: restauro apenas SETTINGS entre instalações, perfil/lista sem correspondência por identidade, mas IDs numéricos coincidentes. Podem ser aplicados arranque, personalizações ou opções de reprodução a outro perfil/lista. A cadeia foi confirmada em código; falta a reprodução integrada. Os remaps estritos das configurações por canal já evitam esta classe de erro em algumas rotas.

**Correção proposta:** usar apenas associações resolvidas para dados dependentes de perfil/fonte. Manter tratamento específico de URLs legadas e chaves locais. Antes de mudar helpers partilhados, testar colisão de ID com identidade diferente, mesma identidade com novo ID, restauro completo e backups antigos. Não trocar todos os remaps indiscriminadamente.

## 2. Reprodução e coerência das definições

### A15-05 — pausa/retoma deixa temporizadores do ExoPlayer por rearmar — P2

`OwnTV_Core/player-core/.../LivePreviewEngine.kt:2863–2891`: pausa chama `mainHandler.clear()`. Na retoma com canal já iniciado, só rearma `progressWatchdog`; se o player continuar BUFFERING, esse watchdog exige READY (`:983–995`) e o `stallWatchdog` só é armado na mudança para BUFFERING (`:1393–1401`). Retomar sem mudar de estado não garante novo callback desse estado.

Em READY sem erro, também não se volta a agendar `healthyReset`. `recoverCurrentPlayback:1331–1350` só o faz dentro do ramo de erro/gaveUp; uma retoma normal não entra nesse ramo. O orçamento pode conservar tentativas antigas até outra transição que rearme o reset.

**Correção:** centralizar o rearmar conforme estado atual, intenção de reprodução e owner; retoma em READY agenda janela saudável, em BUFFERING iniciado agenda supervisão de stall, abertura inicial mantém o seu prazo. Não criar outra rotina de reconnect nem aumentar todos os timeouts.

**Aceitação:** READY→pausa→READY sem novo callback de estado; BUFFERING→pausa→BUFFERING; pausa longa; owner substituído; janela saudável real antes de repor orçamento. Não foi reproduzido spinner infinito numa box nem se excluem limites externos do controlador.

### A15-06 — gravações consultam opção de leitor externo de Filmes — P2

`SettingsRepository.kt:1551–1552` envia LIVE para `externalPlayerMovies`. `OwnTV-main/app/.../recordings/RecordingsViewModel.kt:164,256–257` usa `externalPlayerFor(MediaType.LIVE)`. A opção visível de TV Direto consulta outro valor.

**Efeito:** ativar o leitor externo de TV pode não se aplicar às gravações; um valor antigo de Filmes pode abri-las externamente com a opção TV desligada.

**Correção:** usar a preferência Live nos dois pontos de gravações ou tornar a matriz de MediaType explícita, preservando downloads VOD. Testar combinações Live/Movies e manter proteções de gravação em curso ou em partes. Não é correção de codec/áudio.

### A15-07 — fábrica HTTP da pré-visualização pode ficar numa geração retirada — P2 condicionado à Home

`HeroPreviewEngine.kt:301` retira a geração HTTP a cada play. O player só é reconstruído se UA/headers/decodificação mudarem (`:320–334`). A construção `:509–524` fixa uma fábrica de chamadas na geração atual. `PlaybackHttpCalls.factory:34–59` cancela novos pedidos quando o owner capturado não é a geração corrente. Portanto reutilizar a mesma fábrica na próxima preview cancela os pedidos dessa preview.

**Prova executada:** compilado o `PlaybackHttpCalls` real com backend Call sintético, sem rede: fábrica antiga cancelada após retirada, fábrica renovada aceite. Não foi instanciado/renderizado HeroPreviewEngine numa box. A Home continua referenciada em OwnTVShell; não afeta necessariamente quem usa apenas a lista e tem preview desligado.

**Correção:** renovar a fábrica/media source por geração preservando o fencing de pedidos antigos; não retirar a proteção nem reconstruir globalmente todos os leitores. Teste de duas previews sequenciais com identidade igual e também stop→play.

## 3. Guia, navegação e apresentação

### A15-08 — pesquisa no guia não é limpa no regresso após Home — P2

`EpgViewModel.kt:583–585` conserva `_query`; `setGuideActive(false):956–968` cancela trabalho mas não reinicia a pesquisa. Em contraste, `LiveViewModel.onBackground:333–338` limpa os filtros Live. Com Activity/ViewModel vivos, Home e reabrir conserva a pesquisa do guia.

**Correção:** separar background real da app de simples saída do guia ou abertura fullscreen; limpar o filtro só no evento correspondente à regra pedida. Testar Home/reabrir, processo morto, mudança de secção e retorno do diferido sem limpar indevidamente navegação.

### A15-09 — pesquisa EPG ignora nomes personalizados sem associação manual — P2

`EpgViewModel.kt:1021–1022` pesquisa `channelsWithGuide(ids,q,...)` antes de aplicar `cust.itemNames` (`:1057`). A pesquisa SQL usa o nome original. Um canal apresentado com outro nome pode não aparecer ao procurar pelo nome mostrado. A integração de matches resolve apenas casos com associações, não todos os renomeados.

**Correção:** incluir candidatos que correspondam aos nomes personalizados com elegibilidade e limites, deduplicar e só depois ordenar. Não carregar todas as listas em RAM para resolver uma pesquisa. Testar nome original/personalizado, categoria oculta, fonte inativa e limite de resultados.

### A15-10 — callbacks de edição EPG não conservam todos o mesmo owner — P2 condicionado

`EpgViewModel.setEpgShift:705–717` e `setEpgMatch:742–747` obtêm o perfil ativo na execução e aplicam um ChannelEntity recebido anteriormente, sem o contrato explícito de `setEpgFallback:733–738`. O LiveViewModel tem rotas semelhantes de edição. Se o diálogo/pedido sobreviver a uma troca de perfil/fontes, pode escrever a escolha no contexto novo.

**Correção:** snapshot de perfil/fontes/revisão ao abrir e revalidação antes de gravar; recusar operação antiga, não reaplicá-la ao perfil atual. Risco de corrida condicionado; não se alegou que uma troca normal seja possível por baixo de todos os diálogos modais. Testar troca de perfil/background/restauro enquanto a operação está pendente.

### A15-11 — guia volta a calcular os mesmos intervalos na navegação — otimização candidata, sem ganho medido

`GuideCore.kt:122` memoriza layout para desenho; `GuideProgrammeCells.at:26–29` e `adjacentTime:32–43` voltam a executar layout, que ordena/sweep dos eventos. São chamados em EpgScreen na navegação e na faixa de informação (`:1003–1005,1117,1130,1172`). O contrato é comum, mas a instância calculada não é partilhada.

**Proposta:** modelo imutável da linha com os mesmos intervalos para desenho, seleção, informação e navegação, invalidado por dados/janela/identidade. Procurar a célula por índice/busca sem nova ordenação a cada tecla. Preservar entidade e início original para diferidos. Executar medições E1/E2 antes/depois; não prometer scrolling 2x nem que isto elimina falhas de rede.

### A15-12 — aviso de alarmes exatos pode ficar desatualizado — P3 de UI

`RecordingsViewModel.kt:69–74` guarda uma capacidade calculada uma vez; `RecordingSettingsScreen.kt:131–134` mostra esse valor. Conceder permissão e voltar ao mesmo ViewModel pode manter aviso antigo. Scheduler e receiver já revalidam o sistema; não foi identificado alarme que deixe de funcionar por causa desta UI.

**Correção:** atualizar ao retomar o ecrã ou observar capacidade atual, sem polling permanente. Testar denied→granted no mesmo processo.

## 4. Limpeza e ferramentas

### A15-13 — falha ao gerar snapshot pode substituir ZIP sem atualizar o lock — P2

`tools/core_snapshot/snapshot_core.py:41–53` escreve diretamente o ZIP final antes de verificar a versão. **Prova executada** com módulo real, ROOT temporário e checkout sintético: exitCode=2, archiveChanged=true, lockUnchanged=true. Nenhum ZIP real foi alterado.

**Correção:** validar antes de escrever, publicar ZIP imutável por hash e substituir lock atomicamente, ou usar candidatos/restauro coerente. Dois renames independentes não tornam o par transacional. Testar versão inválida e falhas entre fases.

### A15-14 — atualização automática por pin não acompanha o Core por snapshot — P2 condicionado ao uso da CI

`gradle/custom-core.settings.gradle.kts` torna o ZIP/lock autoritativos. `android.yml:55,80–95,103` dispensa testes nas branches bump/core e compara versão/locale com um repo remoto; `automerge-core-bump.yml:64–80` pode aprovar catálogo/locales sem integrar código no snapshot. As exclusões i18n e comentários/GPR da resolução conservam o modelo antigo.

**Correção:** desativar ou converter o bump automático em proposta de revisão; gates devem validar os inputs reais. Remover apenas resolução/inputs/comentários comprovadamente obsoletos, preservando publicação do próprio Core. Não foi verificado se este workflow está ativo remotamente nem se algum merge ocorreu.

### A15-15 — CI App não pede explicitamente as suites Core personalizadas — P2

`android.yml:132–135` executa apenas `testStandardDebugUnitTest`; compilar o composite não executa automaticamente os testes dos dois módulos Core. A CI upstream não cobre as alterações locais embaladas no snapshot.

**Correção:** pedir explicitamente `:OwnTV_Core:core:testDebugUnitTest` e `:OwnTV_Core:player-core:testDebugUnitTest`, pelo menos quando source/lock muda. Teste intencionalmente falhado no snapshot deve bloquear aprovação. Não confundir JVM com testes SQLite/Android.

### A15-16 — fixture de desempenho exige preparação incompatível com o default Apenas diferidos — P2 de validação

`tools/performance/local_iptv_fixture.py:35–36` não fornece catchup, corretamente. O default guideReplayOnly=true elimina esses canais da grelha; o benchmark exige guia pronto. O documento E1/E2 não manda desativar esse filtro na lista sintética.

**Correção:** preparação explícita com restauro da preferência e erro claro, ou instrução manual inequívoca. Não inventar suporte de arquivo na fixture. Diferenciar teste funcional em emulador de medição de desempenho na box real.

## Inovações pequenas que fazem sentido após as correções

1. **Prévia do restauro:** mostrar quais perfis/listas foram identificados e quantas configurações serão aplicadas ou ignoradas, antes de escrever. Complementa A15-04; não exigir ao utilizador conhecer IDs.
2. **Validação de exportação pelo mesmo contrato de importação:** detectar imediatamente uma preferência que produziria um backup recusado. A primeira correção continua a ser o domínio errado de A15-01; não esconder o erro nem remover preferências silenciosamente.
3. **Sessão de diagnóstico com duração definida:** iniciar, terminar e exportar um ensaio curto com a janela e percurso identificados, reutilizando E1/E2. Apenas opcional, para facilitar recolha e evitar diagnóstico ativo por esquecimento; sem novas heurísticas de qualidade de streaming.

## Limpeza que não aprovo em bloco

- `RecordingManager.relocate:716–743` não tem consumidor encontrado na TV: pode ser marcado legado/documentado. É API pública; remover exige rever consumidores e compatibilidade. Não foi demonstrado risco de perda no percurso TV atual, porque não é chamado.
- Não apagar migrações, chaves históricas, helpers Core de VOD/legendas ou dados de backup só porque a UI TV não os mostra. Core e Mobile privado têm fronteiras diferentes.
- Não remover bibliotecas inteiras sem verificar consumidor, empacotamento nativo e reflexão. Não foi encontrada prova nova que justifique isso.
- Não aplicar flags TS/HLS globais, novos delays, mudança de buffer, teardown JNI concorrente ou reset do orçamento em qualquer READY.

## Contraprovas e limites da revisão

- O salto ao limite direito do guia foi considerado, mas o guard de EpgScreen (`next <= cursorTime -> edge`) já trata esse caso; não é incluído como bug novo confirmado.
- Categoria adulta reaparecer no guia não demonstra autorização de reprodução adulta.
- Fábrica HTTP antiga cancelada prova o mecanismo de geração, não uma recusa do servidor.
- Estado de alarme na UI desatualizado não implica scheduler errado.
- Performance medida em emulador não representa a Xiaomi/Thomson; E1/E2 prepara ensaios, não os executa.
- Três provas isoladas executadas: domínio do validador de backup, fencing de geração HTTP e falha de publicação de snapshot. Não houve novo build completo, execução das suites Android, backup real, medição física, ou pedido ao fornecedor. Os totais de testes anteriores não são apresentados como execução desta auditoria.

## Ordem recomendada para implementação futura

| Lote | Conteúdo | Condição de saída |
|---|---|---|
| 1 — integridade e privacidade | A15-01/02/03; fixture de colisão e correção A15-04 | Roundtrip válido; logs sintéticos sem segredos; filtros finais respeitados; restauro seletivo sem cruzar identidades |
| 2 — reprodução e preferências | A15-05/06/07 | Matriz de pausa/retoma, orçamento saudável e owners; gravações seguem opção visível; previews sequenciais abrem |
| 3 — coerência da interface | A15-08/09/10/12 | Pesquisa limpa só no background real, nomes personalizados pesquisáveis, callbacks antigos recusados, aviso atual |
| 4 — manutenção e validação | A15-13/14/15/16; limpeza de comentários/inputs comprovada | Snapshot recuperável, CI testa código real, fixture preparada sem alterar defaults permanentes |
| 5 — otimização medida e opções | A15-11 e inovações selecionadas | Medição repetida na mesma box, sem regressão de memória/foco/diferido; sem prometer ganho antes da comparação |

Nenhum lote foi implementado nesta análise.

## Documentação primária consultada

- [Media3: estados, intenção e eventos de reprodução](https://developer.android.com/media/media3/exoplayer/listening-to-player-events): pausa/retoma e estado de capacidade são propriedades diferentes.
- [Android: informação sensível em logs](https://developer.android.com/privacy-and-security/risks/log-info-disclosure).
- [Android: concessão de alarmes exatos](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms).
- [Android Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview): medir no hardware real e não tratar o emulador como evidência da box.

As fontes sustentam os contratos das plataformas; os defeitos concretos foram rastreados no checkout, não inferidos de relatos de terceiros.
