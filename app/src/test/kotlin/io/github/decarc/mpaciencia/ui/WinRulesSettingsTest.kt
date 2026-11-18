package io.github.decarc.mpaciencia.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.data.AnimationSpeed
import io.github.decarc.mpaciencia.data.DataStoreSettingsStore
import io.github.decarc.mpaciencia.data.Settings
import io.github.decarc.mpaciencia.ui.game.WinOverlay
import io.github.decarc.mpaciencia.ui.rules.RulesScreen
import io.github.decarc.mpaciencia.ui.settings.SettingsScreen
import io.github.decarc.mpaciencia.ui.theme.MPacienciaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w869dp-h411dp-land-xxhdpi")
class WinRulesSettingsTest {
    @get:Rule
    val rule = createComposeRule()

    private fun win(onNewGame: () -> Unit = {}, onMenu: () -> Unit = {}) {
        rule.setContent {
            MPacienciaTheme {
                WinOverlay(
                    score = 2015, moves = 413, elapsedMs = 754_000, gameNumber = 220, mode = DrawMode.THREE,
                    animationSpeed = AnimationSpeed.OFF, onNewGame = onNewGame, onMenu = onMenu,
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `win overlay shows score, moves, time and game number`() {
        win()
        rule.onNodeWithText("Você venceu!").assertExists()
        rule.onNodeWithText("Pontos: 2015").assertExists()
        rule.onNodeWithText("Movimentos: 413").assertExists()
        rule.onNodeWithText("Tempo: 12:34").assertExists()
        rule.onNodeWithText("Jogo #220 · Vira 3").assertExists()
        rule.onRoot().captureRoboImage("src/test/snapshots/win.png")
    }

    @Test
    fun `win overlay buttons call back`() {
        var newGame = 0
        var menu = 0
        win(onNewGame = { newGame++ }, onMenu = { menu++ })
        rule.onNodeWithTag("win-new").performClick()
        rule.onNodeWithTag("win-menu").performClick()
        rule.waitForIdle()
        assertEquals(1, newGame)
        assertEquals(1, menu)
    }

    @Test
    fun `rules screen shows the empty column rule`() {
        rule.setContent { MPacienciaTheme { RulesScreen(onBack = {}) } }
        rule.onNodeWithText("Coluna vazia só aceita um Rei", substring = true).assertExists()
        rule.onRoot().captureRoboImage("src/test/snapshots/rules.png")
    }

    private val file = File(Files.createTempDirectory("prefs").toFile(), "settings.preferences_pb")
    private val store = DataStoreSettingsStore(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob()), produceFile = { file }),
    )

    private fun showSettings() {
        rule.setContent {
            MPacienciaTheme {
                val s by store.settings.collectAsState(Settings())
                val scope = rememberCoroutineScope()
                SettingsScreen(s, onChange = { t -> scope.launch { store.update(t) } }, onBack = {})
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun `changing draw mode in settings is persisted`() {
        showSettings()
        rule.onNodeWithTag("settings-mode-three").performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) { runBlocking { store.settings.first().defaultDrawMode == DrawMode.THREE } }
        assertEquals(DrawMode.THREE, runBlocking { store.settings.first() }.defaultDrawMode)
        rule.onRoot().captureRoboImage("src/test/snapshots/settings.png")
    }

    @Test
    fun `animation speed off and sound off are persisted`() {
        showSettings()
        rule.onNodeWithTag("settings-speed-OFF").performClick()
        rule.onNodeWithTag("settings-sound").performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) {
            runBlocking { store.settings.first().let { it.animationSpeed == AnimationSpeed.OFF && !it.soundOn } }
        }
        assertTrue(runBlocking { store.settings.first() }.let { it.animationSpeed == AnimationSpeed.OFF && !it.soundOn })
    }
}
