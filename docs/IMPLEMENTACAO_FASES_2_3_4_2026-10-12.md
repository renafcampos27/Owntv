# Fases 2, 3 e 4 — configurações, comando, backup e diagnóstico

Data: 12/10/2026. App Android TV: `C:\Users\renat\Downloads\OwnTV-main`; Core personalizado: `C:\Users\renat\Downloads\OwnTV_Core`. Mobile intocado. Continuação da Fase 1 concluída em 11/10.

## Fase 2 — integridade das configurações

- Ajustes em lote partem das opções efetivas reconciliadas com o legado TS/Exo dentro da mesma transação DataStore. Só os campos escolhidos são alterados; só o legado dos canais alvo é migrado.
- Editor individual distingue carregamento de ausência real. Aguarda leitura válida, identifica o draft por perfil/fonte/canal e conserva edições locais perante emissões seguintes.
- Mudança de perfil fecha a seleção antiga. Guardar/aplicar transportam o perfil esperado; a comparação com o perfil ativo ocorre dentro de DataStore.edit antes de qualquer escrita.
- Exo Live usa revisão independente de zoom: uma leitura antiga não substitui a escolha mais recente no HUD, nem o ajuste de áudio bloqueia o restauro legítimo de zoom.

## Fase 3 — seleção completa e comando

- Lote separado da pesquisa limitada usada para escolher um canal de arranque. Nova projeção SQL completa e leve contém identidade, nome e categoria; não transporta URLs nem outros dados de reprodução.
- Visibilidade, categorias ocultas/adultas e fonte do perfil são resolvidas antes da seleção. Categoria e pesquisa trabalham sobre o conjunto completo; exclusões permanecem ao mudar filtros.
- A apresentação inicia com 100 linhas e oferece carregar mais. O limite visual não limita “Selecionar todos” nem os alvos escritos. A confirmação captura os alvos e opções mostrados.
- Cabeçalho informa fonte, total, visíveis, categoria, pesquisa, correspondências e exclusões.
- Seletor genérico posiciona LazyListState na opção atual antes de pedir foco. Em pesquisa, foco inicial continua na pesquisa; lista vazia usa Fechar. Reutiliza pedidos limitados por frames e guarda de entrada do utilizador, sem espera arbitrária de 80 ms nem roubo tardio de foco após comando/toque.

## Fase 4 — backup e clareza

- Exportação remota tem job/geração e propriedade do servidor. Fechar/substituir invalida antes de cancelar. Conclusão antiga não inicia servidor nem publica estado, e não para uma sessão posterior.
- Cada exportação remota usa diretório UUID privado. O nome fixo do ficheiro Core deixa de permitir que uma escrita antiga sobrescreva o backup novo já servido.
- Ficheiros cancelados encriptados ficam no cache privado até limpeza Android/app; não são servidos automaticamente. Uma escrita bloqueante já iniciada pode terminar, e o registo histórico Core pode indicar essa escrita concluída, sem reativar o painel/servidor.
- Diagnóstico publica CPU por janela real, heaps Java/nativo separados e GC no ciclo de recolha de dez segundos já existente. Contadores indisponíveis/reiniciados mostram N/D; não há novo polling nem soma apresentada como memória total.
- Informação do canal em direto inclui origem das preferências: Canal/Lista/Geral para motor, formato, atraso, reserva e pré-buffer. Os valores vêm do contexto da sintonia/resolvedores existentes; motor/formato efetivamente usados continuam nas linhas próprias. Origem exige chave do conteúdo correspondente; não inventa origem para pedidos sem identidade. Preview não herda o pin do canal fullscreen anterior.
- Recursos EN/PT/PT-rPT adicionados e inventários i18n atualizados apenas para SQL, logs, chaves e vazios técnicos revistos.

## Validação

Validação concluída:

| Módulo | Testes | Falhas/erros/ignorados |
|---|---:|---:|
| Core | 1 156 | 0 |
| Leitor | 520 | 0 |
| App | 357 | 0 |
| Total | **2 033** | **0** |

- Compilação StandardRelease e lintVitalStandardRelease aprovados na verificação final: **BUILD SUCCESSFUL em 42 s**.
- A execução integrada inicial terminou os testes e a compilação, mas identificou PluralsCandidate num texto novo do lote. As contagens foram reformuladas como etiquetas independentes EN/PT/PT-rPT; a verificação final passou sem suprimir o lint nem criar baseline para o erro.
- Verificações i18n aprovadas; permanecem avisos existentes de funcionalidades Gradle descontinuadas. Não foi feita migração para Gradle 10.
- Snapshot usado nas execuções: 954 ficheiros, SHA-256 `b1366e8df0bf8e5888087a26b35d728458140e0f2ba88a56a971a94e9f41893d`.
- Logs: `C:\Users\renat\AppData\Local\Temp\owntv-phase234-validation.log` (testes/integrado) e `C:\Users\renat\AppData\Local\Temp\owntv-phase234-release-final.log` (release final aprovada).

Validação especializada anterior ao build central:

- Query SQL real executada em SQLite: fonte 750 canais, categoria posterior aos primeiros 500 com 250, apresentação de 100 e seleção de 248 após duas exclusões; outra fonte isolada.
- Nove testes Kotlin/JUnit isolados de diagnóstico aprovados (sete novos e dois existentes).
- Testes novos: quatro de patch legado, dois de seleção completa, oito de backup concorrente, sete de métricas e quatro de origem das preferências.
- Revisão independente encontrou e corrigiu a guarda de perfil fora da transação e o pin indevidamente herdado em preview.

## Aceitação em Android TV pendente

1. Abrir editor com leitura de opções atrasada; alterar só áudio e confirmar que motor/formato/buffer permanecem. Trocar perfil e confirmar ausência de escrita antiga.
2. Alterar zoom imediatamente após abrir canal e confirmar que o restauro não o reverte.
3. Fonte >500 canais; selecionar categoria tardia, procurar, excluir e aplicar. Confirmar contagem e alcance real.
4. Abrir seletores nos valores 10/20/50; validar opção visível/focada, pesquisa vazia, OK, Voltar e entrada rápida antes do foco automático.
5. Fechar backup durante preparação; iniciar outro; confirmar que só a sessão atual apresenta PIN/QR e serve o ficheiro correto.
6. Confirmar legibilidade de CPU/heap/GC e origens na box; não inferir ganhos de desempenho destas medições.

Sem APK, commit ou push. Volume da app a 100%, HLS, configurações por canal, diferidos, gravações e Multiview preservados. Sem alteração global de buffers, delays, User-Agent ou reconnects. Testes JVM/SQLite/compilação não substituem validação de foco, MediaCodec, AudioTrack e ciclo de vida numa box física.

## Lote 4 — Sincronização Local (B3) e Lembrete de Programa no EPG (I3)

- **B3 — Sincronização Local (`LocalSyncManager.kt`, `LocalSyncDiscovery.kt`, `LocalSyncViewModel.kt`)**:
  - `startHosting()` e `send()` usam subdiretórios independentes (`cache/local_sync/outgoing_host/backup.own` vs `cache/local_sync/outgoing_push/backup.own`) através de `prepareOutgoingDir`, impedindo que um envio para dispositivo emparelhado apague ou substitua o contentor servido pelo servidor HTTP de emparelhamento ativo.
  - `NsdManager.DiscoveryListener.onServiceLost` está ligado através de `DiscoveryCoordinator` com canal único de eventos (`Found`/`Lost`) entregue na coroutine coletora, proteção contra anúncios duplicados em resolução e recuperação da fila se `resolveService` lançar exceção síncrona.
- **I3 — Lembrete de Programa no EPG com Confirmação (`ProgrammeReminderStore.kt`, `ProgrammeReminderDialog.kt`, `EpgViewModel.kt`, `GuideCore.kt`, `EpgScreen.kt`, `OwnTVShell.kt`)**:
  - Botão de agendar/cancelar lembrete adicionado a `ProgrammeDetailDialog` para programas futuros (`programme.startMs > nowMs`), com recursos i18n em `values`, `values-pt` e `values-pt-rPT/epg_reminders.xml`.
  - O relógio de avaliação em `OwnTVShell` corre apenas em primeiro plano via `guideLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)`, recalculando imediatamente ao regressar de `ON_STOP` para `ON_START`.
  - `canShowReminderDialog`, `resolveActiveLiveEdgeChannelId` e `evaluateDue` exigem ciclo de vida ativo (`isAtLeast(Lifecycle.State.STARTED)`), e o consumo silencioso em direto exige ainda reprodução ativa no motor (`dockedPlaying`), evitando avaliação, apresentação de diálogos ou consumo silencioso em segundo plano.
  - Em diferido/arquivo, gravação/cópia local, timeshift recuado ou VOD (filme/série), `shouldProtectCurrentPlayback` coloca o foco inicial em **Ignorar** e apresenta aviso explícito antes de interromper a sessão atual.
  - Nota de âmbito: `ProgrammeReminderStore.shared` mantém os lembretes em memória durante a sessão da app (não persistem após terminar/reiniciar o processo).
- **Validação Lote 4**:
  - `LocalSyncB3Test` (8 testes em `OwnTV_Core`), `LocalSyncDiscoveryStateTest` (2 testes em `OwnTV-main`) e `ProgrammeReminderStoreTest` (8 testes em `OwnTV-main`).
  - `check_hardcoded_strings.py verify --bootstrap` aprovado em `OwnTV_Core` e `OwnTV-main`.
  - Snapshot Core sincronizado: 959 ficheiros, 3 957 438 bytes, SHA-256 `d28481a63cb0bbb73e153531d0841d98cd91c8275af001ea5f6a2f56df1075b0`.

