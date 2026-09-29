package io.github.gabrielevanger.kds.core.domain.testing

import io.github.gabrielevanger.kds.core.domain.model.Modifier
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.OrderItem
import io.github.gabrielevanger.kds.core.domain.model.Origin
import io.github.gabrielevanger.kds.core.domain.model.PaymentStatus
import io.github.gabrielevanger.kds.core.domain.model.ProductionArea
import io.github.gabrielevanger.kds.core.domain.model.Stage
import java.time.Instant
import kotlinx.collections.immutable.toImmutableList

val BASE_TIME: Instant = Instant.parse("2026-09-25T23:00:00Z")

fun anOrder(
    id: Long = 1,
    stage: Stage = Stage.PENDING,
    version: Long = 1,
    origin: Origin = Origin.POS,
    table: Int? = null,
    createdAt: Instant = BASE_TIME,
    items: List<OrderItem> = listOf(anItem()),
): Order = Order(
    id = OrderId(id),
    reference = "#%04d".format(id),
    origin = origin,
    stage = stage,
    paymentStatus = PaymentStatus.PAID,
    table = table,
    createdAt = createdAt,
    updatedAt = createdAt,
    version = version,
    note = null,
    items = items.toImmutableList(),
)

fun anItem(
    name: String = "Smash Clássico",
    quantity: Int = 1,
    productionArea: ProductionArea = ProductionArea.CHAPA,
    modifiers: List<Modifier> = emptyList(),
): OrderItem = OrderItem(
    id = name,
    name = name,
    productionArea = productionArea,
    quantity = quantity,
    note = null,
    modifiers = modifiers.toImmutableList(),
)
