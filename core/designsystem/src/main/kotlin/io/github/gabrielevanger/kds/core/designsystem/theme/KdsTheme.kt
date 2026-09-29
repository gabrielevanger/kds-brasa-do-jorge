package io.github.gabrielevanger.kds.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalKdsColors = staticCompositionLocalOf { KitchenDarkColors }
private val LocalKdsTypography = staticCompositionLocalOf { KitchenTypography }
private val LocalKdsSpacing = staticCompositionLocalOf { KdsSpacing() }
private val LocalKdsSizes = staticCompositionLocalOf { KdsSizes() }
private val LocalKdsShapes = staticCompositionLocalOf { KdsShapes() }

/** Ponto único de acesso aos tokens: componentes não declaram cores, medidas ou formas soltas. */
object KdsTheme {
    val colors: KdsColors
        @Composable get() = LocalKdsColors.current

    val typography: KdsTypography
        @Composable get() = LocalKdsTypography.current

    val spacing: KdsSpacing
        @Composable get() = LocalKdsSpacing.current

    val sizes: KdsSizes
        @Composable get() = LocalKdsSizes.current

    val shapes: KdsShapes
        @Composable get() = LocalKdsShapes.current
}

/** Segue o modo claro ou escuro do aparelho, que o dono escolhe nas configurações do Android. */
@Composable
fun KdsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) KitchenDarkColors else KitchenLightColors
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val materialColors = base.copy(
        primary = colors.queued,
        onPrimary = colors.onQueued,
        background = colors.background,
        onBackground = colors.onSurface,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceRaised,
        onSurfaceVariant = colors.onSurfaceMuted,
        outline = colors.outline,
        error = colors.late,
        onError = colors.onLate,
    )
    CompositionLocalProvider(
        LocalKdsColors provides colors,
        LocalKdsTypography provides KitchenTypography,
        LocalKdsSpacing provides KdsSpacing(),
        LocalKdsSizes provides KdsSizes(),
        LocalKdsShapes provides KdsShapes(),
    ) {
        MaterialTheme(colorScheme = materialColors, content = content)
    }
}
