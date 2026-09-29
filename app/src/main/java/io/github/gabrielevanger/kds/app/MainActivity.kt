package io.github.gabrielevanger.kds.app

import android.content.res.Configuration
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.feature.board.BoardRoute
import io.github.gabrielevanger.kds.feature.board.TvPanelRoute
import io.github.gabrielevanger.kds.feature.expedition.ExpeditionRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val configuration = LocalConfiguration.current
            val isTelevision =
                configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
            // Todos os aparelhos seguem o modo do Android, que por padrão é claro.
            val darkTheme = isSystemInDarkTheme()
            SystemBarsAppearance(window, darkTheme)
            KdsTheme(darkTheme = darkTheme) {
                when (KitchenScreen.forDevice(configuration.smallestScreenWidthDp, isTelevision)) {
                    KitchenScreen.BOARD -> {
                        KitchenDisplayMode(window)
                        BoardRoute()
                    }

                    KitchenScreen.EXPEDITION -> ExpeditionRoute()

                    KitchenScreen.TV_PANEL -> {
                        KitchenDisplayMode(window)
                        TvPanelRoute()
                    }
                }
            }
        }
    }
}

/** Ícones da barra de status e de navegação escuros no tema claro, e claros no escuro, para não sumirem. */
@Composable
private fun SystemBarsAppearance(window: Window, darkTheme: Boolean) {
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

/**
 * No pico ninguém toca no tablet por minutos, e na TV ninguém toca nunca: a tela não pode apagar
 * nem entrar no descanso de tela. As barras do sistema somem para ganhar espaço e evitar um
 * "voltar" acidental; um deslize na borda as mostra.
 * O celular do garçom fica de fora: ele vive no bolso e usa o aparelho para outras coisas.
 */
@Composable
private fun KitchenDisplayMode(window: Window) {
    DisposableEffect(window) {
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
