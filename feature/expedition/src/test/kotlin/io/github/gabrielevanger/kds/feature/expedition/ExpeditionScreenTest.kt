package io.github.gabrielevanger.kds.feature.expedition

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.OriginLabel
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpeditionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun order(id: Long) = ReadyOrderUi(
        id = OrderId(id),
        reference = "#%04d".format(id),
        origin = OriginLabel.COUNTER,
        tableNumber = null,
        readyAt = Instant.parse("2026-09-25T23:00:00Z"),
        mustCharge = false,
        itemCount = 1,
        items = persistentListOf(),
    )

    private fun show(orders: List<ReadyOrderUi>) {
        val state = ExpeditionUiState.Initial.copy(
            orders = orders.toImmutableList(),
            connection = ConnectionState.Connected,
        )
        composeRule.setContent {
            KdsTheme {
                ExpeditionScreen(state = state, notice = null, onDeliver = {}, onUndo = {}, onDismissAlert = {})
            }
        }
    }

    @Test
    fun balcaoVazioDizQueNaoHaPedidoPronto() {
        show(emptyList())

        composeRule.onNodeWithText(context.getString(R.string.expedition_empty)).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    @Test
    fun cabecalhoContaOsPedidosNoBalcao() {
        show(listOf(order(1), order(2)))

        composeRule.onNodeWithText(context.getString(R.string.expedition_title)).assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onAllNodesWithTag(ReadyOrderCardTags.CARD).assertCountEquals(2)
    }
}
