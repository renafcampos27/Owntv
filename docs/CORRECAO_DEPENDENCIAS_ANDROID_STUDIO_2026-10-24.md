# Correção da verificação de dependências no Android Studio — 24/10/2026

Este documento conserva as validações sucessivas. O estado atual consta da última
secção: inclui fontes, Javadoc e exemplos AndroidX com nomes alternativos, com
409 pedidos de anexos verificados na App e 275 no Core.

## Causa

O pedido de fontes dos plugins de compilação feito pelo Android Studio falhava com
`Dependency verification failed` nas configurações `detachedConfiguration2` da App,
baselineprofile e módulos Core. O relatório identifica 108 ficheiros únicos
`*-sources.jar` sem SHA-256 autorizado. Os checks de compilação anteriores não
exercitavam esse pedido específico do IDE.

## Correção aplicada

- Conferidos os 108 ficheiros locais contra os repositórios oficiais Google Maven,
  Maven Central ou Gradle Plugin Portal, por SHA-256 publicado ou pelos bytes oficiais.
- Acrescentados 108 hashes exatos à App e 104 ao Core, que tem menos plugins.
- Preservados os 1 213 hashes anteriores da App e os 756 do Core; não há autorização
  por wildcard, exclusão das fontes ou desativação da verificação.
- Evidência atualizada em `tools/release/dependency-provenance.json` e lista de
  regressão em `tools/release/ide-source-artifacts.json`.
- Adicionado `tools/release/verify_ide_sources.py`: resolve os ficheiros de fontes
  num projeto temporário com o Gradle e os metadados reais em modo estrito.
- Atualizada a cópia de fontes do Core: 1 071 ficheiros,
  SHA-256 `5bf05aa5e3e31d31beda25d6bbdb8437de294fd957f7c90be14236774b37470e`.
  A única diferença de conteúdo do Core face à cópia anterior é o XML dos hashes.

Antes de alterar, foi criada uma cópia com hashes verificados em
`C:/Users/renat/Downloads/OwnTV-ide-sources-backup-20261024-171715`.

## Validação

- Gradle 9.8.0: resolução estrita das 108 fontes da App passou.
- Gradle 9.7.1: resolução estrita das 104 fontes do Core passou.
- Teste negativo isolado: alterar um hash de fontes fez o Gradle rejeitar o ficheiro.
- Verificador de proveniência: 1 321 artefactos da App e 860 do Core autorizados por
  evidência dos repositórios oficiais.
- 11 testes das ferramentas de release e 6 das ferramentas de cópia do Core passaram.
- Compilação Kotlin Debug de `core` e `player-core` passou.
- Compilação Kotlin Standard Debug e Release da App e
  `lintVitalStandardRelease` passaram com verificação estrita, usando a nova cópia do Core.
- Conteúdo, nomes e SHA-256 da nova cópia do Core confirmados contra o checkout local.

O pedido de fontes foi reproduzido através do Gradle; a sincronização na interface do
Android Studio deve ser repetida pelo utilizador. Não foi gerado APK nem alterado
o código de reprodução, as configurações ou os dados guardados da aplicação.

## Próximo passo no Android Studio

### Complemento após o segundo relatório

O segundo relatório revelou mais nove fontes sem hash: o ZIP de fontes do Gradle
9.8.0 e oito JARs de Kotlin/KSP/Guava. A validação foi então alargada aos pedidos reais
dos plugins da App, do Core incluído e do Core isolado. Isso identificou também a fonte
transitiva de coroutines 1.9.0 e as variantes de Kotlin usadas pelos dois wrappers:
2.4.0 no Core isolado e 2.4.10 no Core incluído. Estas versões referem-se às fontes
pedidas pelas ferramentas; a versão configurada do compilador da App não foi alterada.

Estado após a segunda passagem, posteriormente alargado à documentação na secção seguinte:

- App: 1 334 artefactos autorizados, mais 13 hashes exatos; todos os anteriores intactos.
- Core: 875 artefactos autorizados, mais 15 hashes exatos; todos os anteriores intactos.
- Fontes conferidas nos repositórios oficiais e verificadas pelo Gradle: 121 na App
  e 119 no Core, incluindo os ZIPs de fontes de ambos os wrappers.
- `verify_ide_model.init.gradle` testa o grafo real dos plugins e confere também os
  bytes de cada fonte contra o XML. Uma consulta de fontes pode adiar a comunicação
  de um erro de verificação até à resolução seguinte; por isso, contar apenas os
  resultados devolvidos pela consulta era insuficiente.
- Consulta real de fontes: 112 resultados na App e 110 no Core, todos conferidos;
  consulta do Core isolado também aprovada.
- Compilação Standard Debug/Release e `lintVitalStandardRelease`: sucesso com
  verificação estrita e a cópia final do Core.
- 14 testes das ferramentas de release e 6 da cópia do Core: sucesso. Incluído um
  teste que verifica as fontes de Kotlin 2.4.0/2.4.10 dentro da cópia bloqueada do Core.
- Cópia final: 1 071 ficheiros, SHA-256
  `45eccda7e353322426b37b333f0bbefe1936c00ace55a7f7196f0428cc7e4daf`.
  Apenas `gradle/verification-metadata.xml` mudou no Core.
- Backup adicional verificado antes destas correções:
  `C:/Users/renat/Downloads/OwnTV-ide-sources-extra-backup-20261024-172636`.

A interface do Android Studio não foi operada neste diagnóstico; os pedidos de fontes,
os hashes e a compilação foram validados por Gradle. Nenhum APK foi gerado.

### Estado anterior — fontes e documentação Javadoc

O terceiro relatório identificou três JARs de documentação sem hash em
`OwnTV_Core:core:releaseCompileClasspath`: ZXing 3.5.4, juniversalchardet 2.5.0 e
listenablefuture 1.0. A validação anterior exercitava fontes, mas ainda não Javadoc.

Foi inventariado o conjunto de anexos já descarregados para componentes presentes
nos metadados revistos do projeto. Os 259 anexos adicionais (237 de fontes e 22 de
Javadoc) foram conferidos independentemente nos repositórios oficiais, antes de
acrescentar os seus SHA-256 exatos. Nenhuma biblioteca foi atualizada ou adicionada
à aplicação. A existência na cache não foi usada como prova de integridade.

Resultados atuais, que substituem os totais e a cópia anteriores:

- App: 1 593 artefactos revistos; 380 anexos de IDE verificados pelo Gradle estrito.
- Core: 1 025 artefactos revistos; 269 anexos de IDE verificados pelo Gradle estrito.
- Todos os hashes anteriores preservados; sem wildcard ou exclusão da verificação.
- Compilação Standard Debug/Release e `lintVitalStandardRelease`: sucesso.
- 15 testes de ferramentas de release e 6 da cópia do Core: sucesso.
- Teste negativo de Javadoc: o Gradle rejeitou um hash incorreto numa cópia isolada.
- Novo teste de regressão exige os três JARs de documentação dentro da cópia bloqueada
  do Core e comprova que o hash de um binário não autoriza o respetivo Javadoc.
- Cópia final do Core: 1 071 ficheiros, conteúdo e nomes verificados contra o checkout;
  SHA-256 `fcf36667644c9cc0ef5860b3a69b25dfa67e36048ae744638c2a793cfd7dc5f3`.
- Apenas `gradle/verification-metadata.xml` mudou no Core. Código de reprodução,
  versões de dependências e dados do utilizador não foram alterados.
- Backup adicional verificado:
  `C:/Users/renat/Downloads/OwnTV-ide-docs-backup-20261024-174227`.

`verify_ide_sources.py` e o ficheiro de referência `ide-source-artifacts.json` mantêm
os nomes antigos por compatibilidade; agora verificam fontes e Javadoc. As futuras
atualizações devem incluir ambos os tipos quando pedidos pelo IDE, sempre com
proveniência oficial e hash exato. A sincronização na interface do Android Studio
continua a ser o passo a repetir pelo utilizador.

### Estado atual — exemplos AndroidX com nomes lógicos diferentes

O relatório seguinte identificou os exemplos de lifecycle-viewmodel-savedstate
2.10.0 e navigationevent 1.0.2. Nestes módulos, o nome lógico do JAR não contém
`-android`, embora o nome do componente e o URL físico contenham esse sufixo.
A recolha anterior filtrava nomes pelo prefixo do módulo e excluía estes anexos.

- Conferidos os hashes dos dois ficheiros e de outros 27 exemplos semelhantes nos
  metadados oficiais do Google. Acrescentados 29 hashes exatos na App e 6 no Core,
  preservando todos os hashes anteriores.
- O verificador resolve estes exemplos através da variante de documentação publicada
  no `.module`, que devolve fontes e exemplos em conjunto. Confere o nome lógico
  esperado em cada pedido, em vez de construir um classificador pelo nome do ficheiro.
- A referência guarda o `docsType` oficial. É `sources` na maioria dos casos;
  Material Icons 1.7.8 publica `fake-sources`. Isto pertence à documentação das
  bibliotecas e não altera a reprodução ou as versões usadas pela App.
- Nova auditoria `--audit-cache`: todos os anexos de fontes, exemplos e Javadoc
  atualmente descarregados para os componentes revistos têm hash autorizado.
  Não filtra pelo prefixo do módulo, não altera ficheiros e não autoriza downloads.
- Verificação estrita pelo Gradle: 409 pedidos na App e 275 no Core aprovados.
- Proveniência: 1 622 artefactos na App e 1 031 no Core revistos contra evidência oficial.
- Compilação Standard Debug/Release e `lintVitalStandardRelease`: sucesso.
- 17 testes de release e 6 da cópia do Core: sucesso. Incluem nomes alternativos,
  seleção do tipo oficial de documentação, auditoria da cache e presença dos dois
  exemplos reportados na cópia bloqueada do Core.
- Teste negativo isolado: o Gradle rejeita um hash incorreto do nome lógico do exemplo,
  mesmo que o hash do `.module` e das fontes principais esteja correto.
- Cópia final do Core: 1 071 ficheiros, nomes e bytes verificados contra o checkout;
  SHA-256 `e96eca676488a1599d43f021f2ec6531af0fcc5ddba6ff1a0f359deb4f1b5280`.
  Apenas `gradle/verification-metadata.xml` mudou no Core.
- Backup verificado antes da alteração:
  `C:/Users/renat/Downloads/OwnTV-ide-alias-backup-20261024-175235`.

A interface do Android Studio continua por sincronizar pelo utilizador; a resolução
dos ficheiros e a compilação foram validadas por Gradle. Nenhum APK foi gerado.

Executar **File → Sync Project with Gradle Files** e esperar pelo fim. Depois, gerar
o APK pelo procedimento habitual. Não é necessário apagar caches nem reinstalar o IDE.

Em futuras atualizações de dependências, acrescentar as novas fontes efetivamente
pedidas pelo IDE, conferir a proveniência e repetir os comandos em
`tools/release/README.md`. Nunca aceitar automaticamente hashes apenas por existirem
na cache local.
