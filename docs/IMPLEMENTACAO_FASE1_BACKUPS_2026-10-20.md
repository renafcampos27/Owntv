# Fase 1 — integridade dos backups e do restauro

Data: 20/10/2026. Plano: `implementar_rena/analise_final_ia_analisadasol.md`, fase 1.

Estado: implementação e validação automática concluídas. Validação física na box pendente; fases seguintes não iniciadas.

## Âmbito implementado

- **INT-01:** os novos backups incluem identificadores portáveis das fontes, mesmo sem selecionar Fontes. São impressões digitais da identidade da fonte, sem acrescentar URL/utilizador/MAC em claro ao bloco de identificação. No destino só uma associação inequívoca permite aplicar os dados; nunca se reutiliza um número interno coincidente. Perfis com nomes ambíguos e associações desconhecidas são omitidos com contagem, em vez de escolher arbitrariamente o primeiro.
- **INT-02:** falha ao ler configurações obrigatórias selecionadas impede publicar um backup aparentemente completo. A escrita atómica conserva o ficheiro anterior válido. A API de exportação também exige palavra-passe, porque URLs e outros metadados podem conter credenciais; continua a ler os ZIP/JSON antigos sem cifra. Cancelamento mantém a sua identidade.
- **INT-03:** validação de esquema, algoritmo, tipos, salt e orçamento de derivação antes de trabalho caro; campos e contentor usam os seus próprios esquemas. Iterações de escrita mantidas em 210 000; leitor aceita inteiros de 1 a 2 000 000, salt de 8–64 bytes e os formatos históricos suportados. Não altera parâmetros dos ficheiros antigos. Nonce/tag/corpo cifrado têm validação de estrutura e tamanho.
- **INT-04:** backups manuais recebem nome com data/hora e identificador único. Não altera a rotação automática nem o nome fixo dentro das pastas de staging remoto. A exportação local e remota pede repetição da palavra-passe; o restauro continua com um só campo. Diálogo com altura limitada e deslocação para caber na TV.
- **INT-05:** restauro mantém PIN e restrições infantis de perfis existentes por defeito. Quando o backup propõe alterações, a App mostra revisão e permite autorização explícita por perfil com o PIN local atual. O Core volta a verificar a autorização e a proteção dentro da transação. Campos ausentes em backups antigos não retiram proteção; perfis novos recebem a proteção disponível no backup. Autorização e palavras-passe ficam apenas no estado transitório da operação.
- **DAT-07:** uma única autoridade por processo serializa acrescentos e o ciclo completo de resolução/substituição de pendências. Não mantém uma edição DataStore aberta durante consultas Room. Falha ou cancelamento da resolução não substitui o conjunto original por uma lista incompleta.

As alterações foram implementadas na origem `C:/Users/renat/Downloads/OwnTV_Core`. A App usa um snapshot personalizado: o par ZIP/lock só é atualizado de forma coerente. Não se edita a cache gerada nem o Core privado da App Mobile.

## Compatibilidade e limites

- **Base de dados continua na versão 51.** Não foi necessária migração de tabelas.
- **Formato do backup passa a 26**, com metadados opcionais de identidade. A leitura dos formatos anteriores mantém-se.
- Backups antigos parciais sem informações suficientes para identificar uma fonte não podem ser associados com segurança por coincidência numérica. As entradas não associáveis são relatadas, sem alterar a fonte errada. Não inventa correspondências por nomes parecidos.
- Guardar um backup novo requer palavra-passe; backups antigos sem cifra continuam restauráveis. Credenciais e configurações completas mantêm-se dentro do contentor cifrado.
- Repetir palavra-passe evita gralhas; não recupera uma palavra-passe perdida.
- A transação Room protege o conjunto de perfis/fontes/links. O restauro completo continua a atravessar Room, DataStore e ficheiros, com o marcador de operação interrompida existente; não é apresentado como uma transação global única.
- Não altera canais guardados, motores, HLS, áudio/volume, buffer, tempos de reabertura, EPG ou a correção +1 h dos diferidos. Não gera nem instala APK, não assina nem publica.

## Salvaguarda antes das alterações

Confirmada igualdade dos 1 007 inputs do Core externo com o snapshot inicial `e2081e78acce95e5ac9b881164c98f066648e6a9400e554c9c9612f527e14890`.

Cópia de recuperação do código: `C:/Users/renat/Downloads/OwnTV-fase1-backup-20261020/`:

- ZIP original do Core e lock original.
- `app-settings-before.zip`: fontes/recursos/testes App e ferramenta de publicação do snapshot antes das alterações.
- `sha256.json`: hashes verificados dos três ficheiros de recuperação.

Esta cópia é do código no PC; não foi feita uma ligação à box nem uma cópia dos dados instalados no dispositivo.

## Validação

Validação concluída em 20/10/2026:

- **2 276 testes JVM aprovados**, sem falhas, erros ou testes ignorados: Core 1 298, player-core 552, App 426. Inclui 28 testes novos desta fase. A execução específica de backups também passou: 146 testes, incluídos no total do Core, não somados novamente.
- Compilação Kotlin da App `standardRelease` e `lintVitalStandardRelease` aprovados. A compilação debug e os testes da App também passaram. Não foram executadas tarefas de empacotamento APK.
- **6 testes Python** da ferramenta de snapshot aprovados.
- Inventário de traduções aprovado em ambos os repositórios; novos textos EN/PT/PT-PT com as mesmas chaves e XML válido. Só literais técnicos revistos foram classificados; baselines de texto por traduzir não aumentaram.
- `git diff --check` da App aprovado; verificação adicional de espaços em linhas acrescentadas nos inputs alterados do Core aprovada.
- ZIP/lock publicados em conjunto: **1 012 ficheiros**, SHA-256 `859f1d6cc93266d4fc09dd3442b73f3cd28a0541b150a6a6f8cfb2678fcd928c`. Cada entrada foi comparada byte a byte com a origem externa do Core.
- A validação conjunta usou `-Powntv.corePath=../OwnTV_Core`, aceite apenas com igualdade integral com o ZIP. Depois, `gradlew help` sem esse override confirmou a configuração normal pelo snapshot atualizado. Esta última operação valida a configuração, não repete a compilação.

Registos no PC: `%TEMP%/owntv-phase1-core-backup.log`, `%TEMP%/owntv-phase1-final-retry.log` e `%TEMP%/owntv-phase1-default-snapshot.log`. A primeira tentativa conjunta detetou literais técnicos novos ainda não classificados na App; o inventário foi corrigido e a execução conjunta repetida com sucesso. Permanecem avisos preexistentes de depreciação do Gradle e dois avisos Kotlin em `LocalTimeshiftServerTest`; não foram ocultados nem tratados como erros desta fase.

Foram acrescentados testes para identidades com IDs trocados, contas diferentes no mesmo servidor, ambiguidades, personalizações, proteção local, exportação sem cifra, falha de leitura, limites da cifra e concorrência/cancelamento das pendências. O catálogo de literais técnicos acompanha as alterações; não foi desativada a verificação nem aumentado o baseline de texto por traduzir.

## Testes na box após gerar o APK

1. Exportar duas vezes na mesma pasta: os dois backups devem coexistir. Palavra-passe repetida diferente não permite exportar.
2. Restaurar um backup cifrado recente e um antigo, selecionando as secções pretendidas; confirmar URL/credenciais, intervalos, personalizações e configuração por canal.
3. Num perfil existente com proteção diferente no backup, restaurar por defeito e confirmar que conserva o PIN/restrições locais. Só autorizar a substituição de propósito, com o PIN atual.
4. Após restauro parcial, confirmar os avisos de entradas não associadas; atualizar as listas normalmente, conforme o comportamento já escolhido.
5. Confirmar legibilidade e foco nos diálogos com comando, 720p e tamanho de letra aumentado.

Testes JVM/compilação não demonstram comportamento real de USB/SAF, firmware, comando ou interrupção por corte de energia.
