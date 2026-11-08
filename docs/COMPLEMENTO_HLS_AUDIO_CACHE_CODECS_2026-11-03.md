# Complemento: HLS, áudio, cache e codecs

Data: 3 de novembro de 2026. Estado: análise e ensaios locais; alterações funcionais por implementar.

Este complemento acrescenta achados à análise `ANALISE_HLS_BLOQUEIOS_E_OTIMIZACOES_2026-11-03.md`. As fases A-C desse documento continuam pendentes. Não substitui as correções da rede já recomendadas.

## Alcance e evidência

Foram aprofundados a cache de timeshift local, o filtro de variantes, os codecs de áudio, a identificação da saída, as políticas de memória, o seletor de faixas e o parser de preferência SDR. Dez ficheiros do Core coincidem byte a byte com o snapshot `70aab7a373d8d6dc8449eda6a3c3e720118387e196a81fa0222f15710456ceb9` utilizado pela App.

Três cenários adicionais foram compilados e executados na JVM com componentes atuais do Core, ficheiros sintéticos e criptografia Java. Não foram usados canais, credenciais ou dados pessoais. Os testes não reproduzem o descodificador, AudioTrack nem a reprodução Android. Não foi gerado APK.

## C08 — Duração-alvo da playlist local muda durante a mesma sessão

**Confirmado por código e ensaio. Afeta timeshift local.**

`LocalSegmentStore.kt:125` calcula `EXT-X-TARGETDURATION` pelo maior segmento ainda visível. Quando um segmento mais longo sai da janela, esse valor pode diminuir. Também pode aumentar quando entra um segmento mais longo.

Ensaio: publicar segmentos de 6 s e 3 s, depois acrescentar outro de 3 s numa janela limitada a 10 s. O segmento de 6 s é retirado e o target muda de 6 para 3 na mesma playlist.

O RFC 8216, secção 6.2.1, exige que esse valor permaneça constante. Foi demonstrada a violação na geração da playlist, não um bloqueio do ExoPlayer no dispositivo. Uma mudança assim pode alterar decisões de atualização e de disponibilidade de segmentos.

**Recomendação:** definir uma duração-alvo fixa para a apresentação local, preferencialmente derivada da playlist upstream validada, e preservá-la após a rotação da cache. Simplesmente guardar o máximo histórico e continuar a aumentá-lo também não cumpre a imutabilidade. Caso surja conteúdo que ultrapasse o valor permitido, tratar explicitamente uma nova apresentação ou uma origem inválida; não alterar silenciosamente o mesmo manifesto. Testar segmentos de duração variável e a janela a rodar.

## C09 — AES-128 é aceite à entrada do timeshift, mas não é desencriptado

**Incoerência confirmada por código e ensaio de componentes. Afeta timeshift local cifrado; não o HLS normal do ExoPlayer.**

O parser diferencia AES-128 suportável de outros métodos não suportados. `LocalTimeshiftSession.kt:160` aceita esse caso. Porém, o método `download` copia os bytes e exige imediatamente que sejam um MPEG-TS legível (`:199`), sem obter a chave nem desencriptar.

Ensaio: uma playlist AES-128 passa o predicado inicial. Um TS sintético cifrado é rejeitado pela validação de TS. O mesmo ficheiro, depois de desencriptado pelo `HlsAes` já existente no Core, volta a passar. Este ensaio comprova os componentes e a incompatibilidade do fluxo lido no código; não executa a sessão Android completa.

**Recomendação em duas opções explícitas:**

1. Correção mínima: detetar o caso cedo e comunicar que aquele stream não é elegível para o timeshift local, mantendo a possibilidade de o ver diretamente.
2. Compatibilidade completa: reutilizar `HlsAes`, obter a chave com os cabeçalhos e cancelamento da sessão, aplicar o IV explícito ou derivado da sequência e só depois validar/publicar o segmento. Testar troca de chave, passagem AES-128/NONE, IV inválido, resposta de chave truncada, falha de desencriptação e cancelamento. Não guardar chaves nos logs ou backups. Outros métodos continuam sujeitos ao suporte real.

A segunda opção é uma funcionalidade adicional com testes próprios; não se deve anunciar suporte apenas por já existir um desencriptador nas gravações.

## C10 — Compatibilidade das variantes depende apenas dos codecs nativos

**Limitação confirmada. Benefício condicionado ao codec e à box. Afeta seleção para timeshift local.**

`HlsVariantSupport` consulta `MediaCodecList` para áudio e vídeo. A app dispõe também de um renderer FFmpeg para certos formatos de áudio, que esta verificação não consulta.

No ensaio, uma variante H.264 + AC-3 foi recusada quando o callback declarou vídeo suportado e AC-3 nativo ausente. Não foi carregado FFmpeg na JVM nem demonstrado suporte no dispositivo. A conclusão segura é que a elegibilidade atual ignora esse possível caminho.

**Recomendação:** usar a capacidade do percurso efetivamente disponível: codec nativo ou suporte confirmado do FFmpeg para áudio, mantendo as restrições de vídeo e da estrutura HLS. Não aceitar todas as variantes só porque a biblioteca FFmpeg carrega. A existência de áudio numa playlist separada continua a ser uma limitação distinta do produtor local.

## C11 — Rota prevista apresentada como se fosse confirmada

**Confirmado pela API e pelo código. Impacto principal em diagnóstico e ajustes de sincronismo por saída.**

Em Android 13+, `AudioRouteSync.kt:38` usa `getAudioDevicesForAttributes()` e devolve `estimated=false`. A documentação diz que a API devolve os dispositivos que seriam utilizados para aqueles atributos. Não confirma a rota do AudioTrack que está a tocar.

**Recomendação:** marcar esse resultado como estimativa. Quando necessário, observar a rota do AudioTrack ativo através de um ponto suportado pelo Media3, com callbacks removidos na libertação. Não usar reflexão sobre campos privados nem criar polling por fotograma. Nos casos sem rota comprovada, assumir estado desconhecido/estimado.

Com todos os offsets por saída a zero, o código atual já evita o polling. Portanto, não há fundamento para culpar esse polling pelos cortes nas definições padrão.

## C12 — Fallback de inicialização não é recuperação de uma falha a meio do áudio

**Limite confirmado do mecanismo; proposta condicionada a evidência de falha de codec. Afeta reprodução direta.**

`setEnableDecoderFallback(true)` trata falhas de inicialização de codecs de menor prioridade. Não garante mudança para FFmpeg se um codec que já começou a tocar ficar posteriormente sem produzir áudio. O registo de um renderer FFmpeg também não significa que esteja selecionado.

**Recomendação:** registar o descodificador e os eventos reais; se houver falha persistente atribuída ao codec de áudio, avaliar uma única tentativa de recuperação com outro percurso de áudio, mantendo vídeo por hardware. Quando a API obrigar a reconstruir o player, assumir essa interrupção e preservação de posição/faixa em vez de prometer uma troca invisível. Não atuar por silêncio de amplitude baixa: um programa pode ter silêncio legítimo.

Não forçar software ou hardware para todos os formatos. AAC em software pode ser o percurso normal e eficiente; uma opção geral de hardware para vídeo não implica que o áudio deva usar um codec hardware.

## C13 — Diagnóstico da origem e da saída de áudio deve ser separado

**Omissão de apresentação confirmada; não é prova de descodificação incorreta.**

A linha de áudio do overlay utiliza o `audioFormat` de entrada. O watchdog já regista a configuração real do AudioTrack, mas a linha de saída não apresenta separadamente a sua frequência e codificação. A mesma emissão pode ter metadados de origem e uma configuração PCM de saída diferentes, sem que isso constitua um erro.

**Recomendação:** distinguir, de forma compacta, codec/frequência de origem e PCM/passthrough/frequência de saída. Juntar eventos de criação e libertação da saída, mudança de formato e underrun ao instante de uma pausa. Não impor 48 kHz nem converter todos os canais apenas por o diagnóstico mostrar outro valor.

## C14 — Alocações evitáveis ao preferir SDR

**Comportamento confirmado por leitura. Ganho por medir e prioridade baixa.**

Com `Preferir SDR` ativo, `SdrVariants.ParserFactory` envolve também as playlists de segmentos numa captura de bytes. Depois de fazer parse, só modifica playlists multivariantes. As playlists normais podem, assim, ser copiadas sem necessidade em cada atualização, até ao limite de captura de 256 KiB.

**Recomendação:** limitar a captura ao percurso em que seja necessária para identificar as variantes. Preservar o parser Media3, as definições de variáveis e os casos especiais que hoje já são excluídos. Confirmar alocações e resultado do parse antes/depois.

Esta opção vem desativada por defeito. Não apresentar esta pequena otimização como solução confirmada para cortes de dois segundos.

## Questões aprofundadas que não justificam uma mudança geral

### Cache de transmissão e chaves

Não foi encontrada uma cache HTTP/disk de segmentos inserida no percurso normal do ExoPlayer. A reutilização das fábricas de MediaSource não é uma cache dos bytes do canal. O timeshift é um produtor local distinto.

Não recomendo acrescentar uma cache genérica de playlists em disco, normalizar URLs retirando tokens/queries ou partilhar chaves de cache entre contas. Playlists, segmentos, byte ranges, init segments e chaves têm identidades e regras de validade diferentes. Um cache hit incorreto pode ser mais prejudicial do que descarregar novamente.

O HLS normal já usa o suporte AES do Media3. A falha C09 não permite concluir que a desencriptação do player normal está avariada. Eventuais rotações de chaves/tokens precisam de observação própria.

### Memória, bitrate e pré-buffer

O ExoPlayer tem alvos de 24 MiB ou 16 MiB no perfil de pouca RAM, com escalamento limitado para reservas maiores. O limite em bytes pode prevalecer sobre o tempo solicitado. Por exemplo, 16 MiB correspondem aproximadamente a 5,37 s a 25 Mbit/s, antes de outras particularidades da transmissão.

Isso não é, por si só, uma implementação avariada: protege a memória. Recomendo explicar quando a duração pretendida não foi atingida por limite de bytes; não desativar globalmente o limite nem prometer que 10 s configurados implicam 10 s carregados. Uma adaptação de orçamento por bitrate exige limites reais do dispositivo e teste de pressão de memória.

O sinal de memória reduz o orçamento de Exo em sintonia posterior deliberadamente, preservando o canal atual. Não identifiquei evidência suficiente para recomendar uma reconstrução imediata no instante do aviso.

### Escolha de faixas, variantes e temporização

O Media3 já tem adaptação HLS e restrições de mistura de codecs, sample rates e canais. Não há fundamento para bloquear todas as adaptações: isso pode causar mais buffering quando a largura de banda cai. Mudanças reais de faixa/codec devem ser correlacionadas com o corte.

As variantes de uma playlist HLS e os canais separados com nomes HD/FHD/HEVC são mecanismos diferentes. Uma otimização não deve alterar silenciosamente a prioridade de versões escolhida pelo utilizador.

Quanto a GOPs, PTS, gaps e descontinuidades, não existem amostras do instante do corte para confirmar um defeito do fornecedor ou do extractor. Manter as exceções por canal; não ativar flags permissivas de MPEG-TS para todos os streams e não fabricar continuidade temporal.

### Renderer de vídeo personalizado

Foi comparada a construção personalizada com a factory Media3 1.11.1. Várias opções não passadas explicitamente têm os mesmos defaults nesta versão. Não está demonstrada uma perda atual de otimização por esse motivo. É um risco de manutenção em futuras atualizações e em setters adicionais, a cobrir por comparação dirigida; não contar como bug de desempenho já confirmado.

O modo assíncrono MediaCodec já segue a política Media3 no modo Automático, que o ativa por defeito em Android 12+. Forçá-lo globalmente não seria uma otimização nova para esses dispositivos e pode prejudicar outros.

## Priorização adicional

- Integrar C11 e C13 na fase de diagnóstico: observação correta antes de decisões automáticas.
- Se for usado timeshift local, corrigir C08 e decidir explicitamente o suporte C09. São os dois achados funcionais mais concretos deste complemento.
- Rever C10 quando existirem variantes recusadas apesar de áudio reproduzível pelo caminho FFmpeg.
- Investigar C12 perante prova de falha de codec, sem criar mais um watchdog de reconexão por defeito.
- Deixar C14 para uma pequena fase de alocações, após medir ou confirmar utilização de Preferir SDR.

As fases de rede A-C do relatório anterior continuam a ser a prioridade para reduzir esperas HLS. Os achados de cache local só devem ser usados para explicar problemas quando o timeshift local estiver realmente ativo.

## Ensaios e limites

```
RESULT local_playlist_targetDuration_before=6 after_eviction=3
RESULT local_AES128_entry_accepted=true TS_before_decrypt=false TS_after_decrypt=true
RESULT local_variant_supported_if_platform_AC3_missing=false checked=[video/avc, audio/ac3]
PASS 3 targeted local component scenarios; no Android playback or provider test
```

Evidência externa ao código funcional:

C:\Users\renat\Downloads\OwnTV-HLS-audio-cache-analise-2026-11-03-uy3j48n3

Nenhuma configuração do utilizador, biblioteca ou ficheiro funcional da app foi alterado. Apenas este complemento de análise foi criado. A validação de cortes de áudio/HLS no dispositivo continua pendente; não há percentagens de melhoria medidas.

## Fontes primárias

- [RFC 8216, secção 6.2.1: imutabilidade da duração-alvo](https://datatracker.ietf.org/doc/html/rfc8216#section-6.2.1).
- [AudioManager: rota prevista para atributos](https://developer.android.com/reference/android/media/AudioManager#getAudioDevicesForAttributes(android.media.AudioAttributes)).
- [AudioRouting: rota do áudio ativo](https://developer.android.com/reference/android/media/AudioRouting#getRoutedDevice()).
- [Media3: fallback de descodificador na inicialização](https://developer.android.com/reference/androidx/media3/exoplayer/DefaultRenderersFactory#setEnableDecoderFallback(boolean)).
- [Media3: personalização e modo assíncrono](https://developer.android.com/media/media3/exoplayer/customization).

Os comportamentos concretos foram cruzados com os sources JAR Media3 1.11.1 e com o Core personalizado atual. As propostas condicionadas não representam falhas reproduzidas na Xiaomi.
