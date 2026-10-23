# OwnTV TV — fase 3: pesquisa e cache EPG

**Data:** 22/10/2026. **Âmbito:** DAT-05 e DAT-08 do plano principal. Esta fase é independente das etapas A–D das novas publicações, já concluídas.

**Estado:** implementada e validada automaticamente no Core e na App. Aceitação física pendente. Não foi gerado APK nem realizada aceitação física.

## DAT-05 — recuperação da pesquisa

- Conferidos os 16 triggers FTS4 com `contentSyncTriggers` do esquema Room 51 e com o código gerado pela versão Room 2.8.5. O contrato inclui os dados legados, sem voltar a expor filmes/séries na App.
- A verificação na abertura passa a detetar triggers ausentes **ou com uma definição incorreta**, além dos objetos anteriormente controlados. Uma base saudável não executa reconstruções nem DDL nesse percurso.
- Reparação protegida por savepoint: uma falha desfaz a reparação parcial, preservando uma eventual transação exterior. Reconstrução de FTS apenas quando faltou a tabela ou houve alteração dos seus triggers; não em todas as aberturas.
- A remoção e a recuperação de índices durante inserções em lote usam transações curtas de Room. A transferência e o parsing do catálogo continuam fora dessas transações.
- Só são retirados índices geridos pelo contrato canónico. Índices desconhecidos são conservados.
- A entrada de escritores e o estado da recuperação não se perdem por cancelamento entre o DDL e o registo do escritor. A recuperação espera pelo último escritor; se falhar, mantém o estado para nova tentativa antes de admitir o próximo.
- Uma falha na limpeza não substitui a exceção original. O diagnóstico de abertura distingue reparação falhada de base saudável.
- **Sem alteração global de `recursive_triggers`.** A implementação de `InvalidationTracker.configureConnection` no Room usado já ativa essa opção nas ligações de escrita. Acrescentado teste Android para verificar esse contrato.

A pesquisa durante uma importação inicial continua sujeita aos dados já importados e à reconstrução final prevista no modo bulk. Não se promete um catálogo completo enquanto a importação não terminou.

## DAT-08 — retenção limitada de EPG

- Cache «agora/seguinte»: até **256 canais** e peso de retenção de **2 MiB**.
- Cache de linhas do fornecedor: até **128 canais** e peso de retenção de **4 MiB**.
- Peso estimado a partir do texto e de uma margem por programa; **não é uma medição nem um limite do heap total da App**. As caches de pedidos/histórico e as suas políticas existentes mantêm-se.
- Remoção dos itens menos recentemente usados, com acessos sincronizados. Um resultado maior que o orçamento é devolvido completo, mas não fica retido. Nunca se corta uma lista de programas para a fazer caber.
- Remover uma entrada de cache não cancela pedidos partilhados em curso. A invalidação explícita por revisão mantém a autoridade anterior.
- Identidade da resposta considera origem/canal, ID remoto, associação e alternativa EPG, URL/cabeçalhos do canal e desvio efetivo. Perfis com políticas diferentes não reutilizam uma resposta incompatível; políticas equivalentes podem partilhar dados.
- Entradas datadas no futuro são recusadas após um recuo do relógio. O TTL e a expiração na mudança de programa mantêm-se.

## Evidência automática

- Core: **1 349 testes JVM**, zero falhas/erros/ignorados.
- Motores: **598 testes JVM**, zero falhas/erros/ignorados.
- **14 novos testes JVM:** 8 sobre contrato/reparação com SQLite real via JDBC e 6 sobre limite de retenção, concorrência e contexto EPG.
- Quatro novos testes Android sobre falha no terceiro DROP, falha de recuperação seguida de nova tentativa, conservação de índice desconhecido e configuração do writer foram **compilados, não executados num dispositivo**. Os testes existentes de escritores concorrentes/cancelamento mantêm-se.
- Seis testes da ferramenta de snapshot aprovados.
- Inventário i18n do Core: **263 entradas de baseline + 5 800 literais técnicos**; baseline não aumentado.
- App: compilação standard debug/release, **429 testes JVM** e `lintVitalStandardRelease` aprovados.
- **Total: 2 376 testes JVM + 6 testes da ferramenta de snapshot aprovados.**
- Configuração normal do projeto, sem forçar snapshot nem ignorar a integridade, aprovada (`gradlew help`).
- Inventário i18n App: **51 entradas de baseline + 1 088 literais técnicos**, sem aumento da dívida.
- Log Core: `%TEMP%/owntv-phase3-core.log` — `BUILD SUCCESSFUL`, 3 min 14 s.
- Log App: `%TEMP%/owntv-phase3-app.log` — `BUILD SUCCESSFUL`, 5 min 9 s. Configuração normal: `%TEMP%/owntv-phase3-normal-config.log` — sucesso em 5 s.

Os avisos anteriores dos testes de timeshift/Gradle não foram silenciados. Não foram medidos ganhos percentuais de velocidade ou memória numa box.

## Integridade e recuperação

- Cópia anterior: `C:/Users/renat/Downloads/OwnTV-fase3-backup-20261022/`, com App/Core, snapshot anterior e manifesto SHA-256 verificado.
- Snapshot anterior: `eb701f07d10fe9fd843faba4ed0b474b0100d6b8570480fb3449ab08a6ea4928`, 1 047 inputs.
- Novo snapshot: `eb335c31686c9e9ad116c2adf91dab4d721cc5e5c117ea5af8b93334118099d9`, **1 052 inputs**. Conferidos o hash, o conjunto de nomes e todos os bytes com o Core externo.
- Esquema continua **51**; formato dos backups, dependências, motores, HLS/exceções por canal, buffers, espera de reabertura, gravações, correção de diferidos +1 h e volume interno de 100% não foram alterados.
- Nenhuma alteração ao código do OwnTV Mobile, assinatura, commit, push ou publicação.

## Aceitação física pendente

1. Atualizar uma lista e confirmar pesquisa por canais novos/renomeados; cancelar uma atualização e repetir.
2. Percorrer muitos canais no guia, regressar a canais anteriores e confirmar que o programa/hora continuam corretos.
3. Alterar perfil, associação EPG e desvio; confirmar ausência de títulos herdados da configuração anterior.
4. Verificar pesquisa/guia depois de suspender/despertar, mantendo reprodução e gravações habituais.
5. Executar os testes Android novos num dispositivo/emulador compatível. As falhas de DDL são ensaiadas apenas em bases de teste, nunca provocadas nos dados da box.

**Próxima fase do plano principal:** fase 4 — reprodução, diferidos e mudança de canal. Não iniciada nesta intervenção.

## Referências verificadas

- [SQLite — savepoints e transações aninhadas](https://www.sqlite.org/lang_savepoint.html).
- [SQLite — FTS4 com conteúdo externo e sincronização por triggers](https://www.sqlite.org/fts3.html#external_content_fts4_tables).
- [SQLite — REPLACE e triggers de eliminação](https://www.sqlite.org/lang_conflict.html).
- Contrato local: `core/schemas/tv.own.owntv.core.database.OwnTVDatabase/51.json`, `OwnTVDatabase_Impl.kt` gerado e fontes de `room-runtime-android:2.8.5`.
