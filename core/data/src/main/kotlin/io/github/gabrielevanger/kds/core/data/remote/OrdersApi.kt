package io.github.gabrielevanger.kds.core.data.remote

import io.github.gabrielevanger.kds.core.data.remote.dto.OrderDto
import io.github.gabrielevanger.kds.core.data.remote.dto.StageChangeRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

interface OrdersApi {

    @PATCH("orders/{id}")
    suspend fun changeStage(@Path("id") id: Long, @Body body: StageChangeRequestDto): Response<OrderDto>
}
