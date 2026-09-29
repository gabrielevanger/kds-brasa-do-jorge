package io.github.gabrielevanger.kds.feature.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.kds.core.designsystem.component.StageHeader
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.component.visual
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.ui.CancellationAlerts
import io.github.gabrielevanger.kds.core.ui.ConnectionBanner
import io.github.gabrielevanger.kds.core.ui.EQUAL_SHARE
import io.github.gabrielevanger.kds.core.ui.FeedbackBar
import io.github.gabrielevanger.kds.core.ui.KitchenClockProvider
import io.github.gabrielevanger.kds.core.ui.KitchenNotice
import io.github.gabrielevanger.kds.core.ui.KitchenSoundEffect
import io.github.gabrielevanger.kds.core.ui.UndoUi
import io.github.gabrielevanger.kds.core.ui.rememberVisibleNotice

/** Tags usadas pelos testes de interface. */
object ExpeditionTags {
    const val LIST = "expedition_list"
}

@Composable
fun ExpeditionRoute(viewModel: ExpeditionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by rememberVisibleNotice(viewModel.notices)
    KitchenSoundEffect(signals = viewModel.signals, vibrate = true)
    KitchenClockProvider {
        ExpeditionScreen(
            state = state,
            notice = notice,
            onDeliver = viewModel::onDeliver,
            onUndo = { undo -> viewModel.onUndo(undo.orderId) },
            onDismissAlert = viewModel::onDismissAlert,
        )
    }
}

/** Expedição no celular do garçom, em pé: uma lista só, do lanche que espera há mais tempo ao mais novo. */
@Composable
fun ExpeditionScreen(
    state: ExpeditionUiState,
    notice: KitchenNotice?,
    onDeliver: (OrderId) -> Unit,
    onUndo: (UndoUi) -> Unit,
    onDismissAlert: (OrderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KdsTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KdsTheme.colors.background)
            .safeDrawingPadding()
            .padding(spacing.s),
        verticalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        StageHeader(
            tone = StageTone.READY,
            title = stringResource(R.string.expedition_title),
            count = state.orders.size,
        )
        ConnectionBanner(connection = state.connection)
        CancellationAlerts(alerts = state.cancellationAlerts, onDismiss = onDismissAlert)
        Box(modifier = Modifier.weight(EQUAL_SHARE)) {
            if (state.orders.isEmpty()) {
                Text(
                    text = stringResource(R.string.expedition_empty),
                    style = KdsTheme.typography.body,
                    color = KdsTheme.colors.onSurfaceMuted,
                    modifier = Modifier.padding(spacing.m),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.s),
                    modifier = Modifier.testTag(ExpeditionTags.LIST),
                ) {
                    items(items = state.orders, key = { it.id.value }) { order ->
                        // Pedido que fica pronto ou é entregue entra e sai animado: o garçom vê o que mudou.
                        ReadyOrderCard(
                            order = order,
                            onDeliver = { onDeliver(order.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        FeedbackBar(notice = notice, undo = state.undo, onUndo = onUndo)
    }
}
