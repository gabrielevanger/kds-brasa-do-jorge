package io.github.gabrielevanger.kds.app

import androidx.window.core.layout.WindowSizeClass

/** Tela que o aparelho mostra: cada aparelho tem um papel fixo na cozinha. */
enum class KitchenScreen {
    /** Tablet na bancada da montagem. */
    BOARD,

    /** Celular do garçom. */
    EXPEDITION,

    /** TV na parede da cozinha, somente leitura. */
    TV_PANEL,
    ;

    companion object {
        /**
         * A TV é reconhecida pelo modo de interface do sistema, e não pelo tamanho: um tablet grande
         * não vira painel. Nos demais, decide a menor largura do aparelho, e não a da janela: girar o
         * celular não pode trocar a tela do garçom pelo board. O limite é o da faixa compacta do Material.
         */
        fun forDevice(smallestWidthDp: Int, isTelevision: Boolean): KitchenScreen = when {
            isTelevision -> TV_PANEL
            smallestWidthDp < WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND -> EXPEDITION
            else -> BOARD
        }
    }
}
