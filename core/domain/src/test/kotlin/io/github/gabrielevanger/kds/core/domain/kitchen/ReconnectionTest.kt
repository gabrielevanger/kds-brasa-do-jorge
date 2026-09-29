package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.OrderReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.SnapshotReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionRequested
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionSent
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cenários de queda e volta da conexão: o snapshot enviado a cada conexão reconcilia o estado
 * sem perder o que a cozinha está fazendo e sem esconder cancelamentos ocorridos no intervalo.
 */
class ReconnectionTest {

    private fun KitchenState.reduce(vararg events: KitchenEvent): KitchenState = events.fold(this, OrderReducer::reduce)

    private val preparing = anOrder(id = 7, stage = Stage.PREPARING, version = 2)
    private val queued = anOrder(id = 8, stage = Stage.PENDING, version = 1)
    private val beforeDrop = KitchenState().reduce(SnapshotReceived(listOf(preparing, queued)))

    @Test
    fun `reconexao sem mudancas no servidor mantem a mesma instancia de estado`() {
        val afterReconnect = beforeDrop.reduce(SnapshotReceived(listOf(preparing, queued)))

        assertSame(beforeDrop, afterReconnect)
    }

    @Test
    fun `pedido cancelado durante a queda gera alerta na reconexao`() {
        val canceledWhileOffline = anOrder(id = 7, stage = Stage.CANCELED, version = 3)

        val state = beforeDrop.reduce(SnapshotReceived(listOf(canceledWhileOffline, queued)))

        assertNull(state.orders[OrderId(7)])
        assertEquals(Stage.PREPARING, state.cancellationAlerts.getValue(OrderId(7)).previousStage)
    }

    @Test
    fun `pedidos criados durante a queda aparecem na reconexao`() {
        val createdWhileOffline = anOrder(id = 9, stage = Stage.PENDING, version = 1)

        val state = beforeDrop.reduce(SnapshotReceived(listOf(preparing, queued, createdWhileOffline)))

        assertEquals(setOf(OrderId(7), OrderId(8), OrderId(9)), state.orders.keys)
    }

    @Test
    fun `acao pendente ainda valida sobrevive a reconexao`() {
        val tapped = beforeDrop.reduce(TransitionRequested(OrderId(8)))

        val state = tapped.reduce(SnapshotReceived(listOf(preparing, queued)))

        assertNotNull(state.pending[OrderId(8)])
        assertEquals(Stage.PREPARING, state.ordersByArrival().single { it.order.id == OrderId(8) }.stage)
    }

    @Test
    fun `acao pendente que o servidor ja aplicou e encerrada pela reconexao`() {
        val inFlight = beforeDrop.reduce(TransitionRequested(OrderId(8)), TransitionSent(OrderId(8)))
        val appliedByServer = anOrder(id = 8, stage = Stage.PREPARING, version = 2)

        val state = inFlight.reduce(SnapshotReceived(listOf(preparing, appliedByServer)))

        assertNull(state.pending[OrderId(8)])
        assertEquals(Stage.PREPARING, state.orders.getValue(OrderId(8)).stage)
    }

    @Test
    fun `pedido ativo ausente do snapshot e removido junto com sua acao pendente`() {
        val tapped = beforeDrop.reduce(TransitionRequested(OrderId(8)))

        val state = tapped.reduce(SnapshotReceived(listOf(preparing)))

        assertEquals(setOf(OrderId(7)), state.orders.keys)
        assertTrue(state.pending.isEmpty())
        assertTrue(state.cancellationAlerts.isEmpty())
    }

    @Test
    fun `evento antigo reenviado depois do snapshot nao regride o pedido`() {
        val readyNow = anOrder(id = 7, stage = Stage.READY, version = 3)

        val state = beforeDrop.reduce(SnapshotReceived(listOf(readyNow, queued)), OrderReceived(preparing))

        assertEquals(Stage.READY, state.orders.getValue(OrderId(7)).stage)
    }
}
