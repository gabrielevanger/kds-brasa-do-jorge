package io.github.gabrielevanger.kds.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderStore
import io.github.gabrielevanger.kds.core.domain.sync.ConnectionState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var orderStore: OrderStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Status provisório para validar a integração; substituído pelo board da cozinha.
                val connection by orderStore.connection.collectAsStateWithLifecycle()
                val state by orderStore.state.collectAsStateWithLifecycle()
                val status = when (val current = connection) {
                    ConnectionState.Connecting -> stringResource(R.string.status_connecting)
                    ConnectionState.Connected -> stringResource(R.string.status_connected)
                    is ConnectionState.Reconnecting -> stringResource(R.string.status_reconnecting, current.attempt)
                }
                // targetSdk 35+ desenha de ponta a ponta: o conteúdo precisa evitar as barras do sistema.
                Text(
                    text = pluralStringResource(R.plurals.status_summary, state.orders.size, status, state.orders.size),
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }
}
