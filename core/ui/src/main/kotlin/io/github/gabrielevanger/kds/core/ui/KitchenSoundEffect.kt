package io.github.gabrielevanger.kds.core.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow

/**
 * Toca os sinais da cozinha enquanto a tela está visível. Usa o canal de alarme, cujo volume
 * costuma ficar alto e não é silenciado pelo modo de notificações. Com [vibrate], o aparelho
 * também vibra: o celular do garçom fica no bolso, onde o som pode não ser ouvido.
 */
@Composable
fun KitchenSoundEffect(signals: Flow<Set<KitchenSignal>>, vibrate: Boolean = false) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val policy = remember { KitchenSoundPolicy() }
    val tones = remember { createToneGenerator() }
    val vibrator = remember(vibrate) { if (vibrate) context.vibrator() else null }
    DisposableEffect(tones) {
        onDispose { tones?.release() }
    }
    LaunchedEffect(signals, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            signals.collect { received ->
                when (policy.soundFor(received, now = SystemClock.elapsedRealtime().milliseconds)) {
                    KitchenSound.ALARM -> {
                        tones?.startTone(ALARM_TONE, ALARM_DURATION_MILLIS)
                        vibrator?.vibrate(VibrationEffect.createWaveform(ALARM_VIBRATION, NO_REPEAT))
                    }

                    KitchenSound.BEEP -> {
                        tones?.startTone(BEEP_TONE, BEEP_DURATION_MILLIS)
                        vibrator?.vibrate(VibrationEffect.createWaveform(BEEP_VIBRATION, NO_REPEAT))
                    }

                    null -> Unit
                }
            }
        }
    }
}

/** Sem áudio disponível a tela segue funcionando, só que em silêncio: melhor mudo do que fechado. */
private fun createToneGenerator(): ToneGenerator? = try {
    ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
} catch (e: RuntimeException) {
    Log.w(LOG_TAG, "Áudio indisponível: a tela seguirá sem avisos sonoros", e)
    null
}

/** Aparelho sem motor de vibração (tablet, emulador) segue só com o som. */
private fun Context.vibrator(): Vibrator? {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        // Única forma antes do Android 12, onde o VibratorManager ainda não existe.
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
    return vibrator?.takeIf { it.hasVibrator() }
}

private const val LOG_TAG = "KDS"

/** Bip duplo e curto: pedido novo ou pronto é rotina. */
private const val BEEP_TONE = ToneGenerator.TONE_PROP_BEEP2
private const val BEEP_DURATION_MILLIS = 400

/** Padrão longo e diferente do bip: cancelamento exige parar o que se está fazendo. */
private const val ALARM_TONE = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
private const val ALARM_DURATION_MILLIS = 2_000

/** Pausa e pulsos em milissegundos: dois toques curtos acompanham o bip duplo. */
private val BEEP_VIBRATION = longArrayOf(0, 200, 150, 200)

/** Três pulsos longos, impossíveis de confundir com o aviso de pedido pronto. */
private val ALARM_VIBRATION = longArrayOf(0, 800, 300, 800, 300, 800)

/** Valor da API para tocar o padrão uma vez só. */
private const val NO_REPEAT = -1
