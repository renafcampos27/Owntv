# AUTO_TLS_FALLBACK — entrega C / V2

Data: 31-10-2026. Continuação da [V1](AUTO_TLS_FALLBACK_V1_2026-10-31.md).

**Implementação V2 concluída; testes JVM, compilação Release, lint e compilação dos testes Android aprovados. Aceitação em dispositivo pendente.** A validação TLS controlada foi feita em loopback, sem contactar o fornecedor IPTV nem alterar certificados do sistema.

## Configuração na app

Definições → Reprodutor de vídeo → **Compatibilidade TLS automática**.

| Opção | Comportamento |
|---|---|
| Validação estrita | Predefinição. Não autoriza exceções. |
| Compatibilidade automática de hostname | V1: cadeia válida, exceção apenas para hostname incorreto. |
| **Compatibilidade completa (avançado)** | V2: também permite o último recurso para certificados expirados, autoassinados ou com CA/cadeia sem confiança. |
| Configurar por lista | Seguir definição geral ou escolher um dos três modos por playlist. |

As opções automáticas têm avisos próprios. No modo completo, a ligação continua cifrada, mas a identidade do servidor pode não ser autenticada e as credenciais IPTV podem ser intercetadas. A escolha é feita uma vez nas Definições; não há confirmações por canal. A preferência é persistente e entra no backup; o modo permissivo efetivo continua temporário.

O âmbito mantém-se em **ExoPlayer Live**: fullscreen, preview Live e sintonia dos tiles. A V2 não alarga o âmbito da V1 a login, Xtream/Stalker API, EPG, atualizações, DRM, downloads, gravações, mpv, Hero preview ou ExoSubtitleEngine.

## Implementação

- `PlaybackTlsPolicy` acrescenta `AUTO_FULL`. STRICT permanece a predefinição e valores desconhecidos continuam a falhar para STRICT.
- `PlaybackTlsTrust` usa os callbacks de socket/engine de `X509ExtendedTrustManager`. O bypass de cadeia só existe no contexto TLS dedicado e quando o peer observado corresponde ao **host e porta exatos** autorizados. Sem esse contexto, a validação é estrita.
- O trust manager original, incluindo `ExtraTrustAnchors`, é delegado nos percursos estritos e em todos os outros destinos. Não foi instalado trust manager, verificador ou fábrica SSL permissivos no cliente geral ou no Android.
- O primeiro passo é sempre STRICT. Hostname errado com cadeia válida usa HOSTNAME_ONLY. Uma falha de cadeia comprovada pode ir diretamente para UNVERIFIED, porque dispensar hostname não resolveria essa cadeia.
- A classificação exige exceções tipadas de certificado/caminho e evidência da tentativa TLS corrente. Não usa texto da mensagem. Uma falha antiga de outra tentativa não autoriza a atual.
- O classificador recusa motivos explícitos de revogação, restrições de algoritmo, assinatura inválida e validade ainda não iniciada, incluindo motivos aninhados num erro de construção de cadeia. Falhas genéricas, DNS, timeout, HTTP e pinning não autorizam este último recurso.
- STRICT, HOSTNAME_ONLY e UNVERIFIED têm pools separados. Certificados, sessões SSL e aprovações individuais não são guardados como autorização permanente.
- A cache continua limitada a 128 endpoints, em memória, por lista+esquema+host+porta. A preferência continua no DataStore e não depende do TTL.
- Após 15 minutos, o próximo pedido natural faz uma verificação estrita com pool e contexto TLS novos. Single-flight permite que os restantes segmentos continuem. Certificado corrigido → STRICT; cadeia corrigida mas hostname ainda errado → HOSTNAME_ONLY; cadeia ainda inválida → último recurso mínimo autorizado.
- O mecanismo não cria pedidos IPTV de sondagem em background. Uma ligação progressiva sem novos pedidos não permite descobrir instantaneamente a correção do servidor.
- Os Calls permanecem nativos; o controlo de cancelamento e fecho de `PlaybackHttpCalls` não foi alterado. Não há uma implementação própria de `okhttp3.Call`.
- A aprendizagem só ocorre após resposta HTTP de sucesso e descreve o transporte TLS, não a confirmação de um primeiro fotograma. A sintonia, descodificação e formatos continuam a ter as validações anteriores.

Quando UNVERIFIED está ativo, a cadeia não é autenticada nesse endpoint até nova verificação. A seleção inicial é limitada às falhas identificadas; isso não transforma o intervalo de bypass numa validação de certificados revogados ou de outras propriedades da cadeia. O aviso da opção refere esta perda de identidade.

## Pinning, redirects e autorização

O `CertificatePinner` é preservado nas alterações da fábrica SSL. Foi testado um certificado expirado com pin correto e incorreto: o primeiro abre no modo completo e o segundo continua recusado. Uma cadeia que o mecanismo de pinning não consiga limpar pode continuar a ser recusada, mesmo com AUTO_FULL; não se remove pinning para obter compatibilidade.

A recuperação desta entrega exige um pedido inicial HTTPS. Um URL inicial HTTP que só passe para HTTPS num redirect não recebe esta recuperação; a validação desse salto mantém-se no percurso anterior.

Um redirect para outro host ou outra porta não recebe o trust bypass. O manager limitado ao endpoint delega esse destino na validação original. Um redirect saudável pode prosseguir com validação normal e sem os headers privados da origem. Nas sessões AUTO é recusado downgrade HTTPS→HTTP. URLs HTTP explícitos existentes mantêm o comportamento anterior.

Mudanças de política retiram os pools e as autorizações incompatíveis. A criação de entradas na cache partilha a sincronização da mudança de configuração, evitando que um scope antigo volte a inserir a política anterior. Listas desativadas/removidas não recebem autorização de fallback.

## Validação real executada

| Verificação | Resultado |
|---|---|
| Testes JVM App | 470, zero falhas/erros/skips |
| Testes JVM Core | 1.402, zero falhas/erros/skips |
| Testes JVM Player | 614, zero falhas/erros/skips |
| Total | **2.486 testes JVM**, mais 12 do que a V1 |
| Bateria dirigida TLS + preferências | **27 testes**, incluindo as regressões da V1 |
| Ferramentas snapshot/release | 23 testes Python aprovados |
| Compilação Standard Release | Aprovada |
| Lint vital Standard Release | Aprovado |
| Testes Android com DataSource Media3 real | Dois compilados; execução em dispositivo pendente |
| Cópia limpa usando apenas o ZIP do Core | Aprovada: 6m18s, offline, verificação estrita, configuration cache guardada |
| Box, reprodução/descodificação real e testes de duração | Não executados |

Os ensaios usam servidores HTTPS de loopback e certificados de teste: correto, hostname errado, expirado e autoassinado sem confiança. Demonstraram:

- Recuperação de cadeia/CA desconhecida, autoassinado e expirado; uso apenas de HOSTNAME_ONLY quando isso chega.
- Ciclo no mesmo endpoint: UNVERIFIED → ainda inválido → HOSTNAME_ONLY → STRICT → nova recuperação.
- Pinning ativo, isolamento por fonte, retirada de AUTO_FULL ao mudar a política e recusa de bypass em outro host/porta.
- Headers necessários preservados, headers privados retirados em redirects, HTTP 401/403/404, timeout, DNS e erro SSL genérico sem falso fallback.
- Cancelamento no segundo handshake mantendo um único Call nativo; concorrência durante revalidação sem bloquear os outros segmentos.
- Oito pedidos saudáveis com AUTO_FULL continuam em STRICT e reutilizam uma única ligação TLS.
- Persistência de AUTO_FULL num DataStore real reaberto; backup com política geral e overrides independentes/remapeados.
- Callbacks sem informação de endpoint permanecem estritos; mensagens de erro, por si só, não são evidência.

O teste Android acrescentado lê manifesto HLS e bytes de segmento através do `OkHttpDataSource` real com um certificado autoassinado. Compilar esse teste não prova que passou em Conscrypt/firmware de uma box nem testa descodificação de vídeo. O provider TLS tem de disponibilizar informação do socket/engine; se essa prova faltar, o mecanismo recusa o bypass.

## Reprodução dos checks e recursos do PC

```powershell
.\gradlew.bat :app:testStandardDebugUnitTest :OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest :app:compileStandardReleaseKotlin :app:lintVitalStandardRelease :OwnTV_Core:player-core:compileDebugAndroidTestKotlin --no-configuration-cache --max-workers=2 --no-parallel "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8" "-Pkotlin.daemon.jvmargs=-Xmx3072m" --dependency-verification=strict --console=plain
```

Resultado: `BUILD SUCCESSFUL in 5m 38s`, 160 tarefas. A exportação limpa, sem outputs/cache do projeto nem checkout Core externo, passou compilação Release e lint em 6m18s, offline e com a cache global normal do Gradle; não foi uma instalação de dependências de raiz. Um processo PowerShell falhou antes por falta de memória do PC; terminaram-se apenas os processos de build inativos desta tarefa e limitaram-se heaps/workers nos comandos. Não se alteraram os limites de memória do projeto nem a política de memória da app para acomodar o PC.

## Snapshot e continuação

- Core: **1.089 inputs**, SHA-256 `8375ddea1894e8e55ac5b3217260ddc1fdcf378044f31c5f2bf4c77766f3402e`.
- ZIP/lock regenerados pela ferramenta do projeto e comparados com os bytes do checkout.
- [Manifesto de evidência V2](tls/v2_evidence_2026-10-31.json).
- Backup verificável anterior: `C:\Users\renat\Downloads\OwnTV-tls-v2-backup-20261031-173129`, incluindo o snapshot V1 completo e os ficheiros App relevantes.
- Wiring geral de rede, raízes originais, backup, propriedade dos Calls, OwnTVPlayer/mpv e ExoSubtitleEngine foram comparados byte a byte com a V1 e não foram alterados nesta entrega.
- Não foram gerados/instalados APKs, alteradas versões/dependências, feitos commits ou publicações.

As entregas A, B e C têm agora implementação/documentação. Permanecem pendentes a execução dos testes Android, a aceitação em box e os ensaios com canais reais autorizados. Não há percentagens de desempenho ou garantias de compatibilidade física sem medição.

## Referências técnicas

A separação por destino usa os callbacks com contexto de socket/engine de [X509ExtendedTrustManager, Android](https://developer.android.com/reference/javax/net/ssl/X509ExtendedTrustManager), disponíveis abaixo do minSdk 26 já existente. A alteração da fábrica ocorre só na cadeia do application interceptor e preserva o pinner, conforme a implementação [RealInterceptorChain, OkHttp 5.5.0](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/http/RealInterceptorChain.kt); o comportamento foi ensaiado com [CertificatePinner](https://github.com/lysine-dev/okhttp/blob/parent-5.5.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/CertificatePinner.kt) no binário usado pelo projeto.
