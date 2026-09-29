package io.github.gabrielevanger.kds.core.data.remote

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackoffPolicyTest {

    @Test
    fun `espera dobra a cada tentativa ate o teto de 30 segundos`() {
        val policy = BackoffPolicy(jitterFactor = { 1.0 })

        val delays = (1..7).map { policy.delayFor(it) }

        assertEquals(listOf(1, 2, 4, 8, 16, 30, 30).map { it.seconds }, delays)
    }

    @Test
    fun `jitter reduz a espera proporcionalmente`() {
        val policy = BackoffPolicy(jitterFactor = { 0.5 })

        assertEquals(500.milliseconds, policy.delayFor(1))
        assertEquals(15.seconds, policy.delayFor(10))
    }

    @Test
    fun `jitter padrao fica entre metade e o valor cheio`() {
        val policy = BackoffPolicy()

        repeat(1_000) {
            val delay = policy.delayFor(3)
            assertTrue(delay >= 2.seconds && delay <= 4.seconds, "fora do intervalo: $delay")
        }
    }

    /**
     * Na JVM, `1L shl n` usa só os 6 bits menores de n: sem limitar o expoente, a tentativa 64
     * daria espera negativa e a 65 voltaria a 1 segundo.
     */
    @Test
    fun `queda muito longa mantem a espera no teto`() {
        val policy = BackoffPolicy(jitterFactor = { 1.0 })

        (6..200).forEach { attempt ->
            assertEquals(30.seconds, policy.delayFor(attempt), "tentativa $attempt")
        }
    }

    @Test
    fun `tentativa zero e rejeitada`() {
        assertThrows(IllegalArgumentException::class.java) { BackoffPolicy().delayFor(0) }
    }
}
