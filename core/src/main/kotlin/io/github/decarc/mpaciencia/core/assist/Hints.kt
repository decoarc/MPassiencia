package io.github.decarc.mpaciencia.core.assist

import io.github.decarc.mpaciencia.core.engine.Engine
import io.github.decarc.mpaciencia.core.engine.Rules
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef

sealed interface Hint {
    data class Play(val move: Move.Transfer) : Hint
    data object DrawStock : Hint
    data object Recycle : Hint
    data object Stuck : Hint
}

object Hints {
    /**
     * Dicas em ordem de prioridade. A interface mostra `all[i % size]` a cada toque em Dica.
     * Vazia quando o jogo já foi vencido.
     */
    fun all(state: GameState, lastMove: Move? = null): List<Hint> {
        if (state.isWon) return emptyList()
        val plays = usefulPlays(state).filterNot { reverses(it, lastMove) }
        if (plays.isNotEmpty()) return plays.map(Hint::Play)
        return when {
            isStuck(state) -> listOf(Hint.Stuck)
            state.stock.isNotEmpty() -> listOf(Hint.DrawStock)
            else -> listOf(Hint.Recycle)
        }
    }

    /** A jogada [candidate] só desfaz [last] (devolve as mesmas cartas para onde estavam). */
    private fun reverses(candidate: Move.Transfer, last: Move?): Boolean {
        if (last !is Move.Transfer) return false
        if (candidate.from != last.to || candidate.count != last.count) return false
        return when (last.from) {
            is PileRef.Foundation -> candidate.to is PileRef.Foundation
            else -> candidate.to == last.from
        }
    }

    /**
     * Travado: nenhuma jogada útil na mesa, e nenhuma carta que chega ao topo do descarte numa
     * passada completa do monte tem para onde ir — nem no estado atual, nem depois de uma jogada
     * que abre espaço (esvaziar uma coluna ou tirar uma carta da fundação).
     */
    fun isStuck(state: GameState): Boolean {
        if (state.isWon || usefulPlays(state).isNotEmpty()) return false
        if (passCanPlay(state)) return false
        return spaceOpeningMoves(state).none { passCanPlay(Engine.apply(state, it)!!) }
    }

    /** Alguma carta que chega ao topo do descarte numa passada completa do monte pode ser jogada. */
    private fun passCanPlay(start: GameState): Boolean {
        var s = start
        val seen = mutableSetOf<Int>()
        while (seen.add(s.stock.size)) {
            if (wasteTopPlayable(s)) return true
            s = when {
                Rules.isLegal(s, Move.Draw) -> Engine.apply(s, Move.Draw)!!
                Rules.isLegal(s, Move.Recycle) -> Engine.apply(s, Move.Recycle)!!
                else -> return false
            }
        }
        return false
    }

    /** Jogadas legais que abrem espaço: coluna inteira para outra coluna, ou fundação para coluna. */
    private fun spaceOpeningMoves(s: GameState): List<Move.Transfer> {
        val emptying = s.tableau.indices.filter { s.tableau[it].isNotEmpty() }.flatMap { i ->
            s.tableau.indices.filter { s.tableau[it].isNotEmpty() }
                .map { Move.Transfer(PileRef.Tableau(i), PileRef.Tableau(it), s.tableau[i].size) }
        }
        val fromFoundation = s.foundations.indices.flatMap { f ->
            s.tableau.indices.map { Move.Transfer(PileRef.Foundation(f), PileRef.Tableau(it)) }
        }
        return (emptying + fromFoundation).filter { Rules.isLegal(s, it) }
    }

    private fun wasteTopPlayable(s: GameState): Boolean =
        s.waste.isNotEmpty() && targets(s).any { Rules.isLegal(s, Move.Transfer(PileRef.Waste, it)) }

    private fun targets(s: GameState): List<PileRef> =
        s.foundations.indices.map { PileRef.Foundation(it) } + s.tableau.indices.map { PileRef.Tableau(it) }

    internal fun usefulPlays(state: GameState): List<Move.Transfer> {
        val base = basePlays(state)
        return (base + foundationToTableau(state, base)).distinct()
    }

    /** Jogadas úteis sem contar a volta da fundação para a coluna. */
    private fun basePlays(s: GameState): List<Move.Transfer> = buildList {
        // 1. para a fundação
        TapToMove.foundationTarget(s, PileRef.Waste)?.let(::add)
        s.tableau.indices.forEach { i -> TapToMove.foundationTarget(s, PileRef.Tableau(i))?.let(::add) }
        // 2. entre colunas
        addAll(tableauPlays(s))
        // 3. do descarte para uma coluna
        firstColumnTarget(s, PileRef.Waste, 1)?.let(::add)
    }

    private fun tableauPlays(s: GameState): List<Move.Transfer> {
        val kingWaiting = kingWaiting(s)
        return s.tableau.indices.flatMap { i ->
            val column = s.tableau[i]
            column.indices.filter { column[it].faceUp }.mapNotNull { k ->
                val count = column.size - k
                val below = column.getOrNull(k - 1)
                val useful = when {
                    below == null -> kingWaiting
                    !below.faceUp -> true
                    else -> canGoToFoundation(s, below.card)
                }
                if (!useful) return@mapNotNull null
                firstColumnTarget(s, PileRef.Tableau(i), count, allowEmpty = below != null)
            }
        }
    }

    /** Um Rei visível que ganharia com uma coluna vazia: no topo do descarte ou com cartas embaixo. */
    private fun kingWaiting(s: GameState): Boolean =
        s.waste.lastOrNull()?.rank == 13 ||
            s.tableau.any { col -> col.withIndex().any { (k, c) -> k > 0 && c.faceUp && c.card.rank == 13 } }

    private fun canGoToFoundation(s: GameState, card: Card): Boolean =
        s.foundations.any { pile ->
            val top = pile.lastOrNull()
            if (top == null) card.rank == 1 else top.suit == card.suit && card.rank == top.rank + 1
        }

    private fun firstColumnTarget(s: GameState, from: PileRef, count: Int, allowEmpty: Boolean = true): Move.Transfer? =
        s.tableau.indices
            .map { Move.Transfer(from, PileRef.Tableau(it), count) }
            .filter { Rules.isLegal(s, it) }
            .filter { allowEmpty || s.tableau[(it.to as PileRef.Tableau).index].isNotEmpty() }
            .sortedBy { if (s.tableau[(it.to as PileRef.Tableau).index].isEmpty()) 1 else 0 }
            .firstOrNull()

    /** Da fundação para uma coluna, só se isso abrir uma jogada útil nova para aquela coluna. */
    private fun foundationToTableau(s: GameState, before: List<Move.Transfer>): List<Move.Transfer> =
        s.foundations.indices.flatMap { f ->
            s.tableau.indices.mapNotNull { t ->
                val move = Move.Transfer(PileRef.Foundation(f), PileRef.Tableau(t))
                val next = Engine.apply(s, move) ?: return@mapNotNull null
                val opened = basePlays(next).any { it.to == PileRef.Tableau(t) && it !in before }
                move.takeIf { opened }
            }
        }
}
