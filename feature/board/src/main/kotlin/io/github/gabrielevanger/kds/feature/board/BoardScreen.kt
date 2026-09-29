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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone
import io.github.gabrielevanger.kds.core.designsystem.component.visual
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Tags usadas pelos testes de interface para localizar as listas de cada coluna. */
object BoardTags {
    private const val COLUMN_PREFIX = "board_column_"

    fun column(tone: StageTone): String = COLUMN_PREFIX + tone.name
}

@Composable
fun BoardRoute(viewModel: BoardViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notice by rememberVisibleNotice(viewModel.notices)
    KitchenClockProvider {
        BoardScreen(
            state = state,
            notice = notice,
            onAdvance = viewModel::onAdvance,
            onUndo = { undo -> viewModel.onUndo(undo.orderId) },
            onDismissAlert = viewModel::onDismissAlert,
            onStationFilterSelected = viewModel::onStationFilterSelected,
        )
    }
}

/** Mantém cada aviso visível por alguns segundos; um aviso novo substitui o anterior. */
@Composable
private fun rememberVisibleNotice(notices: Flow<BoardNotice>): State<BoardNotice?> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val visible = remember { mutableStateOf<BoardNotice?>(null) }
    LaunchedEffect(notices, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            notices.collectLatest { notice ->
                visible.value = notice
                delay(NOTICE_DURATION)
                visible.value = null
            }
        }
    }
    return visible
}

/** Board do tablet na horizontal: uma coluna por etapa, na ordem da linha de produção. */
@Composable
fun BoardScreen(
    state: BoardUiState,
    notice: BoardNotice?,
    onAdvance: (OrderId) -> Unit,
    onUndo: (UndoUi) -> Unit,
    onDismissAlert: (OrderId) -> Unit,
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
        CancellationAlerts(alerts = state.cancellationAlerts, onDismiss = onDismissAlert)
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
        FeedbackBar(notice = notice, undo = state.undo, onUndo = onUndo)
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
    val listState = rememberLazyListState()
    val oldestOrderId = orders.firstOrNull()?.id
    // A LazyColumn mantém no lugar o card visível quando entra um pedido acima dele, escondendo o mais
    // antigo. Se a coluna estava no início, volta ao topo; se alguém rolou para baixo, a posição fica.
    LaunchedEffect(oldestOrderId) {
        if (listState.firstVisibleItemIndex <= CARD_PUSHED_BY_INSERT) listState.scrollToItem(FIRST_CARD)
    }
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
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(spacing.s),
                modifier = Modifier.testTag(BoardTags.column(tone)),
            ) {
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

private const val FIRST_CARD = 0

/** Posição em que o card do topo fica depois que um pedido mais antigo entra acima dele. */
private const val CARD_PUSHED_BY_INSERT = 1

/** Tempo de leitura do aviso de falha, igual à janela de desfazer para manter um ritmo único. */
private val NOTICE_DURATION = 5.seconds
