package io.github.decarc.mpaciencia.ui.menu

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.decarc.mpaciencia.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseGameNumberTest {
    @Test
    fun `parseGameNumber accepts 1 to 99999 only`() {
        assertEquals(1, parseGameNumber("1"))
        assertEquals(99999, parseGameNumber("99999"))
        assertEquals(220, parseGameNumber(" 220 "))
        listOf("0", "100000", "abc", "", "-5", "12.5").forEach { assertNull(parseGameNumber(it), it) }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class MenuFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun startNewGame() {
        rule.onNodeWithTag("menu-new").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("dialog-play").performClick()
        rule.waitForIdle()
    }

    @Test
    fun `menu hides Continuar without a saved game`() {
        rule.onAllNodesWithTag("menu-continue").assertCountEquals(0)
        rule.onNodeWithTag("menu-new").assertExists()
    }

    @Test
    fun `new game from the menu opens the table`() {
        startNewGame()
        rule.onNodeWithTag("stock").assertExists()
    }

    @Test
    fun `back to menu then Continuar restores the same game`() {
        startNewGame()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("menu").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("ingame-exit").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("menu-continue").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Mov. 1", substring = true).assertExists()
    }

    private fun vm() = androidx.lifecycle.ViewModelProvider(rule.activity)[io.github.decarc.mpaciencia.ui.game.GameViewModel::class.java]

    @Test
    fun `clock runs after New Game from the in-game menu`() {
        startNewGame()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("menu").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("ingame-new").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("dialog-play").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("discard-confirm").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        kotlin.test.assertTrue(vm().timerRunning, "o relógio deveria estar correndo")
    }

    @Test
    fun `system back from the game stops the clock`() {
        startNewGame()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        kotlin.test.assertTrue(vm().timerRunning)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag("menu-continue").assertExists()
        kotlin.test.assertTrue(!vm().timerRunning, "o relógio deveria ter parado ao voltar ao menu")
    }

    @Test
    fun `new game over a game in progress asks for confirmation`() {
        startNewGame()
        rule.onNodeWithTag("stock").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("menu").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("ingame-exit").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("menu-new").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("dialog-play").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Descartar a partida em andamento?").assertExists()
    }
}
