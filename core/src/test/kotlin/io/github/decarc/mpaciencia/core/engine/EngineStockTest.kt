package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.Suit.CLUBS
import io.github.decarc.mpaciencia.core.model.Suit.HEARTS
import io.github.decarc.mpaciencia.core.state
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineStockTest {
    private val c1 = card(1, CLUBS)
    private val c2 = card(2, CLUBS)
    private val c3 = card(3, CLUBS)
    private val c4 = card(4, HEARTS)

    @Test
    fun `draw one moves stock top to waste`() {
        val s = state(stock = listOf(c1, c2), waste = listOf(c3))
        val r = Engine.apply(s, Move.Draw)!!
        assertEquals(listOf(c1), r.stock)
        assertEquals(listOf(c3, c2), r.waste)
        assertEquals(1, r.moves)
        assertEquals(0, r.score)
        assertTrue(Rules.isLegal(s, Move.Draw))
    }

    @Test
    fun `draw three puts the third drawn card on top`() {
        val s = state(drawMode = DrawMode.THREE, stock = listOf(c1, c2, c3, c4))
        val r = Engine.apply(s, Move.Draw)!!
        assertEquals(listOf(c4, c3, c2), r.waste)
        assertEquals(listOf(c1), r.stock)
        assertEquals(1, r.moves)
    }

    @Test
    fun `draw three with two cards left draws two`() {
        val s = state(drawMode = DrawMode.THREE, stock = listOf(c1, c2))
        val r = Engine.apply(s, Move.Draw)!!
        assertEquals(listOf(c2, c1), r.waste)
        assertTrue(r.stock.isEmpty())
    }

    @Test
    fun `recycle restores original order`() {
        var s = Deal.newGame(3, DrawMode.ONE)
        val original = s.stock
        repeat(65) { s = Engine.apply(s, Move.Draw)!! }
        assertTrue(s.stock.isEmpty())
        s = Engine.apply(s, Move.Recycle)!!
        assertEquals(original, s.stock)
        assertTrue(s.waste.isEmpty())
        assertEquals(66, s.moves)
    }

    @Test
    fun `recycle in draw three also restores order`() {
        var s = Deal.newGame(8, DrawMode.THREE)
        val original = s.stock
        repeat(22) { s = Engine.apply(s, Move.Draw)!! } // 21 x 3 + 1 x 2 = 65
        assertTrue(s.stock.isEmpty())
        s = Engine.apply(s, Move.Recycle)!!
        assertEquals(original, s.stock)
    }

    @Test
    fun `recycle with empty stock and empty waste is illegal`() {
        val s = state()
        assertFalse(Rules.isLegal(s, Move.Recycle))
        assertNull(Engine.apply(s, Move.Recycle))
    }

    @Test
    fun `draw with empty stock is illegal`() {
        val s = state(waste = listOf(c1))
        assertFalse(Rules.isLegal(s, Move.Draw))
        assertNull(Engine.apply(s, Move.Draw))
    }

    @Test
    fun `recycle with cards still in stock is illegal`() {
        val s = state(stock = listOf(c1), waste = listOf(c2))
        assertFalse(Rules.isLegal(s, Move.Recycle))
        assertNull(Engine.apply(s, Move.Recycle))
    }
}
