package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val formatted = formatElapsed(elapsedSeconds)
    when (level) {
        WaitLevel.NORMAL -> Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wait_normal),
                contentDescription = null,
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(KdsTheme.sizes.iconM),
            )
            Text(text = formatted, style = typography.timer, color = colors.onSurface)
        }

        WaitLevel.ATTENTION -> StatusPill(
            icon = R.drawable.ic_wait_attention,
            text = stringResource(R.string.wait_attention, formatted),
            containerColor = colors.attention,
            contentColor = colors.onAttention,
            textStyle = typography.timer,
            modifier = modifier,
        )

        WaitLevel.LATE -> StatusPill(
            icon = R.drawable.ic_wait_late,
            text = stringResource(R.string.wait_late, formatted),
            containerColor = colors.late,
            contentColor = colors.onLate,
            textStyle = typography.timer,
            modifier = modifier,
        )
    }
}

/** Minutos e segundos; acima de uma hora os minutos seguem contando (ex.: 75:03). */
internal fun formatElapsed(elapsedSeconds: Long): String {
    val safe = elapsedSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", safe / SECONDS_PER_MINUTE, safe % SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60
