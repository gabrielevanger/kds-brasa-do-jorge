package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cores do KDS. Tema escuro de alto contraste: a cozinha é iluminada, a tela é vista de longe
 * e o turno é longo. Cada par cor/texto tem contraste mínimo WCAG AA (verificado em teste).
 * Cor nunca é o único sinal de estado: ícone e texto sempre acompanham.
 */
@Immutable
data class KdsColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val queued: Color,
    val onQueued: Color,
    val preparing: Color,
    val onPreparing: Color,
    val ready: Color,
    val onReady: Color,
    val attention: Color,
    val onAttention: Color,
    val late: Color,
    val onLate: Color,
    val canceled: Color,
    val onCanceled: Color,
    /** Destaque dos modificadores ("sem cebola", "mal passado") sobre o fundo do card. */
    val modifierHighlight: Color,
)

val KitchenColors = KdsColors(
    background = Color(0xFF0E0F11),
    surface = Color(0xFF1B1D21),
    surfaceRaised = Color(0xFF26292E),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceMuted = Color(0xFFB8BCC4),
    outline = Color(0xFF3A3E45),
    queued = Color(0xFF5B8DEF),
    onQueued = Color(0xFF0E0F11),
    preparing = Color(0xFFFFA726),
    onPreparing = Color(0xFF1A1200),
    ready = Color(0xFF43C463),
    onReady = Color(0xFF04210D),
    attention = Color(0xFFFFD54F),
    onAttention = Color(0xFF1A1400),
    late = Color(0xFFD32F2F),
    onLate = Color(0xFFFFFFFF),
    canceled = Color(0xFFB71C1C),
    onCanceled = Color(0xFFFFFFFF),
    modifierHighlight = Color(0xFFFFD54F),
)
