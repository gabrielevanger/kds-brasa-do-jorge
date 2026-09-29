package io.github.gabrielevanger.kds.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.gabrielevanger.kds.core.designsystem.R as DesignR
import io.github.gabrielevanger.kds.core.designsystem.component.KitchenActionButton
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

object FeedbackBarTags {
    const val UNDO = "feedback_undo"
    const val NOTICE = "feedback_notice"
}

/**
 * Barra inferior de retorno do toque. Um aviso de falha tem prioridade sobre o Desfazer:
 * quem tocou precisa saber primeiro que o pedido voltou de etapa.
 */
@Composable
fun FeedbackBar(notice: KitchenNotice?, undo: UndoUi?, onUndo: (UndoUi) -> Unit, modifier: Modifier = Modifier) {
    when {
        notice != null -> NoticeBar(notice, modifier)
        undo != null -> UndoBar(undo, onUndo = { onUndo(undo) }, modifier = modifier)
    }
}

@Composable
private fun UndoBar(undo: UndoUi, onUndo: () -> Unit, modifier: Modifier) {
    val colors = KdsTheme.colors
    val spacing = KdsTheme.spacing
    MessageWithAction(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised, KdsTheme.shapes.card)
            .padding(horizontal = spacing.m, vertical = spacing.xs)
            .testTag(FeedbackBarTags.UNDO),
        message = {
            Text(
                text = undo.targetTone
                    ?.let { stringResource(R.string.kitchen_undo_message, undo.reference, it.title()) }
                    ?: stringResource(R.string.kitchen_undo_delivered, undo.reference),
                style = KdsTheme.typography.itemName,
                color = colors.onSurface,
                modifier = Modifier.weight(EQUAL_SHARE),
            )
        },
        action = {
            KitchenActionButton(
                icon = DesignR.drawable.ic_undo,
                text = stringResource(R.string.kitchen_undo_action),
                containerColor = colors.onSurface,
                contentColor = colors.background,
                onClick = onUndo,
            )
        },
    )
}

@Composable
private fun NoticeBar(notice: KitchenNotice, modifier: Modifier) {
    val colors = KdsTheme.colors
    val spacing = KdsTheme.spacing
    val message = when (notice.kind) {
        KitchenNotice.Kind.NOT_SENT -> R.string.kitchen_notice_not_sent
        KitchenNotice.Kind.REJECTED -> R.string.kitchen_notice_rejected
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KdsTheme.sizes.actionButtonHeight)
            .background(colors.attention, KdsTheme.shapes.card)
            .padding(horizontal = spacing.m, vertical = spacing.xs)
            .testTag(FeedbackBarTags.NOTICE),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
    ) {
        Icon(
            painter = painterResource(DesignR.drawable.ic_wait_attention),
            contentDescription = null,
            tint = colors.onAttention,
            modifier = Modifier.size(KdsTheme.sizes.iconL),
        )
        Text(
            text = stringResource(message, notice.reference, notice.currentTone.title()),
            style = KdsTheme.typography.itemName,
            color = colors.onAttention,
        )
    }
}
