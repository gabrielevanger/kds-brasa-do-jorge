package io.github.gabrielevanger.kds.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.component.OriginKind
import io.github.gabrielevanger.kds.core.designsystem.component.StageTone

/* Textos e ícones compartilhados pelas telas da cozinha, todos a partir de strings.xml. */

@Composable
fun OriginLabel.text(tableNumber: Int?): String = when (this) {
    OriginLabel.TABLE ->
        tableNumber
            ?.let { stringResource(R.string.kitchen_origin_table, it) }
            ?: stringResource(R.string.kitchen_origin_counter)

    OriginLabel.COUNTER -> stringResource(R.string.kitchen_origin_counter)

    OriginLabel.WHATSAPP -> stringResource(R.string.kitchen_origin_whatsapp)

    OriginLabel.IFOOD -> stringResource(R.string.kitchen_origin_ifood)

    OriginLabel.PIGZ -> stringResource(R.string.kitchen_origin_pigz)

    OriginLabel.WEB_MENU -> stringResource(R.string.kitchen_origin_web_menu)

    OriginLabel.LOYALTY -> stringResource(R.string.kitchen_origin_loyalty)

    OriginLabel.OTHER -> stringResource(R.string.kitchen_origin_other)
}

fun OriginLabel.iconKind(): OriginKind = when (this) {
    OriginLabel.TABLE -> OriginKind.TABLE
    OriginLabel.COUNTER -> OriginKind.COUNTER
    OriginLabel.WHATSAPP -> OriginKind.WHATSAPP
    OriginLabel.IFOOD, OriginLabel.PIGZ, OriginLabel.WEB_MENU -> OriginKind.DELIVERY
    OriginLabel.LOYALTY -> OriginKind.LOYALTY
    OriginLabel.OTHER -> OriginKind.OTHER
}

@Composable
fun StageTone.title(): String = when (this) {
    StageTone.QUEUED -> stringResource(R.string.kitchen_stage_queued)
    StageTone.PREPARING -> stringResource(R.string.kitchen_stage_preparing)
    StageTone.READY -> stringResource(R.string.kitchen_stage_ready)
}
