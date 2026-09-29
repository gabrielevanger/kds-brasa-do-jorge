package io.github.gabrielevanger.kds.core.domain.model

import io.github.gabrielevanger.kds.core.domain.testing.anItem
import io.github.gabrielevanger.kds.core.domain.testing.anOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OrderTest {

    @Test
    fun `itemCount soma as quantidades e nao a quantidade de linhas`() {
        val order = anOrder(
            items = listOf(
                anItem(name = "Smash Bacon", quantity = 2),
                anItem(name = "Coca-Cola Lata", quantity = 4),
            ),
        )

        assertEquals(6, order.itemCount)
    }

    @Test
    fun `pedido grande a partir de cinco itens somando as quantidades`() {
        val four = anOrder(items = listOf(anItem(quantity = 2), anItem(name = "Onion Rings", quantity = 2)))
        val five = anOrder(items = listOf(anItem(quantity = 2), anItem(name = "Onion Rings", quantity = 3)))

        assertFalse(four.isLarge)
        assertTrue(five.isLarge)
    }
}
