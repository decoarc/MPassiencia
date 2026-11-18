package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef

object Rules {
    /** Depois da vitória nada mais se move. */
    fun isLegal(state: GameState, move: Move): Boolean = !state.isWon && when (move) {
        Move.Draw -> state.stock.isNotEmpty()
        Move.Recycle -> state.stock.isEmpty() && state.waste.isNotEmpty()
        is Move.Transfer -> movingCards(state, move) != null
    }

    /** As cartas que a jogada leva (de baixo para o topo), ou `null` se a jogada for ilegal. */
    internal fun movingCards(state: GameState, move: Move.Transfer): List<Card>? {
        if (move.count < 1 || move.from == move.to) return null
        val cards = sourceCards(state, move.from, move.count) ?: return null
        val accepted = when (val to = move.to) {
            is PileRef.Foundation -> move.from !is PileRef.Foundation && foundationAccepts(state, to.index, cards)
            is PileRef.Tableau -> tableauAccepts(state, to.index, cards)
            PileRef.Stock, PileRef.Waste -> false
        }
        return cards.takeIf { accepted }
    }

    private fun sourceCards(state: GameState, from: PileRef, count: Int): List<Card>? = when (from) {
        PileRef.Stock -> null
        PileRef.Waste -> state.waste.lastOrNull()?.takeIf { count == 1 }?.let(::listOf)
        is PileRef.Foundation ->
            state.foundations.getOrNull(from.index)?.lastOrNull()?.takeIf { count == 1 }?.let(::listOf)
        is PileRef.Tableau -> {
            val column = state.tableau.getOrNull(from.index)
            val tail = column?.takeIf { it.size >= count }?.takeLast(count)
            tail?.takeIf { t -> t.all { it.faceUp } }?.map { it.card }?.takeIf(::isRun)
        }
    }

    private fun foundationAccepts(state: GameState, index: Int, cards: List<Card>): Boolean {
        val pile = state.foundations.getOrNull(index) ?: return false
        val card = cards.singleOrNull() ?: return false
        val top = pile.lastOrNull() ?: return card.rank == 1
        return top.suit == card.suit && card.rank == top.rank + 1
    }

    private fun tableauAccepts(state: GameState, index: Int, cards: List<Card>): Boolean {
        val column = state.tableau.getOrNull(index) ?: return false
        val first = cards.first()
        val top = column.lastOrNull() ?: return first.rank == 13
        return top.faceUp && top.card.color != first.color && top.card.rank == first.rank + 1
    }

    /** Sequência decrescente com cores alternadas. */
    internal fun isRun(cards: List<Card>): Boolean =
        cards.zipWithNext().all { (a, b) -> a.color != b.color && a.rank == b.rank + 1 }
}
