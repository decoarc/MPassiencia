package io.github.decarc.mpaciencia.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.Ink
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import io.github.decarc.mpaciencia.ui.theme.feltBackground

/** Cabeçalho comum às telas secundárias: botão Voltar e título. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Ink),
            modifier = Modifier.testTag("back"),
        ) { Text("← " + stringResource(R.string.back), fontWeight = FontWeight.Bold) }
        Text(title, color = TitleCream, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    }
}

@Composable
fun RulesScreen(onBack: () -> Unit) {
    val sections = listOf(
        R.string.rules_goal_title to R.string.rules_goal,
        R.string.rules_table_title to R.string.rules_table,
        R.string.rules_columns_title to R.string.rules_columns,
        R.string.rules_stock_title to R.string.rules_stock,
        R.string.rules_play_title to R.string.rules_play,
    )
    Box(Modifier.fillMaxSize().feltBackground().windowInsetsPadding(WindowInsets.displayCutout)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(stringResource(R.string.rules_title), onBack)
            Column(
                Modifier.padding(top = 12.dp).widthIn(max = 760.dp).align(Alignment.CenterHorizontally)
                    .background(Cream, RoundedCornerShape(14.dp)).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sections.forEach { (title, body) ->
                    Text(stringResource(title), color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(stringResource(body), color = Ink, fontSize = 16.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}
