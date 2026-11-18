package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.col
import io.github.decarc.mpaciencia.core.down
import io.github.decarc.mpaciencia.core.foundationWith
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef.Foundation
import io.github.decarc.mpaciencia.core.model.PileRef.Stock
import io.github.decarc.mpaciencia.core.model.PileRef.Tableau
import io.github.decarc.mpaciencia.core.model.PileRef.Waste
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.DIAMONDS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.Suit.SPADES
import io.github.decarc.mpaciencia.core.state
import io.github.decarc.mpaciencia.core.up
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {
    private fun legal(s: io.github.decarc.mpaciencia.core.model.GameState, m: Move) = Rules.isLegal(s, m)

    // --- colunas ---
    @Test
    fun `group onto opposite color one rank higher is legal`() {
        val s = state(tableau = listOf(col(up(9, HEARTS), up(8, SPADES)), col(up(10, CLUBS))))
        assertTrue(legal(s, Move.Transfer(Tableau(0), Tableau(1), 2)))
    }

    @Test
    fun `same color is illegal`() {
        val s = state(tableau = listOf(col(up(9, CLUBS)), col(up(10, SPADES))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(1))))
    }

    @Test
    fun `wrong rank is illegal`() {
        val s = state(tableau = listOf(col(up(8, HEARTS)), col(up(10, SPADES))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(1))))
    }

    @Test
    fun `empty column takes a king group only`() {
        val s = state(tableau = listOf(col(up(13, HEARTS), up(12, SPADES)), col(), col(up(12, HEARTS))))
        assertTrue(legal(s, Move.Transfer(Tableau(0), Tableau(1), 2)))
        assertFalse(legal(s, Move.Transfer(Tableau(2), Tableau(1))))
    }

    @Test
    fun `face-down card cannot be moved`() {
        val s = state(tableau = listOf(col(down(7, CLUBS), up(9, HEARTS)), col(up(10, CLUBS)), col(up(8, HEARTS))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(2), 2)))
    }

    @Test
    fun `broken sequence cannot move as group`() {
        val s = state(tableau = listOf(col(up(9, HEARTS), up(8, HEARTS)), col(up(10, CLUBS))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(1), 2)))
    }

    @Test
    fun `cannot place onto a face-down top`() {
        val s = state(tableau = listOf(col(up(9, HEARTS)), col(down(10, CLUBS))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(1))))
    }

    // --- descarte ---
    @Test
    fun `waste top goes to a column, only one card`() {
        val s = state(waste = listOf(card(5, CLUBS), card(9, HEARTS)), tableau = listOf(col(up(10, SPADES))))
        assertTrue(legal(s, Move.Transfer(Waste, Tableau(0))))
        assertFalse(legal(s, Move.Transfer(Waste, Tableau(0), 2)))
    }

    @Test
    fun `empty waste cannot be moved`() {
        val s = state(tableau = listOf(col(up(10, SPADES))))
        assertFalse(legal(s, Move.Transfer(Waste, Tableau(0))))
    }

    // --- fundações ---
    @Test
    fun `any empty foundation takes any ace`() {
        val s = state(tableau = listOf(col(up(1, SPADES))))
        assertTrue(legal(s, Move.Transfer(Tableau(0), Foundation(7))))
    }

    @Test
    fun `foundation builds up by suit`() {
        val f = foundationWith(0, card(1, HEARTS))
        assertTrue(legal(state(foundations = f, tableau = listOf(col(up(2, HEARTS)))), Move.Transfer(Tableau(0), Foundation(0))))
        assertFalse(legal(state(foundations = f, tableau = listOf(col(up(2, DIAMONDS)))), Move.Transfer(Tableau(0), Foundation(0))))
        assertFalse(legal(state(foundations = f, tableau = listOf(col(up(3, HEARTS)))), Move.Transfer(Tableau(0), Foundation(0))))
        assertFalse(legal(state(foundations = f, tableau = listOf(col(up(1, HEARTS, 1)))), Move.Transfer(Tableau(0), Foundation(0))))
    }

    @Test
    fun `only one card at a time goes to a foundation`() {
        val s = state(tableau = listOf(col(up(2, SPADES), up(1, HEARTS))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Foundation(0), 2)))
    }

    @Test
    fun `foundation top can go back to a column`() {
        val s = state(foundations = foundationWith(0, card(1, SPADES), card(2, SPADES)), tableau = listOf(col(up(3, HEARTS))))
        assertTrue(legal(s, Move.Transfer(Foundation(0), Tableau(0))))
    }

    @Test
    fun `foundation to foundation is illegal`() {
        val s = state(foundations = foundationWith(0, card(1, SPADES)))
        assertFalse(legal(s, Move.Transfer(Foundation(0), Foundation(1))))
    }

    @Test
    fun `nothing moves after the game is won`() {
        val full = (0 until 3).flatMap { deck ->
            io.github.decarc.mpaciencia.core.model.Suit.entries.map { suit -> (1..13).map { card(it, suit, deck) } }
        }
        val s = state(foundations = full)
        assertTrue(s.isWon)
        assertFalse(legal(s, Move.Transfer(Foundation(0), Tableau(0))))
        assertTrue(Engine.apply(s, Move.Transfer(Foundation(0), Tableau(0))) == null)
    }

    // --- jogadas impossíveis ---
    @Test
    fun `same pile, stock and waste targets are illegal`() {
        val s = state(stock = listOf(card(1, SPADES)), waste = listOf(card(5, HEARTS)), tableau = listOf(col(up(6, CLUBS))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(0))))
        assertFalse(legal(s, Move.Transfer(Stock, Foundation(0))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Waste)))
        assertFalse(legal(s, Move.Transfer(Waste, Stock)))
    }

    @Test
    fun `out of range indexes and bad counts are illegal, not a crash`() {
        val s = Deal.newGame(1, DrawMode.ONE)
        assertFalse(legal(s, Move.Transfer(Tableau(13), Tableau(0))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Tableau(-1))))
        assertFalse(legal(s, Move.Transfer(Tableau(0), Foundation(12))))
        assertFalse(legal(s, Move.Transfer(Tableau(5), Tableau(6), 0)))
        assertFalse(legal(s, Move.Transfer(Tableau(5), Tableau(6), 99)))
    }
}
