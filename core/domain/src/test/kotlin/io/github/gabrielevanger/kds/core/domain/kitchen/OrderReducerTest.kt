package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.CancellationAlertDismissed
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.OrderReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.SnapshotReceived
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.testing.BASE_TIME
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OrderReducerTest {

    private fun KitchenState.reduce(vararg events: KitchenEvent): KitchenState = events.fold(this, OrderReducer::reduce)

    private val empty = KitchenState()

    @Nested
    inner class `Idempotencia por version` {

        @Test
        fun `pedido novo entra no estado`() {
            val order = anOrder(id = 7)

            val state = empty.reduce(OrderReceived(order))

            assertEquals(order, state.orders[OrderId(7)])
        }

        @Test
        fun `mesmo evento duas vezes nao duplica nem altera o estado`() {
            val order = anOrder(id = 7)
            val once = empty.reduce(OrderReceived(order))

            val twice = once.reduce(OrderReceived(order))

            assertSame(once, twice)
            assertEquals(1, twice.orders.size)
        }

        @Test
        fun `evento com version menor que a conhecida e ignorado`() {
            val current = anOrder(id = 7, stage = Stage.READY, version = 3)
            val stale = anOrder(id = 7, stage = Stage.PREPARING, version = 2)

            val state = empty.reduce(OrderReceived(current), OrderReceived(stale))

            assertEquals(Stage.READY, state.orders.getValue(OrderId(7)).stage)
        }

        @Test
        fun `eco do proprio PATCH com a mesma version nao reaplica`() {
            val confirmed = anOrder(id = 7, stage = Stage.PREPARING, version = 2)
            val afterResponse = empty.reduce(OrderReceived(confirmed))

            val afterEcho = afterResponse.reduce(OrderReceived(confirmed.copy()))

            assertSame(afterResponse, afterEcho)
        }

        @Test
        fun `alteracao com version maior e aplicada`() {
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = Stage.PENDING, version = 1)),
                OrderReceived(anOrder(id = 7, stage = Stage.PREPARING, version = 2)),
            )

            assertEquals(Stage.PREPARING, state.orders.getValue(OrderId(7)).stage)
        }
    }

    @Nested
    inner class `Pedidos finalizados` {

        @Test
        fun `pedido entregue sai do estado`() {
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = Stage.READY, version = 3)),
                OrderReceived(anOrder(id = 7, stage = Stage.DONE, version = 4)),
            )

            assertTrue(state.orders.isEmpty())
            assertTrue(state.cancellationAlerts.isEmpty())
        }

        @ParameterizedTest
        @EnumSource(Stage::class, names = ["DONE", "CANCELED"])
        fun `pedido desconhecido que ja chega finalizado e ignorado`(stage: Stage) {
            val state = empty.reduce(OrderReceived(anOrder(id = 7, stage = stage, version = 5)))

            assertEquals(empty, state)
        }
    }

    @Nested
    inner class Cancelamento {

        @ParameterizedTest
        @EnumSource(Stage::class, names = ["PREPARING", "READY"])
        fun `pedido cancelado depois de comecar vira alerta e sai da fila`(stage: Stage) {
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = stage, version = 2)),
                OrderReceived(anOrder(id = 7, stage = Stage.CANCELED, version = 3)),
            )

            assertTrue(state.orders.isEmpty())
            val alert = state.cancellationAlerts.getValue(OrderId(7))
            assertEquals(stage, alert.previousStage)
            assertEquals(Stage.CANCELED, alert.order.stage)
        }

        @ParameterizedTest
        @EnumSource(Stage::class, names = ["PENDING", "CONFIRMED"])
        fun `pedido cancelado ainda na fila sai sem alerta`(stage: Stage) {
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = stage, version = 1)),
                OrderReceived(anOrder(id = 7, stage = Stage.CANCELED, version = 2)),
            )

            assertTrue(state.orders.isEmpty())
            assertTrue(state.cancellationAlerts.isEmpty())
        }

        @Test
        fun `cancelamento repetido nao recria alerta ja dispensado`() {
            val canceled = anOrder(id = 7, stage = Stage.CANCELED, version = 3)
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = Stage.PREPARING, version = 2)),
                OrderReceived(canceled),
                CancellationAlertDismissed(OrderId(7)),
                OrderReceived(canceled),
            )

            assertTrue(state.cancellationAlerts.isEmpty())
        }

        @Test
        fun `dispensar remove somente o alerta indicado`() {
            val state = empty.reduce(
                OrderReceived(anOrder(id = 7, stage = Stage.PREPARING, version = 2)),
                OrderReceived(anOrder(id = 8, stage = Stage.READY, version = 3)),
                OrderReceived(anOrder(id = 7, stage = Stage.CANCELED, version = 3)),
                OrderReceived(anOrder(id = 8, stage = Stage.CANCELED, version = 4)),
                CancellationAlertDismissed(OrderId(7)),
            )

            assertEquals(setOf(OrderId(8)), state.cancellationAlerts.keys)
        }
    }

    @Nested
    inner class Snapshot {

        @Test
        fun `snapshot inicial carrega os ativos e ignora os finalizados`() {
            val state = empty.reduce(
                SnapshotReceived(
                    listOf(
                        anOrder(id = 1, stage = Stage.PREPARING),
                        anOrder(id = 2, stage = Stage.PENDING),
                        anOrder(id = 3, stage = Stage.DONE),
                        anOrder(id = 4, stage = Stage.CANCELED),
                    ),
                ),
            )

            assertEquals(setOf(OrderId(1), OrderId(2)), state.orders.keys)
            assertTrue(state.cancellationAlerts.isEmpty())
        }

        @Test
        fun `pedido que chega no snapshot e de novo como criado nao duplica`() {
            val order = anOrder(id = 7)

            val state = empty.reduce(SnapshotReceived(listOf(order)), OrderReceived(order))

            assertEquals(1, state.orders.size)
        }
    }

    @Test
    fun `fila fica em ordem de chegada com empate decidido pelo id`() {
        val state = empty.reduce(
            OrderReceived(anOrder(id = 3, createdAt = BASE_TIME.plusSeconds(60))),
            OrderReceived(anOrder(id = 2, createdAt = BASE_TIME)),
            OrderReceived(anOrder(id = 1, createdAt = BASE_TIME)),
        )

        assertEquals(listOf(1L, 2L, 3L), state.ordersByArrival().map { it.id.value })
    }
}
