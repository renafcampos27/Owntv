# AUTO_TLS_FALLBACK — entrega B / V1

Data: 31-10-2026.

Continuação: a [entrega V2](AUTO_TLS_FALLBACK_V2_2026-10-31.md) acrescenta AUTO_FULL. Este relatório conserva os resultados e limites históricos da V1.

**Implementação V1 concluída.** Testes JVM, compilação Release, lint vital e compilação do teste Android passaram. A execução do teste Android e a aceitação em box continuam pendentes. A V2 não foi implementada.

## Utilização

Definições → Reprodutor de vídeo → secção do motor → **Compatibilidade TLS automática**.

- **Validação estrita:** predefinição; não autoriza recuperação de hostname.
- **Compatibilidade automática de hostname:** começa por validar normalmente e só tenta a exceção depois de um mismatch comprovado.
- **Configurar por lista:** permite escolher Seguir definição geral / Validação estrita / Compatibilidade automática de hostname para cada lista com TV ativa.

A descrição avisa do risco de interceção das credenciais de streaming. É uma escolha nas Definições, sem pedidos de aprovação por canal. Ativar aplica-se à próxima sintonia; desativar retira a autorização dos novos pedidos. Um override explícito por lista prevalece sobre a definição geral.

O âmbito funcional desta entrega é **ExoPlayer Live**, incluindo o preview Live, fullscreen e o caminho de sintonia dos tiles Multiview. mpv, Hero preview, reprodução de arquivos/diferidos em ExoSubtitleEngine, gravações, downloads, captura de timeshift e serviços de API mantêm os caminhos anteriores. A proteção de licenças DRM permanece separada.

## Comportamento implementado

1. O ID persistente da lista percorre ChannelEntity → LiveTuneController → LiveRequest → TuningRequest → fábrica HTTP. A origem tem de coincidir com a do canal, ser positiva e ter TV ativa. `TuneToken.sourceId` continua a ser apenas geração técnica.
2. O contexto é imutável por fábrica/sintonia e segue em tags locais do pedido. Não são enviados headers de controlo TLS, IDs ou chaves de sessão ao fornecedor.
3. Um application interceptor no cliente exclusivo de reprodução utiliza as APIs OkHttp 5.5.0. São preservados os Calls nativos e o controlo existente de cancelamento/fecho `PlaybackHttpCalls`.
4. Apenas GET/HEAD HTTPS autorizados podem recuperar. Um verificador estrito estável regista host e porta do mismatch na tentativa corrente. A comparação exata precede uma única repetição; o trust manager e o pinning continuam ativos.
5. Pools separados impedem que um socket de hostname dispensado seja reutilizado como ligação estrita. A identidade estável do verificador permite reutilizar sockets entre segmentos saudáveis.
6. Aprendizagem só em memória, limitada a 128 endpoints, por lista+esquema+host+porta. O modo de hostname é memorizado após uma resposta HTTP de sucesso; isto é recuperação do transporte, não uma declaração de que o conteúdo já produziu um fotograma.
7. Após 15 minutos, o próximo pedido natural faz a revalidação estrita com pool e contexto TLS novos. Não há pedido IPTV extra em segundo plano. Single-flight permite aos outros segmentos prosseguir durante essa revalidação.
8. Quando a verificação estrita tem sucesso, os novos pedidos usam o pool estrito e retiram-se da circulação os sockets permissivos inativos. Épocas de estado impedem resultados/snapshots antigos de reativar a exceção.
9. Se o certificado continuar com hostname incorreto, uma recuperação bem-sucedida renova o prazo. DNS, timeout, erro HTTP ou TLS genérico não ampliam a confiança nem apagam indiscriminadamente o estado anterior.
10. A preferência persiste no DataStore; a aprendizagem transitória desaparece ao reiniciar. O primeiro pedido volta a tentar STRICT, mantendo a capacidade automática de recuperação.
11. Desativar a política efetiva, remover ou desativar a lista invalida a autorização e os pools relevantes para novos pedidos. O DAO de fontes fornece a lista de origens efetivamente ativas.
12. Backup inclui política geral e overrides por lista. Exporta apenas fontes selecionadas, remapeia IDs e retira overrides antigos dos IDs substituídos, evitando herança por colisão numérica. Valores desconhecidos falham para STRICT; não existe AUTO_FULL na V1.

Eventos sanitizados: `TLS_STRICT_OK`, `TLS_HOSTNAME_RECOVERED`, `TLS_RECHECK_DUE`, `TLS_RECHECK_STRICT_OK`, `TLS_RECHECK_STILL_INVALID`. Contêm ID da fonte, hostname, modo e razão técnica; não contêm URL, senha, tokens, cookies ou texto livre da exceção. São emitidos por transição/descoberta, não em cada segmento. O diagnóstico Live recebe os eventos da própria sessão.

## Limites explícitos

- Não recupera CA desconhecida, autoassinatura sem confiança, expiração, pinning, protocolo/cipher TLS, DNS, timeout ou HTTP 401/403/404. Cadeias inválidas pertencem à V2.
- Mismatch descoberto noutro host/porta de um redirect falha em segurança. Um hostname diferente referenciado diretamente por um manifesto recebe a sua própria avaliação e chave de endpoint; não herda aprendizagem de outro host.
- Falhas compostas de múltiplas rotas/IPs, que não chegam como `SSLPeerUnverifiedException` da tentativa instrumentada, podem ser recusadas conservadoramente. Não há classificação por mensagens de erro.
- Em sessões AUTO são recusados redirects HTTPS→HTTP. Em redirects entre origens são removidos cabeçalhos privados; UA, Range e os headers públicos necessários são preservados. Listas HTTP explícitas existentes não são convertidas para HTTPS nem bloqueadas por este mecanismo.
- Não há garantia de detetar uma correção num fluxo que continue indefinidamente no mesmo socket sem novos pedidos. O prazo é verificado no próximo pedido necessário.
- Não se atribuiu um modo TLS único ao overlay de um fluxo multi-host. Os estados disponíveis nesta V1 são por endpoint nos registos. O modo efetivo do mpv continua por medir e não foi alterado.

## Ficheiros principais

| Área | Ficheiros |
|---|---|
| Política e transporte | Core `network/PlaybackTlsPolicy.kt`, `PlaybackTlsInterceptor.kt`, `StreamingHttpClient.kt` e `di/DataModule.kt` |
| Preferências e backup | Core `settings/SettingsRepository.kt`, `backup/BackupManager.kt` |
| Contexto e promoção de preview | Player `LiveEngines.kt`, `LiveTuneController.kt`, `LivePreviewEngine.kt` |
| Interface | App `features/settings/SettingsViewModel.kt`, `VideoPlayerSettingsScreen.kt` |
| Tradução e inventário | Recursos novos EN/PT/PT-PT e classificação dos identificadores técnicos nos manifests i18n |
| Testes JVM | `PlaybackTlsInterceptorTest`, `PlaybackTlsPreferencesTest`, regressão em `LiveTuneControllerTest` |
| Teste Android | `player-core/src/androidTest/.../PlaybackTlsDataSourceTest.kt` + fixtures de teste |

Não se alteraram OwnTVPlayer/mpv, ExtraTrustAnchors, ExoSubtitleEngine ou HeroPreviewEngine: foram comparados byte a byte com o snapshot inicial. As fixtures de certificados estão apenas nas árvores de teste; a CA não é instalada no sistema e a chave privada da CA não integra o projeto.

## Validação

Baseline anterior à funcionalidade: `BUILD SUCCESSFUL in 2m 49s`, após corrigir exclusivamente o hash em falta de `groovy-bom-4.0.29.pom`. Os bytes locais foram comparados com o POM oficial do Maven Central; SHA-256 `c24277dec93f146bcda25f5ae4391d6527e384e2132efa32184c1e852b42bca9`. Mantida verificação estrita. Total corroborado: 1.623 artefactos App e 1.032 Core.

Validação final da implementação: **BUILD SUCCESSFUL in 5m 30s**, 160 tarefas.

| Verificação | Resultado |
|---|---|
| JVM App | 470 testes; zero falhas/erros/skips |
| JVM Core | 1.390 testes; zero falhas/erros/skips |
| JVM Player | 614 testes; zero falhas/erros/skips |
| Total JVM | **2.474 testes**, incluindo **16 testes novos** desta entrega |
| Ferramentas de snapshot/release | 23 testes Python passaram |
| Compilação Standard Release | Passou |
| Lint vital Standard Release | Passou |
| Teste Android com DataSource Media3 real | Compilou; execução em dispositivo pendente |
| Cópia limpa com Core exclusivamente do ZIP | Passou: 8m26s, offline, verificação estrita; configuration cache guardada |
| Hardware / canal do fornecedor | Não executado; não foi provocada falha no fornecedor |

Os testes TLS usam servidores HTTPS de loopback com uma CA própria de teste: hostname errado, correto e certificado expirado. Demonstram o ciclo inválido → recuperação → certificado corrigido → STRICT → nova recuperação, TTL com relógio injetado, single-flight, reutilização de socket, pinning/CA/timeout/DNS/HTTP, cancelamento no segundo handshake, headers, isolamento e invalidação. O teste Android cobre abertura de manifesto e bytes de segmento com `OkHttpDataSource` real; não é um teste de descodificação de vídeo.

Comando da bateria final, sem gerar APK:

```powershell
.\gradlew.bat :app:testStandardDebugUnitTest :OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest :app:compileStandardReleaseKotlin :app:lintVitalStandardRelease :OwnTV_Core:player-core:compileDebugAndroidTestKotlin --no-configuration-cache --dependency-verification=strict --console=plain
```

A cópia limpa foi exportada sem outputs/cache do projeto nem checkout Core externo, com apenas o ZIP referido no lock e o caminho local do SDK. Usou a cache global normal do Gradle; não é uma instalação de dependências de raiz. O comando de compilação Release + lint manteve a configuration cache predefinida e terminou com `Configuration cache entry stored.`

O ensaio de persistência em disco faz uma escrita confirmada e reabre o DataStore. O restante ciclo de alterações/backup é testado com DataStore em memória: a implementação Android de FileStorage usa `renameTo`, cuja substituição de ficheiro existente difere no Windows de teste. Não se alterou a persistência de produção para acomodar o ambiente de testes.

## Snapshot, evidência e continuação

- Core: 1.086 inputs; SHA-256 `357e9e5b7b829b020646aa21a0da859eb15bad09479639bf92e8706aefed8500`.
- ZIP e `core-source-lock.json` regenerados pela ferramenta oficial e comparados com os bytes do checkout.
- [Manifesto final desta entrega](tls/v1_evidence_2026-10-31.json).
- Backup prévio: `C:\Users\renat\Downloads\OwnTV-tls-v1-backup-20261031-163356`, com manifesto; o ZIP inicial `e96eca...` foi preservado. Qualquer reversão deve comparar os inputs alterados com essa base e preservar edições posteriores.
- [Auditoria da Fase 0](AUTO_TLS_FALLBACK_FASE_0_2026-10-31.md) permanece como histórico do estado inicial.
- Próxima entrega: **V2**, independente, para cadeias inválidas/expiradas e opção AUTO_FULL. Nenhum trust manager permissivo foi criado nesta V1.

Não foi gerado/instalado APK, alterada a versão da app, feito commit, push ou publicação. Não há percentagens de ganho de desempenho ou garantia de hardware sem medição.
