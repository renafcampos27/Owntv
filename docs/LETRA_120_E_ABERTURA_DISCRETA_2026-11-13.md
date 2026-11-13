# Letra a 120% e abertura discreta dos canais — 13/11/2026

## Comportamento

- Tamanho principal da letra predefinido em **120%**. O valor é usado pela primeira configuração, leitura de preferências ausentes, criação de `FontCustomization` e reposição dos valores predefinidos. Uma escolha de tamanho já guardada pelo utilizador continua a ser respeitada; não foi criada uma migração para a sobrescrever. Escalas independentes de popups, legendas e geometria da interface mantidas.
- Abrir um canal em fullscreen começa sem barra de botões, relógios, logótipo, motor, codecs, timeline ou outros detalhes. O cartão inicial contém apenas **nome do canal, título do programa e descrição**, quando fornecidos pelo EPG.
- O cartão desaparece depois de cerca de **4,5 segundos** sem carregamento/espera e com reprodução pedida. Os estados de carregamento, espera do fornecedor e erro continuam identificáveis.
- **OK abre os controlos completos no fim da pressão**, consumindo também o evento de libertação. O primeiro OK não é enviado ao botão Play/Pause recém-apresentado. Enter e NumPad Enter seguem o mesmo percurso.
- Com controlos ocultos, Cima/Baixo continuam a mudar de canal, incluindo durante uma transição em que o motor ainda não marcou o conteúdo como direto. Com controlos abertos, as setas continuam a navegar nos botões.
- No **modo TV Avó**, OK passa também a abrir os controlos completos; ao ocultá-los, regressa à apresentação discreta. Mantidos os presets de agrupamento, prioridade e alternativas já associados ao modo. O painel técnico aberto explicitamente não é fechado pelo temporizador de ocultação do modo TV Avó.
- A apresentação do programa valida o canal proprietário do EPG e o intervalo temporal do programa. Dados antigos de outro canal ou de um instante diferente não são apresentados como se fossem atuais. Se o EPG não estiver disponível, não se inventa uma descrição.

## Implementação

| Ficheiro | Alteração |
|---|---|
| Core: `core/src/main/java/tv/own/owntv/core/theme/FontCustomization.kt` | `UiFontScale.DEFAULT`: 100 → 120. Sem alteração das chaves de preferências ou dos formatos de backup. |
| App: `app/src/main/java/tv/own/owntv/player/PlayerHudVisibility.kt` | Estado partilhado distingue introdução, controlos e vídeo sem sobreposição. A expiração da introdução não oculta controlos entretanto abertos com OK. |
| App: `app/src/main/java/tv/own/owntv/player/ChannelIntroCard.kt` | Cartão não focável, com texto branco, dimensões limitadas e descrição até três linhas. |
| App: `app/src/main/java/tv/own/owntv/player/PlayerHud.kt` | Abertura compacta por identidade do canal, temporizador, OK completo, foco com tentativas limitadas, zapping durante abertura e detalhes adicionais disponíveis nos controlos. |
| App: `app/src/main/java/tv/own/owntv/player/TvAvoPlayerHud.kt` | Usa o estado partilhado e permite abrir o leitor completo com OK, sem iniciar os observadores avançados enquanto está no modo discreto. |
| App: `app/src/main/java/tv/own/owntv/features/shell/OwnTVShell.kt` | Fornece nome e programa do canal efetivamente em reprodução, incluindo a programação do instante diferido. |
| App: `app/src/main/java/tv/own/owntv/features/live/LiveViewModel.kt` | Conserva a identidade do canal junto dos resultados EPG. As apresentações antiga e nova partilham a mesma consulta, os mesmos atrasos e a mesma cache do leitor EPG. Preservado o orçamento de retenção de cinco segundos do guia diferido. |
| App: `app/src/main/res/values/tv_avo.xml`, `app/src/main/res/values-pt-rPT/tv_avo.xml` | Descrição do modo atualizada para explicar OK. |
| App: `app/src/test/java/tv/own/owntv/player/PlayerHudVisibilityTest.kt` | Cinco testes de transições, ocultação, troca de canal e preservação do arranque de reprodução não associada a canais. |
| App: `app/src/test/java/tv/own/owntv/player/TvAvoKeyPolicyTest.kt` | Nome do teste ajustado ao novo significado de OK; mantida a validação de que o gesto é tratado na libertação e não corresponde a alternar reprodução. |

O aviso de gravação fica nos controlos durante a reprodução de canais, conforme a apresentação pedida; a gravação não é interrompida. Os percursos de transmissão, fecho de ligações, codecs, áudio, gravação e seek permanecem no Core anterior. Não foram adicionadas bibliotecas, opções de configuração ou chamadas de rede para a introdução.

## Integração

Snapshot Core: **1117 inputs**, SHA-256 **`dda32fd9d2ef4442120e07570827ab88b4491228d271f04cf52368041d936021`**. Comparação com a entrada desta tarefa, `cc8be13dbe0507f4c3e4c781f07c7294ba9811074af4dcd3ca69e243d6563de0`: apenas `FontCustomization.kt` deve diferir. Mantidos Gradle 9.9.0, AGP 9.5.1, Kotlin/Compose 2.4.20, Media3 1.11.1, libmpv 2026.11.0 e esquemas de dados/backup.

A constante **120** foi também confirmada na classe efetivamente compilada para Release, não apenas no checkout externo. Backup, hashes e logs em `C:/Users/renat/Downloads/OwnTV-letra-hud-2026-11-13-195208/`.

## Validação concluída

- **518 testes JVM da App aprovados**, incluindo os cinco novos testes de visibilidade; zero falhas, erros ou skips.
- Compilação Standard Release e `lintVitalStandardRelease` aprovados com Gradle 9.9.0 e verificação estrita de dependências.
- Testes Android compilados, sem execução no dispositivo. Comando/foco e apresentação visual ainda exigem aceitação na box.
- Inventário de tradução aprovado, sem aumento da baseline de 47 entradas/56 ocorrências.
- Fontes finais mantidos iguais aos hashes registados antes da validação final. Snapshot, Core externo e extração efetivamente compilada coincidem byte a byte; apenas `FontCustomization.kt` difere do Core anterior.
- Constante de letra **120** confirmada no bytecode Release. Logs, resultados XML e `validation-summary.json` guardados junto do backup.
- Sem APK, instalação, commit ou push nesta intervenção. Preservadas as alterações anteriores do Gradle 9.9.0 e dos logótipos.

## Aceitação na box

Gerar e instalar o APK atualizado. Verificar abertura de canal, cartão inicial, OK sem pausa, navegação dos controlos, Voltar, zapping com controlos ocultos e durante carregamento, troca rápida de canais sem descrição antiga, diferidos e modo TV Avó. Confirmar letra a 120% numa configuração sem tamanho guardado ou após repor o tamanho predefinido. Testes automáticos e compilação não substituem esta verificação visual e do comando.
