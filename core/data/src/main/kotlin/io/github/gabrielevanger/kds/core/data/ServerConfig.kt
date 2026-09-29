package io.github.gabrielevanger.kds.core.data

import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Endereço e características do servidor de pedidos, fornecidos pelo app.
 *
 * [sourceZone] é o fuso em que o servidor grava datas sem fuso (UTC no mock).
 * [heartbeatInterval] é o intervalo do heartbeat SSE; o stream considera a conexão muda
 * depois de três intervalos sem nenhum dado.
 */
data class ServerConfig(val baseUrl: String, val sourceZone: ZoneId, val heartbeatInterval: Duration = 15.seconds) {
    init {
        require(baseUrl.endsWith("/")) { "baseUrl deve terminar com '/': $baseUrl" }
    }
}
