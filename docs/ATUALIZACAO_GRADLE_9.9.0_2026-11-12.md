# Atualização do Gradle para 9.9.0 — 12/11/2026

Pedido: substituir o Gradle 9.8.2 pela versão 9.9.0 recomendada pelo Android Studio. Alteração limitada às ferramentas de compilação e respetiva verificação.

## Alterações

- Wrapper da App: **9.8.2 → 9.9.0**. Wrapper standalone do Core: **9.7.1 → 9.9.0**. O build composto usa a versão do wrapper que o inicia; o alinhamento permite também abrir o Core separadamente.
- Regenerados os wrappers com a tarefa oficial da versão 9.9.0. Preservadas as opções personalizadas existentes, incluindo os limites de rede da App. JARs dos dois wrappers confrontados com o SHA-256 oficial.
- Adicionados, na App e no Core, os checksums específicos do arquivo de fontes Gradle 9.9.0 e dos artefactos Kotlin 2.4.21 usados pelo próprio Gradle. Isto cobre as novas dependências de configuração e os anexos que o editor pode pedir ao sincronizar.
- Atualizados o inventário de anexos do editor, os testes correspondentes e o registo de proveniência. Não foram removidos os checksums anteriores.
- **Verificação estrita, verificação de assinaturas e lista anterior de chaves de confiança preservadas.** Não foi usado `--write-verification-metadata` para aceitar automaticamente os erros de compilação: os novos artefactos foram confrontados com o servidor oficial ou descarregados e calculados diretamente do Maven Central antes de autorizar os hashes.
- README atualizado para indicar AGP 9.5.1 / Gradle 9.9.0.

Mantidos **AGP 9.5.1, Kotlin/Compose da App e Core 2.4.20, KSP 2.3.11, Media3 1.11.1 e libmpv 2026.11.0**. O Kotlin 2.4.21 apresentado por `gradlew --version` pertence ao Gradle; não é uma mudança do catálogo da aplicação. Sem alteração de código de reprodução, logótipos, áudio, HLS, dados ou definições.

## Integridade e snapshot Core

| Artefacto | SHA-256 oficial |
|---|---|
| `gradle-9.9.0-bin.zip` | `0b00b0ffe07c4d4789c12d4935a44a7057d55774b7540bd4a3f76f336deb177a` |
| `gradle-9.9.0-src.zip` | `c16d860198430fd8fd1c6914c4ca8283820d181ed0c5efa6fcce2275819a7bb4` |
| Wrapper JAR 9.9.0 | `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8` |

Core externo e snapshot comparados antes da alteração: eram iguais. O snapshot final contém **1117 inputs**, SHA-256 **`cc8be13dbe0507f4c3e4c781f07c7294ba9811074af4dcd3ca69e243d6563de0`**. Comparação com o snapshot anterior `b96bf0f13976cfe350e70c2750a73eca472abf2c98c70c1230d3bb590882e5bb`: apenas cinco inputs Core mudaram — `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties` e `gradle/verification-metadata.xml`. Todos os restantes inputs, incluindo reprodução e esquemas, foram preservados.

Principais ficheiros da App: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/verification-metadata.xml`, `tools/release/ide-source-artifacts.json`, `tools/release/dependency-provenance.json`, `tools/release/test_ide_sources.py`, `core-source-lock.json`, ZIP imutável em `vendor/` e README. Core: os cinco inputs referidos acima.

Backup e provas: `C:/Users/renat/Downloads/OwnTV-gradle-9.9.0-2026-11-12-230342/`. Inclui checksums oficiais, provas Maven, versões efetivas, verificação de anexos, comparação do snapshot e logs de compilação.

## Validação concluída

- App e Core standalone confirmam **Gradle 9.9.0**.
- Compilação Standard Release, `lintVitalStandardRelease`, verificação de classes duplicadas e junção das bibliotecas nativas aprovadas. Build composto concluído: 159 tarefas, 134 executadas e 25 recuperadas da cache.
- **513 testes JVM da App executados e aprovados**, zero falhas, erros ou skips.
- **23 testes Python aprovados**: 17 das ferramentas de release e seis do snapshot. Um teste funcional opt-in de substituição do snapshot/configuration cache não foi executado; não é contado como aprovado.
- Os três anexos novos — fontes Gradle 9.9.0, kotlin-stdlib e kotlin-reflect 2.4.21 — foram resolvidos pelo Gradle com verificação estrita, separadamente com o manifesto da App e do Core.
- Testes Android compilados, sem execução física. Não foi gerado APK, efetuado commit/push ou instalado software na box.
- Hashes dos nove inputs principais reconfirmados após a compilação. Os 1117 inputs do snapshot, checkout Core e extração realmente usada na compilação coincidem byte a byte. Alteração pré-existente num ficheiro `.pyc` preservada.
- Foram emitidos três avisos nos testes de reflexão de `HomePreviewHandoverTest` e o aviso geral de APIs Gradle obsoletas antes da versão 10. Nenhum foi silenciado ou tratado como erro de compilação.

## Utilização no Android Studio

Sincronizar o projeto com os ficheiros Gradle após a alteração. A primeira utilização pode demorar mais por ter de preparar as caches da versão nova. Depois, gerar o APK pelo processo habitual. Não é necessário mudar o leitor, reinstalar o Android Studio ou apagar os dados da OwnTV.

O Gradle 9.9.0 melhora a compilação incremental dos scripts Kotlin em determinados projetos; o ganho depende do que mudou e das caches existentes. Não se atribui uma percentagem de aceleração ao projeto sem medições comparáveis. Esta atualização não é uma otimização da reprodução no APK. Referências: [notas oficiais do Gradle 9.9.0](https://docs.gradle.org/9.9.0/release-notes.html) e [compatibilidade do AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes).
