# Implementação — robustez do EPG e limpeza

Data: 06/10/2026. Autorização: aplicar as recomendações da análise aprofundada. Core personalizado e app mantidos; sem substituir o Core upstream.

## Alterações aplicadas

### E1 — cache segura, memória limitada e substituição atómica

Cada ficheiro XMLTV é analisado completamente antes de alterar a base de dados. Os programas elegíveis são preparados num ficheiro temporário privado; apenas depois da leitura completa é iniciada uma transação Room. São substituídos exclusivamente os pares fonte/canal presentes nesse resultado. A inserção usa lotes de, no máximo, 500 programas.

Ficheiro truncado, erro estrutural ou cancelamento não publica o resultado parcial. Falha na inserção provoca rollback da transação. Fontes que falham e canais não pedidos conservam os dados anteriores. O ficheiro temporário é eliminado no finally. Uma reposição que não encontra todos os canais pedidos permite a atualização de rede para os restantes.

O modo estrito do parser aplica-se à reposição destrutiva da cache; o comportamento tolerante das importações normais foi preservado.

### E2/E6 — mesma política de leitura e prioridade determinística

Grelha, barra, seletor de diferidos e continuação procuram a chave de EPG associada ao canal nos dados guardados. Foram retirados os filtros posteriores por fonte que só existiam em algumas leituras.

Os feeds completos ficam guardados. A sincronização deixou de eliminar duplicados globalmente entre fontes. A resolução na leitura prefere, para o mesmo título e intervalos sobrepostos, a fonte da própria playlist quando presente; depois prefere maior duração e usa início, fonte e id como desempates determinísticos. A normalização de títulos usa Locale.ROOT. Entradas intercaladas com outro título já não escondem duplicados. Programas diferentes e repetições sem sobreposição não são fundidos.

Todas estas operações conservam o canal/sourceId/remoteId de destino. Herança de EPG não troca de stream nem de conta.

### E3 — atualização de agora/seguinte e definições de arranque

Os fluxos visíveis de agora/seguinte recebem um relógio alinhado ao minuto. O relógio é frio e só corre com subscritores. A cache continua a expirar na fronteira do programa, evitando pedidos de rede repetidos enquanto a resposta ainda é válida. A preferência de atraso durante navegação continua aplicada.

Alterações nas tabelas do guia invalidam as leituras do direto com debounce. A grelha já possuía a sua observação. O offset persistido é uma entrada explícita nos fluxos de leitura, incluindo barra e informação de diferido, evitando depender do valor inicial zero. O seletor e a continuação leem o offset persistido antes de consultar.

### E4 — horas originais preservadas e referência da grelha explícita

Mantidos +60 minutos como correção operacional quando essa é a preferência guardada e displayStartMs/displayStopMs como horas originais. O diálogo de detalhe, apenas quando existe shift, acrescenta a posição corrigida do programa no guia. A etiqueta original não foi substituída.

O fuso, a correção do guia e a correção adicional do pedido permanecem operações distintas. Não foi subtraído um shift global da régua partilhada: diferentes canais podem ter correções diferentes. Esta alteração esclarece a referência; não demonstra que o fornecedor entrega o programa correto em qualquer configuração.

### E5 — fuso dos diferidos por lista

Nova opção dentro de Definições → Programas em diferido → Fuso dos diferidos por lista. Selecionar a lista e escolher seguir definição geral, fuso do dispositivo ou manual. O modo manual ajusta o desvio UTC em passos de 30 minutos, de −12 h a +14 h.

A resolução é comum à abertura de programas, rewind e downloads/gravação de diferidos. catchupOffsetMin representa o desvio UTC do fuso, não uma segunda correção de início. As preferências usam os campos existentes de SourceEntity, já incluídos no backup. Sem migração ou alteração de versão da base de dados/backup.

### E7 — histórico Xtream sob pedido

Quando uma janela antiga não tem programas XMLTV guardados, a OwnTV pode consultar get_simple_data_table para esse canal Xtream com diferido. Também se aplica ao seletor de programas antigos e à continuação quando faltam dados guardados.

Limites: até duas consultas históricas em simultâneo; quatro respostas em cache por cinco minutos; até 2.048 entradas da janela por resposta; chave por fonte/canal/janela e deduplicação de consultas. A leitura é incremental, tolera stop_timestamp ou end_timestamp e filtra a janela pedida. Pedidos sem consumidores são cancelados; um consumidor sair não cancela outros que partilham o pedido.

Não há sincronização histórica de todos os canais ao abrir a app. A consulta só preenche as leituras pedidas; não modifica a base de dados nem concede suporte de diferido a um canal sem essa capacidade. HTTP inválido/erro de protocolo não é guardado como resposta vazia bem-sucedida. O painel pode devolver apenas parte do histórico ou não suportar o endpoint; esta funcionalidade não cria gravações que não existam no fornecedor.

### E8 e limpeza

Cancelamento propagado na reposição, preenchimento de lacunas, descrições, cobertura e resolução de diferidos Stalker. Conservadas as proteções de geração e identidade existentes.

Removidos a eliminação destrutiva entre feeds, comentários que alegavam equivalência incorreta, variáveis/parâmetros/imports sem utilização nos caminhos alterados. Renomeado o argumento operacional do rewind. Declarações técnicas de SQL, ficheiros, protocolos e diagnóstico atualizadas no inventário de traduções; textos novos da interface em recursos inglês/português.

Não foram copiadas renomeações gerais de migrações, traduções upstream, projeções de campos inexistentes ou funções KTX sem benefício. O lote #229 anteriormente adiado continua pendente.

## Verificação

Validação final concluída: compilação Kotlin da aplicação aprovada; 1.354 testes unitários aprovados (1.046 do Core e 308 da aplicação), sem falhas ou erros. Incluem 14 novos testes unitários. Os quatro novos testes de integração Android foram compilados, mas não executados num dispositivo. Verificação de traduções e de formatação do diff aprovada. Não foi gerado APK nem realizada reprodução numa box/telemóvel.

Casos acrescentados: falha a meio da análise, cancelamento, rollback, preservação de outras fontes/canais, resposta vazia, lotes limitados, prioridade e desempate, títulos intercalados, resolução de fuso, cancelamento do último consumidor. Testes Android preparados para XMLTV completo/truncado, transação Room real e resposta histórica Xtream.

## Teste na box/telemóvel

1. Gerar e instalar um novo APK no Android Studio. Manter as preferências anteriores para comparar.
2. Com +1 h no guia, confirmar etiqueta original, posição indicada e programa aberto. Testar um programa de há cinco dias que o fornecedor realmente mantenha.
3. Permanecer num canal durante a passagem entre programas; confirmar atualização de agora/seguinte sem mudar de canal.
4. Comparar grelha, barra e seletor de diferidos no mesmo canal. Testar regresso à lista e troca de canal durante carregamento do guia.
5. Se necessário, testar o fuso apenas da lista afetada; as listas em seguir geral continuam com o comportamento anterior. Confirmar exportação/restauro dessas preferências.

Sem APK gerado e sem reprodução física nesta intervenção. Nenhuma promessa de eliminação de problemas do fornecedor.

Referências: [análise autorizada](EPG_LIMPEZA_CORE_1.0.63_1.0.64_ANALISE_APROFUNDADA_2026-10-06.md), [pendências](ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md), [implementação pública de player_api.php com get_simple_data_table](https://github.com/gtaman92/XtreamCodesExtendAPI/blob/master/player_api.php).
