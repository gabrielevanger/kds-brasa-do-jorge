package io.github.gabrielevanger.kds.app

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.kds.core.designsystem.theme.KdsTheme
import io.github.gabrielevanger.kds.feature.board.BoardRoute
import io.github.gabrielevanger.kds.feature.expedition.ExpeditionRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KdsTheme {
                when (KitchenScreen.forDevice(LocalConfiguration.current.smallestScreenWidthDp)) {
                    KitchenScreen.BOARD -> {
                        KitchenDisplayMode(window)
                        BoardRoute()
                    }

                    KitchenScreen.EXPEDITION -> ExpeditionRoute()
                }
            }
        }
    }
}

/**
 * No pico ninguém toca no tablet por minutos: a tela não pode apagar. As barras do sistema
 * somem para ganhar espaço e evitar um "voltar" acidental; um deslize na borda as mostra.
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
