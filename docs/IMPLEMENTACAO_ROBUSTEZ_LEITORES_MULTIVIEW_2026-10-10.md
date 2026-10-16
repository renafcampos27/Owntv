# Robustez dos leitores e Multiview — 10/10/2026

## Implementação

O Multiview foi mantido. A análise da outra IA foi revista antes de escolher as alterações: a gravidade e os ganhos anunciados nesse relatório não constituem prova de comportamento no dispositivo.

1. **Transições de motor:** todas as entradas no Exo verificam a retirada do mpv. Uma confirmação negativa impede a abertura seguinte. Geração e identidade impedem que uma tentativa antiga cancelada abra depois do novo canal. Foi retirado o atraso fixo adicional desse percurso.
2. **Retirada nativa mpv:** a espera é limitada no tempo e exige `idle-active == true` e ausência de `path` da instância capturada. A ausência de `path` sozinha não prova libertação. O cancelamento do chamador não interrompe a limpeza independente. Timeout conserva a quarentena; uma tentativa posterior pode repetir a confirmação.
3. **Pausa:** uma mudança manual de motor aguarda Play quando o motor de origem estava pausado. Uma nova pausa durante a retirada também é respeitada e suspende o orçamento de abertura.
4. **Multiview:** ON_STOP, disposição da composição e saída explícita libertam leitores e claims. A limpeza é idempotente; callbacks antigos não podem voltar a destruir o pool de um novo grid. A restauração após recriação não recupera um fullscreen sem o respetivo grid.
5. **Ficheiros SAF:** descritores pertencem à instância nativa e só fecham após confirmação de unload ou conclusão de destroy. Um limite de duas leases impede crescimento ilimitado. Não se fecha um descritor só porque chegou um END_FILE sem identidade fiável.
6. **Lista:** cálculo de limites visíveis sem criar uma lista de índices a cada deslocação; preserva overscan, Paging e canal em preview. Animação de fundo das superfícies sólidas lida na fase de desenho; superfícies de vidro preservam o seu percurso anterior.
7. **Ver do início:** botão no HUD de direto para programa atual elegível com pelo menos um minuto e dentro da janela de arquivo anunciada. Reutiliza o callback de diferidos e verifica novamente elegibilidade ao clicar. Não cria pedidos EPG adicionais.

## Decisões conservadoras

- Mantida a espera legítima de pausa em `awaitMpvOutcome`; não ficou demonstrado um loop infinito terminal que justificasse remover esse comportamento.
- Sem flags NON-IDR globais, alterações globais de buffer, novos atrasos obrigatórios ou reinício do leitor em todos os canais.
- Sem índice SQL, tamanho fixo global de logótipos, duplicação de prefetch EPG ou novo painel de diagnóstico neste lote.
- Mantidas as preferências de volume e os mecanismos anteriores de reabertura recente. Não alterada a app Mobile.

## Validação

Validação integrada da revisão final concluída: **BUILD SUCCESSFUL**. Compilação Kotlin da app StandardDebug, testes da app (327), Core (1123) e player-core (462): **1912 testes, zero falhas, erros ou testes ignorados** nos relatórios XML. Os testes Core sem alterações foram reutilizados pelo Gradle; os testes da app e player-core correram na revisão final.

Inventários de literais/traduções da app e Core aprovados. Classificados apenas os novos identificadores de protocolo e mensagens de invariantes técnicas, sem acrescentar frases inglesas visíveis na interface. Verificação de whitespace dos ficheiros alterados deste lote aprovada. O build anterior indicou depreciações de Gradle já presentes; a revisão final não apresentou o aviso Kotlin da condição redundante corrigida.

Testes novos: oito cenários do controlador (retirada pendente/negativa, cancelamento, pausa e troca de motor), seis cenários de prova nativa/leases e dois de elegibilidade do reinício do programa.

## Limites e testes físicos

A API JNI de command devolve Unit: não permite recuperar um código de erro nativo descartado pelo JNI. O estado observado confirma a retirada local, não a libertação da sessão pelo servidor IPTV. Nenhum bloqueio Kotlin é mantido durante stop, leitura de propriedades ou destroy, para não impedir a destruição de um núcleo bloqueado. A concorrência de operações nativas já em execução depende do contrato da biblioteca; confirmações antigas são recusadas por identidade. Testes unitários não executam JNI, codecs ou lifecycle real de uma box. Não foi gerado APK.

Na box, testar A→B→C→A repetidamente, pausas e troca manual de motor, Home com Multiview ativo e retorno, reprodução de ficheiros SAF sucessivos e Ver do início em canais com arquivo. Confirmar foco, áudio e fluidez visual. Uma falha exclusiva de Exo ou um bloqueio do fornecedor pode precisar de diagnóstico separado.
