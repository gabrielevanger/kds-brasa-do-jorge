package io.github.gabrielevanger.kds.core.domain.sync

import io.github.gabrielevanger.kds.core.domain.model.Order
import kotlinx.coroutines.flow.Flow

/**
 * Fonte de pedidos em tempo real. A implementação cuida da conexão e da reconexão;
 * quem coleta recebe o estado inicial, as mudanças e o estado da conexão num único fluxo.
 */
interface OrderStream {
    fun events(): Flow<StreamEvent>
}

sealed interface StreamEvent {

    /** Estado completo do servidor, enviado a cada conexão, inclusive após uma queda. */
    data class Snapshot(val orders: List<Order>) : StreamEvent

    data class OrderChanged(val order: Order) : StreamEvent

    data class Connection(val state: ConnectionState) : StreamEvent
}

sealed interface ConnectionState {

    data object Connecting : ConnectionState

    data object Connected : ConnectionState

    /** Conexão perdida; [attempt] conta as tentativas desde a última conexão bem-sucedida. */
    data class Reconnecting(val attempt: Int) : ConnectionState
}
