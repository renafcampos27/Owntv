# Matriz de alcance das definições — OwnTV TV

Revisão de 24/10/2026. Esta matriz liga a interface à persistência e ao consumidor. Testes JVM confirmam contratos e valores; não demonstram que um codec específico funciona numa box, que o comando tem o foco correto ou que o fornecedor aceita uma ligação.

## Percurso comum

`VideoPlayerSettingsScreen`/`VideoQuickRef` → `SettingsViewModel` → `SettingsRepository` (DataStore) → `PlaybackSettings.await`/`field` → `LiveTuneController`, `LivePreviewEngine` e `OwnTVPlayer`. O primeiro canal espera pela leitura das definições; as preferências por canal/lista continuam a sobrepor-se às globais nos pontos já existentes.

| Opção na interface | Persistência e consumidor | Evidência automática / limite |
|---|---|---|
| Hardware (`vp_hw`) | `hwDecoding`; snapshot → configuração do descodificador dos motores | `PlaybackSettingsReachabilityTest` testa alteração e retorno. Escolha efetiva depende do codec/dispositivo; exige box. |
| Preferir áudio por software (`vp_software_audio`) | `softwareAudio`; snapshot → política de seleção de descodificador de áudio | Reachability + `SoftwareAudioDecoderPolicyTest`; não garante que qualquer codec esteja disponível. |
| Surround (`vp_surround`) | `surroundMode`; snapshot → política de saída/renderers | Reachability confirma valor; PCM/passthrough/HDMI exigem teste físico. |
| Preferir SDR (`vp_prefer_sdr`) e HDR (`vp_hdr`) | Campos independentes no snapshot; seleção de faixa/configuração de imagem | `SdrVariantsTest` e contratos existentes. Preferência SDR não converte uma origem exclusivamente HDR. |
| Apenas HLS (`vp_hls_only`) | `liveHlsOnly`; snapshot → escada de sintonia e exceções por canal | Reachability recria repositório e arma escada estrita; `HlsOnlyPolicyTest` cobre política. |
| Motor (`vp_live_engine`) | `liveEnginePreference`; snapshot → `LiveTuneController` | Reabertura no teste mantém `EXO_ONLY`; players externos e codecs precisam de box. |
| Latência (`vp_live_latency`) | modo/custom → `liveBufferSeconds` | Reachability confirma 9 s independente da reserva. É objetivo face ao direto, não teto rígido de RAM. |
| Reserva (`vp_live_reserve`) | `liveReserveConfiguration`, par mínimo/extra coerente → load control | Reachability confirma 8–24 s após recriar repositório; testes `LiveReservePreferences`/resolver e `LiveBufferTest`. Segmento inteiro pode exceder limiar superior. |
| Pré-buffer (`vp_preroll`) | `livePrerollSecs`; snapshot → limiar inicial | Reachability confirma 5 s; retoma após quebra tem política separada, preservada. |
| Reabrir canal (`vp_reopen_interval`) | `channelReopenIntervalSecs`; snapshot → controlo de acesso/reabertura | Reachability confirma 25 s; testes do controlador cobrem cancelamento. Não certifica tempos de sessão do fornecedor. |
| Desistir do canal (`vp_tune_timeout`) | `liveTuneTimeoutSecs`; snapshot → orçamento de sintonia | Reachability confirma 19 s; não confundir espera deliberada para reabrir com falha de rede. |
| Atraso de áudio/saída (`vp_audio_output_route`, `vp_audio_output_delays`, `vp_audio_sync`) | `audioSyncOutput` + `audioOutputDelays` → `AudioRouteSync`, em fluxo próprio; atraso base no snapshot | Testes de rota/compensação existentes; deteção HDMI/Bluetooth e sincronismo real continuam físicos. Não resolve ruído analógico nem falta de segmentos. |
| EPG/desvio e correção dos diferidos | Repositório/fluxos EPG e construção de URL, fora do snapshot de vídeo | Preservada separação entre hora mostrada e correção de abertura; testes EPG/catchup existentes. Arquivo disponível depende do fornecedor. |
| Backup/restauro das preferências | `BackupManager`/validação/importação → repositório → mesmos consumidores | `CompleteTvSettingsBackupTest` e testes de identidade/validação. Restauro não substitui teste de efeito no motor. |

## Opções e compatibilidade preservadas

- Volume do leitor continua em 100%; volume do dispositivo é controlado pela box. Campos antigos de volume persistidos não foram apagados nem reintroduzidos na interface.
- Preferências globais não apagam ajustes por canal/lista. Não foram mudados os defaults de buffer, latência, reabertura, áudio ou HDR nesta intervenção.
- Filmes/séries, meteorologia, animações e funcionalidades Mobile não foram reintroduzidos por esta revisão.
- Definições de criação de PIN não são testes de descodificação. O contador de tentativas é comum à verificação de PIN, separado das definições de reprodução.

## Aceitação física ainda necessária

1. Alterar cada opção relevante, sair e reabrir, conferir o diagnóstico do canal; repetir com override por canal/lista e após restauro.
2. Xiaomi/Thomson e box com problema MPEG-L2: hardware/software, AAC/MPEG-L2, PCM/passthrough, HDMI/Bluetooth.
3. Zapping durante atualização/ordenação da lista, Home «No momento» com canais que entram/saem e entrada em timeshift local durante reprodução Exo.
4. LocalSync entre dois dispositivos: emparelhar, revogar sem reiniciar o servidor, cancelar transferência e confirmar que um backup completo continua restaurável.

O resultado das execuções locais está no relatório de subfases. Não há medição de percentagem de velocidade ou robustez nem validação física implícita.
