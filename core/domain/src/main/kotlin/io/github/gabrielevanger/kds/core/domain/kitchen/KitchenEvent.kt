package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId

/** Tudo o que pode mudar o estado da cozinha. */
sealed interface KitchenEvent {

    /** Lista completa enviada pelo servidor ao conectar, incluindo pedidos finalizados. */
    data class SnapshotReceived(val orders: List<Order>) : KitchenEvent

    /** Pedido criado ou alterado no servidor, recebido pelo stream. */
    data class OrderReceived(val order: Order) : KitchenEvent

    data class CancellationAlertDismissed(val orderId: OrderId) : KitchenEvent

    /** Toque principal no pedido: a tela avança na hora e o envio aguarda a janela de desfazer. */
    data class TransitionRequested(val orderId: OrderId) : KitchenEvent

    data class TransitionUndone(val orderId: OrderId) : KitchenEvent

    /** A janela de desfazer terminou e a requisição foi enviada. */
    data class TransitionSent(val orderId: OrderId) : KitchenEvent

    /** Resposta de sucesso do servidor, com o pedido atualizado. */
    data class TransitionConfirmed(val order: Order) : KitchenEvent

    /** Servidor recusou a transição (409) e devolveu o pedido como ele está. */
    data class TransitionRejected(val currentOrder: Order) : KitchenEvent

    /** A requisição não chegou ao servidor; a tela volta à etapa anterior. */
    data class TransitionFailed(val orderId: OrderId) : KitchenEvent
}
