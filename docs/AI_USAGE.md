# Uso de IA

O desafio pede para contar como a IA foi usada: onde ajudou, onde errou e foi corrigida, e o que foi decidido sem ela. Este registro foi escrito ao longo do trabalho, com data, e não reconstruído no fim.

## Como a IA foi conduzida

- **Ferramenta:** Claude Code (Claude Opus 5.5), no terminal e no app de desktop. Todo commit leva o rodapé `Co-Authored-By` da IA.
- **Passos pequenos, um commit por passo.** Antes de cada passo, a IA explicava o que ia fazer; eu revisava e aprovava. Nenhum commit foi feito sem eu ler a mensagem antes.
- **Nada entra sem verificação.** Cada passo passou por build, testes, ktlint e Android Lint, e as regras de negócio por teste de mutação manual: a regra é quebrada de propósito e algum teste precisa falhar. As telas foram validadas no emulador de tablet, celular e TV antes de cada PR.
- **Portões que não dependem de ninguém lembrar:** um hook de commit versionado e o CI em todo PR barram mensagem fora do padrão, avisos de Lint e teste quebrado, inclusive quando quem errou foi a IA.

## Onde a IA ajudou

- **Ler o código que não era meu.** A leitura do `server.js` levantou as armadilhas do mock antes de escrever o app: datas em UTC sem "Z", `PATCH` aceitando qualquer transição, SSE sem heartbeat, eventos duplicados e eco sem número de versão, pedidos encerrados vindo junto com os ativos.
- **Estender o mock com cuidado:** `version`, validação de transições com 409, heartbeat e `PATCH` idempotente, com a tabela no README do mock.
- **Validar versões antes de usar.** Um projeto descartável com as versões candidatas foi compilado de verdade antes de montar o projeto; foi isso que mostrou que o detekt estável não roda com o Kotlin atual.
- **Apresentar alternativas com custo** para cada decisão (SSE, desfazer, injeção de dependência, escolha da tela), para eu decidir.
- **Escrever código e testes** seguindo o padrão combinado, e rodar o teste de mutação em cada regra.
- **Investigar no aparelho:** `dumpsys` de áudio, vibração, energia e quadros para provar que o som tocou, que a vibração saiu no padrão certo, que a tela não apaga e quão fluido o board fica no pico.

## Onde a IA errou e como foi corrigido

28/09:

- **Propôs filtrar pedidos encerrados no mock.** Revendo, isso esconderia um cancelamento ocorrido durante a queda de rede, que só aparece no `snapshot` da reconexão. Revertido antes de implementar.
- **Esqueceu `READY → CANCELED`** na tabela de transições; percebido ao reler o cancelamento automático do mock.
- **Apagou arquivos por um comando que falhou em silêncio:** um `git mv -k` em arquivos ainda não versionados não moveu nada, e o `rm -rf` seguinte apagou os fontes do `build-logic`. Recriados e validados com build limpo. Regra adotada: conferir o resultado do passo anterior antes de qualquer remoção.
- **Título de commit com 74 caracteres**, barrado pelo hook criado no próprio projeto.
- **Usou `!!` no reducer.** Reestruturado para o compilador provar que o valor não é nulo.
- **Teste de estouro do backoff que não testava nada:** usava a tentativa 10.000, que não reproduz o problema (o deslocamento de bits usa só 6 bits). A mutação revelou; o teste passou a cobrir as tentativas de 6 a 200, e a falha real aparecia na 64.
- **Testes faltando no `OrderStore`:** a mutação mostrou que "desfazer cancela o envio" e "toque duplo não agenda outro envio" não eram testados. Investigando, havia um bug latente: tocar, desfazer e tocar de novo faria o envio do primeiro toque sair antes da janela do segundo. Corrigido com teste, e um trecho morto que a mutação também revelou foi removido.
- **Copiar o projeto de referência sem conferir a versão** teria quebrado o build: a assinatura `CommonExtension<*,*,*,*,*,*>` não existe no AGP 9.

29/09:

- **Pedido escondido acima do topo da lista.** Com todos os testes verdes, a validação no emulador mostrou que um pedido que volta para a fila (desfazer, falha de envio, reconexão) ficava fora da tela, porque a `LazyColumn` ancora o primeiro item visível. Nem a IA nem os testes tinham previsto. Corrigido em TDD, com o teste do lado oposto (quem rolou para baixo não é puxado para o topo).
- **Conclusão apressada no emulador:** a primeira tentativa de reproduzir o desfazer falhou por causa do script (as capturas de tela demoravam mais que a janela de 5 s), não do app. Conferido pelo print antes de concluir.
- **Teste reprovado pelo Lint:** um estado criado dentro da composição num teste da IA.
- **Testes fracos nos avisos sonoros:** deixavam passar "comparar a quantidade de pedidos em vez dos ids" (pedido novo perdido quando outro sai na mesma atualização) e "alarme repetido enquanto o alerta está na tela". A mutação revelou; os testes foram criados.
- **Contagem errada do título de um commit:** a IA anunciou 58 caracteres, e eram 66. Percebido ao conferir antes de enviar a mensagem.
- **Componentes que quebravam no celular.** Ao levar a interface compartilhada para o `core/ui` e testar na largura do celular, o alerta de cancelamento e a barra de desfazer espremiam o texto até sumir (botão fixo de 240 dp ao lado). Corrigido com layout empilhado abaixo de 600 dp.
- **ENTREGUE sem desfazer.** A barra de desfazer era montada a partir da etapa de destino, e "entregue" não tem coluna; o toque mais difícil de reverter ficava sem desfazer, também no board. Teste falhando antes da correção e conferência no emulador.
- **Comandos errados passados para mim:** a IA me deu comandos para mover pedidos no mock que filtravam o objeto `{ pagination, orders }` em vez da lista, e o `PATCH` ia para `/orders/` com 404. Percebido quando a própria IA rodou os comandos; as versões corretas foram reenviadas.
- **Banner da TV reprovado pelo Lint:** um vetor de 320 dp é lento de desenhar. Refeito em camadas, reaproveitando o ícone do app.
- **Documento prometendo um teste que não existia:** o SPECS dizia que "o teste de carga usa o maior", e não havia teste de carga. Corrigido, e o pico passou a ser medido de verdade.
- **Leitura errada da medição:** com o build debug, a IA concluiu que "a carga não piora a fluidez". Por sugestão minha, medimos também o build release, e a conclusão mudou: no release, a carga aumenta os quadros lentos de 13,5% para 19,2%, sem travadas longas. O texto foi corrigido nos dois documentos.
- **Descrição errada dos testes no `ARCHITECTURE.md`:** dizia que os fakes ficavam em `testFixtures`, onde estão só as fábricas de pedidos. Corrigido na conferência do documento contra o código, antes do commit.
- **Build travado por arquivo em uso:** o `classes.jar` de um módulo ficou bloqueado. A primeira suspeita da IA foi o Android Studio aberto; fechá-lo não resolveu, e a causa real era um daemon do Gradle que ele deixou vivo. `gradlew --stop` liberou o arquivo, sem perder nada.

## O que eu decidi

- **Kotlin nativo com Compose**, a stack que domino e a preferida pela Pigz para um app de tablet.
- **Hilt em vez de Koin.** Uso Koin no dia a dia; escolhi o Hilt pela validação do grafo na compilação, já que o app roda sozinho no tablet durante o pico.
- **Estrutura modular** e o padrão de construção de um projeto de referência que eu já conhecia, adaptado: versões atuais, menos módulos, sem camadas que só repassam chamadas.
- **Desfazer por envio adiado**, entre as opções apresentadas, aceitando o custo de perder o toque se o app morrer na janela.
- **Regras de negócio confirmadas:** faixas de atraso de 8 e 15 minutos, "COBRAR" para pedido não pago, um tablet na montagem e pronto por pedido.
- **Nada de números mágicos nem textos fixos:** medidas em tokens do design system e textos em `strings.xml`, exigência minha revisando o código gerado.
- **Densidade do board mantida** em cerca de um card e meio por coluna, priorizando a leitura a 2 metros.
- **TV sem alerta de cancelamento**, aceitando a recomendação e o motivo.
- **Medir o release**, e não só o debug, o que corrigiu a conclusão sobre o pico.
- **Tema claro.** Olhando o app como usuário, achei estranho ser todo escuro. A IA defendeu o escuro como padrão de cozinha e propôs oferecer os dois temas seguindo o Android; aprovei a direção depois de ver o card no tema claro, antes de qualquer código.
- **Polimento visual sem Figma.** Perguntei se valia montar um Figma; a IA recomendou polir direto no Compose, cada item ligado a uma dor da cozinha e com teste (itens agrupados, faixa de atraso, barra superior, animação). Escolhi esse pacote.
- **Forma de trabalhar:** passos pequenos, mensagem de commit revisada antes, Conventional Commits formais em português, PR com merge commit para preservar o histórico.
