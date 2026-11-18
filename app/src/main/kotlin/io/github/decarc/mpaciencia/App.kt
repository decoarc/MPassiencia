package io.github.decarc.mpaciencia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.decarc.mpaciencia.audio.SoundPlayer
import io.github.decarc.mpaciencia.audio.SoundPoolBackend
import io.github.decarc.mpaciencia.data.DataStoreSettingsStore
import io.github.decarc.mpaciencia.data.FileGameStore
import io.github.decarc.mpaciencia.data.settingsDataStore
import io.github.decarc.mpaciencia.ui.game.GameEntry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import io.github.decarc.mpaciencia.ui.game.GameViewModel
import io.github.decarc.mpaciencia.ui.menu.MenuScreen
import io.github.decarc.mpaciencia.ui.menu.NewGameDialog
import io.github.decarc.mpaciencia.ui.rules.RulesScreen
import io.github.decarc.mpaciencia.ui.settings.SettingsScreen
import io.github.decarc.mpaciencia.ui.theme.Cream
import kotlinx.serialization.Serializable

@Serializable data object MenuRoute : NavKey

@Serializable data object GameRoute : NavKey

@Serializable data object RulesRoute : NavKey

@Serializable data object SettingsRoute : NavKey

/** Volta uma tela só se [route] ainda estiver no topo: um toque duplo não esvazia a pilha. */
fun popIfTop(backStack: MutableList<NavKey>, route: NavKey) {
    if (backStack.size > 1 && backStack.last() == route) backStack.removeAt(backStack.lastIndex)
}

@Composable
fun App() {
    val context = LocalContext.current.applicationContext
    val vm: GameViewModel = viewModel(
        factory = GameViewModel.factory(FileGameStore(context.filesDir), DataStoreSettingsStore(context.settingsDataStore)),
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val player = remember { SoundPlayer(SoundPoolBackend(context)) { vm.settings.value.soundOn } }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(vm, player) { vm.sounds.collect(player::play) }

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
        backStack.removeAll { it != MenuRoute }
    }

    // O relógio corre só com a mesa à vista, sem menu ou diálogo aberto, e com o app em primeiro plano.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val started = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val tableVisible = backStack.lastOrNull() == GameRoute && !showInGameMenu && !showNewGame && ui.game != null
    LaunchedEffect(tableVisible, started) {
        if (tableVisible && started) vm.resume() else vm.pause()
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
                GameEntry(
                    vm = vm,
                    onMissing = { toMenu() },
                    onOpenMenu = { showInGameMenu = true },
                    onNewGame = { showNewGame = true },
                    onExitToMenu = { toMenu() },
                )
            }
            entry<RulesRoute> { RulesScreen(onBack = { popIfTop(backStack, RulesRoute) }) }
            entry<SettingsRoute> {
                SettingsScreen(settings, onChange = vm::updateSettings, onBack = { popIfTop(backStack, SettingsRoute) })
            }
        },
    )

    if (showInGameMenu) {
        InGameMenu(
            onContinue = { showInGameMenu = false },
            onNewGame = {
                showInGameMenu = false
                showNewGame = true
            },
            onRestart = {
                showInGameMenu = false
                vm.restart()
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

