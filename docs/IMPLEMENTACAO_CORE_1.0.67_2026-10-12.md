# Integração seletiva Core 1.0.67 — 12/10/2026

Autorização: implementar as recomendações, exceto opcionais. Âmbito: OwnTV TV e Core externo personalizado. O projeto Mobile e o seu Core privado não foram alterados.

## Lote A — Backups e transporte

- Só falha de autenticação GCM mantém WrongPasswordException. Cabeçalho inválido, estrutura truncada e falhas de execução preservam a sua classificação em vez de pedir novamente uma palavra-passe correta.
- Um corpo HTTP que termina antes de Content-Length é recusado antes de chegar ao importador. O cliente é fechado, a vaga de atendimento é libertada e o servidor continua a aceitar ligações. Logs contêm apenas o tipo de erro.
- Preservados limites de ficheiro/ZIP, desencriptação offset/length, autenticação e matriz de permissões LOCAL_SYNC.

## Lote B — Disponibilidade de gravações

- Nova verificação read-only de ficheiro inexistente, armazenamento indisponível, falta de permissão e ficheiro ilegível. Não cria pastas, não altera autorizações e não muda o destino.
- O estado aparece na lista e é atualizado de cinco em cinco segundos apenas com o ecrã ativo/subscrito. Progresso de uma gravação em curso não reinicia a pesquisa das gravações terminadas.
- Abrir uma gravação volta a verificar disponibilidade em IO antes de passar o ficheiro ao leitor interno/externo. Pacotes repartidos têm os seus segmentos validados ao abrir, evitando varrer centenas de partes a cada atualização da lista.
- A indisponibilidade é transitória: a linha e o estado histórico da gravação são preservados. Recolocar o ficheiro/USB permite recuperar a disponibilidade.
- Reutilizados os motivos existentes de captura, quota, espaço, limite de ficheiro e finalização. Sem alteração de esquema: a base de dados mantém v50.
- Corrigidos finais de linha inconsistentes no ficheiro RecordingsScreen durante a edição, preservando os restantes comportamentos.

## Lote C — Novas importações

- URLs de importação eliminam espaços exteriores e convertem separadores HTML &amp; para &. Percent-encoding de credenciais/assinaturas não é descodificado indiscriminadamente.
- Uma ligação get.php com credenciais é testada por player_api, com prazo máximo de oito segundos e corpo de resposta limitado a 64 KiB. Só uma resposta reconhecida como Xtream muda o tipo da nova origem.
- Sem confirmação da API, continua como M3U. Cancelar a operação cancela a ligação e não inicia uma importação de fallback.
- Preservados nome, credenciais codificadas, subdiretório do servidor, User-Agent, Referer e URL EPG. O fluxo existente guarda o intervalo de atualização e a escolha de lista predefinida.
- Modo só TV não ativa sincronização de filmes/séries. HLS estrito mantém preferência HLS mesmo quando a ligação get.php indica output=ts.
- O teste de ligação de uma origem M3U também reconhece get.php e usa a URL limpa, sem reescrever a origem guardada. As listas já existentes não são convertidas.

## Exclusões

Não implementados downloads paralelos, arbitragem adicional de sessões, restauro de categoria, filtros novos de pesquisa, ClearKey inline, vídeo por software acima de 1080p ou funcionalidades de smartphone. Não mudaram zapping, tempos de reabertura, buffers, áudio, volume a 100% ou dependência libmpv.

## Preservação e validação

Originais copiados e verificados por hash: C:/Users/renat/AppData/Local/Temp/owntv-core67-before-20261012. Não usar reset global de Git: existem alterações anteriores nos dois projetos.

- 62 testes dirigidos aprovados: classificação de falhas de backup, limites de memória/GCM, protocolo e servidor Companion, disponibilidade de ficheiros, reconhecimento/cancelamento de importação Xtream e operações de gravação.
- Inventários de tradução da app/Core e validação XML aprovados; textos visíveis têm recursos inglês/português. O inventário de dívida de tradução do Core não aumentou.
- Validação conjunta aprovada: 1 200 testes Core e 373 da app (1 573 no total, zero falhas/erros/ignorados), compilação standardRelease e lint vital; BUILD SUCCESSFUL em 6 min 12 s.
- Corrigido posteriormente um aviso de nulabilidade num teste novo, sem alterar código de produção. Snapshot final: 967 ficheiros, SHA-256 b3b360fc608f76b4ead3275c6cd32ec0b86bac7427a3cb46d65df676d45930c5. Reverificação aprovada: os três testes de disponibilidade passaram e release/lint foram confirmados nesse snapshot; BUILD SUCCESSFUL em 14 s.
- Registos: C:/Users/renat/AppData/Local/Temp/owntv-core67-directed-tests.log, owntv-core67-app-validation.log e owntv-core67-final-snapshot-validation.log.
- Nenhum APK, commit ou publicação. Não foram executados testes físicos de USB/SAF, foco D-pad, restauro em box ou fornecedor real.

Esta integração é seletiva, não equivalência integral à 1.0.67. Não declara resolvidos o bloqueio A–B–A ou problemas de qualidade de streams do fornecedor.
