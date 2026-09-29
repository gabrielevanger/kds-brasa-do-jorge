package io.github.gabrielevanger.kds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme

@Preview(widthDp = PREVIEW_WIDTH_DP)
@Composable
private fun ComponentsPreview() {
    KdsTheme {
        Column(
            modifier = Modifier
                .background(KdsTheme.colors.surface)
                .padding(KdsTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(KdsTheme.spacing.s),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(KdsTheme.spacing.xs)) {
                OriginTag(OriginKind.TABLE, SAMPLE_TABLE_LABEL)
                OriginTag(OriginKind.DELIVERY, SAMPLE_DELIVERY_LABEL)
            }
            WaitTimer(elapsedSeconds = SAMPLE_NORMAL_WAIT_SECONDS, level = WaitLevel.NORMAL)
            WaitTimer(elapsedSeconds = SAMPLE_ATTENTION_WAIT_SECONDS, level = WaitLevel.ATTENTION)
            WaitTimer(elapsedSeconds = SAMPLE_LATE_WAIT_SECONDS, level = WaitLevel.LATE)
            ModifierLine(ModifierKind.REMOVE, SAMPLE_REMOVE_MODIFIER)
            ModifierLine(ModifierKind.ADD, SAMPLE_ADD_MODIFIER)
            ModifierLine(ModifierKind.OTHER, SAMPLE_NOTE)
            StageTone.entries.forEach { tone ->
                val visual = tone.visual()
                KitchenActionButton(
                    icon = visual.actionIcon,
                    text = SAMPLE_ACTION_LABEL,
                    containerColor = visual.color,
                    contentColor = visual.onColor,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private const val PREVIEW_WIDTH_DP = 420
private const val SAMPLE_NORMAL_WAIT_SECONDS = 4 * 60L + 5
private const val SAMPLE_ATTENTION_WAIT_SECONDS = 9 * 60L + 12
private const val SAMPLE_LATE_WAIT_SECONDS = 16 * 60L + 40

// Dados de exemplo, como chegariam do servidor; os rótulos reais das ações ficam no board.
private const val SAMPLE_TABLE_LABEL = "MESA 4"
private const val SAMPLE_DELIVERY_LABEL = "PIGZ"
private const val SAMPLE_REMOVE_MODIFIER = "Sem cebola"
private const val SAMPLE_ADD_MODIFIER = "Cheddar extra"
private const val SAMPLE_NOTE = "Caprichar no ponto"
private const val SAMPLE_ACTION_LABEL = "AÇÃO"
