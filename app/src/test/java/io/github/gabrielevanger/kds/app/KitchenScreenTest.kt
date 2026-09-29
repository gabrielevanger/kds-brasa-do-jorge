package io.github.gabrielevanger.kds.app

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class KitchenScreenTest {

    @ParameterizedTest(name = "menor largura {0} dp -> {1}")
    @CsvSource(
        // Celulares comuns, em pé ou deitados: a menor largura não muda com a rotação.
        "360, EXPEDITION",
        "411, EXPEDITION",
        "599, EXPEDITION",
        // Tablets, inclusive o de 1920x1200 a 320 dpi usado na validação (600 dp).
        "600, BOARD",
        "800, BOARD",
    )
    fun `aparelho define a tela pela menor largura`(smallestWidthDp: Int, expected: KitchenScreen) {
        assertEquals(expected, KitchenScreen.forDevice(smallestWidthDp))
    }
}
