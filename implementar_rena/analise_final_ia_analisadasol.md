# OwnTV TV — análise final das propostas das outras IA

**Estado atual — 24/10/2026:** A1, A2, A3, A4 e lotes B/C/D/E recomendados estão implementados e validados automaticamente. Ver **secção 26** e [relatório final de execução](../docs/IMPLEMENTACAO_SUBFASES_RESTANTES_2026-10-24.md). OPT-03 tem matriz das opções críticas e testes de alcance; os ensaios de hardware/comando permanecem pendentes. MED/NOV e upscaling continuam condicionais; TV-AVO-01 foi implementado posteriormente por autorização específica; ver secções 27–28.

**Leitura do histórico:** as indicações abaixo de «fase 6 por completar» ou «restantes não iniciadas» são fotografias anteriores, superadas pelo fecho da secção 26.

**Data da revisão original:** 20/10/2026. **Reanálise crítica atual: 23/10/2026.** As fases 1–5 têm implementação e validação automática histórica nos âmbitos registados, mas a nova revisão identificou seis IDs com complementos ou lacunas residuais. Não reaplicar o plano em bloco. A fase 6 continua por completar; aceitação física, medições e novidades opcionais mantêm-se condicionais. **A secção 24 e a [reanálise atual do council](REANALISE_COUNCIL_PLANO_2026-10-23.md) prevalecem para decidir o trabalho seguinte.** As secções anteriores conservam o histórico, não constituem um novo inventário de tarefas pendentes.

**Execução posterior em 23–24/10/2026:** primeira subfase **A1 — R1 / DAT-03**, URLs de diferidos, **concluída e validada automaticamente**; ver secção 25. As restantes subfases não foram iniciadas. Contagem após A1: 29 IDs implementados no âmbito inspecionado, cinco com resíduos, seis blocos da fase 6 a completar e OPT-03 transversal; aceitação física pendente.

## 1. Decisão do council

**Não aplicar os sete relatórios em bloco.** Contêm correções úteis, duplicados, funcionalidades retiradas deliberadamente, hipóteses apresentadas como certezas e soluções que os próprios autores corrigem em secções posteriores.

Este documento reúne o que vale a pena implementar, com adaptações e testes obrigatórios. Um item «aprovado» significa que a alteração delimitada é recomendada; não significa que o defeito tenha sido reproduzido numa box ou que o pseudocódigo original esteja aprovado.

Participaram três revisões especializadas independentes: reprodução/HLS/áudio e ciclo de vida; dados/Room/EPG/backup; segurança/build/limpeza. O coordenador reviu definições, foco, configuração de fontes e interface, confrontou os pareceres e submeteu as alterações sensíveis a contraditório. Isto é revisão técnica multiagente, não uma certificação externa.

### Regras que todas as fases devem respeitar

- App focada em **Android TV/boxes**. Não reintroduzir UI de smartphone, filmes/séries, OpenSubtitles, meteorologia, animações, brilho ambiente, favoritos/histórico ou menus ocultados por decisão do utilizador.
- Manter **volume interno a 100%**. Não introduzir boost, normalização, ducking interno ou processamento extra de áudio como parte desta revisão.
- Manter HLS estrito e exceções explícitas por canal, prioridades/alternativas configuradas, buffer/latência, arranque fixo e configuração existente de cada canal.
- Manter o intervalo configurável de reabertura e as confirmações de libertação HTTP/nativa. Um timeout não prova que o fornecedor fechou a sessão nem que o decoder foi libertado.
- Manter setas para mudar de canal em fullscreen, modo simples, foco no canal ao regressar, gravações, recuo, diferidos e pesquisa limpa quando a app realmente sai de primeiro plano.
- Manter a correção de diferidos de **+1 h separada da hora apresentada**. Não voltar a somar correções em várias camadas.
- Preservar dados legados e os contratos de backup. Remover uma superfície da App não autoriza apagar tabelas, chaves ou migradores do Core.
- Não substituir o Core personalizado pelo upstream, não editar o Core Mobile e não tratar a cache `.gradle/core-snapshot` como origem de desenvolvimento.

## 2. Base real e documentos avaliados

**App:** `C:/Users/renat/Downloads/OwnTV-main`, HEAD `bdffec4`. **Core externo:** `C:/Users/renat/Downloads/OwnTV_Core`, HEAD `e7a044e`, com alterações locais. O build da App usa o **snapshot personalizado fixado**, não automaticamente qualquer alteração nesse Core externo.

**Fotografia da revisão original de 20/10/2026:** `core-source-lock.json` → SHA-256 `e2081e78acce95e5ac9b881164c98f066648e6a9400e554c9c9612f527e14890`, 1 007 ficheiros; hash do ZIP conferido nessa revisão. **Não é o snapshot atual:** ver secções 23–24 e a reanálise de 23/10. Esquema personalizado: **51**. Referências `Core:` das propostas originais são ao código dessa fotografia: os pacotes `backup/`, `parser/`, etc. ficam em `core/src/main/java/tv/own/owntv/core/`; `player-core/` abrevia `player-core/src/main/java/tv/own/owntv/player/`. Referências `App:` são relativas a `app/src/main/java/tv/own/owntv/`, salvo indicação em contrário. As linhas são referências históricas, não identificadores permanentes.

| Referência | Documento de origem | Linhas |
|---|---|---:|
| F1 | [ANALISE_APP_OWNTV.md](ANALISE_APP_OWNTV.md) | 4 036 |
| F2 | [ANALISE_OWNTV_2026-10-19.md](ANALISE_OWNTV_2026-10-19.md) | 1 370 |
| F3 | [ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md](ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md) | 5 925 |
| F4 | [ANALISE_OWNTV_COUNCIL_RENA_2026-10-19.md](ANALISE_OWNTV_COUNCIL_RENA_2026-10-19.md) | 413 |
| F5 | [ANALISE_OWNTV_RONDA_4_2026-10-19.md](ANALISE_OWNTV_RONDA_4_2026-10-19.md) | 249 |
| F6 | [ANALISE_OWNTV_RONDA_6_COUNCIL_2026-10-19.md](ANALISE_OWNTV_RONDA_6_COUNCIL_2026-10-19.md) | 681 |
| F7 | [DECISAO_COUNCIL_OWNTV_2026-10-19.md](DECISAO_COUNCIL_OWNTV_2026-10-19.md) | 439 |

**Total documental:** 13 113 linhas, com muita repetição. Os IDs originais não são globalmente únicos: por exemplo, `BUG-P1-01` significa coisas diferentes em F1 e F4. Por isso, este plano usa IDs próprios e indica documento + achado de origem. Datas futuras e declarações de «confirmado», «medido» ou «100% validado» nos documentos não foram tomadas como prova independente.

**Método:** triagem dos inventários e revisões dos sete ficheiros, deduplicação e verificação dirigida dos percursos relevantes no código atual. Não foi uma leitura linha a linha de todo o repositório. Não se executaram builds, suites de testes, benchmarks ou ensaios físicos nesta análise; os testes descritos abaixo são critérios para a implementação futura. Os 2 248 testes da integração anterior não foram repetidos e não provam estes novos achados.

## 3. Implementações aprovadas — configuração, comando e interface

### UI-01 — DNS coerente e não destrutivo — prioridade alta de usabilidade

- **Origem:** F6 S5–S8/S17 e R7-MELH-02; F3 SET-04. **Evidência:** `App:features/settings/DnsSettingsScreen.kt:75–99,112–119,174–182,222–225`.
- **Problema verificado:** desligar apaga host/porta/DoH; guardar e testar usam interpretações diferentes; uma porta inválida pode tornar-se 53 sem aviso; abrir a página pode focar um preset que grava imediatamente.
- **Implementar:** parser único para guardar/testar/apresentar, IPv4/hostname/IPv6 com porta explícita e DoH HTTPS válido; portas 1–65535; erros junto ao campo. Guardar `enabled=false` sem apagar servidor. Foco inicial seguro; escolher preset prepara a alteração para Guardar, ou exige confirmação se substituir configuração existente. Tornar o estado «por guardar» visível.
- **Aceitação:** desligar/religar conserva configuração; teste usa a porta escolhida; IPv6 com/sem colchetes é tratado conforme a sintaxe suportada; OK ao abrir não altera DNS. Nenhuma escrita inválida chega ao Core.

### UI-02 — Proxy e campos de rede com validação comum

- **Origem:** F1 BUG-OIT-05/06; F3 SET-04; F6 C-A8. **Evidência:** `App:features/settings/NetworkSettingsScreen.kt:84–90` transforma entrada inválida em porta 0 e pode guardar ao ligar; `SettingsViewModel.kt:1451,1544` são os pontos de escrita.
- **Implementar:** validar host/porta quando ativo e preservar configuração ao desativar; reutilizar a validação no teste. Na adição de fontes/EPG validar o tipo de entrada permitido, sem proibir M3U local, `content://`, portas privadas ou HTTP IPTV legítimo. Alertar duplicados sem fundir fontes automaticamente.
- **Aceitação:** inválidos não substituem valores válidos; URLs/credenciais existentes continuam utilizáveis; modo desligado não obriga a preencher os campos.

### UI-03 — Recuperação de foco nos diálogos e nos resultados das definições

- **Origem:** F3 SET-02/03 e UX-08/PLY-04; F6 V-03/A.1.9.
- **Evidência:** `SettingsScreen.kt:1084–1092` dá precedência ao catálogo genérico sobre ações específicas; `NumberInputDialog.kt:89–90,120` pede foco só na abertura, embora o botão possa ficar desativado ao atingir um limite. `FocusTrap.kt:189` já contém `rememberStepperFocus`; `BoundedFocusRequest.kt:42–64` já fornece tentativas limitadas.
- **Implementar:** conservar a ação/âncora específica de cada resultado; usar os helpers existentes para foco após composição e ao atingir mínimo/máximo. Terminar restauro com sucesso ou fallback explícito, sem deixar flags pendentes nem insistir depois de nova ação no comando.
- **Aceitação:** abrir por pesquisa → alterar → fechar devolve foco ao resultado/campo correto; atingir extremos não deixa o comando sem foco; fechar rapidamente não provoca um refoco tardio.

### UI-04 — Identidade de foco nas listas com cabeçalhos

- **Origem:** F5 R4-B02/B03/B19; F6 A.1.1/A.1.9.
- **Evidência:** `features/recordings/RecordingsScreen.kt:211–216,304–319` e `features/downloads/DownloadsScreen.kt:154–172,297` misturam índice de linha com índice dos itens sem cabeçalhos. Downloads também não conclui restauro quando a lista está vazia.
- **Implementar:** guardar ID e contexto do item; calcular separadamente posição visual e vizinho após remoção. Tratar lista vazia e incluir identidade/geração nas chaves de restauro. Nos percursos VOD que não são alcançáveis, não reabrir funcionalidades para justificar esta correção; aplicar ao componente/rota efetivamente utilizado ou ao contrato partilhado.
- **Aceitação:** dois grupos com cabeçalhos, apagar primeiro/meio/último e mudar estado de gravação mantêm foco no vizinho previsto. Refresh posterior não rouba foco por um pedido antigo.

### UI-05 — Teste de listas e edição de credenciais por identidade

- **Origem:** F6 C-A5/A6. **Evidência:** `ManageSourcesScreen.kt:376` procura pelo nome; `SettingsViewModel.kt:1782–1801` só transporta `sourceName`; `:1142–1149` não agenda nova sincronização só por mudar URL/utilizador/senha.
- **Implementar:** transportar `sourceId` e revisão da configuração em todos os estados do teste. Ao editar dados que invalidam URLs de catálogo ou autenticação, cancelar o trabalho antigo apropriado e agendar uma sincronização não destrutiva da mesma fonte; invalidar caches da revisão anterior. Manter os IDs e as personalizações.
- **Aceitação:** duas listas com o mesmo nome não se confundem; corrigir senha provoca atualização sem apagar canais/configurações; um resultado de teste antigo não altera a lista entretanto editada.

### UI-06 — Importação recuperável e tentativa com dono explícito

- **Origem:** F6 C-A1/A2/A4; F7 adenda 4, onboarding após morte do processo.
- **Evidência:** `SettingsViewModel.kt:1187` devolve sempre `null` para a última fonte falhada; `SetupViewModel.kt:133–145,185–202` mantém `backgroundHandoff` como estado da instância; `SetupWizard.kt:101–115,836–841` pode restaurar etapa IMPORTING com importer Idle e mostrar spinner.
- **Implementar:** estado por operação, rascunho limitado em memória enquanto a tentativa existir, Retry preenchido e `backgroundHandoff` reposto por tentativa. Se o trabalho já não existe após recriação, apresentar «importação interrompida» e reconciliar o que foi persistido. Perfis provisórios precisam de dono e reconciliação, não de um delete indiscriminado ao fechar o wizard.
- **Aceitação:** falha→Retry e duas importações seguidas não herdam flags; morte do processo não produz spinner infinito nem perfis duplicados. Não guardar passwords/PINs em `rememberSaveable`, logs ou ficheiros de estado sem proteção.

### UI-07 — Continuidade do ecrã inicial

- **Origem:** F6 D-A3–A8/R7-MELH-13/14.
- **Evidência:** `HomeScreen.kt:245–274,1184–1191` pode reposicionar ao atualizar e força cartão 0 na reentrada; `HomeViewModel.kt:548–560` reinicia o índice e compara o preview só com o primeiro item; `HomeGuideSlice.kt:177–205,294–301` retém índices e usa `guide.now` capturado.
- **Implementar:** preservar item por chave, rolar apenas num pedido real de restauro; revalidar índices quando a lista encolhe; não parar um preview cujo item ainda existe. Relógio limitado à secção «No momento», enquanto visível; saída D-pad previsível nos extremos.
- **Aceitação:** refresh em fundo não salta para o topo nem corta o preview sem necessidade; mudança de programa atualiza o cartão; a lista reduzida não fica visualmente vazia.

### UI-08 — Deep link consumido uma única vez

- **Origem:** F2 M1; F6 A.1.4; F1 BUG-QUIN-09.
- **Evidência:** `MainActivity.kt:140–144,202,479–480` volta a interpretar `intent.data`, mas consumir limpa apenas o estado transitório.
- **Implementar:** marcar/limpar o lançamento consumido no Intent e distinguir criação, recriação e novo Intent. Preservar uma ação ainda não entregue e validar perfil/visibilidade antes de reproduzir.
- **Aceitação:** recriar Activity não reabre o canal; um novo pedido deliberado para o mesmo canal continua a funcionar; não remover a entrada do launcher nem depender de um caller package inexistente.

### UI-09 — Mensagens de falha e ações de gravação seguras

- **Origem:** F5 R4-B09/B10; F6 HUD-REC/DL-MSG; F3 DWN-01/CAN-01.
- **Evidência:** `RecordingsViewModel.kt:261–288,322–325` tem ações sem tratamento local; `LiveViewModel.kt:436` pode recusar gravação sem motivo na UI. O Core já tem orçamentos de armazenamento/sessões — a falha está também na propagação do motivo.
- **Implementar:** resultado tipado e mensagem acionável para falta de espaço, limite de ligações, armazenamento removido, programa indisponível e erro de escrita. Capturar falhas operacionais junto à ação, preservando `CancellationException`; não transformar falha em sucesso nem apagar o motivo original.
- **Aceitação:** ação recusada responde visivelmente; erro não fecha a App; cancelar/trocar perfil não mostra erro de uma ação antiga. Persistir motivo de falha só com migração aditiva e dados legados válidos.

### UI-10 — Acessibilidade e estados honestos

- **Origem:** F6 R6-UX-01/02/04, S15 e SUB-UNK; F3 CAT-6.
- **Implementar:** nomes acessíveis nos botões de ação sem texto, estado selecionado coerente, mensagens no host existente e texto legível em 720p. Mostrar bitrate desconhecido como «—», não como prova de 0 Mbps; distinguir medição instantânea sem tráfego de metadado não declarado. Mostrar código de idioma desconhecido sem o rotular falsamente como Automático.
- **Limites:** não adicionar descrições repetidas a ícones meramente decorativos; não truncar informação diagnóstica necessária; não construir uma pontuação de saúde a partir de uma amostra.
- **Aceitação:** foco/ação identificáveis por comando e leitor de ecrã; diagnóstico não acusa avaria por ausência de metadados; mensagens não se sobrepõem a um erro obsoleto.

## 4. Implementações aprovadas — integridade, segurança e distribuição

### INT-01 — Identidade de backup sem reutilização de IDs de outra instalação

- **Origem:** F1 BUG-P1-04; F4 BUG-P1-03; F6 R6-DAT-01.
- **Evidência:** `Core:backup/BackupManager.kt:1038,1086–1089` conserva IDs crus quando não existe mapeamento, incluindo `userData` sem SOURCES.
- **Implementar:** mapear perfil e fonte por identidades exportadas/resolvidas, mesmo no restauro seletivo. Uma associação não resolvida fica pendente ou é relatada; nunca apontar ao ID numericamente igual do destino. Não remover a exportação seletiva como atalho.
- **Aceitação:** duas instalações com IDs coincidentes mas fontes/perfis diferentes; exportação sem Fontes; fonte omitida; dois nomes iguais; restauro repetido. Credenciais, intervalos de atualização e preferências completas mantêm-se.

### INT-02 — Backup completo não pode esconder falhas de leitura

- **Origem:** F3 DATA-01; F6 MELH-04; F7 adenda 4.
- **Evidência:** `Core:backup/BackupManager.kt:216,321–342` converte algumas exceções de leitura em listas vazias.
- **Implementar:** exportação completa falha explicitamente quando uma secção pedida não pôde ser lida, ou produz um estado parcial inequívoco aceite pelo utilizador. Publicar ficheiro final só após fecho e validação. Conservar o último backup válido. Resumo de restauro deve distinguir importado, reutilizado, pendente e omitido. Auditar chamadas Core de exportação sem cifra: um backup completo sensível requer contentor cifrado; uma exportação sem segredos precisa de política explícita, sem amputar os backups cifrados atuais.
- **Aceitação:** falha DAO/ficheiro/legenda não aparece como «backup completo»; erro não substitui o backup bom. Fixtures de exportação devem passar o contrato real de importação sem aplicar dados numa BD em produção.

### INT-03 — Metadados de cifra validados antes de derivar a chave

- **Origem:** F3 SEC-12/13, com as correções da §22; F6 S11/MELH-04.
- **Evidência:** `Core:backup/BackupCrypto.kt:65–69` aceita parâmetros sem limites suficientes; `BackupContainer.kt:176,238` usa esquema de contentor distinto do esquema dos campos.
- **Implementar:** validar KDF, esquema esperado pelo chamador, salt, nonce e limites de iterações compatíveis com os backups suportados, antes de trabalho caro. Não impor um mínimo novo que torne ilegíveis backups antigos legítimos. Derivação fora da thread principal.
- **Aceitação:** contentor cifrado, campos cifrados legados, senha errada, esquema desconhecido, metadados truncados, 0/negativo/Int.MAX_VALUE. **Proibido** comparar todos os esquemas com uma única constante: isso quebraria backups válidos.

### INT-04 — Exportações manuais distinguíveis e confirmação da palavra-passe

- **Origem:** F6 S9/S18; F3 SET-06.
- **Evidência:** `Core:backup/BackupManager.kt:276,1557` usa nome fixo; `App:features/settings/BackupScreen.kt:435–447` tem um só campo.
- **Implementar:** nome único ou confirmação explícita na exportação manual; repetir palavra-passe apenas ao criar backup. Conservar rotação/nomenclatura automática e pastas isoladas já usadas pelo export remoto.
- **Aceitação:** dois backups manuais coexistem; palavras-passe diferentes não iniciam exportação; restore pede a senha uma vez. Não cancelar escrita/importação arbitrariamente para acrescentar um botão Cancelar.

### INT-05 — Proteção de perfil conservada no restauro

- **Origem:** F5 R4-B01; F6 MELH-02; F3 SEC-04.
- **Evidência:** `Core:backup/BackupManager.kt:879–883` pode substituir `isKids` e `pinHash` num perfil existente.
- **Implementar:** preservar proteção local por defeito; alteração explícita exige autorização local e aparece no resumo. Numa instalação nova, permitir restauro legítimo da proteção do backup. Não ignorar PIN/Kids sempre nem considerar um backup como autenticação do espectador.
- **Aceitação:** merge em perfil protegido não o desbloqueia; restauração autorizada continua completa; hashes antigos mantêm leitura; backups de outros perfis não trocam proteções por coincidência de ID/nome.

### INT-06 — Limite central de tentativas de PIN com compatibilidade

- **Origem:** F5 R4-B07/B08; F6 S11; F3 SEC-04.
- **Evidência:** `Core:util/Pin.kt:13–24` usa hash rápido legado e vários gates não têm política central de tentativas. A autorização de editar/apagar com grants já existe e deve ser conservada.
- **Implementar:** atraso temporário progressivo por alvo, persistido e partilhado entre os gates, sem bloqueio permanente por reinício/relógio. Migração posterior de hash versionado, lendo o legado e regravando após autenticação bem-sucedida. Reduzir restrições de um Kids sem PIN deve pedir autorização adulta quando a política parental estiver ativa.
- **Aceitação:** seleção/edição/remoção/backup seguem a mesma política; PIN antigo funciona; trocar perfil não transfere bloqueio indevidamente. Não exigir novo PIN de seis dígitos a todos nem negar todos os canais sem categoria durante um update silencioso.

### INT-07 — Revogação e limites no sincronismo local existente

- **Origem:** F5 R4-B05/B06; F6 R6-CORE-01/DAT-04/DAT-05.
- **Evidência:** `Core:sync/local/LocalSyncManager.kt:173,275` mantém snapshot de segredos; `LocalSyncClient.kt:103,118` lê sem teto. `App:LocalSyncViewModel.kt:87,197–220` omite falha de preview e deixa ficheiros recebidos.
- **Implementar:** revogação imediata na autoridade ativa; não substituir par existente por `id` apresentado pelo cliente sem autorização; limites por rota aos bytes efetivamente lidos, com ou sem Content-Length; staging com dono, limpeza em IO e TTL apenas de temporários próprios sem uso. Distinguir «ficheiro recebido» de «restauro aplicado».
- **Aceitação:** desemparelhar→401 no pedido seguinte; tamanho ausente/falso/excessivo/truncado/cancelamento; preview inválido tem erro visível; ficheiro em uso não é apagado. Não voltar a expor o menu ocultado nem mudar o protocolo Base64 usado pelo browser e pares antigos.

### INT-08 — Publicação com versão e assinatura coerentes

- **Origem:** F2 B2/R5-D3; F5 R4-B20; F6 R6-REL/MELH-01.
- **Evidência:** `.github/workflows/android.yml:113–121` trata três componentes e dev 99999; `app/build.gradle.kts:175–179` e workflow `:194–195` permitem fallback debug.
- **Implementar:** uma fonte de versão, parser explícito para o formato realmente usado e códigos superiores às versões já distribuídas. Falhar **publicação/distribuição** sem a assinatura correta. Manter Sync Gradle, testes e compilação local possíveis sem secrets. Conservar a chave de assinatura instalada.
- **Aceitação:** tags com quatro componentes, tags inválidas, build local sem keystore e tentativa de publicação sem chave. Inspecionar versão, package, certificado e ABIs do artefacto que vier a ser gerado; não basta sucesso do job.

### INT-09 — CI e integridade dos inputs de build

- **Origem:** F5 R4-B21/22/23; F3 F8/QA-01/REL-08/09; F1 MEL-06/07.
- **Implementar:** permissões mínimas com write só no job de release; Actions fixadas por SHA; escolha determinística dos assets existentes; checksum oficial da distribuição Gradle e verificação revista das dependências, incluindo libmpv. CI deve testar Core personalizado explicitamente, lint release e publicação do snapshot; testes Android em emulador/dispositivo ficam num job próprio.
- **Limites:** não inventar equivalência ao upstream, remover universal, atualizar targetSdk/bibliotecas em lote, confiar cegamente em checksums gerados localmente ou eliminar snapshots de rollback. O snapshot já tem hash, defesa contra zip-slip e publicação atómica; conservar esses mecanismos.
- **Aceitação:** PR não recebe escrita desnecessária; ficheiro ausente não entra na lista de release; dependência adulterada falha; snapshot preserva determinismo. Alterações no Core só chegam à App após validação e atualização coerente de ZIP+lock.

## 5. Implementações aprovadas — EPG, parsing e base de dados

### DAT-01 — EPG truncado não pode substituir o guia válido

- **Origem:** F2 R5-C2; F6 C03/C04. **Evidência:** `Core:parser/XmltvParser.kt:34,89–92,111–126,286–293`; `repository/EpgRepository.kt:353,375,409–421`.
- **Problema:** o parser pode terminar após erro sem exigir documento completo; o chamador pode considerar a leitura concluída, promover o temporário e podar dados. Também falta um teto por nó de texto e a deteção de gzip pressupõe uma leitura de dois bytes completa.
- **Implementar:** separar completude estrutural do documento da tolerância por programa. Preservar a cache anterior e publicar a nova de forma atómica; executar poda apenas com prova de completude. O staging atual refere-se ao ficheiro, não a uma transação global Room: linhas válidas já recebidas podem permanecer com resultado parcial, sem declarar sincronização completa. Não envolver a transferência/parse completos numa única transação da BD. Limitar acumulação de texto, consumindo corretamente o restante nó; ler a assinatura gzip até dois bytes/EOF. Um guia válido vazio precisa de resultado explícito diferente de documento inválido.
- **Aceitação:** XML cortado, ligação interrompida, `<desc>` enorme e stream que devolve um byte por leitura não apagam o guia anterior. Uma atualização válida posterior funciona. Não materializar todo o XMLTV em RAM.

### DAT-02 — Detetar tipo de fonte sem confundir indisponibilidade com «não é Xtream»

- **Origem:** F2 R5-C1/C5. **Evidência:** `Core:parser/XtreamClient.kt:511–534` converte timeout/exceção em false; `setup/SourceImporter.kt:235–247` usa essa decisão para o tipo persistido. Um M3U sem entradas utilizáveis também pode chegar ao sucesso de sincronização.
- **Implementar:** resultado positivo/negativo/indeterminado; não converter timeout, 429 ou 5xx em mudança permanente de tipo. Na importação M3U, contabilizar entradas aceites/rejeitadas e informar formato inválido ou lista vazia sem apagar conteúdo saudável por engano.
- **Aceitação:** painel temporariamente indisponível permite Retry sem voltar a preencher nem mudar tipo; ficheiro HTML de erro não aparece como catálogo sincronizado; lista vazia intencional segue política explícita.

### DAT-03 — Construção correta de URLs Xtream

- **Origem:** F2 M6; F6 C-A7/V-02. **Evidência:** `Core:parser/XtreamClient.kt:227,366,444–458,514–515` mistura concatenação de paths e encoding de query.
- **Implementar:** builder único que distingue segmentos de path de parâmetros de query; codificar valores lógicos exatamente uma vez. As credenciais de conta/probe nas queries já são codificadas; corrigir as lacunas nos paths e IDs, sem recodificar esse percurso saudável. Não usar `URLEncoder` indiscriminadamente nos paths: espaços, `/`, `?`, `#`, `%` e `+` têm semânticas diferentes. Validar/escapar IDs de API conforme o contrato, preservando IDs legítimos dos portais e definindo o tratamento de encoding legado.
- **Aceitação:** utilizador/senha com caracteres reservados, credenciais já existentes, portas, subpaths e links de diferidos. Testar que URL enviada é correta e que logs não expõem os valores.

### DAT-04 — Títulos Xtream e charset M3U conservadores

- **Origem:** F6 C01/C02; F2 M7/R5-C3/C4.
- **Evidência:** `Core:parser/XtreamClient.kt:437–438` descodifica Base64 permissivamente; `parser/M3uParser.kt:85` assume UTF-8.
- **Implementar:** descodificar títulos apenas quando a hipótese Base64 produz UTF-8 válido e texto plausível; conservar texto original em casos ambíguos. Para M3U, respeitar BOM e UTF-8 válido; quando necessário, opção de charset por fonte ou fallback justificado por amostras. Tratar atributos inválidos com métricas/diagnóstico sanitizado.
- **Aceitação:** `News`, `Test`, `Live`, títulos Base64 reais e nomes acentuados não são corrompidos. UTF-8 válido continua com o mesmo resultado. Não introduzir deteção universal nem fallback Windows-1252 obrigatório que estrague outros alfabetos.

### DAT-05 — Triggers FTS e restauração de índices com contrato verificável

- **Origem:** F3 DB-01/02/03/06/07/08; F6 C05.
- **Evidência:** `Core:database/BulkInsertHelper.kt:40,101–145` guarda/restaura estado de DROPs de forma vulnerável a exceção parcial; `database/OwnTVDatabase.kt:1547–1557,1592–1620` não cobre integralmente triggers FTS no heal/probe.
- **Implementar:** obter DDL canónico do esquema Room realmente gerado, sondar objetos necessários e reparar só ausentes/incorretos. Registar o que foi removido antes de cada DROP e restaurar de forma reexecutável, mesmo após falha parcial; só remover índices geridos pelo contrato. Validar FTS após REPLACE, bulk e migração, incluindo a política de `recursive_triggers` em cada ligação relevante.
- **Limites:** não colar uma lista de 16 triggers sem conferir a geração atual; não ativar PRAGMA global nem reconstruir todo FTS a cada abertura sem testar a semântica e o custo. Rebuild apenas quando necessário para consistência e em momento controlado. Não apagar a BD.
- **Aceitação:** apagar um trigger numa cópia de teste; falhar no terceiro DROP e durante restauração; INSERT/UPDATE/REPLACE/DELETE após bulk/migração mantêm pesquisa sincronizada. Atualizar entidade, migração, schema exportado e contratos de índices em conjunto quando houver DDL novo.

### DAT-06 — Operações EPG em lote sem perder dono nem atomicidade

- **Origem:** F2 R5-A1; F3 DB-05/O10.
- **Evidência:** `App:features/epg/EpgViewModel.kt:887,956` chama repetidamente `setEpgMatch`; `Core:customize/CustomizationStore.kt:244–248` faz uma edição por chamada. A substituição de cache do guia também tem DELETE por chave quando existe operação em lote.
- **Implementar:** um update atómico das associações do mesmo perfil/revisão, revalidando owner e conservando escolhas manuais; agrupar DELETEs pelo orçamento real de parâmetros dentro da mesma transação. Proteger sequências clear/insert relacionadas que sejam alcançáveis na sincronização TV.
- **Aceitação:** trocar perfil durante lote não aplica associações ao perfil novo; cancelamento/falha não deixa metade das escolhas; atualização do guia mantém resultados idênticos. Não aplicar `chunked(450)` cegamente a queries com LIMIT/ORDER BY nem mudar caminhos VOD não utilizados por esta App.

### DAT-07 — Pendências de restauro sem perda por concorrência

- **Origem:** F6 R6-DAT-02. **Evidência:** `Core:backup/UserDataResolver.kt:371–379` resolve um snapshot e substitui pendências, enquanto `addPending` pode acrescentar registos.
- **Implementar:** serializar o ciclo de resolução/escrita e acrescentos, ou remover apenas chaves resolvidas por merge sob a mesma autoridade. Não manter um `DataStore.edit` aberto enquanto se consultam muitas linhas Room. Capturar perfil/fonte da operação; não usar o perfil ativo tardio para decidir o destino. O facto de cada edit ser serializado pelo DataStore não protege, sozinho, o ciclo read-resolve-replace fora do edit.
- **Aceitação:** intercalar `addPending` com resolução; não perder o novo registo, não duplicar resolvidos e não cruzar perfis/fontes. Mesmo que histórico/favoritos estejam ocultos, conservar compatibilidade dos dados de backup.

### DAT-08 — Cache EPG limitada sem adulterar resultados

- **Origem:** F6 C06/R7-MELH-24. **Evidência:** `Core:live/LiveEpgReader.kt:73,78` mantém mapas de now/next e linhas sem política de capacidade equivalente à cache de histórico já limitada.
- **Implementar:** capacidade conservadora e eviction com acesso sincronizado; chave/invalidação por fonte, perfil ou política efetivamente usada, offset e revisão do guia. Separar eviction de dados de cancelamento de pedidos em curso.
- **Aceitação:** percorrer milhares de canais respeita o teto e o canal regressado mostra EPG atual; mudança de offset/perfil/revisão invalida corretamente. Escolher capacidade por memória/uso real; não prometer hit rate de 90% nem ganho percentual sem medir.

### DAT-09 — Catálogo/EPG respeitam Retry-After

- **Origem:** F6 C12/R7-MELH-25. **Evidência:** `Core:network/HttpClient.kt:91–100` faz backoff curto em 429 sem transportar plenamente a espera pedida pelo servidor; o player já tem tratamento próprio que deve ser preservado.
- **Implementar:** propagar status e Retry-After sanitizados, interpretar segundos/data HTTP com limites e cancelamento, guardar o próximo instante permitido no âmbito correto. Se ultrapassa o orçamento da operação, devolver adiamento/erro explícito ou reagendar trabalho; não bloquear a interface nem voltar a pedir 750 ms depois.
- **Aceitação:** 429 com 60 s não provoca novo pedido antes do prazo; ausência/header inválido mantém política controlada; cancelar termina a espera. Não confundir recusa HTTP com «offline» nem aplicar a espera de uma conta a fornecedores independentes.

## 6. Implementações aprovadas — reprodução e HLS

### PLY-01 — Confirmação de libertação também na abertura interna de diferidos

- **Origem:** F6 R6-PLY-04; F3 PLY-13 apenas na parte de ownership/libertação, não na proposta de expirar pausa.
- **Evidência:** `App:features/live/LiveViewModel.kt:2483–2484,2686–2690` chama `releaseForArchive()` e abre `player.play()` sem aguardar confirmação de libertação do Exo; um dos percursos também chama `player.stop()`. O percurso externo `:2426–2427` já espera drain/release. `Core:player-core/LiveTuneController.kt:408–413` não fornece sozinho a confirmação síncrona.
- **Implementar:** gate único com dono/geração que aguarda os recursos pertinentes antes de iniciar o leitor de arquivo. Revalidar seleção, perfil e CatchupOwner depois de cada suspensão; cancelamento abandona o pedido novo, não a limpeza necessária do anterior.
- **Aceitação:** direto→diferido→direto, recuo, duas seleções rápidas e sair durante drain. Num deadline esgotado, não fingir fecho nem iniciar outra ligação sobreposta. Manter programa escolhido e correção temporal; falha de diferido não abre direto silenciosamente.

### PLY-02 — Classificação de falhas antes de fallback e aprendizagem

- **Origem:** F3 PLY-14/CAT-1. **Evidência:** `Core:player-core/LiveTuneController.kt:780` e `LiveLadder.kt:193–209` podem tratar falha não HTTP/transporte como incompatibilidade de formato e memorizar uma conclusão demasiado ampla.
- **Implementar:** distinguir incompatibilidade comprovada de formato/extrator, codec/decoder, transporte, recusa HTTP, timeout, cancelamento e handover. Só aprender uma limitação de HLS quando a evidência realmente a demonstra. Não ensinar «canal sem HLS» a partir de codec/recurso ocupado ou 403/404.
- **Aceitação:** tabela de falhas sintéticas e fallback; HLS-only nunca é ultrapassado; exceção manual por canal continua a funcionar; um erro de sessão não altera permanentemente as preferências. Aprendizagem futura deve expirar com revisão da fonte/motor e poder ser reposta.

### PLY-03 — Play/Pausa apresenta a intenção correta

- **Origem:** F3 PLY-16. **Evidência:** `App:player/PlayerHudChrome.kt:293`, `MiniPlayer.kt:64,135` apresentam ícone por `isPlaying`, mas o toggle responde à intenção `playbackRequested`.
- **Implementar:** usar intenção para ícone, rótulo e ação; usar reprodução efetiva para progresso/estado Buffering. Uma emissão a carregar com intenção de Play deve oferecer Pausa, não Play.
- **Aceitação:** início, buffering, pausa, retoma e troca de motor não apresentam uma ação que faz o contrário do anunciado. Não arrancar áudio automaticamente para «corrigir» o indicador.

### PLY-04 — MediaSession/foco de áudio têm um dono, incluindo multiview

- **Origem:** F6 R6-PLY-02/03.
- **Evidência:** `App:features/multiview/MultiviewState.kt:142–150` gere mute dos mosaicos fora de PlaybackSession; `OwnTVShell.kt:318–322` faz attach mas precisa de teardown coerente quando o shell desaparece.
- **Implementar:** integrar o mosaico audível na autoridade existente de foco/transportes, sem várias sessões concorrentes. Desassociar apenas se o efeito ainda detém o mesmo engine/sessão; impedir que o dispose antigo desligue o owner novo. Perda de foco segue a política já existente de pausa, sem reintroduzir ducking/volume variável.
- **Aceitação:** trocar mosaico audível, Assistant/outra app, mudar perfil, fechar/reabrir shell e Home; só o dono atual responde às teclas. Multiview continua disponível e gravações não são interrompidas pelo fecho da sessão de visualização.

### PLY-05 — Freeze frame sem reciclar um bitmap ainda apresentado

- **Origem:** F3 PLY-09; F7 adenda 4. **Evidência:** `Core:player-core/OwnTVPlayer.kt:1627–1630` publica frame e recicla o anterior; `App:player/MpvVideoSurface.kt:138–141` usa a imagem no Compose.
- **Implementar:** contrato de ownership do frame publicado. Para as imagens já limitadas, avaliar deixar o GC gerir os frames expostos ou usar lease/release confirmado pela UI. Não copiar bitmaps indiscriminadamente na thread principal e não fazer `recycle()` enquanto puderem estar em desenho.
- **Aceitação:** sucessivos handovers/saídas não usam bitmap reciclado; medir heap e memória gráfica. Classificação é risco de concorrência sustentado pelo mecanismo, não crash de box reproduzido nesta revisão.

### PLY-06 — Recuperação orientada a eventos, cancelável e respeitando pausa

- **Origem:** F2 M3; F5 R4-B18; F6 A.1.3/A.1.10; F3 PLY-13/15 apenas com adaptação.
- **Evidência:** `App:features/live/LiveViewModel.kt:2146–2151` faz polling de 50 ms; `ChannelAlternatives.kt:134–138` converte cancelamento em false sem distinguir o chamador. No Core, esperas pausadas têm semântica deliberada e testes específicos.
- **Implementar:** trocar o polling da recuperação por sinal de outcome com owner e prazo, permitindo subscrição antes de iniciar a tentativa para não perder um evento síncrono. Propagar cancelamento do chamador; falha do filho continua distinta. Pausa deliberada suspende tentativa/tempo pertinente; stop, erro terminal ou troca de owner deixam a observação sem disparar fallback indevido.
- **Aceitação:** pausa mais longa que o deadline, Play posterior, erro enquanto pausado, stop, troca perfil/canal e cancelamento em cascata. **Não** sair/avançar só porque `playbackRequested=false`, nem consumir deadline da pausa e arrancar o motor de destino por iniciativa da App.

### PLY-07 — Zapping não deve ficar limitado às páginas visíveis

- **Origem:** F6 D-A9/R7-MELH-15. **Evidência:** `LiveScreen.kt:814` passa `itemSnapshotList.items`; `LiveZapList.kt:131–138` adota a lista como contexto completo.
- **Implementar:** distinguir contexto completo/parcial, preservar filtros/ordem/ocultações/grupos e carregar IDs ou uma janela adjacente com limites, revalidando geração e canal âncora. Não substituir a seleção pela categoria física do fornecedor quando o utilizador veio de um contexto personalizado.
- **Aceitação:** CH± chega a canais fora da primeira página e regressa corretamente, com ordenação personalizada e versões agrupadas. Não carregar todas as entidades de 50 mil canais na RAM nem antecipar streams vizinhos para resolver paginação.

### PLY-08 — Continuidade e gravação com erro controlado

- **Origem:** F5 R4-B09/B10; F6 HUD-REC; complemento de UI-09.
- **Evidência:** `LiveViewModel.kt:384–436,2536–2554` contém operações que podem falhar durante gravação/continuação de catch-up sem resultado completo para a UI.
- **Implementar:** resultado tipado até ao HUD, captura de falha operacional com owner e cancelamento preservados. Erro ao encontrar o programa seguinte termina a continuação de forma controlada e permite decisão do utilizador.
- **Aceitação:** erro DAO/rede/perfil trocado não fecha a App; não continuar noutro programa/canal nem iniciar direto como recuperação implícita. Não duplicar a lógica de UI-09: implementar os dois como um contrato ponta a ponta.

### PLY-09 — OSD do canal selecionado e foco real no multiview

- **Origem:** F3 PLY-01/02; F7 adenda 4. **Evidência:** `PlayerHud.kt:599–605` usa metadados assíncronos no flash de zapping; `MultiviewScreen.kt:90–92` associa requesters à estrutura e faz pedido inicial uma vez.
- **Implementar:** o OSD identifica seleção pendente/confirmada por owner, sem mostrar o canal anterior por atraso dos metadados. No multiview, associar foco à identidade do mosaico e pedir foco só quando a ação exigir, após composição.
- **Aceitação:** A→B→C rápido não mostra A como C; adicionar/remover mosaico mantém o foco real no destaque correto. Não reinicializar motores por alteração de foco e não afirmar que seleção pendente já reproduz.

## 7. Otimizações e limpeza aprovadas com âmbito pequeno

### OPT-01 — Formatação e espaço em disco fora do progresso frequente

- **Origem:** F6 PERF-FMT/PERF-STOR/REC-LABEL; F2 R5-B4.
- **Evidência:** `RecordingsScreen.kt:689,797`, `DownloadsScreen.kt:376,472` criam formatters; `RecordingsViewModel.kt:66` e `DownloadsViewModel.kt:102` recalculam armazenamento com emissões de progresso.
- **Implementar:** formatter por locale com confinamento correto — `NumberFormat` não é thread-safe para partilha global. Separar progresso e consulta de espaço; amostragem/coalescência limitada enquanto o ecrã está visível, mais invalidação imediata por raiz/ficheiros/erro de armazenamento. Relógio dos rótulos Retry/Recuperar só onde necessário.
- **Aceitação:** progresso continua fluido; espaço atualiza ao apagar/mudar destino; a consulta informativa não atrasa nem substitui os controlos reais de quota antes da escrita. Medir chamadas/tempo em vez de prometer X%.

### OPT-02 — Remover resíduos comprovadamente mortos

- **Correção da reanálise de 23/10/2026:** a evidência seguinte é histórica. O picker de arranque agora chama `setStartupChannel` (`features/shell/components/SettingsScreen.kt:1409–1419`) e o de cor também funciona (`:1476–1482`); não os remover. `GuideLoadGate` já descreve coalescência. Revalidar consumidores atuais antes de qualquer limpeza; ver secção 24.
- **Origem:** F3 SET-01/05, DAT-12/DAT-21; F6 S4/S13; F7 reavaliação UX-03/04.
- **Evidência:** `SettingsScreen.kt:211,1394–1402` picker só fecha; `:228,1413–1437` layout VOD só fecha; `MetadataSettingsScreen.kt:254` bloco `if(false && ...)`; `LiveViewModel.kt:947–948,1321–1327,1876` mapa `versionGroupKeys` só escrito/limpo. `ProgrammeReminderStore.kt:96–104,131,143` mantém chaves redundantes com remoção dos lembretes sob o mesmo lock.
- **Implementar:** remover blocos/estado/imports/strings sem consumidores reais da App TV e corrigir comentários que prometem opções retiradas. Eliminar o mapa write-only. Remover `dismissedKeys` apenas com regressões de dismiss/consumo/reagendamento. Clarificar «prewarm» se o comportamento real é coalescência de invalidations (`GuideLoadGate.kt:3–24`).
- **Aceitação:** compilação/lint e testes dos percursos preservados; pesquisa global de consumidores antes de cada remoção. Não remover APIs/tabelas Core, contratos de backup, Room KTX ou diretórios de rollback com base apenas em não aparecerem no menu.

### OPT-03 — Testes de alcance de definições para o produto realmente suportado

- **Origem:** F3 SET-01; F6 R7-MELH-04; F1 MEL-09.
- **Implementar:** matriz explícita `opção suportada → entrada UI/atalho → setter → persistência → aplicação no engine`, com testes das opções que alteram reprodução e validação de defaults nas fixtures. Permitir exceções documentadas para configuração interna, legado e opções intencionalmente ocultas.
- **Aceitação:** um controlo de hardware/áudio/buffer/HLS altera o valor efetivamente entregue ao motor; reiniciar preserva a escolha; uma chave legada não obriga a reintroduzir o menu. Não exigir mecanicamente que todo setter tenha um composable consumidor.

### OPT-04 — Contratos de intenção de reprodução e comentários coerentes

- **Origem:** F3 R7-03/R7-09 e B4. **Evidência:** `Core:player-core/PlaybackEngine.kt:15,82–83` oferece defaults frágeis para intenção e setter; as implementações reais já os substituem. `PlaybackSession.kt:29,34,232` conserva nomes DUCK/PAUSE apesar da política real de pausa.
- **Implementar:** tornar explícito/obrigatório o contrato de intenção nos adaptadores e fakes; limpar enum/parâmetro/comentário enganador quando não tem consumidor real. Alteração preventiva de manutenção, não correção de um bug atual demonstrado nos motores.
- **Aceitação:** comandos de Play/Pausa idempotentes, buffering não altera intenção e todos os implementadores compilam. Ganho interno continua unitário; nenhuma atenuação ou amplificação nova.

## 8. Bons candidatos que exigem medição ou desenho adicional

**Não entram como patches imediatos.** Passaram como linhas de investigação, não como alterações já justificadas nos seus detalhes.

| ID | Propostas de origem | O que fazer primeiro / condição para avançar |
|---|---|---|
| MED-01 | F3 DB-04; F6 C07 | `EXPLAIN QUERY PLAN` e catálogo TV representativo para índices EPG/catch-up. Medir leituras, escritas e espaço. Não importar índices de filmes/séries; `(catchup,sourceId,name)` não garante ordenar todos os casos. `NOT IN → EXISTS` exige equivalência com NULL e scoping por fonte. |
| MED-02 | F2/F3 DAT-01/02/03/09, PERF-01 | Trace de arranque, navegação longa e heap antes de mudar Paging, 44 flows ou Eagerly. Manter snapshots completos onde a prioridade/ordem global dos grupos exige. Paginar apenas entidades/IDs corretos; maxSize finito não pode quebrar foco nem limitar CH±. |
| MED-03 | F2 B3; F3 PERF-02; F1 BUG-P3-01 | Confinar progresso/relógio ao componente necessário e atrasar leitura de estado para draw/layout quando o trace o justificar. Não declarar 12 recomposições por segundo ou ganho 2×–4× como medição desta revisão. Não reintroduzir animações para otimizar animações inexistentes. |
| MED-04 | F3 O9; F7 DAT-04 | Medir gravação/timeshift com reprodução simultânea antes de reduzir `fsync`, alterar remux ou transformar todo backup em foreground. Custo de I/O não autoriza perder durabilidade. Publicação do backup continua atómica. |
| MED-05 | F6 DL-FGS; F2 R5-B1 | Testar recusa de `setForeground` nas versões Android relevantes e recuperar/reagendar mantendo fila/estado. **Não** engolir a exceção e continuar trabalho longo sem foreground permitido. As restrições do sistema continuam aplicáveis. |
| MED-06 | F6 C10/C11; F3 DAT-06/19 | Coordenador terminal mpv e diagnóstico de recursos retidos só com testes de executor/JNI bloqueados. Daemon/timeout/cancelar scope não provam fecho; jobs de retirement precisam de terminar antes da sua autoridade desaparecer. |
| MED-07 | F6 C08/C09/A.1.11; F5 R4-B16 | Updater: cliente exclusivo HTTPS sem downgrade, digest do asset quando disponível, versão/package/ABI e certificado com rotação/lineage. Definir compatibilidade para releases antigas sem digest e hosts CDN válidos. Não impor allowlist incompleta nem alterar o cliente IPTV para este fim. |
| MED-08 | F3 SEC-14; F6 MELH-03 | Streaming de Base64 mantendo exatamente o protocolo, limites do servidor e carry correto. Upload atual tem limite de 16 MiB de corpo, diferente de 16 MiB de backup binário. TLS autenticado requer versão de protocolo/pareamento e compatibilidade browser/pares; é projeto separado. |
| MED-09 | F5 R4-B12/B14; F1 BUG-NIN-02/05 | Mapear todos os emissores FileProvider, raízes configuráveis, backups, updates, SAF e leitor externo. Depois restringir concessões/caminhos onde não forem necessários. `exported=false` já existe; não chamar diretórios públicos nem estreitar cegamente para duas subpastas. |
| MED-10 | F5 R4-B04/B08; F6 MELH-02 | Lista branca Kids/credencial adulta independente são mudanças de produto: definir migração e preview autorizados. Negar tudo sem categoria durante update silencioso oculta canais legítimos e não é patch neutro. |
| MED-11 | F2 M7; F6 C02/A.1.7 | Charset e headers M3U ambíguos precisam de fixtures reais. Reparar `&` cru como continuação de header não é regra universal; preservar percent-encoding e entradas válidas. Amostras sintéticas sem URLs/credenciais reais. |
| MED-12 | F2 B4; F6 DL-DEL-PLAY/DL-IDPREM/DL-MSG | `DownloadEngine` aceita 416/EOF sem prova suficiente de completude, mas os enqueue atuais são VOD; downloads de programas EPG usam RecordingManager/RecordingEngine. Hardening no Core legado é válido se o percurso continuar alcançável: validar Content-Range, identidade, validators e tamanho; stop/await antes de apagar o URI exato. Não vender isto como correção já demonstrada do download EPG. |
| MED-13 | F7 colisão de legendas e MIME de legenda | Hash de conteúdo/identidade pode resolver ficheiros homónimos; formatos externos devem ser identificados antes de mapear MIME. Validar consumidores legados e suporte real dos motores. Não restaurar OpenSubtitles ou VOD nem anunciar formatos que o motor não suporta. |
| MED-14 | F3 R7-01/R7-10/CAT-1 | Bloqueio A–B–A: medir por host/conta/canal e respeitar o tempo escolhido. Recusa HTTP não justifica retries agressivos. Aprender sucesso com TTL/revisão apenas após PLY-02; nunca ignorar espera manual, converter 403 em «formato errado» ou manter spinner durante minutos por defeito. |
| MED-15 | F1 MEL-01; F3 F15 | Extrair estado/dispatch/diálogos de monólitos gradualmente, depois das correções, com API pública e comportamento preservados. Não reescrever arquitetura como pré-requisito para um fix local nem trocar DI só porque os parâmetros são posicionais. |

## 9. Inovações que fazem sentido, depois de estabilizar

São **propostas de produto**, não bugs nem autorização de implementação automática. Todas devem aproveitar as estruturas existentes, ser opcionais quando alteram comportamento e evitar mais menus no uso diário.

| ID | Ideia aprovada para desenho | Como fazer sem prejudicar |
|---|---|---|
| NOV-01 | Diagnóstico guiado «porque não abriu?» | Reunir outcome, estado HTTP, motivo do handover, buffer e janela HLS num snapshot da tentativa; sessão detalhada com duração limitada e exportação manual sanitizada. Reutilizar diagnóstico atual, sem telemetria remota, polling por frame ou score arbitrário 0–100. Origens F1 INN-03, F3 O5/CAT-6, F6 I-02/I-13. |
| NOV-02 | Resumo do que um restauro vai alterar | Expandir o preview já existente com configurações alteradas, proteção do perfil, fontes resolvidas/pendentes e omissões. Confirmar antes de aplicar; validação e atomicidade continuam no Core. Não apresentar preview de passwords em claro. Origens F1 INN-01/02, F2 D11, F6 MELH-04. |
| NOV-03 | Temporizador de sono TV | 30/60/90 min ou escolha manual; ao expirar parar/libertar apenas a sessão de visualização e permitir apagar ecrã conforme o sistema. Nunca parar gravações nem forçar power-off. Não manter WakeLock extra. Origem F5 U1/F3 U1. |
| NOV-04 | Janela de diferidos e gravações mais compreensível | Mostrar disponibilidade declarada/confirmada e expiração estimada; ação manual «Guardar programa» usa o motor de gravação já existente e a quota definida. Não garantir cinco/sete dias que o fornecedor não oferece nem iniciar downloads automáticos. Origens F6 Cofre/HUD, F2 agenda/régua de gravação. |
| NOV-05 | «Desfazer» apenas em alterações reversíveis | Snapshot curto de DNS/ordem/apresentação com revisão, TTL e confirmação; desfazer só se ninguém alterou de novo o valor. Não aplicar a apagamentos de ficheiros, PINs, credenciais ou restauros parcialmente escritos. Origem F6 I-12. |
| NOV-06 | Alternativas do mesmo programa no EPG | Pesquisa local na janela carregada/BD por identidade e horário, com confirmação do utilizador e filtros de perfil/ocultos. Não mudar automaticamente de canal, não assumir equivalência por título parecido e não criar pedidos adicionais de streaming. Origem F6 I-06. |

**Não propor de novo como novidade o que já existe:** ver do início/recuo e diferidos, agrupamento de versões, exceções por canal, atalhos numéricos, backup automático e diagnóstico da transmissão. Melhorar alcance/feedback é diferente de implementar uma segunda solução.

## 10. O que não deve ser aplicado — motivos e alternativa segura

| Proposta dos relatórios | Decisão do council |
|---|---|
| F3 PLY-13/15 e antigo B23: expirar/abortar ao estar `playbackRequested=false` | **Rejeitar.** Destrói pausa deliberada. Os testes existentes `LiveTuneControllerTest.kt:695,755,979,992,1006,1021` verificam pausa antes/durante handover, prazo suspenso e cancelamento. Play/zap pode libertar a espera; não prova deadlock. Aplicar apenas PLY-06 com semântica preservada. |
| F6 C10/C11: daemonificar destroy e cancelar retirementScope como cura de recursos | **Rejeitar o patch literal.** Daemon não interrompe JNI nem fecha FD; cancelar cleanup cedo pode abandonar a libertação. Ver MED-06. |
| Ativar non-IDR/access-unit globalmente para corrigir áudio/lipsync | **Rejeitar.** Não há prova de que a ausência da flag cause dessincronização crónica; pode aceitar quadros incompletos. Já existe compatibilidade explícita por canal. |
| Buffer/array mutável único entre sockets timeshift | **Rejeitar.** Transferências concorrentes têm estado próprio; partilha sem ownership pode corromper respostas. O mecanismo alegado nalguns relatórios nem corresponde ao código atual. |
| Cortar um segmento HLS ou descartar conteúdo porque ultrapassa o alvo de buffer | **Rejeitar.** Objetivo de carregamento não é corte temporal exato; um segmento inteiro pode ultrapassar o alvo. Não cura bloqueio de conta nem demonstra bug. |
| Antecipar streams dos próximos canais/prewarm com decoder extra por defeito | **Rejeitar neste plano.** Consome RAM, decoders e sessões do fornecedor; pode agravar A–B–A. Pré-carregar metadados limitados é outra coisa e exige orçamento. |
| Reutilizar cegamente uma ligação antiga, limpar tokens/URL com timestamp aleatório ou substituir User-Agent para contornar bloqueio | **Rejeitar.** Pode usar sessão expirada, alterar cache/assinatura e violar limites. Manter waits explícitos, owner e erro real. |
| Score de saúde que muda motor/formato/qualidade sozinho; auto-skip de gaps/cenas | **Rejeitar como correção automática.** Pode criar flapping, saltar programas ou ultrapassar HLS-only/prioridade escolhida. Diagnóstico é NOV-01; comportamento novo exige requisito e evidência. |
| Aumentar delays globais, buffers/retries ou reconstruir Exo a cada canal | **Rejeitar sem medição dirigida.** Penaliza canais saudáveis, não confirma fecho no servidor e pode mascarar recusa HTTP. |
| `scheme != SCHEME` universal nos backups | **Rejeitar.** Confunde cifra de campos com contentor; quebra os `.own` atuais. Aplicar INT-03. |
| Chave errada de backup é sempre ignorada por causa de `getOrNull` | **Falso positivo nesse fluxo.** `BackupManager.kt:795–804` valida segredos selecionados antes da escrita; `BackupImportValidation.kt:222–257` autentica. Omissão legada sem chave é caso distinto de UX. |
| Selar username como objeto novo ou retirar credenciais de todos os backups | **Rejeitar o formato unilateral.** Leitura/matching esperam string; o backup completo cifrado tem de conservar URL/utilizador/senha. Definir política de exportação sem cifra, não amputar backup completo. |
| Cancelar importação/restauro em qualquer Back | **Rejeitar.** Pode deixar estado parcial. Conservar operação com dono e publicação atómica; cancelamento só em checkpoints seguros e com recuperação definida. |
| Tornar `MainActivity` não exportada ou remover todo deep link | **Rejeitar.** Quebra launcher/integrações. Validar e consumir a intenção uma vez (UI-08); avaliar confirmação só nos lançamentos externos que realmente a necessitem. |
| Bloquear HTTP globalmente com XML «por cada fonte» | **Rejeitar.** XML é estático e as fontes são escolhidas em runtime; quebra IPTV HTTP, redirects e serviços locais. Preferir HTTPS suportado e aviso, sem reescrever ou proibir ligações válidas. |
| Remover permissões/raízes de armazenamento por princípio | **Rejeitar sem matriz OEM/SAF.** Pode tornar gravações, backups ou reprodução externa inacessíveis em boxes antigas. Ver MED-09. |
| `SCREEN_OFF` garante HDMI-CEC/standby em todas as boxes | **Rejeitar a garantia.** O evento depende de firmware/topologia e o lifecycle já gere encerramento. Só adicionar autoridade nova perante falha reproduzida. |
| Voltar a mostrar filmes/séries, histórico, favoritos, Quick, OpenSubtitles ou LocalSync porque estão «inalcançáveis» | **Rejeitar.** Grande parte é decisão expressa do utilizador. Limpar resíduos App e conservar dados/contratos legados em vez de reintroduzir menus. |
| Mostrar Pesquisa/Último canal no topo para corrigir «descoberta» | **Rejeitar neste plano.** Foram removidos por pedido do utilizador; preservar pesquisa/atalhos existentes sem recolocar botões. |
| «Todos os AsyncImage sem size podem carregar 4K e causar OOM»; forçar sempre 128×128 | **Não confirmado assim.** AsyncImage usa constraints para determinar tamanho. Verificar constraints e origem real antes de impor dimensão que desfoca logos/arte. |
| Apagar `Log.i/w/e` em release | **Rejeitar.** Perde diagnóstico de HLS/áudio/zapping. O HttpClient atual já redige origem/path/query; procurar e redigir lacunas reais, incluindo export/crash, sem silenciar tudo. |
| Encriptar chave BuildConfig elimina segredo distribuído | **Rejeitar a promessa.** Chave e decifrador continuam no cliente. Não criar proxy novo nem migrar serviços de metadados sem requisito. |
| Timestamp real no ZIP, criar version catalog, retirar Room KTX | **Rejeitar.** Timestamp fixo mantém determinismo; catalog já existe; `useWriterConnection` tem consumidor. Remoções exigem prova por configuração/variante. |
| Remover universal/afirmar só armv7 ou link APK sempre 404 | **Não sustentado pelo CI atual.** Já seleciona/verifica ARM universal e cria alias `OwnTV.apk`. Publicação remota pode ter problemas próprios, mas não foi demonstrada nesta análise. |
| Editar `.gradle/core-snapshot` ou importar esquema upstream v49 sobre o v51 | **Rejeitar.** Cache é gerada e verificada; perder-se-iam personalizações/dados. Alterar origem TV controlada, testar e publicar novo snapshot. |
| Apagar vendor, `work/before-recovery`, cache e rollback em massa | **Rejeitar.** Existência de ficheiros não prova código runtime nem autorização para perder recuperação. Inventário e dry-run primeiro; preservar lock/referências e cópias necessárias. |
| Substituir `/backup` Base64 por binário sem negociação | **Rejeitar.** Quebra browser e pares existentes. Streaming compatível ou endpoint novo versionado, MED-08. |
| Stalker dump completo só porque `items.size == totalItems` | **Rejeitar como prova.** Categoria parcial pode ter total internamente consistente; o trecho referido é VOD. Não é correção prioritária da App TV nem garante completude. |
| Todo 416 significa download concluído; todo EOF prova ficheiro completo | **Rejeitar.** Validar Content-Range/identidade/tamanho; distinguir o download VOD do motor de gravação de EPG (MED-12). |
| Congelar sync se catálogo baixar sempre mais de 30% | **Rejeitar regra fixa.** O fornecedor pode remover canais legitimamente. Detetar parsing incompleto e apresentar alterações anómalas com contexto, sem bloquear updates para sempre. |
| Exigir índices novos sem query/EXPLAIN; mudar globalmente recursive_triggers | **Rejeitar aplicação cega.** Índices custam escrita e espaço; REPLACE/FTS/FKs e conexões exigem ensaio. DAT-05/MED-01 definem o caminho seguro. |
| Tratar todos os scopes não cancelados como leaks P0/P1 | **Rejeitar a inferência.** Scope sem trabalho/raiz externa não prova fuga; limpeza de recursos pode precisar de sobreviver ao ecrã. Medir e definir fecho terminal, não cancelar prematuramente. |
| Apagar pesquisa do guia sempre que este fica oculto | **Não aplicar cegamente.** Background real já limpa em `EpgViewModel.onBackground():984–987`. Entrar no player não deve apagar a pesquisa/contexto que permite regressar ao canal. Definir ciclo de sessão explicitamente. |
| Identificar todos os canais/programas por nome aproximado | **Rejeitar.** Pode abrir/gravar outro conteúdo. Identidade estável + fonte/conta; ambiguidades pedem decisão, nunca associação silenciosa. |
| Atualizar targetSdk, BOM, codecs/libmpv e arquitetura num só lote | **Rejeitar neste plano.** Mistura mudanças e torna regressões difíceis de isolar. Atualizações precisam de análise/compatibilidade própria. |
| Trakt/Plex, cast/DLNA, Watch Party, mobile remote, catálogo demo, nuvem automática, temas/OLED, IA remota e estatísticas de histórico | **Fora do âmbito recomendado agora.** São expansões de produto, algumas contrárias à simplicidade/privacidade/uso pedido, sem correção de estabilidade demonstrada. Não entram no backlog aprovado. |
| Upscaling por hardware/buffer como melhoria certa de imagem | **Manter apenas a pendência existente UPSCALING-BETA-01.** Buffer não cria detalhe. Sem implementação nesta fase e sem repropor como cura para streaming. |

## 11. Já implementado ou protegido — não repetir trabalho

| Alegação dos MD | Estado atual a preservar |
|---|---|
| Opacidade -1 invalida backup | Validação atual já trata o valor sentinela; não repetir o suposto P1. |
| Watchdogs Exo não rearmam na retoma | A correção já existe; preservar testes de pausa/retoma e ownership. |
| Gravações usam Movies para leitor externo | `RecordingsViewModel.kt:257` já usa preferência LIVE. |
| Categorias ocultas e nomes personalizados não funcionam no guia | `EpgViewModel.kt:1058–1071,1109–1111` já inclui candidatos renomeados e reaplica elegibilidade após correspondências. Não duplicar filtros nem apagar diferidos válidos. |
| Callbacks EPG sem owner | `EpgViewModel.kt:760–773` já valida fonte e EpgEditSession.Owner. Adaptar batch sem perder essa proteção. |
| CategoryRail usa só índice como key | `CategoryRail.kt:177–180,319` já usa chaves por categoria e requesters associados. |
| Multiview não fecha em Home | `OwnTVShell.kt:735–754` já fecha em ON_STOP e dispose; não duplicar teardown no MainActivity nem remover multiview. |
| ContentFds sem sincronização | Já tem leases por instância e `@Synchronized`; preservar unload/destroy antes de libertar descritores. |
| HeroPreview reutiliza fábrica HTTP antiga e stop destrói scopes reutilizáveis | Factory por geração e stop/await/dispose distintos já existem. ExoSubtitleEngine também separa stop/release. |
| Parar/guardar gravação cancela sempre remux | `RecordingEngine.kt:229–252,413–468` já separa cancelamento de captura/finalização e observação do motor. |
| Backup GET no servidor carrega sempre ficheiro inteiro | O servidor já transmite por streaming; o problema remanescente está no cliente e no upload Base64, não nesse GET. |
| Companion precisa de matriz nova de permissões por modo | A matriz já distingue os modos/rotas; testar após alterações, não implementar uma segunda autoridade. |
| Datas, resolvedor externo, timezone de diferidos, M3U archive conventions, memória Android 14 e timeshift offline são todos trabalho novo | Foram integrados seletivamente no lote OwnTV 5.2.0/Core 1.0.68 de 18/10. Usar o registo da implementação; reabrir apenas uma lacuna concreta com evidência. |
| M3U renumera sempre IDs e gravações colidem todas pelo título | Sync mantém identidade estável; `RecordingRules.kt:104–107` inclui perfil/canal/início. Lacunas pontuais precisam de reprodução, não substituição geral. |
| Migração 5→6 é a principal falha desta versão | Não justifica substituir a cadeia personalizada atual v51. Só estudar BD de desenvolvimento realmente afetada, separadamente; nunca fallback destrutivo. |

## 12. Plano de execução recomendado, por lotes pequenos

| Fase | Objetivo | Itens | Condição de saída |
|---|---|---|---|
| 0 | Fixar base, proteger dados e provas | Inventário do App/Core/snapshot; backup verificado; fixtures de regressão; OPT-03 | Core que será alterado coincide com o input pretendido; caminhos de rollback e dados atuais identificados. Sem APK ou mudança de dependências nesta preparação. |
| 1 | Evitar perda e associação errada de dados | INT-01/02/03/05/04, DAT-07 | Roundtrips completos/seletivos/legados; falha não publica backup válido; preservação de proteção/credenciais/preferências. |
| 2 | Atualizar EPG e catálogos sem danificar estado saudável | DAT-01/02/03/04/06/09, UI-05/06 | XMLTV truncado não promove/poda; Retry de fonte e troca de credenciais seguros; URLs corretos e sem segredo nos logs. |
| 3 | Consistência de pesquisa/bulk | DAT-05, depois DAT-08 | Ensaios SQLite com falhas injetadas, FTS/índices/FKs e migrações; sem destruição de conteúdo. Índices novos só depois de MED-01. |
| 4 | Playback e handover com intenção preservada | PLY-01/02/03/04/05/06/08/09, UI-09, OPT-04 | Testes de pausa, owner/cancelamento, libertação e HUD; teste físico de zapping/diferidos sem nova ligação antes do fecho requerido. |
| 5 | Comando, definições e continuidade | UI-01/02/03/04/07/08/10, PLY-07 | Comando funcional nos extremos/diálogos/regresso; escolhas aplicam e persistem; CH± ultrapassa a janela visível sem perder contexto. |
| 6 | Hardening dos serviços/distribuição e limpeza | INT-06/07/08/09, OPT-01/02 | PIN legado, revogação/tamanhos/temporários; publicação sem fallback debug; recursos/backup preservados após limpeza. INT-08/09 podem decorrer antes em paralelo **se houver publicação programada**. |
| 7 | Medição e produto opcional | MED-01–15, escolher NOV-01–06 | Só avançar em cada alteração após evidência/requisito próprio. Diagnóstico e preview de restauro têm prioridade sobre expansão de produto. |

**Não há dependência de publicar no GitHub para poder testar uma correção local.** Corrigir o CI é importante para distribuição, mas não deve bloquear toda a manutenção de uma App cujo APK é gerado no Android Studio.

**Atualização do Core:** implementar na origem TV controlada, comparar com o snapshot, executar testes e regenerar ZIP/lock coerentes por lote validado. Não deixar várias fases de dados sem teste só para fazer «um único bump». Não aumentar schema por alterações que não exigem DDL; quando exigir, usar a próxima versão realmente disponível, migração aditiva e atualização conjunta de contratos/backup.

## 13. Critérios comuns para não estragar a App

1. Testes proporcionais ao risco: unidade para classificadores/parsers/estado; SQLite real/Room para migrações, FTS e transações; testes de corrotinas com cancelamento/relógio controlados para handover; UI/box para foco e ciclo de vida. Não escrever apenas testes que repetem a implementação.
2. Casos obrigatórios de backup: fonte com URL/conta diferente e mesmo ID; restauro sem SOURCES; nomes repetidos; config por canal; palavra-passe errada; formatos antigos; falha de leitura/escrita; proteção local. O ficheiro deve continuar recuperável.
3. Casos obrigatórios de playback: Exo e mpv, HLS-only e exceção manual, A→B→A respeitando espera, pausa maior que deadline, regresso à lista, direto→diferido→direto, erro/cancelamento durante fecho, áudio/MediaSession e Home. Prova de libertação não é um `delay` nem timeout.
4. Perfis e fontes fazem parte do owner das operações; qualquer resultado tardio deve ser recusado se já pertence a contexto anterior.
5. Nas boxes Xiaomi, Thomson e outra box de compatibilidade, testar 720p/1080p, comando, armazenamento interno/externo/SAF e sessões longas. Android antigo e recente precisam de validação própria para áudio, foreground e permissões.
6. Não declarar resolvidos os cortes de áudio ou recusas do fornecedor com base só em compilação/JVM. Bibliotecas novas, codecs, HDR, AFR e passthrough precisam de comparação isolada e dispositivo real.
7. Aceitar otimização só se não degradar memória, arranque, foco, zapping, qualidade ou consumo de sessões. Medir antes/depois com a mesma lista, rede, box e configuração.

**Percentagens:** esta análise não mediu velocidade, RAM ou taxa de falhas. Não atribuir «+X% de robustez», «zero falhas» ou ganhos de 2×–4× a este plano. Número de propostas, testes aprovados e desempenho são métricas diferentes. UI-09/PLY-08 e outras referências cruzadas são uma intervenção ponta a ponta, não bugs independentes para inflacionar totais.

## 14. Documentação primária usada para validar as decisões

- [Android — fases do Compose](https://developer.android.com/develop/ui/compose/phases): o local da leitura de estado determina a fase invalidada; não basta trocar o nome do Modifier para eliminar recomposição.
- [Coil — AsyncImage/Compose](https://coil-kt.github.io/coil/compose/): resolução considera constraints; ausência de `.size(128)` não prova bitmap original de 4K.
- [Android — HLS no ExoPlayer](https://developer.android.com/media/media3/exoplayer/hls): suporte depende também do formato das amostras; variante HLS, codec e transporte não devem ser confundidos.
- [SQLite — ON CONFLICT/REPLACE](https://www.sqlite.org/lang_conflict.html) e [FTS3/FTS4](https://www.sqlite.org/fts3.html): triggers e índices de conteúdo externo precisam de manutenção coerente; testar REPLACE, bulk e rebuild.
- [Android — FileProvider e diretórios expostos](https://developer.android.com/privacy-and-security/risks/file-providers): evitar âmbito desnecessário, com mapeamento dos fluxos reais e concessões temporárias.
- [Android — configuração de segurança de rede](https://developer.android.com/privacy-and-security/security-config): configurações declarativas são estáticas; não justificam bloquear indiscriminadamente fontes HTTP configuradas em runtime.
- [Android — workers de longa duração](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running): foreground e restrições do sistema exigem tratamento próprio, não apenas ignorar uma exceção.
- [Android — Activities/exported](https://developer.android.com/guide/topics/manifest/activity-element#exported) e [tasks/back stack](https://developer.android.com/guide/components/activities/tasks-and-back-stack): preservar entrada pelo launcher e consumir corretamente lançamentos externos.
- [GitHub — segurança de Actions](https://docs.github.com/en/actions/reference/security/secure-use): permissões mínimas e dependências de workflows controladas.

**Entrega desta intervenção:** apenas este novo ficheiro de análise. Os sete documentos de origem foram conservados; App, Core, dependências, base de dados, APKs, assinaturas e publicação não foram alterados.

## 15. Execução posterior — fase 1

Em **20/10/2026**, a fase 1 foi implementada: **INT-01, INT-02, INT-03, INT-04, INT-05 e DAT-07**. O texto anterior descreve a entrega original da análise; este registo documenta a implementação posteriormente autorizada.

Resultado automático: **2 276 testes JVM + 6 testes da ferramenta de snapshot aprovados**, compilação release e `lintVitalStandardRelease` aprovados. Snapshot do Core atualizado e conferido integralmente com as fontes. Base de dados mantém versão 51; backup passa a versão 26 com leitura dos formatos anteriores preservada.

Detalhes, compatibilidade, cópia de recuperação e testes pendentes na box: [IMPLEMENTACAO_FASE1_BACKUPS_2026-10-20.md](../docs/IMPLEMENTACAO_FASE1_BACKUPS_2026-10-20.md).

**Não foi gerado APK nem iniciada a fase 2.** A aprovação automática não substitui testar exportação/restauro e comando numa box real.

## 16. Ponto de retoma e implementações pendentes — 22/10/2026

**Registo histórico da interrupção, pedido pelo utilizador para continuar mais tarde. O fecho posterior da fase 2 está na secção 17 e prevalece sobre as pendências desta secção.** A frase «não iniciada a fase 2» na secção 15 refere-se ao fim da fase 1; no momento desta interrupção a fase 2 tinha sido iniciada, mas ainda não estava fechada.

### 16.1. Estado real de cada fase

| Fase | Estado na interrupção | Próximo passo |
|---|---|---|
| 1 — Backups e restauro | Implementada e validada automaticamente; ver secção 15. Teste físico na box pendente. | Preservar as alterações e os testes existentes. |
| 2 — EPG e catálogos | Código alterado no Core externo e na App; validação parcial do Core aprovada. **Integração final da App ainda pendente.** | Terminar a revisão, atualizar o snapshot e validar o conjunto antes de declarar concluída. |
| 3 — Pesquisa, índices e cache EPG | Por iniciar. | DAT-05 e DAT-08, após fechar a fase 2. |
| 4 — Reprodução e mudança de canal | Por iniciar neste plano. | PLY-01/02/03/04/05/06/08/09, UI-09 e OPT-04. |
| 5 — Comando, definições e continuidade | Por iniciar. | UI-01/02/03/04/07/08/10 e PLY-07. |
| 6 — Serviços, distribuição e limpeza | Por iniciar. | INT-06/07/08/09 e OPT-01/02. |
| 7 — Medição e funcionalidades opcionais | Não autorizada como implementação automática. | Avaliar MED-01–15 e escolher NOV-01–06 apenas quando se justificar. |

As fases 3–6 contêm **26 blocos aprovados ainda por iniciar**. Este número não inclui os oito blocos da fase 2 em curso, a matriz de preparação OPT-03, as investigações condicionais ou as funcionalidades opcionais.

### 16.2. Fechar primeiro a fase 2 — não começar de novo

Âmbito: **DAT-01, DAT-02, DAT-03, DAT-04, DAT-06, DAT-09, UI-05 e UI-06**. As definições e os critérios completos continuam nas secções 3 e 5 deste documento.

Já existem alterações no código para:

- Exigir XMLTV completo na atualização principal, limitar a acumulação de texto, reconhecer gzip com leituras curtas e conservar o ficheiro de guia anterior em atualizações incompletas ou vazias.
- Substituir o ficheiro de cache por rename, sem apagar primeiro o anterior; serializar atualizações da mesma origem e verificar se o pedido ainda pertence à configuração atual.
- Distinguir confirmação, rejeição e indisponibilidade na deteção de Xtream; uma falha temporária não converte automaticamente a origem para M3U.
- Construir URLs Xtream com tratamento separado de path e query, incluindo credenciais, IDs, HLS de teste e diferidos; reduzir a exposição de dados nas mensagens de diagnóstico.
- Descodificar títulos Base64 de forma conservadora, respeitar BOM/UTF-8 válido e recusar listas sem entradas utilizáveis ou incompletas antes da poda final.
- Guardar associações EPG em lote, com verificação do perfil/sessão e comparação com as escolhas manuais atuais; agrupar eliminações da cache dentro da transação existente.
- Respeitar Retry-After de catálogo/EPG por identidade de conta, sem aplicar essa política ao cliente de reprodução.
- Identificar testes de listas por ID/revisão; cancelar e aguardar sincronizações antigas ao editar; preencher Retry a partir do rascunho em memória; recuperar a tentativa do assistente através de IDs, sem guardar credenciais no estado do Android.
- Não atualizar a data de sucesso do EPG quando a tentativa falha, nem gravar o resultado de um pedido antigo sobre uma origem entretanto editada.

**Evidência disponível, sem extrapolar:** a execução de 20/10/2026 em `%TEMP%/owntv-phase2-core-final.log` terminou com `BUILD SUCCESSFUL`, para `:core:testDebugUnitTest` e `:core:compileDebugAndroidTestKotlin`. Os relatórios JVM consultados contêm **1 316 testes, zero falhas, zero erros e zero ignorados**. Compilar testes Android não significa executá-los numa box. Estes resultados não substituem a validação final conjunta App/Core, que ainda falta.

**Pendente antes de fechar a fase 2:**

1. Rever o diff completo contra a cópia anterior à fase 2, preservando as alterações preexistentes e as da fase 1. Não usar o diff global do Core como se todas as modificações fossem desta fase.
2. Rever especialmente a recuperação de uma fonte parcialmente importada em `SourceRepository.addAndLink`: o ramo atual usa `source.copy(id = old.id, createdAt = old.createdAt)`. Confirmar/corrigir para conservar os ajustes anteriores da fonte que não pertencem ao formulário, em vez de repor os valores por defeito ao retomar.
3. Rever a consistência concorrente de `CatalogCooldowns`: prazo e código HTTP estão em dois mapas. Avaliar uma única entrada atómica por conta e o âmbito de identidade quando existirem cabeçalhos de autenticação. O intervalo atual é em memória; não prometer conservação após morte do processo.
4. Confirmar os percursos de recuperação do assistente: perfil/fonte ainda existentes, dados parcialmente persistidos, Retry, cancelamento, segunda importação e morte do processo. Não apagar perfis ou fontes indiscriminadamente. As origens EPG independentes não devem ser reescritas automaticamente só porque as credenciais de uma lista mudaram.
5. Conferir os novos textos EN/PT/PT-PT e os inventários de literais técnicos em ambos os repositórios, sem desativar verificações nem aumentar a dívida de tradução.
6. Repetir os testes necessários após os últimos ajustes; compilar a App em debug/release e executar os seus testes e `lintVitalStandardRelease`. Não existe ainda um resultado final conjunto da fase 2 a anunciar.
7. **Regenerar o ZIP/lock do Core de forma coerente, depois da validação necessária.** O lock atual ainda aponta ao snapshot da fase 1: `859f1d6cc93266d4fc09dd3442b73f3cd28a0541b150a6a6f8cfb2678fcd928c`, 1 012 ficheiros. Na conferência desta retoma, 20 inputs já existentes nesse ZIP diferem da origem externa; além disso, há ficheiros novos. O snapshot atual não inclui a fase 2. Não declarar que um APK gerado assim contém estas alterações; o override `owntv.corePath` também exige igualdade com o snapshot.
8. Executar os seis testes da ferramenta de snapshot, comparar ZIP/fontes byte a byte e conferir a configuração normal sem override. Guardar um registo final da fase 2 e atualizar esta secção só depois dos resultados reais.
9. Posteriormente, testar numa box: atualização XMLTV cortada seguida de atualização válida, lista vazia/HTML, duas listas com o mesmo nome, alteração de senha, origem com subpath/caracteres reservados, Retry-After e manutenção das associações EPG. Verificar também que a correção de diferidos +1 h e as configurações por canal se mantêm.

**Recuperação do código antes desta fase:** `C:/Users/renat/Downloads/OwnTV-fase2-backup-20261020/`, com ZIP/lock da fase 1, `app-before.zip` e `sha256.json`, verificados antes das alterações. É uma cópia do código no PC, não dos dados instalados na box.

**Ficheiros de trabalho temporários:** `%TEMP%/owntv_phase2_*.py`, `%TEMP%/owntv-phase2-core.log` e `%TEMP%/owntv-phase2-core-final.log`. Servem de apoio histórico; na retoma, ler o código atual e este plano. Não voltar a executar cegamente os scripts de substituição, pois as alterações já foram aplicadas.

### 16.3. Restantes implementações aprovadas, pela ordem prevista

**Fase 3 — Pesquisa e armazenamento do guia**

- **DAT-05:** conferir e recuperar os triggers FTS e os índices removidos durante operações em lote, mesmo quando existe uma falha a meio. Testar com SQLite real; não apagar a base de dados.
- **DAT-08:** limitar as caches de EPG em memória, mantendo a identificação por origem/perfil/offset/revisão e sem adulterar os programas devolvidos.

**Fase 4 — Reprodução, diferidos e mudança de canal**

- **PLY-01:** confirmar a libertação da sessão anterior também ao abrir diferidos internamente.
- **PLY-02:** distinguir recusa HTTP, formato, rede e falha do motor antes de tentar alternativas ou aprender preferências.
- **PLY-03:** manter Play/Pausa coerente com a intenção do utilizador durante buffering e transições.
- **PLY-04:** garantir um único responsável por MediaSession e foco de áudio, incluindo multiview.
- **PLY-05:** não reciclar uma imagem congelada enquanto ainda está a ser apresentada.
- **PLY-06:** tornar a recuperação orientada a eventos, cancelável e respeitadora da pausa.
- **PLY-08 e UI-09:** manter continuidade e ações de gravação seguras, com erros e mensagens corretos; são referências relacionadas, não contagens duplicadas de bugs.
- **PLY-09:** corrigir a correspondência entre canal selecionado, OSD e foco no multiview.
- **OPT-04:** explicitar os contratos de intenção de reprodução e corrigir nomes/comentários incoerentes, conservando o volume interno a 100%.

**Fase 5 — Comando e definições**

- **UI-01:** DNS coerente, validado e sem apagar escolhas saudáveis.
- **UI-02:** validação comum dos campos de proxy e rede.
- **UI-03:** recuperação de foco em diálogos e resultados de testes das definições.
- **UI-04:** foco estável em listas com cabeçalhos, inserções e remoções.
- **UI-07:** continuidade do ecrã inicial e rejeição de resultados antigos.
- **UI-08:** consumir cada deep link uma única vez.
- **UI-10:** acessibilidade e apresentação fiel dos estados reais.
- **PLY-07:** permitir CH+/CH− para além da página de canais atualmente visível, conservando o contexto.

**Fase 6 — Serviços, distribuição e limpeza**

- **INT-06:** autoridade central de limite de tentativas de PIN, preservando compatibilidade.
- **INT-07:** revogação, limites de dados e limpeza de temporários no sincronismo local já existente, sem reintroduzir menus retirados.
- **INT-08:** coerência entre versão e assinatura na publicação; nunca fallback silencioso para assinatura debug.
- **INT-09:** integridade dos inputs de build e do CI.
- **OPT-01:** retirar formatação e consultas informativas de espaço em disco do caminho de progresso frequente, preservando a quota antes da escrita.
- **OPT-02:** remover apenas código, estado, imports, comentários e recursos comprovadamente sem consumidores; manter contratos de backup e dados legados necessários.

**Preparação transversal ainda a conferir — OPT-03:** completar a matriz `opção → UI/atalho → setter → persistência → aplicação efetiva no motor`. Não considerar esta matriz automaticamente concluída só por terem passado os testes de backups ou do Core.

### 16.4. Investigações e novidades que continuam condicionais

- **MED-01–15:** mantêm-se como investigação/medição, com os limites da secção 8. Não converter propostas em alterações automáticas nem prometer ganhos percentuais sem medir.
- **NOV-01–06:** diagnóstico guiado, preview alargado de restauro, temporizador de sono, informação de disponibilidade de diferidos/gravações, desfazer reversível e alternativas do mesmo programa. Continuam opcionais; a escolha deve anteceder a implementação.
- **UPSCALING-BETA-01:** continua apenas registado em `docs/MELHORIAS_FUTURAS_PENDENTES.md`; não implementar ou ativar por arrasto desta sequência.
- As propostas recusadas e os comportamentos já presentes continuam excluídos, conforme as secções anteriores. Não reintroduzir funcionalidades de smartphone, filmes/séries, OpenSubtitles, meteorologia ou animações retiradas por decisão do utilizador.

### 16.5. Instrução prática para a próxima retoma

Começar por **«fechar a fase 2»**, seguindo a lista 16.2 e inspecionando o estado atual dos dois repositórios. Não reiniciar a fase 1 nem avançar diretamente para a fase 3. Não gerar APK, assinar, fazer commit, push ou publicar sem pedido próprio. Preservar a base de dados personalizada, HLS/exceções manuais, tempos de reabertura, configurações por canal, volume interno a 100% e correção de diferidos +1 h.

Esta atualização de 22/10/2026 guardou o ponto de retoma e as pendências. **Não alterou código nem concluiu validações que estavam em falta.**

## 17. Fecho posterior da fase 2 — 22/10/2026

**A fase 2 foi integrada e validada automaticamente. Esta secção substitui o estado de interrupção da secção 16.** Foram fechados os pontos da recuperação de fontes e da consistência de Retry-After, mantendo as alterações anteriores.

- Core: 1 320 testes JVM aprovados e testes Android compilados; App: compilação debug/release, 426 testes JVM e lint vital de release aprovados. Seis testes da ferramenta de snapshot aprovados.
- Snapshot isolado desta fase: 1 025 inputs, SHA-256 `71e13bdf202335a94bc9d3b103a31b2f66144b2252a5d04cd1fc52b9c3aef648`. Alterações posteriores autorizadas podem substituir o lock; conferir sempre o atual.
- Registo completo e limites: [IMPLEMENTACAO_FASE2_CATALOGOS_2026-10-22.md](../docs/IMPLEMENTACAO_FASE2_CATALOGOS_2026-10-22.md).
- Testes numa box continuam pendentes. Sem APK, assinatura, commit, push ou publicação.
- As fases 3–6 deste plano não foram iniciadas como conjunto. Mantêm-se as 26 propostas e os critérios da secção 16.3. Novidades opcionais, medições e upscaling permanecem condicionais.
- Foi autorizada separadamente a primeira etapa das novas publicações OwnTV 5.2.1/Core 1.0.69/libmpv 2026.10.4: R07/R08/R01/R02, conforme o relatório em `docs/ANALISE_UPSTREAM_5.2.1_CORE_1.0.69_LIBMPV_2026.10.4_2026-10-22.md`. Essa etapa não renumera nem dá como implementadas as fases restantes deste plano.

**Fecho dessa etapa separada:** implementada e validada automaticamente, com 2 312 testes JVM + 6 testes de snapshot, compilação debug/release, lint e bibliotecas nativas aprovados. O lock atual inclui a fase 2 e a etapa A: SHA-256 `15a5cbe97bcf5682a8ed24609c1e8ac31b0eb08e07203337fc87afed1ccc425b`, 1 030 inputs. Ver [IMPLEMENTACAO_UPSTREAM_ETAPA_A_2026-10-22.md](../docs/IMPLEMENTACAO_UPSTREAM_ETAPA_A_2026-10-22.md). Sem APK ou validação física. A próxima etapa dessas publicações é B (R03/R04/R05/R06), ainda pendente; as fases 3–6 deste plano principal mantêm o âmbito anterior.


## 18. Etapa B das novas publicações — 22/10/2026

A autorização seguinte refere-se à etapa **B (R03/R04/R05/R06)** da análise OwnTV 5.2.1/Core 1.0.69/libmpv 2026.10.4, não à fase 3 deste plano.

Implementação concluída e validada automaticamente: **2 331 testes JVM + 6 testes de snapshot aprovados**, compilação debug/release, lint vital de release e bibliotecas nativas aprovados. Configuração normal de build confirmada. Registo e limites em [IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md](../docs/IMPLEMENTACAO_UPSTREAM_ETAPA_B_2026-10-22.md). O fecho da superfície conserva a espera exigida pelo Android; não se declara integralmente assíncrono nem garante ausência de bloqueio nativo.

Snapshot desta etapa: 1 035 inputs, SHA-256 `350b830685de9e7e2b1453cc4a5f3a48f41a1bd8db03659e52a885e735463fc0`, conferido com o Core externo. Core: 1 329 testes; motores: 573 testes; App: 429 testes na verificação final. Sem APK ou testes na box.

**Próxima etapa destas publicações: C — R09/R10** (diagnóstico eficiente e retoma de posição). Não iniciada. C01/C02/C03 continuam condicionais/opcionais. As fases 3–6 do plano principal continuam com o âmbito anterior; não foram automaticamente executadas nesta integração.

## 19. Etapa C das novas publicações — 22/10/2026

Foi autorizada a etapa **C — R09/R10**. Implementação concluída e validada automaticamente: **1 330 testes Core + 588 testes dos motores + 429 testes App = 2 347 testes JVM**, mais seis testes da ferramenta de snapshot aprovados. Compilação standard debug/release, lint vital de release, bibliotecas nativas e configuração normal de build aprovados. Snapshot com 1 039 inputs, SHA-256 `6eabcd44c622925b2f505a10559a4320606e08113ac57728d941f7feeaa3c1f4`, conferido com os fontes. Ver [IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md](../docs/IMPLEMENTACAO_UPSTREAM_ETAPA_C_2026-10-22.md).

Inclui deduplicação limitada de avisos mpv, leitura periódica dos contadores apenas com diagnóstico detalhado ativo (uma leitura pendente no máximo), expressões de ocultação compiladas uma vez e retoma inicial precisa com alternativa por geração. Não altera a correção +1 h do EPG, buffers, HLS, motores ou volume. Backup anterior em `C:/Users/renat/Downloads/OwnTV-upstream-C-backup-20261022/`.

A sequência recomendada A–C destas publicações ficou implementada e validada automaticamente. Aceitação física na box pendente. **Etapa D / C01/C02/C03** mantém-se opcional/condicional, sem implementação automática. As fases 3–6 do plano principal conservam o âmbito anterior. Sem APK ou aceitação física nesta intervenção.


## 20. Etapa D das novas publicações — 22/10/2026

C01/C02/C03 autorizadas e implementadas: correção de sincronismo por saída com backup e zero por defeito; preferência SDR apenas para HLS elegível no ExoPlayer, desligada por defeito; validade do timeshift estacionado após suspensão, preservação da idade original e retoma expirada com alternativa segura ao direto. Gravações, quotas, motores, HLS obrigatório, buffers, reabertura e volume interno de 100% preservados.

Validação final: **2 362 testes JVM + 6 testes de snapshot aprovados**, compilação debug/release, lint vital de release, preparação nativa e configuração normal aprovados. Snapshot **1 047 inputs**, SHA-256 `eb701f07d10fe9fd843faba4ed0b474b0100d6b8570480fb3449ab08a6ea4928`. Sem APK e sem aceitação física na box.

[Registo da etapa D](../docs/IMPLEMENTACAO_UPSTREAM_ETAPA_D_2026-10-22.md). A–D desta análise ficam implementadas no âmbito descrito; as fases 3–6 do plano principal continuam com o estado anterior. Este registo não autoriza fases seguintes.


## 21. Fecho da fase 3 do plano principal — 22/10/2026

O utilizador autorizou continuar por fases, começando pela primeira pendente: **fase 3 — DAT-05 e DAT-08**. Implementada e validada automaticamente. Esta secção prevalece sobre as indicações históricas de fase 3 por iniciar nas secções 16–20; as etapas A–D mantêm-se concluídas no seu âmbito.

- **DAT-05:** contrato dos 16 triggers conferido com o esquema Room 51; deteção de ausência/definição incorreta; reparação com savepoint e reconstrução dirigida. DDL de bulk em transações, estado de recuperação preservado se houver falha, recuperação pelo último escritor e índices desconhecidos conservados. Diagnóstico não rotula reparação falhada como base saudável. Sem novo PRAGMA global: o Room já configura recursive_triggers nos writers.
- **DAT-08:** caches de agora/seguinte e linhas do fornecedor com limites por canais e peso de texto, eviction sincronizada, identidade/política efetiva e invalidação por revisão preservadas. Respostas grandes são devolvidas completas sem retenção; não se cortam programas nem se cancelam pedidos partilhados por eviction.
- **Validação:** 1 349 testes Core + 598 motores + 429 App = **2 376 testes JVM**, mais seis testes da ferramenta de snapshot aprovados. Inclui 14 testes JVM novos, com SQLite real e concorrência de cache. Quatro testes Android novos compilados, não executados na box. Compilação debug/release, lint vital de release, i18n e configuração normal aprovados.
- **Snapshot:** 1 052 inputs; SHA-256 `eb335c31686c9e9ad116c2adf91dab4d721cc5e5c117ea5af8b93334118099d9`, conferido integralmente com o Core externo. Cópia anterior com manifesto verificado: `C:/Users/renat/Downloads/OwnTV-fase3-backup-20261022/`.
- **Preservado:** esquema 51, backups e dados legados, motores/HLS, configurações por canal, buffers, reabertura, gravações, correção de diferidos +1 h, volume interno de 100% e âmbito exclusivo TV. Sem APK, assinatura, commit, push ou publicação.

Detalhes, provas e aceitação física pendente: [IMPLEMENTACAO_FASE3_PESQUISA_CACHE_EPG_2026-10-22.md](../docs/IMPLEMENTACAO_FASE3_PESQUISA_CACHE_EPG_2026-10-22.md).

**Próxima implementação:** fase 4 — PLY-01/02/03/04/05/06/08/09, UI-09 e OPT-04, confrontando cada proposta com as proteções já integradas nas etapas A–D. Fases 4–6 conservam os **24 blocos do plano** ainda por executar como conjunto, sujeitos a deduplicação na implementação; isso não equivale a 24 bugs reproduzidos. Preparação transversal OPT-03, MED-01–15, NOV-01–06 e UPSCALING-BETA-01 mantêm os critérios anteriores. Nenhuma fase seguinte foi iniciada nesta intervenção.

## 22. Fecho da fase 4 do plano principal — 22/10/2026

O utilizador autorizou a **fase 4 — reprodução, diferidos e mudança de canal**. Implementada e validada automaticamente no âmbito **PLY-01/02/03/04/05/06/08/09, UI-09 e OPT-04**. Esta secção prevalece sobre as indicações históricas de fase 4 pendente, incluindo a próxima implementação da secção 21. As etapas A–D não são renumeradas.

- **Diferidos:** confirmação conjunta de libertação local antes de abrir programa/recuo; cancelamento não abandona a limpeza; origem/escolha/geração revalidadas e Pausa durante a transição preservada. Não se confunde confirmação local com fecho remoto pelo fornecedor.
- **Falhas e alternativas:** HTTP, transporte, timeout, handover, cancelamento, formato, codec e desconhecido distinguidos. Aprendizagem temporária de ausência de HLS só após erro de formato e TS confirmado no mesmo motor, limitada por URL/motor, capacidade e TTL de 30 minutos. HLS obrigatório e exceções manuais mantidos.
- **Intenção e recuperação:** ícones por intenção, contratos obrigatórios nos motores, resultados por escolha/origem/canal, observador filho da tentativa e cancelamento propagado. Pausa não consome o prazo de abertura nem desencadeia alternativas. A verificação ativa de metadados/prazos continua limitada a 50 ms; esperas de outcome/pausa usam eventos.
- **Sessão e grelha:** proprietário único de MediaSession/foco de áudio, incluindo quadrado audível; limpeza antiga não desliga novo proprietário. Suspensão de sessão fora do primeiro plano e foco estável ao alterar quadrados. Proteções anteriores de fecho da grelha conservadas.
- **Imagem, gravações e OSD:** bitmaps publicados deixam de ser reciclados antes do fim do desenho; ações de gravação têm resultados/mensagens coerentes e perfil conferido. Apagar ficheiro em reprodução exige libertação confirmada; continuação de diferidos trata falhas sem substituição silenciosa. O aviso de zapping acompanha o canal escolhido, não metadados antigos.
- **Validação:** **1 355 testes Core + 610 motores + 441 App = 2 406 testes JVM**, zero falhas/erros/ignorados; **30 testes novos**, mais seis testes da ferramenta de snapshot aprovados. Compilação debug/release, lint vital de release, i18n e configuração normal aprovados. Testes Android compilados, não executados na box. Baselines i18n não aumentados.
- **Integridade:** snapshot com **1 058 inputs**, SHA-256 `12015db51124dafae2de1cff483eb7147d1a15cc780858b7b254c1fa2da2d942`, conferido integralmente com o Core externo. Cópia anterior e manifesto verificados em `C:/Users/renat/Downloads/OwnTV-fase4-backup-20261022/`.
- **Preservado:** esquema 51, backups, configurações por canal, buffers/latência, motores/HLS/exceções, intervalo de reabertura, correção de diferidos +1 h independente da hora apresentada, gravações, volume interno de 100% e âmbito TV. OwnTV Mobile não alterado. Sem APK, assinatura, commit, push ou publicação.

Detalhes, limites da validação e ensaios físicos pendentes: [IMPLEMENTACAO_FASE4_PLAYBACK_DIFERIDOS_2026-10-22.md](../docs/IMPLEMENTACAO_FASE4_PLAYBACK_DIFERIDOS_2026-10-22.md). Não há medição que permita atribuir ganhos percentuais nem garantia de eliminar recusas do fornecedor ao reabrir canais.

**Próxima fase, ainda não iniciada: fase 5 — comando e definições**, UI-01/02/03/04/07/08/10 e PLY-07. A fase 6 mantém INT-06/07/08/09 e OPT-01/02. Restam **14 blocos** destas duas fases; não são 14 bugs reproduzidos. OPT-03, MED-01–15, NOV-01–06 e UPSCALING-BETA-01 conservam os critérios condicionais anteriores. Este fecho não inicia nem autoriza automaticamente a fase seguinte.

## 23. Fecho da fase 5 do plano principal — 22/10/2026

Autorizada e implementada a **fase 5 — comando, definições e continuidade**, âmbito **UI-01/02/03/04/07/08/10 e PLY-07**. Validação automática concluída. Esta secção prevalece sobre as indicações históricas de fase 5 pendente, incluindo a próxima fase da secção 22; as etapas A–D mantêm o seu âmbito.

- **DNS/proxy:** interpretação comum em Guardar/Testar, IPv6/portas válidas, DoH HTTPS e conservação da configuração ao desativar. Presets preparados sem gravação imediata e alterações por guardar visíveis. Endereços de fontes/EPG verificados sem proibir HTTP IPTV, portas privadas ou ficheiros locais/content. Duplicados avisados, sem fusão ou apagamento automático.
- **Comando/definições:** foco ao atingir extremos numéricos; ações específicas prevalecem na pesquisa e conservam destino de regresso; tentativas limitadas, fallback e invalidação por input. Gravações/listas agrupadas usam identidade e posição visual distintas dos cabeçalhos; tratam vizinho, vazio e mudanças de perfil sem pedidos antigos a roubar o cursor. Sem reintroduzir menus retirados.
- **Início e atalhos:** cartão conservado por chave, sem scroll incondicional no refresh ou corte do preview apenas por deixar de estar no primeiro cartão. «No momento» atualiza enquanto visível/em primeiro plano e pede guia ao expirar a janela. Resultados de perfil antigo recusados, preservando o contrato de entrada com perfil validado. Deep links consumidos sobrevivem a recriação; novos Intents para o mesmo canal têm revisão própria.
- **Estados honestos/acessibilidade:** bitrate desconhecido «—», transferência medida a zero distinta, códigos de idioma desconhecidos sem rótulo Automático; seleção/papel acessível também nos percursos de toque médio/longo. Verificação visual em 720p e com leitor de ecrã pendente.
- **Zapping paginado:** passos dentro da janela continuam em memória; ao atravessar páginas, preservam contexto, filtro, ordem, ocultações e grupos. Índice de IDs, leitura por páginas de 256 fora da thread principal e janela máxima de 17 entidades; origem/geração/âncora conferidas. Sem abrir streams vizinhos nem duplicar todo o catálogo de entidades. A cache agrupada preexistente não foi redesenhada; medir primeira indexação na box.
- **Validação:** **1 360 testes Core + 610 motores + 452 App = 2 422 testes JVM**, zero falhas/erros/ignorados; **16 testes novos** e seis testes da ferramenta de snapshot aprovados. Compilação debug/release, lint vital, i18n e configuração normal aprovados. Testes Android Core compilados, não executados na box. Baseline i18n Core conservado; App reduzido de 51 para 47 entradas ao retirar formatação antiga de DNS.
- **Integridade:** snapshot com **1 061 inputs**, SHA-256 `1dcc86280d668242d65d379f3b845b357a73901464e4674b71b941212afb5011`, conferido por hash, inventário e bytes com o Core externo. Cópia anterior e manifesto verificados: `C:/Users/renat/Downloads/OwnTV-fase5-backup-20261022/`.
- **Preservado:** esquema 51, backups, gravações, motores/HLS/exceções, buffers/latência, intervalo de reabertura, configurações por canal, correção dos diferidos +1 h, volume interno de 100% e âmbito TV. OwnTV Mobile não alterado. Sem APK, assinatura, commit, push ou publicação. Sem ganhos percentuais medidos.

Provas, decisões de implementação e testes físicos pendentes: [IMPLEMENTACAO_FASE5_COMANDO_DEFINICOES_2026-10-22.md](../docs/IMPLEMENTACAO_FASE5_COMANDO_DEFINICOES_2026-10-22.md).

**Próxima fase, ainda não iniciada: fase 6 — serviços, distribuição e limpeza**, INT-06/07/08/09 e OPT-01/02: **seis blocos**, não seis bugs reproduzidos. OPT-03, MED-01–15, NOV-01–06 e UPSCALING-BETA-01 conservam os critérios condicionais anteriores. Este fecho não inicia nem autoriza automaticamente a fase seguinte.

## 24. Reanálise crítica atual e decisão do council — 23/10/2026

Foi pedido reavaliar este plano com agentes especializados, sem implementar. Foram confrontados código atual, testes existentes e os fechos das fases por três agentes — dados/EPG/backup, reprodução/HLS e segurança/distribuição/limpeza — com revisão de interface pelo coordenador e contraditório dos achados. **O histórico de conclusão das fases 1–5 não significa que todos os casos limite estejam resolvidos.**

**Referência atual para decidir trabalho:** [REANALISE_COUNCIL_PLANO_2026-10-23.md](REANALISE_COUNCIL_PLANO_2026-10-23.md). Esta secção e a reanálise prevalecem sobre linhas antigas ou listas históricas de pendências.

- **Base conferida agora:** ZIP/lock e os 1 061 inputs do Core externo coincidem em inventário e bytes; SHA-256 `1dcc86280d668242d65d379f3b845b357a73901464e4674b71b941212afb5011`, esquema 51. Não houve nova execução de testes/builds, APK ou ensaio físico; os 2 422 testes da secção 23 são evidência histórica de 22/10.
- **41 IDs principais:** 28 implementados no âmbito inspecionado; seis com lacuna/complemento residual; seis blocos da fase 6 a completar; OPT-03 transversal por completar. Não são contagens de bugs reproduzidos.
- **Resíduos identificados:** DAT-03, query PHP de diferidos ainda interpola credenciais; INT-02, omissões de recursos de legenda/seleção Off sem resumo inequívoco; PLY-01, percurso adjacente Exo→timeshift local não espera toda a libertação antes de iniciar produtor; PLY-02, aprendizagem temporária sem invalidação explícita por revisão de fonte/headers; PLY-07, índice de zapping não acompanha alteração do catálogo; UI-07, seleção de «No momento» pode ficar fora da janela visível após reordenação. A reanálise delimita condições, evidências e testes futuros de cada um.
- **Fase 6 aprovada com ajustes:** INT-06/07/08/09 e OPT-01/02 continuam úteis; não reaplicar proteções existentes. LocalSync já tem autenticação/modos/GET streaming; CI já testa Core/snapshot; gravações já amostram e consultas de espaço já usam IO. Corrigir as lacunas remanescentes.
- **Correção importante de OPT-02:** os pickers de canal de arranque e cor funcionam atualmente; **não remover** com base nas linhas antigas. O comentário de GuideLoadGate já foi corrigido. Remover apenas consumidores mortos confirmados, sem eliminar contratos Core ou dados legados.
- **Correção importante de INT-07:** rejeitar framing inválido/não suportado e limitar respostas do cliente; não ler corpos indefinidamente até EOF para prometer suporte geral a pedidos sem Content-Length. Emparelhamento por ID exige PIN válido; não descrever a lacuna como substituição sem autenticação.
- **MED-01–15/NOV-01–06:** continuam condicionais/opcionais, várias com infraestrutura parcial. Sem novos índices, alterações globais de buffer/flags/retries, redução de fsync ou novas funcionalidades sem requisito/evidência. UPSCALING-BETA-01 permanece investigação futura.

**Ordem futura recomendada:** resíduos funcionais delimitados; integridade residual de backup/invalidação; distribuição INT-08/09 antes de publicar; PIN/LocalSync; otimização medida e limpeza. OPT-03 acompanha os lotes desde o primeiro. Resumo e critérios completos no relatório. Esta reanálise altera apenas documentação e **não inicia implementação nem autoriza automaticamente qualquer lote**.

## 25. Primeira subfase da reanálise — A1 / R1 / DAT-03 — 23–24/10/2026

Autorizada a implementação por subfases, começando pela primeira. Delimitada a **A1 — conversão de URLs de diferidos com credenciais especiais**. Corrigidos o fallback de arquivo para PHP e o modo M3U xc/xtream, usando valores lógicos dos segmentos e parâmetros de query seguros. Tokens existentes preservados, incluindo repetições e percent-encoding; grafia dos horários conservada, sem alteração da correção +1 h ou das opções de formato/HLS.

- **Core:** 1 367 testes JVM aprovados, zero falhas/erros/ignorados, incluindo sete testes novos. Antes do fix, seis dos sete novos testes reproduziram falhas. Nenhum teste existente foi enfraquecido. i18n aprovado sem aumento do baseline; seis testes da ferramenta de snapshot aprovados.
- **App:** compilação debug/release e lint vital aprovados a 23/10 e novamente a 24/10, em 3 min 34 s, com a versão atual `105.1.0.6`, alterada fora desta subfase. **A1 concluída e validada automaticamente.**
- **Integridade:** novo snapshot com 1 062 inputs, SHA-256 `d6c9d4b67ff4b791f10d91926e1d6694ba986a7b77a368937166543bdc7df4b5`, conferido com os fontes externos. Cópia anterior com manifesto em `C:/Users/renat/Downloads/OwnTV-subfase-A1-backup-20261023-163539/`; snapshot anterior conservado.
- **Âmbito preservado:** esquema 51, HLS/exceções, formatos/fallback, áudio, buffers/latência, reabertura, configurações por canal, gravações, dados instalados e volume de 100%. OwnTV Mobile não alterado. Sem APK, assinatura, commit, push ou publicação; aceitação física pendente.

Registo: [IMPLEMENTACAO_SUBFASE_A1_URLS_DIFERIDOS_2026-10-23.md](../docs/IMPLEMENTACAO_SUBFASE_A1_URLS_DIFERIDOS_2026-10-23.md).

**Subfases seguintes, ainda não iniciadas:** A2 — R4 / PLY-07, índice de zapping após atualização do catálogo; A3 — R3 / PLY-01, confirmação Exo→timeshift local; A4 — R5 / UI-07, seleção visível em «No momento». O lote B mantém R2 / INT-02 e R6 / PLY-02. Fase 6, OPT-03, MED/NOV e UPSCALING mantêm as condições da reanálise. Este registo não inicia nem autoriza automaticamente o trabalho seguinte.


## 26. Fecho das restantes subfases autorizadas — 24/10/2026

Foram implementados **A2/R4, A3/R3, A4/R5, B1/R2 e B2/R6**, e completados os blocos recomendados **INT-06/07/08/09 e OPT-01/02**, com a matriz transversal OPT-03. Não reaplicar estes blocos a partir das listas históricas.

- Zapping: índice paginado invalidado por catálogo/ordenação, revisão no cache e recusa de resultados antigos; janela limitada e direção preservada quando desaparece o canal.
- Reprodução: confirmação conjunta Exo/mpv antes do produtor de timeshift local; lições temporárias de formato invalidadas por configurações/headers novos.
- Home: seleção por identidade e janela visível reconciliadas ao mudar a lista.
- Backup: export completo não omite silenciosamente recursos selecionados; seleções Off/timings sem cache preservados; resumo de itens de legendas não aplicados.
- PIN/LocalSync: autoridade comum/persistida, hashes versionados compatíveis com leitura legada, proteção Kids condicionada a adulto protegido, revogação ativa, identidades não substituídas silenciosamente, limites de bytes reais, cancelamento e temporários com owner/leases/TTL.
- Distribuição: versão única, progressão conferida contra APK publicado, assinatura autorizada/package/ABIs/hashes, permissões por job, Actions fixadas, wrappers/dependências verificadas e matriz CI Android 26/34.
- Custo/limpeza: amostragem de armazenamento informativo, formatters por locale/thread e resíduos sem consumidores confirmados; quotas, lembretes, canal de arranque e cores preservados.

**Evidência:** 460 testes App + 1 375 Core + 613 motores = **2 448 JVM**, sem falhas; **15 testes Python**; compilação App debug/release e lint debug/vital release aprovados com verificação estrita de dependências. Os testes instrumentados foram compilados, não executados. Snapshot de 1 071 inputs, SHA-256 `ff76713c2d027257ff49b416d7bb13c56a5968045319b88252577e4ab796014a`, conferido contra os bytes atuais do Core.

Não foi gerado/publicado APK. CI remoto, foco com comando, codecs/HDMI, fornecedor e LocalSync entre dispositivos continuam a exigir aceitação real. Não há percentagens de ganho medidas. Consultar o [relatório final](../docs/IMPLEMENTACAO_SUBFASES_RESTANTES_2026-10-24.md) para limitações de PIN em versões antigas e configuração de assinatura para publicação futura.

**Futuro, sem implementação nesta intervenção:** MED/NOV, UPSCALING-BETA-01 e **TV-AVO-01 — modo «TV Avó»**. Este último deixa visíveis apenas play/pausa, nome do canal e programação, ocultando os restantes controlos do leitor e preservando CH+/CH− e saída. Está registado em [melhorias futuras](../docs/MELHORIAS_FUTURAS_PENDENTES.md); não ficou ativo por este registo.


## 27. TV-AVO-01 implementado por autorização específica — 24/10/2026

A proposta registada no fecho anterior foi autorizada e implementada como opção do leitor interno, desligada por defeito: play/pausa, canal e programação, sem ferramentas avançadas. Setas/CH mantêm o zapping; Voltar regressa à lista, de onde se pode desativar o modo nas definições. A escolha persiste e integra o backup completo, sem alterar motores, buffers, Core ou dados instalados.

**Validação desta implementação:** 466 testes App (seis novos), compilação debug/release, lint debug/vital release sem erros, inventário i18n e igualdade do Core com o snapshot. Sem APK ou teste em box. Detalhes e aceitação física pendente no [relatório TV Avó](../docs/IMPLEMENTACAO_TV_AVO_2026-10-24.md). O registo antigo de TV-AVO-01 como ideia futura é histórico; não reaplicar a implementação. MED/NOV e UPSCALING conservam as suas condições.


## 28. Complemento TV Avó — sem play/pausa e com versões automáticas

Por pedido posterior de 24/10/2026, foi removido o botão de play/pausa. OK apenas revela nome/programação. Ativar o modo liga agrupamento das versões, prioridade de reprodução e recuperação automática entre qualidades; aplica também no arranque/perfil, preservando ordem manual, versões ocultas e tempo de espera. Desativar o modo não reverte essas opções.

470 testes App, compilação debug/release e lint sem erros. Core/motores/snapshot sem alterações e nenhum APK gerado. Ver [estado atual e aceitação física](../docs/IMPLEMENTACAO_TV_AVO_2026-10-24.md). A descrição com play/pausa da secção 27 é histórica e fica superada por este pedido.
