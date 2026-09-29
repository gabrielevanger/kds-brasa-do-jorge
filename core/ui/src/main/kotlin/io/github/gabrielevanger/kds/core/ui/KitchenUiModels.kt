package io.github.gabrielevanger.kds.core.ui

import androidx.compose.runtime.Immutable
import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import kotlinx.collections.immutable.ImmutableList

/** Como a origem do pedido é rotulada na tela; o texto de cada uma fica em strings.xml. */
enum class OriginLabel { TABLE, COUNTER, WHATSAPP, IFOOD, PIGZ, WEB_MENU, LOYALTY, OTHER }

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
    /** O card saiu da tela: o alerta precisa dizer o que parar de fazer. */
    val items: ImmutableList<OrderItemUi>,
)

/** Toque mais recente que ainda pode ser desfeito. */
@Immutable
data class UndoUi(val orderId: OrderId, val reference: String, val targetTone: StageTone)

/** Aviso pontual sobre um toque que não chegou ao servidor, com a etapa em que o card ficou. */
@Immutable
data class KitchenNotice(val kind: Kind, val reference: String, val currentTone: StageTone) {
    enum class Kind {
        /** Falha de rede: o card voltou para a etapa anterior. */
        NOT_SENT,

        /** Outro aparelho mudou o pedido antes; o card mostra a etapa real. */
        REJECTED,
    }
}
