package io.github.gabrielevanger.kds.feature.board

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val undo = UndoUi(OrderId(5), "#0005", StageTone.PREPARING)
    private val notice = BoardNotice(BoardNotice.Kind.NOT_SENT, "#0005", StageTone.QUEUED)

    private fun show(notice: BoardNotice?, undo: UndoUi?, onUndo: (UndoUi) -> Unit = {}) {
        composeRule.setContent {
            KdsTheme { FeedbackBar(notice = notice, undo = undo, onUndo = onUndo) }
        }
    }

    private fun columnTitle(resId: Int) = context.getString(resId)

    @Test
    fun desfazerMostraOPedidoEAEtapaParaOndeFoi() {
        show(notice = null, undo = undo)

        val message = context.getString(
            R.string.board_undo_message,
            "#0005",
            columnTitle(R.string.board_column_preparing),
        )
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun tocarEmDesfazerDesfazOToqueDaquelePedido() {
        val undone = mutableListOf<UndoUi>()
        show(notice = null, undo = undo, onUndo = { undone += it })

        composeRule.onNodeWithText(context.getString(R.string.board_undo_action)).performClick()

        assertEquals(listOf(undo), undone)
    }

    @Test
    fun falhaDeEnvioTemPrioridadeSobreODesfazer() {
        show(notice = notice, undo = undo)

        val message = context.getString(
            R.string.board_notice_not_sent,
            "#0005",
            columnTitle(R.string.board_column_queued),
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
