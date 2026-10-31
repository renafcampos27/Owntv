# Correção da verificação das fontes do Gradle 9.8.2

Data: 31-10-2026.

## Problema

O Android Studio passou a solicitar `gradle:gradle:9.8.2`, artefacto `gradle-9.8.2-src.zip`, nas configurações documentais da App e do baseline profile. Faltava a autorização SHA-256 exata. O erro não demonstra corrupção do ZIP: o ficheiro encontrado na cache corresponde ao checksum publicado pelo Gradle.

## Alterações

- Incluída a autorização exata do ZIP de fontes nos metadados da App e do Core, com prova oficial registada no manifesto de proveniência.
- Acrescentada a versão 9.8.2 à cobertura dos testes de fontes da IDE, incluindo o Core efetivamente consumido pelo snapshot.
- Preservada a atualização do wrapper da App para Gradle 9.8.2. Repostos o checksum oficial da distribuição binária, a validação de URL e os limites de rede que o Android Studio tinha removido.
- Mantido AGP 9.4.1. AGP e Gradle são ferramentas diferentes; esta correção não migra o AGP.
- Regenerado o snapshot do Core: `c91fe55d29a50de7515721d47c72f010bcee7c4b141f6dfc0a99e5223d29cf35`, 1.089 ficheiros. A única alteração face ao snapshot anterior `8375ddea...` é `gradle/verification-metadata.xml`.
- Verificação de dependências mantida estrita, sem autorizações genéricas. Reprodução/TLS não alterados.

## Checksums oficiais confirmados

- Fontes: `96c9aba0564daa2998f578b589eba9ae1f6656e368fde85f7132e05321bab553` — [checksum oficial das fontes](https://services.gradle.org/distributions/gradle-9.8.2-src.zip.sha256).
- Distribuição binária: `21bd5da2de49b642904077f38c0efdc7778e92b3ed0889392dca61b97e389607` — [checksum oficial do binário](https://services.gradle.org/distributions/gradle-9.8.2-bin.zip.sha256).

Backup dos ficheiros anteriores em `C:/Users/renat/Downloads/OwnTV-gradle-982-backup-20261031-182914`.

## Validação

- Compilação `:app:compileStandardReleaseKotlin` e `:app:lintVitalStandardRelease`, Gradle 9.8.2 + AGP 9.4.1, offline e com verificação estrita: **BUILD SUCCESSFUL in 8m 18s**, 82 tarefas; configuration cache guardada.
- Permanecem avisos de APIs Gradle obsoletas para uma futura migração ao Gradle 10; não impediram esta compilação.
- 17 testes das ferramentas de release e seis testes de snapshot passaram.
- 1.624 artefactos da App e 1.033 do Core corroborados com a proveniência oficial registada.
- Resolução isolada do ZIP de fontes pelo Gradle 9.8.2, com verificação estrita: **BUILD SUCCESSFUL in 26s**.
- Auditoria da cache: sem fontes/documentação por aprovar; 410 pedidos documentais cobertos na App e 276 no Core.
- ZIP do Core comparado byte a byte com todos os 1.089 ficheiros atuais; checksum e conjunto de caminhos coincidem.

Não foi gerado APK. A sincronização na interface do Android Studio deve ser repetida pelo utilizador.
