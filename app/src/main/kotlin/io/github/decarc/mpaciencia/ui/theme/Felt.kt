package io.github.decarc.mpaciencia.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/** Feltro verde: degradê radial do centro (a 40% da altura) até a borda. */
fun Modifier.feltBackground(): Modifier = drawBehind {
    val center = Offset(size.width / 2f, size.height * 0.4f)
    val radius = maxOf(size.width, size.height) * 0.75f
    drawRect(Brush.radialGradient(listOf(FeltCenter, FeltEdge), center = center, radius = radius))
}
