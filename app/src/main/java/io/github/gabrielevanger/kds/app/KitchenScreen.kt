package io.github.gabrielevanger.kds.app

import androidx.window.core.layout.WindowSizeClass

/** Tela que o aparelho mostra: cada aparelho tem um papel fixo na cozinha. */
enum class KitchenScreen {
    /** Tablet na bancada da montagem. */
    BOARD,

    /** Celular do garçom. */
    EXPEDITION,
    ;

    companion object {
        /**
         * Decide pela menor largura do aparelho, e não pela largura da janela: girar o celular não
         * pode trocar a tela do garçom pelo board. O limite é o da faixa compacta do Material.
         */
        fun forDevice(smallestWidthDp: Int): KitchenScreen =
            if (smallestWidthDp < WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) EXPEDITION else BOARD
    }
}
