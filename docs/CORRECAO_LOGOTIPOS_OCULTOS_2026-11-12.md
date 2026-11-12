# Recuperação dos logótipos em falta — 12/11/2026

Pedido: alguns logótipos deixaram de aparecer após a introdução do armazenamento persistente. Correção limitada à App; não altera os motores, áudio, HLS, diferidos, base de dados ou preferências.

## Problemas verificados no código

1. A interface passou a aceitar apenas a nova miniatura persistente. Se o download/conversão/escrita falhasse, não reutilizava uma cópia válida que pudesse existir na cache anterior do Coil.
2. O download rejeitava originais acima de 2 MiB, apesar de o carregador anterior poder apresentá-los. Além disso, a conversão rejeitava o resultado se as suas dimensões não estivessem já dentro de 192 × 192, em vez de garantir explicitamente a redução final.
3. Os bytes transferidos não conservavam o Content-Type para a descodificação. O detetor normal de SVG do Coil apenas inspeciona o início do ficheiro; um preâmbulo XML/comentário longo pode impedir a identificação de um SVG válido. Isto não significa que todos os SVG fossem rejeitados: normalmente já eram rasterizados corretamente.
4. O worker não voltava a tentar as falhas já registadas na mesma geração da lista, mesmo após corrigir o código de conversão. Um update da App podia, portanto, manter logótipos em falta.

Estas fragilidades estão confirmadas no código. Sem o APK instalado, os URLs dos logótipos afetados e registos da box, não se confirma qual delas explica cada imagem em falta no dispositivo do utilizador.

## Alterações implementadas

| Ficheiro, relativo à raiz da App | Alteração |
|---|---|
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoImages.kt` | Quando falta/falha a miniatura nova, permite ler a cache anterior do Coil, com rede desativada e disco só para leitura. Isola o estado por URL. Passa o MIME ao descodificador SVG, reduz explicitamente o bitmap final e usa PNG se a compressão WebP devolver falha. |
| `app/src/main/java/tv/own/owntv/ui/images/LogoThumbnailSize.kt` | Calcula dimensões até 192 × 192, sem aumentar imagens pequenas, conservando a proporção dentro do arredondamento e impedindo dimensões zero. |
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoRepository.kt` | Aceita originais até 8 MiB. Transfere por blocos de 32 KiB para um ficheiro temporário, sem criar uma cópia integral em RAM. Conserva o MIME e reconhece `.svg` quando o tipo está ausente/genérico. Remove o temporário no sucesso, erro ou cancelamento. |
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoDiskCache.kt` | Aceita miniaturas WebP e PNG com a mesma quota. Metadados passam a incluir formato e versão da política. Entradas antigas sem imagem recebem uma nova tentativa; imagens antigas válidas continuam utilizáveis. |
| Testes em `app/src/test/java/tv/own/owntv/ui/images/` | Migração de falhas, preservação de imagens boas, quota PNG/WebP, limites conhecidos/desconhecidos, ficheiros temporários, cancelamento, MIME e dimensões. |
| `app/src/androidTest/java/tv/own/owntv/ui/images/ChannelLogoEncodingTest.kt` | Acrescenta SVG com preâmbulo longo e verificação do pedido offline à cache anterior. |
| `tools/i18n/safe_literals.txt` | Apenas classificação dos novos literais técnicos; sem nova dívida de tradução. |

## Limites e compatibilidade preservados

- Miniatura até **192 × 192**, **192 KiB**, quota de imagens persistentes **64 MiB** e **duas operações de transferência/conversão** em simultâneo.
- Os 8 MiB são o teto do original transferido, não o tamanho guardado de cada logótipo. Existem, no máximo, dois temporários ativos destes downloads; são removidos após a operação. O limite temporal HTTP mantém-se.
- O novo percurso continua a ser o proprietário das transferências. O recurso à cache anterior não faz um segundo pedido de rede nem descarrega originais sem limite.
- Preservados ETag/Last-Modified, HTTP 304, backoff, Retry-After persistido, cancelamento por subscrição e última imagem válida. A migração não contorna um Retry-After explícito do fornecedor.
- Não se apaga a cache existente, não se exige limpar dados e não se alteram as URLs ou a escolha de logótipos do EPG.
- Nenhuma dependência nova. Core e seu snapshot permanecem iguais ao estado à entrada desta correção: SHA-256 `b96bf0f13976cfe350e70c2750a73eca472abf2c98c70c1230d3bb590882e5bb`, 1117 inputs. As correções anteriores de recuo/retoma foram preservadas.

## Validação

- **513 testes JVM da App aprovados**, zero falhas, erros ou skips; incluem **30 testes** da cache/repositório/dimensões e cinco da política HTTP anterior. Há 12 testes JVM novos nesta correção.
- Compilação Standard Release e lint vital aprovados, com verificação estrita das dependências e snapshot Core bloqueado.
- Cinco testes Android de imagem/pedido offline compilados; **não executados num dispositivo**. A compilação não prova a apresentação real de PNG/SVG nem o conteúdo da cache da box.
- Inventário de tradução aprovado: baseline mantida em 47 entradas/56 ocorrências; apenas literais técnicos atualizados.
- Os hashes de 16 inputs foram registados antes da execução final e reconfirmados depois. `core-source-lock.json` coincide com o backup inicial desta correção.
- Backup, logs e resultados: `C:/Users/renat/Downloads/OwnTV-correcao-logos-2026-11-12-223924/`, incluindo `app-validation-final.log`, `validated-input-hashes.json` e `validation-summary.json`.

Não foi gerado/instalado APK, efetuado commit/push ou medido desempenho na box. A correção permite recuperar falhas do novo percurso; não pode criar um logótipo cujo URL esteja ausente ou cujo servidor continue a responder com erro.

## Aceitação na box

1. Gerar e instalar o novo APK sobre a instalação atual, sem limpar os dados.
2. Abrir com Internet e deixar a preparação automática decorrer. Confirmar canais anteriormente em falta na lista, guia e leitor.
3. Fechar/reabrir e confirmar novamente; depois verificar os logótipos já guardados sem Internet.
4. Atualizar a lista: imagens modificadas devem renovar e uma falha remota deve conservar a última imagem válida.

Fundamentação do carregador: [caches e carregador singleton do Coil](https://coil-kt.github.io/coil/image_loaders/) e [identificação/descodificador SVG](https://coil-kt.github.io/coil/svgs/), confrontados com os fontes da versão 3.6.0 usada pelo projeto.
