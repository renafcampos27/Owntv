# OwnTV TV — nova auditoria de robustez e usabilidade

Data: 11/10/2026. Estado: análise; nenhuma correção desta ronda implementada.

Âmbito: `C:\Users\renat\Downloads\OwnTV-main` e Core personalizado em `C:\Users\renat\Downloads\OwnTV_Core`. Mobile excluído. Foram lidos os dois relatórios de implementação desta data para evitar repetir trabalho concluído.

Método: três especialistas em reprodução/áudio, EPG/gravações/backup e interface/definições; verificação dos percursos pelo agente principal; revisão crítica independente do council. As referências de linha correspondem ao código observado nesta data. São provas de comportamento possível no código, não reprodução em dispositivo nem medições de desempenho.

## Achados e correções mínimas

### A1 — Surround pode recarregar o motor errado [P1 condicionado]

Evidência Core: `player-core/.../OwnTVPlayer.kt:1166–1180`, `:870–875`, `:2918`, `:3054`, `:3124`.

Com Exo ativo e mpv inicializado, mudar Surround chama `reloadCurrentInPlace()` sem distinguir o proprietário da reprodução. Este helper usa `loadUrl(resetRetries=false)`: o percurso não desativa Exo nem inicia uma reconstrução Exo, mas pode iniciar mpv. A abertura de um segundo motor é um risco de ligação concorrente e de estado/áudio inconsistentes; não foi reproduzida em box nesta ronda.

Correção: aplicar a opção ao motor ativo. Se Exo exigir reconstrução, reutilizar o percurso Exo conservando geração, posição, pausa, descritor do programa/gravação e superfície. mpv só deve recarregar quando é o proprietário. Não alterar volume de 100%.

Aceitação: Exo exclusivo/preferido, gravação e diferido, em pausa e em reprodução; uma única cadeia de reprodução, nenhuma chamada de arranque mpv no ajuste Exo, posição e pausa preservadas.

### A2 — Retry perde as alternativas do diferido [P2]

Evidência: App `player/PlayerHud.kt:772`; Core `OwnTVPlayer.kt:3747`, `:3769`, `:3787`, `:2980–2981`.

“Tentar novamente” reinicializa o item sem passar `archiveAlternatives` nem `archiveManifestTypes`. Os defaults vazios substituem as alternativas específicas do programa por dedução genérica de formatos. Isto pode tornar uma segunda tentativa menos capaz que a abertura original.

Correção: capturar e transmitir o descritor completo aos caminhos de URL direto e URL renovado. Reiniciar o orçamento de tentativas sem apagar o contexto do programa.

Aceitação: URL A falha, alternativa explícita B com tipo não dedutível de A é conservada após retry; repetir com URL renovado e com cancelamento por stop/zapping. Não testar com um fornecedor real como única prova.

### A3 — Live→mpv perde o tipo declarado do manifest [P2, contexto específico]

Evidência Core: `LiveTuneController.kt:900`, `LiveEngines.kt:153–171`, `OwnTVPlayer.kt:771`, `:835`, `:3475`.

O pedido contém `manifestType`, mas o adaptador mpv não o transmite. Um endereço `.ts` declarado HLS, sem HLS exclusivo nem quirk aprendido, pode receber a política de probe/reconexão de TS contínuo. Não equivale a afirmar que todos os canais HLS falham.

Correção: transmitir o tipo e incluí-lo na classificação efetiva usada por mpv. Separar tipo de conteúdo e preferência HLS exclusivo; manter TS verdadeiro intacto.

Aceitação: HLS declarado num URL `.ts` e host novo; TS sem declaração; `.m3u8`; host aprendido. Verificar argumentos de probe/reconexão, sem impor conversão global de formato.

### A4 — Ajuste em lote apaga configurações TS legadas [P2]

Evidência Core: `core/.../settings/ChannelPlaybackConfig.kt:91–97`, `SettingsRepository.kt:1462–1487`; App `ChannelPlaybackSettingsDialog.kt:715`.

Na leitura, `includeLegacy` recupera TS/Exo de `ManualTsChannel`. Na escrita em lote, a base considera apenas linhas modernas e remove a referência legada do alvo. Aplicar somente um atraso de áudio pode apagar formato e motor, apesar da promessa de conservar os restantes campos.

Correção: formar a base efetiva com `includeLegacy` dentro da mesma transação DataStore e migrar os alvos preservando campos não selecionados. Conservar leitura/importação de backups antigos.

Aceitação: configuração apenas legada; legado com linha moderna parcial; alteração só de áudio/reserva; reposição deliberada de formato/motor para herança; canais fora do lote intocados.

### A5 — Seleção em lote está limitada silenciosamente [P2]

Evidência App: `SettingsViewModel.kt:538`, `:668–673`; `ChannelPlaybackSettingsDialog.kt:61`, `:298–300`, `:602`. Core: `ChannelDao.kt:368`.

O seletor reutiliza a procura do canal de arranque: consulta 500 e conserva 300. Categoria e “Selecionar todos” operam depois deste corte. Uma categoria posterior ao prefixo alfabético pode aparecer vazia ou incompleta. O problema é alcance da operação, não prova de consumo excessivo de memória.

Correção: separar procura limitada de um canal e resolução dos alvos do lote. Resolver fonte/categoria/pesquisa/exclusões no repositório; paginar a apresentação e confirmar o total real. Reutilizar a seleção resolvida na escrita, evitando que a confirmação e aplicação representem conjuntos diferentes.

Aceitação: fonte >500 canais; categoria fora dos primeiros resultados; ocultos; pesquisa anterior; confirmação e escrita correspondem exatamente aos alvos apresentados.

### A6 — Seletor pode pedir foco numa opção não composta [P2]

Evidência App: `VideoPlayerSettingsScreen.kt:2137–2142`, `:2170–2175`, `:1428–1435`; Core `ChannelReopenInterval.kt:5`.

O seletor pede foco na linha selecionada após 80 ms, mas a LazyColumn começa no topo, sem estado posicionado nessa linha. No intervalo 10–50, o valor inicial 20 fica longe da área inicial. A exceção de foco é absorvida. O fallback visual concreto ainda requer teste com comando.

Correção: posicionar `LazyListState` no índice selecionado e pedir foco quando a linha estiver composta. Definir fallback para lista vazia/seleção inexistente e restaurar foco ao fechar. Não substituir 80 ms por outro atraso arbitrário.

Aceitação: valores 10/20/50, seleção visível e focada, OK aplica a opção focada, Voltar regressa ao controlo de origem; pesquisa sem resultados.

### A7 — Leitura tardia pode substituir o zoom escolhido no Exo Live [P2]

Evidência Core: `LivePreviewEngine.kt:2895–2909`, `:2939–2942`; App `PlayerHud.kt:858`.

O restauro assíncrono verifica identidade da sintonia e revisão do áudio, mas não revisão do zoom. Se o utilizador mudar o zoom antes de terminar a leitura da preferência antiga, essa leitura pode sobrescrever a escolha nova no mesmo canal. O guard de OwnTVPlayer já foi corrigido; este é outro motor/percurso.

Correção: revisão independente de zoom capturada antes da leitura e avançada em escolhas explícitas do utilizador. Restaurar apenas quando identidade e revisão coincidirem; não acoplar a revisão de áudio.

Aceitação: DAO atrasado, mudar zoom antes de emitir, escolha nova conservada; outro canal não recebe o restauro antigo; áudio não bloqueia restauro legítimo de zoom.

### A8 — Editor por canal pode guardar um draft inicial vazio [P2, corrida por reproduzir]

Evidência App: `SettingsViewModel.kt:528`; `ChannelPlaybackSettingsDialog.kt:52`, `:134`, `:267`; Core `SettingsRepository.kt:1448–1451`.

As configurações começam como lista vazia. O editor transforma ausência em defaults e inicializa o draft uma vez por canal; emissão persistida posterior não atualiza esse draft. Guardá-lo pode substituir opções existentes. A subscrição do ecrã anterior normalmente já terá carregado os dados: frequência e interleaving não foram demonstrados.

Correção: distinguir carregamento de ausência real e inicializar uma vez após leitura válida para canal/perfil. Não usar simplesmente `remember(channel.id, initial)`, pois emissões posteriores podem apagar uma edição em curso.

Aceitação: emissão de configurações deliberadamente atrasada; editor aguarda ou recebe as opções completas; mudar só áudio conserva motor/formato/reserva; alterações locais resistem a emissões posteriores.

### A9 — Exportação remota pode abrir o servidor após fechar o painel [P2]

Evidência App: `BackupViewModel.kt:43–55`, `RemoteBackupRestoreScreen.kt:160`, `BackupScreen.kt:336`; Core `CompanionController.kt:83`, `:263–266`.

Fechar o painel chama apenas `companion.stop()`. A exportação continua no ViewModel e, ao terminar, inicia o servidor de download, mesmo sem painel PIN/QR aberto. Uma conclusão antiga também pode substituir o estado de uma nova exportação. Não se conclui daqui que o servidor esteja sem autenticação.

Correção: job e geração próprios da exportação; invalidar ao fechar/substituir; verificar propriedade e atividade antes de iniciar servidor/publicar resultado. Cancelamento isolado não basta perante trabalho bloqueante. A limpeza de uma operação antiga não pode parar uma sessão nova. Definir política para o ficheiro de cache cancelado sem o servir automaticamente.

Aceitação: terminar A depois de fechar; A/B concluem fora de ordem; falha antiga depois de sucesso novo; exportação normal inicia uma vez e fechar termina o seu servidor.

## Fases recomendadas

Decisão do council: incluir os nove percursos; A1 é P1 condicionado à propriedade Exo com mpv já inicializado, os restantes são P2. Não classificar nenhum como P0. A8 exige reprodução controlada do carregamento tardio; o sintoma de A6 exige validação com D-pad. No seletor pesquisável, conservar o foco inicial na pesquisa; posicionar a opção selecionada sem a forçar como alvo inicial nessa modalidade. O reconhecimento automático de mpv pode compensar A3 em alguns streams, mas não justifica perder o hint configurado.

1. **Reprodução e descritor completo:** A1, A2 e A3. Uma reprodução por proprietário; retry preserva o contexto; formato declarado chega ao motor.
2. **Integridade das preferências:** A4 e A7; reproduzir A8 com emissão atrasada e aplicar guarda de carregamento. Ajustes não podem apagar outros campos nem ser revertidos por leituras antigas.
3. **Seleção e comando:** A5 e A6. Total real do lote, opção atual visível/focada, regresso previsível.
4. **Operações remotas e diagnóstico:** A9; melhorias abaixo apenas após validação das correções.

Cada fase deve ter testes de regressão dirigidos, compilação/lint relevantes e validação Android TV. Alterações no Core exigem atualizar/verificar o snapshot local usado pela App. Esta auditoria não executou novos testes nem gerou APK.

## Melhorias úteis sem aumentar o trabalho da reprodução

- **Diagnóstico de CPU/memória usando amostras existentes:** `UiPerformanceMonitor.kt:54` já mede CPU por janela, heap Java/nativo e GC a cada dez segundos. Expor estes escalares no resumo sem novo polling. Distinguir parcelas Java/nativo de memória total; não inferir fuga de memória nem causa de jank sem série temporal.
- **Pré-visualização do lote:** fonte, categoria, pesquisa, exclusões e total real antes de aplicar; usar o mesmo conjunto resolvido na confirmação e escrita.
- **Origem da configuração efetiva:** proposta já pendente do lote anterior, não novidade implementada. Mostrar compacto “Canal / Lista / Geral” para motor/formato/buffer, a partir do resolvedor existente, sem criar outro sistema de preferências.

## Limpeza e propostas excluídas

- Extrair a procura limitada de canal de arranque do contrato de seleção em lote. Isto corrige responsabilidade misturada; não exige eliminar APIs de backup nem colunas antigas.
- Defeitos encontrados no DownloadManager/DownloadEngine de filmes/séries não alcançam os downloads EPG, que usam RecordingManager/RecordingEngine. Não priorizar esse código legado como causa dos problemas TV atuais.
- Não recomendar buffer maior, delay global, reconnect adicional, User-Agent novo, flags MPEG-TS universais nem atualizar bibliotecas sem ligação a um achado.
- Não apresentar os problemas A–B–A do fornecedor como resolvidos por estas alterações. Não prometer ganhos de fluidez quantitativos nem ausência total de falhas.

## Referências primárias

- [Android: listas lazy e LazyListState](https://developer.android.com/develop/ui/compose/lists): posicionar a lista explicitamente; uma linha não visível pode ainda não estar composta.
- [Android: restauro do foco](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-restoration): foco e regresso a listas devem ter identidade estável.
- [Media3: reprodução HLS](https://developer.android.com/media/media3/exoplayer/hls): declarar o tipo quando o URI não identifica HLS. Esta orientação reforça a preservação do tipo; a política específica de mpv precisa dos testes acima.
- [Media3: resolução de problemas](https://developer.android.com/media/media3/exoplayer/troubleshooting) e [diretos](https://developer.android.com/media/media3/exoplayer/live-streaming): configurações de buffer, latência e compatibilidade são distintas e devem seguir o problema observado.
