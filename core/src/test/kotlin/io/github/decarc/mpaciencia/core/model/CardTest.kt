package io.github.decarc.mpaciencia.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CardTest {
    @Test
    fun `full deck has 156 unique cards`() {
        val deck = fullDeck()
        assertEquals(156, deck.size)
        assertEquals(156, deck.toSet().size)
        assertEquals(12, deck.count { it.rank == 1 })
        assertEquals(39, deck.count { it.suit == Suit.HEARTS })
    }

    @Test
    fun `suit colors`() {
        assertEquals(CardColor.RED, Suit.DIAMONDS.color)
        assertEquals(CardColor.RED, Suit.HEARTS.color)
        assertEquals(CardColor.BLACK, Suit.CLUBS.color)
        assertEquals(CardColor.BLACK, Suit.SPADES.color)
    }

    @Test
    fun `invalid rank or deck is rejected`() {
        assertFailsWith<IllegalArgumentException> { Card(Suit.SPADES, 14, 0) }
        assertFailsWith<IllegalArgumentException> { Card(Suit.SPADES, 0, 0) }
        assertFailsWith<IllegalArgumentException> { Card(Suit.SPADES, 1, 3) }
    }
}
