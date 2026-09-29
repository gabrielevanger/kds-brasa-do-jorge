package io.github.gabrielevanger.kds.feature.board

import io.github.gabrielevanger.kds.core.designsystem.theme.KitchenDarkColors
import io.github.gabrielevanger.kds.core.designsystem.theme.KitchenLightColors
import io.github.gabrielevanger.kds.core.domain.kitchen.WaitBand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** A faixa lateral repete a cor do selo do timer, para os atrasados se destacarem de longe. */
class UrgencyStripeTest {

    @Test
    fun `tempo normal nao tem faixa`() {
        assertNull(WaitBand.NORMAL.stripeColor(KitchenDarkColors))
    }

    @Test
    fun `atencao e atraso usam as cores do selo do timer nos dois temas`() {
        listOf(KitchenDarkColors, KitchenLightColors).forEach { colors ->
            assertEquals(colors.attention, WaitBand.ATTENTION.stripeColor(colors))
            assertEquals(colors.late, WaitBand.LATE.stripeColor(colors))
        }
    }
}
