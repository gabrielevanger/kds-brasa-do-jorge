package io.github.gabrielevanger.kds.feature.board

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.R as DesignR
import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * O card é o componente central do KDS: tudo o que a cozinha precisa saber de um pedido está nele.
 * Roda na JVM com Robolectric, sem emulador, em todo PR.
 */
@RunWith(AndroidJUnit4::class)
class OrderCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val createdAt: Instant = Instant.parse("2026-09-25T23:00:00Z")

    private val card = OrderCardUi(
        id = OrderId(7),
        reference = "#0007",
        origin = OriginLabel.TABLE,
        tableNumber = 4,
        createdAt = createdAt,
        itemCount = 5,
        isLarge = true,
        items = persistentListOf(
            OrderItemUi(
                key = "7-0",
                quantity = 2,
                name = "Smash Bacon",
                modifiers = persistentListOf(ModifierUi(ModifierKind.REMOVE, "Sem cebola")),
                note = "Caprichar no ponto",
            ),
        ),
        note = null,
        tone = StageTone.QUEUED,
        isAwaitingServer = false,
    )

    private fun show(card: OrderCardUi, waitedSeconds: Long = 0, onAdvance: () -> Unit = {}) {
        val now = mutableStateOf(createdAt.plusSeconds(waitedSeconds))
        composeRule.setContent {
            KdsTheme {
                CompositionLocalProvider(LocalNow provides now) {
                    OrderCard(card = card, onAdvance = onAdvance)
                }
            }
        }
    }

    @Test
    fun mostraDeQuemEParaOndeVaiOPedido() {
        show(card)

        composeRule.onNodeWithText("#0007").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.board_origin_table, 4)).assertIsDisplayed()
    }

    @Test
    fun modificadoresEObservacaoAparecemAbaixoDoItemSemAbrirNada() {
        show(card)

        composeRule.onNodeWithText("Smash Bacon").assertIsDisplayed()
        composeRule.onNodeWithText("Sem cebola").assertIsDisplayed()
        composeRule.onNodeWithText("Caprichar no ponto").assertIsDisplayed()
    }

    @Test
    fun tamanhoDoPedidoEDestaqueDePedidoGrande() {
        show(card)

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.board_item_count, 5, 5),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.board_large_order)).assertIsDisplayed()
    }

    /** O estado precisa ser entendido sem depender de cor: o texto do nível aparece junto do tempo. */
    @Test
    fun atrasoApareceComoTextoAlemDaCor() {
        show(card, waitedSeconds = 16 * 60 + 40)

        composeRule.onNodeWithTag(OrderCardTags.TIMER, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(DesignR.string.wait_late)).assertIsDisplayed()
        composeRule.onNodeWithText("16:40").assertIsDisplayed()
    }

    @Test
    fun atencaoApareceComoTextoAPartirDeOitoMinutos() {
        show(card, waitedSeconds = 8 * 60)

        composeRule.onNodeWithText(context.getString(DesignR.string.wait_attention)).assertIsDisplayed()
    }

    @Test
    fun esperaNormalNaoExibeRotuloDeAlerta() {
        show(card, waitedSeconds = 4 * 60)

        composeRule.onNodeWithText("04:00").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(DesignR.string.wait_attention)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(DesignR.string.wait_late)).assertDoesNotExist()
    }

    @Test
    fun umToqueNoBotaoAvancaOPedidoUmaUnicaVez() {
        var advances = 0
        show(card, onAdvance = { advances++ })

        composeRule.onNodeWithTag(OrderCardTags.ACTION)
            .assertTextContains(context.getString(R.string.board_action_start))
            .assertIsEnabled()
            .performClick()

        assertEquals(1, advances)
    }

    @Test
    fun botaoFicaDesabilitadoEnquantoOToqueAguardaOServidor() {
        var advances = 0
        show(card.copy(tone = StageTone.PREPARING, isAwaitingServer = true), onAdvance = { advances++ })

        composeRule.onNodeWithTag(OrderCardTags.ACTION)
            .assertTextContains(context.getString(R.string.board_action_ready))
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, advances)
    }
}
