package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.StageChangeResult
import java.io.IOException
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteOrderCommandsTest {

    private val server = MockWebServer()
    private lateinit var commands: RemoteOrderCommands

    @BeforeEach
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(NetworkJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OrdersApi::class.java)
        commands = RemoteOrderCommands(api, NetworkJson, OrderMapper(ZoneOffset.UTC))
    }

    @AfterEach
    fun tearDown() = server.close()

    private fun orderJson(stage: String, version: Long) =
        """{"id":7,"reference":"#0007","origin":"POS","stage":"$stage","status":"PAID",""" +
            """"created":"2026-09-21T20:14:03","updated":"2026-09-21T20:15:00","version":$version,"orderItems":[]}"""

    private fun json(code: Int, body: String) = MockResponse.Builder()
        .code(code)
        .setHeader("Content-Type", "application/json")
        .body(body)
        .build()

    @Test
    fun `envia PATCH com a etapa no corpo`() = runTest {
        server.enqueue(json(200, orderJson("PREPARING", version = 2)))

        commands.changeStage(OrderId(7), Stage.PREPARING)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/orders/7", request.target)
        assertEquals("""{"stage":"PREPARING"}""", request.body?.utf8())
    }

    @Test
    fun `sucesso devolve o pedido atualizado`() = runTest {
        server.enqueue(json(200, orderJson("PREPARING", version = 2)))

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        val order = (result as StageChangeResult.Confirmed).order
        assertEquals(Stage.PREPARING, order.stage)
        assertEquals(2, order.version)
    }

    @Test
    fun `conflito 409 devolve o pedido como esta no servidor`() = runTest {
        server.enqueue(
            json(
                409,
                """{"error":"Transição de READY para PREPARING não permitida","code":"ORDER-409-001",""" +
                    """"order":${orderJson("READY", version = 3)}}""",
            ),
        )

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        val current = (result as StageChangeResult.Rejected).currentOrder
        assertEquals(Stage.READY, current.stage)
        assertEquals(3, current.version)
    }

    @Test
    fun `conflito sem pedido no corpo vira falha`() = runTest {
        server.enqueue(json(409, """{"error":"conflito"}"""))

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        assertInstanceOf(HttpException::class.java, (result as StageChangeResult.Failed).cause)
    }

    @Test
    fun `erro do servidor vira falha`() = runTest {
        server.enqueue(json(500, """{"error":"falha interna"}"""))

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        val cause = (result as StageChangeResult.Failed).cause
        assertEquals(500, (cause as HttpException).code())
    }

    @Test
    fun `conexao que cai no meio da requisicao vira falha`() = runTest {
        server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        assertInstanceOf(IOException::class.java, (result as StageChangeResult.Failed).cause)
    }

    @Test
    fun `sucesso com corpo fora do contrato vira falha`() = runTest {
        server.enqueue(json(200, """{"id":"sete"}"""))

        val result = commands.changeStage(OrderId(7), Stage.PREPARING)

        assertInstanceOf(StageChangeResult.Failed::class.java, result)
    }
}
