package io.github.gabrielevanger.kds.feature.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.ui.ConnectionBanner
import io.github.gabrielevanger.kds.core.ui.EQUAL_SHARE
import io.github.gabrielevanger.kds.core.ui.KitchenClockProvider

@Composable
fun TvPanelRoute(viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    KitchenClockProvider {
        TvPanelScreen(state = state)
    }
}

/**
 * Painel de TV, somente leitura: a fila da cozinha vista de longe, sem botões, filtro nem desfazer.
 * Cancelamentos não geram alerta nem som aqui; quem os trata é o tablet, onde alguém toca em CIENTE.
 * Na TV ninguém dispensaria o alerta, e ele ficaria na tela para sempre.
 */
@Composable
fun TvPanelScreen(state: BoardUiState, modifier: Modifier = Modifier) {
    val spacing = KdsTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KdsTheme.colors.background)
            .safeDrawingPadding()
            .padding(spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        ConnectionBanner(connection = state.connection)
        BoardColumns(columns = state.columns, onAdvance = null, modifier = Modifier.weight(EQUAL_SHARE))
    }
}
