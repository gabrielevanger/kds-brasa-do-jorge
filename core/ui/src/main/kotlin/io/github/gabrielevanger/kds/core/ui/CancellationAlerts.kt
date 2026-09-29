package io.github.gabrielevanger.kds.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.R as DesignR
import io.github.gabrielevanger.kds.core.designsystem.component.KitchenActionButton
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import kotlinx.collections.immutable.ImmutableList

object CancellationAlertTags {
    const val ALERT = "cancellation_alert"
}

/**
 * Cancelamentos de pedidos que a cozinha já tinha começado. Ficam no topo da tela, em vermelho,
 * e só saem com o toque em CIENTE: um pedido cancelado em preparo não pode passar despercebido.
 */
@Composable
fun CancellationAlerts(
    alerts: ImmutableList<CancellationAlertUi>,
    onDismiss: (OrderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs)) {
        alerts.forEach { alert ->
            CancellationAlert(alert = alert, onDismiss = { onDismiss(alert.id) })
        }
    }
}

@Composable
private fun CancellationAlert(alert: CancellationAlertUi, onDismiss: () -> Unit) {
    val colors = KdsTheme.colors
    val spacing = KdsTheme.spacing
    val typography = KdsTheme.typography
    MessageWithAction(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canceled, KdsTheme.shapes.card)
            .padding(horizontal = spacing.m, vertical = spacing.s)
            .testTag(CancellationAlertTags.ALERT),
        message = {
            AlertMessage(alert)
        },
        action = {
            KitchenActionButton(
                icon = DesignR.drawable.ic_action_ready,
                text = stringResource(R.string.kitchen_alert_dismiss),
                containerColor = colors.onCanceled,
                contentColor = colors.canceled,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun RowScope.AlertMessage(alert: CancellationAlertUi) {
    val colors = KdsTheme.colors
    val spacing = KdsTheme.spacing
    val typography = KdsTheme.typography
    Icon(
        painter = painterResource(DesignR.drawable.ic_canceled),
        contentDescription = null,
        tint = colors.onCanceled,
        modifier = Modifier.size(KdsTheme.sizes.iconL),
    )
    Column(modifier = Modifier.weight(EQUAL_SHARE), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
        Text(
            // Pronto já foi feito: a instrução passa a ser não entregar.
            text = if (alert.previousTone == StageTone.READY) {
                stringResource(R.string.kitchen_alert_title_ready, alert.reference)
            } else {
                stringResource(R.string.kitchen_alert_title, alert.reference)
            },
            style = typography.columnTitle,
            color = colors.onCanceled,
        )
        Text(
            text = stringResource(
                R.string.kitchen_alert_context,
                alert.previousTone.title(),
                alert.origin.text(alert.tableNumber),
            ),
            style = typography.label,
            color = colors.onCanceled,
        )
        Text(text = itemsSummary(alert.items), style = typography.body, color = colors.onCanceled)
    }
}

@Composable
private fun itemsSummary(items: ImmutableList<OrderItemUi>): String {
    val separator = stringResource(R.string.kitchen_items_separator)
    return items
        .map { item -> stringResource(R.string.kitchen_item_summary, item.quantity, item.name) }
        .joinToString(separator)
}
