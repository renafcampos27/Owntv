# Adaptação smartphone e Android TV — implementação

Data: 2026-10-01. Âmbito autorizado: propostas 1 a 8. O ponto 9, PiP do sistema Android, foi excluído pelo utilizador.

## Comportamento entregue

| Área | Comportamento |
|---|---|
| Leitor por toque | Tocar no vídeo mostra/esconde os controlos. Ações de canal anterior/seguinte ficam disponíveis no leitor compacto. |
| Barras de tempo | Arrastar mostra a posição pretendida; apenas largar efetua um pedido de navegação. Multitoque, cancelamento, desativação e mudança de contexto rejeitam o gesto antigo. |
| Direto HLS | A atualização normal da duração/janela não cancela o arrasto; o alcance fica fixo durante o gesto. A barra nativa só permite recuar quando o motor anuncia uma janela realmente navegável. |
| HUD compacto | Nome, voltar e menu no topo; transporte no centro e barra em baixo. O menu conserva áudio, legendas, volume, velocidade quando aplicável, gravação e as restantes ações existentes. |
| Bloqueio tátil | Disponível no menu do leitor compacto. Mantém um botão de desbloqueio; Back também desbloqueia. Uma mudança de conteúdo ou erro de reprodução liberta o bloqueio. |
| Janela compacta | Abaixo de 840 dp efetivos, a navegação passa para botões horizontais e categorias para um seletor; canais usam a largura disponível. EPG e gravações distribuem ações por linhas. |
| Janela larga | Mantém os painéis e a navegação existentes. A decisão de layout depende da largura, separadamente da decisão entre toque e comando. |
| Teclado e diálogos | Smartphone conserva o teclado nativo e escala de base normal; TV conserva a lógica de comando. O host do popup desconta o teclado uma única vez e respeita barras/cutouts laterais. |
| Formulários | Adicionar/editar fonte conserva o rascunho durante rotação em memória. Apenas um identificador aleatório é salvo no estado Android; URL e credenciais não entram no Bundle. Sair descarta o rascunho. |
| Acessibilidade | Controlos comuns têm alvo mínimo de 48 dp em toque. Botões do leitor e barras expõem descrições localizadas e ações de progresso. |
| Perfil visual | Definições → Aparência → Perfil visual: Automático, Leve ou Completo. A seleção é persistente e incluída no backup. |
| Medição da interface | Opção independente em Aparência, desligada por defeito; resumo recente de dez segundos em memória, apenas enquanto a Activity está visível. Também pode ser encontrada na pesquisa das definições. |

O perfil Automático aplica orçamento leve em dispositivos low-RAM ou com até 3 GiB de RAM. Leve reduz animação, desfocagem, profundidade e brilho animado; Completo respeita as escolhas individuais. Não altera resolução, decoder, protocolo, buffers ou qualidade do vídeo. As escolhas de aparência permanecem guardadas ao alternar perfis.

A medição reporta fotogramas da interface acima do prazo de apresentação e a demora da fila de comandos. Não mede perdas de fotogramas do vídeo. Usa histogramas limitados e um resumo substituído periodicamente; não cria histórico ilimitado nem guarda URLs ou nomes de canais.

## Limites intencionais

- Não foi adicionado PiP Android; o mini-player interno existente continua disponível.
- O rascunho não é recuperado após o Android matar o processo, porque dados sensíveis ficam exclusivamente em memória. Fontes já guardadas mantêm a persistência existente.
- O preview lateral não é iniciado em janela compacta. Um preview ativo é fechado ao entrar nessa disposição, sem parar o leitor fullscreen/dock que já detém o motor.
- O toque no EPG abre o detalhe do programa pelo mesmo caminho existente; não força a sintonização do direto nem aumenta os dias de arquivo disponíveis no fornecedor.
- Não houve geração de APK, publicação, alteração de versão ou migração de base de dados nesta entrega.

## Validação automática

Os testes novos cobrem limite do layout compacto, preservação/isolamento/limpeza do rascunho, largura/escala dos popups, política visual, matemática da barra e autorização de commit único/cancelamento de gestos. A suite consolidada inclui App, Core e player-core, com verificação do inventário i18n.

Suite consolidada: **1 511 testes, zero falhas, zero erros e zero testes ignorados** — App: 263; Core: 937; player-core: 311. Compilação Kotlin, recursos Android e verificação i18n passaram. O registo da execução está em `adaptacao-smartphone-box-validation.log` na raiz da App.

## Aceitação no equipamento

1. Smartphone: abrir canal; tocar para mostrar/esconder; arrastar a barra por mais de cinco segundos e largar; confirmar uma só mudança de posição.
2. Cancelar com outro dedo e mudar de canal durante o arrasto: o gesto antigo não deve navegar no novo canal.
3. Experimentar canais sem arquivo/janela navegável: não deve ser inventada capacidade de recuar.
4. Ver EPG em retrato e paisagem; deslocar horizontalmente e tocar num programa passado; confirmar programa correto no detalhe.
5. Escrever uma fonte em adicionar/editar, rodar o dispositivo e abrir o teclado; confirmar texto preservado e ações alcançáveis. Sair e voltar deve limpar o rascunho.
6. Verificar categorias, pesquisa, listas, perfis, Mais, definições e gravações em janela pequena.
7. TV: testar setas ↑/↓, OK, Back, zapping, barras e diálogos com teclado do comando.
8. Alterar perfil visual, fechar/reabrir e exportar/restaurar backup; confirmar a seleção. Medir desempenho apenas durante a comparação e desligar depois.

Não houve ensaio físico de toque/IME/rotação, TalkBack ou comando nesta entrega. Compilação e testes unitários não comprovam fluidez nem comportamento OEM na Xiaomi/Thomson.
