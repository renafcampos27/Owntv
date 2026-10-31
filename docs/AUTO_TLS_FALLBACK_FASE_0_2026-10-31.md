# AUTO_TLS_FALLBACK — Fase 0: auditoria do Core personalizado

Data: 31-10-2026. Entrega A da especificação `OWNTV_AUTO_TLS_FALLBACK_ESPECIFICACAO_FINAL_2026-10-31.md`.

Continuação: a [entrega V1](AUTO_TLS_FALLBACK_V1_2026-10-31.md) implementa hostname automático e regista a resolução do bloqueio de dependências. Este relatório conserva os resultados históricos da auditoria inicial.

**Estado: auditoria estática concluída; baseline Android bloqueado por verificação de uma dependência preexistente. Nenhuma alteração funcional de TLS foi aplicada.** A V1 e a V2 continuam pendentes. O incidente do fornecedor é histórico e foi dado como resolvido pelo utilizador; não foi reproduzido nem provocado sobre o fornecedor.

## 1. Código efetivamente auditado

| Item | Resultado verificado |
|---|---|
| App local | `C:\Users\renat\Downloads\OwnTV-main` |
| Commit App inicial | `1ae0279be7d3a2c464588092d00896da2a4e9377`; árvore limpa antes desta entrega |
| Core local | `C:\Users\renat\Downloads\OwnTV_Core` |
| Commit base Core | `e7a044eaccf96f2143852028c1e207bed8cedcd3`; 666 entradas locais modificadas/não rastreadas, preservadas |
| Fonte de verdade | `core-source-lock.json` + ZIP personalizado; o commit base do Core não descreve sozinho as personalizações |
| Versão técnica | `1.0.57-own.20261011` |
| ZIP | `vendor/owntv-core-source-e96eca676488a1599d43f021f2ec6531af0fcc5ddba6ff1a0f359deb4f1b5280.zip` |
| SHA-256 | `e96eca676488a1599d43f021f2ec6531af0fcc5ddba6ff1a0f359deb4f1b5280` |
| Integridade | ZIP extraído numa pasta temporária independente; 1.071 entradas, nomes e bytes iguais aos 1.071 inputs do checkout personalizado |
| Bibliotecas declaradas | OkHttp 5.5.0, Media3 1.11.1, libmpv 2026.10.4 |

Foi utilizado o checkout existente e o ZIP extraído, em vez de substituir o projeto por um clone upstream. Os ficheiros listados abaixo foram lidos no checkout; a igualdade integral com o ZIP foi verificada e o inventário dos construtores foi repetido na cópia extraída. Não se alterou o ZIP nem o lock nesta fase.

## 2. Percurso real de reprodução e lacuna de contexto

```text
ChannelEntity.sourceId = identificador persistente da lista IPTV
  -> LiveViewModel.liveTuner / MultiviewState -> LiveViewModel.tuneTile
  -> LiveTuneController.start / playTile
  -> host.sourceOf(channel.sourceId) -> SourceEntity
  -> LiveTuneController.request(channel, source)
  -> LiveRequest                       [aqui NÃO transporta o ID da lista]
  -> LiveEngines / EnginePair -> LivePreviewEngine.play(TuningRequest)
  -> httpDataSourceFor -> PlaybackHttpCalls.factory
  -> OkHttpDataSource.Factory -> diagnosticHttpClient
  -> StreamingHttpClient.client -> OkHttp nativo
```

**Distinção obrigatória:** `TuneToken.sourceId`, em `TuneOwnership.kt`, é um contador de geração de MediaSource, incrementado por `nextSource()`. Não é `SourceEntity.id`. Usá-lo como chave de autorização TLS misturaria identidades e tornaria a cache incorreta. `diagnosticOwner` e `choiceId` também não substituem o ID da lista.

O ID correto existe no controlador, mas não chega atualmente ao transporte Live como contexto explícito. A V1 deve acrescentar um contexto imutável ao `LiveRequest`, passá-lo pelo `TuningRequest` e capturá-lo ao construir a fábrica da sessão. Uma variante de outra lista recebe o contexto dessa outra lista. A ausência de contexto válido resulta em STRICT.

## 3. Inventário efetivo e limites de integração

Os caminhos Core são relativos à raiz do ZIP/Core; os caminhos App à raiz da App.

| Ficheiro/ponto | Comportamento atual e decisão |
|---|---|
| Core `core/.../di/DataModule.kt:44–80` | OkHttp geral com proxy/DNS/UA, HTTP/1.1 e `ExtraTrustAnchors`. Manter estrito; não instalar aqui o fallback. |
| Core `core/.../network/ExtraTrustAnchors.kt:39–65` | Trust manager valida primeiro com as raízes do sistema, depois com as raízes adicionais. Preservar integralmente nos modos STRICT e HOSTNAME_ONLY. |
| Core `core/.../network/StreamingHttpClient.kt:22–59` | Deriva do cliente geral; tem pool próprio e evicção assíncrona. É partilhado por vários motores; instalar uma exceção incondicional aqui alargaria indevidamente o âmbito. |
| Core `player-core/.../LiveTuneController.kt:164,351,939` | Possui canal e lista, prepara configurações e também sintoniza Multiview. Local principal para criar o contexto de lista. |
| Core `player-core/.../LiveEngines.kt:7,96` | `LiveRequest` não tem contexto TLS/ID da lista; adaptador encaminha para LivePreviewEngine. |
| Core `player-core/.../LivePreviewEngine.kt:637,3243,3498` | TuningRequest e fábrica HTTP por sessão; cliente com diagnóstico, backoff, deadlines e guardas existentes. Preservar ordem/semântica dos interceptors e a propriedade dos pedidos. |
| Core `player-core/.../PlaybackHttpCalls.kt:33` | Já existe `Call.Factory` útil: cria Calls nativas, acompanha cancelamento/fecho e invalida gerações antigas. Não substituir, remover nem criar implementação própria de `Call`. |
| Core `player-core/.../TuneOwnership.kt:4–17` | Identidade técnica da tentativa/MediaSource; nunca interpretar como identidade de playlist. |
| App `features/live/LiveViewModel.kt:1290,1365` | Usa o controlador comum no fullscreen/preview Live e nos tiles. Integrar o contexto no caminho comum, evitando lógica TLS duplicada na UI. |
| App `features/multiview/MultiviewState.kt:186–204` | Admissão de ligações por lista, depois tune de cada tile. Cada tile tem de manter o seu próprio contexto. |
| Core `player-core/.../HeroPreviewEngine.kt:77,538` | Cliente/pool e controlo de cancelamento próprios; HomeViewModel já passa `sourceId`. Não herdar automaticamente permissões de Live para trailers/URLs de terceiros. Fora da primeira integração Live. |
| Core `player-core/.../ExoSubtitleEngine.kt:547,568` | Cliente derivado com deteção de arquivo; também suporta ficheiros/legendas locais. Fora da primeira integração Live; integração de diferidos exige contexto próprio depois. |
| Core `player-core/.../OwnTVPlayer.kt:2614` | Cria mpv por `MPVLib.create`; não há `tls-verify=no` nem outra atribuição explícita de `tls-verify` no código personalizado pesquisado. Preservar nesta entrega. |
| Core `core/.../network/DnsConfig.kt:86` | Cliente auxiliar para DNS/DoH; não é transporte de vídeo e fica fora do fallback. |
| Core `core/.../timeshift/LocalTimeshiftSession.kt:38` | Captura HTTP com pool próprio. Distinguir a captura remota do servidor loopback local; sem herança automática da política Live. |
| Core `core/.../recording/RecordingEngine.kt:69` e `download/DownloadEngine.kt:43` | Recebem OkHttp geral por DI; ficam estritos e inalterados na V1 Live. |
| Core `core/.../settings/SettingsRepository.kt` | Há DataStore, preferências por fonte e infraestrutura de backup/remapeamento de IDs. Reutilizar; não é necessário migrar a base Room só para guardar a política. |

Login, Xtream/Stalker API e resolução de URL, EPG, imagens, updates, downloads e gravações não devem passar a usar o cliente permissivo. No código auditado não há instalação de `CertificatePinner` personalizado; isso não permite ignorar pinning nos testes. As licenças DRM usam o provider padrão Media3: o fonte publicado 1.11.1 confirma uma `DefaultHttpDataSource.Factory` própria quando não há override. Manter essa separação.

**HTTP já existente:** o manifesto da App já declara `usesCleartextTraffic="true"`, necessário a listas HTTP existentes. Esta fase não o introduziu. A V1 não deve remover suporte HTTP legítimo nem aproveitar essa declaração para transformar uma falha TLS em downgrade HTTPS→HTTP. Redirects de sessões AUTO exigem ensaios específicos.

## 4. OkHttp e mpv: o que ficou demonstrado

### OkHttp Android 5.5.0

O AAR local `com.squareup.okhttp3:okhttp-android:5.5.0` tem SHA-256 `6c7fd12f092e64ca2eae0b8a8023900c0d7ffec57888cf8abc8085c6f42e1dcc`. Extraí o `classes.jar` e inspecionei `okhttp3.Interceptor$Chain` com `javap`: existem **withHostnameVerifier, withConnectionPool e withSslSocketFactory**. Os catálogos Core/App declaram 5.5.0. Uma execução separada de `:OwnTV_Core:player-core:dependencyInsight` no `releaseCompileClasspath`, offline e com verificação estrita, confirmou a seleção efetiva de `okhttp-android:5.5.0`, inclusive para o Media3 que pede transitivamente 4.12.0: `BUILD SUCCESSFUL in 6s`. Isto confirma o classpath do leitor, mas não substitui o baseline completo bloqueado abaixo.

O fonte oficial da tag 5.5.0 confirma overrides em application interceptor e rejeita alterações TLS/pool em network interceptor. A implementação permite estruturalmente mais de um `proceed` em application interceptors. **Isto não prova a recuperação após handshake falhado:** a V1 deve demonstrar esse caso num servidor TLS controlado antes de o ligar ao player.

### mpv personalizado

Não foi encontrado `tls-verify=no` no ZIP/Core personalizado, ao contrário da hipótese upstream da especificação. O bytecode Java/Kotlin de `MPVLib` no AAR 2026.10.4 também não contém essa atribuição; a criação acaba numa função JNI nativa. A ausência da atribuição no wrapper não demonstra o valor efetivo aceite pela biblioteca nativa.

**Estado efetivo de TLS mpv: não medido.** Não declarar nem STRICT nem UNVERIFIED a partir desta auditoria. Sem dispositivo ADB ligado, não foi possível ler a opção efetiva nem executar o ensaio RTP 2/mpv. Não se usaram contas, URLs ou credenciais do fornecedor para provocar falhas.

## 5. Estratégia mínima para a V1

1. Adicionar política DataStore `STRICT` / `AUTO_HOSTNAME`, padrão STRICT, com override por fonte usando a infraestrutura já existente; incluir backup/restauro e remapeamento do ID da lista.
2. Transportar `PlaybackTlsScope` imutável com ID persistente da lista e identidade efémera da sintonia. Usar tags locais no pedido/fábrica existente ou cliente por sessão; nunca headers artificiais enviados ao servidor.
3. Criar um application interceptor só no caminho de reprodução elegível. Sem scope/autorização, deixar a validação estrita. Preservar `PlaybackHttpCalls`, guards, DNS/proxy, UA/Range/headers e a fábrica Media3 existente.
4. Classificar mismatch por verificador estrito instrumentado e por tentativa; classe/texto de exceção isolados não autorizam bypass. Trust manager e pinning continuam ativos. Exceção apenas para o host exato observado.
5. Cache limitada, inicialmente em memória, por lista+esquema+host+porta. TTL inicial 15 min; pools separados para modos estrito e hostname. Single-flight e épocas de atualização impedem resultados antigos de restaurar exceções já retiradas.
6. Revalidar no pedido natural após TTL, sem abrir uma sessão IPTV paralela. **Pool novo prova socket novo, mas não prova sozinho uma cadeia de certificado nova em presença de resumption TLS.** O teste tem de observar a verificação estrita e o certificado/handshake efetivo; não basta consultar a cache ou receber HTTP 200.
7. Mostrar a opção/aviso uma vez nas Definições, com eventos sanitizados por transição. Sem prompts por canal, sem log por segmento, sem modificar mpv nesta V1.

### Gates antes de entregar a V1

- Corrigir/verificar o bloqueio de baseline sem desativar verificação de dependências.
- Provar `proceed` após falha de hostname em TLS real com o OkHttp 5.5.0; se falhar, avaliar delegação de DataSource, preservando os Calls nativos existentes.
- Provar ciclo hostname errado → fallback → certificado corrigido → handshake estrito novo → STRICT → nova falha, com relógio de teste.
- Recusar cadeia inválida/expirada/autoassinada, pinning, protocolo TLS, DNS, timeout e HTTP 401/403/404 na V1.
- Provar isolamento de fontes/tiles, cancelamento no zapping, redirects e headers sensíveis; nenhuma ligação permissiva reutilizada no caminho estrito.
- Cumprir V1-01 a V1-16 da especificação com resultados reais, distinguindo testes automatizados dos ensaios de box; regenerar ZIP/lock apenas depois de alterar e validar o Core.

## 6. Validação desta entrega

| Verificação | Resultado atual |
|---|---|
| SHA, extração, nomes e bytes ZIP/checkout | PASSOU: 1.071/1.071 inputs iguais |
| API do OkHttp Android 5.5.0 | PASSOU: três overrides encontrados no binário |
| Classpath Release do player-core | PASSOU: `dependencyInsight` selecionou OkHttp Android 5.5.0, verificação estrita, build em 6 s |
| Testes Python de snapshot | PASSOU: 6 testes |
| Testes Python de ferramentas release/dependências | PASSOU: 17 testes |
| Baseline JVM App/Core/Player + compilação Release + lint vital | BLOQUEADO antes da execução: verificação de `org.apache.groovy:groovy-bom:4.0.29`, `groovy-bom-4.0.29.pom` |
| Relatório de configuration cache | Também falha ao serializar AAPT2 porque a resolução acima falhou; não atribuir a TLS nem afirmar uma segunda causa independente sem investigação |
| TLS real em laboratório e V1 | PENDENTE; nenhum fallback foi implementado nesta fase |
| RTP 2, Exo/mpv e hardware | NÃO EXECUTADO; `adb devices` não encontrou dispositivos |

Comando do baseline, sem gerar APK:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testStandardDebugUnitTest :OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest :app:compileStandardReleaseKotlin :app:lintVitalStandardRelease -PowntvUseCoreSnapshot=true --dependency-verification=strict --console=plain
```

O build terminou com `BUILD FAILED in 10m 28s`. Log original: `%TEMP%\owntv-tls-phase0-baseline.log`. Relatório Gradle: `build/reports/dependency-verification/at-1793463416559/dependency-verification-report.html`. Não se acrescentaram hashes de confiança nem se desativou a verificação para contornar este baseline.

Consulta independente do classpath que passou (log `%TEMP%\owntv-tls-phase0-classpath.log`):

```powershell
.\gradlew.bat :OwnTV_Core:player-core:dependencyInsight --dependency com.squareup.okhttp3:okhttp --configuration releaseCompileClasspath -PowntvUseCoreSnapshot=true --offline --no-configuration-cache --dependency-verification=strict --console=plain
```

Evidência adicional: [manifesto da auditoria](tls/phase0_evidence_2026-10-31.json). Os caminhos temporários nele registados são evidência desta execução, não dependências do projeto.

## 7. Continuação e âmbito

| Entrega | Estado |
|---|---|
| A — Fase 0 | Auditoria entregue; bloqueio prévio de build e ensaios físicos documentados |
| B — V1 | Pendente: hostname errado com cadeia válida, contexto por lista, preferência, revalidação, testes e integração |
| C — V2 | Pendente, entrega separada: cadeia inválida/expiração/CA desconhecida e AUTO_FULL, após gates próprios |

Esta entrega acrescenta documentação/evidência apenas. Não altera fontes de produção, defaults, dados, versões, Core ZIP/lock, áudio, buffers, zapping ou TV Avó. Não houve APK, commit, push nem publicação. Para retirar apenas esta entrega, remover este relatório e o respetivo manifesto; preservar todas as personalizações e os inputs do Core.

## Referências consultadas

- [OkHttp 5.5.0 — Interceptor.Chain](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/Interceptor.kt).
- [OkHttp 5.5.0 — RealInterceptorChain](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/http/RealInterceptorChain.kt).
- [Media3 — transporte por DataSource.Factory](https://developer.android.com/media/media3/exoplayer/network-stacks).
- Código e AARs locais identificados acima; especificação original com SHA-256 no manifesto.
