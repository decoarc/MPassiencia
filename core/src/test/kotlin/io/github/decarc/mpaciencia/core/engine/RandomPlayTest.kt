package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.assist.Hint
import io.github.decarc.mpaciencia.core.assist.Hints
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.random.Pcg32
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomPlayTest {
    @Test
    fun `random legal play never breaks invariants`() {
        for (n in listOf(1, 2, 3, 220, 99999)) for (mode in DrawMode.entries) {
            var g = Game.new(n, mode)
            val rng = Pcg32(n.toULong(), 7u)
            repeat(1500) {
                val options = Hints.all(g.state).filterIsInstance<Hint.Play>().map { it.move } +
                    listOfNotNull(
                        Move.Draw.takeIf { Rules.isLegal(g.state, it) },
                        Move.Recycle.takeIf { Rules.isLegal(g.state, it) },
                    )
                if (options.isEmpty()) return@repeat
                g = g.play(options[rng.nextInt(options.size)])!!
                if (rng.nextInt(10) == 0) g = g.undo() ?: g
                assertInvariants(g.state, "jogo $n/$mode, jogada ${g.moves.size}")
            }
        }
    }
}

fun assertInvariants(s: GameState, where: String) {
    val all: List<Card> = s.stock + s.waste + s.foundations.flatten() + s.tableau.flatten().map { it.card }
    assertEquals(156, all.size, "$where: total de cartas")
    assertEquals(156, all.toSet().size, "$where: cartas duplicadas")
    s.foundations.forEach { pile ->
        pile.forEachIndexed { i, c ->
            assertEquals(i + 1, c.rank, "$where: fundação fora de ordem")
            assertEquals(pile.first().suit, c.suit, "$where: fundação com naipes misturados")
        }
    }
    s.tableau.forEach { column ->
        if (column.isNotEmpty()) assertTrue(column.last().faceUp, "$where: topo de coluna virado para baixo")
        val firstUp = column.indexOfFirst { it.faceUp }.let { if (it < 0) column.size else it }
        assertTrue(column.drop(firstUp).all { it.faceUp }, "$where: carta virada para baixo sobre carta virada para cima")
        assertTrue(Rules.isRun(column.drop(firstUp).map { it.card }), "$where: sequência inválida na coluna")
    }
    assertTrue(s.score >= 0, "$where: pontuação negativa")
}
