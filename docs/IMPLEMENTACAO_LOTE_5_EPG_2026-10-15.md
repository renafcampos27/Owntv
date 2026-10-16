# Lote 5 — reutilização do modelo da linha EPG (A15-11)

## Estado

Parte de código implementada e validada automaticamente em 15/10/2026, apenas em `OwnTV-main`. **Aceitação de desempenho na box continua pendente:** `adb devices` não apresentou dispositivos ligados. Não há aceleração medida na Xiaomi/Thomson, resultado de latência tecla–imagem ou declaração de melhoria do streaming.

Core, snapshot/lock, Mobile, base de dados, configurações de reprodução, HLS, áudio, buffers e intervalos de reabertura não foram alterados neste lote. Não foi gerado APK, commit ou push.

## Alteração

- `GuideProgrammeRow` prepara uma fotografia imutável dos programas e dos intervalos já definidos pelo algoritmo de sobreposições existente. Desenho, seleção, OK, informação do programa e navegação reutilizam esses intervalos. Seleção/navegação procuram a célula por pesquisa binária, sem repetir a ordenação e o sweep a cada tecla.
- O modelo conserva a entidade original selecionada, a hora de início e os metadados de desvio. Recortar o desenho à janela não recorta o pedido de reprodução em diferido. A política de lacunas, sobreposições e passagem para outra janela mantém-se.
- Cache derivada, propriedade da UI no ViewModel: até 24 linhas e 16 000 entradas ponderadas (referências das listas de entrada/fotografia e células). É um orçamento de estruturas, não uma garantia de RAM em bytes. Linha acima do orçamento pode ser apresentada, mas não entra na cache. Entradas vazias também respeitam o limite de linhas.
- A identidade inclui a chave existente de perfil/canal/origem/EPG/herança/desvio/janela de leitura; o reaproveitamento exige a mesma lista de dados e os mesmos limites de desenho. Dados novos ou janela diferente substituem o modelo. A cache é limpa juntamente com as linhas invalidadas e ao desativar o Guia; não existe cache global do processo nem novas leituras de rede/base de dados.
- O mini-guia prepara uma linha por conjunto de dados/janela, reutilizando-a entre as teclas. Não partilha dados entre canais ou sessões.
- Removidos os helpers antigos de seleção/navegação e abertura no código de produção. O algoritmo de seleção anterior fica apenas nos testes como referência independente para comparação.

## Medição local antes/depois

Fixture determinística: 240 programas com sobreposições, 100 posições, duas operações por posição (seleção e avanço), cinco rondas na mesma JVM. O código anterior de seleção está congelado no teste e chama o mesmo algoritmo de layout, que não foi reescrito. O novo percurso prepara a linha uma vez. Os contadores usam a instrumentação `EPG_LAYOUT` existente.

| Ronda | Cálculos anteriores | Cálculos novos | Tempo acumulado anterior em layout (ms) | Tempo novo em layout (ms) |
|---|---:|---:|---:|---:|
| 1 | 200 | 1 | 29.000 | 0.169 |
| 2 | 200 | 1 | 30.781 | 0.114 |
| 3 | 200 | 1 | 28.537 | 0.109 |
| 4 | 200 | 1 | 25.635 | 0.107 |
| 5 | 200 | 1 | 21.289 | 0.112 |

Estes tempos medem **só o trabalho de layout instrumentado na JVM do PC**. Excluem desenho Android, foco, Compose, leituras de dados, preparação/cópia do modelo e a latência de resposta do comando. As rondas partilham aquecimento JIT/GC; não são um benchmark de desempenho do dispositivo. Não converter estes números num fator de aceleração da app. A prova repetível é a remoção de 199 reconstruções de layout neste percurso estático. Dados/janelas novos continuam legitimamente a produzir novos cálculos.

## Validação executada

- **414 testes unitários da App aprovados**, zero falhas, erros ou testes ignorados, incluindo cinco novos testes de equivalência, identidade de diferidos, limites/invalidação da cache e contagem de trabalho.
- Comparação determinística com o algoritmo anterior: 25 conjuntos desordenados, sobreposições, intervalos inválidos, lacunas, limites da janela e deslocações -2/-1/0/+1/+2.
- App compilada em debug/release; `lintVitalStandardRelease` aprovado; testes Android da App e módulo benchmark compilados. Não executados num dispositivo.
- Inventário i18n, formatação numérica, overflow de texto e whitespace verificados. Core não foi alterado; não se repetiram as suas suites neste lote.
- Registo final: `C:/Users/renat/AppData/Local/Temp/owntv-lote5-final-validation.log` — BUILD SUCCESSFUL.
- Backup dos ficheiros anteriores: `C:/Users/renat/AppData/Local/Temp/owntv-lote5-before-20261015-234704`.

## Fecho de aceitação na box

1. Comparar antes/depois na mesma box, fonte, período, filtro e definições. Usar o percurso E1/E2 documentado; anotar o valor de Apenas diferidos e desligá-lo apenas para a fixture sintética sem arquivo, repondo-o no fim.
2. Com programas carregados, navegar 100 passos esquerda/direita e entre canais. Repetir três vezes; comparar `EPG_LAYOUT`, `ui_perf`, memória/GC e foco. Separar trabalho de leitura de períodos novos de navegação sobre dados já carregados.
3. Recuar vários dias, passar por lacunas/sobreposições e limites de janela, abrir um diferido e regressar. Confirmar que o programa e a hora continuam corretos e que não há células vazias novas nem perda do cursor.
4. Repetir mudança de perfil/origem, atualização EPG, desvio por canal e Home/regresso. Confirmar que não reaparece um modelo de dados antigo.

Só dar a aceitação física como concluída depois desta comparação. Um emulador pode validar o percurso, mas não representa a fluidez da box.

## Ideias opcionais da auditoria

Prévia completa de restauro, validação de exportação pelo contrato de importação e sessão de diagnóstico temporizada mantêm-se propostas separadas. Este lote aplica A15-11; não introduz novos fluxos de backup nem novas opções de diagnóstico para acompanhar uma otimização da grelha.
