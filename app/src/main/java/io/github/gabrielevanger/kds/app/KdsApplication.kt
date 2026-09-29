package io.github.gabrielevanger.kds.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderStore
import javax.inject.Inject

@HiltAndroidApp
class KdsApplication : Application() {

    @Inject
    lateinit var orderStore: OrderStore

    /** O tablet recebe pedidos enquanto o app estiver aberto, independente da tela visível. */
    override fun onCreate() {
        super.onCreate()
        orderStore.start()
    }
}
