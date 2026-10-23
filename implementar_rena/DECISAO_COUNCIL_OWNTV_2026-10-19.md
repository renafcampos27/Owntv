# OwnTV Android TV/box — síntese do Council: bugs, melhorias e inovação

**Data:** 19/10/2026  
**Checkout analisado:** branch main, commit bdffec4  
**Core personalizado:** snapshot SHA-256 e2081e78acce95e5ac9b881164c98f066648e6a9400e554c9c9612f527e14890

Esta nota organiza as prioridades para implementação e complementa as análises especializadas desta pasta: [análise consolidada das rondas 1–4](ANALISE_OWNTV_2026-10-19.md), [ronda 4](ANALISE_OWNTV_RONDA_4_2026-10-19.md) e [Council Rena](ANALISE_OWNTV_COUNCIL_RENA_2026-10-19.md). A ronda 5 contém o inventário de candidatos em [ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md](ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md); as suas classificações são hipóteses até serem confirmadas nesta síntese e no código.

## Âmbito e método

Foram feitas três leituras especializadas e independentes: reprodução/lifecycle; persistência, backups e rede; interface TV, segurança e build. O Council cruzou os achados, distinguiu defeitos de riscos condicionais e corrigiu severidades exageradas. A revisão foi estática e não alterou código. Não foram executados builds, testes ou ensaios na box nesta ronda.

O registo de implementação de 18/10 já documenta 2 248 testes JVM, seis testes do snapshot, compilação Release e lint vital aprovados. Esses resultados são anteriores a esta revisão e não cobrem os novos cenários descritos abaixo. Não há validação física de codecs, firmware, HDMI, acessibilidade com TalkBack ou serviços IPTV reais.

## Conclusão executiva

A base de reprodução, o Core personalizado e as migrações recentes estão bem documentados. As prioridades do Council são:

1. Impedir que uma execução de release por tag publique um APK assinado com a chave de debug.
2. Clarificar e proteger a política parental quando falta categoria, e impedir que um restauro altere silenciosamente as proteções locais de um perfil.
3. Proteger a sincronização local, que envia credenciais e a chave do backup por HTTP.
4. Limitar os parâmetros de backups importados e tornar explícitas as exportações incompletas.
5. Impedir que itens de download diferentes reutilizem o mesmo ficheiro e corrigir a corrida entre zap pendente e sintonia direta por número.

## Correções prioritárias

### P1 — Assinatura de releases da CI

O workflow continua se não existir KEYSTORE_BASE64: informa que fará uma compilação assinada com debug em .github/workflows/android.yml:127–141. A execução por tag publica os artefactos sem uma verificação explícita da assinatura em :271–280. Em app/build.gradle.kts:175–179, a Release local também escolhe a assinatura debug quando falta keystore. Assim, uma chave ausente ou mal configurada pode resultar numa Release que não atualiza instalações assinadas com a chave de produção.

**Correção:** falhar qualquer build por tag se faltar keystore, alias ou passwords; verificar o fingerprint da APK antes da publicação. Manter a fallback de debug apenas para builds de desenvolvimento. Remover o caminho local absoluto em app/build.gradle.kts:40–42 e usar configuração por ambiente/propriedade, sem guardar segredos no repositório.

### P1 se Kids for controlo parental estrito — conteúdo sem categoria

A classificação atual é deliberadamente baseada em categorias, porque os fornecedores não fornecem um indicador adulto fiável. No entanto, categoria nula ou sem correspondência é tratada como permitida. Isto aparece no AdultCategoryClassifier.kt:36–43, 74–85, e nos filtros de EPG/Live, por exemplo EpgViewModel.kt:1102–1105 e LiveViewModel.kt:1110–1113. O mesmo caminho afeta pesquisa, Home e a decisão final de iniciar reprodução.

**Correção:** definir uma regra explícita para conteúdo não classificado. Se o modo Kids prometer bloqueio estrito, aplicar negação por omissão em todos os pontos de entrada e disponibilizar exceção parental explícita. Evitar classificar títulos de programas com regras ingénuas, que podem esconder conteúdo legítimo.

### P1/P2 — O restauro pode mudar as proteções de um perfil existente

Ao importar a secção SOURCES, o Core associa perfis existentes pelo nome e copia isKids e, quando presente, pinHash em BackupManager.kt:837–887. O ecrã de importação não pede o PIN local do perfil de destino nem mostra essa alteração no preview. É necessária uma importação confirmada; não é uma exploração remota. Um backup cifrado ou um backup legado pode substituir o PIN, e qualquer backup correspondente pode desligar a proteção Kids.

**Correção:** preservar por defeito as definições de segurança do perfil local; ou pedir autenticação/confirmar alterações ao PIN e a isKids, apresentando-as antes do restauro.

### P2 — Sincronização local sem cifragem de transporte

LocalSyncClient.kt:97–127 força HTTP e coloca o PIN ou segredo persistente em X-Companion-Pin. O servidor devolve a password de sessão que abre o backup em CompanionHttpServer.kt:287–315 e LocalSyncManager.kt:182–197. O ficheiro vai cifrado, mas a password também atravessa a mesma ligação HTTP. Um observador na rede local durante o hosting/sync pode capturar os dados necessários para ler o backup e autenticar-se. Não há evidência de exposição pela Internet.

**Correção:** usar um canal cifrado e autenticado ou um acordo de chave com desafio e token efémero; não transmitir o segredo do container na mesma sessão observável. Para listas IPTV configuradas pelo utilizador que só aceitam HTTP, manter compatibilidade com aviso claro por origem, em vez de bloquear tudo sem teste de fornecedores.

### P2 condicional — URLs com credenciais num backup sem palavra-passe

O Core consegue exportar um ZIP sem cifragem. Nesse caminho, BackupManager.kt:1502–1519 grava URL e EPG URL em texto, e o export bruto de fontes EPG também preserva os valores. Se uma URL tiver password, token ou credenciais em userinfo/query, esses dados ficam no ZIP. O ecrã normal de exportação exige palavra-passe e o LocalSync cifra sempre; portanto, isto não descreve o fluxo normal atual da interface, mas continua a ser um risco da API Core e de formatos/integrações futuras ou legadas.

**Correção:** detetar credenciais nas URLs e impedir exportação sem cifragem, ou omitir/redigir os valores sensíveis. Alinhar também a documentação que promete que todos os segredos são omitidos.

### P2 — Validação de backups e sinalização de dados incompletos

- BackupCrypto.kt:64–69 passa as iterações PBKDF2 do cabeçalho importado diretamente à derivação de chave; a validação aceita até Int.MAX_VALUE. Abrir ou pré-visualizar um ficheiro fabricado pode prender o processador. Validar algoritmo, salt e intervalos de iterações antes da derivação.
- BackupManager.kt:216, 321–325 e 342 converte algumas falhas de leitura em listas vazias. A exportação pode concluir com sucesso sem preferências de reprodução ou dados de legendas. Fazer falhar a exportação quando uma secção selecionada não puder ser lida, ou mostrar e guardar um aviso de dados omitidos.
- LocalSyncClient.kt:88–118 carrega o ficheiro e cria cópias Base64/string em memória. Para backups grandes, isso pode gerar picos de RAM numa box limitada. Preferir envio por streaming e validar limites de tamanho.

### P2 — Revogação de um dispositivo emparelhado

LocalSyncManager.kt:173–197 guarda uma fotografia de acceptedSecrets quando o hosting começa; unpair em :275 remove apenas o dispositivo persistido. O segredo continua aceite enquanto o hosting ativo não terminar.

**Correção:** ler a lista de segredos aceite em tempo real ou atualizá-la no desemparelhamento. Cobrir o caso em que um dispositivo perdido é revogado sem parar o servidor.

### Alegação rejeitada — orçamento de ligações fullscreen e multiview

A primeira leitura confundiu WatchSession, que representa a sessão fullscreen, com OpenStreamRegistry, que reserva slots para multiview e gravações. O Core mantém estes mecanismos separados de propósito. Não há evidência de que a sessão fullscreen deva consumir a reserva dos tiles.

**Decisão:** não unificar os budgets nem adicionar uma claim fullscreen. Preservar a separação. Reabrir apenas se uma medição com fornecedor que imponha limite mostrar excesso real de sessões ou recusa de playback.

## Melhorias de qualidade e UX

- **Performance da interface:** a análise detalhada identificou que SettingsScreen concentra 44 StateFlows no composable raiz e reconstrói o catálogo em recomposições; Home atualiza o progresso do hero a cada 80 ms. Mover cada leitura de estado para o componente que a usa e medir jank em box modesta antes/depois. Referências e plano estão na análise consolidada, secções 1 e 3.
- **Acessibilidade:** a navegação D-pad já tem mecanismos e testes, mas falta uma suite de semântica para botões, diálogos e células do Guia e uma revisão manual com TalkBack. Tratar como lacuna de validação, não como defeito específico já reproduzido.
- **Primeiro CH+/CH− depois de escolha manual:** LiveViewModel.kt:1743–1747 mantém lastZapPressMs; ensurePlaying em :2016–2018 limpa ZapTiming, mas não o timestamp. Uma escolha deliberada logo após um zap pode fazer a primeira pressão seguinte herdar o atraso de 500 ms. Limpar o estado de burst na seleção deliberada; validar com o comando remoto.
- **Redes medidas:** a ronda 4 identificou atualizações/downloads que não dão uma escolha clara para redes metered. Acrescentar controlo por origem para atualizar EPG e descarregar conteúdo apenas em Wi-Fi, com exceção explícita.
- **Chave de metadados no APK:** TMDB_EDGE_KEY é incorporada em BuildConfig (app/build.gradle.kts:76–84) e enviada pelo cliente. Pode ser extraída do APK; o impacto depende das regras do serviço remoto, que não foram inspecionadas. Tratar a chave como pública e aplicar quotas/limites no servidor.

## Ideias de funcionalidade e inovação

1. **Temporizador de sono:** 30/60/90 minutos no HUD, respeitando gravações ativas e desligando a reprodução de forma limpa.
2. **Audiodescrição:** destacar faixas com marcação de acessibilidade no seletor de áudio e guardar a preferência por perfil.
3. **Surpreende-me:** escolher aleatoriamente um filme/série que não tenha sido visto e que respeite favoritos, itens ocultos e política Kids.
4. **Stream Health Score local:** medir tempo até primeira imagem, falhas, reconexões e interrupções por canal; explicar a recomendação de motor/configuração sem enviar telemetria para terceiros.
5. **O que ver agora:** juntar EPG local, catch-up disponível, favoritos e histórico, respeitando perfil e conteúdo oculto.
6. **UPSCALING-BETA-01:** manter como estudo opcional e desligado por defeito. Avaliar primeiro escala da TV/box, capacidades reais de ExoPlayer/mpv, GPU/RAM, latência e sincronismo A/V. Aumentar o buffer não cria detalhe de imagem.

## Classificações revistas pelo Council

- Retirar o achado de MultiviewState.cleanupScope da lista de bugs P1. releaseAll marca o estado como libertado e fill recusa novos trabalhos; não foi demonstrada fuga de recursos.
- Não tratar a migração 5→6 em falta como falha pública ativa sem evidência de uma base de dados v5 em instalações suportadas. O código descreve v5 como esquema transitório que nunca foi publicado.
- A limpeza assíncrona de timeshift local fica como hipótese/P3 até um registo da box demonstrar pedidos ou sessão ainda ativos após sair/parar.
- A classificação de canais sem categoria e as alterações de segurança ao restaurar são comportamentos confirmados; a severidade depende da promessa de controlo parental e do caminho de importação.

## Plano de implementação sugerido

**Fase 1 — Proteção e publicação:** gate de assinatura na CI; política de categoria desconhecida em Kids; proteger PIN/isKids no restauro; cifrar o transporte LocalSync.

**Fase 2 — Integridade e recuperação:** revogação imediata de emparelhamento; limites PBKDF2; exportação completa ou com aviso; URLs sensíveis sempre cifradas; transferência por streaming.

**Fase 3 — Playback e experiência:** corrigir a corrida da fila Stalker e o estado de zap pendente; definir a política para redes medidas; validar TalkBack/semântica; orientar refatorações por medições de recomposição; depois avançar para temporizador de sono e audiodescrição.

## Ensaios que faltam

- Release por tag sem keystore deve falhar antes de publicar; verificar fingerprint quando há keystore.
- Restaurar um backup que altera isKids/PIN; confirmar que o preview mostra as mudanças e que o PIN local não é substituído sem autorização.
- Em perfil Kids, testar canais sem categoria na lista, pesquisa, EPG, Home, catch-up e deep link.
- Capturar uma sessão LocalSync numa rede controlada e confirmar que PIN/segredo/password não ficam observáveis após a correção.
- Importar backups com parâmetros KDF extremos; devem ser rejeitados rapidamente. Não executar o caso extremo em box de produção.
- Com limite de sessões conhecido, abrir fullscreen, multiview e gravação; confirmar a recusa previsível e a libertação dos recursos após erro.
- Na box real, testar rede perdida/restaurada durante Local Timeshift, suspensão/retoma, regresso de leitor externo, foco D-pad e TalkBack.

Percentagens de melhoria não foram medidas. Esta nota recomenda trabalho; não altera nem autoriza alterações à App.

## Base sólida a preservar

Manter o Core TV personalizado identificado pelo snapshot e a migração aditiva 50→51. O registo de 18/10 confirma que as alterações de fuso/catch-up, recuperação de timeshift, zapping isolado/rápido e pressão de memória foram integradas seletivamente. Não substituir esse Core por uma versão upstream nem repetir essas tarefas sem uma diferença concreta demonstrada.

## Adenda — segunda ronda, triagem e reavaliação final

**Data:** 19/10/2026  
**Base:** checkout `main`, commit `bdffec4`; Core personalizado no snapshot `e2081e78…e14890`.  
**Revisão cruzada:** especialistas de reprodução, dados/sincronização e interface/arquitetura TV, seguidos de leitura cruzada pelo Council. A ronda 5 mantém-se como inventário de candidatos em ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md; os rótulos de gravidade desse inventário não são aceites sem triagem. Os candidatos incluídos nesta decisão foram verificados no código e cruzados com os relatórios anteriores.

Esta adenda prevalece quando altera uma classificação ou recomendação das secções anteriores. A análise continuou estática: não alterei código, não executei builds/testes e não validei numa box ou com um fornecedor IPTV real. O Core foi lido através do snapshot usado pelo projeto; não o substituir nem alterar como parte destas recomendações.

### Conclusão revista

As prioridades mais sólidas são agora: reparar a publicação e instalação das releases; fechar os caminhos que permitem sair de Kids sem autorização adulta; proteger restauros, backups e sincronização; corrigir a corrida da fila Stalker; e tornar visíveis as funções que a TV já implementa ou anuncia. Há também uma lacuna real de localização no módulo da app. Desempenho e lifecycle devem ser priorizados depois de medição em boxes representativas, sem tratar contagens estáticas como prova de lentidão ou de fuga de memória.

## Achados aceites nesta ronda e solução

### 1. Instalação e publicação

| Veredicto | Achado e evidência | Solução recomendada |
|---|---|---|
| **P1 — confirmado** | O README local aponta para `renatofc27/OwnTV`, mas o remote deste checkout é `renafcampos27/Owntv`; o endereço de release documentado não resolve para o repositório atual. A release pública `v105.1.0.5` apresenta apenas `app-standard-armeabi-v7a-release.apk`, sem nota de release. | Corrigir os links do README e o link de download do Fire TV para o repositório e asset efetivamente publicados. Acrescentar um preflight de release que teste URLs e exija o asset esperado, a arquitetura e notas de release. Não fazer substituição automática dos links do Core: confirmar separadamente qual repositório Core é o upstream pretendido. |
| **P1 — confirmado no workflow local** | `.github/workflows/android.yml` divide a tag em três variáveis (`MA MI PA`). Nas tags atuais com quatro componentes, como `105.1.0.5`, o último campo recebido por `PA` é `0.5`, inválido como inteiro Bash; o passo de tag falha. | Aceitar explicitamente os quatro componentes e rejeitar formatos malformados. Calcular o `versionCode` numa única fonte de verdade (Gradle ou script chamado pelo Gradle), monotónico e superior ao das versões já distribuídas. Adicionar verificação automatizada de mapeamentos de tags antes de voltar a publicar. |
| **P1/P2 — confirmado, impacto de tamanho** | O workflow escolhe `app-standard-universal-release.apk` apesar de existirem saídas por ABI. O artefacto publicado pela API do GitHub nesta revisão era apenas ARMv7. Isto exclui boxes 64-bit-only e pode descarregar mais dados do que um APK arm64. Não foi medida diferença de desempenho no dispositivo. | Publicar arm64 como opção principal e manter uma opção ARMv7/universal para boxes antigas. Usar nomes de saída explícitos; inspecionar o conteúdo de cada APK e falhar a publicação se a ABI não corresponder ao nome. Publicar x86_64 apenas depois de verificar o artefacto produzido. |
| **P1 condicional — confirmado no código** | Sem keystore, a configuração Release local e o passo de CI podem escolher assinatura debug. Nas tags atuais, o cálculo de versão falha antes do build; portanto, a publicação debug não é o resultado demonstrado para essas tags. | Para tags/releases, falhar se faltarem keystore, alias/password ou fingerprint esperado; confirmar o certificado do APK antes de o publicar. Preservar o fallback debug apenas para desenvolvimento/PR, sem permitir que esse artefacto entre na release. |
| **P2 — confirmado** | O changelog da app não tem secção para `v105.*` e a release atual tem corpo vazio; assim, a atualização não comunica alterações. | Exigir uma secção correspondente à tag num job de preflight e usar essa secção para preencher o corpo da release. Falhar a publicação quando a nota estiver ausente, em vez de emitir texto vazio ou placeholder. |
| **P2 — confirmado** | O README instrui a criar um token do GitHub Packages, embora o Core deste projeto seja carregado de um snapshot local verificado. | Atualizar as instruções de build para explicar o snapshot vendorizado e a verificação SHA-256; retirar o passo do token e documentar `owntv.corePath` apenas como override para quem desenvolve o Core. |
| **P2 — precisa de validação do artefacto** | A seleção de APK x86_64 usa `ls … | head -n1`, que não garante uma ABI correta nem uma saída única. A interação exata entre flavor/splits depende da saída real do AGP. | Selecionar o caminho esperado pelo nome, verificar `lib/<abi>/` dentro do APK e falhar se estiver ausente. Não publicar um artefacto escolhido por ordem do diretório. |

**Verificação pública de 19/10/2026:** [release `v105.1.0.5`](https://github.com/renafcampos27/Owntv/releases/tag/v105.1.0.5) e [API de releases](https://api.github.com/repos/renafcampos27/Owntv/releases/latest). O endereço antigo usado pelo README é [este link de release](https://github.com/renatofc27/OwnTV/releases/latest), que não corresponde ao remote desta cópia. O estado publicado pode mudar; voltar a confirmar antes de uma release futura.

### 2. Perfis Kids e autorização adulta

| Veredicto | Achado e evidência | Solução recomendada |
|---|---|---|
| **P1 se Kids for controlo parental — confirmado** | Pelo gate de perfil, o tile “Adicionar perfil” abre o onboarding sem autorização. O onboarding permite criar um perfil adulto sem PIN e, ao terminar, a app entra nesse perfil. Uma sessão Kids consegue assim mudar para um perfil sem as restrições Kids sem provar autorização adulta. | Criar uma credencial de adulto/guardian na configuração inicial e pedi-la antes de adicionar um perfil adulto ou reduzir restrições. Validar novamente após o onboarding. A autorização não pode depender do PIN do perfil alvo, pois esse PIN pode não existir. |
| **P1 se Kids for controlo parental — confirmado** | A edição de um perfil Kids sem PIN não exige uma credencial adulta independente; as ações sensíveis podem ficar sem confirmação. | Exigir autorização adulta para retirar Kids, remover o PIN ou alterar a proteção, mesmo quando o perfil de destino não tem PIN. Negar por omissão alterações que enfraqueçam a proteção; mostrar ao adulto a alteração antes de a gravar. |
| **P1/P2 — confirmado, importação iniciada pelo utilizador** | O restauro de `SOURCES` pode alterar `isKids` e, se o backup tiver valor, o `pinHash` do perfil existente com nome correspondente. O PIN nulo do backup é preservado, mas `isKids` ainda pode mudar. Isto exige que alguém importe o ficheiro; não é acesso remoto. | Preservar as definições de segurança locais por defeito. Mostrar no preview cada diferença de PIN/Kids e exigir autorização adulta local antes de aplicar uma alteração; registar o resultado da importação. |
| **P1 apenas se a promessa for bloqueio estrito — confirmado** | Conteúdo sem categoria/sem correspondência passa pelo filtro Kids. É uma limitação da classificação por categorias de terceiros, não um sinal de que o filtro falha em todas as fontes. | Definir a política para conteúdo não classificado: permitir com aviso, ou negar por omissão se o produto prometer bloqueio estrito. Aplicar a mesma regra a lista, pesquisa, EPG, Home, catch-up e decisão de reprodução; fornecer exceção adulta autenticada. |
| **P2 — hardening local** | O PIN usa hash SHA-256 com salt, mas a derivação é rápida e o gate no dispositivo não tem limite persistente de tentativas/backoff. O risco principal é tentativa local ou extração da base de dados, não exploração pela rede. | Migrar gradualmente para KDF lento versionado (PBKDF2/Argon2, com salt) e comparação constante; adicionar atraso/limite persistente no gate e manter compatibilidade para PINs existentes durante a migração. |

### 3. Reprodução e fila

| Veredicto | Achado e evidência | Solução recomendada |
|---|---|---|
| **P2 — confirmado estaticamente; frequência depende do provider** | Nos episódios Stalker, `loadItem()` inicia a resolução assíncrona sem invalidar a geração anterior. Dois pedidos podem partilhar a mesma geração; se o pedido antigo terminar primeiro, pode começar o episódio antigo e a seleção mais recente ser descartada. A troca prévia do resolver/headers também pode deixar Retry a combinar estado de dois itens. | No início de cada seleção, cancelar/inutilizar a resolução anterior e atribuir um identificador exclusivo ao novo pedido. Só publicar URL, headers, metadados e provider de reconexão depois de confirmar que o identificador continua atual. Repropagar `CancellationException`; não transformar cancelamento em falha de stream. |
| **P3 — defeito pequeno confirmado** | Depois de escolher manualmente um canal, `lastZapPressMs` pode manter o timestamp de uma rajada anterior e fazer a pressão CH+/CH− seguinte herdar o atraso de 500 ms. Não foi encontrado risco de escolher o canal errado. | Limpar `lastZapPressMs` no mesmo caminho que limpa `ZapTiming` numa seleção deliberada. |

### 4. Backups, sincronização e dados

| Veredicto | Achado e evidência | Solução recomendada |
|---|---|---|
| **P2 — confirmado, ficheiro escolhido pelo utilizador** | O leitor de backups aceita `iterations` PBKDF2 do cabeçalho sem impor limite antes de derivar a chave. Um `.own` fabricado pode consumir CPU durante a importação; não há evidência de vetor remoto nem de bloqueio comprovado da UI, pois o caminho usa IO. | Validar formato, algoritmo, salt e intervalo de iterações antes da derivação; rejeitar valores fora do intervalo suportado com erro claro. Manter o limite compatível com backups legítimos anteriores e futuros. |
| **P2 condicional — confirmado** | Falhas em algumas leituras de DAO durante a exportação são convertidas em listas vazias, permitindo sucesso com secções em falta. Só há perda se uma dessas leituras falhar. | Recolher as falhas por secção e falhar a exportação ou gerar um backup explicitamente marcado como incompleto. A UI deve indicar quais os dados omitidos; não mostrar “backup concluído” sem ressalvas. |
| **P2 no cenário de observador da LAN — confirmado** | LocalSync usa HTTP na rede local e envia a credencial no cabeçalho; o fluxo autorizado também disponibiliza material associado à chave do backup. Isto expõe dados a quem observa essa rede durante a operação; não demonstra exposição pela Internet. | Cifrar/autenticar o transporte com TLS e fingerprint trocado no emparelhamento, ou cifrar o payload com uma chave efémera negociada fora do canal observável. Limitar a validade do PIN/segredo, revogá-lo de imediato e nunca colocá-lo em URL/query. |
| **P2/P3 condicional — cópias confirmadas, impacto não medido** | LocalSync materializa o ficheiro, Base64, texto do corpo e bytes de envio/receção. Backups grandes podem pressionar a memória de uma box modesta; a estimativa máxima da ronda 5 não foi medida nesta máquina. | Usar corpo binário em streaming com buffer limitado, limite de tamanho validado antes da leitura e gravação incremental do lado recetor. Medir pico de heap em backups representativos antes de atribuir severidade de OOM. |
| **P2 condicional — confirmado** | Durante hosting, a lista de credenciais aceites pode ficar em memória; remover um emparelhamento da persistência não garante que a cópia em memória deixe de o aceitar imediatamente. | Atualizar o estado em memória no `unpair`, ou consultar as credenciais atuais em cada pedido autenticado. Confirmar revogação enquanto o hosting permanece ativo. |
| **P2 condicional — hipótese, não reproduzida** | O Worker de backup automático não chama `setForeground`; o `finally` apaga o staging se houver cancelamento. Não foi medida a duração real de uma exportação nem demonstrado que o WorkManager interrompe backups normais. | Medir primeiro com backups grandes em boxes modestas. Se puder exceder os limites de execução, mostrar notificação/usar o tipo de foreground permitido para a versão Android e tornar o processo retomável ou particionado; preservar o ficheiro anterior até o novo estar completo. |
| **P2 — risco da API do Core, não do fluxo normal da UI** | Uma via de exportação sem cifra pode serializar URLs com credenciais. A interface normal pede palavra-passe e a LocalSync cifra os dados. | Impedir exportação sem cifra quando uma source contém credenciais embutidas, ou redigir esses campos e assinalar explicitamente a omissão. Manter o fluxo normal cifrado. |

### 5. Entrada e navegação na TV

| Veredicto | Achado e evidência | Solução recomendada |
|---|---|---|
| **P2 — descoberta, não inacessibilidade total** | A Pesquisa não tem botão na navegação principal, mas `OPEN_SEARCH` pode ser associado a um atalho remoto nas Definições. A classificação “sem qualquer porta” é exagerada; a porta é escondida e exige configuração. | Colocar Pesquisa no TopBar ou na navegação principal e manter o atalho remoto como opção. Garantir foco D-pad previsível ao abrir e ao voltar. |
| **P2 — confirmada a ausência de separadores funcionais** | O ecrã chamado Downloads desenha apenas o separador LIVE e o respetivo botão não faz nada; os ramos Movies/Series não são alcançáveis. O destino atual contém gravações. | Se a app suporta downloads de filmes/episódios, desenhar separadores reais e ligá-los ao `DownloadsViewModel`. Se o destino for apenas gravações, renomeá-lo para “Gravações” e retirar o botão inerte e ramos sem utilização. |
| **P2 — funcionalidade VOD atualmente desligada; intenção por decidir** | `HomeViewModel` chama `feed.load(..., liveOnly = true)`, que esvazia Trending e Continue VOD. O restante código continua a expor estado/indicadores para essas linhas. Pode ser uma decisão de produto, mas a UI e o estado não estão alinhados. | Escolher uma política: oferecer um toggle “Home com VOD” e carregar essas linhas quando ligado; ou remover as linhas/indicadores e configurações mortas se Home for deliberadamente Live-only. Se forem ativadas, reduzir a cadência do hero e pausar animação quando fora do ecrã. |
| **P2 — hub incompleto, com rotas alternativas** | More não desenha as linhas Favorites/History/Local Sync que existem nos enums/painéis. Local Sync continua acessível em Definições e há outras entradas de favoritos; portanto, não é correto afirmar que tudo está globalmente inacessível. | Acrescentar ao hub apenas os destinos que se pretende oferecer e ligar as páginas existentes; remover enum/painéis e coletores sem uso. Preservar a rota de Local Sync em Definições. |
| **P2 — confirmado na contagem de recursos** | O catálogo anuncia 26 locales, mas as 266 strings próprias do módulo app estão no inglês por omissão e só têm conjuntos adicionais `pt` (170/266) e `pt-rPT` (233/266). Nos restantes locales, essas strings do módulo app recorrem ao inglês. | Criar um verificador de cobertura que compare as chaves de `values/` com cada locale anunciado; traduzir primeiro os idiomas prioritários ou reduzir a lista de idiomas alegados. Substituir o `MissingTranslation` global por exceções pontuais documentadas. |

## Reavaliação dos riscos e falsos positivos

| Alegação anterior/candidata | Veredicto final | Tratamento e solução |
|---|---|---|
| “O orçamento de ligações não inclui fullscreen”, proposta na primeira síntese | **Refutada; retirar P2 e a correção de contar fullscreen no mesmo budget.** O Core mantém `WatchSession` separado do `OpenStreamRegistry` deliberadamente para não consumir a reserva de multiview/gravações. | Nenhuma alteração. Preservar a separação e só corrigir claims de sessões extras reais, como local timeshift, se uma medição os mostrar. |
| `MultiviewState.cleanupScope` é fuga P1 | **Não demonstrada; não é P1.** Estado libertado e guards pós-suspensão impedem novos trabalhos; scope sem cancelamento, por si só, não prova stream ou recurso retido. | Sem correção urgente. Só acrescentar cancelamento explícito se profiling/lifecycle na box mostrar trabalho ou memória retida. |
| `CoroutineScope(Dispatchers.IO)` do cleanup do timeshift vaza um scope por chamada | **Exagerada; P3/hipótese.** O job anterior é encadeado e o Core fecha recursos de forma cancelável/NonCancellable conforme o caso. | Não reescrever às cegas. Registar sessões/pedidos ativos ao sair/parar; se persistirem, centralizar cleanup no lifecycle do ViewModel/Core. |
| Zap burst cria três pedidos de canal numa sequência de três pressões | **Refutada pela lógica de cancelamento.** A pressão seguinte cancela o tune pendente intermédio; o cenário observado estaticamente produz pedido inicial e final. | Não corrigir este relato como bug. Separadamente, limpar `lastZapPressMs` após seleção deliberada (P3 acima). |
| Pesquisa está “totalmente inacessível” | **Parcial.** Existe atalho remoto configurável, sem entrada visível por omissão. | Corrigir como descoberta P2, adicionando entrada visível; não como falha de execução da pesquisa. |
| Regresso ao Android Home mantém perfil desbloqueado | **Política atual, não defeito confirmado.** A sessão dura enquanto a Activity/ViewModelStore vive. | Ideia opcional: timeout de inatividade configurável para boxes partilhadas, com aviso e reautenticação adulta. Não reautenticar em cada retorno sem opção do utilizador. |
| `usesCleartextTraffic=true` expõe credenciais sempre | **Matizar.** O risco ocorre quando a source usa HTTP; com HTTPS, o transporte é cifrado. O suporte a HTTP pode ser necessário para providers. | Desativar cleartext por defeito e permitir exceção explícita por source/domínio, com aviso ao gravar a source HTTP. Não bloquear providers HTTP sem migração/configuração. |
| `TMDB_EDGE_KEY` extraível compromete o segredo mestre | **A primeira parte é real; a conclusão sobre o segredo mestre não é demonstrada.** Uma chave cliente compilada é extraível; o impacto depende das regras/quotas efetivas do Worker. | Tratar a chave distribuída como pública; aplicar quotas e limites no serviço. Não afirmar comprometimento do master sem evidência do backend. |
| `FileProvider` com `external-path path="."` expõe já todos os ficheiros | **Risco de hardening, não fuga demonstrada.** Provider não exportado e só URIs concedidos são partilhados pelo fluxo atual. | Restringir a pastas necessárias e validar que o ficheiro está dentro delas antes de criar URI. |
| Atualizador sem SHA-256 instala APK arbitrário | **Não demonstrado como bypass.** O PackageInstaller exige a assinatura compatível da app. A ausência de hash é defesa em profundidade, sobretudo contra artefacto errado ou chave comprometida. | Opcional: publicar digest assinado/manifesto e conferir o certificado instalado, package e versionCode antes do instalador. |
| `MANAGE_EXTERNAL_STORAGE` e `requestLegacyExternalStorage` são permissões simplesmente mortas | **Não confirmado.** O Manifest documenta fallback de seleção de pastas para TVs/OEMs onde SAF não aparece. | Manter o comportamento compatível até validar a matriz de boxes; preferir SAF quando disponível e pedir acesso amplo apenas no momento em que a ação o requer. Rever separadamente requisitos de distribuição. |
| Credenciais em Room/DataStore equivalem a roubo remoto | **Não.** O risco é principalmente extração local/root/backup; Android sandbox e encriptação do dispositivo são mitigação, não substituem gestão de segredos. | Considerar Keystore envelope para campos sensíveis numa migração compatível; não classificar como vulnerabilidade remota sem vetor adicional. |
| `androidTest` ausente do CI / zero verificações de release | **Lacuna de validação, não bug da app.** A pipeline precisa de evidência executável para artefacto e ecrãs Android. | Acrescentar uma smoke suite de emulador e verificações de artefacto/assinatura num workflow apropriado; não tratar isso como prova de funcionamento em box física. |

## Desempenho: manter como trabalho condicionado por medição

Os pontos seguintes são plausíveis e dão boas hipóteses de perfil, mas a severidade da ronda 5 não está medida em hardware. As correções só devem entrar depois de confirmar impacto, para evitar trocar comportamento funcional por uma otimização presumida.

| Ponto a medir | Quando se torna relevante | Solução caso a medição confirme impacto |
|---|---|---|
| `GroupedChannelPagingSource` agrega o catálogo inteiro; Paging Live usa `MAX_SIZE_UNBOUNDED` | Catálogos muito grandes e opção de agrupar versões ativa; pode aumentar heap e trabalho após invalidações. | Fazer agrupamento no DAO/SQL ou produzir projeção compacta; limitar cache de páginas sem quebrar o agrupamento ou o retorno ao canal focado. |
| Cargas `Eagerly` e retenção do catálogo no arranque | Se LiveViewModel inicia antes de o utilizador abrir Live e afeta primeiro frame/memória. | Adiar queries não essenciais até a secção ser usada; conservar apenas flows de estado necessários no shell. Confirmar por trace de startup. |
| N+1 em `DownloadsViewModel.isHidden` para perfil Kids/itens ocultos | Muitas transferências e novas emissões de progresso. | Carregar metadados/categorias em lote e filtrar em memória com uma única emissão. |
| EPG mantém listas grandes de `ChannelEntity` completas | Catálogo EPG grande e memória limitada. | Paginar ou guardar projeção mínima de identidade/categoria/EPG, carregando detalhes só para as linhas visíveis. |
| `SettingsScreen` concentra coletores de estado; animação do hero usa intervalo de 80 ms | Mudanças frequentes em settings ou ativação das linhas VOD. | Recolher estado nos componentes que o usam; usar animação do frame clock/intervalo menos frequente e parar quando a linha está invisível. Medir recomposições e jank antes/depois. |
| Agenda do backup é diária para políticas mensais e usa `KEEP` ao mudar constraints | O agendamento pode acordar sem exportar e alterações a constraints podem ser ignoradas. | Agendar no intervalo real ou atualizar o trabalho periódico com política `UPDATE`; confirmar que intervalo, constraints e “executar agora” correspondem ao estado apresentado na UI. |

## Ideias válidas para produto — cada uma com caminho de implementação

Estas ideias têm valor, mas ficam depois de corrigir distribuição, proteção Kids e acessibilidade das funções já existentes. A proposta de upscaling permanece investigação opcional e desligada por defeito; buffering extra não demonstra aumento de detalhe.

| Ideia | Solução/proposta concreta | Dependências e limite |
|---|---|---|
| **Temporizador de sono** | No HUD, escolher 20/30/60/90 min; parar o stream e libertar engine/ligação ao fim do tempo; opção de fade do volume. | Cancelar ao desligar a box e não parar gravação ativa. O “fim do programa” só entra quando o EPG tem dados fiáveis. |
| **Diagnóstico local de qualidade por canal** | Guardar localmente tempo até primeira imagem, rebuffer, falha/retry e troca de engine; mostrar tendência simples e exportar relatório sanitizado pelo utilizador. | Não enviar telemetria. Separar sinais de rede, provider e decoder; sem dados suficientes, dizer “causa inconclusiva” em vez de atribuir culpa. |
| **“O que ver agora”** | Home com próximos programas EPG/catch-up, favoritos e histórico local, com explicação curta do motivo da sugestão. | Aplicar primeiro filtros Kids, ocultos e privacidade; fallback honesto quando não há EPG nem metadados. |
| **“Surpreende-me”** | Selecionar um filme/série ainda não visto a partir do catálogo local, com filtros de tipo, género e duração; ação de desfazer/novo sorteio. | Usar chaves estáveis e respeitar ocultos/Kids/favoritos; não enviar histórico para cloud. |
| **Faixa de audiodescrição por perfil** | Dar destaque a faixas com sinalização de audiodescrição no seletor e guardar preferência por perfil quando o stream expõe metadados. | Não inferir a partir do idioma sozinho; o provider pode não marcar a faixa corretamente. |
| **Cartão de saúde da source** | Após sincronização, mostrar número de canais, cobertura EPG, falhas observadas e última sincronização bem-sucedida. | Não testar dezenas de URLs sem autorização: sondagens gastam sessões. Preferir estatísticas recolhidas de uso normal e opção explícita para diagnóstico serial. |
| **Onboarding com progresso e saída utilizável** | Mostrar contagens por secção durante importação, permitir abrir Live assim que estiver pronto e apresentar “TV pronta” com passos claros quando ainda não há source. | Não adicionar streams de demonstração sem validar licença/origem. Um preview com dados sintéticos pode mostrar a UI sem assumir conteúdo IPTV. |
| **Voz para Pesquisa** | Botão de microfone apenas em devices com recognizer, com resultado convertido em pesquisa normal e indicação clara se o reconhecimento usa serviço cloud. | Opt-in, esconder quando não há microfone/reconhecedor e preservar pesquisa por D-pad. Resolver privacidade/plataforma antes do lançamento. |
| **Timeout parental configurável** | Para boxes partilhadas, bloquear perfil Kids após inatividade escolhida e exigir autorização adulta para retomar. | Conveniência, não segurança forte: relógio ou processo do device podem ser alterados. Não tratar como substituto da autorização adulta. |

**Adiar nesta fase:** Watch Party/relay entre redes, tradução/dobragem por IA e Chromecast exigem dependências de contas, licenciamento, suporte de receptor ou transmissão de dados que não se justificam antes de estabilizar a experiência local. A beta `UPSCALING-BETA-01` continua apenas como estudo com hardware real, medição de GPU/RAM, latência e sincronismo A/V; manter opcional e desligada se algum dia for construída.

## Plano final por ordem

1. **Release/install:** corrigir parser de tags, fonte única de versionCode, links, ABI, notas e gate de assinatura; inspecionar artefacto antes de publicar.
2. **Proteção Kids:** autenticação adulta em adicionar/alterar perfil, regra explícita para categorias desconhecidas e preview/autorização de alterações de segurança no restauro.
3. **Integridade de dados e rede:** limite PBKDF2, erros de exportação visíveis, proteção LocalSync e revogação imediata; streaming se a medição confirmar pressão de heap.
4. **Correção da fila Stalker:** invalidar/cancelar resoluções antigas antes de iniciar uma nova; depois confirmar com teste de regressão e provider real.
5. **TV/i18n:** entrada visível de Pesquisa, Downloads coerentes, política Home VOD clara, rotas More e cobertura de recursos alinhada com os idiomas anunciados.
6. **Desempenho e ideias:** medir em box modesta e só então priorizar as otimizações; começar novas funções por temporizador de sono e diagnóstico local sem telemetria.

## Estado final do Council

- **Manter** os achados confirmados nas análises das rondas 1–4 e as soluções registadas nesta síntese, exceto onde as adendas alteram explicitamente o veredicto. Usar os relatórios ligados acima como contexto e voltar ao código antes de implementar.
- **Acrescentar** a corrida de resolução Stalker, o bypass Kids por “Adicionar perfil”, as falhas de publicação/ABI/links, o limite ausente no KDF, exportação silenciosamente incompleta e lacunas de navegação/localização.
- **Rebaixar ou rejeitar** as alegações sem reprodução ou refutadas acima. Em particular, remover a correção que juntaria fullscreen ao budget de multiview/gravações.
- **Não executar** as recomendações automaticamente: este documento é um plano com soluções; não autoriza alteração do app/Core.
- **Evidência não recolhida:** comportamento de codec/HDMI, performance em boxes, providers reais, TalkBack/controlo remoto físico, duração dos backups e comportamento do PackageInstaller após instalação. Não foram medidas percentagens de melhoria.

## Adenda — terceira ronda: downloads, playback e reavaliação global

**Data:** 19/10/2026  
**Checkout:** main, commit bdffec4.  
**Core:** snapshot personalizado e2081e78…e14890.  
**Revisão:** os agentes de dados/sincronização, reprodução e interface/arquitetura fizeram leituras independentes; o Council cruzou as evidências com as duas adendas anteriores e voltou ao código para resolver duplicações e falsos positivos.

A revisão foi estática. Não alterei código da App/Core, não executei builds nem testes e não validei numa box, com comando remoto ou fornecedor IPTV. As referências a desempenho, temporização e comportamento de armazenamento continuam a precisar de validação em hardware.

### Achados adicionais aceites pelo Council — cada um com solução

| Veredicto | Achado e evidência | Solução concreta |
|---|---|---|
| **P2 — colisão confirmada pelo fluxo; ocorrência depende de itens com o mesmo nome** | Filmes e episódios criam o nome de destino a partir do título sanitizado e extensão: MovieViewModel.kt:700–713 e SeriesViewModel.kt:1077–1094. O DownloadManager grava em raiz/diretório/nome e o MediaRoot reutiliza um ficheiro já existente. O DownloadEngine retoma a partir do tamanho que encontrar e considera HTTP 416 uma transferência já concluída. Dois itens diferentes com o mesmo título/extensão — por exemplo, de fontes ou perfis diferentes — podem partilhar um ficheiro; o segundo pode ser marcado concluído com o primeiro conteúdo, anexar bytes incompatíveis ou sobrescrever o destino. | Gerar um identificador de destino estável e único por perfil, source, tipo e ID do item; manter o título apenas como parte legível do nome. Nunca retomar um ficheiro até confirmar que pertence à mesma identidade de conteúdo. Se houver colisão ou estado legado sem identidade, escolher nome novo ou pedir resolução explícita. Reservar o destino atomicamente para impedir que dois downloads concorrentes o escolham ao mesmo tempo. |
| **P2/P3 condicional — eliminação do perfil deixa ficheiros sem registo** | ProfileManager.kt:115–135 apaga o perfil e os dados associados; a tabela downloads tem FK com ON DELETE CASCADE e guarda filePath em UserDataEntities.kt:229–240. A remoção do perfil elimina os registos da base de dados, mas o fluxo do ProfileManager não remove os ficheiros do armazenamento. O próprio comentário de ProfileManager promete apagar tudo o que está associado ao perfil. | Definir no diálogo de eliminar perfil se os downloads offline também serão removidos ou mantidos. Se forem removidos, suspender as transferências desse perfil, recolher os caminhos antes da cascata, apagar apenas ficheiros sem referência por outro perfil/transferência e mostrar falhas de limpeza. Se forem mantidos, transferi-los para uma biblioteca global visível em Downloads, em vez de deixar ficheiros órfãos invisíveis. |
| **P2 condicional — direct tune pode cancelar o próprio canal que aparenta já estar ativo** | zapTo() publica o alvo em _previewChannel antes de iniciar a reprodução e pode deixar pendingZapTuneJob à espera (LiveViewModel.kt:1744–1770). tuneByNumber() chama cancelChannelRecovery(), que cancela esse job, usa _previewChannel como canal atual e retorna sucesso sem reproduzir quando o número introduzido corresponde ao alvo pendente (LiveViewModel.kt:1782–1787, 1860–1877; cancelChannelRecovery em :2060–2075). O resultado possível é o vídeo do canal anterior continuar enquanto a interface afirma que o alvo já está sintonizado. | Manter separados o canal efetivamente reproduzido e o alvo de zap/preview. Comparar o número pedido com o canal ativo confirmado. Se for o mesmo alvo pendente, concluir imediatamente essa sintonia antes de responder sucesso; se for outro canal, cancelar o pedido antigo e iniciar o novo. Atualizar o estado ativo apenas quando a abertura do stream tiver sido aceite. |
| **P3 — identificação visual pode ficar atrasada durante o zap** | PlayerHud.kt:588–605 mostra ChannelCard(player) na animação breve de mudança de canal. Esse cartão usa a metadata corrente do player; o comentário próximo confirma que Exo/mpv e Stalker podem publicar metadata apenas depois de uma resolução assíncrona. Durante esse intervalo, o OSD pode exibir o nome/logo do canal anterior. | Para o flash de zap, alimentar o cartão com a identidade do canal alvo guardada no estado de seleção. Usar metadata do motor apenas quando não existir alvo pendente. Limpar o alvo e voltar ao cartão normal quando a seleção falhar ou a reprodução for confirmada. |

### Complementos e candidatos condicionais após a revisão

| Ponto reavaliado | Decisão do Council | Solução se mantido |
|---|---|---|
| **F4 — indicador de cobertura de idiomas** | A lacuna de tradução do módulo App já estava corretamente identificada na segunda adenda. O agente confirmou um complemento: o seletor chama coverageBadgePercent() em LanguageSettingsScreen.kt:356–370, mas a fonte de cobertura/SupportedLocales pertence ao Core; tools/i18n/README.md e .github/workflows/i18n.yml dizem que os strings vivem no Core. Como o checkout também tem recursos próprios em app/src/main/res/values e apenas conjuntos pt e pt-rPT, a percentagem pode não refletir todos os textos que a App mostra. Aceitar como parte de F4, sem criar um bug duplicado. | Calcular cobertura com a união dos recursos do Core e da App, incluindo plurais e placeholders, e alimentar o selo do seletor com esse resultado. Alternativamente, mover os recursos app-specific para o Core e tornar o pipeline de tradução a única fonte de verdade. O seletor deve distinguir tradução do Core e cobertura efetivamente visível na App se as árvores continuarem separadas. |
| **Histórico após abrir um canal pelo Guia — P3 condicional** | watchFromGuide() agenda escrita imediata, mas o mesmo historyJob é cancelado por uma sintonização subsequente; as sintonizações normais voltam a usar debounce de cinco segundos (LiveViewModel.kt:1713–1717, 2222, 2301–2313). A concorrência só afeta a gravação se uma operação Room ainda estiver em curso quando chega outra seleção. A intenção do debounce — não registar canais pelos quais o utilizador apenas passou — é válida; não é bug confirmado. | Primeiro decidir o contrato: Histórico deve guardar qualquer abertura deliberada do Guia ou apenas o último canal visto de forma estável? Se a abertura deliberada tiver de persistir sempre, separar esse evento da fila de debounce e serializar a gravação por perfil. Se o produto quiser apenas canal estável, manter o comportamento atual e acrescentar verificação de timing em box antes de alterar. |
| **PLY-15 — publicação da âncora direct tune** | Rejeitar como bug. O único callsite passa uma callback síncrona que inicia o trabalho de playback e retorna; não foi encontrada suspensão que permita à UI intercalar uma pressão D-pad antes de publicar a âncora. | Não alterar a ordem só com base na hipótese estática. Reabrir apenas se uma reprodução real do erro mostrar CH+/CH− sem resposta. |
| **Foco/D-pad** | Não surgiu um defeito novo com evidência suficiente nesta ronda. Os itens já listados continuam como validação de usabilidade e TalkBack, não como falhas reproduzidas. | Manter testes de semântica e ensaio manual com comando remoto/TalkBack na matriz de validação; corrigir apenas falhas observadas. |

### Reavaliação integral do que já estava no relatório

| Área | Veredicto final | Solução que continua válida |
|---|---|---|
| **Assinatura, tags e publicação de release** | Manter P1: ausência de keystore de produção, parser de tags com quatro componentes, fonte de versionCode, seleção de ABI, links e notas são problemas independentes da experiência na box. | Falhar releases sem credenciais válidas; confirmar fingerprint antes de publicar; aceitar/rejeitar formatos de tag explicitamente; calcular versão numa só fonte; publicar ARM64 e manter ARMv7/universal compatível com boxes antigas; validar artefactos, URLs e notas no preflight. |
| **Proteção Kids e autorização adulta** | Manter como P1 se a promessa for controlo parental: adicionar perfil, alterar proteção e conteúdo não classificado são caminhos que podem ultrapassar a intenção do perfil. | Exigir credencial adulta independente para criar perfil adulto ou enfraquecer Kids; aplicar regra documentada a conteúdo desconhecido em lista, pesquisa, EPG, Home, catch-up e playback; dar ao adulto preview e aprovação explícita de mudanças de PIN/Kids no restauro. |
| **Restauro de PIN/Kids** | Manter P1/P2 condicional ao ficheiro ser escolhido pelo utilizador; o código permite importar alterações de segurança locais. | Preservar a política local por defeito; apresentar diferenças de PIN e Kids no preview; autorizar localmente cada alteração sensível antes de gravar. |
| **Backups e importação** | Manter: PBKDF2 sem intervalo seguro, falhas de DAO convertidas em secções vazias e possibilidade de exportação API sem cifra com credenciais em URL têm impacto concreto, mas dependem do ficheiro/operação. | Validar algoritmo/salt/iteração antes de derivar chave; manter intervalo compatível com backups suportados; falhar ou marcar backup incompleto com secções omitidas; não exportar URLs com credenciais sem cifra; mostrar claramente o resultado. |
| **LocalSync** | Manter com âmbito correto: o risco de HTTP é para observador da LAN durante a sessão, não exposição pública pela Internet. O custo de memória por cópias e a revogação em memória também continuam. | Usar canal cifrado/autenticado e credencial efémera; revogar imediatamente no estado ativo; não transmitir segredo em URL; usar transferência binária em streaming apenas após medir tamanho/heap representativos. |
| **Fila de reprodução Stalker** | Manter P2 confirmado estaticamente no Core bloqueado: resoluções assíncronas antigas podem competir com seleção nova. | Identificador exclusivo por seleção; cancelar/inutilizar a geração anterior antes de resolver; publicar URL, headers e metadados apenas quando a geração ainda for atual; propagar CancellationException. |
| **Zapping e direct tune** | Manter o atraso herdado após seleção deliberada como P3; acrescentar a corrida P2 descrita nesta ronda. A alegação de três pedidos por rajada continua refutada pelo cancelamento do tune intermédio. | Limpar timestamp de rajada numa escolha deliberada; separar alvo pendente de canal ativo; validar ordem e timing com comando remoto físico. |
| **Downloads** | Manter a revisão da navegação: o separador LIVE inerte e os ramos Movies/Series sem acesso devem ser resolvidos por produto. Acrescentar colisão de ficheiros como P2 condicional e ficheiros órfãos ao eliminar perfil como P2/P3 condicional. | Decidir se Downloads é biblioteca de ficheiros offline ou gravações; nomear as rotas de acordo; atribuir caminho único por identidade estável; ao eliminar perfil, eliminar com segurança ou migrar os ficheiros para uma biblioteca visível. |
| **Pesquisa, Home, More e localização** | As lacunas existentes continuam válidas como melhorias de produto. A Pesquisa tem atalho configurável, portanto a falha é descoberta; Home VOD pode ser escolha de produto; More tem rotas alternativas; a cobertura de idiomas precisa de contar recursos da App. | Expor Pesquisa na navegação; escolher toggle VOD ou remover linhas/configuração mortas; ligar apenas rotas que serão suportadas; alinhar todas as traduções e o indicador de cobertura com os recursos realmente empacotados. |
| **Rede medida e chave TMDB** | Manter como melhoria/hardening, não como vulnerabilidade confirmada. Não há medição de custos de rede nem análise das quotas atuais do serviço. | Oferecer Wi-Fi apenas por origem para EPG/downloads com exceção explícita; tratar chave embutida no APK como pública e aplicar quota/restrições no serviço, sem prometer segredo no cliente. |
| **Desempenho e lifecycle** | Agrupamento Paging, cargas eager, coletores de Settings, N+1 em Downloads, lista EPG, animação do hero e política do Worker continuam hipóteses de custo, não bugs medidos. A limpeza do timeshift também não é fuga comprovada. | Traçar arranque, heap, jank, recomposições e duração em box modesta; otimizar apenas o gargalo medido; exigir evidência de trabalho/recurso ativo após saída antes de reescrever scopes ou lifecycle. |
| **Alegações de segurança/armazenamento rebaixadas** | Manter rejeições anteriores: budget fullscreen não deve ser agregado ao budget de multiview; cleanupScope sem trabalho retido não demonstra fuga P1; FileProvider não exportado não prova acesso arbitrário; a permissão ampla tem fallback OEM documentado; o PackageInstaller exige assinatura compatível para atualizar. | Não adicionar correções especulativas. Reabrir só com falha observada no dispositivo, artefacto ou fornecedor compatível. |

### Ideia nova e ordem de execução proposta

**Gestor de armazenamento offline por perfil.** Juntar ao ecrã Downloads um resumo de espaço, downloads concluídos/falhados, ficheiros ausentes e ficheiros sem registo, com ações de retomar e limpar. A implementação deve consultar a tabela de downloads e as raízes já concedidas pela SAF; não varrer nem apagar armazenamento externo sem consentimento. Ao remover perfil, deixar explícito se os conteúdos são eliminados ou transferidos para uma biblioteca comum. Esta ideia resolve o problema de ficheiros invisíveis e ajuda o utilizador a recuperar espaço sem perder conteúdo de outro perfil.

Ordem revista pelo Council:

1. Corrigir assinatura/parser/ABI da release e os caminhos Kids/restauro.
2. Corrigir destinos únicos de download e a corrida Stalker/direct tune; preservar cancelamento correto.
3. Fechar transporte/revogação de LocalSync e validação/exportação dos backups.
4. Definir a política de Downloads e eliminação de perfil; alinhar a cobertura i18n com a App.
5. Corrigir navegação e executar validações físicas; medir antes de otimizar performance.
6. Avançar primeiro com temporizador de sono e diagnóstico local; depois decidir as outras ideias existentes segundo privacidade, suporte do provider e valor observado.

### Estado final desta terceira ronda

- **Aceites:** colisão de destinos de downloads, limpeza/retensão de ficheiros ao apagar perfil, corrida de direct tune por número, identificação temporariamente errada no OSD e complemento do indicador i18n.
- **Condicionais:** o efeito da colisão depende de nomes iguais; a severidade dos ficheiros órfãos depende da política de retenção; PLY-11 depende do timing; a escrita do Histórico precisa de uma decisão explícita sobre o que significa “visto”.
- **Rejeitados ou não promovidos:** budget fullscreen agregado, três pedidos por rajada, PLY-15, fuga P1 de cleanupScope e o Histórico como bug reproduzido.
- **Ideias mantidas:** todas as ideias do quadro anterior continuam válidas como propostas com as respetivas soluções; temporizador de sono e diagnóstico local têm melhor relação entre valor e dependências. A beta UPSCALING-BETA-01 permanece investigação opcional, desligada por defeito, dependente de medição de GPU/RAM, latência e sincronismo A/V numa box real.
- **Limite:** esta análise não implementa nem autoriza implementar as recomendações; percentagens de melhoria não foram medidas.

## Adenda — quarta ronda: reanálise independente e triagem final

**Data:** 19/10/2026  
**Checkout:** main, commit bdffec4.  
**Core:** snapshot personalizado e2081e78…e14890.  
**Revisão:** agentes de dados/sincronização, reprodução e interface/arquitetura; o Council cruzou os pareceres com o código deste checkout, a análise consolidada da Roda 5 e as decisões já registadas neste documento.  
**Referência:** [relatório consolidado da Roda 5](ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md).

Esta passagem foi estática e não alterou App ou Core. Não foram executados builds, testes nem validações em box. Os riscos de foco, memória, tempo de execução e comportamento do provider continuam sujeitos a validação física; as classificações abaixo distinguem o mecanismo visível no código do impacto ainda não reproduzido.

### Correções adicionais aceites — com solução

| Veredicto | Achado e evidência | Solução concreta |
|---|---|---|
| **P3 condicional — colisão de legendas locais com o mesmo nome** | SubtitleRepository.kt:132–140 reutiliza a cache pelo nome do ficheiro antes de ler o conteúdo; SubtitleDao.kt:44–46 procura apenas por fileName. O restauro faz a mesma associação em BackupManager.kt:1294–1301. Se dois ficheiros de pastas diferentes se chamarem, por exemplo, sub.srt e tiverem conteúdos distintos, a segunda importação pode devolver a primeira legenda. | Calcular SHA-256 do conteúdo normalizado e deduplicar por hash; manter o nome original apenas como rótulo. Criar caminho e linha próprios quando o hash divergir. Para backups antigos, calcular o hash dos bytes restaurados; numa implementação Core, fazer uma migração aditiva e não substituir o snapshot personalizado. |
| **P3 — restauro de legendas pode terminar com conteúdo omitido sem aviso** | BackupManager.kt:1242–1247 converte uma falha global de importação de legendas em zero e continua; em :1303–1310 falhas de escrita são ignoradas item a item. ImportSummary (:574–580) e BackupScreen.kt:191–205 não apresentam um contador de legendas omitidas. | Devolver totais importados, reutilizados e omitidos, com causa por secção; apresentar “restauro parcial” e permitir repetir a secção. Se uma secção selecionada não puder ser aplicada por falha global, não a apresentar como restauro completo. |
| **P2 condicional — onboarding pode reaparecer como importação em curso após morte do processo** | SetupWizard.kt:101 guarda Step com rememberSaveable, enquanto importOrigin e os dados de apoio ficam em remember (:105–115). SetupViewModel.kt:54–55 expõe diretamente o estado transitório do SourceImporter; a injeção do Core é factory e um novo SourceImporter começa em Idle. SetupWizard.kt:833–854 apresenta Idle e Running com o mesmo spinner de “a preparar catálogo”. Depois de o Android matar o processo, a etapa IMPORTING pode ser restaurada sem o trabalho nem o estado que lhe davam suporte. | Ao recriar o fluxo, se a etapa for IMPORTING mas o importer estiver Idle, apresentar “importação interrompida” com retomar, recomeçar ou sair. Tornar a importação repetível por operação/identidade persistida e reconciliar perfis e conteúdo que já chegaram à base de dados. Não guardar PINs, passwords nem URLs de credenciais no estado restaurado; não criar automaticamente um perfil novo ao repetir. Confirmar o cenário com process death num dispositivo Android. |

### Playback — achados mantidos, condicionais e rejeitados

| Item reavaliado | Veredicto do Council | Solução se mantido |
|---|---|---|
| **R2-3 — faixa de áudio ao mudar de engine** | **Manter P3**, com impacto maior se a faixa escolhida for audiodescrição. OwnTVPlayer.kt:2230–2295 transporta a seleção de legenda durante o toggle manual, mas não captura/restaura a seleção de áudio nos dois sentidos. | Antes de trocar, guardar a faixa ativa por idioma/label e canais, com índice como fallback; reaplicá-la quando o engine de destino publicar as faixas. Se não houver correspondência, manter Auto e comunicar a mudança. |
| **R2-6 — MIME de legenda externa no ExoPlayer** | **Manter P3 condicional** para formatos como .sup: ExoSubtitleEngine.kt:414–418 trata qualquer extensão desconhecida como SubRip. Não generalizar para .sub, que pode ser texto ou VobSub. | Mapear só formatos suportados e identificados com segurança; encaminhar formatos não suportados para engine compatível ou indicar indisponibilidade. Confirmar a compatibilidade do formato em cada engine antes de anunciar suporte. |
| **PLY-02 — foco do D-pad no multiview** | **Manter P2 condicional.** MultiviewScreen.kt:90 recria FocusRequesters quando muda o número de mosaicos; o addTile atualiza o índice selecionado (:128), mas o pedido inicial de foco só acontece uma vez (:92). O código permite desfasar o destaque selecionado e o foco real; ainda não foi reproduzido com comando remoto. | Associar requesters a uma identidade estável por mosaico e pedir foco ao novo mosaico depois de o compor. Validar a ordem do foco com adição/remoção de tiles numa box. |
| **PLY-04 — pedidos de foco em diálogos de catch-up** | **Condicional P2 se reproduzido; de outro modo, consistência de UX.** CatchupJumpDialog.kt e CatchupManualTimeDialog.kt usam pedidos com atraso fixo; já existe um helper com tentativas em PlayerHudDialogs.kt:201 que estes diálogos não partilham. | Extrair um helper comum que aguarde a composição e tente foco de forma limitada, sem ciclo infinito; usá-lo nos diálogos e medir no comando remoto antes de classificar como defeito observado. |
| **PLY-09 — ciclo de vida do bitmap do freeze frame** | **Manter como risco P2 condicional, confiança média.** MpvVideoSurface.kt:138–141 entrega o bitmap ao Compose; o Core pode reciclar o anterior ao publicar um novo. O código estático não demonstra que o Compose volte a desenhar depois do recycle nem que ocorra crash. | Definir ownership explícito: entregar uma cópia sob responsabilidade da UI ou libertar o bitmap original só depois de a UI confirmar que já não o usa. Reproduzir com atualização rápida de frames e recolher logcat antes de o chamar crash confirmado. |
| **PLY-03 — dois FocusRequesters no mesmo modificador** | **Rejeitar como bug.** A presença de dois requesters no mesmo elemento não demonstra que um oculte o outro; não foi encontrada falha funcional. | Não alterar por hipótese. Só simplificar se houver outro motivo de manutenção. |
| **PLY-07 — alegação de três aberturas numa rajada** | **Rejeitar a contagem de três.** O fluxo abre a primeira seleção e cancela as seleções intermédias para chegar à última; o código atual não sustenta três pedidos simultâneos. A limitação que um provider possa impor a duas aberturas continua dependente desse provider. | Não acrescentar debounce adicional sem medir; se um provider real limitar o padrão primeira+última, avaliar opção baseada em KeyUp ou atraso configurável sem penalizar um zap isolado. |
| **PLY-10 — cancelar lookup do direct tune** | **Rejeitar como bug confirmado.** Abrir outro diálogo/overlay cancela a lookup por desenho; a consulta verifica a geração antes de despachar e não foi demonstrado que o tune seja aplicado enquanto só o OSD se perde. | Manter o cancelamento por contexto; reabrir apenas com sequência reproduzida em box que mostre estado do player e OSD divergentes. |
| **PLY-15 — publicação da âncora do direct tune** | **Manter rejeitado**, conforme a terceira ronda: o callsite é síncrono e não foi identificada janela para uma pressão intercalar antes da âncora. | Não mudar a ordem sem reprodução de CH+/CH− sem resposta. |

### Dados, backups e segurança — reavaliação dos itens R5/R6

| Item | Veredicto revisto | Solução se mantido |
|---|---|---|
| **SEC-12 — iterações PBKDF2 do cabeçalho** | **Manter P2 condicionado a importar um ficheiro local malicioso.** A derivação decorre em IO, por isso o bloqueio direto da interface foi exagerado; o número de iterações ainda não tem limite validado antes da derivação. | Validar formato, algoritmo, salt e intervalo antes de derivar; rejeitar valores acima do teto com erro legível e compatibilidade para backups legítimos. |
| **SEC-13 — cabeçalho e iterações na exportação** | **Rejeitar como bug atual.** A escrita usa o mesmo valor para derivação e cabeçalho; mantê-lo numa constante/parâmetro explícito é higiene futura. | Não corrigir uma assimetria que não existe; se o formato evoluir, testar a consistência entre cabeçalho e KDF. |
| **SEC-14 — cópias de memória no LocalSync** | **Manter condicional, impacto não medido.** LocalSyncClient.kt:88–118 materializa bytes, Base64/String e novo array, mas não confirma OOM, tamanho típico nem pressão em box. | Impor limite de tamanho antes de ler e transmitir; passar para streaming binário com buffer limitado se uma medição em backups representativos justificar. |
| **SEC-15 — exportação Core sem palavra-passe** | **Rebaixar para P3/hardening.** A API Core permite exportação sem cifra, mas BackupViewModel.kt:146–153 bloqueia senha vazia e LocalSync cifra. | Na API Core, recusar conteúdo com credenciais sem cifra ou redigir os campos sensíveis de forma explícita; preservar o fluxo normal cifrado. |
| **DATA-01 — backup com secções vazias após falha DAO** | **Manter condicional.** BackupManager.kt:216 e :321–342 usa valores vazios após falhas de leitura, podendo gerar arquivo aparentemente válido sem todas as secções. É a mesma família do erro de exportação já aceite, não uma descoberta independente. | Falhar a exportação ou escrever um indicador de backup incompleto com lista de secções omitidas; não mostrar sucesso sem ressalvas. |
| **DAT-01 — snapshot e invalidação no paging Live** | **Manter como custo estrutural com impacto por medir.** GroupedChannelPagingSource.kt:22–60 mantém snapshot/invalidations que podem reconstruir dados em alterações; não se atribui aqui um número de heap/tempo medido. | Carregar por páginas e revisão de conteúdo; remover o callback de invalidação quando a fonte for descartada e evitar refazer o snapshot se a revisão não mudou. |
| **DAT-02 — flows Eagerly no Live/Epg ViewModel** | **Rebaixar a otimização para P3 condicionada a cold-start/perfilagem.** O início de flows em ViewModels Activity-scoped é verificável, mas a composição não prova que todas as consultas pesem no arranque. | Tornar preguiçosos os flows de base de dados sem consumidor; manter Eagerly apenas para flags necessárias ao playback síncrono. Comparar trace de arranque e abertura de Live antes/depois. |
| **DAT-03 — maxSize unbounded no paging** | **Rebaixar para risco condicional.** O Paging retém páginas conforme a navegação; a configuração não demonstra por si só retenção imediata de 50 mil canais nem os 15–30 MB indicados. | Medir heap ao percorrer catálogos grandes; se a retenção for material, impor maxSize e placeholders compatíveis com as operações de foco/scroll. |
| **DAT-04 — worker de backup sem foreground** | **Manter condicional, sem chamar falha de execução confirmada.** Não foi medida uma exportação que exceda os limites do Worker nem um ciclo de cancelamento/retry real. | Medir backup completo em box modesta. Só se ultrapassar limites, usar foreground permitido e checkpoint retomável; preservar o último backup válido enquanto o novo não estiver completo. |
| **DAT-05 — throttle dentro de flatMapLatest** | **P3 condicional a alterações frequentes das customizações e custo de COUNT.** A reinicialização do throttle é plausível no fluxo descrito; não foi medido o custo percebido. | Mover o throttle para depois de flatMapLatest ou usar coalescência equivalente; comparar queries em alterações rápidas antes de alterar. |
| **DAT-06 — scope de cleanup do timeshift** | **Rejeitar a alegação de leak acumulado por toda a vida do processo.** Os trabalhos são curtos/serializados; um scope não cancelado não demonstra, por si só, trabalho ou recurso retido após a conclusão. | Não fazer uma mudança de lifecycle urgente. Se a tarefa ainda puder sobreviver após fechar a tela, guardar o Job e cancelar apenas quando for seguro interromper a limpeza, ou garantir um executor de cleanup com conclusão observável. |
| **DAT-07 — N+1 ao filtrar Downloads** | **Manter P3 condicional sobretudo a perfis Kids com muitas linhas.** DownloadsViewModel.kt:53–88 pode consultar metadados por item; o impacto por emissão não foi medido. | Buscar os IDs/categorias necessários em lote e filtrar na memória; medir com catálogo/downloads representativos antes de priorizar. |
| **DAT-08 — reconsulta da validade da source** | **Rebaixar para P3 condicional e limitar ao painel Manage Sources.** Falhas/null podem repetir em emissões; respostas positivas ficam cacheadas no ViewModel. Não é uma tempestade geral de rede. | Cache negativo com TTL curto e invalidação por revisão de credenciais; executar a consulta em IO e observar mudanças reais da source. |
| **DAT-09 — listas completas do EPG** | **P3, hipótese de pico de memória a perfilar.** A coexistência de listas é possível durante a carga, mas o pico descrito não foi medido. | Fazer heap trace com guia grande; se for gargalo, guardar projeção mínima ou paginar canais e manter dados de programa lazy. |
| **DAT-10 — query por candidato no picker EPG** | **P3 para o picker manual limitado a 2.000 candidatos.** O padrão N+1 existe, mas não equivale a pesquisa de catálogo ilimitada. | Usar consulta em lote de IDs e janela de tempo se o tempo do picker justificar. |
| **DAT-11 — painel de detalhe após reentrada** | **P3 condicional de UX.** WhileSubscribed sem timeout pode reiniciar o atraso ao sair/entrar; não está demonstrado que o utilizador perca o detalhe em todos os retornos. | Preservar o valor em cache e aplicar o atraso apenas à fonte de fallback; validar reentrada com comando remoto. |
| **DAT-12 — versionGroupKeys write-only** | **P3 de limpeza/allocação.** As referências verificadas só escrevem e limpam o mapa; confirmar o conjunto completo de consumidores antes de remover. | Remover estado e escritas apenas após confirmar ausência de leitores; caso exista intenção futura, documentar e limitar a capacidade. |
| **DAT-13 — leitura síncrona de locale** | **Rebaixar para possível custo de cold-start, não para leitura repetida do ficheiro em cada Activity.** OwnTVApp.kt:62–65 e MainActivity.kt:135–137 leem a mesma preferência; SharedPreferences é síncrono, mas o segundo acesso não prova nova leitura/parsing em disco. | Reutilizar o tag já lido pela Application ao preparar os recursos da Activity, mantendo a necessidade de aplicar configuração nos dois contextos; medir cold start em armazenamento lento antes de priorizar. |
| **DAT-14 — worker diário para política mensal** | **Manter como otimização de baixa prioridade. Rejeitar a alegação de constraints antigas causada por KEEP:** a periodicidade/constraints são fixas no pedido atual e o worker consulta a política guardada quando executa. | Se o wake-up diário tiver custo medido, agendar mais perto do intervalo configurado ou atualizar o pedido quando a política mudar; não afirmar que KEEP ignora constraints que hoje não variam. |
| **DAT-15 — pedido manual de backup com KEEP** | **P3/condicional a uma corrida rara.** O ecrã desativa ações quando a operação está ativa, reduzindo o cenário de toque engolido. | Exibir “em fila/em curso” e permitir substituir o pedido pendente com política explícita, sem iniciar duas exportações concorrentes. |
| **DAT-16 — polling de progresso VOD a cada 10 s** | **P3 de eficiência; 8.640 é limite teórico, não wakeups medidos.** O valor depende de ViewModel ativo e playback. | Garantir que o loop só exista durante reprodução e termine em pause/stop/saída; medir bateria antes de mudar a frequência. |
| **DAT-17 — episodes Eagerly** | **P3 condicionado a SeriesViewModel permanecer no back stack.** O fluxo não prova query permanente quando não há série aberta. | Usar WhileSubscribed com timeout nas consultas de episódios, preservando o estado visual da série ao regressar. |
| **DAT-18 — dois cachedIn no Paging** | **Rejeitar a alegação de Paging incorreto.** A cadeia observada pode manter o mesmo PagingData para múltiplas leituras; redundância não demonstra re-collect inválido. | Não reescrever sem falha de coleta/reprodução mínima; se houver, reduzir para um cachedIn e conservar teste de nova subscrição. |
| **DAT-19 — cleanupScope do multiview** | **Rejeitar severidade P1/leak demonstrado; no máximo higiene de scope.** O encerramento não prova acumulação de trabalho ativo. | Se for ajustado, cancelar o SupervisorJob no fecho final do estado sem interromper liberação de recursos que ainda esteja em curso. |
| **DAT-20 — mapa now-playing e coletores por linha** | **P3 de otimização, sem impacto medido.** Há cópias de mapa e coletores associados às linhas visíveis, mas a lista é lazy e os mapas são limitados; não foi provado atraso no D-pad. | Se um trace mostrar custo, publicar seletores por canal ou um estado mais estreito e evitar acordar consumidores que não estão visíveis. |
| **DAT-21 — dismissedKeys de lembretes** | **Confirmar redundância P3 de sessão.** ProgrammeReminderStore.kt:47,96–104,131,143,174–176 adiciona chaves ao conjunto sem limite nem limpeza automática; como dismiss/auto-consumo já remove o lembrete sob o mesmo lock, o lookup posterior aparenta ser inalcançável. Não é fuga persistente entre reinícios. | Remover dismissedKeys e o respetivo lookup se a revisão final confirmar que nenhum fluxo mantém lembretes dismissados na lista; em alternativa, podar chaves expiradas. |

### Interface, acessibilidade e desempenho — reavaliação de UX/PERF

| Item | Veredicto final | Solução se mantido |
|---|---|---|
| **UX-01 Pesquisa** | **Manter P2 de descoberta.** Há atalho configurável, por isso a pesquisa não é totalmente inacessível, mas falta entrada visível na navegação. | Adicionar Pesquisa ao TopBar/rail e preservar o atalho; confirmar foco ao abrir e regressar pelo D-pad. |
| **UX-02 Home sem Trending/Continue VOD** | **Manter como escolha de produto, P2 se a promessa inclui VOD.** HomeViewModel.kt:546–548 passa liveOnly=true. | Escolher e documentar Home Live-only ou ativar toggle VOD; remover os estados/indicadores mortos se ficar Live-only. PERF-02 só volta a ter custo se a linha for composta. |
| **UX-03 Downloads** | **Manter P2 de coerência, dependente da definição do produto.** O ecrã fixa LIVE, mostra um botão sem ação e abre Gravações; não oferece separadores de downloads VOD. | Implementar separadores Movies/Series ligados aos downloads ou renomear a rota para Gravações e remover botão e ramos inertes. UX-14 fica fundido aqui. |
| **UX-04 More** | **Manter como hub incompleto; rejeitar “destinos globalmente inalcançáveis”.** Favoritos/Histórico e LocalSync têm outras rotas. | Acrescentar ao hub só os destinos suportados e remover enums/coletores sem uso. |
| **UX-05 continuar a ver** | **Rebaixar para P3 de descoberta.** Existe atalho de último conteúdo, mas não um chip visível geral de VOD. | Se for importante para o produto, expor “Continuar a ver” com estado local e respeitar o perfil; não tratar como falha de reprodução. |
| **UX-06 descrição dos botões** | **Manter como lacuna específica de acessibilidade P2/P3.** MiniPlayer.kt:184–221 e AudioNowPlayingBar.kt:375–418 têm ações focáveis só com ícone. O efeito real em TalkBack requer validação. | Adicionar descrição semântica por ação/estado e nomear janelas/controles; verificar TalkBack e D-pad em box. |
| **UX-07 confirmar apagar download/gravação** | **Rebaixar a preferência P3, não defeito.** A eliminação segue uma ação explícita do menu. | Preferir Undo temporário ou lixeira para reduzir perda acidental; confirmação adicional é decisão do produto. |
| **UX-08 foco do Mini Guide** | **Condicional P3, não bug reproduzido.** Tentativas de foco e trap podem atrasar ou falhar em hardware específico. | Partilhar o helper de foco limitado e verificar abertura, saída e retorno ao controlo remoto antes de o elevar. |
| **UX-09 categoria vazia** | **Condicional P3.** Uma lista vazia pode deixar o overlay sem destino de foco, mas depende do estado em que o seletor é aberto. | Se reproduzido, apresentar “Sem categorias” e ação focável Fechar; limitar o focus trap à área válida. |
| **UX-10 recomposição do shell** | **Manter a atualização periódica como facto, rebaixar impacto para P3 até trace.** Compose pode ignorar filhos estáveis; “toda a árvore reconstruída” não está demonstrado. | Mover o ticker para o consumidor e comparar contagem de recomposições/jank antes e depois em hardware. |
| **UX-11 focusRestorer condicional** | **Condicional P3, por destino.** Pode perder o ponto anterior, mas Live tem requesters próprios e o comportamento não foi reproduzido. | Testar retorno por destino; instalar o restorer de forma uniforme onde o teste demonstrar perda. |
| **UX-12 Back na faixa de Guia do Home** | **Rejeitar como bug confirmado.** O Back limpa a seleção do modo da faixa; não é necessariamente uma ação sem efeito. | Se a política desejada for sair no primeiro Back, limpar o estado e devolver o evento ao shell. Decidir após teste de navegação. |
| **UX-13 fallback focável do Hero** | **Manter fundido com Home vazio/onboarding, não como achado autónomo.** | Trocar o placeholder por uma ação útil ou retirar o foco se não existir ação; apresentar estado “TV pronta” com próximos passos. |
| **UX-14 botão LIVE inerte** | **Fundido em UX-03.** | Resolver com a bifurcação de produto indicada em UX-03. |
| **UX-15 controlos ocultos com alpha zero** | **Manter hipótese P3 de acessibilidade.** O foco D-pad pode revelar os controlos, mas TalkBack pode anunciar conteúdo visualmente oculto. | Fornecer ação semântica “Mostrar controlos” e anunciar expansão; ocultar o grupo da árvore semântica só quando existir rota para o revelar. Validar em box. |
| **PERF-01 Settings com muitos flows** | **Oportunidade P3 a medir, não defeito de jank confirmado.** | Traçar o ecrã e mover cada leitura de estado para o componente que a consome apenas onde o perfil demonstrar trabalho evitável. |
| **PERF-02 animação do Hero a 80 ms** | **Condicional e dormente no Home atual**, porque Trending não é composto com liveOnly=true. | Se UX-02 reativar VOD, limitar animação ao Hero visível/ativo e medir. Não presumir que animateFloatAsState evita recomposições. |

### Inovações — o que continua válido e como implementar

| Ideia | Veredicto do Council | Caminho de implementação |
|---|---|---|
| **Catálogo demo/primeiro arranque útil** | **Válida com dependência legal; protótipo visual é imediato.** | Começar com catálogo sintético sem URLs de stream. Só acrescentar streams públicos depois de validar licença e origem; identificá-los como demonstração e nunca misturá-los com sources do utilizador. |
| **Coach D-pad e ação focável “Opções”** | **Válida como melhoria de descoberta/acessibilidade.** O long-press continua a funcionar, mas não é autoexplicativo. | Mostrar um coach curto, descartável e com opção de ignorar; adicionar uma ação focável “Opções” para o item selecionado, e overflow na lista quando a área de preview estiver oculta. Dar rótulo semântico e testar TalkBack/foco. |
| **Temporizador de sono** | **Válida, prioridade inicial de produto.** | Escolher duração no HUD, cancelar com confirmação simples, libertar engine/ligação ao terminar e nunca parar gravação ativa. Não depender de “fim do programa” sem EPG fiável. |
| **Diagnóstico local da qualidade** | **Válida se não atribuir culpas sem evidência.** | Recolher localmente tempo até primeira imagem, rebuffer, erros e retries; exportar só por ação do utilizador e sem telemetria. Separar rede/provider/decoder e mostrar “causa inconclusiva” quando os sinais não bastam. |
| **Cartão de saúde da source** | **Válido sem sondagens automáticas.** | Mostrar contagens, cobertura EPG, falhas vistas durante uso normal e última sync. Se existir teste ativo, exigir opt-in, executar serialmente e parar no limite configurado pelo provider. |
| **O que ver agora / Surpreende-me** | **Válidas como descoberta local.** | Combinar EPG/catch-up e catálogo já carregado; sortear VOD não visto com filtros de favoritos, itens ocultos e Kids. Guardar histórico local e oferecer novo sorteio/undo. |
| **Audiodescrição por perfil** | **Válida e de acessibilidade.** | Dar destaque a faixas explicitamente marcadas como áudio descrição e guardar preferência por perfil; não inferir audiodescrição apenas pelo idioma. |
| **Voz na Pesquisa** | **Válida com opt-in e transparência.** | Mostrar microfone apenas quando houver recognizer; explicar se o áudio sai para um serviço cloud e preservar teclado D-pad como alternativa. |
| **Pacotes de tema** | **Nova proposta válida P3, app-only.** | Exportar/importar pacote visual versionado com preview e limites de tamanho/dimensões; allow-list apenas tema, fonte e wallpaper. Excluir fontes IPTV, URL, PIN, Kids e estado de playback; ignorar pacote incompatível sem perder o tema atual. |
| **Ratings etários no Kids** | **Condicional, depois das salvaguardas parentais P1.** | Começar por VOD com correspondência de metadata confiável; mostrar fonte do rating. Rating em falta nunca significa seguro; manter o classificador atual como fallback e exigir autorização adulta para override. Não prometer cobertura de Live. |
| **Comando da TV pelo telemóvel** | **Condicional, bloqueado pelo hardening LocalSync.** | MVP com ações allow-listed de navegar, pesquisar e selecionar; não transmitir URL nem credenciais IPTV. Emparelhar com token efémero, autenticação forte, limite de tentativas, revogação imediata e teste em LAN real. |
| **Seleção manual de bitrate** | **Condicional a variantes HLS e suporte igual nos engines.** | Mostrar opções apenas quando a playlist e o engine expuserem variantes; manter Auto por omissão, indicar resolução/bitrate e nunca mudar qualidade sem escolha. Esconder para TS/M3U sem variantes. |
| **Auto-Tune Memory** | **A ideia mantém valor, mas fica bloqueada.** | Só persistir após playback confirmado; guardar por canal/provider com TTL curto, auto-desaprender, botão de reset e proteção para lições de DASH não substituírem URL explícita. Corrigir primeiro as dependências B1/B7 descritas na Roda 5. |
| **EPG diferencial** | **Condicional, não genérico para todos os providers.** | Usar ETag/Last-Modified quando oferecidos, com fallback para fetch completo e confirmação de conteúdo; não inventar endpoint delta. A opção “só em Wi-Fi” para EPG já permanece válida. |
| **Códigos partilháveis / retoma entre TVs** | **Condicionais e adiados por privacidade e isolamento de perfis.** | Se avançarem, partilhar só referência opaca sem URL nem credenciais; no destino, resolver contra source local. Retoma fica local por perfil; partilha entre perfis exige PIN adulto e confirmação. |
| **OLED, plugins de metadata, relay e salto de anúncios** | **Adiar.** Não há prova de que o screensaver falhe; plugins elevam risco de scraper/credenciais; relay aumenta superfície de dados; salto automático tem falsos positivos. | Reabrir OLED só com comportamento observado em box. Plugins teriam de usar presets sem transmitir credenciais. Relay exigiria cifragem ponta a ponta e opt-in. Salto de anúncios só após dados suficientes e validação de falsos positivos. |
| **UPSCALING-BETA-01** | **Manter como estudo futuro opcional, desligado por defeito.** | Medir primeiro escala nativa da TV, capacidades Exo/mpv, GPU/RAM, latência e sincronismo A/V na box-alvo. Buffer maior não prova detalhe de imagem adicional. |

### Reavaliação final e ordem revista

1. **Primeiro: distribuição e controlo parental.** Preservar os bloqueios de release, validação de assinatura/ABI/versionCode e autorização adulta para alterações Kids/restauro. As soluções da primeira parte deste documento continuam válidas.
2. **Depois: integridade dos dados.** Aplicar limites de KDF; tornar backups incompletos visíveis; proteger LocalSync; corrigir identidade de legendas por conteúdo e contabilizar omissões.
3. **Playback e downloads.** Manter a corrida Stalker/direct tune e colisões de destinos já aceites; preservar a faixa de áudio ao trocar engine; tratar .sup apenas com fallback realmente suportado. Validar PLY-02/04/09 em box.
4. **Recuperação e usabilidade.** Corrigir recuperação do onboarding após process death, navegação de Downloads/Pesquisa e labels acessíveis; testar D-pad/TalkBack antes de classificar as hipóteses de foco como bugs.
5. **Medição antes de otimização.** DAT/PERF de listas, flows, cópias, polling e recomposições ficam em P3 ou condicionais onde não há trace/heap/bateria; medir em box modesta e corrigir só o custo reproduzido.
6. **Produto novo.** Começar por sono, coach/ação Opções, pacotes de tema e diagnóstico local; manter metadata parental, segundo ecrã e bitrate atrás das dependências indicadas.

**Estado do Council:** aceites como novas correções a colisão de conteúdo de legendas, o restauro parcial silencioso e a recuperação do onboarding; mantidos como riscos condicionais o foco multiview, foco de diálogos e ciclo de vida do freeze frame; reclassificadas várias afirmações R5 de desempenho/memória; rejeitadas alegações sem suporte atual como PLY-03, três aberturas por rajada, PLY-10, SEC-13, DAT-18, DAT-19 como P1 e Back do Home Guide como bug. As ideias acima que continuam válidas têm solução e dependências explicitadas; as restantes ficam adiadas pelos motivos registados.

**Limite final:** esta decisão é um plano de análise, não uma autorização de implementação. Não foram medidos ganhos percentuais nem feitos builds, testes ou ensaios físicos nesta ronda.
