# Integração seletiva OwnTV 5.1.0 — categorias e pesquisa

Data: 06/10/2026. Estado: implementado; compilação e testes locais aprovados. Validação em smartphone/box pendente. Não foi gerado APK.

## Âmbito autorizado

Aplicadas as recomendações funcionais da análise `ANALISE_OWNTV_5.1.0_2026-10-06.md`, preservando as personalizações existentes. Excluídas a adaptação HLS/TS, a reformulação Stage, a retoma de timeshift local e a recuperação de diferidos #229, que continua pendente. Funcionalidades já presentes não foram reaplicadas.

## Alterações

1. **Visibilidade coerente:** ocultar uma categoria do fornecedor deixa de esconder os seus canais dentro de uma categoria personalizada visível. A regra é comum à paginação, contagens, seleção e lista de zapping. Canais individualmente ocultos e restrições infantis continuam a ser respeitados. Categorias personalizadas ocultas não expõem os seus membros.
2. **Retirar da categoria:** ação disponível no menu do canal e na personalização quando a categoria é personalizada. Remove apenas a associação, regista a remoção através de UserDataWriter e devolve o canal à origem quando deixa de pertencer a qualquer categoria personalizada. Não apaga canais nem altera grupos de versões.
3. **Pesquisa de categorias:** o texto fica no LiveViewModel e mantém-se ao abrir/fechar o leitor. É limpo ao trocar perfil ou seleção de listas; não é uma preferência persistente após reiniciar a aplicação.
4. **Categorias no leitor:** o navegador passa a incluir categorias personalizadas com nomes, ordem e ocultação próprios. A navegação mantém a correspondência entre variantes do mesmo canal. Carregamentos cancelados não podem substituir uma lista mais recente; categorias vazias preservam o contexto anterior.
5. **Pesquisa das definições:** resultados específicos para configurações por canal, versões, prioridade, desistência automática, ocultação de categorias/barra lateral, fuso de diferidos por lista, destino e limites de armazenamento. Estilo de legendas e diagnóstico encaminham para a opção concreta. Os nomes de serviços DNS abrem a página DNS; pesquisar não altera a configuração.
6. **Navegação:** seta esquerda na lista regressa às categorias quando a coluna está disponível. O evento só é consumido se o foco tiver sido obtido. O navegador de categorias do leitor volta a focar a categoria selecionada.

Não foram necessárias migrações de base de dados. Foram acrescentadas consultas aos dados existentes. Os motores, a política HLS, o buffer, o áudio e as esperas de transição entre canais mantêm o comportamento anterior a este lote.

## Validação efetuada

- Compilação `:app:compileStandardDebugKotlin`: aprovada.
- Testes da aplicação `:app:testStandardDebugUnitTest`: **311**, sem falhas nem erros.
- Testes do Core `:OwnTV_Core:core:testDebugUnitTest`: **1053**, sem falhas nem erros.
- Compilação dos testes de instrumentação do Core `:OwnTV_Core:core:compileDebugAndroidTestKotlin`: aprovada; não executados num dispositivo.
- Dez testes novos: sete de visibilidade e restrições; três de categorias personalizadas, contexto vazio e cancelamento de carregamentos.
- Oito cenários SQLite da consulta de contagem: aprovados, incluindo fonte, perfil, categoria, ocultação individual e restrições.
- Verificações de inventário de traduções e de whitespace dos ficheiros revistos: aprovadas.

Resultado conjunto: **1364 testes unitários aprovados**. Isto não substitui a validação de foco, disposição visual e reprodução numa box física.

## Teste manual recomendado

1. Associar um canal a uma categoria personalizada; ocultar a categoria original do fornecedor. Confirmar que o canal aparece e reproduz na personalizada, com contagem correta. Ocultar o canal individualmente e confirmar que desaparece.
2. Retirar o canal da personalizada pelo menu e pela personalização. Confirmar que a lista original mantém o canal e que a remoção sobrevive ao reinício; se existir associação a outra personalizada, confirmar que é preservada.
3. Pesquisar uma categoria, abrir um canal e voltar à lista. Confirmar texto preservado, foco e navegação por setas. Trocar perfil/listas e confirmar limpeza da pesquisa.
4. No leitor, escolher uma categoria personalizada, mudar de canal com cima/baixo e selecionar uma variante. Trocar rapidamente de categoria e confirmar que não reaparece uma lista antiga.
5. Nas definições, pesquisar prioridade, desistência, buffer por canal, barra lateral, fuso, armazenamento, legendas e diagnóstico. Confirmar que cada resultado chega à configuração correspondente.
6. Confirmar modo simples, categorias ocultas e restrições infantis, sem reaparecimento de favoritos, histórico, filmes ou séries nas definições.

## Ficheiros principais

- App: LiveViewModel, LiveZapList, LiveScreen, CategoryRail, CategoryBrowserOverlay, OwnTVShell, CustomizeItemsViewModel/Screen e ecrãs de definições.
- Core: LiveQueries e CustomCategoryDao.
- Traduções: channel_categories.xml em inglês e português; inventários de literais atualizados.

Para gerar o APK no Android Studio, usar o projeto `C:\Users\renat\Downloads\OwnTV-main` com o Core local `C:\Users\renat\Downloads\OwnTV_Core`, a mesma configuração validada neste lote.
