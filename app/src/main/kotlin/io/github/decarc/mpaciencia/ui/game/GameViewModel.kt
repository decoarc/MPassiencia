package io.github.decarc.mpaciencia.ui.game

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.decarc.mpaciencia.core.assist.AutoComplete
import io.github.decarc.mpaciencia.core.assist.TapToMove
import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.data.GameStore
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.data.Settings
import io.github.decarc.mpaciencia.data.SettingsStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Estado da partida em andamento. Regras, pontos e desfazer vêm sempre do `core`. */
class GameViewModel(
    private val store: GameStore,
    settingsStore: SettingsStore,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _sounds = MutableSharedFlow<SoundEvent>(extraBufferCapacity = 16)
    val sounds: SharedFlow<SoundEvent> = _sounds

    val settings: StateFlow<Settings> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val saveLock = Mutex()

    // Cronômetro: começa na primeira jogada e só corre entre resume() e pause().
    private var accumulatedMs = 0L
    private var runningSince: Long? = null
    private var started = false
    private var active = false

    init {
        viewModelScope.launch {
            val saved = store.load()
            _state.update { it.copy(hasSavedGame = saved != null) }
        }
    }

    fun elapsedNow(): Long = accumulatedMs + (runningSince?.let { clock() - it } ?: 0L)

    fun newGame(number: Int, mode: DrawMode) {
        startWith(Game.new(number, mode), elapsedMs = 0)
        save()
    }

    fun continueSaved() {
        viewModelScope.launch {
            val saved = store.load() ?: return@launch
            startWith(saved.game, saved.elapsedMs)
        }
    }

    fun restart() {
        val game = _state.value.game ?: return
        newGame(game.gameNumber, game.drawMode)
    }

    private fun startWith(game: Game, elapsedMs: Long) {
        accumulatedMs = elapsedMs
        runningSince = null
        started = elapsedMs > 0 || game.moves.isNotEmpty()
        if (started && active) runningSince = clock()
        _state.value = GameUiState(
            game = game,
            elapsedMs = elapsedMs,
            canFinish = AutoComplete.canFinish(game.state),
            won = game.state.isWon,
            hasSavedGame = true,
        )
    }

    fun tapStock(): Boolean {
        val s = _state.value.game?.state ?: return false
        val move = if (s.stock.isNotEmpty()) Move.Draw else Move.Recycle
        return play(move)
    }

    fun tapCard(from: PileRef, count: Int): Boolean {
        val s = _state.value.game?.state ?: return false
        val move = TapToMove.bestMove(s, from, count) ?: return false
        return play(move)
    }

    fun drop(from: PileRef, count: Int, to: PileRef?): Boolean {
        if (to == null) return false
        return play(Move.Transfer(from, to, count))
    }

    fun undo() {
        val game = _state.value.game ?: return
        val previous = game.undo() ?: return
        onNewGameState(previous, sound = null)
    }

    fun pause() {
        active = false
        runningSince?.let { accumulatedMs += clock() - it }
        runningSince = null
        _state.update { it.copy(elapsedMs = accumulatedMs) }
        if (_state.value.game != null && !_state.value.won) save()
    }

    fun resume() {
        active = true
        if (started && runningSince == null && !_state.value.won) runningSince = clock()
    }

    /** Aplica [move]; `false` se for ilegal (nada muda e nada é salvo). */
    internal fun play(move: Move): Boolean {
        val game = _state.value.game ?: return false
        if (_state.value.won) return false
        val next = game.play(move) ?: return false
        if (!started) {
            started = true
            if (active) runningSince = clock()
        }
        onNewGameState(next, soundFor(move, game.state, next.state))
        return true
    }

    private fun onNewGameState(next: Game, sound: SoundEvent?) {
        _state.update {
            it.copy(
                game = next,
                elapsedMs = elapsedNow(),
                hint = null,
                stuck = false,
                canFinish = AutoComplete.canFinish(next.state),
            )
        }
        sound?.let { _sounds.tryEmit(it) }
        save()
    }

    private fun soundFor(move: Move, before: GameState, after: GameState): SoundEvent = when {
        move is Move.Transfer && move.to is PileRef.Foundation -> SoundEvent.Foundation
        move == Move.Draw || move == Move.Recycle -> SoundEvent.Flip
        faceDownCount(after) < faceDownCount(before) -> SoundEvent.Flip
        else -> SoundEvent.Place
    }

    private fun faceDownCount(s: GameState) = s.tableau.sumOf { col -> col.count { !it.faceUp } }

    private fun save() {
        val game = _state.value.game ?: return
        val saved = SavedGame(game, elapsedNow())
        viewModelScope.launch { saveLock.withLock { store.save(saved) } }
    }

    companion object {
        fun factory(store: GameStore, settings: SettingsStore): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(store, settings) }
        }
    }
}
