package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Cores do KDS, em duas paletas com o mesmo significado. Cada par cor/texto tem contraste mínimo
 * WCAG AA nas duas (verificado em teste). Cor nunca é o único sinal de estado: ícone e texto
 * sempre acompanham.
 */
@Immutable
data class KdsColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    /** Contorno do card: no tema claro, card branco sobre fundo cinza-claro precisa dele para se separar. */
    val cardOutline: Color,
    val queued: Color,
    val onQueued: Color,
    val preparing: Color,
    val onPreparing: Color,
    val ready: Color,
    val onReady: Color,
    /** Fundos suaves das etapas, para cabeçalhos: a cor forte fica reservada ao que pede ação. */
    val queuedContainer: Color,
    val onQueuedContainer: Color,
    val preparingContainer: Color,
    val onPreparingContainer: Color,
    val readyContainer: Color,
    val onReadyContainer: Color,
    val attention: Color,
    val onAttention: Color,
    val late: Color,
    val onLate: Color,
    val canceled: Color,
    val onCanceled: Color,
    /** Destaque dos modificadores ("sem cebola", "mal passado") sobre o fundo do card. */
    val modifierHighlight: Color,
    /** Chama da marca ao lado do nome da casa. Elemento gráfico: contraste mínimo de 3:1 com o fundo. */
    val brand: Color,
)

/**
 * Escuro, em grafite quente: as cores de estado se destacam mais sobre fundo escuro, e a tela
 * brilha menos num turno longo perto da chapa.
 */
val KitchenDarkColors = KdsColors(
    background = Color(0xFF121110),
    surface = Color(0xFF1D1C1A),
    surfaceRaised = Color(0xFF2C2A27),
    onSurface = Color(0xFFF2EDE8),
    onSurfaceMuted = Color(0xFFB9B1A8),
    outline = Color(0xFF3A3632),
    cardOutline = Color(0xFF2C2A27),
    queued = Color(0xFF5B8DEF),
    onQueued = Color(0xFF0E0F11),
    preparing = Color(0xFFFFA726),
    onPreparing = Color(0xFF1A1200),
    ready = Color(0xFF43C463),
    onReady = Color(0xFF04210D),
    queuedContainer = Color(0xFF1B2536),
    onQueuedContainer = Color(0xFFA9C4FA),
    preparingContainer = Color(0xFF33260F),
    onPreparingContainer = Color(0xFFFFC870),
    readyContainer = Color(0xFF16301E),
    onReadyContainer = Color(0xFF8EE0A6),
    attention = Color(0xFFFFD54F),
    onAttention = Color(0xFF1A1400),
    late = Color(0xFFD32F2F),
    onLate = Color(0xFFFFFFFF),
    canceled = Color(0xFFB71C1C),
    onCanceled = Color(0xFFFFFFFF),
    modifierHighlight = Color(0xFFFFD54F),
    brand = Color(0xFFFF7A1A),
)

/**
 * Claro, em branco quente, o padrão do Android e o mais agradável à primeira vista. Os tons de etapa
 * ficam mais escuros para o texto branco manter o contraste, e o modificador e a chama da marca saem
 * dos tons claros, que somem no branco.
 */
val KitchenLightColors = KdsColors(
    background = Color(0xFFF7F3EE),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFEFE9E2),
    onSurface = Color(0xFF1C1917),
    onSurfaceMuted = Color(0xFF57504A),
    outline = Color(0xFFD9D0C5),
    cardOutline = Color(0xFFE2D9CE),
    queued = Color(0xFF1D5FD1),
    onQueued = Color(0xFFFFFFFF),
    preparing = Color(0xFFF59E0B),
    onPreparing = Color(0xFF1A1200),
    ready = Color(0xFF15803D),
    onReady = Color(0xFFFFFFFF),
    queuedContainer = Color(0xFFE3ECFD),
    onQueuedContainer = Color(0xFF1747A6),
    preparingContainer = Color(0xFFFDF0D5),
    onPreparingContainer = Color(0xFF7A4A00),
    readyContainer = Color(0xFFDCF3E3),
    onReadyContainer = Color(0xFF0F5E2C),
    attention = Color(0xFFFACC15),
    onAttention = Color(0xFF1A1400),
    late = Color(0xFFC62828),
    onLate = Color(0xFFFFFFFF),
    canceled = Color(0xFFB71C1C),
    onCanceled = Color(0xFFFFFFFF),
    modifierHighlight = Color(0xFF92400E),
    brand = Color(0xFFC2410C),
)
