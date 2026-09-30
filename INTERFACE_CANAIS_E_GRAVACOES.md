# Interface centrada em canais e gravações

## Implementado
- Filmes e séries deixam de aparecer na navegação estática ou dinâmica. Gravações continuam disponíveis mesmo sem uma lista online.
- Transferências passam a apresentar Gravações; os separadores de filmes e séries deixam de ser apresentados.
- Pesquisa, resumos de favoritos/histórico e o ecrã inicial deixam de apresentar catálogos de filmes/séries já guardados.
- Personalização, larguras dos painéis, menus de contexto e atalhos deixam de oferecer esses catálogos. Atalhos antigos e ligações do launcher não iniciam a sua reprodução.
- Formulários de fontes deixam de oferecer a importação desses catálogos. A sincronização fica limitada a canais; os valores antigos de modo TV em backups não reativam os catálogos.
- Tendências de filmes/séries deixam de ser carregadas. Os cartões antigos de continuar a ver no launcher são reconciliados quando a integração é atualizada.
- Retiradas as opções de reprodução automática de episódios e de layout/metadados dos catálogos. Mantidas as opções de vídeo gravado partilhadas com gravações.
- Revistos os textos visíveis afetados em português e inglês.

## Preservado
- Direto, guia EPG, canais em diferido/catch-up, avanço e retrocesso suportados pelo stream, gravação, agendamento e reprodução das gravações.
- Definições de reprodução, HLS, recuperação automática, timeout, modo simples e backups.
- Tabelas e dados antigos, sem migração destrutiva. Os componentes partilhados de reprodução e os identificadores legados permanecem no código para compatibilidade. Esta alteração remove as referências/acessos da interface; não elimina todos os módulos ou traduções antigas do repositório.

## Validação
- Compilação Kotlin da app concluída.
- 1202 testes passaram: app 180, Core 768 e player 254; zero falhas.
- Três testes de navegação verificam a ausência dos catálogos, a presença do guia e a acessibilidade das gravações sem canais online.
- Verificação de recursos Android executada após a última revisão dos textos.
- Não foi gerado APK. A apresentação e reprodução reais na Xiaomi/Thomson não foram testadas nesta sessão.

## Teste na box
1. Confirmar menus, pesquisa, definições e adição/edição de listas sem separadores de filmes/séries.
2. Abrir Gravações, reproduzir uma gravação e testar avanço/retrocesso.
3. No guia, abrir um programa em diferido e testar os comandos disponíveis.
4. Confirmar que a restauração de um backup antigo mantém o modo simples e o tempo de recuperação sem voltar a mostrar os catálogos.
