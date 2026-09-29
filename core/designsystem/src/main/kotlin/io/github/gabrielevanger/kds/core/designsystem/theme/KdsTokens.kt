package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Escala de espaçamento em passos de 4 dp. Componentes não usam valores fora dela. */
@Immutable
data class KdsSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val s: Dp = 12.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
)

/** Tamanhos fixos. Alvos de toque dimensionados para mão suja e pressa: nenhuma ação exige mira. */
@Immutable
data class KdsSizes(
    val iconS: Dp = 24.dp,
    val iconM: Dp = 28.dp,
    val iconL: Dp = 32.dp,
    val borderWidth: Dp = 2.dp,
    val cardBorderWidth: Dp = 1.dp,
    /** Faixa lateral do card que marca atenção ou atraso, visível de longe. */
    val urgencyStripeWidth: Dp = 8.dp,
    /** Linha na cor da etapa no topo do cabeçalho da coluna, que mantém a cor legível de longe. */
    val stageAccentHeight: Dp = 4.dp,
    /** Ponto do indicador de conexão, sempre acompanhado de texto. */
    val statusDot: Dp = 14.dp,
    val minTouchTarget: Dp = 64.dp,
    val actionButtonHeight: Dp = 72.dp,
    /** Largura de ações secundárias ao lado de um texto, como o "Desfazer" da barra inferior. */
    val secondaryActionWidth: Dp = 240.dp,
    /** Abaixo desta largura (celular em pé, faixa compacta do Material) a ação vai para baixo do texto. */
    val narrowLayoutMaxWidth: Dp = 600.dp,
)

@Immutable
data class KdsShapes(
    val pill: Shape = RoundedCornerShape(8.dp),
    val button: Shape = RoundedCornerShape(12.dp),
    val card: Shape = RoundedCornerShape(16.dp),
    val dot: Shape = CircleShape,
)
