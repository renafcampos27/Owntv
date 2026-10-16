# Segmentos HLS e limiar superior de carregamento

Data: 2026-10-11. Âmbito: OwnTV para Android TV e Core local.

## Implementação

- O diagnóstico do ExoPlayer/HLS mostra a duração do último segmento publicado, o intervalo mínimo–máximo dos segmentos da playlist, a duração de referência declarada, a janela disponível e, quando aplicável, a referência das partes LL-HLS.
- A leitura usa a playlist já recebida pelo ExoPlayer. Não faz pedidos adicionais, não guarda URLs e não transporta valores de outro canal.
- «Último publicado» não significa último descarregado. A janela HLS disponível também não é o conteúdo em buffer.
- O diagnóstico apresenta o conteúdo efetivamente em buffer, o limiar superior de carregamento e se o leitor está a carregar conteúdo ou em espera.
- A reserva personalizada permite um limiar inferior de 1–60 segundos e um superior independente até 70 segundos. Exemplos: 8–30, 8–45, 8–60 e 8–70 segundos.
- Configuração global, por lista e por canal, edição em lote, persistência e backup/restauro aceitam o novo intervalo. As configurações anteriores válidas são preservadas.
- O superior parte do intervalo escolhido, antes da elevação do inferior pelo buffer inicial. Exemplos: 2–30 com início de 10 segundos resulta em 10–30; 1–70 com início automático de 1,5 segundos resulta em 1,5–70.
- A predefinição Equilibrada 8–10 segundos mantém-se. Aumentar apenas o superior não aumenta o objetivo de memória nem altera os limiares de início e retoma.

## Interpretação

O superior é um limiar para parar o carregamento antecipado, não um limite exato de duração em memória. Um segmento já em descarga pode levar o buffer acima do valor escolhido. O objetivo de memória, o débito do canal, a disponibilidade do direto e a posição de reprodução podem impedir que o valor seja atingido.

O fornecedor define a duração e a disponibilidade dos segmentos. Não define o máximo de buffer da aplicação. Não foi acrescentado um modo de reserva «Automático»: os valores nativos do ExoPlayer não equivalem à política Equilibrada da OwnTV. O atraso automático face ao direto continua disponível e é independente desta reserva.

## Ensaio na box

1. Em Reserva de reprodução → Personalizada, manter o inferior em 8 segundos e escolher superior de 30 segundos.
2. Reabrir o canal para aplicar a configuração. Manter o buffer inicial e o atraso face ao direto constantes.
3. Consultar os segmentos publicados, o conteúdo em buffer, o estado de carregamento e a memória no diagnóstico.
4. Comparar com superior de 45 ou 60 segundos. Se o buffer parar antes disso, verificar memória e janela disponível; não aumentar automaticamente a memória.
5. Confirmar navegação por comando, persistência após reiniciar e backup/restauro na box.

## Referências

- [DefaultLoadControl](https://developer.android.com/reference/androidx/media3/exoplayer/DefaultLoadControl)
- [HlsMediaPlaylist](https://developer.android.com/reference/androidx/media3/exoplayer/hls/playlist/HlsMediaPlaylist)

## Validação

Validação central concluída: compilação Kotlin Standard Release, 330 testes da app, 1 149 do Core e 476 do player, sem falhas; 1 955 testes no total. O lint vital Standard Release e as verificações i18n da app e do Core passaram. Foram acrescentados 12 testes neste lote. Uma expectativa incorreta num teste de limites foi corrigida e a validação repetida com sucesso.

A validação visual e de reprodução numa box física permanece necessária. Não foi gerado um APK.
