package io.github.decarc.mpaciencia.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.core.engine.Deal
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.Gold
import io.github.decarc.mpaciencia.ui.theme.Ink
import kotlin.random.Random

/** Número do jogo válido (1 a 99999), ou `null`. */
fun parseGameNumber(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in Deal.MIN_GAME..Deal.MAX_GAME }

private fun randomGameNumber() = Random.nextInt(Deal.MIN_GAME, Deal.MAX_GAME + 1)

@Composable
fun NewGameDialog(
    defaultMode: DrawMode,
    gameInProgress: Boolean,
    onPlay: (number: Int, mode: DrawMode) -> Unit,
    onDismiss: () -> Unit,
    initialNumber: Int = randomGameNumber(),
) {
    var text by remember { mutableStateOf(initialNumber.toString()) }
    var mode by remember { mutableStateOf(defaultMode) }
    var confirming by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val number = parseGameNumber(text)

    if (confirming && number != null) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.discard_title)) },
            confirmButton = {
                TextButton(onClick = { onPlay(number, mode) }, modifier = Modifier.testTag("discard-confirm")) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.back)) } },
            containerColor = Cream,
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_game_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editing) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { v -> text = v.filter(Char::isDigit).take(5) },
                            label = { Text(stringResource(R.string.game_number_label)) },
                            singleLine = true,
                            isError = number == null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("game-number"),
                        )
                    } else {
                        // Mostra o número sorteado; o campo (e o teclado) só aparece se a pessoa quiser trocar.
                        TextButton(
                            onClick = { editing = true },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("game-number-edit"),
                        ) { Text(stringResource(R.string.game_number_value, text), fontSize = 20.sp) }
                    }
                    val desc = stringResource(R.string.random_number_desc)
                    TextButton(
                        onClick = { text = randomGameNumber().toString() },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = desc },
                    ) { Text(stringResource(R.string.random_number)) }
                }
                ModeOption(stringResource(R.string.mode_one), mode == DrawMode.ONE) { mode = DrawMode.ONE }
                ModeOption(stringResource(R.string.mode_three), mode == DrawMode.THREE) { mode = DrawMode.THREE }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (number != null) { if (gameInProgress) confirming = true else onPlay(number, mode) } },
                enabled = number != null,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                modifier = Modifier.testTag("dialog-play"),
            ) { Text(stringResource(R.string.play)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        containerColor = Cream,
    )
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.semantics { })
    }
}
