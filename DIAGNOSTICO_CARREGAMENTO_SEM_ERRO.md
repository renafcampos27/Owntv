# Diagnóstico de carregamento sem erro

## Alteração

A lista de erros formais não mostrava a atividade do diagnóstico. A exportação incluía a memória da sessão atual, mas não lia o ficheiro persistido. Estas limitações foram confirmadas no código; não demonstram por si só a causa do bloqueio na box.

- Inicialização do destino de diagnóstico no arranque da aplicação, antes de abrir um player.
- Evento de ativação/desativação do diagnóstico.
- Amostra a cada 2 segundos, apenas com diagnóstico ligado e transmissão selecionada: identidade da sintonia/fonte, estados da app e ExoPlayer, buffering, intenção de reproduzir, posição, buffer em ms, contador de fotogramas, validade da superfície, sintonia suspensa, preparação pendente e pedidos HTTP ativos.
- Amostragem independente dos temporizadores de recuperação: mantém visibilidade quando estes estão suspensos. Usa o scope do player e termina ao libertá-lo. Não regista URL, utilizador, senha ou tokens nas novas amostras.
- Exportação inclui histórico em disco e memória atual em secções identificadas; pode haver sobreposição. Leitura limitada ao tamanho máximo do histórico, em IO, sincronizada com a escrita.
- O ecrã do registo mostra diagnóstico ligado/desligado e contagem de eventos recentes, mesmo quando não há erros formais. O contador é limitado à janela de 1000 eventos, não é um total acumulado.

Esta entrega altera observabilidade, não a lógica de sintonia, buffers ou recuperação.

## Teste na box

Gerar e instalar novo APK. Ativar Reprodutor de vídeo → Diagnóstico → Registo detalhado de reprodução. Abrir um canal e confirmar no registo que o diagnóstico está ligado e existem eventos. Reproduzir a falha, aguardar alguns segundos e exportar. Enviar o ficheiro no caminho indicado e o canal/hora aproximada. Se a gravação continuar ausente, enviar fotografia do novo estado/contador e do resultado da exportação.

O histórico já escrito é incluído mesmo depois de reiniciar a app; os últimos eventos ainda em fila podem perder-se se o processo terminar abruptamente. A memória atual é incluída imediatamente, sem esperar pela fila de disco.

## Validação

Testes adicionados para recuperar eventos persistidos sem depender da memória da sessão, leitura limitada por bytes, preservação de linhas UTF-8 e ficheiro inexistente. A validação de exportação através do MediaStore e o bloqueio na Xiaomi/Thomson dependem de teste num dispositivo real.

Compilação concluída; 250 testes do player-core e 179 da app passaram, sem falhas ou testes ignorados. APK não gerado.
