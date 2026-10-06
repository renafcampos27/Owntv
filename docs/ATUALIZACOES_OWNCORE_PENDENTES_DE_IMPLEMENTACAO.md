# Atualizações OwnCore pendentes de implementação

Última atualização: 06/10/2026.

## Versão upstream de referência

A aplicação personalizada tem referência de equivalência **v5.1.0 de ahXN00/OwnTV**; consultar [referência e pendências da aplicação](ATUALIZACOES_OWNTV_PENDENTES_DE_IMPLEMENTACAO.md) juntamente com este ficheiro nas próximas análises.

**O Core personalizado tem como referência de equivalência a v1.0.64 de ahXN00/OwnTV_Core (projeto ahXN00/OwnTV), registada em 06/10/2026 por indicação do utilizador.** Mantém alterações locais e integração seletiva: esta referência não significa cópia integral ou equivalência de todas as funcionalidades upstream. Nas próximas análises, comparar as novas versões com esta referência, com o código local e com as pendências abaixo.
## Regra para futuras análises

Sempre que se analisar uma nova versão do OwnTV Core, consultar este ficheiro juntamente com o código da nova versão e o código atual do projeto personalizado. Comparar cada pendência, registar o resultado e atualizar o seu estado. Uma nova publicação não autoriza automaticamente a implementação das pendências.

## Implementado — lote 2: recuperação de diferidos sem imagem

**Estado:** autorizado e implementado em 06/10/2026 como lote D da integração OwnTV 5.1.0. Corrigido o ramo sem imagem e reutilizada a troca de motor protegida já existente, respeitando motores exclusivos. Compilação e testes locais aprovados; reprodução física ainda por validar. Ver [implementação C/D/E](IMPLEMENTACAO_OWNTV_5.1.0_LOTES_C_D_E_2026-10-06.md). As propostas e critérios abaixo preservam o histórico da decisão anterior.

**Origem:** análise do OwnTV Core 1.0.62, ramo de recuperação de arquivos/diferidos sem vídeo referido como #229, em `player-core/src/main/java/tv/own/owntv/player/OwnTVPlayer.kt`.

### Problema identificado

Quando o programa diferido foi pedido corretamente, mas não apresenta imagem, o código local já tenta recuperação por descodificação de software. Num ramo em que essa tentativa falha, pode terminar com um diagnóstico inadequado de ficheiro sem fast-start.

### Proposta a reavaliar

- Corrigir a classificação e a mensagem dessa falha específica.
- Avaliar uma única tentativa com o outro motor, apenas quando as preferências de reprodução autorizarem essa troca.
- Se o utilizador escolher exclusivamente ExoPlayer ou mpv, respeitar essa escolha e apresentar o erro correto.
- Preservar a identidade e a geração da reprodução, o cancelamento de tentativas antigas e as proteções de pausa e de áudio sem vídeo; impedir ciclos de recuperação.

Esta proposta não corrige horários do EPG, pedidos que abrem o programa errado, dias de arquivo indisponíveis ou bloqueios do fornecedor.

### Critérios para uma futura decisão

1. Verificar se a nova versão altera este ramo, corrige o diagnóstico ou oferece uma recuperação mais adequada.
2. Comparar com o código local atualizado; não copiar o Core completo nem pressupor que a pendência continua igual.
3. Explicar benefício, riscos e comportamento em modo de motor exclusivo antes de propor a implementação.
4. Se aprovada, testar diferidos com vídeo, sem vídeo e apenas áudio, troca rápida de programa e respeito pelo motor selecionado.

## Contexto dos outros lotes

- **Lotes 3, 4 e 5:** implementados seletivamente em 03/10/2026; consultar o relatório de implementação abaixo para o âmbito e os limites de validação. Não voltar a tratá-los como pendentes sem verificar o código atual.
- **Lote 1:** consta da análise original, mas não foi incluído na autorização de implementação dos lotes 3, 4 e 5. Manter separado da decisão de adiar o lote 2.
- **Lote 6:** proposta opcional na análise original; não incluída na implementação dos lotes 3, 4 e 5.

Preservar nas futuras integrações: HLS estrito e exceções autorizadas por canal, escolha de motor, áudio FFmpeg do ExoPlayer, volume da app a 100%, definições por canal, diferidos, correção horária e controlo de passagem entre canais. Verificar as versões atuais da base de dados e do backup antes de adaptar alterações upstream.

## Documentos de referência

- [Análise crítica do Core 1.0.62](ANALISE_CORE_1.0.62_COUNCIL_2026-10-03.md).
- [Implementação dos lotes 3, 4 e 5](IMPLEMENTACAO_CORE_LOTES_3_4_5_2026-10-03.md).

## Histórico de decisões

| Data | Decisão |
|---|---|
| 04/10/2026 | Utilizador adiou o lote 2 para futura discussão quando existir uma nova versão do Core e pediu este registo para acompanhar as próximas análises. |
| 06/10/2026 | Reavaliadas as versões 1.0.63 e 1.0.64. O ramo #229 não mudou; lote 2 permanece adiado. Recomendada, sem implementação, a declaração de visibilidade de leitores externos da 1.0.63. A barra fixa local já satisfaz o objetivo visual da 1.0.64. |
| 06/10/2026 | Após a comparação da OwnTV 5.1.0, o utilizador autorizou os lotes C/D e a retoma local do E. O lote #229 foi implementado seletivamente; deixou de estar adiado. |

## Revisão mais recente

A autorização mais recente inclui o lote #229, implementado juntamente com a robustez seletiva HLS e a retoma de cópia local. Ver [registo C/D/E](IMPLEMENTACAO_OWNTV_5.1.0_LOTES_C_D_E_2026-10-06.md). Os parágrafos seguintes registam revisões anteriores a esta autorização.

As recomendações locais de EPG foram posteriormente autorizadas e aplicadas em 06/10/2026; consultar [implementação de robustez do EPG e limpeza](IMPLEMENTACAO_EPG_ROBUSTEZ_E_LIMPEZA_2026-10-06.md). Esta autorização não inclui o lote #229 anteriormente adiado.

Consultar [análise das versões 1.0.63 e 1.0.64](ANALISE_CORE_1.0.63_1.0.64_2026-10-06.md). Após autorização do utilizador, a deteção de leitores externos foi implementada em 06/10/2026; manifesto mesclado validado. A análise aprofundada de EPG e limpeza está em [relatório dedicado](EPG_LIMPEZA_CORE_1.0.63_1.0.64_ANALISE_APROFUNDADA_2026-10-06.md); as recomendações EPG foram posteriormente autorizadas e implementadas, conforme o registo acima. Esta revisão não altera as decisões anteriores sobre os lotes 1, 2 ou 6.
