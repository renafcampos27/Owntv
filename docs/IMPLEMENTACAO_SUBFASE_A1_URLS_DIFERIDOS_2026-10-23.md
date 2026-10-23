# OwnTV TV — subfase A1: URLs de diferidos e credenciais

**Data:** 23/10/2026. **Autorização:** primeira subfase do plano reavaliado; âmbito limitado a **R1 / DAT-03**. As restantes subfases não foram iniciadas.

Plano: [reanálise do council](../implementar_rena/REANALISE_COUNCIL_PLANO_2026-10-23.md). Esta implementação fecha a lacuna de construção de URL identificada em R1, sem reaplicar o bloco DAT-03 inteiro.

## Problema e comportamento corrigido

O builder principal Xtream já codificava corretamente as credenciais. Porém, a conversão de uma URL de arquivo para `streaming/timeshift.php` e a construção desse endpoint a partir de um canal M3U `xc/xtream` ainda interpolavam segmentos do path diretamente na query. Um `&` podia separar parâmetros e um `+` podia passar a espaço. A conversão M3U também descartava a query existente.

- Os dois percursos passam a ler os valores lógicos dos segmentos com HttpUrl e a construir os parâmetros com o builder de query.
- Cada segmento é decodificado uma vez. Uma credencial que contém literalmente `%2F` conserva esse texto; uma barra codificada continua parte da credencial, sem criar outro segmento.
- São conservados os tokens de query já codificados, incluindo ordem, duplicados, flags, valores vazios e grafia de percent-encoding.
- O fallback de arquivo preserva subdiretório e porta do fornecedor; M3U conserva os formatos de path reconhecidos, com `/live/` ou compacto.
- A grafia anterior dos dois pontos do timestamp é preservada. Fuso, duração e correção de diferidos continuam nos mesmos pontos do código, sem nova soma de +1 h.
- O conversor PHP só aceita endereços HTTP(S) reconhecidos. Não cria alternativas para ficheiros ou URLs que não correspondem aos percursos suportados.

Implementação: `C:/Users/renat/Downloads/OwnTV_Core/core/src/main/java/tv/own/owntv/core/epg/CatchupUrl.kt`. Novo teste: `core/src/test/java/tv/own/owntv/core/epg/CatchupCredentialUrlTest.kt`. O inventário técnico de literais foi atualizado apenas para esse ficheiro; o baseline i18n ficou idêntico ao anterior.

Não foram alteradas as opções de formato/fallback, HLS obrigatório, motores, áudio, buffer/latência, intervalo de reabertura, gravações, esquema 51, dados instalados ou OwnTV Mobile. Não foi gerado APK, alterada assinatura, feito commit, push ou publicação.

## Validação automática

- Antes da correção, **seis dos sete novos testes falharam**, reproduzindo a falha de credenciais/tokens e a aceitação indevida de um esquema não HTTP pelo conversor.
- A primeira execução alargada detetou mudança de grafia do timestamp (`:` para `%3A`). O código foi ajustado para preservar o formato anterior; os testes existentes não foram enfraquecidos ou alterados.
- Execução final do Core: **1 367 testes JVM**, zero falhas/erros/ignorados; inclui os **sete testes novos**. Contagem extraída dos relatórios XML desta execução.
- Os testes verificam `&`, `+`, `%`, `/`, `?`, `#`, percent-encoding literal, Unicode, credenciais recebidas logicamente, tokens repetidos, subdiretório/porta, caminhos M3U compactos, passagem por CatchupFormats, HLS restrito e horário com fuso/correção aplicado uma vez.
- Verificação i18n aprovada, sem aumento do baseline e sem alterações semânticas no inventário de outros ficheiros.
- **Seis testes da ferramenta de snapshot aprovados.**
- Compilação App debug/release e lint vital de release: **em validação no momento de criação deste registo**; atualizar antes do fecho.
- Verificação de whitespace do diff aprovada com CR de fim de linha reconhecido, preservando os fins de linha preexistentes do Windows.

Logs desta intervenção: `%TEMP%/owntv-subphase-A1-red.log`, `owntv-subphase-A1-core.log`, `owntv-subphase-A1-app.log`. A passagem intermédia com a diferença de timestamp está em `owntv-subphase-A1-green.log` e não representa o resultado final.

## Integridade e recuperação

- Antes de alterar: ZIP/lock e **1 061 inputs** conferidos com o Core externo, sem diferenças.
- Cópia anterior verificada: `C:/Users/renat/Downloads/OwnTV-subfase-A1-backup-20261023-163539/`, com manifesto SHA-256, fonte anterior, inventário técnico anterior, documentação, lock e ZIP anteriores.
- Novo snapshot: **1 062 inputs**, SHA-256 `d6c9d4b67ff4b791f10d91926e1d6694ba986a7b77a368937166543bdc7df4b5`.
- Novo ZIP, lock, inventário e bytes conferidos com o Core externo. Relativamente ao snapshot anterior, há apenas um novo teste e alterações em CatchupUrl e no inventário técnico dos seus literais. Nenhum input removido; baseline i18n preservado.
- O snapshot anterior foi mantido. A App resolve o novo lock na configuração normal, sem editar a cache gerada nem substituir o Core personalizado por upstream.

## Aceitação física e próximo trabalho

Falta gerar/instalar o APK e testar programas em diferido na box com a lista real. Os testes de URL são locais e usam credenciais sintéticas; não demonstram que o fornecedor aceita cada endpoint nem resolvem, por si só, todos os HTTP 404 ou limites de sessão.

**Próxima subfase proposta, ainda não iniciada: A2 — R4 / PLY-07**, invalidar o índice de zapping quando o catálogo muda. Depois A3 — R3, confirmação Exo→timeshift local; A4 — R5, seleção visível em «No momento». Integridade residual de backup e invalidação de aprendizagem ficam no lote B. A sequência não autoriza automaticamente a implementação seguinte.
