package io.github.gabrielevanger.kds.core.domain.kitchen

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

enum class WaitBand { NORMAL, ATTENTION, LATE }

/**
 * Faixas de atraso do pedido, contadas desde a criação: é o tempo que o cliente está esperando,
 * não o tempo que a cozinha levou. Limites confirmados com a Brasa do Jorge: 8 e 15 minutos.
 */
class WaitPolicy(
    private val attentionAfter: Duration = DEFAULT_ATTENTION_AFTER,
    private val lateAfter: Duration = DEFAULT_LATE_AFTER,
) {
    init {
        require(attentionAfter < lateAfter) {
            "attentionAfter ($attentionAfter) deve ser menor que lateAfter ($lateAfter)"
        }
    }

    /** Relógio do aparelho atrás do servidor não gera espera negativa: conta como zero. */
    fun elapsed(createdAt: Instant, now: Instant): Duration =
        java.time.Duration.between(createdAt, now).toKotlinDuration().coerceAtLeast(Duration.ZERO)

    fun bandFor(createdAt: Instant, now: Instant): WaitBand {
        val waited = elapsed(createdAt, now)
        return when {
            waited >= lateAfter -> WaitBand.LATE
            waited >= attentionAfter -> WaitBand.ATTENTION
            else -> WaitBand.NORMAL
        }
    }

    companion object {
        val DEFAULT_ATTENTION_AFTER = 8.minutes
        val DEFAULT_LATE_AFTER = 15.minutes
    }
}
