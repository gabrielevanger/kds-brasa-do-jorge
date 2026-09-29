package io.github.gabrielevanger.kds.feature.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.component.visual
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import kotlinx.collections.immutable.ImmutableList

@Composable
fun BoardRoute(viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    KitchenClockProvider {
        BoardScreen(
            state = state,
            onAdvance = viewModel::onAdvance,
            onStationFilterSelected = viewModel::onStationFilterSelected,
        )
    }
}

/** Board do tablet na horizontal: uma coluna por etapa, na ordem da linha de produção. */
@Composable
fun BoardScreen(
    state: BoardUiState,
    onAdvance: (OrderId) -> Unit,
    onStationFilterSelected: (StationFilter) -> Unit,
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
        StationFilterRow(selected = state.stationFilter, onSelected = onStationFilterSelected)
        Row(modifier = Modifier.weight(EQUAL_SHARE), horizontalArrangement = Arrangement.spacedBy(spacing.s)) {
            state.columns.forEach { column ->
                BoardColumn(
                    tone = column.tone,
                    orders = column.orders,
                    onAdvance = onAdvance,
                    modifier = Modifier.weight(EQUAL_SHARE),
                )
            }
        }
    }
}

@Composable
private fun StationFilterRow(selected: StationFilter, onSelected: (StationFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs)) {
        StationFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(text = filter.label(), style = KdsTheme.typography.label) },
                modifier = Modifier.heightIn(min = KdsTheme.sizes.minTouchTarget),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KdsTheme.colors.onSurface,
                    selectedLabelColor = KdsTheme.colors.background,
                    labelColor = KdsTheme.colors.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun BoardColumn(
    tone: StageTone,
    orders: ImmutableList<OrderCardUi>,
    onAdvance: (OrderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KdsTheme.spacing
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        ColumnHeader(tone = tone, count = orders.size)
        if (orders.isEmpty()) {
            Text(
                text = stringResource(R.string.board_empty_column),
                style = KdsTheme.typography.body,
                color = KdsTheme.colors.onSurfaceMuted,
                modifier = Modifier.padding(spacing.m),
            )
        } else {
            // key estável por pedido: um pedido novo não recompõe nem reposiciona os outros cards.
            LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                items(items = orders, key = { it.id.value }, contentType = { ORDER_CARD_CONTENT_TYPE }) { card ->
                    OrderCard(card = card, onAdvance = { onAdvance(card.id) })
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(tone: StageTone, count: Int) {
    val visual = tone.visual()
    val spacing = KdsTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(visual.color, KdsTheme.shapes.pill)
            .padding(horizontal = spacing.m, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            painter = painterResource(visual.icon),
            contentDescription = null,
            tint = visual.onColor,
            modifier = Modifier.size(KdsTheme.sizes.iconM),
        )
        Text(text = tone.columnTitle(), style = KdsTheme.typography.columnTitle, color = visual.onColor)
        Box(modifier = Modifier.weight(EQUAL_SHARE))
        Text(text = count.toString(), style = KdsTheme.typography.columnTitle, color = visual.onColor)
    }
}

private const val ORDER_CARD_CONTENT_TYPE = "order_card"

/** Peso de layout: colunas e espaçadores dividem o espaço em partes iguais. */
private const val EQUAL_SHARE = 1f
