package io.github.gabrielevanger.kds.core.data.network

import kotlinx.coroutines.flow.Flow

/** Disponibilidade de rede do aparelho. Emite o estado atual ao coletar e depois cada mudança. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
