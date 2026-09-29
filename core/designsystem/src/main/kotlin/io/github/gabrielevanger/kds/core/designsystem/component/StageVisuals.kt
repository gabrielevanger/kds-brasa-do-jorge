package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.gabrielevanger.kds.core.designsystem.R
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/** Cor, ícone e ação de cada etapa num lugar só, para colunas, badges e botões concordarem. */
@Immutable
data class StageVisual(
    val color: Color,
    val onColor: Color,
    @DrawableRes val icon: Int,
    @DrawableRes val actionIcon: Int,
)

@Composable
fun StageTone.visual(): StageVisual {
    val colors = KdsTheme.colors
    return when (this) {
        StageTone.QUEUED -> StageVisual(
            colors.queued,
            colors.onQueued,
            R.drawable.ic_stage_queued,
            R.drawable.ic_action_start,
        )

        StageTone.PREPARING -> StageVisual(
            colors.preparing,
            colors.onPreparing,
            R.drawable.ic_stage_preparing,
            R.drawable.ic_action_ready,
        )

        StageTone.READY -> StageVisual(
            colors.ready,
            colors.onReady,
            R.drawable.ic_stage_ready,
            R.drawable.ic_action_deliver,
        )
    }
}
