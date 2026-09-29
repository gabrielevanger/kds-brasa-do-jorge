package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Estado da cozinha. [orders] guarda apenas pedidos ativos, na última versão conhecida
 * do servidor. [pending] guarda toques já refletidos na tela e ainda não confirmados.
 * [cancellationAlerts] guarda cancelamentos que exigem atenção até alguém dispensar.
 */
data class KitchenState(
    val orders: PersistentMap<OrderId, Order> = persistentMapOf(),
    val pending: PersistentMap<OrderId, PendingTransition> = persistentMapOf(),
    val cancellationAlerts: PersistentMap<OrderId, CancellationAlert> = persistentMapOf(),
) {
    /** Fila como a tela deve mostrar, na ordem de chegada; empate no horário é decidido pelo id. */
    fun ordersByArrival(): List<KitchenOrder> = orders.values
        .sortedWith(compareBy({ it.createdAt }, { it.id.value }))
        .map { KitchenOrder(order = it, pendingTransition = pending[it.id]) }
}

/**
 * Pedido como aparece na tela: a etapa otimista prevalece sobre a do servidor
 * enquanto houver uma transição pendente.
 */
data class KitchenOrder(val order: Order, val pendingTransition: PendingTransition?) {
    val stage: Stage get() = pendingTransition?.to ?: order.stage
}

/**
 * Toque já aplicado na tela. Em [Phase.WAITING] ainda pode ser desfeito;
 * em [Phase.IN_FLIGHT] a requisição já saiu.
 */
data class PendingTransition(val from: Stage, val to: Stage, val phase: Phase) {
    val canUndo: Boolean get() = phase == Phase.WAITING

    enum class Phase { WAITING, IN_FLIGHT }
}

/** Pedido cancelado depois que a cozinha começou. [previousStage] diz até onde ele chegou. */
data class CancellationAlert(val order: Order, val previousStage: Stage)
