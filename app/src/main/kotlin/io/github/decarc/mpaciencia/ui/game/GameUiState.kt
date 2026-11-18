package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.core.assist.Hint
import io.github.decarc.mpaciencia.core.engine.Game

data class GameUiState(
    val game: Game? = null,
    /** Tempo no último evento (jogada ou pausa). Para mostrar o relógio, use `GameViewModel.elapsedNow()`. */
    val elapsedMs: Long = 0,
    val hint: Hint? = null,
    val canFinish: Boolean = false,
    val finishing: Boolean = false,
    val won: Boolean = false,
    val stuck: Boolean = false,
    val hasSavedGame: Boolean = false,
)

sealed interface SoundEvent {
    data object Flip : SoundEvent
    data object Place : SoundEvent
    data object Foundation : SoundEvent
    data object Win : SoundEvent
}
