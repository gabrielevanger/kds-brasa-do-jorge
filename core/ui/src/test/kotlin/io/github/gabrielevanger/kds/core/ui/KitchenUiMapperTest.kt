package io.github.gabrielevanger.kds.core.ui

import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderReducer
import io.github.gabrielevanger.kds.core.domain.kitchen.StoreNotice
import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.testing.anItem
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KitchenUiMapperTest {

    private fun stateOf(vararg orders: Order, events: List<KitchenEvent> = emptyList()): KitchenState =
        (listOf(KitchenEvent.SnapshotReceived(orders.toList())) + events).fold(KitchenState(), OrderReducer::reduce)

    @ParameterizedTest(name = "{0} com mesa {1} -> {2}")
    @CsvSource(
        "POS, 4, TABLE",
        "POS, , COUNTER",
        "WHATSAPP_AI, , WHATSAPP",
        "IFOOD, , IFOOD",
        "MARKETPLACE, , PIGZ",
        "CARDAPIO_WEB, , WEB_MENU",
        "CLIENTE_FIEL, , LOYALTY",
        "OTHER, , OTHER",
    )
    fun `origem vira o rotulo da etiqueta`(origin: Origin, table: Int?, expected: OriginLabel) {
        assertEquals(expected, KitchenUiMapper.originLabel(anOrder(id = 1, origin = origin, table = table)))
    }

    @Test
    fun `modificadores sao classificados pelo grupo enviado pelo servidor`() {
        val item = anItem(
            modifiers = listOf(
                Modifier(group = "Remover", option = "Sem cebola"),
                Modifier(group = "Adicionais", option = "Cheddar extra"),
                Modifier(group = "Ponto da carne", option = "Mal passado"),
            ),
        )

        assertEquals(
            listOf(
                ModifierUi(ModifierKind.REMOVE, "Sem cebola"),
                ModifierUi(ModifierKind.ADD, "Cheddar extra"),
                ModifierUi(ModifierKind.OTHER, "Mal passado"),
            ),
            KitchenUiMapper.toItem(item).modifiers,
        )
    }

    @Test
    fun `alerta agrupa itens iguais que o servidor envia em linhas separadas`() {
        val order = anOrder(
            id = 1,
            stage = Stage.PREPARING,
            version = 2,
            items = listOf(
                anItem(name = "Smash Bacon", quantity = 1),
                anItem(name = "Milkshake Ovomaltine", quantity = 1),
                anItem(name = "Smash Bacon", quantity = 2),
            ),
        )
        val state =
            stateOf(order, events = listOf(KitchenEvent.OrderReceived(order.copy(stage = Stage.CANCELED, version = 3))))

        val items = KitchenUiMapper.cancellationAlerts(state).single().items

        assertEquals(listOf("Smash Bacon" to 3, "Milkshake Ovomaltine" to 1), items.map { it.name to it.quantity })
    }

    @Test
    fun `entrega tambem oferece desfazer`() {
        val state = stateOf(
            anOrder(id = 8, stage = Stage.READY, version = 3),
            events = listOf(KitchenEvent.TransitionRequested(OrderId(8))),
        )

        assertEquals(UndoUi(OrderId(8), "#0008", targetTone = null), KitchenUiMapper.latestUndoable(state))
    }

    @Nested
    inner class `Avisos de envio` {

        @Test
        fun `falha de envio informa o pedido e a etapa para onde o card voltou`() {
            val state = stateOf(anOrder(id = 5, stage = Stage.PENDING))

            val notice = KitchenUiMapper.mapNotice(StoreNotice.TransitionNotSent(OrderId(5)), state)

            assertEquals(KitchenNotice(KitchenNotice.Kind.NOT_SENT, "#0005", StageTone.QUEUED), notice)
        }

        @Test
        fun `rejeicao informa a etapa real do pedido no servidor`() {
            val state = stateOf(anOrder(id = 5, stage = Stage.READY, version = 3))

            val notice = KitchenUiMapper.mapNotice(StoreNotice.TransitionRejected(OrderId(5)), state)

            assertEquals(KitchenNotice(KitchenNotice.Kind.REJECTED, "#0005", StageTone.READY), notice)
        }

        @Test
        fun `pedido que ja saiu da tela nao gera aviso`() {
            assertNull(KitchenUiMapper.mapNotice(StoreNotice.TransitionNotSent(OrderId(99)), KitchenState()))
        }
    }
}
