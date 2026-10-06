# Predefinições da OwnTV

Valores solicitados em 2026-10-06. Aplicam-se a instalações novas e a preferências sem valor guardado. Uma escolha explicitamente guardada continua a prevalecer após atualização, incluindo as configurações específicas por canal. Não existe uma reposição automática nem uma migração que apague personalizações.

| Área | Predefinição |
|---|---|
| Guia TV | Apenas diferidos ligado |
| Desvio visual do EPG | 0 minutos, desativado |
| Origens EPG | Atualização automática: 6 horas |
| Correção do pedido de programas em diferido | +60 minutos; não altera a hora apresentada no guia |
| Prioridade de versões do mesmo canal | Ligada |
| Alternativas a canais | Ligadas |
| Tempo antes de tentar outra versão | 7 segundos |
| Modo simples | Categorias e barra lateral ocultas |
| Descodificação de vídeo | Hardware ligado |
| HDR | Desligado |
| Apenas HLS | Ligado; exceções guardadas por canal preservadas |
| Atraso face ao direto | Automático, sem impor um desvio personalizado |
| Reserva de reprodução | Equilibrada |
| Desistir do canal | 15 segundos |
| Preferir áudio por software | Automático, sem forçar software |
| Som surround | Apenas estéreo |
| Animações | Desligadas |
| Meteorologia | Desligada |

A atualização EPG conserva o mecanismo existente: seis horas é o limiar de antiguidade verificado no arranque/retoma; não promete uma atualização pontual enquanto a aplicação está fechada ou a box desligada.

As opções guardadas, os grupos e ordens de versões e as personalizações de reprodução por canal mantêm-se. As preferências continuam incluídas nos mecanismos de backup existentes.

Ao selecionar explicitamente uma versão do canal, as alternativas continuam apenas após essa versão na ordem de prioridade, sem regressar ao início. A ordem automática é Full HD → HEVC/HVEC → HD → HQ → normal → Low. Com prioridade ativa, uma ordem manual guardada prevalece. Versões ocultas, outros canais/listas e ligações duplicadas são excluídos. Sem alternativas seguintes, aplica-se o tratamento de falha existente.

Validação: compilação `standardDebug` e testes unitários da aplicação, Core e player-core concluídos com sucesso. Não foi gerado APK nem realizada validação física numa box ou smartphone.
