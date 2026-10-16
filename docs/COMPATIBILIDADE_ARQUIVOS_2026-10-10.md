# Compatibilidade dos diferidos — implementação de 10/10/2026

## Funcionalidades

### Identificação de HLS/DASH em endereços sem extensão

O ExoPlayer observa a resposta do pedido de abertura existente, exclusivamente para arquivos e antes da primeira imagem. Usa Content-Type ou até 512 bytes do conteúdo. Se a leitura falhar por parsing e a resposta identificar HLS ou DASH diferente do tipo usado, reabre o mesmo endereço uma vez com o MIME confirmado. Uma revisão/URL da abertura impede usar uma deteção antiga. Não há pedido adicional de sondagem nem reabertura de reprodução saudável. A correção prevalece sobre um tipo manual incorreto.

### Alternativas por lista/canal

Definições → Guia e diferidos → Programas em diferido → **Endereços alternativos dos diferidos**.

Selecionar lista e editar a lista inteira ou pesquisar um canal. Até três modelos fornecidos pelo serviço, com escolha de tipo Automático, HLS ou DASH. Automático permite a deteção normal de contentores como MP4. Guardar/apagar personalização; a do canal prevalece sobre a da lista.

Os modelos exigem início e fim/duração: `{utc}`/`{start}`, `{end}`/`{duration}`, ou componentes UTC `{Y}`, `{m}`, `{d}`, `{H}`, `{M}`. Podem usar `{streamId}`, `{username}`, `{password}`. Credenciais são substituídas apenas na resolução, com encoding de URI. O intervalo já inclui a correção dos diferidos configurada pelo utilizador, sem aplicar um segundo desvio.

Exemplo de estrutura, não um endereço universal:

`https://servidor.example/arquivo/{streamId}?inicio={utc}&duracao={duration}`

As alternativas manuais vêm depois dos endereços normais. São deduplicadas e o conjunto fica limitado a seis endereços. ExoPlayer e mpv avançam apenas em falhas iniciais de endereço/formato permitidas; preservam o programa, não passam para direto, não contornam autenticação/limites de sessões e não sondam os endereços ao guardar. O leitor externo recebe o primeiro endereço; a app não consegue monitorizar as suas falhas para aplicar automaticamente a cadeia.

Os modelos são persistidos e seguem os IDs das listas ao restaurar. Para os incluir integralmente no backup, usar proteção por palavra-passe: um modelo pode conter credenciais ou tokens literais.

### Flussonic

Só para origem declarada Flussonic, sem template explícito que já tenha prioridade:

- Programa finito HLS: `index-início-duração.m3u8`.
- Variante fMP4: `index-início-duração.fmp4.m3u8`.
- Recuo contínuo: `timeshift_abs-início.m3u8`, preservando `.fmp4` quando presente.
- TS mantém `archive-início-duração.ts`; queries/tokens conservados.

Estas convenções são documentadas no [manual HLS Flussonic](https://flussonic.com/doc/fms/protocols/hls/) e no [manual DVR](https://flussonic.com/doc/fms/dvr/playback/). Não são aplicadas por tentativa a qualquer lista Xtream.

### Retoma conforme capacidade

Uma recuperação/troca de motor pode conservar a posição quando o arquivo confirma seeking: timeline do ExoPlayer, ou propriedade `seekable` do mpv juntamente com manifesto/contentor adequado. Arquivos TS sem essa confirmação mantêm reabertura desde o início. Não se assume suporte a Range em todos os servidores.

## Limites

Não foram inventados endereços DASH/MP4 por troca de extensão. Não foram acrescentados SmoothStreaming ou RTSP sem uma fonte real que os necessite. Os metadados DRM do direto não são copiados cegamente para o arquivo. Um 404 de arquivo inexistente continua a ser um erro; EPG não comprova gravação disponível. A deteção automática é limitada a um prefixo curto e a manifestações reconhecidas, não a qualquer resposta arbitrária.

## Validação

Validação concluída: compilação Kotlin Standard Release, testes unitários da app/Core/player e lint vital Standard Release passaram. Foram executados 1 941 testes (329 app, 1 143 Core, 469 player), sem falhas nem erros. Os inventários de internacionalização e a verificação de espaços dos ficheiros alterados também passaram. Não foi gerado APK nem ensaiada reprodução num fornecedor/box reais. A navegação D-pad dos novos controlos e a disponibilidade real dos formatos de arquivo precisam de ensaio no dispositivo.
