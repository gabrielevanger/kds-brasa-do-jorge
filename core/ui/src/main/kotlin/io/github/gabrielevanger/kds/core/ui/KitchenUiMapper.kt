package io.github.gabrielevanger.kds.core.ui

import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.kitchen.CancellationAlert
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.kitchen.StoreNotice
import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderItem
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.Stage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** Traduções do estado da cozinha usadas por todas as telas. Funções puras, testáveis sem Android. */
object KitchenUiMapper {

    /** Nomes dos grupos de complementos enviados pelo servidor (ATTRIBUTE_GROUPS em mock/server.js). */
    private const val GROUP_REMOVE = "Remover"
    private const val GROUP_ADD = "Adicionais"

    /**
     * Itens do card. O servidor pode mandar o mesmo item em linhas separadas ("2× Onion Rings",
     * "1× Onion Rings"): linhas com o mesmo nome, os mesmos modificadores e a mesma observação viram
     * uma só, com a quantidade somada, na ordem da primeira aparição. Qualquer diferença mantém a
     * linha separada, para nenhum modificador se perder.
     */
    fun toItems(items: List<OrderItem>): ImmutableList<OrderItemUi> = items
        .groupBy { Triple(it.name, it.modifiers, it.note) }
        .values
        .map { same -> toItem(same.first()).copy(quantity = same.sumOf { it.quantity }) }
        .toImmutableList()

    fun toItem(item: OrderItem) = OrderItemUi(
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

    fun cancellationAlerts(state: KitchenState): ImmutableList<CancellationAlertUi> =
        state.cancellationAlerts.values.map(::toAlert).toImmutableList()

    private fun toAlert(alert: CancellationAlert) = CancellationAlertUi(
        id = alert.order.id,
        reference = alert.order.reference,
        origin = originLabel(alert.order),
        tableNumber = alert.order.table,
        previousTone = toneOf(alert.previousStage) ?: StageTone.PREPARING,
        items = summarizeByName(alert.order.items),
    )

    /**
     * Resumo sem modificadores: itens iguais em linhas separadas viram uma linha só
     * ("3× Smash Bacon"), na ordem em que aparecem no pedido.
     */
    fun summarizeByName(items: List<OrderItem>): ImmutableList<OrderItemUi> = items
        .groupBy { it.name }
        .map { (name, sameName) ->
            OrderItemUi(
                key = name,
                quantity = sameName.sumOf { it.quantity },
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
    fun mapNotice(notice: StoreNotice, state: KitchenState): KitchenNotice? {
        val (orderId, kind) = when (notice) {
            is StoreNotice.TransitionNotSent -> notice.orderId to KitchenNotice.Kind.NOT_SENT
            is StoreNotice.TransitionRejected -> notice.orderId to KitchenNotice.Kind.REJECTED
        }
        val order = state.orders[orderId] ?: return null
        val currentTone = toneOf(state.pending[orderId]?.to ?: order.stage) ?: return null
        return KitchenNotice(kind, order.reference, currentTone)
    }

    /** O mapa de pendências preserva a ordem de inserção: o último que ainda cabe desfazer é o mais recente. */
    fun latestUndoable(state: KitchenState): UndoUi? {
        val (orderId, pending) = state.pending.entries.lastOrNull { it.value.canUndo } ?: return null
        val order = state.orders[orderId] ?: return null
        return UndoUi(orderId, order.reference, targetTone = toneOf(pending.to))
    }

    /** Etapa como a cozinha a enxerga; pedido encerrado não aparece em nenhuma tela. */
    fun toneOf(stage: Stage): StageTone? = when (stage) {
        Stage.PENDING, Stage.CONFIRMED -> StageTone.QUEUED
        Stage.PREPARING -> StageTone.PREPARING
        Stage.READY -> StageTone.READY
        Stage.DONE, Stage.CANCELED -> null
    }

    fun originLabel(order: Order): OriginLabel = when (order.origin) {
        Origin.POS -> if (order.table != null) OriginLabel.TABLE else OriginLabel.COUNTER
        Origin.WHATSAPP_AI -> OriginLabel.WHATSAPP
        Origin.IFOOD -> OriginLabel.IFOOD
        Origin.MARKETPLACE -> OriginLabel.PIGZ
        Origin.CARDAPIO_WEB -> OriginLabel.WEB_MENU
        Origin.CLIENTE_FIEL -> OriginLabel.LOYALTY
        Origin.OTHER -> OriginLabel.OTHER
    }
}
