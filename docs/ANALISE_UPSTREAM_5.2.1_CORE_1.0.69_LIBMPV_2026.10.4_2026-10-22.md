# Análise seletiva — OwnTV 5.2.1, Core 1.0.69 e libmpv 2026.10.4

Data: 22/10/2026. Estado original: **análise; nenhuma implementação autorizada por este documento**. A implementação posteriormente autorizada da etapa A está registada na secção 9.

## 1. Conclusão

Recomendo aproveitar correções concretas de reprodução, categorias e sincronização, sem substituir o Core personalizado pelo upstream. As três publicações são de 21/10/2026. Parte das novidades já está resolvida de outra forma nesta versão; algumas opções novas seriam redundantes ou contrariariam as preferências do utilizador.

A prioridade prática é fechar a fase 2 já iniciada antes de integrar estas alterações. A atualização nativa é útil sobretudo para mpv; não corrige diretamente o transporte HLS do ExoPlayer nem comprova resolução de recusas HTTP do fornecedor.

## 2. Método e limites

- Consultei as três páginas de publicação e comparei os fontes completos das tags OwnTV 5.2.0→5.2.1, Core 1.0.68→1.0.69 e libmpv 2026.10.3→2026.10.4.
- Conferi os trechos relevantes com a App e o Core locais. As cópias upstream foram extraídas em `%TEMP%/owntv-release-audit-20261022`, sem executar código desses projetos.
- A App tem personalizações substanciais. Uma ausência de nome de classe não basta para concluir que falta uma função: foram também examinados os percursos equivalentes.
- Não compilei, não executei testes, não instalei APK e não medi boxes nesta análise. As medições citadas nos patches são do autor upstream e não demonstram ganhos na Xiaomi, Thomson ou Mortal.
- Este trabalho não incluiu uma sessão nova de multiagentes/council.

## 3. Estado local confirmado

- `OwnTV_Core/gradle/libs.versions.toml`: **libmpv 2026.10.3**, Media3 **1.11.1**.
- O Core personalizado mantém esquema de base de dados 51 e backup 26; os números do upstream não são uma migração a aplicar automaticamente.
- A App consome um snapshot ZIP/lock do Core. O registo mais recente do plano principal indica que o snapshot ainda contém a fase 1, enquanto existem alterações da fase 2 nos fontes externos e na App.
- Antes de uma integração futura: concluir revisão/validação da fase 2, regenerar o snapshot coerentemente e verificar os inputs. Não declarar que um APK contém a fase 2 apenas porque o Core externo foi alterado.

Referência de continuidade: `../implementar_rena/analise_final_ia_analisadasol.md`, secção 16. Este relatório não substitui nem dá como terminadas as fases desse plano.

## 4. Correções recomendadas

### R01 — libmpv 2026.10.3 → 2026.10.4

**Decisão: recomendar atualização controlada.** Continua a usar as mesmas bases mpv/FFmpeg da 2026.10.3; a diferença principal está nos patches de compatibilidade.

Benefícios identificados nos fontes:

1. Uma resposta HTTP HLS com transferência `chunked` já terminada não é confundida com transferência incompleta, evitando reconexões desnecessárias durante a abertura no mpv. Uma transferência realmente truncada continua a permitir recuperação.
2. A abertura MediaCodec com superfície declara dimensões máximas adaptativas. O upstream relata recuperação de H.264 acima de 30 fps num TCL/Realtek. O patch não se limita a essa marca; o resultado nas outras boxes precisa de teste.
3. HEVC sem dados iniciais de configuração deixa de entrar num caminho que pode abortar o processo nativo. Relevante para diferidos que começam a meio do fluxo.
4. Nova opção `wait_keyframe`, desligada por defeito: permite ao chamador descartar os primeiros pacotes de vídeo até ao primeiro fotograma-chave, com limite de 300 pacotes. Esse limite não equivale a um timeout fixo de seis segundos.
5. Estimativa de FPS também no percurso MediaCodec direto, útil quando o canal não declara a cadência.

**Integração:** conferir artefactos/JNI/ABIs; testar arm64 e armv7 quando usados; guardar possibilidade de regressar à 2026.10.3. Para ativar o ponto 4 é necessário também o ajuste Kotlin de R02. Atualizar apenas a versão não ativa essa opção.

**Não atribuir à atualização:** resolução de 403/404, redução garantida do tempo de reabertura do fornecedor, mais resolução ou melhor qualidade de som. A correção anterior de áudio da 2026.10.3 não deve ser contada como novidade.

### R02 — Diferidos em hardware sem aprendizagem permanente de software

**Decisão: recomendar com adaptação.**

Evidência local: `ArchiveDecodeStore.kt` guarda `software_archive_hosts`; `LiveStreamQuirks.installArchivePersistence` volta a instalar essa decisão no arranque através de `OwnTVApp.kt`. O armazenamento local examinado não contém prazo de expiração nem remoção automática. Uma falha pode, portanto, afetar futuros diferidos desse servidor indefinidamente.

O upstream remove essa persistência e mantém a aprendizagem na sessão. Recomendo abandonar a despromoção automática permanente, preservar as escolhas explícitas por canal e evitar classificar uma recusa HTTP ou um erro de formato como defeito do descodificador.

Com a libmpv nova, aplicar `vd-lavc-o=wait_keyframe=1` exclusivamente aos diferidos apropriados, repondo a opção em todos os carregamentos seguintes. Testar abertura, procura temporal e mudança de canal; não atrasar todos os diretos à espera de um fotograma-chave.

### R03 — Ligar/desligar a superfície do mpv sem bloquear o comando

**Decisão: recomendar com revisão de ciclo de vida.**

Evidência local: `OwnTVPlayer.attachSurface` e `detachSurface` ainda chamam diretamente funções nativas na thread principal. Operações nativas demoradas podem prender interface e comando.

O upstream serializa a ligação na tarefa do mpv e invalida trabalhos antigos por geração da superfície. É uma boa direção. Contudo, o novo detach ainda espera até 2,5 s na thread principal: não o tratar como solução integralmente assíncrona ou livre de bloqueio.

Adaptação proposta: fila serial única para operações nativas, geração/identidade da superfície, abertura apenas depois da ligação válida e contrato explícito de destruição. Garantir que não se usa uma superfície já destruída; não substituir isso por detach simplesmente assíncrono sem proteção. Validar rapidez do comando e duração real de attach/detach.

A repetição de pedidos de layout do upstream para recuperar uma superfície ausente deve ser limitada e aplicada só ao caso necessário; não adicionar polling em todas as mudanças de canal sem evidência.

### R04 — Recuperação de imagem congelada e fim efetivo de uma reprodução em erro

**Decisão: recomendar integrar no mecanismo existente, sem outro watchdog paralelo.**

O código local já deteta ausência de progresso e de frames, mas o ramo de imagem congelada chama novamente `reconnect`. O upstream distingue reconexão, novo descodificador/superfície e eventual outro motor; também evita iniciar o descodificador numa superfície provisória e termina a reprodução quando declara erro terminal.

Adaptação proposta:

- Recuperação limitada por sintonia: uma tentativa simples; se se repetir a falha, reconstruir descodificador/superfície com a ligação anterior libertada.
- Estado terminal coerente: não continuar a tocar som/imagem por baixo de uma mensagem de erro; confirmar libertação, não apenas texto na interface.
- Distinguir frames submetidos, descartados e realmente apresentados. O contador de metadata não prova apresentação física no ecrã.
- Recuperação e contadores respeitam pausa, áudio sem vídeo, nova geração, limite de tentativas e opção do motor.
- Troca automática ExoPlayer→mpv apenas quando o modo escolhido a permite. Em ExoPlayer exclusivo, apresentar falha/ação adequada em vez de contrariar a escolha.

Não usar quedas pontuais de frames no arranque para provocar reconexões. As regras novas de deteção de slideshow precisam de validação com os contadores reais e o watchdog existente.

### R05 — Auto Frame Rate com cadência precisa e regresso ao modo normal

**Decisão: recomendar quando AFR está ativo.**

Evidência local: `FrameRateController.apply` retorna imediatamente quando não encontra modo compatível, podendo conservar o modo pedido para o canal anterior. A medição atual de ExoPlayer usa amostragem de frames, sem o novo `PtsFrameRate`.

Trazer a medição temporal para distinguir 59,94/60 e 23,976/24, restaurar o modo normal quando não existe correspondência e aceitar resultados medidos apenas após estabilização. Manter a opção AFR desligado realmente desligada, a limitação de frequência de alterações e as preferências do sistema/televisor.

O patch nativo de R01 fornece informação útil ao mpv, mas é também necessário consumir e validar a leitura na App/Core. Não aumentar a resolução nem forçar mudanças HDMI em todos os canais.

### R06 — Atributos de áudio coerentes e saída estéreo em Android antigo

**Decisão: recomendar o ajuste específico.**

O foco de áudio local já usa `USAGE_MEDIA`/`CONTENT_TYPE_MOVIE`, mas os builders ExoPlayer examinados não aplicam os mesmos atributos ao player. Alinhar os players com o foco e a consulta de capacidades, mantendo o foco exclusivamente no responsável já existente.

No mpv, em Android até 12, a ausência de saída HDMI identificável deve ter um fallback conservador em vez de anunciar multicanal a colunas estéreo. Adaptar a deteção para Bluetooth/USB e preservar a opção explícita do utilizador: dispositivos ligados não são sempre a saída efetivamente usada.

Isto melhora coerência/compatibilidade; não comprova a causa dos cortes periódicos de AAC/MP2 relatados. Volume interno permanece 100%.

### R07 — Conservar ordem de categorias das outras listas

**Decisão: recomendar. Lacuna confirmada no código local.**

`CustomizationStore.setCategoryOrder` substitui `categoryOrder` pelas chaves recebidas. Ao editar só uma lista, isso pode eliminar a ordenação das restantes.

Trazer `mergeCategoryOrder` e os testes relevantes, conservando chaves de listas não visíveis, categorias ocultas e categorias personalizadas. Não alterar a ordenação manual dos canais nem o modo simples.

### R08 — Eliminar órfãos de personalizações em lotes na sincronização

**Decisão: recomendar. Lacuna confirmada no código local.**

`UserDataResolver.purgeSnapshotOrphans` ainda elimina ordenações e associações a categorias uma a uma. O upstream agrupa por perfil/tipo e usa lotes limitados para diminuir varrimentos e o tempo da transação com muitos itens personalizados.

Integrar apenas esse percurso e os métodos DAO necessários, preservando o mutex/identidades introduzidos na fase 1, transações e limites SQLite. Testar isolamento entre perfis/listas e consistência após falha. Não importar o ficheiro completo nem migrações upstream; a otimização não exige substituir o esquema personalizado.

O ganho depende do volume de personalizações; não existe nesta análise uma medição de velocidade.

### R09 — Reduzir trabalho repetido de diagnóstico sem perder limites de memória

**Decisão: recomendar apenas as partes em falta.**

O código local já tem `DiagnosticWriter` com fila limitada, lotes e rotação limitada. Portanto não substituir esse mecanismo pela implementação upstream com executor sem limite explícito de fila.

Partes úteis: deduplicar avisos nativos repetidos, acrescentar contadores mpv com registo detalhado ativo e pré-compilar as expressões regulares da ocultação de URLs. Manter credenciais ocultas, limites de mensagens/fila/ficheiro e ordem dos eventos.

Registar estado/carregamento/tempo de libertação ajuda a diagnosticar reabertura; aumentar o volume de logs por defeito não melhora o streaming.

### R10 — Retoma numa posição sem abrir primeiro no início e procurar depois

**Decisão: recomendar adaptar aos diferidos e gravações compatíveis.**

O código local ainda executa, após `FILE_LOADED`, um seek com `seekMs / 1000`, perdendo precisão inferior a um segundo. O upstream passa a posição como opção `start` antes de carregar e conserva uma procura alternativa caso seja necessária.

Aplicar apenas aos percursos compatíveis e separar tempo relativo do ficheiro, posição no arquivo e horário do programa. Repor `start=none` em todos os carregamentos sem posição; preservar pausa inicial e geração da sintonia. Pode evitar trabalho/requisição adicional em alguns servidores, mas não garante uma única ligação HTTP em todos os formatos ou servidores.

## 5. Melhorias úteis, mas opcionais ou condicionais

### C01 — Atraso de áudio separado por saída

Útil se o utilizador alternar entre TV/HDMI, soundbar e Bluetooth: guardar correção própria por saída, zero por defeito e incluída no backup. Somar com a correção global/por canal exatamente uma vez e identificar a origem no diagnóstico.

Não corrige silêncios, perdas de rede ou ruído analógico. Em Android antigo, a identificação da saída pode ser estimada; não prometer deteção perfeita só pela lista de dispositivos ligados.

### C02 — «Preferir SDR» com explicação verdadeira

A opção atual de HDR local afeta uma indicação do mpv; não equivale a garantir SDR no ExoPlayer. A opção nova escolhe variantes SDR quando um manifesto oferece SDR e HDR em simultâneo. Se só existir HDR, não cria uma versão SDR nem garante conversão de cores.

Pode substituir uma descrição enganadora após decisão de compatibilidade/migração das definições. Não mudar automaticamente o significado das preferências guardadas, nem copiar sem análise o parser que lê o manifesto inteiro para memória. Preservar variantes válidas e limites existentes.

### C03 — Timeshift após suspensão e fronteiras de continuidade

O upstream expira o buffer estacionado também pelo tempo decorrido durante standby e marca interrupções entre partes. A nossa implementação usa `LocalTimeshiftSession`/servidor/armazenamento diferentes e já marca descontinuidades por sequência e reconexão.

Não importar `TimeshiftManager` completo. Conferir primeiro se a nossa sessão estacionada pode sobreviver à suspensão com validade incorreta; só aplicar uma correção equivalente se esse percurso existir/reproduzir. Testar relógio, suspensão, retenção e quotas; não eliminar uma gravação intencional apenas porque envelheceu um buffer temporário.

## 6. O que não recomendo copiar

1. **Novo interruptor FFmpeg desligado por defeito:** já existe escolha de áudio automático/software nesta versão, relevante para MPEG-L2 na Mortal T1. Não adicionar duas opções contraditórias, desligar um fallback necessário ou fazer a opção «software» continuar a escolher hardware. A política automática pode ser afinada sem importar esse novo default/reinício obrigatório.
2. **Troca automática de motor incondicional:** respeitar «apenas ExoPlayer», modo automático e exceções por canal.
3. **Novo mínimo global de dois segundos para retomar mpv:** respeitar o comportamento de retoma e os buffers personalizados. Não alterar valores saudáveis só por virem no upstream.
4. **Preview do EPG e dois OK obrigatórios:** o guia personalizado não sintoniza preview ao passar o foco, pelo que o bug anunciado não corresponde ao nosso percurso. A pesquisa do EPG também tem estrutura diferente. Não reintroduzir interface retirada.
5. **Desporto, filmes, séries e menus removidos:** fora do âmbito da app TV personalizada.
6. **Reescrita do player, base de dados ou configurações pelo ficheiro upstream inteiro:** pode perder HLS exclusivo, exceções, tempo de reabertura, limites de buffer, +1 h dos diferidos, backups e definições por canal.
7. **Percentagens de ganho, «zero falhas» ou melhoria garantida de A→B→A:** sem ensaio comparativo não são conclusões suportadas. A aplicação pode provar o fecho dos seus recursos, não a libertação de uma sessão no servidor do fornecedor.
8. **Upscaling beta por associação ao patch de dimensões adaptativas:** são coisas diferentes. `UPSCALING-BETA-01` permanece pendente/opcional, sem implementação autorizada.

## 7. Ordem futura recomendada

Estas etapas são uma proposta para estas publicações e **não renumeram** as fases 3–6 do plano principal.

| Etapa | Âmbito | Verificação essencial |
|---|---|---|
| Pré-requisito | Fechar fase 2 em curso e snapshot | Revisão, testes App/Core, compilação/lint e igualdade ZIP/fontes |
| A — Integridade e compatibilidade | R07, R08, R01, R02 | Categorias em duas listas; resync; rollback nativo; H.264/HEVC em direto/diferido |
| B — Reprodução e fluidez | R03, R04, R05, R06 | Comando durante attach/detach; A→B→A; pausa; Exo exclusivo; AFR; áudio em boxes antigas/recentes |
| C — Eficiência e retoma | R09, R10 | Logs limitados e sem dados privados; retoma/seek correto sem estado herdado |
| D — Escolha posterior | C01, C02 e C03 quando aplicável | Saídas de áudio; HDR/SDR real; suspensão/timeshift com quotas |

Para cada etapa: alterações pequenas, testes específicos e compilação/lint conforme o âmbito. A confirmação de áudio/vídeo, superfície, AFR, fornecedor e memória exige APK numa box real. Conservar modo simples, volume 100%, HLS exclusivo, perfis/fontes e configurações por canal.

## 8. Fontes primárias

- [OwnTV 5.2.1 — publicação](https://github.com/ahXN00/OwnTV/releases/tag/v5.2.1)
- [Core 1.0.69 — publicação](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.69)
- [libmpv 2026.10.4 — publicação](https://github.com/ahXN00/OwnTV_libmpv/releases/tag/v2026.10.4)
- [OwnTV — comparação 5.2.0…5.2.1](https://github.com/ahXN00/OwnTV/compare/v5.2.0...v5.2.1)
- [Core — comparação 1.0.68…1.0.69](https://github.com/ahXN00/OwnTV_Core/compare/core-1.0.68...core-1.0.69)
- [libmpv — comparação 2026.10.3…2026.10.4](https://github.com/ahXN00/OwnTV_libmpv/compare/v2026.10.3...v2026.10.4)
- [Patches FFmpeg da libmpv 2026.10.4](https://github.com/ahXN00/OwnTV_libmpv/tree/v2026.10.4/buildscripts/patches/ffmpeg)
- [PTS e cadência no Core](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/player-core/src/main/java/tv/own/owntv/player/PtsFrameRate.kt)
- [Ordenação de categorias](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/core/src/main/java/tv/own/owntv/core/customize/CustomizationStore.kt)
- [Eliminação de órfãos em lotes](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/core/src/main/java/tv/own/owntv/core/backup/UserDataResolver.kt)
- [Recuperação do player em direto](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt)
- [Operações nativas, superfícies e retoma](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt)
- [Seleção SDR de variantes HLS](https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.69/player-core/src/main/java/tv/own/owntv/player/SdrVariants.kt)

Entrega: apenas este relatório. Sem mudanças ao código, dependências, base de dados, snapshot, plano de execução ou APK.

## 9. Implementação posterior autorizada — primeira etapa

Em 22/10/2026, o utilizador autorizou começar pela primeira etapa. Foi fechado primeiro o pré-requisito da fase 2 anterior, e foram implementados **R07, R08, R01 e R02**. As frases de entrega e estado anteriores descrevem a análise original.

- Resultado automático final: **2 312 testes JVM + 6 testes da ferramenta de snapshot aprovados**, compilação debug/release da App, lint vital de release e preparação das bibliotecas nativas aprovados.
- Snapshot final: 1 030 inputs, SHA-256 `15a5cbe97bcf5682a8ed24609c1e8ac31b0eb08e07203337fc87afed1ccc425b`, conferido com os fontes. Configuração normal de build confirmada.
- Registo completo: [IMPLEMENTACAO_UPSTREAM_ETAPA_A_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_A_2026-10-22.md).
- Sem APK ou teste físico da box. Sem ganhos percentuais medidos. O Core personalizado não foi substituído pelo upstream integral.
- **Próxima etapa à data do fecho A: B — R03/R04/R05/R06.** O estado posterior está na secção 10. A etapa C e C01/C02/C03 mantêm-se pendentes nos termos desta análise.

## 10. Implementação posterior autorizada — etapa B

Foi autorizada e implementada a etapa B — R03/R04/R05/R06. Validação final: **2 331 testes JVM + 6 testes de snapshot aprovados**, compilação debug/release, lint vital e bibliotecas nativas aprovados. Snapshot final de 1 035 inputs, SHA-256 `350b830685de9e7e2b1453cc4a5f3a48f41a1bd8db03659e52a885e735463fc0`, conferido com os fontes; configuração normal confirmada. Ver [IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md) para implementação, backup, snapshot, resultados e aceitação física pendente.

A destruição da superfície mantém uma espera de segurança conforme o contrato Android; não foi aplicado um timeout que permita continuar a usar a janela destruída. As preferências de motor, HLS, buffers, reabertura e áudio foram preservadas. Não há medição de ganhos percentuais ou garantia de correção do A→B→A do fornecedor.

Próxima etapa: **C — R09/R10**. Opcionais C01/C02/C03 e restantes fases do plano principal continuam pendentes.

## 11. Implementação posterior autorizada — etapa C

Etapa C — R09/R10 implementada e validada automaticamente: **2 347 testes JVM + 6 testes de snapshot aprovados**, compilação debug/release, lint vital, bibliotecas nativas e configuração normal de build aprovados. Snapshot de 1 039 inputs, SHA-256 `6eabcd44c622925b2f505a10559a4320606e08113ac57728d941f7feeaa3c1f4`, conferido com os fontes. A sequência recomendada A–C ficou fechada no código; testes físicos na box continuam pendentes. Registo e ponto de continuação em [IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md). Não foram aplicadas as opcionais C01/C02/C03 nem renumeradas as fases do plano principal.


## 12. Implementação posterior autorizada — etapa D

Após autorização expressa, C01/C02/C03 foram implementadas seletivamente e validadas. Sincronização por família de saída (zero por defeito, escolha automática/manual, backup), preferência SDR HLS no ExoPlayer (desligada por defeito, captura limitada, sem conversão HDR) e correções confirmadas de expiração/retoma da cópia temporária após suspensão. Mantêm-se as limitações de deteção por saída e de manifestos suportados explicitadas na interface e no registo.

Resultado final: **2 362 testes JVM + 6 testes de snapshot aprovados**, compilação debug/release, lint vital, bibliotecas nativas e configuração normal de build aprovados. Snapshot de **1 047 inputs**, SHA-256 `eb701f07d10fe9fd843faba4ed0b474b0100d6b8570480fb3449ab08a6ea4928`, conferido com o Core externo.

Detalhes, defaults, limites e aceitação na box: [IMPLEMENTACAO_UPSTREAM_ETAPA_D_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_D_2026-10-22.md). Sem APK ou teste físico. As referências anteriores a opcionais pendentes descrevem os estados anteriores a esta autorização. As fases 3–6 do plano principal não foram executadas automaticamente por esta etapa.
