# Melhorias futuras pendentes — OwnTV TV

## Regra de consulta

Na próxima análise de **melhorias, correções ou inovações**, consultar este ficheiro e incluir as pendências na avaliação. O registo não autoriza implementação automática.

## UPSCALING-BETA-01 — Upscaling por hardware

- **Registado:** 16/10/2026, a pedido do utilizador.
- **Estado:** ideia futura; pendente de análise de viabilidade, sem implementação.
- **Objetivo:** avaliar a possibilidade de melhorar a apresentação de canais de resolução inferior através de upscaling por hardware, como funcionalidade beta e opcional.
- **Hipótese do utilizador:** possível utilização de buffer; avaliar se tem utilidade neste percurso, sem assumir que aumentar o buffer melhora a resolução ou cria detalhe na imagem.
- **Analisar antes de propor implementação:** distinguir o escalonamento já realizado pelo dispositivo/TV de processamento adicional; verificar APIs e capacidades efetivas nas boxes alvo, integração com ExoPlayer/mpv, custos de GPU/RAM, latência, compatibilidade e sincronismo áudio/vídeo.
- **Condições para uma eventual beta:** desativada por defeito, ativação explícita, possibilidade de voltar ao percurso normal e comparação visual/desempenho nas boxes reais, sem degradar canais que já funcionam bem.
- **Próximo passo:** retomar esta ideia na próxima análise pedida pelo utilizador, decidir se existe benefício real e só depois propor uma fase de implementação.
