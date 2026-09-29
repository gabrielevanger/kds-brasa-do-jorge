package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.kitchen.PendingTransition.Phase
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.model.StageMachine
import io.github.gabrielevanger.kds.core.domain.model.isInProgress
import io.github.gabrielevanger.kds.core.domain.model.isTerminal

/**
 * Função pura que calcula o próximo estado da cozinha a partir de um evento.
 * Sem rede, relógio ou coroutines: todo o comportamento é verificável com chamadas diretas.
 */
object OrderReducer {

    fun reduce(state: KitchenState, event: KitchenEvent): KitchenState = when (event) {
        is KitchenEvent.SnapshotReceived -> reconcileSnapshot(state, event.orders)

        is KitchenEvent.OrderReceived -> applyServerOrder(state, event.order)

        is KitchenEvent.CancellationAlertDismissed -> state.copy(
            cancellationAlerts = state.cancellationAlerts.remove(event.orderId),
        )

        is KitchenEvent.TransitionRequested -> requestTransition(state, event.orderId)

        is KitchenEvent.TransitionUndone -> state.updatePending(event.orderId) { pending ->
            if (pending.canUndo) null else pending
        }

        is KitchenEvent.TransitionSent -> state.updatePending(event.orderId) { pending ->
            pending.copy(phase = Phase.IN_FLIGHT)
        }

        is KitchenEvent.TransitionConfirmed -> applyServerOrder(state, event.order).withoutPending(event.order.id)

        is KitchenEvent.TransitionRejected ->
            applyServerOrder(state, event.currentOrder).withoutPending(event.currentOrder.id)

        is KitchenEvent.TransitionFailed -> state.withoutPending(event.orderId)
    }

    /**
     * O snapshot chega a cada conexão e é a verdade do servidor naquele momento. Cada pedido passa
     * pela mesma regra de version, então cancelamentos ocorridos durante a queda geram alerta e
     * transições pendentes ainda válidas são mantidas. Pedido ativo ausente do snapshot saiu do
     * servidor por motivo desconhecido e é removido sem alerta.
     */
    private fun reconcileSnapshot(state: KitchenState, snapshot: List<Order>): KitchenState {
        val folded = snapshot.fold(state, ::applyServerOrder)
        val applied = if (folded.hasSnapshot) folded else folded.copy(hasSnapshot = true)
        val presentIds = snapshot.mapTo(HashSet()) { it.id }
        return applied.orders.keys
            .filterNot { it in presentIds }
            .fold(applied) { current, absentId ->
                current.copy(orders = current.orders.remove(absentId)).withoutPending(absentId)
            }
    }

    /** Toque duplo, pedido desconhecido ou etapa final não geram nova transição. */
    private fun requestTransition(state: KitchenState, orderId: OrderId): KitchenState {
        if (orderId in state.pending) return state
        val order = state.orders[orderId] ?: return state
        val next = StageMachine.nextStage(order.stage) ?: return state
        return state.copy(
            pending = state.pending.put(orderId, PendingTransition(from = order.stage, to = next, Phase.WAITING)),
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

        val updated = when (incoming.stage) {
            Stage.CANCELED -> cancel(state, incoming, previousStage = state.displayedStage(known))
            Stage.DONE -> state.copy(orders = state.orders.remove(incoming.id))
            else -> state.withOrder(incoming)
        }
        return updated.reconcilePending(incoming.id)
    }

    /**
     * O alerta considera a etapa que a cozinha está vendo: se alguém já tocou em "preparar",
     * para quem está na chapa o pedido começou, mesmo que o servidor ainda não saiba.
     */
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

    /**
     * Uma transição pendente só continua válida enquanto o servidor estiver na etapa de onde ela partiu.
     * Isso cobre o eco da própria requisição, a mudança feita por outro aparelho e o pedido que saiu do estado.
     */
    private fun KitchenState.reconcilePending(orderId: OrderId): KitchenState {
        val pendingTransition = pending[orderId] ?: return this
        val serverStage = orders[orderId]?.stage
        return if (serverStage == pendingTransition.from) this else withoutPending(orderId)
    }

    private fun KitchenState.displayedStage(order: Order): Stage = pending[order.id]?.to ?: order.stage

    private fun KitchenState.withOrder(order: Order): KitchenState = copy(orders = orders.put(order.id, order))

    private fun KitchenState.withoutPending(orderId: OrderId): KitchenState =
        if (orderId in pending) copy(pending = pending.remove(orderId)) else this

    private fun KitchenState.updatePending(
        orderId: OrderId,
        transform: (PendingTransition) -> PendingTransition?,
    ): KitchenState {
        val current = pending[orderId] ?: return this
        val updated = transform(current)
        return when {
            updated == current -> this
            updated == null -> copy(pending = pending.remove(orderId))
            else -> copy(pending = pending.put(orderId, updated))
        }
    }
}
