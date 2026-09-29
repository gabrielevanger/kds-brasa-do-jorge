package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.gabrielevanger.kds.core.designsystem.R
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/** Para onde o pedido vai ("MESA 4", "iFOOD"): responde de imediato se é salão ou entrega. */
@Composable
fun OriginTag(kind: OriginKind, label: String, modifier: Modifier = Modifier) {
    val colors = KdsTheme.colors
    Row(
        modifier = modifier
            .border(width = KdsTheme.sizes.borderWidth, color = colors.onSurfaceMuted, shape = KdsTheme.shapes.pill)
            .padding(horizontal = KdsTheme.spacing.s, vertical = KdsTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(kind.icon),
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(KdsTheme.sizes.iconM),
        )
        Text(text = label, style = KdsTheme.typography.origin, color = colors.onSurface)
    }
}

private val OriginKind.icon: Int
    get() = when (this) {
        OriginKind.TABLE -> R.drawable.ic_origin_table
        OriginKind.COUNTER -> R.drawable.ic_origin_counter
        OriginKind.WHATSAPP -> R.drawable.ic_origin_whatsapp
        OriginKind.DELIVERY -> R.drawable.ic_origin_delivery
        OriginKind.LOYALTY -> R.drawable.ic_origin_loyalty
        OriginKind.OTHER -> R.drawable.ic_origin_counter
    }
