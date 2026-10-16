# Fase 1 — reprodução, áudio e retry de diferidos

Data: 11/10/2026. Âmbito: App TV e Core personalizado; Mobile intocado.

## Alterações

- A alteração de Surround identifica o motor ativo. ExoPlayer é reconstruído pelo seu percurso de superfície nova; mpv só recebe o recarregamento quando é proprietário da reprodução. O volume permanece a 100%.
- A reconstrução Exo pendente é cancelada ao substituir uma reconstrução, mudar de item ou desativar Exo. Um pedido de abertura já publicado é limpo antes de esperar pela nova reconstrução, impedindo uma superfície atrasada de consumir esse pedido antigo.
- A posição, intenção atual de pausa/reprodução e seleção de legendas são transportadas pelo percurso Exo. A seleção de áudio também é conservada na reconstrução de Surround quando a faixa continua disponível.
- Retry conserva cópias das alternativas explícitas e mapa de tipos do diferido. Os caminhos de URL direto e renovado recebem esse contexto; o cancelamento/geração existente impede que uma renovação antiga substitua outro item. A intenção de pausa é lida no momento de abrir, incluindo alterações durante a renovação.
- O adaptador Live→mpv transmite o manifest declarado. Probe e opções de reconexão partilham a mesma classificação HLS, usando declaração, extensão e políticas já existentes. HLS declarado num URL `.ts` não recebe a reconexão em EOF destinada a TS contínuo. Strict/host aprendido continuam limitados a live.
- Inventário i18n atualizado para a redução das ocorrências locais do literal de extensão. Não foi desativada nenhuma verificação.

## Validação

Validação integrada concluída: **BUILD SUCCESSFUL em 3 min 32 s**.

- Leitor: **516 testes**, zero falhas/erros/ignorados; 18 testes novos nesta fase.
- Core/player compilados nas variantes necessárias à validação.
- App: `compileStandardReleaseKotlin` e `lintVitalStandardRelease` aprovados, sem erros bloqueantes.
- Verificação i18n integrada aprovada. Permanecem os avisos existentes de funcionalidades Gradle descontinuadas; esta fase não migra o projeto para Gradle 10.
- Snapshot personalizado atualizado e usado na validação: 947 ficheiros; SHA-256 `3544b442dd64dde984c7a027d317ec7302c3aafdde96c9b29e5bb7e05421d925`.
- Log: `C:\Users\renat\AppData\Local\Temp\owntv-phase1-validation.log`.

Os testes de contexto de retry e formato verificam preservação de alternativas/tipos, cópias independentes e isolamento live/arquivo, HLS declarado sem extensão ou com extensão TS, TS verdadeiro, âmbito de HLS exclusivo e política de EOF. Os testes de retenção de áudio verificam identidade por ordinal/idioma/label, reordenação e ausência/ambiguidade sem forçar uma faixa diferente.

## Validação necessária em Android TV

1. Diferido/gravação em Exo, depois de mpv ter sido usado: mudar Stereo/Auto; confirmar motor Exo, som, posição e volume da box.
2. Repetir em pausa e alterar pausa/reprodução durante a reconstrução.
3. Fazer duas alterações de Surround rapidamente e sair/mudar de item durante a espera: nenhuma reabertura antiga nem reprodução mpv concorrente.
4. Selecionar outra faixa de áudio, mudar Surround e confirmar a faixa quando disponível.
5. Retry de diferido com alternativas tipadas; confirmar que a tentativa conserva o programa e não regressa ao direto.

Não foi gerado APK, feito commit/push nem iniciada a Fase 2. Compilação e testes JVM não demonstram comportamento de MediaCodec/AudioTrack/superfícies em boxes físicas, nem eliminam limitações do fornecedor.
