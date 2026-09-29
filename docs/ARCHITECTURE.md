# Arquitetura e decisões

Este documento explica como o KDS está organizado e, principalmente, por que cada decisão foi tomada e o que ela custa. Os requisitos e as premissas estão em [SPECS.md](SPECS.md); o uso de IA, em [AI_USAGE.md](AI_USAGE.md).

## Módulos

```
app ................. MainActivity, escolha da tela pelo aparelho, configuração do servidor
feature/board ....... board do tablet e painel de TV somente leitura
feature/expedition .. tela do garçom no celular
core/ui ............. interface compartilhada pelas telas da cozinha
core/designsystem ... tema, tokens e componentes visuais sem regra de negócio
core/data ........... SSE, REST, DTOs, mapeamento e reconexão
core/domain ......... Kotlin puro: pedido, máquina de estados, reducer, store e regras
build-logic ......... plugins de convenção do Gradle
```

Dependências (quem depende de quem):

```
app -> feature/board, feature/expedition, core/data, core/designsystem, core/domain
feature/* -> core/ui, core/designsystem, core/domain
core/ui -> core/designsystem, core/domain
core/data -> core/domain
core/domain -> nada do Android
```

- **`core/domain` é um módulo JVM, sem Android.** O compilador garante a regra de dependência: nenhuma regra de negócio consegue importar uma `View` ou um `Context`. Os 110 testes do domínio rodam na JVM em segundos.
- **`core/ui` nasceu com a Expedição.** A faixa de conexão, a barra de desfazer, o alerta de cancelamento, o relógio e os sons estavam no board; quando a segunda tela precisou deles, foram movidos para um módulo comum em vez de uma feature depender da outra.
- **Sem camada de UseCase.** As regras estão no `StageMachine`, no `OrderReducer` e no `OrderStore`; um UseCase que só repassasse a chamada ao store seria uma camada a mais sem regra nenhuma.

## O caminho de um pedido

Do servidor à tela:

1. O mock empurra eventos por SSE: `snapshot` ao conectar, depois `order.created` e `order.updated`.
2. `SseOrderConnection` transforma o callback do OkHttp num `Flow` (`callbackFlow`). Com o consumidor lento, a leitura espera (`trySendBlocking`) em vez de descartar um pedido.
3. `ReconnectingOrderStream` mantém a conexão viva: a cada queda espera o backoff e conecta de novo.
4. `OrderStore`, iniciado pela `KdsApplication`, converte cada evento num evento do reducer e guarda o resultado num `StateFlow<KitchenState>`. Recebe pedidos enquanto o app estiver aberto, qualquer que seja a tela.
5. `OrderReducer` é uma função pura `(estado, evento) -> estado`: aplica a regra de `version`, as transições otimistas e os alertas de cancelamento.
6. Cada ViewModel combina estado e conexão, mapeia para o modelo da tela fora da thread principal (`flowOn(Dispatchers.Default)`) e expõe um `StateFlow` com `WhileSubscribed(5 s)`, que sobrevive à rotação sem refazer o mapeamento.
7. O Compose coleta com `collectAsStateWithLifecycle`.

Do toque ao servidor:

1. O toque chama `OrderStore.advance`: o card muda de coluna na hora (transição otimista) e o envio é agendado.
2. Depois de 5 s sem "Desfazer", sai o `PATCH`.
3. Resposta 200 confirma; 409 traz o pedido como está no servidor, que substitui o otimismo, com aviso; falha de rede devolve o card e avisa "não foi enviado".

## Tempo real: por que SSE

| Opção | Por que não, ou por que sim |
|---|---|
| Polling | Latência igual ao intervalo e carga constante no servidor; cada resposta traz a lista inteira e exige comparar tudo de novo |
| WebSocket | Canal nos dois sentidos, mas a cozinha só precisa receber; as ações são poucas e cabem em REST. O mock não oferece |
| **SSE** | O mock já oferece, o fluxo é exatamente servidor para cliente, roda sobre HTTP comum e as ações seguem por `PATCH` |

O que costuma ser esquecido, e como foi tratado:

- **O OkHttp SSE não reconecta sozinho.** O `retry: 3000` do servidor é uma dica para o `EventSource` do navegador; no Android ninguém a lê. O `ReconnectingOrderStream` reconecta com backoff exponencial de 1 s a 30 s, com jitter entre 50% e 100% do valor, para tablet, celulares e TV não reconectarem todos no mesmo instante quando o Wi-Fi volta. A contagem zera a cada conexão bem-sucedida, e se a rede do aparelho volta durante a espera, a reconexão é imediata.
- **Conexão muda.** Um roteador que perde a rota pode deixar o socket aberto sem entregar nada, e o app pareceria conectado para sempre. O mock ganhou um heartbeat (`: ping` a cada 15 s) e o cliente do stream usa `readTimeout` de três heartbeats (45 s): sem nenhum byte nesse tempo, a conexão é dada como morta e reconecta. O cliente do stream não tem `callTimeout`, que derrubaria uma conexão saudável.
- **Rede local sem internet.** O monitor de rede exige só `NET_CAPABILITY_INTERNET`, sem `VALIDATED`: uma cozinha com o servidor na rede local e sem saída para a internet seria vista como sempre offline.

## Evento repetido, fora de ordem e eco: `version` e reducer

O mock original tinha três fontes de duplicidade: o `snapshot` e um `order.created` do mesmo pedido, o eco do próprio `PATCH` e o `PATCH` para a etapa atual, que também gerava evento. Sem número de versão, não havia como saber qual informação era a mais nova.

- **O mock ganhou `version`**, incrementada a cada alteração. `PATCH` para a etapa atual responde 200 sem mudar a versão e sem evento.
- **O reducer só aplica versão maior que a conhecida.** Evento repetido, fora de ordem ou eco não altera nada; a mesma entrada duas vezes devolve o mesmo estado. É o ponto que torna o resto do sistema simples.
- **Reconciliação na reconexão.** O `snapshot` passa pelo mesmo reducer: pedidos criados durante a queda aparecem, um cancelamento ocorrido na queda vira alerta, e um toque ainda pendente sobrevive se continuar válido.
- **O mock não filtra pedidos encerrados** (o README dele diz "ativos", mas o código envia todos). A decisão foi manter assim e filtrar no cliente: só vendo o pedido `CANCELED` no `snapshot` o app consegue alertar um cancelamento que aconteceu enquanto estava offline.

## Ciclo de vida: `StageMachine`

| De | Para |
|---|---|
| PENDING | CONFIRMED, PREPARING, CANCELED |
| CONFIRMED | PREPARING, CANCELED |
| PREPARING | READY, CANCELED |
| READY | DONE, PREPARING (refazer), CANCELED |
| DONE, CANCELED | nenhuma (finais) |

- A mesma tabela está no app e no mock. O mock original aceitava qualquer transição; agora responde 409 (`ORDER-409-001`) com o pedido atual, e o `RemoteOrderCommands` traduz isso num resultado tipado.
- Os `when` são exaustivos: uma etapa nova no enum não compila até alguém decidir as regras dela.
- O toque principal leva PENDING e CONFIRMED direto para PREPARING, porque no fluxo atual ninguém confirma pedidos (premissa registrada no SPECS).

## Toque sem confirmação: desfazer por envio adiado

A cozinha não pode parar para confirmar diálogos, mas um toque errado no pico precisa ter volta.

| Opção | Custo |
|---|---|
| Diálogo de confirmação | Viola a restrição "não pode ficar clicando" |
| Enviar na hora e desfazer com um `PATCH` de volta | O servidor teria de aceitar transições para trás; os outros aparelhos veriam o pedido ir e voltar |
| **Enviar depois de 5 s, desfazer cancela o envio** | Se o app morrer nesses 5 s, o toque se perde e o card fica na etapa anterior |

Foi escolhido o envio adiado, no modelo do "desfazer envio" do Gmail: o servidor nunca vê uma ação desfeita. O custo foi aceito porque o card fica visível na etapa anterior e basta tocar de novo. O desfazer também cobre o ENTREGUE, que tira o card da tela.

Os botões continuam ativos sem conexão: a janela de 5 s absorve oscilações curtas e, se o envio falhar, o card volta e a barra avisa.

## Datas e fuso

O mock grava `created` e `updated` em UTC sem o "Z" (`toISOString().slice(0, 19)`). O fuso de origem fica em `ServerConfig.sourceZone`, hoje UTC, para que um back que grave no horário local não exija mudar código. Data inválida descarta o pedido em vez de derrubar o stream.

O tempo de espera no board conta desde a criação, que é o tempo que o cliente sente. Na Expedição, o tempo no balcão conta desde a última alteração do pedido pronto, porque o servidor não registra quando ele ficou pronto.

## Interface: MVVM com fluxo unidirecional

- Cada tela tem um ViewModel que expõe um `StateFlow` de estado, um `Flow` de avisos pontuais (consumidos uma vez) e um `Flow` de sinais sonoros. A tela só envia intenções: avançar, desfazer, dispensar alerta, filtrar.
- A tradução do domínio para a tela é feita por mappers puros (`BoardUiMapper`, `ExpeditionUiMapper`, `KitchenUiMapper`), testados na JVM sem Android.
- Os modelos de tela são `@Immutable` com `ImmutableList`, para o Compose pular a recomposição do que não mudou.

## Aguentar o pico

- **Um relógio só.** Um `State<Instant>` que muda a cada segundo, alinhado à virada do segundo, é entregue por `CompositionLocal`. Só o texto do timer lê o valor; card, lista e coluna não recompõem a cada segundo.
- **Lista com `key` e `contentType`.** Um pedido novo não recompõe nem reposiciona os outros cards.
- **Âncora da lista.** A `LazyColumn` mantém visível o card que estava no topo quando entra um item acima dele, o que esconderia justamente o pedido mais antigo (um pedido desfeito, recusado ou vindo da reconexão). Se a coluna estava no início, ela volta ao topo; se alguém rolou para baixo, a posição é mantida.
- **Mapeamento fora da thread principal** e coleções imutáveis.
- **Som com intervalo mínimo.** Uma rajada de pedidos vira um bip a cada 3 s, não uma sirene.

Medição no emulador de tablet (60 s de uso com rolagens na fila, `dumpsys gfxinfo`, uma rodada de cada). "Pico" é o mock com um pedido novo a cada 0,3 s; "quase vazio", os pedidos da semente e um novo a cada 5 s:

| Build | Cenário | Pedidos ativos | Quadros lentos | p50 | p90 | p99 |
|---|---|---|---|---|---|---|
| debug | quase vazio | 5 | 24,9% | 27 ms | 38 ms | 85 ms |
| debug | pico | 297 a 432 | 24,9% | 23 ms | 36 ms | 53 ms |
| release | quase vazio | 4 | 13,5% | 21 ms | 32 ms | 61 ms |
| release | pico | 230 a 370 | 19,2% | 18 ms | 28 ms | 34 ms |

- O build release corta quase pela metade os quadros lentos: o debug é marcado como depurável, e o Android desliga otimizações do runtime nele.
- No release, a carga aumenta a proporção de quadros lentos (de 13,5% para 19,2%), mas a cauda não piora: com centenas de pedidos chegando a cada 0,3 s, o p99 ficou em 34 ms, contra 61 ms com o board quase vazio. Não há travadas longas.
- O release foi medido sem R8 e com HTTP liberado só no experimento local, porque o release aceita apenas HTTPS e o mock é HTTP. O emulador tem GPU emulada e variação entre rodadas; o número de produção precisa ser medido num tablet físico, o que está na v2.
- A medição foi feita antes do polimento visual (faixa de atraso, barra superior e animação dos cards). Os três foram desenhados para não pesar: a faixa e a hora não recompõem o card a cada segundo, e só os cards visíveis animam.

## Leitura rápida na cozinha

O que a cozinha precisa ler de longe, no pico, sem parar o que está fazendo:

- **Itens iguais agrupados.** O servidor manda o mesmo item em linhas separadas ("2×, 1× e 1× Onion Rings"), e somar de cabeça no pico é erro na certa; o card mostra "4× Onion Rings". Só se juntam linhas com o mesmo nome, os mesmos modificadores e a mesma observação: um "sem cebola" nunca é engolido por um lanche normal.
- **Faixa lateral de atraso** na cor do selo do timer (âmbar em atenção, vermelha em atraso), para os atrasados se destacarem na coluna sem ler o número. A faixa é calculada com `derivedStateOf`, que só avisa quando o pedido muda de faixa (duas vezes na vida dele), e pintada na fase de desenho (`drawBehind`), sem recompor nem remedir o card.
- **Barra superior** com o nome da casa, os filtros, a hora e o estado da conexão sempre visível, com ponto e texto (AO VIVO, CONECTANDO, SEM CONEXÃO). Conectado também aparece: sem isso, ninguém sabe se a tela parada está viva ou travada. A hora recompõe só na virada do minuto.
- **Cards animados** ao entrar, sair e mudar de lugar, com as animações padrão do Compose: a cozinha percebe o que mudou em vez de ver a coluna saltar.
- **Tema claro e escuro.** O escuro é comum em cozinha: as cores de estado se destacam mais e a tela brilha menos num turno longo. Mas uma cozinha ou um salão muito claros fazem a tela escura parecer um buraco, e o claro é o padrão do Android e o mais agradável à primeira vista. Todos os aparelhos, inclusive a TV, seguem o modo do Android, que por padrão é claro, e trocam sem reabrir o app. As duas paletas usam tons quentes (branco quente e grafite quente) e têm o mesmo significado de cada cor; na clara, os tons de etapa ficam mais escuros para o texto branco e o modificador vai para âmbar escuro, porque o amarelo some no branco. O teste de contraste roda nas duas: WCAG AA para texto e 3:1 para a chama da marca.
- **Menos grito, mais hierarquia.** Texto normal em colunas, botões, filtros e origens, com caixa alta só nos alertas (ATRASADO, CANCELADO, COBRAR, RECONECTANDO). Os cabeçalhos das etapas têm fundo suave e uma linha na cor forte: a cor forte fica para os botões e alertas, e o olho vai primeiro ao que pede ação.

## Contexto de uso: tablet, celular e TV

O mesmo APK assume o papel do aparelho:

| Aparelho | Como é reconhecido | Tela |
|---|---|---|
| TV | Modo de interface do sistema (`UI_MODE_TYPE_TELEVISION`) | Painel somente leitura |
| Celular | Menor largura abaixo de 600 dp | Expedição |
| Tablet | Menor largura a partir de 600 dp | Board |

- **TV pelo modo do sistema, e não pelo tamanho:** uma TV 1080p tem 540 dp de menor largura e, pela largura, viraria Expedição.
- **Menor largura do aparelho, e não da janela:** girar o celular não troca a tela do garçom pelo board.
- **Tela sempre ligada e barras escondidas só no tablet e na TV.** O celular do garçom fica no bolso e é usado para outras coisas.
- **Abaixo de 600 dp, a ação vai para baixo do texto** no alerta de cancelamento e na barra de desfazer; um botão de 240 dp ao lado do texto o espremia até sumir.
- **A TV não mostra alerta de cancelamento nem toca som.** Ninguém tocaria em CIENTE, e o alerta ficaria na tela para sempre; quem trata o cancelamento é o tablet.

## Avisos sonoros e vibração

| Sinal | Board | Expedição | TV |
|---|---|---|---|
| Pedido novo | bip | não toca (não há o que buscar) | não toca |
| Pedido pronto | não toca (a própria cozinha marcou) | bip e vibração curta | não toca |
| Cancelamento em preparo | alarme | não toca (a tela não mostra esse alerta) | não toca |
| Cancelamento de pedido pronto | alarme | alarme e vibração longa | não toca |

- Os sinais comparam estados consecutivos a partir do estado já exibido: abrir o app, girar a tela ou reconectar com os mesmos pedidos não toca nada.
- O aviso de pronto usa a etapa confirmada pelo servidor, e não a otimista: desfazer uma entrega não "devolve" o pedido ao balcão, e o toque de pronto só avisa quando o servidor o aceita.
- O som sai pelo canal de alarme, que o modo de notificações não silencia. Sem áudio disponível, a tela segue funcionando em silêncio.

## Injeção de dependência: Hilt

O app roda sozinho no tablet durante o pico; um erro de injeção não pode virar uma tela quebrada no meio do serviço. O Hilt valida o grafo na compilação. O Koin resolve em tempo de execução; o `verify()` em teste mitiga, mas seria compensar em teste o que o compilador garante.

O store e o escopo de coroutines são `@Singleton`, interfaces são ligadas com `@Binds`, e o endereço do servidor vem do `BuildConfig`, definido por `-Pkds.serverUrl` ou pelo `local.properties`.

## Build e qualidade

- **Plugins de convenção** em `build-logic` (`kds.android.library`, `kds.android.compose`, `kds.android.feature` e outros) e catálogo de versões: cada módulo declara só o que é dele.
- **Portões:** ktlint e Android Lint com avisos tratados como erro. O detekt ficou de fora: a versão estável é compilada com Kotlin 2.0 e a 2.x ainda estava em alpha.
- **Mensagens de commit:** um hook versionado (`.githooks/commit-msg`) barra título fora do Conventional Commits, título acima de 72 caracteres e travessão; o CI roda o mesmo hook em cada commit do PR.
- **CI no GitHub Actions** em todo PR: ktlint, Lint, testes unitários e de interface, e o APK debug publicado como artefato.

## Testes

| Módulo | Testes |
|---|---|
| core/domain | 110 |
| core/data | 32 |
| core/designsystem | 9 |
| core/ui | 52 |
| feature/board | 31 |
| feature/expedition | 21 |
| app | 8 |
| **Total** | **263** |

- **Onde importa:** transições, evento duplicado e fora de ordem, reconexão, envio adiado, backoff, sinais sonoros, agrupamento de itens, contraste das duas paletas e o card do pedido.
- **Fakes em vez de mocks no domínio:** fábricas de pedidos em `testFixtures`, compartilhadas pelos módulos; stream e comandos falsos no teste do store; eventos duplicados e fora de ordem alimentados direto no reducer; tempo virtual para o backoff e a janela de desfazer.
- **Interface com Robolectric na JVM**, e não teste instrumentado: roda no CI em todo PR, sem emulador lento e instável. Os testes rodam em Java 21 (exigido pelo Robolectric para API 36) enquanto o código compila para 17, usam API 36 porque o Espresso ainda chama uma API removida no Android 17, e simulam o celular ou o tablet conforme a tela. Cada uma dessas configurações foi provada necessária removendo-a e vendo o teste falhar.
- **Teste de mutação manual em cada regra de negócio:** a regra é quebrada de propósito e algum teste precisa falhar. Isso revelou testes que faltavam e dois bugs reais, contados em [AI_USAGE.md](AI_USAGE.md).
- **Validação no emulador** de tablet, celular e TV antes de cada PR. Foi assim que apareceram o pedido escondido acima do topo da lista e o ENTREGUE sem desfazer, os dois com todos os testes verdes.

## Limitações conhecidas e v2

- **Alertas de cancelamento vivem em memória.** Se o app reiniciar com um alerta sem CIENTE, ele se perde: o `snapshot` traz o pedido já cancelado, que é tratado como histórico. Aceitável com o app aberto o turno todo; a v2 persiste os alertas localmente.
- **Toque perdido se o app morrer na janela de 5 s.** Custo aceito do envio adiado; a v2 pode persistir o envio agendado.
- **O tempo de espera depende do relógio do aparelho.** Os timers comparam o horário do pedido, gravado pelo servidor, com o relógio local; um aparelho com a hora errada mostra tempos errados. Apareceu na validação: o emulador de TV estava quase duas horas atrasado, e todos os timers ficavam em 00:00, porque para ele os pedidos tinham sido criados no futuro. Com a hora automática, como em qualquer aparelho de uso real, não acontece. A v2 calcula a diferença entre o relógio do servidor e o do aparelho e corrige os timers.
- **Fila offline de ações.** Hoje uma ação sem rede volta com aviso; a v2 pode enfileirar e reenviar.
- **Build release com R8 e medição em tablet físico.** Exige regras de keep para Hilt, serialization e Retrofit; um erro nelas quebra o app só no release, risco desnecessário perto da entrega.
- **Densidade do board.** Poucos cards por coluna no tablet de 1920x1200, priorizando leitura a 2 m; os itens agrupados e os filtros na barra superior devolveram parte da altura.
- **Fora do escopo por falta de dado no back:** sincronizar chapa e fritadeira, pronto por item e métricas de tempo médio. A priorização entre delivery e salão é decisão do dono, não do sistema.
