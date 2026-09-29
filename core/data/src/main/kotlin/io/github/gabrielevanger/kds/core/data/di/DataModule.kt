package io.github.gabrielevanger.kds.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.kds.core.data.DiagnosticLog
import io.github.gabrielevanger.kds.core.data.LogcatDiagnosticLog
import io.github.gabrielevanger.kds.core.data.ServerConfig
import io.github.gabrielevanger.kds.core.data.network.ConnectivityNetworkMonitor
import io.github.gabrielevanger.kds.core.data.network.NetworkMonitor
import io.github.gabrielevanger.kds.core.data.remote.BackoffPolicy
import io.github.gabrielevanger.kds.core.data.remote.NetworkJson
import io.github.gabrielevanger.kds.core.data.remote.OrderMapper
import io.github.gabrielevanger.kds.core.data.remote.OrdersApi
import io.github.gabrielevanger.kds.core.data.remote.ReconnectingOrderStream
import io.github.gabrielevanger.kds.core.data.remote.RemoteOrderCommands
import io.github.gabrielevanger.kds.core.data.remote.SseOrderConnection
import io.github.gabrielevanger.kds.core.data.remote.StreamEventParser
import io.github.gabrielevanger.kds.core.domain.kitchen.OrderStore
import io.github.gabrielevanger.kds.core.domain.sync.OrderCommands
import io.github.gabrielevanger.kds.core.domain.sync.OrderStream
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val HEARTBEATS_BEFORE_TIMEOUT = 3

    @Provides
    @Singleton
    fun json(): Json = NetworkJson

    @Provides
    @Singleton
    fun orderMapper(config: ServerConfig): OrderMapper = OrderMapper(config.sourceZone)

    /** Cliente das requisições pontuais: numa cozinha, esperar mais que isso já é falha. */
    @Provides
    @Singleton
    fun httpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Compartilha o pool de conexões do cliente base, mas sem limite de duração total da chamada. */
    @Provides
    @Singleton
    @StreamHttpClient
    fun streamHttpClient(base: OkHttpClient, config: ServerConfig): OkHttpClient = base.newBuilder()
        .readTimeout((config.heartbeatInterval * HEARTBEATS_BEFORE_TIMEOUT).toJavaDuration())
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Provides
    @Singleton
    fun ordersApi(client: OkHttpClient, json: Json, config: ServerConfig): OrdersApi = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OrdersApi::class.java)

    @Provides
    @Singleton
    fun orderCommands(api: OrdersApi, json: Json, mapper: OrderMapper): OrderCommands =
        RemoteOrderCommands(api, json, mapper)

    @Provides
    @Singleton
    fun orderStream(
        @StreamHttpClient client: OkHttpClient,
        config: ServerConfig,
        json: Json,
        mapper: OrderMapper,
        networkMonitor: NetworkMonitor,
        log: DiagnosticLog,
    ): OrderStream {
        val connection = SseOrderConnection(
            eventSourceFactory = EventSources.createFactory(client),
            eventsUrl = "${config.baseUrl}events".toHttpUrl(),
            parser = StreamEventParser(json, mapper, log),
        )
        return ReconnectingOrderStream(connection::open, BackoffPolicy(), networkMonitor, log)
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun orderStore(stream: OrderStream, commands: OrderCommands, @ApplicationScope scope: CoroutineScope): OrderStore =
        OrderStore(stream, commands, scope)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun diagnosticLog(impl: LogcatDiagnosticLog): DiagnosticLog
}
