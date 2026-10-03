# OwnTV — implementação da revisão council para box e smartphone

Data: 2026-10-02. App e Core local atualizados. Relatório de origem: [AUDITORIA_COUNCIL_BOX_SMARTPHONE_REVISAO_2026-10-02.md](AUDITORIA_COUNCIL_BOX_SMARTPHONE_REVISAO_2026-10-02.md).

## Alterações

| Item | Resultado |
|---|---|
| R1 — pausa e recuperação | O controlador, os watchers Exo e a espera de abertura mpv deixam de consumir o orçamento ativo durante pausa deliberada. Play retoma o acompanhamento com o orçamento restante. Geração/seleção antiga não autoriza handoff. Retry-After conserva o prazo absoluto do fornecedor e os limites de crédito. |
| R2 — conectividade | Exceções de consulta/registo não terminam a observação partilhada. Registo limitado a três tentativas por subscrição; falha de permissão interrompe essas tentativas e conserva poll. Observação indeterminada não declara Internet disponível; tarifação desconhecida é conservadora. Cancelamento propaga. |
| R3 — alarmes | Falha de permissão no alarme exato tenta imediatamente o inexato. O agendamento retorna sucesso/falha e expõe IDs não armados até um rearm bem-sucedido ou cancelamento. Não promete pontualidade do alarme inexato. |
| R4 — fecho da fila | Revisão/proprietário ligam o fim do drain ao worker. Kick antes do fecho exige nova passagem; após fecho admite um único sucessor persistido. Arranque frio consulta WorkManager fora de Main. Conserva um escritor e as restrições de rede. |
| R5 — espaço físico | Reservas pendentes, scratch e deltas escritos são partilhados por volume. A quota lógica continua independente. Revisão cruzada detetou ainda a janela leitura StatFs→publicação; a validação final desta correção é registada abaixo. |
| R6 — arranque | Ocultar a barra lateral deixa de impedir a ação de arranque escolhida. A aplicação atrasada verifica input, perfil e player inativo para não substituir uma decisão posterior. |
| R7 — foco | Restauro de scroll/foco aborta perante input ou arrasto ativo e conserva cancelamento. Diálogos EPG pedem foco dentro da janela com tentativas limitadas; resultados tardios não retiram foco à pesquisa já utilizada. Animação programática bringIntoView continua distinta do arrasto do utilizador. |
| R8 — EPG oculto | Alterações fora do guia invalidam sem reconstruir a grelha. Ativação por destino/lifecycle carrega uma vez; desativação cancela leitura e conserva dados/cache. EPG do canal, diferidos, histórico e gravações permanecem disponíveis. |
| R9 — teclado/janela | Smartphone usa altura e coordenadas locais da janela; baseline é renovado quando mudam dimensões/origem. A política de TV e a sua estimativa OEM foram conservadas. A regressão aritmética de resize foi coberta por testes; multiwindow/animação no aparelho continuam por validar. |
| R10 — ensaios | Readiness Exo deriva da confirmação atual do renderer/áudio, sendo invalidada por fonte/surface/pausa/buffering/stop. mpv exige confirmação, reprodução e ausência de buffering/erro. Percurso TV abre canais também por OK; guia exige grelha não vazia carregada. |
| R11 — transição Exo→mpv | Stop aguarda drenagem HTTP rastreada, com limite existente de 1,5 s e cancelamento. Depois espera apenas a margem de decoder restante. Timeout recusa abrir mpv e cancela o alarme; nova seleção cancela o handoff anterior. Não acrescenta um atraso fixo a todos os zaps. |

Volume interno permanece a 100%, controlado pela box/sistema. Não foram alterados buffer, latência, qualidade, passthrough ou número de threads. Preservados Apenas HLS, setas de canais, modo simples, recuo, diferidos, EPG e gravações. Sem PiP ou novos favoritos/recentes.

## Revisão e testes

Três especialistas implementaram áreas separadas, com integração e validação pelo agente principal. A revisão cruzada final inclui interface/janela e concorrência de armazenamento/filas. Isto não substitui reprodução real numa box/smartphone.

Novos casos cobrem orçamentos de pausa, handoff/drain/timeout/cancelamento, registo e consulta de rede, fallback de alarme, handoff de filas e arranque frio, dois escritores e reserva física, foco/arrasto/cancelamento, EPG oculto, arranque invalidado por interação/perfil e geometria de janela.

Validação final concluída após a última revisão de armazenamento e a correção adicional de fecho HTTP no zapping: BUILD SUCCESSFUL em 5 min 28 s.

| Área | Testes | Falhas/erros/ignorados |
|---|---:|---:|
| App | 294 | 0 |
| Core | 995 | 0 |
| Player Core | 363 | 0 |
| **Total** | **1652** | **0** |

Compilação da app standardDebug e dos percursos baselineprofile benchmarkRelease/nonMinifiedRelease aprovada; estes últimos foram compilados, sem execução em aparelho. Lint do Player Core: zero erros e cinco avisos preexistentes. Lint completo da app não foi repetido nesta ronda; o relatório anterior contém quatro erros preexistentes (três RestrictedApi em MainActivity e um LocalContextGetResourceValueCall no EpgScreen). Não se declara Lint global limpo.

Inventários i18n com comparação ao baseline anterior aprovados: app 64 entradas baseline + 1167 técnicas; Core 310 + 5318. Baselines não aumentados. Verificações de locale numérico, overflow de texto e git diff --check aprovadas.

## Correção adicional — bloqueio após mudanças de canal

A mensagem `Previous stream requests did not finish within the handover deadline` é produzida pela OwnTV quando ainda existem pedidos da fonte anterior ao terminar o prazo de drenagem. Não demonstra uma recusa HTTP do fornecedor.

Foi identificado um caminho de fuga de resposta: no adaptador OkHttp do Media3 1.11.1, a interrupção da espera por headers cancela o Call, mas uma Response entregue em simultâneo pode ficar sem consumidor e sem fecho do corpo. Cancelar transporte não substitui o fecho desse corpo para concluir todos os callbacks de vida do pedido. Referência: [código oficial do adaptador Media3](https://raw.githubusercontent.com/androidx/media/1.11.1/libraries/datasource_okhttp/src/main/java/androidx/media3/datasource/okhttp/OkHttpDataSource.java), métodos executeCall e close.

A OwnTV passa a conservar a resposta antes de a entregar ao Media3. Ao retirar uma fonte, cancela os seus pedidos e fecha os corpos conhecidos; uma resposta tardia da fonte retirada é fechada antes de poder chegar ao leitor. Os contadores continuam a depender do término real dos pedidos, sem serem apagados para forçar a abertura do canal seguinte. Mantida a separação por motor/fonte e a verificação antes da nova ligação.

A regressão de 30 aberturas A→B→C→A usa HTTP local com corpo incompleto e deixa de fechar manualmente a resposta pelo leitor: o fecho deve ocorrer pela própria retirada da fonte. Também cobre resposta entregue após cancelamento, cancelamentos repetidos e continuação da limpeza quando um fecho falha. Os 48 testes focados de HTTP, controlador e identidade de sintonia passaram, seguidos da validação integrada indicada acima. Isto não equivale a reproduzir a race na Xiaomi ou testar uma playlist HLS real do fornecedor.

O registo detalhado acrescenta início/fim de DNS e início/fim/falha de ligação, com identidade de fonte e hora, sem domínio/IP ou URL completos nesses eventos. Em conjunto com os eventos HTTP existentes, permite distinguir uma resposta abandonada de uma resolução DNS/ligação ainda por terminar. Cancelamento durante DNS pode não produzir evento terminal dentro de 1,5 s; o prazo de segurança não foi removido para contornar esse caso.

Os contadores são apenas memória do processo; não sobrevivem a um reinício real da box. O relato de persistência após reinício não pode ser atribuído a esses contadores sem logs: pode ser nova ocorrência da mesma falha, APK anterior ou erro remoto independente. A correção visa o defeito local demonstrável; não certifica quando o fornecedor liberta uma sessão no servidor.

Logs desta ronda:

- `%TEMP%/owntv-council-integrated-validation.log` — primeira compilação/testes integrados.
- `%TEMP%/owntv-council-final-validation.log` — integração e Lint após ajustes de agendamento/readiness.
- `%TEMP%/owntv-council-zapping-final-validation.log` — validação integrada definitiva, incluindo revisão de espaço físico e fecho de respostas no zapping.
- `%TEMP%/owntv-council-storage-final-validation.log` — tentativa intermédia interrompida; não é usada como evidência de aprovação.

Backups dos principais ficheiros preexistentes:

- `%TEMP%/owntv-council-root-before-20261002-170247`.
- `%TEMP%/owntv-playback-r1-r10-r11-20261002-170307`.
- `%TEMP%/owntv-network-storage-before-20261002-170405`.
- `%TEMP%/owntv-r7-r8-before-20261002-170229`.
- `%TEMP%/owntv-http-body-retirement-20261002-220119` — estado anterior à correção adicional de zapping.

## Limites e ensaio recomendado

ADB não apresentou dispositivos ligados. Não foram executados Macrobenchmarks, gerados Baseline Profiles novos ou testados HDMI/AFR, foco áudio Android, suspensão, WorkManager/AlarmManager reais, multiwindow ou reprodução prolongada. Testes do gate de filas não certificam o agendamento físico do sistema.

Os callbacks do renderer demonstram confirmação no motor, sem certificar a apresentação no painel. O tag de EPG demonstra uma grelha de canais carregada, sem garantir que cada programa já foi obtido. Fecho HTTP local não certifica quando o fornecedor liberta a sessão remota. A reserva física pode ser conservadora perante commits tardios já refletidos por StatFs; reconcilia na observação seguinte. Outras apps podem consumir disco fora do controlo da OwnTV.

1. Box e smartphone: A→B→C→A repetido; Pause durante abertura por mais de 30 s, depois Play; Pause durante recuperação; Home/retoma. Confirmar ausência de erro ou novo pedido durante pausa e nenhuma seleção antiga interferindo.
2. Arranque com sidebar oculta e canal específico; navegar/mudar perfil durante a espera. Verificar zapping completo.
3. Fechar diálogos e navegar/arrastar imediatamente; EPG manual com resposta lenta; smartphone com teclado/rotação/janela dividida.
4. Atualizar XMLTV enquanto o canal toca, depois abrir EPG; validar histórico/diferidos e uma única reconstrução do guia.
5. Gravação agendada, revogação de alarme exato, inclusão de nova gravação na conclusão de outra e duas gravações perto da margem de disco; USB removido e cancelamento.
6. Canal saudável por 30–60 minutos, repetindo com lista/EPG e gravação ativos. Correlacionar cortes com áudio, HTTP/HLS, timestamps, foco/rota e Perfetto. As correções deste lote não comprovam eliminação dos cortes de áudio.

Não implementados: alteração especulativa de buffers/sink/threads; hipótese de reentrância de foco sem reprodução; expansão indiscriminada de recuperação da primeira abertura; correção 416 do download legado de filmes/episódios, fora da app de canais. A medição física é a fase que exige equipamento.

Código anterior preservado. Sem geração de APK, alteração de versão, commit ou push. Confirmada a configuração local owntv.corePath=C:/Users/renat/Downloads/OwnTV_Core; gerar o APK a partir de OwnTV-main no Android Studio inclui este Core local.
