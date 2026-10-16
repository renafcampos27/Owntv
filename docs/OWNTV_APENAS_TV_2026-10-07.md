# OwnTV — aplicação dedicada a Android TV e boxes

Decisão do utilizador: 2026-10-07. A utilização em smartphone passa para a aplicação separada OwnTV Mobile.

## Alterações

- Removida a classificação de dispositivo para alternar entre smartphone e TV.
- Removidos os layouts de smartphone e a navegação horizontal alternativa da shell.
- Leitor utiliza sempre o HUD de TV; removidos o HUD compacto, menu móvel, bloqueio de toque e gestos táteis de arrastar na linha temporal.
- Mantidos a barra temporal, os diferidos e o recuo/avanço pelo comando.
- Campos e pesquisa usam sempre o foco em duas etapas da TV: focar com as setas e premir OK para editar.
- Popups usam a escala e a geometria do teclado de TV, com os mecanismos de compatibilidade dos teclados OEM.
- Removidos os ajustes de alvos táteis e os módulos/testes exclusivamente móveis.
- MainActivity usa orientação horizontal.
- Mantidos ambos os launchers e leanback opcional para não excluir boxes genéricas sem declaração de Android TV.
- Percursos de baseline profile/benchmark passam a usar apenas navegação por comando.
- Política de sessão de áudio fixa no comportamento de TV já existente; sem seleção automática da política de telefone.

## O que permanece por ser funcionalidade da TV

Introdução de listas e envio de imagens para a TV a partir de outro equipamento na rede, cliques de rato/air mouse, acessibilidade, teclado do Android e proteções para TVs de baixa resolução. Não são adaptações para executar a app no smartphone.

Não foram alterados os motores, transporte HLS, buffers, volume, dados de utilizador ou APIs partilhadas do Core que a OwnTV Mobile possa utilizar.

## Planeamento

Esta decisão substitui as recomendações anteriores de adaptar esta aplicação a smartphones. A recomendação A7 do documento REVISAO_COUNCIL_APP_APOS_LIBMPV_2026.10.1.md, relativa à adaptação móvel, fica cancelada para este projeto. As restantes correções desse plano continuam propostas, sem implementação neste trabalho.

## Validação

Compilação, testes unitários e compilação do módulo de benchmark: resultados registados no fim da intervenção. Sem geração de APK e sem validação física nesta intervenção. Antes de distribuir, verificar em box: foco da lista, entrada de URL e senha, pesquisa, teclado nos popups, fullscreen, zapping, Voltar e linha temporal dos diferidos.

### Resultado final

- Compilação Kotlin standardDebug: sucesso.
- Testes unitários da aplicação: 303, falhas 0, erros 0, ignorados 0.
- Compilação Kotlin do módulo baselineprofile/benchmark: sucesso; o percurso não foi executado num dispositivo.
- Verificação de literais: sucesso.
- Pesquisa de referências aos módulos móveis removidos: nenhuma referência restante.
