# Mock server — KDS Brasa do Jorge

Backend falso para o desafio. **Não tem dependências**: roda só com Node (>= 18).

```bash
node mock/server.js
```

Sobe em `http://localhost:4000` (mude com a env `PORT`).

## Endpoints

| Método | Rota | O que faz |
|---|---|---|
| `GET` | `/orders` | Lista os pedidos ativos |
| `GET` | `/orders/:id` | Um pedido |
| `PATCH` | `/orders/:id` | Muda o stage — corpo `{ "stage": "PREPARING" }` |
| `GET` | `/events` | Stream **SSE** em tempo real |
| `GET` | `/health` | Healthcheck |

`GET /orders` responde `{ "pagination": null, "orders": [ ... ] }`. Um erro sempre
vem como `{ "error": "..." }`; erros de validação trazem também `identifier`,
`code` e `errors[]`.

### Transições de stage

O `PATCH` só aceita as transições abaixo. Qualquer outra responde **409 Conflict**
com o pedido atual em `order`, para o cliente corrigir a tela sem outra requisição.

| De | Para |
|---|---|
| `PENDING` | `CONFIRMED`, `PREPARING`, `CANCELED` |
| `CONFIRMED` | `PREPARING`, `CANCELED` |
| `PREPARING` | `READY`, `CANCELED` |
| `READY` | `DONE`, `PREPARING` (refazer), `CANCELED` |
| `DONE`, `CANCELED` | nenhuma (estados finais) |

Enviar o stage em que o pedido já está responde 200 sem alterar nada (sem novo
`version` e sem evento), então repetir a requisição após uma falha de rede é seguro.

Exemplo de resposta 409 (pedido resumido):

```json
{
  "error": "Transição de DONE para PENDING não permitida",
  "identifier": "ORDER-409-001",
  "code": "ORDER-409-001",
  "order": { "id": 7, "stage": "DONE", "version": 5 }
}
```

## Tempo real (SSE)

`GET /events` abre um stream. No connect, ele manda um evento `snapshot` com a
lista atual; depois vai empurrando:

- `order.created` — pedido novo entrou na cozinha
- `order.updated` — pedido mudou de stage (inclui cancelamento pelo cliente)

Exemplo no browser/React Native:

```js
const es = new EventSource('http://localhost:4000/events');
es.addEventListener('order.created', (e) => console.log('novo', JSON.parse(e.data)));
es.addEventListener('order.updated', (e) => console.log('mudou', JSON.parse(e.data)));
```

Um pedido novo entra a cada **5s** por padrão. Ajuste com a env `EVENT_INTERVAL_MS`
(ms), ex.: `EVENT_INTERVAL_MS=2000 node mock/server.js`.

## Formato do pedido

```json
{
  "id": 7,
  "reference": "#0007",
  "origin": "IFOOD",
  "stage": "PREPARING",
  "status": "PAID",
  "table": null,
  "total": "54.00",
  "created": "2026-09-21T20:14:03",
  "updated": "2026-09-21T20:16:40",
  "version": 3,
  "note": null,
  "orderItems": [
    {
      "id": "7-0",
      "name": "Smash Duplo Cheddar",
      "productionArea": "CHAPA",
      "quantity": 1,
      "price": "34.00",
      "total": "34.00",
      "note": "Caprichar no ponto",
      "attributes": [
        { "name": "Ponto da carne", "items": [{ "name": "Mal passado" }] }
      ]
    }
  ]
}
```

### Valores possíveis

- `id` (número) e `reference` (código exibível, ex.: `#0007`): identificam o pedido. Use o `id` como chave — inclusive para não duplicar o card quando o mesmo evento chegar duas vezes.
- `version` (número): começa em `1` e cresce a cada alteração do pedido (mudança de stage ou cancelamento). Um evento só deve ser aplicado se o `version` dele for maior que o que o cliente já tem; assim, evento repetido, eco do próprio PATCH e evento fora de ordem são descartados.
- `origin` (canal de onde o pedido veio): `POS` (balcão), `WHATSAPP_AI` (WhatsApp), `IFOOD`, `MARKETPLACE` (app/marketplace Pigz), `CARDAPIO_WEB` (cardápio web), `CLIENTE_FIEL` (Cliente Fiel)
- `stage`: `PENDING` → `CONFIRMED` → `PREPARING` → `READY` → `DONE`, ou `CANCELED`
- `status` (pagamento): `PAID` | `NO_PAID`
- `table`: número da mesa do salão, ou `null` (delivery/balcão)
- `productionArea` (**área de produção** do item — a estação da cozinha que o prepara): `CHAPA` | `FRITADEIRA` | `MONTAGEM`

Os pedidos chegam de vários canais (`origin`) ao mesmo tempo — pense em como a tela lida com essa mistura. Para o tempo de espera, compare `created` com o horário atual.

## De onde vêm esses nomes

Para ficar perto da realidade, o mock usa **convenções reais do nosso back**
(o que já é público no app):

- `stage` (`PENDING`/`CONFIRMED`/`PREPARING`/`CANCELED`), `status`
  (`PAID`/`NO_PAID`), valores monetários em **decimal string** com 2 casas,
  datas em **ISO 8601 sem timezone** nos campos `created`/`updated`, e a
  estrutura `orderItems` → item → `attributes` → `items`.

O que é **específico deste desafio** (não existe no back real):

- `stage` `READY` e `DONE` — o back de vocês é focado em entrega e não tem um
  "pronto na cozinha". **Como você modela o caminho até o pedido sair da cozinha
  faz parte do desafio.**
- `productionArea` (CHAPA/FRITADEIRA/MONTAGEM) — a **área de produção** do item, na linha de produção da Brasa do Jorge.

> Este mock é de propósito simples e sem firulas. **Pode mexer nele** — adicionar
> campo, endpoint, mudar a cadência dos eventos. Ler e estender código alheio faz
> parte do desafio.
