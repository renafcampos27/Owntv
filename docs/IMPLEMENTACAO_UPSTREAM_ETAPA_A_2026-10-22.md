# Primeira etapa — OwnTV 5.2.1 / Core 1.0.69 / libmpv 2026.10.4

Data: 22/10/2026. Âmbito autorizado: **R07, R08, R01 e R02** do relatório de análise. As etapas B/C e as propostas opcionais não foram implementadas.

Estado: **implementada e validada automaticamente; testes físicos na box pendentes**.

## 1. Alterações

### R07 — Ordenação de categorias

`CustomizationStore.setCategoryOrder` usa uma união ordenada com os dados anteriores. Editar uma lista conserva as chaves das outras listas e das categorias ocultas; chaves repetidas não são duplicadas. Uma grelha vazia não apaga a ordem guardada. Categorias partilhadas mantêm o comportamento de ordem global já existente.

Fontes: `core/customize/CategoryOrderMerge.kt` e `CustomizationStore.kt`. Cinco testes de regressão cobrem listas independentes, categorias partilhadas/ocultas, lista vazia e duplicados.

### R08 — Limpeza de personalizações após sincronização

`UserDataResolver` mantém a resolução e o controlo de concorrência existentes, mas elimina órfãos de ordenações e de associações a categorias em lotes de até 900 IDs. Cada lote é limitado por operação, perfil, tipo e IDs capturados; só remove registos cujo conteúdo já não existe. A transação original foi mantida.

Quatro testes JVM cobrem partição, limite de parâmetros SQLite, duplicados e snapshots vazios. Foi acrescentado um teste Android para os DAO reais. As duas consultas exatas também foram executadas em SQLite no PC, incluindo preservação de conteúdos existentes, outros perfis/tipos, IDs fora do snapshot e rollback da transação. Isso não equivale a executar Room numa box.

Sem migração de esquema: base de dados permanece na versão 51 e backup na 26.

### R01 — Biblioteca nativa

Dependência `libmpv` atualizada de **2026.10.3 para 2026.10.4**. Media3 permanece **1.11.1**.

- AAR Maven auditado: SHA-256 `30da5e483342ee7c44ac57a694050279a1a69a5a0d55876ebf94cfdd90fbbfe4`.
- O AAR resolvido pelo Gradle foi conferido contra esse hash.
- Verificados 30 ficheiros nativos em arm64-v8a, armeabi-v7a e x86_64: arquitetura, alinhamento adequado à ABI, contrato JNI MPV e presença de `wait_keyframe`.
- Alinhamento LOAD: 16 KiB nas ABIs de 64 bits; requisitos de 4 KiB nas bibliotecas de 32 bits. Não afirmar que todos os ELF de 32 bits têm alinhamento de 16 KiB.
- A ponte de áudio própria usa `JNI_OnLoad`/`RegisterNatives`: classes e métodos esperados foram conferidos nos binários, sem alterar a política de áudio automático/software.
- O download da publicação GitHub foi conferido pelo digest publicado. AAR GitHub e Maven têm classes iguais; as únicas diferenças encontradas nas secções nativas são build IDs de seis wrappers. Os restantes conteúdos dessas secções coincidem.
- Na preparação release da App, os 30 binários da versão nova coincidiram byte a byte com o AAR auditado.
- O registo `tools/mpv/native_manifest.json` foi atualizado para o artefacto efetivamente usado.

As correções nativas incluem HLS HTTP chunked, proteção de HEVC sem configuração inicial e compatibilidade MediaCodec. Não são uma garantia de correção de 403/404, qualidade superior de imagem/som ou recuperação do servidor do fornecedor.

O antigo verificador `tools/ffmpeg/verify_audio.py` corresponde a outro backend/manifesto e procura binários já ausentes. Não foi usado como prova da ponte atual nem foram restauradas bibliotecas antigas. A ponte real desta versão é `libowntvffmpeg.so`, fornecida pelo AAR acima.

### R02 — Diferidos em hardware

- Removidos `ArchiveDecodeStore`, a sua instalação em DI e o carregamento no arranque da App. A aprendizagem automática de software deixa de sobreviver ao processo. O ficheiro legado na box não é consultado; não se apagam preferências explícitas do utilizador.
- `wait_keyframe=1` é aplicado apenas a diferidos com hardware selecionado; a opção é limpa em cada carregamento seguinte quando não se aplica.
- Diferidos e outros conteúdos fora do direto mantêm a análise completa; só o MPEG-TS em direto pode usar a análise abreviada, como antes para esse percurso.
- HTTP, rede, SSL, formato e áudio não são tratados como prova de necessidade de software na recuperação específica de arquivos. Uma abertura silenciosa que nem concluiu a análise também não ensina essa preferência.
- Mantida a tentativa de recuperação em software durante a sessão quando existe evidência de descodificação/ausência de imagem após abertura. Permanecem as definições manuais e os controlos dos motores existentes.

Cinco testes novos dos motores cobrem âmbito/reposição da opção, análise completa, classificação de falhas e duração da aprendizagem limitada à sessão.

## 2. Pré-requisito e proteção de dados

A fase 2 anterior foi fechada antes desta integração: ver [IMPLEMENTACAO_FASE2_CATALOGOS_2026-10-22.md](IMPLEMENTACAO_FASE2_CATALOGOS_2026-10-22.md).

No fecho foram preservadas as configurações de fontes ao recuperar importações, consolidado Retry-After por identidade autenticada e corrigida a reutilização da fonte numa segunda tentativa. A revisão final protege ainda o caso de sair do formulário enquanto a limpeza de cancelamento continua: não se elimina uma fonte recuperada nem se repõe a intenção de Retry depois de o utilizador a abandonar.

Cópia de recuperação verificada: `C:/Users/renat/Downloads/OwnTV-upstream-A-backup-20261022/`, com App/Core anteriores à intervenção, ZIP/lock e manifesto SHA-256. Nenhuma operação na base de dados da box foi executada no PC.

## 3. Validação final

- Core e motores: `:core:testDebugUnitTest`, `:player-core:testDebugUnitTest` e `:core:compileDebugAndroidTestKotlin` — `BUILD SUCCESSFUL`. Relatórios finais: **1 329 testes Core + 557 testes dos motores**, zero falhas/erros/ignorados.
- App contra o snapshot definitivo: compilação standard debug e release, `:app:testStandardDebugUnitTest`, `:app:lintVitalStandardRelease` e `:app:mergeStandardReleaseNativeLibs` — `BUILD SUCCESSFUL`. **426 testes da App**, zero falhas/erros/ignorados.
- Total final, sem contar repetições: **2 312 testes JVM**. Mais **6 testes** da ferramenta de snapshot aprovados. Os testes Android foram compilados, não executados num dispositivo.
- Snapshot definitivo: **1 030 inputs**, SHA-256 `15a5cbe97bcf5682a8ed24609c1e8ac31b0eb08e07203337fc87afed1ccc425b`. Conteúdo conferido byte a byte com o Core externo, incluindo ausência do armazenamento automático legado e presença da libmpv nova.
- Configuração normal de build, sem forçar o modo snapshot: `gradlew help` aprovado, depois da validação acima. Não foi desativado o controlo de integridade do Core.
- Inventários i18n e verificação de espaços do diff aprovados. As verificações existentes não foram desligadas para obter um resultado verde.
- Logs finais: `%TEMP%/owntv-upstream-a-core-final.log`, `%TEMP%/owntv-upstream-a-app-final.log` e `%TEMP%/owntv-upstream-a-normal-config.log`.
- As bibliotecas de vídeo/áudio foram auditadas estaticamente e preparadas para release. Não foi executada descodificação nativa numa box nem medido um ganho de desempenho.

Próxima etapa destas publicações: **B — R03/R04/R05/R06** (superfícies, recuperação de imagem, AFR e compatibilidade de áudio). Continua pendente; C e opcionais também.

## 4. Aceitação física pendente

1. Em duas listas, reordenar categorias e confirmar a ordem da outra lista, incluindo ocultas e partilhadas.
2. Sincronizar uma lista com categorias/ordenações personalizadas e confirmar manutenção das escolhas e isolamento entre perfis.
3. Em mpv, abrir diferidos H.264/HEVC, procurar temporalmente, sair, abrir um direto e alternar canais. Confirmar motor/descodificador no diagnóstico.
4. Confirmar que um erro HTTP de um arquivo não faz os restantes arquivos desse servidor arrancarem em software em futuras execuções.
5. Conferir as definições por canal, áudio a 100%, HLS exclusivo/exceções, reservas de buffer, intervalo de reabertura e correção de diferidos +1 h.
6. Repetir numa box antiga e numa recente. Compilação, JNI estático e testes JVM não provam comportamento do hardware ou do fornecedor.

Não foi gerado APK, alterada assinatura, feito commit/push ou publicada uma versão. O número de versão comercial da App não foi mudado para sugerir equivalência integral com a OwnTV 5.2.1 ou Core 1.0.69.

## 5. Atualização posterior

A etapa B foi posteriormente autorizada, implementada e validada automaticamente. O estado e as limitações atuais estão em [IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md). A próxima etapa destas publicações é C — R09/R10; os resultados e o hash desta etapa A acima mantêm-se como registo histórico.
