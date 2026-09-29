package io.github.gabrielevanger.kds.core.domain.kitchen

import app.cash.turbine.test
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.OrderCommands
import io.github.gabrielevanger.kds.core.domain.sync.OrderStream
import io.github.gabrielevanger.kds.core.domain.sync.StageChangeResult
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Store com fakes e tempo virtual: a janela de 5 s de desfazer passa sem espera real. */
class OrderStoreTest {

    private val id = OrderId(7)
    private val queued = anOrder(id = 7, stage = Stage.PENDING, version = 1)
    private val preparing = anOrder(id = 7, stage = Stage.PREPARING, version = 2)

    private class FakeOrderStream : OrderStream {
        val events = MutableSharedFlow<StreamEvent>(replay = 16)
        override fun events(): Flow<StreamEvent> = events
    }

    private class FakeOrderCommands(var result: StageChangeResult) : OrderCommands {
        val calls = mutableListOf<Pair<OrderId, Stage>>()
        override suspend fun changeStage(orderId: OrderId, to: Stage): StageChangeResult {
            calls += orderId to to
            return result
        }
    }

    private val stream = FakeOrderStream()
    private val commands = FakeOrderCommands(StageChangeResult.Confirmed(preparing))

    private fun TestScope.startedStore(): OrderStore {
        val store = OrderStore(stream, commands, backgroundScope)
        store.start()
        stream.events.tryEmit(StreamEvent.Snapshot(listOf(queued)))
        runCurrent()
        return store
    }

    private fun OrderStore.displayedStage(): Stage = state.value.ordersByArrival().single { it.order.id == id }.stage

    @Test
    fun `eventos do stream chegam ao estado e a conexao e exposta`() = runTest {
        val store = startedStore()

        stream.events.tryEmit(StreamEvent.Connection(ConnectionState.Reconnecting(2)))
        stream.events.tryEmit(StreamEvent.OrderChanged(preparing))
        runCurrent()

        assertEquals(ConnectionState.Reconnecting(2), store.connection.value)
        assertEquals(Stage.PREPARING, store.state.value.orders.getValue(id).stage)
    }

    @Test
    fun `toque muda a tela na hora e so envia depois da janela de desfazer`() = runTest {
        val store = startedStore()

        store.advance(id)
        assertEquals(Stage.PREPARING, store.displayedStage())

        advanceTimeBy(4_999)
        assertTrue(commands.calls.isEmpty())

        advanceTimeBy(2)
        assertEquals(listOf(id to Stage.PREPARING), commands.calls)
        assertTrue(store.state.value.pending.isEmpty())
    }

    @Test
    fun `desfazer dentro da janela nao envia nada ao servidor`() = runTest {
        val store = startedStore()

        store.advance(id)
        advanceTimeBy(3_000)
        store.undo(id)
        advanceTimeBy(10_000)

        assertTrue(commands.calls.isEmpty())
        assertEquals(Stage.PENDING, store.displayedStage())
    }

    /**
     * Sem cancelar o agendamento anterior, o envio do primeiro toque encontraria a pendência do
     * segundo e enviaria antes de a janela do segundo toque terminar.
     */
    @Test
    fun `tocar de novo depois de desfazer respeita a janela completa do novo toque`() = runTest {
        val store = startedStore()

        store.advance(id)
        advanceTimeBy(1_000)
        store.advance(id)
        advanceTimeBy(1_000)
        store.undo(id)
        advanceTimeBy(1_000)
        store.advance(id)

        advanceTimeBy(4_999)
        assertTrue(commands.calls.isEmpty(), "enviou antes da janela do último toque terminar")
        advanceTimeBy(2)
        assertEquals(1, commands.calls.size)
    }

    @Test
    fun `toque duplo agenda um unico envio`() = runTest {
        val store = startedStore()

        store.advance(id)
        advanceTimeBy(1_000)
        store.advance(id)
        advanceTimeBy(10_000)

        assertEquals(1, commands.calls.size)
    }

    @Test
    fun `pedido alterado por outro aparelho durante a janela nao e enviado`() = runTest {
        val store = startedStore()

        store.advance(id)
        stream.events.tryEmit(StreamEvent.OrderChanged(preparing))
        advanceTimeBy(10_000)

        assertTrue(commands.calls.isEmpty())
        assertEquals(Stage.PREPARING, store.displayedStage())
    }

    @Test
    fun `desfazer depois do envio nao tem efeito`() = runTest {
        val store = startedStore()
        commands.result = StageChangeResult.Confirmed(preparing)

        store.advance(id)
        advanceTimeBy(5_001)
        store.undo(id)

        assertEquals(1, commands.calls.size)
        assertEquals(Stage.PREPARING, store.displayedStage())
    }

    @Test
    fun `rejeicao aplica o pedido do servidor e avisa a tela`() = runTest {
        val store = startedStore()
        val readyElsewhere = anOrder(id = 7, stage = Stage.READY, version = 3)
        commands.result = StageChangeResult.Rejected(readyElsewhere)

        store.notices.test {
            store.advance(id)
            advanceTimeBy(5_001)

            assertEquals(StoreNotice.TransitionRejected(id), awaitItem())
        }
        assertEquals(Stage.READY, store.displayedStage())
        assertNull(store.state.value.pending[id])
    }

    @Test
    fun `falha no envio devolve o card e avisa que a acao nao foi enviada`() = runTest {
        val store = startedStore()
        commands.result = StageChangeResult.Failed(IOException("sem rede"))

        store.notices.test {
            store.advance(id)
            advanceTimeBy(5_001)

            assertEquals(StoreNotice.TransitionNotSent(id), awaitItem())
        }
        assertEquals(Stage.PENDING, store.displayedStage())
    }
}
