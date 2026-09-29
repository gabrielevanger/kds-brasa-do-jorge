package io.github.gabrielevanger.kds.core.data

/** Registro de situações anormais que não devem interromper o app. */
fun interface DiagnosticLog {
    fun warn(message: String, cause: Throwable?)
}
