# Fase 3 — estabilidade da sintonização e zapping

Implementada em 28/09/2026, nos projetos locais `OwnTV-main` e `OwnTV_Core`.

## Âmbito

Esta entrega corresponde à Fase 3 da lista de cinco fases: LiveTuneController, snapshot PlaybackSettings e orçamento de reconexão mpv. Não inclui timeshift local, migrações de base de dados, canal anterior ou novos binários.

## Alterações

- A decisão de motor, os passos de fallback, os temporizadores de abertura, a resolução Stalker e a passagem entre ExoPlayer/mpv ficam no `LiveTuneController` do Core. O LiveViewModel mantém a navegação, as regras do perfil e as alternativas entre versões do mesmo canal.
- Uma nova escolha cancela a sintonia anterior, os observadores e resoluções pendentes. Os callbacks dos observadores verificam também a geração da sintonia, incluindo o caso de sair e voltar ao mesmo canal.
- Preview, multiview e fullscreen reutilizam a preparação central. A promoção do preview exige uma origem ainda válida e sem erro. As passagens entre motores não cancelam a sua própria tarefa durante a libertação do decoder.
- O primeiro live espera pelo snapshot das definições guardadas. Os motores recebem explicitamente os valores antes da abertura, sem depender da ordem em que os seus observadores arrancam. Os pins de motor também são lidos antes da decisão.
- O mpv só repõe o orçamento de reconexões após pelo menos 60 segundos de progresso saudável observado pelo watchdog. Falta de progresso, buffering, pausa ou ausência de imagem esperada interrompem essa janela. Uma nova carga começa uma nova janela.
- Mantidos o tempo configurável para procurar alternativas, a procura de versões do mesmo canal, os filtros de perfil e a navegação pelas setas.
- Catch-up existente cancela sintonizações live pendentes; os resultados de pesquisa antigos não podem substituir uma nova escolha.

## Correção necessária para compilar o trabalho existente

A opção de pausa do Auto Frame Rate já tinha a linha nas definições e o armazenamento, mas faltavam `afrPauseLabel`, o caso `AFR_PAUSE` e o respetivo seletor. Essas ligações foram completadas usando os recursos existentes. Não foi alterado o algoritmo de mudança de frequência nesta entrega.

O inventário de traduções foi ajustado para os diagnósticos movidos para o Core e para os identificadores técnicos existentes do AFR.

## Validação executada

- `:app:compileStandardDebugKotlin`: passou.
- `:app:testStandardDebugUnitTest`: 179 testes, zero falhas/erros.
- `:OwnTV_Core:player-core:testDebugUnitTest`: 226 testes, zero falhas/erros.
- Entre os testes do Core: 15 do controlador, 5 de routing e 3 da janela de estabilidade.
- Entre os testes da app: 13 das alternativas e 4 da identidade da seleção.
- Verificações de literais e de whitespace: passaram.
- Nenhum APK foi gerado nesta entrega.

## Validação pendente nas boxes

1. Arranque a frio com motor/latência/descodificação previamente configurados: confirmar a primeira abertura.
2. Pressionar rapidamente cima/baixo, incluindo A → B → A; confirmar que só a última escolha prevalece e que voltar à lista silencia a reprodução quando o preview está desligado.
3. Canal indisponível com alternativas: confirmar o tempo escolhido e que uma ação manual interrompe a procura automática.
4. Se utilizados, verificar preview → fullscreen, troca manual de motor e entrada/saída do catch-up.
5. Reproduzir um canal estável e um canal problemático durante pelo menos 30 minutos, com registos. Os testes locais não comprovam ausência de pausas, perdas de fotogramas ou falhas específicas da Xiaomi/Thomson.

Esta fase corrige coordenação e reconexões; não demonstra que todos os engasgos de rede, origem ou descodificação estejam resolvidos.
