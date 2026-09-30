# Diferido e grelha EPG — 30/09/2026

## Correções

- O diálogo distingue «Ver canal em direto», «Ver desde o início» e «Tentar ver este programa». Quando os metadados indicam menos dias de arquivo, existe uma tentativa explícita para programas antigos em canais com diferido, até ao limite suportado de 31 dias. A tentativa não é apresentada como disponibilidade confirmada.
- A abertura interna de um programa antigo cancela a sintonia anterior e liberta o direto antes de resolver/abrir o arquivo. Atualiza o canal e contexto de zapping. Se não consegue construir o pedido de arquivo, apresenta indisponibilidade; não troca esse pedido por uma sintonia em direto.
- Os endereços Xtream continuam a usar o início e a duração do programa selecionado. A preferência HLS do direto não transforma o endereço do arquivo.
- EPG publicado pode ser conservado até 31 dias. A janela do guia mantém pelo menos sete dias e cresce conforme o máximo de arquivo anunciado pelos canais da lista. O seletor de programas diferidos respeita a profundidade do canal, elimina duplicados e deixa de truncar a lista nos primeiros 80 programas.
- Pedidos ao portal Stalker permanecem limitados à profundidade anunciada, evitando pedir 31 dias a todas as fontes.
- Cada bloco visual termina antes do bloco seguinte quando os feeds se sobrepõem. Texto e marcações são recortados dentro do bloco; títulos parcialmente fora do ecrã são posicionados na parte visível. O cursor abre o programa mais recente que cobre a hora selecionada, coerente com a grelha desenhada.
- O botão de favorito foi removido do leitor de canais, incluindo o diferido e o modo mini. Os favoritos da lista/EPG não foram alterados.

## Limites

EPG e arquivo são serviços diferentes. Mostrar um programa de há cinco dias não comprova que o fornecedor ainda tenha a gravação. Não foram usados dados/credenciais reais da conta, pelo que não foi comprovado o arquivo de nenhum canal.

A app não fabrica programas ausentes do EPG nem recupera conteúdo já apagado. Os dias adicionais publicados começam a ficar disponíveis após atualizar as listas/EPG; dados anteriormente eliminados só reaparecem se o fornecedor voltar a fornecê-los. Metadados ausentes usam sete dias para navegação; uma tentativa fora da janela indicada depende da resposta do servidor.

Estas alterações não validam o conteúdo de uma resposta HTTP que o próprio fornecedor sirva como direto num endereço de arquivo. Se persistir, é necessário analisar o canal, programa/hora, endereço redigido e resposta efetiva, sem expor credenciais.

## Validação

Compilação conjunta aprovada e **1 360 testes passaram**, sem falhas, erros ou testes ignorados (App 200, Core 852, player-core 308). Seis testes novos cobrem sobreposição/delimitação dos programas e políticas de histórico/tentativa de arquivo. Verificação i18n e diferenças dos ficheiros alterados aprovadas. Sem APK gerado. Sem teste na box ou nos arquivos reais do fornecedor.

Registo: `C:\Users\renat\Documents\Codex\2026-09-22\c-users-renat-downloads-owntv-main\work\epg-archive-fix-validation-final.log`.

Na Xiaomi: atualizar listas/EPG; escolher um programa de há cinco dias; usar a ação de programa/arquivo, confirmar título e início. Comparar dois canais com profundidades diferentes e verificar limites dos blocos ao percorrer a grelha com as setas. Confirmar também zapping ↑/↓, voltar ao direto e ausência do botão de favorito.
