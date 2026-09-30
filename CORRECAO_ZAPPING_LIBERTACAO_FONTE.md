# Zapping: libertar a fonte antes de esperar pelo fecho HTTP

## Alteração
Em LivePreviewEngine.reprepare, a ordem passa a ser: invalidar a geração anterior e cancelar pedidos; cancelar temporizadores/preparação pendente; parar o ExoPlayer e retirar os media items antigos; aguardar o término dos pedidos anteriores; verificar que a seleção ainda é atual; instalar e preparar uma nova fonte.

Antes, a fonte antiga permanecia instalada enquanto se esperava pelo fim dos pedidos. Cancelar o transporte não substitui a libertação dos leitores da fonte. O player é reutilizado, mas a fonte é retirada em cada nova preparação, incluindo retries.

Mantêm-se a espera limitada a 1,5 s, rejeição de callbacks antigos e cancelamento da abertura quando o utilizador escolhe outro canal. Não se alteraram HLS-only, latência, buffers ou configuração do descodificador.

## Validação
Novo teste de transporte com servidor HTTP local e 30 aberturas alternadas de três URLs. Cada resposta permanece incompleta; o teste cancela e fecha o corpo anterior e confirma que os pedidos anteriores terminaram antes de abrir o seguinte. Executado juntamente com os testes do player e compilação Kotlin da app (consultar work/zap-source-release/build.log no workspace).

Este teste valida o transporte e a barreira de fecho, não executa o ExoPlayer Android nem confirma a libertação da sessão no fornecedor.

## Teste na box
Repetir A-B-C-A-B-C pelo menos 30 mudanças, com intervalos de 1 a 5 segundos, primeiro pelas setas em fullscreen e depois pela lista. Confirmar imagem e som em cada abertura. Se persistir, exportar o diagnóstico detalhado: source_drain_wait, source_drain_timeout, prepare, http_start/end/failed e first_frame permitem distinguir a espera de fecho da falha na nova abertura.

## Limite
Esta é uma correção da sequência local, não uma confirmação de que a causa observada na box está resolvida. A paragem explícita pode alterar o tempo de zapping; comparar na box. Os engasgos do canal atribuídos ao fornecedor ficam fora deste trabalho.
