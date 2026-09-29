package io.github.gabrielevanger.kds.core.data.remote.dto

import kotlinx.serialization.Serializable

/*
 * Espelho do JSON do servidor. Valores enumerados chegam como String para que um valor novo
 * no back não quebre a desserialização; a interpretação fica no OrderMapper.
 */

@Serializable
data class OrderDto(
    val id: Long,
    val reference: String,
    val origin: String,
    val stage: String,
    val status: String,
    val table: Int? = null,
    val created: String,
    val updated: String,
    val version: Long,
    val note: String? = null,
    val orderItems: List<OrderItemDto> = emptyList(),
)

@Serializable
data class OrderItemDto(
    val id: String,
    val name: String,
    val productionArea: String,
    val quantity: Int,
    val note: String? = null,
    val attributes: List<AttributeDto> = emptyList(),
)

@Serializable
data class AttributeDto(val name: String, val items: List<AttributeItemDto> = emptyList())

@Serializable
data class AttributeItemDto(val name: String)

@Serializable
data class OrdersResponseDto(val orders: List<OrderDto>)

@Serializable
data class StageChangeRequestDto(val stage: String)

/** Corpo de erro do servidor. No 409, [order] traz o pedido como está no servidor. */
@Serializable
data class ApiErrorDto(val error: String, val code: String? = null, val order: OrderDto? = null)
