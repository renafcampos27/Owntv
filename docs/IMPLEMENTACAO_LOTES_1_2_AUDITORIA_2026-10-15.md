# Implementação dos lotes 1 e 2 — 15/10/2026

Âmbito autorizado: A15-01 a A15-07 de `AUDITORIA_ESPECIALIZADA_TV_NOVOS_ACHADOS_2026-10-15.md`, na App TV e Core personalizado. Os lotes 3–5, E3/JNI e E4/ingestão EPG não fazem parte desta implementação. Mobile não foi alterado. Não se alteraram buffers, cooldowns, User-Agent, headers enviados, volume interno, versões de bibliotecas ou defaults de reprodução.

## Lote 1

- **A15-01:** domínio de backup do fundo de legendas usa os limites do contrato SubtitleStyle: -1 significa predefinição; 0..100 mantém os valores explícitos. Valores -2/101 continuam inválidos. Testes usam SettingsRepository e DataStore em memória, com setter, exportação, serialização JSON e importação reais.
- **A15-02:** logs do HttpClient de catálogos recebem apenas esquema/host/porta. Não escrevem caminho, userinfo, query, fragmento ou User-Agent bruto. Erros registam classe/status/tentativa, sem mensagem nem cadeia de exceção bruta. Status e exceções continuam a chegar ao chamador; pedidos mantêm URL, UA e Referer originais. O helper público de apresentação redactUrl não foi substituído por uma política que escondesse todos os endereços da UI. Testes incluem servidor HTTP loopback, resposta403 e verificação de identidade enviada. Não representa auditoria universal de todos os logs da aplicação.
- **A15-03:** elegibilidade final do guia é reaplicada depois de integrar matches/fallbacks: fonte ativa, categoria visível e item visível. Usa os IDs ocultos já calculados pelo classificador de perfil infantil. Testes abrangem categoria oculta, categoria adulta, fonte inativa, item oculto e reimportação com ID novo. Não se removeu a validação adicional de reprodução.
- **A15-04:** preferências do restauro SETTINGS dependentes de perfil/lista só se ligam a identidades resolvidas. Contadores numéricos coincidentes deixam de servir de fallback para arranque, Customize PIN, pins de motor, preferências, quirks e associação de legendas. Chaves antigas por URL/ficheiro e espaços de fonte negativa mantêm tratamento próprio. Fontes iguais com ID diferente continuam a remapear. A reparação dos modos de arranque restringe-se aos perfis resolvidos. Entradas ignoradas são contadas e apresentadas no resumo da App com texto EN/PT/PT-PT.

O modo de arranque SPECIFIC_CHANNEL e o seu alvo são validados em conjunto: uma lista não resolvida não pode ativar esse modo aproveitando um alvo antigo da instalação. O teste cobre preservação de HOME/alvo anterior e remapeamento válido de ambos os campos.

Os remaps genéricos legados não foram reescritos globalmente: outros contratos/seções mantêm a compatibilidade existente. Este lote não promete resolver todos os possíveis casos de restauro CUSTOMIZE ou userData; a alteração refere-se aos percursos SETTINGS identificados na auditoria. Não muda o formato do backup.

## Lote 2

- **A15-05:** LiveResumeWatchdogs rearma os temporizadores existentes conforme o estado no momento da retoma. READY agenda uma nova janela saudável completa e supervisão de progresso; BUFFERING já iniciado agenda stall; início sem primeiro frame conserva o watchdog de abertura. Pausa ou orçamento esgotado não ganha reconexões por este helper. TuneHandler mantém a validação de geração/owner dos temporizadores. Os testes controlam fila e intervalos, pausa/retoma e invalidação da fonte. Não se criou uma segunda rotina de reconnect nem se repõe orçamento num READY breve.
- **A15-06:** externalPlayerFor(LIVE) usa externalPlayerLive. A opção visível TV Direto governa as gravações nos dois pontos de leitura existentes. A matriz Movies/Series/Episode conserva a semântica VOD anterior, e não foram alteradas proteções de gravações em curso/em partes. Teste cobre a matriz de preferências divergentes.
- **A15-07:** o HeroPreviewEngine cria MediaSource/fábrica HTTP para a geração atual em cada play; não fixa a fábrica antiga no player reutilizado. O renderer/decoder continua reutilizável sob a política anterior. A proteção de pedidos antigos permanece ativa. Teste verifica duas previews com a mesma identidade, stop→play e recusa de pedidos pelas fábricas anteriores. Não foi removido o fecho/claim de sessão nem reconstruído globalmente o ExoPlayer.

## Preservação

Os sete ficheiros principais anteriores às alterações foram copiados e os hashes verificados em `C:\Users\renat\AppData\Local\Temp\owntv-lotes12-20261015-before`, com manifesto. Preservadas alterações não commitadas existentes; sem reset/clean. Sem APK, commit ou publicação.

## Validação

Passagem final coordenada concluída: **1 255 testes Core + 549 testes Player + 404 testes App = 2 208 testes aprovados**, sem falhas, erros ou testes ignorados. Inclui 22 novos testes relativamente ao estado anterior. Compilação Kotlin debug/release, lintVitalStandardRelease e compilação dos testes Android Core/App aprovados. Inventários i18n Core/App e verificação de whitespace aprovados.

A primeira passagem App detetou o tipo Map em hiddenItems, quando o filtro esperava o conjunto de chaves. A ligação foi corrigida para hiddenItems.keys. A passagem final também inclui o teste e a validação conjunta de modo/alvo de arranque acrescentados na revisão. Não se apresenta o build falhado intermédio como validação final.

Avisos preexistentes sobre ResponseBody/reflexão de tipos nos testes e depreciações Gradle não foram ocultados nem confundidos com erros. Nenhum baseline/supressão foi adicionado para contornar lint.

Snapshot final atualizado: 997 ficheiros, SHA-256 `dbf78379616179416032d9f6492fdc5978a7e9b5cb184ad089dc16bb756f2387`. Mantida a versão personalizada; não é uma nova alegação de equivalência upstream.

Registo final: `C:\Users\renat\AppData\Local\Temp\owntv-lotes12-final-validation.log`, BUILD SUCCESSFUL. Registo inicial Core: `owntv-lotes12-core-validation.log`. Os testes de helpers/settings não são um ensaio de restauro completo com Room/ficheiros numa box. Os testes Android foram compilados, não executados num dispositivo; a renderização da pré-visualização e a reprodução real ainda precisam dos ensaios abaixo.

## Ensaios físicos após instalar o APK atualizado

1. Guardar um backup, escolher predefinição do fundo de legendas, exportar e restaurar Definições.
2. Restaurar só Definições entre instalações com listas/perfis diferentes; verificar opções locais preservadas e resumo das opções ignoradas. Repetir com identidades iguais e IDs diferentes.
3. Confirmar que canais de categorias ocultas não voltam ao guia após associação/fallback de EPG, incluindo perfil infantil.
4. ExoPlayer: pausar/retomar normalmente e durante buffering; confirmar pausa respeitada, recuperação limitada e ausência de tentativa de sessão antiga após zapping.
5. Ativar/desativar leitor externo de TV Direto e abrir gravação concluída. Confirmar proteção de gravações em curso/em partes.
6. Se usar Home com preview, alternar previews sequenciais com a mesma origem e regressar depois de stop/Home. Verificar imagem e libertação de sessão.

Nenhuma alteração deste lote garante que um fornecedor liberte imediatamente a sessão remota ao voltar a um canal.
