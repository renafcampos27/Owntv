# Integração seletiva OwnTV 5.2.0 / Core 1.0.68

Data: 18/10/2026. App: `OwnTV-main`. Core: `OwnTV_Core` personalizado para TV.

Plano de origem: [análise e decisões](ANALISE_OWNTV_5.2.0_CORE_1.0.68_2026-10-18.md). Implementação autorizada pelo utilizador depois da análise. Não foi substituído o Core pelo upstream.

## Fase A — correções e coerência

- Formatação de datas remove os campos de hora existentes antes de escolher o ciclo 12/24 h. Corrige a chamada das gravações que já incluía `jm`.
- Leitor externo usa o resolvedor normal do Android, conservando candidatos MIME, headers e o marcador de regresso. A escolha persistente depende do resolvedor da box. A leitura bloqueante do idioma deixou de fazer parte do lançamento bem-sucedido.
- Importação de definições valida os passos de seek/recuo, presets de latência/reserva, posição do mini-leitor, atraso de áudio e escalas de legendas. Não aplica parcialmente um backup rejeitado. Campos opcionais desconhecidos continuam compatíveis.
- Atualização automática de EPG respeita o resultado da espera de dois minutos pela reprodução terminar. Reagenda até três tentativas; depois dessa margem pode atualizar com reprodução ativa, para não deixar uma TV continuamente ligada com o guia permanentemente desatualizado. A atualização manual não recebe essa espera. Os intervalos escolhidos mantêm-se.

## Fase B — diferidos e relógios

- `ResolvedArchiveSource` transporta início efetivo apresentado, instante pedido, zona usada, correção e renovação do URL quando necessária.
- Xtream e M3U `xc` sem template próprio alinham o contador ao minuto realmente solicitado. A correção manual continua a atuar no pedido; não acrescenta uma hora ao relógio apresentado.
- Novo modo **Automático (fornecedor)**, geral e por lista. Reprodução, recuo e gravação/download de diferidos partilham a mesma resolução do fuso. Usa a zona válida recebida em `server_info`; sem informação utilizável, recorre ao fuso de fallback. As instalações existentes continuam com a seleção que tinham; não se ativa o novo modo durante a migração. A informação do painel é obtida ao sincronizar a lista.
- O offset inferido de `time_now`/`timestamp_now` é fixo: não demonstra regras históricas de horário de verão. Uma zona IANA válida tem prioridade; a opção manual permanece disponível.
- M3U interpreta `catchup-correction`, `timeshift="N"`, o estilo `default` com os parâmetros de arquivo convencionais e tokens com argumentos, como `{duration:60}` e `{utc:YmdHMS}`. A correção declarada pela lista e a correção manual são valores separados, somados uma vez.
- `{timestamp}` passa a significar o instante atual nos templates do fornecedor; os templates alternativos configurados manualmente conservam a interpretação anterior como início, para não alterar ligações já guardadas.
- Diferidos usam identidade `CATCHUP`, independente de `LIVE`, quando o canal tem identificador estável. Em Stalker, Retry obtém uma ligação nova para o mesmo programa/instante. Mantêm-se as alternativas de formato e os headers locais.
- O registo detalhado inclui `archive_clock` com zona, correção e instantes de pedido/apresentação, sem URL nem credenciais.

## Fase C — memória e timeshift local

- Um monitor partilhado é iniciado depois de a base de dados estar pronta. Em Android 14+, verifica pressão de memória a cada cinco segundos enquanto um dos percursos monitorizados está ativo. Em Android anteriores, continuam os callbacks do sistema.
- A pressão limpa imagens e permite reduzir a cache mpv. Em pressão crítica, os leitores Exo passam a usar o orçamento reduzido na próxima construção/sintonia; não é provocado um reinício imediato do canal atual. Os valores das definições não são substituídos.
- `UI_HIDDEN` não é tratado como pressão crítica. O monitor é único por processo e o início é idempotente.
- O timeshift local mantém a mesma sessão e o mesmo endereço de leitura durante falta de conectividade, aguardando de forma cancelável. Não fecha a cópia para depois tentar reutilizar um endpoint encerrado.
- O tempo offline não consome o prazo de ausência de progresso. Ao regressar a rede, renova as referências; um reinício da sequência do fornecedor é aceite apenas depois dessa recuperação e introduz uma descontinuidade real.
- Falhas HTTP não são convertidas numa espera de conectividade; os limites de tentativas e `Retry-After` continuam em vigor. Sair/fechar cancela a espera. Uma falha permanente não passa silenciosamente um programa antigo para direto.
- Esta adaptação preserva o produtor local existente; não importa `TimeshiftManager`/`TimeshiftDownloader` completos do upstream.

## Fase D — zapping e diagnóstico

- Uma tecla isolada inicia a seleção sem a espera adicional de 500 ms. Pressões separadas por menos de 1,5 s continuam a ser agregadas com 500 ms de espera.
- Mantêm-se os intervalos configurados para voltar a um canal recente, a libertação confirmada dos recursos e o backoff do fornecedor.
- `ZapTiming` mede da tecla até à confirmação de reprodução/primeira imagem no percurso que a expõe. Associa o evento a uma chave estável do canal, permite a transição de motor e rejeita callbacks de seleções anteriores. Não executa trabalho adicional por fotograma.
- Mantido o sistema local de ownership e de confirmação de abertura; não foi instalado um segundo controlador upstream.

## Persistência e distribuição das fontes

- Migração local **50 → 51**, aditiva e idempotente: `sources.serverTimeZone` e `channels.catchupCorrectionMin`.
- Mantidos os campos personalizados das versões anteriores, incluindo os de gravações e reserva. Sem fallback destrutivo.
- A zona do servidor atravessa sincronização, exportação/restauração e merge de fontes. Backups antigos sem o campo conservam o valor existente.
- A correção M3U atravessa parser, importação e hash de conteúdo. O valor zero conserva o comportamento/hash anterior; as listas continuam a ser atualizadas depois de restaurar, conforme a decisão anterior do utilizador.
- Snapshot do Core publicado pelo fluxo existente; arquivo e lock atualizados em conjunto. Não alterar para a versão v49 de base de dados do upstream.

## O que foi preservado

Volume interno a 100%, HLS estrito e exceções por canal, opções de buffer/latência, arranque, modo simples, tempos de reabertura, backups personalizados, gravações, guia/diferidos e a libertação dos pedidos anteriores.

Não incluídos: Sports Hub, alterações de buffer de filmes aplicadas ao direto, reforma Stage/glass, funcionalidades anteriormente retiradas ou a beta de upscaling. Media3 1.11.1 e libmpv 2026.10.3 mantêm-se. Não é apresentada uma cura para cortes periódicos de áudio ou bloqueios impostos pelo fornecedor.

## Validação

- Core: testes JVM dos módulos `core` e `player-core` aprovados, incluindo migração SQLite real, backup, clocks, templates, recuperação de rede, cancelamento e política de zapping.
- Publicação de snapshot: seis testes Python aprovados.
- App debug/release e lint vital: aprovados na verificação final conjunta da App e do Core.
- Não foram gerados APKs, assinaturas, commits ou pushes.

## Ensaios físicos ainda necessários

1. Gerar/instalar o APK e confirmar diferidos com a correção de +1 h, mantendo a hora original apresentada.
2. Testar o modo automático após sincronizar a lista, sem mudar a correção manual durante a comparação.
3. No timeshift local, interromper/restabelecer a rede e testar saída/mudança de canal durante a espera.
4. Comparar uma pressão isolada com pressão contínua; voltar A→B→A respeitando o intervalo configurado.
5. Testar escolha predefinida de leitor externo e um único Voltar para regressar à lista.

Os testes JVM e a compilação não demonstram comportamento de codecs, firmware, HDMI ou servidores reais.

## Resultado final confirmado

- **BUILD SUCCESSFUL** na execução conjunta final (5 min 19 s; não é um benchmark de desempenho).
- **2 248 testes JVM**, sem falhas/erros/ignorados: 1 270 Core, 552 player-core e 426 App. Contagem dos XML finais, sem somar execuções repetidas.
- **6 testes Python** da publicação do snapshot aprovados.
- Verificados: `:OwnTV_Core:core:testDebugUnitTest`, `:OwnTV_Core:player-core:testDebugUnitTest`, `:app:testStandardDebugUnitTest`, `:app:compileStandardReleaseKotlin` e `:app:lintVitalStandardRelease`; a compilação debug é dependência dos testes App.
- Snapshot final: `e2081e78acce95e5ac9b881164c98f066648e6a9400e554c9c9612f527e14890`, 1 007 ficheiros. Verificados SHA-256 e igualdade de todas as entradas com o checkout Core atual.
- Registo: `C:/Users/renat/AppData/Local/Temp/owntv-520-core68-verified.log`.
- As três opções de fuso e a seleção por lista têm painéis limitados à altura disponível e com deslocação por comando. Não foi feito ensaio visual numa box.
- Subsistem avisos anteriores nos testes e de funcionalidades Gradle descontinuadas; não foram desativados checks nem criado um baseline para ocultar erros.

Pode gerar-se o próximo APK no Android Studio a partir deste projeto, após sincronizar o Gradle. Nenhum APK foi produzido nesta intervenção.
