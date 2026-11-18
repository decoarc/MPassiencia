package io.github.decarc.mpaciencia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import io.github.decarc.mpaciencia.data.DataStoreSettingsStore
import io.github.decarc.mpaciencia.data.FileGameStore
import io.github.decarc.mpaciencia.data.settingsDataStore
import io.github.decarc.mpaciencia.ui.game.GameScreen
import io.github.decarc.mpaciencia.ui.game.GameViewModel
import io.github.decarc.mpaciencia.ui.menu.MenuScreen
import io.github.decarc.mpaciencia.ui.menu.NewGameDialog
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import io.github.decarc.mpaciencia.ui.theme.feltBackground
import kotlinx.serialization.Serializable

@Serializable data object MenuRoute : NavKey

@Serializable data object GameRoute : NavKey

@Serializable data object RulesRoute : NavKey

@Serializable data object SettingsRoute : NavKey

@Composable
fun App() {
    val context = LocalContext.current.applicationContext
    val vm: GameViewModel = viewModel(
        factory = GameViewModel.factory(FileGameStore(context.filesDir), DataStoreSettingsStore(context.settingsDataStore)),
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(MenuRoute)
    var showNewGame by remember { mutableStateOf(false) }
    var showInGameMenu by remember { mutableStateOf(false) }

    fun openGame() {
        if (backStack.lastOrNull() != GameRoute) {
            backStack.removeAll { it != MenuRoute }
            backStack.add(GameRoute)
        }
    }

    fun toMenu() {
        vm.pause()
        backStack.removeAll { it != MenuRoute }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) },
        entryProvider = entryProvider {
            entry<MenuRoute> {
                MenuScreen(
                    hasSavedGame = ui.hasSavedGame,
                    soundOn = settings.soundOn,
                    onContinue = {
                        vm.continueSaved()
                        openGame()
                    },
                    onNewGame = { showNewGame = true },
                    onRules = { backStack.add(RulesRoute) },
                    onSettings = { backStack.add(SettingsRoute) },
                    onToggleSound = { vm.updateSettings { it.copy(soundOn = !it.soundOn) } },
                )
            }
            entry<GameRoute> {
                GameScreen(
                    vm = vm,
                    onOpenMenu = { showInGameMenu = true },
                    onNewGame = { showNewGame = true },
                )
            }
            entry<RulesRoute> { Placeholder(stringResource(R.string.how_to_play)) { backStack.removeAt(backStack.lastIndex) } }
            entry<SettingsRoute> { Placeholder(stringResource(R.string.options)) { backStack.removeAt(backStack.lastIndex) } }
        },
    )

    if (showInGameMenu) {
        InGameMenu(
            onContinue = {
                showInGameMenu = false
                vm.resume()
            },
            onNewGame = {
                showInGameMenu = false
                showNewGame = true
            },
            onRestart = {
                showInGameMenu = false
                vm.restart()
                vm.resume()
            },
            onSettings = {
                showInGameMenu = false
                backStack.add(SettingsRoute)
            },
            onRules = {
                showInGameMenu = false
                backStack.add(RulesRoute)
            },
            onExit = {
                showInGameMenu = false
                toMenu()
            },
        )
    }

    if (showNewGame) {
        val game = ui.game
        NewGameDialog(
            defaultMode = settings.defaultDrawMode,
            gameInProgress = ui.hasSavedGame && game != null && !ui.won && game.moves.isNotEmpty(),
            onPlay = { number, mode ->
                showNewGame = false
                vm.newGame(number, mode)
                openGame()
            },
            onDismiss = { showNewGame = false },
        )
    }
}

@Composable
private fun InGameMenu(
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
    onRules: () -> Unit,
    onExit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onContinue,
        confirmButton = {},
        containerColor = Cream,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                MenuItem(stringResource(R.string.continue_game), "ingame-continue", onContinue)
                MenuItem(stringResource(R.string.new_game), "ingame-new", onNewGame)
                MenuItem(stringResource(R.string.restart_game), "ingame-restart", onRestart)
                MenuItem(stringResource(R.string.options), "ingame-options", onSettings)
                MenuItem(stringResource(R.string.how_to_play), "ingame-rules", onRules)
                MenuItem(stringResource(R.string.exit_to_menu), "ingame-exit", onExit)
            }
        },
    )
}

@Composable
private fun MenuItem(label: String, tag: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) { Text(label) }
}

/** Tela provisória até a Task 9. */
@Composable
private fun Placeholder(title: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().feltBackground(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onBack) { Text(title, color = TitleCream) }
    }
}
