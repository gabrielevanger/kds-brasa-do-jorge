package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.DiagnosticLog
import io.github.gabrielevanger.kds.core.data.remote.dto.OrderDto
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.sync.StreamEvent
import kotlinx.serialization.json.Json

/**
 * Traduz os eventos SSE do servidor. Evento malformado, de tipo desconhecido ou com pedido
 * inválido é descartado e registrado, sem interromper o stream.
 */
class StreamEventParser(private val json: Json, private val mapper: OrderMapper, private val log: DiagnosticLog) {

    fun parse(type: String?, data: String): StreamEvent? = try {
        when (type) {
            "snapshot" -> StreamEvent.Snapshot(json.decodeFromString<List<OrderDto>>(data).mapNotNull(::toDomain))

            "order.created", "order.updated" -> toDomain(json.decodeFromString<OrderDto>(data))
                ?.let(StreamEvent::OrderChanged)

            else -> null
        }
    } catch (e: IllegalArgumentException) {
        log.warn("Evento '$type' malformado ignorado", e)
        null
    }

    private fun toDomain(dto: OrderDto): Order? = mapper.toDomainOrNull(dto).also { order ->
        if (order == null) log.warn("Pedido ${dto.reference} ignorado: etapa ou data inválida", null)
    }
}
