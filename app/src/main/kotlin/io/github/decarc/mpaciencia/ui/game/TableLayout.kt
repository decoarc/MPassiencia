package io.github.decarc.mpaciencia.ui.game

import io.github.decarc.mpaciencia.core.model.GameState
import io.github.decarc.mpaciencia.core.model.PileRef

/** Retângulo em dp. */
data class Rect(val x: Float, val y: Float, val w: Float, val h: Float) {
    fun contains(px: Float, py: Float): Boolean = px >= x && px < x + w && py >= y && py < y + h
}

/** Distância vertical entre cartas sobrepostas de uma coluna. */
data class ColumnSteps(val faceDown: Float, val faceUp: Float)

data class TableGeometry(
    val cardW: Float,
    val cardH: Float,
    val stock: Rect,
    val waste: Rect,
    val wasteFanStep: Float,
    val foundations: List<Rect>,
    /** Retângulo da primeira carta de cada coluna. */
    val columns: List<Rect>,
    val steps: List<ColumnSteps>,
    val tableauTop: Float,
    /** Altura da coluna mais longa, a partir de [tableauTop]. */
    val tableauContentHeight: Float,
    val scrolls: Boolean,
)

/** Geometria da mesa com o celular deitado (spec 6.2). Função pura, tudo em dp. */
object TableLayout {
    const val TOP_BAR = 40f
    const val MARGIN = 8f
    const val GAP_RATIO = 0.10f
    const val ASPECT = 1.4f
    const val MAX_CARD_W = 96f
    const val DOWN_COMFY = 0.15f
    const val UP_COMFY = 0.30f
    const val DOWN_MIN = 4f
    const val UP_MIN = 18f

    /** Fileira de cima: monte + descarte em leque (1,5 carta) + 12 fundações. */
    private const val TOP_ROW_SLOTS = 14.5f
    private const val COLUMNS = 13

    fun compute(widthDp: Float, heightDp: Float, state: GameState): TableGeometry {
        val byWidth = (widthDp - 2 * MARGIN) / (TOP_ROW_SLOTS + COLUMNS * GAP_RATIO)
        val byHeight = ((heightDp - TOP_BAR - 3 * MARGIN) / (2 + 4 * UP_COMFY)) / ASPECT
        val w = minOf(byWidth, byHeight, MAX_CARD_W)
        val h = w * ASPECT
        val gap = w * GAP_RATIO
        val step = w + gap

        val rowWidth = TOP_ROW_SLOTS * w + COLUMNS * gap
        val x0 = (widthDp - rowWidth) / 2
        val rowY = TOP_BAR + MARGIN
        val stock = Rect(x0, rowY, w, h)
        val waste = Rect(x0 + step, rowY, 1.5f * w, h)
        val firstFoundation = x0 + 2.5f * w + 2 * gap
        val foundations = List(GameState.FOUNDATIONS) { Rect(firstFoundation + it * step, rowY, w, h) }

        val tableauTop = rowY + h + MARGIN
        val tableauWidth = COLUMNS * w + (COLUMNS - 1) * gap
        val cx0 = (widthDp - tableauWidth) / 2
        val columns = List(COLUMNS) { Rect(cx0 + it * step, tableauTop, w, h) }

        val available = heightDp - tableauTop - MARGIN
        val steps = state.tableau.map { column ->
            stepsFor(column.count { !it.faceUp }, column.count { it.faceUp }, h, available)
        }
        val content = state.tableau.indices.maxOf { columnHeight(state, it, steps[it], h) }
        return TableGeometry(
            cardW = w,
            cardH = h,
            stock = stock,
            waste = waste,
            wasteFanStep = 0.25f * w,
            foundations = foundations,
            columns = columns,
            steps = steps,
            tableauTop = tableauTop,
            tableauContentHeight = content,
            scrolls = content > available + 0.01f,
        )
    }

    /** Aperta primeiro as viradas para baixo, depois as viradas para cima; abaixo dos mínimos, a área rola. */
    private fun stepsFor(down: Int, up: Int, h: Float, available: Float): ColumnSteps {
        val upGaps = maxOf(0, up - 1)
        fun need(sd: Float, su: Float) = h + down * sd + upGaps * su

        var sd = DOWN_COMFY * h
        var su = UP_COMFY * h
        if (need(sd, su) <= available) return ColumnSteps(sd, su)
        if (down > 0) {
            sd = maxOf(DOWN_MIN, (available - h - upGaps * su) / down)
            if (need(sd, su) <= available) return ColumnSteps(sd, su)
            sd = DOWN_MIN
        }
        if (upGaps > 0) su = maxOf(UP_MIN, (available - h - down * sd) / upGaps)
        return ColumnSteps(sd, su)
    }

    private fun columnHeight(state: GameState, column: Int, steps: ColumnSteps, h: Float): Float {
        val cards = state.tableau[column]
        if (cards.isEmpty()) return h
        return offsetOf(cards.size - 1, cards.map { it.faceUp }, steps) + h
    }

    private fun offsetOf(index: Int, faceUp: List<Boolean>, steps: ColumnSteps): Float =
        (0 until index).sumOf { (if (faceUp[it]) steps.faceUp else steps.faceDown).toDouble() }.toFloat()

    /** Topo (y, sem rolagem) da carta [indexInColumn] da coluna [column]. */
    fun cardTop(g: TableGeometry, column: Int, indexInColumn: Int, state: GameState): Float =
        g.columns[column].y + offsetOf(indexInColumn, state.tableau[column].map { it.faceUp }, g.steps[column])

    /** A pilha sob o ponto (x, y), ou `null`. [scrollY] é a rolagem da área das colunas. */
    fun hitTest(g: TableGeometry, state: GameState, x: Float, y: Float, scrollY: Float = 0f): PileRef? {
        if (g.stock.contains(x, y)) return PileRef.Stock
        if (g.waste.contains(x, y)) return PileRef.Waste
        g.foundations.indexOfFirst { it.contains(x, y) }.takeIf { it >= 0 }?.let { return PileRef.Foundation(it) }
        if (y < g.tableauTop) return null
        val yy = y + scrollY
        g.columns.forEachIndexed { i, col ->
            val height = columnHeight(state, i, g.steps[i], g.cardH)
            if (Rect(col.x, col.y, col.w, height).contains(x, yy)) return PileRef.Tableau(i)
        }
        return null
    }
}
