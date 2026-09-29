package io.github.gabrielevanger.kds.feature.expedition

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.PaymentStatus
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.KitchenUiMapper
import kotlinx.collections.immutable.toImmutableList

/** Traduz o estado da cozinha para o que o garçom vê. Função pura, testável sem Android. */
internal object ExpeditionUiMapper {

    fun map(state: KitchenState, connection: ConnectionState): ExpeditionUiState = ExpeditionUiState(
        orders = readyOrders(state).map(::toReadyOrder).toImmutableList(),
        connection = connection,
        // Cancelamento em preparo é assunto da cozinha; o garçom só precisa saber o que não entregar.
        cancellationAlerts = KitchenUiMapper.cancellationAlerts(state)
            .filter { state.cancellationAlerts[it.id]?.previousStage == Stage.READY }
            .toImmutableList(),
        undo = KitchenUiMapper.latestUndoable(state),
    )

    /**
     * Pedidos prontos na tela, incluindo a etapa otimista: o toque em ENTREGUE tira o card na hora,
     * e desfazer o devolve ao mesmo lugar. O servidor não guarda quando o pedido ficou pronto; a
     * última alteração ([Order.updatedAt]) serve, porque o pedido pronto só muda de novo ao sair do balcão.
     */
    private fun readyOrders(state: KitchenState): List<Order> = state.ordersByArrival()
        .filter { it.stage == Stage.READY }
        .map { it.order }
        .sortedWith(compareBy({ it.updatedAt }, { it.id.value }))

    private fun toReadyOrder(order: Order) = ReadyOrderUi(
        id = order.id,
        reference = order.reference,
        origin = KitchenUiMapper.originLabel(order),
        tableNumber = order.table,
        readyAt = order.updatedAt,
        mustCharge = order.paymentStatus != PaymentStatus.PAID,
        itemCount = order.itemCount,
        items = KitchenUiMapper.summarizeByName(order.items),
    )
}
