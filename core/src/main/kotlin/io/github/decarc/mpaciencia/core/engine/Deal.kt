package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.TableauCard
import io.github.decarc.mpaciencia.core.model.fullDeck
import io.github.decarc.mpaciencia.core.random.Pcg32
import io.github.decarc.mpaciencia.core.random.shuffledWith

object Deal {
    const val MIN_GAME = 1
    const val MAX_GAME = 99999

    /** Sequência fixa do PCG32 para a distribuição ("PACN"). Mudar isto muda todos os jogos. */
    private const val DEAL_SEQUENCE = 0x5041434EuL

    fun newGame(gameNumber: Int, drawMode: DrawMode): GameState {
        require(gameNumber in MIN_GAME..MAX_GAME) { "número do jogo fora de $MIN_GAME..$MAX_GAME: $gameNumber" }
        val cards = fullDeck().shuffledWith(Pcg32(gameNumber.toULong(), DEAL_SEQUENCE))
        var next = 0
        val tableau = (0 until GameState.COLUMNS).map { col ->
            List(col + 1) { i -> TableauCard(cards[next++], faceUp = i == col) }
        }
        return GameState(
            gameNumber = gameNumber,
            drawMode = drawMode,
            stock = cards.drop(next),
            waste = emptyList(),
            foundations = List(GameState.FOUNDATIONS) { emptyList() },
            tableau = tableau,
        )
    }
}
