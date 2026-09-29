package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.gabrielevanger.kds.core.designsystem.R
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/**
 * Cor, ícone e ação de cada etapa num lugar só, para colunas, badges e botões concordarem.
 * [color] é a cor forte, dos botões; [container] é o fundo suave, dos cabeçalhos.
 */
@Immutable
data class StageVisual(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
    @DrawableRes val icon: Int,
    @DrawableRes val actionIcon: Int,
)

@Composable
fun StageTone.visual(): StageVisual {
    val colors = KdsTheme.colors
    return when (this) {
        StageTone.QUEUED -> StageVisual(
            color = colors.queued,
            onColor = colors.onQueued,
            container = colors.queuedContainer,
            onContainer = colors.onQueuedContainer,
            icon = R.drawable.ic_stage_queued,
            actionIcon = R.drawable.ic_action_start,
        )

        StageTone.PREPARING -> StageVisual(
            color = colors.preparing,
            onColor = colors.onPreparing,
            container = colors.preparingContainer,
            onContainer = colors.onPreparingContainer,
            icon = R.drawable.ic_stage_preparing,
            actionIcon = R.drawable.ic_action_ready,
        )

        StageTone.READY -> StageVisual(
            color = colors.ready,
            onColor = colors.onReady,
            container = colors.readyContainer,
            onContainer = colors.onReadyContainer,
            icon = R.drawable.ic_stage_ready,
            actionIcon = R.drawable.ic_action_deliver,
        )
    }
}
