# Formatos dos diferidos — 10/10/2026

## Configuração

Definições → Guia e diferidos → Programas em diferido → **Formato dos diferidos**.

| Opção | Comportamento |
| --- | --- |
| Automático (predefinido) | Conserva o endereço inicial. Num arquivo Xtream iniciado em TS, pode tentar o endereço PHP e depois HLS. Se o endereço inicial já for HLS, pode tentar TS e depois PHP. |
| TS | Abre em TS. Pode tentar o endereço PHP alternativo do mesmo arquivo, sem passar automaticamente para HLS. |
| HLS | Abre em HLS quando o endereço Xtream reconhecido permite construir essa variante. Não passa automaticamente para TS. |

A preferência fica persistida e integra o backup das configurações. Não altera a preferência HLS dos canais em direto nem as personalizações por canal.

## Limites da recuperação

- Exclusiva de arquivo Xtream reconhecido; mantém canal, início e duração.
- Até três endereços distintos na cadeia Automático, sem ciclo entre formatos. Os mecanismos existentes de repetição e troca de motor continuam separados.
- Só na abertura, antes da primeira imagem no ExoPlayer ou do FILE_LOADED no mpv; não troca o formato a meio de um programa saudável.
- Aplica-se a HTTP 404, 405 e 415, ou a falhas identificadas de reconhecimento/leitura de formato sem outro código HTTP.
- Não tenta contornar 401/403, arquivo expirado (410), limite de pedidos (429), erros do servidor ou indisponibilidade de rede.
- Não muda a hora nem abre o direto como substituição. Os tokens de query são conservados.
- ExoPlayer e mpv internos usam a política. Um reprodutor externo recebe o endereço inicial escolhido, mas a OwnTV não consegue observar as suas falhas para aplicar a cadeia automaticamente.
- As alternativas dependem do servidor. Não se inventam endereços MP4, DASH ou formatos de outros portais. URLs M3U e Stalker que não correspondam a um arquivo Xtream mantêm o percurso original.

## Implementação

Política pura `CatchupFormats`, preferência `catchup_format` no DataStore e snapshot `PlaybackSettings`. Os resolvedores de programas e de recuo no direto aplicam a preferência inicial. Os callbacks de erro dos motores usam a cadeia do item atual, com as proteções de geração existentes; uma tentativa mpv alternativa deixa de continuar também com o pedido antigo.

Esta alteração amplia a tentativa PHP descrita em `CORRECOES_GUIA_DIFERIDOS_2026-10-10.md`; as correções de navegação e carregamento histórico desse documento permanecem.

## Validação

**BUILD SUCCESSFUL**: compilação StandardRelease, lint vital StandardRelease e testes aprovados. Relatórios XML: app 329, Core 1129, player-core 464; **1922 testes, zero falhas, erros ou ignorados**. Os quatro testes novos cobrem a cadeia limitada, formato escolhido, exclusões por estado/código HTTP, preservação de tokens e recusa de formatos/endereço não reconhecidos. Inventários de tradução e whitespace dos ficheiros alterados aprovados.

Não foi gerado APK nem realizado ensaio com arquivos reais do fornecedor numa box. A existência de EPG não comprova a existência do vídeo em TS ou HLS.
