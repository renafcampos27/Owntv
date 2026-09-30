# Fase 5 — Base de dados

Data: 28/09/2026. Âmbito: estrutura, acesso aos dados e backup. Timeshift não implementado. Sem gerar APK.

## Entrega

- Room: versão 43 → 46, com migrações incrementais 43→44→45→46 e schemas exportados.
- `playback_quirks`: estrutura para exceções técnicas por conteúdo (motor, apenas áudio, atraso de áudio), identificadas por chave estável e fonte; DAO com operações transacionais e registo na injeção de dependências.
- `playback_prefs`: fonte, idioma de áudio e idioma de legendas. Os métodos existentes preservam os novos campos; a limpeza de preferências não elimina linhas que ainda contenham idiomas.
- `sources`: campos opcionais para fuso/offset de catch-up, motor VOD, timeout de sintonia e Referer HTTP.
- `metadata_cache`: idioma original opcional.
- Backup v24: inclui os novos campos e exceções, remapeia os identificadores das fontes ao restaurar e mantém compatibilidade com backups anteriores. O Referer só é exportado na modalidade cifrada, pois pode conter credenciais. Um Referer indecifrável não substitui o valor existente.
- A coluna experimental `softwareDecode` das versões intermédias é removida na migração 45→46, conforme o esquema de referência.

## Limites deliberados

Esta entrega prepara a persistência; não introduz menus nem ativa novas políticas de reprodução. As preferências atuais de motor/apenas áudio continuam nos respetivos stores. O atraso de áudio existente continua associado ao perfil; não foi convertido numa preferência partilhada entre perfis. Os novos campos de fonte e idioma original ainda precisam de consumidores para produzirem efeitos na reprodução/metadados.

Não há melhoria de fluidez mensurada atribuível a esta migração. Não foi alterada a navegação com as setas nem a mudança automática entre alternativas.

## Validação

- Compilação Kotlin da app e Core concluída.
- Migrações reais executadas em SQLite no computador a partir dos schemas 43, 44 e 45; comparação das colunas, tipos, nulabilidade e presença dos índices com o schema 46; conservação de preferências/exceções e `integrity_check` verificados.
- Testes do backup: campos opcionais, proteção do Referer, compatibilidade com campos ausentes, remapeamento de fontes/chaves e limites numéricos.
- Testes completos: 179 da app + 238 do player-core + 765 do Core; zero falhas, erros ou testes ignorados.
- Sem dispositivo ligado: falta validar atualização de uma instalação real e exportação/restauro pela interface na Xiaomi/Thomson. Os testes SQLite usam JDBC no computador; não substituem o teste Room/Android numa box.

## Timeshift local — proposta, não implementada

Ao ver um canal, a app guardaria continuamente os segmentos recebidos numa janela circular no armazenamento. Pausar suspenderia a reprodução, mantendo a receção. Retomar leria os segmentos guardados; recuar só seria possível dentro do período já capturado. Uma ação «Direto» regressaria à transmissão atual.

A janela teria limite de espaço e de duração, eliminando os segmentos mais antigos. Quando uma pausa excedesse a janela disponível, a app teria de avisar e retomar no ponto mais antigo ainda existente. A proposta é limpar a janela ao mudar de canal ou sair da app. Não seria uma gravação permanente nem permitiria recuperar programas anteriores à abertura do canal.

Proposta inicial: desligado por defeito, implementação HLS/ExoPlayer primeiro, sem transcodificação, uma única receção do fornecedor reaproveitada para guardar/reproduzir, espaço livre mínimo e limite configurável. Outros formatos/motores exigem avaliação própria. A 8 Mb/s, 30 minutos representam aproximadamente 1,8 GB, antes de overhead.

Isto acrescenta escritas no armazenamento e alguma complexidade. Não recupera segmentos que a origem não entregou nem corrige falhas de descodificação; não deve ser apresentado como solução para os engasgos atuais. Só avançar após autorização e testes de consumo de CPU, armazenamento, pausa/retoma, falta de espaço e mudança rápida de canal.
