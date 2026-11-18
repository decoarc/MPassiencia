package io.github.decarc.mpaciencia

import androidx.navigation3.runtime.NavKey
import io.github.decarc.mpaciencia.core.engine.GameCodec
import io.github.decarc.mpaciencia.core.engine.Game
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.data.FileGameStore
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.ui.game.FakeClock
import io.github.decarc.mpaciencia.ui.game.FakeGameStore
import io.github.decarc.mpaciencia.ui.game.FakeSettingsStore
import io.github.decarc.mpaciencia.ui.game.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Regressões da revisão final do Plano 2. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewFixesTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `continueSaved reports a missing save`() = runTest(dispatcher) {
        val vm = GameViewModel(FakeGameStore(), FakeSettingsStore(), FakeClock())
        var missing = 0
        vm.continueSaved(onMissing = { missing++ })
        advanceUntilIdle()
        assertEquals(1, missing)
        assertNull(vm.state.value.game)
    }

    @Test
    fun `undo and hint do nothing after the win`() = runTest(dispatcher) {
        val json = javaClass.classLoader!!.getResource("near_win.json")!!.readText()
        val vm = GameViewModel(FakeGameStore(SavedGame(GameCodec.decode(json)!!, 0)), FakeSettingsStore(), FakeClock())
        vm.continueSaved()
        advanceUntilIdle()
        vm.finish()
        advanceUntilIdle()
        val won = vm.state.value.game!!
        vm.undo()
        vm.hint()
        assertEquals(won.moves.size, vm.state.value.game!!.moves.size)
        assertTrue(vm.state.value.won)
        assertNull(vm.state.value.hint)
    }

    @Test
    fun `timerRunning reflects resume, pause and the first move`() = runTest(dispatcher) {
        val vm = GameViewModel(FakeGameStore(), FakeSettingsStore(), FakeClock())
        vm.newGame(1, DrawMode.ONE)
        vm.resume()
        assertTrue(!vm.timerRunning)
        vm.tapStock()
        assertTrue(vm.timerRunning)
        vm.pause()
        assertTrue(!vm.timerRunning)
    }

    @Test
    fun `popping twice never empties the back stack`() {
        val stack = mutableListOf<NavKey>(MenuRoute, RulesRoute)
        popIfTop(stack, RulesRoute)
        popIfTop(stack, RulesRoute)
        assertEquals(listOf<NavKey>(MenuRoute), stack)
    }

    @Test
    fun `a failing disk does not crash the save`() = runTest(dispatcher) {
        // "dir" é um arquivo comum: não dá para criar o save.json dentro dele.
        val notADir = File(Files.createTempDirectory("x").toFile(), "file").apply { writeText("x") }
        FileGameStore(notADir).save(SavedGame(Game.new(1, DrawMode.ONE), 0))
    }
}
