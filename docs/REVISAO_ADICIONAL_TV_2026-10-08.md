# OwnTV — análise adicional para Android TV e boxes

Data: 2026-10-08. Natureza: revisão estática e planeamento; nenhuma alteração do código da aplicação. Complementa REVISAO_COUNCIL_APP_APOS_LIBMPV_2026.10.1.md e respeita OWNTV_APENAS_TV_2026-10-07.md. Recomendações de smartphone estão excluídas.

## Evidência e limites

Inspeção do checkout atual da App e Core. Fontes oficiais Android e Kotlin consultadas. Não foram executados testes, reproduções em box ou medições nesta intervenção. Distinguir defeito visível no código, risco condicionado a concorrência/provedor de ficheiros e oportunidade de desempenho não medida. Esta análise não atribui estas fragilidades ao zapping HLS em direto.

## A9 — procurar corretamente partes de gravações externas interrompidas

Classificação: defeito confirmado na lógica de descoberta; manifestação depende de finalização interrompida e destino SAF.

Evidência: Core/core/src/main/java/tv/own/owntv/core/recording/RecordingParts.kt:88 retorna null para documento não-index quando create=false, antes de procurar a pasta existente .owntv-parts-ID. stagedBytes em :219 e delete em :227 usam essa descoberta sem criação. RecordingManager.kt:622 chama delete antes de apagar a linha. RecordingEngine.kt:393 contabiliza stagedBytes ao fechar a reserva.

Consequência: uma pasta criada durante finalização e ainda não publicada como index pode não ser encontrada. A limpeza pode considerar que não há partes e o cálculo pode omitir esses bytes. Não afirmar que todas as gravações externas falham.

Solução: procurar sempre a pasta existente; create=false deve impedir apenas a criação. Validar nome, proprietário e conteúdo antes da eliminação. Manter a linha se a limpeza falhar. Aceitação: interromper entre partes e publicação do index, reiniciar e verificar descoberta, contabilização, recuperação e eliminação; garantir que pastas alheias nunca são apagadas.

## A10 — não presumir que todo documento externo permite seek

Classificação: fragilidade de compatibilidade confirmada; ocorrência condicionada ao provedor SAF.

Evidência: RecordingParts.kt:208 exige FileInputStream para servir uma parte. LocalTimeshiftServer.kt:85 reposiciona input.channel. MediaTarget.kt:167 usa ContentResolver.openInputStream. A abstração de Reader em LocalSegmentStore também exige FileInputStream.

Consequência: o provedor pode devolver um stream diferente ou um descriptor não reposicionável; nesse caso a leitura pode ser recusada ou falhar. Não há reprodução desta falha numa pen específica nesta análise.

Solução: representar explicitamente leitura sequencial e leitura reposicionável. Detetar capacidades; usar alternativa sequencial com limites quando viável, ou preparação local limitada para a parte requisitada. Não copiar uma gravação inteira para RAM nem consumir disco sem quota. Manter o percurso rápido para ficheiros normais. Testar GET/HEAD/Range, descriptor não reposicionável, cancelamento e remoção da pen.

Fonte oficial: https://developer.android.com/reference/android/content/ContentResolver — descriptors podem ser pipes e não permitir seek.

## A11 — apenas a última escolha deve abrir uma gravação

Classificação: risco de concorrência identificado; requer teste com atrasos controlados.

Evidência: App/app/src/main/java/tv/own/owntv/features/recordings/RecordingsViewModel.kt:106 inicia um novo job por escolha, sem geração ou cancelamento de uma seleção anterior. Existem verificações de perfil, mas não de identidade da última escolha antes de assumir o leitor e publicar lastPlayedId.

Cenário: selecionar A e depois B; a preparação de A demora mais. A pode chegar depois de B e assumir o leitor, apesar de já não ser a seleção desejada. A interleaving concreta depende do destino e do motor.

Solução: controlador de abertura com identidade monotónica, uma escolha atual, cancelamento da preparação anterior e fecho do handle local se ficar obsoleto. Verificar identidade antes de assumir o player e antes de publicar resultado/foco. Não adicionar atrasos fixos.

Aceitação: A→B com A lento; A→B→A; erro tardio da seleção antiga; trocar perfil; sair do ecrã; nenhuma abertura indevida nem servidor local abandonado.

## A12 — confirmar gravação dos ajustes individuais antes de fechar

Classificação: lacuna confirmada de comunicação e persistência; complemento de A1, não repetir a solução como lote independente.

Evidência: App/features/settings/ChannelPlaybackSettingsDialog.kt:47 fecha o editor logo após chamar setChannelPlaybackOptions. SettingsViewModel.kt:518 inicia gravação assíncrona sem devolver sucesso/falha ao editor. A transação DataStore do Core já existe; não é necessário refazê-la.

Solução: manter o rascunho, estado de guardar e erro; fechar depois de confirmação, associado ao canal/perfil da sessão. Reutilizar o padrão de edição segura previsto em A1. Testar falha de escrita, repetição e mudança de contexto.

## A13 — operações de gravação devem explicar por que não terminaram

Classificação: lacuna confirmada de interface; proteção dos ficheiros já existe e deve manter-se.

Evidência: Core/recording/RecordingManager.kt:608 inicia delete em job próprio; :613 espera até cerca de 5 s por leitores sob actionMutex; :614, :618 e :622 podem terminar sem resultado para a interface. App/RecordingsViewModel.kt:167 não observa conclusão ou motivo da recusa.

Solução: resultado tipado por operação: sucesso, ainda em uso, armazenamento indisponível, permissão, falha de eliminação e seleção obsoleta. Mostrar atividade e mensagem breve; evitar pedidos repetidos. A espera pode ser feita fora do bloqueio global desde que se revalidem identidade e leitores e se obtenha exclusão segura antes de eliminar. Nunca contornar a proteção de gravações em uso.

Aceitação: pen removida, leitura ainda ativa, ficheiro sem permissão, falha parcial na limpeza e operações sobre outra gravação durante a espera.

## A14 — reduzir consultas repetidas ao preparar versões de canais

Classificação: padrão de consultas confirmado; ganho de velocidade não medido.

Evidência: App/LiveViewModel.kt:1287 prepara versões; :1300 chama AdultCategoryClassifier.allows por candidato, e :1301 consulta categoria novamente. Core/content/AdultCategoryClassifier.kt:73 lê o perfil e :75 pode ler categoria em cada chamada. loadVersionGroup :1306 também resolve membros explícitos individualmente.

Solução: carregar perfil e categorias necessárias uma vez por operação; resolver membros explícitos em lote quando o DAO permitir; aplicar filtros sobre um snapshot consistente. Preservar restrições de perfil infantil, canais ocultos, categorias personalizadas e prioridade. Não adicionar uma cache global sem invalidação.

Aceitação: mesmos resultados antes/depois em todos os filtros; contar consultas e medir tempo para grupos pequenos/grandes; confirmar que uma alteração de perfil/invisibilidade invalida o resultado pendente.

## A15 — finalização de gravações longas com progresso e limites operacionais

Classificação: oportunidade de arquitetura; duração problemática não medida.

Evidência: Core/RecordingEngine.kt:354 executa toda a finalização em NonCancellable + IO; RecordingParts.kt:122 copia e verifica todas as partes, e :766 do engine chama finish. A proteção evita perder dados quando o utilizador para a gravação. Não é correto simplesmente tornar tudo cancelável.

Solução faseada: primeiro medir duração e comunicar estado/progresso da finalização. Se necessário, separar captura, finalização retomável e publicação final. Manter o checkpoint e reservar NonCancellable para commits/fechos curtos; permitir interromper uma preparação apenas quando seja possível retomá-la de forma consistente. Não forçar término de threads nem apagar spool incompleto.

Fonte oficial: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html — cancelamento de coroutines é cooperativo; mudar dispatcher não torna automaticamente uma cópia bloqueante cancelável.

## Inovações úteis e contidas

- Indicador apenas durante pressão longa no canal: sinalizar quando soltar OK abre versões (2 s) e quando abre o menu (3 s). Preservar os tempos e ações atuais; sem informação permanente a ocupar a lista.
- Verificação manual das gravações: relatório de partes ausentes, ficheiros não associados e espaço ocupado. Primeiro apresentar resultados e associação com a base de dados; nunca apagar automaticamente pastas por prefixo. Complementa A9.

## Ordem recomendada

1. Juntar A12 ao lote já proposto A1–A3 para proteger a edição/persistência.
2. A9 e A13: descoberta correta e resultados das operações de ficheiros.
3. A11: abertura de gravações com identidade e cancelamento seguro.
4. A10: compatibilidade de leitura SAF; manter o percurso atual dos providers que funcionam.
5. Medir A14 e A15, junto de A8; implementar otimização estrutural apenas depois dos resultados.
6. Inovações opcionais após as correções.

Continuam pendentes os restantes pontos úteis do primeiro relatório: limites de backup, avatar amostrado/atómico, HTTP retry de gravação e custo de armazenamento/playlists locais. Não aumentar buffers ou atrasos globais, não reiniciar motores em todos os canais e não afirmar que estes pontos provam a causa de perdas de fotogramas em direto. Não foram alterados código, configurações, dependências ou dados nesta análise.

