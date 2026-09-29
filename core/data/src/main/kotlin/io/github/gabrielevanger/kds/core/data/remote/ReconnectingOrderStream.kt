package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.DiagnosticLog
import io.github.gabrielevanger.kds.core.data.network.NetworkMonitor
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.OrderStream
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mantém o stream de pedidos vivo: conecta, e a cada queda ou encerramento espera o backoff
 * e tenta de novo, até a coleta ser cancelada. A contagem de tentativas volta a zero a cada
 * conexão bem-sucedida. Se a rede do aparelho voltar durante a espera, reconecta na hora.
 */
class ReconnectingOrderStream(
    private val connect: () -> Flow<StreamEvent>,
    private val backoff: BackoffPolicy,
    private val networkMonitor: NetworkMonitor,
    private val log: DiagnosticLog,
) : OrderStream {

    override fun events(): Flow<StreamEvent> = flow {
        var attempt = 0
        emit(StreamEvent.Connection(ConnectionState.Connecting))
        while (true) {
            // catch captura só falhas da conexão; erros de quem coleta continuam propagando.
            emitAll(
                connect()
                    .onEach { event -> if (event == CONNECTED) attempt = 0 }
                    .catch { cause -> log.warn("Conexão com o stream de pedidos perdida", cause) },
            )
            attempt++
            emit(StreamEvent.Connection(ConnectionState.Reconnecting(attempt)))
            waitBeforeRetry(attempt)
        }
    }

    /** Espera o backoff, mas encerra antes se a rede cair e voltar nesse intervalo. */
    private suspend fun waitBeforeRetry(attempt: Int) {
        withTimeoutOrNull(backoff.delayFor(attempt)) {
            networkMonitor.isOnline
                .dropWhile { online -> online }
                .first { online -> online }
        }
    }

    private companion object {
        val CONNECTED = StreamEvent.Connection(ConnectionState.Connected)
    }
}
