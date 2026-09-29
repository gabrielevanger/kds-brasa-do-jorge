package io.github.gabrielevanger.kds.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Tempo de leitura do aviso de falha, igual à janela de desfazer para manter um ritmo único. */
private val NOTICE_DURATION = 5.seconds

/** Mantém cada aviso visível por alguns segundos; um aviso novo substitui o anterior. */
@Composable
fun rememberVisibleNotice(notices: Flow<KitchenNotice>): State<KitchenNotice?> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val visible = remember { mutableStateOf<KitchenNotice?>(null) }
    LaunchedEffect(notices, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            notices.collectLatest { notice ->
                visible.value = notice
                delay(NOTICE_DURATION)
                visible.value = null
            }
        }
    }
    return visible
}
