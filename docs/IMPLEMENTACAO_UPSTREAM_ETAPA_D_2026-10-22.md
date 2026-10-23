# OwnTV — etapa D: áudio por saída, SDR e timeshift após suspensão

Data: 22/10/2026. Estado: implementada e validada automaticamente; aceitação física na box pendente.

A etapa D foi autorizada expressamente após a explicação das propostas C01/C02/C03. Não executa nem renumera as fases 3–6 do plano principal. O Core personalizado, motores escolhidos, buffers, intervalo de reabertura, HLS obrigatório/exceções, volume interno de 100%, EPG e correção dos diferidos foram preservados.

## C01 — Sincronização por família de saída

Em **Definições → Reprodutor de vídeo → Som** existem duas novas opções:

- **Sincronização do áudio por saída**: correções independentes para HDMI/TV/soundbar, Bluetooth, cabo/jack, USB e altifalante integrado. Todas começam em **0 ms**. Ajuste em passos de 25 ms, de −5 000 a +5 000 ms; valores positivos atrasam o som.
- **Saída usada para corrigir o áudio**: Automático por defeito; permite escolher manualmente uma família quando a deteção da box é ambígua.

O ajuste adicional é somado **uma vez** ao valor global ou ao valor próprio do canal/conteúdo, limitado ao intervalo suportado de ±5 s. O HUD e as preferências por conteúdo continuam a guardar o valor base, sem incorporar a saída; trocar Bluetooth → HDMI → Bluetooth não acumula correções. O valor efetivo chega ao relógio de áudio do ExoPlayer e ao `audio-delay` do mpv. As leituras de diagnóstico identificam origem/base/adicional/total quando há correção não nula.

No Android 13+ consulta-se a saída **prevista para multimédia** para atributos de filme. Esta API não é confirmação física do cabo nem do DAC em utilização. Em Android anterior, apenas uma família externa ligada inequívoca é aceite como estimativa. HDMI e Bluetooth presentes em simultâneo ou uma saída desconhecida resultam em saída não identificada e correção adicional zero em Automático. Pode usar-se a escolha manual do perfil de correção; não muda a saída física escolhida no Android. HDMI não distingue a TV de uma soundbar ligada através dela; Bluetooth não identifica individualmente cada auscultador. Não se guardam endereços ou identificadores físicos.

A consulta periódica de encaminhamento só existe quando há correções não nulas e o modo é Automático; callbacks/consulta são cancelados com o scope dos leitores. Com os valores iniciais não se acrescenta polling de saída. As novas preferências integram o backup/restauro, com validação de tipos, enum e limites antes de qualquer alteração. Defaults explícitos/unsets da política de snapshot continuam compatíveis. Não se alterou o formato da base de dados ou a versão do backup.

Não corrige cortes de áudio, rede, ruído analógico nem a latência própria do Bluetooth; permite compensar sincronismo A/V.

## C02 — Preferir SDR no HLS do ExoPlayer

Nova opção em **Reprodutor de vídeo → Motor e imagem → Preferir SDR (HLS · ExoPlayer)**, desligada por defeito. A opção antiga de HDR continua separada, com o seu significado anterior de indicação de cores ao mpv; nenhuma preferência guardada foi reinterpretada.

A preferência nova aplica-se na próxima abertura. Reutiliza o pedido e parser Media3 existentes, observando uma única passagem da resposta. Captura no máximo **256 KiB**; ultrapassar esse limite desativa a filtragem para esse manifesto, sem truncar os dados entregues ao parser. Não existe leitura integral sem limite nem outro pedido HTTP para descobrir SDR.

Só se retiram URLs identificados exclusivamente como PQ/HLG se continuar uma variante explicitamente SDR com vídeo identificável. Manifestações só HDR, só SDR, desconhecidas ou URLs contraditórios são preservadas. Áudio separado, legendas, DRM, timeouts, legendas CEA e parâmetros de compatibilidade por canal mantêm o percurso existente. Manifestos com variáveis, content steering ou rendições alternativas de vídeo não são filtrados nesta versão conservadora. Conteúdos com legendas externas seguem a fábrica de fusão original do Media3.

Aplica-se a HLS no ExoPlayer de direto e ao percurso ExoPlayer de arquivos/gravações elegíveis. **Não converte HDR para SDR, não força o ExoPlayer, não muda mpv ou leitores externos e não cria versões SDR ausentes.** A regra não garante que o fornecedor descreva corretamente as variantes nem que a variante SDR seja descodificável em todo o hardware; a disponibilidade real continua a depender do serviço e do aparelho.

## C03 — Correções confirmadas no timeshift temporário

A proteção de posições já usava `elapsedRealtime`, contando suspensão, e as descontinuidades por sequência/reconexão já existiam. Não foi importado outro gestor de timeshift nem duplicado esse tratamento.

Foram confirmadas e corrigidas três lacunas:

1. A tarefa de expiração usava apenas uma espera que pode ficar atrasada em deep sleep. A validade é agora reavaliada ao regressar à app e antes de oferecer a retoma, além da verificação periódica por relógio decorrido.
2. Voltar a estacionar uma sessão já parada podia renovar o início dos 30 minutos. Agora conserva o instante original dessa cópia e apenas atualiza a posição.
3. A cópia podia expirar entre a apresentação da pergunta e o fim da preparação do leitor. A posição/identidade são verificadas novamente depois das esperas; se não for possível retomar, volta ao direto em vez de sair silenciosamente da rotina e deixar a reprodução sem avançar.

Se a pergunta de retoma expirar enquanto a app está visível, regressa ao direto; em segundo plano não abre nova ligação e só considera essa alternativa ao regressar, se perfil/fonte/canal ainda coincidirem e não existir arranque num canal fixo ou abertura por ligação externa.

O fecho da cópia expirada aguarda a limpeza anterior em IO e não bloqueia a interface. Só é apagada a sessão temporária estacionada, nunca uma gravação guardada, um download intencional ou uma sessão local que esteja em uso. Mantêm-se os limites existentes: 30 minutos de retenção temporária, 256 MiB e reserva de armazenamento da sessão. A mudança da hora civil/fuso não altera essa validade.

## Segurança e validação

- Rollback anterior à etapa: `C:\Users\renat\Downloads\OwnTV-upstream-D-backup-20261022`, com arquivos App/Core, lock/snapshot anterior e manifesto SHA-256 conferidos. Não substitui o backup dos dados da box.
- Ponto de partida: snapshot anterior `6eabcd44c622925b2f505a10559a4320606e08113ac57728d941f7feeaa3c1f4`, 1 039 inputs, zero diferenças para o Core externo.
- Snapshot desta etapa: `eb701f07d10fe9fd843faba4ed0b474b0100d6b8570480fb3449ab08a6ea4928`, **1 047 inputs**, hash e bytes conferidos com os fontes.
- Core: **1 335 testes JVM** aprovados; leitores: **598**, sem falhas/erros/ignorados. **15 testes novos**: 5 de preferências/backup/validade e 10 de composição de atrasos, classificação conservadora de saídas e seleção/captura limitada do manifesto.
- Compilação dos testes instrumentados do Core aprovada; não foram executados num aparelho.
- Ferramenta de snapshot: **6 testes aprovados**.
- App: compilação `standardDebug` e `standardRelease`, **429 testes JVM**, lint vital de release e preparação das bibliotecas nativas aprovados, usando o snapshot definitivo. Total: **2 362 testes JVM + 6 testes da ferramenta de snapshot**, sem falhas/erros/ignorados.
- Configuração normal de build (`help`, sem forçar snapshot nem desativar a verificação do Core) aprovada. O Android Studio tem acesso às mesmas alterações/snapshot.
- Verificação de textos: Core **263 baseline / 5 783 declarações técnicas**; App **51 / 1 088**. Sem aumento da dívida de tradução. Recursos novos em inglês e português/PT-PT.
- Logs finais em `%TEMP%`: `owntv-upstream-d-core.log`, `owntv-upstream-d-app-final.log`, `owntv-upstream-d-normal-config.log`.
- Avisos preexistentes de testes e de funcionalidades Gradle obsoletas continuam fora deste âmbito; não se desativou o lint nem se criou uma baseline para esconder erros.
- Sem APK, instalação, assinatura, push, teste físico de saída de áudio ou reprodução com fornecedor real. Sem ganhos percentuais medidos.

## Aceitação na box

1. Testar HDMI 0 ms/Bluetooth +150 ms, alternar saídas e confirmar que global/canal guardado não ganha mais 150 ms a cada troca; verificar ExoPlayer e mpv e um reinício/backup-restauro. Nas versões antigas ou com encaminhamento ambíguo usar a seleção manual se necessário.
2. Comparar um HLS com variantes explícitas SDR/HDR ao abrir com a opção desligada/ligada. Confirmar que HDR-only, canais sem manifesto multivariante e preferências de motor continuam a funcionar. A aplicação só promete esta seleção dentro dos limites apresentados na definição.
3. Pausar/recuar o direto local, sair/suspender mais de 30 minutos e regressar; a cópia estacionada deve expirar. Repetir antes do prazo e confirmar a posição correta. Deixar a pergunta de retoma aberta até expirar e confirmar regresso ao direto. Conferir que as gravações guardadas permanecem.

## Referências primárias consultadas

- [Android — getAudioDevicesForAttributes, API 33](https://developer.android.com/reference/android/media/AudioManager#getAudioDevicesForAttributes(android.media.AudioAttributes)): encaminhamento previsto para os atributos fornecidos.
- [Media3 — HlsPlaylistParserFactory](https://developer.android.com/reference/androidx/media3/exoplayer/hls/playlist/HlsPlaylistParserFactory) e [HlsMultivariantPlaylist](https://developer.android.com/reference/androidx/media3/exoplayer/hls/playlist/HlsMultivariantPlaylist).
- Fontes locais do Media3 1.11.1 e Core upstream 1.0.69 consultados para integração seletiva; não foram substituídos ficheiros personalizados por ficheiros upstream completos.
