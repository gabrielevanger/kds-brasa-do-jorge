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
| 1 | Pedido novo aparece sozinho, em tempo real | Comanda perdida | a definir |
| 2 | Fila por ordem de chegada, em colunas Na fila, Preparando, Pronto | Qual fazer primeiro | a definir |
| 3 | Tempo de espera visível, com faixas normal, atenção e atrasado | Qual fazer primeiro | a definir |
| 4 | Avançar etapa com um toque, sem confirmação, com Desfazer por ~5 s | Não pode clicar | a definir |
| 5 | Modificadores e observações em destaque abaixo do item | Modificadores | a definir |
| 6 | Origem forte: MESA 4, BALCÃO, iFood, WhatsApp, Pigz | Delivery vs salão | a definir |
| 7 | Quantidade de itens em destaque no card | Coca vs combos | a definir |
| 8 | Cancelamento de pedido em andamento vira alerta com som até alguém dispensar | Cliente desiste | a definir |
| 9 | Tela de Expedição no celular: prontos e ação Entregue | Garçom não sabe | a definir |
| 10 | Filtro por estação (Chapa, Fritadeira, Montagem) | Sincronização (parcial) | a definir |
| 11 | Painel para TV, somente leitura (opcional) | Qual fazer primeiro | a definir |

## Requisitos não funcionais

| Requisito | Onde |
|---|---|
| Reconexão automática com backoff quando a rede cai | a definir |
| Evento repetido ou fora de ordem não duplica nem regride o pedido | a definir |
| Fluido no pico: centenas de pedidos ativos e evento a cada 300 ms | a definir |
| Sem tela branca nem erro cru: mantém o último estado com aviso "Reconectando" | a definir |
| Ciclo de vida modelado numa máquina de estados, transição inválida rejeitada | a definir |
| Alvos de toque grandes, legível a distância, estado com cor, ícone e texto | a definir |
| Layout por contexto: tablet horizontal, celular e TV | a definir |
| Testes nas transições, no evento duplicado e num componente central | a definir |
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
- Um pedido ativo que deixa de vir no snapshot após uma reconexão saiu do servidor por motivo desconhecido e é removido da tela sem alerta. O mock sempre envia todos os pedidos; a regra protege contra um back que omita os finalizados.
- Os números do cenário não fecham (60% de 3.200 pedidos dá ~63 por hora no pico; o texto diz até 14 em 20 min, ~42 por hora). O teste de carga usa o maior, com margem.

## Fora de escopo (v2)

- Sincronizar chapa e fritadeira: precisa do tempo de preparo por item.
- Priorização automática delivery vs salão: é decisão do dono, não do sistema.
- Pronto por item ou por estação: o back só tem etapa por pedido.
- Métricas de tempo médio e fila offline de ações.
