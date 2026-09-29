package io.github.gabrielevanger.kds.core.designsystem.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WaitTimerFormatTest {

    @ParameterizedTest(name = "{0} s -> {1}")
    @CsvSource(
        "0, 00:00",
        "59, 00:59",
        "60, 01:00",
        "552, 09:12",
        "3600, 60:00",
        "4503, 75:03",
        "-5, 00:00",
    )
    fun `formata minutos e segundos sem virar horas`(seconds: Long, expected: String) {
        assertEquals(expected, formatElapsed(seconds))
    }
}
