# OwnTV — revisão especializada e decisão do council

Data: 2026-10-07. Âmbito: aplicação, usabilidade, simplicidade, memória, gravação HLS e desempenho local. Análise sem implementação.

Projetos analisados: `C:\Users\renat\Downloads\OwnTV-main` e `C:\Users\renat\Downloads\OwnTV_Core`.

## Método e limites

Revisão por agentes especializados em interface, desempenho e fiabilidade, seguida de decisão do council e verificação dos pontos principais no código atual. Foram consultadas fontes oficiais Android e HTTP. Não foram executados testes nem medições em dispositivos nesta revisão. Uma fragilidade no código não demonstra que tenha causado uma falha observada durante a reprodução.

Preservar TV DIRETO, comando e toque, diferidos, gravações, HLS exclusivo, volume a 100% e configurações individuais existentes. Não reintroduzir favoritos, histórico, filmes, séries ou PiP. A atualização libmpv 2026.10.1 já foi realizada e não faz parte das alterações propostas aqui.

## Decisão do council

Primeiro corrigir integridade de configurações e resultados. Depois limitar operações de importação e melhorar gravações. Finalmente melhorar utilização no smartphone e otimizar operações locais mediante medição. Não aumentar atrasos ou buffers globais nem reiniciar os motores indiscriminadamente.

## Fase 1 — integridade e consistência

### A1 — edição segura das versões de um canal

**Evidência:** `app/src/main/java/tv/own/owntv/features/live/ChannelVersionsDialog.kt:82` e `:84` enviam uma nova ordem baseada na lista apresentada. `LiveViewModel.kt:1352` grava assincronamente. Ações rápidas podem competir usando uma ordem desatualizada. Em `ChannelVersionsDialog.kt:135`, adicionar uma versão fecha o diálogo antes de confirmar a gravação. As setas de ordenação não fecham o diálogo.

**Implementação:** usar um rascunho local ou serializar alterações com o estado mais recente; associar operações ao perfil/grupo/sessão; apresentar estado de gravação e falha; fechar a adição apenas depois do sucesso. Reutilizar os princípios do editor de ordem geral já existente.

**Aceitação:** várias alterações rápidas mantêm a ordem final; falha mantém o rascunho e permite repetir; mudar de perfil durante a gravação não altera o grupo errado; comando e toque funcionam.

### A2 — impedir perda silenciosa de configurações individuais

**Evidência:** `core/src/main/java/tv/own/owntv/core/settings/ChannelPlaybackConfig.kt:51` lê no máximo 1000 entradas, mas a escrita não tem o mesmo limite. `ManualTsChannel.kt:16` apresenta o mesmo padrão. Entradas acima desse limite podem desaparecer numa leitura posterior.

**Implementação:** tornar leitura e escrita coerentes, preservar dados existentes e substituir truncamento silencioso por validação explícita ou armazenamento adequado. Decidir limites com base em ficheiros reais e compatibilidade de backups.

**Aceitação:** round-trip e atualização com mais de 1000 entradas preservam todas as configurações aceites; duplicados e dados inválidos têm tratamento definido. Isto não explica, por si só, lentidão numa lista de 240 canais.

### A3 — pesquisa atualizada ao mudar perfil ou lista

**Evidência:** `app/src/main/java/tv/own/owntv/features/search/SearchViewModel.kt:71` observa o contexto, mas `:78` inicia resultados apenas quando o texto muda. A pesquisa lê o contexto em `:132`, sem esse contexto fazer parte do gatilho.

**Implementação:** combinar texto, perfil e origens; cancelar a pesquisa antiga e verificar a identidade do contexto antes de publicar resultados.

**Aceitação:** manter a mesma pesquisa e trocar de perfil/lista atualiza os resultados; resposta tardia do contexto anterior não aparece; exclusão de filmes/séries mantém-se.

## Fase 2 — importações e gravação robustas

### A4 — leitura limitada dos backups

**Evidência:** `core/src/main/java/tv/own/owntv/core/backup/BackupContainer.kt:168` lê o ficheiro completo, e `:198` expande entradas ZIP com readBytes. Há cópias adicionais na desencriptação. Não há limites prévios equivalentes sobre todo o ficheiro e expansão.

**Implementação:** impor limites durante a leitura para ficheiro de entrada, JSON, tamanho de cada entrada, total descomprimido e quantidade de entradas. Aplicar a proteção também à inspeção das secções. Usar orçamentos existentes de exportação e amostras válidas; rejeitar antes de restaurar dados. Manter backups antigos válidos.

**Aceitação:** backups normais, encriptados e próximos dos limites funcionam; ficheiro excessivo, ZIP de expansão excessiva e entradas malformadas são rejeitados sem alteração parcial. Risco estrutural de memória; não há prova de crash físico nesta revisão.

### A5 — importar fotografias com menos memória e sem perder o avatar anterior

**Evidência:** `core/src/main/java/tv/own/owntv/core/profile/ProfileAvatarStore.kt:42` descodifica a imagem completa antes de reduzir para 512 px. `:54` escreve diretamente no destino e `:55` ignora o resultado de compressão. A operação já corre em IO.

**Implementação:** obter dimensões primeiro e descodificar com amostragem; usar entrada reabrível ou preparação limitada; escrever num ficheiro temporário na mesma pasta e substituir apenas após sucesso. Preservar o avatar anterior em qualquer falha.

**Aceitação:** imagens grandes, orientação, entrada inválida, erro de escrita e cancelamento; medir pico de memória. Referência: https://developer.android.com/topic/performance/graphics/load-bitmap

### A6 — respeitar o servidor nas tentativas de gravação HLS

**Evidência:** `core/src/main/java/tv/own/owntv/core/recording/RecordingEngine.kt:711` reduz falhas HTTP a false. `HlsSegmentRetry.kt:15` usa espera fixa de 500 ms, até três tentativas, sem conhecer o código HTTP ou Retry-After. Já existe cancelamento em recordingHttp e atualização de URL assinada: preservar ambos.

**Implementação:** resultado tipado com código HTTP e Retry-After; reutilizar HttpRetryAfter; esperar de forma cancelável quando indicado pelo servidor, dentro da validade do segmento/janela. Erros permanentes não devem provocar tentativas inúteis. Se o segmento expirar, assinalar a falha/lacuna, sem espera indefinida.

**Aceitação:** 429/503 com segundos e data HTTP, 403, timeout, cancelamento e segmento expirado. Esta correção é da gravação, não do zapping normal de ExoPlayer/mpv. Referência: https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after

## Fase 3 — smartphone e otimização medida

### A7 — guia e versões mais fáceis de usar por toque

**Evidência:** `app/src/main/java/tv/own/owntv/features/epg/EpgScreen.kt:957` trata toques no corpo do guia; o deslocamento horizontal está no eixo em `:1168`. `ChannelVersionsDialog.kt:59` e `:79` organizam ações em linhas fixas. `ui/components/ChannelPressInput.kt:79` expõe semanticamente reprodução e menu, sem ação dedicada para escolher versão.

**Implementação:** permitir arrastar horizontalmente o corpo do EPG usando o mesmo estado de deslocamento; distinguir toque e arrasto e preservar deslocamento vertical/comando. Adaptar ações das versões à largura disponível. Acrescentar ação acessível de escolha de versão, preservando os gestos de 2 s e 3 s.

**Aceitação:** smartphone estreito, texto ampliado, toque/arrasto, leitor de ecrã e D-pad; sem cliques involuntários ao arrastar ou regressões no foco da TV.

### A8 — reduzir trabalho de armazenamento e das playlists de gravações locais

**Evidência:** `core/src/main/java/tv/own/owntv/core/storage/MediaStorageBudget.kt:28` faz verificações de armazenamento sob o mutex também usado nas reservas/escrita. Já existe limitação de atualização a cada 5 s. `core/recording/RecordingPlaybackStore.kt:65` constrói a playlist sob synchronized e `:76` formata cada segmento em cada pedido; publicação/leitura partilham o bloqueio.

**Implementação:** primeiro medir duração das verificações, espera do escritor e construção da playlist. Se relevante, fazer verificações lentas fora do bloqueio e aplicar um snapshot com revisão/validação; nunca comprometer quotas. Cachear ou atualizar linhas da playlist por revisão, com bloqueio curto. Preservar segmentos, diferidos, descontinuidades e ENDLIST.

**Aceitação:** gravação e reprodução simultâneas, armazenamento externo lento, remoção do dispositivo, quota quase cheia e longa gravação; comparar latências e memória antes/depois. Não prometer mais FPS sem medição.

## Inovações opcionais, depois das correções

- **I1 — aplicar ajustes a vários canais:** selecionar canais e campos, apresentar pré-visualização, preservar os restantes campos e a herança de definições. Útil para configurar um grupo sem repetir operações.
- **I2 — estimativa de capacidade de gravação:** mostrar espaço utilizável e duração aproximada com margem conservadora baseada no bitrate medido; indicar que é estimativa, sujeita a bitrate variável e disponibilidade do armazenamento.

## Propostas que não avançam agora

- Mais atraso obrigatório, buffers maiores ou reinício global dos motores: sem evidência específica podem piorar o arranque e o zapping.
- Alterações globais ao HLS por causa de um canal problemático do fornecedor.
- Nova limpeza de consultas de filmes/séries: Home já carrega liveOnly e pesquisa já exclui a sincronização desses conteúdos.
- Afirmar que todo o EPG corre na thread principal: leituras/deduplicação já usam IO e ordenação usa Default. Não refazer sem perfil de desempenho.
- Retocar paginação/caches indiscriminadamente ou duplicar diagnóstico já existente. Usar os eventos atuais e métricas Android/Media3: https://developer.android.com/media/media3/exoplayer/analytics
- DASH na gravação: avaliar depois uniformização do cancelamento das chamadas diretas em RecordingEngine.kt:953 e :1077; fora da prioridade atual HLS.

## Como começar a implementação

1. A1–A3: alterações pequenas e testes de persistência/concorrrência/contexto.
2. A4–A6: proteção de entrada e tratamento HTTP, com casos válidos e falhas controladas.
3. A7–A8: validação física em smartphone/TV e medições antes/depois; otimizar apenas o trabalho demonstrado.

Compilação e testes unitários são necessários quando aplicáveis, mas não substituem testes reais de foco, som, fluidez, servidor e armazenamento. Nenhuma alteração da aplicação foi feita nesta revisão.
