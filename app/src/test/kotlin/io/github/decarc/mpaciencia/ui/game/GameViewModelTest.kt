package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.core.assist.TapToMove
import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.PileRef
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeGameStore()
    private val clock = FakeClock()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(): GameViewModel = GameViewModel(store, FakeSettingsStore(), clock).also { advanceUntilIdle() }

    // No jogo 1, a coluna 5 tem um Ás de ouros no topo.
    private val aceColumn = PileRef.Tableau(5)

    @Test
    fun `new game starts with no time running`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.resume()
        clock.advance(10_000)
        assertEquals(0, vm.elapsedNow())
        assertEquals(1, vm.state.value.game!!.gameNumber)
    }

    @Test
    fun `tap on a card plays the best move and saves`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        advanceUntilIdle()
        val savesBefore = store.saves
        assertTrue(vm.tapCard(aceColumn, 1))
        advanceUntilIdle()
        assertEquals(15, vm.state.value.game!!.state.score)
        assertEquals(savesBefore + 1, store.saves)
        assertEquals(vm.state.value.game!!.moves, store.saved!!.game.moves)
    }

    @Test
    fun `tap on a card with no destination changes nothing and does not save`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        advanceUntilIdle()
        val game = vm.state.value.game!!
        val noDestination = (0 until 13).first { TapToMove.bestMove(game.state, PileRef.Tableau(it)) == null }
        val savesBefore = store.saves
        assertFalse(vm.tapCard(PileRef.Tableau(noDestination), 1))
        advanceUntilIdle()
        assertTrue(vm.state.value.game!!.moves.isEmpty())
        assertEquals(savesBefore, store.saves)
    }

    @Test
    fun `two quick taps on the same card play at most two legal moves`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.tapCard(aceColumn, 1)
        vm.tapCard(aceColumn, 1)
        advanceUntilIdle()
        val game = vm.state.value.game!!
        assertTrue(game.moves.size in 1..2)
        assertNotNull(Game.fromSnapshot(game.snapshot()))
    }

    @Test
    fun `illegal drop changes nothing`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        advanceUntilIdle()
        val savesBefore = store.saves
        assertFalse(vm.drop(aceColumn, 1, null))
        assertFalse(vm.drop(aceColumn, 1, PileRef.Tableau(0)))
        advanceUntilIdle()
        assertTrue(vm.state.value.game!!.moves.isEmpty())
        assertEquals(savesBefore, store.saves)
    }

    @Test
    fun `drop onto a legal pile plays the move`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        assertTrue(vm.drop(aceColumn, 1, PileRef.Foundation(4)))
        assertEquals(1, vm.state.value.game!!.state.foundations[4].size)
    }

    @Test
    fun `undo goes back and saves`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.tapCard(aceColumn, 1)
        advanceUntilIdle()
        val savesBefore = store.saves
        vm.undo()
        advanceUntilIdle()
        assertTrue(vm.state.value.game!!.moves.isEmpty())
        assertEquals(savesBefore + 1, store.saves)
    }

    @Test
    fun `tapStock draws and recycles`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(3, DrawMode.ONE)
        repeat(65) { assertTrue(vm.tapStock()) }
        assertTrue(vm.state.value.game!!.state.stock.isEmpty())
        assertTrue(vm.tapStock())
        assertEquals(65, vm.state.value.game!!.state.stock.size)
    }

    @Test
    fun `timer counts only between resume and pause, after the first move`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.resume()
        clock.advance(3_000)
        vm.tapStock()
        clock.advance(5_000)
        vm.pause()
        clock.advance(60_000)
        assertEquals(5_000, vm.elapsedNow())
        vm.resume()
        clock.advance(1_000)
        assertEquals(6_000, vm.elapsedNow())
    }

    @Test
    fun `restores game and elapsed time from the store`() = runTest(dispatcher) {
        val vm1 = vm()
        vm1.newGame(220, DrawMode.THREE)
        vm1.resume()
        vm1.tapStock()
        vm1.tapStock()
        clock.advance(4_000)
        vm1.pause()
        advanceUntilIdle()

        val vm2 = vm()
        assertTrue(vm2.state.value.hasSavedGame)
        vm2.continueSaved()
        advanceUntilIdle()
        assertEquals(vm1.state.value.game!!.state, vm2.state.value.game!!.state)
        assertEquals(4_000, vm2.elapsedNow())
    }

    @Test
    fun `foundation move emits Foundation sound and stock emits Flip`() = runTest(dispatcher) {
        val vm = vm()
        val events = mutableListOf<SoundEvent>()
        val job = launch { vm.sounds.toList(events) }
        advanceUntilIdle()
        vm.newGame(1, DrawMode.ONE)
        vm.tapCard(aceColumn, 1)
        vm.tapStock()
        advanceUntilIdle()
        assertEquals(listOf(SoundEvent.Foundation, SoundEvent.Flip), events.take(2))
        job.cancel()
    }

    @Test
    fun `column move emits Place`() = runTest(dispatcher) {
        val vm = vm()
        val events = mutableListOf<SoundEvent>()
        val job = launch { vm.sounds.toList(events) }
        advanceUntilIdle()
        // Procura um jogo cuja primeira jogada de toque seja coluna → coluna sem virar carta (coluna 0 tem 1 carta).
        val number = (1..500).first { n ->
            val s = Game.new(n, DrawMode.ONE).state
            TapToMove.bestMove(s, PileRef.Tableau(0))?.to is PileRef.Tableau
        }
        vm.newGame(number, DrawMode.ONE)
        assertTrue(vm.tapCard(PileRef.Tableau(0), 1))
        advanceUntilIdle()
        assertEquals(SoundEvent.Place, events.last())
        job.cancel()
    }

    @Test
    fun `new game over a saved one replaces it`() = runTest(dispatcher) {
        val vm = vm()
        vm.newGame(1, DrawMode.ONE)
        vm.tapStock()
        advanceUntilIdle()
        vm.newGame(2, DrawMode.ONE)
        advanceUntilIdle()
        assertEquals(2, store.saved!!.game.gameNumber)
        assertTrue(store.saved!!.game.moves.isEmpty())
    }
}
