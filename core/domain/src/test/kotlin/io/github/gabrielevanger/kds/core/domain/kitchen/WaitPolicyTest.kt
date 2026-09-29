package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.testing.BASE_TIME
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WaitPolicyTest {

    private val policy = WaitPolicy()

    private fun after(waited: Duration) = BASE_TIME.plus(waited.toJavaDuration())

    /** Fronteiras exatas: é onde um ">" no lugar de ">=" passaria despercebido. */
    @ParameterizedTest(name = "{0} s de espera -> {1}")
    @CsvSource(
        "0, NORMAL",
        "479, NORMAL",
        "480, ATTENTION",
        "899, ATTENTION",
        "900, LATE",
        "5400, LATE",
    )
    fun `faixa de atraso segue os limites de 8 e 15 minutos`(seconds: Long, expected: WaitBand) {
        assertEquals(expected, policy.bandFor(createdAt = BASE_TIME, now = after(seconds.seconds)))
    }

    @Test
    fun `relogio do aparelho atras do servidor conta como espera zero`() {
        val createdInTheFuture = after(2.minutes)

        assertEquals(Duration.ZERO, policy.elapsed(createdAt = createdInTheFuture, now = BASE_TIME))
        assertEquals(WaitBand.NORMAL, policy.bandFor(createdAt = createdInTheFuture, now = BASE_TIME))
    }

    @Test
    fun `limites sao configuraveis`() {
        val strict = WaitPolicy(attentionAfter = 5.minutes, lateAfter = 10.minutes)

        assertEquals(WaitBand.ATTENTION, strict.bandFor(BASE_TIME, after(5.minutes)))
        assertEquals(WaitBand.LATE, strict.bandFor(BASE_TIME, after(10.minutes)))
    }

    @Test
    fun `atencao precisa vir antes do atraso`() {
        assertThrows(IllegalArgumentException::class.java) {
            WaitPolicy(attentionAfter = 15.minutes, lateAfter = 8.minutes)
        }
    }
}
