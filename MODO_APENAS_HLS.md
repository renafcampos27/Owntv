# Apenas ligações HLS

Opção em Definições → Reprodutor de vídeo, junto à escolha do motor de TV em direto. Ligada por defeito, incluindo instalações anteriores sem valor guardado. A alteração aplica-se na próxima abertura de uma transmissão.

## Comportamento

- Ligada: TV em direto no leitor interno usa ExoPlayer e HlsMediaSource. A preferência de motor global/por lista e os pins são temporariamente sobrepostos, sem serem apagados. A recuperação não pode avançar para TS direto, DASH ou mpv, nem para direct_source de formato desconhecido.
- Desligada: recupera as preferências atuais de motor e o comportamento anterior de preferência HLS/alternativas.
- Fontes Xtream: solicita a variante .m3u8 de um endereço .ts, mantendo query e fragmento. Não inventa endpoints alternativos para URLs M3U ou Stalker arbitrárias; estes são abertos pelo parser HLS e falham se não forem uma playlist HLS válida.
- Segmentos TS dentro de HLS continuam permitidos: não são uma ligação TS direta.
- As alternativas de canais continuam a ser procuradas pela lógica existente, mas cada abertura passa pela política HLS. Canais sem HLS não passam a funcionar por se ligar esta opção.
- Aplica-se também ao preview e aos mosaicos. Filmes, séries, arquivo e aplicações externas não são abrangidos.

## Persistência

Chave global live_hls_only no DataStore, valor inicial true. Incluída na lista de configurações exportadas/restauradas; o backup escreve o valor efetivo mesmo quando o utilizador nunca tocou no botão. Backups antigos sem a chave não apagam a escolha atual. Sem nova migração da base de dados.

## Validação

Testes adicionados: ausência de fallback TS/mpv após falha HLS, preferência mpv sobreposta sem alterar pins, recuperação da preferência ao desligar, preservação de query/fragmento na variante Xtream e ausência de reescrita de URLs arbitrárias.

A validação com o fornecedor e na Xiaomi/Thomson continua pendente. Esta opção impede mudanças indesejadas de formato; não demonstra nem corrige por si só a causa de um 403 no HLS.

Compilação concluída. Passaram 254 testes do player-core, 765 do Core e 179 da app. Não foi gerado APK.
