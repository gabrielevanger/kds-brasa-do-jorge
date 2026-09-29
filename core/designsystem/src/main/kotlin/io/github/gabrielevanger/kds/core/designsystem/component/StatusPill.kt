package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.TextStyle
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/** Ícone e texto sobre uma cor: o estado nunca depende só da cor para ser entendido. */
@Composable
fun StatusPill(
    @DrawableRes icon: Int,
    text: String,
    containerColor: Color,
    contentColor: Color,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(containerColor, KdsTheme.shapes.pill)
            .padding(horizontal = KdsTheme.spacing.s, vertical = KdsTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(KdsTheme.sizes.iconS),
        )
        Text(text = text, color = contentColor, style = textStyle)
    }
}
