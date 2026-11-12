# Recuo num programa e retoma — consolidação e proteção de comandos

Data: 12/11/2026. OwnTV Android TV/Core personalizado. Pedido autorizado: implementar as proteções propostas após o relato de falha ao abrir um programa terminado, recuar bastante e carregar em Play.

## Sintoma e conclusão limitada

O utilizador relata o reaparecimento do arranque Google TV. Não existe log do instante da falha nem identificação confirmada da box, motor ou codec efetivamente ativos. A animação de arranque, isoladamente, não distingue reinício do framework de reinício completo do dispositivo.

Foram corrigidas duas fragilidades demonstráveis no código: um seek por cada repetição de seta na barra dos programas e comandos mpv de seek/play/pause sem a mesma proteção de geração já utilizada noutros percursos. **Não se considera comprovada nem resolvida a causa do reinício do Android.** A aceitação física permanece pendente.

## Comportamento implementado

1. Na barra temporal dos programas, as repetições de esquerda/direita alteram uma posição de seleção local. Não enviam um seek ao descodificador por cada KeyDown.
2. Largar a seta envia um único delta para o ponto escolhido. Passar para outro controlo também entrega a seleção pendente antes de a ação seguinte prosseguir. O estado pendente é limpo após entrega, evitando nova entrega pela perda de foco.
3. A posição escolhida aparece na barra e no balão temporal. É limitada a zero/duração conhecida, com aritmética resistente a overflow. Uma duração desconhecida não autoriza um salto.
4. A seleção pertence à geração do carregamento. Substituir/reabrir o programa, mesmo com URL/título iguais, invalida a seleção anterior. Uma duração entretanto mais curta volta a limitar o destino.
5. Não há timer/debounce nem espera por READY introduzida nesta alteração. A seleção não altera o pedido de pausa/play. O comando final e o Play seguem a fila nativa existente; não se cria outro executor.
6. Seek e play/pause mpv são revalidados através de `mpvForLoad`, que confere geração e instância nativa na fila de destino. Play/pause preserva ainda a proteção por revisão da intenção. Um comando não passa a atuar no mpv depois de uma mudança para ExoPlayer.
7. Os deltas mpv preservam milissegundos e sinal, sem truncar para segundos inteiros ou depender da vírgula decimal do idioma.
8. Com diagnóstico detalhado ativo, `seek_input` e `playback_intent` registam geração, ramo do motor, posição/duração/delta e intenção de pausa. Sem URLs ou credenciais. Reutiliza o escritor assíncrono limitado existente; um reinício abrupto pode impedir a persistência das últimas linhas.

## Âmbito preservado

A consolidação é ativada na `VodTimeline` usada pelos programas em diferido e outros conteúdos nesse leitor. A `NativeLiveTimeline` conserva o seu percurso anterior. Não altera a mudança de canais com cima/baixo, criação de URLs de arquivo, formato HLS/TS, buffers, latência, cooldowns, configurações de hardware/software, volume ou gravações. Não introduz mudança automática global de motor nem atribui a falha ao fornecedor.

## Ficheiros de implementação

Raiz App: `C:/Users/renat/Downloads/OwnTV-main/`.

| Ficheiro | Alteração |
|---|---|
| `app/src/main/java/tv/own/owntv/player/TimelineScrub.kt` — novo | Modelo puro da seleção, limites, acumulação e geração. |
| `app/src/main/java/tv/own/owntv/player/PlayerHudControls.kt` | Pré-visualização nos KeyDown; entrega no KeyUp/navegação/perda de foco; acessibilidade mantém seleção direta. |
| `app/src/main/java/tv/own/owntv/player/PlayerHudChrome.kt` | Ativa a consolidação apenas na barra do leitor de programas e fornece a geração atual. |
| `app/src/test/java/tv/own/owntv/player/TimelineScrubTest.kt` — novo | Nove casos do modelo: repetições, direção, relógio a avançar, substituição, overflow, duração desconhecida/encurtada, salto nulo e passo desativado. |
| `core-source-lock.json` e ZIP em `vendor/` | Integram os inputs Core efetivamente validados. |

Raiz Core: `C:/Users/renat/Downloads/OwnTV_Core/`.

| Ficheiro | Alteração |
|---|---|
| `player-core/src/main/java/tv/own/owntv/player/PlaybackEngine.kt` | Nova propriedade de leitura `seekGeneration`, com default compatível e delegação no adaptador mpv. Não faz leituras JNI. |
| `player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt` | Expõe a geração existente; reusa `mpvForLoad` nos seeks e pedidos play/pause; diagnósticos opcionais. |
| `player-core/src/main/java/tv/own/owntv/player/MpvLoadCommand.kt` | Formatação decimal exata do seek relativo. |
| `player-core/src/test/java/tv/own/owntv/player/MpvRelativeSeekTest.kt` — novo | Três testes de sinal, frações e limites de Long. |
| `tools/i18n/safe_literals.txt` | Apenas os novos literais técnicos/de diagnóstico. Baseline de tradução não aumentada. |

O helper `PlaybackContinuation`/`mpvForLoad` não foi reinventado. A suite existente já cobre substituição entre submissão/execução, reabertura do mesmo URL e troca de instância nativa. Mantidos AGP 9.5.1, Media3 1.11.1, libmpv 2026.11.0, schema 51 e backup 26.

## Validação e provas

- Módulo player: **636 testes JVM aprovados**, incluindo os três novos testes de seek relativo e os testes existentes de ownership/continuações.
- App: **501 testes JVM aprovados**, incluindo os nove novos testes de seleção temporal. Compilação Standard Release e `lintVitalStandardRelease` aprovados com verificação estrita das dependências.
- Total: **1137 testes JVM aprovados**, dos quais **12 novos**, zero falhas/erros/skips. A última invocação da App terminou com `BUILD SUCCESSFUL`; fontes confirmados estáveis durante a validação.
- Não foi executado o gesto real no comando nem o descodificador da box. Os testes JVM do modelo não comprovam ausência de reinício do sistema.
- Snapshot Core: `b96bf0f13976cfe350e70c2750a73eca472abf2c98c70c1230d3bb590882e5bb`, **1117 inputs**, comparados entre checkout externo, ZIP e extração usada na build.
- Backup e provas: `C:/Users/renat/Downloads/OwnTV-recuo-retoma-2026-11-12-221356/`. Snapshot anterior preservado; sem limpeza do checkout nem de dados persistentes.

## Aceitação física

Gerar/instalar o APK atualizado pelos meios habituais. No mesmo programa, confirmar: pausa; seta mantida para recuar; libertação da seta; um reposicionamento; Play; retoma no ponto escolhido. Verificar também avanços, início/fim, mudança de programa durante uma seleção e regresso à lista.

Se o Google TV voltar a reiniciar, recolher o diagnóstico detalhado e o motivo/registos de reinício do Android. Esses dados são necessários para investigar codec/firmware/serviços do sistema sem atribuir a causa por suposição. Referências: [procura e reinício da descodificação no ExoPlayer](https://developer.android.com/media/media3/exoplayer/troubleshooting) e [motivos de reinício Android](https://source.android.com/docs/core/architecture/bootloader/boot-reason).

Sem APK gerado, instalação, commit ou push nesta execução. A origem do reinício mantém-se em investigação até ao teste físico.
