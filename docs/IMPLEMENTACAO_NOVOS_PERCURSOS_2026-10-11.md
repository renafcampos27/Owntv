# OwnTV TV — implementação da nova auditoria por fases

Referência: `AUDITORIA_COUNCIL_NOVOS_PERCURSOS_2026-10-11.md`.
Âmbito: App TV e Core personalizado. Mobile não alterado. Nenhum APK, commit ou publicação.

## Fase 1 — gravações e retoma

- A abertura exige foreground e seleção atual; ON_STOP e saída do ecrã invalidam/cancelam preparações pendentes, sem parar uma reprodução já transferida.
- Perfil, cancelamento e seleção são verificados novamente antes de abrir o leitor interno/externo e antes do callback de navegação.
- Ao sair, o leitor liberta a publicação local e o lease. Guarda apenas identidade da gravação, posição e intenção de pausa/reprodução.
- Ao regressar, reabre gravações por partes/em curso através de RecordingManager. Valida disponibilidade, perfil e propriedade da sessão antes da transferência; recursos adquiridos e não transferidos são fechados.
- Gravações apagadas/indisponíveis ou perfil alterado não são reabertas com o URL local antigo. O percurso legado sem identidade também não tenta recuperar esse URL fechado.
- Diferidos conservam manifest, alternativas tipadas, escolha do motor, DRM, headers/UA, provider de reconexão e correção de áudio. Não há retenção de ligação em background para obter retoma.
- A revisão independente verificou geração, identidade do descritor, foreground, existência de outra reprodução e fecho após cancelamento.

## Fase 2 — definições

- Formulários proxy/metadados aguardam a configuração persistida antes de inicializar o draft; dados vazios iniciais já não são confundidos com uma configuração carregada.
- Testes proxy/DNS têm job e identidade. Guardar/editar/reset/disposal invalida resultados anteriores; a chamada HTTP de teste do proxy é cancelada, mesmo se for criada após o reset.
- DNS não expõe um handle para interromper o resolver bloqueante: a coroutine é cancelada e o resultado antigo não pode publicar, mas a resolução pode terminar em background.
- Áudio/zoom recordados têm revisões independentes das escolhas do utilizador, além da geração da reprodução; uma leitura atrasada não substitui um ajuste recente do HUD.
- AFR atualiza o valor e a estratégia Media3 do Exo ativo na thread principal, sem reabrir a transmissão; o valor também é usado nos arranques/rebuilds seguintes.
- Limpar valores guardados aplica-se às próximas aberturas, como os textos EN/PT/PT-rPT agora indicam. Mantém a reprodução atual e a prioridade das opções por canal; um ajuste posterior deliberado no HUD pode guardar novamente.

## Fase 3 — guia e comando

- Quando segue TV DIRETO com ordem Personalizada, o guia usa os ranks ALL_CONTEXT do perfil ativo, conservando a ordem relativa após filtragem e o fallback determinístico dos canais sem posição.
- A janela de desvio EPG participa no mesmo grupo de diálogos. O restauro do foco só é pedido depois de fechar a última janela.

## Fase 4 — trabalho limitado e limpeza

- Pré-carregamento do guia copia no máximo os três canais seguintes, em vez de toda a lista restante.
- Removidos um local sem uso e uma condição que preservava sempre o valor.
- Comentários de preferências atualizados para distinguir o uso TV a 100% das APIs/colunas de volume legadas; contratos de backups preservados.
- Nenhuma nova cache, atraso global, rotina de reconnect, alteração de buffer ou eliminação de funcionalidades de diferidos/gravações/Multiview.

## Validação

Validação integrada concluída: BUILD SUCCESSFUL em 9 min 36 s.

- Core: 1 152 testes, sem falhas/erros.
- Leitor: 498 testes, sem falhas/erros.
- App: 340 testes, sem falhas/erros. Total: 1 990 testes, nenhum ignorado.
- Compilação StandardRelease e lintVitalStandardRelease aprovados, sem erros bloqueantes.
- Verificação i18n e teste da seleção dos ficheiros do snapshot aprovados.
- Snapshot atualizado: 941 ficheiros; SHA-256 `38a9031933d951ee8286ddc810d2490b04ee932e76932de9bb3431b0dc949f16`.

Log da execução: `C:\Users\renat\AppData\Local\Temp\owntv-next-phases-validation.log`.
O Gradle ainda regista avisos de funcionalidades descontinuadas para uma migração futura; não houve
migração para Gradle 10 neste trabalho nem se afirma ausência de todos os warnings.

Novos testes cobrem: abertura após Home/seleções antigas; propriedade da retoma; leituras de preferências versus escolhas recentes; testes de rede antigos/transportes criados tarde; ordem personalizada filtrada; pré-carregamento numa lista instrumentada de 20 000 itens.

Pendente em dispositivo: Home/retorno em partes e gravação em curso, diferidos com alternativas, AFR e saída HDMI, foco OK/Voltar na janela de desvio, preenchimento inicial e edição dos formulários. Testes automatizados não provam ausência de problemas do fornecedor nem ganhos quantitativos de fluidez.

Uma indicação adicional compacta de origem canal/lista/global é uma proposta de apresentação do relatório, não um novo resolvedor de preferências implementado neste lote. As correções por fases usam as fontes de definições já existentes.
