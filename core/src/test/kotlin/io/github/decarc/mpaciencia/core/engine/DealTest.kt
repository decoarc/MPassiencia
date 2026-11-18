package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.DIAMONDS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.Suit.SPADES
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.fullDeck
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DealTest {
    @Test
    fun `layout of a new game`() {
        val s = Deal.newGame(220, DrawMode.ONE)
        assertEquals((1..13).toList(), s.tableau.map { it.size })
        s.tableau.forEach { col ->
            assertTrue(col.last().faceUp)
            assertTrue(col.dropLast(1).none { it.faceUp })
        }
        assertEquals(65, s.stock.size)
        assertTrue(s.waste.isEmpty())
        assertEquals(12, s.foundations.size)
        assertTrue(s.foundations.all { it.isEmpty() })
        val all = s.stock + s.tableau.flatten().map { it.card }
        assertEquals(156, all.size)
        assertEquals(fullDeck().toSet(), all.toSet())
        assertEquals(0, s.score)
        assertEquals(0, s.moves)
        assertEquals(220, s.gameNumber)
    }

    @Test
    fun `same number gives same deal, different numbers differ`() {
        assertEquals(Deal.newGame(5, DrawMode.ONE), Deal.newGame(5, DrawMode.ONE))
        assertNotEquals(Deal.newGame(5, DrawMode.ONE).stock, Deal.newGame(6, DrawMode.ONE).stock)
    }

    @Test
    fun `draw mode does not change the deal`() {
        assertEquals(Deal.newGame(9, DrawMode.ONE).tableau, Deal.newGame(9, DrawMode.THREE).tableau)
        assertEquals(Deal.newGame(9, DrawMode.ONE).stock, Deal.newGame(9, DrawMode.THREE).stock)
    }

    @Test
    fun `game number outside 1-99999 is rejected`() {
        assertFailsWith<IllegalArgumentException> { Deal.newGame(0, DrawMode.ONE) }
        assertFailsWith<IllegalArgumentException> { Deal.newGame(100_000, DrawMode.ONE) }
    }

    @Test
    fun `golden deal for game 1 never changes`() {
        val s = Deal.newGame(1, DrawMode.ONE)
        assertEquals(GOLDEN_GAME1_TABLEAU_TOPS, s.tableau.map { it.last().card })
        assertEquals(GOLDEN_GAME1_STOCK_TOP3, s.stock.takeLast(3))
    }

    private companion object {
        val GOLDEN_GAME1_TABLEAU_TOPS: List<Card> = listOf(
            Card(DIAMONDS, 12, 1), Card(SPADES, 10, 1), Card(SPADES, 5, 1), Card(HEARTS, 9, 2),
            Card(CLUBS, 12, 2), Card(DIAMONDS, 1, 2), Card(CLUBS, 5, 1), Card(HEARTS, 1, 2),
            Card(CLUBS, 1, 0), Card(CLUBS, 9, 0), Card(SPADES, 7, 2), Card(DIAMONDS, 12, 0),
            Card(CLUBS, 6, 2),
        )
        val GOLDEN_GAME1_STOCK_TOP3: List<Card> = listOf(Card(CLUBS, 3, 0), Card(HEARTS, 8, 2), Card(CLUBS, 7, 1))
    }
}
