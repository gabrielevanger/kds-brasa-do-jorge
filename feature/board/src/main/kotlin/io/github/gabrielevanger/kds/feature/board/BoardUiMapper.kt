package io.github.gabrielevanger.kds.feature.board

import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenOrder
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.KitchenUiMapper
import kotlinx.collections.immutable.toImmutableList

/** Traduz o estado da cozinha para o que o board desenha. Função pura, testável sem Android. */
internal object BoardUiMapper {

    fun map(state: KitchenState, connection: ConnectionState, filter: StationFilter): BoardUiState {
        val cardsByTone = state.ordersByArrival().mapNotNull { toCard(it, filter) }.groupBy { it.tone }
        val columns = StageTone.entries.map { tone ->
            BoardColumnUi(tone, cardsByTone[tone].orEmpty().toImmutableList())
        }
        return BoardUiState(
            columns = columns.toImmutableList(),
            stationFilter = filter,
            connection = connection,
            cancellationAlerts = KitchenUiMapper.cancellationAlerts(state),
            undo = KitchenUiMapper.latestUndoable(state),
        )
    }

    /** Com filtro de estação, o pedido sem itens daquela estação não aparece. */
    private fun toCard(kitchenOrder: KitchenOrder, filter: StationFilter): OrderCardUi? {
        val order = kitchenOrder.order
        val tone = KitchenUiMapper.toneOf(kitchenOrder.stage) ?: return null
        val items = order.items.filter { filter.includes(it.productionArea) }
        if (items.isEmpty()) return null
        return OrderCardUi(
            id = order.id,
            reference = order.reference,
            origin = KitchenUiMapper.originLabel(order),
            tableNumber = order.table,
            createdAt = order.createdAt,
            itemCount = order.itemCount,
            isLarge = order.isLarge,
            items = items.map(KitchenUiMapper::toItem).toImmutableList(),
            note = order.note,
            tone = tone,
            isAwaitingServer = kitchenOrder.pendingTransition != null,
        )
    }

    private fun StationFilter.includes(area: ProductionArea): Boolean = when (this) {
        StationFilter.ALL -> true
        StationFilter.CHAPA -> area == ProductionArea.CHAPA
        StationFilter.FRITADEIRA -> area == ProductionArea.FRITADEIRA
        StationFilter.MONTAGEM -> area == ProductionArea.MONTAGEM
    }
}
