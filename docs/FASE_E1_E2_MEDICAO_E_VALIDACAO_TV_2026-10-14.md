# Fase E1/E2 — validação na box e medição dirigida

## Estado e âmbito

Preparação implementada em 14/10/2026, apenas no projeto TV. Core, Mobile, configurações de reprodução, HLS, buffers, cooldowns, volume e backups não foram alterados. Não há dispositivo ADB ligado nesta execução: os ensaios físicos estão pendentes, sem resultados inventados ou promessa de aceleração.

O objetivo desta fase é localizar a lentidão antes de implementar otimizações condicionadas. Não foi criada outra opção nem outra thread de recolha: reutiliza-se o monitor de interface existente, com a mesma janela de 10 segundos e o mesmo ciclo de vida STARTED.

## Alterações

- `UiWorkWindow`: contadores e histogramas de dimensão fixa para cálculo das células EPG, leitura visível de linhas, leitura antecipada e preparação das linhas visíveis na lista TV. Não guarda nomes, IDs, URLs, credenciais, textos dos programas ou teclas.
- `UiWorkDiagnostics`: desativado por defeito, acompanha o monitor existente. Sem leituras de relógio ou registo quando desligado. As funções instrumentadas conservam resultados, falhas e cancelamento; uma operação antiga não pode publicar noutra sessão.
- `UiPerformanceMonitor`: regista `ui_work` no diagnóstico a cada janela com operações concluídas, canceladas e falhadas; total/máximo em microssegundos e P95 em intervalos de milissegundos.
- Benchmark do cronograma separado do arranque/abertura de canal. Exige programas carregados e foco correto; falha explicitamente se os dados não estiverem disponíveis ou o foco se perder.
- Seleção da variante do alvo pelo parâmetro `owntv.benchmarkAbi`: `standard` para ARM ou `x86_64` para emulador, que permanece predefinido. Não muda a variante normal de build da app.

Os tempos `EPG_ROW_READ`/`EPG_PREFETCH_READ` são duração decorrida da leitura real, incluindo suspensões e possível acesso à origem. Não são tempo exclusivo de CPU nem velocidade de transferência. Cache hits não passam pelo loader e não entram nestas contagens. Uma operação que atravessa uma janela é atribuída à janela da sua conclusão. Somar operações concorrentes pode exceder a duração da janela.

P95=1000 significa intervalo superior de 1000 ms ou mais, não um limite ao tempo real: `maxUs` conserva o máximo medido. Os fotogramas lentos da UI não são fotogramas perdidos do vídeo; atraso na fila de teclas não é latência completa até à imagem. As medições podem adicionar algum custo, pelo que a comparação principal deve usar diagnóstico desligado, e o ensaio com diagnóstico serve para localizar o trabalho.

## E1 — ensaios de aceitação na box

Usar um APK atualizado e guardar primeiro um backup. Repetir na Xiaomi e noutra box disponível, com as mesmas definições, fonte, rede, resolução e frequência de saída. Anotar versão instalada, Android, dispositivo, pré-visualização ativa/inativa e reprodutor.

| Ensaio | Procedimento | Resultado esperado |
|---|---|---|
| Lista e regresso | Abrir um canal, regressar à lista; repetir 10 vezes. | Foco no canal atual; sem abertura involuntária nem fuga para a barra lateral. |
| Lista e comando | Percorrer pelo menos 30 linhas para baixo e para cima, incluindo pressão contínua. | Foco acompanha os canais, sem ficar preso ou sair da lista. |
| Guia e períodos antigos | Entrar na linha com OK, percorrer vários programas para a esquerda e regressar à direita; repetir 5 vezes. | Programas carregam sem exigir uma deslocação inversa para aparecer; lacunas reais não inventam programas. |
| Sobreposições | Se a origem tiver um programa longo interrompido por uma entrada curta, selecionar a continuação. | Seleção corresponde ao programa desenhado e usa a entidade original. |
| Diferidos | Abrir programas de ontem e de vários dias atrás dentro da retenção do fornecedor. | Abre o programa escolhido; não troca silenciosamente para direto. Registar HTTP/erro quando falhar. |
| Pausa/retoma | Pausar e retomar um diferido/timeshift disponível. | Estado de pausa preservado e som retoma por ação do utilizador. |
| Reabertura de canal | A→B→A e A→B→C→A com o intervalo atual configurado. | Espera e erro apresentados coerentemente; distinguir recusa do fornecedor de falha interna. Não reduzir a espera neste ensaio. |
| Ciclo de vida | Home e voltar, depois sair e abrir novamente. | Sem reprodução indevida em background nem dados de medição de uma sessão anterior. |

## E2 — recolher medições sem mudar reprodução

1. Começar com diagnóstico desligado. Aquecer a app e repetir lista e guia 3 vezes; anotar onde e quando se percebe a lentidão.
2. Ativar **Definições → Medir desempenho da interface**. Para guardar também o registo persistente, ativar **Reprodutor de vídeo → Diagnóstico → Registo detalhado de reprodução**.
3. Medir lista e guia em percursos separados, aproximadamente 60 segundos cada. Não abrir novos canais durante a medição da lista. No guia, comparar programas já carregados com mudança para um período ainda não carregado.
4. Exportar o diagnóstico pelo mecanismo existente. Comparar `ui_perf` e `ui_work` no mesmo intervalo. Os novos campos não contêm dados pessoais; o diagnóstico completo segue as regras existentes de ocultação de endereços.
5. Desligar os dois diagnósticos ao terminar. Sem diagnóstico ativo, não existe nova recolha de tempos nem escrita de `ui_work`.

Se `EPG_LAYOUT` dominar, investigar recomputação da grelha. Se o trabalho de leitura dominar, separar cache/base de dados/origem antes de mudar concorrência. Se houver GC e fotogramas lentos sem tempos elevados nestes pontos, usar System Trace para procurar alocações, composição, desenho e concorrência fora das regiões instrumentadas. Não inferir a causa apenas de um contador.

## Ensaios automáticos de desempenho

Requer uma box preparada, ADB autorizado, lista de teste com pelo menos três canais funcionais, EPG com programas e navegação TV/Guia visível. O ensaio pode abrir canais; usar uma fonte de teste para evitar limites de sessões. O benchmark gera/instala o alvo de ensaio ao executar, mas não foi executado nem gerado APK nesta fase.

A lista criada por `tools/performance/local_iptv_fixture.py` fornece HLS sintético e EPG, mas não arquivo. Antes de medir o Guia, anotar o valor atual de **Apenas diferidos**, desligar esse filtro e confirmar que os programas de teste aparecem. No fim, repor o valor anterior. O ensaio verifica esta preparação e termina com uma mensagem clara se o filtro continuar ativo; não altera preferências nem inventa suporte de diferidos. Usar um perfil de teste.

Emulador serve para verificar o percurso e a instrumentação; medir fluidez e aprovar desempenho requer hardware físico. A fonte sintética de 180 segundos não valida arquivo, serviço IPTV real nem reprodução prolongada.

PowerShell, na raiz do projeto (usar o JDK do Android Studio):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest '-Powntv.benchmarkAbi=standard' '-Pandroid.testInstrumentationRunnerArguments.class=tv.own.owntv.baselineprofile.IptvPerformanceBenchmark#warmListNavigation'
.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest '-Powntv.benchmarkAbi=standard' '-Pandroid.testInstrumentationRunnerArguments.class=tv.own.owntv.baselineprofile.IptvPerformanceBenchmark#warmGuideTimelineNavigation'
```

Para emulador x86_64, omitir o parâmetro `owntv.benchmarkAbi`. Não regenerar o baseline profile só para recolher estas métricas. Macrobenchmark exige API 28+; nas boxes Android 8/API 26–27 usar o diagnóstico manual existente. A preparação anterior ao percurso medido não equivale a arranque frio.

Comparar as distribuições e os rastos de cinco iterações, não apenas a média. Só aprovar uma otimização após melhoria repetida no mesmo hardware e ausência de regressão no foco, EPG, memória e reprodução. Não se define uma aceleração universal sem baseline físico.

## Referências

- [Métricas Macrobenchmark e FrameTimingMetric](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics).
- [Eventos de rastreio Android](https://developer.android.com/topic/performance/tracing/custom-events): para aprofundamento pelo System Trace. Esta fase usa agregação própria; não adiciona nem anuncia captura Perfetto automática.

## Validação desta implementação

Concluída: aplicação compilada em debug/release, 400 testes unitários aprovados (incluindo 7 novos do medidor), sem falhas, erros ou testes ignorados, lintVitalStandardRelease aprovado e módulo benchmark compilado com `owntv.benchmarkAbi=standard`. Inventário i18n aprovado: 51 entradas baseline e 1 071 literais técnicos classificados; não foram adicionadas mensagens de interface fora dos recursos. Verificação de whitespace aprovada.

Registo: `C:\Users\renat\AppData\Local\Temp\owntv-e12-validation.log`, BUILD SUCCESSFUL. Nenhuma alteração no Core: o snapshot anterior mantém-se. Os testes Core/player do lote anterior não foram repetidos nesta fase porque os seus ficheiros não mudaram.

Os ensaios físicos E1/E2 continuam pendentes porque não há box ligada. O módulo benchmark foi compilado, não executado num dispositivo. Não foi gerado APK, medida aceleração nem declarado um novo problema de streaming resolvido.
