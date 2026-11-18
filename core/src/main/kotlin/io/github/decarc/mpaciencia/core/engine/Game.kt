package io.github.decarc.mpaciencia.core.engine

import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import kotlinx.serialization.Serializable

/** O que é salvo: só o número, o modo e as jogadas. O estado é reconstruído repetindo as jogadas. */
@Serializable
data class GameSnapshot(
    val version: Int = 1,
    val gameNumber: Int,
    val drawMode: DrawMode,
    val moves: List<Move>,
)

/** Uma partida imutável. Desfazer repete todas as jogadas menos a última, a partir da distribuição. */
class Game private constructor(
    val gameNumber: Int,
    val drawMode: DrawMode,
    val moves: List<Move>,
    val state: GameState,
) {
    val canUndo: Boolean get() = moves.isNotEmpty()

    fun play(move: Move): Game? {
        val next = Engine.apply(state, move) ?: return null
        return Game(gameNumber, drawMode, moves + move, next)
    }

    fun undo(): Game? {
        if (!canUndo) return null
        return replay(gameNumber, drawMode, moves.dropLast(1))
    }

    fun restart(): Game = new(gameNumber, drawMode)

    fun snapshot(): GameSnapshot = GameSnapshot(gameNumber = gameNumber, drawMode = drawMode, moves = moves)

    companion object {
        fun new(gameNumber: Int, drawMode: DrawMode): Game =
            Game(gameNumber, drawMode, emptyList(), Deal.newGame(gameNumber, drawMode))

        /** Reconstrói a partida; `null` se o número for inválido ou alguma jogada for ilegal. */
        fun fromSnapshot(snapshot: GameSnapshot): Game? {
            if (snapshot.gameNumber !in Deal.MIN_GAME..Deal.MAX_GAME) return null
            return replay(snapshot.gameNumber, snapshot.drawMode, snapshot.moves)
        }

        private fun replay(gameNumber: Int, drawMode: DrawMode, moves: List<Move>): Game? {
            var state = Deal.newGame(gameNumber, drawMode)
            for (move in moves) state = Engine.apply(state, move) ?: return null
            return Game(gameNumber, drawMode, moves, state)
        }
    }
}
