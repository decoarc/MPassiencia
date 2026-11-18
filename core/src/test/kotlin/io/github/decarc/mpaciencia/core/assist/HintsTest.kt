package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.col
import io.github.decarc.mpaciencia.core.down
import io.github.decarc.mpaciencia.core.foundationWith
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef.Foundation
import io.github.decarc.mpaciencia.core.model.PileRef.Tableau
import io.github.decarc.mpaciencia.core.model.PileRef.Waste
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.DIAMONDS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.Suit.SPADES
import io.github.decarc.mpaciencia.core.state
import io.github.decarc.mpaciencia.core.up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HintsTest {
    // cartas que não jogam em lugar nenhum nos estados abaixo
    private val dead1 = card(7, HEARTS, 1)
    private val dead2 = card(7, DIAMONDS, 1)
    private val dead3 = card(6, HEARTS, 2)

    @Test
    fun `foundation move is hinted first`() {
        val s = state(
            waste = listOf(card(9, HEARTS)),
            tableau = listOf(col(up(10, SPADES)), col(down(4, CLUBS), up(1, CLUBS))),
        )
        assertEquals(Hint.Play(Move.Transfer(Tableau(1), Foundation(0))), Hints.all(s).first())
    }

    @Test
    fun `move that reveals a face-down card is hinted`() {
        val s = state(tableau = listOf(col(down(4, CLUBS), up(9, HEARTS)), col(up(10, CLUBS))))
        assertEquals(listOf<Hint>(Hint.Play(Move.Transfer(Tableau(0), Tableau(1)))), Hints.all(s))
    }

    @Test
    fun `pointless column-to-column shuffle is not hinted`() {
        val s = state(tableau = listOf(col(up(10, CLUBS), up(9, HEARTS)), col(up(10, SPADES)), col(up(12, CLUBS))))
        assertTrue(Hints.all(s).none { it is Hint.Play })
    }

    @Test
    fun `emptying a column is useful only when a king is waiting`() {
        val noKing = state(tableau = listOf(col(up(9, HEARTS)), col(up(10, CLUBS))))
        assertTrue(Hints.all(noKing).none { it is Hint.Play })
        val kingWaiting = state(waste = listOf(card(13, SPADES)), tableau = listOf(col(up(9, HEARTS)), col(up(10, CLUBS))))
        assertTrue(Hint.Play(Move.Transfer(Tableau(0), Tableau(1))) in Hints.all(kingWaiting))
    }

    @Test
    fun `waste to column is hinted`() {
        val s = state(waste = listOf(card(9, HEARTS)), tableau = listOf(col(up(10, SPADES))))
        assertEquals(listOf<Hint>(Hint.Play(Move.Transfer(Waste, Tableau(0)))), Hints.all(s))
    }

    @Test
    fun `foundation to column is hinted only when it enables a useful play`() {
        val f = foundationWith(0, card(1, SPADES), card(2, SPADES), card(3, SPADES), card(4, SPADES), card(5, SPADES))
        val enabling = state(foundations = f, tableau = listOf(col(up(6, HEARTS)), col(down(9, CLUBS), up(4, HEARTS))))
        assertTrue(Hint.Play(Move.Transfer(Foundation(0), Tableau(0))) in Hints.all(enabling))
        val useless = state(foundations = f, tableau = listOf(col(up(6, HEARTS))))
        assertTrue(Hints.all(useless).none { it is Hint.Play })
    }

    @Test
    fun `no table moves but stock has a playable card hints DrawStock`() {
        val s = state(stock = listOf(card(1, SPADES), dead1), tableau = listOf(col(up(12, CLUBS))))
        assertEquals(listOf<Hint>(Hint.DrawStock), Hints.all(s))
        assertFalse(Hints.isStuck(s))
    }

    @Test
    fun `no table moves, empty stock, playable card in waste hints Recycle`() {
        val s = state(waste = listOf(card(1, SPADES), dead1), tableau = listOf(col(up(12, CLUBS))))
        assertEquals(listOf<Hint>(Hint.Recycle), Hints.all(s))
    }

    @Test
    fun `stuck when no card of a full stock pass can ever be played`() {
        val s = state(stock = listOf(dead1, dead2), waste = listOf(dead3), tableau = listOf(col(up(12, CLUBS))))
        assertTrue(Hints.isStuck(s))
        assertEquals(listOf<Hint>(Hint.Stuck), Hints.all(s))
    }

    @Test
    fun `not stuck when a card deep in the stock becomes playable in draw one`() {
        val s = state(stock = listOf(card(1, HEARTS), dead1, dead2, dead3), tableau = listOf(col(up(12, CLUBS))))
        assertFalse(Hints.isStuck(s))
    }

    @Test
    fun `in draw three a card unreachable in the cycle does not prevent stuck`() {
        // monte [dead1, Ás, dead2] (topo = dead2): vira 3 deixa dead1 no topo do descarte; o Ás nunca fica no topo
        val s = state(drawMode = DrawMode.THREE, stock = listOf(dead1, card(1, HEARTS), dead2), tableau = listOf(col(up(12, CLUBS))))
        assertTrue(Hints.isStuck(s))
    }

    @Test
    fun `won game is not reported as stuck and has no hints`() {
        val full: List<List<Card>> = (0 until 3).flatMap { deck ->
            Suit.entries.map { suit -> (1..13).map { Card(suit, it, deck) } }
        }
        val s = state(foundations = full)
        assertTrue(s.isWon)
        assertFalse(Hints.isStuck(s))
        assertTrue(Hints.all(s).isEmpty())
    }

    @Test
    fun `king at the bottom of a column is never hinted to an empty column`() {
        val s = state(tableau = listOf(col(up(13, SPADES), up(12, HEARTS)), col()))
        assertTrue(Hints.all(s).none { it is Hint.Play })
    }
}
