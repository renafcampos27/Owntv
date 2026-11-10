# UP70-B — escrita EPG e avaliação do preload

Data: 08/11/2026. OwnTV Android TV/Core personalizado. Autorização: implementar UP70-B; UP70-C não incluído.
Estado: B1 implementado, compilação/testes/lint vital aprovados; B2 avaliado e não integrado por não cumprir o gate de arranque/memória em todos os cenários.

## Decisão B1 — remover a transação redundante, manter o writer do Room

A inspeção atual corrigiu uma premissa da proposta inicial: **o DAO já prepara uma única instrução por lote**. No KSP de `EpgDao_Impl.kt`, `upsertProgrammes` chama `performSuspending(__db, false, true)` e o `EntityInsertAdapter.insert(connection, Iterable)` do Room 2.8.5 prepara uma vez, faz bind/step/reset por programa e fecha com `use`.

O custo evitável em `core/src/main/java/tv/own/owntv/core/repository/EpgRepository.kt`, função local `storeProgrammes`, era:

```kotlin
// Antes
// db.transaction { epgDao.upsertProgrammes(batch) }

// Agora: uma única transação controlada pelo DAO.
epgDao.upsertProgrammes(batch)
```

O `internalPerform` do Room abre a transação IMMEDIATE, sincroniza o tracker de invalidação antes da escrita e agenda `refreshAsync` após o commit exterior. A chamada direta elimina o savepoint adicional do wrapper e conserva a publicação dos resultados para queries/Flows do guia. Não se introduziu outro writer, notificação manual ou cópia do bind SQL.

**Alteração de produção estritamente necessária:** substituir a transação exterior pela chamada direta e explicar a decisão no comentário. Mantido `ensureSource()` antes de cada lote. Permanecem o IO dispatcher, mutex por origem, parser completo, janela de retenção, cache temporária/promovida só após parse completo e prune protegido. Não se alteram os oito campos persistidos, `nullif(id,0)`, REPLACE, IDs, hashes, descrições nulas ou horários. `guideShiftMs` continua fora da persistência.

As outras transações de `EpgRepository` foram preservadas: cache de programas e guia de portal executam várias operações/limpeza que precisam de atomicidade conjunta. Remover essas transações só por semelhança visual seria incorreto. `RoomTransaction.kt`, `EpgDao.kt`, `ProgrammeHashTracker.kt`, parser e schema não mudaram.

**Provas de código:** na pasta de cópias estão o KSP anterior e os fontes locais da dependência efetivamente usada: `room-generated-EpgDao_Impl-before.kt`, `EntityInsertAdapter.kt` e `DBUtil.kt`. Esta é uma adaptação mais pequena da intenção upstream; não uma reprodução do SQL upstream de treze colunas, que não corresponde ao schema local de oito colunas.

## B2 — protótipo/medição, sem ativação no APK

O plano aprovado condicionava a integração a ganho medido sem aumento material de memória/arranque e sem risco de prune parcial. Foram criados **apenas instrumentos de desenvolvimento**, em `OwnTV-main/tools/epg/`:

- `benchmark_up70_b.py`: bases SQLite sintéticas exclusivamente em memória; compara o lazy atual com um cursor ordenado e limitado a 100000 entradas. Arrays no modelo, cursor fechado ao atingir o limite, nunca promoção de prefixo de canal incompleto.
- `test_preload_model.py`: cinco testes do modelo: canal maior que o teto, limite exato, canal pré-carregado ausente no feed, parse incompleto, timestamps zero/negativos e teto zero.

Não há novo cache global, preferência, dependência, índice ou código de preload executado pela App. O tracker primitivo atual e a consulta limitada a saldo+1 continuam em produção.

### Resultados sintéticos

SQLite 3.50.4 do Python no PC; três repetições, medianas, com `tracemalloc`. **Não são medições do Room, heap Kotlin ou box Android.** O plano da consulta ordenada usa o índice natural existente, sem uma nova B-tree de ordenação.

| Cenário | Consultas lazy / preload | Linhas lidas lazy / preload | Tempo total do modelo lazy / preload | Pico Python lazy / preload |
|---|---:|---:|---:|---:|
| full_240 | 240 / 1 | 96000 / 96000 | 423.4 / 320.2 ms | 2063 / 1987 KiB |
| partial_12_of_240 | 12 / 1 | 4800 / 96000 | 22.1 / 352.5 ms | 184 / 1978 KiB |
| full_3000_small_channels | 3000 / 1 | 48000 / 48000 | 328.6 / 210.5 ms | 2101 / 2253 KiB |

O preload ganha tempo total nos feeds completos, sobretudo com milhares de canais. Mas adia todo o início do processamento: no modelo completo de 240 canais, aproximadamente 320 ms são gastos antes de processar o feed, em vez de leituras progressivas. No feed parcial de 12 dos 240 canais, lê 96000 programas em vez de 4800 e mantém 96000 entradas em vez de 4800; é mais lento e aumenta o pico de memória do modelo.

**Decisão:** não ativar preload global. Não é uma melhoria universal e falha o gate aprovado no cenário parcial. O resultado não prova que qualquer variante futura seja pior: uma abordagem adaptativa dirigida aos canais realmente alcançados pode ser reavaliada, se houver perfil Room/box que justifique o trabalho e um teto firme por linha. Não introduzir parâmetros arbitrários de preload nesta versão.

A semântica do protótipo só permite candidatos a prune em canais efetivamente alcançados e parse completo. O modelo não substitui os flags `seen` nem os testes do tracker de produção. Não se importou o tracker upstream que pode ultrapassar o limite dentro de um canal. A normalização/fusos/correções de diferidos não mudam.

Provas: `preload-benchmark.json` na pasta de cópias; o script pode repetir o ensaio sem aceder às listas ou credenciais reais. Estes números não demonstram percentagens de melhoria na App.

## Testes de implementação

- Novo `OwnTV_Core/core/src/test/java/tv/own/owntv/core/repository/EpgGeneratedInsertTest.kt`: executa o adapter KSP real sobre uma ponte SQLite/JDBC em memória. Não repete o bind de produção. Verifica IDs zero/positivos/negativos, colisões da chave natural, isolamento por source, texto Unicode, null/empty, hashes e horários sem shift; erro a meio com fecho de statement/rollback; lote de 4000 programas com exatamente uma preparação e um fecho.
- Novo `OwnTV_Core/core/src/androidTest/java/tv/own/owntv/core/epg/EpgBatchTransactionTest.kt`: driver BundledSQLite da App, chamada DAO sem wrapper exterior. Cobre agora/seguinte, resumos do guia, observadores após atualização sem mudar contagem, reversão do lote se uma linha falha e caller já cancelado antes da escrita. **Compilação não equivale a execução em dispositivo.**
- A chamada DAO mantém a semântica transacional/cancelamento do Room. Não se promete interrupção de cada bind síncrono: se houver cancelamento durante um lote, exige-se atomicidade, não uma meia gravação. A aceitação física deve verificar esse percurso.

- Core: **217 testes JVM dirigidos** (repository, database, EPG e workers EPG), zero falhas/erros/ignorados. Execução final em 12 s, depois de corrigir duas indicações de nullability do novo teste de reflexão, sem supressões.
- Modelo de preload: **cinco testes Python aprovados**, em SQLite sintético; três repetições por cenário no benchmark.
- `:core:compileDebugAndroidTestKotlin`: aprovado; os três testes novos de Room/Flows/rollback/cancelamento foram **compilados, não executados** em Android.
- App: `compileStandardReleaseKotlin`, **470 testes JVM** sem falhas/erros/ignorados e `lintVitalStandardRelease` aprovados. Relatório vital: **No issues found.**
- Build conjunto com `--dependency-verification=strict -PowntvUseCoreSnapshot=true`: **BUILD SUCCESSFUL in 4m 20s**; 143 tarefas, 59 executadas, 48 da cache, 36 atualizadas. Este tempo é de build, não de execução da App.
- Total desta validação: **687 testes JVM + cinco testes do modelo**. Não confundir com os 1542 testes de âmbito diferente executados no lote A anterior.
- Inventário i18n aprovado sem alteração dos manifests/baselines: a mudança de produção não acrescenta textos nem SQL literal.
- A suite integral do Core e o lint integral do Core não foram reexecutados neste lote; os resultados anteriores de gravação/pluralização não foram suprimidos nem apresentados como resolvidos.

## Preservação, integração e limites

Cópias e logs: `C:/Users/renat/Downloads/OwnTV-up70b-2026-11-08-223756/`. Backup do snapshot anterior integral, hashes dos 1107 inputs e estado Git anterior antes de alterar o código. Checkout já modificado preservado. Nenhum dado persistente de utilizador foi migrado ou usado nos testes.

Snapshot: **1109 inputs**, SHA-256 `5a537cee362fa0c99fad1ecb681e3d4e4819326fb3122c6792590a7d4610a87d`. Todos comparados byte a byte entre checkout, ZIP e extração efetivamente compilada na App. Face ao snapshot anterior de 1107 inputs: **um ficheiro de produção alterado e dois ficheiros de testes adicionados**, zero inputs removidos. Os scripts de ensaio estão nas ferramentas da App, fora do código de execução e do APK. Prova em `integration-input-proof.json` e `final-validation.json`.

Sem alteração de schema 51, backup 26, configurações guardadas, tempos EPG/catchup, filtros de diferidos, ordenação manual ou prioridades. Mantidos AGP 9.5.1, Media3 1.11.1 e libmpv 2026.10.4. Volume a 100%, buffers/HLS e cooldown preservados.

Não se gerou APK, não se instalou na box, não houve commit/push. UP70-C mantém-se pendente. As dez falhas anteriores de testes de gravação, reproduzidas também antes de UP70-A, estão documentadas nesse lote e não são alvo desta alteração.

Aceitação física pendente: atualização completa e interrompida do EPG, programa atual/seguinte após refresh sem mudar contagem, navegação enquanto sincroniza, diferidos com correção horária existente e fontes editadas/removidas durante o refresh. Não anunciar ganho percentual sem medir este APK no dispositivo.
