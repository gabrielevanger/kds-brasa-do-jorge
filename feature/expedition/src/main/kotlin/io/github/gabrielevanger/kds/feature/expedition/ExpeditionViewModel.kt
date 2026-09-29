package io.github.gabrielevanger.kds.feature.expedition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderStore
import io.github.gabrielevanger.kds.core.domain.model.OrderId
import io.github.gabrielevanger.kds.core.ui.KitchenNotice
import io.github.gabrielevanger.kds.core.ui.KitchenUiMapper
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ExpeditionViewModel @Inject constructor(private val store: OrderStore) : ViewModel() {

    /** O mapeamento roda fora da thread principal: o estado muda a cada evento da cozinha inteira. */
    val uiState: StateFlow<ExpeditionUiState> = combine(store.state, store.connection, ExpeditionUiMapper::map)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_SHARING_AFTER_MILLIS), ExpeditionUiState.Initial)

    /** Avisos pontuais já com o número do pedido e a etapa em que o card ficou. */
    val notices: Flow<KitchenNotice> = store.notices.mapNotNull { KitchenUiMapper.mapNotice(it, store.state.value) }

    /** No balcão a única ação é entregar: o pedido pronto avança para entregue. */
    fun onDeliver(orderId: OrderId) = store.advance(orderId)

    fun onUndo(orderId: OrderId) = store.undo(orderId)

    fun onDismissAlert(orderId: OrderId) = store.dismissAlert(orderId)

    private companion object {
        /** Mantém a coleta durante uma rotação de tela, sem reiniciar o mapeamento. */
        const val STOP_SHARING_AFTER_MILLIS = 5_000L
    }
}
