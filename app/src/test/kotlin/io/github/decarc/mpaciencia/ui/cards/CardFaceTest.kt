package io.github.decarc.mpaciencia.ui.cards

import io.github.decarc.mpaciencia.core.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals

class CardFaceTest {
    @Test
    fun `rank labels`() {
        assertEquals(listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"), (1..13).map(::rankLabel))
    }

    @Test
    fun `suit symbols`() {
        assertEquals("♥♦♠♣", Suit.entries.joinToString("") { suitSymbol(it) })
    }
}
