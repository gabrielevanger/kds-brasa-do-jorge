# Specs: KDS Brasa do Jorge

## Leitura da dor

Peso definido por quanto a dor custa (cliente perdido, retrabalho, prejuízo) e por quanto o KDS resolve sem depender de dado que o back não tem.

| Dor do Seu Jorge | Peso | Por quê |
|---|---|---|
| Comanda perdida, impressora travando | Alto | Cliente esperou 40 min. Eliminar papel e impressora resolve na raiz |
| Não sabe qual fazer primeiro | Alto | É o que ele chama de "meu problema" |
| Cliente desiste e ninguém avisa | Alto | Prejuízo direto de insumo e tempo de chapa no pico |
| Modificadores se perdem | Alto | Prato volta, retrabalho no pior horário |
| Garçom não sabe que ficou pronto | Alto | Lanche esfria no balcão |
| Uma coca e quatro combos parecem iguais | Médio | Custo baixo de resolver, mas não perde cliente sozinho |
| Delivery e salão disputam a cozinha | Médio | O KDS mostra a origem; decidir a prioridade é regra do dono |
| Não pode ficar clicando | Restrição | Vale para todas as telas, não é uma funcionalidade |
| Batata e hambúrguer não sincronizam | v2 | Precisa do tempo de preparo por item, que o back não tem |

## Requisitos funcionais

| # | Requisito | Dor | Onde |
|---|---|---|---|
| 1 | Pedido novo aparece sozinho, em tempo real | Comanda perdida | SSE em `core/data` (`SseOrderConnection`, `ReconnectingOrderStream`) alimentando o `OrderStore`; bip pelo `KitchenSignals`. Testes: `SseOrderConnectionTest`, `OrderStoreTest`, `KitchenSignalsTest` |
| 2 | Fila por ordem de chegada, em colunas Na fila, Preparando, Pronto | Qual fazer primeiro | `KitchenState.ordersByArrival`, `BoardUiMapper`, `BoardScreen`. Testes: `BoardUiMapperTest`, `BoardColumnScrollTest` (o pedido mais antigo nunca fica escondido acima do topo) |
| 3 | Tempo de espera visível, com faixas normal, atenção e atrasado | Qual fazer primeiro | `WaitPolicy` no domínio, `WaitTimer` no design system, um relógio único (`KitchenClock`), faixa lateral de atraso no card. Testes: `WaitPolicyTest`, `WaitTimerFormatTest`, `OrderCardTest`, `UrgencyStripeTest` |
| 4 | Avançar etapa com um toque, sem confirmação, com Desfazer por ~5 s | Não pode clicar | Envio adiado no `OrderStore`, transição otimista no `OrderReducer`, barra no `FeedbackBar` (inclusive no ENTREGUE). Testes: `OptimisticTransitionTest`, `OrderStoreTest`, `FeedbackBarTest`, `KitchenUiMapperTest` |
| 5 | Modificadores e observações em destaque abaixo do item | Modificadores | `KitchenUiMapper.toItem` (grupo do servidor vira remover, adicionar ou outro), linhas iguais agrupadas sem perder modificador (`KitchenUiMapper.toItems`), `ModifierLine`, `OrderCard`. Testes: `KitchenUiMapperTest`, `BoardUiMapperTest`, `OrderCardTest` |
| 6 | Origem forte: MESA 4, BALCÃO, iFood, WhatsApp, Pigz | Delivery vs salão | `KitchenUiMapper.originLabel`, `OriginTag`, textos em `core/ui`. Testes: `KitchenUiMapperTest`, `OrderCardTest` |
| 7 | Quantidade de itens em destaque no card | Coca vs combos | `Order.itemCount` e `Order.isLarge`, selo GRANDE no `OrderCard`. Testes: `OrderTest`, `OrderCardTest` |
| 8 | Cancelamento de pedido em andamento vira alerta com som até alguém dispensar | Cliente desiste | Alerta no `OrderReducer` (inclusive na reconexão), `CancellationAlerts` com CIENTE, alarme pelo `KitchenSignals` e `KitchenSoundEffect`. Testes: `OrderReducerTest`, `ReconnectionTest`, `CancellationAlertsTest`, `KitchenSignalsTest`, `KitchenSoundPolicyTest` |
| 9 | Tela de Expedição no celular: prontos e ação Entregue | Garçom não sabe | `feature/expedition` (COBRAR, tempo no balcão, ENTREGUE com desfazer, bip e vibração no pronto). Testes: `ExpeditionUiMapperTest`, `ReadyOrderCardTest`, `ExpeditionScreenTest` |
| 10 | Filtro por estação (Chapa, Fritadeira, Montagem) | Sincronização (parcial) | `StationFilter` no `BoardUiMapper`. Teste: `BoardUiMapperTest` (filtro por estação) |
| 11 | Painel para TV, somente leitura (opcional) | Qual fazer primeiro | `TvPanelScreen`, escolhido pela `KitchenScreen` quando o aparelho é TV. Testes: `TvPanelScreenTest`, `KitchenScreenTest` |

## Requisitos não funcionais

| Requisito | Onde |
|---|---|
| Reconexão automática com backoff quando a rede cai | `ReconnectingOrderStream` com `BackoffPolicy` (1 a 30 s, com jitter) e `NetworkMonitor`; heartbeat `: ping` no mock. Testes: `ReconnectingOrderStreamTest`, `BackoffPolicyTest` |
| Evento repetido ou fora de ordem não duplica nem regride o pedido | `version` no mock e `OrderReducer` que só aplica versão maior. Testes: `OrderReducerTest` (evento duplicado, fora de ordem, eco do PATCH), `ReconnectionTest` |
| Fluido no pico: centenas de pedidos ativos e evento a cada 300 ms | `LazyColumn` com key e contentType (`BoardScreen`), mapeamento fora da thread principal (`BoardViewModel`), relógio único que recompõe só o timer (`KitchenClock`), coleções imutáveis. Medido em 29/09 no emulador de tablet, build release, 60 s com rolagens (`dumpsys gfxinfo`): com 230 a 370 pedidos ativos e um novo a cada 0,3 s, 19,2% de quadros lentos e p99 de 34 ms, contra 13,5% e 61 ms com o board quase vazio; a carga aumenta os quadros lentos, mas não cria travadas longas. Tabela completa, com o build debug, no `ARCHITECTURE.md`. O número de produção precisa ser medido num tablet físico. Sem teste de carga automatizado |
| Sem tela branca nem erro cru: mantém o último estado com aviso "Reconectando" | O `OrderStore` mantém o estado durante a queda; `ConnectionBanner` avisa, e a barra superior mostra a conexão sempre, inclusive conectado (`KitchenTopBar`). Testes: `ConnectionBannerTest`, `KitchenTopBarTest`, `ReconnectionTest` |
| Ciclo de vida modelado numa máquina de estados, transição inválida rejeitada | `StageMachine` no app; tabela de transições e 409 no mock; `RemoteOrderCommands` traduz o 409. Testes: `StageMachineTest`, `RemoteOrderCommandsTest`, `OptimisticTransitionTest` |
| Alvos de toque grandes, legível a distância, estado com cor, ícone e texto | Tokens do `core/designsystem` (toque mínimo de 64 dp, botão de 72 dp, tipografia para 2 m), `WaitTimer` e `StageVisuals` com cor, ícone e texto; temas escuro e claro com o mesmo significado de cada cor. Testes: `KdsColorsContrastTest` (contraste AA nas duas paletas), `OrderCardTest` |
| Layout por contexto: tablet horizontal, celular e TV | `KitchenScreen` e `MainActivity` escolhem a tela; `MessageWithAction` empilha a ação no celular. Testes: `KitchenScreenTest`, `CancellationAlertsTest` e `FeedbackBarTest` (posição do botão no celular e no tablet) |
| Testes nas transições, no evento duplicado e num componente central | `StageMachineTest`, `OrderReducerTest` e `OrderCardTest`, com teste de mutação manual em cada regra de negócio |
| Uso de IA documentado | `docs/AI_USAGE.md` |

## Premissas

- Os horários do mock vêm em UTC sem o "Z" (`toISOString().slice(0, 19)` no `server.js`).
- Ninguém avança PENDING para CONFIRMED no mock; os dois aparecem como "Na fila" e o primeiro toque leva a PREPARING.
- O tempo de espera conta desde a criação do pedido, porque é o tempo que o cliente sente.
- Faixas de atraso: até 8 min normal, 8 a 15 min atenção, acima de 15 min atrasado. Confirmadas com o cliente e configuráveis.
- Pedido com 5 itens ou mais, somando as quantidades, é destacado como grande.
- Pedido não pago (`NO_PAID`) mostra "COBRAR" na Expedição, para o garçom não entregar sem receber. Confirmado com o cliente.
- Na Expedição, o tempo no balcão conta desde a última alteração do pedido pronto (`updated`): o servidor não registra quando ele ficou pronto, e o pedido pronto só muda de novo ao sair do balcão.
- A Expedição só mostra cancelamentos de pedidos que já estavam prontos ("NÃO ENTREGAR"); cancelamento em preparo é assunto da cozinha.
- Um tablet na montagem com a visão geral; "pronto" é marcado no pedido inteiro.
- Linhas do pedido com o mesmo nome, os mesmos modificadores e a mesma observação são o mesmo item e aparecem somadas; qualquer diferença mantém a linha separada.
- Todos os aparelhos, inclusive a TV, seguem o modo claro ou escuro do Android, que por padrão é claro.
- Um pedido ativo que deixa de vir no snapshot após uma reconexão saiu do servidor por motivo desconhecido e é removido da tela sem alerta. O mock sempre envia todos os pedidos; a regra protege contra um back que omita os finalizados.
- Os números do cenário não fecham (60% de 3.200 pedidos dá ~63 por hora no pico; o texto diz até 14 em 20 min, ~42 por hora). A lista foi pensada para o maior, com margem.

## Fora de escopo (v2)

- Sincronizar chapa e fritadeira: precisa do tempo de preparo por item.
- Priorização automática delivery vs salão: é decisão do dono, não do sistema.
- Pronto por item ou por estação: o back só tem etapa por pedido.
- Métricas de tempo médio e fila offline de ações.
- Build release com R8 e medição de fluidez num tablet físico: ligar o R8 exige regras de keep para Hilt, serialization e Retrofit, e um erro nelas quebra o app só no release.
