package io.github.gabrielevanger.kds.core.data.remote

import app.cash.turbine.test
import io.github.gabrielevanger.kds.core.data.network.NetworkMonitor
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Reconexão com tempo virtual: nenhum teste espera de verdade pelos segundos de backoff. */
class ReconnectingOrderStreamTest {

    private val connecting = StreamEvent.Connection(ConnectionState.Connecting)
    private val connected = StreamEvent.Connection(ConnectionState.Connected)
    private val snapshot = StreamEvent.Snapshot(emptyList())
    private fun reconnecting(attempt: Int) = StreamEvent.Connection(ConnectionState.Reconnecting(attempt))

    private val online = MutableStateFlow(true)
    private val networkMonitor = object : NetworkMonitor {
        override val isOnline: Flow<Boolean> = online
    }

    private val failingConnection: Flow<StreamEvent> = flow { throw IOException("sem rota") }
    private val droppingConnection: Flow<StreamEvent> = flow {
        emit(connected)
        emit(snapshot)
        throw IOException("conexão caiu")
    }
    private val stableConnection: Flow<StreamEvent> = flow {
        emit(connected)
        awaitCancellation()
    }

    /** Cria o stream com conexões pré-definidas, registrando o instante virtual de cada tentativa. */
    private fun TestScope.streamOf(vararg connections: Flow<StreamEvent>): Pair<ReconnectingOrderStream, List<Long>> {
        val attemptTimes = mutableListOf<Long>()
        val queue = ArrayDeque(connections.toList())
        val stream = ReconnectingOrderStream(
            connect = {
                attemptTimes += testScheduler.currentTime
                queue.removeFirstOrNull() ?: stableConnection
            },
            backoff = BackoffPolicy(jitterFactor = { 1.0 }),
            networkMonitor = networkMonitor,
            log = { _, _ -> },
        )
        return stream to attemptTimes
    }

    @Test
    fun `sinaliza conectando e repassa os eventos da conexao`() = runTest {
        val (stream, _) = streamOf(droppingConnection)

        stream.events().test {
            assertEquals(connecting, awaitItem())
            assertEquals(connected, awaitItem())
            assertEquals(snapshot, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `falhas seguidas aumentam a espera entre tentativas`() = runTest {
        val (stream, attemptTimes) = streamOf(failingConnection, failingConnection, failingConnection)

        stream.events().test {
            assertEquals(connecting, awaitItem())
            assertEquals(reconnecting(1), awaitItem())
            assertEquals(reconnecting(2), awaitItem())
            assertEquals(reconnecting(3), awaitItem())
            assertEquals(connected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(0L, 1_000L, 3_000L, 7_000L), attemptTimes)
    }

    @Test
    fun `conexao bem sucedida zera a contagem de tentativas`() = runTest {
        val (stream, _) = streamOf(failingConnection, failingConnection, droppingConnection)

        stream.events().test {
            assertEquals(connecting, awaitItem())
            assertEquals(reconnecting(1), awaitItem())
            assertEquals(reconnecting(2), awaitItem())
            assertEquals(connected, awaitItem())
            assertEquals(snapshot, awaitItem())
            assertEquals(reconnecting(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `servidor que encerra o stream normalmente tambem provoca reconexao`() = runTest {
        val closedByServer: Flow<StreamEvent> = flow { emit(connected) }
        val (stream, attemptTimes) = streamOf(closedByServer)

        stream.events().test {
            assertEquals(connecting, awaitItem())
            assertEquals(connected, awaitItem())
            assertEquals(reconnecting(1), awaitItem())
            assertEquals(connected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(0L, 1_000L), attemptTimes)
    }

    @Test
    fun `rede que volta durante a espera antecipa a reconexao`() = runTest {
        val (stream, attemptTimes) = streamOf(*Array(6) { failingConnection })

        stream.events().test {
            repeat(7) { awaitItem() } // conectando e as tentativas 1 a 6; a sexta espera 30 s
            online.value = false
            advanceTimeBy(2_000)
            online.value = true
            runCurrent()
            assertEquals(connected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val lastWait = attemptTimes.last() - attemptTimes[attemptTimes.size - 2]
        assertEquals(2_000L, lastWait)
    }

    @Test
    fun `cancelar a coleta encerra a conexao aberta`() = runTest {
        var connectionClosed = false
        val tracked = flow {
            emit(connected)
            awaitCancellation()
        }.onCompletion { connectionClosed = true }
        val (stream, _) = streamOf(tracked)

        stream.events().test {
            assertEquals(connecting, awaitItem())
            assertEquals(connected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(connectionClosed)
    }
}
