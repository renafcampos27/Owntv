# Revisão dos lotes 1 a 3 — 12/10/2026

## Reverificação após as correções — 12/10/2026

As duas falhas F1/F2 descritas na revisão inicial abaixo estão corrigidas no código atual. A análise inicial foi preservada como histórico; as suas recomendações de correção já não estão pendentes.

- F1: geração do preview e Job cancelável abrangem a espera e resolução da URL; a transição bloqueia novos previews; as verificações após IO rejeitam resultados antigos. Cliques repetidos usam a geração de abertura mais recente. A Shell valida essa geração antes de abrir fullscreen e limpa o estado de transição quando necessário.
- F2: HeroPreviewEngine usa um ConnectionPool próprio. A paragem cancela os seus pedidos e já não limpa o pool partilhado de streaming. A limpeza do pool próprio é condicionada aos recursos da sessão.
- Os novos testes exercitam resolução tardia, abertura durante a espera de 520 ms, mudança de seleção, cliques repetidos durante libertação, falha de confirmação e limpeza do estado de transição. O teste de isolamento usa sockets HTTP locais e verifica reutilização da ligação de outro leitor.
- Executados nesta reverificação: HomePreviewHandoverTest (6 testes) e HeroPreviewAndSubtitleLifecycleTest (6 testes), total de 12, todos passaram; zero falhas, erros ou testes ignorados. Compilação debug necessária para estes testes passou. Não se repetiu a compilação/lint release da revisão inicial.
- Snapshot Core usado: `0c7aafa32d8884da969c519f5b2fa89300809014a76296b4abad283661b1c11f`. HeroPreviewEngine e o teste de ciclo de vida incluídos no snapshot correspondem aos ficheiros do Core de trabalho.

Conclusão atual: as pendências identificadas nesta revisão foram implementadas e passaram os testes dirigidos. Isto não garante o término de sessões remotas no fornecedor nem demonstra resolução de todos os bloqueios A–B–A. Não foi gerado APK nem testada uma box. Os restantes resultados abaixo são da revisão inicial, não uma nova execução dos testes dos lotes 1/2.

## Âmbito e conclusão

Revisão do plano ajustado fornecido pelo utilizador: gravações; memória dos backups e permissões do Companion; sincronização M3U, Home preview e ciclo de vida. O lote opcional de sincronização local e lembretes está excluído.

Código analisado: `C:/Users/renat/Downloads/OwnTV-main` e `C:/Users/renat/Downloads/OwnTV_Core`. Não foram alterados leitores, definições ou comportamento da aplicação nesta revisão. Este relatório é o único ficheiro criado.

| Lote | Resultado |
| --- | --- |
| 1 — Gravações | Implementação coerente com o plano; testes dirigidos passaram. Validação física de armazenamento/remux ainda necessária. |
| 2 — Backups e Companion | Implementação coerente com o plano; testes de encriptação, streaming e permissões passaram. |
| 3 — M3U e Home | M3U, ciclo de vida e limpeza de desenho implementados. A transição do preview para o leitor principal precisa das correções abaixo. |

## Falhas encontradas

### F1 — Prioridade alta: abertura do canal não invalida o preview ainda pendente

Referências: `HomeViewModel.kt:280`, `:291`, `:318`; `HomeScreen.kt:318` e `:526`.

`stopPreview()` para o motor, mas não invalida a operação que ainda resolve a URL. `startPreview()` resolve a URL em IO e depois chama `heroPreviewEngine.play()` sem confirmar que a seleção continua válida. A abertura do canal só consulta/paralisa a sessão já conhecida do motor. O produtor de preview também pode estar na espera de 520 ms quando o utilizador abre o canal.

Consequência possível: uma resolução antiga termina durante a transição e tenta iniciar o preview depois de o leitor principal começar a sintonizar. A proteção de sessão única reduz alguns casos, mas está dentro de `play()`, depois da resolução; uma resolução Stalker pode já ter emitido um pedido ao portal. Isto é uma condição de corrida identificada no código, não uma reprodução medida numa box.

Correção proposta:

1. Ao abrir o canal, invalidar a geração do preview e cancelar o trabalho pendente antes de aguardar o fecho da sessão.
2. Impedir novos previews enquanto decorre essa transição.
3. Depois da resolução em IO, confirmar geração, seleção e autorização de preview antes de publicar/iniciar reprodução.
4. Em cliques repetidos, só a seleção mais recente pode abrir o leitor.
5. Preservar o fecho confirmado dos pedidos efetivamente abertos; não substituir esta coordenação por um atraso fixo.

Teste necessário: suspender a resolução do preview A, pedir abertura de B, concluir A e verificar que A não abre nem publica estado tardio. Os testes atuais do motor não exercitam este produtor assíncrono de HomeViewModel/HomeScreen.

### F2 — Prioridade média: parar o preview limpa ligações ociosas de outros leitores

Referências: `HeroPreviewEngine.kt:325–337`; `core/network/StreamingHttpClient.kt:27–49`.

`stopInternal()` chama sempre `streamingHttp.evictAllAsync()`, mesmo sem sessão própria. O cliente de preview deriva do cliente partilhado de reprodução, sem pool exclusivo. O mesmo serviço é usado pelos outros leitores internos. A Home chama `stop()` também durante mudanças de seleção e quando suprime o preview.

Consequência: pode descartar ligações ociosas reutilizáveis de outros leitores, obrigando a novas ligações e negociações. Não termina pedidos ativos; não constitui prova de cortes de áudio. Este pool é de streaming, separado do cliente de EPG/API/imagens.

Correção proposta: cancelar e aguardar apenas os pedidos pertencentes ao preview; evitar a limpeza global numa paragem sem recursos próprios. Se for necessário fechar sockets por compatibilidade com um fornecedor, usar uma estratégia limitada ao preview, por exemplo um pool dedicado e reutilizável, com libertação explícita.

Teste necessário: manter uma ligação ociosa de outro leitor, parar um preview sem sessão e confirmar que a ligação continua reutilizável; confirmar em separado que os pedidos do preview são fechados.

## O que está corretamente implementado

### Lote 1

- A finalização já não fica dependente do Job cancelado da captura. Tem controlo próprio, ligado à atividade do scope do motor por vigilância de cancelamento.
- Parar/guardar aguarda a captura e a finalização; apagar cancela ambas e só depois remove os ficheiros.
- Pausa de download conserva o checkpoint e o spool; shutdown durante captura/finalização conserva recuperação e marcação de incompleto.
- Os testes usam o RecordingEngine real com dependências controladas e cobrem parar/guardar, apagar durante remux, pausa, recuperação e shutdown.

### Lote 2

- Desencriptação usa offset/length sem a cópia intermédia do payload; valida limites e autenticação GCM.
- Download de backup usa streaming de ficheiro para a saída, sem carregar o ficheiro inteiro nessa rota.
- Matriz explícita por modo mantém upload/download em LOCAL_SYNC e conserva autenticação.
- Esta melhoria reduz cópias; não torna toda a importação/exportação um processo de memória constante.

### Lote 3

- M3U recebe o âmbito de conteúdo selecionado; modo só TV evita processamento de filmes/séries e o cálculo de legacyStableKey quando não existe lookup.
- Preview passa pelo resolvedor e prefere HLS; headers e User-Agent da origem são preservados.
- A confirmação de fecho e a exclusão da sessão do próprio preview estão presentes. Falta cobrir o trabalho anterior à sessão, como descrito em F1.
- Stop reutilizável e dispose/release definitivo estão separados nos scopes dos motores.
- Paint/BlurMaskFilter são reutilizados e o fundo desfocado duplicado durante o overlay foi removido. Isto não demonstra ganhos específicos de FPS; continuam a existir animação de largura e outros efeitos de blur em contextos diferentes.

## Validação

- Snapshot Core efetivamente usado: `65764e6f2f79595a86da33fad37cf2a64ab3e551cb687b7c727045576a3c8318`.
- 58 testes dirigidos passaram, sem falhas: RecordingEngineOperationsTest, CompanionHttpServerTest, M3uSyncerLiveOnlyTest, BackupContainerMemorySafetyTest e HeroPreviewAndSubtitleLifecycleTest.
- Compilação Kotlin da variante standardRelease e lint vital: ambos passaram (`BUILD SUCCESSFUL`, 5 min 56 s). Este lint verifica os erros fatais da variante; não equivale a uma auditoria completa de todos os avisos.
- Não foi gerado APK nem realizado teste numa box. SAF real, remux nativo, fornecedores com sessão única e fluidez física continuam por validar.

## Ordem recomendada

Corrigir F1, acrescentar o teste da transição assíncrona, corrigir F2 e acrescentar o teste de isolamento das ligações. Depois repetir os testes dirigidos e validar numa box: abrir canal durante resolução de preview, navegar rapidamente entre itens, abrir/parar novamente canais e testar gravação/paragem/apagamento com armazenamento real.

## Lote 4 — Sincronização Local (B3) e Lembrete de Programa no EPG (I3) — 12/10/2026

### B3 — Sincronização Local (NSD e Diretórios de Exportação)

- **Diretórios temporários separados (`LocalSyncManager.kt`)**:
  - `startHosting()` exporta agora para `cache/local_sync/outgoing_host/backup.own` (`OUTGOING_HOST_DIR`) e `send()` exporta para `cache/local_sync/outgoing_push/backup.own` (`OUTGOING_PUSH_DIR`), através de `prepareOutgoingDir(baseCacheDir, subdir)`.
  - Enviar definições para um dispositivo emparelhado enquanto o modo de alojamento está ativo deixa de apagar/substituir o contentor servido em `/backup.own` (e vice-versa).
- **Remoção de dispositivos perdidos em NSD (`LocalSyncDiscovery.kt`, `LocalSyncManager.kt`, `LocalSyncViewModel.kt`)**:
  - `DiscoveryCoordinator<T>` gere fila pendente, resolução ativa e serviços resolvidos sob sincronização explícita.
  - `onServiceLost` no `NsdManager.DiscoveryListener` remove serviços ainda na fila antes de `resolveService`, suprime emissões tardias se o serviço desaparecer durante a resolução e invoca `onServiceLost(serviceName)` para serviços já emitidos.
  - `LocalSyncViewModel.beginPairing()` usa `upsertDiscoveredDevice` e `removeLostDevice` para atualizar anúncios repetidos e remover da lista `found` dispositivos que saiam da rede.

### I3 — Lembrete de Programa no EPG com Confirmação

- **Agendamento e isolamento por perfil (`ProgrammeReminderStore.kt`, `EpgViewModel.kt`, `GuideCore.kt`, `EpgScreen.kt`)**:
  - `ProgrammeDetailDialog` apresenta ação de agendar/cancelar lembrete para programas futuros (`programme.startMs > nowMs`), com navegação D-pad nativa no comando TV.
  - Lembretes são isolados por `profileId`, limpam automaticamente entradas expiradas (`endMs <= nowMs`) e disparam na janela `startMs - 60_000L .. endMs`.
  - Se o utilizador já estiver a ver em direto o canal do lembrete (no live edge), o lembrete é consumido silenciosamente sem abrir diálogo.
- **Confirmação explícita e proteção de reprodução ativa (`ProgrammeReminderDialog.kt`, `OwnTVShell.kt`)**:
  - Quando o lembrete vence, `OwnTVShell` exibe `ProgrammeReminderDialog` com as opções **Ver canal** (sintoniza o canal e consome o lembrete) e **Ignorar** (descarta o lembrete mantendo a reprodução/ecrã atual).
  - Nunca sintoniza automaticamente sem confirmação do utilizador. Quando `shouldProtectCurrentPlayback` deteta diferido/arquivo (`isArchivePlayback`), gravação/cópia local (`isTimeshiftPlayback`), timeshift recuado (`!liveNow`) ou VOD (filme/série), o foco inicial recai em **Ignorar** e o diálogo exibe aviso explícito antes de interromper a sessão atual.

### Validação do Lote 4

- **Testes unitários adicionados e aprovados**:
  - `OwnTV_Core`: `LocalSyncB3Test` (5 testes — isolamento `outgoing_host` vs `outgoing_push`, limpeza seletiva por subdiretório, `DiscoveryCoordinator` para serviço perdido em fila, perdido durante resolução e perdido após resolução).
  - `OwnTV-main`: `LocalSyncDiscoveryStateTest` (2 testes — `upsertDiscoveredDevice` e `removeLostDevice`) e `ProgrammeReminderStoreTest` (5 testes — janela de disparo/isolamento por perfil, consumo silencioso no mesmo canal em direto, manter confirmação em diferido no mesmo canal, descartar/consumir sem repetição e `shouldProtectCurrentPlayback`).
- **Verificações i18n e Snapshot Core**:
  - `verifyI18nLiterals` aprovado em `OwnTV_Core` e `OwnTV-main` (strings adicionadas em `values`, `values-pt` e `values-pt-rPT/epg_reminders.xml`).
  - Snapshot Core sincronizado: 959 ficheiros, 3 956 526 bytes, SHA-256 `7e61750f114d60c1fffc06f2f0f2f83bfa9ca2e9fe007c9c24c7405a4257a483`.

