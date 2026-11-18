package io.github.decarc.mpaciencia.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.core.assist.Hint
import io.github.decarc.mpaciencia.core.model.Card
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.PileRef
import io.github.decarc.mpaciencia.ui.cards.CardBack
import io.github.decarc.mpaciencia.ui.cards.CardFace
import io.github.decarc.mpaciencia.ui.cards.EmptySlot
import io.github.decarc.mpaciencia.ui.theme.HintOutline
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import kotlin.math.roundToInt

/** Uma carta desenhada na mesa: posição em dp (sem rolagem), pilha de origem e quantas cartas vão junto ao tocar. */
private data class Sprite(
    val card: Card,
    val faceUp: Boolean,
    val x: Float,
    val y: Float,
    val pile: PileRef,
    /** Cartas da pilha a partir desta até o topo; 0 = não pode ser tocada/arrastada. */
    val grab: Int,
    val z: Float,
    val inTableau: Boolean,
)

private fun sprites(s: GameState, g: TableGeometry): List<Sprite> = buildList {
    var z = 0f
    s.stock.forEach { add(Sprite(it, false, g.stock.x, g.stock.y, PileRef.Stock, 0, z++, false)) }
    val fanned = if (s.drawMode == DrawMode.THREE) 3 else 1
    val fanStart = s.waste.size - minOf(fanned, s.waste.size)
    s.waste.forEachIndexed { i, card ->
        val fanIndex = (i - fanStart).coerceAtLeast(0)
        val x = g.waste.x + if (i >= fanStart) fanIndex * g.wasteFanStep else 0f
        add(Sprite(card, true, x, g.waste.y, PileRef.Waste, if (i == s.waste.lastIndex) 1 else 0, z++, false))
    }
    s.foundations.forEachIndexed { f, pile ->
        pile.forEachIndexed { i, card ->
            add(Sprite(card, true, g.foundations[f].x, g.foundations[f].y, PileRef.Foundation(f), if (i == pile.lastIndex) 1 else 0, z++, false))
        }
    }
    s.tableau.forEachIndexed { c, column ->
        column.forEachIndexed { i, tc ->
            val grab = if (tc.faceUp) column.size - i else 0
            add(Sprite(tc.card, tc.faceUp, g.columns[c].x, TableLayout.cardTop(g, c, i, s), PileRef.Tableau(c), grab, z++, true))
        }
    }
}

private fun Card.tag() = "card-$rank${suit.name}$deck"

/** Cartas do topo da pilha [pile] que a dica aponta. */
private fun hintSourceCards(s: GameState, hint: Hint?): Set<Card> {
    val move = (hint as? Hint.Play)?.move ?: return emptySet()
    val cards = when (val from = move.from) {
        PileRef.Waste -> s.waste.takeLast(1)
        is PileRef.Foundation -> s.foundations[from.index].takeLast(1)
        is PileRef.Tableau -> s.tableau[from.index].takeLast(move.count).map { it.card }
        PileRef.Stock -> emptyList()
    }
    return cards.toSet()
}

private fun hintTargetRect(s: GameState, g: TableGeometry, hint: Hint?): Rect? {
    val move = (hint as? Hint.Play)?.move ?: return null
    return when (val to = move.to) {
        is PileRef.Foundation -> g.foundations[to.index]
        is PileRef.Tableau -> {
            val col = s.tableau[to.index]
            val y = if (col.isEmpty()) g.columns[to.index].y else TableLayout.cardTop(g, to.index, col.lastIndex, s)
            Rect(g.columns[to.index].x, y, g.cardW, g.cardH)
        }
        else -> null
    }
}

/**
 * A mesa: cada carta é desenhada uma vez, com a própria [Card] como chave, e desliza animada
 * para a posição que a [TableLayout] calcula. Toque = jogada automática; arrastar = escolher o destino.
 */
@Composable
fun TableView(
    state: GameState,
    hint: Hint?,
    moveMillis: Int,
    onTapStock: () -> Unit,
    onTapCard: (PileRef, Int) -> Boolean,
    onDrop: (PileRef, Int, PileRef?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val g = remember(state, maxWidth, maxHeight) { TableLayout.compute(maxWidth.value, maxHeight.value, state) }
        val density = LocalDensity.current
        var scrollY by remember { mutableFloatStateOf(0f) }
        val maxScroll = if (g.scrolls) g.tableauContentHeight - (maxHeight.value - g.tableauTop - TableLayout.MARGIN) else 0f
        if (scrollY > maxScroll) scrollY = maxScroll.coerceAtLeast(0f)

        // Arrastar: origem, quantidade e deslocamento em dp.
        var dragFrom by remember { mutableStateOf<PileRef?>(null) }
        var dragCount by remember { mutableIntStateOf(0) }
        var dragDelta by remember { mutableStateOf(Offset.Zero) }
        val releaseFrom = remember { mutableStateMapOf<Card, Offset>() }
        val shakes = remember { mutableStateMapOf<Card, Int>() }

        val all = sprites(state, g)
        val sources = hintSourceCards(state, hint)
        val draggedCards = dragFrom?.let { from -> all.filter { it.pile == from && it.grab in 1..dragCount }.map { it.card }.toSet() } ?: emptySet()

        // Espaços vazios (por baixo das cartas).
        g.foundations.forEachIndexed { i, r -> Slot(r, "A", Modifier.testTag("foundation-$i")) }
        g.columns.forEachIndexed { i, r ->
            if (r.y - scrollY >= g.tableauTop - 0.5f) Slot(Rect(r.x, r.y - scrollY, r.w, r.h), "K", Modifier.testTag("column-$i"))
        }
        Slot(g.waste.copy(w = g.cardW), "", Modifier.testTag("waste"))
        if (state.stock.isEmpty()) Slot(g.stock, stringResource(R.string.recycle_label), Modifier)

        // Rolagem da área das colunas (só quando a coluna mais longa não cabe).
        if (g.scrolls) {
            Box(
                Modifier.offset(0.dp, g.tableauTop.dp)
                    .size(maxWidth, (maxHeight.value - g.tableauTop).dp)
                    .scrollable(
                        rememberScrollableState { delta ->
                            val d = delta / density.density
                            val before = scrollY
                            scrollY = (scrollY - d).coerceIn(0f, maxScroll)
                            (before - scrollY) * density.density
                        },
                        Orientation.Vertical,
                    ),
            )
        }

        // Valores sempre atuais para os gestos (os blocos de gesto só reiniciam quando a carta muda de pilha).
        val currentState by rememberUpdatedState(state)
        val currentG by rememberUpdatedState(g)
        val currentAll by rememberUpdatedState(all)
        val currentOnDrop by rememberUpdatedState(onDrop)
        val currentOnTap by rememberUpdatedState(onTapCard)

        all.forEach { sp ->
            key(sp.card) {
                // Posição sem rolagem; a rolagem e o arraste só são lidos no layout/desenho (sem recompor a mesa).
                val target = Offset(sp.x, sp.y)
                val anim = remember { Animatable(target, Offset.VectorConverter) }
                val release = releaseFrom[sp.card]
                LaunchedEffect(target, release) {
                    if (release != null) {
                        anim.snapTo(release)
                        releaseFrom.remove(sp.card)
                    }
                    if (moveMillis <= 0) anim.snapTo(target) else anim.animateTo(target, tween(moveMillis))
                }
                val shake = remember { Animatable(0f) }
                val shakeTick = shakes[sp.card] ?: 0
                LaunchedEffect(shakeTick) {
                    if (shakeTick > 0) {
                        repeat(3) {
                            shake.animateTo(4f, tween(40))
                            shake.animateTo(-4f, tween(40))
                        }
                        shake.animateTo(0f, tween(40))
                    }
                }
                val dragging = sp.card in draggedCards
                val inTableau = sp.inTableau
                fun shownY(): Float {
                    val base = if (dragging) target.y + dragDelta.y else anim.value.y
                    return if (inTableau) base - scrollY else base
                }
                // A fileira de cima fica acima das colunas; o grupo arrastado fica acima de tudo.
                val z = when {
                    dragging -> 10_000f + sp.z
                    inTableau -> sp.z
                    else -> 2_000f + sp.z
                }
                var m = Modifier
                    .zIndex(z)
                    .offset {
                        val x = (if (dragging) target.x + dragDelta.x else anim.value.x) + shake.value
                        IntOffset((x * density.density).roundToInt(), (shownY() * density.density).roundToInt())
                    }
                    .size(g.cardW.dp, g.cardH.dp)
                    // Coluna rolada: a parte da carta acima da linha da mesa não é desenhada.
                    .drawWithContent {
                        val hidden = (g.tableauTop - shownY()) * density.density
                        if (inTableau && !dragging && !anim.isRunning && hidden > 0f) {
                            clipRect(top = hidden) { this@drawWithContent.drawContent() }
                        } else {
                            drawContent()
                        }
                    }
                    .testTag(sp.card.tag())
                if (sp.card in sources) {
                    m = m.border(3.dp, HintOutline, RoundedCornerShape((g.cardW * 0.08f).dp))
                }
                if (sp.grab > 0) {
                    m = m.pointerInput(sp.card, sp.pile, sp.grab) {
                        detectTapGestures {
                            if (!currentOnTap(sp.pile, sp.grab)) shakes[sp.card] = (shakes[sp.card] ?: 0) + 1
                        }
                    }.pointerInput(sp.card, sp.pile, sp.grab, "drag") {
                        detectDragGestures(
                            onDragStart = {
                                dragFrom = sp.pile
                                dragCount = sp.grab
                                dragDelta = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += Offset(amount.x / density.density, amount.y / density.density)
                            },
                            onDragEnd = {
                                val from = dragFrom
                                val count = dragCount
                                val delta = dragDelta
                                val geo = currentG
                                val group = currentAll.filter { it.pile == from && it.grab in 1..count }
                                group.forEach { releaseFrom[it.card] = Offset(it.x, it.y) + delta }
                                dragFrom = null
                                dragDelta = Offset.Zero
                                val top = group.maxByOrNull { it.grab } ?: return@detectDragGestures
                                val cx = top.x + delta.x + geo.cardW / 2
                                val cy = (if (top.inTableau) top.y - scrollY else top.y) + delta.y + geo.cardH / 2
                                if (from != null) currentOnDrop(from, count, TableLayout.hitTest(geo, currentState, cx, cy, scrollY))
                            },
                            onDragCancel = {
                                dragFrom = null
                                dragDelta = Offset.Zero
                            },
                        )
                    }
                }
                Box(m) { if (sp.faceUp) CardFace(sp.card, Modifier.fillMaxSize()) else CardBack(Modifier.fillMaxSize()) }
            }
        }

        // Toque no monte (por cima das cartas viradas do monte).
        Box(
            Modifier.zIndex(5_000f)
                .offset(g.stock.x.dp, g.stock.y.dp)
                .size(g.cardW.dp, g.cardH.dp)
                .testTag("stock")
                .clickable(remember { MutableInteractionSource() }, indication = null) { onTapStock() },
        )
        if (state.stock.isNotEmpty()) {
            Text(
                state.stock.size.toString(),
                color = TitleCream,
                fontSize = 10.sp,
                modifier = Modifier.zIndex(5_001f).offset((g.stock.x + 2).dp, (g.stock.y + g.cardH + 1).dp),
            )
        }
        if (hint == Hint.DrawStock || hint == Hint.Recycle) {
            Box(
                Modifier.zIndex(5_002f).offset(g.stock.x.dp, g.stock.y.dp).size(g.cardW.dp, g.cardH.dp)
                    .border(3.dp, HintOutline, RoundedCornerShape((g.cardW * 0.08f).dp)).testTag("hint-source"),
            )
        }
        all.firstOrNull { it.card in sources }?.let { first ->
            val y = if (first.inTableau) first.y - scrollY else first.y
            Box(Modifier.zIndex(9_001f).offset(first.x.dp, y.dp).size(g.cardW.dp, g.cardH.dp).testTag("hint-source"))
        }
        hintTargetRect(state, g, hint)?.let { r ->
            val y = if (r.y >= g.tableauTop) r.y - scrollY else r.y
            Box(
                Modifier.zIndex(9_000f).offset(r.x.dp, y.dp).size(r.w.dp, r.h.dp)
                    .border(3.dp, HintOutline, RoundedCornerShape((g.cardW * 0.08f).dp)).testTag("hint-target"),
            )
        }
    }
}

@Composable
private fun Slot(r: Rect, label: String, modifier: Modifier) {
    EmptySlot(label, modifier.offset(r.x.dp, r.y.dp).size(r.w.dp, r.h.dp))
}
