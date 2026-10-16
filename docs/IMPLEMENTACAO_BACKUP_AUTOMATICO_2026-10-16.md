# OwnTV TV — backup automático, 16/10/2026

## Opção nova

Local: **Definições → Backup**.

Frequências: **Manual**, **a cada 3 meses**, **a cada 6 meses (predefinição)**, **uma vez por ano**. A configuração inicial escolhe perfis, pasta e palavra-passe. O seletor existente verifica o PIN dos outros perfis protegidos. Depois, não é necessário escrever a palavra-passe em cada execução.

Inclui também **Criar backup agora**. O botão mostra o estado da operação e impede pedidos manuais duplicados.

## Conteúdo e armazenamento

Usa o mesmo exportador completo e encriptado `.own` do backup manual: listas, URL e credenciais, opções por origem, perfis autorizados, personalizações e definições gerais/por canal incluídas pelo exportador existente. Não transfere ficheiros de gravações nem as listas de canais em cache.

Pasta predefinida: `OwnTV/backups/automatic` dentro do armazenamento da app. Está acessível pelo explorador existente. Pode escolher-se outra pasta, incluindo USB. Uma pasta USB indisponível não é substituída silenciosamente por outra.

A pasta da app é apagada ao desinstalar ou limpar os dados. Para uma cópia independente da instalação, escolher uma pasta externa. Guardar a palavra-passe: o ficheiro pode ser restaurado noutro dispositivo com essa palavra-passe.

## Segurança e robustez

- Palavra-passe, pasta e autorização dos perfis ficam num bloco local protegido por **Android Keystore/AES-GCM**. Não são exportadas para outra instalação.
- O intervalo entra no backup das definições. Backups antigos sem esse campo conservam o valor atual.
- Alterações ao PIN, às restrições ou à identidade do perfil invalidam a autorização automática. Há verificações antes e depois de preparar o ficheiro.
- Preparação numa pasta temporária própria; publicação por rename no mesmo volume; rotação apenas após publicação bem-sucedida.
- Conserva as **três cópias automáticas mais recentes**, sem remover ficheiros manuais ou nomes alheios ao recurso.
- Operações automáticas e pedidos imediatos são serializados; trabalhos duplicados têm nomes únicos no WorkManager.
- Uma falha não substitui backups concluídos por ficheiros incompletos. A UI assinala que é necessário verificar/reconfigurar.

## Agendamento

Intervalos por meses do calendário, com normalização de fim de mês e ano bissexto. A âncora é a configuração inicial ou o último backup automático concluído, não cada abertura da app. Uma exportação manual comum não altera a âncora automática.

O WorkManager mantém um verificador diário leve; só exporta quando o intervalo vence. O agendamento é recuperado após reiniciar. Com a box desligada, armazenamento indisponível ou restrições do Android, a execução é adiada. Não há garantia de uma hora exata.

Referência: [Android — definir trabalho persistente](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).

## Validação

**BUILD SUCCESSFUL** (2 min 10 s na verificação final).

- App compilada em debug e release.
- `lintVitalStandardRelease` aprovado.
- **424 testes App**, incluindo **9 testes novos de backup automático**, sem falhas, erros ou ignorados.
- Cobertura: calendário, datas-limite, modo manual, validação do intervalo restaurado, revogação de autorização e rotação de ficheiros reais.
- Verificações i18n, formatação numérica, overflow e whitespace aprovadas. Baseline de texto da App mantém 51 entradas; novos nomes técnicos foram classificados explicitamente.
- Registo final: `C:/Users/renat/AppData/Local/Temp/owntv-automatic-backup-final.log`.

Compilação e testes JVM não substituem os ensaios numa box com Android Keystore, USB e WorkManager após reinício. Não foi gerado APK. Implementação feita apenas na App TV; sem alterações ao esquema Room nem ao código do Core nesta funcionalidade.
