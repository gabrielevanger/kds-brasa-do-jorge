package io.github.gabrielevanger.kds.core.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class CancellationAlertsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun alert(id: Long, previousTone: StageTone = StageTone.PREPARING) = CancellationAlertUi(
        id = OrderId(id),
        reference = "#%04d".format(id),
        origin = OriginLabel.TABLE,
        tableNumber = 4,
        previousTone = previousTone,
        items = persistentListOf(
            OrderItemUi("$id-0", 2, "Smash Bacon", persistentListOf(), null),
            OrderItemUi("$id-1", 1, "Batata Rústica", persistentListOf(), null),
        ),
    )

    private fun show(alerts: ImmutableList<CancellationAlertUi>, onDismiss: (OrderId) -> Unit = {}) {
        composeRule.setContent {
            KdsTheme { CancellationAlerts(alerts = alerts, onDismiss = onDismiss) }
        }
    }

    @Test
    fun alertaDizQualPedidoParar() {
        show(persistentListOf(alert(9)))

        composeRule.onNodeWithText(context.getString(R.string.kitchen_alert_title, "#0009")).assertIsDisplayed()
    }

    /** O lanche pronto já foi feito: o risco agora é alguém entregá-lo. */
    @Test
    fun pedidoCanceladoDepoisDeProntoPedeParaNaoEntregar() {
        show(persistentListOf(alert(9, StageTone.READY)))

        composeRule.onNodeWithText(context.getString(R.string.kitchen_alert_title_ready, "#0009")).assertIsDisplayed()
    }

    @Test
    fun alertaDizEmQueEtapaEstavaEDeOndeVeio() {
        show(persistentListOf(alert(9)))

        val context = context.getString(
            R.string.kitchen_alert_context,
            context.getString(R.string.kitchen_stage_preparing),
            context.getString(R.string.kitchen_origin_table, 4),
        )
        composeRule.onNodeWithText(context).assertIsDisplayed()
    }

    /** O card saiu da tela: sem os itens no alerta, a cozinha não saberia o que parar de fazer. */
    @Test
    fun alertaListaOQueEraOPedido() {
        show(persistentListOf(alert(9)))

        composeRule.onNodeWithText("Smash Bacon", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Batata Rústica", substring = true).assertIsDisplayed()
    }

    @Test
    fun cienteDispensaSomenteOAlertaTocado() {
        val dismissed = mutableListOf<OrderId>()
        show(persistentListOf(alert(9), alert(12, StageTone.READY)), onDismiss = { dismissed += it })

        composeRule.onAllNodesWithText(context.getString(R.string.kitchen_alert_dismiss))[1].performClick()

        assertEquals(listOf(OrderId(12)), dismissed)
    }

    @Test
    fun variosCancelamentosAparecemTodosDeUmaVez() {
        show(persistentListOf(alert(9), alert(12), alert(15)))

        composeRule.onAllNodesWithTag(CancellationAlertTags.ALERT).assertCountEquals(3)
    }

    /** No celular em pé, o botão ao lado espremeria o texto até ele sumir. */
    @Test
    fun noCelularCienteFicaAbaixoDoTexto() {
        show(persistentListOf(alert(9)))

        val (title, button) = titleAndButtonBounds()

        assertTrue(button.top >= title.bottom)
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun noTabletCienteFicaAoLadoDoTexto() {
        show(persistentListOf(alert(9)))

        val (title, button) = titleAndButtonBounds()

        assertTrue(button.left >= title.right)
    }

    private fun titleAndButtonBounds(): Pair<DpRect, DpRect> {
        val title = composeRule.onNodeWithText(context.getString(R.string.kitchen_alert_title, "#0009"))
        val button = composeRule.onNodeWithText(context.getString(R.string.kitchen_alert_dismiss))
        return title.getUnclippedBoundsInRoot() to button.getUnclippedBoundsInRoot()
    }

    @Test
    fun semCancelamentosNadaAparece() {
        show(persistentListOf())

        composeRule.onAllNodesWithTag(CancellationAlertTags.ALERT).assertCountEquals(0)
    }
}
