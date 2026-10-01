# Entrada de texto adaptativa — TV e smartphone

Implementada na app Kotlin/Jetpack Compose. A arquitetura armv7/arm64 apenas determina a compatibilidade do binário; não determina teclado ou tipo de dispositivo. Sem nova preferência, migração ou alteração da versão.

## Decisão do council

A OwnTV já usa o teclado nativo Android na TV. Não existe necessidade de importar o exemplo Java/EditText/XML ou remover TYPE_NULL/descendantFocusability. A causa identificada é o foco de duas etapas aplicado globalmente aos componentes partilhados.

- Modo televisão ou funcionalidades TV/Leanback: manter foco por comando, OK para editar e Back/Done para voltar ao campo exterior.
- Dispositivo sem touchscreen: manter comportamento de comando como alternativa para boxes Android genéricas.
- Smartphone/tablet com toque, sem capacidades TV: campo real diretamente focável/tocável, teclado nativo, cursor, seleção e colagem. Done/Search fecha o teclado e limpa o foco.
- Teclado URL, numérico, palavra-passe e PIN respeitam o tipo do campo. As palavras-passe genéricas passam a usar KeyboardType.Password; os PIN numéricos conservam o tipo explicitamente indicado.
- Nos diálogos táteis, conservar a compensação única existente, usando apenas insets/medição real. Não estimar 45% de ocupação nem guardar calibração TV a partir de medidas do telemóvel. Não adicionar atrasos repetidos ou forçar teclado ao abrir todos os ecrãs.

Componentes alterados: DeviceTextInput, OwnTVTextField, SearchBar, OwnTVPopup e TvImeMetrics. Os formulários que usam o componente comum recebem a adaptação, incluindo fontes/credenciais e pesquisa.

## Limites e aceitação

Esta entrega adapta a entrada de texto; não redesenha todos os ecrãs para orientação vertical ou layout móvel. Uma box que declare touchscreen sem modo/capacidades TV será tratada como tátil. Um pedido explícito de foco de um ecrã existente segue o comportamento nativo do campo móvel.

Testar fisicamente no smartphone: URL M3U, servidor Xtream, utilizador, senha, pesquisa e números; toque inicial, cursor, seleção/colar, mostrar senha, Done/Search, Back e diálogos aninhados. Com teclado físico ligado, o diálogo não deve inventar espaço ocupado por teclado virtual.

Testar na TV: setas passam pelos campos sem abrir teclado; OK abre escrita; Back/Done devolve foco; mostrar senha e guardar continuam acessíveis. Sem alterações no streaming, HLS, zapping, grupos, gravações ou quotas.

Validação automatizada: quatro testes de decisão cobrem smartphone/tablet, modo TV com toque, capacidade TV em modo genérico e box sem toque. Estes testes e compilação não substituem testes reais de IME/seleção/gestos no dispositivo.

Referências oficiais: [foco em campos Compose](https://developer.android.com/develop/ui/compose/touch-input/focus/focus-in-text-fields) e [visibilidade do teclado Android](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility).
