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
import io.github.gabrielevanger.kds.core.designsystem.R
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

/**
 * Modificador ou observação logo abaixo do item, em destaque: é a informação que hoje se perde
 * na comanda de papel ("sem cebola", "mal passado") e gera retrabalho.
 */
@Composable
fun ModifierLine(kind: ModifierKind, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs),
    ) {
        Icon(
            painter = painterResource(kind.icon),
            contentDescription = null,
            tint = KdsTheme.colors.modifierHighlight,
            modifier = Modifier.size(KdsTheme.sizes.iconS),
        )
        Text(text = text, style = KdsTheme.typography.modifier, color = KdsTheme.colors.modifierHighlight)
    }
}

private val ModifierKind.icon: Int
    get() = when (this) {
        ModifierKind.REMOVE -> R.drawable.ic_modifier_remove
        ModifierKind.ADD -> R.drawable.ic_modifier_add
        ModifierKind.OTHER -> R.drawable.ic_note
    }
