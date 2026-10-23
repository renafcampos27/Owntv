# Fase 2 — fecho da integração App/Core

Data: 22/10/2026. Plano: `../implementar_rena/analise_final_ia_analisadasol.md`, secção 16.

## Resultado

As alterações da fase 2 anteriormente existentes foram revistas e integradas num snapshot próprio, antes da integração das novidades de OwnTV 5.2.1/Core 1.0.69. A validação automática desta integração terminou com sucesso. Os ensaios numa box continuam pendentes.

## Ajustes finais

- A recuperação de uma importação parcial conserva configurações da fonte que não pertencem ao formulário: buffer, motor, correções de diferidos e restantes preferências. Só atualiza os campos do formulário e continua a invalidar deteções de ligação quando mudam as credenciais/transporte.
- Um segundo Retry de uma fonte recuperada reutiliza a mesma fonte, em vez de perder a identidade da tentativa anterior. Abandonar explicitamente o formulário limpa a intenção de recuperação em memória.
- O controlo de Retry-After do catálogo guarda prazo e código HTTP na mesma entrada atómica. O prazo mais longo conserva o respetivo código; espera-se novamente se outro pedido prolongar o prazo. Contas com Authorization/Cookie diferentes não partilham a espera. Só se retêm identidades em hash; esta política não é aplicada ao cliente de streaming.
- A revisão preservou XMLTV completo/cache anterior, identidade e cancelamento de pedidos, construção de URLs, títulos Base64/UTF-8, proteção de listas vazias/incompletas, associações EPG em lote e dados sensíveis fora do estado Android.

## Evidência automática

- Core: `:core:testDebugUnitTest` e `:core:compileDebugAndroidTestKotlin` — `BUILD SUCCESSFUL`, **1 320 testes JVM, zero falhas/erros**.
- App com `-PowntvUseCoreSnapshot=true`: compilação Kotlin standard debug e release, `:app:testStandardDebugUnitTest` e `:app:lintVitalStandardRelease` — `BUILD SUCCESSFUL`, **426 testes JVM da App sem falhas**.
- Ferramenta de snapshot: **6 testes aprovados**.
- Inventários de literais técnicos verificados, sem desligar verificações de tradução/lint.
- Snapshot de fecho desta fase: **1 025 inputs**, SHA-256 `71e13bdf202335a94bc9d3b103a31b2f66144b2252a5d04cd1fc52b9c3aef648`. Esta é uma evidência histórica da fase 2, não uma obrigação de manter esse hash após alterações posteriores autorizadas.
- Logs: `%TEMP%/owntv-phase2-close-core.log` e `%TEMP%/owntv-phase2-close-app.log`.

Base de dados continua na versão 51 e backup na 26. Não foi gerado APK, executado teste Android numa box, alterada assinatura ou publicada uma versão.

## Recuperação e continuidade

Cópia anterior a este fecho: `C:/Users/renat/Downloads/OwnTV-upstream-A-backup-20261022/`, com fontes App/Core, ZIP/lock e manifesto SHA-256 conferidos. É uma cópia do código, não dos dados instalados na box.

As fases 3–6 do plano principal permanecem por executar, salvo futuras autorizações específicas. A etapa A das publicações novas tem relatório próprio e pode produzir um novo snapshot que já inclua esta fase 2.

Testes físicos pendentes: importação/interrupção/Retry, duas listas com nomes iguais, alteração de credenciais, XMLTV cortado seguido de válido, Retry-After, correção de diferidos +1 h e manutenção de preferências por canal.
