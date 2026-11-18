package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef

/** Botão "Terminar": leva tudo para as fundações quando não há mais nada escondido. */
object AutoComplete {
    fun canFinish(state: GameState): Boolean =
        !state.isWon &&
            state.stock.isEmpty() &&
            state.waste.isEmpty() &&
            state.tableau.all { column -> column.all { it.faceUp } }

    /** A carta de menor valor entre os topos das colunas que pode ir para uma fundação. */
    fun nextMove(state: GameState): Move.Transfer? =
        state.tableau.indices
            .filter { state.tableau[it].isNotEmpty() }
            .sortedWith(compareBy({ state.tableau[it].last().card.rank }, { it }))
            .firstNotNullOfOrNull { TapToMove.foundationTarget(state, PileRef.Tableau(it)) }
}
