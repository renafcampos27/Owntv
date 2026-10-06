# OwnTV 5.1.0 — lotes C, D e retoma local do E

Data: 06/10/2026. Estado: implementado e validado por compilação/testes locais. Falta validação física em box e smartphone. Não foi gerado APK nem publicada uma versão.

## C — servidores com HLS e TS misturados

- Uma resposta HLS confirmada continua a ensinar o formato do stream. A aprendizagem tem limite de 512 entradas por conjunto e expira ao fim de 30 minutos; não é persistida.
- Se uma tentativa baseada apenas na aprendizagem do servidor receber TS confirmado e falhar no parser, deixa de impor HLS aos outros canais desse servidor. Os canais que comprovadamente redirecionam para HLS mantêm a sua aprendizagem individual.
- A prova de TS exige três fronteiras de pacotes de 188 bytes numa resposta HTTP bem-sucedida ao pedido original. Não se inspecionam os segmentos normais de canais saudáveis para esta recuperação.
- A repetição usa o endereço original, uma vez, com verificação da identidade da sintonia e do player. O fluxo de preparação existente continua a aguardar a libertação dos pedidos anteriores.
- Modo apenas HLS, TS manual e formato declarado têm prioridade. Esta recuperação não permite TS onde esteja proibido. Erros HTTP, HTML e manifestos inválidos não são tratados como prova de TS.

Não foram alteradas as esperas locais de 300 ms/retorno recente, buffers, áudio ou regras de bloqueio do fornecedor. Este lote não demonstra que a sessão já terminou no servidor nem resolve por si só todas as falhas de zapping.

## D — diferido carregado sem imagem (#229)

Depois da tentativa de recuperação por software já existente, um diferido sem imagem deixa de ser classificado nesse ramo como ficheiro sem fast-start. Passa pelo diagnóstico de abertura/descodificação.

A tentativa com ExoPlayer reutiliza o handover existente: uma tentativa, identidade/geração, libertação do motor anterior e respeito pela preferência de motor exclusivo. Não se acrescentou uma segunda rotina de reconexão nem se assumiu que todos os arquivos são MPEG-TS. O ramo de conteúdos não classificados como diferidos mantém o diagnóstico anterior.

Corrigido também o arranque de diferidos com ExoPlayer exclusivo: esse motor começa diretamente a reprodução. Antes, o arranque de arquivo estava sempre encaminhado para mpv, mesmo com ExoPlayer exclusivo. Os modos automáticos mantêm a rota inicial existente.

Não corrige horários EPG, arquivo indisponível ou respostas do fornecedor que abram o direto.

## E — retomar timeshift local

Nova opção em **Definições → Reprodutor de vídeo → Direto → Retomar timeshift local**, também acessível pela pesquisa. Usa três escolhas: **Perguntar** (predefinida), **retomar automaticamente** e **Nunca**. A preferência é guardada em DataStore e incluída no backup de configurações.

Funcionamento:

1. O utilizador inicia explicitamente o timeshift local pelo comando existente do canal.
2. Ao abandonar essa sessão, a captura é cancelada e os pedidos terminam antes de libertar a reserva da ligação. Conservam-se a cópia local e a posição válida.
3. Ao voltar ao mesmo canal e perfil, a aplicação oferece ou executa a retoma conforme a opção selecionada. Não abre primeiro o direto nem faz pedidos ao fornecedor para ler a cópia.
4. A cópia terminada é reproduzida como conteúdo local finito, no leitor ExoPlayer usado pelo timeshift local. Mantém as setas cima/baixo para mudar de canal e acrescenta acesso ao direto. Ao atingir o fim da cópia, não se inventa conteúdo que não foi gravado; o utilizador pode escolher o direto.
5. Escolher direto ignora a oferta para essa abertura e segue a sintonia normal. Voltar atrás no diálogo também escolhe o direto.

Limites e proteção:

- Uma cópia local por vez, com os limites já existentes de 256 MiB e 30 minutos de conteúdo e reserva de espaço livre.
- Conservação até 30 minutos após abandonar a cópia, durante a sessão da aplicação. Não retoma após a morte do processo, forçar paragem ou reinício da box; só a preferência é persistente e incluída no backup.
- Começar uma nova captura substitui a cópia anterior. Trocar perfil/listas ou destruir o ViewModel encerra e liberta a cópia.
- A posição deriva da sequência do manifesto efetivamente lido pelo player, não apenas da janela mais recente do produtor. Se a sequência/posição já tiver sido removida ou restarem menos de três segundos, não se oferece retoma.
- O conteúdo copiado contém apenas o que foi capturado. Não continua a gravar em segundo plano enquanto se vê outro canal.
- A retoma não liga automaticamente o timeshift nos canais normais nem altera o canal de arranque escolhido.

## Validação

Comando executado a partir de OwnTV-main, usando o Core personalizado local:

```text
gradlew.bat :app:compileStandardDebugKotlin :app:testStandardDebugUnitTest :OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest :OwnTV_Core:core:compileDebugAndroidTestKotlin --console=plain
```

Resultado final: **BUILD SUCCESSFUL**, **1811 testes unitários**, sem falhas nem erros:

- App: 311.
- Core: 1060.
- Player Core: 440.

Quinze testes novos cobrem formato misto, expiração, rejeição de HTML, bloqueio de TS em modo HLS exclusivo, diagnóstico de diferido, escolhas de motor e arranque de arquivo em ExoPlayer exclusivo, perfil/fonte/canal da retoma, posições removidas, fim da cópia e conservação/leitura local após terminar a captura. O teste HTTP confirmou que a leitura da cópia não gera pedidos ao servidor de origem e que o encerramento apaga os ficheiros locais.

Os testes de instrumentação foram compilados, não executados em aparelho. Inventários de tradução e verificações de whitespace passaram. Os testes não provam que um fornecedor real liberta imediatamente uma sessão, nem validam descodificação em todas as boxes.

## Testar na box/smartphone

1. HLS exclusivo ativo: alternar A→B→C→A e confirmar que não surgem tentativas TS. Com exceção manual/formato misto autorizado, verificar a recuperação do endereço originalmente anunciado como TS.
2. Abrir um diferido com falha de imagem em mpv exclusivo e depois em modo automático; confirmar diagnóstico adequado e ausência de ciclos ou troca de motor em modo exclusivo.
3. Iniciar timeshift local, capturar algum conteúdo, recuar, sair e voltar ao mesmo canal. Testar Perguntar, automático e Nunca; confirmar as setas para mudar de canal e a opção de direto.
4. Testar troca de perfil/lista, início de nova captura e cópia expirada. Não deve ser oferecida uma posição de outro contexto.
5. Exportar/restaurar configurações e confirmar a opção de retoma. Os segmentos transitórios não fazem parte do backup.

## Referências

- [Análise original da OwnTV 5.1.0](ANALISE_OWNTV_5.1.0_2026-10-06.md).
- [Lotes A/B já aplicados](IMPLEMENTACAO_OWNTV_5.1.0_CATEGORIAS_PESQUISA_2026-10-06.md).
- [Pendências e histórico do Core](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md).
- [Documentação oficial Media3 HLS](https://developer.android.com/media/media3/exoplayer/hls).
- [API oficial Player e posição de reprodução](https://developer.android.com/reference/androidx/media3/common/Player).
- Código upstream transferido OwnTV 5.1.0 e OwnTV_Core 1.0.64, comparado seletivamente com o código personalizado.
