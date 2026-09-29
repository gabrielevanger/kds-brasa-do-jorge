package io.github.gabrielevanger.kds.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gabrielevanger.kds.app.BuildConfig
import io.github.gabrielevanger.kds.core.data.ServerConfig
import java.time.ZoneOffset
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerConfigModule {

    /** O mock grava datas sem fuso em UTC (toISOString().slice(0, 19) no server.js). */
    @Provides
    @Singleton
    fun serverConfig(): ServerConfig = ServerConfig(baseUrl = BuildConfig.KDS_SERVER_URL, sourceZone = ZoneOffset.UTC)
}
