package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.col
import io.github.decarc.mpaciencia.core.down
import io.github.decarc.mpaciencia.core.engine.Engine
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.model.TableauCard
import io.github.decarc.mpaciencia.core.state
import io.github.decarc.mpaciencia.core.up
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutoCompleteTest {
    /** Fundações com Ás a 10 de cada naipe/baralho; Valetes, Damas e Reis em 12 colunas válidas. */
    private fun nearlyWon(): GameState {
        val foundations = (0 until 3).flatMap { deck -> Suit.entries.map { suit -> (1..10).map { Card(suit, it, deck) } } }
        val black = listOf(Suit.SPADES, Suit.CLUBS)
        val red = listOf(Suit.HEARTS, Suit.DIAMONDS)
        val columns = (0 until 3).flatMap { deck ->
            (0 until 2).flatMap { i ->
                listOf(
                    listOf(Card(black[i], 13, deck), Card(red[i], 12, deck), Card(black[i], 11, deck)),
                    listOf(Card(red[i], 13, deck), Card(black[i], 12, deck), Card(red[i], 11, deck)),
                )
            }
        }.map { cards -> cards.map { TableauCard(it, faceUp = true) } }
        return state(foundations = foundations, tableau = columns)
    }

    @Test
    fun `canFinish when everything is face up and stock and waste are empty`() {
        assertTrue(AutoComplete.canFinish(nearlyWon()))
    }

    @Test
    fun `canFinish is false while stock or waste has cards`() {
        assertFalse(AutoComplete.canFinish(state(stock = listOf(card(5, CLUBS)), tableau = listOf(col(up(4, HEARTS))))))
        assertFalse(AutoComplete.canFinish(state(waste = listOf(card(5, CLUBS)), tableau = listOf(col(up(4, HEARTS))))))
    }

    @Test
    fun `canFinish is false while any tableau card is face down`() {
        assertFalse(AutoComplete.canFinish(state(tableau = listOf(col(down(5, CLUBS), up(4, HEARTS))))))
    }

    @Test
    fun `repeated nextMove wins the game`() {
        var s = nearlyWon()
        var steps = 0
        while (true) {
            val move = AutoComplete.nextMove(s) ?: break
            s = Engine.apply(s, move)!!
            steps++
        }
        assertTrue(s.isWon)
        assertEquals(36, steps)
        assertEquals(360, s.score)
        assertFalse(AutoComplete.canFinish(s))
        assertNull(AutoComplete.nextMove(s))
    }
}
