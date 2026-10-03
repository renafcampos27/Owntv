# Correção dos avisos de compilação — 2026-10-03

## Alterações

- Renomeados 48 parâmetros `migrate(db: SQLiteConnection)` para `connection`, com as referências correspondentes. Comparação dos literais confirmou que o texto SQL ficou inalterado. Sem mudança de versão ou esquema da base de dados.
- Removidos acessos de nulidade redundantes identificados pelo compilador em PlayerHud, HlsMediaPlaylist, StorageVolumeInfo, LocalTimeshiftSession e testes. O aviso de ManageSourcesScreen já estava resolvido no código atual.
- Removido o `else` redundante de LiveEpgReader, mantendo o filtro prévio dos tipos de fonte.
- Atualizados os testes de áudio para `flush(AudioProcessor.StreamMetadata.DEFAULT)`, a API disponível na versão instalada de Media3. Sem alteração do processamento de áudio de produção.
- Centralizada a verificação da característica antiga de TV em `hasTelevisionFeature`, com supressão local documentada. Mantidos FEATURE_LEANBACK, modo de televisão e deteção de toque para preservar a compatibilidade.
- Documentada e isolada a utilização de TRIM_MEMORY_RUNNING_LOW em OwnTVPlayer: necessária para receber pressão de memória em versões antigas do Android. A política de redução de cache ficou igual.
- Adicionada dependência `compileOnly` de kotlin-annotations-jvm, alinhada com a versão Kotlin do catálogo, para o compilador Java ler as anotações presentes nas dependências. Sem adicionar esta biblioteca à implementação da aplicação.
- Classificadas as duas novas strings `DEPRECATION` como identificadores técnicos no inventário de internacionalização.

## Aviso externo

O rastreio de `help --warning-mode=all --stacktrace` identifica a chamada descontinuada Configuration.setVisible em `com.android.build.gradle.internal.plugins.BasePlugin.createAndroidJdkImageConfiguration`: pertence ao Android Gradle Plugin atualmente instalado. Não foi ocultado e não foram alteradas versões do Gradle ou AGP apenas para eliminar este aviso.

## Validação

Concluído: `BUILD SUCCESSFUL in 2m 22s`, com 86 tarefas (52 executadas, 8 recuperadas da cache e 26 atualizadas). Compilados Kotlin/Java da app/Core/player-core e os testes Android de migrações. Sem avisos Kotlin ou Java no registo desta validação, incluindo a compilação Java do player-core que antes emitia MigrationStatus.STRICT.

Testes unitários: app 307, Core 1006, player-core 405; total 1718, sem falhas, erros ou testes ignorados. Os testes Android de migrações foram compilados, mas não executados numa box.

O registo final reutilizou a cache de configuração; por isso não voltou a emitir o aviso externo de setVisible. O rastreio separado confirmou que este continua no AGP e não foi corrigido nem suprimido.

Registos: `%TEMP%\owntv-warnings-validation.log` e `%TEMP%\owntv-warnings-gradle-origin.log`.

Backup dos 14 ficheiros de código/configuração alterados: `C:\Users\renat\Downloads\OwnTV-warnings-backup-20261003-194734.zip`.

Não foi gerado APK. Estas alterações de manutenção não demonstram uma correção de falhas do provedor, bloqueios de reprodução ou ausência de som em dispositivos físicos.
