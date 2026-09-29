package io.github.gabrielevanger.kds.feature.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone

/* Textos próprios do board, todos a partir de strings.xml; os compartilhados ficam em core/ui. */

@Composable
internal fun StageTone.actionText(): String = when (this) {
    StageTone.QUEUED -> stringResource(R.string.board_action_start)
    StageTone.PREPARING -> stringResource(R.string.board_action_ready)
    StageTone.READY -> stringResource(R.string.board_action_deliver)
}

@Composable
internal fun StationFilter.label(): String = when (this) {
    StationFilter.ALL -> stringResource(R.string.board_filter_all)
    StationFilter.CHAPA -> stringResource(R.string.board_filter_chapa)
    StationFilter.FRITADEIRA -> stringResource(R.string.board_filter_fritadeira)
    StationFilter.MONTAGEM -> stringResource(R.string.board_filter_montagem)
}
