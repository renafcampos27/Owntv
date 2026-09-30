# Buffer de TV em direto — implementação e validação

Data: 30/09/2026. App `OwnTV-main` e biblioteca local `OwnTV_Core`.

As correções adicionais de recuperação, edição das opções, foco e backup estão descritas em [Correções da auditoria](AUDITORIA_CORRECOES_2026-09-30.md). A validação abaixo regista a implementação de buffer anterior a essas correções; consultar o novo documento para a verificação conjunta atual.

## Configurações

Em **Definições → Reprodutor de vídeo → TV em direto**:

| Opção | Valores | Aplicação |
|---|---|---|
| Buffer inicial | Automático; 2, 3, 4…10 s | Objetivo de conteúdo antes de começar um canal. |
| Reserva de reprodução | Equilibrada 8–10 s; Estável 15–17 s; Personalizada | Objetivos de carregamento durante a reprodução. |
| Reserva personalizada | Mínimo 1–60 s; máximo entre o mínimo e mínimo +10 s | Permite comparar, por exemplo, 8–10 com 8–12 s. |
| Retoma após buffering | Imediata, informação fixa | Sem voltar a exigir o buffer inicial. Depende de existirem dados reproduzíveis e de o decoder estar pronto. |
| Atraso face ao direto | Automático HLS; modos anteriores/personalizado | Alvo aplicado pelo ExoPlayer/HLS; o mpv conserva o comportamento da origem. |
| Ajustes por lista | Seguir global ou valores explícitos | Buffer inicial, reserva e atraso configurados separadamente. |

As alterações aplicam-se na próxima abertura de canal. Não reiniciam um canal saudável apenas por editar uma opção. Definições previamente guardadas são preservadas, incluindo valores antigos de buffer inicial até 30 s.

O buffer inicial é um objetivo, não um cronómetro nem uma garantia de duração exata. O Media3 pode autorizar arranque antes desse objetivo devido ao atraso do direto ou ao objetivo de bytes. Um buffer inicial superior ao mínimo escolhido eleva o intervalo de reserva necessário; por exemplo, 10 s de buffer inicial com Equilibrada resulta em objetivos de 10–12 s. A reserva real depende da origem, rede e memória.

## Comportamento implementado

- ExoPlayer: limiar adicional de retoma de 0 ms, independente do buffer inicial. Reserva e atraso HLS são resolvidos separadamente.
- mpv: buffer inicial personalizado retirado apenas após confirmar reprodução nativa da carga atual. Interrupções posteriores não repetem esse limiar. Abertura do ficheiro por si só não conclui a preparação inicial.
- Eventos antigos: libertação do buffer inicial valida geração, origem e estado nativo; as alterações de estado publicadas após chamadas nativas voltam a validar a sintonia na thread principal.
- Arranque: uma margem adicional limitada, até 10 s, exige progresso observado em bytes ou buffer. Uma ligação aberta sem progresso não recebe margem; tentativas internas não renovam indefinidamente o orçamento.
- Observação mpv: leitura local durante a preparação, limitada a 60 s e cancelada numa nova carga/paragem. Não cria pedidos HLS adicionais.
- Pré-buffer inacessível no ExoPlayer: a avaliação considera atividade de pedidos e cadência dos segmentos. A redução de pré-buffer fica limitada à tentativa atual, sem apagar a configuração do utilizador.
- Oscilações persistentes de reprodução: reutilizam a reconexão existente e o seu orçamento; não são atribuídas ao pré-buffer inicial após o canal já ter começado.
- Diagnóstico: buffer inicial pedido e limiar configurado, reserva real e máxima, atraso real quando disponível, bytes do buffer/objetivo e limitações. Estes bytes não representam toda a RAM da app. A informação completa tem scroll; setas navegam nela enquanto aberta e Voltar fecha-a.
- A navegação de canais com ↑/↓ continua disponível no fullscreen com o diagnóstico fechado.

## Persistência e backup

- Reserva global guardada numa única transação e publicada como configuração conjunta ao reprodutor.
- Base de dados 47: campos por lista para modo, mínimo e margem de reserva; migração 46→47 preserva a escolha anterior que acoplava reserva e atraso.
- Backup 25: inclui reserva global e por lista, mantendo as restantes configurações já portáveis.
- Backups antigos migram a opção combinada. Campos novos e antigos não são misturados num restauro parcial; herança explícita limpa todo o ajuste por lista.
- A recuperação das listas continua a depender da atualização normal das fontes, conforme pedido anteriormente.

## Validação local

Comandos usados, sem tarefa de geração de APK:

```powershell
# OwnTV_Core
.\gradlew.bat :core:testDebugUnitTest :player-core:testDebugUnitTest

# OwnTV-main, com owntv.corePath a apontar para a biblioteca local
.\gradlew.bat :app:compileStandardDebugKotlin :app:testStandardDebugUnitTest
```

Cobertura relevante: limiar de retoma zero para todas as escolhas; intervalo personalizado; preservação/migração de preferências; backup novo/antigo/parcial; migração SQLite; pré-buffer e cadência HLS; orçamento finito de arranque; proteção da carga inicial A→B→A; deteção de oscilações persistentes; pesquisa das definições.

Validação conjunta final: **BUILD SUCCESSFUL**, 30/09/2026. App: **182 testes**; Core: **787**; reprodutores: **281**. Total **1 250**, sem falhas, erros ou testes ignorados. Inventário de traduções e verificação de espaços também passaram; os XML das novas configurações foram analisados sem erros.

O comando final executou compilação e as três suites na mesma invocação, usando o Core local configurado em `C:/Users/renat/.gradle/gradle.properties`:

```powershell
.\gradlew.bat :app:compileStandardDebugKotlin :app:testStandardDebugUnitTest :OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest
```

Log final: `C:/Users/renat/Documents/Codex/2026-09-22/c-users-renat-downloads-owntv-main/work/buffer-validation-final.log`. Relatórios XML: `app/build/test-results/testStandardDebugUnitTest`, `OwnTV_Core/core/build/test-results/testDebugUnitTest` e `OwnTV_Core/player-core/build/test-results/testDebugUnitTest`.

Não existem dispositivos ligados por ADB nesta sessão; a reprodução e navegação reais nas boxes permanecem por validar. Nenhuma tarefa de APK foi executada.

| Requisito | Evidência local | Estado |
|---|---|---|
| Separar arranque, reserva e atraso | `LiveBufferTest`, configuração dos motores e menu compilado | Implementado e verificado localmente |
| Retoma sem repetir o pré-buffer | `LiveBufferTest`, `InitialLiveCacheGateTest`; configuração Exo/mpv | Implementado; comportamento físico pendente |
| Opções Automático/2–10 e intervalo personalizado | Menu compilado, XML, `LiveBufferTest` | Implementado; navegação física pendente |
| Preservação de escolhas e herança por lista | `LiveReservePreferencesTest`, `LiveReserveSourceBackupTest`, `PlaybackDatabaseMigrationTest`, schema 47 | Verificado localmente |
| Backup novo/antigo/parcial | Testes de backup e chamadas de exportação/importação na versão 25 | Verificado localmente |
| Arranque com progresso e orçamento finito | `LiveStartupGraceTest`, testes de alternativas, observação limitada nos motores | Verificado localmente; origem real pendente |
| Deteção de oscilações sem retirar pré-buffer inicial | `RebufferFlapDetectorTest`, `TuneStateTest`, reconexão existente | Verificado localmente |
| Diagnóstico completo e pesquisa das opções | Compilação, `SettingsSearchCoverageTest`, XML e controlo de foco/scroll | Verificado localmente; leitura física pendente |
| Zapping, fullscreen e estabilidade nas boxes | Protocolo abaixo; nenhum dispositivo disponível nesta sessão | Pendente |

## Ensaio nas boxes

1. Começar com buffer inicial 5 s, reserva Equilibrada e atraso Automático, mantendo as outras opções constantes. São valores de ensaio; não substituem automaticamente configurações existentes.
2. Confirmar no diagnóstico os valores configurados e o buffer efetivamente disponível.
3. Repetir A→B→C→A com intervalos curtos, com ↑/↓ e através da lista; entrar/sair de fullscreen. Verificar que callbacks antigos não alteram o canal escolhido.
4. Observar uma interrupção transitória: a retoma deve ocorrer quando o motor dispuser de dados reproduzíveis, sem aguardar novamente os 5 s iniciais. Não provocar perda de rede indefinida para avaliar esta condição.
5. Comparar 8–10 e 8–12 s durante pelo menos 30 minutos por configuração, em canais saudáveis; registar arranque, interrupções, reserva real e memória. Comparar 8/10 s iniciais apenas se 5 s não chegar.
6. Reiniciar a app/box e restaurar um backup; verificar configurações globais, ajustes por lista e herança.

O ensaio de reprodução não é substituído pelos testes unitários. Ajustes de buffer não comprovam resolver recusas HTTP 403 nem falhas da origem.

## Referências técnicas

- [Media3 1.11.1 — DefaultLoadControl](https://github.com/androidx/media/blob/1.11.1/libraries/exoplayer/src/main/java/androidx/media3/exoplayer/DefaultLoadControl.java)
- [ExoPlayer — reprodução em direto](https://developer.android.com/media/media3/exoplayer/live-streaming)
- [mpv — cache e eventos de reprodução](https://mpv.io/manual/stable/)
