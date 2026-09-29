package io.github.gabrielevanger.kds.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Instant
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1_000L

/**
 * Relógio único da tela. O [State] é entregue uma vez e nunca troca; só quem lê o valor
 * (o timer do card) é recomposto a cada segundo, e não o card, a lista ou a coluna.
 */
val LocalNow = staticCompositionLocalOf<State<Instant>> { mutableStateOf(Instant.EPOCH) }

@Composable
fun KitchenClockProvider(content: @Composable () -> Unit) {
    val now = produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            // Alinha o próximo tique à virada do segundo, para todos os timers mudarem juntos.
            delay(MILLIS_PER_SECOND - value.toEpochMilli() % MILLIS_PER_SECOND)
        }
    }
    CompositionLocalProvider(LocalNow provides now, content = content)
}
