package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/**
 * Ação principal do card, na largura toda e com 72 dp de altura: acerta-se sem mirar.
 * É um botão, e não o card inteiro, para que um esbarrão no card não avance o pedido.
 */
@Composable
fun KitchenActionButton(
    @DrawableRes icon: Int,
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(KdsTheme.sizes.actionButtonHeight),
        shape = KdsTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(KdsTheme.sizes.iconL))
        Spacer(Modifier.width(KdsTheme.spacing.xs))
        Text(text = text, style = KdsTheme.typography.action)
    }
}
