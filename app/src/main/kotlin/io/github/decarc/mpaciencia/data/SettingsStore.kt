package io.github.decarc.mpaciencia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.decarc.mpaciencia.core.model.DrawMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AnimationSpeed(val moveMillis: Int) { NORMAL(220), FAST(110), OFF(0) }

data class Settings(
    val defaultDrawMode: DrawMode = DrawMode.ONE,
    val soundOn: Boolean = true,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
)

interface SettingsStore {
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

/** O DataStore único do app. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsStore(private val dataStore: DataStore<Preferences>) : SettingsStore {
    private val drawMode = stringPreferencesKey("draw_mode")
    private val soundOn = booleanPreferencesKey("sound_on")
    private val animationSpeed = stringPreferencesKey("animation_speed")

    override val settings: Flow<Settings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { p ->
            val current = p.toSettings()
            val next = transform(current)
            p[drawMode] = next.defaultDrawMode.name
            p[soundOn] = next.soundOn
            p[animationSpeed] = next.animationSpeed.name
        }
    }

    private fun Preferences.toSettings() = Settings(
        defaultDrawMode = this[drawMode]?.let { v -> DrawMode.entries.firstOrNull { it.name == v } } ?: DrawMode.ONE,
        soundOn = this[soundOn] ?: true,
        animationSpeed = this[animationSpeed]?.let { v -> AnimationSpeed.entries.firstOrNull { it.name == v } } ?: AnimationSpeed.NORMAL,
    )
}
