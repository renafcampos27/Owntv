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


## TV-AVO-01 — Modo «TV Avó» — implementado

- **Registado e autorizado:** 24/10/2026; inicialmente como ideia futura, depois autorizado explicitamente pelo utilizador.
- **Estado:** implementado e validado automaticamente; teste físico nas boxes pendente. Já não é uma implementação futura por iniciar.
- **Resultado:** leitor interno apenas com nome do canal e programação, sem botão de play/pausa; restantes controlos, cronologia, detalhes técnicos e atalhos avançados ocultos. Indicador de carregamento/mensagem curta de falha mantidos.
- **Comando:** cima/baixo e CH mantêm o zapping, mesmo com play focado. Um toque em Voltar regressa à lista. OK mostra a informação sem pausar. Ao ativar, liga agrupamento, prioridade das versões e troca automática entre qualidades; preserva ordens manuais e o tempo configurado.
- **Ativação:** opcional, desligado por defeito, em Definições → Esquema → Personalização do menu lateral → Modo TV Avó; também pesquisável nas definições. Desligar a mesma opção recupera o leitor completo.
- **Persistência:** DataStore existente e backup completo; preparação também no arranque e por perfil. Desligar o modo não reverte automaticamente as opções de canais. Testes de reabertura, restauro e aplicação do preset passaram.
- **Preservado:** motores, ligações/buffers, volume da box, gravações, diferidos e preferências existentes. Nenhum APK gerado nesta implementação.
- **Evidência e testes físicos a fazer:** [Implementação TV Avó](IMPLEMENTACAO_TV_AVO_2026-10-24.md).
