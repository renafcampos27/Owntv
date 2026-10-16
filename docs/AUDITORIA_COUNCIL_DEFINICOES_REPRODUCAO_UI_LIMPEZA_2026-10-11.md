# OwnTV TV — auditoria especializada e plano de melhorias

Data: 11/10/2026. Estado: análise; implementação não iniciada neste trabalho.

Continuação: as recomendações aprovadas foram implementadas nas quatro fases descritas em [IMPLEMENTACAO_FASES_AUDITORIA_2026-10-11.md](IMPLEMENTACAO_FASES_AUDITORIA_2026-10-11.md). O texto abaixo conserva o estado e a evidência da auditoria anterior à implementação.

## Âmbito e método

Foram analisados o código atual de `C:\Users\renat\Downloads\OwnTV-main` e o Core personalizado em `C:\Users\renat\Downloads\OwnTV_Core`. Participaram especialistas em reprodução, definições e interface/desempenho, seguidos de revisão crítica independente do council e verificação dos percursos principais pelo agente coordenador.

Não foram alterados fontes da aplicação/Core, preferências, bases de dados ou dependências. Não foram executados novos testes, builds, medições de desempenho ou ensaios em boxes nesta auditoria. As alterações anteriores existentes no workspace foram preservadas. OwnTV Mobile e o seu Core privado ficam fora do âmbito.

“Confirmado no código” significa que a sequência e o efeito decorrem do código observado; não significa que a frequência ou o sintoma tenham sido reproduzidos num dispositivo. P1 identifica correções prioritárias; P2, inconsistências e melhorias relevantes; P3, otimização/limpeza sem urgência demonstrada. Não foi demonstrado um novo P0.

## A. Correções de reprodução prioritárias

### R1 — Callback antigo pode parar uma reprodução nova — P1

Evidência: `OwnTV_Core/player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt:1920–1932`, `:2323–2349` e `:1046–1048`.

Na passagem mpv→Exo, a captura PixelCopy executa uma continuação que agenda `stop`, `vo=null` e `detachSurface`. A geração só é verificada depois dessas ações. Se outra reprodução começar enquanto a captura espera, a operação antiga pode modificar o motor que já está a servir a nova sessão. A publicação da imagem também não verifica a identidade da sessão/superfície.

Correção mínima: associar captura e handoff à geração, instância do motor e superfície proprietárias. Revalidar antes de publicar a imagem, antes de agendar ações destrutivas e dentro da fila nativa, imediatamente antes de as executar. Uma verificação apenas antes de entrar na fila deixa outra janela de corrida. Não publicar bitmaps obsoletos; preservar o contrato de vida do bitmap durante PixelCopy e executar a continuação válida uma única vez.

Aceitação: captura de A pendente, abrir B, chegar callback/timeout de A → B não recebe stop, detach, imagem antiga ou mudança de estado. Repetir com geração alterada entre o agendamento e a execução nativa; sucesso tardio depois de timeout não duplica o handoff.

Âmbito: leitor OwnTVPlayer de diferidos/gravações e trocas de motor/legendas. Não demonstra a causa do bloqueio A→B→A em TV em direto.

### R2 — Comparar só o URL não impede A₁→B₂→A₃ — P1

Evidência: `OwnTVPlayer.kt:2196–2202` e `:2402–2406`, em `toggleVodEngine` e `revertToMpv`.

Depois do atraso de libertação do decoder, algumas tarefas só comparam `currentUrl` com o URL antigo. Ao regressar ao mesmo URL, a condição volta a ser verdadeira, embora a tarefa pertença a uma sessão anterior. Pode reiniciar o conteúdo com posição/estado antigos.

Correção: identidade de sessão/geração, job de handoff com proprietário e cancelamento ao ser substituído. Cancelamento e validação de identidade complementam-se. Preservar pausa, posição e metadados apenas da sessão atual.

Aceitação: A→B→A com a tarefa de A₁ pendente; duas trocas rápidas de motor; saída durante o atraso. Só a continuação atual pode carregar ou modificar a superfície. Não resolver com mais milissegundos de espera.

### R3 — Remover o mosaico audível não transfere realmente o som — P1

Evidência: App `features/multiview/MultiviewState.kt:215–226`; Core `player/LiveEnginePool.kt:86–88`.

`clear()` muda o índice audível da interface. O pool liberta o motor e deixa o seu tile audível em null; os sobreviventes continuam silenciados. O distintivo pode indicar som num canal que permanece mudo.

Correção: escolher um sobrevivente válido e chamar `pool.giveSoundTo(next)`; sem sobreviventes, usar null. Manter estado visual e pool consistentes.

Aceitação: remover o primeiro de dois mosaicos quando tem som → o segundo recebe áudio; remover o último → nenhum motor audível; verificar sobrevivente em modo áudio e exatamente um motor audível.

## B. Definições que exigem correção ou maior clareza

### S1 — Alterações de decoder/idioma não chegam ao Exo ativo em diferido — P2

Evidência: Core `OwnTVPlayer.kt:1134–1137`, `:1204–1215`, `:1960–1968`; `ExoSubtitleEngine.kt:351–363` e `:750–759`.

O arranque Exo recebe hardware/software, áudio por software e idiomas. Os observadores durante reprodução atualizam principalmente o mpv ou os campos locais; não aplicam todas essas alterações ao Exo já ativo. `applyRenderConfig` também deve respeitar qual motor possui a apresentação.

O percurso é acessível: App `features/shell/OwnTVShell.kt:812–819` e `:834` abrem Definições por atalho mantendo a reprodução em MINI; `:746–750` só muda o modo, `:919` não pára MINI e `:1461–1494` mantém a superfície. Pela saída normal com Voltar (`:630–641`), o leitor é parado e a próxima abertura já recebe as opções.

Correção preferida, com menor risco: explicitar “Aplica-se na próxima abertura” para alterações que requerem recriar o renderer; separar configuração de render por motor. Idiomas podem ser atualizados por track selection sem recriar a ligação. Se for adotada aplicação imediata de decoder, fazer uma única recriação controlada, preservando posição, pausa e identidade; não efetuar um restart por cada preferência emitida.

Aceitação: diferido Exo → atalho Definições → alteração; a interface informa corretamente o momento de aplicação e o decoder efetivo corresponde depois da abertura seguinte. Testar alternativa de aplicação imediata apenas se escolhida. Ecrã preto por esta falha não foi reproduzido.

### S2 — HLS exclusivo pode prevalecer sobre o motor selecionado — P2 de apresentação

Evidência: Core `core/settings/ChannelPlaybackConfig.kt:33–38`, `LiveTuneController.kt:184–190`; App `VideoPlayerSettingsScreen.kt:1478–1483`.

É política deliberada e documentada: HLS exclusivo seleciona Exo, salvo exceção mpv explícita por canal. Uma escolha mpv por lista pode parecer sem efeito.

Correção: mostrar motor pedido, motor efetivo e motivo no seletor; explicar a exceção por canal. Não remover a exclusividade HLS nem mudar silenciosamente a política.

### S3 — HDR aparenta abrangência superior à implementada — P2 de apresentação

Evidência: Core `OwnTVPlayer.kt:1130–1132` e `:2540`, aplicação de `target-colorspace-hint`; não há consumo equivalente de `hdrEnabled` nos motores Exo analisados.

Correção: identificar a opção como sinalização HDR do mpv e a sua não aplicabilidade no Exo. Desligar sinalização não equivale a converter universalmente HDR em SDR. Não adicionar tone mapping por software global, especialmente em boxes com poucos recursos.

### S4 — Hardware desativado é preferência por software com fallback — P2 de apresentação

Evidência: Core `ExoRenderers.kt:41–63` e `LivePreviewEngine.kt:3615`.

O código ordena candidatos software primeiro, mantendo hardware disponível. Isto é deliberado para compatibilidade. O decoder efetivo pode continuar hardware se não existir alternativa adequada.

Correção: usar “Preferir descodificação por software”, mostrar pedido versus decoder efetivo e explicar o fallback. Não eliminar hardware da lista globalmente. Áudio por software é uma preferência específica do Exo; mpv já usa o seu percurso de áudio por software.

## C. Diagnóstico, fluidez e consultas

### U1 — Multiview confunde demora com limite de hardware — P2

Evidência: App `MultiviewScreen.kt:206–214`, `:359`; `MultiviewState.kt:190–197`.

Doze segundos com `isPlaying=false` e sem erro explícito fazem recusar o mosaico como limite do dispositivo. Isto não distingue arranque, buffering e rede de falha de decoder.

Correção: classificar com estados/eventos reais. Se o orçamento expirar sem causa específica, usar uma mensagem neutra e uma ação de nova tentativa. Não aumentar globalmente o orçamento nem esperar indefinidamente. Testar origem lenta, buffering após início, falha HTTP e erro real de decoder.

`LiveTuneController.playTile:327–340` não usa o intervalo de reabertura do leitor principal. Rever esse percurso separadamente com testes; não atribuir este caso automaticamente à opção de 20–50 segundos.

### U2 — Animações continuam a existir no modo Áudio — P3

Evidência: App `player/AudioNowPlayingBar.kt:348–375`.

Pulsação e equalizador constroem transições infinitas sem respeitar a política de animações desligadas. O equalizador cria-as mesmo pausado.

Correção: não construir transições com animações OFF ou reprodução pausada; usar desenho estático. Impacto restrito ao modo Áudio. Não há prova de que isto cause cortes HLS normais. Validar ausência de animações/temporizadores no estado fixo.

### U3 — Guardar scroll faz a raiz observar mudanças do índice — P3

Evidência: App `features/live/LiveScreen.kt:268–269`.

`LaunchedEffect(firstVisibleItemIndex)` lê o índice em composição apenas para o guardar. Mudar o índice invalida esse observador na raiz.

Correção: efeito identificado pelo listState/categoria, com `snapshotFlow` e `distinctUntilChanged` para guardar índices. Preservar restauro por categoria e troca de lista. Medir recomposições e fluidez; não prometer ganhos de 2×/4× sem ensaio.

### U4 — Navegação estática ainda observa capacidades dinâmicas — P3

Evidência: Core `core/nav/NavVisibility.kt:46–100`.

STATIC combina `dynamicCaps()` embora ignore o resultado; capacidades ainda observam contagens de filmes/séries que não contribuem para a navegação TV atual.

Correção: branch STATIC dependente só das preferências. Na configuração TV, observar apenas capacidades necessárias, preservando contratos e consumidores do Core. Validar que alterações em catálogos irrelevantes não reconsultam a navegação, enquanto canais continuam a atualizar LIVE/EPG em modo dinâmico.

## D. Limpeza e consistência de release

### C1 — Wiring App de meteorologia e trailers sem uso efetivo — P3

Meteorologia: Core `SettingsRepository.kt:825` força false; App mantém passagens em `ShellViewModel.kt:319–332`, `MainActivity.kt:286–287,492–493` e `TopBar.kt:210–289`. São ligações residuais; não foram demonstrados pedidos HTTP meteorológicos ocultos.

Trailers: App `HomeScreen.kt:390,540–542` depende de trending; `HomeViewModel.kt:309–311` usa feed liveOnly; Core `HomeFeed.kt:190–199,314–324` produz conjuntos VOD/trending vazios nessa configuração. A biblioteca `youtube.player` permanece em App `build.gradle.kts:417`.

Correção: retirar componentes, parâmetros, callbacks e dependência exclusivos desses percursos na App após pesquisa final de usos. Manter chaves antigas de backup/migração e serviços compartilhados do Core. Não apagar downloads, gravações, arquivo, DAOs ou enums só por conterem palavras movie/series. Medir tamanho/build se se quiser quantificar benefício.

### C2 — Core personalizado local não está identificado de forma reproduzível — validação de release prioritária

Evidência: TV `gradle/libs.versions.toml:76,80–81` e Core `build.gradle.kts:14` indicam 1.0.57. TV `settings.gradle.kts:57` substitui dependências pelo Core local através de `owntv.corePath`. CI `.github/actions/resolve-core/action.yml` resolve artefactos publicados ou clona `core-1.0.57`, não o conteúdo local personalizado por definição.

O Core local contém alterações adicionais. A divergência de resolução é confirmada; não foi executado build limpo/CI para demonstrar qual falha concreta ocorre ou verificar o conteúdo remoto da tag.

Correção: fixar uma revisão própria verificável, alinhar resolução local/CI e registar commit/versão efetivos do Core e libmpv. A anotação de equivalência parcial a upstream não substitui identidade de build. Não subir cegamente a versão para uma tag upstream nem publicar neste trabalho.

Aceitação: checkout limpo resolve o Core pretendido, compila APIs e identifica os mesmos commits/dependências que o build local. Deve estar resolvido antes de declarar uma release reproduzível.

## E. Inovações leves aprovadas para fase posterior

1. **Estado de espera explicável:** “A aguardar reabertura: X s”, “A ligar” e “A recuperar ligação”, apenas quando corresponderem ao estado real. Reutilizar dados do controlador; atualização no máximo uma vez por segundo e apenas visível; cancelamento ao mudar de sessão. Sem nova ligação de teste.
2. **EPG com estado por linha:** distinguir carregamento, vazio e erro; “OK para tentar novamente” relê só a linha falhada. Aproveitar a cache existente e as suas gerações; não invalidar o guia inteiro.
3. **Definições transparentes:** indicar âmbito, momento de aplicação e valor efetivo nas opções em conflito. Evitar uma segunda fonte de preferências: calcular o resultado pelo mesmo resolvedor que a reprodução utiliza.

## F. O que não implementar com a evidência atual

- Mais buffer, delays universais ou reinícios para corrigir continuations antigas. É um problema de identidade/propriedade das operações.
- Limite rígido que interrompa segmentos HLS só porque o buffer ultrapassou o limiar configurado. Segmentos inteiros podem fazer exceder um limiar de decisão de carregamento; duração em memória, limite de bytes e atraso face ao direto são diferentes.
- Remover fallback hardware ou aplicar flags MPEG-TS/HLS de compatibilidade a todos os canais.
- Declarar fuga de Multiview em ON_STOP: existe libertação em `OwnTVShell.kt:669–689`.
- Repetir a otimização de desenho de `FocusableSurface` ou criar outra cache EPG: as proteções anteriores já existem.
- Declarar OOM de logos apenas por `AsyncImage(model=url)`: os usos observados têm constraints finitas; resolução/tamanho precisam medir.
- Declarar que esta auditoria resolveu o bloqueio Live A→B→A do fornecedor. A proteção de geração encontrada aqui pertence a outros percursos e não substitui diagnóstico do acesso Live.
- Novos favoritos, histórico, controlos de volume, funcionalidades smartphone ou remoção de Multiview. Preservar volume da aplicação a 100%, diferidos, gravações e configurações por canal.

## G. Ordem de implementação recomendada pelo council

| Fase | Conteúdo | Validação necessária |
|---|---|---|
| 1 | R1, R2 e R3: identidade de handoffs e transferência de áudio | Testes determinísticos de callbacks/filas/gerações e estado do pool; build focado; ensaios de diferidos/legendas/Multiview |
| 2 | S1–S4 e U1: aplicação/abrangência das definições e mensagens corretas | Atalho para Definições com MINI Exo ativo; próximo arranque; motor efetivo; origens lentas e erros distintos |
| 3 | U2–U4: remover trabalho desnecessário da interface/navegação | Restauro de scroll/foco; métricas de recomposição; observação de consultas; modo áudio pausado |
| 4 | C1 e inovações E: limpeza App e estados úteis | Pesquisa de usos, build/lint, guia com erro/retentativa local, regressão de downloads e gravações |
| Antes da release | C2: alinhar identidade/resolução do Core | Build limpo com o Core efetivamente distribuído; proveniência documentada |

Depois: validação em Android TV real de zapping, regresso à lista/foco, pausa, suspensão/retoma, arquivo, gravações, legendas e áudio. Compilação e testes automatizados não certificam comportamento de MediaCodec, rede do fornecedor ou apresentação no aparelho.

## Referências técnicas consultadas

- [Android — fases e desempenho do Compose](https://developer.android.com/develop/ui/compose/performance/phases): suporta adiar leituras frequentes e evitar recomposição sem necessidade. Não prova o ganho concreto na OwnTV.
- [Media3 — DefaultLoadControl](https://developer.android.com/reference/androidx/media3/exoplayer/DefaultLoadControl): distingue decisões de continuar carregamento/iniciar reprodução e a configuração do controlo de carga. Não transforma o máximo configurado num corte exato de cada segmento.
- [mpv — target-colorspace-hint](https://mpv.io/manual/stable/#options-target-colorspace-hint): enquadra a opção de sinalização do espaço de cor do motor; não é uma opção global de conversão HDR→SDR do ExoPlayer.
