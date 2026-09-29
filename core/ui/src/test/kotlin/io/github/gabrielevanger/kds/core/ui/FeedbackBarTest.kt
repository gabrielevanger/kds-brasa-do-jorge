package io.github.gabrielevanger.kds.core.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class FeedbackBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val undo = UndoUi(OrderId(5), "#0005", StageTone.PREPARING)
    private val notice = KitchenNotice(KitchenNotice.Kind.NOT_SENT, "#0005", StageTone.QUEUED)

    private fun show(notice: KitchenNotice?, undo: UndoUi?, onUndo: (UndoUi) -> Unit = {}) {
        composeRule.setContent {
            KdsTheme { FeedbackBar(notice = notice, undo = undo, onUndo = onUndo) }
        }
    }

    private fun stageTitle(resId: Int) = context.getString(resId)

    @Test
    fun desfazerMostraOPedidoEAEtapaParaOndeFoi() {
        show(notice = null, undo = undo)

        val message = context.getString(
            R.string.kitchen_undo_message,
            "#0005",
            stageTitle(R.string.kitchen_stage_preparing),
        )
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun entregaMostraQueOPedidoFoiEntregue() {
        show(notice = null, undo = UndoUi(OrderId(8), "#0008", targetTone = null))

        composeRule.onNodeWithText(context.getString(R.string.kitchen_undo_delivered, "#0008")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.kitchen_undo_action)).assertIsDisplayed()
    }

    @Test
    fun tocarEmDesfazerDesfazOToqueDaquelePedido() {
        val undone = mutableListOf<UndoUi>()
        show(notice = null, undo = undo, onUndo = { undone += it })

        composeRule.onNodeWithText(context.getString(R.string.kitchen_undo_action)).performClick()

        assertEquals(listOf(undo), undone)
    }

    @Test
    fun noCelularDesfazerFicaAbaixoDaMensagem() {
        show(notice = null, undo = undo)

        val (message, button) = messageAndButtonBounds()

        assertTrue(button.top >= message.bottom)
    }

    @Test
    @Config(qualifiers = TABLET_QUALIFIERS)
    fun noTabletDesfazerFicaAoLadoDaMensagem() {
        show(notice = null, undo = undo)

        val (message, button) = messageAndButtonBounds()

        assertTrue(button.left >= message.right)
    }

    private fun messageAndButtonBounds(): Pair<DpRect, DpRect> {
        val text = context.getString(
            R.string.kitchen_undo_message,
            "#0005",
            stageTitle(R.string.kitchen_stage_preparing),
        )
        val message = composeRule.onNodeWithText(text)
        val button = composeRule.onNodeWithText(context.getString(R.string.kitchen_undo_action))
        return message.getUnclippedBoundsInRoot() to button.getUnclippedBoundsInRoot()
    }

    @Test
    fun falhaDeEnvioTemPrioridadeSobreODesfazer() {
        show(notice = notice, undo = undo)

        val message = context.getString(
            R.string.kitchen_notice_not_sent,
            "#0005",
            stageTitle(R.string.kitchen_stage_queued),
        )
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithTag(FeedbackBarTags.UNDO).assertDoesNotExist()
    }

    @Test
    fun semToqueRecenteNemAvisoABarraNaoAparece() {
        show(notice = null, undo = null)

        composeRule.onNodeWithTag(FeedbackBarTags.UNDO).assertDoesNotExist()
        composeRule.onNodeWithTag(FeedbackBarTags.NOTICE).assertDoesNotExist()
    }
}
