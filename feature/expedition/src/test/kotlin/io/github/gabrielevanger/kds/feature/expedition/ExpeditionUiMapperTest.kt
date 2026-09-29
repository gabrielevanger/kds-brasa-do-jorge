package io.github.gabrielevanger.kds.feature.expedition

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderReducer
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.OrderItem
import io.github.gabrielevanger.kds.core.domain.model.PaymentStatus
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.testing.BASE_TIME
import io.github.gabrielevanger.kds.core.domain.testing.anItem
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import io.github.gabrielevanger.kds.core.ui.UndoUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpeditionUiMapperTest {

    private fun stateOf(vararg orders: Order, events: List<KitchenEvent> = emptyList()): KitchenState =
        (listOf(KitchenEvent.SnapshotReceived(orders.toList())) + events).fold(KitchenState(), OrderReducer::reduce)

    private fun map(state: KitchenState) = ExpeditionUiMapper.map(state, ConnectionState.Connected)

    /** Pedido pronto [minutesAgo] minutos antes de [BASE_TIME], criado bem antes disso. */
    private fun readyOrder(
        id: Long,
        minutesAgo: Long,
        paymentStatus: PaymentStatus = PaymentStatus.PAID,
        items: List<OrderItem> = listOf(anItem()),
    ) = anOrder(
        id = id,
        stage = Stage.READY,
        version = 3,
        createdAt = BASE_TIME.minusSeconds(SECONDS_PER_HOUR),
        updatedAt = BASE_TIME.minusSeconds(minutesAgo * SECONDS_PER_MINUTE),
        paymentStatus = paymentStatus,
        items = items,
    )

    @Nested
    inner class Lista {

        @Test
        fun `mostra so os pedidos prontos`() {
            val state = stateOf(
                anOrder(id = 1, stage = Stage.PENDING),
                anOrder(id = 2, stage = Stage.PREPARING, version = 2),
                readyOrder(id = 3, minutesAgo = 1),
            )

            assertEquals(listOf(OrderId(3)), map(state).orders.map { it.id })
        }

        /** Quem chegou primeiro ao balcão esfria primeiro, mesmo que tenha sido criado depois. */
        @Test
        fun `ordena pelo tempo no balcao e nao pela criacao`() {
            val createdFirstReadyLast = readyOrder(id = 1, minutesAgo = 1)
            val createdLastReadyFirst = readyOrder(id = 2, minutesAgo = 9).copy(createdAt = BASE_TIME)

            val orders = map(stateOf(createdFirstReadyLast, createdLastReadyFirst)).orders

            assertEquals(listOf(OrderId(2), OrderId(1)), orders.map { it.id })
            assertEquals(BASE_TIME.minusSeconds(9 * SECONDS_PER_MINUTE), orders.first().readyAt)
        }

        @Test
        fun `entregar tira o pedido na hora e oferece desfazer`() {
            val state = stateOf(
                readyOrder(id = 1, minutesAgo = 5),
                readyOrder(id = 2, minutesAgo = 3),
                events = listOf(KitchenEvent.TransitionRequested(OrderId(1))),
            )

            val expedition = map(state)

            assertEquals(listOf(OrderId(2)), expedition.orders.map { it.id })
            assertEquals(UndoUi(OrderId(1), "#0001", targetTone = null), expedition.undo)
        }

        @Test
        fun `desfazer a entrega devolve o pedido ao mesmo lugar`() {
            val state = stateOf(
                readyOrder(id = 1, minutesAgo = 5),
                readyOrder(id = 2, minutesAgo = 3),
                events = listOf(
                    KitchenEvent.TransitionRequested(OrderId(1)),
                    KitchenEvent.TransitionUndone(OrderId(1)),
                ),
            )

            assertEquals(listOf(OrderId(1), OrderId(2)), map(state).orders.map { it.id })
        }
    }

    @Test
    fun `pedido nao pago pede para cobrar`() {
        val orders = map(
            stateOf(
                readyOrder(id = 1, minutesAgo = 2, paymentStatus = PaymentStatus.NOT_PAID),
                readyOrder(id = 2, minutesAgo = 1),
            ),
        ).orders

        assertTrue(orders[0].mustCharge)
        assertFalse(orders[1].mustCharge)
    }

    @Test
    fun `itens iguais viram uma linha so para conferir a sacola`() {
        val order = readyOrder(
            id = 1,
            minutesAgo = 1,
            items = listOf(
                anItem(name = "Smash Bacon", quantity = 1),
                anItem(name = "Coca-Cola Lata", quantity = 2),
                anItem(name = "Smash Bacon", quantity = 1),
            ),
        )

        val card = map(stateOf(order)).orders.single()

        assertEquals(listOf("Smash Bacon" to 2, "Coca-Cola Lata" to 2), card.items.map { it.name to it.quantity })
        assertEquals(4, card.itemCount)
    }

    @Nested
    inner class `Sinais sonoros` {

        private val preparing = anOrder(id = 1, stage = Stage.PREPARING, version = 2)
        private val ready = readyOrder(id = 2, minutesAgo = 1)
        private val loaded = stateOf(preparing, ready)

        private fun signalsAfter(vararg events: KitchenEvent) =
            ExpeditionUiMapper.signals(loaded, events.fold(loaded, OrderReducer::reduce))

        @Test
        fun `pedido que fica pronto avisa o garcom`() {
            val signals = signalsAfter(KitchenEvent.OrderReceived(preparing.copy(stage = Stage.READY, version = 3)))

            assertEquals(setOf(KitchenSignal.ORDER_READY), signals)
        }

        /** Pedido novo entra na fila da cozinha: não há nada para o garçom buscar ainda. */
        @Test
        fun `pedido novo nao toca na expedicao`() {
            assertEquals(emptySet<KitchenSignal>(), signalsAfter(KitchenEvent.OrderReceived(anOrder(id = 3))))
        }

        @Test
        fun `cancelamento de pedido pronto toca o alarme`() {
            val signals = signalsAfter(KitchenEvent.OrderReceived(ready.copy(stage = Stage.CANCELED, version = 4)))

            assertEquals(setOf(KitchenSignal.CANCELLATION), signals)
        }

        /** Um alerta sem CIENTE não pode virar sirene a cada evento que chega. */
        @Test
        fun `alerta ainda na tela nao repete o alarme`() {
            val alerted = OrderReducer.reduce(
                loaded,
                KitchenEvent.OrderReceived(ready.copy(stage = Stage.CANCELED, version = 4)),
            )
            val next = OrderReducer.reduce(alerted, KitchenEvent.OrderReceived(anOrder(id = 3)))

            assertEquals(emptySet<KitchenSignal>(), ExpeditionUiMapper.signals(alerted, next))
        }

        /** Sem alerta desse pedido na tela, o alarme tocaria sem o garçom saber por quê. */
        @Test
        fun `cancelamento de pedido em preparo nao toca na expedicao`() {
            val signals = signalsAfter(KitchenEvent.OrderReceived(preparing.copy(stage = Stage.CANCELED, version = 3)))

            assertEquals(emptySet<KitchenSignal>(), signals)
        }
    }

    @Nested
    inner class `Alertas de cancelamento` {

        private fun canceled(order: Order) =
            KitchenEvent.OrderReceived(order.copy(stage = Stage.CANCELED, version = order.version + 1))

        @Test
        fun `pedido cancelado depois de pronto avisa o garcom`() {
            val ready = readyOrder(id = 1, minutesAgo = 2)

            val alerts = map(stateOf(ready, events = listOf(canceled(ready)))).cancellationAlerts

            assertEquals(listOf(OrderId(1)), alerts.map { it.id })
        }

        /** Cancelamento em preparo é assunto da cozinha: no balcão não há o que deixar de entregar. */
        @Test
        fun `pedido cancelado em preparo nao aparece na expedicao`() {
            val preparing = anOrder(id = 1, stage = Stage.PREPARING, version = 2)

            val alerts = map(stateOf(preparing, events = listOf(canceled(preparing)))).cancellationAlerts

            assertTrue(alerts.isEmpty())
        }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_PER_HOUR = 3_600L
    }
}
