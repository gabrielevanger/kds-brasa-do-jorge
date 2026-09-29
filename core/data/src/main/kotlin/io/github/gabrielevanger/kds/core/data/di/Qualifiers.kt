package io.github.gabrielevanger.kds.core.data.di

import javax.inject.Qualifier

/** Cliente HTTP do stream SSE, com timeout de leitura ligado ao heartbeat e sem limite de duração. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StreamHttpClient

/** Escopo que vive enquanto o processo do app estiver ativo. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
