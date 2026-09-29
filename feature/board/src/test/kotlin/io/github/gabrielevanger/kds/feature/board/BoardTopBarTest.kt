package io.github.gabrielevanger.kds.feature.board

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.ui.KitchenTopBarTags
import io.github.gabrielevanger.kds.core.ui.R as UiR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A barra divide uma linha só no tablet: nome, os quatro filtros, conexão e hora precisam caber. */
@RunWith(AndroidJUnit4::class)
class BoardTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun barraDoTabletMostraNomeFiltrosConexaoEHora() {
        composeRule.setContent {
            KdsTheme {
                BoardScreen(
                    state = BoardUiState.Initial.copy(connection = ConnectionState.Connected),
                    notice = null,
                    onAdvance = {},
                    onUndo = {},
                    onDismissAlert = {},
                    onStationFilterSelected = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(UiR.string.kitchen_brand)).assertIsDisplayed()
        StationFilter.entries.forEach { filter ->
            composeRule.onNodeWithText(context.getString(filter.labelRes())).assertIsDisplayed()
        }
        composeRule.onNodeWithTag(KitchenTopBarTags.STATUS).assertIsDisplayed()
        composeRule.onNodeWithTag(KitchenTopBarTags.CLOCK).assertIsDisplayed()
    }

    private fun StationFilter.labelRes(): Int = when (this) {
        StationFilter.ALL -> R.string.board_filter_all
        StationFilter.CHAPA -> R.string.board_filter_chapa
        StationFilter.FRITADEIRA -> R.string.board_filter_fritadeira
        StationFilter.MONTAGEM -> R.string.board_filter_montagem
    }
}
