package io.github.gabrielevanger.kds.feature.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.R as DesignR
import io.github.gabrielevanger.kds.core.designsystem.component.KitchenActionButton
import io.github.gabrielevanger.kds.core.designsystem.component.OriginTag
import io.github.gabrielevanger.kds.core.designsystem.component.WaitLevel
import io.github.gabrielevanger.kds.core.designsystem.component.WaitTimer
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.ui.LocalNow
import io.github.gabrielevanger.kds.core.ui.R as UiR
import io.github.gabrielevanger.kds.core.ui.iconKind
import io.github.gabrielevanger.kds.core.ui.text
import java.time.Duration
import java.time.Instant

/** Tags usadas pelos testes de interface para localizar partes do card. */
object ReadyOrderCardTags {
    const val CARD = "ready_order_card"
    const val CHARGE = "ready_order_charge"
}

/**
 * Card do pedido no balcão, na ordem em que o garçom decide: qual pedido e para onde vai, se precisa
 * cobrar, há quanto tempo espera, o que conferir na sacola e, por último, a entrega.
 */
@Composable
fun ReadyOrderCard(order: ReadyOrderUi, onDeliver: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = KdsTheme.spacing
    val colors = KdsTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ReadyOrderCardTags.CARD),
        shape = KdsTheme.shapes.card,
        color = colors.surface,
    ) {
        Column(modifier = Modifier.padding(spacing.m), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            // No celular, uma origem longa ("CARDÁPIO WEB") desce para a linha de baixo em vez de espremer o número.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = order.reference, style = KdsTheme.typography.orderNumber, color = colors.onSurface)
                OriginTag(kind = order.origin.iconKind(), label = order.origin.text(order.tableNumber))
            }
            if (order.mustCharge) ChargeBadge()
            CounterTimer(readyAt = order.readyAt)
            HorizontalDivider(color = colors.outline)
            Text(
                text = pluralStringResource(R.plurals.expedition_item_count, order.itemCount, order.itemCount),
                style = KdsTheme.typography.label,
                color = colors.onSurfaceMuted,
            )
            order.items.forEach { item ->
                Text(
                    text = stringResource(UiR.string.kitchen_item_summary, item.quantity, item.name),
                    style = KdsTheme.typography.itemName,
                    color = colors.onSurface,
                )
            }
            KitchenActionButton(
                icon = DesignR.drawable.ic_action_deliver,
                text = stringResource(R.string.expedition_deliver),
                containerColor = colors.ready,
                contentColor = colors.onReady,
                onClick = onDeliver,
            )
        }
    }
}

/** Faixa larga, com ícone e texto: entregar sem cobrar é prejuízo direto para a casa. */
@Composable
private fun ChargeBadge() {
    val colors = KdsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.attention, KdsTheme.shapes.pill)
            .padding(horizontal = KdsTheme.spacing.s, vertical = KdsTheme.spacing.xs)
            .testTag(ReadyOrderCardTags.CHARGE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_payment),
            contentDescription = null,
            tint = colors.onAttention,
            modifier = Modifier.size(KdsTheme.sizes.iconL),
        )
        Text(
            text = stringResource(R.string.expedition_charge),
            style = KdsTheme.typography.columnTitle,
            color = colors.onAttention,
        )
    }
}

/** Único ponto do card que lê o relógio: só este trecho recompõe a cada segundo. */
@Composable
private fun CounterTimer(readyAt: Instant) {
    val now = LocalNow.current.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.expedition_on_counter),
            style = KdsTheme.typography.label,
            color = KdsTheme.colors.onSurfaceMuted,
        )
        WaitTimer(elapsedSeconds = Duration.between(readyAt, now).seconds, level = WaitLevel.NORMAL)
    }
}
