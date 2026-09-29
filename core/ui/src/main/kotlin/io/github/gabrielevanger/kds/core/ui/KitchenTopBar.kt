package io.github.gabrielevanger.kds.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object KitchenTopBarTags {
    const val CLOCK = "top_bar_clock"
    const val STATUS = "top_bar_status"
}

/**
 * Barra do topo das telas da cozinha: o nome da casa, o conteúdo da tela (os filtros do board),
 * o estado da conexão sempre visível e a hora. Conectado também aparece: sem isso, ninguém sabe
 * se a tela parada está viva ou travada.
 */
@Composable
fun KitchenTopBar(
    connection: ConnectionState,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    content: @Composable RowScope.() -> Unit = {},
) {
    val spacing = KdsTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        Text(
            text = stringResource(R.string.kitchen_brand),
            style = KdsTheme.typography.label,
            color = KdsTheme.colors.onSurfaceMuted,
        )
        content()
        Box(modifier = Modifier.weight(EQUAL_SHARE))
        ConnectionStatus(connection)
        KitchenClockText(zone)
    }
}

@Composable
private fun ConnectionStatus(connection: ConnectionState) {
    val colors = KdsTheme.colors
    val (dotColor: Color, text: String) = when (connection) {
        ConnectionState.Connected -> colors.ready to stringResource(R.string.kitchen_status_live)
        ConnectionState.Connecting -> colors.onSurfaceMuted to stringResource(R.string.kitchen_status_connecting)
        is ConnectionState.Reconnecting -> colors.late to stringResource(R.string.kitchen_status_offline)
    }
    Row(
        modifier = Modifier.testTag(KitchenTopBarTags.STATUS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(KdsTheme.sizes.statusDot)
                .background(dotColor, KdsTheme.shapes.dot),
        )
        Text(text = text, style = KdsTheme.typography.label, color = colors.onSurface)
    }
}

/** Lê o relógio único, mas recompõe só na virada do minuto: o texto mostra horas e minutos. */
@Composable
private fun KitchenClockText(zone: ZoneId) {
    val now = LocalNow.current
    val formatter = remember(zone) { DateTimeFormatter.ofPattern(CLOCK_PATTERN).withZone(zone) }
    val time by remember(now, formatter) { derivedStateOf { formatter.format(now.value) } }
    Text(
        text = time,
        style = KdsTheme.typography.columnTitle,
        color = KdsTheme.colors.onSurface,
        modifier = Modifier.testTag(KitchenTopBarTags.CLOCK),
    )
}

private const val CLOCK_PATTERN = "HH:mm"
