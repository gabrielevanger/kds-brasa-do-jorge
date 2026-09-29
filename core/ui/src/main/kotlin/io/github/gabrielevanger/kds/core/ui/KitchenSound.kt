package io.github.gabrielevanger.kds.core.ui

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class KitchenSound { ALARM, BEEP }

/**
 * Escolhe o som de um conjunto de sinais. O alarme de cancelamento tem prioridade e nunca é
 * suprimido; o bip de pedido novo respeita um intervalo mínimo, para uma rajada de pedidos no
 * pico virar poucos bips e não uma sirene.
 */
class KitchenSoundPolicy(private val minBeepInterval: Duration = DEFAULT_MIN_BEEP_INTERVAL) {

    private var lastBeepAt: Duration? = null

    /** [now] é um instante monotônico (ex.: tempo desde o boot), imune a ajustes do relógio. */
    fun soundFor(signals: Set<KitchenSignal>, now: Duration): KitchenSound? = when {
        KitchenSignal.CANCELLATION in signals -> KitchenSound.ALARM

        KitchenSignal.NEW_ORDER in signals && canBeep(now) -> {
            lastBeepAt = now
            KitchenSound.BEEP
        }

        else -> null
    }

    private fun canBeep(now: Duration): Boolean = lastBeepAt?.let { now - it >= minBeepInterval } ?: true

    companion object {
        val DEFAULT_MIN_BEEP_INTERVAL = 3.seconds
    }
}
