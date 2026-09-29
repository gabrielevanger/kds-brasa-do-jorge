package io.github.gabrielevanger.kds.core.data

import android.util.Log
import javax.inject.Inject

class LogcatDiagnosticLog @Inject constructor() : DiagnosticLog {

    override fun warn(message: String, cause: Throwable?) {
        Log.w(TAG, message, cause)
    }

    private companion object {
        const val TAG = "KDS"
    }
}
