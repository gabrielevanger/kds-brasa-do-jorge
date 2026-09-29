package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.OrderCommands
import io.github.gabrielevanger.kds.core.domain.sync.OrderStream
import io.github.gabrielevanger.kds.core.domain.sync.StageChangeResult
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Guarda o estado da cozinha e liga o reducer ao mundo: eventos do stream entram como eventos
 * do reducer, e cada toque é enviado ao servidor depois da janela de desfazer.
 *
 * O estado muda só por [MutableStateFlow.update], atômico: um evento do stream e um toque
 * simultâneos não se sobrescrevem.
 */
class OrderStore(
    private val stream: OrderStream,
    private val commands: OrderCommands,
    private val scope: CoroutineScope,
    private val undoWindow: Duration = 5.seconds,
) {
    private val mutableState = MutableStateFlow(KitchenState())
    val state: StateFlow<KitchenState> = mutableState.asStateFlow()

    private val mutableConnection = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connection: StateFlow<ConnectionState> = mutableConnection.asStateFlow()

    private val noticeChannel = Channel<StoreNotice>(Channel.BUFFERED)

    /** Avisos pontuais para a tela, consumidos uma única vez. */
    val notices: Flow<StoreNotice> = noticeChannel.receiveAsFlow()

    private val scheduledSends = ConcurrentHashMap<OrderId, Job>()

    /** Começa a receber pedidos. A coleta dura enquanto o [scope] estiver ativo. */
    fun start(): Job = scope.launch {
        stream.events().collect { event ->
            when (event) {
                is StreamEvent.Snapshot -> dispatch(KitchenEvent.SnapshotReceived(event.orders))
                is StreamEvent.OrderChanged -> dispatch(KitchenEvent.OrderReceived(event.order))
                is StreamEvent.Connection -> mutableConnection.value = event.state
            }
        }
    }

    /** Toque principal. Toque duplo ou pedido sem próxima etapa não agendam envio. */
    fun advance(orderId: OrderId) {
        var created = false
        mutableState.update { current ->
            val next = OrderReducer.reduce(current, KitchenEvent.TransitionRequested(orderId))
            created = orderId !in current.pending && orderId in next.pending
            next
        }
        if (created) scheduledSends[orderId] = scope.launch { sendAfterUndoWindow(orderId) }
    }

    /** Desfaz dentro da janela. Depois do envio, o toque já está com o servidor e não é desfeito. */
    fun undo(orderId: OrderId) {
        dispatch(KitchenEvent.TransitionUndone(orderId))
        if (orderId !in mutableState.value.pending) scheduledSends.remove(orderId)?.cancel()
    }

    fun dismissAlert(orderId: OrderId) = dispatch(KitchenEvent.CancellationAlertDismissed(orderId))

    private suspend fun sendAfterUndoWindow(orderId: OrderId) {
        delay(undoWindow)
        scheduledSends.remove(orderId)

        dispatch(KitchenEvent.TransitionSent(orderId))
        // O servidor pode ter mudado o pedido durante a janela; nesse caso a pendência já não existe.
        val pending = mutableState.value.pending[orderId] ?: return

        when (val result = commands.changeStage(orderId, pending.to)) {
            is StageChangeResult.Confirmed -> dispatch(KitchenEvent.TransitionConfirmed(result.order))

            is StageChangeResult.Rejected -> {
                dispatch(KitchenEvent.TransitionRejected(result.currentOrder))
                noticeChannel.trySend(StoreNotice.TransitionRejected(orderId))
            }

            is StageChangeResult.Failed -> {
                dispatch(KitchenEvent.TransitionFailed(orderId))
                noticeChannel.trySend(StoreNotice.TransitionNotSent(orderId))
            }
        }
    }

    private fun dispatch(event: KitchenEvent) = mutableState.update { OrderReducer.reduce(it, event) }
}

sealed interface StoreNotice {

    /** Outro aparelho mudou o pedido antes; a tela já mostra a etapa real. */
    data class TransitionRejected(val orderId: OrderId) : StoreNotice

    /** A mudança não chegou ao servidor; o card voltou à etapa anterior. */
    data class TransitionNotSent(val orderId: OrderId) : StoreNotice
}
