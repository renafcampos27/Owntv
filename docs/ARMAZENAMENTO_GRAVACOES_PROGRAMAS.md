# Armazenamento de gravações e programas guardados

Implementação local de 2026-10-01, complementar às fases 5 e 6. Projetos: OwnTV-main e OwnTV_Core. A versão não foi alterada; não foi gerado APK nem publicado código.

## Utilização

Em **Definições → Gravações**:

| Opção | Predefinição | Intervalo |
|---|---:|---:|
| Destino de gravações e programas guardados | Pasta interna da app | Pasta interna ou USB local montado |
| Limite no armazenamento interno | 4 GiB | 1–32 GiB |
| Limite no armazenamento USB | 16 GiB | 1–256 GiB |

Os valores são limites, não espaço pré-alocado nem promessa de capacidade disponível. Num disco comercial de 256 GB, a capacidade em GiB é inferior a 256. O limite útil também depende de outros ficheiros, reserva livre e espaço temporário.

A quota conta conteúdos registados de todos os perfis no mesmo volume. USB diferentes têm contagem própria e usam o limite USB definido. Outros ficheiros da box não contam como conteúdos OwnTV, mas reduzem o espaço livre real. Mudar de pasta aplica-se às capturas seguintes; as existentes mantêm o caminho original. Reduzir uma quota não apaga conteúdos: bloqueia o crescimento acima do novo limite.

No **EPG → programa já terminado → Guardar programa**, a app solicita o arquivo do fornecedor e acompanha o resultado em **Gravações**. O pedido tem confirmação ou aviso de indisponibilidade. Pode tentar programas até ao limite de histórico já suportado, mesmo quando a metadata de retenção está desatualizada; isso não garante que o fornecedor ainda conserve o programa. Não existe fallback implementado para gravar o direto quando a resolução do arquivo falha.

Até uma captura de arquivo por fonte é admitida de cada vez. Continua a respeitar o orçamento de ligações da conta e a opção de reservar uma ligação para ver televisão. Não adiciona uma fila ilimitada de downloads de arquivo.

## Proteções

- Interno: reserva pelo menos **3 GiB ou 10% da capacidade real**, prevalecendo o maior.
- USB: reserva pelo menos **1 GiB ou 5% da capacidade real**, prevalecendo o maior.
- Contagem e admissão da quota são partilhadas por gravações e transferências. Reservas de escritores ativos não são substituídas por progresso antigo da base de dados.
- A montagem e quota são reavaliadas durante a escrita. Sem destino acessível, a captura termina com motivo de armazenamento; não é desviada silenciosamente para o interno.
- HLS prepara segmentos no volume de destino. Em destinos SAF/USB usa uma pasta da app no mesmo volume, ou recusa a captura se isso não for possível. Conta espaço adicional para montagem/remux/cópia final.
- DASH com faixas separadas também prepara no volume de destino; os seus dados são conservados se a junção falhar.
- Quando falta quota ou espaço, os conteúdos confirmados permanecem. Se o ficheiro não puder ser finalizado, conservar os segmentos não significa que já exista um ficheiro pronto a reproduzir.
- A deteção de FAT32 tenta observar também o volume físico por baixo de FUSE. Mantém margem abaixo do limite por ficheiro e trata erros de ficheiro demasiado grande como armazenamento, sem reconexões de rede inúteis. Não há divisão automática em ficheiros de várias partes neste lote. Filesystems não identificáveis dependem do erro real reportado pelo Android.
- Apagar uma gravação remove os segmentos/trilhas que lhe pertencem antes de retirar o registo. Um destino inacessível mantém o registo para não perder a referência aos ficheiros.
- As novas capturas incluem a identidade de perfil/canal/programa no nome para evitar colisões entre gravações com nomes e minutos iguais. Os ficheiros antigos não são renomeados.

O recuo local temporário continua separado, com os limites anteriores de 256 MiB/30 minutos. Passa a usar a reserva física interna acima. Não transforma gravações permanentes em buffer circular.

## Persistência e custo

O destino e os limites ficam nas preferências persistentes e na secção Definições do backup. O backup não transporta vídeos, permissões SAF nem a montagem de um USB: depois de restaurar, um destino inválido deve ser escolhido novamente.

O estado de armazenamento só é observado enquanto o ecrã o utiliza. A capacidade é atualizada aproximadamente a cada dois segundos; o inventário é reconciliado no máximo uma vez a cada cinco segundos, ou antes de admitir um escritor. Consultas de volume são reutilizadas por pasta/volume durante essa reconciliação. O progresso durável evita percorrer milhares de segmentos HLS em cada atualização. O trabalho de disco e base de dados corre fora da interface.

## Validação

Compilação app/Core/player-core e verificações de traduções passaram. **1 434 testes**: app 228, Core 896, player-core 310; zero falhas, erros ou testes ignorados. Doze testes novos cobrem disputa concorrente, quota entre perfis, contagem na retoma, conservação parcial, redução de limite, duplicação de escritores, volumes separados, reservas físicas, overflow de metadata e colisões de nomes. Log: `storage-validation-final.log`.

Não houve ensaio nesta Xiaomi/Thomson, USB/SAF real, fornecedor IPTV, decoder ou superfície Android TV. Estes testes não demonstram a montagem do USB, permissões OEM, duração real de uma gravação nem integridade audiovisual no hardware.

## Ensaio na box

1. Escolher interno e fazer uma gravação curta. Confirmar progresso, ficheiro final e reprodução pela biblioteca.
2. Escolher USB e repetir com um canal HLS. Confirmar ficheiro e consumo no USB, incluindo preparação temporária, sem consumo interno crescente.
3. Guardar um programa terminado no EPG; verificar que é o programa solicitado. Repetir com um programa que o fornecedor já não serve: deve falhar sem abrir o direto como substituto.
4. Iniciar uma segunda captura de arquivo na mesma fonte e verificar a recusa. Testar a reserva de ligação enquanto se vê outro canal.
5. Reduzir a quota abaixo dos conteúdos existentes: nada deve ser eliminado e nova escrita deve ser recusada. Aumentar o limite e voltar a tentar.
6. Remover o USB durante a captura. Confirmar motivo de armazenamento, ausência de fallback e conservação do registo. Voltar a ligar e verificar os ficheiros antes de decidir recuperar/apagar.
7. Testar uma interrupção de finalização, a paragem manual e a exclusão: o progresso parcial não deve passar incorretamente a zero, e os ficheiros conservados devem continuar a contar.
8. Reiniciar a box e restaurar um backup. Confirmar limites/destino, e reescolher a pasta se o dispositivo ou permissões mudaram.
9. Em FAT32, testar o limite por ficheiro; em exFAT/ext4, testar uma gravação maior apenas se houver capacidade, quota e suporte efetivo da box.
