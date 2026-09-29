package io.github.gabrielevanger.kds.feature.board

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.CancellationAlertTags
import io.github.gabrielevanger.kds.core.ui.CancellationAlertUi
import io.github.gabrielevanger.kds.core.ui.ConnectionBannerTags
import io.github.gabrielevanger.kds.core.ui.FeedbackBarTags
import io.github.gabrielevanger.kds.core.ui.OriginLabel
import io.github.gabrielevanger.kds.core.ui.UndoUi
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvPanelScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun card(id: Long, tone: StageTone) = OrderCardUi(
        id = OrderId(id),
        reference = "#%04d".format(id),
        origin = OriginLabel.COUNTER,
        tableNumber = null,
        createdAt = Instant.parse("2026-09-25T23:00:00Z"),
        itemCount = 1,
        isLarge = false,
        items = persistentListOf(),
        note = null,
        tone = tone,
        isAwaitingServer = false,
    )

    /** Estado com tudo o que o tablet mostraria: alerta, desfazer e reconexão. */
    private val busyKitchen = BoardUiState.Initial.copy(
        columns = persistentListOf(
            BoardColumnUi(StageTone.QUEUED, persistentListOf(card(1, StageTone.QUEUED))),
            BoardColumnUi(StageTone.PREPARING, persistentListOf(card(2, StageTone.PREPARING))),
            BoardColumnUi(StageTone.READY, persistentListOf(card(3, StageTone.READY))),
        ),
        connection = ConnectionState.Reconnecting(attempt = 2),
        cancellationAlerts = persistentListOf(
            CancellationAlertUi(
                OrderId(9),
                "#0009",
                OriginLabel.COUNTER,
                null,
                StageTone.PREPARING,
                persistentListOf(),
            ),
        ),
        undo = UndoUi(OrderId(2), "#0002", StageTone.PREPARING),
    )

    private fun show(state: BoardUiState) {
        composeRule.setContent { KdsTheme { TvPanelScreen(state = state) } }
    }

    @Test
    fun mostraAsTresColunasComOsPedidos() {
        show(busyKitchen)

        composeRule.onAllNodesWithTag(OrderCardTags.CARD).assertCountEquals(3)
    }

    /** Somente leitura: na TV não há quem toque, e um toque do controle remoto não pode avançar pedido. */
    @Test
    fun cardsNaoTemBotaoDeAcao() {
        show(busyKitchen)

        composeRule.onAllNodesWithTag(OrderCardTags.ACTION).assertCountEquals(0)
    }

    @Test
    fun naoMostraFiltroNemDesfazer() {
        show(busyKitchen)

        composeRule.onNodeWithText(context.getString(R.string.board_filter_all)).assertDoesNotExist()
        composeRule.onNodeWithTag(FeedbackBarTags.UNDO).assertDoesNotExist()
    }

    /** Ninguém dispensaria o alerta na TV; quem trata o cancelamento é o tablet. */
    @Test
    fun cancelamentoNaoViraAlertaNaTv() {
        show(busyKitchen)

        composeRule.onAllNodesWithTag(CancellationAlertTags.ALERT).assertCountEquals(0)
    }

    @Test
    fun avisaQueOsPedidosPodemEstarDesatualizados() {
        show(busyKitchen)

        composeRule.onNodeWithTag(ConnectionBannerTags.BANNER).assertExists()
    }
}
