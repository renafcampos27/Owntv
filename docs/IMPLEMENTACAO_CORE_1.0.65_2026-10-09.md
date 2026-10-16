# Integração seletiva Core 1.0.65 — 09/10/2026

Autorização: implementar as recomendações da análise Core 1.0.65. Projeto TV `OwnTV-main` com Core externo `OwnTV_Core`. Não se substituiu o Core completo nem se alterou o Core privado Mobile. A versão nativa libmpv permanece 2026.10.2.

## Funcionalidades e correções

1. **Codec mpv:** `hwdecCovers` reconhece descrições H.264/HEVC/MPEG-2/MPEG-4/VC-1/VP8/VP9/AV1. Descrições desconhecidas não constituem prova de incompatibilidade. Preserva hardware/software escolhidos pelo utilizador.
2. **Diferidos:** retry/reload e regresso do background conservam `isArchive` e a posição pedida. A disponibilidade de seek continua dependente do fornecedor e do leitor; a atualização não força o reinício do programa nem substitui um programa antigo pelo direto.
3. **Backup:** adoção de perfil restaurado passa para o fim, antes da limpeza do marcador de restauro. Mantém o backup personalizado e remapeamento de IDs.
4. **Rede live:** DNS com espera máxima inicial de 5 s, ou 1 s quando existe resposta anterior válida. Cache limitada a 64 hosts e validade de 5 minutos; execução partilhada limitada a quatro threads e fila de 64. Consultas simultâneas ao mesmo host partilham trabalho. O resolver Android subjacente pode continuar após o prazo; não se afirma que tenha sido interrompido.
5. **Cabeçalhos HTTP:** prazo de 8 s por salto HTTP somente após o canal já ter reproduzido. O prazo é desarmado quando chegam os cabeçalhos; o corpo mantém o timeout normal. Os mecanismos de cancelamento e identidade de sintonia existentes continuam responsáveis pela libertação.
6. **Recuperação live:** após duas falhas de descodificação/conteúdo numa sequência de reprodução, permite que o controlador existente trate o erro em vez de repetir indefinidamente o mesmo conteúdo. Contagem limpa em nova sintonia e após reprodução estável. Uma recuperação que já recebe bytes atuais pode ter uma única margem de 3 s. Falhas transitórias de ligação não devem ensinar incompatibilidade de formato. Não se criou outra máquina de reconnect.
7. **Vídeo mpv:** acompanha associação efetiva da Surface ao core mpv e volta a ligá-la antes do load quando necessário. Mantém ownership, geração e saída de vídeo atuais.
8. **AES-128 HLS em gravação:** conserva chave/IV por segmento, mudança para METHOD=NONE e chaves rotativas. Obtém chaves de 16 bytes com pedidos autenticados e leitura limitada. Desencripta por blocos para disco, verifica padding, sincroniza e só depois valida/commita media. Preserva variantes/master playlists locais, gaps, validação TS/fMP4 e estado PARTIAL. Reserva scratch adicional somente nos segmentos encriptados.
9. **Gravações inválidas:** páginas HTML/XML/JSON de erro e texto de playlist não são escritos como captura binária. A validação estrutural já existente dos segmentos mantém-se; não se usa 32 KiB como prova de ficheiro reproduzível.
10. **Agendamento manual:** menu completo do canal → **Agendar gravação**. Dia até 30 dias à frente, hora/minuto de início e fim, passagem da meia-noite, timezone e mudança de hora. O gestor verifica perfil, existência da fonte e sobreposição de timers antes de agendar; utiliza o armazenamento e scheduler existentes. Não depende de EPG ou do seu desvio horário.
11. **Recuo remoto:** pedidos ao arquivo do fornecedor ficam a pelo menos 60 s do direto; avançar através desse limite regressa ao direto. Uma seleção absoluta indisponível é recusada em vez de deslocada silenciosamente. Buffer/timeshift local não recebe esse limite.
12. **Atualização automática:** catálogo aguarda até 2 minutos por inatividade e, se continuar a reprodução, pede retry ao WorkManager. EPG também aguarda até 2 minutos, mas depois prossegue para não adiar indefinidamente o guia. Atualização manual não é adiada por esta política; intervalo configurado, incluindo 6 h, permanece.

## Limites intencionais

- HLS estrito, motores exclusivos, volume a 100%, definições por canal, tempos de desistência/reabertura e correção horária preservados.
- Não foram importados defaults de mediacodec-copy para todos os diferidos, hard-reset global, recursos de smartphone, filmes/séries/favoritos ou outro agendador automático paralelo.
- Não se ativa uma passagem automática de diferido para direto devido a falha de rede. Essa proposta ficou opcional na análise; a identidade do programa pedido mantém-se.
- AES-128 não é SAMPLE-AES/DRM. Byte ranges encriptados são recusados; init fMP4 encriptado exige IV explícito. Pausa/retoma persistente de download de arquivo AES continua recusada sem proveniência durável da chave; a captura AES normal é suportada.
- Agendamento manual recusa timers sobrepostos na mesma fonte conservadoramente; não presume sessões adicionais disponíveis no fornecedor.

## Verificação

Compilação Kotlin standardDebug e suites Core/player-core concluídas com sucesso: Core 1 123 testes; player-core 448 testes; total 1 571, sem falhas/erros/ignorados. Inventários de traduções atualizados apenas para os novos literais técnicos; rótulos novos em recursos, com português europeu.

Testes novos abrangem codec, DNS bloqueado/cache expirada, deadline de cabeçalhos/desarme após resposta, AES e padding inválido, rotação de chaves e METHOD=NONE, documentos de erro, horários/DST e recuo remoto. Bibliotecas nativas não foram substituídas por esta integração Core.

Registo da verificação final: `C:\Users\renat\AppData\Local\Temp\owntv_core65_final_build.log`. Sem APK, commit ou publicação. Testes JVM não demonstram comportamento físico de áudio/vídeo, foco D-pad ou fornecedor IPTV.

## Teste necessário na box

Gerar o APK habitual no Android Studio após sincronizar Gradle. Testar A–B–A e A–B–C–A antes de 15 s; retry/regresso de diferido; gravação AES disponível no fornecedor; agendamento atravessando meia-noite; menu/foco do diálogo; atualização EPG durante sessão prolongada. Confirmar reprodução normal sem regressões. O bloqueio de reabertura rápida ainda não foi dado como resolvido.

Originais preservados pelo editor de integração em `C:\Users\renat\AppData\Local\Temp\owntv-core65-before-20261009`; não usar reposição global de Git para reverter, pois existem alterações anteriores do utilizador.

Referências: [análise](ANALISE_CORE_1.0.65_2026-10-09.md), [release upstream](https://github.com/ahXN00/OwnTV_Core/releases/tag/core-1.0.65), [comparação upstream](https://github.com/ahXN00/OwnTV_Core/compare/core-1.0.64...core-1.0.65).
