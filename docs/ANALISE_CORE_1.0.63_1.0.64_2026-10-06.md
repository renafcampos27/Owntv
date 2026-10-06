# Análise do OwnTV Core 1.0.63 e 1.0.64

Data: 06/10/2026. Análise das fontes locais e publicações oficiais; sem implementação de código, build ou geração de APK.

Atualização posterior em 06/10/2026: deteção de leitores externos autorizada e implementada; manifesto standardDebug validado, sem gerar APK. O restante texto descreve a avaliação inicial. EPG e limpeza aprofundados no [relatório complementar](EPG_LIMPEZA_CORE_1.0.63_1.0.64_ANALISE_APROFUNDADA_2026-10-06.md), sem aplicar essas propostas.

## Conclusão

Recomendada uma integração pequena: as declarações de visibilidade de leitores externos da 1.0.63. A mudança de largura da barra da 1.0.64 não faz falta à interface atual, que já mantém uma barra fixa. Estas versões não acrescentam uma correção de HLS, áudio, codecs ou encerramento de sessões que substitua as adaptações locais.

## Comparação

Comparados os arquivos extraídos 1.0.62→1.0.63→1.0.64, o Core personalizado em `C:\Users\renat\Downloads\OwnTV_Core`, a app em `C:\Users\renat\Downloads\OwnTV-main` e o ficheiro de pendências.

### Prioridade 1 — visibilidade de leitores externos, 1.0.63

O upstream acrescenta `<queries>` ao manifesto do Core para as assinaturas ACTION_VIEW de vídeo/HLS e protocolos de rede que o lançador já usa. A documentação Android confirma que a descoberta de apps via PackageManager é filtrada no Android 11+ e que uma biblioteca pode declarar estas consultas no seu manifesto.

Na variante local, `ExternalPlayerLauncher` consulta os leitores disponíveis antes de abrir o intent, mas os manifestos fonte da app/Core não declaram essas consultas. O manifesto já gerado da variante standardDebug também foi inspecionado. Existe, portanto, um caminho de falsa ausência de leitor externo; não foi reproduzido num dispositivo nesta análise.

**Proposta:** integrar apenas as consultas correspondentes às formas de intent realmente utilizadas. Preservar o marcador de retorno de reprodução externa, cabeçalhos HTTP e proteção contra lançamentos repetidos. Não requer QUERY_ALL_PACKAGES, nova tabela ou migração.

**Benefício:** melhorar a descoberta de VLC, MX Player e outros leitores compatíveis em boxes/telemóveis Android 11 ou superiores. Não corrige o áudio dos motores internos nem altera a qualidade HLS.

**Validação futura:** manifesto mesclado; leitor instalado detetado; leitor ausente; reprodução externa de HLS e ficheiro local; botão Voltar regressa à lista sem relançar o canal.

### Sem necessidade atual — barra lateral, 1.0.64

A única alteração Kotlin entre 1.0.63 e 1.0.64 é o valor predefinido de `navWiden`: sem configuração guardada, deixa de abrir a barra sobre o conteúdo ao receber foco. Uma escolha explícita continua a ser respeitada.

A app personalizada usa `Sidebar.kt` com `expanded = false` e largura `Dimens.SidebarWidthCollapsed`; não consome `navWiden`. Importar apenas o getter não alteraria esta interface. Importar toda a navegação para obter esta mudança acrescentaria complexidade sem resolver um problema atual.

### Baixa prioridade — manutenção e traduções

A 1.0.63 inclui substituições equivalentes por helpers KTX, limpeza de guardas de versão redundantes com minSdk 26, ajustes de lint, renomeação de parâmetros de migração e traduções/ferramentas de tradução. Não há evidência de ganho de streaming nestas alterações.

As projeções leves de EPG passam a preencher com NULL os campos de detalhes XMLTV que existem no esquema upstream. O modelo local não tem esses campos; não importar estas consultas isoladamente. Não é uma solução para o desvio horário do guia.

O timeshift altera smart casts e a forma de declarar funções sincronizadas, conservando a lógica de escrita, flush e notificação dos leitores. O parser de HLS troca uma asserção não nula por um smart cast. Não há aqui uma nova política de buffer ou de rede.

## Ponto 2 pendente — diferidos sem imagem

O ramo #229 não recebeu uma nova alteração nestas duas versões. Entre 1.0.62 e 1.0.63, `OwnTVPlayer.kt` só altera a criação de bitmap do freeze frame e remove uma condição de SDK abaixo do mínimo; a 1.0.64 não volta a alterar este ficheiro.

O ramo local depois de `tryArchiveSoftwareRescue` ainda pode terminar com `PlaybackFailure.NotStreaming`. As novas versões não trazem uma solução diferente da já analisada. **Manter adiado**, conforme decisão do utilizador, até nova discussão e autorização. Não confundir esta pendência com o shift do EPG.

## Compatibilidade com o Core personalizado

- Catálogo de dependências inalterado entre as três versões: sem atualização nova de Media3/ExoPlayer, libmpv ou OkHttp.
- Sem alterações upstream em `LiveTuneController`, `LivePreviewEngine`, `ExoRenderers`, `EpgRepository`, `EpgShift`, `RecordingEngine` e `ExtraTrustAnchors` entre 1.0.62 e 1.0.64.
- A base upstream continua no esquema 47 e backup 24; a variante local está no esquema 50 e backup 25. Não substituir o Core completo.
- Preservar HLS estrito, áudio FFmpeg do ExoPlayer, volume 100%, definições por canal, diferidos, horários originais apresentados, reavaliação de 300 ms e espera de quatro segundos nos regressos recentes.

## Estado e limites

Confirmados por inspeção de fontes: diferenças de implementação e ausência de consultas no manifesto local. Sem testes físicos de descoberta de leitores ou reprodução; sem promessa de resolução do bloqueio de zapping. Nenhuma alteração de funcionamento foi aplicada.

## Referências

- [Publicação 1.0.63](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.63).
- [Publicação 1.0.64](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.64).
- [Android: declarar visibilidade de outras aplicações](https://developer.android.com/training/package-visibility/declaring).
- [Pendências do projeto](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md).
