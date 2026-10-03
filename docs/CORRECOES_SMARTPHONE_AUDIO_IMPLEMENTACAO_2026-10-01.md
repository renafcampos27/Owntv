# Correções de smartphone, reprodução e áudio — implementação

Data: 2026-10-01. Implementação da auditoria `AUDITORIA_SMARTPHONE_AUDIO_2026-10-01.md` no projeto OwnTV-main e no Core local utilizado pelo Gradle (`C:/Users/renat/Downloads/OwnTV_Core`).

## Resultado por problema

| Auditoria | Implementado | Proteção relevante |
|---|---|---|
| A1 — aviso de erro durante reprodução | Recuperação comprovada da fonte atual limpa erro, detalhe e desistência; um sucesso tardio também pode fazê-lo sem recarregar | READY sozinho não conta; fonte antiga, erro atual do ExoPlayer e imagem congelada não comprovam sucesso |
| A2 — sucesso versus limite de tempo | Verificação final da reprodução antes de desistir ou trocar de tentativa; abertura comunicada uma vez por tentativa | Mantém limites para canais que realmente não abrem e cancelamento das escolhas anteriores |
| A3 — áudio reutilizado | Mudança de formato conserva avanço válido; criação de nova saída rearma a deteção; PCM/passthrough segue a configuração do AudioTrack | Mantém recuperação por erro real da saída e por saída nova que não avança |
| A4 — recuperação sem reserva | Live exige reprodução e pelo menos 2 s de dados antes de atribuir underruns repetidos a passthrough | PCM, falta de media e interrupções isoladas não provocam este reinício |
| A5 — processamento síncrono imposto | Opção persistente Automático / Assíncrono / Compatibilidade síncrona nos três ExoPlayers | Aplica na próxima abertura; Automático respeita Media3; compatibilidade permite comparação e retorno |
| A6 — diagnóstico e foco | Eventos de formato/reutilização, decoder, saída, falta de dados, timestamps, velocidade, foco e alterações das saídas disponíveis | Dados de contexto do Live incluem engine/tune/source, reserva e velocidade; sem URL nova nos eventos, escrita limitada e fora da UI pelo mecanismo existente |
| A7 — Back em ecrã compacto | Layout compacto e modo simples têm decisões de regresso separadas | Definições regressam a Mais; outras páginas compactas regressam aos canais; modo simples mantém o menu rápido |
| A8 — rotação | MainActivity gere background real; recriação preserva recuperação e timeshift local | Home continua a libertar reprodução; troca de perfil limpa a sessão; disposição da shell cancela recuperação apenas fora de recriação |
| A9 — áudio de telefone | Classificação pelas capacidades do dispositivo escolhe PAUSE e pausa ao desligar a saída nos smartphones | TV mantém DUCK; escolha independente de largura de janela e ARMv7/ARMv8 |

## Nova configuração

**Definições → Reprodutor de vídeo → Motor e imagem → Processamento do ExoPlayer**.

- **Automático**, predefinido: utiliza o comportamento da biblioteca para a versão do Android.
- **Assíncrono**: permite ensaio explícito do caminho assíncrono.
- **Compatibilidade síncrona**: reproduz a política anterior quando o modo automático/assíncrono apresentar regressões.

A escolha aparece também na pesquisa e pode ser fixada às opções rápidas. É guardada em DataStore, incluída na exportação/restauro das definições e carregada no snapshot de reprodução. Valores ausentes/desconhecidos usam Automático. A construção do reprodutor só muda na abertura seguinte, sem interromper um canal saudável.

Não se alteraram qualidade, formato HLS, latência, pré-buffer ou reserva definidos pelo utilizador. Não foi gerado um APK nem publicado código nesta tarefa.

## Validação automatizada

- Compilação Kotlin da app StandardDebug e de player-core concluída.
- Testes: app **267**, Core **939**, player-core **327**; total **1 533**, sem falhas, erros ou testes ignorados.
- Regressões novas: recuperação/falha e evidência atual, ausência de primeira imagem, imagem congelada, rádio, sucesso no limite do tempo, erro anterior após recuperação, sucesso de canal anterior, formato com saída reutilizada, PCM sem callback de decoder, nova saída sem avanço, underruns espaçados, libertação atrasada, callbacks antigos, foco e Back.
- Inventários de internacionalização verificados pelo build; texto da opção em inglês e português, chaves e eventos técnicos classificados.
- Android Lint do player-core concluído: **0 erros e 5 avisos**.
- Android Lint da app concluído, mas **não aprovado**: **4 erros, 116 avisos e 32 sugestões**. Os quatro erros estão em trechos anteriores a este lote, confirmados na cópia prévia: três ocorrências de `RestrictedApi` no override/chamada de `dispatchKeyEvent` de MainActivity e uma de `LocalContextGetResourceValueCall` no texto da notificação do EPG. Não foram suprimidos nem tratados como aprovação.
- A primeira execução integral de Lint ficou demoradamente na regra `UElementAsPsi`. A repetição excluiu apenas essa regra através de um script temporário desta execução; nenhuma regra do projeto foi desativada permanentemente. Por isso, também não se declara aprovação integral de todas as regras de Lint.

Relatórios de análise estática: `app/build/reports/lint-results-standardDebug.html` e `OwnTV_Core/player-core/build/reports/lint-results-debug.html`. Log da execução final: `C:/Users/renat/AppData/Local/Temp/owntv-playback-validated.log`. Cópia do código antes das alterações: `C:/Users/renat/AppData/Local/Temp/owntv-playback-before-20261001-230013`.

Os testes de unidade verificam políticas, callbacks de áudio e sequenciação com tempo controlado. Não reproduzem um canal de um fornecedor nem medem o equipamento real.

## Aceitação no equipamento

1. Smartphone: abrir A → B → C → A várias vezes, com HLS e ExoPlayer; reprodução saudável não deve conservar o aviso anterior. Uma falha real deve continuar visível e permitir repetir.
2. Rodar durante abertura, reprodução e timeshift local. Confirmar que o canal e a sessão local permanecem; Home e troca de perfil devem parar a reprodução e libertar a sessão.
3. Back: testar Definições → Mais → canais, saída na raiz e menu do modo simples.
4. Smartphone: testar perda/retoma de foco, pausa manual e retirada de auscultadores/Bluetooth. Não deve reproduzir involuntariamente no altifalante após retirada da saída.
5. Box: observar o mesmo canal e saída durante 30–60 minutos com diagnóstico detalhado. Comparar Automático e Compatibilidade síncrona, sem mudar outras opções; Assíncrono fica disponível para comparação explícita.
6. Se persistir um corte, exportar o diagnóstico. Separar reserva esgotada, underrun com reserva, descontinuidade de timestamps, recriação de saída e perda de foco.

Os tipos de saída registados são os dispositivos **disponíveis**, não prova da rota física efetiva do AudioTrack. Não foi demonstrado que as correções eliminem todos os cortes de 0,2 s relatados; isso exige registos do instante do corte e teste na Xiaomi/Thomson.
