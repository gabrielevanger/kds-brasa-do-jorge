package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import java.io.IOException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener

/**
 * Uma conexão SSE com o servidor de pedidos. O fluxo emite Connected ao abrir, depois os eventos,
 * e termina quando a conexão acaba: normalmente se o servidor encerrar, com erro se ela falhar.
 * Reconectar é responsabilidade de quem coleta.
 *
 * O readTimeout do cliente deve ser maior que o intervalo do heartbeat do servidor: sem nenhum
 * byte nesse período, a conexão está muda e a leitura falha, encerrando o fluxo com erro.
 */
class SseOrderConnection(
    private val eventSourceFactory: EventSource.Factory,
    private val eventsUrl: HttpUrl,
    private val parser: StreamEventParser,
) {

    fun open(): Flow<StreamEvent> = callbackFlow {
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySendBlocking(StreamEvent.Connection(ConnectionState.Connected))
            }

            // Bloquear a thread de leitura quando o consumidor está lento é preferível a descartar um pedido.
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                parser.parse(type, data)?.let { trySendBlocking(it) }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("Stream de pedidos respondeu HTTP ${response?.code}"))
            }
        }
        val eventSource = eventSourceFactory.newEventSource(Request.Builder().url(eventsUrl).build(), listener)
        awaitClose { eventSource.cancel() }
    }
}
