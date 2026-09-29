package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.remote.dto.OrderDto
import io.github.gabrielevanger.kds.core.data.remote.dto.OrderItemDto
import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.OrderItem
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.PaymentStatus
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.model.Stage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlinx.collections.immutable.toImmutableList

/**
 * Converte o pedido do servidor no modelo do domínio.
 *
 * O back envia datas ISO 8601 sem fuso ("2026-09-21T20:14:03"); o mock as gera em UTC.
 * Ler como hora local deslocaria o tempo de espera em 4 horas no fuso de Boa Vista, por isso
 * o fuso de origem é explícito e configurável. Datas que já trazem fuso são respeitadas.
 */
class OrderMapper(private val sourceZone: ZoneId) {

    /** Devolve null quando o pedido não pode ser interpretado com segurança (etapa ou data inválida). */
    fun toDomainOrNull(dto: OrderDto): Order? {
        val stage = enumValueOrNull<Stage>(dto.stage) ?: return null
        val createdAt = parseInstantOrNull(dto.created) ?: return null
        val updatedAt = parseInstantOrNull(dto.updated) ?: return null
        return Order(
            id = OrderId(dto.id),
            reference = dto.reference,
            origin = enumValueOrNull<Origin>(dto.origin) ?: Origin.OTHER,
            stage = stage,
            paymentStatus = paymentStatusOf(dto.status),
            table = dto.table,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = dto.version,
            note = dto.note,
            items = dto.orderItems.map(::toDomain).toImmutableList(),
        )
    }

    private fun toDomain(dto: OrderItemDto): OrderItem = OrderItem(
        id = dto.id,
        name = dto.name,
        productionArea = enumValueOrNull<ProductionArea>(dto.productionArea) ?: ProductionArea.OTHER,
        quantity = dto.quantity,
        note = dto.note,
        modifiers = dto.attributes
            .flatMap { group -> group.items.map { option -> Modifier(group = group.name, option = option.name) } }
            .toImmutableList(),
    )

    /** Status desconhecido é tratado como não pago: é melhor o garçom conferir do que deixar de cobrar. */
    private fun paymentStatusOf(value: String): PaymentStatus = when (value) {
        "PAID" -> PaymentStatus.PAID
        else -> PaymentStatus.NOT_PAID
    }

    private fun parseInstantOrNull(value: String): Instant? = try {
        when (val parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(value, ZonedDateTime::from, LocalDateTime::from)) {
            is ZonedDateTime -> parsed.toInstant()
            is LocalDateTime -> parsed.atZone(sourceZone).toInstant()
            else -> null
        }
    } catch (_: DateTimeParseException) {
        null
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value }
}
