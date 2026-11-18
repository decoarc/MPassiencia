package io.github.decarc.mpaciencia.ui.menu

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class MenuScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun menu(hasSaved: Boolean) {
        rule.setContent {
            MPacienciaTheme { MenuScreen(hasSaved, soundOn = true, onContinue = {}, onNewGame = {}, onRules = {}, onSettings = {}, onToggleSound = {}) }
        }
        rule.waitForIdle()
    }

    @Test
    fun menuWithContinue() {
        menu(hasSaved = true)
        rule.onRoot().captureRoboImage("src/test/snapshots/menu_continue.png")
    }

    @Test
    fun menuWithoutContinue() {
        menu(hasSaved = false)
        rule.onRoot().captureRoboImage("src/test/snapshots/menu_first_time.png")
    }

    @Test
    fun newGameDialog() {
        rule.setContent { MPacienciaTheme { NewGameDialog(DrawMode.ONE, gameInProgress = false, onPlay = { _, _ -> }, onDismiss = {}, initialNumber = 220) } }
        rule.waitForIdle()
        captureScreenRoboImage("src/test/snapshots/new_game_dialog.png")
    }
}
