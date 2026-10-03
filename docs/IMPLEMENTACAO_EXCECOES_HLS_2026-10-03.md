# Exceções manuais ao modo Apenas HLS

Implementado em 03/10/2026.

## Utilização

Definições → Reprodutor de vídeo → Exceções ao modo Apenas HLS.

1. Selecionar Adicionar canal em TS direto.
2. Procurar e selecionar o canal. A seleção guarda a exceção; não abre qualquer stream.
3. Selecionar Concluído e fechar o diálogo.
4. Abrir novamente o canal para aplicar a escolha.
5. Selecionar uma exceção na lista guardada para a remover. Na próxima abertura, o canal volta à definição geral.

## Comportamento

- Apenas canais adicionados pelo utilizador abrem em TS direto, com ExoPlayer. Não há aprendizagem automática de exceções.
- Todos os outros canais continuam a seguir Apenas HLS e as suas configurações globais.
- A referência inclui fonte e identificador remoto; a versão HD não inclui automaticamente Full HD nem canais de outra fonte. Sem identificador remoto, usa o nome exato dentro da mesma fonte.
- Xtream: converter somente o sufixo .m3u8 para .ts, preservando query/fragmento. Outras fontes: conservar o endereço original; não inventar endereços alternativos para URLs assinadas ou portais.
- O pedido transporta a escolha explicitamente. A fonte TS usa MIME MPEG-TS para evitar uma inferência HLS residual, incluindo aprendizagem anterior por host. Não se tenta HLS/mpv como alternativa de uma exceção TS.
- A escolha acompanha reconexão, reconstrução do player e regresso da app do segundo plano. Um novo canal recebe a sua própria escolha.
- Um preview com uma política diferente não é promovido para fullscreen sem reaplicar a escolha. As grelhas recebem a política por tile.
- O leitor do timeshift local conserva HLS; a exceção refere-se à ligação ao fornecedor.
- Áudio, vídeo, headers, DRM, volume e mecanismo de libertação da ligação anterior mantêm o percurso existente.

## Persistência e backup

DataStore: live_manual_ts_channels. Os registos guardam fonte, identificador remoto e nome, sem URL, senha ou token.

Backup completo: bloco opcional manualTsChannels na secção de configurações, limitado às fontes dos perfis escolhidos. No restauro, adaptar sourceId pelo mapa das fontes restauradas; ignorar referências sem correspondência, evitando aplicar a exceção a uma fonte diferente. Backups antigos sem o bloco não apagam as escolhas atuais. Não é necessária migração SQLite.

## Validação

Compilação StandardDebug concluída. 307 testes da app, 397 testes do player-core e 1000 testes do core sem falhas. Testes específicos cobrem isolamento TS→HLS, remoção da exceção, preview com política antiga, timeshift local, identidade de fonte/canal, conversão do endpoint, seleção da fonte de reprodução e remapeamento no backup.

Não foi gerado um APK. Não foi testado um canal real numa box. A exceção exige que o fornecedor disponibilize um stream TS válido; não transforma respostas HTML, recusas de acesso ou streams indisponíveis em vídeo.
