package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.core.model.TableauCard

object Scoring {
    const val TO_FOUNDATION = 10
    const val FLIP = 5
    const val WASTE_TO_TABLEAU = 5
    const val FOUNDATION_TO_TABLEAU = -15
}

object Engine {
    /** Aplica [move] e devolve o novo estado, ou `null` se a jogada for ilegal. */
    fun apply(state: GameState, move: Move): GameState? = when (move) {
        Move.Draw -> draw(state)
        Move.Recycle -> recycle(state)
        is Move.Transfer -> transfer(state, move)
    }

    /** Tira as cartas uma por vez do topo do monte para o topo do descarte. */
    private fun draw(state: GameState): GameState? {
        if (!Rules.isLegal(state, Move.Draw)) return null
        val n = minOf(state.drawMode.count, state.stock.size)
        val drawn = state.stock.takeLast(n).reversed()
        return state.copy(stock = state.stock.dropLast(n), waste = state.waste + drawn, moves = state.moves + 1)
    }

    /** Devolve o descarte ao monte de modo que a próxima passada saia na mesma ordem. */
    private fun recycle(state: GameState): GameState? {
        if (!Rules.isLegal(state, Move.Recycle)) return null
        return state.copy(stock = state.waste.reversed(), waste = emptyList(), moves = state.moves + 1)
    }

    private fun transfer(state: GameState, move: Move.Transfer): GameState? {
        val cards = Rules.movingCards(state, move) ?: return null
        var delta = 0
        var waste = state.waste
        val foundations = state.foundations.toMutableList()
        val tableau = state.tableau.toMutableList()

        when (val from = move.from) {
            PileRef.Waste -> waste = waste.dropLast(1)
            is PileRef.Foundation -> foundations[from.index] = foundations[from.index].dropLast(1)
            is PileRef.Tableau -> {
                val remaining = tableau[from.index].dropLast(cards.size)
                val top = remaining.lastOrNull()
                tableau[from.index] = if (top != null && !top.faceUp) {
                    delta += Scoring.FLIP
                    remaining.dropLast(1) + top.copy(faceUp = true)
                } else {
                    remaining
                }
            }
            PileRef.Stock -> return null
        }

        when (val to = move.to) {
            is PileRef.Foundation -> {
                foundations[to.index] = foundations[to.index] + cards
                delta += Scoring.TO_FOUNDATION
            }
            is PileRef.Tableau -> {
                tableau[to.index] = tableau[to.index] + cards.map { TableauCard(it, faceUp = true) }
                delta += when (move.from) {
                    PileRef.Waste -> Scoring.WASTE_TO_TABLEAU
                    is PileRef.Foundation -> Scoring.FOUNDATION_TO_TABLEAU
                    else -> 0
                }
            }
            PileRef.Stock, PileRef.Waste -> return null
        }

        return state.copy(
            waste = waste,
            foundations = foundations,
            tableau = tableau,
            score = scored(state.score, delta),
            moves = state.moves + 1,
        )
    }

    private fun scored(score: Int, delta: Int) = maxOf(0, score + delta)
}
