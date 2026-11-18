package io.github.decarc.mpaciencia.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.ui.cards.CardFace
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.CreamShadow
import io.github.decarc.mpaciencia.ui.theme.Gold
import io.github.decarc.mpaciencia.ui.theme.GoldShadow
import io.github.decarc.mpaciencia.ui.theme.Ink
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import io.github.decarc.mpaciencia.ui.theme.feltBackground

@Composable
fun MenuScreen(
    hasSavedGame: Boolean,
    soundOn: Boolean,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
    onToggleSound: () -> Unit,
) {
    Box(Modifier.fillMaxSize().feltBackground().windowInsetsPadding(WindowInsets.displayCutout)) {
        Row(Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val soundDesc = stringResource(if (soundOn) R.string.sound_on else R.string.sound_off)
            RoundIcon(if (soundOn) "🔊" else "🔇", soundDesc, "menu-sound", onToggleSound)
            RoundIcon("⚙", stringResource(R.string.options), "menu-settings", onSettings)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            CardFan()
            Text(
                stringResource(R.string.app_name),
                style = TextStyle(
                    color = TitleCream,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 4f), 6f),
                ),
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            if (hasSavedGame) {
                MenuButton(stringResource(R.string.continue_game), primary = true, width = 300.dp, tag = "menu-continue", onClick = onContinue)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 10.dp)) {
                MenuButton(stringResource(R.string.new_game), primary = false, width = 144.dp, tag = "menu-new", onClick = onNewGame)
                MenuButton(stringResource(R.string.how_to_play), primary = false, width = 144.dp, tag = "menu-rules", onClick = onRules)
            }
            MenuButton(stringResource(R.string.options), primary = false, width = 300.dp, tag = "menu-options", onClick = onSettings, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun CardFan() {
    val cards = listOf(Card(Suit.HEARTS, 1, 0), Card(Suit.SPADES, 13, 0), Card(Suit.DIAMONDS, 12, 0), Card(Suit.CLUBS, 11, 0))
    val angles = listOf(-24f, -8f, 8f, 24f)
    Box(Modifier.size(150.dp, 100.dp), contentAlignment = Alignment.TopCenter) {
        cards.forEachIndexed { i, card ->
            CardFace(
                card,
                Modifier.offset(y = 6.dp).width(54.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1.25f)
                        rotationZ = angles[i]
                    }
                    .shadow(4.dp, RoundedCornerShape(5.dp)),
            )
        }
    }
}

@Composable
private fun MenuButton(label: String, primary: Boolean, width: Dp, tag: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shadow = if (primary) GoldShadow else CreamShadow
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.width(width)) {
        // "Sombra" sólida embaixo do botão, como nos rascunhos.
        Box(Modifier.matchParentSize().offset(y = 3.dp).background(shadow, shape))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = if (primary) Gold else Cream, contentColor = Ink),
            shape = shape,
            modifier = Modifier.width(width).height(if (primary) 56.dp else 50.dp).testTag(tag),
        ) { Text(label, fontWeight = FontWeight.Bold, fontSize = if (primary) 20.sp else 17.sp) }
    }
}

@Composable
private fun RoundIcon(symbol: String, description: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick).semantics { contentDescription = description }.testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 20.sp, color = TitleCream) }
}
