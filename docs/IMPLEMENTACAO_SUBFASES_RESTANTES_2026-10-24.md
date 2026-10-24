# OwnTV TV — execução das restantes subfases

**Autorização:** 24/10/2026, avançar com as próximas subfases todas. Âmbito: A2/A3/A4, B, C, D, E e verificação transversal OPT-03. MED/NOV continuam condicionais; o modo «TV Avó» será apenas registado como melhoria futura.

**Estado: implementação concluída e validada automaticamente em 24/10/2026.** Aceitação física e publicação remota permanecem pendentes; não foram executadas nesta intervenção.

## Base e recuperação

- App `C:/Users/renat/Downloads/OwnTV-main`; Core TV `C:/Users/renat/Downloads/OwnTV_Core`. Mobile e dados instalados preservados.
- Snapshot inicial conferido: 1 062 inputs, SHA-256 `d6c9d4b67ff4b791f10d91926e1d6694ba986a7b77a368937166543bdc7df4b5`.
- Cópia antes da intervenção: `C:/Users/renat/Downloads/OwnTV-subfases-restantes-backup-20261024-002838/`: ZIP do Core, lock e 588 ficheiros App/configuração/documentação em ZIP, com manifesto e hashes conferidos.
- Preservar esquema 51, credenciais/backups, gravações, HLS/exceções, buffers/latência, reabertura configurável, +1 h separado do relógio e volume interno de 100%.

## Progresso

| Subfase | Alteração | Estado |
|---|---|---|
| A2 / R4 | Invalidação Room do índice de zapping, cache por revisão e recusa de janela antiga; nova consulta limitada se o alvo desaparece. | Implementado; testes e compilação/lint aprovados. Zapping na box por confirmar. |
| A3 / R3 | Confirmação conjunta Exo/mpv antes de criar produtor de timeshift local. | Integrado com o contrato conjunto existente; testes do controlador e compilação aprovados. Transição real por confirmar. |
| A4 / R5 | Reconciliar identidade, seleção e janela visível de «No momento». | Implementado; três testes do reconciliador e integração App aprovados. Foco real por confirmar. |
| B1 / R2 | Export completo recusa omissões de assets; conserva Off sem ficheiro; resumo de itens de legendas não aplicados no restauro. | Core/App compilados, testes e lint aprovados. Restauro integral em dispositivo por confirmar. |
| B2 / R6 | Invalidar aprendizagem temporária de formato por credenciais/UA/identidade da fonte ou headers do canal; limitar retenção. | Core/motores validados por JVM; ensaio com fornecedor pendente. |
| C / INT-08/09 | Versão única, barreira de assinatura/artefacto, permissões/Actions/assets, wrapper/dependências, CI Android. | Ferramentas, YAML e integração Gradle estrita aprovados. Publicação/CI remoto por confirmar. |
| D / INT-06/07 | Tentativas de PIN persistidas e comuns; revogação, limites e staging/temporários de LocalSync. | Core compilado e testes JVM passados; UI e dois dispositivos por validar. |
| E / OPT-01/02 | Consultas informativas/formatters e resíduos comprovados, mantendo quotas e lembretes. | Testes de amostragem/formatters, regressões e lint aprovados; medições de uso na box pendentes. |
| OPT-03 | Matriz de opções, persistência e aplicação ao motor. | Matriz criada; testes repositório→snapshot/escada e reabertura passaram. Hardware exige box. |

## Evidência final automática

- Core: **1 375 testes JVM**, sem falhas; motores: **613 testes JVM**, sem falhas. Testes instrumentados do Core **compilados**, não executados.
- Nova execução normal com verificação estrita de dependências passou sem regenerar checksums (`owntv-remaining-core-strict.log`).
- Snapshot atualizado: **1 071 inputs**, SHA-256 `ff76713c2d027257ff49b416d7bb13c56a5968045319b88252577e4ab796014a`; nomes e bytes comparados com o Core.
- Nove testes Python de release e seis de snapshot passaram; YAML lido e barreiras de permissões/Actions/release conferidas estaticamente.
- 756 artefactos de dependências do Core corroborados por checksum ou bytes/metadata nos repositórios oficiais. AAPT2 inclui Windows/Linux/macOS. Evidência: `tools/release/dependency-provenance.json`.
- **App: 460 testes JVM sem falhas; compilação debug/release, lint debug e lint vital release passaram** na execução normal com dependências verificadas, sem regenerar metadata. Total: **2 448 testes JVM**.
- **15 testes Python:** nove de release e seis de snapshot, sem falhas. YAML lido, Actions fixadas e barreiras de publicação conferidas.
- **App + Core: 1 213 artefactos de dependências corroborados**, incluindo os 756 do Core; são conjuntos sobrepostos, não se somam.
- Logs finais locais: `%TEMP%/owntv-remaining-core-complete.log`, `owntv-remaining-core-strict.log`, `owntv-remaining-app-final.log` (testes Core/motores e compilação Android; falhou lint antes da correção) e **`owntv-remaining-app-verified.log` (BUILD SUCCESSFUL)**. A versão final do código App foi compilada e os 460 testes repetidos após o último ajuste de foco.
- Lint debug: **0 erros**, 143 avisos não fatais; release vital sem erros. Não foi criada baseline de lint nem desativado globalmente o controlo. Os avisos/deprecações existentes não foram todos removidos.
- Baselines i18n conservados: Core 263; App 47. Novas mensagens em inglês e português europeu.

## Detalhes das alterações

- **A:** zapping invalida índices por revisão das tabelas Room e recusa resultados antigos; janela limitada e fallback direcional se o canal desapareceu. Timeshift espera o fecho conjunto dos motores. Home mantém a identidade selecionada visível quando a ordem muda.
- **B:** exportação completa falha explicitamente se faltam recursos de legendas selecionados; seleções Off/timings sem ficheiro não desaparecem. Restauro apresenta itens de legendas não aplicados. Lições temporárias de formato são invalidadas por alteração de credenciais/UA/identidade/headers e têm retenção limitada.
- **C:** `version.properties` mantém **105.1.0.6 / 10510006**. Compilar/testar não exige secrets; release sem chave fica unsigned. CI recusa publicar tag sem chave e certificado autorizado, confere package/versão/debug/ABIs/hashes, espera pelos testes Android e usa permissões de escrita apenas no job de publicação. Actions fixadas em commits oficiais; wrappers com checksum oficial. Verificação de dependências estrita, sem trust global. O controlo da progressão inspeciona o APK realmente publicado e o seu certificado, pois código antigo podia substituir a versão por variáveis de ambiente; não confia apenas no versionCode escrito no Git.
- **D:** PIN com tentativas comuns por alvo/credencial, espera limitada persistida fora do backup e compatibilidade com hashes legados. Novos hashes PBKDF2; migração de hashes de perfis após autenticação válida. Remover restrições/apagar Kids sem PIN exige adulto protegido quando existe. LocalSync revoga segredos no servidor ativo, não substitui identidades silenciosamente, limita bytes reais, cancela pedidos, usa staging por tentativa e limpa temporários por owner/leases/TTL. Fechar o ecrã invalida também uma preparação ainda em curso.
- **E:** informação de armazenamento em Downloads amostra progresso a cada 2 s, mantendo alterações estruturais/raiz imediatas e quotas intactas. Formatters são reutilizados por locale/thread. Removidos mapa sem leitores, bloco experimental desativado, diálogo VOD sem abertura e redundância de lembretes; picker de arranque e cor mantidos.
- **Integração lint:** objetos de foco estabilizados com `remember` por identidade, mantendo as instâncias ao redimensionar o multiview; títulos de canais observados por Flow com snapshot inicial recordado; mensagens de gravação EPG usam recursos atualizados por configuração. Supressão `RestrictedApi` apenas no callback público `Activity.dispatchKeyEvent`, para preservar o despacho do comando; não autoriza helpers internos AndroidX. O fixture JVM do preview configura/liberta explicitamente `Dispatchers.Main`.
- **OPT-03:** consultar `MATRIZ_DEFINICOES_REPRODUCAO_2026-10-24.md`; não confundir valor guardado/aplicado ao snapshot com efeito físico do codec/HDMI.

## Publicação e aceitação por fazer

- Não foi gerado, assinado, instalado ou publicado APK. Não houve execução de CI remoto ou de emuladores nesta intervenção.
- Antes de uma publicação automática futura, configurar a chave de assinatura existente e `vars.RELEASE_CERT_SHA256` com o fingerprint autorizado; não foi criada chave nova nem inventada a identidade instalada.
- Backups antigos continuam legíveis. Novos PIN usam hash versionado PBKDF2; uma aplicação antiga que só reconheça SHA-256 não deve ser considerada compatível com esses novos PIN sem atualização. Não foi alterado o Core privado da aplicação Mobile.
- API 26/34 está na matriz do novo job de testes Android; a sua execução remota permanece por observar.
- Confirmar nas boxes zapping com catálogo alterado, Exo→timeshift, foco Home e preferências por canal; ensaiar LocalSync entre dois dispositivos, PIN/restauro e HDMI/Bluetooth.
- MED/NOV e upscaling continuam condicionais/opcionais. **TV-AVO-01** foi apenas registado em `MELHORIAS_FUTURAS_PENDENTES.md`, conforme pedido.

## Fontes para distribuição

- [Gradle: dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html).
- [GitHub: secure use of Actions](https://docs.github.com/en/actions/reference/security/secure-use).
- [Android: app signing](https://developer.android.com/studio/publish/app-signing).
