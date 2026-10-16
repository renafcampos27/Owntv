# Core 1.0.67 — integração seletiva proposta

Data: 12/10/2026. Apenas análise; nenhuma alteração funcional, dependência, base de dados ou snapshot foi aplicada.

## Base da comparação

- Upstream: tag core-1.0.67, commit 35beff0c8af5ee00c89c78cfeb0134822e6bf85e; comparação de código com core-1.0.66 numa cópia temporária isolada.
- Local: C:/Users/renat/Downloads/OwnTV_Core e OwnTV-main, incluindo as alterações recentes dos lotes 1–4.
- Consultado ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md; a integração anterior da 1.0.66 é seletiva, não uma cópia integral.
- O esquema upstream é v48. O local é v50, com migrações próprias de gravações. Nunca importar a numeração ou o esquema upstream por substituição.
- Não foram executados testes de integração da 1.0.67 nem reprodução física. Os resultados de validação dos lotes anteriores não validam esta proposta.

## Decisões

| Alteração | Comparação local | Recomendação |
| --- | --- | --- |
| Classificação dos erros de backup | decryptContainer ainda converte qualquer falha de decryptBytes em WrongPasswordException e trata cabeçalho de derivação inválido como palavra-passe errada. | Prioritário: distinguir corrupção, autenticação GCM e erros de execução; não repetir um pedido de senha para falha de memória ou cabeçalho inválido. |
| Recusar upload HTTP incompleto | readBody pode terminar antes de Content-Length e devolver o conteúdo parcial. | Prioritário: recusar antes de guardar/restaurar; tratamento controlado de EOF e teste com socket interrompido. |
| Menos memória nos backups | Local já lê ZIP por InputStream, usa offset/length na desencriptação e possui leitura JSON isolada e limites adicionais. | Já presente em grande parte. Preservar a implementação local e os limites; não substituir pelo helper upstream mais simples. |
| Limpar buffering ao parar | LivePreviewEngine local já limpa explicitamente _buffering em stop. | Já presente. |
| Ficheiro ausente | Upstream acrescenta verificação periódica de ficheiros de downloads/gravações. | Útil nas gravações locais: estado explícito para ficheiro ausente, USB desligado ou permissão perdida. Sem apagar registos nem criar ficheiros substitutos. |
| Razões de falha de download | Upstream guarda reason/httpCode. Local já tem motivos de gravação, quota, espaço, USB e limite de ficheiro, mas o percurso DownloadEngine antigo não tem essa classificação. | Adaptar apenas as lacunas dos percursos utilizados. Não reintroduzir downloads de filmes/séries para obter esta melhoria. |
| Prioridade de ver sobre descarregar | DownloadWatchGuard pausa downloads quando deteta reprodução numa origem de uma ligação e oferece escolha. Local já tem OpenStreamRegistry e admissão atómica das capturas. | Reutilizar o registo local. Qualquer preempção deve concluir cancelamento/fecho antes do novo pedido; não adicionar um segundo controlador de sessões. |
| Downloads paralelos | Novo motor transfere blocos HTTP Range, aumentando até oito ligações. É destinado a ficheiros de filmes/episódios. | Não ativar globalmente nem portar para HLS direto/gravações. Avaliar separadamente apenas para ficheiros estáveis e fontes autorizadas. |
| URLs M3U com &amp; | Novo XtreamM3uLink limpa a representação HTML. | Recomendada a limpeza de URLs de importação, sem alterar URLs assinadas, credenciais ou nomes armazenados de forma indiscriminada. |
| get.php como Xtream | Novo parser extrai credenciais e testa player_api antes de importar como Xtream. | Útil para novas importações; preservar modo só TV, HLS estrito, UA/Referer, EPG e intervalo de atualização. Não converter listas existentes silenciosamente. |
| ClearKey inline | DRM local aceita servidor de licença, sem mapa de chaves inline. Upstream acrescenta parser e callback local Media3. | Opcional, se a lista fornecer estas chaves. Não é uma correção de HLS comum nem do bloqueio A–B–A. |
| Software de vídeo acima de 1080p | Upstream permite contornar o teto de recuperação por software, desligado por defeito. | Não recomendado para as boxes visadas como otimização. Pode aumentar carga e fotogramas perdidos. Manter hardware e recuperação limitada. |
| Fallback EPG no resumo atual/seguinte | Novo parâmetro providerFallback em GuideReader.slots, desligado por defeito, pensado para poucos canais no ecrã. Local já usa fallback nas linhas e nos intervalos ausentes. | Baixo ganho imediato. Se necessário, limitar aos canais visíveis/selecionados e reutilizar cache, cancelamento e correção horária local. Não fazer um pedido por todos os canais ao navegar. |
| Restauro de categoria | CategoryRestore prepara resumo de ocultos, itens movidos e ordem manual antes de restaurar. | Opcional útil: ação única com confirmação e âmbito claro. Mostrar que voltar a tornar visível um item atua globalmente, não só nessa categoria. |
| Filtros de pesquisa | SearchKind separa canais e programas EPG, além de filmes/séries. | Adaptar só canais/programas se necessário. Aplicar filtro também à consulta, evitando calcular resultados excluídos apenas para os esconder depois. |
| ProviderTags/Stage/episódios/locales | Novos elementos de apresentação, tratamento de títulos e alterações VOD/i18n. | Não prioritários para esta app TV só canais. ProviderTags é apresentação; não substituir agrupamento/prioridades locais nem modificar nomes usados como chave. |

## Riscos concretos de copiar o novo motor de downloads

1. Limite desconhecido permite até oito ligações; isto não prova que o fornecedor as suporte.
2. connectionCap conserva um mínimo de uma ligação mesmo com orçamento esgotado. No local deve haver espera quando não existe licença de ligação, usando admissão atómica.
3. DownloadWatchGuard reage à abertura da reprodução: não é, por si só, prova de que o download foi fechado antes do primeiro pedido do leitor.
4. Maior paralelismo de ficheiros não melhora automaticamente playlists/segmentos HLS nem a libertação de sessões remotas.
5. Preservar no local quotas, SAF, spool, retoma, identidade da captura, fecho HTTP e finalização independente já corrigidos.

## Plano recomendado, sem autorização de implementação

### Lote A — Backups e transporte

Corrigir classificação de falhas de desencriptação e recusar corpos HTTP truncados. Preservar GCM, limites de tamanho, autenticação e matriz de permissões. Testar senha incorreta, cabeçalho inválido, dados truncados, erro de execução e upload interrompido; o restauro não pode iniciar com um corpo incompleto.

### Lote B — Disponibilidade e diagnóstico de gravações

Verificar ficheiros/volumes em IO apenas enquanto o ecrã estiver ativo, ao abrir e ao regressar à app. Distinguir inexistência de permissão/USB indisponível, mantendo a possibilidade de recuperação. Aproveitar os motivos locais existentes e acrescentar apenas os que faltam. Novas colunas, se necessárias, exigem migração local aditiva posterior à v50, schemas e teste de conservação de dados; não copiar MIGRATION_47_48 upstream.

### Lote C — Importação de listas

Limpeza HTML de URL e reconhecimento validado de get.php em novas importações. Manter credenciais fora de logs, testar percent-encoding/subdiretórios, cancelamento e falha de player_api com fallback M3U. Preservar TV-only, HLS e preferências locais.

### Lote D — Sessões e conveniência, condicionado à necessidade

Avaliar arbitragem de reprodução/captura com o controlador local e testar que o novo pedido só começa após libertação local confirmada. Opcionalmente, restauro de categoria e filtros canais/programas. ClearKey apenas se houver canais relevantes na lista. Nenhum destes itens justifica aumentar globalmente ligações, remover proteções de vídeo ou mudar tempos de reabertura.

## Efeito esperado

Backups deixam de apresentar diagnósticos enganadores; envios incompletos são recusados cedo; gravações com USB desligado deixam de parecer prontas; novas listas podem ser reconhecidas com menos intervenção. Esta release não contém evidência suficiente para declarar resolvido o bloqueio A–B–A nem para prometer menos fotogramas perdidos em HLS. Nas alterações player-core examinadas, o núcleo das novidades é ClearKey, opção de vídeo por software, bloqueio de preview durante download e limpeza de buffering.

## Fontes primárias

- https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.67
- https://github.com/ahXN00/OwnTV_Core/compare/core-1.0.66...core-1.0.67
- https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.67/core/src/main/java/tv/own/owntv/core/backup/BackupContainer.kt
- https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.67/core/src/main/java/tv/own/owntv/core/companion/CompanionHttpProtocol.kt
- https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.67/core/src/main/java/tv/own/owntv/core/download/DownloadEngine.kt
- https://github.com/ahXN00/OwnTV_Core/blob/core-1.0.67/core/src/main/java/tv/own/owntv/core/drm/DrmConfig.kt
