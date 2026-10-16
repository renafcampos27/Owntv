# Revisão independente do lote 4 — 12/10/2026

## Reverificação após as correções — 12/10/2026

As pendências F1/F2 da revisão inicial abaixo estão corrigidas no código atual. A revisão inicial foi preservada como histórico.

- F1: snapshot atualizado para `d28481a63cb0bbb73e153531d0841d98cd91c8275af001ea5f6a2f56df1075b0`; a configuração normal com o Core local passou a verificação de integridade. Os testes adicionais de descoberta NSD agora fazem parte do Core validado.
- F2: o relógio está dentro de repeatOnLifecycle(STARTED); o observador da Shell atualiza o estado de ciclo de vida; apresentação e avaliação são bloqueadas em background. O consumo silencioso exige também reprodução efetivamente ativa pelo motor. Ao voltar à app, a avaliação usa a hora atual.
- Executados na reverificação: LocalSyncB3Test (8), LocalSyncDiscoveryStateTest (2), ProgrammeReminderStoreTest (8): 18 testes, zero falhas/erros/ignorados. Os testes novos exercitam bloqueio de avaliação/consumo após ON_STOP e avaliação após ON_START, bem como a necessidade de o motor estar efetivamente a reproduzir. São testes unitários de lógica, não instrumentação da Activity numa box.
- Compilação Kotlin standardRelease e lint vital: ambos passaram, BUILD SUCCESSFUL em 5 min 11 s na execução conjunta com os testes. A configuração normal com o Core local foi usada, sem forçar o snapshot nem desativar verificações de integridade.
- A limitação de persistência dos lembretes mantém-se: só existem durante a sessão do processo.

Não foram alterados código de comportamento, configuração de build ou snapshot nesta reverificação; apenas este relatório foi atualizado. Não foi gerado APK nem testada uma box física.

Conclusão atual: as duas pendências identificadas estão resolvidas e o lote 4 pode ser considerado concluído no âmbito do plano e da validação automática. A validação física de NSD, suspensão e foco D-pad continua por realizar.

## Veredicto

As funcionalidades do lote 4 existem: pastas separadas para alojamento/envio, tratamento de serviços perdidos na rede e lembretes EPG com confirmação. Não considerar o lote completamente fechado: a compilação com o Core local está bloqueada por divergência do snapshot, e o avaliador de lembretes não respeita o estado de primeiro plano.

Revisão de leitura; nenhuma correção de comportamento aplicada. Este relatório foi criado para registar o resultado.

## F1 — P1: Core de trabalho e snapshot da app não correspondem

`core-source-lock.json` aponta para o snapshot `7e61750f114d60c1fffc06f2f0f2f83bfa9ca2e9fe007c9c24c7405a4257a483`.

`LocalSyncDiscovery.kt` no Core de trabalho contém alterações ausentes desse snapshot: eventos Found/Lost no mesmo canal, callback de perda na coroutine coletora, publicação ordenada sob coordenação, proteção contra anúncios duplicados durante resolução e avanço da fila quando resolveService lança uma exceção síncrona. Os testes do Core de trabalho também têm casos adicionais ausentes do snapshot.

O PC tem `owntv.corePath=C:/Users/renat/Downloads/OwnTV_Core` em gradle.properties global. A compilação com esse override falhou antes dos testes com:

> Customized Core changed: core/src/main/java/tv/own/owntv/core/sync/local/LocalSyncDiscovery.kt.

O bloqueio é uma proteção correta da compilação. Não desativá-la. Validar o Core de trabalho e regenerar o snapshot/lock com a ferramenta existente. Uma compilação forçada a usar apenas o snapshot passa, mas utiliza a versão anterior da descoberta NSD.

## F2 — P2: relógio e avaliação de lembretes não estão limitados ao primeiro plano

`OwnTVShell.kt:407–416` usa produceState com um ciclo de cinco segundos. A chave guideLifecycle é o objeto Lifecycle; não existe repeatOnLifecycle nem verificação do seu estado nesse ciclo. `reminderCanShow`, em :421, também não exige a Activity ativa.

Enquanto a composição continua viva após Home/suspensão, o trabalho pode continuar, avaliar lembretes e preparar diálogos em segundo plano. O consumo silencioso também depende de flags do leitor, sem confirmar aqui que a reprodução continua efetivamente ativa. A MainActivity para os leitores em onStop, mas não cancela este avaliador.

Correção recomendada: executar o relógio com repeatOnLifecycle e permitir apresentação/consumo apenas no estado adequado de primeiro plano; recalcular imediatamente ao regressar. Adicionar teste com ON_STOP/ON_START ou RESUMED/PAUSED para confirmar ausência de consumo/apresentação em background e retoma correta.

Referência: https://developer.android.com/topic/libraries/architecture/coroutines

## Implementações corretas

- LocalSyncManager usa outgoing_host e outgoing_push, limpando apenas a pasta da operação; envio deixa de apagar o ficheiro servido pelo alojamento.
- DiscoveryCoordinator remove serviços perdidos da fila, suprime resultados de uma resolução cancelada e notifica a perda de dispositivos já anunciados. LocalSyncViewModel remove o dispositivo correspondente.
- Lembretes podem ser agendados/cancelados para programas futuros, estão isolados por perfil e expiram.
- A mudança de canal exige clicar em Ver canal; não existe sintonia automática ao vencer o lembrete.
- Em diferido, timeshift recuado ou reprodução local, o diálogo avisa e coloca o foco inicial em Ignorar. Agendar/apresentar não cancela uma gravação.
- O popup automático não se regista como janela do utilizador, evitando que a sua própria presença o esconda repetidamente.

## Limitação funcional, não incumprimento do plano

ProgrammeReminderStore.shared guarda os lembretes apenas em memória. São perdidos quando o processo termina/reinicia; não há persistência, inclusão em backup ou alarme de sistema. O plano pediu avaliação/introdução de lembretes com confirmação, sem exigir persistência. Deve ficar claro que esta implementação funciona durante a sessão da app. Para lembretes duráveis é necessário um lote próprio com armazenamento e política de apresentação após retoma.

## Validação efetivamente realizada nesta revisão

- Snapshot: LocalSyncB3Test, 5 testes aprovados.
- App: LocalSyncDiscoveryStateTest, 2 testes aprovados; ProgrammeReminderStoreTest, 6 testes aprovados.
- Total: 13 testes, zero falhas/erros/ignorados; compilação debug necessária passou.
- Compilação/testes com override do Core de trabalho: bloqueados pela divergência de LocalSyncDiscovery.kt; não contar os testes adicionais desse Core como executados nesta revisão.
- Não foram gerados APKs, executado lint release nem testados NSD, foco D-pad ou standby numa box física.

## Próxima ordem recomendada

1. Validar as alterações mais recentes do Core e sincronizar snapshot/lock, preservando a verificação de integridade.
2. Limitar o avaliador dos lembretes ao primeiro plano e acrescentar o teste de ciclo de vida.
3. Repetir testes e validar numa box a perda/regresso de dispositivos na rede, envio durante alojamento e lembretes durante direto/diferido/suspensão.

## Resolução aplicada (12/10/2026)

- **F1 resolvido**: `vendor/owntv-core-source.zip` e `core-source-lock.json` regenerados com `tools/core_snapshot/snapshot_core.py --core C:/Users/renat/Downloads/OwnTV_Core` (959 ficheiros, 3 957 438 bytes, SHA-256 `d28481a63cb0bbb73e153531d0841d98cd91c8275af001ea5f6a2f56df1075b0`), incluindo as últimas correções de `LocalSyncDiscovery.kt` e os 8 testes de `LocalSyncB3Test.kt`.
- **F2 resolvido**:
  - `OwnTVShell.kt` executa agora o relógio `reminderClockMs` dentro de `guideLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED)`, suspendendo o ciclo de 5 segundos em `ON_STOP` e atualizando imediatamente o relógio ao regressar a `STARTED`.
  - `ProgrammeReminderStore.canShowReminderDialog`, `ProgrammeReminderStore.resolveActiveLiveEdgeChannelId` e `ProgrammeReminderStore.evaluateDue` exigem ciclo de vida em primeiro plano (`isAtLeast(Lifecycle.State.STARTED)`), o consumo silencioso exige que o motor ativo (`dockedPlaying`) esteja efetivamente em reprodução, e a avaliação em primeiro plano mantém a poda/auto-consumo mesmo com sobreposições abertas enquanto `reminderCanShow` governa a exibição do diálogo.
  - Adicionados 2 novos testes unitários em `ProgrammeReminderStoreTest.kt` (`background lifecycle ON_STOP suppresses evaluation and auto-consumption until ON_START` e `resolveActiveLiveEdgeChannelId requires both foreground lifecycle and active engine playback`), elevando `ProgrammeReminderStoreTest` para 8 testes.

