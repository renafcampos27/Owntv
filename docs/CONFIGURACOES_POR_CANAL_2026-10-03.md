# Configurações de reprodução por canal

Implementação concluída em 03/10/2026.

## Onde encontrar

Definições → Reprodutor de vídeo → Motor e imagem → Configurações por canal.

Selecionar lista, procurar canal, abrir o editor, escolher os ajustes e Guardar. Voltar a abrir o canal para aplicar. Cancelar não guarda alterações. Repor definições gerais/lista prepara a remoção dos ajustes; confirmar em Guardar.

Este ecrã substitui Exceções ao modo Apenas HLS. As escolhas TS antigas são apresentadas sem exigir nova seleção.

## Opções

- Motor: herdar, ExoPlayer ou mpv. HLS exclusivo mantém ExoPlayer por defeito. Selecionar mpv explicitamente neste canal mantém a ligação HLS e impede a alternativa TS direta.
- Formato: herdar, apenas HLS, TS direto ou permitir outros formatos apenas neste canal.
- Reserva: herdar ou 1–60 s.
- Margem adicional: herdar ou 0–10 s. No ExoPlayer, o máximo de duração deriva da reserva efetiva mais esta margem; o pré-buffer pode elevar a reserva mínima. Não é um limite de RAM nem uma garantia de disponibilidade de segmentos.
- Pré-buffer inicial: herdar, automático (0) ou 1–10 s. O limiar de retoma após rebuffer mantém-se independente e imediato quando existe conteúdo reproduzível.
- Latência do direto: herdar ou alvo de 1–60 s. Tem efeito em HLS/DASH no ExoPlayer; não afeta TS direto e não garante a distância real ao direto.
- Áudio por software (ExoPlayer): herdar, automático Android/FFmpeg ou preferir FFmpeg. Não altera a descodificação de vídeo nem o motor mpv.
- Compatibilidade HLS (ExoPlayer): detetar limites dos fotogramas H.264/TS; aceitar fotogramas-chave não IDR; descobrir faixas pelos segmentos. Três escolhas independentes, desligadas por defeito. As duas primeiras não se aplicam a fMP4/HEVC. A primeira exige mais processamento; a segunda pode causar corrupção temporária ao abrir/recuar; a terceira aumenta o arranque e não corrige genericamente CODECS incorreto. Não ativar automaticamente pelo nome do canal.
- Sincronização de áudio: herdar ou −5000 a +5000 ms, em passos de 50 ms. Valor explícito tem prioridade sobre o atraso memorizado no HUD; herdar conserva o percurso de preferências existente. Não resolve buffering.

Não existe volume por canal. O volume interno mantém-se a 100%.

## Aplicação e isolamento

A identidade inclui fonte e identificador remoto, com nome como fallback quando não há identificador. Cada versão do canal tem a sua referência. Nenhuma escolha usa apenas um nome transversal a todas as listas.

O controlador carrega as escolhas antes da sintonia. Buffer/pré-buffer/latência por canal têm prioridade sobre os valores da lista, que por sua vez seguem os valores globais. O pedido inclui um snapshot de opções e os valores resolvidos. O ExoPlayer recebe as escolhas de áudio/formato antes de criar ou reutilizar o player. O mpv recebe reserva, pré-buffer e sincronização através do pedido de reprodução.

Reconexões, reconstruções e retorno do segundo plano preservam o snapshot. Um novo canal volta a resolver as próprias escolhas. Um preview com opções diferentes não é promovido sem reaplicar a configuração. Multiview resolve as opções de cada tile independentemente.

O leitor local de timeshift conserva HLS e não herda a exceção de ligação ao fornecedor. Endereços Xtream TS/HLS são convertidos apenas por sufixo, mantendo parâmetros. Não são fabricadas alternativas para URLs arbitrárias assinadas.

## Persistência e backup

DataStore, chave channel_playback_configs: fonte/identificador/nome e apenas os valores personalizados. Sem credenciais ou URLs de reprodução. Sem migração SQLite.

O backup completo inclui channelPlaybackConfigs na secção de configurações, limitado às fontes dos perfis exportados. O restauro adapta sourceId ao mapa de fontes; referências sem correspondência não são aplicadas. As antigas manualTsChannels continuam legíveis. Editar ou repor uma configuração migra/remove a exceção antiga correspondente de forma atómica.

## Validação

Compilação StandardDebug concluída; 307 testes da app, 401 do player-core e 1005 do core, sem falhas ou testes ignorados. Testes cobrem persistência JSON, zero/false explícitos, limites válidos de buffer, migração de exceções TS, remapeamento de fontes, motor/formato, transporte dos valores ao pedido e ausência de contaminação no canal seguinte. Incluem também rejeição da promoção de um preview com configuração antiga.

Não foi gerado um novo APK e não houve teste físico do Odisseia. Estas opções permitem comparar configurações; não estabelecem a causa de uma interrupção no fornecedor, rede ou decoder.
