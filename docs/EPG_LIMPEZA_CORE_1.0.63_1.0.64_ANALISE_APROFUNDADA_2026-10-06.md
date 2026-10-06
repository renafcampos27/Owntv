# EPG e limpeza do Core — análise aprofundada

Data: 06/10/2026. Código local inspecionado; alterações EPG/limpeza propostas, não implementadas. Apenas a deteção de leitores externos foi autorizada e aplicada nesta revisão.

Atualização posterior: recomendações autorizadas e implementadas em 06/10/2026. Este ficheiro conserva a análise anterior; consultar o [registo de implementação e validação](IMPLEMENTACAO_EPG_ROBUSTEZ_E_LIMPEZA_2026-10-06.md).

## 1. Implementação concluída: leitores externos

Acrescentadas oito assinaturas ACTION_VIEW no manifesto do Core: video/*, application/x-mpegURL e protocolos http, https, rtsp, rtmp, udp e mms. Preservados ExternalPlayerLauncher, cabeçalhos, marcador de retorno à lista e proteção contra aberturas repetidas. Sem QUERY_ALL_PACKAGES, migração ou mudança de motor.

Validação: processStandardDebugManifest passou; o manifesto mesclado da app contém as oito assinaturas. O projeto usa o Core local por owntv.corePath. Não foi gerado APK. Descoberta e regresso do leitor externo ainda exigem teste na box/telemóvel com o novo APK.

## 2. O que a 1.0.63 altera realmente no EPG

EpgDao adiciona NULL AS categories/year/rating/lengthMin/episode às duas consultas leves. Esses campos pertencem ao EpgProgrammeEntity upstream e já existiam na 1.0.62. A alteração completa a projeção e evita diferenças entre o resultado da consulta e a entidade que Room materializa; não acrescenta dados, não corrige fusos, não muda seleção nem URLs de diferido.

O EpgProgrammeEntity local tem id/sourceId/epgChannelId/startMs/stopMs/title/description/contentHash. As consultas leves locais já devolvem esses campos. guideShiftMs é transitório e ignorado pelo Room, reconstruído após leitura. Importar cinco NULL adicionais sem importar o modelo de detalhes não oferece benefício. Importar o modelo exigiria uma decisão separada de funcionalidades, esquema e backup.

**Decisão:** não copiar a alteração EpgDao 1.0.63. Corrigir os problemas locais abaixo tem mais valor. A 1.0.64 não altera EPG.

## 3. Achados locais e correções propostas

### E1 — P1: reposição a partir da cache pode substituir dados válidos por dados incompletos

Evidência: EpgRepository.kt, storeProgrammesForIdsFromCache, linhas 609–660. Os callbacks acrescentam programas a collected enquanto o ficheiro é lido; runCatching regista uma falha, mas conserva a parte já recolhida. Depois clearChannelForSources usa todos os storeIds dos ficheiros candidatos, incluindo os que falharam. A eliminação e as inserções em lotes não estão dentro de uma única transação.

Cenário: cache A válida e cache B truncada. Existem programas válidos de B na base; A produz linhas para o canal. A reposição pode eliminar as linhas de B e substituí-las por uma resposta parcial, ou deixar o canal vazio se for interrompida depois do DELETE.

Proposta: aceitar apenas ficheiros totalmente analisados, recolher resultados por fonte/canal, preservar fontes com falha e fazer substituição atómica dos pares autorizados dentro de transação. Propagar CancellationException. Limitar a memória de preparação; não transformar isto numa carga de todo o EPG em RAM.

Teste necessário: duas fontes, uma falha a meio; cancelamento entre eliminação e inserção; ausência de um dos canais pedidos. Dados anteriormente válidos da fonte falhada devem permanecer.

### E2 — P1: grelha, barra do direto e seletor de diferidos não usam o mesmo conjunto de fontes

Evidência: GuideReader.row lê programas por epgChannelId sem filtrar sourceId. LiveEpgReader.timelineProgrammes (linha 403) e catchupProgrammes (linha 420) filtram depois sourceId contra liveSourceIds + EPG sources. programmeAfter volta a ler sem esse filtro, e calcula uma lista ids que não utiliza.

Consequência condicional: um programa pode aparecer no Guia e não aparecer na barra ou na lista de diferidos, consoante a origem que o armazenou. GuideInheritance também escolhe a chave por disponibilidade sem aplicar esse filtro posterior.

Proposta: definir uma política comum de fontes elegíveis e usá-la na escolha da chave, grelha, agora/seguinte, barra, lista de arquivo e continuação. Evitar simplesmente alargar tudo sem definir precedência entre feeds. Manter canal/sourceId/remoteId originais ao construir o pedido: herdar EPG nunca significa reproduzir o stream da outra fonte.

Teste necessário: EPG numa fonte diferente da playlist, duas fontes com a mesma chave, fonte removida, perfil/lista selecionada e herança de EPG entre versões do canal.

### E3 — P1: agora/seguinte pode ficar preso no mesmo canal

Evidência: LiveViewModel.kt, browseNowNext (linha 507) e nowNext (linha 521), só reagem ao canal, epgRefresh e, no primeiro caso, atraso de consulta. Não existe gatilho de passagem do tempo nesses fluxos. A expiração da cache em LiveEpgReader apenas atua quando alguém volta a consultar; não emite por si uma atualização. A barra timelineProgrammes possui um relógio separado de cinco minutos. O Guia observa alterações de tabelas, mas estes fluxos do direto não observam esse evento.

Consequência: permanecer no mesmo canal pode deixar a informação do programa anterior no leitor/lista; uma sincronização que muda o título/hora também não garante atualização imediata destes fluxos.

Proposta: atualizar na próxima fronteira do programa ou, como fallback, no minuto, apenas enquanto a informação estiver visível. Associar alterações do EPG a invalidação com debounce. Não aumentar polling de rede nem atualizar toda a lista a cada segundo; manter cache e pedidos deduplicados.

Risco adicional a testar: epgOffset começa com 0 e ignora a primeira emissão persistida para invalidação. Uma leitura muito precoce pode usar 0 antes de chegar o +60, sem esse primeiro valor emitir epgRefresh. Garantir snapshot inicial ou combinar explicitamente o offset no fluxo de leitura, preservando a prevenção do NPE de inicialização.

### E4 — P1 de coerência visual: hora original e posição corrigida são coordenadas diferentes

Evidência: EpgShift desloca startMs/stopMs e conserva as etiquetas originais em displayStartMs/displayStopMs. GuideProgrammeCells.layout desenha e seleciona pelos tempos operacionais; a régua do EpgScreen usa o relógio real. Textos usam displayStartMs.

Exemplo: programa original 20:00–21:00, correção +60. O pedido/seleção utiliza 21:00–22:00 e a etiqueta mantém 20:00–21:00. É o contrato pedido pelo utilizador, mas a etiqueta pode parecer deslocada uma hora relativamente à régua. Não é prova de que o pedido esteja errado.

Proposta: preservar +1 e etiquetas originais, tornar explícita a diferença de referência e testar clique, marcador agora, passagem de dia e relógio do diferido. Não subtrair o shift globalmente da régua partilhada: canais podem ter offsets diferentes. Não alterar silenciosamente a preferência do utilizador.

As três operações devem continuar distintas: correção do guia, correção adicional do pedido catchupRequestShiftMinutes e fuso usado para formatar a URL. Valores acumulados podem deslocar o pedido duas vezes. Sem resposta real do fornecedor e configuração instalada não é possível atribuir a causa exata da hora errada.

### E5 — P2: configurações de diferido por fonte estão armazenadas mas não são consumidas

Evidência: SourceEntity tem catchupTimezone/catchupOffsetMin (ProfileEntities.kt, linhas 129–130), com migração e backup. LiveArchiveUrls.forProgramme usa settings.correctCatchupProgramme e settings.resolveCatchupTimeZone globais; forTimeshift usa o tz recebido e correção global. Não encontrei consumo desses campos de SourceEntity na resolução de URLs.

Proposta: se esses campos forem destinados a preferências efetivas por lista, criar uma resolução única fonte → fallback global, documentando o significado do offset. Aplicar aos programas, rewind e downloads/gravação de diferidos; não confundir offset UTC com avanço do instante do pedido. Sem interface que os configure, tratá-los como funcionalidade incompleta, não prometer uma correção automática.

### E6 — P2: eliminação de duplicados e leitura não têm a mesma regra

Evidência: EpgDao.collapseDuplicateProgrammes compara títulos literalmente e desempata durações iguais pelo menor id. EpgDedupe normaliza caixa/espaços e, com duração igual, mantém a primeira entrada ordenada por início/fim. O comentário do DAO afirma equivalência exata, mas ela não existe. A eliminação é global entre fontes e favorece a maior duração, não a fonte mais fiável.

Experiência SQLite em memória: id 20, início 0, fim 100; id 10, início 10, fim 110; mesmo canal/título. O SQL conserva id 10, enquanto o algoritmo Kotlin conserva id 20. Uma sincronização pode, portanto, mudar o início escolhido sem mudar o programa. Isto não demonstra a origem de um desvio de uma hora.

Proposta: definir precedência determinística de fonte e regra comum. Antes de continuar a apagar dados entre feeds, avaliar deduplicação só na leitura ou preservação da identidade original do arquivo. Corrigir o comentário. Testar durações iguais, títulos normalizados, sobreposições intercaladas e repetições legítimas. Não unir programas diferentes só porque se sobrepõem.

### E7 — P2: fallback Xtream curto não recupera cinco dias de histórico

Evidência: GuideReader.row, quando não há dados guardados, chama providerProgrammes → getShortEpg. O XtreamClient local tem apenas o endpoint de EPG curto; não encontrei integração Xtream get_simple_data_table. Esse fallback informa agora/próximos programas, não preenche uma janela antiga arbitrária.

Proposta: manter XMLTV como fonte principal; se faltar uma janela antiga, avaliar consulta histórica por canal e janela quando o painel suportar, com cache limitada, uma consulta em curso por chave e cancelamento. Não fazer pedidos para todos os canais ao abrir o Guia. Não anunciar dias de arquivo que o fornecedor não entrega: dados de programação e gravações disponíveis são serviços diferentes.

### E8 — P2: cancelamento tratado como falha comum em caminhos de EPG

Evidência: runCatching envolvendo chamadas suspensas nas descrições de GuideReader/LiveEpgReader, reposição da cache e resolução Stalker em LiveArchiveUrls. Pode converter CancellationException em null/erro normal. Os ViewModels já contêm várias proteções de geração e checkCatchup, que devem ser mantidas.

Proposta: propagar cancelamento explicitamente e conservar o tratamento normal apenas para falhas reais. O maior risco está na reposição com escrita, não numa descrição em falta. Não remover proteções existentes em nome de simplificação.

## 4. Limpeza upstream: vale a pena?

| Mudança 1.0.63 | Avaliação para a variante local |
|---|---|
| db → connection nas migrações | Comparação normalizada confirmou apenas renomeação. Não resolve EPG nem justifica tocar em todas as migrações. |
| Uri.parse → toUri | Equivalência funcional; aplicar apenas nos ficheiros editados por outros motivos. |
| Bitmap.createBitmap → createBitmap | Manutenção; sem benefício comprovado no scrolling/streaming. |
| Guardas Android anterior a 8 removidas | Redundantes com minSdk 26; limpeza segura após confirmar cada chamada, sem retirar verificações necessárias de Android 12/13/14+. |
| LocaleStore SuppressLint UseKtx | Faz sentido manter commit verificado; não converter cegamente em apply. |
| Timeshift sincronizado/smart casts | Não oferece nova otimização de I/O. Não transportar alterações sem comparar a sessão personalizada. |
| Limiar de tradução 70% → 75% | Política de traduções, sem correção de EPG. |

Prioridade real de limpeza: variáveis ids não utilizadas, comentários que descrevem uma regra diferente do código, nomes displayStartMs para valores operacionais e tratamento inconsistente de cancelamento. Não remover gravações, diferidos, migrações históricas nem proteções de geração.

## 5. Ordem proposta de implementação futura

1. E1: reposição segura e atómica da cache, com cancelamento preservado.
2. E2 + E3: política comum de fontes e atualização de agora/seguinte, sem multiplicar pedidos.
3. E4: coerência das referências horárias e testes de arranque com +60; respeitar etiquetas originais.
4. E5 + E6: resolução por lista e precedência entre feeds, com testes antes de qualquer eliminação destrutiva.
5. E7: histórico Xtream sob pedido apenas se necessário e suportado pelo painel.
6. Limpeza pequena nos ficheiros envolvidos; E8 incluído nos respetivos lotes.

O lote 2 anterior (#229, diferido sem imagem) permanece adiado. Estes achados tratam dados/horários/atualizações; não constituem autorização para mudar recuperação ou motor.

## 6. Validação e limites

- Manifesto standardDebug processado com sucesso; oito consultas presentes no resultado mesclado.
- 27 testes existentes passaram: EpgShiftTest 9, CatchupRequestTimeTest 5, XmltvTimeTest 5, EpgDedupeTest 8.
- Esses testes verificam regras locais, não a resposta do fornecedor, o Guia completo ou a descoberta de aplicações numa box real.
- Experiência SQLite confirmou a divergência de desempate; não usou nem alterou dados reais.
- Sem implementação EPG/limpeza, sem APK e sem reprodução física nesta revisão. Causa específica do EPG do utilizador ainda não demonstrada.

Referências: [Android — visibilidade de aplicações](https://developer.android.com/training/package-visibility/declaring), [Android — projeções de consultas Room](https://developer.android.com/training/data-storage/room/accessing-data), [revisão 1.0.63/1.0.64](ANALISE_CORE_1.0.63_1.0.64_2026-10-06.md), [pendências](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md).
