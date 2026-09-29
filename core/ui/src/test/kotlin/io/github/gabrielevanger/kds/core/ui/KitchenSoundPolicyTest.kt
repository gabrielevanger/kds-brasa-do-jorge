package io.github.gabrielevanger.kds.core.ui

import io.github.gabrielevanger.kds.core.domain.kitchen.KitchenSignal
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class KitchenSoundPolicyTest {

    private val policy = KitchenSoundPolicy(minBeepInterval = 3.seconds)
    private val newOrder = setOf(KitchenSignal.NEW_ORDER)
    private val cancellation = setOf(KitchenSignal.CANCELLATION)

    @Test
    fun `pedido novo toca bip`() {
        assertEquals(KitchenSound.BEEP, policy.soundFor(newOrder, now = 10.seconds))
    }

    @Test
    fun `rajada de pedidos dentro do intervalo toca um unico bip`() {
        val sounds = listOf(0, 500, 1_000, 2_999).map { policy.soundFor(newOrder, now = it.milliseconds) }

        assertEquals(listOf(KitchenSound.BEEP, null, null, null), sounds)
    }

    @Test
    fun `bip volta a tocar depois do intervalo minimo`() {
        policy.soundFor(newOrder, now = 0.seconds)

        assertEquals(KitchenSound.BEEP, policy.soundFor(newOrder, now = 3.seconds))
    }

    @Test
    fun `cancelamento toca alarme mesmo logo depois de um bip`() {
        policy.soundFor(newOrder, now = 0.seconds)

        assertEquals(KitchenSound.ALARM, policy.soundFor(cancellation, now = 100.milliseconds))
    }

    @Test
    fun `alarme tem prioridade quando pedido novo e cancelamento chegam juntos`() {
        assertEquals(KitchenSound.ALARM, policy.soundFor(newOrder + cancellation, now = 0.seconds))
    }

    @Test
    fun `alarme nao consome o intervalo do bip`() {
        policy.soundFor(cancellation, now = 0.seconds)

        assertEquals(KitchenSound.BEEP, policy.soundFor(newOrder, now = 100.milliseconds))
    }

    @Test
    fun `sem sinais nao toca nada`() {
        assertNull(policy.soundFor(emptySet(), now = 0.seconds))
    }
}
