package io.github.gabrielevanger.kds.core.ui

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * Sinais entre estados consecutivos, escolhidos por [select]. A comparação parte do estado já
 * exibido quando a coleta começa: voltar ao app ou girar a tela não repete avisos antigos.
 */
fun StateFlow<KitchenState>.signals(
    select: (previous: KitchenState, next: KitchenState) -> Set<KitchenSignal>,
): Flow<Set<KitchenSignal>> = flow {
    var previous = value
    collect { next ->
        val signals = select(previous, next)
        if (signals.isNotEmpty()) emit(signals)
        previous = next
    }
}
