package io.github.decarc.mpaciencia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.decarc.mpaciencia.R
import io.github.decarc.mpaciencia.core.model.DrawMode
import io.github.decarc.mpaciencia.data.AnimationSpeed
import io.github.decarc.mpaciencia.data.Settings
import io.github.decarc.mpaciencia.ui.rules.ScreenHeader
import io.github.decarc.mpaciencia.ui.theme.Cream
import io.github.decarc.mpaciencia.ui.theme.Gold
import io.github.decarc.mpaciencia.ui.theme.Ink
import io.github.decarc.mpaciencia.ui.theme.feltBackground

@Composable
fun SettingsScreen(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().feltBackground().windowInsetsPadding(WindowInsets.displayCutout)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(stringResource(R.string.settings_title), onBack)
            Column(
                Modifier.padding(top = 12.dp).widthIn(max = 620.dp).align(Alignment.CenterHorizontally)
                    .background(Cream, RoundedCornerShape(14.dp)).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Label(stringResource(R.string.settings_default_mode))
                ModeRow(stringResource(R.string.mode_one), settings.defaultDrawMode == DrawMode.ONE, "settings-mode-one") {
                    onChange { it.copy(defaultDrawMode = DrawMode.ONE) }
                }
                ModeRow(stringResource(R.string.mode_three), settings.defaultDrawMode == DrawMode.THREE, "settings-mode-three") {
                    onChange { it.copy(defaultDrawMode = DrawMode.THREE) }
                }

                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("settings-sound")
                        .toggleable(settings.soundOn, role = Role.Switch) { on -> onChange { it.copy(soundOn = on) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.settings_sound), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = settings.soundOn, onCheckedChange = null)
                }

                Label(stringResource(R.string.settings_speed))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val labels = mapOf(
                        AnimationSpeed.NORMAL to R.string.speed_normal,
                        AnimationSpeed.FAST to R.string.speed_fast,
                        AnimationSpeed.OFF to R.string.speed_off,
                    )
                    AnimationSpeed.entries.forEach { speed ->
                        FilterChip(
                            selected = settings.animationSpeed == speed,
                            onClick = { onChange { it.copy(animationSpeed = speed) } },
                            label = { Text(stringResource(labels.getValue(speed)), fontSize = 16.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor = Ink,
                                labelColor = Ink,
                            ),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("settings-speed-${speed.name}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun ModeRow(label: String, selected: Boolean, tag: String, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onSelect).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, color = Ink, fontSize = 16.sp)
    }
}
