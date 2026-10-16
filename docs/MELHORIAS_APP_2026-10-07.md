# OwnTV — melhorias da app de 7 de outubro de 2026

## Âmbito

Implementação dos pontos 1, 2, 4, 5, 6, 7 e 8 da revisão. O ponto 3, relativo à dimensão das páginas/cache de Paging, não foi aplicado. Sem novo APK, publicação ou alteração das credenciais.

## 1. Foco e animações

O editor de ordem deixa de repetir o pedido de foco assim que este é aceite. A opção de animações desligadas usa deslocação imediata. O editor usa a janela de popup existente para isolar o foco e gerir o espaço do teclado.

## 2. Ordem personalizada

- Setas para cima/baixo deslocam o canal.
- Direita/esquerda permite navegar nas opções adicionais; «Mover com as setas» regressa ao modo de deslocação.
- Posição numérica, início e fim; posições fora da lista não alteram a ordem.
- Desfazer a última alteração da sessão, incluindo a reposição da ordem.
- Restaurar a ordem da lista exige uma segunda confirmação e continua pendente até Guardar.
- Cancelar não grava a ordem; Guardar mantém a ordenação Personalizada.
- Consulta de persistência limitada ao perfil, tipo e contexto em edição; posições guardadas de fontes inativas são preservadas.

A reposição ordena os canais desse contexto pela origem, posição do fornecedor e nome. Não apaga a personalização de outras categorias/perfis. O desfazer limita-se à sessão atual.

## 4. Composição da interface

O ecrã principal acompanha apenas a abertura/fecho dos editores de ordem. Cada editor observa internamente as suas alterações. As linhas observam o título do seu programa com deduplicação por canal; o guia e os estados do painel de preview são observados no painel. Atualizações de EPG deixam de invalidar todo o ecrã da lista. Não se alteraram pedidos de EPG, regras de zapping, buffer ou qualidade.

## 5. Definições

Cinco grupos: Canais; Reprodução; Guia e diferidos; Gravações e armazenamento; Aplicação. Opções técnicas ficam em «Avançado», continuam pesquisáveis e são expandidas ao regressar de uma subpágina. As configurações por canal indicam se usam as definições gerais ou opções próprias, mantendo a herança das restantes opções.

## 6. Limpeza segura

Removidos MoviesScreen.kt e SeriesScreen.kt e as suas chamadas nos ecrãs de dados e no shell. Identificadores de navegação antigos são mantidos para compatibilidade, redirecionando para canais. Removidos comentários que apontavam para os ecrãs retirados.

Conservados modelos, tabelas, migrações, importação/exportação de backups e componentes utilizados por pesquisa, Home, reprodução de ficheiros, gravações e diferidos. Não se removeram bibliotecas sem prova de ausência de utilização. Esta limpeza reduz código; não demonstra por si só um ganho de FPS.

## 7. Diagnóstico da sessão

Definições → Aplicação → Avançado → Diagnóstico da sessão de reprodução. O mesmo relatório existente inclui versão instalada, ID da sessão/canal/origem, estado de abertura, reprodutor iniciado, tempo até confirmação de reprodução, motivo da mudança, última falha terminal e configurações resolvidas para o pedido.

Os detalhes técnicos podem ser expandidos. O estado é limitado à sessão do processo e rejeita alterações de proprietários ultrapassados. Os novos dados não incluem URL, nome de utilizador, senha, cabeçalhos ou tokens. A exportação reaplica a ocultação de dados nas URLs. Os logs HLS/HTTP existentes continuam a fornecer os detalhes de libertação e erros; a sessão não prova que o fornecedor encerrou a ligação.

Não se introduziram novos atrasos, tentativas de ligação ou mecanismos de recuperação. «Configuração» representa opções enviadas ao leitor; a confirmação do codec/decoder real continua nos diagnósticos existentes.

## 8. Medições

Além do percurso existente de arranque/lista/guia/gravações, há benchmarks separados para navegação na lista e para reabrir A→B→C→A, duas voltas, com confirmação de reprodução em cada abertura. A gravação de baseline profile também percorre as reaberturas. Não foi gerado um perfil novo nesta implementação.

Preparação e comandos: DESEMPENHO_BOX_SMARTPHONE_VALIDACAO.md. Usar fixture local sem credenciais, navegação visível e canais com nomes diferentes para não confundir a prioridade de versões com a abertura de um canal individual.

## Validação e limites

Compilação da app StandardDebug, Core/player-core e instrumentação BenchmarkRelease concluída com sucesso. Passaram 1 820 testes unitários: app 316, Core 1 061 e player-core 443, sem falhas, erros ou testes ignorados. O controlo de traduções passou e a nova consulta foi validada em SQLite quanto a isolamento de perfil/tipo/contexto e ordenação. Testes de foco/toque, teclado, apresentação e desempenho necessitam de APK num dispositivo. Comparar antes/depois no mesmo aparelho com as mesmas opções; guardar percentis de frames, tempo de abertura e falhas. Não há percentagem de melhoria medida nesta entrega.

Verificações manuais prioritárias:

1. Reordenar por setas/toque e posição; Guardar, Cancelar, Desfazer e confirmar a reposição.
2. Regressar das opções avançadas e de um diagnóstico: foco no controlo correto.
3. Abrir e fechar o teclado numérico, incluindo smartphone em orientação horizontal.
4. Verificar atualização de títulos de EPG e preview, mantendo o foco na lista.
5. A→B→C→A com apenas HLS; confirmar imagem/som e tempos no relatório.
6. Gravações, recuo no leitor, diferidos e restauração de backup continuam a funcionar.
