package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.engine.Rules
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef

/** Escolhe o destino de uma carta (ou grupo) tocada. */
object TapToMove {
    /**
     * Melhor jogada para as [count] cartas do topo de [from], ou `null` se não houver destino útil.
     * Prioridade: fundação (uma carta só) → coluna com cartas → coluna vazia (grupo que começa com Rei).
     */
    fun bestMove(state: GameState, from: PileRef, count: Int = 1): Move.Transfer? {
        if (count == 1 && from !is PileRef.Foundation) {
            foundationTarget(state, from)?.let { return it }
        }
        val legalColumns = state.tableau.indices
            .map { Move.Transfer(from, PileRef.Tableau(it), count) }
            .filter { Rules.isLegal(state, it) }
        legalColumns.firstOrNull { state.tableau[(it.to as PileRef.Tableau).index].isNotEmpty() }
            ?.let { return it }
        return legalColumns.firstOrNull().takeUnless { movesWholeColumn(state, from, count) }
    }

    /** Fundação que já tem o naipe primeiro; para um Ás, a primeira vazia da esquerda. */
    internal fun foundationTarget(state: GameState, from: PileRef): Move.Transfer? {
        val legal = state.foundations.indices
            .map { Move.Transfer(from, PileRef.Foundation(it), 1) }
            .filter { Rules.isLegal(state, it) }
        return legal.firstOrNull { state.foundations[(it.to as PileRef.Foundation).index].isNotEmpty() }
            ?: legal.firstOrNull()
    }

    /** Um grupo que já ocupa a coluna inteira não ganha nada indo para outra coluna vazia. */
    private fun movesWholeColumn(state: GameState, from: PileRef, count: Int): Boolean =
        from is PileRef.Tableau && state.tableau.getOrNull(from.index)?.size == count
}
