# Validar desempenho OwnTV em TV e smartphone

Os percursos de Baseline Profile e Macrobenchmark agora verificam destinos por identificadores estáveis, independentes do idioma. Incluem lista de canais, scroll por D-pad ou toque, reprodução confirmada pelo player, zapping por setas na TV, regresso à lista, EPG e gravações. Um assistente inicial, lista vazia, canal sem reprodução ou destino não alcançado faz o teste falhar.

## Preparação sem credenciais IPTV

1. Ter `ffmpeg` disponível no PATH para gerar vídeo sintético. O script não instala dependências.
2. Criar uma pasta nova e dedicada, por exemplo:

```powershell
python tools/performance/local_iptv_fixture.py create C:\Users\renat\AppData\Local\Temp\owntv-iptv-fixture
python tools/performance/local_iptv_fixture.py serve C:\Users\renat\AppData\Local\Temp\owntv-iptv-fixture
```

3. Num emulador Android, adicionar `http://10.0.2.2:8765/channels.m3u`. A M3U aponta também para `epg.xml`; atualizar o guia se necessário. O servidor só escuta no loopback do computador.
4. Num dispositivo físico, criar outra fixture com `--base-url http://127.0.0.1:8765` e usar `adb reverse tcp:8765 tcp:8765`. Adicionar `http://127.0.0.1:8765/channels.m3u` no dispositivo. Não são necessários servidor IPTV, utilizador ou senha.
5. Concluir configuração e manter navegação Live/Guia/Gravações visível para o percurso completo. EPG deve estar carregado. Pode ocultar as categorias para medir a lista simples mantendo a navegação entre destinos.

A fixture tem 240 canais e EPG sintéticos e HLS de 180 segundos. Serve para medir UI, arranque, abertura e zapping, não qualidade do fornecedor, duração ilimitada de live, passthrough/HDR, catch-up Xtream ou pausa durante 30–60 minutos. Após passar o tempo do vídeo, voltar a abri-lo reinicia-o. Não usar streams reais como fixture pública.

## Perfil e benchmark

O módulo de gravação utiliza o flavor x86_64 do projeto. Usar um emulador TV API 33+ e depois um emulador smartphone API 33+, preparados com a fixture. O perfil contém caminhos de código e não código máquina específico da ABI.

```powershell
.\gradlew.bat :app:generateBaselineProfile
.\gradlew.bat :baselineprofile:connectedBenchmarkReleaseAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=tv.own.owntv.baselineprofile.IptvPerformanceBenchmark
```

Estes comandos de medição instalam APKs no dispositivo, ao contrário da verificação de compilação feita na implementação. Executar apenas quando se quiser gerar e medir. O gerador escolhe D-pad ou toque conforme as capacidades do dispositivo. A compilação dos testes não significa que um perfil novo foi gravado: conferir o resultado gerado em `app/src/main/generated/baselineProfiles/` e a sua inclusão no release.

## Comparação

Medir release antes/depois no mesmo aparelho, mesma fonte, rede e configuração. Guardar StartupTimingMetric, FrameTimingMetric, PSS, tempo até imagem e erros. Cinco iterações por percurso são o mínimo configurado. O Macrobenchmark é medição; não substituir percentis de frames por uma impressão visual.

Na Xiaomi/Thomson fazer também 100 mudanças A/B/C e regressos a canais, 30–60 minutos num canal saudável, gravação em paralelo e Home/retoma. No smartphone acrescentar rotação, Wi-Fi→dados móveis, teclado, fontes grandes e janela dividida. A fixture não demonstra libertação de sessões no servidor real.

## Diagnóstico de cortes

Ativar registo detalhado apenas durante a recolha. Correlacionar `audio_underrun`, `audio_output_created/released`, formato/codec, tempos HTTP/segmentos, reserva disponível e estado da superfície pela mesma sintonia/fonte. `first_frame_metadata` significa envio para renderização; `first_frame` vem do callback de primeiro frame do renderer. Nenhum deles certifica apresentação física no painel.

Volume interno permanece a 100%. Buffer, qualidade e número de threads não devem ser alterados automaticamente com base apenas num corte observado. Perfil térmico e prioridades de I/O exigem medição no aparelho antes de mudar o comportamento.
