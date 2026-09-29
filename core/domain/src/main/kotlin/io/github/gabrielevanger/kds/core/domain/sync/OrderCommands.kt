package io.github.gabrielevanger.kds.core.domain.sync

import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage

/** Comandos enviados ao servidor. Não lança exceção: todo desfecho vira um [StageChangeResult]. */
interface OrderCommands {
    suspend fun changeStage(orderId: OrderId, to: Stage): StageChangeResult
}

sealed interface StageChangeResult {

    data class Confirmed(val order: Order) : StageChangeResult

    /** O servidor recusou a transição (outro aparelho mexeu no pedido) e informou como ele está. */
    data class Rejected(val currentOrder: Order) : StageChangeResult

    /** A transição não foi aplicada e não há estado do servidor para reconciliar. */
    data class Failed(val cause: Throwable) : StageChangeResult
}
