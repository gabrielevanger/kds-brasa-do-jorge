package io.github.gabrielevanger.kds.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/**
 * Mensagem com um botão de ação. No tablet o botão fica ao lado; no celular em pé ele não cabe ao
 * lado sem espremer o texto até sumir, então vai para baixo, na largura toda.
 */
@Composable
internal fun MessageWithAction(
    modifier: Modifier,
    message: @Composable RowScope.() -> Unit,
    action: @Composable () -> Unit,
) {
    val spacing = KdsTheme.spacing
    val sizes = KdsTheme.sizes
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < sizes.narrowLayoutMaxWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.s)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.m),
                    content = message,
                )
                action()
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                message()
                Box(modifier = Modifier.width(sizes.secondaryActionWidth)) { action() }
            }
        }
    }
}
