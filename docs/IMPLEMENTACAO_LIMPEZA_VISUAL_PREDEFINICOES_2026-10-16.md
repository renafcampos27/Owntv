# OwnTV TV — limpeza visual e predefinições, 16/10/2026

Pedido: barra lateral visível, prioridade de versões desativada e remoção de brilho ambiente, animações decorativas e meteorologia.

## Comportamento implementado

- **Ocultar barra lateral: desligado.** Default para instalações novas e aplicação única a instalações existentes. A opção continua disponível.
- **Prioridade de versões: desligada.** Default do modelo e do JSON, com migração única das personalizações LIVE existentes. Grupos, nomes, canais ocultos e ordens manuais são preservados. As alternativas automáticas continuam independentes desta preferência.
- As duas escolhas são aplicadas uma vez; alterações posteriores e backups restaurados após essa aplicação são respeitados. A marca técnica da migração não é exportada para outro dispositivo.
- Se a gravação das preferências falhar, a operação não marca a migração como concluída e pode repetir no arranque seguinte.

## Componentes retirados

- Brilho ambiente: opção, pesquisa, janela, observadores, setters, Canvas do shell e aura decorativa do assistente inicial.
- Animações decorativas: enum e política do Core, propagação no tema, helper de duração, timeline de parallax/foco, transições de cor/tamanho, pulsações de badges e equalizador e crossfade dos logótipos. O feedback de foco e seleção passa a ser imediato.
- Meteorologia: repositório de rede/cache, modelo, registo de dependências, preferências e recursos de texto/ícones exclusivos.
- Removido o teste de uma combinação de APIs de animação que já não existe na aplicação.
- Mantêm-se indicadores funcionais de carregamento e contagens de espera do leitor; não são opções decorativas.

## Compatibilidade e limites

Os novos backups deixam de exportar as seis preferências retiradas. Backups antigos com esses valores, incluindo metadados `unsetPortableKeys`, continuam aceites; os campos descontinuados são ignorados. Apenas os seis nomes persistem na camada de compatibilidade.

A alteração pertence exclusivamente à OwnTV TV e ao seu Core personalizado. Não exige migração do esquema Room. A verificação visual, foco com comando e arranque numa box física ainda precisam de teste após instalar o novo APK.

## Validação

**BUILD SUCCESSFUL** na validação final (12 min 34 s; 154 tarefas).

- App compilada em debug e release.
- `lintVitalStandardRelease` aprovado.
- **415 testes App + 1 256 testes Core + 549 testes player-core = 2 220 testes unitários**, sem falhas, erros ou ignorados.
- **6 testes do empacotamento do Core** aprovados.
- XML de recursos validado; verificações i18n, números, overflow e whitespace aprovadas.
- Inventário i18n: App 51 baseline + 1 048 literais técnicos; Core 266 baseline + 5 619 literais técnicos. Novos nomes técnicos classificados explicitamente, sem acrescentar texto de interface à baseline.
- Pacote de código Core atualizado: 994 ficheiros, SHA-256 `f2cdeba215d82cdb4b987ef3406547fc3f5dccd0ed8ecad13307af200daf459e`.

Registos: `C:/Users/renat/AppData/Local/Temp/owntv-feature-cleanup-core.log` e `C:/Users/renat/AppData/Local/Temp/owntv-feature-cleanup-app.log`.

Mantêm-se avisos preexistentes de testes e deprecações Gradle; não foram tratados como falhas nem ocultados.

Compilação e testes automatizados não substituem validação numa box física. Não foi gerado nem instalado APK.

## Inventários atualizados

- `INVENTARIO_DEFINICOES_TV_2026-10-16.md`
- `ESCOLHER_PREDEFINICOES_TV_2026-10-16.csv`
- `INVENTARIO_TECNICO_DEFINICOES_TV_2026-10-16.md`
