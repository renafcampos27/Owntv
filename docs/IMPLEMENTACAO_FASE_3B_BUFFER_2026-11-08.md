# Fase 3B — informação de buffer, segmentos e memória

Data: 08/11/2026. OwnTV Android TV personalizada.
Estado: fase 3B implementada e validada localmente no âmbito informativo aprovado; aceitação física pendente.

## Âmbito executado

Apenas P17, no âmbito reduzido da secção 16 do plano. As medições de buffer, limites temporais, bytes alocados/alvo e durações HLS já existiam. Foram reutilizadas, sem criar métricas, modos, preferências ou temporizadores.

O resumo anterior dizia «parar acima de», sugerindo um máximo exato. Agora identifica o limiar e explica compactamente que segmentos podem ultrapassá-lo e que a memória pode limitar a reserva. Nos detalhes, «limite de carga» passa a «limiar superior», a referência temporal passa a ser identificada como pertencente à playlist e os MiB ficam identificados como buffer alocado/alvo, não como RAM total da aplicação ou da box.

A linha HLS no resumo passa a dizer «Último publicado: 9,5 s», por exemplo, em vez de mostrar só uma duração sem identificar a medição. Continua a representar a última duração publicada na playlist disponível, não um segmento garantidamente descarregado. Se não existir uma duração válida, conserva «Não disponível»; não fabrica zero. A referência da playlist não se torna uma definição da App.

## Ficheiros alterados

| Base | Ficheiro relativo | Alteração |
| --- | --- | --- |
| `C:/Users/renat/Downloads/OwnTV_Core` | `core/src/main/res/values/live_buffer_diagnostics.xml` | Quatro textos ingleses: limiar, último segmento publicado, referência da playlist e buffer alocado/alvo. |
| Core | `core/src/main/res/values-pt/live_buffer_diagnostics.xml` | Os mesmos quatro textos em português, herdados também pelo locale PT-PT quando não existe override específico. |
| `C:/Users/renat/Downloads/OwnTV-main` | `app/src/main/res/values/ui_clarity.xml` | Resumo inglês sem promessa de teto rígido, com explicação curta numa segunda linha. |
| App | `app/src/main/res/values-pt/ui_clarity.xml` e `app/src/main/res/values-pt-rPT/ui_clarity.xml` | Mesmo resumo em português/PT-PT. |
| App | `app/src/main/java/tv/own/owntv/player/StreamInfoOverlay.kt` | Reutilizar a formatação numérica existente para mostrar o rótulo traduzido «Último publicado» sem duplicar a unidade. Mantida a apresentação de dados desconhecidos. |

Não foi necessário alterar `StreamInfo.kt`, os seus modelos ou `displayText`: os campos e a seleção dos recursos já eram adequados. A janela mantém dimensões, botões, foco, scroll e frequência de atualização. Não foi acrescentada outra linha técnica, aviso persistente ou diálogo. O texto do resumo ocupa uma segunda linha no valor de buffer existente; a apresentação física/fontes da box ainda requer observação.

## Comportamento preservado e prova de âmbito

Antes de editar, os **1098 inputs** do Core foram comparados byte a byte com o snapshot final da fase 3A, SHA-256 `4d0cd73fee3e62243dc4202a7c32e17aa3514e83f4b0f27328fcd7666b1646ed`. Foram guardadas cópias, hashes e o estado Git existente.

A comparação após a alteração confirma que **somente os dois XML de textos do Core mudaram**. `LiveLatency.kt`, load control, limites de bytes/RAM, pré-buffer, arranque/retoma, valores persistidos, cancelamento, HLS, áudio, codecs, DNS, TLS e a recuperação da fase 3A conservaram os mesmos bytes. Não houve migração ou acesso a listas/credenciais do utilizador.

A explicação corresponde ao `DefaultLoadControl.shouldContinueLoading` da dependência **Media3 1.11.1 efetivamente instalada**: os limiares de duração e o alvo de bytes participam na decisão de continuar a carregar. O texto não promete precisão de corte dentro do segmento nem um limite rígido de RAM total. Não foi alterado esse algoritmo.

## Validação

- Os cinco XML de recursos alterados foram analisados; mantidos todos os nomes de recursos e argumentos de formatação em inglês/PT/PT-PT.
- Core: **21 testes JVM existentes** de `LiveBufferTest` e `HlsSegmentDiagnosticsTest`, zero falhas/erros/ignorados; `BUILD SUCCESSFUL in 10s`. Não foram criados testes que apenas repetissem os novos textos.
- App: compilação `:app:compileStandardReleaseKotlin`, **470 testes JVM** e `:app:lintVitalStandardRelease` aprovados. Relatório **No issues found.** O comando terminou **BUILD SUCCESSFUL in 5m 6s**. Total executado nesta fase: **491 testes**, zero falhas/erros/ignorados. Os 631 testes do reprodutor executados na fase 3A não foram contados como se tivessem sido repetidos nesta fase.
- Snapshot de integração: **1098 inputs**, SHA-256 `a7645d3038d51596469feaa3616bc4cac8ea081a7c96252f2f86382f37ddade2`. Checkout, ZIP e inputs extraídos da compilação comparados integralmente. O build da App reconheceu explicitamente este SHA e invalidou a configuração anterior ao mudar o lock.

Cópias e provas: `C:/Users/renat/Downloads/OwnTV-fase3b-2026-11-08-133943`.

O lint integral do Core não foi repetido; mantém os cinco erros anteriores de pluralização documentados nas fases 2A/2B, em recursos que esta fase não alterou. A aprovação do lint vital Release da App não equivale à aprovação desse lint integral.

Esta fase melhora a explicação dos valores; não demonstra ganhos de velocidade, resolução de bloqueios A–B–A ou correção dos cortes de áudio. Não houve APK, instalação, commit, publicação ou validação física na box. A fase 1B condicionada e as fases seguintes continuam pendentes.
