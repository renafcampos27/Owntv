# Implementação das recomendações aprovadas — 14/10/2026

Âmbito: OwnTV TV e OwnTV_Core personalizado. Autorização: “implementa apenas o que recomendas”, após revisão crítica de 13/10/2026. Mobile e seu Core privado não foram alterados. Preservados volume interno unitário, escolhas existentes, HLS normal, backups e separação entre horário EPG e correção dos diferidos. Sem APK, commit ou publicação.

## Fase A — integridade, imagens e perfis

- B1: modo ftsOnly deixa os índices B-tree intactos. Conservados coordenação dos escritores, FTS e restauração no finally; sem migração de schema.
- B16: política Coil específica de imagens recusa persistir erros HTTP e solicita novamente entradas antigas de erro. Imagens válidas continuam em cache; metadata revalidada por 304 conserva código de sucesso e corpo existente. O interceptor deixa de impor cache longa a respostas falhadas; cliente de streaming não é alterado.
- B13: operações sensíveis em perfil protegido exigem autorização do próprio alvo, com PIN atual, ação, estado de proteção, prazo curto e uso único. Core valida e DAO atualiza/apaga condicionalmente; alvo alterado entretanto é recusado. Nome/avatar não exigem autorização sensível. Avatar usa update pontual para não sobrescrever PIN antigo. Último perfil não pode ser apagado. Não foi criada função admin nem mudado formato de hash/backup.

## Fase B — EPG

- B31: parser XMLTV aceita precisão 12/14, ±HHMM/±HH:MM e Z, preservando UTC implícito e tolerância dos formatos legados.
- B36: deduplicação conserva pequenas sobreposições de fronteira e eventos com sinopses não vazias conflitantes. Duplicados precisam de grande cobertura temporal e inícios próximos, incluindo eventos curtos. Esta escolha conservadora pode preservar entradas de feeds com sinopses divergentes em vez de apagar um possível programa distinto.
- B39: grelha usa intervalos sem sobreposição; um programa longo pode retomar visualmente após um apontamento curto. Cada fragmento conserva a entidade e o início original, com chave estável; desenho, seleção e navegação partilham o mesmo contrato. Lacunas reais continuam vazias. Não há mudança de timestamp pedido ao arquivo por causa do início visual do fragmento.
- L3: consolidado avanço duplicado do parser, sem alterar profundidade/cancelamento.
- B18: cache restaurado calcula computeContentHash canónico; mantidos staging em disco, lotes de 500 e atomicidade.

## Fase C — fornecedor

- B30: leituras escalares residuais Xtream são tolerantes, com tratamento por campo. Flags verdadeiras/falsas preservam significado; ausência de formato não vira extensão .false.
- B34: template útil pode ativar diferido sem tag redundante; dias sem método e modos none/off/false não inventam capacidade.
- B38: query relativa resolve contra URL base preservando parâmetros; paths relativos e referências de host usam resolução URI adequada, separados de append/query. URLs absolutas continuam diretas.
- B29: resolução Stalker nula/expirada tem resultado de falha explícito e vinculado ao owner. Não inicia descodificadores nem percorre motores para uma URL que o portal não forneceu. Prazos e cancelamento mantidos.

## Fase D — faixas, foco e resposta local

- B28: escolha explícita e retenção de faixa normalizam códigos ISO, conservando labels e ambiguidade; não trocam regiões ou scripts conhecidos diferentes. Preferência automática genérica permanece igual.
- O11/B25/O9: chegada assíncrona de canais volta a solicitar foco no alvo com requestFocus Boolean/repetição limitada; categorias usam chave real/virtual serializada, incluindo nomes duplicados e renomeações; o rótulo não é usado como identidade no Live.
- B4: indicador de som do multiview acompanha a saída efetivamente disponível quando o tile falha e não existe sobrevivente.
- B32: servidor local distingue preparação (503 com marcador privado) de indisponibilidade terminal (410), produzindo HTTP completo também no HEAD. Apenas manifesto do servidor privado 127.0.0.1/UUID com esse marcador recebe repetição limitada; não ativa backoff do fornecedor, fallback de faixa nem nova espera global. Fluxo normal continua a aguardar segmentos antes de abrir.

## O que fica pendente deliberadamente

B35/L6/JNI, B23/abortar pausa, B33/sequência de eventos do comando, O10/WAL de desempenho, GPU/novos índices/chunking geral, flags TS progressivo, SAF novo, zapping numérico no overlay e purga de legendas. São hipóteses, opções ou alterações condicionadas que a revisão não aprovou para implementação direta. Não correspondem a falhas esquecidas deste lote.

## Validação

Verificações finais concluídas:

- Core: 1 242 testes unitários, sem falhas, erros ou testes ignorados; testes Android compilados.
- Player: 544 testes unitários, sem falhas, erros ou testes ignorados, incluindo regiões/scripts de idioma.
- App: 393 testes unitários, sem falhas, erros ou testes ignorados; compilação Kotlin debug/release e compilação dos testes Android concluídas.
- Total: 2 179 testes unitários aprovados. Compilação repetida após a última correção de nulabilidade no guia e lintVitalStandardRelease aprovados.
- Inventário i18n: App e Core aprovados, sem novas mensagens de interface fora dos recursos.
- Testes dirigidos adicionais: XMLTV, deduplicação/cache EPG, células do guia, autorização de perfis e cache de imagens. Incluem 5 testes Coil com NetworkFetcher/DiskCache reais e respostas controladas. Estes testes sobrepõem-se às suites e não são somados aos totais.

Snapshot do Core atualizado e confirmado pela integração Gradle: 989 ficheiros, SHA-256 `7d575983924a02912495339bc8b31faf2ba1f4c00ae7aa989ee0d2be5b3367a6`. Não representa uma nova equivalência ao upstream.

Registos desta execução em `C:\Users\renat\AppData\Local\Temp`:

- `owntv-recommended-core-validation-pass.log`: testes Core/Player e compilação dos testes Android Core.
- `owntv-recommended-app-validation.log`: compilação App debug/release, testes, testes Android e lint release; BUILD SUCCESSFUL.
- `owntv-recommended-final-validation.log`: revisão final após correção do aviso no guia e testes adicionais de idioma; BUILD SUCCESSFUL. Esta é a confirmação final dos ficheiros atuais.

Permanece o aviso geral de funcionalidades Gradle descontinuadas antes da versão 10; não bloqueou a compilação e não foi mascarado com baseline de lint. O aviso novo de nulabilidade no guia foi corrigido na origem.

Os testes Android foram apenas compilados. Não foi gerado APK nem realizado ensaio físico, medição de CPU/RAM ou teste de ligação ao fornecedor. Os resultados automáticos não demonstram, por si só, ausência de falhas na box.

## Ensaios recomendados na box

- Guia: pequeno overlap em programas com título igual, programa longo intercalado, continuação e abertura do diferido pelo horário original.
- Comando: abrir lista antes de os dados chegarem, reorder/filtrar categorias, regressar do player e verificar foco sem autoplay.
- Perfis: PIN errado/correto, trocar PIN durante edição, editar outro perfil protegido, apagar ativo e impedir apagar último.
- Imagens: logótipo ausente que passa a existir, cache antiga e arranque posterior.
- Timeshift: preparação, pronto, fim/fecho; sem espera indefinida nem troca de versão por um estado local transitório.
