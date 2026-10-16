# Guia e diferidos — 10/10/2026

## Sintomas comunicados

- Ao recuar com a seta esquerda, a linha do guia fica vazia e pode recuperar depois de navegar para a direita.
- Vários programas diferidos falham com HTTP 404 / IO bad HTTP status.

## Correções

### Navegação do guia

A mudança do bloco de leitura chamava a invalidação global da cache, cancelando leituras partilhadas e eliminando blocos anteriores. Os dados da linha eram também reinicializados a null ao mudar de bloco.

Agora a navegação conserva blocos completos e cancela apenas leituras pendentes de janelas obsoletas e o prefetch antigo. Uma leitura cancelada que termina mais tarde já não remove uma nova leitura da mesma chave. As chaves já incluem a janela; os limites LRU existentes de linhas e programas continuam a limitar memória. Sincronização, mudança de perfil e alterações de horários continuam a invalidar dados como antes. A linha conserva a resposta anterior do mesmo canal enquanto aguarda a nova; o desenho continua a recortar ao viewport. Não se juntam programas de canais diferentes nem se carrega todo o histórico.

Foi também corrigida a leitura parcial no Core: ter alguns programas guardados dentro da janela já não impede a leitura do trecho histórico anterior que falta. A consulta complementar só se aplica a Xtream com arquivo, no prefixo histórico em falta; não faz consultas de arquivo para lacunas atuais/futuras. Se o fornecedor falhar, os dados guardados continuam disponíveis como fallback, sem guardar essa falha como uma leitura completa.

### Diferidos ExoPlayer

O mpv já tinha um percurso alternativo Xtream de path timeshift para timeshift.php. O callback do Exo descartava o código HTTP na falha e encaminhava diretamente para a falha/troca de motor. Em Exo-only, esse percurso alternativo não era tentado.

Acrescentada a tentativa do endereço PHP do mesmo arquivo, mantendo canal, duração e início. É exclusiva de arquivo Xtream reconhecido, HTTP 404, antes da primeira imagem e uma vez por item. Não altera streams de direto nem responde a 401/403/410/429. O leitor Exo é parado antes da reabertura e usa o percurso de recriação de superfície existente, com identidade de geração. Se o endereço alternativo também falhar, aplica-se o tratamento de erro normal. Não existe fallback para direto.

## Validação

Concluída a validação da revisão final: **BUILD SUCCESSFUL**. Compilação StandardRelease, lint vital Release e testes aprovados. Relatórios XML: app 329, Core 1125, player-core 464; **1918 testes, zero falhas, erros ou ignorados**. Os seis testes novos cobrem preservação de blocos adjacentes, cancelamento com regresso rápido à mesma janela, política da alternativa Xtream (identidade, tentativa única e exclusões) e limites da consulta complementar do histórico. Inventários de tradução e verificação de whitespace dos ficheiros deste lote aprovados.

## Limites

Não foram recolhidos pedidos HTTP reais do fornecedor nem reproduzida a navegação numa box. Uma grelha com dados EPG não comprova que o fornecedor conserva o vídeo. Um 404 persistente pode significar arquivo ausente, janela anunciada incorreta ou horário pedido inválido. A correção horária de diferidos configurada pelo utilizador foi preservada; não foram feitas tentativas automáticas com horas diferentes. Não foi gerado APK.
