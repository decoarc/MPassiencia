package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.core.assist.AutoComplete
import io.github.decarc.mpaciencia.core.assist.Hint
import io.github.decarc.mpaciencia.core.assist.Hints
import io.github.decarc.mpaciencia.core.engine.GameCodec
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Move
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.data.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelAssistTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    /** Jogo 1 jogado até só faltar levar as cartas para as fundações. */
    private fun nearWin(): SavedGame {
        val json = javaClass.classLoader!!.getResource("near_win.json")!!.readText()
        val game = GameCodec.decode(json)!!
        check(AutoComplete.canFinish(game.state))
        return SavedGame(game, 60_000)
    }

    private fun TestScope.vm(
        store: FakeGameStore = FakeGameStore(),
        hints: (io.github.decarc.mpaciencia.core.model.GameState, Move?) -> List<Hint> = Hints::all,
    ) = GameViewModel(store, FakeSettingsStore(), clock, hints).also { advanceUntilIdle() }

    @Test
    fun `hint cycles through hints and resets after a move`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        val all = Hints.all(vm.state.value.game!!.state)
        vm.hint()
        assertEquals(all[0], vm.state.value.hint)
        vm.hint()
        assertEquals(all[1 % all.size], vm.state.value.hint)
        vm.tapStock()
        assertNull(vm.state.value.hint)
        vm.hint()
        assertEquals(Hints.all(vm.state.value.game!!.state, Move.Draw)[0], vm.state.value.hint)
    }

    @Test
    fun `hint on a stuck game sets stuck`() = runTest(dispatcher) {
        val vm = vm(hints = { _, _ -> listOf(Hint.Stuck) })
        vm.newGame(1, DrawMode.ONE)
        vm.hint()
        assertTrue(vm.state.value.stuck)
        vm.dismissStuck()
        assertFalse(vm.state.value.stuck)
    }

    @Test
    fun `hint never suggests reversing the last move`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.tapCard(PileRef.Tableau(5), 1) // Ás para a fundação
        val last = vm.state.value.game!!.moves.last() as Move.Transfer
        repeat(10) {
            vm.hint()
            val h = vm.state.value.hint
            if (h is Hint.Play) assertNotEquals(Move.Transfer(last.to, last.from, last.count), h.move)
        }
    }

    @Test
    fun `finish plays all remaining cards and wins`() = runTest(dispatcher) {
        val store = FakeGameStore(nearWin())
        val vm = vm(store)
        vm.continueSaved()
        advanceUntilIdle()
        assertTrue(vm.state.value.canFinish)
        vm.finish()
        advanceUntilIdle()
        assertTrue(vm.state.value.game!!.state.isWon)
        assertTrue(vm.state.value.won)
        assertFalse(vm.state.value.finishing)
    }

    @Test
    fun `win clears the save and hides Continuar`() = runTest(dispatcher) {
        val store = FakeGameStore(nearWin())
        val vm = vm(store)
        vm.continueSaved()
        advanceUntilIdle()
        vm.finish()
        advanceUntilIdle()
        assertNull(store.saved)
        assertFalse(vm.state.value.hasSavedGame)
    }

    @Test
    fun `win emits the Win sound`() = runTest(dispatcher) {
        val vm = vm(FakeGameStore(nearWin()))
        val events = mutableListOf<SoundEvent>()
        val job = launch { vm.sounds.toList(events) }
        vm.continueSaved()
        advanceUntilIdle()
        vm.finish()
        advanceUntilIdle()
        assertEquals(SoundEvent.Win, events.last())
        job.cancel()
    }

    @Test
    fun `timer stops at the win and does not run in background`() = runTest(dispatcher) {
        val vm = vm(FakeGameStore(nearWin()))
        vm.continueSaved()
        advanceUntilIdle()
        vm.resume()
        clock.advance(2_000)
        vm.finish()
        advanceUntilIdle()
        val atWin = vm.elapsedNow()
        assertEquals(62_000, atWin)
        clock.advance(30_000)
        vm.pause()
        vm.resume()
        clock.advance(30_000)
        assertEquals(atWin, vm.elapsedNow())
    }

    @Test
    fun `no moves are accepted after the win`() = runTest(dispatcher) {
        val vm = vm(FakeGameStore(nearWin()))
        vm.continueSaved()
        advanceUntilIdle()
        vm.finish()
        advanceUntilIdle()
        assertFalse(vm.tapStock())
        assertFalse(vm.tapCard(PileRef.Foundation(0), 1))
    }
}
