package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.core.engine.Deal
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.core.model.TableauCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TableLayoutTest {
    private val newGame = Deal.newGame(1, DrawMode.ONE)

    /** Coluna 0 com [down] cartas viradas para baixo e uma sequência de [up] cartas (K para baixo, cores alternadas). */
    private fun columnState(down: Int, up: Int): GameState {
        val downs = List(down) { TableauCard(Card(Suit.CLUBS, (it % 13) + 1, 0), faceUp = false) }
        val ups = List(up) { i ->
            val rank = 13 - i
            TableauCard(Card(if (i % 2 == 0) Suit.SPADES else Suit.HEARTS, rank, 1), faceUp = true)
        }
        return newGame.copy(tableau = listOf(downs + ups) + List(12) { emptyList() })
    }

    @Test
    fun `new game on a Galaxy Note 10 fits comfortably`() {
        val g = TableLayout.compute(869f, 411f, newGame)
        assertTrue(g.cardW in 53f..57f, "cardW=${g.cardW}")
        assertEquals(g.cardW * TableLayout.ASPECT, g.cardH, 0.001f)
        assertEquals(12, g.foundations.size)
        assertEquals(13, g.columns.size)
        assertTrue(g.foundations.last().let { it.x + it.w } <= 869f - TableLayout.MARGIN + 0.5f)
        assertFalse(g.scrolls)
        assertTrue(g.steps.all { it.faceUp == TableLayout.UP_COMFY * g.cardH })
        assertTrue(g.steps.all { it.faceDown == TableLayout.DOWN_COMFY * g.cardH })
    }

    @Test
    fun `top row and columns never exceed the width on any screen`() {
        for ((w, h) in listOf(869f to 411f, 640f to 360f, 1280f to 800f, 1920f to 1200f)) {
            val g = TableLayout.compute(w, h, newGame)
            val right = (g.foundations + g.columns).maxOf { it.x + it.w }
            val left = (listOf(g.stock) + g.columns).minOf { it.x }
            assertTrue(right <= w - TableLayout.MARGIN + 0.5f, "$w x $h: right=$right")
            assertTrue(left >= TableLayout.MARGIN - 0.5f, "$w x $h: left=$left")
            assertTrue(g.stock.y >= TableLayout.TOP_BAR, "$w x $h: top row under the bar")
        }
    }

    @Test
    fun `tablet cards are capped and the table is centered`() {
        val g = TableLayout.compute(1920f, 1200f, newGame)
        assertEquals(TableLayout.MAX_CARD_W, g.cardW)
        val right = g.foundations.last().let { it.x + it.w }
        assertEquals(g.stock.x, 1920f - right, 0.5f)
    }

    @Test
    fun `long column on a small phone compresses then scrolls`() {
        val g = TableLayout.compute(640f, 360f, columnState(12, 13))
        assertEquals(TableLayout.DOWN_MIN, g.steps[0].faceDown)
        assertEquals(TableLayout.UP_MIN, g.steps[0].faceUp)
        assertTrue(g.scrolls)
        assertTrue(g.tableauContentHeight > 360f - g.tableauTop - TableLayout.MARGIN)
    }

    @Test
    fun `small phone fits 12 face-down and 8 face-up cards without scrolling`() {
        // Só cabe porque o passo mínimo das viradas para cima é o da spec (~18 dp, uns 5 mm).
        val g = TableLayout.compute(640f, 360f, columnState(12, 8))
        assertFalse(g.scrolls)
        assertTrue(g.steps[0].faceUp in 18f..20f, "faceUp=${g.steps[0].faceUp}")
    }

    @Test
    fun `medium column compresses face-down cards first`() {
        val g = TableLayout.compute(869f, 411f, columnState(12, 5))
        assertEquals(TableLayout.UP_COMFY * g.cardH, g.steps[0].faceUp)
        assertTrue(g.steps[0].faceDown < TableLayout.DOWN_COMFY * g.cardH)
        assertTrue(g.steps[0].faceDown >= TableLayout.DOWN_MIN)
        assertFalse(g.scrolls)
    }

    @Test
    fun `cardTop stacks face-down then face-up steps`() {
        val s = columnState(2, 3)
        val g = TableLayout.compute(869f, 411f, s)
        val top = g.columns[0].y
        assertEquals(top, TableLayout.cardTop(g, 0, 0, s))
        assertEquals(top + 2 * g.steps[0].faceDown, TableLayout.cardTop(g, 0, 2, s), 0.001f)
        assertEquals(top + 2 * g.steps[0].faceDown + 2 * g.steps[0].faceUp, TableLayout.cardTop(g, 0, 4, s), 0.001f)
    }

    @Test
    fun `hitTest finds stock, waste, a foundation and a column`() {
        val g = TableLayout.compute(869f, 411f, newGame)
        fun center(r: Rect) = (r.x + r.w / 2) to (r.y + r.h / 2)
        center(g.stock).let { (x, y) -> assertEquals(PileRef.Stock, TableLayout.hitTest(g, newGame, x, y)) }
        center(g.waste).let { (x, y) -> assertEquals(PileRef.Waste, TableLayout.hitTest(g, newGame, x, y)) }
        center(g.foundations[7]).let { (x, y) -> assertEquals(PileRef.Foundation(7), TableLayout.hitTest(g, newGame, x, y)) }
        val lastCardTop = TableLayout.cardTop(g, 12, 12, newGame)
        assertEquals(PileRef.Tableau(12), TableLayout.hitTest(g, newGame, g.columns[12].x + 5f, lastCardTop + 5f))
    }

    @Test
    fun `hitTest outside every pile is null`() {
        val g = TableLayout.compute(869f, 411f, newGame)
        assertNull(TableLayout.hitTest(g, newGame, 2f, 2f))
        assertNull(TableLayout.hitTest(g, newGame, -10f, 200f))
        val gapX = g.columns[0].x + g.cardW + 0.5f * (g.columns[1].x - g.columns[0].x - g.cardW)
        assertNull(TableLayout.hitTest(g, newGame, gapX, g.columns[0].y + 5f))
    }

    @Test
    fun `hitTest below a short column misses it`() {
        val g = TableLayout.compute(869f, 411f, newGame)
        val belowCol0 = g.columns[0].y + g.cardH + 20f
        assertNull(TableLayout.hitTest(g, newGame, g.columns[0].x + 5f, belowCol0))
    }

    @Test
    fun `hitTest accounts for tableau scroll`() {
        val s = columnState(12, 13)
        val g = TableLayout.compute(640f, 360f, s)
        val lastTop = TableLayout.cardTop(g, 0, 24, s)
        val scrollY = lastTop - g.tableauTop
        assertEquals(PileRef.Tableau(0), TableLayout.hitTest(g, s, g.columns[0].x + 5f, g.tableauTop + 5f, scrollY))
    }
}
