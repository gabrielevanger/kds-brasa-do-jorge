package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/**
 * Cabeçalho de uma etapa com a contagem de pedidos. Fundo suave e uma linha na cor forte da etapa:
 * a cor continua legível de longe, mas deixa o destaque para os botões e alertas.
 */
@Composable
fun StageHeader(tone: StageTone, title: String, count: Int, modifier: Modifier = Modifier) {
    val visual = tone.visual()
    val spacing = KdsTheme.spacing
    val accentHeight = KdsTheme.sizes.stageAccentHeight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KdsTheme.shapes.pill)
            .background(visual.container)
            .drawBehind { drawRect(visual.color, size = Size(size.width, accentHeight.toPx())) }
            .padding(horizontal = spacing.m, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            painter = painterResource(visual.icon),
            contentDescription = null,
            tint = visual.onContainer,
            modifier = Modifier.size(KdsTheme.sizes.iconM),
        )
        Text(
            text = title,
            style = KdsTheme.typography.columnTitle,
            color = visual.onContainer,
            modifier = Modifier.weight(FILL_REMAINING),
        )
        Text(text = count.toString(), style = KdsTheme.typography.columnTitle, color = visual.onContainer)
    }
}

/** Peso de layout: o título ocupa o espaço que sobra e empurra a contagem para a direita. */
private const val FILL_REMAINING = 1f
