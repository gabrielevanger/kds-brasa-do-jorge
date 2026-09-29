package io.github.gabrielevanger.kds.core.data.remote

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Espera entre tentativas de reconexão: dobra a cada tentativa até [maxDelay].
 * O jitter sorteia entre 50% e 100% do valor, para que tablet, celulares e TV não
 * reconectem todos no mesmo instante quando o Wi-Fi da cozinha volta.
 */
class BackoffPolicy(
    private val initialDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 30.seconds,
    private val jitterFactor: () -> Double = { 0.5 + Random.nextDouble() * 0.5 },
) {

    /** [attempt] começa em 1. */
    fun delayFor(attempt: Int): Duration {
        require(attempt >= 1) { "attempt começa em 1, recebido $attempt" }
        // Limita o expoente para 2^n não estourar em quedas muito longas.
        val exponential = initialDelay * (1L shl (attempt - 1).coerceAtMost(MAX_EXPONENT)).toDouble()
        return minOf(exponential, maxDelay) * jitterFactor()
    }

    private companion object {
        const val MAX_EXPONENT = 20
    }
}
