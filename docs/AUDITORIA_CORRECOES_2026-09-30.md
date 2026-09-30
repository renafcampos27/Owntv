# OwnTV — correções da auditoria de 30/09/2026

Implementação na app e no Core local. Não gera APK, não publica alterações e não muda os modos de motor/formato escolhidos pelo utilizador.

## Alterações

| Auditoria | Implementação |
|---|---|
| A1 | Orçamento de abertura encerrado quando reproduz; recuperação tem orçamento próprio limitado, sem renovar alternativas já gastas. |
| A2 | Esperas do fornecedor creditadas desde a primeira observação, uma vez por identidade; crédito total limitado a 60 s por orçamento. |
| A3 | Preparação após reconexão recebe até 5 s adicionais apenas com progresso da nova origem; retoma natural não repete o inicial. |
| A4 | Jobs de resolução cancelados ao abandonar a sintonia; cancelamento Stalker alcança a chamada HTTP e leitura do corpo. O mpv também cancela o seu resolver ao abandonar a carga. |
| A5 | mpv confirma reprodução nativa da carga atual, incluindo rádio; FILE_LOADED ou dimensões de vídeo não encerram a abertura por si só. |
| B1 | Regresso à lista verifica sucesso real de foco, com tentativas limitadas por frames e alternativas. |
| B2 | Consulta EPG cancelada/falhada não publica ausência; gerações impedem resultados antigos após invalidar cache. |
| B3 | Callbacks estáveis leem os dados atuais do canal. |
| C1 | Editor de reserva usa rascunho local; Concluído guarda CUSTOM e o intervalo; Voltar cancela; Restaurar altera o rascunho. |
| C2 | Interface e reprodução usam o mesmo resolver. Lista sem modo segue global; modo explícito com valores ausentes usa os padrões desse ajuste. |
| C3 | Dados das secções selecionadas são validados antes das primeiras escritas, incluindo tipos e limites das configurações. |

HLS exclusivo continua a impedir alternativas TS/mpv. ↑/↓ no fullscreen, diferidos, rewind e gravações permanecem disponíveis.

## Configurações e diagnóstico

Em **Definições → Reprodutor de vídeo → TV em direto**, a reserva personalizada passa a ser guardada ao selecionar **Concluído**. O formulário mostra os objetivos efetivos quando o inicial aumenta o mínimo. A interface explica que o mpv usa o máximo de reserva, sem reproduzir o controlo mínimo/máximo do ExoPlayer.

O registo detalhado inclui decisões de orçamento, sintonia/origem, espera por pedidos antigos e tempo até primeiro frame. Os novos eventos não contêm credenciais.

Com **Registo detalhado de reprodução** ligado, existe também uma linha `ui_perf` a cada aproximadamente 10 s enquanto a app está visível:

- Frames da interface e frames que excedem o prazo de renderização; o primeiro desenho é excluído.
- Percentil 95 aproximado em intervalos de 1 ms, com último intervalo ≥1000 ms.
- Atraso de chegada das teclas à Activity; não é uma medição completa entre tecla e imagem.
- Tempo de CPU do processo, heap Java/nativo e contadores de GC durante a janela.
- Relatórios de métricas não entregues pelo Android; não são fotogramas perdidos do vídeo.

A recolha usa histogramas fixos e um worker de prioridade de fundo. É removida ao desligar o diagnóstico ou ocultar a Activity. Não mede toda a memória de codec/GPU nem substitui um perfil de sistema.

Referência da instrumentação: [FrameMetrics — Android Developers](https://developer.android.com/reference/android/view/FrameMetrics).

## Validação

Compilação conjunta da app Standard Debug com o Core local concluída com sucesso. **1304 testes unitários passaram**, sem falhas, erros ou testes ignorados:

| Módulo | Testes | Suites |
|---|---:|---:|
| App | 195 | 27 |
| Core | 805 | 85 |
| Player Core | 304 | 39 |

As verificações de literais da app/Core e a leitura XML dos recursos de reserva passaram. A verificação de espaços das alterações desta tarefa passou; a app mantém uma linha vazia final anterior em `app/proguard-rules.pro`, fora deste lote.

Durante a validação foi corrigida uma falha do novo `InitialOnlyLoadControl`: a delegação Kotlin não encaminhava os métodos Java predefinidos. O wrapper encaminha explicitamente os métodos atuais de Media3, incluindo lifecycle, seleção de faixas, carregamento e allocator; há teste de regressão para estes percursos.

Registo da validação: [audit-implementation-validation-final.log](C:/Users/renat/Documents/Codex/2026-09-22/c-users-renat-downloads-owntv-main/work/audit-implementation-validation-final.log).

Não foi gerado APK. Não foram executados pedidos ao fornecedor ou ensaios físicos na box nesta implementação. Estes testes não demonstram ganhos de desempenho nem a eliminação de bloqueios no dispositivo.

## Ensaio após gerar o APK

1. Editar Personalizada com vários cliques rápidos; confirmar, voltar a abrir e verificar o intervalo. Repetir por lista. Verificar que Voltar cancela.
2. Restaurar um backup válido e confirmar opções globais/por lista. Um ficheiro com tipo de configuração inválido deve falhar antes de aplicar outras secções.
3. Navegar pela lista e sair/entrar no fullscreen. O foco deve regressar a uma linha ou pesquisa utilizável.
4. Fazer pelo menos 100 mudanças incluindo A→B→C→A, por lista e ↑/↓; observar os tempos e os bloqueios.
5. Com diagnóstico detalhado, navegar 30 s pela lista e reproduzir um canal saudável durante uma sessão prolongada. Comparar `ui_perf` e eventos de sintonia entre cenários.
6. Testar retoma de interrupção transitória e, se utilizado, mpv com inicial Automático e canal só de áudio.

O cancelamento local não garante que o fornecedor liberte imediatamente uma sessão remota. A eliminação dos bloqueios e os ganhos nas Xiaomi/Thomson precisam deste ensaio.

## Android Studio

Usar `C:\Users\renat\Downloads\OwnTV-main` e sincronizar o Gradle. As alterações dos reprodutores e backup estão também em `C:\Users\renat\Downloads\OwnTV_Core`; o projeto deve continuar a usar esse Core local. Gerar APK só depois da sincronização. Esta tarefa não executa a geração.
