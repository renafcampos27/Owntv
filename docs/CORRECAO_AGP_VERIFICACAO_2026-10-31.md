# Falha de verificação após seleção de AGP 9.5.0

Data: 31-10-2026.

O relatório `build/reports/dependency-verification/at-1793470780045/dependency-verification-report.html` identifica falta de checksum para `com.android.application:com.android.application.gradle.plugin:9.5.0`, ficheiro `com.android.application.gradle.plugin-9.5.0.pom`. Não é uma prova de ficheiro adulterado nem de defeito no AGP 9.5.0.

O catálogo da App tinha sido alterado de 9.4.1 para 9.5.0; o Core mantinha 9.4.1 e os hashes aprovados correspondiam à ferramenta anterior. A atualização da ferramenta não estava concluída/validada no conjunto App+Core.

## Decisão aplicada

Restaurada apenas a linha `agp = "9.4.1"` no catálogo da App, para recuperar a combinação já validada. A seleção 9.5.0 foi preservada em `C:\Users\renat\Downloads\OwnTV-agp-config-backup-20261031-182211`, com manifesto SHA-256.

Não foram acrescentadas autorizações de confiança, desativada a verificação de dependências ou alterados o Core e o mecanismo TLS. O snapshot continua `8375ddea1894e8e55ac5b3217260ddc1fdcf378044f31c5f2bf4c77766f3402e`.

## Validação

- Compilação Standard Release e lint vital, com verificação estrita e offline: **BUILD SUCCESSFUL in 1m 38s**, configuration cache guardada.
- Consulta das fontes do buildscript usado na integração IDE: **112 App + 110 Core**, todos os hashes correspondem aos aprovados; **BUILD SUCCESSFUL in 26s**.
- Não foi gerado APK nem operada a interface do Android Studio. É necessário repetir a sincronização no IDE para atualizar o modelo aberto.

## Atualização futura

AGP 9.5.0 tem correções reais, incluindo avisos relacionados com Gradle 9.8 e obtenção de fontes AndroidX KMP, além de funcionalidades experimentais de teste de backup/restauro. Estas melhorias dizem respeito às ferramentas de desenvolvimento; não demonstram um ganho de reprodução IPTV nesta app. [Notas oficiais AGP 9.5.0](https://developer.android.com/build/releases/agp-9-5-0-release-notes).

Uma migração futura deve alinhar App/Core, verificar os hashes oficiais das novas dependências e repetir compilação, lint e testes. Não basta aceitar o primeiro POM em falta: a atualização pode introduzir outros artefactos. [Verificação de dependências Gradle](https://docs.gradle.org/current/userguide/dependency_verification.html).
