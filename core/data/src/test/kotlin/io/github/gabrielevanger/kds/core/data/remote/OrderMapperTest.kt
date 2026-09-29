package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.remote.dto.OrderDto
import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.PaymentStatus
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.model.Stage
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrderMapperTest {

    private val boaVista = ZoneId.of("America/Boa_Vista")
    private val mapper = OrderMapper(sourceZone = ZoneOffset.UTC)

    /** Exemplo do mock/README.md, com o campo version adicionado ao mock. */
    private val readmeExample = """
        {
          "id": 7,
          "reference": "#0007",
          "origin": "IFOOD",
          "stage": "PREPARING",
          "status": "PAID",
          "table": null,
          "total": "54.00",
          "created": "2026-09-21T20:14:03",
          "updated": "2026-09-21T20:16:40",
          "version": 3,
          "note": null,
          "orderItems": [
            {
              "id": "7-0",
              "name": "Smash Duplo Cheddar",
              "productionArea": "CHAPA",
              "quantity": 1,
              "price": "34.00",
              "total": "34.00",
              "note": "Caprichar no ponto",
              "attributes": [
                { "name": "Ponto da carne", "items": [{ "name": "Mal passado" }] }
              ]
            }
          ]
        }
    """.trimIndent()

    private fun decode(json: String): OrderDto = NetworkJson.decodeFromString(json)

    private fun readmeWith(vararg replacements: Pair<String, String>): OrderDto =
        decode(replacements.fold(readmeExample) { json, (from, to) -> json.replace(from, to) })

    @Test
    fun `converte o exemplo do README do mock`() {
        val order = mapper.toDomainOrNull(decode(readmeExample))!!

        assertEquals(OrderId(7), order.id)
        assertEquals(Origin.IFOOD, order.origin)
        assertEquals(Stage.PREPARING, order.stage)
        assertEquals(PaymentStatus.PAID, order.paymentStatus)
        assertEquals(3, order.version)
        val item = order.items.single()
        assertEquals(ProductionArea.CHAPA, item.productionArea)
        assertEquals("Caprichar no ponto", item.note)
        assertEquals(listOf(Modifier(group = "Ponto da carne", option = "Mal passado")), item.modifiers)
    }

    @Test
    fun `data sem fuso e lida como UTC e nao como hora local`() {
        val order = mapper.toDomainOrNull(decode(readmeExample))!!

        assertEquals(Instant.parse("2026-09-21T20:14:03Z"), order.createdAt)
        assertEquals(16, order.createdAt.atZone(boaVista).hour)
    }

    @Test
    fun `fuso de origem configuravel para um back que envie hora local`() {
        val localTimeMapper = OrderMapper(sourceZone = boaVista)

        val order = localTimeMapper.toDomainOrNull(decode(readmeExample))!!

        assertEquals(Instant.parse("2026-09-22T00:14:03Z"), order.createdAt)
    }

    @Test
    fun `data que ja traz fuso e respeitada`() {
        val dto = readmeWith("\"2026-09-21T20:14:03\"" to "\"2026-09-21T16:14:03-04:00\"")

        val order = mapper.toDomainOrNull(dto)!!

        assertEquals(Instant.parse("2026-09-21T20:14:03Z"), order.createdAt)
    }

    @Test
    fun `canal e estacao desconhecidos viram OTHER em vez de descartar o pedido`() {
        val dto = readmeWith("\"IFOOD\"" to "\"RAPPI\"", "\"CHAPA\"" to "\"FORNO\"")

        val order = mapper.toDomainOrNull(dto)!!

        assertEquals(Origin.OTHER, order.origin)
        assertEquals(ProductionArea.OTHER, order.items.single().productionArea)
    }

    @Test
    fun `etapa desconhecida descarta o pedido`() {
        val dto = readmeWith("\"PREPARING\"" to "\"IN_TRANSIT\"")

        assertNull(mapper.toDomainOrNull(dto))
    }

    @Test
    fun `data invalida descarta o pedido`() {
        val dto = readmeWith("\"2026-09-21T20:14:03\"" to "\"ontem\"")

        assertNull(mapper.toDomainOrNull(dto))
    }

    @Test
    fun `pagamento pendente e status desconhecido pedem cobranca`() {
        val notPaid = mapper.toDomainOrNull(readmeWith("\"PAID\"" to "\"NO_PAID\""))!!
        val unknown = mapper.toDomainOrNull(readmeWith("\"PAID\"" to "\"PARTIAL\""))!!

        assertEquals(PaymentStatus.NOT_PAID, notPaid.paymentStatus)
        assertEquals(PaymentStatus.NOT_PAID, unknown.paymentStatus)
    }

    @Test
    fun `atributo com varias opcoes vira um modificador por opcao`() {
        val dto = readmeWith(
            "[{ \"name\": \"Mal passado\" }]" to "[{ \"name\": \"Sem cebola\" }, { \"name\": \"Sem picles\" }]",
            "\"Ponto da carne\"" to "\"Remover\"",
        )

        val modifiers = mapper.toDomainOrNull(dto)!!.items.single().modifiers

        assertEquals(listOf(Modifier("Remover", "Sem cebola"), Modifier("Remover", "Sem picles")), modifiers)
    }
}
