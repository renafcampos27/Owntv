# Implementação da auditoria consolidada — 13/10/2026

Âmbito: OwnTV Android TV e Core personalizado. OwnTV Mobile e o respetivo Core privado não foram alterados. Não há alteração de volume interno: continua a política de volume unitário/100%. Não foram gerados APKs nem publicados commits.

## A — Integridade, arquitetura e sessões

- **N3:** um arquivo RAW interrompido depois de receber bytes termina como parcial; não repete um corpo completo sobre o prefixo existente. Não se inventou retoma Range sem validação do servidor. Resposta 206 sem Range válido é recusada.
- **N4:** finalização DASH só é sucesso depois de stop do muxer e validação do contentor/saída. Fontes não são apagadas perante falha. Cópia para SAF valida tamanho e coopera com cancelamento.
- **N1/N2:** CI escolhe o APK universal explicitamente; atualizador escolhe arquitetura suportada e verifica bibliotecas do APK. Sem escolha arbitrária do primeiro asset.
- **R1/R2:** o orçamento respeita o total de reservas registadas. Gravação não presume preempção de um leitor ativo. Admissão e reserva de multiview são atómicas.
- **R3:** retirada de tiles e transições principal↔grelha aguardam confirmação de fecho local; a reserva antiga só é libertada nessa confirmação. Uma falha de retirada mantém reserva e permite repetição explícita, sem rotina infinita nem autorização automática por timeout. Não representa confirmação de libertação da sessão no servidor IPTV.

## B — Pedidos, identidade e diagnóstico

- **R4:** Referer explícito da lista é usado no download M3U e como default nos pedidos de reprodução. Headers do canal prevalecem; Stalker e fontes locais não herdam identidade genérica. Sem alterar User-Agent global.
- **R5:** HTTP é registado antes das decisões de recuperação, associado ao proprietário da sessão. O resumo conserva código, tipo de pedido e instante sem URL, senha ou tokens. Eventos antigos não alteram sessão nova.
- **N5:** Retry-After em abertura, playlist e chave de gravação chega à política de repetição. Espera monotónica, cancelável e limitada pela janela da gravação.
- **N6/N7:** leitores externos validam perfil, fonte e geração antes do lançamento; mudanças/background invalidam pedidos pendentes. Diferidos preservam User-Agent e headers. Leitores internos são retirados antes da abertura externa na rota Live.
- **N8:** pedido renovado Stalker aplica a mesma correção temporal dos diferidos; datas guardadas na base de dados mantêm-se originais.

## C — Configuração, operações e backup

- **S1/S2:** acrescentadas oito preferências de gravação/multiview. Novos backups incluem os defaults efetivos principais e metadados de chaves portáteis implicitamente definidas. Validação rejeita conflitos, duplicados e chaves não permitidas antes de escrever. Backups antigos preservam merge.
- Limite: defaults portáteis que não foram materializados usam o default da versão de destino. Não se promete clonagem de defaults desconhecidos entre builds diferentes; caminhos SAF e permissões de outra box não são resetados pela nova política.
- **S6:** operações locais de backup são serializadas. Fechar/reiniciar um diálogo durante escrita não cancela o import/export nem admite outro escritor. Resultados obsoletos não substituem uma operação nova.
- **U1–U4:** gravação das opções e versões usa snapshots imutáveis, estado de operação e confirmação. Falha ao carregar versões não apaga o rascunho; restaurar/guardar aguardam catálogo autoritativo. Desassociação aguarda resultado.

## D — Guia, apresentação e coerência

- **U5:** nome de descodificador desconhecido continua visível.
- **U6:** falhas de carregamento têm estado próprio e repetição, em vez de catálogo vazio fictício.
- **U7/U8:** células válidas retidas permitem OK durante carregamento adjacente. Identidade/revisão da cache evita programas de outro perfil/desvio. Saída do guia cancela trabalho pendente e conserva LRU concluído.
- **U9:** historial diagnóstico de tentativas falhadas é limpo quando muda perfil/fontes.
- **S3/S4:** Pulso removido; diálogo de números mostra Concluído, coerente com passos aplicados imediatamente.
- **S5:** pré-buffer novo usa escolhas comuns e preserva valores legados guardados; zero mantém o significado automático do motor. Desentrelaçamento é identificado como opção mpv. Não se ativou um timeout legado de fonte sem função comprovada.
- **N9:** estar no canal antes do programa começar suprime o aviso sem consumir o lembrete. Mudar de canal permite aviso; continuar até ao início consome-o.

## E — Limpeza

Removidas dependências App navigation-compose e KSP/Room compiler sem consumidores locais. Core conserva geração Room; App conserva runtime Room, Media3, paging e perfis de baseline.

Removidos componentes mortos de diálogo/badge/progress/preview cinematográfico e a cadeia App OpenSubtitles Account/Search/Delete, incluindo registos DI. Helpers de contagem e timestamp usados foram preservados em ficheiros próprios. Legendas embutidas/importadas, cache/backend Core, dados de backup, TimeshiftResumeDialog e layouts ainda referenciados foram mantidos.

## Validação

Validação final coordenada concluída:

- Core: 1 225 testes, zero falhas/erros.
- Player Core: 532 testes, zero falhas/erros, repetidos após a vinculação de diagnóstico na promoção do preview.
- App: 385 testes, zero falhas/erros. Total: 2 142 testes.
- Compilação Kotlin standardRelease e testes Android: concluídas.
- lintVitalStandardRelease: passou, sem supressão dos erros fatais.
- Inventários de i18n: passaram; snapshot Core sincronizado e validado.
- SHA-256 do snapshot: 30d0c47c4f0c051fcf88672db738072c0d555a186876f89858571f1b4e1dc0b5.

A primeira passagem detetou testes de formatação exclusivos do ecrã OpenSubtitles eliminado; foram preservados em backup e retirados com o respetivo código morto. A segunda passagem completa passou. Avisos preexistentes de reflexão em testes/depreciação Gradle não foram confundidos com erros de release.

Compilação/testes não substituem ensaio físico do comando/foco, SAF, muxer Android e fornecedor IPTV. Nenhum APK foi gerado.

## Ensaios na box

1. Multiview: substituições rápidas, fechar/abrir, Home e regressar; verificar que não há som de tiles antigos nem pedidos novos antes da retirada.
2. Gravação/diferido: interromper rede, parar/guardar, apagar durante finalização, destino interno e SAF; reproduzir ficheiro final e parcial.
3. Backup: exportar/restaurar nova cópia e uma antiga; confirmar reservas, tempos, margens e opções multiview, sem alterar permissões SAF.
4. Guia/versões: sair/reentrar durante carga, simular falha, manter rascunho, guardar/desassociar e regressar com D-pad.
5. Externo: selecionar outro canal/perfil ou Home durante resolução; não pode abrir posteriormente um pedido antigo.
