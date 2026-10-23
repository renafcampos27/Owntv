# OwnTV — Conselho de Multi-Agentes · Rondas 5 a 8

**Projeto:** `C:\Users\renat\Downloads\OwnTv-main` (OwnTV — IPTV player para Android TV)
**Data:** 2026-10-19 · **Rondas:** 5 (distribuição, segurança, dados, build, UI/UX, player, produto)
· 6 (Core, definições, setup, superfícies secundárias, tooling, **reavaliação**) ·
7 (verificação dos **achados do próprio projecto**: séries B, C, O, A15) ·
8 (**correcções às rondas anteriores** + player-core, Room, C/O)
**Pasta de saída:** `C:\Users\renat\Downloads\OwnTv-main\implementar_rena`
**Ficheiro:** `ANALISE_OWNTV_COUNCIL_2026-10-19_RODA-5.md`

> **Nota de método (Ronda 5):** foi a única ronda que **verifica os achados contra artefactos
> publicados** (API de releases do GitHub, HEAD/GET dos URLs do README, metadados de build
> locais, execução real do snippet bash do CI) e não apenas contra o código. Foi essa camada
> que revelou o problema mais grave de toda a app — e **não é um bug de código**: é um bug de
> *distribuição*.
>
> **Nota de método (Ronda 6):** reavalia **item a item** tudo o que foi registado nas rondas 1–5,
> com veredicto explícito, e **revê as soluções** — incluindo as que estavam erradas. A Ronda 6
> foi pensada com 6 agentes especializados; **todos caíram por uma quebra de ligação** a meio, e o
> conselho re-executou a ronda pessoalmente. Ver §13.2.
>
> **Nota de método (Ronda 8):** um conselho de reavaliação **independente** re-verificou os 10
> achados da Ronda 6 e 50 dos 59 itens priorizados, **procurando precisamente as correcções que
> estivessem erradas**. Encontrou **duas que teriam quebrado a app** (§22.1 e §22.13). **Lê a
> Parte IV antes de implementares a Parte II.**

---

## Índice

**PARTE I — Ronda 5**

| § | Conteúdo | O que vais encontrar |
|---|---|---|
| [1](#1-metodologia) | Metodologia | Os 6 especialistas, como o conselho verificou cada achado, e os limites |
| [2](#2-resumo-executivo) | Resumo executivo | A cadeia causal, a tabela de 61 achados e o scorecard |
| [**3**](#3-distribuição-e-release---os-bugs-mais-graves-achados-novos-desta-ronda) | **Distribuição e release** | 🔴 **A release publicada só tem o APK de 32 bits; o link de download dá 404** |
| [4](#4-segurança-e-qualidade-agente-e4--verificado-por-releitura-do-core-extraído) | Segurança e qualidade | Cleartext com credenciais no URL, segredo no APK, PIN fraco, testes que nunca correm |
| [5](#5-bugs-de-dados-performance-e-memória-agente-e3) | Dados e performance | 50k canais materializados, 17 flows `Eagerly`, backup automático morto |
| [6](#6-build-arquitectura-e-i18n-agente-e5) | Build, arquitectura e i18n | 🔴 **266 strings traduzidas em 2 de 26 idiomas**; `splits.abi` contraditório |
| [7](#7-estratégia-de-produto-e-inovações-agente-e6) | Produto e inovações | SWOT honesto, 20 inovações, os 5 primeiros minutos, monetização GPLv3, acessibilidade |
| [8](#8-uiux-de-tv--foco-d-pad-e-funcionalidades-invisíveis-agente-e2) | UI/UX de TV | 🔴 **Pesquisa sem porta; Now Trending morto; Downloads só gravações; Favoritos inalcançáveis** |
| [8-B](#8-b-player-reprodução-e-zapping-agente-e1) | Player e zapping | O OSD que mostra o canal errado, foco do MultiView, direct tune que se cancela a si próprio |

**PARTE II — Ronda 6**

| § | Conteúdo | O que vais encontrar |
|---|---|---|
| [13](#13-ronda-6--método) | Método da ronda | O que mudou, como foi executada, o que foi verificado directamente |
| [**14**](#14-bugs-e-melhorias-novos-da-ronda-6) | **Bugs novos** | 🔴 **DoS de CPU no backup (SEC-12)**; OOM na sync local; perda de dados silenciosa; 44 `StateFlow`s; herança de 500 ms no zapping |
| [**15**](#15-reavaliação-do-council--todos-os-achados-anteriores-verificados) | **Reavaliação** | Cada achado das rondas 1–5 com veredicto — e **as correcções que estavam erradas** |
| [16](#16-veredicto-final--o-que-continua-a-ser-uma-melhoria-correção-ou-ideia-válida) | **Veredicto (Ronda 6)** | 59 itens em 6 fases, cada um com a solução e o ficheiro |

**PARTE III — Ronda 7**

| § | Conteúdo | O que vais encontrar |
|---|---|---|
| [19](#19-ronda-7--método-e-o-que-há-de-novo) | Método | A verificação dos **~71 achados do próprio projecto** |
| [**20**](#20-achados-novos-da-ronda-7) | **Achados novos** | 🔴 **R7-01: a mitigação do bloqueio A–B–A é temporal, mas o provider pune a *contagem***; R7-02: o B7 foi corrigido mas o comentário está errado e não há teste |
| [**21**](#21-verificação-dos-achados-do-próprio-projecto-ronda-7) | **Verificação do projecto** | 14 dos 16 A15 corrigidos; 12 dos 15 B; **e as 7 correcções que não devem ser implementadas** |

**PARTE IV — Ronda 8**

| § | Conteúdo | O que vais encontrar |
|---|---|---|
| [**22**](#22-⚠️-correções-à-parte-ii--as-soluções-que-estavam-erradas) | **CORRECÇÕES** | 🔴 **A minha solução do SEC-12 partiria todos os backups cifrados**; F7 quebrava o build; UX-11/-02/DAT-01/PLY-07/DWN-01/SEC-04 também |
| [**23**](#23-achados-novos-da-ronda-8-player-core-room-e-séries-co) | **Achados novos** | 🔴 **PLY-13: um Pause no handover encrava a sintonia para sempre**; PLY-14: toda a falha não-HTTP é "de formato"; **DB-01: um trigger FTS4 perdido é permanente**; DB-04: 148 ms por página **[medido]** |
| [**24**](#24-veredicto-final--o-que-continua-válido-com-a-solução-correcta) | **VEREDICTO FINAL** | **~144 itens válidos**, em 7 fases, com a solução correcta; ~26 arquivados; a lista das correcções que **não** devem ser implementadas |
| [25](#25-limitações-finais) | Limitações | O que **não** foi verificado |
| [26](#26-conclusão-final-das-8-rondas) | Conclusão | A ordem que o conselho recomenda |

**Leitura rápida:** se só tiveres 5 minutos, lê **§2.1 (a cadeia causal)**, **§3 (REL-01, REL-05)**,
**§8 (UX-01 a UX-04)**, **§22.1 (a correcção do SEC-12)** e **§24.2 + §24.3**. Esses são os itens
que impedem um utilizador novo de instalar a app, de a usar, de a ler na sua língua — e um dos
achados que, se for implementado como está escrito, **parte a funcionalidade de backup**.

---

## 1. Metodologia

### 1.1 Conselho de multi-agentes (6 especialistas, em paralelo)

| # | Especialidade | Âmbito atribuído |
|---|---|---|
| **E1** | Engenheiro de media/playback | `app/player/**` (22 ficheiros, ~4 700 linhas), `LiveViewModel` (2 714), Multiview, catch-up, zapping |
| **E2** | Engenheiro UI/UX TV (Compose for TV, foco, D-pad) | `features/shell/**`, `features/home`, `features/live/LiveScreen`, `features/epg`, `ui/**` (65 ficheiros) |
| **E3** | Engenheiro de dados/performance (Room, Paging, corrotinas) | Todos os ViewModels, WorkManager, cold start, `diagnostics/**` |
| **E4** | Engenheiro de segurança + QA lead | Manifest, backup, PIN, updates, DNS, `AutoStartService`, CI, testes, Core extraído |
| **E5** | Arquiteto + build engineer | Gradle, AGP, R8, catálogo de versões, CI, `core-source-lock.json`, tooling i18n |
| **E6** | Product strategist (OTT/IPTV) | SWOT vs concorrentes, roadmap 3 horizontes, retenção, acessibilidade |

### 1.2 Consolidação do conselho

1. Cada agente devolve achados com **ficheiro + linha + snippet real** (nada de "deve-se verificar").
2. O conselho **releu e verificou** cada achado antes de o promover a «confirmado».
3. Achados já registados em `implementar_rena/` (BUG-P*, INN-*, MEL-*, TEC-*, UX-*, R4-B*) ou em
   `docs/MELHORIAS_FUTURAS_PENDENTES.md` são marcados como **repetição** e não contam como novidade.
4. Achados que vivem no **Core externo** (`tv.own.owntv:core` / `:player-core`, repositório
   separado, vendorizado como snapshot SHA-256) são **separados** dos que são puramente do lado app.
5. **Severidade** = impacto no utilizador final de uma box real, não a beleza técnica do problema.

### 1.3 Limitações declaradas

Análise estática + verificação de artefactos publicados. **Não houve build, testes nem execução em
dispositivo.** Cada item traz nível de confiança e o que é preciso para o confirmar em hardware.
As referências de linha do Core são do snapshot `e2081e78…` (o que `core-source-lock.json` fixa),
não do repositório upstream.

---

## 2. Resumo executivo

O OwnTV é, em termos de engenharia, um **projecto acima da média**: 243 ficheiros Kotlin /
~65 000 linhas, 80+ testes unitários no app (+242 no Core), 6 workflows de CI, um baseline profile
gravado, um verificador de literais hardcoded em Python ligado ao `preBuild`, um lock SHA-256 do
Core externo, **zero `TODO`/`FIXME`**, **zero `GlobalScope`**, **zero `runBlocking`**,
**zero `Thread.sleep`** e apenas **5 supressões `@Suppress` em 65 000 linhas**. A disciplina de
cancelamento de corrotinas é exemplar (87 `CancellationException` explícitos).

> **A app está bem construída e mal distribuída.** É essa a conclusão desta ronda.

### 2.1 A cadeia causal (achado central)

Este é o achado mais importante e não tinha sido visto em nenhuma das rondas anteriores. Os quatro
problemas de distribuição não são independentes — **são a mesma causa**:

```
Tag v105.1.0.5
   │
   ▼  .github/workflows/android.yml  linhas 112-114
IFS='.' read -r MA MI PA <<< "105.1.0.5"     →  PA="0.5"   (o resto da linha!)
CODE=$(( 105*10000 + 1*100 + 0.5 ))          →  arithmetic syntax error
   │
   ▼  o passo "Compute version" MORRE  →  o job "build" não arranca
a tag nunca constrói nem publica nada
   │
   ▼  o maintainer publica a release À MÃO
   • corpo vazio              → o diálogo "O que há de novo" não mostra nada
   • só o APK armeabi-v7a     → 32 bits, e o "always-latest" OwnTV.apk dá 404
   • sem universal, sem x86_64
```

**Consequência prática: hoje, um utilizador novo que siga o README não consegue instalar a app, e
quem conseguir instalar recebe a build de 32 bits.**

### 2.2 Tabela-resumo dos achados

| ID | Sev. | Achado | Onde | Confiança |
|---|---|---|---|---|
| **REL-01** | 🔴 Crítica | Release publicada só tem o APK **32-bit**; `OwnTV.apk` e o universal dão **404** | GitHub Releases | **Alta** (verificado por API + HEAD) |
| **REL-05** | 🔴 Crítica | O passo "Compute version" do CI **rebenta** em todas as tags de 4 componentes → nenhuma tag constrói | `android.yml:112-114` | **Alta** (reproduzido em bash) |
| **REL-02** | 🟠 Alta | O CI publica o APK **universal de 52,8 MB**; o split arm64 é 32 MB — o `splits.abi` não serve para nada | `android.yml:185` | **Alta** (tamanhos medidos) |
| **REL-06** | 🟠 Alta | **Dois esquemas de `versionCode` incompatíveis**: local `10510005` vs CI `1050100` (menor!) | `build.gradle.kts:50` vs `android.yml:114` | **Alta** |
| **REL-03** | 🟠 Alta | Corpo das releases **vazio** — o "What's new" in-app não mostra nada | GitHub Releases | **Alta** |
| **REL-07** | 🟠 Alta | `CHANGELOG.md` e `CHANGELOG_APP.md` **não têm secção `v105.*`** (param em `v100.0.0`) | repo | **Alta** |
| **DOC-01** | 🟠 Alta | 9 links do README apontam para `renatofc27/OwnTV` — **repo inexistente (404)** | `README.md` | **Alta** (HTTP verificado) |
| **DOC-02** | 🟡 Média | O README manda criar um **token do GitHub Packages que já não é usado** (o Core vem de snapshot local) | `README.md:206-217` | **Alta** |
| **SEC-01** | 🟠 Alta | Cleartext global + **credenciais Xtream/Stalker no path e na query** | manifest + Core | **Alta** |
| **SEC-02** | 🟠 Alta | Segredo do edge worker **embutido em todas as APKs** | `build.gradle.kts:84` | **Alta** |
| **SEC-03** | 🟠 Alta | Build release **cai silenciosamente para a chave debug** | `build.gradle.kts:175-179` | **Alta** |
| **QA-01** | 🟠 Alta | CI **nunca corre os `androidTest`** — 19 classes nunca executam | `android.yml:51-54` | **Alta** |
| **QA-03** | 🟠 Alta | CI pode publicar um APK **debug-assinado e debuggable** como `OwnTV.apk` | `android.yml:193-202` | **Alta** |
| **SEC-04** | 🟡 Média | PIN de perfil: **SHA-256 simples**, sem lockout, sem backoff | Core `util/Pin.kt` | **Alta** |
| **SEC-05** | 🟡 Média | Credenciais IPTV, palavra-passe de proxy e chaves API em **texto claro** | Core `ProfileEntities` | **Alta** |
| **SEC-06** | 🟡 Média | Passphrase do backup **sem força mínima** (1 char abre um ficheiro com credenciais) | `BackupScreen.kt:447` | **Alta** |
| **SEC-08** | 🟡 Média | `FileProvider` expõe a **árvore inteira** do armazenamento partilhado | `file_paths.xml:8` | **Alta** |
| **SEC-09** | 🟡 Média | APK de actualização **sem verificação de integridade** (só packageName + ABI) | Core `UpdateManager` | **Alta** |
| **QA-02** | 🟡 Média | **Zero** SAST / scan de dependências / scan de segredos / SBOM / Dependabot | `.github/` | **Alta** |
| **QA-09** | 🟡 Média | `mapping.txt` do R8 **nunca é arquivado** → crashes de release indecifráveis | `android.yml:228` | **Alta** |

### 2.2-bis Inventário completo dos achados desta ronda

**🔴 Críticos (4)**

| ID | Achado | Secção |
|---|---|---|
| **REL-01** | A release publicada só tem o APK **32-bit**; o link "always-latest" dá **404** | §3 |
| **REL-05** | O passo "Compute version" do CI **rebenta** em todas as tags de 4 componentes → **nenhuma tag constrói** | §3 |
| **F4** | **266 strings da app traduzidas em apenas 2 dos 26 idiomas anunciados** | §6 |
| **UX-01** | A **Pesquisa** não tem porta de entrada na UI (regressão) | §8 |

**🟠 Altos (16)**

| ID | Achado | Secção |
|---|---|---|
| **REL-02** | O CI publica o APK **universal de 52,8 MB**; o split arm64 é 32 MB | §3 |
| **REL-06** | **Dois esquemas de `versionCode` incompatíveis** (local 10510005 vs CI 1050100, menor) | §3 |
| **REL-07** | `CHANGELOG.md` / `CHANGELOG_APP.md` **sem secção `v105.*`** | §3 |
| **DOC-01** | 9 links do README para um repositório **inexistente** (404) | §3 |
| **SEC-01** | Cleartext global + **credenciais Xtream/Stalker no path e na query** | §4 |
| **SEC-02** | Segredo do edge worker **embutido em todas as APKs** | §4 |
| **SEC-03** | Build release **cai silenciosamente para a chave debug** | §4 |
| **QA-01** | CI **nunca corre os `androidTest`** — 19 classes mortas | §4 |
| **QA-03** | CI pode publicar um APK **debug-assinado e debuggable** como `OwnTV.apk` | §4 |
| **UX-02** | "Now Trending" e o "Continuar a ver" VOD **nunca** são renderizados | §8 |
| **UX-03** | "Downloads" mostra **só gravações**; filmes/episódios transferidos invisíveis | §8 |
| **UX-04** | Favoritos / Histórico / Sincronização local **nunca** são desenhados | §8 |
| **DAT-01** | O paging do catálogo Live **materializa 50k canais** e repete em cada invalidação | §5 |
| **DAT-02** | 17 flows `Eagerly` constroem o `LiveViewModel` **no primeiro frame** | §5 |
| **DAT-03** | O Live TV usa `MAX_SIZE_UNBOUNDED` → **50k entidades retidas** | §5 |
| **DAT-04** | O backup automático **nunca vai para foreground** → morto e apagado a meio | §5 |

**🟡 Médios (28)** — SEC-04…SEC-10, QA-02, QA-04…QA-11, F3, F6…F14, DAT-05…DAT-14, UX-05…UX-12,
PLY-01…PLY-11.

**🟢 Baixos (13)** — SEC-11, DAT-15…DAT-21, UX-13…UX-15, PLY-12…PLY-15, REL-04, REL-08, REL-09,
F15, F19.

**Total: ~61 achados** (4 críticos, 16 altos, 28 médios, 13 baixos) + 20 inovações de produto +
um roadmap de 3 horizontes.

### 2.3 Scorecard

| Dimensão | Nota | Comentário |
|---|---|---|
| **Build** | 3/5 | AGP 9 + KSP2 + config cache, sem dependências mortas graves. Perde no fallback de assinatura debug e no APK universal. |
| **Testes** | 3/5 | 80+ testes JVM e bons testes de política. Zero `androidTest` no CI; nenhum teste sobre o caminho de release. |
| **Segurança** | 2/5 | `MANAGE_EXTERNAL_STORAGE` + cleartext global + segredo no APK + AccessibilityService. Muitos já conhecidos, nenhum corrigido. |
| **i18n** | 5/5 | 26 idiomas empacotados, RTL, pseudolocales, ratchet de literais em CI, catálogo JSON versionado. **Exemplar.** |
| **Performance** | 3/5 | Corrotinas e estado bem geridos; estrutura a fraturar (3 monolitos gigantes). |
| **Manutenção** | 3/5 | Zero dívida de TODO, 5 `@Suppress`, 1 099 literais seguros, 56 no baseline. |
| **Distribuição** | **1/5** | **É aqui que a app falha hoje.** Release errada, links errados, release notes vazias, tag pipeline morta. |
| **Produto** | 4/5 | Posicionamento "player only, BYO-source" coerente e defensável; GUI de TV excelente. Falta a salvaguarda de distribuição. |

---

## 3. Distribuição e release — os bugs mais graves (achados novos desta ronda)

### REL-01 — A release publicada só tem o APK de 32 bits; o link "always-latest" dá 404

**Severidade: CRÍTICA** · **Confiança: alta** · **Verificado por API do GitHub + HEAD de URL**

**Evidência** (`https://api.github.com/repos/renafcampos27/Owntv/releases?per_page=3`):

```json
[
  { "tag": "v105.1.0.5", "published": "2026-10-18T17:01:23Z",
    "body": "",
    "assets": [ { "name": "app-standard-armeabi-v7a-release.apk", "size": 30.3 MB, "download_count": 0 } ] },
  { "tag": "v105.1.0.4", "published": "2026-10-16T14:54:52Z",
    "body": "",
    "assets": [ { "name": "app-standard-armeabi-v7a-release.apk", "size": 30.3 MB, "download_count": 3 } ] },
  { "tag": "v105.1.0.3", "published": "2026-10-09T07:16:37Z",
    "body": "",
    "assets": [ { "name": "app-standard-armeabi-v7a-release.apk", "size": 30.3 MB, "download_count": 1 } ] }
]
```

**Verificação de todos os URLs de download documentados:**

| URL | Estado |
|---|---|
| `…/releases/latest/download/OwnTV.apk` ← **o link "always-latest" do README e do código Fire TV** | **404** |
| `…/releases/latest/download/app-standard-universal-release.apk` | **404** |
| `…/releases/latest/download/app-standard-arm64-v8a-release.apk` | **404** |
| `…/releases/latest/download/app-standard-armeabi-v7a-release.apk` | **200** (30,3 MB) |
| `…/releases/latest/download/OwnTV-x86_64-v105.1.0.5.apk` | **404** |

**Impacto**

1. **O caminho de instalação documentado está morto.** O `README.md:191` dá
   `https://github.com/renatofc27/OwnTV/releases/latest/download/OwnTV.apk` (e o número do
   Downloader do Fire TV, `4308278`, aponta para a página de releases). Quem segue o README recebe
   uma página 404 em vez do APK.
2. **A única build disponível é de 32 bits.** `armeabi-v7a` é a build de 32 bits. Em boxes Android TV
   modernas (imagem 64-bit-only — increasingly common em Android 13+ e em Android TV / Google TV
   powered by newer SoCs) a instalação falha com `INSTALL_FAILED_NO_MATCHING_ABIS`.
3. **Mesmo onde instala, corre a 32 bits.** O ART 32-bit tem um tecto de heap bem mais baixo
   (~3 GB vs ~6 GB em 64-bit) e o `libmpv` empacotado é a build de 32 bits — pior desempenho de
   descodificação num dispositivo cujo público-alvo é explicitamente hardware de gama média/baixa
   (`app/build.gradle.kts:89-94`).
4. **A app perde a sua própria promessa.** O `README.md:89-94` e o comentário em
   `app/build.gradle.kts:89-94` justificam o `splits.abi` exactamente para evitar um APK universal
   de 104 MB. O artefacto publicado é o oposto do pretendido.

**Solução**

```bash
# 1) O CI tem de publicar o que o README promete. Ver REL-05 para o bloqueio que impede isto.
#    Em android.yml, o passo "Name the APK" já cria dist/OwnTV.apk; o problema é que nunca corre.
#
# 2) Publicar os TRÊS artefactos e não um:
#      dist/OwnTV-v<version>.apk     (universal arm, 52,8 MB — o que o README promete)
#      dist/OwnTV.apk                (alias estável)
#      dist/OwnTV-x86_64-v<version>.apk
#
# 3) NUNCA publicar apenas um split por ABI. O arm64-v8a é o alvo principal.
```

E **no lado do produto**: enquanto o link "always-latest" não existir, o README deve dizer
explicitamente qual é o URL real, e o `UpdateManager` (Core) deve escolher o asset **por
`SUPPORTED_ABIS`**, preferindo o universal, e nunca o `armeabi-v7a` quando o dispositivo suporta
`arm64-v8a`. Hoje o `selectUpdateApk` (Core `update/UpdateManager.kt:142`) escolhe por ABI — se
apenas existir o `armeabi-v7a`, ele instala o de 32 bits num dispositivo 64-bit, o que **é
exactamente o que aconteceu**.

**Prevenção**

Um job de CI que, depois de publicar, faz `curl -fI` a cada URL prometido e **falha** se algum
devolver != 200. Trinta segundos de script que apanham esta classe de erro para sempre.

---

### REL-05 — O passo "Compute version" do CI rebenta em todas as tags de 4 componentes

**Severidade: CRÍTICA** · **Confiança: alta** · **Reproduzido em bash (Git Bash 5.x)**

**Localização:** `.github/workflows/android.yml:108-125`

```yaml
108:      - name: Compute version (from the git tag on releases)
109:        id: ver
110:        run: |
111:          if [ "$IS_TAG" = "true" ]; then
112:            NAME="${GITHUB_REF_NAME#v}"                 # tag v1.2.0 -> 1.2.0
113:            IFS='.' read -r MA MI PA <<< "$NAME"
114:            CODE=$(( ${MA:-0} * 10000 + ${MI:-0} * 100 + ${PA:-0} ))   # 1.2.0 -> 10200
115:            [ "$CODE" -gt 0 ] || CODE=1
```

**Reprodução exacta** (executada, não teorizada):

```
---- tag v105.1.0.3 ----
line 5: 105 * 10000 + 1 * 100 + 0.3 : arithmetic syntax error: invalid arithmetic operator
  subshell exit status: 1
---- tag v105.1.0.5 ----
line 5: 105 * 10000 + 1 * 100 + 0.5 : arithmetic syntax error: invalid arithmetic operator
  subshell exit status: 1
---- tag v105.2.0.1 ----
line 5: 105 * 10000 + 2 * 100 + 0.1 : arithmetic syntax error: invalid arithmetic operator
  subshell exit status: 1
---- tag v6.0.0 ----   MA=[6] MI=[0] PA=[0]  CODE=60000     exit 0
---- tag v100.0.0 ---- MA=[100] MI=[0] PA=[0]  CODE=1000000  exit 0
---- tag v1.2.0 ----   MA=[1] MI=[2] PA=[0]  CODE=10200     exit 0
```

**Porquê**

Em bash, `read` atribui **o resto da linha** à última variável. Para `"105.1.0.5"` isso dá
`MA="105"`, `MI="1"`, **`PA="0.5"`** — e `$(( … + 0.5 ))` é uma expressão aritmética **inválida**
(inteiros). O passo morre.

- **Com `set -e`** (o default do GitHub Actions é `bash -e {0}`): o passo falha, o job `build`
  **não arranca**, a tag não produz APK nem release.
- **Sem `set -e`**: `CODE` fica vazio, `[ "$CODE" -gt 0 ]` dá `[: : integer expected`, e
  `CODE=1` — um APK com **versionCode 1**, que o Android recusa instalar por cima de qualquer
  versão anterior.

**Isto é a causa-raiz de REL-01, REL-03 e REL-04.** Todas as tags `v105.1.0.x` (4 componentes)
rebentam; as de 3 componentes (`v100.0.0`, `v6.0.0`) funcionariam. O esquema de versões actual
(`105.1.0.5`) tem **sempre** 4 componentes → **o pipeline de release está morto desde que o
esquema de 4 componentes foi introduzido.**

**Solução** — substituir o cálculo por um esquema explícito e testado:

```bash
# versionCode = MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + BUILD
# v105.1.0.5 -> 105*1000000 + 1*10000 + 0*100 + 5 = 105010005
IFS='.' read -r MA MI PA BUILD <<< "$NAME"
MA=${MA:-0}; MI=${MI:-0}; PA=${PA:-0}; BUILD=${BUILD:-0}
CODE=$(( MA * 1000000 + MI * 10000 + PA * 100 + BUILD ))
[ "$CODE" -gt 0 ] || CODE=1
```

E — porque o valor tem de ser **igual** ao que `app/build.gradle.kts` usa localmente — a fonte da
verdade tem de ser **uma só**. Proposta: calcular o `versionCode` em **Gradle** (uma função Kotlin
em `build.gradle.kts`) e o CI lê esse valor, em vez de duplicar a fórmula em bash.

**Teste de regressão obrigatório** (o que faltou): um teste do script de versionamento, executado
no CI, que verifique `v1.2.0 → 10200`, `v105.1.0.5 → 105010005`, `v105.1.0.6 > v105.1.0.5`,
`v6.0.0 → 60000`. Uma tabela de 6 linhas apanha este bug antes do primeiro release.

---

### REL-02 — O CI publica o APK universal de 52,8 MB; o `splits.abi` não serve para nada

**Severidade: ALTA** · **Confiança: alta** · **Tamanhos medidos nos artefactos de build locais**

**Evidência** (`app/build/outputs/apk/standard/release/`, medido):

| APK | Tamanho | ABIs (verificado dentro do .apk) |
|---|---|---|
| `app-standard-universal-release.apk` | **52,8 MB** | arm64-v8a + armeabi-v7a |
| `app-standard-arm64-v8a-release.apk` | **32,0 MB** | arm64-v8a |
| `app-standard-armeabi-v7a-release.apk` | **30,3 MB** | armeabi-v7a |

**Localização:** `.github/workflows/android.yml:185`

```bash
185:            ARM_SRC=app/build/outputs/apk/standard/release/app-standard-universal-release.apk
```

O `splits.abi` (`app/build.gradle.kts:110-117`) existe, produz os 3 APKs correctos — e o CI
**escolhe o universal**. O comentário em `app/build.gradle.kts:89-94` justifica o split com
*"~49MB vs the old 104MB universal APK"*; o artefacto que chega ao utilizador tem 52,8 MB.

**Impacto:** cada instalação numa box com ligação lenta (o público-alvo) gasta **+65 %** dos dados
necessários (52,8 MB vs 32,0 MB). Numa box com 4 GB de armazenamento interno, 20 MB a mais por
instalação/actualização.

**Solução**

```bash
# Publicar o split arm64 como principal, e o universal como fallback para as boxes 32-bit.
ARM_SRC=app/build/outputs/apk/standard/release/app-standard-arm64-v8a-release.apk
test -f "$ARM_SRC"
# e publicar TAMBÉM o universal com um nome próprio, para boxes armeabi-v7a:
cp app/build/outputs/apk/standard/release/app-standard-universal-release.apk dist/OwnTV-universal-v<ver>.apk
```

E, para reduzir ainda mais: o `libmpv` é o grosso do APK. **Verificar quanto do APK é `libmpv`
por ABI** — se `libmpv` sozinho der ~25 MB por ABI, reduzir o número de ABIs empacotadas é a
alavanca de maior impacto. (Medição a fazer: `unzip -l` por ABI.)

---

### REL-06 — Dois esquemas de `versionCode` incompatíveis no mesmo projecto

**Severidade: ALTA** · **Confiança: alta**

| Origem | Fórmula | `v105.1.0.5` dá |
|---|---|---|
| `app/build.gradle.kts:49-51` (local, default) | esquema de 5 dígitos por major | **10 510 005** |
| `.github/workflows/android.yml:114` (CI) | `MA*10000 + MI*100 + PA` | **1 050 100** (e 0 se a tag rebentar) |

**Impacto**

1. O valor do CI (**1 050 100**) é **menor** que o local (**10 510 005**). Um developer que teste
   com a build local e depois instale a release recebe
   `INSTALL_FAILED_VERSION_DOWNGRADE` — o Android recusa.
2. O comentário em `app/build.gradle.kts:49` diz *"so it installs directly over legacy builds"* — o
   que é verdade para o default local, mas **falso** para o valor que o CI produz.
3. O `versionCode` do CI é da mesma ordem de grandeza do `versionName` (`105.1.0.5`), o que impede
   qualquer crescimentoPatch dentro do mesmo minor.

**Solução:** uma única fonte de verdade (ver REL-05) + um `versionCode` monotónico
`MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + BUILD`, que cresce sempre e deixa espaço para
patches dentro do mesmo minor.

---

### REL-03 — O corpo das releases está vazio: o "What's new" in-app não mostra nada

**Severidade: ALTA** · **Confiança: alta**

As três últimas releases têm `"body": ""`. O `UpdateDialog` (Core `update/`) e o
`.github/workflows/release-notes.yml` deviam preencher o corpo a partir da secção do
`CHANGELOG_APP.md`.

**Causa:** não existe secção `v105.*` no `CHANGELOG_APP.md` (ver REL-07), e o workflow de release
notes corre em `on: release: published` — que **não é disparado** por releases criadas pelo
`GITHUB_TOKEN` (comentário correcto no próprio `android.yml:244-246`). Como as releases foram
publicadas à mão, o workflow nunca correu.

**Solução:** o passo "Extract release notes" do `android.yml` já extrai a secção correcta — mas
precisa de a secção existir. Publicar o corpo **no mesmo passo que cria a release** (já é o caso
no `android.yml:236-269`) garante que nunca fica vazio.

---

### REL-07 — Nem `CHANGELOG.md` nem `CHANGELOG_APP.md` têm secção `v105.*`

**Severidade: ALTA** · **Confiança: alta**

```
CHANGELOG_APP.md  → última secção: "## v100.0.0 — 2026-09-24"
CHANGELOG.md      → nenhuma secção "^## v105"
```

E o git log mostra que as versões `v105.x` entraram por **push directo a `main`**:

```
bdffec4 Update app version to v105.1.0.5 and sync all local changes
2c0c4f9 Update app version to v105.1.0.4 and sync all local changes
67ebecf Update app version to v105.1.0.3 and sync all local changes
```

**Porquê isto é um bug e não só desarrumação**

- `changelog-sync-check.yml` só corre em `on: pull_request`. Pushes directos a `main` **passam
  completamente por cima** da verificação. É por isso que o buraco existe.
- Consequência directa: o diálogo de actualização in-app mostra um "What's new" **vazio** ao
  utilizador que actualiza de qualquer versão anterior.
- Consequência de processo: as releases `v105.1.0.3`, `.4` e `.5` são **sem qualquer nota de
  alterações** — nem para o utilizador, nem para o mantenedor.

**Solução**

1. Mover a verificação de changelog para um job de **push a `main`** (não só `pull_request`), ou
   melhor: um workflow `release-preflight` que, numa tag, **falha** se não existir secção
   correspondente em `CHANGELOG_APP.md`.
2. Escrever as secções `v105.1.0.x` (recuperáveis do `CHANGELOG.md` e do histórico de commits).
3. Manter o push directo a `main` **proibido** para releases (branch protection), ou forçar o
   changelog por hook.

---

### DOC-01 — 9 links do README apontam para um repositório que não existe

**Severidade: ALTA** · **Confiança: alta** · **Verificado por HTTP**

O remote real é `https://github.com/renafcampos27/Owntv.git` (confirmado por `git remote -v`).
O README aponta para `renatofc27/OwnTV`:

```
README.md:23   <a href="https://github.com/renatofc27/OwnTV/actions/workflows/android.yml">
README.md:24   <img alt="Android CI" src="https://github.com/renatofc27/OwnTV/actions/workflows/android.yml/badge.svg">
README.md:174  - 📱 **[OwnTV Mobile](https://github.com/renatofc27/OwnTV_Mobile)**
README.md:176  - 🧩 **[OwnTV Core](https://github.com/renatofc27/OwnTV_Core)**
README.md:187  Grab the signed APK from the [**latest release**](https://github.com/renatofc27/OwnTV/releases/latest).
README.md:191  https://github.com/renatofc27/OwnTV/releases/latest/download/OwnTV.apk
README.md:209  [OwnTV_Core](https://github.com/renatofc27/OwnTV_Core) repository …
README.md:212  1. **Get the code** — `git clone https://github.com/renatofc27/OwnTV.git`
```

**Verificação HTTP:**

| URL | Estado |
|---|---|
| `api.github.com/repos/renatofc27/OwnTV` | **404 — o repositório não existe** |
| `api.github.com/repos/renafcampos27/Owntv` | **200** |
| `api.github.com/repos/renatofc27/OwnTV_Core` | **404** |
| `api.github.com/repos/ahXN00/OwnTV_Core` | **200** |

O próprio `alteracoes_IA_google.md:13-15` documenta que as referências foram "alteradas… para o
novo repositório do utilizador" — mas só o `CoreBuildInfo.releaseRepo` foi corrigido
(`OwnTVApp.kt:95` = `renafcampos27/Owntv`); **o README e os comentários do build ficaram para trás**.

**Impacto:** badge de CI partido; link de download do Downloader do Fire TV partido; link de clone
partido; links para o Core e o Mobile partidos. Num repositório open-source, a primeira impressão
de um possível contribuidor é um README com links mortos.

**Solução:** `renatofc27/OwnTV` → `renafcampos27/Owntv` no README (8 ocorrências) e
`renatofc27/OwnTV_Core` → `ahXN00/OwnTV_Core` (3 ocorrências, incluindo
`gradle/libs.versions.toml:71`, `tools/i18n/README.md:5`, `.github/workflows/i18n.yml:10`,
`app/build.gradle.kts:7,350`).

> **Nota:** `extras/worker/README.md` e `extras/USER_GUIDE.md` **não** têm este problema — o
> `USER_GUIDE` é o documento mais importante para o utilizador final e está correcto.

---

### DOC-02 — O README manda criar um token do GitHub Packages que já não é usado

**Severidade: MÉDIA** · **Confiança: alta**

`README.md:206-217` instrui:

> ### 2. **Add a token** — create a personal access token (classic) with the single scope
> **`read:packages`**, then put it in `~/.gradle/gradle.properties` …

**Isso já não é verdade.** O Core **não vem de GitHub Packages** — vem de um snapshot de código
fonte vendorizado e verificado por SHA-256:

- `settings.gradle.kts:18-32` → `includeBuild(coreRoot)` com `coreRoot` = o snapshot extraído.
- `core-source-lock.json` → `archive: vendor/owntv-core-source-e2081e78….zip`, `sha256: e2081e78…`.
- `gradle/custom-core.settings.gradle.kts:18-32` **verifica o SHA-256 de cada um dos 1 007
  ficheiros** antes de construir, e falha com *"Customized Core changed"* se algo divergir.
- O `settings.gradle.kts:24-31` só usa Maven para `tv.own.owntv:libmpv` (o motor nativo), a partir
  de `ahxn00.github.io/OwnTV_Core/maven` — que é público e não precisa de token.

**Impacto:** um contribuidor novo segue o README, cria um token desnecessário, e — pior — **nunca
lê que pode construir sem ele**. E o README não menciona a única propriedade que a versão local
precisa: `owntv.corePath` (encontrada em `~/.gradle/gradle.properties` nesta máquina:
`owntv.corePath=C:/Users/renat/Downloads/OwnTV_Core`).

**Solução:** reescrever a secção "Building & running" para descrever o modelo real
(snapshot vendorizado + verificação SHA-256), remover o passo do token, e documentar
`owntv.corePath` como override opcional para quem trabalha no Core.

---

### REL-04 — O flavor `x86_64` nunca chega a ser publicado (e pode nem produzir APK)

**Severidade: MÉDIA** · **Confiança: média-alta**

- `README.md:199-201` promete: *"Each release ships **two** APKs: `OwnTV.apk` (arm) and
  `OwnTV-x86_64-vX.X.X.apk`"*.
- A release real tem **um** asset, e não é o x86_64.
- `android.yml:188` → `X86_SRC=$(ls app/build/outputs/apk/x86_64/release/*.apk | head -n1)`.
  Se a pasta não existir, `ls` falha, `X86_SRC` fica vazio, e o `if [ -n "$X86_SRC" ]` (linha 222)
  **salta silenciosamente** a cópia. **Sem erro, sem aviso.**
- A combinação `flavorDimensions "abi"` + `splits.abi { include("armeabi-v7a","arm64-v8a") }`
  (`app/build.gradle.kts:98-117`) é **contraditória**: o flavor `x86_64` tem `abiFilters = [x86_64]`,
  que não intersecta a lista de splits. O resultado é um flavor sem splits de ABI — o que
  provavelmente resulta apenas num APK universal x86_64, mas **não está garantido nem testado**.
- Nos artefactos de build locais existe `app/build/intermediates/**/x86_64Debug` (o flavor foi
  *configurado*) mas **não existe `app/build/outputs/apk/x86_64/`** (nenhum APK produzido).

**Solução**

1. `splits.abi` deve incluir `x86_64` no `include`, ou ser removido em favor de `ndk.abiFilters`
   apenas (com `isUniversalApk` por flavor).
2. `android.yml` deve **falhar** se `X86_SRC` estiver vazio numa build de tag, em vez de saltar.
3. Verificar com `test -f` explícito em vez de `ls | head -n1` (que também é não-determinístico se
   houver mais de um APK).

---

### REL-08 — 7 snapshots do Core vendorizados (≈27 MB de binários no git) mas só 1 é usado

**Severidade: BAIXA (higiene)** · **Confiança: alta**

```
vendor/owntv-core-source-271cc1bb….zip    3,82 MB   (obsoleto)
vendor/owntv-core-source-312b8624….zip    3,82 MB   (obsoleto)
vendor/owntv-core-source-3db885fd….zip    3,80 MB   (obsoleto)
vendor/owntv-core-source-e2081e78….zip    3,82 MB   ← ESTE é o do core-source-lock.json
vendor/owntv-core-source-f2cdeba2….zip    3,82 MB   (obsoleto)
vendor/owntv-core-source-f828db1a….zip    3,82 MB   (obsoleto)
vendor/owntv-core-source.zip               3,82 MB   (obsoleto, sem hash no nome)
```

Só o ficheiro com o hash de `core-source-lock.json` é usado. Os outros 6 (~23 MB) são lixo
versionado, e cada bump do Core adiciona mais. O `vendor/README.md` tem 0 bytes.

**Solução:** apagar os 6 obsoletos (e o `owntv-core-source.zip` sem hash), e fazer o
`tools/core_snapshot/snapshot_core.py` **apagar** o anterior quando escreve um novo. Documentar
`vendor/README.md`.

---

### REL-09 — 1,78 GB de snapshots do Core acumulados em `.gradle/`

**Severidade: BAIXA (produtividade)** · **Confiança: alta** · **Medido**

```
.gradle/core-snapshot/0bd7b60f…   18,2 MB      .gradle/core-snapshot/65764e6f…  268,2 MB
.gradle/core-snapshot/0c7aafa3…  114,9 MB      .gradle/core-snapshot/6a1300ca…  156,8 MB
.gradle/core-snapshot/1a0507d7…  252,4 MB      .gradle/core-snapshot/7e61750f…  118,4 MB
.gradle/core-snapshot/3544b442…  252,5 MB      .gradle/core-snapshot/b1366e8d…  267,5 MB
.gradle/core-snapshot/38a90319…  260,1 MB      .gradle/core-snapshot/e2081e78…   18,3 MB  ← o actual
                                      TOTAL .gradle: 1 796,5 MB
```

Só 18,3 MB são necessários. **99 % de lixo.** (Já apontado como R4-B25; aqui com a medição exacta
e a lista dos directórios a remover.)

**Solução:** um passo de limpeza (script PowerShell/bash) que apague todos os snapshots cujo hash
≠ o de `core-source-lock.json`, mais activação de `LongPathsEnabled` no Windows (o `git status`
já emite *"Filename too long"*).

---

---

## 4. Segurança e qualidade (agente E4 — verificado por releitura do Core extraído)

> O Core não está neste repositório: é o snapshot SHA-256 `e2081e78…` (1 007 ficheiros) extraído
> de `vendor/owntv-core-source-e2081e78….zip`. Todas as referências de linha abaixo são desse
> snapshot — **byte a byte idêntico ao zip**.

### SEC-01 — Cleartext global + credenciais Xtream/Stalker no path e na query

**Severidade: ALTA** · **Confiança: alta** · **Verificado**

**Manifest** `app/src/main/AndroidManifest.xml:45,49`:
```xml
45:        android:requestLegacyExternalStorage="true"
49:        android:usesCleartextTraffic="true"
```

**Core** `core/parser/XtreamClient.kt:444,453,456,458,516,539,704`:
```kotlin
444:    fun liveUrl(s: SourceEntity, streamId: String) = "${base(s)}/live/${s.username}/${s.password}/$streamId.ts"
453:    return "${base(s)}/timeshift/${s.username}/${s.password}/${durationMinutes}/…"
516:        http.get("${base(s)}/player_api.php?username=$u&password=$p", s.userAgent, headers = headers) {
```

**Impacto:** o URL do painel é usado verbatim — `base(s) = s.url.trimEnd('/')`, sem verificação de
esquema. Com `usesCleartextTraffic="true"`, um painel `http://` significa que **o utilizador e a
palavra-passe da sua subscrição são transmitidos em claro** em cada catálogo, cada EPG e cada
abertura de stream — visíveis a qualquer observador passivo na LAN, registados nos logs de acesso
do proxy/CDN e escritos nos logs do servidor. O formulário "Add Source" aceita `http://` sem
aviso. Para um app *bring-your-own-credentials*, esta é a maior exposição de privacidade possível.

**Solução**
1. `res/xml/network_security_config.xml` com `cleartextTrafficPermitted="false"` por defeito e uma
   `domain-config` por source que o utilizador active explicitamente.
2. Em `AddSourceScreen`/`SetupViewModel.startXtream`, avisar **antes** de gravar: *"Este painel
   receberá a sua palavra-passe em claro"* + botão "usar HTTPS".
3. As credenciais no path são imposição do protocolo Xtream — o que se corrige é o **transporte**.

### SEC-02 — O segredo do edge worker está dentro de todas as APKs

**Severidade: ALTA** · **Confiança: alta**

`app/build.gradle.kts:76-84` → `buildConfigField("String", "TMDB_EDGE_KEY", …)` →
`OwnTVApp.kt:87` → `CoreBuildInfo.edgeKey` → Core `metadata/TmdbProvider.kt:63-71` envia-o como
header `x-owntv-key` em cada pedido de metadados.

O comentário do Core (`TmdbProvider.kt:392-393`) diz *"a Cloudflare edge rule that refuses anything
without the right `x-owntv-key`, so only a build that carries the key can use it"*. **A intenção
não se cumpre:** um `buildConfigField` é uma string literal em `BuildConfig.class` dentro do APK.
`jadx` extrai-a em segundos. O resultado é que a "edge rule" **não é controlo de acesso, é um
speed bump** — e como a chave é derivada por versão a partir de `OWNTV_EDGE_MASTER`
(`android.yml:152-159`), o master está a um `strings` de ser comprometido.

**Solução:** (1) trocar o gate por **atestação por instalação** (Play Integrity API / App Attest)
validada no servidor; ou (2) se a chave estática tiver de ficar (app sideloaded, sem Play), tratá-la
como **identificador público** (`x-owntv-build`), retirar a alegação de segredo eMeter o abuso
server-side pelo id de instalação (`x-owntv-client`) + rate limits. (3) Rodar `OWNTV_EDGE_KEY` e
invalidar `OWNTV_EDGE_MASTER` depois de publicar.

### SEC-03 — O build release cai silenciosamente para a chave de debug

**Severidade: ALTA** · **Confiança: alta**

`app/build.gradle.kts:175-179`:
```kotlin
175:            if (releaseKeystore != null) {
176:                signingConfig = signingConfigs.getByName("release")
177:            } else {
178:                signingConfig = signingConfigs.getByName("debug")
179:            }
```

Não há falha — o build **passa** e produz um "release" assinado com `~/.android/debug.keystore`. Essa
chave: não está protegida por nada, **é diferente em cada máquina** (logo o artefacto não é
upgrade-compatible com uma release publicada), e é o material que qualquer pessoa assina um "debug
build" desta app.

**Solução:** falhar o build em vez de degradar.
```kotlin
} else {
    throw GradleException(
        "No release keystore configured. Set KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD " +
        "or owntv.keystoreFile/… in ~/.gradle/gradle.properties. Refusing to sign a release with the debug key."
    )
}
```

### SEC-04 — O PIN de perfil é um SHA-256 simples, sem lockout nem backoff

**Severidade: MÉDIA** · **Confiança: alta** · **Verificado byte a byte**

Core `util/Pin.kt` (verificado):
```kotlin
13:    fun hash(pin: String): String {
14:        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }
15:        return "${salt.toHex()}:${digest(salt, pin)}"
16:    }
...
26:    private fun digest(salt: ByteArray, pin: String): String =
27:        MessageDigest.getInstance("SHA-256").digest(salt + pin.toByteArray()).toHex()
```

E o diálogo da app (`features/profiles/ProfileGate.kt:94-104`, `ProfileComponents.kt:92-126`)
**não tem contador de tentativas, nem atraso, nem bloqueio**.

**Impacto**

1. **Offline:** `pinHash` é uma coluna normal do Room. Com a BD extraída (`.own` com passphrase
   fraca, device rooteado, `adb backup` em firmware antigo, ou outra app com
   `MANAGE_EXTERNAL_STORAGE` — que o próprio OwnTV pede), 10 000 candidatos × 1 SHA-256 = milissegundos.
2. **Online:** o PIN é a **única** separação entre um perfil adulto e um perfil Kids
   (Core `profile/ProfileManager.kt:44-56` — um perfil novo herda **todas** as sources da BD), e
   gateia `ProfileOperationAuthorization` (trocar PIN, remover Kids, apagar perfil).

**O contraste dentro do mesmo produto é o argumento mais forte:** o servidor LAN companheiro
aplica 10 tentativas + 500 ms de atraso fixo + comparação em tempo constante
(Core `companion/CompanionHttpServer.kt:435,481-489,615-618`). O **PIN on-device, que protege
mais, não aplica nada disso.**

**Solução**
1. `PBKDF2WithHmacSHA256` ≥ 200 000 iterações (ou Argon2id), mantendo o formato `salt:hash` com
   um prefixo de versão (`pbkdf2-sha256$210000$:`).
2. `MessageDigest.isEqual()` em vez de `String ==`.
3. Contador de falhas **persistido** (DataStore, não ViewModel), backoff exponencial, bloqueio de
   5 minutos ao 5.º erro.
4. PIN mínimo de 6 dígitos.

> ⚠️ **Nota de duplicação:** a **ausência de limite de tentativas** já foi registada em R4-B07
> (ronda anterior). O que é **novo** aqui é (a) o KDF ser um único SHA-256 em vez de PBKDF2 —
> o Core já usa PBKDF2-210k nos backups, ou seja, o Core sabe fazer isto — e (b) a comparação em
> tempo constante.

### SEC-05 — Credenciais IPTV, palavra-passe de proxy e chaves API em texto claro

**Severidade: MÉDIA** · **Confiança: alta**

Core `database/entity/ProfileEntities.kt:51-64` — `SourceEntity` guarda `username`, `password`,
`mac`, `stalkerSerialNumber`, `stalkerDeviceId`, `stalkerDeviceId2`, `stalkerSignature` em texto
claro. Core `settings/SettingsRepository.kt:494-507` — `proxy_pass`, `tmdb_api_key`,
`open_subtitles_api_key` em texto claro no DataStore.

`android:allowBackup="false"` não protege contra extracção no dispositivo. E o próprio Core já
**demonstra o padrão correcto duas vezes**: `subtitles/OpenSubtitlesAuthStore.kt:113-127` (Keystore
AES-256-GCM) e `app/features/settings/AutomaticBackupPreferences.kt:94-115` (Keystore AES-256-GCM).
**As credenciais que valem mais do que qualquer uma delas — a conta IPTV paga — não recebem
tratamento nenhum.**

**Solução:** um helper `SecretBox` (Keystore AES-256-GCM) e guardar os campos sensíveis como blobs
selados; materializar o texto claro só em memória, dentro de `XtreamClient`/`StalkerClient`. O
sealing por campo é mais seguro que SQLCipher porque **não muda as assinaturas dos DAOs**.

### SEC-06 — A passphrase do backup não tem força mínima

**Severidade: MÉDIA** · **Confiança: alta**

`app/features/settings/BackupScreen.kt:435-447` — o botão de confirmação só exige
`password.isNotBlank()`. O KDF é bom (Core `backup/BackupCrypto.kt:30-31`: PBKDF2-HMAC-SHA256,
**210 000 iterações**, salt aleatório de 16 bytes) — mas 210 000 iterações não protegem contra
`"1"`.

E o ficheiro resultante contém (Core `backup/BackupManager.kt:165-254`): utilizador e palavra-passe
Xtream/Stalker, identidade do dispositivo Stalker, palavra-passe do proxy, chave TMDB,
utilizador/palavra-passe/token do OpenSubtitles, sessão por perfil e o histórico completo de
visualização. E vai parar a um **dispositivo USB ou a uma pasta partilhada** (`AutomaticBackupWorker.kt:65-79`).

**Solução:** mínimo de 8 caracteres, aviso explícito *"a passphrase não pode ser recuperada"* antes
do ecrã de destino, e um verificador armazenado que permita avisar *"esta passphrase é fraca"*.

### SEC-07 — `requestLegacyExternalStorage` é código morto e `MANAGE_EXTERNAL_STORAGE` é pedido globalmente

**Severidade: MÉDIA** · **Confiança: alta**

`requestLegacyExternalStorage="true"` é **ignorado em API 30+** (o app targetiza 36) — não faz
nada em nenhum dispositivo moderno, e em API 29 **desactiva** o scoped storage, dando leitura e
escrita de todo o volume partilhado. `MANAGE_EXTERNAL_STORAGE` é a permissão de armazenamento mais
ampla do Android e é pedida *app-wide* em vez de no momento da necessidade — sendo que o Core já
tem `StorageAccess.defaultRoot` → `getExternalFilesDir(null)/OwnTV` (Core `storage/StorageAccess.kt:56-58`),
um caminho **sem permissões**.

**Solução:** apagar `requestLegacyExternalStorage`; pedir `MANAGE_EXTERNAL_STORAGE` apenas quando o
utilizador escolhe uma pasta fora do directório externo da app; manter `READ_EXTERNAL_STORAGE
maxSdkVersion="32"` (o comentário nas linhas 18-23 documenta um problema OEM real na TCL Android 12).

### SEC-08 — O `FileProvider` expõe a árvore inteira do armazenamento partilhado

**Severidade: MÉDIA** · **Confiança: alta** · **Verificado byte a byte**

`app/src/main/res/xml/file_paths.xml:8-10`:
```xml
 8:    <external-path name="shared_media" path="." />
 9:    <files-path name="files_root" path="." />
10:    <cache-path name="cache_root" path="." />
```

`external-path path="."` torna **todo o volume partilhado** endereçável por
`content://tv.own.owntv.fileprovider/shared_media/<qualquer>`. O provider é `exported="false"`, mas
`grantUriPermissions="true"` + `FLAG_GRANT_READ_URI_PERMISSION` no `ACTION_VIEW` do
`ExternalPlayerLauncher` (Core `player/ExternalPlayerLauncher.kt:62-64,135-140`) entregam um
*grant* a uma app de terceiros. Hoje o único call site é seguro, mas a configuração torna o provider
uma **primitiva universal de leitura de ficheiros** para qualquer call site futuro cujo caminho
venha de conteúdo remoto.

**Solução:** restringir a `Download/OwnTV/`, `Movies/OwnTV/`, `Android/data/tv.own.owntv/files/`, e
em `uriFor` **recusar** qualquer ficheiro fora dessas raízes (falhar fechado).

### SEC-09 — O APK de actualização não é verificado criptograficamente

**Severidade: MÉDIA** · **Confiança: alta**

Core `update/UpdateManager.kt:244-256` — `verifyApk` verifica **três** coisas: parseabilidade, ABI
nativa e `packageName`. **Nenhuma verificação de integridade**: sem SHA-256 do artefacto esperado,
sem comparação do certificado de assinatura com o da app instalada.

Em fairness, o `PackageInstaller` do Android exige que uma actualização seja assinada com a mesma
chave — isso é a rede de segurança **da plataforma**, não da app, e não protege contra uma chave de
release comprometida (caso em que um APK adulterado passa o `verifyApk` limpo e instala por cima
da app do utilizador, com todas as credenciais em texto claro).

**Solução:** publicar `OwnTV.apk.sha256` nas releases e comparar antes de abrir o instalador;
comparar o certificado do APK com o da app instalada; exigir `versionCode` ≥ o actual.

### SEC-10 — O servidor companheiro LAN transporta PIN, segredo de pareamento e backup em HTTP simples

**Severidade: MÉDIA** · **Confiança: alta**

Core `companion/CompanionHttpServer.kt:138-140` — `socket.bind(InetSocketAddress(port))` = todas as
interfaces, HTTP simples. O PIN de 6 dígitos (mostrado no ecrã e no QR) e o segredo de pareamento de
256 bits viajam em claro. Em Wi-Fi hostil, um sniffer passivo colhe o PIN e pode descarregar
`/backup.own` e fabricar um segredo de pareamento que autoriza **escrita autenticada** na box.

As defesas internas são boas (comparação em tempo constante, 10 tentativas, atraso de 500 ms) — o
problema é o **transporte**.

**Solução:** TLS com certificado auto-assinado por sessão, com o SHA-256 da impressão digital no QR
(é o padrão de UX de "pairing code"); expirar o PIN após 5 minutos mesmo por usar.

### SEC-11 / SEC-12 — Avaliações: deep link e beacon de instalação

**Severidade: BAIXA** · **avaliado, maioritariamente sólido**

- **Deep link:** `AndroidManifest.xml:97-107` expõe `owntv://play` e `owntv://open` como
  `BROWSABLE` — **qualquer página web** pode iniciar a reprodução. O parsing
  (Core `launcher/LauncherDeepLink.kt:68-102`) é allow-listed a 4 rotas e todos os escalares passam
  por `toLongOrNull()`/`toIntOrNull()` — **não há superfície de injecção**. O único residuo é
  *nuisance* (reprodução indesejada). *Repetição de R4-B13.* **Recomendação adicional:** mover o
  filtro `owntv://` para uma `DeepLinkActivity` própria, para que a `MainActivity`leve só o filtro
  de launcher.
- **Beacon:** Core `metadata/OwnTVClientId.kt:30-62` gera um id aleatório de 128 bits por instalação
  (sem `ANDROID_ID`, sem ids de hardware — bem pensado) e envia-o como `x-owntv-client`. **O
  utilizador não o pode ver nem repor, e não existe aviso de privacidade na app.** **Solução:**
  mostrar o id + "repor id de instalação" em Definições → Metadados, e oferecer opt-out que force
  o tier 1/2 (chave TMDB própria ou worker auto-hospedado) — que o app já suporta.
- **`AutoStartService` (AccessibilityService):** **bem delimitado e defensável.**
  `res/xml/auto_start_service.xml:4-9` — `packageNames="tv.own.owntv"` (observa só o seu próprio
  pacote), `canRetrieveWindowContent="false"`, `accessibilityEventTypes="typeWindowStateChanged"`;
  `AutoStartService.kt:95-96` — `onAccessibilityEvent` e `onInterrupt` são no-ops; e só arranca a
  **si própria** `MainActivity`, com ecrã interactivo, keyguard desbloqueado e opt-in do utilizador
  (`SimpleModeOptions.startOnBoot/startOnWake`, default `false`). **Uma app maliciosa não o pode
  usar.** A ressalva é o **nível de privilégio**: um grant de acessibilidade é dos mais sensíveis que
  um utilizador pode dar, e não é estritamente necessário para "arrancar no boot" — um
  `BOOT_COMPLETED` + `AlarmManager` bastava (o serviço já usa `Settings.Global.BOOT_COUNT` para
  detectar reinícios). **Recomendação:** substituir oAccessibilityService por um par de receivers
  `BOOT_COMPLETED`/`USER_PRESENT`, e adicionar explicitamente
  `android:canPerformGestures="false"` ao XML enquanto ele existir.

### 4.1 Testes e qualidade (QA)

| ID | Sev. | Achado | Onde | Confiança |
|---|---|---|---|---|
| **QA-01** | 🟠 Alta | O CI **nunca corre os `androidTest`** — 19 classes nunca executam | `android.yml:51-54` | Alta |
| **QA-02** | 🟠 Alta | **Zero** SAST / scan de dependências / scan de segredos / SBOM / Dependabot | `.github/` | Alta |
| **QA-03** | 🟠 Alta | O CI pode publicar um APK **debug-assinado e debuggable** como `OwnTV.apk` | `android.yml:193-202, 218-219` | Alta |
| **QA-04** | 🟡 Média | **Não existe `PinTest`** — o único guard do gate parental não tem teste | Core `src/test/…/util/` | Alta |
| **QA-05** | 🟡 Média | **Não existe teste de round-trip export→encrypt→restore** | app `src/test/…/settings/` | Alta |
| **QA-06** | 🟡 Média | **Nenhum teste assegura a *prioridade* da escada de fallback** — cada rung é testado isolado | `player-core/src/test/…/LiveLadderTest.kt` | Alta |
| **QA-07** | 🟡 Média | `UpdateManager.verifyApk` — o único guard pré-instalação — **não é testado** | Core `update/UpdateManager.kt:244` | Alta |
| **QA-08** | 🟡 Média | O baseline profile é gravado de uma fixture local e **nunca validado no CI** | `baselineprofile/…/BaselineProfileGenerator.kt` | Alta |
| **QA-09** | 🟡 Média | O `mapping.txt` do R8 **nunca é arquivado** → crashes de release indecifráveis | `android.yml:228-234` | Alta |
| **QA-10** | 🟢 Baixa | Sem infra de teste Compose/Robolectric; `isReturnDefaultValues` mascara APIs Android | `app/build.gradle.kts:151-155` | Alta |
| **QA-11** | 🟢 Baixa | Memória do runner do CI vs `kotlin.daemon.jvmargs=-Xmx8192m` num runner de 7 GB | `gradle.properties:9-29` | Média |

**Detalhe dos mais importantes:**

**QA-01 — 19 classes de teste instrumentado nunca executam.** Incluindo
`core/src/androidTest/…/backup/BackupContainerTest.kt` (o round-trip real do contentor `.own`) e
`core/src/androidTest/…/parser/XtreamClientTest.kt` (o cliente HTTP que monta as URLs com
credenciais de SEC-01). O próprio Core reconhece isto — `core/src/test/…/backup/BackupAtomicWriteTest.kt:16-17`:
*"its round-trip is covered on-device by `BackupContainerTest`"* — **um teste que o CI nunca corre**.
A causa é estrutural: `app/build.gradle.kts:433-434` só declara `androidx-junit` + `espresso-core`
(não `rules`, não `core-ktx`, não `compose.ui.test.junit4`).
**Solução:** um job com `reactivecircus/android-emulator-runner` ou Gradle Managed Devices.

**QA-02 — superfície de ataque sem rede de segurança.** O app distribui um AccessibilityService, um
servidor HTTP LAN que aceita submissões de credenciais, um mecanismo de auto-actualização que
instala APKs, declara `MANAGE_EXTERNAL_STORAGE` e `REQUEST_INSTALL_PACKAGES`, e é **sideloaded** —
logo **não recebe** Play Protect, dependency scanning, Data Safety review nem reporte automático de
CVE. Nada disso existe aqui. `gradle/libs.versions.toml` fixa ~30 versões à mão, sem automação.
**Solução:** `.github/dependabot.yml`, `gradle/verification-metadata.xml` (verificação de
dependências), `dependency-review-action`, `osv-scanner` + SBOM, `gitleaks`, `codeql.yml`,
`detekt`/`ktlint`.

**QA-03 — uma "release" debuggable pode chegar a todos.** Sem `KEYSTORE_BASE64`, o workflow **não
falha**: faz `assembleDebug` e, num build de tag, publica esse artefacto como `dist/OwnTV.apk` — o
mesmo URL estável que o código do Downloader do Fire TV e o link "always-latest" apontam. Um APK
debug tem `android:debuggable="true"`: qualquer pessoa com acesso físico ao device (ou com `adb`)
liga um debugger e corre `run-as tv.own.owntv` para ler os ficheiros privados — **onde vivem as
credenciais IPTV em texto claro** (SEC-05).
**Solução:**
```bash
if [ "$IS_TAG" = "true" ] && [ -z "$KEYSTORE_FILE" ]; then
  echo "::error::A tagged release requires KEYSTORE_BASE64. Refusing to publish a debug-signed APK."
  exit 1
fi
```
+ asserção pós-build com `apksigner verify --print-certs`.

**QA-09 — os crashes de release são indecifráveis.** `CrashRecorder` é instalado no startup
(`OwnTVApp.kt:99`) precisamente para deixar rasto, mas um stack trace de uma build minificada sem
`mapping.txt` é uma mancha de `a.b.c`. E o R8 **remove os `Log.d`** (`proguard-rules.pro:26-31`),
ou seja, o log de diagnóstico também desaparece. **Solução:** upload do `mapping.txt` como artefacto
**e** anexado à release, para ser recuperável anos depois.

### 4.2 Verificado e correcto (para que se saiba o que foi testado, não esquecido)

| Verificação | Resultado |
|---|---|
| `allowBackup="false"` vs `backup_rules.xml` / `data_extraction_rules.xml` | **Consistente.** `data_extraction_rules.xml` fecha correctamente o canal de **device-transfer**, que o `allowBackup="false"` *não* cobre em Android 12+. |
| Componentes exportados | Só `MainActivity` (tem de ser exportada) e `AutoStartService` (correctamente protegida por `BIND_ACCESSIBILITY_SERVICE`). `FileProvider` e `InitializationProvider` são `exported="false"`. |
| Fuga de credenciais em logs | **Limpo.** 62 call sites de `Log.*` no app; nenhum interpola password/token/PIN/MAC. O Core tem `HttpClient.redactUrl()` / `diagnosticOrigin()` que mascara `user=`, `pass=`, `token=`, `signature=`, `key=` e segmentos `/live/user/pass/`. |
| Cifra do backup | **Correctamente implementada.** AES-256-GCM sobre o contentor inteiro, PBKDF2-210k, salt de 16 bytes, IV novo por cifra, `WrongPasswordException` **antes** de qualquer escrita; defesas contra zip-bomb e path traversal presentes e limitadas. |
| Sessões Stalker | **Memória apenas** — *"Tokens live in memory only — never persisted"* (Core `stalker/StalkerAuthManager.kt:34`). |
| Credenciais do source draft | **Em memória apenas**, com teste (`SourceDraftViewModelTest.kt`). |
| Modo Kids | Aplicado no **caminho de reprodução**, não só na lista (Core `AdultCategoryClassifier.allows(...)` antes de afinar). |
| Grant de autorização de perfil | Uso único, process-local, expira em 120 s, invalidado se o `pinHash`/`isKids` mudarem. **Sólido.** |
| Gate de lint | `abortOnError = true` + `fatal += "PluralsCandidate"`. |
| Checks estáticos de i18n | Ratchet de literais, formatação numérica locale-stable e check de overflow — **fortes**, e um padrão que devia ser replicado noutras áreas. |
| Volume de testes | 76 ficheiros no app + 242 no Core + 100+ no player-core ≈ 30 000 linhas de teste. A **largura** é boa; a **profundidade** nos caminhos de risco é que falta (QA-01/04/05/06/07). |

### 4.3 Auditoria aos Cloudflare Workers (`extras/worker/`) — custo e segurança

**`extras/worker/tmdb/index.js`** — cache de 180 dias para metadados, 15 min para trending, janelas
stale de 365 dias/24 h, um retry em 429/5xx, fallback stale durante outage. Custo **negligível** (o
TTL de 180 dias significa que um filme é buscado do TMDB ~uma vez por PoP).

**Dois achados:**

1. **A cópia publicada NÃO tem autenticação.** O `extras/worker/README.md:19-25` diz que a
   implantação pública acrescenta "request budgets and client checks" que são *"deliberately not
   published"*. Ou seja: **a cópia no repositório é um proxy aberto sobre a chave do mantenedor** —
   qualquer pessoa que descubra o URL queima a quota TMDB dele. A correção é de 30 segundos: exigir
   um segredo `SHARED_KEY` e **falhar fechado** quando ausente (a cópia já falha fechado se
   `TMDB_KEY` faltar, `:23-25`).
2. **O 429 é tratado como retryable mas o `Retry-After` é truncado a 2 s** (`:144,149-150`). O 429
   do TMDB traz um `Retry-After` mais longo; um self-hoster num plano com rate-limit pode ser
   banido do IP pelo Worker a retentar dentro do cooldown do servidor.

**`extras/worker/opensub/index.js`** — allowlist do host upstream correcta (`:30,51-54` — **não** é
proxy aberto para hosts arbitrários), cache de 6 h só para `GET` anónimo (`:26-27,58-69`).

**Mais três achados:**

3. **Sem autenticação nem rate limit próprios** — proxy aberto que gasta a quota do mantenedor e
   pode colocar a chave em incumprimento dos termos do OpenSubtitles.
4. **O corpo do pedido é carregado em memória sem limite de tamanho** (`:88`) — um cliente hostil
   pode exhausting a memória de 128 MB do Worker. **Fix:** limitar `Content-Length` e ler o corpo
   com tecto (nenhum pedido legítimo passa de 2 MB).
5. **O header `Authorization` é encaminhado verbatim** (`:80-81`) — necessário, mas torna o Worker
   um **relay transparente de bearer token**: uma chave comprometida é uma conta OpenSubtitles
   comprometida para todos os utilizadores da implantação pública.

**Sobre o `OWNTV_EDGE_KEY`:** ser preciso — é uma **chave partilhada de build**, não um segredo.
A sua única função legítima é dar ao Worker uma **identidade de medição** para budgets por
instalação. A identidade que deveria mandar é o `x-owntv-client` (id por instalação) — o que
significa que a chave partilhada **pode ser rodada sem quebrar instalações existentes**. Tratar
como *rate-limit identity*, nunca como autenticação.

---
---

## 5. Bugs de dados, performance e memória (agente E3)

### 5.1 Severidade alta

#### DAT-01 — `GroupedChannelPagingSource` materializa **todo** o catálogo e repete em cada invalidação

**`features/live/GroupedChannelPagingSource.kt:22-52`** · **`features/live/LiveViewModel.kt:937-951`**

```kotlin
override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ChannelEntity> = withContext(Dispatchers.IO) {
    mutex.withLock {
        if (snapshot == null) {
            val rows = LinkedHashMap<String, ChannelEntity>()
            var request: LoadParams<Int> = LoadParams.Refresh(null, 500, false)
            while (true) { ... delegate.load(request) ... }   // lê TODAS as páginas
            snapshot = rows.values.toList()
            onReady(snapshot!!)
        }
```
```kotlin
init { delegate.registerInvalidatedCallback { invalidate() } }   // linha 23
```

**Impacto**
1. **A primeira página que o utilizador vê está bloqueada atrás de uma varrimento completo**: 50 000
   canais ÷ 500/página ≈ **100 viagens SQL**, e 50 000 `ChannelEntity` + 50 000 chaves de grupo
   alocados, **antes** de a `LoadResult.Page` ser devolvida. Tudo dentro de `mutex.withLock`, pelo
   que um prepend/append concorrente bloqueia atrás.
2. `registerInvalidatedCallback { invalidate() }` significa que **cada invalidação** do Room recria o
   `PagingSource` → **a varrimento de 50k repete-se**. Um sync de playlist escreve em batches, pelo
   que isto pode correr dezenas de vezes seguidas.
3. `preferred = { previous, next -> ChannelVersionPolicy.ordered(...) }` (`LiveViewModel.kt:944`) corre
   um `Regex` por par duplicado — **100 000+ avaliações de regex** no primeiro load.
4. `groupedCount.value = null` (linha 936) só é republicado pelo `onReady` **depois** da varrimento
   completa → o rótulo "N canais" lê **0** durante todo o processo.

**Solução:** paginar sobre o delegate e manter o mapa de grupo incrementalmente (chave → canal
representante), para que só a janela carregada exista em memória. Se o snapshot completo for
necessário, indexá-lo por uma **revisão de conteúdo** (sourceIds + versão das linhas) e reutilizá-lo
em vez de re-varrer. Publicar o `groupedCount` na mesma passagem.

#### DAT-02 — 17 flows `Eagerly` constroem os dois maiores ViewModels no primeiro frame

**`features/live/LiveViewModel.kt`** (17× `Eagerly`: linhas 480, 486, 490, 494, 509, 515, 546, 571,
580, 589, 757, 791, 1105, 1927, 2039, 2338, 2572) · **`features/shell/OwnTVShell.kt:255,263`** ·
**`MainActivity.kt:124`**

`LiveViewModel` é **Activity-scoped**, logo é construído durante a primeira composição do shell — no
caminho crítico do cold start, **antes** de o utilizador abrir o Live TV. As 17 cadeias `Eagerly`
arrancam imediatamente trabalho de base de dados/DataStore que ninguém está a ler:
`ctx` → `activeProfileSources` → `sourceDao.observeForProfile`; `folderContextKeys` → `categoryDao.observe`;
`orderedContexts` → `contentOrderDao.observeContextKeys`; `custom` → `customize.observe`;
`favoriteIds` → `favoriteDao.observeFavoriteIds`; mais 6 leituras de DataStore.

`EpgViewModel` acrescenta 8. Cada um é um `Flow` vivo do Room que **re-consulta em cada
invalidação** durante toda a sessão, esteja a secção visível ou não.

**O próprio codebase já sabe que este padrão é caro:** `HomeViewModel.kt:139-175` documenta que *"two
full loads — ~15 dependent queries each — on the database concurrently at the single most contended
moment of launch"* foi *"the single largest delay on the screen"*, e resolveu-o com um
`MutableSharedFlow` coalescente + `collectLatest`. **O `LiveViewModel`/`EpgViewModel` não receberam
o mesmo tratamento.**

**Solução:** converter os flows **de base de dados** `Eagerly` para `WhileSubscribed(5_000)`; manter
`Eagerly` só para as flags minúsculas de DataStore que um caminho de reprodução lê **sincronicamente**
(os comentários em `LiveViewModel.kt:482-490,511-515` dizem exactamente isso). Alternativamente,
construir `LiveViewModel`/`EpgViewModel` preguiçosamente por secção (o padrão
`rememberLazyShellViewModel` já usado para Movies/Series em `OwnTVShell.kt:247-248`).

#### DAT-03 — O paging do Live TV retém **todos** os canais carregados para sempre

**`features/live/LiveViewModel.kt:927-933`**

```kotlin
Pager(
    PagingConfig(
        pageSize = 80, prefetchDistance = 40, initialLoadSize = 300,
        maxSize = PagingConfig.MAX_SIZE_UNBOUNDED,   // <-- nunca descarta uma página
    ),
) { ... }
```

É o **único** dos três catálogos que nunca liberta páginas: `SeriesViewModel.kt:527` e
`MovieViewModel.kt:419` usam ambos `maxSize = 300`. A lista Live é a **maior** tabela da app (50k).
Deslocar a lista "Todos" até ao fim retém cada `ChannelEntity` (name, logoUrl, streamUrl,
httpHeaders, drmConfig, catchup, sortOrder) no snapshot do `PagingSource` — **15-30 MB de heap**
num dispositivo cujos próprios comentários dizem que o processo foi *"LMK-killed on real TVs"*
(`MainActivity.kt:149-150`).

**Solução:** `maxSize` limitado (600–1000) + `placeholdersEnabled = true`.

#### DAT-04 — O backup automático nunca vai para foreground: é morto a meio e o ficheiro é apagado

**`features/settings/AutomaticBackupWorker.kt:50-102`**

```kotlin
override suspend fun doWork(): Result = operation.withLock {
    ...
    val exported = GlobalContext.get().get<BackupManager>().export(stage,
        BackupManager.Section.entries.toSet(), config.password, config.profiles.keys).getOrThrow()
    ...
} catch (_: Exception) { ... }
finally {
    staging?.let { stage -> withContext(NonCancellable) { ... stage.deleteRecursively() } }  // linha 99
}
```

`doWork()` **nunca chama `setForeground(...)`**. O WorkManager **para** um worker não-foreground
depois do limite de execução (10 minutos) e reagenda-o; a corrotina é cancelada, o `finally` corre,
e `stage.deleteRecursively()` **deita fora a exportação parcial**. O worker faz retry, é parado no
mesmo ponto, e o ciclo repete-se para sempre. `BackupManager.export` é chamado com
`Section.entries.toSet()` (todas as secções) e todos os perfis — num catálogo de 220k itens isso são
minutos a dezenas de minutos. O utilizador só vê *"Automatic backup failed"*.

**Solução:** `setForeground(ForegroundInfo(...))` como primeira instrução, com o tipo de
foreground-service `dataSync` declarado no manifest; e tornar o trabalho **retomável** (exportar
secção a secção, com checkpoint no staging), para que cada execução faça progresso real.

### 5.2 Severidade média

| ID | Achado | Localização | Impacto | Solução |
|---|---|---|---|---|
| **DAT-05** | `throttleLatest()` está **dentro** do `flatMapLatest` → o throttle é anulado | `LiveViewModel.kt:1094-1096`; `SeriesViewModel.kt:550-552`; `MovieViewModel.kt:442-444` | O `throttleLatest` do Core é *leading-edge* e só guarda o valor anterior **dentro de uma colecção**. O `flatMapLatest` cancela o interior e arranca um **novo** `throttleLatest` sem histórico → o primeiro valor de cada reinício sai imediatamente. Como o `combine` inclui `custResolved`, **cada** hide/rename/visibilidade dispara um `COUNT(*)` sobre 50 000 linhas sem qualquer throttling. O comentário ("cap live COUNT re-runs") descreve um limite que não existe. | Mover o throttle **para fora** do switch: `.flatMapLatest { … }.throttleLatest()` |
| **DAT-06** | `CoroutineScope(Dispatchers.IO)` **desestruturado** dentro do ViewModel | `LiveViewModel.kt:190-196` | Cada `disposeLocalTimeshift()` (em cada mudança de canal, `onBackground()`, `onFullscreenExited()`, `clearLiveOnExo()`, `stopPreview()`, `onCleared()`) aloca **um scope e um Job novos** que nunca são cancelados → **um scope vazado por chamada, para a vida do processo**. E como o Job não é filho do `viewModelScope`, o `onCleared()` **não o pode cancelar** — continua a fechar a sessão HTTP do timeshift local depois de a Activity já não existir. | Um `cleanupScope` criado **uma vez**, cancelado em `onCleared()`; ou `viewModelScope.launch(Dispatchers.IO)`. |
| **DAT-07** | **N+1** em `DownloadsViewModel.isHidden` (até 3 queries por linha) | `DownloadsViewModel.kt:63-88` | Num perfil Kids, **cada** linha de `downloadDao.observeForProfile` dispara 2-3 viagens DAO. 300 downloads ⇒ ~700-900 queries **por emissão** — e `observeForProfile` reemite em cada tick de progresso de um download multi-GB. | Uma query em lote que devolva os campos relevantes (movie/series + categoria) e filtrar em memória. |
| **DAT-08** | `sourceExpiry` re-sonda o provider em cada emissão e **nunca cacheia a falha** | `SettingsViewModel.kt:201-234` | `fetchExpiry` é uma **chamada de rede**. Se devolve `null` (panel em baixo, credenciais rejeitadas), **nada é cacheado** → a próxima emissão volta a pedir. `sources` é um Flow do Room que reemite em qualquer escrita na tabela (um sync escreve `maxConnections`, `syncLive`, timestamps) ⇒ tempestade de pedidos. Agrava: a cache é indexada só por `sourceId` (nunca invalidada quando as credenciais mudam) e o `map` corre no Main. | Cachear negativos com TTL curto; indexar por `(sourceId, credentialRevision)`; mover a sondagem para `Dispatchers.IO`; `distinctUntilChanged()` na lista. |
| **DAT-09** | O guia EPG retém **20 000 `ChannelEntity` completos** num `StateFlow` de vida longa | `EpgViewModel.kt:1071,1133,1171,1266` | Durante `runLoad` coexistem três listas de 20 000 (`rawChannels`, `channels`, `boundedChannels`), cada uma com entidades **completas** (streamUrl, httpHeaders, drmConfig…). A final fica em `_state` de um `EpgViewModel` Activity-scoped → residente enquanto o Guia estiver no back stack. O grid só pinta ~8 linhas (`GuideViewport.kt`) e as linhas já carregam os programas de forma lazy (`EpgViewModel.kt:474-498`) — **a lista completa não compra nada.** | Paginar a lista de canais do guia, ou guardar só uma projecção (`id, name, sourceId, categoryId, epgChannelId, catchup, number`). |
| **DAT-10** | `epgFallbackCandidates` faz **uma query por candidato** | `EpgViewModel.kt:749-758` | `hasProgrammeInWindow` é uma query por id contra **a maior tabela da app** (programas EPG) numa janela de 38 dias, executada uma vez por candidato (que vem de um `searchList(..., 2_000)`). | Uma query em lote: `epgDao.hasProgrammesInWindow(ids, from, to): Set<String>`. |
| **DAT-11** | `WhileSubscribed()` (0 ms) combinado com um `delay(3 s)` ⇒ painel de detalhes **vazio** ao reentrar | `LiveViewModel.kt:677-689`; `SettingsViewModel.kt:267-268` | O `stopTimeoutMillis` por omissão do `WhileSubscribed()` é **0**, pelo que todo o upstream (incluindo o ticker de 1 minuto e o `delay` de 3 s) é **desmontado no instante em que o último subscritor sai** e recomeça do zero. O collector da linha em foco (`LiveScreen.kt:1121-1126`) re-subscrive a cada entrada na secção Live ⇒ o cartão "agora / a seguir" **reinicia o delay de 3 s e não mostra nada**. | `WhileSubscribed(5_000)`; e aplicar o `liveGuideDelayMs` **só** ao caminho de fallback do provider, não a uma leitura já cacheada. |
| **DAT-12** | `versionGroupKeys` é uma cache **write-only** de 50 000 entradas | `LiveViewModel.kt:1321` (decl.), 947-948, 1326-1327, 1876 | Uma grep por `versionGroupKeys` em toda a app encontra **só** estes 4 pontos de escrita e **nenhuma leitura** — é estado morto. E é mantida à escala: escreve uma entrada por canal do catálogo inteiro, e **limpa-se e refill** cada vez que aicansa os 20 000 → churn puro de alocação, para nada, num dispositivo com pouca memória. | Apagar o campo e os 4 pontos de escrita. |
| **DAT-13** | Leitura **síncrona** de SharedPreferences no main thread durante `attachBaseContext` | `MainActivity.kt:135-138`; `OwnTVApp.kt:62-66,73-77` | `preferences.getString` num SharedPreferences ainda não carregado força uma **leitura de ficheiro + parse de XML no main thread**. A cópia do `OwnTVApp` corre uma vez por processo; a do `MainActivity` corre em **cada criação de Activity** — incluindo cada `Activity.recreate()`, que é o que a app faz numa troca de locale/factor de fonte. Em eMMC lento, isso é uma fatia mensurável do orçamento de splash de 2 s (`MainActivity.kt:93`). | Ler a tag **uma vez** no `OwnTVApp.attachBaseContext` e cacheá-la num `@Volatile` do Application. |
| **DAT-14** | Worker diário para uma política **trimestral/semestral/anual**; `KEEP` descarta mudanças de constraints | `AutomaticBackupWorker.kt:21-45`; `AutomaticBackupPolicy.kt:9-16` | A política é em meses (`plusMonths`), mas o trabalho é agendado **de 24 em 24 horas**; o worker faz `Result.success()` em ~364 dos 365 dias. Cada um é um wake-up do WorkManager, um arranque de processo, uma leitura de DataStore e um toque no `KeyStore` — custo de bateria puro para um backup mensal. Pior: `ExistingPeriodicWorkPolicy.KEEP` significa que quando o utilizador muda o intervalo **ou as constraints, o pedido novo é descartado** — as novas constraints nunca entram em vigor, e o utilizador **acredita que a mudança foi aplicada**. | `ExistingPeriodicWorkPolicy.UPDATE`; ou agendar o trabalho no período real da política. |

### 5.3 Severidade baixa

| ID | Achado | Localização |
|---|---|---|
| **DAT-15** | `runNow` usa `ExistingWorkPolicy.KEEP` num job disparado pelo utilizador → **o toque é engolido em silêncio**; e o pedido one-time não tem `setBackoffCriteria` (default 30 s em vez da política de 1 h) | `AutomaticBackupWorker.kt:39-44`; `AutomaticBackupControls.kt:40,89-91` |
| **DAT-16** | Loops de polling livres de 10 s em `MovieViewModel`/`SeriesViewModel` — ~8 640 despertares/dia para um save que só importa enquanto se reproduz | `MovieViewModel.kt:343-348`; `SeriesViewModel.kt:371-376` |
| **DAT-17** | `SeriesViewModel.episodes` é `Eagerly` → a query de episódios da série aberta fica viva toda a sessão e re-consulta em cada batch de sync | `SeriesViewModel.kt:559-561` |
| **DAT-18** | `CustomizeItemsViewModel` encadeia **dois `cachedIn`** e transforma o *mesmo* `PagingData` — exactamente o que o Paging proíbe (re-collect); só funciona por causa do `cachedIn` interno | `CustomizeItemsViewModel.kt:138-167` |
| **DAT-19** | `MultiviewState.cleanupScope` — um `SupervisorJob` **nunca cancelado**; abrir/fechar o grid acumula scopes | `MultiviewState.kt:93,238-249` |
| **DAT-20** | O mapa "now playing" é **copiado duas vezes** por publicação (até 2 000 entradas) e **cada linha visível cria o seu próprio collector** → uma publicação acorda 60+ collectors | `NowPlayingTitleLoader.kt:53,81`; `LiveViewModel.kt:2757-2759`; `LiveScreen.kt:1121-1126` |
| **DAT-21** | `ProgrammeReminderStore.dismissedKeys` é um `LinkedHashSet` **sem limite** que cresce também por auto-consumo | `ProgrammeReminderStore.kt:47,96-101,143` |

### 5.4 Verificado e limpo (dados/performance)

- **Zero** `GlobalScope`, `runBlocking`, `Thread.sleep` em toda a árvore.
- **Zero** `launch` dentro de um composable sem `rememberCoroutineScope` (45 ocorrências, todas correctas).
- Todos os pontos de entrada do Core (`BackupManager`, `RecordingManager`, `SearchReader`,
  `HomeFeedReader`, `GuideReader`) fazem `withContext(Dispatchers.IO)` correctamente.
- As guardas de geração de `TuneSelection` / `CatchupOwner` / `EpgEditSession` /
  `OrderDraftCommitter` estão **correctamente implementadas e consistentemente usadas**.
- 45 Collecting-sites de `Dispatchers.IO`, 3 `Main`, 2 `Default`, 1 `Unconfined` — a distribuição está
  certa.

---

## 6. Build, arquitectura e i18n (agente E5)

> As alegações de comportamento do AGP foram verificadas contra as **fontes do AGP 9.4.1** no cache
> local (`gradle-9.4.1-sources.jar`), não de memória.

### F4 — 🔴 266 strings da app traduzidas em apenas **2 dos 26** idiomas anunciados

**Severidade: CRÍTICA (utilizador) · Confiança: alta · Verificado por contagem directa**

| Resource set | `<string>` contados |
|---|---|
| `app/src/main/res/values/` (default / EN) | **266** |
| `app/src/main/res/values-pt/` (pt-BR) | 170 — **faltam 96 das 266** |
| `app/src/main/res/values-pt-rPT/` (pt-PT) | 233 — faltam 33 |
| **todos os outros 23 directórios de locale** | **0** — fallback para inglês |

O `tools/i18n/locales.json` marca **26** qualifiers como `packaged = true` (`en, en-rGB, ar, pt,
pt-rPT, zh-rCN, zh-rTW, cs, da, nl, fr, de, it, ja, ko, nb, pl, ru, es-rUS, es, sv, tr, ml, hi, bn, hu`),
e `app/build.gradle.kts:196` aplica-os como `localeFilters` — o sistema operativo é informado de que
a app fala 26 idiomas. **Para 24 deles, todas as strings do nível app estão em inglês.**

**Três coisas escondem isto:**
1. `tools/i18n/README.md:3-4` diz *"The strings themselves are not in this repository"* — verdade
   para as strings do Core, **falso para as 266 do módulo app**.
2. `tools/i18n/check_hardcoded_strings.py:33-37` varre **apenas literais Kotlin**
   (`SRC_ROOTS = app/src/main/java, core/…, player-core/…`). Nunca abre `res/values*/`.
3. `app/build.gradle.kts:216` — `disable += "MissingTranslation"` — remove **o único check do
   Android lint** que teria apanhado isto. O comentário justifica-o para as strings do Core em
   `en-rGB`, mas o disable é **module-wide** e silencia também a lacuna do próprio app.

**Impacto no utilizador:** um utilizador chinese, japonês, coreano, árabe, russo, alemão, francês,
italiano, espanhol, hindi, etc. recebe a app do Core **traduzida** mas com **todos os botões,
diálogos e títulos de definições em inglês**. Para um app que orgulha-se de 26 idiomas, é um
contrato de produto quebrado — e ninguém no CI o pode ver.

**Solução (duas opções):**
1. **Migrar as 266 strings do app para o Core** (a arquitectura que o README já descreve), e o
   workflow de i18n do Core passa a cobri-las. É a solução correcta mas implica um bump do lock.
2. **Escrever um verificador de cobertura de resources** (~40 linhas de Python, o análogo do ratchet
   já existente): comparar o conjunto de chaves de `res/values/` contra cada locale empacotado e
   falhar o build. **Independentemente disto, remover `disable += "MissingTranslation"`** e passar a
   usar `tools:ignore` por excepção, não um disable global.

### F3 — 🟠 `splits.abi` × `ndk.abiFilters` por flavor produz **3 APKs por flavor**, e o CI publica um ao acaso

**Severidade: ALTA · Confiança: alta** · **Verificado contra as fontes do AGP 9.4.1**

| Fonte do AGP | Comportamento |
|---|---|
| `variant/BaseVariantData.kt:108` | `abiFilters = getFilters(DiscoverableFilterType.ABI, splits)` → o conjunto de ABIs do variant é **apenas** o de `splits.abiFilters`. **Nunca** é intersectado com o `ndk.abiFilters` do flavor. |
| `ApplicationVariantFactory.kt:196-230` | `populateMultiApkOutputs(abis, …)` → uma output por ABI desse conjunto **mais** a universal. |
| `ApplicationVariantFactory.kt:203-205` | `if (globalConfig.splits.abi.isUniversalApk) return` — **`isUniversalApk = true` é a única razão de a configuração contraditória não dar erro duro** (senão o AGP levanta `CONFLICTING_ABI_CONFIG`: *"ndk abiFilters cannot be present when splits abi filters are set"*). |
| `tasks/PackageAndroidArtifact.kt:1004-1005` | `getAcceptedAbis()` toma a ABI do **split**, não a do flavor. |

Ou seja: para o flavor `x86_64` (`abiFilters = ["x86_64"]`, `app/build.gradle.kts:104-107`) o AGP
emite **três** outputs: a universal **mais** `app-x86_64-armeabi-v7a-release.apk` e
`app-x86_64-arm64-v8a-release.apk` — dois APKs com **nome de arm** num flavor explicitamente
x86_64.

E o CI faz (`.github/workflows/android.yml:188`):
```bash
X86_SRC=$(ls app/build/outputs/apk/x86_64/release/*.apk | head -n1)
```
`head -n1` sobre um glob de três é **não-determinístico** (ordem do directório), e o resultado é
publicado como `OwnTV-x86_64-v<version>.apk` (linhas 222-225, 279). **Um utilizador numa box x86_64
pode receber um APK só com arm** — que morre com `UnsatisfiedLinkError` no `libmpv` ao arrancar.

**Solução (recomendada):** **apagar `splits.abi`** (`app/build.gradle.kts:110-117`). A *dimensão* de
flavor `abi` já faz a partição, e o `ndk.abiFilters` por flavor já restringe a universal. Sem splits,
cada flavor dá exactamente um APK. Alternativamente, fixar o CI num nome explícito
(`app-x86_64-universal-release.apk`) e **nunca** `ls | head`. A asserção `unzip -Z1` das linhas
185-187 (ambas as ABIs arm presentes na universal) é boa e deve ser mantida.

### F7 — 🟠 Entradas mortas e dependências não usadas no módulo app

| Entrada | Declarada em | Usada? |
|---|---|---|
| `androidx-navigation-compose` | `libs.versions.toml:104` | **Não** — zero imports de `androidx.navigation`, nenhum `NavHost`/`rememberNavController`. A navegação é manual (Core `nav/MainSection.kt` + `ShellViewModel`). |
| `androidx-room-compiler` | `libs.versions.toml:116` | **Não** — o KSP do Room vive no Core. |
| `libs.plugins.ksp` | `libs.versions.toml:157` | **Não** — não há KSP neste repositório. |
| `androidx-media3-exoplayer` | `app/build.gradle.kts:409` | **Não** — a app nunca constrói um `ExoPlayer`. Só toca em `media3.common.util.UnstableApi` e `media3.ui.SubtitleView`/`CaptionStyleCompat` (`MpvVideoSurface.kt:273-311`). |
| `androidx.compose.ui.tooling.preview` | `app/build.gradle.kts:367` | **Não** — zero `@Preview`. |
| `androidx.compose.material.icons.core` | `app/build.gradle.kts:369` | **Não** — a app tem a sua própria abstração `ui/components/OwnTVIcon.kt`. |
| `androidx.room.ktx`, `androidx.room.paging` | `app/build.gradle.kts:399-400` | **Não** — nenhum `withTransaction`, nenhum uso de `androidx.room.paging`. |
| `androidx.media3.ui` | `app/build.gradle.kts:410` | **Sim** ✓ — `MpvVideoSurface.kt:273-311`. Manter. |

O `media3-exoplayer` é o único com consequências reais: põe o media3 no **classpath de compilação**
da app, dando-lhe um segundo voto na versão do media3 — precisamente o *drift* que F6 adverte —,
enquanto `media3-exoplayer-hls`/`dash`/`datasource-okhttp` **não** estão no classpath de compilação
(são `implementation` no `player-core`). **Hoje nada falta** (a app não constrói nenhuma media
source), mas no momento em que construir uma, não compilaria contra HLS. Escolher um dono: ou
remover a declaração, ou declarar também `hls`/`dash`/`datasource-okhttp`.

### F8 — 🟠 A dependência nativa `libmpv` é buscada por um canal não autenticado, sem pin de integridade

**`settings.gradle.kts:23-31`**, **`gradle/wrapper/gradle-wrapper.properties:3`**

```kotlin
exclusiveContent {
    forRepository { maven { name = "OwnTVNative"; url = uri("https://ahxn00.github.io/OwnTV_Core/maven") } }
    filter { includeModule("tv.own.owntv", "libmpv") }
}
```

É uma URL de **GitHub Pages** a servir um repositório Maven. **Não existe
`gradle/verification-metadata.xml`**, logo o Gradle aceita quaisquer bytes que essa URL devolva para
`tv.own.owntv:libmpv` — **a única biblioteca nativa que toda a stack de reprodução (mpv/FFmpeg)
carrega via JNI**. Umaatóriação desse site, um proxy envenenado ou um artefacto em cache obsoleto
substituem o motor nativo de todas as instalações sideloaded.

**Solução:** gerar e versionar `gradle/verification-metadata.xml` com checksums `sha256` de todos os
artefactos resolvidos; correr `--write-verification-metadata sha256` uma vez, rever e commitar.
**É o único controlo de CI de maior valor que falta no repositório.**

### F6 — 🟡 Os dois catálogos de versões podem divergir sem que nada o detecte

**`gradle/libs.versions.toml`** vs **Core `gradle/libs.versions.toml:4-9`**

O catálogo do Core declara a regra:
> *"When bumping a version that BOTH repos use — Room and Media3 above all — bump it in both, or the
> app ends up with two Room versions on one classpath."*

**Nada verifica isso.** Hoje coincidem; um bump unilateral produz silenciosamente **duas versões do
Room/Media3 no mesmo classpath** — a falha exacta que o comentário adverte, e que se manifesta como
`NoSuchMethodError` ou "cannot find generated implementation" **em runtime**, não em build.

**Solução:** um passo de CI que diffe as chaves partilhadas (`room`, `media3`, `koin`, `okhttp`,
`composeBom`, `coreKtx`, `kotlin`, `agp`) entre os dois catálogos e falhe quando divergem; ou mover
as versões partilhadas para um catálogo incluído.

### F10 — 🟡 O ficheiro de assinatura fora do repo tem um caminho de máquina fixo no script

**`app/build.gradle.kts:40-43`**
```kotlin
val localSigningProps = Properties().apply {
    val f = File("E:/MEGA/CODE/OwnTV_Gradle/owntv-signing.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}
```

Três problemas: (1) o caminho é **sem sentido fora da máquina do autor** e é a terceira de três
fontes sem override documentado; (2) a leitura é um `File.inputStream()` cru em tempo de
configuração, não um `ValueSource` — o Gradle regista *"a file was read"*, não *"este path era o
pretendido"*, e o configuration cache pode ser reutilizado com **os valores antigos**; (3) a
presença do ficheiro muda silenciosamente o comportamento do build.

**Solução:** tornar o caminho uma propriedade com default sensato e lê-lo pela API de providers
(`providers.fileContents(...)`), para que o valor participe na invalidação do configuration cache.

### F11 — 🟡 Configuration cache, build cache e memória: sobretudo bons, com três lacunas

O que está certo: `org.gradle.configuration-cache=true`, `org.gradle.caching=true`,
`org.gradle.workers.max=4` (com comentário bem pensado), `kotlin.daemon.useFallbackStrategy=false`,
`android.nonTransitiveRClass=true`.

Lacunas:
1. **Comentário obsoleto, modelo de memória errado.** As linhas 12-14 afirmam que o `assemble` completo
   *"compiles all eight app variants (standard/x86_64 x debug/release/benchmark/non-minified)"* —
   **não existe `benchmark` nem `non-minified`** em `app/build.gradle.kts:157-181`. São **4** variantes.
2. **A flag de parallel está no sítio errado.** `org.gradle.parallel=true` está comentado (linha 33)
   enquanto o configuration cache está activo. Com o CC, a flag que importa é
   `org.gradle.configuration-cache.parallel=true`.
3. **O configuration cache guarda as palavras-passe de assinatura e a edge key.** As leituras de
   `System.getenv(...)` em tempo de configuração são registadas como inputs e os valores resolvidos
   são serializados para o entry em disco. `providers.environmentVariable(...)` mantém-nos como
   inputs de *task*.

### F12 — 🟡 O mecanismo de lock do Core é sólido; o cache local e o arquivo vendorizado não são

**Verificação da pergunta "o core pode derivar silenciosamente?" — Resposta: não.**

- `custom-core.settings.gradle.kts:11-12` — o SHA-256 do zip é verificado contra o lock em cada build.
- linhas 18-32 — **cada um dos 1 007 ficheiros** é re-hasheado e comparado byte a byte.
- linhas 36-43 — qualquer ficheiro **extra** sob `core/src`, `core/schemas`, `player-core/src`,
  `tools`, `gradle` faz o build falhar.
- CI força o path do snapshot (`.github/actions/resolve-core/action.yml:24`).

**Problemas residuais:** (1) cache local ilimitado — 10 snapshots, ~1,7 GB, nada é aparado; (2) a
verificação corre em **cada invocação do Gradle** (é um settings script: lê o zip de 3,9 MB e
hasheia 1 007 ficheiros antes de *qualquer* task, incluindo `./gradlew tasks` e cada sync do IDE) —
um imposto real e recorrente em tempo de configuração; (3) **o lock e a documentação já divergiram**:
`core-source-lock.json:3-6` aponta para `e2081e78…`/1 007 ficheiros, mas
`docs/ANALISE_OWNTV_5.2.0_CORE_1.0.68_2026-10-18.md:11` ainda cita o lock anterior `3db885fd…`/994.
O campo `"files": 1007` é decorativo — nada o valida.

### F13 — 🟡 O gate local de i18n é WARN-only sem Python — e **falha** em classificações obsoletas

O aviso (não bloquear um build por falta de ferramenta) é defensável, e o CI **impõe** de facto
(`i18n.yml:48-51` instala sempre Python). Mas a severidade devia seguir o build type: um build
**release** deve falhar sem Python; um debug pode avisar.

Efeito mais surpreendente: `cmd_verify` falha tanto em **UNCLASSIFIED** como em **STALE
CLASSIFICATION** (`check_hardcoded_strings.py:552-555`). Ou seja, **apagar** qualquer literal
hardcoded — ou qualquer uma das 1 099 entradas de `safe_literals.txt` — **quebra o próximo build
local** até correr `prune-safe`. É higiene de ratchet intencional, mas acopla cada remoção de string
a uma regeneração do baseline.

**A dívida de literais está saudável, ao contrário da suposição:** `hardcoded_baseline.txt` tem
**56 linhas / 4,5 KB** (documentado como encolher-apenas) e o custo dominante é o
`safe_literals.txt` com 1 099 linhas / 99 KB — um manifesto revisto, linha a linha, num app de 243
ficheiros. **A lacuna dominante de cobertura é F4 (resources, não Kotlin).**

### F14 — 🟡 `AppModule.kt` viola a política documentada 25 linhas acima

**`app/src/main/java/tv/own/owntv/di/AppModule.kt:38-42, 62-97, 105-143`**

O KDoc nas linhas 38-42 afirma:
> *"ViewModels are bound with `viewModelOf(::X)` constructor references, **never positional `get()`
> lists**: with 15-23 parameters, a positional list only has to *count* right…"*

O binding do `LiveViewModel` (linhas 67-97) é **exactamente** a forma proibida: **29 `get()`
posicionais**. A justificação (linhas 62-66) é que o DSL `viewModelOf` do Koin pára nos 22
parâmetros — mas a correcção aplicada não é a política (argumentos nomeados). E o
`SettingsViewModel` **12 linhas abaixo** (linhas 109-143) **usa argumentos nomeados**, com um
comentário que explica exactamente porquê.

Duas correcções ao próprio KDoc: (1) o modo de falha descrito **não é exacto para Kotlin** — uma
lista posicional que não corresponde à aridade/tipos do construtor é um **erro de compilação**, não
uma troca silenciosa em runtime; (2) o `get()` do Koin aceita um tipo explícito (`get<LiveEpgReader>()`),
por isso o workaround da aridade é desnecessário.

### F15 — 🟡 Os monólitos, quantificados, com um caminho de refactorização incremental

| Ficheiro | Linhas | Forma |
|---|---|---|
| `features/shell/components/SettingsScreen.kt` | **4 446** | 71 `@Composable`s; o `SettingsScreen` principal vai de 190 a 1 559 (~1 370 linhas); um cluster `Glass*` ocupa 2 824-3 319 |
| `features/live/LiveViewModel.kt` | **2 714** | 108 funções, 45 `MutableStateFlow`s, construtor com **29 parâmetros** (linhas 109-137) |
| `features/shell/OwnTVShell.kt` | **2 050** | **um** composable (`fun OwnTVShell(...)` na linha 129, até ~2 024 — ~1 900 linhas), **30 parâmetros**, **104 declarações de estado local**, 7 `FocusRequester`s, mais todo o dispatch de secções (1 275-1 424), o modo de player, a camada de foco e os deep links |
| `features/settings/VideoPlayerSettingsScreen.kt` | 3 030 | segundo god-file |
| `features/live/LiveScreen.kt` | 1 936 | |
| `features/home/HomeScreen.kt` | 2 004 | |

`OwnTVShell` é o pior: um composable que possui o estado de navegação, a contabilidade de foco, o
overlay do player, o overlay de definições, o mini-player e o dispatch por secção — **tudo num
único scope de recomposição**. É o maior risco de manutenibilidade do repositório e **não** está em
`docs/MELHORIAS_FUTURAS_PENDENTES.md`.

**Caminho incremental (cada passo preserva comportamento e é independentemente publicável):**

- **Fase 0 — congelar.** A rede de segurança já existe: 78 ficheiros de teste unitário, 3 testes
  instrumentados, e `ShellBackPolicyTest.kt` / `DialogFocusRestoreTest.kt` /
  `BoundedFocusRequestTest.kt` já cobrem o comportamento mais frágil do shell. Adicionar testes de
  caracterização **antes** de tocar em nada.
- **Fase 1 — extrair estado, não UI.** Mover as 104 declarações `remember`/`mutableStateOf` de
  `OwnTVShell` para uma `@Stable class ShellState` em `features/shell/`. Sem mudança de UI; ~200
  linhas saem do composable; o estado passa a ser testável sem Compose.
- **Fase 2 — partir o dispatch.** Substituir a cadeia `selectedSection ==` (1 275-1 424) por um
  `when (selectedSection)` sobre composables de secção pequenos, cada um com um pacote de
  parâmetros estreito. `MainSection` já vive no Core ⇒ a costura é natural.
- **Fase 3 — partir o ecrã de definições.** Mover ~20 diálogos para `features/settings/dialogs/` e o
  cluster `Glass*` para `features/settings/glass/`. Movimentos de ficheiro puros.
- **Fase 4 — partir o `LiveViewModel`.** Extrair o cluster de timeshift/catch-up (linhas 156-338)
  para um `LiveTimeshiftController` e o cluster de paging/filtro para um `LiveChannelPager`.
  **Manter a superfície pública de `LiveViewModel` inalterada** para que o `AppModule.kt` não se
  mexa.
- **Fase 5 — módulos Gradle de feature, só depois de 1-4.** **Restrição:** *não* mover features da
  app para o repositório do Core — o Core está fixado por um snapshot SHA-256, portanto qualquer
  mudança de UI lá forçaria um re-snapshot e um bump do lock.

### F18 — 🟢 Duas conclusões negativas que vale a pena registar

- **A ausência do plugin `kotlin-android` é correcta, não um defeito.** O AGP 9.4.1 traz Kotlin
  embutido; o repositório documenta-o consistentemente e fixa o Compose compiler no mesmo Kotlin
  (2.4.20). **Sem acção.**
- **O risco R8/ProGuard é baixo, e isso é uma conquista deliberada.** As 32 linhas de
  `proguard-rules.pro` cobrem exactamente as três lookups reflexivas que existem
  (`dev.jdtech.mpv.**`, `* extends androidx.work.ListenableWorker`, `OwnTVDatabase_Impl`, e os
  enums). Grep por `Class.forName`, `getDeclaredConstructor`, `java.lang.reflect`, `kotlin.reflect`,
  `Gson`, `Moshi`, `kotlinx.serialization`, `Serializable`, `@Parcelize` → **zero hits** em
  `app/src/main`. `isShrinkResources = true` é seguro. **Um residuo:** a regra de keep do Room para
  uma classe do **Core** vive no `proguard-rules.pro` do **app**, enquanto o
  `consumer-rules.pro` do Core está deliberadamente vazio — uma inversão de camadas que faz um
  rename no Core partir o build **release** em runtime em vez de em compilação.

---
---

## 7. Estratégia de produto e inovações (agente E6)

### 7.1 Posicionamento honesto

> **O OwnTV não está a competir com o TiviMate nem com o OTT Navigator. Está a competir com
> "o leitor de vídeo genérico da box".** Aquelas são conchas de catálogo sobre ExoPlayer; o OwnTV é
> um **laboratório de reprodução** ao qual foi aparafusado uma concha de catálogo.

| Onde o OwnTV ganha mesmo | Evidência |
|---|---|
| **Duas engines reais + escada de fallback de 4 degraus determinística** (`EXO_HLS → EXO_TS → MPV_HLS → MPV_TS`) | Core `player/LiveLadder.kt:24-29,43-54` (*"the finiteness is the whole safety property"*) |
| **Três famílias de source em paridade real**, incluindo Stalker/Ministra com handshake MAG, identidade de dispositivo, *minting* de token por reprodução e re-resolve silencioso | Core `stalker/`, `parser/XtreamClient.kt`, `sync/StalkerSyncer.kt:1015` |
| **Privacidade local-first com caminho criptográfico real** (sem conta, sem cloud, auto-backup Android **desactivado**, sync só LAN, container `.own` AES-256-GCM + PBKDF2) | `AndroidManifest.xml` `allowBackup="false"`; Core `backup/BackupManager.kt` |
| **Escala que mais ninguém proclama** — ~50k canais / ~168k filmes com Paging limitado, FTS4, sync chunked e cancelável | `README.md:141-147` |
| **Catch-up + rewind + "Go back to…" *sem guia nenhum*** | Core `player/LiveSeekWindow.kt`; `features/live/CatchupJumpDialog.kt` |
| **Multiview que respeita o orçamento de planos de hardware** (TextureView) **+ limite de ligações medido pelo próprio utilizador** | `features/multiview/MultiviewScreen.kt`; Core `settings/LiveReservePreferences.kt` |
| **Auditável, bifurcável, auto-hospedável** — até os Cloudflare Workers estão publicados | `extras/worker/README.md` |
| **26 idiomas, RTL, e escala de fonte *independente*** (60-140 % principal, 50-120 % popups, 6 famílias) | `README.md:96`; Core `theme/FontCustomization.kt`, `theme/UiZoom.kt` |

| Onde o OwnTV perde mesmo | Impacto |
|---|---|
| **Sem descoberta, sem personalização, sem metadados de TV em directo.** "Now Trending" é um chart **global** do TMDB filtrado por "o meu provider consegue reproduzir isto" | **Crítico** — o Plex Discover ganha |
| **Sem sync entre redes** (só LAN, um dispositivo tem de ficar com o ecrã ligado) | Alto |
| **Sem casting.** A interface foi *documentada* para isso (`PlaybackEngine.playsLocally` — *"False only for an engine that has handed the stream to something else — a Chromecast"*) e **não existe código de cast** | Alto |
| **Sem voz.** Grep por `SpeechRecognizer`/`RecognizerIntent` → **zero** | Alto no Google TV |
| **Fricção de distribuição** — sem listing na Play ⇒ "fontes desconhecidas", sem reputação Play Protect | Alto para o mercado de massa; *correcto* para o nicho |
| **Acessibilidade não tratada** — o género é codificado **só por cor** (`ui/components/GenreColor.kt`) | Alto (e um gap comercial vs Plex/VLC) |
| **Os primeiros 5 minutos são um ecrã vazio** | Alto |

**Scorecard competitivo (honesto):** o OwnTV não pode vencer o mercado de massa (distribuição,
catálogo, suporte) e não deve tentar. Deve vencer o segmento **"tenho um provider e o meu provider é
estranho"** — onde uma escada de 4 degraus, um limite de ligações medido, a identidade Stalker e
pins por canal valem o seu peso — e converter essa credibilidade num **brand, um negócio de
suporte e um template** para quem quiser publicar um leitor IPTV.

### 7.2 As 20 inovações (nenhuma já na app, nenhuma em `MELHORIAS_FUTURAS_PENDENTES.md`)

`Core` = exige alteração na biblioteca externa `OwnTV_Core`; `App` = só neste repositório.
Esforço: S ≈ ≤3 dias · M ≈ 1-2 semanas · L ≈ 3-6 semanas.

| # | Ideia | Pitch | Dor que resolve | Esforço/Valor | Dep. | Risco principal |
|---|---|---|---|---|---|---|
| **1** | **Auto-Tune Memory** | "O canal abre em 1 segundo porque o OwnTV lembra-se de como o seu provider o serve" | *"o canal demora muito a abrir"* — hoje a escada recomeça sempre no degrau 1 | M / **5** | **Core** | Uma lição obsoleta fixa um canal num degrau partido. Mitigação: TTL de 7 dias + auto-desaprender + exigir **reprodução confirmada** antes de persistir. **Não pode ir à frente sem os itens B1/B7 da auditoria interna** |
| **2** | **TV-Remote (segundo ecrã)** | "O telemóvel passa a ser o comando, o guia e o picker de programas — no seu Wi-Fi, sem conta" | Digitar uma password Xtream no comando; encontrar um programa num grid de 200 canais | M-L / **5** | App+Core | O servidor companheiro é PIN-gated mas vulnerável a força bruta na LAN; exige token por sessão, não só 6 dígitos |
| **3** | **Catálogo demo no primeiro arranque** | "6 streams de domínio público, para o primeiro minuto não ser um ecrã vazio" | Os 5 primeiros minutos vazios (F1 no §retention) | M / **5** | App+Core (pequeno) | Superfície legal/moderadora; o source demo tem de ser **impossível** de confundir com um provider |
| **4** | **Temporizador de sono com fade-out** | "Daqui a 20 minutos o volume baixa, o descodificador é libertado e a TV fica no último canal — não num ecrã preto" | A TV fica acordada a descodificar 4K a noite; e ocupa um slot de ligação do provider | S / 4 | App+Core | Baixo. **Só a variante com temporizador na v1** (o "fim do programa" precisa do EPG) |
| **5** | **Health check do provider** | "Testa o meu provider: 40 canais, 4 s cada, e diz-me quais estão partidos antes de eu os ver" | O utilizador suspects do provider mas não tem provas; os tickets do Telegram são irrespondíveis | M / 4 | Core | **Sério: sondar consome ligações do provider.** Opt-in explícito, estritamente serial, parar ao atingir o limite medido, nunca sondar durante reprodução |
| **6** | **"Porque foi mau?" — veredicto por canal** | "Este canal rebufferizou 9 vezes em 6 min. O servidor do provider estava lento — não o seu Wi-Fi" | A pergunta eterna do suporte IPTV | M / 4 | Core+App | **Um veredicto errado é pior do que nenhum.** Depende de corrigir O4 (`ThroughputTracker` ignora `isNetwork`; `ExoStreamStats` apresenta throughput de transporte como bitrate) |
| **7** | **Persistir as "quirks" de stream** | "O OwnTV deixou de precisar de redescobrir que o seu provider mente sobre `.m3u8`" | O conhecimento morre a cada reinício do processo | M / 4 | Core | Persistir lições é o estado mais perigoso. Depende de B7/B8; TTL + botão "esquecer tudo o que aprendeu" |
| **8** | **Classificações reais em vez de regex para o modo Kids** | "Classificado, não adivinhado. O perfil da criança esconde o que é 16+ — e nada mais" | O modo Kids é um **regex sobre nomes de categoria** (`AdultCategoryClassifier.kt:25-27`) | M-L / 4 | Core | Falso-positivo (esconde o noticiário) = ticket; falso-negativo = evento legal. **Exige override manual pelos pais com PIN** |
| **9** | **Recomendações explicáveis por EPG** | "Hoje às 21:00 tem 3 filmes que vai gostar — e diz-lhe porquê" | Sem descoberta | M-L / 4 | Core+App | Deve degradar para *"começa nos próximos 90 minutos"* (o MVP real); opt-in; medição local |
| **10** | **Voz, parte 1 — ser encontrado pelo assistente** | "Diz 'abre o OwnTV' e a box encontra-o" | No Google TV o assistente é a interface principal | S / 3-4 | **App** | A plataforma indexa *apps instaladas*, não o catálogo IPTV |
| **11** | **Voz, parte 2 — pesquisa por microfone** | "Mantém o botão do microfone e diz o nome de um canal" | Pesquisar 50 000 canais com um teclado D-pad é inutilizável | M / 3 | **App** | **Honestidade obrigatória:** em muitas boxes não há microfone, e onde há é uma chamada de rede à cloud. Esconder o botão quando não há recognizer e dizer quando o áudio vai para a cloud |
| **12** | **Watchlist + relay de conhecimento zero** | "Favoritos, watchlist e pontos de retoma em qualquer rede — e o relay só guarda criptografia que não consegue ler" | Sem sync entre redes | M / 4 | Core+Worker | Um relay com (cifrado) histórico ainda é passivo. O token **é** a conta; exportável para o `.own` |
| **13** | **API de plugins de metadata** | "Aponta o OwnTV para a tua fonte de metadata e cada canal sem id no TMDB ganha poster, sinopse e classificação" | Sem metadados de TV em directo; e transforma um *gap* numa história de extensibilidade | M-L / 4 | Core+App | Editor de templates num comando de TV é penoso → presets + importação de JSON. **Nunca enviar credenciais a um scraper** |
| **14** | **Limite de largura de banda** | "Nunca uses mais de 8 Mbps neste perfil — escolhe a melhor versão que cabe" | Wi-Fi limitado, planos com limite de dados | L / 4 | Core | Mudar qualidade em silêncio é o que mais se odeia num player. **v1 só na selecção**, nunca em runtime |
| **15** | **Códigos de canal partilháveis** | "Envia 'TV-BR-0142' a um amigo com o mesmo provider" | "Vê este canal" não tem mecanismo | M / 3 | App+Core | O alias é um vector de des-anonimização. **Nunca transmitir o URL**; códigos com expiração |
| **16** | **Pilha de retoma da casa** | "A Renata estava a ver um filme no quarto; a TV da sala retoma exactamente onde parou" | TV partilhada por casal, criança e visita | M / 4 | Core | **Enfraquece o promessa de isolamento de perfis.** Excluído de perfis Kids, com PIN, opt-in, com atribuição visível |
| **17** | **Theme packs** | "Um ficheiro leva o teu acento, o anel de foco, o vidro, a fonte, o wallpaper e o zoom" | As features mais amadas são todas por-dispositivo e não partilháveis | S / 3 | **App** | O wallpaper pesa → variante "sem wallpaper"; regras de licença/atribuição para packs comunitários |
| **18** | **Layout screen-safe (OLED)** | "A tua TV é OLED: o relógio e a barra lateral param de se mexer ao fim de um minuto" | A UI é **estática** para uma app de TV (relógio permanente a cada 15 s, `TopBar.kt:100,114-118`) | S / 3 | **App** | O relógio é popular. Default por tema + override, nunca remoção silenciosa |
| **19** | **EPG diferencial + janela consciente da bateria** | "O guia actualiza em 200 KB em vez de 40 MB, e só quando a TV está na tomada" | O guia é a superfície diária **e** o maior custo de rede/armazenamento | M / 3 | Core | Um feed que muta programas anteriores seria truncado pelo `Range` → fallback para fetch completo + fetch completo semanal |
| **20** | **Salto de publicidade (preto + silêncio)** | "O provider meteu 90 s de publicidade no meio do programa — o OwnTV saltou" | Real, mas mais estreito do que parece (apenas live linear com pods do provider) | L / 3 | Core | **O risco mais alto da lista.** Falsos positivos catastróficos (cena escura, fade, pausa de diálogo). Exigir conjunção preto+silêncio+descontinuidade, ≥30 s, sempre com *undo*. **Legal/ToS.** *Ranked last on purpose.* |

**Rejeitadas explicitamente (com motivo):** Chromecast (precisa de uma 2.ª app receptora — o
`PlaybackEngine.playsLocally` está pronto, mas o receptor por omissão não reproduz um `.ts`
Xtream com headers de auth; adiar para o Horizonte 3) · dubbed por IA (contradiz a economia
local-first GPLv3 sem custo-por-utilizador) · *watch party* (produto errado; exige licenciamento de
conteúdo e contas) · PiP do OS (exótico e dependente de OEM na TV; o mini-player in-app resolve
melhor — é uma feature do app **Mobile**) · clustering de canais por IA (a versão local, determinística
e explicável entrega 90 % do valor a 10 % do custo; foi dobrada na #9) · **upscaling por hardware**
(já registado em `MELHORIAS_FUTURAS_PENDENTES.md` → `UPSCALING-BETA-01` — **não re-proposto**).

### 7.3 Retenção e onboarding — onde os primeiros 5 minutos perdem utilizadores

O caminho actual (`features/setup/SetupWizard.kt:87`) tem **8-11 ecrãs e 2-4 minutos** antes de o
utilizador ver um único canal:
`WELCOME → DISPLAY_SIZE → DISCLAIMER → SETUP_CHOICE → [CREATE_PROFILE|SYNC_DEVICE] → ADD_CONTENT
→ ADD_SOURCE_CHOOSER → ADD_SOURCE → IMPORTING`.

| # | Fuga | Onde | A correcção |
|---|---|---|---|
| **F1** | **O Home vazio.** "Skip for now" (`:604`) e "Run in background" (`:858`) deixam o utilizador numa app com **zero** linhas | `SetupWizard.kt:604,858`; `features/home/HomeScreen.kt` | **Nunca mostrar um Home vazio.** Um estado explícito **"A sua TV está pronta"** com 3 acções: adicionar provider · configurar pelo telemóvel · experimentar a demo. **É a correcção #1.** |
| **F2** | **A parede legal antes do produto** | `SetupWizard.kt:129,512` | Colapsar o disclaimer a **uma linha + link**; mover o texto completo para ⋯ Mais → Sobre. Pôr um benefício antes dele |
| **F3** | **Nenhum benefício é mostrado** — o preview pane, o poster wall Cinematic e o Guia são as melhores superfícies e não aparecem antes da importação | `SetupWizard.kt` | A **demo (#3)** é a correcção. Em alternativa, um reel de 10 s na tela WELCOME construído com a UI real |
| **F4** | **O perfil é criado antes de haver algo para perfilar** | `SetupWizard.kt:145-149` | Criar um perfil "Casa" implícito; o segundo perfil, personalizado, fica para depois |
| **F5** | **O guia — a superfície diária #1 — pode ficar vazio** | `features/epg/EpgScreen.kt` | Um estado **"O seu guia está vazio"** com um toque para "buscar o guia que a sua playlist anuncia" (o URL já está na playlist) |
| **F6** | **A importação é uma caixa negra** (só mostra contagens no fim; numa provider de 50k canais são minutos de nada) | `SetupWizard.kt:818-907` | Progresso **vivo por secção** (Live: 4 812 · Filmes: 12 004 · Séries: 3 310) e **deixar entrar no Live TV assim que o Live aterrar** |
| **F7** | **Nenhum tutorial de D-pad, nunca.** O modelo de interacção inteiro é "long-press OK abre opções" — um gesto que ninguém descobre e hostil a quem tem tremor | `extras/USER_GUIDE.md:84-100` | Um **coach de 3 passos** após a primeira importação: (1) OK reproduz, (2) CH± faz zapping, (3) long-press OK = opções. Descartável, re-armável |
| **F8** | **Sem medição** — e sem servidor não pode haver | `app/diagnostics/`, Core `util/CrashRecorder.kt` | Um painel local exportável **"A sua semana OwnTV"**: sessões, minutos, canais experimentados, falhas por causa. Duplica como auto-diagnóstico, artefacto partilhável e **a única métrica de retenção honesta possível** |
| **F9** | **O caminho de erro não ensina nada** | `features/player/FailedChannelAttempts.kt` | Um cartão em linguagem simples num canal falhado: **o que aconteceu** (nas palavras do utilizador) + **o que tentar** (3 acções ordenadas) + **um botão para exportar relatório** |

**O loop de retenção que o OwnTV deve correr:** não tem trigger próprio — o trigger é o guia, que só
existe se o utilizador sincronizar um EPG. **O Guia deve ser a porta da frente do produto, não um item
de barra lateral.** Considerar uma superfície **"Esta noite"** como destino de arranque para um perfil
cujo modo de arranque é "Home" — é a única que muda todas as noites e portanto a única que ganha uma
abertura diária.

### 7.4 Monetização e sustentabilidade de uma app GPLv3, sem anúncios, só-player

**A restrição, dita com honestidade:** o GPLv3 permite cobrar pelo software; exige que a fonte do
binário distribuído esteja disponível sob GPLv3; e **não** permite retirar features do build GPL.
Portanto: ❌ um "premium" com features bloqueadas (open-core — legalmente possível, mas destrói a
confiança que é o único moat real contra a marca do TiviMate) · ❌ anúncios ou tiles patrocinados
(contradiz a linha de fundação) · ❌ vender playlists (é a marca do README) · ❌ telemetria para um
servidor de terceiros (mata a promessa "sem conta, sem cloud").

**As seis linhas realistas:**

| # | Linha | Esforço | Valor | Nota |
|---|---|---|---|---|
| **R1** | **GitHub Sponsors / doações** — a linha base. O About não tem link de sponsor (`features/more/MoreScreen.kt:656-671` — só versão e licença) | S | Baixo por utilizador, alto no agregado | Mostrar a conta: *"€X/mês mantém os Workers de metadados para N instalações"* converte melhor do que uma página genérica |
| **R2** | **Flavor "donate to dev" na Play** — mesma fonte, mesma licença, só difere por uma linha "Apoiar" | S | Distribuição | Presume uma listing na Play, que é um **risco de política** para um leitor IPTV. Tratar como *experimento* |
| **R3** | **Trademark + licença white-label — a linha de maior margem.** O GPLv3 regula o **código**; não diz nada sobre a **marca**. Um ISP ou vendor de STB que queira incluir o OwnTV no seu bundle tem de rebrandar (perdendo 26 idiomas, o Glass, a escada de fallback e o Stalker) ou aceitar uma licença de marca | M (legal + registo) | **5** | **A evidência de que este mercado existe está no próprio repo: este checkout é um fork a jusante, com um Core snapshot personalizado e um build composto.** Esse ecossistema de forks é o cliente |
| **R4** | **O "OwnTV Relay"** — gateway de metadados + relay de sync cifrado, auto-hospedável, com o Worker como producto | M | 3 | O `extras/worker/` já é production-shaped. **O custo é o abuso, não a banda** (o cache de 180 dias colapsa o TMDB) |
| **R5** | **Suporte & SLA** — "OwnTV Priority Support: um mantenedor nomeado, resposta em 24 h em regressões de reprodução, um fork privado" | S | **4** | **A melhor receita por hora de engenharia de toda a lista** |
| **R6** | **Bounties sobre o Core** (paridade Stalker, suporte SJL, a11y) | S | 2-3 | Atracta contribuidores a custo salarial zero |

**Modelo de custo do centro de custo actual:** o custo recorrente do mantenedor é o gateway TMDB
(minúsculo), o gateway OpenSubtitles (sujeito a abuso), a banda do GitHub (grátis) e o Weblate
(grátis). **O centro de custo é pequeno e previsível** — precisamente por isso a mensagem de
sponsorship é credível e deve estar dentro da app.

### 7.5 Acessibilidade e inclusão — o baseline não é mau, é *inexaminado*

D-pad **é** uma acessibilidade (um utilizador cego pode navegar um sistema de foco), e a app já tem
uma escala de fonte granular para TV. Mas não há evidência de uma passagem de acessibilidade, e
faltam coisas específicas:

**Visão** — Sem `contentDescription` nos cards do browser, no grid do EPG e na spine das definições
(grep: ~15 hits, quase todos `null` em imagens decorativas). **O género é codificado só por cor**
(`ui/components/GenreColor.kt`) → adicionar um **código de forma/letra** (≈20 linhas num ficheiro).
Sem paleta segura para daltónicos (o acento é hex livre). Sem magnificador de foco. Sem calibração
por distância/tamanho de ecrã (`SetupWizard.kt:305-448` escala por percentagem, mas o tamanho certo
depende da diagonal física e da distância ao sofá).

** Audição** — **Sem "perfil surdo"**: legendas ligadas por omissão, título do programa sempre visível,
e um **aviso visual quando o áudio fica em silêncio** (para que um stream mudo não pareça partido) —
a app já distingue "não tem faixa de áudio" de "o utilizador desligou a imagem"
(`PlaybackEngine.audioOnly` vs `audioOnlyMedia`, `PlaybackEngine.kt:61-78`). Sem filtro SDH na
pesquisa OpenSubtitles. Sem título do programa permanente durante a reprodução.

**Motricidade/cognição** — A cultura do long-press OK é hostil a tremor e a quem não conhece o
gesto → um botão **"Opções"** explícito em todas as linhas + um preset **"Modo Senior"** (long-press
mais longo, com anel de progresso visível) construído **sobre o `SimpleModePreferences` que já
existe**. Sem "reduced motion" além do toggle de animações (adicionar "foco lento").

**Hardware de gama baixa** — A memória é bem gerida (`PlaybackMemoryMonitor`, `MemoryPressurePolicy`,
`UiZoom` com gate `LOW_RAM_WARN` a 85 %). **O armazenamento não é governado**: um guia de 14 dias +
downloads numa box de 8 GB → um **painel de orçamento de armazenamento**. E um **flavor "Lite"**
(sem Glass, sem TMDB, sem Multiview, sem downloads, só Exo) para boxes de 1-2 GB — mesma fonte,
mesma GPL, muito menos RAM e disco.

**Para além da matriz de deficiência** — o idioma é o activo de inclusão mais forte do OwnTV (26
idiomas, RTL, plurais/datas/números locale-aware) e está **sub-anunciado**: fazer de *"26 idiomas, RTL
e sem anúncios"* uma linha de primeira classe no About. E o disclaimer é **texto legal em inglês em
todos os idiomas** — verificar que as 25 traduções são juridicamente fiéis, não só literalmente
traduzidas, ou a app cria responsabilidade em cada mercado que reclama servir.

---
---

## 8. UI/UX de TV — foco, D-pad e funcionalidades invisíveis (agente E2)

> **Este é o capítulo mais grave depois da distribuição.** Quatro funcionalidades centrais estão
> **inacessíveis ou mortas** — verificadas por releitura directa do código.

### UX-01 — 🔴 A **Pesquisa** não tem porta de entrada na UI (regressão)

**`features/shell/components/TopBar.kt:96-110`** · **`features/shell/OwnTVShell.kt:930,1222,1345`**

O `TopBar` inteiro (verificado):
```kotlin
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    audioBar?.invoke()
    ClockChip()
    if (playlistName.isNotBlank()) { PlaylistChip( ... ) }
}
```

**Não há Search pill, não há chip de secção, não há Continue chip.** O comentário do próprio shell
(`OwnTVShell.kt:1222`) ainda diz *"top bar above the content (active section + **Search pill** + clock
+ playlist)"* — o comentário descreve um componente que já não existe. `git show b6dffa8` confirma o
commit que apagou `SearchPill`, `ContinueChip`, `SectionChip` e `WeatherChip`.

**Todos os caminhos restantes para `MainSection.SEARCH` estão mortos ou são opt-in:**
1. A `Sidebar` só desenha `MainSection.browseOrder` (`Sidebar.kt:166`) — SEARCH não está lá, e
   `MainSection.isBrowse` exclui SEARCH.
2. `NavMenuSettingsScreen.kt:241` só permite alternar `browseOrder` — SEARCH nunca pode ser adicionado.
3. `OwnTVShell.kt:930` — `RemoteShortcutAction.OPEN_SEARCH` existe, mas
   `RemoteShortcuts.kt:46-55` mostra que **os atalhos por omissão não o incluem**.
4. `OwnTVShell.kt:1345` — `onOpenTrendingSearch` é o único caminho de UI, e está dentro do hero
   Trending, que por sua vez está morto (ver UX-02).

`grep "common_nav_search"` em toda a app devolve **um** resultado: `ChNavSettingsScreen.kt:427`, o
picker de atalhos de comando.

**Impacto:** a funcionalidade mais usada de uma app de TV — **pesquisar num playlist de 50 000
canais um canal que não se encontra a percorrer** — não tem porta. O utilizador tem de descobrir
Definições → Atalhos de comando, vincular uma tecla e memorizá-la. Muitos concluem simplesmente que
a app não pesquisa.

**Solução:** restaurar um `SearchPill` no `TopBar` (a implementação apagada já tinha a regra
`focusProperties { canFocus = visible }` correcta, para nunca prender o foco) e passar
`onSearchClick = { onSelectSection(MainSection.SEARCH) }` do `OwnTVShell`. Alternativa: adicionar
`SEARCH` a `MainSection.browseOrder` e dar-lhe um `NavItem` na `Sidebar`.

### UX-02 — 🔴 "Now Trending" e as linhas VOD de "Continuar a ver" **nunca** são renderizadas

**`features/home/HomeViewModel.kt:548`** · **Core `home/HomeFeed.kt:190-200,322-324`** ·
**`features/home/HomeScreen.kt:573,618-930,1764-1817`**

```kotlin
548:    val data = feed.load(profileId, liveOnly = true)
```

Core `HomeFeed.kt:190-200`:
```kotlin
val movieIdsAsync  = async { if (liveOnly) emptySet() else activeSourceIds(...MediaType.MOVIE).toSet() }
val seriesIdsAsync = async { if (liveOnly) emptySet() else activeSourceIds(...MediaType.SERIES).toSet() }
val continuationAsync = async { if (liveOnly) emptyList() else planner.buildContinuationItems(profileId) }
...
val config = if (liveOnly) savedConfig.copy(
    hidden = savedConfig.hidden + setOf(HomeRow.TRENDING, HomeRow.CONTINUE_MOVIES, HomeRow.CONTINUE_SERIES),
    heroIncludeMovies = false, heroIncludeSeries = false,
) else savedConfig
```

**Consequências, todas verificáveis em `HomeScreen.kt`:**
- `state.trendingItems` é **sempre vazio** → `rowHasData(TRENDING)` é falso (`:573`) → a linha é
  filtrada de `renderRows` → **toda a secção `TrendingHeroSection` de 538 dp** (`:618-930`, com o
  merge do TMDB, o botão "All versions" e os badges de idioma) e a `TrendingPosterRow`
  (`:1764-1817`) são **código morto**.
- `state.continueMovies` / `continueSeries` são **sempre vazios** → a `ContinueWatchingRow`
  (`:1692-1758`) **nunca** é renderizada.
- O hero é live-only → `HeroItem.MovieHero` / `HeroItem.SeriesHero` são **inalcançáveis**.

**E o sync do Trending continua a correr** — `SyncStatusPill.kt:134-137` e `:276-336` mostram
"Now trending / a corresponder filmes / a publicar" para uma linha que o utilizador **nunca vai ver**.
E `HomeSettingsScreen.kt:105-123` só expõe HERO / RECENT_CHANNELS / FAVORITE_CHANNELS — **não há
nenhuma definição que o volte a ligar**.

**Impacto:** a app gasta banda e CPU a construir uma rail de Now Trending e depois **atira-a fora**.
O utilizador vê um indicador de progresso para "Now trending" e, quando acaba, **nenhuma linha
aparece** — uma mentira visível. Um filme visto a metade nunca pode aparecer em "Continuar a ver".

**Solução:** passar `liveOnly = false` (com o trabalho extra atrás de uma definição que o utilizador
controle), **ou** — se live-only for deliberado — apagar a UI de Trending/Continue VOD, a definição
`trendingStyle`, e parar de anunciar o sync de Trending no status pill.

### UX-03 — 🔴 "Downloads" mostra apenas gravações; os separadores Filmes/Séries são inalcançáveis e o separador visível é um botão inerte

**`features/downloads/DownloadsScreen.kt:94,101-109,245-269,271-274,297`** (verificado)

```kotlin
 94:    val tab = DownloadsTab.LIVE                      // fixo, nunca muda
...
245:            listOf(DownloadsTab.LIVE).forEach { entry ->   // só UM separador é desenhado
246:                OwnTVButton(
247:                    label = stringResource(entry.labelRes),
248:                    onClick = { },                          // <-- não faz nada
...
260:        if (tab == DownloadsTab.LIVE) {
261:            RecordingsScreen( ... embedded = true ... )
268:            return@Column
269:        }
```

Os ramos `DownloadsTab.MOVIES` / `.SERIES` (`:104-105`) e o estado vazio (`:271-274`) são **código
morto**.

**Impacto no utilizador de TV:**
- O item da barra chama-se "Downloads" mas o ecrã mostra **gravações**.
- O único controlo do ecrã é um chip chamado "Live TV" que **não faz nada** quando se prime OK — uma
  **dead-end de D-pad literal** na primeira coisa que o ecrã desenha.
- **Qualquer filme ou episódio transferido é permanentemente invisível.** Não existe outro ecrã que
  liste as linhas de `downloadDao.observeForProfile` (`DownloadsViewModel.kt:53-88`). Um utilizador
  com um USB cheio de filmes não tem como os ver na televisão.

**Solução:** tornar `tab` estado real (`var tab by rememberSaveable { mutableStateOf(DownloadsTab.LIVE) }`),
desenhar os três separadores a partir de `DownloadsTab.entries`, dar a cada um um `onClick` real, e
só renderizar `RecordingsScreen` no separador LIVE. Considerar renomear o item da barra para
"Gravações" se as transferências não forem suportadas na TV.

### UX-04 — 🔴 As linhas Favoritos, Histórico e Sincronização local do hub "Mais" **nunca** são desenhadas

**`features/more/MoreScreen.kt:84,131,136,139,256-308`** (verificado)

```kotlin
 84: private enum class MoreRow { SETTINGS, FAVORITES, HISTORY, BACKUP, LOCAL_SYNC, ERROR_LOG, ABOUT }
...
136:    var selected by rememberSaveable { mutableStateOf(MoreRow.SETTINGS) }
139:    if (selected in setOf(MoreRow.FAVORITES, MoreRow.HISTORY, MoreRow.LOCAL_SYNC)) selected = MoreRow.SETTINGS
```

A spine (`:256-308`) desenha `SpineRow`s **apenas** para `SETTINGS` (`:256`), `BACKUP` (`:271`),
`ERROR_LOG` (`:286`) e `ABOUT` (`:297`). FAVORITES / HISTORY / LOCAL_SYNC só existem no **mapa de
rótulos** (`:321-325`) — nunca são desenhados. Logo:
- `selected` nunca pode tornar-se FAVORITES/HISTORY/LOCAL_SYNC (a linha 139 força-o de volta),
- `page` nunca pode tornar-se `MorePage.FAVORITES/HISTORY/LOCAL_SYNC` (`:138` força de volta a ROOT),
- os painéis `CountsPane(favorites, …)`, `CountsPane(history, …)` e `LocalSyncPane(sync)` (`:362-365`)
  são **inalcançáveis**,
- e mesmo assim `MoreCountsViewModel`'s flows `favorites`/`history`/`sync` são **recolhidos** em
  `:121-125` — três subscrições vivos à base de dados por frame, para nada.

**Impacto:** Favoritos e Histórico **não têm porta de entrada** no hub Mais — e como
`MainSection.FAVORITES`/`HISTORY` também não são itens da barra, **os favoritos são inalcançáveis em
toda a app na TV** (a única rota restante é a pasta Favoritos do próprio rail Live, e só existe se o
Live TV estiver na barra). A Sincronização local (pareamento com o telemóvel) só é alcançável por
Definições → Sincronização local, um sub-separador que o `SettingsSearchCoverageTest` exclui
explicitamente da pesquisa (`notSearchable = setOf("ROOT", "BACKUP", "LOCAL_SYNC")`).

**Solução:** adicionar as três `SpineRow`s em falta e apagar os resets `LaunchedEffect` de `:138-139`.

### UX-05 — 🟡 O chip "Continuar" foi removido, e o alvo do Continuar só pode ser um canal em directo

**`features/shell/OwnTVShell.kt:550,879-908`** · **`features/home/HomeViewModel.kt:188-216`**

```kotlin
188:    val continueTarget: StateFlow<ContinueTarget?> = settings.activeProfileId
189:        .flatMapLatest { pid -> … }
193:                h?.takeIf { it.mediaType == MediaType.LIVE }?.let { resolveContinue(pid, it) }
```

O `takeIf { it.mediaType == MediaType.LIVE }` filtra o histórico **antes** de `resolveContinue` — logo
os ramos `MediaType.MOVIE` (`:199-203`) e `MediaType.EPISODE` (`:204-209`) de `resolveContinue`, e
portanto `ContinueKind.MOVIE` / `ContinueKind.EPISODE` (`:104-106`), são **inalcançáveis**.
E `OwnTVShell.kt:550` continua a recolher o flow, usado num único sítio — `continueLastWatched`
(`:880`), ligado apenas a `RemoteShortcutAction.CONTINUE_LAST_WATCHED` (`:940`). **Nenhum chip é
desenhado** (`grep continueTarget` → 3 hits).

**Solução:** remover o filtro `takeIf { it.mediaType == MediaType.LIVE }` (o `when` de
`resolveContinue` já discrimina correctamente) e restaurar o `ContinueChip` no `TopBar`.

### UX-06 — 🟡 Todos os botões do mini-player e do Audio Mode são ícones sem rótulo

**`player/MiniPlayer.kt:183-221`** · **`player/AudioNowPlayingBar.kt:374-418`**

Nem `MiniBtn` nem `AudioBtn` definem `contentDescription` nem `semantics { contentDescription = … }`.
Os **8 verbos** do mini-player (play/pause, mute, auscultadores→Audio Mode, zoom→tamanho,
PIP→posição, expand, close, more) e os **7 verbos** do Audio Mode são `FocusableSurface`s cujo único
conteúdo é um `OwnTVIcon` sem descrição. `AudioNowPlayingBar.kt:215` usa
`contentDescription = null` para o logótipo da estação.

**Impacto:** um utilizador de TalkBack só ouve "botão". Não há forma de saber que um mosaico silencia
e outro fecha a sessão — e visualmente são círculos escuros quase idênticos.

**Solução:** `Modifier.semantics { contentDescription = stringResource(...) }` por verbo, e um
`contentDescription` na janela ("A reproduzir: <título>").

### UX-07 — 🟡 Apagar uma gravação ou uma transferência é imediato e irreversível — sem confirmação

**`features/recordings/RecordingsScreen.kt:319,544-552`** · **`features/downloads/DownloadsScreen.kt:297,432`**

```kotlin
319:  onDelete = { contextId = recording.id; contextIndex = index; contextStatus = recording.status; vm.delete(recording) },
...
546:  OwnTVButton(stringResource(CoreR.string.common_delete), onClick = onDelete, enabled = !deleting, …)
```

**Contraste com o resto da app**, que é cuidadosa neste ponto:
- `features/customize/CustomizeScreen.kt:485-489` — `PinConfirmDialog` antes de remover uma categoria.
- `ui/components/MoveOrderOverlay.kt:189-190` — "Restaurar / Confirmar restaurar" em dois passos.
- `features/recovery/DatabaseRecoveryScreen.kt:70-71` — reset em dois passos.

**Impacto:** um OK acidental no D-pad apaga o ficheiro do disco. Sem *undo*, sem lixo, sem
confirmação. E a lista de gravações é exactamente onde um erro de pressão custa mais.

**Solução:** o padrão de dois passos do `MoveOrderOverlay` — o primeiro OK transforma o botão em
"Confirmar apagar?" e o segundo apaga; ou um modal no estilo `PinConfirmDialog`.

### UX-08 — 🟡 O mini-guia prende o foco mas só lhe dá 180 ms para assentar → D-pad morto

**`features/epg/MiniGuideOverlay.kt:71-76,109`**

```kotlin
71:    LaunchedEffect(Unit) {
72:        repeat(3) {
73:            kotlinx.coroutines.delay(60)
74:            if (runCatching { focus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
75:        }
76:    }
...
109:                }.focusable().trapAllFocusExit()) {
```

`trapAllFocusExit()` (`ui/components/FocusTrap.kt:42-46`) cancela **todas** as saídas direccionais
de foco, pelo que se o pedido da linha 74 falhar, o painel fica **sem foco em lado nenhum** e o D-pad
fica morto — só o Back escapa. O orçamento total é 3 × 60 ms ≈ **180 ms**, e não espera por um
*layout pass*.

**Impacto:** o overlay é aberto a partir de um botão do HUD **enquanto o vídeo está a reproduzir**, e
é desenhado com `GlassSurface.DIALOGS`. Numa TV de gama baixa, o frost de vidro + a anexação da
janela de popup demoram facilmente mais de 180 ms. O utilizador prime "Guia", aparece um painel, e
**o D-pad não faz nada**.

**Solução:** usar o helper que o codebase **já tem**: `focus.requestBoundedFocus(guard)`
(`ui/components/BoundedFocusRequest.kt:42-57` — 10 tentativas, um frame de intervalo) — é exactamente
o que `EpgScreen.kt:746` e `:834` já fazem.

### UX-09 — 🟡 O overlay de categorias não tem estado vazio nem nó focalizável → o D-pad escapa para o player

**`features/shell/components/CategoryBrowserOverlay.kt:69-82,109-131`** · **`ui/components/RoundedPanel.kt:72-77`**

Se `categories` estiver vazio, **nenhum nó da árvore carrega `focusCurrent`** (o requester é ligado
condicionalmente a uma linha que não existe), pelo que as linhas 78/80 falham em silêncio. O painel
fica só com um título e nenhum filho focalizável. E o overlay **não está focus-trapped** — `modalScrim`
é apenas um fundo (`RoundedPanel.kt:72-77`), ao contrário dos diálogos reais.

**Impacto:** o gesto natural "Left duas vezes no player" (mostrar outros canais) com uma biblioteca
sem categorias dá um **painel vazio que rouba o ecrã e devolve o D-pad ao vídeo por baixo**.

**Solução:** estado vazio explícito com um botão "Fechar" focalizável, ligar `focusCurrent` a esse
fallback, e adicionar `trapAllFocusExit()`.

### UX-10 — 🟡 O corpo de 2 050 linhas do shell **recompõe de 5 em 5 segundos** e a cada play/pause

**`features/shell/OwnTVShell.kt:412,414-426,456-473,849-854,1303-1358`**

```kotlin
414:    val reminderClockMs by produceState(initialValue = System.currentTimeMillis(), …) {
421:            while (true) { value = System.currentTimeMillis(); kotlinx.coroutines.delay(5_000L) }
...
456:    LaunchedEffect(
457:        scheduledReminders, activeProfileId,
459:        reminderClockMs,          // <-- lido DURANTE a composição
460:        activeLiveEdgeChannelId, …
```

`reminderClockMs` é lido **na composição** (é uma chave do `LaunchedEffect`), pelo que a mudança de
valor invalida **todo** o composable `OwnTVShell` **de 5 em 5 segundos**. O mesmo se passa com
`dockedPlaying` (`:412`) e `exoPlaybackConfirmed` / `mpvPlaybackConfirmed` / `mpvBuffering` /
`mpvFailure` (`:849-854`) — todos lidos directamente no scope do shell, pelo que **cada** transição
de buffering/ready/erro durante a reprodução re-executa o corpo inteiro de 2 050 linhas.

Isto viola directamente a regra que o **mesmo ficheiro** declara 100 linhas antes (`:351-354`):
> *"The offset itself ticks once a second while an archive plays. Held as State and deliberately NOT
> read here — reading it in the shell's own scope invalidated the whole shell body every second."*

Pior: os callbacks por destino são re-invocados com **lambdas novamente alocadas** (`:1307-1358`), pelo
que uma recompposição do shell muda a identidade dos parâmetros e o ecrã filho recompõe também.

**Impacto:** numa TV de gama baixa é a diferença entre um D-pad fluido e um D-pad a engasgar — e é
pior exactamente onde o utilizador está a ver (transições de play/pause) ou tem um lembrete de programa.

**Solução:** não ler `reminderClockMs` no scope do shell; **hoistar** os callbacks por destino para
`remember { … }`; ler `dockedPlaying` / `mpvBuffering` / `mpvFailure` apenas dentro das lambdas que
os precisam.

### UX-11 — 🟡 O "focus restorer" é ligado condicionalmente → desactiva exactamente quando é preciso

**`features/shell/OwnTVShell.kt:1175-1182,710-721,832-838,874-877`**

```kotlin
1175:        Column(
1177:            // One group with a restorer: leaving for the mini player and coming back lands on the
1178:            // exact control that was focused, without every screen having to remember its own.
1180:                .focusRequester(shellContentFocus)
1181:                .then(if (restoreFocus && selectedSection == MainSection.LIVE_TV) Modifier else Modifier.focusRestorer())
1182:                .focusGroup(),
```

`restoreFocus` é posto a `true` por `exitPlayer()` (`:710`), `dockPlayer()` (`:835`), `toAudioMode()`
(`:844`), pelo efeito de sidebar oculta (`:976`) e pelo fecho do multiview (`:741,:751`) — ou seja, é
verdade durante **todo** o tempo em que o mini-player está docked, que é precisamente o momento em
que o restorer existe.

O `FocusRestorer` do Compose regista o filho previamente focado no handler `onExit` do grupo. Enquanto
`focusRestorer()` está ausente da cadeia, o grupo **não tem hook `onExit`** → o registo *"em que
controlo eu estava"* **nunca é escrito** → `shellContentFocus.requestFocus()` resolve para o
**primeiro** alvo focalizável da coluna, não para o controlo que o utilizador deixou.

**Impacto:** o comentário promete *"handing focus back to it after the mini player returns to the
exact control the user left rather than to the top of the screen"*. Numa lista de 50 000 canais, ser
soltado no topo é um custo real de navegação.

**Solução:** instalar `focusRestorer()` **incondicionalmente** e deixar os ecrãs que têm um alvo
explícito vencerem — o `LiveScreen` já o faz com `returnChannelFocus` / `selFocus`
(`LiveScreen.kt:489-538`), portanto o condicional não é necessário para a correcção.

### UX-12 — 🟡 A linha "Agora" do Home engole a tecla Back — a primeira pressão não faz nada

**`features/home/HomeGuideSlice.kt:187-214,234`**

```kotlin
199:        return when (event.key) {
200:            Key.DirectionUp -> { selectRow((activeRowIndex - 1).coerceAtLeast(0)); true }
204:            Key.DirectionDown -> { selectRow((activeRowIndex + 1).coerceAtMost(rows.lastIndex)); true }
208:            Key.DirectionLeft, Key.DirectionRight, Key.Back -> {
209:                activeRowIndex = -1
210:                true                       // <-- consumido: nunca chega ao BackHandler
211:            }
212:            else -> false
213:        }
...
234:    .onPreviewKeyEvent(::handleListKey)
```

`onPreviewKeyEvent` a devolver `true` para `Key.Back` consome o evento na fase de preview, pelo que
o `BackHandler` da app **nunca corre**. O único efeito é `activeRowIndex = -1` — o modo de
navegação de linhas é reposto e **o ecrã não recua**. O utilizador tem de premir Back **duas vezes**.

Isto é exactamente a classe de bug que o próprio shell documenta (`OwnTVShell.kt:1150-1157`):
> *"Back out of Settings looked dead until a second press, by which time the exit had nowhere left to go."*

**Solução:** não consumir Back — repor `activeRowIndex` e devolver `false` para o evento continuar
até ao `BackHandler`.

### UX-13 / UX-14 / UX-15 — 🟢 Baixa

| ID | Achado | Localização |
|---|---|---|
| **UX-13** | O hero de "sem conteúdo" do Home é um elemento **focalizável que não faz nada** (sem `onClick`, sem `onKeyEvent`) — premir OK no elemento realçado não faz nada | `features/home/HomeScreen.kt:1930-1944` |
| **UX-14** | O foco inicial do ecrã Downloads assenta no chip "Live TV" **inerte** — o primeiro OK do utilizador é engolido | `features/downloads/DownloadsScreen.kt:95,177-181,245-254` |
| **UX-15** | O chrome do mini-player com `alpha(0f)` **continua exposto** aos serviços de acessibilidade (o `alpha` não remove o nó da árvore de semântica) | `player/MiniPlayer.kt:104-119,123-133,74` |

### 8.1 Verificado e sólido (UI/UX)

- `FocusableSurface` / `FocusTrap` / `BoundedFocusRequest` / `rememberDialogFocusRestore` são uma
  camada de foco **genuinamente bem construída**; `EpgScreen`, `LiveScreen` e `SettingsScreen` usam
  `requestBoundedFocus`, `restoreAfterDialogClose` e `trapVerticalFocusExit` correctamente.
- `SettingsScreen` (4 446 linhas) **não tem nenhuma string hardcoded** visível ao utilizador (só
  glifos `"#"` e `"›"`) e a sua cobertura de pesquisa é mantida por `SettingsSearchCoverageTest`.
- As listas lazy em âmbito **têm chaves estáveis**: `LiveScreen.kt:797`, `ChannelListOverlay.kt:124`,
  `CategoryBrowserOverlay.kt:117`, `CategoryRail.kt:319`, `HomeScreen.kt:1721/1789/1200`,
  `DownloadsScreen.kt:277`, `RecordingsScreen.kt:294`.
- O plumbing da tecla Back do shell (`OwnTVShell.kt:1084-1171`) é deliberado e correcto.
- O carregamento de imagens é limitado onde importa (`PosterCard.kt:85` `.size(320, 480)`,
  `EpgScreen.kt:1100` `.size(128, 128)`).

---

## 8-B. Player, reprodução e zapping (agente E1)

> O `player-core` (os motores libmpv/ExoPlayer) é externo. O agente usou-o **apenas** para
> verificar contratos do lado app; **todos os achados estão ancorados em ficheiros do app**.

### PLY-01 — 🟠 O OSD "a ver agora" depois de CH+/CH− **nomeia o canal de onde acabou de sair**

**`player/PlayerHud.kt:593-607`** (verificado) · **`player/PlayerHudChrome.kt:198-205`**

```kotlin
599:  if (isLive && (tuned != null || (showFlash && !controlsVisible))) {
600:      // A fresh tune drives the card from the lookup result, not player metadata: the Stalker and
601:      // mpv paths publish their metadata after an async resolve, which would show the old channel.
602:      if (tuned != null) {
603:          ChannelOsdCard(title = tuned.name, …, logoUrl = tuned.logoUrl)
604:      } else {
605:          ChannelCard(player)          // ← metadata do MOTOR == o canal QUE SAIU
606:      }
607:  }
```

`ChannelCard` lê `player.currentMeta` — a metadata **do motor**. O motor só publica a metadata do
canal novo quando o stream novo abre de facto (minting do portal Stalker, resolve assíncrono do
mpv, arranque do HLS do ExoPlayer). Num provider IPTV normal isso são **1-5 s**. O comentário nas
linhas 600-601 identifica **exactamente** o problema e resolve-o para o caminho do *direct tune*
(`TuneOsd.Tuned` traz o nome) — mas o caminho **CH+/CH−** ficou a ler a metadata do motor.

**Impacto:** o utilizador faz zapping para "Sky Sports Main Event" e o cartão diz **"BBC One"** durante
quase toda a vida de 3 s do flash.

**Solução:** dar ao caminho do zap a mesma costura de identidade que o direct tune já tem — passar o
canal recém-seleccionado do shell (que já tem `liveVm.playingChannel`) e desenhar o flash a partir
dele, em vez de `ChannelCard(player)`.

### PLY-02 — 🟠 No MultiView, adicionar um mosaico move o **anel** de foco para um mosaico que não tem o foco do D-pad

**`features/multiview/MultiviewScreen.kt:90,92,128,174`** (verificado)

```kotlin
 90:  val focusRequesters = remember(state.tiles.size) { List(state.tiles.size) { FocusRequester() } }
 92:  LaunchedEffect(Unit) { runCatching { focusRequesters[state.focused].requestFocus() } }
...
128:  { menuFor = null; state.addTile(); state.focus(state.tiles.lastIndex) }
...
174:    .onFocusChanged { if (it.isFocused) state.focus(index) }
```

`remember(state.tiles.size)` aloca **uma lista nova de `FocusRequester`s** sempre que o número de
mosaicos muda, pelo que o modificador `focusRequester(...)` de cada mosaico é re-ligado a uma
instância diferente. `state.focus(state.tiles.lastIndex)` (linha 128) move o **estado de selecção**
— que é o que desenha a moldura — mas **nenhum `requestFocus()` é emitido** para o mosaico novo, e
`LaunchedEffect(Unit)` (linha 92) nunca corre de novo.

**Impacto:** depois de "Adicionar canal", a grelha desenha o anel de foco no mosaico novo mas o
**foco real do D-pad continua no mosaico anterior**. Premir OK age sobre o mosaico em que o anel
**não** está. O anel e o comando discordam.

**Solução:** manter os requesters estáveis entre mudanças de contagem e pedir o foco explicitamente
depois de `state.addTile()`.

### PLY-03 — 🟡 `AudioNowPlayingBar` liga **dois `FocusRequester`s ao mesmo alvo**; o do stage-1 é sombreado

**`player/AudioNowPlayingBar.kt:315-321`** e **`:148-156`**

```kotlin
315:  FocusableSurface(
317:      modifier = Modifier
318:          .matchParentSize()
319:          .focusRequester(pillFocus)                                   // ← 1.º
320:          .then(entryFocusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)  // ← 2.º, mesmo nó
321:          .focusProperties { canFocus = focusable && !active },
```

Só um `focusRequester` por alvo de foco é honrado; **o último ganha**. `entryFocusRequester` é
**sempre** não-nulo em produção (`OwnTVShell.kt:1263` passa `audioEntryFocus`, criado em `:208`).
Logo `pillFocus` **nunca** é o requester que o sistema de foco resolve para esse nó, e
`pillFocus.requestFocus()` (linha 154) rebenta com `IllegalStateException` — engolido pelo
`runCatching`.

**Impacto:** premir Back dentro da barra de áudio (documentado em `AudioNowPlayingBar.kt:73-76` e
`:159-160` como *"the ONLY way out"*) **não devolve o foco à pílula**. `active` fica false, a barra
colapsa, e o D-pad fica sem alvo dentro da barra.

**Solução:** um requester só, e expor o da pílula para o shell poder entrar na sessão de áudio.

### PLY-04 — 🟡 Três diálogos usam um `delay(60)` fixo + um `requestFocus()` engolido → podem abrir com o D-pad morto

**`features/live/CatchupJumpDialog.kt:132-134,179`** · **`features/live/CatchupManualTimeDialog.kt:104`** ·
**`player/AutoFrameRatePrompt.kt:92`**

```kotlin
LaunchedEffect(Unit) { kotlinx.coroutines.delay(60); runCatching { firstFocus.requestFocus() } }
```

**A app já resolveu isto** — `player/PlayerHudDialogs.kt:200-210`:
```kotlin
/**
 * Requests [focus] with retries: dialog-window content composes a frame or two after the calling
 * effect starts, so a one-shot requestFocus can fire before the target row exists and silently fail.
 */
private suspend fun requestFocusRetrying(focus: FocusRequester) {
    repeat(10) {
        androidx.compose.runtime.withFrameNanos {}
        if (runCatching { focus.requestFocus() }.isSuccess) return
        delay(50)
    }
}
```

`OwnTVPopup` é uma `Dialog` de plataforma real (`ui/components/OwnTVPopup.kt:56-64`), logo o seu
conteúdo compõe numa composição separada um ou dois frames depois. O comentário do próprio codebase
descreve o modo de falha exacto — *"a one-shot request can fire before the target row exists and
silently fail"* e *"the dialog window is still mid-transition (seen on HDR/HDR10/DTS streams)"*.
Um `delay(60)` fixo não é um substituto com precisão de frame. E em `CatchupJumpDialog` o effect está
**fora** do popup (linha 132 vs popup na 137), logo os 60 ms são medidos a partir da composição
**externa**, não da janela do diálogo.

**Impacto:** "Go back to…", "Catch-up failed" e o prompt de auto-frame-rate podem abrir sem nada
focado; **o D-pad não faz nada e só o Back funciona**.

**Solução:** promover o helper a uma função `internal` partilhada em `ui/components/FocusTrap.kt` e
usá-la nos quatro sítios (é o mesmo achado que **UX-08** no mini-guia).

### PLY-05 — 🟡 `MultiviewState` cria um scope que nunca é cancelado (confirma DAT-19)

**`features/multiview/MultiviewState.kt:93,167,238-249`** — `releaseAll()` cancela os jobs de tune
e aposenta os motores, mas **nunca cancela o `SupervisorJob`** que sustenta o `cleanupScope`. Um
bloco `cleanupScope.launch` suspenso dentro de `retired.await()` sobrevive à libertação (a verificação
`released` só corre *depois* do await). A grelha é composition-owned e descartada com o ecrã, logo
o scope, o grafo de jobs e o `MultiviewState` capturado **sobrevivem-lhe**.

### PLY-06 — 🟡 A escrita "imediata" do histórico ao escolher no guia é cancelada pelo write *debounced* do próprio tune

**`features/live/LiveViewModel.kt:1713-1717,2202-2223,2301-2314`**

```kotlin
1713:  fun watchFromGuide(channel: ChannelEntity) {
1714:      zapList.armFor(channel)
1715:      ensurePlaying(channel)                          // → playChannel → liveTuner.launch { … tuneChannel … }
1716:      recordLiveHistory(channel, immediate = true)
1717:  }
```
```kotlin
2301:  private fun recordLiveHistory(channel: ChannelEntity, immediate: Boolean = false) {
2302:      historyJob?.cancel()                           // ← cancela o que está em curso
2303:      historyJob = viewModelScope.launch {
2304:          if (!immediate) delay(HISTORY_DEBOUNCE_MS)   // 5 000 ms
```

O doc nas linhas 1706-1708 declara a intenção: *"history is written straight away: this is a
deliberate one-shot pick … and such a channel was not reliably landing in History."* Mas a escrita
**não está ordenada face ao tune**: com `Dispatchers.Main.immediate`, o bloco de recovery corre
*sem despacho* até à primeira suspensão real, e depois o write imediato arranca e suspende num
`currentProfileId()`. O job de recovery retoma, e ao chegar a `tuneChannel` → `recordLiveHistory(channel)`
→ **`historyJob?.cancel()` cancela o write imediato em curso** e agenda um deferred de 5 s.

**Impacto:** a escolha deliberada é rebaixada a write debounced, e um zapping dentro da janela de 5 s
cancela-o outra vez — **o canal nunca chega ao Histórico**. É dependente de timing (o write imediato
costuma ganhar por ter uma leitura de DataStore a atravessar), o que explica exactamente o sintoma
*"not reliably landing in History"*.

**Solução:** tornar "immediate" **sticky**, para que uma chamada debounced posterior não o possa
rebaixar (um flag `historyImmediate` que só a chamada `immediate = true` pone).

### PLY-07 — 🟡 A protecção do zap-delay **não cobre a primeira** request de uma rajada

**`features/live/ZapTuneDelay.kt:3-7`** · **`features/live/LiveViewModel.kt:1721-1768`**

```kotlin
3:  /** A single deliberate step opens now; a held key/burst opens only the final selection after 500 ms. */
6:  internal fun zapTuneDelayMs(lastPressMs: Long?, nowMs: Long): Long =
7:      if (lastPressMs == null || nowMs - lastPressMs !in 0 until ZAP_ISOLATED_PRESS_MS) 0L else ZAP_TUNE_DELAY_MS
```

A justificação (`LiveViewModel.kt:1726-1741`) é que **cada** abertura numa rajada é o que faz um
panel Xtream bloquear a conta (HTTP 458) ou um portal Stalker devolver 429. Mas `zapTuneDelayMs`
devolve `0` para a **primeira** pressão de qualquer rajada — a decisão tem de ser retrospectiva, a
app não pode saber que o utilizador vai manter a tecla premida.

Concretamente, manter CH+ durante 3 canais produz: request do ch2 em t=0 (**desprotegida**), ch2
cancelado aos t=170 ms, request do ch3 aos t=670 ms, request do ch4 aos t=840 ms → **3 requests ao
provider**, sendo a primeira exactamente a que a mitigação existe para evitar.

**Efeito secundário do mesmo código:** `_previewChannel` / `_playingChannel` são movidos nas linhas
1753-1754, **antes** de o stream abrir — durante até 500 ms (mais o tempo de abertura) a app reporta
um canal que não está no ecrã nem é audível. (É a causa de PLY-01.)

**Solução:** decidir "pressão isolada" no **key release** em vez da próxima pressão — o HUD já recebe
as duas arestas (`PlayerHud.kt:485-487` só age no `KeyDown`).

### PLY-08 — 🟡 `GroupedChannelPagingSource` nunca desregista o callback de invalidação do delegate (confirma DAT-01)

**`features/live/GroupedChannelPagingSource.kt:21-23,50-51`**

`registerInvalidatedCallback` nunca é emparelhado com `unregisterInvalidatedCallback`. O tracker de
invalidação do Room mantém o delegate vivo, e o callback do delegate mantém uma **referência forte**
a este `GroupedChannelPagingSource` — com o seu `snapshot` (**todas** as linhas da playlist) e as
lambdas `include`/`identity`/`preferred`. O pager é recriado a cada mudança de chave/pesquisa/ordem
(`LiveViewModel.kt:923-934` → `flatMapLatest` → novo `Pager` → nova source), pelo que **cada**
troque deixa para trás mais um snapshot completo do catálogo e mais um callback registado.

### PLY-09 — 🟡 O freeze frame é um `Bitmap` reciclável, propriedade do Core, guardado directamente em estado Compose

**`player/MpvVideoSurface.kt:138-146`** (app) e o contrato do Core

```kotlin
138:  val freeze by player.freezeFrame.collectAsStateWithLifecycle()
141:          bitmap = bmp.asImageBitmap(),
```

O produtor no Core recicla o bitmap anterior **de forma síncrona** logo a seguir a publicar o novo
(`OwnTVPlayer.kt:1626-1632`):
```kotlin
1627:    val previous = _freezeFrame.value
1628:    _freezeFrame.value = next
1629:    if (previous != null && previous !== next && !previous.isRecycled) { runCatching { previous.recycle() } }
```

A app é o **único** consumidor de um bitmap cujo ciclo de vida não controla. Entre o `recycle()` e a
próxima recomposição do Compose, a display-list ainda referencia o bitmap reciclado —
`Canvas.drawBitmap` sobre um bitmap reciclado lança
`IllegalArgumentException: Canvas: trying to use a recycled bitmap`, que **não é apanhável** pela app
e **derruba o processo**.

*HIPÓTESE sobre o gatilho:* depende de se um frame é desenhado entre o recycle e a próxima
recomposição. É mais provável numa TV lenta durante o handoff **mpv→ExoPlayer**, que é exactamente
quando o freeze frame existe. Para confirmar: registar um `Log.w` quando `bmp.isRecycled` é true no
momento do draw, ou procurar `IllegalArgumentException: Canvas: trying to use a recycled bitmap` no
logcat durante as trocas de engine.

**Solução (lado app):** fazer uma **cópia** no momento em que o freeze frame muda (um frame 4K
limitado a `FREEZE_MAX_W`, ~2 MB, e acontece uma vez por handoff):
`remember(freeze) { freeze?.takeIf { !it.isRecycled }?.copy(Bitmap.Config.ARGB_8888, false) }`.

### PLY-10 — 🟡 Cancelar um direct tune cancela a **lookup em curso**, não só a entrada pendente

**`player/PlayerHud.kt:300,310-322,335-357,380-381`** (verificado)

```kotlin
310:  val cancelDirectTune: () -> Unit = {
311:      digitBuffer = ""
312:      submissionRequest = null
313:      tuneOsd = null
314:      heldDigitKeys.clear()
315:      submissionTick++          // ← re-chaveia o effect de submissão ⇒ cancela-o
316:      tuneOsdTick++
317:  }
...
335:  LaunchedEffect(submissionTick) {
336:      val num = submissionRequest ?: return@LaunchedEffect
339:      val result = try { onTuneToNumber?.invoke(num) }
```

`submissionTick` é a **chave do effect**. Incrementá-lo **cancela a corrotina que está à espera de
`onTuneToNumber?.invoke(num)`** — a chamada suspendida ao `LiveViewModel.tuneByNumber`, que está a
fazer uma query ao Room em `Dispatchers.IO` e que depois despacha o tune
(`LiveViewModel.kt:1877` → `zapList.directTune(...) { playChannel(it) }`).

Dois maus resultados concretos: (a) cancelado **antes** do despacho → a lookup é deitada fora e o
HUD **não mostra nada** (sem OSD, sem erro) apesar de o utilizador ter premido OK com um número
válido; (b) cancelado **depois** do despacho → o canal **já mudou**, mas `tuneOsd` nunca é posto (o
effect morreu) → **sem confirmação** do que acabou de ser sintonizado.

Os gatilhos `inert`/`dialog` (`:380-381`) são os surpreendentes: **abrir um diálogo do HUD** (p. ex.
o selector de faixa de áudio) enquanto uma sintonização numérica está a resolver mata a sintonização
em silêncio.

**Solução:** separar "cancelar a entrada pendente" de "cancelar o pedido" — **não** re-chavear o
effect no cancelamento; marcar o pedido em curso como abandonado e descartar o resultado
explicitamente por *request id*.

### PLY-11 — 🟡 O direct tune resolve contra o canal **pendente** do zap, não contra o canal no ecrã

**`features/live/LiveViewModel.kt:1753-1754,1782-1786,1860-1877`**

`zapTo` faz `_previewChannel.value = channel` **imediatamente** (linha 1753) enquanto o stream desse
canal ainda está pendente (até 500 ms + tempo de abertura). `tuneByNumber` usa `_previewChannel.value`
como "o canal que está no ecrã". Durante essa janela:

- Digitar o número do canal **pendente** devolve `restarted = false` ("já está a reproduzir") — mas
  **não** está; o ecrã ainda é o canal anterior. O feedback do HUD fica à espera de um arranque de
  reprodução que não será atribuído correctamente, e o OSD anuncia um canal que não está no ecrã.
- Digitar o número do canal **que está mesmo a reproduzir** entra no ramo `tuned.id != currentChannel.id`
  e chama `playChannel(...)` para ele — um **reinício completo de stream** do canal que já está no
  ecrã, com um intervalo preto visível.

**Solução:** resolver o direct tune contra o canal genuinamente em reprodução
(`_playingChannel.value ?: _previewChannel.value`) e manter o alvo de zap pendente separado.

### PLY-12 / PLY-13 / PLY-14 / PLY-15 — 🟢 Baixa

| ID | Achado | Localização |
|---|---|---|
| **PLY-12** | `versionGroupKeys` é estado **write-only** — 20 000 entradas construídas e destruídas para nada. **Confirma DAT-12** (os dois agentes o encontraram por caminhos diferentes — o que reforça a confiança) | `LiveViewModel.kt:1321,1326-1327,947-948,1876` |
| **PLY-13** | O gesto de *scrub* ancorou a base no offset do **primeiro** press, mas o commit subtrai o offset **actual** → o delta fica curto exactamente pelo *drift* do timeshift enquanto o utilizador segura a tecla | `player/PlayerHudControls.kt:478-486`; `player/LiveScrubGesture.kt:11-40` |
| **PLY-14** | O comentário de `wrappedZapIndex` diz **"CH+ = −1"** mas **todos** os call sites usam **CH+ = +1**. O comportamento é internamente consistente (CH+ desce na lista, a convenção de TV), mas o comentário é o único sítio que diz o contrário — e quem o "corrigir" vai inverter o CH+ para todos os utilizadores | `player/DirectTune.kt:57-72` vs `PlayerHud.kt:486,558-561`, `OwnTVShell.kt:1676-1677`, `LiveZapList.kt:181` |
| **PLY-15** | CH± fica **morto** durante a chamada `play()` de um direct tune: o `pending` (âncora de fallback) é publicado **depois** da chamada suspendida, pelo que `next(delta)` encontra `pending == null` | `features/live/LiveZapList.kt:218-248` |

### 8-B.1 Verificado e sólido (player)

- **Ciclo de vida da surface** (`MpvVideoSurface.kt:66-77`): `surfaceCreated` → `attachSurface`,
  `surfaceDestroyed` → `detachSurface(surface)`. O Core protege com `if (attachedSurface !== surface) return`
  (`OwnTVPlayer.kt:4231-4235`), pelo que a sobreposição old/new durante a recriação por
  `key(surfaceResetToken)` é segura.
- **Cancelamento do zapping** (`LiveViewModel.kt:2060-2076`): `cancelChannelRecovery()` incrementa
  `tuneSelection`, cancela o job de recovery, o job de zap pendente e o trabalho de catch-up/timeshift;
  `TuneSelection.requireCurrent` lança `CancellationException` numa escolha superada, e **toda** a
  operação longa a re-verifica.
- **Handover de engine** (`LiveTuneController.kt:497-524,616-620`): `retireMpvBeforeExo` captura a
  revisão de reprodução **depois** da paragem interna, para que um Pause do utilizador não seja
  confundido com um handover, e espera que o descodificador seja **realmente** libertado antes de a
  próxima engine ligar.
- **Superfície de erro**: `PlayerHud.kt:747-807` desenha um erro estruturado (motivo simples →
  especificação da media → linha crua do motor) com um Retry funcional, e `LiveViewModel.kt:2185-2195`
  faz *toast* de `channel_recovery_failed` em vez de falhar em silêncio.
- **Disciplina de recomposição do HUD**: `PlayerHud.kt:258-261` usa `derivedStateOf` para o tick
  por segundo não recompôr o HUD inteiro; `PlayerHudChrome.kt:509-523` lê a posição por lambda, para
  só a barra de *seek* se redesenhar.
- **Limites de recursos do MultiView**: `MultiviewState.fill` (linhas 160-191) pede um orçamento de
  ligações ao `OpenStreamRegistry.admit` **antes** de sintonizar e mostra a recusa no mosaico em vez
  de deixar um spinner; `releaseAll` (238-249) aposenta todos os mosaicos e todas as reservas.
- **`FailedChannelAttempts`** (`player/FailedChannelAttempts.kt`): todos os mutadores são
  `@Synchronized` no mesmo monitor e a lista está limitada a 10 entradas.

---

## 9. Roadmap — 3 horizontes

`A` = só app · `C` = exige alteração no `OwnTV_Core` externo · `D` = depende de um item de
engenharia pendente (`docs/PLANO_MESTRE_*`, `docs/AUDITORIA_*`, `docs/EVOLUCAO_*`).

### Horizonte 1 — AGORA (Q4 2026 → Q1 2027) · *"fazer os próximos 30 minutos funcionar"*

Tudo aqui é do lado app, pequeno, e ataca o problema do app-vazio e do baseline de acessibilidade.
**Sem dependência do Core ⇒ nada disto espera pelo repositório do Core.**

| # | Item | Sev. | Esforço | Dep. |
|---|---|---|---|---|
| 1 | **REL-05** — corrigir a fórmula do `versionCode` no CI (uma fonte de verdade) | 🔴 | S | A |
| 2 | **REL-01** — publicar os artefactos certos (`OwnTV.apk`, universal, x86_64) e parar de publicar à mão | 🔴 | S | A |
| 3 | **UX-01** — restaurar a porta de entrada da Pesquisa | 🔴 | S | A |
| 4 | **UX-03 / UX-04** — desenhar as separadores e linhas em falta (Downloads, Favoritos, Histórico, Sincronização local) | 🔴 | S | A |
| 5 | **UX-02** — decidir live-only vs. Home completo, e fazer o código e o pill de sync concordarem | 🟠 | S | A |
| 6 | **SEC-03 / QA-03** — falhar o build em vez de assinar com a chave debug | 🟠 | S | A |
| 7 | **F4** — verificador de cobertura de resources por locale (ou migrar as 256 strings para o Core) | 🟠 | M | A |
| 8 | **REL-07 + REL-03** — escrever as secções `v105.*` e fazer o pipeline extrair as release notes | 🟠 | S | A |
| 9 | **DOC-01 / DOC-02** — corrigir os 9 links do README e reescrever a secção de build | 🟠 | S | A |
| 10 | **F3** — apagar `splits.abi` (ou fixar o CI em nomes explícitos) e publicar o arm64 de 32 MB | 🟠 | S | A |
| 11 | **UX-07** — confirmação em dois passos para apagar gravações/transferências | 🟡 | S | A |
| 12 | **UX-08 / UX-09** — `requestBoundedFocus` nos overlays do player + focus trap | 🟡 | S | A |
| 13 | **UX-12** — deixar o Back passar no "Agora" do Home | 🟡 | S | A |
| 14 | **UX-06** — `contentDescription` em todos os botões do mini-player / Audio Mode | 🟡 | S | A |
| 15 | **DAT-01 / DAT-03** — paginação incremental do catálogo Live + `maxSize` limitado | 🟠 | M | A |
| 16 | **DAT-04** — pôr o backup automático em foreground e torná-lo retomável | 🟠 | M | A |
| 17 | **DAT-02** — `WhileSubscribed` nos flows de BD do `LiveViewModel`/`EpgViewModel` | 🟠 | M | A |
| 18 | **UX-10** — parar de ler estado que ticka no scope do shell | 🟡 | M | A |
| 18b | **PLY-01** — o OSD do CH+/CH− tem de mostrar o canal **novo**, não o que saiu | 🟠 | S | A |
| 18c | **PLY-02** — no MultiView, pedir foco ao mosaico novo (o anel e o D-pad discordam) | 🟠 | S | A |
| 18d | **PLY-03** — um requester só na `AudioNowPlayingBar` (o Back não devolve o foco) | 🟡 | S | A |
| 18e | **PLY-04 / UX-08** — `requestFocusRetrying` partilhado nos 4 diálogos com `delay(60)` | 🟡 | S | A |
| 18f | **PLY-09** — copiar o freeze frame antes de o desenhar (risco de crash) | 🟡 | S | A |
| 18g | **PLY-10** — `cancelDirectTune` não pode matar a lookup em curso | 🟡 | S | A |
| 18h | **PLY-11** — o direct tune resolve contra o canal **em reprodução** | 🟡 | M | A |
| 18i | **PLY-06** — tornar "immediate" o write de histórico *sticky* | 🟡 | S | A |
| 18j | **PLY-07** — proteger a **primeira** request de uma rajada de zapping | 🟡 | M | A |
| 19 | **QA-01 / QA-02** — `androidTest` no CI + Dependabot + dependency-review + SBOM + gitleaks | 🟠 | M | A |
| 20 | **SEC-06** — força mínima da passphrase do backup | 🟡 | S | A |
| 21 | **CAT-5** — temporizador de sono com fade-out | — | S | A+C |
| 22 | **CAT-1** — estado "A sua TV está pronta" em vez de um Home vazio | — | S | A |
| 23 | **CAT-2** — estado "O seu guia está vazio" | — | S | A |
| 24 | **CAT-3** — coach de 3 passos de D-pad | — | S | A |
| 25 | **CAT-5 (worker)** — endurecer os Workers publicados: `SHARED_KEY` obrigatório, limite do corpo, `Retry-After` | — | S | A |
| 26 | **CAT-13** — paleta segura para daltónicos + código de forma no ponto de género | — | S | A |
| 27 | **CAT-18** — layout screen-safe (OLED) | — | S | A |
| 28 | **R1** — linha de sponsor no About + declaração pública do custo de operação | — | S | A |

### Horizonte 2 — A SEGUIR (Q2 → Q3 2027) · *"tornar durable a inteligência do motor e dar um emprego ao telemóvel"*

Exigem o Core. Publicar como **uma única release coordenada do Core** (a app e o Mobile consomem a
mesma versão de `core`/`player-core`).

| # | Item | Esforço | Valor | Dep. |
|---|---|---|---|---|
| 29 | **CAT-1** — Auto-Tune Memory (persistir o degrau vencedor por canal) | M | **5** | C + D(B1,B7) |
| 30 | **CAT-7** — persistir as quirks de stream (DataStore-backed `StreamQuirkStore`) | M | 4 | C + D(B7,B8) |
| 31 | **CAT-3** — catálogo demo (só domínio público) no primeiro arranque | M | 5 | C (pequeno) |
| 32 | **CAT-2** — TV-Remote (segundo ecrã) sobre o servidor companheiro LAN | M-L | **5** | C + A |
| 33 | **CAT-6** — veredicto de saúde por canal ("porque foi mau?") | M | 4 | C + D(O4) |
| 34 | **CAT-5** — health check do provider (meterizado contra o limite medido) | M | 4 | C |
| 35 | **CAT-16** — pilha de retoma da casa (com PIN, excluída de Kids) | M | 4 | C |
| 36 | **CAT-12** — watchlist + relay de conhecimento zero | M | 4 | C + Worker |
| 37 | **CAT-13** — API de plugins de metadata (template + mapeamento) | M-L | 4 | C + A |
| 38 | **CAT-11** — pesquisa por voz (escondida sem recognizer; cloud-vs-offline declarado) | M | 3 | A |
| 39 | **CAT-15** — códigos de canal partilháveis (alias + número; nunca o URL) | M | 3 | C + A |
| 40 | **CAT-19** — fetch diferencial do EPG (ETag/Range) + janela só na tomada | M | 3 | C |
| 41 | **Acessibilidade, passe 1** — semântica TalkBack nos componentes reutilizáveis + grid do EPG | M | 5 | A |
| 42 | **Presets Senior / Surdo** (construídos sobre o `SimpleModePreferences` existente) | M | 4 | A + C |
| 43 | **DAT-05..DAT-14** — o resto das correções de dados/performance | M | 4 | A |
| 43b | **PLY-05 / PLY-08 / PLY-12** — higiene de recursos: scopes não cancelados, callbacks não desregistados, cache write-only | S | 3 | A |
| 43c | **F7** — apagar dependências mortas (`navigation-compose`, `room-compiler`, `media3-exoplayer`, `room-ktx`, `room-paging`, `tooling-preview`, `material-icons-core`) | S | 2 | A |
| 44 | **F15 fases 0-4** — extrair estado/UI do shell, partir o ecrã de definições, partir o `LiveViewModel` | L | 4 | A |
| 45 | **SEC-01/04/05/08/09** — as correcções de segurança que exigem Core | M | 5 | C |
| 46 | **R3 / R5** — **linha de negócio**: licença de marca + white-label + contratos de suporte | M | **5** | — |

### Horizonte 3 — DEPOIS (Q4 2027 → 2028) · *"as apostas grandes"*

| # | Item | Esforço | Valor | Dep. |
|---|---|---|---|---|
| 47 | **CAT-14** — limite de banda / selecção de variante (só na selecção na v1) | L | 4 | C + D(Fase 3A) |
| 48 | **CAT-8** — classificações reais a substituir o regex do modo Kids | L | 4 | C |
| 49 | **CAT-9** — recomendações por EPG, locais e explicáveis | L | 4 | C |
| 50 | **CAT-20** — salto de publicidade (só live, off por omissão, com undo) | L | 3 | C |
| 51 | **Lite flavor** para boxes de 1-2 GB (sem Glass/TMDB/Multiview/downloads; só Exo) | M | 4 | A |
| 52 | **Magnificador de foco** (específico de TV: ampliar a linha focada) | M | 3 | A |
| 53 | **Chromecast** (só se uma 2.ª app receptora for aceitável) | L | 4 | C + nova app |

### 9.1 As dependências que realmente mordem

1. **O pipeline de release tem de ser reparado ANTES de tudo o resto.** Enquanto a tag não constrói
   (REL-05) e o artefacto publicado é o errado (REL-01), **nada do roadmap chega ao utilizador**.
2. **O `Auto-Tune Memory` não pode ir à frente sem B1 e B7.** B7 é *"DASH learning from another
   channel overwrites an explicit HLS URL"* — persistir as lições transformaria um bug de sessão num
   bug **persistente** que sobrevive a reinícios e envia um canal para o parser errado.
3. **O veredicto de saúde não pode ir à frente sem O4** (`ThroughputTracker` a ignorar `isNetwork`;
   `ExoStreamStats` a apresentar throughput de transporte como bitrate). Um veredicto construído
   sobre uma métrica que confunde taxa de download com bitrate vai culpar o Wi-Fi do utilizador pelo
   CDN do provider.
4. **F4 (i18n) é uma decisão de arquitectura, não um bug point-fix.** Migrar as 256 strings para o
   Core resolve de vez; o verificador de resources é a mitigação rápida.
5. **O `Auto-Tune Memory`, as quirks persistidas e o veredicto de saúde querem todos uma nova
   superfície de persistência no Core.** Landar **uma** família de stores DataStore-backed
   (`ForceMpvStore` / `AudioOnlyStore` / `ArchiveDecodeStore` já establecem o padrão) e reutilizá-la.
6. **O demo e o coach são uma só feature** — o coach precisa de conteúdo sobre o qual ensinar.
7. **O TV-Remote e o relay são um só transporte** — o servidor LAN e o relay Worker devem falar o
   **mesmo protocolo**.
8. **As classificações não devem ir à frente sem override parental e sem explicação.** Uma
   classificação que esconde o canal errado é um incidente de suporte e, no conteúdo errado, legal.
9. **Horizonte 3 (exceto decisões de negócio) exige um Core saudável.** O trabalho de engenharia
   pendente é **pré-requisito, não uma via paralela.** E a app está hoje **à frente** do snapshot do
   Core em vários pontos — a CI **tem de correr** `:OwnTV_Core:core:testDebugUnitTest` e
   `:OwnTV_Core:player-core:testDebugUnitTest` (já o faz, mas **não** no `release` nem no `x86_64`).

---

## 10. Deduplicação — o que **já** estava registado (para não repetir trabalho)

| ID anterior | Onde | Relação com esta ronda |
|---|---|---|
| R4-B01 | restauro de backup altera `isKids`/`pinHash` | Confirmado, não repetido |
| R4-B02 / R4-B03 | índices desfasados `rows` vs `items` em Downloads/Gravações | Confirmado; **agora com mais contexto** (UX-03 mostra que a lista de Downloads nem sequer tem separadores) |
| R4-B04 | perfis Kids deixam passar canais sem `categoryId` | Confirmado |
| R4-B05 / R4-B06 | revogação do pareamento; `id` do cliente substitui um dispositivo | Confirmado |
| R4-B07 | **sem limite de tentativas no PIN** | **Confirmado**; esta ronda acrescenta o KDF fraco e a comparação não-atempada (SEC-04) |
| R4-B08 | perfil Kids sem PIN pode ser desbloqueado | Confirmado |
| R4-B09 / R4-B10 | acções de gravação / catch-up sem `try/catch` | Confirmado |
| R4-B11 / R4-B12 | credenciais a apps externas; `FileProvider` amplo | Confirmado (SEC-08) |
| R4-B13 | deep link `owntv://` é `BROWSABLE` | Confirmado (SEC-11) |
| R4-B14 | cleartext global + armazenamento amplo | **Confirmado e escalado** (SEC-01: as credenciais Xtream vão no path e na query) |
| R4-B16 | actualização sem checksum | Confirmado (SEC-09) |
| R4-B17 | `FLAG_KEEP_SCREEN_ON` limpo fora do bloco | Confirmado |
| R4-B20 | tag sem keystore publica APK debug | Confirmado (QA-03) + **SEC-03** (o bug do build local) |
| R4-B21 / R4-B22 / R4-B23 | permissões do CI; verificação do Gradle; testes só em `standardDebug` | Confirmados (QA-01, QA-02, F8) |
| R4-B24 | dependências atrasadas e não usadas | Confirmado (F7 — e acrescido: `media3-exoplayer` dá um segundo voto na versão) |
| R4-B25 | 1,8 GB de snapshots em `.gradle/` | Confirmado com medição exacta (REL-09) |
| R4-B26 | `TMDB_EDGE_KEY` embutida nas APKs | Confirmado (SEC-02) |
| BUG-P3-03 | foco em overlays do guia não refoca | Confirmado (UX-08 — com o número exacto: 180 ms) |
| BUG-P1-01..P5-01, INN-01..06, MEL-01..09, TEC-01..05, UX-01..05 | rondas 1-2 | Fora do âmbito desta ronda (já cobertos); **não re-propostos** |
| `UPSCALING-BETA-01` | `docs/MELHORIAS_FUTURAS_PENDENTES.md` | **Excluído explicitamente** — não re-proposto |

### 10.1 O que esta ronda acrescenta de **novo** (e não estava em lado nenhum)

| # | Achado novo | Porquê é novo |
|---|---|---|
| 1 | **REL-05** — o passo "Compute version" do CI rebenta em tags de 4 componentes | Ninguém tinha executado o snippet bash; é a **causa-raiz** de 3 dos achados |
| 2 | **REL-01** — a release publicada só tem o APK 32-bit; o "always-latest" dá 404 | Requer interrogar a API de releases do GitHub |
| 3 | **REL-02** — o CI publica o universal de 52,8 MB em vez do arm64 de 32 MB | Requer medir os artefactos de build |
| 4 | **REL-06/REL-07** — dois esquemas de `versionCode`; changelog sem secções `v105.*` | Cruzamento entre o script do CI e o `build.gradle.kts` |
| 5 | **DOC-01/DOC-02** — 9 links do README para um repo inexistente; token do GitHub Packages obsoleto | Requer verificar os URLs por HTTP |
| 6 | **F4** — 266 strings da app traduzidas em 2 de 26 idiomas | Requer contar os `<string>` por directório de locale |
| 7 | **UX-01/02/03/04** — Pesquisa sem porta; Trending/Continue mortos; Downloads só gravações; Favoritos/Histórico/Sync inalcançáveis | Requer ler a UI, não só o ViewModel |
| 8 | **DAT-01/02/03/04** — catalog paging materializa 50k; 17 `Eagerly`; `MAX_SIZE_UNBOUNDED`; backup automático morto | Requer seguir o fluxo de dados |
| 9 | **QA-09** — `mapping.txt` nunca arquivado | Requer seguir os artefactos do CI |
| 10 | **F3** — `splits.abi` × `ndk.abiFilters` produz 3 APKs por flavor, e o CI publica um ao acaso | Requer ler as **fontes do AGP** |
| 11 | **F8** — `libmpv` buscada sem verificação de integridade | Requer ver a ausência de `verification-metadata.xml` |
| 12 | **R3** — GPLv3 não restringe a **marca**: linha de negócio de white-label/trademark | Análise de produto, não de código |

---

## 11. Limitações desta análise

1. **Análise estática + verificação de artefactos publicados.** Não houve build, testes nem execução
   em dispositivo. Nada foi reproduzido numa box real.
2. **O Core é um snapshot.** Todas as referências ao Core apontam para
   `.gradle/core-snapshot/e2081e78…/OwnTV_Core` (o hash que `core-source-lock.json` fixa) — **não** para
   o repositório upstream, que pode já ter divergido.
3. **Linhas verificadas vs. linhas citadas.** Os achados com maior impacto (REL-01/02/05/06, F4,
   UX-01/02/03/04, SEC-04/08, DAT-01/02/03) foram **releídos e contados** pelo conselho. Os restantes
   trazem a linha exacta tal como o agente a leu, e devem ser confirmados linha a linha antes de
   qualquer correcção.
4. **A verificação bash foi feita em Git Bash 5.x**, não no runner exact do GitHub Actions. O
   comportamento do `read` e da expansão aritmética do bash é o mesmo, mas o *shell por omissão* do
   GitHub (`bash -e {0}`) é que determina se o passo morre ou produz `CODE=1` — ambos os cenários
   estão documentados em REL-05.
5. **Os achados de AGP** (F3) foram verificados contra as fontes do **AGP 9.4.1** no cache local.
   O conteúdo exacto dos `.so` dentro dos APKs x86_64 com nome "arm" não foi verificado byte a byte
   (o flavor x86_64 nunca foi compilado nesta máquina) — marcado como tal.
6. **Um bug do fork pode ter sido já corrigido a montante.** Este checkout tem um Core snapshot
personalizado; o upstream pode ter resolvido parte destas questões.
7. **As métricas de retenção não puderam ser medidas** — a app não tem servidor e não tem
   instrumentação de funil (F8 no §retention). Qualquer afirmação sobre *onde* os utilizadores
   abandonam é uma **hipótese fundamentada pelo código**, não uma medição.
8. **A secção 7 (produto) é opinião fundamentada**, não medição de mercado. As comparações com
   TiviMate / Plex / VLC baseiam-se no conhecimento geral da categoria e nas funcionalidades
   verificáveis do código — **não** em relatórios de quota de mercado.

---

## 12. Conclusão

O OwnTV tem, hoje, **um codebase melhor do que a sua distribuição**. A app é limpa, rápida, bem
testada, bem internacionalizada e tecnicamente acima da categoria (duas engines, escada de fallback
determinística, três famílias de source em paridade, privacidade local-first, 26 idiomas, 0 dívida
de TODO).

Mas o que o utilizador **recebe** hoje é: um link de download que dá 404, um APK de 32 bits quando
tem um dispositivo de 64 bits, release notes vazias, uma app cujas 24 línguas estão a meio em
inglês, e **quatro funcionalidades centrais sem porta de entrada** (Pesquisa, Now Trending,
Downloads de filmes, Favoritos/Histórico).

Nenhum desses problemas exige uma linha de arquitectura nova. Todos são **decisões de pipeline e de
UI**. A ordem correcta é:

1. **Reparar o pipeline de release** (REL-05 → REL-01 → REL-02 → REL-06/07) — sem isto, nada do
   resto chega ao utilizador.
2. **Reparar as portas de entrada da UI** (UX-01 → UX-03/04 → UX-02) — são features inteiras,
   desabilitadas por omissão.
3. **Reparar a cobertura de idiomas** (F4) — é a promessa de produto que está a ser quebrada.
4. **Depois, a engenharia de fundo** (dados, performance, segurança, refactorização) — que é
   importante, mas **secundária** em relação a "a app instala e mostra-se como promete".

O resto do trabalho — as 20 inovações, os 3 horizontes, a linha de negócio de marca e suporte — é
**executável depois disso**, e o produto tem posicionado para o sustentar.

---

# ═══════════════════════════════════════════════════════════════════════
# PARTE II — RONDA 6 (19/10/2026)
# Bugs adicionais, reavaliação do Council e veredito final
# ═══════════════════════════════════════════════════════════════════════

---

## 13. Ronda 6 — método

### 13.1 O que muda nesta ronda

A Ronda 5 provou que a app está **bem construída e mal distribuída**. A Ronda 6 faz três coisas que
as rondas anteriores não fizeram:

1. **Escava as áreas que faltavam** — o **Core** (1 007 ficheiros: base de dados, parsers, rede,
   sync, backup, motores), os **ecrãs de definições**, o **wizard de setup**, as **superfícies
   secundárias** (pesquisa, transferências, gravações, personalização, EPG) e o **tooling**.
2. **Reavalia cada achado anterior** contra o código actual, com veredicto explícito
   (✅ válido · 🔧 já corrigido · ⚠️ a actualizar · ❌ falso positivo · 🔁 duplicado · ❓ não verificável).
3. **Revê as *soluções***, não só os diagnósticos — e **corrige as correcções que estariam erradas**.

### 13.2 Como esta ronda foi executada

> **Nota de transparência:** a Ronda 6 foi lançada com **6 agentes especializados** (Core, Definições,
> Setup/Onboarding, Superfícies secundárias, Tooling/i18n/Edge, e Conselho de reavaliação). **Todos
> seis falharam por uma queda de ligação** (`getaddrinfo ENOTFOUND opencode.ai`) a meio da execução.
> **Nada do que eles tinham produzido foi perdido, porque nada tinha sido entregue.**
>
> O conselho **re-executou a ronda por conta própria**, directamente contra o código, cobrindo as
> mesmas seis áreas. Todas as afirmações da Parte II são **verificadas por leitura directa**, com
> ficheiro e linha — nenhuma vem de um agente que não chegou a correr. Onde a cobertura ficou mais
> fina do que o previsto, isso está declarado em §17.

### 13.3 Verificação directa do conselho da Ronda 6

| O que foi verificado | Resultado |
|---|---|
| Release publicada (API do GitHub, re-consultada) | **Inalterada** — continua só `armeabi-v7a`, corpo vazio → **REL-01 mantém-se** |
| `SettingsSearchCoverageTest` + o bloco `searchResults` | **Sólido** — deriva de `VIDEO_QUICK_ROWS` + entradas `bespoke`; a pesquisa das definições está genuinamente mantida |
| Varredura de todos os `catch (X: Exception)` (243 ficheiros) | **87 sítios** re-lançam `CancellationException` correctamente; restam **3** sem guarda (§14, CAN-01) |
| R4-B02 / R4-B03 (índices `rows` vs `items`) | **Mantêm-se** — diagnóstico mais preciso e **melhor solução** (§15.2) |
| R4-B09 (acções de gravação sem `try/catch`) | **Mantém-se** — verificado linha a linha |
| R4-B10 (`continueAfterCatchup` sem `try/catch`) | **Mantém-se** — verificado linha a linha |
| `BackupCrypto.deriveKey` — validação das iterações | **FALTA** → nova **SEC-12** |
| Assimetria escrita/leitura do KDF do contentor | **CONFIRMADA** → nova **SEC-13** |
| `LocalSyncClient` — cópias do backup em heap | **CONFIRMADA** → nova **SEC-14** |
| `BackupManager` — falhas de leitura silenciadas | **CONFIRMADA** → nova **DATA-01** |
| `OpenStreamRegistry` e o orçamento de ligações | **O P2 do Council anterior é um FALSO POSITIVO** (§15.3) |
| `lastZapPressMs` não reposto numa escolha deliberada | **CONFIRMADO** → novo **ZAP-01** |
| `SettingsScreen` — leituras de `StateFlow` na raiz | **44 confirmadas** → novo **PERF-01** |
| `HomeScreen` — auto-avanço do hero a 80 ms | **CONFIRMADO** → novo **PERF-02** |
| `Room` — índices dos DAOs vs. esquema | **Verificado** (§14, DATA-02) |
| Ecrãs de definições — chaves órfãs nas duas direcções | **Verificado** (§14, SET-01) |
| Wizard de setup — máquina de estados e focus | **Verificado** (§14, SET-03) |
| Transferências/descargas — integridade do ficheiro parcial | **Verificado** (§14, DWN-01) |

---

## 14. Bugs e melhorias novos da Ronda 6

### SEC-12 — 🔴 O número de iterações PBKDF2 vindo do ficheiro importado **não é validado** → DoS de CPU

**Severidade: ALTA** · **Confiança: alta** · **Verificado byte a byte no Core**

**Localização:** Core `backup/BackupCrypto.kt:64-70`, `backup/BackupContainer.kt:238`,
`backup/BackupManager.kt:798`

```kotlin
// BackupCrypto.kt:64-70
/** Derives the key from a [crypto] root block + passphrase. Returns null if the block is malformed. */
fun deriveKey(passphrase: String, crypto: JSONObject?): SecretKey? {
    crypto ?: return null
    val salt = runCatching { unb64(crypto.getString("salt")) }.getOrNull() ?: return null
    val iters = crypto.optInt("iterations", ITERATIONS)          // ← SEM VALIDAÇÃO
    return deriveKey(passphrase.toCharArray(), salt, iters)
}
```

O cabeçalho do contentor é **plaintext por desenho** — o próprio Core documenta isso
(`BackupContainer.kt:30`): *"The header must stay plaintext: restore needs the salt and iteration
count to derive the key"*. E `BackupCrypto.cryptoBlock()` escreve explicitamente
`put("iterations", ITERATIONS)`.

**Impacto**

Um ficheiro `.own` fabricado (ou simplesmente editado à mão) com `"iterations": 2147483647` faz a app
executar **~2,1 mil milhões de iterações PBKDF2-HMAC-SHA256** — `SecretKeyFactory.generateSecret`
com um `PBEKeySpec` de iterações massivas. Numa box Android TV de gama baixa (o alvo explícito
desta app) isso são **horas de CPU com a UI bloqueada**: a app fica em ANR e só regressa quando o
cálculo acaba. É um **DoS local** que só precisa de o utilizador abrir um ficheiro.

**Dois caminhos de importação são afectados:**
- `BackupContainer.kt:238` — `BackupCrypto.deriveKey(pass, header)` (o contentor `.own` cifrado);
- `BackupManager.kt:798` — `BackupCrypto.deriveKey(pass, crypto)` (o formato legado `.json` com
  campos selados).

**Solução**

```kotlin
// BackupCrypto.kt — validar o cabeçalho ANTES de derivar
private const val MIN_ITERATIONS = 100_000
private const val MAX_ITERATIONS = 2_000_000

fun deriveKey(passphrase: String, crypto: JSONObject?): SecretKey? {
    crypto ?: return null
    if (crypto.optString("scheme", SCHEME) != SCHEME) return null
    if (crypto.optString("kdf", KDF) != KDF) return null
    val iters = crypto.optInt("iterations", ITERATIONS)
    if (iters !in MIN_ITERATIONS..MAX_ITERATIONS) return null      // ← recusa o DoS
    val salt = runCatching { unb64(crypto.getString("salt")) }.getOrNull() ?: return null
    if (salt.size < 8) return null
    return deriveKey(passphrase.toCharArray(), salt, iters)
}
```

`MAX_ITERATIONS` tem de ser **≥ `ITERATIONS`** com folga, para que um backup legítimo escrito por
uma versão futura (com mais iterações) ainda seja aceite. Alternativamente, aceitar apenas
`iterations == ITERATIONS` e versionar o formato com um número de esquema próprio.

**Teste de regressão obrigatório:** um `.own` com `"iterations": 2147483647` tem de ser **rejeitado
em <100 ms**, sem nunca chamar a `SecretKeyFactory`. Medir o tempo — não basta "não-crasha".

---

### SEC-13 — 🟠 O KDF é escrito com a **constante** e lido com o **valor do ficheiro** — assimetria perigosa

**Severidade: MÉDIA** · **Confiança: alta** · **Verificado**

```kotlin
// BackupContainer.kt:174-176  — ESCRITA
val salt = BackupCrypto.newSalt()
val key = BackupCrypto.deriveKey(pass.toCharArray(), salt, BackupCrypto.ITERATIONS)   // ← a CONSTANTE
val header = BackupCrypto.cryptoBlock(salt).put("scheme", CONTAINER_SCHEME).toString()...

// BackupContainer.kt:238      — LEITURA
val key = BackupCrypto.deriveKey(pass, header) ?: error("Corrupt backup header")         // ← o VALOR DO FICHEIRO
```

Hoje os dois coincidem (o `cryptoBlock` escreve `ITERATIONS`), por isso o round-trip funciona. Mas:

1. **A escrita ignora o cabeçalho.** Se amanhã se subir `ITERATIONS` para 400 000, a escrita passa a
   usar 400 000 e a leitura continua a usar os 210 000 que estão no cabeçalho dos ficheiros antigos →
   **todos os backups existentes deixam de abrir**, com um `AEADBadTagException` traduzido como
   *"palavra-passe errada"* — um diagnóstico **enganador** para o utilizador.
2. A leitura não valida o esquema nem o KDF, por isso aceita qualquer valor (ver SEC-12).

**Solução:** fazer a **escrita usar exactamente o valor que vai no cabeçalho** (uma única fonte de
verdade) e **versionar o formato**:

```kotlin
// escrita
val iters = BackupCrypto.ITERATIONS
val key = BackupCrypto.deriveKey(pass.toCharArray(), salt, iters)
val header = BackupCrypto.cryptoBlock(salt, iters).put("scheme", CONTAINER_SCHEME)...

// leitura — aceitar iterações dentro de uma janela, não apenas a constante
if (iters < MIN_ITERATIONS || iters > MAX_ITERATIONS) return null
```

E acrescentar `CONTAINER_SCHEME = "container-aes-gcm-v2"` quando o formato mudar, para que um
ficheiro antigo seja reconhecido como tal e não falhe como "palavra-passe errada".

---

### SEC-14 — 🟠 A sincronização local materializa o backup inteiro **cinco vezes** em heap

**Severidade: MÉDIA-ALTA** · **Confiança: alta** · **Verificado**

**Core `sync/local/LocalSyncClient.kt:91-92` e `:114`**

```kotlin
 91:    val encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
 92:    post(url(address, port, "/backup"), credential, "data:application/octet-stream;base64,$encoded", "text/plain")
...
114:    val bytes = body.toByteArray(Charsets.UTF_8)
```

**Cadeia de cópias**, para um backup de 30 MB:

| # | Cópia | Tamanho |
|---|---|---|
| 1 | `file.readBytes()` | 30 MB (`ByteArray`) |
| 2 | `Base64.encodeToString(...)` | 40 MB (`String`; em UTF-16, até 80 MB) |
| 3 | `"data:...;base64,$encoded"` (concatenação) | +40 MB (`String` novo) |
| 4 | `body.toByteArray(UTF_8)` | +40 MB (`ByteArray`) |
| — | **pico total** | **~150–190 MB de heap** |

Numa box de 1–2 GB, com a app a reproduzir (descodificadores + buffers de vídeo), isto é um **OOM
quase garantido** — e é exactamente a feature que o README vende como *"configure a sua TV a partir
da que já tem"*.

**Solução:** enviar o ficheiro em **streaming**, sem materializar:

```kotlin
// LocalSyncClient — escrever o corpo directamente no socket
val conn = URL(url(address, port, "/backup")).openConnection() as HttpURLConnection
conn.requestMethod = "POST"
conn.doOutput = true
conn.setRequestProperty("Content-Type", "application/octet-stream")
conn.setRequestProperty("X-Companion-Pin", credential)
conn.setFixedLengthStreamingMode(file.length())          // ← sem buffering em memória
conn.outputStream.use { out -> file.inputStream().use { it.copyTo(out, 64 * 1024) } }
```

E fazer o servidor (`CompanionHttpServer`) aceitar o corpo binário cru em vez do `data:` URI
Base64 — o que também **reduz o payload em ~25 %** (o Base64 cresce 33 %).

---

### DATA-01 — 🟠 Uma falha de leitura durante a exportação é silenciosamente convertida em **lista vazia**

**Severidade: MÉDIA** · **Confiança: alta** · **Verificado**

**Core `backup/BackupManager.kt:216, 321, 323, 325, 342`**

```kotlin
216:        val rows = runCatching { db.playbackPrefsDao().getAllOnce() }.getOrDefault(emptyList())
321:        val selections = runCatching { dao.allSelectionsOnce() }.getOrDefault(emptyList())
323:        val timings   = runCatching { dao.allTimingsOnce() }.getOrDefault(emptyList())
325:        val links     = runCatching { dao.allLinksOnce() }.getOrDefault(emptyList())
342:        val cacheRows = runCatching { dao.allCacheOnce() }.getOrDefault(emptyList())
```

**Impacto:** a exportação **conclui com sucesso** mas omite silenciosamente as preferências de
reprodução, as selecções de canais, os tempos, as ligações e a cache de metadados. O utilizador
acredita ter um backup completo; no restauro, essas secções voltam vazias. É o **caminho de perda de
dados mais silencioso que existe nesta app** — e ninguém é avisado.

**Solução:** recolher as falhas e **ou** falhar a exportação **ou** marcar o ficheiro como
incompleto, de forma visível:

```kotlin
val failures = mutableListOf<String>()
val rows = runCatching { db.playbackPrefsDao().getAllOnce() }
    .onFailure { failures += "playbackPrefs: ${it.message}" }
    .getOrDefault(emptyList())
...
if (failures.isNotEmpty()) manifest.put("incomplete_sections", JSONArray(failures))
```

E a UI de exportação deve mostrar **"Backup concluído com 2 secções em falta"**, não *"Backup
concluído"*.

---

### SEC-15 — 🟡 O export **sem cifra** grava URLs com credenciais em texto claro

**Severidade: MÉDIA (risco de API do Core)** · **Confiança: média-alta**

O Core consegue exportar um ZIP **sem** cifragem (`BackupManager.kt:1502-1519`, segundo a nota de
decisão do Council anterior) e, nesse caminho, grava a `url` e a `epgUrl` **em texto claro** — e uma
URL Xtream é da forma `http://host/live/UTILIZADOR/PALAVRA_PASSE/123.ts`.

O ecrã normal de exportação **exige** palavra-passe (`BackupViewModel.kt:149` recusa uma palavra-passe
vazia) e a sincronização local cifra sempre — pelo que **não descreve o fluxo normal da UI**. É um
risco da **API do Core** e de integrações futuras ou legadas.

**Solução:** detectar credenciais embebidas na URL e **impedir a exportação sem cifra** para essas
sources, ou redigir o segmento de credenciais com um marcador e registar que foi redigido.

```kotlin
private fun URL.hasInlineCredentials(): Boolean =
    userInfo != null || query.contains("password=") || path.contains(Regex("/live/[^/]+/[^/]+/"))
```

---

### PERF-01 — 🟠 O `SettingsScreen` lê **44 `StateFlow`s** no composable raiz

**Severidade: MÉDIA** · **Confiança: alta** · **Contado por script sobre o ficheiro**

```
collectAsStateWithLifecycle dentro do composable raiz SettingsScreen (linhas 190-1559): 44
collectAsStateWithin no mesmo intervalo                                              :  0
total no ficheiro                                                                    : 51
```

Cada um dos 44 fluxos que muda **recompõe o composable raiz inteiro** — que tem ~1 370 linhas e 71
sub-composables — e o `SettingsScreen` vive dentro do `OwnTVShell`, que é um composable de ~1 900
linhas. Na prática, mudar o valor de uma preferência **reconstrói a tela de definições inteira**.

**Solução:** mover cada leitura para o sub-composable que a usa.

```kotlin
// ANTES — no raiz
val accent by vm.accent.collectAsStateWithLifecycle()
AccentRow(accent = accent)

// DEPOIS — no sub-composable
@Composable private fun AccentRow(vm: SettingsViewModel) {
    val accent by vm.accent.collectAsStateWithLifecycle()
    …
}
```

É um **refactor mecânico e seguro** (não muda comportamento) e é o item de maior impacto de
performance da app depois do `DAT-01`/`DAT-02`. Deve ser feito **junto** com a Fase 1 do plano de
refactorização do shell (§6, F15).

---

### PERF-02 — 🟠 O hero "Now Trending" avança a **12,5 Hz** — e está dormente por causa do UX-02

**Severidade: MÉDIA** · **Confiança: alta** · **Verificado**

**`features/home/HomeScreen.kt:663-676`**

```kotlin
663:    LaunchedEffect(activeIndex, manuallyPaused, actionButtonsFocused, resetClock, items.size) {
...
668:        while (true) {
669:            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L
670:            progress = (startProgress + (1f - startProgress) * elapsedMs.toFloat() / duration).coerceIn(0f, 1f)
671:            if (elapsedMs >= duration) break
672:            kotlinx.coroutines.delay(80L)          // ← 12,5 Hz
673:        }
```

`progress` é estado de Compose lido no composable → **12,5 recomposições por segundo** do hero
(fundo, poster, título, elenco, badges).

**A interacção com o UX-02 é o achado mais interessante desta ronda:** como `feed.load(profileId,
liveOnly = true)` devolve sempre `trendingItems` **vazio**, o hero **nunca chega a compor** — e o loop
**nunca corre**. Ou seja:

- **Hoje** o custo é zero — mas a **funcionalidade está morta** (UX-02).
- **Assim que o UX-02 for corrigido** (passando `liveOnly = false`), o loop de 12,5 Hz acorda
  imediatamente, em todas as boxes, mesmo sem o utilizador estar a olhar para a row.

**As duas correcções têm de ser feitas em conjunto.** Corrigir o UX-02 sem corrigir isto reintroduz
um problema de performance e bateria.

**Solução:** animar com o `Animatable`/`animateFloatAsState` do Compose (que corre no *frame clock*
do sistema e **não** provoca recomposição por frame) e parar o avanço quando a row não está visível:

```kotlin
val progress by animateFloatAsState(
    targetValue = if (active) 1f else 0f,
    animationSpec = tween(durationMillis = intervalMs.toInt(), easing = LinearEasing),
)
```

---

### ZAP-01 — 🟡 A primeira pressão de CH+ depois de uma **escolha deliberada** herda o atraso de 500 ms

**Severidade: MÉDIA-BAIXA** · **Confiança: alta** · **Verificado**

**`features/live/LiveViewModel.kt:1743-1747` e `:2016-2019`**

```kotlin
1743:    private var lastZapPressMs: Long? = null
1746:        val tuneDelay = zapTuneDelayMs(lastZapPressMs, now)
1747:        lastZapPressMs = now
```

```kotlin
2016:    fun ensurePlaying(channel: ChannelEntity, explicitVersion: Boolean = false) {
2017:        tv.own.owntv.player.ZapTiming.shared.clear()
2018:        startSelectedChannel(channel, explicitVersion)
2019:    }
```

`ensurePlaying` (a **escolha deliberada** de um canal) limpa `ZapTiming.shared` e cancela
`pendingZapTuneJob` — mas **NÃO** repõe `lastZapPressMs`. O único sítio que o repõe é a linha 346,
dentro do reset de sessão (`onBackground`).

**Cenário:** o utilizador prime CH+ (t=0) → `lastZapPressMs = 0`. Escolhe **um canal à mão** às
t=1 s (o `ensurePlaying` não repõe o timestamp). Preme CH+ outra vez às t=1,2 s →
`zapTuneDelayMs(0, 1200)` → `1200 - 0 = 1200`, que está dentro de `0 until 1500` → devolve
**500 ms**. **O canal que o utilizador acabou de pedir não abre logo** — a sensação é de "a app está
lenta depois de eu escolher um canal".

**Solução (uma linha):** repor o timestamp na escolha deliberada, exactamente onde `ZapTiming` já é
limpo:

```kotlin
fun ensurePlaying(channel: ChannelEntity, explicitVersion: Boolean = false) {
    tv.own.owntv.player.ZapTiming.shared.clear()
    lastZapPressMs = null          // ← uma escolha manual não herda o burst anterior
    startSelectedChannel(channel, explicitVersion)
}
```

Isto complementa o **PLY-07** (a primeira request de uma rajada não é protegida): os dois são o mesmo
mecanismo de *burst* visto de dois lados.

---

### CAN-01 — 🟢 Três sítios engolem `CancellationException` (de 90)

**Severidade: BAIXA** · **Confiança: alta** · **Varredura completa dos 243 ficheiros**

Varredura de todos os `catch (X: Exception)` do app, procurando um clause irmão
`catch (cancelled: CancellationException) { throw cancelled }`:

```
Sítios com guarda correcta .................................. 87
Sítios sem guarda ......................................... 3
Sítios falsos-positivos (clause irmão já tratado) ........ 4
```

Os três:
- `features/movies/MovieViewModel.kt:570` — `catch (e: Exception) { Log.w(…); null }` em
  `resolvedUrlOrNull`;
- `features/movies/MovieViewModel.kt:631` — `catch (e: Exception) { Log.w(…); return@launch }`;
- `features/series/SeriesViewModel.kt:917` — `catch (e: Exception) { Log.w(…); null }`.

**Impacto real: BAIXO.** Nos três sítios o bloco termina logo a seguir, pelo que a corrotina morre
de qualquer forma. O que muda é o **log**: um *resolve* Stalker **cancelado** é registado como
*"stalker resolve failed"*, o que mistura um cancelamento com uma falha real no diagnóstico.

**Porquê registar mesmo assim:** a codebase tem **87** sítios que fazem isto correctamente. Estes 3
são uma excepção inconsistente — e a excepção é o que se torna bug quando alguém copia o padrão.

**Solução (uniformizar):**
```kotlin
} catch (cancelled: kotlinx.coroutines.CancellationException) {
    throw cancelled
} catch (e: Exception) {
    Log.w(TAG, "stalker resolve failed movieId=${movie.id}", e)
    null
}
```

---
### DWN-01 — 🟡 A recusa do orçamento de armazenamento **perde o motivo**, e o utilizador vê só "falhou"

**Severidade: BAIXA-MÉDIA** · **Confiança: alta** · **Verificado**

**Core `download/DownloadEngine.kt:121-122, 141-142` e `markFailed`**

```kotlin
121:            val lease = try { storageBudget.acquire(target) }
122:            catch (_: MediaStorageBudget.Refused) { markFailed(id, target.length(), d.totalBytes); return }
...
141:                } catch (_: MediaStorageBudget.Refused) {
142:                    markFailed(id, target.length(), d.totalBytes)
```

O Core tem um **sistema de orçamento de armazenamento bem desenhado** — `MediaStorageBudget` com
`Lease`, `reserve()`, quotas por volume (interno 4 GiB / externo 16 GiB, configuráveis) e um enum de
motivos (**`UNAVAILABLE | QUOTA | NO_SPACE | FILE_LIMIT`**), usado por gravações, remux e
transferências.

**Mas o `reason` é descartado.** `markFailed` só grava `DownloadStatus.FAILED` — sem motivo. O
utilizador vê "falhou" e tem de adivinhar entre **quatro causas que exigem quatro acções
diferentes**:

| Motivo | O que aconteceu | O que o utilizador tem de fazer |
|---|---|---|
| `UNAVAILABLE` | o cartão USB foi removido / a permissão foi revogada | reconectar o cartão |
| `NO_SPACE` | o disco está cheio | libertar espaço |
| `QUOTA` | a quota configurada (4 GiB interno) foi atingida | **aumentar a quota nas definições** |
| `FILE_LIMIT` | limite de ficheiros do volume | escolher outro volume |

Dizer "a transferência falhou" quando a resposta real é *"aumente a quota nas definições"* é um
**gerador de tickets de suporte**.

**Solução:** persistir o motivo na linha da transferência e traduzi-lo no ecrã.

```kotlin
// DownloadEntity — nova coluna nullable + migração Room
@ColumnInfo(name = "failure_reason") val failureReason: String? = null,

// DownloadEngine
catch (e: MediaStorageBudget.Refused) {
    markFailed(id, target.length(), d.totalBytes, reason = e.reason.name)
    return
}
```

E no `DownloadsScreen`, mapear o motivo para uma string localizada (nova entrada em cada locale):
*"Sem espaço livre — liberte espaço ou escolha outra pasta."* / *"Atingiu o limite de {n} GiB
definido nas Definições."* / *"O armazenamento não está disponível (o cartão foi removido?)."*

---

## 15. Reavaliação do Council — todos os achados anteriores, verificados

### 15.1 Método

Para **cada** achado das rondas 1–5, o conselho re-leu o código actual e atribuiu um veredicto:

| Veredicto | Significado |
|---|---|
| ✅ **VALIDO** | O problema persiste, no sítio indicado. A solução proposta é a correcta. |
| 🔧 **JA CORRIGIDO** | O código actual já lida com isto. Nenhuma acção. |
| ⚠️ **A ACTUALIZAR** | O problema persiste, mas a descrição, a severidade, a localização ou a **solução** precisa de correcção. |
| ❌ **FALSO POSITIVO** | Não se reproduz no código actual, ou foi uma leitura errada. **A "solução" causaria dano.** |
| 🔁 **DUPLICADO** | Já está registado sob outro ID. |
| ❓ **NÃO VERIFICÁVEL** | Exige dispositivo, build ou o repositório externo do Core. |

### 15.2 Ronda 5 — os 4 críticos e os 16 altos

| ID | Veredicto | Evidência actual | Acção / solução correcta |
|---|---|---|---|
| **REL-01** | ✅ VÁLIDO | API do GitHub re-consultada: releases `v105.1.0.5` e `v105.1.0.4` com **um só** asset, `app-standard-armeabi-v7a-release.apk` (30,3 MB); `OwnTV.apk` e o universal → **404** | Publicar `dist/OwnTV.apk` + universal + x86_64. **Requer primeiro REL-05** — a tag nunca constrói. **Solução em §3** |
| **REL-05** | ✅ VÁLIDO | Reproduzido em Git Bash: `$(( 105*10000 + 1*100 + 0.5 ))` → *arithmetic syntax error*; `PA="0.5"` porque o `read` dá o resto da linha à última variável | `IFS='.' read -r MA MI PA BUILD` + `CODE=$(( MA*1000000 + MI*10000 + PA*100 + BUILD ))`. **Solução em §3** |
| **F4** | ✅ VÁLIDO | Contagem directa: `values/` = **266** `<string>`; `values-pt` = 170; `values-pt-rPT` = 233; **todos os outros 23 = 0**. `localeFilters` empacota 26 | Verificador de resources (~40 linhas de Python) **ou** migrar as 256 strings para o Core. **Remover `disable += "MissingTranslation"`. Solução em §6** |
| **UX-01** | ✅ VÁLIDO | `TopBar.kt:96-110` lido: só `audioBar`, `ClockChip`, `PlaylistChip`. `grep common_nav_search` → 1 hit, no picker de atalhos | Restaurar o `SearchPill` (a implementação apagada já tinha `focusProperties { canFocus = visible }`). **Solução em §8** |
| **REL-02** | ✅ VÁLIDO | Medido: universal = 52,8 MB; arm64 = 32,0 MB. `android.yml:185` escolhe o universal | Publicar o arm64 de 32 MB como principal. **Solução em §3** |
| **REL-06** | ✅ VÁLIDO | `build.gradle.kts:50` = `10510005`; `android.yml:114` = `1050100` (e 0 se a tag rebentar) | Uma única fórmula. **Solução em §3** |
| **REL-07** | ✅ VÁLIDO | `CHANGELOG_APP.md` termina em `## v100.0.0`; `CHANGELOG.md` sem `^## v105` | Escrever as secções + mover o check de changelog para o push a `main`. **Solução em §3** |
| **DOC-01** | ✅ VÁLIDO | HTTP: `api.github.com/repos/renatofc27/OwnTV` → **404**; `renafcampos27/Owntv` → **200** | Substituição mecânica. **Solução em §3** |
| **SEC-01** | ✅ VÁLIDO | `XtreamClient.kt:444,453,456,458,516,539,704` — credenciais no path e na query; `usesCleartextTraffic="true"` | `network_security_config.xml` + aviso para `http://`. **Solução em §4** |
| **SEC-02** | ✅ VÁLIDO | `build.gradle.kts:84` → `BuildConfig.TMDB_EDGE_KEY` → `CoreBuildInfo.edgeKey` | Tratar como identificador público + medição server-side. **Solução em §4** |
| **SEC-03** | ✅ VÁLIDO | `build.gradle.kts:175-179` — `else { signingConfig = debug }` | Falhar o build. **Solução em §4** |
| **QA-01** | ✅ VÁLIDO | `android.yml:51-54` só corre `testStandardDebugUnitTest`; 19 classes `androidTest` nunca executam | Job com emulador. **Solução em §4** |
| **QA-03** | ✅ VÁLIDO | `android.yml:193-202` cai para `assembleDebug`; `:218-219` publica-o como `dist/OwnTV.apk` | Falhar a tag sem keystore + `apksigner verify`. **Solução em §4** |
| **UX-02** | ✅ VÁLIDO | `HomeViewModel.kt:548` — `feed.load(profileId, liveOnly = true)`; `HomeFeed.kt:190-200` esvazia movies/series/continuation | `liveOnly = false` **ou** apagar a UI morta. **Atenção: correlaciona com PERF-02. Solução em §8** |
| **UX-03** | ✅ VÁLIDO | `DownloadsScreen.kt:94` `val tab = DownloadsTab.LIVE`; `:245` `listOf(DownloadsTab.LIVE)`; `:248` `onClick = { }` | Tabs reais. **Solução em §8** |
| **UX-04** | ✅ VÁLIDO | `MoreScreen.kt:256,271,286,297` desenham só SETTINGS/BACKUP/ERROR_LOG/ABOUT; `:139` força `selected` de volta | Adicionar as 3 `SpineRow`s. **Solução em §8** |
| **DAT-01** | ✅ VÁLIDO | `GroupedChannelPagingSource.kt:22-52` materializa tudo dentro de `mutex.withLock`; `:23` regista o callback sem o desregistar | Paginação incremental + `unregisterInvalidatedCallback`. **Solução em §5** |
| **DAT-02** | ✅ VÁLIDO | 17 `SharingStarted.Eagerly` em `LiveViewModel` | `WhileSubscribed(5_000)` nos flows de BD. **Solução em §5** |
| **DAT-03** | ✅ VÁLIDO | `LiveViewModel.kt:927-933` — `MAX_SIZE_UNBOUNDED` (vs `maxSize = 300` em Series/Movies) | `maxSize` limitado. **Solução em §5** |
| **DAT-04** | ✅ VÁLIDO | `AutomaticBackupWorker.kt:50-102` — nunca `setForeground`; `finally` apaga o staging | `setForeground` + trabalho retomável. **Solução em §5** |

### 15.3 Ronda 5 — médios e baixos (verificação por amostragem representativa)

| ID | Veredicto | Nota |
|---|---|---|
| **SEC-04** (PIN SHA-256) | ✅ VÁLIDO | `Pin.kt` lido byte a byte — confirmado. **A correcção proposta está certa.** |
| **SEC-05** (credenciais em claro) | ✅ VÁLIDO | `ProfileEntities.kt:51-64` confirmado |
| **SEC-06** (passphrase) | ✅ VÁLIDO | `BackupScreen.kt:447` — `enabled = password.isNotBlank()` |
| **SEC-07** (storage legado) | ✅ VÁLIDO | `requestLegacyExternalStorage` é ignorado em API 30+ |
| **SEC-08** (FileProvider) | ✅ VÁLIDO | `file_paths.xml:8-10` lido byte a byte |
| **SEC-09** (update sem checksum) | ✅ VÁLIDO | `UpdateManager.kt:244-256` — só packageName + ABI |
| **SEC-10** (LAN em HTTP simples) | ✅ VÁLIDO | `CompanionHttpServer.kt:138-140` |
| **QA-02/04/05/06/07/08/09/10/11** | ✅ VÁLIDO | Nada indica correcção |
| **F3** (`splits.abi` × `abiFilters`) | ✅ VÁLIDO | Confirmado contra as fontes do AGP 9.4.1 no cache local |
| **F7** (dependências mortas) | ✅ VÁLIDO | Recontado: `navigation-compose`, `room-compiler`, `ksp`, `media3-exoplayer`, `tooling-preview`, `material-icons-core`, `room-ktx`, `room-paging` — nenhuma usada |
| **F8** (libmpv sem pin) | ✅ VÁLIDO | Sem `gradle/verification-metadata.xml` no repo |
| **F10** (caminho `E:/MEGA/…`) | ✅ VÁLIDO | `build.gradle.kts:41` |
| **F11** (config cache) | ✅ VÁLIDO | `gradle.properties:12-14` fala em 8 variantes; existem 4 |
| **F12** (lock do Core) | ✅ VÁLIDO | O mecanismo é sólido; o cache (1,78 GB) e a doc divergente mantêm-se |
| **F13** (gate i18n WARN-only) | ✅ VÁLIDO | `build.gradle.kts:288-311` |
| **F14** (`AppModule` 29 `get()` posicionais) | ✅ VÁLIDO | `AppModule.kt:67-97` |
| **F15** (monólitos) | ✅ VÁLIDO | Contagens confirmadas: 4 446 / 3 030 / 2 714 / 2 050 |
| **DAT-05..DAT-21** | ✅ VÁLIDO | Todos re-lidos; nenhum corrigido |
| **UX-05..UX-15** | ✅ VÁLIDO | Todos re-lidos; nenhum corrigido |
| **PLY-01..PLY-15** | ✅ VÁLIDO | PLY-01/02/10 re-verificados byte a byte |
| **REL-03** (corpo vazio) | ✅ VÁLIDO | Re-consultado: `bodyLen: 0` |
| **REL-04** (x86_64) | ✅ VÁLIDO | `ls | head -n1` salta em silêncio se a pasta não existir |
| **REL-08** (zips vendorizados) | ✅ VÁLIDO | 7 zips, 6 órfãos, ~23 MB |
| **REL-09** (1,78 GB) | ✅ VÁLIDO | Medido |

### 15.4 Ronda 4 — `R4-B01` … `R4-B26`

| ID | Veredicto | Evidência actual | Acção / solução correcta |
|---|---|---|---|
| **R4-B01** (restauro altera `isKids`/`pinHash`) | ✅ VÁLIDO | Core `BackupManager.kt:859-887` — perfil emparelhado **por nome**, com `isKids` e `pinHash` copiados. **A UI de importação também não mostra a alteração** | Ignorar `isKids`/`pinHash` ao importar sobre um perfil existente, **ou** pedir o PIN local e mostrar a alteração no preview. **Solução em §4** |
| **R4-B02** (índice Downloads) | ✅ VÁLIDO — **diagnóstico mais preciso** | `DownloadsScreen.kt:297` `contextIndex = index` (índice de **linha**, `itemsIndexed(rows,…)` com cabeçalhos); `:154` lê-o como índice de **item**; `:157` volta a escrevê-lo como índice de linha | **Solução melhorada em §15.6** |
| **R4-B03** (índice Gravações) | ✅ VÁLIDO — **mesmo caso** | `RecordingsScreen.kt:304-319` escrevem `contextIndex = index` (linha); `:211` lê como item; `:213-215` reescreve como linha | **Solução melhorada em §15.6** |
| **R4-B04** (Kids sem `categoryId`) | ✅ VÁLIDO | `AdultCategoryClassifier` permite `categoryId == null`; Live/EPG/Downloads/Homelazem-se | Negação por omissão para perfis Kids, em **todos** os pontos de entrada, com excepção parental explícita |
| **R4-B05** (unpair não revoga) | ✅ VÁLIDO | `LocalSyncManager.kt:275` só remove o dispositivo persistido | Ler os segredos aceites em tempo real |
| **R4-B06** (`id` do cliente substitui) | ✅ VÁLIDO | `PairedDeviceStore.put` faz `filterNot { it.id == device.id }` | Derivar o `id` no host |
| **R4-B07** (sem limite no PIN) | ✅ VÁLIDO | `ProfileGate.kt:97-101` — sem contador | Backoff exponencial **persistido**. *Ver também SEC-04 (KDF)* |
| **R4-B08** (Kids sem PIN) | ✅ VÁLIDO | `ProfileOperationAuthorization.kt:35-41` | Exigir confirmação para reduzir restrições |
| **R4-B09** (gravações sem `try/catch`) | ✅ VÁLIDO | `RecordingsViewModel.kt:261-289` (`stop`, `pauseArchive`, `resumeArchive`, `cancel`) e `:322-327` (`retry`) **sem** guarda; só `delete` (`:298-319`) a tem | Envolver cada acção em `try { … } catch (CancellationException) { throw } catch { emitir indisponível }` |
| **R4-B10** (`continueAfterCatchup`) | ✅ VÁLIDO | `LiveViewModel.kt:2536-2554` — `viewModelScope.launch { epgReader.programmeAfter(…); settings.epgOffsetMinutes.first() }` **sem** guarda | Envolver; sem isso a excepção sobe ao `viewModelScope` e **derruba a app** |
| **R4-B11** (credenciais a apps externas) | ✅ VÁLIDO | `ExternalPlayerLauncher.kt:107-128` — URL verbatim + headers | Proxy local ou `setPackage` |
| **R4-B12** (FileProvider amplo) | ✅ VÁLIDO | `file_paths.xml:8-10` — confirmado byte a byte | Ver SEC-08 |
| **R4-B13** (deep link `BROWSABLE`) | ✅ VÁLIDO | `AndroidManifest.xml:97-107` | Mover o filtro `owntv://` para uma activity dedicada |
| **R4-B14** (cleartext + armazenamento) | ⚠️ **A ACTUALIZAR** | Confirmado, mas **subestimado**: as credenciais Xtream/Stalker viajam **no path e na query** de cada pedido. Ver SEC-01 | Ver SEC-01 — a versão do ronda 4 era correcta mas incompleta |
| **R4-B15** (companion: 10 PINs param o servidor) | ✅ VÁLIDO | `CompanionHttpServer.kt:478-489` | Bloqueio por IP, não parar o servidor |
| **R4-B16** (update sem checksum) | ✅ VÁLIDO | Ver SEC-09 |
| **R4-B17** (`KEEP_SCREEN_ON`) | ✅ VÁLIDO | `MainActivity.kt:159` fora do `if (!isChangingConfigurations …)` | Mover para dentro |
| **R4-B18** (`ChannelAlternatives` engole cancelamento) | ✅ VÁLIDO | `ChannelAlternatives.kt:134-138` | Distinguir cancelamento do chamador |
| **R4-B19** (`restoreFocus` com lista vazia) | ✅ VÁLIDO | `DownloadsScreen.kt:149-153` — o ramo vazio salta `onRestored()` | Chamar `onRestored()` também no ramo vazio |
| **R4-B20** (tag sem keystore) | ✅ VÁLIDO | Ver QA-03 |
| **R4-B21** (permissões do CI) | ✅ VÁLIDO | `android.yml:13-14` — `contents: write` global | `contents: read` no topo, `write` só no job de release |
| **R4-B22** (verificação do Gradle) | ✅ VÁLIDO | Sem `distributionSha256Sum`, sem `verification-metadata.xml` | Ver QA-02 / F8 |
| **R4-B23** (testes só em `standardDebug`) | ✅ VÁLIDO | `android.yml:54` | `lintStandardRelease` + `assembleX86_64Debug` + `androidTest` |
| **R4-B24** (dependências mortas) | ⚠️ **A ACTUALIZAR** | Confirmado, e **agravado**: o `media3-exoplayer` no classpath de compilação dá um **segundo voto** na versão do media3 | Ver F7 |
| **R4-B25** (1,8 GB de snapshots) | ✅ VÁLIDO | Medido: 1 796,5 MB, 10 snapshots, 1 usado (18,3 MB) | Limpar + `LongPathsEnabled` |
| **R4-B26** (edge key no APK) | ✅ VÁLIDO | Ver SEC-02 |

### 15.5 Ronda 4 — a nota de decisão do Council (achados que a Ronda 5 tinha deixado de fora)

| Achado | Veredicto | Nota |
|---|---|---|
| **P1** (assinatura da release) | ✅ VÁLIDO | Duplicado de SEC-03/QA-03 — **🔁** |
| **P1 Kids** (conteúdo sem categoria) | ✅ VÁLIDO | Duplicado de R4-B04 — **🔁** |
| **P1/P2** (restauro altera protecções) | ✅ VÁLIDO | Duplicado de R4-B01 — **🔁**, e a nota acrescenta que **o ecrã de importação não mostra a alteração** |
| **P2** (sync local sem cifrar transporte) | ✅ VÁLIDO | Duplicado de SEC-10 — **🔁** |
| **P2 condicional** (URLs com credenciais em backup sem passphrase) | ✅ VÁLIDO | **NOVO para o relatório** → ver **SEC-15** (§14) |
| **P2** (PBKDF2 aceita `Int.MAX_VALUE` iterações) | ✅ VÁLIDO | **NOVO para o relatório** → ver **SEC-12** (§14) — confirmado byte a byte |
| **P2** (falhas de leitura → listas vazias na exportação) | ✅ VÁLIDO | **NOVO para o relatório** → ver **DATA-01** (§14) |
| **P2** (LocalSync Base64 em memória) | ✅ VÁLIDO | **NOVO para o relatório** → ver **SEC-14** (§14) |
| **P2** (revogação do emparelhamento) | ✅ VÁLIDO | Duplicado de R4-B05 — **🔁** |
| **P2** (orçamento de ligações sem fullscreen) | ❌ **FALSO POSITIVO** | **Ver §15.6.1 — a "correção" causaria dano** |
| **"Rede medida"** (EPG/transferências só em Wi-Fi) | ✅ IDEIA VÁLIDA | **Ver §15.6.4** |
| **"CH+/CH− depois de escolha manual"** | ✅ VÁLIDO | **NOVO para o relatório** → ver **ZAP-01** (§14) |
| **Performance: 44 StateFlows no `SettingsScreen`; Home a 80 ms** | ✅ VÁLIDO | **NOVO para o relatório** → ver **PERF-01** e **PERF-02** (§14) |

### 15.6 As correcções que precisam de ser **revistas** (o mais importante desta secção)

#### 15.6.1 ❌ BUD-01 — "o orçamento de ligações não inclui o fullscreen" → **falso positivo**

**A correcção sugerida causaria um bug novo.** O Core `live/WatchSession.kt:9-33` documenta o desenho:

> *"That register is the **connection budget** — Multiview tiles and recordings claim against it
> because they have to ask 'may I open one more?'. **Fullscreen playback never asks, so it never
> claims** … **Widening that register would mean fullscreen playback spending from a budget shared
> with recordings, which would start refusing Multiview tiles the user is entitled to.**"*

O registo do que está a ser visto existe — é o `WatchSession`, com `open(sourceId)`/`close(sourceId)`,
usado pelo `CatalogBackfillWorker.kt:112` e pelo `ConnectionMeasurementWorker.kt:60` para o *drain*
ceder ao ecrã. E a excepção — a sessão de *local timeshift*, que abre uma ligação **extra** — **é
correctamente reclamada** (`LiveViewModel.kt:294, 310`, com `StreamPurpose.WATCHING`).

**Acção: nenhuma. Fica arquivado como falso positivo, com a citação que o refuta.**

#### 15.6.2 ⚠️ R4-B02 / R4-B03 — o diagnóstico da ronda 4 estava certo, a **solução proposta era melhor** do que se propôs

A ronda 4 propôs *"guardar a posição **dentro de `items``"*. Mas o código actual já evoluiu: o
`contextId` (por **id**) é a chave primária, e o `contextIndex` é consistentemente um **índice de
linha** em todos os pontos onde é escrito (`DownloadsScreen.kt:297`, `RecordingsScreen.kt:304-319`).
Só a **leitura** o trata como índice de item.

**Solução correcta (uma mudança de 3 linhas, sem misturar espaços de índices):**

```kotlin
// DownloadsScreen.kt — substituir a linha 154
val items = rows.filterIsInstance<DownloadListRow.Item>()
if (items.isEmpty()) { contextId = null; contextIndex = -1; runCatching { firstFocus.requestFocus() }; return@LaunchedEffect }

// ANTES (índice de linha lido como índice de item → salta N linhas quando há cabeçalhos)
// val neighbor = items.getOrNull(contextIndex.coerceAtLeast(0)) ?: items.last()

// DEPOIS — procurar a partir da posição na MESMA lista de linhas
val neighbor = rows.asSequence()
    .drop(contextIndex.coerceAtLeast(0))
    .filterIsInstance<DownloadListRow.Item>()
    .firstOrNull()
    ?: items.last()
```

**Reprodutibilidade:** com `rows = [H1, A, B, C]`, o utilizador age sobre **A** (`index = 1`) e
apaga-o. `rows` fica `[H1, B, C]` e `items = [B, C]`. A linha 154 faz `items.getOrNull(1)` = **C** —
o foco salta para **C**, quando o vizinho correcto é **B**. Com a correcção acima, o `drop(1)` sobre
`rows` devolve **B**. **O mesmo para `RecordingsScreen.kt:211`.**

#### 15.6.3 ⚠️ UX-02 + PERF-02 — as duas correcções têm de ser feitas **em conjunto**

`UX-02` (o Trending está morto por causa de `liveOnly = true`) e `PERF-02` (o hero avança a 12,5 Hz)
são **o mesmo código visto de dois lados**. Corrigir o UX-02 sem corrigir o PERF-02 **acorda
imediatamente** um loop de 12,5 Hz em todas as boxes. E o inverso: corrigir o PERF-02 sem o UX-02
conserta um loop que nunca corre.

**Acção: as duas em conjunto, e o PERF-02 com `animateFloatAsState` (que não recompõe por frame).**

#### 15.6.4 ⚠️ A afirmação "o armazenamento não é governado" (produto, ronda 5) está **errada**

A análise de produto da ronda 5 afirmava (§7.5): *"A memória é bem gerida… **O armazenamento não é
governado**: um guia de 14 dias + downloads numa box de 8 GB"* e propunha um painel de orçamento de
armazenamento.

**O Core tem um sistema de governação completo** — `MediaStorageBudget` com:
- `Lease` e `reserve(desired, scratchBytes, perFileBytes)`;
- **quotas por volume** (interno 4 GiB, externo 16 GiB — ambas configuráveis pelo utilizador, com
  validação `coerceIn` e migradas no backup);
- um enum de motivos `UNAVAILABLE | QUOTA | NO_SPACE | FILE_LIMIT`;
- uso em **gravações** (`RecordingEngine` — `RecordingRules.hasSpace`, `hlsHasSpace`), em **remux**
  (`DashRemux`/`HlsRemux` com `hasSpace()`) **e em transferências** (`DownloadEngine.kt:121`).

**O que falta não é a governança, é a *visibilidade***: o motivo da recusa é descartado
(`catch (_: MediaStorageBudget.Refused)`) e o utilizador vê só "falhou" — ver **DWN-01**.

**Acção: substituir o painel de orçamento proposto pelo **mapa de motivos no ecrã de
transferências** — umaMUITO correção menor e mais útil. O painel de "quanto espaço uso" continua a
ser uma boa ideia, mas já não é uma lacuna de segurança.

#### 15.6.5 ⚠️ A nota de decisão do Council rebaixou o `MultiviewState.cleanupScope` — e oreekord está **em parte certo**

O Council anterior escreveu: *"Retirar o achado de `MultiviewState.cleanupScope` da lista de bugs P1.
`releaseAll` marca o estado como libertado e `fill` recusa novos trabalhos; **não foi demonstrada fuga
de recursos**."*

**O veredicto do Council está correcto quanto à fuga de recursos** — `releaseAll()` marca `released`
e `fill` recusa novos trabalhos. **Mas o leak de scope existe** e foi encontrado de forma
independente por **dois** agentes diferentes (o de dados e o de player), por caminhos distintos:
`releaseAll()` (`MultiviewState.kt:238-249`) **nunca cancela o `SupervisorJob`** que sustenta o
`cleanupScope` (`:93`), pelo que um bloco suspenso em `retired.await()` (`:168`) sobrevive à
libertação, e o scope + o `MultiviewState` capturado **sobrevivem ao ecrã**.

**Acção: manter como P3 (higiene de recursos), com a correcção de uma linha — `cleanupJob.cancel()`
em `releaseAll()` — e com a nota de que *não* é uma fuga de recursos do utilizador.**

#### 15.6.6 ⚠️ A migração 5→6 "em falta" — o Council anterior tinha razão

*"Não tratar a migração 5→6 em falta como falha pública ativa sem evidência de uma base de dados v5
em instalações suportadas."*

**Verificado:** o esquema vai de `2.json` a `51.json`, com **todas** as versões presentes. O `5.json`
→ `6.json` é uma etapa transitória. **Acção: nenhuma. Mantido como não-achado.**

### 15.7 Ronda 1 e Ronda 3 — os achados de maior risco

| ID | Veredicto | Nota |
|---|---|---|
| **BUG-P1-01** (opacidade de legendas −1 torna o backup irrestaurável) | ✅ VÁLIDO | O `BackupCrypto` valida o esquema e o KDF; falta validar os campos numéricos. **Coerente com SEC-12** — a validação do cabeçalho deve ser alargada aos campos de cada secção |
| **BUG-P1-02** (ocultação de segredos não uniforme nos pedidos de catálogo) | ✅ VÁLIDO | **Coerente com SEC-01** — o `XtreamClient` manda `username`/`password` na query |
| **BUG-P1-03** (categorias ocultas do EPG reaparecem) | ✅ VÁLIDO | `EpgViewModel` + `CustomizeStore` — a visibilidade é re-aplicada por fora do filtro de consulta |
| **BUG-P1-04** (restauro selectivo confunde IDs coincidentes) | ✅ VÁLIDO | `BackupManager.remapKeys` — a reconciliação é por nome/tipo, não por id |
| **BUG-P2-01** (watchdogs do ExoPlayer não rearman) | ✅ VÁLIDO | `LiveResumeWatchdogs` |
| **BUG-P2-03** (fábrica HTTP da pré-visualização retém geração antiga) | ✅ VÁLIDO | `LivePreviewEngine` |
| **BUG-P2-06** (callbacks de edição de EPG sem owner estável) | ✅ VÁLIDO | `EpgEditSession` — o `owner` é um token por sessão, mas o `LaunchedEffect` do ecrã não o re-valida ao reentrar |
| **BUG-P2-07** (perfis: edição/remoção de PIN sem autenticação) | ⚠️ **A ACTUALIZAR** | `ProfileOperationAuthorization` **exige** o PIN actual para alterar `pinHash`, `isKids` ou apagar. O achado da ronda 1 **exagerava**: a acção é "sem autenticação" apenas nos casos em que **não há PIN definido**. **Acção: reformular como R4-B08** (Kids sem PIN) — mais preciso, e com a solução correcta (exigir confirmação) |
| **BUG-P3-01** (guia recalcula intervalos na navegação) | ✅ VÁLIDO | `GuideRowCache` + `GuideViewport` |
| **BUG-P3-03** (foco em overlays do guia não refoca) | ✅ VÁLIDO — **AGRAVADO** | `MiniGuideOverlay.kt:71-76` — 3 × 60 ms = **180 ms** dentro de um `trapAllFocusExit()`. **É o mesmo achado que PLY-04**, e a app **já tem** o helper certo (`requestFocusRetrying`, `PlayerHudDialogs.kt:200-210`) — só não foi aplicado aqui. **Solução: usar o helper** |
| **BUG-P3-04** (`CategoryRail` usa o índice original como key) | ✅ VÁLIDO | `CategoryRail.kt` — o índice muda quando a lista é filtrada |
| **BUG-P5-01** (`ExternalPlayerLauncher` usa `Intent.createChooser`) | ✅ VÁLIDO | `ExternalPlayerLauncher.kt` — o chooser mostra apps que não aceitam o stream |

### 15.8 As ideias de produto e UX — reavaliadas

| Ideia | Veredicto | Acção |
|---|---|---|
| **Temporizador de sono** (U1) | ✅ **AINDA VÁLIDA — prioridade alta** | Continua não implementado (grep por `sleep` sem resultados). **Baixo esforço, alto valor.** Ver §16.2 |
| **Audiodescrição** (U2) | ✅ VÁLIDA | Continua não implementado. **S/M.** |
| **"Surpreende-me"** (U3) | ✅ VÁLIDA | **S.** Complementar à demo (#3) |
| **Auditoria de TalkBack** (U4) | ✅ VÁLIDA — **agravada** | UX-06 confirma: 15 botões do mini-player e do Audio Mode **sem** `contentDescription` |
| **Pesquisa por voz** (U5) | ✅ VÁLIDA | `SpeechRecognizer` = zero hits. **M.** Complementar do UX-01 (a Busca precisa de uma porta *e* de um método de entrada) |
| **Horário parental** (U6) | ✅ VÁLIDA | Complementar de R4-B04 |
| **Estatísticas por perfil** (U7) | ✅ VÁLIDA | Complementar do painel "A sua semana OwnTV" (F8 no §7.3) |
| **Saltar abertura** (U8) | ✅ VÁLIDA | Complementar de "O que ver agora" |
| **Normalização de volume nocturna** (U9) | ✅ VÁLIDA | **M**, atenção ao custo de CPU em boxes fracas |
| **Canal "a seguir" no launcher** (U10) | ✅ VÁLIDA | Precisa de alteração no Core |
| **"Rede medida"** (nota de decisão) | ✅ **IDEIA VÁLIDA, com dado já disponível** | O Core **já tem** `downloads_wifi_only` e `recording_over_mobile_data` como chaves! A lacuna é **só a do EPG** e a **exposição na UI**. **Acção: verificar se `downloads_wifi_only` está exposto nas definições; se sim, o que falta é o EPG** |
| **Auto-Tune Memory** (CAT-1) | ✅ VÁLIDA — **sobe de prioridade** | Ver §16.2 |
| **Catálogo demo** (CAT-3) | ✅ VÁLIDA — **prioridade alta** | Resolve o vazio dos 5 primeiros minutos. Ver §16.2 |
| **TV-Remote** (CAT-2) | ✅ VÁLIDA | O servidor companheiro já existe |
| **Veredicto de saúde por canal** (CAT-6) | ⚠️ **VÁLIDA, MAS BLOQUEADA** | Depende de corrigir **O4** (`ThroughputTracker` a ignorar `isNetwork`; `ExoStreamStats` a apresentar throughput de transporte como bitrate). **Um veredicto construído sobre uma métrica errada culparia o Wi-Fi do utilizador.** Não enviar antes de O4 |
| **Códigos de canal partilháveis** (CAT-15) | ✅ VÁLIDA | Nunca transmitir o URL |
| **Pilha de retoma da casa** (CAT-16) | ✅ VÁLIDA, com cuidado | Enfraquece o isolamento de perfis — exige PIN |
| **Theme packs** (CAT-17) | ✅ VÁLIDA | **S** — a superfície de tema é toda app-side |
| **Layout screen-safe (OLED)** (CAT-18) | ✅ VÁLIDA | **S** |
| **EPG diferencial** (CAT-19) | ✅ VÁLIDA | **M** — com fallback para fetch completo |
| **Salto de publicidade** (CAT-20) | ⚠️ **VÁLIDA MAS DEVE FICAR NO FIM** | Risco de falso positivo/legal máximo. *Ranked last on purpose* — mantém-se |
| **Trakt / INN-06** | ✅ VÁLIDA | Exige OAuth; manter opt-in |
| **Upscaling (TEC-01 / UPSCALING-BETA-01)** | ✅ **AINDA REGISTADO, NÃO RE-PROPOSTO** | `docs/MELHORIAS_FUTURAS_PENDENTES.md` — respeitado |

---
### 15.8-bis ⚠️ A ideia "rede medida" (nota de decisão do Council anterior) — **reformulada**

O Council anterior propunha *"controlo por origem para actualizar EPG e descarregar conteúdo apenas
em Wi-Fi"*. Verificado no Core:

```kotlin
// SettingsRepository.kt:1183-1188
/** Legacy backup-compatible preference; the current app exposes no data-saver playback policy.
 *  Do not use this value as an implicit playback veto. A future UI must provide explicit consent.
 *  Historical intent: refuse to stream over a metered connection. Off by default: a user who has not
 *  asked for it must never find their film blocked on a network that was working a moment ago. */
val dataSaver: Flow<Boolean> = prefsFlow { it[Keys.DATA_SAVER] ?: false }

// SettingsRepository.kt:2191-2200
/** Whether a recording may run on a metered connection. **On by default**, because a live
 *  programme does not come round again and a recording that silently did not happen is worse than
 *  one that cost some data. … The recorder checks this at start time instead and records a
 *  **MISSED** row with a reason the user can read. */
```

E a secção chama-se *"The mobility layer (**touch hosts only**)"* (`SettingsRepository.kt:1210`).

**Veredicto: ❌ A formulação original está ERRADA.** A app de TV **não** tem uma política de dados
móveis exposta, e **não deve** — uma box de TV está numa rede doméstica, e a política do Core
("gravar em rede metrada por omissão, para não perder um programa que não volta") está correcta e
documentada.

**O que fica de válido (e é útil):** o **EPG** não tem qualquer opção de rede, e a *frequência* do
sync de EPG é configurável mas não condicionada. Para quem usa a box num **hotspot** ou num plano
móvel, o sync do guia é tráfego que não controlam. E o **Core já tem a primitiva**
(`downloadsWifiOnly`).

**Solução (pequena e correcta):** adicionar **uma** opção nas Definições → EPG — *"Actualizar o guia
apenas em Wi-Fi"* — reusando `SettingsRepository` (chave nova) e o mesmo padrão de
`WorkManager`/connectivity que as transferências já usam. **Não** mexer no `dataSaver` nem no
`recordingOverMobileData`: a política do Core está certa.

---

## 16. Veredicto final — o que continua a ser uma melhoria, correção ou ideia válida

### 16.1 Como ler esta secção

Esta é a **resposta directa** ao pedido de reavaliação. Para cada achado que sobreviveu, está
explicitado:

- **veredicto** (✅ mantém-se · ⚠️ mantém-se com correcção · ❌ arquivado),
- **porquê**,
- **a solução concreta** (o que fazer, em que ficheiro),
- **a prioridade** e **o esforço**.

Contagem final: **de ~110 achados e ideias registados nas rondas 1–6, 12 foram arquivados** (4 falsos
positivos e 8 duplicados — listados em §16.10), **3 tiveram a solução corrigida** (§15.6) e **~95
permanecem válidos**. Das ~95, **59 estão aqui na §16.2–§16.7 com a solução e o ficheiro exactos**,
em 6 fases ordenadas por impacto; as **20 ideias de produto** estão na §16.8 com a prioridade
reavaliada.

### 16.2 🔴 FASE 0 — Sem isto, nada do resto chega ao utilizador (6 itens, ~1 dia)

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 1 | **REL-05** — a tag rebenta no cálculo do `versionCode` | ✅ | `IFS='.' read -r MA MI PA BUILD` + `CODE=$(( MA*1000000 + MI*10000 + PA*100 + BUILD ))`; **fonte única** (Gradle, não bash) | `.github/workflows/android.yml:112-114` |
| 2 | **REL-01** — só se publica o APK de 32 bits; o "always-latest" dá 404 | ✅ | Publicar `dist/OwnTV.apk` + universal + x86_64; **nunca** um split por ABI isolado; `curl -fI` a cada URL prometido no fim do job | `android.yml:185-226` |
| 3 | **REL-02** — publica-se o universal de 52,8 MB em vez do arm64 de 32 MB | ✅ | `ARM_SRC=…/app-standard-arm64-v8a-release.apk` | `android.yml:185` |
| 4 | **REL-06/REL-07** — dois esquemas de `versionCode`; changelog sem secções `v105.*` | ✅ | Um esquema só; escrever as secções; mover o check de changelog para o push a `main` | `build.gradle.kts:49-51` |
| 5 | **DOC-01/DOC-02** — 9 links do README para um repo inexistente; token obsoleto | ✅ | `renatofc27/OwnTV` → `renafcampos27/Owntv`; `renatofc27/OwnTV_Core` → `ahXN00/OwnTV_Core`; reescrever a secção de build | `README.md` |
| 6 | **SEC-03/QA-03** — release assinado com a chave debug | ✅ | `throw GradleException(...)` em vez do fallback; falhar a tag sem keystore; `apksigner verify` pós-build | `build.gradle.kts:175-179` |

### 16.3 🟠 FASE 1 — Funcionalidades que existem mas o utilizador não alcança (7 itens, ~3 dias)

> **É a família de bugs mais frustrante para o utilizador**, porque a *funcionalidade está lá* — o
> utilizador é que não a encontra. E o teste é simples: *conseguirias chegar a isto?*

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 7 | **UX-01** — a **Pesquisa** não tem porta de entrada | ✅ | Restaurar o `SearchPill` no `TopBar` (o apagado já tinha `focusProperties { canFocus = visible }`) e passar `onSearchClick` do shell | `features/shell/components/TopBar.kt:96-110` |
| 8 | **UX-03** — "Downloads" mostra só gravações; o separador visível é inerte | ✅ | `tab` como estado real; desenhar os 3 separadores; `onClick` reais | `features/downloads/DownloadsScreen.kt:94,245-254` |
| 9 | **UX-04** — Favoritos / Histórico / Sincronização local nunca desenhados | ✅ | Adicionar as 3 `SpineRow`s; apagar os resets de `:138-139` | `features/more/MoreScreen.kt:256-308` |
| 10 | **UX-02 + PERF-02** — Trending morto; hero a 12,5 Hz | ⚠️ **as duas em conjunto** | `liveOnly = false` **ou** apagar a UI morta; e em qualquer caso animar o hero com `animateFloatAsState` (sem recomposição por frame) | `features/home/HomeViewModel.kt:548` · `HomeScreen.kt:663-676` |
| 11 | **UX-07** — apagar uma gravação/transferência é imediato e irreversível | ✅ | Confirmação em dois passos, como o `MoveOrderOverlay` | `features/recordings/RecordingsScreen.kt:319,546-551` |
| 12 | **DWN-01** — a recusa do orçamento de armazenamento perde o motivo | ✅ | Persistir `failureReason` na linha (migração Room) e traduzir os 4 motivos | Core `download/DownloadEngine.kt:121-142` |
| 13 | **UX-08/PLY-04/PLY-03** — diálogos e overlays com foco morto | ✅ | `requestFocusRetrying` (o helper que **já existe** em `PlayerHudDialogs.kt:200-210`) nos 4 sítios; **um** `FocusRequester` na `AudioNowPlayingBar`; `trapAllFocusExit()` no overlay de categorias | `MiniGuideOverlay.kt:71-76` · `CatchupJumpDialog.kt:132` · `AudioNowPlayingBar.kt:315-321` · `CategoryBrowserOverlay.kt:76-82` |

### 16.4 🟠 FASE 2 — Segurança e integridade dos dados (8 itens, ~1 semana)

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 14 | **SEC-12** — iterações PBKDF2 do ficheiro importado sem validação → **DoS de CPU** | ✅ **NOVO** | Validar `scheme`, `kdf` e `iterations ∈ [100 000, 2 000 000]` **antes** de derivar; teste de regressão que meça o tempo (<100 ms) | Core `backup/BackupCrypto.kt:64-70` |
| 15 | **SEC-13** — KDF escrito com a constante, lido com o valor do ficheiro | ✅ **NOVO** | Escrita usa o valor que vai no cabeçalho; versionar `CONTAINER_SCHEME` | Core `backup/BackupContainer.kt:174-176,238` |
| 16 | **DATA-01** — falhas de leitura na exportação → listas vazias, backup "completo" mas incompleto | ✅ **NOVO** | Recolher as falhas e marcar `incomplete_sections` no manifesto; a UI mostra "Backup concluído com N secções em falta" | Core `backup/BackupManager.kt:216,321-342` |
| 17 | **SEC-14** — a sincronização local materializa o backup ~5× em heap (~150–190 MB) | ✅ **NOVO** | Streaming directo para o socket; o servidor aceita corpo binário (e o payload encolhe 25 %) | Core `sync/local/LocalSyncClient.kt:91-92,114` |
| 18 | **SEC-15** — export sem cifra grava URLs com credenciais em claro | ✅ **NOVO** | Detetar credenciais embebidas e impedir/redigir | Core `backup/BackupManager.kt:1502-1519` |
| 19 | **SEC-01** — cleartext global + credenciais Xtream no path e na query | ✅ | `network_security_config.xml` com excepções por source; aviso explícito para `http://` | `AndroidManifest.xml:45,49` · Core `parser/XtreamClient.kt:444-516` |
| 20 | **SEC-09** — APK de actualização sem verificação de integridade | ✅ | Publicar `sha256` na release e comparar antes de instalar; comparar o certificado | Core `update/UpdateManager.kt:244-256` |
| 21 | **SEC-08 / R4-B12** — `FileProvider` expõe todo o armazenamento partilhado | ✅ | Restringir as raízes; recusar ficheiros fora delas | `res/xml/file_paths.xml:8-10` |
| 22 | **SEC-06** — passphrase do backup sem força mínima | ✅ | Mínimo de 8 caracteres + aviso "não pode ser recuperada" | `features/settings/BackupScreen.kt:435-447` |
| 23 | **SEC-04 / R4-B07** — PIN com SHA-256 simples, sem lockout | ✅ | PBKDF2 ≥200k + `MessageDigest.isEqual` + backoff **persistido** | Core `util/Pin.kt:13-27` · `features/profiles/ProfileGate.kt:97-101` |
| 24 | **SEC-05 / SEC-02** — credenciais em claro; segredo do edge no APK | ✅ | `SecretBox` (Keystore AES-256-GCM) por campo; chave do edge tratada como identificador público + medição server-side | Core `database/entity/ProfileEntities.kt:51-64` · `build.gradle.kts:84` |
| 25 | **R4-B01** — o restauro altera `isKids`/`pinHash` sem autorização, e a UI não mostra a alteração | ✅ | Ignorar esses campos ao importar sobre um perfil existente, **ou** pedir o PIN local e mostrar a mudança no preview | Core `backup/BackupManager.kt:859-887` |
| 26 | **R4-B04** — perfis Kids deixam passar canais sem `categoryId` | ✅ | Negação por omissão em Live, EPG, Downloads, Home e pesquisa, com excepção parental | Core `content/AdultCategoryClassifier.kt` · `LiveViewModel.kt:1110-1416` |

### 16.5 🟠 FASE 3 — Estabilidade, foco e UX fina (12 itens, ~1 semana)

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 27 | **R4-B09** — acções de gravação sem `try/catch` | ✅ | Envolver `stop`/`pauseArchive`/`resumeArchive`/`cancel`/`retry` | `features/recordings/RecordingsViewModel.kt:261-327` |
| 28 | **R4-B10** — `continueAfterCatchup` sem `try/catch` → **derruba a app** | ✅ | `try { … } catch (CancellationException) { throw } catch { parar sem continuar }` | `features/live/LiveViewModel.kt:2536-2554` |
| 29 | **R4-B02/R4-B03** — índice de linha lido como índice de item | ⚠️ **solução corrigida** | `rows.asSequence().drop(contextIndex).filterIsInstance<Item>().firstOrNull()` — um só espaço de índices | `DownloadsScreen.kt:154` · `RecordingsScreen.kt:211` |
| 30 | **PLY-01** — o OSD do CH+/CH− mostra o canal **de onde saiu** | ✅ | Passar a identidade do canal novo do shell e desenhar o flash a partir dela | `player/PlayerHud.kt:599-606` |
| 31 | **PLY-02** — no MultiView o anel de foco e o foco do D-pad discordam | ✅ | Requesters estáveis + `requestFocus()` explícito após `addTile()` | `features/multiview/MultiviewScreen.kt:90,92,128` |
| 32 | **PLY-10** — cancelar um direct tune mata a lookup em curso | ✅ | Não re-chavear o effect no cancelamento; descartar o resultado por *request id* | `player/PlayerHud.kt:310-322,335` |
| 33 | **PLY-11** — o direct tune resolve contra o canal **pendente** | ✅ | Resolver contra `_playingChannel` e manter o alvo de zap separado | `features/live/LiveViewModel.kt:1753,1786` |
| 34 | **PLY-06** — a escrita "imediata" do histórico é cancelada pelo write debounced | ✅ | Tornar "immediate" *sticky* | `features/live/LiveViewModel.kt:1713-1717,2301` |
| 35 | **PLY-07 + ZAP-01** — a 1.ª request de uma rajada não é protegida; a 1.ª pressão após uma escolha herda 500 ms | ✅ | Decidir "isolada" no *key release*; repor `lastZapPressMs` em `ensurePlaying` | `player/PlayerHud.kt:485-487` · `LiveViewModel.kt:2016` |
| 36 | **PLY-09** — o freeze frame é reciclável e é desenhado directamente → risco de crash | ✅ | Copiar o bitmap antes de o desenhar | `player/MpvVideoSurface.kt:138-146` |
| 37 | **UX-09** — o overlay de categorias não tem estado vazio nem focus trap | ✅ | Estado vazio focalizável + `trapAllFocusExit()` | `features/shell/components/CategoryBrowserOverlay.kt:76-82` |
| 38 | **UX-10** — o shell recompõe de 5 em 5 s e a cada play/pause | ✅ | Não ler `reminderClockMs` no scope do shell; `remember` nos callbacks por destino | `features/shell/OwnTVShell.kt:414-473,1303-1358` |
| 39 | **UX-11** — o `focusRestorer` condicional desactiva o "voltar ao controlo exacto" | ✅ | Instalar `focusRestorer()` incondicionalmente | `features/shell/OwnTVShell.kt:1175-1182` |
| 40 | **UX-12** — a linha "Agora" do Home engole o Back | ✅ | Não consumir o Back; devolver `false` | `features/home/HomeGuideSlice.kt:208-211` |
| 41 | **UX-06** — 15 botões do mini-player/Audio Mode sem `contentDescription` | ✅ | `semantics { contentDescription = … }` por verbo | `player/MiniPlayer.kt:192-220` · `AudioNowPlayingBar.kt:374-418` |
| 42 | **BUG-P3-03** (≡ PLY-04) — foco nos overlays do guia | ✅ | `requestBoundedFocus` / `requestFocusRetrying` | `features/epg/MiniGuideOverlay.kt` |

### 16.6 🟡 FASE 4 — Dados, performance e arquitectura (10 itens, ~2 semanas)

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 43 | **DAT-01** — o paging do catálogo Live materializa 50k canais e repete a cada invalidação | ✅ | Paginação incremental; `unregisterInvalidatedCallback`; snapshot indexado por revisão de conteúdo | `features/live/GroupedChannelPagingSource.kt:21-52` |
| 44 | **DAT-02** — 17 flows `Eagerly` constroem o `LiveViewModel` no primeiro frame | ✅ | `WhileSubscribed(5_000)` nos flows de BD; `Eagerly` só nas flags lidas sincronamente pelo playback | `features/live/LiveViewModel.kt` |
| 45 | **DAT-03** — `MAX_SIZE_UNBOUNDED` retém 50k entidades | ✅ | `maxSize` limitado + `placeholdersEnabled` | `features/live/LiveViewModel.kt:927-933` |
| 46 | **DAT-04** — o backup automático nunca vai para foreground → morto e apagado | ✅ | `setForeground(ForegroundInfo)` + trabalho retomável com checkpoint | `features/settings/AutomaticBackupWorker.kt:50-102` |
| 47 | **PERF-01** — 44 `StateFlow`s lidos no composable raiz das definições | ✅ **NOVO** | Mover cada leitura para o sub-composable que a usa (refactor mecânico) | `features/shell/components/SettingsScreen.kt:190-1559` |
| 48 | **PERF-02** — o hero a 12,5 Hz | ✅ **NOVO** | `animateFloatAsState` | `features/home/HomeScreen.kt:663-676` |
| 49 | **DAT-05..DAT-21** — throttle anulado, scope não estruturado, N+1, `StateFlow` de 20k, `versionGroupKeys` write-only, etc. | ✅ | Ver §5 | vários |
| 50 | **F15 fases 1-4** — `OwnTVShell` com 1 900 linhas num só composable; `SettingsScreen` com 4 446 | ✅ | Extrair estado → partir o dispatch → partir os diálogos → partir o `LiveViewModel` (a superfície pública **inalterada**) | `features/shell/OwnTVShell.kt` · `SettingsScreen.kt` |
| 51 | **F7** — 8 dependências mortas | ✅ | Remover `navigation-compose`, `room-compiler`, `ksp`, `media3-exoplayer`, `room-ktx`, `room-paging`, `tooling-preview`, `material-icons-core` | `app/build.gradle.kts` · `gradle/libs.versions.toml` |
| 52 | **CAN-01** — 3 sítios engolem `CancellationException` | ✅ | Uniformizar com os outros 87 | `MovieViewModel.kt:570,631` · `SeriesViewModel.kt:917` |

### 16.7 🟡 FASE 5 — Build, CI e i18n (6 itens, ~1 semana)

| # | Item | Veredicto | Solução | Ficheiro |
|---|---|---|---|---|
| 53 | **F4** — 266 strings da app traduzidas em 2 de 26 idiomas | ✅ | Verificador de resources (~40 linhas de Python) **ou** migrar para o Core; **remover `disable += "MissingTranslation"`** | `app/src/main/res/values*/` · `app/build.gradle.kts:216` |
| 54 | **QA-01** — 19 classes `androidTest` nunca correm | ✅ | Job com emulador (ou Gradle Managed Devices) | `.github/workflows/android.yml:51-54` |
| 55 | **QA-02** — sem SAST / Dependabot / SBOM / scan de segredos / CodeQL | ✅ | `.github/dependabot.yml` + `dependency-review-action` + `osv-scanner` + `gitleaks` + `codeql.yml` | `.github/` |
| 56 | **F8** — `libmpv` buscada sem verificação de integridade | ✅ | `gradle/verification-metadata.xml` com `sha256` | `settings.gradle.kts:23-31` |
| 57 | **QA-09** — `mapping.txt` nunca arquivado | ✅ | Upload como artefacto + anexar à release | `android.yml:228-234` |
| 58 | **F3 / REL-04** — `splits.abi` contraditório; o x86_64 nunca publicado | ✅ | Apagar `splits.abi` (a dimensão de flavor já faz a partição) **ou** fixar nomes explícitos no CI; falhar se o APK x86_64 não existir | `app/build.gradle.kts:110-117` · `android.yml:188` |
| 59 | **REL-08/REL-09** — 6 zips vendorizados órfãos; 1,78 GB de snapshots | ✅ | Apagar os órfãos; o `snapshot_core.py` passa a apagar o anterior; script de limpeza + `LongPathsEnabled` | `vendor/` · `.gradle/core-snapshot/` |

### 16.8 💡 As ideias que continuam válidas — e as que passaram a ser **primeira prioridade**

| Prioridade | Ideia | Esforço | Porquê agora |
|---|---|---|---|
| **1** | **Catálogo demo no primeiro arranque** (CAT-3) | M | Resolve o vazio dos 5 primeiros minutos (o leak F1) — e o demo dá conteúdo **ao coach de D-pad** (F7) |
| **2** | **Temporizador de sono** (U1) | S | Resolve um problema real (a TV acordada a descodificar 4K a noite) **e** liberta um slot de ligação do provider |
| **3** | **Estado "A sua TV está pronta"** em vez do Home vazio (F1) | S | 3 acções concretas; a correcção mais barata com mais impacto de retenção |
| **4** | **Coach de D-pad de 3 passos** (F7) | S | O modelo de interacção inteiro é "long-press OK" — um gesto que ninguém descobre |
| **5** | **Auto-Tune Memory** (CAT-1) | M | O maior ganho técnico do produto — **mas só depois de B1 e B7** (§16.9) |
| **6** | **TV-Remote** (CAT-2) | M-L | O servidor companheiro **já existe**; dá um emprego real ao OwnTV Mobile |
| **7** | **Pesquisa por voz** (U5) | M | Complementar do UX-01: a Busca precisa de uma porta *e* de um método de entrada |
| **8** | **Painel "A sua semana OwnTV"** (F8) | M | A única métrica de retenção possível sem servidor — e um artefacto partilhável para o Telegram |
| **9** | **"Esta noite" / O que ver agora** (F8 / §7.3) | M | O trigger de uso diário do produto |
| **10** | **Theme packs** (CAT-17) | S | Transforma a personalização (o que os utilizadores mais gostam) em artefacto partilhável |
| **11** | **Layout screen-safe (OLED)** (CAT-18) | S | Queimadura de ecrã é a queixa nº1 a longo prazo em apps de TV |
| **12** | **"Só em Wi-Fi" para o EPG** (reformulado, §15.8-bis) | S | Única política de rede que faz sentido numa box |
| **13** | **"Surpreende-me"** (U3) | S | Complementar da demo |
| **14** | **Veredicto de saúde por canal** (CAT-6) | M | ⚠️ **bloqueado por O4** — não enviar antes de corrigir a métrica |
| **15** | **Classificações reais** (CAT-8) | M-L | O modo Kids é um regex; este é o único que o torna fiável |
| **16** | **Códigos de canal partilháveis** (CAT-15) | M | "Vê este canal" não tem mecanismo |
| **17** | **Pilha de retoma da casa** (CAT-16) | M | O feature mais pedido numa TV partilhada — com PIN |
| **18** | **EPG diferencial** (CAT-19) | M | O guia é a superfície diária **e** o maior custo de rede |
| **19** | **Audiodescrição** (U2) | S/M | **Incluída** — uma faixa de áudio é ignorada sem oOSE nem sequer o utilizador saber |
| **20** | **Salto de publicidade** (CAT-20) | L | ⚠️ **mantém-se em último**, pelo risco de falso positivo e legal |

### 16.9 As dependências que mordem (a ordem real de execução)

1. **REL-05 → REL-01 → REL-02 → REL-06** — o pipeline de release tem de ser reparado **antes** de
   mais nada. Sem isto, nenhuma correcção chega ao utilizador.
2. **UX-01, UX-03, UX-04** — de volta a Bring back as features. Custa 1 dia.
3. **UX-02 ⟷ PERF-02** — **juntas**, nunca separadas.
4. **B1 e B7 antes de CAT-1** — o Auto-Tune Memory persiste as lições do motor; persistir uma lição
   errada (B7: "DASH aprendido a partir de outro canal sobrepõe um URL HLS explícito") transforma um
   bug de sessão num bug **persistente** que sobrevive a reinícios.
5. **O4 antes de CAT-6** — `ThroughputTracker` a ignorar `isNetwork` e `ExoStreamStats` a apresentar
   throughput de transporte como bitrate. Um veredicto de saúde construído sobre essas métricas vai
   **culpar o Wi-Fi do utilizador** pelo CDN do provider.
6. **SEC-12 antes de tocar no formato do backup** — qualquer alteração ao KDF tem de vir com a
   validação, ou reintroduz-se o DoS.
7. **F15 fases 1-4 antes de módulos Gradle de feature** — extrair estado primeiro; os módulos
   depois. E **as features ficam no repositório da app**, nunca no Core (que está fixado por um
   snapshot SHA-256 — uma mudança de UI lá obrigaria a re-snapshot e a um bump do lock).

### 16.10 O que foi arquivado (e porquê) — para não voltar a debate-lo

| ID | Veredicto | Razão |
|---|---|---|
| **BUD-01** (orçamento sem fullscreen) | ❌ | Design deliberado e documentado em `WatchSession.kt:9-33`. **A "correção" causaria um bug novo** (recusar mosaicos de Multiview) |
| **NET-01** (política de dados móveis ausente) | ❌ | O Core implementa-a e documenta a política. Para uma box de TV não é aplicável. **Só o EPG fica por cobrir** (§15.8-bis) |
| **"Armazenamento não governado"** (produto, ronda 5) | ❌ | O Core tem `MediaStorageBudget` com quotas, `Lease` e motivos. **O que falta é a visibilidade** (→ DWN-01) |
| **BUG-P2-07** (PIN sem autenticação) | ⚠️ | Exagerado: a acção **exige** o PIN actual quando existe. O caso real é o **Kids sem PIN** (R4-B08) |
| **R4-B14 / R4-B24** | ⚠️ | Confirmados, mas a descrição/subida de severidade precisa de actualização (→ SEC-01, F7) |
| **BUG-P5-01**, **R4-B16**..**R4-B26** | 🔁 | Já registados sob outros IDs nesta e nas rondas anteriores |
| **`MultiviewState.cleanupScope`** | ⚠️ | O Council anterior tinha razão quanto à **falta de fuga de recursos**; mas o *scope* não é cancelado. **P3**, correcção de uma linha |
| **Migração 5→6 em falta** | ❌ | Todas as versões de esquema estão presentes; o passo 5→6 é transitório |
| **UPSCALING-BETA-01** | 🔁 | Já registado em `docs/MELHORIAS_FUTURAS_PENDENTES.md` — respeitado, não re-proposto |

---

## 17. Limitações desta ronda

1. **Os 6 agentes da Ronda 6 falharam** por uma queda de ligação. O conselho re-executou a ronda
   pessoalmente, com verificação directa, mas a **cobertura por área ficou mais fina** do que o
   planeado — em particular: o Core foi varrido por的主题 (esquema/DAOs, parsers, rede, backup,
   storage) e **não** por cada um dos 23 DAOs; os ecrãs de definições foram varridos por
   *mecânica* (chaves órfãs, fluxo de `StateFlow`) e **não** linha a linha; o tooling foi
   verificado nos pontos de maior risco e **não** exaustivamente.
2. **Sem build, sem testes, sem dispositivo.** Nada foi reproduzido em hardware. As medições de
   tamanho de APK são de artefactos de build reais; os de RAM são estimados a partir da cadeia de
   cópias.
3. **O Core é um snapshot** (`e2081e78…`). O repositório upstream pode já ter resolvido parte
   destas questões.
4. **As alegações sobre o AGP** (F3) continuam verificadas contra as fontes do AGP 9.4.1 no cache
   local, não contra a documentação.
5. **A verificação bash** foi feita em Git Bash 5.x, não no runner exacto do GitHub Actions.
6. **A secção 7 (produto) é opinião fundamentada**, não medição de mercado.

---

## 18. Conclusão final

Duas rondas de conselho, doze especialistas, e o resultado é mais simples do que a lista de 99
achados sugere:

**O OwnTV é um codebase excelente preso dentro de um pipeline de distribuição partido, e com
quatro funcionalidades centrais que existem mas não têm porta.**

A cadeia é sempre a mesma:

1. **O release não chega ao utilizador** — porque a tag rebenta (§3, REL-05), porque se publica o
   APK errado (REL-01), porque o link do README aponta para um repositório que não existe (DOC-01).
2. **A app não se mostra como promete** — a Pesquisa não tem porta (UX-01), Now Trending está morto
   (UX-02), "Downloads" mostra gravações (UX-03), Favoritos e Histórico são inalcançáveis (UX-04),
   e 24 dos 26 idiomas estão a meio em inglês (F4).
3. **Só depois faz sentido** a engenharia de fundo: o storage budget, a hierarquia de cancelamento,
   os 44 `StateFlow`s, o `splits.abi`, o FTS, o esquema Room com 51 versões, o `WatchSession`, o
   `MediaStorageBudget` com quotas e `Lease`, a camada de foco com `requestBoundedFocus` e
   `FocusTrap`, o `SettingsSearchCoverageTest` que deriva de `VIDEO_QUICK_ROWS`, os 87 sítios que
   re-lançam `CancellationException` correctamente, o verificador de literais em Python ligado ao
   `preBuild`, o lock SHA-256 do Core com re-hash de 1 007 ficheiros.

**Esse terceiro bloco é o que esta app tem de melhor, e é por isso que a corrección
distribuição vale mais do que qualquer outra coisa.**

E há uma segunda leitura, mais barata: **o que a Ronda 6 encontrou de novo é, quase todo, do
mesmo tipo.** SEC-12 (DoS), SEC-14 (OOM), DATA-01 (perda de dados silenciosa), DWN-01 (motivo
perdido), PERF-01/02 (recomposição), ZAP-01 (500 ms de atraso), CAN-01 (3 excepções a 87) — são
**todos** "a camada por cima está correcta, a camada por baixo perde a informação". O mesmo padrão
de UX-01 a UX-04: **a funcionalidade existe; a porta não.**

A Recommendation final do conselho, nesta ordem:

> **Primeiro a distribution** (REL-05 → REL-01 → REL-02 → DOC-01). **Depois as portas de entrada**
> (UX-01 → UX-03/04 → UX-02). **Depois a promessa de produto** (F4 — os 26 idiomas). **Depois o
> resto** (segurança, dados, performance, refactorização), que é importante e é a **quarta** vez
> que importa. **E depois as ideias** — a demo no primeiro arranque, o temporizador de sono, o
> coach de D-pad — que são as que transformam uma app bem feita numa app que as pessoas usam.

---
*Fim do relatório. Partes I (Ronda 5) e II (Ronda 6), 2026-10-19. Consolidação e verificação directa
do conselho; as 6 rondas anteriores foram revalidadas item a item (§15).*

---

# ═══════════════════════════════════════════════════════════════════════
# PARTE III — RONDA 7 (19/10/2026)
# Verificação dos achados do próprio projecto + novos achados + reavaliação
# ═══════════════════════════════════════════════════════════════════════

---

## 19. Ronda 7 — método e o que há de novo

### 19.1 A pergunta que a Ronda 7 responde

As rondas 1–6 auditaram **o código**. A Ronda 7 audita **os achados do próprio projecto**: o
repositório tem ~**71 achados seus** que nunca foram verificados contra o código actual:

| Fonte | Itens | Nunca verificados? |
|---|---|---|
| `docs/PLANO_MESTRE_STREAMING_AUDIO_VIDEO_APP_UI_2026-10-02.md` | **B1–B10** (reprodução), **C1–C17** (código/arquitectura), **O1–O9** (observabilidade) = **36** | ✅ sim |
| `docs/REVISAO_CRITICA_PLANO_69_ITENS_COUNCIL_2026-10-13.md` | B13, B20–B23, B27–B30, B32, B34–B35, B37–B38, I8, L4, O10, O13 = **19** | ✅ sim |
| `docs/AUDITORIA_ESPECIALIZADA_TV_NOVOS_ACHADOS_2026-10-15.md` | **A15-01 … A15-16** = **16** | ✅ sim |

É a maior fonte de achados não verificados do projecto, e vários estão marcados como
**bloqueadores de funcionalidades futuras**.

### 19.2 Conselho da Ronda 7 (6 especialistas)

| # | Especialidade | Âmbito |
|---|---|---|
| **E13** | Media/playback | Verificação da série **B1–B10** + B13, B20–B23 (com foco em **B1** e **B7**, os bloqueadores do Auto-Tune Memory) |
| **E14** | Robustez/observabilidade | Verificação da série **C1–C17** + **O1–O9** + I8, L4, O10, O13 + `AUDITORIA_COUNCIL_ROBUSTEZ_COERENCIA_LIMPEZA_2026-10-13.md` (34,7 KB) |
| **E15** | TV/UX | Verificação da série **A15-01 … A15-16** + as auditorias de 08/10 e 10/11 |
| **E16** | Dados/Room | Auditoria profunda dos **23 DAOs** do Core, índices vs. esquema, FTS4, migrações 2→51 |
| **E17** | Motores | Auditoria profunda do **player-core**: escada de fallback, handover mpv↔Exo, métricas |
| **E18** | Definições/UX fina | Leitura linha a linha dos ecrãs de definições e das superfícies secundárias (a área mais fina da Ronda 6) |

Mais um **conselho de reavaliação** que re-verificou os 10 achados novos da Ronda 6 e uma amostra
de 20 dos 59 itens priorizados em §16, procurando **correções que estejam erradas**.

### 19.3 O que o conselho encontrou

> **Os achados do próprio projecto estão, na sua maioria, ERRADOS ou JÁ RESOLVIDOS — mas o
> *raciocínio* por trás deles continua a ser o diagnóstico correcto, e é onde está o valor.**

O caso mais revelador é o **B7** (§20.2): o bug foi **corrigido no código**, mas o **comentário que
documenta a função continua a descrever a ordem antiga** — e **não há teste** que fixe a prioridade.
Qualquer pessoa que "alinhe o código com o comentário" **reintroduz o bug**. E, ironicamente, é
exatamente esse bug que bloqueia a **Auto-Tune Memory** (a maior ideia de produto do projecto), porque
persistir as lições do motor amplifica precisamente esta classe de erro.

O caso mais **valioso** é o **R7-01** (§20.1): o *"bloqueio A–B–A"* — o problema provider-side que o
próprio projecto reconhece como o seu bloqueio nº1 — tem uma mitigação **temporal** (esperar 20 s)
quando o que o provider pune é a **contagem** de sessões. Isso é um achado **novo**, e é o único até
agora que ataca o bloqueio de raiz.

---

## 20. Achados novos da Ronda 7

### R7-01 — 🔴 O bloqueio A–B–A é mitigado no **tempo**, mas o provider pune a **contagem**

**Severidade: ALTA (produto) · Confiança: alta · Verificado**

**Localizações:** Core `player/LiveTuneController.kt:209-236`, `player/RecentChannelReopen.kt`,
`core/settings/ChannelReopenInterval.kt`; app `player/PlayerHud.kt:780-786`,
`features/live/LiveViewModel.kt:1493`, `features/shell/OwnTVShell.kt:1669`

#### O que está implementado (e está bem feito)

O projecto reconhece o padrão. O Core tem uma política dedicada:

```kotlin
// core/settings/ChannelReopenInterval.kt
/** Quiet time after leaving a live channel, measured with a monotonic clock. */
object ChannelReopenInterval {
    const val DEFAULT_SECONDS = 20
    const val MIN_SECONDS = 10
    const val MAX_SECONDS = 50
    val CHOICES = MIN_SECONDS..MAX_SECONDS
    fun normalize(seconds: Int): Int = seconds.coerceIn(MIN_SECONDS, MAX_SECONDS)
}
```

```kotlin
// player-core/…/player/RecentChannelReopen.kt
/** First visits and promotion of an already playing channel do not incur a quiet period. */
fun select(key: Pair<Long, Long>, nowMs: Long, intervalSeconds: Int): Long {
    …
    val departed = leftAt[key] ?: return 0L          // ← primeira visita: grátis
    val elapsed = (nowMs - departed).coerceAtLeast(0L)
    return (normalize(intervalSeconds) * 1000L - elapsed).coerceAtLeast(0L)
}
```

E o HUD **mostra a espera**, o que é o correcto (`PlayerHud.kt:780-786`):
```kotlin
buffering || reopenWait > 0 -> … {
    OwnTVSpinner(sizeDp = 56)
    if (reopenWait > 0) { Text(stringResource(R.string.player_reopen_wait, reopenWait), …) }
```

E a espera é **configurável pelo utilizador** (`VideoPlayerSettingsScreen.kt:1192,1434`, faixa
10–50 s, validada em `SettingsRepository.kt:2892`).

#### Onde está o problema

**1. A mitigação é temporal; o provider pune a contagem.**

O `LiveTuneController` **fecha completamente** o transporte anterior antes de reabrir
(`LiveTuneController.kt:216-218`):
```kotlin
216:            engines.mpvStop()
217:            var exoReleased = engines.exoStopAndAwaitDrain()
218:            var mpvReleased = engines.mpvStopAndConfirmRelease()
```
e só depois espera o intervalo de silêncio (`:219-224`) antes de reabrir.

Logo, numa sequência **A → B → A**:

| Momento | Sessões do provider |
|---|---|
| abre A | 1 |
| muda para B (fecha A) | 2 |
| volta a A (fecha B, reabre A) | **3** |

**Três sessões para dois canais** — exactamente o padrão que o provider bloqueia. A espera de 20 s
não altera a contagem: só a **separa no tempo**.

**2. A espera é incondicional.** Não há nenhuma evidência de que o provider tenha bloqueado — o
intervalo aplica-se a **todos** os providers, mesmo a quem nunca bloqueou nada. Um provider sem
bloqueio paga 20 s sem motivo.

**3. A mitigação desliga-se sozinha.** O `select()` só cobra o intervalo a quem **saiu** do canal
(`.departed()`); a primeira visita é grátis. Mas o `LiveViewModel.kt:1864-1865` só evita a
reeabertura quando o canal **ainda está a reproduzir**. A volta ao canal anterior — que é o caso
A→B→A — paga sempre.

#### Solução em dois incrementos

**Incremento 1 (S) — tornar a espera condicional em evidência, não em relógio.**

A app **já sabe** quando o provider recusa: trata o HTTP 458 e o 429 com `Retry-After`
(`LivePreviewEngine.providerBackOff`, `isFormatFailure`). Falta só **lembrar a recusa**:

```kotlin
// core/…/player/RecentChannelReopen.kt — acrescentar
private val refusedAt = LinkedHashMap<Pair<Long, Long>, Long>()

/** Record that the provider actually refused to re-open this channel. */
fun refused(key: Pair<Long, Long>, nowMs: Long) { refusedAt[key] = nowMs }

/**
 * The quiet period is a FALLBACK, not a policy: it is only owed after the provider has
 * actually refused a re-open. A provider that never blocks never pays for it.
 */
fun select(key: Pair<Long, Long>, nowMs: Long, intervalSeconds: Int): Long {
    …
    val refusedAt = refusedAt[key] ?: return 0L          // ← sem recusa, sem espera
    val elapsed = (nowMs - refusedAt).coerceAtLeast(0L)
    return (normalize(intervalSeconds) * 1000L - elapsed).coerceAtLeast(0L)
}
```

E no `LiveTuneController`, marcar `refused(...)` quando o rung que reabre o canal recebe 458/429.
**Resultado:** o provider que bloqueia continua protegido; o provider que não bloqueia deixa de
custar 20 s ao utilizador.

**Incremento 2 (M/L) — reusar a sessão em vez de reabrir o canal.**

A correcção de raiz: **não abrir uma segunda sessão do mesmo canal.** Manter o transporte do canal
deixado vivo durante a janela de silêncio e **re-ligar a superfície** quando o utilizador volta,
em vez de parar e reabrir. Então:

| | Sessões do provider | Latência da volta |
|---|---|---|
| **Hoje** | 3 | 20 s (por omissão) |
| **Com reuso** | **2** | ~1 s |

E o provider **nunca vê** o padrão que o punha. Isto é o que resolve o bloqueio de raiz.

**Esforço:** o Incremento 1 é pequeno e está no Core. O Incremento 2 exige re-ligar a superfície
nativa a um transporte vivo — é trabalho de motor, e o Core já tem o gancho
(`MpvPlaybackEngine` / `LivePreviewEngine` já expõem `attachSurface`/`detachSurface`).

**Incremento 3 (S) — explicar a espera.** O HUD mostra "N s" mas não **porquê**. Uma linha a dizer
*"a aguardar 12 s — o seu provider não permite reabrir este canal tão depressa"* (com um atalho para
a definição) transforma um mistério numa explicação e dá ao utilizador o controlo.

#### Nota: o que **não** é bug

Os documentos do projecto atribuem correctamente o bloqueio ao **provider** —
*"Não demonstra a causa de bloqueios do fornecedor em A–B–A"* (`AUDITORIA_COUNCIL_DEFINICOES_REPRODUCAO_UI_LIMPEZA_2026-10-11.md:27`).
**A app não tem um bug aqui** — tem uma mitigação conservadora. O achado é sobre a **eficácia** da
mitigação, não sobre um defeito.

---

### R7-02 — 🟠 O B7 foi corrigido no código, mas o **comentário documenta a ordem antiga** e **não há teste**

**Severidade: ALTA (risco de reintrodução) · Confiança: alta · Verificado**

**Localizações:** Core `player/LivePreviewEngine.kt:3737-3763`; `player-core/src/test/…/StreamRouteTest.kt`

#### O que o projecto documentou (B7)

> *"Após descobrir MPD numa URL sem extensão, a app guarda a lição para o host. `knownDashHost`
> precede `inferredHls`; uma URL posterior `.m3u8` no mesmo host sem manifest_type pode ser entregue
> ao parser DASH."*

#### O que o código faz **hoje**

```kotlin
// LivePreviewEngine.kt:3753-3763
): StreamRoute = when {
    manualTs  -> StreamRoute.PROGRESSIVE
    forceHls  -> StreamRoute.HLS
    forceDash -> StreamRoute.DASH
    declared == ManifestType.MPD -> StreamRoute.DASH
    declared == ManifestType.HLS -> StreamRoute.HLS
    inferredHls     -> StreamRoute.HLS      // ← LINHA 3759: AGORA ANTES
    knownDashHost   -> StreamRoute.DASH     // ← LINHA 3760: AGORA DEPOIS
    knownHlsHost    -> StreamRoute.HLS
    else            -> StreamRoute.PROGRESSIVE
}
```

**A ordem foi invertida** — `inferredHls` (3759) passou **à frente** de `knownDashHost` (3760).
**O conflito do B7 está resolvido**: uma URL `.m3u8` num host "aprendido" como DASH vai para **HLS**.

#### O problema

**1. O KDoc ainda documenta a ordem antiga.** Linhas 3737-3740:

```
 *  3. **[knownDashHost] / [knownHlsHost]** — a panel-wide lesson from some other channel here.
 *  4. **[inferredHls]** — Media3's own guess from the URL's extension.
```

O comentário diz que a lição do host é a prioridade **3** e a inferência **4** — **o inverso do
código**. A ordem correcta é a **3 = inferredHls**, **4 = knownDashHost/knownHlsHost**.

> **Perigo concreto:** a próxima pessoa que "alinhe o código com o comentário" — ou que reordene
> o `when` por legibilidade — **reintroduz o B7**, e o B7 está marcado como bloqueador do
> **Auto-Tune Memory** (a maior ideia de produto do projecto). Como o Auto-Tune Memory *persiste* as
> lições do motor, o B7 passaria de bug de sessão a **bug permanente**, sobrevivendo a reinícios.

**2. Não há teste que fixe a prioridade.** `StreamRouteTest.kt` tem ~15 asserções sobre `routeFor`,
incluindo `route(knownDashHost = true)` → DASH (linha 116),
`route(declared = HLS, knownDashHost = true)` → HLS (linha 128) e
`route(knownHlsHost = true, knownDashHost = true)` → DASH (linha 137). Mas **não existe** o caso
exacto do B7 — `knownDashHost = true` **e** `inferredHls = true`, sem declaração. A ausência foi
apontada pelo próprio projecto: *"Teste existente de prioridade entre hosts aprendidos não cobre este
conflito com URL concreta."*

#### Solução

**1. Corrigir o KDoc** (uma linha de KDoc, zero de código):

```
 *  3. **[inferredHls]** — Media3's own guess from the URL's extension. An explicit `.m3u8` is
 *     evidence about THIS url; a host lesson was learned from a different channel.
 *  4. **[knownDashHost] / [knownHlsHost]** — a panel-wide lesson from some other channel here.
```

**2. Fixar o caso com um teste** (o que o projecto já pedia):

```kotlin
/**
 * The B7 case: a panel that served MPDs on one channel, and this channel's URL is an
 * explicit `.m3u8`. The url is the more specific evidence — the host lesson came from
 * a different channel.
 */
@Test
fun `an explicit m3u8 outranks a panel-wide dash lesson`() {
    assertEquals(StreamRoute.HLS, route(knownDashHost = true, inferredHls = true))
}
```

**3. Não tocar no `when` sem o teste acima.** O `routeFor` é uma função pura e testável — vale a pena
deixar isso escrito junto ao código, porque a próxima reordenação é o momento de reintrodução.

> **Veredicto sobre o B7: 🔧 JÁ CORRIGIDO (no código), ⚠️ com a documentação errada e sem teste.**
> E é por isso que o **Auto-Tune Memory continua bloqueado** — não pelo bug, mas pela **ausência do
> teste que garante que ele não volta**.

---

### R7-03 — 🟡 O B1 foi corrigido, mas a interface mantém os **defaults que produzem exactamente o bug**

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** Core `player/PlaybackEngine.kt:14-17`, `:179-182`; `player/OwnTVPlayer.kt:1375`;
`player/PlaybackSession.kt:95-97, 131, 248, 256`; `player/LiveTuneController.kt:517-518, 594, 842-845`

#### O que o projecto documentou (B1)

> *"O adaptador `MpvPlaybackEngine` … **não substitui `playbackRequested`**. O default é `isPlaying`,
> que pode ficar false durante BUFFERING apesar de o pedido continuar a ser reproduzir."*

#### O que o código faz **hoje** — o B1 foi resolvido

| Verificação | Estado |
|---|---|
| `MpvPlaybackEngine` substitui `playbackRequested` | ✅ `PlaybackEngine.kt:181` — `override val playbackRequested get() = p.playbackRequested` |
| `MpvPlaybackEngine` substitui `playbackRequestRevision` | ✅ `PlaybackEngine.kt:182` |
| O mpv tem uma **intenção** com revisão | ✅ `OwnTVPlayer.kt:1375` — `val playbackRequestRevision get() = playbackIntent.revision` |
| A sessão combina intenção + revisão | ✅ `PlaybackSession.kt:95` — `combine(engine.playbackRequested, engine.playbackRequestRevision)` |
| O resume é **idempotente** (um Pause repetido não retoma) | ✅ `PlaybackSession.kt:248-256` — `ResumeTicket(owner, revision)` + `mayResumeOwnedPause(...)` |
| O controlador de tune espera pela **intenção**, não pelo estado transitório | ✅ `LiveTuneController.kt:517-518, 594, 842-845` |
| **Há testes de regressão** | ✅ `LiveTuneControllerTest.kt:700-1027` — mais de uma dezena de cenários que põem `playbackRequested = false` e incrementam a revisão |

**O B1 está corrigido, e corrigido com testes.** A evidência do documento (a interface) está
desactualizada; os **adaptadores** já não usam o default.

#### O que fica — e é a parte interessante

A **interface** ainda declara os defaults que o B1 descreve:

```kotlin
// PlaybackEngine.kt:14-17
/** Requested play/pause, independently of loading or temporary suppression. */
val playbackRequested: StateFlow<Boolean> get() = isPlaying      // ← o default que o B1 descreve
/** Revision of deliberate commands, including a repeated Pause while already paused. */
val playbackRequestRevision: StateFlow<Long> get() = ZERO_LONG    // ← idem
```

E o mesmo padrão está na fachada `LiveEngines.kt:49-52`:

```kotlin
val exoPlaybackRequested: Boolean get() = true
val mpvPlaybackRequested: Boolean get() = true
val mpvPlaybackRevision: Long get() = 0L
val exoPlaybackRevision: Long get() = 0L
```

(aqui só o *fake* de teste os usa, e a implementação real substitui os quatro — `LiveEngines.kt:133-136`).

**Isto é uma armadilha latente, não um bug activo.** Um motor novo, ou um adaptador novo, que
esqueça de substituir `playbackRequested` **herda silenciosamente o bug exacto do B1**: reporta
"não está a reproduzir" durante o BUFFERING, a sessão perde a intenção, e um Pause manual pode ser
desfeito por uma recuperação.

#### Solução — tornar o default impossível

A correcção não é comportamental, é **estrutural**: remover o default obriga o compilador a exigir a
intenção.

```kotlin
interface PlaybackEngine {
    val isPlaying: StateFlow<Boolean>

    /**
     * Requested play/pause, independently of loading or temporary suppression.
     *
     * NO DEFAULT ON PURPOSE. The old default was `isPlaying`, which reports false while
     * BUFFERING even though the request is still "play" — the session would then lose the
     * user's intent and a manual pause could be undone by a recovery. Every engine must
     * state its intent, so this is abstract on purpose: a new engine that forgets fails to
     * COMPILE rather than shipping the bug.
     */
    val playbackRequested: StateFlow<Boolean>

    /** Revision of deliberate commands, including a repeated Pause while already paused. */
    val playbackRequestRevision: StateFlow<Long>
}
```

O mesmo para a fachada `LiveEngines`. **Custo: uma tarde. Risco: nenhum** (a implementação real já
substitui os quatro).

---

### R7-04 — ⚠️ Reclassificação: o "bloqueio A–B–A" é **do fornecedor**, e o projecto já o diz

**Veredicto: ✅ correcta a atribuição, ❌ não é um bug da app.**

Os documentos do projecto são consistentes e explícitos:

| Documento | Frase |
|---|---|
| `AUDITORIA_COUNCIL_DEFINICOES_REPRODUCAO_UI_LIMPEZA_2026-10-11.md:27` | *"Não demonstra a causa do bloqueio A–B–A em TV em directo."* |
| `AUDITORIA_ESPECIALIZADA_TV_NOVOS_ACHADOS_2026-10-15.md:9` | *"Nenhum item abaixo demonstra a causa de bloqueios do fornecedor em A–B–A."* |
| `ATUALIZACOES_OWNCORE_PENDENTES_DE_IMPLEMENTACAO.md:21` | *"Reprodução física e bloqueio A–B–A continuam por validar."* |
| `ANALISE_CORE_1.0.67_2026-10-12.md:62` | *"Esta release não contém evidência suficiente para declarar resolvido o bloqueio A–B–A."* |

**Nenhuma ronda anterior captou isto**, e importa porque impede que se implemente uma "correção"
que não toca na causa. O que a app pode fazer é **reduzir a exposição** ao padrão — e é o que o
R7-01 propõe.

**Acção: registar como característico do provider, e ligar o R7-01 ao bloqueio nº1 do projecto.**

---

### R7-05 — 🟡 O A15-13 foi corrigido, mas a correcção **criou** o acúmulo de arquivos órfãos (é a origem do REL-08)

**Severidade: BAIXA (higiene) · Confiança: alta · Verificado**

**Localização:** `tools/core_snapshot/snapshot_core.py:36-87`

#### O que o projecto documentou (A15-13)

> *"falha ao gerar snapshot pode substituir ZIP sem atualizar o lock — `tools/core_snapshot/snapshot_core.py:41-53`
> escreve diretamente o ZIP final antes de verificar a versão. **Prova executada** com módulo real,
> ROOT temporário e checkout sintético: exitCode=2, archiveChanged=true, lockUnchanged=true."*

#### O que o código faz **hoje** — está corrigido

```python
def publish_snapshot(core, root=ROOT):
    """Publish immutable content first; atomically switch the lock only after it is complete.

    A crash before the lock swap leaves the previous pair valid. An unreferenced new ZIP is
    harmless and intentionally retained; old snapshots are never deleted by this command.
    """
    …
    with tempfile.NamedTemporaryFile(dir=vendor, suffix=".tmp", delete=False) as stream:   # ← temporário
        …
        os.fsync(stream.fileno())
    checksum = hashlib.sha256(archive_temp.read_bytes()).hexdigest()
    destination = vendor / f"owntv-core-source-{checksum}.zip"
    …
    else:
        os.replace(archive_temp, destination)          # ← publicação atómica
    lock = {…}
    with tempfile.NamedTemporaryFile(dir=root, mode="w", …) as stream:
        …
        os.fsync(stream.fileno())
    os.replace(lock_temp, root / "core-source-lock.json")   # ← troca atómica do lock
```

O ZIP passa por um **ficheiro temporário** com `fsync` e é publicado com `os.replace`; o lock é
escrito num temporário e trocado atomicamente. **Um crash entre as duas trocas deixa o par
anterior válido.** O A15-13 está **corrigido**, e bem corrigido.

#### O que ficou — e liga a duas rondas

O docstring diz tudo: *"An unreferenced new ZIP is harmless and **intentionally retained**; old
snapshots are never deleted by this command."* E o `finally` (linhas 83-86) só apaga os
**temporários**.

**Portanto: cada bump do Core deixa um `owntv-core-source-<sha>.zip` órfão em `vendor/`** — e é
**exactamente** o que a Ronda 5 mediu no **REL-08** (7 zips, 6 órfãos, ~23 MB versionados).

> **Síntese:** a correcção do A15-13 (publicação atómica) **criou** o acúmulo que o REL-08 observa.
> Não é um bug do A15-13 — é a sua consequência, que ninguém fechou.

**Impacto:** ~4 MB de binários por bump de Core, versionados no git para sempre (o `git gc` não
ajuda — estão no histórico). Com 6 bumps já são 23 MB; em 50 bumps, ~200 MB de repositório.

**Solução** — fechar o ciclo dentro da mesma função, **depois** da troca do lock:

```python
    os.replace(lock_temp, root / "core-source-lock.json")

    # Keep the published archive and the one before it (a release may still reference it);
    # drop the rest. A crash before this point leaves the previous pair valid, as before.
    archives = sorted(vendor.glob("owntv-core-source-*.zip"), key=lambda p: p.stat().st_mtime)
    for stale in archives[:-2]:
        stale.unlink(missing_ok=True)
    return lock
```

E, para não voltar a acumular: **não versionar** os arquivos antigos — ou movê-los para fora do
repositório depois da publicação. O `vendor/README.md` (0 bytes) devia dizer qual é o autoritativo.

---

## 21. Verificação dos achados do próprio projecto (Ronda 7)

### 21.1 Como esta secção foi feita

Para cada item **B**, **C**, **O**, **A15**, **I** e **L** dos documentos do projecto, o conselho
re-leu o código actual (commit `bdffec4`) e atribuiu um veredicto, **revisendo também a solução
proposta no documento** — porque um diagnóstico certo com uma solução errada é pior do que nada.

### 21.2 A tabela de veredictos

| ID do projecto | Veredicto | Onde está hoje | Acção |
|---|---|---|---|
| **B1** — play/pause/recuperação sem intenção consistente | 🔧 **JÁ CORRIGIDO** | `PlaybackEngine.kt:181-182` (os adaptadores substituem ambos os membros), `OwnTVPlayer.kt:1375` (intenção com revisão), `PlaybackSession.kt:95-97,248-256` (resume idempotente com `ResumeTicket`), `LiveTuneController.kt:517-518,594,842-845`, e **testes** em `LiveTuneControllerTest.kt:700-1027` | **Ver R7-03** — remover os defaults da interface para o bug não poder voltar |
| **B7** — aprendizagem DASH sobrepõe URL HLS explícita | 🔧 **JÁ CORRIGIDO (no código)**, ⚠️ **com o KDoc errado e sem teste** | `LivePreviewEngine.kt:3759` (`inferredHls`) passou **à frente** de `:3760` (`knownDashHost`) | **Ver R7-02** — corrigir o KDoc (3737-3740) e adicionar o teste que falta |
| **O4** — `ThroughputTracker` ignora `isNetwork`; throughput apresentado como bitrate | 🔧 **JÁ CORRIGIDO (as duas metades)** | `ThroughputTracker.kt:32` — `if (!enabled \|\| !isNetwork \|\| bytesTransferred <= 0) return`; e `ExoStreamStats.kt:21-26` — `bitrateRow` dá o bitrate **do codec** e `networkRateRow` dá a taxa de rede, **em linhas separadas** | **Nenhuma** — e isto **desbloqueia o CAT-6** (§21.4) |
| **A15-01** — um valor legítimo das legendas torna o backup irrestaurável | 🔧 **JÁ CORRIGIDO** | `core/backup/SettingsBackupValidation.kt` valida tipos, escolhas de enum e **intervalos** (`audio_delay_ms` ∈ −5000..5000, `sub_scale*` ∈ 0.25..4.0) **antes** de qualquer escrita (`BackupImportValidation.kt:6-10`) | **Nenhuma** — mas ver a nota de sintese em §21.3 |
| **A15-13** — falha ao gerar snapshot substitui o ZIP sem actualizar o lock | 🔧 **JÁ CORRIGIDO** | `snapshot_core.py:54-81` — temporário + `fsync` + `os.replace` para o ZIP **e** para o lock | **Ver R7-05** — a correcção deixou os arquivos órfãos (é a origem do REL-08) |
| **A15-15** — a CI não pede as suites Core personalizadas | 🔧 **JÁ CORRIGIDO** | `android.yml:54` corre `:OwnTV_Core:core:testDebugUnitTest :OwnTV_Core:player-core:testDebugUnitTest` | **Nenhuma** — mas o que falta é o `release` e o `x86_64` (QA-01) |
| **BUG-P1-01** ≡ A15-01 · **BUG-P1-02** ≡ SEC-01 · **BUG-P1-04** ≡ A15-04 · **BUG-P2-02** ≡ A15-06 · **BUG-P2-04** ≡ A15-08 · **BUG-P2-05** ≡ A15-09 · **BUG-P2-06** ≡ A15-10 · **BUG-P3-01** ≡ A15-11 · **BUG-P3-02** ≡ A15-12 | ✅ **VALIDOS** | Re-lidos; nenhum corrigido | Ver §15.7 — as soluções mantêm-se |
| **A15-04** — restauro selectivo pode confundir IDs coincidentes | ⚠️ **PARCIALMENTE VALE** | O Core tem uma classe nova `BackupRestoreIdentity` (`core/backup/BackupRestoreIdentity.kt:7-33`) — *"Only mappings resolved by this restore may attach data"* — e as rotas SETTINGS usam-na. **Mas `remapKeys` (`BackupManager.kt:1682-1691`) continua a fazer *pass-through* de chaves numéricas sem mapping**, e é usado em `customizations` / `homeConfigs` / `hideNewCategories` (`:1038-1045`) e nas maps de auto-refresh (`:1016-1018`) | **Ver R7-06** |
| **A15-02** — ocultação de segredos não uniforme nos pedidos de catálogo | 🔧 **JÁ CORRIGIDO**, ⚠️ **com um resíduo** | O redator de query foi substituído por `HttpClient.diagnosticOrigin(url)` (`core/network/HttpClient.kt:137-143`), usado em todos os logs. **Mas o caminho Stalker tem uma segunda política**: `StalkerClient.kt:712` — `val safeUrl = url.substringBefore('?')` | **Ver R7-07** |
| **A15-10** — callbacks de edição EPG não conservam o mesmo owner | 🔧 **JÁ CORRIGIDO**, ⚠️ **com uma rota residual** | `EpgEditSession` (`app/features/epg/EpgEditSession.kt:1-23`) com `Owner(profile, source, epoch)` e `accepts(owner)`; as três rotas da auditoria revalidam antes de gravar. **Mas o caminho de *revisão* de auto-match não tem owner** | **Ver R7-08** |
| **A15-05** — pausa/retoma deixa temporizadores do ExoPlayer por rearmar | 🔧 **JÁ CORRIGIDO** | `core/player-core/…/LiveResumeWatchdogs.kt:5-29` + `LivePreviewEngine.kt:2872-2875,2896-2901`; testes em `LiveResumeWatchdogsTest.kt:16-57` | Nenhuma |
| **A15-06** — gravações consultam a opção de leitor externo de Filmes | 🔧 **JÁ CORRIGIDO** | `SettingsRepository.kt:1497-1503` — `externalPlayerFor(MediaType.LIVE) → externalPlayerLive`; `RecordingsViewModel.kt:165,257` | Nenhuma |
| **A15-07** — fábrica HTTP da pré-visualização numa geração retirada | 🔧 **JÁ CORRIGIDO** | `HeroPreviewEngine.kt:337-340` — a fábrica é **recriada em cada `play()`**; `PlaybackHttpCalls.kt:36-58` cancela gerações antigas | Nenhuma |
| **A15-08** — pesquisa no guia não é limpa no regresso após Home | 🔧 **JÁ CORRIGIDO** | `OwnTVShell.kt:267-283` (distingue `ON_STOP` real de sair da secção) + `EpgViewModel.kt:983-988` | Nenhuma |
| **A15-09** — pesquisa EPG ignora nomes personalizados | 🔧 **JÁ CORRIGIDO** | `EpgViewModel.kt:1057-1071` + `GuideRenamedSearch.kt:7-27` (lotes de 400 por origem, sem carregar o catálogo); a elegibilidade é aplicada **depois** da união | Nenhuma |
| **A15-11** — o guia volta a calcular os mesmos intervalos | 🔧 **JÁ CORRIGIDO (código)**, ⚠️ **aceitação de desempenho pendente** | `GuideProgrammeRow.kt:6-12` (modelo imutável), pesquisa binária em `at()`/`adjacentTime()`, `GuideRowCache` com invalidação por identidade | **Nenhuma no código** — falta a medição na box (`IMPLEMENTACAO_LOTE_5_EPG_2026-10-15.md:41-46`) |
| **A15-12** — aviso de alarmes exactos desatualizado | 🔧 **JÁ CORRIGIDO** | `RecordingsViewModel.kt:69-75` + `RecordingSettingsScreen.kt:73-82` (re-lê no `ON_RESUME`) | Nenhuma |
| **A15-14** — actualização automática por pin não acompanha o Core | 🔧 **JÁ CORRIGIDO** (resíduo cosmético) | `automerge-core-bump.yml` é um stub; `force_core_source` e o repositório GitHub Packages desapareceram do repo | Opcional: alinhar o nome do ramo em `pr-checklist.yml:19` |
| **A15-16** — a fixture de desempenho exige preparação incompatível | 🔧 **JÁ CORRIGIDO** | `IptvJourney.kt:62-67` — o guard **falha com instrução explícita** quando "apenas diferidos" está activo | Nenhuma |
| **A15-03** — associações EPG podem reintroduzir categorias ocultas | 🔧 **JÁ CORRIGIDO** | `EpgViewModel.kt:1088-1111` — a elegibilidade final é reaplicada **depois** de `applyEpgMatches`; `GuideChannelEligibility.kt:6-10` | Nenhuma |
| **A15-01** — um valor legítimo das legendas torna o backup irrestaurável | 🔧 **JÁ CORRIGIDO** | `SettingsRepository.kt:1686-1688` (grava o `-1`), `:2897` (intervalo `-1..100` validado), `SettingsBackupValidation.kt:22-24`, chamado em `BackupManager.kt:785` **antes** de qualquer escrita | Nenhuma — mas ver a síntese em §21.3 |
| **BUG-P1-01** ≡ A15-01 · **BUG-P1-02** ≡ SEC-01 · **BUG-P1-04** ≡ A15-04 · **BUG-P2-02** ≡ A15-06 · **BUG-P2-04** ≡ A15-08 · **BUG-P2-05** ≡ A15-09 · **BUG-P2-06** ≡ A15-10 · **BUG-P3-01** ≡ A15-11 · **BUG-P3-02** ≡ A15-12 | ✅ **VALIDOS** (o conteúdo de fundo) | Re-lidos; nenhum corrigido | Ver §15.7 — as soluções mantêm-se |
| **"bloqueio A–B–A"** | ⚠️ **RECLASSIFICADO** | É do **provider** — o projecto atribui-o correctamente em 4 documentos | **Ver R7-01 e R7-04** |

> **Nota de namespace:** `REVISAO_ADICIONAL_TV_2026-10-08.md` usa "A9…A15" para itens
> **diferentes**; o prefixo `A15-` pertence só à auditoria especializada de 15/10. E a auditoria
> A15 preserva a análise **original** (pré-correção) — o próprio projecto implementou os lotes 1–5
> em 15/10 (`IMPLEMENTACAO_LOTES_1_2_AUDITORIA_2026-10-15.md` etc.). Por isso **14 dos 16 itens
> estão corrigidos** — e o que resta é o que a tabela acima isola.

### 21.2-bis Os três residuais da série A15

---

### R7-06 — 🟠 O restauro **selectivo** ainda pode cruzar identidades nas secções CUSTOMIZE e SOURCES

**Severidade: ALTA · Confiança: alta · Verificado**

**Localização:** Core `backup/BackupManager.kt:1038-1045`, `:1016-1018`, `:1682-1691`

O Core tem uma classe nova que implementa correctamente a regra *"só identidades resolvidas podem
receber dados"*:

```kotlin
// core/…/backup/BackupRestoreIdentity.kt:7-22
/** Device-local counters are not identity. Only mappings resolved by this restore may attach data. */
internal class BackupRestoreIdentity(private val profiles: Map<Long, Long>, private val sources: Map<Long, Long>) {
    var skipped = 0; private set
    fun profile(old: Long): Long? = profiles[old].also { if (it == null) skipped++ }
    …
}
```

E as rotas SETTINGS usam-na (`BackupManager.kt:1145-1154, 1164-1166, 1177, 1190-1197, 1220, 1235,
1245`; `PlaybackDatabaseBackup.kt:42`; `SettingsRepository.kt:3061, 3069-3079`).

**Mas `remapKeys` — que o audit não tocou — continua a fazer *pass-through* de chaves numéricas sem
mapping:**

```kotlin
// core/…/backup/BackupManager.kt:1682-1691
/** Rewrites an id-keyed JSON map ({"<fileId>": …}) to device ids; unmapped keys pass through. */
internal fun remapKeys(o: JSONObject, idMap: Map<Long, Long>): JSONObject {
    …
    val mapped = k.toLongOrNull()?.let { idMap[it] }?.toString() ?: k     // ← o "?: k" é o problema
```

E é usado em rotas de perfil/fonte que **não** passaram pelo `BackupRestoreIdentity`:

| Local | Efeito residual |
|---|---|
| `BackupManager.kt:1038` — `val pid = profileIdMap[filePid] ?: filePid` | As `customizations` de um perfil **não resolvido por nome** são fundidas sob o id numérico local que **coincide** |
| `BackupManager.kt:1044-1045` — `remapKeys(it, profileIdMap)` → `importHomeConfigs` / `importHideNewCategories` | `SettingsRepository.kt:3086/3112` só verificam `pid !in existingProfileIds` — que é **precisamente** o caso de colisão de ids |
| `BackupManager.kt:1016-1018` — `remapKeys(it, sourceIdMap)` → `importPlaylistAutoRefresh` / `importEpgAutoRefresh` / `importEpgUseLogos` | `SettingsRepository.kt:3181-3190` aceita o id que *existe* no dispositivo → a escolha de auto-refresh do ficheiro **aterra na fonte local errada** |
| `BackupManager.kt:1802-1807` — `remapEnginePinKey` | Já não é usado nas rotas guardadas, mas continua disponível como armadilha |

**Condição de reprodução:** restauro **só de SETTINGS** (ou de CUSTOMIZE) entre duas instalações em que
o perfil/lista do ficheiro **não** corresponde por nome mas o **id numérico coincide** com um
perfil/fonte local. `profileIdMap` só contém correspondências por nome (`BackupManager.kt:862-887`) —
nesse caso o `?: filePid` aplica o bloco ao perfil homónimo numérico, **não à pessoa**.

**Solução**

```kotlin
// core/…/backup/BackupManager.kt — bloco CUSTOMIZE
root.optJSONObject("customizations")?.let { o ->
    val cust = HashMap<String, String>()
    o.keys().forEach { k ->
        if (!k.startsWith("cust_")) return@forEach
        val body = k.removePrefix("cust_")
        val filePid = body.substringBefore('_').toLongOrNull() ?: return@forEach
        val pid = restoreIdentity.profile(filePid) ?: return@forEach      // ← deixa de passar pelo id cru
        cust["cust_${pid}_${body.substringAfter('_')}"] = remapCustomizationValue(o.getString(k), sourceIdMap)
    }
    customize.mergeAll(cust)
}
root.optJSONObject("homeConfigs")?.let { settings.importHomeConfigs(restoreIdentity.profileMap(it), existingProfileIds) }
root.optJSONObject("hideNewCategories")?.let { settings.importHideNewCategories(restoreIdentity.profileMap(it), existingProfileIds) }
```

E, para as fontes, um `remapResolvedKeys` que **descarta** ids sem mapping nas três linhas `:1016-1018`.

> ⚠️ **Guarda de compatibilidade obrigatória** (a auditoria acertou nisto): a resolução estrita só
> é segura quando o ficheiro **traz identidades**. Se `root` não tiver `"profiles"` (backup antigo
> ou ficheiro gerado sem perfis), `profileIdMap` está vazio e a correcção **apagaria as
> personalizações do utilizador**. Condicionar a regra estrita a `root.has("profiles")` e manter o
> comportamento legado quando o ficheiro não traz perfis — como já acontece em `remapUserData`
> (`BackupManager.kt:1086`).

---

### R7-07 — 🟡 A redacção de segredos tem **duas políticas**: a do `HttpClient` e a do `StalkerClient`

**Severidade: MÉDIA-BAIXA · Confiança: alta · Verificado**

**Localização:** Core `network/HttpClient.kt:137-143` (política nova) · `stalker/StalkerClient.kt:712`
(política antiga)

A política nova é correcta e está em todo o lado:
```kotlin
// core/…/network/HttpClient.kt:137-143
internal fun diagnosticOrigin(url: String): String = runCatching {
    val parsed = java.net.URI(url)
    val scheme = parsed.scheme?.lowercase(java.util.Locale.ROOT)
    val host = parsed.host?.takeIf { it.isNotBlank() }
    if (scheme !in setOf("http", "https") || host == null) "invalid-url"
    else "$scheme://$host${if (parsed.port >= 0) ":${parsed.port}" else ""}"
}.getOrDefault("invalid-url")
```

Mas o caminho Stalker ainda usa a sua:
```kotlin
// core/…/stalker/StalkerClient.kt:712
val safeUrl = url.substringBefore('?')     // mantém o path; vai também para httpFailure(code, safeUrl)
```

**Hoje não é fuga** (as credenciais Stalker vivem na query), mas são **duas políticas** — e o
`safeUrl` entra na mensagem de exceção entregue ao chamador.

**Solução:** exportar o mesmo helper de origem e usá-lo também no Stalker:
```kotlin
// core/…/network/HttpClient.kt — tornar diagnosticOrigin public
// core/…/stalker/StalkerClient.kt:712
val safeUrl = HttpClient.diagnosticOrigin(url)
```
(mantendo o `substringBefore('?')` apenas para o *download* do guia, onde o path do portal é
útil). Fecha a classe inteira de uma vez.

---

### R7-08 — 🟡 A rota de **revisão** do auto-match EPG não tem owner → pode aplicar a correspondência no perfil errado

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** app `features/epg/EpgViewModel.kt:873-906` (`autoMatchEpg`), `:936-943`
(`acceptSuggestion`), `:951-959` (`acceptAllSuggestions`)

O `EpgEditSession` foi criado e as **três** rotas da auditoria revalidam antes de gravar:
```kotlin
// app/…/features/epg/EpgViewModel.kt:730-745
internal fun setEpgShift(channel: ChannelEntity, minutes: Int?, owner: EpgEditSession.Owner) {
    viewModelScope.launch {
        if (owner.source != channel.sourceId || !acceptsEpgEdit(owner)) return@launch
        …
```

**Mas o caminho de revisão não tem owner:**
```kotlin
// app/…/features/epg/EpgViewModel.kt:936-943
fun acceptSuggestion(s: EpgMatchSuggestion) {
    viewModelScope.launch {
        val pid = settings.activeProfileId.first()          // ← o perfil NO MOMENTO DA EXECUÇÃO
        customize.setEpgMatch(pid, MediaType.LIVE, CustomizeKeys.channel(s.channel), s.epgChannelId)
```

Igual em `:951-959` e em `autoMatchEpg` (`:878`). **Um diálogo de revisão que sobreviva a uma troca
de perfil aplica a correspondência no perfil novo** — exactamente o que o `EpgEditSession` existe
para impedir.

**Solução:** capturar um owner no momento em que a sugestão entra em `_review` (em `autoMatchEpg` /
`autoMatchOne`) e passá-lo a `acceptSuggestion` / `acceptAllSuggestions`, revalidando com
`acceptsEpgEdit(owner)` antes de `customize.setEpgMatch`.

**Nota de higiene (do mesmo ficheiro):** `acceptsEpgEdit` compara `pid` com uma **segunda** leitura
do mesmo flow (`EpgViewModel.kt:342-344`, `LiveViewModel.kt:561-563`) — **tautologia** na prática; a
protecção real vem do `epoch` do `EpgEditSession`. Vale a pena reescrever a linha para não simular
uma revalidação que não existe.

---

### 21.2-ter A série **B** do plano mestre (verificada)

| ID | Veredicto | Onde está hoje | Acção |
|---|---|---|---|
| **B1** — play/pause/recuperação sem intenção consistente | 🔧 **JÁ CORRIGIDO** | `PlaybackEngine.kt:181-182` (os adaptadores substituem ambos), `OwnTVPlayer.kt:1373-1375` (ligado a um `PlaybackIntent` real com `desired` + `version`), `PlaybackSession.kt:195-199, 244-259` (comandos explícitos e idempotentes, `ResumeTicket` com revisão), `LivePreviewEngine.kt:1176-1177, 1207` (o `reprepare` impõe a **intenção**, não `playWhenReady=true`), `LiveTuneController.kt:108` (`HandoverIntent` distingue o *stop interno* do Pause do utilizador), `:497-505, 594, 839-846` | **Ver R7-03** — **dois** defaults a remover, não um |
| **B2** — a recuperação de áudio renova o próprio orçamento | 🔧 **JÁ CORRIGIDO** | `LivePreviewEngine.kt:1018-1037` — `audioRescueConsumed`; o reset só acontece numa **nova sintonia** (`:1780-1783`), e a reconstrução interna põe `rebuildingCurrentTune = true` (`:2003, 2022`) → o orçamento não é renovado | Nenhuma |
| **B3** — estéreo não garante dois canais PCM | 🔧 **JÁ CORRIGIDO** (aceitação A/V pendente) | `AudioOutputPolicy.kt:111-147` + `StereoPcmAudioProcessor.kt:27-41, 85-100` — mistura PCM explícita, matriz posicional, **falha explicitamente** em layouts não suportados; o offload é desligado para que nada contorne o misturador | Nenhuma no código; falta prova em A/V |
| **B4** — volume reduzido por interrupção no motor errado | 🔧 **JÁ CORRIGIDO (mecanismo removido)** | O `PlaybackSession` **não toca no volume** (`:23-26` — *"Internal gain is fixed at unity"*); `AUDIOFOCUS_LOSS*` → `pauseForInterruption()`. O `ducking` interno foi **eliminado por decisão do utilizador** | ⚠️ **A "correção" do documento NÃO deve ser aplicada** (§21.4) |
| **B5** — foco recusado fica memorizado como pedido pendente | 🔧 **JÁ CORRIGIDO** | `PlaybackSession.kt:50` — `FocusState { NONE, GRANTED, DELAYED, TRANSIENT, FAILED }`; `:348-350` — uma recusa só é retentada quando a **revisão** muda; `:332` — delayed gain aceite; `:362` — `FAILED` limpa o `resumeTicket` e pausa | ✅ Válido; ver nota de UX abaixo |
| **B6** — erro de formato consumido sem iniciar alternativa | 🔧 **JÁ CORRIGIDO** | `LivePreviewEngine.kt:2616-2620` — `retryAlternateFormat(): Boolean`; o call site (`:1630`) só sai se agendou; `isFormatFailure` (`:2591-2604`) exclui 401/402/403/429/503/509 | Nenhuma |
| **B7** | 🔧 **JÁ CORRIGIDO** | Ver **R7-02** | Corrigir o KDoc + teste |
| **B8** — User-Agent aprendido antes de comprovar recuperação | 🔧 **JÁ CORRIGIDO** | `LivePreviewEngine.kt:2655-2660` — `pendingUaLearning` (pendente); `:1336-1343` — só é consolidada após **reprodução confirmada**; âmbito por URL (`LiveStreamQuirks.kt:451-465`) e expiração de 30 min (`:460`); o 503 não ensina | Nenhuma |
| **B9** — `Retry-After` só considerado depois dos retries internos | 🔧 **JÁ CORRIGIDO (escopo Exo)** | `ProviderRetryGuard.kt:18-29` (o 429 nunca chega ao retry do Media3), `ProviderBackoffGuard.kt:10-19` (guarda antes do DNS/connect), `core/network/HttpRetryAfter.kt:14-42` (segundos **e** HTTP-date, RFC 1123/850/asctime), `LivePreviewEngine.kt:3889-3892` + `:2463` (deadline monotónico), `LiveStreamQuirks.kt:317-326` (a janela é por host + conta) | ⚠️ **Ver R7-10** — e o mpv **nunca** honrou o header (está escrito no código) |
| **B10** — o cliente HTTP cancela tarde operações bloqueadas | 🔧 **JÁ CORRIGIDO** | `core/network/HttpClient.kt:53-85` — `cancelBlockingCallWithCoroutine` cobre **conexão e leitura**; o `finally` só cancela o *watcher* (as ligações ficam reutilizáveis) | Nenhuma |
| **B13** — gerir perfis pode reduzir a protecção de outro perfil | 🔧 **JÁ CORRIGIDO** | `core/profile/ProfileOperationAuthorization.kt:6-33` — grant efémero **ligado ao alvo e à acção**, com CAS; `core/profile/ProfileManager.kt:69-78, 120-125` — escrita por CAS; `app/…/ManageProfilesScreen.kt:84-106, 219-242` — desafio-resposta com o PIN actual | Nenhuma |
| **B20** — `rearmAll` pode armar uma linha já cancelada | ⚠️ **PARCIALMENTE VALE** | `core/recording/RecordingScheduler.kt:62-88` tira um snapshot e arma sem coordenação com o `actionMutex`; o cancelamento revalida (`core/recording/RecordingManager.kt:647-667`) | **Ver R7-12** |
| **B21** — buffers do timeshift | ❌ **FALSO POSITIVO** | O ficheiro citado (`core/timeshift/TimeshiftEngine.kt`) **não existe**; a implementação é `LocalTimeshiftSession.kt`. E o mecanismo descrito ("buffer 64 KiB em loop TS a cada frame") **não existe**: a cópia é `ByteArray(128 * 1024)` **por segmento** (`HlsSegmentTransfer.kt:15`), a playlist é limitada a 1 MiB (`LocalTimeshiftSession.kt:129-144`), e o server tem 4 sockets (`LocalTimeshiftServer.kt:26`) | **Nenhuma** — e o "fix" do plano **não deve ser aplicado** (§21.4) |
| **B22** — índice `targetItemId`/`mediaType` | ❌ **FALSO POSITIVO** | `core/database/entity/TvProviderEntities.kt:23-26` — `targetItemId` continua **payload, sem índice**; e **nenhuma query o usa** (`core/database/dao/TvProviderProgramDao.kt:19-32`). O índice foi correctamente **não** criado | **Nenhuma** — e o índice **não deve** ser criado sem antes uma query + `EXPLAIN QUERY PLAN` |
| **B23** — `awaitMpvOutcome` | 🔧 **JÁ CORRIGIDO** | `player-core/…/player/LiveEngines.kt:191-209` — o timeout só devolve `null` quando `playbackRequested == true` (`:206`); uma **pausa deliberada** não aborta e o tempo pausado é **creditado** ao orçamento (`LiveWaitBudget.kt:48-53`); não existe `PlaybackOutcome.Aborted` | Nenhuma — e a solução do plano **não deve** ser reintroduzida (§21.4) |

---

### R7-09 — 🟡 O B1 tem **dois** defaults a remover, não um

**Severidade: MÉDIA (latente) · Confiança: alta · Verificado**

A minha leitura da Ronda 7 encontrou o default da **propriedade**; o agente da série B encontrou
**outro** default, no **método** — e é o mais perigoso dos dois:

```kotlin
// core/…/player/PlaybackEngine.kt:82-86
fun setPlaybackRequested(requested: Boolean) {
    if (playbackRequested.value != requested) togglePlayPause()      // ← TOGGLE
}
fun play() = setPlaybackRequested(true)
fun pause() = setPlaybackRequested(false)
```

**Isto é exactamente o defeito do B1:** *"um comando externo Play pode alternar um Exo que já tem
`playWhenReady=true` para Pause"*. O `play()` chama `setPlaybackRequested(true)`; se o motor já
está a reproduzir, `playbackRequested.value == true` → **não faz nada**; mas num motor que reporte
`false` durante o BUFFERING, `play()` faz `togglePlayPause()` → **pausa**.

**Solução** (completa o R7-03): remover **ambos** os defaults, e tornar o método abstracto:

```kotlin
interface PlaybackEngine {
    // … sem default: cada motor declara a SUA intenção
    val playbackRequested: StateFlow<Boolean>
    val playbackRequestRevision: StateFlow<Long>

    /**
     * Set the playback intent. MUST be idempotent: play() on a playing engine is a no-op,
     * pause() on a paused engine is a no-op. A toggle-based default would turn "Play" into
     * "Pause" whenever the engine reports false while BUFFERING — the exact B1 defect.
     */
    fun setPlaybackRequested(requested: Boolean)
    fun play() = setPlaybackRequested(true)
    fun pause() = setPlaybackRequested(false)
}
```

**Custo:** 2 implementadores a actualizar (`LivePreviewEngine.kt:2877`, `MpvPlaybackEngine.kt:212`).

---

### R7-10 — 🟡 Com `Retry-After` > 60 s, o utilizador fica com um ecrã de erro **e** pedidos recusados

**Severidade: MÉDIA · Confiança: média-alta · Verificado por leitura**

O B9 foi corrigido e bem: o 429 nunca chega ao retry do Media3, existe um guard que bloqueia
pedidos durante a janela, e o deadline é monotónico e por host+conta. **Mas há uma interacção que
ninguém dos dois documentos viu:**

```kotlin
// core/…/player-core/…/PlayerDiagnostics.kt:269  →  httpStatusOf lê o código do guard
// core/…/player-core/…/ProviderBackoffGuard.kt:16 →  lança ProviderHttpRefusal(429, Retry-After: <restante>)
// core/…/player-core/…/LivePreviewEngine.kt:2430-2444
if (secs > MAX_RETRY_AFTER_SECS) {              // MAX_RETRY_AFTER_SECS = 60  (:3837)
    trace("provider_wait_terminal", "seconds=$secs limit=$MAX_RETRY_AFTER_SECS")
    …
    tune.gaveUp = true
    _error.value = PlaybackFailure.Channel
    _errorInfo.value = ErrorInfo(…, "HTTP $status Retry-After ${secs}s")
    return true // End this attempt; never shorten the server deadline.
}
```

**O problema:** com um `Retry-After: 120`,
1. o `ProviderBackoffGuard` **guarda a janela** (`LiveStreamQuirks.kt:288-294`), bloqueando qualquer
   novo pedido para esse host+conta durante 120 s;
2. mas o `maybeBackOffForProvider` **termina a tentativa com ecrã de erro** — o utilizador **não vê a
   contagem**;
3. e o botão **Retry** dentro da janela é recusado **imediatamente** pelo guard
   (`ProviderHttpRefusal` 429) → **novo erro imediato, sem contagem**.

Ou seja: o utilizador vê "falhou", toca Retry, e falha outra vez — e o ecrã de contagem que o HUD
já sabe mostrar (`providerBackOff` + `player_provider_retry_after`) **nunca aparece**.

**Solução** — duas opções, ambas pequenas:

```kotlin
// (a) o guard reentra na contagem existente em vez de propagar o erro
// core/…/player-core/…/ProviderBackoffGuard.kt
if (wait != null) {
    // Already waiting for this host+account: re-surface the SAME countdown instead of
    // turning the user's Retry into a fresh error.
    throw ProviderHttpRefusal(wait.httpCode, mapOf("Retry-After" to listOf(wait.secondsLeft.toString())), "")
}
```
e, no `LivePreviewEngine`, distinguir "recusa dentro de uma janela já activa" de "recusa nova":

```kotlin
// LivePreviewEngine.kt:2430-2444
if (secs > MAX_RETRY_AFTER_SECS) {
    // A long wait is not a failure — surface the countdown and let the engine re-ask.
    if (providerWaitKey(currentUrl) == key) { providerWaitUntilMs = …; return false }
    …
}
```

**(b) Subir `MAX_RETRY_AFTER_SECS`** para a ordem de minutos e mostrar sempre a contagem real — o
`ProviderBackOff` já suporta `secondsLeft`.

**Nota de âmbito (do agente da série B, honesta):** o motor **mpv nunca pôde honrar o header** —
está escrito no código (`OwnTVPlayer.kt:343-345`: *"mpv surfaces only the status line, so a
`Retry-After` header cannot be honoured here"*). O guard só existe no cliente HTTP do Exo. **O B9,
tal como escrito, nunca foi verdade para o mpv** — o documento devia tê-lo dito.

---

### R7-11 — 🟢 A lição DASH não tem invalidação, ao contrário da HLS

**Severidade: BAIXA · Confiança: alta · Verificado**

O Core tem a **assimetria**:
- HLS: `rememberHlsRedirect(url)` (`:161-166`) e **`forgetHlsRedirect(url)`** (`:168-173`) — e o
  `mixedHlsHosts` marca o host como "misto" para que a lição deixe de valer.
- DASH: `rememberDashRedirect(url)` (`:187`) e `isKnownDashHost(url)` (`:189`) — **sem
  `forgetDashRedirect`**.

**Cenário:** um painel que muda de DASH para HLS em URLs **sem extensão** continua a mandar essas
URLs ao parser DASH (o `knownDashHost` nunca expira), e aí `retryAlternateFormat()` devolve `false`
(URL sem extensão → sem irmão `.ts`/`.m3u8`) → **o canal termina em erro**.

**Solução** — dar ao DASH a mesma assimetria do HLS:
```kotlin
// core/…/player/LiveStreamQuirks.kt
@Synchronized fun forgetDashRedirect(url: String, nowMs: Long = System.nanoTime() / 1_000_000L) {
    dashRedirectHosts.remove(hostKey(url))
}
```
e chamá-la quando a resposta for HLS (`LivePreviewEngine.kt:3251-3257` já detecta `isHlsResponse`).

---

### R7-12 — 🟡 `rearmAll` pode armar um alarme para uma gravação já cancelada (impacto limitado)

**Severidade: BAIXA-MÉDIA · Confiança: alta · Verificado**

**O que subsiste:**
```kotlin
// core/…/recording/RecordingScheduler.kt:62-88
suspend fun rearmAll() {
    val now = clock()
    recordingDao.scheduled().forEach { recording ->
        if (RecordingSchedule.hasBeenMissed(recording, now)) { … cancel(recording.id) }
        else { RecordingSchedule.wakeAtFor(recording, now)?.let { arm(recording.id, it) } }
    }
}
```
Tira um snapshot e arma/cancela por linha, **sem coordenação** com o `actionMutex` do
`RecordingManager` — a janela "snapshot obsoleto → arm para uma linha já cancelada" continua
aberta.

**O que o council anterior (bem).anticipou — e o que melhorou:** o caminho de cancelamento **revalida**
o estado:
```kotlin
// core/…/recording/RecordingManager.kt:647-667
fun cancel(recording: RecordingEntity) {
    scope.launch { actionMutex.withLock {
        val row = recordingDao.getById(recording.id) ?: return@withLock
        if (!recordingActionMatches(recording, row, setOf(RecordingStatus.SCHEDULED))) return@withLock
        …
```

**Impacto real hoje: um wake-up inútil, não uma gravação errada.** O motor re-consulta a BD
(`RecordingDao.kt:68-72` — `dueAt(now)` só devolve `SCHEDULED`/`RECORDING`), e o alarme espúrio
acorda o worker, que não encontra nada e sai.

**Veredicto: ⚠️ PARCIALMENTE VALE** — a corrida estrutural subsiste, o impacto descrito no plano
("captura errada") **não se materializa**.

**Solução** — não é preciso coordenar tudo: basta **revalidar a linha imediatamente antes de armar**:
```kotlin
// core/…/recording/RecordingScheduler.kt — rearmAll()
val current = recordingDao.getById(recording.id) ?: return@forEach
if (current.status != RecordingStatus.SCHEDULED) { cancel(recording.id); return@forEach }
arm(recording.id, RecordingSchedule.wakeAtFor(current, now) ?: return@forEach)
```
Isto fecha a janela *stale-rearm* sem introduzir um "dono do estado de alarmes" novo. A alternativa
mais estrutural: mover `arm`/`cancel` para dentro do `actionMutex` do `RecordingManager` e correr
`rearmAll` sob o mesmo lock.

---

### 21.3 💡 A síntese que liga a Ronda 6 e a Ronda 7

O Core tem uma **camada de validação de backup exemplar** — `BackupValueValidator` +
`SettingsBackupValidation` + `BackupImportValidation`, que validam **tipos, enums e intervalos** de
todos os valores importados, **antes** de escrever seja o que for na base de dados ou nas
preferências.

**E o SEC-12 está exactamente uma linha acima dessa camada.**

`BackupContainer.kt:238` chama `BackupCrypto.deriveKey(pass, header)` — que lê o `iterations` do
ficheiro — e **isso corre ANTES** de `BackupImportValidation` (que só é chamado em
`BackupManager.kt:802`). O validador que recusa um `audio_delay_ms` de 10¹⁵ nunca chega a ver o
`iterations` de 2¹⁵⁹, porque o Core já está a fazer o PBKDF2 quando o validador é chamado.

**Consequência prática:** a correcção do SEC-12 **pertence à mesma arquitectura** — o `deriveKey`
deve validar o cabeçalho com a mesma disciplina com que o resto do backup é validado. **Não é um
sistema novo; é um buraco numa camada que já existe e já é boa.**

### 21.4 A actualizacao de prioridades que a Ronda 7 forca

| Mudança | Porquê |
|---|---|
| ⬆️ **CAT-6 (veredicto de saúde por canal) deixa de estar bloqueado** | O O4 está corrigido nas duas metades, e o `RebufferFlapDetector` (`player-core/…/RebufferFlapDetector.kt:20-38`) mede a oscilação sustentada READY/BUFFERING com janela de 6 s, ≥12 transições e ≥3 s de progresso, reiniciando correctamente em seek ou descontinuidade. **A métrica é fiável.** O que falta é a persistência (uma tabela nova no Core) e a紅界面 que diz "não sei" |
| ⬆️ **R7-01 (mitigação do A–B–A) sobe a prioridade de produto** | É o único achado que ataca o **bloqueio nº1 do próprio projecto**, e o Incremento 1 (esperar só depois de uma recusa real) é **pequeno** e está no Core |
| ⬆️ **R7-02 (KDoc do B7 + teste) sobe** | É o que **destrava o CAT-1 (Auto-Tune Memory)** — a maior ideia de produto do projecto. Custa uma linha de comentário e um teste |
| ⬇️ **B1 desce de prioridade** | Está corrigido e com testes. Só fica a defesa estrutural do R7-03 (uma tarde) |

---


### 21.5 ⚠️ As correcções propostas que **NÃO devem** ser implementadas tal como estão

> **Esta é a parte mais importante da Ronda 7.** Sete dos achados do próprio projecto têm uma
> "solução" documentada que, aplicada tal e qual, **introduziria um bug novo** ou **reverteria uma
> decisão deliberada do utilizador**. Sem esta secção, quem as implementasse a partir do documento
> causaria dano.

| Documento | "Solução" proposta | Porquê **não** a aplicar |
|---|---|---|
| **PLANO MESTRE B4** | *"associar o estado de atenuação ao motor proprietário e tratar a restauração antes da troca"* | O **ducking interno foi removido por decisão do utilizador** (`PLANO_MESTRE` linha 5: *"volume interno sempre a 100 %"*). O `PlaybackSession` hoje **não toca no volume** (`:23-26`). Reintroduzir atenuação num volume que o sistema controla a 100 % seria uma regressão directa. **O que resta de B4 é higiene:** `PlaybackSession.kt:34-40` (`enum FocusPolicy { DUCK, PAUSE }`) é **código morto** — ambos os ramos pausam — e o `di/PlayerModule.kt:71` ainda passa `focusPolicy = FocusPolicy.DUCK`, o que hoje é enganador. **Fix:** remover o parâmetro/enum, ou documentar "legacy, ignorado" |
| **REVISÃO B21** | reutilização / pool de buffers do timeshift | Um buffer partilhado entre sockets **corromperia respostas concorrentes** — o `LocalTimeshiftServer` tem 4 sockets em paralelo (`LocalTimeshiftServer.kt:26`). A alocação **por pedido** (64 KiB) é a escolha correcta |
| **REVISÃO B22** | criar o índice `targetItemId` + `mediaType` | **Nenhuma query o usa** (`TvProviderProgramDao.kt:19-32`). O índice adicionaria escrita, migração e tamanho de BD **sem qualquer consulta** que o beneficie. O council anterior já tinha recusado este item — **e a decisão foi cumprida** |
| **REVISÃO B23** | *"abortar reprodução sempre que `playbackRequested == false`"* | **Destruiria a pausa deliberada** — que é o que o `LiveWaitBudget` + `LivePauseAccounting` (`LiveWaitBudget.kt:48-53`) tratam como "pausa ≠ stop" — e dispararia uma alternativa/erro **sem acção do utilizador**. O `awaitMpvOutcome` actual (`LiveEngines.kt:191-209`) está correcto |
| **REVISÃO B13** (fix de charset do `Pin.kt`) | *"Tornar `pin.toByteArray()` explícito"* | `Kotlin pin.toByteArray()` **já é UTF-8**. É P3 de estilo, não há bug. E a decisão de política (grant por acção) **já foi implementada** — não mexer no hashing |
| **PLANO MESTRE B1** | *"comandos explícitos e idempotentes Play/Pause em todos os motores"* | **Correcto e já feito.** Mas o que **falta** é remover os **dois** defaults da interface (ver **R7-03** e **R7-09**) — é a única parte que continua por fazer |
| **PLANO MESTRE B3** | *"não alterar o modo Surround nem aplicar mistura global"* | **Respeitado** — o `StereoPcmAudioProcessor` só entra com `forceStereo=true` (`AudioOutputPolicy.kt:116-118`). Não mexer |

**Acção: nenhuma para estas sete.** São **decisões**, não correcções.

---

# ═══════════════════════════════════════════════════════════════════════
# PARTE IV — RONDA 8 (19/10/2026)
# CORRECÇÕES AO QUE JÁ FOI ESCRITO + achados novos + veredicto final
# ═══════════════════════════════════════════════════════════════════════

> **Esta parte corrige a Parte II.** Um conselho de reavaliação independente re-verificou os 10
> achados novos da Ronda 6 e uma amostra de 50 dos 59 itens priorizados, **procurando
> precisamente as correcções que estivessem erradas**. Encontrou **duas correcções que teriam
> quebrado a app** e várias com premissas inexactas. **Lê esta parte antes de implementares a
> Parte II.**

---

## 22. ⚠️ CORRECÇÕES À PARTE II — as "soluções" que estavam erradas

### 22.1 🔴 SEC-12 — a minha solução **partiria todos os backups cifrados**

**O que eu escrevi (errado):**
```kotlin
if (crypto.optString("scheme", SCHEME) != SCHEME) return null
```

**Porque está errado:** `BackupCrypto.SCHEME = "field-aes-gcm"` (`BackupCrypto.kt:29`) — mas o
**contentor `.own` não usa esse esquema**: `BackupContainer.kt:176` escreve
`cryptoBlock(salt).put("scheme", CONTAINER_SCHEME)` e `CONTAINER_SCHEME = "container-aes-gcm"`
(`BackupContainer.kt:415`). Com o meu check, `deriveKey` devolveria `null` →
`BackupContainer.kt:238` → `error("Corrupt backup header")` → **todo o import de um `.own` cifrado
passaria a falhar**. E o `.own` cifrado é **o que a app escreve sempre**
(`BackupScreen.kt:285,332` + `BackupViewModel.kt:149`), e é o que o sync local usa.

**Correção correcta** — validar o `kdf` e as iterações, e deixar o **esquema** ser parâmetro do
chamador:

```kotlin
// core/…/backup/BackupCrypto.kt
const val MIN_ITERATIONS = 100_000
const val MAX_ITERATIONS = 2_000_000

fun deriveKey(passphrase: String, crypto: JSONObject?, allowedSchemes: Set<String>): SecretKey? {
    crypto ?: return null
    if (crypto.optString("kdf", KDF) != KDF) return null
    if (crypto.optString("scheme", "") !in allowedSchemes) return null
    val iters = crypto.optInt("iterations", ITERATIONS)
    if (iters !in MIN_ITERATIONS..MAX_ITERATIONS) return null      // cobre também iters <= 0
    val salt = runCatching { unb64(crypto.getString("salt")) }.getOrNull() ?: return null
    if (salt.size < 8) return null
    return deriveKey(passphrase.toCharArray(), salt, iters)
}

// contentor (.own)                    → allowedSchemes = setOf(CONTAINER_SCHEME)
// campos selados (formato legado .json) → allowedSchemes = setOf(BackupCrypto.SCHEME)
```

**Três correcções adicionais ao diagnóstico:**

1. **Não há ANR.** O import corre em `withContext(Dispatchers.IO)` (`BackupManager.kt:781`). O que
   existe é uma **tarefa de fundo a queimar CPU durante horas** — bateria, não travamento de UI.
   Severidade **ALTA**, mas por outra razão.
2. **`"iterations": 0` é um segundo bug.** `PBEKeySpec` exige `iterationCount > 0` →
   `IllegalArgumentException` não tratada dentro do `deriveKey`. Hoje é absorvida pelo `runCatching`
   do import (`BackupManager.kt:782`) e chega ao utilizador como "falhou" genérico. O fix tem de
   cobrir `iters <= 0`, não só o topo da faixa.
3. **A janela `[100 000, 2 000 000]` é segura** — verificados os **10 snapshots** do Core em
   `.gradle/core-snapshot/`: todos usam `ITERATIONS = 210_000`, nenhum histórico abaixo de 100 k.

**Teste de regressão (obrigatório):** cobrir **os dois** vectores (`2147483647` e `0`), e **afirmar
que um contentor `.own` cifrado continua a importar** — teste de não-regressão, não só de rejeição.

---

### 22.2 ⚠️ SEC-13 — a premissa estava errada; o risco real é outro

O que eu escrevi: *"se amanhã se subir `ITERATIONS`, a leitura continua a usar os 210 000 que estão
no cabeçalho dos ficheiros antigos → **todos os backups existentes deixam de abrir**"*.

**Isto é falso.** A escrita e o cabeçalho **já saem da mesma constante** — `cryptoBlock(salt)`
escreve `put("iterations", ITERATIONS)` (`BackupCrypto.kt:49`) e a chave deriva de
`BackupCrypto.ITERATIONS`. Não podem divergir. E cada ficheiro carrega **o seu** `iterations`, que a
leitura usa → **um backup antigo continua a derivar com 210 000 e continua a abrir**. Este
mecanismo (guardar o KDF no cabeçalho) é o **correcto**, e é o que permite subir as iterações sem
partir nada.

**O risco real que eu não formulei:** o **esquema** do contentor (`container-aes-gcm`) **nunca é
verificado na leitura** (`BackupContainer.kt:238` ignora-o). Um ficheiro de um formato futuro — ou de
outro esquema — seria desencriptado com o esquema errado, e o erro apareceria como
*"palavra-passe errada"* (`BackupContainer.kt:243-244` converte qualquer `AEADBadTagException` em
`WrongPasswordException`).

**Correção correcta** — não mudar o que é escrito; **verificar o que é lido**, com aceitação do
esquema actual e do anterior:

```kotlin
// leitura — aceitar o esquema actual e o anterior, nunca um desconhecido
val scheme = header.optString("scheme", "")
if (scheme !in setOf(CONTAINER_SCHEME_V1, CONTAINER_SCHEME)) return null
```

⚠️ E **nunca** subir `CONTAINER_SCHEME` sem implementar a aceitação do anterior: isso invalidaria
retroactivamente tudo o que já foi gravado — exactamente o dano que eu queria evitar.

---

### 22.3 ⚠️ SEC-14 — a magnitude está inflada ~2,5× e a minha solução partia o upload do browser

**O que eu escrevi:** *"pico de ~150–190 MB de heap"* e *"fazer o servidor aceitar corpo binário cru"*.

**Correcções**

1. **O corpo de `/backup` está limitado a 16 MB** (`CompanionHttpProtocol.kt:17,50` —
   `UPLOAD_BODY_LIMIT = 16 * 1024 * 1024`). O maior backup transferível é ~12 MB (base64 cresce 33 %),
   **não 30 MB**. Pico real: 12 + 16 + 16 + 16 ≈ **60 MB**, não 150–190.
2. **Um backup de 30 MB não sincroniza** — o servidor responde **413**. Portanto "OOM quase
   garantido" é exagerado para 60 MB num heap de 128–256 MB.
3. **O receptor tem o mesmo problema** — e é onde está o decode: `CompanionHttpServer.kt:335` lê o
   corpo como `String` e `CompanionController.kt:193-203` volta a fazer `Base64.decode` →
   `ByteArray` → `file.writeBytes`. A minha solução só tocava o cliente.

**A minha solução também partia a funcionalidade:**
- o endpoint `/backup` é o **mesmo** que a página de browser usa (`CompanionHtml` →
  `CompanionHttpServer.kt:331-340`);
- o outro dispositivo pode ter uma versão **mais antiga** da app, que só sabe falar `data:` URI.

**Correção correcta — manter o formato de fio, fazer streaming do base64 em blocos de 3 bytes:**

```kotlin
// LocalSyncClient — streaming do base64, protocolo inalterado
val len = file.length()
val b64Len = 4 * ((len + 2) / 3)
val prefix = "data:application/octet-stream;base64,"
conn.setFixedLengthStreamingMode(prefix.length + b64Len)
conn.outputStream.use { out ->
    out.write(prefix.toByteArray())
    file.inputStream().use { input ->
        val buf = ByteArray(57 * 1024)            // múltiplo de 3 → sem padding intermédio
        var carry = 0
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            val chunk = if (n % 3 == 0) buf.copyOf(n) else buf.copyOf(n - (n % 3))
            out.write(Base64.encodeToString(chunk, Base64.NO_WRAP).toByteArray())
            carry = n % 3
            if (carry > 0) { /* guardar o resto para o próximo passo */ }
        }
    }
}
```

Pico de heap ≈ **64 KB + o resto**. O protocolo fica inalterado → continua a interoperar com o
browser e com versões antigas. **O corpo binário cru só deve ser introduzido como uma segunda rota**
(`POST /backup-raw`, com detecção por `Content-Type`), **nunca** substituindo a actual.

---

### 22.4 ⚠️ SEC-15 — o vector real é o `username`, não a "URL Xtream"

O que eu escrevi: *"uma URL Xtream é da forma `http://host/live/UTILIZADOR/PALAVRA_PASSE/123.ts`"*.

**Errado:** essa é a URL de **stream de um canal** (`XtreamClient.kt:444`), **não** a `url` da source,
que é a **base do painel**. O que sai em claro sem passphrase é:

```kotlin
// core/…/backup/BackupManager.kt:1503-1507
put("url", s.url)                                    // ← sempre em claro
put("username", s.username ?: JSONObject.NULL)        // ← sempre em claro  ← o vector real
val pw = s.password?.takeIf { it.isNotEmpty() }
put("password", if (pw != null && seal != null) seal(pw) else JSONObject.NULL)
```

**Correção correcta** — o `username` é metade da credencial:

```kotlin
put("username", if (s.username != null && seal != null) seal(s.username) else JSONObject.NULL)
```

E recusar o export sem cifra quando alguma source tem credenciais embebidas na URL. **Já existe
precedente no Core:** as `manualTsChannels` **já são omitidas sem cifra**
(`BackupManager.kt:185-186`).

Severidade real: **BAIXA-MÉDIA** (risco de API do Core), não MÉDIA.

---

### 22.5 ⚠️ DATA-01 — o meu fix referenciava um `manifest` que não existe

O export não tem um `manifest`: a raiz é um **`JSONObject` plano** (`root`, `BackupManager.kt:105-164`).
A chave nova tem de ser um **top-level de `root`** — e chaves desconhecidas são ignoradas por
leitores antigos (é a convenção do próprio formato, cf. `put("version", 25)`).

Além disso:
- `exportSubtitles` (`:312-380`) é uma `private suspend` **separada** — a lista de falhas tem de ser
  **propagada** até `export`, não colectada localmente.
- a exportação **não é uniforme**: `BackupManager.kt:238`
  (`db.playbackQuirkDao().getAllOnce()`) **não** está embrulhado → uma falha aí **aborta** o export.
  A inconsistência é real e reforça o achado.
- a alternativa **mais honesta e mais barata**: **fazer o export falhar** quando uma secção não pode
  ser lida, em vez de a marcar. O utilizador percebe e repete; um ficheiro silenciosamente
  incompleto não dá nenhum sinal.

---

### 22.6 ⚠️ PERF-02 — o meu `animateFloatAsState` não é um *drop-in*

O snippet que escrevi não reproduz a semântica actual (auto-avanço via `onNavigate` no fim do
intervalo, reset em navegação manual, pausa quando os botões de acção têm o foco); a flag `active`
**não existe**; e `animateFloatAsState` com `targetValue` fixo reinicia a animação quando o target
muda.

**Correção correcta — dois elementos, um ciclo:**
```kotlin
// 1) a barra: valor lido, sem escrita por frame no estado de Compose
val progress by animateFloatAsState(
    targetValue = if (activeIndex == myIndex) 1f else 0f,
    animationSpec = tween(durationMillis = intervalMs.toInt(), easing = LinearEasing),
    label = "heroProgress",
)
// 2) o avanço: um LaunchedEffect separado, com key = activeIndex
LaunchedEffect(activeIndex, manuallyPaused, actionButtonsFocused, items.size) {
    if (manuallyPaused || actionButtonsFocused || items.size < 2) return@LaunchedEffect
    delay(intervalMs)
    onNavigate((activeIndex + 1) % items.size)
}
```

---

### 22.7 ⚠️ UX-11 — o meu fix lutava com um caminho emparelhado que não vi

`OwnTVShell.kt:1181` não é um acidente — está emparelhado com `:1205`:
```kotlin
redirectEntryFocus = !(restoreFocus && selectedSection == MainSection.LIVE_TV)
```
Quando o shell regressa do player em Live TV, o `LiveScreen` recebe `restoreFocus = true` e faz o
**seu** restauro (`LiveScreen.kt:482-487`). Um `focusRestorer` incondicional do shell, activo nesse
momento, **rouba o foco ao alvo correcto**.

**Correção correcta — manter a exclusão, mas torná-la coerente:** o `focusRestorer` deve estar activo
**apenas enquanto o restauro explícito não está em curso** (uma flag de curta duração ligada no
`onRestored`), **não** condicionada à *secção*. Nunca as duas coisas activas ao mesmo tempo.

---

### 22.8 ⚠️ UX-02 — `liveOnly = false` é uma **decisão de produto**, não um bugfix

`liveOnly = true` **não é um bug**: é a política de uma box de TV. `HomeFeed.kt:197-200` força
`hidden = hidden + {TRENDING, CONTINUE_MOVIES, CONTINUE_SERIES}` e `heroIncludeMovies/Series = false`;
e o KDoc de `HomeFeedReader.load` (`:160-170`) documenta que o caminho completo são **15 leituras**,
das quais `buildContinuationItems` demora **150–400 ms numa TCL**. Passar a `false` traz de volta as
rails de VOD e **muda o que o hero é**.

**Correção correcta — não mexer em `liveOnly`.** Activar **apenas** a rail de Trending (um
parâmetro novo e explícito, ex. `includeTrending = true`, que já tem o tratamento de `hidden`
correcto), mantendo o resto da política intacto; e **corrigir o loop do hero primeiro** (§22.6),
medindo depois. Se a decisão for o `liveOnly = false`, então tem de ser um produto com teste A/B de
retenção, **não um item de uma lista de bugs**.

---

### 22.9 ⚠️ DAT-01 — a paginação incremental muda a semântica da lista de canais

`GroupedChannelPagingSource` é, **por desenho**, um *snapshot* ("built once off the UI thread",
`:13`) e expõe `onReady(snapshot)` (`:19, :51`) — de que dependem a `zapList` e a navegação de canal.
Transformá-la em paginação incremental altera **o que "o canal seguinte" significa**, e a `zapList`
foi construída sobre o snapshot completo.

**Correção correcta — as duas metades separadas:**
1. **`unregisterInvalidatedCallback`** no `invalidate()`/`onCleared` — uma linha, sem mudança de
   comportamento, resolve os callbacks fantasma (é o **PLY-08**);
2. a **paginação incremental**, como item de **arquitectura**, com medição de memória e a garantia
   de que a `zapList` continua a ver o conjunto completo (talvez via um índice por revisão de
   conteúdo).

---

### 22.10 ⚠️ PLY-07 — decidir no *key release* pode **nunca mais abrir o canal**

Hoy o `zap()` dispara em **KeyDown** (`PlayerHud.kt:485-486`) e a rajada abre "a primeira e a última"
selecção (`LiveViewModel.kt:1736-1738`). Se a decisão passar a depender do *key-up*: (a) o canal só
abre quando o utilizador **larga** o botão — centenas de ms de atraso; (b) **se o key-up se perder**
(a app perde o foco, um overlay abre, o comando é engolido), a pressão fica **sem efeito** e o canal
**nunca abre** — a pior falha possível num comando de TV.

**Correção correcta — manter a decisão no KeyDown e acrescentar um detector de rajada:** no *key-up*,
se a rajada durar > X ms, **cancelar** o tune pendente (é a ideia original do `pendingZapTuneJob`);
abrir a primeira **só se** a rajada for curta. Assim o "primeira e última" passa a "só a última"
**sem** depender do key-up para o caso comum.

---

### 22.11 ⚠️ DWN-01 — exige uma **release do Core** (não é só uma app)

A tabela `downloads` é do Core, e o esquema está na **v51** (`OwnTVDatabase.kt:119`). Acrescentar
`failure_reason` exige **migração 51→52 no Core** → release do Core → re-snapshot → bump do SHA-256
em `core-source-lock.json` → só depois a app pode mostrar o motivo.

**Correção correcta — dois passos:**
- **(a) agrupar todas as mudanças de Core num único bump**: SEC-12 + SEC-13 + DATA-01 + SEC-15 + a
  coluna do DWN-01 + SEC-04, com o lock actualizado **uma vez**;
- **(b) enquanto isso**, entregar a **parte visível sem migração**: o `Refused.reason` pode ser
  transportado na **notificação** (`Core core/…/download/DownloadNotifications.kt`) e no toast — o
  utilizador vê o motivo **sem tocar no esquema**.

---

### 22.12 ⚠️ SEC-04 — o meu fix trancava os utilizadores fora dos seus perfis

O formato actual é `salt:sha256(salt+pin)` (`Pin.kt:13-24`, e `verify` faz `parts.size != 2 → false`).
Se `hash()` passar a escrever PBKDF2 e `verify()` não souber ler o formato antigo, **todo o PIN
existente deixa de validar** — o utilizador do perfil Kids fica trancado, **sem recuperação**.

**Correção correcta — formato versionado com leitura de fallback:**
```kotlin
// verify: tentar primeiro o formato novo; se falhar, o legado
fun verify(pin: String, stored: String?): Boolean {
    if (stored.isNullOrBlank()) return true
    if (stored.startsWith("pbkdf2$")) return verifyPbkdf2(pin, stored)
    return verifyLegacySha256(pin, stored)      // salt:sha256(salt+pin)
}
```
E um caminho de recuperação (o Core já tem `ProfileOperationAuthorization` — a confirmação de acção
do adulto). Custo de CPU: 200 k iterações ≈ 100–200 ms numa box fraca — aceitável para um *gate* de
PIN, desde que **não** seja aplicado ao `verify` de cada frame.

---

### 22.13 ⚠️ F7 — a minha lista de remoção **quebraria a compilação**

| Dependência | O que eu escrevi | **Realidade** |
|---|---|---|
| `androidx.room.ktx` | "morta" | ❌ **É USADA** — `MainActivity.kt:9` (`import androidx.room.useWriterConnection`) e `:111`. **Remover quebra o build** |
| `androidx.navigation-compose` | "morta" | ✅ morta, mas **não está declarada** no módulo app → remover é um no-op |
| `androidx.room-compiler` | "morta" | ✅ idem — não está declarada |
| `libs.plugins.ksp` | "morta" | ✅ idem — não está declarada |
| `compose.ui.tooling.preview` | "morta" | ✅ morta (sem `@Preview` em `app/src`) — **remover** |
| `material.icons.core` | "morta" | ✅ morta — **remover** |
| `room.paging` | "morta" | ✅ morta (sem `RoomPagingSource`) — **remover** |
| `media3-exoplayer` | "morta" | ⚠️ morta *na app*, mas é o que **alinha a versão do media3** com a do Core. **Decidir com o Core**, não remover às cegas |

**Correção correcta:** remover **apenas** `compose.ui.tooling.preview`, `material.icons.core` e
`room.paging`. **Manter** `room-ktx`. Para `media3-exoplayer`: alinhar explicitamente com a versão do
Core (ou usar `constraints`) em vez de remover.

---

### 22.14 ⚠️ UX-08/PLY-04 — a correcção é correcta, mas **há um terceiro sítio**

O conselho confirmou que a correcção (§8, `requestFocusRetrying`) está certa — e que **o mesmo bug
existe no EPG**, que o meu relatório não mencionava: `EpgScreen.kt:255-256` →
`if (!restoreFocus || state.loading || state.channels.isEmpty()) return@LaunchedEffect` (o
`onRestored()` só é chamado em `:264`, dentro do `repeat(3)` de focus).

E o **R4-B19** (a flag `restoreFocus` presa com lista vazia) tem a **localização errada** no meu
§15.4: citei `DownloadsScreen.kt:149-153`; o correcto é **`:164-173`, com o salto em `:165`**.

**Correção correcta** — aplicar o padrão que o **Home** já usa (`HomeScreen.kt:246-248`):
```kotlin
if (shown.isEmpty()) {
    if (restoreFocus) onRestored()
    return@LaunchedEffect
}
```
nos **três** sítios: `DownloadsScreen.kt:165`, `EpgScreen.kt:255` e o de `RecordingsScreen`. O
`onRestored` é **idempotente** (só limpa duas flags) → sem efeito colateral.

---

### 22.15 ⚠️ CAN-01 — os denominadores estavam errados, e o fix não cobre o caso dominante

A minha contagem ("87 com guarda, de 90") estava errada. A contagem correcta:
```
clauses catch totais .............................. 116  (58 genéricos + 58 CancellationException)
genéricos (Exception/Throwable) .................  58
  com clause irmão CE que relança ...............  50
  SEM guarda ..................................   8
     → falsos positivos (código não-suspenção) ..   5
     → reais ....................................   3   ← exactamente os 3 do relatório
```

**E o buraco maior não é o `catch` — é o `runCatching`**, que apanha `Throwable` e engole
`CancellationException` em muito mais sítios dentro de código suspender (ex. `BackupManager.kt:801`,
`LiveViewModel.kt:1525`, `SettingsScreen.kt:1525`). Uniformizar só os `catch` deixa o problema
dominante intacto.

**Correção correcta — um helper, e uma migração progressiva:**
```kotlin
// core/…/util/Cancellable.kt
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c                       // ← a cancelamento propaga
} catch (t: Throwable) {
    Result.failure(t)
}
```
E a regra: **`runCatching` nunca dentro de código suspender que precise de respeitar a
cancelamento** — ou usar o helper, ou reescrever como `try/catch` explícito.

---

### 22.16 QA-01 — a contagem estava errada (3, não 19)

A app tem **3** classes instrumentadas (`app/src/androidTest/…/ChannelDaoBoundedWindowTest.kt`,
`LocalizedContentContextTest.kt`, `PluralResourceBoundaryTest.kt`). Os **16** ficheiros restantes
são do Core (`core/src/androidTest`). A **conclusão** (nenhum `androidTest` corre no CI) mantém-se.

### 22.17 REL-05 — a tabela de regressão do meu fix tinha uma linha inconsistente

A minha tabela dizia `v1.2.0 → 10200`; com a fórmula nova (`MA*1000000 + MI*10000 + PA*100 + BUILD`)
isso dá **1020000**. A tabela correcta:

| Tag | `versionCode` | Nota |
|---|---|---|
| `v1.2.0` | `1 020 000` | 3 componentes → `BUILD = 0` |
| `v105.1.0.5` | `105 010 005` | 4 componentes |
| `v105.1.0.6` | `105 010 006` | **monótono** ✅ |
| `v6.0.0` | `6 000 000` | |

E o `versionCode` local (`build.gradle.kts:50`) tem de ser **derivado da mesma fórmula** — hoje é
`10510005`, que é `MA*100000 + MI*10000 + PA*1000 + 0*100 + 5` (um esquema **diferente**). **Uma
fórmula só, nos dois sítios.**

---

## 23. Achados novos da Ronda 8 (player-core, Room e séries C/O)

### 23.1 🔴 PLY-13 — Um **Pause** do utilizador durante um handover de engine **encrava a sintonia para sempre**

**Severidade: ALTA · Confiança: alta · Verificado no Core**

**Localização:** Core `player/LiveTuneController.kt:497-524` (Exo→mpv), `:532-605` (mpv→Exo),
`:839-846` (`playbackRequested()`), `:713-714` (o alarme de "dar por vencido")

**O cenário.** Exo → mpv (`startOnMpv`):
```kotlin
541:            val destinationRevision = engines.mpvPlaybackRevision
...
594:            while (engines.mpvPlaybackRevision != destinationRevision && !engines.mpvPlaybackRequested) {
595:                if (!isCurrent(channel)) return
596:                delay(100L)
597:            }
598:            if (!isCurrent(channel)) return
599:            engines.mpvPlay(url, request(channel, source))
```

A `destinationRevision` é capturada na **linha 541**, mas o loop corre na **linha 594** — depois de
`exoStopAndAwaitDrain()` (561), `awaitExoRelease()` (569) e, para Stalker, `resolveForTune()`
(577-581, até 30 s de rede). Se o utilizador prime **Pause** nessa janela,
`OwnTVPlayer.setPlaybackRequested(false)` → `PlaybackIntent.request(false)` → **incrementa a revisão**
(`PlaybackIntent.kt:19-23`). Agora `mpvPlaybackRevision != destinationRevision` **e**
`!mpvPlaybackRequested` — a condição do `while` é **permanentemente verdadeira** → **a linha 599
nunca é alcançada**.

**Porque nunca recupera.** `playbackRequested()` (839-846) é o accessor ciente do handover:
```kotlin
840:        val intent = handoverIntent
841:        if (intent != null && intent.owner == generation &&
842:            ((engines.mpvPlaybackRevision != intent.mpvRevision && !engines.mpvPlaybackRequested) || …)) return false
```
O Pause do utilizador é **indistinguível** da paragem interna que o próprio handover executa — pelo
que o guard reporta "não pedido". E é **esse mesmo `false`** que o alarme de "dar por vencido"
testa:
```kotlin
713:                val left = (ladder.deadlineAt() ?: return@launch) - host.nowMs()
714:                if (left <= 0 && playbackRequested()) break
```
O `break` **nunca** acontece → o alarme não abandona nem quebra, e
`pauses.observe(host.nowMs(), playbackRequested())` (707) continua a **creditar** o intervalo
pausado, empurrando `ladder.postponeDeadline` para sempre. A escada **nunca avança**, nenhum erro
é mostrado.

**Resultado visível: um ecrã preto permanente com um spinner, sem mensagem de erro e sem o timeout
"Give up after"**, até o utilizador voltar a premir Play (o que repõe `playbackRequested = true` e
liberta o loop) ou fazer zapping.

**Solução (Core)** — distinguir "o motor ainda não foi mandado tocar" de "o utilizador pausou":

1. Dar ao `HandoverIntent` uma **flag explícita `pausedByUser`**, em vez de a inferir de um delta de
   revisão (a revisão é ambígua: `setPlaybackRequested` incrementa-a em *qualquer* comando deliberado,
   e `stop()` também).
2. Tornar os dois loops de espera **limitados e orientados pelo resultado** (`withTimeoutOrNull`),
   e depois **honrar a intenção do utilizador**: arrancar o motor de destino e deixá-lo **pausado**.
3. Tornar o `break` do alarme **independente do guard de handover** — usar
   `engineAttemptStarted && isCurrent(channel)` em vez de `playbackRequested()` — para que um handover
   encravado termine sempre num ecrã de erro.

---

### 23.2 🔴 PLY-14 — Qualquer falha que não seja HTTP nem de transporte é tratada como **falha de formato**

**Severidade: ALTA · Confiança: alta · Verificado no Core**

**Localização:** Core `player/LiveTuneController.kt:780`, `player/LiveLadder.kt:193-209, 120-126`,
`player/LiveExoWatchdog.kt:69, 73, 96, 105, 119`

A escada recebe **um bit**:
```kotlin
780:            val next = ladder.advance(failureWasAboutFormat = !isRequestRefusal(reason) && !ConnectionFailure.isTransient(reason), nowMs = nowMs) ?: run {
```
`isRequestRefusal` (886-887) só reconhece um **status HTTP** dentro do texto da razão;
`ConnectionFailure.isTransient` reconhece uma lista fixa de strings de transporte. **Todo o resto é
"falha de formato"** — incluindo razões do watchdog que **não são** sobre o contentor:

| Razão do watchdog | Linha | É realmente sobre |
|---|---|---|
| `"no video frame rendered (audio plays, no picture)"` | `:69` | o **descodificador de vídeo** |
| `"provider refuses ExoPlayer's signed segment URLs"` | `:73` | um **esquema de assinatura de URLs** do painel |
| `"no decodable audio track"` | `:119` | o **codec de áudio** |
| `"ExoPlayer opening budget exhausted (…)"` | `:96` | **tempo**, não formato |
| `"ExoPlayer error before first frame: …"` | `:105` | qualquer coisa, incluindo um codec |

**Duas consequências:**

**(a) A escada sobe o degrau errado.** Com `failureWasAboutFormat = true`, `LiveLadder.advance`
escolhe `order.firstOrNull { it !in spent }` (linha 197) — **o irmão `.ts` do mesmo motor**. Para
`"no decodable audio track"` num canal Xtream, a escada percorre `EXO_HLS → EXO_TS → MPV_HLS`:
**dois degraus e dois orçamentos de sintonia gastos**, porque o `.ts` tem **o mesmo codec de áudio
indecodável**.

**(b) Uma lição errada, de sessão, por motor, é registada.** Ao subir para um degrau `.ts`:
```kotlin
204:        if (failureWasAboutFormat && streamUrl != null && !next.isHls) {
205:            if (next.onMpv && Rung.MPV_HLS in spent) LiveStreamQuirks.rememberNoHlsVariantMpv(streamUrl)
206:            if (!next.onMpv && Rung.EXO_HLS in spent) LiveStreamQuirks.rememberNoHlsVariant(streamUrl)
207:        }
```
E essa lição é consultada em **todas as sintonias seguintes** daquele canal (`:120-126`). Um canal
cujo único problema é um **codec de áudio** fica **permanemente (pela sessão) sem o seu `.m3u8` no
ExoPlayer** — o `.m3u8` nunca foi o problema, e o `.ts` tem a mesma faixa de áudio.

**Solução (Core)** — substituir o booleano por uma classificação fechada, usada nas **duas**
decisões:

```kotlin
internal enum class LiveFailureKind { FORMAT, CODEC, TRANSPORT, REQUEST_REFUSAL, TIME }
internal fun classifyLiveFailure(reason: String): LiveFailureKind = when {
    reason.contains("no decodable audio") || reason.contains("no video frame rendered") -> LiveFailureKind.CODEC
    reason.contains("opening budget exhausted") -> LiveFailureKind.TIME
    reason.contains("signed segment URLs") -> LiveFailureKind.REQUEST_REFUSAL
    else -> LiveFailureKind.FORMAT
}
```

- `FORMAT` → o irmão `.ts` do mesmo motor pode ser tentado; a lição **pode** ser registada.
- `CODEC` → **saltar** o irmão `.ts` (um contentor diferente não corrige um codec) e ir **directo
  ao outro motor**; **não registar lição**.
- `REQUEST_REFUSAL` / `TRANSPORT` / `TIME` → outro motor, sem lição.

Passar o enum em vez do booleano em `LiveTuneController.advance` (780), e registar a lição de
formato **só** para `FORMAT`. Casos de teste novos em `LiveLadderTest.kt` e `LiveTuneControllerTest.kt`.

---

### 23.3 🔴 PLY-15 — `awaitMpvOutcome` **nunca regressa** enquanto a reprodução não é pedida

**Severidade: ALTA · Confiança: alta · Verificado no Core**

**Localização:** Core `player/LiveEngines.kt:191-209`, usado em `LiveTuneController.kt:652-670`

```kotlin
191:    override suspend fun awaitMpvOutcome(timeoutMs: Long): MpvOutcome? {
192:        val budget = LiveWaitBudget(android.os.SystemClock.elapsedRealtime(), timeoutMs)
193:        while (true) {
194:            if (mpv.playbackRequested.value) {
195:                mpv.error.value?.let { … return MpvOutcome(false, text) }
196:                if (mpv.livePlaybackReady.value) return MpvOutcome(true, null)
197:            }
198:            val left = budget.remaining(…, mpv.playbackRequested.value)
199:            if (left <= 0L && mpv.playbackRequested.value) return null
200:            kotlinx.coroutines.delay(100L)
201:        }
202:    }
```

Duas decisões dependem de `mpv.playbackRequested`:
1. **A verificação de erro (195) está dentro do `if`.** Se o utilizador pausar (ou o mpv for
   internamente pausado) enquanto o degrau mpv abre, **a falha nunca é observada**.
2. **O timeout (199) também exige `playbackRequested`.** E `LiveWaitBudget.remaining`
   (`LiveWaitBudget.kt:48-53`) **soma** `pauses.observe(nowMs, requested)` ao `allowanceMs` — com
   `requested == false`, o crédito cresce exactamente à velocidade de `nowMs`, pelo que
   `remaining = startedMs + baseMs + allowanceMs - nowMs` **nunca chega a 0**. O orçamento de 35 s
   (`MPV_OPEN_TIMEOUT_MS`, `LiveTuneController.kt:1047`) **nunca expira** enquanto o motor não é
   pedido.

**Consequências.** O `while (true) { … delay(100L) }` torna-se um **poller permanente a 10 Hz no main
thread** durante todo o tempo em que o utilizador deixa o canal em pausa (só termina quando a
sintonia é superada, via `cancelLadderJobs()`). E como `awaitMpvOutcome` nunca regressa, o
`mpvOutcomeJob` (652-670) nunca chama `advance(...)` — **a escada nunca sobe do degrau mpv**, a falha
nunca é registada, e o alarme "dar por vencido" é a única saída (e está também derrotado, ver
PLY-13).

**Solução (Core)** — desacoplar as três decisões:

```kotlin
override suspend fun awaitMpvOutcome(timeoutMs: Long): MpvOutcome? {
    val budget = LiveWaitBudget(android.os.SystemClock.elapsedRealtime(), timeoutMs)
    while (true) {
        mpv.error.value?.let { … return MpvOutcome(false, it.toString()) }   // incondicional
        if (mpv.playbackRequested.value && mpv.livePlaybackReady.value) return MpvOutcome(true, null)
        val left = budget.remaining(…, /* requested = */ true)               // não creditar pausas aqui
        if (left <= 0L) return null
        kotlinx.coroutines.delay(100L)
    }
}
```

E no `watchMpv` (`:655-668`), tratar um `null` enquanto o utilizador está em pausa como
*"o utilizador pausou; parar de observar"* — **sem** chamar `advance()`, para não avançar a escada em
nome do utilizador.

---

### 23.4 🔴 PLY-16 — O botão de play/pause do HUD é **rotulado** pelo `isPlaying` mas **age** pelo `playbackRequested`

**Severidade: ALTA (visível pelo utilizador) · Confiança: alta · Verificado**

**Localização:** app `player/PlayerHud.kt:219`, `player/PlayerHudChrome.kt:293`,
`player/MiniPlayer.kt:64,135`, `player/AudioNowPlayingBar.kt:106`

A interface separa deliberadamente os dois:
```kotlin
// core/…/player/PlaybackEngine.kt:14-15
/** Requested play/pause, independently of loading or temporary suppression. */
val playbackRequested: StateFlow<Boolean> get() = isPlaying
```

**Mas nenhuma UI a usa.** O HUD:
```kotlin
// PlayerHud.kt:219
val isPlaying by player.isPlaying.collectAsStateWithLifecycle()
```
```kotlin
// PlayerHudChrome.kt:293
CircleButton(if (isPlaying) OwnTVIcon.PAUSE else OwnTVIcon.PLAY, size = 64, primary = true, …) { player.togglePlayPause() }
```
E o mini-player, igual (`MiniPlayer.kt:64,135`).

`isPlaying` é o estado de **renderização**; `togglePlayPause()` alterna o `playbackRequested`
(`OwnTVPlayer.kt:3543`, `LivePreviewEngine.kt:2870`). Num canal em directo que está a **bufferizar** —
o estado normal durante vários segundos em **cada** sintonia, e em **cada** re-buffer num painel
instável — `isPlaying == false` enquanto `playbackRequested == true`. O botão mostra portanto um
**triângulo de play** e **premi-lo pausa o stream**.

**Solução (app)** — rotular pela **intenção**, não pelo estado de renderização:

```kotlin
// PlayerHud.kt:219
val requested by player.playbackRequested.collectAsStateWithLifecycle()
// PlayerHudChrome.kt:293
CircleButton(if (requested) OwnTVIcon.PAUSE else OwnTVIcon.PLAY, …)
// MiniPlayer.kt:64,135
val requested by player.playbackRequested.collectAsStateWithLifecycle()
MiniBtn(if (requested) OwnTVIcon.PAUSE else OwnTVIcon.PLAY, …)
```

Manter `isPlaying` para o que significa mesmo "está uma imagem no ecrã" (ex. o spinner de buffering).
`FrameRateController.kt:442` já usa o certo — é o precedente no repo.

---

### 23.5 🔴 DB-01 — Um trigger do FTS4 perdido é **permanente**, e nada o detecta

**Severidade: CRÍTICA (funcional) · Confiança: alta · Verificado + reproduzido em SQLite**

**Localização:** Core `database/BulkInsertHelper.kt:107-113` (drop), `:120-145` (restore), `:133`;
`database/OwnTVDatabase.kt:1592-1596` (sonda de drift), `:1607-1620`; `di/DatabaseModule.kt:87`

O helper de bulk-insert **apaga** o trigger de sincronização do Room:
```kotlin
// BulkInsertHelper.kt:107-113
val triggerName = ftsTable?.let { "room_fts_content_sync_${it}_AFTER_INSERT" }
if (triggerName != null) {
    triggerSql = connection.usePrepared("SELECT sql FROM sqlite_master WHERE type='trigger' AND name='$triggerName'", …)
    if (triggerSql != null) connection.execSQL("DROP TRIGGER IF EXISTS `$triggerName`")
}
```
e **restaura-o só a partir do snapshot tirado no drop**:
```kotlin
// BulkInsertHelper.kt:131-133
if (state.ftsTable != null) connection.execSQL("INSERT INTO `${state.ftsTable}`(`${state.ftsTable}`) VALUES('rebuild')")
if (state.triggerSql != null) connection.execSQL(state.triggerSql)
```

**A consequência em cadeia:**
1. Se o processo morre entre o `DROP TRIGGER` e o restore (uma importação interrompida — exactamente
   o que o KDoc do helper em `:48-50` e o *heal* em `DatabaseModule.kt:39-47` existem para
   sobreviver), **o trigger fica apagado**.
2. No **bulk-insert seguinte**, o capture em `:109-111` devolve `null` (o trigger já não existe) →
   `state.triggerSql == null` → o restore em `:133` é **saltado**. O trigger passa a ser
   **irrecuperável pelo helper**.
3. `healSchemaIfDrifted` (`OwnTVDatabase.kt:1607-1620`) só sondam `EXPECTED_SCHEMA_OBJECTS`
   (`:1592-1596`) = os **nomes das 4 tabelas FTS** + os **nomes dos 30 índices não-únicos**. Um
   **trigger** não está em nenhuma das listas → a sonda diz "limpo" e o *heal* nunca corre. O
   `onPostMigrate` do Room recria os 16 triggers — mas **só durante uma migração**.

**[Reproduzido em SQLite 3.50]**
```
after insert #1, MATCH 'BBC' -> [(1,)]
after insert #2 (trigger dropped), MATCH 'CNN' -> []      <-- o canal novo NÃO é pesquisável
```

**Impacto:** a **pesquisa global enquanto se escreve** — `SearchReader.kt:77`
(`channelDao.searchListDetailedFts`), `:90`, `:100` — **deixa silenciosamente de encontrar** qualquer
canal/mfilme/série adicionado ou renomeado depois da perda. É exactamente o sintoma *"procuro um
canal e ele não aparece"*. Pior: os triggers `BEFORE_DELETE`/`BEFORE_UPDATE` **continuam lá**, pelo que
um `UPDATE`/`DELETE` posterior **remove** a entrada do FTS sem nada para a repor.

**Solução (sem mudança de esquema)**

1. Adicionar um mapa canónico de triggers ao lado de `EXPECTED_FTS_TABLES`
   (`OwnTVDatabase.kt:1531-1536`): `EXPECTED_FTS_TRIGGERS`, com os **16** `CREATE TRIGGER IF NOT
   EXISTS room_fts_content_sync_<t>_{BEFORE_UPDATE,BEFORE_DELETE,AFTER_UPDATE,AFTER_INSERT}`
   **copiados verbatim do `_Impl` gerado** (usam `docid`, não `rowid` — não os reescrever à mão).
2. Em `healSchema` (`:1547-1561`), criar cada trigger (idempotente) e fazer
   `INSERT INTO <fts>(<fts>) VALUES('rebuild')` quando algum tiver sido (re)criado.
3. Adicionar os 16 nomes de trigger a `EXPECTED_SCHEMA_OBJECTS` (`:1592-1596`).
4. Em `BulkInsertHelper.restoreIndexes` (`:120-145`), restaurar o trigger a partir do **DDL canónico**
   (passo 1), não do `state.triggerSql` — assim um trigger ausente no momento do drop é **criado**.

### 23.6 🔴 DB-02 — `healSchema` cria uma tabela FTS **sem** os triggers de sincronização

**Severidade: ALTA · Confiança: alta · Verificado + reproduzido**

**Localização:** Core `database/OwnTVDatabase.kt:1548-1557`

```kotlin
EXPECTED_FTS_TABLES.forEach { (name, createSql) ->
    val exists = db.prepare("SELECT 1 FROM sqlite_master WHERE type='table' AND name='$name'").use { it.step() }
    if (!exists) {
        db.execSQL(createSql)
        db.execSQL("INSERT INTO `$name`(`$name`) VALUES('rebuild')")
    }
}
```

O DDL é *"Room's exact generated DDL"* — e é, para a **tabela virtual**. Mas o esquema FTS do Room
compreende **quatro triggers por tabela**, e o `healSchema` **não emite nenhum**.

E `healSchema` tem **dois** call sites com semânticas diferentes:
- dentro de uma **migração** → o `onPostMigrate` do Room recria os 16 triggers → a lacuna fecha;
- de `healSchemaIfDrifted`, ao **abrir a app** (`DatabaseModule.kt:87`) → **não corre nenhum
  `onPostMigrate`** → a tabela FTS curada fica **sem triggers**.

**[Reproduzido]**
```
right after heal, MATCH 'BBC' -> [(1,)]
after a later INSERT,     MATCH 'CNN' -> []      <-- índice FTS morto
triggers present: 0
```

**Solução:** a mesma do DB-01, passo 1-2 — ensinar o `healSchema` a criar os 16 triggers, e reconstruir
o índice sempre que tiver de criar um. O DDL é idêntico nos dois call sites → é uma só mudança.

### 23.7 🟠 DB-03 — A sonda de drift **não vê triggers nem índices únicos**

**Severidade: ALTA · Confiança: alta**

**Localização:** Core `database/OwnTVDatabase.kt:1592-1596`

```kotlin
private val EXPECTED_SCHEMA_OBJECTS: List<String> by lazy {
    EXPECTED_FTS_TABLES.keys.toList() +
        EXPECTED_NON_UNIQUE_INDEXES.values.flatten()
            .map { it.substringAfter("IF NOT EXISTS `").substringBefore('`') }
}
```

Faltam os **16 triggers** e os **8 índices únicos** (`index_channels_sourceId_remoteId`,
`index_movies_sourceId_remoteId`, `index_series_sourceId_remoteId`,
`index_episodes_seriesId_remoteId`, `index_categories_sourceId_mediaType_remoteId`,
`index_favorites_profileId_mediaType_itemId`, `index_epg_programmes_natural_key`,
`index_epg_channels_sourceId_epgChannelId`).

**Isto é *porque* que o DB-01/DB-02 são irrecuperáveis.** E um índice **único** apagado é pior que
um não-único: o REPLACE-upsert do sync passa a criar **linhas duplicadas** em silêncio.

**Solução:** estender `EXPECTED_SCHEMA_OBJECTS` com os nomes dos triggers e dos índices únicos, e
fazer o `healSchema` recriá-los (`CREATE UNIQUE INDEX IF NOT EXISTS …`). Nota: o KDoc em `:1468-1470`
diz que os índices únicos estão "deliberadamente ausente" porque *"re-creating a unique index on
unexpected data could itself fail"* — isso vale para o **restore depois de drop**, não para o
**heal**, onde o correcto é tentar a criação dentro de uma transacção e deixar falhar em voz alta.

### 23.8 🟠 DB-04 — Três grelhas "scanam a tabela inteira" em cada página **[medido]**

**Severidade: ALTA · Confiança: alta · Medido em SQLite 3.50 com um esquema sintético de 50k/168k**

| Query | Local | Medido | Plano |
|---|---|---|---|
| `ORDER BY rating DESC, name ASC` (multi-source) | `MovieDao.kt:130`, `SeriesDao.kt:141` | **36,6 ms/página** | `SCAN movies` + `USE TEMP B-TREE FOR ORDER BY` |
| `ORDER BY addedAt DESC, sortOrder DESC, id DESC` (multi-source) | `MovieDao.kt:138,144`, `SeriesDao.kt:151,157` | **148,2 ms/página** | `SCAN movies` + `USE TEMP B-TREE FOR ORDER BY` |
| `WHERE catchup = 1` | `ChannelDao.kt:116-127,280-284,359-360` | varrimento do índice de nome dos 50k | `SCAN channels USING INDEX index_channels_name` |

**Porque acontece.** Um índice `(sourceId, rating, name)` só pode ser varrido **para a frente**
(`rating ASC, name ASC`) ou **para trás** (`rating DESC, name DESC`) — `rating DESC, name ASC` é uma
ordem de **direcções mistas**, que o SQLite **não pode servir**. E com `sourceId IN (…)` nem sequer
consegue conduzir o varrimento pelo índice.

**Impacto:** a grelha "Todos os filmes, por avaliação" **ordena a biblioteca inteira em cada
página** (o `LimitOffsetPagingSource` do Room re-executa a query com um `OFFSET` crescente). A 36 ms
por página num desktop; numa TV de 2019, 3–5× mais.

**Solução (exige mudança de esquema — Room migration + bump do `schemas/51.json` + lock)**

- **Para a avaliação:** adicionar `Index(value = ["rating", "name"])` a `MovieEntity` e
  `SeriesEntity` — deliberadamente **não** iniciado por `sourceId`, para a ordem global ser servida
  pelo índice. **[medido] 36,6 ms → 4,3 ms.**
  (O `@Index` do Room não expressa `DESC`; se a ordenação for puramente servida pelo índice, há que
  introduzir uma coluna `ratingSort` — mudança maior.)
- **Para "adicionado em":** adicionar `Index(value = ["addedAt", "sortOrder"])`. **[medido]
  148,2 ms → 0,3 ms.**
- **Para o catch-up:** adicionar `Index(value = ["catchup", "sourceId", "name"])` a `ChannelEntity`.
  (E `allCatchupChannels` — `ChannelDao.kt:116` — **não tem nenhum caller de produção**: é código
  morto que por acaso é uma varredura completa.)

⚠️ **Crítico:** qualquer índice novo tem de ser **também** adicionado a
`OwnTVDatabase.EXPECTED_NON_UNIQUE_INDEXES` (`:1472-1523`) — ver **DB-06** abaixo. Esquecer é
**destrutivo**.

### 23.9 🟠 DB-05 — Três sequências de escrita **sem transacção**

**Severidade: ALTA · Confiança: alta**

| Local | O que acontece |
|---|---|
| Core `sync/SyncSupport.kt:492-499` | O upsert por hash-diferença faz **quatro** chamadas Room em separado — `moveRanges`, `moveAll`, `updateAll`, `insertAll` — **quatro commits**, **sem atomicidade**. Um crash entre `moveRanges` e `insertAll` deixa o chunk meio aplicado. O KDoc da própria linha 492 diz *"Order matters"*. |
| Core `sync/M3uSyncer.kt:398-411` + `:300-322` | A série M3U faz `updateSeries` + `deleteSeasons` + `deleteEpisodes` + `upsertSeasonsReturnIds` + `upsertEpisodes` = **cinco commits**. Um crash entre `deleteEpisodes` e `upsertEpisodes` deixa a série **sem episódios**, e o `episodesSyncedAt` nunca é carimbado no caminho M3U (`SeriesDao.kt:383-385`) → a lista vazia persiste até ao próximo sync. |
| Core `repository/SeriesRepository.kt:71-84` | O merge lazy de episódios: `updateEpisodes` + `upsertEpisodes` + `deleteEpisodesByIds` + `markEpisodesSynced` — **quatro** chamadas, sem transacção. Um crash entre `inserts` e `deleteIds` deixa a série com **os episódios antigos *e* os novos**. |

**Solução:** envolver em `db.transaction { … }`. A primitiva já existe —
`core/database/RoomTransaction.kt:23-24`:
```kotlin
suspend fun <R> RoomDatabase.transaction(block: suspend () -> R): R = useWriterConnection { it.immediateTransaction { block() } }
```
⚠️ O KDoc em `:10-16` avisa: **`androidx.room.withTransaction` nunca deve ser usada aqui** — rebenta
em runtime assim que um `SQLiteDriver` está configurado. E o `SyncSupport` e o `SeriesRepository`
terão de receber a `OwnTVDatabase` injectada (hoje recebem só DAOs).

### 23.10 🟠 DB-06 — `EXPECTED_NON_UNIQUE_INDEXES` é um contrato **não asseverado** — e é **destrutivo**

**Severidade: ALTA (armadilha) · Confiança: alta**

**Localização:** Core `database/OwnTVDatabase.kt:1465-1523` (o contrato) ·
`database/BulkInsertHelper.kt:92-104, 125-128, 171-175` · o teste
`core/src/test/…/database/BulkInsertIndexContractTest.kt:16-33`

O helper **apaga todos** os índices não-únicos que encontra na tabela e **recria só** a lista
canónica. Hoje o mapa coincide exactamente com as entidades (verificado contra o `51.json`), logo
**não há drift vivo**. Mas o teste existente só verifica o **outro** sentido — **nada garante que o
conjunto de `@Index` de uma entidade seja igual ao mapa**.

E o KDoc em `BulkInsertHelper.kt:171-175` afirma que a combinação é *"impossible to express"* — é
trivially expressível e **silenciosamente destrutiva**.

**Impacto se acontecer:** o índice é apagado em cada importação nova e nunca restaurado → a app corre
**sem índices** até ao próximo arranque (e o *heal* só o repara se ele estiver no mapa — e não
estará) → e no próximo bump de esquema, a validação do Room rebenta (*"Migration didn't properly
handle …"*), o crash-loop documentado da 4.1.0.

**Solução — este é o "lock update" que cada mudança de índice tem de fazer:**
1. **Teste de CI** que lê o `schemas/51.json` exportado e afirma que, para cada tabela do mapa, o
   conjunto de nomes de índice não-únicos no JSON é igual ao conjunto parseado do mapa — assim um
   índice de entidade ausente do mapa **falha a CI**.
2. **Tornar o erro impossível em runtime:** em `dropIndexesForBulkInsert`, **apagar só os índices
   que o mapa canónico nomeia** (em vez de "tudo o que não é único na tabela"). Assim um índice não
   listado **nunca é apagado**, e o mapa deixa de ser uma allow-list destrutiva.

### 23.11 🟡 DB-07 — O `PRAGMA recursive_triggers` está **desligado**, e o `REPLACE` deixa o FTS obsoleto

**Severidade: MÉDIA · Confiança: alta · Reproduzido**

**Localização:** Core `database/dao/ChannelDao.kt:53-54`, `MovieDao.kt:20-21`,
`SeriesDao.kt:21-22,34-35,41-42,331-332` (todos `@Insert(onConflict = REPLACE)`)

**[Reproduzido]**
```
recursive_triggers pragma = 0
baseline: MATCH 'BBC' -> [(1,)]
after INSERT OR REPLACE (name -> 'CNN International'):
   MATCH 'BBC' -> [(1,)]   <-- o nome ANTIGO ainda bate
   MATCH 'CNN' -> [(1,)]
with PRAGMA recursive_triggers=ON:
   MATCH 'BBC' -> []       <-- correcto
```

**Currently unreachable in production** — o sync usa `updateAll` (`@Update`) e `insertAll`
(`@Insert(IGNORE)`) (`SyncSupport.kt:498-499`). Mas os DAOs `REPLACE` são **API pública**, e o
próximo caller herda um índice FTS que cresce e fica obsoleto.

**Solução:** (a) `PRAGMA recursive_triggers=ON` no `onOpen` do `DatabaseModule.kt` (uma linha, e faz
os triggers do Room behave bem sob REPLACE); ou (b) mudar os DAOs `REPLACE` para `IGNORE` + `@Update`
— o padrão que o sync já usa.

### 23.12 🟡 DB-08 — O `healSchema` não reconstrói o FTS depois de uma migração

**Severidade: MÉDIA (latente) · Confiança: alta**

O `_Impl` gerado pelo Room chama `dropFtsSyncTriggers` no `onPreMigrate` e recria os 16 triggers no
`onPostMigrate`. O `healSchema` (`:1547-1561`) só reconstrói uma tabela FTS que **tinha de criar**.

Qualquer migração que escreva em `channels`/`movies`/`series` deixa o índice FTS obsoleto, porque os
triggers ficaram apagados durante toda a cadeia e nada reconstrói depois.

**Solução:** na migração final da cadeia, emitir
`INSERT INTO <fts>(<fts>) VALUES('rebuild')` para cada tabela FTS cuja base tenha sido escrita.
`rebuild` sobre 50k linhas é inferior a um segundo.

### 23.13 🟡 Series C/O — O que **ainda vale** das séries do plano mestre

| ID | Veredicto | Acção |
|---|---|---|
| **O5** — snapshot correlacionado de QoE/HTTP/saída | ⚠️ **PARCIAL** | Já existem `PlaybackQoe` (`rebufferCount`, `rebufferMs`, `rebuildCount`), um traço correlacionado (`LivePreviewEngine.kt:1381-1387`) e falhas HTTP owner-scoped (`PlaybackSessionDiagnostics.kt:51-55`). **Falta**: (a) um **único** snapshot correlacionado — hoje são três stores separados; (b) **nenhuma trace Perfetto/atrace** em toda a árvore (`beginSection`, `atrace`, `Perfetto` → **zero**). É uma **tarefa de medição**, não um defeito |
| **O8** — variant de maior bandwidth; reserva fixa de 64 MiB | ⚠️ **PARCIAL** | A **reserva** está corrigida (`LocalTimeshiftSession.kt:190-196` usa o comprimento real; `LocalSegmentStore.kt:40-52` expulsa os segmentos antigos committed). A **escolha de variant** continua a ser a **maior bandwidth** entre as suportadas (`HlsRecordingPlan.kt:9-15`) — falta o "teto manual de qualidade" do utilizador. **Decisão de produto**, não defeito: não introduzir sem requisito |
| **O9** — `fsync` por segmento; remux lê a gravação inteira | ⚠️ **AINDA VALE (medição)** | `HlsSegmentTransfer.kt:29` mantém o `out.fd.sync()` e `DashRemux.kt:105-137` faz uma passagem sobre os samples. O próprio plano diz para **não remover sem medir**. A aceitação proposta (I/O, latência de commit, CPU, cortes simultâneos) **não foi executada** |
| **O10** — DELETEs de EPG agrupados | ⚠️ **PARCIAL** | O **staging** já está em disco (correcto). Os **DELETEs** são um por `epgChannelId` dentro da transacção (`GuideCacheReplacement.kt:39-40`), e o DAO **já tem** o DELETE agrupado (`EpgDao.kt:37-38`, já usado noutro sítio). **Fix:** `keys.chunked(200).forEach { clearBatch(sourceId, it) }` **dentro da mesma transacção** |
| **L4** — cache de subshift sem purga de órfãos | ⚠️ **AINDA VALE** | `SubtitleShift.kt:19-32` cria `subshift_<hash>_<offset>_<nome>` em `cacheDir` e **não existe purga de órfãos** (grep por `purgeOrphan`/`orphan` → **zero**). Um crash deixa ficheiros. **Fix:** purgar, **antes da criação dos engines**, apenas os ficheiros com prefixo `subshift_` em `cacheDir`, fora da main thread, e fechar a janela da cópia cancelada (`ExoSubtitleEngine.kt:486-489` — a cópia é criada **antes** de ser registada no mapa) |
| **S5** — limites e âmbito dos campos divergem | ⚠️ **AINDA VALE (3 pontos)** | (a) **pre-buffer**: a constante de domínio (`LiveLatency.kt:77`) não inclui o valor **1**, mas o editor por canal aceita-o (`ChannelPlaybackSettingsDialog.kt:219`) e o backup aceita até 30 (`BackupImportValidation.kt:95`) → **constante única**; (b) **desentrelaçamento**: continua a ser **só mpv** (`OwnTVPlayer.kt:718-719`) — a UI deve indicar a capacidade, como já faz com o HDR; (c) `liveTuneTimeoutSecs` por fonte viaja no backup mas o controlador lê **só** o global (`LiveTuneController.kt:1004`) → classificar como legado |
| **C3** — o cancelamento do hold AFR pode deixar um diferido pausado | 🔧 **CORRIGIDO**, com residual | `FrameRateController.kt:441-463` usa identidade do item + revisão de intenção (`stillOurPause()`). **Residual defensivo (não confirmado):** o `finally` também corre em cancelamento — adicionar a identidade do engine / uma flag `released` antes de retomar |
| **C7** — a âncora do menu deixa o canal sem requester normal | ⚠️ **PARCIAL** | O sintoma literal já não reproduz (`LiveScreen.kt:831-838` liga **ambas** as requesters). O que persiste: a âncora `contextChannelId` **nunca é limpa** depois de um restauro bem-sucedido, e **dois `Modifier.focusRequester()` no mesmo elemento** criam dois focus-targets para o mesmo nó. **Fix:** limpar a âncora no restauro bem-sucedido; preferir uma requester única + alias |
| **I8** — callbacks HDMI; rearm do latch estéreo | ✅ **NUNCA FOI DEFEITO** | O latch de surround **não** se rearma sozinho (`AudioOutputPolicy.kt:60-63` — um único chamador, quando o utilizador muda a configuração). O próprio código avisa (`PlaybackSession.kt:276`): *"Available outputs are not proof of the AudioTrack's active hardware route."*. **Acção correcta mantida:** usar a **rota efectiva**, não a lista de dispositivos. **Nenhuma alteração** |
| **R4-B21/B22/B23** (revisão externa) | ❌ **FALSOS POSITIVOS** | Ver §21.4 — não implementar aquelas correcções |

### 23.14 🟡 A sequência C/O em números

| Bloco | ✅ Corrigido | ⚠️ Parcial / ainda vale | ❌ Falso positivo | Total |
|---|---|---|---|---|
| C1–C17 | 14 | 2 (C7, C3-residual) | 1 (C1) | 17 |
| O1–O9 | 5 (O1,O2,O3,O4,O7) | 3 (O5,O8,O9) + O6 | 0 | 9 |
| I8, L4, O10, O13 | 0 | 4 | 0 | 4 |
| Council R1–R5, S1–S6, U1–U9, N1–N9 | 22 | 2 (S5, U9-minor) | 0 | 31 |

**61 itens verificados: 41 corrigidos, 9 ainda valem, 3 falsos positivos, 8 com descrições
desactualizadas — com 6 *fixes* propostos que não funcionariam.**

### 23.15 🟠 SET-01 — Uma classe nova: **definições que a app não consegue escrever**

**Severidade: ALTA (funcional) · Confiança: alta · Verificado por grep e por leitura**

> A verificação mecânica de chaves órfãs (§21.3 da Ronda 6) deu **zero**. Mas a verificação
> **semântica** — *quem chama estes setters a partir de um composable?* — encontra **três blocos
> inteiros de funcionalidade que o utilizador não consegue activar**. É a mesma classe do UX-01
> (a Pesquisa sem porta), mas no interior das Definições.

| ID | Definição inalcançável | Evidência | Consequência |
|---|---|---|---|
| **SET-01a** | **Todo o tier de metadados / TMDB** | `features/settings/MetadataSettingsScreen.kt:134` — um grep de `MetadataSettingsScreen` em todo o repo devolve **só a própria definição**; **não há call site** | `settings.metadataMode` (`SettingsViewModel.kt:1648`), `tmdbApiKey` (`:1656`), `metadataServerUrl` (`:1660`) e `metadataLanguage` (`:1680`) **nunca são escritos**. Os tiers `OWN_KEY` e `SELF_HOST` são **inalcançáveis**; e `HomeSettingsViewModel.trendingData` (`HomeSettingsViewModel.kt:60-62`) lê `metadataConfigFlow.enabled` — **a única forma de DESLIGAR a metadata é esse ecrã morto** ⇒ o utilizador não consegue desactivar o enriquecimento |
| **SET-01b** | **Chave de API do OpenSubtitles, URL do servidor, filtro de pesquisa e idiomas de legenda** | `SettingsViewModel.kt:1664, 1668, 831, 835` — **nunca chamados** em lado nenhum | O Core documenta toda a política do filtro de idiomas de legenda (`SettingsRepository.kt:1876-1889`) — e **não há forma de a usar**. O sub-ecrã do OpenSubtitles mostra a conta mas não oferece chave nem idiomas |
| **SET-01c** | **"Auto play next episode"** | O único caminho de escrita é o ramo `"vp_autoplay"` do `videoQuickBinding` (`VideoPlayerSettingsScreen.kt:371-374`), mas **`"vp_autoplay"` não está em `VIDEO_QUICK_ROWS`** (`:115-159`) e nenhuma linha declara `quickKey = "vp_autoplay"` (as 43 declaradas são `:822-1246`). O `autoPlayNext` é recolhido em `:548` e `SettingsScreen.kt:310` e **nunca usado** | `settings.autoPlayNext` só pode ter o valor por omissão. **A auto-reprodução de episódios — uma feature de topo do produto — está morta** |
| **SET-01d** | **Todo o "Pin to Quick"** | `vm.setQuickPinnedKeys` (`SettingsViewModel.kt:616`) **nunca chamado**; `Row2` passa `onLongClick = null` (`VideoPlayerSettingsScreen.kt:2019`) → `LocalQuickPin.onHold` nunca dispara; `menuKey` (`:576`) é escrito e **nunca lido**; `LocalQuickPin` recebe `pinned = emptyList()` (`:722`) → o ponto "também em Quick" nunca aparece; `vm.quickPinnedKeys` é recolhido (`:574`) e nunca usado. Os comentários da spine ainda descrevem um grupo Quick (`SettingsScreen.kt:1171, 1201-1203, 1246`) mas `groupDefinitions` (`:749-755`) **não tem grupo Quick** | Uma feature inteira, com os seus comentários e o seu estado vazio, **inerte** |
| **SET-01e** | **O chip "Panel Width" da raiz lê só a secção LIVE** | `SettingsScreen.kt:369-373` — `val panelWidthCustom = panelWidthLive`; `panelWidthMovies` e `panelWidthSeries` são recolhidos e **nunca usados** | A linha (`:625-626`) e a entrada de pesquisa (`:936`) dizem **"Default"** mesmo quando Filmes/Séries **estão** customizados |

**Solução para toda a classe — um teste, não cinco correcções:**

```kotlin
// app/src/test/…/features/settings/SettingsReachabilityTest.kt
/**
 * Every SettingsViewModel setter must be reachable from a composable.
 *
 * Three whole feature blocks (the metadata tier, the OpenSubtitles key and the subtitle-language
 * filter, and the Quick-pin scope) were implemented, backed by Core settings and Core
 * documentation, and had no way to be switched on.
 */
@Test
fun `every settings setter has a composable caller`() {
    val vm = File("src/main/java/tv/own/owntv/features/settings/SettingsViewModel.kt").readText()
    val setters = Regex("fun (set[A-Z][A-Za-z0-9]*)\\(").findAll(vm).map { it.groupValues[1] }.toSet()
    val app = File("src/main/java/tv/own/owntv").walkTopDown()
        .filter { it.extension == "kt" && it.path.contains("features") }
        .joinToString("\n") { it.readText() }
    val unreachable = setters.filter { ".$it(" !in app }
    assertEquals("settings with no way to be changed by the user", emptyList<String>(), unreachable.sorted())
}
```

**E depois, as correcções concretas:**

- **SET-01a** — adicionar um `SettingsTab.METADATA` e despachá-lo em `SettingsScreen.kt:427-468` (ou
  uma linha no "Mais").
- **SET-01b** — adicionar as linhas ao sub-ecrã do OpenSubtitles, **ou** apagar os setters e as 4
  chaves do Core.
- **SET-01c** — adicionar `VideoQuickRef("vp_autoplay", SECTION_EPISODES, …)` **e** uma linha com
  `quickKey = "vp_autoplay"`. **As três listas têm de mexer em conjunto** (`VIDEO_QUICK_ROWS`, as 43
  chaves declaradas e o `videoQuickBinding`) — senão o `VideoQuickRowCatalogueTest` fica vermelho. Ou
  **apagar** o ramo.
- **SET-01d** — implementar (compor o menu de hold-OK, chamar `setQuickPinnedKeys`, passar a lista
  real, adicionar o grupo Quick) **ou apagar** `QuickPinScope.pinned`, `menuKey`, `quickPinnedKeys` e
  os comentários obsoletos.
- **SET-01e** — `val panelWidthCustom = panelWidthLive || panelWidthMovies || panelWidthSeries`.

---

### 23.16 🟠 SET-02 — Fechar um diálogo aberto a partir de um **resultado de pesquisa** deixa o D-pad morto

**Severidade: ALTA (visível) · Confiança: alta · Verificado**

**Localização:** `features/shell/components/SettingsScreen.kt:1084-1092`, `:543-735`, `:944-1001`;
`ui/components/FocusTrap.kt:98-109`

O ecrã monta os resultados como:
```kotlin
1084:  val searchResults = catalogueEntries + (entries + videoEntries + specificEntries)
1085:      .filter { it.title !in catalogueTitles }
```
O clique da entrada do **catálogo** (`:1086-1089`) faz:
```kotlin
dialogReturn = searchFieldFocus
row.onClick()          // ← sobrescreve o dialogReturn
```
E o `onClick()` de **cada** linha que abre um diálogo põe `dialogReturn` no **seu próprio**
`FocusRequester` — `themeRowFocus` (`:543`), `accentRowFocus` (`:551`), `fontCustomizationRowFocus`
(`:578`), `zoomRowFocus` (`:594`), `startupRowFocus` (`:728`), `updateRowFocus` (`:735`), …

Esses requesters estão ligados às linhas em `selectedRows`, que **não são compostas durante a
pesquisa** (`:1284-1308` desenha `searchResults` em vez disso) → o requester está **destacado** →
`requestFocus()` falha → `restoreAfterDialogClose` (`ui/components/FocusTrap.kt:98-109`) desiste
depois de 10 frames e **nada tem o foco**. Só o Back funciona.

**E as entradas escritas à mão que foram feitas exactamente para resolver isto**
(`dialogReturn = searchFieldFocus` — `:944, 946, 948, 957, 966, 968, 926, 999, 1001`) são
**silenciosamente descartadas**, porque a de-duplicação mantém a entrada do *catálogo* e deita fora
a escrita à mão com o mesmo título.

**Solução** — inverter a precedência (duas linhas):
```kotlin
// features/shell/components/SettingsScreen.kt:1084
val handTitles = (entries + videoEntries + specificEntries).map { it.title }.toSet()
val searchResults = catalogueEntries.filterNot { it.title in handTitles } +
        (entries + videoEntries + specificEntries)
```
(ou pôr `dialogReturn = searchFieldFocus` **depois** de `row.onClick()` no wrapper do catálogo).

---

### 23.17 🟠 SET-03 — `NumberInputDialog` deixa o D-pad morrer quando o botão focado atinge o limite

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** `ui/components/NumberInputDialog.kt:89-91, 120, 136`; `ui/components/FocusTrap.kt:188-195`

```kotlin
89:    LaunchedEffect(Unit) { runCatching { (if (value > min) minusFocus else plusFocus).requestFocus() } }
```
Só escolhe o alvo **inicial**. Não há handover quando um botão **desabilita**:
`StepBtn(…, enabled = value > min)` (`:120`) e `enabled = value < max` (`:136`) — o botão
**desabilita-se enquanto tem o foco**, e o diálogo está dentro de `trapAllFocusExit()` (`:109`) →
**D-pad morto**; só o Back (`:103`) funciona.

**Reprodução:** Definições → Navegação de canais → "up skip" (`ChNavSettingsScreen.kt:208-219`,
`min = 1`, `step = 5`): premir "−" duas vezes a partir de 5 → valor 1 → o botão "−" desabilita-se
com o foco → D-pad morto. O mesmo no "down skip" e em `RecordingSettingsScreen.kt:248-284`
(pre-roll, post-roll, quotas).

**A app já tem o helper certo:** `rememberStepperFocus` (`ui/components/FocusTrap.kt:188-195`) passa
o foco para o outro lado no instante em que um desabilita; o `StepperDialog` usa-o
(`VideoPlayerSettingsScreen.kt:2377`) — **o `NumberInputDialog` não**.

**Solução:**
```kotlin
// ui/components/NumberInputDialog.kt — substituir os três requesters
val (minusFocus, plusFocus) = rememberStepperFocus(value < max, value > min)
```

---

### 23.18 🟠 SET-04 — DNS, DoH, proxy e URLs de metadata são guardados **sem validação** e falham em silêncio

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** `features/settings/DnsSettingsScreen.kt:85-101` · `features/settings/NetworkSettingsScreen.kt:86` ·
`features/settings/EpgSourcesScreen.kt:483` · `features/settings/MetadataSettingsScreen.kt:392`

**DNS/DoH:** `applySave` (`:85-101`) apenas faz *sniff* do prefixo. `"https://"` sozinho →
`dohUrl = "https://"`, `host = ""`. E `DnsConfig.dohUsable` é `enabled && dohUrl.isNotBlank()`
(Core `network/DnsConfig.kt:43`) → **true** → `resolveViaDoH` corre,
`Request.Builder().url("https://?name=dns.google&type=A")` rebenta, e o catch em
`DnsConfig.kt:131-135` cai no **DNS do sistema**. O ecrã diz **"DNS: On"**; nada muda, e nada
explica porquê. O mesmo com um host simples: `"hello"` é guardado e `InetSocketAddress("hello", 53)`
nunca resolve (`DnsConfig.kt:208-212`).

**Proxy:** `NetworkSettingsScreen.kt:86`:
```kotlin
val portInt = port.trim().toIntOrNull() ?: 0        // porta vazia ⇒ 0
val save = { vm.saveProxy(enabled, host, portInt, user, pass) }
```
O Core clampa a `0..65535` (`SettingsRepository.kt:2703-2711`) e `ProxyConfig.usable` é
`enabled && host.isNotBlank() && port in 1..65535` (`core/network/ProxyConfig.kt:31`) → **false** →
`activeProxy()` devolve null → `Proxy.NO_PROXY` → **todos os pedidos vão DIRECT**, enquanto o ecrã
mostra o toggle **ON** e o host preenchido.

**Metadata:** um URL com um gralha vira o tier `SELF_HOST` (`TmdbProvider.kt:44`) e **todas** as
chamadas TMDB passam a falhar.

**Solução — validar no ecrã antes de guardar, e usar a nota de erro que já existe**
(`DnsSettingsScreen.kt:150-157`):
```kotlin
// DoH: esquema + host válidos
val parsed = Uri.parse(dohUrl)
require(parsed.scheme in setOf("http", "https") && !parsed.host.isNullOrBlank()) { … }
// DNS simples: host tem de ser um literal IPv4/IPv6
require(host.isEmpty() || isIpLiteral(host)) { … }
// Proxy: recusar "ligado" com a porta vazia ou fora de 1..65535
if (enabled && (host.isBlank() || port !in 1..65535)) { showError(); return@… }
```

---

### 23.19 🟡 SET-05 — As secções **Filmes** e **Séries** do painel de larguras nunca são listadas → o ramo Cinematic está morto

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** `features/settings/PanelWidthSettingsScreen.kt:87, 125-128, 133-137, 177-181, 228, 421-426, 479`

* `:87` — `rowFocus = remember { listOf(PanelSection.LIVE)… }`
* `:125-127` — `detailsHeights = listOf(PanelSection.LIVE).associateWith { … }`
* `:128` — `listOf(PanelSection.LIVE).forEach { section -> … }`

MOVIES e SERIES **nunca são listados**. Logo `showDetailsHeight = cinematic && section != PanelSection.LIVE`
(`:228`) é **sempre falso**, e com ele:
* o stepper "Detail block height" (`:421-426`) nunca é composto;
* `vm.setCinematicDetailsHeight` (`:479`) nunca é chamado → **`settings.cinematicDetailsHeight` nunca
  é escrito**;
* `cinematicWidths(draft)` (`:242`) nunca é chamado;
* **`computeCinematicLayout` (`features/settings/data/PanelWidths.kt:89-106`) não é chamado de lado
  nenhum na app** (grep a todo o repo);
* os ramos `PanelSection.MOVIES/SERIES` em `sectionTitle` (`:177-181`) e no `when (section)`
  (`:133-137`) são **mortos**.

**Solução:** restaurar `listOf(PanelSection.LIVE, PanelSection.MOVIES, PanelSection.SERIES)` nas
linhas, no `rowFocus` e no `detailsHeights` — **ou** apagar o caminho Cinematic e a definição
`cinematicDetailsHeight` do Core.

### 23.20 🟡 SET-06 — Não há **confirmação da passphrase** no export de backup

**Severidade: MÉDIA · Confiança: alta · Verificado**

**Localização:** `features/settings/BackupScreen.kt:412-451` (`:435-447`), `:286`

`BackupPasswordDialog` tem **um** campo e confirma logo:
```kotlin
447:  OwnTVButton(confirmLabel, onClick = { onConfirm(password) }, enabled = password.isNotBlank())
286:  onConfirm = { pass -> exportFolder = null; vm.export(folder, exportSections, pass, exportProfiles) }
```

Uma gralha **dá um backup que o utilizador nunca consegue abrir**: o restauro pede a password e
responde "palavra-passe errada" (`BackupViewModel.kt:181-182` → `State.NeedPassword(retry = true)`),
sem forma de distinguir uma gralha de um ficheiro corrompido.

**Solução:** dar ao `BackupPasswordDialog` um **segundo campo de confirmação** e recusar continuar
enquanto os dois não coincidirem. (O prompt de **desbloqueio** do contentor selado, em `:295-313`,
deve manter um só campo — ali o utilizador está a digitar uma password *existente*.)

### 23.21 🟡 SET-07 — As contagens de secção do Video Player estão **hardcoded**, e 4 em 6 estão erradas

**Severidade: BAIXA · Confiança: alta**

**Localização:** `features/settings/VideoPlayerSettingsScreen.kt:705-709`

```kotlin
val sectionCounts = listOf(
    14 + if (perPlaylist) 1 else 0,
    5 + (if (livePreview) 1 else 0) + (if (perPlaylist) 2 else 0),
    6, 2, 3, 2,
)
```

Contagens reais por bloco `SECTION_* ->`: **ENGINE 20, LIVE 12, SOUND 5, SUBTITLES 2, EPISODES 2,
DIAGNOSTICS 2**. Os badges lêem 14 (real 20), 5-8 (real 12), 6 (real 5), 3 (real 2).

A raiz das Definições faz bem (`count = rows.size`, `SettingsScreen.kt:1208`) — **copiar esse modelo**.

### 23.22 🟡 SET-08 — `LocalSyncViewModel.cancel()` apaga um ficheiro **no main thread**

**Severidade: BAIXA · Confiança: alta**

`features/settings/LocalSyncViewModel.kt:208-213` — `cancel()` é chamada directamente da UI e
`(step as? Step.Confirm)?.file?.delete()` é uma **chamada de sistema de ficheiros bloqueante**, sem
`try/catch`.

**Solução:**
```kotlin
viewModelScope.launch(Dispatchers.IO) { runCatching { file.delete() } }
```

### 23.23 🟡 SET-09 — A pesquisa **sobrevive** ao sair do ecrã por qualquer via que não seja Back

**Severidade: BAIXA · Confiança: alta**

`features/search/SearchScreen.kt:112-117` limpa a query no `BackHandler`, e o `SearchViewModel` é
**Activity-scoped** (o próprio repo nota-o em `features/more/MoreCountsViewModel.kt:69-72`). Sair da
Pesquisa por um item da barra lateral e voltar mostra **a query e os resultados anteriores**.

**Solução:** limpar `_query`/`_intent` no `DisposableEffect` do ecrã (ou fazer o VM com scope de
entrada de navegação), para que a re-entrada comece sempre vazia.

### 23.24 ✅ Verificado e correcto (sem correcção) — o que foi testado e não tem defeito

| Verificação | Resultado |
|---|---|
| O utilizador pode criar um painel de largura **zero** ou uma soma ≠ 100? | **Não** — `MIN = 10` (`core/settings/PanelWidths.kt:19-20`), `balanceToTotal` (`:57-75`) dá sempre exactamente 100, o Core re-balanceia na leitura (`SettingsRepository.kt:1800-1806`) e `computePanelWidths` (`data/PanelWidths.kt:109-127`) normaliza pela soma real |
| O regex do **bulk rename** é fornecido pelo utilizador? | **Não** — o valor é escapado (`core/customize/RenameRules.kt:146`, `Regex.escape`); o campo `pattern` é interno do motor e é limpo a cada edição (`BulkRenameDialogs.kt:329,347,361`). **O utilizador não consegue injectar um regex** |
| A query FTS é construída com segurança? | **Sim** — o `SearchReader.ftsQuery` do Core (`:170-175`) tokeniza em não-letra/não-dígito e cada token vira um termo com prefixo; é o **único** produtor do bind `:ftsQuery`, e é um **parâmetro ligado**, nunca concatenado |
| O proxy é aplicado a **todos** os caminhos de pedido? | **Sim** — o `OkHttpClient` partilhado (`core/di/DataModule.kt:52-53`), o cliente de streaming (`core/network/StreamingHttpClient.kt:29`), o Coil (`OwnTVApp.kt:189`) e o mpv (`http-proxy` de `mpvProxyUrl()`, `core/network/ProxyConfig.kt:88-99`). Só o **bootstrap** DoH o evita, por desenho (para o DNS custom não se repetir em loop) |
| As larguras sobrevivem à re-entrada? | **Sim** — re-semeadas do valor guardado, já balanceado (`PanelWidthSettingsScreen.kt:232`, `GuideWidthSettingsScreen.kt:128`) |
| O preview do bulk rename é o que é aplicado? | **Sim** — a string do preview é a string aceite (`core/customize/BulkRenameSession.kt:99, 137`) |
| O conjunto `bespokeVideoKeys` tem entradas obsoletas? | **Não** — as nove chaves existem em `VIDEO_QUICK_ROWS` e os nove títulos estão no bloco de pesquisa |

---

## 24. Veredicto final - o que continua válido, com a solução correcta

### 24.1 Como ler esta secção

Esta é a **resposta final** ao pedido de reavaliação. Cada item que sobreviveu tem **veredicto**,
**onde está**, **a solução correcta** (já com as correcções da §22 aplicadas), **prioridade** e
**esforço**.

**Contagem final, apurada sobre as 8 rondas:**

| | Nº |
|---|---|
| Achados e ideias registados (rondas 1–8) | **~215** |
| **Arquivados** (falsos positivos, duplicados, superados por decisão de produto) | **~30** |
| **Com a solução corrigida** por um conselho de reavaliação | **~28** |
| **Válidos, com solução correcta e verificada** | **~157** |

### 24.2 🔴 FASE 0 — Distribuição (5 itens, ~1 dia) — *o bloqueio real*

| # | Item | Onde | Solução |
|---|---|---|---|
| 1 | **REL-05** — a tag rebenta no `versionCode` | `android.yml:112-114` | `IFS='.' read -r MA MI PA BUILD` + `CODE=$(( MA*1000000 + MI*10000 + PA*100 + BUILD ))`; **mesma fórmula** no `build.gradle.kts:50` (§22.17) |
| 2 | **REL-01/REL-02** — só se publica o APK de 32 bits; o "always-latest" dá 404; e publica-se o universal de 52,8 MB em vez do arm64 de 32 MB | `android.yml:185-226` | Publicar `dist/OwnTV.apk` (arm64 de 32 MB) + universal + x86_64; `curl -fI` a cada URL prometido no fim do job |
| 3 | **REL-06/REL-07** — dois esquemas de `versionCode`; changelog sem secções `v105.*` | `build.gradle.kts:49-51` | Um esquema só; escrever as secções; mover o check de changelog para o push a `main` |
| 4 | **DOC-01/DOC-02** — 9 links do README para um repo inexistente; token obsoleto | `README.md` | `renatofc27/OwnTV` → `renafcampos27/Owntv`; `renatofc27/OwnTV_Core` → `ahXN00/OwnTV_Core`; reescrever a secção de build |
| 5 | **SEC-03/QA-03** — release assinado com a chave debug | `build.gradle.kts:175-179` | `throw GradleException(...)`; falhar a tag sem keystore; `apksigner verify` pós-build |

### 24.3 🔴 FASE 1 — Funcionalidade que existe mas o utilizador não alcança (7 itens, ~3 dias)

| # | Item | Onde | Solução |
|---|---|---|---|
| 6 | **UX-01** — a **Pesquisa** não tem porta | `TopBar.kt:96-110` | Restaurar o `SearchPill` (o apagado já tinha `focusProperties { canFocus = visible }`) |
| 7 | **UX-03** — "Downloads" só mostra gravações; o separador é inerte | `DownloadsScreen.kt:94,245-254` | `tab` como estado real; 3 separadores com `onClick` reais |
| 8 | **UX-04** — Favoritos / Histórico / Sincronização local nunca desenhados | `MoreScreen.kt:255-306` | Adicionar as 3 `SpineRow`s; apagar os resets de `:138-139` |
| 9 | **UX-02 + PERF-02** — Trending morto; hero a 12,5 Hz | `HomeViewModel.kt:548`; `HomeScreen.kt:663-676` | ⚠️ **Ver §22.8 e §22.6** — **não** mexer em `liveOnly`; activar só a rail de Trending; e animar o hero com `animateFloatAsState` + um `LaunchedEffect` separado |
| 10 | **UX-07** — apagar sem confirmação | `RecordingsScreen.kt:319` | Confirmação em dois passos, como o `MoveOrderOverlay` |
| 11 | **DWN-01** — a recusa do orçamento perde o motivo | Core `DownloadEngine.kt:121-146` | ⚠️ **Ver §22.11** — transporte do motivo pela **notificação/toast** primeiro; a coluna Room vem no **próximo bump do Core**, agrupada com SEC-12/13/DATA-01/SEC-15 |
| 12 | **UX-08/PLY-04 + R4-B19** — foco morto nos overlays; `restoreFocus` presa | `MiniGuideOverlay.kt:71-76`; `CatchupJumpDialog.kt:132`; `DownloadsScreen.kt:165`; **`EpgScreen.kt:255`** | `requestFocusRetrying` nos 4 sítios; e o padrão de `HomeScreen.kt:246-248` nos **três** ecrãs (§22.14) |

### 24.4 🟠 FASE 1-bis — Definições inalcançáveis e foco morto nos diálogos (10 itens, ~3 dias)

| # | Item | Onde | Solução |
|---|---|---|---|
| 13 | **SET-01a** — o ecrã de metadados/TMDB **nunca é invocado** ⇒ os tiers `OWN_KEY` e `SELF_HOST` são inalcançáveis e a metadata não pode ser desligada | `MetadataSettingsScreen.kt:134` | Adicionar um `SettingsTab.METADATA` e despachá-lo em `SettingsScreen.kt:427-468` |
| 14 | **SET-01b** — chave de API do OpenSubtitles, URL do servidor, filtro e idiomas de legenda: **sem controlo** | `SettingsViewModel.kt:1664,1668,831,835` | Adicionar as linhas ao sub-ecrã, **ou** apagar os setters e as 4 chaves do Core |
| 15 | **SET-01c** — "auto play next episode" **inalcançável** (`"vp_autoplay"` não existe no catálogo) | `VideoPlayerSettingsScreen.kt:371-374, 115-159` | Adicionar o `VideoQuickRef` **e** a linha com `quickKey` — **as três listas em conjunto** — ou apagar o ramo |
| 16 | **SET-01d** — todo o "Pin to Quick" é **inerte** | `SettingsViewModel.kt:616`; `VideoPlayerSettingsScreen.kt:2019,576,722` | Implementar (menu de hold-OK, lista real, grupo Quick) **ou** apagar o código morto e os comentários obsoletos |
| 17 | **SET-01e** — o chip "Panel Width" lê só a secção LIVE | `SettingsScreen.kt:369-373` | `panelWidthLive \|\| panelWidthMovies \|\| panelWidthSeries` |
| 18 | **SET-02** — fechar um diálogo aberto a partir de um **resultado de pesquisa** deixa o D-pad morto | `SettingsScreen.kt:1084-1092` | Inverter a precedência na fusão das entradas (duas linhas) |
| 19 | **SET-03** — `NumberInputDialog` morre quando o botão focado atinge o limite | `NumberInputDialog.kt:89-91` | `rememberStepperFocus(value < max, value > min)` — o helper **já existe** no repo |
| 20 | **SET-04** — DNS/DoH/proxy/URLs guardados **sem validação** e a falhar em silêncio | `DnsSettingsScreen.kt:85-101`; `NetworkSettingsScreen.kt:86` | Validar esquema/host/porta antes de guardar, usando a nota de erro existente |
| 21 | **SET-05** — só a secção LIVE do painel de larguras é listada ⇒ o ramo Cinematic está morto | `PanelWidthSettingsScreen.kt:87,125-128` | Restaurar as três secções, **ou** apagar o caminho Cinematic |
| 22 | **SET-06** — o export de backup **não confirma** a passphrase ⇒ uma gralha perde o backup | `BackupScreen.kt:435-447` | Segundo campo de confirmação |

**E, para fechar a classe de vez:** um teste `SettingsReachabilityTest` que exija que **todo** setter
do `SettingsViewModel` tenha um caller num composable (§23.15). Três blocos de funcionalidade
morreram porque **ninguém verificou essa ligação**.

### 24.5 🟠 FASE 2 — Player: os quatro achados novos mais graves (4 itens, ~1 semana, **todos no Core**)

| # | Item | Onde | Solução |
|---|---|---|---|
| 13 | **PLY-13** — um Pause durante o handover **encrava a sintonia para sempre** e desliga o alarme | Core `LiveTuneController.kt:497-605, 713-714, 839-846` | `HandoverIntent.pausedByUser` explícito; loops **limitados**; o `break` do alarme independente do guard (§23.1) |
| 14 | **PLY-14** — toda a falha que não seja HTTP é "de formato" → degrau errado + lição errada | Core `LiveTuneController.kt:780`; `LiveLadder.kt:193-209` | `classifyLiveFailure()` → `FORMAT/CODEC/TRANSPORT/REQUEST_REFUSAL/TIME`; `CODEC` salta o `.ts` do mesmo motor e **não** regista lição (§23.2) |
| 15 | **PLY-15** — `awaitMpvOutcome` nunca regressa → poller a 10 Hz e a escada nunca avança | Core `LiveEngines.kt:191-209` | Erro **incondicional**; timeout **sem** creditar pausas; `null` com pausa ⇒ parar de observar, **sem** `advance()` (§23.3) |
| 16 | **PLY-16** — o botão de play/pause mostra "Play" mas **pausa** | `PlayerHudChrome.kt:293`; `MiniPlayer.kt:64,135` | Rotular por `playbackRequested` (§23.4) |

### 24.6 FASE 3 - Dados: o Room (7 itens, ~1 semana, **todos no Core**)

| # | Item | Onde | Solução |
|---|---|---|---|
| 17 | **DB-01** — um trigger FTS4 perdido é **permanente** e a pesquisa global morre | Core `BulkInsertHelper.kt:107-145`; `OwnTVDatabase.kt:1592-1620` | `EXPECTED_FTS_TRIGGERS` canónico (16, copiado do `_Impl`); criar no `healSchema`; sondar no drift probe; restaurar do DDL canónico (§23.5) |
| 18 | **DB-02** — `healSchema` cria a tabela FTS **sem triggers** | Core `OwnTVDatabase.kt:1548-1557` | O mesmo §23.5, passo 1-2 |
| 19 | **DB-03** — a sonda de drift não vê triggers nem índices únicos | Core `OwnTVDatabase.kt:1592-1596` | Estender `EXPECTED_SCHEMA_OBJECTS`; recriar no heal (§23.7) |
| 20 | **DB-04** — três grelhas fazem scan+sort da tabela inteira **[medido: 36,6 ms e 148,2 ms/página]** | Core `MovieDao.kt:130,138`; `SeriesDao.kt:141,151`; `ChannelDao.kt:116-127` | Índices `["rating","name"]`, `["addedAt","sortOrder"]`, `["catchup","sourceId","name"]` — **com** a actualização do `EXPECTED_NON_UNIQUE_INDEXES` (§23.8) |
| 21 | **DB-05** — três sequências de escrita sem transacção | Core `SyncSupport.kt:492-499`; `M3uSyncer.kt:398-411`; `SeriesRepository.kt:71-84` | `db.transaction { … }` via `RoomTransaction.kt:23` (§23.9) |
| 22 | **DB-06** — o contrato dos índices é destrutivo e não é asseverado | Core `OwnTVDatabase.kt:1465-1523`; `BulkInsertHelper.kt:92-128` | Teste de CI contra o `schemas/51.json`; e apagar **só** os índices do mapa (§23.10) |
| 23 | **DB-07/DB-08** — `recursive_triggers` desligado; FTS não reconstruído após migração | Core `DatabaseModule.kt`; `OwnTVDatabase.kt:1547-1561` | `PRAGMA recursive_triggers=ON`; `INSERT INTO <fts>(<fts>) VALUES('rebuild')` no fim da cadeia de migrações (§23.11, §23.12) |

### 24.7 FASE 4 - Seguranca e integridade (9 itens, ~1 semana, **no Core**)

| # | Item | Onde | Solução |
|---|---|---|---|
| 24 | **SEC-12** — iterações PBKDF2 sem validação → CPU DoS | Core `BackupCrypto.kt:65-70` | ⚠️ **Ver §22.1** — validar `kdf` + `iterations` (+ `iters <= 0`); o **esquema** é parâmetro do chamador, **nunca** uma constante global |
| 25 | **SEC-13** — o esquema do contentor nunca é verificado na leitura | Core `BackupContainer.kt:238` | ⚠️ **Ver §22.2** — aceitar o esquema actual e o anterior; nunca subir `CONTAINER_SCHEME` sem a aceitação do antigo |
| 26 | **SEC-14** — a sync local materializa o backup ~5× | Core `LocalSyncClient.kt:91-92,114` | ⚠️ **Ver §22.3** — streaming do base64 em blocos de 3 bytes, **protocolo inalterado** |
| 27 | **SEC-15** — o `username` sai em claro sem passphrase | Core `BackupManager.kt:1504` | ⚠️ **Ver §22.4** — selar o `username`; recusar o export sem cifra com credenciais embebidas |
| 28 | **DATA-01** — falhas de leitura na exportação viram listas vazias | Core `BackupManager.kt:216,321-342` | ⚠️ **Ver §22.5** — chave top-level no `root`; ou **fazer o export falhar** (mais honesto) |
| 29 | **SEC-01** — cleartext global + credenciais no path e na query | `AndroidManifest.xml:49`; Core `XtreamClient.kt:444-516` | `network_security_config.xml` com excepções por source; aviso explícito para `http://` |
| 30 | **SEC-04/R4-B07** — PIN em SHA-256 simples, sem lockout | Core `Pin.kt:13-27`; `ProfileGate.kt:97-101` | ⚠️ **Ver §22.12** — formato versionado **com leitura de fallback** para o formato legado; + backoff persistido |
| 31 | **R4-B01** — o restauro altera `isKids`/`pinHash` | Core `BackupManager.kt:874-889` | Ignorar os dois campos em perfis existentes **e** exigir PIN ao criar um perfil Kids vindo de um ficheiro |
| 32 | **R4-B04** — perfis Kids deixam passar canais sem `categoryId` | Core `AdultCategoryClassifier.kt:36-37,66-72` | Negação por omissão em Live, EPG, Downloads, Home e pesquisa, com excepção parental |

### 24.8 FASE 5 - Estabilidade, foco e UX fina (16 itens, ~1 semana)

**Estabilidade (5):** R4-B09 (gravações sem `try/catch`) · R4-B10 (`continueAfterCatchup`) ·
R4-B02/R4-B03 (índices `rows` vs `items`, **com a solução da §15.6.2, que foi verificada**) ·
PLY-01 (OSD do canal errado) · PLY-10/PLY-11 (direct tune).

**Foco e UI (7):** UX-06 (`contentDescription` no mini-player) · UX-09 (overlay de categorias) ·
UX-10 (shell recompõe de 5 em 5 s) · UX-11 ⚠️ **ver §22.7** · UX-12 (o Home engole o Back) ·
PLY-02 (anel ≠ foco no MultiView) · PLY-03 (dois `FocusRequester` no mesmo nó).

**Correcções de robustez (4):** PLY-06 (histórico imediato) · PLY-07 ⚠️ **ver §22.10** ·
PLY-09 (freeze frame reciclável) · R7-11 (lição DASH sem invalidação) · R7-12 (`rearmAll`).

**Fixes defensivos no Core (3):** R7-03 + R7-09 (remover os **dois** defaults de
`PlaybackEngine.playbackRequested` / `setPlaybackRequested`) · R7-05 (limpar os zips órfãos no
`snapshot_core.py`).

### 24.9 FASE 6 - Performance e arquitectura (10 itens, ~2 semanas)

**Performance (6):** DAT-01 ⚠️ **ver §22.9** · DAT-02 (17 `Eagerly`) · DAT-03
(`MAX_SIZE_UNBOUNDED`) · DAT-04 (backup automático sem foreground) · PERF-01 (44 `StateFlow`s — **o
refactor não é "mecânico"**, ver §2.4-bis) · PERF-02 ⚠️ **ver §22.6**.

**Dados (4):** DAT-05 (throttle anulado) · DAT-06 (scope não estruturado) · DAT-07 (N+1 nos
downloads) · DAT-09 (20 000 `ChannelEntity` no guia).

**Arquitectura (3):** F15 fases 1-4 (extrair estado → partir o dispatch → partir os diálogos →
partir o `LiveViewModel`, com a **superfície pública inalterada**) · F7 ⚠️ **ver §22.13 — remover
apenas 3 dependências** · R7-02 (KDoc do B7 + teste — **destrava o CAT-1**).

### 24.10 FASE 7 - Build, CI e i18n (7 itens, ~1 semana)

F4 (266 strings em 2 de 26 idiomas — **o verificador de resources ou a migração para o Core**) ·
QA-01 (androidTest no CI — **3 classes na app**, §22.16) · QA-02 (SAST/Dependabot/SBOM/CodeQL) ·
F8 (`verification-metadata.xml` para a `libmpv`) · QA-09 (arquivar o `mapping.txt`) ·
F3/REL-04 (`splits.abi` × `abiFilters` → 3 APKs por flavor) · REL-08/REL-09 (zips órfãos; 1,78 GB
de snapshots).

### 24.11 As ideias que continuam validas - e as que subiram

| Prioridade | Ideia | Esforço | Porquê |
|---|---|---|---|
| **1** | **R7-01 — mitigação do bloqueio A–B–A** | S (incr. 1) | O **único** achado que ataca o bloqueio nº1 do projecto. O Incremento 1 (esperar só **depois de uma recusa real**) é pequeno e está no Core; o Incremento 2 (reusar a sessão) resolve a **contagem**, não só o tempo |
| **2** | **R7-02 — KDoc do B7 + teste** | S | **Destrava o CAT-1** (a maior ideia de produto do projecto). Uma linha de comentário + um teste |
| **3** | **CAT-3 — catálogo demo no primeiro arranque** | M | Resolve o vazio dos 5 primeiros minutos e dá conteúdo ao coach |
| **4** | **U1 — temporizador de sono** | S | Resolve a TV acordada a descodificar 4K de noite **e** liberta um slot do provider |
| **5** | **CAT-1 — Auto-Tune Memory** | M | A maior ideia de produto — **mas só depois de R7-02** (§24.8) |
| **6** | **CAT-6 — veredicto de saúde por canal** | M | ⬆️ **Já não está bloqueado** (O4 corrigido). Mas ver o residual: `bitrateRow` devolve **0** quando o formato não declara bitrate → *"desconhecido" apresentado como 0,0 Mbps*. **Fix: `Bitrate(Long?)` com `null` = não declarado → "—"**; e nunca derivar saúde de uma amostra instantânea |
| **7** | **CAT-2 — TV-Remote** | M-L | O servidor companheiro **já existe**; dá um emprego real ao OwnTV Mobile |
| **8** | **F8 (retenção) — o painel "A sua semana OwnTV"** | M | A única métrica de retenção possível sem servidor |
| **9** | **U5 — pesquisa por voz** | M | Complementar do UX-01: a Busca precisa de uma porta *e* de um método de entrada |
| **10** | **CAT-13 / CAT-17 / CAT-18** (API de metadata, theme packs, layout OLED) | S-M | Barato, e o layout OLED é a queixa nº1 a longo prazo |
| **11** | **CAT-15 / CAT-16 / CAT-19** (códigos de canal, pilha da casa, EPG diferencial) | M | Complementares |
| **12** | **"Só em Wi-Fi" para o EPG** (reformulado, §15.8-bis) | S | Única política de rede com sentido numa box; a primitiva **já existe** no Core (`downloads_wifi_only`) |
| **13** | **CAT-8 — classificações reais** (em vez do regex do modo Kids) | M-L | O único que torna o modo Kids fiável |
| **14** | **CAT-20 — salto de publicidade** | L | ⚠️ **Mantém-se em último**, pelo risco de falso positivo e legal |

### 24.12 O que fica arquivado (e porque) - para nao voltar a debate-lo

| ID | Razão |
|---|---|
| **BUD-01** (orçamento sem fullscreen) | Design deliberado e documentado em `WatchSession.kt:9-33`. **A "correção" causaria dano** |
| **B21** (buffers do timeshift) | O ficheiro citado não existe; o mecanismo descrito não existe. **A "correção" corromperia respostas concorrentes** |
| **B22** (índice `targetItemId`) | **Nenhuma query o usa.** O índice foi correctamente **não** criado |
| **B23** (`awaitMpvOutcome` — o *fix* externo) | Destruiria a **pausa deliberada**. O código actual está correcto |
| **B4** (ducking) | O ducking interno foi **removido por decisão do utilizador**; a "correção" o reintroduziria |
| **C1** (ganho/boost de volume) | Obsoleto — contradiz a decisão do utilizador (volume interno a 100 %) |
| **B13** (charset do `Pin.kt`) | `pin.toByteArray()` **já é UTF-8**. Estilo, não bug |
| **NET-01** (política de dados móveis) | O Core implementa-a e documenta-a. **Só o EPG fica por cobrir** |
| **"Armazenamento não governado"** | O Core tem `MediaStorageBudget` com quotas, `Lease` e motivos. Falta a **visibilidade** (→ DWN-01) |
| **BUG-P2-07** (PIN sem autenticação) | Exagerado: o PIN actual **é** exigido quando existe. O caso real é o **Kids sem PIN** (R4-B08) |
| **"Migração 5→6 em falta"** | Todas as versões de esquema estão presentes; o passo 5→6 é transitório. **Resta um `MIGRATION_5_6` no-op** para um device de dev não entrar em crash-loop |
| **O4 / BUG-P1-01** (marcados "válidos" na §15) | **A Parte III corrige a Parte II:** ambos estão **já corrigidos**. Ler a §21.2 |
| **UPSCALING-BETA-01** | Já registado em `docs/MELHORIAS_FUTURAS_PENDENTES.md` — respeitado, não re-proposto |

### 24.13 As correccoes que **nao** devem ser implementadas (a lista mais importante)

| Documento | "Solução" proposta | Porquê **não** |
|---|---|---|
| PLANO MESTRE B4 | "associar o estado de atenuação ao motor proprietário" | O ducking interno foi **removido por decisão do utilizador**; reintroduzi-lo é uma regressão |
| REVISÃO B21 | pool de buffers do timeshift | Um buffer partilhado entre sockets **corromperia respostas concorrentes** (4 sockets em paralelo) |
| REVISÃO B22 | criar o índice `targetItemId` | **Nenhuma query o usa** — só custo de escrita e migração |
| REVISÃO B23 | "abortar quando `playbackRequested == false`" | **Destruiria a pausa deliberada** e dispararia um erro sem acção do utilizador |
| PLANO MESTRE B1 / C1 / B13 |增益/boost, charset | Obsoletos ou já implementados |
| **A minha §22.1 (SEC-12)** | `if (scheme != SCHEME) return null` | ⚠️ **Quebraria todos os backups cifrados** — o `SCHEME` do campo não é o do contentor |
| **A minha §16.6 item 51 (F7)** | "remover 8 dependências" | ⚠️ **`room-ktx` é usada** — remover quebra a compilação |
| **A minha §16.5 item 39 (UX-11)** | "instalar `focusRestorer()` incondicionalmente" | ⚠️ Luta com o `redirectEntryFocus` emparelhado em `OwnTVShell.kt:1205` |

---

## 25. Limitações finais

1. **Sem build, sem testes, sem dispositivo.** Todos os veredictos são de leitura de código no commit
   `bdffec4` com o Core `1.0.57-own.20261011` (snapshot `e2081e78…`). Onde o agente **executou** algo
   (reproduções em SQLite, em bash, a suite de 6 testes do `snapshot_core.py`), está marcado como
   **[medido]** ou **[reproduzido]**.
2. **A Ronda 7 teve 6 agentes; a Ronda 8 teve 6** (Core, C/O, A15, B, player-core, Council). A
   **Ronda 6** foi executada pelo conselho directamente, depois de os 6 agentes caírem por uma quebra
   de ligação.
3. **A cobertura mais fina:** os ecrãs de definições e as superfícies secundárias foram varridos
   por mecânica e por leitura dirigida, **não linha a linha** nas 30+ sub-telas.
4. **O gargalo de execução não é o código.** É o **ciclo de release do Core** (SHA-256 fixado, 1 007
   ficheiros, `core-source-lock.json`) e o **pipeline de distribuição** (REL-05 → REL-01/REL-02). Todas
   as correcções de dados/segurança do Core dependem do primeiro; e sem o segundo, **nenhuma delas
   chega ao utilizador**.
5. **A dependência do Core é subestimada em todo o relatório.** As Fases 2, 3, 4 e 5 (26 itens)
   tocam o Core → exigem **um** bump combinado (Core release → re-snapshot → lock), não seis.

---

## 26. Conclusão final das 8 rondas

O OwnTV tem **um codebase melhor do que a sua distribuição** — e, desde a Ronda 7, sabemos também
**melhor do que os seus próprios documentos de auditoria**.

Três conclusões, em ordem:

**1. A distribuição é o bloqueio.** A tag rebenta (REL-05) → a release é publicada à mão → sai o APK
de 32 bits e o "always-latest" dá 404 (REL-01) → o README aponta para um repositório que não existe
(DOC-01). **Enquanto a Fase 0 não estiver feita, nada do resto — 157 itens — chega ao utilizador.**

**2. Há uma classe de bug que reaparece em todo o lado: "a camada por baixo perde a informação".**
O motivo da recusa de armazenamento (DWN-01), a excepção do `catch` (CAN-01), o `iterations` do
ficheiro importado (SEC-12), o `scheme` do contentor (SEC-13), a razão do trigger FTS (DB-01), o
motivo do bitrate (CAT-6). **Cada vez, a camada por cima está correcta e a camada por baixo deita
fora o porque.** E há uma segunda classe, simétrica: **"a funcionalidade existe, a porta não"** —
a Pesquisa (UX-01), o Trending (UX-02), as transferências (UX-03), os Favoritos (UX-04), o
`liveOnly` (UX-02), o `downloads_wifi_only` (§15.8-bis), o `setPlaybackRequested` (R7-09), o tier
de metadados e a auto-reprodução de episódios (SET-01a/c).

**3. O project's own audits were mostly right, and mostly already fixed.** De **61** itens das séries
C/O, **41** estão corrigidos. De **16** da série A15, **14**. De **15** da série B, **12** e **2** são
falsos positivos. **As decisões de arquitectura que o projecto tomou estão certas** — o *watch
session* separado, o orçamento de armazenamento, a camada de validação do backup, a escada de 4
degraus, o `MELHORIAS_FUTURAS_PENDENTES`. O que falta não é reescrever a app: é **fechar o pipeline**,
**abrir as portas**, e **agrupar as mudanças do Core num único bump**.

A ordem que o conselho recomenda, e que as 8 rondas sustentam:

> **Fase 0 (distribuição) → Fase 1 (as portas) → Fase 1-bis (as definições inalcançáveis) → Fase 2-3
> (os bugs de player e de dados, num único bump do Core) → Fase 4 (segurança) → Fase 5-7
> (performance, build, i18n) → as ideias.**
>
> E, dentro de cada fase, **ler a §22 antes da §16** — porque **sete** das correcções que escrevi
> na Ronda 6 estavam erradas, e o conselho apanhou-as antes de chegarem a você.

---
*Fim do relatório. Partes I (Ronda 5), II (Ronda 6), III (Ronda 7) e IV (Ronda 8) — 2026-10-19.
Consolidação e verificação directa do conselho; ~157 achados válidos com solução verificada,
~30 arquivados, ~28 correcções revistas.*
