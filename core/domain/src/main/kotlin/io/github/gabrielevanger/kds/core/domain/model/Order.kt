package io.github.gabrielevanger.kds.core.domain.model

import java.time.Instant
import kotlinx.collections.immutable.ImmutableList

@JvmInline
value class OrderId(val value: Long)

/**
 * Pedido como a cozinha precisa dele. [version] cresce a cada alteração no servidor
 * e decide qual de duas informações sobre o mesmo pedido é a mais recente.
 */
data class Order(
    val id: OrderId,
    val reference: String,
    val origin: Origin,
    val stage: Stage,
    val paymentStatus: PaymentStatus,
    val table: Int?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long,
    val note: String?,
    val items: ImmutableList<OrderItem>,
) {
    /** Soma das quantidades: diferencia "uma coca" de "quatro combos". */
    val itemCount: Int = items.sumOf { it.quantity }

    /** Pedido que ocupa a cozinha por mais tempo e merece destaque na fila. */
    val isLarge: Boolean get() = itemCount >= LARGE_ORDER_ITEM_COUNT

    companion object {
        const val LARGE_ORDER_ITEM_COUNT = 5
    }
}

data class OrderItem(
    val id: String,
    val name: String,
    val productionArea: ProductionArea,
    val quantity: Int,
    val note: String?,
    val modifiers: ImmutableList<Modifier>,
)

/** Opção escolhida dentro de um grupo de complementos, ex.: grupo "Remover", opção "Sem cebola". */
data class Modifier(val group: String, val option: String)

enum class Stage {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DONE,
    CANCELED,
}

/** Canal de venda. [OTHER] absorve canais novos do back sem quebrar a leitura do pedido. */
enum class Origin {
    POS,
    WHATSAPP_AI,
    IFOOD,
    MARKETPLACE,
    CARDAPIO_WEB,
    CLIENTE_FIEL,
    OTHER,
}

/** Estação da cozinha que prepara o item. [OTHER] absorve estações novas do back. */
enum class ProductionArea {
    CHAPA,
    FRITADEIRA,
    MONTAGEM,
    OTHER,
}

enum class PaymentStatus {
    PAID,
    NOT_PAID,
}
