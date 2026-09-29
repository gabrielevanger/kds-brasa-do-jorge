package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * Estado da cozinha. [orders] guarda apenas pedidos ativos, na última versão conhecida
 * do servidor. [cancellationAlerts] guarda cancelamentos que exigem atenção até alguém dispensar.
 */
data class KitchenState(
    val orders: PersistentMap<OrderId, Order> = persistentMapOf(),
    val cancellationAlerts: PersistentMap<OrderId, CancellationAlert> = persistentMapOf(),
) {
    /** Fila na ordem de chegada; empate no horário é decidido pelo id. */
    fun ordersByArrival(): List<Order> = orders.values.sortedWith(compareBy({ it.createdAt }, { it.id.value }))
}

/** Pedido cancelado depois que a cozinha começou. [previousStage] diz até onde ele chegou. */
data class CancellationAlert(val order: Order, val previousStage: Stage)
