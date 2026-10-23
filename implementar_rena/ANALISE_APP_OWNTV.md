# 📊 Análise Completa — OwnTV (Android TV IPTV Player)

**Projeto analisado:** `C:\Users\renat\Downloads\OwnTv-main`  
**Pasta de saída desta análise:** `C:\Users\renat\Downloads\OwnTv-main\implementar_rena`  
**Ficheiro gerado:** `implementar_rena/ANALISE_APP_OWNTV.md`  
**Data da análise:** 18/10/2026  
**Elaborado por:** Conselho de multi-agentes profissionais especializados (UI/EPG, Player/Áudio/Lifecycle, Core/Parser/Armazenamento/DB, Coordenador/Council)  
**Modalidade:** Análise estática de fontes e contratos — **sem implementação de código**, **sem build**, **sem execução de testes**, **sem ensaios em dispositivos físicos**

---

## 1. Metodologia — Conselho de Multi-Agentes

A análise foi estruturada como se quatro agentes especializados tivessem inspecionado o checkout de forma independente, seguidos por um **Council** coordenador que consolida, deduplica, classifica por severidade e define a ordem de implementação.

### Agentes envolvidos

| Agente | Domínio de responsabilidade |
|--------|-----------------------------|
| **Agente UI/EPG** | Interface Compose para TV, foco (D-pad), navegação, guia (EPG), pesquisa, categorias, perfis, ecrãs de settings |
| **Agente Player/Áudio/Lifecycle** | Motores libmpv/ExoPlayer, zapping, timeshift, gravações, multiview, áudio, foco de áudio, standby, teardown nativo |
| **Agente Core/Parser/Armazenamento/DB** | Parsers Xtream/M3U/Stalker, XMLTV, Room DB, FTS4, Paging 3, backup/restore, redacção de segredos, CI/snapshots |
| **Council (Coordenador)** | Consolida resultados, rejeita falsos positivos, define severidade (P1/P2/P3), estabelece ordem de fases e critérios de aceitação |

### Regras do Council aplicadas

- Não aplicar correções em bloco sem evidência no código.
- Distinguir defeito confirmado por leitura de hipótese não reproduzida.
- Não propor "revisões gerais" do player, da base de dados ou do modelo de threads.
- Priorizar domínios de **backup e privacidade**, **coerência do guia e das definições**, depois **pausa/retoma**.
- Medir antes de aplicar otimizações (ex: grelha do guia, upscaling por hardware).
- Não prometer eliminação de bloqueios de fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias.
- Para cada item identificado, deve existir uma **solução descrita** (o que fazer, não implementação linha a linha).

---

## 2. Resumo Executivo

A OwnTV é uma aplicação **Android TV** nativa (Kotlin + Jetpack Compose for TV) que funciona como **player IPTV "bring-your-own-source"**: Xtream, M3U e Stalker/Ministra. Conta com dois motores de reprodução (libmpv/FFmpeg e ExoPlayer/Media3), suporte a EPG/XMLTV, timeshift, gravações, multiview (até 4 canais), perfis com PIN e Kids mode, downloads offline, backup/restore encriptado, 26 idiomas (RTL-aware) e atualização in-app.

O código está **bem estruturado, maduro e já sujeito a várias auditorias** (documentos em `docs/`). No entanto, foram identificados **vários bugs críticos (P1)**, problemas P2/P3, oportunidades de melhoria de código e UX, e ideias de inovação que devem ser avaliadas **após** a estabilização.

Cada item identificado nesta análise tem a sua **solução descrita** (o que corrigir/melhorar/implementar), sem escrita de código.

---

## 3. Bugs Identificados — Com Soluções

### 3.1 Integridade e Privacidade (P1 — corrigir primeiro)

#### BUG-P1-01 — Opacidade de legendas (-1) torna backup irrestaurável

**Descrição:** `SubtitleStyle.OPACITY_DEFAULT = -1` é aceite na exportação de backup, mas o validador de importação aceita apenas `0..100`. Restaurar um backup com este valor bloqueia o restauro de todas as secções selecionadas.

**Localização:** `SettingsRepository.kt` (Core) + `BackupValueValidator` + `VideoPlayerSettingsScreen.kt:3033` (app)

**Solução:**
- Alterar o `BackupValueValidator` (ou equivalente) para aceitar exatamente `-1` e `0..100` na chave `SUB_BG_OPACITY`, distinguindo "predefinição do renderer" (-1) de "opaco total" (0).
- Garantir que o contrato de exportação e o de importação usem o mesmo intervalo válido.
- Testes: setter → exportação → JSON → importação; valores `-2` e `101` devem ser recusados.
- Não remover a opção "Default" da UI, apenas alinhar o validador com o domínio real.

#### BUG-P1-02 — Ocultação de segredos não uniforme nos pedidos de catálogo

**Descrição:** O redator privado em `HttpClient.kt` cobre apenas algumas chaves de query. O helper público `redactUrl` cobre mais casos, mas mensagens de exceção e `Throwable` bruto podem vazar credenciais em caminhos Xtream, userinfo e tokens.

**Localização:** `HttpClient.kt` (Core): linhas 45, 58, 108–109, 144–151, 65–76, 93–97

**Solução:**
- Unificar a política de redacção de URLs para todos os caminhos de catálogo (query, caminho Xtream, userinfo, chaves assinadas).
- Rever mensagens de log e cadeias de exceção para não incluir `Throwable` bruto com segredos.
- Conservar status/tipo/duração úteis nos logs; evitar que credenciais apareçam em mensagens de `IOException`.
- Teste de aceitação: URLs sintéticas com query, caminho Xtream, userinfo e endereço em mensagem de `IOException` não revelam segredos.

#### BUG-P1-03 — Categorias ocultas EPG reaparecem no guia

**Descrição:** `applyEpgMatches` resolve canais em falta e os acrescenta novamente sem reaplicar `hiddenCatIds`. A lista final só filtra capacidade de diferido. Pode reaparecer nome/EPG de categoria oculta, inclusive no perfil infantil.

**Localização:** `EpgViewModel.kt:1155–1177` (app)

**Solução:**
- Aplicar a política final de elegibilidade **depois** de integrar associações EPG, incluindo fontes, categorias ocultas e itens ocultos.
- Garantir que o perfil infantil continue a respeitar `hiddenCatIds` após a fusão de matches.
- Testes: match/fallback, categoria oculta (normal e infantil), reimportação com novo ID de fonte.
- Não demonstra bypass de reprodução adulta (revalidado noutros percursos), apenas exposição no guia.

#### BUG-P1-04 — Restauro seletivo de backup confunde IDs coincidentes

**Descrição:** Perfis associados por nome; fontes entram no mapa apenas quando resolvidas; `remapKeys` e `remapEnginePinKey` preservam entradas numéricas sem mapping. IDs coincidentes entre instalações podem aplicar arranque/personalizações/opções de reprodução a outro perfil/lista.

**Localização:** `BackupManager.kt` (Core): `remapKeys:1673–1681`, `remapEnginePinKey:1792–1797`, `PlaybackDatabaseBackup.kt:36–45`

**Solução:**
- Usar apenas associações resolvidas para dados dependentes de perfil/fonte.
- Manter tratamento específico de URLs legadas e chaves locais.
- Antes de mudar helpers partilhados, testar: colisão de ID com identidade diferente, mesma identidade com novo ID, restauro completo, backups antigos.
- Não trocar todos os remaps indiscriminadamente.

### 3.2 Reprodução e Preferências (P2)

#### BUG-P2-01 — Watchdogs do ExoPlayer não rearman na pausa/retoma

**Descrição:** Pausa chama `mainHandler.clear()`. Na retoma com canal já iniciado, só rearma `progressWatchdog`; se o player continuar BUFFERING, o watchdog exige READY e o `stallWatchdog` só é armado na mudança para BUFFERING. Retomar sem mudar de estado não garante novo callback desse estado. O orçamento pode conservar tentativas antigas até outra transição que rearme o reset.

**Localização:** `LivePreviewEngine.kt:2863–2891, 1331–1350` (Core)

**Solução:**
- Centralizar o rearmar de watchdogs conforme estado atual, intenção de reprodução e owner.
- Retoma em READY: agendar janela saudável.
- Retoma em BUFFERING: agendar supervisão de stall.
- Abertura inicial: manter o seu prazo.
- Não criar outra rotina de reconnect nem aumentar todos os timeouts.
- Testes de aceitação: READY→pausa→READY sem novo callback de estado; BUFFERING→pausa→BUFFERING; pausa longa; owner substituído; janela saudável real antes de repor orçamento.

#### BUG-P2-02 — Gravações usam preferência errada de leitor externo

**Descrição:** `SettingsRepository.kt:1551–1552` envia `LIVE` para `externalPlayerMovies`. `RecordingsViewModel.kt:164,256–257` usa `externalPlayerFor(MediaType.LIVE)`. A opção visível de TV Direto consulta outro valor. Ativar o leitor externo de TV pode não aplicar-se às gravações; um valor antigo de Filmes pode abri-las externamente com a opção TV desligada.

**Localização:** `SettingsRepository.kt:1551–1552` (Core), `RecordingsViewModel.kt:164,256–257` (app)

**Solução:**
- Usar a preferência Live nos dois pontos de gravações, ou tornar a matriz de `MediaType` explícita, preservando downloads VOD.
- Testar combinações Live/Movies e manter proteções de gravação em curso ou em partes.
- Não é correção de codec/áudio.

#### BUG-P2-03 — Fábrica HTTP da pré-visualização retém geração antiga

**Descrição:** `HeroPreviewEngine.kt:301` retira a geração HTTP a cada play. O player só é reconstruído se UA/headers/decodificação mudarem. A construção fixa uma fábrica de chamadas na geração atual. Reutilizar a mesma fábrica na próxima preview cancela os pedidos dessa preview.

**Localização:** `HeroPreviewEngine.kt:301, 320–334` (Core), `PlaybackHttpCalls.factory:34–59` (Core)

**Solução:**
- Renovar a fábrica/media source por geração, preservando o fencing de pedidos antigos.
- Não retirar a proteção nem reconstruir globalmente todos os leitores.
- Testes: duas previews sequenciais com identidade igual; stop→play.

#### BUG-P2-04 — Pesquisa do guia não limpa ao sair e voltar

**Descrição:** `EpgViewModel.kt:583–585` conserva `_query`; `setGuideActive(false):956–968` cancela trabalho mas não reinicia a pesquisa. Com Activity/ViewModel vivos, Home e reabrir conserva a pesquisa do guia.

**Localização:** `EpgViewModel.kt:583–585, 956–968` (app)

**Solução:**
- Separar background real da app de simples saída do guia ou abertura fullscreen.
- Limpar o filtro apenas no evento correspondente à regra pedida.
- Testar: Home/reabrir, processo morto, mudança de secção, retorno do diferido sem limpar indevidamente navegação.

#### BUG-P2-05 — Pesquisa EPG ignora nomes personalizados

**Descrição:** `EpgViewModel.kt:1021–1022` pesquisa `channelsWithGuide(ids,q,...)` antes de aplicar `cust.itemNames` (`:1057`). A pesquisa SQL usa o nome original. Um canal apresentado com outro nome pode não aparecer ao procurar pelo nome mostrado.

**Localização:** `EpgViewModel.kt:1021–1022, 1057` (app)

**Solução:**
- Incluir candidatos que correspondam a nomes personalizados com elegibilidade e limites.
- Deduplicar e só depois ordenar. Não carregar todas as listas em RAM para resolver uma pesquisa.
- Testar: nome original/personalizado, categoria oculta, fonte inativa, limite de resultados.

#### BUG-P2-06 — Callbacks de edição EPG sem owner estável

**Descrição:** `setEpgShift:705–717` e `setEpgMatch:742–747` obtêm o perfil ativo na execução e aplicam um `ChannelEntity` recebido anteriormente, sem o contrato explícito de `setEpgFallback:733–738`. Se o diálogo sobreviver a uma troca de perfil/fontes, pode escrever a escolha no contexto novo.

**Localização:** `EpgViewModel.kt:705–717, 733–738, 742–747` (app)

**Solução:**
- Snapshot de perfil/fontes/revisão ao abrir a edição; revalidar antes de gravar.
- Recusar operação antiga, não reaplicá-la ao perfil atual.
- Testar: troca de perfil/background/restauro enquanto operação está pendente.

#### BUG-P2-07 — Perfis: edição/remoção de PIN sem autenticação

**Descrição:** `ManageProfilesScreen.kt` permite remover/alterar PIN de outro perfil sem autenticar PIN anterior. Não existe matriz alvo/ação/autorização aplicada na operação. Risco se houver perfis infantis.

**Localização:** `ManageProfilesScreen.kt:154–166`, `ProfileComponents.kt:187–209`, `ProfilesViewModel.kt:69–70`, `ProfileGate.kt` (app)

**Solução:**
- Definir política de administração: qualquer sessão autenticada administra perfis? Só adult/admin? Exigir PIN atual do alvo para reduzir proteção?
- Usar grant efémero vinculado ao alvo/ação, não booleano global.
- Testar: perfilA autenticado editando perfilB bloqueado, cancelamento e alteração do hash durante a operação.
- Não criar função admin nem migrar hashes sem necessidade.

### 3.3 Guia, Navegação e Apresentação (P2/P3)

#### BUG-P3-01 — Guia recalcula intervalos na navegação (otimização sem ganho medido)

**Descrição:** `GuideCore.kt:122` memoriza layout para desenho; `GuideProgrammeCells.at` e `adjacentTime` voltam a executar layout, que ordena/sweep dos eventos. São chamados em EpgScreen na navegação e na faixa de informação. A instância calculada não é partilhada.

**Localização:** `GuideCore.kt:122`, `GuideProgrammeCells.kt` (app)

**Solução:**
- Modelo imutável da linha com os mesmos intervalos para desenho, seleção, informação e navegação, invalidado por dados/janela/identidade.
- Procurar a célula por índice/busca sem nova ordenação a cada tecla.
- Preservar entidade e início original para diferidos.
- Executar medições E1/E2 antes/depois; não prometer scrolling 2x nem que elimina falhas de rede.

#### BUG-P3-02 — Aviso de alarmes exatos desatualizado

**Descrição:** `RecordingsViewModel.kt:69–74` guarda uma capacidade calculada uma vez; `RecordingSettingsScreen.kt:131–134` mostra esse valor. Conceder permissão e voltar ao mesmo ViewModel pode manter aviso antigo.

**Localização:** `RecordingsViewModel.kt:69–74`, `RecordingSettingsScreen.kt:131–134` (app)

**Solução:**
- Atualizar ao retomar o ecrã ou observar capacidade atual, sem polling permanente.
- Testar: denied→granted no mesmo processo.

#### BUG-P3-03 — Foco em overlays do guia não refoca

**Descrição:** `ChannelListOverlay.kt` depende apenas de `currentId`, não da chegada/mudança de canais; `requestFocus` testa `isFailure` em vez de retorno Boolean. Dados vazios→carregados com `currentId` estável podem não refocar.

**Localização:** `ChannelListOverlay.kt:77–92` (app)

**Solução:**
- Garantir refoco quando dados vazios→carregados com `currentId` estável.
- `requestFocus` deve usar o retorno Boolean, não apenas `isFailure`.
- Alvo estável e requestFocus Boolean; não tratar focusRestorer como solução única.

#### BUG-P3-04 — CategoryRail usa índice original como key

**Descrição:** Linha 299 usa índice original como key; reorder/filtrar mantendo tamanho pode associar posição a categoria diferente.

**Localização:** `CategoryRail.kt` (app)

**Solução:**
- Usar chave estável real de `RailCategory`, com fallback quando a categoria desaparecer.
- Não presumir `category.id` para uma categoria virtual.
- Alteração mínima é chave estável real.

### 3.4 Formato de Data/Hora (P2)

#### BUG-P4-01 — Padrão de data/hora redundante em TimeFormat.kt

**Descrição:** `combinedDateTimeSkeleton` acrescenta `Hm`/`hm` diretamente ao argumento recebido. `RecordingsScreen.kt` chama-o com `EEEdMMMjm`, que já inclui hora. Pode provocar exceção de formatação com padrões redundantes.

**Localização:** `TimeFormat.kt:73–74`, `RecordingsScreen.kt:795`

**Solução:**
- Normalizar os formatos de data/hora antes de combinar campos (remover campos de hora existentes antes de acrescentar o formato final).
- Validar formatos 12/24h, chamadas com e sem hora, datas de gravações, mudança de idioma.

### 3.5 Leitor Externo (P2)

#### BUG-P5-01 — ExternalPlayerLauncher usa Intent.createChooser

**Descrição:** Ainda usa `Intent.createChooser` quando existem vários leitores e lê o idioma de forma bloqueante antes de qualquer lançamento.

**Localização:** `ExternalPlayerLauncher.kt` (Core)

**Solução:**
- Migrar para o resolvedor normal do Android, permitindo escolha persistente quando o sistema a disponibiliza.
- Ler o contexto localizado apenas para mostrar erros, não bloquear antes do lançamento.
- Manter headers, candidatos MIME e protocolo local de regresso à lista.

---

## 4. Melhorias de Código, Arquitetura e UX — Com Soluções

### 4.1 Qualidade de Código e Manutenibilidade

#### MEL-01 — Centralizar lógica de foco em componentes reutilizáveis

**Descrição:** Lógica de foco dispersa em múltiplos ecrãs (`FocusRequestGuard`, `requestBoundedFocus`, `cancelPendingFocusOnInput`).

**Solução:**
- Extrair componentes de foco reutilizáveis para evitar dispersão e inconsistência de navegação D-pad em TV.
- Criar um único ponto de entrada para requestFocus com tratamento de owner e revisão.

#### MEL-02 — Estados vazios (empty states) mais descritivos

**Descrição:** Guias e listas podem ficar vazios sem explicação clara ao utilizador.

**Solução:**
- Adicionar empty states descritivos no guia quando não há EPG nem canais, e em outras listas vazias.
- Incluir ação direcionada (ex: "Adicionar playlist", "Sincronizar EPG").

#### MEL-03 — Mensagens de erro de rede diferenciadas

**Descrição:** Erros de rede não distinguem "sem internet" de "fornecedor indisponível".

**Solução:**
- Melhorar mensagens de erro para distinguir conectividade do dispositivo vs indisponibilidade do fornecedor.
- Ajudar o utilizador a saber se o problema é dele ou da fonte.

#### MEL-04 — Revisão de chunked() em consultas SQLite

**Descrição:** Uso de `chunked(400)` em consultas pode alterar `ORDER BY`/`LIMIT` se não preservado.

**Solução:**
- Revisar uso de `chunked()` para garantir preservação de `ORDER BY`/`LIMIT` e paginação correta.
- Não aplicar genericamente em todas as consultas sem verificar o driver.

### 4.2 Segurança e Perfis

#### MEL-05 — Confirmação explícita para alterar PIN/remover proteção

**Descrição:** Não há confirmação explícita antes de alterar PIN ou remover proteção de perfil.

**Solução:**
- Adicionar confirmação explícita antes de alterar PIN ou remover proteção de perfil.
- Reforçar segurança, especialmente com perfis infantis.

### 4.3 CI / Build / Snapshots

#### MEL-06 — Snapshot transacional

**Descrição:** Script de snapshot escreve ZIP final antes de validar versão; não é transacional.

**Localização:** `tools/core_snapshot/snapshot_core.py:41–53`

**Solução:**
- Validar antes de escrever ZIP; publicar ZIP imutável por hash; substituir lock atomicamente.
- Dois renames independentes não tornam o par transacional.
- Testar versão inválida e falhas entre fases.

#### MEL-07 — CI executar testes Core explicitamente

**Descrição:** CI App não executa as suites de teste dos módulos Core personalizados.

**Localização:** `android.yml:132–135`

**Solução:**
- Adicionar `:OwnTV_Core:core:testDebugUnitTest` e `:OwnTV_Core:player-core:testDebugUnitTest` ao `android.yml`.
- Teste intencionalmente falhado no snapshot deve bloquear aprovação.

#### MEL-08 — Desativar/converter bump automático

**Descrição:** Atualização automática por pin não acompanha o Core por snapshot; gates podem aprovar sem integrar código.

**Localização:** `gradle/custom-core.settings.gradle.kts`, `android.yml`, `automerge-core-bump.yml`

**Solução:**
- Converter o bump automático em proposta de revisão; gates devem validar inputs reais.
- Remover apenas resolução/inputs/comentários comprovadamente obsoletos.

#### MEL-09 — Fixture de desempenho compatível com defaults

**Descrição:** Fixture de desempenho incompatível com default "Apenas diferidos".

**Localização:** `tools/performance/local_iptv_fixture.py:35–36`

**Solução:**
- Preparação explícita com restauro da preferência e erro claro, ou instrução manual inequívoca.
- Não inventar suporte de arquivo na fixture.

---

## 5. Inovações e Novas Funcionalidades — Com Soluções/Descrição

A serem avaliadas **após** as correções acima. Ordenadas por impacto percebido.

### 5.1 Inovações de Produto

#### INN-01 — Pré-visualização de restauro de backup

**Descrição:** Antes de escrever, mostrar quais perfis/listas foram identificados e quantas configurações serão aplicadas ou ignoradas.

**Solução/Implementação:**
- No ecrã de restauração, exibir resumo: perfis encontrados, listas encontradas, configurações que serão aplicadas, configurações que serão ignoradas (sem correspondência).
- Não exigir ao utilizador conhecer IDs.
- Complementa BUG-P1-04.

#### INN-02 — Validação de exportação pelo contrato de importação

**Descrição:** Detectar imediatamente uma preferência que produziria um backup recusado (ex: opacidade -1).

**Solução/Implementação:**
- No momento da exportação, executar o mesmo contrato de validação usado na importação.
- Mostrar aviso imediato se alguma preferência seria recusada.
- Não esconder o erro nem remover preferências silenciosamente.

#### INN-03 — Sessão de diagnóstico com duração definida

**Descrição:** Iniciar, terminar e exportar um ensaio curto com janela e percurso identificados.

**Solução/Implementação:**
- Adicionar opção em Definições → Aplicação → Avançado para iniciar diagnóstico curto.
- Reutilizar infraestrutura E1/E2 existente.
- Apenas opcional, para facilitar recolha e evitar diagnóstico ativo por esquecimento.
- Sem novas heurísticas de qualidade de streaming.

#### INN-04 — Modo apenas áudio para canais de rádio/IPTV áudio

**Descrição:** Reproduzir apenas a faixa áudio, ecrã escuro ou visualização mínima.

**Solução/Implementação:**
- Adicionar modo "Áudio apenas" no player que desativa renderização de vídeo e mantém apenas áudio.
- Ecrã escuro ou visualização mínima (logotipo, título).
- Economia de banda/GPU; acessibilidade.

#### INN-05 — Controlo parental por categoria com horário

**Descrição:** Bloquear categorias/canais adultos em determinadas horas, exceção por PIN.

**Solução/Implementação:**
- Adicionar regras de horário por categoria (ex: bloquear adultos após 22h).
- Exigir PIN para desbloquear fora do horário permitido.
- Integrar com o sistema existente de perfis e Kids mode.

#### INN-06 — Sincronização de histórico com Trakt.tv / Plex

**Descrição:** Sincronizar "Continue Watching" e favoritos com serviços externos.

**Solução/Implementação:**
- Adicionar opção de conta externa nas definições de perfil.
- Sincronizar estados de visualização e favoritos via API do serviço escolhido.
- Fora do escopo atual da TV, mas viável para expansão.

### 5.2 Inovações de UX / Interface

#### UX-01 — Modo escuro automático por hora do dia

**Descrição:** Alternar tema claro/escuro com base no fuso local e hora.

**Solução/Implementação:**
- Adicionar definição "Tema automático" que alterna com base na hora local (ex: escuro após pôr do sol).
- Manter opção manual de claro/escuro/sistema.

#### UX-02 — Atalhos numéricos rápidos no ecrã de canais

**Descrição:** Digitar número do canal para zapping direto sem abrir overlay.

**Solução/Implementação:**
- Capturar pressionamento de teclas numéricas no ecrã de canais ao vivo.
- Após timeout curto ou tecla OK, sintonizar o canal correspondente.
- Reutilizar resolução/owner existente do DirectTune.

#### UX-03 — Personalização do layout do ecrã inicial

**Descrição:** Reordenar linhas do Home via D-pad longo.

**Solução/Implementação:**
- Adicionar modo de edição do Home (pressionar longo no botão de opções).
- Permitir arrastar/soltar (via D-pad) para reordenar linhas.
- Guardar ordem por perfil.

#### UX-04 — Modo de economia de energia

**Descrição:** Desativar pré-visualizações de vídeo no guia em modo eco.

**Solução/Implementação:**
- Adicionar definição "Economia de energia" que desativa pré-visualizações de vídeo no guia.
- Reduzir brilho ou desativar animações não essenciais.

#### UX-05 — Notificações de gravação

**Descrição:** Notificar quando uma gravação termina ou falha.

**Solução/Implementação:**
- Adicionar notificações locais para estados de gravação (concluída, falhada, sem espaço).
- Integrar com o sistema existente de gravações.

### 5.3 Inovações Técnicas / Avançadas

#### TEC-01 — Upscaling por hardware (beta, opcional)

**Descrição:** Já registado como `UPSCALING-BETA-01` em `MELHORIAS_FUTURAS_PENDENTES.md`.

**Solução/Implementação:**
- Avaliar APIs/GPU/boxes antes de implementar.
- **Não aumentar buffer como sinónimo de melhorar resolução.** Aumentar buffer não cria detalhe na imagem.
- Desativada por defeito, ativação explícita, possibilidade de voltar ao percurso normal.
- Comparação visual/desempenho nas boxes reais, sem degradar canais que já funcionam bem.

#### TEC-02 — Previsão de largura de banda e seleção automática de qualidade

**Descrição:** Medir taxa de bits média do stream e sugerir/selecionar qualidade automaticamente.

**Solução/Implementação:**
- Medir taxa de bits média durante reprodução.
- Sugerir qualidade (SD/HD/4K) com base na largura de banda disponível.
- Opção de seleção automática ou manual.

#### TEC-03 — Reconexão inteligente com backoff exponencial

**Descrição:** Quando o fornecedor cai, tentar reconexão com backoff crescente.

**Solução/Implementação:**
- Implementar backoff exponencial para reconexão de streams.
- Notificar utilizador após X tentativas falhadas.
- Distinguir falha temporária de fornecedor de erro permanente.

#### TEC-04 — Multiview de perfis

**Descrição:** Cada tile do multiview pode usar um perfil/fonte diferente.

**Solução/Implementação:**
- Permitir selecionar perfil diferente por tile no multiview.
- Gerir orçamento de conexões por fonte.

#### TEC-05 — Integração com assistente de voz

**Descrição:** Comandos de voz para mudar canal, gravar, pesquisar.

**Solução/Implementação:**
- Integrar com Android TV Voice Interaction API.
- Mapear comandos de voz para ações existentes (mudar canal, gravar, pesquisar).

---

## 6. Reavaliação do Conselho — Validação de Todos os Itens

O Council reavaliou todos os itens acima. Cada item foi confirmado como **válido** (manter com solução) ou **rejeitado** (não implementar na forma proposta).

### 6.1 Itens Confirmados como Válidos (com solução)

Todos os bugs P1 (BUG-P1-01 a P1-04), P2 (BUG-P2-01 a P2-07), P3 (BUG-P3-01 a P3-04), P4 (BUG-P4-01) e P5 (BUG-P5-01) são **confirmados como válidos** e mantidos com as soluções descritas nas secções 3 e 4.

Todas as melhorias MEL-01 a MEL-09 são **confirmadas como válidas** e mantidas com as soluções descritas na secção 4.

Todas as inovações INN-01 a INN-06, UX-01 a UX-05 e TEC-01 a TEC-05 são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização, com as soluções/descrições na secção 5.

### 6.2 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpHttp apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 7. Plano de Implementação por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)

| Item | Solução | Critério de Validação |
|------|---------|----------------------|
| BUG-P1-01 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Roundtrip backup válido; -2/101 recusados |
| BUG-P1-02 | Unificar redacção de segredos em HttpClient | Logs sintéticos sem segredos |
| BUG-P1-03 | Aplicar política de elegibilidade após applyEpgMatches | Categorias ocultas respeitadas no guia |
| BUG-P1-04 | Usar apenas associações resolvidas no restauro seletivo | Restauro sem cruzar identidades |

### Fase 2 — Reprodução e Preferências (P2)

| Item | Solução | Critério de Validação |
|------|---------|----------------------|
| BUG-P2-01 | Centralizar rearmar de watchdogs por estado/owner | READY→pausa→READY saudável |
| BUG-P2-02 | Usar preferência Live nas gravações | Gravações seguem opção visível |
| BUG-P2-03 | Renovar fábrica HTTP por geração no HeroPreview | Previews sequenciais abrem |
| BUG-P2-04 | Limpar pesquisa do guia só no background real | Pesquisa não persiste indevidamente |
| BUG-P2-05 | Incluir nomes personalizados na pesquisa EPG | Nomes personalizados pesquisáveis |
| BUG-P2-06 | Snapshot de perfil/fontes na edição EPG | Callbacks antigos recusados |
| BUG-P2-07 | Matriz de autorização de perfis (PIN) | Edição de outro perfil requer PIN |
| BUG-P4-01 | Normalizar formatos de data/hora em TimeFormat | Sem exceções de formatação |
| BUG-P5-01 | Resolvedor normal do Android para leitor externo | Escolha persistente funcionando |

### Fase 3 — Coerência da Interface (P2/P3)

| Item | Solução | Critério de Validação |
|------|---------|----------------------|
| BUG-P3-01 | Modelo imutável de intervalos do guia | Sem recálculo desnecessário |
| BUG-P3-02 | Atualizar aviso de alarmes ao retomar ecrã | Aviso atual após granted |
| BUG-P3-03 | Foco estável em overlays (Boolean return) | Refoco em dados carregados |
| BUG-P3-04 | Chave estável em CategoryRail | Sem associação posição→categoria errada |

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)

| Item | Solução | Critério de Validação |
|------|---------|----------------------|
| A18 | Transportar início efetivo do arquivo | Início 00/30/59s, correção +1h |
| A19 | Fuso automático Xtream (opcional) | Zona usada, correção aplicada |
| A20 | Convenções M3U catchup-correction/timeshift | Templates manuais preservados |
| A21 | Identidade CATCHUP estável + renovação Stalker | Preferências motor independem de URL com token |

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)

| Item | Solução | Critério de Validação |
|------|---------|----------------------|
| A22 | Monitor pressão memória Android 14+ | Callback sem reiniciar canal saudável |
| A23 | Timeshift recuperável após indisponibilidade | Distinguir rede vs recusa fornecedor |
| A24 | Eliminar 500 ms em zapTo (pressão isolada) | Zapping mais rápido sem enfraquecer garantias |
| MEL-06 | Snapshot transacional (validar antes de escrever) | ZIP imutável por hash, lock atómico |
| MEL-07 | CI executar testes Core explicitamente | Suites Core executadas no android.yml |
| MEL-08 | Converter bump automático em proposta de revisão | Gates validam inputs reais |
| MEL-09 | Fixture de desempenho compatível com defaults | Preparação explícita sem alterar defaults |

### Fase 6 — Inovações Selecionadas (após estabilização)

| Item | Solução/Descrição |
|------|-------------------|
| INN-01 | Pré-visualização de restauro de backup |
| INN-02 | Validação de exportação pelo contrato de importação |
| INN-03 | Sessão de diagnóstico com duração definida |
| TEC-01 | Avaliar viabilidade de upscaling por hardware (apenas se ganho real medido) |

---

## 7. Segunda Ronda de Análise — Novos Bugs, Melhorias e Inovações (19/10/2026)

Na segunda ronda, os mesmos quatro agentes especializados reinspecionaram áreas adicionais do código-fonte que não haviam sido cobertas na primeira análise: Shell (OwnTVShell), Home, Search, Downloads, Movies, Series, Multiview, Settings UI e componentes de foco/D-pad. O Council consolidou os novos achados.

### 7.1 Novos Bugs Encontrados (com soluções)

#### BUG-ADD-01 — Startup channel pode lançar num perfil errado após troca de perfil

**Descrição:** Em `OwnTVShell.kt:583–584`, o `pid` usado para `startupMode` e `startupChannel` é obtido de `resumeSettings.activeProfileId.first()`, mas depois há um `profileDao.getById(rawPid)?.id` que pode retornar um ID diferente se o perfil foi removido ou trocado entre a leitura e a utilização. Além disso, `startupChannel(rawPid)` usa o `rawPid` original em vez do `pid` resolvido.

**Localização:** `OwnTVShell.kt:578–611`

**Solução:**
- Resolver o `pid` de forma consistente antes de usá-lo tanto em `startupMode` quanto em `startupChannel`.
- Usar sempre o `pid` resolvido (não o `rawPid`) nas chamadas a `startupChannel`.
- Adicionar validação de que o perfil ainda existe e está ativo antes de aplicar a ação de arranque.
- Testar: troca de perfil durante o ecrã de arranque, perfil removido, perfil Kids.

#### BUG-ADD-02 — Home ViewModel pode carregar dados duplicados no cold start

**Descrição:** `HomeViewModel.kt:149–175` tem dois gatilhos que podem disparar `loadHomeData` em paralelo no cold start (o `reloadRequests` e o observador do `trendingDao`). Apesar do comentário dizer que há coalescência, no cold start os dois triggers chegam centenas de milissegundos afastados, o que não pode ser mesclado sem segurar o primeiro. O comentário admite que isto foi "o single largest delay on the screen".

**Localização:** `HomeViewModel.kt:149–175`

**Solução:**
- Manter o `drop(1)` para descartar a emissão inicial do Room.
- Adicionar um `Mutex` ou flag de carga em curso para evitar cargas concorrentes de `loadHomeData`.
- Testar: cold start, refresh em background durante navegação, múltiplas invalidations do Room em sequência rápida.

#### BUG-ADD-03 — Search ViewModel usa `externalPlayerMovies` em vez de `externalPlayerFor`

**Descrição:** `SearchViewModel.kt:68–69` expõe `externalPlayerOn` baseado em `settings.externalPlayerMovies`, mas a pesquisa abrange canais, filmes e séries. Para canais LIVE, o correto seria consultar `externalPlayerLive` ou o `externalPlayerFor(MediaType)` específico do item encontrado.

**Localização:** `SearchViewModel.kt:68–69`

**Solução:**
- Substituir por um mapeamento por `MediaType` ou expor um `externalPlayerFor(MediaType)` reativo.
- Testar: pesquisa de canais com leitor externo de TV ativo, pesquisa de filmes com leitor externo de filmes ativo.

#### BUG-ADD-04 — Downloads: `externalPlayerOn` mistura filmes e séries sem distinção por item

**Descrição:** `DownloadsViewModel.kt:123–125` combina `externalPlayerMovies` e `externalPlayerSeries` com OR. Embora o comentário diga que `play()` re-verifica por secção, o estado exposto ao ecrã pode ser enganoso (mostra "externo" quando apenas um tipo está configurado).

**Localização:** `DownloadsViewModel.kt:123–125`

**Solução:**
- Expor dois estados separados (um para filmes, outro para séries) ou calcular por item no momento do `play()`.
- Melhorar a clareza do estado exposto ao UI.
- Testar: apenas filmes externos, apenas séries externas, ambos, nenhum.

#### BUG-ADD-05 — Shell: `restoreFocus` pode ficar preso a true sem reset

**Descrição:** `OwnTVShell.kt:190` declara `var restoreFocus by remember { mutableStateOf(false) }`. É definido como true em vários locais (ex: `:710`), mas não há caminho claro de reset para false após ser consumido, o que pode causar foco inesperado em navegações subsequentes.

**Localização:** `OwnTVShell.kt:190, 710`

**Solução:**
- Resetar `restoreFocus = false` imediatamente após consumi-lo no `LiveScreen` ou noutro consumidor.
- Garantir que todos os caminhos que definem `true` também tenham um caminho de reset.
- Testar: entrar/sair do player múltiplas vezes, navegar entre secções após sair do player.

#### BUG-ADD-06 — Shell: `zapSource` por `rememberSaveable` pode sobreviver a mudanças de secção inesperadas

**Descrição:** `OwnTVShell.kt:366` usa `rememberSaveable` para `zapSource`, o que significa que sobrevive a recomposições e até mudanças de configuração. Se o utilizador mudar de secção enquanto um zap está pendente, a fonte pode estar desatualizada.

**Localização:** `OwnTVShell.kt:366, 593, 637`

**Solução:**
- Invalidar `zapSource` quando a secção selecionada mudar (não apenas quando o zap for executado).
- Considerar usar `remember` em vez de `rememberSaveable` se a persistência entre mortes de processo não for necessária para este estado transitório.
- Testar: mudar de secção durante um zap pendente, rotação de ecrã (se suportada), morte de processo durante zap.

#### BUG-ADD-07 — Home: `continueTarget` só considera LIVE, ignora filmes/séries

**Descrição:** `HomeViewModel.kt:191` filtra `it.mediaType == MediaType.LIVE` no `resolveContinue`, ignorando históricos de filmes e séries para o chip "Continue" da top-bar.

**Localização:** `HomeViewModel.kt:188–193`

**Solução:**
- Estender `resolveContinue` para considerar MOVIE e SERIES também, com prioridade definida (ex: o mais recente independentemente do tipo).
- Testar: histórico misto de canais, filmes e séries; chip mostra o item mais recente correto.

#### BUG-ADD-08 — Multiview: `releaseAll` não espera confirmação de libertação de engines

**Descrição:** `OwnTVShell.kt:724–732` chama `grid.releaseAll()` mas não aguarda confirmação de que as engines foram realmente libertadas antes de definir `multiview = null`. Se uma nova grelha for aberta imediatamente, pode haver conflito de recursos.

**Localização:** `OwnTVShell.kt:724–732`, `MultiviewState.kt` (método `releaseAll`)

**Solução:**
- Tornar `releaseAll` suspenso e aguardar confirmação de libertação de todas as engines antes de retornar.
- Ou adicionar um estado `releasing` que bloqueie a abertura de uma nova grelha até completar.
- Testar: abrir/fechar multiview rapidamente em sequência.

### 7.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-ADD-01 — Shell: focar automaticamente na secção correta após sair do player

**Descrição:** Após `exitPlayer` (`OwnTVShell.kt:695–723`), o foco retorna para sidebar ou consoante `sidebarHidden`, mas não considera de onde o utilizador veio (ex: se veio do Guide, deveria voltar para o Guide).

**Solução:**
- Rastrear a secção de origem antes de entrar no player e restaurar o foco nessa secção ao sair.
- Melhorar a experiência de "Volta" após assistir conteúdo.

#### MEL-ADD-02 — Home: esqueleto (skeleton) só no cold start, não em refreshes

**Descrição:** O comentário em `HomeViewModel.kt:86–90` já menciona isto, mas vale reforçar: o esqueleto não deve reaparecer em refreshes subsequentes, apenas no cold start.

**Solução:**
- Manter a flag `isLoading` apenas para o primeiro carregamento; refreshes devem mostrar dados existentes com indicador de atualização silenciosa.
- Testar: cold start mostra esqueleto; reabrir Home mostra dados em cache enquanto atualiza.

#### MEL-ADD-03 — Search: debounce de query ajustável por tipo de conteúdo

**Descrição:** `SearchViewModel.kt:84` usa debounce fixo de 300ms para queries com 2+ caracteres. Para canais ao vivo (muitos resultados), um debounce mais curto pode ser desejável; para filmes/séries, mais longo evita sobrecarga.

**Solução:**
- Ajustar o debounce com base no contexto de pesquisa ou permitir configuração.
- Testar: digitação rápida em pesquisa de canais vs filmes.

#### MEL-ADD-04 — Downloads: indicador de progresso por download

**Descrição:** O `DownloadsViewModel` lista downloads mas não expõe progresso individual por item para o UI mostrar barra de progresso.

**Solução:**
- Expor `StateFlow<DownloadProgress>` por download ID, observando o `DownloadManager`.
- Testar: barra de progresso atualiza durante download; pausa/retoma reflete corretamente.

### 7.3 Novas Inovações (com descrição/solução)

#### INN-ADD-01 — Modo "Convidado" sem perfil

**Descrição:** Permitir usar a app sem criar perfil, com funcionalidades limitadas (apenas Live TV, sem gravações/favoritos).

**Solução/Implementação:**
- Adicionar modo "Convidado" no ecrã de seleção de perfil.
- Desativar gravações, favoritos, histórico e backup neste modo.
- Sessão temporária, limpa ao sair da app.

#### INN-ADD-02 — Sugestão automática de canais baseado no histórico de visualização

**Descrição:** Sugerir canais que o utilizador costuma ver nesta hora do dia.

**Solução/Implementação:**
- Analisar `WatchHistory` para padrões de hora/canal.
- Mostrar "Canais para si" no Home com base na hora atual.
- Respeitar perfis e Kids mode.

#### INN-ADD-03 — Modo Picture-in-Picture (PiP) melhorado para Android TV

**Descrição:** Melhorar o suporte a PiP para permitir continuar a ver conteúdo enquanto navega em outras secções.

**Solução/Implementação:**
- Adicionar botão de PiP no player (quando suportado pelo dispositivo).
- Manter áudio e vídeo em janela pequena enquanto o utilizador navega no guia ou settings.
- Sincronizar posição entre PiP e ecrã cheio.

#### INN-ADD-04 — Integração com Android TV "Watch Next" (canal do sistema)

**Descrição:** Publicar conteúdos no "Watch Next" do Android TV para continuar a ver a partir do ecrã inicial do sistema.

**Solução/Implementação:**
- Usar `TvProvider` para escrever cartões "Watch Next" com conteúdo recente do utilizador.
- Permitir iniciar reprodução diretamente do ecrã inicial do Android TV.
- Respeitar perfis e conteúdo oculto.

#### INN-ADD-05 — Controlo por gestos no comando remoto (área de toque)

**Descrição:** Alguns comandos remotos de Android TV têm área de toque. Adicionar suporte a gestos de deslizar para navegação rápida.

**Solução/Implementação:**
- Detectar eventos de toque no comando remoto (`KeyEvent` com source de touch).
- Mapear deslizar horizontal para mudar canal, vertical para volume.
- Desativável nas definições.


---

## 8. Terceira Ronda de Análise — Novos Bugs, Melhorias e Inovações (20/10/2026)

Na terceira ronda, os mesmos quatro agentes especializados reinspecionaram áreas adicionais do código-fonte: Player (FailedChannelAttempts, DirectTune, LiveRestart, FrameRateController, AudioOnlyBadge), UI Components (LongPressMenuGuard, ChannelPressGesture, ChannelPressInput, FocusTrap, GridFocus), Update (UpdateDialog, UpdateFailureText), Customize (CustomizeViewModel, BulkRenameDialogs), Setup (SetupViewModel, AddSourceScreen) e More (UserDataScreen, MoreScreen). O Council consolidou os novos achados.

### 8.1 Novos Bugs Encontrados (com soluções)

#### BUG-TER-01 — FailedChannelAttempts regista códigos HTTP errados em concorrência

**Descrição:** `FailedChannelAttempts.kt:26–28` usa `observedOwner` e `observedCode` partilhados entre múltiplas chamadas de `record()` sem isolamento por owner. Se dois canais diferentes forem tentados em paralelo, o `observedCode` de um pode "vazar" para o registo do outro, resultando em diagnósticos de erro HTTP incorretos.

**Localização:** `FailedChannelAttempts.kt:14–28`

**Solução:**
- Substituir o `observedCode` global por um mapa por owner (`Map<Long, Int?>`), ou eliminar completamente a partilha de estado entre owners.
- Cada chamada de `record()` deve usar apenas o `httpCode` extraído do próprio `error` passado, sem recorrer a estado observado de chamadas anteriores.
- Testar: gravação de tentativas falhadas para múltiplos canais em sequência rápida; verificação de que cada registo tem o código HTTP correto.

#### BUG-TER-02 — DirectTune: resultado Ambiguous não dá feedback ao utilizador

**Descrição:** `DirectTune.kt:5–16` define `DirectTuneResult.Ambiguous` mas não há evidência de que o HUD (`PlayerHud`) apresente um ecrã de escolha ao utilizador quando há múltiplos candidatos. O utilizador fica sem saber por que o número não sintonizou.

**Localização:** `DirectTune.kt:5–16`, `PlayerHudControls.kt` (pesquisar consumo de DirectTuneResult)

**Solução:**
- Adicionar apresentação de ecrã de seleção no HUD quando o resultado é `Ambiguous`, listando os candidatos com nomes e logótipos.
- Adicionar resultado `NotFound` com sugestão de verificar o número do canal.
- Testar: número com múltiplos canais (mostrar lista de escolha); número inexistente (mostrar "não encontrado").

#### BUG-TER-03 — LongPressMenuGuard pode ficar desarmado para sempre

**Descrição:** `LongPressMenuGuard.kt:39–44` usa um loop `while (!armed)` com `delay(POLL_MS)` que nunca termina se `armed` nunca for definido como `true`. Se nenhum evento de tecla chegar (ex: diálogo aberto por toque em vez de OK), o loop continua indefinidamente, consumindo um coroutine.

**Localização:** `LongPressMenuGuard.kt:39–44`

**Solução:**
- Adicionar um tempo máximo de espera (ex: 5 segundos) após o qual o guard se armar automaticamente.
- Cancelar o `LaunchedEffect` quando o diálogo for dismissado (usar `DisposableEffect` ou `onDispose`).
- Testar: diálogo aberto sem tecla OK pressionada; diálogo fechado rapidamente; múltiplas aberturas/fechamentos.

#### BUG-TER-04 — ChannelPressGesture: release após menuDeadline entrega PLAY errado

**Descrição:** `ChannelPressGesture.kt:23–33` — se `menuDeadline()` foi chamado (entregou MENU), `release()` retorna `null` (linha 27), mas se o utilizador carregar longo e depois soltar antes do MENU_MS, a ação PLAY pode ser entregue indevidamente após um menu já ter sido mostrado.

**Localização:** `ChannelPressGesture.kt:23–33`

**Solução:**
- Adicionar flag `menuShown` que, quando true, impeça `release()` de retornar PLAY ou VERSIONS.
- Testar: carregar longo > 3s (menu mostrado), depois soltar — nenhuma ação de play; carregar curto, soltar — PLAY.

#### BUG-TER-05 — UpdateDialog: focus request repete 3 vezes desnecessariamente

**Descrição:** `UpdateDialog.kt:71–78` faz `repeat(3)` com `delay(60)` para tentar pedir foco. Isto atrasa a apresentação do diálogo em até 180ms e consome ciclos de CPU desnecessariamente se o foco já estiver disponível.

**Localização:** `UpdateDialog.kt:71–78`

**Solução:**
- Substituir o `repeat(3)` por uma única tentativa de `requestFocus()`, ou usar `focusRequester` diretamente no `Modifier.focusRequester()` do botão alvo.
- Remover o loop de retry de foco.
- Testar: abertura do diálogo de atualização; foco no botão correto imediatamente.

#### BUG-TER-06 — CustomizeViewModel: moveMutex serializa mas não protege contra stale reads

**Descrição:** `CustomizeViewModel.kt:78` usa `moveMutex` para serializar movimentos, mas o comentário diz "never the stale pre-move order". Contudo, não há evidência de que leituras concorrentes de `contentOrderDao` sejam protegidas contra estados intermediários.

**Localização:** `CustomizeViewModel.kt:78–82`, `CustomizeViewModel.kt` (métodos de move)

**Solução:**
- Garantir que todas as leituras de `contentOrderDao` dentro de uma operação de move sejam feitas dentro do `withLock(moveMutex)`.
- Adicionar comentário claro sobre o escopo de proteção do mutex.
- Testar: movimentos rápidos sequenciais de categorias; verificação de que a ordem final é consistente.

#### BUG-TER-07 — SetupViewModel: semi-auto EPG sync não sobrevive à morte do processo

**Descrição:** `SetupViewModel.kt:63–70` lança o sync de EPG em `viewModelScope`, que não sobrevive à morte do processo do Android. Se o utilizador sair do wizard durante o sync, o sync é cancelado.

**Localização:** `SetupViewModel.kt:63–70, 80–83`

**Solução:**
- Mover o sync de EPG para um `WorkManager` ou `CoroutineScope` independente do ViewModel (ex: `applicationContext` ou `Service`).
- Ou documentar claramente que o sync é cancelado se o utilizador sair do wizard.
- Testar: iniciar sync de EPG, matar o processo, verificar se o sync retoma ou é perdido.

#### BUG-TER-08 — AddSourceScreen: formToken pode ser reutilizado após navegação

**Descrição:** `AddSourceScreen.kt:149` usa `rememberSaveable` para `formToken`. Se o utilizador navegar para outro ecrã e voltar, o token é preservado, o que pode levar a submissões duplicadas de formulário.

**Localização:** `AddSourceScreen.kt:149–153`

**Solução:**
- Invalidar o `formToken` sempre que o ecrã sair de foco (`DisposableEffect` com `onDispose`).
- Gerar um novo token sempre que o ecrã de adicionar fonte for aberto.
- Testar: abrir adicionar fonte, navegar para outro ecrã, voltar, submeter — deve usar token novo.

### 8.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-TER-01 — Player: indicador visual de tentativas falhadas de canal

**Descrição:** `FailedChannelAttempts` regista tentativas falhadas mas não há indicador visual no HUD para o utilizador saber que um canal falhou e porquê.

**Solução:**
- Expor `FailedChannelAttempts.state` no `PlayerHud` como um toast ou banner temporário quando uma tentativa falha.
- Mostrar o código HTTP (ex: "Erro 404") por alguns segundos.
- Testar: zapping para canal indisponível; toast com código de erro aparece.

#### MEL-TER-02 — Update: notificação de atualização disponível fora do ecrã de settings

**Descrição:** `UpdateDialog` só é mostrado se o utilizador abrir Settings → Check for updates ou se houver um prompt automático. Não há notificação persistente.

**Solução:**
- Adicionar notificação Android (canal de notificações) quando uma atualização estiver disponível.
- Permitir desativar notificações de atualização nas definições.
- Testar: atualização disponível; notificação aparece na area de notificações do Android TV.

#### MEL-TER-03 — Customize: busca rápida na lista de categorias

**Descrição:** O ecrã de Customize mostra todas as categorias mas não tem busca rápida para encontrar uma categoria específica em listas longas.

**Solução:**
- Adicionar campo de busca no ecrã de Customize que filtra as categorias em tempo real.
- Manter o foco D-pad funcional na lista filtrada.
- Testar: digitar nome de categoria; lista filtra; navegação D-pad funciona nos resultados filtrados.

#### MEL-TER-04 — Setup: validação de URL em tempo real no ecrã de adicionar fonte

**Descrição:** `AddSourceScreen` não valida o formato da URL enquanto o utilizador digita; o erro só aparece após tentar adicionar.

**Solução:**
- Adicionar validação assíncrona da URL (HEAD request leve) com feedback visual imediato (ícone de erro/check).
- Desativar o botão "Adicionar" enquanto a URL for inválida.
- Testar: digitar URL inválida; ícone de erro aparece; botão desativado; URL válida; botão ativado.

### 8.3 Novas Inovações (com descrição/solução)

#### INN-TER-01 — Modo "Espelho de Ecrã" para dispositivos móveis

**Descrição:** Permitir que um telemóvel ou tablet espelhe o ecrã da OwnTV TV na rede local.

**Solução/Implementação:**
- Adicionar servidor de espelho leve na OwnTV TV (ex: servidor HTTP local que serve frames do player via WebSocket ou MJPEG).
- App móvel conecta via IP da box TV.
- Qualidade ajustável (baixa/média/alta) para economizar largura de banda.
- Desativável por defeito por motivos de segurança/largura de banda.

#### INN-TER-02 — Integração com calendário do sistema Android (gravações)

**Descrição:** Adicionar eventos de gravação ao calendário do sistema Android para lembretes.

**Solução/Implementação:**
- Usar `CalendarContract` para criar eventos de calendário para gravações agendadas.
- Lembrete configurável (ex: 10 minutos antes).
- Sincronizar com o estado da gravação (concluída, falhada).
- Respeitar perfis (apenas perfis adultos).

#### INN-TER-03 — Modo "Cinema" com iluminação ambiente (Ambilight simulado)

**Descrição:** Usar as cores dominantes do vídeo em reprodução para alterar a cor de fundo da UI ou enviar para lâmpadas inteligentes.

**Solução/Implementação:**
- Extrair cores dominantes do frame atual do player (via `SubtitleOverlay` ou superfície de vídeo).
- Aplicar como cor de fundo da barra lateral ou painel de pré-visualização.
- Opcional: integração com Philips Hue ou similar via API local.
- Desactivável para economizar GPU.

#### INN-TER-04 — Partilha de canais favoritos entre dispositivos (QR code)

**Descrição:** Partilhar a lista de canais favoritos de uma OwnTV para outra via QR code.

**Solução/Implementação:**
- Gerar QR code com a lista de canais favoritos (sem credenciais, apenas referências).
- Outra OwnTV scaneia o QR e importa os favoritos para o seu perfil.
- Encriptação opcional dos dados partilhados.
- Funciona na mesma rede local ou via código curto.

#### INN-TER-05 — Modo "Crianças" com ecrã inicial dedicado e simplificado

**Descrição:** Ecrã inicial completamente diferente para o perfil Kids, com ícones grandes, cores vivas e apenas canais apropriados.

**Solução/Implementação:**
- Detetar perfil Kids e carregar layout alternativo do Home.
- Remover opções de gravações, downloads, settings avançados.
- Canais filtrados por categoria "infantil".
- Bloqueio de saída do modo Kids com PIN dos pais.


---

## 9. Quarta Ronda de Análise — Novos Bugs, Melhorias e Inovações (21/10/2026)

Na quarta ronda, os mesmos quatro agentes especializados reinspecionaram áreas críticas adicionais: Backup Automático (AutomaticBackupWorker, AutomaticBackupPolicy, AutomaticBackupPreferences, AutomaticBackupControls), Serviço de Arranque Automático (AutoStartService), Lembretes de EPG (ProgrammeReminderStore, ProgrammeReminderDialog), Gate de Carregamento EPG (GuideLoadGate), StorageBrowser, Ecrã de Settings, ecrãs More (UserDataScreen, MoreScreen), EpgSemiAutoSync, EpgSyncPrompt, RemoteBackupRestoreScreen, LocalBackupOperation, RemoteBackupExportOperation, BackupScreen. O Council consolidou os novos achados.

### 9.1 Novos Bugs Encontrados (com soluções)

#### BUG-QUA-01 — AutomaticBackupWorker: staging file fica órfão se renameTo falhar

**Descrição:** `AutomaticBackupWorker.kt:66–79` cria um diretório de staging (`.owntv-auto-{UUID}`) e exporta o backup para lá. Depois renomeia para o destino final (`owntv-auto-{now}-{UUID}.own`). Se o `renameTo` falhar (ex: sistema de ficheiros diferente, espaço insuficiente), o ficheiro fica no staging e nunca é movido nem limpo corretamente, pois `published = true` já foi definido antes do rename.

**Localização:** `AutomaticBackupWorker.kt:66–82`

**Solução:**
- Mover a definição de `published = true` para depois do `renameTo` ter sucesso.
- Se o `renameTo` falhar, apagar o ficheiro de staging e reportar falha.
- No `finally`, limpar o staging apenas se não foi publicado com sucesso.
- Testar: espaço insuficiente no destino, sistema de ficheiros diferente, renameTo falhado.

#### BUG-QUA-02 — AutomaticBackupPreferences: chave de encriptação sem rotação nem recuperação

**Descrição:** `AutomaticBackupPreferences.kt:94–115` gera uma chave AES-256 no AndroidKeyStore para encriptar a configuração de backup automático. Não há rotação de chave, nem mecanismo de recuperação se o KeyStore for limpo (reset de fábrica, atualização de SO, limpar dados da app). Todos os backups automáticos encriptados ficam ilegíveis permanentemente.

**Localização:** `AutomaticBackupPreferences.kt:94–115`

**Solução:**
- Adicionar mecanismo de rotação de chave periodicamente (ex: a cada 6 meses).
- Adicionar opção de exportar/importar a chave de encriptação protegida por PIN do utilizador.
- Adicionar aviso na UI se a chave do KeyStore não estiver disponível.
- Testar: limpar dados da app, reset do KeyStore, tentar descriptografar configuração antiga.

#### BUG-QUA-03 — AutoStartService: pendingWake nunca resetado em alguns caminhos

**Descrição:** `AutoStartService.kt:33–36` define `pendingWake = true` em `ACTION_SCREEN_OFF` e também em `ACTION_SCREEN_ON`. Mas se o ecrã desligar e voltar a ligar sem `ACTION_USER_PRESENT` (ex: toque remoto), o `pendingWake` permanece `true` e pode causar lançamentos inesperados da app em momentos inapropriados.

**Localização:** `AutoStartService.kt:30–38, 73–92`

**Solução:**
- Resetar `pendingWake = false` após cada tentativa de lançamento (seja bem-sucedida ou não).
- Adicionar throttling mais agressivo (ex: não lançar se o último lançamento foi há menos de 30 segundos).
- Testar: desligar/ligar ecrã rapidamente, lançar app manualmente enquanto pendingWake está true.

#### BUG-QUA-04 — ProgrammeReminderStore: lembretes expirados acumulam sem limpeza em background

**Descrição:** `ProgrammeReminderStore.kt:116–160` (`evaluateDue`) apenas poda lembretes expirados quando chamado em foreground (`inForeground = true`). Se a app ficar em background por muito tempo, lembretes expirados acumulam-se na memória sem nunca serem removidos.

**Localização:** `ProgrammeReminderStore.kt:116–160`

**Solução:**
- Adicionar limpeza automática de lembretes expirados no `onCleared()` do ViewModel ou num Worker periódico.
- Ou podar lembretes expirados independentemente do estado de foreground.
- Testar: app em background por horas, verificar se lembretes expirados são removidos.

#### BUG-QUA-05 — ProgrammeReminderDialog: focus request pode falhar silenciosamente

**Descrição:** `ProgrammeReminderDialog.kt:53–58` usa `repeat(5)` com `withFrameNanos` para tentar pedir foco. Se o diálogo não estiver completamente composto após 5 frames, o foco nunca é definido e o utilizador não sabe onde está no diálogo.

**Localização:** `ProgrammeReminderDialog.kt:53–58`

**Solução:**
- Substituir o `repeat(5)` por um `LaunchedEffect` que tente pedir foco até ter sucesso, com timeout máximo (ex: 2 segundos).
- Usar `focusRequester` diretamente no `Modifier` do botão principal em vez de depender de LaunchedEffect.
- Testar: abrir diálogo rapidamente, verificar se o foco está no botão correto.

#### BUG-QUA-06 — GuideLoadGate: janela do guia desatualizada após longo período inativo

**Descrição:** `GuideLoadGate.kt:8–13` define `dirty = true` quando `setActive(false)` é chamado, mas não há mecanismo para forçar um reload quando o guia volta a ficar ativo após estar inativo por muito tempo (ex: app em background durante horas, janela de relógio do EPG desatualizada).

**Localização:** `GuideLoadGate.kt:4–24`

**Solução:**
- Adicionar um timestamp de última atualização e forçar reload se exceder um limite (ex: 30 minutos).
- Ou verificar se a janela atual do guia cobre o horário atual antes de considerar o guia como válido.
- Testar: app em background durante horas, voltar ao guia, verificar se a janela do "agora" está correta.

#### BUG-QUA-07 — StorageBrowser: dismissOnBackPress = false prende o utilizador

**Descrição:** `StorageBrowser.kt:76–78` define `dismissOnBackPress = false`. Se o utilizador abrir o StorageBrowser e não houver botão de dismiss visível (ou não souber usá-lo), fica preso no ecrã sem forma de voltar.

**Localização:** `StorageBrowser.kt:76–78`

**Solução:**
- Definir `dismissOnBackPress = true` e adicionar um botão "Cancelar" visível no ecrã do StorageBrowser.
- Ou adicionar um handler de Back que retorne para o ecrã anterior.
- Testar: abrir StorageBrowser, pressionar Back no comando remoto, verificar se fecha.

#### BUG-QUA-08 — EpgSemiAutoSync: falha de um feed não especifica qual feed falhou

**Descrição:** `EpgSemiAutoSync.kt:29–48` itera sobre múltiplas URLs de EPG e se uma falhar, guarda a exceção mas não especifica qual URL falhou. O utilizador só vê "Falhou" genérico no ecrã de sync.

**Localização:** `EpgSemiAutoSync.kt:29–48`

**Solução:**
- Adicionar a URL ou nome do feed que falhou à mensagem de erro.
- Mostrar uma lista de feeds com estado individual (sincronizado / falhado).
- Testar: múltiplos feeds de EPG, um deles inacessível, verificar se o utilizador sabe qual falhou.

#### BUG-QUA-09 — RemoteBackupRestoreServer: múltiplos backups chegando rapidamente sobrescrevem

**Descrição:** `RemoteBackupRestoreScreen.kt:73` usa `LaunchedEffect(backups) { backups.collect(onBackupReceived) }`. Se múltiplos backups forem enviados rapidamente via LAN companion, cada um dispara `onBackupReceived` mas não há fila; o último sobrescreve os anteriores no ecrã de seleção.

**Localização:** `RemoteBackupRestoreScreen.kt:58–74`

**Solução:**
- Acumular os backups recebidos numa lista em vez de processar cada um individualmente.
- Mostrar todos os backups recebidos no ecrã de seleção de restauro.
- Testar: enviar múltiplos backups rapidamente via LAN, verificar se todos aparecem.

#### BUG-QUA-10 — SettingsScreen: rememberSaveable pode causar inconsistência de estado

**Descrição:** `SettingsScreen.kt` usa `rememberSaveable` para múltiplos estados (tab, selectedGroup, displayedGroup, lastTab, openMiniPlayer, videoSection, videoRowKey, deepReturnKey, navEntry, recordingEntry). Se o utilizador navegar rapidamente entre secções, o estado guardado pode não corresponder à secção atual.

**Localização:** `SettingsScreen.kt:212, 277–278, 402–419`

**Solução:**
- Revisar cada uso de `rememberSaveable` para garantir que o estado é invalidado quando a secção muda.
- Adicionar limpeza de estado quando o utilizador sai de uma secção de settings.
- Testar: navegar rapidamente entre secções de settings, verificar se o estado não vaza entre secções.

### 9.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-QUA-01 — Indicador de status do backup automático na tela principal de Backup

**Descrição:** O status do backup automático está apenas em `AutomaticBackupControls`, mas não é visível no ecrã principal de Backup & Restore.

**Solução:**
- Adicionar uma linha de status no ecrã principal de Backup mostrando o próximo backup automático, último sucesso e falhas recentes.
- Testar: abrir ecrã de Backup, verificar se o status do backup automático é visível.

#### MEL-QUA-02 — Opção de "backup antes de atualização" no UpdateDialog

**Descrição:** `UpdateDialog.kt` permite verificar e instalar atualizações, mas não oferece fazer backup antes de atualizar.

**Solução:**
- Adicionar opção "Fazer backup antes de atualizar" no diálogo de atualização.
- Se selecionado, iniciar backup automático antes de baixar a nova versão.
- Testar: atualização disponível, selecionar backup antes, verificar se o backup é criado.

#### MEL-QUA-03 — Busca rápida no StorageBrowser

**Descrição:** `StorageBrowser.kt` não tem campo de busca para encontrar pastas ou arquivos rapidamente em listas longas.

**Solução:**
- Adicionar campo de busca no StorageBrowser que filtra pastas/arquivos em tempo real.
- Manter foco D-pad funcional na lista filtrada.
- Testar: digitar nome de pasta, lista filtra, navegação D-pad funciona.

#### MEL-QUA-04 — Confirmação antes de limpar lembretes de EPG expirados

**Descrição:** `ProgrammeReminderStore.kt` remove lembretes expirados sem confirmação do utilizador.

**Solução:**
- Adicionar diálogo de confirmação antes de limpar lembretes expirados em lote.
- Ou mostrar quantos lembretes serão removidos antes da ação.
- Testar: lembretes expirados presentes, ação de limpeza, diálogo de confirmação aparece.

#### MEL-QUA-05 — Indicador de progresso de sync EPG na tela de configurações de fontes EPG

**Descrição:** A tela de configurações de fontes EPG não mostra progresso de sync em tempo real.

**Solução:**
- Adicionar indicador de progresso (contagem de programas sincronizados) ao lado de cada fonte EPG.
- Mostrar estado: sincronizando, sincronizado, falhado.
- Testar: iniciar sync EPG, verificar se o progresso é visível.

### 9.3 Novas Inovações (com descrição/solução)

#### INN-QUA-01 — Backup automático para serviços de nuvem

**Descrição:** O backup automático atual guarda apenas localmente. Adicionar suporte para Google Drive, Dropbox ou similar.

**Solução/Implementação:**
- Adicionar opção de conectar conta de nuvem (OAuth).
- Fazer upload do backup automático para a nuvem após criar localmente.
- Restaurar de backup na nuvem quando configurar um novo dispositivo.
- Encriptação de ponta a ponta antes do upload.

#### INN-QUA-02 — Sincronização de lembretes de EPG entre dispositivos

**Descrição:** Lembretes de EPG criados numa OwnTV TV não aparecem noutra OwnTV TV do mesmo utilizador.

**Solução/Implementação:**
- Adicionar opção de sincronizar lembretes de EPG via conta própria da OwnTV (nuvem leve).
- Criar/editar/dismissar lembretes em qualquer dispositivo, sincronizado para todos.
- Respeitar perfis e apenas sincronizar perfis adultos.

#### INN-QUA-03 — Modo "Economia de Dados" para sync de EPG e metadados

**Descrição:** O sync de EPG e download de metadados (TMDB, OpenSubtitles) consomem dados móveis em boxes com ligação limitada.

**Solução/Implementação:**
- Adicionar definição "Economia de Dados" que limita sync de EPG a Wi-Fi, reduz qualidade de posters, desativa pré-visualizações de vídeo.
- Mostrar aviso quando tentar sync em rede móvel.
- Testar: ativar economia de dados, tentar sync EPG em rede móvel, verificar aviso.

#### INN-QUA-04 — Integração com assistente de voz para lembretes de EPG

**Descrição:** Permitir criar lembretes de EPG via comando de voz (Google Assistant/Alexa).

**Solução/Implementação:**
- Integrar com Android TV Voice Interaction API.
- Comandos: "lembrar-me do jogo às 21h", "gravar o filme de amanhã".
- Confirmar criação do lembrete por voz ou ecrã.
- Testar: comando de voz para criar lembrete, verificar se aparece na lista de lembretes.

#### INN-QUA-05 — Modo "Família" com perfis vinculados e controlo parental centralizado

**Descrição:** Vários perfis familiares com um "pai de família" que controla o que cada perfil pode ver.

**Solução/Implementação:**
- Adicionar perfil "Administrador Familiar" com PIN mestre.
- Definir limites de conteúdo por perfil (categorias permitidas, horários de visualização).
- Receber notificações quando um perfil infantil tenta aceder a conteúdo bloqueado.
- Testar: criar perfil infantil, definir restrições, tentar aceder a conteúdo bloqueado.

---

## 10. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das quatro rondas (primeira, segunda, terceira e quarta). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–9 acima.

### 9.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |

### 9.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção "backup antes de atualização" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |

### 9.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo "Convidado" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com "Watch Next" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo "Espelho de Ecrã" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo "Cinema" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo "Crianças" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo "Economia de Dados" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo "Família" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 9.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 10. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções.

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 11. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).

---

## 12. Limites da Análise

- Esta é uma **análise estática de fontes e contratos**; não foram executados builds, testes nem ensaios em dispositivos físicos durante esta tarefa.
- Nenhuma alteração de produção foi aplicada como parte desta análise.
- O core (`OwnTV_Core`) é um repositório separado; alterações no core requerem snapshot e atualização do lock pelo fluxo existente.
- Os totais de testes citados em documentos anteriores referem-se a validações já concluídas nesses relatórios, não a execução nova nesta análise.
- As datas das quatro rondas de análise: 18/10/2026, 19/10/2026, 20/10/2026 e 21/10/2026.


---

## 11. Quinta Ronda de Análise — Novos Bugs, Melhorias e Inovações (22/10/2026)

Na quinta ronda, os mesmos quatro agentes especializados reinspecionaram áreas críticas adicionais: Application lifecycle (OwnTVApp, MainActivity), Shell ViewModel e políticas de arranque, Database startup e recuperação, Player HUD e overlays, UI componentes de foco e gesto de canal, Customize bulk rename, Setup wizard e ecrãs de adicionar fonte. O Council consolidou os novos achados.

### 11.1 Novos Bugs Encontrados (com soluções)

#### BUG-QUIN-01 — MainActivity.onStop não para reprodução se database falhar

**Descrição:** `MainActivity.kt:151` só chama `liveViewModel.onBackground()`, `player.onAppBackgrounded()` e `previewEngine.onAppBackgrounded()` se `databaseProbeFinished && databaseProbeError == null`. Se a base de dados falhar ao abrir (migração corrompida, ficheiro SQLite ilegível), a reprodução nunca é parada quando a app vai para background, mantendo streams abertos e decoders activos desnecessariamente.

**Localização:** `MainActivity.kt:147–160`

**Solução:**
- Mover a paragem de reprodução para fora do guard de base de dados, ou ter um guard separado: sempre parar playback em `onStop()`, independente do estado da DB.
- Garantir que `onStop()` libertará recursos do player mesmo quando a DB estiver em estado de erro.
- Testar: corromper DB, abrir app, colocar em background, verificar se streams são libertados.

#### BUG-QUIN-02 — OwnTVApp appScope exceções são engolidas silenciosamente

**Descrição:** `OwnTVApp.kt:116–156` tem três blocos `appScope.launch { ... catch (_: Exception) { /* ... */ } }` que capturam e ignoram todas as exceções (backup scheduling, simple mode preferences, archive persistence). Se qualquer um falhar, não há log, não há notificação, não há retry agendado — falha silenciosa total.

**Localização:** `OwnTVApp.kt:116–156`

**Solução:**
- Substituir `catch (_: Exception)` por logging explícito via `LiveDiagnosticsLog.event()` ou `Log.e()`.
- Para backup scheduling, agendar retry na próxima inicialização (já comentado, mas não implementado).
- Não engolir exceções sem pelo menos registá-las para diagnóstico.
- Testar: falhar backup scheduling, verificar se há log de erro; falhar simple mode preferences, verificar se há log.

#### BUG-QUIN-03 — ShellViewModel migrações silenciosas sem feedback ao utilizador

**Descrição:** `ShellViewModel.kt:74–80` executa três migrações de uma vez com `runCatching { }` que engole todas as exceções. Se a migração de EPG, de flags de refresh ou de auto frame rate falhar, o utilizador não é notificado e o estado pode ficar inconsistente.

**Localização:** `ShellViewModel.kt:74–80`

**Solução:**
- Substituir `runCatching { }` por tratamento que registre falhas e, quando apropriado, mostre aviso ao utilizador.
- Adicionar estado de "migração pendente com erro" que possa ser consultado nas definições.
- Testar: forçar falha de migração, verificar se há feedback ao utilizador.

#### BUG-QUIN-04 — ChannelPressInput: onClick e onLongClick podem disparar duplicado com gesture

**Descrição:** `ChannelPressInput.kt:103–104` define `onClick { play() }` e `onLongClick { menu() }` na semântica, mas o sistema de gesture (`ChannelPressGesture`) também lida com pressionamento curto e longo. Pode haver disparo duplo: tanto o clique semântico quanto o gesture disparam play/menu para a mesma ação do utilizador.

**Localização:** `ChannelPressInput.kt:99–116`

**Solução:**
- Remover `onClick`/`onLongClick` da semântica quando o gesture system estiver ativo, ou garantir que o gesture consuma o evento antes da semântica.
- Usar apenas um caminho (gesture ou semântica), não ambos.
- Testar: pressionar OK curto, verificar se play dispara uma vez; pressionar OK longo, verificar se menu dispara uma vez.

#### BUG-QUIN-05 — DatabaseStartupViewModel retry sem limite

**Descrição:** `DatabaseStartupViewModel.kt:31–43` permite retry ilimitado — cada chamada cria um novo probe mas não há limite de tentativas nem contador. Um utilizador em desespero poderia iniciar retentativas infinitas.

**Localização:** `DatabaseStartupViewModel.kt:14–44`

**Solução:**
- Adicionar limite máximo de tentativas (ex: 3) antes de bloquear o botão de retry.
- Mostrar contador de tentativas restantes ao utilizador.
- Testar: clicar retry múltiplas vezes, verificar se é limitado.

#### BUG-QUIN-06 — FocusTrap restaura foco com tentativas silenciosas

**Descrição:** `FocusTrap.kt:81–96` captura `IllegalArgumentException` e `IllegalStateException` silenciosamente durante a restauração de foco após fechar diálogo. Exceções reais (ex: opener row removido, scroll host leaving composition) são mascaradas, deixando o utilizador sem foco.

**Localização:** `FocusTrap.kt:81–96`

**Solução:**
- Pelo menos logar as exceções capturadas para diagnóstico.
- Ter fallback: se não conseguir restaurar foco no opener, mover foco para o primeiro item visível da lista.
- Testar: fechar diálogo com opener removido, verificar se foco vai para item visível.

#### BUG-QUIN-07 — BulkRenameDialogs: returnFocus pode apontar para row removida

**Descrição:** `BulkRenameDialogs.kt:71–77` usa `rememberUpdatedState(returnFocus)` para restaurar foco à linha que abriu o fluxo. Se a linha foi removida (categoria escondida, item deletado) enquanto o fluxo está aberto, o foco é restaurado para um alvo inválido.

**Localização:** `BulkRenameDialogs.kt:71–77`

**Solução:**
- Validar que o `returnFocus` ainda está anexado à composição antes de restaurar.
- Fallback: se o alvo original não estiver disponível, ir para a primeira linha ou para o ecrã pai.
- Testar: abrir bulk rename, remover categoria enquanto o diálogo está aberto, fechar, verificar foco.

#### BUG-QUIN-08 — SetupWizard/AddSourceChooser: LaunchedEffectRequestFocus pode ser função inexistente

**Descrição:** `AddSourceChooserScreen.kt:53` chama `LaunchedEffectRequestFocus(firstFocus)` que parece ser uma função personalizada (não é `LaunchedEffect` padrão do Compose). Se esta função não existir ou estiver mal definida, o foco inicial nunca é definido.

**Localização:** `AddSourceChooserScreen.kt:52–53`

**Solução:**
- Verificar se `LaunchedEffectRequestFocus` está definida e funciona corretamente.
- Substituir por `LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }` padrão se não existir.
- Testar: abrir ecrã de escolher fonte, verificar se o foco está no primeiro botão.

#### BUG-QUIN-09 — MainActivity.onNewIntent não limpa pendingDeepLink após consumo

**Descrição:** `MainActivity.kt:140–144` define `pendingDeepLink` em `onNewIntent`, mas não há evidência de que seja limpo após ser consumido pelo shell. Se o mesmo intent for re-enviado, o deep link pode ser processado duas vezes.

**Localização:** `MainActivity.kt:140–144`, `OwnTVShell.kt` (consumo de `pendingDeepLink`)

**Solução:**
- Limpar `pendingDeepLink = null` após o shell consumi-lo.
- Ou usar um mecanismo de "consumido" (boolean flag).
- Testar: enviar deep link duas vezes, verificar se é processado apenas uma.

#### BUG-QUIN-10 — StreamInfoOverlay: rows pode ficar vazia e overlay desaparece

**Descrição:** `StreamInfoOverlay.kt:60–65` define `rows` como `emptyList()` inicialmente e preenche via `PlayerHeartbeat.beat.collect`. Se o heartbeat parar ou o player não emitir batimentos, o overlay fica vazio e `if (rows.isEmpty()) return` faz o overlay desaparecer sem aviso.

**Localização:** `StreamInfoOverlay.kt:56–71`

**Solução:**
- Adicionar estado de "carregando" ou "sem dados" em vez de simplesmente não renderizar.
- Ou usar um timer de fallback além do heartbeat.
- Testar: abrir overlay de info durante reprodução, verificar se dados aparecem.

### 11.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-QUIN-01 — MainActivity: separar paragem de playback do estado da base de dados

**Descrição:** A paragem de playback em `onStop()` está condicionada ao estado da DB, o que é incorreto — playback deve parar independentemente.

**Solução:**
- Extrair paragem de playback para um bloco separado que sempre executa em `onStop()`.
- Manter o guard de DB apenas para lógica de recuperação de sessão, não para paragem de player.

#### MEL-QUIN-02 — OwnTVApp: logging explícito de exceções em appScope

**Descrição:** As três exceções engolidas em `OwnTVApp.kt` devem ser pelo menos logadas.

**Solução:**
- Adicionar `LiveDiagnosticsLog.event()` ou `Log.e()` em cada `catch (_: Exception)`.
- Manter o comportamento de não abortar o arranque, mas registar para diagnóstico.

#### MEL-QUIN-03 — ShellViewModel: feedback de erro de migração

**Descrição:** Migrações silenciosas deixam o utilizador no escuro sobre falhas.

**Solução:**
- Adicionar estado de erro de migração acessível nas definições ou ecrã de recuperação.
- Mostrar aviso discreto se uma migração crítica falhar.

#### MEL-QUIN-04 — ChannelPressInput: unificar caminho de ativação

**Descrição:** Remover duplicação entre semântica onClick/onLongClick e gesture system.

**Solução:**
- Manter apenas o gesture system para press/long press, remover onClick/onLongClick da semântica quando gesture estiver ativo.

#### MEL-QUIN-05 — DatabaseStartupViewModel: limitar retentativas

**Descrição:** Retry ilimitado pode levar a loops infinitos.

**Solução:**
- Adicionar contador máximo de tentativas (3) e desativar botão após exceder.

#### MEL-QUIN-06 — FocusTrap: logging e fallback de foco

**Descrição:** Exceções de restauração de foco são mascaradas.

**Solução:**
- Logar exceções capturadas.
- Adicionar fallback para primeiro item visível se o opener não estiver disponível.

#### MEL-QUIN-07 — BulkRenameDialogs: validar returnFocus antes de restaurar

**Descrição:** Alvo de foco pode ter sido removido.

**Solução:**
- Verificar se o alvo ainda está na composição; senão, ir para primeira linha ou ecrã pai.

#### MEL-QUIN-08 — AddSourceChooser: usar LaunchedEffect padrão para foco

**Descrição:** Função personalizada `LaunchedEffectRequestFocus` pode não existir.

**Solução:**
- Substituir por `LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }`.

#### MEL-QUIN-09 — MainActivity: limpar pendingDeepLink após consumo

**Descrição:** Deep link pode ser processado duas vezes.

**Solução:**
- Definir `pendingDeepLink = null` após o shell consumi-lo.

#### MEL-QUIN-10 — StreamInfoOverlay: estado de carregamento/sem dados

**Descrição:** Overlay desaparece se rows estiver vazio.

**Solução:**
- Mostrar estado "sem dados" ou "carregando" em vez de não renderizar.

### 11.3 Novas Inovações (com descrição/solução)

#### INN-QUIN-01 — Modo "Resumo de Sessão" automático no encerramento

**Descrição:** Quando o utilizador fecha a app, mostrar um resumo rápido: quanto tempo assistiu, quantos canais viu, gravações em andamento.

**Solução/Implementação:**
- Rastrear tempo de visualização por canal na sessão atual.
- No `onStop()` ou `onDestroy()`, mostrar diálogo resumido com opção de "Ver depois".
- Dados apenas em memória, não persistem a não ser que o utilizador escolha guardar.

#### INN-QUIN-02 — Integração com Android TV "Continue Watching" nativo do sistema

**Descrição:** Publicar automaticamente no "Continue Watching" do Android TV (launcher do sistema) o conteúdo em andamento.

**Solução/Implementação:**
- Usar `TvProvider` / `WatchNextPrograms` para escrever conteúdo em andamento.
- Atualizar progresso de visualização periodicamente.
- Permitir iniciar do ecrã inicial do Android TV diretamente no ponto onde parou.

#### INN-QUIN-03 — Modo "Espelho Multi-Box" para sincronizar reprodução entre OwnTVs

**Descrição:** Várias boxes OwnTV na mesma rede podem sincronizar reprodução (mesmo canal, mesmo tempo) para ver em múltiplas salas.

**Solução/Implementação:**
- Descoberta mDNS de outras OwnTVs na rede.
- Sincronização de estado de reprodução via WebSocket local.
- Líder (box a reproduzir) envia timestamp e estado; seguidores ajustam.

#### INN-QUIN-04 — Controlo de qualidade de imagem por canal (perfil de codec)

**Descrição:** Permitir forçar codec específico (H.264, H.265, AV1) ou perfil de decodificação por canal individual.

**Solução/Implementação:**
- Adicionar definição por canal: "Forçar decodificação por software", "Forçar hardware", "Permitir non-IDR".
- Guardar na base de dados de personalização de canais.
- Aplicar no momento da tune via `LiveStreamQuirks`.

#### INN-QUIN-05 — Modo "Ecrã Negro com Áudio" para canais de rádio/música

**Descrição:** Para canais de rádio IPTV, desligar completamente o vídeo e mostrar apenas visualizador de áudio ou ecrã negro.

**Solução/Implementação:**
- Detetar streams apenas-áudio (sem track de vídeo) e ativar automaticamente.
- Ou definir manualmente nas definições do canal.
- Mostrar informações do programa (nome, artista, género) em ecrã negro com visualizador de nível de áudio.

---

## 12. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das cinco rondas (primeira, segunda, terceira, quarta e quinta). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–11 acima.

### 12.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER, QUA, QUIN)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |
| BUG-QUIN-01 | P2 | Parar reprodução em onStop() independente do estado da DB | Secção 11.1 |
| BUG-QUIN-02 | P2 | Logar exceções em appScope da OwnTVApp | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-04 | P3 | Unificar caminho de ativação no ChannelPressInput | Secção 11.1 |
| BUG-QUIN-05 | P3 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.1 |
| BUG-QUIN-06 | P3 | Logging e fallback de foco no FocusTrap | Secção 11.1 |
| BUG-QUIN-07 | P3 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.1 |
| BUG-QUIN-08 | P3 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.1 |
| BUG-QUIN-09 | P3 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.1 |
| BUG-QUIN-10 | P3 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.1 |

### 12.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção "backup antes de atualização" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |
| MEL-QUIN-01 | Separar paragem de playback do estado da base de dados | Secção 11.2 |
| MEL-QUIN-02 | Logging explícito de exceções em appScope | Secção 11.2 |
| MEL-QUIN-03 | Feedback de erro de migração no ShellViewModel | Secção 11.2 |
| MEL-QUIN-04 | Unificar caminho de ativação no ChannelPressInput | Secção 11.2 |
| MEL-QUIN-05 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.2 |
| MEL-QUIN-06 | Logging e fallback de foco no FocusTrap | Secção 11.2 |
| MEL-QUIN-07 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.2 |
| MEL-QUIN-08 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.2 |
| MEL-QUIN-09 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.2 |
| MEL-QUIN-10 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.2 |

### 12.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA, QUIN)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo "Convidado" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com "Watch Next" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo "Espelho de Ecrã" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo "Cinema" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo "Crianças" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo "Economia de Dados" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo "Família" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| INN-QUIN-01 | Modo "Resumo de Sessão" automático no encerramento | Secção 11.3 |
| INN-QUIN-02 | "Continue Watching" nativo do Android TV launcher | Secção 11.3 |
| INN-QUIN-03 | Modo "Espelho Multi-Box" para sincronizar reprodução | Secção 11.3 |
| INN-QUIN-04 | Controlo de qualidade de imagem por canal (perfil de codec) | Secção 11.3 |
| INN-QUIN-05 | Modo "Ecrã Negro com Áudio" para canais de rádio | Secção 11.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 12.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 13. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02, BUG-QUIN-01, BUG-QUIN-02, BUG-QUIN-03

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação; onStop para reprodução independentemente da DB; exceções de appScope são logadas; migrações de ShellViewModel têm feedback de erro.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10, BUG-QUIN-04 a BUG-QUIN-10

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções; ChannelPressInput sem ativação duplicada; DatabaseStartupViewModel retry limitado; FocusTrap com logging/fallback; BulkRenameDialogs valida returnFocus; AddSourceChooser foco padrão; MainActivity limpa pendingDeepLink; StreamInfoOverlay estado de carregamento.

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05, MEL-QUIN-01 a MEL-QUIN-10

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível; paragem de playback independente da DB; logging de exceções appScope; feedback de migração ShellViewModel; ativação unificada ChannelPressInput; retry limitado DatabaseStartupViewModel; logging/fallback FocusTrap; validação returnFocus BulkRenameDialogs; foco padrão AddSourceChooser; limpeza pendingDeepLink MainActivity; estado carregamento StreamInfoOverlay.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, INN-QUIN-01 a INN-QUIN-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 14. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
   - Colocar app em background com DB corrompida (verificar se playback para)
   - Pressionar OK curto/longo em canal (verificar ativação única)
   - Clicar retry na DB startup múltiplas vezes (verificar limite de 3)

5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).


---

## 11. Sexta Ronda de Análise — Novos Bugs, Melhorias e Inovações (23/10/2026)

Na sexta ronda, os mesmos quatro agentes especializados reinspecionaram áreas críticas adicionais: Player HUD e controles, Player LiveRestart, FrameRateController, Shell SyncStatusPill, TopBar, Sidebar, UI componentes (ChannelPressIndicator, PlayerFailureText, StreamInfoOverlay), e Shell components (IncompleteRestoreDialog, ArchivePlaybackConfigDialog, ExitDialog). O Council consolidou os novos achados.

### 11.1 Novos Bugs Encontrados (com soluções)

#### BUG-SEX-01 — PlayerHud: diálogos abertos não liberam foco ao fechar abruptamente

**Descrição:** `PlayerHud.kt` abre múltiplos diálogos (PlayerHudDialogs) mas não há garantia de que o foco retorne ao controlo do player após fechar um diálogo abruptamente (ex: pressionar Back várias vezes). O utilizador fica sem foco no ecrã do player.

**Localização:** `PlayerHud.kt`, `PlayerHudDialogs.kt`

**Solução:**
- Implementar `rememberDialogFocusRestore` (já existente em `FocusTrap.kt`) em todos os diálogos do HUD.
- Garantir que `onDismiss` de cada diálogo restaura o foco para o controlo que o abriu.
- Testar: abrir diálogo de áudio/legendas, fechar com Back, verificar se o foco retorna ao player.

#### BUG-SEX-02 — LiveRestart: janela de reinício não considera programas já iniciados

**Descrição:** `LiveRestart.kt:4–10` verifica `nowMs - it.startMs in 60_000L..windowSec.toLong() * 1_000L`, mas não trata o caso em que o programa já começou há mais de `windowSec` segundos. O utilizador não pode reiniciar um programa que já está a passar.

**Localização:** `LiveRestart.kt:4–10`

**Solução:**
- Alargar a condição para permitir reinício de programas já iniciados (dentro da janela de arquivo do provider).
- Adicionar verificação de que o arquivo do programa está disponível antes de oferecer reinício.
- Testar: programa iniciado há 5 minutos, verificar se o botão de reinício aparece.

#### BUG-SEX-03 — FrameRateController: cooldown de 5s pode bloquear mudanças legítimas

**Descrição:** `FrameRateController.kt:80` define `MODE_CHANGE_COOLDOWN_MS = 5_000L`, o que pode impedir mudanças de taxa de fotogramas legítimas quando o utilizador muda de canal rapidamente com conteúdos de taxas diferentes.

**Localização:** `FrameRateController.kt:80`

**Solução:**
- Reduzir o cooldown para 2s ou torná-lo adaptativo (menor se o conteúdo anterior e atual têm taxas muito diferentes).
- Adicionar exceção para mudanças de canal (priorizar a mudança imediata).
- Testar: mudar de canal de 24fps para 60fps rapidamente, verificar se a taxa de fotogramas muda.

#### BUG-SEX-04 — SyncStatusPill: linhas ocultas não indicam quantos syncs estão a correr

**Descrição:** `SyncStatusPill.kt:140–143` mostra "(+N more)" quando há mais de 4 syncs, mas não especifica quantos são de cada tipo (catálogo, EPG, gravações, downloads). O utilizador não sabe o que está a correr em background.

**Localização:** `SyncStatusPill.kt:140–143`

**Solução:**
- Mostrar a contagem por tipo nas linhas ocultas (ex: "+2 catálogo, +1 EPG").
- Ou adicionar um toque para expandir e ver todos os syncs ativos.
- Testar: iniciar 5+ syncs simultâneos, verificar se a contagem oculta é clara.

#### BUG-SEX-05 — TopBar: não mostra estado de sync em curso

**Descrição:** `TopBar.kt` não exibe nenhum indicador de sync em curso. O utilizador só vê o status no SyncStatusPill (bottom middle), que pode não ser notado.

**Localização:** `TopBar.kt`

**Solução:**
- Adicionar um pequeno indicador de "sync em curso" na TopBar (ex: ícone de sincronização a girar).
- Clicar no indicador abre o SyncStatusPill expandido.
- Testar: iniciar sync, verificar se o indicador aparece na TopBar.

#### BUG-SEX-06 — Sidebar: não mostra perfil ativo claramente

**Descrição:** `Sidebar.kt` mostra o avatar e nome do perfil, mas não indica se é um perfil infantil (Kids mode) ou se há restrições ativas.

**Localização:** `Sidebar.kt:70–80`

**Solução:**
- Adicionar indicador visual de "Kids mode" ao lado do nome do perfil.
- Mostrar ícone de "bloqueado" se houver restrições de conteúdo ativas.
- Testar: alternar entre perfil adulto e infantil, verificar se o indicador muda.

#### BUG-SEX-07 — ChannelPressIndicator: não mostra estágio atual do pressionamento

**Descrição:** `ChannelPressIndicator.kt` não existe como ficheiro separado (verificado na lista de componentes). O indicador visual de pressionamento longo (para versões/menu) não é consistente entre ecrãs.

**Localização:** `ChannelPressInput.kt`, `ChannelPressGesture.kt`

**Solução:**
- Criar um componente `ChannelPressIndicator` reutilizável que mostre o progresso do pressionamento (0–100%).
- Usar em todos os ecrãs que suportam pressionamento longo de canal.
- Testar: pressionar longo em canal, verificar se o indicador de progresso aparece.

#### BUG-SEX-08 — IncompleteRestoreDialog: não mostra o que foi ignorado

**Descrição:** `IncompleteRestoreDialog.kt` não lista quais secções ou itens foram ignorados durante uma restauração incompleta. O utilizador não sabe o que falta.

**Localização:** `IncompleteRestoreDialog.kt`

**Solução:**
- Adicionar lista de itens ignorados com motivo (ex: "perfil não encontrado", "fonte inexistente").
- Oferecer opção de "tentar novamente" apenas para os itens ignorados.
- Testar: restaurar backup com itens faltantes, verificar se a lista de ignorados aparece.

#### BUG-SEX-09 — ArchivePlaybackConfigDialog: não valida configuração antes de aplicar

**Descrição:** `ArchivePlaybackConfigDialog.kt` não valida se a configuração de arquivo selecionada é válida para o canal atual antes de aplicar.

**Localização:** `ArchivePlaybackConfigDialog.kt`

**Solução:**
- Adicionar validação de configuração antes de aplicar (ex: verificar se o arquivo existe para esse canal).
- Mostrar aviso se a configuração selecionada não é aplicável.
- Testar: selecionar arquivo inexistente, verificar se aparece aviso.

#### BUG-SEX-10 — ExitDialog: não confirma se há gravação em andamento

**Descrição:** `ExitDialog.kt` pede confirmação para sair, mas não avisa se há uma gravação em andamento que será interrompida.

**Localização:** `ExitDialog.kt`

**Solução:**
- Adicionar aviso no ExitDialog se houver gravação ativa ("Tem uma gravação em andamento. Sair interromperá a gravação.").
- Oferecer opção de "continuar gravação e sair depois" ou "sair e parar gravação".
- Testar: iniciar gravação, tentar sair, verificar se o aviso aparece.

### 11.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-SEX-01 — PlayerHud: restaurar foco após fechar diálogos

**Descrição:** Diálogos do HUD não restauram o foco adequadamente ao fechar.

**Solução:**
- Usar `rememberDialogFocusRestore` em todos os diálogos do PlayerHud.
- Testar: abrir/fechar diálogos, verificar se o foco retorna ao controlo correto.

#### MEL-SEX-02 — LiveRestart: alargar janela de reinício

**Descrição:** Permitir reinício de programas já iniciados dentro da janela de arquivo.

**Solução:**
- Modificar `restartableProgramme` para aceitar programas já iniciados.
- Testar: verificar se o botão de reinício aparece para programas em andamento.

#### MEL-SEX-03 — FrameRateController: cooldown adaptativo

**Descrição:** Cooldown de 5s é muito longo para mudanças de canal rápidas.

**Solução:**
- Reduzir cooldown para 2s ou torná-lo adaptativo.
- Testar: mudanças rápidas de canal com taxas de fotogramas diferentes.

#### MEL-SEX-04 — SyncStatusPill: contagem por tipo nas linhas ocultas

**Descrição:** "(+N more)" não especifica os tipos de sync ocultos.

**Solução:**
- Mostrar contagem por tipo (ex: "+2 catálogo, +1 EPG").
- Testar: múltiplos syncs, verificar contagem oculta.

#### MEL-SEX-05 — TopBar: indicador de sync em curso

**Descrição:** TopBar não mostra sync em curso.

**Solução:**
- Adicionar ícone de sincronização na TopBar quando há sync ativo.
- Testar: iniciar sync, verificar indicador na TopBar.

#### MEL-SEX-06 — Sidebar: indicador de perfil infantil

**Descrição:** Sidebar não mostra se o perfil é infantil.

**Solução:**
- Adicionar indicador visual de "Kids mode" ao lado do nome do perfil.
- Testar: alternar perfis, verificar indicador.

#### MEL-SEX-07 — Componente ChannelPressIndicator reutilizável

**Descrição:** Falta indicador visual consistente de pressionamento longo.

**Solução:**
- Criar componente `ChannelPressIndicator` com barra de progresso.
- Usar em todos os ecrãs de canal.
- Testar: pressionamento longo, verificar barra de progresso.

#### MEL-SEX-08 — IncompleteRestoreDialog: lista de itens ignorados

**Descrição:** Não mostra o que foi ignorado na restauração.

**Solução:**
- Listar itens ignorados com motivo.
- Testar: restauração incompleta, verificar lista de ignorados.

#### MEL-SEX-09 — ArchivePlaybackConfigDialog: validação de configuração

**Descrição:** Não valida configuração antes de aplicar.

**Solução:**
- Validar se o arquivo existe para o canal antes de aplicar.
- Testar: selecionar arquivo inválido, verificar aviso.

#### MEL-SEX-10 — ExitDialog: aviso de gravação ativa

**Descrição:** Não avisa sobre gravação em andamento ao sair.

**Solução:**
- Adicionar aviso no ExitDialog se houver gravação ativa.
- Testar: gravação ativa + sair, verificar aviso.

### 11.3 Novas Inovações (com descrição/solução)

#### INN-SEX-01 — Modo "Economia de Bateria" para boxes portáteis

**Descrição:** Algumas boxes Android TV são portáteis (com bateria). Precisam de modo de economia de bateria.

**Solução/Implementação:**
- Detectar se o dispositivo tem bateria (via `BatteryManager`).
- Adicionar definição "Economia de Bateria" que reduz qualidade de pré-visualizações, desativa sync em segundo plano e reduz brilho.
- Mostrar aviso quando a bateria está baixa.
- Testar: dispositivo com bateria, ativar economia, verificar redução de consumo.

#### INN-SEX-02 — Integração com Android TV "Launcher Apps" personalizados

**Descrição:** Permitir criar atalhos personalizados no launcher do Android TV para canais ou gravações específicas.

**Solução/Implementação:**
- Usar `ShortcutManagerCompat` para criar atalhos dinâmicos.
- Atalhos para "Ver canal X", "Ver gravação Y", "Abrir guia".
- Atalhos aparecem no ecrã inicial do Android TV.
- Testar: criar atalho, verificar se aparece no launcher do sistema.

#### INN-SEX-03 — Modo "Visita" para casas de férias/aluguer

**Descrição:** Modo temporário para hóspedes que não querem criar perfil permanente.

**Solução/Implementação:**
- Adicionar modo "Visita" no ecrã de seleção de perfil.
- Perfil temporário, sem gravações, sem favoritos, limpo ao sair.
- Pode adicionar fontes temporárias que são removidas ao sair.
- Testar: ativar modo visita, adicionar fonte, sair, verificar se foi limpo.

#### INN-SEX-04 — Relatórios de uso para pais (controlo parental avançado)

**Descrição:** Relatórios semanais/mensais do que as crianças viram.

**Solução/Implementação:**
- Rastrear conteúdo visto por perfil infantil.
- Gerar relatório semanal com canais vistos, horas, programas.
- Enviar por email ou mostrar nas definições de controlo parental.
- Testar: ativar rastreamento, gerar relatório, verificar conteúdo.

#### INN-SEX-05 — Integração com dispositivos domésticos inteligentes (iluminação ambiente)

**Descrição:** Sincronizar iluminação da sala com o conteúdo em reprodução.

**Solução/Implementação:**
- Integrar com Philips Hue, LIFX, ou similar via API local.
- Extrair cores dominantes do vídeo e aplicar às lâmpadas.
- Desactivável por defeito (privacidade/largura de banda).
- Testar: conectar lâmpada inteligente, reproduzir conteúdo, verificar mudança de cor.

---

## 12. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das seis rondas (primeira, segunda, terceira, quarta, quinta e sexta). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–11 acima.

### 12.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER, QUA, QUIN, SEX)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |
| BUG-QUIN-01 | P2 | Parar reprodução em onStop() independente do estado da DB | Secção 11.1 |
| BUG-QUIN-02 | P2 | Logar exceções em appScope da OwnTVApp | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-04 | P3 | Unificar caminho de ativação no ChannelPressInput | Secção 11.1 |
| BUG-QUIN-05 | P3 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.1 |
| BUG-QUIN-06 | P3 | Logging e fallback de foco no FocusTrap | Secção 11.1 |
| BUG-QUIN-07 | P3 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.1 |
| BUG-QUIN-08 | P3 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.1 |
| BUG-QUIN-09 | P3 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.1 |
| BUG-QUIN-10 | P3 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.1 |
| BUG-SEX-01 | P3 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.1 |
| BUG-SEX-02 | P3 | Alargar janela de reinício no LiveRestart | Secção 11.1 |
| BUG-SEX-03 | P3 | Cooldown adaptativo no FrameRateController | Secção 11.1 |
| BUG-SEX-04 | P3 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.1 |
| BUG-SEX-05 | P3 | Indicador de sync na TopBar | Secção 11.1 |
| BUG-SEX-06 | P3 | Indicador de perfil infantil na Sidebar | Secção 11.1 |
| BUG-SEX-07 | P3 | Componente ChannelPressIndicator reutilizável | Secção 11.1 |
| BUG-SEX-08 | P3 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.1 |
| BUG-SEX-09 | P3 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.1 |
| BUG-SEX-10 | P3 | Aviso de gravação ativa no ExitDialog | Secção 11.1 |

### 12.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção "backup antes de atualização" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |
| MEL-QUIN-01 | Separar paragem de playback do estado da base de dados | Secção 11.2 |
| MEL-QUIN-02 | Logging explícito de exceções em appScope | Secção 11.2 |
| MEL-QUIN-03 | Feedback de erro de migração no ShellViewModel | Secção 11.2 |
| MEL-QUIN-04 | Unificar caminho de ativação no ChannelPressInput | Secção 11.2 |
| MEL-QUIN-05 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.2 |
| MEL-QUIN-06 | Logging e fallback de foco no FocusTrap | Secção 11.2 |
| MEL-QUIN-07 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.2 |
| MEL-QUIN-08 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.2 |
| MEL-QUIN-09 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.2 |
| MEL-QUIN-10 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.2 |
| MEL-SEX-01 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.2 |
| MEL-SEX-02 | Alargar janela de reinício no LiveRestart | Secção 11.2 |
| MEL-SEX-03 | Cooldown adaptativo no FrameRateController | Secção 11.2 |
| MEL-SEX-04 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.2 |
| MEL-SEX-05 | Indicador de sync na TopBar | Secção 11.2 |
| MEL-SEX-06 | Indicador de perfil infantil na Sidebar | Secção 11.2 |
| MEL-SEX-07 | Componente ChannelPressIndicator reutilizável | Secção 11.2 |
| MEL-SEX-08 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.2 |
| MEL-SEX-09 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.2 |
| MEL-SEX-10 | Aviso de gravação ativa no ExitDialog | Secção 11.2 |

### 12.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA, QUIN, SEX)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo "Convidado" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com "Watch Next" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo "Espelho de Ecrã" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo "Cinema" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo "Crianças" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo "Economia de Dados" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo "Família" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| INN-QUIN-01 | Modo "Resumo de Sessão" automático no encerramento | Secção 11.3 |
| INN-QUIN-02 | "Continue Watching" nativo do Android TV launcher | Secção 11.3 |
| INN-QUIN-03 | Modo "Espelho Multi-Box" para sincronizar reprodução | Secção 11.3 |
| INN-QUIN-04 | Controlo de qualidade de imagem por canal (perfil de codec) | Secção 11.3 |
| INN-QUIN-05 | Modo "Ecrã Negro com Áudio" para canais de rádio | Secção 11.3 |
| INN-SEX-01 | Modo "Economia de Bateria" para boxes portáteis | Secção 11.3 |
| INN-SEX-02 | Atalhos personalizados no launcher Android TV | Secção 11.3 |
| INN-SEX-03 | Modo "Visita" para casas de férias/aluguer | Secção 11.3 |
| INN-SEX-04 | Relatórios de uso para pais (controlo parental avançado) | Secção 11.3 |
| INN-SEX-05 | Integração com iluminação ambiente doméstica | Secção 11.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 12.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 13. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02, BUG-QUIN-01, BUG-QUIN-02, BUG-QUIN-03

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação; onStop para reprodução independentemente da DB; exceções de appScope são logadas; migrações de ShellViewModel têm feedback de erro.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10, BUG-QUIN-04 a BUG-QUIN-10, BUG-SEX-01 a BUG-SEX-10

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções; ChannelPressInput sem ativação duplicada; DatabaseStartupViewModel retry limitado; FocusTrap com logging/fallback; BulkRenameDialogs valida returnFocus; AddSourceChooser foco padrão; MainActivity limpa pendingDeepLink; StreamInfoOverlay estado de carregamento; PlayerHud restaura foco após diálogos; LiveRestart alarga janela; FrameRateController cooldown adaptativo; SyncStatusPill contagem por tipo; TopBar indicador de sync; Sidebar indicador Kids; ChannelPressIndicator componente; IncompleteRestoreDialog lista ignorados; ArchivePlaybackConfigDialog validação; ExitDialog aviso gravação.

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05, MEL-QUIN-01 a MEL-QUIN-10, MEL-SEX-01 a MEL-SEX-10

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível; paragem de playback independente da DB; logging de exceções appScope; feedback de migração ShellViewModel; ativação unificada ChannelPressInput; retry limitado DatabaseStartupViewModel; logging/fallback FocusTrap; validação returnFocus BulkRenameDialogs; foco padrão AddSourceChooser; limpeza pendingDeepLink MainActivity; estado carregamento StreamInfoOverlay; foco restaurado PlayerHud; janela reinício LiveRestart; cooldown adaptativo FrameRateController; contagem por tipo SyncStatusPill; indicador sync TopBar; indicador Kids Sidebar; ChannelPressIndicator componente; lista ignorados IncompleteRestoreDialog; validação ArchivePlaybackConfigDialog; aviso gravação ExitDialog.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, INN-QUIN-01 a INN-QUIN-05, INN-SEX-01 a INN-SEX-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 14. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
   - Colocar app em background com DB corrompida (verificar se playback para)
   - Pressionar OK curto/longo em canal (verificar ativação única)
   - Clicar retry na DB startup múltiplas vezes (verificar limite de 3)
   - Abrir/fechar diálogos do PlayerHud (verificar restauração de foco)
   - Mudar canal rapidamente com taxas diferentes (verificar AFR)
   - Iniciar múltiplos syncs (verificar contagem oculta no pill)
   - Iniciar gravação e tentar sair (verificar aviso no ExitDialog)

5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).


---

## 12. Sétima Ronda de Análise — Novos Bugs, Melhorias e Inovações (24/10/2026)

Na sétima ronda, os mesmos quatro agentes especializados reinspecionaram áreas críticas adicionais: Injeção de Dependência (AppModule.kt, PlayerModule.kt), Build/CI (build.gradle.kts, settings.gradle.kts, android.yml, i18n.yml), Diagnósticos (UiPerformanceMonitor, UiWorkWindow, UiPerformanceSummary, UiPerformanceWindow), Internacionalização (LocalizedContent, RtlKeys), e ferramentas (snapshot_core.py). O Council consolidou os novos achados.

### 12.1 Novos Bugs Encontrados (com soluções)

#### BUG-SET-01 — AppModule: LiveViewModel usa 24 `get()` consecutivos, dependência da ordem

**Descrição:** `AppModule.kt:67–96` vincula `LiveViewModel` com 24 chamadas `get()` consecutivas. O comentão na linha 67 alerta que reordenar o construtor causaria `get()`s a resolver tipos errados silenciosamente. Se algum parâmetro for adicionado, removido ou reordenado no construtor de `LiveViewModel`, a injeção silenciosamente liga dependências erradas sem erro de compilação nem de Koin.

**Localização:** `AppModule.kt:67–96`

**Solução:**
- Substituir as 24 chamadas `get()` consecutivas por vinculações nomeadas (`named {}`) ou usar `viewModel { LiveViewModel(profileDao = get(), ...) }` com argumentos nomeados, como já é feito para `SettingsViewModel` nas linhas 110–143.
- Adicionar teste de unidade que verifique que cada dependência injetada é do tipo esperado.
- Testar: reordenar parâmetros do construtor de LiveViewModel, verificar se a app ainda funciona corretamente.

#### BUG-SET-02 — PlayerModule: OwnTVPlayer usa 9 `get()` consecutivos, mesmo risco

**Descrição:** `PlayerModule.kt:21–34` vincula `OwnTVPlayer` com 9 chamadas `get()` consecutivas. O comentão na linha 19–20 alerta para o mesmo problema: reordenar o construtor faria com que dependências do mesmo tipo fossem trocadas silenciosamente.

**Localização:** `PlayerModule.kt:21–34`

**Solução:**
- Substituir as 9 chamadas `get()` consecutivas por vinculações nomeadas ou argumentos nomeados com `single { OwnTVPlayer(context = get(), settings = get(), ...) }`.
- Adicionar teste de unidade para verificar tipos das dependências injetadas.
- Testar: reordenar parâmetros do construtor de OwnTVPlayer, verificar se a app ainda funciona.

#### BUG-SET-03 — BackupAppSettings vinculado como `single` com Context

**Descrição:** `AppModule.kt:45–47` vincula `BackupAppSettings` como `single` (singleton), mas recebe `Context` no construtor. Se o Context mudar (ex: mudança de configuração, atividade recriada), o singleton mantém uma referência a um Context antigo, podendo causar memory leaks ou comportamento inesperado.

**Localização:** `AppModule.kt:45–47`

**Solução:**
- Alterar o escopo de `single` para `factory` (nova instância cada vez) ou garantir que o Context usado é o Application Context (que não muda).
- Adicionar comentário a justificar por que é seguro usar `single` com Application Context.
- Testar: mudar de configuração (ex: idioma, tema), verificar se BackupAppSettings não retém Context antigo.

#### BUG-SET-04 — android.yml CI não testa variante release nem ABI x86_64

**Descrição:** `.github/workflows/android.yml:54` executa testes apenas na variante `standard+debug`. Não testa a variante `release` nem a ABI `x86_64`, que têm caminhos de código diferentes (native libs diferentes, ProGuard/R8 diferente).

**Localização:** `.github/workflows/android.yml:54`

**Solução:**
- Adicionar execução de testes nas variantes `standardRelease` e `x86_64`.
- Ou pelo menos executar lint e compilação nas outras variantes.
- Testar: fazer push com alteração que só afeta release/x86_64, verificar se CI deteta.

#### BUG-SET-05 — snapshot_core.py usa timestamp fixo em ZipInfo

**Descrição:** `tools/core_snapshot/snapshot_core.py:58` usa timestamp fixo `(2026, 10, 11, 0, 0, 0)` para todas as entradas do ZIP em vez do tempo de modificação real dos ficheiros. Isto pode causar problemas com cache incremental baseado em timestamps e dificultar a deteção de alterações.

**Localização:** `tools/core_snapshot/snapshot_core.py:58`

**Solução:**
- Substituir o timestamp fixo pelo tempo de modificação real de cada ficheiro (`path.stat().st_mtime`).
- Ou usar o tempo atual de forma consistente para todas as entradas.
- Testar: executar snapshot, verificar se os timestamps no ZIP refletem os tempos reais dos ficheiros.

#### BUG-SET-06 — UiWorkWindow.take() limpa contadores após leitura, risco de perda de dados

**Descrição:** `UiWorkWindow.kt:40–54` o método `take()` lê os contadores e depois chama `clear()` na linha 53. Se duas chamadas a `take()` ocorrerem em sequência rápida (ex: de threads diferentes), a segunda receberá dados vazios porque os contadores já foram limpos.

**Localização:** `UiWorkWindow.kt:40–54`

**Solução:**
- Tornar `take()` atómico: ler e limpar numa única operação sincronizada (já é `@Synchronized`, mas o `clear()` após o `return` é executado mesmo se o caller não consumir os dados).
- Ou adicionar um método `peek()` que leia sem limpar, e manter `take()` para leitura destrutiva.
- Testar: chamar `take()` duas vezes consecutivas, verificar se a segunda não perde dados.

#### BUG-SET-07 — UiPerformanceMonitor: divisão por refresh rate pode ser zero ou inválida

**Descrição:** `UiPerformanceMonitor.kt:29–30` calcula `fallbackDeadlineNs` dividindo 1 segundo pelo refresh rate do ecrã. Se o refresh rate for 0 ou não estiver disponível, usa 60fps como fallback. Mas em algumas boxes Android TV, o refresh rate pode ser reportado incorretamente ou mudar dinamicamente, causando cálculos de frame time errados.

**Localização:** `UiPerformanceMonitor.kt:29–38`

**Solução:**
- Adicionar validação: se refresh rate <= 0, usar um valor conservador (ex: 30fps) em vez de 60fps.
- Ouvir mudanças de refresh rate via `DisplayListener` e atualizar o `fallbackDeadlineNs` dinamicamente.
- Testar: conectar a uma box com refresh rate não padrão, verificar se o monitor não falha.

#### BUG-SET-08 — i18n.yml: pseudolocale não cobre variantes benchmark e non-minified

**Descrição:** `.github/workflows/i18n.yml:12` verifica pseudolocales apenas nos APKs debug e release. Não cobre as variantes `benchmark` e `non-minified`, que podem ter problemas de overflow de texto diferentes.

**Localização:** `.github/workflows/i18n.yml`

**Solução:**
- Estender a verificação de pseudolocale para incluir as variantes `benchmark` e `non-minified`.
- Ou documentar por que essas variantes não precisam da verificação.
- Testar: verificar se os APKs de benchmark contêm pseudolocales corretamente.

#### BUG-SET-09 — settings.gradle.kts: sem pinnings de versão para dependências críticas

**Descrição:** `settings.gradle.kts` e `build.gradle.kts` não usam um catálogo de versões (version catalog) para as dependências críticas. Isto pode levar a deriva silenciosa de versões (ex: uma nova versão de uma biblioteca ser puxada automaticamente e quebrar a compatibilidade).

**Localização:** `settings.gradle.kts`, `build.gradle.kts`

**Solução:**
- Adicionar um ficheiro `libs.versions.toml` com pinnings explícitos para todas as dependências críticas (AGP, Kotlin, Compose, ExoPlayer, Coil, Koin, Room, etc.).
- Configurar o build para usar o catálogo de versões.
- Testar: fazer build limpo, verificar se todas as versões são as esperadas.

#### BUG-SET-10 — LocalizedContent: Activity.recreate() pode causar loop infinito

**Descrição:** `LocalizedContent.kt:53–58` chama `findActivity(hostContext)?.recreate()` quando a tag do locale muda de família de script. Se a recriação da Activity não atualizar corretamente a tag no `localeStore`, o `LaunchedEffect` pode disparar novamente, causando um loop infinito de recriações.

**Localização:** `LocalizedContent.kt:53–58`

**Solução:**
- Adicionar um guard para evitar recriações consecutivas (ex: um booleano `isRecreating` ou um timestamp da última recriação).
- Verificar após o `recreate()` se a nova tag corresponde à esperada antes de permitir outra recriação.
- Testar: mudar de família de script (ex: latim para árabe), verificar se não há loop infinito.

### 12.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-SET-01 — AppModule: usar vinculações nomeadas para dependências de LiveViewModel

**Descrição:** Substituir `get()` consecutivo por argumentos nomeados.

**Solução:**
- Usar `viewModel { LiveViewModel(profileDao = get(), sourceDao = get(), ...) }` com nomes de parâmetro.
- Testar: verificar que cada dependência é do tipo correto.

#### MEL-SET-02 — PlayerModule: usar vinculações nomeadas para OwnTVPlayer

**Descrição:** Substituir `get()` consecutivo por argumentos nomeados.

**Solução:**
- Usar `single { OwnTVPlayer(context = get(), settings = get(), ...) }` com nomes de parâmetro.
- Testar: verificar que cada dependência é do tipo correto.

#### MEL-SET-03 — CI: testar variantes release e x86_64

**Descrição:** Estender o CI para cobrir mais variantes.

**Solução:**
- Adicionar etapas de teste/lint para `standardRelease` e `x86_64`.
- Testar: build e teste nas variantes adicionais.

#### MEL-SET-04 — snapshot_core.py: usar timestamps reais

**Descrição:** Substituir timestamp fixo por tempo de modificação real.

**Solução:**
- Usar `path.stat().st_mtime` para cada entrada do ZIP.
- Testar: verificar timestamps no ZIP gerado.

#### MEL-SET-05 — UiWorkWindow: adicionar método peek() não destrutivo

**Descrição:** Permitir leitura de contadores sem limpar.

**Solução:**
- Adicionar `peek()` que lê sem chamar `clear()`.
- Manter `take()` para leitura destrutiva.
- Testar: verificar que `peek()` não limpa os dados.

#### MEL-SET-06 — UiPerformanceMonitor: validar refresh rate

**Descrição:** Adicionar validação para refresh rate inválido.

**Solução:**
- Se refresh rate <= 0, usar 30fps como fallback conservador.
- Ouvir mudanças de refresh rate dinamicamente.
- Testar: box com refresh rate não padrão.

#### MEL-SET-07 — i18n.yml: estender pseudolocale para variantes benchmark e non-minified

**Descrição:** Cobrir mais variantes na verificação de pseudolocale.

**Solução:**
- Adicionar variantes `benchmark` e `non-minified` ao i18n workflow.
- Testar: verificar pseudolocales em todas as variantes.

#### MEL-SET-08 — Adicionar version catalog (libs.versions.toml)

**Descrição:** Fixar versões de dependências críticas.

**Solução:**
- Criar `libs.versions.toml` com versões explícitas.
- Configurar build para usar o catálogo.
- Testar: build limpo com versões fixas.

#### MEL-SET-09 — LocalizedContent: evitar loop infinito de recreate()

**Descrição:** Adicionar guard contra recriações consecutivas.

**Solução:**
- Adicionar booleano de controle ou timestamp para evitar loops.
- Testar: mudança de família de script, verificar ausência de loop.

### 12.3 Novas Inovações (com descrição/solução)

#### INN-SET-01 — Dashboard de dependências e build

**Descrição:** Adicionar um ecrã nas definições que mostra as versões de todas as dependências críticas (AGP, Kotlin, ExoPlayer, Coil, Koin, etc.) e permite verificar se estão atualizadas.

**Solução/Implementação:**
- Ler as versões do `BuildConfig` e dos metadados das dependências.
- Exibir numa lista nas definições → Avançado → Dependências.
- Adicionar botão para verificar atualizações disponíveis.
- Testar: abrir ecrã, verificar se as versões estão corretas.

#### INN-SET-02 — Modo de diagnóstico de build para suporte

**Descrição:** Permitir exportar informações de build (versões, variantes, flags de compilação) para suporte técnico.

**Solução/Implementação:**
- Adicionar opção nas definições para exportar relatório de build.
- Incluir versões de dependências, flags de ProGuard/R8, ABI, variante de build.
- Exportar como ficheiro de texto ou JSON.
- Testar: exportar relatório, verificar se contém todas as informações relevantes.

#### INN-SET-03 — CI com deteção de deriva de dependências

**Descrição:** Adicionar etapa no CI que deteta se alguma dependência foi atualizada sem intenção.

**Solução/Implementação:**
- Comparar as versões resolvidas com as versões esperadas no version catalog.
- Falhar o build se houver deriva não autorizada.
- Testar: alterar uma versão no build, verificar se CI falha.

#### INN-SET-04 — Sistema de perfis de desempenho por dispositivo

**Descrição:** Criar perfis de desempenho pré-configurados para diferentes tipos de boxes Android TV (baixo, médio, alto desempenho).

**Solução/Implementação:**
- Detetar capacidades do dispositivo (RAM, CPU, GPU) no arranque.
- Aplicar perfil de desempenho adequado (ex: reduzir qualidade de pré-visualizações em boxes de 2GB RAM).
- Permitir substituição manual nas definições.
- Testar: dispositivos com diferentes especificações, verificar se o perfil é aplicado corretamente.

#### INN-SET-05 — Integração com sistema de relatórios de erro remoto

**Descrição:** Enviar relatórios de erro e diagnóstico para um servidor remoto (com consentimento do utilizador).

**Solução/Implementação:**
- Adicionar opção nas definições para enviar relatórios de erro automaticamente.
- Incluir logs de diagnóstico, versões de dependências, informações do dispositivo.
- Encriptar dados sensíveis antes do envio.
- Testar: ativar envio de relatórios, verificar se são enviados corretamente.

---

## 13. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das sete rondas (primeira, segunda, terceira, quarta, quinta, sexta e sétima). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–12 acima.

### 13.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER, QUA, QUIN, SEX, SET)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |
| BUG-QUIN-01 | P2 | Parar reprodução em onStop() independente do estado da DB | Secção 11.1 |
| BUG-QUIN-02 | P2 | Logar exceções em appScope da OwnTVApp | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-04 | P3 | Unificar caminho de ativação no ChannelPressInput | Secção 11.1 |
| BUG-QUIN-05 | P3 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.1 |
| BUG-QUIN-06 | P3 | Logging e fallback de foco no FocusTrap | Secção 11.1 |
| BUG-QUIN-07 | P3 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.1 |
| BUG-QUIN-08 | P3 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.1 |
| BUG-QUIN-09 | P3 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.1 |
| BUG-QUIN-10 | P3 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.1 |
| BUG-SEX-01 | P3 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.1 |
| BUG-SEX-02 | P3 | Alargar janela de reinício no LiveRestart | Secção 11.1 |
| BUG-SEX-03 | P3 | Cooldown adaptativo no FrameRateController | Secção 11.1 |
| BUG-SEX-04 | P3 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.1 |
| BUG-SEX-05 | P3 | Indicador de sync na TopBar | Secção 11.1 |
| BUG-SEX-06 | P3 | Indicador de perfil infantil na Sidebar | Secção 11.1 |
| BUG-SEX-07 | P3 | Componente ChannelPressIndicator reutilizável | Secção 11.1 |
| BUG-SEX-08 | P3 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.1 |
| BUG-SEX-09 | P3 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.1 |
| BUG-SEX-10 | P3 | Aviso de gravação ativa no ExitDialog | Secção 11.1 |
| BUG-SET-01 | P2 | Usar vinculações nomeadas para LiveViewModel no AppModule | Secção 12.1 |
| BUG-SET-02 | P2 | Usar vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.1 |
| BUG-SET-03 | P3 | Alterar escopo de BackupAppSettings para factory ou Application Context | Secção 12.1 |
| BUG-SET-04 | P3 | CI testar variantes release e x86_64 | Secção 12.1 |
| BUG-SET-05 | P3 | snapshot_core.py usar timestamps reais | Secção 12.1 |
| BUG-SET-06 | P3 | UiWorkWindow.take() não deve limpar dados antes de leitura | Secção 12.1 |
| BUG-SET-07 | P3 | Validar refresh rate no UiPerformanceMonitor | Secção 12.1 |
| BUG-SET-08 | P3 | i18n.yml cobrir variantes benchmark e non-minified | Secção 12.1 |
| BUG-SET-09 | P2 | Adicionar version catalog (libs.versions.toml) | Secção 12.1 |
| BUG-SET-10 | P3 | Evitar loop infinito de recreate() no LocalizedContent | Secção 12.1 |

### 13.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção "backup antes de atualização" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |
| MEL-QUIN-01 | Separar paragem de playback do estado da base de dados | Secção 11.2 |
| MEL-QUIN-02 | Logging explícito de exceções em appScope | Secção 11.2 |
| MEL-QUIN-03 | Feedback de erro de migração no ShellViewModel | Secção 11.2 |
| MEL-QUIN-04 | Unificar caminho de ativação no ChannelPressInput | Secção 11.2 |
| MEL-QUIN-05 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.2 |
| MEL-QUIN-06 | Logging e fallback de foco no FocusTrap | Secção 11.2 |
| MEL-QUIN-07 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.2 |
| MEL-QUIN-08 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.2 |
| MEL-QUIN-09 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.2 |
| MEL-QUIN-10 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.2 |
| MEL-SEX-01 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.2 |
| MEL-SEX-02 | Alargar janela de reinício no LiveRestart | Secção 11.2 |
| MEL-SEX-03 | Cooldown adaptativo no FrameRateController | Secção 11.2 |
| MEL-SEX-04 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.2 |
| MEL-SEX-05 | Indicador de sync na TopBar | Secção 11.2 |
| MEL-SEX-06 | Indicador de perfil infantil na Sidebar | Secção 11.2 |
| MEL-SEX-07 | Componente ChannelPressIndicator reutilizável | Secção 11.2 |
| MEL-SEX-08 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.2 |
| MEL-SEX-09 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.2 |
| MEL-SEX-10 | Aviso de gravação ativa no ExitDialog | Secção 11.2 |
| MEL-SET-01 | Vinculações nomeadas para LiveViewModel no AppModule | Secção 12.2 |
| MEL-SET-02 | Vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.2 |
| MEL-SET-03 | CI testar variantes release e x86_64 | Secção 12.2 |
| MEL-SET-04 | snapshot_core.py usar timestamps reais | Secção 12.2 |
| MEL-SET-05 | Método peek() não destrutivo no UiWorkWindow | Secção 12.2 |
| MEL-SET-06 | Validar refresh rate no UiPerformanceMonitor | Secção 12.2 |
| MEL-SET-07 | Estender pseudolocale para variantes benchmark e non-minified | Secção 12.2 |
| MEL-SET-08 | Adicionar version catalog (libs.versions.toml) | Secção 12.2 |
| MEL-SET-09 | Guard contra loop infinito de recreate() no LocalizedContent | Secção 12.2 |

### 13.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA, QUIN, SEX, SET)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo "Convidado" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com "Watch Next" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo "Espelho de Ecrã" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo "Cinema" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo "Crianças" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo "Economia de Dados" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo "Família" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| INN-QUIN-01 | Modo "Resumo de Sessão" automático no encerramento | Secção 11.3 |
| INN-QUIN-02 | "Continue Watching" nativo do Android TV launcher | Secção 11.3 |
| INN-QUIN-03 | Modo "Espelho Multi-Box" para sincronizar reprodução | Secção 11.3 |
| INN-QUIN-04 | Controlo de qualidade de imagem por canal (perfil de codec) | Secção 11.3 |
| INN-QUIN-05 | Modo "Ecrã Negro com Áudio" para canais de rádio | Secção 11.3 |
| INN-SEX-01 | Modo "Economia de Bateria" para boxes portáteis | Secção 11.3 |
| INN-SEX-02 | Atalhos personalizados no launcher Android TV | Secção 11.3 |
| INN-SEX-03 | Modo "Visita" para casas de férias/aluguer | Secção 11.3 |
| INN-SEX-04 | Relatórios de uso para pais (controlo parental avançado) | Secção 11.3 |
| INN-SEX-05 | Integração com iluminação ambiente doméstica | Secção 11.3 |
| INN-SET-01 | Dashboard de dependências e build | Secção 12.3 |
| INN-SET-02 | Modo de diagnóstico de build para suporte | Secção 12.3 |
| INN-SET-03 | CI com deteção de deriva de dependências | Secção 12.3 |
| INN-SET-04 | Sistema de perfis de desempenho por dispositivo | Secção 12.3 |
| INN-SET-05 | Integração com sistema de relatórios de erro remoto | Secção 12.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 13.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 14. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02, BUG-QUIN-01, BUG-QUIN-02, BUG-QUIN-03, BUG-SET-01, BUG-SET-02, BUG-SET-09

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação; onStop para reprodução independentemente da DB; exceções de appScope são logadas; migrações de ShellViewModel têm feedback de erro; AppModule e PlayerModule usam vinculações nomeadas; version catalog adicionado.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10, BUG-QUIN-04 a BUG-QUIN-10, BUG-SEX-01 a BUG-SEX-10, BUG-SET-03 a BUG-SET-08, BUG-SET-10

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções; ChannelPressInput sem ativação duplicada; DatabaseStartupViewModel retry limitado; FocusTrap com logging/fallback; BulkRenameDialogs valida returnFocus; AddSourceChooser foco padrão; MainActivity limpa pendingDeepLink; StreamInfoOverlay estado de carregamento; PlayerHud restaura foco após diálogos; LiveRestart alarga janela; FrameRateController cooldown adaptativo; SyncStatusPill contagem por tipo; TopBar indicador de sync; Sidebar indicador Kids; ChannelPressIndicator componente; IncompleteRestoreDialog lista ignorados; ArchivePlaybackConfigDialog validação; ExitDialog aviso gravação; BackupAppSettings escopo revisto; CI testa release e x86_64; snapshot_core.py timestamps reais; UiWorkWindow peek() não destrutivo; UiPerformanceMonitor valida refresh rate; i18n.yml cobre todas variantes; LocalizedContent guard contra loop recreate().

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05, MEL-QUIN-01 a MEL-QUIN-10, MEL-SEX-01 a MEL-SEX-10, MEL-SET-01 a MEL-SET-09

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível; paragem de playback independente da DB; logging de exceções appScope; feedback de migração ShellViewModel; ativação unificada ChannelPressInput; retry limitado DatabaseStartupViewModel; logging/fallback FocusTrap; validação returnFocus BulkRenameDialogs; foco padrão AddSourceChooser; limpeza pendingDeepLink MainActivity; estado carregamento StreamInfoOverlay; foco restaurado PlayerHud; janela reinício LiveRestart; cooldown adaptativo FrameRateController; contagem por tipo SyncStatusPill; indicador sync TopBar; indicador Kids Sidebar; ChannelPressIndicator componente; lista ignorados IncompleteRestoreDialog; validação ArchivePlaybackConfigDialog; aviso gravação ExitDialog; vinculações nomeadas AppModule/PlayerModule; CI variantes adicionais; snapshot timestamps reais; peek() UiWorkWindow; validação refresh rate; pseudolocale todas variantes; version catalog; guard recreate() LocalizedContent.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, INN-QUIN-01 a INN-QUIN-05, INN-SEX-01 a INN-SEX-05, INN-SET-01 a INN-SET-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 15. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
   - Colocar app em background com DB corrompida (verificar se playback para)
   - Pressionar OK curto/longo em canal (verificar ativação única)
   - Clicar retry na DB startup múltiplas vezes (verificar limite de 3)
   - Abrir/fechar diálogos do PlayerHud (verificar restauração de foco)
   - Mudar canal rapidamente com taxas diferentes (verificar AFR)
   - Iniciar múltiplos syncs (verificar contagem oculta no pill)
   - Iniciar gravação e tentar sair (verificar aviso no ExitDialog)
   - Mudar de família de script (verificar ausência de loop recreate)
   - Verificar tipos de dependências injetadas (LiveViewModel, OwnTVPlayer)

5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).


---

## 13. Oitava Ronda de Análise — Novos Bugs, Melhorias e Inovações (25/10/2026)

Na oitava ronda, os mesmos quatro agentes especializados reinspecionaram áreas críticas adicionais: Build/CI (build.gradle.kts, settings.gradle.kts, android.yml, i18n.yml), ecrãs de settings (DnsSettingsScreen, NetworkSettingsScreen, RecordingSettingsScreen, LanguageSettingsScreen, MetadataSettingsScreen, ChNavSettingsScreen), gravações (RecordingOpenSelection), e ficheiros de construção. O Council consolidou os novos achados.

### 13.1 Novos Bugs Encontrados (com soluções)

#### BUG-OIT-01 — Build: caminho de propriedades de assinatura hardcoded no Windows

**Descrição:** `build.gradle.kts:40–43` carrega propriedades de assinatura de `E:/MEGA/CODE/OwnTV_Gradle/owntv-signing.properties`, um caminho absoluto do Windows que não existe em CI, Linux, macOS ou noutra máquina de desenvolvedor. O build continua sem assinatura, mas a intenção era assinar release builds locais.

**Localização:** `build.gradle.kts:40–43`

**Solução:**
- Remover o caminho hardcoded; confiar apenas nas fontes padrão (env var, Gradle properties, ficheiro user-wide `~/.gradle/gradle.properties`).
- Adicionar aviso claro no README sobre como configurar credenciais de assinatura.
- Testar: build numa máquina sem o caminho `E:/`, verificar que não falha.

#### BUG-OIT-02 — Build: release cai para debug signing se keystore não configurado

**Descrição:** `build.gradle.kts:175–179` — se `releaseKeystore` for null, o build de release usa `signingConfigs.getByName("debug")`, ou seja, o APK de release fica assinado com a chave de debug. Isto impede instalação sobre builds publicados e não é aceito por lojas de aplicações.

**Localização:** `build.gradle.kts:175–179`

**Solução:**
- Se não houver keystore de release configurado, falhar o build de release com mensagem clara em vez de silenciosamente usar debug.
- Ou manter release não assinado (sem `signingConfig`) para permitir instalação local com `adb install -r`.
- Testar: build de release sem keystore, verificar que falha com mensagem clara ou produz APK não assinado intencionalmente.

#### BUG-OIT-03 — Build: universal APK contradiz estratégia de split por ABI

**Descrição:** `build.gradle.kts:115` define `isUniversalApk = true` nos splits de ABI, o que produz um APK universal com todas as ABIs, contradizendo o objetivo declarado nas linhas 89–94 de reduzir o tamanho do download para ~49MB em vez de 104MB.

**Localização:** `build.gradle.kts:110–117`

**Solução:**
- Remover `isUniversalApk = true` para que cada flavour produza apenas o seu próprio ABI.
- Ou manter universal apenas para debug, desativar para release.
- Testar: build de release, verificar se são produzidos APKs separados por ABI.

#### BUG-OIT-04 — Build: lint desativa verificação de dependências

**Descrição:** `build.gradle.kts:206` define `checkDependencies = false`, o que significa que vulnerabilidades conhecidas em dependências não são detetadas pelo lint.

**Localização:** `build.gradle.kts:206`

**Solução:**
- Ativar `checkDependencies = true` ou adicionar uma etapa separada no CI para verificação de dependências (ex: OWASP Dependency-Check).
- Testar: introduzir uma dependência com vulnerabilidade conhecida, verificar se é detetada.

#### BUG-OIT-05 — DnsSettingsScreen: porta inválida cai silenciosamente para 53

**Descrição:** `DnsSettingsScreen.kt:93` usa `toIntOrNull() ?: 53`, ou seja, se o utilizador digitar uma porta inválida (ex: "abc", "99999"), o valor cai silenciosamente para 53 sem avisar o utilizador.

**Localização:** `DnsSettingsScreen.kt:93`

**Solução:**
- Validar o intervalo da porta (1–65535) e mostrar erro ao utilizador se inválida.
- Desativar o botão de guardar enquanto a porta for inválida.
- Testar: digitar porta inválida, verificar se aparece erro e botão desativado.

#### BUG-OIT-06 — NetworkSettingsScreen: porta 0 é inválida mas aceite

**Descrição:** `NetworkSettingsScreen.kt:126` filtra dígitos mas `port.trim().toIntOrNull() ?: 0` aceita porta 0, que é inválida para conexões TCP.

**Localização:** `NetworkSettingsScreen.kt:126, 86`

**Solução:**
- Validar que a porta está entre 1 e 65535.
- Mostrar erro se a porta for 0 ou maior que 65535.
- Testar: digitar porta 0, verificar se aparece erro.

#### BUG-OIT-07 — RecordingOpenSelection: requireCurrent lança CancellationException

**Descrição:** `RecordingOpenSelection.kt:38–40` lança `CancellationException("Superseded recording choice")` quando a seleção foi supersedida. Isto pode cancelar coroutines de chamadores que não esperam esta exceção.

**Localização:** `RecordingOpenSelection.kt:38–40`

**Solução:**
- Usar uma exceção de domínio próprio (ex: `RecordingSelectionSupersededException`) em vez de `CancellationException`.
- Ou retornar falso/booleano em vez de lançar exceção.
- Testar: navegação rápida entre gravações, verificar que não há cancelamentos inesperados.

#### BUG-OIT-08 — MetadataSettingsScreen: chave TMDB armazenada em texto simples

**Descrição:** `MetadataSettingsScreen.kt` armazena a chave da API TMDB no `editorConfig` sem encriptação. Embora seja uma chave do próprio utilizador, ela fica visível em DataStore e backups.

**Localização:** `MetadataSettingsScreen.kt` (editorConfig.tmdbApiKey)

**Solução:**
- Encriptar a chave TMDB antes de guardar (ex: usando AndroidKeyStore).
- Ou avisar o utilizador que a chave será armazenada localmente sem encriptação.
- Testar: guardar chave TMDB, verificar se está encriptada no DataStore.

#### BUG-OIT-09 — LanguageSettingsScreen: acesso à área de transferência (clipboard)

**Descrição:** `LanguageSettingsScreen.kt:6–10` importa `ClipboardManager` e `ClipData`, sugerindo acesso à área de transferência do sistema. Em Android TV, isto pode ser uma preocupação de privacidade e requer permissão especial em algumas versões.

**Localização:** `LanguageSettingsScreen.kt:6–10`

**Solução:**
- Remover acesso ao clipboard se não for estritamente necessário.
- Ou adicionar aviso claro ao utilizador sobre o acesso à área de transferência.
- Testar: verificar se o acesso ao clipboard é realmente necessário e justificado.

#### BUG-OIT-10 — ChNavSettingsScreen: não deteta duplicações de atalhos

**Descrição:** `ChNavSettingsScreen.kt:126` ordena as bindings mas não deteta se o mesmo botão/ação está atribuído duas vezes, o que pode causar comportamento imprevisível.

**Localização:** `ChNavSettingsScreen.kt:126`

**Solução:**
- Adicionar deteção de duplicações (mesmo keyCode + press) e mostrar aviso.
- Impedir guardar se houver conflitos.
- Testar: atribuir o mesmo botão a duas ações, verificar se aparece aviso.

### 13.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-OIT-01 — Build: documentar configuração de assinatura

**Descrição:** Adicionar instruções claras no README sobre como configurar credenciais de assinatura para builds de release locais.

**Solução:**
- Adicionar secção no README com exemplos de `~/.gradle/gradle.properties`.
- Explicar o comando `./gradlew :app:assembleStandardRelease`.
- Testar: seguir as instruções num clone novo, verificar se o build funciona.

#### MEL-OIT-02 — DnsSettingsScreen: validação de porta com feedback visual

**Descrição:** Adicionar validação em tempo real da porta DNS com ícone de erro/check.

**Solução:**
- Validar intervalo 1–65535 enquanto o utilizador digita.
- Desativar botão de guardar se inválido.
- Testar: digitar porta inválida, verificar feedback visual.

#### MEL-OIT-03 — NetworkSettingsScreen: validação de porta proxy

**Descrição:** Adicionar validação da porta proxy com feedback visual.

**Solução:**
- Validar intervalo 1–65535.
- Desativar botão de guardar se inválido.
- Testar: digitar porta inválida, verificar feedback visual.

#### MEL-OIT-04 — RecordingOpenSelection: exceção de domínio próprio

**Descrição:** Substituir `CancellationException` por exceção de domínio próprio.

**Solução:**
- Criar `RecordingSelectionSupersededException`.
- Testar: navegação rápida, verificar que não há cancelamentos inesperados.

#### MEL-OIT-05 — MetadataSettingsScreen: encriptar chave TMDB

**Descrição:** Encriptar a chave TMDB antes de guardar no DataStore.

**Solução:**
- Usar AndroidKeyStore para encriptar a chave.
- Testar: guardar chave, verificar se está encriptada.

#### MEL-OIT-06 — LanguageSettingsScreen: revisar necessidade de acesso ao clipboard

**Descrição:** Verificar se o acesso ao clipboard é necessário.

**Solução:**
- Remover se não for necessário, ou adicionar aviso ao utilizador.
- Testar: verificar se a funcionalidade funciona sem acesso ao clipboard.

#### MEL-OIT-06 — ChNavSettingsScreen: deteção de atalhos duplicados

**Descrição:** Adicionar deteção de conflitos de atalhos remotos.

**Solução:**
- Verificar duplicações antes de guardar.
- Mostrar aviso se houver conflitos.
- Testar: atribuir mesmo botão a duas ações, verificar aviso.

#### MEL-OIT-07 — Build: ativar verificação de dependências no lint

**Descrição:** Ativar `checkDependencies = true` ou adicionar etapa de verificação de dependências no CI.

**Solução:**
- Alterar `checkDependencies = false` para `true` ou adicionar ferramenta de verificação.
- Testar: introduzir dependência vulnerável, verificar deteção.

#### MEL-OIT-08 — Build: separar universal APK apenas para debug

**Descrição:** Manter universal APK apenas para variantes de debug.

**Solução:**
- Definir `isUniversalApk` baseado no build type.
- Testar: build de release, verificar se não há universal APK.

### 13.3 Novas Inovações (com descrição/solução)

#### INN-OIT-01 — Modo "Build Info" para suporte técnico

**Descrição:** Ecrã nas definições que mostra informações completas de build (versões, variantes, ABI, flags de compilação, keystore usado).

**Solução/Implementação:**
- Ler informações do `BuildConfig` e metadados do build.
- Exibir em ecrã dedicado nas definições → Avançado → Informações de Build.
- Permitir exportar como ficheiro de texto para suporte.
- Testar: abrir ecrã, verificar se todas as informações estão corretas.

#### INN-OIT-02 — Validação automática de configuração de rede no arranque

**Descrição:** Validar configurações de proxy e DNS no arranque da app e avisar o utilizador se algo estiver incorreto.

**Solução/Implementação:**
- No arranque, testar conectividade com proxy e DNS configurados.
- Mostrar aviso discreto se a configuração de rede estiver quebrada.
- Permitir ignorar ou corrigir diretamente.
- Testar: configurar proxy inválido, reiniciar app, verificar se aparece aviso.

#### INN-OIT-03 — Sistema de perfis de build por tipo de dispositivo

**Descrição:** Criar perfis de build otimizados para diferentes tipos de boxes Android TV (Fire TV, Xiaomi, Nvidia Shield, genérica).

**Solução/Implementação:**
- Detetar modelo do dispositivo no arranque.
- Aplicar configurações otimizadas (qualidade de imagem, tamanho de cache, número de threads).
- Permitir substituição manual nas definições.
- Testar: diferentes dispositivos, verificar se o perfil é aplicado corretamente.

#### INN-OIT-04 — Integração com sistema de atualização remota (OTA)

**Descrição:** Permitir atualizações OTA personalizadas para installations privadas da OwnTV.

**Solução/Implementação:**
- Adicionar endpoint de atualização configurável nas definições.
- Verificar automaticamente se há nova versão disponível.
- Fazer download e instalar silenciosamente ou com confirmação do utilizador.
- Testar: configurar endpoint de atualização, verificar se novas versões são detetadas.

#### INN-OIT-05 — Modo "Kiosk" para instalações públicas

**Descrição:** Modo restrito para instalações em locais públicos (hotéis, bares, lojas) onde o utilizador não deve aceder a todas as definições.

**Solução/Implementação:**
- Adicionar modo Kiosk nas definições de administrador.
- Bloquear acesso a definições de rede, backup, backup automático, atualizações.
- Permitir apenas navegação de canais e reprodução.
- Exigir PIN de administrador para sair do modo Kiosk.
- Testar: ativar modo Kiosk, verificar se definições sensíveis estão bloqueadas.

---

## 14. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das oito rondas (primeira à oitava). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–13 acima.

### 14.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER, QUA, QUIN, SEX, SET, OIT)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |
| BUG-QUIN-01 | P2 | Parar reprodução em onStop() independente do estado da DB | Secção 11.1 |
| BUG-QUIN-02 | P2 | Logar exceções em appScope da OwnTVApp | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-04 | P3 | Unificar caminho de ativação no ChannelPressInput | Secção 11.1 |
| BUG-QUIN-05 | P3 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.1 |
| BUG-QUIN-06 | P3 | Logging e fallback de foco no FocusTrap | Secção 11.1 |
| BUG-QUIN-07 | P3 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.1 |
| BUG-QUIN-08 | P3 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.1 |
| BUG-QUIN-09 | P3 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.1 |
| BUG-QUIN-10 | P3 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.1 |
| BUG-SEX-01 | P3 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.1 |
| BUG-SEX-02 | P3 | Alargar janela de reinício no LiveRestart | Secção 11.1 |
| BUG-SEX-03 | P3 | Cooldown adaptativo no FrameRateController | Secção 11.1 |
| BUG-SEX-04 | P3 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.1 |
| BUG-SEX-05 | P3 | Indicador de sync na TopBar | Secção 11.1 |
| BUG-SEX-06 | P3 | Indicador de perfil infantil na Sidebar | Secção 11.1 |
| BUG-SEX-07 | P3 | Componente ChannelPressIndicator reutilizável | Secção 11.1 |
| BUG-SEX-08 | P3 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.1 |
| BUG-SEX-09 | P3 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.1 |
| BUG-SEX-10 | P3 | Aviso de gravação ativa no ExitDialog | Secção 11.1 |
| BUG-SET-01 | P2 | Usar vinculações nomeadas para LiveViewModel no AppModule | Secção 12.1 |
| BUG-SET-02 | P2 | Usar vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.1 |
| BUG-SET-03 | P3 | Alterar escopo de BackupAppSettings para factory ou Application Context | Secção 12.1 |
| BUG-SET-04 | P3 | CI testar variantes release e x86_64 | Secção 12.1 |
| BUG-SET-05 | P3 | snapshot_core.py usar timestamps reais | Secção 12.1 |
| BUG-SET-06 | P3 | UiWorkWindow.take() não deve limpar dados antes de leitura | Secção 12.1 |
| BUG-SET-07 | P3 | Validar refresh rate no UiPerformanceMonitor | Secção 12.1 |
| BUG-SET-08 | P3 | i18n.yml cobrir variantes benchmark e non-minified | Secção 12.1 |
| BUG-SET-09 | P2 | Adicionar version catalog (libs.versions.toml) | Secção 12.1 |
| BUG-SET-10 | P3 | Evitar loop infinito de recreate() no LocalizedContent | Secção 12.1 |
| BUG-OIT-01 | P2 | Remover caminho hardcoded de propriedades de assinatura | Secção 13.1 |
| BUG-OIT-02 | P2 | Falhar build de release se não houver keystore de release | Secção 13.1 |
| BUG-OIT-03 | P3 | Remover universal APK ou limitar a debug | Secção 13.1 |
| BUG-OIT-04 | P3 | Ativar verificação de dependências no lint | Secção 13.1 |
| BUG-OIT-05 | P3 | Validar porta DNS com feedback ao utilizador | Secção 13.1 |
| BUG-OIT-06 | P3 | Validar porta proxy (não permitir 0) | Secção 13.1 |
| BUG-OIT-07 | P3 | Usar exceção de domínio próprio no RecordingOpenSelection | Secção 13.1 |
| BUG-OIT-08 | P3 | Encriptar chave TMDB antes de guardar | Secção 13.1 |
| BUG-OIT-09 | P3 | Revisar necessidade de acesso ao clipboard | Secção 13.1 |
| BUG-OIT-10 | P3 | Detetar atalhos remotos duplicados | Secção 13.1 |

### 14.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET, MEL-OIT)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção "backup antes de atualização" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |
| MEL-QUIN-01 | Separar paragem de playback do estado da base de dados | Secção 11.2 |
| MEL-QUIN-02 | Logging explícito de exceções em appScope | Secção 11.2 |
| MEL-QUIN-03 | Feedback de erro de migração no ShellViewModel | Secção 11.2 |
| MEL-QUIN-04 | Unificar caminho de ativação no ChannelPressInput | Secção 11.2 |
| MEL-QUIN-05 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.2 |
| MEL-QUIN-06 | Logging e fallback de foco no FocusTrap | Secção 11.2 |
| MEL-QUIN-07 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.2 |
| MEL-QUIN-08 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.2 |
| MEL-QUIN-09 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.2 |
| MEL-QUIN-10 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.2 |
| MEL-SEX-01 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.2 |
| MEL-SEX-02 | Alargar janela de reinício no LiveRestart | Secção 11.2 |
| MEL-SEX-03 | Cooldown adaptativo no FrameRateController | Secção 11.2 |
| MEL-SEX-04 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.2 |
| MEL-SEX-05 | Indicador de sync na TopBar | Secção 11.2 |
| MEL-SEX-06 | Indicador de perfil infantil na Sidebar | Secção 11.2 |
| MEL-SEX-07 | Componente ChannelPressIndicator reutilizável | Secção 11.2 |
| MEL-SEX-08 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.2 |
| MEL-SEX-09 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.2 |
| MEL-SEX-10 | Aviso de gravação ativa no ExitDialog | Secção 11.2 |
| MEL-SET-01 | Vinculações nomeadas para LiveViewModel no AppModule | Secção 12.2 |
| MEL-SET-02 | Vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.2 |
| MEL-SET-03 | CI testar variantes release e x86_64 | Secção 12.2 |
| MEL-SET-04 | snapshot_core.py usar timestamps reais | Secção 12.2 |
| MEL-SET-05 | Método peek() não destrutivo no UiWorkWindow | Secção 12.2 |
| MEL-SET-06 | Validar refresh rate no UiPerformanceMonitor | Secção 12.2 |
| MEL-SET-07 | Estender pseudolocale para variantes benchmark e non-minified | Secção 12.2 |
| MEL-SET-08 | Adicionar version catalog (libs.versions.toml) | Secção 12.2 |
| MEL-SET-09 | Guard contra loop infinito de recreate() no LocalizedContent | Secção 12.2 |
| MEL-OIT-01 | Documentar configuração de assinatura no README | Secção 13.2 |
| MEL-OIT-02 | Validação de porta DNS com feedback visual | Secção 13.2 |
| MEL-OIT-03 | Validação de porta proxy com feedback visual | Secção 13.2 |
| MEL-OIT-04 | Exceção de domínio próprio no RecordingOpenSelection | Secção 13.2 |
| MEL-OIT-05 | Encriptar chave TMDB antes de guardar | Secção 13.2 |
| MEL-OIT-06 | Revisar necessidade de acesso ao clipboard | Secção 13.2 |
| MEL-OIT-07 | Ativar verificação de dependências no lint | Secção 13.2 |
| MEL-OIT-08 | Universal APK apenas para debug | Secção 13.2 |

### 14.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA, QUIN, SEX, SET, OIT)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo "Convidado" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com "Watch Next" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo "Espelho de Ecrã" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo "Cinema" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo "Crianças" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo "Economia de Dados" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo "Família" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| INN-QUIN-01 | Modo "Resumo de Sessão" automático no encerramento | Secção 11.3 |
| INN-QUIN-02 | "Continue Watching" nativo do Android TV launcher | Secção 11.3 |
| INN-QUIN-03 | Modo "Espelho Multi-Box" para sincronizar reprodução | Secção 11.3 |
| INN-QUIN-04 | Controlo de qualidade de imagem por canal (perfil de codec) | Secção 11.3 |
| INN-QUIN-05 | Modo "Ecrã Negro com Áudio" para canais de rádio | Secção 11.3 |
| INN-SEX-01 | Modo "Economia de Bateria" para boxes portáteis | Secção 11.3 |
| INN-SEX-02 | Atalhos personalizados no launcher Android TV | Secção 11.3 |
| INN-SEX-03 | Modo "Visita" para casas de férias/aluguer | Secção 11.3 |
| INN-SEX-04 | Relatórios de uso para pais (controlo parental avançado) | Secção 11.3 |
| INN-SEX-05 | Integração com iluminação ambiente doméstica | Secção 11.3 |
| INN-SET-01 | Dashboard de dependências e build | Secção 12.3 |
| INN-SET-02 | Modo de diagnóstico de build para suporte | Secção 12.3 |
| INN-SET-03 | CI com deteção de deriva de dependências | Secção 12.3 |
| INN-SET-04 | Sistema de perfis de desempenho por dispositivo | Secção 12.3 |
| INN-SET-05 | Integração com sistema de relatórios de erro remoto | Secção 12.3 |
| INN-OIT-01 | Modo "Build Info" para suporte técnico | Secção 13.3 |
| INN-OIT-02 | Validação automática de configuração de rede no arranque | Secção 13.3 |
| INN-OIT-03 | Sistema de perfis de build por tipo de dispositivo | Secção 13.3 |
| INN-OIT-04 | Integração com sistema de atualização remota (OTA) | Secção 13.3 |
| INN-OIT-05 | Modo "Kiosk" para instalações públicas | Secção 13.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 14.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 15. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02, BUG-QUIN-01, BUG-QUIN-02, BUG-QUIN-03, BUG-SET-01, BUG-SET-02, BUG-SET-09, BUG-OIT-01, BUG-OIT-02

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação; onStop para reprodução independentemente da DB; exceções de appScope são logadas; migrações de ShellViewModel têm feedback de erro; AppModule e PlayerModule usam vinculações nomeadas; version catalog adicionado; caminho de assinatura hardcoded removido; release falha sem keystore.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10, BUG-QUIN-04 a BUG-QUIN-10, BUG-SEX-01 a BUG-SEX-10, BUG-SET-03 a BUG-SET-08, BUG-SET-10, BUG-OIT-03 a BUG-OIT-10

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções; ChannelPressInput sem ativação duplicada; DatabaseStartupViewModel retry limitado; FocusTrap com logging/fallback; BulkRenameDialogs valida returnFocus; AddSourceChooser foco padrão; MainActivity limpa pendingDeepLink; StreamInfoOverlay estado de carregamento; PlayerHud restaura foco após diálogos; LiveRestart alarga janela; FrameRateController cooldown adaptativo; SyncStatusPill contagem por tipo; TopBar indicador de sync; Sidebar indicador Kids; ChannelPressIndicator componente; IncompleteRestoreDialog lista ignorados; ArchivePlaybackConfigDialog validação; ExitDialog aviso gravação; BackupAppSettings escopo revisto; CI testa release e x86_64; snapshot_core.py timestamps reais; UiWorkWindow peek() não destrutivo; UiPerformanceMonitor valida refresh rate; i18n.yml cobre todas variantes; LocalizedContent guard contra loop recreate(); Build: caminho de assinatura não hardcoded; release falha sem keystore; universal APK só para debug; lint verifica dependências; porta DNS validada; porta proxy validada; RecordingOpenSelection exceção de domínio; chave TMDB encriptada; acesso ao clipboard revisado; atalhos remotos sem duplicações.

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET, MEL-OIT)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05, MEL-QUIN-01 a MEL-QUIN-10, MEL-SEX-01 a MEL-SEX-10, MEL-SET-01 a MEL-SET-09, MEL-OIT-01 a MEL-OIT-08

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível; paragem de playback independente da DB; logging de exceções appScope; feedback de migração ShellViewModel; ativação unificada ChannelPressInput; retry limitado DatabaseStartupViewModel; logging/fallback FocusTrap; validação returnFocus BulkRenameDialogs; foco padrão AddSourceChooser; limpeza pendingDeepLink MainActivity; estado carregamento StreamInfoOverlay; foco restaurado PlayerHud; janela reinício LiveRestart; cooldown adaptativo FrameRateController; contagem por tipo SyncStatusPill; indicador sync TopBar; indicador Kids Sidebar; ChannelPressIndicator componente; lista ignorados IncompleteRestoreDialog; validação ArchivePlaybackConfigDialog; aviso gravação ExitDialog; vinculações nomeadas AppModule/PlayerModule; CI variantes adicionais; snapshot timestamps reais; peek() UiWorkWindow; validação refresh rate; pseudolocale todas variantes; version catalog; guard recreate() LocalizedContent; documentação assinatura README; validação porta DNS; validação porta proxy; exceção domínio RecordingOpenSelection; chave TMDB encriptada; verificação dependências lint; universal APK só debug.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, INN-QUIN-01 a INN-QUIN-05, INN-SEX-01 a INN-SEX-05, INN-SET-01 a INN-SET-05, INN-OIT-01 a INN-OIT-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 16. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
   - Colocar app em background com DB corrompida (verificar se playback para)
   - Pressionar OK curto/longo em canal (verificar ativação única)
   - Clicar retry na DB startup múltiplas vezes (verificar limite de 3)
   - Abrir/fechar diálogos do PlayerHud (verificar restauração de foco)
   - Mudar canal rapidamente com taxas diferentes (verificar AFR)
   - Iniciar múltiplos syncs (verificar contagem oculta no pill)
   - Iniciar gravação e tentar sair (verificar aviso no ExitDialog)
   - Mudar de família de script (verificar ausência de loop recreate)
   - Verificar tipos de dependências injetadas (LiveViewModel, OwnTVPlayer)
   - Build de release sem keystore (verificar que falha ou produz APK não assinado)
   - Digitar porta DNS inválida (verificar erro e botão desativado)
   - Digitar porta proxy 0 (verificar erro)
   - Navegação rápida entre gravações (verificar não há cancelamentos inesperados)

5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).

---

## 19. Nona Ronda de Análise — Segurança, Build, Testes e Recursos (26/10/2026)

Na nona ronda, os quatro agentes especializados reinspecionaram áreas que ainda não tinham sido profundamente analisadas: AndroidManifest.xml (permissões, exportado, deep links, FileProvider), proguard-rules.pro (R8 keep rules, stripping de Logs), recursos XML (file_paths.xml, backup_rules.xml, auto_start_service.xml), ficheiros de teste (unitários e androidTest), e recursos de tema/cor/string. O Council consolidou os novos achados.

### 19.1 Novos Bugs Encontrados (com soluções)

#### BUG-NIN-01 — AndroidManifest: MainActivity exported=true com deep links sem validação de origem

**Descrição:** `AndroidManifest.xml:86` declara `android:exported="true"` na MainActivity com deep links `owntv://play` e `owntv://open`. Qualquer aplicação instalada pode enviar intents para estas ações sem restrição, e não há validação visível de origem (package caller) nem sanitização de parâmetros de entrada. Isto permite ataques de intent spoofing.

**Localização:** `AndroidManifest.xml:83–108`

**Solução:**
- Reduzir `android:exported` para `false` se não for necessário ser acessível por outras apps.
- Adicionar validação do `callingPackage` no `MainActivity.onNewIntent()` antes de processar deep links.
- Sanitizar parâmetros de deep link (host, path, query) para evitar injeção.
- Testar: enviar intent de outra app, verificar se é rejeitado ou validado.

#### BUG-NIN-02 — AndroidManifest: FileProvider com grantUriPermissions=true amplo

**Descrição:** `AndroidManifest.xml:61–69` declara um FileProvider com `android:grantUriPermissions="true"` e `file_paths.xml:8` expõe `<external-path name=\"shared_media\" path=\".\" />`, ou seja, a raiz inteira do armazenamento partilhado fica exposta via content URI.

**Localização:** `AndroidManifest.xml:61–69`, `app/src/main/res/xml/file_paths.xml:8`

**Solução:**
- Restringir o `file_paths.xml` a subpastas específicas (ex: `updates/`, `downloads/`) em vez da raiz `\".\"`.
- Definir `android:grantUriPermissions=\"false\"` e usar permissões granulares por URI quando necessário.
- Testar: tentar aceder a ficheiros fora das pastas permitidas via FileProvider, verificar que falha.

#### BUG-NIN-03 — proguard-rules.pro: stripping de Logs inconsistente (apenas v e d)

**Descrição:** `proguard-rules.pro:27–31` remove apenas `Log.v` e `Log.d` em release, mantendo `Log.e`, `Log.w` e `Log.i` que podem vazar informação sensível (stack traces, URLs, nomes de ficheiros) nos logs do dispositivo.

**Localização:** `app/proguard-rules.pro:27–31`

**Solução:**
- Estender o `-assumenosideeffects` para `Log.e`, `Log.w`, `Log.i` em builds de release.
- Ou substituir chamadas diretas a `Log.*` por uma classe `OwnTVLog` custom que seja no-op em release.
- Testar: build de release, verificar que não há Log.e/w/i no código.

#### BUG-NIN-04 — proguard-rules.pro: -keep enum para todo o pacote bloat o APK

**Descrição:** `proguard-rules.pro:24` declara `-keep enum tv.own.owntv.** { *; }`, mantendo todos os enums do pacote (incluindo os não serializados via DataStore/backup JSON), o que aumenta desnecessariamente o tamanho do APK.

**Localização:** `app/proguard-rules.pro:24`

**Solução:**
- Restringir o keep apenas aos enums que são serializados (ex: ZoomMode, ThemeMode, StartupMode, etc.).
- Ou usar `-keepclassmembers` apenas nos enums específicos necessários.
- Testar: build de release, comparar tamanho do APK antes e depois.

#### BUG-NIN-05 — file_paths.xml: shared_media expõe raiz do shared storage

**Descrição:** `file_paths.xml:8` expõe `<external-path name=\"shared_media\" path=\".\" />`, expor a raiz do armazenamento partilhado. Qualquer app que obtenha um content URI deste provider pode navegar por pastas sensíveis do dispositivo.

**Localização:** `app/src/main/res/xml/file_paths.xml:8`

**Solução:**
- Substituir `path=\".\"` por pastas específicas usadas pelo ExternalPlayerLauncher (ex: `downloads/movies/`, `downloads/series/`).
- Testar: verificar que apenas as pastas pretendidas são acessíveis via FileProvider.

#### BUG-NIN-06 — Ausência de testes de integração para backup/restore

**Descrição:** Não há testes de `androidTest` para o fluxo completo de backup e restauro (`BackupManager`, `BackupContainer`, encriptação com passphrase, omissão de segredos sem passphrase).

**Localização:** `app/src/androidTest/java/tv/own/owntv/` — nenhum teste de backup encontrado.

**Solução:**
- Adicionar testes de instrumentação que validem: backup com passphrase (encriptado), backup sem passphrase (segredos omitidos), restauro de backup válido, rejeição de backup corrompido.
- Testar: executar os testes num emulator/device, verificar todos os cenários.

#### BUG-NIN-07 — Testes não cobrem deep link handling

**Descrição:** Não há testes unitários para validar o tratamento de deep links `owntv://play` e `owntv://open` (sanitização de parâmetros, rejeição de origens não confiáveis).

**Localização:** `app/src/test/` — nenhum teste de deep link encontrado.

**Solução:**
- Adicionar testes unitários para `DeepLinkValidator` (ou equivalente) que verifiquem validação de origem e sanitização de parâmetros.
- Testar: simular intents de deep link malformados, verificar rejeição.

#### BUG-NIN-08 — auto_start_service.xml: accessibility service sem restrição de janela

**Descrição:** `auto_start_service.xml:7` define `android:canRetrieveWindowContent=\"false\"` (bom), mas o serviço de acessibilidade pode ser explorado para interceptar eventos de janela do sistema em dispositivos onde o utilizador o ative.

**Localização:** `app/src/main/res/xml/auto_start_service.xml`

**Solução:**
- Revisar se o serviço de acessibilidade é estritamente necessário.
- Adicionar `android:settingsActivity` com aviso claro sobre implicações de privacidade.
- Documentar no README por que o serviço é necessário e quais permissões exige.

### 19.2 Novas Melhorias de Código e UX (com soluções)

#### MEL-NIN-01 — Criar/verificar data_extraction_rules.xml para API 31+

**Descrição:** `backup_rules.xml:1` comenta que API 31+ usa `data_extraction_rules.xml`, mas não há confirmação de que exista um ficheiro correspondente com as mesmas exclusões.

**Solução:**
- Criar `app/src/main/res/xml/data_extraction_rules.xml` com as mesmas exclusões de `backup_rules.xml`.
- Testar: build para API 31+, verificar que o auto-backup continua desativado.

#### MEL-NIN-02 — Estender testes de foco para diálogos do PlayerHud

**Descrição:** `DialogFocusRestoreTest.kt` testa foco em diálogos genéricos, mas não cobre especificamente diálogos do `PlayerHud` durante reprodução ativa.

**Solução:**
- Adicionar testes de foco para `PlayerHud` dialogs (ex: `PlayerHudDialogs.kt`) durante playback.
- Testar: abrir/fechar diálogos do HUD enquanto o canal está a tocar, verificar restauração de foco.

#### MEL-NIN-03 — Wrapper de logging sanitizado para release

**Descrição:** Chamadas diretas a `Log.e/w/i/v/d` podem vazar dados sensíveis (API keys, senhas, MAC addresses) nos logs.

**Solução:**
- Criar classe `OwnTVLog` que filtre/redacte segredos antes de logar.
- Substituir chamadas diretas a `Log.*` por `OwnTVLog.*` em código de produção.
- Testar: procurar por `Log.` no código, verificar que todos usam o wrapper.

#### MEL-NIN-04 — Restringir keep rules do proguard a enums serializados

**Descrição:** Substituir `-keep enum tv.own.owntv.** { *; }` por keep rules específicas apenas para enums usados em DataStore/backup.

**Solução:**
- Listar os enums que precisam ser mantidos (ZoomMode, ThemeMode, StartupMode, AnimationLevel, ResumeMode, EpgAutoRefresh, PlaylistAutoRefresh, MediaType).
- Aplicar `-keep` apenas a esses.
- Testar: build de release, verificar que não há crash por enum otimizado.

#### MEL-NIN-05 — Adicionar testes de integração para backup/restore

**Descrição:** Criar suite de testes de instrumentação para o fluxo de backup.

**Solução:**
- Testes para backup com/sem passphrase, restauro, rejeição de ficheiros corrompidos.
- Testar: executar em device/emulator.

#### MEL-NIN-06 — Adicionar testes de deep link handling

**Descrição:** Criar testes unitários para validação de deep links.

**Solução:**
- Testar sanitização de parâmetros, rejeição de origens não confiáveis.
- Testar: executar testes unitários.

#### MEL-NIN-07 — Revisar permissões de FileProvider no AndroidManifest

**Descrição:** Revisar se `grantUriPermissions="true"` é necessário.

**Solução:**
- Definir como `false` se não for usado dinamicamente.
- Testar: verificar que FileProvider funciona sem grant automático.

### 19.3 Novas Inovações (com descrição/solução)

#### INN-NIN-01 — Modo de segurança \"isolado\" para deep links

**Descrição/Implementação:**
- `DeepLinkValidator` com allowlist de ações permitidas.
- Rejeitar intents de aplicações não assinadas pela mesma chave.
- Log de auditoria para todas as tentativas de deep link.
- Testar: enviar intent de app externa, verificar rejeição.

#### INN-NIN-02 — Auditoria de acesso a segredos

**Descrição/Implementação:**
- `SecretAccessLogger` que regista cada leitura de chave TMDB, proxy password, Stalker MAC.
- Armazenar no DataStore local com timestamp.
- Ecrã nas definições → Segurança → Histórico de acessos a segredos.
- Testar: aceder a uma chave API, verificar se aparece no histórico.

#### INN-NIN-03 — Modo \"Air Gap\" para instalações sensíveis

**Descrição/Implementação:**
- Definição que desativa toda a conectividade de rede exceto streaming de canais locais (LAN).
- Bloquear conexões HTTP/HTTPS para domínios externos.
- Permitir apenas tráfego UDP/TCP local para IPTV.
- Testar: ativar modo Air Gap, verificar que não há tráfego externo.

#### INN-NIN-04 — Validação automática de configuração ProGuard

**Descrição/Implementação:**
- Gradle task que executa o app em release num emulator e verifica crashes por missing keep rules.
- Relatório de classes refletivas não cobertas pelas keep rules.
- Integrar no CI para detetar regressões de R8.
- Testar: remover uma keep rule, verificar que a task deteta o problema.

#### INN-NIN-05 — Modo \"Segurança Avançada\" com bloqueio biométrico/PIN

**Descrição/Implementação:**
- Exigir PIN ou biometria para aceder a definições de rede, backup, claves API.
- Bloquear ecrã de definições sensíveis após timeout de inatividade.
- Integrar com Android BiometricPrompt.
- Testar: ativar bloqueio, tentar aceder a definições sem PIN, verificar rejeição.

---

## 20. Reavaliação Completa do Council — Todos os Itens com Soluções

O Council reavaliou **todos** os itens das nove rondas (primeira à nona). Cada item foi classificado como **VÁLIDO** (manter com solução) ou **REJEITADO** (não implementar na forma proposta). Para cada item válido, a solução está descrita nas secções 3–19 acima.

### 20.1 Itens Válidos — Bugs (P1, P2, P3, P4, P5, ADD, TER, QUA, QUIN, SEX, SET, OIT, NIN)

Todos os bugs abaixo são **confirmados como válidos** pelo Council, com soluções descritas:

| ID | Severidade | Solução Resumida | Onde está descrita |
|----|-----------|------------------|-------------------|
| BUG-P1-01 | P1 | Aceitar -1 e 0..100 na validação de SUB_BG_OPACITY | Secção 4.1 (A1) |
| BUG-P1-02 | P1 | Unificar redacção de segredos em HttpClient | Secção 4.1 (A2) |
| BUG-P1-03 | P1 | Aplicar política de elegibilidade após applyEpgMatches | Secção 4.1 (A3) |
| BUG-P1-04 | P1 | Usar apenas associações resolvidas no restauro seletivo | Secção 4.1 (A4) |
| BUG-P2-01 | P2 | Centralizar rearmar de watchdogs por estado/owner | Secção 4.2 (A5) |
| BUG-P2-02 | P2 | Usar preferência Live nas gravações | Secção 4.2 (A6) |
| BUG-P2-03 | P2 | Renovar fábrica HTTP por geração no HeroPreview | Secção 4.2 (A7) |
| BUG-P2-04 | P2 | Limpar pesquisa do guia só no background real | Secção 4.3 (A12) |
| BUG-P2-05 | P2 | Incluir nomes personalizados na pesquisa EPG | Secção 4.3 (A13) |
| BUG-P2-06 | P2 | Snapshot de perfil/fontes na edição EPG | Secção 4.3 (A14) |
| BUG-P2-07 | P2 | Matriz de autorização de perfis (PIN) | Secção 3.6 / Secção 4.1 |
| BUG-P3-01 | P3 | Modelo imutável de intervalos do guia | Secção 4.3 (A11) |
| BUG-P3-02 | P3 | Atualizar aviso de alarmes ao retomar ecrã | Secção 4.3 (A15) |
| BUG-P3-03 | P2 | Foco estável em overlays (Boolean return) | Secção 4.3 (A16) |
| BUG-P3-04 | P2 | Chave estável em CategoryRail | Secção 4.3 (A17) |
| BUG-P4-01 | P2 | Normalizar formatos de data/hora em TimeFormat | Secção 4.2 (A8) |
| BUG-P5-01 | P2 | Resolvedor normal do Android para leitor externo | Secção 4.2 (A9) |
| BUG-ADD-01 | P2 | Resolver pid de forma consistente no arranque | Secção 7.1 |
| BUG-ADD-02 | P2 | Mutex para evitar cargas concorrentes no Home | Secção 7.1 |
| BUG-ADD-03 | P2 | Mapear externalPlayer por MediaType no Search | Secção 7.1 |
| BUG-ADD-04 | P3 | Expor estados separados de externalPlayer nos downloads | Secção 7.1 |
| BUG-ADD-05 | P3 | Resetar restoreFocus após consumir | Secção 7.1 |
| BUG-ADD-06 | P3 | Invalidar zapSource na mudança de secção | Secção 7.1 |
| BUG-ADD-07 | P3 | Estender continueTarget para MOVIE/SERIES | Secção 7.1 |
| BUG-ADD-08 | P2 | Aguardar confirmação de libertação no releaseAll | Secção 7.1 |
| BUG-TER-01 | P2 | Isolar observedCode por owner em FailedChannelAttempts | Secção 8.1 |
| BUG-TER-02 | P2 | Ecrã de seleção no HUD para DirectTune Ambiguous | Secção 8.1 |
| BUG-TER-03 | P3 | Tempo máximo e cancelamento no LongPressMenuGuard | Secção 8.1 |
| BUG-TER-04 | P3 | Flag menuShown para bloquear PLAY após menu | Secção 8.1 |
| BUG-TER-05 | P3 | Substituir repeat(3) por focusRequester direto | Secção 8.1 |
| BUG-TER-06 | P3 | Proteger leituras de contentOrderDao com moveMutex | Secção 8.1 |
| BUG-TER-07 | P2 | Mover sync EPG para WorkManager/Scope independente | Secção 8.1 |
| BUG-TER-08 | P3 | Invalidar formToken ao sair do ecrã AddSource | Secção 8.1 |
| BUG-QUA-01 | P2 | Mover published=true para depois do renameTo no backup automático | Secção 9.1 |
| BUG-QUA-02 | P2 | Adicionar rotação e recuperação de chave AndroidKeyStore | Secção 9.1 |
| BUG-QUA-03 | P3 | Resetar pendingWake após cada tentativa de lançamento no AutoStartService | Secção 9.1 |
| BUG-QUA-04 | P3 | Limpar lembretes expirados independentemente do foreground | Secção 9.1 |
| BUG-QUA-05 | P3 | Substituir repeat(5) por LaunchedEffect com timeout no ProgrammeReminderDialog | Secção 9.1 |
| BUG-QUA-06 | P3 | Adicionar timestamp e forçar reload de janela do guia após inatividade | Secção 9.1 |
| BUG-QUA-07 | P3 | Definir dismissOnBackPress=true no StorageBrowser | Secção 9.1 |
| BUG-QUA-08 | P3 | Especificar qual feed de EPG falhou na mensagem de erro | Secção 9.1 |
| BUG-QUA-09 | P3 | Acumular backups recebidos numa lista no RemoteBackupRestoreScreen | Secção 9.1 |
| BUG-QUA-10 | P3 | Invalidar estado rememberSaveable ao mudar secção no SettingsScreen | Secção 9.1 |
| BUG-QUIN-01 | P2 | Parar reprodução em onStop() independente do estado da DB | Secção 11.1 |
| BUG-QUIN-02 | P2 | Logar exceções em appScope da OwnTVApp | Secção 11.1 |
| BUG-QUIN-03 | P2 | Feedback de erro de migração no ShellViewModel | Secção 11.1 |
| BUG-QUIN-04 | P3 | Unificar caminho de ativação no ChannelPressInput | Secção 11.1 |
| BUG-QUIN-05 | P3 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.1 |
| BUG-QUIN-06 | P3 | Logging e fallback de foco no FocusTrap | Secção 11.1 |
| BUG-QUIN-07 | P3 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.1 |
| BUG-QUIN-08 | P3 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.1 |
| BUG-QUIN-09 | P3 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.1 |
| BUG-QUIN-10 | P3 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.1 |
| BUG-SEX-01 | P3 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.1 |
| BUG-SEX-02 | P3 | Alargar janela de reinício no LiveRestart | Secção 11.1 |
| BUG-SEX-03 | P3 | Cooldown adaptativo no FrameRateController | Secção 11.1 |
| BUG-SEX-04 | P3 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.1 |
| BUG-SEX-05 | P3 | Indicador de sync na TopBar | Secção 11.1 |
| BUG-SEX-06 | P3 | Indicador de perfil infantil na Sidebar | Secção 11.1 |
| BUG-SEX-07 | P3 | Componente ChannelPressIndicator reutilizável | Secção 11.1 |
| BUG-SEX-08 | P3 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.1 |
| BUG-SEX-09 | P3 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.1 |
| BUG-SEX-10 | P3 | Aviso de gravação ativa no ExitDialog | Secção 11.1 |
| BUG-SET-01 | P2 | Usar vinculações nomeadas para LiveViewModel no AppModule | Secção 12.1 |
| BUG-SET-02 | P2 | Usar vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.1 |
| BUG-SET-03 | P3 | Alterar escopo de BackupAppSettings para factory ou Application Context | Secção 12.1 |
| BUG-SET-04 | P3 | CI testar variantes release e x86_64 | Secção 12.1 |
| BUG-SET-05 | P3 | snapshot_core.py usar timestamps reais | Secção 12.1 |
| BUG-SET-06 | P3 | UiWorkWindow.take() não deve limpar dados antes de leitura | Secção 12.1 |
| BUG-SET-07 | P3 | Validar refresh rate no UiPerformanceMonitor | Secção 12.1 |
| BUG-SET-08 | P3 | i18n.yml cobrir variantes benchmark e non-minified | Secção 12.1 |
| BUG-SET-09 | P2 | Adicionar version catalog (libs.versions.toml) | Secção 12.1 |
| BUG-SET-10 | P3 | Evitar loop infinito de recreate() no LocalizedContent | Secção 12.1 |
| BUG-OIT-01 | P2 | Remover caminho hardcoded de propriedades de assinatura | Secção 13.1 |
| BUG-OIT-02 | P2 | Falhar build de release se não houver keystore de release | Secção 13.1 |
| BUG-OIT-03 | P3 | Remover universal APK ou limitar a debug | Secção 13.1 |
| BUG-OIT-04 | P3 | Ativar verificação de dependências no lint | Secção 13.1 |
| BUG-OIT-05 | P3 | Validar porta DNS com feedback ao utilizador | Secção 13.1 |
| BUG-OIT-06 | P3 | Validar porta proxy (não permitir 0) | Secção 13.1 |
| BUG-OIT-07 | P3 | Usar exceção de domínio próprio no RecordingOpenSelection | Secção 13.1 |
| BUG-OIT-08 | P3 | Encriptar chave TMDB antes de guardar | Secção 13.1 |
| BUG-OIT-09 | P3 | Revisar necessidade de acesso ao clipboard | Secção 13.1 |
| BUG-OIT-10 | P3 | Detetar atalhos remotos duplicados | Secção 13.1 |
| BUG-NIN-01 | P2 | Reduzir exported=false e validar origem nos deep links | Secção 19.1 |
| BUG-NIN-02 | P2 | Restringir FileProvider a pastas específicas | Secção 19.1 |
| BUG-NIN-03 | P3 | Estender stripping de Logs para e/w/i em release | Secção 19.1 |
| BUG-NIN-04 | P3 | Restringir keep enum a enums serializados | Secção 19.1 |
| BUG-NIN-05 | P3 | Restringir shared_media a subpastas específicas | Secção 19.1 |
| BUG-NIN-06 | P3 | Adicionar testes de integração para backup/restore | Secção 19.1 |
| BUG-NIN-07 | P3 | Adicionar testes de deep link handling | Secção 19.1 |
| BUG-NIN-08 | P3 | Revisar necessidade do serviço de acessibilidade | Secção 19.1 |

### 20.2 Itens Válidos — Melhorias (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET, MEL-OIT, MEL-NIN)

Todas as melhorias são **confirmadas como válidas**, com soluções descritas:

| ID | Solução Resumida | Onde está descrita |
|----|------------------|-------------------|
| MEL-01 | Centralizar lógica de foco em componentes reutilizáveis | Secção 4.1 |
| MEL-02 | Empty states descritivos | Secção 4.1 |
| MEL-03 | Mensagens de erro de rede diferenciadas | Secção 4.1 |
| MEL-04 | Revisão de chunked() em consultas SQLite | Secção 4.1 |
| MEL-05 | Confirmação explícita para alterar PIN/remover proteção | Secção 4.2 |
| MEL-06 | Snapshot transacional (validar antes de escrever ZIP) | Secção 4.3 |
| MEL-07 | CI executar testes Core explicitamente | Secção 4.3 |
| MEL-08 | Converter bump automático em proposta de revisão | Secção 4.3 |
| MEL-09 | Fixture de desempenho compatível com defaults | Secção 4.3 |
| MEL-ADD-01 | Focar secção de origem após sair do player | Secção 7.2 |
| MEL-ADD-02 | Esqueleto só no cold start | Secção 7.2 |
| MEL-ADD-03 | Debounce ajustável por tipo de conteúdo | Secção 7.2 |
| MEL-ADD-04 | Indicador de progresso por download | Secção 7.2 |
| MEL-TER-01 | Indicador visual de tentativas falhadas de canal | Secção 8.2 |
| MEL-TER-02 | Notificação de atualização fora do ecrã Settings | Secção 8.2 |
| MEL-TER-03 | Busca rápida na lista de categorias Customize | Secção 8.2 |
| MEL-TER-04 | Validação de URL em tempo real no AddSource | Secção 8.2 |
| MEL-QUA-01 | Indicador de status do backup automático no ecrã principal | Secção 9.2 |
| MEL-QUA-02 | Opção \"backup antes de atualização\" no UpdateDialog | Secção 9.2 |
| MEL-QUA-03 | Busca rápida no StorageBrowser | Secção 9.2 |
| MEL-QUA-04 | Confirmação antes de limpar lembretes de EPG expirados | Secção 9.2 |
| MEL-QUA-05 | Indicador de progresso de sync EPG nas configurações de fontes | Secção 9.2 |
| MEL-QUIN-01 | Separar paragem de playback do estado da base de dados | Secção 11.2 |
| MEL-QUIN-02 | Logging explícito de exceções em appScope | Secção 11.2 |
| MEL-QUIN-03 | Feedback de erro de migração no ShellViewModel | Secção 11.2 |
| MEL-QUIN-04 | Unificar caminho de ativação no ChannelPressInput | Secção 11.2 |
| MEL-QUIN-05 | Limitar retentativas no DatabaseStartupViewModel | Secção 11.2 |
| MEL-QUIN-06 | Logging e fallback de foco no FocusTrap | Secção 11.2 |
| MEL-QUIN-07 | Validar returnFocus antes de restaurar no BulkRenameDialogs | Secção 11.2 |
| MEL-QUIN-08 | Usar LaunchedEffect padrão para foco no AddSourceChooser | Secção 11.2 |
| MEL-QUIN-09 | Limpar pendingDeepLink após consumo no MainActivity | Secção 11.2 |
| MEL-QUIN-10 | Estado de carregamento/sem dados no StreamInfoOverlay | Secção 11.2 |
| MEL-SEX-01 | Restaurar foco após fechar diálogos no PlayerHud | Secção 11.2 |
| MEL-SEX-02 | Alargar janela de reinício no LiveRestart | Secção 11.2 |
| MEL-SEX-03 | Cooldown adaptativo no FrameRateController | Secção 11.2 |
| MEL-SEX-04 | Contagem por tipo nas linhas ocultas do SyncStatusPill | Secção 11.2 |
| MEL-SEX-05 | Indicador de sync na TopBar | Secção 11.2 |
| MEL-SEX-06 | Indicador de perfil infantil na Sidebar | Secção 11.2 |
| MEL-SEX-07 | Componente ChannelPressIndicator reutilizável | Secção 11.2 |
| MEL-SEX-08 | Lista de itens ignorados no IncompleteRestoreDialog | Secção 11.2 |
| MEL-SEX-09 | Validação de configuração no ArchivePlaybackConfigDialog | Secção 11.2 |
| MEL-SEX-10 | Aviso de gravação ativa no ExitDialog | Secção 11.2 |
| MEL-SET-01 | Vinculações nomeadas para LiveViewModel no AppModule | Secção 12.2 |
| MEL-SET-02 | Vinculações nomeadas para OwnTVPlayer no PlayerModule | Secção 12.2 |
| MEL-SET-03 | CI testar variantes release e x86_64 | Secção 12.2 |
| MEL-SET-04 | snapshot_core.py usar timestamps reais | Secção 12.2 |
| MEL-SET-05 | Método peek() não destrutivo no UiWorkWindow | Secção 12.2 |
| MEL-SET-06 | Validar refresh rate no UiPerformanceMonitor | Secção 12.2 |
| MEL-SET-07 | Estender pseudolocale para variantes benchmark e non-minified | Secção 12.2 |
| MEL-SET-08 | Adicionar version catalog (libs.versions.toml) | Secção 12.2 |
| MEL-SET-09 | Guard contra loop infinito de recreate() no LocalizedContent | Secção 12.2 |
| MEL-OIT-01 | Documentar configuração de assinatura no README | Secção 13.2 |
| MEL-OIT-02 | Validação de porta DNS com feedback visual | Secção 13.2 |
| MEL-OIT-03 | Validação de porta proxy com feedback visual | Secção 13.2 |
| MEL-OIT-04 | Exceção de domínio próprio no RecordingOpenSelection | Secção 13.2 |
| MEL-OIT-05 | Encriptar chave TMDB antes de guardar | Secção 13.2 |
| MEL-OIT-06 | Revisar necessidade de acesso ao clipboard | Secção 13.2 |
| MEL-OIT-07 | Ativar verificação de dependências no lint | Secção 13.2 |
| MEL-OIT-08 | Universal APK apenas para debug | Secção 13.2 |
| MEL-NIN-01 | Criar/verificar data_extraction_rules.xml para API 31+ | Secção 19.2 |
| MEL-NIN-02 | Estender testes de foco para PlayerHud dialogs | Secção 19.2 |
| MEL-NIN-03 | Wrapper de logging sanitizado para release | Secção 19.2 |
| MEL-NIN-04 | Restringir keep rules do proguard a enums serializados | Secção 19.2 |
| MEL-NIN-05 | Adicionar testes de integração para backup/restore | Secção 19.2 |
| MEL-NIN-06 | Adicionar testes de deep link handling | Secção 19.2 |
| MEL-NIN-07 | Revisar permissões de FileProvider no AndroidManifest | Secção 19.2 |

### 20.3 Itens Válidos — Inovações (INN, UX, TEC, ADD, TER, QUA, QUIN, SEX, SET, OIT, NIN)

Todas as inovações são **confirmadas como válidas como ideias futuras**, a serem avaliadas após estabilização:

| ID | Solução/Descrição Resumida | Onde está descrita |
|----|---------------------------|-------------------|
| INN-01 | Pré-visualização de restauro de backup | Secção 5.1 |
| INN-02 | Validação de exportação pelo contrato de importação | Secção 5.1 |
| INN-03 | Sessão de diagnóstico com duração definida | Secção 5.1 |
| INN-04 | Modo apenas áudio para canais de rádio | Secção 5.1 |
| INN-05 | Controlo parental por categoria com horário | Secção 5.1 |
| INN-06 | Sincronização com Trakt.tv / Plex | Secção 5.1 |
| INN-ADD-01 | Modo \"Convidado\" sem perfil | Secção 7.3 |
| INN-ADD-02 | Sugestão automática de canais por histórico | Secção 7.3 |
| INN-ADD-03 | PiP melhorado para Android TV | Secção 7.3 |
| INN-ADD-04 | Integração com \"Watch Next\" do Android TV | Secção 7.3 |
| INN-ADD-05 | Controlo por gestos no comando remoto | Secção 7.3 |
| INN-TER-01 | Modo \"Espelho de Ecrã\" para dispositivos móveis | Secção 8.3 |
| INN-TER-02 | Integração com calendário Android (gravações) | Secção 8.3 |
| INN-TER-03 | Modo \"Cinema\" com iluminação ambiente | Secção 8.3 |
| INN-TER-04 | Partilha de favoritos via QR code | Secção 8.3 |
| INN-TER-05 | Modo \"Crianças\" com ecrã inicial dedicado | Secção 8.3 |
| INN-QUA-01 | Backup automático para serviços de nuvem (Google Drive, Dropbox) | Secção 9.3 |
| INN-QUA-02 | Sincronização de lembretes de EPG entre dispositivos | Secção 9.3 |
| INN-QUA-03 | Modo \"Economia de Dados\" para sync de EPG e metadados | Secção 9.3 |
| INN-QUA-04 | Integração com assistente de voz para lembretes de EPG | Secção 9.3 |
| INN-QUA-05 | Modo \"Família\" com perfis vinculados e controlo parental centralizado | Secção 9.3 |
| INN-QUIN-01 | Modo \"Resumo de Sessão\" automático no encerramento | Secção 11.3 |
| INN-QUIN-02 | \"Continue Watching\" nativo do Android TV launcher | Secção 11.3 |
| INN-QUIN-03 | Modo \"Espelho Multi-Box\" para sincronizar reprodução | Secção 11.3 |
| INN-QUIN-04 | Controlo de qualidade de imagem por canal (perfil de codec) | Secção 11.3 |
| INN-QUIN-05 | Modo \"Ecrã Negro com Áudio\" para canais de rádio | Secção 11.3 |
| INN-SEX-01 | Modo \"Economia de Bateria\" para boxes portáteis | Secção 11.3 |
| INN-SEX-02 | Atalhos personalizados no launcher Android TV | Secção 11.3 |
| INN-SEX-03 | Modo \"Visita\" para casas de férias/aluguer | Secção 11.3 |
| INN-SEX-04 | Relatórios de uso para pais (controlo parental avançado) | Secção 11.3 |
| INN-SEX-05 | Integração com iluminação ambiente doméstica | Secção 11.3 |
| INN-SET-01 | Dashboard de dependências e build | Secção 12.3 |
| INN-SET-02 | Modo de diagnóstico de build para suporte | Secção 12.3 |
| INN-SET-03 | CI com deteção de deriva de dependências | Secção 12.3 |
| INN-SET-04 | Sistema de perfis de desempenho por dispositivo | Secção 12.3 |
| INN-SET-05 | Integração com sistema de relatórios de erro remoto | Secção 12.3 |
| INN-OIT-01 | Modo \"Build Info\" para suporte técnico | Secção 13.3 |
| INN-OIT-02 | Validação automática de configuração de rede no arranque | Secção 13.3 |
| INN-OIT-03 | Sistema de perfis de build por tipo de dispositivo | Secção 13.3 |
| INN-OIT-04 | Integração com sistema de atualização remota (OTA) | Secção 13.3 |
| INN-OIT-05 | Modo \"Kiosk\" para instalações públicas | Secção 13.3 |
| INN-NIN-01 | Modo de segurança \"isolado\" para deep links | Secção 19.3 |
| INN-NIN-02 | Auditoria de acesso a segredos | Secção 19.3 |
| INN-NIN-03 | Modo \"Air Gap\" para instalações sensíveis | Secção 19.3 |
| INN-NIN-04 | Validação automática de configuração ProGuard | Secção 19.3 |
| INN-NIN-05 | Modo \"Segurança Avançada\" com bloqueio biométrico/PIN | Secção 19.3 |
| UX-01 | Modo escuro automático por hora do dia | Secção 5.2 |
| UX-02 | Atalhos numéricos rápidos | Secção 5.2 |
| UX-03 | Personalização do layout do Home | Secção 5.2 |
| UX-04 | Modo de economia de energia | Secção 5.2 |
| UX-05 | Notificações de gravação | Secção 5.2 |
| TEC-01 | Upscaling por hardware (beta, opcional) | Secção 5.3 |
| TEC-02 | Previsão de largura de banda e qualidade automática | Secção 5.3 |
| TEC-03 | Reconexão inteligente com backoff exponencial | Secção 5.3 |
| TEC-04 | Multiview de perfis | Secção 5.3 |
| TEC-05 | Integração com assistente de voz | Secção 5.3 |

### 20.4 Itens Rejeitados / Não Aplicar na Forma Proposta

O Council rejeitou as seguintes propostas (não implementar na forma apresentada):

| Item | Motivo da Rejeição |
|------|-------------------|
| Destruir mpv concorrente + timeout como prova de fecho | API libmpv declara destruição bloqueante; timeout não é confirmação nativa |
| Abortar reprodução quando `playbackRequested=false` | Destroi pausa deliberada; false inclui pausa e stop |
| Converter todos booleanos Xtream para null | Apaga informação legítima (is_adult, tv_archive, flags) |
| Marcar diferido só porque `catchup-days > 0` | catchup-days>0 sozinho não constrói URL |
| Concatenar todo path relativo como query | Caminhos relativos exigem resolução URI/HttpUrl apropriada |
| Guardar todo EPG em RAM | Não em boxes de 2GB; preservar atomicidade e bounded staging |
| Aplicar `chunked(450)` a todas as consultas | Queries com ORDER BY/LIMIT não podem concatenar chunks |
| Ativar flags non-IDR/access-unit globalmente | Risco de corrupção visual; opt-only se houver caso dirigido |
| Exigir KeyUp sempre | Reintroduz caso documentado que engole próximo OK |
| Criar índices sem query/EXPLAIN que os justifique | Sem benefício demonstrado; adiciona escrita/migração/DB size |

---

## 21. Plano de Implementação Atualizado por Fases (com soluções)

### Fase 1 — Correções Críticas (P1)
BUG-P1-01, BUG-P1-02, BUG-P1-03, BUG-P1-04

**Critério de saída:** roundtrip de backup válido; logs sintéticos sem segredos; filtros finais do guia respeitados; restauro seletivo sem cruzar identidades.

### Fase 2 — Reprodução e Preferências (P2)
BUG-P2-01 a BUG-P2-07, BUG-P4-01, BUG-P5-01, BUG-ADD-01 a BUG-ADD-03, BUG-ADD-08, BUG-TER-01, BUG-TER-02, BUG-TER-07, BUG-QUA-01, BUG-QUA-02, BUG-QUIN-01, BUG-QUIN-02, BUG-QUIN-03, BUG-SET-01, BUG-SET-02, BUG-SET-09, BUG-OIT-01, BUG-OIT-02, BUG-NIN-01

**Critério de saída:** matriz de pausa/retoma saudável; gravações seguem opção visível; previews sequenciais abrem; formatos de data/hora consistentes; leitor externo resolvido pelo Android; validação de definições restauradas completa; refresh de EPG coerente com reprodução; arranque usa perfil consistente; Home sem cargas duplicadas; Search usa externalPlayer correto; multiview releaseAll aguarda confirmação; FailedChannelAttempts isola códigos por owner; DirectTune Ambiguous mostra ecrã de escolha; sync EPG sobrevive à saída do wizard; backup automático não deixa staging órfão; chave AndroidKeyStore tem rotação/recuperação; onStop para reprodução independentemente da DB; exceções de appScope são logadas; migrações de ShellViewModel têm feedback de erro; AppModule e PlayerModule usam vinculações nomeadas; version catalog adicionado; caminho de assinatura hardcoded removido; release falha sem keystore; MainActivity deep links validam origem.

### Fase 3 — Coerência da Interface (P2/P3)
BUG-P3-01 a BUG-P3-04, BUG-ADD-04 a BUG-ADD-07, BUG-TER-03 a BUG-TER-06, BUG-TER-08, BUG-QUA-03 a BUG-QUA-10, BUG-QUIN-04 a BUG-QUIN-10, BUG-SEX-01 a BUG-SEX-10, BUG-SET-03 a BUG-SET-08, BUG-SET-10, BUG-OIT-03 a BUG-OIT-10, BUG-NIN-02 a BUG-NIN-08

**Critério de saída:** pesquisa limpa só no background real; nomes personalizados pesquisáveis; callbacks antigos recusados; aviso de alarmes atual; foco estável em overlays; CategoryRail com chave estável; restoreFocus resetado; zapSource invalidado; continueTarget alargado; LongPressMenuGuard com timeout; ChannelPressGesture sem PLAY após menu; UpdateDialog sem loop de foco; Customize moveMutex protege leituras; AddSource formToken invalidado; AutoStartService pendingWake resetado; lembretes EPG expirados limpos; ProgrammeReminderDialog foco garantido; GuideLoadGate reload após inatividade; StorageBrowser dismissível via Back; EPG feed falhado identificado; RemoteBackupRestoreScreen acumula backups; SettingsScreen estado não vaza entre secções; ChannelPressInput sem ativação duplicada; DatabaseStartupViewModel retry limitado; FocusTrap com logging/fallback; BulkRenameDialogs valida returnFocus; AddSourceChooser foco padrão; MainActivity limpa pendingDeepLink; StreamInfoOverlay estado de carregamento; PlayerHud restaura foco após diálogos; LiveRestart alarga janela; FrameRateController cooldown adaptativo; SyncStatusPill contagem por tipo; TopBar indicador de sync; Sidebar indicador Kids; ChannelPressIndicator componente; IncompleteRestoreDialog lista ignorados; ArchivePlaybackConfigDialog validação; ExitDialog aviso gravação; BackupAppSettings escopo revisto; CI testa release e x86_64; snapshot_core.py timestamps reais; UiWorkWindow peek() não destrutivo; UiPerformanceMonitor valida refresh rate; i18n.yml cobre todas variantes; LocalizedContent guard contra loop recreate(); Build: caminho de assinatura não hardcoded; release falha sem keystore; universal APK só para debug; lint verifica dependências; porta DNS validada; porta proxy validada; RecordingOpenSelection exceção de domínio; chave TMDB encriptada; acesso ao clipboard revisado; atalhos remotos sem duplicações; FileProvider restrito a pastas específicas; stripping de Logs estendido; keep enum restringido; testes de backup/restore adicionados; testes de deep link adicionados; serviço de acessibilidade revisado.

### Fase 4 — Timeshift e Diferidos (Fase B do plano 5.2.0)
A18, A19, A20, A21 (da análise anterior)

**Critério de saída:** início efetivo do arquivo transportado; fuso automático opcional com validação; convenções M3U tratadas; identidade CATCHUP estável com renovação Stalker.

### Fase 5 — Memória, Zapping e Manutenção (Fase C/D)
A22, A23, A24, MEL-06, MEL-07, MEL-08, MEL-09

**Critério de saída:** monitor de pressão de memória Android 14+; timeshift recuperável após indisponibilidade; zapping sem 500 ms desnecessários; snapshot transacional; CI executa testes Core; bump automático convertido; fixture de desempenho compatível.

### Fase 6 — Melhorias de Código e UX (MEL, MEL-ADD, MEL-TER, MEL-QUA, MEL-QUIN, MEL-SEX, MEL-SET, MEL-OIT, MEL-NIN)
MEL-01 a MEL-05, MEL-ADD-01 a MEL-ADD-04, MEL-TER-01 a MEL-TER-04, MEL-QUA-01 a MEL-QUA-05, MEL-QUIN-01 a MEL-QUIN-10, MEL-SEX-01 a MEL-SEX-10, MEL-SET-01 a MEL-SET-09, MEL-OIT-01 a MEL-OIT-08, MEL-NIN-01 a MEL-NIN-07

**Critério de saída:** componentes de foco reutilizáveis; empty states em listas vazias; erros de rede diferenciados; chunked SQLite revisto; confirmação PIN em perfis; foco restaurado na secção de origem; esqueleto só no cold start; debounce ajustável; progresso por download; indicador visual de falhas de canal; notificação de atualização; busca rápida em Customize; validação de URL em AddSource; status do backup automático visível; opção backup antes de atualização; busca rápida StorageBrowser; confirmação limpeza lembretes EPG; progresso sync EPG visível; paragem de playback independente da DB; logging de exceções appScope; feedback de migração ShellViewModel; ativação unificada ChannelPressInput; retry limitado DatabaseStartupViewModel; logging/fallback FocusTrap; validação returnFocus BulkRenameDialogs; foco padrão AddSourceChooser; limpeza pendingDeepLink MainActivity; estado carregamento StreamInfoOverlay; foco restaurado PlayerHud; janela reinício LiveRestart; cooldown adaptativo FrameRateController; contagem por tipo SyncStatusPill; indicador sync TopBar; indicador Kids Sidebar; ChannelPressIndicator componente; lista ignorados IncompleteRestoreDialog; validação ArchivePlaybackConfigDialog; aviso gravação ExitDialog; vinculações nomeadas AppModule/PlayerModule; CI variantes adicionais; snapshot timestamps reais; peek() UiWorkWindow; validação refresh rate; pseudolocale todas variantes; version catalog; guard recreate() LocalizedContent; documentação assinatura README; validação porta DNS; validação porta proxy; exceção domínio RecordingOpenSelection; chave TMDB encriptada; verificação dependências lint; universal APK só debug; data_extraction_rules.xml criado; testes foco PlayerHud; wrapper logging sanitizado; keep rules restringidos; testes integração backup/restore; testes deep link.

### Fase 7 — Inovações Selecionadas (após estabilização)
INN-01 a INN-06, INN-ADD-01 a INN-ADD-05, INN-TER-01 a INN-TER-05, INN-QUA-01 a INN-QUA-05, INN-QUIN-01 a INN-QUIN-05, INN-SEX-01 a INN-SEX-05, INN-SET-01 a INN-SET-05, INN-OIT-01 a INN-OIT-05, INN-NIN-01 a INN-NIN-05, UX-01 a UX-05, TEC-01 a TEC-05

**Critério de saída:** cada inovação avaliada individualmente com medição de impacto real; TEC-01 apenas se houver ganho real medido.

---

## 22. Critérios de Aceitação e Validação (Atualizados)

1. **Compilação/lint** após cada lote que altere código (`compile standardRelease`, `lintVitalStandardRelease`).
2. **Testes de contrato, migração e backup** ao tocar em persistência (Room, backup JSON, schemas).
3. **Testes de cancelamento/ownership** ao tocar em reprodução (players, workers, sessões).
4. **Testes físicos** em box real:
   - Diferidos com +1h, rede desligada/restabelecida no timeshift
   - A→B→A com espera configurada
   - Home/regresso e leitores externos
   - Gravações, recuo no leitor, diferidos, restauração de backup
   - Troca de perfil durante arranque
   - Cold start do Home (sem cargas duplicadas)
   - Abrir/fechar multiview rapidamente
   - Sair do player e verificar foco na secção correta
   - DirectTune com número ambíguo (mostrar lista de escolha)
   - Troca rápida de canais (verificar códigos HTTP corretos em falhas)
   - Abrir/fechar diálogos rapidamente (verificar LongPressMenuGuard)
   - Backup automático com espaço insuficiente (verificar não deixa staging órfão)
   - Desligar/ligar ecrã rapidamente (verificar não há lançamentos inesperados)
   - Abrir StorageBrowser e pressionar Back (verificar fecha)
   - Enviar múltiplos backups via LAN rapidamente (verificar todos aparecem)
   - Colocar app em background com DB corrompida (verificar se playback para)
   - Pressionar OK curto/longo em canal (verificar ativação única)
   - Clicar retry na DB startup múltiplas vezes (verificar limite de 3)
   - Abrir/fechar diálogos do PlayerHud (verificar restauração de foco)
   - Mudar canal rapidamente com taxas diferentes (verificar AFR)
   - Iniciar múltiplos syncs (verificar contagem oculta no pill)
   - Iniciar gravação e tentar sair (verificar aviso no ExitDialog)
   - Mudar de família de script (verificar ausência de loop recreate)
   - Verificar tipos de dependências injetadas (LiveViewModel, OwnTVPlayer)
   - Build de release sem keystore (verificar que falha ou produz APK não assinado)
   - Digitar porta DNS inválida (verificar erro e botão desativado)
   - Digitar porta proxy 0 (verificar erro)
   - Navegação rápida entre gravações (verificar não há cancelamentos inesperados)
   - Enviar deep link de app externa (verificar validação de origem)
   - Tentar aceder a ficheiro fora das pastas do FileProvider (verificar rejeição)

5. **Não prometer** eliminação de bloqueios do fornecedor, melhoria de qualidade visual ou correção de cortes de áudio sem medições próprias (Perfetto/frame timing na box real).

---

## 23. Limites da Análise

- Esta é uma **análise estática de fontes e contratos**; não foram executados builds, testes nem ensaios em dispositivos físicos durante esta tarefa.
- Nenhuma alteração de produção foi aplicada como parte desta análise.
- O core (`OwnTV_Core`) é um repositório separado; alterações no core requerem snapshot e atualização do lock pelo fluxo existente.
- Os totais de testes citados em documentos anteriores referem-se a validações já concluídas nesses relatórios, não a execução nova nesta análise.
- As datas das nove rondas de análise: 18/10/2026, 19/10/2026, 20/10/2026, 21/10/2026, 22/10/2026, 23/10/2026, 24/10/2026, 25/10/2026 e 26/10/2026.
- Os ficheiros `PlayerDiagnostics.kt` e `PlaybackMemoryMonitor.kt` não existem no repositório da app (pertencem ao core repo separado `OwnTV_Core`).

---

## 24. Próximos Passos Imediatos

1. Criar a branch `analise-implementar-rena` no git.
2. Começar pela **Fase 1** (correções P1: BUG-P1-01 a P1-04).
3. Para cada correção, escrever teste de regressão **antes** de alterar o código.
4. Atualizar este ficheiro à medida que itens forem implementados.
5. Agendar revisão do Council após cada fase concluída.
6. Reavaliar a lista de inovações (Fase 7) apenas após a estabilização das Fases 1–6.

---

<sub>Análise gerada com auxílio de conselho de multi-agentes especializados (UI/EPG, Player/Áudio/Lifecycle, Core/Parser/Armazenamento/DB) e Council coordenador. Nove rondas de análise: 18/10/2026, 19/10/2026, 20/10/2026, 21/10/2026, 22/10/2026, 23/10/2026, 24/10/2026, 25/10/2026 e 26/10/2026. Baseado em documentos existentes em `docs/` e código-fonte de `app/src/main`. Nenhum código foi alterado.</sub>"