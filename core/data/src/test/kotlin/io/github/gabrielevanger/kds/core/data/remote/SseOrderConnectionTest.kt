package io.github.gabrielevanger.kds.core.data.remote

import app.cash.turbine.test
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import java.io.IOException
import java.io.InterruptedIOException
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.sse.EventSources
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Conexão SSE contra um servidor HTTP real em memória. */
class SseOrderConnectionTest {

    private val server = MockWebServer()
    private val warnings = mutableListOf<String>()

    @BeforeEach
    fun setUp() = server.start()

    @AfterEach
    fun tearDown() = server.close()

    private fun connection(readTimeoutMillis: Long = 5_000): SseOrderConnection {
        val client = OkHttpClient.Builder().readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS).build()
        val parser = StreamEventParser(NetworkJson, OrderMapper(ZoneOffset.UTC)) { message, _ -> warnings += message }
        return SseOrderConnection(EventSources.createFactory(client), server.url("/events"), parser)
    }

    private fun sse(body: String) = MockResponse.Builder()
        .setHeader("Content-Type", "text/event-stream")
        .body(body)

    private fun orderJson(id: Long, stage: String = "PENDING", version: Long = 1) =
        """{"id":$id,"reference":"#000$id","origin":"POS","stage":"$stage","status":"PAID",""" +
            """"created":"2026-09-21T20:14:03","updated":"2026-09-21T20:14:03","version":$version,"orderItems":[]}"""

    @Test
    fun `emite conectado, snapshot e eventos, e termina quando o servidor encerra`() = runTest {
        server.enqueue(
            sse(
                "retry: 3000\n\n" +
                    "event: snapshot\ndata: [${orderJson(1)},${orderJson(2, "PREPARING")}]\n\n" +
                    "event: order.created\ndata: ${orderJson(3)}\n\n" +
                    ": ping\n\n" +
                    "event: order.updated\ndata: ${orderJson(3, "CANCELED", version = 2)}\n\n",
            ).build(),
        )

        connection().open().test {
            assertEquals(StreamEvent.Connection(ConnectionState.Connected), awaitItem())
            assertEquals(listOf(OrderId(1), OrderId(2)), (awaitItem() as StreamEvent.Snapshot).orders.map { it.id })
            assertEquals(OrderId(3), (awaitItem() as StreamEvent.OrderChanged).order.id)
            assertEquals(Stage.CANCELED, (awaitItem() as StreamEvent.OrderChanged).order.stage)
            awaitComplete()
        }
    }

    @Test
    fun `evento malformado ou de tipo desconhecido e ignorado sem derrubar o stream`() = runTest {
        server.enqueue(
            sse(
                "event: order.created\ndata: {quebrado\n\n" +
                    "event: order.refunded\ndata: {}\n\n" +
                    "event: order.created\ndata: ${orderJson(1, stage = "IN_TRANSIT")}\n\n" +
                    "event: order.created\ndata: ${orderJson(2)}\n\n",
            ).build(),
        )

        connection().open().test {
            assertEquals(StreamEvent.Connection(ConnectionState.Connected), awaitItem())
            assertEquals(OrderId(2), (awaitItem() as StreamEvent.OrderChanged).order.id)
            awaitComplete()
        }
        assertEquals(2, warnings.size)
    }

    @Test
    fun `resposta de erro do servidor encerra o fluxo com falha`() = runTest {
        server.enqueue(MockResponse.Builder().code(503).build())

        connection().open().test {
            val error = awaitError()
            assertInstanceOf(IOException::class.java, error)
            assertTrue(error.message.orEmpty().contains("503"))
        }
    }

    @Test
    fun `conexao muda alem do readTimeout encerra o fluxo com falha`() = runTest {
        server.enqueue(sse("event: snapshot\ndata: []\n\n").bodyDelay(3, TimeUnit.SECONDS).build())

        connection(readTimeoutMillis = 300).open().test {
            assertEquals(StreamEvent.Connection(ConnectionState.Connected), awaitItem())
            assertInstanceOf(InterruptedIOException::class.java, awaitError())
        }
    }

    @Test
    fun `heartbeat dentro do readTimeout mantem a conexao viva`() = runTest {
        val pings = ": ping\n\n".repeat(6)
        server.enqueue(
            sse(pings + "event: order.created\ndata: ${orderJson(1)}\n\n")
                .throttleBody(8, 150, TimeUnit.MILLISECONDS)
                .build(),
        )

        connection(readTimeoutMillis = 400).open().test(timeout = kotlin.time.Duration.parse("10s")) {
            assertEquals(StreamEvent.Connection(ConnectionState.Connected), awaitItem())
            assertEquals(OrderId(1), (awaitItem() as StreamEvent.OrderChanged).order.id)
            awaitComplete()
        }
    }
}
