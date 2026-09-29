package io.github.gabrielevanger.kds.feature.board

import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.kitchen.CancellationAlert
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenOrder
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.kitchen.StoreNotice
import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderItem
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** Traduz o estado da cozinha para o que o board desenha. Função pura, testável sem Android. */
internal object BoardUiMapper {

    /** Nomes dos grupos de complementos enviados pelo servidor (ATTRIBUTE_GROUPS em mock/server.js). */
    private const val GROUP_REMOVE = "Remover"
    private const val GROUP_ADD = "Adicionais"

    fun map(state: KitchenState, connection: ConnectionState, filter: StationFilter): BoardUiState {
        val cardsByTone = state.ordersByArrival().mapNotNull { toCard(it, filter) }.groupBy { it.tone }
        val columns = StageTone.entries.map { tone ->
            BoardColumnUi(tone, cardsByTone[tone].orEmpty().toImmutableList())
        }
        return BoardUiState(
            columns = columns.toImmutableList(),
            stationFilter = filter,
            connection = connection,
            cancellationAlerts = state.cancellationAlerts.values.map(::toAlert).toImmutableList(),
            undo = latestUndoable(state),
        )
    }

    /** Com filtro de estação, o pedido sem itens daquela estação não aparece. */
    private fun toCard(kitchenOrder: KitchenOrder, filter: StationFilter): OrderCardUi? {
        val order = kitchenOrder.order
        val tone = kitchenOrder.stage.toTone() ?: return null
        val items = order.items.filter { filter.includes(it.productionArea) }
        if (items.isEmpty()) return null
        return OrderCardUi(
            id = order.id,
            reference = order.reference,
            origin = order.originLabel(),
            tableNumber = order.table,
            createdAt = order.createdAt,
            itemCount = order.itemCount,
            isLarge = order.isLarge,
            items = items.map(::toItem).toImmutableList(),
            note = order.note,
            tone = tone,
            isAwaitingServer = kitchenOrder.pendingTransition != null,
        )
    }

    private fun toItem(item: OrderItem) = OrderItemUi(
        key = item.id,
        quantity = item.quantity,
        name = item.name,
        modifiers = item.modifiers.map(::toModifier).toImmutableList(),
        note = item.note,
    )

    private fun toModifier(modifier: Modifier) = ModifierUi(
        kind = when (modifier.group) {
            GROUP_REMOVE -> ModifierKind.REMOVE
            GROUP_ADD -> ModifierKind.ADD
            else -> ModifierKind.OTHER
        },
        text = modifier.option,
    )

    private fun toAlert(alert: CancellationAlert) = CancellationAlertUi(
        id = alert.order.id,
        reference = alert.order.reference,
        origin = alert.order.originLabel(),
        tableNumber = alert.order.table,
        previousTone = alert.previousStage.toTone() ?: StageTone.PREPARING,
        items = summarizeByName(alert.order.items),
    )

    /**
     * No alerta não há modificadores: itens iguais em linhas separadas viram uma linha só
     * ("3× Smash Bacon"), na ordem em que aparecem no pedido.
     */
    private fun summarizeByName(items: List<OrderItem>) = items
        .groupBy { it.name }
        .map { (name, sameName) ->
            OrderItemUi(
                key = name,
                quantity = sameName.sumOf {
                    it.quantity
                },
                name = name,
                modifiers = persistentListOf(),
                note = null,
            )
        }
        .toImmutableList()

    /**
     * Traduz um aviso do store usando o estado do momento. Pedido que já saiu da tela (cancelado
     * ou entregue nesse meio tempo) não gera aviso: o card não existe mais para o aviso se referir.
     */
    fun mapNotice(notice: StoreNotice, state: KitchenState): BoardNotice? {
        val (orderId, kind) = when (notice) {
            is StoreNotice.TransitionNotSent -> notice.orderId to BoardNotice.Kind.NOT_SENT
            is StoreNotice.TransitionRejected -> notice.orderId to BoardNotice.Kind.REJECTED
        }
        val order = state.orders[orderId] ?: return null
        val currentTone = (state.pending[orderId]?.to ?: order.stage).toTone() ?: return null
        return BoardNotice(kind, order.reference, currentTone)
    }

    /** O mapa de pendências preserva a ordem de inserção: o último que ainda cabe desfazer é o mais recente. */
    private fun latestUndoable(state: KitchenState): UndoUi? {
        val (orderId, pending) = state.pending.entries.lastOrNull { it.value.canUndo } ?: return null
        val order = state.orders[orderId] ?: return null
        val targetTone = pending.to.toTone() ?: return null
        return UndoUi(orderId, order.reference, targetTone)
    }

    private fun Stage.toTone(): StageTone? = when (this) {
        Stage.PENDING, Stage.CONFIRMED -> StageTone.QUEUED
        Stage.PREPARING -> StageTone.PREPARING
        Stage.READY -> StageTone.READY
        Stage.DONE, Stage.CANCELED -> null
    }

    private fun Order.originLabel(): OriginLabel = when (origin) {
        Origin.POS -> if (table != null) OriginLabel.TABLE else OriginLabel.COUNTER
        Origin.WHATSAPP_AI -> OriginLabel.WHATSAPP
        Origin.IFOOD -> OriginLabel.IFOOD
        Origin.MARKETPLACE -> OriginLabel.PIGZ
        Origin.CARDAPIO_WEB -> OriginLabel.WEB_MENU
        Origin.CLIENTE_FIEL -> OriginLabel.LOYALTY
        Origin.OTHER -> OriginLabel.OTHER
    }

    private fun StationFilter.includes(area: ProductionArea): Boolean = when (this) {
        StationFilter.ALL -> true
        StationFilter.CHAPA -> area == ProductionArea.CHAPA
        StationFilter.FRITADEIRA -> area == ProductionArea.FRITADEIRA
        StationFilter.MONTAGEM -> area == ProductionArea.MONTAGEM
    }
}
