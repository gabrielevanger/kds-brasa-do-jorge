package io.github.gabrielevanger.kds.core.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TABLET_QUALIFIERS)
class KitchenTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val now = mutableStateOf(Instant.parse("2026-09-25T23:05:42Z"))

    private fun show(connection: ConnectionState) {
        composeRule.setContent {
            KdsTheme {
                CompositionLocalProvider(LocalNow provides now) {
                    KitchenTopBar(connection = connection, zone = ZoneOffset.UTC)
                }
            }
        }
    }

    /** Conectado também aparece: sem isso, ninguém sabe se a tela parada está viva ou travada. */
    @Test
    fun conectadoMostraQueATelaEstaAoVivo() {
        show(ConnectionState.Connected)

        composeRule.onNodeWithText(context.getString(R.string.kitchen_status_live)).assertIsDisplayed()
    }

    @Test
    fun conectandoAvisaQueAindaNaoHaDados() {
        show(ConnectionState.Connecting)

        composeRule.onNodeWithText(context.getString(R.string.kitchen_status_connecting)).assertIsDisplayed()
    }

    @Test
    fun reconectandoAvisaQueEstaSemConexao() {
        show(ConnectionState.Reconnecting(attempt = 2))

        composeRule.onNodeWithText(context.getString(R.string.kitchen_status_offline)).assertIsDisplayed()
    }

    @Test
    fun relogioMostraHorasEMinutosNoFusoDoAparelho() {
        show(ConnectionState.Connected)

        composeRule.onNodeWithTag(KitchenTopBarTags.CLOCK).assertTextEquals("23:05")
    }

    @Test
    fun relogioAvancaNaViradaDoMinuto() {
        show(ConnectionState.Connected)

        now.value = Instant.parse("2026-09-25T23:05:59Z")
        composeRule.onNodeWithTag(KitchenTopBarTags.CLOCK).assertTextEquals("23:05")

        now.value = Instant.parse("2026-09-25T23:06:00Z")
        composeRule.onNodeWithTag(KitchenTopBarTags.CLOCK).assertTextEquals("23:06")
    }
}
