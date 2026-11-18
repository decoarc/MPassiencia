package io.github.decarc.mpaciencia.ui.game

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.decarc.mpaciencia.core.engine.GameCodec
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.data.SavedGame
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun capture(name: String, vm: GameViewModel) {
        rule.setContent { MPacienciaTheme { GameScreen(vm, onOpenMenu = {}, onNewGame = {}) } }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    private fun newGame() = GameViewModel(FakeGameStore(), FakeSettingsStore(), FakeClock()).also { it.newGame(1, DrawMode.ONE) }

    /** Fim de partida real: colunas longas, tudo virado. */
    private fun lateGame(): GameViewModel {
        val json = javaClass.classLoader!!.getResource("near_win.json")!!.readText()
        return GameViewModel(FakeGameStore(SavedGame(GameCodec.decode(json)!!, 0)), FakeSettingsStore(), FakeClock())
            .also { it.continueSaved() }
    }

    @Test @Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
    fun note10NewGame() = capture("game_note10_new", newGame())

    @Test @Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
    fun note10LateGame() = capture("game_note10_late", lateGame())

    @Test @Config(qualifiers = "w640dp-h360dp-land-xhdpi")
    fun smallNewGame() = capture("game_small_new", newGame())

    @Test @Config(qualifiers = "w640dp-h360dp-land-xhdpi")
    fun smallLateGame() = capture("game_small_late", lateGame())

    @Test @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun tabletNewGame() = capture("game_tablet_new", newGame())

    @Test @Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
    fun tabletLateGame() = capture("game_tablet_late", lateGame())
}
