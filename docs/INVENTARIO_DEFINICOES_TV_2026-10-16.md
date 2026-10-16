# Inventário de definições OwnTV TV — 16/10/2026

**Valores predefinidos verificados no código, não valores lidos da tua box.** Instalações existentes, restauros e exceções por lista/canal podem ter outros valores. A configuração da box física não foi consultada. Na verificação final ADB apenas estava disponível um emulador; os seus dados não foram usados como se fossem os da tua box. Não foram lidos nem incluídos utilizadores, palavras-passe, URLs privados ou ficheiros de configuração pessoais.

Atualizado após a escolha do utilizador: procura de atualizações ligada, barra lateral visível e prioridade de versões desligada. As duas últimas opções são aplicadas uma vez nesta atualização; escolhas posteriores continuam válidas. Brilho ambiente, animações decorativas e meteorologia foram removidos da interface e do código funcional. Backups antigos continuam aceites, ignorando os campos descontinuados. O repositório de atualizações mantém-se `renafcampos27/Owntv`.

## Como escolher

Preenche **Novo valor** e/ou **Remover da UI?**. Célula vazia significa manter como está. Para limpar um valor, escreve «Vazio»; para voltar às definições gerais de uma lista/canal, escreve «Herdar». Remover uma opção da interface não significa apagar automaticamente os dados de backups antigos ou funções partilhadas do Core. As linhas com valores condicionais indicam a regra efetiva; não inventam um valor numérico universal.

Também existe uma folha CSV para editar em Excel: `ESCOLHER_PREDEFINICOES_TV_2026-10-16.csv`. O anexo `INVENTARIO_TECNICO_DEFINICOES_TV_2026-10-16.md` conserva todas as definições globais reativas encontradas no SettingsRepository, as funções de perfil/layout/menu e os modelos de configurações por lista/canal, incluindo campos antigos e estados internos que não são opções visíveis.

## Diferenças importantes para a tua escolha

- «Áudio por software» desligado significa que a preferência forçada não está ativa; a seleção automática e as compatibilidades de codec continuam a poder usar software.
- O Core guarda `EXO_FIRST`, mas Apenas HLS restringe a escolha herdada a ExoPlayer; uma regra explícita por canal pode alterar o comportamento permitido.
- Reserva Equilibrada significa limiares 8–10 s. Não é um corte rígido num segmento HLS a meio.
- O desvio do EPG é 0 e a correção do pedido de diferido é +1 h. São parâmetros diferentes.
- Agrupar versões e priorizar versões estão Desligados. São parâmetros diferentes.
- Há preferências de filmes/séries, smartphone, favoritos, histórico e OpenSubtitles mantidas no Core por compatibilidade. O anexo identifica os campos; a sua presença não prova que exista opção visível nesta edição TV.

## Aplicação

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `updateCheckOnStart` | Procurar atualizações ao abrir | Ligado; ativação única nesta atualização, mesmo se antes estava desligado | | |
| `releaseRepo` | Repositório de atualizações | renafcampos27/Owntv; só verifica/oferece, não instala automaticamente | | |
| `liveTvOnlyMode` | Edição apenas TV | Ligado e fixo nesta edição; filmes/séries ocultos | | |
| `visualProfile` | Perfil visual | Automático, adaptado ao dispositivo | | |
| `startupMode(profileId)` | Ecrã/canal ao arrancar, por perfil | HOME por omissão; instalações antigas podem herdar Último canal se essa preferência estava ligada | | |
| `startupChannel(profileId)` | Canal de arranque fixo | Nenhum até o utilizador escolher | | |
| `startOnBoot` | Abrir com o arranque da box | Desligado | | |
| `startOnWake` | Abrir ao sair da suspensão | Desligado | | |
| `activeProfileId` | Perfil ativo | Depende dos perfis existentes; chave sem valor = -1, resolvida pela app | | |
| `avatarId` | Avatar | Índice 0 | | |

## Canais e lista

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `hideCategories` | Modo simples: ocultar categorias | Ligado | | |
| `hideSidebar` | Ocultar barra lateral | Desligado; barra visível, aplicado uma vez nesta atualização | | |
| `channelRecovery` | Alternativas do mesmo canal | Ligado | | |
| `recoveryTimeoutSeconds` | Tempo antes de tentar outra versão | 7 segundos; intervalo normalizado 1–60 | | |
| `prioritizeChannelVersions` | Prioridade de versões do mesmo canal | Desligado; grupos e ordens manuais preservados | | |
| `groupChannelVersions` | Agrupar versões numa linha | Desligado | | |
| `channelVersionOrders` | Ordem personalizada de versões por grupo | Nenhuma ordem manual guardada inicialmente | | |
| `channelVersionGroups` | Associação manual de versões ao grupo | Nenhuma associação inicial | | |
| `sortLive` | Ordem da lista de canais | Ordem da lista/fornecedor (PLAYLIST); pode escolher A–Z ou Personalizada | | |
| `categoryOrder` | Ordem personalizada de categorias | Vazia; usa a ordem normal até editar | | |
| `hiddenCategories` | Categorias ocultas | Nenhuma inicialmente; diferente de ocultar toda a coluna | | |
| `hiddenItems` | Canais ocultos | Nenhum inicialmente | | |
| `categoryNames` | Nomes personalizados de categorias | Nenhum inicialmente | | |
| `itemNames` | Nomes personalizados de canais | Nenhum inicialmente | | |
| `customCategories` | Categorias criadas pelo utilizador | Nenhuma inicialmente | | |
| `movedFromOrigin` | Canais movidos entre categorias | Nenhum inicialmente | | |
| `hideNewCategoriesDefault(profileId)` | Ocultar categorias que apareçam numa atualização | Desligado | | |
| `customizePin(profileId)` | PIN de personalização | Sem PIN | | |
| `rememberCategoryLive` | Memorizar categoria TV | Ligado | | |
| `rememberLastLive` | Memorizar último item TV | Desligado; o regresso imediato do leitor mantém o seu próprio foco | | |
| `liveGuideDelayMs` | Consultar agora/seguinte após parar num canal | 3 segundos; alternativa imediata = 0 | | |
| `directTune` | Abrir canal diretamente | Ligado | | |

## Comando

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `chNavEnabled` | Navegação por canais/páginas no comando | Ligado | | |
| `chNavUpSkip` | Salto na lista para cima | 10 itens | | |
| `chNavDownSkip` | Salto na lista para baixo | 10 itens | | |
| `remoteShortcutBindings` | Atalhos do comando | CH+/CH− e recuar/avançar: toque curto muda página; longo vai ao primeiro/último | | |

## Reprodução

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `hwDecoding` | Descodificação de vídeo por hardware | Ligado; a capacidade real depende do codec/dispositivo | | |
| `decoderQueueing` | Fila do descodificador ExoPlayer | Automática; também existem síncrona e assíncrona | | |
| `liveEnginePreference` | Preferência de motor em direto | EXO_FIRST no Core; Apenas HLS faz herança usar ExoPlayer, salvo escolha explícita compatível por canal | | |
| `liveHlsOnly` | Apenas ligações HLS | Ligado | | |
| `externalPlayerLive` | Leitor externo para canais | Desligado; usa leitor interno | | |
| `liveLatencyMode` | Atraso face ao direto | Equilibrado/automático; sem impor um offset fixo | | |
| `liveLatencyCustomSecs` | Atraso personalizado, quando selecionado | 8 segundos; só atua em Personalizado | | |
| `liveReserveMode` | Reserva de reprodução | Equilibrada numa instalação nova; compatibilidade pode herdar antiga configuração de buffer | | |
| `liveReserveCustomSecs` | Reserva personalizada, quando selecionada | 8 segundos; só atua em Personalizado | | |
| `liveReserveExtraSecs` | Diferença entre limiar inferior e superior de carregamento | 2 segundos | | |
| `liveReserveSeconds` | Reserva efetiva em Equilibrada | Limiar inferior de 8 s e superior de 10 s; segmentos completos podem ultrapassar o superior | | |
| `livePrerollSecs` | Pré-buffer inicial extra | Desligado (0); limiar automático inicial existente de 1,5 s. UI oferece 2–10 s e preserva valor antigo | | |
| `channelReopenIntervalSecs` | Intervalo para reabrir o mesmo canal | 20 segundos; opções de 10 a 50 | | |
| `liveTuneTimeoutSecs` | Desistir de abrir o canal após | 15 segundos; UI: 9–30 s, 60 s ou Nunca | | |
| `hdrEnabled` | HDR | Desligado | | |
| `deinterlace` | Desentrelaçamento mpv | Desligado | | |
| `autoFrameRate` | Ajuste automático de frequência da TV (AFR) | Desligado | | |
| `afrPauseSecs` | Pausa durante troca de frequência | 0 segundos; limite 5 | | |
| `afrMatchResolution` | Mudar também a resolução com AFR | Desligado | | |
| `defaultZoom` | Ajuste inicial da imagem | FIT: caber no ecrã | | |
| `seekStepSec` | Passo de avanço/recuo do leitor | 10 segundos | | |
| `liveRewindStepSec` | Passo de recuo no direto/diferido | 30 segundos | | |
| `multiviewEnabled` | Multiview | Desligado | | |
| `multiviewTiles` | Número de leitores Multiview | 2 leitores quando a funcionalidade estiver ligada | | |
| `livePreviewEnabled` | Pré-visualização da lista TV | Ligada no armazenamento por defeito; depende de o painel de preview estar visível | | |
| `livePreviewAudio` | Som da pré-visualização | Desligado | | |
| `miniPlayerStyle` | Estilo do mini-leitor | Flutuante | | |
| `miniPlayerSizePct` | Largura do mini-leitor | 25% do ecrã | | |
| `miniPlayerPosition` | Posição do mini-leitor | Canto inferior direito | | |
| `localTimeshiftResumeMode` | Retomar timeshift local | Perguntar | | |
| `resumeMode` | Retomar posição de reprodução guardada | Perguntar | | |

## Áudio

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `softwareAudio` | Preferir descodificação de áudio por software | Desligado = escolha automática; não força hardware para codecs incompatíveis | | |
| `surroundMode` | Saída de som | Apenas estéreo | | |
| `surroundSound` | Compatibilidade antiga de surround | Desligado; o modo de saída explícito tem prioridade | | |
| `defaultVolume` | Volume interno predefinido | 100%; controlo do volume pela box, conforme política atual do leitor | | |
| `audioDelayMs` | Correção de sincronismo A/V | 0 ms | | |
| `preferredAudioLang` | Idioma de áudio preferido | Sem preferência explícita | | |

## Legendas

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `preferredSubLang` | Idioma de legendas preferido | Sem preferência explícita | | |
| `subtitleStyleEnabled` | Estilo personalizado de legendas | Desligado numa instalação nova | | |
| `subtitleScaleExo` | Tamanho de legendas ExoPlayer | 100% / escala 1,0 | | |
| `subtitleScaleMpv` | Tamanho de legendas mpv | 100% / escala 1,0 | | |
| `subtitleFont` | Tipo de letra das legendas | Predefinição do leitor | | |
| `subtitleColor` | Cor das legendas | Predefinição do leitor/stream | | |
| `subtitlePosition` | Posição das legendas | Predefinição do leitor/stream | | |
| `subtitleBgOpacity` | Opacidade do fundo das legendas | Predefinição do leitor (-1); não equivale a fundo opaco | | |

## EPG e diferidos

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `guideReplayOnly` | Mostrar apenas canais com diferidos no Guia | Ligado | | |
| `sortGuide` | Ordenação do Guia | Segue a lista TV (LIVE_TV) | | |
| `guideView` | Vista do Guia guardada | Sem escolha explícita; a edição TV apresenta a grelha | | |
| `guideDensityPct` | Densidade legada do Guia | 100%; não confundir com larguras ou zoom | | |
| `epgOffsetMinutes` | Desvio global do EPG | 0 minutos / desativado | | |
| `guideDaysToKeep` | Dias futuros do EPG a guardar | 7 dias; não garante 7 dias de arquivo do fornecedor | | |
| `epgAutoRefresh` | Atualização das origens EPG | A cada 6 horas quando não há escolha específica por origem | | |
| `epgUseLogos` | Usar logótipos das origens EPG | Desligado por origem inicialmente | | |
| `catchupPlayer` | Leitor para programas em diferido | Interno | | |
| `catchupFormat` | Formato dos programas em diferido | Automático | | |
| `catchupTimezone` | Fuso horário dos diferidos | Do dispositivo | | |
| `catchupOffsetMinutes` | Desvio manual de fuso dos diferidos | 0 minutos; relevante no modo Manual | | |
| `catchupRequestShiftMinutes` | Correção do pedido do diferido | +60 minutos / +1 hora | | |
| `epgMatches` | Associação EPG manual por canal | Nenhuma inicialmente; mantém a identificação normal | | |
| `epgFallbacks` | Guia alternativo aprovado por canal | Nenhum inicialmente | | |
| `epgShifts` | Desvio EPG por canal | Sem exceções; segue o global | | |
| `archivePlaybackConfigs` | Exceções de formato/templates de diferidos | Lista vazia inicialmente; escolhas existentes mantêm-se | | |

## Gravações e armazenamento

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `recordingReserveConnection` | Reservar uma ligação para ver TV | Ligado | | |
| `recordingPreRollMinutes` | Margem antes do programa | 1 minuto | | |
| `recordingPostRollMinutes` | Margem depois do programa | 3 minutos | | |
| `recordingOverMobileData` | Gravar numa ligação de dados limitada | Ligado; disponibilidade depende da ligação da box | | |
| `recordWhatImWatching` | Gravar automaticamente o que estou a ver | Desligado | | |
| `internalMediaQuotaGiB` | Quota de multimédia no armazenamento interno | 4 GiB; configurável entre 1 e 32 | | |
| `externalMediaQuotaGiB` | Quota de multimédia no armazenamento externo | 16 GiB; configurável entre 1 e 256 | | |
| `downloadRoot` | Pasta de gravações/downloads | Sem pasta explícita; usa resolução de armazenamento interna existente | | |

## Interface

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `navMenuMode` | Comportamento da navegação lateral | Estático | | |
| `navMenuHidden` | Secções ocultas individualmente na navegação | Nenhuma adicional inicialmente; a edição só TV aplica o seu próprio filtro | | |
| `themeMode` | Tema | Escuro | | |
| `accent` | Cor de destaque | TEAL / verde-azulado | | |
| `customAccent` | Cor de destaque personalizada | Nenhuma | | |
| `focusHighlight` | Cor personalizada do cursor/foco | Nenhuma; segue o tema | | |
| `focusHighlightWidth` | Espessura do destaque de foco | 2 | | |
| `uiZoomPercent` | Zoom da interface | 90% | | |
| `fontCustomization.sizePercent` | Tamanho da letra geral | 100% | | |
| `fontCustomization.mainFamily` | Letra principal | Sistema sem serifa | | |
| `fontCustomization.popupFamily` | Letra das janelas | Lora | | |
| `fontCustomization.popupFontSizePercent` | Tamanho da letra das janelas | 100% | | |
| `fontCustomization.popupSizePercent` | Tamanho das janelas | 100% | | |
| `bgImagePath` | Imagem de fundo personalizada | Nenhuma | | |
| `glassConfig` | Efeito de vidro por área | Nenhuma área ativa por defeito; parâmetros guardados: alpha 56%, blur 78%, realce 55% | | |
| `panelWidthEnabled` | Larguras personalizadas da lista TV | Desligado; sem percentagens guardadas | | |
| `guideWidthEnabled` | Larguras personalizadas do Guia | Desligado; sem percentagens guardadas | | |
| `heroPreviewEnabled` | Pré-visualização da página inicial | Automática: ligada salvo dispositivo de poucos recursos; só se essa página estiver em uso | | |
| `homeConfig` | Conteúdo da página inicial | Ordem normal; recentes TV ocultos; restantes regras no anexo, quando a página estiver em uso | | |

## Rede

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `proxyConfig.enabled` | Proxy | Desligado | | |
| `proxyConfig.host` | Servidor proxy | Vazio | | |
| `proxyConfig.port` | Porta proxy | 0 / não configurada | | |
| `proxyConfig.username` | Utilizador proxy | Vazio | | |
| `proxyConfig.password` | Palavra-passe proxy | Vazia | | |
| `dnsConfig.enabled` | DNS personalizado | Desligado | | |
| `dnsConfig.host` | Servidor DNS | Vazio | | |
| `dnsConfig.port` | Porta DNS | 53 | | |
| `dnsConfig.dohUrl` | DNS por HTTPS | URL vazio | | |

## Diagnóstico

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `detailedDiagnostics` | Registo detalhado de reprodução | Desligado | | |
| `measuredStreamStats` | Estatísticas de transferência medidas | Ligado | | |
| `uiDiagnostics` | Diagnóstico de desempenho da interface | Desligado | | |

## Por lista/canal

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `playlistAutoRefresh` | Atualização da lista de reprodução | Desligada quando não há escolha por origem | | |
| `defaultSourceId` | Lista ativa/predefinida | Sem origem específica (-1); resolve todas as origens permitidas no perfil | | |
| `source.identity` | Nome, tipo, URL, utilizador, palavra-passe, MAC e dados Stalker | Introduzidos pelo utilizador; não existe valor universal nem foram lidos da box | | |
| `source.userAgent` | User-Agent por lista/EPG | IPTVSmartersPro por defeito; um valor explícito na lista/canal tem prioridade | | |
| `source.httpReferer` | Referer HTTP por lista | Nenhum inicialmente | | |
| `source.preferHls` | Preferir HLS por lista | Falso no construtor; não desativa a restrição geral Apenas HLS | | |
| `source.playbackOverrides` | Motor, pré-buffer, atraso, reserva e timeout por lista | Seguir configurações gerais quando não há substituição | | |
| `channelPlaybackConfigs` | Configuração personalizada por canal | Lista inicialmente vazia; regras pré-existentes e exceções TS são preservadas | | |
| `ChannelPlaybackOptions.engine` | Motor por canal | Herdar | | |
| `ChannelPlaybackOptions.format` | Formato por canal | Herdar; pode escolher HLS, TS ou Automático | | |
| `ChannelPlaybackOptions.reserveSecs` | Reserva mínima por canal | Herdar | | |
| `ChannelPlaybackOptions.extraSecs` | Margem superior da reserva por canal | Herdar | | |
| `ChannelPlaybackOptions.prerollSecs` | Pré-buffer por canal | Herdar | | |
| `ChannelPlaybackOptions.latencySecs` | Atraso face ao direto por canal | Herdar | | |
| `ChannelPlaybackOptions.softwareAudio` | Áudio por software por canal | Herdar | | |
| `ChannelPlaybackOptions.audioDelayMs` | Correção A/V por canal | Herdar | | |
| `ChannelPlaybackOptions.hlsDetectAccessUnits` | Detetar unidades de acesso HLS por canal | Herdar; sem exceção global nova | | |
| `ChannelPlaybackOptions.hlsAllowNonIdrKeyframes` | Aceitar keyframes não IDR por canal | Herdar; sem exceção global nova | | |
| `ChannelPlaybackOptions.hlsPrepareFromSegments` | Preparação HLS a partir dos segmentos por canal | Herdar; sem exceção global nova | | |
| `manualTsChannels` | Exceções manuais a Apenas HLS | Nenhuma inicialmente | | |

## Validação da alteração de atualizações

A migração usa um marcador técnico local, sem alterar o formato do backup de modo simples. Testes verificam a ativação, a persistência após reabrir o armazenamento, o respeito por desligar posteriormente e a repetição após falha sem mexer nas outras opções. Resultado final de compilação/testes registado no fim deste ficheiro.


Validação da limpeza visual: debug/release compilados, lint release aprovado e 2 220 testes unitários aprovados (415 App, 1 256 Core, 549 reprodutores), além de 6 testes do pacote Core. Detalhes em `IMPLEMENTACAO_LIMPEZA_VISUAL_PREDEFINICOES_2026-10-16.md`. Teste numa box física pendente.

## Backups automáticos

| ID | Definição | Predefinição atual no código | Novo valor | Remover da UI? |
|---|---|---|---|---|
| `automaticBackupIntervalMonths` | Frequência de backup automático | A cada 6 meses; opções Manual, 3 meses, 6 meses e anual | | |

Exige configurar uma vez a pasta, a palavra-passe e os perfis autorizados. Preserva as três últimas cópias automáticas. A pasta predefinida é `OwnTV/backups/automatic` no armazenamento da app; é apagada ao desinstalar/limpar os dados. A pasta e a palavra-passe não são exportadas para outro dispositivo.

Validação do backup automático: debug/release e lint release aprovados; 424 testes App aprovados, incluindo 9 novos. Detalhes em `IMPLEMENTACAO_BACKUP_AUTOMATICO_2026-10-16.md`.
