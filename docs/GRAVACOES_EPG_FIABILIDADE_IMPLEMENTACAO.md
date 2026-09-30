# Gravações EPG — implementação e validação

Data: 30/09/2026. Alterações na app e no Core local. Não foi gerado APK.

## Comportamento implementado

1. **Segmentos HLS com falhas:** até três tentativas sequenciais do mesmo segmento, com nova leitura da playlist e atualização dos endereços assinados. Depois de esgotar as tentativas, o corte fica registado. Um erro de acesso persistente não é contornado aumentando o buffer.
2. **Continuidade:** só são confirmados segmentos descarregados integralmente, validados e escritos em disco. A sequência confirmada sobrevive a reconexões e à interrupção do processo. URLs diferentes não duplicam a mesma sequência; segmentos de sequências diferentes com conteúdo igual continuam válidos.
3. **Formatos:** seleção e fixação de uma variante compatível da playlist master; intervalos de bytes e inicialização fMP4; ficheiro TS ou MP4, sem recodificar. Áudio e vídeo mantêm a qualidade da variante selecionada.
4. **Resultado honesto:** estado «Parcial», com reprodução do ficheiro disponível, duração de media captada, duração prevista e quantidade/duração estimada dos cortes. Gravações antigas sem medição não passam a mostrar uma duração inventada.
5. **Horários EPG:** atualização de agendamentos futuros após importação automática ou manual. Mantém o ID e as margens antes/depois. Exige correspondência única de título/canal e mudança de até três horas; se houver ambiguidade, mantém o horário anterior. Gravações em execução, recuperações e cancelamentos não são reprogramados.
6. **Recuperação:** em gravações parciais, falhadas ou perdidas, o botão de recuperação cria uma nova linha e um novo ficheiro. Se o programa ainda estiver no ar, grava o restante; se terminou e o arquivo estiver disponível, pede o programa pelo diferido. Conserva o original e respeita o orçamento de ligações da fonte. Não junta automaticamente os dois ficheiros.

## Escrita, armazenamento e migração

- Pequenos registos por segmento e um resumo de tamanho constante: evita reescrever uma lista crescente na base de dados a cada segmento.
- Download temporário antes de confirmar; cancelamento fecha o pedido e descarta o segmento incompleto.
- Recuperação de gravações interrompidas que ainda têm segmentos persistidos; classificação terminal quando a janela já terminou.
- Reserva de espaço para finalizar o ficheiro, além dos 500 MiB protegidos. A finalização fMP4 pode exigir três cópias temporárias dos dados, contando os segmentos originais.
- Eliminar uma gravação elimina também o seu armazenamento temporário retido, depois de parar o gravador.
- Migração Room 47 → 48: preserva linhas/ficheiros existentes e acrescenta métricas, checkpoint e identificação das tentativas de recuperação. As gravações continuam locais, fora do backup portátil/sincronização, como anteriormente.

## Limites explícitos

- HLS com áudio numa playlist separada permanece **não suportado** pelo gravador: escolhe uma variante multiplexada se disponível; caso contrário recusa, em vez de guardar vídeo sem som.
- fMP4 com mudança de inicialização/período é recusado de forma explícita. O suporte a todos os codecs depende do extrator/muxer Android e exige teste na box.
- HLS encriptado/DRM continua recusado.
- Destino por caminho local: teto de captura 16 GiB. Destino SAF: staging interno limitado a 512 MiB; gravações longas neste modo podem terminar por falta de espaço/orçamento. Para gravações longas, validar o destino por caminho local/USB.
- MP4 gravado num destino SAF conserva atualmente o nome/extensão `.ts`; o conteúdo é MP4. A extensão e o tipo do documento necessitam de melhoria futura.
- Falha na finalização conserva os segmentos para não os destruir, mas não garante um ficheiro reproduzível. Recuperar pelo diferido cria uma nova captura; não é uma operação de reparação desses segmentos.
- Checkpoints protegem contra interrupção do processo; não constitui prova de resistência a todos os cortes físicos de energia ou falhas do armazenamento.

## Validação

Compilação conjunta aprovada. **1 354 testes passaram**, sem falhas, erros ou testes ignorados: App 196, Core 850, player-core 308. Incluem 41 testes adicionais nesta implementação, com migração JDBC 47 → 48, restauro de segmentos, retry/cancelamento, formatos HLS, matching EPG e apresentação da duração. O schema Room 48 foi gerado pela compilação. Verificação de literais i18n e verificação de diferenças dos ficheiros alterados aprovadas.

Registo final: `C:\Users\renat\Documents\Codex\2026-09-22\c-users-renat-downloads-owntv-main\work\recording-improvements-validation-final.log`.

Não foram feitos testes na Xiaomi/Thomson nem pedidos ao fornecedor IPTV. A migração Room em Android, os alarmes reais, o remux com codecs do fornecedor e a reprodução dos ficheiros na box permanecem por validar.

Testes manuais necessários:

1. Gravar um programa HLS saudável e verificar som, imagem e duração.
2. Interromper a rede durante a gravação; confirmar retoma sem segmentos duplicados e estado parcial quando existe perda.
3. Interromper/reabrir a app durante uma gravação e verificar os segmentos já captados.
4. Atualizar um EPG com horário alterado; confirmar margens e ausência de timers duplicados.
5. Recuperar pelo diferido; confirmar que o original permanece intacto e a nova cópia reproduz.
6. Testar USB, espaço reduzido, cancelamento e eliminação sem pastas temporárias abandonadas.

Referências técnicas: [HLS — RFC 8216](https://www.rfc-editor.org/rfc/rfc8216) e [MediaMuxer Android](https://developer.android.com/reference/android/media/MediaMuxer).
