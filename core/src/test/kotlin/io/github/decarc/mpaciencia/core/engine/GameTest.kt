package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.allLegalMoves
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.core.random.Pcg32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GameTest {
    // No jogo 1, a coluna 5 tem 5 cartas viradas para baixo e um Ás de ouros no topo.
    private val aceToFoundation = Move.Transfer(PileRef.Tableau(5), PileRef.Foundation(0))

    @Test
    fun `new game starts from the deal with no moves`() {
        val g = Game.new(1, DrawMode.ONE)
        assertEquals(Deal.newGame(1, DrawMode.ONE), g.state)
        assertTrue(g.moves.isEmpty())
        assertFalse(g.canUndo)
    }

    @Test
    fun `undo returns exactly the previous state including score and moves`() {
        val g0 = Game.new(1, DrawMode.ONE)
        val g1 = g0.play(aceToFoundation)!!
        assertEquals(15, g1.state.score)
        val back = g1.undo()!!
        assertEquals(g0.state, back.state)
        assertTrue(back.moves.isEmpty())
    }

    @Test
    fun `undo restores an automatically flipped card`() {
        val g1 = Game.new(1, DrawMode.ONE).play(aceToFoundation)!!
        assertTrue(g1.state.tableau[5].last().faceUp)
        assertFalse(g1.undo()!!.state.tableau[5][4].faceUp)
    }

    @Test
    fun `undo after recycle restores stock and waste`() {
        var g = Game.new(4, DrawMode.ONE)
        repeat(65) { g = g.play(Move.Draw)!! }
        val beforeRecycle = g.state
        g = g.play(Move.Recycle)!!
        assertEquals(beforeRecycle, g.undo()!!.state)
    }

    @Test
    fun `undo on a fresh game returns null`() {
        assertNull(Game.new(1, DrawMode.THREE).undo())
    }

    @Test
    fun `undo walks back through every recorded state, score included`() {
        for (n in listOf(1, 2, 77, 4321)) for (mode in DrawMode.entries) {
            val rng = Pcg32(n.toULong(), 11u)
            var g = Game.new(n, mode)
            val history = mutableListOf<GameState>(g.state)
            repeat(300) {
                val options = allLegalMoves(g.state)
                if (options.isEmpty()) return@repeat
                g = g.play(options[rng.nextInt(options.size)])!!
                history += g.state
            }
            while (g.canUndo) {
                history.removeLast()
                g = g.undo()!!
                assertEquals(history.last(), g.state, "jogo $n/$mode após desfazer até ${g.moves.size} jogadas")
            }
        }
    }

    @Test
    fun `illegal play returns null and keeps the game unchanged`() {
        val g = Game.new(1, DrawMode.ONE)
        assertNull(g.play(Move.Recycle))
        assertNull(g.play(Move.Transfer(PileRef.Tableau(0), PileRef.Tableau(0))))
        assertTrue(g.moves.isEmpty())
    }

    @Test
    fun `restart keeps number and mode with no moves`() {
        val g = Game.new(321, DrawMode.THREE).play(Move.Draw)!!.play(Move.Draw)!!
        val r = g.restart()
        assertEquals(321, r.gameNumber)
        assertEquals(DrawMode.THREE, r.drawMode)
        assertTrue(r.moves.isEmpty())
        assertEquals(Deal.newGame(321, DrawMode.THREE), r.state)
    }

    @Test
    fun `snapshot round trips through fromSnapshot`() {
        val g = Game.new(9, DrawMode.ONE).play(Move.Draw)!!.play(Move.Draw)!!
        val back = Game.fromSnapshot(g.snapshot())!!
        assertEquals(g.state, back.state)
        assertEquals(g.moves, back.moves)
    }

    @Test
    fun `play keeps the same instance semantics - original is immutable`() {
        val g = Game.new(1, DrawMode.ONE)
        val before = g.state
        g.play(Move.Draw)
        assertSame(before, g.state)
    }
}
