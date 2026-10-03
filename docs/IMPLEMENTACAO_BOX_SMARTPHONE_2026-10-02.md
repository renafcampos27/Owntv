# OwnTV — implementação da auditoria box/smartphone

Data: 2026-10-02. Alterações em `OwnTV-main` e no composite build local `OwnTV_Core`. Alterações anteriores preservadas. Sem APK, alteração de versão, commit ou publicação.

## Resultado por item

| Item | Implementação | Estado |
|---|---|---|
| A1 | VMs de filmes, séries e pesquisa resolvidos apenas ao primeiro uso, com o mesmo owner/key Koin; observações de catálogo no shell apenas para o tipo efetivamente reproduzido. Sair de um canal não cria VMs para gravar progresso de conteúdo inexistente. | Implementado; ganho físico por medir. |
| A2 | `surfaceDestroyed` mpv desliga a superfície concreta. Callback de dimensão também verifica propriedade antes de atualizar o player. | Implementado; rotação/recriação em hardware por testar. |
| A3 | Rede predefinida observada por identidade e capacidades recebidas nos callbacks; perdas/capacidades antigas não substituem a atual. Poll OEM fora dos callbacks e protegido por revisão. Um callback/poll partilhado entre consumidores. Live regista transições e só utiliza a recuperação já existente quando elegível. | Implementado e reducer testado. |
| A4 | Pedidos de foco verificados por resultado, com tentativas por frame limitadas. Toque ou tecla invalida pedidos pendentes. Foco inicial de texto/diferidos não toma o foco se já houve interação; shell guarda a revisão no instante do pedido. Correção de `.isSuccess` para o Boolean real nas restaurações comuns. | Implementado e política testada. |
| A5 | Identificadores de teste estáveis, percursos TV/toque com destinos verificados, reprodução/zapping, EPG e gravações. Macrobenchmark de arranque/frames. Fixture local de M3U, XMLTV e HLS sintéticos sem credenciais. | Ferramentas implementadas e compiladas; fixture validada por HTTP. Gravação de perfil e medições em Android pendentes. |
| A6 | Dimensão da janela separada da interação. Smartphones largos mantêm controlos táteis compactos; distribuição de painéis continua dependente da janela. Boxes compactas continuam com comando. | Implementado e combinações de política testadas. |
| A7 | `first_frame_metadata` separado de `first_frame` do renderer. Contador identificado como frames enviados para renderização; amostras incluem geração/estado da superfície. | Implementado; não certifica apresentação física. |
| A8 | Preferência antiga `dataSaver` identificada como compatibilidade de backup, sem introduzir uma opção inoperante ou um veto escondido à reprodução. | Decisão aplicada/documentada; chave preservada. |

## Validação executada

- App: compilação `standardDebug` e **281 testes** sem falhas, erros ou testes ignorados.
- Core: **971 testes** sem falhas, erros ou testes ignorados, incluindo cinco novos casos de rede.
- player-core: **353 testes** sem falhas, erros ou testes ignorados.
- Total: **1605 testes**. Testes de foco incluem falso→retry→sucesso, alvo ausente, input durante layout e input anterior à chegada do conteúdo.
- Gerador/benchmark compilados nas variantes `benchmarkRelease` e `nonMinifiedRelease`.
- Lint player-core: **0 erros, 5 avisos**. Não foi repetido o Lint completo da app; os quatro erros de baseline identificados na ronda anterior não foram corrigidos neste lote.
- Inventários de internacionalização aprovados; identificadores de teste/log declarados técnicos. Baselines existentes mantidos.
- Fixture criada com três canais para validação: M3U, XMLTV com 147 programas, playlist HLS e segmento servidos por HTTP. Servidor de validação terminado no fim. O gerador predefinido cria 240 canais.
- `git diff --check` sem erros de whitespace.

Logs de validação em `%TEMP%`: `owntv-box-phone-final-validation.log` e `owntv-box-phone-app-final.log`. Backup dos principais ficheiros alterados e inventários em `C:\Users\renat\AppData\Local\Temp\owntv-box-phone-before-20261002-153322`.

## Limites e continuidade

Nenhum Android estava ligado ao ADB. Não foram gravados novos Baseline Profiles nem executados Macrobenchmarks, testes de rotação, perda/troca de rede ou reprodução prolongada em box/smartphone. Compilar o gerador não atualiza automaticamente o perfil incluído no APK.

Não alterar buffers, qualidade, passthrough ou prioridade/quantidade de threads apenas com base nos cortes breves de áudio. Medir eventos de áudio, tempos HLS, buffer e UI no mesmo instante; a instrumentação anterior de saída áudio/HTTP foi preservada. Volume interno permanece a 100%, com controlo pela box/sistema. Mantidos HLS-only, setas de zapping, EPG, diferidos, gravações e rewind.

Instruções para medir e gerar o perfil: [DESEMPENHO_BOX_SMARTPHONE_VALIDACAO.md](DESEMPENHO_BOX_SMARTPHONE_VALIDACAO.md). A fonte sintética não valida limites de sessões do fornecedor, live sustentado, HDR, passthrough nem catch-up Xtream. Para medição de lista sem categorias, ocultar categorias e manter os destinos de navegação visíveis; testar manualmente também a vista com barra lateral totalmente oculta.
