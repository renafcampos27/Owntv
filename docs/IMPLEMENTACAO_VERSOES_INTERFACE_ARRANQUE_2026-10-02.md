# Implementação — versões de canais, interface e canal de arranque

Data: 2026-10-02. Código alterado no projeto OwnTV-main e no Core local associado. Sem gerar APK, mudar a versão, fazer commit ou publicar.

## Versões dos canais

- Texto branco sobre fundo escuro no seletor e nos controlos de gestão, independente do tema.
- Foco identificado por fundo azul e contorno branco; opções desativadas mantêm indicação visual.
- «Adicionar canal às versões»: pesquisar outro canal da mesma lista e associá-lo ao grupo. A pesquisa é local, limitada e aguarda 250 ms após escrever; não abre uma ligação de vídeo.
- «Desassociar» na gestão completa permite desfazer associações manuais. As versões reconhecidas automaticamente continuam reconhecidas pelo nome.
- Associações usam identificadores estáveis do fornecedor, por perfil e por lista. São usadas na lista agrupada, na escolha de versões e nas alternativas de recuperação; não misturam contas/listas.
- Canais ocultos continuam fora da prioridade e da reprodução automática.
- Associações incluídas no backup; ao restaurar noutra instalação, são remapeados tanto o canal associado como o grupo de destino.
- Mantidos os gestos anteriores: toque normal reproduz; soltar entre 2 e 3 segundos abre as versões; manter 3 segundos abre o menu completo.

## Interface

- Removidos do topo: último canal, botão de pesquisa e título da secção «TV em direto». Mantidos relógio e seleção da lista.
- Removido o botão «Ver canal em direto» do diálogo de programas no EPG. Diferidos, gravações e restantes ações mantidos; quando não há ação de diferido, o foco inicial vai para Fechar.
- Ocultadas em Mais/Definições as entradas Sincronização local, Histórico e Favoritos. Os dados não foram apagados. Páginas antigas restauradas são encaminhadas para o menu principal.
- Removidos o grupo Rápido e os menus de fixação de opções nesse grupo; as configurações de reprodução continuam nas respetivas secções.
- Removidas as etiquetas «X ajustes» dos cabeçalhos das páginas de definições.
- Em Personalizar categorias e itens, ficou a ação A–Z; removidos ordenar por fornecedor, filtro visíveis/ocultos e o separador redundante TV em direto. Todos os itens continuam acessíveis para editar a visibilidade.

## Canal de arranque

- Selecionar um canal de arranque grava o canal e o modo específico numa única operação.
- Resolvido o perfil real também quando o identificador inicial do perfil ainda não está disponível.
- Na reabertura da app, o canal fixo é aplicado sem retomar primeiro a ligação do último canal visto. Foi centralizada a autoridade de retoma no ciclo de vida da Activity.
- A escolha permanece independente do último canal visto. A reprodução de outros canais não altera o canal fixo guardado.
- Mudanças de configuração/rotação preservam a reprodução existente; uma abertura explícita por ligação tem precedência sobre o arranque automático.
- A resolução do canal respeita perfil e origem através da integração de arranque existente. Se já não estiver disponível, regressa ao início com indicação de indisponibilidade, em vez de ficar num leitor vazio.

## Validação

- Compilação Kotlin StandardDebug: passou.
- Testes da App: 306, sem falhas, erros ou testes ignorados.
- Testes do Core: 997, sem falhas, erros ou testes ignorados.
- Novos testes: associação de nomes diferentes, estabilidade após mudança dos IDs locais, exclusão de versões ocultas, bloqueio de associações entre listas, persistência, remapeamento do backup e política de reabertura/rotação/abertura explícita.
- Verificações de textos traduzíveis e recursos: passaram; não foi aumentada a dívida anterior de tradução.
- Registo final: `%TEMP%/owntv-channel-groups-cleanup-final-validation.log`.

## Teste recomendado na box e no smartphone

1. Abrir versões com o gesto de 2 segundos; percorrer com as setas e confirmar texto branco e foco visível.
2. Adicionar uma versão com nome diferente; confirmar o grupo, fechar/reabrir a app e confirmar persistência. Desassociar para testar reversibilidade.
3. Escolher o canal fixo em Definições → Arranque da aplicação → Canal específico. Ver outros canais, carregar Home e voltar à app; deve abrir o escolhido. Repetir após encerrar o processo/reiniciar a box.
4. Exportar e restaurar um backup e confirmar a associação manual e o canal de arranque.
5. Verificar o EPG/diferidos/gravações, a navegação pelas setas e a personalização de itens ocultos.

Não houve validação física nesta execução. A compilação e os testes não substituem a verificação visual, o comando da box ou uma ligação real ao fornecedor.
