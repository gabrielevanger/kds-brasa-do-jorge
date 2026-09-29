package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId

/** Tudo o que pode mudar o estado da cozinha. */
sealed interface KitchenEvent {

    /** Lista completa enviada pelo servidor ao conectar, incluindo pedidos finalizados. */
    data class SnapshotReceived(val orders: List<Order>) : KitchenEvent

    /** Pedido criado ou alterado no servidor, pelo stream ou pela resposta de uma requisição. */
    data class OrderReceived(val order: Order) : KitchenEvent

    data class CancellationAlertDismissed(val orderId: OrderId) : KitchenEvent
}
