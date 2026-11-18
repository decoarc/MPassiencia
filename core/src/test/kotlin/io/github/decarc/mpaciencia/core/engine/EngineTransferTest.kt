package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.col
import io.github.decarc.mpaciencia.core.down
import io.github.decarc.mpaciencia.core.foundationWith
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef.Foundation
import io.github.decarc.mpaciencia.core.model.PileRef.Tableau
import io.github.decarc.mpaciencia.core.model.PileRef.Waste
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.Suit.SPADES
import io.github.decarc.mpaciencia.core.state
import io.github.decarc.mpaciencia.core.up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTransferTest {
    @Test
    fun `group move flips the exposed card and scores`() {
        val s = state(tableau = listOf(col(down(7, CLUBS), up(9, HEARTS), up(8, SPADES)), col(up(10, CLUBS))))
        val r = Engine.apply(s, Move.Transfer(Tableau(0), Tableau(1), count = 2))!!
        assertEquals(listOf(10, 9, 8), r.tableau[1].map { it.card.rank })
        assertTrue(r.tableau[0].single().faceUp)
        assertEquals(5, r.score)
        assertEquals(1, r.moves)
    }

    @Test
    fun `waste to column scores 5`() {
        val s = state(waste = listOf(card(9, HEARTS)), tableau = listOf(col(up(10, SPADES))))
        val r = Engine.apply(s, Move.Transfer(Waste, Tableau(0)))!!
        assertTrue(r.waste.isEmpty())
        assertEquals(card(9, HEARTS), r.tableau[0].last().card)
        assertTrue(r.tableau[0].last().faceUp)
        assertEquals(5, r.score)
    }

    @Test
    fun `column to foundation scores 10 plus 5 for the flip`() {
        val s = state(tableau = listOf(col(down(4, CLUBS), up(1, SPADES))))
        val r = Engine.apply(s, Move.Transfer(Tableau(0), Foundation(3)))!!
        assertEquals(listOf(card(1, SPADES)), r.foundations[3])
        assertTrue(r.tableau[0].single().faceUp)
        assertEquals(15, r.score)
    }

    @Test
    fun `waste to foundation scores 10`() {
        val s = state(waste = listOf(card(1, HEARTS)))
        val r = Engine.apply(s, Move.Transfer(Waste, Foundation(0)))!!
        assertEquals(10, r.score)
        assertEquals(1, r.moves)
    }

    @Test
    fun `foundation to tableau costs 15 but score never below 0`() {
        val s = state(score = 10, foundations = foundationWith(0, card(1, SPADES), card(2, SPADES)), tableau = listOf(col(up(3, HEARTS))))
        val r = Engine.apply(s, Move.Transfer(Foundation(0), Tableau(0)))!!
        assertEquals(0, r.score)
        assertEquals(listOf(card(1, SPADES)), r.foundations[0])
        assertEquals(card(2, SPADES), r.tableau[0].last().card)
    }

    @Test
    fun `foundation to tableau from a higher score subtracts 15`() {
        val s = state(score = 40, foundations = foundationWith(0, card(1, SPADES), card(2, SPADES)), tableau = listOf(col(up(3, HEARTS))))
        assertEquals(25, Engine.apply(s, Move.Transfer(Foundation(0), Tableau(0)))!!.score)
    }

    @Test
    fun `illegal move returns null`() {
        val s = state(tableau = listOf(col(up(9, CLUBS)), col(up(10, SPADES))))
        assertNull(Engine.apply(s, Move.Transfer(Tableau(0), Tableau(1))))
    }

    @Test
    fun `out of range index is illegal, not a crash`() {
        assertNull(Engine.apply(Deal.newGame(1, DrawMode.ONE), Move.Transfer(Tableau(13), Tableau(0))))
    }
}
