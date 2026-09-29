package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.R
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import java.util.Locale

/**
 * Tempo de espera do pedido. Em atenção e atrasado, o nível aparece também como ícone e texto,
 * não só como cor. Deve ser o único composable do card a ler o relógio, para que o card
 * inteiro não recomponha a cada segundo.
 */
@Composable
fun WaitTimer(elapsedSeconds: Long, level: WaitLevel, modifier: Modifier = Modifier) {
    val colors = KdsTheme.colors
    val typography = KdsTheme.typography
    val spacing = KdsTheme.spacing
    val style = when (level) {
        WaitLevel.NORMAL -> WaitStyle(R.drawable.ic_wait_normal, null, Color.Transparent, colors.onSurface)

        WaitLevel.ATTENTION -> WaitStyle(
            R.drawable.ic_wait_attention,
            R.string.wait_attention,
            colors.attention,
            colors.onAttention,
        )

        WaitLevel.LATE -> WaitStyle(R.drawable.ic_wait_late, R.string.wait_late, colors.late, colors.onLate)
    }
    // Rótulo menor e dígitos grandes na mesma linha: o card não cresce quando o pedido atrasa.
    Row(
        modifier = modifier
            .background(style.container, KdsTheme.shapes.pill)
            // Sem fundo, o timer alinha com o número do pedido; com fundo, o texto não encosta na borda.
            .padding(horizontal = if (style.label == null) spacing.none else spacing.s, vertical = spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            painter = painterResource(style.icon),
            contentDescription = null,
            tint = style.content,
            modifier = Modifier.size(KdsTheme.sizes.iconM),
        )
        style.label?.let { label ->
            Text(text = stringResource(label), style = typography.label, color = style.content, maxLines = 1)
        }
        Text(text = formatElapsed(elapsedSeconds), style = typography.timer, color = style.content, maxLines = 1)
    }
}

private data class WaitStyle(
    @DrawableRes val icon: Int,
    @StringRes val label: Int?,
    val container: Color,
    val content: Color,
)

/** Minutos e segundos; acima de uma hora os minutos seguem contando (ex.: 75:03). */
internal fun formatElapsed(elapsedSeconds: Long): String {
    val safe = elapsedSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", safe / SECONDS_PER_MINUTE, safe % SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60
