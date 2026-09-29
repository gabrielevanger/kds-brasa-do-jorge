package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.remote.dto.ApiErrorDto
import io.github.gabrielevanger.kds.core.data.remote.dto.OrderDto
import io.github.gabrielevanger.kds.core.data.remote.dto.StageChangeRequestDto
import io.github.gabrielevanger.kds.core.domain.model.Order
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.domain.model.Stage
import io.github.gabrielevanger.kds.core.domain.sync.OrderCommands
import io.github.gabrielevanger.kds.core.domain.sync.StageChangeResult
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CONFLICT
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

/**
 * Envia a mudança de etapa ao servidor. O 409 é um desfecho esperado numa cozinha com vários
 * aparelhos, não uma exceção: o corpo traz o pedido como está, para a tela se corrigir sozinha.
 */
class RemoteOrderCommands(private val api: OrdersApi, private val json: Json, private val mapper: OrderMapper) :
    OrderCommands {

    override suspend fun changeStage(orderId: OrderId, to: Stage): StageChangeResult = try {
        val response = api.changeStage(orderId.value, StageChangeRequestDto(stage = to.name))
        when {
            response.isSuccessful -> response.body()?.let(mapper::toDomainOrNull)
                ?.let(StageChangeResult::Confirmed)
                ?: StageChangeResult.Failed(IllegalStateException("Resposta de sucesso sem pedido válido"))

            response.code() == HTTP_CONFLICT -> currentOrderFrom(response)
                ?.let(StageChangeResult::Rejected)
                ?: StageChangeResult.Failed(HttpException(response))

            else -> StageChangeResult.Failed(HttpException(response))
        }
    } catch (e: IOException) {
        StageChangeResult.Failed(e)
    } catch (e: IllegalArgumentException) {
        // Resposta de sucesso com JSON que não corresponde ao contrato.
        StageChangeResult.Failed(e)
    }

    private fun currentOrderFrom(response: Response<OrderDto>): Order? {
        val body = response.errorBody()?.string() ?: return null
        val error = try {
            json.decodeFromString<ApiErrorDto>(body)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return error.order?.let(mapper::toDomainOrNull)
    }
}
