package io.github.gabrielevanger.kds.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.R as DesignR
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState

object ConnectionBannerTags {
    const val BANNER = "connection_banner"
}

/**
 * Estado da conexão com o servidor. Conectado não mostra nada; sem conexão, a tela continua
 * visível com o último estado conhecido, e a faixa avisa que ele pode estar desatualizado.
 */
@Composable
fun ConnectionBanner(connection: ConnectionState, modifier: Modifier = Modifier) {
    val colors = KdsTheme.colors
    when (connection) {
        ConnectionState.Connected -> Unit

        ConnectionState.Connecting -> Banner(
            title = stringResource(R.string.kitchen_connection_connecting),
            detail = null,
            containerColor = colors.surfaceRaised,
            contentColor = colors.onSurface,
            modifier = modifier,
        )

        is ConnectionState.Reconnecting -> Banner(
            title = stringResource(R.string.kitchen_connection_reconnecting, connection.attempt),
            detail = stringResource(R.string.kitchen_connection_stale),
            containerColor = colors.attention,
            contentColor = colors.onAttention,
            modifier = modifier,
        )
    }
}

@Composable
private fun Banner(title: String, detail: String?, containerColor: Color, contentColor: Color, modifier: Modifier) {
    val spacing = KdsTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, KdsTheme.shapes.card)
            .padding(horizontal = spacing.m, vertical = spacing.s)
            .testTag(ConnectionBannerTags.BANNER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_offline),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(KdsTheme.sizes.iconL),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Text(text = title, style = KdsTheme.typography.columnTitle, color = contentColor)
            detail?.let { Text(text = it, style = KdsTheme.typography.body, color = contentColor) }
        }
    }
}
