# TV-AVO-01 — modo TV Avó

**Estado atual:** implementado e validado automaticamente em 24/10/2026, incluindo o pedido posterior de retirar play/pausa e ativar as opções de versões. Aceitação física na box pendente.

## Ativar e desativar

**Definições → Esquema → Personalização do menu lateral → Modo TV Avó**. A opção também aparece na pesquisa das definições. Desligada por defeito.

Voltar regressa diretamente à lista, num único toque. A partir da lista, abrir as definições e desligar a mesma opção recupera o leitor completo; o acesso existente continua disponível com a barra lateral oculta.

## Apresentação e comando

- No leitor interno ficam apenas **nome do canal e programação disponível**. O botão de play/pausa foi removido.
- A faixa desaparece após 4,5 s de reprodução; OK apenas a mostra, mesmo se já estiver visível, sem pausar a emissão. Setas e CH mantêm o zapping.
- Linha temporal, recuo/avanço, motores, codecs, diagnósticos, relógios e menus avançados ficam ocultos. Atalhos dessas ferramentas não abrem opções escondidas; a sugestão de AFR também não aparece.
- Volume da box e botões físicos de sistema/multimédia mantêm a sua função. O volume interno continua em 100%. O gesto Voltar é processado no KeyUp e gestos cancelados não executam a saída.
- Carregamento e falha têm apenas um indicador/mensagem curta, sem detalhes técnicos. A mensagem de erro já não manda carregar num play que não existe.
- O leitor externo conserva a interface própria. Gravações em curso e restantes dados não são apagados ou parados.

## Configuração automática das versões

Ao ativar o modo:

1. Ativa **agrupar versões do mesmo canal** no perfil atual.
2. Ativa **prioridade de reprodução das versões** no mesmo perfil.
3. Ativa **alternativas/troca automática entre qualidades**.

O perfil é preparado antes de publicar a ativação do modo. Modo e recuperação são gravados atomicamente no DataStore de modo simples. Não se alteram os mapas de ordem manual/associação, versões ocultas, ordem geral da lista, tempo de recuperação, buffers ou motor. A escada existente de qualidades continua a ser usada, respeitando as preferências atuais.

A preparação também corre no arranque e ao mudar/ativar um perfil, cobrindo modos que já estavam ativos antes deste update. O observador espera por um perfil válido e não reescreve a configuração por cada alteração de outra preferência.

**Não é um bloqueio das definições:** as opções continuam editáveis. Desligar TV Avó não desliga nem restaura automaticamente estas três opções; podem ser ajustadas individualmente. Voltar a ativar o modo, arrancar com ele ativo ou entrar noutro perfil volta a aplicar a preparação.

## Persistência e backup

- `tv_avo_mode` no DataStore App existente. Incluído no backup completo através de `TvBackupAppSettings`/`SimpleModeBackupCodec`, com true/false explícitos.
- Agrupamento/prioridade já integram o backup das personalizações do perfil; recuperação e tempo já integram o modo simples. Os percursos de backup existentes foram preservados.
- Backups antigos sem o campo mantêm a escolha atual; dados inválidos são recusados antes de aplicar preferências. A migração antiga de defaults corre antes da preparação automática, evitando que volte a desligar a prioridade.

## Implementação e validação

- `TvAvoPlayerHud.kt`/`TvAvoKeyPolicy.kt`: leitor sem botões no ecrã, foco único para o comando, OK informativo e ferramentas bloqueadas.
- `TvAvoChannelPreset.kt`: cópia apenas dos dois flags de versões e observador por ativação/perfil.
- `SimpleModePreferences`, `SettingsViewModel`, `NavMenuSettingsScreen` e `OwnTVApp`: ativação ordenada, recuperação persistida e preparação de perfis. Textos atualizados em inglês e português europeu.
- **470 testes JVM App**, sem falhas/erros/skips; quatro testes novos neste complemento, além dos seis da implementação inicial.
- Testes de setter real/reabertura do armazenamento, backup/restauro, falha de preparação sem publicar ativação, preservação do tempo e ordens manuais, troca de perfil, ausência de escrita por preferências alheias, zapping e OK sem pausa.
- Compilação debug/release e lint debug/vital release sem erros. Log final: `%TEMP%/owntv-tv-avo-automatic-validation.log` (`BUILD SUCCESSFUL`). Baseline i18n mantida em 47 entradas.
- **Core, snapshot e motores sem alterações**, 1 071 inputs conferidos, SHA-256 `ff76713c2d027257ff49b416d7bb13c56a5968045319b88252577e4ab796014a`. Não foi gerado/assinado/instalado APK.
- Rollback inicial: `C:/Users/renat/Downloads/OwnTV-tv-avo-backup-20261024-154702/`. Antes deste complemento: `C:/Users/renat/Downloads/OwnTV-tv-avo-sem-transporte-backup-20261024-161617/`. ZIPs e manifestos conferidos por SHA-256.

## Aceitação física pendente

Ativar e confirmar as três opções no perfil; testar Exo/mpv, cima/baixo, CH, OK sem pausa, um único Voltar e volume da box. Reabrir/reiniciar e mudar de perfil com o modo ativo; conferir a ordem manual e o tempo configurado. Desativar o modo deve recuperar o leitor normal sem reverter as opções de canais. Testar falha de canal, diálogo sobreposto e backup/restauro.
