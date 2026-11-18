package io.github.decarc.mpaciencia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FeltScheme = lightColorScheme(
    primary = Ink,
    onPrimary = TitleCream,
    secondary = Gold,
    onSecondary = Ink,
    background = FeltEdge,
    onBackground = TitleCream,
    surface = Cream,
    onSurface = Ink,
)

@Composable
fun MPacienciaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FeltScheme, content = content)
}
