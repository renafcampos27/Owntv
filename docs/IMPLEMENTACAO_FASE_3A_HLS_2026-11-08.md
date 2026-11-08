# Fase 3A — decisões HLS limitadas à tentativa atual

Data: 08/11/2026. OwnTV Android TV personalizada.
Estado: fase 3A implementada e validada localmente no reprodutor e na App. Aceitação física pendente.

## Problema demonstrado antes da alteração

A implementação anterior guardava duas recusas de segmentos num mapa por `host:port`, válido durante dez minutos. O controlador consultava esse mapa ao abrir outro canal e podia arrancar diretamente em mpv, apesar de a preferência ser ExoPlayer primeiro. A regra abrangia também outras contas no mesmo servidor e atribuía a falha a tokens assinados sem prova suficiente.

Foi acrescentado um teste antes da alteração de produção: ensinar a recusa de um canal e pedir a rota de outro canal/conta no mesmo domínio. O teste falhou no código anterior exatamente por escolher mpv. O XML e o teste de reprodução dessa falha foram preservados na pasta de provas.

## Implementação exata

Raiz Core: `C:/Users/renat/Downloads/OwnTV_Core`.

| Ficheiro, relativo ao Core | Alteração |
| --- | --- |
| `player-core/src/main/java/tv/own/owntv/player/LiveStreamQuirks.kt` | Removidos exclusivamente mapa, validade e métodos da aprendizagem global de recusas de segmentos. Mantidas as restantes aprendizagens de formato, URL, UA, arquivo e esperas do fornecedor. |
| `player-core/src/main/java/tv/own/owntv/player/LiveRouting.kt` | Removido o argumento que propagava a conclusão por servidor. A rota inicial segue DRM, preferência explícita por canal e definição efetiva, pela ordem existente. |
| `player-core/src/main/java/tv/own/owntv/player/LiveTuneController.kt` | Retirada só a consulta dessa aprendizagem ao decidir o motor inicial e o helper sem consumidores. A escada e as proteções Exo/apenas HLS/exceções manuais continuam existentes. |
| `player-core/src/main/java/tv/own/owntv/player/HlsSegmentRefusals.kt` — novo | Contador por load, sem memória partilhada. Duas URLs distintas com o mesmo código podem sinalizar a escada existente; 403 + 404, por exemplo, não somam como a mesma observação. Mantida janela de 30 s, agora medida no relógio monotónico. Retry da mesma URL não conta como outro segmento; sucesso, fonte nova e reset saudável limpam a sequência. Chamadas no listener/handler principal existente, sem worker ou timer novo. |
| `player-core/src/main/java/tv/own/owntv/player/LivePreviewEngine.kt` | Integrado o contador, mantido o filtro `C.DATA_TYPE_MEDIA`, removida a escrita/reset do mapa global. Logs neutros com código observado. Evento persistente de recusa descreve HTTP/rede em vez de anunciar uma mudança para mpv que pode não ser permitida. Acrescentados task ID, papel do load, bytes e duração aos eventos existentes. |
| `player-core/src/main/java/tv/own/owntv/player/MediaLoadRole.kt` — novo | Papel baseado no data type Media3, não na extensão da URL: playlist/manifesto, segmento/média, inicialização e chave/DRM. A classificação de HLS é capturada ao construir a fonte, para um callback antigo não herdar a rota da fonte seguinte. |
| `player-core/src/main/java/tv/own/owntv/player/LiveExoWatchdog.kt` | Mensagem de handover baseada em erros HLS repetidos na tentativa atual; sem afirmar assinatura inválida ou sucesso garantido noutro motor. Nenhuma alteração a prazos, watchers ou gates. |
| `tools/i18n/safe_literals.txt` e `tools/i18n/hardcoded_baseline.txt` | Atualizado o inventário técnico: 16 novos registos de log/protocolo, oito registos obsoletos removidos e duas entradas antigas de diagnóstico retiradas do baseline. Sem aceitar novos textos de interface por traduzir, desativar a verificação ou alterar traduções. |

As falhas de playlist, inicialização e chave continuam fora do contador de segmentos. Nos fontes Media3 1.11.1 instalados, `HlsChunkSource.EncryptionKeyChunk` usa `C.DATA_TYPE_DRM`; o diagnóstico diz `key_or_drm`, porque esse tipo não prova isoladamente que o pedido é uma chave AES. Um load concluído indica bytes/duração; não se inventa um código HTTP de sucesso que o evento não fornece.

O callback de conclusão mantém a construção de strings sob `LiveDiagnosticsLog.enabled`. As falhas raras continuam no ring limitado existente. Mantidos os tokens de propriedade, rejeição de callbacks antigos e redação de URL. Não existe novo listener periódico, leitura do Player numa thread HTTP ou crescimento do painel técnico.

## Preservado

- Retry por URL, orçamentos, recuperação Media3 e escada já existente: sem nova rotina de refresh/reabertura, pedidos de sondagem ou saltos de segmentos.
- Apenas HLS, ExoPlayer apenas, escolha explícita de motor e exceções manuais; uma notificação do contador não ultrapassa essas políticas.
- Esperas de reabertura A–B–A e `Retry-After`, cancelamento/drain e isolamento da fila de reprodução da fase 2A.
- DNS da fase 2B, TLS, UA, headers, buffers, áudio/volume, codecs, EPG, gravações e preferências persistidas. Não há migração de dados ou novas definições.

## Testes e integração

**11 testes novos:** sete em `HlsSegmentRefusalsTest.kt`, três em `MediaLoadRoleTest.kt` e um em `LiveTuneControllerTest.kt`. Cobrem retries da mesma URL, códigos mistos, sucesso/reset, expiração, isolamento entre instâncias, classificação de chave/manifesto e recuperação A seguida de abertura ExoPlayer de B/outra conta no mesmo servidor. O teste do controlador usa motores simulados; não representa um ensaio real de Media3 contra um fornecedor.

Atualizados os testes de `LiveRoutingTest.kt` para todas as preferências. Removidos quatro testes da API global eliminada e duas asserções dessa API em testes de outras aprendizagens, preservando as suas verificações restantes.

- Core: `:player-core:testDebugUnitTest` terminou **BUILD SUCCESSFUL**, **631 testes**, zero falhas/erros/ignorados, incluindo regras de HLS, exceções manuais, cancelamento e esperas existentes.
- App: compilação `:app:compileStandardReleaseKotlin`, **470 testes JVM** e `:app:lintVitalStandardRelease` aprovados. Relatório: **No issues found.** Comando final: **BUILD SUCCESSFUL in 1m 10s**. Total desta fase: **1101 testes**, zero falhas/erros/ignorados na execução final. Não foram contados novamente os testes de rede/ferramentas inalterados.
- A primeira execução da App falhou no teste preexistente `SearchReactivePipelineTest.valid query triggers search with active context`: a chamada já tinha sido registada, mas o collector ainda não tinha recebido o resultado antes de `emissions.last()`. O mesmo teste passou isoladamente sem alteração, confirmando a intermitência. Foi feita uma correção mínima **apenas no teste** `app/src/test/java/tv/own/owntv/features/search/SearchReactivePipelineTest.kt`: aguardar também a receção dos canais antes das mesmas asserções, sem aumentar timeout ou alterar o pipeline de pesquisa da App. A suite completa foi repetida e aprovada; falha inicial e execução isolada preservadas, sem as contar como aprovação global.
- O JAR Release realmente compilado foi inspecionado: `LiveRouting.decide` tem os três argumentos finais; o contador local e o classificador de loads estão presentes. Não se usou um resultado antigo como prova da alteração.
- Snapshot: **1098 inputs**, SHA-256 `4d0cd73fee3e62243dc4202a7c32e17aa3514e83f4b0f27328fcd7666b1646ed`. Checkout, ZIP bloqueado e inputs extraídos da compilação comparados byte a byte. A App reconheceu explicitamente esse SHA e invalidou a configuração anterior ao mudar o lock.

O lint integral do Core não foi repetido: já tem cinco erros anteriores de pluralização documentados nas fases 2A/2B. Os respetivos recursos não foram alterados nesta fase. Não se deve confundir o lint vital Release da App com a aprovação desse lint integral.

Cópias/provas: `C:/Users/renat/Downloads/OwnTV-fase3a-2026-11-08-131813`. Incluem os hashes dos 1094 inputs anteriores, cópias dos ficheiros alterados, teste/XML que demonstrou a falha anterior e resultados finais. A comparação com essa baseline mostra dez inputs existentes alterados, quatro acrescentados e nenhum removido. Não foi usado o commit Git antigo como se fosse o estado inicial personalizado.

## Aceitação física pendente e limites

Após gerar/instalar o APK atualizado, testar um canal que falha e outro saudável no mesmo fornecedor, tanto com ExoPlayer/HLS apenas como com ambos os motores permitidos. O segundo canal deve respeitar a preferência inicial; um canal realmente problemático conserva os percursos permitidos de recuperação da própria tentativa. Verificar ainda A–B–A e ausência de mudanças indevidas para TS.

Os testes locais demonstram o isolamento e as regras de recuperação, não garantem aceitação do fornecedor, eliminação dos bloqueios A–B–A, continuidade física de áudio ou ganhos percentuais de velocidade. Não houve APK, instalação, commit ou publicação. Fase 3B, 1B condicionada e fases posteriores permanecem pendentes.

## Referências verificadas

- [RFC 9110 — 403 Forbidden](https://www.rfc-editor.org/rfc/rfc9110.html#name-403-forbidden), [404 Not Found](https://www.rfc-editor.org/rfc/rfc9110.html#name-404-not-found) e [410 Gone](https://www.rfc-editor.org/rfc/rfc9110.html#name-410-gone): os códigos têm significados distintos e não demonstram uma incompatibilidade global de tokens.
- [Android Media3 — Troubleshooting](https://developer.android.com/media/media3/exoplayer/troubleshooting): referência de investigação de falhas; a alteração desta fase não substitui o pipeline Media3.
- Fontes locais da dependência efetivamente usada: `media3-exoplayer-hls-1.11.1-sources.jar`, `HlsChunkSource.EncryptionKeyChunk`, para classificação da chave e preservação do filtro Media3.
