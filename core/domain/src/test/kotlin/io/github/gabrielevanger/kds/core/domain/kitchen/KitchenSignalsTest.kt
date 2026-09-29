package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.OrderReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.SnapshotReceived
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionRequested
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent.TransitionUndone
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KitchenSignalsTest {

    private val queued = anOrder(id = 1, stage = Stage.PENDING)
    private val preparing = anOrder(id = 2, stage = Stage.PREPARING, version = 2)
    private val loaded = KitchenState().reduce(SnapshotReceived(listOf(queued, preparing)))

    private fun KitchenState.reduce(vararg events: KitchenEvent): KitchenState = events.fold(this, OrderReducer::reduce)

    private fun signalsAfter(before: KitchenState, vararg events: KitchenEvent) =
        KitchenSignals.between(before, before.reduce(*events))

    @Test
    fun `primeiro snapshot ao abrir o app nao toca nada`() {
        assertEquals(emptySet<KitchenSignal>(), KitchenSignals.between(KitchenState(), loaded))
    }

    @Test
    fun `primeiro snapshot marca que a cozinha ja tem dados`() {
        assertFalse(KitchenState().hasSnapshot)
        assertTrue(loaded.hasSnapshot)
    }

    @Test
    fun `pedido novo toca o aviso de pedido novo`() {
        assertEquals(setOf(KitchenSignal.NEW_ORDER), signalsAfter(loaded, OrderReceived(anOrder(id = 3))))
    }

    @Test
    fun `cozinha vazia depois do primeiro snapshot tambem avisa o primeiro pedido`() {
        val emptyKitchen = KitchenState().reduce(SnapshotReceived(emptyList()))

        assertEquals(setOf(KitchenSignal.NEW_ORDER), signalsAfter(emptyKitchen, OrderReceived(anOrder(id = 3))))
    }

    @Test
    fun `reconexao com os mesmos pedidos nao toca nada`() {
        assertEquals(emptySet<KitchenSignal>(), signalsAfter(loaded, SnapshotReceived(listOf(queued, preparing))))
    }

    @Test
    fun `reconexao trazendo varios pedidos criados na queda toca um unico aviso`() {
        val createdWhileOffline = (3L..6L).map { anOrder(id = it) }

        val signals = signalsAfter(loaded, SnapshotReceived(listOf(queued, preparing) + createdWhileOffline))

        assertEquals(setOf(KitchenSignal.NEW_ORDER), signals)
    }

    /** A quantidade não muda quando um pedido sai e outro entra na mesma atualização. */
    @Test
    fun `reconexao em que um pedido saiu e outro chegou avisa o pedido novo`() {
        val signals = signalsAfter(loaded, SnapshotReceived(listOf(preparing, anOrder(id = 3))))

        assertEquals(setOf(KitchenSignal.NEW_ORDER), signals)
    }

    /** Um alerta sem CIENTE não pode virar sirene: o alarme toca na chegada do cancelamento, não a cada evento. */
    @Test
    fun `alerta ainda na tela nao repete o alarme nos eventos seguintes`() {
        val alerted = loaded.reduce(OrderReceived(anOrder(id = 2, stage = Stage.CANCELED, version = 3)))

        assertEquals(setOf(KitchenSignal.NEW_ORDER), signalsAfter(alerted, OrderReceived(anOrder(id = 3))))
        assertEquals(emptySet<KitchenSignal>(), signalsAfter(alerted, TransitionRequested(OrderId(1))))
    }

    @Test
    fun `pedido que muda de coluna ou toque desfeito nao toca nada`() {
        assertEquals(emptySet<KitchenSignal>(), signalsAfter(loaded, TransitionRequested(OrderId(1))))
        assertEquals(
            emptySet<KitchenSignal>(),
            signalsAfter(loaded, TransitionRequested(OrderId(1)), TransitionUndone(OrderId(1))),
        )
        assertEquals(
            emptySet<KitchenSignal>(),
            signalsAfter(loaded, OrderReceived(anOrder(id = 1, stage = Stage.PREPARING, version = 2))),
        )
    }

    @Test
    fun `cancelamento de pedido iniciado toca o alarme`() {
        val canceled = anOrder(id = 2, stage = Stage.CANCELED, version = 3)

        assertEquals(setOf(KitchenSignal.CANCELLATION), signalsAfter(loaded, OrderReceived(canceled)))
    }

    @Test
    fun `cancelamento de pedido ainda na fila nao toca alarme`() {
        val canceled = anOrder(id = 1, stage = Stage.CANCELED, version = 2)

        assertEquals(emptySet<KitchenSignal>(), signalsAfter(loaded, OrderReceived(canceled)))
    }

    @Test
    fun `dispensar o alerta nao toca nada`() {
        val alerted = loaded.reduce(OrderReceived(anOrder(id = 2, stage = Stage.CANCELED, version = 3)))

        assertEquals(
            emptySet<KitchenSignal>(),
            signalsAfter(alerted, KitchenEvent.CancellationAlertDismissed(OrderId(2))),
        )
    }
}
