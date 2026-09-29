package io.github.gabrielevanger.kds.feature.board

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A LazyColumn mantém no lugar o primeiro item visível quando entra um item acima dele. Num KDS isso
 * esconderia justamente o pedido mais antigo, o mais urgente: um pedido desfeito, recusado ou vindo
 * da reconexão ficaria fora da tela, acima do topo.
 */
@RunWith(AndroidJUnit4::class)
class BoardColumnScrollTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-25T23:30:00Z")

    private fun card(id: Long) = OrderCardUi(
        id = OrderId(id),
        reference = "#%04d".format(id),
        origin = OriginLabel.COUNTER,
        tableNumber = null,
        createdAt = now.minusSeconds(ONE_MINUTE_IN_SECONDS * (NEWEST_ORDER_ID - id)),
        itemCount = 1,
        isLarge = false,
        items = persistentListOf(OrderItemUi("$id-0", 1, "Smash Clássico", persistentListOf(), null)),
        note = null,
        tone = StageTone.QUEUED,
        isAwaitingServer = false,
    )

    private fun boardWithQueue(ids: LongRange) = BoardUiState.Initial.copy(
        columns = persistentListOf(
            BoardColumnUi(StageTone.QUEUED, ids.map(::card).toImmutableList()),
            BoardColumnUi(StageTone.PREPARING, persistentListOf()),
            BoardColumnUi(StageTone.READY, persistentListOf()),
        ),
    )

    private fun showBoard(state: MutableState<BoardUiState>) {
        val clock = mutableStateOf(now)
        composeRule.setContent {
            KdsTheme {
                CompositionLocalProvider(LocalNow provides clock) {
                    BoardScreen(
                        state = state.value,
                        notice = null,
                        onAdvance = {},
                        onUndo = {},
                        onStationFilterSelected = {},
                    )
                }
            }
        }
    }

    @Test
    fun pedidoMaisAntigoQueVoltaParaAFilaApareceNoTopo() {
        val state = mutableStateOf(boardWithQueue(6L..NEWEST_ORDER_ID))
        showBoard(state)
        composeRule.onNodeWithText("#0006").assertIsDisplayed()

        state.value = boardWithQueue(5L..NEWEST_ORDER_ID)

        composeRule.onNodeWithText("#0005").assertIsDisplayed()
    }

    /** Quem rolou para ler pedidos mais abaixo não pode ter a tela puxada para o topo. */
    @Test
    fun colunaRoladaParaBaixoMantemAPosicaoQuandoEntraPedidoAcima() {
        val state = mutableStateOf(boardWithQueue(6L..NEWEST_ORDER_ID))
        showBoard(state)
        composeRule.onNodeWithTag(BoardTags.column(StageTone.QUEUED)).performScrollToIndex(SCROLLED_CARD_INDEX)
        composeRule.onNodeWithText("#0009").assertIsDisplayed()

        state.value = boardWithQueue(5L..NEWEST_ORDER_ID)

        composeRule.onNodeWithText("#0009").assertIsDisplayed()
        composeRule.onNodeWithText("#0005").assertDoesNotExist()
    }

    private companion object {
        const val ONE_MINUTE_IN_SECONDS = 60L
        const val NEWEST_ORDER_ID = 10L

        /** Quarto card da fila inicial (#0009). */
        const val SCROLLED_CARD_INDEX = 3
    }
}
