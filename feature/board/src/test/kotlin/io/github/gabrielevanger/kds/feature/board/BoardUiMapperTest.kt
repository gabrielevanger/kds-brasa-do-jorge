package io.github.gabrielevanger.kds.feature.board

import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenEvent
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderReducer
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.testing.BASE_TIME
import io.github.gabrielevanger.kds.core.domain.testing.anItem
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import io.github.gabrielevanger.kds.core.ui.UndoUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BoardUiMapperTest {

    private fun stateOf(vararg orders: Order, events: List<KitchenEvent> = emptyList()): KitchenState =
        (listOf(KitchenEvent.SnapshotReceived(orders.toList())) + events).fold(KitchenState(), OrderReducer::reduce)

    private fun map(state: KitchenState, filter: StationFilter = StationFilter.ALL) =
        BoardUiMapper.map(state, ConnectionState.Connected, filter)

    private fun BoardUiState.column(tone: StageTone) = columns.single { it.tone == tone }.orders

    @Nested
    inner class Colunas {

        @Test
        fun `sempre tres colunas, na ordem da linha de producao`() {
            val board = map(KitchenState())

            assertEquals(listOf(StageTone.QUEUED, StageTone.PREPARING, StageTone.READY), board.columns.map { it.tone })
        }

        @Test
        fun `pendente e confirmado ficam juntos na fila`() {
            val board = map(stateOf(anOrder(id = 1, stage = Stage.PENDING), anOrder(id = 2, stage = Stage.CONFIRMED)))

            assertEquals(listOf(OrderId(1), OrderId(2)), board.column(StageTone.QUEUED).map { it.id })
        }

        @Test
        fun `cada coluna mantem a ordem de chegada`() {
            val board = map(
                stateOf(
                    anOrder(id = 2, stage = Stage.PREPARING, createdAt = BASE_TIME.plusSeconds(60)),
                    anOrder(id = 1, stage = Stage.PREPARING, createdAt = BASE_TIME),
                ),
            )

            assertEquals(listOf(OrderId(1), OrderId(2)), board.column(StageTone.PREPARING).map { it.id })
        }

        @Test
        fun `toque otimista ja mostra o pedido na proxima coluna e marca a espera do servidor`() {
            val state = stateOf(anOrder(id = 1), events = listOf(KitchenEvent.TransitionRequested(OrderId(1))))

            val card = map(state).column(StageTone.PREPARING).single()

            assertEquals(OrderId(1), card.id)
            assertTrue(card.isAwaitingServer)
            assertTrue(map(state).column(StageTone.QUEUED).isEmpty())
        }
    }

    @Nested
    inner class `Filtro por estacao` {

        private val combo = anOrder(
            id = 1,
            items = listOf(
                anItem(name = "Smash Bacon", quantity = 2, productionArea = ProductionArea.CHAPA),
                anItem(name = "Batata Rústica", quantity = 3, productionArea = ProductionArea.FRITADEIRA),
            ),
        )
        private val drinkOnly =
            anOrder(id = 2, items = listOf(anItem(name = "Coca-Cola Lata", productionArea = ProductionArea.MONTAGEM)))

        @Test
        fun `filtro mostra so os itens da estacao, mas o tamanho do pedido inteiro`() {
            val card = map(stateOf(combo, drinkOnly), StationFilter.CHAPA).column(StageTone.QUEUED).single()

            assertEquals(listOf("Smash Bacon"), card.items.map { it.name })
            assertEquals(5, card.itemCount)
            assertTrue(card.isLarge)
        }

        @Test
        fun `pedido sem itens da estacao filtrada nao aparece`() {
            val queue = map(stateOf(combo, drinkOnly), StationFilter.FRITADEIRA).column(StageTone.QUEUED)

            assertEquals(listOf(OrderId(1)), queue.map { it.id })
        }

        @Test
        fun `sem filtro o pedido aparece inteiro`() {
            val card = map(stateOf(combo)).column(StageTone.QUEUED).single()

            assertEquals(2, card.items.size)
        }
    }

    @Nested
    inner class `Desfazer e alertas` {

        @Test
        fun `oferece desfazer o toque mais recente ainda dentro da janela`() {
            val state = stateOf(
                anOrder(id = 1),
                anOrder(id = 2),
                events = listOf(
                    KitchenEvent.TransitionRequested(OrderId(1)),
                    KitchenEvent.TransitionRequested(OrderId(2)),
                ),
            )

            assertEquals(UndoUi(OrderId(2), "#0002", StageTone.PREPARING), map(state).undo)
        }

        @Test
        fun `toque ja enviado ao servidor nao oferece desfazer`() {
            val state = stateOf(
                anOrder(id = 1),
                events = listOf(KitchenEvent.TransitionRequested(OrderId(1)), KitchenEvent.TransitionSent(OrderId(1))),
            )

            assertNull(map(state).undo)
        }

        @Test
        fun `cancelamento de pedido em preparo vira alerta com a etapa em que estava`() {
            val state = stateOf(
                anOrder(id = 1, stage = Stage.PREPARING, version = 2, origin = Origin.POS, table = 7),
                events = listOf(KitchenEvent.OrderReceived(anOrder(id = 1, stage = Stage.CANCELED, version = 3))),
            )

            val alert = map(state).cancellationAlerts.single()

            assertEquals(OrderId(1), alert.id)
            assertEquals(StageTone.PREPARING, alert.previousTone)
            assertEquals(listOf("Smash Clássico"), alert.items.map { it.name })
            assertTrue(map(state).columns.all { it.orders.isEmpty() })
        }
    }
}
