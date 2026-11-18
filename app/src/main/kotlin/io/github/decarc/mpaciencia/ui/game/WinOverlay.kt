package io.github.decarc.mpaciencia.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.data.AnimationSpeed
import io.github.decarc.mpaciencia.ui.cards.CardFace
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.CreamShadow
import io.github.decarc.mpaciencia.ui.theme.Gold
import io.github.decarc.mpaciencia.ui.theme.Ink
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WinOverlay(
    score: Int,
    moves: Int,
    elapsedMs: Long,
    gameNumber: Int,
    mode: DrawMode,
    animationSpeed: AnimationSpeed,
    onNewGame: () -> Unit,
    onMenu: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        if (animationSpeed != AnimationSpeed.OFF) FallingCards()
        Column(
            Modifier.width(360.dp).background(Cream, RoundedCornerShape(18.dp)).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.win_title), color = Ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
            Text(stringResource(R.string.win_points, score), color = Ink, fontSize = 18.sp)
            Text(stringResource(R.string.win_moves, moves), color = Ink, fontSize = 18.sp)
            Text(stringResource(R.string.win_time, formatElapsed(elapsedMs)), color = Ink, fontSize = 18.sp)
            val modeLabel = stringResource(if (mode == DrawMode.ONE) R.string.mode_short_one else R.string.mode_short_three)
            Text(stringResource(R.string.win_game, gameNumber, modeLabel), color = Ink.copy(alpha = 0.8f), fontSize = 15.sp)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onNewGame,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
                    modifier = Modifier.height(50.dp).testTag("win-new"),
                ) { Text(stringResource(R.string.new_game), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                Button(
                    onClick = onMenu,
                    colors = ButtonDefaults.buttonColors(containerColor = CreamShadow, contentColor = Ink),
                    modifier = Modifier.height(50.dp).testTag("win-menu"),
                ) { Text(stringResource(R.string.win_menu), fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            }
        }
    }
}

/** 24 cartas caindo e quicando, em ciclo. */
@Composable
private fun FallingCards() {
    val t by rememberInfiniteTransition(label = "win").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "fall",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value
        repeat(24) { i ->
            val phase = (t + i / 24f) % 1f
            val x = (i % 12) / 12f * w + 10f
            val y = abs(sin(phase * Math.PI * 2.5)).toFloat() * -0.55f * h + h * phase * 0.9f
            CardFace(
                Card(Suit.entries[i % 4], 13 - (i % 13), i % 3),
                Modifier.offset(x.dp, y.dp).width(48.dp).rotate(phase * 360f * if (i % 2 == 0) 1 else -1),
            )
        }
    }
}
