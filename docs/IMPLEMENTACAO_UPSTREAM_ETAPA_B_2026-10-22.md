# OwnTV — etapa B: reprodução e fluidez (22/10/2026)

Estado: implementada e validada automaticamente; aceitação física na box pendente. Âmbito R03/R04/R05/R06 da análise das publicações OwnTV 5.2.1, Core 1.0.69 e libmpv 2026.10.4. Não renumera as fases 3–6 do plano principal.

## Alterações

### R03 — Superfície mpv e ordem das operações

- A ligação nativa passa para a mesma fila serial dos comandos mpv. Cada vida da superfície tem uma geração; uma ligação antiga ou de uma instância nativa substituída não é executada.
- A destruição da janela antiga aguarda também quando a nova já está selecionada mas a ligação continua na fila; não invalida nem desliga a substituta.
- O carregamento confirma que existe uma superfície válida da geração atual. Se o SurfaceView tiver sido recriado enquanto a libertação de rede decorria, a abertura retoma na nova superfície ou fica pendente até esta existir.
- Ao substituir uma ligação nativa existente, liberta-se primeiro a referência JNI anterior; não se sobrepõem chamadas de attach que substituam o campo nativo sem apagar a referência anterior.
- A destruição invalida ligações pendentes e aguarda o fecho nativo na fila antes de devolver a superfície ao Android. Há um registo `surface_detach_waitMs` quando o diagnóstico detalhado está ativo.
- **Limite deliberado:** este fecho não é integralmente assíncrono. `SurfaceHolder.Callback.surfaceDestroyed` exige que o renderer deixe de aceder à superfície antes do retorno. O patch upstream com espera máxima de 2,5 s não garante esse contrato depois de expirar. Não foi copiado um timeout que permita usar a superfície destruída. Um bloqueio dentro da libmpv pode, por isso, ainda atrasar a destruição; deve ser medido na box. Não há garantia de ausência de ANR nem de libertação de sessão no fornecedor.
- Não foram adicionados ciclos de pedidos de layout em todos os zappings nem alterada a SurfaceView para TextureView.

### R04 — Recuperação limitada e erro terminal real

- O watchdog existente de imagem congelada passa por reconexão, reconstrução de player/descodificador/superfície e, se a falha persistir, erro terminal. Não foi acrescentado outro watchdog.
- O orçamento pertence à sintonia e não se repõe em cada READY/reprepare; repõe-se após reprodução saudável ou seleção nova.
- A reconstrução verifica a identidade da fonte/sintonia e da instância, além do URL. Uma sequência A→B→A não aceita o trabalho antigo de A apenas por o endereço coincidir.
- A pausa cancela preparação pendente sem deixar o motor sem instância reutilizável; só a intenção atual de reproduzir pode iniciar o novo pedido.
- Todos os percursos terminais do LivePreviewEngine invalidam a fonte, cancelam resolução/preparação/pedidos, param o player e retiram os media items. O endereço e a intenção são conservados para retentativa explícita ou recuperação de rede já existente. Não continua áudio/vídeo sob o erro.
- A verificação de reprodução realmente confirmada continua a rejeitar falsas falhas. A falha persistente detetada pelo watchdog não é descartada apenas por o relógio/áudio continuarem.
- Os renderers Exo aguardam uma superfície real na primeira configuração do codec. Um codec já iniciado mantém a política Media3 de troca, evitando recriá-lo em todas as transições. As escolhas de queueing, hardware/software, fallback de descodificador, DRM e áudio permanecem.
- O contador de metadata mede submissão de frames, não apresentação física no painel. Não é apresentado como prova de renderização final.
- O modo ExoPlayer exclusivo mantém-se; esta etapa não acrescenta mudança incondicional para mpv.

### R05 — AFR com timestamps e libertação do modo anterior

- Medição de PTS com janela limitada, sincronizada entre threads, sem listas por frame. Exige pelo menos 4 s de timestamps válidos, filtra frames em falta e limpa a janela após descontinuidades. Distingue cadências fracionárias de inteiras.
- Exo em direto mede mesmo quando o header declara uma cadência arredondada. Diferidos/gravações em Exo usam a medição temporal quando AFR está ativo. Há fallback para a declaração/contador quando a amostra ainda não é suficiente.
- No mpv, a estimativa nativa da libmpv 2026.10.4 só substitui a cadência após duas amostras estáveis e próximas de uma cadência normalizada; não arredonda uma amostra ruidosa de 24,6 para 25.
- Sem correspondência válida, liberta-se o pedido de modo anterior para o modo do sistema, conservando cooldown, opção AFR e preferências do televisor. O modo atual é reconhecido como seamless, mas 59,94 e 60 não são presumidos alternativas seamless por mera proximidade.

### R06 — Classificação de áudio e Android antigo

- Os três players Exo usam os mesmos atributos de media/filme que PlaybackSession; não passam a disputar o foco de áudio.
- Em Android anterior a 13 sem HDMI identificável, o modo Automático do mpv assume estéreo conservador: dispositivos USB/Bluetooth ligados não provam qual a saída efetiva. Surround explícito mantém as capacidades USB anunciadas ou a política anterior quando não são conhecidas. HDMI conhecido e a consulta da saída efetiva em Android 13+ mantêm o percurso próprio.
- Continua a existir a proteção de áudio da sessão. Volume interno permanece 100%; nada nesta alteração prova a causa dos cortes AAC/MP2 periódicos.

## Proteção e verificação

Cópia de recuperação anterior à etapa: `C:/Users/renat/Downloads/OwnTV-upstream-B-backup-20261022/`, com App/Core, lock/snapshot anterior e manifesto SHA-256 verificado. O Core externo foi comparado com o snapshot anterior antes da intervenção: nenhuma divergência nos inputs.

Mantidos: Core personalizado TV, base de dados/backup, HLS exclusivo e exceções, configurações por canal, limites de buffer, reabertura de canal e correção +1 h de diferidos. Sem alterações ao projeto Mobile.

Resultados Core: 1 329 testes Core e 573 testes dos motores aprovados (zero falhas/erros/ignorados); testes Android do Core compilados, sem execução num dispositivo. A etapa acrescenta 16 testes dos motores e 3 da App.

Snapshot definitivo: 1 035 inputs, SHA-256 `350b830685de9e7e2b1453cc4a5f3a48f41a1bd8db03659e52a885e735463fc0`, comparado byte a byte com o Core externo, com nomes e conteúdo iguais. Inventários i18n: Core 263 baseline/5 748 safe, App 51 baseline/1 084 safe; dívida não aumentada. Sem alteração ao esquema de base de dados ou formato de backup.

Validação final da App contra o snapshot definitivo: compilação standard debug e release, 429 testes JVM, lint vital de release e preparação das bibliotecas nativas — `BUILD SUCCESSFUL`. Configuração normal de build confirmada por `gradlew help`, sem forçar o snapshot e sem desativar a sua integridade.

Total final, sem contar repetições: **2 331 testes JVM**, zero falhas/erros/ignorados, mais **6 testes da ferramenta de snapshot** aprovados. A etapa acrescenta **19 testes** aos existentes. A comparação do snapshot com os fontes foi repetida no fecho e os nomes/conteúdos coincidem. Verificação de espaços nas linhas alteradas e inventários i18n aprovada.

Logs finais em `%TEMP%`: `owntv-upstream-b-core-final.log`, `owntv-upstream-b-app-final.log` e `owntv-upstream-b-normal-config.log`. Os testes Android foram apenas compilados. Não houve reprodução real, medição de fluidez/áudio ou ensaio do fornecedor no PC.

Não foi gerado APK, alterada assinatura, feito commit/push ou publicada versão.

## Aceitação na box pendente

1. Exo e mpv: abrir canal, A→B→A, voltar à lista/fullscreen, Home/retorno, pausa/retoma e saída durante uma espera de reabertura. Não iniciar pedido durante a pausa nem aceitar recuperação de sintonia antiga.
2. Em mpv, medir `surface_detach_waitMs` e resposta ao comando ao recriar/sair do vídeo, incluindo rede lenta. Sem acesso à superfície depois de destruída.
3. Provocar imagem congelada real/controlada e conferir a escalada limitada; confirmar que um erro terminal liberta o fluxo e não deixa áudio por baixo da mensagem.
4. Exo exclusivo não deve mudar para mpv. Modo áudio sem vídeo e canais de rádio não devem ser classificados como imagem congelada.
5. AFR desligado/ligado, 25/50, 23,976/24 e 59,94/60 fps; respeito por Never/Seamless do Android, cooldown e regresso ao modo normal.
6. Áudio em Xiaomi, Mortal T1 e box antiga; estéreo, Automático, Surround explícito, HDMI e USB/Bluetooth. Ensaiar cortes anteriores em reprodução prolongada, sem os declarar resolvidos pelos testes JVM.
7. Conferir preservação de HLS, buffers, reabertura, preferências por canal e diferidos/gravações.

## Fontes e decisão técnica

- [Android — SurfaceHolder.Callback](https://developer.android.com/reference/android/view/SurfaceHolder.Callback): a destruição exige terminar acesso do renderer antes do retorno.
- [Media3 — MediaCodecVideoRenderer](https://developer.android.com/reference/androidx/media3/exoplayer/video/MediaCodecVideoRenderer): condições de superfície provisória e configuração do codec.
- [Core 1.0.69 — implementação upstream](https://github.com/ahXN00/OwnTV_Core/tree/core-1.0.69/player-core/src/main/java/tv/own/owntv/player): integração seletiva, sem substituir os ficheiros personalizados por inteiro.

Próxima etapa destas publicações: **C — R09/R10**, ainda não iniciada. C01/C02/C03 continuam condicionais/opcionais; as fases 3–6 do plano principal conservam o âmbito anterior.

## Atualização posterior — etapa C

A etapa C foi autorizada, implementada e validada automaticamente. Ver [IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md](IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md). A sequência recomendada A–C destas publicações ficou fechada no código; C01/C02/C03 mantêm-se opcionais/condicionais e a aceitação física na box permanece pendente.
