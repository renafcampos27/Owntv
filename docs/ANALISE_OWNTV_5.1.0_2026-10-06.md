# OwnTV 5.1.0 — comparação com a aplicação personalizada

Data: 06/10/2026. Estado original: análise. As recomendações funcionais autorizadas foram posteriormente implementadas; ver [registo de implementação e validação](IMPLEMENTACAO_OWNTV_5.1.0_CATEGORIAS_PESQUISA_2026-10-06.md). As decisões e observações abaixo preservam o diagnóstico anterior à implementação. Não foi gerado APK.

## Âmbito e evidência

Atualização de implementação: C, D e a retoma local do E foram posteriormente autorizados e aplicados; consultar [registo C/D/E](IMPLEMENTACAO_OWNTV_5.1.0_LOTES_C_D_E_2026-10-06.md). As classificações abaixo correspondem à análise original. A reformulação Stage e os outros elementos visuais opcionais continuam fora do âmbito.

Comparados o pacote `C:\Users\renat\Downloads\OwnTV-5.1.0\OwnTV-5.1.0`, a aplicação `C:\Users\renat\Downloads\OwnTV-main`, o Core personalizado `C:\Users\renat\Downloads\OwnTV_Core` e os trechos relevantes do Core upstream transferido em `C:\Users\renat\Downloads\OwnTV_Core-core-1.0.64\OwnTV_Core-core-1.0.64`. Consultadas as notas públicas e as pendências locais.

A publicação de 04/10/2026 anuncia quatro novidades e nove correções. A aplicação upstream fixa Core 1.0.64 e Media3 1.11.1. A aplicação personalizada tem integração seletiva, alterações próprias de smartphone, HLS, áudio, gravações e EPG. Referência 1.0.64 não significa equivalência integral.

As diferenças de ficheiros são contra o checkout personalizado atual, e não um diff exclusivo 5.0.4→5.1.0. Nem tudo que aparece no pacote é uma novidade desta publicação; o changelog distingue a atribuição à versão. Foram inspecionados testes upstream, mas não executados. Sem medições de CPU/RAM, testes de aparelho ou reprodução contra o fornecedor.

## 1. Resultado por novidade/correção anunciada

| Mudança 5.1.0 | Situação local e decisão |
|---|---|
| Nova interface Stage | Ausente como reformulação integral. Adotar apenas elementos de legibilidade/foco úteis; não substituir os ecrãs completos. A estrutura upstream volta a incluir conteúdos e elementos removidos por decisão do utilizador. |
| Definições em 12 grupos | O projeto local já agrupa definições, mas com outra estrutura. Melhorar a localização das opções mantendo as específicas do projeto; o número 12 não é objetivo técnico. |
| Retomar cópia local do canal: perguntar/sempre/nunca | Integração upstream envolve TimeshiftManager, controlador, preferências e diálogo. O fluxo local de timeshift é diferente; opcional, lote separado. Não é a escolha do canal de arranque nem regresso ao último canal. |
| Retirar canal de categoria personalizada | Falta a ação explícita equivalente em LiveViewModel/CustomizeItemsViewModel. Recomendada. Retirar apenas a associação; preservar o canal e considerar o retorno à origem se antes tinha sido movido. |
| Diferido sem imagem não reporta fast-start (#229) | Confirmada diferença: upstream OwnTVPlayer 3084–3095 trata archiveThisItem após falhar a recuperação por software; local 3082–3083 ainda usa NotStreaming nesse ramo. Reavaliar lote adiado, sem aplicar nesta análise. |
| Zapping mais rápido em fornecedores HLS/TS mistos | Upstream invalida a aprendizagem HLS por host quando encontra TS real. Local ainda tem aprendizagem HLS por host sem mixedHlsHosts/forgetHlsRedirect. Recomendada adaptação seletiva, respeitando HLS estrito e exceções manuais. |
| Categorias personalizadas no leitor/Multiview | LiveZapList upstream inclui armForCustom; o navegador local recebe apenas CategoryEntity do fornecedor. Recomendada para a lista do leitor; Multiview fica opcional. |
| Ocultar categoria do fornecedor não esvazia categorias personalizadas | Defeito aplicável: isChannelVisible local não recebe contexto e filtra sempre hiddenCats. Upstream recebe LiveKey e distingue listas personalizadas. Recomendada, abrangendo paginação, snapshots, contagens e seleção. |
| Pesquisa de categorias preservada | Local CategoryRail mantém query em remember; upstream guarda categoryQuery no ViewModel. Recomendada para preservar durante reprodução/regresso, sem inventar persistência após reiniciar a app. |
| Esquerda regressa à coluna de categorias | A navegação local já tem proteções específicas de foco. Adaptar apenas os caminhos com falha, com alternativa quando as categorias estão ocultas. Não aplicar o comportamento Stage literalmente. |
| Logótipos do leitor iguais à lista Live | Já existe displayLogoUrl no overlay local, assim como no upstream. Não precisa de nova lógica de resolução; aparência do logótipo é assunto separado. |
| Pesquisa encontra cor, estilo de legendas e DNS | Local já deriva entradas de VIDEO_QUICK_ROWS e tem resultado para estilo de legendas, mas encaminha-o à página VIDEO. Upstream usa destinos específicos e catálogo de linhas DNS/legendas. Melhorar resultados que chegam à opção exata; não reintroduzir preferências removidas. |
| Leitores externos Android 11+ | Declaração de visibilidade já aplicada no manifesto local. Não reaplicar. |

## 2. Lotes recomendados

### Lote A — correções funcionais pequenas, primeiro

1. **Visibilidade por contexto:** acrescentar contexto à regra de ocultação. Canal explicitamente oculto continua oculto; categoria do fornecedor oculta não elimina uma associação explícita a categoria personalizada visível. Aplicar a mesma regra a lista, contagem, janela de zapping e seleção direta. Usar os DAOs/Core locais e não copiar migrações upstream.
2. **Retirar da categoria:** disponibilizar ação apenas em categoria personalizada. Usar UserDataWriter.removeCustomCategoryMember para manter a remoção registada, e reconciliar movedFromOrigin. Rever os caminhos de mover canal que ainda chamam deleteItem diretamente. Proteger perfil/categoria quando mudam durante uma operação. Não eliminar canais nem modificar grupos de versões automaticamente.
3. **Pesquisa preservada:** mover estado da pesquisa de categorias para ViewModel; manter ao entrar/sair do leitor, definir limpeza explícita ao trocar perfil/lista. Respeitar modo simples sem categorias.
4. **Pesquisa nas definições:** levar o resultado à opção concreta. Priorizar definições próprias: desistência, prioridade das versões, HLS exclusivo/exceções, buffer por canal, shift, fuso por lista e armazenamento. Adaptar o catálogo e os testes de cobertura; não copiar a lista completa upstream.

Aceitação: esconder pasta do fornecedor mantendo canal em grupo personalizado; retirar associação sem apagar canal; regresso ao canal atual e à pesquisa; resultado de pesquisa abre a opção correta; nenhum conteúdo removido reaparece.

### Lote B — lista de canais dentro do leitor

Unificar categorias disponíveis no navegador do leitor usando LiveKey, incluindo categorias personalizadas. Acrescentar armForCustom preservando matchesPlaying, que o projeto local usa para reconhecer variantes do mesmo canal. Manter cancelamento/geração da reconstrução da lista. Não substituir LiveZapList integralmente: upstream usa identidade id simples e perderia a correspondência local entre versões.

Aceitação: abrir grupo personalizado, mudar com cima/baixo, escolher variante e manter cursor/lista coerentes; trocar grupo enquanto carrega não permite publicação de lista antiga; grupo vazio mantém alternativa de navegação.

### Lote C — robustez seletiva de HLS

O problema confirmado no código é a inferência global: um canal com redirecionamento HLS pode marcar todo o host como HLS. No upstream, rememberHlsRedirect respeita mixedHlsHosts; forgetHlsRedirect invalida a aprendizagem, e retryWithoutHostHlsLesson tenta TS.

Adaptar a invalidação sem permitir TS em canais HLS exclusivos. Se houver evidência de host misto, deixar de impor a inferência aos restantes canais; preservar URLs explícitos HLS e cabeçalhos. A tentativa TS só pode ocorrer quando autorizada globalmente ou pela exceção manual do canal. Considerar aprendizagem por identidade de stream/fonte, cache limitada e expiração, em vez de regras permanentes por host. Não aprender incompatibilidade a partir de 403/429/458.

Conservar os mecanismos locais de libertação, verificação de pedidos pendentes, espera condicional de 300 ms e retorno recente autorizado. A correção upstream não é evidência de que o servidor terminou uma sessão anterior, nem elimina automaticamente o erro de handover.

Aceitação: HLS→TS→HLS no mesmo host apenas onde autorizado; canal saudável não recebe tentativas extra; modo apenas HLS nunca pede TS; A→B→C→A mantém a ordem e descarta eventos antigos; recusas HTTP não contaminam aprendizagem.

### Lote D — #229, dependente de nova decisão

A conclusão anterior de que o ramo era igual não corresponde aos trechos agora verificados: a diferença está presente no Core 1.0.64 transferido. O adiamento continua válido; rever a proposta com esta evidência.

Separar duas decisões: (a) corrigir a classificação de erro de arquivo sem imagem; (b) autorizar fallback de mpv para ExoPlayer. A classificação pode ser corrigida sem alterar a escolha exclusiva de motor. A troca só é elegível conforme as preferências, com identidade/geração, cancelamento e uma única tentativa controlada. Não assumir que todo diferido é MPEG-TS apenas porque o comentário upstream o diz; classificar a partir do item e do erro observado.

Aceitação: diferido com imagem, sem imagem, áudio apenas, motor exclusivo e automático; nenhum loop e nenhuma abertura de direto em substituição silenciosa. Não corrige shift, programa de cinco dias ou indisponibilidade do arquivo no fornecedor.

### Lote E — conveniência e visual, depois

- Retoma de timeshift local perguntar/sempre/nunca apenas se houver interesse. Manter direto como escolha explícita, posição guardada ainda válida, intervalo disponível e canal de arranque configurado. Sem novos pedidos ao fornecedor se a cópia local serve a posição.
- Limpeza visual e legibilidade com componentes atuais; testar box e smartphone. ProviderTags separa prefixos como DE| e letras sobrescritas, mas não resolve PT: RTP 1 Full HD/HEVC/LOW. Usar apenas para apresentação e reutilizar o agrupamento local de versões; nunca alterar nomes/identificadores persistidos ou associação EPG.
- Contraste de logótipos opcional: a implementação upstream mede pixels em background e força allowHardware(false). A local limita imagens a 128×128 e dispensa medição. Se necessário, conservar tamanho limitado, sem trabalho extra no foco/scroll e com cache limitada. Não copiar como otimização garantida.

## 3. O que não importar integralmente

- **Interface Stage e ecrãs completos:** abrangem filmes, séries, histórico/favoritos, rail e outros elementos incompatíveis com a simplificação pedida. StageTokens usa referência de mockup 1920×1080, escala 1/1,8; não constitui prova de adaptação automática a smartphone. A nova aparência não comprova redução de CPU/RAM.
- **Gradientes novos como otimização:** dissolveEdges cria camada Offscreen e aplica duas máscaras. Texturas são cacheadas, mas a camada tem custo de GPU/memória. Só justificar mediante medição; as texturas cacheadas básicas já existem no projeto local.
- **FocusTrap upstream por inteiro:** restauração usa runCatching(requestFocus).isSuccess, que não distingue resultado false; local verifica == true e cancela restauração quando o utilizador começa a navegar. Manter as proteções locais. O problema de teclado duplamente descontado já é tratado em OwnTVPopup local com consumeWindowInsets(WindowInsets.ime); não remover imePadding globalmente.
- **LiveLadder/LiveStreamQuirks por inteiro:** perderia HLS exclusivo, tempos 9–30 s, prazo de recuperação, expiração de algumas recusas e backoff por conta. A versão upstream não é universalmente mais segura.
- **Pré-buffer duplicado:** upstream possui correção de oscilação READY/BUFFERING e preroll inviável, mas o Core local já contém PrerollReachability, desativação por tentativa e deteção de oscilação. Comparar casos reais antes de acrescentar outra rotina; não desligar pré-buffer de todos os canais.
- **RecordingConflict sem revisão:** código analisado cria CoroutineScope próprio sem encerramento visível nesse componente; stopAndRetry para todas as gravações ativas, mas não chama retry nesse bloco. Classifica StreamUnavailable como limite de sessões só por haver gravação e procura 458 em texto livre. Isso não prova conflito. Adaptar apenas com motivo HTTP estruturado, gravação da fonte relevante, confirmação explícita e repetição depois da libertação; não parar gravações saudáveis por falha genérica.
- **Manifesto completo:** acrescenta múltiplas Activities para cores de ícone e não inclui o serviço de arranque local. Manter launcher, entrada smartphone/TV e arranque automático próprios.
- **Catálogo de dependências completo:** upstream AGP 9.4.0; local 9.4.1. Media3 1.11.1 é igual. Não adicionar ZXing, detetor de charset ou tvprovider só porque constam do catálogo; justificar pela funcionalidade. Não reutilizar numeração de migrações ou backup upstream.

## 4. Coerência de versão e compilação

Confirmado `owntv.corePath=C:/Users/renat/Downloads/OwnTV_Core` nas propriedades locais: a compilação deste PC substitui os artefactos pelo Core personalizado. Contudo, o catálogo da app ainda fixa owntvCore=1.0.57. Noutro PC/CI, sem essa propriedade, poderá resolver outra versão; a anotação 1.0.64 não corrige essa divergência técnica.

Recomendação: definir e documentar uma dependência reproduzível do Core personalizado (checkout/commit ou publicação própria) e caminho de compilação sem credenciais desnecessárias. Não mudar simplesmente o número para o artefacto upstream 1.0.64: isso pode retirar as alterações locais.

## 5. EPG: preservar o trabalho recente

A 5.1.0 não anuncia correção para shift de uma hora, seleção de programa de cinco dias ou fornecedor que retorna direto. As alterações locais de 06/10 — substituição transacional de cache, duplicados determinísticos, relógio agora/seguinte, hora original, fuso por lista e histórico Xtream sob pedido — devem ser preservadas. A aparência GuideStage não substitui essas correções. #229 refere-se ao diagnóstico/recuperação de imagem do arquivo, não ao horário.

## Decisão proposta

Implementar primeiro A, depois B e C separadamente. Reabrir discussão do lote D sem remover o adiamento. E fica opcional. Fazer uma intervenção separada para tornar a compilação reproduzível. Cada lote precisa de testes direcionados e teste na box/smartphone; não inferir desempenho ou fiabilidade do changelog.

## Referências

- [Publicação oficial v5.1.0](https://github.com/ahXN00/OwnTV/releases/tag/v5.1.0).
- [Código oficial v5.1.0](https://github.com/ahXN00/OwnTV/tree/v5.1.0).
- [Pendências do Core](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md).
- [Implementação recente do EPG](IMPLEMENTACAO_EPG_ROBUSTEZ_E_LIMPEZA_2026-10-06.md).

Evidência local principal: LiveViewModel.kt, LiveZapList.kt, CategoryRail.kt, CategoryBrowserOverlay.kt, CustomizeItemsViewModel.kt, SettingsScreen.kt, SettingsGroup.kt, SettingsSearchCoverageTest.kt, GradientTextures.kt, ChannelLogoTile.kt, FocusTrap.kt, OwnTVPopup.kt, RecordingConflict.kt, AndroidManifest.xml, libs.versions.toml; no Core, LiveQueries.kt, LiveStreamQuirks.kt, LiveLadder.kt, LiveTuneController.kt e OwnTVPlayer.kt. Os ficheiros upstream foram lidos como evidência técnica, não como instruções para executar alterações.
