# Predefinição geral: compatibilidade de hostname

Data: 31-10-2026. Alteração pedida explicitamente pelo utilizador após comparação das três políticas TLS.

## Comportamento

- Se `playback_tls_global` não existir, a definição geral efetiva é agora `AUTO_HOSTNAME`.
- Aplica-se a instalações novas e a instalações anteriores que nunca tenham guardado uma escolha geral TLS. Não existe uma migração que substitua escolhas explícitas.
- As escolhas gerais já guardadas e as exceções por lista mantêm-se. Repor a política geral através do setter com valor nulo usa a nova predefinição.
- O backup passa a materializar o valor geral efetivo, incluindo quando era apenas implícito, para preservar esta escolha em futuros restauros.
- Valores guardados desconhecidos continuam a resultar em `STRICT`. A configuração de transporte sem preferências carregadas continua estrita; não foi alterada a política de erro de leitura das preferências.

## Limites preservados

A exceção de hostname mantém o comportamento existente: começa com validação normal, permite a exceção apenas após falha correspondente de hostname e volta a testar a validação estrita ao expirar a exceção temporária. Não autoriza automaticamente certificados expirados ou emissores não reconhecidos, nem trata HTTP 403/404 como falha TLS. O âmbito de reprodução existente não foi alargado.

Esta decisão substitui apenas a predefinição estrita descrita nos documentos históricos de implementação TLS V1/V2. As restantes proteções e os limites desses documentos continuam válidos.

## Entrega

- Alterações de produção limitadas a `SettingsRepository.kt`; testes em `PlaybackTlsPreferencesTest.kt`.
- Snapshot do Core: `70aab7a373d8d6dc8449eda6a3c3e720118387e196a81fa0222f15710456ceb9`, 1.089 ficheiros. Comparação com o snapshot anterior: apenas os dois ficheiros acima mudaram.
- Backup anterior: `C:/Users/renat/Downloads/OwnTV-tls-hostname-default-backup-20261031-214504`.
- Não foi gerado APK nem testada uma box física nesta alteração.

## Validação concluída

- 29 testes dirigidos de TLS/preferências (24 do interceptor e cinco das preferências), sem falhas, erros ou testes ignorados.
- 19 testes de backup/restauro/validação de definições, sem falhas, erros ou testes ignorados.
- Seis testes das ferramentas de snapshot passaram.
- Compilação Standard Release da App e testes TLS com Gradle 9.8.2, offline e verificação estrita: **BUILD SUCCESSFUL in 3m 34s**.
- Verificação adicional de backup: **BUILD SUCCESSFUL in 11s**.
- Não foi repetido lint nesta alteração de preferências; não foram alterados recursos ou dependências.
