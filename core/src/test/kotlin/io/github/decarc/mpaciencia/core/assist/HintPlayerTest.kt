package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import kotlin.test.Test
import kotlin.test.assertTrue

/** Quem segue sempre a primeira dica não pode ficar girando em círculo. */
class HintPlayerTest {
    @Test
    fun `following the first hint never revisits a table position`() {
        for (n in 1..10) {
            var g = Game.new(n, DrawMode.ONE)
            val seen = mutableSetOf<Any>()
            for (step in 0 until 3000) {
                if (g.state.isWon || AutoComplete.canFinish(g.state)) break
                val move: Move = when (val h = Hints.all(g.state, g.moves.lastOrNull()).firstOrNull()) {
                    is Hint.Play -> h.move
                    Hint.DrawStock -> Move.Draw
                    Hint.Recycle -> Move.Recycle
                    Hint.Stuck, null -> break
                }
                g = g.play(move)!!
                val position = listOf(g.state.tableau, g.state.foundations, g.state.stock, g.state.waste)
                assertTrue(seen.add(position), "jogo $n: a dica voltou a uma posição já vista na jogada $step (${g.moves.takeLast(4)})")
            }
        }
    }
}
