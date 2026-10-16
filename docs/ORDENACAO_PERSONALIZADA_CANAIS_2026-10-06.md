# Ordenação personalizada de canais

O filtro de ordenação da lista de canais passa a alternar entre ordem da lista, A–Z e Personalizada.

Para editar, abrir o menu completo do canal, selecionar Reordenar canal, deslocar com as setas ou os botões Subir/Descer e Guardar. Cancelar repõe a ordenação anterior. Guardar mantém Personalizada selecionada.

A lista de todos os canais ganha uma ordem persistida por perfil no contexto `__all__` da tabela existente `content_order`. O filtro de canais em diferido e a pesquisa nessa lista conservam a ordem global; editar a partir dos diferidos usa a lista completa para preservar a posição dos restantes canais. As categorias continuam a dispor dos seus contextos de ordenação.

Os canais novos sem posição guardada aparecem após os ordenados, na ordem original da lista. Uma edição preserva as entradas guardadas de canais não incluídos na sessão, incluindo origens temporariamente inativas. A gravação é feita numa transação e verifica o perfil proprietário da sessão.

O modo de ordenação é guardado nas preferências existentes. As posições usam a tabela e os mecanismos de backup e recuperação após sincronização já existentes. Não é necessária alteração de esquema da base de dados.

Validação adicional em SQLite com a consulta real: ordem guardada, persistência após reabrir, isolamento de perfil/origens, pesquisa, filtro de diferidos e posição de canais novos. A compilação Room verifica as novas consultas. A interface deve ainda ser testada numa box e num smartphone; não foi gerado APK.

Compilação `standardDebug` e testes unitários da aplicação e Core concluídos com sucesso.
