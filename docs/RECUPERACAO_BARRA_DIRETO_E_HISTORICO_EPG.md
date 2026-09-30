# Barra do direto e histórico do EPG — 30/09/2026

## Correções

- A barra volta a aparecer nos controlos do leitor em direto, incluindo canais sem a marcação de diferidos.
- Quando existe arquivo do fornecedor, mantém-se a barra de recuo original e os saltos para programas anteriores.
- Sem essa marcação, o ExoPlayer permite navegar na janela HLS publicada pelo servidor. Esquerda recua, direita avança e o botão de direto regressa à posição segura do direto na mesma reprodução, sem voltar a sintonizar o canal.
- O passo de recuo HLS acompanha a definição existente de recuo do direto.
- A janela HLS só é consultada enquanto a barra está visível. As operações verificam novamente a sintonia atual e a capacidade de seek; não usam uma janela de um canal anterior.
- O EPG conserva e permite consultar sete dias anteriores também em canais sem diferidos, mantendo a leitura por linha. A possibilidade de reproduzir esses programas continua a depender do arquivo do canal.
- Canais com arquivo mas sem número de dias declarado usam o mesmo padrão de sete dias no EPG e no recuo do direto.

O recuo HLS não é uma gravação local nem altera o tamanho da reserva de memória. A duração disponível depende da playlist do servidor. Sem janela navegável nem arquivo, a barra aparece mas não oferece um recuo fictício.

Referência: [Media3 — navegação na janela do direto](https://developer.android.com/media/media3/exoplayer/live-streaming).

## Utilização e verificação na box

1. Gerar e instalar o novo APK, mantendo a instalação e as listas existentes.
2. Num canal em fullscreen, carregar em OK para mostrar os controlos, levar o foco até à barra e usar esquerda/direita. Verificar também o regresso ao direto e ↑/↓ para mudar de canal.
3. Abrir o EPG e atualizar a programação. Esta atualização é necessária se os programas antigos já foram apagados pela política anterior; só serão recuperados os que a fonte ainda disponibilizar.
4. Navegar para dias anteriores e testar um programa num canal com arquivo.

## Validação

Compilação conjunta Standard Debug com o Core local concluída com sucesso. **1313 testes unitários passaram**, sem falhas, erros ou testes ignorados: App 195, Core 810, Player Core 308. Inclui nove novas regressões para a janela HLS e a política de histórico do EPG. As verificações de literais da app/Core passaram.

[Registo da validação](C:/Users/renat/Documents/Codex/2026-09-22/c-users-renat-downloads-owntv-main/work/rewind-epg-validation-final.log).

Não foi gerado APK, feito pedido ao fornecedor ou ensaio físico na box. A utilização do recuo e a reposição dos programas devem ser confirmadas no dispositivo com a fonte real.
