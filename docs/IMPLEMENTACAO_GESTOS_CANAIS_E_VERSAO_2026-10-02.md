# Gestos de canais e versão/atualizações

Implementado em 2026-10-02 no projeto local OwnTV-main, com recursos no OwnTV_Core local.

## Lista principal de canais

| Gesto no canal | Resultado |
|---|---|
| Pressão inferior a 2 s e soltar | Reprodução normal |
| Manter 2 s e soltar antes de 3 s | Escolher uma versão visível do mesmo canal |
| Manter até 3 s | Menu completo existente do canal |

A escolha intermédia é decidida ao soltar, evitando que um popup aos 2 s capture o comando e impeça chegar ao menu de 3 s. O menu pode abrir aos 3 s sem depender de eventos de repetição do comando. A libertação posterior não reproduz o canal nem confirma uma opção por engano.

As mesmas durações aplicam-se ao toque na lista. Arrasto, perda de foco, cancelamento e remoção da linha cancelam o gesto. Só uma linha pressionada possui um temporizador; não foram adicionados temporizadores aos canais em repouso.

O seletor rápido mostra apenas nomes reproduzíveis, exclui versões ocultas e não permite editar prioridades. A reprodução só é solicitada ao escolher uma versão. O seletor de gestão continua acessível pelo menu completo e conserva os controlos de prioridade existentes. Preservada a navegação por canais após abrir o leitor.

## Versão e atualizações

Definições e Mais usam o mesmo ecrã, com versão instalada, verificação ao iniciar, procura manual e decisão de atualizar/adiar quando existe uma versão disponível. Os detalhes não iniciam uma instalação. O verificador existente continua a consultar renafcampos27/Owntv.

Removidos desse ecrã o Telegram, QR, publicidade/comunidade e ligação do projeto original. Conservada uma linha de licença. O painel lateral de detalhes deixa de mostrar a contagem de idiomas e o repositório original.

## Validação

- Compilação standardDebug aprovada, sem geração de APK.
- App: 302 testes; Core: 995; zero falhas, erros ou testes ignorados.
- Oito testes novos cobrem os limites de 2/3 s, repetição de KeyDown, abertura única do menu, libertação posterior, cancelamento e KeyUp sem pressão anterior.
- Inventário de literais, recursos/traduções, overflow de texto e locale numérico aprovados. Sem aumento do baseline de texto não traduzido.
- Teste físico de comando e toque não executado; os testes JVM verificam a decisão temporal e a compilação verifica a integração, sem substituir gestos reais em Android.

Log: `%TEMP%/owntv-channel-hold-settings-final-validation.log`.
Backup: `%TEMP%/owntv-channel-hold-settings-before-20261002-230353`.

As propostas de isolamento da fila de streaming, recuperação de ligações e junção de comandos rápidos foram explicadas; não integram este lote de interface. Sem alteração de versão, publicação ou instalação.
