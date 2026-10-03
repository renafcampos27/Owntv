# Limpeza de código, dependências e ficheiros — 03/10/2026

## Âmbito e evidência

Limpeza sem alterações funcionais pretendidas. Pesquisa conjunta na app e no core, incluindo código de testes, Gradle, manifests, regras R8 e referências indiretas. Contar referências apenas num módulo não basta: KoinWorkerFactory parece sem utilização dentro do core, mas é criado por OwnTVApp; os geradores de baseline são descobertos pelo runner JUnit. Ambos preservados.

## Código removido

- SectionChip — app\src\main\java\tv\own\owntv\features\customize\CustomizeScreen.kt; 25 linhas.
- trendingStatusText — app\src\main\java\tv\own\owntv\features\settings\HomeSettingsScreen.kt; 28 linhas.
- GlassEffectContentScreen — app\src\main\java\tv\own\owntv\features\shell\components\SettingsScreen.kt; 247 linhas.
- mpvPinKey — app\src\main\java\tv\own\owntv\features\live\LiveViewModel.kt; 4 linhas.
- glassPresetDescription — descrição usada apenas pelo ecrã antigo removido; 13 linhas.
- OwnTVCard.kt: componente sem chamadas no checkout da app/core; removido.
- Constantes sem leituras: WeatherFogColor, DEFAULT_TIMESCALE.
- 192 importações explícitas sem utilização. Importações usadas implicitamente por operadores/delegação foram preservadas.

## Dependências e catálogos

- app: androidx.tvprovider
- app: zxing.core
- app: juniversalchardet
- app: libmpv
- app: androidx.media3.exoplayer.hls
- app: androidx.media3.exoplayer.dash
- app: androidx.media3.datasource.okhttp
- core: androidx.sqlite.framework

Removidos 10 aliases de catálogo e 4 versões sem consumidores (libmpv, juniversalchardet, tvProvider e zxing no catálogo da app; versões mantidas nos módulos responsáveis). HLS, DASH, libmpv, FFmpeg, QR, charsets e integração do launcher continuam disponíveis nos módulos que os utilizam. A remoção de uma declaração duplicada não significa uma biblioteca a menos no APK. Framework SQLite não é o driver configurado; o driver BundledSQLite e a sua versão permanecem.

## Ficheiros e recuperação

43 artefactos de análise antigos retirados das raízes: logs de compilação/validação, recortes de código e temp_cls.jar. O componente OwnTVCard completa 44 ficheiros removidos. Não removidos caches de compilação, Gradle Wrapper, configuração local, schemas, traduções, bibliotecas nativas, licenças, planos ou testes.

Backup das fontes editadas e ficheiros removidos: `C:\Users\renat\Downloads\OwnTV-limpeza-backup-20261003-183233.zip`. O ZIP foi aberto e todos os conteúdos conferidos pelo SHA-256 do manifest.json antes da limpeza. Para recuperar, usar as correspondências de manifest.json; depois atualizar o inventário i18n.

## Validação

Compilação StandardDebug e verificações i18n concluídas. 1718 testes JVM sem falhas, erros ou ignorados: app: 307, core: 1006, player-core: 405. Compilação dos testes Android de migração concluída; não executados num dispositivo. Comparação antes/depois: mesmos 259 módulos/versionamentos runtime, sem bibliotecas adicionadas ou removidas. Redução líquida de 552 linhas Kotlin. Log final: %TEMP%/owntv-cleanup-validation-final.log. Não foi gerado APK nem executado teste físico.

## Limites

YouTube/trailers, filmes/séries e serviços de metadados ainda têm chamadas no código existente. Removê-los exige uma alteração funcional separada; não foram considerados mortos só porque parte da interface está oculta. Interfaces públicas do core e dados antigos/migrações foram preservados. Não se promete melhoria mensurável de FPS, arranque ou tamanho do APK por esta limpeza.
