package io.github.gabrielevanger.kds.core.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun show(connection: ConnectionState) {
        composeRule.setContent { KdsTheme { ConnectionBanner(connection) } }
    }

    @Test
    fun conectadoNaoMostraFaixa() {
        show(ConnectionState.Connected)

        composeRule.onNodeWithTag(ConnectionBannerTags.BANNER).assertDoesNotExist()
    }

    /** Sem a faixa, a tela vazia diria "Nenhum pedido" antes mesmo de os dados chegarem. */
    @Test
    fun conectandoAvisaQueOsDadosAindaNaoChegaram() {
        show(ConnectionState.Connecting)

        composeRule.onNodeWithText(context.getString(R.string.kitchen_connection_connecting)).assertIsDisplayed()
    }

    @Test
    fun reconectandoMostraATentativaEAvisaQueOsPedidosPodemEstarDesatualizados() {
        show(ConnectionState.Reconnecting(attempt = 3))

        composeRule.onNodeWithText(context.getString(R.string.kitchen_connection_reconnecting, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.kitchen_connection_stale)).assertIsDisplayed()
    }
}
