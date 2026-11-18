package io.github.decarc.mpaciencia.ui.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.CardColor
import io.github.decarc.mpaciencia.core.model.Suit
import io.github.decarc.mpaciencia.ui.theme.BackRed
import io.github.decarc.mpaciencia.ui.theme.BackRedLight
import io.github.decarc.mpaciencia.ui.theme.CardBlack
import io.github.decarc.mpaciencia.ui.theme.CardBorder
import io.github.decarc.mpaciencia.ui.theme.CardRed
import io.github.decarc.mpaciencia.ui.theme.SlotLine

/** Proporção altura/largura de todas as cartas. */
const val CARD_ASPECT = 1.4f

fun rankLabel(rank: Int): String = when (rank) {
    1 -> "A"
    11 -> "J"
    12 -> "Q"
    13 -> "K"
    else -> rank.toString()
}

fun suitSymbol(suit: Suit): String = when (suit) {
    Suit.HEARTS -> "♥"
    Suit.DIAMONDS -> "♦"
    Suit.SPADES -> "♠"
    Suit.CLUBS -> "♣"
}

private fun cardShape(width: Dp) = RoundedCornerShape(width * 0.08f)

@Composable
private fun Dp.asSp(fraction: Float): TextUnit = with(LocalDensity.current) { (this@asSp * fraction).toSp() }

/** Face da carta: canto com valor e naipe; no centro, o naipe grande ou a letra da figura. */
@Composable
fun CardFace(card: Card, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f / CARD_ASPECT)) {
        val w = maxWidth
        val color = if (card.color == CardColor.RED) CardRed else CardBlack
        val shape = cardShape(w)
        Box(
            Modifier.fillMaxSize()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, CardBorder, shape),
        ) {
            val corner = TextStyle(color = color, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = w.asSp(0.26f), lineHeight = w.asSp(0.27f))
            // Valor e naipe na mesma linha: aparecem juntos até na tira mínima de uma coluna apertada.
            Text(
                rankLabel(card.rank) + suitSymbol(card.suit),
                style = corner,
                softWrap = false,
                modifier = Modifier.padding(start = w * 0.06f, top = w * 0.03f),
            )
            val face = card.rank >= 11
            Column(
                Modifier.align(Alignment.Center).padding(top = w * 0.30f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (face) {
                    Text(rankLabel(card.rank), style = corner.copy(fontSize = w.asSp(0.42f), lineHeight = w.asSp(0.44f)), softWrap = false)
                } else {
                    Text(suitSymbol(card.suit), style = corner.copy(fontSize = w.asSp(0.45f), lineHeight = w.asSp(0.47f)), softWrap = false)
                }
            }
        }
    }
}

/** Verso próprio: listras diagonais vermelhas com moldura branca. */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f / CARD_ASPECT)) {
        val w = maxWidth
        val shape = cardShape(w)
        Box(
            Modifier.fillMaxSize()
                .clip(shape)
                .background(Color.White)
                .border(1.dp, CardBorder, shape)
                .padding(w * 0.06f)
                .clip(RoundedCornerShape(w * 0.05f)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(BackRed)
                val stripe = size.width / 6f
                var x = -size.height
                while (x < size.width) {
                    drawLine(BackRedLight, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stripe / 2f)
                    x += stripe
                }
            }
        }
    }
}

/** Espaço vazio sobre o feltro, com uma letra discreta ("A" nas fundações, "K" nas colunas). */
@Composable
fun EmptySlot(label: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.aspectRatio(1f / CARD_ASPECT)) {
        val w = maxWidth
        val shape = cardShape(w)
        Box(
            Modifier.fillMaxSize()
                .clip(shape)
                .background(Color.Black.copy(alpha = 0.08f))
                .border(2.dp, SlotLine, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = SlotLine, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = w.asSp(0.34f))
        }
    }
}
