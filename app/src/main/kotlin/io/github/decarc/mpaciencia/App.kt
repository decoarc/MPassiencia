package io.github.decarc.mpaciencia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.decarc.mpaciencia.ui.theme.TitleCream
import io.github.decarc.mpaciencia.ui.theme.feltBackground

@Composable
fun App() {
    Box(Modifier.fillMaxSize().feltBackground(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.app_name), color = TitleCream)
    }
}
