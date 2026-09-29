package io.github.gabrielevanger.kds.feature.expedition

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.CancellationAlertUi
import io.github.gabrielevanger.kds.core.ui.OrderItemUi
import io.github.gabrielevanger.kds.core.ui.OriginLabel
import io.github.gabrielevanger.kds.core.ui.UndoUi
import java.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class ExpeditionUiState(
    /** Do pedido pronto há mais tempo ao mais recente: o primeiro é o lanche que está esfriando. */
    val orders: ImmutableList<ReadyOrderUi>,
    val connection: ConnectionState,
    val cancellationAlerts: ImmutableList<CancellationAlertUi>,
    val undo: UndoUi?,
) {
    companion object {
        val Initial = ExpeditionUiState(
            orders = persistentListOf(),
            connection = ConnectionState.Connecting,
            cancellationAlerts = persistentListOf(),
            undo = null,
        )
    }
}

/** Pedido no balcão. [items] é o resumo para conferir a sacola, sem modificadores. */
@Immutable
data class ReadyOrderUi(
    val id: OrderId,
    val reference: String,
    val origin: OriginLabel,
    val tableNumber: Int?,
    val readyAt: Instant,
    /** Pedido ainda não pago: o garçom não pode entregar sem cobrar. */
    val mustCharge: Boolean,
    val itemCount: Int,
    val items: ImmutableList<OrderItemUi>,
)
