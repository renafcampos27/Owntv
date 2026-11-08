# Alinhamento da compilação com AGP 9.5.1

Data: 08/11/2026. OwnTV Android TV personalizada.
Estado: correção concluída; compilação Release, testes e lint vital da App aprovados com verificação estrita.

## Causa e correção

O catálogo da App já tinha sido atualizado pelo utilizador para AGP 9.5.1. O catálogo do Core e o snapshot bloqueado ainda usavam AGP 9.5.0. A configuração conjunta era recusada com «Using multiple versions of the Android Gradle Plugin».

- Mantida a escolha 9.5.1 do utilizador em `C:/Users/renat/Downloads/OwnTV-main/gradle/libs.versions.toml`.
- Atualizado apenas `agp = "9.5.0"` para `agp = "9.5.1"` em `C:/Users/renat/Downloads/OwnTV_Core/gradle/libs.versions.toml`.
- Acrescentados aos dois `gradle/verification-metadata.xml` **110 hashes de 35 componentes específicos** da ferramenta AGP 9.5.1 e suas bibliotecas Android 32.5.1, incluindo os sources já descarregados para o IDE. Cada ficheiro foi comparado com o checksum ou conteúdo do repositório HTTPS oficial pelo verificador existente do projeto. As provas com URL/hash estão na pasta de cópias.
- Todos os componentes/hash anteriores e a configuração de verificação de assinaturas/confiança foram preservados. Sem wildcard novo, desativação de verificação, aceitação de um hash diferente ou alterações de dependências de reprodução.
- Regenerado o snapshot imutável do Core e atualizado o lock da App. A comparação integral com o snapshot anterior confirmou que, entre os **1098 inputs do Core**, apenas catálogo e metadados de verificação mudaram.

Os wrappers existentes foram preservados: Gradle 9.8.2 na App e 9.7.1 no Core autónomo. A documentação oficial da linha AGP 9.5 exige no mínimo Gradle 9.6.0; ambos satisfazem esse mínimo. No build conjunto usa-se o wrapper da App. Não houve atualização de Media3, libmpv, Kotlin, opções guardadas ou código funcional da App.

## Validação e integração

- Comando executado com `--dependency-verification=strict -PowntvUseCoreSnapshot=true`: `:app:compileStandardReleaseKotlin :app:testStandardDebugUnitTest :app:lintVitalStandardRelease`.
- **BUILD SUCCESSFUL in 20m 51s**; 143 tarefas, 118 executadas e 25 recuperadas da cache. Esta primeira execução com AGP novo refez a compilação; este tempo não mede o desempenho da App.
- **470 testes JVM da App**, zero falhas/erros/ignorados. Lint vital Release: **No issues found.** O Core e player-core foram compilados em Debug/Release como dependências da App; não se declara uma execução de testes próprios do Core nem um build autónomo pelo seu wrapper.
- Confirmados AGP 9.5.1 nos catálogos da App, do checkout do Core e da extração efetivamente compilada. O catálogo da App preserva exatamente a atualização já feita pelo utilizador.
- A primeira execução teve três warnings em `HomePreviewHandoverTest.kt` sobre wrappers Java Boolean/Integer/Long e avisos de APIs Gradle obsoletas. Não bloquearam a compilação e não foram suprimidos ou tratados como falha de reprodução.
- Snapshot: **1098 inputs**, SHA-256 `c46ccf95335e84fd58fd83a5a4fa21bbc461482987ac2b36ee6aecdce3eaf361`. Checkout, ZIP bloqueado e inputs extraídos do build comparados integralmente, incluindo o catálogo com 9.5.1.
- Cópias/estado anterior, hashes, provas oficiais e logs: `C:/Users/renat/Downloads/OwnTV-agp951-2026-11-08-154244`.

O primeiro arranque invalidou o observador/cache de ficheiros da sessão anterior ao encontrar o Core antigo dentro de `.gradle`; o Gradle desligou a observação e continuou a configurar o novo build. Esse aviso não foi usado para desativar verificações ou alterar preferências do projeto.

O lint integral do Core não foi executado nesta correção; o histórico regista cinco erros anteriores de pluralização. Esses recursos não foram alterados e não se declara uma nova aprovação desse lint integral. O alvo desta correção é o conflito de versões AGP. Compilação de fontes, testes e lint vital da App não são prova de instalação ou reprodução física.

## Android Studio

No projeto já aberto, executar **File → Sync Project with Gradle Files** e aguardar. Depois usar o processo habitual de geração de APK. Os registos de hashes de versões antigas podem continuar nos XML de verificação; isso não significa que essas versões estejam a ser usadas na compilação.

Não houve geração de APK, assinatura, instalação, commit ou publicação. As fases funcionais pendentes do plano HLS/áudio não avançaram.

Referência: [Android Gradle Plugin 9.5 — compatibilidade e 9.5.1](https://developer.android.com/build/releases/agp-9-5-0-release-notes).
