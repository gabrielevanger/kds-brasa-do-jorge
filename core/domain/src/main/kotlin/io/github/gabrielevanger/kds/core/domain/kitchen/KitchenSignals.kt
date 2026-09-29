package io.github.gabrielevanger.kds.core.domain.kitchen

/** O que merece um aviso sonoro na cozinha. */
enum class KitchenSignal {
    /** Chegou pedido que a tela ainda não conhecia. */
    NEW_ORDER,

    /** Um pedido já iniciado foi cancelado. */
    CANCELLATION,
}

/**
 * Decide os avisos sonoros comparando dois estados consecutivos. Só é novidade o que a tela não
 * conhecia: o primeiro snapshot, um pedido que muda de coluna ou uma reconexão com os mesmos
 * pedidos não tocam nada. Cada mudança de estado gera no máximo um sinal de cada tipo.
 */
object KitchenSignals {

    fun between(previous: KitchenState, next: KitchenState): Set<KitchenSignal> = buildSet {
        val newOrderArrived = previous.hasSnapshot && next.orders.keys.any { it !in previous.orders }
        if (newOrderArrived) add(KitchenSignal.NEW_ORDER)

        val newCancellation = next.cancellationAlerts.keys.any { it !in previous.cancellationAlerts }
        if (newCancellation) add(KitchenSignal.CANCELLATION)
    }
}
