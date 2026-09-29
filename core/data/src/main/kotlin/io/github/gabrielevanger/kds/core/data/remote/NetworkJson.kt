package io.github.gabrielevanger.kds.core.data.remote

import kotlinx.serialization.json.Json

/** Campos novos no back são ignorados em vez de derrubar a leitura dos pedidos. */
val NetworkJson: Json = Json {
    ignoreUnknownKeys = true
}
