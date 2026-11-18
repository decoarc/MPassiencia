package io.github.decarc.mpaciencia.ui.game

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.decarc.mpaciencia.core.engine.GameCodec
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class GameScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(vm: GameViewModel) {
        rule.setContent { MPacienciaTheme { GameScreen(vm, onOpenMenu = {}, onNewGame = {}) } }
        rule.waitForIdle()
    }

    private fun newGameVm(): GameViewModel =
        GameViewModel(FakeGameStore(), FakeSettingsStore(), FakeClock()).also { it.newGame(1, DrawMode.ONE) }

    private fun nearWinVm(): GameViewModel {
        val json = javaClass.classLoader!!.getResource("near_win.json")!!.readText()
        val store = FakeGameStore(SavedGame(GameCodec.decode(json)!!, 0))
        return GameViewModel(store, FakeSettingsStore(), FakeClock()).also { it.continueSaved() }
    }

    @Test
    fun `top bar shows number, points and moves`() {
        show(newGameVm())
        rule.onNodeWithText("#1 · Pontos 0 · Mov. 0", substring = true).assertExists()
    }

    @Test
    fun `tapping the stock draws a card`() {
        show(newGameVm())
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Mov. 1", substring = true).assertExists()
    }

    @Test
    fun `tapping the ace on column 5 sends it to a foundation`() {
        show(newGameVm())
        rule.onNodeWithTag("card-1DIAMONDS2").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pontos 15", substring = true).assertExists()
    }

    @Test
    fun `undo button reverts the last move`() {
        show(newGameVm())
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Mov. 0", substring = true).assertExists()
    }

    @Test
    fun `hint button outlines the source card`() {
        show(newGameVm())
        rule.onNodeWithTag("hint").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithTag("hint-source").assertCountEquals(1)
    }

    @Test
    fun `finish button appears only when the game can be finished`() {
        show(newGameVm())
        rule.onAllNodesWithTag("finish").assertCountEquals(0)
    }

    @Test
    fun `finish button shows on a nearly won game`() {
        show(nearWinVm())
        rule.onNodeWithTag("finish").assertExists()
    }
}
