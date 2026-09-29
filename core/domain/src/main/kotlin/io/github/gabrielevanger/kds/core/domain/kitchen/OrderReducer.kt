package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.model.isInProgress
import io.github.gabrielevanger.kds.core.domain.model.isTerminal

/**
 * Função pura que calcula o próximo estado da cozinha a partir de um evento.
 * Sem rede, relógio ou coroutines: todo o comportamento é verificável com chamadas diretas.
 */
object OrderReducer {

    fun reduce(state: KitchenState, event: KitchenEvent): KitchenState = when (event) {
        is KitchenEvent.SnapshotReceived -> event.orders.fold(state, ::applyServerOrder)

        is KitchenEvent.OrderReceived -> applyServerOrder(state, event.order)

        is KitchenEvent.CancellationAlertDismissed -> state.copy(
            cancellationAlerts = state.cancellationAlerts.remove(event.orderId),
        )
    }

    /**
     * Aplica um pedido vindo do servidor somente se for mais novo que o conhecido.
     * O [Order.version] resolve de uma vez evento repetido, eco do próprio PATCH e evento fora de ordem.
     */
    private fun applyServerOrder(state: KitchenState, incoming: Order): KitchenState {
        val known = state.orders[incoming.id]
            // Pedido desconhecido que já chega finalizado é histórico do servidor, não trabalho da cozinha.
            ?: return if (incoming.stage.isTerminal) state else state.withOrder(incoming)

        if (incoming.version <= known.version) return state

        return when (incoming.stage) {
            Stage.CANCELED -> cancel(state, incoming, previousStage = known.stage)
            Stage.DONE -> state.copy(orders = state.orders.remove(incoming.id))
            else -> state.withOrder(incoming)
        }
    }

    private fun KitchenState.withOrder(order: Order): KitchenState = copy(orders = orders.put(order.id, order))

    private fun cancel(state: KitchenState, canceled: Order, previousStage: Stage): KitchenState {
        val alerts = if (previousStage.isInProgress) {
            state.cancellationAlerts.put(canceled.id, CancellationAlert(canceled, previousStage))
        } else {
            state.cancellationAlerts
        }
        return state.copy(
            orders = state.orders.remove(canceled.id),
            cancellationAlerts = alerts,
        )
    }
}
