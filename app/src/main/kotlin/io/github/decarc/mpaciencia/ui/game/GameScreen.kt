package io.github.decarc.mpaciencia.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.Gold
import io.github.decarc.mpaciencia.ui.theme.Ink
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import io.github.decarc.mpaciencia.ui.theme.feltBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Tempo em mm:ss, ou h:mm:ss a partir de 1 hora. */
fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun GameScreen(
    vm: GameViewModel,
    onOpenMenu: () -> Unit,
    onNewGame: () -> Unit,
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val game = ui.game ?: return

    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.resume() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.pause() }

    var clockText by remember { mutableLongStateOf(0L) }
    // O relógio espera fora da thread principal: ela fica livre (e ociosa) entre os tiques.
    LaunchedEffect(vm) {
        withContext(Dispatchers.Default) {
            while (true) {
                val now = vm.elapsedNow()
                if (now / 1000 != clockText / 1000) clockText = now
                delay(250)
            }
        }
    }

    Box(Modifier.fillMaxSize().feltBackground().windowInsetsPadding(WindowInsets.displayCutout)) {
        TableView(
            state = game.state,
            hint = ui.hint,
            moveMillis = settings.animationSpeed.moveMillis,
            onTapStock = { vm.tapStock() },
            onTapCard = vm::tapCard,
            onDrop = vm::drop,
        )

        // Barra superior.
        Row(
            Modifier.fillMaxWidth().height(TableLayout.TOP_BAR.dp).background(Color.Black.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp).zIndex(20_000f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.game_header, game.gameNumber, game.state.score, game.state.moves, formatElapsed(clockText)),
                color = TitleCream,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BarButton(stringResource(R.string.undo), "undo") { vm.undo() }
                BarButton(stringResource(R.string.hint), "hint") { vm.hint() }
                BarButton(stringResource(R.string.menu), "menu") {
                    vm.pause()
                    onOpenMenu()
                }
            }
        }

        if (ui.canFinish && !ui.finishing) {
            Button(
                onClick = { vm.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).zIndex(20_000f).testTag("finish"),
            ) { Text(stringResource(R.string.finish), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }

        if (ui.stuck) {
            AlertDialog(
                onDismissRequest = { vm.dismissStuck() },
                title = { Text(stringResource(R.string.stuck_title)) },
                text = { Text(stringResource(R.string.stuck_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        vm.dismissStuck()
                        onNewGame()
                    }) { Text(stringResource(R.string.new_game)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            vm.dismissStuck()
                            vm.undo()
                        }) { Text(stringResource(R.string.stuck_undo)) }
                        TextButton(onClick = {
                            vm.dismissStuck()
                            vm.restart()
                        }) { Text(stringResource(R.string.restart_game)) }
                    }
                },
                containerColor = Cream,
            )
        }
    }
}

@Composable
private fun BarButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Ink),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = Modifier.height(30.dp).testTag(tag),
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}
