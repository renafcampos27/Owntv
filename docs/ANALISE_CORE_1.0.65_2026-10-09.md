# Core 1.0.65 — integração seletiva proposta

Data: 09/10/2026. Análise de fontes; sem implementação ou teste físico. Comparação upstream core-1.0.64 → core-1.0.65 e inspeção do Core TV personalizado. Não substituir o Core completo nem alterar o Core privado Mobile.

Atualização posterior: as recomendações foram autorizadas e implementadas seletivamente. Consultar `IMPLEMENTACAO_CORE_1.0.65_2026-10-09.md`; o texto seguinte preserva a análise anterior à implementação.

## Prioridade 1 — correções pequenas e objetivas

### Identificação dos codecs mpv

Local: OwnTVPlayer.hwdecCovers ainda usa a primeira palavra de video-codec. Uma descrição `H.264 / AVC / …` resulta em `h.264`, diferente do nome autorizado `h264`. Upstream normaliza descrições e distingue codec desconhecido de comprovadamente não autorizado. Integrar esta normalização, com testes de H.264/HEVC/MPEG-2 e preservação da preferência hardware/software. Evita decisões falsas de incompatibilidade e recuperações desnecessárias; não demonstra a causa da reabertura rápida.

### Preservar identidade de diferido

Local: reload chama loadUrl sem isArchive; BackgroundRestore precisa da mesma revisão. Upstream conserva isArchive em retry e regresso do background. Integrar a identidade, mas avaliar separadamente a escolha upstream de reiniciar o diferido do princípio: preservar a posição quando o fornecedor/caminho suportar, sem regressar silenciosamente ao direto.

### Ativar perfil só depois do backup restaurado

Local: BackupManager ativa o perfil antes de concluir todo o restauro. Upstream guarda profileToAdopt e aplica no fim, antes de limpar o marcador. Integrar respeitando backup completo local, remapeamento de IDs e retomada/interrupção da operação. Evita abrir interface com configurações ainda incompletas.

## Prioridade 2 — rede e transição de reprodução

- BoundedDns: consulta com espera limitada, resposta anterior em falha e partilha de consulta em curso por hostname. Ausente localmente. Adaptar com limites de concorrência/cache e validade das respostas, respeito por DNS personalizado e diagnóstico sem dados sensíveis. Não copiar sem revisão o executor cachedThreadPool ou cache sem prazo/limite. A resolução Android subjacente pode continuar depois do timeout; não declarar que o pedido foi totalmente terminado por esse mecanismo.
- ResponseDeadline: limita espera dos cabeçalhos HTTP por salto de redirect; cancela e fecha respostas na corrida com timeout. Ausente localmente. Adaptar ao cliente de live e identidade/geração local. Não aplicar indiscriminadamente o valor upstream de 4 s a todo o HLS; testar segmentos, origens lentas e live com latência variável. Preservar o orçamento global de abertura configurado.
- Live recovery: upstream distingue conteúdo impossível de descodificar de falha de rede, observa bytes recebidos e dá pequena margem quando já está a recuperar. Adaptar ao controlador existente, sem duas máquinas de reconnect simultâneas. Respeitar Exo/mpv exclusivo, HLS estrito, cancelamento e timeout do utilizador. Não importar a política de raw-TS para canais HLS estritos.
- Surface mpv: upstream distingue existência de Surface da associação ao core mpv e volta a associá-la depois de handover. Local não apresenta o novo mpvHasSurface. Candidato útil, mas validar ownership/lifecycle, cancelamento e retorno lista/fullscreen antes de adaptar.
- Auto refresh: TV já agenda atualização em ShellViewModel; não duplicar com AutoRefresh do Core. Workers locais não contêm a nova espera de WatchSession. Considerar apenas impedir concorrência de atualizações automáticas durante reprodução; atualizações manuais e intervalo EPG de 6 h devem continuar respeitados. Decidir política para utilização contínua, evitando adiar indefinidamente o guia.

## Prioridade 3 — gravação e recuo

- AES-128 HLS: upstream adiciona chaves/IV por segmento e desencriptação. Local recusa listas encriptadas; benefício novo real para gravações autorizadas de HLS AES-128. Não confundir com DRM/SAMPLE-AES. Integrar limites de tamanho, validação de chaves/IV, rotação e headers/redirects; não carregar segmentos arbitrariamente grandes sem limite.
- Master playlist: local já segue até três níveis de variantes e impede escrever master como media. Não substituir pela implementação upstream que segue apenas um nível. Acrescentar testes/funcionalidade AES mantendo a seleção local e personalização de qualidade.
- Agendamento por dia/hora sem EPG: upstream scheduleManual/manualWindow ainda não aparecem na API local inspecionada. Candidato funcional separado, com passagem da meia-noite, timezone/DST, conflitos, armazenamento e UI TV.
- Gravação muito pequena: upstream mínimo 32 KiB não prova conteúdo reproduzível. Local mantém PARTIAL para falhas/lacunas; preservar essa classificação e preferir validação de media em vez de copiar outcomeOf upstream.
- Recuo próximo do direto: upstream aplica mínimo de 60 s aos pedidos de arquivo do fornecedor, evitando solicitar um arquivo ainda não escrito. Local LiveTimeshift.scrub permite offsets menores. Adaptar apenas ao arquivo remoto; não limitar buffer local. Testar 30/60/90 s e correção horária independente.
- Diferido ainda em emissão: liveAfterStall propõe voltar ao direto quando o arquivo chega ao programa atual. Só integrar como comportamento explícito/opcional; rede lenta não deve mudar um programa histórico para direto.

## Não importar globalmente

libmpv 2026.10.2 já foi implementado e validado na integração local. Não repetir atualização.

Não importar funcionalidades de smartphone, filmes/séries/favoritos, nem preview EPG ativo por defeito. Os novos recursos de pesquisa de programas podem ser avaliados futuramente, mas não são correção dos horários EPG. A extração de epgKeyOf em GuideReader é reorganização para reutilização, não corrige o desvio de uma hora.

Não tornar mediacodec-copy o padrão para todos os diferidos sem teste: upstream assume arquivos até 1080p, o que não deve ser assumido sobre as listas locais. Não acrescentar globalmente hard-reset/watchdogs de primeiro frame sem integrar os existentes e testar a recuperação da 2026.10.2.

## Ordem de implementação proposta

1. Normalização de codec, identidade de arquivo e fim do restauro.
2. Rede com prazos e transição de Surface, uma alteração de cada vez; testar A–B–A em menos de 15 s e A–B–C–A, sem atribuir resolução antecipadamente.
3. AES-128 gravação e agendamento manual, preservando recorder local mais desenvolvido.
4. Mínimo de recuo remoto e política de refresh, com critérios funcionais explícitos.

Fontes: https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.65 e https://github.com/ahXN00/OwnTV_Core/compare/core-1.0.64...core-1.0.65.

Validação posterior: testes unitários focados, compilação TV/Core e testes físicos de live/diferidos/zapping/gravação. Nesta análise não foram executados testes nem modificadas dependências/código.
