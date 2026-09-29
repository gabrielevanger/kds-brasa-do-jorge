package io.github.gabrielevanger.kds.core.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow

/**
 * Toca os sinais da cozinha enquanto a tela está visível. Usa o canal de alarme, cujo volume
 * costuma ficar alto e não é silenciado pelo modo de notificações.
 */
@Composable
fun KitchenSoundEffect(signals: Flow<Set<KitchenSignal>>) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val policy = remember { KitchenSoundPolicy() }
    val tones = remember { createToneGenerator() }
    DisposableEffect(tones) {
        onDispose { tones?.release() }
    }
    LaunchedEffect(signals, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            signals.collect { received ->
                when (policy.soundFor(received, now = SystemClock.elapsedRealtime().milliseconds)) {
                    KitchenSound.ALARM -> tones?.startTone(ALARM_TONE, ALARM_DURATION_MILLIS)
                    KitchenSound.BEEP -> tones?.startTone(BEEP_TONE, BEEP_DURATION_MILLIS)
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

private const val LOG_TAG = "KDS"

/** Bip duplo e curto: pedido novo é rotina. */
private const val BEEP_TONE = ToneGenerator.TONE_PROP_BEEP2
private const val BEEP_DURATION_MILLIS = 400

/** Padrão longo e diferente do bip: cancelamento exige parar o que se está fazendo. */
private const val ALARM_TONE = ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
private const val ALARM_DURATION_MILLIS = 2_000
