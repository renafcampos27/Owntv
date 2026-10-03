# OwnTV — correções e otimizações para box e smartphone

Data: 2026-10-02. Análise do checkout atual OwnTV-main e OwnTV_Core. Não foram alterados ficheiros de código nem gerados APKs nesta ronda. Inspeção dirigida de código e documentação oficial; sem ensaio físico, medição de desempenho ou reprodução de falhas no dispositivo. As alterações anteriores existentes no checkout foram preservadas.

## Decisão recomendada

Priorizar trabalho desnecessário no shell, propriedade da superfície mpv, observação de rede e foco dos diálogos. Depois medir percursos reais em builds release e ajustar apenas os gargalos observados. Preservar volume interno a 100%, setas para mudar canais, HLS-only, EPG, diferidos, gravações e rewind. Não acrescentar PiP, favoritos ou últimos canais.

## A1 — módulos de filmes e séries continuam ativos no shell

**Confirmado no código; ganho por medir.** `OwnTVShell.kt:244–245` cria MovieViewModel e SeriesViewModel incondicionalmente. Ambos têm vários `stateIn(SharingStarted.Eagerly)`, observações do player e tarefas que acordam de 10 em 10 segundos (`MovieViewModel.kt:341`, `SeriesViewModel.kt:368`). Assim, retirar as opções visuais não desativa todo o trabalho desses módulos.

**Impacto:** observações, objetos e acordares desnecessários numa utilização centrada em canais. Os métodos de gravação de progresso podem retornar sem escrever quando não há filme/episódio; não afirmar que existe escrita em disco a cada tick ou que este trabalho causa os cortes de áudio.

**Solução:** separar os componentes por destino e instanciar apenas os necessários; eliminar as dependências incondicionais dos callbacks comuns depois de verificar os usos. Manter a persistência de posição dos diferidos/gravações que realmente precisam dela. Não apagar tabelas ou dados antigos para obter esta otimização.

**Aceitação:** utilização só de canais sem criação dos dois VMs, sem os respetivos loops; EPG, rewind e gravações mantêm funcionamento. Comparar arranque, PSS e frames lentos antes/depois.

## A2 — desligamento mpv sem identidade da superfície

**Inconsistência confirmada; manifestação por reproduzir.** `MpvVideoSurface.kt:75` chama `player.detachSurface()` sem argumento. OwnTVPlayer já oferece `detachSurface(surface)` com comparação de identidade (`OwnTVPlayer.kt:3816`); ExoPreviewSurface já usa proteção equivalente.

**Risco:** se a destruição da superfície antiga chegar depois de uma nova ligação, pode remover a ligação nova. O shell mantém o mesmo ponto de composição entre mini-player e fullscreen; por isso essa transição, isoladamente, não demonstra a corrida. Testar também rotação e recriação forçada da superfície.

**Solução:** usar a superfície concreta no desligamento do callback e testar a ordem attach(A), attach(B), destroy(A). Rever separadamente os comandos assíncronos de dimensão; evitar alterações globais de motor ou troca para TextureView.

**Aceitação:** destroy(A) deixa B ligado; destroy(B) desliga B; ensaios de rotação, mini/fullscreen e recriação sem imagem negra persistente. Este achado pertence ao anfitrião mpv, incluindo eventual handoff Exo; não prova a causa de uma falha no Live ExoPreviewSurface.

## A3 — observação de rede sujeita a corrida

**Padrão confirmado.** `ConnectivityObserver.kt:63–67` consulta `activeNetwork/getNetworkCapabilities()` nos callbacks, incluindo onAvailable. A documentação Android desaconselha consultas síncronas aí: os resultados podem não corresponder à transição que está a ser notificada. O fluxo publica apenas Boolean e deduplica-o, não distinguindo Wi-Fi→dados móveis quando ambos continuam online.

**Solução:** observar a rede predefinida nas versões suportadas; guardar identidade e capacidades fornecidas pelo callback, com serialização das atualizações; tratar onLost apenas para a rede correspondente. Separar estado de interface, validação Android e falha HTTP do fornecedor. Partilhar o observador entre consumidores para evitar callbacks/polling redundantes.

**Limites:** a rede mudar não obriga a reiniciar um stream saudável. A validação Android também não prova que o servidor IPTV está acessível. Disparar recuperação apenas segundo o estado e a identidade da sintonia, sem duplicar a recuperação já existente.

**Aceitação:** Wi-Fi→móvel e Ethernet→Wi-Fi sem estados de interfaces antigas a sobrepor a atual; pausa manual continua pausada; recuperação não ignora Retry-After.

Referências: https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback e https://developer.android.com/develop/connectivity/network-ops/reading-network-state

## A4 — foco ainda depende de uma única tentativa em alguns percursos

**Confirmado no código; falha visual por reproduzir.** `TextInputDialog.kt` espera 80 ms e faz um único requestFocus dentro de runCatching. CatchupListDialog em LiveScreen faz o mesmo com 60 ms. OwnTVShell também pede foco imediatamente depois de mudar playerMode em callbacks. Estes pontos não receberam todos a política de confirmação usada na lista principal.

**Solução:** pedido associado ao alvo e à composição efetivamente pronta, com tentativas limitadas e resultado verificado. Cancelar se o utilizador já tocou/navegou, o alvo desapareceu ou o diálogo fechou. Não substituir por sleeps maiores nem por retries infinitos. Rever cada percurso: restoreFocus pode já fornecer um segundo caminho e não se deve duplicá-lo.

**Aceitação:** renomear, abrir diferidos, sair do player e regressar à lista com D-pad; teclado e toque sem foco roubado. Testar também dispositivo lento e lista vazia.

## A5 — perfil de otimização não valida os percursos atuais

**Cobertura limitada confirmada.** BaselineProfileGenerator percorre seis itens fixos com D-pad e não afirma que entrou no ecrã desejado ou que abriu um canal. Já existem baselineprofile e profileinstaller: a proposta é melhorar a cobertura, não adicioná-los novamente.

**Solução:** percursos separados e verificáveis para TV e toque: arranque com fonte configurada, lista simples, scrolling, EPG, abrir canal, zapping, voltar à lista e gravações. Usar fixture local sem credenciais. Gerar e verificar o perfil integrado no release; comparar com Macrobenchmark de arranque e FrameTimingMetric.

**Aceitação:** o gerador falha se não atingir o destino; perfis contêm os caminhos realmente executados; medir melhoria na Xiaomi/Thomson e smartphone. Não prometer multiplicadores de desempenho sem dados.

Referências: https://developer.android.com/develop/ui/compose/performance/baseline-profiles e https://developer.android.com/develop/ui/compose/performance/bestpractices

## A6 — separar dimensão da janela e tipo de interação

**Melhoria de desenho, não bug reproduzido.** AdaptiveLayout define compacto por largura inferior a 840 dp; DeviceTextInput classifica comando por uiMode/features/touchscreen. Esta separação já existe parcialmente. Shell e HUD usam sobretudo o Boolean compacto, tornando pertinente testar smartphones/tablets largos, multiwindow e boxes com escalas de UI diferentes.

**Solução:** manter classes de tamanho para distribuição dos painéis e um estado independente para toque/comando. Um smartphone largo pode ter dois painéis com controlos táteis; uma box estreita pode ter layout compacto com navegação D-pad. Não usar ABI como identificação de TV nem forçar dois painéis num telemóvel.

**Aceitação:** portrait/landscape, multiwindow, fontes grandes, teclado aberto e D-pad externo. Os controlos táteis do HUD já têm vários mínimos de 48 dp e as barras já têm semântica de progresso; não propor essa implementação como se estivesse ausente.

## A7 — diagnóstico distingue entrega de frame e apresentação

**Limitação confirmada.** LivePreviewEngine incrementa frameCounter no VideoFrameMetadataListener e regista first_frame nesse callback. A API informa que o frame está prestes a ser renderizado, não confirma que apareceu fisicamente no painel. O watchdog usa o contador como sinal de progresso: útil, mas não prova ausência de bloqueio no compositor/display.

**Solução:** nomes de eventos precisos; distinguir primeiro frame do renderer, metadados de frame, progresso de áudio e superfície ligada. Correlacionar contadores/drops/underruns com fonte e geração da superfície. Não adicionar consultas do player nem trabalhos no main a cada frame.

**Aceitação:** relatórios distinguem atraso HTTP, falta de dados, falha de saída áudio e ausência de superfície. Nenhum novo watchdog sobreposto sem caso reproduzido.

Referência: https://developer.android.com/reference/androidx/media3/exoplayer/video/VideoFrameMetadataListener

## A8 — políticas e preferências sem aplicação

**Confirmado para dataSaver:** pesquisa nos dois repositórios encontrou a preferência, setter, accessor e inclusão no backup, mas nenhum consumidor de reprodução nem controlo atual na app. É configuração residual, não uma opção visível que se tenha demonstrado estar a falhar.

**Solução:** decidir entre manter apenas compatibilidade de backup ou expor uma política completa. Se for exposta, verificar antes de abrir e nas mudanças de rede; avisar e permitir decisão explícita. Não interromper por predefinição uma ligação que funciona. Não adicionar opções que só gravam um Boolean.

## O que preservar e medir

- SurfaceView para vídeo normal/fullscreen; TextureView apenas onde a composição o exige. A documentação Media3 favorece SurfaceView em consumo, timing e HDR. Não substituir todas as superfícies para tentar curar lentidão: https://developer.android.com/media/media3/ui/surface
- Buffers atuais já têm política temporal e orçamento em bytes. Segundos de buffer não equivalem diretamente a RAM; variar bitrate, resolução e número de players antes de concluir que um valor universal resolve.
- Cortes de áudio: medir underrun, criação/libertação de AudioTrack, codec/canais, buffer e tempos HTTP no mesmo instante. Comparar PCM estéreo com passthrough quando disponível; manter o volume interno a 100%. Não impor estéreo nem aumentar buffers sem relação demonstrada com a falha.
- Trabalho concorrente: medir playback sozinho, com EPG e com gravação. Se houver competição, limitar concorrência de tarefas secundárias e prioritizar leitura de reprodução. Não aumentar indiscriminadamente pools de threads ou pedidos simultâneos.
- Política térmica no smartphone é oportunidade por medir. Reduzir primeiro animações/trabalho secundário; qualquer limite de resolução/bitrate precisa de controlo explícito. RAM elevada não demonstra capacidade sustentada de decode.

## Ordem dos lotes

1. **Correções pequenas:** A2 e A4; testes de propriedade da superfície e foco, sem tocar nas políticas de buffer.
2. **Redução de trabalho e rede:** A1 e A3; validar arranque, utilização só de canais, reconexão e transições de rede.
3. **Perfis e adaptação:** A5 e A6; comparar release e verificar toque/D-pad em tamanhos diferentes.
4. **Reprodução sustentada:** A7 e ensaios de áudio/HLS/EPG/gravações; implementar apenas correções sustentadas pelos eventos coincidentes. A8 é limpeza/decisão de produto, de prioridade inferior.

Critérios físicos: pelo menos 100 mudanças A/B/C e retornos a canais; 30–60 min num canal saudável; rotação no smartphone; EPG e rewind; uma gravação em paralelo; Home/retoma; perda e mudança de rede. Registar falhas, tempo até imagem, percentil 95 do tempo de frames, PSS e eventos de áudio. Não existem medições novas nesta auditoria.
