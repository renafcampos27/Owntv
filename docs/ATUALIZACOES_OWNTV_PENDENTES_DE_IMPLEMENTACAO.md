# Atualizações OwnTV — referência e pendências

Última atualização: 18/10/2026.

## Integração mais recente — OwnTV 5.2.0 / Core 1.0.68

Integradas seletivamente as recomendações A–D aprovadas pelo utilizador, preservando a apresentação TV e o Core personalizado. A v5.1.0 abaixo é a referência histórica registada anteriormente; as próximas comparações devem também consultar a [integração 5.2.0/1.0.68](IMPLEMENTACAO_OWNTV_5.2.0_CORE_1.0.68_2026-10-18.md) e o código atual. Não corresponde à importação integral de funcionalidades da v5.2.0 nem altera a versão comercial da App.

## Versão upstream de referência

**A versão personalizada da aplicação OwnTV tem como referência de equivalência a v5.1.0 de ahXN00/OwnTV, registada em 06/10/2026 por indicação do utilizador.**

Referência oficial: [OwnTV v5.1.0](https://github.com/ahXN00/OwnTV/releases/tag/v5.1.0).

Esta equivalência identifica a versão de referência para futuras comparações. A aplicação mantém alterações próprias e integração seletiva; não significa cópia integral, identidade de código ou presença de todas as funcionalidades upstream. Não altera o número da versão apresentado pela aplicação nem gera um APK.

## Regra para futuras análises

Sempre que se analisar uma nova versão de ahXN00/OwnTV, consultar este ficheiro, a análise da v5.1.0, os registos de implementação e o código personalizado atual. Comparar as novidades com esta referência, distinguir o que já existe do que falta e atualizar as pendências. Uma publicação upstream não autoriza automaticamente a sua implementação.

## Integrações registadas

- Categorias, visibilidade, remoção de associações, pesquisa e navegação: [registo A/B](IMPLEMENTACAO_OWNTV_5.1.0_CATEGORIAS_PESQUISA_2026-10-06.md).
- Robustez seletiva HLS, diferidos sem imagem e retoma de timeshift local: [registo C/D/E](IMPLEMENTACAO_OWNTV_5.1.0_LOTES_C_D_E_2026-10-06.md).

## Pontos não importados

- Reformulação integral da interface Stage e organização literal em 12 grupos de definições.
- Restantes elementos visuais opcionais do lote E, incluindo tratamento de prefixos e contraste automático de logótipos.
- Outras adaptações classificadas como condicionais na [análise original](ANALISE_OWNTV_5.1.0_2026-10-06.md), sem autorização específica.

Estes pontos não devem ser considerados defeitos ou obrigações de integração apenas por existirem na versão oficial. Preservar a simplificação da aplicação e as funcionalidades que o utilizador decidiu excluir.

## Referência do Core

O Core personalizado tem referência upstream v1.0.64. Consultar também [atualizações e histórico do Core](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md) nas próximas comparações.

## Histórico

| Data | Decisão |
|---|---|
| 06/10/2026 | Utilizador pediu para registar que a sua versão da OwnTV equivale à referência v5.1.0 de ahXN00/OwnTV, mantendo as personalizações e a integração seletiva. |
