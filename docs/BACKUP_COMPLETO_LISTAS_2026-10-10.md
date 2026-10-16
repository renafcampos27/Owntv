# Backup completo das listas — 10/10/2026

A exportação local e a exportação para outro dispositivo exigem agora uma palavra-passe de backup. Deixa de ser possível escolher exportar sem credenciais na interface da OwnTV TV; os dois métodos de exportação do ViewModel também recusam uma palavra-passe vazia. A infraestrutura partilhada do Core mantém compatibilidade com os seus outros consumidores e com backups antigos.

Para guardar tudo, selecionar os perfis pretendidos e manter todas as secções selecionadas. Apenas as listas ligadas aos perfis selecionados são exportadas.

Incluído:

- Nome, tipo, URL, utilizador, palavra-passe e identidade Stalker quando aplicável.
- User-Agent, Referer e origem EPG.
- Intervalo de atualização de cada lista, atualizado com os IDs correspondentes no restauro.
- Lista predefinida e associações aos perfis.
- Preferências de HLS, motor, pré-buffer, latência, reserva, timeout e correção dos diferidos.
- Com as restantes secções selecionadas: configurações gerais, modo simples, personalizações, ordenação e configurações por canal, incluindo endereços alternativos de arquivo.

A palavra-passe escolhida protege o ficheiro completo e os campos secretos. É necessária para restaurar o backup; não é a palavra-passe do serviço IPTV.

O restauro respeita também um User-Agent ou uma origem EPG explicitamente vazios no backup, conservando os valores atuais apenas quando o campo não existe num backup antigo.

As listas de canais continuam a ser atualizadas a partir do fornecedor após o restauro, conforme a escolha anterior do utilizador. O backup não arquiva o catálogo de canais nem transfere os ficheiros de gravação. Backups antigos sem credenciais continuam legíveis, mas não permitem recuperar segredos que não continham.

Validação: compilação Kotlin Standard Release, testes da app (329) e do Core (1 145), lint vital Standard Release e inventários de internacionalização passaram. Dois testes novos verificam todos os campos persistidos da lista e a preservação do intervalo de atualização ao remapear IDs. Não foi gerado APK nem realizado um restauro numa box física.