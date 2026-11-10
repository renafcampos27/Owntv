# Logótipos de canais — armazenamento persistente e apresentação leve

Implementação iniciada em 09/11 e concluída em 10/11/2026. OwnTV Android TV/Core personalizado. Pedido: manter os logótipos disponíveis, atualizá-los com a lista e reduzir o custo de armazenamento/apresentação.

## Comportamento implementado

- Uma preparação em segundo plano obtém os logótipos dos canais, por páginas de 200 referências. Não carrega URLs de streams, palavras-passe ou objetos completos de canais nesta consulta.
- Cada imagem remota é descodificada uma vez para, no máximo, **192 × 192 píxeis**, convertida para **WebP, qualidade 90**, com transparência. A apresentação reutiliza essa miniatura em vez do ficheiro remoto original.
- Os ficheiros ficam em `Context.noBackupFilesDir/channel-logos`, privados à App. Sobrevivem ao fecho, reinício, atualização normal do APK e limpeza da cache temporária do Android. Desinstalar ou limpar os dados da App remove-os. São dados derivados, excluídos do backup automático do Android; as listas/URLs continuam no contrato de backup existente.
- As miniaturas têm um teto de **64 MiB** no disco; cada miniatura tem, adicionalmente, um limite de 192 KiB. Metadados pequenos e temporários de escrita são separados desse teto. O espaço não é pré-alocado.
- A atualização bem-sucedida de uma lista altera `lastSyncAt`, que agenda a verificação dos logótipos. URLs novas são obtidas; URLs iguais são revalidadas com **ETag/Last-Modified**, quando disponíveis. HTTP 304 mantém a mesma imagem, sem nova descodificação ou compressão.
- Uma falha HTTP, corpo inválido, erro de descodificação ou falha de armazenamento **não apaga a última imagem válida**. Quando não existe nenhuma imagem válida, conserva-se o substituto visual da interface. Não se inventa um logótipo para URLs ausentes/inválidos.
- O primeiro preenchimento precisa de acesso ao fornecedor. Após estar guardado, o logótipo pode ser apresentado sem Internet. Arrancar novamente a App com a mesma lista não descarrega outra vez as imagens válidas.

## Controlo de recursos e concorrência

1. A rede dos logótipos tem pool/dispatcher próprios, derivados do cliente existente: preserva proxy, DNS, TLS e identidade HTTP. Usa o User-Agent da lista quando definido; de resto herda o cliente habitual. Não copia indiscriminadamente credenciais/headers dos streams para servidores de imagens.
2. Um **semáforo de duas operações** limita efetivamente os downloads e a conversão. O limite não depende só de `Dispatcher.maxRequests`, porque as chamadas são síncronas. Chamadas ao mesmo URL são serializadas e a segunda reutiliza o resultado já guardado.
3. Cada corpo HTTP é limitado a **2 MiB**, incluindo respostas sem comprimento conhecido; timeout total de 10 s e leitura de 8 s. Rejeição/cancelamento fecha o pedido antes de continuar.
4. Um pedido associado a uma linha é filho da subscrição dessa linha. Abandoná-la cancela o pedido pendente/em curso; não cancela a preparação independente da lista. Uma revalidação lenta não impede mostrar imediatamente a cópia anterior do disco.
5. Falhas de imagens em falta têm backoff de dez minutos para pedidos visíveis. As retomas do worker não repetem as falhas já tentadas na mesma geração da lista. **Retry-After explícito em 429/503 é persistido e respeitado**, inclusive após reinício ou nova atualização da lista.
6. O worker usa restrições de rede ligada/armazenamento suficiente, orçamento de sete minutos por tentativa e retomas limitadas. Listas internacionais muito grandes têm preparação limitada a cerca de 8192 referências por passagem; imagens adicionais são obtidas quando aparecem na interface. Fontes com TV desativada não são pré-carregadas.
7. Ficheiros de conteúdo são imutáveis, identificados por hash. A escrita termina com `fsync` e a troca dos metadados é atómica. Uma atualização interrompida deixa a imagem anterior utilizável. Revisões antigas/órfãs são limpas; a quota elimina imagens antigas quando necessário.
8. A UI reutiliza o carregador Coil existente e a sua cache de memória. Os pedidos locais têm tamanho explícito, sem crossfade; não fazem I/O de disco ou rede em cada fotograma. A identidade só muda quando muda o conteúdo. Não se recalcula o hash durante cada recomposição.

## Cobertura da interface e compatibilidade

A mesma cópia local é usada na lista de canais e respetivas sobreposições, guia, leitor/HUD, OSD, barra de áudio, barra lateral, gravações, resultados de pesquisa de canais e cartões de TV da página inicial. Continua a respeitar `displayLogoUrl` e a escolha existente de logótipos do EPG. Imagens locais/content URIs mantêm o percurso local; posters de outros conteúdos conservam o carregamento anterior.

Foi adicionado o [módulo SVG oficial do Coil](https://coil-kt.github.io/coil/svgs/), na versão **3.6.0**, alinhada com o Coil já utilizado. A expansão de entidades XML é desativada antes da criação do carregador. Não se instala resolver de ficheiros externos. O resultado SVG também é rasterizado e guardado como miniatura; não é interpretado novamente a cada apresentação.

O [carregador singleton e as caches do Coil](https://coil-kt.github.io/coil/image_loaders/) continuam a ser reutilizados. A cache persistente é específica dos logótipos; a política de posters não foi substituída.

## Ficheiros alterados

Raiz App: `C:/Users/renat/Downloads/OwnTV-main/`.

| Ficheiro | Responsabilidade |
|---|---|
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoDiskCache.kt` — novo | Miniaturas imutáveis, metadados, escrita atómica, quota, falhas e revalidação. Não guarda URLs/credenciais nos nomes de ficheiros. |
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoRepository.kt` — novo | Leitura local, deduplicação, cancelamento por subscrição, downloads limitados, validação HTTP e conservação da imagem válida. |
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoImages.kt` — novo | Conversão Android para WebP e componente partilhado da UI. |
| `app/src/main/java/tv/own/owntv/ui/images/ChannelLogoWorker.kt` — novo | Observa as gerações das listas e prepara imagens através de WorkManager, sem depender de uma Activity. |
| `app/src/main/java/tv/own/owntv/OwnTVApp.kt` | Agenda a manutenção; configura SVG com entidades XML desativadas. Falhas desta manutenção não abortam o arranque. |
| `app/src/main/java/tv/own/owntv/di/AppModule.kt` | Regista um repositório de logótipos por processo. |
| `app/src/main/java/tv/own/owntv/ui/components/ChannelLogoTile.kt` | Remove a falha memorizada apenas pelo URL e utiliza a cópia local partilhada. |
| `app/src/main/java/tv/own/owntv/features/epg/EpgScreen.kt` | Guia utiliza a mesma miniatura, podendo recuperar após atualização do conteúdo. |
| `app/src/main/java/tv/own/owntv/player/PlayerHudChrome.kt` | HUD/OSD reutilizam a miniatura e mantêm o substituto textual enquanto necessário. |
| `app/src/main/java/tv/own/owntv/player/AudioNowPlayingBar.kt` | Miniatura local na barra de áudio. |
| `app/src/main/java/tv/own/owntv/features/shell/components/Sidebar.kt` | Miniatura local na barra lateral. |
| `app/src/main/java/tv/own/owntv/features/recordings/RecordingsScreen.kt` | Miniatura local para o canal da gravação. |
| `app/src/main/java/tv/own/owntv/features/search/SearchScreen.kt` | Pesquisa de canais utiliza o novo percurso, sem converter posters em logótipos. |
| `app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt` | Cartões de TV usam a cache e respeitam o logótipo efetivo do EPG. |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` | Extensão SVG alinhada; acesso direto ao seu AndroidSVG 1.4 para configurar o parser antes da utilização. |
| `gradle/verification-metadata.xml` | Oito artefactos verificados no Maven Central, com SHA-256 local comparado com os checksums publicados; chave de assinatura AndroidSVG autorizada apenas para `com.caverock:androidsvg-aar:1.4`. Verificação estrita preservada. |
| `tools/i18n/safe_literals.txt` | Apenas os literais técnicos novos. Sem novas mensagens visíveis nem aumento da baseline de tradução. |
| `core-source-lock.json` e ZIP em `vendor/` | Integração dos inputs Core validados. |

Raiz Core: `C:/Users/renat/Downloads/OwnTV_Core/`.

- `core/src/main/java/tv/own/owntv/core/database/dao/ChannelDao.kt`: projeção `ChannelLogoRef` e consulta paginada `logoPage`. Só leitura; não altera tabelas ou dados.
- `core/src/main/java/tv/own/owntv/core/network/BlockingCallCancellation.kt`: torna público o helper de cancelamento já existente para o reutilizar na App. O corpo do helper mantém-se igual; não foi criada outra lógica de cancelamento.
- `tools/i18n/safe_literals.txt`: nova consulta SQL classificada como literal técnico.

**Schema 51, formato de backup 26, AGP 9.5.1, Media3 1.11.1 e libmpv 2026.11.0 mantidos.** Não altera a sintonia, áudio, HLS, gravações ou timeshift. Foram comparados 300 inputs de player/gravações/timeshift com o estado anterior: byte a byte iguais.

## Validação concluída e limites

- **96 testes JVM de rede do Core aprovados.**
- **492 testes JVM da App aprovados**, incluindo **18 novos** sobre reinício, cache, quota, atualização/304, falhas, cancelamento de linhas, concorrência, User-Agent e Retry-After.
- Total desta execução: **588 testes JVM aprovados**, zero falhas/erros/skips.
- Compilação Standard Release e `lintVitalStandardRelease` aprovados.
- Três testes Android de PNG/transparência, SVG e HTML inválido foram **compilados, não executados**. A conversão real de bitmaps e a apresentação/foco na box continuam a exigir teste no dispositivo.
- Snapshot Core: **1116 inputs**, SHA-256 `372e6d340b96e5ff2dad1505878f088ff32ff4650e790dfcec01f7d73ef47823`. ZIP, Core externo e extração usada pela App comparados. Classes Debug/Release confirmadas com a implementação final de cancelamento; hashes dos fontes mantidos durante a validação final.
- Backups e provas: `C:/Users/renat/Downloads/OwnTV-logos-2026-11-09-233753/`, incluindo `validation-final.json`, `snapshot-verification.json`, logs, resultados XML e `svg-maven-checksums.json`.
- O aviso geral de funcionalidades Gradle obsoletas antes da versão 10 mantém-se; não foi suprimido. Não foi executada a suite global do Core, cujas falhas de gravação anteriores estão documentadas separadamente.

Não foi gerado/instalado APK nem feito commit/push. Sem medições físicas, não se atribui uma percentagem de aceleração. Para testar: gerar o APK no Android Studio, permitir a primeira preparação dos logótipos, percorrer lista/guia/leitor, fechar/reabrir e verificar novamente sem Internet. Uma atualização da lista deve renovar imagens alteradas e conservar a cópia anterior se o fornecedor falhar.
