package io.github.gabrielevanger.kds.feature.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderStore
import io.github.gabrielevanger.kds.core.domain.kitchen.StoreNotice
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BoardViewModel @Inject constructor(private val store: OrderStore) : ViewModel() {

    private val stationFilter = MutableStateFlow(StationFilter.ALL)

    /** O mapeamento roda fora da thread principal: no pico são centenas de pedidos a cada evento. */
    val uiState: StateFlow<BoardUiState> = combine(store.state, store.connection, stationFilter, BoardUiMapper::map)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MILLIS), BoardUiState.Initial)

    val notices: Flow<StoreNotice> = store.notices

    fun onAdvance(orderId: OrderId) = store.advance(orderId)

    fun onUndo(orderId: OrderId) = store.undo(orderId)

    fun onDismissAlert(orderId: OrderId) = store.dismissAlert(orderId)

    fun onStationFilterSelected(filter: StationFilter) {
        stationFilter.value = filter
    }

    private companion object {
        /** Mantém a coleta durante uma rotação de tela, sem reiniciar o mapeamento. */
        const val STOP_SHARING_AFTER_MILLIS = 5_000L
    }
}
