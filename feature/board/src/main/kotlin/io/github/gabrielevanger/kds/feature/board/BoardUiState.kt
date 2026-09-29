package io.github.gabrielevanger.kds.feature.board

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import java.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Estação da cozinha selecionada no filtro; [ALL] mostra o pedido inteiro. */
enum class StationFilter { ALL, CHAPA, FRITADEIRA, MONTAGEM }

/** Como a origem do pedido é rotulada na tela; o texto de cada uma fica em strings.xml. */
enum class OriginLabel { TABLE, COUNTER, WHATSAPP, IFOOD, PIGZ, WEB_MENU, LOYALTY, OTHER }

@Immutable
data class BoardUiState(
    val columns: ImmutableList<BoardColumnUi>,
    val stationFilter: StationFilter,
    val connection: ConnectionState,
    val cancellationAlerts: ImmutableList<CancellationAlertUi>,
    val undo: UndoUi?,
) {
    companion object {
        val Initial = BoardUiState(
            columns = persistentListOf(
                BoardColumnUi(StageTone.QUEUED, persistentListOf()),
                BoardColumnUi(StageTone.PREPARING, persistentListOf()),
                BoardColumnUi(StageTone.READY, persistentListOf()),
            ),
            stationFilter = StationFilter.ALL,
            connection = ConnectionState.Connecting,
            cancellationAlerts = persistentListOf(),
            undo = null,
        )
    }
}

@Immutable
data class BoardColumnUi(val tone: StageTone, val orders: ImmutableList<OrderCardUi>)

/**
 * Card do pedido. [itemCount] e [isLarge] descrevem o pedido inteiro mesmo com filtro de estação,
 * para a cozinha saber o tamanho do que está montando; [items] mostra só a estação filtrada.
 */
@Immutable
data class OrderCardUi(
    val id: OrderId,
    val reference: String,
    val origin: OriginLabel,
    val tableNumber: Int?,
    val createdAt: Instant,
    val itemCount: Int,
    val isLarge: Boolean,
    val items: ImmutableList<OrderItemUi>,
    val note: String?,
    val tone: StageTone,
    val isAwaitingServer: Boolean,
)

@Immutable
data class OrderItemUi(
    val key: String,
    val quantity: Int,
    val name: String,
    val modifiers: ImmutableList<ModifierUi>,
    val note: String?,
)

@Immutable
data class ModifierUi(val kind: ModifierKind, val text: String)

@Immutable
data class CancellationAlertUi(
    val id: OrderId,
    val reference: String,
    val origin: OriginLabel,
    val tableNumber: Int?,
    val previousTone: StageTone,
)

/** Toque mais recente que ainda pode ser desfeito. */
@Immutable
data class UndoUi(val orderId: OrderId, val reference: String, val targetTone: StageTone)
