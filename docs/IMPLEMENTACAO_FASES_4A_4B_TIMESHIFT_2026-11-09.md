# Fases 4A e 4B — integridade do timeshift local

Data: 09/11/2026. Autorização: «implementa o 4a e 4b». Âmbito: P10/P11a do plano HLS de 03/11, revisto pelos critérios de paridade da secção 16.

## 1. Resultado e limites

A cópia temporária local passa a publicar um `EXT-X-TARGETDURATION` fixo para a sessão. Se uma atualização trouxer um segmento que não cabe nesse target, termina a captura controladamente, mantendo o conteúdo válido. O mesmo URL local nunca muda silenciosamente de apresentação.

Segmentos novos com chave AES ativa são verificados no início da resposta que já seria descarregada. Conteúdo incompatível com o percurso MPEG-TS claro é recusado antes da escrita e da reserva/eviction de disco. Uma tag AES incorreta sobre TS claro continua aceite. Chaves em segmentos já consumidos e GAP não provocam rejeição nem pedidos de chaves.

A App aguarda o fim da captura e a libertação da reserva de ligação antes do aviso. Se já existem segmentos completos, o leitor local atual conserva-os, com manifesto finalizado, sem manter uma reserva de ligação ao fornecedor. Uma conclusão atrasada não interfere com o canal/perfil novo.

**Não foi implementada descifra AES no timeshift.** O suporte completo continua pendente em P11b/6D. Não foram alterados HLS direto, codecs, áudio, volume, buffers, latência, reabertura de canais, TLS, user-agent, gravações guardadas, arquivos do fornecedor ou a base de dados. Não há novas definições, diálogo ou abertura automática de outro stream. A seleção normal do canal permite voltar ao direto, como informa a mensagem existente.

## 2. Falhas reproduzidas antes das alterações

Foram executados cinco testes novos contra a implementação anterior: quatro falharam e um passou.

| Caso | Antes | Depois |
|---|---|---|
| Remover da janela o segmento de maior duração | O target diminuía de 8 s para 4 s | Mantém 8 s |
| Chegar um segmento acima do target estabelecido | Aceitava e alterava a apresentação; podia eliminar história válida | Recusa antes de transferir/evictar; preserva os segmentos anteriores |
| EXTINF local de 6,499 s | Arredondava sempre para cima, para 7 s | Arredondamento ao inteiro mais próximo: 6 s; 6,500 s exige 7 s |
| Corpo AES incompatível, comprimento desconhecido | Descarregava o corpo completo antes da validação final | Inspeção curta; cancela o pedido e fecha a resposta antes da escrita |
| Tag AES sobre MPEG-TS já claro | Era aceite | Continua aceite; este teste impede a rejeição por tag isolada |

Provas anteriores: `C:/Users/renat/Downloads/OwnTV-fase4ab-2026-11-09-231154/reproduction-before.log` e `reproduction-before-results/`.

## 3. Alterações exatas de produção

Os caminhos Core abaixo são relativos a `C:/Users/renat/Downloads/OwnTV_Core/`; os da App, a `C:/Users/renat/Downloads/OwnTV-main/`.

| Ficheiro | Alteração |
|---|---|
| Core `core/src/main/java/tv/own/owntv/core/timeshift/LocalTimeshiftTarget.kt` — novo | Lê a tag original do texto já obtido, separada do fallback do parser. Deriva um target inteiro positivo a partir da declaração utilizável e das durações da janela inicial elegível. Tags ausentes, inválidas, zero/negativas ou fora do limite estrutural da janela não causam nova rejeição; declaração subestimada é coberta pelas durações. |
| Core `core/src/main/java/tv/own/owntv/core/timeshift/LocalSegmentStore.kt` | Inicialização interna do target, idempotente apenas para o mesmo valor; manifesto usa esse valor estável. Validação da duração arredondada antes de qualquer eviction no commit. Utilização independente da store deriva o valor do primeiro commit; o produtor inicializa com a janela completa. Quota, leitores, sequences e descontinuidades preservados. |
| Core `core/src/main/java/tv/own/owntv/core/timeshift/LocalTimeshiftSession.kt` | Transporta a declaração original no snapshot sem guardar o texto inteiro. Inicializa o target e valida cada candidato antes do pedido, incluindo candidatos substituídos por refresh. Verifica apenas o segmento elegível com chave ativa; cancela respostas recusadas antes de `close`, evitando drenagem para reutilizar a ligação. Mantém cancelamento, budgets, Retry-After e os retries existentes. |
| Core `core/src/main/java/tv/own/owntv/core/timeshift/LocalTimeshiftInput.kt` — novo | Inspeciona até 189 bytes e repõe-os para a cópia normal. Usa os mesmos dois sync bytes e o mesmo alinhamento de 188 bytes da validação MPEG-TS anterior. Para comprimento desconhecido mantém a validação final após EOF. Não é detetor criptográfico nem novo parser de codecs. |
| App `app/src/main/java/tv/own/owntv/features/live/LiveViewModel.kt` | Falha inicial aguarda o cleanup antes de avisar. Falha após preparação termina/junta o produtor e liberta o claim da própria sessão antes de avisar, mantendo a janela local já válida. Não para globalmente outro leitor após uma suspensão. |
| App `app/src/main/java/tv/own/owntv/features/live/LocalTimeshiftFailure.kt` — novo | Pequena sequência testável: confirmar proprietário → aguardar paragem → reconfirmar proprietário → libertar claim → avisar. Não introduz estado persistente. |
| Core `tools/i18n/safe_literals.txt` | Classifica apenas a nova ocorrência técnica de `#EXT-X-TARGETDURATION:`. A baseline de dívida de tradução mantém exatamente os mesmos bytes. |
| App `core-source-lock.json` e novo ZIP em `vendor/` | Integram os inputs Core validados na build da App. O snapshot anterior foi preservado. |

O target não passa a ser um máximo histórico crescente. Uma duração futura incompatível finaliza a captura local; não abre outra apresentação no mesmo URL nem bloqueia o HLS direto. O arredondamento segue a [RFC 8216, secções 4.3.3.1 e 6.2.1](https://www.rfc-editor.org/rfc/rfc8216.html).

### Adaptação deliberada ao plano original

Recusar antes de obter qualquer byte só com base na tag AES quebraria um caso já aceite, demonstrado pelo teste anterior. A recusa foi por isso adaptada: inspeção limitada **dentro do mesmo pedido**, sem request de sondagem e antes de escrever/evictar. A identificação baseia-se na incompatibilidade com o percurso TS claro existente; não afirma provar que todo corpo recusado esteja cifrado.

Após falha com media já publicada, não se fecha imediatamente o servidor local nem se apaga a janela. `retainCopy()` termina/junta o produtor e conserva os segmentos finalizados; o claim upstream é libertado antes do aviso. A limpeza normal continua responsável pelo servidor/ficheiros quando se abandona essa sessão. Isto preserva os dados e evita uma segunda ligação automática.

## 4. Validação

- **Core: 101 testes dirigidos aprovados**, zero falhas/erros/skips: `tv.own.owntv.core.timeshift.*` e `tv.own.owntv.core.recording.Hls*`.
- Inclui 15 testes novos de target/rotação, arredondamento, metadados não canónicos, publicação, preservação de história/leitor, AES claro/cifrado, cancelamento, histórico de chaves e GAP.
- **App: 474 testes aprovados**, zero falhas/erros/skips. Inclui quatro testes novos de ordem de fecho/libertação/aviso, mudança de proprietário durante a espera, conclusão já obsoleta e cancelamento.
- **Compilação `:app:compileStandardReleaseKotlin` aprovada** com o novo snapshot; classes novas confirmadas nos JARs compilados Debug e Release do Core incluído.
- **`:app:lintVitalStandardRelease` aprovado**. A invocação conjunta terminou com `BUILD SUCCESSFUL`, 143 tarefas, em 8 min 39 s.
- Total: **575 testes JVM aprovados**, dos quais **19 novos**. Ver `core-validation.log`, `core-validation-results/`, `app-validation.log`, `app-validation-results/` e `snapshot-verification.json` na pasta de provas.
- Verificação estrita de dependências mantida. A build continua a emitir o aviso geral de funcionalidades Gradle obsoletas antes da versão 10; não foi contornado nem se alteraram ferramentas nesta fase.

Não se executou a suite global do Core: as dez falhas anteriores de `RecordingEngineOperationsTest` são assunto separado, já documentado em UP70-A/B/C. Os ficheiros de gravação e `player-core` não foram alterados nesta fase.

### Snapshot e provas

- Core integrado: `e5796ee0f13439d1acc8ef833f3b369bce3aeb2000696ea61a7c0179e7333ded`, **1116 inputs**.
- ZIP, checkout Core externo e extração usada pela build: **1116 inputs byte a byte idênticos**.
- **279 inputs de player/gravações** mantêm os hashes anteriores.
- Backup anterior verificável e logs: `C:/Users/renat/Downloads/OwnTV-fase4ab-2026-11-09-231154/`.
- Mantidos AGP 9.5.1, Media3 1.11.1, libmpv 2026.11.0, schema 51 e formato de backup 26. Não há equivalência upstream adicional.

## 5. Aceitação física pendente

1. Ativar o recuo local num canal HLS/MPEG-TS claro; pausar, recuar e continuar após rodar a janela. Confirmar timeline e som/imagem habituais.
2. Trocar de canal/perfil durante a preparação e durante uma falha local: a sessão nova não deve receber avisos nem paragens da antiga.
3. Num canal incompatível com a captura local, confirmar o aviso; selecionar normalmente o canal e confirmar o direto sem ligação local paralela.
4. Confirmar que os diferidos do fornecedor e as gravações guardadas continuam a abrir pelos percursos habituais.

Sem APK gerado, instalação, assinatura, commit ou push. Estes resultados são validação automatizada; não medem melhoria percentual nem confirmam comportamento físico nas boxes.
