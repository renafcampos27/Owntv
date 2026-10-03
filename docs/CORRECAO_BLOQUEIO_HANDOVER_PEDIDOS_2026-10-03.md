# Correção do bloqueio interno na abertura de canais
> Atualização: a confirmação de cancelamento foi reforçada após revisão independente. Ver [Council e implementação final](COUNCIL_HANDOVER_HLS_CANCELAMENTO_2026-10-03.md). A descrição abaixo regista a primeira correção.

Mensagem confirmada pelo utilizador: `Previous stream requests did not finish within the handover deadline`.

## Defeito e âmbito

O percurso ExoPlayer aguardava até 1500 ms por um contador alimentado pelos eventos finais HTTP. Cancelar um pedido/fechar a resposta e receber a notificação final são etapas diferentes. Por exemplo, uma resolução DNS ainda bloqueada pode atrasar o evento final depois do cancelamento, mesmo sem existir uma ligação utilizável. O contador antigo impedia a abertura do canal seguinte e podia afetar outras tentativas enquanto esse evento não chegasse.

Reproduzido em teste local com DNS deliberadamente atrasado, pedidos reais OkHttp e servidor local. A causa particular do atraso na Xiaomi não foi observada: sem registos do dispositivo, não se conclui que tenha sido DNS.

## Alteração

- PlaybackHttpCalls confirma cancelamento e fecho local dos recursos que possui. As entradas só são retiradas dessa proteção após cancelamento confirmado e fecho sem exceção, ou após evento terminal real.
- O bloqueio de abertura passa a verificar os recursos antigos ainda por fechar, em vez do contador de eventos usado nos diagnósticos.
- Mantidos os contadores HTTP para observação, incluindo notificações tardias.
- Retiradas operações de fecho concorrentes através de um monitor separado, sem segurar o monitor do estado durante callbacks HTTP.
- As factories antigas continuam invalidadas; respostas tardias de chamadas canceladas continuam rejeitadas.
- Aplicado à abertura/reabertura ExoPlayer e ao fecho antes de passagem para outro motor.
- Sem alterar HLS apenas, buffers, latência, volume, ordem das versões, EPG ou configurações.

O fecho local não confirma o instante em que o servidor libertou a sua sessão. Um verdadeiro 403/429 continua sujeito ao tratamento próprio, não é ignorado por esta correção.

## Validação

- Novo canal no mesmo servidor abre enquanto o evento final do pedido antigo está deliberadamente atrasado. O servidor recebe o pedido novo, sem receber o pedido antigo cancelado.
- Falha simulada no cancelamento mantém a proteção; tentativa posterior de fecho bem-sucedida liberta-a.
- Mantidos testes de 30 alternâncias entre variantes com corpos reais de streaming abertos, fábricas antigas, respostas tardias e isolamento entre motores.
- Compilação StandardDebug da app: passou.
- App: 306 testes; falhas=0, erros=0, ignorados=0.
- Leitor: 365 testes; falhas=0, erros=0, ignorados=0.
- Registo: `%TEMP%/owntv-handover-local-close-final.log`.
- Backup prévio dos três ficheiros alterados: `%TEMP%/owntv-handover-local-close-20261003`.

Referência técnica: [implementação oficial de cancelamento do OkHttp 5.5.0](https://github.com/square/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RealCall.kt).

## Verificação no dispositivo

Gerar novo APK no Android Studio com o Core local associado e instalá-lo. Fazer Forçar paragem uma vez, voltar a abrir e testar A → B → C → A durante várias alternâncias rápidas e lentas; depois Home → reabertura. Não apagar dados/configurações. Não foi gerado APK nem houve teste físico nesta execução.

Se existir outra falha, distinguir a mensagem: erro interno de fecho, recusa HTTP efetiva do fornecedor ou ausência de dados. Os testes demonstram a correção deste bloqueio, não garantem a eliminação de todas as causas de falta de reprodução.
