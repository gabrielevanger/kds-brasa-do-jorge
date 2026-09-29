package io.github.gabrielevanger.kds.core.domain.kitchen

import io.github.gabrielevanger.kds.core.domain.model.Stage

/** O que merece um aviso sonoro na cozinha. Cada tela escolhe quais sinais tocam. */
enum class KitchenSignal {
    /** Chegou pedido que a tela ainda não conhecia. */
    NEW_ORDER,

    /** Um pedido já iniciado foi cancelado. */
    CANCELLATION,

    /** O servidor confirmou um pedido como pronto: há lanche esperando no balcão. */
    ORDER_READY,
}

/**
 * Decide os avisos sonoros comparando dois estados consecutivos. Só é novidade o que a tela não
 * conhecia: o primeiro snapshot, um toque ainda não confirmado ou uma reconexão com os mesmos
 * pedidos não tocam nada. Cada mudança de estado gera no máximo um sinal de cada tipo.
 */
object KitchenSignals {

    fun between(previous: KitchenState, next: KitchenState): Set<KitchenSignal> = buildSet {
        val newOrderArrived = previous.hasSnapshot && next.orders.keys.any { it !in previous.orders }
        if (newOrderArrived) add(KitchenSignal.NEW_ORDER)

        val newCancellation = next.cancellationAlerts.keys.any { it !in previous.cancellationAlerts }
        if (newCancellation) add(KitchenSignal.CANCELLATION)

        if (previous.hasSnapshot && becameReady(previous, next)) add(KitchenSignal.ORDER_READY)
    }

    /**
     * Compara a etapa confirmada pelo servidor, e não a otimista da tela: desfazer uma entrega não
     * "devolve" o pedido ao balcão, e o toque de pronto só avisa quando o servidor o aceita.
     */
    private fun becameReady(previous: KitchenState, next: KitchenState): Boolean = next.orders.values.any { order ->
        order.stage == Stage.READY && previous.orders[order.id]?.stage != Stage.READY
    }
}
