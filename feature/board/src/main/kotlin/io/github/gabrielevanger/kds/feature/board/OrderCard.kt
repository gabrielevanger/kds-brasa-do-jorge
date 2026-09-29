package io.github.gabrielevanger.kds.feature.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.component.KitchenActionButton
import io.github.gabrielevanger.kds.core.designsystem.component.ModifierKind
import io.github.gabrielevanger.kds.core.designsystem.component.ModifierLine
import io.github.gabrielevanger.kds.core.designsystem.component.OriginTag
import io.github.gabrielevanger.kds.core.designsystem.component.WaitLevel
import io.github.gabrielevanger.kds.core.designsystem.component.WaitTimer
import io.github.gabrielevanger.kds.core.designsystem.component.visual
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.kitchen.WaitBand
import io.github.gabrielevanger.kds.core.domain.kitchen.WaitPolicy
import io.github.gabrielevanger.kds.core.ui.LocalNow
import io.github.gabrielevanger.kds.core.ui.OrderItemUi
import io.github.gabrielevanger.kds.core.ui.iconKind
import io.github.gabrielevanger.kds.core.ui.text
import java.time.Instant

/** Tags usadas pelos testes de interface para localizar partes do card. */
object OrderCardTags {
    const val CARD = "order_card"
    const val ACTION = "order_card_action"
    const val TIMER = "order_card_timer"
}

private val waitPolicy = WaitPolicy()

/**
 * Card do pedido, na ordem em que a cozinha lê de longe: quem é e para onde vai, há quanto tempo
 * espera, o tamanho, os itens com os modificadores em destaque e, por último, a ação.
 * Sem [onAdvance] (painel de TV, somente leitura), o card não tem botão.
 */
@Composable
fun OrderCard(card: OrderCardUi, onAdvance: (() -> Unit)?, modifier: Modifier = Modifier) {
    val spacing = KdsTheme.spacing
    val visual = card.tone.visual()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(OrderCardTags.CARD),
        shape = KdsTheme.shapes.card,
        color = KdsTheme.colors.surface,
    ) {
        Column(modifier = Modifier.padding(spacing.m), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = card.reference, style = KdsTheme.typography.orderNumber, color = KdsTheme.colors.onSurface)
                OriginTag(kind = card.origin.iconKind(), label = card.origin.text(card.tableNumber))
            }
            // Numa coluna estreita, o tamanho desce para a linha de baixo em vez de ser espremido pelo timer.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                OrderWaitTimer(createdAt = card.createdAt)
                OrderSize(itemCount = card.itemCount, isLarge = card.isLarge)
            }
            HorizontalDivider(color = KdsTheme.colors.outline)
            card.items.forEach { item -> OrderItemRow(item) }
            card.note?.let { note -> ModifierLine(kind = ModifierKind.OTHER, text = note) }
            if (onAdvance != null) {
                KitchenActionButton(
                    icon = visual.actionIcon,
                    text = card.tone.actionText(),
                    containerColor = visual.color,
                    contentColor = visual.onColor,
                    onClick = onAdvance,
                    enabled = !card.isAwaitingServer,
                    modifier = Modifier.testTag(OrderCardTags.ACTION),
                )
            }
        }
    }
}

/** Único ponto do card que lê o relógio: só este texto recompõe a cada segundo. */
@Composable
private fun OrderWaitTimer(createdAt: Instant) {
    val now = LocalNow.current.value
    WaitTimer(
        elapsedSeconds = waitPolicy.elapsed(createdAt, now).inWholeSeconds,
        level = waitPolicy.bandFor(createdAt, now).toLevel(),
        modifier = Modifier.testTag(OrderCardTags.TIMER),
    )
}

@Composable
private fun OrderSize(itemCount: Int, isLarge: Boolean) {
    val typography = KdsTheme.typography
    Row(
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.board_item_count, itemCount, itemCount),
            style = typography.label,
            color = KdsTheme.colors.onSurfaceMuted,
        )
        if (isLarge) {
            Text(
                text = stringResource(R.string.board_large_order),
                style = typography.label,
                color = KdsTheme.colors.onSurface,
                modifier = Modifier
                    .background(KdsTheme.colors.surfaceRaised, KdsTheme.shapes.pill)
                    .padding(horizontal = KdsTheme.spacing.xs, vertical = KdsTheme.spacing.xxs),
            )
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItemUi) {
    Column(verticalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xxs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.board_item_quantity, item.quantity),
                style = KdsTheme.typography.itemName,
                color = KdsTheme.colors.onSurface,
            )
            Text(text = item.name, style = KdsTheme.typography.itemName, color = KdsTheme.colors.onSurface)
        }
        item.modifiers.forEach { modifier ->
            ModifierLine(
                kind = modifier.kind,
                text = modifier.text,
                modifier = Modifier.padding(start = KdsTheme.spacing.l),
            )
        }
        item.note?.let { note ->
            ModifierLine(
                kind = ModifierKind.OTHER,
                text = note,
                modifier = Modifier.padding(start = KdsTheme.spacing.l),
            )
        }
    }
}

private fun WaitBand.toLevel(): WaitLevel = when (this) {
    WaitBand.NORMAL -> WaitLevel.NORMAL
    WaitBand.ATTENTION -> WaitLevel.ATTENTION
    WaitBand.LATE -> WaitLevel.LATE
}
