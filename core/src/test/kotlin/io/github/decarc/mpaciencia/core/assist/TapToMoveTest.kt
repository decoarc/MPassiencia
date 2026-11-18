package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.col
import io.github.decarc.mpaciencia.core.down
import io.github.decarc.mpaciencia.core.foundationWith
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef.Foundation
import io.github.decarc.mpaciencia.core.model.PileRef.Tableau
import io.github.decarc.mpaciencia.core.model.PileRef.Waste
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.DIAMONDS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.Suit.SPADES
import io.github.decarc.mpaciencia.core.state
import io.github.decarc.mpaciencia.core.up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TapToMoveTest {
    @Test
    fun `single card prefers foundation over tableau`() {
        val s = state(
            foundations = foundationWith(2, card(1, HEARTS)),
            tableau = listOf(col(up(2, HEARTS)), col(up(3, CLUBS))),
        )
        assertEquals(Move.Transfer(Tableau(0), Foundation(2)), TapToMove.bestMove(s, Tableau(0)))
    }

    @Test
    fun `ace goes to the leftmost empty foundation`() {
        val s = state(foundations = foundationWith(0, card(1, CLUBS)), tableau = listOf(col(up(1, SPADES))))
        assertEquals(Move.Transfer(Tableau(0), Foundation(1)), TapToMove.bestMove(s, Tableau(0)))
    }

    @Test
    fun `group goes to a legal column, never to a foundation`() {
        val s = state(
            foundations = foundationWith(0, card(1, SPADES)),
            tableau = listOf(col(up(3, HEARTS), up(2, SPADES)), col(up(9, CLUBS)), col(up(4, CLUBS))),
        )
        assertEquals(Move.Transfer(Tableau(0), Tableau(2), 2), TapToMove.bestMove(s, Tableau(0), 2))
    }

    @Test
    fun `leftmost legal column wins`() {
        val s = state(tableau = listOf(col(up(10, SPADES)), col(up(9, HEARTS)), col(up(10, CLUBS))))
        assertEquals(Move.Transfer(Tableau(1), Tableau(0)), TapToMove.bestMove(s, Tableau(1)))
    }

    @Test
    fun `king at the bottom of a column is not moved to another empty column`() {
        val s = state(tableau = listOf(col(up(13, SPADES), up(12, HEARTS)), col(), col()))
        assertNull(TapToMove.bestMove(s, Tableau(0), 2))
    }

    @Test
    fun `king with cards under it can go to an empty column`() {
        val s = state(tableau = listOf(col(down(4, CLUBS), up(13, SPADES)), col(up(5, HEARTS)), col()))
        assertEquals(Move.Transfer(Tableau(0), Tableau(2)), TapToMove.bestMove(s, Tableau(0)))
    }

    @Test
    fun `tapping a face-down card returns null`() {
        val s = state(tableau = listOf(col(down(9, HEARTS), up(5, CLUBS)), col(up(10, SPADES))))
        assertNull(TapToMove.bestMove(s, Tableau(0), 2))
    }

    @Test
    fun `card with no destination returns null`() {
        val s = state(tableau = listOf(col(up(5, CLUBS)), col(up(10, SPADES))))
        assertNull(TapToMove.bestMove(s, Tableau(0)))
    }

    @Test
    fun `waste top goes to foundation when possible`() {
        val s = state(
            waste = listOf(card(2, DIAMONDS)),
            foundations = foundationWith(4, card(1, DIAMONDS)),
            tableau = listOf(col(up(3, CLUBS))),
        )
        assertEquals(Move.Transfer(Waste, Foundation(4)), TapToMove.bestMove(s, Waste))
    }

    @Test
    fun `foundation top can go back to a column`() {
        val s = state(foundations = foundationWith(0, card(1, SPADES), card(2, SPADES)), tableau = listOf(col(up(3, HEARTS))))
        assertEquals(Move.Transfer(Foundation(0), Tableau(0)), TapToMove.bestMove(s, Foundation(0)))
    }
}
