package io.github.gabrielevanger.kds.core.data.remote

/** Identificadores do contrato com o servidor de pedidos (mock/README.md), num único lugar. */
internal object ServerProtocol {
    const val EVENTS_PATH = "events"

    const val EVENT_SNAPSHOT = "snapshot"
    const val EVENT_ORDER_CREATED = "order.created"
    const val EVENT_ORDER_UPDATED = "order.updated"

    const val PAYMENT_STATUS_PAID = "PAID"
}
