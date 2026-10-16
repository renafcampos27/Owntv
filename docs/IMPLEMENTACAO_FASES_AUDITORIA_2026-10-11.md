# OwnTV TV — implementação por fases da auditoria

Plano de referência: `AUDITORIA_COUNCIL_DEFINICOES_REPRODUCAO_UI_LIMPEZA_2026-10-11.md`.
Âmbito: app Android TV e Core personalizado. Mobile não alterado. Não foi gerado APK.

## Fase 1 — identidade da reprodução e áudio Multiview

- Captura PixelCopy/timeout usam uma continuação única vinculada à sessão, superfície e motor originais.
- A fila nativa verifica novamente geração e motor antes de stop/detach; uma tarefa antiga não pode atuar na sessão nova.
- Regresso Exo→mpv verifica geração, além do URL, e mantém um job de handoff cancelável.
- O job é desligado antes de chamar loadUrl, evitando cancelar a própria continuação válida.
- Remover/expirar o mosaico audível transfere áudio para um sobrevivente sem erro; sem sobreviventes, nenhum motor recebe som.
- A revisão independente do council detetou a necessidade de excluir mosaicos ERROR; o ajuste foi aplicado nos dois percursos.

## Fase 2 — definições e diagnóstico

- Idiomas são aplicados ao Exo ativo sem reabrir a ligação. Configuração de render mpv não altera a apresentação pertencente ao Exo.
- Descodificador em diferidos/gravações Exo informa aplicação na abertura seguinte; não há recriação global a cada alteração.
- Seletores mostram motor escolhido/efetivo e sobreposição por HLS exclusivo através do resolvedor existente.
- HDR identificado como sinalização do mpv; preferir software distingue preferência e fallback.
- Pesquisa das definições alinhada com o novo título HDR; o teste de cobertura detetou a divergência e passou após a correção do índice.
- Multiview sem reprodução após 12 s recebe mensagem neutra, sem acusar o hardware; erros específicos conservam a sua identificação.

## Fase 3 — trabalho desnecessário

- Scroll guardado por snapshotFlow, fora da observação da composição principal.
- Indicadores do modo Áudio não criam animações quando desligadas ou pausados.
- Navegação STATIC não subscreve capacidades; dinâmica observa os canais necessários. Contratos públicos do Core preservados.

## Fase 4 — limpeza e estados úteis

- Removido wiring de meteorologia da App, preservando chaves/compatibilidade do Core.
- Removido leitor de trailers, callbacks e dependência YouTube sem percurso de dados na configuração TV.
- EPG distingue carga, vazio e falha; nova tentativa apenas na linha afetada, conservando dados anteriores/fallback.
- Durante carregamento, OK não abre programação antiga. Em falha com dados disponíveis, ações existentes continuam disponíveis.
- HUD mostra espera de reabertura em segundos ou carregamento; usa dados do controlador sem pedidos adicionais.
- Mantida mensagem específica de espera HTTP do fornecedor quando disponível. Não se inventa uma causa de recuperação.

## Consistência do Core local e CI

- Core e catálogo usam versão própria `1.0.57-own.20261011`, sem alegar equivalência total a upstream.
- Snapshot de fontes identificado por SHA-256 acompanha o projeto; CI não volta a escolher a antiga tag/artefacto 1.0.57.
- Override local só é aceite se corresponder aos inputs registados; divergências falham com instrução explícita.
- Snapshot exclui outputs, ficheiros locais, chaves e credenciais Gradle. Não é incluído no APK.
- Regeneração futura documentada em `vendor/README.md`.
- Snapshot atual: 937 ficheiros, SHA-256 `1a0507d713a2fa0a8f8f664707fed002bf94bf39bc0fa3e135f01948193fdb80`.

## Validação

Resultados confirmados na primeira execução integrada:

- Compilação StandardRelease da app e módulos Core/player passou.
- Core: 1 152 testes, zero falhas/erros/ignorados.
- Player: 490 testes, zero falhas/erros/ignorados.
- App: 331 testes, zero falhas/erros/ignorados na execução final; total dos três módulos: 1 973 testes.
- Testes novos de propriedade de continuations, subscrições de navegação e contagem/cancelamento de espera.
- Teste da seleção de inputs do snapshot passou, incluindo exclusão de outputs e configurações locais.
- Verificação i18n passou: 53 entradas de baseline e 1 140 literais classificados.
- Configuração Gradle habitual, sem forçar o snapshot, passou e validou a identidade do Core.

Validação final com o snapshot atualizado: `:app:testStandardDebugUnitTest`,
`:OwnTV_Core:player-core:testDebugUnitTest`, `:app:compileStandardReleaseKotlin` e
`:app:lintVitalStandardRelease` passaram (BUILD SUCCESSFUL, 2 min 51 s).
Lint vital release sem erros bloqueantes. Permanecem avisos de funcionalidades Gradle descontinuadas;
este trabalho não constitui migração para Gradle 10 nem afirma ausência de todos os warnings.

Validação física pendente: PixelCopy/MediaCodec/MPV reais, foco e retry EPG com comando, dois/três mosaicos,
pausa/retoma, diferidos e gravações, suspensão, zapping e regressos à lista. Testes automatizados não provam
a resolução do problema de reabertura Live do fornecedor nem ganhos quantitativos de fluidez.

Preservados: volume da aplicação a 100%, políticas HLS, configurações por canal, diferidos, gravações,
downloads, backups e limpeza da pesquisa ao sair. Nenhum commit, push ou publicação feito neste trabalho.
