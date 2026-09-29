package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * "Legível a distância" como regra verificada: todo par texto/fundo do tema precisa de
 * contraste WCAG 2.1 nível AA (4,5:1 para texto comum).
 */
class KdsColorsContrastTest {

    private val colors = KitchenColors

    private val pairs = mapOf(
        "texto no fundo" to (colors.onSurface to colors.background),
        "texto no card" to (colors.onSurface to colors.surface),
        "texto no card elevado" to (colors.onSurface to colors.surfaceRaised),
        "texto secundário no card" to (colors.onSurfaceMuted to colors.surface),
        "fila" to (colors.onQueued to colors.queued),
        "preparando" to (colors.onPreparing to colors.preparing),
        "pronto" to (colors.onReady to colors.ready),
        "atenção" to (colors.onAttention to colors.attention),
        "atrasado" to (colors.onLate to colors.late),
        "cancelado" to (colors.onCanceled to colors.canceled),
        "modificador no card" to (colors.modifierHighlight to colors.surface),
    )

    @Test
    fun `todo par de texto e fundo atinge contraste WCAG AA`() {
        val failures = pairs.mapNotNull { (name, pair) ->
            val ratio = contrastRatio(pair.first, pair.second)
            if (ratio < AA_NORMAL_TEXT) "$name: %.2f:1".format(ratio) else null
        }

        assertTrue(failures.isEmpty(), "Abaixo de 4,5:1 -> $failures")
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val lighter = max(relativeLuminance(foreground), relativeLuminance(background))
        val darker = min(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Fórmula de luminância relativa da WCAG 2.1, sobre os canais sRGB. */
    private fun relativeLuminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    private companion object {
        const val AA_NORMAL_TEXT = 4.5
    }
}
