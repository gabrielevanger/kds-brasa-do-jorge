package io.github.gabrielevanger.kds.feature.expedition

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.ui.LocalNow
import io.github.gabrielevanger.kds.core.ui.OrderItemUi
import io.github.gabrielevanger.kds.core.ui.OriginLabel
import io.github.gabrielevanger.kds.core.ui.R as UiR
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Roda na largura padrão do Robolectric, um celular em pé: a tela do garçom. */
@RunWith(AndroidJUnit4::class)
class ReadyOrderCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val readyAt: Instant = Instant.parse("2026-09-25T23:00:00Z")

    private val order = ReadyOrderUi(
        id = OrderId(24),
        reference = "#0024",
        origin = OriginLabel.WEB_MENU,
        tableNumber = null,
        readyAt = readyAt,
        mustCharge = false,
        itemCount = 3,
        items = persistentListOf(
            OrderItemUi("Smash Bacon", 2, "Smash Bacon", persistentListOf(), null),
            OrderItemUi("Batata Rústica", 1, "Batata Rústica", persistentListOf(), null),
        ),
    )

    private fun show(order: ReadyOrderUi, onCounterSeconds: Long = 0, onDeliver: () -> Unit = {}) {
        val now = mutableStateOf(readyAt.plusSeconds(onCounterSeconds))
        composeRule.setContent {
            KdsTheme {
                CompositionLocalProvider(LocalNow provides now) {
                    ReadyOrderCard(order = order, onDeliver = onDeliver)
                }
            }
        }
    }

    @Test
    fun mostraQualPedidoEParaOndeVaiMesmoComOrigemLonga() {
        show(order)

        composeRule.onNodeWithText("#0024").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(UiR.string.kitchen_origin_web_menu)).assertIsDisplayed()
    }

    @Test
    fun pedidoNaoPagoMostraCobrar() {
        show(order.copy(mustCharge = true))

        composeRule.onNodeWithTag(ReadyOrderCardTags.CHARGE).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.expedition_charge)).assertIsDisplayed()
    }

    @Test
    fun pedidoPagoNaoMostraCobrar() {
        show(order)

        composeRule.onNodeWithTag(ReadyOrderCardTags.CHARGE).assertDoesNotExist()
    }

    @Test
    fun mostraHaQuantoTempoOPedidoEstaNoBalcao() {
        show(order, onCounterSeconds = SIX_MINUTES_TWELVE_SECONDS)

        composeRule.onNodeWithText(context.getString(R.string.expedition_on_counter)).assertIsDisplayed()
        composeRule.onNodeWithText("06:12").assertIsDisplayed()
    }

    @Test
    fun listaOQueConferirNaSacola() {
        show(order)

        composeRule.onNodeWithText(context.getString(UiR.string.kitchen_item_summary, 2, "Smash Bacon")).assertExists()
        composeRule.onNodeWithText(
            context.getString(UiR.string.kitchen_item_summary, 1, "Batata Rústica"),
        ).assertExists()
    }

    @Test
    fun entregueEntregaOPedido() {
        var delivered = 0
        show(order, onDeliver = { delivered++ })

        composeRule.onNodeWithText(context.getString(R.string.expedition_deliver)).performClick()

        assertEquals(1, delivered)
    }

    private companion object {
        const val SIX_MINUTES_TWELVE_SECONDS = 6 * 60L + 12
    }
}
