package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.OrderReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionConfirmed
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionFailed
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionRejected
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionRequested
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionSent
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionUndone
import io.github.gabrielevanger.kds.core.domain.kitchen.PendingTransition.Phase
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Toque otimista com envio adiado: a tela muda na hora, o envio espera a janela de desfazer
 * e a resposta do servidor (sucesso, 409 ou falha de rede) decide o estado final.
 */
class OptimisticTransitionTest {

    private val id = OrderId(7)
    private val queued = anOrder(id = 7, stage = Stage.PENDING, version = 1)
    private val preparing = anOrder(id = 7, stage = Stage.PREPARING, version = 2)

    private fun KitchenState.reduce(vararg events: KitchenEvent): KitchenState = events.fold(this, OrderReducer::reduce)

    private fun KitchenState.displayed(): KitchenOrder = ordersByArrival().single { it.order.id == id }

    private val withQueuedOrder = KitchenState().reduce(OrderReceived(queued))

    @Nested
    inner class Toque {

        @Test
        fun `toque mostra a proxima etapa na hora sem alterar o dado do servidor`() {
            val state = withQueuedOrder.reduce(TransitionRequested(id))

            assertEquals(Stage.PREPARING, state.displayed().stage)
            assertEquals(Stage.PENDING, state.orders.getValue(id).stage)
            assertEquals(PendingTransition(Stage.PENDING, Stage.PREPARING, Phase.WAITING), state.pending[id])
        }

        @Test
        fun `toque duplo enquanto ha acao pendente e ignorado`() {
            val afterFirstTap = withQueuedOrder.reduce(TransitionRequested(id))

            val afterSecondTap = afterFirstTap.reduce(TransitionRequested(id))

            assertSame(afterFirstTap, afterSecondTap)
            assertEquals(Stage.PREPARING, afterSecondTap.displayed().stage)
        }

        @Test
        fun `toque em pedido desconhecido e ignorado`() {
            val state = withQueuedOrder.reduce(TransitionRequested(OrderId(99)))

            assertSame(withQueuedOrder, state)
        }
    }

    @Nested
    inner class Desfazer {

        @Test
        fun `desfazer dentro da janela devolve o card a etapa anterior`() {
            val state = withQueuedOrder.reduce(TransitionRequested(id), TransitionUndone(id))

            assertTrue(state.pending.isEmpty())
            assertEquals(Stage.PENDING, state.displayed().stage)
        }

        @Test
        fun `depois do envio nao e mais possivel desfazer`() {
            val sent = withQueuedOrder.reduce(TransitionRequested(id), TransitionSent(id))

            val state = sent.reduce(TransitionUndone(id))

            assertSame(sent, state)
            assertFalse(state.pending.getValue(id).canUndo)
            assertEquals(Stage.PREPARING, state.displayed().stage)
        }
    }

    @Nested
    inner class `Resposta do servidor` {

        private val inFlight = withQueuedOrder.reduce(TransitionRequested(id), TransitionSent(id))

        @Test
        fun `confirmacao aplica o pedido da resposta e encerra a pendencia`() {
            val state = inFlight.reduce(TransitionConfirmed(preparing))

            assertNull(state.pending[id])
            assertEquals(preparing, state.orders.getValue(id))
        }

        @Test
        fun `eco pelo stream antes da resposta nao faz o card voltar`() {
            val afterEcho = inFlight.reduce(OrderReceived(preparing))
            val afterResponse = afterEcho.reduce(TransitionConfirmed(preparing))

            assertEquals(Stage.PREPARING, afterEcho.displayed().stage)
            assertEquals(Stage.PREPARING, afterResponse.displayed().stage)
            assertTrue(afterResponse.pending.isEmpty())
        }

        @Test
        fun `rejeicao 409 desfaz o otimismo e mostra o pedido como o servidor tem`() {
            val preparedElsewhere = anOrder(id = 7, stage = Stage.READY, version = 3)

            val state = inFlight.reduce(TransitionRejected(preparedElsewhere))

            assertNull(state.pending[id])
            assertEquals(Stage.READY, state.displayed().stage)
        }

        @Test
        fun `rejeicao sem mudanca no servidor devolve o card a etapa real`() {
            val state = inFlight.reduce(TransitionRejected(queued))

            assertNull(state.pending[id])
            assertEquals(Stage.PENDING, state.displayed().stage)
        }

        @Test
        fun `falha de rede devolve o card a etapa anterior`() {
            val state = inFlight.reduce(TransitionFailed(id))

            assertNull(state.pending[id])
            assertEquals(Stage.PENDING, state.displayed().stage)
        }

        @Test
        fun `entrega confirmada tira o pedido da fila`() {
            val ready = anOrder(id = 7, stage = Stage.READY, version = 3)
            val delivered = anOrder(id = 7, stage = Stage.DONE, version = 4)

            val state = KitchenState().reduce(
                OrderReceived(ready),
                TransitionRequested(id),
                TransitionSent(id),
                TransitionConfirmed(delivered),
            )

            assertTrue(state.orders.isEmpty())
            assertTrue(state.pending.isEmpty())
        }
    }

    @Nested
    inner class `Mudancas vindas de fora` {

        @Test
        fun `outro aparelho avanca o pedido durante a janela e a acao local e descartada`() {
            val state = withQueuedOrder.reduce(TransitionRequested(id), OrderReceived(preparing))

            assertNull(state.pending[id])
            assertEquals(Stage.PREPARING, state.displayed().stage)
        }

        @Test
        fun `cancelamento logo apos o toque em preparar gera alerta`() {
            val canceled = anOrder(id = 7, stage = Stage.CANCELED, version = 2)

            val state = withQueuedOrder.reduce(TransitionRequested(id), OrderReceived(canceled))

            assertTrue(state.orders.isEmpty())
            assertTrue(state.pending.isEmpty())
            assertEquals(Stage.PREPARING, state.cancellationAlerts.getValue(id).previousStage)
        }
    }
}
