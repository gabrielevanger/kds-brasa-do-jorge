package io.github.gabrielevanger.kds.feature.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.component.OriginKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone

/* Textos e ícones de cada valor da tela, todos a partir de strings.xml. */

@Composable
internal fun OriginLabel.text(tableNumber: Int?): String = when (this) {
    OriginLabel.TABLE ->
        tableNumber
            ?.let { stringResource(R.string.board_origin_table, it) }
            ?: stringResource(R.string.board_origin_counter)

    OriginLabel.COUNTER -> stringResource(R.string.board_origin_counter)

    OriginLabel.WHATSAPP -> stringResource(R.string.board_origin_whatsapp)

    OriginLabel.IFOOD -> stringResource(R.string.board_origin_ifood)

    OriginLabel.PIGZ -> stringResource(R.string.board_origin_pigz)

    OriginLabel.WEB_MENU -> stringResource(R.string.board_origin_web_menu)

    OriginLabel.LOYALTY -> stringResource(R.string.board_origin_loyalty)

    OriginLabel.OTHER -> stringResource(R.string.board_origin_other)
}

internal fun OriginLabel.iconKind(): OriginKind = when (this) {
    OriginLabel.TABLE -> OriginKind.TABLE
    OriginLabel.COUNTER -> OriginKind.COUNTER
    OriginLabel.WHATSAPP -> OriginKind.WHATSAPP
    OriginLabel.IFOOD, OriginLabel.PIGZ, OriginLabel.WEB_MENU -> OriginKind.DELIVERY
    OriginLabel.LOYALTY -> OriginKind.LOYALTY
    OriginLabel.OTHER -> OriginKind.OTHER
}

@Composable
internal fun StageTone.columnTitle(): String = when (this) {
    StageTone.QUEUED -> stringResource(R.string.board_column_queued)
    StageTone.PREPARING -> stringResource(R.string.board_column_preparing)
    StageTone.READY -> stringResource(R.string.board_column_ready)
}

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
