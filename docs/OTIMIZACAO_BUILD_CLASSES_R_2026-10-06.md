# Otimização da compilação — classes R não transitivas

Data: 2026-10-06.

## Alterações

- Ativado `android.nonTransitiveRClass=true` em `gradle.properties` da aplicação.
- Migradas 3 276 referências de recursos em 121 ficheiros Kotlin: recursos do Core passam a usar `tv.own.owntv.core.R`, com o alias `CoreR`; os recursos próprios da aplicação continuam a usar o seu `R`.
- Adaptado o teste `SettingsSearchCoverageTest` para reconhecer os dois namespaces sem perder a verificação de cobertura da pesquisa das definições.
- Atualizado o comentário de compatibilidade em `OwnTV_Core/gradle.properties`; mantido o comportamento não transitivo predefinido do Core.
- Preservados os caches de configuração e de compilação existentes e o limite de quatro workers. Não aumentados os limites de memória nem ativadas opções experimentais.

O objetivo é reduzir a propagação de alterações nos recursos entre módulos e o trabalho de compilação incremental. Esta mudança não introduz funcionalidades nem altera deliberadamente a reprodução, áudio, EPG ou interface. Não se espera uma melhoria de desempenho da aplicação instalada.

## Validação

Compilação Kotlin concluída para `standardDebug`, `standardRelease`, `x86_64Debug` e os testes Android de `standardDebug`.

Testes unitários: aplicação 311, Core 1 060 e player-core 440; total 1 811, sem falhas. Os testes de dispositivo foram compilados, mas não executados numa box ou smartphone.

Compilação repetida de `:app:compileStandardDebugKotlin`, sem alterações: 1,94 s de tempo total medido; Gradle indicou 1 s, 30 tarefas atualizadas e reutilização do cache de configuração. Isto é uma medição com caches preparados e não uma medição da geração de APK, de compilação a frio ou do ganho percentual causado pela migração. A referência anterior de 43,69 s não tinha condições de cache equivalentes e não permite uma comparação direta.

`git diff --check` passou para as fontes da aplicação e a configuração verificadas. Nenhum APK foi gerado neste trabalho.

## Utilização no Android Studio

1. Executar uma vez **File → Sync Project with Gradle Files** após estas alterações de configuração.
2. Para testes habituais, selecionar a variante `standardDebug` e compilar apenas a variante necessária.
3. Evitar **Clean Project** antes de cada APK: a limpeza elimina saídas que a compilação incremental poderia reutilizar. Usar a limpeza apenas quando houver uma razão concreta.
4. A primeira compilação após a migração pode reconstruir recursos/classes; as seguintes aproveitam os caches.

## Cópia anterior à migração

As fontes migradas e a configuração original da aplicação foram guardadas em:

`C:\Users\renat\AppData\Local\Temp\owntv_r_migration_backup_20261006_212113.zip`

Esta cópia cobre os ficheiros da migração, não substitui um backup completo do projeto e está numa pasta temporária.

## Referências

- Android: https://developer.android.com/build/optimize-your-build
- Gradle: https://docs.gradle.org/current/userguide/configuration_cache.html
